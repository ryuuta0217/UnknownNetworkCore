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

import com.ryuuta0217.packets.forge.v4.ModVersions;
import com.ryuuta0217.util.MinecraftPacketReader;
import com.velocitypowered.api.event.connection.PluginMessageEvent;
import com.velocitypowered.api.event.connection.PreLoginEvent;
import com.velocitypowered.api.proxy.InboundConnection;
import com.velocitypowered.api.proxy.Player;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import net.unknown.shared.fml.ModClientInformation;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class ForgePlayer extends ModdedPlayer implements ModdedHandshakeProcessor {
    private String logPrefix;
    private InboundConnection connection;
    private Player player;
    private ModVersions mods;
    private Map<String, String> channels;
    private Map<String, String> registries;

    private int currentPhase = 0;

    public ModVersions getMods() {
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
        event.setResult(PluginMessageEvent.ForwardResult.handled());

        if (event.getSource() instanceof Player player) {
            ByteBuf buf = Unpooled.wrappedBuffer(event.getData());

            switch (this.currentPhase) {
                case 0 -> {
                    String brand = MinecraftPacketReader.readString(buf, 32767);
                    LOGGER.info(this.logPrefix + "  -> Brand " + brand + " received (phase " + this.currentPhase + ")");
                    this.currentPhase++;
                    player.sendPluginMessage(ForgeListener.FORGE_HANDSHAKE_IDENTIFIER, this.createModVersions());
                    LOGGER.info(this.logPrefix + " <-  ModVersions sent");
                }
                case 1 -> {
                    int phase = MinecraftPacketReader.readVarInt(buf);
                    this.mods = ModVersions.decode(buf);
                    LOGGER.info(this.logPrefix + "  -> ModVersions reply received with mods " + this.mods.mods().keySet() + " (phase " + this.currentPhase + ")");

                    if (this.currentPhase == phase) {
                        this.currentPhase++;
                        player.sendPluginMessage(ForgeListener.FORGE_HANDSHAKE_IDENTIFIER, this.createChannelVersions());
                        LOGGER.info(this.logPrefix + " <-  ChannelVersions sent");
                    }
                }
                case 2 -> {
                    int phase = MinecraftPacketReader.readVarInt(buf);
                    int channelCount = MinecraftPacketReader.readVarInt(buf);
                    Map<String, String> channels = new HashMap<>();
                    for (int i = 0; i < channelCount; i++) {
                        String channelName = MinecraftPacketReader.readString(buf, 32767);
                        int channelVersion = MinecraftPacketReader.readVarInt(buf);
                        channels.put(channelName, String.valueOf(channelVersion));
                    }
                    this.channels = Collections.unmodifiableMap(channels);
                    LOGGER.info(this.logPrefix + "  -> ChannelVersions reply received with channels " + channels.keySet() + " (phase " + this.currentPhase + ")");

                    if (this.currentPhase == phase) { // unused?
                        this.currentPhase++;
                        player.sendPluginMessage(ForgeListener.FORGE_HANDSHAKE_IDENTIFIER, this.createRegistryList());
                        LOGGER.info(this.logPrefix + " <-  RegistryList sent");
                    }
                }
                case 3 -> {
                    int identifier = MinecraftPacketReader.readVarInt(buf); // always 0
                    int token = MinecraftPacketReader.readVarInt(buf); // 0 .. registry size
                    LOGGER.info(this.logPrefix + "  -> RegistryList reply received with token " + token + " (phase " + this.currentPhase + ")");

                    ForgeListener.ESTABLISHING_MODDED_PLAYERS.remove(player.getUsername());
                    ForgeListener.MODDED_PLAYERS.put(player.getUsername(), this);
                }
            }
        }
    }

    @Override
    public void onPreLogin(PreLoginEvent event) {
        this.logPrefix = "[" + event.getUsername() + "|" + event.getConnection().getRemoteAddress() + "]";

        LOGGER.info(this.logPrefix + "  -> Connected as using FORGE protocol");
        LOGGER.info(this.logPrefix + "  -  Initializing FORGE Handshake");
    }

    private byte[] createModVersions() { // phase 1
        ByteBuf buf = Unpooled.buffer();

        // Write phase
        MinecraftPacketReader.writeVarInt(this.currentPhase, buf);

        // <modid, <name, version>>
        Map<String, ModVersions.Info> serverInstalledModsDummy = new HashMap<>() {{
            put("minecraft", new ModVersions.Info("Minecraft", "1.20.4"));
            put("forge", new ModVersions.Info("Forge", "49.0.14"));
            put("networkdebugger", new ModVersions.Info("Network Debugger", "1.0"));
        }};

        LOGGER.info(this.logPrefix + "  -  Initializing ModVersions with mods " + serverInstalledModsDummy.keySet());
        new ModVersions(serverInstalledModsDummy).encode(buf);

        return ByteBufUtil.getBytes(buf);
    }

    private byte[] createChannelVersions() { // phase 2
        ByteBuf buf = Unpooled.buffer();

        // Write phase
        MinecraftPacketReader.writeVarInt(this.currentPhase, buf);

        // <channel, version>
        Map<String, Integer> serverChannelsDummy = new HashMap<>() {{
            put("minecraft:register", 0);
            put("minecraft:unregister", 0);
            put("forge:login", 0);
            put("forge:handshake", 0);
            put("forge:tier_sorting", 1);
        }};

        LOGGER.info(this.logPrefix + "  -  Initializing ChannelVersions with channels " + serverChannelsDummy.keySet());

        MinecraftPacketReader.writeVarInt(serverChannelsDummy.size(), buf);
        serverChannelsDummy.forEach((k, v) -> {
            MinecraftPacketReader.writeString(k, buf);
            MinecraftPacketReader.writeVarInt(v, buf);
        });

        return ByteBufUtil.getBytes(buf);
    }

    private byte[] createRegistryList() { // phase 3
        ByteBuf buf = Unpooled.buffer();

        // Write phase
        MinecraftPacketReader.writeVarInt(this.currentPhase, buf);

        // Write token (start is 0)
        MinecraftPacketReader.writeVarInt(0, buf);

        // <normal registry name>
        Set<String> serverNormalRegistriesDummy = new HashSet<>();
        MinecraftPacketReader.writeVarInt(serverNormalRegistriesDummy.size(), buf);
        serverNormalRegistriesDummy.forEach(s -> MinecraftPacketReader.writeString(s, buf));

        // <datapack registry name>
        Set<String> serverDataPackRegistriesDummy = new HashSet<>();
        MinecraftPacketReader.writeVarInt(serverDataPackRegistriesDummy.size(), buf);
        serverDataPackRegistriesDummy.forEach(s -> MinecraftPacketReader.writeString(s, buf));

        return ByteBufUtil.getBytes(buf);
    }

    private byte[] createRegistryData() { // phase 4
        ByteBuf buf = Unpooled.buffer();

        // Write phase
        MinecraftPacketReader.writeVarInt(this.currentPhase, buf);

        // <registry name, registry data>
        Map<String, ByteBuf> serverRegistryDataDummy = new HashMap<>();
        MinecraftPacketReader.writeVarInt(serverRegistryDataDummy.size(), buf);
        serverRegistryDataDummy.forEach((k, v) -> {
            MinecraftPacketReader.writeString(k, buf);
            buf.writeBytes(v);
        });

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

        MinecraftPacketReader.writeVarInt(this.channels.size(), buf);
        this.channels.forEach((k, v) -> {
            MinecraftPacketReader.writeString(k, buf);
            MinecraftPacketReader.writeString(v, buf);
        });
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
