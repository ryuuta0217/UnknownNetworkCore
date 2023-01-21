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

import net.unknown.UnknownNetworkCore;
import org.bukkit.Bukkit;
import org.bukkit.event.Event;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.plugin.EventExecutor;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Predicate;

public class ListenerManager {
    private static final Set<Listener> REGISTERED_LISTENERS = new HashSet<>();

    public static void registerListener(Listener listener) {
        Bukkit.getPluginManager().registerEvents(listener, UnknownNetworkCore.getInstance());
        REGISTERED_LISTENERS.add(listener);
    }

    public static void unregisterListener(Listener listener) {
        if (REGISTERED_LISTENERS.contains(listener)) {
            HandlerList.unregisterAll(listener);
            REGISTERED_LISTENERS.remove(listener);
        }
    }

    public static boolean isRegisteredListener(Listener listener) {
        return REGISTERED_LISTENERS.contains(listener);
    }

    public static Listener registerEventListener(Class<? extends Event> eventClass, Listener listener, EventPriority priority, boolean ignoreCancelled, EventExecutor eventExecutor) {
        if (listener == null) listener = new Listener() {
        };
        Bukkit.getPluginManager().registerEvent(eventClass, listener, priority, eventExecutor, UnknownNetworkCore.getInstance(), ignoreCancelled);
        REGISTERED_LISTENERS.add(listener);
        return listener;
    }

    public static <T extends Event> void waitForEvent(Class<T> eventClass, boolean ignoreCancelled, EventPriority priority,
                                                      Predicate<T> condition, Consumer<T> action,
                                                      long timeout, TimeType timeType, Runnable timeoutAction) {
        Listener dummyListener = new Listener() {
        };

        BukkitTask timeoutTask = RunnableManager.runAsyncDelayed(() -> {
            unregisterListener(dummyListener);
            timeoutAction.run();
        }, timeType.toTick(timeout));

        registerEventListener(eventClass, dummyListener, priority, ignoreCancelled, (listener, event) -> {
            if (condition.test((T) event)) {
                timeoutTask.cancel();
                HandlerList.unregisterAll(listener);
                action.accept((T) event);
            }
        });
    }


    public enum TimeType {
        TICK(1),
        SECONDS(20),
        MINUTES(1200),
        HOURS(72000),
        DAYS(1728000);

        private final int i;

        TimeType(int i) {
            this.i = i;
        }

        public long toTick(long input) {
            return input * i;
        }
    }
}
