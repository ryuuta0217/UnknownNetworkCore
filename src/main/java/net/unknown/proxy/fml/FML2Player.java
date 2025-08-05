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

import com.ryuuta0217.packets.forge.v2.C2SModListReply;
import com.ryuuta0217.packets.forge.v2.FML2HandshakePacket;
import com.ryuuta0217.packets.forge.v2.S2CModList;
import com.ryuuta0217.util.MinecraftPacketReader;
import com.velocitypowered.api.event.connection.PluginMessageEvent;
import com.velocitypowered.api.event.connection.PreLoginEvent;
import com.velocitypowered.api.proxy.InboundConnection;
import com.velocitypowered.api.proxy.LoginPhaseConnection;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import net.unknown.shared.fml.ModClientInformation;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class FML2Player extends ModdedPlayer implements ModdedHandshakeProcessor {
    private InboundConnection connection;
    private Player player;
    private Set<String> mods;
    private Map<String, String> channels;
    private Map<String, String> registries;

    public Set<String> getMods() {
        return this.mods;
    }

    public Map<String, String> getChannels() {
        return this.channels;
    }

    public Map<String, String> getRegistries() {
        return this.registries;
    }

    @Override
    public void onPluginMessageReceived(PluginMessageEvent event) {
        // unused
    }

    @Override
    public void onPreLogin(PreLoginEvent event) {
        this.setConnection(event.getConnection());

        String logPrefix = "[" + event.getUsername() + "|" + event.getConnection().getRemoteAddress() + "]";

        LOGGER.info(logPrefix + "  -> Connected as using FML2 protocol");
        LOGGER.info(logPrefix + "  -  Initializing FML2 Handshake (S2CModList)");

        S2CModList packet = new S2CModList(new HashSet<>() {{
            add("minecraft");
            add("forge");
        }}, new HashMap<>() {{
            put("forge:tier_sorting", "1.0");
        }}, new HashSet<>());
        ByteBuf outgoingBuf = Unpooled.buffer();
        packet.encode(outgoingBuf);

        if (event.getConnection() instanceof LoginPhaseConnection loginConnection) {
            loginConnection.sendLoginPluginMessage(MinecraftChannelIdentifier.create("fml", "handshake"), ByteBufUtil.getBytes(outgoingBuf), (response) -> {
                ByteBuf incomingBuf = Unpooled.wrappedBuffer(response != null ? response : new byte[0]);

                try {
                    C2SModListReply handshake = C2SModListReply.decode(incomingBuf);
                    this.mods = handshake.getMods();
                    this.channels = handshake.getChannels();
                    this.registries = handshake.getRegistries();

                    ForgeListener.ESTABLISHING_MODDED_PLAYERS.remove(event.getUsername());
                    ForgeListener.MODDED_PLAYERS.put(event.getUsername(), this);
                    LOGGER.info(logPrefix + " <-> Successfully FML2 handshake completed");
                    LOGGER.info(logPrefix + " <-> Connected as using mods: " + handshake.getMods());
                } catch (IllegalArgumentException e) {
                    e.printStackTrace();
                }
            });
            LOGGER.info(logPrefix + " <-  LoginPayloadRequest (FML2 Handshake S2CModList) Sent");
        }
    }

    @Override
    public void setPlayer(Player player) {
        this.player = player;
    }

    @Override
    public Player getPlayer() {
        return this.player;
    }

    @Override
    public void setConnection(InboundConnection connection) {
        this.connection = connection;
        if (connection instanceof Player player) this.setPlayer(player);
    }

    @NotNull
    @Override
    public InboundConnection getConnection() {
        return this.connection;
    }

    @Override
    public void getData(ByteBuf buf) {
        if (this.player == null) throw new IllegalStateException("Player is not set");
        MinecraftPacketReader.writeUUID(this.player.getUniqueId(), buf);
        new C2SModListReply(this.mods, this.channels, this.registries).encode(buf);
    }

    @Override
    public int getFMLVersion() {
        return 2;
    }

    @Override
    public ModClientInformation toModClientInformation() {
        return new ModClientInformation(this.player.getUniqueId(), new FML2HandshakePacket(this.mods, this.channels, this.registries));
    }
}
