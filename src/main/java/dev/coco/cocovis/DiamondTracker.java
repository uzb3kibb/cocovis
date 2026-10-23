package dev.coco.cocovis;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.ShulkerBoxMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.BarrelBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.ShulkerBoxBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraft.world.phys.BlockHitResult;

import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/** Считает алмазы (алмаз = 1, блок = 9), помнит содержимое сундуков и эндер-сундука, копит уведомления. */
public final class DiamondTracker {
    public static final class Toast {
        public final long createdMs = System.currentTimeMillis();
        public final int delta;
        public final int total;
        Toast(int delta, int total) { this.delta = delta; this.total = total; }
    }

    public static final List<Toast> TOASTS = new ArrayList<>();
    public static int total, chestsSum, chestsCount, enderTotal;

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Type CHESTS_TYPE = new TypeToken<Map<String, Map<String, Integer>>>() {}.getType();
    private static final Type ENDER_TYPE = new TypeToken<Map<String, Integer>>() {}.getType();

    private static final Map<String, Integer> chests = new HashMap<>();
    private static String loadedServer;
    private static boolean ready, dirtyChests, dirtyEnder;
    private static int baseline, pending, quiet, silentTicks, openTicks, validateTicks;
    private static String openKey;

    private DiamondTracker() {}

    public static int value(ItemStack s) {
        if (s.isEmpty()) return 0;
        if (s.is(Items.DIAMOND)) return s.getCount();
        if (s.is(Items.DIAMOND_BLOCK)) return s.getCount() * 9;
        return 0;
    }

