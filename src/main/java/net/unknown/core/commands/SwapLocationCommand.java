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
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.core.PositionImpl;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetActionBarTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

public class SwapLocationCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        LiteralArgumentBuilder<CommandSourceStack> builder = LiteralArgumentBuilder.literal("swaplocation");
        builder.requires(source -> source.hasPermission(2));
        builder.then(Commands.argument("target", EntityArgument.entity())
                        .executes(ctx -> execute(ctx.getSource(), ctx.getSource().getEntityOrException(), EntityArgument.getEntity(ctx, "target")))
                        .then(Commands.argument("destination", EntityArgument.entity())
                                .executes(ctx -> execute(ctx.getSource(), EntityArgument.getEntity(ctx, "target"), EntityArgument.getEntity(ctx, "destination")))));
        dispatcher.register(builder);
    }

    private static int execute(CommandSourceStack source, Entity a, Entity b) {
        a.teleportTo((ServerLevel) b.level(), new PositionImpl(b.position().x(), b.position().y(), b.position().z()));
        if (a instanceof ServerPlayer player) player.connection.send(new ClientboundSetActionBarTextPacket(Component.empty().append(b.getName()).append(" と場所が入れ替わりました")));
        b.teleportTo((ServerLevel) a.level(), new PositionImpl(a.position().x(), a.position().y(), a.position().z()));
        if (b instanceof ServerPlayer player) player.connection.send(new ClientboundSetActionBarTextPacket(Component.empty().append(a.getName()).append(" と場所が入れ替わりました")));
        if (a.equals(source.getEntity()) || b.equals(source.getEntity())) {
            source.sendSuccess(() -> Component.empty().append(a.equals(source.getEntity()) ? b.getName() : a.getName()).append(" と場所を入れ替えました"), true);
        } else {
            source.sendSuccess(() -> Component.empty().append(a.getName()).append(" と ").append(b.getName()).append(" の場所を交換しました"), true);
        }
        return 0;
    }
}
