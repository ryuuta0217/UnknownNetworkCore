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

package net.unknown.survival.commands.home.admin;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.server.level.ServerPlayer;
import net.unknown.core.util.BrigadierUtil;
import net.unknown.core.util.MessageUtil;
import net.unknown.survival.data.PlayerData;
import net.unknown.survival.enums.Permissions;

public class SetHomeCountCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        LiteralArgumentBuilder<CommandSourceStack> builder = LiteralArgumentBuilder.literal("sethomecount");
        builder.requires(Permissions.COMMAND_SETHOME_COUNT::check);

        builder.executes(SetHomeCountCommand::sendCurrent)
                .then(Commands.argument("対象", EntityArgument.player())
                        .executes(SetHomeCountCommand::sendCurrent)
                        .then(Commands.literal("base")
                                .then(Commands.argument("ベース個数", IntegerArgumentType.integer(-1))
                                        .executes(ctx -> {
                                            ServerPlayer player = EntityArgument.getPlayer(ctx, "対象");
                                            PlayerData.HomeData data = PlayerData.of(player).getHomeData();
                                            int newCount = IntegerArgumentType.getInteger(ctx, "ベース個数");
                                            data.setHomeBaseCount(newCount);
                                            MessageUtil.sendAdminMessage(ctx.getSource(), player.displayName + " のホーム設定上限値(Base)を " + newCount + " に設定しました");
                                            return 1;
                                        })))
                        .then(Commands.literal("additional")
                                .then(Commands.argument("追加個数", IntegerArgumentType.integer(-1))
                                        .executes(ctx -> {
                                            ServerPlayer player = EntityArgument.getPlayer(ctx, "対象");
                                            PlayerData.HomeData data = PlayerData.of(player).getHomeData();
                                            int newCount = IntegerArgumentType.getInteger(ctx, "追加個数");
                                            data.setHomeAdditionalCount(newCount);
                                            MessageUtil.sendAdminMessage(ctx.getSource(), player.displayName + " のホーム設定上限値(Additional)を " + newCount + " に設定しました");
                                            return 1;
                                        }))));

        dispatcher.register(builder);
    }

    private static int sendCurrent(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = BrigadierUtil.getArgumentOrDefault(ctx, ServerPlayer.class, "対象", ctx.getSource().getPlayerOrException());
        PlayerData.HomeData data = PlayerData.of(player).getHomeData();
        MessageUtil.sendAdminMessage(ctx.getSource(), player.displayName + " のホーム設定上限値は " + data.getHomeBaseCount() + "(base) + " + data.getHomeAdditionalCount() + "(additional) = " + data.getMaxHomeCount() + "個 です", false);
        return 1;
    }
}
