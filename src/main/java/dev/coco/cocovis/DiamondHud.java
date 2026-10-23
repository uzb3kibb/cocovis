package dev.coco.cocovis;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.GuiGraphics; // Или DrawContext в зависимости от версии
import net.minecraft.client.util.math.MatrixStack;

public class DiamondHud {
    // Позиция и масштаб
    public static float x = 100.0f;
    public static float y = 100.0f;
    public static float scale = 1.0f;

    // Границы для кликов/проверок
    public static int boundsX = (int) x;
    public static int boundsY = (int) y;
    public static int boundsW = (int) (120 * scale);
    public static int boundsH = (int) (50 * scale);

    private static boolean isDragging = false;
    private static double dragX, dragY;

    // Отрисовка виджета
    public static void drawPanel(GuiGraphics context, MinecraftClient client) {
        boundsX = (int) x;
        boundsY = (int) y;
        boundsW = (int) (120 * scale);
        boundsH = (int) (50 * scale);

        // Пример отрисовки фона и текста
        context.fill(boundsX, boundsY, boundsX + boundsW, boundsY + boundsH, 0x80000000);
        context.drawText(client.textRenderer, "Алмазы", boundsX + 6, boundsY + 6, 0xFFFFFF, true);
    }

    // Логика перетаскивания мышей через события
    public static void onMouseDown(double mouseX, double mouseY, int button) {
        if (button == 0) {
            if (mouseX >= boundsX && mouseX <= boundsX + boundsW && mouseY >= boundsY && mouseY <= boundsY + boundsH) {
                isDragging = true;
                dragX = mouseX - x;
                dragY = mouseY - y;
            }
        }
    }

    public static void onMouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (isDragging) {
            x = (float) (mouseX - dragX);
            y = (float) (mouseY - dragY);
        }
    }

    public static void onMouseUp(int button) {
        if (button == 0) {
            isDragging = false;
        }
    }

    // Изменение размера колесиком
    public static void onMouseScrolled(double amount) {
        scale += (amount > 0 ? 0.1f : -0.1f);
        if (scale < 0.5f) scale = 0.5f;
        if (scale > 3.0f) scale = 3.0f;
    }
}
