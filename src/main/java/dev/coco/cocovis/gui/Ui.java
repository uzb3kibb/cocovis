package dev.coco.cocovis.gui;

import dev.coco.cocovis.CocoVisClient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.Identifier;

import java.lang.reflect.Method;

/**
 * Drawing helpers. Shapes are rasterised at real-pixel precision with anti-aliased edges
 * (not in blocky GUI pixels), text uses the bundled Inter font.
 */
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

    // ------------------------------------------------------------------ colors / hit tests

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

    public static boolean inside(double mx, double my, float x, float y, float w, float h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    // ------------------------------------------------------------------ real-pixel raster core

    private static int guiScale() {
        return Math.max(1, (int) Math.round(Minecraft.getInstance().getWindow().getGuiScale()));
    }

    /** Switches to "1 unit = 1 real screen pixel" coordinates with the origin at (x, y). */
    private static int begin(GuiGraphicsExtractor g, float x, float y) {
        int gs = guiScale();
        g.pose().pushMatrix();
        g.pose().translate(Math.round(x * gs) / (float) gs, Math.round(y * gs) / (float) gs);
        g.pose().scale(1f / gs, 1f / gs);
        return gs;
    }

    private static void end(GuiGraphicsExtractor g) {
        g.pose().popMatrix();
    }

    /** Horizontal inset of a rounded rect's edge at pixel row {@code row} (radius r, height h, real pixels). */
    private static double inset(int r, int h, int row) {
        if (r <= 0) {
            return 0;
        }
        double yc = row + 0.5;
        double dy;
        if (yc < r) {
            dy = r - yc;
        } else if (yc > h - r) {
            dy = yc - (h - r);
        } else {
            return 0;
        }
        return r - Math.sqrt(Math.max(0.0, (double) r * r - dy * dy));
    }

    private static void px(GuiGraphicsExtractor g, int x, int row, int color, double coverage) {
        if (coverage <= 0.02) {
            return;
        }
        g.fill(x, row, x + 1, row + 1, fade(color, (float) Math.min(1.0, coverage)));
    }

    /** One pixel row filled from real-pixel x = a to x = b with anti-aliased ends. */
    private static void span(GuiGraphicsExtractor g, int row, double a, double b, int color) {
        if (b <= a) {
            return;
        }
        int pa = (int) Math.floor(a);
        int pb = (int) Math.floor(b);
        if (pa == pb) {
            px(g, pa, row, color, b - a);
            return;
        }
        int fullStart = pa + 1;
        double cl = pa + 1 - a;
        if (cl < 0.98) {
            px(g, pa, row, color, cl);
        } else {
            fullStart = pa;
        }
        int fullEnd = pb;
        double cr = b - pb;
        if (cr > 0.02) {
            px(g, pb, row, color, cr);
        }
        if (fullEnd > fullStart) {
            g.fill(fullStart, row, fullEnd, row + 1, color);
        }
    }

    // ------------------------------------------------------------------ shapes

    public static void roundGradient(GuiGraphicsExtractor g, float x, float y, float w, float h, float r, int top, int bot) {
        if (w <= 0 || h <= 0) {
            return;
        }
        int gs = begin(g, x, y);
        int pw = Math.round(w * gs);
        int ph = Math.round(h * gs);
        if (pw <= 0 || ph <= 0) {
            end(g);
            return;
        }
        int pr = Math.max(0, Math.min(Math.round(r * gs), Math.min(pw, ph) / 2));
        boolean flat = top == bot;
        for (int row = 0; row < pr; row++) {
            double a = inset(pr, ph, row);
            span(g, row, a, pw - a, flat ? top : mix(top, bot, row / (float) Math.max(1, ph - 1)));
            int mirror = ph - 1 - row;
            span(g, mirror, a, pw - a, flat ? bot : mix(top, bot, mirror / (float) Math.max(1, ph - 1)));
        }
        if (ph - 2 * pr > 0) {
            if (flat) {
                g.fill(0, pr, pw, ph - pr, top);
            } else {
                for (int row = pr; row < ph - pr; row++) {
                    g.fill(0, row, pw, row + 1, mix(top, bot, row / (float) Math.max(1, ph - 1)));
                }
            }
        }
        end(g);
    }

    public static void roundFill(GuiGraphicsExtractor g, float x, float y, float w, float h, float r, int color) {
        roundGradient(g, x, y, w, h, r, color, color);
    }

    /** 1 GUI pixel (= {@code guiScale} real pixels) thick outline. */
    public static void roundOutline(GuiGraphicsExtractor g, float x, float y, float w, float h, float r, int color) {
        if (w <= 1 || h <= 1) {
            return;
        }
        int gs = begin(g, x, y);
        int t = gs;
        int pw = Math.round(w * gs);
        int ph = Math.round(h * gs);
        if (pw <= 2 * t || ph <= 2 * t) {
            end(g);
            return;
        }
        int pr = Math.max(0, Math.min(Math.round(r * gs), Math.min(pw, ph) / 2));
        int ir = Math.max(0, pr - t);
        int iw = pw - 2 * t;
        int ih = ph - 2 * t;
        for (int row = 0; row < ph; row++) {
            if (row >= pr && row < ph - pr) {
                continue; // straight part handled below
            }
            double a0 = inset(pr, ph, row);
            if (row < t || row >= ph - t) {
                span(g, row, a0, pw - a0, color);
            } else {
                double ai = t + inset(ir, ih, row - t);
                span(g, row, a0, ai, color);
                span(g, row, pw - ai, pw - a0, color);
            }
        }
        if (ph - 2 * pr > 0) {
            g.fill(0, pr, t, ph - pr, color);
            g.fill(pw - t, pr, pw, ph - pr, color);
        }
        end(g);
    }

    public static void shadow(GuiGraphicsExtractor g, float x, float y, float w, float h, float r) {
        roundFill(g, x - 1, y, w + 2, h + 3, r + 1, 0x14000000);
        roundFill(g, x - 2, y + 1, w + 4, h + 5, r + 2, 0x0C000000);
    }
    
    // Fast GUI-resolution primitives for frequently redrawn screens.
    // Unlike the high-resolution rasterizer above, these issue one fill per GUI row,
    // avoiding thousands of tiny real-pixel fill operations every frame.
    public static void fastRoundGradient(GuiGraphicsExtractor g, float x, float y, float w, float h,
                                         float radius, int top, int bottom) {
        int ix = Math.round(x), iy = Math.round(y);
        int iw = Math.max(1, Math.round(w)), ih = Math.max(1, Math.round(h));
        int r = Math.max(0, Math.min(Math.round(radius), Math.min(iw, ih) / 2));
        for (int row = 0; row < ih; row++) {
            double inset = inset(r, ih, row);
            int left = ix + (int) Math.ceil(inset);
            int right = ix + iw - (int) Math.ceil(inset);
            if (right > left) {
                int color = top == bottom ? top : mix(top, bottom, row / (float) Math.max(1, ih - 1));
                g.fill(left, iy + row, right, iy + row + 1, color);
            }
        }
    }

    public static void fastRoundFill(GuiGraphicsExtractor g, float x, float y, float w, float h,
                                     float radius, int color) {
        fastRoundGradient(g, x, y, w, h, radius, color, color);
    }

    public static void fastRoundOutline(GuiGraphicsExtractor g, float x, float y, float w, float h,
                                        float radius, int color) {
        int ix = Math.round(x), iy = Math.round(y);
        int iw = Math.max(1, Math.round(w)), ih = Math.max(1, Math.round(h));
        int r = Math.max(0, Math.min(Math.round(radius), Math.min(iw, ih) / 2));
        int t = 1;
        int innerW = iw - 2 * t, innerH = ih - 2 * t;
        int innerR = Math.max(0, r - t);
        for (int row = 0; row < ih; row++) {
            double outer = inset(r, ih, row);
            int left = ix + (int) Math.ceil(outer);
            int right = ix + iw - (int) Math.ceil(outer);
            if (row < t || row >= ih - t || innerW <= 0 || innerH <= 0) {
                if (right > left) g.fill(left, iy + row, right, iy + row + 1, color);
            } else {
                double inner = inset(innerR, innerH, row - t);
                int innerLeft = ix + t + (int) Math.ceil(inner);
                int innerRight = ix + iw - t - (int) Math.ceil(inner);
                if (innerLeft > left) g.fill(left, iy + row, innerLeft, iy + row + 1, color);
                if (right > innerRight) g.fill(innerRight, iy + row, right, iy + row + 1, color);
            }
        }
    }

    public static void fastShadow(GuiGraphicsExtractor g, float x, float y, float w, float h, float r) {
        fastRoundFill(g, x - 1, y, w + 2, h + 3, r + 1, 0x14000000);
        fastRoundFill(g, x - 2, y + 1, w + 4, h + 5, r + 2, 0x0C000000);
    }

    public static void fastCircle(GuiGraphicsExtractor g, float x, float y, float d, int color) {
        fastRoundFill(g, x, y, d, d, d / 2f, color);
    }

    public static void fastRing(GuiGraphicsExtractor g, float x, float y, float d, int color) {
        fastRoundOutline(g, x, y, d, d, d / 2f, color);
    }

    public static void panel(GuiGraphicsExtractor g, float x, float y, float w, float h) {
        shadow(g, x, y, w, h, 6);
        roundGradient(g, x, y, w, h, 6, PANEL_TOP, PANEL_BOT);
        roundOutline(g, x, y, w, h, 6, BORDER);
    }

    /** Plain rectangle at real-pixel precision (for dividers, lines, sliders). */
    public static void rect(GuiGraphicsExtractor g, float x, float y, float w, float h, int color) {
        if (w <= 0 || h <= 0) {
            return;
        }
        int gs = begin(g, x, y);
        int pw = Math.max(1, Math.round(w * gs));
        int ph = Math.max(1, Math.round(h * gs));
        g.fill(0, 0, pw, ph, color);
        end(g);
    }

    public static void hBar(GuiGraphicsExtractor g, int x, int y, int w, int h, int left, int right) {
        for (int i = 0; i < w; i++) {
            float t = w <= 1 ? 0f : (float) i / (w - 1);
            g.fill(x + i, y, x + i + 1, y + h, mix(left, right, t));
        }
    }

    /** Dark rounded card used by the HUD panels (diamonds, effects). */
    public static void card(GuiGraphicsExtractor g, float x, float y, float w, float h) {
        roundFill(g, x, y, w, h, 7, 0xE6101115);
        roundOutline(g, x, y, w, h, 7, 0xFF26272E);
    }

    public static void circle(GuiGraphicsExtractor g, float x, float y, float d, int color) {
        roundFill(g, x, y, d, d, d / 2f, color);
    }

    public static void ring(GuiGraphicsExtractor g, float x, float y, float d, int color) {
        roundOutline(g, x, y, d, d, d / 2f, color);
    }

    /** Filled diamond (rotated square) inside a size x size box. */
    public static void diamond(GuiGraphicsExtractor g, float x, float y, float size, int color) {
        int gs = begin(g, x, y);
        int ps = Math.round(size * gs);
        double half = ps / 2.0;
        for (int row = 0; row < ps; row++) {
            double hw = half * (1.0 - Math.abs(row + 0.5 - half) / half);
            span(g, row, half - hw, half + hw, color);
        }
        end(g);
    }

    /** Filled triangle pointing down (w x h box). */
    public static void triangleDown(GuiGraphicsExtractor g, float x, float y, float w, float h, int color) {
        int gs = begin(g, x, y);
        int pw = Math.round(w * gs);
        int ph = Math.round(h * gs);
        for (int row = 0; row < ph; row++) {
            double hw = pw / 2.0 * (1.0 - (row + 0.5) / ph);
            span(g, row, pw / 2.0 - hw, pw / 2.0 + hw, color);
        }
        end(g);
    }

    /** Filled triangle pointing right (w x h box). */
    public static void triangleRight(GuiGraphicsExtractor g, float x, float y, float w, float h, int color) {
        int gs = begin(g, x, y);
        int pw = Math.round(w * gs);
        int ph = Math.round(h * gs);
        double half = ph / 2.0;
        for (int row = 0; row < ph; row++) {
            double len = pw * (1.0 - Math.abs(row + 0.5 - half) / half);
            span(g, row, 0, len, color);
        }
        end(g);
    }

    // ------------------------------------------------------------------ text (Inter via font providers)

    private static Style uiStyle;
    private static Style titleStyle;
    private static boolean styleInit;
    private static Method textMethod;
    private static boolean textFailed;

    /** Style that selects the cocovis:<path> font, or null if the game cannot apply it. */
    public static Style fontStyle(String path) {
        try {
            Identifier id = Identifier.fromNamespaceAndPath(CocoVisClient.MOD_ID, path);
            for (Method m : Style.class.getMethods()) {
                if (!m.getName().equals("withFont") || m.getParameterCount() != 1) {
                    continue;
                }
                Class<?> p = m.getParameterTypes()[0];
                Object arg;
                if (p.isAssignableFrom(Identifier.class)) {
                    arg = id;
                } else {
                    try {
                        Class<?> res = Class.forName("net.minecraft.network.chat.FontDescription$Resource");
                        arg = res.getConstructor(Identifier.class).newInstance(id);
                    } catch (Throwable t) {
                        continue;
                    }
                    if (!p.isInstance(arg)) {
                        continue;
                    }
                }
                return (Style) m.invoke(Style.EMPTY, arg);
            }
        } catch (Throwable t) {
            CocoVisClient.LOGGER.warn("CocoVis UI font could not be applied: {}", String.valueOf(t));
        }
        return null;
    }

    private static void initText() {
        if (styleInit) {
            return;
        }
        styleInit = true;
        uiStyle = fontStyle("ui");
        titleStyle = fontStyle("title");
        try {
            for (Method m : GuiGraphicsExtractor.class.getMethods()) {
                Class<?>[] p = m.getParameterTypes();
                if (m.getName().equals("text") && p.length == 6 && p[1] == Component.class
                        && p[2] == int.class && p[5] == boolean.class) {
                    textMethod = m;
                    break;
                }
            }
        } catch (Throwable t) {
            textFailed = true;
        }
        if (textMethod == null || uiStyle == null) {
            textFailed = true;
            CocoVisClient.LOGGER.warn("CocoVis falls back to the vanilla font (custom font style unavailable)");
        }
    }

    private static Component styled(String s, boolean title) {
        Style st = title && titleStyle != null ? titleStyle : uiStyle;
        return Component.literal(s).withStyle(st);
    }

    /** Draws text with the UI font. Shadows are dropped for a flat look when the custom font works. */
    public static void text(GuiGraphicsExtractor g, Font font, String s, float x, float y, int color, boolean shadow) {
        draw(g, font, s, x, y, color, shadow, false);
    }

    /** Semi-bold variant for titles. */
    public static void title(GuiGraphicsExtractor g, Font font, String s, float x, float y, int color) {
        draw(g, font, s, x, y, color, false, true);
    }

    private static void draw(GuiGraphicsExtractor g, Font font, String s, float x, float y, int color, boolean shadow, boolean title) {
        initText();
        int ix = (int) Math.floor(x);
        int iy = (int) Math.floor(y);
        if (!textFailed) {
            int gs = guiScale();
            float fx = Math.round((x - ix) * gs) / (float) gs;
            float fy = Math.round((y - iy) * gs) / (float) gs;
            boolean shifted = fx != 0f || fy != 0f;
            try {
                if (shifted) {
                    g.pose().pushMatrix();
                    g.pose().translate(fx, fy);
                }
                textMethod.invoke(g, font, styled(s, title), ix, iy, color, false);
                if (shifted) {
                    g.pose().popMatrix();
                }
                return;
            } catch (Throwable t) {
                if (shifted) {
                    g.pose().popMatrix();
                }
                textFailed = true;
            }
        }
        g.text(font, s, ix, iy, color, shadow);
    }

    public static int width(Font font, String s) {
        initText();
        if (!textFailed) {
            return font.width(styled(s, false));
        }
        return font.width(s);
    }

    public static int titleWidth(Font font, String s) {
        initText();
        if (!textFailed) {
            return font.width(styled(s, true));
        }
        return font.width(s);
    }
}
