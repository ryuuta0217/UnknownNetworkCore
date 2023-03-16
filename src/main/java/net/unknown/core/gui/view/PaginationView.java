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

import com.ryuuta0217.util.ListUtil;
import net.kyori.adventure.text.Component;
import net.unknown.core.define.DefinedItemStackBuilders;
import net.unknown.core.define.DefinedTextColor;
import net.unknown.core.gui.GuiBase;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Function;

public class PaginationView<T, G extends GuiBase> implements View {
    private final G gui;
    //private final Set<T> data;
    private List<Set<T>> splitData;
    private final Function<T, ItemStack> processor;
    private final Map<Integer, T> slot2data = new HashMap<>();
    private final BiConsumer<InventoryClickEvent, T> onClick;
    private BiConsumer<InventoryClickEvent, PaginationView<T, G>> createNewAction;
    private BiConsumer<InventoryClickEvent, PaginationView<T, G>> previousAction;
    private boolean compact = false; // TODO: Support compact mode

    private int currentPage = 1;

    /*
     * Compact mode
     *  0  1  2  3  4  5  6  7  8
     *  9 10 11 12 13 14 15 16 17
     * 18 19 20 21 22 23 24 25 26
     *
     * Normal mode
     *  0  1  2  3  4  5  6  7  8
     *  9 10 11 12 13 14 15 16 17
     * 18 19 20 21 22 23 24 25 26
     * 27 28 29 30 31 32 33 34 35
     * 36 37 38 39 40 41 42 43 44
     * 45 46 47 48 49 50 51 52 53
     */
    public PaginationView(G gui, Collection<T> data, Function<T, ItemStack> processor, boolean setPreviousButton, boolean setCreateNewButton) {
        this(gui, data, processor, null, setCreateNewButton ? (e,v) -> {} : null, setPreviousButton ? (e,v) -> {} : null);
    }

    public PaginationView(G gui, Collection<T> data, Function<T, ItemStack> processor, BiConsumer<InventoryClickEvent, T> onClick, BiConsumer<InventoryClickEvent, PaginationView<T, G>> createNewAction, BiConsumer<InventoryClickEvent, PaginationView<T, G>> previousAction) {
        this.gui = gui;
        this.compact = this.gui.getInventory().getSize() == 27;
        this.setData(data, false);
        this.processor = processor;
        this.onClick = onClick;
        this.createNewAction = createNewAction;
        this.previousAction = previousAction;
    }

    @Override
    public void initialize() {
        this.clearInventory();
        this.showPage(1);
        if (this.createNewAction != null) this.gui.getInventory().setItem(this.compact ? 22 : 49, DefinedItemStackBuilders.plus()
                .displayName(Component.text("新規追加", DefinedTextColor.GREEN))
                .build());

        if (this.previousAction != null) this.gui.getInventory().setItem(this.compact ? 18 : 45, DefinedItemStackBuilders.leftArrow()
                .displayName(Component.text("戻る", DefinedTextColor.GREEN))
                .build());
    }

    public void setData(Collection<T> data, boolean reload) {
        this.clearInventory();
        //this.data = data;
        this.splitData = ListUtil.splitListAsLinkedSet(data, this.compact ? 18 : 45);
        if (reload) this.showPage(Math.min(this.splitData.size(), this.currentPage));
    }

    public void showPage(int newPage) {
        if (newPage < 1) throw new IllegalArgumentException("Page is greater than 1");
        if (this.splitData.size() > newPage) throw new IllegalArgumentException("Maximum allowed page " + this.splitData.size() + " reached.");
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
                this.gui.getInventory().setItem(this.compact ? 25 : 52, DefinedItemStackBuilders.leftArrow()
                        .displayName(Component.text("前のページ", DefinedTextColor.YELLOW))
                        .build());
            } else {
                this.gui.getInventory().clear(this.compact ? 25 : 52);
            }

            if (this.currentPage < this.splitData.size()) {
                this.gui.getInventory().setItem(this.compact ? 26 : 53, DefinedItemStackBuilders.rightArrow()
                        .displayName(Component.text("次のページ", DefinedTextColor.YELLOW))
                        .build());
            } else {
                this.gui.getInventory().clear(this.compact ? 26 : 53);
            }
        }
    }

    @Override
    public void onClick(InventoryClickEvent event) {
        if (this.compact) {
            switch (event.getSlot()) {
                case 18 -> {
                    if (this.previousAction != null) {
                        this.previousAction.accept(event, this);
                    }
                    this.onPreviousButtonClicked(event);
                }

                case 22 -> {
                    if (this.createNewAction != null) {
                        this.createNewAction.accept(event, this);
                    }
                    this.onCreateNewButtonClicked(event);
                }

                case 25 -> {
                    if ((this.currentPage - 1) > 0) {
                        this.showPage(this.currentPage - 1);
                    }
                }

                case 26 -> {
                    if ((this.currentPage + 1) < this.splitData.size()) {
                        this.showPage(this.currentPage + 1);
                    }
                }

                default -> {
                    if (this.slot2data.containsKey(event.getSlot())) {
                        if (this.onClick != null) this.onClick.accept(event, this.slot2data.get(event.getSlot()));
                        this.onElementButtonClicked(event, this.slot2data.get(event.getSlot()));
                    }
                }
            }
        } else {
            switch (event.getSlot()) {
                case 45 -> {
                    if (this.previousAction != null) {
                        this.previousAction.accept(event, this);
                    }
                    this.onPreviousButtonClicked(event);
                }
                case 49 -> {
                    if (this.createNewAction != null) {
                        this.createNewAction.accept(event, this);
                    }
                    this.onCreateNewButtonClicked(event);
                }
                case 52 -> {
                    if ((this.currentPage - 1) > 0) {
                        this.showPage(this.currentPage - 1);
                    }
                }
                case 53 -> {
                    if ((this.currentPage + 1) < this.splitData.size()) {
                        this.showPage(this.currentPage + 1);
                    }
                }
                default -> {
                    if (this.slot2data.containsKey(event.getSlot())) {
                        if (this.onClick != null) this.onClick.accept(event, this.slot2data.get(event.getSlot()));
                        this.onElementButtonClicked(event, this.slot2data.get(event.getSlot()));
                    }
                }
            }
        }
    }

    @Override
    public void clearInventory() {
        this.slot2data.keySet().forEach(slot -> this.gui.getInventory().clear(slot));
        this.slot2data.clear();
        this.gui.getInventory().clear(this.compact ? 18 : 45);
        this.gui.getInventory().clear(this.compact ? 22 : 49);
        this.gui.getInventory().clear(this.compact ? 25 : 52);
        this.gui.getInventory().clear(this.compact ? 26 : 53);
    }

    public int getCurrentPage() {
        return this.currentPage;
    }

    protected G getGui() {
        return this.gui;
    }

    public void onElementButtonClicked(InventoryClickEvent event, T element) {

    }

    public void onPreviousButtonClicked(InventoryClickEvent event) {

    }

    public void onCreateNewButtonClicked(InventoryClickEvent event) {

    }
}
