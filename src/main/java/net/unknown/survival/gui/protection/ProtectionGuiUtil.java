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

package net.unknown.survival.gui.protection;

import com.sk89q.worldedit.LocalSession;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.RegionSelector;
import com.sk89q.worldedit.regions.selector.CuboidRegionSelector;
import com.sk89q.worldguard.protection.flags.BooleanFlag;
import com.sk89q.worldguard.protection.flags.Flag;
import com.sk89q.worldguard.protection.flags.Flags;
import com.sk89q.worldguard.protection.flags.StateFlag;
import com.sk89q.worldguard.protection.regions.ProtectedCuboidRegion;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import net.unknown.core.builder.ItemStackBuilder;
import net.unknown.core.define.DefinedTextColor;
import net.unknown.core.gui.SignGui;
import net.unknown.core.managers.ListenerManager;
import net.unknown.core.managers.RunnableManager;
import net.unknown.core.util.NewMessageUtil;
import net.unknown.survival.dependency.WorldEdit;
import net.unknown.survival.dependency.WorldGuard;
import net.unknown.survival.gui.protection.view.ProtectionGuiErrorView;
import net.unknown.survival.gui.protection.view.ProtectionGuiViewBase;
import org.bukkit.*;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;
import java.util.function.Consumer;

public class ProtectionGuiUtil {
    public static void openSetNameWindow(@Nullable String regionName, ProtectionGuiViewBase view, ProtectionGuiState state, Consumer<String> onComplete) {
        new SignGui().withTarget(view.getGui().getPlayer())
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
                        view.getGui().setGuiState(ProtectionGuiState.ERROR);
                        view.getGui().setView(new ProtectionGuiErrorView(Component.text("使用できない名前です", DefinedTextColor.RED),
                                Arrays.asList(Component.text("保護領域名に使用できない名前が含まれています", DefinedTextColor.YELLOW),
                                        Component.text("使用できる文字はA-Za-z0-9_,'-+/です。", DefinedTextColor.YELLOW)),
                                view.getGui(), view, state, null));
                    }
                    view.getGui().getPlayer().openInventory(view.getGui().getInventory());
                }).open();
    }

    public static void startSelectionMode(Player player, World initialWorld, BlockVector3 initialMin, BlockVector3 initialMax, Consumer<SelectionResult> onComplete) {
        Set<Listener> listeners = new HashSet<>();

        SelectionResult result = new SelectionResult(initialWorld, initialMin, initialMax);

        ItemStack currentHand = player.getInventory().getItemInMainHand();
        ItemStack newHand = new ItemStackBuilder(Material.GOLDEN_AXE)
                .displayName(Component.text("範囲選択斧", DefinedTextColor.GOLD))
                .addEnchantment(Enchantment.EFFICIENCY, 1)
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
                if (!e.getItem().getEnchantments().containsKey(Enchantment.EFFICIENCY)) return;

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
                if (!e.getItemDrop().getItemStack().getEnchantments().containsKey(Enchantment.EFFICIENCY)) return;
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
                .addEnchantment(Enchantment.PROJECTILE_PROTECTION, 1)
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
                if (!toDropItemEnchants.containsKey(Enchantment.PROJECTILE_PROTECTION)) return;
                if (toDropItemEnchants.get(Enchantment.PROJECTILE_PROTECTION) != 1) return;
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

    public static void showUnsupportedRegionError(ProtectionGuiViewBase view, ProtectionGuiState state) {
        view.clearInventory();
        view.getGui().setGuiState(ProtectionGuiState.ERROR);
        view.getGui().setView(new ProtectionGuiErrorView(Component.text("サポートされていない保護領域です。", DefinedTextColor.RED),
                Collections.singletonList(Component.text("運営にお問い合わせください。", DefinedTextColor.YELLOW)),
                view.getGui(), view, state, null));
    }

    public static void showRegionAreaIntersectedError(List<ProtectedRegion> intersectedRegions, ProtectionGuiViewBase view, ProtectionGuiState state) {
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
        view.getGui().setGuiState(ProtectionGuiState.ERROR);
        view.getGui().setView(new ProtectionGuiErrorView(Component.text("エラーが発生しました", Style.style(DefinedTextColor.RED, TextDecoration.BOLD.withState(true), TextDecoration.UNDERLINED.withState(true))),
                Arrays.asList(
                        Component.text("保護範囲が重複しています。", DefinedTextColor.YELLOW),
                        Component.text("範囲を再選択してください。", DefinedTextColor.YELLOW),
                        Component.text("重複している保護領域の数: " + intersectedRegions.size(), DefinedTextColor.YELLOW)
                ), view.getGui(), view, state, null));
    }

    public static Component coordinates2Str(BlockVector3 vec3) {
        return Component.translatable("chat.coordinates", TextColor.color(0xFFFF))
                .args(Component.text(vec3.getX()), Component.text(vec3.getY()), Component.text(vec3.getZ()));
    }

    public static Component getStateFlagValueDisplayName(@Nonnull StateFlag stateFlag, @Nullable StateFlag.State state) {
        String text;
        TextColor color;
        if (state != null) {
            text = state == StateFlag.State.ALLOW ? "許可" : "拒否";
            color = state == StateFlag.State.ALLOW ? DefinedTextColor.GREEN : DefinedTextColor.RED;
        } else {
            if (stateFlag.getDefault() == StateFlag.State.ALLOW) {
                text = "許可 (デフォルト)";
                color = DefinedTextColor.YELLOW;
            } else {
                text = "拒否 (デフォルト)";
                color = DefinedTextColor.LIGHT_PURPLE;
            }
        }

        return Component.text(text, color);
    }

    public static Component getBooleanFlagValueDisplayName(@Nonnull BooleanFlag boolFlag, @Nullable Boolean bool) {
        if (bool == null) return Component.text("未設定", DefinedTextColor.YELLOW);
        if (bool) return Component.text("有効", DefinedTextColor.GREEN);
        else return Component.text("無効", DefinedTextColor.RED);
    }

    public static String getFlagDisplayName(Flag<?> flag) {
        HashMap<Flag<?>, String> names = new HashMap<>() {{
            put(Flags.BLOCK_BREAK, "ブロックの破壊");
            put(Flags.BLOCK_PLACE, "ブロックの設置");
        }};

        return names.getOrDefault(flag, flag.getName());
    }

    public static class SelectionResult {
        private World world = null;
        private BlockVector3 min = null;
        private BlockVector3 max = null;

        public SelectionResult() {
        }

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
