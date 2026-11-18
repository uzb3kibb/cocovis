package dev.coco.cocovis.gui;

import dev.coco.cocovis.CocoVisConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

import java.awt.Color;
import java.util.Locale;

/** Compact HSV wheel with a live HEX field. Kept separate from the main CocoVis menu. */
public final class SkyColorPickerScreen extends Screen {
    private static final int PANEL = 0xF20B0D12;
    private static final int BORDER = 0xFF292D38;
    private static final int TEXT = 0xFFE8EDF5;
    private static final int MUTED = 0xFF8994A4;
    private static final int ACCENT = 0xFF8DA2FB;

    private int mouseX;
    private int mouseY;
    private int panelX, panelY, panelW, panelH;
    private float centerX, centerY, outerR, innerR;
    private boolean draggingWheel;
    private boolean hexFocused;
    private String hex = "7B2CFF";

    public SkyColorPickerScreen() {
        super(Component.literal("CocoVis — Sky color"));
        String value = CocoVisConfig.get().skyColorHex;
        if (value != null) hex = value.replace("#", "").toUpperCase(Locale.ROOT);
    }

    private static int clamp(int v) { return Math.max(0, Math.min(255, v)); }

    private void applyHex() {
        if (hex.matches("[0-9A-Fa-f]{6}")) {
            CocoVisConfig.get().skyColorHex = "#" + hex.toUpperCase(Locale.ROOT);
            CocoVisConfig.get().skyColorMode = 5;
            CocoVisConfig.get().skyColorEnabled = true;
            CocoVisConfig.save();
        }
    }

    private void setFromWheel(double mx, double my) {
        double dx = mx - centerX;
        double dy = my - centerY;
        double distance = Math.sqrt(dx * dx + dy * dy);
        if (distance < innerR || distance > outerR) return;
        float hue = (float) ((Math.atan2(dy, dx) / (Math.PI * 2.0) + 1.0) % 1.0);
        float saturation = 1f;
        int rgb = Color.HSBtoRGB(hue, saturation, 1f) & 0xFFFFFF;
        hex = String.format(Locale.ROOT, "%06X", rgb);
        applyHex();
    }

