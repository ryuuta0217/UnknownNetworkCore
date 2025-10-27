package net.unknown.survival.economy.transaction;

import net.unknown.survival.economy.UnknownNetworkEconomy;
import org.bukkit.configuration.ConfigurationSection;
import org.json.JSONObject;

import javax.annotation.Nullable;
import java.math.BigDecimal;
import java.util.UUID;

public class Transaction {
    private final Type type;
    private final BigDecimal amount;
    private final BigDecimal balance;
    @Nullable
    private final UUID target;

    public Transaction(Type type, BigDecimal amount, BigDecimal balance, @Nullable UUID target) {
        this.type = type;
        this.amount = amount;
        this.balance = balance;
        this.target = target;
    }

    public Transaction(Type type, double amount, double balance, UUID target) {
        this(type, BigDecimal.valueOf(amount), BigDecimal.valueOf(balance), target);
    }

    protected Transaction(Type type, long scaledAmount, long scaledBalance, UUID target) {
        this(type, BigDecimal.valueOf(scaledAmount).scaleByPowerOfTen(-UnknownNetworkEconomy.FRACTIONAL_DIGITS), BigDecimal.valueOf(scaledBalance).scaleByPowerOfTen(-UnknownNetworkEconomy.FRACTIONAL_DIGITS), target);
    }

    public static Transaction createDeposit(BigDecimal amount, BigDecimal balance) {
        return new Transaction(Type.DEPOSIT, amount, balance, null);
    }

    public static Transaction createDeposit(double amount, double balance) {
        return createDeposit(BigDecimal.valueOf(amount), BigDecimal.valueOf(balance));
    }

    public static Transaction createDeposit(long scaledAmount, long scaledBalance) {
        return createDeposit(BigDecimal.valueOf(scaledAmount).scaleByPowerOfTen(-UnknownNetworkEconomy.FRACTIONAL_DIGITS), BigDecimal.valueOf(scaledBalance).scaleByPowerOfTen(-UnknownNetworkEconomy.FRACTIONAL_DIGITS));
    }

    public static Transaction createWithdraw(BigDecimal amount, BigDecimal balance) {
        return new Transaction(Type.WITHDRAW, amount, balance, null);
    }

    public static Transaction createWithdraw(double amount, double balance) {
        return createWithdraw(BigDecimal.valueOf(amount), BigDecimal.valueOf(balance));
    }

    public static Transaction createWithdraw(long scaledAmount, long scaledBalance) {
        return createWithdraw(BigDecimal.valueOf(scaledAmount).scaleByPowerOfTen(-UnknownNetworkEconomy.FRACTIONAL_DIGITS), BigDecimal.valueOf(scaledBalance).scaleByPowerOfTen(-UnknownNetworkEconomy.FRACTIONAL_DIGITS));
    }

    public static Transaction createTransferSent(UUID target, BigDecimal amount, BigDecimal balance) {
        return new Transaction(Type.TRANSFER_SEND, amount, balance, target);
    }

    public static Transaction createTransferSent(UUID target, double amount, double balance) {
        return createTransferSent(target, BigDecimal.valueOf(amount), BigDecimal.valueOf(balance));
    }

    public static Transaction createTransferSent(UUID target, long scaledAmount, long scaledBalance) {
        return createTransferSent(target, BigDecimal.valueOf(scaledAmount).scaleByPowerOfTen(-UnknownNetworkEconomy.FRACTIONAL_DIGITS), BigDecimal.valueOf(scaledBalance).scaleByPowerOfTen(-UnknownNetworkEconomy.FRACTIONAL_DIGITS));
    }

    public static Transaction createTransferReceive(UUID target, BigDecimal amount, BigDecimal balance) {
        return new Transaction(Type.TRANSFER_RECEIVE, amount, balance, target);
    }

    public static Transaction createTransferReceive(UUID target, double amount, double balance) {
        return createTransferReceive(target, BigDecimal.valueOf(amount), BigDecimal.valueOf(balance));
    }

    public static Transaction createTransferReceive(UUID target, long scaledAmount, long scaledBalance) {
        return createTransferReceive(target, BigDecimal.valueOf(scaledAmount).scaleByPowerOfTen(-UnknownNetworkEconomy.FRACTIONAL_DIGITS), BigDecimal.valueOf(scaledBalance).scaleByPowerOfTen(-UnknownNetworkEconomy.FRACTIONAL_DIGITS));
    }

    public Type getType() {
        return this.type;
    }

    public BigDecimal getAmount() {
        return this.amount;
    }

    public BigDecimal getBalance() {
        return this.balance;
    }

    public void save(JSONObject json) {
        json.put("type", this.type.name());
        json.put("amount", this.amount.scaleByPowerOfTen(-UnknownNetworkEconomy.FRACTIONAL_DIGITS).longValue());
        json.put("balance", this.balance.scaleByPowerOfTen(-UnknownNetworkEconomy.FRACTIONAL_DIGITS).longValue());

        if ((this.type == Type.TRANSFER_SEND || this.type == Type.TRANSFER_RECEIVE) && this.target != null) {
            json.put("target", this.target.toString());
        }
    }

    public void save(ConfigurationSection config) {
        config.set("type", this.type.name());
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
            UUID target = json.has("target") ? UUID.fromString(json.getString("target")) : null;
            return new Transaction(type, json.getLong("amount"), json.getLong("balance"), target);
        }
        return null;
    }

    @Nullable
    public static Transaction load(ConfigurationSection config) {
        if (config.isSet("type") && config.isSet("amount") && config.isSet("balance")) {
            Type type = Type.valueOf(config.getString("type"));
            UUID target = config.isSet("target") ? UUID.fromString(config.getString("target")) : null;
            return new Transaction(type, config.getLong("amount"), config.getLong("balance"), target);
        }
        return null;
    }

    public enum Type {
        DEPOSIT, WITHDRAW, TRANSFER_SEND, TRANSFER_RECEIVE
    }
}
