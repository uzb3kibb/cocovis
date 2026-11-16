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
 * CocoVis Control Center — redesigned dashboard with a persistent sidebar,
 * category counters, animated module cards and a compact status header.
 */
public class VisualsScreen extends Screen {
    private static final int ROW_H = 52;
    private static final int GAP = 7;
    private static final int CAT_H = 31;
    private static final int CAT_GAP = 5;
    private static final int TOG_W = 34;
    private static final int TOG_H = 16;
    private static final int SET_W = 82;
    private static final int SIDEBAR_W = 142;
    private static final String VERSION = FabricLoader.getInstance().getModContainer(CocoVisClient.MOD_ID)
            .map(c -> c.getMetadata().getVersion().getFriendlyString()).orElse("?");

    private static int savedCategory = 0;
    private int catIndex = savedCategory;
    private int scroll;
    private int mX, mY;
    private long lastNs = System.nanoTime();
    private float open;
    private final Map<Module, Float> toggleAnim = new HashMap<>();
    private final Map<Module, Float> hoverAnim = new HashMap<>();
    private final Map<Item, ItemStack> icons = new HashMap<>();
    private final float[] catHover = new float[ModuleManager.CATEGORIES.size()];
    private int x0, y0, w, h, cX, cW, listTop, listBottom;

    public VisualsScreen() { super(Component.literal("CocoVis Control Center")); }

    private ItemStack icon(Item item) { return icons.computeIfAbsent(item, k -> new ItemStack(k)); }

    private static float approach(float cur, float target, float step) {
        return cur < target ? Math.min(target, cur + step) : Math.max(target, cur - step);
    }

    private float ease() {
        float t = 1f - open;
        return 1f - t * t * t;
    }

    private void layout(int sw, int sh) {
        w = Math.min(sw - 24, 590);
        h = Math.min(sh - 24, 350);
        x0 = (sw - w) / 2;
        y0 = (sh - h) / 2;
    }

    private List<Module> modules() { return ModuleManager.CATEGORIES.get(catIndex).modules(); }

    private String fit(Font font, String s, int maxW) {
        if (maxW <= 0) return "";
        if (font.width(s) <= maxW) return s;
        String ellipsis = "…";
        while (!s.isEmpty() && font.width(s + ellipsis) > maxW) s = s.substring(0, s.length() - 1);
        return s + ellipsis;
    }

    private void bigText(GuiGraphicsExtractor g, Font font, String text, int x, int y, float scale, int color) {
        g.pose().pushMatrix();
        g.pose().translate((float) x, (float) y);
        g.pose().scale(scale, scale);
        g.text(font, text, 0, 0, color, true);
        g.pose().popMatrix();
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
        for (ModuleManager.Category category : ModuleManager.CATEGORIES) count += category.modules().size();
        return count;
    }

