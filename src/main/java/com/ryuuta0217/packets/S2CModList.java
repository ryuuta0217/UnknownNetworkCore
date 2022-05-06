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
        if(magic != MAGIC) throw new IllegalArgumentException("Invalid discriminator byte " + magic);

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