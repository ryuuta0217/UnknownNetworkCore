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

public class S2CModList {
    private static final byte MAGIC = 1;

    private final Set<String> mods;
    private final Map<String, String> channels;
    private final Set<String> registries;

    public S2CModList(Set<String> mods, Map<String, String> channels, Set<String> registries) {
        this.mods = mods;
        this.channels = channels;
        this.registries = registries;
    }

    public static S2CModList decode(ByteBuf buf) {
        short magic = buf.readUnsignedByte();
        if (magic != MAGIC) throw new IllegalArgumentException("Invalid discriminator byte " + magic);

        int count = MinecraftPacketReader.readVarInt(buf);
        Set<String> mods = new HashSet<>();
        IntStream.range(0, count).forEach(i -> mods.add(MinecraftPacketReader.readString(buf, 256)));

        count = MinecraftPacketReader.readVarInt(buf);
        Map<String, String> channels = new HashMap<>();
        IntStream.range(0, count).forEach(i -> channels.put(MinecraftPacketReader.readString(buf, 32767), MinecraftPacketReader.readString(buf, 256)));

        count = MinecraftPacketReader.readVarInt(buf);
        Set<String> registries = new HashSet<>();
        IntStream.range(0, count).forEach(i -> registries.add(MinecraftPacketReader.readString(buf, 32767)));

        return new S2CModList(mods, channels, registries);
    }

    public ByteBuf encode(ByteBuf buf) {
        buf.writeByte(MAGIC);
        MinecraftPacketReader.writeVarInt(this.mods.size(), buf);
        this.mods.forEach(mod -> MinecraftPacketReader.writeString(mod, buf));

        MinecraftPacketReader.writeVarInt(this.channels.size(), buf);
        this.channels.forEach((identifier, version) -> {
            MinecraftPacketReader.writeString(identifier, buf);
            MinecraftPacketReader.writeString(version, buf);
        });

        MinecraftPacketReader.writeVarInt(this.registries.size(), buf);
        this.registries.forEach(registry -> MinecraftPacketReader.writeString(registry, buf));

        return buf;
    }
}