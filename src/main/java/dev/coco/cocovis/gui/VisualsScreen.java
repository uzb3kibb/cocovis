package dev.coco.cocovis.gui;

import dev.coco.cocovis.CocoVisClient;
import dev.coco.cocovis.CocoVisConfig;
import dev.coco.cocovis.module.Module;
import dev.coco.cocovis.module.ModuleManager;
import dev.coco.cocovis.module.Setting;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * CocoVis menu: sidebar + two columns of modules. A module's "..." opens a drawer with its options
 * (so many old functions are now just options of one module). Compact window, animated selection,
 * staggered rows, glow on active toggles.
 */
public class VisualsScreen extends Screen {
    private static final int ROW_H = 28;
    private static final int SET_H = 24;
    private static final int TITLE_H = 24;
    private static final int SECTION_GAP = 12;
    private static final int COL_GAP = 16;
    private static final int NAV_H = 26;
    private static final int NAV_GAP = 3;
    private static final int TOG_W = 26;
    private static final int TOG_H = 13;

    // palette
    private static final int BG = 0xFF09090B;
    private static final int DIVIDER = 0xFF17181C;
    private static final int FIELD = 0xFF0D0E11;
    private static final int FIELD_LINE = 0xFF1D1E23;
    private static final int TEXT = 0xFFE6E8ED;
    private static final int ROW_TEXT = 0xFFC9CCD4;
    private static final int NAV_TEXT = 0xFFB5B8C1;
    private static final int MUTED = 0xFF5C606A;
    private static final int BLUE = 0xFF8DA2FB;
    private static final int NAV_SEL_BG = 0xFF0E0F14;
    private static final int NAV_SEL_LINE = 0xFF2B3040;
    private static final int TOG_OFF = 0xFF22232A;
    private static final int KNOB_OFF = 0xFF4C4F58;
    private static final int TOG_ON = 0xFF6C84F5;
    private static final int ROW_HOT = 0xFF0E0F13;
    private static final int DRAWER = 0xFF0B0C10;

    private static final String VERSION = FabricLoader.getInstance()
            .getModContainer(CocoVisClient.MOD_ID)
            .map(c -> c.getMetadata().getVersion().getFriendlyString()).orElse("?");

    private static final Set<String> COLLAPSED = new HashSet<>();
    private static final Set<String> EXPANDED = new HashSet<>();
    private static int savedCategory;

    private record Nav(String name, int icon, Runnable action) {}

    /** type 0 = section title, 1 = module row, 2 = option row. */
    private record Cell(int type, int x, int y, int w, int h, Module module, Setting setting,
                        String key, String title, int index, int drawerH) {}

    private final List<Nav> manage = List.of(
            new Nav("Позиции HUD", 4, () -> Minecraft.getInstance().gui.setScreen(new HudEditor())),
            new Nav("Сбросить позиции", 5, () -> {
                CocoVisConfig.get().resetPositions();
                CocoVisConfig.save();
            }));

    private int catIndex = savedCategory;
    private int scroll;
    private int mX, mY;
    private boolean searchFocused;
    private String search = "";
    private long lastNs = System.nanoTime();
    private float open;
    private float appear;
    private float navSelY = -1000f;
    private int contentHeight;
    private final Map<Module, Float> toggleAnim = new HashMap<>();
    private final Map<Module, Float> hoverAnim = new HashMap<>();
    private final Map<Module, Float> openAnim = new HashMap<>();
    private final Map<Setting, Float> settingAnim = new HashMap<>();
    private final float[] navHover = new float[8];

    private int x0, y0, w, h, topY, sbW, contentX, contentW, listTop, listBottom;

    public VisualsScreen() {
        super(Component.literal("CocoVis"));
    }

    private static float approach(float cur, float target, float step) {
        return cur < target ? Math.min(target, cur + step) : Math.max(target, cur - step);
    }

    private static float clamp01(float v) {
        return Math.max(0f, Math.min(1f, v));
    }

