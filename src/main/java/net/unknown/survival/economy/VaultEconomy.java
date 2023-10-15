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

package net.unknown.survival.economy;

import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import net.unknown.UnknownNetworkCore;
import net.unknown.survival.economy.repository.PlayerRepository;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.ServicePriority;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

public class VaultEconomy implements Economy {
    private static final VaultEconomy INSTANCE = new VaultEconomy();

    public static void hookVault() {
        Bukkit.getServicesManager().register(Economy.class, INSTANCE, UnknownNetworkCore.getInstance(), ServicePriority.Normal);
        UnknownNetworkCore.getInstance().getLogger().info("Successfully hooked Vault!");
    }

    /**
     * Checks if economy method is enabled.
     *
     * @return Success or Failure
     */
    @Override
    public boolean isEnabled() {
        return true;
    }

    /**
     * Gets name of economy method
     *
     * @return Name of Economy Method
     */
    @Override
    public String getName() {
        return "UnknownNetworkCore";
    }

    /**
     * Returns true if the given implementation supports banks.
     *
     * @return true if the implementation supports banks
     */
    @Override
    public boolean hasBankSupport() {
        return true;
    }

    /**
     * Some economy plugins round off after a certain number of digits.
     * This function returns the number of digits the plugin keeps
     * or -1 if no rounding occurs.
     *
     * @return number of digits after the decimal point kept
     */
    @Override
    public int fractionalDigits() {
        return 2;
    }

    /**
     * Format amount into a human readable String This provides translation into
     * economy specific formatting to improve consistency between plugins.
     *
     * @param amount to format
     * @return Human readable string describing amount
     */
    @Override
    public String format(double amount) {
        return amount + "円";
    }

    /**
     * Returns the name of the currency in plural form.
     * If the economy being used does not support currency names then an empty string will be returned.
     *
     * @return name of the currency (plural)
     */
    @Override
    public String currencyNamePlural() {
        return "円";
    }

    /**
     * Returns the name of the currency in singular form.
     * If the economy being used does not support currency names then an empty string will be returned.
     *
     * @return name of the currency (singular)
     */
    @Override
    public String currencyNameSingular() {
        return "銭";
    }

    /**
     * @param playerName
     * @deprecated As of VaultAPI 1.4 use {@link #hasAccount(OfflinePlayer)} instead.
     */
    @Deprecated
    @Override
    public boolean hasAccount(String playerName) {
        return PlayerRepository.hasAccount(Bukkit.getPlayerUniqueId(playerName));
    }

    /**
     * Checks if this player has an account on the server yet
     * This will always return true if the player has joined the server at least once
     * as all major economy plugins auto-generate a player account when the player joins the server
     *
     * @param player to check
     * @return if the player has an account
     */
    @Override
    public boolean hasAccount(OfflinePlayer player) {
        return PlayerRepository.hasAccount(player.getUniqueId());
    }

    /**
     * @param playerName
     * @param worldName
     * @deprecated As of VaultAPI 1.4 use {@link #hasAccount(OfflinePlayer, String)} instead.
     */
    @Deprecated
    @Override
    public boolean hasAccount(String playerName, String worldName) {
        return this.hasAccount(playerName);
    }

    /**
     * Checks if this player has an account on the server yet on the given world
     * This will always return true if the player has joined the server at least once
     * as all major economy plugins auto-generate a player account when the player joins the server
     *
     * @param player    to check in the world
     * @param worldName world-specific account
     * @return if the player has an account
     */
    @Override
    public boolean hasAccount(OfflinePlayer player, String worldName) {
        return this.hasAccount(player);
    }

    /**
     * @param playerName
     * @deprecated As of VaultAPI 1.4 use {@link #getBalance(OfflinePlayer)} instead.
     */
    @Deprecated
    @Override
    public double getBalance(String playerName) {
        return PlayerRepository.getAccountOrCreate(Bukkit.getPlayerUniqueId(playerName)).getBalance().doubleValue();
    }

