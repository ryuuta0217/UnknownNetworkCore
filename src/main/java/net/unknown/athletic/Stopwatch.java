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

package net.unknown.athletic;

import net.unknown.UnknownNetworkCorePlugin;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Optional;

public class Stopwatch extends BukkitRunnable  {
    private static Stopwatch INSTANCE = null;
    private static final NamespacedKey RUNNING_KEY = new NamespacedKey("athletic", "stopwatch_running");
    private static final NamespacedKey TIME_KEY = new NamespacedKey("athletic", "stopwatch_time");

    public static void init() {
        if (INSTANCE != null && !INSTANCE.isCancelled()) {
            INSTANCE.cancel();
            INSTANCE = null;
        }

        INSTANCE = new Stopwatch();
        INSTANCE.runTaskTimerAsynchronously(UnknownNetworkCorePlugin.getInstance(), 0, 1L);
    }

    public static void startStopwatch(Player player) {
        player.getPersistentDataContainer().set(RUNNING_KEY, PersistentDataType.BOOLEAN, true);
    }

    public static void stopStopwatch(Player player) {
        player.getPersistentDataContainer().set(RUNNING_KEY, PersistentDataType.BOOLEAN, false);
    }

    @Override
    public void run() {
        Bukkit.getOnlinePlayers().parallelStream().forEach(player -> {
            PersistentDataContainer dataContainer = player.getPersistentDataContainer();
            if (dataContainer.has(RUNNING_KEY, PersistentDataType.BOOLEAN)) {
                if (Boolean.TRUE.equals(dataContainer.get(RUNNING_KEY, PersistentDataType.BOOLEAN))) {
                    int ticks = 0;
                    if (dataContainer.has(TIME_KEY, PersistentDataType.INTEGER)) {
                        ticks = Optional.ofNullable(dataContainer.get(TIME_KEY, PersistentDataType.INTEGER)).orElse(0);
                    }
                    ticks += 1;
                    dataContainer.set(RUNNING_KEY, PersistentDataType.INTEGER, ticks);
                }
            }
        });
    }
}
