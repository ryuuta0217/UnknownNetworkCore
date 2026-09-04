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

package net.unknown.survival.listeners;

import net.kyori.adventure.text.Component;
import net.unknown.core.define.DefinedTextColor;
import net.unknown.survival.data.PlayerData;
import net.unknown.survival.data.Spawns;
import net.unknown.survival.data.model.Spawn;
import org.bukkit.Location;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerBedEnterEvent;
import org.bukkit.event.player.PlayerRespawnEvent;

public class SpawnConfigListener implements Listener {

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        PlayerData.SpawnConfigData config = PlayerData.of(event.getPlayer()).getSpawnConfigData();

        // overrideRespawn が無効で、ベッド/アンカーのリスポーンがある場合はバニラに任せる
        if (event.isBedSpawn() || event.isAnchorSpawn()) {
            if (!config.isOverrideRespawn()) {
                return;
            }
        }

        // identifier からリスポーン先を解決
        Location loc = config.resolveLocation();
        if (loc != null) {
            event.setRespawnLocation(loc);
        }

        // 解決に失敗した場合はバニラ/Multiverse のデフォルト動作になる
    }

    @EventHandler
    public void onPlayerBedEnter(PlayerBedEnterEvent event) {
        if (!event.enterAction().canSetSpawn().success()) return;

        PlayerData.SpawnConfigData config = PlayerData.of(event.getPlayer()).getSpawnConfigData();
        if (config.isOverrideRespawn()) {
            Spawn respawnPoint = Spawns.getSpawn(config.getSpawnIdentifier());

            if (respawnPoint != null) {
                Component warning = Component.empty()
                        .append(Component.text("⚠ ", DefinedTextColor.YELLOW))
                        .append(Component.text("リスポーン地点の上書きが有効なため、ベッドで寝てもリスポーン地点は変更されません。", DefinedTextColor.GOLD));

                Component currentSpawn = Component.empty()
                        .append(Component.text("現在のリスポーン先: ", DefinedTextColor.GRAY))
                        .append(respawnPoint.getDisplayName());

                event.getPlayer().sendMessage(warning);
                event.getPlayer().sendMessage(currentSpawn);
            }
        }
    }
}

