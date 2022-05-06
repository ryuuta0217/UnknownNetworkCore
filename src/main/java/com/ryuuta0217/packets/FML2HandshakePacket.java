/*
 * Copyright (c) 2022 Unknown Network Developers and contributors.
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

package com.ryuuta0217.packets;

import com.ryuuta0217.util.MinecraftPacketReader;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

public class FML2HandshakePacket {
    private static final byte MAGIC = 1;
    private final List<String> mods;
    private final Map<String, String> channels;
    private final Map<String, String> registries;

    public FML2HandshakePacket(List<String> mods, Map<String, String> channels, Map<String, String> registries) {
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
        List<String> mods = new ArrayList<>();
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
        List<String> mods = new ArrayList<>();
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

    public List<String> getMods() {
        return this.mods;
    }

    public Map<String, String> getChannels() {
        return this.channels;
    }

    public Map<String, String> getRegistries() {
        return this.registries;
    }
}
