package dev.coco.cocovis;

import dev.coco.cocovis.gui.Ui;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.List;

/** Diamond panel (inventory / chests / ender chest / total) and the change toasts. */
public final class DiamondHud {
    public static int boundsX, boundsY, boundsW, boundsH;
    public static int toastBX, toastBY, toastBW = 120, toastBH = 24;

    private static ItemStack diamondIcon, chestIcon, enderIcon;

    private DiamondHud() {}

    private static void initIcons() {
        if (diamondIcon == null) {
            diamondIcon = new ItemStack(Items.DIAMOND);
            chestIcon = new ItemStack(Items.CHEST);
            enderIcon = new ItemStack(Items.ENDER_CHEST);
        }
    }

    public static void render(GuiGraphicsExtractor g, Minecraft mc) {
        if (mc.player == null) {
            return;
        }
        CocoVisConfig cfg = CocoVisConfig.get();
        if (cfg.showHud) {
            draw(g, mc, false);
        }
        if (cfg.showToasts) {
            drawToasts(g, mc);
        }
    }

    public static void draw(GuiGraphicsExtractor g, Minecraft mc, boolean preview) {
        initIcons();
        CocoVisConfig cfg = CocoVisConfig.get();
        Font font = mc.font;

        int inv = preview ? 64 : DiamondCount.inInventory(mc.player);
        int chestsSum = preview ? 128 : DiamondTracker.chestsSum();
        int chestsCount = preview ? 3 : DiamondTracker.chestsCount();
        int ender = preview ? 32 : DiamondTracker.enderTotal();
        int total = inv + chestsSum + ender;

        String[] labels = {"Инвентарь", "Сундуки", "Эндер-сундук"};
        String[] values = {Fmt.num(inv), Fmt.num(chestsSum), Fmt.num(ender)};
        ItemStack[] icons = {diamondIcon, chestIcon, enderIcon};
        String totalText = "Всего: " + Fmt.num(total) + " " + Fmt.diamonds(total);
        String memText = "Сундуков в памяти: " + chestsCount;

        int w = 130;
        for (int i = 0; i < 3; i++) {
            w = Math.max(w, 28 + Ui.width(font, labels[i]) + 14 + Ui.width(font, values[i]) + 10);
        }
        w = Math.max(w, 14 + Ui.width(font, totalText));
        w = Math.max(w, 14 + Ui.width(font, memText));
        int rowH = 18;
        int h = 22 + 3 * rowH + 4 + 12 + 12 + 6;

        float s = cfg.hudScale <= 0 ? 1.0f : cfg.hudScale;
        g.pose().pushMatrix();
        g.pose().translate((float) cfg.hudX, (float) cfg.hudY);
        g.pose().scale(s, s);
        Ui.card(g, 0, 0, w, h);
        Ui.text(g, font, "Алмазы", 8, 7, Ui.ACCENT, true);
        Ui.hBar(g, 8, 18, w - 16, 1, Ui.ACCENT, Ui.fade(Ui.ACCENT, 0.05f));
        for (int i = 0; i < 3; i++) {
            int y = 22 + i * rowH;
            g.item(icons[i], 6, y);
            Ui.text(g, font, labels[i], 28, y + 5, Ui.MUTED, true);
            Ui.text(g, font, values[i], w - 8 - Ui.width(font, values[i]), y + 5, Ui.TEXT, true);
        }
        int sepY = 22 + 3 * rowH + 2;
        g.fill(8, sepY, w - 8, sepY + 1, Ui.BORDER);
        Ui.text(g, font, totalText, 8, sepY + 5, Ui.TEXT, true);
        Ui.text(g, font, memText, 8, sepY + 17, Ui.DIM, true);
        g.pose().popMatrix();

        boundsX = cfg.hudX;
        boundsY = cfg.hudY;
        boundsW = Math.round(w * s);
        boundsH = Math.round(h * s);
    }

    public static void drawToasts(GuiGraphicsExtractor g, Minecraft mc) {
        CocoVisConfig cfg = CocoVisConfig.get();
        List<DiamondTracker.Toast> list = DiamondTracker.TOASTS;
        int y = cfg.toastY;
        for (DiamondTracker.Toast t : list) {
            float alpha = Math.min(1f, Math.min((t.age + 1) / 6f, t.ticksLeft / 12f));
            if (alpha < 0.08f) {
                continue;
            }
            drawToast(g, mc, cfg.toastX, y, t.amount, alpha);
            y += toastBH + 4;
        }
    }

    public static void drawToastPreview(GuiGraphicsExtractor g, Minecraft mc) {
        CocoVisConfig cfg = CocoVisConfig.get();
        drawToast(g, mc, cfg.toastX, cfg.toastY, 12, 1f);
    }

    private static void drawToast(GuiGraphicsExtractor g, Minecraft mc, int x, int y, int amount, float alpha) {
        initIcons();
        Font font = mc.font;
        boolean gain = amount > 0;
        int color = gain ? Ui.GOOD : Ui.BAD;
        String text = (gain ? "+" : "-") + Fmt.num(Math.abs(amount)) + " " + Fmt.diamonds(amount);
        int w = 8 + 16 + 6 + Ui.width(font, text) + 12;
        int h = 24;
        Ui.roundGradient(g, x, y, w, h, 4, Ui.fade(Ui.PANEL_TOP, alpha), Ui.fade(Ui.PANEL_BOT, alpha));
        Ui.roundOutline(g, x, y, w, h, 4, Ui.fade(Ui.BORDER, alpha));
        g.fill(x + 1, y + 4, x + 3, y + h - 4, Ui.fade(color, alpha));
        g.item(diamondIcon, x + 8, y + 4);
        Ui.text(g, font, text, x + 30, y + 8, Ui.fade(color, alpha), true);
        toastBX = x;
        toastBY = y;
        toastBW = w;
        toastBH = h;
    }
}
