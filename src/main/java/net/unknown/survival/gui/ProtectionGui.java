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
import com.sk89q.worldedit.LocalSession;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.regions.RegionSelector;
import com.sk89q.worldedit.regions.selector.CuboidRegionSelector;
import com.sk89q.worldguard.domains.DefaultDomain;
import com.sk89q.worldguard.domains.GroupDomain;
import com.sk89q.worldguard.domains.PlayerDomain;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedCuboidRegion;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import net.unknown.UnknownNetworkCore;
import net.unknown.core.builder.ItemStackBuilder;
import net.unknown.core.define.DefinedItemStackBuilders;
import net.unknown.core.define.DefinedTextColor;
import net.unknown.core.gui.GuiBase;
import net.unknown.core.gui.SignGui;
import net.unknown.core.gui.view.View;
import net.unknown.core.managers.ListenerManager;
import net.unknown.core.managers.RunnableManager;
import net.unknown.core.util.MessageUtil;
import net.unknown.core.util.NewMessageUtil;
import net.unknown.survival.dependency.WorldEdit;
import net.unknown.survival.dependency.WorldGuard;
import net.unknown.core.gui.view.PlayerSelectionView;
import org.bukkit.*;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.IntStream;

// TODO RegionAreaが他人と被っているときの処理
// TODO 対象ワールドの処理 (範囲選択時にprivateフィールドにWorldを保持しておく？)
// TODO FlagEditor
// TODO
public class ProtectionGui extends GuiBase {
    private static final Map<UUID, ProtectionGui> TEMP_DATA = new HashMap<>();

    private final Player player;
    private State guiState;
    private View view;

