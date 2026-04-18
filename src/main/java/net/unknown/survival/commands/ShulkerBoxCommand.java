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
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.tree.LiteralCommandNode;
import net.kyori.adventure.text.Component;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.unknown.core.util.NewMessageUtil;
import net.unknown.survival.enums.Permissions;
import net.unknown.survival.feature.OpenShulkerBoxInHand;

// /<shulkerbox|sb> <how-open> <click|inventory> <mode>
public class ShulkerBoxCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        LiteralArgumentBuilder<CommandSourceStack> builder = LiteralArgumentBuilder.literal("shulkerbox");
        builder.requires(Permissions.COMMAND_SHULKERBOX::checkAndIsPlayer);

        LiteralArgumentBuilder<CommandSourceStack> clickNode = Commands.literal("click")
                .executes(ShulkerBoxCommand::showClickOpenMode);
        for (OpenShulkerBoxInHand.OpenMode mode : OpenShulkerBoxInHand.OpenMode.values()) {
            clickNode.then(Commands.literal(mode.name())
                    .executes(ctx -> setClickOpenMode(ctx, mode)));
        }

        LiteralArgumentBuilder<CommandSourceStack> inventoryNode = Commands.literal("inventory")
                .executes(ShulkerBoxCommand::showInventoryOpenMode);
        for (OpenShulkerBoxInHand.InventoryOpenMode mode : OpenShulkerBoxInHand.InventoryOpenMode.values()) {
            inventoryNode.then(Commands.literal(mode.name())
                    .executes(ctx -> setInventoryOpenMode(ctx, mode)));
        }

        builder.then(Commands.literal("how-open")
                .then(clickNode)
                .then(inventoryNode)
                .executes(ctx -> showClickOpenMode(ctx) + showInventoryOpenMode(ctx)));

        LiteralCommandNode<CommandSourceStack> node = dispatcher.register(builder);

        LiteralArgumentBuilder<CommandSourceStack> aliasBuilder = Commands.literal("sb")
                        .requires(Permissions.COMMAND_SHULKERBOX::checkAndIsPlayer)
                                .redirect(node);

        dispatcher.register(aliasBuilder);
    }

    private static int showClickOpenMode(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        OpenShulkerBoxInHand.OpenMode mode = OpenShulkerBoxInHand.getOpenMode(ctx.getSource().getPlayerOrException().getUUID());
        NewMessageUtil.sendMessage(ctx.getSource(), mode.getDescription());
        return 0;
    }

    private static int showInventoryOpenMode(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        OpenShulkerBoxInHand.InventoryOpenMode mode = OpenShulkerBoxInHand.getInventoryOpenMode(ctx.getSource().getPlayerOrException().getUUID());
        NewMessageUtil.sendMessage(ctx.getSource(), mode.getDescription());
        return 0;
    }

    private static int setClickOpenMode(CommandContext<CommandSourceStack> ctx, OpenShulkerBoxInHand.OpenMode mode) throws CommandSyntaxException {
        OpenShulkerBoxInHand.setOpenMode(ctx.getSource().getPlayerOrException().getUUID(), mode);
        NewMessageUtil.sendMessage(ctx.getSource(), mode.getModeChangedMessage());
        return 0;
    }

    private static int setInventoryOpenMode(CommandContext<CommandSourceStack> ctx, OpenShulkerBoxInHand.InventoryOpenMode mode) throws CommandSyntaxException {
        OpenShulkerBoxInHand.setInventoryOpenMode(ctx.getSource().getPlayerOrException().getUUID(), mode);
        NewMessageUtil.sendMessage(ctx.getSource(), mode.getModeChangedMessage());
        return 0;
    }
}
