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

package net.unknown.survival.feature;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.ObserverBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.unknown.launchwrapper.event.ObserverBlockCheckNeighborEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class BlockDetector implements Listener {
    private static final Map<BlockPos, BlockState> LAST_BLOCKS = new HashMap<>();
    private static final Map<BlockPos, Set<Block>> DETECT_WHITELIST = new HashMap<>();

    @EventHandler
    public void onCheckNeighbor(ObserverBlockCheckNeighborEvent event) {
        if (DETECT_WHITELIST.containsKey(event.getObserverPos())) {
            event.setPredicate((data -> {
                boolean validBlock = DETECT_WHITELIST.get(event.getObserverPos()).contains(data.neighborState().getBlock());
                boolean validFacing = data.observer().getValue(ObserverBlock.FACING) == data.observerDirection();
                boolean equalsLastState = event.getNeighborState() == LAST_BLOCKS.getOrDefault(data.observerPos(), null);
                return validFacing && validBlock && !equalsLastState;
            }));
        }
    }

    /*@EventHandler
    public void onBlockBreak(ObserverBlockRemoveEvent event) {

    }*/
}
