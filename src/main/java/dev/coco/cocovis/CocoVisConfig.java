package dev.coco.cocovis;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

/** Config file: .minecraft/config/cocovis.json */
public final class CocoVisConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static CocoVisConfig instance = new CocoVisConfig();

    /** Master switch for clan nametags. */
    public boolean enabled = true;

    /**
     * Regex applied to the player's TAB text (team prefix/suffix included).
     * Group 1 = clan name. Presets:
     *   [CLAN] Nick   ->  ^\[([^\]]+)\]
     *   Nick | CLAN   ->  \|\s*(.+)$
     *   Nick [CLAN]   ->  \[([^\]]+)\]\s*$
     *   (CLAN) Nick   ->  ^\(([^)]+)\)
     */
    public String clanPattern = "^\\[([^\\]]+)\\]";

    /** Show a small emblem glyph before the clan name (chosen from the clan name). */
    public boolean showEmblem = true;
    /** Show the colored accent bar between nick and clan. */
    public boolean showAccent = true;
    /** Use the plain profile nick instead of the vanilla nametag text (avoids duplicate clan prefix). */
    public boolean useProfileName = true;
    /** Optional manual colors: "clanname" (lowercase) -> "#RRGGBB". Others get an automatic color. */
    public Map<String, String> colorOverrides = new LinkedHashMap<>();

    /** Diamond counter panel (diamonds + diamond blocks x9 in your inventory). */
    public boolean showDiamondHud = true;
    public int hudX = 6;
    public int hudY = 6;
    public float hudScale = 1.0f;

    /** Price lines in item tooltips, in diamonds. Edit/add items here ("minecraft:emerald": 0.5). */
    public boolean showTooltips = true;
    public Map<String, Double> prices = defaultPrices();

    private static Map<String, Double> defaultPrices() {
        Map<String, Double> m = new LinkedHashMap<>();
        m.put("minecraft:diamond", 1.0);
        m.put("minecraft:diamond_block", 9.0);
        return m;
    }

    public static CocoVisConfig get() {
        return instance;
    }

    private static Path path() {
        return FabricLoader.getInstance().getConfigDir().resolve("cocovis.json");
    }

    public static void load() {
        Path p = path();
        try {
            if (Files.exists(p)) {
                CocoVisConfig loaded = GSON.fromJson(Files.readString(p), CocoVisConfig.class);
                if (loaded != null) {
                    instance = loaded;
                }
            }
            save();
        } catch (Exception e) {
            CocoVisClient.LOGGER.warn("Could not read cocovis.json, using defaults", e);
        }
    }

    public static void save() {
        try {
            Files.writeString(path(), GSON.toJson(instance));
        } catch (IOException e) {
            CocoVisClient.LOGGER.warn("Could not write cocovis.json", e);
        }
    }
}
