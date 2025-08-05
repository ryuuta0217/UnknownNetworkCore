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
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

import javax.annotation.Nullable;
import java.lang.ref.Reference;
import java.lang.reflect.Field;
import java.util.Objects;

public final class Home extends LocationRef {
    private final String name;

    public Home(String name, String worldName, double x, double y, double z, float yaw, float pitch) {
        super(worldName, x, y, z, yaw, pitch);
        this.name = name;
    }

    public boolean isAvailable() {
        return this.asLocation() != null;
    }

    public World world() {
        Location location = this.asLocation();
        if (location == null) return null;
        if (location.isWorldLoaded()) return location.getWorld();
        try {
            Field worldField = Location.class.getDeclaredField("world");
            if (worldField.trySetAccessible()) {
                Reference<World> oldWorldRef = (Reference<World>) worldField.get(location);
                if (oldWorldRef.get() != null) {
                    World oldWorld = oldWorldRef.get();
                    World newWorld = Bukkit.getWorld(oldWorld.getName());
                    worldField.set(this.asLocation(), newWorld);
                    return this.asLocation().getWorld();
                } else {
                    throw new IllegalStateException("Object is already garbage collected.");
                }
            }
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new IllegalStateException("World is unloaded.", e);
        }
        throw new IllegalStateException("An internal error occurred when gathering World object.");
    }

    public void teleportPlayer(Player player) {
        player.teleportAsync(this.asLocation());
    }

    public String name() {
        return name;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (Home) obj;
        return Objects.equals(this.name, that.name) &&
                Objects.equals(this.worldName, that.worldName) &&
                Double.doubleToLongBits(this.x) == Double.doubleToLongBits(that.x) &&
                Double.doubleToLongBits(this.y) == Double.doubleToLongBits(that.y) &&
                Double.doubleToLongBits(this.z) == Double.doubleToLongBits(that.z) &&
                Float.floatToIntBits(this.yaw) == Float.floatToIntBits(that.yaw) &&
                Float.floatToIntBits(this.pitch) == Float.floatToIntBits(that.pitch);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.name, this.worldName, this.x, this.y, this.z, this.yaw, this.pitch);
    }

    @Override
    public String toString() {
        return "Home[" +
                "name=" + this.name + ", " +
                "worldName=" + this.worldName + ", " +
                "x=" + this.x + ", " +
                "y=" + this.y + ", " +
                "z=" + this.z + ", " +
                "yaw=" + this.yaw + ", " +
                "pitch=" + this.pitch + ']';
    }

}
