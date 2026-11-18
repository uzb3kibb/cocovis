package dev.coco.cocovis;

import net.fabricmc.loader.api.FabricLoader;

import java.time.LocalTime;

/** Client-side sky clock and smoothly animated sky tint. */
public final class SkyState {
    private static float red = -1f;
    private static float green;
    private static float blue;
    private static long lastColorUpdate;

    private SkyState() {}

    public static boolean active() {
        // Cirrus owns the client sky clock when installed. Avoid competing clock overrides.
        return !FabricLoader.getInstance().isModLoaded("cirrus")
                && CocoVisConfig.get().skyEnabled;
    }

    public static boolean colorActive() {
        CocoVisConfig cfg = CocoVisConfig.get();
        return cfg.skyColorEnabled && cfg.skyColorMode > 0;
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

    /** Returns a packed RGB color with frame-rate-independent smoothing. */
    public static int color() {
        CocoVisConfig cfg = CocoVisConfig.get();
        float[] target = targetColor(cfg);
        long now = System.nanoTime();
        float dt = lastColorUpdate == 0L ? 1f : Math.min(0.1f, (now - lastColorUpdate) / 1_000_000_000f);
        lastColorUpdate = now;
        float amount = 1f - (float) Math.exp(-dt * 4.5f);
        if (red < 0f) {
            red = target[0];
            green = target[1];
            blue = target[2];
        } else {
            red += (target[0] - red) * amount;
            green += (target[1] - green) * amount;
            blue += (target[2] - blue) * amount;
        }
        return (clamp(Math.round(red * 255f)) << 16)
                | (clamp(Math.round(green * 255f)) << 8)
                | clamp(Math.round(blue * 255f));
    }

    private static float[] targetColor(CocoVisConfig cfg) {
        double day = ticks() / 24000.0;
        double pulse = (Math.sin(System.nanoTime() / 8_000_000_000.0) + 1.0) * 0.5;
        return switch (cfg.skyColorMode) {
            case 1 -> mix(new int[]{0x00D9FF, 0xB000FF}, pulse);
            case 2 -> mix(new int[]{0x2525B8, 0x20D9D0}, (Math.sin(day * Math.PI * 2.0) + 1.0) * 0.5);
            case 3 -> mix(new int[]{0xFF6A3D, 0xA52C79}, (Math.sin(day * Math.PI * 2.0 - Math.PI / 2.0) + 1.0) * 0.5);
            case 4 -> mix(new int[]{0x00D68F, 0x285CFF}, pulse);
            case 5 -> parseHex(cfg.skyColorHex);
            default -> new float[]{0f, 0f, 0f};
        };
    }

    private static float[] mix(int[] colors, double t) {
        int a = colors[0], b = colors[1];
        return new float[]{
                (((a >> 16) & 255) * (1f - (float) t) + ((b >> 16) & 255) * (float) t) / 255f,
                (((a >> 8) & 255) * (1f - (float) t) + ((b >> 8) & 255) * (float) t) / 255f,
                ((a & 255) * (1f - (float) t) + (b & 255) * (float) t) / 255f
        };
    }

    private static float[] parseHex(String value) {
        try {
            String hex = value == null ? "7B2CFF" : value.replace("#", "").trim();
            if (!hex.matches("[0-9a-fA-F]{6}")) hex = "7B2CFF";
            int rgb = Integer.parseInt(hex, 16);
            return new float[]{((rgb >> 16) & 255) / 255f, ((rgb >> 8) & 255) / 255f, (rgb & 255) / 255f};
        } catch (RuntimeException ignored) {
            return new float[]{0.48f, 0.17f, 1f};
        }
    }

    private static int clamp(int value) {
        return Math.max(0, Math.min(255, value));
    }
}
