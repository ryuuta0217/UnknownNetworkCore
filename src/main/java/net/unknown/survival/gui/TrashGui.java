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
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextDecoration;
import net.unknown.core.builder.ItemStackBuilder;
import net.unknown.core.define.DefinedTextColor;
import net.unknown.core.enums.Permissions;
import net.unknown.core.gui.GuiBase;
import net.unknown.core.managers.RunnableManager;
import net.unknown.core.managers.TrashManager;
import net.unknown.core.util.MinecraftAdapter;
import org.bukkit.Material;
import org.bukkit.craftbukkit.v1_19_R3.entity.CraftHumanEntity;
import org.bukkit.craftbukkit.v1_19_R3.entity.CraftPlayer;
import org.bukkit.craftbukkit.v1_19_R3.util.CraftMagicNumbers;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.ItemStack;

import javax.annotation.Nonnull;
import java.util.stream.IntStream;

public class TrashGui extends GuiBase {
    private final Player player;
    private Player opener;

    public TrashGui(Player player) {
        this(player, player);
    }

    public TrashGui(Player player, Player opener) {
        super(player, 54, Component.text("ゴミ箱", DefinedTextColor.RED), (inventory) -> {
            TrashManager.getItemsBukkit(player.getUniqueId()).forEach(inventory::setItem);
            inventory.setItem(53, new ItemStackBuilder(Material.LAVA_BUCKET)
                    .displayName(Component.text("全て削除", Style.style(DefinedTextColor.RED, TextDecoration.BOLD)))
                    .lore(Component.text("ゴミ箱のアイテムが「確認なしで」全て削除されます。", Style.style(DefinedTextColor.YELLOW, TextDecoration.BOLD)))
                    .build());
        }, true);
        this.player = player;
        this.opener = opener;
    }

    @Override
    public void open(@Nonnull HumanEntity target) {
        if (this.opener == null && target instanceof Player targetPlayer) {
            this.opener = targetPlayer;
        }
        if (this.opener != null && Permissions.COMMAND_TRASH.checkAndIsPlayer(((CraftPlayer) this.opener).getHandle().createCommandSourceStack())) {
            super.open(this.opener);
        }
    }

    @Override
    public void onClick(InventoryClickEvent event) {
        if (event.getSlot() == 53) {
            // Clear the items button
            IntStream.rangeClosed(0, 52).forEach(this.getInventory()::clear);
            this.save();
        } else {
            // Content area, set editable
            event.setCancelled(false);
        }
    }

    @Override
    public void onClose(InventoryCloseEvent event) {
        this.save();
    }

    private void save() {
        for (int slot = 0; slot < this.getInventory().getSize(); slot++) {
            if (slot == 53) return; // Slot 53 is the clear button
            ItemStack itemStack = this.getInventory().getItem(slot);
            if (itemStack != null && itemStack.getType() != Material.AIR) {
                TrashManager.setItem(this.player.getUniqueId(), slot, MinecraftAdapter.ItemStack.itemStack(itemStack));
            } else {
                TrashManager.setItem(this.player.getUniqueId(), slot, net.minecraft.world.item.ItemStack.EMPTY);
            }
        }
        RunnableManager.runAsync(() -> TrashManager.save(this.player.getUniqueId()));
    }
}
