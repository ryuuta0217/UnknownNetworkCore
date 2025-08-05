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

package net.unknown.athletic.feature;

import net.kyori.adventure.text.Component;
import net.unknown.UnknownNetworkCorePlugin;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitRunnable;

public class StopwatchActionBar extends BukkitRunnable {
    private static StopwatchActionBar INSTANCE = null;

    public static void init() {
        if (INSTANCE != null) {
            if (!INSTANCE.isCancelled()) INSTANCE.cancel();
            INSTANCE = null;
        }

        INSTANCE = new StopwatchActionBar();
        INSTANCE.runTaskTimerAsynchronously(UnknownNetworkCorePlugin.getInstance(), 0L, 1L);
    }

    @Override
    public void run() {
        Bukkit.getOnlinePlayers().parallelStream().forEach(player -> {
            if (Stopwatch.isRunningStopwatch(player)) {
                player.sendActionBar(Component.text(format(Stopwatch.getTime(player))));
            }
        });
    }

    /**
     * 入力されたticksを HH時間 mm分 ss秒 tティック の形式で出力します。
     * @param ticks ティック
     * @return HH:mm:ss tTicks
     */
    private static String format(int ticks) {
        int remainingTicks = ticks;

        int seconds = ticks / 20;
        remainingTicks -= 20 * seconds;

        int minutes = seconds / 60;
        seconds -= 60 * minutes;

        int hours = minutes / 60;
        minutes -= 60 * hours;

        StringBuilder text = new StringBuilder();
        if (hours > 0) text.append(hours).append(":");
        if (minutes > 0 || seconds > 0) text.append(minutes < 10 ? "0" + minutes : minutes).append(":");
        if (seconds > 0 || minutes > 0) text.append(seconds < 10 ? "0" + seconds : seconds);
        text.append(" ").append(remainingTicks < 10 ? "0" + remainingTicks : remainingTicks).append("ticks");
        return text.toString();
    }
}
