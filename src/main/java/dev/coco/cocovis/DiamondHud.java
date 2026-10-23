package dev.coco.cocovis;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;

public class DiamondHud {
    public static int x = 100;
    public static int y = 100;
    public static float scale = 1.0f;
    public static int width = 120;
    public static int height = 50;

    public static boolean dragging = false;
    private static double dragOffsetX = 0;
    private static double dragOffsetY = 0;

    public static void render(DrawContext context, float delta) {
        MinecraftClient client = MinecraftClient.getInstance();
        int scaledWidth = (int) (width * scale);
        int scaledHeight = (int) (height * scale);

        // Отрисовка фона и текста виджета
        context.fill(x, y, x + scaledWidth, y + scaledHeight, 0x80000000);
        context.drawText(client.textRenderer, "Алмазы", x + 6, y + 6, 0xFFFFFF, true);
    }

    public static boolean isHovered(double mouseX, double mouseY) {
        int scaledWidth = (int) (width * scale);
        int scaledHeight = (int) (height * scale);
        return mouseX >= x && mouseX <= x + scaledWidth && mouseY >= y && mouseY <= y + scaledHeight;
    }

    public static void mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && isHovered(mouseX, mouseY)) {
            dragging = true;
            dragOffsetX = mouseX - x;
            dragOffsetY = mouseY - y;
        }
    }

    public static void mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (dragging) {
            x = (int) (mouseX - dragOffsetX);
            y = (int) (mouseY - dragOffsetY);
        }
    }

    public static void mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0) {
            dragging = false;
        }
    }

    public static void mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (isHovered(mouseX, mouseY)) {
            scale += verticalAmount * 0.1f;
            if (scale < 0.5f) scale = 0.5f;
            if (scale > 3.0f) scale = 3.0f;
        }
    }
}
