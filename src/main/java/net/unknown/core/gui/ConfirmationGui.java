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

package net.unknown.core.gui;

import net.kyori.adventure.text.Component;
import net.unknown.core.builder.ItemStackBuilder;
import net.unknown.core.define.DefinedItemStackBuilders;
import net.unknown.core.define.DefinedTextColor;
import org.bukkit.Material;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

import java.util.function.Consumer;
import java.util.function.Supplier;

public class ConfirmationGui extends GuiBase {
    private final Consumer<InventoryClickEvent> onConfirm;
    private final Consumer<InventoryClickEvent> onCancel;

    public ConfirmationGui(InventoryHolder owner, Component title, Supplier<ItemStack> sign, Supplier<ItemStack> confirmButton, Supplier<ItemStack> cancelButton, Consumer<InventoryClickEvent> onConfirm, Consumer<InventoryClickEvent> onCancel) {
        super(owner, 27, title, (inventory) -> {
            inventory.setItem(4, sign == null ? null : sign.get());
            inventory.setItem(12, confirmButton == null ? new ItemStackBuilder(Material.LIME_WOOL).displayName(Component.text("OK", DefinedTextColor.GREEN)).build() : confirmButton.get());
            inventory.setItem(14, cancelButton == null ? new ItemStackBuilder(Material.RED_WOOL).displayName(Component.text("キャンセル", DefinedTextColor.RED)).build() : cancelButton.get());
        }, true);
        this.onConfirm = onConfirm;
        this.onCancel = onCancel;
    }

    @Override
    public void onClick(InventoryClickEvent event) {
        switch(event.getSlot()) {
            case 12: // Confirm button
                if (onConfirm != null) {
                    onConfirm.accept(event);
                }
                break;
            case 14: // Cancel button
                if (onCancel != null) {
                    onCancel.accept(event);
                }
                break;
            default:
                // Do nothing for other slots
                break;
        }
    }
}
