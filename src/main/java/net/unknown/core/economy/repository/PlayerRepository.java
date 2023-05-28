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

package net.unknown.core.economy.repository;

import net.unknown.shared.SharedConstants;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

// Save to: ~/../economy/players/UUID.yml
// exf) /root/unknown-network/economy/players/UUID.yml
public class PlayerRepository implements Repository {
    private static final Logger LOGGER = LoggerFactory.getLogger("UNC/PlayerRepository");
    private static final File SAVE_DIR = new File(SharedConstants.DATA_FOLDER, "economy/players");
    private static final Map<UUID, PlayerRepository> REPOSITORIES = new HashMap<>();

    private final UUID player;
    private BigDecimal balance;

    public PlayerRepository(UUID player, BigDecimal balance) {
        this.player = player;
        this.balance = balance;
    }

    /**
     * プレイヤーの所持金に指定した金額を入金します。
     *
     * @param value 入金する金額
     * @return 入金後のプレイヤーの所持金
     */
    @Override
    public BigDecimal deposit(BigDecimal value) {
        if (value.compareTo(BigDecimal.ZERO) < 0) throw new IllegalArgumentException("金額は0以上である必要があります。");
        this.balance = this.balance.add(value);
        return this.balance;
    }

    /**
     * プレイヤーの所持金から指定した金額を出金します。
     *
     * @param value 出金する金額
     * @return 出金後のプレイヤーの所持金
     */
    @Override
    public BigDecimal withdraw(BigDecimal value) {
        if (value.compareTo(BigDecimal.ZERO) < 0) throw new IllegalArgumentException("金額は0以上である必要があります。");
        this.balance = this.balance.subtract(value);
        return this.balance;
    }

    /**
     * プレイヤーの所持金を取得します。
     *
     * @return プレイヤーの所持金
     */
    @Override
    public BigDecimal getBalance() {
        return this.balance;
    }

    /**
     * 口座の所有者を取得します。
     *
     * @return このインスタンス（口座）の所有者
     */
    @Override
    public UUID getOwner() {
        return this.player;
    }

    @Override
    public void save() {
        if (SAVE_DIR.exists() || SAVE_DIR.mkdirs()) {
            File file = new File(SAVE_DIR, this.getOwner().toString() + ".yml");
            try {
                if (file.exists() || file.createNewFile()) {
                    FileConfiguration config = YamlConfiguration.loadConfiguration(file);
                    config.set("balance", this.getBalance().doubleValue());
                    config.save(file);
                }
            } catch (Throwable t) {
                LOGGER.error("プレイヤー " + this.getOwner() + " の所持金を保存できませんでした", t);
            }
        }
    }

    public static boolean hasAccount(UUID player) {
        return REPOSITORIES.containsKey(player);
    }

    public static PlayerRepository getAccount(UUID player) {
        return REPOSITORIES.getOrDefault(player, null);
    }

    public static PlayerRepository getAccountOrCreate(UUID player) {
        if (hasAccount(player)) return getAccount(player);
        REPOSITORIES.put(player, new PlayerRepository(player, BigDecimal.valueOf(10000.0D)));
        return getAccountOrCreate(player);
    }

    public static synchronized void loadExists() {

    }

    public static synchronized void saveAll() {
        REPOSITORIES.values().forEach(PlayerRepository::save);
    }
}
