/*
 * Copyright (C) 2023 Ryuta Iwakura (ryuuta0217)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation version 3 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.ryuuta0217.util;

import org.json.JSONObject;

import java.io.IOException;
import java.util.UUID;

// See: https://wiki.vg/Mojang_API
public class MojangApi {
    // Globally On Error json: {"path": "", "errorMessage":"<error message>"}

    // GET https://sessionserver.mojang.com/session/minecraft/profile/<uuid>
    // response json: {"id":"<profile identifier>","name":"<player name>","properties":[{"name":"textures","value":"<base64 string>","signature":"<base64 string; signed data using Yggdrasil's private key>"}]}
    public static String getName(UUID uniqueId) throws MojangException{
        try {
            String responseRaw = HTTPFetch.fetchGet("https://sessionserver.mojang.com/session/minecraft/profile/" + uniqueId.toString()).sentAndReadAsString();
            JSONObject response = new JSONObject(responseRaw);
            if (response.has("name")) {
                return response.getString("name");
            } else if (response.has("errorMessage")) {
                throw new MojangException(response.getString("errorMessage"));
            } else {
                throw new IllegalStateException("Unknown response received: " + responseRaw);
            }
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }


    // On Success json: {"id":"<uuid without hyphens>","name":"<name>"}
    public static UUID getUUID(String name) throws MojangException {
        try {
            String responseRaw = HTTPFetch.fetchGet("https://api.mojang.com/users/profiles/minecraft/" + name).sentAndReadAsString();
            JSONObject response = new JSONObject(responseRaw);
            if (response.has("id")) {
                return UUID.fromString(response.getString("id").replaceAll("(\\w{8})(\\w{4})(\\w{4})(\\w{4})(\\w{12})", "$1-$2-$3-$4-$5"));
            } else if (response.has("errorMessage")) {
                throw new MojangException(response.getString("errorMessage"));
            } else {
                throw new IllegalStateException("Unknown response received: " + responseRaw);
            }
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    public static class MojangException extends IllegalArgumentException {
        private MojangException(String message) {
            super(message);
        }
    }
}
