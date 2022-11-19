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

package net.unknown.survival.gui.prefix.view;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import net.unknown.core.builder.ItemStackBuilder;
import net.unknown.core.define.DefinedTextColor;
import net.unknown.core.gui.SignGui;
import net.unknown.core.prefix.PlayerPrefixes;
import net.unknown.core.prefix.Prefix;
import net.unknown.core.util.NewMessageUtil;
import net.unknown.survival.gui.MainGui;
import net.unknown.survival.gui.prefix.PrefixGui;
import net.unknown.core.gui.view.PaginationView;
import net.unknown.survival.gui.prefix.PrefixGuiState;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryCloseEvent;

public class PrefixesView extends PaginationView<Prefix> {
    public PrefixesView(PrefixGui gui, Player player) {
        super(gui, PlayerPrefixes.getPrefixesSorted(player),
                (prefix) -> new ItemStackBuilder(Material.NAME_TAG)
                        .displayName(prefix.getPrefix())
                        .lore(Component.empty(), Component.text("クリックで接頭辞を適用", Style.style(DefinedTextColor.GREEN, TextDecoration.BOLD)), Component.text("Shiftを押しながら右クリックで削除", Style.style(DefinedTextColor.RED, TextDecoration.BOLD)))
                        .build(),
                (event, prefix) -> {
                    if (event.getClick() == ClickType.SHIFT_RIGHT) {
                        PlayerPrefixes.removePrefix(event.getWhoClicked().getUniqueId(), prefix);
                        event.getWhoClicked().closeInventory();
                        NewMessageUtil.sendMessage(event.getWhoClicked(), Component.empty()
                                .append(Component.text("接頭辞 "))
                                .append(prefix.getPrefix())
                                .append(Component.text(" を"))
                                .append(Component.text("削除", DefinedTextColor.RED))
                                .append(Component.text("しました")));
                    } else {
                        PlayerPrefixes.setPrefix(event.getWhoClicked().getUniqueId(), prefix);
                        event.getWhoClicked().closeInventory();
                        NewMessageUtil.sendMessage(event.getWhoClicked(), Component.empty()
                                .append(Component.text("接頭辞を "))
                                .append(prefix.getPrefix())
                                .append(Component.text(" に変更しました")));
                    }
                },
                (event, view) -> {
                    gui.setState(PrefixGuiState.WAITING_CALLBACK);
                    new SignGui()
                            .withTarget(player)
                            .withLines(Component.empty(),
                                    Component.text("^^^^^^^^^^^^^^^^^^^^"),
                                    Component.text("接頭辞を入力"),
                                    Component.text("\"&r\"が最後ﾆ挿入ｻﾚﾏｽ"))
                            .onComplete(lines -> {
                                String raw = "&7[&r" + PlainTextComponentSerializer.plainText().serialize(lines.get(0)) + "&7]&r";
                                Component colored = LegacyComponentSerializer.legacyAmpersand().deserialize(raw);
                                Prefix prefix = PlayerPrefixes.addPrefix(player, colored);
                                NewMessageUtil.sendMessage(event.getWhoClicked(), Component.empty()
                                        .append(Component.text("接頭辞 "))
                                        .append(prefix.getPrefix())
                                        .append(Component.text(" を追加しました"))); // "&r " が挿入されているはずなので、スペースは無し TODO: (ChatManagerのほうでスペースを挿入するか？)
                                player.openInventory(gui.getInventory());
                                gui.setState(PrefixGuiState.AVAILABLE_PREFIXES);
                                view.setData(PlayerPrefixes.getPrefixesSorted(player), true);
                            }).open();
                },
                (event, view) -> {
                    event.getWhoClicked().closeInventory(InventoryCloseEvent.Reason.PLUGIN);
                    MainGui.getGui().open(event.getWhoClicked());
                });
        this.initialize();
    }
}
