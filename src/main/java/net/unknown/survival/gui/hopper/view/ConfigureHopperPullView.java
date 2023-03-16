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
import net.unknown.core.gui.view.View;
import net.unknown.survival.gui.hopper.ConfigureHopperGui;
import org.bukkit.Material;
import org.bukkit.event.inventory.InventoryClickEvent;

public class ConfigureHopperPullView extends ConfigureHopperViewBase {
    public ConfigureHopperPullView(ConfigureHopperViewBase parentView) {
        super(parentView);
    }

    @Override
    public void initialize() {
        this.getGui().getInventory().setItem(12, new ItemStackBuilder(this.getGui().getMixinHopper().isEnabledFindItem() ? Material.LIME_WOOL : Material.RED_WOOL)
                .displayName(this.getGui().getMixinHopper().isEnabledFindItem() ? Component.text("吸引: 有効", DefinedTextColor.GREEN) : Component.text("", DefinedTextColor.RED))
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
                this.getGui().getMixinHopper().setEnabledFindItem(!this.getGui().getMixinHopper().isEnabledFindItem());
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
        this.getGui().getInventory().clear(12);
        this.getGui().getInventory().clear(45);
    }
}
