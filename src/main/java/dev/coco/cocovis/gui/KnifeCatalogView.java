package dev.coco.cocovis.gui;

import dev.coco.cocovis.Knives;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.HashMap;
import java.util.Map;
import java.util.function.IntUnaryOperator;

/**
 * Каталог ножей прямо внутри меню (категория «Ножи»): сетка квадратных плиток с настоящими моделями.
 * Первая страница — выбор («Стандартные мечи» / Huntsman), вторая — скины Huntsman.
 * Плитки переносятся на следующие ряды, лишнее прокручивается колесом мыши.
 */
public final class KnifeCatalogView {
    private enum Page { KNIVES, SKINS }

    private static final int TILE = 84, GAP = 10, NAME_H = 16;
    private static final String NS = "cocovis";
    /** Скины по порядку Knives.SKIN_IDS (последний, «Авто», рисуется отдельно). */
    private static final String[] SKIN_ITEMS = {
            "huntsman_vanilla", "huntsman_rust", "huntsman_sapphire", "huntsman_dark", "huntsman_fade",
            "huntsman_crimson", "huntsman_case", "huntsman_slaughter", "huntsman_night", "huntsman_forest"};
    private static final String[] SWORD_ITEMS = {"wooden_sword", "iron_sword", "diamond_sword", "netherite_sword"};
    /** «Авто»: какой скин получает какой меч. */
    private static final String[] AUTO_ITEMS = {"huntsman_rust", "huntsman_vanilla", "huntsman_sapphire", "huntsman_dark"};

    private final Map<String, ItemStack> stacks = new HashMap<>();
    private final float[] hover = new float[64];
    private Page page = Page.KNIVES;
    private int scroll;
    private int hovered = -1;

    // раскладка последнего кадра
    private int ax, ay, aw, ah, cols;

    public void reset() {
        page = Page.KNIVES;
        scroll = 0;
    }

    public boolean canGoBack() { return page == Page.SKINS; }

    /** Возврат на первую страницу каталога. false — если уже на ней. */
    public boolean back() {
        if (page == Page.SKINS) {
            page = Page.KNIVES;
            scroll = 0;
            return true;
        }
        return false;
    }

    public void scroll(double dy) { scroll -= (int) (dy * 26); }

    /** Описание плитки под курсором (для строки подсказки) или null. */
    public String hoverInfo() { return hovered >= 0 ? info(hovered) : null; }

    /** Стек с «моделью предмета» из нашего пространства имён — рисуется независимо от выбранного ножа. */
    private ItemStack preview(String id) {
        return stacks.computeIfAbsent(id, k -> {
            ItemStack s = new ItemStack(Items.PAPER);
            s.set(DataComponents.ITEM_MODEL, Identifier.fromNamespaceAndPath(NS, k));
            return s;
        });
    }

    private int count() { return page == Page.KNIVES ? 2 : Knives.SKIN_IDS.length; }

    private int tileX(int i) { return ax + (i % cols) * (TILE + GAP); }
    private int tileY(int i) { return ay + (i / cols) * (TILE + GAP) - scroll; }

    private int maxScroll() {
        int rows = (count() + cols - 1) / cols;
        return Math.max(0, rows * (TILE + GAP) - GAP - ah);
    }

    private boolean inArea(int mx, int my) { return mx >= ax && mx < ax + aw && my >= ay && my < ay + ah; }

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

