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

package net.unknown.survival.data;

import net.minecraft.server.level.ServerPlayer;
import net.unknown.UnknownNetworkCore;
import net.unknown.core.configurations.Config;
import net.unknown.core.configurations.ConfigurationSerializer;
import net.unknown.core.managers.RunnableManager;
import net.unknown.survival.data.model.Home;
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
    private static final int VERSION = 3;
    private static final Map<UUID, PlayerData> PLAYER_DATA_MAP = new HashMap<>();
    private static final int DEFAULT_MAX_HOME_COUNT = 5;
    private final UUID uniqueId;
    private Map<String, Map<String, Home>> homes;
    private String defaultGroup;
    private Map<String, Material> group2Material;
    private int homeBaseCount;
    private int homeAdditionalCount;
    private boolean isAfk = false;
    private ChatData chatData;

    public PlayerData(UUID uniqueId) {
        super("players/" + uniqueId + ".yml", false, "UNC/PlayerData/" + Bukkit.getOfflinePlayer(uniqueId).getName());
        this.uniqueId = uniqueId;
    }

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
        if (!PLAYER_DATA_MAP.containsKey(uniqueId)) {
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
        if (!f.exists()) return;
        File[] files = f.listFiles(p -> {
            if (p.getName().endsWith(".yml")) {
                try {
                    UUID.fromString(p.getName().replace(".yml", ""));
                    return true;
                } catch (IllegalArgumentException ignored) {
                }
            }
            return false;
        });

        for (File file : files) {
            UUID uniqueId = UUID.fromString(file.getName().replace(".yml", ""));
            PLAYER_DATA_MAP.put(uniqueId, new PlayerData(uniqueId));
        }
    }

    private static UUID extractUniqueIdFromFileName(String fileName) {
        Matcher m = Pattern.compile("players/(.*)\\.yml").matcher(fileName);
        if (m.matches() && m.groupCount() == 1) return UUID.fromString(m.group(1));
        return null;
    }

    @Override
    public void onLoad() {
        UUID tempUniqueId = extractUniqueIdFromFileName(this.getFileName());

        if (!this.getConfig().isSet("config-version") && this.getConfig().getKeys(false).size() > 0) {
            this.getLogger().warning("Unknown configuration version, version set to 1.");
            this.getConfig().set("config-version", 1);
        } else if (!this.getConfig().isSet("config-version")) {
            this.getLogger().warning("Empty configuration, version set to latest(" + VERSION + ").");
            this.getConfig().set("config-version", VERSION);
        }

        if (this.getConfig().getInt("config-version") < VERSION) {
            this.getLogger().warning("Old version config detected, migrating...");

            int version = this.getConfig().getInt("config-version");

            switch (version) {
                case 1:
                    Migrators.MigrateToV2FromV1.migrate(tempUniqueId, this.getFile(), this.getConfig());
                case 2:
                    Migrators.MigrateToV3FromV2.migrate(tempUniqueId, this.getFile(), this.getConfig());
            }

            if (this.getConfig().getInt("config-version") == VERSION) {
                this.getLogger().info("Successfully migrated to V" + VERSION);
            }
        }

        if (this.homes == null) this.homes = new LinkedHashMap<>();
        else if (!this.homes.isEmpty()) this.homes.clear();
        ConfigurationSection homeGroups = this.getConfig().getConfigurationSection("homes");
        if (homeGroups != null) {
            long start = System.nanoTime();
            homeGroups.getKeys(false).forEach(groupName -> {
                ConfigurationSection groupedHomes = homeGroups.getConfigurationSection(groupName);
                if (groupedHomes != null) {
                    Map<String, Home> homes = new LinkedHashMap<>();
                    groupedHomes.getKeys(false).forEach(homeName -> {
                        Location loc = ConfigurationSerializer.getLocationData(groupedHomes, homeName);
                        if (loc != null) homes.put(homeName, new Home(homeName, loc));
                    });
                    this.homes.put(groupName, homes);
                }
            });
            long end = System.nanoTime();
            long durationNS = end - start;
            this.getLogger().info("Home load completed with " + durationNS + "ns (" + TimeUnit.NANOSECONDS.toMillis(durationNS) + "ms)");
        } else if (!this.getConfig().isSet("homes")) {
            this.addGroup("default");
            this.getLogger().info("Home isn't set, created default group.");
        }

        if (this.group2Material == null) this.group2Material = new HashMap<>();
        else if (!this.group2Material.isEmpty()) this.group2Material.clear();
        ConfigurationSection homeGroupItems = this.getConfig().getConfigurationSection("homeGroupItems");
        if (homeGroupItems != null) {
            homeGroupItems.getKeys(false).forEach(groupName -> this.group2Material.put(groupName, Material.valueOf(homeGroupItems.getString(groupName))));
        }

        this.homeBaseCount = this.getConfig().isSet("home-base-count") ? this.getConfig().getInt("home-base-count") : DEFAULT_MAX_HOME_COUNT;
        this.homeAdditionalCount = this.getConfig().isSet("home-additional-count") ? this.getConfig().getInt("home-additional-count") : 0;

        this.chatData = ChatData.load(this, this.getConfig());
    }

    @Override
    public synchronized void save() {
        this.getConfig().set("homes", null);
        this.homes.forEach((groupName, categorizedHomes) -> {
            if (categorizedHomes.size() > 0) {
                categorizedHomes.forEach((homeName, home) -> {
                    ConfigurationSerializer.setLocationData(this.getConfig(), "homes." + groupName + "." + homeName, home.location());
                });
            } else {
                this.getConfig().createSection("homes." + groupName);
            }
        });

        this.getConfig().set("homeGroupItems", null);
        this.group2Material.forEach((groupName, material) -> this.getConfig().set("homeGroupItems." + groupName, material.name()));

        this.getConfig().set("home-base-count", homeBaseCount);
        this.getConfig().set("home-additional-count", homeAdditionalCount);
        this.chatData.save(this.getConfig());
        super.save();
    }

    public boolean isOnline() {
        return Bukkit.getOfflinePlayer(this.uniqueId).isOnline();
    }

    @Nullable
    public Player asPlayer() {
        return Bukkit.getPlayer(this.uniqueId);
    }

    public Map<String, Map<String, Home>> getGroupedHomes() {
        return this.homes;
    }

    public Set<String> getGroups() {
        return this.homes.keySet();
    }

    public String getDefaultGroup() {
        if (this.defaultGroup == null) this.defaultGroup = new ArrayList<>(this.getGroups()).get(0);
        return this.defaultGroup;
    }

    public boolean isGroupExists(String groupName) {
        return this.homes.containsKey(groupName);
    }

    public void addGroup(String newGroupName) {
        if (this.isGroupExists(newGroupName)) return;
        this.homes.put(newGroupName, new LinkedHashMap<>());
        RunnableManager.runAsync(this::save);
    }

    public Material getGroupMaterial(String groupName) {
        return this.group2Material.getOrDefault(groupName, Material.WHITE_WOOL);
    }

    public void setGroupMaterial(String groupName, Material newMaterial) {
        this.group2Material.put(groupName, newMaterial);
        RunnableManager.runAsync(this::save);
    }

    @Nullable
    public Map<String, Home> getHomes(String groupName) {
        return this.homes.getOrDefault(groupName, null);
    }

    @Nullable
    public Map<String, Home> getDefaultHomes() {
        return this.homes.getOrDefault(getDefaultGroup(), null);
    }

    public Set<String> getHomeNames(String groupName) {
        return this.homes.getOrDefault(groupName, new HashMap<>()).keySet();
    }

    public Set<String> getDefaultHomeNames() {
        return this.homes.getOrDefault(getDefaultGroup(), new HashMap<>()).keySet();
    }

    public boolean isHomeExists(String groupName, String name) {
        return this.getHomeNames(groupName).contains(name);
    }

    @Nullable
    public Home getHome(String groupName, String name) {
        return this.homes.getOrDefault(groupName, new HashMap<>()).getOrDefault(name, null);
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
        RunnableManager.runAsync(this::save);
    }

    public int getHomeAdditionalCount() {
        return this.homeAdditionalCount;
    }

    public void setHomeAdditionalCount(int newAdditionalCount) {
        this.homeAdditionalCount = newAdditionalCount;
        RunnableManager.runAsync(this::save);
    }

    public int getMaxHomeCount() {
        return this.getHomeBaseCount() + this.getHomeAdditionalCount();
    }

    public Home addHome(String groupName, String name, Location loc, boolean overwrite) {
        if (name.contains(".")) return null;
        if (!overwrite && this.homes.getOrDefault(groupName, new HashMap<>()).containsKey(name)) return null;
        Map<String, Home> categorizedHomes = this.homes.getOrDefault(groupName, new HashMap<>());
        categorizedHomes.put(name, new Home(name, loc));
        this.homes.put(groupName, categorizedHomes);
        RunnableManager.runAsync(this::save);
        return this.homes.get(groupName).get(name);
    }

    public boolean removeHome(String groupName, String name) {
        if (this.homes.containsKey(groupName)) {
            if (this.homes.get(groupName).containsKey(name)) {
                this.homes.get(groupName).remove(name);
                return true;
            }
        }
        return false;
    }

    public boolean isAfk() {
        return this.isAfk;
    }

    public void setAfk(boolean afk) {
        this.isAfk = afk;
    }

    public ChatData getChatData() {
        return this.chatData;
    }

    public static class ChatData {
        private final PlayerData parent;
        private UUID replyTarget;
        private String forceGlobalChatPrefix = "g.";
        private boolean useKanaConvert = false;

        public ChatData(PlayerData parent, String replyTargetStr, String forceGlobalChatPrefix, boolean useKanaConvert) {
            this.parent = parent;
            this.replyTarget = replyTargetStr != null ? UUID.fromString(replyTargetStr) : null;
            this.forceGlobalChatPrefix = forceGlobalChatPrefix;
            this.useKanaConvert = useKanaConvert;
        }

        public UUID getPrivateMessageReplyTarget() {
            return this.replyTarget;
        }

        public void setPrivateMessageReplyTarget(UUID replyTarget) {
            this.replyTarget = replyTarget;
            RunnableManager.runAsync(this.parent::save);
        }

        public String getForceGlobalChatPrefix() {
            return this.forceGlobalChatPrefix;
        }

        public void setForceGlobalChatPrefix(String forceGlobalChatPrefix) {
            this.forceGlobalChatPrefix = forceGlobalChatPrefix;
            RunnableManager.runAsync(this.parent::save);
        }

        public boolean isUseKanaConvert() {
            return this.useKanaConvert;
        }

        public void setUseKanaConvert(boolean useKanaConvert) {
            this.useKanaConvert = useKanaConvert;
            RunnableManager.runAsync(this.parent::save);
        }

        public static ChatData load(PlayerData parent, FileConfiguration config) {
            return new ChatData(parent,
                    config.getString("reply-target", null),
                    config.getString("force-global-chat-prefix", "g."),
                    config.getBoolean("use-kana-convert", false));
        }

        public void save(FileConfiguration config) {
            if (this.replyTarget != null) config.set("reply-target", this.replyTarget.toString());
            config.set("force-global-chat-prefix", this.forceGlobalChatPrefix);
            config.set("use-kana-convert", this.useKanaConvert);
        }
    }

    public static class Migrators {
        public static class MigrateToV2FromV1 {
            public static void migrate(UUID uniqueId, File file, FileConfiguration config) {
                if (config.isSet("config-version") && config.getInt("config-version") <= 1) {
                    String playerName = uniqueId.toString();
                    OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(uniqueId);
                    if (offlinePlayer != null) playerName = offlinePlayer.getName();
                    Logger LOGGER = Logger.getLogger("UNC/PlayerDataMigrator/V1 -> V2/" + uniqueId);

                    if (config.isSet("homes")) {
                        LOGGER.info("Started migration for " + playerName + "'s homes");
                        ConfigurationSection section = config.getConfigurationSection("homes");
                        if (section != null) {
                            /* LOAD OLD HOMES */
                            Map<String, Home> oldHomeMap = new LinkedHashMap<>(); // 登録順が崩れないようにLinkedを使う
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

        public static class MigrateToV3FromV2 {
            public static void migrate(UUID uniqueId, File configFile, FileConfiguration config) {
                if (config.isSet("config-version") && config.getInt("config-version") == 2) {
                    String playerName = uniqueId.toString();
                    OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(uniqueId);
                    if (offlinePlayer != null) playerName = offlinePlayer.getName();
                    Logger LOGGER = Logger.getLogger("UNC/PlayerDataMigrator/V2 -> V3/" + playerName);

                    Map<String, Material> category2Material = new HashMap<>();

                    if (config.isSet("homeCategoryItems")) {
                        ConfigurationSection homeCategoryItemsSection = config.getConfigurationSection("homeCategoryItems");
                        if (homeCategoryItemsSection == null) throw new IllegalStateException("Something wrong.");
                        homeCategoryItemsSection.getKeys(false).forEach(categoryName -> {
                            category2Material.put(categoryName, Material.valueOf(homeCategoryItemsSection.getString(categoryName)));
                        });
                    }

                    Map<String, ConfigurationSection> uncategorizedHomes = new LinkedHashMap<>(); // 登録順が崩れないようにLinkedを使う

                    if (config.isSet("homes.uncategorized")) {
                        ConfigurationSection uncategorizedHomeSection = config.getConfigurationSection("homes.uncategorized");
                        if (uncategorizedHomeSection == null) throw new IllegalStateException("Something wrong.");

                        uncategorizedHomeSection.getKeys(false).forEach(homeName -> {
                            if (uncategorizedHomeSection.isSet(homeName)) {
                                uncategorizedHomes.put(homeName, uncategorizedHomeSection.getConfigurationSection(homeName));
                            }
                        });
                    }

                    config.set("homeCategoryItems", null);

                    category2Material.forEach((categoryName, material) -> {
                        if (categoryName.equalsIgnoreCase("uncategorized")) categoryName = "default";
                        config.set("homeGroupItems." + categoryName, material.name());
                    });

                    if (uncategorizedHomes.size() > 0) {
                        LOGGER.info("Renaming Home Category \"uncategorized\" to Home Group \"default\"");
                        config.set("homes.uncategorized", null);

                        uncategorizedHomes.forEach((homeName, section) -> {
                            config.set("homes.default." + homeName, section);
                        });
                    }

                    config.set("config-version", 3);

                    try {
                        config.save(configFile);
                    } catch (IOException e) {
                        LOGGER.warning("Failed to save migrated configuration data.");
                    }
                }
            }
        }

        public static class MigrateToV4FromV3 {
            public static void migrate(UUID uniqueId, File configFile, FileConfiguration config) {
                if (config.isSet("config-version") && config.getInt("config-version") == 3) {
                    String playerName = uniqueId.toString();
                    OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(uniqueId);
                    if(offlinePlayer.getName() != null) playerName = offlinePlayer.getName();
                    Logger LOGGER = Logger.getLogger("UNC/PlayerDataMigrator/V3 -> V4/" + playerName);

                    Map<String, String> channelChatForcePrefixes = new HashMap<>();

                    if(config.isSet("force-global-chat-prefix")) {
                        channelChatForcePrefixes.put("global", config.getString("force-global-chat-prefix"));
                    }

                    config.set("force-global-chat-prefix", null);

                    config.createSection("force-channel-chat-prefixes", channelChatForcePrefixes);

                    config.set("config-version", 4);

                    try {
                        config.save(configFile);
                    } catch (IOException e) {
                        LOGGER.warning("Failed to save migrated configuration data.");
                    }
                }
            }
        }
    }
}
