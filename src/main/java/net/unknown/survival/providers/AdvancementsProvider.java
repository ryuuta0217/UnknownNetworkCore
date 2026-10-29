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

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.JsonOps;
import net.minecraft.advancements.Advancement;
import net.minecraft.resources.RegistryOps;
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
        UUID uuid = request.target();
        if (Bukkit.isPrimaryThread()) {
            return buildAdvancementsData(uuid);
        }
        try {
            return Bukkit.getScheduler().callSyncMethod(UnknownNetworkCorePlugin.getInstance(), () -> buildAdvancementsData(uuid)).get(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return buildFallbackAdvancementsData(uuid);
        } catch (ExecutionException | TimeoutException e) {
            UnknownNetworkCorePlugin.getInstance().getLogger().log(java.util.logging.Level.WARNING, "Failed to retrieve advancements for player " + uuid, e);
            return buildFallbackAdvancementsData(uuid);
        }
    }

    @Override
    public JsonElement serialize(JsonObject data) {
        return data != null ? data : new JsonObject();
    }

    private static JsonObject buildFallbackAdvancementsData(UUID uuid) {
        JsonObject root = new JsonObject();
        root.add("player", getPlayerAdvancementsJson(uuid));
        root.add("advancements", new JsonArray());
        return root;
    }

    private static JsonObject buildAdvancementsData(UUID uuid) {
        JsonObject root = new JsonObject();
        root.add("player", getPlayerAdvancementsJson(uuid));

        MinecraftServer server = MinecraftServer.getServer();
        if (server == null || server.getAdvancements() == null || server.getAdvancements().tree() == null) {
            return root;
        }

        RegistryOps<JsonElement> registryOps = server.registryAccess().createSerializationContext(JsonOps.INSTANCE);
        JsonArray advancements = new JsonArray();
        server.getAdvancements().tree().nodes().forEach(node -> {
            if (node.advancement().display().isEmpty()) return;
            if (node.root() != null && node.root().holder().id().getPath().startsWith("recipes/")) return;

            try {
                JsonObject advObj = Advancement.CODEC.encodeStart(registryOps, node.advancement())
                        .getOrThrow()
                        .getAsJsonObject();

                advObj.addProperty("id", node.holder().id().toString());
                advObj.addProperty("x", node.x());
                advObj.addProperty("y", node.y());
                if (node.root() != null) {
                    advObj.addProperty("root", node.root().holder().id().toString());
                }

                advancements.add(advObj);
            } catch (Exception e) {
                UnknownNetworkCorePlugin.getInstance().getLogger().warning("Failed to serialize advancement " + node.holder().id() + ": " + e.getMessage());
            }
        });

        root.add("advancements", advancements);
        return root;
    }

    public static JsonObject getPlayerAdvancementsJson(UUID uuid) {
        MinecraftServer server = MinecraftServer.getServer();
        if (server != null && server.getPlayerList() != null) {
            ServerPlayer player = server.getPlayerList().getPlayer(uuid);
            if (player != null) {
                if (Bukkit.isPrimaryThread()) {
                    return serializeOnlinePlayerAdvancements(player.getAdvancements());
                }
                try {
                    return Bukkit.getScheduler().callSyncMethod(UnknownNetworkCorePlugin.getInstance(), () -> serializeOnlinePlayerAdvancements(player.getAdvancements())).get(5, TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } catch (ExecutionException | TimeoutException e) {
                    throw new IllegalStateException("Failed to retrieve online player advancements on main thread", e);
                }
            }
        }

        if (server != null) {
            Path file = server.getWorldPath(LevelResource.PLAYER_ADVANCEMENTS_DIR).resolve(uuid + ".json");
            if (Files.isRegularFile(file)) {
                try {
                    JsonElement element = JsonParser.parseString(Files.readString(file, StandardCharsets.UTF_8));
                    if (element != null && element.isJsonObject()) {
                        return element.getAsJsonObject();
                    }
                } catch (IOException e) {
                    throw new IllegalStateException("Failed to read offline player advancements file for " + uuid, e);
                }
            }
        }

        return new JsonObject();
    }

    @SuppressWarnings("unchecked")
    private static JsonObject serializeOnlinePlayerAdvancements(PlayerAdvancements advancements) {
        try {
            Field codecField = PlayerAdvancements.class.getDeclaredField("codec");
            codecField.setAccessible(true);
            Method asDataMethod = PlayerAdvancements.class.getDeclaredMethod("asData");
            asDataMethod.setAccessible(true);

            Codec<Object> codec = (Codec<Object>) codecField.get(advancements);
            Object data = asDataMethod.invoke(advancements);
            MinecraftServer server = MinecraftServer.getServer();
            DynamicOps<JsonElement> ops = server != null ? server.registryAccess().createSerializationContext(JsonOps.INSTANCE) : JsonOps.INSTANCE;
            JsonElement element = codec.encodeStart(ops, data).getOrThrow();
            return element.isJsonObject() ? element.getAsJsonObject() : new JsonObject();
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Failed to serialize PlayerAdvancements to JSON", e);
        }
    }
}
