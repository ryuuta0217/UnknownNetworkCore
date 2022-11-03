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

package net.unknown.core.athletic;

import net.kyori.adventure.text.Component;
import net.minecraft.Util;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.ShulkerBoxBlock;
import net.minecraft.world.level.block.entity.ShulkerBoxBlockEntity;
import net.unknown.UnknownNetworkCore;
import net.unknown.core.managers.RunnableManager;
import net.unknown.core.util.MessageUtil;
import net.unknown.core.util.MinecraftAdapter;
import net.unknown.shared.SharedConstants;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.craftbukkit.v1_19_R1.inventory.CraftInventoryPlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.logging.Logger;

// athletics.yml
public class Athletics {
    private static final Logger LOGGER = Logger.getLogger("UNC/ATH");

    private static final File DATA_FILE = new File(SharedConstants.DATA_FOLDER, "athletic/athletics.yml");
    private static final Set<Athletic> ATHLETICS = new HashSet<>();
    private static final Map<UUID, PlayerProgress> PROGRESSES = new HashMap<>();

    public static Set<Athletic> getAthletics() {
        return new HashSet<>(ATHLETICS);
    }

    public static Athletic getAthletic(UUID uniqueId) {
        return ATHLETICS.stream().filter(athletic -> athletic.getUniqueId().equals(uniqueId)).findFirst().orElse(null);
    }

    public static Athletic getAthletic(String name) {
        return ATHLETICS.stream().filter(athletic -> athletic.getName().equals(name)).findFirst().orElse(null);
    }

    public static Athletic addAthletic(String name, Component displayName, Location start, Location end) {
        UUID randomUniqueId = UUID.randomUUID();
        while (hasAthletic(randomUniqueId)) {
            randomUniqueId = UUID.randomUUID();
        }

        if (!hasAthletic(name)) {
            Athletic athletic = new Athletic(randomUniqueId, name, displayName, start, end);
            if (ATHLETICS.add(athletic)) {
                RunnableManager.runAsync(Athletics::save);
                return athletic;
            }
        } else {
            throw new IllegalArgumentException("Athletic \"" + name + "\" already exists. please try again with other name.");
        }
        return null;
    }

    public static void removeAthletic(String name) {
        ATHLETICS.removeIf(athletic -> athletic.getName().equals(name));
    }

    public static void removeAthletic(UUID uniqueId) {
        ATHLETICS.removeIf(athletic -> athletic.getUniqueId().equals(uniqueId));
    }

    public static void removeAthletic(Athletic athletic) {
        ATHLETICS.remove(athletic);
    }

    public static boolean hasAthletic(String name) {
        return ATHLETICS.stream().anyMatch(athletic -> athletic.getName().equals(name));
    }

    public static boolean hasAthletic(UUID uniqueId) {
        return ATHLETICS.stream().anyMatch(athletic -> athletic.getUniqueId().equals(uniqueId));
    }

    public static boolean hasAthletic(Athletic athletic) {
        return ATHLETICS.contains(athletic);
    }

    public static PlayerProgress getProgress(Player player) {
        return getProgress(player.getUniqueId());
    }

    public static PlayerProgress getProgress(UUID uniqueId) {
        if (!PROGRESSES.containsKey(uniqueId)) PROGRESSES.put(uniqueId, PlayerProgress.load(uniqueId));
        return PROGRESSES.get(uniqueId);
    }

    public static void loadProgresses() {
        File[] files = PlayerProgress.DATA_PATH.listFiles(file -> file.getName().endsWith(".yml"));
        if (files != null) {
            for (File file : files) {
                String fileName = file.getName().replace(".yml", "");
                if (MessageUtil.isUUID(fileName)) {
                    UUID uniqueId = UUID.fromString(fileName);
                    PROGRESSES.put(uniqueId, PlayerProgress.load(uniqueId));
                }
            }
        }
    }

    public static void load() {
        ATHLETICS.clear();
        try {
            if ((DATA_FILE.getParentFile().exists() || DATA_FILE.getParentFile().mkdirs()) && (DATA_FILE.exists() || DATA_FILE.createNewFile())) {
                YamlConfiguration config = YamlConfiguration.loadConfiguration(DATA_FILE);
                ConfigurationSection athleticsSection = config.getConfigurationSection(UnknownNetworkCore.getEnvironment().name());
                if (athleticsSection != null) {
                    athleticsSection.getKeys(false).forEach(athleticUniqueIdStr -> {
                        Athletic athletic = Athletic.load(athleticsSection.getConfigurationSection(athleticUniqueIdStr));
                        ATHLETICS.add(athletic);
                        LOGGER.info("Athletic " + athletic.getName() + " loaded.");
                    });
                    LOGGER.info("Loaded " + ATHLETICS.size() + " athletics.");
                }
            } else {
                LOGGER.warning("Failed to create athletics.yml");
            }
        } catch (IOException e) {
            LOGGER.warning("Failed to load athletics data.");
        }
    }

