/*
 * Copyright (c) 2026 Unknown Network Developers and contributors.
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

package net.unknown.survival.feature;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import net.unknown.core.define.DefinedTextColor;
import net.unknown.core.managers.ListenerManager;
import net.unknown.core.managers.RunnableManager;
import net.unknown.shared.SharedConstants;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.MemoryConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.craftbukkit.util.CraftMagicNumbers;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class AdminStorage implements InventoryHolder {
    private static final String DATA_FILE_NAME = "admin_storage.yml";
    private static final File DATA_FILE = new File(SharedConstants.DATA_FOLDER, DATA_FILE_NAME);

    private static final Comparator<StorageIdentifier> PRIORITY_COMPARATOR = Comparator.comparingInt(StorageIdentifier::priority);
    private static final Comparator<StorageIdentifier> CREATED_AT_COMPARATOR = Comparator.comparingLong(StorageIdentifier::createdAt);

    private static final AdminStorage INSTANCE = new AdminStorage();
    private static final Logger LOGGER = LoggerFactory.getLogger("UNC/AdminStorage");

    private final Inventory EMPTY_INVENTORY = Bukkit.createInventory(this, 54, Component.text("ERROR!", DefinedTextColor.DARK_RED));
    private final TreeMap<StorageIdentifier, Inventory> inventories = new TreeMap<>(PRIORITY_COMPARATOR.thenComparing(CREATED_AT_COMPARATOR));
    private final InventoryViewHandler viewHandler = new InventoryViewHandler();
    private YamlConfiguration config;

    private AdminStorage() {
        ListenerManager.registerListener(this.viewHandler);
        RunnableManager.runAsyncRepeating(this::save, (20 * 60) * 5, (20 * 60) * 5);
    }

    public static AdminStorage getInstance() {
        return INSTANCE;
    }

    public synchronized void load() {
        this.inventories.clear();

        if (!DATA_FILE.exists()) {
            this.config = new YamlConfiguration();
            LOGGER.warn("Admin storage data file does not exist, skipping load");
            return;
        }
        this.config = YamlConfiguration.loadConfiguration(DATA_FILE);

        this.config.getKeys(false).forEach(identifierKey -> {
            ConfigurationSection inventorySection = this.config.getConfigurationSection(identifierKey);
            if (inventorySection != null) {
                StorageIdentifier identifier = StorageIdentifier.fromConfig(inventorySection);

                Inventory inventory = Bukkit.createInventory(this, identifier.size(), identifier.displayName());

                ConfigurationSection contentsSection = inventorySection.getConfigurationSection("contents");
                contentsSection.getKeys(false).forEach(slotStr -> {
                    String itemJsonStr = contentsSection.getString(slotStr);
                    if (itemJsonStr != null) {
                        JsonElement itemJson = JsonParser.parseString(itemJsonStr);

                        ItemStack item = CraftMagicNumbers.INSTANCE.deserializeItemFromJson(itemJson.getAsJsonObject());

                        inventory.setItem(Integer.parseInt(slotStr), item);
                    }
                });

                this.inventories.put(identifier, inventory);
                LOGGER.info("Inventory {} loaded.", identifier.identifier());
            }
        });

        this.applyInventoryTitles();
    }

    public synchronized void save() {
        this.inventories.forEach((identifier, inventory) -> {
            ConfigurationSection inventorySection = identifier.toConfig();

            ConfigurationSection contentsSection = inventorySection.createSection("contents");
            for (int i = 0; i < (inventory.getSize()); i++) {
                ItemStack item = inventory.getItem(i);
                if (item != null) {
                    contentsSection.set(String.valueOf(i), CraftMagicNumbers.INSTANCE.serializeItemAsJson(item).toString());
                }
            }

            this.config.set(identifier.identifier(), inventorySection);
        });

        try {
            this.config.save(DATA_FILE);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to save", e);
        }
    }

    public TreeMap<StorageIdentifier, Inventory> getInventories() {
        return this.inventories;
    }

    public boolean hasInventory(String identifierStr) {
        return this.inventories.keySet().stream()
                .anyMatch(id -> id.identifier().equals(identifierStr));
    }

    public Map.Entry<StorageIdentifier, Inventory> getInventory(String identifierStr) {
        return this.inventories.entrySet().stream()
                .filter(entry -> entry.getKey().identifier().equals(identifierStr))
                .findFirst()
                .orElse(null);
    }

    public void addInventory(String identifierStr, Component displayName, int priority, int size) {
        StorageIdentifier identifier = new StorageIdentifier(identifierStr, displayName, System.currentTimeMillis(), priority, size);
        if (this.hasInventory(identifierStr)) {
            throw new IllegalArgumentException("Inventory with identifier " + identifierStr + " already exists!");
        }

        this.inventories.put(identifier, Bukkit.createInventory(this, size, displayName));
        RunnableManager.runAsync(this::save);
        this.applyInventoryTitles();
    }

    public void removeInventory(String identifierStr) {
        StorageIdentifier identifier = this.inventories.keySet().stream()
                .filter(id -> id.identifier().equals(identifierStr))
                .findFirst()
                .orElse(null);

        if (identifier != null) {
            this.inventories.remove(identifier);
            RunnableManager.runAsync(this::save);
            this.applyInventoryTitles();
        } else {
            throw new IllegalArgumentException("Inventory with identifier " + identifierStr + " does not exist!");
        }
    }

    public void openInventory(Player player) {
        player.openInventory(this.inventories.firstEntry().getValue());
    }

    public StorageIdentifier getCurrentIdentifier(Inventory inventory) {
        return this.inventories.entrySet().stream()
                .filter(entry -> entry.getValue().equals(inventory))
                .map(Map.Entry::getKey)
                .findFirst()
                .orElse(null);
    }

    public Inventory getPreviousInventory(Inventory current) {
        if (current.getHolder() instanceof AdminStorage) {
            return this.inventories.descendingMap().values().stream()
                    .dropWhile(inv -> !inv.equals(current))
                    .skip(1)
                    .findFirst()
                    .orElse(null);
        } else {
            throw new IllegalArgumentException("Not a valid inventory, does not belong to AdminStorage");
        }
    }

    public Inventory getNextInventory(Inventory current) {
        if (current.getHolder() instanceof AdminStorage) {
            return this.inventories.values().stream()
                    .dropWhile(inv -> !inv.equals(current))
                    .skip(1)
                    .findFirst()
                    .orElse(null);
        } else {
            throw new IllegalArgumentException("Not a valid inventory, does not belong to AdminStorage");
        }
    }

    public void applyInventoryTitles() {
        int totalPages = this.inventories.size();
        int currentPage = 1;

        for (Map.Entry<StorageIdentifier, Inventory> entry : this.inventories.entrySet()) {
            Component newTitle = Component.empty()
                    .append(Component.text("[" + currentPage + "/" + totalPages + "]", DefinedTextColor.DARK_PURPLE, TextDecoration.BOLD))
                    .appendSpace()
                    .append(Component.text("[運営倉庫]", NamedTextColor.DARK_AQUA, TextDecoration.BOLD))
                    .appendSpace()
                    .append(entry.getKey().displayName());

            Inventory oldInventory = entry.getValue();
            List<HumanEntity> oldInventoryViewers = List.copyOf(oldInventory.getViewers());

            Inventory newInventory = Bukkit.createInventory(this, oldInventory.getSize(), newTitle);

            oldInventoryViewers.forEach(HumanEntity::closeInventory); // 変更を防ぐため、強制的に閉じさせておく
            newInventory.setContents(oldInventory.getContents()); // 内容を新しいインベントリにコピーする

            entry.setValue(newInventory); // 新しいタイトルが適用された新しいインベントリをセットする
            oldInventoryViewers.forEach(viewer -> viewer.openInventory(newInventory)); // 新しいインベントリを開く
            currentPage++;
        }
    }

    @Override
    public @NotNull Inventory getInventory() {
        return Optional.ofNullable(this.inventories.firstEntry()).map(Map.Entry::getValue).orElse(EMPTY_INVENTORY);
    }

    public static class InventoryViewHandler implements Listener {
        @EventHandler
        public void onInventoryClick(InventoryClickEvent event) {
            if (event.getInventory().getHolder() instanceof AdminStorage) {
                if (event.getSlotType() == InventoryType.SlotType.OUTSIDE && event.isLeftClick()) {
                    Optional.ofNullable(AdminStorage.INSTANCE.getPreviousInventory(event.getInventory())).ifPresent(prevInventory -> {
                        event.getWhoClicked().openInventory(prevInventory);
                    });
                } else if (event.getSlotType() == InventoryType.SlotType.OUTSIDE && event.isRightClick()) {
                    Optional.ofNullable(AdminStorage.INSTANCE.getNextInventory(event.getInventory())).ifPresent(nextInventory -> {
                        event.getWhoClicked().openInventory(nextInventory);
                    });
                }
            }
        }
    }

    public static final class StorageIdentifier {
        private final String identifier;
        private Component displayName;
        private final long createdAt;
        private int priority;
        private final int size;

        public StorageIdentifier(String identifier, Component displayName, long createdAt, int priority, int size) {
            this.identifier = identifier;
            this.displayName = displayName;
            this.createdAt = createdAt;
            this.priority = priority;
            this.size = size;
        }

        public static StorageIdentifier fromConfig(ConfigurationSection config) {
            if (!config.isSet("identifier") || !config.isSet("display-name") || !config.isSet("created-at") || !config.isSet("priority")) {
                throw new IllegalArgumentException("Invalid configuration section for StorageIdentifier");
            }

            String identifier = config.getString("identifier");
            Component displayName = GsonComponentSerializer.gson().deserialize(config.getString("display-name"));
            long createdAt = config.getLong("created-at");
            int priority = config.getInt("priority");
            int size = config.isSet("size") ?  config.getInt("size") : 54;
            return new StorageIdentifier(identifier, displayName, createdAt, priority, size);
        }

        public ConfigurationSection toConfig() {
            MemoryConfiguration config = new MemoryConfiguration();
            config.set("identifier", this.identifier);
            config.set("display-name", GsonComponentSerializer.gson().serialize(this.displayName));
            config.set("created-at", this.createdAt);
            config.set("priority", this.priority);
            if (this.size != 54) config.set("size", this.size);
            return config;
        }

        @Override
        public int hashCode() {
            return this.identifier.hashCode();
        }

        public String identifier() {
            return identifier;
        }

        public Component displayName() {
            return displayName;
        }

        public void displayName(Component displayName) {
            this.displayName = displayName;
        }

        public long createdAt() {
            return createdAt;
        }

        public int priority() {
            return priority;
        }

        public void priority(int priority) {
            this.priority = priority;
        }

        public int size() {
            return this.size;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == this) return true;
            if (obj == null || obj.getClass() != this.getClass()) return false;
            var that = (StorageIdentifier) obj;
            return Objects.equals(this.identifier, that.identifier);
        }

        @Override
        public String toString() {
            return "StorageIdentifier[" +
                    "identifier=" + identifier + ", " +
                    "displayName=" + displayName + ", " +
                    "createdAt=" + createdAt + ", " +
                    "priority=" + priority + ", " +
                    "size=" + size + ']';
        }

    }
}