    private ProtectionGui(Player owner) {
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

    public static ProtectionGui of(Player owner) {
        return TEMP_DATA.getOrDefault(owner.getUniqueId(), new ProtectionGui(owner));
    }

    private static Component coordinates2Str(BlockVector3 vec3) {
        return Component.translatable("chat.coordinates", TextColor.color(0xFFFF))
                .args(Component.text(vec3.getX()), Component.text(vec3.getY()), Component.text(vec3.getZ()));
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
        if (this.guiState != State.WAITING_CALLBACK && this.guiState != State.NEW_REGION) {
            TEMP_DATA.remove(this.player.getUniqueId());
            this.unRegisterAsListener();
        }
        if (this.guiState == State.NEW_REGION) {
            TEMP_DATA.put(this.player.getUniqueId(), this);
        }
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
        WAITING_REGION_REMOVE,
        ERROR
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
            this.reloadRegionsOnly();
            this.showRegions(1);
        }

        public void reloadRegionsOnly() {
            this.regions = ListUtil.splitListAsLinkedSet(WorldGuard.getProtectedRegions(this.gui.player), 45);
        }

        public void showRegions(int newPage) {
            this.currentPage = newPage;
            this.clearInventory();
            this.regions.get(this.currentPage - 1).forEach(region -> {
                int toAddSlot = this.gui.inventory.firstEmpty();
                this.gui.inventory.setItem(toAddSlot, new ItemStackBuilder(Material.LIME_WOOL)
                        .displayName(Component.text(region.getId()))
                        .lore(Component.text("ワールド: " + MessageUtil.getWorldNameDisplay(region.world()), DefinedTextColor.GREEN),
                                Component.empty()
                                        .append(coordinates2Str(region.region().getMinimumPoint()))
                                        .append(Component.text(" -> ", DefinedTextColor.GREEN))
                                        .append(coordinates2Str(region.region().getMaximumPoint())),
                                region.getCreatorUniqueId() != null ? Component.text("作成者: " + Bukkit.getOfflinePlayer(region.getCreatorUniqueId()).getName(), DefinedTextColor.GOLD) : Component.empty())
                        .build());
                this.slot2region.put(toAddSlot, region);
            });

            this.gui.inventory.setItem(49, DefinedItemStackBuilders.plus()
                    .displayName(Component.text("新規保護", DefinedTextColor.GREEN))
                    .build());

            this.gui.inventory.setItem(50, new ItemStackBuilder(Material.ENDER_CHEST)
                    .displayName(Component.text("グループ管理", DefinedTextColor.GOLD))
                    .lore(Component.text("グループの追加、編集、削除が行えます。", DefinedTextColor.YELLOW),
                            Component.text("グループメンバーの追加、削除が行えます。", DefinedTextColor.YELLOW))
                    .build());

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

        @Override
        public void clearInventory() {
            super.clearInventory();
            this.slot2region.clear();
            this.gui.inventory.clear(49);
            this.gui.inventory.clear(50);
        }

        @Override
        public void initialize() {
            this.loadRegions();
        }

        @Override
        public void onClick(InventoryClickEvent event) {
            if (slot2region.containsKey(event.getSlot())) {
                WorldGuard.WrappedProtectedRegion selectedRegion = slot2region.get(event.getSlot());
                this.clearInventory();
                this.gui.inventory.clear(52);
                this.gui.inventory.clear(53);

                this.gui.guiState = State.REGION_INFORMATION;
                this.gui.view = new RegionInfoView(this, selectedRegion);
            } else if (event.getSlot() == 49) {
                this.gui.guiState = State.WAITING_CALLBACK;
                this.gui.player.closeInventory(InventoryCloseEvent.Reason.PLUGIN);
                Util.openSetNameWindow(null, this, State.REGIONS, (name) -> {
                    this.gui.inventory.clear(52);
                    this.gui.inventory.clear(53);

                    this.gui.guiState = State.NEW_REGION;
                    this.gui.view = new NewRegionView(this, name);
                });
            } else if (event.getSlot() == 50) {
                // Group Management
            } else if (event.getSlot() == 52 && this.currentPage > 1) {
                this.showRegions(this.currentPage - 1);
            } else if (event.getSlot() == 53 && this.currentPage < this.regions.size()) {
                this.showRegions(this.currentPage + 1);
            }
        }

        private static class NewRegionView extends ViewBase {
            private final RegionsView regionsView;
            private String newRegionName;
            private World world;
            private BlockVector3 min;
            private BlockVector3 max;

            public NewRegionView(RegionsView regionsView, String newRegionName) {
                super(regionsView.gui);
                this.regionsView = regionsView;
                this.newRegionName = newRegionName;
                this.initialize();
            }

            @Override
            public void initialize() {
                this.gui.inventory.setItem(13, new ItemStackBuilder(Material.PAPER)
                        .displayName(Component.text("新規保護領域の作成",
                                Style.style(DefinedTextColor.GREEN,
                                        TextDecoration.BOLD.withState(true))))
                        .lore(Component.text("名前: " + this.newRegionName,
                                        Style.style(DefinedTextColor.AQUA,
                                                TextDecoration.ITALIC.withState(false))),
                                Component.text("範囲: ", Style.style(DefinedTextColor.YELLOW,
                                                TextDecoration.ITALIC.withState(false)))
                                        .append((this.min == null && this.max == null ? Component.text("未設定") : (this.min != null ? (coordinates2Str(this.min).append(this.max != null ? Component.text(" -> ").append(coordinates2Str(this.max)) : Component.text(" -> [未設定]"))) : Component.text("[未設定]")))))
                        .build());

                this.gui.inventory.setItem(30, new ItemStackBuilder(Material.NAME_TAG)
                        .displayName(Component.text("保護領域名", DefinedTextColor.GOLD))
                        .lore(Component.text("現在の設定値: " + this.newRegionName,
                                        Style.style(DefinedTextColor.AQUA,
                                                TextDecoration.ITALIC.withState(false))),
                                Component.text(""),
                                Component.text("クリックして名前を変更",
                                        Style.style(DefinedTextColor.YELLOW,
                                                TextDecoration.ITALIC.withState(false))))
                        .build());
                this.gui.inventory.setItem(32, new ItemStackBuilder(Material.OAK_FENCE)
                        .displayName(Component.text("保護範囲", DefinedTextColor.YELLOW))
                        .lore(Component.text("地点#1: ", Style.style(DefinedTextColor.AQUA, TextDecoration.ITALIC.withState(false)))
                                        .append(this.min == null ? Component.text("未設定") : coordinates2Str(this.min)),
                                Component.text("地点#2: ", Style.style(DefinedTextColor.AQUA, TextDecoration.ITALIC.withState(false)))
                                        .append(this.max == null ? Component.text("未設定") : coordinates2Str(this.max)),
                                Component.text(""),
                                Component.text("クリックして範囲設定を開始",
                                        Style.style(DefinedTextColor.YELLOW,
                                                TextDecoration.ITALIC.withState(false))))
                        .build());

                if (this.newRegionName != null && this.min != null && this.max != null) {
                    this.gui.inventory.setItem(49, new ItemStackBuilder(Material.LIME_WOOL)
                            .displayName(Component.text("保護を確定", Style.style(
                                    DefinedTextColor.GREEN,
                                    TextDecoration.BOLD.withState(true),
                                    TextDecoration.UNDERLINED.withState(true))))
                            .lore(Component.text("名前: " + this.newRegionName,
                                            Style.style(DefinedTextColor.AQUA,
                                                    TextDecoration.ITALIC.withState(false))),
                                    Component.text("範囲: ", Style.style(DefinedTextColor.YELLOW,
                                                    TextDecoration.ITALIC.withState(false)))
                                            .append(coordinates2Str(this.min))
                                            .append(Component.text(" -> "))
                                            .append(coordinates2Str(this.max)))
                            .build());
                }
            }

            @Override
            public void clearInventory() {
                super.clearInventory();
                this.gui.inventory.setItem(49, null);
            }

            @Override
            public void onClick(InventoryClickEvent event) {
                if (event.getSlot() == 30) {
                    this.gui.guiState = State.WAITING_CALLBACK;
                    this.gui.player.closeInventory(InventoryCloseEvent.Reason.PLUGIN);
                    Util.openSetNameWindow(this.newRegionName, this, State.NEW_REGION, name -> {
                        this.newRegionName = name;
                        this.gui.guiState = State.NEW_REGION;
                        this.initialize();
                    });
                } else if (event.getSlot() == 32) {
                    this.gui.guiState = State.WAITING_CALLBACK;
                    this.gui.player.closeInventory(InventoryCloseEvent.Reason.PLUGIN);
                    Util.startSelectionMode(this.gui.player, null, null, null, (result) -> {
                        this.world = result.world();
                        this.min = result.min();
                        this.max = result.max();

                        WorldEdit.getRegionSelector(this.gui.player, result.world()).clear(); // Clear selection to remove visualizer

                        this.gui.guiState = State.NEW_REGION;
                        this.initialize();
                        this.gui.player.openInventory(this.gui.inventory);
                    });
                } else if (event.getSlot() == 45) {
                    /* 保護領域一覧に戻る */
                    this.gui.guiState = State.REGIONS;
                    this.gui.view = this.regionsView;
                    this.clearInventory();
                    this.regionsView.loadRegions();
                    this.regionsView.showRegions(1);
                } else if (event.getSlot() == 49 && event.getCurrentItem() != null) {
                    /* 保護領域作成確定 */
                    if (this.newRegionName != null && this.min != null && this.max != null) {
                        ProtectedCuboidRegion region = new ProtectedCuboidRegion(this.gui.player.getUniqueId() + WorldGuard.SPLITTER + this.newRegionName, false, this.min, this.max);
                        region.getOwners().addPlayer(this.gui.player.getUniqueId());

                        RegionManager manager = WorldGuard.getPlatform().getRegionContainer().get(BukkitAdapter.adapt(this.world));
                        WorldEdit.getRegionSelector(this.gui.player, this.world).clear();

                        // 保護領域重複チェック
                        List<ProtectedRegion> intersectedRegions = region.getIntersectingRegions(manager.getRegions().values());
                        if (intersectedRegions.size() > 0) {
                            Util.showRegionAreaIntersectedError(intersectedRegions, this, State.NEW_REGION);
                            this.min = null;
                            this.max = null;
                            WorldEdit.getRegionSelector(this.gui.player, this.world).clear();
                        } else {
                            manager.addRegion(region);
                            NewMessageUtil.sendMessage(this.gui.player, Component.text("ワールド " + MessageUtil.getWorldName(this.world) + " に新しい保護領域 " + this.newRegionName + " を作成しました！"));
                            this.clearInventory();
                            this.gui.guiState = State.REGION_INFORMATION;
                            this.gui.view = new RegionInfoView(this.regionsView, new WorldGuard.WrappedProtectedRegion(this.world, region));
                        }
                    }
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
                this.showInformation();
            }

            private void showInformation() {
                this.gui.inventory.setItem(13, new ItemStackBuilder(Material.PAPER)
                        .displayName(Component.text("保護領域の情報", Style.style(DefinedTextColor.GOLD, TextDecoration.ITALIC.withState(false))))
                        .lore(Component.text("ワールド: " + MessageUtil.getWorldName(this.region.world().getName()), Style.style(DefinedTextColor.AQUA, TextDecoration.ITALIC.withState(false))),
                                Component.text("名前: " + this.region.getId(), Style.style(DefinedTextColor.AQUA, TextDecoration.ITALIC.withState(false))),
                                Component.text("範囲: ", Style.style(DefinedTextColor.AQUA, TextDecoration.ITALIC.withState(false)))
                                        .append(coordinates2Str(this.region.region().getMinimumPoint()))
                                        .append(Component.text(" -> "))
                                        .append(coordinates2Str(this.region.region().getMaximumPoint())))
                        .build());

                this.gui.inventory.setItem(29, new ItemStackBuilder(Material.NAME_TAG)
                        .displayName(Component.text("保護領域名の変更", Style.style(DefinedTextColor.GREEN, TextDecoration.ITALIC.withState(false))))
                        .lore(Component.text("現在の名前: " + this.region.getId(), DefinedTextColor.AQUA))
                        .build());

                this.gui.inventory.setItem(31, new ItemStackBuilder(Material.SHIELD)
                        .displayName(Component.text("フラグの設定", Style.style(DefinedTextColor.YELLOW, TextDecoration.ITALIC.withState(false))))
                        .build());

                this.gui.inventory.setItem(33, new ItemStackBuilder(Material.DIAMOND_CHESTPLATE)
                        .displayName(Component.text("メンバーの管理", Style.style(DefinedTextColor.LIGHT_PURPLE, TextDecoration.ITALIC.withState(false))))
                        .lore(new ArrayList<Component>() {{
                            DefaultDomain owners = region.region().getOwners();
                            PlayerDomain ownerPlayers = owners.getPlayerDomain();
                            GroupDomain ownerGroups = owners.getGroupDomain();
                            DefaultDomain members = region.region().getMembers();
                            PlayerDomain memberPlayers = members.getPlayerDomain();
                            GroupDomain memberGroups = members.getGroupDomain();

                            add(Component.text("オーナー", DefinedTextColor.AQUA));
                            ownerPlayers.getUniqueIds().forEach(uuid -> Util.appendPlayerComponent(this, uuid));
                            ownerGroups.getGroups().forEach(group -> Util.appendGroupComponent(this, group));

                            if (members.size() > 0) {
                                add(Component.empty());

                                add(Component.text("メンバー", DefinedTextColor.GREEN));
                                memberPlayers.getUniqueIds().forEach(uuid -> Util.appendPlayerComponent(this, uuid));
                                memberGroups.getGroups().forEach(group -> Util.appendGroupComponent(this, group));
                            }
                        }}.toArray(new Component[0]))
                        .addItemFlag(ItemFlag.HIDE_ATTRIBUTES)
                        .build());

                this.gui.inventory.setItem(51, new ItemStackBuilder(Material.SPYGLASS)
                        .displayName(Component.text("保護領域の範囲を表示", Style.style(DefinedTextColor.AQUA, TextDecoration.BOLD)))
                        .build());

                this.gui.inventory.setItem(52, new ItemStackBuilder(Material.OAK_FENCE)
                        .displayName(Component.text("保護範囲を変更", Style.style(DefinedTextColor.GREEN, TextDecoration.BOLD)))
                        .build());

                this.gui.inventory.setItem(53, new ItemStackBuilder(Material.LAVA_BUCKET)
                        .displayName(Component.text("保護領域の削除", Style.style(DefinedTextColor.RED, TextDecoration.BOLD)))
                        .build());
            }

            @Override
            public void clearInventory() {
                super.clearInventory();
                this.gui.inventory.clear(51);
                this.gui.inventory.clear(52);
                this.gui.inventory.clear(53);
            }

            @Override
            public void initialize() {
                this.showInformation();
            }

            @Override
            public void onClick(InventoryClickEvent event) {
                switch (event.getSlot()) {
                    // 名前変更
                    case 29 -> {
                        if (this.region.region() instanceof ProtectedCuboidRegion) {
                            this.gui.guiState = State.WAITING_CALLBACK;
                            this.gui.player.closeInventory(InventoryCloseEvent.Reason.PLUGIN);
                            String oldFullId = this.region.getFullId();
                            String oldId = this.region.getId();
                            Util.openSetNameWindow(this.region.getId(), this, State.REGION_INFORMATION, (newId) -> {
                                String newFullId = this.gui.player.getUniqueId() + WorldGuard.SPLITTER + newId;
                                if (newFullId.equals(oldFullId)) {
                                    this.gui.guiState = State.REGION_INFORMATION;
                                    this.initialize();
                                } else {
                                    if (!this.region.regionManager().hasRegion(newFullId)) {
                                        ProtectedRegion renamed = new ProtectedCuboidRegion(newFullId,
                                                false,
                                                this.region.region().getMinimumPoint(),
                                                this.region.region().getMaximumPoint());

                                        renamed.copyFrom(this.region.region()); // Inherit data from Old Region
                                        this.region.regionManager().removeRegion(oldFullId); // Remove Old Region
                                        this.region.regionManager().addRegion(renamed); // Create New Renamed Region
                                        this.gui.guiState = State.REGION_INFORMATION;
                                        NewMessageUtil.sendMessage(this.gui.player, Component.text("保護領域の名前を " + oldId + " から " + newId + " に変更しました"));
                                        this.gui.view = new RegionInfoView(this.regionsView,
                                                new WorldGuard.WrappedProtectedRegion(this.region.world(), renamed));
                                    } else {
                                        this.gui.guiState = State.ERROR;
                                        this.gui.view = new ErrorView(Component.text("保護領域名 " + newId + " は既に使用されています。", DefinedTextColor.RED),
                                                Arrays.asList(Component.text("ほかの保護領域名を検討してください。", DefinedTextColor.YELLOW),
                                                        Component.text("ワールド: " + this.region.world().getName())), this.gui, this.gui.view, State.REGION_INFORMATION, null);
                                    }
                                }
                            });
                        } else {
                            Util.showUnsupportedRegionError(this, State.REGION_INFORMATION);
                        }
                    }

                    // フラグ管理
                    case 31 -> {

                    }

                    // メンバー管理
                    case 33 -> {
                        this.clearInventory();
                        this.gui.guiState = State.MANAGE_MEMBERS;
                        this.gui.view = new RegionMemberManagementView(this);
                    }

                    // 戻る
                    case 45 -> {
                        this.clearInventory();
                        this.gui.guiState = State.REGIONS;
                        this.gui.view = this.regionsView;
                        this.regionsView.reloadRegionsOnly();
                        this.regionsView.showRegions(this.regionsView.currentPage);
                    }

                    // 保護範囲表示
                    case 51 -> {
                        if (this.region.region() instanceof ProtectedCuboidRegion region) {
                            this.gui.guiState = State.WAITING_CALLBACK;
                            this.gui.player.closeInventory(InventoryCloseEvent.Reason.PLUGIN);

                            Util.startSelectionViewMode(this.gui.player, this.region, () -> {
                                WorldEdit.getRegionSelector(this.gui.player, this.region.world()).clear();
                                this.gui.guiState = State.REGION_INFORMATION;
                                this.gui.player.openInventory(this.gui.getInventory());
                            });
                        }
                    }

                    // 範囲再設定
                    case 52 -> {
                        if (this.region.region() instanceof ProtectedCuboidRegion region) {
                            this.gui.guiState = State.WAITING_CALLBACK;
                            this.gui.player.closeInventory(InventoryCloseEvent.Reason.PLUGIN);

                            // TODO 既存の選択範囲を引き継ぎながら、変更可能にする
                            //WorldEdit.getSession((Player) event.getWhoClicked()).setRegionSelector(BukkitAdapter.adapt(event.getWhoClicked().getWorld()), new CuboidRegionSelector(BukkitAdapter.adapt(this.region.world()), region.getMinimumPoint(), region.getMaximumPoint()));
                            Util.startSelectionMode(this.gui.player, null, null, null, (result) -> {
                                World world = result.world();
                                BlockVector3 min = result.min();
                                BlockVector3 max = result.max();
                                if (world == null || min == null || max == null) {
                                    this.gui.guiState = State.REGION_INFORMATION;
                                } else {
                                    ProtectedCuboidRegion newRegion = new ProtectedCuboidRegion(region.getId(), min, max);
                                    newRegion.copyFrom(region);
                                    RegionManager manager = WorldGuard.getRegionManager(result.world());
                                    // 重複チェック
                                    List<ProtectedRegion> intersectedRegions = newRegion.getIntersectingRegions(manager.getRegions().values())
                                            .stream()
                                            .filter(intersectedRegion -> !intersectedRegion.getId().equals(region.getId()))
                                            .toList();
                                    if (intersectedRegions.size() > 0) {
                                        Util.showRegionAreaIntersectedError(intersectedRegions, this, State.REGION_INFORMATION);
                                    } else {
                                        this.region.regionManager().removeRegion(region.getId());
                                        manager.addRegion(newRegion);
                                        this.clearInventory();
                                        this.gui.view = new RegionInfoView(this.regionsView, new WorldGuard.WrappedProtectedRegion(result.world(), newRegion));
                                    }

                                    WorldEdit.getRegionSelector(this.gui.player, result.world()).clear();
                                }

                                this.gui.player.openInventory(this.gui.getInventory());
                            });
                        } else {
                            Util.showUnsupportedRegionError(this, State.REGION_INFORMATION);
                        }
                    }

                    // 削除
                    case 53 -> {
                        this.clearInventory();
                        this.gui.guiState = State.WAITING_REGION_REMOVE;
                        this.gui.view = new RegionRemoveConfirmView(this);
                    }
                }
            }

            private static class RegionMemberManagementView extends ViewBase {
                public static final State STATE = State.MANAGE_MEMBERS;
                private final RegionInfoView regionInfoView;

                public RegionMemberManagementView(RegionInfoView regionInfoView) {
                    super(regionInfoView.gui);
                    this.regionInfoView = regionInfoView;
                    this.initialize();
                }

                @Override
                public void initialize() {
                    /*
                     *  [00] [01] [02] [03] [04] [05] [06] [07] [08]
                     *  [09] [10] [11] [12] [13] [14] [15] [16] [17]
                     *  [18] [19] [20] [21] [22] [23] [24] [25] [26]
                     *  [27] [28] [29] [30] [31] [32] [33] [34] [35]
                     *  [36] [37] [38] [39] [40] [41] [42] [43] [44]
                     *  [45] [46] [47] [48] [49] [50] [51] [52] [53]
                     */
                    this.gui.inventory.setItem(12, new ItemStackBuilder(Material.OAK_SIGN)
                            .displayName(Component.text("オーナー一覧", DefinedTextColor.GOLD))
                            .lore(new ArrayList<Component>() {{
                                DefaultDomain owners = regionInfoView.region.region().getOwners();
                                PlayerDomain ownerPlayers = owners.getPlayerDomain();
                                GroupDomain ownerGroups = owners.getGroupDomain();

                                ownerPlayers.getUniqueIds().forEach(uuid -> Util.appendPlayerComponent(this, uuid));
                                ownerGroups.getGroups().forEach(group -> Util.appendGroupComponent(this, group));
                            }}.toArray(new Component[0]))
                            .build());

                    this.gui.inventory.setItem(30, DefinedItemStackBuilders.plus()
                            .displayName(Component.text("追加", DefinedTextColor.GREEN))
                            .build()); // +
                    this.gui.inventory.setItem(39, DefinedItemStackBuilders.minus()
                            .displayName(Component.text("削除"))
                            .build()); // -

                    this.gui.inventory.setItem(14, new ItemStackBuilder(Material.BIRCH_SIGN)
                            .displayName(Component.text("メンバー一覧", DefinedTextColor.GREEN))
                            .lore(new ArrayList<Component>() {{
                                DefaultDomain members = regionInfoView.region.region().getMembers();
                                PlayerDomain memberPlayers = members.getPlayerDomain();
                                GroupDomain memberGroups = members.getGroupDomain();

                                memberPlayers.getUniqueIds().forEach(uuid -> Util.appendPlayerComponent(this, uuid));
                                memberGroups.getGroups().forEach(group -> Util.appendGroupComponent(this, group));
                            }}.toArray(new Component[0]))
                            .build());

                    this.gui.inventory.setItem(32, DefinedItemStackBuilders.plus()
                            .displayName(Component.text("追加", DefinedTextColor.GREEN))
                            .build()); // +

                    this.gui.inventory.setItem(41, DefinedItemStackBuilders.minus()
                            .displayName(Component.text("削除"))
                            .build()); // -

                    this.gui.inventory.setItem(45, DefinedItemStackBuilders.leftArrow()
                            .displayName(Component.text("戻る", DefinedTextColor.GREEN))
                            .build());
                }

                @Override
                public void onClick(InventoryClickEvent event) {
                    switch (event.getSlot()) {
                        // Owners
                        case 30 -> {
                            this.clearInventory();
                            this.gui.view = new PlayerSelectionView(this.gui, true, null, (e) -> {
                                this.gui.view = this;
                                this.initialize();
                            }, view -> {
                                this.gui.guiState = State.WAITING_CALLBACK;
                            }, name -> {
                                this.gui.guiState = State.ERROR;
                                this.gui.view = new ErrorView(Component.text("プレイヤーが見つかりません", DefinedTextColor.RED),
                                        Collections.singletonList(Component.text("プレイヤー " + name + " は見つかりませんでした", DefinedTextColor.YELLOW)),
                                        this.gui, this, STATE, null);
                            }, player -> {
                                this.regionInfoView.region.region().getOwners().addPlayer(player.getUniqueId());
                                this.gui.view = this;
                                this.initialize();
                                if (this.gui.guiState == State.WAITING_CALLBACK) {
                                    this.gui.player.openInventory(this.gui.getInventory());
                                    this.gui.guiState = State.MANAGE_MEMBERS;
                                }
                            });
                        }

                        case 39 -> {
                            this.clearInventory();
                            this.gui.view = new PlayerSelectionView(
                                    this.gui,
                                    false,
                                    this.regionInfoView.region.region().getOwners().getPlayerDomain()
                                            .getUniqueIds().stream()
                                            .map(Bukkit::getOfflinePlayer).toList(),
                                    e -> {
                                        this.gui.view = this;
                                        this.initialize();
                                    },
                                    null,
                                    null,
                                    player -> {
                                        this.regionInfoView.region.region().getOwners().getPlayerDomain().removePlayer(player.getUniqueId());
                                        this.gui.view = this;
                                        this.initialize();
                                    });
                        }

                        // Members
                        case 32 -> {
                            this.clearInventory();
                            this.gui.view = new PlayerSelectionView(this.gui, true, null, (e) -> {
                                this.gui.view = this;
                                this.initialize();
                            }, view -> {
                                this.gui.guiState = State.WAITING_CALLBACK;
                            }, name -> {
                                this.gui.guiState = State.ERROR;
                                this.gui.view = new ErrorView(Component.text("プレイヤーが見つかりません", DefinedTextColor.RED),
                                        Collections.singletonList(Component.text("プレイヤー " + name + " は見つかりませんでした", DefinedTextColor.YELLOW)),
                                        this.gui, this, STATE, null);
                            }, player -> {
                                this.regionInfoView.region.region().getMembers().addPlayer(player.getUniqueId());
                                this.gui.view = this;
                                this.initialize();
                                if (this.gui.guiState == State.WAITING_CALLBACK) {
                                    this.gui.player.openInventory(this.gui.getInventory());
                                    this.gui.guiState = State.MANAGE_MEMBERS;
                                }
                            });
                        }

                        case 41 -> {
                            this.clearInventory();
                            this.gui.view = new PlayerSelectionView(
                                    this.gui,
                                    false,
                                    this.regionInfoView.region.region().getMembers().getPlayerDomain()
                                            .getUniqueIds().stream()
                                            .map(Bukkit::getOfflinePlayer).toList(),
                                    e -> {
                                        this.gui.view = this;
                                        this.initialize();
                                    },
                                    null,
                                    null,
                                    player -> {
                                        this.regionInfoView.region.region().getMembers().getPlayerDomain().removePlayer(player.getUniqueId());
                                        this.gui.view = this;
                                        this.initialize();
                                    });
                        }

                        case 45 -> {
                            this.gui.guiState = State.REGION_INFORMATION;
                            this.clearInventory();
                            this.gui.view = this.regionInfoView;
                            this.gui.view.initialize();
                        }
                    }
                }
            }

            private static class RegionRemoveConfirmView extends ViewBase {
                private final RegionInfoView regionInfoView;

                public RegionRemoveConfirmView(RegionInfoView regionInfoView) {
                    super(regionInfoView.gui);
                    this.regionInfoView = regionInfoView;
                    this.initialize();
                }

                @Override
                public void initialize() {
                    this.gui.inventory.setItem(13, new ItemStackBuilder(Material.PAPER)
                            .displayName(Component.text("本当に保護領域を削除しますか？", Style.style(DefinedTextColor.YELLOW, TextDecoration.ITALIC.withState(false), TextDecoration.BOLD.withState(true))))
                            .lore(Component.text("ワールド: " + this.regionInfoView.region.world().getName()),
                                    Component.text("名前: " + this.regionInfoView.region.getId()),
                                    Component.text("範囲: ", Style.style(DefinedTextColor.AQUA, TextDecoration.ITALIC.withState(false)))
                                            .append(coordinates2Str(this.regionInfoView.region.region().getMinimumPoint()))
                                            .append(Component.text(" -> "))
                                            .append(coordinates2Str(this.regionInfoView.region.region().getMaximumPoint())))
                            .build());

                    this.gui.inventory.setItem(30, new ItemStackBuilder(Material.LIME_WOOL)
                            .displayName(Component.text("はい", Style.style(DefinedTextColor.GREEN, TextDecoration.ITALIC.withState(false), TextDecoration.BOLD.withState(true))))
                            .build());

                    this.gui.inventory.setItem(32, new ItemStackBuilder(Material.RED_WOOL)
                            .displayName(Component.text("いいえ", Style.style(DefinedTextColor.RED, TextDecoration.ITALIC.withState(false), TextDecoration.BOLD.withState(true))))
                            .build());
                }

                @Override
                public void onClick(InventoryClickEvent event) {
                    if (event.getSlot() == 30) {
                        WorldGuard.WrappedProtectedRegion wrapped = this.regionInfoView.region;
                        wrapped.regionManager().removeRegion(wrapped.getFullId());
                        NewMessageUtil.sendMessage(this.gui.player, Component.text("保護領域 " + wrapped.getId() + " を削除しました"));
                        this.clearInventory();
                        this.gui.guiState = State.REGIONS;
                        this.gui.view = this.regionInfoView.regionsView;
                        this.regionInfoView.regionsView.loadRegions();
                    } else if (event.getSlot() == 45 || event.getSlot() == 32) {
                        this.clearInventory();
                        this.gui.guiState = State.REGION_INFORMATION;
                        this.gui.view = this.regionInfoView;
                        this.regionInfoView.showInformation();
                    }
                }
            }
        }
    }

    private static class ErrorView extends ViewBase {
        private final Component errorTitle;
        private final List<Component> errorDetail;
        private final View prevView;
        private final State prevState;
        private final Consumer<Inventory> initializer;

        public ErrorView(@Nullable Component errorTitle,
                         @Nullable List<Component> errorDetail,
                         @Nonnull ProtectionGui gui,
                         @Nonnull View prevView,
                         @Nonnull State prevState,
                         @Nullable Consumer<Inventory> initializer) {
            super(gui);
            this.errorTitle = errorTitle;
            this.errorDetail = errorDetail;
            this.prevView = prevView;
            this.prevState = prevState;
            this.initializer = initializer;
            this.showError();
        }

        private void showError() {
            if (this.initializer != null) this.initializer.accept(this.gui.inventory);
            IntStream.rangeClosed(0, 44).forEach(i -> {
                this.gui.inventory.setItem(i, new ItemStackBuilder(Material.BARRIER).displayName(Component.empty()).build());
            });
            if (this.errorTitle == null) return;
            this.gui.inventory.setItem(13, new ItemStackBuilder(Material.PAPER)
                    .displayName(Component.empty()
                            .style(Style.style(DefinedTextColor.WHITE, TextDecoration.ITALIC.withState(false)))
                            .append(this.errorTitle))
                    .lore(this.errorDetail.stream()
                            .map(c -> Component.empty()
                                    .style(Style.style(DefinedTextColor.WHITE, TextDecoration.ITALIC.withState(false)))
                                    .append(c)).toArray(Component[]::new))
                    .build());
        }

        @Override
        public void initialize() {
            this.showError();
        }

        @Override
        public void onClick(InventoryClickEvent event) {
            if (event.getSlot() == 45) {
                this.clearInventory();
                this.gui.guiState = prevState;
                this.gui.view = prevView;
                this.prevView.initialize();
            }
        }
    }

    private static abstract class ViewBase implements View {
        protected final ProtectionGui gui;

        public ViewBase(ProtectionGui gui) {
            this.gui = gui;
        }

        @Override
        public abstract void initialize();

        @Override
        public abstract void onClick(InventoryClickEvent event);

        @Override
        public void clearInventory() {
            IntStream.rangeClosed(0, 44).forEach(this.gui.inventory::clear);
        }
    }

    public static class Util {
        public static void openSetNameWindow(@Nullable String regionName, ViewBase view, State state, Consumer<String> onComplete) {
            new SignGui().withTarget(view.gui.player)
                    .withLines(regionName != null ? Component.text(regionName) : Component.empty(),
                            Component.text("^^^^^^^^^^^^^^"),
                            Component.text("保護領域名を入力"),
                            Component.empty())
                    .onComplete(lines -> {
                        String newRegionName = PlainTextComponentSerializer.plainText().serialize(lines.get(0));
                        view.clearInventory();
                        if (ProtectedRegion.isValidId(newRegionName)) {
                            onComplete.accept(newRegionName);
                        } else {
                            view.gui.guiState = State.ERROR;
                            view.gui.view = new ErrorView(Component.text("使用できない名前です", DefinedTextColor.RED),
                                    Arrays.asList(Component.text("保護領域名に使用できない名前が含まれています", DefinedTextColor.YELLOW),
                                            Component.text("使用できる文字はA-Za-z0-9_,'-+/です。", DefinedTextColor.YELLOW)),
                                    view.gui, view, state, null);
                        }
                        view.gui.player.openInventory(view.gui.getInventory());
                    }).open();
        }

        public static void startSelectionMode(Player player, World initialWorld, BlockVector3 initialMin, BlockVector3 initialMax, Consumer<SelectionResult> onComplete) {
            Set<Listener> listeners = new HashSet<>();

            SelectionResult result = new SelectionResult(initialWorld, initialMin, initialMax);

            ItemStack currentHand = player.getInventory().getItemInMainHand();
            ItemStack newHand = new ItemStackBuilder(Material.GOLDEN_AXE)
                    .displayName(Component.text("範囲選択斧", DefinedTextColor.GOLD))
                    .addEnchantment(Enchantment.DIG_SPEED, 1)
                    .custom(is -> is.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ENCHANTS))
                    .build();
            player.getInventory().setItem(player.getInventory().getHeldItemSlot(), newHand);

            NewMessageUtil.sendMessage(player, Component.empty()
                    .append(Component.keybind("key.drop"))
                    .append(Component.text(" "))
                    .append(Component.text("を押して、範囲選択用の金の斧を投げると範囲選択を終了します。")), false);

            BukkitTask task = RunnableManager.runAsyncRepeating(() -> {
                player.sendActionBar(Component.text("≪範囲選択モードが有効です≫").style(Style.style(DefinedTextColor.GOLD, TextDecoration.BOLD)));
            }, 0L, 3L);

            ListenerManager.registerEventListener(PlayerInteractEvent.class, createListener(listeners), EventPriority.MONITOR, false, (l, ev) -> {
                if (ev instanceof PlayerInteractEvent e) {
                    if (!e.getPlayer().equals(player)) return;
                    if (e.getHand() != EquipmentSlot.HAND) return;

                    if (e.getItem() == null || e.getItem().getType() != Material.GOLDEN_AXE) return;
                    if (e.getItem().getItemMeta() == null || e.getItem().getItemMeta().displayName() == null) return;
                    if (!e.getItem().getEnchantments().containsKey(Enchantment.DIG_SPEED)) return;

                    if (e.getClickedBlock() == null || !e.hasBlock()) return;
                    if (e.getAction() != Action.LEFT_CLICK_BLOCK && e.getAction() != Action.RIGHT_CLICK_BLOCK) return;

                    e.setCancelled(true);
                    Location clickedLoc = e.getClickedBlock().getLocation();
                    BlockVector3 clickedLocConv = BlockVector3.at(clickedLoc.getX(), clickedLoc.getY(), clickedLoc.getZ());
                    if (result.world() != null && !clickedLoc.getWorld().equals(result.world())) {
                        result.min(null);
                        result.max(null);
                        NewMessageUtil.sendMessage(player, Component.text("ワールドが変更されたようです。選択範囲がリセットされました。"));
                    } else {
                        result.world(clickedLoc.getWorld());
                    }

                    if (e.getAction() == Action.LEFT_CLICK_BLOCK) {
                        // #1
                        result.min(clickedLocConv);
                        NewMessageUtil.sendMessage(player, Component.text("地点 #1 を ")
                                .append(coordinates2Str(result.min()))
                                .append(Component.text(" "))
                                .append(Component.text(" に設定しました")));

                    } else {
                        // #2
                        result.max(clickedLocConv);
                        NewMessageUtil.sendMessage(player, Component.text("地点 #2 を ")
                                .append(coordinates2Str(result.max()))
                                .append(Component.text(" "))
                                .append(Component.text(" に設定しました")));
                    }

                    if (result.min() != null && result.max() != null) {
                        NewMessageUtil.sendMessage(player, Component.text("両地点を選択しました。"), false);
                        LocalSession session = WorldEdit.getSession(player);
                        RegionSelector selector = WorldEdit.getRegionSelector(player, result.world());
                        if (!(selector instanceof CuboidRegionSelector)) {
                            selector = new CuboidRegionSelector();
                            session.setRegionSelector(BukkitAdapter.adapt(result.world()), selector);
                        }
                        selector.selectPrimary(result.min(), null);
                        selector.selectSecondary(result.max(), null);
                    } else {
                        NewMessageUtil.sendMessage(player, Component.text("地点を片方しか選択していません。保護を完了するには、両地点の選択を完了してください。"), false);
                    }
                }
            });

            ListenerManager.registerEventListener(PlayerDropItemEvent.class, createListener(listeners), EventPriority.MONITOR, false, (l, ev) -> {
                if (ev instanceof PlayerDropItemEvent e) {
                    if (!e.getPlayer().equals(player)) return;
                    if (e.getItemDrop().getItemStack().getType() != Material.GOLDEN_AXE) return;
                    if (e.getItemDrop().getItemStack().getItemMeta() == null || e.getItemDrop().getItemStack().getItemMeta().displayName() == null)
                        return;
                    if (!e.getItemDrop().getItemStack().getEnchantments().containsKey(Enchantment.DIG_SPEED)) return;
                    player.getInventory().setItem(player.getInventory().getHeldItemSlot(), currentHand);
                    unregisterAllListeners(listeners);
                    if (!task.isCancelled()) task.cancel();
                    e.getItemDrop().remove();
                    onComplete.accept(result);
                }
            });

            ListenerManager.registerEventListener(PlayerQuitEvent.class, createListener(listeners), EventPriority.MONITOR, false, (l, ev) -> {
                if (ev instanceof PlayerQuitEvent e) {
                    if (e.getPlayer().equals(player)) {
                        player.getInventory().setItem(player.getInventory().getHeldItemSlot(), currentHand);
                        unregisterAllListeners(listeners);
                        if (!task.isCancelled()) task.cancel();
                    }
                }
            });

            ListenerManager.registerEventListener(PlayerSwapHandItemsEvent.class, createListener(listeners), EventPriority.LOWEST, false, (l, ev) -> {
                if (ev instanceof PlayerSwapHandItemsEvent e) {
                    if (e.getPlayer().equals(player)) {
                        e.setCancelled(true);
                    }
                }
            });
        }

        public static void startSelectionViewMode(Player player, WorldGuard.WrappedProtectedRegion region, Runnable onEnd) {
            Set<Listener> listeners = new HashSet<>();

            ItemStack currentHand = player.getInventory().getItemInMainHand();
            ItemStack newHand = new ItemStackBuilder(Material.SPYGLASS)
                    .addEnchantment(Enchantment.PROTECTION_PROJECTILE, 1)
                    .custom(is -> is.addItemFlags(ItemFlag.HIDE_ENCHANTS))
                    .build();

            if (region.region() instanceof ProtectedCuboidRegion cuboid) {
                WorldEdit.getSession(player).setRegionSelector(BukkitAdapter.adapt(player.getWorld()), new CuboidRegionSelector(BukkitAdapter.adapt(region.world()), cuboid.getMinimumPoint(), cuboid.getMaximumPoint()));
            } else {
                NewMessageUtil.sendErrorMessage(player, Component.empty().append(Component.text("この形の保護領域には対応していません。正常に表示されない可能性があります。")));
            }

            player.getInventory().setItem(player.getInventory().getHeldItemSlot(), newHand);

            NewMessageUtil.sendMessage(player, Component.empty()
                    .append(Component.keybind("key.drop"))
                    .append(Component.text(" を押して、 "))
                    .append(Component.translatable(Material.SPYGLASS))
                    .append(Component.text(" を投げると保護範囲確認モードを終了します。")), false);

            BukkitTask task = RunnableManager.runAsyncRepeating(() -> {
                player.sendActionBar(Component.text("≪範囲確認モードが有効です≫").style(Style.style(DefinedTextColor.GOLD, TextDecoration.BOLD)));
            }, 0L, 3L);

            ListenerManager.registerEventListener(PlayerDropItemEvent.class, createListener(listeners), EventPriority.MONITOR, false, (l, ev) -> {
                if (ev instanceof PlayerDropItemEvent e) {
                    /* Pre-tests */
                    if (!e.getPlayer().equals(player)) return;

                    Item toDropItemEntity = e.getItemDrop();
                    ItemStack toDropItem = toDropItemEntity.getItemStack();
                    if (toDropItem.getType() != Material.SPYGLASS) return;

                    Map<Enchantment, Integer> toDropItemEnchants = toDropItem.getEnchantments();
                    if (!toDropItemEnchants.containsKey(Enchantment.PROTECTION_PROJECTILE)) return;
                    if (toDropItemEnchants.get(Enchantment.PROTECTION_PROJECTILE) != 1) return;
                    /* End of Pre-tests */

                    // TODO: インベントリからアイテムを捨てられると、HeldItemSlotの場所に対象のアイテムがあるとは限らないので、別途処理
                    // TODO: getHeldItemSlot() のスロットに指定したアイテム以外がある場合は、firstEmpty()のスロットに入れるようにすると良さそう
                    e.getPlayer().getInventory().setItem(e.getPlayer().getInventory().getHeldItemSlot(), currentHand);
                    unregisterAllListeners(listeners);
                    if (!task.isCancelled()) task.cancel();
                    e.getItemDrop().remove();
                    onEnd.run();
                }
            });
        }

        public static Listener createListener(Set<Listener> listeners) {
            Listener listener = new Listener() {
            };
            listeners.add(listener);
            return listener;
        }

        private static void unregisterAllListeners(Set<Listener> listeners) {
            listeners.forEach(HandlerList::unregisterAll);
        }

        public static void appendGroupComponent(List<Component> lore, String group) {
            lore.add(Component.text("[G] " + group, DefinedTextColor.GREEN));
        }

        public static void appendPlayerComponent(List<Component> lore, UUID uuid) {
            OfflinePlayer player = Bukkit.getOfflinePlayer(uuid);
            if (player != null && player.getName() != null) {
                lore.add(Component.text(player.getName(), DefinedTextColor.AQUA));
            } else {
                lore.add(Component.text("Unknown{" + uuid + "}", DefinedTextColor.YELLOW));
            }
        }

        public static void showUnsupportedRegionError(ViewBase view, State state) {
            view.clearInventory();
            view.gui.guiState = State.ERROR;
            view.gui.view = new ErrorView(Component.text("サポートされていない保護領域です。", DefinedTextColor.RED),
                    Collections.singletonList(Component.text("運営にお問い合わせください。", DefinedTextColor.YELLOW)),
                    view.gui, view, state, null);
        }

        public static void showRegionAreaIntersectedError(List<ProtectedRegion> intersectedRegions, ViewBase view, State state) {
            /*long createdRegionIntersects = intersectedRegions.stream().filter(intersectedRegion -> {
                String id = intersectedRegion.getId();
                return (WorldGuard.ID_PATTERN.matcher(id).matches() && UUID.fromString(id.split(WorldGuard.SPLITTER, 2)[0]).equals(this.gui.player.getUniqueId()));
            }).count();

            long ownedRegionIntersects = intersectedRegions.stream().filter(intersectedRegion -> {
                return intersectedRegion.getOwners().contains(this.gui.player.getUniqueId());
            }).count();

            long otherRegionIntersects = intersectedRegions.size() - createdRegionIntersects - ownedRegionIntersects;*/
            // TODO 選択範囲の重複の種類 (あなたの作成した保護領域と... | あなたが管理している保護領域と... | 他人の保護領域と...)
            view.clearInventory();
            view.gui.guiState = State.ERROR;
            view.gui.view = new ErrorView(Component.text("エラーが発生しました", Style.style(DefinedTextColor.RED, TextDecoration.BOLD.withState(true), TextDecoration.UNDERLINED.withState(true))),
                    Arrays.asList(
                            Component.text("保護範囲が重複しています。", DefinedTextColor.YELLOW),
                            Component.text("範囲を再選択してください。", DefinedTextColor.YELLOW),
                            Component.text("重複している保護領域の数: " + intersectedRegions.size(), DefinedTextColor.YELLOW)
                    ), view.gui, view, state, null);
        }

        public static class SelectionResult {
            private World world = null;
            private BlockVector3 min = null;
            private BlockVector3 max = null;

            public SelectionResult() {}

            public SelectionResult(World world, BlockVector3 initialMin, BlockVector3 initialMax) {
                this.world = world;
                this.min = initialMin;
                this.max = initialMax;
            }

            public World world() {
                return this.world;
            }

            public void world(World world) {
                this.world = world;
            }

            public BlockVector3 min() {
                return this.min;
            }

            public void min(BlockVector3 min) {
                this.min = min;
            }

            public BlockVector3 max() {
                return this.max;
            }

            public void max(BlockVector3 max) {
                this.max = max;
            }
        }
    }
}
