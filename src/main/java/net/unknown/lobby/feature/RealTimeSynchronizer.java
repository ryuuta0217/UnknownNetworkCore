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

package net.unknown.lobby.feature;

import net.unknown.UnknownNetworkCore;
import net.unknown.shared.util.TickUtil;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitRunnable;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;

public class RealTimeSynchronizer extends BukkitRunnable {
    private static RealTimeSynchronizer INSTANCE;

    public static void start() {
        if (INSTANCE != null && !INSTANCE.isCancelled()) throw new IllegalStateException("Synchronize task is already running!");
        INSTANCE = new RealTimeSynchronizer();
        INSTANCE.runTaskTimer(UnknownNetworkCore.getInstance(), 0, 20);
    }

    public static void stop() {
        if (INSTANCE == null || INSTANCE.isCancelled()) throw new IllegalStateException("Synchronize task is not running.");
        INSTANCE.cancel();
    }

    @Override
    public void run() {
        long tick = TickUtil.realTime2TickTime(LocalDateTime.now(ZoneId.of("Asia/Tokyo")));
        Bukkit.getWorlds().forEach(world -> world.setTime(tick));
    }
}
