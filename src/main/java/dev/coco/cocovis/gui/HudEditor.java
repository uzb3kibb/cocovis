package dev.coco.cocovis.gui;

import dev.coco.cocovis.DiamondHud;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;

public class HudEditor extends Screen {

    private final DiamondHud diamondHud;

    public HudEditor(DiamondHud diamondHud) {
        super(Text.literal("HUD Editor"));
        this.diamondHud = diamondHud;
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        
        // Рендерим сам виджет в режиме редактирования
        diamondHud.render(context, true);

        context.drawCenteredTextWithShadow(
            this.textRenderer, 
            "Режим редактора HUD. Перетаскивайте мышкой.", 
            this.width / 2, 
            20, 
            0xFFFFFF
        );
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        // 344 - правый Shift (RShift), 256 - ESC
        if (keyCode == 344 || keyCode == 256) {
            this.close();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }
}
