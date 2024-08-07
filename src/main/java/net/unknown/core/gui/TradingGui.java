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

package net.unknown.core.gui;

import io.papermc.paper.event.player.PlayerPurchaseEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MerchantMenu;
import net.minecraft.world.item.trading.Merchant;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import net.unknown.core.util.MinecraftAdapter;
import net.unknown.core.util.NewMessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.SoundGroup;
import org.bukkit.craftbukkit.CraftSound;
import org.bukkit.craftbukkit.inventory.CraftInventoryMerchant;
import org.bukkit.craftbukkit.inventory.CraftMerchant;
import org.bukkit.craftbukkit.inventory.CraftMerchantCustom;
import org.bukkit.craftbukkit.inventory.CraftMerchantRecipe;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.MerchantInventory;
import org.bukkit.inventory.MerchantRecipe;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.OptionalInt;

public class TradingGui {
    protected org.bukkit.inventory.Merchant merchant;
    protected int level;
    protected net.kyori.adventure.text.Component title;

    protected TradingGui(org.bukkit.inventory.Merchant merchant, int level, net.kyori.adventure.text.Component title) {
        this.merchant = merchant;
        this.level = level;
        this.title = title;
    }

    @Nullable
    public InventoryView open(org.bukkit.entity.Player player, boolean force) {
        if (!force && this.merchant.isTrading()) return null;
        else if (this.merchant.isTrading()) this.merchant.getTrader().closeInventory();

        Merchant minecraftMerchant = ((CraftMerchant) this.merchant).getMerchant();
        minecraftMerchant.setTradingPlayer(MinecraftAdapter.player(player));
        minecraftMerchant.openTradingScreen(MinecraftAdapter.player(player), NewMessageUtil.convertAdventure2Minecraft(this.title), this.level);

        return MinecraftAdapter.player(player).containerMenu.getBukkitView();
    }

    public Builder asBuilder() {
        return new Builder(this.merchant);
    }

    public Builder asCopyBuilder() {
        if (this.merchant != null && this.merchant instanceof CraftMerchant craftMerchant && craftMerchant.getMerchant() instanceof Merchant customMerchant) {
            return new Builder(customMerchant.getOffers(), this.title, this.level, customMerchant.getVillagerXp(),customMerchant.showProgressBar(), CraftSound.minecraftToBukkit(customMerchant.getNotifyTradeSound()), customMerchant.canRestock());
        }
        return null;
    }

    public static class Builder {
        private final List<MerchantRecipe> trades = new ArrayList<>();
        private net.kyori.adventure.text.Component title = net.kyori.adventure.text.Component.translatable("entity.minecraft.villager");
        private int level = 1;
        private int villagerXp = 0;
        private boolean showProgressBar = false;
        private Sound notifyTradeSound = Sound.ENTITY_EXPERIENCE_ORB_PICKUP;
        private boolean canRestock = false;

        private org.bukkit.inventory.Merchant merchant;

        public Builder() {}

        protected Builder(org.bukkit.inventory.Merchant merchant) {
            this.merchant = merchant;
        }

        protected Builder(List<MerchantOffer> trades, net.kyori.adventure.text.Component title, int level, int villagerXp, boolean showProgressBar, Sound notifyTradeSound, boolean canRestock) {
            this.trades.addAll(trades.stream().map(MerchantOffer::asBukkit).toList());
            this.title = title;
            this.level = level;
            this.villagerXp = villagerXp;
            this.showProgressBar = showProgressBar;
            this.notifyTradeSound = notifyTradeSound;
            this.canRestock = canRestock;
        }

        public List<MerchantRecipe> getTrades() {
            return new ArrayList<>(this.trades);
        }

        public Builder addTrade(MerchantRecipe trade) {
            trades.add(trade);
            return this;
        }

        public Builder addTrade(int i, MerchantRecipe trade) {
            trades.add(i, trade);
            return this;
        }

        public Builder addTrade(@Nonnull ItemStack ingredientA, @Nullable ItemStack ingredientB, @Nonnull ItemStack result) {
            return this.addTrade(ingredientA, ingredientB, result, 0, Integer.MAX_VALUE, false, 0, 1);
        }

