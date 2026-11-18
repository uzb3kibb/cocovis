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
    public String clanPattern = "^\\[([^\\]]+)\\]";
    public boolean showEmblem = true;
    public boolean showAccent = true;
    public boolean useProfileName = true;
    public boolean showHealth = true;
    public boolean healthBar = true;
    public boolean healthNumber = true;
    public int healthSegments = 5;
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
    public boolean showInfo = true;
    public int infoStyle = 1;
    public boolean showNick = true;
    public boolean showFps = true;
    public boolean showPing = true;
    public boolean showCoords = true;
    public boolean showSpeed = true;

    // ---- potion effects panel ----
    public boolean showEffects = true;
    public boolean effectsPills = false;
    public int effX = 6;
    public int effY = 170;
    public float effScale = 1.0f;
    public boolean nametagEffects = true;

    // ---- sky / time of day (client side only) ----
    public boolean skyEnabled = false;
    public int skyTime = 18000;
    public boolean skyRealTime = false;

    // ---- sky color ----
    public boolean skyColorEnabled = false;
    /** 0 = default, 1 = Neon, 2 = Gradient, 3 = Sunset, 4 = Aurora, 5 = Custom. */
    public int skyColorMode = 1;
    /** Custom sky tint in #RRGGBB format. */
    public String skyColorHex = "#7B2CFF";
    /** 0..100 percent tint strength. */
    public int skyColorIntensity = 85;

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

    public static void openConfig() {
        Path p = path();
        try {
            if (!Files.exists(p)) {
                save();
            }
            // Launch the operating system's file handler without relying on
            // Minecraft-version-specific platform APIs or AWT Desktop.
            String os = System.getProperty("os.name", "").toLowerCase();
            ProcessBuilder opener;
            if (os.contains("win")) {
                opener = new ProcessBuilder("cmd", "/c", "start", "", p.toAbsolutePath().toString());
            } else if (os.contains("mac")) {
                opener = new ProcessBuilder("open", p.toAbsolutePath().toString());
            } else {
                opener = new ProcessBuilder("xdg-open", p.toAbsolutePath().toString());
            }
            opener.start();
        } catch (Exception e) {
            CocoVisClient.LOGGER.error("Could not open cocovis.json at {}", p, e);
        }
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
            if (instance.prices == null) instance.prices = defaultPrices();
            if (instance.colorOverrides == null) instance.colorOverrides = new LinkedHashMap<>();
            if (instance.skyColorHex == null || !instance.skyColorHex.matches("#?[0-9a-fA-F]{6}")) {
                instance.skyColorHex = "#7B2CFF";
            }
            instance.skyColorIntensity = Math.max(0, Math.min(100, instance.skyColorIntensity));
            instance.skyColorMode = Math.max(0, Math.min(5, instance.skyColorMode));
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