    public synchronized static void save() {
        try {
            YamlConfiguration config = YamlConfiguration.loadConfiguration(DATA_FILE);
            config.set(UnknownNetworkCore.getEnvironment().name(), null); // clear exists data
            ConfigurationSection athleticsSection = config.createSection(UnknownNetworkCore.getEnvironment().name());
            ATHLETICS.forEach(athletic -> athletic.save(athleticsSection));
            config.save(DATA_FILE);
        } catch (IOException e) {
            LOGGER.warning("An error occurred while saving athletics data.");
            e.printStackTrace();
        } catch (Exception e) {
            LOGGER.warning("Failed to write athletics data.");
            e.printStackTrace();
        }
    }

    public static class PlayerProgress {
        public static final File DATA_PATH = new File(SharedConstants.DATA_FOLDER, "athletic/players");

        private final UUID playerUniqueId;
        private Athletic active;
        private long startedAt = -1L;
        private Athletic.Checkpoint checkpoint;
        private Map<Athletic.Checkpoint, Long> laps = new HashMap<>();
        private long lastLogoutTime = -1L;
        private long diffTime = 0L;
        private Map<Integer, String> inventory = new HashMap<>();

        public PlayerProgress(UUID player) {
            this.playerUniqueId = player;
        }

        private PlayerProgress(UUID player, Athletic activeAthletic, long startedAt, Athletic.Checkpoint checkpoint, Map<Athletic.Checkpoint, Long> laps, long lastLogoutTime, long diffTime, Map<Integer, String> inventory) {
            this.playerUniqueId = player;
            this.active = activeAthletic;
            this.startedAt = startedAt;
            this.checkpoint = checkpoint;
            this.laps = laps;
            this.lastLogoutTime = lastLogoutTime;
            this.diffTime = diffTime;
            this.inventory = inventory;
        }

        public boolean isActive() {
            return this.active != null;
        }

        @Nullable
        public Athletic getActiveAthletic() {
            return this.active;
        }

        public long getStartedAt() {
            return this.startedAt;
        }

        @Nullable
        public Athletic.Checkpoint getCheckpoint() {
            return this.checkpoint;
        }

        public Map<Athletic.Checkpoint, Long> getLaps() {
            return this.laps;
        }

        public long getLastLogoutTime() {
            return this.lastLogoutTime;
        }

        public long getDiffTime() {
            return this.diffTime;
        }

        public Map<Integer, String> getSavedInventory() {
            return this.inventory;
        }

        public void startAthletic(Athletic athletic) {
            if (Bukkit.getOfflinePlayer(this.playerUniqueId).isOnline()) {
                this.active = athletic;
                this.startedAt = System.currentTimeMillis();
                this.checkpoint = null;
                this.laps.clear();
                this.lastLogoutTime = -1L;
                this.diffTime = 0L;
                this.inventory.clear();
                Player player = Bukkit.getPlayer(this.playerUniqueId);
                if (player != null) {
                    Inventory playerInventory = ((CraftInventoryPlayer) player.getInventory()).getInventory();
                    List<ItemStack> contents = playerInventory.getContents();
                    for (int i = 0; i < contents.size(); i++) {
                        this.inventory.put(i, MinecraftAdapter.ItemStack.json(contents.get(i)));
                    }
                    this.save();
                    playerInventory.clearContent();
                }
            }
        }

        public void lap(Athletic.Checkpoint checkpoint) {
            if (this.active != null) {
                this.laps.put(checkpoint, System.currentTimeMillis());
                this.checkpoint = checkpoint;
                this.save();
            }
        }

        public void logout() {
            if (this.active != null) {
                this.lastLogoutTime = System.currentTimeMillis();
                this.save();
            }
        }

        public void login() {
            if (this.active != null && this.lastLogoutTime != -1L) {
                this.diffTime += System.currentTimeMillis() - this.lastLogoutTime;
                this.lastLogoutTime = -1L;
                this.save();
            }
        }

        public long finish() {
            if (Bukkit.getOfflinePlayer(this.playerUniqueId).isOnline() && this.active != null) {
                long finishTime = System.currentTimeMillis() - this.diffTime; // 終了時刻ミリ秒から差分ミリ秒を引く
                long elapsedTime = finishTime - this.startedAt; // 経過(ミリ秒)
                Player bukkitPlayer = Bukkit.getPlayer(this.playerUniqueId);
                if (bukkitPlayer != null) {
                    ServerPlayer player = MinecraftAdapter.player(bukkitPlayer);
                    if (player != null) {
                        player.getInventory().clearContent();
                        this.inventory.forEach((slot, item) -> {
                            ItemStack itemStack = MinecraftAdapter.ItemStack.json(item);
                            if (itemStack != null) {
                                player.getInventory().setItem(slot, itemStack);
                            }
                        });
                        this.inventory.clear();
                        this.active = null;
                        this.startedAt = -1L;
                        this.checkpoint = null;
                        this.laps.clear();
                        this.lastLogoutTime = -1L;
                        this.diffTime = 0L;
                        this.save();
                        return elapsedTime;
                    }
                }
            }
            return -1;
        }

