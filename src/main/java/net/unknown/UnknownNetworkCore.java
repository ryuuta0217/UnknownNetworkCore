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

package net.unknown;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.dedicated.DedicatedServer;
import net.unknown.core.commands.Commands;
import net.unknown.core.fixer.ThirdPartyPluginPermissionsFixer;
import net.unknown.core.gui.SignGui;
import net.unknown.core.managers.PacketManager;
import net.unknown.core.managers.TrashManager;
import net.unknown.core.tab.TabListPingManager;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.v1_19_R1.CraftServer;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.java.JavaPlugin;
import org.json.simple.parser.JSONParser;

import java.io.File;

public class UnknownNetworkCore extends JavaPlugin {
    private static final File SHARED_DATA_FOLDER = new File("../shared");
    private static final JSONParser JSON_PARSER = new JSONParser();
    private static final Environment ENV = Environment.valueOf(System.getProperty("un.env", "SURVIVAL"));
    private static UnknownNetworkCore INSTANCE;

    public UnknownNetworkCore() {
        INSTANCE = this;
    }

    public static CommandDispatcher<CommandSourceStack> getBrigadier() {
        return getDedicatedServer().vanillaCommandDispatcher.getDispatcher();
    }

    public static File getSharedDataFolder() {
        return SHARED_DATA_FOLDER;
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

    @Override
    public void onLoad() {
        long start = System.nanoTime();
        getLogger().info("Plugin was loaded with environment: " + ENV.name());
        if(!this.getDataFolder().exists() && this.getDataFolder().mkdir()) {
            getLogger().info("Plugin folder created.");
        }
        Commands.init();
        ENV.onLoad();
        long end = System.nanoTime();
        getLogger().info("Plugin was loaded in " + (end - start) / 1000000 + "ms");
    }

    @Override
    public void onEnable() {
        long start = System.nanoTime();
        Bukkit.getPluginManager().registerEvents(PacketManager.getInstance(), this);
        Bukkit.getPluginManager().registerEvents(new SignGui.Listener(), this);
        Bukkit.getPluginManager().registerEvents(new TabListPingManager(), this);
        TabListPingManager.startTask();
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
}
