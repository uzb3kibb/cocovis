package dev.coco.cocovis.gui;

import com.mojang.blaze3d.platform.InputConstants;
import dev.coco.cocovis.module.Module;
import dev.coco.cocovis.module.ModuleManager;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Меню CocoVis: боковая панель категорий, поиск, карточки модулей с переключателями. */
public class VisualsScreen extends Screen {
    private static final int ACCENT = 0xFF5CC8FF;
    private static final int TEXT = 0xFFFFFFFF;
    private static final int MUTED = 0xFF8E99A6;
    private static final int WINDOW = 0xE00A0D11;
    private static final int PANEL = 0xD0121820;
    private static final int CARD = 0xE0171D26;
    private static final int CARD_HOVER = 0xE01F2733;
    private static final int OFF = 0xFF2A323C;
    private static final int ROW_H = 38, GAP = 6, CAT_H = 22;

    private int catIndex = 0;
    private String query = "";
    private int scroll = 0;
    private long lastNs = System.nanoTime();
    private final Map<Module, Float> anim = new IdentityHashMap<>();
    private ItemStack[] icons;
    private int mX, mY, nClick, lastBtn;

    // раскладка (пересчитывается каждый кадр)
    private int x0, y0, w, h, sbX, sbY, sbW, sbH, cX, cY, cW, listTop, listBottom;

    public VisualsScreen() {
        super(Component.literal("CocoVis"));
    }

    @Override
    protected void init() {
        icons = new ItemStack[ModuleManager.CATEGORIES.size()];
        for (int i = 0; i < icons.length; i++) icons[i] = new ItemStack(ModuleManager.CATEGORIES.get(i).icon());
    }

    private void layout() {
        w = Math.min(width - 24, 440);
        h = Math.min(height - 24, 270);
        x0 = (width - w) / 2;
        y0 = (height - h) / 2;
        sbX = x0 + 8;
        sbY = y0 + 8;
        sbW = 104;
        sbH = h - 16;
        cX = sbX + sbW + 8;
        cY = y0 + 8;
        cW = x0 + w - 8 - cX;
        listTop = cY + 36;
        listBottom = y0 + h - 8;
    }

