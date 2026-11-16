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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * CocoVis Ultra Control Center.
 * A wider, editorial-style control panel with a dedicated navigation rail,
 * module cards, active-state accents and responsive sizing.
 */
public class VisualsScreen extends Screen {
    private static final int SIDEBAR_W = 176;
    private static final int CARD_H = 70;
    private static final int CARD_GAP = 9;
    private static final int TOG_W = 38;
    private static final int TOG_H = 18;
    private static final int SET_W = 88;
    private static final String VERSION = FabricLoader.getInstance()
            .getModContainer(CocoVisClient.MOD_ID)
            .map(c -> c.getMetadata().getVersion().getFriendlyString()).orElse("?");

    private static int savedCategory;
    private int catIndex = savedCategory;
    private int scroll;
    private int mX, mY;
    private long lastNs = System.nanoTime();
    private float open;
    private final Map<Module, Float> toggleAnim = new HashMap<>();
    private final Map<Module, Float> hoverAnim = new HashMap<>();
    private final Map<Item, ItemStack> icons = new HashMap<>();
    private final float[] catHover = new float[ModuleManager.CATEGORIES.size()];
    private int x0, y0, w, h, cX, cW, listTop, listBottom, topY;

    public VisualsScreen() {
        super(Component.literal("CocoVis Ultra Control Center"));
    }

    private ItemStack icon(Item item) {
        return icons.computeIfAbsent(item, k -> new ItemStack(k));
    }

    private static float approach(float cur, float target, float step) {
        return cur < target ? Math.min(target, cur + step) : Math.max(target, cur - step);
    }

    private float ease() {
        float t = 1f - open;
        return 1f - t * t * t;
    }

    private void layout(int sw, int sh) {
        w = Math.min(sw - 36, 900);
        h = Math.min(sh - 36, 520);
        w = Math.max(500, w);
        h = Math.max(300, h);
        x0 = (sw - w) / 2;
        y0 = (sh - h) / 2;
    }

    private List<Module> modules() {
        return ModuleManager.CATEGORIES.get(catIndex).modules();
    }

    private String fit(Font font, String s, int maxW) {
        if (maxW <= 0) return "";
        if (font.width(s) <= maxW) return s;
        String suffix = "…";
        while (!s.isEmpty() && font.width(s + suffix) > maxW) {
            s = s.substring(0, s.length() - 1);
        }
        return s + suffix;
    }

    private void scaledText(GuiGraphicsExtractor g, String value, int x, int y, float scale, int color) {
        g.pose().pushMatrix();
        g.pose().translate((float) x, (float) y);
        g.pose().scale(scale, scale);
        g.text(Minecraft.getInstance().font, value, 0, 0, color, true);
        g.pose().popMatrix();
    }

    private int enabledCount(List<Module> list) {
        int count = 0;
        for (Module module : list) if (module.isEnabled()) count++;
        return count;
    }

    private int totalEnabled() {
        int count = 0;
        for (ModuleManager.Category category : ModuleManager.CATEGORIES) {
            count += enabledCount(category.modules());
        }
        return count;
    }

    private int totalModules() {
        int count = 0;
        for (ModuleManager.Category category : ModuleManager.CATEGORIES) {
            count += category.modules().size();
        }
        return count;
    }

