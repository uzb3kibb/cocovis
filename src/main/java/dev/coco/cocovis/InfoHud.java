package dev.coco.cocovis;

import dev.coco.cocovis.gui.Ui;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Info panel: logo, nick, FPS, ping, coordinates, speed in three looks (pills / slab / column). */
public final class InfoHud {
    public static int boundsX, boundsY, boundsW, boundsH;

    private static final int PILL_H = 16;
    private static final int GAP = 4;
    private static final int PILL_BG = 0xCC1B1C21;
    private static final int SLAB_BG = 0xD2131419;
    private static final int SLAB_LINE = 0xFF24262D;
    private static final int DIVIDER = 0xFF2A2C34;
    private static final int TEXT = 0xFFF1F2F6;
    private static final int MUTED = 0xFF7D8491;
    private static final int BLUE = 0xFF8DA2FB;
    private static final int OK = 0xFF55D6A0;
    private static final int WARN = 0xFFF2C94C;
    private static final int BAD = 0xFFE8606E;

    private enum Icon { LOGO, USER, FPS, PING, PIN, SPEED }

    /** row: 0 = first line (logo, nick, fps, ping), 1 = second line (coords, speed). */
    private record Item(int row, Icon icon, String text, String label, String value, int iconColor, int valueColor) {}

    private static float speedSmooth;

    private InfoHud() {}

    public static void render(GuiGraphicsExtractor g, Minecraft mc) {
        draw(g, mc, false);
    }

    private static int pingColor(int ms) {
        return ms < 80 ? OK : ms < 160 ? WARN : BAD;
    }

    private static List<Item> items(Minecraft mc, boolean preview) {
        CocoVisConfig cfg = CocoVisConfig.get();
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
        String speed = String.format(Locale.ROOT, "%.1f", preview ? 7.1f : speedSmooth);
        String xyz = (int) Math.floor(mc.player.getX()) + " " + (int) Math.floor(mc.player.getY())
                + " " + (int) Math.floor(mc.player.getZ());

        List<Item> list = new ArrayList<>();
        list.add(new Item(0, Icon.LOGO, "CocoVis", "CocoVis", "", BLUE, TEXT));
        if (cfg.showNick || preview) {
            String nick = mc.player.getName().getString();
            list.add(new Item(0, Icon.USER, nick, "Ник", nick, 0xFF3A3F4B, TEXT));
        }
        if (cfg.showFps || preview) {
            list.add(new Item(0, Icon.FPS, mc.getFps() + "fps", "FPS", String.valueOf(mc.getFps()), BLUE, TEXT));
        }
        if (cfg.showPing || preview) {
            list.add(new Item(0, Icon.PING, ping + "мс", "Пинг", ping + " мс", pingColor(ping), pingColor(ping)));
        }
        if (cfg.showCoords || preview) {
            list.add(new Item(1, Icon.PIN, "x" + (int) Math.floor(mc.player.getX())
                    + " y" + (int) Math.floor(mc.player.getY())
                    + " z" + (int) Math.floor(mc.player.getZ()), "XYZ", xyz, BLUE, TEXT));
        }
        if (cfg.showSpeed || preview) {
            list.add(new Item(1, Icon.SPEED, speed + "bps", "Скорость", speed + " bps", BLUE, TEXT));
        }
        return list;
    }

    public static void draw(GuiGraphicsExtractor g, Minecraft mc, boolean preview) {
        CocoVisConfig cfg = CocoVisConfig.get();
        if (mc.player == null || (!preview && !cfg.showInfo)) {
            boundsW = 0;
            boundsH = 0;
            return;
        }
        List<Item> items = items(mc, preview);
        Font font = mc.font;
        float s = cfg.infoScale <= 0 ? 1.0f : cfg.infoScale;
        int style = Math.max(0, Math.min(2, cfg.infoStyle));

        g.pose().pushMatrix();
        g.pose().translate((float) cfg.infoX, (float) cfg.infoY);
        g.pose().scale(s, s);
        int[] size = style == 0 ? drawPills(g, font, items) : style == 1 ? drawSlab(g, font, items) : drawColumn(g, font, items);
        g.pose().popMatrix();

        boundsX = cfg.infoX;
        boundsY = cfg.infoY;
        boundsW = Math.round(size[0] * s);
        boundsH = Math.round(size[1] * s);
    }

    private static int textW(Font font, Item it) {
        return it.icon() == Icon.LOGO ? Ui.titleWidth(font, it.text()) : Ui.width(font, it.text());
    }

    private static void drawText(GuiGraphicsExtractor g, Font font, Item it, int x, int y) {
        if (it.icon() == Icon.LOGO) {
            Ui.title(g, font, it.text(), x, y, TEXT);
        } else {
            Ui.text(g, font, it.text(), x, y, TEXT, false);
        }
    }

    // ------------------------------------------------------------------ style 0: separate pills

    private static int[] drawPills(GuiGraphicsExtractor g, Font font, List<Item> items) {
        int maxW = 0;
        int rows = 0;
        for (int row = 0; row < 2; row++) {
            int x = 0;
            int y = row * (PILL_H + GAP);
            boolean any = false;
            for (Item it : items) {
                if (it.row() != row) {
                    continue;
                }
                any = true;
                int w = 6 + 9 + 4 + textW(font, it) + 7;
                Ui.roundFill(g, x, y, w, PILL_H, PILL_H / 2, PILL_BG);
                drawIcon(g, it, x + 6, y + (PILL_H - 9) / 2);
                drawText(g, font, it, x + 6 + 9 + 4, y + (PILL_H - 8) / 2 + 1);
                x += w + GAP;
            }
            if (any) {
                maxW = Math.max(maxW, x - GAP);
                rows++;
            }
        }
        return new int[] {maxW, Math.max(PILL_H, rows * PILL_H + (rows - 1) * GAP)};
    }

