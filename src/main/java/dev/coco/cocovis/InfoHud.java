package dev.coco.cocovis;

import dev.coco.cocovis.gui.Ui;
import dev.coco.cocovis.module.ModuleManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;

import java.util.ArrayList;
import java.util.List;

/** Одна панель для Watermark / FPS / Coords. Двигается и масштабируется в редакторе. */
public final class InfoHud {
    /** Границы панели в GUI-координатах (для редактора). */
    public static int boundsX, boundsY, boundsW = 100, boundsH = 30;

    private record Line(String label, String value) {}

    private InfoHud() {}

    public static void render(GuiGraphicsExtractor g, Minecraft mc) {
        if (!ModuleManager.WATERMARK.isEnabled() && !ModuleManager.FPS.isEnabled() && !ModuleManager.COORDS.isEnabled()) return;
        draw(g, mc, false);
    }

    public static void draw(GuiGraphicsExtractor g, Minecraft mc, boolean preview) {
        DiamondConfig cfg = DiamondConfig.get();
        Font f = mc.font;

        boolean wm = ModuleManager.WATERMARK.isEnabled();
        List<Line> lines = new ArrayList<>();
        if (ModuleManager.FPS.isEnabled()) lines.add(new Line("FPS", String.valueOf(mc.getFps())));
        if (ModuleManager.COORDS.isEnabled() && mc.player != null) {
            lines.add(new Line("XYZ", String.format("%.0f  %.0f  %.0f", mc.player.getX(), mc.player.getY(), mc.player.getZ())));
        }
        if (preview && !wm && lines.isEmpty()) wm = true; // в редакторе всегда есть за что схватиться
        if (!wm && lines.isEmpty()) return;

        int rowH = 16;
        int top = wm ? 26 : 8;
        int w = 100;
        if (wm) w = Math.max(w, f.width("CocoVis") + 62);
        for (Line l : lines) w = Math.max(w, f.width(l.label()) + f.width(l.value()) + 36);
        int h = top + lines.size() * rowH + (lines.isEmpty() ? 0 : 4);

        float s = cfg.infoScale;
        int x = cfg.infoX >= 0 ? cfg.infoX : 6;
        int y = cfg.infoY >= 0 ? cfg.infoY
                : ((ModuleManager.DIAMONDS.isEnabled() || preview) ? DiamondHud.boundsY + DiamondHud.boundsH + 6 : 6);

        boundsX = x;
        boundsY = y;
        boundsW = Math.round(w * s);
        boundsH = Math.round(h * s);

        var pose = g.pose();
        pose.pushMatrix();
        pose.translate((float) x, (float) y);
        pose.scale(s, s);

        Ui.panel(g, 0, 0, w, h, 9, Ui.PANEL_TOP, Ui.PANEL_BOT);

        if (wm) {
            Ui.circle(g, 14, 13, 6, 0x305CC8FF);
            Ui.circle(g, 14, 13, 3, Ui.ACCENT);
            g.text(f, "CocoVis", 25, 8, Ui.TEXT, true);
            Ui.hBar(g, 25, 19, 26, 2, Ui.ACCENT, Ui.ACCENT2);
        }
        if (!lines.isEmpty()) {
            if (wm) g.fill(9, top - 3, w - 9, top - 2, 0x18FFFFFF);
            int y0 = top;
            for (Line l : lines) {
                g.text(f, l.label(), 10, y0 + 4, Ui.MUTED, false);
                g.text(f, l.value(), w - 10 - f.width(l.value()), y0 + 4, Ui.TEXT, true);
                y0 += rowH;
            }
        }
        pose.popMatrix();
    }
}
