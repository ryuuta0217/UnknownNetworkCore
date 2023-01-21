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

package net.unknown.core.gui.view;

import net.unknown.core.gui.GuiBase;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

public class ChooseView implements View {
    private final GuiBase gui;
    private final Map<Integer, ItemStack> items;
    private final Map<Integer, Consumer<InventoryClickEvent>> actions;


    private ChooseView(GuiBase gui, Map<Integer, ItemStack> items, Map<Integer, Consumer<InventoryClickEvent>> actions) {
        this.gui = gui;
        this.items = items;
        this.actions = actions;
    }

    @Override
    public void initialize() {
        this.items.forEach((slot, item) -> {
            if(this.gui.getInventory().getItem(slot) == null) {
                this.gui.getInventory().setItem(slot, item);
            }
        });
    }

    @Override
    public void onClick(InventoryClickEvent event) {
        this.actions.getOrDefault(event.getSlot(), (e) -> {}).accept(event);
    }

    @Override
    public void clearInventory() {
        this.items.keySet().forEach(this.gui.getInventory()::clear);
    }

    public static class Builder {
        private final GuiBase gui;
        private final Map<Integer, ItemStack> items = new HashMap<>();
        private final Map<Integer, Consumer<InventoryClickEvent>> actions = new HashMap<>();

        public Builder(GuiBase gui) {
            this.gui = gui;
        }

        public static Builder newBuilder(GuiBase gui) {
            return new Builder(gui);
        }

        public Builder addSelection(int slot, ItemStack item, Consumer<InventoryClickEvent> action) {
            if ((this.gui.getInventory().getSize() - 1) > slot) {
               throw new IllegalArgumentException("Slot " + slot + " is out of range <" + (this.gui.getInventory().getSize() - 1)  + ">");
            }

            this.items.put(slot, item);
            this.actions.put(slot, action);
            return this;
        }

        public ChooseView build() {
            return new ChooseView(this.gui, this.items, this.actions);
        }
    }
}