    /**
     * Gets balance of a player
     *
     * @param player of the player
     * @return Amount currently held in players account
     */
    @Override
    public double getBalance(OfflinePlayer player) {
        return PlayerRepository.getAccountOrCreate(player.getUniqueId()).getBalance().doubleValue();
    }

    /**
     * @param playerName
     * @param world
     * @deprecated As of VaultAPI 1.4 use {@link #getBalance(OfflinePlayer, String)} instead.
     */
    @Deprecated
    @Override
    public double getBalance(String playerName, String world) {
        return this.getBalance(playerName);
    }

    /**
     * Gets balance of a player on the specified world.
     * IMPLEMENTATION SPECIFIC - if an economy plugin does not support this the global balance will be returned.
     *
     * @param player to check
     * @param world  name of the world
     * @return Amount currently held in players account
     */
    @Override
    public double getBalance(OfflinePlayer player, String world) {
        return this.getBalance(player);
    }

    /**
     * @param playerName
     * @param amount
     * @deprecated As of VaultAPI 1.4 use {@link #has(OfflinePlayer, double)} instead.
     */
    @Deprecated
    @Override
    public boolean has(String playerName, double amount) {
        return this.getBalance(playerName) >= amount;
    }

    /**
     * Checks if the player account has the amount - DO NOT USE NEGATIVE AMOUNTS
     *
     * @param player to check
     * @param amount to check for
     * @return True if <b>player</b> has <b>amount</b>, False else wise
     */
    @Override
    public boolean has(OfflinePlayer player, double amount) {
        return this.getBalance(player) >= amount;
    }

    /**
     * @param playerName
     * @param worldName
     * @param amount
     * @deprecated As of VaultAPI 1.4 use @{link {@link #has(OfflinePlayer, String, double)} instead.
     */
    @Deprecated
    @Override
    public boolean has(String playerName, String worldName, double amount) {
        return this.has(playerName, amount);
    }

    /**
     * Checks if the player account has the amount in a given world - DO NOT USE NEGATIVE AMOUNTS
     * IMPLEMENTATION SPECIFIC - if an economy plugin does not support this the global balance will be returned.
     *
     * @param player    to check
     * @param worldName to check with
     * @param amount    to check for
     * @return True if <b>player</b> has <b>amount</b>, False else wise
     */
    @Override
    public boolean has(OfflinePlayer player, String worldName, double amount) {
        return this.has(player, amount);
    }

    /**
     * @param playerName
     * @param amount
     * @deprecated As of VaultAPI 1.4 use {@link #withdrawPlayer(OfflinePlayer, double)} instead.
     */
    @Deprecated
    @Override
    public EconomyResponse withdrawPlayer(String playerName, double amount) {
        try {
            PlayerRepository.getAccountOrCreate(Bukkit.getPlayerUniqueId(playerName)).withdraw(BigDecimal.valueOf(amount));
            return new EconomyResponse(amount, this.getBalance(playerName), EconomyResponse.ResponseType.SUCCESS, "SUCCESS");
        } catch (Throwable t) {
            return new EconomyResponse(amount, this.getBalance(playerName), EconomyResponse.ResponseType.FAILURE, t.getMessage());
        }
    }

    /**
     * Withdraw an amount from a player - DO NOT USE NEGATIVE AMOUNTS
     *
     * @param player to withdraw from
     * @param amount Amount to withdraw
     * @return Detailed response of transaction
     */
    @Override
    public EconomyResponse withdrawPlayer(OfflinePlayer player, double amount) {
        try {
            PlayerRepository.getAccountOrCreate(player.getUniqueId()).withdraw(BigDecimal.valueOf(amount));
            return new EconomyResponse(amount, this.getBalance(player), EconomyResponse.ResponseType.SUCCESS, "SUCCESS");
        } catch (Throwable t) {
            return new EconomyResponse(amount, this.getBalance(player), EconomyResponse.ResponseType.FAILURE, t.getMessage());
        }
    }

    /**
     * @param playerName
     * @param worldName
     * @param amount
     * @deprecated As of VaultAPI 1.4 use {@link #withdrawPlayer(OfflinePlayer, String, double)} instead.
     */
    @Deprecated
    @Override
    public EconomyResponse withdrawPlayer(String playerName, String worldName, double amount) {
        return this.withdrawPlayer(playerName, amount);
    }

