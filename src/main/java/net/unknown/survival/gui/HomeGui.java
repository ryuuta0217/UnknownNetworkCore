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

package net.unknown.survival.gui;

import com.ryuuta0217.util.ListUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import net.unknown.core.builder.ItemStackBuilder;
import net.unknown.core.define.DefinedItemStackBuilders;
import net.unknown.core.define.DefinedTextColor;
import net.unknown.core.gui.GuiBase;
import net.unknown.core.gui.SignGui;
import net.unknown.core.util.MessageUtil;
import net.unknown.core.util.NewMessageUtil;
import net.unknown.survival.data.model.Home;
import net.unknown.survival.data.PlayerData;
import net.unknown.survival.data.model.HomeGroup;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;

import java.util.*;
import java.util.stream.IntStream;

public class HomeGui extends GuiBase {
    private final Player target;
    private final PlayerData.HomeData homeData;
    private final Map<Integer, HomeGroup> slot2CategoryMap = new HashMap<>();
    private final Map<Integer, Home> slot2HomeMap = new HashMap<>();
    private State guiState;
    private HomeGroup selectedGroup = null;
    private List<Set<HomeGroup>> splitGroups;
    private List<Set<Home>> splitHomes;
    private int currentPage = 1;

    public HomeGui(Player player) {
        super(player, 54, Component.text("ホーム"), false);
        this.guiState = State.CATEGORIES;

        this.target = player;
        this.homeData = PlayerData.of(this.target).getHomeData();

        this.inventory.setItem(45, DefinedItemStackBuilders.leftArrow().displayName(Component.text("戻る", TextColor.color(5635925))).build());

        this.loadData();

        this.setGroups(this.currentPage);
    }

    private static Material dimension2Material(Location loc) {
        return switch (loc.getWorld().getEnvironment()) {
            case NORMAL -> Material.GRASS_BLOCK;
            case NETHER -> Material.NETHERRACK;
            case THE_END -> Material.END_STONE;
            default -> Material.WHITE_WOOL;
        };
    }

    private static TextColor dimension2TextColor(Location loc) {
        return switch (loc.getWorld().getEnvironment()) {
            case NORMAL -> TextColor.color(5635925);
            case NETHER -> TextColor.color(16733525);
            case THE_END -> TextColor.color(16733695);
            default -> TextColor.color(16777215);
        };
    }

    private static String getCoordinateAsString(Location loc) {
        String x = String.valueOf(loc.getX());
        String y = String.valueOf(loc.getY());
        String z = String.valueOf(loc.getZ());

        return x.substring(0, Math.min(x.indexOf(".") + 2, x.length())) + ", " +
                y.substring(0, Math.min(y.indexOf(".") + 2, y.length())) + ", " +
                z.substring(0, Math.min(z.indexOf(".") + 2, z.length()));
    }

    private static String getRotationAsString(Location loc) {
        String yaw = String.valueOf(loc.getYaw());
        String pitch = String.valueOf(loc.getPitch());

        return yaw.substring(0, Math.min(yaw.indexOf(".") + 2, yaw.length())) + ", " + pitch.substring(0, Math.min(pitch.indexOf(".") + 2, pitch.length()));
    }

