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

package net.unknown.core.util;

import net.kyori.adventure.identity.Identity;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.contents.LiteralContents;
import net.minecraft.world.level.BaseCommandBlock;
import net.minecraft.world.level.GameRules;
import net.unknown.core.define.DefinedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameRule;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.craftbukkit.v1_20_R1.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.bukkit.permissions.Permissible;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Pattern;

public class MessageUtil {
    private static final String PREFIX = "§7[§6§lU.N.§r§7] §r";
    private static final Component PREFIX_ADVENTURE_COMPONENT = Component.empty()
            .append(Component.text("[", DefinedTextColor.GRAY))
            .append(Component.text("U.N.", net.kyori.adventure.text.format.Style.style(DefinedTextColor.GOLD, TextDecoration.BOLD.withState(true))))
            .append(Component.text("]", DefinedTextColor.GRAY))
            .append(Component.text(" "));
    private static final MutableComponent PREFIX_MINECRAFT_COMPONENT = MutableComponent.create(new LiteralContents(""))
            .append(MutableComponent.create(new LiteralContents("[")).withStyle(ChatFormatting.GRAY))
            .append(MutableComponent.create(new LiteralContents("U.N.")).withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD))
            .append(MutableComponent.create(new LiteralContents("]")).withStyle(ChatFormatting.GRAY))
            .append(MutableComponent.create(new LiteralContents(" ")));
    private static final String PREFIX_ERROR = "§c";
    private static final String PREFIX_ADMIN = "§r";
    private static final String PREFIX_ADMIN_ERROR = "§c";
    private static final Map<String, String> WORLD_NAME_DISPLAY = new HashMap<>() {{
        put("world", "メイン");
        put("world_nether", "メインネザー");
        put("world_the_end", "メインエンド");
        put("main2", "メイン2");
        put("main2_nether", "メイン2ネザー");
        put("main2_the_end", "メイン2エンド");
        put("resource", "資源");
        put("resource_nether", "資源ネザー");
        put("resource_the_end", "資源エンド");
    }};

    private static final Pattern UUID_PATTERN = Pattern.compile("(?i)^[\\dA-F]{8}-[\\dA-F]{4}-4[\\dA-F]{3}-[89AB][\\dA-F]{3}-[\\dA-F]{12}");

    public static void sendMessage(Player player, String msg) {
        sendMessage(((CraftPlayer) player).getHandle().createCommandSourceStack(), msg, true);
    }

    public static void sendMessage(CommandSourceStack commandSourceStack, String msg) {
        sendMessage(commandSourceStack, msg, true);
    }

    public static void sendMessage(CommandSourceStack commandSourceStack, String msg, boolean showOthers) {
        commandSourceStack.sendSuccess(() -> PREFIX_MINECRAFT_COMPONENT.copy().append(net.minecraft.network.chat.Component.literal(msg.replace("\n", "\n" + PREFIX))), false);
        if (showOthers) {
            String msgFormat = String.format("§7§o[%s: %s]", commandSourceStack.getTextName(), ChatColor.stripColor(msg));
            broadcastCommandFeedback(msgFormat, commandSourceStack);
        }
    }

    public static void sendErrorMessage(Player player, String msg) {
        player.sendMessage(PREFIX_ERROR + msg);
    }

    public static void sendErrorMessage(CommandSourceStack commandSourceStack, String msg) {
        sendErrorMessage(commandSourceStack, msg, false);
    }

    public static void sendErrorMessage(CommandSourceStack commandSourceStack, String msg, boolean showOthers) {
        commandSourceStack.sendSuccess(() -> net.minecraft.network.chat.Component.literal(PREFIX_ERROR + msg.replace("\n", "\n" + PREFIX_ERROR)).setStyle(Style.EMPTY.withColor(ChatFormatting.RED)), false);
        if (showOthers) {
            String msgFormat = String.format("§7§o[%s: §c%s§7]", commandSourceStack.getTextName(), ChatColor.stripColor(msg));
            broadcastCommandFeedback(msgFormat, commandSourceStack);
        }
    }

    public static void sendAdminMessage(Player player, String msg) {
        player.sendMessage(PREFIX_ADMIN + msg);
    }

    public static void sendAdminMessage(CommandSourceStack commandSourceStack, String msg) {
        sendAdminMessage(commandSourceStack, msg, true);
    }

    public static void sendAdminMessage(CommandSourceStack commandSourceStack, String msg, boolean showOthers) {
        commandSourceStack.sendSuccess(() -> net.minecraft.network.chat.Component.literal(PREFIX_ADMIN + msg.replace("\n", "\n" + PREFIX_ADMIN)), false);
        if (showOthers) {
            String msgFormat = String.format("§7§o[%s: %s]", commandSourceStack.getTextName(), ChatColor.stripColor(msg));
            broadcastCommandFeedback(msgFormat, commandSourceStack);
        }
    }

    public static void sendAdminErrorMessage(Player player, String msg) {
        player.sendMessage(PREFIX_ADMIN_ERROR + msg);
    }

    public static void sendAdminErrorMessage(CommandSourceStack commandSourceStack, String msg) {
        sendAdminErrorMessage(commandSourceStack, msg, false);
    }

    public static void sendAdminErrorMessage(CommandSourceStack commandSourceStack, String msg, boolean showOthers) {
        commandSourceStack.sendSuccess(() -> net.minecraft.network.chat.Component.literal(PREFIX_ADMIN_ERROR + msg.replace("\n", "\n" + PREFIX_ADMIN_ERROR)).withStyle(Style.EMPTY.withColor(ChatFormatting.RED)), false);
        if (showOthers) {
            String msgFormat = String.format("§7§o[%s: §c%s§7]", commandSourceStack.getTextName(), ChatColor.stripColor(msg));
            broadcastCommandFeedback(msgFormat, commandSourceStack);
        }
    }

    public static void broadcast(Component message, UUID broadcaster) {
        broadcast(message, broadcaster, true);
    }

    public static void broadcast(Component message, UUID broadcaster, boolean prefix) {
        Bukkit.getOnlinePlayers().forEach(p -> {
            p.sendMessage(broadcaster == null ? Identity.nil() : Identity.identity(broadcaster), prefix ? Component.text(PREFIX).append(message) : message);
        });
    }

    private static void broadcastCommandFeedback(String message, CommandSourceStack sentBy) {
        Set<CommandSender> recipients = new HashSet<>();

        if (sentBy.source instanceof BaseCommandBlock commandBlock) {
            boolean commandBlockOutput = commandBlock.getLevel().getGameRules().getBoolean(GameRules.RULE_COMMANDBLOCKOUTPUT);
            if (!commandBlockOutput) return;
        }

        for (Permissible permissible : Bukkit.getPluginManager().getPermissionSubscriptions("minecraft.admin.command_feedback")) {
            if (permissible instanceof CommandSender sender && (permissible.hasPermission("minecraft.admin.command_feedback") || permissible.isOp())) {
                if (sender instanceof Player player) {
                    /*
                     * when Executor World is sendCommandFeedback: true
                     * but receiver World is sendCommandFeedback: false
                     * -> Feedback not received.
                     *
                     * when executor world is sendCommandFeedback: false
                     * but receiver world is sendCommandFeedback: true
                     * -> Feedback receive.
                     */
                    if (!player.getWorld().getGameRuleValue(GameRule.SEND_COMMAND_FEEDBACK)) return;
                }

                if (!sentBy.getBukkitSender().getName().equals(((CommandSender) permissible).getName())) {
                    recipients.add((CommandSender) permissible);
                }
            }
        }

        for (CommandSender recipient : recipients) {
            recipient.sendMessage(message);
        }
    }

    public static String getWorldNameDisplay(World world) {
        return (WORLD_NAME_DISPLAY.containsKey(world.getName()) ? WORLD_NAME_DISPLAY.get(world.getName()) : world.getName()) + "ワールド";
    }

    public static String getWorldName(World world) {
        return getWorldName(world.getName());
    }

    public static String getWorldName(String worldName) {
        return WORLD_NAME_DISPLAY.getOrDefault(worldName, worldName);
    }

    public static String getMessagePrefix() {
        return PREFIX;
    }

    public static Component getMessagePrefixComponent() {
        return PREFIX_ADVENTURE_COMPONENT;
    }

    public static String convertMillisToDateFormat(long millis, String format) {
        if (format == null) format = "MM/dd HH:mm:ss";
        SimpleDateFormat sdf = new SimpleDateFormat(format);
        sdf.setTimeZone(TimeZone.getTimeZone("Asia/Tokyo"));
        return sdf.format(new Date(millis));
    }

    /*public static Component getPlayerComponent(Player player, Component playerComponent, boolean prefix, boolean suffix) {
        Component baseComponent = Component.text("");
        if(prefix) baseComponent = baseComponent.append(Component.text(LuckPerms.getPrefix(player.getUniqueId())));
        if(playerComponent != null) {
            baseComponent = baseComponent.append(playerComponent.hoverEvent(HoverEvent.showEntity(HoverEvent.ShowEntity.of(Key.key("player"), player.getUniqueId(), Component.text(player.getName()))))
                    .clickEvent(ClickEvent.suggestCommand("/tell " + player.getName() + " ")));
        } else {
            baseComponent = baseComponent.append(player.displayName().hoverEvent(HoverEvent.showEntity(HoverEvent.ShowEntity.of(Key.key("player"), player.getUniqueId(), Component.text(player.getName()))))
                    .clickEvent(ClickEvent.suggestCommand("/tell " + player.getName() + " ")));
        }
        if(suffix) baseComponent = baseComponent.append(Component.text(LuckPerms.getSuffix(player.getUniqueId())));
        return baseComponent;
    }*/

    public static Component convertNMS2Adventure(net.minecraft.network.chat.Component nms) {
        return GsonComponentSerializer.gson().deserializeFromTree(net.minecraft.network.chat.Component.Serializer.toJsonTree(nms));
    }

    public static net.minecraft.network.chat.Component convertAdventure2NMS(Component adventure) {
        return net.minecraft.network.chat.Component.Serializer.fromJson(GsonComponentSerializer.gson().serializeToTree(adventure));
    }

    public static boolean isUUID(String s) {
        return UUID_PATTERN.matcher(s).matches();
    }
}
