package dev.coco.cocovis.gui;

import dev.coco.cocovis.DiamondHud;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.LiteralText;

public class HudEditor extends Screen {

    public HudEditor() {
        super(new LiteralText("HUD Editor"));
    }

    @딜 true // если нужна пауза, но оставим false
    @Override
    public boolean shouldPause() {
        return false;
    }

    @Override
    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        this.renderBackground(matrices);
        
        // Рендерим панель
        DiamondHud.drawPanel(matrices, this.client);
        
        drawCenteredString(matrices, this.textRenderer, "Редактор HUD (RShift для выхода)", this.width / 2, 20, 0xFFFFFF);
        
        super.render(matrices, mouseX, mouseY, delta);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        DiamondHud.onMouseDown(mouseX, mouseY, button);
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        DiamondHud.onMouseDragged(mouseX, mouseY, button, deltaX, deltaY);
        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        DiamondHud.onMouseUp(button);
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
        if (mouseX >= DiamondHud.boundsX && mouseX <= DiamondHud.boundsX + DiamondHud.boundsW &&
            mouseY >= DiamondHud.boundsY && mouseY <= DiamondHud.boundsY + DiamondHud.boundsH) {
            DiamondHud.onMouseScrolled(amount);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, amount);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        // 344 - RShift, 256 - ESC
        if (keyCode == 344 || keyCode == 256) {
            this.close();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }
}
