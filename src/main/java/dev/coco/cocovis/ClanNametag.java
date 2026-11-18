package dev.coco.cocovis;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.world.entity.player.Player;

import java.util.List;
import java.util.Locale;

/** Builds the nametag text: "Nick ▎ ✦ CLAN  ❤ ▮▮▮▮▯ 16". */
public final class ClanNametag {
    private static final String[] EMBLEMS = {"✦", "❖", "♦", "♠", "♣", "♥", "★", "☠", "⚔", "☀"};
    private static final int NICK_COLOR = 0xFFFFFF;
    private static final int HEART_COLOR = 0xE8606E;
    private static final int EMPTY_COLOR = 0x5B6672;
    private static final int ABSORB_COLOR = 0xFFD84D;

    private ClanNametag() {}

    private static Style color(int rgb) {
        return Style.EMPTY.withColor(TextColor.fromRgb(rgb & 0xFFFFFF));
    }

    /** @return new nametag, or null to keep the vanilla one. */
    public static Component build(Player player, Component original) {
        CocoVisConfig cfg = CocoVisConfig.get();
        if (!cfg.clanNametags) {
            return null;
        }
        String clan = ClanResolver.resolve(player);
        List<EffectsHud.Entry> effects = cfg.nametagEffects ? EffectsHud.of(player) : List.of();
        if (clan == null && !cfg.showHealth && effects.isEmpty()) {
            return null;
        }

        MutableComponent out = Component.empty();
        if (cfg.useProfileName) {
            out.append(Component.literal(player.getName().getString()).withStyle(color(NICK_COLOR)));
        } else {
            out.append(original);
        }

        if (clan != null) {
            Style clanStyle = color(colorFor(clan, cfg));
            if (cfg.showAccent) {
                out.append(Component.literal(" ▎").withStyle(clanStyle));
            }
            out.append(Component.literal(" "));
            if (cfg.showEmblem) {
                out.append(Component.literal(emblemFor(clan) + " ").withStyle(clanStyle));
            }
            out.append(Component.literal(clan).withStyle(clanStyle));
        }

        if (cfg.showHealth) {
            out.append(Component.literal("  "));
            out.append(health(player, cfg));
        }
        if (!effects.isEmpty()) {
            out.append(Component.literal("  "));
            Style iconFont = EffectGlyphs.style();
            int shown = Math.min(4, effects.size());
            for (int i = 0; i < shown; i++) {
                EffectsHud.Entry e = effects.get(i);
                Character glyph = EffectGlyphs.glyph(e.id());
                if (iconFont != null && glyph != null) {
                    out.append(Component.literal(String.valueOf(glyph)).withStyle(iconFont));
                } else {
                    out.append(Component.literal("●").withStyle(color(e.color())));
                }
                if (!e.infinite() && e.ticks() >= 0) {
                    int seconds = e.ticks() / 20;
                    String time = String.format(Locale.ROOT, " %d:%02d", seconds / 60, seconds % 60);
                    out.append(Component.literal(time).withStyle(color(seconds <= 10 ? 0xFF6B6B : 0xFFFFFF)));
                }
                if (i < shown - 1) {
                    out.append(Component.literal("  "));
                }
            }
            if (effects.size() > shown) {
                out.append(Component.literal(" +" + (effects.size() - shown)).withStyle(color(EMPTY_COLOR)));
            }
        }
        return out;
    }

    private static Component health(Player player, CocoVisConfig cfg) {
        float max = Math.max(1f, player.getMaxHealth());
        float hp = Math.max(0f, player.getHealth());
        float ratio = Math.min(1f, hp / max);
        int hpColor = ratio > 0.66f ? 0x55E08A : ratio > 0.33f ? 0xF2C94C : 0xE8606E;

        MutableComponent out = Component.empty();
        out.append(Component.literal("❤ ").withStyle(color(HEART_COLOR)));
        if (cfg.healthBar) {
            int seg = Math.max(3, Math.min(10, cfg.healthSegments));
            int filled = Math.round(ratio * seg);
            if (hp > 0f && filled == 0) {
                filled = 1;
            }
            out.append(Component.literal("▮".repeat(filled)).withStyle(color(hpColor)));
            out.append(Component.literal("▯".repeat(seg - filled)).withStyle(color(EMPTY_COLOR)));
        }
        if (cfg.healthNumber) {
            out.append(Component.literal(" " + (int) Math.ceil(hp)).withStyle(color(hpColor)));
            float absorb = player.getAbsorptionAmount();
            if (absorb > 0f) {
                out.append(Component.literal(" +" + (int) Math.ceil(absorb)).withStyle(color(ABSORB_COLOR)));
            }
        }
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
