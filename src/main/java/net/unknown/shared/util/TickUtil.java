package net.unknown.shared.util;

import java.time.LocalDateTime;
import java.time.LocalTime;

public class TickUtil {
    public static long realTime2TickTime(LocalDateTime now) {
        java.time.LocalDateTime base = (now.getHour() < 6 ? now.minusDays(1) : now).with(java.time.LocalTime.of(6, 0));
        long diffSeconds = java.time.Duration.between(base, now).toSeconds();
        System.out.println(diffSeconds);
        return java.lang.Math.round(diffSeconds * 0.2777777777777778);
    }

    public static LocalTime tickTime2RealTime(long tick) {
        if (tick < 0 || tick > 24000) throw new IllegalArgumentException("0 - 24000");
        double seconds = tick / 0.2777777777777778;
        return LocalTime.of(6, 0).plusSeconds(Math.round(seconds));
    }
}