    private float ease() {
        float t = 1f - open;
        return 1f - t * t * t;
    }

    private void layout(int sw, int sh) {
        w = Math.max(400, Math.min(sw - 70, 620));
        h = Math.max(260, Math.min(sh - 70, 350));
        x0 = (sw - w) / 2;
        y0 = (sh - h) / 2;
        topY = y0 + Math.round((1f - ease()) * 10f);
        sbW = Math.max(118, Math.min(148, w / 4));
        contentX = x0 + sbW + 14;
        contentW = w - sbW - 28;
        listTop = topY + 46;
        listBottom = topY + h - 10;
    }

    private String fit(Font font, String s, int maxW) {
        if (maxW <= 0) {
            return "";
        }
        if (Ui.width(font, s) <= maxW) {
            return s;
        }
        while (!s.isEmpty() && Ui.width(font, s + "…") > maxW) {
            s = s.substring(0, s.length() - 1);
        }
        return s + "…";
    }

    private void scaled(GuiGraphicsExtractor g, Font font, String text, int x, int y, float scale, int color, boolean title) {
        g.pose().pushMatrix();
        g.pose().translate((float) x, (float) y);
        g.pose().scale(scale, scale);
        if (title) {
            Ui.title(g, font, text, 0, 0, color);
        } else {
            Ui.text(g, font, text, 0, 0, color, false);
        }
        g.pose().popMatrix();
    }

    /** Vector-style nav icons built from smooth shapes (12x12 box). */
    private void drawNavIcon(GuiGraphicsExtractor g, int kind, int x, int y, int color) {
        switch (kind) {
            case 0 -> { // person
                Ui.circle(g, x + 3, y, 6, color);
                Ui.roundFill(g, x + 1, y + 7, 10, 5, 3, color);
            }
            case 1 -> Ui.diamond(g, x, y, 12, color);
            case 2 -> { // grid
                Ui.roundFill(g, x, y, 5, 5, 1, color);
                Ui.roundFill(g, x + 7, y, 5, 5, 1, color);
                Ui.roundFill(g, x, y + 7, 5, 5, 1, color);
                Ui.roundFill(g, x + 7, y + 7, 5, 5, 1, color);
            }
            case 3 -> { // eye-like: ring + dot
                Ui.ring(g, x, y, 12, color);
                Ui.circle(g, x + 4, y + 4, 4, color);
            }
            case 4 -> { // move: plus
                Ui.roundFill(g, x + 5, y, 2, 12, 1, color);
                Ui.roundFill(g, x, y + 5, 12, 2, 1, color);
            }
            default -> { // reset: ring with a tip
                Ui.ring(g, x, y + 1, 11, color);
                Ui.triangleRight(g, x + 6, y - 1, 5, 5, color);
            }
        }
    }

    private List<ModuleManager.Section> activeSections() {
        String q = search.trim().toLowerCase(Locale.ROOT);
        if (q.isEmpty()) {
            return ModuleManager.CATEGORIES.get(catIndex).sections();
        }
        List<Module> found = new ArrayList<>();
        for (ModuleManager.Category category : ModuleManager.CATEGORIES) {
            for (Module module : category.modules()) {
                boolean hit = module.name.toLowerCase(Locale.ROOT).contains(q)
                        || module.description().toLowerCase(Locale.ROOT).contains(q);
                for (Setting option : module.options) {
                    hit |= option.name.toLowerCase(Locale.ROOT).contains(q);
                }
                if (hit) {
                    found.add(module);
                }
            }
        }
        int half = (found.size() + 1) / 2;
        List<ModuleManager.Section> result = new ArrayList<>();
        result.add(new ModuleManager.Section("Результаты поиска", new ArrayList<>(found.subList(0, half))));
        if (found.size() > half) {
            result.add(new ModuleManager.Section("", new ArrayList<>(found.subList(half, found.size()))));
        }
        return result;
    }

    private float openOf(Module m) {
        return openAnim.getOrDefault(m, 0f);
    }

