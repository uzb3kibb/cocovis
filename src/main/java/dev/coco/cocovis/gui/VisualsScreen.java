package dev.coco.cocovis.gui;

import dev.coco.cocovis.CocoVisClient;
import dev.coco.cocovis.module.Module;
import dev.coco.cocovis.module.ModuleManager;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * CocoVis Control Panel — clean graphite UI inspired by modern client dashboards.
 * Sidebar navigation, search, two-column module grid and compact toggles.
 */
public class VisualsScreen extends Screen {
    private static final int SIDEBAR_W = 164;
    private static final int ROW_H = 46;
    private static final int COL_GAP = 12;
    private static final int ROW_GAP = 7;
    private static final int TOG_W = 31;
    private static final int TOG_H = 15;
    private static final String VERSION = FabricLoader.getInstance()
            .getModContainer(CocoVisClient.MOD_ID)
            .map(c -> c.getMetadata().getVersion().getFriendlyString()).orElse("?");

    private static int savedCategory;
    private int catIndex = savedCategory;
    private int scroll;
    private int mX, mY;
    private int x0, y0, w, h, contentX, contentW, listTop, listBottom, topY;
    private int colW;
    private boolean searchFocused;
    private String search = "";
    private long lastNs = System.nanoTime();
    private float open;
    private final Map<Module, Float> toggleAnim = new HashMap<>();
    private final Map<Module, Float> hoverAnim = new HashMap<>();
    private final Map<Item, ItemStack> icons = new HashMap<>();
    private final float[] catHover = new float[ModuleManager.CATEGORIES.size()];

    private static final int BG = 0xF20A0C10;
    private static final int PANEL = 0xFF11141A;
    private static final int PANEL_ALT = 0xFF151922;
    private static final int CARD = 0xFF141820;
    private static final int CARD_HOVER = 0xFF1B222D;
    private static final int LINE = 0xFF252D38;
    private static final int TEXT = 0xFFE8EDF5;
    private static final int MUTED = 0xFF8994A4;
    private static final int DIM = 0xFF596575;
    private static final int BLUE = 0xFF7CA8FF;
    private static final int GREEN = 0xFF55D6A0;

    public VisualsScreen() {
        super(Component.literal("CocoVis"));
    }

    private ItemStack icon(Item item) {
        return icons.computeIfAbsent(item, ItemStack::new);
    }

    private static float approach(float cur, float target, float step) {
        return cur < target ? Math.min(target, cur + step) : Math.max(target, cur - step);
    }

    private float ease() {
        float t = 1f - open;
        return 1f - t * t * t;
    }

    private void layout(int sw, int sh) {
        w = Math.max(420, Math.min(sw - 28, 940));
        h = Math.max(280, Math.min(sh - 28, 620));
        x0 = (sw - w) / 2;
        y0 = (sh - h) / 2;
    }

    private List<Module> filteredModules() {
        String q = search.trim().toLowerCase(Locale.ROOT);
        List<Module> result = new ArrayList<>();
        for (Module module : ModuleManager.CATEGORIES.get(catIndex).modules()) {
            if (q.isEmpty() || module.name.toLowerCase(Locale.ROOT).contains(q)
                    || module.description().toLowerCase(Locale.ROOT).contains(q)) {
                result.add(module);
            }
        }
        return result;
    }

    private String fit(Font font, String s, int maxW) {
        if (maxW <= 0) return "";
        if (font.width(s) <= maxW) return s;
        String suffix = "…";
        while (!s.isEmpty() && font.width(s + suffix) > maxW) s = s.substring(0, s.length() - 1);
        return s + suffix;
    }

    private int enabledCount(List<Module> list) {
        int count = 0;
        for (Module module : list) if (module.isEnabled()) count++;
        return count;
    }

    private int totalEnabled() {
        int count = 0;
        for (ModuleManager.Category category : ModuleManager.CATEGORIES)
            count += enabledCount(category.modules());
        return count;
    }

