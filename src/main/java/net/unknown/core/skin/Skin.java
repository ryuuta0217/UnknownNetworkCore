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

package net.unknown.core.skin;

import com.destroystokyo.paper.profile.PlayerProfile;
import com.destroystokyo.paper.profile.ProfileProperty;
import com.sk89q.worldedit.bukkit.BukkitConfiguration;
import org.bukkit.configuration.Configuration;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.MemorySection;
import org.bukkit.configuration.file.YamlConfiguration;

import javax.annotation.Nullable;

public record Skin(SkinSource source, String base64, String signature) {
    public ProfileProperty asProfileProperty() {
        return new ProfileProperty("textures", base64, signature);
    }

    public void asConfig(ConfigurationSection output) {
        output.set("source", this.source.name());
        output.set("base64", this.base64);
        output.set("signature", this.signature);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Skin skin) this.equals(skin);
        if (obj instanceof ProfileProperty property) this.equals(property);
        if (obj instanceof PlayerProfile profile) this.equals(profile);
        return false;
    }

    public boolean equals(Skin skin) {
        return this.base64.equals(skin.base64) && this.signature.equals(skin.signature);
    }

    public boolean equals(ProfileProperty property) {
        return this.base64.equals(property.getValue()) && this.signature.equals(property.getSignature());
    }

    public boolean equals(PlayerProfile profile) {
        return profile.getProperties().stream().anyMatch(this::equals);
    }

    @Nullable
    public static Skin readFromConfig(ConfigurationSection readFrom) {
        if (readFrom.contains("source") && readFrom.contains("base64") && readFrom.contains("signature")) {
            return new Skin(SkinSource.valueOf(readFrom.getString("source")), readFrom.getString("base64"), readFrom.getString("signature"));
        } else {
            return null;
        }
    }
}
