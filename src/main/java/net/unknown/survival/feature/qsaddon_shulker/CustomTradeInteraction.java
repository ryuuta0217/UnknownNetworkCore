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
import com.ghostchu.quickshop.api.economy.EconomyProvider;
import com.ghostchu.quickshop.api.event.QSHandleChatEvent;
import com.ghostchu.quickshop.api.inventory.InventoryWrapper;
import com.ghostchu.quickshop.api.shop.Info;
import com.ghostchu.quickshop.api.shop.Shop;
import com.ghostchu.quickshop.api.shop.ShopAction;
import com.ghostchu.quickshop.api.shop.ShopManager;
import com.ghostchu.quickshop.api.shop.interaction.InteractionBehavior;
import com.ghostchu.quickshop.api.shop.interaction.InteractionClick;
import com.ghostchu.quickshop.api.shop.interaction.InteractionType;
import com.ghostchu.quickshop.common.util.CommonUtil;
import com.ghostchu.quickshop.shop.SimpleInfo;
import com.ghostchu.quickshop.shop.inventory.BukkitInventoryWrapper;
import com.ghostchu.quickshop.util.Util;
import net.unknown.core.managers.ListenerManager;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class CustomTradeInteraction implements InteractionBehavior {
    private final InteractionBehavior original;

    public CustomTradeInteraction(@Nullable InteractionBehavior original) {
        this.original = original;
    }

    @Override
    public String identifier() {
        return "TRADE_INTERACTION";
    }

    // --- イベントハンドラー (買取ショップ以外は全てオリジナルに即時委譲) ---
    @Override
    public void handle(@NotNull QuickShopAPI plugin, @Nullable Shop shop, @NotNull Player player, @NotNull PlayerInteractEvent event, @NotNull InteractionClick clickType, @Nullable InteractionType interaction) {
        if (shop != null && shop.isBuying() && !shop.isFrozen()) {
            if (sellToShopWithShulker(player, shop)) {
                event.setCancelled(true);
                event.setUseInteractedBlock(Event.Result.DENY);
                event.setUseItemInHand(Event.Result.DENY);
                return;
            }
        }
        if (original != null) original.handle(plugin, shop, player, event, clickType, interaction);
    }

    @Override
    public void handle(@NotNull QuickShopAPI plugin, @Nullable Shop shop, @NotNull Player player, @NotNull PlayerInteractEntityEvent event, @NotNull InteractionClick clickType, @Nullable InteractionType interaction) {
        if (shop != null && shop.isBuying() && !shop.isFrozen()) {
            if (sellToShopWithShulker(player, shop)) {
                event.setCancelled(true);
                return;
            }
        }
        if (original != null) original.handle(plugin, shop, player, event, clickType, interaction);
    }

    @Override
    public void handle(@NotNull QuickShopAPI plugin, @Nullable Shop shop, @NotNull Player player, @NotNull EntityDamageByEntityEvent event, @NotNull InteractionClick clickType, @Nullable InteractionType interaction) {
        if (shop != null && shop.isBuying() && !shop.isFrozen()) {
            if (sellToShopWithShulker(player, shop)) {
                event.setCancelled(true);
                return;
            }
        }
        if (original != null) original.handle(plugin, shop, player, event, clickType, interaction);
    }

    private boolean sellToShopWithShulker(@NotNull Player player, @NotNull Shop shop) {
        QuickShopAPI plugin = QuickShopAPI.getInstance();
        if (!player.hasPermission("quickshop.use")) return false;

        plugin.getShopManager().sendShopInfo(player, shop);
        shop.setSignText(plugin.getTextManager().findRelativeLanguages(player));
        Util.playClickSound(player);
        shop.onClick(player);

        if (shop.getRemainingSpace() == 0) {
            plugin.getTextManager().of(player, "purchase-out-of-space", shop.ownerName()).send();
            return true;
        }

        // シュルカー含めた最大売却可能数を計算
        InventoryWrapper wrapper = new ShulkerBoxInventoryWrapper(new BukkitInventoryWrapper(player.getInventory()));
        int maxCanSell = getMaxSellable(shop, wrapper);
        String allWord = plugin.getConfig().getString("shop.word-for-trade-all-items", "all");

        Info info = new SimpleInfo(shop.bukkitLocation(), ShopAction.PURCHASE_SELL, null, null, shop, false);
        ShopManager.InteractiveManager actions = plugin.getShopManager().getInteractiveManager();
        actions.put(player.getUniqueId(), info);

        // 「X個所持しています...」
        if (shop.isStackingShop()) {
            plugin.getTextManager().of(player, "how-many-sell-stack", shop.getItem().getAmount(), maxCanSell, allWord).send();
        } else {
            plugin.getTextManager().of(player, "how-many-sell", maxCanSell, allWord).send();
        }

        ListenerManager.waitForEvent(
                QSHandleChatEvent.class, false, EventPriority.LOW,
                e -> e.getSender().getUniqueId() != null && e.getSender().getUniqueId().equals(player.getUniqueId()) && actions.get(player.getUniqueId()) == info,
                e -> {
                    Info active = actions.remove(player.getUniqueId());
                    if (active != null && active.getAction().isTrading()) {
                        Util.regionThread(player.getLocation(), () -> handleChatTrade(player, active, e.getMessage()));
                    }
                },
                60L, ListenerManager.TimeType.SECONDS,
                () -> {
                    if (actions.get(player.getUniqueId()) == info) actions.remove(player.getUniqueId());
                }
        );

        return true;
    }

    private void handleChatTrade(@NotNull Player player, @NotNull Info info, @NotNull String message) {
        QuickShopAPI plugin = QuickShopAPI.getInstance();
        Shop shop = plugin.getShopManager().getShop(info.getLocation());
        if (shop == null || !shop.isBuying() || !Util.canBeShop(info.getLocation().getBlock()) || info.hasChanged(shop)) {
            plugin.getTextManager().of(player, "shop-has-changed").send();
            return;
        }

        InventoryWrapper wrapper = new ShulkerBoxInventoryWrapper(new BukkitInventoryWrapper(player.getInventory()));
        String allKeyword = plugin.getConfig().getString("shop.word-for-trade-all-items", "all");
        int amount;

        if (message.equalsIgnoreCase(allKeyword)) {
            amount = getMaxSellable(shop, wrapper);
        } else if (CommonUtil.isInteger(message)) {
            amount = Integer.parseInt(message);
        } else {
            plugin.getTextManager().of(player, "not-a-integer", message).send();
            return;
        }

        if (amount <= 0) {
            plugin.getTextManager().of(player, "you-dont-have-that-many-items", amount, Util.getItemStackName(shop.getItem())).send();
            return;
        }

        EconomyProvider eco = plugin.getEconomyManager().provider();
        if (eco != null) {
            // まるなげ
            plugin.getShopManager().actionBuying(player, wrapper, eco, info, shop, amount);
        }
    }

    // "all" 用
    private int getMaxSellable(@NotNull Shop shop, @NotNull InventoryWrapper wrapper) {
        int itemsHave = Util.countItems(wrapper, shop);
        int shopSpace = shop.isUnlimited() ? Integer.MAX_VALUE : Math.max(0, shop.getRemainingSpace());
        if (shop.isFreeShop() || shop.getPrice() <= 0) return Math.min(itemsHave, shopSpace);

        EconomyProvider eco = QuickShopAPI.getInstance().getEconomyManager().provider();
        double balance = eco != null ? eco.balance(shop.getOwner(), shop.bukkitLocation().getWorld().getName(), shop.getCurrency()).doubleValue() : 0.0;
        int ownerCanAfford = (shop.isUnlimited() && !QuickShopAPI.getInstance().getConfig().getBoolean("shop.pay-unlimited-shop-owners"))
                ? Integer.MAX_VALUE : (int) (balance / shop.getPrice());

        return Math.max(0, Math.min(itemsHave, Math.min(shopSpace, ownerCanAfford)));
    }
}