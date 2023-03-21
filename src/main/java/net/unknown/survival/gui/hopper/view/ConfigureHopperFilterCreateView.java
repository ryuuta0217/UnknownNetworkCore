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
import org.bukkit.Material;
import org.bukkit.event.inventory.InventoryClickEvent;

public class ConfigureHopperFilterCreateView extends ConfigureHopperViewBase {
    public ConfigureHopperFilterCreateView(ConfigureHopperViewBase parentView) {
        super(parentView);
    }

    @Override
    public void initialize() {
        for (int i = 0; i < this.getGui().getInventory().getSize(); i++) {
            this.getGui().getInventory().setItem(i, new ItemStackBuilder(Material.RED_STAINED_GLASS_PANE)
                    .displayName(Component.empty())
                    .build());
        }

        // S L O T S
        // 13: insert the item slot (copy item by clickEvent)
        // 31: Equals Tag (use Material.NAME_TAG) right click to edit tag using BookGui TODO: create BookGui class
        // 40: Confirm (create filter)
        // 45: Back, cancel (to parent view)
    }

    @Override
    public void onClick(InventoryClickEvent event) {

    }

    @Override
    public void clearInventory() {

    }
}
