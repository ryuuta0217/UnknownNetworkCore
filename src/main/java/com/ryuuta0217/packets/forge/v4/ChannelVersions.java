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

import com.ryuuta0217.util.MinecraftPacketReader;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.unknown.proxy.NetworkDirection;

import java.util.HashMap;
import java.util.Map;

public record ChannelVersions(Map<String, Integer> channels) {
    public static final int PACKET_ID = ModVersions.PACKET_ID + 1; // maybe; 2

    public static ChannelVersions decode(ByteBuf buf) {
        int channelCount = MinecraftPacketReader.readVarInt(buf);
        Map<String, Integer> channels = new HashMap<>(channelCount);
        for (int i = 0; i < channelCount; i++) {
            channels.put(MinecraftPacketReader.readString(buf), MinecraftPacketReader.readVarInt(buf));
        }
        return new ChannelVersions(channels);
    }

    public static ChannelVersions decode(ForgePayload payload) {
        if (payload.packetId() != PACKET_ID) throw new IllegalStateException("Invalid packet ID: " + payload.packetId());
        return decode(payload.data());
    }

    public void encode(ByteBuf out) {
        MinecraftPacketReader.writeVarInt(this.channels.size(), out);
        this.channels.forEach((k, v) -> {
            MinecraftPacketReader.writeString(k, out);
            MinecraftPacketReader.writeVarInt(v, out);
        });
    }

    public ForgePayload encode() {
        ByteBuf buf = Unpooled.buffer();
        encode(buf);
        return new ForgePayload(PACKET_ID, buf);
    }
}
