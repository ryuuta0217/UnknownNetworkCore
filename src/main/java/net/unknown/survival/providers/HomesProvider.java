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
import net.unknown.UnknownNetworkCorePlugin;
import net.unknown.provider.DataProvider;
import net.unknown.provider.requests.PlayerTargetedRequest;
import net.unknown.provider.util.NamespacedKey;
import net.unknown.survival.data.PlayerData;
import net.unknown.survival.data.model.Home;
import net.unknown.survival.data.model.HomeGroup;
import org.bukkit.Bukkit;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class HomesProvider implements DataProvider<PlayerTargetedRequest, List<HomesProvider.HomeEntry>> {
    public record HomeEntry(String name, String group, String world, double x, double y, double z, float yaw, float pitch) {}

    @Override
    public NamespacedKey id() {
        return new NamespacedKey("survival", "player/homes");
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
    public List<HomeEntry> provide(PlayerTargetedRequest request) {
        UUID uuid = request.target();
        if (Bukkit.isPrimaryThread()) {
            return readHomes(uuid);
        }
        try {
            return Bukkit.getScheduler()
                    .callSyncMethod(UnknownNetworkCorePlugin.getInstance(), () -> readHomes(uuid))
                    .get(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return Collections.emptyList();
        } catch (ExecutionException | TimeoutException e) {
            throw new IllegalStateException("Failed to retrieve player homes on main thread", e);
        }
    }

    @Override
    public JsonElement serialize(List<HomeEntry> data) {
        JsonObject root = new JsonObject();
        JsonArray homesArray = new JsonArray();
        if (data != null) {
            for (HomeEntry entry : data) {
                JsonObject obj = new JsonObject();
                obj.addProperty("name", entry.name());
                obj.addProperty("group", entry.group());
                obj.addProperty("world", entry.world());
                obj.addProperty("x", entry.x());
                obj.addProperty("y", entry.y());
                obj.addProperty("z", entry.z());
                obj.addProperty("yaw", entry.yaw());
                obj.addProperty("pitch", entry.pitch());
                homesArray.add(obj);
            }
        }
        root.add("homes", homesArray);
        return root;
    }

    private static List<HomeEntry> readHomes(UUID uuid) {
        boolean isOnline = Bukkit.getPlayer(uuid) != null;
        File playerFile = new File(UnknownNetworkCorePlugin.getInstance().getDataFolder(), "players/" + uuid + ".yml");
        if (!isOnline && !PlayerData.getAll().containsKey(uuid) && !playerFile.exists()) {
            return Collections.emptyList();
        }

        PlayerData.HomeData homeData = PlayerData.of(uuid).getHomeData();
        if (homeData == null || homeData.getGroups() == null) {
            return Collections.emptyList();
        }

        String defaultGroupName = "default";
        try {
            HomeGroup defaultGroup = homeData.getDefaultGroup();
            if (defaultGroup != null && defaultGroup.getName() != null) {
                defaultGroupName = defaultGroup.getName();
            }
        } catch (IllegalStateException ignored) {
        }

        List<HomeEntry> entries = new ArrayList<>();
        for (Map.Entry<String, HomeGroup> groupEntry : homeData.getGroups().entrySet()) {
            String groupName = groupEntry.getKey();
            HomeGroup group = groupEntry.getValue();
            if (group == null) continue;
            for (Home home : group.getHomes().values()) {
                String displayName = groupName.equals(defaultGroupName) ? home.name() : groupName + ":" + home.name();
                entries.add(new HomeEntry(
                        displayName,
                        groupName,
                        home.worldName(),
                        home.x(),
                        home.y(),
                        home.z(),
                        home.yaw(),
                        home.pitch()
                ));
            }
        }
        return entries;
    }
}
