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

package net.unknown.core.block;

import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.commands.data.BlockDataAccessor;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.unknown.UnknownNetworkCore;
import org.bukkit.craftbukkit.v1_19_R2.inventory.CraftInventory;
import org.bukkit.event.EventHandler;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;

public class MultiPageChest {
    public static class Listener implements org.bukkit.event.Listener {
        @EventHandler
        public void onInventoryClick(InventoryClickEvent event) {
            if (event.getInventory() instanceof CraftInventory inventory && inventory.getInventory() instanceof ChestBlockEntity chest) {
                BlockDataAccessor accessor = new BlockDataAccessor(chest, chest.getBlockPos());
                CompoundTag originalData = accessor.getData();

                if (!originalData.contains("PublicBukkitValues")) return;
                CompoundTag chestData = originalData.getCompound("PublicBukkitValues");

                //{
                //  "MultiPageChest": {
                //    "CurrentPage": 0,
                //    "Pages": [
                //      {
                //        "Items": [
                //        ]
                //      },
                //      {
                //        "Items": [
                //          {"Slot": 0b, "id": "minecraft:stick", "Count": 24b}
                //        ]
                //      },
                //      {
                //        "Items": [
                //        ]
                //      }
                //    ]
                //  }
                //}
                if (chestData.contains("MultiPageChest")) {
                    ClickActionPageDirection pageDirection = ClickActionPageDirection.valueOf(event);
                    if (pageDirection == ClickActionPageDirection.UNKNOWN) return;

                    CompoundTag multiPageChestData = chestData.getCompound("MultiPageChest");
                    if (multiPageChestData.contains("CurrentPage") && multiPageChestData.contains("Pages")) {
                        int currentPage = multiPageChestData.getInt("CurrentPage");
                        if (currentPage == 0 && pageDirection == ClickActionPageDirection.PREVIOUS) return;

                        int nextPage = currentPage + (pageDirection == ClickActionPageDirection.NEXT ? 1 : -1);

                        // Save the current page
                        ListTag pages = multiPageChestData.getList("Pages", CompoundTag.TAG_COMPOUND);
                        ContainerHelper.saveAllItems(pages.getCompound(currentPage), (NonNullList<ItemStack>) chest.getContents());
                        accessor.setData(originalData);


                    }
                }
            }
            UnknownNetworkCore.getInstance().getLogger().info("ClickType: " + event.getClick() + " | Slot: " + event.getSlot() + " | Container: " + event.getClickedInventory());
        }
    }

    public enum ClickActionPageDirection {
        UNKNOWN(ClickType.UNKNOWN, Integer.MIN_VALUE, null),
        NEXT(ClickType.RIGHT, -999, null),
        PREVIOUS(ClickType.LEFT, -999, null);

        private final ClickType clickType;
        private final int slot;
        private final Inventory container;

        ClickActionPageDirection(ClickType clickType, int slot, Inventory container) {
            this.clickType = clickType;
            this.slot = slot;
            this.container = container;
        }

        public boolean isMatch(InventoryClickEvent event) {
            return event.getClick() == this.clickType && event.getSlot() == this.slot && event.getClickedInventory() == this.container;
        }

        public static ClickActionPageDirection valueOf(InventoryClickEvent event) {
            for (ClickActionPageDirection direction : values()) {
                if (direction.isMatch(event)) {
                    return direction;
                }
            }
            return ClickActionPageDirection.UNKNOWN;
        }
    }
}
