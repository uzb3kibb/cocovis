package dev.coco.cocovis.gui;

import com.mojang.blaze3d.platform.InputConstants;
import dev.coco.cocovis.Knives;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

/** Каталог ножей: сначала выбор ножа (стандартные мечи / Huntsman), затем каталог скинов Huntsman. */
public class KnifeCatalogScreen extends Screen {
    private enum Page { KNIVES, SKINS }

    private static final int CARD_H = 104, GAP = 10, COLS = 3;

    // цвета нарисованных ножей: клинок сверху/снизу, рукоять сверху/снизу, гарда
    private static final int[][] SKIN_COLORS = {
            {0xFFDCE6EF, 0xFF6E86A0, 0xFF26282C, 0xFF111214, 0xFF8D98A1},   // Стандартный
            {0xFFB9C4CC, 0xFFA8693A, 0xFF1D1E21, 0xFF0D0E10, 0xFF7B5A44},   // Ржавый
            {0xFF8BA0FF, 0xFF1B2CA8, 0xFF1A2252, 0xFF0A0B17, 0xFF3550D8},   // Сапфир
            {0xFF7A8187, 0xFF1C1F22, 0xFFEEEBE2, 0xFF9A968A, 0xFF5A6065},   // Тёмный
    };
    private static final int[] DEFAULT_COLORS = {0xFFD9DEE3, 0xFF8A929A, 0xFF8A5A2B, 0xFF5B3A1A, 0xFFB08D57};

    private Page page = Page.KNIVES;
    private long lastNs = System.nanoTime();
    private float open = 0f;
    private int mX, mY;
    private final float[] hover = new float[8];

    private int x0, y0, w, h, gridX, gridY, cardW;

    public KnifeCatalogScreen() {
        super(Component.literal("Ножи"));
    }

    private float ease() {
        float t = Math.max(0f, Math.min(1f, open));
        return 1f - (1f - t) * (1f - t) * (1f - t);
    }

    private int c(int color) { return Ui.fade(color, ease()); }

    private void layout() {
        w = Math.min(width - 24, 470);
        h = Math.min(height - 24, 292);
        x0 = (width - w) / 2;
        y0 = (height - h) / 2 + Math.round((1f - ease()) * 12);
        gridX = x0 + 16;
        gridY = y0 + 82;
        cardW = (w - 32 - GAP * (COLS - 1)) / COLS;
    }

    private int count() { return page == Page.KNIVES ? 2 : Knives.SKIN_IDS.length; }

    private int cardX(int i) { return gridX + (i % COLS) * (cardW + GAP); }
    private int cardY(int i) { return gridY + (i / COLS) * (CARD_H + GAP); }

    private boolean backHit(int mx, int my) { return Ui.inside(mx, my, x0 + 14, y0 + 12, 74, 18); }

    private boolean isSelected(int i) {
        if (page == Page.KNIVES) return i == 0 ? !Knives.isActive() : Knives.isActive();
        return Knives.isActive() && Knives.skinIndex() == i;
    }

    private String name(int i) {
        if (page == Page.KNIVES) return i == 0 ? "Стандартные мечи" : "Huntsman";
        return Knives.SKIN_NAMES[i];
    }

    private String info(int i) {
        if (page == Page.KNIVES) return i == 0 ? "Обычные модели Minecraft" : "Нож из CS2. Внутри каталог скинов";
        return Knives.SKIN_INFO[i];
    }

    private String fit(String s, int maxW) {
        if (font.width(s) <= maxW) return s;
        while (s.length() > 1 && font.width(s + "…") > maxW) s = s.substring(0, s.length() - 1);
        return s + "…";
    }

