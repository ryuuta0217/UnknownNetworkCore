/*
 * Copyright (c) 2026 Unknown Network Developers and contributors.
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

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.unknown.UnknownNetworkCorePlugin;
import net.unknown.core.enums.Permissions;
import net.unknown.core.util.NewMessageUtil;

public class UnknownNetworkCoreCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        LiteralArgumentBuilder<CommandSourceStack> builder = LiteralArgumentBuilder.literal("unc");
        builder.requires(Permissions.COMMAND_UNC::check);
        builder.then(Commands.literal("reload")
                .then(Commands.literal("config")
                        .executes(ctx -> {
                            UnknownNetworkCorePlugin.onConfigUnload();
                            UnknownNetworkCorePlugin.getInstance().reloadConfig();
                            UnknownNetworkCorePlugin.onConfigLoad();
                            NewMessageUtil.sendMessage(ctx.getSource(), "config.yml を再読み込みしました", true);
                            return Command.SINGLE_SUCCESS;
                        }))
                .then(Commands.literal("shared_config").executes(ctx -> {
                    UnknownNetworkCorePlugin.onConfigUnload();
                    UnknownNetworkCorePlugin.onSharedConfigUnload();
                    UnknownNetworkCorePlugin.getInstance().reloadSharedConfig();
                    UnknownNetworkCorePlugin.onConfigLoad();
                    UnknownNetworkCorePlugin.onSharedConfigLoad();
                    NewMessageUtil.sendMessage(ctx.getSource(), "shared/config.yml を再読み込みしました", true);
                    return Command.SINGLE_SUCCESS;
                })));
        dispatcher.register(builder);
    }
}