    private int maxScroll() {
        int content = modules().size() * (ROW_H + GAP) - GAP;
        return Math.max(0, content - (listBottom - listTop));
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
        open = approach(open, 1f, dt * 5.5f);
        float e = ease();

        int sw = mc.getWindow().getGuiScaledWidth();
        int sh = mc.getWindow().getGuiScaledHeight();
        layout(sw, sh);
        int oy = Math.round((1f - e) * 18f);
        int top = y0 + oy;

        g.fill(0, 0, sw, sh, Ui.fade(0xFF08060D, 0.72f * e));
        // Soft ambient light behind the control center.
        Ui.roundFill(g, x0 - 5, top + 8, w + 10, h + 8, 12, Ui.fade(0xFF7651A8, 0.10f));
        Ui.panel(g, x0, top, w, h);

        // Brand block
        Ui.roundFill(g, x0 + 10, top + 10, SIDEBAR_W - 20, 43, 7, 0xFF211A30);
        Ui.roundFill(g, x0 + 10, top + 10, 3, 43, 2, Ui.ACCENT);
        bigText(g, font, "CocoVis", x0 + 20, top + 15, 1.35f, Ui.TEXT);
        g.text(font, "CONTROL CENTER  ·  v" + VERSION, x0 + 20, top + 34, Ui.MUTED, true);

        // Sidebar categories
        int catY = top + 65;
        g.text(font, "РАЗДЕЛЫ", x0 + 15, catY - 12, Ui.DIM, true);
        for (int i = 0; i < ModuleManager.CATEGORIES.size(); i++) {
            ModuleManager.Category category = ModuleManager.CATEGORIES.get(i);
            int cx = x0 + 9;
            int cy = catY + i * (CAT_H + CAT_GAP);
            int cw = SIDEBAR_W - 18;
            boolean hover = Ui.inside(mX, mY, cx, cy, cw, CAT_H);
            catHover[i] = approach(catHover[i], hover ? 1f : 0f, dt * 8f);
            boolean selected = i == catIndex;
            if (selected) {
                Ui.roundGradient(g, cx, cy, cw, CAT_H, 6, Ui.CARD_HOT_TOP, Ui.CARD_HOT_BOT);
                Ui.roundFill(g, cx, cy + 7, 2, CAT_H - 14, 1, Ui.ACCENT);
            } else if (catHover[i] > 0.01f) {
                Ui.roundFill(g, cx, cy, cw, CAT_H, 6, Ui.fade(Ui.CARD_TOP, catHover[i]));
            }
            g.item(icon(category.icon()), cx + 8, cy + 7);
            g.text(font, category.name(), cx + 31, cy + 11, selected ? Ui.TEXT : Ui.MUTED, true);
            String count = enabledCount(category.modules()) + "/" + category.modules().size();
            g.text(font, count, cx + cw - font.width(count) - 8, cy + 11,
                    selected ? Ui.ACCENT : Ui.DIM, true);
        }

        // Sidebar status panel
        int statusY = top + h - 58;
        Ui.roundGradient(g, x0 + 10, statusY, SIDEBAR_W - 20, 44, 6, 0xFF1D1928, 0xFF17131F);
        g.text(font, "АКТИВНЫЕ МОДУЛИ", x0 + 18, statusY + 8, Ui.DIM, true);
        bigText(g, font, String.valueOf(totalEnabled()), x0 + 18, statusY + 20, 1.15f, Ui.ACCENT);
        String total = "/ " + totalModules() + " включено";
        g.text(font, total, x0 + 38, statusY + 25, Ui.MUTED, true);

        // Vertical separator
        g.fill(x0 + SIDEBAR_W, top + 10, x0 + SIDEBAR_W + 1, top + h - 10, Ui.BORDER);

        // Main content header and live status chip
        ModuleManager.Category category = ModuleManager.CATEGORIES.get(catIndex);
        cX = x0 + SIDEBAR_W + 16;
        cW = w - SIDEBAR_W - 30;
        bigText(g, font, category.name(), cX, top + 13, 1.35f, Ui.TEXT);
        g.text(font, category.subtitle(), cX, top + 32, Ui.MUTED, true);

        String active = enabledCount(modules()) + " АКТИВНО";
        int chipW = font.width(active) + 16;
        int chipX = x0 + w - chipW - 15;
        Ui.roundFill(g, chipX, top + 13, chipW, 19, 8, 0xFF292039);
        Ui.roundFill(g, chipX + 7, top + 20, 5, 5, 3, Ui.GOOD);
        g.text(font, active, chipX + 16, top + 19, Ui.TEXT, true);

        // Accent divider under header
        Ui.hBar(g, cX, top + 52, Math.min(cW, 90), 2, Ui.ACCENT, Ui.ACCENT2);
        Ui.hBar(g, cX + Math.min(cW, 90), top + 52, Math.max(0, cW - 90), 1, Ui.BORDER, Ui.BORDER);

        listTop = top + 63;
        listBottom = top + h - 29;
        scroll = Math.max(0, Math.min(scroll, maxScroll()));

        g.enableScissor(cX - 2, listTop, cX + cW + 2, listBottom);
        List<Module> mods = modules();
        for (int i = 0; i < mods.size(); i++) {
            Module module = mods.get(i);
            int cy = listTop + i * (ROW_H + GAP) - scroll;
            if (cy + ROW_H < listTop || cy > listBottom) continue;

            boolean hover = Ui.inside(mX, mY, cX, cy, cW, ROW_H) && mY >= listTop && mY < listBottom;
            float hv = approach(hoverAnim.getOrDefault(module, 0f), hover ? 1f : 0f, dt * 8f);
            hoverAnim.put(module, hv);
            boolean enabled = module.isEnabled();

            Ui.roundGradient(g, cX, cy, cW, ROW_H, 7,
                    Ui.mix(Ui.CARD_TOP, Ui.CARD_HOT_TOP, hv), Ui.mix(Ui.CARD_BOT, Ui.CARD_HOT_BOT, hv));
            Ui.roundOutline(g, cX, cy, cW, ROW_H, 7,
                    Ui.mix(Ui.BORDER, Ui.fade(Ui.ACCENT, 0.72f), hv));

            // Icon tile, with a subtle enabled glow.
            int tile = enabled ? 0xFF302440 : 0xFF292631;
            Ui.roundFill(g, cX + 8, cy + 9, 34, 34, 7, tile);
            if (enabled) Ui.roundFill(g, cX + 8, cy + 9, 2, 34, 1, Ui.ACCENT);
            g.item(icon(module.icon), cX + 17, cy + 17);

            int tx = cX + cW - TOG_W - 12;
            int ty = cy + (ROW_H - TOG_H) / 2;
            int rightEdge = tx;
            if (module.settings != null) {
                int sx = tx - SET_W - 9;
                int sy = cy + (ROW_H - 19) / 2;
                boolean sHover = Ui.inside(mX, mY, sx, sy, SET_W, 19);
                Ui.roundGradient(g, sx, sy, SET_W, 19, 5,
                        sHover ? Ui.CARD_HOT_TOP : 0xFF282333, sHover ? Ui.CARD_HOT_BOT : Ui.CARD_BOT);
                Ui.roundOutline(g, sx, sy, SET_W, 19, 5, sHover ? Ui.ACCENT : Ui.BORDER);
                String label = fit(font, module.settingsLabel, SET_W - 10);
                g.text(font, label, sx + (SET_W - font.width(label)) / 2, sy + 6,
                        sHover ? Ui.TEXT : Ui.MUTED, true);
                rightEdge = sx;
            }

            int textX = cX + 51;
            int textMax = rightEdge - textX - 10;
            g.text(font, fit(font, module.name, textMax), textX, cy + 11, Ui.TEXT, true);
            g.text(font, fit(font, module.description(), textMax), textX, cy + 29,
                    enabled ? Ui.MUTED : Ui.DIM, true);

            float ta = approach(toggleAnim.getOrDefault(module, enabled ? 1f : 0f),
                    enabled ? 1f : 0f, dt * 9f);
            toggleAnim.put(module, ta);
            Ui.roundFill(g, tx, ty, TOG_W, TOG_H, 8,
                    Ui.mix(0xFF3A3544, Ui.ACCENT, ta));
            int kx = tx + 2 + Math.round(ta * (TOG_W - 14));
            Ui.roundFill(g, kx, ty + 2, 12, 12, 6, 0xFFF8F5FF);
        }
        g.disableScissor();

        int ms = maxScroll();
        if (ms > 0) {
            int trackH = listBottom - listTop;
            int barH = Math.max(18, trackH * trackH / (trackH + ms));
            int barY = listTop + Math.round((trackH - barH) * (scroll / (float) ms));
            Ui.roundFill(g, cX + cW + 4, listTop, 2, trackH, 1, Ui.BORDER);
            Ui.roundFill(g, cX + cW + 4, barY, 2, barH, 1, Ui.ACCENT);
        }

        g.text(font, "Esc — закрыть  ·  Колесо — прокрутка",
                cX, top + h - 16, Ui.DIM, true);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        int top = y0 + Math.round((1f - ease()) * 18f);
        int catY = top + 65;
        for (int i = 0; i < ModuleManager.CATEGORIES.size(); i++) {
            int cy = catY + i * (CAT_H + CAT_GAP);
            if (Ui.inside(mX, mY, x0 + 9, cy, SIDEBAR_W - 18, CAT_H)) {
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
                int cy = listTop + i * (ROW_H + GAP) - scroll;
                if (!Ui.inside(mX, mY, cX, cy, cW, ROW_H)) continue;
                int tx = cX + cW - TOG_W - 12;
                int ty = cy + (ROW_H - TOG_H) / 2;
                if (Ui.inside(mX, mY, tx - 5, ty - 5, TOG_W + 10, TOG_H + 10)) {
                    module.toggle();
                    return true;
                }
                if (module.settings != null) {
                    int sx = tx - SET_W - 9;
                    int sy = cy + (ROW_H - 19) / 2;
                    if (Ui.inside(mX, mY, sx, sy, SET_W, 19)) {
                        module.settings.run();
                        return true;
                    }
                }
                return true;
            }
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        scroll = Math.max(0, Math.min(maxScroll(), scroll - (int) Math.round(scrollY * 22)));
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
    public boolean isPauseScreen() { return false; }
}
