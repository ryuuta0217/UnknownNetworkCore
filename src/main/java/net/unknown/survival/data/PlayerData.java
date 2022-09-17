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

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.Level;
import net.unknown.UnknownNetworkCore;
import net.unknown.core.configurations.Config;
import net.unknown.core.configurations.ConfigurationSerializer;
import net.unknown.core.managers.RunnableManager;
import net.unknown.core.util.MinecraftAdapter;
import net.unknown.core.util.ObfuscationUtil;
import net.unknown.survival.data.model.Home;
import net.unknown.survival.data.model.HomeGroup;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.block.BlockFace;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.MemoryConfiguration;
import org.bukkit.configuration.MemorySection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.craftbukkit.v1_19_R1.block.CraftBlock;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerToggleSneakEvent;

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
    private HomeData homeData;
    private final SessionData sessionData = new SessionData(this);
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

        this.homeData = HomeData.load(this);
        this.chatData = ChatData.load(this);
    }

    @Override
    public synchronized void save() {
        this.homeData.save(this.getConfig());
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

    public HomeData getHomeData() {
        return this.homeData;
    }

    public SessionData getSessionData() {
        return this.sessionData;
    }

    public ChatData getChatData() {
        return this.chatData;
    }

    public static class HomeData {
        private final PlayerData parent;
        private LinkedHashMap<String, HomeGroup> homeGroups;
        private String defaultGroup;
        private int homeBaseCount;
        private int homeAdditionalCount;

        public HomeData(PlayerData parent, String defaultGroup, int homeBaseCount, int homeAdditionalCount) {
            this.parent = parent;
            this.defaultGroup = defaultGroup;
            this.homeBaseCount = homeBaseCount;
            this.homeAdditionalCount = homeAdditionalCount;
        }

        private void setGroups(LinkedHashMap<String, HomeGroup> homeGroups) {
            this.homeGroups = homeGroups;
        }

        public PlayerData getPlayerData() {
            return this.parent;
        }

        public Map<String, HomeGroup> getGroups() {
            return this.homeGroups;
        }

        public HomeGroup getGroup(String groupName) {
            if (groupName == null) {
                return this.homeGroups.getOrDefault(this.defaultGroup, null);
            }
            return this.homeGroups.getOrDefault(groupName, null);
        }

        public HomeGroup createGroup(String groupName, Material icon) throws IllegalArgumentException {
            if (isGroupExists(groupName)) throw new IllegalArgumentException("ホームグループ " + groupName + " は既に存在します");
            HomeGroup group = new HomeGroup(this, groupName, icon, new LinkedHashMap<>());
            this.homeGroups.put(groupName, group);
            this.saveAsync();
            return this.getGroup(groupName);
        }

        public HomeGroup getDefaultGroup() throws IllegalStateException {
            if (this.defaultGroup != null && !this.homeGroups.containsKey(this.defaultGroup)) {
                throw new IllegalStateException("デフォルトに設定されているホームグループ " + this.defaultGroup + " は存在しません。");
            }
            return this.defaultGroup != null ? this.homeGroups.get(this.defaultGroup) : this.homeGroups.get("default");
        }

        public void setDefaultGroup(HomeGroup group) {
            if (group == null) {
                this.defaultGroup = null;
                this.saveAsync();
            } else if (this.homeGroups.containsKey(group.getName()) && this.homeGroups.containsValue(group)) {
                if (!this.defaultGroup.equals(group.getName())) {
                    this.defaultGroup = group.getName();
                    this.saveAsync();
                }
            }
        }

        public boolean isGroupExists(String groupName) {
            return this.homeGroups.containsKey(groupName);
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
            return this.homeBaseCount + this.homeAdditionalCount;
        }

        public static HomeData load(PlayerData data) {
            String defaultGroup = data.getConfig().getString("home-default-group", null);
            int homeBaseCount = data.getConfig().getInt("home-base-count");
            int homeAdditionalCount = data.getConfig().getInt("home-additional-count");
            HomeData homeData = new HomeData(data, defaultGroup, homeBaseCount, homeAdditionalCount);
            LinkedHashMap<String, HomeGroup> groups = new LinkedHashMap<>();

            ConfigurationSection groupItemsSection = data.getConfig().getConfigurationSection("homeGroupItems");

            data.getConfig().getConfigurationSection("homes").getKeys(false).forEach(groupName -> {
                ConfigurationSection groupSection = data.getConfig().getConfigurationSection("homes." + groupName);
                HomeGroup group = HomeGroup.load(homeData, groupName, groupSection, groupItemsSection);
                groups.put(group.getName(), group);
            });

            homeData.setGroups(groups);

            return homeData;
        }

        public void save(FileConfiguration config) {
            config.set("homes", null);
            config.set("homeGroupItems", null); // TODO in migrate v4, change to "home-group-items"

            ConfigurationSection groupItemsSection = config.createSection("homeGroupItems");
            this.homeGroups.forEach((groupName, group) -> {
                group.save(config.createSection("homes." + groupName), groupItemsSection);
            });

            if (this.defaultGroup != null) config.set("home-default-group", this.defaultGroup);
            config.set("home-base-count", this.homeBaseCount);
            config.set("home-additional-count", this.homeAdditionalCount);
        }

        private void saveAsync() {
            if (this.getPlayerData().getHomeData().equals(this)) {
                RunnableManager.runAsync(() -> this.getPlayerData().save());
            }
        }
    }

    public static class SessionData {
        private final PlayerData parent;
        private boolean isAfk = false;

        public SessionData(PlayerData parent) {
            this.parent = parent;
        }

        public PlayerData getPlayerData() {
            return this.parent;
        }

        public boolean isAfk() {
            return this.isAfk;
        }

        public void setAfk(boolean afk) {
            this.isAfk = afk;
        }
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

        public PlayerData getPlayerData() {
            return this.parent;
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

        public static ChatData load(PlayerData parent) {
            return new ChatData(parent,
                    parent.getConfig().getString("reply-target", null),
                    parent.getConfig().getString("force-global-chat-prefix", "g."),
                    parent.getConfig().getBoolean("use-kana-convert", false));
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
