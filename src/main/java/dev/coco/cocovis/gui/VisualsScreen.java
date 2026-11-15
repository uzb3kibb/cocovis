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

/** CocoVis main menu: categories on the left, module cards with toggles on the right. */
public class VisualsScreen extends Screen {
    private static final int ROW_H = 46;
    private static final int GAP = 6;
    private static final int CAT_H = 28;
    private static final int CAT_GAP = 4;
    private static final int TOG_W = 30;
    private static final int TOG_H = 14;
    private static final int SET_W = 78;
    private static final int SIDEBAR_W = 118;
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

    public VisualsScreen() {
        super(Component.literal("CocoVis"));
    }

    private ItemStack icon(Item item) {
        return icons.computeIfAbsent(item, k -> new ItemStack(k));
    }

    private static float approach(float cur, float target, float step) {
        if (cur < target) {
            return Math.min(target, cur + step);
        }
        return Math.max(target, cur - step);
    }

    private float ease() {
        float t = 1f - open;
        return 1f - t * t * t;
    }

    private void layout(int sw, int sh) {
        w = Math.min(sw - 30, 470);
        h = Math.min(sh - 30, 270);
        x0 = (sw - w) / 2;
        y0 = (sh - h) / 2;
    }

    private List<Module> modules() {
        return ModuleManager.CATEGORIES.get(catIndex).modules();
    }

    private String fit(Font font, String s, int maxW) {
        if (font.width(s) <= maxW) {
            return s;
        }
        String ellipsis = "…";
        while (!s.isEmpty() && font.width(s + ellipsis) > maxW) {
            s = s.substring(0, s.length() - 1);
        }
        return s + ellipsis;
    }

    private void bigText(GuiGraphicsExtractor g, Font font, String text, int x, int y, float scale, int color) {
        g.pose().pushMatrix();
        g.pose().translate((float) x, (float) y);
        g.pose().scale(scale, scale);
        g.text(font, text, 0, 0, color, true);
        g.pose().popMatrix();
    }

