/*
 * Copyright (c) 2022 Unknown Network Developers and contributors.
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

package net.unknown.core.commands.vanilla;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.Util;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameRules;
import net.unknown.core.enums.Permissions;
import net.unknown.core.util.BrigadierUtil;
import org.bukkit.GameMode;

import java.util.Collection;
import java.util.Collections;

// /<gamemode|gm> <survival|s|0|creative|c|1|adventure|a|2|spectator|3|sp>
public class GamemodeCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        // FORCE UNREGISTER minecraft:gamemode COMMAND FROM COMMAND DISPATCHER
        BrigadierUtil.forceUnregisterCommand("gamemode");
        // FORCE UNREGISTER END

        for (String command : new String[] {"gamemode", "gm"}) {
            LiteralArgumentBuilder<CommandSourceStack> builder = Commands.literal(command);
            builder.requires(Permissions.COMMAND_GAMEMODE::check);

            for (String mode : new String[] {"survival", "s", "0", "creative", "c", "1", "adventure", "a", "2", "spectator", "sp", "3"}) {
                builder.then(Commands.literal(mode)
                        .executes(ctx -> setMode(ctx, getGameModeFromString(mode), Collections.singletonList((ServerPlayer) ctx.getSource().getEntity())))
                        .then(Commands.argument("targets", EntityArgument.players())
                                .requires(source -> source.hasPermission(2, Permissions.COMMAND_GAMEMODE.getPermissionNode() + ".other"))
                                .executes(ctx -> setMode(ctx, getGameModeFromString(mode), EntityArgument.getPlayers(ctx, "targets")))));
            }

            dispatcher.register(builder);
        }

        for (String mode : new String[] {"s", "c", "a", "sp"}) {
            LiteralArgumentBuilder<CommandSourceStack> builder = LiteralArgumentBuilder.literal("gm" + mode);
            builder.requires(clw -> clw.hasPermission(2) || clw.getBukkitSender().hasPermission("minecraft.command.gm" + mode)); // TODO プレイヤー指定対応
            builder.executes(ctx -> setMode(ctx, getGameModeFromString(mode), Collections.singleton(ctx.getSource().getPlayerOrException())))
                    .then(Commands.argument("targets", EntityArgument.players())
                            .requires(source -> source.hasPermission(2, "minecraft.command.gm" + mode + ".other"))
                            .executes(ctx -> setMode(ctx, getGameModeFromString(mode), EntityArgument.getPlayers(ctx, "targets"))));
            dispatcher.register(builder);
        }
    }

    public static int setMode(CommandContext<CommandSourceStack> ctx, GameMode gameMode, Collection<ServerPlayer> players) {
        players.forEach(player -> {
            if (player.getBukkitEntity().getGameMode() != gameMode) {
                player.getBukkitEntity().setGameMode(gameMode);
                if (player.getBukkitEntity().getGameMode() != gameMode) {
                    ctx.getSource().sendFailure(new TextComponent("Failed to set the gamemode of '" + player.getName() + "'"));
                } else {
                    logGamemodeChange(ctx.getSource(), player, gameMode);
                }
            }
        });
        return 1;
    }

    public static void logGamemodeChange(CommandSourceStack source, ServerPlayer player, GameMode gameMode) {
        Component gameModeTranslation = new TranslatableComponent("gameMode." + gameMode.name().toLowerCase());

        if (source.getEntity() == player) {
            // 実行者と対象が一致する場合は、自身のゲームモードを変更したことを <source> に通知する
            source.sendSuccess(new TranslatableComponent("commands.gamemode.success.self", gameModeTranslation), true);
        } else {
            //　実行者と対象が一致しない
            if (source.getLevel().getGameRules().getBoolean(GameRules.RULE_SENDCOMMANDFEEDBACK)) {
                // gameRule commandFeedbackがtrue
                player.sendMessage(new TranslatableComponent("gameMode.changed", gameModeTranslation), Util.NIL_UUID);
            }

            // 他のプレイヤーのゲームモードを変更したことを <source> に通知する
            source.sendSuccess(new TranslatableComponent("commands.gamemode.success.other", player.getScoreboardName(), gameModeTranslation), true);
        }
    }

    /**
     * 文字列をGameModeEnumに変換します
     *
     * @param gameMode gamemode string
     * @return gamemode enum
     */
    private static GameMode getGameModeFromString(String gameMode) {
        if (gameMode.matches("(survival|s|0)")) {
            return GameMode.SURVIVAL;
        } else if (gameMode.matches("(creative|c|1)")) {
            return GameMode.CREATIVE;
        } else if (gameMode.matches("(adventure|a|2)")) {
            return GameMode.ADVENTURE;
        } else {
            return GameMode.SPECTATOR;
        }
    }
}
