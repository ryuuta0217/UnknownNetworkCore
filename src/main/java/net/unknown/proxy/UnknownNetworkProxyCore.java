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

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.event.connection.PreLoginEvent;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import io.netty.channel.*;
import net.unknown.proxy.fml.ForgeListener;
import net.unknown.proxy.util.VelocityConnectionReflectUtil;
import net.unknown.shared.SharedConstants;
import org.spongepowered.configurate.CommentedConfigurationNode;
import org.spongepowered.configurate.yaml.YamlConfigurationLoader;

import javax.inject.Inject;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiPredicate;
import java.util.logging.Level;
import java.util.logging.Logger;

@ChannelHandler.Sharable
@Plugin(id = "unknown-network-core", name = "UnknownNetworkCore", version = SharedConstants.VERSION)
public class UnknownNetworkProxyCore {
    private static final String PACKET_HANDLER_NAME = "unc-proxy-handler";

    private static RegisteredServer LOBBY;
    private static RegisteredServer SURVIVAL;

    private static UnknownNetworkProxyCore INSTANCE;

    private static YamlConfigurationLoader CONFIG_LOADER;
    private static CommentedConfigurationNode CONFIG;

    public static UnknownNetworkProxyCore getInstance() {
        return UnknownNetworkProxyCore.INSTANCE;
    }

    public static RegisteredServer getLobbyServer() {
        return LOBBY;
    }

    public static RegisteredServer getSurvivalServer() {
        return SURVIVAL;
    }

    public static CommentedConfigurationNode getConfig() {
        return CONFIG;
    }

    public static YamlConfigurationLoader createConfigLoader(File configFile) {
        return YamlConfigurationLoader.builder().file(configFile).build();
    }

    public static YamlConfigurationLoader createConfigLoader(Path configFilePath) {
        return YamlConfigurationLoader.builder().path(configFilePath).build();
    }

    public static void saveConfig() {
        try {
            CONFIG_LOADER.save(CONFIG);
        } catch (IOException e) {
            getInstance().getLogger().warning("Failed to save configuration file!");
        }
    }

    private static void loadConfig() throws IOException {
        File configFile = new File(getInstance().getDataFolder(), "config.yml");
        if ((configFile.getParentFile().exists() || configFile.getParentFile().mkdirs()) && (configFile.exists() || configFile.createNewFile())) {
            CONFIG_LOADER = createConfigLoader(configFile);
            CONFIG = CONFIG_LOADER.load();
        } else {
            throw new IOException("Failed to create configuration file!");
        }
    }

    public static boolean hasPacketListener(String velocityPacketName, NetworkDirection direction, String listenerName) {
        return getInstance().packetListeners.containsKey(velocityPacketName) && getInstance().packetListeners.get(velocityPacketName).containsKey(direction) && getInstance().packetListeners.get(velocityPacketName).get(direction).containsKey(listenerName);
    }

    public static void registerPacketListener(String velocityPacketName, NetworkDirection direction, String listenerName, BiPredicate<String, Object> listener) {
        if (hasPacketListener(velocityPacketName, direction, listenerName)) {
            throw new IllegalArgumentException("Packet " + velocityPacketName + " Listener " + listenerName + " already registered!");
        }
        getInstance().packetListeners.computeIfAbsent(velocityPacketName, k -> new HashMap<>()).computeIfAbsent(direction, k -> new HashMap<>()).put(listenerName, listener);
    }

    public static void unregisterPacketListener(String velocityPacketName, NetworkDirection direction, String listenerName) {
        if (hasPacketListener(velocityPacketName, direction, listenerName)) {
            getInstance().packetListeners.get(velocityPacketName).get(direction).remove(listenerName);
        }
    }

    @Inject
    @DataDirectory
    private Path dataDir;
    private final ProxyServer proxy;
    private final Logger logger;
    private final Map<String, Map<NetworkDirection, Map<String, BiPredicate<String, Object>>>> packetListeners = new HashMap<String, Map<NetworkDirection, Map<String, BiPredicate<String, Object>>>>();

    @Inject
    public UnknownNetworkProxyCore(ProxyServer proxy, Logger logger) {
        this.proxy = proxy;
        this.logger = logger;
        INSTANCE = this;
    }

    public File getDataFolder() {
        return this.dataDir.toFile();
    }

    public Logger getLogger() {
        return this.logger;
    }

    @Subscribe
    public void onProxyInit(ProxyInitializeEvent event) {
        try {
            loadConfig();
        } catch (IOException e) {
            this.getLogger().log(Level.SEVERE, "Failed to load configuration file!", e);
        }
        this.proxy.getEventManager().register(this, new ForgeListener());
        this.proxy.getEventManager().register(this, new PingListener());
        this.proxy.getEventManager().register(this, new ChatLogging());
        this.proxy.getEventManager().register(this, new ServerDisconnectListener());
        this.proxy.getChannelRegistrar().register(MinecraftChannelIdentifier.forDefaultNamespace("brand"), MinecraftChannelIdentifier.forDefaultNamespace("register"), ForgeListener.FORGE_HANDSHAKE_IDENTIFIER, ForgeListener.FORGE_LOGIN_IDENTIFIER);
        LOBBY = this.proxy.getServer("lobby").orElse(null);
        SURVIVAL = this.proxy.getServer("survival").orElse(null);
    }

    public ProxyServer getProxy() {
        return this.proxy;
    }

    @Subscribe
    public void onPreLogin(PreLoginEvent event) {
        VelocityConnectionReflectUtil.getNettyChannel(event.getConnection()).pipeline().addAfter("minecraft-encoder", PACKET_HANDLER_NAME, new ChannelDuplexHandler() {
            private final String username = event.getUsername();

            @Override
            public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
                NetworkDirection direction = NetworkDirection.SERVER_TO_CLIENT;
                String velocityPacketClassName = msg.getClass().getName();
                String velocityPacketName = velocityPacketClassName.substring(velocityPacketClassName.lastIndexOf('.') + 1);
                if (packetListeners.containsKey(velocityPacketName) && packetListeners.get(velocityPacketName).containsKey(direction)) {
                    Map<String, BiPredicate<String, Object>> listeners = packetListeners.get(velocityPacketName).get(direction);
                    boolean toProcess = listeners.isEmpty() || listeners.entrySet().stream().allMatch(e -> e.getValue().test(this.username, msg));
                    if (!toProcess) {
                        return;
                    }
                }
                super.write(ctx, msg, promise);
            }

            @Override
            public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
                NetworkDirection direction = NetworkDirection.CLIENT_TO_SERVER;
                String velocityPacketClassName = msg.getClass().getName();
                String velocityPacketName = velocityPacketClassName.substring(velocityPacketClassName.lastIndexOf('.') + 1);
                if (packetListeners.containsKey(velocityPacketName) && packetListeners.get(velocityPacketName).containsKey(direction)) {
                    Map<String, BiPredicate<String, Object>> listeners = packetListeners.get(velocityPacketName).get(direction);
                    boolean toProcess = listeners.isEmpty() || listeners.entrySet().stream().allMatch(e -> e.getValue().test(this.username, msg));
                    if (!toProcess) {
                        return;
                    }
                }
                super.channelRead(ctx, msg);
            }
        });
    }

    @Subscribe
    public void onDisconnect(DisconnectEvent event) {
        Channel channel = VelocityConnectionReflectUtil.getNettyChannel(event.getPlayer());
        channel.eventLoop().submit(() -> channel.pipeline().remove(PACKET_HANDLER_NAME));
    }
}
