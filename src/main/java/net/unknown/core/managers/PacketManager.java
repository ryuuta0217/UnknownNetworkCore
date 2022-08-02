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

package net.unknown.core.managers;

import io.netty.channel.Channel;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPipeline;
import net.minecraft.network.protocol.Packet;
import net.minecraft.server.level.ServerPlayer;
import net.unknown.core.events.PacketReceivedEvent;
import net.unknown.core.events.interfaces.PacketListener;
import org.bukkit.craftbukkit.v1_19_R1.entity.CraftPlayer;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class PacketManager implements Listener {
    private static final Logger LOGGER = LoggerFactory.getLogger("UNC/PacketManager");
    private static final PacketManager INSTANCE = new PacketManager();
    private final Map<String, Set<PacketListener>> REGISTERED_LISTENERS = new HashMap<>();

    protected PacketManager() {
    }

    public static PacketManager getInstance() {
        return INSTANCE;
    }

    public void registerListener(Class<? extends Packet<?>> packetClass, PacketListener listener) {
        if (!REGISTERED_LISTENERS.containsKey(packetClass.getName())) {
            REGISTERED_LISTENERS.put(packetClass.getName(), new HashSet<>());
        }

        REGISTERED_LISTENERS.get(packetClass.getName()).add(listener);
    }

    public boolean unregisterListener(Class<? extends Packet<?>> packetClass, PacketListener listener) {
        if (REGISTERED_LISTENERS.containsKey(packetClass.getName())) {
            Set<PacketListener> listeners = REGISTERED_LISTENERS.get(packetClass.getName());
            if (listeners.contains(listener)) {
                listeners.remove(listener);
                return true;
            }
        }
        return false;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        ServerPlayer player = ((CraftPlayer) event.getPlayer()).getHandle();
        ChannelDuplexHandler packetHandler = new ChannelDuplexHandler() {
            @Override
            public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
                //long start = System.nanoTime();
                if (msg instanceof Packet packet) {
                    PacketReceivedEvent event = new PacketReceivedEvent(player, packet);
                    if (REGISTERED_LISTENERS.containsKey(packet.getClass().getName())) {
                        REGISTERED_LISTENERS.get(packet.getClass().getName()).forEach(listener -> {
                            if (listener.isIgnoreCancelled() && event.isCancelled()) return;
                            listener.onPacketReceived(event);
                        });
                    }
                    if (event.isCancelled()) return;
                } else {
                    LOGGER.info("PacketManager detected unknown instance packet: " + msg.getClass().getName());
                }
                // long end = System.nanoTime();
                // LOGGER.info("PacketManager took " + (end - start) + "ns (" + (end - start) / 1000000 + "ms) to handle packet!");
                // on Listeners non-registered: took 3000-5000 ns, in this injection code.

                super.channelRead(ctx, msg);
            }
        };
        ChannelPipeline pipeline = player.connection.connection.channel.pipeline();
        pipeline.addBefore("packet_handler", player.getGameProfile().getName(), packetHandler);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Channel c = ((CraftPlayer) event.getPlayer()).getHandle().connection.getConnection().channel;
        c.eventLoop().submit(() -> c.pipeline().remove(event.getPlayer().getName()));
    }
}
