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

package net.unknown.survival.gui.protection.view;

import com.ryuuta0217.util.ListUtil;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldguard.domains.DefaultDomain;
import com.sk89q.worldguard.domains.GroupDomain;
import com.sk89q.worldguard.domains.PlayerDomain;
import com.sk89q.worldguard.protection.flags.*;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedCuboidRegion;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import io.papermc.paper.dialog.Dialog;
import io.papermc.paper.registry.data.dialog.ActionButton;
import io.papermc.paper.registry.data.dialog.DialogBase;
import io.papermc.paper.registry.data.dialog.action.DialogAction;
import io.papermc.paper.registry.data.dialog.body.DialogBody;
import io.papermc.paper.registry.data.dialog.input.DialogInput;
import io.papermc.paper.registry.data.dialog.input.SingleOptionDialogInput;
import io.papermc.paper.registry.data.dialog.input.TextDialogInput;
import io.papermc.paper.registry.data.dialog.type.DialogType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickCallback;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.unknown.core.builder.ItemStackBuilder;
import net.unknown.core.define.DefinedItemStackBuilders;
import net.unknown.core.define.DefinedTextColor;
import net.unknown.core.gui.view.PlayerSelectionView;
import net.unknown.core.util.MessageUtil;
import net.unknown.core.util.NewMessageUtil;
import net.unknown.survival.dependency.WorldEdit;
import net.unknown.survival.dependency.WorldGuard;
import net.unknown.survival.gui.protection.ProtectionGui;
import net.unknown.survival.gui.protection.ProtectionGuiState;
import net.unknown.survival.gui.protection.ProtectionGuiUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.ItemFlag;

import java.util.*;

public class ProtectionGuiRegionsView extends ProtectionGuiViewBase {
    private final Map<Integer, WorldGuard.WrappedProtectedRegion> slot2region = new HashMap<>();
    private List<Set<WorldGuard.WrappedProtectedRegion>> regions;
    private int currentPage = 1;

    public ProtectionGuiRegionsView(ProtectionGui gui) {
        super(gui);
        this.loadRegions();
    }

    public void loadRegions() {
        this.reloadRegionsOnly();
        this.showRegions(1);
    }

    public void reloadRegionsOnly() {
        this.regions = ListUtil.splitListAsLinkedSet(WorldGuard.getProtectedRegions(this.gui.getPlayer()), 45);
    }

    public void showRegions(int newPage) {
        this.currentPage = newPage;
        this.clearInventory();
        this.regions.get(this.currentPage - 1).forEach(region -> {
            int toAddSlot = this.gui.getInventory().firstEmpty();
            this.gui.getInventory().setItem(toAddSlot, new ItemStackBuilder(Material.LIME_WOOL)
                    .displayName(Component.text(region.getId()))
                    .lore(Component.text("ワールド: " + MessageUtil.getWorldNameDisplay(region.world()), DefinedTextColor.GREEN),
                            Component.empty()
                                    .append(ProtectionGuiUtil.coordinates2Str(region.region().getMinimumPoint()))
                                    .append(Component.text(" -> ", DefinedTextColor.GREEN))
                                    .append(ProtectionGuiUtil.coordinates2Str(region.region().getMaximumPoint())),
                            region.getCreatorUniqueId() != null ? Component.text("作成者: " + Bukkit.getOfflinePlayer(region.getCreatorUniqueId()).getName(), DefinedTextColor.GOLD) : Component.empty())
                    .build());
            this.slot2region.put(toAddSlot, region);
        });

        this.gui.getInventory().setItem(49, DefinedItemStackBuilders.plus()
                .displayName(Component.text("新規保護", DefinedTextColor.GREEN))
                .build());

        this.gui.getInventory().setItem(50, new ItemStackBuilder(Material.ENDER_CHEST)
                .displayName(Component.text("グループ管理", DefinedTextColor.GOLD))
                .lore(Component.text("グループの追加、編集、削除が行えます。", DefinedTextColor.YELLOW),
                        Component.text("グループメンバーの追加、削除が行えます。", DefinedTextColor.YELLOW))
                .build());

        if (this.regions.size() > 1 && this.currentPage < this.regions.size()) {
            this.gui.getInventory().setItem(53, DefinedItemStackBuilders.rightArrow()
                    .displayName(Component.text("次のページ", TextColor.color(5635925)))
                    .build());
        } else {
            this.gui.getInventory().clear(53);
        }

        if (this.currentPage > 1) {
            this.gui.getInventory().setItem(52, DefinedItemStackBuilders.leftArrow()
                    .displayName(Component.text("前のページ", TextColor.color(5635925)))
                    .build());
        } else {
            this.gui.getInventory().clear(52);
        }
    }

