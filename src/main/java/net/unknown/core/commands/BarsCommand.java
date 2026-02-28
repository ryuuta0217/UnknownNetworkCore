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
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.IdentifierArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import net.minecraft.server.bossevents.CustomBossEvent;
import net.minecraft.server.level.ServerPlayer;
import net.unknown.core.enums.Permissions;
import net.unknown.core.managers.BossBarManager;
import net.unknown.core.util.BrigadierUtil;

import java.util.Collection;
import java.util.List;

public class BarsCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        LiteralArgumentBuilder<CommandSourceStack> builder = LiteralArgumentBuilder.literal("bars");

        builder.requires(Permissions.COMMAND_BARS::check)
                .executes(BarsCommand::showStatus)
                .then(Commands.literal("toggle")
                        .then(Commands.argument("identifier", IdentifierArgument.id())
                                .suggests((ctx, suggestionsBuilder) -> SharedSuggestionProvider.suggestResource(BossBarManager.getInstance().getRegisteredBossBars().parallelStream().map(CustomBossEvent::getTextId), suggestionsBuilder))
                                .executes(BarsCommand::toggle)
                                .then(Commands.argument("targets", EntityArgument.players())
                                        .executes(BarsCommand::toggle))))
                .then(Commands.literal("show")
                        .then(Commands.argument("identifier", IdentifierArgument.id())
                                .suggests((ctx, suggestionsBuilder) -> SharedSuggestionProvider.suggestResource(BossBarManager.getInstance().getRegisteredBossBars().parallelStream().map(CustomBossEvent::getTextId), suggestionsBuilder))
                                .executes(BarsCommand::show)
                                .then(Commands.argument("targets", EntityArgument.players())
                                        .executes(BarsCommand::show))))
                .then(Commands.literal("hide")
                        .then(Commands.argument("identifier", IdentifierArgument.id())
                                .suggests((ctx, suggestionsBuilder) -> SharedSuggestionProvider.suggestResource(BossBarManager.getInstance().getRegisteredBossBars().parallelStream().map(CustomBossEvent::getTextId), suggestionsBuilder))
                                .executes(BarsCommand::hide)
                                .then(Commands.argument("targets", EntityArgument.players())
                                        .executes(BarsCommand::hide))))
                .then(Commands.literal("status")
                        .executes(BarsCommand::showStatus)
                        .then(Commands.argument("target", EntityArgument.player())
                                .executes(BarsCommand::showStatus)));

        dispatcher.register(builder);
    }

    private static int showStatus(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = BrigadierUtil.isArgumentKeyExists(ctx, "target") ? EntityArgument.getPlayer(ctx, "target") : ctx.getSource().getPlayerOrException();

        ctx.getSource().sendSuccess(() -> {
            MutableComponent statusMessage = Component.literal("===== 登録されているボスバー (" + BossBarManager.getInstance().getRegisteredBossBars().size() + ") =====");
            BossBarManager.getInstance().getRegisteredBossBars().forEach(bossBar -> {
                boolean visible = BossBarManager.getInstance().getVisibilityHandler().isVisible(player.getBukkitEntity(), bossBar.getTextId());
                statusMessage.append("\n").append(Component.literal("["  + (visible ? "S" : "H") + "]").withStyle(visible ? ChatFormatting.GREEN : ChatFormatting.RED)).append(" - ").append(bossBar.getTextId().toString());
            });
            return statusMessage;
        }, false);

        return Math.toIntExact(BossBarManager.getInstance().getRegisteredBossBars().stream().filter(bossBar -> BossBarManager.getInstance().getVisibilityHandler().isVisible(player.getBukkitEntity(), bossBar.getTextId())).count());
    }

    private static int toggle(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        Identifier identifier = IdentifierArgument.getId(ctx, "identifier");
        if (BossBarManager.getInstance().getRegisteredBossBars().parallelStream().map(CustomBossEvent::getTextId).noneMatch(registeredIdentifier -> registeredIdentifier.equals(identifier))) {
            ctx.getSource().sendFailure(Component.literal("指定された識別子のボスバーは存在しません。"));
            return -1;
        }

        Collection<ServerPlayer> targets = BrigadierUtil.isArgumentKeyExists(ctx, "targets") ? EntityArgument.getPlayers(ctx, "targets") : List.of(ctx.getSource().getPlayerOrException());

        targets.forEach(target -> BossBarManager.getInstance().getVisibilityHandler().toggleVisibility(target.getBukkitEntity(), identifier));

        ctx.getSource().sendSuccess(() -> {
            return Component.empty()
                    .append(targets.size() == 1 ? targets.iterator().next().getDisplayName() : Component.literal(String.valueOf(targets.size())))
                    .append(" のボスバー " + identifier + " の表示状態を切り替えました");
        }, true);

        return targets.size();
    }

    private static int show(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        Identifier identifier = IdentifierArgument.getId(ctx, "identifier");
        if (BossBarManager.getInstance().getRegisteredBossBars().parallelStream().map(CustomBossEvent::getTextId).noneMatch(registeredIdentifier -> registeredIdentifier.equals(identifier))) {
            ctx.getSource().sendFailure(Component.literal("指定された識別子のボスバーは存在しません。"));
            return -1;
        }
        Collection<ServerPlayer> targets = BrigadierUtil.isArgumentKeyExists(ctx, "targets") ? EntityArgument.getPlayers(ctx, "targets") : List.of(ctx.getSource().getPlayerOrException());

        if (targets.size() == 1) {
            ServerPlayer target = targets.iterator().next();
            boolean selfTarget = ctx.getSource().getPlayer() != null && target.getUUID().equals(ctx.getSource().getPlayer().getUUID());
            boolean alreadyVisible = BossBarManager.getInstance().getVisibilityHandler().isVisible(target.getBukkitEntity(), identifier);
            if (alreadyVisible) {
                ctx.getSource().sendFailure(Component.empty()
                        .append(!selfTarget ? target.getDisplayName() : Component.empty())
                        .append((!selfTarget ? "の" : "") + "ボスバー " + identifier + " はすでに表示されています"));
                return -2;
            }
        }

        targets.forEach(target -> BossBarManager.getInstance().getVisibilityHandler().setVisible(target.getBukkitEntity(), identifier, true));

        ctx.getSource().sendSuccess(() -> {
            return Component.empty()
                    .append(targets.size() == 1 ? targets.iterator().next().getDisplayName() : Component.literal(String.valueOf(targets.size())))
                    .append(" のボスバー " + identifier + " の表示状態を ")
                    .append(Component.literal("表示").withStyle(ChatFormatting.GREEN))
                    .append(" に変更しました");
        }, true);

        return targets.size();
    }

    private static int hide(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        Identifier identifier = IdentifierArgument.getId(ctx, "identifier");
        if (BossBarManager.getInstance().getRegisteredBossBars().parallelStream().map(CustomBossEvent::getTextId).noneMatch(registeredIdentifier -> registeredIdentifier.equals(identifier))) {
            ctx.getSource().sendFailure(Component.literal("指定された識別子のボスバーは存在しません。"));
            return -1;
        }

        Collection<ServerPlayer> targets = BrigadierUtil.isArgumentKeyExists(ctx, "targets") ? EntityArgument.getPlayers(ctx, "targets") : List.of(ctx.getSource().getPlayerOrException());

        if (targets.size() == 1) {
            ServerPlayer target = targets.iterator().next();
            boolean selfTarget = ctx.getSource().getPlayer() != null && target.getUUID().equals(ctx.getSource().getPlayer().getUUID());
            boolean alreadyHidden = !BossBarManager.getInstance().getVisibilityHandler().isVisible(target.getBukkitEntity(), identifier);
            if (alreadyHidden) {
                ctx.getSource().sendFailure(Component.empty()
                        .append(!selfTarget ? target.getDisplayName() : Component.empty())
                        .append((!selfTarget ? "の" : "") + "ボスバー " + identifier + " はすでに非表示になっています"));
                return -2;
            }
        }

        targets.forEach(target -> BossBarManager.getInstance().getVisibilityHandler().setVisible(target.getBukkitEntity(), identifier, false));

        ctx.getSource().sendSuccess(() -> {
            return Component.empty()
                    .append(targets.size() == 1 ? targets.iterator().next().getDisplayName() : Component.literal(String.valueOf(targets.size())))
                    .append(" のボスバー " + identifier + " の表示状態を ")
                    .append(Component.literal("非表示").withStyle(ChatFormatting.RED))
                    .append(" に変更しました");
        }, true);
        return targets.size();
    }
}