    /** Positions of every section title, module row and option row (shared by drawing and clicks). */
    private List<Cell> cells() {
        List<Cell> cells = new ArrayList<>();
        int colW = (contentW - COL_GAP) / 2;
        int[] colY = {0, 0};
        int stagger = 0;
        for (ModuleManager.Section section : activeSections()) {
            int col = colY[1] < colY[0] ? 1 : 0;
            int x = contentX + col * (colW + COL_GAP);
            String key = catIndex + ":" + section.title();
            boolean collapsed = COLLAPSED.contains(key);
            int y = listTop + colY[col] - scroll;
            int used = 0;
            if (!section.title().isEmpty()) {
                cells.add(new Cell(0, x, y, colW, TITLE_H, null, null, key, section.title(), stagger++, 0));
                used += TITLE_H;
            }
            if (!collapsed) {
                for (Module module : section.modules()) {
                    float a = openOf(module);
                    int drawerH = Math.round(module.options.size() * SET_H * a);
                    int idx = stagger++;
                    cells.add(new Cell(1, x, y + used, colW, ROW_H, module, null, key, "", idx, drawerH));
                    if (a > 0.01f) {
                        for (int i = 0; i < module.options.size(); i++) {
                            cells.add(new Cell(2, x, y + used + ROW_H + i * SET_H, colW, SET_H, module,
                                    module.options.get(i), key, "", i, drawerH));
                        }
                    }
                    used += ROW_H + drawerH;
                }
            }
            colY[col] += used + SECTION_GAP;
        }
        contentHeight = Math.max(0, Math.max(colY[0], colY[1]) - SECTION_GAP);
        return cells;
    }

    private int maxScroll() {
        return Math.max(0, contentHeight - Math.max(1, listBottom - listTop));
    }

    private int navTop() {
        return topY + 66;
    }

    private int manageTop() {
        return navTop() + ModuleManager.CATEGORIES.size() * (NAV_H + NAV_GAP) + 24;
    }

