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

package net.unknown.survival.gui;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.unknown.core.builder.ItemStackBuilder;
import net.unknown.core.define.DefinedTextColor;
import net.unknown.core.gui.GuiBase;
import net.unknown.core.util.MessageUtil;
import net.unknown.survival.UnknownNetworkSurvival;
import net.unknown.survival.gui.prefix.PrefixGui;
import net.unknown.survival.gui.warp.WarpGui;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;

public class MainGui extends GuiBase {
    private static final MainGui GUI = new MainGui();

    public MainGui() {
        super(null, 27, Component.text("メインGUI"),
                (inv) -> {
                    inv.setItem(12, new ItemStackBuilder(Material.RESPAWN_ANCHOR)
                            .displayName(Component.text("Home", DefinedTextColor.GOLD))
                            .lore(Component.text("ホームを追加したり", DefinedTextColor.GREEN),
                                    Component.text("テレポートしたり", DefinedTextColor.YELLOW),
                                    Component.text("削除したり", DefinedTextColor.RED),
                                    Component.text("できます。", DefinedTextColor.GREEN))
                            .build());

                    if (UnknownNetworkSurvival.isWorldGuardEnabled()) {
                        inv.setItem(13, new ItemStackBuilder(Material.WOODEN_AXE)
                                .displayName(Component.text("保護", DefinedTextColor.YELLOW))
                                .lore(Component.text("建物を保護しよう", DefinedTextColor.GREEN))
                                .itemFlags(ItemFlag.HIDE_ATTRIBUTES)
                                .build());
                    }

                    inv.setItem(14, new ItemStackBuilder(Material.END_PORTAL_FRAME)
                            .displayName(Component.text("ワープ", DefinedTextColor.LIGHT_PURPLE))
                            .lore(Component.text("・公開ワープポイントへのTP", DefinedTextColor.YELLOW),
                                    Component.text("・新しい公開ワープポイントの設定", DefinedTextColor.GREEN),
                                    Component.text("・設定した公開ワープポイントの削除", DefinedTextColor.RED),
                                    Component.text("が行えます。", DefinedTextColor.GOLD))
                            .build());

                    inv.setItem(15, new ItemStackBuilder(Material.NAME_TAG)
                            .displayName(Component.text("接頭辞の設定", DefinedTextColor.YELLOW))
                            .lore(Component.text("接頭辞の追加、変更はこちら", DefinedTextColor.GREEN))
                            .build());

                    inv.setItem(18, new ItemStackBuilder(Material.PLAYER_HEAD)
                            .displayName(Component.text("頭に被る", TextColor.color(0x657CFF)))
                            .lore(Component.text("アイテムを持ってここを左クリックで頭に被ります", TextColor.color(0xFF76A6)))
                            .build());
                }, false);
    }

    public static MainGui getGui() {
        return GUI;
    }

    @Override
    public void onClick(InventoryClickEvent event) {
        switch (event.getSlot()) {
            case 12 -> event.getWhoClicked().openInventory(new HomeGui((Player) event.getWhoClicked()).getInventory());
            case 13 -> {
                if (UnknownNetworkSurvival.isWorldGuardEnabled()) {
                    event.getWhoClicked().openInventory(ProtectionGui.of((Player) event.getWhoClicked()).getInventory());
                }
            }
            case 14 -> event.getWhoClicked().openInventory(new WarpGui(this, (Player) event.getWhoClicked()).getInventory());
            case 15 -> event.getWhoClicked().openInventory(new PrefixGui(this, (Player) event.getWhoClicked()).getInventory());
            case 18 -> {
                if (event.getCursor() == null || event.getCursor().getType() == Material.AIR) return;
                if (event.getClick() != ClickType.LEFT) return;
                ItemStack newHead = event.getView().getCursor();
                ItemStack oldHead = event.getWhoClicked().getInventory().getHelmet();
                event.getWhoClicked().getInventory().setHelmet(newHead);
                event.getView().setCursor(oldHead);
                MessageUtil.sendMessage((Player) event.getWhoClicked(), "アイテムを頭に被りました");
            }
        }
    }
}
