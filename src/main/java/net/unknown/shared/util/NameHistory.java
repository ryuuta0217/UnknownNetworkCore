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

package net.unknown.shared.util;

import net.unknown.core.managers.ListenerManager;
import net.unknown.shared.SharedConstants;
import org.bukkit.Bukkit;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerQuitEvent;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.*;

public class NameHistory {
    private static final Logger LOGGER = LoggerFactory.getLogger("UNC/NameHistory");
    private static final File NAME_HISTORY_FILE = new File(SharedConstants.DATA_FOLDER, "name_history.json");
    private static final Map<UUID, Map<String, Long>> NAME_HISTORY = new HashMap<>();

    static {
        ListenerManager.registerEventListener(PlayerQuitEvent.class, null, EventPriority.MONITOR, false, (l, e) -> {
            if (e instanceof PlayerQuitEvent event) {
                updateLastSeen(event.getPlayer().getUniqueId(), event.getPlayer().getName(), System.currentTimeMillis());
            }
        });
    }

    public static void loadNameHistory() {
        NAME_HISTORY.clear();
        try {
            if ((NAME_HISTORY_FILE.getParentFile().exists() || NAME_HISTORY_FILE.getParentFile().mkdirs()) && (NAME_HISTORY_FILE.exists() || NAME_HISTORY_FILE.createNewFile())) {
                String jsonStr = String.join("\n", Files.readAllLines(NAME_HISTORY_FILE.toPath()));
                JSONObject json = new JSONObject(jsonStr);
                json.keySet().forEach(uuidStr -> {
                    UUID uuid = UUID.fromString(uuidStr);

                    JSONObject nameHistoryJson = json.getJSONObject(uuidStr);
                    Map<String, Long> nameHistory = new HashMap<>();
                    nameHistoryJson.keySet().forEach(name -> nameHistory.put(name, nameHistoryJson.getLong(name)));

                    NAME_HISTORY.put(uuid, nameHistory);
                });
            }
        } catch (Throwable t) {
            LOGGER.error("Failed to load name history", t);
        }
    }

    public static Map<String, Long> getNameHistory(UUID uniqueId) {
        return NAME_HISTORY.getOrDefault(uniqueId, Collections.emptyMap());
    }

    public static void updateLastSeen(UUID uniqueId, String playerName, long lastSeen) {
        NAME_HISTORY.computeIfAbsent(uniqueId, k -> new HashMap<>()).put(playerName, lastSeen);
    }

    public static synchronized void save() {
        try {
            JSONObject json = new JSONObject(NAME_HISTORY);
            Files.write(NAME_HISTORY_FILE.toPath(), Collections.singleton(json.toString()));
        } catch (IOException e) {
            LOGGER.error("Failed to save name history", e);
        }
    }
}
