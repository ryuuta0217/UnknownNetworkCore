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

package net.unknown.survival.data.model;

import net.unknown.core.builder.ItemStackBuilder;
import net.unknown.core.managers.EvalManager;
import net.unknown.core.managers.RunnableManager;
import net.unknown.core.util.MinecraftAdapter;
import net.unknown.survival.data.VoteTicketExchangeItems;
import org.bukkit.block.BlockState;
import org.bukkit.block.Container;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.HumanEntity;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.ScriptableObject;

import javax.annotation.Nullable;
import java.util.*;
import java.util.stream.Collectors;

public class VoteTicketExchangeItem {
    private final ItemType type;
    private ItemStack item;
    private int price;
    private final Object[] additionalData; // [0] = container, [1] = stacks, [2] = choices, [3] = script, [4] = compiled script

    private VoteTicketExchangeItem(ItemType type, ItemStack item, int price, Object... additionalData) {
        this.type = type;
        this.item = item;
        this.price = price;
        this.additionalData = additionalData;
    }

    private VoteTicketExchangeItem(ItemStack item, int price) {
        this(ItemType.SIMPLE, item, price);
    }

    public static VoteTicketExchangeItem ofSimple(ItemStack item, int price) {
        return new VoteTicketExchangeItem(ItemType.SIMPLE, item, price);
    }

    public static VoteTicketExchangeItem ofSimpleRandom(ItemStack displayItem, int price, Map<String, ItemStack> choices) {
        return new VoteTicketExchangeItem(ItemType.SIMPLE_RANDOM, displayItem, price, null, null, choices);
    }

    public static VoteTicketExchangeItem ofContainer(ItemStack container, int stacks, ItemStack item, int price) {
        return new VoteTicketExchangeItem(ItemType.CONTAINER, item, price, container, stacks);
    }

    public static VoteTicketExchangeItem ofSelectableContainer(ItemStack container, ItemStack displayItem, int stacks, @Nullable Map<String, ItemStack> choices, int price) {
        if (choices == null) choices = new HashMap<>();
        return new VoteTicketExchangeItem(ItemType.SELECTABLE_CONTAINER, displayItem, price, container, stacks, choices);
    }

    public static VoteTicketExchangeItem ofScript(String getItemScript, String getDisplayItemScript, String getPriceScript) {
        return new VoteTicketExchangeItem(ItemType.SCRIPT, null, -1, null, null, null, getItemScript, EvalManager.compileFunction("VoteTicketExchangeItem", getItemScript), getDisplayItemScript, EvalManager.compileFunction("VoteTicketExchangeItem", getDisplayItemScript), getPriceScript, EvalManager.compileFunction("VoteTicketExchangeItem", getPriceScript));
    }

    public ItemType getType() {
        return this.type;
    }

    @Nullable
    public ItemStack getItem() {
        return this.getItem(null, null);
    }

    @Nullable
    public ItemStack getItem(@Nullable HumanEntity player, @Nullable ItemStack choice) {
        return switch (this.type) {
            case SIMPLE -> this.item;
            case SIMPLE_RANDOM -> {
                List<ItemStack> choicesAsList = new ArrayList<>(this.getChoices().values());
                Collections.shuffle(choicesAsList);
                yield choicesAsList.get(0);
            }
            case CONTAINER, SELECTABLE_CONTAINER -> new ItemStackBuilder(this.getContainer())
                    .custom(itemStack -> {
                        ItemMeta itemMeta = itemStack.getItemMeta();
                        if (itemMeta instanceof BlockStateMeta blockStateItemMeta) {
                            BlockState blockState = blockStateItemMeta.getBlockState();
                            if (blockState instanceof Container container) {
                                for (int i = 0; i < this.getStacks(); i++) {
                                    if (this.type == ItemType.CONTAINER && choice == null) container.getInventory().addItem(this.item);
                                    else if (this.type == ItemType.SELECTABLE_CONTAINER && choice != null) container.getInventory().addItem(choice);
                                }
                                blockStateItemMeta.setBlockState(container);
                                itemStack.setItemMeta(blockStateItemMeta);
                            }
                        }
                    })
                    .build();
            case SCRIPT -> this.execGetItemFunction(player, choice);
        };
    }

