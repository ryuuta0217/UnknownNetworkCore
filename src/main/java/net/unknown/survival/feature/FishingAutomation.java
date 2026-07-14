/*
 * Copyright (c) 2026 Unknown Network Developers and contributors.
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

import net.minecraft.world.InteractionHand;
import net.unknown.core.managers.RunnableManager;
import net.unknown.core.util.MinecraftAdapter;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.craftbukkit.CraftEquipmentSlot;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

public class FishingAutomation implements Listener {
    private static final NamespacedKey DATA_KEY = new NamespacedKey("survival", "automated_fishing");

    public static void apply(ItemStack fishingRod) {
        fishingRod.editPersistentDataContainer(container -> {
            container.set(DATA_KEY, PersistentDataType.BOOLEAN, true);
        });
    }

    @EventHandler
    public void onFish(PlayerFishEvent event) {
        if (event.getState() == PlayerFishEvent.State.BITE) {
            InteractionHand hand = event.getPlayer().getInventory().getItemInMainHand().getType() == Material.FISHING_ROD ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND;

            ItemStack fishingRod = event.getPlayer().getInventory().getItem(CraftEquipmentSlot.getHand(hand));
            if (fishingRod.getPersistentDataContainer().getOrDefault(DATA_KEY, PersistentDataType.BOOLEAN, false)) {
                RunnableManager.runDelayed(() -> {
                    MinecraftAdapter.player(event.getPlayer()).getItemInHand(hand).use(MinecraftAdapter.level(event.getPlayer().getWorld()), MinecraftAdapter.player(event.getPlayer()), hand);

                    RunnableManager.runDelayed(() -> {
                        MinecraftAdapter.player(event.getPlayer()).getItemInHand(hand).use(MinecraftAdapter.level(event.getPlayer().getWorld()), MinecraftAdapter.player(event.getPlayer()), hand);
                    }, 1);
                }, 1);
            }
        }
    }
}
