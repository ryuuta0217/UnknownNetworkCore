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
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import net.unknown.core.managers.RunnableManager;
import net.unknown.core.util.MessageUtil;
import net.unknown.shared.SharedConstants;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class PlayerPrefixes implements Listener {
    private static final File FOLDER = new File(SharedConstants.DATA_FOLDER, "prefixes");
    private static final Logger LOGGER = Logger.getLogger("UNC/PlayerPrefixes");
    private static final Map<UUID, Component> ACTIVE_PREFIXES = new HashMap<>();
    private static final Map<UUID, Set<Component>> AVAILABLE_PREFIXES = new HashMap<>();

    public static void loadAll() {
        File[] files = FOLDER.listFiles((file) -> file.isFile() && file.getName().endsWith(".yml"));
        if(files == null) return;

        Stream.of(files)
                .filter(file -> MessageUtil.isUUID(file.getName().replace(".yml", "")))
                .map(file -> UUID.fromString(file.getName().replace(".yml", "")))
                .forEach(PlayerPrefixes::load);
    }

    public static void load(Player player) {
        load(player.getUniqueId());
    }

    public static void load(UUID uniqueId) {
        if (Bukkit.getOfflinePlayer(uniqueId).getName() == null) throw new IllegalArgumentException("Illegal UUID provided, player not found.");

        File configFile = new File(FOLDER, uniqueId + ".yml");
        try {
            if (configFile.getParentFile().exists() || configFile.getParentFile().mkdirs()) {
                if (configFile.exists() || configFile.createNewFile()) {
                    YamlConfiguration config = YamlConfiguration.loadConfiguration(configFile);

                    if (config.isSet("current") && config.isString("current")) {
                        ACTIVE_PREFIXES.put(uniqueId, GsonComponentSerializer.gson().deserialize(config.getString("current")));
                    } else {
                        ACTIVE_PREFIXES.put(uniqueId, Component.empty());
                    }

                    if (config.isSet("available") && config.isList("available")) {
                        AVAILABLE_PREFIXES.put(uniqueId, config.getStringList("available").stream().map(GsonComponentSerializer.gson()::deserialize).collect(Collectors.toSet()));
                    } else {
                        AVAILABLE_PREFIXES.put(uniqueId, new LinkedHashSet<>());
                    }
                }
            }
        } catch(IOException e) {
            ACTIVE_PREFIXES.put(uniqueId, Component.empty());
            LOGGER.warning("Failed to load prefixes, player " + Bukkit.getOfflinePlayer(uniqueId).getName() + "'s prefixes is now default.");
            e.printStackTrace();
        }
    }

    public static Component getCurrentPrefix(Player player) {
        return getCurrentPrefix(player.getUniqueId());
    }

    public static Component getCurrentPrefix(UUID uniqueId) {
        return ACTIVE_PREFIXES.getOrDefault(uniqueId, Component.empty());
    }

    public static Set<Component> getAvailablePrefixes(Player player) {
        return getAvailablePrefixes(player.getUniqueId());
    }

    public static Set<Component> getAvailablePrefixes(UUID uniqueId) {
        return Set.copyOf(AVAILABLE_PREFIXES.getOrDefault(uniqueId, new HashSet<>()));
    }

    public static void addAvailablePrefix(Player player, Component prefix) {
        addAvailablePrefix(player.getUniqueId(), prefix);
    }

    public static void addAvailablePrefix(UUID uniqueId, Component prefix) {
        if (!AVAILABLE_PREFIXES.containsKey(uniqueId)) AVAILABLE_PREFIXES.put(uniqueId, new HashSet<>());

        if(AVAILABLE_PREFIXES.get(uniqueId).contains(prefix)) {
            throw new IllegalArgumentException("Prefix " + PlainTextComponentSerializer.plainText().serialize(prefix) + " is already found.");
        }

        AVAILABLE_PREFIXES.get(uniqueId).add(prefix);
        RunnableManager.runAsync(() -> save(uniqueId));
    }

    public static void setPrefix(Player player, Component newPrefix) {
        setPrefix(player.getUniqueId(), newPrefix);
    }

    public static void setPrefix(UUID uniqueId, Component newPrefix) {
        if (ACTIVE_PREFIXES.containsKey(uniqueId) && !ACTIVE_PREFIXES.get(uniqueId).equals(Component.empty())) {
            AVAILABLE_PREFIXES.get(uniqueId).add(ACTIVE_PREFIXES.get(uniqueId));
        }
        ACTIVE_PREFIXES.put(uniqueId, newPrefix);
        AVAILABLE_PREFIXES.get(uniqueId).remove(newPrefix);
        RunnableManager.runAsync(() -> save(uniqueId));
    }

    private static void save(Player player) {
        save(player.getUniqueId());
    }

    private static void save(UUID uniqueId) {
        File configFile = new File(FOLDER, uniqueId + ".yml");
        try {
            if(configFile.getParentFile().exists() || configFile.getParentFile().mkdirs()) {
                if(configFile.exists() || configFile.createNewFile()) {
                    YamlConfiguration config = new YamlConfiguration();
                    config.set("current", GsonComponentSerializer.gson().serialize(ACTIVE_PREFIXES.getOrDefault(uniqueId, Component.empty())));
                    config.set("available", AVAILABLE_PREFIXES.getOrDefault(uniqueId, new HashSet<>()).stream().map(GsonComponentSerializer.gson()::serialize).toList());

                    try {
                        config.save(configFile);
                    } catch (IOException e) {
                        LOGGER.warning("Failed to save prefixes.");
                        e.printStackTrace();
                    }
                }
            }
        } catch(IOException e) {
            LOGGER.warning("Failed to create file or folder.");
            e.printStackTrace();
        }
    }

    public static class Listener implements org.bukkit.event.Listener {
        @EventHandler
        public void onPlayerJoin(PlayerJoinEvent event) {
            load(event.getPlayer());
        }
    }
}
