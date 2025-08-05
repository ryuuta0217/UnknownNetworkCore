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

import java.util.*;
import java.util.stream.IntStream;

public class FML2HandshakePacket {
    private static final byte MAGIC = 1;
    private final Set<String> mods;
    private final Map<String, String> channels;
    private final Map<String, String> registries;

    public FML2HandshakePacket(Set<String> mods, Map<String, String> channels, Map<String, String> registries) {
        this.mods = mods;
        this.channels = channels;
        this.registries = registries;
    }

    public static FML2HandshakePacket parseC2S(ByteBuf buf) {
        //
        //  [Client -> Server] Mod List Reply
        //
        //       [Mod Count] [VarInt]
        //                   [Mod Name] [String]
        //                   ...
        //   [Channel Count] [VarInt]
        //                   [  Name  ] [String]
        //                   [ Version] [String]
        //                   ...
        //  [Registry Count] [VarInt]
        //                   [Registry] [String]
        //                   [ Version] [String]
        //                   ...
        //
        int modCount = MinecraftPacketReader.readVarInt(buf); // [Mod Count]
        Set<String> mods = new HashSet<>();
        IntStream.range(0, modCount).forEach(i -> {
            mods.add(MinecraftPacketReader.readString(buf, 256)); // [Mod Name]
        });

        int channelCount = MinecraftPacketReader.readVarInt(buf); // [Channel Count]
        Map<String, String> channels = new HashMap<>();
        IntStream.range(0, channelCount).forEach(i -> {
            channels.put(MinecraftPacketReader.readString(buf, 32767), MinecraftPacketReader.readString(buf, 256)); // [Channel Name]
        });

        int registryCount = MinecraftPacketReader.readVarInt(buf); // [Registry Count]
        Map<String, String> registries = new HashMap<>();
        IntStream.range(0, registryCount).forEach(i -> {
            registries.put(MinecraftPacketReader.readString(buf, 32767), MinecraftPacketReader.readString(buf, 256)); // <[Registry], [Version]>
        });

        return new FML2HandshakePacket(mods, channels, registries);
    }

    public static FML2HandshakePacket parseS2C(ByteBuf buf) {
        //
        //  [Server -> Client] Mod List
        //
        //       [Mod Count] [VarInt]
        //                   [Mod Name] [String]
        //                   ...
        //   [Channel Count] [VarInt]
        //                   [  Name  ] [String]
        //                   [ Version] [String]
        //                   ...
        //  [Registry Count] [VarInt]
        //                   [Registry] [String]
        //                   ...
        //
        int modCount = MinecraftPacketReader.readVarInt(buf); // [Mod Count]
        Set<String> mods = new HashSet<>();
        IntStream.range(0, modCount).forEach(i -> {
            mods.add(MinecraftPacketReader.readString(buf, 256)); // [Mod Name]
        });

        int channelCount = MinecraftPacketReader.readVarInt(buf); // [Channel Count]
        Map<String, String> channels = new HashMap<>();
        IntStream.range(0, channelCount).forEach(i -> {
            channels.put(MinecraftPacketReader.readString(buf, 32767), MinecraftPacketReader.readString(buf, 256)); // [Channel Name]
        });

        int registryCount = MinecraftPacketReader.readVarInt(buf); // [Registry Count]
        Map<String, String> registries = new HashMap<>();
        IntStream.range(0, registryCount).forEach(i -> {
            registries.put(MinecraftPacketReader.readString(buf, 32767), null); // <[Registry], [Version]>
        });

        return new FML2HandshakePacket(mods, channels, registries);
    }

    public ByteBuf encodeC2S(ByteBuf buf) {
        //
        //  [Client -> Server] Mod List Reply
        //
        //       [Mod Count] [VarInt]
        //                   [Mod Name] [String]
        //                   ...
        //   [Channel Count] [VarInt]
        //                   [  Name  ] [String]
        //                   [ Version] [String]
        //                   ...
        //  [Registry Count] [VarInt]
        //                   [Registry] [String]
        //                   [ Version] [String]
        //                   ...
        //

        MinecraftPacketReader.writeVarInt(this.getMods().size(), buf);
        this.getMods().forEach(mod -> MinecraftPacketReader.writeString(mod, buf));

        MinecraftPacketReader.writeVarInt(this.getChannels().size(), buf);
        this.getChannels().forEach((k, v) -> {
            MinecraftPacketReader.writeString(k, buf);
            MinecraftPacketReader.writeString(v, buf);
        });

        MinecraftPacketReader.writeVarInt(this.getRegistries().size(), buf);
        this.getRegistries().forEach((k, v) -> {
            MinecraftPacketReader.writeString(k, buf);
            MinecraftPacketReader.writeString((v == null ? "unknown" : v), buf);
        });
        return buf;
    }

    public ByteBuf encodeS2C(ByteBuf buf) {
        //
        //  [Server -> Client] Mod List
        //
        //       [Mod Count] [VarInt]
        //                   [Mod Name] [String]
        //                   ...
        //   [Channel Count] [VarInt]
        //                   [  Name  ] [String]
        //                   [ Version] [String]
        //                   ...
        //  [Registry Count] [VarInt]
        //                   [Registry] [String]
        //                   ...
        //
        MinecraftPacketReader.writeVarInt(this.getMods().size(), buf);
        this.getMods().forEach(mod -> MinecraftPacketReader.writeString(mod, buf));

        MinecraftPacketReader.writeVarInt(this.getChannels().size(), buf);
        this.getChannels().forEach((k, v) -> {
            MinecraftPacketReader.writeString(k, buf);
            MinecraftPacketReader.writeString(v, buf);
        });

        MinecraftPacketReader.writeVarInt(this.getRegistries().size(), buf);
        this.getRegistries().forEach((k, v) -> {
            MinecraftPacketReader.writeString(k, buf);
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