    private int maxScroll() {
        int content = modules().size() * (CARD_H + CARD_GAP) - CARD_GAP;
        return Math.max(0, content - Math.max(1, listBottom - listTop));
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
        open = approach(open, 1f, dt * 4.8f);
        float e = ease();

        int sw = mc.getWindow().getGuiScaledWidth();
        int sh = mc.getWindow().getGuiScaledHeight();
        layout(sw, sh);
        topY = y0 + Math.round((1f - e) * 22f);

        // Dimmed world and atmospheric accent bloom.
        g.fill(0, 0, sw, sh, Ui.fade(0xFF07060C, 0.78f * e));
        Ui.roundFill(g, x0 - 18, topY + 22, w + 36, h + 22, 18, Ui.fade(0xFF7952B5, 0.075f));
        Ui.roundFill(g, x0 + w / 2 - 150, topY - 16, 300, 3, 2, Ui.fade(Ui.ACCENT, 0.35f));

        Ui.shadow(g, x0, topY, w, h, 13);
        Ui.roundGradient(g, x0, topY, w, h, 13, 0xFF1C1826, 0xFF100E17);
        Ui.roundOutline(g, x0, topY, w, h, 13, 0xFF443653);

        // Top brand strip.
        Ui.roundFill(g, x0 + 1, topY + 1, w - 2, 3, 2, Ui.ACCENT);
        Ui.roundFill(g, x0 + 17, topY + 16, SIDEBAR_W - 34, 58, 9, 0xFF251E32);
        Ui.roundFill(g, x0 + 17, topY + 16, 3, 58, 2, Ui.ACCENT);
        scaledText(g, "CocoVis", x0 + 30, topY + 23, 1.55f, Ui.TEXT);
        g.text(font, "ULTRA CONTROL CENTER", x0 + 31, topY + 48, Ui.ACCENT, true);
        g.text(font, "BUILD  " + VERSION, x0 + 31, topY + 61, Ui.DIM, true);

        // Sidebar navigation.
        int navTitleY = topY + 91;
        g.text(font, "WORKSPACE", x0 + 22, navTitleY, Ui.DIM, true);
        int navY = navTitleY + 14;
        for (int i = 0; i < ModuleManager.CATEGORIES.size(); i++) {
            ModuleManager.Category category = ModuleManager.CATEGORIES.get(i);
            int nx = x0 + 15;
            int ny = navY + i * 48;
            int nw = SIDEBAR_W - 30;
            boolean hover = Ui.inside(mX, mY, nx, ny, nw, 41);
            catHover[i] = approach(catHover[i], hover ? 1f : 0f, dt * 7f);
            boolean selected = i == catIndex;

            if (selected) {
                Ui.roundGradient(g, nx, ny, nw, 41, 8, 0xFF352A47, 0xFF282033);
                Ui.roundOutline(g, nx, ny, nw, 41, 8, 0xFF57436F);
                Ui.roundFill(g, nx, ny + 8, 3, 25, 2, Ui.ACCENT);
            } else if (catHover[i] > 0.01f) {
                Ui.roundFill(g, nx, ny, nw, 41, 8, Ui.fade(0xFF514260, catHover[i] * 0.55f));
            }

            Ui.roundFill(g, nx + 8, ny + 7, 27, 27, 7,
                    selected ? 0xFF463458 : 0xFF292431);
            g.item(icon(category.icon()), nx + 13, ny + 12);
            g.text(font, category.name(), nx + 43, ny + 9,
                    selected ? Ui.TEXT : Ui.MUTED, true);
            String count = enabledCount(category.modules()) + "/" + category.modules().size();
            g.text(font, count, nx + nw - font.width(count) - 9, ny + 24,
                    selected ? Ui.ACCENT : Ui.DIM, true);
        }

        // Sidebar footer stats.
        int footerY = topY + h - 77;
        Ui.roundGradient(g, x0 + 16, footerY, SIDEBAR_W - 32, 59, 9, 0xFF282137, 0xFF1C1827);
        g.text(font, "SYSTEM STATUS", x0 + 26, footerY + 9, Ui.DIM, true);
        Ui.roundFill(g, x0 + 27, footerY + 26, 7, 7, 4, Ui.GOOD);
        scaledText(g, String.valueOf(totalEnabled()), x0 + 41, footerY + 22, 1.15f, Ui.TEXT);
        String stat = " / " + totalModules() + " modules active";
        g.text(font, stat, x0 + 58, footerY + 30, Ui.MUTED, true);

        // Sidebar divider.
        g.fill(x0 + SIDEBAR_W, topY + 15, x0 + SIDEBAR_W + 1, topY + h - 15, 0xFF393044);

        // Main workspace header.
        cX = x0 + SIDEBAR_W + 25;
        cW = w - SIDEBAR_W - 48;
        ModuleManager.Category category = ModuleManager.CATEGORIES.get(catIndex);
        scaledText(g, category.name(), cX, topY + 21, 1.55f, Ui.TEXT);
        g.text(font, category.subtitle(), cX + 1, topY + 46, Ui.MUTED, true);

        String active = enabledCount(modules()) + " ENABLED";
        int chipW = font.width(active) + 28;
        int chipX = x0 + w - chipW - 22;
        Ui.roundFill(g, chipX, topY + 22, chipW, 24, 12, 0xFF292239);
        Ui.roundFill(g, chipX + 9, topY + 31, 6, 6, 3, Ui.GOOD);
        g.text(font, active, chipX + 20, topY + 30, Ui.TEXT, true);

        // Header separator and accent.
        g.fill(cX, topY + 78, cX + cW, topY + 79, 0xFF393044);
        Ui.hBar(g, cX, topY + 77, Math.min(112, cW), 2, Ui.ACCENT, Ui.ACCENT2);

        // List heading.
        listTop = topY + 96;
        listBottom = topY + h - 30;
        scroll = Math.max(0, Math.min(scroll, maxScroll()));
        g.text(font, "MODULES", cX + 2, topY + 86, Ui.DIM, true);
        String countLabel = modules().size() + " FEATURES";
        g.text(font, countLabel, cX + cW - font.width(countLabel), topY + 86, Ui.DIM, true);

        g.enableScissor(cX - 2, listTop, cX + cW + 2, listBottom);
        List<Module> mods = modules();
        for (int i = 0; i < mods.size(); i++) {
            Module module = mods.get(i);
            int cy = listTop + i * (CARD_H + CARD_GAP) - scroll;
            if (cy + CARD_H < listTop || cy > listBottom) continue;

            boolean hover = Ui.inside(mX, mY, cX, cy, cW, CARD_H)
                    && mY >= listTop && mY < listBottom;
            float hv = approach(hoverAnim.getOrDefault(module, 0f), hover ? 1f : 0f, dt * 8f);
            hoverAnim.put(module, hv);
            boolean enabled = module.isEnabled();

            int cardTop = Ui.mix(0xFF211C2B, 0xFF30263D, hv);
            int cardBot = Ui.mix(0xFF191622, 0xFF241D30, hv);
            Ui.roundGradient(g, cX, cy, cW, CARD_H, 10, cardTop, cardBot);
            Ui.roundOutline(g, cX, cy, cW, CARD_H, 10,
                    enabled ? Ui.mix(0xFF4B3B5E, Ui.ACCENT, hv * 0.45f) : Ui.mix(Ui.BORDER, 0xFF6D5A80, hv));
            if (enabled) {
                Ui.roundFill(g, cX + 1, cy + 12, 3, CARD_H - 24, 2, Ui.ACCENT);
                Ui.roundFill(g, cX + 7, cy + 7, 42, 56, 9, 0xFF30243E);
                Ui.roundFill(g, cX + 7, cy + 7, 42, 2, 1, Ui.fade(Ui.ACCENT, 0.85f));
            } else {
                Ui.roundFill(g, cX + 7, cy + 7, 42, 56, 9, 0xFF282330);
            }
            g.item(icon(module.icon), cX + 20, cy + 24);

            int tx = cX + cW - TOG_W - 15;
            int ty = cy + (CARD_H - TOG_H) / 2;
            int settingsX = tx;
            if (module.settings != null) {
                int sx = tx - SET_W - 11;
                int sy = cy + (CARD_H - 23) / 2;
                boolean settingsHover = Ui.inside(mX, mY, sx, sy, SET_W, 23);
                Ui.roundGradient(g, sx, sy, SET_W, 23, 6,
                        settingsHover ? 0xFF443451 : 0xFF292332,
                        settingsHover ? 0xFF35283F : 0xFF211D2B);
                Ui.roundOutline(g, sx, sy, SET_W, 23, 6,
                        settingsHover ? Ui.ACCENT : 0xFF44394F);
                String label = fit(font, module.settingsLabel, SET_W - 12);
                g.text(font, label, sx + (SET_W - font.width(label)) / 2, sy + 8,
                        settingsHover ? Ui.TEXT : Ui.MUTED, true);
                settingsX = sx;
            }

            int textX = cX + 63;
            int textMax = settingsX - textX - 13;
            g.text(font, fit(font, module.name, textMax), textX, cy + 17,
                    enabled ? Ui.TEXT : 0xFFD5D0DE, true);
            g.text(font, fit(font, module.description(), textMax), textX, cy + 39,
                    enabled ? Ui.MUTED : Ui.DIM, true);

            float ta = approach(toggleAnim.getOrDefault(module, enabled ? 1f : 0f),
                    enabled ? 1f : 0f, dt * 10f);
            toggleAnim.put(module, ta);
            Ui.roundFill(g, tx - 2, ty - 2, TOG_W + 4, TOG_H + 4, 11,
                    Ui.fade(Ui.ACCENT, ta * 0.12f));
            Ui.roundFill(g, tx, ty, TOG_W, TOG_H, 9,
                    Ui.mix(0xFF3B3544, 0xFFB58CFF, ta));
            int knobX = tx + 2 + Math.round(ta * (TOG_W - 16));
            Ui.roundFill(g, knobX, ty + 2, 14, 14, 7, 0xFFF8F5FF);
        }
        g.disableScissor();

        int max = maxScroll();
        if (max > 0) {
            int trackH = listBottom - listTop;
            int thumbH = Math.max(22, trackH * trackH / (trackH + max));
            int thumbY = listTop + Math.round((trackH - thumbH) * (scroll / (float) max));
            Ui.roundFill(g, cX + cW + 7, listTop, 2, trackH, 1, 0xFF302A3A);
            Ui.roundFill(g, cX + cW + 7, thumbY, 2, thumbH, 1, Ui.ACCENT);
        }

        String footer = "ESC  CLOSE     •     SCROLL  BROWSE";
        g.text(font, footer, cX, topY + h - 17, Ui.DIM, true);
        String brand = "COCOVIS  /  CLIENT UI";
        g.text(font, brand, x0 + w - font.width(brand) - 19, topY + h - 17, Ui.DIM, true);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        int navTitleY = topY + 91;
        int navY = navTitleY + 14;
        for (int i = 0; i < ModuleManager.CATEGORIES.size(); i++) {
            int ny = navY + i * 48;
            if (Ui.inside(mX, mY, x0 + 15, ny, SIDEBAR_W - 30, 41)) {
                catIndex = i;
                savedCategory = i;
                scroll = 0;
                return true;
            }
        }

        if (mY >= listTop && mY < listBottom) {
            List<Module> mods = modules();
            for (int i = 0; i < mods.size(); i++) {
                Module module = mods.get(i);
                int cy = listTop + i * (CARD_H + CARD_GAP) - scroll;
                if (!Ui.inside(mX, mY, cX, cy, cW, CARD_H)) continue;

                int tx = cX + cW - TOG_W - 15;
                int ty = cy + (CARD_H - TOG_H) / 2;
                if (Ui.inside(mX, mY, tx - 6, ty - 7, TOG_W + 12, TOG_H + 14)) {
                    module.toggle();
                    return true;
                }
                if (module.settings != null) {
                    int sx = tx - SET_W - 11;
                    int sy = cy + (CARD_H - 23) / 2;
                    if (Ui.inside(mX, mY, sx, sy, SET_W, 23)) {
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
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        scroll = Math.max(0, Math.min(maxScroll(), scroll - (int) Math.round(scrollY * 28)));
        return true;
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (event.key() == 256) {
            Minecraft.getInstance().gui.setScreen(null);
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
