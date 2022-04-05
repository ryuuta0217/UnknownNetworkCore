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

package net.unknown.survival.dependency;

import io.papermc.paper.text.PaperComponents;
import net.kyori.adventure.text.Component;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.cacheddata.CachedDataManager;
import net.luckperms.api.cacheddata.CachedMetaData;
import net.luckperms.api.model.user.User;
import net.luckperms.api.model.user.UserManager;
import org.bukkit.ChatColor;

import javax.annotation.Nullable;
import java.util.UUID;

public class LuckPerms {
    private static final net.luckperms.api.LuckPerms LP = LuckPermsProvider.get();

    public static UserManager getUserManager() {
        return LP.getUserManager();
    }

    public static User getUser(UUID uniqueId) {
        return getUserManager().getUser(uniqueId);
    }

    public static CachedDataManager getCachedData(UUID uniqueId) {
        return getUser(uniqueId).getCachedData();
    }

    public static CachedMetaData getMetaData(UUID uniqueId) {
        return getCachedData(uniqueId).getMetaData();
    }

    @Nullable
    public static String getPrefix(UUID uniqueId) {
        return getMetaData(uniqueId).getPrefix();
    }

    public static Component getPrefixAsComponent(UUID uniqueId) {
        String raw = getPrefix(uniqueId);
        if (raw == null) return Component.empty();
        else return PaperComponents.legacySectionSerializer().deserialize(ChatColor.translateAlternateColorCodes('&', raw));
    }

    @Nullable
    public static String getSuffix(UUID uniqueId) {
        return getMetaData(uniqueId).getSuffix();
    }

    public static Component getSuffixAsComponent(UUID uniqueId) {
        String raw = getSuffix(uniqueId);
        if (raw == null) return Component.empty();
        else
            return PaperComponents.legacySectionSerializer().deserialize(ChatColor.translateAlternateColorCodes('&', raw));
    }
}