    @Override
    public void onClick(InventoryClickEvent event) {
        switch (this.guiState) {
            case CATEGORIES -> {
                if (this.slot2CategoryMap.containsKey(event.getSlot())) {
                    if (event.getClick() == ClickType.LEFT) {
                        this.selectedGroup = this.slot2CategoryMap.get(event.getSlot());
                        this.splitHomes = ListUtil.splitListAsLinkedSet(this.selectedGroup.getHomes().values(), 45);
                        this.clearCategories();
                        this.guiState = State.HOMES;
                        this.currentPage = 1;
                        this.setHomes(this.currentPage);
                    } else if (event.getClick() == ClickType.RIGHT && event.getCursor() != null) {
                        if (event.getCursor().getType() != Material.AIR) {
                            this.slot2CategoryMap.get(event.getSlot()).setIcon(event.getCursor().getType());
                            this.loadData();
                            this.clearCategories();
                            this.setGroups(this.currentPage);
                        }
                    }
                } else if (event.getSlot() == 45) {
                    event.getWhoClicked().openInventory(MainGui.getGui().getInventory());
                } else if (event.getSlot() == 49 && event.getCurrentItem() != null) {
                    this.guiState = State.WAITING_CALLBACK;
                    this.target.closeInventory(InventoryCloseEvent.Reason.PLUGIN);
                    new SignGui().withTarget((Player) event.getWhoClicked())
                            .withLines(Component.empty(),
                                    Component.text("^^^^^^^^^^^^^^"),
                                    Component.text("グループ名を入力"),
                                    Component.empty())
                            .onComplete(lines -> {
                                String newCategoryName = PlainTextComponentSerializer.plainText().serialize(lines.get(0))
                                        .replace(".", "・")
                                        .replace(":", "："); // ぐるーぷ名をConfigのkeyにするので、使えない文字対策
                                if (newCategoryName.length() > 0) {
                                    PlayerData.of(this.target).getHomeData().createGroup(newCategoryName, null);
                                    this.guiState = State.CATEGORIES;
                                } else {
                                    NewMessageUtil.sendMessage(this.target, Component.text(""));
                                }
                                this.target.openInventory(this.getInventory());
                                this.clearCategories();
                                this.loadData();
                                this.setGroups(this.splitGroups.size());
                            }).open();
                } else if (event.getSlot() == 52) {
                    if (this.splitGroups.size() > 1 && this.currentPage > 1) {
                        this.clearCategories();
                        this.setGroups(this.currentPage - 1);
                    }
                } else if (event.getSlot() == 53) {
                    if (this.splitGroups.size() > 1 && this.currentPage < this.splitGroups.size()) {
                        this.clearCategories();
                        this.setGroups(this.currentPage + 1);
                    }
                }
            }
            case HOMES -> {
                if (this.slot2HomeMap.containsKey(event.getSlot())) {
                    if (event.getClick() == ClickType.LEFT) {
                        Home home = this.slot2HomeMap.get(event.getSlot());
                        home.teleportPlayer((Player) event.getWhoClicked());
                        event.getWhoClicked().closeInventory();
                        MessageUtil.sendMessage((Player) event.getWhoClicked(), "ホーム " + home.name() + " にテレポートしました (GUI)");
                    } else if (event.getClick() == ClickType.SHIFT_RIGHT) {
                        this.selectedGroup.removeHome(this.slot2HomeMap.get(event.getSlot()));
                        this.loadData();
                        this.clearHomes();
                        this.setHomes(this.currentPage);
                    }
                } else if (event.getSlot() == 45) {
                    this.guiState = State.CATEGORIES;
                    this.selectedGroup = null;
                    this.currentPage = 1;
                    this.clearHomes();
                    this.loadData();
                    this.setGroups(this.currentPage);
                } else if (event.getSlot() == 52) {
                    if (this.currentPage > 1 && this.splitHomes.size() > 1) {
                        this.clearHomes();
                        this.setHomes(this.currentPage - 1);
                    }
                } else if (event.getSlot() == 53) {
                    if (this.splitHomes.size() > 1) { // 2 pages found
                        if (this.currentPage < this.splitHomes.size()) {
                            this.clearHomes();
                            this.setHomes(this.currentPage + 1);
                        }
                    }
                }
            }
        }
    }

    @Override
    public void onClose(InventoryCloseEvent event) {
        if (this.guiState != State.WAITING_CALLBACK) {
            this.unRegisterAsListener();
        }
    }

    private void loadData() {
        this.splitGroups = null;
        this.splitHomes = null;

        this.splitGroups = ListUtil.splitListAsLinkedSet(homeData.getGroups().values(), 45);
    }

    private void clearCategories() {
        this.slot2CategoryMap.clear();
        IntStream.rangeClosed(0, 44).forEach(this.inventory::clear);
        this.inventory.clear(49);
    }

    private void clearHomes() {
        this.slot2HomeMap.clear();
        IntStream.rangeClosed(0, 44).forEach(this.inventory::clear);
        this.splitHomes = null;
    }

