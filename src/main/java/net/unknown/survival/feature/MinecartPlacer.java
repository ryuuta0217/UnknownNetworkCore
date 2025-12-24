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

import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.vehicle.minecart.AbstractMinecart;
import net.minecraft.world.entity.vehicle.minecart.Minecart;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.gameevent.GameEvent;
import net.unknown.core.util.MinecraftAdapter;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

public class MinecartPlacer implements Listener {
    @EventHandler
    public void onInteractRail(PlayerInteractEvent event) {
        if (event.getPlayer().getGameMode() == GameMode.SPECTATOR) return;
        if (event.getHand() == EquipmentSlot.OFF_HAND) return;
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        if (!event.getClickedBlock().getType().name().endsWith("RAIL")) return;
        Inventory playerInventory = event.getPlayer().getInventory();
        Location blockLocation = event.getClickedBlock().getLocation();

        if (playerInventory.contains(Material.MINECART) || event.getPlayer().getGameMode() == GameMode.CREATIVE) {
            ItemStack minecartItem = event.getPlayer().getGameMode() == GameMode.CREATIVE ? new ItemStack(Material.MINECART) : playerInventory.getItem(event.getPlayer().getInventory().first(Material.MINECART));
            if (minecartItem != null) {
                Minecart minecartEntity = AbstractMinecart.createMinecart(MinecraftAdapter.level(blockLocation.getWorld()), blockLocation.getX(), blockLocation.getY() + 0.0625, blockLocation.getZ(), EntityType.MINECART, EntitySpawnReason.DISPENSER, MinecraftAdapter.ItemStack.itemStack(minecartItem), MinecraftAdapter.player(event.getPlayer()));
                if (MinecraftAdapter.level(blockLocation.getWorld()).addFreshEntity(minecartEntity)) {
                    MinecraftAdapter.level(blockLocation.getWorld()).gameEvent(GameEvent.ENTITY_PLACE, MinecraftAdapter.vec3(blockLocation), GameEvent.Context.of(MinecraftAdapter.player(event.getPlayer()), MinecraftAdapter.blockState(event.getClickedBlock())));
                    minecartItem.setAmount(minecartItem.getAmount() - 1);
                    MinecraftAdapter.player(event.getPlayer()).startRiding(minecartEntity, true, true);
                }
            }
        }
    }
}
