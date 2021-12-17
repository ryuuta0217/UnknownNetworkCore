/*
 * Copyright (c) 2021 Unknown Network Developers and contributors.
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
 *     loss of use data or profits; or business interpution) however caused and on any theory of liability,
 *     whether in contract, strict liability, or tort (including negligence or otherwise)
 *     arising in any way out of the use of this source code, event if advised of the possibility of such damage.
 */

package net.unknown.proxy;

import com.ryuuta0217.packets.FML2HandshakePacket;
import com.ryuuta0217.util.MinecraftPacketReader;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.handler.codec.haproxy.HAProxyMessageDecoder;
import net.md_5.bungee.BungeeCord;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.config.ListenerInfo;
import net.md_5.bungee.api.event.ClientConnectEvent;
import net.md_5.bungee.connection.InitialHandler;
import net.md_5.bungee.netty.HandlerBoss;
import net.md_5.bungee.netty.PipelineUtils;
import net.md_5.bungee.protocol.*;
import net.md_5.bungee.protocol.packet.LoginPayloadResponse;
import net.unknown.proxy.fml.FML2Player;
import net.unknown.proxy.fml.ModdedPlayer;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.net.SocketAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

public class ModdedInitialHandler extends InitialHandler {
    private static final Logger LOGGER = ProxyServer.getInstance().getLogger();
    public static final Map<Integer, Object> PENDING_FORGE_PLAYER_CONNECTIONS = new HashMap<>();
    public static final Map<String, ModdedPlayer> FORGE_PLAYERS = new HashMap<>();

    public ModdedInitialHandler(BungeeCord bungee, ListenerInfo listener) {
        super(bungee, listener);
    }

    @Override
    public void handle(LoginPayloadResponse response) {
        LOGGER.info("[ " + this.getSocketAddress() + "|" + this.getName() + "] -> LoginPayloadResponse received.");

        if (!PENDING_FORGE_PLAYER_CONNECTIONS.containsKey(response.getId())) {
            LOGGER.info("[" + this.getSocketAddress() + "|" + this.getName() + "] <-> Message ID " + response.getId() + " is unknown. Disconnecting");
            this.disconnect(ChatColor.RED + "接続中に問題が発生しました: Message ID is Unknown.\n" +
                    ChatColor.AQUA + "Mod環境をご使用中の場合は、バニラ環境にて再接続をお試しください。\n" +
                    ChatColor.AQUA + "また、この問題を公式Discordにて開発に報告していただけると助かります。");
            return;
        }
        PENDING_FORGE_PLAYER_CONNECTIONS.remove(response.getId());

        ByteBuf buf = Unpooled.wrappedBuffer(response.getData());

        int packetId = MinecraftPacketReader.readVarInt(buf);

        if (packetId == 2) {
            FML2HandshakePacket handshake = FML2HandshakePacket.parse(buf);
            FORGE_PLAYERS.put(this.getName(), new FML2Player(handshake.getMods(), handshake.getChannels(), handshake.getRegistries()));
            LOGGER.info("[" + this.getSocketAddress() + "|" + this.getName() + "] <-> Successfully FML2 handshake completed");
            LOGGER.info("[" + this.getSocketAddress() + "|" + this.getName() + "] <-> Connected as using mods: " + handshake.getMods());
        }
    }

    @Override
    public String toString() {
        return "[" + this.getSocketAddress() + (this.getName() != null ? "|" + this.getName() : "") + "] <-> ModdedInitialHandler";
    }

    private static final KickStringWriter legacyKicker = getLegacyKicker();
    private static ChannelInitializer<Channel> moddedInitializer;

    public static void injectModdedInitialHandler() {
        if(moddedInitializer == null) moddedInitializer = new ChannelInitializer<>() {
            @Override
            protected void initChannel(Channel ch) throws Exception {
                SocketAddress remoteAddress = (ch.remoteAddress() == null) ? ch.parent().localAddress() : ch.remoteAddress();

                if (BungeeCord.getInstance().getConnectionThrottle() != null && BungeeCord.getInstance().getConnectionThrottle().throttle(remoteAddress)) {
                    ch.close();
                    return;
                }

                ListenerInfo listener = ch.attr(PipelineUtils.LISTENER).get();

                if (BungeeCord.getInstance().getPluginManager().callEvent(new ClientConnectEvent(remoteAddress, listener)).isCancelled()) {
                    ch.close();
                    return;
                }

                PipelineUtils.BASE.initChannel(ch);
                ch.pipeline().addBefore(PipelineUtils.FRAME_DECODER, PipelineUtils.LEGACY_DECODER, new LegacyDecoder());
                ch.pipeline().addAfter(PipelineUtils.FRAME_DECODER, PipelineUtils.PACKET_DECODER, new MinecraftDecoder(Protocol.HANDSHAKE, true, ProxyServer.getInstance().getProtocolVersion()));
                ch.pipeline().addAfter(PipelineUtils.FRAME_PREPENDER, PipelineUtils.PACKET_ENCODER, new MinecraftEncoder(Protocol.HANDSHAKE, true, ProxyServer.getInstance().getProtocolVersion()));
                ch.pipeline().addBefore(PipelineUtils.FRAME_PREPENDER, PipelineUtils.LEGACY_KICKER, legacyKicker);
                ch.pipeline().get(HandlerBoss.class).setHandler(new ModdedInitialHandler(BungeeCord.getInstance(), listener));

                if (listener.isProxyProtocol()) {
                    ch.pipeline().addFirst(new HAProxyMessageDecoder());
                }
            }
        };

        if(PipelineUtils.SERVER_CHILD.equals(moddedInitializer)) {
            LOGGER.info(" -> Already injected.");
            return;
        }

        try {
            Field serverChildField = PipelineUtils.class.getDeclaredField("SERVER_CHILD");

            /* MAKE THE VALUE OF THE FINAL FIELD CHANGEABLE */
            Field modifiersField = Field.class.getDeclaredField("modifiers");
            modifiersField.setAccessible(true);
            modifiersField.setInt(serverChildField, serverChildField.getModifiers() & ~Modifier.FINAL);

            /* MODIFY */
            serverChildField.set(null, moddedInitializer);

            /* CHECK */
            if (PipelineUtils.SERVER_CHILD.equals(moddedInitializer)) {
                LOGGER.info(" -> Injection completed.");
            } else {
                LOGGER.warning(" -> Injection failed, running without forge detection support.");
            }
        } catch(Exception e) {
            LOGGER.warning(" -> Injection failed: " + e.getLocalizedMessage());
            e.printStackTrace();
        }
    }

    private static KickStringWriter getLegacyKicker() {
        try {
            Field legacyKickerF = PipelineUtils.class.getDeclaredField("legacyKicker");
            legacyKickerF.setAccessible(true);
            return (KickStringWriter) legacyKickerF.get(null);
        } catch(Exception ignored) {}
        return null;
    }
}
