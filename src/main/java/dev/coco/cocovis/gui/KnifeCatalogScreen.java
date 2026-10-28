package dev.coco.cocovis.gui;

import com.mojang.blaze3d.platform.InputConstants;
import dev.coco.cocovis.Knives;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.HashMap;
import java.util.Map;

/**
 * Каталог ножей: сетка квадратных плиток с настоящими моделями.
 * Первая страница — выбор («Стандартные мечи» / Huntsman), вторая — скины Huntsman.
 * Плиток может быть сколько угодно: они переносятся на следующие ряды, лишнее прокручивается колесом мыши.
 */
public class KnifeCatalogScreen extends Screen {
    private enum Page { KNIVES, SKINS }

    private static final int TILE = 84, GAP = 10, NAME_H = 16;
    private static final String NS = "cocovis";
    /** Скины по порядку Knives.SKIN_IDS (последний, «Авто», рисуется отдельно). */
    private static final String[] SKIN_ITEMS = {"huntsman_vanilla", "huntsman_rust", "huntsman_sapphire", "huntsman_dark"};
    private static final String[] SWORD_ITEMS = {"wooden_sword", "iron_sword", "diamond_sword", "netherite_sword"};
    /** «Авто»: какой скин получает какой меч. */
    private static final String[] AUTO_ITEMS = {"huntsman_rust", "huntsman_vanilla", "huntsman_sapphire", "huntsman_dark"};

    private final Map<String, ItemStack> stacks = new HashMap<>();
    private Page page = Page.KNIVES;
    private long lastNs = System.nanoTime();
    private float open = 0f;
    private int mX, mY, scroll;
    private final float[] hover = new float[32];

    private int x0, y0, w, h, gridX, gridTop, gridBottom, cols;

    public KnifeCatalogScreen() {
        super(Component.literal("Ножи"));
    }

    /** Стек с «моделью предмета» из нашего пространства имён — рисуется независимо от выбранного ножа. */
    private ItemStack preview(String id) {
        return stacks.computeIfAbsent(id, k -> {
            ItemStack s = new ItemStack(Items.PAPER);
            s.set(DataComponents.ITEM_MODEL, Identifier.fromNamespaceAndPath(NS, k));
            return s;
        });
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
        gridTop = y0 + 82;
        gridBottom = y0 + h - 30;
        cols = Math.max(1, (w - 32 + GAP) / (TILE + GAP));
    }

    private int count() { return page == Page.KNIVES ? 2 : Knives.SKIN_IDS.length; }

    private int tileX(int i) { return gridX + (i % cols) * (TILE + GAP); }
    private int tileY(int i) { return gridTop + (i / cols) * (TILE + GAP) - scroll; }

    private int maxScroll() {
        int rows = (count() + cols - 1) / cols;
        return Math.max(0, rows * (TILE + GAP) - GAP - (gridBottom - gridTop));
    }

    private boolean backHit(int mx, int my) { return Ui.inside(mx, my, x0 + 14, y0 + 12, 74, 18); }

    private boolean inGrid(int mx, int my) { return my >= gridTop && my < gridBottom && mx >= gridX && mx < x0 + w - 12; }

    private boolean isSelected(int i) {
        if (page == Page.KNIVES) return i == 0 ? !Knives.isActive() : Knives.isActive();
        return Knives.isActive() && Knives.skinIndex() == i;
    }

    private String name(int i) {
        if (page == Page.KNIVES) return i == 0 ? "Стандартные мечи" : "Huntsman";
        return Knives.SKIN_NAMES[i];
    }

    private String info(int i) {
        if (page == Page.KNIVES) return i == 0 ? "Обычные модели Minecraft" : "Нож из CS2 со своими анимациями. Внутри каталог скинов";
        return Knives.SKIN_INFO[i];
    }

    private String fit(String s, int maxW) {
        if (font.width(s) <= maxW) return s;
        while (s.length() > 1 && font.width(s + "…") > maxW) s = s.substring(0, s.length() - 1);
        return s + "…";
    }

