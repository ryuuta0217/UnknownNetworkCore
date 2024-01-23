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

package net.unknown.shared.fml;

import com.ryuuta0217.packets.Packet;
import com.ryuuta0217.packets.forge.v2.FML2HandshakePacket;
import com.ryuuta0217.packets.forge.v4.ModVersions;
import com.ryuuta0217.util.MinecraftPacketReader;
import io.netty.buffer.ByteBuf;
import net.unknown.shared.enums.ConnectionEnvironment;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

public record ModClientInformation(ConnectionEnvironment version, UUID uniqueId, Map<String, Map.Entry<String, String>> mods, Map<String, String> channels) implements Packet {
    public ModClientInformation(UUID uniqueId, FML2HandshakePacket fml2) {
        this(ConnectionEnvironment.FML2, uniqueId, fml2.getMods().stream().collect(Collectors.toMap(modId -> modId, modId -> Map.entry(modId, "0.0"))), fml2.getChannels());
    }

    public ModClientInformation(UUID uniqueId, ModVersions fml4, Map<String, String> channels) {
        this(ConnectionEnvironment.FORGE, uniqueId, fml4.mods().entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, entry -> Map.entry(entry.getKey(), entry.getValue().version()))), channels);
    }

    public static ModClientInformation decode(ByteBuf buf) {
        ConnectionEnvironment version = ConnectionEnvironment.valueOf(MinecraftPacketReader.readString(buf));
        UUID uniqueId = MinecraftPacketReader.readUUID(buf);
        int modCount = MinecraftPacketReader.readVarInt(buf);
        Map<String, Map.Entry<String, String>> mods = new HashMap<>();
        for (int i = 0; i < modCount; i++) {
            String modId = MinecraftPacketReader.readString(buf);
            String modVersion = MinecraftPacketReader.readString(buf);
            String modFileName = MinecraftPacketReader.readString(buf);
            mods.put(modId, Map.entry(modVersion, modFileName));
        }
        int channelCount = MinecraftPacketReader.readVarInt(buf);
        Map<String, String> channels = new HashMap<>();
        for (int i = 0; i < channelCount; i++) {
            String channelName = MinecraftPacketReader.readString(buf);
            String channelVersion = MinecraftPacketReader.readString(buf);
            channels.put(channelName, channelVersion);
        }
        return new ModClientInformation(version, uniqueId, mods, channels);
    }

    @Override
    public void encode(ByteBuf buf) {
        MinecraftPacketReader.writeString(this.version.name(), buf);
        MinecraftPacketReader.writeUUID(this.uniqueId, buf);
        MinecraftPacketReader.writeVarInt(this.mods.size(), buf);
        this.mods.forEach((id, entry) -> {
            MinecraftPacketReader.writeString(id, buf);
            MinecraftPacketReader.writeString(entry.getKey(), buf);
            MinecraftPacketReader.writeString(entry.getValue(), buf);
        });
        MinecraftPacketReader.writeVarInt(this.channels.size(), buf);
        this.channels.forEach((id, version) -> {
            MinecraftPacketReader.writeString(id, buf);
            MinecraftPacketReader.writeString(version, buf);
        });
    }
}
