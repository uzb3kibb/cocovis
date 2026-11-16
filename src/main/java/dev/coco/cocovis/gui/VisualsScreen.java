package dev.coco.cocovis.gui;

import dev.coco.cocovis.CocoVisClient;
import dev.coco.cocovis.CocoVisConfig;
import dev.coco.cocovis.module.Module;
import dev.coco.cocovis.module.ModuleManager;
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
 * CocoVis menu in the style of the "sidebar + two columns of modules" reference:
 * near-black flat surfaces, outlined active nav item, blue section titles,
 * thin row dividers, pill toggles and "..." for module settings.
 */
public class VisualsScreen extends Screen {
    private static final int ROW_H = 32;
    private static final int TITLE_H = 26;
    private static final int SECTION_GAP = 14;
    private static final int COL_GAP = 20;
    private static final int NAV_H = 28;
    private static final int NAV_GAP = 4;
    private static final int TOG_W = 28;
    private static final int TOG_H = 14;

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

    private static final String VERSION = FabricLoader.getInstance()
            .getModContainer(CocoVisClient.MOD_ID)
            .map(c -> c.getMetadata().getVersion().getFriendlyString()).orElse("?");

    private static final Set<String> COLLAPSED = new HashSet<>();
    private static int savedCategory;

    private record Nav(String name, int icon, Runnable action) {}

    private record Cell(int type, int x, int y, int w, int h, Module module, String key, String title) {}

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
    private int contentHeight;
    private final Map<Module, Float> toggleAnim = new HashMap<>();
    private final Map<Module, Float> hoverAnim = new HashMap<>();
    private final float[] navHover = new float[8];

    private int x0, y0, w, h, topY, sbW, contentX, contentW, listTop, listBottom;

    public VisualsScreen() {
        super(Component.literal("CocoVis"));
    }

    private static float approach(float cur, float target, float step) {
        return cur < target ? Math.min(target, cur + step) : Math.max(target, cur - step);
    }

    private float ease() {
        float t = 1f - open;
        return 1f - t * t * t;
    }