    private float stagger(int index) {
        float t = clamp01((appear - index * 0.09f) / 0.35f);
        return t * t * (3f - 2f * t);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        mX = mouseX;
        mY = mouseY;
        Minecraft mc = Minecraft.getInstance();
        Font font = mc.font;
        long now = System.nanoTime();
        float dt = Math.min(0.1f, (now - lastNs) / 1_000_000_000f);
        lastNs = now;
        open = approach(open, 1f, dt * 6f);
        appear = Math.min(12f, appear + dt * 2.4f);
        float e = ease();

        int sw = mc.getWindow().getGuiScaledWidth();
        int sh = mc.getWindow().getGuiScaledHeight();
        layout(sw, sh);

        for (ModuleManager.Category category : ModuleManager.CATEGORIES) {
            for (Module m : category.modules()) {
                openAnim.put(m, approach(openOf(m), EXPANDED.contains(m.name) ? 1f : 0f, dt * 7f));
            }
        }

        g.fill(0, 0, sw, sh, Ui.fade(0xFF000000, 0.55f * e));
        Ui.shadow(g, x0, topY, w, h, 6);
        Ui.roundFill(g, x0, topY, w, h, 6, BG);
        Ui.roundGradient(g, x0 + 1, topY + 1, sbW - 1, 46, 5, Ui.fade(BLUE, 0.11f), Ui.fade(BLUE, 0f));
        Ui.roundOutline(g, x0, topY, w, h, 6, DIVIDER);
        int half = (w - 40) / 2;
        Ui.hBar(g, x0 + 20, topY, half, 1, Ui.fade(BLUE, 0f), Ui.fade(BLUE, 0.9f));
        Ui.hBar(g, x0 + 20 + half, topY, w - 40 - half, 1, Ui.fade(BLUE, 0.9f), Ui.fade(BLUE, 0f));

        // ---------- sidebar ----------
        int logoW = Math.round(Ui.titleWidth(font, "CocoVis") * 1.4f);
        scaled(g, font, "CocoVis", x0 + (sbW - logoW) / 2, topY + 15, 1.4f, BLUE, true);
        g.fill(x0 + 1, topY + 42, x0 + sbW, topY + 43, DIVIDER);
        g.fill(x0 + sbW, topY + 1, x0 + sbW + 1, topY + h - 1, DIVIDER);

        scaled(g, font, "ФУНКЦИИ", x0 + 12, topY + 52, 0.8f, MUTED, false);

        // animated selection plate
        float targetSel = navTop() + catIndex * (NAV_H + NAV_GAP);
        if (navSelY < -500f) {
            navSelY = targetSel;
        }
        navSelY += (targetSel - navSelY) * Math.min(1f, dt * 16f);
        if (search.isEmpty()) {
            int selY = Math.round(navSelY);
            Ui.roundFill(g, x0 + 7, selY, sbW - 14, NAV_H, 7, NAV_SEL_BG);
            Ui.roundOutline(g, x0 + 7, selY, sbW - 14, NAV_H, 7, NAV_SEL_LINE);
            g.fill(x0 + 7, selY + 7, x0 + 9, selY + NAV_H - 7, BLUE);
        }
        for (int i = 0; i < ModuleManager.CATEGORIES.size(); i++) {
            ModuleManager.Category category = ModuleManager.CATEGORIES.get(i);
            drawNav(g, font, category.name(), category.icon(), x0 + 7, navTop() + i * (NAV_H + NAV_GAP),
                    sbW - 14, i == catIndex && search.isEmpty(), i, dt);
        }
        scaled(g, font, "УПРАВЛЕНИЕ", x0 + 12, manageTop() - 13, 0.8f, MUTED, false);
        for (int i = 0; i < manage.size(); i++) {
            Nav nav = manage.get(i);
            drawNav(g, font, nav.name(), nav.icon(), x0 + 7, manageTop() + i * (NAV_H + NAV_GAP),
                    sbW - 14, false, 4 + i, dt);
        }

        // profile footer
        int fy = topY + h - 36;
        g.fill(x0 + 1, fy - 7, x0 + sbW, fy - 6, DIVIDER);
        Ui.roundFill(g, x0 + 11, fy, 22, 22, 11, 0xFF1B1C22);
        Ui.roundOutline(g, x0 + 10, fy - 1, 24, 24, 12, Ui.fade(BLUE, 0.45f));
        Ui.text(g, font, "?", x0 + 19, fy + 7, 0xFF7A7F8A, false);
        String nick = mc.player != null ? mc.player.getName().getString() : "CocoVis";
        Ui.text(g, font, fit(font, nick, sbW - 48), x0 + 40, fy + 2, TEXT, false);
        Ui.text(g, font, "CocoVis " + VERSION, x0 + 40, fy + 12, MUTED, false);

        // ---------- search ----------
        int sy = topY + 9;
        Ui.roundFill(g, contentX, sy, contentW, 24, 6, FIELD);
        Ui.roundOutline(g, contentX, sy, contentW, 24, 6, searchFocused ? 0xFF3A4668 : FIELD_LINE);
        int mc0 = searchFocused ? BLUE : MUTED;
        Ui.roundOutline(g, contentX + 9, sy + 7, 8, 8, 4, mc0);
        Ui.diamond(g, contentX + 15, sy + 14, 5, mc0);
        if (search.isEmpty()) {
            Ui.text(g, font, "Поиск", contentX + 26, sy + 8, MUTED, false);
        } else {
            Ui.text(g, font, fit(font, search, contentW - 42), contentX + 26, sy + 8, TEXT, false);
        }
        if (searchFocused && (System.currentTimeMillis() / 500) % 2 == 0) {
            int cx = contentX + 26 + (search.isEmpty() ? 0 : Ui.width(font, fit(font, search, contentW - 42)));
            g.fill(cx + 1, sy + 6, cx + 2, sy + 18, TEXT);
        }

        // ---------- columns ----------
        cells();
        scroll = Math.max(0, Math.min(scroll, maxScroll()));
        List<Cell> cells = cells();

        int colW = (contentW - COL_GAP) / 2;
        g.enableScissor(contentX - 2, listTop, contentX + contentW + 2, listBottom);
        int dividerX = contentX + colW + COL_GAP / 2;
        g.fill(dividerX, listTop, dividerX + 1, listBottom, DIVIDER);
        Module hovered = null;
        for (Cell cell : cells) {
            if (cell.y() + cell.h() < listTop || cell.y() > listBottom) {
                continue;
            }
            float af = stagger(cell.type() == 2 ? cell.index() : cell.index());
            int slide = Math.round((1f - af) * 5f);
            if (cell.type() == 0) {
                drawTitle(g, font, cell, af, slide);
            } else if (cell.type() == 1) {
                boolean inView = mY >= listTop && mY < listBottom;
                boolean hover = inView && Ui.inside(mX, mY, cell.x(), cell.y(), cell.w(), cell.h());
                if (hover) {
                    hovered = cell.module();
                }
                drawRow(g, font, cell, hover, dt, af, slide);
            } else {
                drawOption(g, font, cell, dt);
            }
        }
        g.disableScissor();

        int max = maxScroll();
        if (max > 0) {
            int trackH = listBottom - listTop;
            int thumbH = Math.max(18, trackH * trackH / (trackH + max));
            int thumbY = listTop + Math.round((trackH - thumbH) * (scroll / (float) max));
            g.fill(contentX + contentW + 7, thumbY, contentX + contentW + 8, thumbY + thumbH, 0xFF3A3D46);
        }

        if (hovered != null) {
            drawTooltip(g, font, hovered.description(), sw);
        }
    }

