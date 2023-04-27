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
import net.unknown.core.define.DefinedItemStackBuilders;
import net.unknown.core.define.DefinedTextColor;
import net.unknown.launchwrapper.hopper.FilterType;
import org.bukkit.Material;
import org.bukkit.event.inventory.InventoryClickEvent;

public class ConfigureHopperFilterView extends ConfigureHopperViewBase {
    public ConfigureHopperFilterView(ConfigureHopperViewBase parentView) {
        super(parentView);
    }

    @Override
    public void initialize() {
        FilterType filterMode = this.getGui().getMixinHopper().getFilterMode();
        boolean isFilterEnabled = this.getGui().getMixinHopper().isFilterEnabled();
        this.getGui().getInventory().setItem(21, new ItemStackBuilder(isFilterEnabled ? Material.LIME_WOOL : Material.RED_WOOL)
                .displayName(isFilterEnabled ? Component.text("フィルター: 有効 (モード: " + filterMode.getLocalizedName() + ")", DefinedTextColor.GREEN) : Component.text("フィルター: 無効", DefinedTextColor.RED))
                .build());

        this.getGui().getInventory().setItem(23, new ItemStackBuilder(Material.COMPARATOR)
                .displayName(Component.text("フィルターの管理", DefinedTextColor.GREEN))
                .build());

        if (this.getParentView() != null) {
            this.getGui().getInventory().setItem(45, DefinedItemStackBuilders.leftArrow()
                    .displayName(Component.text("戻る", DefinedTextColor.YELLOW))
                    .build());
        }
    }

    @Override
    public void onClick(InventoryClickEvent event) {
        switch (event.getSlot()) {
            case 21 -> {
                int nextMode = ((this.getGui().getMixinHopper().getFilterMode().ordinal() + 1) % 3);
                this.getGui().getMixinHopper().setFilterMode(FilterType.values()[nextMode]);
                this.getGui().getView().clearInventory();
                this.getGui().getView().initialize();
            }

            case 23 -> {
                this.getGui().getView().clearInventory();
                this.getGui().setView(new ConfigureHopperFilterManageView(this));
                this.getGui().getView().initialize();
            }

            case 45 -> {
                this.getGui().getView().clearInventory();
                this.getGui().setView(this.getParentView());
                this.getGui().getView().initialize();
            }
        }
    }

    @Override
    public void clearInventory() {
        this.getGui().getInventory().clear(21);
        this.getGui().getInventory().clear(23);
        this.getGui().getInventory().clear(45);
    }
}
