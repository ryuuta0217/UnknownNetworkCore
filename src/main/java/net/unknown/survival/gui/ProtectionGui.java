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
import com.sk89q.worldedit.LocalSession;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.bukkit.WorldEditPlugin;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.regions.RegionSelector;
import com.sk89q.worldedit.regions.selector.CuboidRegionSelector;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedCuboidRegion;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import net.md_5.bungee.api.chat.Keybinds;
import net.unknown.UnknownNetworkCore;
import net.unknown.core.builder.ItemStackBuilder;
import net.unknown.core.define.DefinedItemStackBuilders;
import net.unknown.core.define.DefinedTextColor;
import net.unknown.core.gui.GuiBase;
import net.unknown.core.gui.SignGui;
import net.unknown.core.managers.RunnableManager;
import net.unknown.core.util.MessageUtil;
import net.unknown.core.util.NewMessageUtil;
import net.unknown.survival.dependency.WorldEdit;
import net.unknown.survival.dependency.WorldGuard;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.*;
import org.bukkit.event.server.ServerLoadEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
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
    private ViewBase view;

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
        if(this.guiState == State.NEW_REGION) {
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
            this.clearRegions();
            this.regions.get(this.currentPage - 1).forEach(region -> {
                int toAddSlot = this.gui.inventory.firstEmpty();
                this.gui.inventory.setItem(toAddSlot, new ItemStackBuilder(Material.LIME_WOOL)
                        .displayName(Component.text(region.getId()))
                        .lore(Component.text("", Style.style(TextColor.color(0xFFFFFF), TextDecoration.ITALIC.withState(false)))
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
        public void initialize() {
            this.loadRegions();
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

                if(this.newRegionName != null && this.min != null && this.max != null) {
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
            protected void clearInventory() {
                super.clearInventory();
                this.gui.inventory.setItem(49, null);
            }

            @Override
            public void onClick(InventoryClickEvent event) {
                if (event.getSlot() == 30) {
                    this.gui.guiState = State.WAITING_CALLBACK;
                    this.gui.player.closeInventory(InventoryCloseEvent.Reason.PLUGIN);
                    new SignGui().withTarget((Player) event.getWhoClicked())
                            .withLines(Component.text(this.newRegionName),
                                    Component.text("^^^^^^^^^^^^^^"),
                                    Component.text("保護領域名を入力"),
                                    Component.empty())
                            .onComplete(lines -> {
                                this.newRegionName = PlainTextComponentSerializer.plainText().serialize(lines.get(0));
                                this.clearInventory();
                                this.initialize();
                                this.gui.player.openInventory(this.gui.getInventory());
                            }).open();
                } else if (event.getSlot() == 32) {
                    ItemStack currentHand = this.gui.player.getInventory().getItemInMainHand();
                    ItemStack newHand = new ItemStackBuilder(Material.GOLDEN_AXE)
                            .displayName(Component.text("範囲選択斧", DefinedTextColor.GOLD))
                            .addEnchantment(Enchantment.DIG_SPEED, 1)
                            .custom(is -> is.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ENCHANTS))
                            .build();
                    this.gui.player.getInventory().setItem(this.gui.player.getInventory().getHeldItemSlot(), newHand);
                    this.gui.guiState = State.WAITING_CALLBACK;
                    this.gui.player.closeInventory(InventoryCloseEvent.Reason.PLUGIN);

                    NewMessageUtil.sendMessage(this.gui.player, Component.empty()
                            .append(Component.keybind("key.drop"))
                            .append(Component.text(" "))
                            .append(Component.text("を押して、範囲選択用の金の斧を投げると範囲選択を終了します。")), false);

                    BukkitTask task = RunnableManager.runAsyncRepeating(() -> {
                        this.gui.player.sendActionBar(Component.text("≪範囲選択モードが有効です≫").style(Style.style(DefinedTextColor.GOLD, TextDecoration.BOLD.withState(true))));
                    }, 0L, 3L);

                    Bukkit.getPluginManager().registerEvent(PlayerInteractEvent.class, createListener(), EventPriority.MONITOR, (l, ev) -> {
                        if (ev instanceof PlayerInteractEvent e) {
                            if (!e.getPlayer().equals(this.gui.player)) return;
                            if (e.getHand() != EquipmentSlot.HAND) return;

                            if (e.getItem() == null || e.getItem().getType() != Material.GOLDEN_AXE) return;
                            if (e.getItem().getItemMeta() == null || e.getItem().getItemMeta().displayName() == null) return;
                            if (!e.getItem().getEnchantments().containsKey(Enchantment.DIG_SPEED)) return;

                            if (e.getClickedBlock() == null || !e.hasBlock()) return;
                            if (e.getAction() != Action.LEFT_CLICK_BLOCK && e.getAction() != Action.RIGHT_CLICK_BLOCK) return;

                            e.setCancelled(true);
                            Location clickedLoc = e.getClickedBlock().getLocation();
                            BlockVector3 clickedLocConv = BlockVector3.at(clickedLoc.getX(), clickedLoc.getY(), clickedLoc.getZ());
                            if(this.world != null && !clickedLoc.getWorld().equals(this.world)) {
                                this.min = null;
                                this.max = null;
                                NewMessageUtil.sendMessage(this.gui.player, Component.text("ワールドが変更されたようです。選択範囲がリセットされました。"));
                            } else {
                                this.world = clickedLoc.getWorld();
                            }

                            if (e.getAction() == Action.LEFT_CLICK_BLOCK) {
                                // #1
                                this.min = clickedLocConv;
                                NewMessageUtil.sendMessage(this.gui.player, Component.text("地点 #1 を ")
                                        .append(coordinates2Str(this.min))
                                        .append(Component.text(" "))
                                        .append(Component.text(" に設定しました")));
                            } else {
                                // #2
                                this.max = clickedLocConv;
                                NewMessageUtil.sendMessage(this.gui.player, Component.text("地点 #2 を ")
                                        .append(coordinates2Str(this.max))
                                        .append(Component.text(" "))
                                        .append(Component.text(" に設定しました")));
                            }

                            if (this.min != null && this.max != null) {
                                NewMessageUtil.sendMessage(this.gui.player, Component.text("両地点を選択しました。"), false);
                                LocalSession session = WorldEdit.getSession(this.gui.player);
                                RegionSelector selector = WorldEdit.getRegionSelector(this.gui.player, this.world);
                                if(!(selector instanceof CuboidRegionSelector)) {
                                    selector = new CuboidRegionSelector();
                                    session.setRegionSelector(BukkitAdapter.adapt(this.world), selector);
                                }
                                selector.selectPrimary(this.min, null);
                                selector.selectSecondary(this.max, null);
                            }
                        }
                    }, UnknownNetworkCore.getInstance(), false);

                    Bukkit.getPluginManager().registerEvent(PlayerDropItemEvent.class, createListener(), EventPriority.MONITOR, (l, ev) -> {
                        if(ev instanceof PlayerDropItemEvent e) {
                            if(!e.getPlayer().equals(this.gui.player)) return;
                            if(e.getItemDrop().getItemStack().getType() != Material.GOLDEN_AXE) return;
                            if(e.getItemDrop().getItemStack().getItemMeta() == null || e.getItemDrop().getItemStack().getItemMeta().displayName() == null) return;
                            if(!e.getItemDrop().getItemStack().getEnchantments().containsKey(Enchantment.DIG_SPEED)) return;
                            this.gui.player.getInventory().setItem(this.gui.player.getInventory().getHeldItemSlot(), currentHand);
                            this.unregisterAllListeners();
                            if(!task.isCancelled()) task.cancel();
                            this.gui.guiState = State.NEW_REGION;
                            this.initialize();
                            this.gui.player.openInventory(this.gui.getInventory());
                            e.getItemDrop().remove();
                        }
                    }, UnknownNetworkCore.getInstance(), false);

                    Bukkit.getPluginManager().registerEvent(PlayerQuitEvent.class, createListener(), EventPriority.MONITOR, (l, ev) -> {
                        if(ev instanceof PlayerQuitEvent e) {
                            if(e.getPlayer().equals(this.gui.player)) {
                                this.gui.player.getInventory().setItem(this.gui.player.getInventory().getHeldItemSlot(), currentHand);
                                this.unregisterAllListeners();
                                if(!task.isCancelled()) task.cancel();
                            }
                        }
                    }, UnknownNetworkCore.getInstance(), false);
                    Bukkit.getPluginManager().registerEvent(PlayerSwapHandItemsEvent.class, createListener(), EventPriority.LOWEST, (l, ev) -> {
                        if(ev instanceof PlayerSwapHandItemsEvent e) {
                            if(e.getPlayer().equals(this.gui.player)) {
                                e.setCancelled(true);
                            }
                        }
                    }, UnknownNetworkCore.getInstance(), false);
                } else if (event.getSlot() == 45) {
                    /* 保護領域一覧に戻る */
                    this.gui.guiState = State.REGIONS;
                    this.gui.view = this.regionsView;
                    this.clearInventory();
                    this.regionsView.loadRegions();
                    this.regionsView.showRegions(1);
                } else if (event.getSlot() == 49 && event.getCurrentItem() != null) {
                    /* 保護領域作成確定 */
                    if(this.newRegionName != null && this.min != null && this.max != null) {
                        ProtectedCuboidRegion region = new ProtectedCuboidRegion(this.gui.player.getUniqueId() + WorldGuard.SPLITTER + this.newRegionName, false, this.min, this.max);
                        region.getOwners().addPlayer(this.gui.player.getUniqueId());

                        RegionManager manager = WorldGuard.getPlatform().getRegionContainer().get(BukkitAdapter.adapt(this.world));
                        WorldEdit.getRegionSelector(this.gui.player, this.world).clear();

                        // 保護領域重複チェック
                        List<ProtectedRegion> intersectedRegions = region.getIntersectingRegions(manager.getRegions().values());
                        if(intersectedRegions.size() > 0) {
                            long createdRegionIntersects = intersectedRegions.stream().filter(intersectedRegion -> {
                                String id = intersectedRegion.getId();
                                return (WorldGuard.ID_PATTERN.matcher(id).matches() && UUID.fromString(id.split(WorldGuard.SPLITTER, 2)[0]).equals(this.gui.player.getUniqueId()));
                            }).count();

                            long ownedRegionIntersects = intersectedRegions.stream().filter(intersectedRegion -> {
                                return intersectedRegion.getOwners().contains(this.gui.player.getUniqueId());
                            }).count();

                            long otherRegionIntersects = intersectedRegions.size() - createdRegionIntersects - ownedRegionIntersects;
                            // TODO 選択範囲の重複の種類 (あなたの作成した保護領域と... | あなたが管理している保護領域と... | 他人の保護領域と...)
                            this.clearInventory();
                            this.gui.guiState = State.ERROR;
                            this.gui.view = new ErrorView(Component.text("エラーが発生しました", Style.style(DefinedTextColor.RED, TextDecoration.BOLD.withState(true), TextDecoration.UNDERLINED.withState(true))),
                                    Arrays.asList(
                                            Component.text("保護領域の範囲が重複しています。", DefinedTextColor.YELLOW),
                                            Component.text("範囲を再選択してください。", DefinedTextColor.YELLOW),
                                            Component.text("重複している保護領域の数: " + intersectedRegions.size(), DefinedTextColor.YELLOW)
                                    ), this, State.NEW_REGION, null);
                            this.min = null;
                            this.max = null;
                            WorldEdit.getRegionSelector(this.gui.player, this.world).clear();
                        } else {
                            manager.addRegion(region);
                            NewMessageUtil.sendMessage(this.gui.player, Component.text("ワールド " + MessageUtil.getWorldName(this.world) + " に新しい保護領域 " + this.newRegionName + " を作成しました！"));
                            this.clearInventory();
                            this.gui.guiState = State.REGIONS;
                            this.gui.view = this.regionsView;
                            this.regionsView.reloadRegionsOnly();
                            this.regionsView.showRegions(this.regionsView.regions.size());
                        }
                    }
                }
            }

            private final Set<Listener> listeners = new HashSet<>();

            private Listener createListener() {
                Listener listener = new Listener() {};
                listeners.add(listener);
                return listener;
            }

            private void unregisterAllListeners() {
                listeners.forEach(HandlerList::unregisterAll);
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
                        .build());

                this.gui.inventory.setItem(31, new ItemStackBuilder(Material.SHIELD)
                        .displayName(Component.text("フラグの設定", Style.style(DefinedTextColor.YELLOW, TextDecoration.ITALIC.withState(false))))
                        .build());

                this.gui.inventory.setItem(33, new ItemStackBuilder(Material.DIAMOND_CHESTPLATE)
                        .displayName(Component.text("メンバーの管理", Style.style(DefinedTextColor.LIGHT_PURPLE, TextDecoration.ITALIC.withState(false))))
                        .addItemFlag(ItemFlag.HIDE_ATTRIBUTES)
                        .build());

                this.gui.inventory.setItem(53, new ItemStackBuilder(Material.LAVA_BUCKET)
                        .displayName(Component.text("保護領域の削除", Style.style(DefinedTextColor.RED, TextDecoration.ITALIC.withState(false), TextDecoration.BOLD.withState(true))))
                        .build());
            }

            private void clearInformation() {
                super.clearInventory();
                this.gui.inventory.clear(53);
            }

            @Override
            public void initialize() {
                this.showInformation();
            }

            @Override
            public void onClick(InventoryClickEvent event) {
                if (event.getSlot() == 29) {
                    if(this.region.region() instanceof ProtectedCuboidRegion) {
                        this.gui.guiState = State.WAITING_CALLBACK;
                        this.gui.player.closeInventory(InventoryCloseEvent.Reason.PLUGIN);
                        String oldFullId = this.region.getFullId();
                        String oldId = this.region.getId();
                        new SignGui()
                                .withTarget(this.gui.player)
                                .withLines(Component.text(this.region.getId()),
                                        Component.text("^^^^^^^^^^^^^^^^^^^^^^"),
                                        Component.text("新しい名前を入力"), null)
                                .withSignType(Material.BIRCH_SIGN)
                                .onComplete(lines -> {
                                    String newId = PlainTextComponentSerializer.plainText().serialize(lines.get(0));
                                    String newFullId = this.gui.player.getUniqueId() + WorldGuard.SPLITTER + newId;
                                    if(newFullId.equals(oldFullId)) {
                                        this.gui.player.openInventory(this.gui.getInventory());
                                        this.gui.guiState = State.REGION_INFORMATION;
                                    } else {
                                        if(!this.region.regionManager().hasRegion(newFullId)) {
                                            ProtectedRegion renamed = new ProtectedCuboidRegion(newFullId,
                                                    false,
                                                    this.region.region().getMinimumPoint(),
                                                    this.region.region().getMaximumPoint());

                                            renamed.copyFrom(this.region.region()); // Inherit data from Old Region
                                            this.region.regionManager().removeRegion(oldFullId); // Remove Old Region
                                            this.region.regionManager().addRegion(renamed); // Create New Renamed Region
                                            this.gui.player.openInventory(this.gui.getInventory());
                                            this.clearInventory();
                                            this.gui.guiState = State.REGION_INFORMATION;
                                            NewMessageUtil.sendMessage(this.gui.player, Component.text("保護領域の名前を " + oldId + " から " + newId + " に変更しました"));
                                            this.gui.view = new RegionInfoView(this.regionsView,
                                                    new WorldGuard.WrappedProtectedRegion(this.region.world(), this.region.regionManager(), renamed));
                                        } else {
                                            this.gui.player.openInventory(this.gui.getInventory());
                                            this.clearInventory();
                                            this.gui.guiState = State.ERROR;
                                            this.gui.view = new ErrorView(Component.text("保護領域名 " + newId + " は既に使用されています。", DefinedTextColor.RED),
                                                    Arrays.asList(Component.text("ほかの保護領域名を検討してください。", DefinedTextColor.YELLOW),
                                                            Component.text("ワールド: " + this.region.world().getName())), this.gui.view, State.REGION_INFORMATION, null);
                                        }
                                    }
                                }).open();
                    } else {
                        this.clearInventory();
                        this.gui.guiState = State.ERROR;
                        this.gui.view = new ErrorView(Component.text("サポートされていない保護領域です。", DefinedTextColor.RED),
                                Collections.singletonList(Component.text("運営にお問い合わせください。", DefinedTextColor.YELLOW)), this.gui.view, State.REGION_INFORMATION, null);
                    }
                } else if (event.getSlot() == 45) {
                    this.clearInformation();
                    this.gui.guiState = State.REGIONS;
                    this.gui.view = this.regionsView;
                    this.regionsView.reloadRegionsOnly();
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
                        super.clearInventory();
                        this.gui.guiState = State.REGIONS;
                        this.gui.view = this.regionInfoView.regionsView;
                        this.regionInfoView.regionsView.loadRegions();
                    } else if (event.getSlot() == 45 || event.getSlot() == 32) {
                        super.clearInventory();
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
        private final ViewBase prevView;
        private final State prevState;
        private final Consumer<Inventory> initializer;
        public ErrorView(@Nullable Component errorTitle,
                         @Nullable List<Component> errorDetail,
                         @Nonnull ViewBase prevView,
                         @Nonnull State prevState,
                         @Nullable Consumer<Inventory> initializer) {
            super(prevView.gui);
            this.errorTitle = errorTitle;
            this.errorDetail = errorDetail;
            this.prevView = prevView;
            this.prevState = prevState;
            this.initializer = initializer;
            this.showError();
        }

        private void showError() {
            if(this.initializer != null) this.initializer.accept(this.gui.inventory);
            IntStream.rangeClosed(0, 44).forEach(i -> {
                this.gui.inventory.setItem(i, new ItemStackBuilder(Material.BARRIER).displayName(Component.empty()).build());
            });
            if(this.errorTitle == null) return;
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

        private void clear() {
            super.clearInventory();
        }

        @Override
        public void initialize() {
            this.showError();
        }

        @Override
        public void onClick(InventoryClickEvent event) {
            if(event.getSlot() == 45) {
                this.clear();
                this.gui.guiState = prevState;
                this.gui.view = prevView;
                this.prevView.initialize();
            }
        }
    }

    private static abstract class ViewBase {
        protected final ProtectionGui gui;

        public ViewBase(ProtectionGui gui) {
            this.gui = gui;
        }

        public abstract void initialize();

        public abstract void onClick(InventoryClickEvent event);

        protected void clearInventory() {
            IntStream.rangeClosed(0, 44).forEach(this.gui.inventory::clear);
        }
    }
}
