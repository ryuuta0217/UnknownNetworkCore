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

import net.unknown.core.util.MinecraftAdapter;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.Ageable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

public class HarvestRightClick implements Listener {
    private static final Set<Material> SUPPORTED_CROPS = new HashSet<>() {{
        add(Material.WHEAT);
        add(Material.BEETROOTS);
        add(Material.CARROTS);
        add(Material.POTATOES);
    }};

    @EventHandler // TODO: エンチャントとして実装時は、右クリックで収穫ではなくBlockBreakEventで処理した上で、モード切り替えをShift+ホットバー切り替えで実装する
    public void onInteractBlock(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK || event.getClickedBlock() == null) return;
        if (!SUPPORTED_CROPS.contains(event.getClickedBlock().getType())) return;
        if (!(event.getClickedBlock().getBlockData() instanceof Ageable ageable) || ageable.getAge() != ageable.getMaximumAge()) return;
        event.setCancelled(true);
        Block clickedBlock = event.getClickedBlock();

        AtomicReference<Material> replantBlock = new AtomicReference<>();

        Collection<ItemStack> drops = clickedBlock.getDrops(event.getItem(), event.getPlayer());

        drops.forEach(item -> {
            if (clickedBlock.getType() == Material.WHEAT) {
                if (item.getType() == Material.WHEAT_SEEDS) {
                    item.setAmount(item.getAmount() - 1);
                    replantBlock.set(Material.WHEAT);
                }
            } else if (clickedBlock.getType() == Material.BEETROOTS) {
                if (item.getType() == Material.BEETROOT_SEEDS) {
                    item.setAmount(item.getAmount() - 1);
                    replantBlock.set(Material.BEETROOTS);
                }
            } else {
                item.setAmount(item.getAmount() - 1);
                replantBlock.set(clickedBlock.getType());
            }
        });
        MinecraftAdapter.level(clickedBlock.getWorld()).destroyBlock(MinecraftAdapter.blockPos(clickedBlock.getLocation()), false, MinecraftAdapter.player(event.getPlayer()));
        MinecraftAdapter.level(clickedBlock.getWorld()).setBlock(MinecraftAdapter.blockPos(clickedBlock.getLocation()), MinecraftAdapter.block(replantBlock.get()).defaultBlockState(), net.minecraft.world.level.block.Block.UPDATE_ALL);
        event.getPlayer().getInventory().addItem(drops.toArray(new ItemStack[0])).forEach((index, item) -> {
            event.getPlayer().getWorld().dropItem(event.getPlayer().getLocation(), item);
        });
    }
}
