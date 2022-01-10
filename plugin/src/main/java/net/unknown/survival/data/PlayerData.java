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

package net.unknown.survival.data;

import net.minecraft.server.level.ServerPlayer;
import net.unknown.UnknownNetworkCore;
import net.unknown.core.configurations.Config;
import net.unknown.core.configurations.ConfigurationSerializer;
import net.unknown.core.managers.RunnableManager;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PlayerData extends Config {
    private static final int VERSION = 2;
    private static final Map<UUID, PlayerData> PLAYER_DATA_MAP = new HashMap<>();
    private static final int DEFAULT_MAX_HOME_COUNT = 5;

    public static PlayerData of(Player player) {
        return of(player.getUniqueId());
    }

    public static PlayerData of(OfflinePlayer offlinePlayer) {
        return of(offlinePlayer.getUniqueId());
    }

    public static PlayerData of(ServerPlayer player) {
        return of(player.getUUID());
    }

    public static PlayerData of(UUID uniqueId) {
        if(!PLAYER_DATA_MAP.containsKey(uniqueId)) {
            PlayerData pd = new PlayerData(uniqueId);
            PLAYER_DATA_MAP.put(uniqueId, pd);
        }
        return PLAYER_DATA_MAP.get(uniqueId);
    }

    public static Map<UUID, PlayerData> getAll() {
        return PLAYER_DATA_MAP;
    }

    public static void loadExists() {
        File f = new File(UnknownNetworkCore.getInstance().getDataFolder(), "players");
        if(!f.exists()) return;
        File[] files = f.listFiles(p -> {
            if(p.getName().endsWith(".yml")) {
                try {
                    UUID.fromString(p.getName().replace(".yml", ""));
                    return true;
                } catch (IllegalArgumentException ignored) {}
            }
            return false;
        });

        for (File file : files) {
            UUID uniqueId = UUID.fromString(file.getName().replace(".yml", ""));
            PLAYER_DATA_MAP.put(uniqueId, new PlayerData(uniqueId));
        }
    }

    private final UUID uniqueId;
    private Map<String, Map<String, Home>> homes;
    private Map<String, Material> category2Material;
    private Map<String, Set<String>> removedHomes;
    private int homeBaseCount;
    private int homeAdditionalCount;
    private UUID replyTarget;

    private boolean isAfk = false;

    private final boolean savingLock = false;

    public PlayerData(UUID uniqueId) {
        super("players/" + uniqueId + ".yml", false, "UNC/PlayerData");
        this.uniqueId = uniqueId;
    }

    @Override
    public void onLoad() {
        UUID tempUniqueId = extractUniqueIdFromFileName(this.getFileName());

        if(!this.getConfig().isSet("config-version") && this.getConfig().getKeys(false).size() > 0) {
            this.getLogger().warning("Unknown configuration version, version set to 1.");
            this.getConfig().set("config-version", 1);
        } else if (!this.getConfig().isSet("config-version")) {
            this.getLogger().warning("Empty configuration, version set to latest(" + VERSION + ").");
            this.getConfig().set("config-version", VERSION);
        }

        if(this.getConfig().getInt("config-version") < VERSION) {
            this.getLogger().warning("Old version config detected, migrating...");

            int version = this.getConfig().getInt("config-version");

            switch(version) {
                case 1:
                    MigrateToV2FromV1.migrate(tempUniqueId, this.getFile(), this.getConfig());
            }
        }

        if(this.homes == null) this.homes = new LinkedHashMap<>();
        else if (!this.homes.isEmpty()) this.homes.clear();
        ConfigurationSection homeCategories = this.getConfig().getConfigurationSection("homes");
        if (homeCategories != null) {
            long start = System.nanoTime();
            homeCategories.getKeys(false).forEach(categoryName -> {
                ConfigurationSection categorizedHomes = homeCategories.getConfigurationSection(categoryName);
                if (categorizedHomes != null) {
                    Map<String, Home> homes = new LinkedHashMap<>();
                    categorizedHomes.getKeys(false).forEach(homeName -> {
                        Location loc = ConfigurationSerializer.getLocationData(categorizedHomes, homeName);
                        if (loc == null) {
                            if (this.removedHomes == null) this.removedHomes = new HashMap<>();
                            Set<String> removedHomes = this.removedHomes.getOrDefault(categoryName, new HashSet<>());
                            removedHomes.add(homeName);
                            this.removedHomes.put(categoryName, removedHomes);
                        } else {
                            homes.put(homeName, new Home(homeName, loc));
                        }
                    });
                    this.homes.put(categoryName, homes);
                }
            });
            long end = System.nanoTime();
            long durationNS = end - start;
            if(tempUniqueId != null) this.getLogger().info(Bukkit.getOfflinePlayer(tempUniqueId).getName() + "'s home load completed with " + durationNS + "ns (" + TimeUnit.NANOSECONDS.toMillis(durationNS) + "ms)");
        }

        if(this.category2Material == null) this.category2Material = new HashMap<>();
        else if(!this.category2Material.isEmpty()) this.category2Material.clear();
        ConfigurationSection homeCategoryItems = this.getConfig().getConfigurationSection("homeCategoryItems");
        if (homeCategoryItems != null) {
            homeCategoryItems.getKeys(false).forEach(categoryName -> this.category2Material.put(categoryName, Material.valueOf(homeCategoryItems.getString(categoryName))));
        }

        this.homeBaseCount = this.getConfig().isSet("home-base-count") ? this.getConfig().getInt("home-base-count") : DEFAULT_MAX_HOME_COUNT;
        this.homeAdditionalCount = this.getConfig().isSet("home-additional-count") ? this.getConfig().getInt("home-additional-count") : 0;

        if(this.getConfig().isSet("reply-target")) this.replyTarget = UUID.fromString(this.getConfig().getString("reply-target"));
    }

    @Override
    public void save() {
        this.getConfig().set("homes", null);
        this.homes.forEach((categoryName, categorizedHomes) -> {
            categorizedHomes.forEach((homeName, home) -> {
                ConfigurationSerializer.setLocationData(this.getConfig(), "homes." + categoryName + "." + homeName, home.location());
            });
        });

        this.getConfig().set("homeCategoryItems", null);
        this.category2Material.forEach((categoryName, material) -> this.getConfig().set("homeCategoryItems." + categoryName, material.name()));

        this.getConfig().set("home-base-count", homeBaseCount);
        this.getConfig().set("home-additional-count", homeAdditionalCount);
        if(this.replyTarget != null) this.getConfig().set("reply-target", this.replyTarget.toString());
        super.save();
    }

    public boolean isOnline() {
        return Bukkit.getOfflinePlayer(this.uniqueId).isOnline();
    }

    @Nullable
    public Player asPlayer() {
        return Bukkit.getPlayer(this.uniqueId);
    }

    public Map<String, Map<String, Home>> getCategorizedHomes() {
        return this.homes;
    }

    public Set<String> getCategories() {
        return this.homes.keySet();
    }

    public Material getCategoryMaterial(String category) {
        return this.category2Material.getOrDefault(category, Material.WHITE_WOOL);
    }

    public void setCategoryMaterial(String category, Material newMaterial) {
        this.category2Material.put(category, newMaterial);
        RunnableManager.runAsync(this::save);
    }

    @Nullable
    public Map<String, Home> getHomes(String category) {
        return this.homes.getOrDefault(category, null);
    }

    public Map<String, Set<String>> getRemovedHomes() {
        return this.removedHomes;
    }

    public Set<String> getHomeNames(String category) {
        return this.homes.getOrDefault(category, new HashMap<>()).keySet();
    }

    public boolean isHomeExists(String category, String name) {
        return this.getHomeNames(category).contains(name);
    }

    @Nullable
    public Home getHome(String category, String name) {
        return this.homes.getOrDefault(category, new HashMap<>()).getOrDefault(name, null);
    }

    public int getHomeCount() {
        AtomicInteger count = new AtomicInteger();
        this.homes.forEach((a, b) -> count.addAndGet(b.size()));
        return count.get();
    }

    public int getHomeBaseCount() {
        return this.homeBaseCount;
    }

    public void setHomeBaseCount(int newBaseCount) {
        this.homeBaseCount = newBaseCount;
    }

    public int getHomeAdditionalCount() {
        return this.homeAdditionalCount;
    }

    public void setHomeAdditionalCount(int newAdditionalCount) {
        this.homeAdditionalCount = newAdditionalCount;
    }

    public int getMaxHomeCount() {
        return this.getHomeBaseCount() + this.getHomeAdditionalCount();
    }

    public Home addHome(String category, String name, Location loc, boolean overwrite) {
        if(name.contains(".")) return null;
        if(!overwrite && this.homes.getOrDefault(category, new HashMap<>()).containsKey(name)) return null;
        Map<String, Home> categorizedHomes = this.homes.getOrDefault(category, new HashMap<>());
        categorizedHomes.put(name, new Home(name, loc));
        this.homes.put(category, categorizedHomes);
        RunnableManager.runAsync(this::save);
        return this.homes.get(category).get(name);
    }

    public boolean removeHome(String category, String name) {
        if(this.homes.containsKey(category)) {
            if(this.homes.get(category).containsKey(name)) {
                this.homes.get(category).remove(name);
                return true;
            }
        }
        return false;
    }

    public void setAfk(boolean afk) {
        this.isAfk = afk;
    }

    public boolean isAfk() {
        return this.isAfk;
    }

    public UUID getPrivateMessageReplyTarget() {
        return this.replyTarget;
    }

    public void setPrivateMessageReplyTarget(UUID replyTarget) {
        this.replyTarget = replyTarget;
        RunnableManager.runAsync(this::save);
    }

    private static UUID extractUniqueIdFromFileName(String fileName) {
        Matcher m = Pattern.compile("players/(.*)\\.yml").matcher(fileName);
        if(m.matches() && m.groupCount() == 1) return UUID.fromString(m.group(1));
        return null;
    }

    public static class MigrateToV2FromV1 {
        public static void migrate(UUID uniqueId, File file, FileConfiguration config) {
            if (config.isSet("config-version") && config.getInt("config-version") <= 1) {
                String playerName = uniqueId.toString();
                OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(uniqueId);
                if(offlinePlayer != null) playerName = offlinePlayer.getName();
                Logger LOGGER = Logger.getLogger("PlayerDataMigrator/V1 -> V2/" + uniqueId);

                if (config.isSet("homes")) {
                    LOGGER.info("Started migration for " + playerName + "'s homes");
                    ConfigurationSection section = config.getConfigurationSection("homes");
                    if (section != null) {
                        /* LOAD OLD HOMES */
                        Map<String, Home> oldHomeMap = new LinkedHashMap<>();
                        section.getKeys(false).forEach(homeName -> {
                            LOGGER.info("Migrating: homes." + homeName + " -> homes.uncategorized." + homeName);
                            Location loc = ConfigurationSerializer.getLocationData(section, homeName);
                            if (loc != null) {
                                oldHomeMap.put(homeName, new Home(homeName, loc));
                            } else LOGGER.warning("Home " + homeName + " was *removed* because world is null.");
                        });

                        /* CLEAR OLD HOMES */
                        config.set("homes", null);

                        /* PUT NEW HOMES */
                        oldHomeMap.forEach((name, home) -> ConfigurationSerializer.setLocationData(config, "homes.uncategorized." + name, home.location()));
                    }
                } else {
                    LOGGER.info("Migration for " + playerName + "'s homes skipped because no homes was found.");
                }

                /* CHANGE CONFIG VERSION */
                config.set("config-version", 2);

                try {
                    config.save(file);
                } catch (IOException e) {
                    LOGGER.warning("Failed to save migrated configuration data.");
                }
            }
        }
    }
}
