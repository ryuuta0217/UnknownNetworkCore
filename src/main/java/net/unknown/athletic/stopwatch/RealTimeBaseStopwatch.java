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
import org.bukkit.entity.Player;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class RealTimeBaseStopwatch implements Stopwatch {
    private long startTimeMillis = -1;
    private long endTimeMillis = -1;

    public RealTimeBaseStopwatch() {}

    public RealTimeBaseStopwatch(Map<String, Object> serialized) {
        this.startTimeMillis = (long) serialized.get("start_time_millis");
        this.endTimeMillis = (long) serialized.get("end_time_millis");
    }

    @Override
    public void reset() {
        this.startTimeMillis = System.currentTimeMillis();
        this.endTimeMillis = -1;
    }

    @Override
    public void start(Player player) {
        StopwatchStartEvent event = new StopwatchStartEvent(this, player);
        if (event.isCancelled()) return;
        this.endTimeMillis = -1;
    }

    @Override
    public void stop(Player player) {
        StopwatchStopEvent event = new StopwatchStopEvent(this, player);
        if (event.isCancelled()) return;
        this.endTimeMillis = System.currentTimeMillis();
    }

    @Override
    public void increment() {
        // No implementation needed for real-time base stopwatch
        throw new IllegalStateException("Increment method is not applicable for real-time base stopwatch.");
    }

    @Override
    public boolean isRunning() {
        return this.endTimeMillis == -1;
    }

    public long getStartTimeMillis() {
        return this.startTimeMillis;
    }

    public long getEndTimeMillis() {
        return this.endTimeMillis;
    }

    @Override
    public int getElapsedTimeSeconds() {
        return Math.toIntExact(TimeUnit.MILLISECONDS.toSeconds((this.endTimeMillis != -1 ? System.currentTimeMillis() : this.endTimeMillis) - this.startTimeMillis));
    }

    @Override
    public String getElapsedTimeFormatted() {
        Duration elapsed = Duration.between(Instant.ofEpochMilli(this.startTimeMillis), (this.endTimeMillis != -1 ? Instant.now() : Instant.ofEpochMilli(this.endTimeMillis)));
        return LocalTime.MIDNIGHT.plus(elapsed).format(DateTimeFormatter.ofPattern("HH:mm:ss.SSS"));
    }

    @Override
    public Map<String, Object> serialize() {
        return Map.of("start_time_millis", this.startTimeMillis, "end_time_millis", this.endTimeMillis);
    }
}
