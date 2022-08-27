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

package net.unknown.survival.gui.warp;

import net.kyori.adventure.text.Component;
import net.unknown.core.define.DefinedItemStackBuilders;
import net.unknown.core.define.DefinedTextColor;
import net.unknown.core.gui.GuiBase;
import net.unknown.core.gui.view.View;
import net.unknown.survival.gui.warp.views.WarpsView;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;

public class WarpGui extends GuiBase {
    private final GuiBase parent;
    private final Player opener;
    private View view;
    private WarpGuiState state;

    public WarpGui(GuiBase parent, Player opener) {
        super(opener, 54, Component.text("ワープ", DefinedTextColor.GREEN), false);
        this.parent = parent;
        this.opener = opener;

        this.view = new WarpsView(this);
        this.state = WarpGuiState.AVAILABLE_WARPS;
        this.view.initialize();

        this.getInventory().setItem(45, DefinedItemStackBuilders.leftArrow()
                .displayName(Component.text("戻る", DefinedTextColor.GREEN))
                .build());
    }

    public void setView(View view, WarpGuiState state) {
        this.view = view;
        this.state = state;
    }

    @Override
    public void onClick(InventoryClickEvent event) {
        if(event.getSlot() == 45) {
            event.getWhoClicked().openInventory(this.parent.getInventory());
        } else this.view.onClick(event);
    }

    @Override
    public void onClose(InventoryCloseEvent event) {
        if(this.state != WarpGuiState.WAITING_CALLBACK) this.unRegisterAsListener();
    }
}