    private void layout(int sw, int sh) {
        w = Math.max(440, Math.min(sw - 36, 860));
        h = Math.max(300, Math.min(sh - 36, 500));
        x0 = (sw - w) / 2;
        y0 = (sh - h) / 2;
        topY = y0 + Math.round((1f - ease()) * 12f);
        sbW = Math.max(150, Math.min(190, w / 5));
        contentX = x0 + sbW + 16;
        contentW = w - sbW - 32;
        listTop = topY + 52;
        listBottom = topY + h - 12;
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

    private void bigTitle(GuiGraphicsExtractor g, Font font, String text, int x, int y, float scale, int color) {
        g.pose().pushMatrix();
        g.pose().translate((float) x, (float) y);
        g.pose().scale(scale, scale);
        Ui.title(g, font, text, 0, 0, color);
        g.pose().popMatrix();
    }

    private void bigText(GuiGraphicsExtractor g, Font font, String text, int x, int y, float scale, int color) {
        g.pose().pushMatrix();
        g.pose().translate((float) x, (float) y);
        g.pose().scale(scale, scale);
        Ui.text(g, font, text, 0, 0, color, true);
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
                if (module.name.toLowerCase(Locale.ROOT).contains(q)
                        || module.description().toLowerCase(Locale.ROOT).contains(q)) {
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

    /** Positions of every section title and module row (shared by drawing and clicks). */
    private List<Cell> cells() {
        List<Cell> cells = new ArrayList<>();
        int colW = (contentW - COL_GAP) / 2;
        int[] colY = {0, 0};
        for (ModuleManager.Section section : activeSections()) {
            int col = colY[1] < colY[0] ? 1 : 0;
            int x = contentX + col * (colW + COL_GAP);
            String key = catIndex + ":" + section.title();
            boolean collapsed = COLLAPSED.contains(key);
            int y = listTop + colY[col] - scroll;
            int used = 0;
            if (!section.title().isEmpty()) {
                cells.add(new Cell(0, x, y, colW, TITLE_H, null, key, section.title()));
                used += TITLE_H;
            }
            if (!collapsed) {
                for (Module module : section.modules()) {
                    cells.add(new Cell(1, x, y + used, colW, ROW_H, module, key, ""));
                    used += ROW_H;
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
        return topY + 78;
    }

    private int manageTop() {
        return navTop() + ModuleManager.CATEGORIES.size() * (NAV_H + NAV_GAP) + 26;
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
        float e = ease();

        int sw = mc.getWindow().getGuiScaledWidth();
        int sh = mc.getWindow().getGuiScaledHeight();
        layout(sw, sh);

        g.fill(0, 0, sw, sh, Ui.fade(0xFF000000, 0.6f * e));
        Ui.shadow(g, x0, topY, w, h, 6);
        Ui.roundFill(g, x0, topY, w, h, 6, BG);
        Ui.roundOutline(g, x0, topY, w, h, 6, DIVIDER);

        // ---------- sidebar ----------
        int logoW = Math.round(Ui.titleWidth(font, "CocoVis") * 1.5f);
        bigTitle(g, font, "CocoVis", x0 + (sbW - logoW) / 2, topY + 17, 1.5f, BLUE);
        g.fill(x0 + 1, topY + 50, x0 + sbW, topY + 51, DIVIDER);
        g.fill(x0 + sbW, topY + 1, x0 + sbW + 1, topY + h - 1, DIVIDER);

        bigText(g, font, "ФУНКЦИИ", x0 + 14, topY + 62, 0.8f, MUTED);
        for (int i = 0; i < ModuleManager.CATEGORIES.size(); i++) {
            ModuleManager.Category category = ModuleManager.CATEGORIES.get(i);
            int nx = x0 + 8;
            int ny = navTop() + i * (NAV_H + NAV_GAP);
            int nw = sbW - 16;
            drawNav(g, font, category.name(), i, nx, ny, nw, i == catIndex, i, dt);
        }
        bigText(g, font, "УПРАВЛЕНИЕ", x0 + 14, manageTop() - 14, 0.8f, MUTED);
        for (int i = 0; i < manage.size(); i++) {
            Nav nav = manage.get(i);
            drawNav(g, font, nav.name(), nav.icon(), x0 + 8, manageTop() + i * (NAV_H + NAV_GAP),
                    sbW - 16, false, 4 + i, dt);
        }

        // profile footer
        int fy = topY + h - 40;
        g.fill(x0 + 1, fy - 8, x0 + sbW, fy - 7, DIVIDER);
        Ui.roundFill(g, x0 + 12, fy, 24, 24, 12, 0xFF1B1C22);
        Ui.text(g, font, "?", x0 + 21, fy + 8, 0xFF7A7F8A, true);
        String nick = mc.player != null ? mc.player.getName().getString() : "CocoVis";
        Ui.text(g, font, fit(font, nick, sbW - 52), x0 + 44, fy + 3, TEXT, true);
        Ui.text(g, font, "CocoVis " + VERSION, x0 + 44, fy + 14, MUTED, true);

        // ---------- search ----------
        int sy = topY + 10;
        Ui.roundFill(g, contentX, sy, contentW, 26, 6, FIELD);
        Ui.roundOutline(g, contentX, sy, contentW, 26, 6, searchFocused ? 0xFF3A4668 : FIELD_LINE);
        int mc0 = searchFocused ? BLUE : MUTED;
        Ui.roundOutline(g, contentX + 10, sy + 8, 8, 8, 4, mc0);
        Ui.diamond(g, contentX + 16, sy + 15, 5, mc0);
        if (search.isEmpty()) {
            Ui.text(g, font, "Поиск", contentX + 28, sy + 9, MUTED, true);
        } else {
            Ui.text(g, font, fit(font, search, contentW - 44), contentX + 28, sy + 9, TEXT, true);
        }
        if (searchFocused && (System.currentTimeMillis() / 500) % 2 == 0) {
            int cx = contentX + 28 + (search.isEmpty() ? 0 : Ui.width(font, fit(font, search, contentW - 44)));
            g.fill(cx + 1, sy + 7, cx + 2, sy + 19, TEXT);
        }

        // ---------- columns ----------
        List<Cell> cells = cells();
        scroll = Math.max(0, Math.min(scroll, maxScroll()));
        cells = cells();

        int colW = (contentW - COL_GAP) / 2;
        g.enableScissor(contentX - 2, listTop, contentX + contentW + 2, listBottom);
        int dividerX = contentX + colW + COL_GAP / 2;
        g.fill(dividerX, listTop, dividerX + 1, listBottom, DIVIDER);
        Module hovered = null;
        for (Cell cell : cells) {
            if (cell.y() + cell.h() < listTop || cell.y() > listBottom) {
                continue;
            }
            if (cell.type() == 0) {
                boolean collapsed = COLLAPSED.contains(cell.key());
                Ui.title(g, font, cell.title(), cell.x(), cell.y() + 8, BLUE);
                if (collapsed) {
                    Ui.triangleRight(g, cell.x() + cell.w() - 12, cell.y() + 8, 4, 7, MUTED);
                } else {
                    Ui.triangleDown(g, cell.x() + cell.w() - 13, cell.y() + 10, 7, 4, MUTED);
                }
                g.fill(cell.x(), cell.y() + TITLE_H - 2, cell.x() + cell.w(), cell.y() + TITLE_H - 1, DIVIDER);
            } else {
                boolean inView = mY >= listTop && mY < listBottom;
                boolean hover = inView && Ui.inside(mX, mY, cell.x(), cell.y(), cell.w(), cell.h());
                if (hover) {
                    hovered = cell.module();
                }
                drawRow(g, font, cell, hover, dt);
            }
        }
        g.disableScissor();

        int max = maxScroll();
        if (max > 0) {
            int trackH = listBottom - listTop;
            int thumbH = Math.max(18, trackH * trackH / (trackH + max));
            int thumbY = listTop + Math.round((trackH - thumbH) * (scroll / (float) max));
            g.fill(contentX + contentW + 8, thumbY, contentX + contentW + 9, thumbY + thumbH, 0xFF3A3D46);
        }

        if (hovered != null) {
            drawTooltip(g, font, hovered.description(), sw);
        }
    }

    private void drawNav(GuiGraphicsExtractor g, Font font, String name, int icon, int x, int y, int w,
                         boolean selected, int hoverIndex, float dt) {
        boolean hover = Ui.inside(mX, mY, x, y, w, NAV_H);
        navHover[hoverIndex] = approach(navHover[hoverIndex], hover ? 1f : 0f, dt * 9f);
        if (selected) {
            Ui.roundFill(g, x, y, w, NAV_H, 7, NAV_SEL_BG);
            Ui.roundOutline(g, x, y, w, NAV_H, 7, NAV_SEL_LINE);
        } else if (navHover[hoverIndex] > 0.01f) {
            Ui.roundFill(g, x, y, w, NAV_H, 7, Ui.fade(0xFF111218, navHover[hoverIndex]));
        }
        drawNavIcon(g, icon, x + 11, y + 8, selected ? BLUE : NAV_TEXT);
        Ui.text(g, font, fit(font, name, w - 38), x + 29, y + 10, selected ? TEXT : NAV_TEXT, true);
    }

    private void drawRow(GuiGraphicsExtractor g, Font font, Cell cell, boolean hover, float dt) {
        Module module = cell.module();
        float hv = approach(hoverAnim.getOrDefault(module, 0f), hover ? 1f : 0f, dt * 9f);
        hoverAnim.put(module, hv);
        boolean enabled = module.isEnabled();
        int x = cell.x();
        int y = cell.y();
        int rw = cell.w();
        if (hv > 0.01f) {
            Ui.roundFill(g, x, y, rw, ROW_H - 1, 4, Ui.fade(ROW_HOT, hv));
        }
        g.fill(x, y + ROW_H - 1, x + rw, y + ROW_H, DIVIDER);

        int tx = x + rw - TOG_W - 10;
        int ty = y + (ROW_H - TOG_H) / 2;
        int textMax = tx - (module.settings != null ? 28 : 0) - (x + 10) - 6;
        Ui.text(g, font, fit(font, module.name, textMax), x + 10, y + 12, enabled ? TEXT : ROW_TEXT, true);

        if (module.settings != null) {
            boolean sh = Ui.inside(mX, mY, tx - 26, y, 22, ROW_H);
            int dc = sh ? TEXT : MUTED;
            for (int d = 0; d < 3; d++) {
                Ui.circle(g, tx - 25 + d * 6, y + ROW_H / 2 - 1, 3, dc);
            }
        }

        float ta = approach(toggleAnim.getOrDefault(module, enabled ? 1f : 0f), enabled ? 1f : 0f, dt * 11f);
        toggleAnim.put(module, ta);
        Ui.roundFill(g, tx, ty, TOG_W, TOG_H, 7, Ui.mix(TOG_OFF, TOG_ON, ta));
        int kx = tx + 2 + Math.round(ta * (TOG_W - 14));
        Ui.roundFill(g, kx, ty + 2, 10, 10, 5, Ui.mix(KNOB_OFF, 0xFFFFFFFF, ta));
    }

    private void drawTooltip(GuiGraphicsExtractor g, Font font, String text, int sw) {
        String t = fit(font, text, 240);
        int tw = Ui.width(font, t) + 12;
        int tx = Math.min(mX + 12, sw - tw - 4);
        int ty = mY + 14;
        Ui.roundFill(g, tx, ty, tw, 18, 4, 0xF0101114);
        Ui.roundOutline(g, tx, ty, tw, 18, 4, 0xFF2A2C34);
        Ui.text(g, font, t, tx + 6, ty + 5, ROW_TEXT, true);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        // categories
        for (int i = 0; i < ModuleManager.CATEGORIES.size(); i++) {
            if (Ui.inside(mX, mY, x0 + 8, navTop() + i * (NAV_H + NAV_GAP), sbW - 16, NAV_H)) {
                catIndex = i;
                savedCategory = i;
                scroll = 0;
                search = "";
                searchFocused = false;
                return true;
            }
        }
        // management actions
        for (int i = 0; i < manage.size(); i++) {
            if (Ui.inside(mX, mY, x0 + 8, manageTop() + i * (NAV_H + NAV_GAP), sbW - 16, NAV_H)) {
                manage.get(i).action().run();
                return true;
            }
        }
        // search field
        if (Ui.inside(mX, mY, contentX, topY + 10, contentW, 26)) {
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
                Module module = cell.module();
                int tx = cell.x() + cell.w() - TOG_W - 10;
                if (module.settings != null && Ui.inside(mX, mY, tx - 26, cell.y(), 22, ROW_H)) {
                    module.settings.run();
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
        scroll = Math.max(0, Math.min(maxScroll(), scroll - (int) Math.round(scrollY * 24)));
        return true;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
