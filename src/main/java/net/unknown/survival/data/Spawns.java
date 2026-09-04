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

package net.unknown.survival.data;

import com.google.gson.JsonParser;
import com.ryuuta0217.util.LocationRef;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import net.minecraft.resources.Identifier;
import net.unknown.core.configurations.ConfigurationBase;
import net.unknown.core.configurations.ConfigurationSerializer;
import net.unknown.core.managers.RunnableManager;
import net.unknown.survival.data.model.Spawn;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.craftbukkit.util.CraftMagicNumbers;
import org.bukkit.inventory.ItemStack;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;

public class Spawns extends ConfigurationBase {
    private static final Spawns INSTANCE = new Spawns();

    private Map<Identifier, Spawn> spawns;
    private Set<Identifier> dirtySpawns;

    private Spawns() {
        super("spawns.yml", false, "UNC/Spawns");
    }

    @Override
    public void onLoad() {
        this.spawns = new HashMap<>();
        this.dirtySpawns = new HashSet<>();

        this.getConfig().getKeys(false).forEach(identifierStr -> {
            ConfigurationSection spawnRawData = this.getConfig().getConfigurationSection(identifierStr);
            if (spawnRawData != null) {
                try {
                    Identifier identifier = Identifier.parse(identifierStr);
                    String name = spawnRawData.getString("name", "不明なスポーン");
                    Component displayName = GsonComponentSerializer.gson().deserialize(spawnRawData.getString("display_name", "{}"));
                    Component description = GsonComponentSerializer.gson().deserialize(spawnRawData.getString("description", "{}"));
                    
                    ItemStack icon = null;
                    String iconStr = spawnRawData.getString("icon", null);
                    if (iconStr != null) {
                        icon = CraftMagicNumbers.INSTANCE.deserializeItemFromJson(JsonParser.parseString(iconStr).getAsJsonObject());
                    }

                    LocationRef location = ConfigurationSerializer.getLocationRefData(spawnRawData, "location");
                    Spawn spawn = new Spawn(identifier, name, displayName, description, location, icon);
                    this.spawns.put(identifier, spawn);
                } catch(Throwable t) {
                    this.getLogger().warning("Failed to parse spawn for identifier: " + identifierStr);
                }
            } else {
                this.getLogger().warning("Invalid spawn data for identifier: " + identifierStr);
            }
        });
    }

    public static Map<Identifier, Spawn> getSpawns() {
        return Collections.unmodifiableMap(Spawns.getInstance().spawns);
    }

    @Nullable
    public static Spawn getSpawn(Identifier identifier) {
        return Spawns.getInstance().spawns.getOrDefault(identifier, null);
    }

    @Nullable
    public static Spawn getSpawn(String identifier) {
        return getSpawn(Identifier.parse(identifier));
    }

    @Nullable
    public static Spawn getSpawn(String namespace, String path) {
        return getSpawn(namespace + ":" + path);
    }

    @Nullable
    public static Spawn getSpawnByName(String name) {
        return getSpawns().values().parallelStream().filter(spawn -> spawn.getName().equalsIgnoreCase(name)).findFirst().orElse(null);
    }

    public static void setSpawn(Identifier identifier, Spawn spawn) {
        Spawns.getInstance().spawns.put(identifier, spawn);
        Spawns.getInstance().dirtySpawns.add(identifier);
        save(false, true);
    }

    public static void setSpawn(Spawn spawn) {
        setSpawn(spawn.getIdentifier(), spawn);
    }

    private static void addSpawn(Spawn spawn) {
        Spawns.getInstance().spawns.put(spawn.getIdentifier(), spawn);
        Spawns.getInstance().dirtySpawns.add(spawn.getIdentifier());
        save(false, true);
    }

    public static void addSpawn(Identifier identifier, String name, Component displayName, Component description, @Nullable ItemStack icon, LocationRef location) {
        addSpawn(new Spawn(identifier, name, displayName, description, location, icon));
    }

    public static void addSpawn(Identifier identifier, String name, Component displayName, Component description, @Nullable ItemStack icon, Location location) {
        addSpawn(identifier, name, displayName, description, icon, new LocationRef(location));
    }

    public static void addSpawn(String identifier, String name, Component displayName, Component description, @Nullable ItemStack icon, LocationRef location) {
        addSpawn(Identifier.parse(identifier), name, displayName, description, icon, location);
    }

    public static void addSpawn(String identifier, String name, Component displayName, Component description, @Nullable ItemStack icon, Location location) {
        addSpawn(Identifier.parse(identifier), name, displayName, description, icon, new LocationRef(location));
    }

    public static void addSpawn(String namespace, String path, String name, Component displayName, Component description, @Nullable ItemStack icon, LocationRef location) {
        addSpawn(Identifier.parse(namespace + ":" + path), name, displayName, description, icon, location);
    }

    public static void addSpawn(String namespace, String path, String name, Component displayName, Component description, @Nullable ItemStack icon, Location location) {
        addSpawn(Identifier.parse(namespace + ":" + path), name, displayName, description, icon, new LocationRef(location));
    }

    public static void removeSpawn(Identifier identifier) {
        Spawns.getInstance().spawns.remove(identifier);
        Spawns.getInstance().dirtySpawns.add(identifier);
        save(false, true);
    }

    public static void removeSpawn(String identifier) {
        removeSpawn(Identifier.parse(identifier));
    }

    public static void removeSpawn(String namespace, String path) {
        removeSpawn(Identifier.parse(namespace + ":" + path));
    }

    public static void save(boolean completely, boolean async) {
        if (completely) {
            if (async) {
                RunnableManager.runAsync(() -> getInstance().save());
            } else {
                getInstance().save();
            }
        } else {
            if (async) {
                RunnableManager.runAsync(() -> getInstance().saveOnlyModified());
            } else {
                getInstance().saveOnlyModified();
            }
        }
    }
    
    public static Spawns getInstance() {
        return INSTANCE;
    }

    private void writeToConfig(@Nonnull Identifier identifier, @Nullable Spawn spawn) {
        String identifierStr = identifier.toString();

        if (spawn == null) {
            this.getConfig().set(identifierStr, null);
        } else {
            ConfigurationSection spawnRawData;
            if (this.getConfig().isConfigurationSection(identifierStr)) {
                spawnRawData = this.getConfig().getConfigurationSection(identifierStr);
            } else {
                spawnRawData = this.getConfig().createSection(identifierStr);
            }

            if (spawnRawData != null) {
                spawnRawData.set("name", spawn.getName());
                spawnRawData.set("display_name", GsonComponentSerializer.gson().serialize(spawn.getDisplayName()));
                spawnRawData.set("description", GsonComponentSerializer.gson().serialize(spawn.getDescription()));
                
                if (spawn.getIcon() != null) {
                    String serialized = CraftMagicNumbers.INSTANCE.serializeItemAsJson(spawn.getIcon()).toString();
                    spawnRawData.set("icon", serialized);
                } else {
                    spawnRawData.set("icon", null);
                }

                ConfigurationSerializer.setLocationData(spawnRawData, "location", spawn.getLocation());
            }
        }
    }

    @Override
    public synchronized void save() {
        this.spawns.forEach(this::writeToConfig);
        super.save();
        this.dirtySpawns = new HashSet<>();
    }

    public synchronized void saveOnlyModified() {
        this.dirtySpawns.forEach(identifier -> {
            Spawn spawn = this.spawns.getOrDefault(identifier, null);
            this.writeToConfig(identifier, spawn);
        });
        super.save();
        this.dirtySpawns = new HashSet<>();
    }
}
