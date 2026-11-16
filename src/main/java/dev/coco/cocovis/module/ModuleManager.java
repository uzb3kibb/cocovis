package dev.coco.cocovis.module;

import dev.coco.cocovis.CocoVisConfig;
import dev.coco.cocovis.MotionBlurBridge;
import dev.coco.cocovis.gui.HudEditor;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public final class ModuleManager {
    /** A titled group of modules (one block in the two-column layout). */
    public record Section(String title, List<Module> modules) {}

    public record Category(String name, String subtitle, Item icon, List<Section> sections) {
        public List<Module> modules() {
            List<Module> all = new ArrayList<>();
            for (Section section : sections) {
                all.addAll(section.modules());
            }
            return all;
        }
    }

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

    public static final Module NAMETAG_EFFECTS = new Module("Эффекты над головой",
            "Цветные точки активных эффектов рядом с ником", Items.POTION,
            () -> CocoVisConfig.get().nametagEffects, cfg((c, v) -> c.nametagEffects = v));

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

    // ---------------- Интерфейс: информация ----------------
    public static final Module WATERMARK = new Module("Логотип CocoVis",
            "Плашка с названием клиента", Items.ENDER_EYE,
            () -> CocoVisConfig.get().showWatermark, cfg((c, v) -> c.showWatermark = v))
            .withSettings("Переместить", ModuleManager::openEditor);

    public static final Module NICK = new Module("Ник игрока",
            "Плашка с вашим ником", Items.PLAYER_HEAD,
            () -> CocoVisConfig.get().showNick, cfg((c, v) -> c.showNick = v))
            .withSettings("Переместить", ModuleManager::openEditor);

    public static final Module FPS = new Module("FPS",
            "Кадры в секунду", Items.CLOCK,
            () -> CocoVisConfig.get().showFps, cfg((c, v) -> c.showFps = v))
            .withSettings("Переместить", ModuleManager::openEditor);

    public static final Module PING = new Module("Пинг",
            "Задержка до сервера, цвет зависит от значения", Items.REDSTONE,
            () -> CocoVisConfig.get().showPing, cfg((c, v) -> c.showPing = v))
            .withSettings("Переместить", ModuleManager::openEditor);

    public static final Module COORDS = new Module("Координаты",
            "Ваши координаты X Y Z", Items.COMPASS,
            () -> CocoVisConfig.get().showCoords, cfg((c, v) -> c.showCoords = v))
            .withSettings("Переместить", ModuleManager::openEditor);

    public static final Module SPEED = new Module("Скорость",
            "Скорость движения в блоках в секунду", Items.FEATHER,
            () -> CocoVisConfig.get().showSpeed, cfg((c, v) -> c.showSpeed = v))
            .withSettings("Переместить", ModuleManager::openEditor);

    // ---------------- Интерфейс: эффекты ----------------
    public static final Module EFFECTS = new Module("Эффекты зелий",
            "Список активных эффектов с таймерами", Items.POTION,
            () -> CocoVisConfig.get().showEffects, cfg((c, v) -> c.showEffects = v))
            .withSettings("Переместить", ModuleManager::openEditor);

    public static final Module EFFECTS_PILLS = new Module("Компактные плашки",
            "Эффекты плашками вместо карточки с полосками", Items.GLASS_BOTTLE,
            () -> CocoVisConfig.get().effectsPills, cfg((c, v) -> c.effectsPills = v));

    // ---------------- Графика ----------------
    public static final Module MOTION_BLUR = new Module("Размытие движения",
            "Плавное размытие при движении камеры (Motion Blur)", Items.ENDER_PEARL,
            MotionBlurBridge::isEnabled, MotionBlurBridge::setEnabled)
            .withDescription(() -> MotionBlurBridge.isAvailable()
                    ? "Плавное размытие при движении камеры (Motion Blur)"
                    : "Мод Natural Motion Blur не найден")
            .withSettings("Настроить", MotionBlurBridge::openConfig);

    public static final List<Category> CATEGORIES = List.of(
            new Category("Ники", "Игроки и кланы над головой", Items.NAME_TAG, List.of(
                    new Section("Ник игрока", List.of(NAMETAGS, NAMETAG_HEALTH, NAMETAG_EFFECTS)),
                    new Section("Клан", List.of(NAMETAG_EMBLEM, NAMETAG_ACCENT)))),
            new Category("Алмазы", "Экономика сервера", Items.DIAMOND, List.of(
                    new Section("Счётчик", List.of(DIAMONDS, TOASTS)),
                    new Section("Цены", List.of(TOOLTIP)))),
            new Category("Интерфейс", "Информация на экране", Items.COMPASS, List.of(
                    new Section("Информация", List.of(WATERMARK, NICK, FPS, PING, COORDS, SPEED)),
                    new Section("Эффекты", List.of(EFFECTS, EFFECTS_PILLS)))),
            new Category("Графика", "Эффекты и картинка", Items.ENDER_EYE, List.of(
                    new Section("Постобработка", List.of(MOTION_BLUR)))));

    private ModuleManager() {}

    public static List<Category> all() {
        return CATEGORIES;
    }
}