    private void bigText(GuiGraphicsExtractor g, String s, int x, int y, float scale, int color) {
        var pose = g.pose();
        pose.pushMatrix();
        pose.translate((float) x, (float) y);
        pose.scale(scale, scale);
        g.text(font, s, 0, 0, color, true);
        pose.popMatrix();
    }

    /** Рисует предмет в масштабе scale (16 пикселей × scale) с левым верхним углом в (x, y). */
    private void item(GuiGraphicsExtractor g, String id, int x, int y, float scale) {
        var pose = g.pose();
        pose.pushMatrix();
        pose.translate((float) x, (float) y);
        pose.scale(scale, scale);
        g.item(preview(id), 0, 0);
        pose.popMatrix();
    }

    /** Содержимое плитки: настоящие модели. */
    private void tileArt(GuiGraphicsExtractor g, int i, int ax, int ay, int size) {
        if (page == Page.KNIVES) {
            if (i == 0) {
                grid4(g, SWORD_ITEMS, ax, ay, size);
            } else {
                String id = Knives.isActive() && Knives.skinIndex() < SKIN_ITEMS.length ? SKIN_ITEMS[Knives.skinIndex()] : SKIN_ITEMS[0];
                item(g, id, ax, ay, size / 16f);
            }
        } else if (i < SKIN_ITEMS.length) {
            item(g, SKIN_ITEMS[i], ax, ay, size / 16f);
        } else {
            grid4(g, AUTO_ITEMS, ax, ay, size);   // «Авто»: по ножу на каждый материал
        }
    }

