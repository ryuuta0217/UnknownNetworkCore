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
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.unknown.core.enums.Permissions;
import net.unknown.core.skin.PlayerSkinRepository;
import net.unknown.core.skin.Skin;
import net.unknown.core.skin.SkinManager;
import net.unknown.core.skin.SkinSource;
import net.unknown.core.util.BrigadierUtil;
import net.unknown.core.util.NewMessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

public class SkinCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        LiteralArgumentBuilder<CommandSourceStack> builder = LiteralArgumentBuilder.literal("skin");
        builder.requires(Permissions.COMMAND_SKIN::check);
        builder.then(Commands.literal("set")
                        .then(Commands.literal("from")
                                .then(Commands.literal("player")
                                        .then(Commands.argument("プレイヤー名", StringArgumentType.word())
                                                .suggests(Suggestions.ALL_PLAYER_SUGGEST)
                                                .requires(Permissions.COMMAND_SKIN::checkAndIsPlayer)
                                                .executes(SkinCommand::setCustomSkinFromPlayer)
                                                .then(Commands.literal("to")
                                                        .requires(Permissions.COMMAND_SKIN::check)
                                                        .then(Commands.argument("対象", EntityArgument.player())
                                                                .executes(SkinCommand::setCustomSkinFromPlayer)))))
                                .then(Commands.literal("string")
                                        .then(Commands.argument("base64", StringArgumentType.string())
                                                .then(Commands.argument("signature", StringArgumentType.string())
                                                        .requires(Permissions.COMMAND_SKIN::checkAndIsPlayer)
                                                        .executes(SkinCommand::setCustomSkinFromString)
                                                        .then(Commands.literal("to")
                                                                .requires(Permissions.COMMAND_SKIN::check)
                                                                .then(Commands.argument("対象", EntityArgument.player())
                                                                        .executes(SkinCommand::setCustomSkinFromString))))))))
                .then(Commands.literal("reset")
                        .requires(Permissions.COMMAND_SKIN::checkAndIsPlayer)
                        .executes(SkinCommand::resetSkin)
                        .then(Commands.literal("to")
                                .requires(Permissions.COMMAND_SKIN::check)
                                .then(Commands.argument("対象", EntityArgument.player())
                                        .executes(SkinCommand::resetSkin))))
                .then(Commands.literal("reload")
                        .executes(SkinCommand::reloadSkin)
                        .then(Commands.literal("to")
                                .requires(Permissions.COMMAND_SKIN::check)
                                .then(Commands.argument("対象", EntityArgument.player())
                                        .executes(SkinCommand::reloadSkin))));
        dispatcher.register(builder);
    }

    private static int setCustomSkinFromPlayer(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        Entity executorEntity = BrigadierUtil.isArgumentKeyExists(ctx, "対象") ? EntityArgument.getPlayer(ctx, "対象") : ctx.getSource().getEntity();
        if (!(executorEntity instanceof ServerPlayer executorPlayer)) {
            NewMessageUtil.sendErrorMessage(ctx.getSource(), "プレイヤーが実行する必要があります。");
            return 1;
        }

        String skinPlayerName = StringArgumentType.getString(ctx, "プレイヤー名");
        OfflinePlayer skinPlayer = Bukkit.getOfflinePlayer(skinPlayerName);

        PlayerSkinRepository skinData = SkinManager.getPlayerSkinRepository(skinPlayer.getUniqueId());
        if (skinData == null) {
            NewMessageUtil.sendErrorMessage(ctx.getSource(), "データの取得中にエラーが発生しました");
            return 2;
        }
        Skin skin = skinData.getOriginalSkin();
        if (skin != null) {
            SkinManager.getPlayerSkinRepository(executorPlayer.getUUID()).setCustomSkin(skin);
            if (executorEntity.equals(ctx.getSource().getEntity())) {
                NewMessageUtil.sendMessage(ctx.getSource(), skinPlayerName + " のスキンに変更しました");
                return 0;
            } else {
                NewMessageUtil.sendMessage(ctx.getSource(), executorPlayer.getName().getString() + " のスキンを " + skinPlayerName + " のスキンに変更しました");
                NewMessageUtil.sendMessage(executorPlayer, ctx.getSource().getDisplayName().getString() + " によってスキンが " + skinPlayerName + " のスキンが変更されました");
                return 0;
            }
        } else {
            NewMessageUtil.sendErrorMessage(ctx.getSource(), "スキンを変更できませんでした");
            return 3;
        }
    }

    private static int setCustomSkinFromString(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        Entity executorEntity = BrigadierUtil.isArgumentKeyExists(ctx, "対象") ? EntityArgument.getPlayer(ctx, "対象") : ctx.getSource().getEntity();
        if (!(executorEntity instanceof ServerPlayer executorPlayer)) {
            NewMessageUtil.sendErrorMessage(ctx.getSource(), "プレイヤーが実行する必要があります。");
            return 1;
        }

        String base64 = StringArgumentType.getString(ctx, "base64");
        String signature = StringArgumentType.getString(ctx, "signature");

        Skin skin = new Skin(SkinSource.CUSTOM, base64, signature);
        PlayerSkinRepository executorSkinRepository = SkinManager.getPlayerSkinRepository(executorPlayer.getUUID());
        if (executorSkinRepository != null) {
            executorSkinRepository.setCustomSkin(skin);
            if (executorEntity.equals(ctx.getSource().getEntity())) {
                NewMessageUtil.sendMessage(ctx.getSource(), "スキンを変更しました");
                return 0;
            } else {
                NewMessageUtil.sendMessage(ctx.getSource(), executorPlayer.getName().getString() + " のスキンを変更しました");
                NewMessageUtil.sendMessage(executorPlayer, ctx.getSource().getDisplayName().getString() + " によってスキンが変更されました");
                return 0;
            }
        } else {
            NewMessageUtil.sendErrorMessage(ctx.getSource(), "スキンを変更できませんでした");
            return 2;
        }
    }

    private static int resetSkin(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        Entity executorEntity = BrigadierUtil.isArgumentKeyExists(ctx, "対象") ? EntityArgument.getPlayer(ctx, "対象") : ctx.getSource().getEntity();
        if (!(executorEntity instanceof ServerPlayer executorPlayer)) {
            NewMessageUtil.sendErrorMessage(ctx.getSource(), "プレイヤーが実行する必要があります。");
            return 1;
        }

        PlayerSkinRepository skinRepository = SkinManager.getPlayerSkinRepository(executorPlayer.getUUID());
        if (skinRepository != null) {
            if (skinRepository.getCustomSkin() != null) {
                skinRepository.setCustomSkin(null);
                if (executorEntity.equals(ctx.getSource().getEntity())) {
                    NewMessageUtil.sendMessage(ctx.getSource(), "スキンをリセットしました");
                    return 0;
                } else {
                    NewMessageUtil.sendMessage(ctx.getSource(), executorPlayer.getName().getString() + " のスキンをリセットしました");
                    NewMessageUtil.sendMessage(executorPlayer, ctx.getSource().getDisplayName().getString() + " によってスキンがリセットされました");
                    return 0;
                }
            } else {
                if (executorEntity.equals(ctx.getSource().getEntity())) {
                    NewMessageUtil.sendErrorMessage(ctx.getSource(), "スキンはすでにオリジナルのものに設定されています");
                    return 2;
                } else {
                    NewMessageUtil.sendErrorMessage(ctx.getSource(), executorPlayer.getName().getString() + " のスキンはすでにオリジナルのものに設定されています");
                    return 2;
                }
            }
        } else {
            NewMessageUtil.sendErrorMessage(ctx.getSource(), "データの取得に失敗しました");
            return 3;
        }
    }

    private static int reloadSkin(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        Entity executorEntity = BrigadierUtil.isArgumentKeyExists(ctx, "対象") ? EntityArgument.getPlayer(ctx, "対象") : ctx.getSource().getEntity();
        if (!(executorEntity instanceof ServerPlayer executorPlayer)) {
            NewMessageUtil.sendErrorMessage(ctx.getSource(), "プレイヤーが実行する必要があります。");
            return 1;
        }

        PlayerSkinRepository skinRepository = SkinManager.getPlayerSkinRepository(executorPlayer.getUUID());
        if (skinRepository != null) {
            if (!skinRepository.getRemoteSkin().equals(skinRepository.getOriginalSkin())) {
                skinRepository.setCustomSkin(null);
                if (executorEntity.equals(ctx.getSource().getEntity())) {
                    NewMessageUtil.sendMessage(ctx.getSource(), "スキンを再読み込みしました");
                    return 0;
                } else {
                    NewMessageUtil.sendMessage(ctx.getSource(), executorPlayer.getName().getString() + " のスキンを再読み込みしました");
                    NewMessageUtil.sendMessage(executorPlayer, ctx.getSource().getDisplayName().getString() + " によってスキンが再読み込みされました");
                    return 0;
                }
            } else {
                if (executorEntity.equals(ctx.getSource().getEntity())) {
                    NewMessageUtil.sendErrorMessage(ctx.getSource(), "すでに最新のスキンを使用しています。");
                    return 2;
                } else {
                    NewMessageUtil.sendErrorMessage(ctx.getSource(), executorPlayer.getName().getString() + " はすでに最新のスキンを使用しています。");
                    return 2;
                }
            }
        } else {
            NewMessageUtil.sendErrorMessage(ctx.getSource(), "データの取得に失敗しました");
            return 2;
        }
    }
}