    @Nullable
    public ItemStack getDisplayItem(HumanEntity player) {
        return switch(this.type) {
            case SIMPLE, CONTAINER -> this.getItem(player, null);
            case SIMPLE_RANDOM, SELECTABLE_CONTAINER -> this.item;
            case SCRIPT -> this.execGetDisplayFunction(player);
        };
    }

    public void setItem(ItemStack item) {
        this.item = item;
        RunnableManager.runAsync(VoteTicketExchangeItems.getInstance()::save);
    }

    public ItemStack getContainer() {
        return this.additionalData[0] instanceof ItemStack type ? type : null;
    }

    public void setContainer(ItemStack container) {
        this.additionalData[0] = container;
        RunnableManager.runAsync(VoteTicketExchangeItems.getInstance()::save);
    }

    public int getStacks() {
        return this.additionalData[1] instanceof Integer stacks ? stacks : -1;
    }

    @SuppressWarnings("unchecked")
    public Map<String, ItemStack> getChoices() {
        return this.additionalData[2] instanceof Map<?,?> ? (Map<String, ItemStack>) this.additionalData[2] : Collections.EMPTY_MAP;
    }

    public boolean hasChoice(String id) {
        return this.getChoices().containsKey(id);
    }

    public ItemStack getChoice(String id) {
        return this.getChoices().get(id);
    }

    public void addChoice(String id, ItemStack choice) {
        if (this.type != ItemType.SELECTABLE_CONTAINER && this.type != ItemType.SIMPLE_RANDOM) throw new IllegalStateException("Cannot add choice to non-selectable container or random!");
        if (id.contains(".")) throw new IllegalArgumentException("ID cannot contain a '.'!");
        if (hasChoice(id)) throw new IllegalStateException("Choice id " + id + " is already used!");
        this.getChoices().put(id, choice);
        RunnableManager.runAsync(VoteTicketExchangeItems.getInstance()::save);
    }

    public boolean removeChoice(String id) {
        if (this.type != ItemType.SELECTABLE_CONTAINER && this.type != ItemType.SIMPLE_RANDOM) return false;
        if (!hasChoice(id)) return false;
        this.getChoices().remove(id);
        RunnableManager.runAsync(VoteTicketExchangeItems.getInstance()::save);
        return true;
    }

    public int getPrice() {
        if (this.type == ItemType.SCRIPT) throw new IllegalStateException("Use #getPriceForScript(player)");
        return this.price;
    }

    public void setPrice(int price) {
        if (this.type == ItemType.SCRIPT) throw new IllegalStateException("Cannot set price for script item!");
        this.price = price;
        RunnableManager.runAsync(VoteTicketExchangeItems.getInstance()::save);
    }

    private ScriptableObject getExecutionScope() {
        ScriptableObject globalCopy = EvalManager.getGlobalScope();
        ScriptableObject.putProperty(globalCopy, "item", this);
        ScriptableObject.putConstProperty(globalCopy, "getItem", this.getGetItemFunction());
        ScriptableObject.putConstProperty(globalCopy, "getDisplayItem", this.getGetDisplayItemFunction());
        ScriptableObject.putConstProperty(globalCopy, "getPrice", this.getGetPriceFunction());
        ScriptableObject.putConstProperty(globalCopy, "onExchanged", this.getOnExchangedFunction());
        return globalCopy;
    }

    public String getGetItemFunctionScript() {
        return this.additionalData[3] instanceof String script ? script : null;
    }

    public void setGetItemFunction(String script) {
        if (this.type != ItemType.SCRIPT) throw new IllegalStateException("Cannot set function for non-script item!");
        this.additionalData[3] = script;
        this.additionalData[4] = EvalManager.compileFunction("VoteTicketExchangeItemScript", script);
        RunnableManager.runAsync(VoteTicketExchangeItems.getInstance()::save);
    }

    public Function getGetItemFunction() {
        return this.additionalData[4] instanceof Function function ? function : null;
    }

