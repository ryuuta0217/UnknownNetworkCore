/*
 * Copyright (C) 2023 Ryuta Iwakura (ryuuta0217)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation version 3 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.ryuuta0217.packets.forge.v2;

import com.ryuuta0217.util.MinecraftPacketReader;
import io.netty.buffer.ByteBuf;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.IntStream;

public class C2SModListReply {
    public static final byte PACKET_ID = 2;

    private final Set<String> mods;
    private final Map<String, String> channels;
    private final Map<String, String> registries;

    public C2SModListReply(Set<String> mods, Map<String, String> channels, Map<String, String> registries) {
        this.mods = mods;
        this.channels = channels;
        this.registries = registries;
    }

    public static C2SModListReply decode(ByteBuf buf) {
        short magic = buf.readUnsignedByte();
        if (magic != PACKET_ID) throw new IllegalArgumentException("Invalid discriminator byte " + magic);

        int count = MinecraftPacketReader.readVarInt(buf);
        Set<String> mods = new HashSet<>();
        IntStream.range(0, count).forEach(i -> mods.add(MinecraftPacketReader.readString(buf, 256)));

        count = MinecraftPacketReader.readVarInt(buf);
        Map<String, String> channels = new HashMap<>();
        IntStream.range(0, count).forEach(i -> channels.put(MinecraftPacketReader.readString(buf, 32767), MinecraftPacketReader.readString(buf, 256)));

        count = MinecraftPacketReader.readVarInt(buf);
        Map<String, String> registries = new HashMap<>();
        IntStream.range(0, count).forEach(i -> registries.put(MinecraftPacketReader.readString(buf, 32767), MinecraftPacketReader.readString(buf, 256)));

        return new C2SModListReply(mods, channels, registries);
    }

    public ByteBuf encode(ByteBuf buf) {
        buf.writeByte(PACKET_ID);
        MinecraftPacketReader.writeVarInt(this.mods.size(), buf);
        this.mods.forEach(mod -> MinecraftPacketReader.writeString(mod, buf));

        MinecraftPacketReader.writeVarInt(this.channels.size(), buf);
        this.channels.forEach((identifier, version) -> {
            MinecraftPacketReader.writeString(identifier, buf);
            MinecraftPacketReader.writeString(version, buf);
        });

        MinecraftPacketReader.writeVarInt(this.registries.size(), buf);
        this.registries.forEach((registry, version) -> {
            MinecraftPacketReader.writeString(registry, buf);
            MinecraftPacketReader.writeString(version, buf);
        });

        return buf;
    }

    public Set<String> getMods() {
        return this.mods;
    }

    public Map<String, String> getChannels() {
        return this.channels;
    }

    public Map<String, String> getRegistries() {
        return this.registries;
    }
}