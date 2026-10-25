package dev.coco.cocovis;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/** Рисует все элементы HUD: панель алмазов, тосты и информационную панель. */
public final class HudRenderer {
    private HudRenderer() {}

    public static void render(GuiGraphicsExtractor g, DeltaTracker delta) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        DiamondHud.render(g, delta);
        InfoHud.render(g, mc);
    }
}
