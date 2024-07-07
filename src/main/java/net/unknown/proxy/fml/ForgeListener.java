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

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.event.connection.PluginMessageEvent;
import com.velocitypowered.api.event.connection.PreLoginEvent;
import com.velocitypowered.api.event.player.ServerConnectedEvent;
import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.api.proxy.InboundConnection;
import com.velocitypowered.api.proxy.LoginPhaseConnection;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.messages.ChannelIdentifier;
import com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;

import java.lang.reflect.Field;
import java.util.*;

public class ForgeListener {
    public static final Map<String, ModdedPlayer> MODDED_PLAYERS = new HashMap<>();
    public static final Map<String, ModdedHandshakeProcessor> ESTABLISHING_MODDED_PLAYERS = new HashMap<>();
    public static final ChannelIdentifier FORGE_HANDSHAKE_IDENTIFIER = MinecraftChannelIdentifier.create("forge", "handshake");

    @Subscribe
    public void onPluginMessageReceived(PluginMessageEvent event) {
        if (event.getSource() instanceof Player player) {
            if (ESTABLISHING_MODDED_PLAYERS.containsKey(player.getUsername())) {
                ESTABLISHING_MODDED_PLAYERS.get(player.getUsername()).onPluginMessageReceived(event);
            }
        }
    }

    @Subscribe
    public void onPreLogin(PreLoginEvent event) {
        if (event.getConnection().getProtocolVersion().greaterThan(ProtocolVersion.MINECRAFT_1_13)) {
            ModdedHandshakeProcessor player = null;

            String extraDataInHandshake = getExtraDataInHandshake(event.getConnection());
            if (extraDataInHandshake.contains("FML2")) {
                player = new FML2Player();
            } else if (extraDataInHandshake.contains("FORGE")) {
                player = new ForgePlayer();
            }

            if (player != null) {
                ESTABLISHING_MODDED_PLAYERS.put(event.getUsername(), player);
                player.onPreLogin(event);
            }
        }
    }

    @Subscribe
    public void onDisconnect(DisconnectEvent event) {
        ESTABLISHING_MODDED_PLAYERS.remove(event.getPlayer().getUsername());
        MODDED_PLAYERS.remove(event.getPlayer().getUsername());
    }

    @Subscribe
    public void onConnectedToServer(ServerConnectedEvent event) {
        if (MODDED_PLAYERS.containsKey(event.getPlayer().getUsername())) {
            ModdedPlayer fp = MODDED_PLAYERS.get(event.getPlayer().getUsername());
            fp.setPlayer(event.getPlayer() instanceof Player connection ? connection : null);
            if (fp.getPlayer() != null) {
                ByteBuf buf = Unpooled.buffer();
                fp.toModClientInformation().encode(buf);
                event.getServer().sendPluginMessage(MinecraftChannelIdentifier.create("unknown", "forge"), ByteBufUtil.getBytes(buf));
            }
        }
    }

    private static String getExtraDataInHandshake(InboundConnection connection) {
        if (connection instanceof LoginPhaseConnection) {
            try {
                Field delegateField = connection.getClass().getDeclaredField("delegate");
                if (delegateField.trySetAccessible()) {
                    Object initialInboundConnection = delegateField.get(connection);
                    Field handshakeField = initialInboundConnection.getClass().getDeclaredField("handshake");
                    if (handshakeField.trySetAccessible()) {
                        Object handshakePacket = handshakeField.get(initialInboundConnection);
                        Field serverAddressField = handshakePacket.getClass().getDeclaredField("serverAddress");
                        if (serverAddressField.trySetAccessible()) {
                            String serverAddress = (String) serverAddressField.get(handshakePacket);
                            String[] splitAddress = serverAddress.split("\0", 2);
                            return splitAddress.length == 2 ? splitAddress[1] : "";
                        }
                    }
                }
            } catch(NoSuchFieldException | IllegalAccessException e) {
                e.printStackTrace();
            }
        } else {
            throw new IllegalArgumentException("Supports only PreLogin phase connection.");
        }
        return "";
    }
}
