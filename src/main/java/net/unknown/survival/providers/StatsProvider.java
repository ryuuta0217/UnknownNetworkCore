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
import com.mojang.authlib.GameProfile;
import net.minecraft.server.MinecraftServer;
import net.minecraft.stats.ServerStatsCounter;
import net.unknown.provider.DataProvider;
import net.unknown.provider.requests.PlayerTargetedRequest;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class StatsProvider implements DataProvider<PlayerTargetedRequest, ServerStatsCounter> {
    @SuppressWarnings("deprecation")
    @Override
    public NamespacedKey id() {
        return new NamespacedKey("survival", "player/stats");
    }

    @Override
    public PlayerTargetedRequest parseRequest(Map<String, String> queryParams) {
        if (!queryParams.containsKey("target")) throw new IllegalArgumentException("Missing required query parameter: target");
        return new PlayerTargetedRequest(UUID.fromString(queryParams.get("target")));
    }

    @Override
    public ServerStatsCounter provide(PlayerTargetedRequest request) {
        if (Bukkit.getOfflinePlayer(request.target()).isOnline()) {
            return MinecraftServer.getServer().getPlayerList().getPlayer(request.target()).getStats();
        } else {
            return MinecraftServer.getServer().getPlayerList().getPlayerStats(new GameProfile(request.target(), Optional.ofNullable(Bukkit.getOfflinePlayer(request.target()).getName()).orElse("Unknown")));
        }
    }

    @Override
    public JsonElement serialize(ServerStatsCounter data) {
        try {
            Method toJson = ServerStatsCounter.class.getDeclaredMethod("toJson");
            toJson.setAccessible(true);
            return (JsonElement) toJson.invoke(data);
        } catch(NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            throw new IllegalStateException(e);
        }
    }
}