    private List<String> wrap(String text, int maxW, int maxLines) {
        List<String> out = new ArrayList<>();
        String[] words = text.split(" ");
        StringBuilder cur = new StringBuilder();
        int i = 0;
        for (; i < words.length; i++) {
            String test = cur.length() == 0 ? words[i] : cur + " " + words[i];
            if (font.width(test) <= maxW || cur.length() == 0) {
                cur.setLength(0);
                cur.append(test);
            } else {
                if (out.size() == maxLines - 1) break;
                out.add(cur.toString());
                cur.setLength(0);
                cur.append(words[i]);
            }
        }
        if (cur.length() > 0) {
            if (i < words.length) {
                StringBuilder rest = new StringBuilder(cur);
                for (int k = i; k < words.length; k++) rest.append(' ').append(words[k]);
                out.add(fit(rest.toString(), maxW));
            } else {
                out.add(fit(cur.toString(), maxW));
            }
        }
        return out;
    }

    private void bigText(GuiGraphicsExtractor g, String s, int x, int y, float scale, int color) {
        var pose = g.pose();
        pose.pushMatrix();
        pose.translate((float) x, (float) y);
        pose.scale(scale, scale);
        g.text(font, s, 0, 0, color, true);
        pose.popMatrix();
    }

    /** Рисует превью карточки: нож нужной расцветки. */
    private void preview(GuiGraphicsExtractor g, int i, int px, int py, int pw, int ph) {
        Ui.roundGradient(g, px, py, pw, ph, 8, c(0xFF121821), c(0xFF0C1016));
        Ui.roundOutline(g, px, py, pw, ph, 8, c(0x14FFFFFF));
        int gw = Math.min(pw - 20, 86);
        int gx = px + (pw - gw) / 2, gy = py + (ph - 22) / 2;
        if (page == Page.KNIVES && i == 0) {
            int[] k = DEFAULT_COLORS;
            Ui.knifeGlyph(g, gx, gy, gw, c(k[0]), c(k[1]), c(k[2]), c(k[3]), c(k[4]));
        } else if (page == Page.SKINS && i == 4) {
            // «Авто»: четыре ножа подряд, по одному на материал
            int seg = gw / 4;
            for (int s = 0; s < 4; s++) {
                int[] k = SKIN_COLORS[s];
                Ui.knifeGlyph(g, px + 8 + s * ((pw - 16) / 4), gy, (pw - 16) / 4 - 2, c(k[0]), c(k[1]), c(k[2]), c(k[3]), c(k[4]));
            }
        } else {
            int[] k = SKIN_COLORS[page == Page.KNIVES ? Knives.skinIndex() % 4 : Math.min(i, 3)];
            Ui.knifeGlyph(g, gx, gy, gw, c(k[0]), c(k[1]), c(k[2]), c(k[3]), c(k[4]));
        }
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
        mX = mouseX;
        mY = mouseY;
        Font f = font;
        long now = System.nanoTime();
        float dt = Math.min(0.1f, (now - lastNs) / 1_000_000_000f);
        lastNs = now;
        open = Math.min(1f, open + dt / 0.2f);
        layout();

        g.fill(0, 0, width, height, c(0x99000000));
        Ui.shadow(g, x0, y0, w, h, 12);
        Ui.roundGradient(g, x0, y0, w, h, 12, c(Ui.BG_TOP), c(Ui.BG_BOT));
        Ui.roundOutline(g, x0, y0, w, h, 12, c(Ui.BORDER));
        Ui.hBar(g, x0 + 18, y0 + 1, w - 36, 2, c(Ui.ACCENT), c(Ui.ACCENT2));

        // кнопка «Назад» и «хлебные крошки»
        boolean bh = backHit(mX, mY);
        Ui.roundGradient(g, x0 + 14, y0 + 12, 74, 18, 7, c(bh ? Ui.CARD_HOT_TOP : Ui.CARD_TOP), c(bh ? Ui.CARD_HOT_BOT : Ui.CARD_BOT));
        Ui.roundOutline(g, x0 + 14, y0 + 12, 74, 18, 7, c(bh ? 0x885CC8FF : 0x30FFFFFF));
        String back = "< Назад";
        g.text(f, back, x0 + 14 + 37 - f.width(back) / 2, y0 + 17, c(Ui.TEXT), false);
        String crumbs = page == Page.KNIVES ? "Ножи" : "Ножи / Huntsman";
        g.text(f, crumbs, x0 + 98, y0 + 17, c(Ui.DIM), false);

        // заголовок
        bigText(g, page == Page.KNIVES ? "Каталог ножей" : "Скины Huntsman", x0 + 18, y0 + 40, 1.35f, c(Ui.TEXT));
        String sub = page == Page.KNIVES
                ? "Выбери, что будет в руке вместо мечей"
                : "Скин применяется ко всем мечам: деревянному, железному, алмазному и незеритовому";
        g.text(f, fit(sub, w - 36), x0 + 18, y0 + 60, c(Ui.MUTED), false);
        Ui.hBar(g, x0 + 16, y0 + 74, w - 32, 1, c(0x30FFFFFF), c(0x00FFFFFF));

        // карточки
        for (int i = 0; i < count(); i++) {
            int cx = cardX(i), cy = cardY(i);
            boolean hov = Ui.inside(mX, mY, cx, cy, cardW, CARD_H);
            hover[i] += Math.max(-dt * 8f, Math.min(dt * 8f, (hov ? 1f : 0f) - hover[i]));
            float ha = hover[i];
            boolean sel = isSelected(i);

            Ui.roundGradient(g, cx, cy, cardW, CARD_H, 9,
                    c(Ui.mix(Ui.CARD_TOP, Ui.CARD_HOT_TOP, ha)), c(Ui.mix(Ui.CARD_BOT, Ui.CARD_HOT_BOT, ha)));
            Ui.roundOutline(g, cx, cy, cardW, CARD_H, 9,
                    c(sel ? 0xCC5CC8FF : Ui.mix(0x1AFFFFFF, 0x665CC8FF, ha * 0.8f)));

            preview(g, i, cx + 8, cy + 8, cardW - 16, 52);

            if (sel) {
                String badge = "Выбран";
                int bw = f.width(badge) + 12;
                Ui.roundGradient(g, cx + cardW - 8 - bw - 4, cy + 12, bw, 14, 7, c(Ui.ACCENT), c(Ui.ACCENT2));
                g.text(f, badge, cx + cardW - 8 - bw - 4 + 6, cy + 15, c(0xFF0B0F16), false);
            }

            g.text(f, fit(name(i), cardW - 16), cx + 10, cy + 66, c(Ui.TEXT), true);
            List<String> lines = wrap(info(i), cardW - 20, 2);
            for (int k = 0; k < lines.size(); k++) g.text(f, lines.get(k), cx + 10, cy + 79 + k * 10, c(Ui.MUTED), false);
        }

        g.text(f, "Осмотр ножа: клавиша H   ·   Esc: назад   ·   RShift: закрыть",
                x0 + 18, y0 + h - 17, c(Ui.DIM), false);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        // Кнопку мыши не проверяем: в 26.3 коды кнопок изменились (SDL). Позиция берётся из кадра отрисовки.
        layout();
        if (backHit(mX, mY)) {
            goBack();
            return true;
        }
        for (int i = 0; i < count(); i++) {
            if (!Ui.inside(mX, mY, cardX(i), cardY(i), cardW, CARD_H)) continue;
            if (page == Page.KNIVES) {
                if (i == 0) Knives.selectDefault();
                else page = Page.SKINS;
            } else {
                Knives.selectSkin(i);
            }
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    private void goBack() {
        if (page == Page.SKINS) page = Page.KNIVES;
        else minecraft.gui.setScreen(new VisualsScreen());
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        int key = event.key();
        if (key == InputConstants.KEY_RSHIFT) {
            onClose();
            return true;
        }
        if (key == InputConstants.KEY_ESCAPE) {
            goBack();
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public boolean isPauseScreen() { return false; }
}
