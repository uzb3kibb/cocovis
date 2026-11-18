package dev.coco.cocovis;

import java.time.LocalTime;

/** Client-side time of day used by the clock mixin. */
public final class SkyState {
    private SkyState() {}

    public static boolean active() {
        return CocoVisConfig.get().skyEnabled;
    }

    /** Ticks of the day: 0 = dawn, 6000 = noon, 12000 = sunset, 18000 = midnight. */
    public static long ticks() {
        CocoVisConfig cfg = CocoVisConfig.get();
        if (cfg.skyRealTime) {
            double seconds = LocalTime.now().toSecondOfDay();
            double fraction = ((seconds - 6 * 3600.0) + 86400.0) % 86400.0 / 86400.0;
            return (long) (fraction * 24000.0);
        }
        return Math.floorMod((long) cfg.skyTime, 24000L);
    }
}
