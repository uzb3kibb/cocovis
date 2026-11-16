package dev.coco.cocovis.gui;

import net.minecraft.client.gui.GuiGraphicsExtractor;

/** Small drawing helpers: dark rounded panels, gradients, colors. */
public final class Ui {
    public static final int ACCENT = 0xFF7CA8FF;
    public static final int ACCENT2 = 0xFF9ABEFF;
    public static final int TEXT = 0xFFE8EDF5;
    public static final int MUTED = 0xFF8994A4;
    public static final int DIM = 0xFF596575;
    public static final int GOOD = 0xFF55D6A0;
    public static final int BAD = 0xFFE8606E;
    public static final int BG_TOP = 0xF20D1118;
    public static final int BG_BOT = 0xF2080B10;
    public static final int PANEL_TOP = 0xF2131821;
    public static final int PANEL_BOT = 0xF20E1219;
    public static final int CARD_TOP = 0xFF141820;
    public static final int CARD_BOT = 0xFF10141A;
    public static final int CARD_HOT_TOP = 0xFF1B222D;
    public static final int CARD_HOT_BOT = 0xFF141820;
    public static final int BORDER = 0xFF252D38;

    private Ui() {}

    public static int mix(int a, int b, float t) {
        t = Math.max(0f, Math.min(1f, t));
        int aa = (a >>> 24) & 0xFF, ar = (a >> 16) & 0xFF, ag = (a >> 8) & 0xFF, ab = a & 0xFF;
        int ba = (b >>> 24) & 0xFF, br = (b >> 16) & 0xFF, bg = (b >> 8) & 0xFF, bb = b & 0xFF;
        int ra = Math.round(aa + (ba - aa) * t);
        int rr = Math.round(ar + (br - ar) * t);
        int rg = Math.round(ag + (bg - ag) * t);
        int rb = Math.round(ab + (bb - ab) * t);
        return (ra << 24) | (rr << 16) | (rg << 8) | rb;
    }

    public static int fade(int color, float alpha) {
        alpha = Math.max(0f, Math.min(1f, alpha));
        int a = Math.round(((color >>> 24) & 0xFF) * alpha);
        return (a << 24) | (color & 0xFFFFFF);
    }

    public static boolean inside(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    private static int inset(int r, int i) {
        int dy = r - i;
        double dx = Math.sqrt((double) r * r - (double) dy * dy);
        return r - (int) Math.floor(dx);
    }

    public static void roundGradient(GuiGraphicsExtractor g, int x, int y, int w, int h, int r, int top, int bot) {
        r = Math.max(0, Math.min(r, Math.min(w, h) / 2));
        for (int row = 0; row < h; row++) {
            int ins = 0;
            if (row < r) {
                ins = inset(r, row);
            } else if (row >= h - r) {
                ins = inset(r, h - 1 - row);
            }
            float t = h <= 1 ? 0f : (float) row / (h - 1);
            g.fill(x + ins, y + row, x + w - ins, y + row + 1, mix(top, bot, t));
        }
    }

    public static void roundFill(GuiGraphicsExtractor g, int x, int y, int w, int h, int r, int color) {
        roundGradient(g, x, y, w, h, r, color, color);
    }

    public static void roundOutline(GuiGraphicsExtractor g, int x, int y, int w, int h, int r, int color) {
        r = Math.max(0, Math.min(r, Math.min(w, h) / 2));
        for (int row = 0; row < h; row++) {
            int ins = 0;
            if (row < r) {
                ins = inset(r, row);
            } else if (row >= h - r) {
                ins = inset(r, h - 1 - row);
            }
            if (row == 0 || row == h - 1) {
                g.fill(x + ins, y + row, x + w - ins, y + row + 1, color);
            } else {
                g.fill(x + ins, y + row, x + ins + 1, y + row + 1, color);
                g.fill(x + w - ins - 1, y + row, x + w - ins, y + row + 1, color);
            }
        }
    }

    public static void shadow(GuiGraphicsExtractor g, int x, int y, int w, int h, int r) {
        roundFill(g, x - 1, y, w + 2, h + 3, r + 1, 0x14000000);
        roundFill(g, x - 2, y + 1, w + 4, h + 5, r + 2, 0x0C000000);
    }

    public static void panel(GuiGraphicsExtractor g, int x, int y, int w, int h) {
        shadow(g, x, y, w, h, 5);
        roundGradient(g, x, y, w, h, 5, PANEL_TOP, PANEL_BOT);
        roundOutline(g, x, y, w, h, 5, BORDER);
    }

    public static void hBar(GuiGraphicsExtractor g, int x, int y, int w, int h, int left, int right) {
        for (int i = 0; i < w; i++) {
            float t = w <= 1 ? 0f : (float) i / (w - 1);
            g.fill(x + i, y, x + i + 1, y + h, mix(left, right, t));
        }
    }

    /** Dark rounded card used by the HUD panels (diamonds, effects). */
    public static void card(GuiGraphicsExtractor g, int x, int y, int w, int h) {
        roundFill(g, x, y, w, h, 6, 0xE6101115);
        roundOutline(g, x, y, w, h, 6, 0xFF26272E);
    }
}
