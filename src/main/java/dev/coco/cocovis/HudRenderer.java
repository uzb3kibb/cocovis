package dev.coco.cocovis;

import dev.coco.cocovis.module.ModuleManager;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;

import java.util.ArrayList;
import java.util.List;

/** Маленькая панель Watermark / FPS / Coords + вызов панели алмазов и тостов. */
public final class HudRenderer {
    private static final int BG = 0xAA101014;
    private static final int ACCENT = 0xFF5CC8FF;
    private static final int TEXT = 0xFFFFFFFF;

    private HudRenderer() {}

    public static void render(GuiGraphicsExtractor g, DeltaTracker delta) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        // панель алмазов и тосты не рисуем здесь поверх вторично — они в DiamondHud
        DiamondHud.render(g, delta);

        List<String> lines = new ArrayList<>();
        if (ModuleManager.WATERMARK.isEnabled()) lines.add("CocoVis");
        if (ModuleManager.FPS.isEnabled()) lines.add(mc.getFps() + " FPS");
        if (ModuleManager.COORDS.isEnabled()) {
            lines.add(String.format("XYZ %.0f %.0f %.0f", mc.player.getX(), mc.player.getY(), mc.player.getZ()));
        }
        if (lines.isEmpty()) return;

        // под панелью алмазов, чтобы не наезжали друг на друга
        int width = 0;
        for (String s : lines) width = Math.max(width, mc.font.width(s));
        int x = 6;
        int y = ModuleManager.DIAMONDS.isEnabled()
                ? DiamondHud.boundsY + DiamondHud.boundsH + 6
                : 6;
        int h = lines.size() * 11 + 6;

        g.fill(x, y, x + width + 10, y + h, BG);
        g.fill(x, y, x + 2, y + h, ACCENT);
        int ty = y + 4;
        for (String s : lines) {
            g.text(mc.font, s, x + 6, ty, TEXT, true);
            ty += 11;
        }
    }
}
