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

import net.unknown.core.configurations.ConfigurationSerializer;
import net.unknown.core.managers.RunnableManager;
import net.unknown.survival.data.PlayerData;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;

import javax.annotation.Nullable;
import java.util.LinkedHashMap;

public class HomeGroup {
    private final PlayerData.HomeData homeData;
    private final String name;
    private Material icon;
    protected final LinkedHashMap<String, Home> homes;

    public HomeGroup(PlayerData.HomeData homeData, String name, Material icon, LinkedHashMap<String, Home> homes) {
        this.homeData = homeData;
        this.name = name;
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

    public Material getIcon() {
        return this.icon != null ? this.icon : Material.WHITE_WOOL;
    }

    public void setIcon(Material icon) {
        this.icon = icon;
        this.saveAsync();
    }

    public static HomeGroup load(PlayerData.HomeData homeData, String groupName, ConfigurationSection groupSection, @Nullable ConfigurationSection itemsSection) {
        Material icon = itemsSection != null ? Material.getMaterial(itemsSection.getString(groupName, "WHITE_WOOL")) : Material.WHITE_WOOL;
        LinkedHashMap<String, Home> homes = new LinkedHashMap<>();
        groupSection.getKeys(false).forEach(homeName -> {
            Location location = ConfigurationSerializer.getLocationData(groupSection, homeName);
            Home home = new Home(homeName, location);
            homes.put(homeName, home);
        });
        return new HomeGroup(homeData, groupName, icon, homes);
    }

    public void save(ConfigurationSection groupSection, ConfigurationSection itemsSection) {
        this.getHomes().forEach((homeName, home) -> {
            ConfigurationSerializer.setLocationData(groupSection, homeName, home.location());
        });

        if (this.getIcon() != null) itemsSection.set(this.getName(), this.getIcon().name());
    }

    private void saveAsync() {
        RunnableManager.runAsync(() -> {
            if (this.homeData.getGroups().containsValue(this)) {
                this.getHomeData().getPlayerData().save();
            }
        });
    }
}
