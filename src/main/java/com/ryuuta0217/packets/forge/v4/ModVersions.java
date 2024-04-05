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

package com.ryuuta0217.packets.forge.v4;

import com.ryuuta0217.packets.Packet;
import com.ryuuta0217.util.MinecraftPacketReader;
import io.netty.buffer.ByteBuf;

import java.util.HashMap;
import java.util.Map;

public record ModVersions(Map<String, Info> mods) implements Packet {
    public static ModVersions decode(ByteBuf buf) {
        int modCount = MinecraftPacketReader.readVarInt(buf);
        HashMap<String, Info> mods = new HashMap<>(modCount);
        for (int i = 0; i < modCount; i++) {
            mods.put(MinecraftPacketReader.readString(buf), new Info(MinecraftPacketReader.readString(buf), MinecraftPacketReader.readString(buf)));
        }
        return new ModVersions(mods);
    }

    public void encode(ByteBuf out) {
        MinecraftPacketReader.writeVarInt(this.mods.size(), out);
        this.mods.forEach((k, v) -> {
            MinecraftPacketReader.writeString(k, out);

            MinecraftPacketReader.writeString(v.name(), out);
            MinecraftPacketReader.writeString(v.version(), out);
        });
    }

    public record Info(String name, String version) {}
}
