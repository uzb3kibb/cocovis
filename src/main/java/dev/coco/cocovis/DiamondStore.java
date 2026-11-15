package dev.coco.cocovis;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

/** Remembers diamonds seen in chests and the ender chest, per server. File: config/cocovis-storage.json */
public final class DiamondStore {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public static final class ServerStore {
        public int ender;
        public Map<String, Integer> chests = new LinkedHashMap<>();
    }

    public static final class Data {
        public Map<String, ServerStore> servers = new LinkedHashMap<>();
    }

    private static Data data = new Data();

    private DiamondStore() {}

    private static Path path() {
        return FabricLoader.getInstance().getConfigDir().resolve("cocovis-storage.json");
    }

    public static void load() {
        try {
            Path p = path();
            if (Files.exists(p)) {
                Data loaded = GSON.fromJson(Files.readString(p), Data.class);
                if (loaded != null && loaded.servers != null) {
                    data = loaded;
                }
            }
        } catch (Exception e) {
            CocoVisClient.LOGGER.warn("Could not read cocovis-storage.json", e);
        }
    }

    public static void save() {
        try {
            Files.writeString(path(), GSON.toJson(data));
        } catch (Exception e) {
            CocoVisClient.LOGGER.warn("Could not write cocovis-storage.json", e);
        }
    }

    public static ServerStore get(String serverKey) {
        ServerStore s = data.servers.get(serverKey);
        if (s == null) {
            s = new ServerStore();
            data.servers.put(serverKey, s);
        }
        if (s.chests == null) {
            s.chests = new LinkedHashMap<>();
        }
        return s;
    }
}
