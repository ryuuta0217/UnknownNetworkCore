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

package net.unknown.survival.dependency;

import net.coreprotect.CoreProtectAPI;
import org.bukkit.Bukkit;
import org.bukkit.Location;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class CoreProtect {
    public static boolean isEnabled() {
        return isPluginEnabled() && isAPIEnabled();
    }

    public static boolean isPluginEnabled() {
        return Bukkit.getPluginManager().isPluginEnabled("CoreProtect");
    }

    public static boolean isAPIEnabled() {
        return net.coreprotect.CoreProtect.getInstance().getAPI() != null && net.coreprotect.CoreProtect.getInstance().getAPI().isEnabled();
    }

    public static CoreProtectAPI getAPI() {
        if (!isEnabled()) throw new IllegalStateException("CoreProtect is not enabled!");
        return net.coreprotect.CoreProtect.getInstance().getAPI();
    }

    public static UUID getLastPlaced(Location location) {
        if (!isEnabled()) throw new IllegalStateException("CoreProtect is not enabled!");
        return getAPI().blockLookup(location.getBlock(), Integer.MAX_VALUE)
                .stream()
                .map(blockChange -> getAPI().parseResult(blockChange))
                .filter(result -> result.getActionId() == 1)
                .findFirst()
                .map(result -> result.getPlayer() != null ? Bukkit.getOfflinePlayer(result.getPlayer()).getUniqueId() : null)
                .orElse(null);
    }
}
