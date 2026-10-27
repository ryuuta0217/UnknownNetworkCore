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

package net.unknown.proxy.commands;

import com.velocitypowered.api.command.SimpleCommand;
import net.kyori.adventure.text.Component;
import net.unknown.proxy.UnknownNetworkProxyCore;

import java.util.Collections;
import java.util.List;

public class UnknownNetworkProxyCoreCommand implements SimpleCommand {
    @Override
    public void execute(Invocation invocation) {
        if (invocation.arguments().length == 0) {
            invocation.source().sendMessage(Component.text("UnknownNetworkProxyCore Command Help:"));
            invocation.source().sendMessage(Component.text("/unpc reload config - Reloads the main configuration."));
            invocation.source().sendMessage(Component.text("/unpc reload shared_config - Reloads the shared configuration."));
        } else if (invocation.arguments().length == 2 && "reload".equalsIgnoreCase(invocation.arguments()[0])) {
            String target = invocation.arguments()[1];
            switch (target.toLowerCase()) {
                case "config":
                    UnknownNetworkProxyCore.reloadConfig();
                    invocation.source().sendMessage(Component.text("Main configuration reloaded."));
                    break;
                case "shared_config":
                    UnknownNetworkProxyCore.reloadSharedConfig();
                    invocation.source().sendMessage(Component.text("Shared configuration reloaded."));
                    break;
                default:
                    invocation.source().sendMessage(Component.text("Unknown reload target: " + target));
                    break;
            }
        } else {
            invocation.source().sendMessage(Component.text("Invalid command usage. Use /unpc for help."));
        }
    }

    @Override
    public List<String> suggest(Invocation invocation) {
        if (invocation.arguments().length == 0) {
            return List.of("reload");
        } else if (invocation.arguments().length == 1 && "reload".equalsIgnoreCase(invocation.arguments()[0])) {
            return List.of("config", "shared_config");
        }

        return Collections.emptyList();
    }

    @Override
    public boolean hasPermission(Invocation invocation) {
        return invocation.source().hasPermission("unknown.proxy.command.unpc");
    }
}
