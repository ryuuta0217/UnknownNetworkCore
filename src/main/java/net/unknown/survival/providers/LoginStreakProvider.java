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
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import net.unknown.UnknownNetworkCorePlugin;
import net.unknown.provider.DataProvider;
import net.unknown.provider.requests.PlayerTargetedRequest;
import net.unknown.provider.util.NamespacedKey;
import net.unknown.survival.data.PlayerData;
import net.unknown.survival.listeners.PlayerJoinListener;
import org.bukkit.Bukkit;

import java.io.File;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class LoginStreakProvider implements DataProvider<PlayerTargetedRequest, Integer> {
    @Override
    public NamespacedKey id() {
        return new NamespacedKey("survival", "player/stats/login_streak");
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
    public Integer provide(PlayerTargetedRequest request) {
        UUID uuid = request.target();
        if (Bukkit.isPrimaryThread()) {
            return readLoginStreak(uuid);
        }
        try {
            return Bukkit.getScheduler().callSyncMethod(UnknownNetworkCorePlugin.getInstance(), () -> readLoginStreak(uuid)).get(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return null;
        } catch (ExecutionException | TimeoutException e) {
            throw new IllegalStateException("Failed to retrieve player login streak on main thread", e);
        }
    }

    @Override
    public JsonElement serialize(Integer data) {
        JsonObject json = new JsonObject();
        if (data != null) {
            json.addProperty("loginStreak", data);
        } else {
            json.add("loginStreak", JsonNull.INSTANCE);
        }
        return json;
    }

    private static Integer readLoginStreak(UUID uuid) {
        boolean isOnline = Bukkit.getPlayer(uuid) != null;
        File playerFile = new File(UnknownNetworkCorePlugin.getInstance().getDataFolder(), "players/" + uuid + ".yml");
        if (!isOnline && !PlayerData.getAll().containsKey(uuid) && !playerFile.exists()) {
            return null;
        }

        PlayerData.PlayerRegistry registries = PlayerData.of(uuid).getRegistries();
        String rawDay = registries.get(PlayerJoinListener.LOGIN_STREAK_REGISTRY_KEY, "last_login_day");
        String rawStreak = registries.get(PlayerJoinListener.LOGIN_STREAK_REGISTRY_KEY, "streak");
        if (rawDay == null || rawStreak == null) {
            return isOnline ? 1 : null;
        }

        try {
            long lastDay = Long.parseLong(rawDay);
            int streak = Integer.parseInt(rawStreak);
            long today = LocalDate.now(ZoneId.systemDefault()).toEpochDay();
            if (lastDay >= today - 1) {
                return Math.max(1, streak);
            }
            return isOnline ? 1 : 0;
        } catch (NumberFormatException e) {
            return isOnline ? 1 : null;
        }
    }
}
