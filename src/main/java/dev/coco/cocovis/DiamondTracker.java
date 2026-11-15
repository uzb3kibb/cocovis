package dev.coco.cocovis;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.ShulkerBoxMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/** Counts diamonds (inventory + remembered chests + ender chest) and raises toasts when the sum changes. */
public final class DiamondTracker {
    public static final class Toast {
        public int amount;
        public int age;
        public int ticksLeft;
    }

    public static final List<Toast> TOASTS = new ArrayList<>();
    private static final int SILENT_TICKS = 20;

    private static String loadedServer;
    private static int lastTotal = -1;
    private static int silent;
    private static int openTicks;
    private static int validateTicks;
    private static boolean wasContainer;
    private static boolean dirty;
    private static String openKey;

    private DiamondTracker() {}

    private static String serverKey(Minecraft mc) {
        ServerData sd = mc.getCurrentServer();
        return sd != null && sd.ip != null ? sd.ip : "singleplayer";
    }

    public static int chestsCount() {
        return loadedServer == null ? 0 : DiamondStore.get(loadedServer).chests.size();
    }

    public static int chestsSum() {
        if (loadedServer == null) {
            return 0;
        }
        int sum = 0;
        for (int v : DiamondStore.get(loadedServer).chests.values()) {
            sum += v;
        }
        return sum;
    }

    public static int enderTotal() {
        return loadedServer == null ? 0 : DiamondStore.get(loadedServer).ender;
    }

    public static void tick(Minecraft mc) {
        CocoVisConfig cfg = CocoVisConfig.get();
        LocalPlayer player = mc.player;
        ClientLevel level = mc.level;
        if (player == null || level == null) {
            loadedServer = null;
            lastTotal = -1;
            TOASTS.clear();
            return;
        }
        String key = serverKey(mc);
        if (!key.equals(loadedServer)) {
            loadedServer = key;
            lastTotal = -1;
            TOASTS.clear();
            validateTicks = 0;
        }

        for (Iterator<Toast> it = TOASTS.iterator(); it.hasNext(); ) {
            Toast t = it.next();
            t.age++;
            t.ticksLeft--;
            if (t.ticksLeft <= 0) {
                it.remove();
            }
        }

        Screen screen = mc.gui.screen();
        boolean container = screen instanceof AbstractContainerScreen<?>;
        if (container) {
            openTicks++;
            trackContainer(mc, (AbstractContainerScreen<?>) screen, player, level);
            silent = SILENT_TICKS;
        } else {
            openTicks = 0;
            openKey = null;
            if (silent > 0) {
                silent--;
            }
            if (wasContainer && dirty) {
                DiamondStore.save();
                dirty = false;
            }
            if (++validateTicks >= 100) {
                validateTicks = 0;
                validateChests(level);
            }
        }
        wasContainer = container;

        int total = DiamondCount.inInventory(player) + chestsSum() + enderTotal();
        if (lastTotal < 0 || container || (cfg.silentAfterScreen && silent > 0)) {
            lastTotal = total;
        } else if (total != lastTotal) {
            addToast(total - lastTotal, cfg);
            lastTotal = total;
        }
    }

    private static void addToast(int delta, CocoVisConfig cfg) {
        if (Math.abs(delta) < Math.max(1, cfg.minToastDiamonds)) {
            return;
        }
        if (!TOASTS.isEmpty()) {
            Toast last = TOASTS.get(TOASTS.size() - 1);
            if (last.age < cfg.toastMergeTicks && (last.amount > 0) == (delta > 0)) {
                last.amount += delta;
                last.age = 0;
                last.ticksLeft = cfg.toastDurationTicks;
                return;
            }
        }
        Toast t = new Toast();
        t.amount = delta;
        t.ticksLeft = cfg.toastDurationTicks;
        TOASTS.add(t);
    }

    private static boolean isEnderTitle(Component title) {
        return title.getString().equals(Component.translatable("container.enderchest").getString());
    }

    private static int countContainer(AbstractContainerMenu menu, LocalPlayer player) {
        int sum = 0;
        for (Slot slot : menu.slots) {
            if (slot.container == player.getInventory()) {
                continue;
            }
            ItemStack stack = slot.getItem();
            if (stack.isEmpty()) {
                continue;
            }
            Item item = stack.getItem();
            if (item == Items.DIAMOND) {
                sum += stack.getCount();
            } else if (item == Items.DIAMOND_BLOCK) {
                sum += stack.getCount() * 9;
            }
        }
        return sum;
    }

    private static void trackContainer(Minecraft mc, AbstractContainerScreen<?> screen, LocalPlayer player, ClientLevel level) {
        AbstractContainerMenu menu = screen.getMenu();
        if (!(menu instanceof ChestMenu) && !(menu instanceof ShulkerBoxMenu)) {
            return;
        }
        if (openTicks <= 2 && openKey == null) {
            openKey = resolveKey(mc, level);
        }
        int sum = countContainer(menu, player);
        DiamondStore.ServerStore store = DiamondStore.get(loadedServer);
        if (isEnderTitle(screen.getTitle())) {
            if (store.ender != sum) {
                store.ender = sum;
                dirty = true;
            }
        } else if (openKey != null) {
            Integer old = store.chests.get(openKey);
            if (sum <= 0) {
                if (old != null) {
                    store.chests.remove(openKey);
                    dirty = true;
                }
            } else if (old == null || old != sum) {
                store.chests.put(openKey, sum);
                dirty = true;
            }
        }
    }

    /** Block under the crosshair when a container was opened; double chests share one key. */
    private static String resolveKey(Minecraft mc, ClientLevel level) {
        HitResult hr = mc.hitResult;
        if (!(hr instanceof BlockHitResult bhr)) {
            return null;
        }
        BlockPos pos = bhr.getBlockPos();
        BlockState state = level.getBlockState(pos);
        if (state.getBlock() instanceof ChestBlock && state.hasProperty(ChestBlock.TYPE)
                && state.getValue(ChestBlock.TYPE) != ChestType.SINGLE) {
            BlockPos other = pos.relative(ChestBlock.getConnectedDirection(state));
            if (other.getX() < pos.getX()
                    || (other.getX() == pos.getX() && other.getZ() < pos.getZ())
                    || (other.getX() == pos.getX() && other.getZ() == pos.getZ() && other.getY() < pos.getY())) {
                pos = other;
            }
        }
        return level.dimension().toString() + "|" + pos.getX() + "," + pos.getY() + "," + pos.getZ();
    }

    /** Forget chests that were broken (loaded chunk, position is air). */
    private static void validateChests(ClientLevel level) {
        if (loadedServer == null) {
            return;
        }
        String prefix = level.dimension().toString() + "|";
        Map<String, Integer> chests = DiamondStore.get(loadedServer).chests;
        boolean changed = false;
        for (Iterator<Map.Entry<String, Integer>> it = chests.entrySet().iterator(); it.hasNext(); ) {
            String key = it.next().getKey();
            if (!key.startsWith(prefix)) {
                continue;
            }
            try {
                String[] xyz = key.substring(prefix.length()).split(",");
                BlockPos pos = new BlockPos(Integer.parseInt(xyz[0]), Integer.parseInt(xyz[1]), Integer.parseInt(xyz[2]));
                if (level.hasChunkAt(pos) && level.getBlockState(pos).isAir()) {
                    it.remove();
                    changed = true;
                }
            } catch (RuntimeException ignored) {
                it.remove();
                changed = true;
            }
        }
        if (changed) {
            DiamondStore.save();
        }
    }
}
