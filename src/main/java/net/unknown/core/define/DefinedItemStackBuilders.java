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

import javax.annotation.Nullable;
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
            meta.setPlayerProfile(createPlayerProfileFromBase64("ewogICJ0aW1lc3RhbXAiIDogMTY1MTA4OTY0ODg1NywKICAicHJvZmlsZUlkIiA6ICJhNjhmMGI2NDhkMTQ0MDAwYTk1ZjRiOWJhMTRmOGRmOSIsCiAgInByb2ZpbGVOYW1lIiA6ICJNSEZfQXJyb3dMZWZ0IiwKICAic2lnbmF0dXJlUmVxdWlyZWQiIDogdHJ1ZSwKICAidGV4dHVyZXMiIDogewogICAgIlNLSU4iIDogewogICAgICAidXJsIiA6ICJodHRwOi8vdGV4dHVyZXMubWluZWNyYWZ0Lm5ldC90ZXh0dXJlL2Y3YWFjYWQxOTNlMjIyNjk3MWVkOTUzMDJkYmE0MzM0MzhiZTQ2NDRmYmFiNWViZjgxODA1NDA2MTY2N2ZiZTIiCiAgICB9CiAgfQp9", "qkUtxaaq+8H6OMkmIzaDSc9BaBL3ZY+jMIf26dHsPis8NJNHujeh4WcxIF+9atNPXp/L9Lp52CykZuMaomARdwqslBZxMvrUdxTLWUQ3HgUDPifuXnxYpghVGk3kv0cBYSU3iQDjPq6UIG6tdf7D0R736pJpN0JYt/BuVtQitAPwEX1HS/V0vO15R3/nUuk6Gxc/CbY7DFMY6Q3yFzRfeZTNgMOIgJmBAWjvrSTV74jADJZb18/kob9NkA7aDPcG6E2onVOxW8dGb/K5NA1YdjPCHiu9isH+BRiJBWjMUu7yfFYbfgFNKv1chE3p3Sb1Qb+HSQfK48KLj2B72FIsjjrQvfxnKRgw2T8s7Uqo3VhIJqbAZcONnyJIbcmyEcOSxXwUc/0LdthzCJONBNVytvKzk91zuFofRC2czx0IKI9NCQUJPwt+uczs4Y+EtQre/hJSXTCeONJjed9oKcLyHkG4d3GCcmpTgeB3cS8XZeUtYvebtneoIsNeyeXlsyzb/3F5D5P+5nW7BYtoJlvl4P7IuOiLifU8k/ggjDpW1/PiAONGiRWp4jWYcb60rRNh9bQEpOEEOA/zgPDGh7m2igyN5LmytsjDZIN2HbomPbxywAy+4LOnhNF7cDe21IdchdPU3uho/T1z08nkg7D10rp7U0hyu0B1OsB1TfIQJGc="));
            is.setItemMeta(meta);
        });
    }

    public static ItemStackBuilder rightArrow() {
        return new ItemStackBuilder(Material.PLAYER_HEAD).custom(is -> {
            SkullMeta meta = (SkullMeta) is.getItemMeta();
            meta.setPlayerProfile(createPlayerProfileFromBase64("ewogICJ0aW1lc3RhbXAiIDogMTY1MTExNDY5Mzg0MSwKICAicHJvZmlsZUlkIiA6ICI1MGM4NTEwYjVlYTA0ZDYwYmU5YTdkNTQyZDZjZDE1NiIsCiAgInByb2ZpbGVOYW1lIiA6ICJNSEZfQXJyb3dSaWdodCIsCiAgInNpZ25hdHVyZVJlcXVpcmVkIiA6IHRydWUsCiAgInRleHR1cmVzIiA6IHsKICAgICJTS0lOIiA6IHsKICAgICAgInVybCIgOiAiaHR0cDovL3RleHR1cmVzLm1pbmVjcmFmdC5uZXQvdGV4dHVyZS9kMzRlZjA2Mzg1MzcyMjJiMjBmNDgwNjk0ZGFkYzBmODVmYmUwNzU5ZDU4MWFhN2ZjZGYyZTQzMTM5Mzc3MTU4IgogICAgfQogIH0KfQ==", "Ue5zLsj9P1ixcO6MXQa9YgiIyJpcYjTf69DlsSEB/AytWUI0ili+YO2oG6W84YnP1jTiwE3f3uER0yx9lVc1gQ9upXDP4AUHt2aYE4XLH0Lg7bg8v095zbRh4EwapaluklKrGXmQZWJwQKV7JTe8jaFtKsV7ycdfD1n1OXha/LXW3ybibv3MwkMjbqyIedJNZML+KRhUQa3myqXyvoGCKYcnlQtCZgpqLqfPtyosMRkkLsB53cqwIMprNFkyKAvNpYRhLXspkJUDP0IeWVewjcxQulpvDnOClyYI5r+pe+GjynGu5Ysfs0S1VaJ9IlVAX5zXE5jmZGzIZ7kgeZm8M/Q0dNEZxULwRx3biUjloHO39BhvgJLbcHcDDwgUVRh+FPhMBOdgxf+TK/9xVMVI3AkfyVYpyl9V6uj+m/SP+4UNBG878S/2OfQPTStQxsPEPKgYCUVicW/RUgAcfzWQFVodi9oFkU+eXnyB1vY9cncZE6LpXKMQve6Z73xAi1RE8E08H4pbEVdwljVPr6CtMnElzDApnj2VajCEJZv52qBhQJN3gGDDnhqvJopebw4Y7wYbrbpgXL123skp78fwyKzx+HFfePfZqdlrbMos3ZeX6blCESaisaKitlCUVz4tZtIGGnbIfNcAHNLo8hz7u2lojAf2zd90G5VquEw9ZX0="));
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
            meta.setPlayerProfile(createPlayerProfileFromBase64("eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvM2VkZDIwYmU5MzUyMDk0OWU2Y2U3ODlkYzRmNDNlZmFlYjI4YzcxN2VlNmJmY2JiZTAyNzgwMTQyZjcxNiJ9fX0=", null));
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

    private static PlayerProfile createPlayerProfileFromBase64(String base64, @Nullable String signature) {
        UUID uniqueId = new UUID(base64.substring(base64.length() - 20).hashCode(), base64.substring(base64.length() - 10).hashCode());
        PlayerProfile profile = new CraftPlayerProfile(uniqueId, "Player");
        profile.getProperties().add(new ProfileProperty("textures", base64));
        return profile;
    }
}