    private void drawNav(GuiGraphicsExtractor g, Font font, String name, int icon, int x, int y, int w,
                         boolean selected, int hoverIndex, float dt) {
        boolean hover = Ui.inside(mX, mY, x, y, w, NAV_H);
        navHover[hoverIndex] = approach(navHover[hoverIndex], hover ? 1f : 0f, dt * 9f);
        if (!selected && navHover[hoverIndex] > 0.01f) {
            Ui.roundFill(g, x, y, w, NAV_H, 7, Ui.fade(0xFF111218, navHover[hoverIndex]));
        }
        drawNavIcon(g, icon, x + 11, y + 7, selected ? BLUE : NAV_TEXT);
        Ui.text(g, font, fit(font, name, w - 38), x + 29, y + (NAV_H - 8) / 2, selected ? TEXT : NAV_TEXT, false);
    }

    private void drawTitle(GuiGraphicsExtractor g, Font font, Cell cell, float af, int slide) {
        boolean collapsed = COLLAPSED.contains(cell.key());
        int y = cell.y() + slide;
        Ui.title(g, font, cell.title(), cell.x(), y + 7, Ui.fade(BLUE, af));
        if (collapsed) {
            Ui.triangleRight(g, cell.x() + cell.w() - 12, y + 8, 4, 7, MUTED);
        } else {
            Ui.triangleDown(g, cell.x() + cell.w() - 13, y + 10, 7, 4, MUTED);
        }
        g.fill(cell.x(), cell.y() + TITLE_H - 2, cell.x() + cell.w(), cell.y() + TITLE_H - 1, DIVIDER);
    }

    private boolean hasDots(Module m) {
        return !m.options.isEmpty();
    }

    private int dotsX(Cell cell) {
        return cell.x() + cell.w() - TOG_W - 10 - 26;
    }

