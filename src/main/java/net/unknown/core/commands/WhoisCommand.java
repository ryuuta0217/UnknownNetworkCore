/*
 * Copyright (c) 2023 Unknown Network Developers and contributors.
 *
 * All rights reserved.
 *
 * NOTICE: This license is subject to change without prior notice.
 *
 * Redistribution and use in source and binary forms, *without modification*,
 *     are permitted provided that the following conditions are met:
 *
 * I. Redistributions of source code must retain the above copyright notice,
 *     this list of conditions and the following disclaimer.
 *
 * II. Redistributions in binary form must reproduce the above copyright notice,
 *     this list of conditions and the following disclaimer in the
 *     documentation and/or other materials provided with the distribution.
 *
 * III. Neither the name of Unknown Network nor the names of its contributors may be used to
 *     endorse or promote products derived from this software without specific prior written permission.
 *
 * IV. This source code and binaries is provided by the copyright holders and contributors "AS-IS" and
 *     any express or implied warranties, including, but not limited to, the implied warranties of
 *     merchantability and fitness for a particular purpose are disclaimed.
 *     In not event shall the copyright owner or contributors be liable for
 *     any direct, indirect, incidental, special, exemplary, or consequential damages
 *     (including but not limited to procurement of substitute goods or services;
 *     loss of use data or profits; or business interruption) however caused and on any theory of liability,
 *     whether in contract, strict liability, or tort (including negligence or otherwise)
 *     arising in any way out of the use of this source code, event if advised of the possibility of such damage.
 */