        public Builder addTrade(@Nonnull ItemStack ingredientA, @Nullable ItemStack ingredientB, @Nonnull ItemStack result, int maxUses) {
            return this.addTrade(ingredientA, ingredientB, result, 0, maxUses, false, 0, 1);
        }

        public Builder addTrade(@Nonnull ItemStack ingredientA, @Nullable ItemStack ingredientB, @Nonnull ItemStack result, int uses, int maxUses, boolean experienceReward, int villagerExperience, int priceMultiplier) {
            MerchantRecipe trade = new MerchantRecipe(result, uses, maxUses, experienceReward, villagerExperience, priceMultiplier);
            trade.addIngredient(ingredientA);
            if (ingredientB != null) trade.addIngredient(ingredientB);
            return this.addTrade(trade);
        }

        public Builder addTrade(int i, @Nonnull ItemStack ingredientA, @Nullable ItemStack ingredientB, @Nonnull ItemStack result) {
            return this.addTrade(i, ingredientA, ingredientB, result, 0, Integer.MAX_VALUE, false, 0, 1);
        }

        public Builder addTrade(int i, @Nonnull ItemStack ingredientA, @Nullable ItemStack ingredientB, @Nonnull ItemStack result, int maxUses) {
            return this.addTrade(i, ingredientA, ingredientB, result, 0, maxUses, false, 0, 1);
        }

        public Builder addTrade(int i, @Nonnull ItemStack ingredientA, @Nullable ItemStack ingredientB, @Nonnull ItemStack result, int uses, int maxUses, boolean experienceReward, int villagerExperience, int priceMultiplier) {
            MerchantRecipe trade = new MerchantRecipe(result, uses, maxUses, experienceReward, villagerExperience, priceMultiplier);
            trade.addIngredient(ingredientA);
            if (ingredientB != null) trade.addIngredient(ingredientB);
            return this.addTrade(i, trade);
        }

        public Builder setTrade(int i, @Nonnull ItemStack ingredientA, @Nullable ItemStack ingredientB, @Nonnull ItemStack result) {
            return this.setTrade(i, ingredientA, ingredientB, result, 0, Integer.MAX_VALUE, false, 0, 1);
        }

        public Builder setTrade(int i, @Nonnull ItemStack ingredientA, @Nullable ItemStack ingredientB, @Nonnull ItemStack result, int maxUses) {
            return this.setTrade(i, ingredientA, ingredientB, result, 0, maxUses, false, 0, 1);
        }

        public Builder setTrade(int i, @Nonnull ItemStack ingredientA, @Nullable ItemStack ingredientB, @Nonnull ItemStack result, int uses, int maxUses, boolean experienceReward, int villagerExperience, int priceMultiplier) {
            MerchantRecipe trade = new MerchantRecipe(result, uses, maxUses, experienceReward, villagerExperience, priceMultiplier);
            trade.addIngredient(ingredientA);
            if (ingredientB != null) trade.addIngredient(ingredientB);
            return this.setTrade(i, trade);
        }

        public Builder setTrade(int i, MerchantRecipe trade) {
            trades.set(i, trade);
            return this;
        }

        public Builder removeTrade(int i) {
            trades.remove(i);
            return this;
        }

        public net.kyori.adventure.text.Component getTitle() {
            return this.title;
        }

        public Builder setTitle(net.kyori.adventure.text.Component title) {
            this.title = title;
            return this;
        }

        public int getVillagerXp() {
            return this.villagerXp;
        }

        public Builder setVillagerXp(int villagerXp) {
            this.villagerXp = villagerXp;
            return this;
        }

        public boolean showProgressBar() {
            return this.showProgressBar;
        }

        public Builder showProgressBar(boolean showProgressBar) {
            this.showProgressBar = showProgressBar;
            return this;
        }

        public Sound getNotifyTradeSound() {
            return this.notifyTradeSound;
        }

        public Builder setNotifyTradeSound(Sound sound) {
            this.notifyTradeSound = sound;
            return this;
        }

        public boolean canRestock() {
            return this.canRestock;
        }

        public Builder canRestock(boolean canRestock) {
            this.canRestock = canRestock;
            return this;
        }