    /**
     * Withdraw an amount from a player on a given world - DO NOT USE NEGATIVE AMOUNTS
     * IMPLEMENTATION SPECIFIC - if an economy plugin does not support this the global balance will be returned.
     *
     * @param player    to withdraw from
     * @param worldName - name of the world
     * @param amount    Amount to withdraw
     * @return Detailed response of transaction
     */
    @Override
    public EconomyResponse withdrawPlayer(OfflinePlayer player, String worldName, double amount) {
        return this.withdrawPlayer(player, amount);
    }

    /**
     * @param playerName
     * @param amount
     * @deprecated As of VaultAPI 1.4 use {@link #depositPlayer(OfflinePlayer, double)} instead.
     */
    @Deprecated
    @Override
    public EconomyResponse depositPlayer(String playerName, double amount) {
        try {
            PlayerRepository.getAccountOrCreate(Bukkit.getPlayerUniqueId(playerName)).deposit(BigDecimal.valueOf(amount));
            return new EconomyResponse(amount, this.getBalance(playerName), EconomyResponse.ResponseType.SUCCESS, "SUCCESS");
        } catch(Throwable t) {
            return new EconomyResponse(amount, this.getBalance(playerName), EconomyResponse.ResponseType.FAILURE, t.getMessage());
        }
    }

    /**
     * Deposit an amount to a player - DO NOT USE NEGATIVE AMOUNTS
     *
     * @param player to deposit to
     * @param amount Amount to deposit
     * @return Detailed response of transaction
     */
    @Override
    public EconomyResponse depositPlayer(OfflinePlayer player, double amount) {
        try {
            PlayerRepository.getAccountOrCreate(player.getUniqueId()).deposit(BigDecimal.valueOf(amount));
            return new EconomyResponse(amount, this.getBalance(player), EconomyResponse.ResponseType.SUCCESS, "SUCCESS");
        } catch (Throwable t) {
            return new EconomyResponse(amount, this.getBalance(player), EconomyResponse.ResponseType.FAILURE, t.getMessage());
        }
    }

    /**
     * @param playerName
     * @param worldName
     * @param amount
     * @deprecated As of VaultAPI 1.4 use {@link #depositPlayer(OfflinePlayer, String, double)} instead.
     */
    @Deprecated
    @Override
    public EconomyResponse depositPlayer(String playerName, String worldName, double amount) {
        return this.depositPlayer(playerName, amount);
    }

    /**
     * Deposit an amount to a player - DO NOT USE NEGATIVE AMOUNTS
     * IMPLEMENTATION SPECIFIC - if an economy plugin does not support this the global balance will be returned.
     *
     * @param player    to deposit to
     * @param worldName name of the world
     * @param amount    Amount to deposit
     * @return Detailed response of transaction
     */
    @Override
    public EconomyResponse depositPlayer(OfflinePlayer player, String worldName, double amount) {
        return this.depositPlayer(player, amount);
    }

    private static EconomyResponse bankNotImplemented() {
        return new EconomyResponse(0, 0, EconomyResponse.ResponseType.FAILURE, "NOT_IMPLEMENTED_BANK");
    }

    /**
     * @param name
     * @param player
     * @deprecated As of VaultAPI 1.4 use {{@link #createBank(String, OfflinePlayer)} instead.
     */
    @Deprecated
    @Override
    public EconomyResponse createBank(String name, String player) {
        return bankNotImplemented();
    }

    /**
     * Creates a bank account with the specified name and the player as the owner
     *
     * @param name   of account
     * @param player the account should be linked to
     * @return EconomyResponse Object
     */
    @Override
    public EconomyResponse createBank(String name, OfflinePlayer player) {
        return bankNotImplemented();
    }

    /**
     * Deletes a bank account with the specified name.
     *
     * @param name of the back to delete
     * @return if the operation completed successfully
     */
    @Override
    public EconomyResponse deleteBank(String name) {
        return bankNotImplemented();
    }