    public ItemStack execGetItemFunction(HumanEntity player, @Nullable ItemStack choice) {
        if (this.type != ItemType.SCRIPT) throw new IllegalStateException("Cannot execute function for non-script item!");
        Object result = Context.jsToJava(this.getGetItemFunction().call(EvalManager.getRhinoContext(), EvalManager.getGlobalScope(), null, new Object[]{player, choice}), ItemStack.class);
        if (!(result instanceof ItemStack is)) return null;
        return is;
    }

    public String getGetDisplayItemFunctionScript() {
        return this.additionalData[5] instanceof String script ? script : null;
    }

    public void setGetDisplayItemFunction(String script) {
        if (this.type != ItemType.SCRIPT) throw new IllegalStateException("Cannot set function for non-script item!");
        this.additionalData[5] = script;
        this.additionalData[6] = EvalManager.compileFunction("VoteTicketExchangeItemScript", script);
        RunnableManager.runAsync(VoteTicketExchangeItems.getInstance()::save);
    }

    public Function getGetDisplayItemFunction() {
        return this.additionalData[6] instanceof Function function ? function : null;
    }

    public ItemStack execGetDisplayFunction(HumanEntity player) {
        if (this.type != ItemType.SCRIPT) throw new IllegalStateException("Cannot execute function for non-script item!");
        Object result = Context.jsToJava(this.getGetDisplayItemFunction().call(EvalManager.getRhinoContext(), this.getExecutionScope(), null, new Object[]{player}), ItemStack.class);
        if (!(result instanceof ItemStack is)) return null;
        return is;
    }

    public String getGetPriceFunctionScript() {
        return this.additionalData[7] instanceof String script ? script : null;
    }

    public void setGetPriceFunction(String script) {
        if (this.type != ItemType.SCRIPT) throw new IllegalStateException("Cannot set function for non-script item!");
        this.additionalData[7] = script;
        this.additionalData[8] = EvalManager.compileFunction("VoteTicketExchangeItemScript", script);
        RunnableManager.runAsync(VoteTicketExchangeItems.getInstance()::save);
    }

    public Function getGetPriceFunction() {
        return this.additionalData[8] instanceof Function function ? function : null;
    }

    public int execGetPriceFunction(HumanEntity player) {
        if (this.type != ItemType.SCRIPT) throw new IllegalStateException("Cannot execute function for non-script item!");
        Object result = Context.jsToJava(this.getGetPriceFunction().call(EvalManager.getRhinoContext(), this.getExecutionScope(), null, new Object[]{player}), Integer.class);
        if (!(result instanceof Integer price)) return this.getPrice();
        return price;
    }

    public String getOnExchangedScript() {
        return this.additionalData[9] instanceof String script ? script : null;
    }

    public void setOnExchangedScript(String script) {
        if (this.type != ItemType.SCRIPT) throw new IllegalStateException("Cannot set function for non-script item!");
        this.additionalData[9] = script;
        this.additionalData[10] = EvalManager.compileFunction("VoteTicketExchangeItemScript", script);
        RunnableManager.runAsync(VoteTicketExchangeItems.getInstance()::save);
    }

    public Function getOnExchangedFunction() {
        return this.additionalData[10] instanceof Function function ? function : null;
    }

    public void execOnExchangedFunction(HumanEntity player, @Nullable ItemStack choice) {
        if (this.type != ItemType.SCRIPT) throw new IllegalStateException("Cannot execute function for non-script item!");
        this.getOnExchangedFunction().call(EvalManager.getRhinoContext(), this.getExecutionScope(), null, new Object[]{player, choice});
    }

