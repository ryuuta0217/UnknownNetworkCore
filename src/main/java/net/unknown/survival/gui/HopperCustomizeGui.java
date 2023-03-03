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

import net.kyori.adventure.text.Component;;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.unknown.core.builder.ItemStackBuilder;
import net.unknown.core.define.DefinedTextColor;
import net.unknown.core.gui.GuiBase;
import net.unknown.core.gui.view.View;
import net.unknown.launchwrapper.hopper.FilterType;
import net.unknown.launchwrapper.hopper.IMixinHopperBlockEntity;
import org.bukkit.Material;
import org.bukkit.craftbukkit.v1_19_R2.block.CraftHopper;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;

public class HopperCustomizeGui extends GuiBase {
    private final HopperBlockEntity hopper;
    private final IMixinHopperBlockEntity mixinHopper;
    private View currentView;

    public HopperCustomizeGui(Player opener, HopperBlockEntity hopper, IMixinHopperBlockEntity mixinHopper) {
        super(opener, InventoryType.CHEST, Component.text("ホッパーの設定", DefinedTextColor.BLUE), true);
        this.hopper = hopper;
        this.mixinHopper = mixinHopper;
        this.currentView = new MainView();
    }

    @Override
    public void onClick(InventoryClickEvent event) {
        this.currentView.onClick(event);
    }

    public class MainView implements View {
        public MainView() {
            if (HopperCustomizeGui.this.currentView != null) HopperCustomizeGui.this.currentView.clearInventory();
            HopperCustomizeGui.this.currentView = this;
            this.initialize();
        }

        @Override
        public void initialize() {
            HopperCustomizeGui.this.inventory.setItem(12, new ItemStackBuilder(Material.HOPPER)
                    .displayName(Component.text("アイテムの吸取設定", DefinedTextColor.YELLOW))
                    .lore(Component.text("ホッパーがアイテムを吸い取るかどうかや、吸取範囲を変更できます。", DefinedTextColor.GREEN),
                            Component.text("ホッパーのアイテム吸取: ", DefinedTextColor.GREEN).append(mixinHopper.isEnabledFindItem() ? Component.text("有効", DefinedTextColor.GREEN) : Component.text("無効", DefinedTextColor.RED)))
                    .build());

            HopperCustomizeGui.this.inventory.setItem(14, new ItemStackBuilder(Material.COMPARATOR)
                    .displayName(Component.text("アイテムフィルター設定", DefinedTextColor.GREEN))
                    .lore(Component.text("ホッパーが吸い取るアイテムにフィルターを設定します。", DefinedTextColor.GREEN),
                            Component.text("アイテムフィルター: ", DefinedTextColor.GREEN).append(mixinHopper.isFilterEnabled() ? Component.text("有効", DefinedTextColor.GREEN).append(mixinHopper.getFilterMode() == FilterType.WHITELIST ? Component.text(" (ホワイトリスト)", DefinedTextColor.AQUA) : Component.text(" (ブラックリスト)", DefinedTextColor.YELLOW)) : Component.text("無効", DefinedTextColor.RED)),
                            Component.text("アイテムフィルター登録数: " + mixinHopper.getFilters().size() + "件", DefinedTextColor.GREEN))
                    .build());
        }

        @Override
        public void onClick(InventoryClickEvent event) {
            switch(event.getSlot()) {
                case 12 -> {
                    new ConfigureFindItemView();
                }

                case 14 -> {
                    new ConfigureFilterItemView();
                }
            }
        }

        @Override
        public void clearInventory() {
            HopperCustomizeGui.this.inventory.setItem(12, null);
            HopperCustomizeGui.this.inventory.setItem(14, null);
        }

        public class ConfigureFindItemView extends MainView {
            @Override
            public void initialize() {
                boolean isEnabledFindItem = HopperCustomizeGui.this.mixinHopper.isEnabledFindItem();
                HopperCustomizeGui.this.inventory.setItem(12, new ItemStackBuilder(isEnabledFindItem ? Material.LIME_WOOL : Material.RED_WOOL)
                        .displayName(isEnabledFindItem ? Component.text("吸引: 有効", DefinedTextColor.GREEN) : Component.text("", DefinedTextColor.RED))
                        .build());
            }

            @Override
            public void onClick(InventoryClickEvent event) {
                switch (event.getSlot()) {
                    case 12 -> {
                        HopperCustomizeGui.this.mixinHopper.setEnabledFindItem(HopperCustomizeGui.this.mixinHopper.isEnabledFindItem());
                        this.clearInventory();
                        this.initialize();
                    }
                }
            }

            @Override
            public void clearInventory() {

            }
        }

        public class ConfigureFilterItemView extends MainView {
            @Override
            public void initialize() {

            }

            @Override
            public void onClick(InventoryClickEvent event) {

            }

            @Override
            public void clearInventory() {

            }
        }
    }

    public static class Listener implements org.bukkit.event.Listener {
        @EventHandler
        public void onInteractHopper(PlayerInteractEvent event) {
            if (event.getHand() != EquipmentSlot.HAND || event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
            if (event.getClickedBlock() == null || event.getClickedBlock().getType() != Material.HOPPER) return;
            if (event.getItem().getType() != Material.STICK || !event.getPlayer().isSneaking()) return;
            HopperBlockEntity hopper = ((CraftHopper) event.getClickedBlock().getState()).getTileEntity();
            IMixinHopperBlockEntity mixinHopper = ((IMixinHopperBlockEntity) hopper);
            HopperCustomizeGui gui = new HopperCustomizeGui(event.getPlayer(), hopper, mixinHopper);
            gui.open(event.getPlayer());
        }
    }
}
