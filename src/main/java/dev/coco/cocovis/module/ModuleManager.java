package dev.coco.cocovis.module;

import dev.coco.cocovis.CocoVisConfig;
import dev.coco.cocovis.MotionBlurBridge;
import dev.coco.cocovis.gui.HudEditor;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public final class ModuleManager {
    public record Category(String name, String subtitle, Item icon, List<Module> modules) {}

    private static void openEditor() {
        Minecraft.getInstance().gui.setScreen(new HudEditor());
    }

    private static Consumer<Boolean> cfg(BiConsumer<CocoVisConfig, Boolean> set) {
        return v -> {
            set.accept(CocoVisConfig.get(), v);
            CocoVisConfig.save();
        };
    }

    // ---------------- Ники ----------------
    public static final Module NAMETAGS = new Module("Клановые ники",
            "Ник игрока с кланом из таба над головой", Items.NAME_TAG,
            () -> CocoVisConfig.get().clanNametags, cfg((c, v) -> c.clanNametags = v));

    public static final Module NAMETAG_HEALTH = new Module("Здоровье над головой",
            "Сердце, полоска и число здоровья рядом с ником", Items.GOLDEN_APPLE,
            () -> CocoVisConfig.get().showHealth, cfg((c, v) -> c.showHealth = v));

    public static final Module NAMETAG_EMBLEM = new Module("Эмблема клана",
            "Значок перед названием клана, выбирается по названию", Items.SHIELD,
            () -> CocoVisConfig.get().showEmblem, cfg((c, v) -> c.showEmblem = v));

    public static final Module NAMETAG_ACCENT = new Module("Полоска цвета клана",
            "Цветная полоска между ником и кланом", Items.GLOW_INK_SAC,
            () -> CocoVisConfig.get().showAccent, cfg((c, v) -> c.showAccent = v));

    // ---------------- Алмазы ----------------
    public static final Module DIAMONDS = new Module("Счётчик алмазов",
            "Алмазы в инвентаре, сундуках и эндер-сундуке", Items.DIAMOND,
            () -> CocoVisConfig.get().showHud, cfg((c, v) -> c.showHud = v))
            .withSettings("Переместить", ModuleManager::openEditor);

    public static final Module TOASTS = new Module("Уведомления об алмазах",
            "Всплывающее сообщение, когда сумма алмазов меняется", Items.BELL,
            () -> CocoVisConfig.get().showToasts, cfg((c, v) -> c.showToasts = v))
            .withSettings("Переместить", ModuleManager::openEditor);

    public static final Module TOOLTIP = new Module("Цены в подсказках",
            "Цена предмета в алмазах при наведении курсора", Items.GOLD_NUGGET,
            () -> CocoVisConfig.get().showTooltips, cfg((c, v) -> c.showTooltips = v));

    // ---------------- Интерфейс ----------------
    public static final Module COORDS = new Module("Координаты",
            "Ваши координаты X Y Z на экране", Items.COMPASS,
            () -> CocoVisConfig.get().showCoords, cfg((c, v) -> c.showCoords = v))
            .withSettings("Переместить", ModuleManager::openEditor);

    public static final Module FPS = new Module("Счётчик FPS",
            "Кадры в секунду на экране", Items.CLOCK,
            () -> CocoVisConfig.get().showFps, cfg((c, v) -> c.showFps = v))
            .withSettings("Переместить", ModuleManager::openEditor);

    public static final Module WATERMARK = new Module("Водяной знак",
            "Надпись CocoVis с версией в углу экрана", Items.ENDER_EYE,
            () -> CocoVisConfig.get().showWatermark, cfg((c, v) -> c.showWatermark = v))
            .withSettings("Переместить", ModuleManager::openEditor);

    // ---------------- Графика ----------------
    public static final Module MOTION_BLUR = new Module("Размытие движения",
            "Плавное размытие при движении камеры (Motion Blur)", Items.ENDER_PEARL,
            MotionBlurBridge::isEnabled, MotionBlurBridge::setEnabled)
            .withDescription(() -> MotionBlurBridge.isAvailable()
                    ? "Плавное размытие при движении камеры (Motion Blur)"
                    : "Мод Natural Motion Blur не найден")
            .withSettings("Настроить", MotionBlurBridge::openConfig);

    public static final List<Category> CATEGORIES = List.of(
            new Category("Ники", "Игроки и кланы над головой", Items.NAME_TAG,
                    List.of(NAMETAGS, NAMETAG_HEALTH, NAMETAG_EMBLEM, NAMETAG_ACCENT)),
            new Category("Алмазы", "Экономика сервера", Items.DIAMOND,
                    List.of(DIAMONDS, TOASTS, TOOLTIP)),
            new Category("Интерфейс", "Информация на экране", Items.COMPASS,
                    List.of(COORDS, FPS, WATERMARK)),
            new Category("Графика", "Эффекты и картинка", Items.ENDER_EYE,
                    List.of(MOTION_BLUR)));

    private ModuleManager() {}

    public static List<Category> all() {
        return CATEGORIES;
    }
}
