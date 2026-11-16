package dev.coco.cocovis.gui;

import dev.coco.cocovis.CocoVisConfig;
import dev.coco.cocovis.DiamondHud;
import dev.coco.cocovis.InfoHud;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

/** Drag the diamond panel, info panel and toasts with the mouse; wheel over a panel = size. */
public class HudEditor extends Screen {
    private static final int DIAMOND = 0;
    private static final int INFO = 1;
    private static final int TOAST = 2;
    private static final int BTN_W = 90;
    private static final int BTN_H = 18;

    private boolean dragging;
    private int target = -1;
    private int mX, mY;
    private double lastX, lastY, posX, posY;

    public HudEditor() {
        super(Component.literal("CocoVis HUD"));
    }

    private int sw() {
        return Minecraft.getInstance().getWindow().getGuiScaledWidth();
    }

    private int sh() {
        return Minecraft.getInstance().getWindow().getGuiScaledHeight();
    }

    private int btnY() {
        return sh() - 40;
    }

    private int resetX() {
        return sw() / 2 - BTN_W - 8;
    }

    private int doneX() {
        return sw() / 2 + 8;
    }

    private void reset() {
        CocoVisConfig.get().resetPositions();
        CocoVisConfig.save();
    }

    private int panelAt(int x, int y) {
        if (Ui.inside(x, y, DiamondHud.toastBX, DiamondHud.toastBY, DiamondHud.toastBW, DiamondHud.toastBH)) {
            return TOAST;
        }
        if (Ui.inside(x, y, DiamondHud.boundsX, DiamondHud.boundsY, DiamondHud.boundsW, DiamondHud.boundsH)) {
            return DIAMOND;
        }
        if (InfoHud.boundsW > 0
                && Ui.inside(x, y, InfoHud.boundsX, InfoHud.boundsY, InfoHud.boundsW, InfoHud.boundsH)) {
            return INFO;
        }
        return -1;
    }

    private void follow() {
        if (!dragging) {
            return;
        }
        posX += mX - lastX;
        posY += mY - lastY;
        lastX = mX;
        lastY = mY;
        int x = (int) Math.max(0, Math.min(sw() - 20, Math.round(posX)));
        int y = (int) Math.max(0, Math.min(sh() - 20, Math.round(posY)));
        CocoVisConfig cfg = CocoVisConfig.get();
        if (target == DIAMOND) {
            cfg.hudX = x;
            cfg.hudY = y;
        } else if (target == INFO) {
            cfg.infoX = x;
            cfg.infoY = y;
        } else if (target == TOAST) {
            cfg.toastX = x;
            cfg.toastY = y;
        }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (Ui.inside(mX, mY, resetX(), btnY(), BTN_W, BTN_H)) {
            reset();
            return true;
        }
        if (Ui.inside(mX, mY, doneX(), btnY(), BTN_W, BTN_H)) {
            onClose();
            return true;
        }
        target = panelAt(mX, mY);
        if (target >= 0) {
            CocoVisConfig cfg = CocoVisConfig.get();
            dragging = true;
            lastX = mX;
            lastY = mY;
            if (target == DIAMOND) {
                posX = cfg.hudX;
                posY = cfg.hudY;
            } else if (target == INFO) {
                posX = cfg.infoX;
                posY = cfg.infoY;
            } else {
                posX = cfg.toastX;
                posY = cfg.toastY;
            }
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (dragging) {
            dragging = false;
            CocoVisConfig.save();
            return true;
        }
        return super.mouseReleased(event);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        int panel = panelAt(mX, mY);
        CocoVisConfig cfg = CocoVisConfig.get();
        float delta = scrollY > 0 ? 0.1f : -0.1f;
        if (panel == DIAMOND) {
            cfg.hudScale = Math.max(0.5f, Math.min(2.5f, Math.round((cfg.hudScale + delta) * 10f) / 10f));
            CocoVisConfig.save();
            return true;
        }
        if (panel == INFO) {
            cfg.infoScale = Math.max(0.5f, Math.min(2.5f, Math.round((cfg.infoScale + delta) * 10f) / 10f));
            CocoVisConfig.save();
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (event.key() == 256) {
            onClose();
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        mX = mouseX;
        mY = mouseY;
        follow();
        Minecraft mc = Minecraft.getInstance();
        Font font = mc.font;
        g.fill(0, 0, sw(), sh(), Ui.fade(0xFF000000, 0.4f));

        InfoHud.draw(g, mc, true);
        DiamondHud.draw(g, mc, true);
        DiamondHud.drawToastPreview(g, mc);

        int hover = dragging ? target : panelAt(mX, mY);
        outline(g, font, "Информация", InfoHud.boundsX, InfoHud.boundsY, InfoHud.boundsW, InfoHud.boundsH, hover == INFO);
        outline(g, font, "Алмазы", DiamondHud.boundsX, DiamondHud.boundsY, DiamondHud.boundsW, DiamondHud.boundsH, hover == DIAMOND);
        outline(g, font, "Уведомления", DiamondHud.toastBX, DiamondHud.toastBY, DiamondHud.toastBW, DiamondHud.toastBH, hover == TOAST);

        button(g, font, "Сбросить", resetX(), btnY(), Ui.inside(mX, mY, resetX(), btnY(), BTN_W, BTN_H));
        button(g, font, "Готово", doneX(), btnY(), Ui.inside(mX, mY, doneX(), btnY(), BTN_W, BTN_H));

        String hint = "Тяни панели мышью  ·  колесо над панелью: размер  ·  Esc: сохранить и выйти";
        g.text(font, hint, (sw() - font.width(hint)) / 2, btnY() - 14, Ui.MUTED, true);
    }

    private static void outline(GuiGraphicsExtractor g, Font font, String label, int x, int y, int w, int h, boolean active) {
        if (w <= 0 || h <= 0) {
            return;
        }
        int color = active ? Ui.ACCENT : Ui.fade(Ui.MUTED, 0.6f);
        Ui.roundOutline(g, x - 2, y - 2, w + 4, h + 4, 5, color);
        g.text(font, label, x, y - 12, color, true);
    }

    private static void button(GuiGraphicsExtractor g, Font font, String text, int x, int y, boolean hover) {
        Ui.roundGradient(g, x, y, BTN_W, BTN_H, 4,
                hover ? Ui.CARD_HOT_TOP : Ui.CARD_TOP, hover ? Ui.CARD_HOT_BOT : Ui.CARD_BOT);
        Ui.roundOutline(g, x, y, BTN_W, BTN_H, 4, hover ? Ui.ACCENT : Ui.BORDER);
        g.text(font, text, x + (BTN_W - font.width(text)) / 2, y + 5, Ui.TEXT, true);
    }

    @Override
    public void onClose() {
        CocoVisConfig.save();
        Minecraft.getInstance().gui.setScreen(new VisualsScreen());
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