        public TradingGui build() {
            if (this.merchant != null && this.merchant instanceof CraftMerchant craftMerchant) {
                net.minecraft.world.item.trading.Merchant minecraftMerchant = craftMerchant.getMerchant();
                if (minecraftMerchant instanceof Merchant customMerchant) {
                    customMerchant.offers.clear();
                    customMerchant.offers.addAll(this.trades.stream().map(CraftMerchantRecipe::fromBukkit).map(CraftMerchantRecipe::toMinecraft).toList());
                    customMerchant.villagerXp = this.villagerXp;
                    customMerchant.showProgressBar = this.showProgressBar;
                    customMerchant.notifyTradeSound = CraftSound.bukkitToMinecraft(this.notifyTradeSound);
                    customMerchant.canRestock = this.canRestock;
                }
            } else {
                this.merchant = new Merchant(null, this.trades, this.villagerXp, this.showProgressBar, CraftSound.bukkitToMinecraft(this.notifyTradeSound), this.canRestock).getCraftMerchant();
            }

            return new TradingGui(this.merchant, this.level, this.title);
        }

        private static class Merchant implements net.minecraft.world.item.trading.Merchant {
            private Player tradingPlayer;
            private MerchantOffers offers;
            private int villagerXp;
            private boolean showProgressBar;
            private SoundEvent notifyTradeSound;
            private boolean canRestock;
            private final CraftMerchant craftMerchant = new CraftMerchant(this);

            public Merchant(@Nullable org.bukkit.entity.Player tradingPlayer, List<MerchantRecipe> offers, int villagerXp, boolean showProgressBar, SoundEvent notifyTradeSound, boolean canRestock) {
                this.tradingPlayer = MinecraftAdapter.player(tradingPlayer);
                this.offers = new MerchantOffers();
                this.offers.addAll(offers.stream().map(CraftMerchantRecipe::fromBukkit).map(CraftMerchantRecipe::toMinecraft).toList());
                this.villagerXp = villagerXp;
                this.showProgressBar = showProgressBar;
                this.notifyTradeSound = notifyTradeSound;
                this.canRestock = canRestock;
            }

            @Override
            public void setTradingPlayer(@Nullable Player customer) {
                this.tradingPlayer = customer;
            }

            @Nullable
            @Override
            public Player getTradingPlayer() {
                return this.tradingPlayer;
            }

            @Override
            public MerchantOffers getOffers() {
                return this.offers;
            }

            @Override
            public void overrideOffers(MerchantOffers offers) {
                this.offers = offers;
            }

            // Call by MerchantResultSlot#onTake
            @Override
            public void processTrade(MerchantOffer merchantRecipe, @Nullable PlayerPurchaseEvent event) {
                net.minecraft.world.item.trading.Merchant.super.processTrade(merchantRecipe, event);
            }

            @Override
            public void notifyTrade(MerchantOffer offer) {
                MerchantRecipe recipe = new CraftMerchantRecipe(offer);
            }

            @Override
            public void notifyTradeUpdated(net.minecraft.world.item.ItemStack stack) {

            }

            @Override
            public int getVillagerXp() {
                return this.villagerXp;
            }

            @Override
            public void overrideXp(int experience) {
                this.villagerXp = experience;
            }

            @Override
            public boolean showProgressBar() {
                return this.showProgressBar;
            }

            @Override
            public SoundEvent getNotifyTradeSound() {
                return this.notifyTradeSound;
            }

            @Override
            public boolean canRestock() {
                return this.canRestock;
            }

            @Override
            public void openTradingScreen(Player player, Component name, int levelProgress) {
                OptionalInt syncIdOptional = player.openMenu(new SimpleMenuProvider((syncId, playerInventory, menuOpenPlayer) -> new MerchantMenu(syncId, playerInventory, this), name));

                if (syncIdOptional.isPresent()) {
                    if (!this.offers.isEmpty()) {
                        player.sendMerchantOffers(syncIdOptional.getAsInt(), this.offers, levelProgress, this.villagerXp, this.showProgressBar, this.canRestock);
                    }
                }
            }

            @Override
            public boolean isClientSide() {
                return false;
            }

            @Override
            public CraftMerchant getCraftMerchant() {
                return this.craftMerchant;
            }
        }
    }
}
