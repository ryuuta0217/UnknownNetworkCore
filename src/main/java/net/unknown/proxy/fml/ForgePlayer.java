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

import com.ryuuta0217.packets.forge.v4.ChannelVersions;
import com.ryuuta0217.packets.forge.v4.ForgePayload;
import com.ryuuta0217.packets.forge.v4.ModVersions;
import com.ryuuta0217.util.MinecraftPacketReader;
import com.velocitypowered.api.event.connection.PluginMessageEvent;
import com.velocitypowered.api.event.connection.PreLoginEvent;
import com.velocitypowered.api.event.player.PlayerClientBrandEvent;
import com.velocitypowered.api.proxy.InboundConnection;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.messages.ChannelIdentifier;
import com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import net.unknown.shared.fml.ModClientInformation;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class ForgePlayer extends ModdedPlayer implements ModdedHandshakeProcessor {
    public static final ChannelIdentifier FORGE_HANDSHAKE_IDENTIFIER = MinecraftChannelIdentifier.create("forge", "handshake");
    public static final ChannelIdentifier FORGE_LOGIN_IDENTIFIER = MinecraftChannelIdentifier.create("forge", "login");
    private String logPrefix;
    private InboundConnection connection;
    private Player player;
    private ModVersions mods;
    private ChannelVersions channels;
    private Map<String, String> registries;

    private int currentPhase = 0;

    public ModVersions getMods() {
        return this.mods;
    }

    public ChannelVersions getChannels() {
        return this.channels;
    }

    public Map<String, String> getRegistries() {
        return this.registries;
    }

    @Override
    public void onClientBrandReceived(PlayerClientBrandEvent event) {
        // for only phase 0 handler
        ByteBuf buf = Unpooled.buffer();
        MinecraftPacketReader.writeString(event.getBrand(), buf);
        this.currentPhase = this.handleHandshake(this.currentPhase, event.getPlayer(), buf);
    }

    @Override
    public void onPluginMessageReceived(PluginMessageEvent event) {
        event.setResult(PluginMessageEvent.ForwardResult.handled());

        if (event.getSource() instanceof Player player) {
            this.handlePluginMessage(player, event.getIdentifier().toString(), Unpooled.wrappedBuffer(event.getData()));
        }
    }

    public void handlePluginMessage(InboundConnection connection, String channel, ByteBuf buf) {
        this.setConnection(connection);
        this.currentPhase = this.handleHandshake(this.currentPhase, this.player, buf);
    }

    @Override
    public void onPreLogin(PreLoginEvent event) {
        this.logPrefix = "[" + event.getUsername() + "|" + event.getConnection().getRemoteAddress() + "]";

        LOGGER.info(this.logPrefix + "  -> Connected as using FORGE protocol");
        LOGGER.info(this.logPrefix + "  -  Initializing FORGE Handshake");
        this.setConnection(event.getConnection());
    }

    private int handleHandshake(int handshakePhase, Player player, ByteBuf buf) {
        switch (handshakePhase) {
            case 0 -> {
                String brand = MinecraftPacketReader.readString(buf, 32767);
                LOGGER.info(this.logPrefix + "  -> Brand " + brand + " received (phase " + handshakePhase + ")");
                handshakePhase++;
                player.sendPluginMessage(FORGE_HANDSHAKE_IDENTIFIER, this.createModVersions());
                LOGGER.info(this.logPrefix + " <-  ModVersions sent");
            }
            case 1 -> {
                ForgePayload payload = ForgePayload.decode(buf);
                this.mods = ModVersions.decode(payload);
                LOGGER.info(this.logPrefix + "  -> ModVersions reply received with mods " + this.mods.mods().keySet() + " (phase " + handshakePhase + ")");

                if (handshakePhase == payload.packetId()) {
                    handshakePhase++;
                    player.sendPluginMessage(FORGE_HANDSHAKE_IDENTIFIER, this.createChannelVersions());
                    LOGGER.info(this.logPrefix + " <-  ChannelVersions sent");
                }
            }
            case 2 -> {
                ForgePayload payload = ForgePayload.decode(buf);
                this.channels = ChannelVersions.decode(payload);
                LOGGER.info(this.logPrefix + "  -> ChannelVersions reply received with channels " + this.channels.channels().keySet() + " (packet id " + handshakePhase + ")");

                ForgeListener.ESTABLISHING_MODDED_PLAYERS.remove(player.getUsername());
                ForgeListener.MODDED_PLAYERS.put(player.getUsername(), this);
            }
        }

        return handshakePhase;
    }

    private byte[] createModVersions() { // phase 1
        ByteBuf buf = Unpooled.buffer();

        // <modid, <name, version>>
        Map<String, ModVersions.Info> serverInstalledModsDummy = new HashMap<>() {{
            put("minecraft", new ModVersions.Info("Minecraft", "1.20.4"));
            put("forge", new ModVersions.Info("Forge", "49.0.14"));
            put("networkdebugger", new ModVersions.Info("Network Debugger", "1.0"));
        }};

        LOGGER.info(this.logPrefix + "  -  Initializing ModVersions with mods " + serverInstalledModsDummy.keySet());
        new ModVersions(serverInstalledModsDummy).encode().encode(buf);

        return ByteBufUtil.getBytes(buf);
    }

    private byte[] createChannelVersions() { // phase 2
        ByteBuf buf = Unpooled.buffer();

        Map<String, Integer> serverChannelsDummy = new HashMap<>() {{
            put("minecraft:register", 0);
            put("minecraft:unregister", 0);
            put("forge:login", 0);
            put("forge:handshake", 0);
            put("forge:tier_sorting", 1);
        }};

        LOGGER.info(this.logPrefix + "  -  Initializing ChannelVersions with channels " + serverChannelsDummy.keySet());
        new ChannelVersions(serverChannelsDummy).encode().encode(buf);

        return ByteBufUtil.getBytes(buf);
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
        MinecraftPacketReader.writeVarInt(4, buf);
        MinecraftPacketReader.writeUUID(this.player.getUniqueId(), buf);

        this.mods.encode(buf);
        this.channels.encode(buf);
    }

    @Override
    public int getFMLVersion() {
        return "FORGE".length();
    }

    @Override
    public ModClientInformation toModClientInformation() {
        return new ModClientInformation(this.player.getUniqueId(), this.mods, this.channels);
    }
}
