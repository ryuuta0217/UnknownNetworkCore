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

package net.unknown.survival.item;

import net.kyori.adventure.text.Component;
import net.unknown.core.item.UnknownNetworkItem;
import net.unknown.core.item.UnknownNetworkItemStack;
import net.unknown.core.util.BlockUtil;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.block.BlockFace;
import org.bukkit.block.TileState;
import org.bukkit.entity.*;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDismountEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.UUID;
import java.util.stream.Collectors;

public class LifterItem extends UnknownNetworkItem implements Listener {
    private static final NamespacedKey CARRYING_KEY = new NamespacedKey("survival", "lifter_carrying");
    private static final NamespacedKey WHO_CARRYING_KEY = new NamespacedKey("survival", "lifter_who_carrying");

    public LifterItem() {
        super(new NamespacedKey("survival", "lifter"));
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent event) {
        if (!this.equals(event.getItem())) return;
        event.setCancelled(true);
        if (event.getHand() != EquipmentSlot.HAND) return;
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        if (event.getPlayer().isSneaking()) {
            if (isCarrying(event.getPlayer())) {
                drop(event.getPlayer(), event.getBlockFace(), event.getClickedBlock().getLocation());
            } else {
                if (!(event.getClickedBlock().getState() instanceof TileState)) {
                    mount(event.getPlayer(), BlockUtil.replaceBlockAsEntity(event.getClickedBlock().getLocation(), fallingBlock -> fallingBlock.shouldAutoExpire(false)));
                }
            }
        }
    }

    @EventHandler
    public void onInteractEntity(PlayerInteractEntityEvent event) {
        if (!this.equals(event.getPlayer().getInventory().getItem(event.getHand()))) return;
        event.setCancelled(true);
        if (event.getHand() != EquipmentSlot.HAND) return;

        if (event.getPlayer().isSneaking()) {
            if (isCarrying(event.getPlayer())) {
                drop(event.getPlayer(), null, event.getRightClicked().getLocation());
            }

            mount(event.getPlayer(), event.getRightClicked());
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        drop(event.getPlayer(), null, null);
    }

    @EventHandler
    public void onEntityDismount(EntityDismountEvent event) {
        if (event.getEntity() instanceof Player player) {

        }
    }

    @EventHandler
    public void onHeldItemChanged(PlayerItemHeldEvent event) {
        if (isCarrying(event.getPlayer())) {
            ItemStack oldItem = event.getPlayer().getInventory().getItem(event.getPreviousSlot());
            ItemStack newItem = event.getPlayer().getInventory().getItem(event.getNewSlot());
            if (!this.equals(newItem)) {
                drop(event.getPlayer(), null, null);
            }
        }
    }

    private static void mount(Player player, Entity entity) {
        if (entity.getType() != EntityType.FALLING_BLOCK && !(entity instanceof Animals)) {
            player.addPassenger(player.getWorld().spawn(entity.getLocation(), AreaEffectCloud.class, aec -> {
                aec.setRadius(0);
                aec.setInvulnerable(true);
                aec.setGravity(false);
                aec.setDuration(Integer.MAX_VALUE);
                aec.setWaitTime(0);
                aec.addScoreboardTag("UNC_LIFTER_SEAT");
                aec.addPassenger(entity);
            }));
        } else {
            player.addPassenger(entity);
        }
        entity.getPersistentDataContainer().set(WHO_CARRYING_KEY, PersistentDataType.STRING, player.getUniqueId().toString());
        setCarrying(player, entity.getUniqueId());
    }

    private static void drop(Player player, @Nullable BlockFace face, @Nullable Location whereToDrop) {
        drop0(player, face, whereToDrop);
        setCarrying(player, null);
    }

    private static void drop0(Player player, @Nullable BlockFace face, @Nullable Location whereToDrop) {
        if (isCarrying(player)) {
            if (whereToDrop == null) whereToDrop = player.getLocation();
            whereToDrop = shift(face == null ? BlockFace.UP : face, whereToDrop);

            final Location finalWhereToDrop = whereToDrop;
            if (!player.getPassengers().isEmpty()) {
                if (player.getPassengers().getFirst() instanceof AreaEffectCloud) {
                    if (!player.getPassengers().getFirst().getPassengers().isEmpty()) {
                        Entity passenger = player.getPassengers().getFirst().getPassengers().getFirst();
                        passenger.leaveVehicle();
                        passenger.getPersistentDataContainer().remove(WHO_CARRYING_KEY);
                        passenger.teleport(finalWhereToDrop);
                        player.getPassengers().getFirst().remove();
                    }
                } else {
                    player.getPassengers().forEach(passenger -> {
                        passenger.leaveVehicle();
                        passenger.getPersistentDataContainer().remove(WHO_CARRYING_KEY);
                        passenger.teleport(finalWhereToDrop);
                    });
                }
            }
        }
    }

    @Nullable
    private static UUID getWhoCarrying(Entity entity) {
        return entity.getPersistentDataContainer().has(WHO_CARRYING_KEY, PersistentDataType.STRING) ? UUID.fromString(entity.getPersistentDataContainer().get(WHO_CARRYING_KEY, PersistentDataType.STRING)) : null;
    }

    private static void setCarrying(Player player, @Nullable UUID entityUniqueId) {
        // If player is already carrying something, drop it
        if (entityUniqueId != null) {
            if (isCarrying(player)) throw new IllegalArgumentException("Only one entity can be carried by a player at a time!");
            player.getPersistentDataContainer().set(CARRYING_KEY, PersistentDataType.STRING, entityUniqueId.toString());
        } else {
            player.getPersistentDataContainer().remove(CARRYING_KEY);
        }
    }

    private static boolean isCarrying(Player player) {
        return player.getPersistentDataContainer().has(CARRYING_KEY);
    }

    private static Location shift(BlockFace face, Location location) {
        return location.clone().add(face.getModX(), face.getModY(), face.getModZ());
    }

    @Override
    public UnknownNetworkItemStack<? extends UnknownNetworkItem> createItemStack() {
        return new Stack(this.createItemStackBuilder(Material.LADDER)
                .maxStackSize(1)
                .custom(is -> is.editMeta(meta -> meta.itemName(Component.translatable("unknown-network.item.lifter", "リフター"))))
                .build());
    }

    public static class Stack extends UnknownNetworkItemStack<LifterItem> {
        public Stack(ItemStack handle) {
            super(handle, Items.LIFTER_ITEM);
        }
    }
}
