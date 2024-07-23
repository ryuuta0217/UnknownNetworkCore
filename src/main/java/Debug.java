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

import com.ryuuta0217.packets.forge.v4.ModVersions;
import com.ryuuta0217.util.MinecraftPacketReader;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.unknown.proxy.NetworkDirection;
import sun.misc.Unsafe;

import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class Debug {
    public static void main(String[] args) {
        /*byte[] bytes = new byte[]{
                1, // packet id?
                15, // string length
                102, 111, 114, 103, 101, 58, 104, 97, 110, 100, 115, 104, 97, 107, 101, // forge:handshake
                1, // packet id (2nd?)
                3, // mods count
                9, // string length
                109, 105, 110, 101, 99, 114, 97, 102, 116, // minecraft
                9, // string length
                77, 105, 110, 101, 99, 114, 97, 102, 116, // Minecraft
                4, // string length
                49, 46, 50, 49, // 1.21
                5, 102, 111, 114, 103, 101, 5, 70, 111, 114, 103, 101, 7, 53, 49, 46, 48, 46, 50, 50, 12, 102, 109, 108, 104, 97, 110, 100, 115, 104, 97, 107, 101, 12, 70, 77, 76, 72, 97, 110, 100, 115, 104, 97, 107, 101, 5, 49, 46, 48, 46, 48};
        */
        byte[] bytes = new byte[] {
                1, // packet id?
                3, // mod count
                9, // string length
                109, 105, 110, 101, 99, 114, 97, 102, 116, // minecraft
                9, // string length
                77, 105, 110, 101, 99, 114, 97, 102, 116, // Minecraft
                4, 49, 46, 50, 49, 5, 102, 111, 114, 103, 101, 5, 70, 111, 114, 103, 101, 7, 53, 49, 46, 48, 46, 50, 50, 12, 102, 109, 108, 104, 97, 110, 100, 115, 104, 97, 107, 101, 12, 70, 77, 76, 72, 97, 110, 100, 115, 104, 97, 107, 101, 5, 49, 46, 48, 46, 48
        };

        System.out.println("Input: " + new String(bytes, StandardCharsets.UTF_8));
        ByteBuf buf = Unpooled.wrappedBuffer(bytes);
        int packetId = MinecraftPacketReader.readVarInt(buf);
        if (packetId == 1) {
            ModVersions modVersions = ModVersions.decode(buf);
            System.out.println(modVersions);
        }
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
