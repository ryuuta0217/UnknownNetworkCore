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

package net.unknown.proxy.fml;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import net.md_5.bungee.UserConnection;
import net.md_5.bungee.api.event.PlayerDisconnectEvent;
import net.md_5.bungee.api.event.PluginMessageEvent;
import net.md_5.bungee.api.event.PreLoginEvent;
import net.md_5.bungee.api.event.ServerConnectedEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;
import net.md_5.bungee.protocol.ProtocolConstants;
import net.unknown.proxy.ModdedInitialHandler;

import java.util.*;

public class ForgeListener implements Listener {
    public static final Map<String, ModdedHandshakeProcessor> ESTABLISHING_MODDED_PLAYERS = new HashMap<>();

    @EventHandler
    public void onPluginMessageReceived(PluginMessageEvent event) {
        if (event.getSender() instanceof UserConnection connection) { // from client
            if (ESTABLISHING_MODDED_PLAYERS.containsKey(connection.getName())) {
                ESTABLISHING_MODDED_PLAYERS.get(connection.getName()).onPluginMessageReceived(event);
            }
        }
    }

    @EventHandler
    public void onPreLogin(PreLoginEvent event) {
        if (!(event.getConnection() instanceof ModdedInitialHandler handler)) return;

        if (handler.getHandshake().getProtocolVersion() >= ProtocolConstants.MINECRAFT_1_13) {
            ModdedHandshakeProcessor player = null;

            if (handler.getExtraDataInHandshake().contains("FML2")) {
                player = new FML2Player();
            } else if (handler.getExtraDataInHandshake().contains("FORGE")) {
                player = new ForgePlayer();
            }

            if (player != null) {
                ESTABLISHING_MODDED_PLAYERS.put(handler.getName(), player);
                player.onPreLogin(event);
            }
        }
    }

    @EventHandler
    public void onDisconnect(PlayerDisconnectEvent event) {
        ESTABLISHING_MODDED_PLAYERS.remove(event.getPlayer().getName());
        ModdedInitialHandler.FORGE_PLAYERS.remove(event.getPlayer().getName());
    }

    @EventHandler
    public void onConnectedToServer(ServerConnectedEvent event) {
        if (ModdedInitialHandler.FORGE_PLAYERS.containsKey(event.getPlayer().getName())) {
            ModdedPlayer fp = ModdedInitialHandler.FORGE_PLAYERS.get(event.getPlayer().getName());
            fp.setProxiedPlayer(event.getPlayer() instanceof UserConnection connection ? connection : null);
            if (fp.getProxiedPlayer() != null) {
                ByteBuf buf = Unpooled.buffer();
                fp.toModClientInformation().encode(buf);
                event.getServer().sendData("unknown:forge", ByteBufUtil.getBytes(buf));
            }
        }
    }
}
