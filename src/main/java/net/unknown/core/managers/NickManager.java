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

package net.unknown.core.managers;

import com.destroystokyo.paper.profile.PlayerProfile;
import com.ryuuta0217.util.MojangApi;
import io.papermc.paper.util.TickThread;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerJoinEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class NickManager implements Listener {
    private static final Map<UUID, String> NICKNAMES = new HashMap<>();

    public static boolean setNickName(UUID target, String nickname) {
        if (!Bukkit.getOfflinePlayer(target).hasPlayedBefore()) return false;
        NICKNAMES.put(target, nickname);
        boolean result = true;
        if (Bukkit.getPlayer(target) != null) result = updatePlayerName(Bukkit.getPlayer(target), nickname) == NickResult.SUCCESS;
        if (!result) NICKNAMES.remove(target);
        return true;
    }

    public static boolean removeNickName(UUID target) {
        if (!Bukkit.getOfflinePlayer(target).hasPlayedBefore()) return false;
        NICKNAMES.remove(target);
        boolean result = true;
        if (Bukkit.getPlayer(target) != null) result = updatePlayerName(Bukkit.getPlayer(target), null) == NickResult.SUCCESS;
        return result;
    }

    public static String getOriginalNameByMojang(UUID target) {
        if (TickThread.isTickThread()) throw new IllegalStateException("Sync access to Mojang REST API! That will be locking server ticks, use from async!");
        return MojangApi.getName(target);
    }

    private static NickResult updatePlayerName(Player onlinePlayer, String name) {
        if (name == null) name = getOriginalNameByMojang(onlinePlayer.getUniqueId());

        try {
            PlayerProfile profile = onlinePlayer.getPlayerProfile();
            NickResult result = NickManager.updatePlayerProfile(profile, name);
            onlinePlayer.setPlayerProfile(profile);
            boolean playerProfileModified = result == NickResult.SUCCESS;

            boolean playerListNameModified = updatePlayerListName(onlinePlayer, name) == NickResult.SUCCESS;
            boolean playerDisplayNameModified = updatePlayerDisplayName(onlinePlayer, name) == NickResult.SUCCESS;
            boolean playerCustomNameModified = updatePlayerCustomName(onlinePlayer, name) == NickResult.SUCCESS;

            if (playerProfileModified && playerListNameModified && playerDisplayNameModified && playerCustomNameModified) {
                return NickResult.SUCCESS;
            } else if (playerListNameModified && playerDisplayNameModified && playerCustomNameModified) {
                return NickResult.SUCCESS_WITHOUT_PROFILE;
            }
        } catch(Throwable t) {
            t.printStackTrace();
        }
        return NickResult.FAILED;
    }

    @SuppressWarnings("removal")
    private static NickResult updatePlayerProfile(PlayerProfile profile, String name) {
        if (name == null) name = getOriginalNameByMojang(profile.getId());
        try {
            if (name.length() <= 16) { // Restrict only 16 characters
                profile.setName(name);
                return NickResult.SUCCESS;
            }
        } catch(Throwable t) {
            t.printStackTrace();
        }
        return NickResult.FAILED;
    }

    private static NickResult updatePlayerDisplayName(Player onlinePlayer, String name) {
        if (name == null) name = getOriginalNameByMojang(onlinePlayer.getUniqueId());
        try {
            onlinePlayer.setDisplayName(name);
            return NickResult.SUCCESS;
        } catch(Throwable t) {
            t.printStackTrace();
        }
        return NickResult.FAILED;
    }

    private static NickResult updatePlayerListName(Player onlinePlayer, String name) {
        if (name == null) name = getOriginalNameByMojang(onlinePlayer.getUniqueId());
        try {
            onlinePlayer.setPlayerListName(name);
            return NickResult.SUCCESS;
        } catch(Throwable t) {
            t.printStackTrace();
        }
        return NickResult.FAILED;
    }

    private static NickResult updatePlayerCustomName(Player onlinePlayer, String name) {
        if (name == null) name = getOriginalNameByMojang(onlinePlayer.getUniqueId());
        try {
            onlinePlayer.setCustomName(name);
            return NickResult.SUCCESS;
        } catch(Throwable t) {
            t.printStackTrace();
        }
        return NickResult.FAILED;
    }

    @EventHandler
    public void onPlayerJoin(AsyncPlayerPreLoginEvent event) {
        if (!NICKNAMES.containsKey(event.getUniqueId())) return;
        updatePlayerProfile(event.getPlayerProfile(), NICKNAMES.get(event.getUniqueId())); // early process PlayerProfile name

        ListenerManager.waitForEvent(PlayerJoinEvent.class, false, EventPriority.NORMAL, (e) -> {
            return e.getPlayer().getUniqueId().equals(event.getUniqueId()); // Restrict only target (PreLogin) player
        }, (e) -> {
            // delayed process PlayerList/Display name
            updatePlayerListName(e.getPlayer(), NICKNAMES.get(event.getUniqueId()));
            updatePlayerDisplayName(e.getPlayer(), NICKNAMES.get(event.getUniqueId()));
            updatePlayerCustomName(e.getPlayer(), NICKNAMES.get(event.getUniqueId()));
        }, 20 * 3, ListenerManager.TimeType.TICK, () -> {
            // when failed to process names because timed out, nothing to do.
        });
    }

    // TODO: save/load nick names

    public enum NickResult {
        SUCCESS,
        SUCCESS_WITHOUT_PROFILE,
        FAILED
    }
}
