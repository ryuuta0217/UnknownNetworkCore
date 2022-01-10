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

package net.unknown.survival;

import net.unknown.survival.chat.ChatManager;
import net.unknown.survival.chat.HeadsUpChat;
import net.unknown.UnknownNetworkCore;
import net.unknown.survival.antivillagerlag.AntiVillagerLag;
import net.unknown.survival.commands.Commands;
import net.unknown.survival.data.PlayerData;
import net.unknown.survival.enchants.HatakeWatari;
import net.unknown.survival.fun.PathfinderGrapple;
import net.unknown.survival.listeners.MainGuiOpenListener;
import org.bukkit.Bukkit;

import java.util.logging.Logger;

public class UnknownNetworkSurvival {
    private static final Logger LOGGER = Logger.getLogger("UNC/Survival");
    private static boolean HOLOGRAPHIC_DISPLAYS_ENABLED = false;

    public static void onLoad() {
        Commands.init();
        //RegistryUtil.Bukkit.unregisterBlockDataMap(HopperBlock.class);
        //RegistryUtil.Bukkit.registerBlockDataMap(CustomHopperBlock.class, CustomCraftHopper::new);
        //RegistryUtil.forceReplace(Registry.BLOCK, Blocks.HOPPER, CustomBlocks.HOPPER);
        //RegistryUtil.forceReplace(Registry.BLOCK_ENTITY_TYPE, BlockEntityType.HOPPER, CustomBlockEntityType.HOPPER);
        //RegistryUtil.forceReplace(Registry.ITEM, Items.HOPPER, CustomItems.HOPPER);
        //RegistryUtil.Bukkit.reloadCraftMagicNumbers();
    }

    public static void onEnable() {
        HOLOGRAPHIC_DISPLAYS_ENABLED = Bukkit.getPluginManager().getPlugin("HolographicDisplays") != null && Bukkit.getPluginManager().isPluginEnabled("HolographicDisplays");

        PlayerData.loadExists();
        AntiVillagerLag.startLoopTask();
        Bukkit.getPluginManager().registerEvents(new MainGuiOpenListener(), UnknownNetworkCore.getInstance());
        Bukkit.getPluginManager().registerEvents(new ChatManager(), UnknownNetworkCore.getInstance());
        if(HOLOGRAPHIC_DISPLAYS_ENABLED) Bukkit.getPluginManager().registerEvents(new HeadsUpChat(), UnknownNetworkCore.getInstance());
        Bukkit.getPluginManager().registerEvents(new PathfinderGrapple(), UnknownNetworkCore.getInstance());
        Bukkit.getPluginManager().registerEvents(new HatakeWatari(), UnknownNetworkCore.getInstance());
        LOGGER.info("Plugin enabled - Running as Survival mode.");
    }

    public static void onDisable() {

    }

    public static boolean isHolographicDisplaysEnabled() {
        return HOLOGRAPHIC_DISPLAYS_ENABLED;
    }
}
