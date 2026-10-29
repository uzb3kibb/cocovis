package dev.coco.cocovis.module;

import dev.coco.cocovis.DiamondConfig;
import dev.coco.cocovis.MotionBlurBridge;
import dev.coco.cocovis.gui.HudEditor;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.List;

public final class ModuleManager {
    /** catalog = true: вместо карточек модулей в категории показывается каталог ножей. */
    public record Category(String name, String subtitle, Item icon, List<Module> modules, boolean catalog) {
        public Category(String name, String subtitle, Item icon, List<Module> modules) {
            this(name, subtitle, icon, modules, false);
        }
    }

    private static final DiamondConfig CFG = DiamondConfig.get();

    public static final Module WATERMARK = new Module("Watermark", "Надпись CocoVis в углу экрана", true).withIcon(Items.NAME_TAG);
    public static final Module COORDS    = new Module("Coords", "Координаты игрока", true).withIcon(Items.COMPASS);
    public static final Module FPS       = new Module("FPS", "Счётчик кадров в секунду", false).withIcon(Items.CLOCK);

    public static final Module DIAMONDS  = new Module("Diamond HUD", "Панель алмазов: инвентарь, сундуки, эндер-сундук",
            CFG.showHud, v -> { CFG.showHud = v; DiamondConfig.save(); })
            .withSettings(() -> Minecraft.getInstance().gui.setScreen(new HudEditor())).withIcon(Items.DIAMOND);
    public static final Module TOOLTIP   = new Module("Price Tooltip", "Цена предмета в алмазах в подсказке",
            CFG.showTooltips, v -> { CFG.showTooltips = v; DiamondConfig.save(); }).withIcon(Items.GOLD_NUGGET);
    public static final Module TOASTS    = new Module("Diamond Toasts", "Уведомление при изменении суммы алмазов",
            CFG.showToasts, v -> { CFG.showToasts = v; DiamondConfig.save(); }).withIcon(Items.BELL);

    public static final Module MOTION_BLUR = new Module("Motion Blur",
            MotionBlurBridge.isAvailable() ? "Плавное размытие движения (Natural Motion Blur, Pasi)" : "Мод Natural Motion Blur не найден",
            false, MotionBlurBridge::setEnabled)
            .withState(MotionBlurBridge::isEnabled)
            .withSettings(MotionBlurBridge::openConfig)
            .withIcon(Items.ENDER_PEARL);

    public static final List<Category> CATEGORIES = List.of(
            new Category("Визуалы", "Информация на экране", Items.ENDER_EYE, List.of(WATERMARK, COORDS, FPS)),
            new Category("Алмазы", "Экономика сервера", Items.DIAMOND, List.of(DIAMONDS, TOOLTIP, TOASTS)),
            new Category("Ножи", "Каталог и скины", Items.DIAMOND_SWORD, List.of(), true),
            new Category("Эффекты", "Постобработка картинки", Items.ENDER_PEARL, List.of(MOTION_BLUR))
    );

    public static List<Module> all() {
        List<Module> out = new ArrayList<>();
        for (Category c : CATEGORIES) out.addAll(c.modules());
        return out;
    }

    private ModuleManager() {}
}
