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

package net.unknown.athletic.stopwatch;

import net.unknown.athletic.event.StopwatchStartEvent;
import net.unknown.athletic.event.StopwatchStopEvent;
import net.unknown.core.managers.RunnableManager;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.Map;

public class TickTimeBaseStopwatch implements Stopwatch {
    private int elapsedTicks = 0;
    private BukkitTask incrementTask = null;

    public TickTimeBaseStopwatch() {}

    public TickTimeBaseStopwatch(Map<String, Object> serialized) {
        this.elapsedTicks = (int) serialized.get("elapsed_ticks");
        boolean isRunning = (boolean) serialized.get("is_running");
        if (isRunning) {
            this.incrementTask = RunnableManager.runAsyncRepeating(this::increment, 1L, 1L);
        } else {
            this.incrementTask = null;
        }
    }

    @Override
    public void reset() {
        this.elapsedTicks = 0;
    }

    @Override
    public void start(Player player) {
        if (this.incrementTask != null && !this.incrementTask.isCancelled()) {
            this.incrementTask.cancel();
        }

        StopwatchStartEvent event = new StopwatchStartEvent(this, player);
        if (event.isCancelled()) return;
        this.incrementTask = RunnableManager.runAsyncRepeating(this::increment, 1L, 1L);
    }

    @Override
    public void stop(Player player) {
        if (this.incrementTask == null || this.incrementTask.isCancelled()) {
            throw new IllegalStateException("Stopwatch is not running, can't stop it.");
        }

        StopwatchStopEvent event = new StopwatchStopEvent(this, player);
        if (event.isCancelled()) return;
        this.incrementTask.cancel();
    }

    @Override
    public void increment() {
        this.elapsedTicks++;
    }

    @Override
    public boolean isRunning() {
        return this.incrementTask != null && !this.incrementTask.isCancelled();
    }

    public int getElapsedTimeTicks() {
        return this.elapsedTicks;
    }

    @Override
    public int getElapsedTimeSeconds() {
        return this.elapsedTicks / 20;
    }

    @Override
    public String getElapsedTimeFormatted() {
        int remainingTicks = this.elapsedTicks;

        int seconds = this.elapsedTicks / 20;
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

    @Override
    public Map<String, Object> serialize() {
        return Map.of("is_running", this.isRunning(), "elapsed_ticks", this.elapsedTicks);
    }
}
