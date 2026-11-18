package dev.coco.cocovis.module;

import dev.coco.cocovis.CocoVisConfig;
import dev.coco.cocovis.MotionBlurBridge;
import dev.coco.cocovis.gui.HudEditor;
import net.minecraft.client.Minecraft;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

public final class ModuleManager {
    public record Section(String title, List<Module> modules) {}

    public record Category(String name, String subtitle, int icon, List<Section> sections) {
        public List<Module> modules() {
            List<Module> all = new ArrayList<>();
            for (Section section : sections) all.addAll(section.modules());
            return all;
        }
    }

    private static Consumer<Boolean> master(BiConsumer<CocoVisConfig, Boolean> set) {
        return v -> { set.accept(CocoVisConfig.get(), v); CocoVisConfig.save(); };
    }

    private static Setting flag(String name, Function<CocoVisConfig, Boolean> get, BiConsumer<CocoVisConfig, Boolean> set) {
        return Setting.toggle(name, () -> get.apply(CocoVisConfig.get()), v -> {
            set.accept(CocoVisConfig.get(), v);
            CocoVisConfig.save();
        });
    }

    private static Setting position() {
        return Setting.action("Позиция на экране", () -> Minecraft.getInstance().gui.setScreen(new HudEditor()));
    }

    // ---------------- Игроки ----------------
    public static final Module NAMETAGS = new Module("Клановые ники",
            "Ник, клан, здоровье и эффекты над головой игрока",
            () -> CocoVisConfig.get().clanNametags, master((c, v) -> c.clanNametags = v))
            .with(flag("Здоровье", c -> c.showHealth, (c, v) -> c.showHealth = v))
            .with(flag("Эффекты", c -> c.nametagEffects, (c, v) -> c.nametagEffects = v))
            .with(flag("Эмблема клана", c -> c.showEmblem, (c, v) -> c.showEmblem = v))
            .with(flag("Полоска цвета клана", c -> c.showAccent, (c, v) -> c.showAccent = v));

    // ---------------- Экран ----------------
    public static final Module INFO = new Module("Информация",
            "Ник, FPS, пинг, координаты и скорость на экране",
            () -> CocoVisConfig.get().showInfo, master((c, v) -> c.showInfo = v))
            .with(Setting.cycle("Вид", List.of("Плашки", "Пласт", "Столбик"),
                    () -> CocoVisConfig.get().infoStyle, i -> {
                        CocoVisConfig.get().infoStyle = i;
                        CocoVisConfig.save();
                    }))
            .with(flag("Ник", c -> c.showNick, (c, v) -> c.showNick = v))
            .with(flag("FPS", c -> c.showFps, (c, v) -> c.showFps = v))
            .with(flag("Пинг", c -> c.showPing, (c, v) -> c.showPing = v))
            .with(flag("Координаты", c -> c.showCoords, (c, v) -> c.showCoords = v))
            .with(flag("Скорость", c -> c.showSpeed, (c, v) -> c.showSpeed = v))
            .with(position());

    public static final Module EFFECTS = new Module("Эффекты зелий",
            "Активные эффекты с таймерами и полосками",
            () -> CocoVisConfig.get().showEffects, master((c, v) -> c.showEffects = v))
            .with(Setting.cycle("Вид", List.of("Карточка", "Плашки"),
                    () -> CocoVisConfig.get().effectsPills ? 1 : 0, i -> {
                        CocoVisConfig.get().effectsPills = i == 1;
                        CocoVisConfig.save();
                    }))
            .with(position());

    public static final Module DIAMONDS = new Module("Алмазы",
            "Счётчик алмазов, уведомления и цены в подсказках",
            () -> CocoVisConfig.get().showHud, master((c, v) -> c.showHud = v))
            .with(flag("Уведомления об изменении", c -> c.showToasts, (c, v) -> c.showToasts = v))
            .with(flag("Цены в подсказках", c -> c.showTooltips, (c, v) -> c.showTooltips = v))
            .with(position());

    // ---------------- Мир ----------------
    private static final int[] SKY_PRESETS = {-1, 0, 6000, 12000, 16000, 18000};
    private static final List<String> SKY_COLOR_MODES = List.of("Стандарт", "Neon", "Gradient", "Sunset", "Aurora", "Custom");

    public static final Module SKY = new Module("Небо",
            "Локальное время суток и окраска неба",
            () -> CocoVisConfig.get().skyEnabled, master((c, v) -> c.skyEnabled = v))
            .with(Setting.cycle("Время суток", List.of("Своё", "Рассвет", "День", "Закат", "Ночь", "Полночь"),
                    () -> {
                        int t = CocoVisConfig.get().skyTime;
                        for (int i = 1; i < SKY_PRESETS.length; i++) if (SKY_PRESETS[i] == t) return i;
                        return 0;
                    }, i -> {
                        if (i > 0) { CocoVisConfig.get().skyTime = SKY_PRESETS[i]; CocoVisConfig.save(); }
                    }))
            .with(Setting.slider("Время", 0, 24000, 100,
                    () -> CocoVisConfig.get().skyTime, v -> {
                        CocoVisConfig.get().skyTime = v;
                        CocoVisConfig.save();
                    }))
            .with(flag("Реальное время", c -> c.skyRealTime, (c, v) -> c.skyRealTime = v))
            .with(flag("Окраска неба", c -> c.skyColorEnabled, (c, v) -> c.skyColorEnabled = v))
            .with(Setting.cycle("Стиль окраски", SKY_COLOR_MODES,
                    () -> CocoVisConfig.get().skyColorMode, i -> {
                        CocoVisConfig.get().skyColorMode = i;
                        CocoVisConfig.save();
                    }))
            .with(Setting.slider("Сила цвета", 0, 100, 5,
                    () -> CocoVisConfig.get().skyColorIntensity, v -> {
                        CocoVisConfig.get().skyColorIntensity = v;
                        CocoVisConfig.save();
                    }));

    // ---------------- Графика ----------------
    public static final Module MOTION_BLUR = new Module("Размытие движения",
            "Плавное размытие при движении камеры (Motion Blur)",
            MotionBlurBridge::isEnabled, MotionBlurBridge::setEnabled)
            .withDescription(() -> MotionBlurBridge.isAvailable()
                    ? "Плавное размытие при движении камеры (Motion Blur)"
                    : "Мод Natural Motion Blur не найден")
            .with(Setting.action("Настройки размытия", MotionBlurBridge::openConfig));

    public static final List<Category> CATEGORIES = List.of(
            new Category("Игроки", "Ники над головой", 0, List.of(new Section("Ники", List.of(NAMETAGS)))),
            new Category("Экран", "Информация на экране", 2, List.of(
                    new Section("Панели", List.of(INFO, EFFECTS)), new Section("Экономика", List.of(DIAMONDS)))),
            new Category("Мир", "Небо и время суток", 6, List.of(new Section("Окружение", List.of(SKY)))),
            new Category("Графика", "Эффекты и картинка", 3, List.of(new Section("Постобработка", List.of(MOTION_BLUR)))));

    private ModuleManager() {}
    public static List<Category> all() { return CATEGORIES; }
}
