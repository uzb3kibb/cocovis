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

import java.awt.Color;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * CocoVis menu: sidebar + two columns of modules. "..." opens a drawer with the module's options
 * (toggles, cycles, sliders, actions). Everything is positioned in floats and drawn at real-pixel
 * precision, animations use exponential smoothing so they stay fluid at any frame rate.
 */
public class VisualsScreen extends Screen {
    private static final int ROW_H = 28;
    private static final int TITLE_H = 24;
    private static final int SECTION_GAP = 12;
    private static final int COL_GAP = 16;
    private static final int NAV_H = 26;
    private static final int NAV_GAP = 3;
    private static final int TOG_W = 26;
    private static final int TOG_H = 13;

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
    private record Cell(int type, float x, float y, float w, float h, Module module, Setting setting,
                        String key, String title, int index, float drawerH, float offset) {}

    private final List<Nav> manage = List.of(
            new Nav("Позиции HUD", 4, () -> Minecraft.getInstance().gui.setScreen(new HudEditor())),
            new Nav("Сбросить позиции", 5, () -> {
                CocoVisConfig.get().resetPositions();
                CocoVisConfig.save();
            }));

    private int catIndex = savedCategory;
    private float scrollTarget;
    private float scroll;
    private int mX, mY;
    private boolean searchFocused;
    private String search = "";
    private long lastNs = System.nanoTime();
    private float open;
    private float appear;
    private float navSelY = -1000f;
    private float contentHeight;
    private Setting dragging;
    private boolean skyWheelDragging;
    private boolean skyHexFocused;
    private String skyHex = "7B2CFF";
    private float skyWheelX, skyWheelY, skyWheelR;
    private float skyHueBarX, skyHueBarY, skyHueBarW, skyHueBarH;

    private final Map<Module, Float> toggleAnim = new HashMap<>();
    private final Map<Module, Float> hoverAnim = new HashMap<>();
    private final Map<Module, Float> openAnim = new HashMap<>();
    private final Map<Setting, Float> settingAnim = new HashMap<>();
    private final Map<Setting, Float> sliderAnim = new HashMap<>();
    private final float[] navHover = new float[10];

    private int x0, y0, w, h, sbW, contentX, contentW;
    private float topY, listTop, listBottom;

    public VisualsScreen() {
        super(Component.literal("CocoVis"));
        String saved = CocoVisConfig.get().skyColorHex;
        if (saved != null) skyHex = saved.replace("#", "").toUpperCase(Locale.ROOT);
    }

    /** Frame-rate independent exponential smoothing. */
    private static float smooth(float cur, float target, float dt, float speed) {
        float next = cur + (target - cur) * (1f - (float) Math.exp(-dt * speed));
        return Math.abs(target - next) < 0.0015f ? target : next;
    }

    private static float clamp01(float v) {
        return Math.max(0f, Math.min(1f, v));
    }

    private static float easeOut(float t) {
        float u = 1f - clamp01(t);
        return 1f - u * u * u;
    }

    private void layout(int sw, int sh) {
        w = Math.max(400, Math.min(sw - 70, 620));
        h = Math.max(260, Math.min(sh - 70, 350));
        x0 = (sw - w) / 2;
        y0 = (sh - h) / 2;
        topY = y0;
        sbW = Math.max(118, Math.min(148, w / 4));
        contentX = x0 + sbW + 14;
        contentW = w - sbW - 28;
        listTop = topY + 46;
        listBottom = topY + h - 10;
    }

