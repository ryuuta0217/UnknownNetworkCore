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

package net.unknown.survival.gui.home.view;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import net.unknown.core.builder.ItemStackBuilder;
import net.unknown.core.define.DefinedTextColor;
import net.unknown.core.gui.SignGui;
import net.unknown.core.gui.view.PaginationView;
import net.unknown.core.util.NewMessageUtil;
import net.unknown.survival.data.PlayerData;
import net.unknown.survival.data.model.HomeGroup;
import net.unknown.survival.gui.MainGui;
import net.unknown.survival.gui.home.HomeGui;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

public class HomeGroupsView extends PaginationView<HomeGroup, HomeGui> {
    public HomeGroupsView(HomeGui gui) {
        super(gui,
                PlayerData.of(gui.getPlayer()).getHomeData().getGroups().values(),
                (homeGroup) -> new ItemStackBuilder(homeGroup.getIcon())
                        .displayName(Component.text(homeGroup.getName(), DefinedTextColor.GREEN))
                        .lore(Component.text("登録ホーム数: " + homeGroup.getHomes().size(), DefinedTextColor.GREEN),
                                Component.text(""),
                                Component.text("持ち物からアイテムを掴んで右クリックで", DefinedTextColor.AQUA),
                                Component.text("          表示アイテムを変更できます", DefinedTextColor.AQUA))
                        .build(),
                true,
                true);
    }

    @Override
    public void onElementButtonClicked(InventoryClickEvent event, HomeGroup homeGroup) {
        this.getGui().setView(new HomesView(this.getGui(), this, homeGroup));
    }

    @Override
    public void onPreviousButtonClicked(InventoryClickEvent event) {
        event.getWhoClicked().closeInventory();
        MainGui.getGui().open(event.getWhoClicked());
    }

    @Override
    public void onCreateNewButtonClicked(InventoryClickEvent event) {
        this.getGui().onceDeferUnregisterOnClose();
        event.getWhoClicked().closeInventory();

        SignGui sign = new SignGui().withTarget((Player) event.getWhoClicked());

        sign.withLines(Component.empty(),
                        Component.text("^^^^^^^^^^^^^^"),
                        Component.text("グループ名を入力"),
                        Component.empty());

        sign.onComplete(lines -> {
            String newCategoryName = PlainTextComponentSerializer.plainText().serialize(lines.get(0));
            if (newCategoryName.contains(".") || newCategoryName.contains(":")) {
                NewMessageUtil.sendErrorMessage(event.getWhoClicked(), Component.text("使用出来ない文字列が含まれています。やり直してください。"));
                sign.open();
            } else {
                if (newCategoryName.length() > 0) {
                    // TODO insert exists check here
                    PlayerData.of(this.getGui().getPlayer()).getHomeData().createGroup(newCategoryName, null);
                }

                this.getGui().open(event.getWhoClicked());
                this.clearInventory();
                this.setData(PlayerData.of(this.getGui().getPlayer()).getHomeData().getGroups().values(), false);
                this.initialize();
            }
        });

        sign.open();
    }
}
