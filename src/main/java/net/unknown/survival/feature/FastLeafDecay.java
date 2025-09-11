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

package net.unknown.survival.feature;

import net.unknown.UnknownNetworkCorePlugin;
import net.unknown.core.managers.RunnableManager;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Tag;
import org.bukkit.block.BlockFace;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPhysicsEvent;

import java.util.Random;

public class FastLeafDecay implements Listener {
    private static final Random RANDOM = new Random();
    public static int MIN_DECAY_TICKS = 8;
    public static int MAX_DECAY_TICKS = 18;

    @EventHandler
    public void onBlockPhysics(BlockPhysicsEvent event) {
        if (event.getBlock().getType().isAir()) {
            for (BlockFace face : BlockFace.values()) {
                if (face.isCartesian()) {
                    Location offLocation = event.getBlock().getLocation().add(face.getModX(), face.getModY(), face.getModZ());
                    if (offLocation.isChunkLoaded()) {
                        if (Tag.LEAVES.isTagged(offLocation.getBlock().getType())) {
                            int delay = MIN_DECAY_TICKS + (RANDOM.nextInt(MAX_DECAY_TICKS - MIN_DECAY_TICKS));
                            if (UnknownNetworkCorePlugin.isFoliaPlatform()) {
                                Bukkit.getRegionScheduler().runDelayed(UnknownNetworkCorePlugin.getInstance(), offLocation, (task) -> {
                                    if (offLocation.getWorld() != null && Tag.LEAVES.isTagged(offLocation.getBlock().getType())) {
                                        offLocation.getBlock().randomTick();
                                    }
                                }, delay);
                            } else {
                                RunnableManager.runDelayed(() -> {
                                    if (offLocation.getWorld() != null && Tag.LEAVES.isTagged(offLocation.getBlock().getType())) {
                                        offLocation.getBlock().randomTick();
                                    }
                                }, delay);
                            }
                        }
                    }
                }
            }
        }
    }
}
