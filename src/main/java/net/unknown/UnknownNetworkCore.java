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

package net.unknown;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.dedicated.DedicatedServer;
import net.unknown.core.athletic.Athletics;
import net.unknown.core.block.MultiPageChest;
import net.unknown.core.bossbar.TPSBar;
import net.unknown.core.chat.CustomChatTypes;
import net.unknown.core.commands.Commands;
import net.unknown.core.fixer.ThirdPartyPluginPermissionsFixer;
import net.unknown.core.gui.SignGui;
import net.unknown.core.managers.ListenerManager;
import net.unknown.core.packet.PacketManager;
import net.unknown.core.managers.TrashManager;
import net.unknown.core.prefix.PlayerPrefixes;
import net.unknown.core.skin.SkinManager;
import net.unknown.core.tab.TabListPingManager;
import net.unknown.core.util.ObfuscationUtil;
import net.unknown.shared.VersionInfo;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.v1_20_R1.CraftServer;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.java.JavaPlugin;
import org.json.simple.parser.JSONParser;

public class UnknownNetworkCore extends JavaPlugin {
    private static final JSONParser JSON_PARSER = new JSONParser();
    private static final Environment ENV = Environment.valueOf(System.getProperty("un.env", "SURVIVAL"));
    private static UnknownNetworkCore INSTANCE;

    public UnknownNetworkCore() {
        INSTANCE = this;
    }

    public static CommandDispatcher<CommandSourceStack> getBrigadier() {
        return getDedicatedServer().vanillaCommandDispatcher.getDispatcher();
    }

    public static DedicatedServer getDedicatedServer() {
        return ((CraftServer) Bukkit.getServer()).getServer();
    }

    public static UnknownNetworkCore getInstance() {
        return INSTANCE;
    }

    public static JSONParser getJsonParser() {
        return JSON_PARSER;
    }

    public static Environment getEnvironment() {
        return ENV;
    }

    @Override
    public void onLoad() {
        long start = System.nanoTime();
        getLogger().info("Plugin was loaded with environment: " + ENV.name());
        if (!this.getDataFolder().exists() && this.getDataFolder().mkdir()) {
            getLogger().info("Plugin folder created.");
        }
        ObfuscationUtil.loadAllMappings();
        getLogger().info("Server launched in " + ObfuscationUtil.OBF_STATE);
        /*ObfuscationUtil.getMapping().forEach((mojangName, clazz) -> {
            try {
                Class<?> spigotClass = Class.forName(clazz.getEffectiveClassName());
                Arrays.stream(spigotClass.getDeclaredFields()).forEach(field -> {
                    if(clazz.getFields().stream().noneMatch(mappingField -> mappingField.obfuscatedFieldName().equals(field.getName()) || mappingField.fieldName().equals(field.getName()))) {
                        getLogger().info(mojangName + "." + field.getName());
                    }
                });
            } catch (Throwable t) {
                if(clazz.hasSpigotName()) {
                    getLogger().info("Unknown Class " + mojangName + "(" + clazz.getSpigotName() + ") found!");
                } else {
                    getLogger().info("Unknown Class " + mojangName + " found!");
                }
                t.printStackTrace();
            }
        });*/
        CustomChatTypes.bootstrap();
        Commands.init();
        ENV.onLoad();
        long end = System.nanoTime();
        getLogger().info("Plugin was loaded in " + (end - start) / 1000000 + "ms");
    }

    @Override
    public void onEnable() {
        long start = System.nanoTime();
        ListenerManager.registerListener(PacketManager.getInstance());
        ListenerManager.registerListener(new SignGui.Listener());
        ListenerManager.registerListener(new TabListPingManager());
        ListenerManager.registerListener(new Athletics.Listener());
        ListenerManager.registerListener(new MultiPageChest.Listener());
        ListenerManager.registerListener(SkinManager.INSTANCE);
        TPSBar.initialize();
        TabListPingManager.startTask();
        PlayerPrefixes.loadAll();
        Athletics.load();
        Athletics.loadProgresses();
        TrashManager.loadExists();
        ThirdPartyPluginPermissionsFixer.scheduleNextTick();
        getLogger().info("");
        getLogger().info("");
        getLogger().info("""


                ██╗   ██╗███╗   ██╗██╗  ██╗███╗   ██╗ ██████╗ ██╗    ██╗███╗   ██╗
                ██║   ██║████╗  ██║██║ ██╔╝████╗  ██║██╔═══██╗██║    ██║████╗  ██║
                ██║   ██║██╔██╗ ██║█████╔╝ ██╔██╗ ██║██║   ██║██║ █╗ ██║██╔██╗ ██║
                ██║   ██║██║╚██╗██║██╔═██╗ ██║╚██╗██║██║   ██║██║███╗██║██║╚██╗██║
                ╚██████╔╝██║ ╚████║██║  ██╗██║ ╚████║╚██████╔╝╚███╔███╔╝██║ ╚████║
                 ╚═════╝ ╚═╝  ╚═══╝╚═╝  ╚═╝╚═╝  ╚═══╝ ╚═════╝  ╚══╝╚══╝ ╚═╝  ╚═══╝
                 
                ███╗   ██╗███████╗████████╗██╗    ██╗ ██████╗ ██████╗ ██╗  ██╗
                ████╗  ██║██╔════╝╚══██╔══╝██║    ██║██╔═══██╗██╔══██╗██║ ██╔╝
                ██╔██╗ ██║█████╗     ██║   ██║ █╗ ██║██║   ██║██████╔╝█████╔╝
                ██║╚██╗██║██╔══╝     ██║   ██║███╗██║██║   ██║██╔══██╗██╔═██╗
                ██║ ╚████║███████╗   ██║   ╚███╔███╔╝╚██████╔╝██║  ██║██║  ██╗
                ╚═╝  ╚═══╝╚══════╝   ╚═╝    ╚══╝╚══╝  ╚═════╝ ╚═╝  ╚═╝╚═╝  ╚═╝

                """);
        getLogger().info("");
        getLogger().info("");
        getLogger().info("");
        ENV.onEnable();
        long end = System.nanoTime();
        getLogger().info("Plugin was enabled in " + (end - start) / 1000000 + "ms");
    }

    @Override
    public void onDisable() {
        HandlerList.unregisterAll(this);
        ENV.onDisable();
    }

    public static VersionInfo getVersion() {
        return VersionInfo.parseFromString(getInstance().getPluginMeta().getVersion());
    }
}
