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

import com.ryuuta0217.packets.FML2HandshakePacket;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import sun.misc.Unsafe;

import java.lang.reflect.Field;
import java.util.*;

public class Debug {
    public static void main(String[] args) {
        FML2HandshakePacket packet = new FML2HandshakePacket(new ArrayList<>() {{
            add("minecraft");
            add("packetdebugger");
            add("forge");
        }}, new HashMap<>() {{
            put("fml:loginwrapper", "FML2");
            put("forge:tier_sorting", "1.0");
            put("fml:handshake", "FML2");
            put("minecraft:unregister", "FML2");
            put("fml:play", "FML2");
            put("minecraft:register", "FML2");
            put("forge:split", "1.1");
        }}, new HashMap<>() {{
            put("minecraft:block", null);
            put("minecraft:fluid", null);
            put("minecraft:item", null);
            put("minecraft:mob_effect", null);
            put("minecraft:sound_event", null);
            put("minecraft:potion", null);
            put("minecraft:enchantment", null);
            put("minecraft:entity_type", null);
            put("minecraft:block_entity_type", null);
            put("minecraft:particle_type", null);
            put("minecraft:menu", null);
            put("minecraft:motive", null);
            put("minecraft:recipe_serializer", null);
            put("minecraft:stat_type", null);
            put("minecraft:villager_profession", null);
            put("minecraft:data_serializers", null);
        }});

        ByteBuf buf = Unpooled.buffer();
        buf.writeByte(1 & 0xff);
        buf = packet.encodeS2C(buf);

        /*
        Mods: [minecraft, packetdebugger, forge]

Channels: {fml:loginwrapper=FML2, forge:tier_sorting=1.0, fml:handshake=FML2, minecraft:unregister=FML2, fml:play=FML2, minecraft:register=FML2, forge:split=1.1}

Registries: [minecraft:villager_profession, minecraft:data_serializers]
         */

        System.out.println(Arrays.toString(ByteBufUtil.getBytes(buf)));
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