    /**
     * Returns the amount the bank has
     *
     * @param name of the account
     * @return EconomyResponse Object
     */
    @Override
    public EconomyResponse bankBalance(String name) {
        return bankNotImplemented();
    }

    /**
     * Returns true or false whether the bank has the amount specified - DO NOT USE NEGATIVE AMOUNTS
     *
     * @param name   of the account
     * @param amount to check for
     * @return EconomyResponse Object
     */
    @Override
    public EconomyResponse bankHas(String name, double amount) {
        return bankNotImplemented();
    }

    /**
     * Withdraw an amount from a bank account - DO NOT USE NEGATIVE AMOUNTS
     *
     * @param name   of the account
     * @param amount to withdraw
     * @return EconomyResponse Object
     */
    @Override
    public EconomyResponse bankWithdraw(String name, double amount) {
        return bankNotImplemented();
    }

    /**
     * Deposit an amount into a bank account - DO NOT USE NEGATIVE AMOUNTS
     *
     * @param name   of the account
     * @param amount to deposit
     * @return EconomyResponse Object
     */
    @Override
    public EconomyResponse bankDeposit(String name, double amount) {
        return bankNotImplemented();
    }

    /**
     * @param name
     * @param playerName
     * @deprecated As of VaultAPI 1.4 use {{@link #isBankOwner(String, OfflinePlayer)} instead.
     */
    @Deprecated
    @Override
    public EconomyResponse isBankOwner(String name, String playerName) {
        return bankNotImplemented();
    }

    /**
     * Check if a player is the owner of a bank account
     *
     * @param name   of the account
     * @param player to check for ownership
     * @return EconomyResponse Object
     */
    @Override
    public EconomyResponse isBankOwner(String name, OfflinePlayer player) {
        return bankNotImplemented();
    }

    /**
     * @param name
     * @param playerName
     * @deprecated As of VaultAPI 1.4 use {{@link #isBankMember(String, OfflinePlayer)} instead.
     */
    @Deprecated
    @Override
    public EconomyResponse isBankMember(String name, String playerName) {
        return bankNotImplemented();
    }

    /**
     * Check if the player is a member of the bank account
     *
     * @param name   of the account
     * @param player to check membership
     * @return EconomyResponse Object
     */
    @Override
    public EconomyResponse isBankMember(String name, OfflinePlayer player) {
        return bankNotImplemented();
    }

    /**
     * Gets the list of banks
     *
     * @return the List of Banks
     */
    @Override
    public List<String> getBanks() {
        return Collections.emptyList();
    }

    /**
     * @param playerName
     * @deprecated As of VaultAPI 1.4 use {{@link #createPlayerAccount(OfflinePlayer)} instead.
     */
    @Deprecated
    @Override
    public boolean createPlayerAccount(String playerName) {
        try {
            return PlayerRepository.getAccountOrCreate(Bukkit.getPlayerUniqueId(playerName)) != null;
        } catch (Throwable t) {
            return false;
        }
    }

    /**
     * Attempts to create a player account for the given player
     *
     * @param player OfflinePlayer
     * @return if the account creation was successful
     */
    @Override
    public boolean createPlayerAccount(OfflinePlayer player) {
        try {
            return PlayerRepository.getAccountOrCreate(player.getUniqueId()) != null;
        } catch (Throwable t) {
            return false;
        }
    }

    /**
     * @param playerName
     * @param worldName
     * @deprecated As of VaultAPI 1.4 use {{@link #createPlayerAccount(OfflinePlayer, String)} instead.
     */
    @Deprecated
    @Override
    public boolean createPlayerAccount(String playerName, String worldName) {
        return this.createPlayerAccount(playerName);
    }

    /**
     * Attempts to create a player account for the given player on the specified world
     * IMPLEMENTATION SPECIFIC - if an economy plugin does not support this then false will always be returned.
     *
     * @param player    OfflinePlayer
     * @param worldName String name of the world
     * @return if the account creation was successful
     */
    @Override
    public boolean createPlayerAccount(OfflinePlayer player, String worldName) {
        return this.createPlayerAccount(player);
    }
}
