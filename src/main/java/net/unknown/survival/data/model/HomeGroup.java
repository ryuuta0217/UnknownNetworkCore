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

package net.unknown.survival.data.model;

import com.ryuuta0217.util.ComponentCollector;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.HoverEvent;
import net.unknown.core.configurations.ConfigurationSerializer;
import net.unknown.core.define.DefinedTextColor;
import net.unknown.core.managers.RunnableManager;
import net.unknown.survival.data.PlayerData;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import javax.annotation.Nullable;
import java.util.*;
import java.util.function.BiPredicate;
import java.util.logging.Level;
import java.util.logging.Logger;

public class HomeGroup {
    private final Logger logger;
    private final PlayerData.HomeData homeData;
    private final String name;
    private Material icon;
    protected final LinkedHashMap<String, Home> homes;

    public HomeGroup(PlayerData.HomeData homeData, String name, Material icon, LinkedHashMap<String, Home> homes) {
        this.homeData = homeData;
        this.name = name;
        this.logger = Logger.getLogger(homeData.getLogger().getName() + "/Group/" + this.name);
        this.icon = icon;
        this.homes = homes;
    }

    public PlayerData.HomeData getHomeData() {
        return this.homeData;
    }

    public String getName() {
        return this.name;
    }

    public LinkedHashMap<String, Home> getHomes() {
        return new LinkedHashMap<>(this.homes);
    }

    @Nullable
    public Home getHome(String name) {
        return this.homes.getOrDefault(name, null);
    }

    public boolean hasHome(String name) {
        return this.homes.containsKey(name);
    }

    public boolean hasHome(Home home) {
        return this.homes.containsValue(home);
    }

    public void addHome(Home home) {
        this.addHome(home, false);
    }

    public void addHome(Home home, boolean overwrite) {
        if (hasHome(home.name()) && !overwrite) throw new IllegalArgumentException("Home already exists");
        this.homes.remove(home.name());
        this.homes.put(home.name(), home);
        this.saveAsync();
    }

    public void removeHome(String name) {
        if (!hasHome(name)) throw new IllegalArgumentException("Home does not exist");
        this.homes.remove(name);
        this.saveAsync();
    }

    public void removeHome(Home home) {
        if (!hasHome(home)) throw new IllegalArgumentException("Provided home " + home.name() + " is not group " + this.getName() + "'s home.");
        this.homes.remove(home.name());
        this.saveAsync();
    }

    public boolean removeIf(BiPredicate<String, Home> filter) {
        boolean removed = this.homes.entrySet().removeIf(e -> filter.test(e.getKey(), e.getValue()));
        if (removed) this.saveAsync();
        return removed;
    }

    public boolean removeAll(Collection<String> c) {
        boolean removed = this.homes.keySet().removeAll(c);
        if (removed) this.saveAsync();
        return removed;
    }

    public Material getIcon() {
        return this.icon != null ? this.icon : Material.WHITE_WOOL;
    }

    public void setIcon(Material icon) {
        this.icon = icon;
        this.saveAsync();
    }

    public Logger getLogger() {
        return this.logger;
    }

    public static HomeGroup load(PlayerData.HomeData homeData, String groupName, ConfigurationSection groupSection, @Nullable ConfigurationSection itemsSection) {
        Logger logger = Logger.getLogger(homeData.getLogger().getName() + "/LoadGroup");
        Material icon = itemsSection != null ? Material.getMaterial(itemsSection.getString(groupName, "WHITE_WOOL")) : Material.WHITE_WOOL;
        LinkedHashMap<String, Home> homes = new LinkedHashMap<>();
        groupSection.getKeys(false).forEach(homeName -> {
            String worldName = ConfigurationSerializer.getWorldNameByConfig(groupSection, homeName);
            double[] position = ConfigurationSerializer.getPositionByConfig(groupSection, homeName);
            float[] rotation = ConfigurationSerializer.getRotationByConfig(groupSection, homeName);
            if (worldName != null && position != null && position.length == 3) {
                Home home = new Home(homeName, worldName, position[0], position[1], position[2], (rotation != null ? rotation[0] : 0.0f), (rotation != null ? rotation[1] : 0.0f));
                if (rotation == null) logger.warning("Home " + homeName + " does not have a rotation, using default rotation (yaw: 0.0, pitch: 0.0)");
                homes.put(homeName, home);
            } else {
                logger.severe("Failed to load home " + homeName + ", invalid data provided. data=(world=" + worldName + ", position=(" + (position != null ? "x: " + position[0] + ", y: " + position[1] + ", z: " + position[2] : "null") + "), rotation=(" + (rotation != null ? "yaw: " + rotation[0] + ", pitch: " + rotation[1] : "null") + "))");
            }
        });
        return new HomeGroup(homeData, groupName, icon, homes);
    }

    public void save(ConfigurationSection groupSection, ConfigurationSection itemsSection) {
        Map<String, Throwable> saveFailures = new HashMap<>();
        this.getHomes().forEach((homeName, home) -> {
            try {
                ConfigurationSerializer.setLocationData(groupSection, homeName, home);
            } catch (Throwable t) {
                saveFailures.put(homeName, t);
            }
        });

        if (this.getIcon() != null) itemsSection.set(this.getName(), this.getIcon().name());
        if (!saveFailures.isEmpty()) {
            saveFailures.forEach((homeName, t) -> this.getLogger().log(Level.WARNING, "Failed to save home " + homeName, t));
            throw new IllegalStateException("Failed to save home(s): " + saveFailures.keySet());
        }
    }

    private void saveAsync() {
        RunnableManager.runAsync(() -> {
            if (this.homeData.getGroups().containsValue(this)) {
                this.getHomeData().getPlayerData().save();
            }
        });
    }
}
