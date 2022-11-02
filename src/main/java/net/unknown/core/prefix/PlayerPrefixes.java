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

package net.unknown.core.prefix;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import net.unknown.core.managers.RunnableManager;
import net.unknown.core.util.MessageUtil;
import net.unknown.shared.SharedConstants;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.List;
import java.util.logging.Logger;

public class PlayerPrefixes {
    public static final File DATA_FOLDER = new File(SharedConstants.DATA_FOLDER, "prefixes");
    private static final Logger LOGGER = Logger.getLogger("UNC/Prefixes");
    private static final Map<UUID, Set<Prefix>> PREFIXES = new HashMap<>();

    public static void loadAll() {
        if (DATA_FOLDER.exists() || DATA_FOLDER.mkdirs()) {
            File[] searchResult = DATA_FOLDER.listFiles((file) -> file.getName().endsWith(".yml"));
            if (searchResult != null) {
                for (File file : searchResult) {
                    String fileName = file.getName().replace(".yml", "");
                    if (MessageUtil.isUUID(fileName)) {
                        UUID uniqueId = UUID.fromString(fileName);
                        PlayerPrefixes.load(uniqueId);
                    }
                }
            }
        }
    }

    public static void load(UUID uniqueId) {
        long start = System.nanoTime();
        try {
            PREFIXES.remove(uniqueId);
            PREFIXES.put(uniqueId, new HashSet<>());
            File configFile = new File(DATA_FOLDER, uniqueId + ".yml");
            if (configFile.exists() || configFile.createNewFile()) {
                FileConfiguration config = YamlConfiguration.loadConfiguration(configFile);
                config.getKeys(false).forEach(createdAtStr -> {
                    long createdAt = Long.parseLong(createdAtStr);
                    String prefixJson = config.getConfigurationSection(createdAtStr).getString("prefix");
                    Component prefix = GsonComponentSerializer.gson().deserialize(prefixJson);
                    boolean active = config.getConfigurationSection(createdAtStr).getBoolean("active");
                    boolean temporary = config.getConfigurationSection(createdAtStr).getBoolean("temporary");
                    Prefix prefixContainer = new Prefix(prefix, createdAt, temporary, active);
                    PREFIXES.get(uniqueId).add(prefixContainer);
                });
            }
        } catch(IOException e) {
            LOGGER.warning("Failed to load prefixes for player " + uniqueId);
            e.printStackTrace();
            return;
        }
        long end = System.nanoTime();
        long elapsedNano = end - start;
        LOGGER.info("Player " + uniqueId + " prefix loaded with " + elapsedNano + "ns (" + elapsedNano/1000000 + "ms)");
    }

    public static void save(UUID uniqueId) {
        FileConfiguration config = new YamlConfiguration();

        Set<Prefix> prefixes = PREFIXES.get(uniqueId);
        if (prefixes != null) {
            prefixes.forEach(prefix -> {
                ConfigurationSection section = config.createSection(String.valueOf(prefix.getCreatedAt()));
                section.set("prefix", GsonComponentSerializer.gson().serialize(prefix.getPrefix()));
                section.set("active", prefix.isActive());
                section.set("temporary", prefix.isTemporary());
            });

            try {
                config.save(new File(DATA_FOLDER, uniqueId + ".yml"));
            } catch (IOException e) {
                LOGGER.warning("Failed to save prefixes for player " + uniqueId);
                e.printStackTrace();
            }
        }
    }

    public static boolean setPrefix(UUID uniqueId, Component prefix, boolean temporary) {
        if (!PREFIXES.containsKey(uniqueId)) load(uniqueId);
        PREFIXES.get(uniqueId).removeIf((prefixContainer) -> {
            if (prefixContainer.isActive()) {
                if (prefixContainer.isTemporary()) return true;
                prefixContainer.setActive(false);
            }
            return false;
        });
        Prefix prefixContainer = new Prefix(prefix, System.currentTimeMillis(), temporary);
        PREFIXES.get(uniqueId).add(prefixContainer);
        prefixContainer.setActive(true);
        RunnableManager.runAsync(() -> save(uniqueId));
        return prefixContainer.equals(getActivePrefix(uniqueId));
    }

    public static boolean setPrefix(UUID uniqueId, Prefix prefix) {
        if (!PREFIXES.containsKey(uniqueId)) load(uniqueId);
        if (!PREFIXES.get(uniqueId).contains(prefix)) return false;
        PREFIXES.get(uniqueId).removeIf((prefixContainer) -> { // イテレータ
            if (prefixContainer.isActive()) { // 現在有効な接頭辞の場合
                if (prefixContainer.isTemporary()) return true; // 一時的な接頭辞の場合はtrueを返して削除する
                prefixContainer.setActive(false); // 一時的でない場合二のみ到達する, 現在有効な接頭辞を無効にする
            }
            return false; // デフォルトではfalseを返して削除しない
        });
        prefix.setActive(true);
        RunnableManager.runAsync(() -> save(uniqueId));
        return prefix.equals(getActivePrefix(uniqueId));
    }

    public static Prefix addPrefix(Player player, Component prefix) {
        return addPrefix(player.getUniqueId(), prefix);
    }

    public static Prefix addPrefix(UUID uniqueId, Component prefix) {
        Prefix prefixContainer = new Prefix(prefix, System.currentTimeMillis(), false);
        if (!PREFIXES.containsKey(uniqueId)) load(uniqueId);
        PREFIXES.get(uniqueId).add(prefixContainer);
        RunnableManager.runAsync(() -> save(uniqueId));
        return prefixContainer;
    }

    @Nullable
    public static Prefix getActivePrefix(UUID uniqueId) {
        return getPrefixes(uniqueId).stream().filter(Prefix::isActive).findAny().orElse(null);
    }

    public static Set<Prefix> getPrefixes(UUID uniqueId) {
        return Set.copyOf(PREFIXES.getOrDefault(uniqueId, new HashSet<>()));
    }

    public static List<Prefix> getPrefixesSorted(Player player) {
        return getPrefixesSorted(player.getUniqueId());
    }

    public static List<Prefix> getPrefixesSorted(UUID uniqueId) {
        return getPrefixes(uniqueId)
                .stream()
                .sorted(Comparator.comparingLong(Prefix::getCreatedAt))
                .toList();
    }
}
