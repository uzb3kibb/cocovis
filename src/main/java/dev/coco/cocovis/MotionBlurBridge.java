package dev.coco.cocovis;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * Связь с модом Natural Motion Blur (LGPL-3.0, автор Pasi), который лежит внутри CocoVis как есть.
 * Всё через рефлексию, поэтому CocoVis не зависит от него при компиляции и не ломается, если мода нет.
 */
public final class MotionBlurBridge {
    private static final String CM = "net.natural.motionblur.config.ConfigManager";
    private static final String KB = "net.natural.motionblur.config.KeybindingManager";

    private MotionBlurBridge() {}

    public static boolean isAvailable() {
        try {
            Class.forName(CM);
            return true;
        } catch (Throwable t) {
            return false;
        }
    }

    private static Object config() throws Exception {
        return Class.forName(CM).getMethod("getConfig").invoke(null);
    }

    public static boolean isEnabled() {
        try {
            Object cfg = config();
            if (cfg == null) return false;
            return cfg.getClass().getField("enabled").getBoolean(cfg);
        } catch (Throwable t) {
            return false;
        }
    }

    /** Включает/выключает размытие, используя собственную логику мода (та же, что у его клавиши). */
    public static void setEnabled(boolean value) {
        try {
            if (isEnabled() == value) return;
            Method toggle = Class.forName(KB).getDeclaredMethod("toggleMotionBlur");
            toggle.setAccessible(true);
            toggle.invoke(null);
        } catch (Throwable t) {
            // запасной путь: просто выставить флаг и сохранить конфиг
            try {
                Object cfg = config();
                if (cfg == null) return;
                Field f = cfg.getClass().getField("enabled");
                f.setBoolean(cfg, value);
                Class.forName(CM).getMethod("saveConfig").invoke(null);
            } catch (Throwable ignored) {
                System.err.println("[cocovis] Не удалось переключить Motion Blur: " + ignored);
            }
        }
    }

    /** Открывает родной экран настроек Natural Motion Blur (он использует YetAnotherConfigLib). */
    public static void openConfig() {
        try {
            Minecraft mc = Minecraft.getInstance();
            Screen parent = mc.gui.screen();
            Object screen = Class.forName(CM).getMethod("createConfigScreen", Screen.class).invoke(null, parent);
            if (screen instanceof Screen s) mc.gui.setScreen(s);
        } catch (Throwable t) {
            System.err.println("[cocovis] Не удалось открыть настройки Motion Blur: " + t);
        }
    }
}
