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
import io.netty.buffer.ByteBufUtil;
import io.netty.handler.codec.DecoderException;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

/**
 * @see net.md_5.bungee.protocol.DefinedPacket
 */
public class MinecraftPacketReader {
    private static boolean hasContinuationBit(byte b) {
        return (b & 128) == 128;
    }

    public static int readVarInt(ByteBuf input) {
        int value = 0;
        int bytes = 0;

        byte currentByte;
        do {
            currentByte = input.readByte();
            value |= (currentByte & 127) << bytes++ * 7;
            if (bytes > 5) {
                throw new RuntimeException("VarInt too big");
            }
        } while(hasContinuationBit(currentByte));

        return value;
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

    public static String readUtf(ByteBuf input) {
        return readUtf(input, 32767);
    }

    public static String readUtf(ByteBuf input, int maxLen) {
        int maxBytes = ByteBufUtil.utf8MaxBytes(maxLen);
        int bytes = readVarInt(input);

        if (bytes > maxBytes) {
            throw new IllegalArgumentException("Buffer length is longer then max allowed (" + bytes + " > " + maxBytes + ")");
        } else if (bytes < 0) {
            throw new IllegalArgumentException("Huh? There is not found.");
        } else {
            int readableBytes = input.readableBytes();
            if (bytes > readableBytes) {
                throw new IllegalArgumentException("Data says \"I have " + bytes + " bytes string!\" but actual data is \"" + readableBytes + " bytes!\" (readerIndex: " + input.readerIndex() + ")");
            } else {
                String str = input.toString(input.readerIndex(), bytes, StandardCharsets.UTF_8);
                input.readerIndex(input.readerIndex() + bytes);
                if (str.length() > maxLen) {
                    throw new IllegalArgumentException("Maximum length of string is " + maxLen + " but got " + str.length() + " characters.");
                } else {
                    return str;
                }
            }
        }
    }

    public static void writeVarInt(int value, ByteBuf out) {
        while ((value & -128) != 0) {
            out.writeByte(value & 127 | 128);
            value >>>= 7;
        }

        out.writeByte(value);
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

    public static void writeUtf(String str, ByteBuf out) {
        writeUtf(str, 32767, out);
    }

    public static void writeUtf(String str, int maxLen, ByteBuf out) {
        if (str.length() > maxLen) {
            throw new IllegalArgumentException("Maximum length of string is " + maxLen + " but provided string is " + str.length() + " characters.");
        } else {
            int strBytes = ByteBufUtil.utf8MaxBytes(str);
            ByteBuf buf = out.alloc().buffer(strBytes);

            try {
                int bytes = ByteBufUtil.writeUtf8(buf, str);
                int maxBytes = ByteBufUtil.utf8MaxBytes(maxLen);
                if (bytes > maxBytes) {
                    throw new IllegalArgumentException("Buffer length is longer then max allowed (" + bytes + " > " + maxBytes + ")");
                }

                writeVarInt(bytes, out);
                out.writeBytes(buf);
            } finally {
                buf.release();
            }
        }
    }
}
