/*
 * Copyright (c) 2023 Unknown Network Developers and contributors.
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

import com.ryuuta0217.util.MinecraftPacketReader;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import sun.misc.Unsafe;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

public class Debug {
    public static void main(String[] args) {
        byte[] bytes = new byte[]{2, 5, 11, 102, 111, 114, 103, 101, 58, 108, 111, 103, 105, 110, 0, 20, 109, 105, 110, 101, 99, 114, 97, 102, 116, 58, 117, 110, 114, 101, 103, 105, 115, 116, 101, 114, 0, 15, 102, 111, 114, 103, 101, 58, 104, 97, 110, 100, 115, 104, 97, 107, 101, 0, 18, 102, 111, 114, 103, 101, 58, 116, 105, 101, 114, 95, 115, 111, 114, 116, 105, 110, 103, 1, 18, 109, 105, 110, 101, 99, 114, 97, 102, 116, 58, 114, 101, 103, 105, 115, 116, 101, 114, 0};
        ByteBuf buf = Unpooled.wrappedBuffer(bytes);
        int phase = MinecraftPacketReader.readVarInt(buf);
        int elementsCount = MinecraftPacketReader.readVarInt(buf);
        Map<String, Integer> elements = new HashMap<>();
        for (int i = 0; i < elementsCount; i++) {
            String element = MinecraftPacketReader.readString(buf, 32767);
            int elementLength = MinecraftPacketReader.readVarInt(buf);
            elements.put(element, elementLength);
        }
        System.out.println("Phase: " + phase);
        System.out.println("Elements count: " + elementsCount);
        System.out.println("Elements: " + elements);
    }

    public static Unsafe getUnsafe() {
        Field f = null;
        try {
            f = Unsafe.class.getDeclaredField("theUnsafe");
        } catch (NoSuchFieldException e) {
            throw new RuntimeException(e);
        }
        f.trySetAccessible();
        try {
            return (Unsafe) f.get(null);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }
}
