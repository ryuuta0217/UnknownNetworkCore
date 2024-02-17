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

import net.unknown.core.util.MinecraftAdapter;
import net.unknown.survival.data.model.vote.ExchangeItemType;
import net.unknown.survival.data.model.vote.SelectableItem;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.HumanEntity;
import org.bukkit.inventory.ItemStack;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;
import java.util.stream.Collectors;

public class SimpleRandomExchangeItem extends SimpleExchangeItem {
    protected final Map<String, ItemStack> choices;

    public SimpleRandomExchangeItem(ItemStack displayItem, @Nonnull Map<String, ItemStack> choices, int price) {
        super(displayItem, price);
        this.choices = new HashMap<>(choices); // always mutable
    }

    @Override
    public ExchangeItemType getType() {
        return ExchangeItemType.SIMPLE_RANDOM;
    }

    @Override
    public ItemStack getDisplayItem(@Nullable HumanEntity exchanger) {
        return super.getItem(exchanger, null);
    }

    @Override
    public ItemStack getItem(@Nullable HumanEntity exchanger, @Nullable String choiceIdentifier) {
        List<ItemStack> choicesAsList = new ArrayList<>(this.choices.values());
        Collections.shuffle(choicesAsList);
        return choicesAsList.get(0);
    }

    @Override
    public void setItem(ItemStack item) {
        throw new UnsupportedOperationException("SimpleRandomExchangeItem is randomly item, can't set result item.");
    }

    public Map<String, ItemStack> getChoices() {
        return Collections.unmodifiableMap(this.choices);
    }

    public boolean hasChoice(String identifier) {
        return this.choices.containsKey(identifier);
    }

    @Nullable
    public ItemStack getChoice(String identifier) {
        return this.choices.getOrDefault(identifier, null);
    }

    public void addChoice(String identifier, ItemStack choice) {
        if (identifier.contains(".")) throw new IllegalArgumentException("ID cannot contain a '.'!");
        if (hasChoice(identifier)) throw new IllegalStateException("Choice id " + identifier + " is already used!");
        this.choices.put(identifier, choice);
        this.save();
    }

    public boolean removeChoice(String identifier) {
        if (!this.hasChoice(identifier)) return false;
        this.choices.remove(identifier);
        this.save();
        return true;
    }

    @Override
    public void write(ConfigurationSection config) {
        super.write(config);
        config.set("choices", this.choices.entrySet()
                .stream()
                .collect(Collectors.toMap(Map.Entry::getKey,
                        e -> MinecraftAdapter.ItemStack.json(e.getValue()))));
    }

    public static SimpleRandomExchangeItem load(ConfigurationSection config) {
        SimpleExchangeItem simpleItem = SimpleExchangeItem.load(config);
        if (config.isSet("choices") && simpleItem != null) {
            Map<String, ItemStack> choices = config.getConfigurationSection("choices")
                    .getValues(false)
                    .entrySet()
                    .stream()
                    .filter(e -> e.getValue() instanceof String)
                    .map(e -> Map.entry(e.getKey(), ((String) e.getValue())))
                    .map(e -> Map.entry(e.getKey(), MinecraftAdapter.ItemStack.itemStack(MinecraftAdapter.ItemStack.json(e.getValue()))))
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
            return new SimpleRandomExchangeItem(simpleItem.item, choices, simpleItem.price);
        }
        return null;
    }
}
