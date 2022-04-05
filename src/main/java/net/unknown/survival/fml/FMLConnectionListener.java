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

package net.unknown.survival.fml;

import com.ryuuta0217.packets.FML2HandshakePacket;
import com.ryuuta0217.util.MinecraftPacketReader;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.unknown.survival.enums.ConnectionEnvironment;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;
import java.util.logging.Logger;

public class FMLConnectionListener implements org.bukkit.plugin.messaging.PluginMessageListener {
    private static final Logger LOGGER = Logger.getLogger("SCK/ModDetector");

    @Override
    public void onPluginMessageReceived(@NotNull String s, @NotNull Player player, byte[] bytes) {
        if (s.equals("unknown:forge")) {
            ByteBuf buf = Unpooled.wrappedBuffer(bytes);

            UUID uniqueId = MinecraftPacketReader.readUUID(buf);
            if (!uniqueId.equals(player.getUniqueId())) {
                LOGGER.warning("Player " + player.getName() + " sended \"unknown:forge\" message is not valid: UUID mismatch");
                return;
            }

            FML2HandshakePacket handshake = FML2HandshakePacket.parse(buf);

            ModdedClientPlayer mcp = new ModdedClientPlayer(ConnectionEnvironment.FML2, handshake.getMods(), handshake.getChannels(), handshake.getRegistries());
            ModdedPlayerManager.addPlayer(player, mcp);

            //Bukkit.broadcast(Component.text(player.getName() + " is using mods! Installed: " + mcp.getModNames()));
        }
    }
}
