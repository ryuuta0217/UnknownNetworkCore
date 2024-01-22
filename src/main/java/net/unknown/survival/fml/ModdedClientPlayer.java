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

package net.unknown.survival.fml;

import net.unknown.shared.enums.ConnectionEnvironment;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class ModdedClientPlayer {
    private final ConnectionEnvironment env;
    private final Map<String, String> mods;
    private final Map<String, String> channels;
    private final Map<String, String> registries;
    private Map<String, String> withoutForgeChannels;

    public ModdedClientPlayer(ConnectionEnvironment env, Set<String> mods, Map<String, String> channels, Map<String, String> registries) {
        this(env, new HashMap<>() {{
            mods.forEach(modName -> put(modName, ""));
        }}, channels, registries);
    }

    public ModdedClientPlayer(ConnectionEnvironment env, Map<String, String> mods, Map<String, String> channels, Map<String, String> registries) {
        this.env = env;
        this.mods = mods;
        this.channels = channels;
        this.registries = registries;
    }

    public ConnectionEnvironment getEnvironment() {
        return this.env;
    }

    public Map<String, String> getMods() {
        return this.mods;
    }

    public Set<String> getModNames() {
        return this.mods.keySet();
    }

    public Map<String, String> getChannels() {
        return this.channels;
    }

    public Map<String, String> getWithoutForgeChannels() {
        if (this.withoutForgeChannels == null) {
            this.withoutForgeChannels = new HashMap<>(this.channels);
            this.channels.remove("fml:loginwrapper");
            this.channels.remove("fml:play");
            this.channels.remove("fml:handshake");
            this.channels.remove("forge:split");
            this.channels.remove("forge:split_11");
            if (this.channels.getOrDefault("minecraft:unregister", "").equals("FML2")) {
                this.withoutForgeChannels.remove("minecraft:unregister");
            }
            if (this.channels.getOrDefault("minecraft:register", "").equals("FML2")) {
                this.withoutForgeChannels.remove("minecraft:register");
            }
        }

        return this.withoutForgeChannels;
    }

    public Map<String, String> getRegistries() {
        return this.registries;
    }
}
