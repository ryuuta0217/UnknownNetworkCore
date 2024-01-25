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

package net.unknown.proxy;

import net.md_5.bungee.api.ServerPing;
import net.md_5.bungee.api.event.ProxyPingEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.config.Configuration;
import net.md_5.bungee.event.EventHandler;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class PingListener implements Listener {
    private static final String PROTOCOL_NAME;
    private static final int BASE_SUPPORTED_PROTOCOL_NUMBER;
    private static final Set<Integer> SUPPORTED_PROTOCOL_NUMBERS;

    static {
        Configuration config = UnknownNetworkProxyCore.getConfig();
        if (!config.contains("protocol-name")) {
            config.set("protocol-name", "Minecraft 1.20.x");
        }

        if (!config.contains("supported-protocol-numbers")) {
            config.set("supported-protocol-numbers", List.of(763, 764, 765)); // 1.20.2, 1.20.3, 1.20.4
        }

        PROTOCOL_NAME = config.getString("protocol-name");

        List<Integer> protocolNumbers = config.getIntList("supported-protocol-numbers");
        BASE_SUPPORTED_PROTOCOL_NUMBER = protocolNumbers.stream().min(Integer::compareTo).orElse(763); // if failed to get min, use default (defined default)
        protocolNumbers.remove(BASE_SUPPORTED_PROTOCOL_NUMBER);
        SUPPORTED_PROTOCOL_NUMBERS = new HashSet<>(protocolNumbers);
    }

    @EventHandler
    public void onPing(ProxyPingEvent event) {
        ServerPing sp = event.getResponse();

        int playerProtocolVersion = event.getConnection().getVersion();
        int protocolVersion = BASE_SUPPORTED_PROTOCOL_NUMBER;
        if (SUPPORTED_PROTOCOL_NUMBERS.contains(playerProtocolVersion)) {
            protocolVersion = playerProtocolVersion;
        }
        sp.setVersion(new ServerPing.Protocol(PROTOCOL_NAME, protocolVersion));

        event.setResponse(sp);
    }
}
