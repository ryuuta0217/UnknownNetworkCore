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

import com.ryuuta0217.util.ListUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.md_5.bungee.api.ChatColor;
import net.unknown.core.builder.ItemStackBuilder;
import net.unknown.core.define.DefinedItemStackBuilders;
import net.unknown.core.gui.GuiBase;
import net.unknown.core.util.MessageUtil;
import net.unknown.survival.data.Home;
import net.unknown.survival.data.PlayerData;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.IntStream;

public class HomeGui extends GuiBase {
    private State guiState;
    private String selectedCategory = null;
    private final Player target;
    private final PlayerData data;
    private List<Set<String>> splitCategories;
    private Map<String, List<Set<Home>>> splitHomes = new HashMap<>();
    private int currentPage = 1;
    private final Map<Integer, String> slot2CategoryMap = new HashMap<>();
    private final Map<Integer, Home> slot2HomeMap = new HashMap<>();

    public HomeGui(Player player) {
        super(player, 54, Component.text("ホーム"), true);
        this.guiState = State.CATEGORIES;

        this.target = player;
        this.data = PlayerData.of(this.target);

        this.loadData();

        this.setCategories(this.currentPage);

        this.inventory.setItem(45, DefinedItemStackBuilders.leftArrow().displayName(Component.text("戻る", TextColor.color(5635925))).build());

        if(this.splitCategories.size() > 1) {
            this.inventory.setItem(53, DefinedItemStackBuilders.rightArrow().displayName(Component.text("次のページ", TextColor.color(5635925))).build());
        }
    }

    @Override
    public void onClick(InventoryClickEvent event) {
        if(event.getClick() == ClickType.LEFT) {
            if(this.guiState == State.CATEGORIES && this.slot2CategoryMap.containsKey(event.getSlot())) {
                this.selectedCategory = this.slot2CategoryMap.get(event.getSlot());
                this.clearCategories();
                this.guiState = State.HOMES;
                this.currentPage = 1;
                this.setHomes(this.currentPage);
                return;
            } else if(this.guiState == State.HOMES && this.slot2HomeMap.containsKey(event.getSlot())) {
                Home home = this.slot2HomeMap.get(event.getSlot());
                home.teleportPlayer((Player) event.getWhoClicked());
                event.getWhoClicked().closeInventory();
                MessageUtil.sendMessage((Player) event.getWhoClicked(), "ホーム " + home.name() + " にテレポートしました (GUI)");
                return;
            }
        } else if (event.getClick() == ClickType.RIGHT) {
            if(this.guiState == State.CATEGORIES && this.slot2CategoryMap.containsKey(event.getSlot()) && event.getCurrentItem() != null) {
                PlayerData.of(event.getWhoClicked().getUniqueId()).setCategoryMaterial(this.slot2CategoryMap.get(event.getSlot()), event.getCurrentItem().getType());
                this.loadData();
                this.clearCategories();
                this.setCategories(this.currentPage);
            }
        } else if (event.getClick() == ClickType.SHIFT_RIGHT) {
            if(this.guiState == State.HOMES && this.slot2HomeMap.containsKey(event.getSlot())) {
                PlayerData.of(event.getWhoClicked().getUniqueId()).removeHome(this.selectedCategory, this.slot2HomeMap.get(event.getSlot()).name());
                this.loadData();
                this.clearHomes();
                this.setHomes(this.currentPage);
            }
        }

        switch (event.getSlot()) {
            case 45:
                if(this.selectedCategory == null) {
                    event.getWhoClicked().openInventory(MainGui.getGui().getInventory());
                } else {
                    this.selectedCategory = null;
                    this.currentPage = 1;
                    this.clearHomes();
                    this.loadData();
                    this.setCategories(this.currentPage);
                }
                break;
            case 52:
                if(this.currentPage > 1) {
                    if(this.guiState == State.CATEGORIES && this.splitCategories.size() > 1) {
                        this.currentPage--;
                        if(this.currentPage <= this.splitCategories.size()) {
                            this.inventory.setItem(53, DefinedItemStackBuilders.rightArrow().displayName(Component.text("次のページ", TextColor.color(5635925))).build());
                        }
                        if(this.currentPage == 1) this.inventory.setItem(52, null);
                        this.clearCategories();
                        this.setCategories(this.currentPage);
                    } else if(this.guiState == State.HOMES && this.splitHomes.get(this.selectedCategory).size() > 1) {
                        this.currentPage--;
                        if (this.currentPage <= this.splitHomes.get(this.selectedCategory).size())
                            this.inventory.setItem(53, DefinedItemStackBuilders.rightArrow().displayName(Component.text("次のページ", TextColor.color(5635925))).build());
                        if (this.currentPage == 1) this.inventory.setItem(52, null);
                        this.clearHomes();
                        this.setHomes(this.currentPage);
                    }
                }
                break;
            case 53:
                if(this.guiState == State.CATEGORIES && this.splitCategories.size() > 1 && this.currentPage <= this.splitCategories.size()) {
                    this.currentPage++;
                    this.inventory.setItem(52, DefinedItemStackBuilders.leftArrow().displayName(Component.text("前のページ", TextColor.color(16777045))).build());
                    if(this.currentPage == this.splitCategories.size()) this.inventory.setItem(53, null);
                    this.clearCategories();
                    this.setCategories(this.currentPage);
                } else if(this.guiState == State.HOMES && this.splitHomes.get(this.selectedCategory).size() > 1 && this.currentPage <= this.splitHomes.get(this.selectedCategory).size()) {
                    this.currentPage++;
                    this.inventory.setItem(52, DefinedItemStackBuilders.leftArrow().displayName(Component.text("前のページ", TextColor.color(16777045))).build());
                    if (this.currentPage == this.splitHomes.get(this.selectedCategory).size()) this.inventory.setItem(53, null);
                    this.clearHomes();
                    this.setHomes(this.currentPage);
                }
                break;
            default:
                break;
        }
    }

