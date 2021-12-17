/*
 * Copyright (c) 2021 Unknown Network Developers and contributors.
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
 *     loss of use data or profits; or business interpution) however caused and on any theory of liability,
 *     whether in contract, strict liability, or tort (including negligence or otherwise)
 *     arising in any way out of the use of this source code, event if advised of the possibility of such damage.
 */

package net.unknown.survival.gui;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.unknown.core.builder.ItemStackBuilder;
import net.unknown.core.gui.GuiBase;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

public class MainGui extends GuiBase {
    private static final MainGui GUI = new MainGui();

    public MainGui() {
        super(null, 27, Component.text("メインGUI"),
                (inv) -> {
                    inv.setItem(13, new ItemStackBuilder(Material.RESPAWN_ANCHOR)
                            .displayName(Component.text("Home", TextColor.color(16755200)))
                            .lore(Component.text("ホームを追加したり、テレポートしたり、削除したりできます。", TextColor.color(5636095)).decoration(TextDecoration.ITALIC, false))
                            .build());
                }, false);
    }

    @Override
    public void onClick(InventoryClickEvent event) {
        if(event.getSlot() == 13) {
            event.getWhoClicked().openInventory(new HomeGui((Player) event.getWhoClicked()).getInventory());
            //event.getWhoClicked().sendMessage(Component.text("まだです..."));
        }
    }

    public static MainGui getGui() {
        return GUI;
    }
}
