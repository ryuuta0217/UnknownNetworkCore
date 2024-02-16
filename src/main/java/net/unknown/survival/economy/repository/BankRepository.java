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

package net.unknown.survival.economy.repository;

import net.unknown.core.managers.RunnableManager;
import net.unknown.shared.SharedConstants;
import net.unknown.survival.economy.UnknownNetworkEconomy;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.io.File;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class BankRepository implements Repository {
    private static final Logger LOGGER = LoggerFactory.getLogger("UNC/BankRepository");
    // /root/unknown-network/2.Survival/../economy/banks
    private static final File SAVE_DIR = new File(SharedConstants.DATA_FOLDER, "economy/banks");
    private static final Map<UUID, Map<String, BankRepository>> BANKS = new HashMap<>();

    private final UUID owner;
    private final String name;
    private BigDecimal balance;
    private final Map<Long, Transaction> transactions;

    public BankRepository(UUID owner, String name, BigDecimal balance, Map<Long, Transaction> transactions) {
        this.owner = owner;
        this.name = name;
        this.balance = balance;
        this.transactions = new HashMap<>(transactions); // always mutable
    }

    public BankRepository(UUID owner, String name, BigDecimal balance) {
        this(owner, name, balance, Collections.emptyMap());
    }

    /**
     * 口座名を取得します。
     *
     * @return 口座名
     */
    public String getName() {
        return this.name;
    }

    /**
     * 口座の所有者を取得します。
     *
     * @return 口座の所有者
     */
    public UUID getOwner() {
        return this.owner;
    }

    /**
     * 口座に指定した金額を入金します。
     *
     * @param value 入金する金額
     * @return 入金後の口座残高
     */
    @Override
    public BigDecimal deposit(BigDecimal value) {
        if (value.compareTo(BigDecimal.ZERO) > 0) throw new IllegalArgumentException("金額は0以上である必要があります。");
        this.balance = this.balance.add(value);
        long timestamp = System.currentTimeMillis();
        this.auditTransaction(timestamp, Transaction.Type.DEPOSIT, value, this.balance, false);
        RunnableManager.runAsync(this::save);
        return this.balance;
    }

    /**
     * 口座から指定した金額を出金します。
     *
     * @param value 出金する金額
     * @return 出金後の口座残高
     */
    @Override
    public BigDecimal withdraw(BigDecimal value) {
        if (value.compareTo(BigDecimal.ZERO) > 0) throw new IllegalArgumentException("金額は0以上である必要があります。");
        this.balance = this.balance.subtract(value);
        long timestamp = System.currentTimeMillis();
        this.auditTransaction(timestamp, Transaction.Type.WITHDRAW, value, this.balance, false);
        RunnableManager.runAsync(this::save);
        return this.balance;
    }

    /**
     * 口座の残高を取得します。
     *
     * @return 口座残高
     */
    @Override
    public BigDecimal getBalance() {
        return this.balance;
    }

    /**
     * 取引を記録します。
     *
     * @param timestamp 取引日時
     * @param type 取引の種類
     * @param amount 取引金額
     * @param balance 取引後の残高
     */
    public void auditTransaction(long timestamp, Transaction.Type type, BigDecimal amount, BigDecimal balance) {
        this.auditTransaction(timestamp, type, amount, balance, true);
    }

    /**
     * 取引を記録します。
     *
     * @param timestamp 取引日時
     * @param type 取引の種類
     * @param amount 取引金額
     * @param balance 取引後の残高
     * @param save ディスクへ保存するかどうか
     */
    public void auditTransaction(long timestamp, Transaction.Type type, BigDecimal amount, BigDecimal balance, boolean save) {
        this.transactions.put(timestamp, new Transaction(type, amount, balance, null));
        if (save) RunnableManager.runAsync(this::save);
    }

    /**
     * 口座情報をファイルに書き込みます。
     */
    @Override
    public synchronized void save() {
        File dir = new File(SAVE_DIR, this.getOwner().toString());
        if (dir.exists() || dir.mkdirs()) {
            File file = new File(dir, this.getName() + ".yml");
            try {
                if (file.exists() || file.createNewFile()) {
                    FileConfiguration config = YamlConfiguration.loadConfiguration(file);
                    config.set("balance", this.getBalance().doubleValue());

                    config.set("transactions", null);
                    ConfigurationSection transactionsSection = config.createSection("transactions");
                    this.transactions.forEach((timestamp, transaction) -> transaction.save(transactionsSection.createSection(String.valueOf(timestamp))));

                    config.save(file);
                }
            } catch (Throwable t) {
                LOGGER.error("プレイヤー " + this.getOwner() + " の口座 " + this.getName() + " を保存できませんでした", t);
            }
        }
    }

    /**
     * 口座を新規作成します。
     *
     * @param owner    口座を所有するプレイヤー
     * @param bankName 口座名
     * @return 新規作成された口座のインスタンス
     * @throws IllegalArgumentException 口座名が既に使用されている場合
     */
    public static BankRepository create(UUID owner, String bankName) {
        if (isExists(owner, bankName)) throw new IllegalArgumentException("口座名 " + bankName + " は既に使用されています。");
        BANKS.get(owner).put(bankName, new BankRepository(owner, bankName, new BigDecimal(0)));
        return BANKS.get(owner).get(bankName);
    }

    /**
     * 口座を削除します。
     *
     * @param owner    口座を所有するプレイヤー
     * @param bankName 口座名
     * @return 削除された口座のインスタンス
     * @throws IllegalArgumentException 口座が存在しない場合
     */
    public static BankRepository remove(UUID owner, String bankName) {
        if (!isExists(owner, bankName)) throw new IllegalArgumentException("口座 " + bankName + " は存在しません");
        return BANKS.get(owner).remove(bankName);
    }

    /**
     * 口座を取得します。
     *
     * @param owner    口座を所有するプレイヤー
     * @param bankName 口座名
     * @return 口座のインスタンス、存在しない場合はnullを返します
     */
    @Nullable
    public static BankRepository get(UUID owner, String bankName) {
        if (!isExists(owner, bankName)) return null;
        return BANKS.get(owner).get(bankName);
    }

    public static Map<UUID, Map<String, BankRepository>> getBanks() {
        return BANKS.entrySet()
                .stream()
                .collect(Collectors.toMap(Map.Entry::getKey, e -> new HashMap<>(e.getValue())));
    }

    /**
     * 口座が存在するかどうかを検証します。
     * プレイヤーがMapに存在しなければ新しくHashMapインスタンスを生成します。
     *
     * @param owner    口座を所有するプレイヤー
     * @param bankName 口座名
     * @return 口座が存在する場合はtrue、存在しない場合はfalseを返します
     */
    public static boolean isExists(UUID owner, String bankName) {
        if (!BANKS.containsKey(owner)) BANKS.put(owner, new HashMap<>());
        return BANKS.get(owner).containsKey(bankName);
    }

    /**
     * 存在する口座全てをファイルから読み込みます。
     */
    public static void loadExists() {
        if (SAVE_DIR.exists() || SAVE_DIR.mkdirs()) {
            Stream.of(SAVE_DIR.listFiles())
                    .filter(File::isDirectory)
                    .forEach(ownerFolder -> {
                        UUID owner = UUID.fromString(ownerFolder.getName());
                        Stream.of(ownerFolder.listFiles())
                                .filter(File::isFile)
                                .filter(file -> file.getName().endsWith(".yml"))
                                .forEach(bankFile -> {
                                    try {
                                        String bankName = bankFile.getName().replaceFirst("\\.yml$", "");
                                        if (!isExists(owner, bankName)) {
                                            BANKS.get(owner).put(bankName, load(owner, bankName));
                                        }
                                    } catch (Throwable t) {
                                        LOGGER.error("プレイヤー " + owner + " の口座ファイル " + bankFile.getName() + " の読み込みに失敗しました", t);
                                    }
                                });
                    });
        }
    }

    public static BankRepository load(UUID owner, String bankName) {
        try {
            File bankFile = new File(SAVE_DIR, owner.toString() + "/" + bankName + ".yml");
            FileConfiguration config = YamlConfiguration.loadConfiguration(bankFile);
            if (config.isSet("balance")) {
                BigDecimal balance = BigDecimal.valueOf(config.getDouble("balance"));
                Map<Long, Transaction> transactions = new HashMap<>();
                if (config.isSet("transactions")) {
                    ConfigurationSection transactionSection = config.getConfigurationSection("transactions");

                    transactionSection.getValues(false).forEach((timestampStr, transactionDataRaw) -> {
                        if (timestampStr.matches("\\d+") && transactionDataRaw instanceof ConfigurationSection transactionData) {
                            long timestamp = Long.parseLong(timestampStr);
                            Transaction transaction = Transaction.load(transactionData);
                            if (transaction != null) {
                                transactions.put(timestamp, transaction);
                            } else {
                                LOGGER.warn("プレイヤー " + owner + " の口座 " + bankName + " の " + timestamp + " に行われた取引履歴を読み込み中にエラーが発生しました");
                            }
                        }
                    });
                }
                return new BankRepository(owner, bankName, balance);
            }
        } catch(Throwable t) {
            t.printStackTrace();
        }
        LOGGER.error("プレイヤー " + owner + " の口座 " + bankName + " の読み込みに失敗しました");
        return null;
    }

    /**
     * プレイヤーの所有する口座情報を全てファイルに書き込みます。
     *
     * @param owner 口座を所有するプレイヤー
     */
    public synchronized static void save(UUID owner) {
        BANKS.get(owner).forEach((name, repo) -> repo.save());
    }

    /**
     * すべてのプレイヤーの口座情報を保存します。
     */
    public synchronized static void saveAll() {
        BANKS.keySet().forEach(BankRepository::save);
    }

    public static class Transaction {
        private final Type type;
        private final BigDecimal amount;
        private final BigDecimal balance;
        @Nullable private final UUID target;

        protected Transaction(Type type, BigDecimal amount, BigDecimal balance, @Nullable UUID target) {
            this.type = type;
            this.amount = amount;
            this.balance = balance;
            this.target = target;
        }

        protected Transaction(Type type, double amount, double balance, UUID target) {
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

        public void save(ConfigurationSection config) {
            config.set("type", this.type.name());
            config.set("amount", this.amount.scaleByPowerOfTen(UnknownNetworkEconomy.FRACTIONAL_DIGITS).longValue());
            config.set("balance", this.balance.scaleByPowerOfTen(UnknownNetworkEconomy.FRACTIONAL_DIGITS).longValue());

            if (this.type == Type.TRANSFER_SEND || this.type == Type.TRANSFER_RECEIVE) {
                config.set("target", this.target.toString());
            }
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
}
