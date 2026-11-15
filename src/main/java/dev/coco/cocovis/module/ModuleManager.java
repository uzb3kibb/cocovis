package dev.coco.cocovis.module;

import dev.coco.cocovis.CocoVisConfig;
import dev.coco.cocovis.MotionBlurBridge;
import dev.coco.cocovis.gui.HudEditor;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

import java.util.List;
import java.util.function.Consumer;

public final class ModuleManager {
    public record Category(String name, String subtitle, Item icon, List<Module> modules) {}

    private static void openEditor() {
        Minecraft.getInstance().gui.setScreen(new HudEditor());
    }

    private static Consumer<Boolean> cfg(java.util.function.BiConsumer<CocoVisConfig, Boolean> set) {
        return v -> {
            set.accept(CocoVisConfig.get(), v);
            CocoVisConfig.save();
        };
    }

    public static final Module NAMETAGS = new Module("Клановые nametags",
            "Ник, клан из таба, эмблема и цвет клана", Items.NAME_TAG,
            () -> CocoVisConfig.get().clanNametags, cfg((c, v) -> c.clanNametags = v));

    public static final Module NAMETAG_HEALTH = new Module("HP над головой",
            "Сердце, полоска и число здоровья в nametag", Items.GOLDEN_APPLE,
            () -> CocoVisConfig.get().showHealth, cfg((c, v) -> c.showHealth = v));

    public static final Module NAMETAG_EMBLEM = new Module("Эмблема клана",
            "Символ перед названием клана (выбирается по названию)", Items.SHIELD,
            () -> CocoVisConfig.get().showEmblem, cfg((c, v) -> c.showEmblem = v));

    public static final Module NAMETAG_ACCENT = new Module("Цветная полоска",
            "Полоска цвета клана между ником и кланом", Items.GLOW_INK_SAC,
            () -> CocoVisConfig.get().showAccent, cfg((c, v) -> c.showAccent = v));

    public static final Module WATERMARK = new Module("Водяной знак",
            "Надпись CocoVis в углу экрана", Items.ENDER_EYE,
            () -> CocoVisConfig.get().showWatermark, cfg((c, v) -> c.showWatermark = v))
            .withSettings("Позиция", ModuleManager::openEditor);

    public static final Module COORDS = new Module("Координаты",
            "Координаты игрока", Items.COMPASS,
            () -> CocoVisConfig.get().showCoords, cfg((c, v) -> c.showCoords = v))
            .withSettings("Позиция", ModuleManager::openEditor);

    public static final Module FPS = new Module("FPS",
            "Счётчик кадров в секунду", Items.CLOCK,
            () -> CocoVisConfig.get().showFps, cfg((c, v) -> c.showFps = v))
            .withSettings("Позиция", ModuleManager::openEditor);

    public static final Module DIAMONDS = new Module("Счётчик алмазов",
            "Алмазы в инвентаре, сундуках и эндер-сундуке", Items.DIAMOND,
            () -> CocoVisConfig.get().showHud, cfg((c, v) -> c.showHud = v))
            .withSettings("Позиция", ModuleManager::openEditor);

    public static final Module TOOLTIP = new Module("Цены в тултипе",
            "Цена предмета в алмазах при наведении", Items.GOLD_NUGGET,
            () -> CocoVisConfig.get().showTooltips, cfg((c, v) -> c.showTooltips = v));

    public static final Module TOASTS = new Module("Уведомления",
            "Уведомление при изменении суммы алмазов", Items.BELL,
            () -> CocoVisConfig.get().showToasts, cfg((c, v) -> c.showToasts = v))
            .withSettings("Позиция", ModuleManager::openEditor);

    public static final Module MOTION_BLUR = new Module("Motion blur",
            "Плавное размытие движения (Natural Motion Blur)", Items.ENDER_PEARL,
            MotionBlurBridge::isEnabled, MotionBlurBridge::setEnabled)
            .withDescription(() -> MotionBlurBridge.isAvailable()
                    ? "Плавное размытие движения (Natural Motion Blur)"
                    : "Мод Natural Motion Blur не найден")
            .withSettings("Настроить", MotionBlurBridge::openConfig);

    public static final List<Category> CATEGORIES = List.of(
            new Category("Кланы", "Ники над игроками", Items.NAME_TAG, List.of(NAMETAGS, NAMETAG_HEALTH, NAMETAG_EMBLEM, NAMETAG_ACCENT)),
            new Category("Визуалы", "Информация на экране", Items.COMPASS, List.of(WATERMARK, COORDS, FPS)),
            new Category("Алмазы", "Экономика сервера", Items.DIAMOND, List.of(DIAMONDS, TOOLTIP, TOASTS)),
            new Category("Эффекты", "Постобработка картинки", Items.ENDER_EYE, List.of(MOTION_BLUR)));

    private ModuleManager() {}

    public static List<Category> all() {
        return CATEGORIES;
    }
}
