package dev.coco.cocovis.gui;

import com.mojang.blaze3d.platform.InputConstants;
import dev.coco.cocovis.DiamondConfig;
import dev.coco.cocovis.DiamondHud;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

/** Редактор панели алмазов: ЛКМ + тяни — двигать, колесо — размер, R — сброс. */
public class HudEditor extends Screen {
    private boolean dragging;

    public HudEditor() {
        super(Component.literal("Diamond HUD"));
    }

    @Override
    protected void init() {
        int cx = width / 2;
        addRenderableWidget(Button.builder(Component.literal("Сбросить"), b -> reset())
                .bounds(cx - 102, height - 30, 100, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Готово"), b -> onClose())
                .bounds(cx + 2, height - 30, 100, 20).build());
    }

    private static void reset() {
        DiamondConfig cfg = DiamondConfig.get();
        cfg.hudX = 6;
        cfg.hudY = 6;
        cfg.hudScale = 1.0f;
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (super.mouseClicked(event, doubleClick)) return true;
        if (event.button() == 0
                && event.x() >= DiamondHud.boundsX && event.x() <= DiamondHud.boundsX + DiamondHud.boundsW
                && event.y() >= DiamondHud.boundsY && event.y() <= DiamondHud.boundsY + DiamondHud.boundsH) {
            dragging = true;
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
        if (dragging) {
            DiamondConfig cfg = DiamondConfig.get();
            cfg.hudX = (int) Math.max(0, Math.min(width - 10, cfg.hudX + dragX));
            cfg.hudY = (int) Math.max(0, Math.min(height - 10, cfg.hudY + dragY));
            return true;
        }
        return super.mouseDragged(event, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        dragging = false;
        return super.mouseReleased(event);
    }

    @Override
    public boolean mouseScrolled(double x, double y, double scrollX, double scrollY) {
        DiamondConfig cfg = DiamondConfig.get();
        cfg.hudScale = Math.max(0.5f, Math.min(3.0f, cfg.hudScale * (1.0f + 0.08f * (float) scrollY)));
        return true;
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (event.key() == InputConstants.KEY_R) {
            reset();
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
        super.extractRenderState(g, mouseX, mouseY, delta);
        DiamondHud.drawPanel(g, minecraft);
        DiamondConfig cfg = DiamondConfig.get();
        String l1 = "ЛКМ + тяни: двигать панель   ·   колесо мыши: размер   ·   R: сброс   ·   Esc или «Готово»: сохранить";
        String l2 = "Размер: " + Math.round(cfg.hudScale * 100) + "%   положение: " + cfg.hudX + ", " + cfg.hudY;
        g.text(font, l1, width / 2 - font.width(l1) / 2, height - 54, 0xFFDDDDDD, true);
        g.text(font, l2, width / 2 - font.width(l2) / 2, height - 44, 0xFFAAAAAA, true);
    }

    @Override
    public void onClose() {
        DiamondConfig.save();
        super.onClose();
    }

    @Override
    public boolean isPauseScreen() { return false; }
}
