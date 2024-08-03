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

package net.unknown.survival.gui.protection;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.unknown.core.define.DefinedItemStackBuilders;
import net.unknown.core.gui.GuiBase;
import net.unknown.core.gui.view.View;
import net.unknown.survival.gui.MainGui;
import net.unknown.survival.gui.protection.view.ProtectionGuiRegionsView;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;

import java.util.*;

// TODO RegionAreaが他人と被っているときの処理
// TODO 対象ワールドの処理 (範囲選択時にprivateフィールドにWorldを保持しておく？)
// TODO FlagEditor
// TODO
public class ProtectionGui extends GuiBase {
    private static final Map<UUID, ProtectionGui> TEMP_DATA = new HashMap<>();

    private final Player player;
    private ProtectionGuiState guiState;
    private View view;

    private ProtectionGui(Player owner) {
        super(owner,
                9 * 6,
                Component.text("保護", TextColor.color(0xFF00)),
                false);
        this.player = owner;

        this.guiState = ProtectionGuiState.REGIONS;
        this.view = new ProtectionGuiRegionsView(this);

        this.inventory.setItem(45, DefinedItemStackBuilders.leftArrow()
                .displayName(Component.text("戻る", TextColor.color(5635925)))
                .build());
    }

    public static ProtectionGui of(Player owner) {
        return TEMP_DATA.getOrDefault(owner.getUniqueId(), new ProtectionGui(owner));
    }

    @Override
    public void onClick(InventoryClickEvent event) {
        if (event.getSlot() == 45 && this.guiState == ProtectionGuiState.REGIONS) {
            event.getWhoClicked().openInventory(MainGui.getGui().getInventory());
        } else {
            this.view.onClick(event);
        }
    }

    @Override
    public void onClose(InventoryCloseEvent event) {
        if (this.guiState != ProtectionGuiState.WAITING_CALLBACK && this.guiState != ProtectionGuiState.NEW_REGION) {
            TEMP_DATA.remove(this.player.getUniqueId());
            this.unRegisterAsListener();
        }
        if (this.guiState == ProtectionGuiState.NEW_REGION) {
            TEMP_DATA.put(this.player.getUniqueId(), this);
        }
    }

    public Player getPlayer() {
        return this.player;
    }

    public View getView() {
        return this.view;
    }

    public void setView(View view) {
        this.view = view;
    }

    public ProtectionGuiState getGuiState() {
        return this.guiState;
    }

    public void setGuiState(ProtectionGuiState guiState) {
        this.guiState = guiState;
    }
}
