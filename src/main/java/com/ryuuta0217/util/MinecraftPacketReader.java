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
}
