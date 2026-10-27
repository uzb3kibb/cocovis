package dev.coco.cocovis;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Выбор ножа и скина.
 * Модели мечей выбирают вариант по состоянию трёх служебных клавиш (key.cocovis.knife0..2): состояние кодируется
 * тремя битами, мод «нажимает» нужные клавиши программно, поэтому ресурсы перезагружать не нужно.
 * Состояния: 0 стандартные мечи, 1 Стандартный, 2 Ржавый, 3 Сапфир, 4 Тёмный, 5 Авто (по материалу меча).
 */
public final class Knives {
    public static final String[] SKIN_IDS = {"vanilla", "rust", "sapphire", "dark", "auto"};
    public static final String[] SKIN_NAMES = {"Стандартный", "Ржавый", "Сапфир", "Тёмный", "Авто"};
    public static final String[] SKIN_INFO = {
            "Полированная сталь, как в CS2",
            "Ржавый клинок, чёрная рукоять",
            "Синий клинок «сапфир»",
            "Тёмный клинок, светлая рукоять",
            "По материалу меча: дерево, железо, алмаз, незерит"
    };
    public static final long INSPECT_MS = 3400;

    private static final KeyMapping[] BITS = new KeyMapping[3];
    private static KeyMapping inspectKey;
    private static long inspectStart = 0;

    private Knives() {}

    public static void register(KeyMapping.Category category) {
        for (int i = 0; i < BITS.length; i++) {
            BITS[i] = KeyMappingHelper.registerKeyMapping(
                    new KeyMapping("key.cocovis.knife" + i, InputConstants.UNKNOWN.getValue(), category));
        }
        inspectKey = KeyMappingHelper.registerKeyMapping(
                new KeyMapping("key.cocovis.inspect", InputConstants.KEY_H, category));
    }

    public static KeyMapping inspectKey() { return inspectKey; }

    public static boolean isActive() { return DiamondConfig.get().knivesOn; }

    public static void setActive(boolean on) {
        DiamondConfig.get().knivesOn = on;
        DiamondConfig.save();
        if (!on) inspectStart = 0;
        apply();
    }

    public static int skinIndex() {
        String id = DiamondConfig.get().huntsmanSkin;
        for (int i = 0; i < SKIN_IDS.length; i++) if (SKIN_IDS[i].equals(id)) return i;
        return 0;
    }

    /** «Стандартные мечи»: возвращаются ванильные модели. */
    public static void selectDefault() {
        setActive(false);
    }

    /** Huntsman с выбранным скином. */
    public static void selectSkin(int index) {
        DiamondConfig cfg = DiamondConfig.get();
        cfg.huntsmanSkin = SKIN_IDS[Math.max(0, Math.min(SKIN_IDS.length - 1, index))];
        cfg.knivesOn = true;
        DiamondConfig.save();
        apply();
    }

    public static int stateCode() {
        return isActive() ? 1 + skinIndex() : 0;
    }

    /** Вызывается каждый тик, каждый кадр HUD и после открытия экранов (игра отпускает все клавиши при открытии экрана). */
    public static void apply() {
        int state = stateCode();
        for (int i = 0; i < BITS.length; i++) {
            if (BITS[i] != null) BITS[i].setDown(((state >> i) & 1) != 0);
        }
    }

    public static String describe() {
        return isActive() ? "Сейчас: Huntsman · " + SKIN_NAMES[skinIndex()] : "Сейчас: стандартные мечи";
    }

    // ------------------------------------------------------------------ осмотр

    public static boolean isCustomSword(ItemStack s) {
        return s.is(Items.WOODEN_SWORD) || s.is(Items.IRON_SWORD) || s.is(Items.DIAMOND_SWORD) || s.is(Items.NETHERITE_SWORD);
    }

    public static boolean isKnifeInHand(Minecraft mc) {
        return mc.player != null && isActive() && isCustomSword(mc.player.getMainHandItem());
    }

    public static void startInspect() {
        Minecraft mc = Minecraft.getInstance();
        if (isKnifeInHand(mc) && DiamondConfig.get().knifeAnimations) inspectStart = System.currentTimeMillis();
    }

    public static void cancelInspect() { inspectStart = 0; }

    /** Прогресс осмотра 0..1 или -1, если осмотр не идёт. */
    public static float inspectProgress() {
        if (inspectStart == 0) return -1f;
        long d = System.currentTimeMillis() - inspectStart;
        if (d >= INSPECT_MS) {
            inspectStart = 0;
            return -1f;
        }
        return d / (float) INSPECT_MS;
    }
}