    private int totalModules() {
        int count = 0;
        for (ModuleManager.Category category : ModuleManager.CATEGORIES)
            count += category.modules().size();
        return count;
    }

    private int maxScroll() {
        int rows = (filteredModules().size() + 1) / 2;
        int content = rows * (ROW_H + ROW_GAP) - ROW_GAP;
        return Math.max(0, content - Math.max(1, listBottom - listTop));
    }

    private void drawModule(GuiGraphicsExtractor g, Font font, Module module, int x, int y,
                            float dt, int width) {
        boolean hover = Ui.inside(mX, mY, x, y, width, ROW_H);
        float hv = approach(hoverAnim.getOrDefault(module, 0f), hover ? 1f : 0f, dt * 9f);
        hoverAnim.put(module, hv);
        boolean enabled = module.isEnabled();

        Ui.roundFill(g, x, y, width, ROW_H, 5, Ui.mix(CARD, CARD_HOVER, hv));
        g.fill(x, y + ROW_H - 1, x + width, y + ROW_H, LINE);
        if (enabled) g.fill(x, y + 8, x + 2, y + ROW_H - 8, BLUE);

        g.item(icon(module.icon), x + 9, y + 14);
        int tx = x + width - TOG_W - 11;
        int ty = y + (ROW_H - TOG_H) / 2;
        int settingsLeft = tx - (module.settings != null ? 27 : 0);
        int textX = x + 34;
        int textMax = settingsLeft - textX - 8;

        g.text(font, fit(font, module.name, textMax), textX, y + 8,
                enabled ? TEXT : 0xFFB7C0CC, true);
        g.text(font, fit(font, module.description(), textMax), textX, y + 25,
                MUTED, true);

        if (module.settings != null) {
            int sx = tx - 25;
            boolean sh = Ui.inside(mX, mY, sx, y + 10, 19, 25);
            g.text(font, "···", sx + 2, y + 15, sh ? BLUE : MUTED, true);
        }

        float ta = approach(toggleAnim.getOrDefault(module, enabled ? 1f : 0f),
                enabled ? 1f : 0f, dt * 11f);
        toggleAnim.put(module, ta);
        Ui.roundFill(g, tx, ty, TOG_W, TOG_H, 8, Ui.mix(0xFF303844, BLUE, ta));
        int knobX = tx + 2 + Math.round(ta * (TOG_W - 13));
        Ui.roundFill(g, knobX, ty + 2, 11, 11, 6, 0xFFF5F7FB);
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
        topY = y0 + Math.round((1f - e) * 12f);

        g.fill(0, 0, sw, sh, Ui.fade(0xFF05070A, 0.82f * e));
        Ui.shadow(g, x0, topY, w, h, 8);
        Ui.roundFill(g, x0, topY, w, h, 8, PANEL);
        Ui.roundOutline(g, x0, topY, w, h, 8, LINE);

        // Left navigation rail
        Ui.roundFill(g, x0 + 1, topY + 1, SIDEBAR_W, h - 2, 7, 0xFF10141A);
        g.fill(x0 + SIDEBAR_W, topY + 12, x0 + SIDEBAR_W + 1, topY + h - 12, LINE);

        // Brand
        Ui.roundFill(g, x0 + 12, topY + 13, 34, 34, 8, 0xFF1D2B40);
        scaledLogo(g, font, x0 + 20, topY + 21);
        g.text(font, "CocoVis", x0 + 55, topY + 18, TEXT, true);
        g.text(font, "CLIENT  " + VERSION, x0 + 55, topY + 33, DIM, true);

        g.text(font, "FEATURES", x0 + 16, topY + 68, DIM, true);
        int navY = topY + 83;
        for (int i = 0; i < ModuleManager.CATEGORIES.size(); i++) {
            ModuleManager.Category category = ModuleManager.CATEGORIES.get(i);
            int nx = x0 + 9;
            int ny = navY + i * 39;
            int nw = SIDEBAR_W - 18;
            boolean hover = Ui.inside(mX, mY, nx, ny, nw, 33);
            catHover[i] = approach(catHover[i], hover ? 1f : 0f, dt * 9f);
            boolean selected = i == catIndex;
            if (selected) {
                Ui.roundFill(g, nx, ny, nw, 33, 5, 0xFF1C2634);
                g.fill(nx, ny + 7, nx + 2, ny + 26, BLUE);
            } else if (catHover[i] > 0.01f) {
                Ui.roundFill(g, nx, ny, nw, 33, 5, Ui.fade(0xFF26303D, catHover[i] * 0.8f));
            }
            g.item(icon(category.icon()), nx + 9, ny + 8);
            g.text(font, category.name(), nx + 32, ny + 10,
                    selected ? 0xFFBBD0F8 : MUTED, true);
            String count = String.valueOf(category.modules().size());
            g.text(font, count, nx + nw - font.width(count) - 9, ny + 10,
                    selected ? BLUE : DIM, true);
        }

        // Footer status
        int footerY = topY + h - 61;
        g.fill(x0 + 12, footerY - 10, x0 + SIDEBAR_W - 12, footerY - 10 + 1, LINE);
        Ui.roundFill(g, x0 + 13, footerY + 1, 7, 7, 4, GREEN);
        g.text(font, totalEnabled() + " active", x0 + 27, footerY, TEXT, true);
        g.text(font, totalModules() + " modules installed", x0 + 13, footerY + 17, DIM, true);

        // Main workspace
        contentX = x0 + SIDEBAR_W + 22;
        contentW = w - SIDEBAR_W - 38;
        int headerY = topY + 17;
        ModuleManager.Category category = ModuleManager.CATEGORIES.get(catIndex);
        g.text(font, category.name(), contentX, headerY, TEXT, true);
        g.text(font, category.subtitle(), contentX, headerY + 17, MUTED, true);

        // Search field
        int searchW = Math.min(210, Math.max(130, contentW / 3));
        int searchX = x0 + w - searchW - 15;
        int searchY = topY + 13;
        Ui.roundFill(g, searchX, searchY, searchW, 27, 5,
                searchFocused ? 0xFF1C2634 : PANEL_ALT);
        Ui.roundOutline(g, searchX, searchY, searchW, 27, 5,
                searchFocused ? 0xFF547DBB : LINE);
        g.text(font, "⌕", searchX + 8, searchY + 7, searchFocused ? BLUE : MUTED, true);
        String searchText = search.isEmpty() ? "Search modules..." : search;
        g.text(font, fit(font, searchText, searchW - 34), searchX + 25, searchY + 8,
                search.isEmpty() ? DIM : TEXT, true);

        String active = enabledCount(category.modules()) + " ENABLED";
        int chipW = font.width(active) + 21;
        int chipX = x0 + w - chipW - 17;
        Ui.roundFill(g, chipX, topY + 49, chipW, 20, 10, 0xFF17251F);
        Ui.roundFill(g, chipX + 7, topY + 56, 5, 5, 3, GREEN);
        g.text(font, active, chipX + 17, topY + 55, 0xFFB4D9C8, true);

        // Header divider and two-column module grid
        int dividerY = topY + 78;
        g.fill(contentX, dividerY, contentX + contentW, dividerY + 1, LINE);
        Ui.hBar(g, contentX, dividerY, Math.min(70, contentW), 2, BLUE, BLUE);
        listTop = topY + 91;
        listBottom = topY + h - 24;
        colW = (contentW - COL_GAP) / 2;
        List<Module> mods = filteredModules();
        int rows = (mods.size() + 1) / 2;
        int contentHeight = rows * (ROW_H + ROW_GAP) - ROW_GAP;
        int max = Math.max(0, contentHeight - (listBottom - listTop));
        scroll = Math.max(0, Math.min(scroll, max));

        String section = search.isEmpty() ? "MODULES" : "SEARCH RESULTS";
        g.text(font, section, contentX + 1, topY + 83, DIM, true);
        String amount = mods.size() + " ITEMS";
        g.text(font, amount, contentX + contentW - font.width(amount), topY + 83, DIM, true);

        g.enableScissor(contentX, listTop, contentX + contentW, listBottom);
        for (int i = 0; i < mods.size(); i++) {
            int col = i % 2;
            int row = i / 2;
            int cx = contentX + col * (colW + COL_GAP);
            int cy = listTop + row * (ROW_H + ROW_GAP) - scroll;
            if (cy + ROW_H < listTop || cy > listBottom) continue;
            drawModule(g, font, mods.get(i), cx, cy, dt, colW);
        }
        g.disableScissor();

        if (max > 0) {
            int trackH = listBottom - listTop;
            int thumbH = Math.max(18, trackH * trackH / (trackH + max));
            int thumbY = listTop + Math.round((trackH - thumbH) * (scroll / (float) max));
            g.fill(contentX + contentW + 4, listTop, contentX + contentW + 5, listBottom, LINE);
            g.fill(contentX + contentW + 4, thumbY, contentX + contentW + 5, thumbY + thumbH, BLUE);
        }

        String footer = searchFocused ? "TYPE TO FILTER  ·  ENTER TO FINISH  ·  ESC TO CLOSE"
                : "ESC  CLOSE     ·     SCROLL  BROWSE";
        g.text(font, footer, contentX, topY + h - 13, DIM, true);
    }

