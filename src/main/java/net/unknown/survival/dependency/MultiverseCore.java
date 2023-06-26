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

package net.unknown.survival.dependency;

import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import net.royawesome.jlibnoise.module.combiner.Min;
import net.unknown.core.util.MinecraftAdapter;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

public class MultiverseCore {
    private static final boolean MULTIVERSE_CORE_ENABLED = Bukkit.getPluginManager().getPlugin("Multiverse-Core") != null && Bukkit.getPluginManager().isPluginEnabled("Multiverse-Core");

    public static boolean isMultiverseCoreEnabled() {
        return MULTIVERSE_CORE_ENABLED;
    }

    public static Location getSpawnLocation(World world) {
        if (!MULTIVERSE_CORE_ENABLED) return world.getSpawnLocation();
        return getInstance().getMVWorldManager().getMVWorld(world).getSpawnLocation();
    }

    public static Location getSpawnLocation(Level level) {
        Vec3 positionVector3 = level.getSharedSpawnPos().getCenter();
        Vec2 rotationVector2 = new Vec2(level.getSharedSpawnAngle(), 0);
        if (MULTIVERSE_CORE_ENABLED) {
            Location multiverseSpawnLocation = getSpawnLocation(MinecraftAdapter.world(level));
            positionVector3 = MinecraftAdapter.vec3(multiverseSpawnLocation);
            rotationVector2 = MinecraftAdapter.vec2(multiverseSpawnLocation);
        }
        return MinecraftAdapter.location(level, positionVector3, rotationVector2);
    }

    public static com.onarandombox.MultiverseCore.MultiverseCore getInstance() {
        return (com.onarandombox.MultiverseCore.MultiverseCore) Bukkit.getPluginManager().getPlugin("Multiverse-Core");
    }
}
