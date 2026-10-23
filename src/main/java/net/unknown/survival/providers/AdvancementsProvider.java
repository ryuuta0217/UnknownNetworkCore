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
import com.google.gson.JsonParser;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.PlayerAdvancements;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.storage.LevelResource;
import net.unknown.UnknownNetworkCorePlugin;
import net.unknown.provider.DataProvider;
import net.unknown.provider.requests.PlayerTargetedRequest;
import net.unknown.provider.util.NamespacedKey;
import org.bukkit.Bukkit;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class AdvancementsProvider implements DataProvider<PlayerTargetedRequest, JsonObject> {
    private static final Field CODEC_FIELD;
    private static final Method AS_DATA_METHOD;

    static {
        try {
            CODEC_FIELD = PlayerAdvancements.class.getDeclaredField("codec");
            CODEC_FIELD.setAccessible(true);
            AS_DATA_METHOD = PlayerAdvancements.class.getDeclaredMethod("asData");
            AS_DATA_METHOD.setAccessible(true);
        } catch (NoSuchFieldException | NoSuchMethodException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    @Override
    public NamespacedKey id() {
        return new NamespacedKey("survival", "player/advancements");
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
    public JsonObject provide(PlayerTargetedRequest request) {
        return getPlayerAdvancementsJson(request.target());
    }

    @Override
    public JsonElement serialize(JsonObject data) {
        return data;
    }

    public static JsonObject getPlayerAdvancementsJson(UUID uuid) {
        if (MinecraftServer.getServer().getPlayerList().getPlayer(uuid) != null) {
            if (Bukkit.isPrimaryThread()) {
                ServerPlayer player = MinecraftServer.getServer().getPlayerList().getPlayer(uuid);
                if (player != null) {
                    return invokeToJson(player.getAdvancements());
                }
            } else {
                try {
                    JsonObject onlineAdvancements = Bukkit.getScheduler().callSyncMethod(UnknownNetworkCorePlugin.getInstance(), () -> {
                        ServerPlayer player = MinecraftServer.getServer().getPlayerList().getPlayer(uuid);
                        return player != null ? invokeToJson(player.getAdvancements()) : null;
                    }).get(5, TimeUnit.SECONDS);
                    if (onlineAdvancements != null) {
                        return onlineAdvancements;
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } catch (ExecutionException | TimeoutException e) {
                    throw new IllegalStateException("Failed to retrieve online player advancements on main thread", e);
                }
            }
        }

        Path advancementsFile = MinecraftServer.getServer().getWorldPath(LevelResource.PLAYER_ADVANCEMENTS_DIR).resolve(uuid + ".json");
        if (Files.isRegularFile(advancementsFile)) {
            try {
                JsonElement element = JsonParser.parseString(Files.readString(advancementsFile, StandardCharsets.UTF_8));
                if (element != null && element.isJsonObject()) {
                    return element.getAsJsonObject();
                }
            } catch (IOException e) {
                throw new IllegalStateException("Failed to read offline player advancements file for " + uuid, e);
            }
        }

        return new JsonObject();
    }

    @SuppressWarnings("unchecked")
    private static JsonObject invokeToJson(PlayerAdvancements advancements) {
        try {
            Codec<Object> codec = (Codec<Object>) CODEC_FIELD.get(advancements);
            Object data = AS_DATA_METHOD.invoke(advancements);
            JsonElement element = codec.encodeStart(JsonOps.INSTANCE, data).getOrThrow();
            return element.isJsonObject() ? element.getAsJsonObject() : new JsonObject();
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new IllegalStateException("Failed to serialize PlayerAdvancements to JSON", e);
        }
    }
}
