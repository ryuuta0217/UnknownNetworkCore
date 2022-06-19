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

package net.unknown.survival.dependency;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.internal.platform.WorldGuardPlatform;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class WorldGuard {
    public static final String SPLITTER = "_";
    public static final Pattern ID_PATTERN = Pattern.compile("(?i)^[\\dA-F]{8}-[\\dA-F]{4}-4[\\dA-F]{3}-[89AB][\\dA-F]{3}-[\\dA-F]{12}_.*");

    public static WorldGuardPlatform getPlatform() {
        return com.sk89q.worldguard.WorldGuard.getInstance().getPlatform();
    }

    public static List<WrappedProtectedRegion> getProtectedRegions(Player owner) {
        List<WrappedProtectedRegion> regions = new ArrayList<>();

        Bukkit.getWorlds().forEach(world -> {
            RegionManager regionManager = getPlatform().getRegionContainer().get(BukkitAdapter.adapt(world));
            if (regionManager != null) {
                regionManager.getRegions().forEach((name, region) -> {
                    if (region.getOwners().contains(owner.getUniqueId()) || region.getId().startsWith(owner.getUniqueId().toString()))
                        regions.add(new WrappedProtectedRegion(world, regionManager, region));
                });
            }
        });

        return regions;
    }

    public record WrappedProtectedRegion(World world, RegionManager regionManager, ProtectedRegion region) {
        public String getFullId() {
            return this.region.getId();
        }

        public String getId() {
            if(ID_PATTERN.matcher(this.getFullId()).matches()) {
                return this.getFullId().split(SPLITTER, 2)[1];
            }
            return this.getFullId();
        }
    }
}
