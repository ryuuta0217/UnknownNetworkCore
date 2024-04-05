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

package com.ryuuta0217.util;

import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.DecoderException;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

/**
 * @see net.md_5.bungee.protocol.DefinedPacket
 */
public class MinecraftPacketReader {
    public static int readVarInt(ByteBuf input) {
        return readVarInt(input, 5);
    }

    public static int readVarInt(ByteBuf input, int maxBytes) {
        int out = 0;
        int bytes = 0;

        while (input.readableBytes() != 0) {
            byte in = input.readByte();
            out |= (in & 127) << bytes++ * 7;
            if (bytes > maxBytes) {
                throw new DecoderException("VarInt too big");
            }

            if ((in & 128) != 128) {
                return out;
            }
        }

        throw new DecoderException("No more bytes reading varint");
    }

    public static String readString(ByteBuf buf) {
        return readString(buf, 32767);
    }

    public static String readString(ByteBuf buf, int maxLen) {
        int len = readVarInt(buf);
        if (len > maxLen * 4) {
            throw new DecoderException(String.format("Cannot receive string longer than %d (got %d bytes)", maxLen * 4, len));
        } else {
            byte[] b = new byte[len];
            buf.readBytes(b);
            String s = new String(b, StandardCharsets.UTF_8);
            if (s.length() > maxLen) {
                throw new DecoderException(String.format("Cannot receive string longer than %d (got %d characters)", maxLen, s.length()));
            } else {
                return s;
            }
        }
    }

    public static UUID readUUID(ByteBuf input) {
        return new UUID(input.readLong(), input.readLong());
    }

    public static void writeVarInt(int value, ByteBuf out) {
        do {
            int part = value & 127;
            value >>>= 7;
            if (value != 0) {
                part |= 128;
            }

            out.writeByte(part);
        } while (value != 0);
    }

    public static void writeString(String s, ByteBuf out) {
        if (s.length() > 32767) {
            throw new IllegalArgumentException("Cannot send string longer than Short.MAX_VALUE (got " + s.length() + " characters)");
        } else {
            byte[] b = s.getBytes(StandardCharsets.UTF_8);
            writeVarInt(b.length, out);
            out.writeBytes(b);
        }
    }

    public static void writeUUID(UUID value, ByteBuf out) {
        out.writeLong(value.getMostSignificantBits());
        out.writeLong(value.getLeastSignificantBits());
    }
}