    private void loadData() {
        this.splitCategories = null;
        this.splitHomes = new HashMap<>();

        this.splitCategories = ListUtil.splitListAsSet(data.getCategories(), 45);
        this.splitCategories.forEach(categories -> categories.forEach(category -> {
            this.splitHomes.put(category, ListUtil.splitListAsSet(data.getHomes(category).values(), 45));
        }));
    }

    private void clearCategories() {
        this.slot2CategoryMap.clear();
        IntStream.rangeClosed(0, 44).forEach(this.inventory::clear);
    }

    private void clearHomes() {
        this.slot2HomeMap.clear();
        IntStream.rangeClosed(0, 44).forEach(this.inventory::clear);
    }

    private void setCategories(int page) {
        this.splitCategories.get(page - 1).forEach(category -> {
            this.slot2CategoryMap.put(this.inventory.firstEmpty(), category);
            this.inventory.setItem(this.inventory.firstEmpty(), new ItemStackBuilder(data.getCategoryMaterial(category))
                            .displayName(Component.text(category, TextColor.color(5635925)))
                            .lore(Component.text("登録ホーム数: " + data.getHomes(category).size()))
                    .build());
        });

        if(this.splitCategories.size() == this.currentPage) {
            this.inventory.setItem(49, DefinedItemStackBuilders.plus()
                    .displayName(Component.text("新規カテゴリ作成", TextColor.color(5635925)))
                    .build());
        }
    }

    private void setHomes(int page) {
        this.splitHomes.get(this.selectedCategory).get(page - 1).forEach(home -> {
            this.slot2HomeMap.put(this.inventory.firstEmpty(), home);
            this.inventory.setItem(this.inventory.firstEmpty(), new ItemStackBuilder(dimension2Material(home.location()))
                    .displayName(Component.text(home.name(), dimension2TextColor(home.location())))
                    .lore(Component.text("ワールド: " + MessageUtil.getWorldNameDisplay(home.getWorld()), TextColor.color(5635925)).decoration(TextDecoration.ITALIC, false),
                            Component.text("座標: " + getCoordinateAsString(home.location()), TextColor.color(5635925)).decoration(TextDecoration.ITALIC, false),
                            Component.text("向き: " + getRotationAsString(home.location()), TextColor.color(5635925)).decoration(TextDecoration.ITALIC, false),
                            Component.text(""),
                            Component.text("クリックでテレポート", TextColor.color(5635925)),
                            Component.text("Shiftキーを押しながら右クリックで削除", TextColor.color(255, 0, 0)))
                    .build());
        });
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

        return yaw.substring(0, Math.min(yaw.indexOf(".") + 2, yaw.length())) + ", " + pitch.substring(0, Math.min(pitch.indexOf("." + 2), pitch.length()));
    }

    private enum State {
        CATEGORIES,
        TO_REMOVE_CATEGORY,
        HOMES,
        TO_REMOVE_HOME
    }
}
