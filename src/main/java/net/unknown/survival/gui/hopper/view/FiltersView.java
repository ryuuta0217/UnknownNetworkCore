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

package net.unknown.survival.gui.hopper.view;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.unknown.core.define.DefinedTextColor;
import net.unknown.core.gui.view.PaginationView;
import net.unknown.core.util.MinecraftAdapter;
import net.unknown.core.util.NewMessageUtil;
import net.unknown.launchwrapper.hopper.Filter;
import net.unknown.launchwrapper.hopper.ItemFilter;
import net.unknown.launchwrapper.hopper.TagFilter;
import net.unknown.survival.gui.hopper.ConfigureHopperGui;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

public class FiltersView extends PaginationView<Filter, ConfigureHopperGui> implements ConfigureHopperView {
    private static final int UPDATE_COOLDOWN_DEFAULT = 10;

    private final ConfigureHopperViewBase parentView;
    private int displayUpdateCooldown = UPDATE_COOLDOWN_DEFAULT;

    public FiltersView(ConfigureHopperViewBase parentView) {
        super(parentView.getGui(), parentView.getGui().getMixinHopper().getFilters(), (filter) -> {
            ItemStack viewItem = ItemStack.EMPTY;
            if (filter instanceof ItemFilter itemFilter) {
                viewItem = new ItemStack(itemFilter.getItem());
                if (itemFilter.getNbt() != null) viewItem.setTag(itemFilter.getNbt());
            } else if (filter instanceof TagFilter tagFilter) {
                Iterable<Holder<Item>> taggedItems = BuiltInRegistries.ITEM.getTagOrEmpty(tagFilter.getTag());

                List<Holder<Item>> taggedItemsList = new ArrayList<>();
                taggedItems.forEach(taggedItemsList::add);
                Collections.shuffle(taggedItemsList);
                int randomIndex = new Random().nextInt(taggedItemsList.size() - 1);

                Holder<Item> taggedFirstItem = taggedItemsList.get(randomIndex);
                viewItem = new ItemStack(taggedFirstItem);
                if (tagFilter.getNbt() != null) viewItem.setTag(tagFilter.getNbt());
            }

            org.bukkit.inventory.ItemStack bukkitViewItem = MinecraftAdapter.ItemStack.itemStack(viewItem);
            ItemMeta bukkitViewItemMeta = bukkitViewItem.getItemMeta();
            List<Component> lore = new ArrayList<>();
            if (bukkitViewItemMeta.hasLore()) {
                lore.addAll(bukkitViewItemMeta.lore());
                lore.add(Component.empty());
            }
            lore.add(Component.text("Shift+右クリックで削除", DefinedTextColor.RED, TextDecoration.BOLD).decoration(TextDecoration.ITALIC, false));
            bukkitViewItemMeta.lore(lore);
            bukkitViewItem.setItemMeta(bukkitViewItemMeta);
            return MinecraftAdapter.ItemStack.itemStack(viewItem);
        }, true, true);
        this.parentView = parentView;
    }

    @Override
    public void onElementButtonClicked(InventoryClickEvent event, Filter filter) {
        switch (event.getClick()) {
            case SHIFT_RIGHT -> {
                parentView.getGui().getMixinHopper().getFilters().remove(filter);
                this.setData(parentView.getGui().getMixinHopper().getFilters(), true);
            }
        }
    }

    @Override
    public void onPreviousButtonClicked(InventoryClickEvent event) {
        this.getGui().getView().clearInventory();
        this.getGui().setView(this.getParentView());
        this.getGui().getView().initialize();
    }

    @Override
    public void onCreateNewButtonClicked(InventoryClickEvent event) {
        this.getGui().setView(new CreateItemFilterView(this));
    }

    @Override
    public void tick() {
        super.tick();
        if (this.displayUpdateCooldown-- == 0) {
            this.showPage(this.getCurrentPage());
            this.displayUpdateCooldown = UPDATE_COOLDOWN_DEFAULT;
        }
    }

    public ConfigureHopperViewBase getParentView() {
        return this.parentView;
    }
}
