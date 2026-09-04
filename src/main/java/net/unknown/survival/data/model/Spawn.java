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

package net.unknown.survival.data.model;

import com.ryuuta0217.util.LocationRef;
import javax.annotation.Nullable;
import net.kyori.adventure.text.Component;
import net.minecraft.resources.Identifier;
import net.unknown.core.managers.RunnableManager;
import net.unknown.survival.data.Spawns;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.inventory.ItemStack;

public class Spawn {
    private final Identifier identifier;
    private String name;
    private Component displayName;
    private Component description;
    private LocationRef location;
    private ItemStack icon;

    public Spawn(Identifier identifier, String name, Component displayName, Component description, LocationRef location, @Nullable ItemStack icon) {
        this.identifier = identifier;
        this.name = name;
        this.displayName = displayName;
        this.description = description;
        this.location = location;
        this.icon = icon;
    }

    public Identifier getIdentifier() {
        return this.identifier;
    }

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
        this.write();
    }

    public Component getDisplayName() {
        return this.displayName;
    }

    public void setDisplayName(Component displayName) {
        this.displayName = displayName;
        this.write();
    }

    public Component getDescription() {
        return this.description;
    }

    public void setDescription(Component description) {
        this.description = description;
        this.write();
    }

    public LocationRef getLocation() {
        return this.location;
    }

    public void setLocation(LocationRef location) {
        this.location = location;
        this.write();
    }

    public void setLocation(Location location) {
        this.setLocation(new LocationRef(location));
    }

    public void setLocation(String worldName, double x, double y, double z, float yaw, float pitch) {
        this.setLocation(new LocationRef(worldName, x, y, z, yaw, pitch));
    }

    public void setLocation(World world, double x, double y, double z, float yaw, float pitch) {
        this.setLocation(world.getName(), x, y, z, yaw, pitch);
    }

    @Nullable
    public ItemStack getIcon() {
        return this.icon;
    }

    public void setIcon(@Nullable ItemStack icon) {
        this.icon = icon;
        this.write();
    }

    private void write() {
        RunnableManager.runAsync(() -> Spawns.setSpawn(this));
    }
}
