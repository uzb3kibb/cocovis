package dev.coco.cocovis.gui;

import com.mojang.blaze3d.platform.InputConstants;
import dev.coco.cocovis.DiamondConfig;
import dev.coco.cocovis.DiamondHud;
import dev.coco.cocovis.InfoHud;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

/** Редактор HUD: зажми любую панель и тяни, колесо мыши над панелью — размер, R — сброс. */
public class HudEditor extends Screen {
    private static final int DIAMOND = 0, INFO = 1;

    private boolean dragging;
    private int target = -1;
    private int mX, mY;
    private double lastX, lastY, posX, posY;

    public HudEditor() {
        super(Component.literal("HUD"));
    }

    private int btnY() { return height - 34; }
    private int resetX() { return width / 2 - 100; }
    private int doneX() { return width / 2 + 4; }

    private static void reset() {
        DiamondConfig cfg = DiamondConfig.get();
        cfg.hudX = 6;
        cfg.hudY = 6;
        cfg.hudScale = 1.0f;
        cfg.infoX = -1;
        cfg.infoY = -1;
        cfg.infoScale = 1.0f;
    }

    /** Какая панель под курсором: INFO сверху, затем DIAMOND, иначе -1. */
    private int panelAt(int mx, int my) {
        if (Ui.inside(mx, my, InfoHud.boundsX, InfoHud.boundsY, InfoHud.boundsW, InfoHud.boundsH)) return INFO;
        if (Ui.inside(mx, my, DiamondHud.boundsX, DiamondHud.boundsY, DiamondHud.boundsW, DiamondHud.boundsH)) return DIAMOND;
        return -1;
    }

    private void follow() {
        DiamondConfig cfg = DiamondConfig.get();
        posX += mX - lastX;
        posY += mY - lastY;
        lastX = mX;
        lastY = mY;
        posX = Math.max(0, Math.min(width - 20, posX));
        posY = Math.max(0, Math.min(height - 20, posY));
        if (target == DIAMOND) {
            cfg.hudX = (int) Math.round(posX);
            cfg.hudY = (int) Math.round(posY);
        } else if (target == INFO) {
            cfg.infoX = (int) Math.round(posX);
            cfg.infoY = (int) Math.round(posY);
        }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        // кнопку мыши не проверяем: в 26.3 её коды изменились (SDL)
        if (Ui.inside(mX, mY, resetX(), btnY(), 96, 20)) {
            reset();
            return true;
        }
        if (Ui.inside(mX, mY, doneX(), btnY(), 96, 20)) {
            onClose();
            return true;
        }
        int p = panelAt(mX, mY);
        if (p >= 0) {
            DiamondConfig cfg = DiamondConfig.get();
            target = p;
            dragging = true;
            lastX = mX;
            lastY = mY;
            if (p == DIAMOND) {
                posX = cfg.hudX;
                posY = cfg.hudY;
            } else {
                // из «автоматического» положения переходим в явное
                posX = InfoHud.boundsX;
                posY = InfoHud.boundsY;
                cfg.infoX = InfoHud.boundsX;
                cfg.infoY = InfoHud.boundsY;
            }
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        dragging = false;
        return super.mouseReleased(event);
    }

    @Override
    public boolean mouseScrolled(double x, double y, double scrollX, double scrollY) {
        DiamondConfig cfg = DiamondConfig.get();
        int p = panelAt(mX, mY);
        if (p < 0) p = target < 0 ? DIAMOND : target;
        float k = 1.0f + 0.08f * (float) scrollY;
        if (p == DIAMOND) cfg.hudScale = Math.max(0.5f, Math.min(3.0f, cfg.hudScale * k));
        else cfg.infoScale = Math.max(0.5f, Math.min(3.0f, cfg.infoScale * k));
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
        mX = mouseX;
        mY = mouseY;
        if (dragging) follow();

        Font f = font;
        g.fill(0, 0, width, height, 0x66000000);

        DiamondHud.drawPanel(g, minecraft);
        InfoHud.draw(g, minecraft, true);

        // подсветка: под курсором / перетаскиваемая — яркая, остальные — тонкая рамка
        int hover = dragging ? target : panelAt(mouseX, mouseY);
        outline(g, DiamondHud.boundsX, DiamondHud.boundsY, DiamondHud.boundsW, DiamondHud.boundsH, hover == DIAMOND);
        outline(g, InfoHud.boundsX, InfoHud.boundsY, InfoHud.boundsW, InfoHud.boundsH, hover == INFO);

        // кнопки
        button(g, f, "Сбросить", resetX(), btnY(), Ui.inside(mouseX, mouseY, resetX(), btnY(), 96, 20), false);
        button(g, f, "Готово", doneX(), btnY(), Ui.inside(mouseX, mouseY, doneX(), btnY(), 96, 20), true);

        DiamondConfig cfg = DiamondConfig.get();
        String l1 = "Зажми панель и тяни  ·  колесо над панелью: размер  ·  R: сброс  ·  Esc: сохранить и выйти";
        String l2 = "Алмазы " + Math.round(cfg.hudScale * 100) + "%   ·   Инфо-панель " + Math.round(cfg.infoScale * 100) + "%";
        g.text(f, l1, width / 2 - f.width(l1) / 2, btnY() - 24, Ui.MUTED, true);
        g.text(f, l2, width / 2 - f.width(l2) / 2, btnY() - 12, Ui.DIM, false);
    }

    private static void outline(GuiGraphicsExtractor g, int x, int y, int w, int h, boolean hot) {
        Ui.roundOutline(g, x - 2, y - 2, w + 4, h + 4, 10, hot ? Ui.ACCENT : 0x40FFFFFF);
    }

    private static void button(GuiGraphicsExtractor g, Font f, String text, int x, int y, boolean hover, boolean primary) {
        int top = primary ? (hover ? Ui.ACCENT : Ui.mix(Ui.ACCENT, 0xFF000000, 0.15f)) : (hover ? Ui.CARD_HOT_TOP : Ui.CARD_TOP);
        int bot = primary ? (hover ? Ui.ACCENT2 : Ui.mix(Ui.ACCENT2, 0xFF000000, 0.15f)) : (hover ? Ui.CARD_HOT_BOT : Ui.CARD_BOT);
        Ui.roundGradient(g, x, y, 96, 20, 7, top, bot);
        Ui.roundOutline(g, x, y, 96, 20, 7, Ui.BORDER);
        g.text(f, text, x + 48 - f.width(text) / 2, y + 6, primary ? 0xFF0B0F16 : Ui.TEXT, false);
    }

    @Override
    public void onClose() {
        DiamondConfig.save();
        super.onClose();
    }

    @Override
    public boolean isPauseScreen() { return false; }
}