package net.unknown.core.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import io.ipinfo.api.IPinfo;
import io.ipinfo.api.model.IPResponse;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.ComponentBuilder;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.selector.EntitySelector;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.unknown.core.define.DefinedTextColor;
import net.unknown.core.enums.Permissions;
import net.unknown.core.feature.WhoisListener;
import net.unknown.core.managers.RunnableManager;
import net.unknown.core.util.BrigadierUtil;
import net.unknown.core.util.NewMessageUtil;
import net.unknown.core.util.TextBasePagination;
import net.unknown.shared.whois.Whois;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class WhoisCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        LiteralArgumentBuilder<CommandSourceStack> builder = LiteralArgumentBuilder.literal("whois");
        builder.requires(Permissions.FEATURE_WHOIS::check)
                .executes(WhoisCommand::showWhoisInformation)
                .then(Commands.argument("target", EntityArgument.player())
                        .executes(WhoisCommand::showWhoisInformation))
                .then(Commands.literal("ipdb")
                        .executes(WhoisCommand::showIpDatabase)
                        .then(Commands.argument("page", IntegerArgumentType.integer(1))
                                .executes(WhoisCommand::showIpDatabase))
                        .then(Commands.literal("by")
                                .requires(Permissions.FEATURE_WHOIS_UNMASKED::check)
                                .then(Commands.literal("player")
                                        .then(Commands.argument("name", StringArgumentType.string())
                                                .suggests((ctx, suggestionsBuilder) -> SharedSuggestionProvider.suggest(Arrays.stream(Bukkit.getOfflinePlayers()).map(OfflinePlayer::getName), suggestionsBuilder))
                                                .executes(WhoisCommand::showIpsByPlayer)
                                                .then(Commands.argument("page", IntegerArgumentType.integer(1))
                                                        .executes(WhoisCommand::showIpsByPlayer))))
                                .then(Commands.literal("ip")
                                        .then(Commands.argument("ip", StringArgumentType.string()) // Supports wildcards (e.g. 60.*, 2408:*)
                                                .suggests((ctx, suggestionsBuilder) -> SharedSuggestionProvider.suggest(Whois.getIpDatabase().keySet().stream().map(InetAddress::getHostAddress), suggestionsBuilder))
                                                .executes(WhoisCommand::showPlayersByIp)
                                                .then(Commands.argument("page", IntegerArgumentType.integer(1))
                                                        .executes(WhoisCommand::showPlayersByIp)))))
                        .then(Commands.literal("lookup")
                                .then(Commands.argument("ip", StringArgumentType.string())
                                        .executes(ctx -> 1))));

        dispatcher.register(builder);
    }

    public static int showIpDatabase(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        int showPage = BrigadierUtil.getArgumentOrDefault(ctx, Integer.class, "page", 1);
        boolean mask = ctx.getSource().isPlayer() && ctx.getSource().getPlayerOrException().getBukkitEntity().hasPermission(Permissions.FEATURE_WHOIS_UNMASKED.getPermissionNode());

        TextBasePagination<Map.Entry<InetAddress, Map<UUID, Long>>> page = new TextBasePagination<>(Whois.getIpDatabase().entrySet(), (db, i) -> {
            Component l = Component.empty();

            Component ipBlock;
            IPResponse ipInfo = (IPResponse) Whois.getIpInfoCache().get(IPinfo.cacheKey(db.getKey().getHostAddress()));
            if (ipInfo != null) {
                ipBlock = Component.text(mask ? Whois.maskIpAddress(db.getKey()) : ipInfo.getIp()).hoverEvent(HoverEvent.showText(
                        Component.text("Country/Region: " + ipInfo.getRegion() + ", " + ipInfo.getCity() + ", " + ipInfo.getCountryName()).appendNewline()
                                .append(Component.text("Hostname: " + (mask ? Whois.maskHostName(ipInfo.getHostname()) : ipInfo.getHostname())))));
            } else {
                ipBlock = Component.text(db.getKey().getHostAddress());
            }
            l = l.append(ipBlock).append(Component.text(":", DefinedTextColor.WHITE)).appendSpace();

            int playerIndex = 0;
            for (Map.Entry<UUID, Long> playerEntry : db.getValue().entrySet()) {
                OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(playerEntry.getKey());
                boolean valid = offlinePlayer.getName() != null;
                l = l.append(Component.text((playerIndex > 0 ? ", " : "") + (valid ? offlinePlayer.getName() : "Unknown"), valid ? DefinedTextColor.YELLOW : DefinedTextColor.RED).hoverEvent(HoverEvent.showEntity(Key.key("minecraft", "player"), offlinePlayer.getUniqueId(), Component.text(valid ? offlinePlayer.getName() : "不明なプレイヤー"))));
                playerIndex++;
            }
            return l;
        }, 10, showPage, true, Component.text("IP Database"), pageNum -> "/whois ipdb " + pageNum);
        ctx.getSource().sendSuccess(() -> net.minecraft.network.chat.Component.Serializer.fromJson(GsonComponentSerializer.gson().serializeToTree(combineComponents(page.getTextLines())), MinecraftServer.getDefaultRegistryAccess()), false);
        return 0;
    }

    private static int showIpsByPlayer(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        String player = StringArgumentType.getString(ctx, "name");
        boolean hasWildcard = player.contains("*");

        if (!hasWildcard) {
            try {
                OfflinePlayer targetPlayer = Bukkit.getOfflinePlayer(player);
                if (!targetPlayer.hasPlayedBefore()) throw new IllegalArgumentException("不明なプレイヤーです: " + player);
            } catch (IllegalArgumentException e) {
                throw CommandSyntaxException.BUILT_IN_EXCEPTIONS.dispatcherParseException().create(e.getMessage());
            }
        }

        int showPage = BrigadierUtil.getArgumentOrDefault(ctx, Integer.class, "page", 1);
        boolean mask = ctx.getSource().isPlayer() && ctx.getSource().getPlayerOrException().getBukkitEntity().hasPermission(Permissions.FEATURE_WHOIS_UNMASKED.getPermissionNode());

        Map<UUID, Map<InetAddress, Long>> entries = Whois.getIpDatabase().entrySet().stream().flatMap(entry -> entry.getValue().entrySet().stream().map(playerEntry -> Map.entry(playerEntry.getKey(), Map.entry(entry.getKey(), playerEntry.getValue())))).collect(Collectors.groupingBy(Map.Entry::getKey, Collectors.toMap(e -> e.getValue().getKey(), e -> e.getValue().getValue())));

        TextBasePagination<Map.Entry<UUID, Map<InetAddress, Long>>> page = new TextBasePagination<>(entries.entrySet(), (db, i) -> {
            Component l = Component.empty();

            OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(db.getKey());
            boolean valid = offlinePlayer.getName() != null;
            l = l.append(Component.text((valid ? offlinePlayer.getName() : "Unknown"), valid ? DefinedTextColor.YELLOW : DefinedTextColor.RED).hoverEvent(HoverEvent.showEntity(Key.key("minecraft", "player"), offlinePlayer.getUniqueId(), Component.text(valid ? offlinePlayer.getName() : "不明なプレイヤー"))).append(Component.text(":", DefinedTextColor.WHITE)).appendSpace());

            int ipIndex = 0;
            for (Map.Entry<InetAddress, Long> ipEntry : db.getValue().entrySet()) {
                IPResponse ipInfo = (IPResponse) Whois.getIpInfoCache().get(IPinfo.cacheKey(ipEntry.getKey().getHostAddress()));
                Component ipBlock = ipInfo != null
                        ? Component.text(mask ? Whois.maskIpAddress(ipEntry.getKey()) : ipInfo.getIp())
                                .hoverEvent(HoverEvent.showText(Component.text("Country/Region: " + ipInfo.getRegion() + ", " + ipInfo.getCity() + ", " + ipInfo.getCountryName())
                                .appendNewline()
                                .append(Component.text("Hostname: " + (mask ? Whois.maskHostName(ipInfo.getHostname()) : ipInfo.getHostname())))))
                        : Component.text(ipEntry.getKey().getHostAddress());
                l = l.append(ipBlock).append(Component.text(":", DefinedTextColor.WHITE)).appendSpace();
                ipIndex++;
            }
            return l;
        }, 10, showPage, true, Component.text("IP Database"), pageNum -> "/whois ipdb by player \"" + player + "\" " + pageNum);
        ctx.getSource().sendSuccess(() -> net.minecraft.network.chat.Component.Serializer.fromJson(GsonComponentSerializer.gson().serializeToTree(combineComponents(page.getTextLines())), MinecraftServer.getDefaultRegistryAccess()), false);
        return 0;
    }

    public static int showPlayersByIp(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        String ip = StringArgumentType.getString(ctx, "ip");
        boolean hasWildcard = ip.contains("*");

        if (!hasWildcard) {
            try {
                InetAddress ipAddr = InetAddress.getByName(ip);
            } catch (UnknownHostException e) {
                throw CommandSyntaxException.BUILT_IN_EXCEPTIONS.dispatcherParseException().create(e.getMessage());
            }
        }
        int showPage = BrigadierUtil.getArgumentOrDefault(ctx, Integer.class, "page", 1);
        boolean mask = ctx.getSource().isPlayer() && ctx.getSource().getPlayerOrException().getBukkitEntity().hasPermission(Permissions.FEATURE_WHOIS_UNMASKED.getPermissionNode());

        Map<InetAddress, Map<UUID, Long>> entries = Whois.getIpDatabase().entrySet().stream().filter(e -> hasWildcard ? processWildcard(ip, e.getKey().getHostAddress()) : e.getKey().getHostAddress().equals(ip)).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        TextBasePagination<Map.Entry<InetAddress, Map<UUID, Long>>> page = new TextBasePagination<>(entries.entrySet(), (db, i) -> {
            Component l = Component.empty();

            Component ipBlock;
            IPResponse ipInfo = (IPResponse) Whois.getIpInfoCache().get(IPinfo.cacheKey(db.getKey().getHostAddress()));
            if (ipInfo != null) {
                ipBlock = Component.text(mask ? Whois.maskIpAddress(db.getKey()) : ipInfo.getIp()).hoverEvent(HoverEvent.showText(
                        Component.text("Country/Region: " + ipInfo.getRegion() + ", " + ipInfo.getCity() + ", " + ipInfo.getCountryName()).appendNewline()
                                .append(Component.text("Hostname: " + (mask ? Whois.maskHostName(ipInfo.getHostname()) : ipInfo.getHostname())))));
            } else {
                ipBlock = Component.text(db.getKey().getHostAddress());
            }
            l = l.append(ipBlock).append(Component.text(":", DefinedTextColor.WHITE)).appendSpace();

            int playerIndex = 0;
            for (Map.Entry<UUID, Long> playerEntry : db.getValue().entrySet()) {
                OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(playerEntry.getKey());
                boolean valid = offlinePlayer.getName() != null;
                l = l.append(Component.text((playerIndex > 0 ? ", " : "") + (valid ? offlinePlayer.getName() : "Unknown"), valid ? DefinedTextColor.YELLOW : DefinedTextColor.RED).hoverEvent(HoverEvent.showEntity(Key.key("minecraft", "player"), offlinePlayer.getUniqueId(), Component.text(valid ? offlinePlayer.getName() : "不明なプレイヤー"))));
                playerIndex++;
            }
            return l;
        }, 10, showPage, true, Component.text("IP Database"), pageNum -> "/whois ipdb by ip \"" + ip + "\" " + pageNum);
        ctx.getSource().sendSuccess(() -> net.minecraft.network.chat.Component.Serializer.fromJson(GsonComponentSerializer.gson().serializeToTree(combineComponents(page.getTextLines())), MinecraftServer.getDefaultRegistryAccess()), false);
        return 0;
    }

    private static Component combineComponents(Collection<Component> components) {
        Component base = Component.empty();
        int index = 0;
        for (Component component : components) {
            if (index != 0) base = base.appendNewline();
            base = base.append(component);
            index++;
        }
        return base;
    }

    public static int showWhoisInformation(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer target;
        if (BrigadierUtil.isArgumentKeyExists(ctx, "target")) {
            try {
                EntitySelector selector = ctx.getArgument("target", EntitySelector.class);
                target = selector.findSinglePlayer(ctx.getSource());
            } catch (IllegalArgumentException e) {
                target = ctx.getSource().getPlayerOrException();
            }
        } else {
            target = ctx.getSource().getPlayerOrException();
        }

        ServerPlayer finalTarget = target;
        ctx.getSource().sendSuccess(() -> net.minecraft.network.chat.Component.Serializer.fromJson(GsonComponentSerializer.gson().serializeToTree(WhoisListener.buildWhoisInformationMessage(finalTarget.getBukkitEntity(), finalTarget.getBukkitEntity().hasPermission(Permissions.FEATURE_WHOIS_UNMASKED.getPermissionNode()))), MinecraftServer.getDefaultRegistryAccess()), false);
        return 0;
    }

    /**
     * Test a wildcard pattern.
     * ex) *, 192.*, *.168.*, 192.16*.1*
     * @param pattern
     * @param text
     * @return true if matched
     */
    private static boolean processWildcard(String pattern, String text) {
        if (pattern.equals("*")) return true;

        int textCursor = 0;
        for (int patternCursor = 0; patternCursor < pattern.length(); patternCursor++) {
            char patternChar = pattern.charAt(patternCursor);

            if (patternChar != '*') { // literal processor
                char textChar = text.charAt(textCursor);
                if (textChar == patternChar) {
                    textCursor++;
                } else {
                    return false;
                }
            } else { // Wildcard processor
                if (patternCursor + 1 == pattern.length()) return true;
                boolean hasNextWildcard = pattern.indexOf('*', patternCursor + 1) != -1;

                char nextPatternChar = pattern.charAt(patternCursor + 1);
                if (nextPatternChar != '*') { // is literal
                    if (hasNextWildcard) { // has next wildcard? if not, use "endsWith"
                        String nextPattern = pattern.substring(patternCursor + 1, pattern.indexOf('*', patternCursor + 1));
                        if (text.indexOf(nextPattern, textCursor) != -1) {
                            textCursor = text.indexOf(nextPattern, textCursor) + nextPattern.length();
                            patternCursor = pattern.indexOf('*', patternCursor + 1) - 1;
                        } else {
                            return false;
                        }
                    } else {
                        return text.endsWith(pattern.substring(patternCursor + 1));
                    }
                } else {
                    // if wildcard? passthrough and continue
                }
            }
        }
        return true;
    }

    private static boolean processWildcardReader(String pattern, String text) {
        if (pattern.equals("*")) return true;
        StringReader patternReader = new StringReader(pattern);
        StringReader textReader = new StringReader(text);

        while (patternReader.canRead()) {
            if (patternReader.peek() == '*') {
                patternReader.skip();
                if (!patternReader.canRead()) return true;
            }

            int startCursor = patternReader.getCursor();
            boolean nextWildcardFound = false;
            while (patternReader.canRead()) {
                if (patternReader.peek() == '*') {
                    nextWildcardFound = true;
                    break;
                } else {
                    patternReader.skip();
                }
            }
            int endCursor = patternReader.getCursor();
            String str = patternReader.getString().substring(startCursor, endCursor);

            int textCursor = nextWildcardFound ? text.indexOf(str, textReader.getCursor()) : text.lastIndexOf(str);
            if (textCursor == -1) return false;
            if (startCursor == 0 && textCursor != 0) return false;

            textReader.setCursor(textCursor + str.length());
            if (!textReader.canRead()) return true;
        }
        return false;
    }
}
