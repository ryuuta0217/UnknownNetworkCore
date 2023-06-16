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

package net.unknown.survival.listeners;

import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec2;
import net.unknown.core.util.MinecraftAdapter;
import net.unknown.launchwrapper.linkchest.LinkChestMode;
import net.unknown.launchwrapper.mixininterfaces.IMixinChestBlockEntity;
import net.unknown.launchwrapper.util.WrappedBlockPos;
import net.unknown.survival.feature.ChestLinkStick;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

public class ChestLinkProtectionListener implements Listener {
    private boolean skipNextEvent = false;

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (this.skipNextEvent) {
            this.skipNextEvent = false;
            return;
        }
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        Block clickedBlock = event.getClickedBlock();
        if (clickedBlock == null || clickedBlock.getType() != Material.CHEST) return;

        IMixinChestBlockEntity blockEntity = ChestLinkStick.getChestBlockEntity(clickedBlock.getLocation());
        if (blockEntity == null) return;
        if (blockEntity.getChestTransportMode() != LinkChestMode.CLIENT) return;

        WrappedBlockPos linkSource = blockEntity.getLinkSource();
        if (linkSource == null) return;

        BlockEntity sourceBlockEntity = linkSource.getBlockEntity(true, 3);
        if (sourceBlockEntity == null) return; // Failed to load the source chest
        if (!(sourceBlockEntity instanceof IMixinChestBlockEntity sourceChestBlockEntity)) return; // BlockEntity is not a chest or not bootstrapped
        if (sourceChestBlockEntity.getChestTransportMode() != LinkChestMode.SOURCE) return; // Specified chest is not a source chest

        Location linkSourceBukkit = MinecraftAdapter.location(linkSource.serverLevel(), linkSource.blockPos().getCenter(), Vec2.ZERO);
        if (!linkSourceBukkit.isChunkLoaded()) linkSourceBukkit.getWorld().loadChunk(linkSourceBukkit.getChunk()); // Some plugins may not work unless the chunk is loaded

        PlayerInteractEvent newEvent = new PlayerInteractEvent(event.getPlayer(), Action.RIGHT_CLICK_BLOCK, event.getItem(), linkSourceBukkit.getBlock(), event.getBlockFace());
        this.skipNextEvent = true;
        Bukkit.getPluginManager().callEvent(newEvent);

        if (newEvent.useInteractedBlock() == Event.Result.DENY) {
            event.setCancelled(true);
            clickedBlock.getLocation().getBlock().breakNaturally(new ItemStack(Material.NETHERITE_AXE), true);
        }
    }
}
