/*
 * Copyright (c) 2026 Unknown Network Developers and contributors.
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

package net.unknown.survival.feature.qsaddon_shulker;

import com.ghostchu.quickshop.api.QuickShopAPI;
import com.ghostchu.quickshop.api.inventory.*;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Container;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class ShulkerBoxInventoryWrapper implements CountableInventoryWrapper {
    private final InventoryWrapper originalWrapper;

    public ShulkerBoxInventoryWrapper(@NotNull InventoryWrapper originalWrapper) {
        this.originalWrapper = originalWrapper;
    }

    private static boolean isContainer(ItemStack item) {
        return item.hasItemMeta()
                && item.getItemMeta() instanceof BlockStateMeta meta
                && meta.hasBlockState()
                && meta.getBlockState() instanceof Container;
    }

    private static int countInsideContainer(ItemStack containerItem, ItemPredicate predicate) {
        BlockStateMeta meta = (BlockStateMeta) containerItem.getItemMeta();
        Container container = (Container) meta.getBlockState();

        int count = 0;
        for (ItemStack inside : container.getInventory().getContents()) {
            if (inside != null && inside.getType() != Material.AIR && predicate.isMatch(inside)) {
                count += inside.getAmount();
            }
        }
        return count;
    }

    @Override
    public int countItem(@NotNull ItemPredicate predicate) {
        int count = 0;
        for (ItemStack item : originalWrapper) {
            if (item == null || item.getType() == Material.AIR) continue;
            // 通常スロットのアイテムをチェック
            if (predicate.isMatch(item)) {
                count += item.getAmount();
            }
            // シュルカーボックス等コンテナの内部をチェック
            else if (isContainer(item)) {
                count += countInsideContainer(item, predicate);
            }
        }
        return count;
    }

    @Override
    public int countSpace(@NotNull ItemPredicate predicate) {
        // 購入取引ではシュルカーへの自動格納は行わないため、通常の空き容量のみを返す
        if (originalWrapper instanceof CountableInventoryWrapper ciw) {
            return ciw.countSpace(predicate);
        }
        int space = 0;
        for (ItemStack item : originalWrapper) {
            if (item == null || item.getType() == Material.AIR) {
                space += 64;
            } else if (predicate.isMatch(item)) {
                space += Math.max(0, item.getMaxStackSize() - item.getAmount());
            }
        }
        return space;
    }

    @Override
    public @NotNull ItemRemoveResult removeItem(final ItemStack... itemStacks) {
        // プレイヤーインベントリから優先的に取り出す
        ItemRemoveResult result = originalWrapper.removeItem(itemStacks);
        if (result.leftovers().isEmpty()) {
            return result; // 通常スロットだけで足りた場合はここでreturn
        }

        // それ以上のアイテムはシュルカーボックスから取り出す
        Map<Integer, ItemStack> leftovers = new HashMap<>(result.leftovers());
        Map<Integer, ItemStack> totalRemoved = new HashMap<>(result.removed());

        InventoryWrapperIterator iterator = originalWrapper.iterator();
        while (iterator.hasNext() && !leftovers.isEmpty()) {
            ItemStack slotItem = iterator.next();
            if (slotItem == null || !isContainer(slotItem)) continue;

            BlockStateMeta meta = (BlockStateMeta) slotItem.getItemMeta();
            Container container = (Container) meta.getBlockState();

            boolean changed = false;

            Iterator<Map.Entry<Integer, ItemStack>> leftoverIt = leftovers.entrySet().iterator();
            while (leftoverIt.hasNext()) {
                Map.Entry<Integer, ItemStack> entry = leftoverIt.next();

                ItemStack needed = entry.getValue();
                int remaining = needed.getAmount();

                // コンテナ内の各スロットから該当アイテムを取り出す
                for (int slot = 0; slot < container.getInventory().getSize() && remaining > 0; slot++) {
                    ItemStack inside = container.getInventory().getItem(slot);
                    if (inside == null || inside.getType() == Material.AIR) continue;
                    if (!QuickShopAPI.getInstance().getItemMatcher().matches(needed, inside)) continue;

                    int take = Math.min(remaining, inside.getAmount());
                    inside.setAmount(inside.getAmount() - take);

                    if (inside.getAmount() <= 0) {
                        container.getInventory().setItem(slot, null);
                    } else {
                        container.getInventory().setItem(slot, inside);
                    }

                    remaining -= take;
                    changed = true;

                    ItemStack removedCopy = needed.clone();
                    removedCopy.setAmount(take);

                    totalRemoved.merge(entry.getKey(), removedCopy, (a, b) -> {
                        a.setAmount(a.getAmount() + b.getAmount());
                        return a;
                    });
                }

                if (remaining <= 0) {
                    leftoverIt.remove();
                } else {
                    needed.setAmount(remaining);
                }
            }

            if (changed) {
                meta.setBlockState(container);
                slotItem.setItemMeta(meta);
                iterator.setCurrent(slotItem);
            }
        }
        return new ItemRemoveResult(leftovers, totalRemoved);
    }

    @Override
    public @NotNull InventoryWrapperIterator iterator() {
        return originalWrapper.iterator();
    }

    @Override
    public void clear() {
        originalWrapper.clear();
    }

    @Override
    public @NotNull ItemStack[] createSnapshot() {
        return originalWrapper.createSnapshot();
    }

    @Override
    public @NotNull InventoryWrapperManager getWrapperManager() {
        return originalWrapper.getWrapperManager();
    }

    @Override
    public @Nullable InventoryHolder getHolder() {
        return originalWrapper.getHolder();
    }

    @Override
    public @NotNull InventoryWrapperType getInventoryType() {
        return originalWrapper.getInventoryType();
    }

    @Override
    public @Nullable Location getLocation() {
        return originalWrapper.getLocation();
    }

    @Override
    public boolean isValid() {
        return originalWrapper.isValid();
    }

    @Override
    public boolean isNeedUpdate() {
        return originalWrapper.isNeedUpdate();
    }

    @Override
    public boolean restoreSnapshot(@NotNull ItemStack[] snapshot) {
        return originalWrapper.restoreSnapshot(snapshot);
    }

    @Override
    public @NotNull Map<Integer, ItemStack> addItem(ItemStack... itemStacks) {
        return originalWrapper.addItem(itemStacks);
    }

    @Override
    public void setContents(ItemStack[] itemStacks) {
        originalWrapper.setContents(itemStacks);
    }
}