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

package net.unknown.survival.fml;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ModdedPlayerManager implements Listener {
    private static final Map<UUID, ModdedClientPlayer> MOD_INSTALLED_PLAYERS = new HashMap<>();

    @Nullable
    public static ModdedClientPlayer getModPlayer(Player player) {
        return MOD_INSTALLED_PLAYERS.getOrDefault(player.getUniqueId(), null);
    }

    public static void addPlayer(Player player, ModdedClientPlayer modPlayer) {
        MOD_INSTALLED_PLAYERS.put(player.getUniqueId(), modPlayer);
    }

    public static void removePlayer(Player player) {
        MOD_INSTALLED_PLAYERS.remove(player.getUniqueId());
    }

    public static boolean isModInstalled(Player player) {
        return MOD_INSTALLED_PLAYERS.containsKey(player.getUniqueId());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        ModdedPlayerManager.removePlayer(event.getPlayer());
    }
}