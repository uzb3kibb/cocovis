package dev.coco.cocovis.module;

import dev.coco.cocovis.DiamondConfig;

import java.util.List;

public final class ModuleManager {
    private static final DiamondConfig CFG = DiamondConfig.get();

    public static final Module WATERMARK = new Module("Watermark", "Надпись CocoVis в углу", true);
    public static final Module COORDS    = new Module("Coords", "Координаты", true);
    public static final Module FPS       = new Module("FPS", "Счётчик FPS", false);
    public static final Module DIAMONDS  = new Module("Diamond HUD", "Панель алмазов: инвентарь, сундуки, эндер-сундук",
            CFG.showHud, v -> { CFG.showHud = v; DiamondConfig.save(); });
    public static final Module TOOLTIP   = new Module("Price Tooltip", "Цена предмета в алмазах в тултипе",
            CFG.showTooltips, v -> { CFG.showTooltips = v; DiamondConfig.save(); });
    public static final Module TOASTS    = new Module("Diamond Toasts", "Уведомление при изменении суммы алмазов",
            CFG.showToasts, v -> { CFG.showToasts = v; DiamondConfig.save(); });

    public static final List<Module> ALL = List.of(WATERMARK, COORDS, FPS, DIAMONDS, TOOLTIP, TOASTS);

    private ModuleManager() {}
}
