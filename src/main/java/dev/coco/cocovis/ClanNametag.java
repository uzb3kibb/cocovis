package dev.coco.cocovis;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.world.entity.player.Player;

import java.util.Locale;

/** Builds the nametag text: "Nick ▎ ✦ CLAN" with a clan color. */
public final class ClanNametag {
    private static final String[] EMBLEMS = {"✦", "❖", "♦", "♠", "♣", "♥", "★", "☠", "⚔", "☀"};
    private static final int NICK_COLOR = 0xFFFFFF;

    private ClanNametag() {}

    /** @return new nametag, or null to keep the vanilla one. */
    public static Component build(Player player, Component original) {
        CocoVisConfig cfg = CocoVisConfig.get();
        if (!cfg.enabled) {
            return null;
        }
        String clan = ClanResolver.resolve(player);
        if (clan == null) {
            return null;
        }
        int rgb = colorFor(clan, cfg);

        MutableComponent out = Component.empty();
        if (cfg.useProfileName) {
            out.append(Component.literal(player.getName().getString())
                    .withStyle(Style.EMPTY.withColor(TextColor.fromRgb(NICK_COLOR))));
        } else {
            out.append(original);
        }
        Style clanStyle = Style.EMPTY.withColor(TextColor.fromRgb(rgb));
        if (cfg.showAccent) {
            out.append(Component.literal(" ▎").withStyle(clanStyle));
        }
        out.append(Component.literal(" "));
        if (cfg.showEmblem) {
            out.append(Component.literal(emblemFor(clan) + " ").withStyle(clanStyle));
        }
        out.append(Component.literal(clan).withStyle(clanStyle));
        return out;
    }

    private static String emblemFor(String clan) {
        int h = clan.toLowerCase(Locale.ROOT).hashCode();
        return EMBLEMS[Math.floorMod(h, EMBLEMS.length)];
    }

    private static int colorFor(String clan, CocoVisConfig cfg) {
        String key = clan.toLowerCase(Locale.ROOT);
        String override = cfg.colorOverrides == null ? null : cfg.colorOverrides.get(key);
        if (override != null) {
            try {
                String hex = override.startsWith("#") ? override.substring(1) : override;
                return Integer.parseInt(hex, 16) & 0xFFFFFF;
            } catch (NumberFormatException ignored) {
                // fall through to the automatic color
            }
        }
        float hue = Math.floorMod(key.hashCode(), 360) / 360f;
        return hsvToRgb(hue, 0.55f, 1.0f);
    }

    private static int hsvToRgb(float h, float s, float v) {
        float c = v * s;
        float x = c * (1 - Math.abs((h * 6) % 2 - 1));
        float m = v - c;
        float r, g, b;
        int sector = (int) (h * 6) % 6;
        switch (sector) {
            case 0 -> { r = c; g = x; b = 0; }
            case 1 -> { r = x; g = c; b = 0; }
            case 2 -> { r = 0; g = c; b = x; }
            case 3 -> { r = 0; g = x; b = c; }
            case 4 -> { r = x; g = 0; b = c; }
            default -> { r = c; g = 0; b = x; }
        }
        int ri = Math.round((r + m) * 255);
        int gi = Math.round((g + m) * 255);
        int bi = Math.round((b + m) * 255);
        return (ri << 16) | (gi << 8) | bi;
    }
}
