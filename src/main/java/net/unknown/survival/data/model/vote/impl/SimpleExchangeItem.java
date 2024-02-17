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

package net.unknown.survival.data.model.vote.impl;

import net.unknown.core.managers.EvalManager;
import net.unknown.core.managers.RunnableManager;
import net.unknown.core.util.MinecraftAdapter;
import net.unknown.survival.data.VoteTicketExchangeItems;
import net.unknown.survival.data.model.vote.ExchangeItem;
import net.unknown.survival.data.model.vote.ExchangeItemType;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.HumanEntity;
import org.bukkit.inventory.ItemStack;
import org.mozilla.javascript.Function;

import javax.annotation.Nullable;
import java.util.*;
import java.util.stream.Collectors;

public class SimpleExchangeItem implements ExchangeItem {
    @Nullable protected ItemStack item;
    protected int price;

    public SimpleExchangeItem(@Nullable ItemStack item, int price) {
        this.item = item;
        this.price = price;
    }

    @Override
    public ExchangeItemType getType() {
        return ExchangeItemType.SIMPLE;
    }

    @Override
    @Nullable
    public ItemStack getItem(@Nullable HumanEntity player, @Nullable String choiceIdentifier) {
        return this.item;
    }

    @Override
    @Nullable
    public ItemStack getDisplayItem(HumanEntity player) {
        return this.item;
    }

    @Override
    public void setItem(ItemStack item) {
        this.item = item;
        RunnableManager.runAsync(VoteTicketExchangeItems.getInstance()::save);
    }

    @Override
    public void setDisplayItem(ItemStack item) {
        this.setItem(item);
    }

    @Override
    public int getPrice(@Nullable HumanEntity exchanger) {
        return this.price;
    }

    @Override
    public void setPrice(int price) {
        this.price = price;
        this.save();
    }

    @Nullable
    public static SimpleExchangeItem load(ConfigurationSection config) {
        if (config.isSet("price")) {
            ItemStack item = config.isSet("item") ? MinecraftAdapter.ItemStack.itemStack(MinecraftAdapter.ItemStack.json(config.getString("item"))) : null;
            int price = config.getInt("price");
            return new SimpleExchangeItem(item, price);
        }
        return null;
    }

    @Override
    public void write(ConfigurationSection config) {
        config.set("item", this.item != null ? MinecraftAdapter.ItemStack.json(this.item) : null);
        config.set("price", this.price);
    }
}
