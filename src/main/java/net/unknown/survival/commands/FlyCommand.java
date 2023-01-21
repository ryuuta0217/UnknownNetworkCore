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
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.world.level.GameType;
import net.unknown.core.util.MessageUtil;
import net.unknown.survival.enums.Permissions;
import net.unknown.survival.managers.FlightManager;

// /fly [end-time] <time>
public class FlyCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        LiteralArgumentBuilder<CommandSourceStack> builder = LiteralArgumentBuilder.literal("fly");
        builder.requires(Permissions.COMMAND_FLY::checkAndIsPlayer);

        builder.executes(ctx -> execute(ctx, -1, -1))
                .then(Commands.literal("end-time")
                        .then(Commands.argument("分", IntegerArgumentType.integer(1))
                                .executes(ctx -> execute(ctx, IntegerArgumentType.getInteger(ctx, "分"), -1))))
                .then(Commands.literal("end-price")
                        .then(Commands.argument("料金", IntegerArgumentType.integer(FlightManager.FLIGHT_PRICE_BASE + FlightManager.FLIGHT_PRICE_PER_MINUTES))
                                .executes(ctx -> execute(ctx, -1, IntegerArgumentType.getInteger(ctx, "料金")))));

        dispatcher.register(builder);
    }

    private static int execute(CommandContext<CommandSourceStack> ctx, int maxFlightTimeMinutes, int maxFlightPrice) throws CommandSyntaxException {
        GameType gameType = ctx.getSource().getPlayerOrException().gameMode.getGameModeForPlayer();
        if (gameType != GameType.SURVIVAL && gameType != GameType.ADVENTURE) {
            MessageUtil.sendErrorMessage(ctx.getSource(), "サバイバルまたはアドベンチャーモードの時にのみ利用できます。");
            return -1;
        }

        if (FlightManager.isFlightEnabled(ctx.getSource().getPlayerOrException())) {
            FlightManager.Result result = FlightManager.disableFlight(ctx.getSource().getPlayerOrException(), FlightManager.EndReason.SELF_END);
            if (result != FlightManager.Result.SUCCESS) {
                MessageUtil.sendErrorMessage(ctx.getSource(), "何らかの理由により、有料飛行を無効にできませんでした");
            }
        } else {
            FlightManager.Result result = FlightManager.enableFlight(ctx.getSource().getPlayerOrException(), maxFlightTimeMinutes, maxFlightPrice);
            if (result == FlightManager.Result.FAILED) {
                MessageUtil.sendErrorMessage(ctx.getSource(), "何らかの理由により、有料飛行を有効にできませんでした");
            } else if (result == FlightManager.Result.EMPTY_BALANCE) {
                MessageUtil.sendErrorMessage(ctx.getSource(), "有料飛行を利用するには、所持金が不足しています。最低でも " + (FlightManager.FLIGHT_PRICE_BASE + FlightManager.FLIGHT_PRICE_PER_MINUTES) + "円 必要です");
            }
        }
        return 0;
    }
}
