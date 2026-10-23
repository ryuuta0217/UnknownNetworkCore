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
import net.unknown.provider.DataProvider;
import net.unknown.provider.requests.PlayerTargetedRequest;
import net.unknown.provider.util.NamespacedKey;
import net.unknown.shared.util.NameHistory;

import java.util.Comparator;
import java.util.Map;
import java.util.UUID;

public class NameHistoryProvider implements DataProvider<PlayerTargetedRequest, Map<String, Long>> {
    @Override
    public NamespacedKey id() {
        return new NamespacedKey("survival", "player/name_history");
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
    public Map<String, Long> provide(PlayerTargetedRequest request) {
        return NameHistory.getNameHistory(request.target());
    }

    @Override
    public JsonElement serialize(Map<String, Long> data) {
        JsonObject root = new JsonObject();
        JsonObject historyObj = new JsonObject();
        JsonArray entriesArray = new JsonArray();

        if (data != null) {
            data.entrySet().stream()
                    .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
                    .forEach(entry -> {
                        historyObj.addProperty(entry.getKey(), entry.getValue());
                        JsonObject item = new JsonObject();
                        item.addProperty("name", entry.getKey());
                        item.addProperty("last_seen", entry.getValue());
                        entriesArray.add(item);
                    });
        }

        root.add("history", historyObj);
        root.add("entries", entriesArray);
        return root;
    }
}
