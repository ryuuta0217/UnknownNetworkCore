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

import net.unknown.core.managers.EvalManager;
import net.unknown.survival.vote.data.ExchangeItem;
import net.unknown.survival.vote.data.ExchangeItemType;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.HumanEntity;
import org.bukkit.inventory.ItemStack;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.ScriptableObject;

import javax.annotation.Nullable;

public class ScriptExchangeItem implements ExchangeItem {
    private String getItemRaw;
    private String getPriceRaw;
    private String getDisplayItemRaw;
    private String onExchangedRaw;
    @Nullable private Function getItemCompiled;
    @Nullable private Function getPriceCompiled;
    @Nullable private Function getDisplayItemCompiled;
    @Nullable private Function onExchangedCompiled;

    public ScriptExchangeItem(String getItem, String getPrice, String getDisplayItem, String onExchanged) {
        this.setGetItemScript(getItem, false);
        this.setGetPriceScript(getPrice, false);
        this.setGetDisplayItemScript(getDisplayItem, false);
        this.setOnExchangedScript(onExchanged, false);
    }

    @Override
    public ExchangeItemType getType() {
        return ExchangeItemType.SCRIPT;
    }

    @Override
    public ItemStack getDisplayItem(@Nullable HumanEntity exchanger) {
        return this.execGetDisplayFunction(exchanger);
    }

    @Override
    public void setDisplayItem(ItemStack item) {
        throw new UnsupportedOperationException("Can't set script displayItem");
    }

    @Override
    public ItemStack getItem(@Nullable HumanEntity exchanger, @Nullable String choiceIdentifier) {
        return this.execGetItemFunction(exchanger, choiceIdentifier);
    }

    @Override
    public void setItem(ItemStack item) {
        throw new UnsupportedOperationException("Can't set script item");
    }

    @Override
    public int getPrice(HumanEntity exchanger) {
        return this.execGetPriceFunction(exchanger);
    }

    @Override
    public void setPrice(int price) {
        throw new UnsupportedOperationException("Can't set script price");
    }

    @Override
    public void write(ConfigurationSection config) {
        config.set("get-item", this.getItemRaw);
        config.set("get-price", this.getPriceRaw);
        config.set("get-display-item", this.getDisplayItemRaw);
        config.set("on-exchanged", this.onExchangedRaw);
    }

    public static ScriptExchangeItem load(ConfigurationSection config) {
        String getItem = config.getString("get-item");
        String getPrice = config.getString("get-price");
        String getDisplayItem = config.getString("get-display-item");
        String onExchanged = config.getString("on-exchanged");

        return new ScriptExchangeItem(getItem, getPrice, getDisplayItem, onExchanged);
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

    public String getGetItemScript() {
        return this.getItemRaw;
    }

    public void setGetItemScript(String script) {
        this.setGetItemScript(script, true);
    }

    public void setGetItemScript(String script, boolean save) {
        this.getItemRaw = script;
        this.getItemCompiled = this.getItemRaw != null ? EvalManager.compileFunction("VoteTicketExchangeItemScript", this.getItemRaw) : null;
        if (save) this.save();
    }

    @Nullable
    public Function getGetItemFunction() {
        return this.getItemCompiled;
    }

    @Nullable
    public ItemStack execGetItemFunction(HumanEntity player, @Nullable String choiceIdentifier) {
        Function getItem = this.getGetItemFunction();
        if (getItem == null) return null;

        Object result = Context.jsToJava(getItem.call(EvalManager.getRhinoContext(), this.getExecutionScope(), null, new Object[]{player, choiceIdentifier}), ItemStack.class);
        if (!(result instanceof ItemStack is)) return null;
        return is;
    }

    public String getGetDisplayItemScript() {
        return this.getDisplayItemRaw;
    }

    public void setGetDisplayItemScript(String script) {
        this.setGetDisplayItemScript(script, true);
    }

    public void setGetDisplayItemScript(String script, boolean save) {
        this.getDisplayItemRaw = script;
        this.getDisplayItemCompiled = this.getDisplayItemRaw != null ? EvalManager.compileFunction("VoteTicketExchangeItemScript", this.getDisplayItemRaw) : null;
        if (save) this.save();
    }

    @Nullable
    public Function getGetDisplayItemFunction() {
        return this.getDisplayItemCompiled;
    }

    public ItemStack execGetDisplayFunction(HumanEntity player) {
        Function getDisplayItem = this.getGetDisplayItemFunction();
        if (getDisplayItem == null) return null;

        Object result = Context.jsToJava(getDisplayItem.call(EvalManager.getRhinoContext(), this.getExecutionScope(), null, new Object[]{player}), ItemStack.class);
        if (!(result instanceof ItemStack is)) return null;
        return is;
    }

    public String getGetPriceScript() {
        return this.getPriceRaw;
    }

    public void setGetPriceScript(String script) {
        this.setGetPriceScript(script, true);
    }

    public void setGetPriceScript(String script, boolean save) {
        this.getPriceRaw = script;
        this.getPriceCompiled = this.getPriceRaw != null ? EvalManager.compileFunction("VoteTicketExchangeItemScript", this.getPriceRaw) : null;
        if (save) this.save();
    }

    @Nullable
    public Function getGetPriceFunction() {
        return this.getPriceCompiled;
    }

    public int execGetPriceFunction(HumanEntity player) {
        Function getPrice = this.getGetPriceFunction();
        if (getPrice == null) return Integer.MAX_VALUE;

        Object result = Context.jsToJava(getPrice.call(EvalManager.getRhinoContext(), this.getExecutionScope(), null, new Object[]{player}), Integer.class);
        if (!(result instanceof Integer price)) return Integer.MAX_VALUE;
        return price;
    }

    public String getOnExchangedScript() {
        return this.onExchangedRaw;
    }

    public void setOnExchangedScript(String script) {
        this.setOnExchangedScript(script, true);
    }

    public void setOnExchangedScript(String script, boolean save) {
        this.onExchangedRaw = script;
        this.onExchangedCompiled = this.onExchangedRaw != null ? EvalManager.compileFunction("VoteTicketExchangeItemScript", this.onExchangedRaw) : null;
        if (save) this.save();
    }

    @Nullable
    public Function getOnExchangedFunction() {
        return this.onExchangedCompiled;
    }

    public void execOnExchangedFunction(HumanEntity player, @Nullable String choiceIdentifier) {
        Function onExchanged = this.getOnExchangedFunction();
        if (onExchanged == null) return;

        onExchanged.call(EvalManager.getRhinoContext(), this.getExecutionScope(), null, new Object[]{player, choiceIdentifier});
    }
}
