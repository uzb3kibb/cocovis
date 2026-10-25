package dev.coco.cocovis.gui;

import net.minecraft.client.gui.GuiGraphicsExtractor;

/** Тема и помощники для рисования: градиенты, скругления, тени, обводки. */
public final class Ui {
    public static final int ACCENT = 0xFF5CC8FF;
    public static final int ACCENT2 = 0xFF9B7BFF;
    public static final int TEXT = 0xFFFFFFFF;
    public static final int MUTED = 0xFF8E9AAB;
    public static final int DIM = 0xFF5B6678;
    public static final int GOOD = 0xFF4ADE80;
    public static final int BAD = 0xFFF87171;

    public static final int BG_TOP = 0xF0161C2A, BG_BOT = 0xF00A0E15;
    public static final int PANEL_TOP = 0xEC1B2231, PANEL_BOT = 0xEC111722;
    public static final int CARD_TOP = 0xEE1F2837, CARD_BOT = 0xEE161D2A;
    public static final int CARD_HOT_TOP = 0xF02A364B, CARD_HOT_BOT = 0xF01E2839;
    public static final int BORDER = 0x2CFFFFFF;

    private Ui() {}

    /** Отступ строки i от края для скруглённого прямоугольника высотой h с радиусом r. */
    private static int inset(int i, int h, int r) {
        if (r <= 0) return 0;
        int j = i < r ? i : (i >= h - r ? h - 1 - i : r);
        if (j >= r) return 0;
        double dy = r - j - 0.5;
        return (int) Math.round(r - Math.sqrt(r * r - dy * dy));
    }

    public static void roundFill(GuiGraphicsExtractor g, int x, int y, int w, int h, int r, int color) {
        if (w <= 0 || h <= 0) return;
        r = Math.min(r, Math.min(w, h) / 2);
        if (r <= 0) {
            g.fill(x, y, x + w, y + h, color);
            return;
        }
        g.fill(x, y + r, x + w, y + h - r, color);
        for (int i = 0; i < r; i++) {
            int in = inset(i, h, r);
            g.fill(x + in, y + i, x + w - in, y + i + 1, color);
            g.fill(x + in, y + h - 1 - i, x + w - in, y + h - i, color);
        }
    }

    /** Скруглённый прямоугольник с вертикальным градиентом. */
    public static void roundGradient(GuiGraphicsExtractor g, int x, int y, int w, int h, int r, int top, int bottom) {
        if (w <= 0 || h <= 0) return;
        r = Math.min(r, Math.min(w, h) / 2);
        for (int i = 0; i < h; i++) {
            int in = inset(i, h, r);
            g.fill(x + in, y + i, x + w - in, y + i + 1, mix(top, bottom, (i + 0.5f) / h));
        }
    }

    /** Тонкая обводка скруглённого прямоугольника. */
    public static void roundOutline(GuiGraphicsExtractor g, int x, int y, int w, int h, int r, int color) {
        if (w <= 2 || h <= 2) return;
        r = Math.min(r, Math.min(w, h) / 2);
        for (int i = 0; i < h; i++) {
            int in = inset(i, h, r);
            if (i == 0 || i == h - 1) {
                g.fill(x + in, y + i, x + w - in, y + i + 1, color);
                continue;
            }
            int other = i < h / 2 ? inset(i - 1, h, r) : inset(i + 1, h, r);
            int len = Math.max(1, other - in);
            g.fill(x + in, y + i, x + in + len, y + i + 1, color);
            g.fill(x + w - in - len, y + i, x + w - in, y + i + 1, color);
        }
    }

    /** Мягкая тень под панелью. */
    public static void shadow(GuiGraphicsExtractor g, int x, int y, int w, int h, int r) {
        for (int k = 4; k >= 1; k--) {
            roundFill(g, x - k, y - k + 2, w + 2 * k, h + 2 * k, r + k, 0x10000000);
        }
    }

    /** Готовая «стеклянная» панель: тень + градиент + обводка + блик сверху. */
    public static void panel(GuiGraphicsExtractor g, int x, int y, int w, int h, int r, int top, int bottom) {
        shadow(g, x, y, w, h, r);
        roundGradient(g, x, y, w, h, r, top, bottom);
        roundOutline(g, x, y, w, h, r, BORDER);
        g.fill(x + r, y + 1, x + w - r, y + 2, 0x18FFFFFF);
    }

    /** Горизонтальная полоса с градиентом. */
    public static void hBar(GuiGraphicsExtractor g, int x, int y, int w, int h, int c1, int c2) {
        int step = 2;
        for (int i = 0; i < w; i += step) {
            int seg = Math.min(step, w - i);
            g.fill(x + i, y, x + i + seg, y + h, mix(c1, c2, (i + seg / 2f) / w));
        }
    }

    public static void circle(GuiGraphicsExtractor g, int cx, int cy, int rad, int color) {
        roundFill(g, cx - rad, cy - rad, rad * 2, rad * 2, rad, color);
    }

    public static int fade(int c, float f) {
        if (f >= 1f) return c;
        int a = (int) (((c >>> 24) & 255) * Math.max(0f, f));
        if (a < 4) a = 4;
        return (a << 24) | (c & 0xFFFFFF);
    }

    public static int mix(int a, int b, float t) {
        t = Math.max(0f, Math.min(1f, t));
        int aa = (int) (((a >>> 24) & 255) * (1 - t) + ((b >>> 24) & 255) * t);
        int ar = (int) (((a >>> 16) & 255) * (1 - t) + ((b >>> 16) & 255) * t);
        int ag = (int) (((a >>> 8) & 255) * (1 - t) + ((b >>> 8) & 255) * t);
        int ab = (int) ((a & 255) * (1 - t) + (b & 255) * t);
        return (aa << 24) | (ar << 16) | (ag << 8) | ab;
    }

    public static boolean inside(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }
}
