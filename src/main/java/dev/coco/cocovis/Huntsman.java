package dev.coco.cocovis;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;

/**
 * Включение ножей Huntsman из меню.
 * Модели мечей объявлены через условие «keybind_down» по служебной клавише key.cocovis.huntsman,
 * поэтому достаточно «нажимать» эту клавишу программно — ресурсы перезагружать не нужно.
 */
public final class Huntsman {
    private static KeyMapping key;

    private Huntsman() {}

    public static void register(KeyMapping.Category category) {
        key = KeyMappingHelper.registerKeyMapping(
                new KeyMapping("key.cocovis.huntsman", InputConstants.UNKNOWN.getValue(), category));
    }

    public static boolean isEnabled() {
        return DiamondConfig.get().huntsmanKnives;
    }

    public static void setEnabled(boolean value) {
        DiamondConfig.get().huntsmanKnives = value;
        DiamondConfig.save();
        apply();
    }

    /** Вызывается каждый тик, каждый кадр HUD и после открытия экранов (игра сбрасывает все клавиши при открытии экрана). */
    public static void apply() {
        if (key != null) key.setDown(isEnabled());
    }
}
