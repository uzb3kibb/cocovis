package dev.coco.cocovis;

import dev.coco.cocovis.gui.Ui;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;

import java.util.ArrayList;
import java.util.List;

/** Small info panel: watermark, coordinates, FPS. */
public final class InfoHud {
    public static int boundsX, boundsY, boundsW, boundsH;

    private static String version;

    private InfoHud() {}

    private static String version() {
        if (version == null) {
            version = FabricLoader.getInstance().getModContainer(CocoVisClient.MOD_ID)
                    .map(c -> c.getMetadata().getVersion().getFriendlyString())
                    .orElse("?");
        }
        return version;
    }

    public static void render(GuiGraphicsExtractor g, Minecraft mc) {
        draw(g, mc, false);
    }

    public static void draw(GuiGraphicsExtractor g, Minecraft mc, boolean preview) {
        CocoVisConfig cfg = CocoVisConfig.get();
        if (mc.player == null) {
            return;
        }
        List<String> lines = new ArrayList<>();
        if (cfg.showWatermark || preview) {
            lines.add("CocoVis " + version());
        }
        if (cfg.showCoords || preview) {
            lines.add("XYZ: " + (int) Math.floor(mc.player.getX()) + " " + (int) Math.floor(mc.player.getY())
                    + " " + (int) Math.floor(mc.player.getZ()));
        }
        if (cfg.showFps || preview) {
            lines.add("FPS: " + mc.getFps());
        }
        if (lines.isEmpty()) {
            boundsW = 0;
            boundsH = 0;
            return;
        }
        Font font = mc.font;
        int w = 0;
        for (String l : lines) {
            w = Math.max(w, font.width(l));
        }
        w += 16;
        int h = 8 + lines.size() * 11;
        float s = cfg.infoScale <= 0 ? 1.0f : cfg.infoScale;

        g.pose().pushMatrix();
        g.pose().translate((float) cfg.infoX, (float) cfg.infoY);
        g.pose().scale(s, s);
        Ui.panel(g, 0, 0, w, h);
        g.fill(1, 4, 3, h - 4, Ui.ACCENT);
        for (int i = 0; i < lines.size(); i++) {
            int color = i == 0 && (cfg.showWatermark || preview) ? Ui.ACCENT : Ui.TEXT;
            g.text(font, lines.get(i), 9, 5 + i * 11, color, true);
        }
        g.pose().popMatrix();

        boundsX = cfg.infoX;
        boundsY = cfg.infoY;
        boundsW = Math.round(w * s);
        boundsH = Math.round(h * s);
    }
}
