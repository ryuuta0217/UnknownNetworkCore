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
import net.unknown.core.builder.ItemStackBuilder;
import net.unknown.core.define.DefinedTextColor;
import net.unknown.launchwrapper.hopper.FilterType;
import net.unknown.survival.gui.hopper.ConfigureHopperGui;
import org.bukkit.Material;
import org.bukkit.event.inventory.InventoryClickEvent;

public class ConfigureHopperView extends ConfigureHopperViewBase {
    public ConfigureHopperView(ConfigureHopperGui gui) {
        super(gui);
    }

    @Override
    public void initialize() {
        this.getGui().getInventory().setItem(12, new ItemStackBuilder(Material.HOPPER)
                .displayName(Component.text("アイテムの吸取設定", DefinedTextColor.YELLOW))
                .lore(Component.text("ホッパーがアイテムを吸い取るかどうかや、吸取範囲を変更できます。", DefinedTextColor.GREEN),
                        Component.text("ホッパーのアイテム吸取: ", DefinedTextColor.GREEN).append(this.getGui().getMixinHopper().isEnabledFindItem() ? Component.text("有効", DefinedTextColor.GREEN) : Component.text("無効", DefinedTextColor.RED)))
                .build());

        this.getGui().getInventory().setItem(14, new ItemStackBuilder(Material.COMPARATOR)
                .displayName(Component.text("アイテムフィルター設定", DefinedTextColor.GREEN))
                .lore(Component.text("ホッパーが吸い取るアイテムにフィルターを設定します。", DefinedTextColor.GREEN),
                        Component.text("アイテムフィルター: ", DefinedTextColor.GREEN).append(this.getGui().getMixinHopper().isFilterEnabled() ? Component.text("有効", DefinedTextColor.GREEN).append(this.getGui().getMixinHopper().getFilterMode() == FilterType.WHITELIST ? Component.text(" (ホワイトリスト)", DefinedTextColor.AQUA) : Component.text(" (ブラックリスト)", DefinedTextColor.YELLOW)) : Component.text("無効", DefinedTextColor.RED)),
                        Component.text("アイテムフィルター登録数: " + this.getGui().getMixinHopper().getFilters().size() + "件", DefinedTextColor.GREEN))
                .build());
    }

    @Override
    public void onClick(InventoryClickEvent event) {
        switch (event.getSlot()) {
            case 12 -> {
                this.getGui().setView(new ConfigureHopperPullView(this));
            }

            case 14 -> {
                this.getGui().setView(new ConfigureHopperFilterView(this));
            }
        }
    }

    @Override
    public void clearInventory() {
        this.getGui().getInventory().clear(12);
        this.getGui().getInventory().clear(14);
    }
}
