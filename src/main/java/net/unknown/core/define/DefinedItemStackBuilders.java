/*
 * Copyright (c) 2022 Unknown Network Developers and contributors.
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

package net.unknown.core.define;

import com.destroystokyo.paper.profile.CraftPlayerProfile;
import com.destroystokyo.paper.profile.PlayerProfile;
import com.destroystokyo.paper.profile.ProfileProperty;
import net.unknown.core.builder.ItemStackBuilder;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.UUID;

public class DefinedItemStackBuilders {
    public static ItemStackBuilder upArrow() {
        return new ItemStackBuilder(Material.PLAYER_HEAD).custom(is -> {
            SkullMeta meta = (SkullMeta) is.getItemMeta();
            meta.setOwningPlayer(Bukkit.getOfflinePlayer(Bukkit.getPlayerUniqueId("MHF_ArrowUp")));
            is.setItemMeta(meta);
        });
    }

    public static ItemStackBuilder leftArrow() {
        return new ItemStackBuilder(Material.PLAYER_HEAD).custom(is -> {
            SkullMeta meta = (SkullMeta) is.getItemMeta();
            meta.setOwningPlayer(Bukkit.getOfflinePlayer(Bukkit.getPlayerUniqueId("MHF_ArrowLeft")));
            is.setItemMeta(meta);
        });
    }

    public static ItemStackBuilder rightArrow() {
        return new ItemStackBuilder(Material.PLAYER_HEAD).custom(is -> {
            SkullMeta meta = (SkullMeta) is.getItemMeta();
            meta.setOwningPlayer(Bukkit.getOfflinePlayer(Bukkit.getPlayerUniqueId("MHF_ArrowRight")));
            is.setItemMeta(meta);
        });
    }

    public static ItemStackBuilder downArrow() {
        return new ItemStackBuilder(Material.PLAYER_HEAD).custom(is -> {
            SkullMeta meta = (SkullMeta) is.getItemMeta();
            meta.setOwningPlayer(Bukkit.getOfflinePlayer(Bukkit.getPlayerUniqueId("MHF_ArrowDown")));
            is.setItemMeta(meta);
        });
    }

    public static ItemStackBuilder plus() {
        return new ItemStackBuilder(Material.PLAYER_HEAD).custom(is -> {
            SkullMeta meta = (SkullMeta) is.getItemMeta();
            meta.setPlayerProfile(createPlayerProfileFromBase64("eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvM2VkZDIwYmU5MzUyMDk0OWU2Y2U3ODlkYzRmNDNlZmFlYjI4YzcxN2VlNmJmY2JiZTAyNzgwMTQyZjcxNiJ9fX0="));
            is.setItemMeta(meta);
        });
    }

    public static ItemStackBuilder question() {
        return new ItemStackBuilder(Material.PLAYER_HEAD).custom(is -> {
            SkullMeta meta = (SkullMeta) is.getItemMeta();
            meta.setOwningPlayer(Bukkit.getOfflinePlayer(Bukkit.getPlayerUniqueId("MHF_Question")));
            is.setItemMeta(meta);
        });
    }

    private static PlayerProfile createPlayerProfileFromBase64(String base64) {
        UUID uniqueId = new UUID(base64.substring(base64.length() - 20).hashCode(), base64.substring(base64.length() - 10).hashCode());
        PlayerProfile profile = new CraftPlayerProfile(uniqueId, "Player");
        profile.getProperties().add(new ProfileProperty("textures", base64));
        return profile;
    }
}