    /** Четыре предмета 2×2. */
    private void grid4(GuiGraphicsExtractor g, String[] ids, int ax, int ay, int size) {
        int half = size / 2;
        for (int k = 0; k < 4; k++) item(g, ids[k], ax + (k % 2) * half, ay + (k / 2) * half, half / 16f);
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
        scroll = Math.max(0, Math.min(maxScroll(), scroll));

        g.fill(0, 0, width, height, c(0x99000000));
        Ui.shadow(g, x0, y0, w, h, 12);
        Ui.roundGradient(g, x0, y0, w, h, 12, c(Ui.BG_TOP), c(Ui.BG_BOT));
        Ui.roundOutline(g, x0, y0, w, h, 12, c(Ui.BORDER));
        Ui.hBar(g, x0 + 18, y0 + 1, w - 36, 2, c(Ui.ACCENT), c(Ui.ACCENT2));

        // «Назад» и «хлебные крошки»
        boolean bh = backHit(mX, mY);
        Ui.roundGradient(g, x0 + 14, y0 + 12, 74, 18, 7, c(bh ? Ui.CARD_HOT_TOP : Ui.CARD_TOP), c(bh ? Ui.CARD_HOT_BOT : Ui.CARD_BOT));
        Ui.roundOutline(g, x0 + 14, y0 + 12, 74, 18, 7, c(bh ? 0x885CC8FF : 0x30FFFFFF));
        String back = "< Назад";
        g.text(f, back, x0 + 14 + 37 - f.width(back) / 2, y0 + 17, c(Ui.TEXT), false);
        g.text(f, page == Page.KNIVES ? "Ножи" : "Ножи / Huntsman", x0 + 98, y0 + 17, c(Ui.DIM), false);

        // заголовок
        bigText(g, page == Page.KNIVES ? "Каталог ножей" : "Скины Huntsman", x0 + 18, y0 + 40, 1.35f, c(Ui.TEXT));
        g.text(f, fit(Knives.describe(), w - 36), x0 + 18, y0 + 60, c(Ui.MUTED), false);
        Ui.hBar(g, x0 + 16, y0 + 74, w - 32, 1, c(0x30FFFFFF), c(0x00FFFFFF));

        // сетка квадратных плиток
        int hoveredTile = -1;
        g.enableScissor(x0 + 8, gridTop - 2, x0 + w - 8, gridBottom);
        for (int i = 0; i < count(); i++) {
            int tx = tileX(i), ty = tileY(i);
            if (ty + TILE < gridTop - 2 || ty > gridBottom) continue;
            boolean hov = inGrid(mX, mY) && Ui.inside(mX, mY, tx, ty, TILE, TILE);
            if (hov) hoveredTile = i;
            hover[i] += Math.max(-dt * 8f, Math.min(dt * 8f, (hov ? 1f : 0f) - hover[i]));
            float ha = hover[i];
            boolean sel = isSelected(i);

            Ui.roundGradient(g, tx, ty, TILE, TILE, 9,
                    c(Ui.mix(Ui.CARD_TOP, Ui.CARD_HOT_TOP, ha)), c(Ui.mix(Ui.CARD_BOT, Ui.CARD_HOT_BOT, ha)));
            Ui.roundOutline(g, tx, ty, TILE, TILE, 9,
                    c(sel ? 0xDD5CC8FF : Ui.mix(0x1AFFFFFF, 0x665CC8FF, ha * 0.8f)));

            // тёмная «витрина» под моделью
            int art = TILE - 16;
            Ui.roundGradient(g, tx + 8, ty + 6, art, art - 6, 7, c(0xFF101620), c(0xFF0B0F16));
            tileArt(g, i, tx + 8 + 2, ty + 6 + 1, art - 4);

            // название
            String nm = fit(name(i), TILE - 12);
            g.text(f, nm, tx + TILE / 2 - f.width(nm) / 2, ty + TILE - NAME_H + 3, c(sel ? Ui.TEXT : Ui.MUTED), true);

            // значок «выбрано»: кружок с галочкой
            if (sel) {
                int cx = tx + TILE - 14, cy = ty + 14;
                Ui.circle(g, cx, cy, 6, c(Ui.ACCENT));
                int[][] tick = {{-3, 0}, {-2, 1}, {-1, 2}, {0, 1}, {1, 0}, {2, -1}, {3, -2}};
                for (int[] p : tick) g.fill(cx + p[0], cy + p[1], cx + p[0] + 1, cy + p[1] + 1, c(0xFF0B0F16));
            }
        }
        g.disableScissor();

        // подпись к плитке под курсором либо подсказка
        String foot = hoveredTile >= 0 ? info(hoveredTile) : "Осмотр ножа: клавиша H   ·   колесо: прокрутка   ·   Esc: назад";
        g.text(f, fit(foot, w - 36), x0 + 18, y0 + h - 17, c(hoveredTile >= 0 ? Ui.MUTED : Ui.DIM), false);

        // полоса прокрутки
        if (maxScroll() > 0) {
            int trackX = x0 + w - 11, viewH = gridBottom - gridTop;
            Ui.roundFill(g, trackX, gridTop, 3, viewH, 1, c(0x20FFFFFF));
            int thumbH = Math.max(18, viewH * viewH / (viewH + maxScroll()));
            int thumbY = gridTop + (int) ((viewH - thumbH) * (scroll / (float) maxScroll()));
            Ui.roundGradient(g, trackX, thumbY, 3, thumbH, 1, c(Ui.ACCENT), c(Ui.ACCENT2));
        }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        // Кнопку мыши не проверяем: в 26.3 коды кнопок изменились (SDL). Позиция берётся из кадра отрисовки.
        layout();
        if (backHit(mX, mY)) {
            goBack();
            return true;
        }
        if (inGrid(mX, mY)) {
            for (int i = 0; i < count(); i++) {
                if (!Ui.inside(mX, mY, tileX(i), tileY(i), TILE, TILE)) continue;
                if (page == Page.KNIVES) {
                    if (i == 0) Knives.selectDefault();
                    else page = Page.SKINS;
                    scroll = 0;
                } else {
                    Knives.selectSkin(i);   // выбор ножа сразу включает и его анимации
                }
                return true;
            }
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseScrolled(double x, double y, double scrollX, double scrollY) {
        scroll -= (int) (scrollY * 26);
        return true;
    }

    private void goBack() {
        if (page == Page.SKINS) {
            page = Page.KNIVES;
            scroll = 0;
        } else {
            minecraft.gui.setScreen(new VisualsScreen());
        }
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
