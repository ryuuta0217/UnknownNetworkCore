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

import com.ryuuta0217.util.ListUtil;
import com.sk89q.worldedit.math.BlockVector3;
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
import net.unknown.survival.dependency.WorldGuard;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.ItemFlag;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.IntStream;

// TODO RegionAreaが他人と被っているときの処理
// TODO FlagEditor
// TODO
public class ProtectionGui extends GuiBase {
    private final Player player;
    private State guiState;
    private ViewBase view;

    public ProtectionGui(Player owner) {
        super(owner,
                9 * 6,
                Component.text("保護", TextColor.color(0xFF00)),
                false);
        this.player = owner;

        this.guiState = State.REGIONS;
        this.view = new RegionsView(this);

        this.inventory.setItem(45, DefinedItemStackBuilders.leftArrow()
                .displayName(Component.text("戻る", TextColor.color(5635925)))
                .build());
    }

    private static Component coordinates2Str(BlockVector3 vec3) {
        return Component.text(vec3.getX() + "," + vec3.getY() + "," + vec3.getZ(), TextColor.color(0xFFFF));
    }

    @Override
    public void onClick(InventoryClickEvent event) {
        if (event.getSlot() == 45 && this.guiState == State.REGIONS) {
            event.getWhoClicked().openInventory(MainGui.getGui().getInventory());
        } else {
            this.view.onClick(event);
        }
    }

    @Override
    public void onClose(InventoryCloseEvent event) {
        if (this.guiState != State.WAITING_CALLBACK) this.unRegisterAsListener();
    }

    //
    private enum State {
        WORLD_SELECTION,
        REGIONS,
        REGION_INFORMATION,
        MANAGE_FLAGS,
        MANAGE_MEMBERS,
        NEW_REGION,
        WAITING_CALLBACK,
        WAITING_REGION_REMOVE
    }

    private static class RegionsView extends ViewBase {
        private final Map<Integer, WorldGuard.WrappedProtectedRegion> slot2region = new HashMap<>();
        private List<Set<WorldGuard.WrappedProtectedRegion>> regions;
        private int currentPage = 1;

        public RegionsView(ProtectionGui gui) {
            super(gui);
            this.loadRegions();
        }

        public void loadRegions() {
            this.regions = ListUtil.splitListAsLinkedSet(WorldGuard.getProtectedRegions(this.gui.player), 45);
            this.showRegions(1);
        }

        public void showRegions(int newPage) {
            this.currentPage = newPage;
            this.clearRegions();
            this.regions.get(this.currentPage - 1).forEach(region -> {
                int toAddSlot = this.gui.inventory.firstEmpty();
                this.gui.inventory.setItem(toAddSlot, new ItemStackBuilder(Material.LIME_WOOL)
                        .displayName(Component.text(region.region().getId()))
                        .lore(Component.text("", Style.style(TextColor.color(0xFFFFFF), TextDecoration.ITALIC.as(false)))
                                .append(coordinates2Str(region.region().getMinimumPoint()))
                                .append(Component.text(" -> ", TextColor.color(0xFF00))).
                                append(coordinates2Str(region.region().getMaximumPoint())))
                        .build());
                this.slot2region.put(toAddSlot, region);
            });

            if (this.regions.size() == this.currentPage) {
                this.gui.inventory.setItem(49, DefinedItemStackBuilders.plus()
                        .displayName(Component.text("新規保護", DefinedTextColor.GREEN))
                        .build());
            }

            if (this.regions.size() > 1 && this.currentPage < this.regions.size()) {
                this.gui.inventory.setItem(53, DefinedItemStackBuilders.rightArrow()
                        .displayName(Component.text("次のページ", TextColor.color(5635925)))
                        .build());
            } else {
                this.gui.inventory.clear(53);
            }

            if (this.currentPage > 1) {
                this.gui.inventory.setItem(52, DefinedItemStackBuilders.leftArrow()
                        .displayName(Component.text("前のページ", TextColor.color(5635925)))
                        .build());
            } else {
                this.gui.inventory.clear(52);
            }
        }

