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

package net.unknown.survival.feature.redirector;

import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import net.unknown.core.define.DefinedTextColor;
import net.unknown.survival.data.PlayerData;
import net.unknown.survival.enums.Permissions;
import net.unknown.survival.feature.OpenShulkerBoxInHand;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Tag;
import org.bukkit.block.BlockState;
import org.bukkit.block.ShulkerBox;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockDropItemEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.event.player.PlayerAttemptPickupItemEvent;
import org.bukkit.inventory.CraftingInventory;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class ShulkerBoxRedirector implements Listener {
    private static final Logger LOGGER = LoggerFactory.getLogger("ShulkerBoxRedirector");
    private static final NamespacedKey MARKERS_CONTAINER_KEY = new NamespacedKey("unknown-network", "shulker_box_redirector/markers");

    private static final NamespacedKey REGISTRY_NAMESPACE = new NamespacedKey("survival", "shulker_box_redirector");

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerTriedPickupItem(PlayerAttemptPickupItemEvent event) {
        if (!event.getItem().canPlayerPickup()) return;

        if (Tag.SHULKER_BOXES.isTagged(event.getItem().getItemStack().getType())) {
            // シュルボ in シュルボは絶対に許さない
            return;
        }

        ShulkerBoxRedirectorMode mode = getMode(event.getPlayer());
        if (mode == ShulkerBoxRedirectorMode.DISABLED) return;

        ShulkerBoxSearchOrder order = getSearchOrder(event.getPlayer());

        PlayerInventory playerInventory = event.getPlayer().getInventory();
        List<ItemStack> shulkerBoxes = getShulkerBoxesInOrder(playerInventory, mode, event.getItem().getItemStack(), order);

        if (shulkerBoxes.isEmpty()) return;

        AtomicBoolean inserted = new AtomicBoolean(false); // アイテムが1個でもシュルカーボックスに入ったかどうか

        for (ItemStack shulkerBox : shulkerBoxes) { // シュルカーボックスのループ
            shulkerBox.editMeta(BlockStateMeta.class, blockStateMeta -> {
                BlockState blockStateCopy = blockStateMeta.getBlockState();
                if (blockStateCopy instanceof ShulkerBox boxStateCopy) {
                    Inventory boxInventory = boxStateCopy.getInventory();

                    ItemStack pickupItem = event.getItem().getItemStack();

                    if (availableToInsert(pickupItem, boxInventory)) { // このシュルカーボックスにアイテムを入れられる場合
                        inserted.set(true);
                        HashMap<Integer, ItemStack> overItems = boxInventory.addItem(pickupItem);
                        if (!overItems.isEmpty()) { // 入りきらなかったアイテムがある場合
                            if (overItems.size() != 1) { // それぞれ別のスタックに分割されることはありえない
                                LOGGER.warn("Unexpected number of leftover items when inserting into shulker box: {}", overItems.size());
                                return;
                            }

                            ItemStack leftoverItem = overItems.values().iterator().next();
                            event.getItem().setItemStack(leftoverItem);
                        } else {
                            event.getItem().setItemStack(ItemStack.empty());
                        }
                        blockStateMeta.setBlockState(blockStateCopy);
                    }
                }
            });

            if (event.getItem().getItemStack().isEmpty()) break;
        }

        if(event.getItem().getItemStack().isEmpty()) {
            event.setCancelled(true);
        }

        if (inserted.get()) event.setFlyAtPlayer(true);

        // empty かつ inserted が true の場合、飛来エフェクト及び拾取音は発生しない。
        // ただし、cancelled かつ inserted が true の場合、飛来エフェクト及び拾取音は発生する。(ItemEntityにアイテムが残っている/残っていないに関わらず)
    }

    private static boolean availableToInsert(ItemStack itemStack, Inventory inventory) {
        if (inventory.firstEmpty() != -1) return true;

        int availableToInsertIndex = -1;
        for (int i = 0; i < inventory.getSize(); i++) {
            ItemStack content = inventory.getItem(i);
            if (content != null && content.isSimilar(itemStack) && content.getAmount() < content.getMaxStackSize()) {
                availableToInsertIndex = i;
                break;
            }
        }

        return availableToInsertIndex != -1;
    }

    private static List<ItemStack> getShulkerBoxesInOrder(PlayerInventory inventory, ShulkerBoxRedirectorMode mode, ItemStack pickupItem, ShulkerBoxSearchOrder order) {
        if (mode == ShulkerBoxRedirectorMode.ANYTHING_WHEN_FULL && availableToInsert(pickupItem, inventory)) {
            // インベントリに空きがある場合は、シュルカーボックスの捜索は行わない
            return Collections.emptyList();
        }

        List<ItemStack> shulkerBoxes = IntStream.range(0, inventory.getSize())
                .parallel()
                .mapToObj(slot -> Map.entry(slot, Optional.ofNullable(inventory.getItem(slot))))
                .filter(entry -> entry.getValue().isPresent())
                .filter(entry -> Tag.SHULKER_BOXES.isTagged(entry.getValue().get().getType()))
                .filter(entry -> !OpenShulkerBoxInHand.isOpenedShulkerBox(entry.getValue().get()))
                .filter(entry -> {
                    if (mode == ShulkerBoxRedirectorMode.ANYTHING || mode == ShulkerBoxRedirectorMode.ANYTHING_WHEN_FULL) return true; // 任意のシュルカーボックスを通過
                    else if (mode == ShulkerBoxRedirectorMode.INSERT_MATCHING) { // シュルカーボックスの中身に拾ったアイテムと一致するアイテムがある場合のみ通過
                        ItemStack boxItem = entry.getValue().get();
                        if (boxItem.getItemMeta() instanceof BlockStateMeta blockStateMeta ) {
                            BlockState blockState = blockStateMeta.getBlockState();
                            if (blockState instanceof ShulkerBox box) {
                                if (Arrays.stream(box.getInventory().getContents()).filter(Objects::nonNull).anyMatch(is -> is.isSimilar(pickupItem))) {
                                    // Match found, this box is a candidate for insertion
                                    return true;
                                }
                            }
                        }
                    } else if (mode == ShulkerBoxRedirectorMode.ONLY_MARKED) { // 拾ったアイテムのMarkerが付与されているシュルカーボックスのみ通過
                        ItemStack boxItem = entry.getValue().get();
                        Set<NamespacedKey> markers = getMarkers(boxItem);
                        if (markers.contains(pickupItem.getType().getKey())) {
                            return true;
                        }
                    }
                    return false;
                })
                .sorted(Comparator.comparingInt(Map.Entry::getKey))
                .map(entry -> entry.getValue().get())
                .toList();

        return switch (order) {
            case FIRST_TO_LAST -> shulkerBoxes;
            case LAST_TO_FIRST -> shulkerBoxes.reversed();
            case RANDOM -> {
                List<ItemStack> list = new ArrayList<>(shulkerBoxes);
                Collections.shuffle(list);
                yield list;
            }
        };
    }

    public static ShulkerBoxRedirectorMode getMode(Player player) {
        return ShulkerBoxRedirectorMode.valueOf(PlayerData.of(player).getRegistries().getOrDefault(REGISTRY_NAMESPACE, "mode", ShulkerBoxRedirectorMode.ONLY_MARKED.name()));
    }

    public static void setMode(Player player, ShulkerBoxRedirectorMode mode) {
        PlayerData.of(player).getRegistries().put(REGISTRY_NAMESPACE, "mode", mode.name());
    }

    public static ShulkerBoxSearchOrder getSearchOrder(Player player) {
        return ShulkerBoxSearchOrder.valueOf(PlayerData.of(player).getRegistries().getOrDefault(REGISTRY_NAMESPACE, "order", ShulkerBoxSearchOrder.FIRST_TO_LAST.name()));
    }

    public static void setSearchOrder(Player player, ShulkerBoxSearchOrder order) {
        PlayerData.of(player).getRegistries().put(REGISTRY_NAMESPACE, "order", order.name());
    }

    public static Set<NamespacedKey> getMarkers(ItemStack shulkerBoxes) {
        if (Tag.SHULKER_BOXES.isTagged(shulkerBoxes.getType())) {
            List<String> markers = shulkerBoxes.getPersistentDataContainer().get(MARKERS_CONTAINER_KEY, PersistentDataType.LIST.strings());
            if (markers != null && !markers.isEmpty()) {
                return markers.stream().filter(Key::parseable).map(NamespacedKey::fromString).collect(Collectors.toSet());
            }
        }
        return Collections.emptySet();
    }

    public static void addMarker(ItemStack shulkerBox, Material type) {
        if (Tag.SHULKER_BOXES.isTagged(shulkerBox.getType())) {
            Set<NamespacedKey> existsMarkers = getMarkers(shulkerBox);
            Set<NamespacedKey> newMarkers = new HashSet<>(existsMarkers);
            newMarkers.add(type.getKey());
            shulkerBox.editPersistentDataContainer(container -> {
                container.set(MARKERS_CONTAINER_KEY, PersistentDataType.LIST.strings(), newMarkers.stream().map(NamespacedKey::toString).toList());
            });
            updateMarkerLore(shulkerBox);
        }
    }

    public static void removeMarker(ItemStack shulkerBoxes, Material type) {
        if (Tag.SHULKER_BOXES.isTagged(shulkerBoxes.getType())) {
            Set<NamespacedKey> existsMarkers = getMarkers(shulkerBoxes);
            Set<NamespacedKey> newMarkers = new HashSet<>(existsMarkers);
            newMarkers.remove(type.getKey());
            shulkerBoxes.editPersistentDataContainer(container -> {
                container.set(MARKERS_CONTAINER_KEY, PersistentDataType.LIST.strings(), newMarkers.stream().map(NamespacedKey::toString).toList());
            });
            updateMarkerLore(shulkerBoxes);
        }
    }

    public static void clearMarkers(ItemStack shulkerBox) {
        if (Tag.SHULKER_BOXES.isTagged(shulkerBox.getType())) {
            shulkerBox.editPersistentDataContainer(container -> {
                container.remove(MARKERS_CONTAINER_KEY);
            });
            updateMarkerLore(shulkerBox);
        }
    }

    public static void updateMarkerLore(ItemStack shulkerBox) {
        if (!Tag.SHULKER_BOXES.isTagged(shulkerBox.getType())) return;
        ItemMeta meta = shulkerBox.getItemMeta();
        if (meta == null) return;

        List<Component> lore = meta.hasLore() && meta.lore() != null ? new ArrayList<>(meta.lore()) : new ArrayList<>();
        lore.removeIf(line -> {
            String plain = PlainTextComponentSerializer.plainText().serialize(line);
            return plain.startsWith("[ 自動回収マーカー:") || plain.startsWith("  ▪ ") || plain.startsWith("  ... 他 ");
        });

        Set<NamespacedKey> markers = getMarkers(shulkerBox);
        if (!markers.isEmpty()) {
            lore.add(Component.text("[ 自動回収マーカー: " + markers.size() + "件 ]", DefinedTextColor.GOLD).decoration(TextDecoration.ITALIC, false));
            int count = 0;
            for (NamespacedKey key : markers) {
                if (count >= 5) {
                    lore.add(Component.text("  ... 他 " + (markers.size() - 5) + "件", DefinedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
                    break;
                }
                Material mat = Material.matchMaterial(key.toString());
                if (mat == null && key.getKey() != null) mat = Material.matchMaterial(key.getKey());
                if (mat != null) {
                    lore.add(Component.text("  ▪ ", DefinedTextColor.YELLOW)
                            .append(Component.translatable(mat).color(DefinedTextColor.GREEN))
                            .decoration(TextDecoration.ITALIC, false));
                } else {
                    lore.add(Component.text("  ▪ ", DefinedTextColor.YELLOW)
                            .append(Component.text(key.toString(), DefinedTextColor.GREEN))
                            .decoration(TextDecoration.ITALIC, false));
                }
                count++;
            }
        }

        meta.lore(lore.isEmpty() ? null : lore);
        shulkerBox.setItemMeta(meta);
    }

    /* CRAFTING HANDLERS */

    @EventHandler(priority = EventPriority.NORMAL)
    public void onPrepareItemCraft(PrepareItemCraftEvent event) {
        if (event.getRecipe() != null) return;
        if (!event.getView().getPlayer().hasPermission(Permissions.FEATURE_SHULKER_BOX_REDIRECTOR_CRAFTING.getPermissionNode())) return;

        ItemStack shulkerBox = null;
        ItemStack materialItem = null;
        int ingredientsCount = 0;

        for (ItemStack item : event.getInventory().getMatrix()) {
            if (item != null && !item.getType().isAir()) {
                ingredientsCount++;
                if (Tag.SHULKER_BOXES.isTagged(item.getType())) {
                    if (shulkerBox == null) {
                        shulkerBox = item;
                    } else {
                        event.getInventory().setResult(null);
                        return;
                    }
                } else {
                    if (materialItem == null) {
                        materialItem = item;
                    } else {
                        return;
                    }
                }
            }
        }

        if (ingredientsCount != 2 || shulkerBox == null || materialItem == null) return;

        ItemStack result = shulkerBox.clone();
        result.setAmount(1);
        Material targetType = materialItem.getType();

        if (getMarkers(shulkerBox).contains(targetType.getKey())) {
            removeMarker(result, targetType);
        } else {
            addMarker(result, targetType);
        }

        event.getInventory().setResult(result);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onCraftItem(InventoryClickEvent event) { // CraftItemEvent が呼ばれないので、代わりに InventoryClickEvent を使う
        if (event.getSlotType() != InventoryType.SlotType.RESULT || event.getRawSlot() != 0) return;
        if (!(event.getInventory() instanceof CraftingInventory inventory)) return;
        if (!event.getView().getPlayer().hasPermission(Permissions.FEATURE_SHULKER_BOX_REDIRECTOR_CRAFTING.getPermissionNode())) return;

        ItemStack result = event.getCurrentItem();
        if (result == null || result.getType().isAir() || !Tag.SHULKER_BOXES.isTagged(result.getType())) return; // 結果スロットをクリックして取得したアイテムがシュルカーボックスでない場合は無視する

        int shulkerSlot = -1;
        int materialSlot = -1;
        int ingredientsCount = 0;

        for (int i = 1; i < inventory.getSize(); i++) {
            ItemStack item = inventory.getItem(i);
            if (item != null && !item.getType().isAir()) {
                ingredientsCount++;
                if (Tag.SHULKER_BOXES.isTagged(item.getType())) {
                    shulkerSlot = i;
                } else {
                    materialSlot = i;
                }
            }
        }

        if (ingredientsCount != 2 || shulkerSlot == -1 || materialSlot == -1) return;

        ItemStack inputShulker = inventory.getItem(shulkerSlot);
        if (inputShulker != null && inputShulker.getType() != result.getType()) {
            // 染色の場合は無視する
            return;
        }

        event.setCancelled(true);
        Player player = (Player) event.getView().getPlayer();

        switch (event.getClick()) {
            case SHIFT_LEFT, SHIFT_RIGHT -> {
                HashMap<Integer, ItemStack> leftover = player.getInventory().addItem(result.clone());
                if (!leftover.isEmpty()) return;
            }
            case LEFT, RIGHT -> {
                if (event.getCursor() != null && !event.getCursor().getType().isAir()) return;
                event.getView().setCursor(result.clone());
            }
            case NUMBER_KEY -> {
                int hotbar = event.getHotbarButton();
                if (hotbar < 0 || hotbar > 8) return;
                ItemStack inHotbar = player.getInventory().getItem(hotbar);
                if (inHotbar != null && !inHotbar.getType().isAir()) return;
                player.getInventory().setItem(hotbar, result.clone());
            }
            case DROP, CONTROL_DROP -> {
                player.getWorld().dropItemNaturally(player.getLocation(), result.clone());
            }
            default -> {
                return;
            }
        }

        inventory.setItem(shulkerSlot, null);
        inventory.setResult(null);
        player.updateInventory();
    }

    /* BLOCK EVENTS */

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        if (!Tag.SHULKER_BOXES.isTagged(event.getBlock().getType())) return;
        Set<NamespacedKey> markers = getMarkers(event.getItemInHand());
        if (markers.isEmpty()) return;

        if (event.getBlock().getState(false) instanceof ShulkerBox shulkerBox) {
            List<String> markerStrings = markers.stream().map(NamespacedKey::toString).toList();
            shulkerBox.getPersistentDataContainer().set(MARKERS_CONTAINER_KEY, PersistentDataType.LIST.strings(), markerStrings);
            shulkerBox.update();
        }
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onBlockDropItem(BlockDropItemEvent event) {
        if (!Tag.SHULKER_BOXES.isTagged(event.getBlockState().getType())) return;
        if (event.getBlockState() instanceof ShulkerBox shulkerBox) {
            List<String> markerStrings = shulkerBox.getPersistentDataContainer().get(MARKERS_CONTAINER_KEY, PersistentDataType.LIST.strings());
            if (markerStrings != null && !markerStrings.isEmpty()) {
                for (org.bukkit.entity.Item drop : event.getItems()) {
                    if (Tag.SHULKER_BOXES.isTagged(drop.getItemStack().getType())) {
                        ItemStack itemStack = drop.getItemStack().clone();
                        itemStack.editPersistentDataContainer(container -> {
                            container.set(MARKERS_CONTAINER_KEY, PersistentDataType.LIST.strings(), markerStrings);
                        });
                        updateMarkerLore(itemStack);
                        drop.setItemStack(itemStack);
                    }
                }
            }
        }
    }
}
