package dev.coco.cocovis.gui;

import dev.coco.cocovis.DiamondHud;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

public class HudEditor extends Screen {

    public HudEditor() {
        super(Text.literal("HUD Editor"));
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        
        // Рендерим панель в редакторе
        DiamondHud.drawPanel(context, this.client);
        
        context.drawCenteredTextWithShadow(
            this.textRenderer, 
            "Редактор HUD (RShift / ESC для выхода)", 
            this.width / 2, 
            20, 
            0xFFFFFF
        );
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
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (mouseX >= DiamondHud.boundsX && mouseX <= DiamondHud.boundsX + DiamondHud.boundsW &&
            mouseY >= DiamondHud.boundsY && mouseY <= DiamondHud.boundsY + DiamondHud.boundsH) {
            DiamondHud.onMouseScrolled(verticalAmount);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
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
