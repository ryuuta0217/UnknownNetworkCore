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

public record ForgePayload(int packetId, ByteBuf data) {
    public static ForgePayload decode(ByteBuf buf) {
        int packetId = MinecraftPacketReader.readVarInt(buf);
        ByteBuf data = buf.readBytes(buf.readableBytes());
        return new ForgePayload(packetId, data);
    }

    public void encode(ByteBuf buf) {
        MinecraftPacketReader.writeVarInt(this.packetId, buf);
        buf.writeBytes(this.data);
    }
}
