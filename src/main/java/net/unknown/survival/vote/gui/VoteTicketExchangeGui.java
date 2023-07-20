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

package net.unknown.survival.vote.gui;

import net.kyori.adventure.text.Component;
import net.unknown.core.define.DefinedTextColor;
import net.unknown.core.gui.GuiBase;
import net.unknown.core.gui.view.View;
import net.unknown.survival.vote.gui.view.ChooseExchangeItemView;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;

public class VoteTicketExchangeGui extends GuiBase {
    private final Player player;
    private View view;

    public VoteTicketExchangeGui(GuiBase parent, Player player) {
        super(player, 54, Component.text("投票チケット交換", DefinedTextColor.DARK_GREEN), true);
        this.player = player;
        this.view = new ChooseExchangeItemView(this, parent != null ? (event, view) -> parent.open(event.getWhoClicked()) : null);
    }

    @Override
    public void onOpen(InventoryOpenEvent event) {
        this.view.initialize();
    }

    @Override
    public void onClick(InventoryClickEvent event) {
        this.view.onClick(event);
    }

    @Override
    protected void unRegisterAsListener() {
        super.unRegisterAsListener();
    }

    public Player getPlayer() {
        return this.player;
    }

    public View getView() {
        return this.view;
    }

    public View setView(View newView) {
        View oldView = this.getView();
        oldView.clearInventory();

        this.view = newView;
        this.getView().initialize();
        return oldView;
    }

    public void open() {
        super.open(this.player);
    }
}