    @Override
    public void clearInventory() {
        super.clearInventory();
        this.slot2region.clear();
        this.gui.getInventory().clear(49);
        this.gui.getInventory().clear(50);
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
            this.gui.getInventory().clear(52);
            this.gui.getInventory().clear(53);

            this.gui.setGuiState(ProtectionGuiState.REGION_INFORMATION);
            this.gui.setView(new RegionInfoView(this, selectedRegion));
        } else if (event.getSlot() == 49) {
            this.gui.setGuiState(ProtectionGuiState.WAITING_CALLBACK);
            this.gui.getPlayer().closeInventory(InventoryCloseEvent.Reason.PLUGIN);
            ProtectionGuiUtil.openSetNameWindow(null, this, ProtectionGuiState.REGIONS, (name) -> {
                this.gui.getInventory().clear(52);
                this.gui.getInventory().clear(53);

                this.gui.setGuiState(ProtectionGuiState.NEW_REGION);
                this.gui.setView(new NewRegionView(this, name));
            });
        } else if (event.getSlot() == 50) {
            // Group Management
        } else if (event.getSlot() == 52 && this.currentPage > 1) {
            this.showRegions(this.currentPage - 1);
        } else if (event.getSlot() == 53 && this.currentPage < this.regions.size()) {
            this.showRegions(this.currentPage + 1);
        }
    }

    private static class NewRegionView extends ProtectionGuiViewBase {
        private final ProtectionGuiRegionsView regionsView;
        private String newRegionName;
        private World world;
        private BlockVector3 min;
        private BlockVector3 max;

        public NewRegionView(ProtectionGuiRegionsView regionsView, String newRegionName) {
            super(regionsView.gui);
            this.regionsView = regionsView;
            this.newRegionName = newRegionName;
            this.initialize();
        }

        @Override
        public void initialize() {
            this.gui.getInventory().setItem(13, new ItemStackBuilder(Material.PAPER)
                    .displayName(Component.text("新規保護領域の作成",
                            Style.style(DefinedTextColor.GREEN,
                                    TextDecoration.BOLD.withState(true))))
                    .lore(Component.text("名前: " + this.newRegionName,
                                    Style.style(DefinedTextColor.AQUA,
                                            TextDecoration.ITALIC.withState(false))),
                            Component.text("範囲: ", Style.style(DefinedTextColor.YELLOW,
                                            TextDecoration.ITALIC.withState(false)))
                                    .append((this.min == null && this.max == null ? Component.text("未設定") : (this.min != null ? (ProtectionGuiUtil.coordinates2Str(this.min).append(this.max != null ? Component.text(" -> ").append(ProtectionGuiUtil.coordinates2Str(this.max)) : Component.text(" -> [未設定]"))) : Component.text("[未設定]")))))
                    .build());

            this.gui.getInventory().setItem(30, new ItemStackBuilder(Material.NAME_TAG)
                    .displayName(Component.text("保護領域名", DefinedTextColor.GOLD))
                    .lore(Component.text("現在の設定値: " + this.newRegionName,
                                    Style.style(DefinedTextColor.AQUA,
                                            TextDecoration.ITALIC.withState(false))),
                            Component.text(""),
                            Component.text("クリックして名前を変更",
                                    Style.style(DefinedTextColor.YELLOW,
                                            TextDecoration.ITALIC.withState(false))))
                    .build());
            this.gui.getInventory().setItem(32, new ItemStackBuilder(Material.OAK_FENCE)
                    .displayName(Component.text("保護範囲", DefinedTextColor.YELLOW))
                    .lore(Component.text("地点#1: ", Style.style(DefinedTextColor.AQUA, TextDecoration.ITALIC.withState(false)))
                                    .append(this.min == null ? Component.text("未設定") : ProtectionGuiUtil.coordinates2Str(this.min)),
                            Component.text("地点#2: ", Style.style(DefinedTextColor.AQUA, TextDecoration.ITALIC.withState(false)))
                                    .append(this.max == null ? Component.text("未設定") : ProtectionGuiUtil.coordinates2Str(this.max)),
                            Component.text(""),
                            Component.text("クリックして範囲設定を開始",
                                    Style.style(DefinedTextColor.YELLOW,
                                            TextDecoration.ITALIC.withState(false))))
                    .build());

            if (this.newRegionName != null && this.min != null && this.max != null) {
                this.gui.getInventory().setItem(49, new ItemStackBuilder(Material.LIME_WOOL)
                        .displayName(Component.text("保護を確定", Style.style(
                                DefinedTextColor.GREEN,
                                TextDecoration.BOLD.withState(true),
                                TextDecoration.UNDERLINED.withState(true))))
                        .lore(Component.text("名前: " + this.newRegionName,
                                        Style.style(DefinedTextColor.AQUA,
                                                TextDecoration.ITALIC.withState(false))),
                                Component.text("範囲: ", Style.style(DefinedTextColor.YELLOW,
                                                TextDecoration.ITALIC.withState(false)))
                                        .append(ProtectionGuiUtil.coordinates2Str(this.min))
                                        .append(Component.text(" -> "))
                                        .append(ProtectionGuiUtil.coordinates2Str(this.max)))
                        .build());
            }
        }

        @Override
        public void clearInventory() {
            super.clearInventory();
            this.gui.getInventory().setItem(49, null);
        }

        @Override
        public void onClick(InventoryClickEvent event) {
            if (event.getSlot() == 30) {
                this.gui.setGuiState(ProtectionGuiState.WAITING_CALLBACK);
                this.gui.getPlayer().closeInventory(InventoryCloseEvent.Reason.PLUGIN);
                ProtectionGuiUtil.openSetNameWindow(this.newRegionName, this, ProtectionGuiState.NEW_REGION, name -> {
                    this.newRegionName = name;
                    this.gui.setGuiState(ProtectionGuiState.NEW_REGION);
                    this.initialize();
                });
            } else if (event.getSlot() == 32) {
                this.gui.setGuiState(ProtectionGuiState.WAITING_CALLBACK);
                this.gui.getPlayer().closeInventory(InventoryCloseEvent.Reason.PLUGIN);
                ProtectionGuiUtil.startSelectionMode(this.gui.getPlayer(), null, null, null, (result) -> {
                    this.world = result.world();
                    this.min = result.min();
                    this.max = result.max();

                    WorldEdit.getRegionSelector(this.gui.getPlayer(), result.world()).clear(); // Clear selection to remove visualizer

                    this.gui.setGuiState(ProtectionGuiState.NEW_REGION);
                    this.initialize();
                    this.gui.getPlayer().openInventory(this.gui.getInventory());
                });
            } else if (event.getSlot() == 45) {
                /* 保護領域一覧に戻る */
                this.gui.setGuiState(ProtectionGuiState.REGIONS);
                this.gui.setView(this.regionsView);
                this.clearInventory();
                this.regionsView.loadRegions();
                this.regionsView.showRegions(1);
            } else if (event.getSlot() == 49 && event.getCurrentItem() != null) {
                /* 保護領域作成確定 */
                if (this.newRegionName != null && this.min != null && this.max != null) {
                    ProtectedCuboidRegion region = new ProtectedCuboidRegion(this.gui.getPlayer().getUniqueId() + WorldGuard.SPLITTER + this.newRegionName, false, this.min, this.max);
                    region.getOwners().addPlayer(this.gui.getPlayer().getUniqueId());

                    RegionManager manager = WorldGuard.getPlatform().getRegionContainer().get(BukkitAdapter.adapt(this.world));
                    WorldEdit.getRegionSelector(this.gui.getPlayer(), this.world).clear();

                    // 保護領域重複チェック
                    List<ProtectedRegion> intersectedRegions = region.getIntersectingRegions(manager.getRegions().values());
                    if (intersectedRegions.size() > 0) {
                        ProtectionGuiUtil.showRegionAreaIntersectedError(intersectedRegions, this, ProtectionGuiState.NEW_REGION);
                        this.min = null;
                        this.max = null;
                        WorldEdit.getRegionSelector(this.gui.getPlayer(), this.world).clear();
                    } else {
                        manager.addRegion(region);
                        NewMessageUtil.sendMessage(this.gui.getPlayer(), Component.text("ワールド " + MessageUtil.getWorldName(this.world) + " に新しい保護領域 " + this.newRegionName + " を作成しました！"));
                        this.clearInventory();
                        this.gui.setGuiState(ProtectionGuiState.REGION_INFORMATION);
                        this.gui.setView(new RegionInfoView(this.regionsView, new WorldGuard.WrappedProtectedRegion(this.world, region)));
                    }
                }
            }
        }
    }

    @SuppressWarnings("UnstableApiUsage")
    private static class RegionInfoView extends ProtectionGuiViewBase {
        private final ProtectionGuiRegionsView regionsView;
        private final WorldGuard.WrappedProtectedRegion region;

        public RegionInfoView(ProtectionGuiRegionsView regionsView, WorldGuard.WrappedProtectedRegion region) {
            super(regionsView.gui);
            this.regionsView = regionsView;
            this.region = region;
            this.showInformation();
        }

        private void showInformation() {
            this.gui.getInventory().setItem(13, new ItemStackBuilder(Material.PAPER)
                    .displayName(Component.text("保護領域の情報", Style.style(DefinedTextColor.GOLD, TextDecoration.ITALIC.withState(false))))
                    .lore(Component.text("ワールド: " + MessageUtil.getWorldName(this.region.world().getName()), Style.style(DefinedTextColor.AQUA, TextDecoration.ITALIC.withState(false))),
                            Component.text("名前: " + this.region.getId(), Style.style(DefinedTextColor.AQUA, TextDecoration.ITALIC.withState(false))),
                            Component.text("範囲: ", Style.style(DefinedTextColor.AQUA, TextDecoration.ITALIC.withState(false)))
                                    .append(ProtectionGuiUtil.coordinates2Str(this.region.region().getMinimumPoint()))
                                    .append(Component.text(" -> "))
                                    .append(ProtectionGuiUtil.coordinates2Str(this.region.region().getMaximumPoint())))
                    .build());

            this.gui.getInventory().setItem(29, new ItemStackBuilder(Material.NAME_TAG)
                    .displayName(Component.text("保護領域名の変更", Style.style(DefinedTextColor.GREEN, TextDecoration.ITALIC.withState(false))))
                    .lore(Component.text("現在の名前: " + this.region.getId(), DefinedTextColor.AQUA))
                    .build());

            this.gui.getInventory().setItem(31, new ItemStackBuilder(Material.SHIELD)
                    .displayName(Component.text("フラグの設定", Style.style(DefinedTextColor.YELLOW, TextDecoration.ITALIC.withState(false))))
                    .build());

            this.gui.getInventory().setItem(33, new ItemStackBuilder(Material.DIAMOND_CHESTPLATE)
                    .displayName(Component.text("メンバーの管理", Style.style(DefinedTextColor.LIGHT_PURPLE, TextDecoration.ITALIC.withState(false))))
                    .lore(new ArrayList<Component>() {{
                        DefaultDomain owners = region.region().getOwners();
                        PlayerDomain ownerPlayers = owners.getPlayerDomain();
                        GroupDomain ownerGroups = owners.getGroupDomain();
                        DefaultDomain members = region.region().getMembers();
                        PlayerDomain memberPlayers = members.getPlayerDomain();
                        GroupDomain memberGroups = members.getGroupDomain();

                        add(Component.text("オーナー", DefinedTextColor.AQUA));
                        ownerPlayers.getUniqueIds().forEach(uuid -> ProtectionGuiUtil.appendPlayerComponent(this, uuid));
                        ownerGroups.getGroups().forEach(group -> ProtectionGuiUtil.appendGroupComponent(this, group));

                        if (members.size() > 0) {
                            add(Component.empty());

                            add(Component.text("メンバー", DefinedTextColor.GREEN));
                            memberPlayers.getUniqueIds().forEach(uuid -> ProtectionGuiUtil.appendPlayerComponent(this, uuid));
                            memberGroups.getGroups().forEach(group -> ProtectionGuiUtil.appendGroupComponent(this, group));
                        }
                    }}.toArray(new Component[0]))
                    .addItemFlag(ItemFlag.HIDE_ATTRIBUTES)
                    .build());

            this.gui.getInventory().setItem(51, new ItemStackBuilder(Material.SPYGLASS)
                    .displayName(Component.text("保護領域の範囲を表示", Style.style(DefinedTextColor.AQUA, TextDecoration.BOLD)))
                    .build());

            this.gui.getInventory().setItem(52, new ItemStackBuilder(Material.OAK_FENCE)
                    .displayName(Component.text("保護範囲を変更", Style.style(DefinedTextColor.GREEN, TextDecoration.BOLD)))
                    .build());

            this.gui.getInventory().setItem(53, new ItemStackBuilder(Material.LAVA_BUCKET)
                    .displayName(Component.text("保護領域の削除", Style.style(DefinedTextColor.RED, TextDecoration.BOLD)))
                    .build());
        }

        @Override
        public void clearInventory() {
            super.clearInventory();
            this.gui.getInventory().clear(51);
            this.gui.getInventory().clear(52);
            this.gui.getInventory().clear(53);
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
                        this.gui.setGuiState(ProtectionGuiState.WAITING_CALLBACK);
                        this.gui.getPlayer().closeInventory(InventoryCloseEvent.Reason.PLUGIN);
                        String oldFullId = this.region.getFullId();
                        String oldId = this.region.getId();
                        ProtectionGuiUtil.openSetNameWindow(this.region.getId(), this, ProtectionGuiState.REGION_INFORMATION, (newId) -> {
                            String newFullId = this.gui.getPlayer().getUniqueId() + WorldGuard.SPLITTER + newId;
                            if (newFullId.equals(oldFullId)) {
                                this.gui.setGuiState(ProtectionGuiState.REGION_INFORMATION);
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
                                    this.gui.setGuiState(ProtectionGuiState.REGION_INFORMATION);
                                    NewMessageUtil.sendMessage(this.gui.getPlayer(), Component.text("保護領域の名前を " + oldId + " から " + newId + " に変更しました"));
                                    this.gui.setView(new RegionInfoView(this.regionsView,
                                            new WorldGuard.WrappedProtectedRegion(this.region.world(), renamed)));
                                } else {
                                    this.gui.setGuiState(ProtectionGuiState.ERROR);
                                    this.gui.setView(new ProtectionGuiErrorView(Component.text("保護領域名 " + newId + " は既に使用されています。", DefinedTextColor.RED),
                                            Arrays.asList(Component.text("ほかの保護領域名を検討してください。", DefinedTextColor.YELLOW),
                                                    Component.text("ワールド: " + this.region.world().getName())), this.gui, this.gui.getView(), ProtectionGuiState.REGION_INFORMATION, null));
                                }
                            }
                        });
                    } else {
                        ProtectionGuiUtil.showUnsupportedRegionError(this, ProtectionGuiState.REGION_INFORMATION);
                    }
                }

                // フラグ管理
                case 31 -> {
                    this.gui.setGuiState(ProtectionGuiState.WAITING_CALLBACK);
                    this.gui.getPlayer().closeInventory(InventoryCloseEvent.Reason.PLUGIN);
                    event.getWhoClicked().showDialog(Dialog.create(builder -> builder.empty()
                            .base(DialogBase.builder(Component.text("フラグの設定"))
                                    .afterAction(DialogBase.DialogAfterAction.WAIT_FOR_RESPONSE)
                                    .externalTitle(Component.text("フラグの設定(Ex)"))
                                    .pause(false)
                                    .body(Collections.singletonList(DialogBody.plainMessage(Component.text("保護領域「" + this.region.getId() + "」のフラグを設定してください"), 1024)))
                                    .inputs(com.sk89q.worldguard.WorldGuard.getInstance()
                                            .getFlagRegistry()
                                            .getAll()
                                            .parallelStream()
                                            .filter(flag -> flag != Flags.BUILD)
                                            .map(flag -> {
                                                String flagName = flag.getName().replace('-', '_');
                                                try {
                                                    if (flag instanceof StateFlag stateFlag) {
                                                        StateFlag.State defaultValue = stateFlag.getDefault();
                                                        StateFlag.State currentValue = this.region.region().getFlag(stateFlag);

                                                        int choice = 0;
                                                        if (currentValue != null) {
                                                            choice = currentValue == StateFlag.State.ALLOW ? 1 : 2;
                                                        }

                                                        return DialogInput.singleOption(
                                                                flagName,
                                                                256,
                                                                Arrays.asList(
                                                                        SingleOptionDialogInput.OptionEntry.create("unset", ProtectionGuiUtil.getStateFlagValueDisplayName(stateFlag, null), choice == 0),
                                                                        SingleOptionDialogInput.OptionEntry.create("allow", ProtectionGuiUtil.getStateFlagValueDisplayName(stateFlag, StateFlag.State.ALLOW), choice == 1),
                                                                        SingleOptionDialogInput.OptionEntry.create("deny", ProtectionGuiUtil.getStateFlagValueDisplayName(stateFlag, StateFlag.State.DENY), choice == 2)
                                                                ),
                                                                Component.text(ProtectionGuiUtil.getFlagDisplayName(flag)),
                                                                true
                                                        );
                                                    }

                                                    if (flag instanceof BooleanFlag boolFlag) {
                                                        Boolean defaultValue = boolFlag.getDefault();
                                                        Boolean currentValue = this.region.region().getFlag(boolFlag);
                                                        if (defaultValue != null) {
                                                            System.out.println("WHAT?! I've found non-null default value in BooleanFlag!");
                                                        }

                                                        return DialogInput.singleOption(
                                                                flagName,
                                                                256,
                                                                Arrays.asList(
                                                                        SingleOptionDialogInput.OptionEntry.create("unset", ProtectionGuiUtil.getBooleanFlagValueDisplayName(boolFlag, null), currentValue == null),
                                                                        SingleOptionDialogInput.OptionEntry.create("true", ProtectionGuiUtil.getBooleanFlagValueDisplayName(boolFlag, true), Boolean.TRUE.equals(currentValue)),
                                                                        SingleOptionDialogInput.OptionEntry.create("false", ProtectionGuiUtil.getBooleanFlagValueDisplayName(boolFlag, false), Boolean.FALSE.equals(currentValue))
                                                                ),
                                                                Component.text(ProtectionGuiUtil.getFlagDisplayName(flag)),
                                                                true
                                                        );
                                                    }

                                                    if (flag instanceof StringFlag strFlag) {
                                                        String defaultValue = strFlag.getDefault();
                                                        String currentValue = this.region.region().getFlag(strFlag);

                                                        return DialogInput.text(
                                                                flagName,
                                                                256,
                                                                Component.text(ProtectionGuiUtil.getFlagDisplayName(flag)),
                                                                true,
                                                                LegacyComponentSerializer.legacyAmpersand().serialize(LegacyComponentSerializer.legacySection().deserialize((currentValue != null ? currentValue : (defaultValue != null ? defaultValue : "")))),
                                                                Integer.MAX_VALUE,
                                                                TextDialogInput.MultilineOptions.create(null, null)
                                                        );
                                                    }

                                                    if (flag instanceof IntegerFlag intFlag) {
                                                        Integer defaultValue = intFlag.getDefault();
                                                        Integer currentValue = this.region.region().getFlag(intFlag);

                                                        return DialogInput.numberRange(flagName, 256, Component.text(ProtectionGuiUtil.getFlagDisplayName(flag)), "%s: %s", -1f, 100f, (currentValue != null ? Float.valueOf(currentValue) : (defaultValue != null ? Float.valueOf(defaultValue) : -1)), 1f);
                                                    }
                                                } catch(Throwable t) {
                                                    System.out.println("Failed to construct input object for flag " + flag.getName() + ": " + t.getLocalizedMessage());
                                                }
                                                return null;
                                            })
                                            .filter(input -> input != null)
                                            .sorted(Comparator.comparing(input -> input.key()))
                                            .toList())
                                    .canCloseWithEscape(true)
                                    .build())
                            .type(DialogType.confirmation(
                                    ActionButton.create(Component.text("変更を保存", DefinedTextColor.GREEN), Component.text("この画面で変更したフラグの設定を適用して、保存します"), 128, DialogAction.customClick((response, audience) -> {
                                        com.sk89q.worldguard.WorldGuard.getInstance()
                                                .getFlagRegistry()
                                                .getAll()
                                                .parallelStream()
                                                .forEach(flag -> {
                                                    String flagName = flag.getName().replace('-', '_');
                                                    if (flag instanceof StateFlag stateFlag) {
                                                        String rawValue = response.getText(flagName);
                                                        if (rawValue != null) {
                                                            StateFlag.State oldState = this.region.region().getFlag(stateFlag);
                                                            StateFlag.State state = rawValue.equals("unset") ? null : (rawValue.equals("allow") ? StateFlag.State.ALLOW : StateFlag.State.DENY);
                                                            this.region.region().setFlag(stateFlag, state);
                                                            if (!Objects.equals(oldState, state)) {
                                                                NewMessageUtil.sendMessage(event.getWhoClicked(), Component.empty().append(Component.text("保護領域 " + this.region.getId() + " のフラグ " + ProtectionGuiUtil.getFlagDisplayName(flag) + " を "))
                                                                        .append(ProtectionGuiUtil.getStateFlagValueDisplayName(stateFlag, oldState))
                                                                        .append(Component.text(" から "))
                                                                        .append(ProtectionGuiUtil.getStateFlagValueDisplayName(stateFlag, state))
                                                                        .append(Component.text(" に変更しました")), true);
                                                            }
                                                        }
                                                    }

                                                    if (flag instanceof BooleanFlag boolFlag) {
                                                        String rawValue = response.getText(flagName);
                                                        if (rawValue != null) {
                                                            Boolean oldValue = this.region.region().getFlag(boolFlag);
                                                            Boolean value = rawValue.equals("unset") ? null : (rawValue.equals("true") ? true : false);
                                                            this.region.region().setFlag(boolFlag, value);
                                                            if (!Objects.equals(oldValue, value)) {
                                                                NewMessageUtil.sendMessage(event.getWhoClicked(), Component.empty().append(Component.text("保護領域 " + this.region.getId() + " のフラグ " + ProtectionGuiUtil.getFlagDisplayName(flag) + " を "))
                                                                        .append(ProtectionGuiUtil.getBooleanFlagValueDisplayName(boolFlag, oldValue))
                                                                        .append(Component.text(" から "))
                                                                        .append(ProtectionGuiUtil.getBooleanFlagValueDisplayName(boolFlag, value))
                                                                        .append(Component.text(" に変更しました")), true);
                                                            }
                                                        }
                                                    }

                                                    if (flag instanceof StringFlag strFlag) {
                                                        String rawValue = response.getText(flagName);
                                                        if (rawValue != null) {
                                                            String defaultValue = strFlag.getDefault();

                                                            String oldValue = this.region.region().getFlag(strFlag);
                                                            if (oldValue != null && oldValue.isEmpty()) oldValue = null;

                                                            String value = rawValue.isEmpty() ? null : LegacyComponentSerializer.legacySection().serialize(LegacyComponentSerializer.legacyAmpersand().deserialize(rawValue));
                                                            if (value != null && value.equals(defaultValue)) value = null;

                                                            this.region.region().setFlag(strFlag, value);
                                                            if (!Objects.equals(oldValue, value)) {
                                                                NewMessageUtil.sendMessage(event.getWhoClicked(), Component.empty().append(Component.text("保護領域 " + this.region.getId() + " のフラグ " + ProtectionGuiUtil.getFlagDisplayName(flag) + " を "))
                                                                        .append(Component.text(oldValue == null ? "null" : oldValue))
                                                                        .append(Component.text(" から "))
                                                                        .append(Component.text(value == null ? "null" : value))
                                                                        .append(Component.text(" に変更しました")), true);
                                                            }
                                                        }
                                                    }

                                                    if (flag instanceof IntegerFlag intFlag) {
                                                        Float rawValue = response.getFloat(flagName);
                                                        if (rawValue != null) {
                                                            Integer oldValue = this.region.region().getFlag(intFlag);
                                                            Integer value = rawValue == -1 ? null : (int) ((float) rawValue);
                                                            this.region.region().setFlag(intFlag, value);
                                                            if (!Objects.equals(oldValue, value)) {
                                                                NewMessageUtil.sendMessage(event.getWhoClicked(), Component.empty().append(Component.text("保護領域 " + this.region.getId() + " のフラグ " + ProtectionGuiUtil.getFlagDisplayName(flag) + " を "))
                                                                        .append(Component.text(oldValue == null ? "null" : String.valueOf(oldValue)))
                                                                        .append(Component.text(" から "))
                                                                        .append(Component.text(value == null ? "null" : String.valueOf(value)))
                                                                        .append(Component.text(" に変更しました")), true);
                                                            }
                                                        }
                                                    }
                                                });
                                        this.gui.setGuiState(ProtectionGuiState.REGION_INFORMATION);
                                        this.initialize();
                                        this.gui.getPlayer().openInventory(this.gui.getInventory());
                                    }, ClickCallback.Options.builder().uses(1).build())),
                                    ActionButton.create(Component.text("キャンセル", DefinedTextColor.YELLOW), Component.text("この画面で変更したフラグの設定は破棄して、前の画面に戻ります"), 128, DialogAction.customClick((response, audience) -> {
                                        this.gui.setGuiState(ProtectionGuiState.REGION_INFORMATION);
                                        this.initialize();
                                        this.gui.getPlayer().openInventory(this.gui.getInventory());
                                    }, ClickCallback.Options.builder().uses(1).build())))
                            )));
                }

                // メンバー管理
                case 33 -> {
                    this.clearInventory();
                    this.gui.setGuiState(ProtectionGuiState.MANAGE_MEMBERS);
                    this.gui.setView(new RegionInfoView.RegionMemberManagementView(this));
                }

                // 戻る
                case 45 -> {
                    this.clearInventory();
                    this.gui.setGuiState(ProtectionGuiState.REGIONS);
                    this.gui.setView(this.regionsView);
                    this.regionsView.reloadRegionsOnly();
                    this.regionsView.showRegions(this.regionsView.currentPage);
                }

                // 保護範囲表示
                case 51 -> {
                    if (this.region.region() instanceof ProtectedCuboidRegion region) {
                        this.gui.setGuiState(ProtectionGuiState.WAITING_CALLBACK);
                        this.gui.getPlayer().closeInventory(InventoryCloseEvent.Reason.PLUGIN);

                        ProtectionGuiUtil.startSelectionViewMode(this.gui.getPlayer(), this.region, () -> {
                            WorldEdit.getRegionSelector(this.gui.getPlayer(), this.region.world()).clear();
                            this.gui.setGuiState(ProtectionGuiState.REGION_INFORMATION);
                            this.gui.getPlayer().openInventory(this.gui.getInventory());
                        });
                    }
                }

                // 範囲再設定
                case 52 -> {
                    if (this.region.region() instanceof ProtectedCuboidRegion region) {
                        this.gui.setGuiState(ProtectionGuiState.WAITING_CALLBACK);
                        this.gui.getPlayer().closeInventory(InventoryCloseEvent.Reason.PLUGIN);

                        // TODO 既存の選択範囲を引き継ぎながら、変更可能にする
                        //WorldEdit.getSession((Player) event.getWhoClicked()).setRegionSelector(BukkitAdapter.adapt(event.getWhoClicked().getWorld()), new CuboidRegionSelector(BukkitAdapter.adapt(this.region.world()), region.getMinimumPoint(), region.getMaximumPoint()));
                        ProtectionGuiUtil.startSelectionMode(this.gui.getPlayer(), null, null, null, (result) -> {
                            World world = result.world();
                            BlockVector3 min = result.min();
                            BlockVector3 max = result.max();
                            if (world == null || min == null || max == null) {
                                this.gui.setGuiState(ProtectionGuiState.REGION_INFORMATION);
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
                                    ProtectionGuiUtil.showRegionAreaIntersectedError(intersectedRegions, this, ProtectionGuiState.REGION_INFORMATION);
                                } else {
                                    this.region.regionManager().removeRegion(region.getId());
                                    manager.addRegion(newRegion);
                                    this.clearInventory();
                                    this.gui.setView(new RegionInfoView(this.regionsView, new WorldGuard.WrappedProtectedRegion(result.world(), newRegion)));
                                }

                                WorldEdit.getRegionSelector(this.gui.getPlayer(), result.world()).clear();
                            }

                            this.gui.getPlayer().openInventory(this.gui.getInventory());
                        });
                    } else {
                        ProtectionGuiUtil.showUnsupportedRegionError(this, ProtectionGuiState.REGION_INFORMATION);
                    }
                }

                // 削除
                case 53 -> {
                    this.clearInventory();
                    this.gui.setGuiState(ProtectionGuiState.WAITING_REGION_REMOVE);
                    this.gui.setView(new RegionInfoView.RegionRemoveConfirmView(this));
                }
            }
        }

        private static class RegionMemberManagementView extends ProtectionGuiViewBase {
            public static final ProtectionGuiState STATE = ProtectionGuiState.MANAGE_MEMBERS;
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
                this.gui.getInventory().setItem(12, new ItemStackBuilder(Material.OAK_SIGN)
                        .displayName(Component.text("オーナー一覧", DefinedTextColor.GOLD))
                        .lore(new ArrayList<Component>() {{
                            DefaultDomain owners = regionInfoView.region.region().getOwners();
                            PlayerDomain ownerPlayers = owners.getPlayerDomain();
                            GroupDomain ownerGroups = owners.getGroupDomain();

                            ownerPlayers.getUniqueIds().forEach(uuid -> ProtectionGuiUtil.appendPlayerComponent(this, uuid));
                            ownerGroups.getGroups().forEach(group -> ProtectionGuiUtil.appendGroupComponent(this, group));
                        }}.toArray(new Component[0]))
                        .build());

                this.gui.getInventory().setItem(30, DefinedItemStackBuilders.plus()
                        .displayName(Component.text("追加", DefinedTextColor.GREEN))
                        .build()); // +
                this.gui.getInventory().setItem(39, DefinedItemStackBuilders.minus()
                        .displayName(Component.text("削除"))
                        .build()); // -

                this.gui.getInventory().setItem(14, new ItemStackBuilder(Material.BIRCH_SIGN)
                        .displayName(Component.text("メンバー一覧", DefinedTextColor.GREEN))
                        .lore(new ArrayList<Component>() {{
                            DefaultDomain members = regionInfoView.region.region().getMembers();
                            PlayerDomain memberPlayers = members.getPlayerDomain();
                            GroupDomain memberGroups = members.getGroupDomain();

                            memberPlayers.getUniqueIds().forEach(uuid -> ProtectionGuiUtil.appendPlayerComponent(this, uuid));
                            memberGroups.getGroups().forEach(group -> ProtectionGuiUtil.appendGroupComponent(this, group));
                        }}.toArray(new Component[0]))
                        .build());

                this.gui.getInventory().setItem(32, DefinedItemStackBuilders.plus()
                        .displayName(Component.text("追加", DefinedTextColor.GREEN))
                        .build()); // +

                this.gui.getInventory().setItem(41, DefinedItemStackBuilders.minus()
                        .displayName(Component.text("削除"))
                        .build()); // -

                this.gui.getInventory().setItem(45, DefinedItemStackBuilders.leftArrow()
                        .displayName(Component.text("戻る", DefinedTextColor.GREEN))
                        .build());
            }

            @Override
            public void onClick(InventoryClickEvent event) {
                switch (event.getSlot()) {
                    // Owners
                    case 30 -> {
                        this.clearInventory();
                        this.gui.setView(new PlayerSelectionView(this.gui, true, null, (e) -> {
                            this.gui.setView(this);
                            this.initialize();
                        }, view -> {
                            this.gui.setGuiState(ProtectionGuiState.WAITING_CALLBACK);
                        }, name -> {
                            this.gui.setGuiState(ProtectionGuiState.ERROR);
                            this.gui.setView(new ProtectionGuiErrorView(Component.text("プレイヤーが見つかりません", DefinedTextColor.RED),
                                    Collections.singletonList(Component.text("プレイヤー " + name + " は見つかりませんでした", DefinedTextColor.YELLOW)),
                                    this.gui, this, STATE, null));
                        }, player -> {
                            this.regionInfoView.region.region().getOwners().addPlayer(player.getUniqueId());
                            this.gui.setView(this);
                            this.initialize();
                            if (this.gui.getGuiState() == ProtectionGuiState.WAITING_CALLBACK) {
                                this.gui.getPlayer().openInventory(this.gui.getInventory());
                                this.gui.setGuiState(ProtectionGuiState.MANAGE_MEMBERS);
                            }
                        }));
                    }

                    case 39 -> {
                        this.clearInventory();
                        this.gui.setView(new PlayerSelectionView(
                                this.gui,
                                false,
                                this.regionInfoView.region.region().getOwners().getPlayerDomain()
                                        .getUniqueIds().stream()
                                        .map(Bukkit::getOfflinePlayer).toList(),
                                e -> {
                                    this.gui.setView(this);
                                    this.initialize();
                                },
                                null,
                                null,
                                player -> {
                                    this.regionInfoView.region.region().getOwners().getPlayerDomain().removePlayer(player.getUniqueId());
                                    this.gui.setView(this);
                                    this.initialize();
                                }));
                    }

                    // Members
                    case 32 -> {
                        this.clearInventory();
                        this.gui.setView(new PlayerSelectionView(this.gui, true, null, (e) -> {
                            this.gui.setView(this);
                            this.initialize();
                        }, view -> {
                            this.gui.setGuiState(ProtectionGuiState.WAITING_CALLBACK);
                        }, name -> {
                            this.gui.setGuiState(ProtectionGuiState.ERROR);
                            this.gui.setView(new ProtectionGuiErrorView(Component.text("プレイヤーが見つかりません", DefinedTextColor.RED),
                                    Collections.singletonList(Component.text("プレイヤー " + name + " は見つかりませんでした", DefinedTextColor.YELLOW)),
                                    this.gui, this, STATE, null));
                        }, player -> {
                            this.regionInfoView.region.region().getMembers().addPlayer(player.getUniqueId());
                            this.gui.setView(this);
                            this.initialize();
                            if (this.gui.getGuiState() == ProtectionGuiState.WAITING_CALLBACK) {
                                this.gui.getPlayer().openInventory(this.gui.getInventory());
                                this.gui.setGuiState(ProtectionGuiState.MANAGE_MEMBERS);
                            }
                        }));
                    }

                    case 41 -> {
                        this.clearInventory();
                        this.gui.setView(new PlayerSelectionView(
                                this.gui,
                                false,
                                this.regionInfoView.region.region().getMembers().getPlayerDomain()
                                        .getUniqueIds().stream()
                                        .map(Bukkit::getOfflinePlayer).toList(),
                                e -> {
                                    this.gui.setView(this);
                                    this.initialize();
                                },
                                null,
                                null,
                                player -> {
                                    this.regionInfoView.region.region().getMembers().getPlayerDomain().removePlayer(player.getUniqueId());
                                    this.gui.setView(this);
                                    this.initialize();
                                }));
                    }

                    case 45 -> {
                        this.gui.setGuiState(ProtectionGuiState.REGION_INFORMATION);
                        this.clearInventory();
                        this.gui.setView(this.regionInfoView);
                        this.gui.getView().initialize();
                    }
                }
            }
        }

        private static class RegionRemoveConfirmView extends ProtectionGuiViewBase {
            private final RegionInfoView regionInfoView;

            public RegionRemoveConfirmView(RegionInfoView regionInfoView) {
                super(regionInfoView.gui);
                this.regionInfoView = regionInfoView;
                this.initialize();
            }

            @Override
            public void initialize() {
                this.gui.getInventory().setItem(13, new ItemStackBuilder(Material.PAPER)
                        .displayName(Component.text("本当に保護領域を削除しますか？", Style.style(DefinedTextColor.YELLOW, TextDecoration.ITALIC.withState(false), TextDecoration.BOLD.withState(true))))
                        .lore(Component.text("ワールド: " + this.regionInfoView.region.world().getName()),
                                Component.text("名前: " + this.regionInfoView.region.getId()),
                                Component.text("範囲: ", Style.style(DefinedTextColor.AQUA, TextDecoration.ITALIC.withState(false)))
                                        .append(ProtectionGuiUtil.coordinates2Str(this.regionInfoView.region.region().getMinimumPoint()))
                                        .append(Component.text(" -> "))
                                        .append(ProtectionGuiUtil.coordinates2Str(this.regionInfoView.region.region().getMaximumPoint())))
                        .build());

                this.gui.getInventory().setItem(30, new ItemStackBuilder(Material.LIME_WOOL)
                        .displayName(Component.text("はい", Style.style(DefinedTextColor.GREEN, TextDecoration.ITALIC.withState(false), TextDecoration.BOLD.withState(true))))
                        .build());

                this.gui.getInventory().setItem(32, new ItemStackBuilder(Material.RED_WOOL)
                        .displayName(Component.text("いいえ", Style.style(DefinedTextColor.RED, TextDecoration.ITALIC.withState(false), TextDecoration.BOLD.withState(true))))
                        .build());
            }

            @Override
            public void onClick(InventoryClickEvent event) {
                if (event.getSlot() == 30) {
                    WorldGuard.WrappedProtectedRegion wrapped = this.regionInfoView.region;
                    wrapped.regionManager().removeRegion(wrapped.getFullId());
                    NewMessageUtil.sendMessage(this.gui.getPlayer(), Component.text("保護領域 " + wrapped.getId() + " を削除しました"));
                    this.clearInventory();
                    this.gui.setGuiState(ProtectionGuiState.REGIONS);
                    this.gui.setView(this.regionInfoView.regionsView);
                    this.regionInfoView.regionsView.loadRegions();
                } else if (event.getSlot() == 45 || event.getSlot() == 32) {
                    this.clearInventory();
                    this.gui.setGuiState(ProtectionGuiState.REGION_INFORMATION);
                    this.gui.setView(this.regionInfoView);
                    this.regionInfoView.showInformation();
                }
            }
        }
    }
}
