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

package net.unknown.proxy.providers;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.unknown.provider.DataProvider;
import net.unknown.provider.requests.EmptyRequest;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;

import java.util.Map;

public class PlayerCountProvider implements DataProvider<EmptyRequest, Map<String, Object>> {

    @SuppressWarnings("deprecation")
    @Override
    public NamespacedKey id() {
        return new NamespacedKey("proxy", "player_count");
    }

    @Override
    public EmptyRequest parseRequest(Map<String, String> queryParams) {
        return new EmptyRequest();
    }

    @Override
    public Map<String, Object> provide(EmptyRequest request) {
        return Map.of(
                "count", Bukkit.getOnlinePlayers().size()
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