    private String fit(Font font, String s, float maxW) {
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

    private void scaled(GuiGraphicsExtractor g, Font font, String text, float x, float y, float scale, int color, boolean title) {
        g.pose().pushMatrix();
        g.pose().translate(x, y);
        g.pose().scale(scale, scale);
        if (title) {
            Ui.title(g, font, text, 0, 0, color);
        } else {
            Ui.text(g, font, text, 0, 0, color, false);
        }
        g.pose().popMatrix();
    }

    /** Vector-style nav icons built from smooth shapes (12x12 box). */
    private void drawNavIcon(GuiGraphicsExtractor g, int kind, float x, float y, int color) {
        switch (kind) {
            case 0 -> { // person
                Ui.fastCircle(g, x + 3, y, 6, color);
                Ui.fastRoundFill(g, x + 1, y + 7, 10, 5, 3, color);
            }
            case 1 -> Ui.diamond(g, x, y, 12, color);
            case 2 -> { // grid
                Ui.fastRoundFill(g, x, y, 5, 5, 1, color);
                Ui.fastRoundFill(g, x + 7, y, 5, 5, 1, color);
                Ui.fastRoundFill(g, x, y + 7, 5, 5, 1, color);
                Ui.fastRoundFill(g, x + 7, y + 7, 5, 5, 1, color);
            }
            case 3 -> { // eye-like: ring + dot
                Ui.fastRing(g, x, y, 12, color);
                Ui.fastCircle(g, x + 4, y + 4, 4, color);
            }
            case 4 -> { // move: plus
                Ui.fastRoundFill(g, x + 5, y, 2, 12, 1, color);
                Ui.fastRoundFill(g, x, y + 5, 12, 2, 1, color);
            }
            case 6 -> { // sun: disc with rays
                Ui.fastCircle(g, x + 2.5f, y + 2.5f, 7, color);
                Ui.fastRoundFill(g, x + 5.2f, y - 0.5f, 1.6f, 2.4f, 0.8f, color);
                Ui.fastRoundFill(g, x + 5.2f, y + 10.1f, 1.6f, 2.4f, 0.8f, color);
                Ui.fastRoundFill(g, x - 0.5f, y + 5.2f, 2.4f, 1.6f, 0.8f, color);
                Ui.fastRoundFill(g, x + 10.1f, y + 5.2f, 2.4f, 1.6f, 0.8f, color);
            }
            default -> { // reset: ring with a tip
                Ui.fastRing(g, x, y + 1, 11, color);
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

    private static float optionsHeight(Module m) {
        float total = 0f;
        for (Setting s : m.options) {
            total += s.height();
        }
        return total;
    }

    /** Positions of every section title, module row and option row (shared by drawing and clicks). */
    private List<Cell> cells() {
        List<Cell> cells = new ArrayList<>();
        float colW = skyPickerVisible() ? (contentW - COL_GAP) / 2f : (contentW - COL_GAP) / 2f;
        float[] colY = {0f, 0f};
        int stagger = 0;
        for (ModuleManager.Section section : activeSections()) {
            int col = skyPickerVisible() ? 0 : (colY[1] < colY[0] ? 1 : 0);
            float x = contentX + col * (colW + COL_GAP);
            String key = catIndex + ":" + section.title();
            boolean collapsed = COLLAPSED.contains(key);
            float y = listTop + colY[col] - scroll;
            float used = 0f;
            if (!section.title().isEmpty()) {
                cells.add(new Cell(0, x, y, colW, TITLE_H, null, null, key, section.title(), stagger++, 0f, 0f));
                used += TITLE_H;
            }
            if (!collapsed) {
                for (Module module : section.modules()) {
                    float a = openOf(module);
                    float drawerH = optionsHeight(module) * a;
                    int idx = stagger++;
                    cells.add(new Cell(1, x, y + used, colW, ROW_H, module, null, key, "", idx, drawerH, 0f));
                    if (a > 0.01f) {
                        float off = 0f;
                        for (int i = 0; i < module.options.size(); i++) {
                            Setting s = module.options.get(i);
                            cells.add(new Cell(2, x, y + used + ROW_H + off, colW, s.height(), module, s, key, "",
                                    i, drawerH, off));
                            off += s.height();
                        }
                    }
                    used += ROW_H + drawerH;
                }
            }
            colY[col] += used + SECTION_GAP;
        }
        contentHeight = Math.max(0f, Math.max(colY[0], colY[1]) - SECTION_GAP);
        return cells;
    }


    private boolean skyPickerVisible() {
        return catIndex == 2 && search.isEmpty();
    }

    private void applySkyHex() {
        if (skyHex.matches("[0-9A-Fa-f]{6}")) {
            CocoVisConfig cfg = CocoVisConfig.get();
            cfg.skyColorHex = "#" + skyHex.toUpperCase(Locale.ROOT);
            cfg.skyColorMode = 5;
            cfg.skyColorEnabled = true;
        }
    }

    private float[] currentSkyHsv() {
        try {
            int c = Integer.parseInt(skyHex, 16);
            return Color.RGBtoHSB((c >> 16) & 255, (c >> 8) & 255, c & 255, null);
        } catch (RuntimeException ignored) {
            return new float[]{0.75f, 0.8f, 1f};
        }
    }

    // Convert screen mouse coordinates back through the menu's pop-in transform.
    // The picker is drawn inside this transform, so hit testing must use the same space.
    private double menuMouseX() {
        float pop = 0.955f + 0.045f * easeOut(open);
        float center = x0 + w / 2f;
        return center + (mX - center) / pop;
    }

    private double menuMouseY() {
        float pop = 0.955f + 0.045f * easeOut(open);
        float center = y0 + h / 2f;
        return center + (mY - center) / pop;
    }

    private void updateSkyWheel(double mx, double my) {
        float[] hsv = currentSkyHsv();
        // The hue strip changes hue; the circular SV field changes saturation and brightness.
        if (Ui.inside(mx, my, skyHueBarX, skyHueBarY - 3, skyHueBarW, skyHueBarH + 6)) {
            hsv[0] = clamp01((float) ((mx - skyHueBarX) / skyHueBarW));
        } else {
            double dx = mx - skyWheelX, dy = my - skyWheelY;
            if (dx * dx + dy * dy > skyWheelR * skyWheelR) return;
            hsv[1] = clamp01((float) ((dx / (2.0 * skyWheelR)) + 0.5));
            hsv[2] = clamp01((float) (0.5 - dy / (2.0 * skyWheelR)));
        }

        int rgb = Color.HSBtoRGB(hsv[0], hsv[1], hsv[2]) & 0xFFFFFF;
        String next = String.format(Locale.ROOT, "%06X", rgb);
        if (!next.equalsIgnoreCase(skyHex)) {
            skyHex = next;
            applySkyHex();
        }
    }

    private void drawSkyPicker(GuiGraphicsExtractor g, Font font) {
        float left = contentX + (contentW + COL_GAP) / 2f;
        float right = contentX + contentW;
        float panelW = right - left;
        float cx = left + panelW / 2f;
        float cy = listTop + 116f;
        skyWheelX = cx;
        skyWheelY = cy;
        skyWheelR = Math.min(42f, panelW * 0.29f);

        Ui.fastRoundFill(g, left + 5, listTop + 5, panelW - 10, listBottom - listTop - 10, 7, DRAWER);
        Ui.fastRoundOutline(g, left + 5, listTop + 5, panelW - 10, listBottom - listTop - 10, 7, FIELD_LINE);
        Ui.title(g, font, "Цвет неба", left + 14, listTop + 17, TEXT);
        Ui.text(g, font, "Оттенок", left + 14, listTop + 39, MUTED, false);

        float[] hsv = currentSkyHsv();
        skyHueBarX = left + 14;
        skyHueBarY = listTop + 57;
        skyHueBarW = panelW - 28;
        skyHueBarH = 10;
        for (int x = 0; x < Math.max(1, (int) skyHueBarW); x++) {
            float hue = x / Math.max(1f, skyHueBarW - 1f);
            int color = 0xFF000000 | (Color.HSBtoRGB(hue, 1f, 1f) & 0xFFFFFF);
            g.fill((int) skyHueBarX + x, (int) skyHueBarY,
                    (int) skyHueBarX + x + 1, (int) skyHueBarY + (int) skyHueBarH, color);
        }
        float hueMarkerX = skyHueBarX + hsv[0] * skyHueBarW;
        Ui.fastRoundOutline(g, hueMarkerX - 2, skyHueBarY - 2, 4, skyHueBarH + 4, 2, 0xFFFFFFFF);

        // Fast GUI-resolution SV disk. No visible outline: the circle is defined only
        // by its clipped color fill, with one gradient operation per column.
        Ui.fastSvColorDisk(g, cx, cy, skyWheelR, hsv[0]);

        float markerX = cx + (hsv[1] - 0.5f) * 2f * skyWheelR;
        float markerY = cy + (0.5f - hsv[2]) * 2f * skyWheelR;
        Ui.fastCircle(g, markerX - 5, markerY - 5, 10, 0xFF111318);
        Ui.fastRing(g, markerX - 5, markerY - 5, 10, 0xFFFFFFFF);

        float fieldY = cy + skyWheelR + 13;
        float fieldX = left + 14;
        float fieldW = panelW - 28;
        Ui.text(g, font, "HEX", fieldX, fieldY + 9, MUTED, false);
        float inputX = fieldX + 34;
        float inputW = fieldW - 34;
        Ui.fastRoundFill(g, inputX, fieldY, inputW, 24, 5, FIELD);
        Ui.fastRoundOutline(g, inputX, fieldY, inputW, 24, 5, skyHexFocused ? BLUE : FIELD_LINE);
        Ui.text(g, font, "#" + skyHex, inputX + 8, fieldY + 8, TEXT, false);
        Ui.text(g, font, "Цвет применяется сразу", fieldX, fieldY + 34, MUTED, false);
    }

    private boolean skyHexFieldAt(double mx, double my) {
        float left = contentX + (contentW + COL_GAP) / 2f;
        float panelW = contentX + contentW - left;
        float cy = listTop + 116f;
        float r = Math.min(42f, panelW * 0.29f);
        float fieldY = cy + r + 13;
        return Ui.inside(mx, my, left + 48, fieldY, panelW - 62, 24);
    }

    private float maxScroll() {
        return Math.max(0f, contentHeight - Math.max(1f, listBottom - listTop));
    }

    private float navTop() {
        return topY + 66;
    }

    private float manageTop() {
        return navTop() + ModuleManager.CATEGORIES.size() * (NAV_H + NAV_GAP) + 24;
    }

    private float stagger(int index) {
        float t = clamp01((appear - index * 0.09f) / 0.4f);
        return t * t * (3f - 2f * t);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        mX = mouseX;
        mY = mouseY;
        Minecraft mc = Minecraft.getInstance();
        Font font = mc.font;
        long now = System.nanoTime();
        float dt = Math.min(0.05f, (now - lastNs) / 1_000_000_000f);
        lastNs = now;
        open = smooth(open, 1f, dt, 11f);
        appear = Math.min(14f, appear + dt * 2.6f);
        float e = easeOut(open);

        int sw = mc.getWindow().getGuiScaledWidth();
        int sh = mc.getWindow().getGuiScaledHeight();
        layout(sw, sh);

        for (ModuleManager.Category category : ModuleManager.CATEGORIES) {
            for (Module m : category.modules()) {
                openAnim.put(m, smooth(openOf(m), EXPANDED.contains(m.name) ? 1f : 0f, dt, 12f));
            }
        }
        scrollTarget = Math.max(0f, Math.min(scrollTarget, maxScroll()));
        scroll = smooth(scroll, scrollTarget, dt, 16f);

        if (skyWheelDragging) updateSkyWheel(menuMouseX(), menuMouseY());

        g.fill(0, 0, sw, sh, Ui.fade(0xFF000000, 0.55f * e));

        // window pops in from the centre
        float pop = 0.955f + 0.045f * e;
        g.pose().pushMatrix();
        g.pose().translate(x0 + w / 2f, y0 + h / 2f);
        g.pose().scale(pop, pop);
        g.pose().translate(-(x0 + w / 2f), -(y0 + h / 2f));

        Ui.fastShadow(g, x0, topY, w, h, 6);
        Ui.fastRoundFill(g, x0, topY, w, h, 6, BG);
        Ui.fastRoundGradient(g, x0 + 1, topY + 1, sbW - 1, 46, 5, Ui.fade(BLUE, 0.11f), Ui.fade(BLUE, 0f));
        Ui.fastRoundOutline(g, x0, topY, w, h, 6, DIVIDER);
        int half = (w - 40) / 2;
        Ui.hBar(g, x0 + 20, Math.round(topY), half, 1, Ui.fade(BLUE, 0f), Ui.fade(BLUE, 0.9f));
        Ui.hBar(g, x0 + 20 + half, Math.round(topY), w - 40 - half, 1, Ui.fade(BLUE, 0.9f), Ui.fade(BLUE, 0f));

        // ---------- sidebar ----------
        int logoW = Math.round(Ui.titleWidth(font, "CocoVis") * 1.4f);
        scaled(g, font, "CocoVis", x0 + (sbW - logoW) / 2f, topY + 15, 1.4f, BLUE, true);
        Ui.rect(g, x0 + 1, topY + 42, sbW - 1, 1, DIVIDER);
        Ui.rect(g, x0 + sbW, topY + 1, 1, h - 2, DIVIDER);

        scaled(g, font, "ФУНКЦИИ", x0 + 12, topY + 52, 0.8f, MUTED, false);

        float targetSel = navTop() + catIndex * (NAV_H + NAV_GAP);
        if (navSelY < -500f) {
            navSelY = targetSel;
        }
        navSelY = smooth(navSelY, targetSel, dt, 18f);
        if (search.isEmpty()) {
            Ui.fastRoundFill(g, x0 + 7, navSelY, sbW - 14, NAV_H, 7, NAV_SEL_BG);
            Ui.fastRoundOutline(g, x0 + 7, navSelY, sbW - 14, NAV_H, 7, NAV_SEL_LINE);
            Ui.rect(g, x0 + 7, navSelY + 7, 2, NAV_H - 14, BLUE);
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
                    sbW - 14, false, 6 + i, dt);
        }

        float fy = topY + h - 36;
        Ui.rect(g, x0 + 1, fy - 7, sbW - 1, 1, DIVIDER);
        Ui.fastRoundFill(g, x0 + 11, fy, 22, 22, 11, 0xFF1B1C22);
        Ui.fastRoundOutline(g, x0 + 10, fy - 1, 24, 24, 12, Ui.fade(BLUE, 0.45f));
        Ui.text(g, font, "?", x0 + 19, fy + 7, 0xFF7A7F8A, false);
        String nick = mc.player != null ? mc.player.getName().getString() : "CocoVis";
        Ui.text(g, font, fit(font, nick, sbW - 48), x0 + 40, fy + 2, TEXT, false);
        Ui.text(g, font, "CocoVis " + VERSION, x0 + 40, fy + 12, MUTED, false);

        // ---------- search ----------
        float sy = topY + 9;
        Ui.fastRoundFill(g, contentX, sy, contentW, 24, 6, FIELD);
        Ui.fastRoundOutline(g, contentX, sy, contentW, 24, 6, searchFocused ? 0xFF3A4668 : FIELD_LINE);
        int mc0 = searchFocused ? BLUE : MUTED;
        Ui.fastRoundOutline(g, contentX + 9, sy + 7, 8, 8, 4, mc0);
        Ui.diamond(g, contentX + 15, sy + 14, 5, mc0);
        if (search.isEmpty()) {
            Ui.text(g, font, "Поиск", contentX + 26, sy + 8, MUTED, false);
        } else {
            Ui.text(g, font, fit(font, search, contentW - 42), contentX + 26, sy + 8, TEXT, false);
        }
        if (searchFocused && (System.currentTimeMillis() / 500) % 2 == 0) {
            float cx = contentX + 26 + (search.isEmpty() ? 0 : Ui.width(font, fit(font, search, contentW - 42)));
            Ui.rect(g, cx + 1, sy + 6, 1, 12, TEXT);
        }

        // ---------- columns ----------
        List<Cell> cells = cells();
        scrollTarget = Math.max(0f, Math.min(scrollTarget, maxScroll()));
        if (dragging != null) {
            for (Cell c : cells) {
                if (c.type() == 2 && c.setting() == dragging) {
                    dragging.setFraction((mX - (c.x() + 14)) / (c.w() - 28));
                    break;
                }
            }
        }

        float colW = (contentW - COL_GAP) / 2f;
        g.enableScissor(contentX - 2, Math.round(listTop), contentX + contentW + 2, Math.round(listBottom));
        Ui.rect(g, contentX + colW + COL_GAP / 2f, listTop, 1, listBottom - listTop, DIVIDER);
        Module hovered = null;
        for (Cell cell : cells) {
            if (cell.y() + cell.h() < listTop || cell.y() > listBottom) {
                continue;
            }
            float af = stagger(cell.type() == 2 ? 0 : cell.index());
            float slide = (1f - af) * 5f;
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

        if (skyPickerVisible()) drawSkyPicker(g, font);

        float max = maxScroll();
        if (max > 0.5f) {
            float trackH = listBottom - listTop;
            float thumbH = Math.max(18f, trackH * trackH / (trackH + max));
            float thumbY = listTop + (trackH - thumbH) * (scroll / max);
            Ui.fastRoundFill(g, contentX + contentW + 7, thumbY, 2, thumbH, 1, 0xFF3A3D46);
        }

        if (hovered != null) {
            drawTooltip(g, font, hovered.description(), sw);
        }
        g.pose().popMatrix();
    }

    private void drawNav(GuiGraphicsExtractor g, Font font, String name, int icon, float x, float y, float w,
                         boolean selected, int hoverIndex, float dt) {
        boolean hover = Ui.inside(mX, mY, x, y, w, NAV_H);
        navHover[hoverIndex] = smooth(navHover[hoverIndex], hover ? 1f : 0f, dt, 14f);
        if (!selected && navHover[hoverIndex] > 0.01f) {
            Ui.fastRoundFill(g, x, y, w, NAV_H, 7, Ui.fade(0xFF111218, navHover[hoverIndex]));
        }
        drawNavIcon(g, icon, x + 11, y + 7, selected ? BLUE : NAV_TEXT);
        Ui.text(g, font, fit(font, name, w - 38), x + 29, y + (NAV_H - 8) / 2f, selected ? TEXT : NAV_TEXT, false);
    }

    private void drawTitle(GuiGraphicsExtractor g, Font font, Cell cell, float af, float slide) {
        boolean collapsed = COLLAPSED.contains(cell.key());
        float y = cell.y() + slide;
        Ui.title(g, font, cell.title(), cell.x(), y + 7, Ui.fade(BLUE, af));
        if (collapsed) {
            Ui.triangleRight(g, cell.x() + cell.w() - 12, y + 8, 4, 7, MUTED);
        } else {
            Ui.triangleDown(g, cell.x() + cell.w() - 13, y + 10, 7, 4, MUTED);
        }
        Ui.rect(g, cell.x(), cell.y() + TITLE_H - 2, cell.w(), 1, DIVIDER);
    }

    private boolean hasDots(Module m) {
        return !m.options.isEmpty();
    }

    private float dotsX(Cell cell) {
        return cell.x() + cell.w() - TOG_W - 10 - 26;
    }

    private void drawRow(GuiGraphicsExtractor g, Font font, Cell cell, boolean hover, float dt, float af, float slide) {
        Module module = cell.module();
        float hv = smooth(hoverAnim.getOrDefault(module, 0f), hover ? 1f : 0f, dt, 14f);
        hoverAnim.put(module, hv);
        boolean enabled = module.isEnabled();
        float oa = openOf(module);
        float x = cell.x();
        float y = cell.y() + slide;
        float rw = cell.w();
        if (hv > 0.01f) {
            Ui.fastRoundFill(g, x, y, rw, ROW_H - 1, 4, Ui.fade(ROW_HOT, hv * af));
        }
        if (oa > 0.005f && cell.drawerH() > 0.5f) {
            Ui.fastRoundFill(g, x + 3, y + ROW_H - 2, rw - 3, cell.drawerH() + 2, 5, Ui.fade(DRAWER, Math.min(1f, oa * 1.6f)));
            if (cell.drawerH() > 10f) {
                Ui.rect(g, x + 3, y + ROW_H + 4, 1, cell.drawerH() - 8, Ui.fade(BLUE, 0.5f * oa));
            }
        }
        Ui.rect(g, x, cell.y() + ROW_H - 1, rw, 1, Ui.fade(DIVIDER, af));

        float tx = x + rw - TOG_W - 10;
        float ty = y + (ROW_H - TOG_H) / 2f;
        float textMax = tx - (hasDots(module) ? 28 : 0) - (x + 10) - 6;
        Ui.text(g, font, fit(font, module.name, textMax), x + 10, y + (ROW_H - 8) / 2f,
                Ui.fade(enabled ? TEXT : ROW_TEXT, af), false);

        if (hasDots(module)) {
            boolean dh = Ui.inside(mX, mY, dotsX(cell), cell.y(), 24, ROW_H);
            int dc = oa > 0.5f ? BLUE : dh ? TEXT : MUTED;
            for (int d = 0; d < 3; d++) {
                Ui.fastCircle(g, dotsX(cell) + 2 + d * 6, y + ROW_H / 2f - 1, 3, Ui.fade(dc, af));
            }
        }

        float ta = smooth(toggleAnim.getOrDefault(module, enabled ? 1f : 0f), enabled ? 1f : 0f, dt, 16f);
        toggleAnim.put(module, ta);
        if (ta > 0.02f) {
            Ui.fastRoundFill(g, tx - 2, ty - 2, TOG_W + 4, TOG_H + 4, 8, Ui.fade(TOG_ON, 0.20f * ta * af));
        }
        Ui.fastRoundFill(g, tx, ty, TOG_W, TOG_H, 6, Ui.fade(Ui.mix(TOG_OFF, TOG_ON, ta), af));
        float kx = tx + 2 + ta * (TOG_W - 13);
        Ui.fastRoundFill(g, kx, ty + 2, 9, 9, 4, Ui.fade(Ui.mix(KNOB_OFF, 0xFFFFFFFF, ta), af));
    }

    private void drawOption(GuiGraphicsExtractor g, Font font, Cell cell, float dt) {
        Setting s = cell.setting();
        float oa = openOf(cell.module());
        float vis = clamp01((cell.drawerH() - cell.offset()) / Math.max(1f, cell.h())) * oa;
        if (vis < 0.04f) {
            return;
        }
        float x = cell.x();
        float y = cell.y();
        float rw = cell.w();
        float rh = cell.h();
        boolean hover = mY >= listTop && mY < listBottom && Ui.inside(mX, mY, x, y, rw, rh);
        if (hover && s.kind != Setting.Kind.SLIDER) {
            Ui.fastRoundFill(g, x + 6, y, rw - 8, rh - 1, 4, Ui.fade(0xFF12131A, vis));
        }
        int label = hover ? TEXT : NAV_TEXT;
        float right = x + rw - 10;
        float labelY = s.kind == Setting.Kind.SLIDER ? y + 5 : y + (rh - 8) / 2f;
        Ui.text(g, font, fit(font, s.name, rw - 90), x + 14, labelY, Ui.fade(label, vis), false);
        switch (s.kind) {
            case TOGGLE -> {
                boolean on = s.isOn();
                float a = smooth(settingAnim.getOrDefault(s, on ? 1f : 0f), on ? 1f : 0f, dt, 16f);
                settingAnim.put(s, a);
                float sx = right - 22;
                float sy = y + (rh - 11) / 2f;
                Ui.fastRoundFill(g, sx, sy, 22, 11, 5, Ui.fade(Ui.mix(TOG_OFF, TOG_ON, a), vis));
                Ui.fastRoundFill(g, sx + 2 + a * 11, sy + 2, 7, 7, 3, Ui.fade(Ui.mix(KNOB_OFF, 0xFFFFFFFF, a), vis));
            }
            case CYCLE -> {
                String v = s.valueText();
                float pw = Ui.width(font, v) + 18;
                float px = right - pw;
                float py = y + (rh - 16) / 2f;
                Ui.fastRoundFill(g, px, py, pw, 16, 5, Ui.fade(FIELD, vis));
                Ui.fastRoundOutline(g, px, py, pw, 16, 5, Ui.fade(hover ? BLUE : FIELD_LINE, vis));
                Ui.text(g, font, v, px + 9, py + 4, Ui.fade(BLUE, vis), false);
            }
            case SLIDER -> {
                float f = smooth(sliderAnim.getOrDefault(s, s.fraction()), s.fraction(), dt, dragging == s ? 40f : 20f);
                sliderAnim.put(s, f);
                String v = s.valueText();
                Ui.text(g, font, v, right - Ui.width(font, v), labelY, Ui.fade(TEXT, vis), false);
                float tx = x + 14;
                float tw = rw - 28;
                float ty = y + 22;
                Ui.fastRoundFill(g, tx, ty, tw, 4, 2, Ui.fade(0xFF22232A, vis));
                if (f > 0.001f) {
                    Ui.fastRoundFill(g, tx, ty, Math.max(4f, tw * f), 4, 2, Ui.fade(Ui.mix(TOG_ON, BLUE, f), vis));
                }
                float glow = dragging == s ? 0.35f : hover ? 0.2f : 0f;
                if (glow > 0f) {
                    Ui.fastCircle(g, tx + tw * f - 8, ty + 2 - 8, 16, Ui.fade(BLUE, glow * vis));
                }
                Ui.fastCircle(g, tx + tw * f - 5, ty + 2 - 5, 10, Ui.fade(0xFFFFFFFF, vis));
            }
            case ACTION -> Ui.triangleRight(g, right - 5, y + (rh - 7) / 2f, 4, 7,
                    Ui.fade(hover ? BLUE : MUTED, vis));
        }
    }

    private void drawTooltip(GuiGraphicsExtractor g, Font font, String text, int sw) {
        String t = fit(font, text, 240);
        float tw = Ui.width(font, t) + 14;
        float tx = Math.min(mX + 12, sw - tw - 4);
        float ty = mY + 14;
        Ui.fastRoundFill(g, tx, ty, tw, 18, 5, 0xF0101114);
        Ui.fastRoundOutline(g, tx, ty, tw, 18, 5, 0xFF2A2C34);
        Ui.text(g, font, t, tx + 7, ty + 5, ROW_TEXT, false);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (skyPickerVisible()) {
            double wheelMouseX = menuMouseX(), wheelMouseY = menuMouseY();
            float dx = (float) wheelMouseX - skyWheelX, dy = (float) wheelMouseY - skyWheelY;
            double distance = Math.sqrt(dx * dx + dy * dy);
            boolean onDisk = distance <= skyWheelR;
            boolean onHueBar = Ui.inside(wheelMouseX, wheelMouseY, skyHueBarX, skyHueBarY - 3,
                    skyHueBarW, skyHueBarH + 6);
            if (onDisk || onHueBar) {
                skyWheelDragging = true;
                skyHexFocused = false;
                updateSkyWheel(wheelMouseX, wheelMouseY);
                return true;
            }
            if (skyHexFieldAt(wheelMouseX, wheelMouseY)) {
                if (!skyHexFocused) skyHex = "";
                skyHexFocused = true;
                return true;
            }
            skyHexFocused = false;
        }
        for (int i = 0; i < ModuleManager.CATEGORIES.size(); i++) {
            if (Ui.inside(mX, mY, x0 + 7, navTop() + i * (NAV_H + NAV_GAP), sbW - 14, NAV_H)) {
                catIndex = i;
                savedCategory = i;
                scrollTarget = 0f;
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
                    Setting s = cell.setting();
                    if (s.kind == Setting.Kind.SLIDER) {
                        dragging = s;
                        s.setFraction((mX - (cell.x() + 14)) / (cell.w() - 28));
                    } else {
                        s.click();
                    }
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
    public boolean mouseReleased(MouseButtonEvent event) {
        if (skyWheelDragging) { skyWheelDragging = false; CocoVisConfig.save(); return true; }
        if (dragging != null) {
            dragging = null;
            return true;
        }
        return super.mouseReleased(event);
    }

    @Override
    public boolean charTyped(CharacterEvent event) {
        // HEX is a focused text field: don't rely on the chat-character filter here.
        // That filter can reject valid key input in this screen on some 26.x builds.
        if (skyHexFocused) {
            String typedHex = event.codepointAsString().toUpperCase(Locale.ROOT);
            if (typedHex.matches("[0-9A-F]") && skyHex.length() < 6) {
                skyHex += typedHex;
                applySkyHex();
                if (skyHex.length() == 6) CocoVisConfig.save();
            }
            return true;
        }
        String typed = event.codepointAsString();
        if (searchFocused && event.isAllowedChatCharacter()) {
            search += typed;
            scrollTarget = 0f;
            appear = 0f;
            return true;
        }
        return super.charTyped(event);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (skyHexFocused && event.key() == 259 && !skyHex.isEmpty()) {
            skyHex = skyHex.substring(0, skyHex.length() - 1);
            applySkyHex();
            if (skyHex.length() == 6) CocoVisConfig.save();
            return true;
        }
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
            scrollTarget = 0f;
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
        scrollTarget = Math.max(0f, Math.min(maxScroll(), scrollTarget - (float) (scrollY * 26)));
        return true;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