    public static int count(LocalPlayer p) {
        int n = 0;
        Inventory inv = p.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) n += value(inv.getItem(i));
        return n;
    }

    private static int countContainer(AbstractContainerMenu menu) {
        int n = 0;
        for (Slot slot : menu.slots) {
            if (slot.container instanceof Inventory) continue;
            n += value(slot.getItem());
        }
        return n;
    }

    public static void tick(Minecraft mc) {
        LocalPlayer p = mc.player;
        if (p == null || mc.level == null) {
            ready = false;
            loadedServer = null;
            TOASTS.clear();
            return;
        }
        DiamondConfig cfg = DiamondConfig.get();
        String server = serverKey(mc);
        if (!server.equals(loadedServer)) {
            loadedServer = server;
            loadChests(server);
            enderTotal = loadEnder(server);
            ready = false;
            pending = 0;
            TOASTS.clear();
        }

        total = count(p);
        Screen screen = mc.gui.screen();

        // какой блок открываем — запоминаем, пока экран закрыт
        if (screen == null) {
            openKey = resolveKey(mc);
            openTicks = 0;
            if (dirtyChests) { saveChests(server); dirtyChests = false; }
            if (dirtyEnder) { saveEnder(server); dirtyEnder = false; }
        }

        boolean storageOpen = false;
        if (screen instanceof AbstractContainerScreen<?> cs
                && (cs.getMenu() instanceof ChestMenu || cs.getMenu() instanceof ShulkerBoxMenu)) {
            storageOpen = true;
            openTicks++;
            if (openTicks >= 5) { // ждём, пока сервер пришлёт содержимое
                int c = countContainer(cs.getMenu());
                if (isEnderTitle(cs.getTitle())) {
                    if (c != enderTotal) { enderTotal = c; dirtyEnder = true; }
                } else if (openKey != null) {
                    Integer old = chests.put(openKey, c);
                    if (old == null || old != c) dirtyChests = true;
                }
            }
        }

        if (++validateTicks >= 100) { validateTicks = 0; validateChests(mc); }
        recomputeChests();

        // уведомления
        if (screen != null && cfg.silentAfterScreen) silentTicks = 20;
        else if (silentTicks > 0) silentTicks--;

        if (!ready) {
            baseline = total; ready = true; pending = 0; quiet = 0;
        } else if (total != baseline) {
            int diff = total - baseline;
            baseline = total;
            if (silentTicks == 0 || !cfg.silentAfterScreen) {
                pending += diff;
                quiet = Math.max(1, cfg.toastMergeTicks);
            }
        }
        if (pending != 0) {
            if (quiet > 0) quiet--;
            if (quiet <= 0) {
                if (cfg.showToasts && Math.abs(pending) >= cfg.minToastDiamonds) TOASTS.add(new Toast(pending, total));
                pending = 0;
            }
        }
    }

    private static void recomputeChests() {
        int sum = 0;
        for (int v : chests.values()) sum += v;
        chestsSum = sum;
        chestsCount = chests.size();
    }

    private static boolean isEnderTitle(Component title) {
        return title.getString().equals(Component.translatable("container.enderchest").getString());
    }

    private static String dimKey(Minecraft mc) {
        return String.valueOf(mc.level.dimension());
    }

    private static String resolveKey(Minecraft mc) {
        if (!(mc.hitResult instanceof BlockHitResult hit)) return null;
        BlockPos pos = hit.getBlockPos();
        BlockState st = mc.level.getBlockState(pos);
        Block b = st.getBlock();
        if (b instanceof ChestBlock) {
            if (st.hasProperty(ChestBlock.TYPE) && st.getValue(ChestBlock.TYPE) != ChestType.SINGLE) {
                BlockPos other = pos.relative(ChestBlock.getConnectedDirection(st));
                if (other.asLong() < pos.asLong()) pos = other; // двойной сундук = один ключ
            }
        } else if (!(b instanceof BarrelBlock) && !(b instanceof ShulkerBoxBlock)) {
            return null;
        }
        return dimKey(mc) + "|" + pos.getX() + "," + pos.getY() + "," + pos.getZ();
    }

    private static void validateChests(Minecraft mc) {
        String dim = dimKey(mc);
        boolean changed = false;
        Iterator<String> it = chests.keySet().iterator();
        while (it.hasNext()) {
            String key = it.next();
            int bar = key.lastIndexOf('|');
            if (bar < 0 || !key.substring(0, bar).equals(dim)) continue;
            try {
                String[] c = key.substring(bar + 1).split(",");
                BlockPos pos = new BlockPos(Integer.parseInt(c[0]), Integer.parseInt(c[1]), Integer.parseInt(c[2]));
                if (!mc.level.hasChunkAt(pos)) continue;
                Block b = mc.level.getBlockState(pos).getBlock();
                if (!(b instanceof ChestBlock) && !(b instanceof BarrelBlock) && !(b instanceof ShulkerBoxBlock)) {
                    it.remove();
                    changed = true;
                }
            } catch (RuntimeException e) {
                it.remove();
                changed = true;
            }
        }
        if (changed) dirtyChests = true;
    }

    // ---- хранение ----

    private static String serverKey(Minecraft mc) {
        ServerData sd = mc.getCurrentServer();
        return sd != null ? sd.ip : "singleplayer";
    }

    private static Path chestsPath() { return FabricLoader.getInstance().getConfigDir().resolve("diamondeconomy-chests.json"); }
    private static Path enderPath() { return FabricLoader.getInstance().getConfigDir().resolve("diamondeconomy-ender.json"); }

    private static Map<String, Map<String, Integer>> readChestFile() {
        Path p = chestsPath();
        if (Files.exists(p)) {
            try (Reader r = Files.newBufferedReader(p, StandardCharsets.UTF_8)) {
                Map<String, Map<String, Integer>> m = GSON.fromJson(r, CHESTS_TYPE);
                if (m != null) return m;
            } catch (Exception e) {
                System.err.println("[cocovis] Не удалось прочитать данные сундуков: " + e);
            }
        }
        return new HashMap<>();
    }

    private static void loadChests(String server) {
        chests.clear();
        Map<String, Integer> m = readChestFile().get(server);
        if (m != null) chests.putAll(m);
    }

    private static void saveChests(String server) {
        Map<String, Map<String, Integer>> all = readChestFile();
        all.put(server, new HashMap<>(chests));
        try (Writer w = Files.newBufferedWriter(chestsPath(), StandardCharsets.UTF_8)) {
            GSON.toJson(all, CHESTS_TYPE, w);
        } catch (Exception e) {
            System.err.println("[cocovis] Не удалось сохранить данные сундуков: " + e);
        }
    }

    private static Map<String, Integer> readEnderMap() {
        Path p = enderPath();
        if (Files.exists(p)) {
            try (Reader r = Files.newBufferedReader(p, StandardCharsets.UTF_8)) {
                Map<String, Integer> m = GSON.fromJson(r, ENDER_TYPE);
                if (m != null) return m;
            } catch (Exception e) {
                System.err.println("[cocovis] Не удалось прочитать данные эндер-сундука: " + e);
            }
        }
        return new HashMap<>();
    }

    private static int loadEnder(String server) {
        Integer v = readEnderMap().get(server);
        return v == null ? 0 : v;
    }

    private static void saveEnder(String server) {
        Map<String, Integer> all = readEnderMap();
        all.put(server, enderTotal);
        try (Writer w = Files.newBufferedWriter(enderPath(), StandardCharsets.UTF_8)) {
            GSON.toJson(all, ENDER_TYPE, w);
        } catch (Exception e) {
            System.err.println("[cocovis] Не удалось сохранить данные эндер-сундука: " + e);
        }
    }
}
