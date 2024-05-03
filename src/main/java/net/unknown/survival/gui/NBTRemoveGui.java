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

package net.unknown.survival.gui;

import net.kyori.adventure.text.Component;
import net.unknown.core.builder.ItemStackBuilder;
import net.unknown.core.define.DefinedTextColor;
import net.unknown.core.gui.GuiBase;
import net.unknown.core.managers.RunnableManager;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.event.Event;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.scheduler.BukkitTask;

public class NBTRemoveGui extends GuiBase {
    private static final int SOURCE_SLOT = 20;
    private static final int RESULT_SLOT = 24;

    private final BukkitTask task;

    public NBTRemoveGui(InventoryHolder owner) {
        super(owner, 54, Component.text("NBT Remove", DefinedTextColor.DARK_AQUA), (inventory) -> {
            ItemStack is = new ItemStackBuilder(Material.BLACK_STAINED_GLASS_PANE)
                    .displayName(Component.text(" "))
                    .build();

            for (int i = 0; i < inventory.getSize(); i++) {
                inventory.setItem(i, is);
            }
        }, true);
        this.passOnlyThisInventory = false;
        this.task = RunnableManager.runAsyncRepeating(this::showResultIfSourceFound, 1L, 1L);
    }

    @Override
    public void onOpen(InventoryOpenEvent event) {
        this.getInventory().clear(SOURCE_SLOT);
        this.getInventory().clear(RESULT_SLOT);
    }

    @Override
    public void onClose(InventoryCloseEvent event) {
        if (this.getSource() != null) {
            event.getPlayer().getInventory().addItem(this.getSource()).forEach((i, stack) -> {
                Item itemEntity = event.getPlayer().getWorld().createEntity(event.getPlayer().getLocation(), Item.class);
                itemEntity.setItemStack(stack);
                event.getPlayer().getWorld().addEntity(itemEntity);
            });
        }
    }

    @Override
    public void onClick(InventoryClickEvent event) {
        if (!event.getInventory().equals(this.getInventory())) return;

        Inventory clickedInventory = event.getClickedInventory();
        event.setCancelled(clickedInventory == null || (clickedInventory.equals(this.getInventory())));

        if (clickedInventory != null && !clickedInventory.equals(this.getInventory()) && event.getAction() == InventoryAction.MOVE_TO_OTHER_INVENTORY) {
            // プレイヤーが自身のインベントリから開いているGUIに向かってアイテムを挿入しようとした時 (Shiftクリックと思われる)
            if (this.getSource() == null) {
                System.out.println("L89");
                event.setCancelled(false);
            }
        }

        if (clickedInventory != null && clickedInventory.equals(this.getInventory()) && event.getSlot() == SOURCE_SLOT) {
            // プレイヤーがソース・スロットにアイテムを設置(挿入)しようとした時
            event.setCancelled(false);
        }

        if (clickedInventory != null && clickedInventory.equals(this.getInventory()) && this.getSource() != null && this.getResult() != null && event.getSlot() == RESULT_SLOT && event.getAction() == InventoryAction.PICKUP_ALL) {
            // プレイヤーがリザルト・スロットにあるアイテムを取り出そうとした時
            event.setCancelled(false);
            this.removeSource();
        }
    }

    @Override
    public void onUnregistering() {
        this.task.cancel();
    }

    private void showResultIfSourceFound() {
        if (this.getSource() != null) {
            ItemStack source = this.getSource().clone();
            this.setResult(new ItemStackBuilder(source.getType())
                    .amount(source.getAmount())
                    .custom(is -> is.editMeta(meta -> {
                        if (meta instanceof Damageable damageable && source.getItemMeta() instanceof Damageable sourceDamageable) {
                            damageable.setDamage(sourceDamageable.getDamage());
                        }
                    }))
                    .build());
        } else {
            this.setResult(null);
        }
    }

    private void removeSource() {
        this.getInventory().clear(SOURCE_SLOT);
    }

    private ItemStack getSource() {
        return this.getInventory().getItem(SOURCE_SLOT);
    }

    private void setResult(ItemStack stack) {
        this.getInventory().setItem(RESULT_SLOT, stack);
    }

    private ItemStack getResult() {
        return this.getInventory().getItem(RESULT_SLOT);
    }
}
