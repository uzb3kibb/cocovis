package dev.coco.cocovis;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;

/** Talks to the bundled Natural Motion Blur mod via reflection (it is not a compile dependency). */
public final class MotionBlurBridge {
    private static final String CONFIG_MANAGER = "net.natural.motionblur.config.ConfigManager";

    private MotionBlurBridge() {}

    public static boolean isAvailable() {
        try {
            Class.forName(CONFIG_MANAGER);
            return true;
        } catch (Throwable t) {
            return false;
        }
    }

    private static Object config() throws Exception {
        return Class.forName(CONFIG_MANAGER).getMethod("getConfig").invoke(null);
    }

    public static boolean isEnabled() {
        try {
            Object cfg = config();
            return cfg.getClass().getField("enabled").getBoolean(cfg);
        } catch (Throwable t) {
            return false;
        }
    }

    public static void setEnabled(boolean value) {
        try {
            Object cfg = config();
            cfg.getClass().getField("enabled").setBoolean(cfg, value);
            Class.forName(CONFIG_MANAGER).getMethod("saveConfig").invoke(null);
        } catch (Throwable t) {
            CocoVisClient.LOGGER.warn("Motion blur toggle failed: {}", String.valueOf(t));
        }
    }

    public static void openConfig() {
        try {
            Minecraft mc = Minecraft.getInstance();
            Screen parent = mc.gui.screen();
            Object screen = Class.forName(CONFIG_MANAGER)
                    .getMethod("createConfigScreen", Screen.class).invoke(null, parent);
            mc.gui.setScreen((Screen) screen);
        } catch (Throwable t) {
            CocoVisClient.LOGGER.warn("Motion blur config screen failed (is YACL installed?): {}", String.valueOf(t));
        }
    }
}
