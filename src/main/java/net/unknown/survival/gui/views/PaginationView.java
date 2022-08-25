/*
 * Copyright (c) 2022 Unknown Network Developers and contributors.
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

package net.unknown.survival.gui.views;

import com.ryuuta0217.util.ListUtil;
import net.unknown.core.gui.GuiBase;
import net.unknown.core.gui.View;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

public class PaginationView<T> implements View {
    private final GuiBase gui;
    private final Set<T> data;
    private final List<Set<T>> splitData;
    private final Function<T, ItemStack> processor;
    private final Map<Integer, T> slot2data = new HashMap<>();

    private int currentPage = 1;

    public PaginationView(GuiBase gui, Set<T> data, Function<T, ItemStack> processor, BiConsumer<InventoryClickEvent, T> onClick) {
        this.gui = gui;
        this.data = data;
        this.splitData = ListUtil.splitListAsLinkedSet(data, 45);
        this.processor = processor;
    }

    @Override
    public void initialize() {
        this.showPage(1);
    }

    private void showPage(int newPage) {
        if (this.splitData.size() >= newPage) throw new IllegalArgumentException("Maximum allowed page " + this.splitData.size() + " reached.");
        this.currentPage = newPage;
        this.clearInventory();
        this.splitData.get(this.currentPage - 1).forEach(data -> {
            int slot = this.gui.getInventory().firstEmpty();
            if (slot != -1) {
                ItemStack item = processor.apply(data);
                this.gui.getInventory().setItem(slot, item);
                slot2data.put(slot, data);
            }
        });

        if (this.splitData.size() > 1) {
            if (this.currentPage > 1) {
                // Previous button
            } else {
                // Remove Previous button
                this.gui.getInventory().clear(52);
            }

            if (this.currentPage <= this.splitData.size()) {
                // Next button
            } else {
                // Remove Next button
                this.gui.getInventory().clear(53);
            }
        }
    }

    @Override
    public void onClick(InventoryClickEvent event) {

    }

    @Override
    public void clearInventory() {
        this.slot2data.keySet().forEach(slot -> this.gui.getInventory().clear(slot));
    }
}
