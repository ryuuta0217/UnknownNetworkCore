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

package net.unknown.survival.economy.transaction;

import net.unknown.survival.economy.UnknownNetworkEconomy;
import org.bukkit.configuration.ConfigurationSection;
import org.json.JSONObject;

import javax.annotation.Nullable;
import java.math.BigDecimal;
import java.util.UUID;

public class Transaction {
    private final Type type;
    private final String content;
    private final BigDecimal amount;
    private final BigDecimal balance;
    @Nullable
    private final UUID target;

    public Transaction(Type type, String content, BigDecimal amount, BigDecimal balance, @Nullable UUID target) {
        this.type = type;
        this.content = content;
        this.amount = amount;
        this.balance = balance;
        this.target = target;
    }

    public Transaction(Type type, String content, double amount, double balance, UUID target) {
        this(type, content, BigDecimal.valueOf(amount), BigDecimal.valueOf(balance), target);
    }

    protected Transaction(Type type, String content, long scaledAmount, long scaledBalance, UUID target) {
        this(type, content, BigDecimal.valueOf(scaledAmount).scaleByPowerOfTen(-UnknownNetworkEconomy.FRACTIONAL_DIGITS), BigDecimal.valueOf(scaledBalance).scaleByPowerOfTen(-UnknownNetworkEconomy.FRACTIONAL_DIGITS), target);
    }

    public static Transaction createDeposit(String content, BigDecimal amount, BigDecimal balance) {
        return new Transaction(Type.DEPOSIT, content, amount, balance, null);
    }

    public static Transaction createDeposit(String content, double amount, double balance) {
        return createDeposit(content, BigDecimal.valueOf(amount), BigDecimal.valueOf(balance));
    }

    public static Transaction createDeposit(String content, long scaledAmount, long scaledBalance) {
        return createDeposit(content, BigDecimal.valueOf(scaledAmount).scaleByPowerOfTen(-UnknownNetworkEconomy.FRACTIONAL_DIGITS), BigDecimal.valueOf(scaledBalance).scaleByPowerOfTen(-UnknownNetworkEconomy.FRACTIONAL_DIGITS));
    }

    public static Transaction createWithdraw(String content, BigDecimal amount, BigDecimal balance) {
        return new Transaction(Type.WITHDRAW, content, amount, balance, null);
    }

    public static Transaction createWithdraw(String content, double amount, double balance) {
        return createWithdraw(content, BigDecimal.valueOf(amount), BigDecimal.valueOf(balance));
    }

    public static Transaction createWithdraw(String content, long scaledAmount, long scaledBalance) {
        return createWithdraw(content, BigDecimal.valueOf(scaledAmount).scaleByPowerOfTen(-UnknownNetworkEconomy.FRACTIONAL_DIGITS), BigDecimal.valueOf(scaledBalance).scaleByPowerOfTen(-UnknownNetworkEconomy.FRACTIONAL_DIGITS));
    }

    public static Transaction createTransferSent(String content, UUID target, BigDecimal amount, BigDecimal balance) {
        return new Transaction(Type.TRANSFER_SEND, content, amount, balance, target);
    }

    public static Transaction createTransferSent(String content, UUID target, double amount, double balance) {
        return createTransferSent(content, target, BigDecimal.valueOf(amount), BigDecimal.valueOf(balance));
    }

    public static Transaction createTransferSent(String content, UUID target, long scaledAmount, long scaledBalance) {
        return createTransferSent(content, target, BigDecimal.valueOf(scaledAmount).scaleByPowerOfTen(-UnknownNetworkEconomy.FRACTIONAL_DIGITS), BigDecimal.valueOf(scaledBalance).scaleByPowerOfTen(-UnknownNetworkEconomy.FRACTIONAL_DIGITS));
    }

    public static Transaction createTransferReceive(String content, UUID target, BigDecimal amount, BigDecimal balance) {
        return new Transaction(Type.TRANSFER_RECEIVE, content, amount, balance, target);
    }

    public static Transaction createTransferReceive(String content, UUID target, double amount, double balance) {
        return createTransferReceive(content, target, BigDecimal.valueOf(amount), BigDecimal.valueOf(balance));
    }

    public static Transaction createTransferReceive(String content, UUID target, long scaledAmount, long scaledBalance) {
        return createTransferReceive(content, target, BigDecimal.valueOf(scaledAmount).scaleByPowerOfTen(-UnknownNetworkEconomy.FRACTIONAL_DIGITS), BigDecimal.valueOf(scaledBalance).scaleByPowerOfTen(-UnknownNetworkEconomy.FRACTIONAL_DIGITS));
    }

    public Type getType() {
        return this.type;
    }

    public String getContent() {
        return this.content;
    }

    public BigDecimal getAmount() {
        return this.amount;
    }

    public BigDecimal getBalance() {
        return this.balance;
    }

    public void save(JSONObject json) {
        json.put("type", this.type.name());
        json.put("content", this.content);
        json.put("amount", this.amount.scaleByPowerOfTen(-UnknownNetworkEconomy.FRACTIONAL_DIGITS).longValue());
        json.put("balance", this.balance.scaleByPowerOfTen(-UnknownNetworkEconomy.FRACTIONAL_DIGITS).longValue());

        if ((this.type == Type.TRANSFER_SEND || this.type == Type.TRANSFER_RECEIVE) && this.target != null) {
            json.put("target", this.target.toString());
        }
    }

    public void save(ConfigurationSection config) {
        config.set("type", this.type.name());
        config.set("content", this.content);
        config.set("amount", this.amount.scaleByPowerOfTen(UnknownNetworkEconomy.FRACTIONAL_DIGITS).longValue());
        config.set("balance", this.balance.scaleByPowerOfTen(UnknownNetworkEconomy.FRACTIONAL_DIGITS).longValue());

        if ((this.type == Type.TRANSFER_SEND || this.type == Type.TRANSFER_RECEIVE) && this.target != null) {
            config.set("target", this.target.toString());
        }
    }

    @Nullable
    public static Transaction load(JSONObject json) {
        if (json.has("type") && json.has("amount") && json.has("balance")) {
            Type type = Type.valueOf(json.getString("type"));
            String content = json.has("content") ? json.getString("content") : null;
            UUID target = json.has("target") ? UUID.fromString(json.getString("target")) : null;
            return new Transaction(type, content, json.getLong("amount"), json.getLong("balance"), target);
        }
        return null;
    }

    @Nullable
    public static Transaction load(ConfigurationSection config) {
        if (config.isSet("type") && config.isSet("amount") && config.isSet("balance")) {
            Type type = Type.valueOf(config.getString("type"));
            String content = config.isSet("content") ? config.getString("content") : null;
            UUID target = config.isSet("target") ? UUID.fromString(config.getString("target")) : null;
            return new Transaction(type, content, config.getLong("amount"), config.getLong("balance"), target);
        }
        return null;
    }

    public enum Type {
        DEPOSIT, WITHDRAW, TRANSFER_SEND, TRANSFER_RECEIVE
    }
}
