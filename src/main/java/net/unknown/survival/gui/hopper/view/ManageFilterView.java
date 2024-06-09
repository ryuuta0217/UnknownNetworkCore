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

public class ManageFilterView extends ConfigureHopperViewBase {
    public ManageFilterView(ConfigureHopperViewBase parentView) {
        super(parentView);
    }

    @Override
    public void initialize() {
        FilterType incomingFilterMode = this.getGui().getMixinHopper().getIncomingFilterMode();
        boolean isIncomingFilterEnabled = this.getGui().getMixinHopper().isIncomingFilterEnabled();
        this.getGui().getInventory().setItem(12, new ItemStackBuilder(isIncomingFilterEnabled ? (incomingFilterMode == FilterType.WHITELIST ? Material.WHITE_WOOL : Material.BLACK_WOOL) : Material.RED_WOOL)
                .displayName(isIncomingFilterEnabled ? Component.text("搬入フィルター: 有効 (モード: " + incomingFilterMode.getLocalizedName() + ")", DefinedTextColor.GREEN) : Component.text("搬入フィルター: 無効", DefinedTextColor.RED))
                .build());

        this.getGui().getInventory().setItem(14, new ItemStackBuilder(Material.COMPARATOR)
                .displayName(Component.text("搬入フィルターの管理", DefinedTextColor.GREEN))
                .build());

        FilterType outgoingFilterMode = this.getGui().getMixinHopper().getOutgoingFilterMode();
        boolean isOutgoingFilterEnabled = this.getGui().getMixinHopper().isOutgoingFilterEnabled();
        this.getGui().getInventory().setItem(30, new ItemStackBuilder(isOutgoingFilterEnabled ? (outgoingFilterMode == FilterType.WHITELIST ? Material.WHITE_WOOL : Material.BLACK_WOOL) : Material.RED_WOOL)
                .displayName(isOutgoingFilterEnabled ? Component.text("搬出フィルター: 有効 (モード: " + outgoingFilterMode.getLocalizedName() + ")", DefinedTextColor.GREEN) : Component.text("搬出フィルター: 無効", DefinedTextColor.RED))
                .build());

        this.getGui().getInventory().setItem(32, new ItemStackBuilder(Material.COMPARATOR)
                .displayName(Component.text("搬出フィルターの管理", DefinedTextColor.GREEN))
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
            case 12 -> {
                int nextMode = ((this.getGui().getMixinHopper().getIncomingFilterMode().ordinal() + 1) % 3);
                this.getGui().getMixinHopper().setIncomingFilterMode(FilterType.values()[nextMode]);
                this.getGui().getView().clearInventory();
                this.getGui().getView().initialize();
            }

            case 14 -> {
                this.getGui().getView().clearInventory();
                this.getGui().setView(new FiltersView(this, true));
                this.getGui().getView().initialize();
            }

            case 30 -> {
                int nextMode = ((this.getGui().getMixinHopper().getOutgoingFilterMode().ordinal() + 1) % 3);
                this.getGui().getMixinHopper().setOutgoingFilterMode(FilterType.values()[nextMode]);
                this.getGui().getView().clearInventory();
                this.getGui().getView().initialize();
            }

            case 32 -> {
                this.getGui().getView().clearInventory();
                this.getGui().setView(new FiltersView(this, false));
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
        this.getGui().getInventory().clear(12);
        this.getGui().getInventory().clear(14);
        this.getGui().getInventory().clear(30);
        this.getGui().getInventory().clear(32);
        this.getGui().getInventory().clear(45);
    }
}