    private void scaledLogo(GuiGraphicsExtractor g, Font font, int x, int y) {
        g.fill(x, y, x + 18, y + 2, BLUE);
        g.fill(x + 4, y + 5, x + 22, y + 7, BLUE);
        g.fill(x + 8, y + 10, x + 26, y + 12, BLUE);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        int navY = topY + 83;
        for (int i = 0; i < ModuleManager.CATEGORIES.size(); i++) {
            int ny = navY + i * 39;
            if (Ui.inside(mX, mY, x0 + 9, ny, SIDEBAR_W - 18, 33)) {
                catIndex = i;
                savedCategory = i;
                scroll = 0;
                search = "";
                searchFocused = false;
                return true;
            }
        }

        int searchW = Math.min(210, Math.max(130, contentW / 3));
        int searchX = x0 + w - searchW - 15;
        if (Ui.inside(mX, mY, searchX, topY + 13, searchW, 27)) {
            searchFocused = true;
            return true;
        }
        searchFocused = false;

        if (mY >= listTop && mY < listBottom) {
            List<Module> mods = filteredModules();
            for (int i = 0; i < mods.size(); i++) {
                int col = i % 2;
                int row = i / 2;
                int cx = contentX + col * (colW + COL_GAP);
                int cy = listTop + row * (ROW_H + ROW_GAP) - scroll;
                if (!Ui.inside(mX, mY, cx, cy, colW, ROW_H)) continue;
                Module module = mods.get(i);
                int tx = cx + colW - TOG_W - 11;
                int ty = cy + (ROW_H - TOG_H) / 2;
                if (Ui.inside(mX, mY, tx - 4, ty - 5, TOG_W + 8, TOG_H + 10)) {
                    module.toggle();
                    return true;
                }
                if (module.settings != null) {
                    int sx = tx - 25;
                    if (Ui.inside(mX, mY, sx, cy + 8, 22, 30)) {
                        module.settings.run();
                        return true;
                    }
                }
                module.toggle();
                return true;
            }
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        if (searchFocused && !Character.isISOControl(chr)) {
            search += chr;
            scroll = 0;
            return true;
        }
        return super.charTyped(chr, modifiers);
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
            if (!search.isEmpty()) search = search.substring(0, search.length() - 1);
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
