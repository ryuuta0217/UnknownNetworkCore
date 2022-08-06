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

package net.unknown.core.managers;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.unknown.UnknownNetworkCore;
import net.unknown.core.util.MinecraftAdapter;
import net.unknown.shared.SharedConstants;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

// save to ~/unknown-network/shared/trash/uuid.yml
public class TrashManager {
    private static final int TRASH_BOX_SIZE = 54;
    private static final File DATA_FOLDER = new File(SharedConstants.DATA_FOLDER, "trash");
    private static final Logger LOGGER = LoggerFactory.getLogger("UNC/TrashManager");
    private static final Map<UUID, Map<Integer, ItemStack>> TRASHES = new HashMap<>();

    public static Map<Integer, ItemStack> getItems(UUID uuid) {
        if (!TRASHES.containsKey(uuid)) TRASHES.put(uuid, new HashMap<>(54));
        return TRASHES.get(uuid);
    }

    public static Map<Integer, org.bukkit.inventory.ItemStack> getItemsBukkit(UUID uuid) {
        return getItems(uuid)
                .entrySet()
                .stream()
                .map(e -> Map.entry(e.getKey(), MinecraftAdapter.ItemStack.itemStack(e.getValue())))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    public static void setItem(UUID uuid, int slot, ItemStack minecraft) {
        if (!TRASHES.containsKey(uuid)) TRASHES.put(uuid, new HashMap<>());
        TRASHES.get(uuid).put(slot, minecraft);
        RunnableManager.runAsync(() -> TrashManager.save(uuid));
    }

    public static void setItems(UUID uuid, List<ItemStack> nmsItems) {
        for (int slot = 0; slot < nmsItems.size(); slot++) {
            setItem(uuid, slot, nmsItems.get(slot));
        }
    }

    public static void setItems(UUID uuid, Map<Integer, ItemStack> nmsItems) {
        TRASHES.put(uuid, nmsItems);
        RunnableManager.runAsync(() -> TrashManager.save(uuid));
    }

    public static void clear(UUID uuid) {
        if (TRASHES.containsKey(uuid)) TRASHES.get(uuid).clear();
        TRASHES.put(uuid, new HashMap<>());
    }

    public static void loadExists() {
        TRASHES.clear();
        if (DATA_FOLDER.exists() || DATA_FOLDER.mkdirs()) {
            Arrays.stream(DATA_FOLDER.listFiles())
                    .filter(file -> file.isFile() && file.getName().endsWith(".yml"))
                    .forEach(file -> {
                        UUID uuid = UUID.fromString(file.getName().replace(".yml", ""));
                        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
                        ConfigurationSection items = config.getConfigurationSection("items");
                        items.getKeys(false).forEach(slotStr -> {
                            try {
                                int slot = Integer.parseInt(slotStr);
                                ItemStack item = MinecraftAdapter.ItemStack.json(items.getString(slotStr));
                                setItem(uuid, slot, item);
                            } catch (NumberFormatException ignored) {
                            }
                        });
                    });
        }
    }

    public synchronized static void save(UUID uuid) {
        try {
            File file = new File(DATA_FOLDER, uuid.toString() + ".yml");
            if (file.getParentFile().exists() || file.getParentFile().mkdirs()) {
                if (file.exists() || file.createNewFile()) {
                    YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
                    config.set("items", null);
                    Map<Integer, ItemStack> items = TRASHES.getOrDefault(uuid, new HashMap<>());
                    ConfigurationSection itemsSection = config.createSection("items");
                    items.entrySet()
                            .stream()
                            .filter(e -> !e.getValue().is(Items.AIR))
                            .map(e -> Map.entry(e.getKey(), MinecraftAdapter.ItemStack.json(e.getValue())))
                            .forEach(e -> itemsSection.set(String.valueOf(e.getKey()), e.getValue()));
                    config.save(file);
                }
            }
        } catch (IOException e) {
            LOGGER.error("Failed to save trash items for " + uuid, e);
        }
    }

    public synchronized static void saveAll() {
        TRASHES.keySet().forEach(TrashManager::save);
    }
}
