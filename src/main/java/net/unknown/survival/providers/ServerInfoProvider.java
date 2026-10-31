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

package net.unknown.survival.providers;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.SharedConstants;
import net.unknown.provider.DataProvider;
import net.unknown.provider.requests.EmptyRequest;
import net.unknown.provider.util.NamespacedKey;
import org.bukkit.Bukkit;

import java.util.Map;

public class ServerInfoProvider implements DataProvider<EmptyRequest, Map<String, Object>> {
    @Override
    public NamespacedKey id() {
        return new NamespacedKey("survival", "server/info");
    }

    @Override
    public EmptyRequest parseRequest(Map<String, String> queryParams) {
        return new EmptyRequest();
    }

    @Override
    public Map<String, Object> provide(EmptyRequest request) {
        Runtime runtime = Runtime.getRuntime();
        return Map.ofEntries(
                Map.entry("version", Bukkit.getVersion()),
                Map.entry("bukkit_version", Bukkit.getBukkitVersion()),
                Map.entry("minecraft_version", SharedConstants.getCurrentVersion().id()),
                Map.entry("minecraft_protocol_version", SharedConstants.getCurrentVersion().protocolVersion()),
                Map.entry("motd", Bukkit.getMotd()),
                Map.entry("online_players", Bukkit.getOnlinePlayers().size()),
                Map.entry("max_players", Bukkit.getMaxPlayers()),
                Map.entry("tps", Bukkit.getTPS()[0]), // Paper API: 1m TPS
                Map.entry("memory_max", runtime.maxMemory()),
                Map.entry("memory_total", runtime.totalMemory()),
                Map.entry("memory_free", runtime.freeMemory()),
                Map.entry("cpu_cores", runtime.availableProcessors())
        );
    }

    @Override
    public JsonElement serialize(Map<String, Object> data) {
        JsonObject json = new JsonObject();
        for (Map.Entry<String, Object> entry : data.entrySet()) {
            Object value = entry.getValue();
            if (value instanceof Number) {
                json.addProperty(entry.getKey(), (Number) value);
            } else if (value instanceof Boolean) {
                json.addProperty(entry.getKey(), (Boolean) value);
            } else {
                json.addProperty(entry.getKey(), String.valueOf(value));
            }
        }
        return json;
    }
}