    private List<Module> visible() {
        if (query.isEmpty()) return ModuleManager.CATEGORIES.get(catIndex).modules();
        List<Module> out = new ArrayList<>();
        String q = query.toLowerCase(Locale.ROOT);
        for (Module m : ModuleManager.all()) {
            if (m.name.toLowerCase(Locale.ROOT).contains(q) || m.description.toLowerCase(Locale.ROOT).contains(q)) out.add(m);
        }
        return out;
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
        layout();
        mX = mouseX;
        mY = mouseY;
        Font f = font;
        long now = System.nanoTime();
        float dt = Math.min(0.1f, (now - lastNs) / 1_000_000_000f);
        lastNs = now;

        g.fill(0, 0, width, height, 0x80000000);
        Ui.roundFill(g, x0, y0, w, h, 10, WINDOW);

        // ---- боковая панель ----
        Ui.roundFill(g, sbX, sbY, sbW, sbH, 8, PANEL);
        var pose = g.pose();
        pose.pushMatrix();
        pose.translate((float) (sbX + 10), (float) (sbY + 9));
        pose.scale(1.4f, 1.4f);
        g.text(f, "CocoVis", 0, 0, TEXT, true);
        pose.popMatrix();
        g.text(f, "visuals 0.1", sbX + 10, sbY + 25, MUTED, false);

        for (int i = 0; i < ModuleManager.CATEGORIES.size(); i++) {
            int ry = sbY + 46 + i * (CAT_H + 2);
            boolean sel = i == catIndex && query.isEmpty();
            boolean hov = Ui.inside(mouseX, mouseY, sbX + 6, ry, sbW - 12, CAT_H);
            if (sel) {
                Ui.roundFill(g, sbX + 6, ry, sbW - 12, CAT_H, 6, Ui.mix(0x00000000, 0x555CC8FF, 1f));
                g.fill(sbX + 6, ry + 5, sbX + 8, ry + CAT_H - 5, ACCENT);
            } else if (hov) {
                Ui.roundFill(g, sbX + 6, ry, sbW - 12, CAT_H, 6, 0x30FFFFFF);
            }
            g.item(icons[i], sbX + 11, ry + 3);
            g.text(f, ModuleManager.CATEGORIES.get(i).name(), sbX + 32, ry + 7, sel ? TEXT : MUTED, false);
        }

        // профиль внизу
        Ui.roundFill(g, sbX + 6, sbY + sbH - 32, sbW - 12, 26, 6, CARD);
        String nick = minecraft != null && minecraft.player != null ? minecraft.player.getName().getString() : "Player";
        g.text(f, nick, sbX + 12, sbY + sbH - 27, TEXT, false);
        g.text(f, "CocoVis", sbX + 12, sbY + sbH - 17, MUTED, false);

        // ---- заголовок и поиск ----
        if (query.isEmpty()) {
            var cat = ModuleManager.CATEGORIES.get(catIndex);
            g.text(f, "Категория: ", cX + 4, cY + 4, MUTED, false);
            g.text(f, cat.name(), cX + 4 + f.width("Категория: "), cY + 4, TEXT, true);
            g.text(f, cat.subtitle(), cX + 4, cY + 16, MUTED, false);
        } else {
            g.text(f, "Поиск: " + query, cX + 4, cY + 4, TEXT, true);
            g.text(f, "Найдено: " + visible().size(), cX + 4, cY + 16, MUTED, false);
        }
        int sw = 96, sh = 16, sx = cX + cW - sw, sy = cY + 4;
        Ui.roundFill(g, sx, sy, sw, sh, 6, CARD);
        boolean blink = (System.currentTimeMillis() / 500) % 2 == 0;
        String shown = query.isEmpty() ? "Поиск..." : query + (blink ? "_" : "");
        g.text(f, shown, sx + 6, sy + 4, query.isEmpty() ? MUTED : TEXT, false);
        g.fill(cX, cY + 30, cX + cW, cY + 31, 0x22FFFFFF);

        // ---- список модулей ----
        List<Module> mods = visible();
        int viewH = listBottom - listTop;
        int maxScroll = Math.max(0, mods.size() * (ROW_H + GAP) - GAP - viewH);
        scroll = Math.max(0, Math.min(maxScroll, scroll));

        g.enableScissor(cX, listTop, cX + cW, listBottom);
        for (int i = 0; i < mods.size(); i++) {
            Module m = mods.get(i);
            int ry = listTop + i * (ROW_H + GAP) - scroll;
            if (ry + ROW_H < listTop || ry > listBottom) continue;

            boolean hov = Ui.inside(mouseX, mouseY, cX, ry, cW - 4, ROW_H)
                    && mouseY >= listTop && mouseY < listBottom;
            Ui.roundFill(g, cX, ry, cW - 4, ROW_H, 8, hov ? CARD_HOVER : CARD);
            g.text(f, m.name, cX + 10, ry + 8, TEXT, true);
            g.text(f, m.description, cX + 10, ry + 21, MUTED, false);

            float target = m.isEnabled() ? 1f : 0f;
            float a = anim.getOrDefault(m, target);
            float step = dt * 8f;
            a += Math.max(-step, Math.min(step, target - a));
            anim.put(m, a);

            int px = cX + cW - 4 - 38, py = ry + 12;
            Ui.roundFill(g, px, py, 28, 14, 7, Ui.mix(OFF, ACCENT, a));
            Ui.roundFill(g, px + 2 + Math.round(a * 14), py + 2, 10, 10, 5, 0xFFFFFFFF);

            if (m.settings != null) {
                int bx = px - 66;
                boolean bh = Ui.inside(mouseX, mouseY, bx, py, 58, 14);
                Ui.roundFill(g, bx, py, 58, 14, 6, bh ? 0xFF2F3B4B : 0xFF232C38);
                String s = "Настроить";
                g.text(f, s, bx + 29 - f.width(s) / 2, py + 3, TEXT, false);
            }
        }
        g.disableScissor();

        g.text(f, "клики " + nClick + ", кнопка " + lastBtn + ", мышь " + mX + "," + mY, x0 + 4, y0 + h + 4, 0xFF555555, false);

        if (mods.isEmpty()) {
            String s = "Ничего не найдено";
            g.text(f, s, cX + cW / 2 - f.width(s) / 2, listTop + 30, MUTED, false);
        }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        // кнопку не проверяем: в 26.3 коды кнопок мыши изменились (SDL).
        // Позицию берём из кадра отрисовки — она точно в GUI-координатах.
        nClick++;
        lastBtn = event.button();
        layout();
        double mx = mX, my = mY;

        for (int i = 0; i < ModuleManager.CATEGORIES.size(); i++) {
            int ry = sbY + 46 + i * (CAT_H + 2);
            if (Ui.inside(mx, my, sbX + 6, ry, sbW - 12, CAT_H)) {
                catIndex = i;
                query = "";
                scroll = 0;
                return true;
            }
        }

        if (my >= listTop && my < listBottom && mx >= cX && mx < cX + cW - 4) {
            List<Module> mods = visible();
            for (int i = 0; i < mods.size(); i++) {
                Module m = mods.get(i);
                int ry = listTop + i * (ROW_H + GAP) - scroll;
                if (!Ui.inside(mx, my, cX, ry, cW - 4, ROW_H)) continue;
                int px = cX + cW - 4 - 38, py = ry + 12;
                if (m.settings != null && Ui.inside(mx, my, px - 66, py, 58, 14)) {
                    m.settings.run();
                } else {
                    m.toggle();
                }
                return true;
            }
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseScrolled(double x, double y, double scrollX, double scrollY) {
        scroll -= (int) (scrollY * 20);
        return true;
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        int key = event.key();
        if (key == InputConstants.KEY_RSHIFT) {
            onClose();
            return true;
        }
        if (key == InputConstants.KEY_ESCAPE) {
            if (!query.isEmpty()) {
                query = "";
                scroll = 0;
                return true;
            }
            return super.keyPressed(event);
        }
        if (key == InputConstants.KEY_BACKSPACE) {
            if (!query.isEmpty()) {
                query = query.substring(0, query.length() - 1);
                scroll = 0;
            }
            return true;
        }
        if (query.length() < 24) {
            if (key >= InputConstants.KEY_A && key <= InputConstants.KEY_Z) {
                query += (char) ('a' + (key - InputConstants.KEY_A));
                scroll = 0;
                return true;
            }
            if (key >= InputConstants.KEY_0 && key <= InputConstants.KEY_9) {
                query += (char) ('0' + (key - InputConstants.KEY_0));
                scroll = 0;
                return true;
            }
            if (key == InputConstants.KEY_SPACE && !query.isEmpty()) {
                query += ' ';
                return true;
            }
        }
        return super.keyPressed(event);
    }

    @Override
    public boolean isPauseScreen() { return false; }
}
