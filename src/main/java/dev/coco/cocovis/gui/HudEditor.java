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
    private boolean dragging, dragSeen;
    private double lastX, lastY, posX, posY;
    private int nClick, nDrag, nMove;

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

    private void moveTo(double x, double y) {
        DiamondConfig cfg = DiamondConfig.get();
        posX += x - lastX;
        posY += y - lastY;
        lastX = x;
        lastY = y;
        posX = Math.max(0, Math.min(width - 20, posX));
        posY = Math.max(0, Math.min(height - 20, posY));
        cfg.hudX = (int) Math.round(posX);
        cfg.hudY = (int) Math.round(posY);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        nClick++;
        if (event.button() == 0
                && Ui.inside(event.x(), event.y(), DiamondHud.boundsX, DiamondHud.boundsY, DiamondHud.boundsW, DiamondHud.boundsH)) {
            DiamondConfig cfg = DiamondConfig.get();
            dragging = true;
            dragSeen = false;
            lastX = event.x();
            lastY = event.y();
            posX = cfg.hudX;
            posY = cfg.hudY;
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
        nDrag++;
        if (dragging) {
            dragSeen = true;
            moveTo(event.x(), event.y());
            return true;
        }
        return super.mouseDragged(event, dragX, dragY);
    }

    @Override
    public void mouseMoved(double x, double y) {
        nMove++;
        // запасной путь: если события перетаскивания не приходят — двигаем по движению мыши
        if (dragging && !dragSeen) moveTo(x, y);
        super.mouseMoved(x, y);
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
        g.text(font, "отладка: клики " + nClick + ", тяга " + nDrag + ", движение " + nMove, 6, height - 12, 0xFF666666, false);
    }

    @Override
    public void onClose() {
        DiamondConfig.save();
        super.onClose();
    }

    @Override
    public boolean isPauseScreen() { return false; }
}
