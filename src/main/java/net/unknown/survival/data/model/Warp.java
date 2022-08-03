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

package net.unknown.survival.data.model;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import net.unknown.annotations.MethodsNonnullByDefault;
import net.unknown.core.configurations.ConfigurationSerializer;
import net.unknown.survival.data.Warps;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.configuration.serialization.ConfigurationSerialization;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@ParametersAreNonnullByDefault
@MethodsNonnullByDefault
public class Warp {
    private final String internalName;
    private final Component displayName;
    private final Location location;
    private Material icon;
    private final UUID createdBy;
    private long expiresIn;

    public Warp(ConfigurationSection data) {
        if(!data.isSet("internal_name") || !data.isSet("display_name") || !data.isSet("location")) {
            throw new IllegalArgumentException("Illegal object provided");
        }

        this.internalName = data.getString("internal_name");
        this.displayName = GsonComponentSerializer.gson().deserialize(data.getString("display_name"));
        this.location = ConfigurationSerializer.getLocationData(data, "location");

        if (this.internalName == null || this.displayName == null || this.location == null) {
            throw new RuntimeException("Illegal object provided");
        }

        this.icon = data.isSet("icon") ? Material.valueOf(data.getString("icon")) : null;
        this.createdBy = data.isSet("created_by") ? UUID.fromString(data.getString("created_by")) : null;
        this.expiresIn = data.isSet("expires_in") ? data.getLong("expires_in") : -1;
    }

    public Warp(String internalName, Component displayName, Location location, @Nullable Material icon, @Nullable UUID createdBy, long expiresIn) {
        this.internalName = internalName;
        this.displayName = displayName;
        this.location = location;
        this.icon = icon;
        this.createdBy = createdBy;
        this.expiresIn = expiresIn;
    }

    public String getInternalName() {
        return internalName;
    }

    public Component getDisplayName() {
        return displayName;
    }

    public Location getLocation() {
        return location;
    }

    @Nullable
    public Material getIcon() {
        return icon;
    }

    public Material setIcon(Material newIcon) {
        Material oldIcon = this.icon;
        this.icon = newIcon;
        Warps.save(true);
        return oldIcon;
    }

    @Nullable
    public UUID getCreatedBy() {
        return createdBy;
    }

    public long getExpiresIn() {
        return expiresIn;
    }

    public long setExpiresIn(long newExpiresIn) {
        long oldExpiresIn = this.expiresIn;
        this.expiresIn = newExpiresIn;
        Warps.save(true);
        return oldExpiresIn;
    }

    public void write(ConfigurationSection rootSection) {
        ConfigurationSection data = rootSection.createSection(internalName);
        data.set("internal_name", this.internalName);
        data.set("display_name", GsonComponentSerializer.gson().serialize(this.displayName));
        ConfigurationSerializer.setLocationData(data, "location", this.location);
        if(this.icon != null) data.set("icon", this.icon.name());
        if(this.createdBy != null) data.set("created_by", this.createdBy.toString());
        if(this.expiresIn > 0) data.set("expires_in", this.expiresIn);
    }
}
