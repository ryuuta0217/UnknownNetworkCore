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

package net.unknown.survival.data;

import net.kyori.adventure.text.Component;
import net.unknown.UnknownNetworkCorePlugin;
import net.unknown.core.managers.RunnableManager;
import net.unknown.survival.data.model.Warp;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Logger;

public class Warps {
    private static final Logger LOGGER = Logger.getLogger("UNC/Warps");

    private static final File FILE = new File(UnknownNetworkCorePlugin.getInstance().getDataFolder(), "warps.yml");
    private static FileConfiguration CONFIG;
    private static final Map<String, Warp> WARPS = new LinkedHashMap<>();

    public static void load() {
        try {
            if(FILE.exists() || !FILE.exists() && FILE.createNewFile()) {
                CONFIG = YamlConfiguration.loadConfiguration(FILE);
                WARPS.clear();
                if(CONFIG.isSet("warps")) {
                    ConfigurationSection warps = CONFIG.getConfigurationSection("warps");
                    if(warps != null) {
                        warps.getKeys(false).forEach(internalName -> {
                            ConfigurationSection data = warps.getConfigurationSection(internalName);
                            if(data != null) {
                                Warp warp = new Warp(data);
                                WARPS.put(internalName, warp);
                            }
                        });
                    }
                }
            }
        } catch (IOException e) {
            LOGGER.warning("Failed to load warp points! - " + e.getLocalizedMessage());
            e.printStackTrace();
        }
    }

    public synchronized static void save() {
        CONFIG.set("warps", null);
        ConfigurationSection warps = CONFIG.createSection("warps");
        WARPS.forEach((name, data) -> data.write(warps));
        try {
            CONFIG.save(FILE);
        } catch (IOException e) {
            LOGGER.warning("Failed to save warp points! - " + e.getLocalizedMessage());
            e.printStackTrace();
        }
    }

    public static Map<String, Warp> getWarps() {
        return new LinkedHashMap<>(WARPS);
    }

    @Nullable
    public static Warp getWarp(String internalName) {
        return WARPS.getOrDefault(internalName, null);
    }

    public synchronized static boolean addWarp(String internalName, Component displayName, Location location, Material icon, @Nullable Player creator, long expiresIn) {
        if(WARPS.containsKey(internalName)) return false;
        WARPS.put(internalName, new Warp(internalName, displayName, location, icon, creator == null ? null : creator.getUniqueId(), expiresIn));
        Warps.save(true);
        return WARPS.containsKey(internalName);
    }

    public synchronized static boolean removeWarp(String internalName) {
        if(WARPS.containsKey(internalName)) {
            WARPS.remove(internalName);
            Warps.save(true);
            return !WARPS.containsKey(internalName);
        }
        return false;
    }

    public static void save(boolean async) {
        if(async) RunnableManager.runAsync(Warps::save);
        else save();
    }
}
