/*
 * Copyright (c) 2022 Unknown Network Developers and contributors.
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

package net.unknown.core.economy.repository;

import net.unknown.UnknownNetworkCore;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.io.File;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;

public class BankRepository implements Repository {
    private static final Logger LOGGER = LoggerFactory.getLogger("BankRepository");
    // /root/unknown-network/2.Survival/../economy/banks
    private static final File SAVE_DIR = new File(UnknownNetworkCore.getSharedDataFolder(), "economy/banks");

    private static final Map<UUID, Map<String, BankRepository>> BANKS = new HashMap<>();
    private final UUID owner;
    private final String name;
    private BigDecimal balance;

    public BankRepository(UUID owner, String name, BigDecimal balance) {
        this.owner = owner;
        this.name = name;
        this.balance = balance;
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
                                        FileConfiguration config = YamlConfiguration.loadConfiguration(bankFile);
                                        if (config.contains("balance") && config.contains("name")) {
                                            String bankName = config.getString("name");
                                            BigDecimal balance = new BigDecimal(config.getLong("balance"));
                                            if (!isExists(owner, bankName)) {
                                                BANKS.get(owner).put(bankName, new BankRepository(owner, bankName, balance));
                                            }
                                        }
                                    } catch (Throwable t) {
                                        LOGGER.error("プレイヤー " + owner + " の口座ファイル " + bankFile.getName() + " の読み込みに失敗しました", t);
                                    }
                                });
                    });
        }
    }

    /**
     * 口座情報をファイルに書き込みます。
     *
     * @param owner 口座を所有するプレイヤー
     */
    public synchronized static void save(UUID owner) {
        File dir = new File(SAVE_DIR, owner.toString());
        if (dir.exists() || dir.mkdirs()) {
            BANKS.get(owner).forEach((name, repo) -> {
                File file = new File(dir, name + ".yml");
                try {
                    if (file.exists() || file.createNewFile()) {
                        FileConfiguration config = YamlConfiguration.loadConfiguration(file);
                        config.set("name", repo.getName());
                        config.set("balance", repo.getBalance().longValue()); // longValue!
                        config.save(file);
                    }
                } catch (Throwable e) {
                    LOGGER.error("プレイヤー " + owner + " の口座 " + name + " を保存できませんでした", e);
                }
            });
        }
    }

    /**
     * すべてのプレイヤーの口座情報を保存します。
     */
    public synchronized static void saveAll() {
        BANKS.keySet().forEach(BankRepository::save);
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
}
