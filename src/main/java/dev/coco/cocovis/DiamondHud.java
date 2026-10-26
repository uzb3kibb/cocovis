package dev.coco.cocovis;

import dev.coco.cocovis.gui.Ui;
import dev.coco.cocovis.module.ModuleManager;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.List;

/** Панель алмазов (двигается/масштабируется в редакторе) и уведомления об изменении суммы. */
public final class DiamondHud {
    private static final int W = 132, HEAD_H = 28, ROW_H = 22, FOOT_H = 14, PAD = 6;

    /** Границы панели в GUI-координатах (для редактора). */
    public static int boundsX, boundsY, boundsW = W, boundsH = 40;

    /** Границы уведомления в GUI-координатах (предпросмотр в редакторе). */
    public static int toastBoundsX, toastBoundsY, toastBoundsW = 96, toastBoundsH = 30;

    private static ItemStack diamondIcon, chestIcon, enderIcon;

    private record Row(ItemStack icon, String label, int value) {}

    private DiamondHud() {}

    public static void render(GuiGraphicsExtractor g, DeltaTracker delta) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        if (ModuleManager.DIAMONDS.isEnabled()) drawPanel(g, mc);
        if (ModuleManager.TOASTS.isEnabled()) drawToasts(g, mc);
    }

    public static void drawPanel(GuiGraphicsExtractor g, Minecraft mc) {
        if (diamondIcon == null) {
            diamondIcon = new ItemStack(Items.DIAMOND);
            chestIcon = new ItemStack(Items.CHEST);
            enderIcon = new ItemStack(Items.ENDER_CHEST);
        }
        DiamondConfig cfg = DiamondConfig.get();
        Font font = mc.font;

        List<Row> rows = new ArrayList<>();
        rows.add(new Row(diamondIcon, "Инвентарь", DiamondTracker.total));
        if (DiamondTracker.chestsCount > 0) rows.add(new Row(chestIcon, "Сундуки", DiamondTracker.chestsSum));
        if (DiamondTracker.enderTotal > 0) rows.add(new Row(enderIcon, "Эндер-сундук", DiamondTracker.enderTotal));
        boolean footer = DiamondTracker.chestsCount > 0;
        int all = DiamondTracker.total + DiamondTracker.chestsSum + DiamondTracker.enderTotal;
        int h = HEAD_H + rows.size() * ROW_H + (footer ? FOOT_H : 0) + PAD - 2;

        float s = cfg.hudScale;
        boundsX = cfg.hudX;
        boundsY = cfg.hudY;
        boundsW = Math.round(W * s);
        boundsH = Math.round(h * s);

        var pose = g.pose();
        pose.pushMatrix();
        pose.translate((float) cfg.hudX, (float) cfg.hudY);
        pose.scale(s, s);

        Ui.panel(g, 0, 0, W, h, 9, Ui.PANEL_TOP, Ui.PANEL_BOT);

        // шапка: заголовок + общая сумма
        Ui.circle(g, PAD + 6, 13, 6, 0x305CC8FF);
        Ui.circle(g, PAD + 6, 13, 3, Ui.ACCENT);
        g.text(font, "Алмазы", PAD + 17, 8, Ui.TEXT, true);
        Ui.hBar(g, PAD + 17, 19, 26, 2, Ui.ACCENT, Ui.ACCENT2);
        String allText = String.valueOf(all);
        g.text(font, allText, W - PAD - 4 - font.width(allText), 8, Ui.ACCENT, true);

        int y = HEAD_H;
        for (Row r : rows) {
            Ui.roundGradient(g, PAD, y, W - 2 * PAD, ROW_H - 3, 6, Ui.CARD_TOP, Ui.CARD_BOT);
            Ui.roundOutline(g, PAD, y, W - 2 * PAD, ROW_H - 3, 6, 0x14FFFFFF);
            g.item(r.icon(), PAD + 3, y + 1);
            g.text(font, r.label(), PAD + 25, y + 5, Ui.MUTED, false);
            String v = String.valueOf(r.value());
            g.text(font, v, W - PAD - 7 - font.width(v), y + 5, Ui.TEXT, true);
            y += ROW_H;
        }
        if (footer) {
            g.text(font, "Сундуков в памяти: " + DiamondTracker.chestsCount, PAD + 4, y + 2, Ui.DIM, false);
        }
        pose.popMatrix();
    }

    private static void drawToasts(GuiGraphicsExtractor g, Minecraft mc) {
        DiamondConfig cfg = DiamondConfig.get();
        long now = System.currentTimeMillis();
        long dur = Math.max(500L, cfg.toastDurationTicks * 50L);
        DiamondTracker.TOASTS.removeIf(t -> now - t.createdMs > dur);

        Font font = mc.font;
        int screenW = mc.getWindow().getGuiScaledWidth();
        boolean custom = cfg.toastX >= 0;
        int y = cfg.toastY >= 0 ? cfg.toastY : 6;
        for (DiamondTracker.Toast t : DiamondTracker.TOASTS) {
            long age = now - t.createdMs;
            double slide = 1.0;
            if (age < 250) slide = age / 250.0;
            else if (age > dur - 250) slide = (dur - age) / 250.0;
            slide = Math.max(0.0, Math.min(1.0, slide));
            slide = 1 - Math.pow(1 - slide, 3); // плавное замедление

            int n = Math.abs(t.delta);
            String title = (t.delta > 0 ? "+" : "-") + n + " " + plural(n);
            String sub = "Всего: " + t.total;
            int w = toastWidth(font, title, sub);
            int x = custom
                    ? cfg.toastX + (int) ((1 - slide) * 24)          // своё место: чуть выезжает справа
                    : screenW - (int) ((w + 8) * slide);               // по умолчанию: выезжает из-за правого края
            toastCard(g, font, x, y, w, title, sub, t.delta > 0 ? Ui.GOOD : Ui.BAD);
            y += TOAST_H + 6;
        }
    }

    private static final int TOAST_H = 30;

    private static int toastWidth(Font font, String title, String sub) {
        return Math.max(font.width(title), font.width(sub)) + 28;
    }

    private static void toastCard(GuiGraphicsExtractor g, Font font, int x, int y, int w, String title, String sub, int col) {
        Ui.panel(g, x, y, w, TOAST_H, 8, Ui.PANEL_TOP, Ui.PANEL_BOT);
        Ui.roundGradient(g, x + 6, y + 7, 3, TOAST_H - 14, 1, col, Ui.mix(col, 0xFF000000, 0.35f));
        g.text(font, title, x + 15, y + 6, col, true);
        g.text(font, sub, x + 15, y + 17, Ui.MUTED, false);
    }

    /** Образец уведомления для редактора: стоит там, где будут появляться настоящие. */
    public static void drawToastPreview(GuiGraphicsExtractor g, Minecraft mc) {
        DiamondConfig cfg = DiamondConfig.get();
        Font font = mc.font;
        String title = "+5 алмазов";
        String sub = "Всего: 12";
        int w = toastWidth(font, title, sub);
        int screenW = mc.getWindow().getGuiScaledWidth();
        int x = cfg.toastX >= 0 ? cfg.toastX : screenW - w - 6;
        int y = cfg.toastY >= 0 ? cfg.toastY : 6;
        toastBoundsX = x;
        toastBoundsY = y;
        toastBoundsW = w;
        toastBoundsH = TOAST_H;
        toastCard(g, font, x, y, w, title, sub, Ui.GOOD);
    }

    private static String plural(int n) {
        int m10 = n % 10, m100 = n % 100;
        if (m10 == 1 && m100 != 11) return "алмаз";
        if (m10 >= 2 && m10 <= 4 && (m100 < 12 || m100 > 14)) return "алмаза";
        return "алмазов";
    }
}
