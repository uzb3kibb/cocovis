package dev.coco.cocovis;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** Рисует все элементы HUD: панель алмазов, тосты и информационную панель. */
public final class HudRenderer {
    private static ItemStack leftGlove;

    private HudRenderer() {}

    /** Левая перчатка в левом нижнем углу, как у CS2: рисуется, пока в правой руке нож. */
    private static void drawLeftGlove(GuiGraphicsExtractor g, Minecraft mc) {
        if (mc.options.hideGui || !Knives.isKnifeInHand(mc)) return;
        if (leftGlove == null) {
            leftGlove = new ItemStack(Items.PAPER);
            leftGlove.set(DataComponents.ITEM_MODEL, Identifier.fromNamespaceAndPath("cocovis", "glove_left"));
        }
        int h = mc.getWindow().getGuiScaledHeight();
        var pose = g.pose();
        pose.pushMatrix();
        pose.translate(-4f, h - 112f);
        pose.scale(7f, 7f);
        g.item(leftGlove, 0, 0);
        pose.popMatrix();
    }

    public static void render(GuiGraphicsExtractor g, DeltaTracker delta) {
        Knives.apply();
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        DiamondHud.render(g, delta);
        drawLeftGlove(g, mc);
        InfoHud.render(g, mc);
    }
}
