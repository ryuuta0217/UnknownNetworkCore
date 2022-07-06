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

package net.unknown.core.fixer;

import com.sk89q.bukkit.util.DynamicPluginCommand;
import net.minecraft.resources.ResourceLocation;
import net.unknown.UnknownNetworkCore;
import net.unknown.core.managers.RunnableManager;
import net.unknown.core.util.ReflectionUtil;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.SimpleCommandMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.util.Map;

public class ThirdPartyPluginPermissionsFixer {
    private static final Logger LOGGER = LoggerFactory.getLogger("PermissionsFixer");

    public static void scheduleNextTick() {
        RunnableManager.runDelayed(ThirdPartyPluginPermissionsFixer::fixAll, 1);
    }

    public static void fixAll() {
        fixMultiverseCore();
        fixMultiverseInventories();
        fixMultiverseNetherPortals();
        fixGSit();
        fixSpark();
    }

    public static void fixMultiverseCore() {
        getKnownCommands()
                .entrySet()
                .stream()
                .filter(e -> e.getValue() instanceof PluginCommand)
                .map(e -> Map.entry(e.getKey(), ((PluginCommand) e.getValue())))
                .filter(e -> e.getValue().getPlugin().getName().equals("Multiverse-Core"))
                .forEach((e) -> {
                    String k = e.getKey();
                    PluginCommand v = e.getValue();

                    if(v.getPermission() == null) v.setPermission("multiverse.core.command." + v.getName());
                });
    }

    public static void fixMultiverseInventories() {
        getKnownCommands()
                .entrySet()
                .stream()
                .filter(e -> e.getValue() instanceof PluginCommand)
                .map(e -> Map.entry(e.getKey(), ((PluginCommand) e.getValue())))
                .filter(e -> e.getValue().getPlugin().getName().equals("Multiverse-Inventories"))
                .forEach(e -> {
                    String k = e.getKey();
                    PluginCommand v = e.getValue();

                    if(v.getPermission() == null) v.setPermission("multiverse.inventories.command." + v.getName());
                });
    }

    public static void fixMultiverseNetherPortals() {
        getKnownCommands()
                .entrySet()
                .stream()
                .filter(e -> e.getValue() instanceof PluginCommand)
                .map(e -> Map.entry(e.getKey(), ((PluginCommand) e.getValue())))
                .filter(e -> e.getValue().getPlugin().getName().equals("Multiverse-NetherPortals"))
                .forEach(e -> {
                    String k = e.getKey();
                    PluginCommand v = e.getValue();

                    if(v.getPermission() == null) v.setPermission("multiverse.netherportals.command." + v.getName());
                });
    }

    public static void fixGSit() {
        getKnownCommands()
                .entrySet()
                .stream()
                .filter(e -> e.getValue() instanceof PluginCommand)
                .map(e -> Map.entry(e.getKey(), ((PluginCommand) e.getValue())))
                .filter(e -> e.getValue().getPlugin().getName().equals("GSit"))
                .forEach(e -> {
                    String k = e.getKey();
                    PluginCommand v = e.getValue();

                    if(v.getPermission() != null) return;

                    if(v.getName().equals("gsit")) {
                        v.setPermission("gsit.sit");
                    } else if (v.getName().equals("glay")) {
                        v.setPermission("gsit.lay");
                    } else if (v.getName().equals("gbellyflop")) {
                        v.setPermission("gsit.bellyflop");
                    } else if (v.getName().equals("gspin")) {
                        v.setPermission("gsit.spin");
                    } else if (v.getName().equals("gcrawl")) {
                        v.setPermission("gsit.crawl");
                    }
                });
    }

    public static void fixSpark() {
        getKnownCommands()
                .entrySet()
                .stream()
                .filter(e -> e.getValue() instanceof PluginCommand)
                .map(e -> Map.entry(e.getKey(), ((PluginCommand) e.getValue())))
                .filter(e -> e.getValue().getPlugin().getName().equals("spark"))
                .forEach(e -> {
                    String k = e.getKey();
                    PluginCommand v = e.getValue();

                    if(v.getPermission() == null) {
                        if(!v.getName().equals("spark")) v.setPermission("spark." + v.getName());
                        else v.setPermission("spark");
                    }
                });
    }

    @Nullable
    public static Map<String, Command> getKnownCommands() {
        try {
            return (Map<String, Command>) SimpleCommandMap.class
                    .getDeclaredField("knownCommands")
                    .get(Bukkit.getCommandMap());
        } catch (IllegalAccessException | NoSuchFieldException e) {
            LOGGER.error("Incompatible Bukkit server environment!", e);
            return null;
        }
    }
}
