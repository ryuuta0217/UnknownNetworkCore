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

package net.unknown.core.packet;

import io.netty.channel.*;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ServerGamePacketListener;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.FilteredText;
import net.minecraft.server.network.TextFilter;
import net.unknown.core.packet.event.PacketReceivedEvent;
import net.unknown.core.packet.event.PacketSendingEvent;
import net.unknown.core.packet.listener.IncomingPacketListener;
import net.unknown.core.packet.listener.OutgoingPacketListener;
import net.unknown.core.util.ReflectionUtil;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.CompletableFuture;

public class PacketManager implements Listener {
    private static final Logger LOGGER = LoggerFactory.getLogger("UNC/PacketManager");
    private static final String PACKET_HANDLER_NAME_FORMAT = "UNC_PacketHandler#%player%";
    private static final PacketManager INSTANCE = new PacketManager();
    private final Map<String, Set<IncomingPacketListener<?>>> REGISTERED_INCOMING_C2S_LISTENERS = new HashMap<>();
    private final Map<String, Set<OutgoingPacketListener<?>>> REGISTERED_OUTGOING_S2C_LISTENERS = new HashMap<>();

    protected PacketManager() {
    }

    public static PacketManager getInstance() {
        return INSTANCE;
    }

    public <P extends Packet<ServerGamePacketListener>> void registerIncomingC2SListener(Class<P> packetClass, IncomingPacketListener<P> listener) {
        if (!REGISTERED_INCOMING_C2S_LISTENERS.containsKey(packetClass.getName())) {
            REGISTERED_INCOMING_C2S_LISTENERS.put(packetClass.getName(), new HashSet<>());
        }

        REGISTERED_INCOMING_C2S_LISTENERS.get(packetClass.getName()).add(listener);
    }

    public <P extends Packet<ServerGamePacketListener>> boolean unregisterIncomingC2SListener(Class<P> packetClass, IncomingPacketListener<P> listener) {
        if (REGISTERED_INCOMING_C2S_LISTENERS.containsKey(packetClass.getName())) {
            Set<IncomingPacketListener<?>> listeners = REGISTERED_INCOMING_C2S_LISTENERS.get(packetClass.getName());
            if (listeners.contains(listener)) {
                listeners.remove(listener);
                return true;
            }
        }
        return false;
    }

    public <P extends Packet<ClientGamePacketListener>> void registerOutgoingS2CListener(Class<P> packetClass, OutgoingPacketListener<P> listener) {
        if (!REGISTERED_OUTGOING_S2C_LISTENERS.containsKey(packetClass.getName())) {
            REGISTERED_OUTGOING_S2C_LISTENERS.put(packetClass.getName(), new HashSet<>());
        }

        REGISTERED_OUTGOING_S2C_LISTENERS.get(packetClass.getName()).add(listener);
    }

    public <P extends Packet<ClientGamePacketListener>> boolean unregisterOutgoingS2CListener(Class<P> packetClass, OutgoingPacketListener<P> listener) {
        if (REGISTERED_OUTGOING_S2C_LISTENERS.containsKey(packetClass.getName())) {
            Set<OutgoingPacketListener<?>> listeners = REGISTERED_OUTGOING_S2C_LISTENERS.get(packetClass.getName());
            if (listeners.contains(listener)) {
                listeners.remove(listener);
                return true;
            }
        }
        return false;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerLogin(PlayerLoginEvent event) {
        if (event.getResult() != PlayerLoginEvent.Result.ALLOWED) return;
        ServerPlayer player = ((CraftPlayer) event.getPlayer()).getHandle();
        TextFilter oTextFilter = player.getTextFilter();
        WrappedTextFilter wTextFilter = new WrappedTextFilter(oTextFilter) {
            @Override
            public void join() {
                ChannelDuplexHandler packetHandler = new ChannelDuplexHandler() {
                    @Override
                    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
                        if (msg instanceof Packet packet) {
                            PacketSendingEvent event = new PacketSendingEvent(player, packet);
                            if (REGISTERED_OUTGOING_S2C_LISTENERS.containsKey(packet.getClass().getName())) {
                                REGISTERED_OUTGOING_S2C_LISTENERS.get(packet.getClass().getName()).forEach(listener -> {
                                    if (listener.isIgnoreCancelled() && event.isCancelled()) return;
                                    listener.onSendingPacket(event);
                                });
                            }
                            if (event.isCancelled()) return;
                            super.write(ctx, event.getPacket(), promise);
                            return;
                        }
                        super.write(ctx, msg, promise);
                    }

                    @Override
                    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
                        //long start = System.nanoTime();
                        if (msg instanceof Packet packet) {
                            PacketReceivedEvent event = new PacketReceivedEvent(player, packet);
                            if (REGISTERED_INCOMING_C2S_LISTENERS.containsKey(packet.getClass().getName())) {
                                REGISTERED_INCOMING_C2S_LISTENERS.get(packet.getClass().getName()).forEach(listener -> {
                                    if (listener.isIgnoreCancelled() && event.isCancelled()) return;
                                    listener.onPacketReceived(event);
                                });
                            }
                            if (event.isCancelled()) return;
                            super.channelRead(ctx, event.getPacket());
                            return;
                        } else {
                            if (!msg.getClass().isSynthetic()) LOGGER.info("PacketManager detected unknown instance packet: " + msg.getClass().getName());
                        }
                        // long end = System.nanoTime();
                        // LOGGER.info("PacketManager took " + (end - start) + "ns (" + (end - start) / 1000000 + "ms) to handle packet!");
                        // on Listeners non-registered: took 3000-5000 ns, in this injection code.

                        super.channelRead(ctx, msg);
                    }
                };
                ChannelPipeline pipeline = player.connection.connection.channel.pipeline();
                pipeline.addBefore("packet_handler", PacketManager.getPacketHandlerName(event.getPlayer().getName()), packetHandler);
                oTextFilter.join();
            }
        };

        try {
            ReflectionUtil.setFinalObject(ServerPlayer.class.getDeclaredField("textFilter"), player, wTextFilter);
        } catch (NoSuchFieldException e) {
            LOGGER.warn("Failed to inject TextFilter into player {}. PacketListener will not work for this player.", player.getName());
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Channel c = ((CraftPlayer) event.getPlayer()).getHandle().connection.connection.channel;
        c.eventLoop().submit(() -> c.pipeline().remove(PacketManager.getPacketHandlerName(event.getPlayer().getName())));
    }

    private static String getPacketHandlerName(String playerName) {
        return PACKET_HANDLER_NAME_FORMAT.replace("%player%", playerName);
    }

    private static class WrappedTextFilter implements TextFilter {
        private final TextFilter textFilter;

        public WrappedTextFilter(TextFilter textFilter) {
            this.textFilter = textFilter;
        }

        @Override
        public void join() {
            this.textFilter.join();
        }

        @Override
        public void leave() {
            this.textFilter.leave();
        }

        @Override
        public CompletableFuture<FilteredText> processStreamMessage(String s) {
            return this.textFilter.processStreamMessage(s);
        }

        @Override
        public CompletableFuture<List<FilteredText>> processMessageBundle(List<String> list) {
            return this.textFilter.processMessageBundle(list);
        }

        @Override
        public int hashCode() {
            return this.textFilter.hashCode();
        }

        @Override
        public boolean equals(Object obj) {
            return this.textFilter.equals(obj);
        }

        @Override
        public String toString() {
            return this.textFilter.toString();
        }
    }
}
