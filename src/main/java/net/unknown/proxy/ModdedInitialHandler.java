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

package net.unknown.proxy;

import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.handler.codec.haproxy.HAProxyMessageDecoder;
import net.md_5.bungee.BungeeCord;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.config.ListenerInfo;
import net.md_5.bungee.api.event.ClientConnectEvent;
import net.md_5.bungee.connection.InitialHandler;
import net.md_5.bungee.netty.ChannelWrapper;
import net.md_5.bungee.netty.HandlerBoss;
import net.md_5.bungee.netty.PipelineUtils;
import net.md_5.bungee.protocol.*;
import net.md_5.bungee.protocol.packet.LoginPayloadResponse;
import net.unknown.core.util.ReflectionUtil;
import net.unknown.proxy.fml.ModdedHandshakeProcessor;
import net.unknown.proxy.fml.ModdedPlayer;

import java.lang.reflect.Field;
import java.net.SocketAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

public class ModdedInitialHandler extends InitialHandler {
    public static final Map<Integer, ModdedPlayer> PENDING_FORGE_PLAYER_CONNECTIONS = new HashMap<>();
    public static final Map<String, ModdedPlayer> FORGE_PLAYERS = new HashMap<>();
    private static final Logger LOGGER = UnknownNetworkProxyCore.getInstance().getProxy().getLogger();
    private static final KickStringWriter legacyKicker = getLegacyKicker();
    private static ChannelInitializer<Channel> moddedInitializer;

    public ModdedInitialHandler(BungeeCord bungee, ListenerInfo listener) {
        super(bungee, listener);
    }

    public static void injectModdedInitialHandler() {
        LOGGER.info(" -> Injection started");
        if (moddedInitializer == null) {
            LOGGER.info(" -> Create new ChannelInitializer instance");
            moddedInitializer = new ChannelInitializer<>() {
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
            LOGGER.info(" -> Instance OK");
        }

        LOGGER.info(" -> Checking currently SERVER_CHILD value");
        if (PipelineUtils.SERVER_CHILD.equals(moddedInitializer)) {
            LOGGER.info(" -> Already injected.");
            return;
        }
        LOGGER.info(" -> SERVER_CHILD is default, continue injection");

        try {
            LOGGER.info(" -> Finding SERVER_CHILD field");
            Field serverChildField = PipelineUtils.class.getDeclaredField("SERVER_CHILD");
            LOGGER.info(" -> Found! " + serverChildField);

            /* MAKE THE VALUE OF THE FINAL FIELD CHANGEABLE */
            //ReflectionUtil.makeNonFinal(serverChildField);

            LOGGER.info(" -> Injecting");
            /* MODIFY */
            ReflectionUtil.setStaticFinalObject(serverChildField, moddedInitializer);

            /* CHECK */
            if (PipelineUtils.SERVER_CHILD.equals(moddedInitializer)) {
                LOGGER.info(" -> Injection completed.");
            } else {
                LOGGER.warning(" -> Injection failed, running without forge detection support.");
            }
        } catch (Exception e) {
            LOGGER.warning(" -> Injection failed: " + e.getLocalizedMessage());
            LOGGER.warning(" -> Proxy is now running without forge detection support.");
            e.printStackTrace();
        }
    }

    private static KickStringWriter getLegacyKicker() {
        try {
            Field legacyKickerF = PipelineUtils.class.getDeclaredField("legacyKicker");
            legacyKickerF.setAccessible(true);
            return (KickStringWriter) legacyKickerF.get(null);
        } catch (Exception ignored) {
        }
        return null;
    }

    @Override
    public void handle(LoginPayloadResponse response) {
        LOGGER.info("[" + this.getName() + "|" + this.getSocketAddress() + "]  -> LoginPayloadResponse received.");

        if (!PENDING_FORGE_PLAYER_CONNECTIONS.containsKey(response.getId())) {
            LOGGER.info("[" + this.getSocketAddress() + "|" + this.getName() + "] <-> Message ID " + response.getId() + " is unknown. Disconnecting");
            this.disconnect(ChatColor.RED + "接続中に問題が発生しました: Message ID is Unknown.\n" +
                    ChatColor.AQUA + "Mod環境をご使用中の場合は、バニラ環境にて再接続をお試しください。\n" +
                    ChatColor.AQUA + "また、この問題を公式Discordにて開発に報告していただけると助かります。");
            return;
        }
        if (PENDING_FORGE_PLAYER_CONNECTIONS.get(response.getId()) instanceof ModdedHandshakeProcessor processor) {
            processor.onLoginPayloadResponseReceived(response);
        }

        PENDING_FORGE_PLAYER_CONNECTIONS.remove(response.getId());
    }

    public ChannelWrapper getChannel() {
        try {
            Field field = InitialHandler.class.getDeclaredField("ch");
            return (ChannelWrapper) field.get(this);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            return null;
        }
    }

    @Override
    public String toString() {
        return "[" + this.getSocketAddress() + (this.getName() != null ? "|" + this.getName() : "") + "] <-> ModdedInitialHandler";
    }
}
