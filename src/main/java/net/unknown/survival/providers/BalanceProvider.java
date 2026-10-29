/*
 * Copyright (c) 2026 Unknown Network Developers and contributors.
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

package net.unknown.survival.providers;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.unknown.UnknownNetworkCorePlugin;
import net.unknown.provider.DataProvider;
import net.unknown.provider.requests.PlayerTargetedRequest;
import net.unknown.provider.util.NamespacedKey;
import net.unknown.survival.economy.repository.PlayerRepository;
import net.unknown.survival.wrapper.economy.WrappedEconomy;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class BalanceProvider implements DataProvider<PlayerTargetedRequest, Double> {
    @Override
    public NamespacedKey id() {
        return new NamespacedKey("survival", "player/economy/balance");
    }

    @Override
    public PlayerTargetedRequest parseRequest(Map<String, String> queryParams) {
        String rawUuid = queryParams.getOrDefault("uuid", queryParams.get("target"));
        if (rawUuid == null || rawUuid.isEmpty()) {
            throw new IllegalArgumentException("Missing required query parameter: uuid");
        }
        return new PlayerTargetedRequest(UUID.fromString(rawUuid));
    }

    @Override
    public Double provide(PlayerTargetedRequest request) {
        UUID uuid = request.target();
        synchronized (PlayerRepository.class) {
            if (PlayerRepository.hasAccount(uuid)) {
                PlayerRepository repo = PlayerRepository.getAccount(uuid);
                if (repo != null) {
                    return repo.getBalance().doubleValue();
                }
            }
        }

        if (WrappedEconomy.INSTANCE != null) {
            if (Bukkit.isPrimaryThread()) {
                OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(uuid);
                return WrappedEconomy.INSTANCE.hasAccount(offlinePlayer) ? WrappedEconomy.INSTANCE.getBalance(offlinePlayer) : 0.0D;
            }
            try {
                return Bukkit.getScheduler().callSyncMethod(UnknownNetworkCorePlugin.getInstance(), () -> {
                    OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(uuid);
                    return WrappedEconomy.INSTANCE.hasAccount(offlinePlayer) ? WrappedEconomy.INSTANCE.getBalance(offlinePlayer) : 0.0D;
                }).get(5, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } catch (ExecutionException | TimeoutException e) {
                throw new IllegalStateException("Failed to retrieve player balance on main thread", e);
            }
        }

        return 0.0D;
    }

    @Override
    public JsonElement serialize(Double data) {
        JsonObject json = new JsonObject();
        double balance = data != null ? data : 0.0D;
        json.addProperty("balance", balance);

        String major = "円";
        String minor = "銭";
        int fractionalDigits = 2;

        if (WrappedEconomy.INSTANCE != null) {
            String p = WrappedEconomy.INSTANCE.currencyNamePlural();
            if (p != null && !p.isEmpty() && !p.equals("$")) {
                major = p;
            }
            fractionalDigits = WrappedEconomy.INSTANCE.fractionalDigits();
        }

        json.addProperty("formatted", formatCurrency(balance, major, minor));

        JsonObject cur = new JsonObject();
        cur.addProperty("major", major);
        cur.addProperty("minor", minor);
        cur.addProperty("plural", major);
        cur.addProperty("singular", minor);
        cur.addProperty("fractionalDigits", fractionalDigits);
        json.add("currency", cur);

        return json;
    }

    public static String formatCurrency(double amount, String major, String minor) {
        boolean negative = amount < 0;
        BigDecimal bd = BigDecimal.valueOf(Math.abs(amount)).setScale(2, RoundingMode.HALF_UP);
        long yen = bd.longValue();
        long sen = bd.remainder(BigDecimal.ONE).movePointRight(2).longValue();

        StringBuilder sb = new StringBuilder();
        if (negative) sb.append("-");
        sb.append(String.format("%,d", yen)).append(major);

        if (sen > 0) {
            sb.append(" ").append(sen).append(minor);
        }
        return sb.toString();
    }
}