    private void drawRow(GuiGraphicsExtractor g, Font font, Cell cell, boolean hover, float dt, float af, int slide) {
        Module module = cell.module();
        float hv = approach(hoverAnim.getOrDefault(module, 0f), hover ? 1f : 0f, dt * 9f);
        hoverAnim.put(module, hv);
        boolean enabled = module.isEnabled();
        float oa = openOf(module);
        int x = cell.x();
        int y = cell.y() + slide;
        int rw = cell.w();
        if (hv > 0.01f) {
            Ui.roundFill(g, x, y, rw, ROW_H - 1, 4, Ui.fade(ROW_HOT, hv * af));
        }
        if (oa > 0.01f && cell.drawerH() > 0) {
            Ui.roundFill(g, x + 3, y + ROW_H - 2, rw - 3, cell.drawerH() + 2, 5, Ui.fade(DRAWER, oa));
            g.fill(x + 3, y + ROW_H + 4, x + 4, y + ROW_H + cell.drawerH() - 4, Ui.fade(BLUE, 0.5f * oa));
        }
        g.fill(x, cell.y() + ROW_H - 1, x + rw, cell.y() + ROW_H, Ui.fade(DIVIDER, af));

        int tx = x + rw - TOG_W - 10;
        int ty = y + (ROW_H - TOG_H) / 2;
        int textMax = tx - (hasDots(module) ? 28 : 0) - (x + 10) - 6;
        Ui.text(g, font, fit(font, module.name, textMax), x + 10, y + (ROW_H - 8) / 2,
                Ui.fade(enabled ? TEXT : ROW_TEXT, af), false);

        if (hasDots(module)) {
            boolean dh = Ui.inside(mX, mY, dotsX(cell), cell.y(), 24, ROW_H);
            int dc = oa > 0.5f ? BLUE : dh ? TEXT : MUTED;
            for (int d = 0; d < 3; d++) {
                Ui.circle(g, dotsX(cell) + 2 + d * 6, y + ROW_H / 2 - 1, 3, Ui.fade(dc, af));
            }
        }

        float ta = approach(toggleAnim.getOrDefault(module, enabled ? 1f : 0f), enabled ? 1f : 0f, dt * 11f);
        toggleAnim.put(module, ta);
        if (ta > 0.02f) {
            Ui.roundFill(g, tx - 2, ty - 2, TOG_W + 4, TOG_H + 4, 8, Ui.fade(TOG_ON, 0.20f * ta * af));
        }
        Ui.roundFill(g, tx, ty, TOG_W, TOG_H, 6, Ui.fade(Ui.mix(TOG_OFF, TOG_ON, ta), af));
        int kx = tx + 2 + Math.round(ta * (TOG_W - 13));
        Ui.roundFill(g, kx, ty + 2, 9, 9, 4, Ui.fade(Ui.mix(KNOB_OFF, 0xFFFFFFFF, ta), af));
    }

    private void drawOption(GuiGraphicsExtractor g, Font font, Cell cell, float dt) {
        Setting s = cell.setting();
        float oa = openOf(cell.module());
        float vis = clamp01((cell.drawerH() - cell.index() * SET_H) / (float) SET_H) * oa;
        if (vis < 0.05f) {
            return;
        }
        int x = cell.x();
        int y = cell.y();
        int rw = cell.w();
        boolean hover = mY >= listTop && mY < listBottom && Ui.inside(mX, mY, x, y, rw, SET_H);
        if (hover) {
            Ui.roundFill(g, x + 6, y, rw - 8, SET_H - 1, 4, Ui.fade(0xFF12131A, vis));
        }
        int label = hover ? TEXT : NAV_TEXT;
        Ui.text(g, font, fit(font, s.name, rw - 90), x + 14, y + (SET_H - 8) / 2, Ui.fade(label, vis), false);
        int right = x + rw - 10;
        switch (s.kind) {
            case TOGGLE -> {
                boolean on = s.isOn();
                float a = approach(settingAnim.getOrDefault(s, on ? 1f : 0f), on ? 1f : 0f, dt * 11f);
                settingAnim.put(s, a);
                int sw = 22;
                int sh = 11;
                int sx = right - sw;
                int sy = y + (SET_H - sh) / 2;
                Ui.roundFill(g, sx, sy, sw, sh, 5, Ui.fade(Ui.mix(TOG_OFF, TOG_ON, a), vis));
                Ui.roundFill(g, sx + 2 + Math.round(a * (sw - 11)), sy + 2, 7, 7, 3,
                        Ui.fade(Ui.mix(KNOB_OFF, 0xFFFFFFFF, a), vis));
            }
            case CYCLE -> {
                String v = s.valueText();
                int pw = Ui.width(font, v) + 18;
                int px = right - pw;
                int py = y + (SET_H - 16) / 2;
                Ui.roundFill(g, px, py, pw, 16, 5, Ui.fade(FIELD, vis));
                Ui.roundOutline(g, px, py, pw, 16, 5, Ui.fade(hover ? BLUE : FIELD_LINE, vis));
                Ui.text(g, font, v, px + 9, py + 4, Ui.fade(BLUE, vis), false);
            }
            case ACTION -> Ui.triangleRight(g, right - 5, y + (SET_H - 7) / 2, 4, 7,
                    Ui.fade(hover ? BLUE : MUTED, vis));
        }
    }

