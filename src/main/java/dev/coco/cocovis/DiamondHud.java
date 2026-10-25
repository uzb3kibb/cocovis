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
    public static final int PANEL = 0xE0121820;
    public static final int CARD = 0xE01A212B;
    public static final int ACCENT = 0xFF5CC8FF;
    private static final int GREEN = 0xFF55FF7F;
    private static final int RED = 0xFFFF5555;
    private static final int WHITE = 0xFFFFFFFF;
    private static final int MUTED = 0xFF8E99A6;
    private static final int W = 128, HEAD_H = 24, ROW_H = 20, FOOT_H = 14, PAD = 5;

    /** Границы панели в GUI-координатах (для редактора). */
    public static int boundsX, boundsY, boundsW = W, boundsH = 40;

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
        int h = HEAD_H + rows.size() * ROW_H + (footer ? FOOT_H : 0) + PAD;

        float s = cfg.hudScale;
        boundsX = cfg.hudX;
        boundsY = cfg.hudY;
        boundsW = Math.round(W * s);
        boundsH = Math.round(h * s);

        var pose = g.pose();
        pose.pushMatrix();
        pose.translate((float) cfg.hudX, (float) cfg.hudY);
        pose.scale(s, s);

        Ui.roundFill(g, 0, 0, W, h, 8, PANEL);
        g.text(font, "Алмазы", PAD + 4, 7, WHITE, true);
        Ui.roundFill(g, PAD + 4, 18, 18, 2, 1, ACCENT);
        String allText = rows.size() > 1 ? "Всего " + all : String.valueOf(all);
        g.text(font, allText, W - PAD - 4 - font.width(allText), 7, ACCENT, true);

        int y = HEAD_H;
        for (Row r : rows) {
            Ui.roundFill(g, PAD, y, W - 2 * PAD, ROW_H - 2, 5, CARD);
            g.item(r.icon(), PAD + 3, y + 1);
            g.text(font, r.label(), PAD + 25, y + 5, MUTED, false);
            String v = String.valueOf(r.value());
            g.text(font, v, W - PAD - 6 - font.width(v), y + 5, WHITE, true);
            y += ROW_H;
        }
        if (footer) {
            g.text(font, "Сундуков в памяти: " + DiamondTracker.chestsCount, PAD + 4, y + 3, MUTED, false);
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
        int y = 6;
        for (DiamondTracker.Toast t : DiamondTracker.TOASTS) {
            long age = now - t.createdMs;
            double slide = 1.0;
            if (age < 250) slide = age / 250.0;
            else if (age > dur - 250) slide = (dur - age) / 250.0;
            slide = Math.max(0.0, Math.min(1.0, slide));

            int n = Math.abs(t.delta);
            String title = (t.delta > 0 ? "+" : "-") + n + " " + plural(n);
            String sub = "Всего: " + t.total;
            int w = Math.max(font.width(title), font.width(sub)) + 20;
            int h = 28;
            int x = screenW - (int) ((w + 6) * slide);
            int col = t.delta > 0 ? GREEN : RED;

            Ui.roundFill(g, x, y, w, h, 7, PANEL);
            Ui.roundFill(g, x + 5, y + 6, 3, h - 12, 1, col);
            g.text(font, title, x + 13, y + 5, col, true);
            g.text(font, sub, x + 13, y + 16, MUTED, false);
            y += h + 4;
        }
    }

    private static String plural(int n) {
        int m10 = n % 10, m100 = n % 100;
        if (m10 == 1 && m100 != 11) return "алмаз";
        if (m10 >= 2 && m10 <= 4 && (m100 < 12 || m100 > 14)) return "алмаза";
        return "алмазов";
    }
}
