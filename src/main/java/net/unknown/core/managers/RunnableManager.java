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

import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import net.unknown.UnknownNetworkCorePlugin;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.TimeUnit;

public final class RunnableManager {
    public static void runSync(Runnable runnable) {
        if (UnknownNetworkCorePlugin.isFoliaPlatform()) {
            Bukkit.getServer().getGlobalRegionScheduler().run(UnknownNetworkCorePlugin.getInstance(), (task) -> runnable.run());
        }
        createBukkitRunnable(runnable).runTask(UnknownNetworkCorePlugin.getInstance());
    }

    public static BukkitTask runDelayed(Runnable runnable, long delay) {
        if (UnknownNetworkCorePlugin.isFoliaPlatform()) {
            return createFoliaBukkitTask(Bukkit.getServer().getGlobalRegionScheduler().runDelayed(UnknownNetworkCorePlugin.getInstance(), (task) -> runnable.run(), delay));
        }
        return createBukkitRunnable(runnable).runTaskLater(UnknownNetworkCorePlugin.getInstance(), delay);
    }

    public static BukkitTask runRepeating(Runnable runnable, long delay, long interval) {
        if (UnknownNetworkCorePlugin.isFoliaPlatform()) {
            if (delay == 0) delay = 1;
            return createFoliaBukkitTask(Bukkit.getServer().getGlobalRegionScheduler().runAtFixedRate(UnknownNetworkCorePlugin.getInstance(), (task) -> runnable.run(), delay, interval));
        }
        return createBukkitRunnable(runnable).runTaskTimer(UnknownNetworkCorePlugin.getInstance(), delay, interval);
    }

    public static BukkitTask runAsync(Runnable runnable) {
        if (UnknownNetworkCorePlugin.isFoliaPlatform()) {
            return createFoliaBukkitTask(Bukkit.getServer().getAsyncScheduler().runNow(UnknownNetworkCorePlugin.getInstance(), (task) -> runnable.run()));
        }
        return createBukkitRunnable(runnable).runTaskAsynchronously(UnknownNetworkCorePlugin.getInstance());
    }

    public static BukkitTask runAsyncDelayed(Runnable runnable, long delay) {
        if (UnknownNetworkCorePlugin.isFoliaPlatform()) {
            return createFoliaBukkitTask(Bukkit.getServer().getAsyncScheduler().runDelayed(UnknownNetworkCorePlugin.getInstance(), (task) -> runnable.run(), 50 * delay, TimeUnit.MILLISECONDS));
        }
        return createBukkitRunnable(runnable).runTaskLaterAsynchronously(UnknownNetworkCorePlugin.getInstance(), delay);
    }

    public static BukkitTask runAsyncRepeating(Runnable runnable, long delay, long interval) {
        if (UnknownNetworkCorePlugin.isFoliaPlatform()) {
            if (delay == 0) delay = 1;
            return createFoliaBukkitTask(Bukkit.getServer().getAsyncScheduler().runAtFixedRate(UnknownNetworkCorePlugin.getInstance(), (task) -> runnable.run(), 50 * delay, 50 * interval, TimeUnit.MILLISECONDS));
        }
        return createBukkitRunnable(runnable).runTaskTimerAsynchronously(UnknownNetworkCorePlugin.getInstance(), delay, interval);
    }

    private static BukkitRunnable createBukkitRunnable(Runnable runnable) {
        return new BukkitRunnable() {
            @Override
            public void run() {
                runnable.run();
            }
        };
    }

    private static BukkitTask createFoliaBukkitTask(ScheduledTask foliaTask) {
        return  new BukkitTask() {
            @Override
            public int getTaskId() {
                return foliaTask.hashCode();
            }

            @Override
            public @NotNull Plugin getOwner() {
                return foliaTask.getOwningPlugin();
            }

            @Override
            public boolean isSync() {
                return true;
            }

            @Override
            public boolean isCancelled() {
                return foliaTask.isCancelled();
            }

            @Override
            public void cancel() {
                foliaTask.cancel();
            }

            @Override
            public int hashCode() {
                return foliaTask.hashCode();
            }

            @Override
            public boolean equals(Object obj) {
                return foliaTask.equals(obj);
            }

            @Override
            public String toString() {
                return foliaTask.toString();
            }
        };
    }
}