    private void drawTooltip(GuiGraphicsExtractor g, Font font, String text, int sw) {
        String t = fit(font, text, 240);
        int tw = Ui.width(font, t) + 14;
        int tx = Math.min(mX + 12, sw - tw - 4);
        int ty = mY + 14;
        Ui.roundFill(g, tx, ty, tw, 18, 5, 0xF0101114);
        Ui.roundOutline(g, tx, ty, tw, 18, 5, 0xFF2A2C34);
        Ui.text(g, font, t, tx + 7, ty + 5, ROW_TEXT, false);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        for (int i = 0; i < ModuleManager.CATEGORIES.size(); i++) {
            if (Ui.inside(mX, mY, x0 + 7, navTop() + i * (NAV_H + NAV_GAP), sbW - 14, NAV_H)) {
                catIndex = i;
                savedCategory = i;
                scroll = 0;
                search = "";
                searchFocused = false;
                appear = 0f;
                return true;
            }
        }
        for (int i = 0; i < manage.size(); i++) {
            if (Ui.inside(mX, mY, x0 + 7, manageTop() + i * (NAV_H + NAV_GAP), sbW - 14, NAV_H)) {
                manage.get(i).action().run();
                return true;
            }
        }
        if (Ui.inside(mX, mY, contentX, topY + 9, contentW, 24)) {
            searchFocused = true;
            return true;
        }
        searchFocused = false;

        if (mY >= listTop && mY < listBottom) {
            for (Cell cell : cells()) {
                if (!Ui.inside(mX, mY, cell.x(), cell.y(), cell.w(), cell.h())) {
                    continue;
                }
                if (cell.type() == 0) {
                    if (!COLLAPSED.remove(cell.key())) {
                        COLLAPSED.add(cell.key());
                    }
                    return true;
                }
                if (cell.type() == 2) {
                    cell.setting().click();
                    return true;
                }
                Module module = cell.module();
                if (hasDots(module) && Ui.inside(mX, mY, dotsX(cell), cell.y(), 24, ROW_H)) {
                    if (!EXPANDED.remove(module.name)) {
                        EXPANDED.add(module.name);
                    }
                    return true;
                }
                module.toggle();
                return true;
            }
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean charTyped(CharacterEvent event) {
        String typed = event.codepointAsString();
        if (searchFocused && event.isAllowedChatCharacter()) {
            search += typed;
            scroll = 0;
            appear = 0f;
            return true;
        }
        return super.charTyped(event);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (event.key() == 256) {
            if (searchFocused) {
                searchFocused = false;
                search = "";
                return true;
            }
            Minecraft.getInstance().gui.setScreen(null);
            return true;
        }
        if (searchFocused && event.key() == 259) {
            if (!search.isEmpty()) {
                search = search.substring(0, search.length() - 1);
            }
            scroll = 0;
            return true;
        }
        if (searchFocused && event.key() == 257) {
            searchFocused = false;
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        scroll = Math.max(0, Math.min(maxScroll(), scroll - (int) Math.round(scrollY * 22)));
        return true;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
