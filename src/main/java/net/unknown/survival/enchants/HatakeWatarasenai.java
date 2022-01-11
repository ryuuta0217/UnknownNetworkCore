/*
 * Copyright (c) 2021 Unknown Network Developers and contributors.
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
 *     loss of use data or profits; or business interpution) however caused and on any theory of liability,
 *     whether in contract, strict liability, or tort (including negligence or otherwise)
 *     arising in any way out of the use of this source code, event if advised of the possibility of such damage.
 */

package net.unknown.survival.enchants;

import com.destroystokyo.paper.event.player.PlayerJumpEvent;
import net.unknown.core.managers.RunnableManager;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;

public class HatakeWatarasenai implements Listener {
    private static BukkitTask EFFECT_UPDATE_TASK;

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerMove(PlayerMoveEvent event) {
        if(event.getPlayer().getInventory().getBoots() == null) return;
        if(event.getPlayer().getInventory().getBoots().getLore() == null) return;
        if(event.getPlayer().getInventory().getBoots().getLore().stream().noneMatch(lore -> lore.equals("§c畑渡らせない"))) return;

        if(event.getFrom().add(0, -1, 0).getBlock().getType() == Material.FARMLAND ||
                event.getFrom().add(0, -2, 0).getBlock().getType() == Material.FARMLAND) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJump(PlayerJumpEvent event) {

    }

    public static void runEffectUpdateTask() {
        if(EFFECT_UPDATE_TASK != null) EFFECT_UPDATE_TASK.cancel();
        EFFECT_UPDATE_TASK = RunnableManager.runAsyncRepeating(() -> {
            Bukkit.getOnlinePlayers().forEach(p -> {
                if(p.getInventory().getBoots() == null) return;
                if(p.getInventory().getBoots().getLore() == null) return;
                if(p.getInventory().getBoots().getLore().parallelStream().noneMatch(lore -> lore.equals("§c畑渡らせない"))) return;
                p.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 30, 127, false, false, false));
            });
        }, 10L, 10L);
    }
}
