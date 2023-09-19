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

package net.unknown.survival.wrapper.economy;

import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import net.unknown.survival.UnknownNetworkSurvival;
import net.unknown.survival.wrapper.economy.event.PlayerDepositEvent;
import net.unknown.survival.wrapper.economy.event.PlayerWithdrawEvent;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

import java.util.List;

public class WrappedEconomy implements Economy {
    public static WrappedEconomy INSTANCE;
    private final Economy original;

    public WrappedEconomy(Economy original) {
        if (WrappedEconomy.INSTANCE != null) throw new IllegalStateException("Wrapper is Already initialized!");
        this.original = original;
        WrappedEconomy.INSTANCE = this;
        UnknownNetworkSurvival.getLogger().info("Successfully wrapped Economy " + this.original.getName() + "!");
    }

    public Economy getOriginal() {
        return this.original;
    }

    @Override
    public boolean isEnabled() {
        return this.original.isEnabled();
    }

    @Override
    public String getName() {
        return this.original.getName() + " (UNC Wrapped)";
    }

    @Override
    public boolean hasBankSupport() {
        return this.original.hasBankSupport();
    }

    @Override
    public int fractionalDigits() {
        return this.original.fractionalDigits();
    }

    @Override
    public String format(double amount) {
        return this.original.format(amount);
    }

    @Override
    public String currencyNamePlural() {
        return this.original.currencyNamePlural();
    }

    @Override
    public String currencyNameSingular() {
        return this.original.currencyNameSingular();
    }

    @Override
    @Deprecated
    public boolean hasAccount(String playerName) {
        return this.original.hasAccount(playerName);
    }

    @Override
    public boolean hasAccount(OfflinePlayer player) {
        return this.original.hasAccount(player);
    }

    @Override
    @Deprecated
    public boolean hasAccount(String playerName, String worldName) {
        return this.original.hasAccount(playerName, worldName);
    }

    @Override
    public boolean hasAccount(OfflinePlayer player, String worldName) {
        return this.original.hasAccount(player, worldName);
    }

    @Override
    @Deprecated
    public double getBalance(String playerName) {
        return this.original.getBalance(playerName);
    }

    @Override
    public double getBalance(OfflinePlayer player) {
        return this.original.getBalance(player);
    }

    @Override
    @Deprecated
    public double getBalance(String playerName, String world) {
        return this.original.getBalance(playerName, world);
    }

    @Override
    public double getBalance(OfflinePlayer player, String world) {
        return this.original.getBalance(player, world);
    }

    @Override
    @Deprecated
    public boolean has(String playerName, double amount) {
        return this.original.has(playerName, amount);
    }

    @Override
    public boolean has(OfflinePlayer player, double amount) {
        return this.original.has(player, amount);
    }

    @Override
    @Deprecated
    public boolean has(String playerName, String worldName, double amount) {
        return this.original.has(playerName, worldName, amount);
    }

    @Override
    public boolean has(OfflinePlayer player, String worldName, double amount) {
        return this.original.has(player, worldName, amount);
    }

    @Override
    @Deprecated
    public EconomyResponse withdrawPlayer(String playerName, double amount) {
        double before = this.getBalance(playerName);
        EconomyResponse response = this.original.withdrawPlayer(playerName, amount);
        if (response.type == EconomyResponse.ResponseType.SUCCESS) {
            new PlayerWithdrawEvent(!Bukkit.isPrimaryThread(), Bukkit.getOfflinePlayer(playerName), amount, before, response.balance).callEvent();
        }
        return response;
    }

    @Override
    public EconomyResponse withdrawPlayer(OfflinePlayer player, double amount) {
        double before = this.getBalance(player);
        EconomyResponse response = this.original.withdrawPlayer(player, amount);
        if (response.type == EconomyResponse.ResponseType.SUCCESS) {
            new PlayerWithdrawEvent(!Bukkit.isPrimaryThread(), player, amount, before, response.balance).callEvent();
        }
        return response;
    }

    @Override
    @Deprecated
    public EconomyResponse withdrawPlayer(String playerName, String worldName, double amount) {
        return this.original.withdrawPlayer(playerName, worldName, amount);
    }

    @Override
    public EconomyResponse withdrawPlayer(OfflinePlayer player, String worldName, double amount) {
        return this.original.withdrawPlayer(player, worldName, amount);
    }

    @Override
    @Deprecated
    public EconomyResponse depositPlayer(String playerName, double amount) {
        double before = this.getBalance(playerName);
        EconomyResponse response = this.original.depositPlayer(playerName, amount);
        if (response.type == EconomyResponse.ResponseType.SUCCESS) {
            new PlayerDepositEvent(!Bukkit.isPrimaryThread(), Bukkit.getOfflinePlayer(playerName), amount, before, response.balance).callEvent();
        }
        return response;
    }

    @Override
    public EconomyResponse depositPlayer(OfflinePlayer player, double amount) {
        double before = this.getBalance(player);
        EconomyResponse response = this.original.depositPlayer(player, amount);
        if (response.type == EconomyResponse.ResponseType.SUCCESS) {
            new PlayerDepositEvent(!Bukkit.isPrimaryThread(), player, amount, before, response.balance).callEvent();
        }
        return response;
    }

    @Override
    @Deprecated
    public EconomyResponse depositPlayer(String playerName, String worldName, double amount) {
        return this.original.depositPlayer(playerName, worldName, amount);
    }

    @Override
    public EconomyResponse depositPlayer(OfflinePlayer player, String worldName, double amount) {
        return this.original.depositPlayer(player, worldName, amount);
    }

    @Override
    @Deprecated
    public EconomyResponse createBank(String name, String player) {
        return this.original.createBank(name, player);
    }

    @Override
    public EconomyResponse createBank(String name, OfflinePlayer player) {
        return this.original.createBank(name, player);
    }

    @Override
    public EconomyResponse deleteBank(String name) {
        return this.original.deleteBank(name);
    }

    @Override
    public EconomyResponse bankBalance(String name) {
        return this.original.bankBalance(name);
    }

    @Override
    public EconomyResponse bankHas(String name, double amount) {
        return this.original.bankHas(name, amount);
    }

    @Override
    public EconomyResponse bankWithdraw(String name, double amount) {
        return this.original.bankWithdraw(name, amount);
    }

    @Override
    public EconomyResponse bankDeposit(String name, double amount) {
        return this.original.bankDeposit(name, amount);
    }

    @Override
    @Deprecated
    public EconomyResponse isBankOwner(String name, String playerName) {
        return this.original.isBankOwner(name, playerName);
    }

    @Override
    public EconomyResponse isBankOwner(String name, OfflinePlayer player) {
        return this.original.isBankOwner(name, player);
    }

    @Override
    @Deprecated
    public EconomyResponse isBankMember(String name, String playerName) {
        return this.original.isBankMember(name, playerName);
    }

    @Override
    public EconomyResponse isBankMember(String name, OfflinePlayer player) {
        return this.original.isBankMember(name, player);
    }

    @Override
    public List<String> getBanks() {
        return this.original.getBanks();
    }

    @Override
    @Deprecated
    public boolean createPlayerAccount(String playerName) {
        return this.original.createPlayerAccount(playerName);
    }

    @Override
    public boolean createPlayerAccount(OfflinePlayer player) {
        return this.original.createPlayerAccount(player);
    }

    @Override
    @Deprecated
    public boolean createPlayerAccount(String playerName, String worldName) {
        return this.original.createPlayerAccount(playerName, worldName);
    }

    @Override
    public boolean createPlayerAccount(OfflinePlayer player, String worldName) {
        return this.original.createPlayerAccount(player, worldName);
    }
}
