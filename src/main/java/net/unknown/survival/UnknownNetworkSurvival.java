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

package net.unknown.survival;

import net.milkbowl.vault.economy.Economy;
import net.unknown.UnknownNetworkCore;
import net.unknown.core.discord.UnknownNetworkDiscordBot;
import net.unknown.core.managers.ListenerManager;
import net.unknown.survival.antivillagerlag.AntiVillagerLag;
import net.unknown.survival.bossbar.BlueMapBar;
import net.unknown.survival.chat.ChatManager;
import net.unknown.survival.chat.CustomChannels;
import net.unknown.survival.commands.Commands;
import net.unknown.survival.commands.SuppressRaidCommand;
import net.unknown.survival.data.PlayerData;
import net.unknown.survival.data.Warps;
import net.unknown.survival.dependency.WorldGuard;
import net.unknown.survival.discord.MinecraftToDiscordMessageListener;
import net.unknown.survival.enchants.*;
import net.unknown.survival.enchants.nms.DamageEnchant;
import net.unknown.survival.events.ModifiableBlockBreakEvent;
import net.unknown.survival.feature.*;
import net.unknown.survival.feature.gnarms.GNArms;
import net.unknown.survival.fml.FMLConnectionListener;
import net.unknown.survival.fml.ModdedPlayerManager;
import net.unknown.survival.fun.DemolitionGun;
import net.unknown.survival.fun.MonsterBall;
import net.unknown.survival.fun.PathfinderGrapple;
import net.unknown.survival.gui.hopper.ConfigureHopperGui;
import net.unknown.survival.listeners.*;
import net.unknown.survival.update.UNCUpdateCheckTask;
import net.unknown.survival.wrapper.economy.WrappedEconomy;
import org.bukkit.Bukkit;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.ServicePriority;

import java.util.Optional;
import java.util.logging.Logger;

public class UnknownNetworkSurvival {
    private static final Logger LOGGER = Logger.getLogger("UNC/Survival");
    private static boolean BOOTSTRAPPED = false;
    private static boolean HOLOGRAPHIC_DISPLAYS_ENABLED = false;
    private static boolean WORLD_GUARD_ENABLED = false;
    private static boolean VAULT_ENABLED = false;
    private static boolean JECON_ENABLED = false;
    private static boolean LUCKPERMS_ENABLED = false;
    private static boolean VOTIFIER_ENABLED = false;

    public static void onLoad() {
        Commands.init();
        DamageEnchant.register();
        try {
            Class.forName("net.unknown.launchwrapper.Main");
            BOOTSTRAPPED = true;
        } catch (ClassNotFoundException ignored) {
        }

        UnknownNetworkDiscordBot.runAnotherThread(UnknownNetworkDiscordBot.defaultBuilder());
    }

    public static void onEnable() {
        HOLOGRAPHIC_DISPLAYS_ENABLED = Bukkit.getPluginManager().getPlugin("HolographicDisplays") != null && Bukkit.getPluginManager().isPluginEnabled("HolographicDisplays");
        WORLD_GUARD_ENABLED = Bukkit.getPluginManager().getPlugin("WorldGuard") != null && Bukkit.getPluginManager().isPluginEnabled("WorldGuard");
        VAULT_ENABLED = Bukkit.getPluginManager().getPlugin("Vault") != null && Bukkit.getPluginManager().isPluginEnabled("Vault");
        JECON_ENABLED = Bukkit.getPluginManager().getPlugin("Jecon") != null && Bukkit.getPluginManager().isPluginEnabled("Jecon");
        LUCKPERMS_ENABLED = Bukkit.getPluginManager().getPlugin("LuckPerms") != null && Bukkit.getPluginManager().isPluginEnabled("LuckPerms");
        VOTIFIER_ENABLED = Bukkit.getPluginManager().getPlugin("Votifier") != null && Bukkit.getPluginManager().isPluginEnabled("Votifier");

        Warps.load();
        //PlayerData.loadExists();
        CustomChannels.load();
        AntiVillagerLag.startLoopTask();
        PlayerDeathListener.load();

        CustomEnchantments.initialize();
        GNArms.initialize();
        BlueMapBar.initialize();
        UNCUpdateCheckTask.start();
        DebugStickEntityEditor.Listener.register();

        Bukkit.getPluginManager().registerEvents(ModifiableBlockBreakEvent.Listener.getInstance(), UnknownNetworkCore.getInstance());

        ListenerManager.registerListener(new MainGuiOpenListener());
        ListenerManager.registerListener(new ChatManager());
        ListenerManager.registerListener(new ColorCodeListener());
        ListenerManager.registerListener(new ModdedPlayerManager());
        ListenerManager.registerListener(new PathfinderGrapple());
        ListenerManager.registerListener(new DemolitionGun());
        ListenerManager.registerListener(new MonsterBall());
        ListenerManager.registerListener(new PlayerDeathListener());
        ListenerManager.registerListener(new ServerRestartListener());
        ListenerManager.registerListener(new ProtectedAreaTestStick());
        ListenerManager.registerListener(new PlayerJoinListener());
        ListenerManager.registerListener(new MinecraftToDiscordMessageListener());
        SuppressRaidCommand.registerListener();
        //ListenerManager.registerListener(new WorldSeparator());
        if (isBootstrapped()) {
            getLogger().info("Successfully Bootstrapped!");
            ListenerManager.registerListener(new BlockDisassembler());
            ListenerManager.registerListener(new ConfigureHopperGui.Listener());
            ListenerManager.registerListener(new ChestLink());
        }

        Bukkit.getMessenger().registerOutgoingPluginChannel(UnknownNetworkCore.getInstance(), "BungeeCord");
        Bukkit.getMessenger().registerIncomingPluginChannel(UnknownNetworkCore.getInstance(), "unknown:forge", new FMLConnectionListener());

        DemolitionGun.BowPullIndicator.boot();

        if (isVaultEnabled() && isJeconEnabled()) {
            Optional<RegisteredServiceProvider<Economy>> optionalEconomyServiceProvider = Optional.ofNullable(Bukkit.getServicesManager().getRegistration(Economy.class));
            if (optionalEconomyServiceProvider.isPresent()) {
                Economy economyService = optionalEconomyServiceProvider.get().getProvider();
                Bukkit.getServicesManager().unregister(Economy.class, economyService);
                WrappedEconomy wrapped = new WrappedEconomy(economyService);
                Bukkit.getServicesManager().register(Economy.class, wrapped, UnknownNetworkCore.getInstance(), ServicePriority.Highest);
            }
        }

        if (isWorldGuardEnabled()) {
            WorldGuard.PermissionsResolver.inject();
        }

        if (isVotifierEnbaled()) {
            ListenerManager.registerListener(new VoteListener());
        }
        LOGGER.info("Plugin enabled - Running as Survival mode.");
    }

    public static void onDisable() {

    }

    public static Logger getLogger() {
        return LOGGER;
    }

    public static boolean isBootstrapped() {
        return BOOTSTRAPPED;
    }

    public static boolean isHolographicDisplaysEnabled() {
        return HOLOGRAPHIC_DISPLAYS_ENABLED;
    }

    public static boolean isWorldGuardEnabled() {
        return WORLD_GUARD_ENABLED;
    }

    public static boolean isVaultEnabled() {
        return VAULT_ENABLED;
    }

    public static boolean isJeconEnabled() {
        return JECON_ENABLED;
    }

    public static boolean isLuckPermsEnabled() {
        return LUCKPERMS_ENABLED;
    }

    public static boolean isVotifierEnbaled() {
        return VOTIFIER_ENABLED;
    }
}
