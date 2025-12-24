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

import com.ryuuta0217.util.LocationRef;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import net.minecraft.resources.ResourceLocation;
import net.unknown.core.configurations.ConfigurationBase;
import net.unknown.core.configurations.ConfigurationSerializer;
import net.unknown.core.managers.RunnableManager;
import net.unknown.survival.data.model.Village;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;

public class Villages extends ConfigurationBase {
    private static final Villages INSTANCE = new Villages();

    private Map<ResourceLocation, Village> villages;
    private Set<ResourceLocation> dirtyVillages;

    private Villages() {
        super("villages.yml", false, "UNC/Villages");
    }

    @Override
    public void onLoad() {
        this.villages = new HashMap<>();
        this.dirtyVillages = new HashSet<>();

        this.getConfig().getKeys(false).forEach(identifierStr -> {
            ConfigurationSection villageRawData = this.getConfig().getConfigurationSection(identifierStr);
            if (villageRawData != null) {
                try {
                    ResourceLocation identifier = ResourceLocation.parse(identifierStr);
                    String name = villageRawData.getString("name", "不明な村");
                    Component displayName = GsonComponentSerializer.gson().deserialize(villageRawData.getString("display_name", "{}"));
                    Component description = GsonComponentSerializer.gson().deserialize(villageRawData.getString("description", "{}"));
                    LocationRef location = ConfigurationSerializer.getLocationRefData(villageRawData, "location");
                    Village village = new Village(identifier, name, displayName, description, location);
                    this.villages.put(identifier, village);
                } catch(Throwable t) {
                    this.getLogger().warning("Failed to parse village for identifier: " + identifierStr);
                }
            } else {
                this.getLogger().warning("Invalid village data for identifier: " + identifierStr);
            }
        });
    }

    public static Map<ResourceLocation, Village> getVillages() {
        return Collections.unmodifiableMap(Villages.getInstance().villages);
    }

    @Nullable
    public static Village getVillage(ResourceLocation identifier) {
        return Villages.getInstance().villages.getOrDefault(identifier, null);
    }

    @Nullable
    public static Village getVillage(String identifier) {
        return getVillage(ResourceLocation.parse(identifier));
    }

    @Nullable
    public static Village getVillage(String namespace, String path) {
        return getVillage(namespace + ":" + path);
    }

    @Nullable
    public static Village getVillageByName(String name) {
        return getVillages().values().parallelStream().filter(village -> village.getName().equalsIgnoreCase(name)).findFirst().orElse(null);
    }

    public static void setVillage(ResourceLocation identifier, Village village) {
        Villages.getInstance().villages.put(identifier, village);
        Villages.getInstance().dirtyVillages.add(identifier);
        save(false, true);
    }

    public static void setVillage(Village village) {
        setVillage(village.getIdentifier(), village);
    }

    private static void addVillage(Village village) {
        Villages.getInstance().villages.put(village.getIdentifier(), village);
        Villages.getInstance().dirtyVillages.add(village.getIdentifier());
        save(false, true);
    }

    public static void addVillage(ResourceLocation identifier, String name, Component displayName, Component description, LocationRef location) {
        addVillage(new Village(identifier, name, displayName, description, location));
    }

    public static void addVillage(ResourceLocation identifier, String name, Component displayName, Component description, Location location) {
        addVillage(identifier, name, displayName, description, new LocationRef(location));
    }

    public static void addVillage(String identifier, String name, Component displayName, Component description, LocationRef location) {
        addVillage(ResourceLocation.parse(identifier), name, displayName, description, location);
    }

    public static void addVillage(String identifier, String name, Component displayName, Component description, Location location) {
        addVillage(ResourceLocation.parse(identifier),name, displayName, description, new LocationRef(location));
    }

    public static void addVillage(String namespace, String path, String name, Component displayName, Component description, LocationRef location) {
        addVillage(ResourceLocation.parse(namespace + ":" + path), name, displayName, description, location);
    }

    public static void addVillage(String namespace, String path, String name, Component displayName, Component description, Location location) {
        addVillage(ResourceLocation.parse(namespace + ":" + path), name, displayName, description, new LocationRef(location));
    }

    public static void removeVillage(ResourceLocation identifier) {
        Villages.getInstance().villages.remove(identifier);
        Villages.getInstance().dirtyVillages.add(identifier);
        save(false, true);
    }

    public static void removeVillage(String identifier) {
        removeVillage(ResourceLocation.parse(identifier));
    }

    public static void removeVillage(String namespace, String path) {
        removeVillage(ResourceLocation.parse(namespace + ":" + path));
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
    
    public static Villages getInstance() {
        return INSTANCE;
    }

    private void writeToConfig(@Nonnull ResourceLocation identifier, @Nullable Village village) {
        String identifierStr = identifier.toString();

        if (village == null) {
            this.getConfig().set(identifierStr, null);
        } else {
            ConfigurationSection villageRawData;
            if (this.getConfig().isConfigurationSection(identifierStr)) {
                villageRawData = this.getConfig().getConfigurationSection(identifierStr);
            } else {
                villageRawData = this.getConfig().createSection(identifierStr);
            }

            if (villageRawData != null) {
                villageRawData.set("name", village.getName());
                villageRawData.set("display_name", GsonComponentSerializer.gson().serialize(village.getDisplayName()));
                villageRawData.set("description", GsonComponentSerializer.gson().serialize(village.getDescription()));
                ConfigurationSerializer.setLocationData(villageRawData, "location", village.getLocation());
            }
        }
    }

    @Override
    public synchronized void save() {
        this.villages.forEach(this::writeToConfig);
        super.save();
        this.dirtyVillages = new HashSet<>();
    }

    public synchronized void saveOnlyModified() {
        this.dirtyVillages.forEach(identifier -> {
            Village village = this.villages.getOrDefault(identifier, null);
            this.writeToConfig(identifier, village);
        });
        super.save();
        this.dirtyVillages = new HashSet<>();
    }
}
