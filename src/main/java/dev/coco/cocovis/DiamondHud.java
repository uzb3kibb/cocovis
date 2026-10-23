package dev.coco.cocovis;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import org.lwjgl.glfw.GLFW;

public class DiamondHud {
    public float x = 100.0f;
    public float y = 100.0f;
    public float scale = 1.0f;

    private boolean isDragging = false;
    private double dragX, dragY;
    private boolean wasMouseDown = false;

    public void render(DrawContext context, boolean isEditing) {
        MinecraftClient client = MinecraftClient.getInstance();
        
        // Получаем актуальные координаты мыши и состояние кнопки напрямую через GLFW/окно
        long window = client.getWindow().getHandle();
        double mouseX = client.mouse.getX() * (double) client.getWindow().getScaledWidth() / (double) client.getWindow().getWidth();
        double mouseY = client.getWindow().getScaledHeight() - (client.mouse.getY() * (double) client.getWindow().getScaledHeight() / (double) client.getWindow().getHeight()) - 1;

        boolean isMouseDown = GLFW.glfwGetMouseButton(window, GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS;

        float width = 120 * scale;
        float height = 50 * scale;

        boolean hovered = mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;

        if (isEditing) {
            if (isMouseDown) {
                if (!wasMouseDown && hovered) {
                    isDragging = true;
                    dragX = mouseX - x;
                    dragY = mouseY - y;
                } else if (isDragging) {
                    x = (float) (mouseX - dragX);
                    y = (float) (mouseY - dragY);
                }
            } else {
                isDragging = false;
            }
        } else {
            isDragging = false;
        }

        wasMouseDown = isMouseDown;

        // Отрисовка виджета
        context.fill((int) x, (int) y, (int) (x + width), (int) (y + height), 0x80000000);
        context.drawText(client.textRenderer, "Алмазы", (int) (x + 6), (int) (y + 6), 0xFFFFFF, true);
        
        if (isEditing && hovered) {
            context.drawBorder((int) x, (int) y, (int) width, (int) height, 0xFFFF00FF);
        }
    }
}
