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
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.unknown.core.enums.Permissions;
import net.unknown.core.feature.HideArmors;
import net.unknown.core.util.NewMessageUtil;

public class HideArmorsCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        LiteralArgumentBuilder<CommandSourceStack> builder = LiteralArgumentBuilder.literal("hidearmors");

        LiteralArgumentBuilder<CommandSourceStack> setNode = Commands.literal("set");
        for (HideArmors.Mode mode : HideArmors.Mode.values()) {
            setNode.then(Commands.literal(mode.name().toLowerCase())
                    .executes(ctx -> setMode(ctx, mode)));
        }

        builder.requires(Permissions.COMMAND_HIDEARMORS::checkAndIsPlayer)
                .executes(HideArmorsCommand::showMode)
                .then(setNode);

        dispatcher.register(builder);
    }

    private static int showMode(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        HideArmors.Mode currentMode = HideArmors.getInstance().getMode(ctx.getSource().getPlayerOrException().getUUID());
        NewMessageUtil.sendMessage(ctx.getSource(), "HideArmors は " + currentMode + " に設定されています");
        return currentMode.ordinal();
    }

    private static int setMode(CommandContext<CommandSourceStack> ctx, HideArmors.Mode newMode) throws CommandSyntaxException {
        HideArmors.Mode currentMode = HideArmors.getInstance().getMode(ctx.getSource().getPlayerOrException().getUUID());
        if (currentMode == newMode) {
            NewMessageUtil.sendMessage(ctx.getSource(), "HideArmors は既に " + newMode + " に設定されています");
            return currentMode.ordinal();
        } else {
            HideArmors.getInstance().enable(ctx.getSource().getPlayerOrException().getBukkitEntity(), newMode);
            NewMessageUtil.sendMessage(ctx.getSource(), "HideArmors の設定を " + newMode + " に変更しました");
            return newMode.ordinal();
        }
    }
}
