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
