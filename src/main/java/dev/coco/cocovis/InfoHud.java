package dev.coco.cocovis;

import dev.coco.cocovis.gui.Ui;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Info pills: client name, nick, FPS, ping / coordinates, speed. */
public final class InfoHud {
    public static int boundsX, boundsY, boundsW, boundsH;

    private static final int PILL_H = 16;
    private static final int GAP = 4;
    private static final int BG = 0xCC1B1C21;
    private static final int TEXT = 0xFFF1F2F6;
    private static final int BLUE = 0xFF8DA2FB;
    private static final int OK = 0xFF55D6A0;
    private static final int WARN = 0xFFF2C94C;
    private static final int BAD = 0xFFE8606E;

    private enum Icon { LOGO, USER, FPS, PING, PIN, SPEED }

    private record Pill(Icon icon, String text, int iconColor) {}

    private static String version;
    private static float speedSmooth;

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

    private static int pingColor(int ms) {
        return ms < 80 ? OK : ms < 160 ? WARN : BAD;
    }

    public static void draw(GuiGraphicsExtractor g, Minecraft mc, boolean preview) {
        CocoVisConfig cfg = CocoVisConfig.get();
        if (mc.player == null) {
            return;
        }
        int ping = 0;
        ClientPacketListener conn = mc.getConnection();
        if (conn != null) {
            PlayerInfo info = conn.getPlayerInfo(mc.player.getUUID());
            if (info != null) {
                ping = info.getLatency();
            }
        }
        Vec3 v = mc.player.getDeltaMovement();
        float bps = preview ? 7.1f : (float) (Math.sqrt(v.x * v.x + v.z * v.z) * 20.0);
        speedSmooth += (bps - speedSmooth) * 0.12f;

        List<Pill> row1 = new ArrayList<>();
        List<Pill> row2 = new ArrayList<>();
        if (cfg.showWatermark || preview) {
            row1.add(new Pill(Icon.LOGO, "CocoVis", BLUE));
        }
        if (cfg.showNick || preview) {
            row1.add(new Pill(Icon.USER, mc.player.getName().getString(), 0xFF3A3F4B));
        }
        if (cfg.showFps || preview) {
            row1.add(new Pill(Icon.FPS, mc.getFps() + "fps", BLUE));
        }
        if (cfg.showPing || preview) {
            row1.add(new Pill(Icon.PING, ping + "мс", pingColor(ping)));
        }
        if (cfg.showCoords || preview) {
            row2.add(new Pill(Icon.PIN, "x" + (int) Math.floor(mc.player.getX())
                    + " y" + (int) Math.floor(mc.player.getY())
                    + " z" + (int) Math.floor(mc.player.getZ()), BLUE));
        }
        if (cfg.showSpeed || preview) {
            row2.add(new Pill(Icon.SPEED, String.format(Locale.ROOT, "%.1fbps", preview ? 7.1f : speedSmooth), BLUE));
        }
        if (row1.isEmpty() && row2.isEmpty()) {
            boundsW = 0;
            boundsH = 0;
            return;
        }

        Font font = mc.font;
        float s = cfg.infoScale <= 0 ? 1.0f : cfg.infoScale;
        g.pose().pushMatrix();
        g.pose().translate((float) cfg.infoX, (float) cfg.infoY);
        g.pose().scale(s, s);
        int w1 = drawRow(g, font, row1, 0);
        int w2 = drawRow(g, font, row2, row1.isEmpty() ? 0 : PILL_H + GAP);
        g.pose().popMatrix();

        int rows = (row1.isEmpty() ? 0 : 1) + (row2.isEmpty() ? 0 : 1);
        boundsX = cfg.infoX;
        boundsY = cfg.infoY;
        boundsW = Math.round(Math.max(w1, w2) * s);
        boundsH = Math.round((rows * PILL_H + (rows - 1) * GAP) * s);
    }

    private static int drawRow(GuiGraphicsExtractor g, Font font, List<Pill> row, int y) {
        int x = 0;
        for (Pill p : row) {
            int textW = font.width(p.text());
            int w = 6 + 9 + 4 + textW + 7;
            Ui.roundFill(g, x, y, w, PILL_H, PILL_H / 2, BG);
            drawIcon(g, font, p, x + 6, y + (PILL_H - 9) / 2);
            g.text(font, p.text(), x + 6 + 9 + 4, y + (PILL_H - 8) / 2 + 1, TEXT, false);
            x += w + GAP;
        }
        return Math.max(0, x - GAP);
    }

    /** 9x9 icons drawn from rectangles. */
    private static void drawIcon(GuiGraphicsExtractor g, Font font, Pill p, int x, int y) {
        int c = p.iconColor();
        switch (p.icon()) {
            case LOGO -> g.text(font, "❖", x, y, c, false);
            case USER -> {
                Ui.roundFill(g, x, y, 9, 9, 4, 0xFF2C3039);
                Ui.roundFill(g, x + 3, y + 1, 3, 3, 1, 0xFF8D93A0);
                Ui.roundFill(g, x + 2, y + 5, 5, 3, 1, 0xFF8D93A0);
            }
            case FPS -> {
                Ui.roundOutline(g, x, y, 9, 7, 1, c);
                g.fill(x + 3, y + 8, x + 6, y + 9, c);
            }
            case PING -> {
                int dim = 0xFF4A4E58;
                g.fill(x, y + 6, x + 2, y + 9, c);
                g.fill(x + 3, y + 3, x + 5, y + 9, c);
                g.fill(x + 6, y, x + 8, y + 9, dim);
            }
            case PIN -> {
                Ui.roundFill(g, x + 1, y, 7, 7, 3, c);
                Ui.roundFill(g, x + 3, y + 2, 3, 3, 1, 0xFF1B1C21);
                g.fill(x + 4, y + 7, x + 5, y + 9, c);
            }
            case SPEED -> g.text(font, "»", x, y - 1, c, false);
        }
    }
}