        public static PlayerProgress load(UUID playerUniqueId) {
            try {
                File dataFile = new File(DATA_PATH, playerUniqueId + ".yml");
                if ((dataFile.getParentFile().exists() || dataFile.getParentFile().mkdirs()) && (dataFile.exists() || dataFile.createNewFile())) {
                    ConfigurationSection config = YamlConfiguration.loadConfiguration(dataFile);
                    if (config.contains(UnknownNetworkCore.getEnvironment().name())) {
                        config = config.getConfigurationSection(UnknownNetworkCore.getEnvironment().name());

                        Athletic activeAthletic = config.contains("athletic") ? getAthletic(UUID.fromString(config.getString("athletic", Util.NIL_UUID.toString()))) : null;
                        if (activeAthletic != null && config.contains("started-at")) {
                            long startedAt = config.getLong("started-at", -1);

                            Athletic.Checkpoint checkpoint = config.contains("checkpoint") ? activeAthletic.getCheckpoint(UUID.fromString(config.getString("checkpoint", Util.NIL_UUID.toString()))) : null;

                            Map<Athletic.Checkpoint, Long> laps = new HashMap<>();
                            if (config.contains("laps")) {
                                ConfigurationSection lapsSection = config.getConfigurationSection("laps");
                                if (lapsSection != null) {
                                    lapsSection.getKeys(false).forEach(checkpointUniqueIdStr -> {
                                        Athletic.Checkpoint lapCheckpoint = activeAthletic.getCheckpoint(UUID.fromString(checkpointUniqueIdStr));
                                        if (lapCheckpoint != null) {
                                            laps.put(lapCheckpoint, lapsSection.getLong("laps." + checkpointUniqueIdStr, -1));
                                        }
                                    });
                                }
                            }

                            long lastLogoutTime = config.getLong("last-logout-time", -1);
                            long diffTime = config.getLong("diff-time", 0);

                            Map<Integer, String> savedInventory = new HashMap<>();
                            ConfigurationSection savedInventorySection = config.getConfigurationSection("inventory");
                            if (savedInventorySection != null) {
                                savedInventorySection.getKeys(false).forEach(slot -> {
                                    if (savedInventorySection.isString(slot)) {
                                        savedInventory.put(Integer.parseInt(slot), savedInventorySection.getString(slot));
                                    }
                                });
                            }

                            return new PlayerProgress(playerUniqueId, activeAthletic, startedAt, checkpoint, laps, lastLogoutTime, diffTime, savedInventory);
                        }
                    }
                }
            } catch(IOException e) {
                LOGGER.warning("Failed to load player progress for " + playerUniqueId);
                e.printStackTrace();
            }

            // nothing to load, return null instance
            return new PlayerProgress(playerUniqueId);
        }

        public synchronized void save() {
            File dataFile = new File(DATA_PATH, playerUniqueId + ".yml");
            FileConfiguration config = YamlConfiguration.loadConfiguration(dataFile);
            config.set(UnknownNetworkCore.getEnvironment().name(), null); // clear current environment state data
            if (this.active != null) {
                ConfigurationSection root = config.createSection(UnknownNetworkCore.getEnvironment().name());

                root.set("athletic", this.active.getUniqueId().toString());
                root.set("started-at", this.startedAt);
                root.set("checkpoint", this.checkpoint != null ? this.checkpoint.getUniqueId().toString() : null);

                ConfigurationSection lapsSection = root.createSection("laps");
                this.laps.forEach((checkpoint, lapTime) -> {
                    lapsSection.set(checkpoint.getUniqueId().toString(), lapTime);
                });

                root.set("last-logout-time", this.lastLogoutTime);
                root.set("diff-time", this.diffTime);

                ConfigurationSection savedInventorySection = root.createSection("saved-inventory");
                this.inventory.forEach((slot, item) -> {
                    savedInventorySection.set(String.valueOf(slot), item);
                });
            }

            try {
                config.save(dataFile);
            } catch (IOException e) {
                LOGGER.warning("Failed to save player progress for " + this.playerUniqueId);
                e.printStackTrace();
            }
        }
    }

    public static class Listener implements org.bukkit.event.Listener {
        @EventHandler
        public void onPlayerQuit(PlayerQuitEvent event) {
            Player player = event.getPlayer();
            PlayerProgress playerProgress = Athletics.getProgress(player);
            if (playerProgress != null && playerProgress.isActive()) {
                playerProgress.logout();
            }
        }

        @EventHandler
        public void onPlayerJoin(PlayerJoinEvent event) {
            Player player = event.getPlayer();
            PlayerProgress playerProgress = Athletics.getProgress(player);
            if (playerProgress != null && playerProgress.isActive()) {
                playerProgress.login();
            }
        }
    }
}
