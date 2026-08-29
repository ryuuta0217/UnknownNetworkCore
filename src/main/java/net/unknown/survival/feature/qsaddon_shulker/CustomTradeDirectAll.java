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
import com.ghostchu.quickshop.api.shop.Info;
import com.ghostchu.quickshop.api.shop.Shop;
import com.ghostchu.quickshop.api.shop.ShopAction;
import com.ghostchu.quickshop.api.shop.ShopManager;
import com.ghostchu.quickshop.api.shop.interaction.InteractionBehavior;
import com.ghostchu.quickshop.api.shop.interaction.InteractionClick;
import com.ghostchu.quickshop.api.shop.interaction.InteractionType;
import com.ghostchu.quickshop.api.shop.permission.BuiltInShopPermission;
import com.ghostchu.quickshop.shop.SimpleInfo;
import com.ghostchu.quickshop.shop.inventory.BukkitInventoryWrapper;
import com.ghostchu.quickshop.util.Util;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.logging.Logger;

/**
 * TRADE_DIRECT_ALL の上書き Behavior
 * shop.isBuying() (売却) の場合にシュルカーボックス対応ラッパーで売却処理を行い、
 * それ以外はオリジナルの Behavior に委譲する
 */
public class CustomTradeDirectAll implements InteractionBehavior {
    private final InteractionBehavior original;
    private final Logger logger;

    public CustomTradeDirectAll(@Nullable InteractionBehavior original, Logger logger) {
        this.original = original;
        this.logger = logger;
    }

    @Override
    public String identifier() {
        return "TRADE_DIRECT_ALL";
    }

    @Override
    public void handle(@NotNull QuickShopAPI plugin, @Nullable Shop shop, @NotNull Player player, @NotNull PlayerInteractEvent event, @NotNull InteractionClick clickType, @Nullable InteractionType interaction) {
        if (shop == null) return;

        // アイテム売却の場合は独自ハンドラへ
        if (shop.isBuying() && !shop.isFrozen()) {
            if (sellToShopWithShulker(player, shop)) {
                event.setCancelled(true);
                event.setUseInteractedBlock(Event.Result.DENY);
                event.setUseItemInHand(Event.Result.DENY);
                return;
            }
        }

        // それ以外はオリジナルへ
        if (original != null) {
            original.handle(plugin, shop, player, event, clickType, interaction);
        }
    }

    @Override
    public void handle(@NotNull QuickShopAPI plugin, @Nullable Shop shop, @NotNull Player player, @NotNull PlayerInteractEntityEvent event, @NotNull InteractionClick clickType, @Nullable InteractionType interaction) {
        if (shop == null) return;

        if (shop.isBuying() && !shop.isFrozen()) {
            if (sellToShopWithShulker(player, shop)) {
                event.setCancelled(true);
                return;
            }
        }

        if (original != null) {
            original.handle(plugin, shop, player, event, clickType, interaction);
        }
    }

    @Override
    public void handle(@NotNull QuickShopAPI plugin, @Nullable Shop shop, @NotNull Player player, @NotNull EntityDamageByEntityEvent event, @NotNull InteractionClick clickType, @Nullable InteractionType interaction) {
        if (shop == null) return;

        if (shop.isBuying() && !shop.isFrozen()) {
            if (sellToShopWithShulker(player, shop)) {
                event.setCancelled(true);
                return;
            }
        }

        if (original != null) {
            original.handle(plugin, shop, player, event, clickType, interaction);
        }
    }

    /**
     * ShopUtil.sellToShop() の処理をシュルカー対応版で再実装
     * {@link com.ghostchu.quickshop.util.ShopUtil#sellToShop(Player, Shop, boolean, boolean)}
     */
    private boolean sellToShopWithShulker(@NotNull Player player, @NotNull Shop shop) {
        QuickShopAPI api = QuickShopAPI.getInstance();
        ShopManager shopManager = api.getShopManager();

        // 権限チェック (オリジナルのコピペ)
        if (!shop.playerAuthorize(player.getUniqueId(), BuiltInShopPermission.PURCHASE) && !player.hasPermission("quickshop.other.use")) {
            return false;
        }
        if (!player.hasPermission("quickshop.use")) {
            return false;
        }

        // ショップ情報表示・クリック音
        shopManager.sendShopInfo(player, shop);
        shop.setSignText(api.getTextManager().findRelativeLanguages(player));
        shop.onClick(player);

        if (shop.getRemainingSpace() == 0) {
            return true; // スペースなしメッセージは shopManager が出すのでここでは何もしない
        }

        EconomyProvider eco = api.getEconomyManager().provider();
        if (eco == null) return false;

        // 重要: ここで処理の迂回 | 自作のInventoryWrapperにすり替える
        BukkitInventoryWrapper rawWrapper = new BukkitInventoryWrapper(player.getInventory());
        ShulkerBoxInventoryWrapper shulkerWrapper = new ShulkerBoxInventoryWrapper(rawWrapper);

        // 売却可能な個数を計算 (シュルカー内も含む合計)
        int totalItems = Util.countItems(shulkerWrapper, shop);
        if (totalItems <= 0) return true;

        // Info オブジェクトを生成して InteractiveManager に登録
        // (actionBuying 内で info.hasChanged(shop) のチェック等に使用される)
        Info info = new SimpleInfo(shop.bukkitLocation(), ShopAction.PURCHASE_SELL, null, null, shop, false);
        shopManager.getInteractiveManager().put(player.getUniqueId(), info);

        // あとはQS APIへ丸投げ
        shopManager.actionBuying(player, shulkerWrapper, eco, info, shop, totalItems);

        logger.fine("[ShulkerAddon] ^^b");
        return true;
    }
}