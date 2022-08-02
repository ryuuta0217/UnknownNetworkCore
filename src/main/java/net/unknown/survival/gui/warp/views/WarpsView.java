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

package net.unknown.survival.gui.warp.views;

import com.ryuuta0217.util.ListUtil;
import net.kyori.adventure.text.Component;
import net.unknown.core.builder.ItemStackBuilder;
import net.unknown.core.define.DefinedItemStackBuilders;
import net.unknown.core.define.DefinedTextColor;
import net.unknown.core.gui.View;
import net.unknown.survival.data.Warps;
import net.unknown.survival.data.model.Warp;
import net.unknown.survival.gui.warp.WarpGui;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemFlag;

import java.util.*;
import java.util.stream.IntStream;

public class WarpsView implements View {
    private final WarpGui parent;
    private List<Set<Warp>> pages;
    private Map<Integer, Warp> slot2warp = new HashMap<>();
    private int page = 1;

    public WarpsView(WarpGui parent) {
        this.parent = parent;
    }

    @Override
    public void initialize() {
        this.pages = ListUtil.splitListAsSet(Warps.getWarps().values(), 45);
        this.showPage(1);
    }

    public void showPage(int page) {
        if(this.page < 1) throw new IllegalArgumentException("Page is greater than 1");
        if(this.page > this.pages.size()) throw new IllegalArgumentException("Page is greater than the total number of pages");
        this.page = page;

        IntStream.rangeClosed(0, 44).forEach(this.parent.getInventory()::clear);
        this.slot2warp.clear();

        this.pages.get(page - 1).forEach(data -> {
            ItemStackBuilder builder = new ItemStackBuilder(data.getIcon() == null ? dimension2Material(data.getLocation()) : data.getIcon());
            builder.displayName(data.getDisplayName());
            builder.itemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_DYE);
            int slot = this.parent.getInventory().firstEmpty();
            this.parent.getInventory().setItem(slot, builder.build());
            this.slot2warp.put(slot, data);
        });

        if(this.page != 1) {
            this.parent.getInventory().setItem(52, DefinedItemStackBuilders.leftArrow()
                    .displayName(Component.text("前のページ", DefinedTextColor.GREEN))
                    .build());
        } else {
            this.parent.getInventory().clear(52);
        }

        if(this.page != this.pages.size()) {
            this.parent.getInventory().setItem(53, DefinedItemStackBuilders.rightArrow()
                    .displayName(Component.text("次のページ", DefinedTextColor.GREEN))
                    .build());
        } else {
            this.parent.getInventory().clear(53);
        }
    }

    @Override
    public void onClick(InventoryClickEvent event) {
        if(this.slot2warp.containsKey(event.getSlot())) {
            event.getWhoClicked().teleportAsync(this.slot2warp.get(event.getSlot()).getLocation());
            event.getWhoClicked().closeInventory();
        }
    }

    @Override
    public void clearInventory() {
        IntStream.rangeClosed(0, 44).forEach(this.parent.getInventory()::clear);
        IntStream.rangeClosed(46, 53).forEach(this.parent.getInventory()::clear);
    }

    private static Material dimension2Material(Location loc) {
        return switch (loc.getWorld().getEnvironment()) {
            case NORMAL -> Material.GRASS_BLOCK;
            case NETHER -> Material.NETHERRACK;
            case THE_END -> Material.END_STONE;
            default -> Material.WHITE_WOOL;
        };
    }
}
