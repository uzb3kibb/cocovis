package dev.coco.cocovis.gui;

import com.mojang.blaze3d.platform.InputConstants;
import dev.coco.cocovis.module.Module;
import net.fabricmc.loader.api.FabricLoader;
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
    private static final int ROW_H = 46, GAP = 8, CAT_H = 26, CAT_GAP = 3;
    private static final int TOG_W = 34, TOG_H = 18, SET_W = 62;
    private static final String VERSION = FabricLoader.getInstance().getModContainer("cocovis")
            .map(c -> c.getMetadata().getVersion().getFriendlyString()).orElse("dev");

    private int catIndex = 0;
    private String query = "";
    private int scroll = 0;
    private long lastNs = System.nanoTime();
    private float open = 0f;
    private int mX, mY;

    private final Map<Module, Float> toggleAnim = new IdentityHashMap<>();
    private final Map<Module, Float> hoverAnim = new IdentityHashMap<>();
    private final Map<Module, ItemStack> moduleIcons = new IdentityHashMap<>();
    private float[] catHover;
    private ItemStack[] catIcons;

    // раскладка (пересчитывается каждый кадр)
    private int x0, y0, w, h, sbX, sbY, sbW, sbH, cX, cY, cW, listTop, listBottom;

    public VisualsScreen() {
        super(Component.literal("CocoVis"));
    }

    @Override
    protected void init() {
        int n = ModuleManager.CATEGORIES.size();
        catIcons = new ItemStack[n];
        catHover = new float[n];
        for (int i = 0; i < n; i++) catIcons[i] = new ItemStack(ModuleManager.CATEGORIES.get(i).icon());
    }

    private float ease() {
        float t = Math.max(0f, Math.min(1f, open));
        return 1f - (1f - t) * (1f - t) * (1f - t);
    }

    private int c(int color) {
        return Ui.fade(color, ease());
    }

    private void layout() {
        w = Math.min(width - 24, 470);
        h = Math.min(height - 24, 292);
        x0 = (width - w) / 2;
        y0 = (height - h) / 2 + Math.round((1f - ease()) * 12);
        sbX = x0 + 8;
        sbY = y0 + 10;
        sbW = 108;
        sbH = h - 18;
        cX = sbX + sbW + 10;
        cY = y0 + 12;
        cW = x0 + w - 10 - cX;
        listTop = cY + 46;
        listBottom = y0 + h - 26;
    }

    private List<Module> visible() {
        if (query.isEmpty()) return ModuleManager.CATEGORIES.get(catIndex).modules();
        List<Module> out = new ArrayList<>();
        String q = query.toLowerCase(Locale.ROOT);
        for (Module m : ModuleManager.all()) {
            if (m.name.toLowerCase(Locale.ROOT).contains(q) || m.description().toLowerCase(Locale.ROOT).contains(q)) out.add(m);
        }
        return out;
    }

    /** Обрезает строку многоточием, чтобы она влезла в maxW. */
    private String fit(String s, int maxW) {
        if (font.width(s) <= maxW) return s;
        while (s.length() > 1 && font.width(s + "…") > maxW) s = s.substring(0, s.length() - 1);
        return s + "…";
    }

    /** Переносит текст по словам не больше чем на maxLines строк; остаток заменяется многоточием. */
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
                if (out.size() == maxLines - 1) break;   // последняя строка: дальше только обрезка
                out.add(cur.toString());
                cur.setLength(0);
                cur.append(words[i]);
            }
        }
        if (cur.length() > 0) {
            if (i < words.length) {                       // остался непоместившийся хвост — склеиваем и режем многоточием
                StringBuilder rest = new StringBuilder(cur);
                for (int k = i; k < words.length; k++) rest.append(' ').append(words[k]);
                out.add(fit(rest.toString(), maxW));
            } else {
                out.add(fit(cur.toString(), maxW));
            }
        }
        return out;
    }

    private static float approach(float cur, float target, float step) {
        return cur + Math.max(-step, Math.min(step, target - cur));
    }

    private void bigText(GuiGraphicsExtractor g, String s, int x, int y, float scale, int color) {
        var pose = g.pose();
        pose.pushMatrix();
        pose.translate((float) x, (float) y);
        pose.scale(scale, scale);
        g.text(font, s, 0, 0, color, true);
        pose.popMatrix();
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

        // ---- окно ----
        Ui.shadow(g, x0, y0, w, h, 12);
        Ui.roundGradient(g, x0, y0, w, h, 12, c(Ui.BG_TOP), c(Ui.BG_BOT));
        Ui.roundOutline(g, x0, y0, w, h, 12, c(Ui.BORDER));
        Ui.hBar(g, x0 + 18, y0 + 1, w - 36, 2, c(Ui.ACCENT), c(Ui.ACCENT2));

        // ---- боковая панель ----
        Ui.roundGradient(g, sbX, sbY, sbW, sbH, 9, c(Ui.PANEL_TOP), c(Ui.PANEL_BOT));
        Ui.roundOutline(g, sbX, sbY, sbW, sbH, 9, c(0x18FFFFFF));
        bigText(g, "Coco", sbX + 12, sbY + 11, 1.5f, c(Ui.TEXT));
        bigText(g, "Vis", sbX + 12 + Math.round(f.width("Coco") * 1.5f), sbY + 11, 1.5f, c(Ui.ACCENT));
        g.text(f, "visuals · v" + VERSION, sbX + 12, sbY + 28, c(Ui.DIM), false);
        Ui.hBar(g, sbX + 12, sbY + 40, 26, 2, c(Ui.ACCENT), c(Ui.ACCENT2));

        for (int i = 0; i < ModuleManager.CATEGORIES.size(); i++) {
            int ry = sbY + 52 + i * (CAT_H + CAT_GAP);
            boolean sel = i == catIndex && query.isEmpty();
            boolean hov = Ui.inside(mX, mY, sbX + 6, ry, sbW - 12, CAT_H);
            catHover[i] = approach(catHover[i], hov ? 1f : 0f, dt * 8f);

            if (sel) {
                Ui.roundGradient(g, sbX + 6, ry, sbW - 12, CAT_H, 7, c(0x665CC8FF), c(0x339B7BFF));
                Ui.roundOutline(g, sbX + 6, ry, sbW - 12, CAT_H, 7, c(0x555CC8FF));
                Ui.roundGradient(g, sbX + 6, ry + 6, 3, CAT_H - 12, 1, c(Ui.ACCENT), c(Ui.ACCENT2));
            } else if (catHover[i] > 0.01f) {
                Ui.roundFill(g, sbX + 6, ry, sbW - 12, CAT_H, 7, c(Ui.mix(0x00FFFFFF, 0x1CFFFFFF, catHover[i])));
            }
            Ui.roundFill(g, sbX + 12, ry + 3, 20, 20, 6, c(0x40000000));
            g.item(catIcons[i], sbX + 14, ry + 5);
            g.text(f, ModuleManager.CATEGORIES.get(i).name(), sbX + 38, ry + 9, c(sel ? Ui.TEXT : Ui.MUTED), false);
        }

        // профиль
        int py0 = sbY + sbH - 38;
        Ui.roundGradient(g, sbX + 6, py0, sbW - 12, 32, 8, c(Ui.CARD_TOP), c(Ui.CARD_BOT));
        Ui.roundOutline(g, sbX + 6, py0, sbW - 12, 32, 8, c(0x18FFFFFF));
        String nick = minecraft != null && minecraft.player != null ? minecraft.player.getName().getString() : "Player";
        String letter = nick.isEmpty() ? "?" : nick.substring(0, 1).toUpperCase(Locale.ROOT);
        Ui.roundGradient(g, sbX + 12, py0 + 7, 18, 18, 9, c(Ui.ACCENT), c(Ui.ACCENT2));
        g.text(f, letter, sbX + 21 - f.width(letter) / 2, py0 + 12, c(0xFF0B0F16), false);
        g.text(f, nick, sbX + 36, py0 + 8, c(Ui.TEXT), false);
        g.text(f, "CocoVis", sbX + 36, py0 + 19, c(Ui.DIM), false);

        // ---- заголовок и поиск ----
        if (query.isEmpty()) {
            var cat = ModuleManager.CATEGORIES.get(catIndex);
            g.text(f, "КАТЕГОРИЯ", cX + 2, cY, c(Ui.DIM), false);
            bigText(g, cat.name(), cX + 2, cY + 11, 1.35f, c(Ui.TEXT));
            g.text(f, cat.subtitle(), cX + 2, cY + 28, c(Ui.MUTED), false);
        } else {
            g.text(f, "ПОИСК", cX + 2, cY, c(Ui.DIM), false);
            bigText(g, "Результаты", cX + 2, cY + 11, 1.35f, c(Ui.TEXT));
            g.text(f, "Найдено: " + visible().size(), cX + 2, cY + 28, c(Ui.MUTED), false);
        }
        int sw = 108, sh = 18, sx = cX + cW - sw, sy = cY + 2;
        Ui.roundGradient(g, sx, sy, sw, sh, 7, c(Ui.CARD_TOP), c(Ui.CARD_BOT));
        Ui.roundOutline(g, sx, sy, sw, sh, 7, c(query.isEmpty() ? 0x20FFFFFF : 0x995CC8FF));
        boolean blink = (System.currentTimeMillis() / 500) % 2 == 0;
        String shown = query.isEmpty() ? "Поиск..." : query + (blink ? "_" : "");
        g.text(f, shown, sx + 8, sy + 5, c(query.isEmpty() ? Ui.DIM : Ui.TEXT), false);
        Ui.hBar(g, cX, cY + 40, cW, 1, c(0x30FFFFFF), c(0x00FFFFFF));

        // ---- список модулей ----
        List<Module> mods = visible();
        int viewH = listBottom - listTop;
        int contentH = mods.size() * (ROW_H + GAP) - GAP;
        int maxScroll = Math.max(0, contentH - viewH);
        scroll = Math.max(0, Math.min(maxScroll, scroll));

        g.enableScissor(cX, listTop, cX + cW, listBottom);
        for (int i = 0; i < mods.size(); i++) {
            Module m = mods.get(i);
            int ry = listTop + i * (ROW_H + GAP) - scroll;
            if (ry + ROW_H < listTop || ry > listBottom) continue;

            int rw = cW - 8;
            boolean hov = Ui.inside(mX, mY, cX, ry, rw, ROW_H) && mY >= listTop && mY < listBottom;
            float ha = approach(hoverAnim.getOrDefault(m, 0f), hov ? 1f : 0f, dt * 8f);
            hoverAnim.put(m, ha);
            float target = m.isEnabled() ? 1f : 0f;
            float a = approach(toggleAnim.getOrDefault(m, target), target, dt * 8f);
            toggleAnim.put(m, a);

            Ui.roundGradient(g, cX, ry, rw, ROW_H, 9,
                    c(Ui.mix(Ui.CARD_TOP, Ui.CARD_HOT_TOP, ha)), c(Ui.mix(Ui.CARD_BOT, Ui.CARD_HOT_BOT, ha)));
            Ui.roundOutline(g, cX, ry, rw, ROW_H, 9,
                    c(Ui.mix(0x1AFFFFFF, 0x555CC8FF, Math.max(a * 0.7f, ha * 0.6f))));
            if (a > 0.01f) {
                Ui.roundGradient(g, cX + 2, ry + 9, 3, ROW_H - 18, 1,
                        c(Ui.mix(0x005CC8FF, Ui.ACCENT, a)), c(Ui.mix(0x009B7BFF, Ui.ACCENT2, a)));
            }

            // иконка модуля
            ItemStack icon = moduleIcons.computeIfAbsent(m, k -> new ItemStack(k.icon));
            Ui.roundGradient(g, cX + 12, ry + 9, 28, 28, 8,
                    c(Ui.mix(0x44000000, 0x445CC8FF, a)), c(Ui.mix(0x33000000, 0x339B7BFF, a)));
            g.item(icon, cX + 18, ry + 15);

            // переключатель
            int px = cX + rw - TOG_W - 12, py = ry + (ROW_H - TOG_H) / 2;
            if (a > 0.01f) {
                Ui.roundFill(g, px - 2, py - 2, TOG_W + 4, TOG_H + 4, 11, c(Ui.mix(0x00000000, 0x305CC8FF, a)));
            }
            Ui.roundGradient(g, px, py, TOG_W, TOG_H, 9,
                    c(Ui.mix(0xFF2A3342, Ui.ACCENT, a)), c(Ui.mix(0xFF222A37, Ui.ACCENT2, a)));
            Ui.roundFill(g, px + 3 + Math.round(a * (TOG_W - TOG_H)), py + 3, TOG_H - 6, TOG_H - 6, (TOG_H - 6) / 2, c(0xFFFFFFFF));

            if (m.settings != null) {
                int bx = px - 10 - SET_W;
                boolean bh = Ui.inside(mX, mY, bx, py, SET_W, TOG_H);
                Ui.roundGradient(g, bx, py, SET_W, TOG_H, 7,
                        c(bh ? Ui.CARD_HOT_TOP : 0xFF263040), c(bh ? Ui.CARD_HOT_BOT : 0xFF1D2531));
                Ui.roundOutline(g, bx, py, SET_W, TOG_H, 7, c(bh ? 0x885CC8FF : 0x30FFFFFF));
                String s = m.settingsLabel;
                g.text(f, s, bx + SET_W / 2 - f.width(s) / 2, py + 5, c(Ui.TEXT), false);
            }

            // название и описание: текст не должен заходить под кнопку и переключатель
            int textX = cX + 50;
            int limit = (m.settings != null ? px - 10 - SET_W : px) - 10;
            int availW = Math.max(40, limit - textX);
            List<String> lines = wrap(m.description(), availW, 2);
            if (lines.size() <= 1) {
                g.text(f, fit(m.name, availW), textX, ry + 11, c(Ui.TEXT), true);
                if (!lines.isEmpty()) g.text(f, lines.get(0), textX, ry + 25, c(Ui.MUTED), false);
            } else {
                g.text(f, fit(m.name, availW), textX, ry + 7, c(Ui.TEXT), true);
                g.text(f, lines.get(0), textX, ry + 19, c(Ui.MUTED), false);
                g.text(f, lines.get(1), textX, ry + 29, c(Ui.MUTED), false);
            }
        }
        g.disableScissor();

        if (mods.isEmpty()) {
            String s = "Ничего не найдено";
            g.text(f, s, cX + cW / 2 - f.width(s) / 2, listTop + 30, c(Ui.MUTED), false);
        }

        // скроллбар
        if (maxScroll > 0) {
            int trackX = cX + cW - 3;
            Ui.roundFill(g, trackX, listTop, 3, viewH, 1, c(0x20FFFFFF));
            int thumbH = Math.max(18, viewH * viewH / (contentH + 1));
            int thumbY = listTop + (int) ((viewH - thumbH) * (scroll / (float) maxScroll));
            Ui.roundGradient(g, trackX, thumbY, 3, thumbH, 1, c(Ui.ACCENT), c(Ui.ACCENT2));
        }

        // подсказка внизу окна
        String hint = "RShift: закрыть   ·   колесо: прокрутка   ·   печатай: поиск";
        g.text(f, hint, cX + 2, y0 + h - 17, c(Ui.DIM), false);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        // Кнопку не проверяем: в 26.3 коды кнопок мыши изменились (SDL).
        // Позицию берём из кадра отрисовки — она точно в GUI-координатах.
        layout();
        double mx = mX, my = mY;

        for (int i = 0; i < ModuleManager.CATEGORIES.size(); i++) {
            int ry = sbY + 52 + i * (CAT_H + CAT_GAP);
            if (Ui.inside(mx, my, sbX + 6, ry, sbW - 12, CAT_H)) {
                catIndex = i;
                query = "";
                scroll = 0;
                return true;
            }
        }

        if (my >= listTop && my < listBottom && mx >= cX && mx < cX + cW) {
            List<Module> mods = visible();
            int rw = cW - 8;
            for (int i = 0; i < mods.size(); i++) {
                Module m = mods.get(i);
                int ry = listTop + i * (ROW_H + GAP) - scroll;
                if (!Ui.inside(mx, my, cX, ry, rw, ROW_H)) continue;
                int px = cX + rw - TOG_W - 12, py = ry + (ROW_H - TOG_H) / 2;
                if (m.settings != null && Ui.inside(mx, my, px - 10 - SET_W, py, SET_W, TOG_H)) {
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
        scroll -= (int) (scrollY * 24);
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
