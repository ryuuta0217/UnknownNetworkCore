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

package net.unknown.survival.events;

import net.minecraft.world.level.block.Block;
import net.unknown.core.util.MinecraftAdapter;
import org.bukkit.Bukkit;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class ModifiableBlockBreakEvent extends Event {
    private static final HandlerList HANDLERS = new HandlerList();
    private boolean customDrops = false;
    private BlockBreakEvent original;
    private List<ItemStack> toDropItems;

    public ModifiableBlockBreakEvent(BlockBreakEvent original) {
        this.original = original;
    }

    public void setCustomDrops(boolean customDrops) {
        this.customDrops = customDrops;
    }

    public boolean isCustomDrops() {
        return this.customDrops;
    }

    public BlockBreakEvent getOriginal() {
        return this.original;
    }

    public List<ItemStack> getDrops() {
        if (this.toDropItems == null) this.toDropItems = new ArrayList<>(this.getOriginalDrops());
        return this.toDropItems;
    }

    private List<ItemStack> getDropsNullable() {
        return this.toDropItems;
    }

    public List<ItemStack> getOriginalDrops() {
        return Block.getDrops(MinecraftAdapter.blockState(this.getOriginal().getBlock()),
                        MinecraftAdapter.level(this.getOriginal().getBlock().getWorld()),
                        MinecraftAdapter.blockPos(this.getOriginal().getBlock().getLocation()),
                        null,
                        MinecraftAdapter.player(this.getOriginal().getPlayer()),
                        MinecraftAdapter.ItemStack.itemStack(this.getOriginal().getPlayer().getInventory().getItemInMainHand()))
                .stream()
                .map(MinecraftAdapter.ItemStack::itemStack)
                .collect(Collectors.toList());
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }

    public static class Listener implements org.bukkit.event.Listener {
        private static final Listener INSTANCE = new Listener();

        public static Listener getInstance() {
            return INSTANCE;
        }

        @EventHandler(priority = EventPriority.MONITOR)
        public void onBlockBreak(BlockBreakEvent event) {
            ModifiableBlockBreakEvent modifiable = new ModifiableBlockBreakEvent(event);
            Bukkit.getPluginManager().callEvent(modifiable);
            if (modifiable.isCustomDrops()) {
                event.setDropItems(false);
                List<ItemStack> drops = modifiable.getDropsNullable();
                if (drops != null && !event.isCancelled()) {
                    drops.forEach(bukkitStack -> {
                        net.minecraft.world.item.ItemStack stack = MinecraftAdapter.ItemStack.itemStack(bukkitStack);
                        Block.popResource(MinecraftAdapter.level(event.getBlock().getWorld()), MinecraftAdapter.blockPos(event.getBlock().getLocation()), stack);
                    });
                }
            }
        }
    }
}