    private void setGroups(int page) {
        this.currentPage = page;

        this.splitGroups.get(page - 1).forEach(group -> {
            this.slot2CategoryMap.put(this.inventory.firstEmpty(), group);
            this.inventory.setItem(this.inventory.firstEmpty(), new ItemStackBuilder(group.getIcon())
                    .displayName(Component.text(group.getName(), DefinedTextColor.GREEN))
                    .lore(Component.text("登録ホーム数: " + group.getHomes().size(), DefinedTextColor.GREEN),
                            Component.text(""),
                            Component.text("持ち物からアイテムを掴んで右クリックで", DefinedTextColor.AQUA),
                            Component.text("          表示アイテムを変更できます", DefinedTextColor.AQUA))
                    .build());
        });

        if (this.splitGroups.size() == this.currentPage) {
            this.inventory.setItem(49, DefinedItemStackBuilders.plus()
                    .displayName(Component.text("新規グループ作成", DefinedTextColor.GREEN))
                    .build());
        }

        // NEXT BUTTON
        if (this.splitGroups.size() > 1 && this.splitGroups.size() > this.currentPage) {
            this.inventory.setItem(53, DefinedItemStackBuilders.rightArrow().displayName(Component.text("次のページ", DefinedTextColor.GREEN)).build());
        } else if (this.splitGroups.size() >= this.currentPage) {
            this.inventory.setItem(53, null);
        }

        // BACK BUTTON
        if (this.splitGroups.size() > 1 && this.currentPage > 1) {
            this.inventory.setItem(52, DefinedItemStackBuilders.leftArrow().displayName(Component.text("前のページ", DefinedTextColor.GREEN)).build());
        } else if (this.splitGroups.size() <= 1) {
            this.inventory.setItem(52, null);
        }
    }

    private void setHomes(int page) {
        this.currentPage = page;

        this.splitHomes.get(page - 1).forEach(home -> {
            this.slot2HomeMap.put(this.inventory.firstEmpty(), home);
            this.inventory.setItem(this.inventory.firstEmpty(), new ItemStackBuilder(dimension2Material(home.location()))
                    .displayName(Component.text(home.name(), Style.style(dimension2TextColor(home.location()), TextDecoration.ITALIC.withState(false))))
                    .lore(Component.text("ワールド: " + MessageUtil.getWorldNameDisplay(home.getWorld()), Style.style(TextColor.color(0, 255, 0), TextDecoration.ITALIC.withState(false))),
                            Component.text("座標: " + getCoordinateAsString(home.location()), Style.style(TextColor.color(0, 255, 0), TextDecoration.ITALIC.withState(false))),
                            Component.text("向き: " + getRotationAsString(home.location()), Style.style(TextColor.color(0, 255, 0), TextDecoration.ITALIC.withState(false))),
                            Component.text(""),
                            Component.text("クリックでテレポート", Style.style(TextColor.color(0, 255, 0), TextDecoration.ITALIC.withState(false))),
                            Component.text("Shiftキーを押しながら右クリックで削除", Style.style(TextColor.color(255, 0, 0), TextDecoration.ITALIC.withState(false))))
                    .build());
        });

        // NEXT BUTTON
        if (this.splitHomes.size() > 1 && this.splitHomes.size() > this.currentPage) {
            this.inventory.setItem(53, DefinedItemStackBuilders.rightArrow().displayName(Component.text("次のページ", TextColor.color(5635925))).build());
        } else if (this.splitHomes.size() == this.currentPage) {
            this.inventory.setItem(53, null);
        }

        // BACK BUTTON
        if (this.splitHomes.size() > 1 && this.currentPage > 1) {
            this.inventory.setItem(52, DefinedItemStackBuilders.leftArrow().displayName(Component.text("前のページ", TextColor.color(5635925))).build());
        } else if (this.currentPage <= 1) {
            this.inventory.setItem(52, null);
        }
    }

    private enum State {
        CATEGORIES,
        ADD_CATEGORY,
        WAITING_CALLBACK,
        HOMES,
        ADD_HOME,
    }
}