        private void clearRegions() {
            super.clearInventory();
            this.slot2region.clear();
            this.gui.inventory.clear(49);
        }

        @Override
        public void onClick(InventoryClickEvent event) {
            if (slot2region.containsKey(event.getSlot())) {
                WorldGuard.WrappedProtectedRegion selectedRegion = slot2region.get(event.getSlot());
                this.clearRegions();
                this.gui.inventory.clear(52);
                this.gui.inventory.clear(53);

                this.gui.guiState = State.REGION_INFORMATION;
                this.gui.view = new RegionInfoView(this, selectedRegion);
            } else if (event.getSlot() == 49 && this.regions.size() == this.currentPage) {
                this.gui.guiState = State.WAITING_CALLBACK;
                this.gui.player.closeInventory(InventoryCloseEvent.Reason.PLUGIN);
                new SignGui().withTarget((Player) event.getWhoClicked())
                        .withLines(Component.empty(),
                                Component.text("^^^^^^^^^^^^^^"),
                                Component.text("保護領域名を入力"),
                                Component.empty())
                        .onComplete(lines -> {
                            String newRegionName = PlainTextComponentSerializer.plainText().serialize(lines.get(0));
                            this.gui.player.openInventory(this.gui.getInventory());
                            this.clearRegions();
                            this.gui.inventory.clear(52);
                            this.gui.inventory.clear(53);
                            this.gui.guiState = State.NEW_REGION;
                            this.gui.view = new NewRegionView(this, newRegionName);
                        }).open();
            } else if (event.getSlot() == 52 && this.currentPage > 1) {
                this.showRegions(this.currentPage - 1);
            } else if (event.getSlot() == 53 && this.currentPage < this.regions.size()) {
                this.showRegions(this.currentPage + 1);
            }
        }

        private static class NewRegionView extends ViewBase {
            private final RegionsView regionsView;
            private final String newRegionName;
            private BlockVector3 min;
            private BlockVector3 max;

            public NewRegionView(RegionsView regionsView, String newRegionName) {
                super(regionsView.gui);
                this.regionsView = regionsView;
                this.newRegionName = newRegionName;
            }

            @Override
            public void onClick(InventoryClickEvent event) {
                if (event.getSlot() == 45) {
                    this.gui.guiState = State.REGIONS;
                    this.gui.view = this.regionsView;
                    this.clearInventory();
                    this.regionsView.loadRegions();
                    this.regionsView.showRegions(1);
                }
            }
        }

        private static class RegionInfoView extends ViewBase {
            private final RegionsView regionsView;
            private final WorldGuard.WrappedProtectedRegion region;

            public RegionInfoView(RegionsView regionsView, WorldGuard.WrappedProtectedRegion region) {
                super(regionsView.gui);
                this.regionsView = regionsView;
                this.region = region;
                this.showInformations();
            }

            private void showInformations() {
                this.gui.inventory.setItem(13, new ItemStackBuilder(Material.PAPER)
                        .displayName(Component.text("保護領域の情報", Style.style(DefinedTextColor.GOLD, TextDecoration.ITALIC.as(false))))
                        .lore(Component.text("ワールド: " + MessageUtil.getWorldName(this.region.world().getName()), Style.style(DefinedTextColor.AQUA, TextDecoration.ITALIC.as(false))),
                                Component.text("名前: " + this.region.region().getId(), Style.style(DefinedTextColor.AQUA, TextDecoration.ITALIC.as(false))),
                                Component.text("範囲: ", Style.style(DefinedTextColor.AQUA, TextDecoration.ITALIC.as(false)))
                                        .append(coordinates2Str(this.region.region().getMinimumPoint()))
                                        .append(Component.text(" -> "))
                                        .append(coordinates2Str(this.region.region().getMaximumPoint())))
                        .build());

                this.gui.inventory.setItem(29, new ItemStackBuilder(Material.NAME_TAG)
                        .displayName(Component.text("保護領域名の変更", Style.style(DefinedTextColor.GREEN, TextDecoration.ITALIC.as(false))))
                        .build());

                this.gui.inventory.setItem(31, new ItemStackBuilder(Material.SHIELD)
                        .displayName(Component.text("フラグの設定", Style.style(DefinedTextColor.YELLOW, TextDecoration.ITALIC.as(false))))
                        .build());

                this.gui.inventory.setItem(33, new ItemStackBuilder(Material.DIAMOND_CHESTPLATE)
                        .displayName(Component.text("メンバーの管理", Style.style(DefinedTextColor.LIGHT_PURPLE, TextDecoration.ITALIC.as(false))))
                        .addItemFlag(ItemFlag.HIDE_ATTRIBUTES)
                        .build());

                this.gui.inventory.setItem(53, new ItemStackBuilder(Material.LAVA_BUCKET)
                        .displayName(Component.text("保護領域の削除", Style.style(DefinedTextColor.RED, TextDecoration.ITALIC.as(false), TextDecoration.BOLD.as(true))))
                        .build());
            }

