package dev.coco.cocovis;

import dev.coco.cocovis.gui.Ui;
import net.minecraft.network.chat.Style;

import java.util.Map;

/** Vanilla effect icons as font glyphs (see assets/cocovis/font/effects.json). */
public final class EffectGlyphs {
    private static final Map<String, Character> CHARS = Map.ofEntries(
            Map.entry("minecraft:speed", (char) 0xE000),
            Map.entry("minecraft:slowness", (char) 0xE001),
            Map.entry("minecraft:haste", (char) 0xE002),
            Map.entry("minecraft:mining_fatigue", (char) 0xE003),
            Map.entry("minecraft:strength", (char) 0xE004),
            Map.entry("minecraft:instant_health", (char) 0xE005),
            Map.entry("minecraft:instant_damage", (char) 0xE006),
            Map.entry("minecraft:jump_boost", (char) 0xE007),
            Map.entry("minecraft:nausea", (char) 0xE008),
            Map.entry("minecraft:regeneration", (char) 0xE009),
            Map.entry("minecraft:resistance", (char) 0xE00A),
            Map.entry("minecraft:fire_resistance", (char) 0xE00B),
            Map.entry("minecraft:water_breathing", (char) 0xE00C),
            Map.entry("minecraft:invisibility", (char) 0xE00D),
            Map.entry("minecraft:blindness", (char) 0xE00E),
            Map.entry("minecraft:night_vision", (char) 0xE00F),
            Map.entry("minecraft:hunger", (char) 0xE010),
            Map.entry("minecraft:weakness", (char) 0xE011),
            Map.entry("minecraft:poison", (char) 0xE012),
            Map.entry("minecraft:wither", (char) 0xE013),
            Map.entry("minecraft:health_boost", (char) 0xE014),
            Map.entry("minecraft:absorption", (char) 0xE015),
            Map.entry("minecraft:saturation", (char) 0xE016),
            Map.entry("minecraft:glowing", (char) 0xE017),
            Map.entry("minecraft:levitation", (char) 0xE018),
            Map.entry("minecraft:luck", (char) 0xE019),
            Map.entry("minecraft:unluck", (char) 0xE01A),
            Map.entry("minecraft:slow_falling", (char) 0xE01B),
            Map.entry("minecraft:conduit_power", (char) 0xE01C),
            Map.entry("minecraft:dolphins_grace", (char) 0xE01D),
            Map.entry("minecraft:bad_omen", (char) 0xE01E),
            Map.entry("minecraft:hero_of_the_village", (char) 0xE01F),
            Map.entry("minecraft:darkness", (char) 0xE020),
            Map.entry("minecraft:trial_omen", (char) 0xE021),
            Map.entry("minecraft:raid_omen", (char) 0xE022),
            Map.entry("minecraft:wind_charged", (char) 0xE023),
            Map.entry("minecraft:weaving", (char) 0xE024),
            Map.entry("minecraft:oozing", (char) 0xE025),
            Map.entry("minecraft:infested", (char) 0xE026));

    private static Style style;
    private static boolean init;

    private EffectGlyphs() {}

    public static Character glyph(String effectId) {
        return CHARS.get(effectId);
    }

    /** Style selecting the icon font, or null if the font cannot be applied. */
    public static Style style() {
        if (!init) {
            init = true;
            style = Ui.fontStyle("effects");
        }
        return style;
    }
}
