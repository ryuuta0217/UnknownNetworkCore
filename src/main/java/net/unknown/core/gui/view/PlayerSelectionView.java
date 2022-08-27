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

package net.unknown.core.gui.view;

import com.ryuuta0217.util.ListUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import net.unknown.core.builder.ItemStackBuilder;
import net.unknown.core.define.DefinedItemStackBuilders;
import net.unknown.core.define.DefinedTextColor;
import net.unknown.core.gui.GuiBase;
import net.unknown.core.gui.SignGui;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.meta.SkullMeta;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.stream.IntStream;

public class PlayerSelectionView implements View {
    private final GuiBase gui;
    private final boolean allowCustomInput;
    private final Consumer<InventoryClickEvent> onBack;
    private final Consumer<PlayerSelectionView> beforeOpenCustomInput;
    private final Consumer<String> customInputError;
    private final Consumer<OfflinePlayer> onSelected;
    private List<Set<OfflinePlayer>> players;
    private final Map<Integer, OfflinePlayer> slot2player = new HashMap<Integer, OfflinePlayer>();
    private int currentPage = 1;

    public PlayerSelectionView(GuiBase gui, boolean allowCustomInput, @Nullable List<OfflinePlayer> players, @Nullable Consumer<InventoryClickEvent> onBack,
                               @Nullable Consumer<PlayerSelectionView> beforeOpenCustomInput,
                               @Nullable Consumer<String> customInputError, @Nonnull Consumer<OfflinePlayer> onSelected) {
        this.gui = gui;
        this.allowCustomInput = allowCustomInput;
        this.onBack = onBack;
        this.beforeOpenCustomInput = beforeOpenCustomInput;
        this.customInputError = customInputError;
        if (this.allowCustomInput) {
            Objects.requireNonNull(this.beforeOpenCustomInput);
            Objects.requireNonNull(this.customInputError);
        }
        this.onSelected = onSelected;
        if (players != null) {
            this.players = ListUtil.splitListAsLinkedSet(players.stream()
                    .filter(Objects::nonNull)
                    .toList(), 44);
        }
        this.initialize();
    }

    @Override
    public void initialize() {
        if (this.players == null) {
            this.players = ListUtil.splitListAsLinkedSet(Bukkit.getOnlinePlayers()
                    .stream()
                    .filter(Objects::nonNull)
                    .map(p -> (OfflinePlayer) p)
                    .toList(), 44);
        }

        this.showPage(1);
    }

    public void showPage(int page) {
        this.currentPage = page;
        this.clearInventoryWithoutBottom();

        AtomicInteger i = new AtomicInteger(0);
        this.players.get(this.currentPage - 1).forEach(p -> {
            gui.getInventory().setItem(i.get(), new ItemStackBuilder(Material.PLAYER_HEAD)
                    .displayName((p.isOnline() ? p.getPlayer().displayName() : Component.text(p.getName())))
                    .custom(is -> {
                        SkullMeta skull = ((SkullMeta) is.getItemMeta());
                        skull.setOwningPlayer(p);
                        is.setItemMeta(skull);
                    })
                    .build());
            this.slot2player.put(i.getAndIncrement(), p);
        });

        if (this.onBack != null) {
            this.gui.getInventory().setItem(45, DefinedItemStackBuilders.leftArrow()
                    .displayName(Component.text("戻る", DefinedTextColor.GREEN))
                    .build());
        }

        if (this.allowCustomInput) {
            this.gui.getInventory().setItem(49, new ItemStackBuilder(Material.OAK_SIGN)
                    .displayName(Component.text("IDを入力する", DefinedTextColor.AQUA))
                    .build());
        }

        if (this.currentPage > 1) {
            this.gui.getInventory().setItem(52, DefinedItemStackBuilders.leftArrow()
                    .displayName(Component.text("前のページ", DefinedTextColor.GREEN))
                    .build());
        } else {
            this.gui.getInventory().clear(52);
        }

        if (this.players.size() > this.currentPage) {
            this.gui.getInventory().setItem(53, DefinedItemStackBuilders.rightArrow()
                    .displayName(Component.text("次のページ", DefinedTextColor.GREEN))
                    .build());
        } else {
            this.gui.getInventory().clear(53);
        }
    }

    @Override
    public void onClick(InventoryClickEvent event) {
        int slot = event.getSlot();
        switch (slot) {
            case 45 -> {
                // Back Button
                if (this.onBack != null) {
                    this.clearInventory();
                    this.onBack.accept(event);
                }
            }

            case 49 -> {
                if (this.allowCustomInput) {
                    this.beforeOpenCustomInput.accept(this);
                    new SignGui().withTarget((Player) event.getWhoClicked())
                            .withLines(Component.empty(),
                                    Component.text("^^^^^^^^^^^^^^^^^^^^^"),
                                    Component.text("プレイヤー名を入力"),
                                    Component.empty())
                            .onComplete(lines -> {
                                String playerName = PlainTextComponentSerializer.plainText().serialize(lines.get(0));
                                UUID playerUniqueId = Bukkit.getPlayerUniqueId(playerName);
                                if (playerUniqueId != null) {
                                    this.clearInventory();
                                    this.onSelected.accept(Bukkit.getOfflinePlayer(playerUniqueId));
                                } else {
                                    this.customInputError.accept(playerName);
                                }
                            }).open();
                }
            }

            case 52 -> {
                if (this.currentPage > 1) {
                    this.showPage(this.currentPage - 1);
                }
            }

            case 53 -> {
                if (this.players.size() > this.currentPage) {
                    this.showPage(this.currentPage + 1);
                }
            }

            default -> {
                if (slot2player.containsKey(slot)) {
                    OfflinePlayer player = slot2player.get(slot);
                    this.clearInventory();
                    this.onSelected.accept(player);
                }
            }
        }
    }

    public void clearInventoryWithoutBottom() {
        this.slot2player.clear();
        IntStream.rangeClosed(0, 44).forEach(this.gui.getInventory()::clear);
    }

    @Override
    public void clearInventory() {
        this.clearInventoryWithoutBottom();
        this.gui.getInventory().clear(45);
        this.gui.getInventory().clear(49);
        this.gui.getInventory().clear(52);
        this.gui.getInventory().clear(53);
    }
}
