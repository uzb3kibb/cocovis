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

    // ---- clan nametags ----
    public boolean clanNametags = true;
    /**
     * Regex applied to the player's TAB text (team prefix/suffix included). Group 1 = clan name.
     *   [CLAN] Nick  ->  ^\[([^\]]+)\]      Nick | CLAN  ->  \|\s*(.+)$
     *   Nick [CLAN]  ->  \[([^\]]+)\]\s*$   (CLAN) Nick  ->  ^\(([^)]+)\)
     */
    public String clanPattern = "^\\[([^\\]]+)\\]";
    public boolean showEmblem = true;
    public boolean showAccent = true;
    public boolean useProfileName = true;
    /** HP in the nametag: heart + segmented bar + number (also for players without a clan). */
    public boolean showHealth = true;
    public boolean healthBar = true;
    public boolean healthNumber = true;
    public int healthSegments = 5;
    /** Optional manual colors: "clanname" (lowercase) -> "#RRGGBB". */
    public Map<String, String> colorOverrides = new LinkedHashMap<>();

    // ---- diamond panel ----
    public boolean showHud = true;
    public int hudX = 6;
    public int hudY = 40;
    public float hudScale = 1.0f;

    // ---- toasts ----
    public boolean showToasts = true;
    public int minToastDiamonds = 1;
    public int toastMergeTicks = 20;
    public int toastDurationTicks = 80;
    public boolean silentAfterScreen = true;
    public int toastX = 6;
    public int toastY = 120;

    // ---- tooltips ----
    public boolean showTooltips = true;
    public Map<String, Double> prices = defaultPrices();

    // ---- info panel ----
    public int infoX = 6;
    public int infoY = 6;
    public float infoScale = 1.0f;
    public boolean showWatermark = true;
    public boolean showNick = true;
    public boolean showFps = true;
    public boolean showPing = true;
    public boolean showCoords = true;
    public boolean showSpeed = true;

    // ---- potion effects panel ----
    public boolean showEffects = true;
    /** false = card with progress bars, true = compact pills. */
    public boolean effectsPills = false;
    public int effX = 6;
    public int effY = 170;
    public float effScale = 1.0f;
    /** Coloured dots for active effects next to the nick. */
    public boolean nametagEffects = true;

    private static Map<String, Double> defaultPrices() {
        Map<String, Double> m = new LinkedHashMap<>();
        m.put("minecraft:diamond", 1.0);
        m.put("minecraft:diamond_block", 9.0);
        m.put("minecraft:iron_ingot", 0.5);
        m.put("minecraft:gold_ingot", 2.0);
        m.put("minecraft:emerald", 1.0);
        return m;
    }

    public static CocoVisConfig get() {
        return instance;
    }

    public void resetPositions() {
        hudX = 6; hudY = 40; hudScale = 1.0f;
        infoX = 6; infoY = 6; infoScale = 1.0f;
        toastX = 6; toastY = 120;
        effX = 6; effY = 170; effScale = 1.0f;
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
            if (instance.prices == null) {
                instance.prices = defaultPrices();
            }
            if (instance.colorOverrides == null) {
                instance.colorOverrides = new LinkedHashMap<>();
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
