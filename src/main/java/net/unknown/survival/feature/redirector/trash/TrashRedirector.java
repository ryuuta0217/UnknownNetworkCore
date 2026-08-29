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

package net.unknown.survival.feature.redirector.trash;

import net.unknown.core.managers.TrashManager;
import net.unknown.core.util.MinecraftAdapter;
import net.unknown.survival.data.PlayerData;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerAttemptPickupItemEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class TrashRedirector implements Listener {
    private static final Logger LOGGER = LoggerFactory.getLogger("TrashRedirector");
    private static final NamespacedKey REGISTRY_NAMESPACE = new NamespacedKey("survival", "trash_redirector");

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerTriedPickupItem(PlayerAttemptPickupItemEvent event) {
        if (!event.getItem().canPlayerPickup()) return;

        Player player = event.getPlayer();
        TrashRedirectorMode mode = getMode(player);
        if (mode == TrashRedirectorMode.DISABLED) return;

        ItemStack pickupItem = event.getItem().getItemStack();
        if (pickupItem.isEmpty() || pickupItem.getType() == Material.AIR) return;

        Map<Integer, ItemStack> existing = TrashManager.getItemsBukkit(player.getUniqueId());

        if (mode == TrashRedirectorMode.INSERT_MATCHING) {
            boolean hasMatching = existing.values().stream()
                    .filter(Objects::nonNull)
                    .anyMatch(item -> !item.isEmpty() && item.getType() != Material.AIR && item.isSimilar(pickupItem));
            if (!hasMatching) return;
        }

        Inventory tempInv = Bukkit.createInventory(null, 54);
        existing.forEach((slot, item) -> {
            if (slot < 53 && item != null && !item.isEmpty() && item.getType() != Material.AIR) {
                tempInv.setItem(slot, item);
            }
        });

        tempInv.setItem(53, new ItemStack(Material.BARRIER)); // 53番スロット (TrashGuiの全クリアボタン枠) は埋めないように

        int originalAmount = pickupItem.getAmount();
        HashMap<Integer, ItemStack> leftover = tempInv.addItem(pickupItem);

        boolean redirected = false;
        if (leftover.isEmpty()) {
            event.getItem().setItemStack(ItemStack.empty());
            redirected = true;
        } else {
            ItemStack remaining = leftover.values().iterator().next();
            if (remaining.getAmount() < originalAmount) {
                redirected = true; // 一部は転送済 フラグを立てる
            }
            TrashOverflowBehavior overflowBehavior = getOverflowBehavior(player);
            if (overflowBehavior == TrashOverflowBehavior.VOID) {
                event.getItem().setItemStack(ItemStack.empty());
                redirected = true;
            } else {
                event.getItem().setItemStack(remaining);
            }
        }

        if (redirected) {
            Map<Integer, net.minecraft.world.item.ItemStack> newMap = new HashMap<>(54);
            for (int i = 0; i < 53; i++) {
                ItemStack item = tempInv.getItem(i);
                if (item != null && !item.isEmpty() && item.getType() != Material.AIR) {
                    newMap.put(i, MinecraftAdapter.ItemStack.itemStack(item));
                }
            }
            if (existing.containsKey(53) && existing.get(53) != null && !existing.get(53).isEmpty() && existing.get(53).getType() != Material.AIR) {
                newMap.put(53, MinecraftAdapter.ItemStack.itemStack(existing.get(53)));
            }
            TrashManager.setItems(player.getUniqueId(), newMap);
        }

        if (event.getItem().getItemStack().isEmpty()) {
            event.setCancelled(true);
        }

        if (redirected) {
            event.setFlyAtPlayer(true);
        }
    }

    public static TrashRedirectorMode getMode(Player player) {
        try {
            return TrashRedirectorMode.valueOf(PlayerData.of(player).getRegistries().getOrDefault(REGISTRY_NAMESPACE, "mode", TrashRedirectorMode.DISABLED.name()));
        } catch (IllegalArgumentException | NullPointerException e) {
            return TrashRedirectorMode.DISABLED;
        }
    }

    public static void setMode(Player player, TrashRedirectorMode mode) {
        PlayerData.of(player).getRegistries().put(REGISTRY_NAMESPACE, "mode", mode.name());
    }

    public static TrashOverflowBehavior getOverflowBehavior(Player player) {
        try {
            return TrashOverflowBehavior.valueOf(PlayerData.of(player).getRegistries().getOrDefault(REGISTRY_NAMESPACE, "overflow", TrashOverflowBehavior.STOP_INSERT.name()));
        } catch (IllegalArgumentException | NullPointerException e) {
            return TrashOverflowBehavior.STOP_INSERT;
        }
    }

    public static void setOverflowBehavior(Player player, TrashOverflowBehavior behavior) {
        PlayerData.of(player).getRegistries().put(REGISTRY_NAMESPACE, "overflow", behavior.name());
    }
}
