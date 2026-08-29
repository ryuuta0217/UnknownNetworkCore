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
 * TRADE_DIRECT の上書き Behavior。
 * 買取ショップでは 1 個だけシュルカー対応売却を行う。
 */
public class CustomTradeDirect implements InteractionBehavior {
    private final InteractionBehavior original;
    private final Logger logger;

    public CustomTradeDirect(@Nullable InteractionBehavior original, Logger logger) {
        this.original = original;
        this.logger = logger;
    }

    @Override
    public String identifier() {
        return "TRADE_DIRECT";
    }

    @Override
    public void handle(@NotNull QuickShopAPI plugin, @Nullable Shop shop, @NotNull Player player, @NotNull PlayerInteractEvent event, @NotNull InteractionClick clickType, @Nullable InteractionType interaction) {
        if (shop == null) return;

        if (shop.isBuying() && !shop.isFrozen()) {
            if (sellOneWithShulker(player, shop)) {
                event.setCancelled(true);
                event.setUseInteractedBlock(Event.Result.DENY);
                event.setUseItemInHand(Event.Result.DENY);
                return;
            }
        }

        if (original != null) {
            original.handle(plugin, shop, player, event, clickType, interaction);
        }
    }

    @Override
    public void handle(@NotNull QuickShopAPI api, @Nullable Shop shop, @NotNull Player player, @NotNull PlayerInteractEntityEvent interactEvent, @NotNull InteractionClick clickType, @Nullable InteractionType interaction) {
        if (shop != null && shop.isBuying() && !shop.isFrozen() && sellOneWithShulker(player, shop)) {
            interactEvent.setCancelled(true);
            return;
        }

        if (original != null) original.handle(api, shop, player, interactEvent, clickType, interaction);
    }

    @Override
    public void handle(@NotNull QuickShopAPI api, @Nullable Shop shop, @NotNull Player player, @NotNull EntityDamageByEntityEvent damageEvent, @NotNull InteractionClick clickType, @Nullable InteractionType interaction) {
        if (shop != null && shop.isBuying() && !shop.isFrozen() && sellOneWithShulker(player, shop)) {
            damageEvent.setCancelled(true);
            return;
        }

        if (original != null) original.handle(api, shop, player, damageEvent, clickType, interaction);
    }

    private boolean sellOneWithShulker(@NotNull Player player, @NotNull Shop shop) {
        QuickShopAPI api = QuickShopAPI.getInstance();
        ShopManager sm = api.getShopManager();

        if (!shop.playerAuthorize(player.getUniqueId(), BuiltInShopPermission.PURCHASE) && !player.hasPermission("quickshop.other.use"))
            return false;
        if (!player.hasPermission("quickshop.use")) return false;

        sm.sendShopInfo(player, shop);
        shop.setSignText(api.getTextManager().findRelativeLanguages(player));
        shop.onClick(player);

        if (shop.getRemainingSpace() == 0) return true;

        EconomyProvider eco = api.getEconomyManager().provider();
        if (eco == null) return false;

        BukkitInventoryWrapper raw = new BukkitInventoryWrapper(player.getInventory());
        ShulkerBoxInventoryWrapper wrapper = new ShulkerBoxInventoryWrapper(raw);

        // シュルカー含めて1個以上持っているか
        int have = Util.countItems(wrapper, shop);
        if (have <= 0) return true;

        Info info = new SimpleInfo(shop.bukkitLocation(), ShopAction.PURCHASE_SELL, null, null, shop, false);

        sm.getInteractiveManager().put(player.getUniqueId(), info);
        sm.actionBuying(player, wrapper, eco, info, shop, 1);
        return true;
    }
}