    @Nullable
    public static VoteTicketExchangeItem load(ConfigurationSection config) {
        if (!config.isSet("type")) return null;
        ItemType type = ItemType.valueOf(config.getString("type"));
        ItemStack item = config.isSet("item") ? MinecraftAdapter.ItemStack.itemStack(MinecraftAdapter.ItemStack.json(config.getString("item"))) : null;
        int price = config.getInt("price");
        ItemStack container = config.isSet("container") ? MinecraftAdapter.ItemStack.itemStack(MinecraftAdapter.ItemStack.json(config.getString("container"))) : null;
        int stacks = config.getInt("stacks", -1);

        Map<String, ItemStack> choices = null;
        if (config.isSet("choices")) {
            ConfigurationSection choicesSection = config.getConfigurationSection("choices");
            if (choicesSection != null) {
                Map<String, Object> values = choicesSection.getValues(false);
                values.replaceAll((k, v) -> {
                    if (v instanceof String str) {
                        net.minecraft.world.item.ItemStack minecraftStack = MinecraftAdapter.ItemStack.json(str);
                        if (minecraftStack != null) return MinecraftAdapter.ItemStack.itemStack(minecraftStack);
                    }
                    return null;
                });
                values.entrySet().removeIf(e -> e.getValue() == null);
                choices = values.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, e -> (ItemStack) e.getValue()));
            }
        }

        String getItem = config.isSet("getItem") ? config.getString("getItem") : null;
        Function getItemFunction = getItem != null ? EvalManager.compileFunction("VoteTicketExchangeItem", getItem) : null;

        String getDisplayItem = config.isSet("getDisplayItem") ? config.getString("getDisplayItem") : null;
        Function getDisplayItemFunction = getDisplayItem != null ? EvalManager.compileFunction("VoteTicketExchangeItem", getDisplayItem) : null;

        String getPrice = config.isSet("getPrice") ? config.getString("getPrice") : null;
        Function getPriceFunction = getPrice != null ? EvalManager.compileFunction("VoteTicketExchangeItem", getPrice) : null;

        String onExchanged = config.isSet("onExchanged") ? config.getString("onExchanged") : null;
        Function onExchangedFunction = onExchanged != null ? EvalManager.compileFunction("VoteTicketExchangeItem", onExchanged) : null;


        // Validate
        if (type == ItemType.SIMPLE && (item == null)) return null;
        if (type == ItemType.SIMPLE_RANDOM && (item == null || choices == null)) return null;
        if (type == ItemType.CONTAINER && (container == null || stacks <= 0 || item == null)) return null;
        if (type == ItemType.SELECTABLE_CONTAINER && (container == null || stacks <= 0 || choices == null)) return null;
        if (type == ItemType.SCRIPT && (getItem == null || getDisplayItem == null || getPrice == null)) return null;
        // End of validate

        return new VoteTicketExchangeItem(type, item, price, container, stacks, choices, getItem, getItemFunction, getDisplayItem, getDisplayItemFunction, getPrice, getPriceFunction, onExchanged, onExchangedFunction);
    }

    public void save(ConfigurationSection config) {
        config.set("type", this.type.name());
        if (this.type != ItemType.SELECTABLE_CONTAINER) config.set("item", MinecraftAdapter.ItemStack.json(MinecraftAdapter.ItemStack.itemStack(this.item)));
        config.set("price", this.price);
        if (this.type == ItemType.CONTAINER || this.type == ItemType.SELECTABLE_CONTAINER) {
            config.set("container", MinecraftAdapter.ItemStack.json(MinecraftAdapter.ItemStack.itemStack(this.getContainer())));
            config.set("stacks", this.getStacks());
        }

        if (this.type == ItemType.SELECTABLE_CONTAINER || this.type == ItemType.SIMPLE_RANDOM) {
            config.set("choices", this.getChoices().entrySet().stream().collect(Collectors.toMap(k -> k.getKey().replace(".", "-"), e -> MinecraftAdapter.ItemStack.json(MinecraftAdapter.ItemStack.itemStack(e.getValue())))));
        }

        if (this.type == ItemType.SCRIPT) {
            config.set("getItem", this.additionalData[3]);
            config.set("getDisplayItem", this.additionalData[5]);
            config.set("getPrice", this.additionalData[7]);
            config.set("onExchanged", this.additionalData[9]);
        }
    }

    public enum ItemType {
        SIMPLE,
        SIMPLE_RANDOM,
        CONTAINER,
        SELECTABLE_CONTAINER,
        SCRIPT
    }
}
