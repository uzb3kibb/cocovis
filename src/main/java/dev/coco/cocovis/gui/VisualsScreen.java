package dev.coco.cocovis.gui;

import dev.coco.cocovis.module.Module;
import dev.coco.cocovis.module.ModuleManager;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class VisualsScreen extends Screen {
    private static final int PANEL_W = 190;
    private static final int ROW_H = 24;

    public VisualsScreen() {
        super(Component.literal("CocoVis"));
    }

    @Override
    protected void init() {
        int x = (width - PANEL_W) / 2 + 8;
        int y = (height - panelHeight()) / 2 + 26;
        for (Module m : ModuleManager.ALL) {
            addRenderableWidget(Button.builder(label(m), btn -> {
                m.toggle();
                btn.setMessage(label(m));
            }).bounds(x, y, PANEL_W - 16, 20).build());
            y += ROW_H;
        }
        addRenderableWidget(Button.builder(Component.literal("Настроить панель алмазов..."),
                btn -> minecraft.gui.setScreen(new HudEditor()))
                .bounds(x, y + 4, PANEL_W - 16, 20).build());
    }

    private static Component label(Module m) {
        return Component.literal(m.name + ": " + (m.isEnabled() ? "ON" : "OFF"));
    }

    private int panelHeight() {
        return 34 + ModuleManager.ALL.size() * ROW_H + 28;
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
        int x = (width - PANEL_W) / 2;
        int y = (height - panelHeight()) / 2;
        g.fill(x, y, x + PANEL_W, y + panelHeight(), 0xDD101014);
        g.fill(x, y, x + PANEL_W, y + 2, 0xFF5CC8FF);
        g.text(font, "CocoVis", width / 2 - font.width("CocoVis") / 2, y + 9, 0xFFFFFFFF, true);
        super.extractRenderState(g, mouseX, mouseY, delta);
    }

    @Override
    public boolean isPauseScreen() { return false; }
}