    private void layout(int sw, int sh) {
        panelW = Math.min(270, sw - 24);
        panelH = 238;
        panelX = (sw - panelW) / 2;
        panelY = (sh - panelH) / 2;
        centerX = sw / 2f;
        centerY = panelY + 112;
        outerR = Math.min(72f, (panelW - 44) / 2f);
        innerR = 24f;
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        this.mouseX = mouseX;
        this.mouseY = mouseY;
        Minecraft mc = Minecraft.getInstance();
        Font font = mc.font;
        int sw = mc.getWindow().getGuiScaledWidth();
        int sh = mc.getWindow().getGuiScaledHeight();
        layout(sw, sh);

        g.fill(0, 0, sw, sh, 0x99000000);
        Ui.shadow(g, panelX, panelY, panelW, panelH, 9);
        Ui.roundGradient(g, panelX, panelY, panelW, panelH, 9, 0xFF171A22, PANEL);
        Ui.roundOutline(g, panelX, panelY, panelW, panelH, 9, BORDER);
        Ui.title(g, font, "Цвет неба", panelX + 16, panelY + 13, TEXT);
        Ui.text(g, font, "Выбери оттенок на круге", panelX + 16, panelY + 30, MUTED, false);

        // HSV wheel. The center is intentionally hollow; the picker marker follows the selected hue.
        int minX = (int) Math.floor(centerX - outerR);
        int maxX = (int) Math.ceil(centerX + outerR);
        int minY = (int) Math.floor(centerY - outerR);
        int maxY = (int) Math.ceil(centerY + outerR);
        for (int py = minY; py <= maxY; py++) {
            for (int px = minX; px <= maxX; px++) {
                double dx = px + 0.5 - centerX;
                double dy = py + 0.5 - centerY;
                double d = Math.sqrt(dx * dx + dy * dy);
                if (d < innerR || d > outerR) continue;
                float hue = (float) ((Math.atan2(dy, dx) / (Math.PI * 2.0) + 1.0) % 1.0);
                float sat = (float) Math.min(1.0, Math.max(0.12, d / outerR));
                int rgb = Color.HSBtoRGB(hue, sat, 1f);
                g.fill(px, py, px + 1, py + 1, 0xFF000000 | (rgb & 0xFFFFFF));
            }
        }
        Ui.ring(g, centerX - outerR - 1, centerY - outerR - 1, (outerR + 1) * 2, 0xFF6C7280);
        Ui.circle(g, centerX - innerR, centerY - innerR, innerR * 2, 0xFF111318);
        Ui.ring(g, centerX - innerR, centerY - innerR, innerR * 2, 0xFF737B8B);

        int rgb = 0x7B2CFF;
        try { rgb = Integer.parseInt(hex, 16); } catch (RuntimeException ignored) {}
        float[] hsv = Color.RGBtoHSB((rgb >> 16) & 255, (rgb >> 8) & 255, rgb & 255, null);
        double angle = hsv[0] * Math.PI * 2.0;
        float markerR = (innerR + outerR) * 0.5f;
        float markerX = centerX + (float) Math.cos(angle) * markerR;
        float markerY = centerY + (float) Math.sin(angle) * markerR;
        Ui.circle(g, markerX - 5, markerY - 5, 10, 0xFF111318);
        Ui.ring(g, markerX - 5, markerY - 5, 10, 0xFFFFFFFF);

        float fieldY = panelY + panelH - 43;
        Ui.text(g, font, "HEX", panelX + 16, fieldY + 9, MUTED, false);
        float fieldX = panelX + 53;
        float fieldW = panelW - 69;
        Ui.roundFill(g, fieldX, fieldY, fieldW, 25, 5, 0xFF0C0E13);
        Ui.roundOutline(g, fieldX, fieldY, fieldW, 25, 5, hexFocused ? ACCENT : BORDER);
        Ui.text(g, font, "#" + hex, fieldX + 9, fieldY + 8, TEXT, false);
        Ui.roundFill(g, panelX + 16, panelY + panelH - 10, 12, 3, 2, rgb | 0xFF000000);
        Ui.text(g, font, "Изменяется сразу", panelX + 34, panelY + panelH - 14, MUTED, false);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (Ui.inside(mouseX, mouseY, centerX - outerR - 4, centerY - outerR - 4,
                outerR * 2 + 8, outerR * 2 + 8)) {
            draggingWheel = true;
            hexFocused = false;
            setFromWheel(mouseX, mouseY);
            return true;
        }
        float fieldY = panelY + panelH - 43;
        float fieldX = panelX + 53;
        if (Ui.inside(mouseX, mouseY, fieldX, fieldY, panelW - 69, 25)) {
            hexFocused = true;
            return true;
        }
        if (!Ui.inside(mouseX, mouseY, panelX, panelY, panelW, panelH)) {
            Minecraft.getInstance().gui.setScreen(new VisualsScreen());
            return true;
        }
        return true;
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        draggingWheel = false;
        return true;
    }


    @Override
    public boolean charTyped(CharacterEvent event) {
        if (!hexFocused || !event.isAllowedChatCharacter()) return super.charTyped(event);
        String s = event.codepointAsString().toUpperCase(Locale.ROOT);
        if (s.matches("[0-9A-F]") && hex.length() < 6) {
            hex += s;
            applyHex();
            return true;
        }
        return false;
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (event.key() == 256) {
            Minecraft.getInstance().gui.setScreen(new VisualsScreen());
            return true;
        }
        if (hexFocused && event.key() == 259 && !hex.isEmpty()) {
            hex = hex.substring(0, hex.length() - 1);
            if (hex.length() == 6) applyHex();
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
        if (draggingWheel) {
            setFromWheel(mouseX, mouseY);
            return true;
        }
        return super.mouseDragged(event, dx, dy);
    }

    @Override
    public boolean isPauseScreen() { return false; }
}
