package dev.coco.cocovis;

import dev.coco.cocovis.gui.Ui;
import dev.coco.cocovis.module.ModuleManager;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/** Карточки Watermark / FPS / Coords в том же стиле, что и меню, + вызов панели алмазов и тостов. */
public final class HudRenderer {
    private static final int WHITE = 0xFFFFFFFF;
    private static final int MUTED = 0xFF8E99A6;

    private HudRenderer() {}

    public static void render(GuiGraphicsExtractor g, DeltaTracker delta) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        DiamondHud.render(g, delta);

        Font f = mc.font;
        int x = 6;
        int y = ModuleManager.DIAMONDS.isEnabled() ? DiamondHud.boundsY + DiamondHud.boundsH + 6 : 6;

        if (ModuleManager.WATERMARK.isEnabled()) {
            int w = f.width("CocoVis") + 24;
            Ui.roundFill(g, x, y, w, 18, 6, DiamondHud.PANEL);
            Ui.roundFill(g, x + 7, y + 6, 6, 6, 3, DiamondHud.ACCENT);
            g.text(f, "CocoVis", x + 18, y + 5, WHITE, true);
            y += 22;
        }
        if (ModuleManager.FPS.isEnabled()) {
            y += chip(g, f, x, y, "FPS", String.valueOf(mc.getFps()));
        }
        if (ModuleManager.COORDS.isEnabled()) {
            String xyz = String.format("%.0f %.0f %.0f", mc.player.getX(), mc.player.getY(), mc.player.getZ());
            y += chip(g, f, x, y, "XYZ", xyz);
        }
    }

    private static int chip(GuiGraphicsExtractor g, Font f, int x, int y, String label, String value) {
        int w = f.width(label) + f.width(value) + 22;
        Ui.roundFill(g, x, y, w, 18, 6, DiamondHud.PANEL);
        g.text(f, label, x + 7, y + 5, MUTED, false);
        g.text(f, value, x + 7 + f.width(label) + 6, y + 5, WHITE, true);
        return 22;
    }
}