    private int maxScroll() {
        int n = modules().size();
        int content = n * (ROW_H + GAP) - GAP;
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
        open = approach(open, 1f, dt * 6f);
        float e = ease();

        int sw = mc.getWindow().getGuiScaledWidth();
        int sh = mc.getWindow().getGuiScaledHeight();
        layout(sw, sh);
        int oy = Math.round((1f - e) * 14f);
        int top = y0 + oy;

        g.fill(0, 0, sw, sh, Ui.fade(0xFF000000, 0.5f * e));
        Ui.panel(g, x0, top, w, h);

        // sidebar
        bigText(g, font, "CocoVis", x0 + 14, top + 11, 1.5f, Ui.ACCENT);
        g.text(font, "v" + VERSION, x0 + 14, top + 28, Ui.DIM, true);
        int catY = top + 46;
        for (int i = 0; i < ModuleManager.CATEGORIES.size(); i++) {
            ModuleManager.Category c = ModuleManager.CATEGORIES.get(i);
            int cx = x0 + 8;
            int cy = catY + i * (CAT_H + CAT_GAP);
            int cw = SIDEBAR_W - 16;
            boolean hover = Ui.inside(mX, mY, cx, cy, cw, CAT_H);
            catHover[i] = approach(catHover[i], hover ? 1f : 0f, dt * 8f);
            boolean selected = i == catIndex;
            if (selected) {
                Ui.roundGradient(g, cx, cy, cw, CAT_H, 4, Ui.CARD_HOT_TOP, Ui.CARD_HOT_BOT);
                g.fill(cx, cy + 6, cx + 2, cy + CAT_H - 6, Ui.ACCENT);
            } else if (catHover[i] > 0.01f) {
                Ui.roundFill(g, cx, cy, cw, CAT_H, 4, Ui.fade(Ui.CARD_TOP, catHover[i]));
            }
            g.item(icon(c.icon()), cx + 7, cy + 6);
            g.text(font, c.name(), cx + 30, cy + 10, selected ? Ui.TEXT : Ui.MUTED, true);
        }

        // divider
        g.fill(x0 + SIDEBAR_W, top + 10, x0 + SIDEBAR_W + 1, top + h - 10, Ui.BORDER);

        // content header
        ModuleManager.Category cat = ModuleManager.CATEGORIES.get(catIndex);
        cX = x0 + SIDEBAR_W + 12;
        cW = w - SIDEBAR_W - 24;
        bigText(g, font, cat.name(), cX, top + 11, 1.3f, Ui.TEXT);
        g.text(font, cat.subtitle(), cX, top + 27, Ui.MUTED, true);
        listTop = top + 44;
        listBottom = top + h - 24;
        scroll = Math.max(0, Math.min(scroll, maxScroll()));

        g.enableScissor(cX - 2, listTop, cX + cW + 2, listBottom);
        List<Module> mods = modules();
        for (int i = 0; i < mods.size(); i++) {
            Module m = mods.get(i);
            int cy = listTop + i * (ROW_H + GAP) - scroll;
            if (cy + ROW_H < listTop || cy > listBottom) {
                continue;
            }
            boolean hover = Ui.inside(mX, mY, cX, cy, cW, ROW_H) && mY >= listTop && mY < listBottom;
            float hv = approach(hoverAnim.getOrDefault(m, 0f), hover ? 1f : 0f, dt * 8f);
            hoverAnim.put(m, hv);
            Ui.roundGradient(g, cX, cy, cW, ROW_H, 5,
                    Ui.mix(Ui.CARD_TOP, Ui.CARD_HOT_TOP, hv), Ui.mix(Ui.CARD_BOT, Ui.CARD_HOT_BOT, hv));
            Ui.roundOutline(g, cX, cy, cW, ROW_H, 5, Ui.mix(Ui.BORDER, Ui.fade(Ui.ACCENT, 0.6f), hv));

            g.item(icon(m.icon), cX + 12, cy + ROW_H / 2 - 8);

            boolean on = m.isEnabled();
            float ta = approach(toggleAnim.getOrDefault(m, on ? 1f : 0f), on ? 1f : 0f, dt * 8f);
            toggleAnim.put(m, ta);
            int tx = cX + cW - TOG_W - 12;
            int ty = cy + (ROW_H - TOG_H) / 2;
            int rightEdge = tx;
            if (m.settings != null) {
                int sx = tx - SET_W - 10;
                int sy = cy + (ROW_H - 16) / 2;
                boolean sHover = Ui.inside(mX, mY, sx, sy, SET_W, 16);
                Ui.roundGradient(g, sx, sy, SET_W, 16, 4,
                        sHover ? Ui.CARD_HOT_TOP : Ui.CARD_TOP, sHover ? Ui.CARD_HOT_BOT : Ui.CARD_BOT);
                Ui.roundOutline(g, sx, sy, SET_W, 16, 4, sHover ? Ui.ACCENT : Ui.BORDER);
                String label = fit(font, m.settingsLabel, SET_W - 8);
                g.text(font, label, sx + (SET_W - font.width(label)) / 2, sy + 4, Ui.TEXT, true);
                rightEdge = sx;
            }
            int textMax = rightEdge - (cX + 36) - 10;
            g.text(font, fit(font, m.name, textMax), cX + 36, cy + 10, Ui.TEXT, true);
            g.text(font, fit(font, m.description(), textMax), cX + 36, cy + 25, Ui.MUTED, true);

            Ui.roundFill(g, tx, ty, TOG_W, TOG_H, 7, Ui.mix(0xFF39414B, Ui.ACCENT, ta));
            int kx = tx + 2 + Math.round(ta * (TOG_W - 14));
            Ui.roundFill(g, kx, ty + 2, 10, 10, 5, 0xFFF4F8FB);
        }
        g.disableScissor();

        // scrollbar
        int ms = maxScroll();
        if (ms > 0) {
            int trackH = listBottom - listTop;
            int barH = Math.max(16, trackH * trackH / (trackH + ms));
            int barY = listTop + Math.round((trackH - barH) * (scroll / (float) ms));
            g.fill(cX + cW + 4, barY, cX + cW + 6, barY + barH, Ui.DIM);
        }

        g.text(font, "Esc: закрыть  ·  колесо: прокрутка", cX, top + h - 16, Ui.DIM, true);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        Minecraft mc = Minecraft.getInstance();
        int sh = mc.getWindow().getGuiScaledHeight();
        int top = y0 + Math.round((1f - ease()) * 14f);
        int catY = top + 46;
        for (int i = 0; i < ModuleManager.CATEGORIES.size(); i++) {
            int cy = catY + i * (CAT_H + CAT_GAP);
            if (Ui.inside(mX, mY, x0 + 8, cy, SIDEBAR_W - 16, CAT_H)) {
                catIndex = i;
                savedCategory = i;
                scroll = 0;
                return true;
            }
        }
        if (mY >= listTop && mY < listBottom) {
            List<Module> mods = modules();
            for (int i = 0; i < mods.size(); i++) {
                Module m = mods.get(i);
                int cy = listTop + i * (ROW_H + GAP) - scroll;
                if (!Ui.inside(mX, mY, cX, cy, cW, ROW_H)) {
                    continue;
                }
                int tx = cX + cW - TOG_W - 12;
                int ty = cy + (ROW_H - TOG_H) / 2;
                if (Ui.inside(mX, mY, tx - 4, ty - 4, TOG_W + 8, TOG_H + 8)) {
                    m.toggle();
                    return true;
                }
                if (m.settings != null) {
                    int sx = tx - SET_W - 10;
                    int sy = cy + (ROW_H - 16) / 2;
                    if (Ui.inside(mX, mY, sx, sy, SET_W, 16)) {
                        m.settings.run();
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
        scroll = Math.max(0, Math.min(maxScroll(), scroll - (int) Math.round(scrollY * 18)));
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
