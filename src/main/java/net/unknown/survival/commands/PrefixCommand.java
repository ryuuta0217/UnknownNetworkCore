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

package net.unknown.survival.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.LongArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.ryuuta0217.util.ComponentCollector;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.ComponentArgument;
import net.minecraft.server.level.ServerPlayer;
import net.unknown.core.define.DefinedTextColor;
import net.unknown.core.prefix.PlayerPrefixes;
import net.unknown.core.prefix.Prefix;
import net.unknown.core.util.NewMessageUtil;
import net.unknown.launchwrapper.util.ComponentUtil;
import net.unknown.survival.enums.Permissions;

import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Set;

public class PrefixCommand {
    private static final SuggestionProvider<CommandSourceStack> PREFIXES_CREATION_TIME_SUGGESTION = (ctx, suggestionsBuilder) -> {
        Set<Prefix> prefixes = PlayerPrefixes.getPrefixes(ctx.getSource().getPlayerOrException().getUUID());
        prefixes.parallelStream().forEach(prefix -> suggestionsBuilder.suggest(String.valueOf(prefix.getCreatedAt()), NewMessageUtil.convertAdventure2Minecraft(prefix.getPrefix())));
        return suggestionsBuilder.buildFuture();
    };

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext buildContext) {
        LiteralArgumentBuilder<CommandSourceStack> builder = LiteralArgumentBuilder.literal("prefix");

        builder.requires(Permissions.COMMAND_PREFIX::check)
                .then(Commands.literal("list")
                        .executes(PrefixCommand::executeList))
                .then(Commands.literal("add")
                        .requires(Permissions.COMMAND_PREFIX_MANAGE::check)
                        .then(Commands.argument("接頭辞", ComponentArgument.textComponent(buildContext))
                                .executes(PrefixCommand::executeAdd)))
                .then(Commands.literal("remove")
                        .requires(Permissions.COMMAND_PREFIX_MANAGE::check)
                        .then(Commands.argument("作成日時", LongArgumentType.longArg(0))
                                .suggests(PREFIXES_CREATION_TIME_SUGGESTION)
                                .executes(PrefixCommand::executeRemove)
                                .then(Commands.literal("show_list_after_remove")
                                        .executes(ctx -> executeRemove(ctx) + executeList(ctx)))))
                .then(Commands.literal("set")
                        .requires(Permissions.COMMAND_PREFIX_MANAGE::check)
                        .then(Commands.literal("temporary")
                                .then(Commands.argument("接頭辞", ComponentArgument.textComponent(buildContext))
                                        .executes(PrefixCommand::executeSetTemporary)))
                        .then(Commands.argument("作成日時", LongArgumentType.longArg(0))
                                .suggests(PREFIXES_CREATION_TIME_SUGGESTION)
                                .executes(PrefixCommand::executeSet)));

        dispatcher.register(builder);
    }

    private static int executeList(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        Set<Prefix> prefixes = PlayerPrefixes.getPrefixes(ctx.getSource().getPlayerOrException().getUUID());
        Component lines = prefixes.parallelStream()
                .sorted(Comparator.comparingLong(Prefix::getCreatedAt).reversed())
                .map(prefix -> {
                    ZonedDateTime createdAt = Instant.ofEpochMilli(prefix.getCreatedAt()).atZone(ZoneId.of("Asia/Tokyo"));
                    Component dateSeparator = Component.text("/", DefinedTextColor.GRAY);
                    Component createdAtComponent = Component.empty()
                            .append(Component.text(DateTimeFormatter.ofPattern("yyyy").format(createdAt), DefinedTextColor.GREEN))
                            .append(dateSeparator)
                            .append(Component.text(DateTimeFormatter.ofPattern("MM").format(createdAt), DefinedTextColor.GREEN))
                            .append(dateSeparator)
                            .append(Component.text(DateTimeFormatter.ofPattern("dd").format(createdAt), DefinedTextColor.GREEN))
                            .appendSpace()
                            .append(Component.text(DateTimeFormatter.ofPattern("HH:mm:ss").format(createdAt), DefinedTextColor.YELLOW))
                            .appendSpace()
                            .append(Component.text("に作成"))
                            .hoverEvent(HoverEvent.showText(Component.text(prefix.getCreatedAt(), DefinedTextColor.AQUA)));

                    return Component.empty()
                            .append(createdAtComponent)
                            .appendSpace()
                            .append(Component.text("[×]", DefinedTextColor.RED).hoverEvent(HoverEvent.showText(Component.text("接頭辞を削除"))).clickEvent(ClickEvent.runCommand("/prefix remove " + prefix.getCreatedAt() + " show_list_after_remove")))
                            .append(Component.text(":"))
                            .appendSpace()
                            .append(prefix.getPrefix())
                            .append(prefix.isActive() ? Component.text(" [現在有効]", DefinedTextColor.GREEN) : Component.empty())
                            .clickEvent(ClickEvent.suggestCommand(""));
                })
                .collect(ComponentCollector.toComponent(Component.newline()))
                .asComponent();

        Component header = Component.text("===== 接頭辞一覧(" + prefixes.size() + ") =====", DefinedTextColor.GOLD).append(Component.newline());

        NewMessageUtil.sendMessage(ctx.getSource(), Component.empty().append(header).append(lines), false);
        return prefixes.size();
    }

    private static int executeAdd(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        Component newPrefixComponent = NewMessageUtil.convertMinecraft2Adventure(ComponentArgument.getRawComponent(ctx, "接頭辞"));

        ServerPlayer player = ctx.getSource().getPlayerOrException();

        Prefix newPrefix = PlayerPrefixes.addPrefix(player.getUUID(), newPrefixComponent);
        NewMessageUtil.sendMessage(ctx.getSource(), Component.empty()
                .append(Component.text("接頭辞 "))
                .append(newPrefix.getPrefix())
                .append(Component.text(" を追加しました (作成日時 " + newPrefix.getCreatedAt() + ")")), true);
        return 0;
    }

    private static int executeRemove(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        long createdAt = LongArgumentType.getLong(ctx, "作成日時");

        ServerPlayer player = ctx.getSource().getPlayerOrException();

        PlayerPrefixes.getPrefixes(player.getUUID())
                .parallelStream()
                .filter(prefix -> prefix.getCreatedAt() == createdAt)
                .findAny()
                .ifPresentOrElse(prefix -> {
                    PlayerPrefixes.removePrefix(player.getUUID(), prefix);
                    NewMessageUtil.sendMessage(ctx.getSource(), Component.empty()
                            .append(Component.text("接頭辞 "))
                            .append(prefix.getPrefix())
                            .append(Component.text(" を削除しました")), true);
                }, () -> {
                    NewMessageUtil.sendErrorMessage(ctx.getSource(), Component.text("指定された作成日時に一致する接頭辞が見つかりませんでした"));
                });
        return 0;
    }

    private static int executeSet(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        long createdAt = LongArgumentType.getLong(ctx, "作成日時");

        ServerPlayer player = ctx.getSource().getPlayerOrException();

        PlayerPrefixes.getPrefixes(player.getUUID())
                .parallelStream()
                .filter(prefix -> prefix.getCreatedAt() == createdAt)
                .findAny()
                .ifPresentOrElse(newPrefix -> {
                    Prefix oldPrefix = PlayerPrefixes.getActivePrefix(player.getUUID());
                    if (oldPrefix != null && oldPrefix.isTemporary()) {
                        NewMessageUtil.sendMessage(ctx.getSource(), Component.empty()
                                .append(Component.text("設定されていた接頭辞 "))
                                .append(oldPrefix.getPrefix())
                                .append(Component.text(" は「一時的」としてマークされていました。接頭辞リストには残らず、削除されます")), true);
                    }

                    PlayerPrefixes.setPrefix(player.getUUID(), newPrefix);
                    NewMessageUtil.sendMessage(ctx.getSource(), Component.empty()
                            .append(Component.text("接頭辞を "))
                            .append(oldPrefix != null ? Component.empty().append(oldPrefix.getPrefix()).append(Component.text(" から ")) : Component.empty())
                            .append(newPrefix.getPrefix())
                            .append(Component.text(" に変更しました")), true);
                }, () -> {
                    NewMessageUtil.sendErrorMessage(ctx.getSource(), Component.text("指定された作成日時に一致する接頭辞が見つかりませんでした"));
                });
        return 0;
    }

    private static int executeSetTemporary(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        Component newPrefixComponent = NewMessageUtil.convertMinecraft2Adventure(ComponentArgument.getRawComponent(ctx, "接頭辞"));

        ServerPlayer player = ctx.getSource().getPlayerOrException();

        Prefix newPrefix = new Prefix(newPrefixComponent, System.currentTimeMillis(), true);
        Prefix oldPrefix = PlayerPrefixes.getActivePrefix(player.getUUID());
        if (oldPrefix != null && oldPrefix.isTemporary()) {
            NewMessageUtil.sendMessage(ctx.getSource(), Component.empty()
                    .append(Component.text("設定されていた接頭辞 "))
                    .append(oldPrefix.getPrefix())
                    .append(Component.text(" は「一時的」としてマークされていました。接頭辞リストには残らず、削除されます")), true);
        }

        PlayerPrefixes.setPrefix(player.getUUID(), newPrefix);
        NewMessageUtil.sendMessage(ctx.getSource(), Component.empty()
                .append(Component.text("接頭辞を "))
                .append(oldPrefix != null ? Component.empty().append(oldPrefix.getPrefix()).append(Component.text(" から ")) : Component.empty())
                .append(newPrefix.getPrefix())
                .append(Component.text(" に変更しました (一時的)")), true);
        return 0;
    }
}
