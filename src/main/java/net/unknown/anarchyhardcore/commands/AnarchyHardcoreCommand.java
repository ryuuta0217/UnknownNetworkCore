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

package net.unknown.anarchyhardcore.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.tree.LiteralCommandNode;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.unknown.anarchyhardcore.UnknownNetworkAnarchyHardcore;
import net.unknown.anarchyhardcore.ban.BanData;

import java.util.Collections;
import java.util.Optional;

// /<anarchyhardcore|ahc> <unban> <player|ip>
public class AnarchyHardcoreCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        LiteralArgumentBuilder<CommandSourceStack> builder = LiteralArgumentBuilder.literal("anarchyhardcore");
        builder.then(Commands.literal("unban")
                .then(Commands.argument("target", StringArgumentType.greedyString())
                        .suggests((ctx, sb) -> SharedSuggestionProvider.suggest(UnknownNetworkAnarchyHardcore.getBanData().stream().map(BanData::target).map(Object::toString), sb))
                        .executes(ctx -> execute(ctx, StringArgumentType.getString(ctx, "target")))));

        LiteralCommandNode<CommandSourceStack> node = dispatcher.register(builder);
        LiteralArgumentBuilder<CommandSourceStack> aliasBuilder = LiteralArgumentBuilder.literal("ahc");
        aliasBuilder.redirect(node);
        dispatcher.register(aliasBuilder);
    }

    private static int execute(CommandContext<CommandSourceStack> ctx, String target) {
        Optional<BanData<?>> banData = UnknownNetworkAnarchyHardcore.getBanData().stream()
                .filter(data -> data.target().toString().equals(target))
                .findFirst();

        if (banData.isPresent()) {
            UnknownNetworkAnarchyHardcore.getBanData().remove(banData.get());
            ctx.getSource().sendSuccess(() -> Component.literal("Unbanned " + target), true);
            return 0;
        } else {
            ctx.getSource().sendFailure(Component.literal("No ban data found for " + target));
            return -1;
        }
    }
}
