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
import net.unknown.launchwrapper.hopper.IMixinHopperBlockEntity;
import org.bukkit.Material;
import org.bukkit.event.inventory.InventoryClickEvent;

public class ManageTransportView extends ConfigureHopperViewBase {
    public ManageTransportView(ConfigureHopperViewBase parentView) {
        super(parentView);
    }

    @Override
    public void initialize() {
        this.getGui().getInventory().setItem(21, new ItemStackBuilder(getIncomingStatusItem(this.getGui().getMixinHopper()))
                .displayName(getIncomingStatusComponent(this.getGui().getMixinHopper()))
                .build());

        this.getGui().getInventory().setItem(23, new ItemStackBuilder(this.getGui().getMixinHopper().isEnabledPushItem() ? Material.LIME_WOOL : Material.RED_WOOL)
                .displayName(this.getGui().getMixinHopper().isEnabledPushItem() ? Component.text("搬出: 有効", DefinedTextColor.GREEN) : Component.text("搬出: 無効", DefinedTextColor.RED))
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
                boolean findItem = this.getGui().getMixinHopper().isEnabledFindItem();
                boolean pullItem = this.getGui().getMixinHopper().isEnabledPullItem();
                int step = findItem && pullItem ? 0 : findItem ? 1 : pullItem ? 2 : 3;
                switch(step) {
                    case 0 -> { // Next step is 1
                        this.getGui().getMixinHopper().setEnabledFindItem(true);
                        this.getGui().getMixinHopper().setEnabledPullItem(false);
                    }

                    case 1 -> { // Next step is 2
                        this.getGui().getMixinHopper().setEnabledFindItem(false);
                        this.getGui().getMixinHopper().setEnabledPullItem(true);
                    }

                    case 2 -> { // Next step is 3
                        this.getGui().getMixinHopper().setEnabledFindItem(false);
                        this.getGui().getMixinHopper().setEnabledPullItem(false);
                    }

                    case 3 -> { // Next step is 0
                        this.getGui().getMixinHopper().setEnabledFindItem(true);
                        this.getGui().getMixinHopper().setEnabledPullItem(true);
                    }
                }
                this.initialize();
            }

            case 23 -> {
                this.getGui().getMixinHopper().setEnabledPushItem(!this.getGui().getMixinHopper().isEnabledPushItem());
                this.initialize();
            }

            case 45 -> {
                if (this.getParentView() != null) {
                    this.getGui().setView(this.getParentView());
                }
            }
        }
    }

    @Override
    public void clearInventory() {
        this.getGui().getInventory().clear(22);
        this.getGui().getInventory().clear(45);
    }

    public static Component getIncomingStatusComponent(IMixinHopperBlockEntity hopper) {
        // 有効 (true, true) -> アイテムのみ有効 (true, false) -> コンテナのみ有効 (false, true) -> 無効 (false, false)
        boolean findItem = hopper.isEnabledFindItem();
        boolean pullItem = hopper.isEnabledPullItem();
        if (findItem && pullItem) return Component.text("搬入: 有効", DefinedTextColor.GREEN);
        if (findItem) return Component.text("搬入: アイテムのみ有効", DefinedTextColor.YELLOW);
        if (pullItem) return Component.text("搬入: コンテナのみ有効", DefinedTextColor.YELLOW);
        if (!findItem && !pullItem) return Component.text("搬入: 無効", DefinedTextColor.RED);
        return Component.text("搬入: 不明", DefinedTextColor.GRAY);
    }

    public static Material getIncomingStatusItem(IMixinHopperBlockEntity hopper) {
        boolean findItem = hopper.isEnabledFindItem();
        boolean pullItem = hopper.isEnabledPullItem();
        if (findItem && pullItem) return Material.LIME_WOOL;
        if (findItem) return Material.YELLOW_WOOL;
        if (pullItem) return Material.YELLOW_WOOL;
        if (!findItem && !pullItem) return Material.RED_WOOL;
        return Material.GRAY_WOOL;
    }
}
