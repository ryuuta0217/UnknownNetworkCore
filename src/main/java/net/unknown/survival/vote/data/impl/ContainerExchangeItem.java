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

package net.unknown.survival.vote.data.impl;

import net.unknown.core.builder.ItemStackBuilder;
import net.unknown.core.util.MinecraftAdapter;
import net.unknown.survival.vote.data.ExchangeItemType;
import org.bukkit.block.BlockState;
import org.bukkit.block.Container;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.HumanEntity;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.bukkit.inventory.meta.ItemMeta;

import javax.annotation.Nullable;

public class ContainerExchangeItem extends SimpleExchangeItem {
    protected ItemStack displayItem;
    protected ItemStack container;
    protected ItemStack item;
    protected int stacks;

    public ContainerExchangeItem(ItemStack displayItem, ItemStack container, ItemStack item, int stacks, int price) {
        super(item, price);
        this.displayItem = displayItem;
        this.container = container;
        this.item = item;
        this.stacks = stacks;
    }

    @Override
    public ExchangeItemType getType() {
        return ExchangeItemType.CONTAINER;
    }

    @Override
    public ItemStack getDisplayItem(@Nullable HumanEntity exchanger) {
        return this.displayItem;
    }

    @Override
    public void setDisplayItem(ItemStack item) {
        this.displayItem = item;
        this.save();
    }

    public ItemStack getContainer(@Nullable HumanEntity exchanger) {
        return this.container;
    }

    public void setContainer(ItemStack container) {
        this.container = container;
        this.save();
    }

    @Override
    public ItemStack getItem(@Nullable HumanEntity exchanger, @Nullable String choiceIdentifier) {
        return new ItemStackBuilder(this.getContainer(exchanger))
                .custom(itemStack -> {
                    ItemMeta itemMeta = itemStack.getItemMeta();
                    if (itemMeta instanceof BlockStateMeta blockStateItemMeta) {
                        BlockState blockState = blockStateItemMeta.getBlockState();
                        if (blockState instanceof Container blockContainer) {
                            for (int i = 0; i < this.getStacks(); i++) {
                                blockContainer.getInventory().addItem(this.item);
                            }
                            blockStateItemMeta.setBlockState(blockContainer);
                            itemStack.setItemMeta(blockStateItemMeta);
                        }
                    }
                })
                .build();
    }

    public int getStacks() {
        return this.stacks;
    }

    @Override
    public void write(ConfigurationSection config) {
        super.write(config);
        config.set("display-item", MinecraftAdapter.ItemStack.json(this.displayItem));
        config.set("container", MinecraftAdapter.ItemStack.json(this.container));
        config.set("stacks", this.stacks);
    }

    @Nullable
    public static ContainerExchangeItem load(ConfigurationSection config) {
        SimpleExchangeItem simpleItem = SimpleExchangeItem.load(config);
        if (simpleItem != null && config.isSet("display-item") && config.isSet("container") && config.isSet("stacks")) {
            ItemStack displayItem = MinecraftAdapter.ItemStack.itemStack(MinecraftAdapter.ItemStack.json(config.getString("display-item")));
            ItemStack container = MinecraftAdapter.ItemStack.itemStack(MinecraftAdapter.ItemStack.json(config.getString("container")));
            int stacks = config.getInt("stacks");
            return new ContainerExchangeItem(displayItem, container, simpleItem.item, stacks, simpleItem.price);
        }
        return null;
    }
}
