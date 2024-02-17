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

package net.unknown.survival.data.model.vote;

import net.unknown.core.managers.RunnableManager;
import net.unknown.survival.data.VoteTicketExchangeItems;
import net.unknown.survival.data.model.vote.impl.*;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.HumanEntity;
import org.bukkit.inventory.ItemStack;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Map;

public interface ExchangeItem {
    static SimpleExchangeItem ofSimple(ItemStack item, int price) {
        return new SimpleExchangeItem(item, price);
    }

    static SimpleRandomExchangeItem ofSimpleRandom(ItemStack displayItem, @Nonnull Map<String, ItemStack> choices, int price) {
        return new SimpleRandomExchangeItem(displayItem, choices, price);
    }

    static ContainerExchangeItem ofContainer(ItemStack displayItem, ItemStack container, ItemStack item, int stacks, int price) {
        return new ContainerExchangeItem(displayItem, container, item, stacks, price);
    }

    static SelectableContainerExchangeItem ofSelectableContainer(ItemStack displayItem, ItemStack container, @Nonnull Map<String, ItemStack> choices, int stacks, int price) {
        return new SelectableContainerExchangeItem(displayItem, container, choices, stacks, price);
    }

    static ScriptExchangeItem ofScript(String getItem, String getPrice, String getDisplayItem, String onExchanged) {
        return new ScriptExchangeItem(getItem, getPrice, getDisplayItem, onExchanged);
    }

    default boolean hasMultipleChoices() {
        return this instanceof SelectableItem;
    }
    ExchangeItemType getType();
    ItemStack getDisplayItem(@Nullable HumanEntity exchanger);
    void setDisplayItem(ItemStack item);
    ItemStack getItem(@Nullable HumanEntity exchanger, @Nullable String choiceIdentifier);
    void setItem(ItemStack item);
    int getPrice(@Nullable HumanEntity exchanger);
    void setPrice(int price);
    default void save() {
        RunnableManager.runAsync(VoteTicketExchangeItems.getInstance()::save);
    }
    void write(ConfigurationSection config);
}