    private static String fit(Font font, String s, int maxW) {
        if (font.width(s) <= maxW) return s;
        while (s.length() > 1 && font.width(s + "…") > maxW) s = s.substring(0, s.length() - 1);
        return s + "…";
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

    private void tileArt(GuiGraphicsExtractor g, int i, int x, int y, int size) {
        if (page == Page.KNIVES) {
            if (i == 0) {
                grid4(g, SWORD_ITEMS, x, y, size);
            } else {
                int idx = Knives.isActive() && Knives.skinIndex() < SKIN_ITEMS.length ? Knives.skinIndex() : 0;
                item(g, SKIN_ITEMS[idx], x, y, size / 16f);
            }
        } else if (i < SKIN_ITEMS.length) {
            item(g, SKIN_ITEMS[i], x, y, size / 16f);
        } else {
            grid4(g, AUTO_ITEMS, x, y, size);   // «Авто»: по ножу на каждый материал
        }
    }

    private void grid4(GuiGraphicsExtractor g, String[] ids, int x, int y, int size) {
        int half = size / 2;
        for (int k = 0; k < 4; k++) item(g, ids[k], x + (k % 2) * half, y + (k / 2) * half, half / 16f);
    }

    public void render(GuiGraphicsExtractor g, Font f, int mx, int my, float dt,
                       int x, int y, int w, int h, IntUnaryOperator c) {
        ax = x;
        ay = y;
        aw = w;
        ah = h;
        cols = Math.max(1, (w - 8 + GAP) / (TILE + GAP));
        scroll = Math.max(0, Math.min(maxScroll(), scroll));

        hovered = -1;
        g.enableScissor(x, y - 2, x + w, y + h);
        for (int i = 0; i < count(); i++) {
            int tx = tileX(i), ty = tileY(i);
            if (ty + TILE < y - 2 || ty > y + h) continue;
            boolean hov = inArea(mx, my) && Ui.inside(mx, my, tx, ty, TILE, TILE);
            if (hov) hovered = i;
            hover[i] += Math.max(-dt * 8f, Math.min(dt * 8f, (hov ? 1f : 0f) - hover[i]));
            float ha = hover[i];
            boolean sel = isSelected(i);

            Ui.roundGradient(g, tx, ty, TILE, TILE, 9,
                    c.applyAsInt(Ui.mix(Ui.CARD_TOP, Ui.CARD_HOT_TOP, ha)), c.applyAsInt(Ui.mix(Ui.CARD_BOT, Ui.CARD_HOT_BOT, ha)));
            Ui.roundOutline(g, tx, ty, TILE, TILE, 9,
                    c.applyAsInt(sel ? 0xDD5CC8FF : Ui.mix(0x1AFFFFFF, 0x665CC8FF, ha * 0.8f)));

            // тёмная «витрина» под моделью
            int art = TILE - 16;
            Ui.roundGradient(g, tx + 8, ty + 6, art, art - 6, 7, c.applyAsInt(0xFF101620), c.applyAsInt(0xFF0B0F16));
            tileArt(g, i, tx + 10, ty + 7, art - 4);

            String nm = fit(f, name(i), TILE - 12);
            g.text(f, nm, tx + TILE / 2 - f.width(nm) / 2, ty + TILE - NAME_H + 3, c.applyAsInt(sel ? Ui.TEXT : Ui.MUTED), true);

            if (sel) {   // значок «выбрано»: кружок с галочкой
                int cx = tx + TILE - 14, cy = ty + 14;
                Ui.circle(g, cx, cy, 6, c.applyAsInt(Ui.ACCENT));
                int[][] tick = {{-3, 0}, {-2, 1}, {-1, 2}, {0, 1}, {1, 0}, {2, -1}, {3, -2}};
                for (int[] p : tick) g.fill(cx + p[0], cy + p[1], cx + p[0] + 1, cy + p[1] + 1, c.applyAsInt(0xFF0B0F16));
            }
        }
        g.disableScissor();

        if (maxScroll() > 0) {
            int trackX = x + w - 3;
            Ui.roundFill(g, trackX, y, 3, h, 1, c.applyAsInt(0x20FFFFFF));
            int thumbH = Math.max(18, h * h / (h + maxScroll()));
            int thumbY = y + (int) ((h - thumbH) * (scroll / (float) maxScroll()));
            Ui.roundGradient(g, trackX, thumbY, 3, thumbH, 1, c.applyAsInt(Ui.ACCENT), c.applyAsInt(Ui.ACCENT2));
        }
    }

    /** Клик по плитке. true — клик обработан. */
    public boolean click(int mx, int my) {
        if (!inArea(mx, my)) return false;
        for (int i = 0; i < count(); i++) {
            if (!Ui.inside(mx, my, tileX(i), tileY(i), TILE, TILE)) continue;
            if (page == Page.KNIVES) {
                if (i == 0) {
                    Knives.selectDefault();
                } else {
                    page = Page.SKINS;
                    scroll = 0;
                }
            } else {
                Knives.selectSkin(i);   // выбор ножа сразу включает и его анимации
            }
            return true;
        }
        return false;
    }
}
