package dev.coco.cocovis;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.Locale;

/** Dark HUD panel with a diamond icon and the diamond count. */
public final class DiamondHud {
    private static final int BG = 0xC0101216;
    private static final int ACCENT = 0xFF3DE0E0;
    private static final int TEXT = 0xFFE8FFFF;
    private static ItemStack icon;

    private DiamondHud() {}

    public static void render(GuiGraphicsExtractor g) {
        CocoVisConfig cfg = CocoVisConfig.get();
        Minecraft mc = Minecraft.getInstance();
        if (!cfg.showDiamondHud || mc.player == null) {
            return;
        }
        if (icon == null) {
            icon = new ItemStack(Items.DIAMOND);
        }
        String text = String.format(Locale.ROOT, "%,d", DiamondCount.inInventory(mc.player));
        int w = 28 + mc.font.width(text) + 8;
        int h = 22;
        float s = cfg.hudScale <= 0 ? 1.0f : cfg.hudScale;

        g.pose().pushMatrix();
        g.pose().translate((float) cfg.hudX, (float) cfg.hudY);
        g.pose().scale(s, s);
        g.fill(0, 0, w, h, BG);
        g.fill(0, 0, 2, h, ACCENT);
        g.item(icon, 6, 3);
        g.text(mc.font, text, 28, 7, TEXT, true);
        g.pose().popMatrix();
    }
}
