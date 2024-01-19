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

package net.unknown.shared.messaging;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.md_5.bungee.Util;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.PluginMessageEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;
import net.unknown.Environment;
import net.unknown.UnknownNetworkCore;
import net.unknown.UnknownNetworkCorePlugin;
import net.unknown.proxy.UnknownNetworkProxyCore;
import org.bukkit.Bukkit;

import javax.annotation.Nullable;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * UnknownNetworkCore 独自のメッセージを処理・管理するクラスです。
 * 主に、PluginMessageで使用します。
 * Bukkit / Bungeecord の両方の環境から呼び出されることに注意すること！
 */
public class UNCMessaging {
    public static final String CHANNEL = "unknown-network:messaging";

    private static final Map<String, ArrayList<Map.Entry<String, MessageProcessor<?, ?>>>> PROCESSORS = new HashMap<>();
    private static final Map<String, ArrayList<MessageListener<? extends Packet>>> LISTENERS = new HashMap<>();

    public static void initBukkit() {
        if (UnknownNetworkCore.getEnvironment() != Environment.PROXY && UnknownNetworkCore.getEnvironment() != Environment.STANDALONE) {
            Bukkit.getMessenger().registerIncomingPluginChannel(UnknownNetworkCorePlugin.getInstance(), CHANNEL, (channel, player, message) -> {
                if (channel.equals(CHANNEL)) {
                    dispatchMessage(Side.BUKKIT, player.getUniqueId(), message);
                }
            });

            Bukkit.getMessenger().registerOutgoingPluginChannel(UnknownNetworkCorePlugin.getInstance(), CHANNEL);
        } else {
            throw new IllegalStateException("UnknownNetworkCore is not running on Bukkit!");
        }
    }

    public static void initBungeeCord() {
        if (UnknownNetworkCore.getEnvironment() == Environment.PROXY) {
            UnknownNetworkProxyCore.getInstance().getProxy().registerChannel(CHANNEL);
            UnknownNetworkProxyCore.getInstance().getProxy().getPluginManager().registerListener(UnknownNetworkProxyCore.getInstance(), new Listener() {
                @EventHandler
                public void onMessage(PluginMessageEvent event) {
                    if (event.getTag().equals(CHANNEL)) {
                        dispatchMessage(Side.BUNGEECORD, event.getReceiver() instanceof ProxiedPlayer p ? p.getUniqueId() : null, event.getData());
                    }
                }
            });
        } else {
            throw new IllegalStateException("UnknownNetworkCore is not running on BungeeCord!");
        }
    }

    public static <S, T> void registerProcessor(String channel, String id, MessageProcessor<S, T> processor) {
        if (!PROCESSORS.containsKey(channel)) {
            PROCESSORS.put(channel, new ArrayList<>());
        }

        PROCESSORS.get(channel).add(Map.entry(id, processor));
    }

    public static void unregisterProcessor(String channel, String id) {
        if (PROCESSORS.containsKey(channel)) {
            PROCESSORS.get(channel).removeIf(entry -> entry.getKey().equals(id));
        }

        if (PROCESSORS.get(channel).isEmpty()) {
            PROCESSORS.remove(channel);
        }
    }

    // [Packet Specifications]
    // Data Layout: [Type (short), length? (int), body (*)] ...
    // types = 0 (string), 1 (uuid), 2 (int), 3 (long), 4 (short), 5 (byte), 6 (float), 7 (double), 8 (boolean)
    //
    // All Packets Layout: Channel (string), Body (*)
    public static void dispatchMessage(Side side, @Nullable UUID player, byte[] message) {
        ByteBuf buf = Unpooled.wrappedBuffer(message);
        short type = buf.readShort();

        if (type == 0) {
            int length = buf.readInt();
            byte[] bytes = new byte[length];
            buf.readBytes(bytes);
            String channel = new String(bytes, StandardCharsets.UTF_8);

            Object in = buf;
            if (PROCESSORS.containsKey(channel)) {
                for (Map.Entry<String, MessageProcessor<?, ?>> processorEntry : PROCESSORS.get(channel)) {
                    // System.out.println("Processing packet to use " + processorEntry.getKey() + " for object " + in.getClass().getName() + "...");
                    if (processorEntry.getValue().isValidSource(in)) in = ((MessageProcessor) processorEntry.getValue()).process(in);
                    else System.out.println("Skipping processor " + processorEntry.getKey() + " for object " + in.getClass().getName() + "...");
                    // System.out.println("Successfully processed packet to use " + processorEntry.getKey() + " as object " + in.getClass().getName() + "...");
                }
            }

            if (LISTENERS.containsKey(channel)) {
                if (in instanceof Packet packet) {
                    for (MessageListener listener : LISTENERS.get(channel)) {
                        listener.onMessage(packet);
                    }
                } else {
                    throw new IllegalStateException("Received message is not finalized to Packet, final type is " + in.getClass().getName() + "!");
                }
            }
        } else {
            throw new IllegalStateException("Unknown header channel type " + type + " detected, require 0 (string)");
        }
    }

    public enum Side {
        BUKKIT,
        BUNGEECORD
    }
}
