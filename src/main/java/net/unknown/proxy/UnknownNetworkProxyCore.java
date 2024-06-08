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
import com.velocitypowered.api.event.lifecycle.ProxyInitializeEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import net.unknown.proxy.fml.ForgeListener;
import net.unknown.shared.SharedConstants;
import org.spongepowered.configurate.CommentedConfigurationNode;
import org.spongepowered.configurate.yaml.YamlConfigurationLoader;

import javax.inject.Inject;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.logging.Logger;

@Plugin(id = "unknown-network-core", name = "UnknownNetworkCore", version = SharedConstants.VERSION)
public class UnknownNetworkProxyCore {
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

    @Inject
    @DataDirectory
    private Path dataDir;
    private final ProxyServer proxy;
    private final Logger logger;

    @Inject
    public UnknownNetworkProxyCore(ProxyServer proxy, Logger logger) {
        this.proxy = proxy;
        this.logger = logger;
        INSTANCE = this;
    }

    public File getDataFolder() {
        return this.dataDir.toFile();
    }

    public java.util.logging.Logger getLogger() {
        return this.logger;
    }

    public void onLoad() {
        ModdedInitialHandler.injectModdedInitialHandler();
        try {
            loadConfig();
        } catch (IOException e) {
            getLogger().warning("Failed to load configuration file!");
        }
    }

    @Subscribe
    public void onProxyInit(ProxyInitializeEvent event) {
        this.proxy.eventManager().register(this, new ForgeListener());
        this.proxy.eventManager().register(this, new PingListener());
        this.proxy.eventManager().register(this, new ChatLogging());
        //this.proxy.eventManager().register(this, new ServerDisconnectListener());
        LOBBY = this.proxy.server("lobby");
        SURVIVAL = this.proxy.server("survival");
    }
}