            private void clearInformation() {
                super.clearInventory();
                this.gui.inventory.clear(53);
            }

            @Override
            public void onClick(InventoryClickEvent event) {
                if (event.getSlot() == 45) {
                    this.clearInformation();
                    this.gui.guiState = State.REGIONS;
                    this.gui.view = this.regionsView;
                    this.regionsView.showRegions(this.regionsView.currentPage);
                } else if (event.getSlot() == 53) {
                    this.clearInformation();
                    this.gui.guiState = State.WAITING_REGION_REMOVE;
                    this.gui.view = new RegionRemoveConfirmView(this);
                }
            }

            private static class RegionRemoveConfirmView extends ViewBase {
                private final RegionInfoView regionInfoView;

                public RegionRemoveConfirmView(RegionInfoView regionInfoView) {
                    super(regionInfoView.gui);
                    this.regionInfoView = regionInfoView;

                    this.gui.inventory.setItem(13, new ItemStackBuilder(Material.PAPER)
                            .displayName(Component.text("本当に保護領域を削除しますか？", Style.style(DefinedTextColor.YELLOW, TextDecoration.ITALIC.as(false), TextDecoration.BOLD.as(true))))
                            .lore(Component.text("ワールド: " + this.regionInfoView.region.world().getName()),
                                    Component.text("名前: " + this.regionInfoView.region.region().getId()),
                                    Component.text("範囲: ", Style.style(DefinedTextColor.AQUA, TextDecoration.ITALIC.as(false)))
                                            .append(coordinates2Str(this.regionInfoView.region.region().getMinimumPoint()))
                                            .append(Component.text(" -> "))
                                            .append(coordinates2Str(this.regionInfoView.region.region().getMaximumPoint())))
                            .build());

                    this.gui.inventory.setItem(30, new ItemStackBuilder(Material.LIME_WOOL)
                            .displayName(Component.text("はい", Style.style(DefinedTextColor.GREEN, TextDecoration.ITALIC.as(false), TextDecoration.BOLD.as(true))))
                            .build());

                    this.gui.inventory.setItem(32, new ItemStackBuilder(Material.RED_WOOL)
                            .displayName(Component.text("いいえ", Style.style(DefinedTextColor.RED, TextDecoration.ITALIC.as(false), TextDecoration.BOLD.as(true))))
                            .build());
                }

                @Override
                public void onClick(InventoryClickEvent event) {
                    if (event.getSlot() == 45) {
                        super.clearInventory();
                        this.gui.guiState = State.REGION_INFORMATION;
                        this.gui.view = this.regionInfoView;
                        this.regionInfoView.showInformations();
                    }
                }
            }
        }
    }

    private static abstract class ViewBase {
        protected final ProtectionGui gui;

        public ViewBase(ProtectionGui gui) {
            this.gui = gui;
        }

        public abstract void onClick(InventoryClickEvent event);

        protected void clearInventory() {
            IntStream.rangeClosed(0, 44).forEach(this.gui.inventory::clear);
        }
    }
}