    // ------------------------------------------------------------------ style 1: one slab

    private static int[] drawSlab(GuiGraphicsExtractor g, Font font, List<Item> items) {
        int padX = 9;
        int padY = 7;
        int rowH = 16;
        int itemGap = 10;
        int rowSep = 8;
        int[] rowW = new int[2];
        int[] rowCount = new int[2];
        for (Item it : items) {
            rowW[it.row()] += 9 + 4 + textW(font, it);
            rowCount[it.row()]++;
        }
        int rows = (rowCount[0] > 0 ? 1 : 0) + (rowCount[1] > 0 ? 1 : 0);
        int inner = 0;
        for (int r = 0; r < 2; r++) {
            if (rowCount[r] > 0) {
                inner = Math.max(inner, rowW[r] + (rowCount[r] - 1) * itemGap);
            }
        }
        int w = inner + padX * 2;
        int h = padY * 2 + rows * rowH + (rows - 1) * rowSep;
        Ui.shadow(g, 0, 0, w, h, 8);
        Ui.roundFill(g, 0, 0, w, h, 8, SLAB_BG);
        Ui.roundOutline(g, 0, 0, w, h, 8, SLAB_LINE);
        accentLine(g, 12, 0, w - 24);

        int rowIndex = 0;
        for (int r = 0; r < 2; r++) {
            if (rowCount[r] == 0) {
                continue;
            }
            int y = padY + rowIndex * (rowH + rowSep);
            int x = padX;
            int n = 0;
            for (Item it : items) {
                if (it.row() != r) {
                    continue;
                }
                if (n > 0) {
                    g.fill(x - itemGap / 2, y + 3, x - itemGap / 2 + 1, y + rowH - 3, DIVIDER);
                }
                drawIcon(g, it, x, y + (rowH - 9) / 2);
                drawText(g, font, it, x + 13, y + (rowH - 8) / 2 + 1);
                x += 13 + textW(font, it) + itemGap;
                n++;
            }
            if (rowIndex == 0 && rows == 2) {
                g.fill(padX, y + rowH + rowSep / 2, w - padX, y + rowH + rowSep / 2 + 1, SLAB_LINE);
            }
            rowIndex++;
        }
        return new int[] {w, h};
    }

    // ------------------------------------------------------------------ style 2: vertical slab

    private static int[] drawColumn(GuiGraphicsExtractor g, Font font, List<Item> items) {
        int pad = 9;
        int lineH = 15;
        int headH = 22;
        int maxLabel = 0;
        int maxValue = 0;
        int lines = 0;
        for (Item it : items) {
            if (it.icon() == Icon.LOGO) {
                continue;
            }
            maxLabel = Math.max(maxLabel, Ui.width(font, it.label()));
            maxValue = Math.max(maxValue, Ui.width(font, it.value()));
            lines++;
        }
        int w = Math.max(Ui.titleWidth(font, "CocoVis") + 13 + pad * 2 + 8,
                pad * 2 + 13 + maxLabel + 18 + maxValue);
        int h = pad + headH + lines * lineH + pad - (lines == 0 ? 0 : 2);
        Ui.shadow(g, 0, 0, w, h, 8);
        Ui.roundFill(g, 0, 0, w, h, 8, SLAB_BG);
        Ui.roundOutline(g, 0, 0, w, h, 8, SLAB_LINE);
        accentLine(g, 12, 0, w - 24);

        Ui.diamond(g, pad, pad + 3, 9, BLUE);
        Ui.title(g, font, "CocoVis", pad + 13, pad + 3, TEXT);
        g.fill(pad, pad + headH - 4, w - pad, pad + headH - 3, SLAB_LINE);
        int y = pad + headH;
        for (Item it : items) {
            if (it.icon() == Icon.LOGO) {
                continue;
            }
            drawIcon(g, it, pad, y + 1);
            Ui.text(g, font, it.label(), pad + 13, y + 1, MUTED, false);
            Ui.text(g, font, it.value(), w - pad - Ui.width(font, it.value()), y + 1, it.valueColor(), false);
            y += lineH;
        }
        return new int[] {w, h};
    }

    private static void accentLine(GuiGraphicsExtractor g, int x, int y, int w) {
        int half = w / 2;
        Ui.hBar(g, x, y, half, 1, Ui.fade(BLUE, 0f), Ui.fade(BLUE, 0.85f));
        Ui.hBar(g, x + half, y, w - half, 1, Ui.fade(BLUE, 0.85f), Ui.fade(BLUE, 0f));
    }

    /** 9x9 icons drawn from smooth shapes. */
    private static void drawIcon(GuiGraphicsExtractor g, Item it, int x, int y) {
        int c = it.iconColor();
        switch (it.icon()) {
            case LOGO -> Ui.diamond(g, x, y, 9, c);
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
            case SPEED -> {
                Ui.triangleRight(g, x, y + 1, 4, 7, c);
                Ui.triangleRight(g, x + 5, y + 1, 4, 7, c);
            }
        }
    }
}
