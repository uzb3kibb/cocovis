package dev.coco.cocovis;

/** Minecraft 26.3 reads keys from an SDL keyboard-state buffer (512 entries); any other index crashes. */
public final class KeyGuard {
    private static boolean warned;

    private KeyGuard() {}

    public static boolean invalid(int key) {
        if (key >= 0 && key < 512) {
            return false;
        }
        if (!warned) {
            warned = true;
            CocoVisClient.LOGGER.warn("CocoVis blocked an invalid key index {} (some mod registered a bad keybind)", key);
        }
        return true;
    }
}
