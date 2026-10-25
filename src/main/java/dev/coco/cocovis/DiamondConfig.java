package dev.coco.cocovis;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

/** Тот же файл и поля, что у старого мода: config/diamondeconomy.json — твои цены и позиция панели подхватятся. */
public class DiamondConfig {
    public boolean showHud = true;
    public int hudX = 6;
    public int hudY = 6;
    public float hudScale = 1.0f;
    public boolean showToasts = true;
    public int minToastDiamonds = 5;
    public int toastMergeTicks = 20;
    public int toastDurationTicks = 70;
    public boolean silentAfterScreen = false;
    public boolean showTooltips = true;
    /** Инфо-панель (Watermark/FPS/Coords). -1 = автоматически под панелью алмазов. */
    public int infoX = -1;
    public int infoY = -1;
    public float infoScale = 1.0f;
    public Map<String, Double> prices = new LinkedHashMap<>();

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static DiamondConfig instance;

    private static Path path() {
        return FabricLoader.getInstance().getConfigDir().resolve("diamondeconomy.json");
    }

    public static DiamondConfig get() {
        if (instance == null) load();
        return instance;
    }

    public static void load() {
        DiamondConfig loaded = null;
        Path p = path();
        boolean existed = Files.exists(p);
        if (existed) {
            try (Reader r = Files.newBufferedReader(p, StandardCharsets.UTF_8)) {
                loaded = GSON.fromJson(r, DiamondConfig.class);
            } catch (Exception e) {
                System.err.println("[cocovis] Не удалось прочитать конфиг: " + e);
            }
        }
        if (loaded == null) loaded = new DiamondConfig();
        if (!existed) {
            loaded.prices.put("minecraft:diamond", 1.0);
            loaded.prices.put("minecraft:diamond_block", 9.0);
            loaded.prices.put("minecraft:iron_ingot", 0.5);
            loaded.prices.put("minecraft:gold_ingot", 2.0);
            loaded.prices.put("minecraft:emerald", 1.0);
        }
        if (loaded.prices == null) loaded.prices = new LinkedHashMap<>();
        instance = loaded;
        if (!existed) save();
    }

    public static void save() {
        try (Writer w = Files.newBufferedWriter(path(), StandardCharsets.UTF_8)) {
            GSON.toJson(get(), w);
        } catch (Exception e) {
            System.err.println("[cocovis] Не удалось сохранить конфиг: " + e);
        }
    }
}
