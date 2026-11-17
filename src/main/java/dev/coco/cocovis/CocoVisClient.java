package dev.coco.cocovis;

import dev.coco.cocovis.gui.HudEditor;
import dev.coco.cocovis.gui.VisualsScreen;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CocoVisClient implements ClientModInitializer {
    public static final String MOD_ID = "cocovis";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    // Minecraft 26.x reads keys through SDL: these are SDL scancodes, not GLFW codes.
    private static final int KEY_RIGHT_SHIFT = 229;
    private static final int KEY_K = 14;

    private static KeyMapping menuKey;
    private static KeyMapping editKey;

    @Override
    public void onInitializeClient() {
        CocoVisConfig.load();
        DiamondStore.load();

        KeyMapping.Category category = KeyMapping.Category.register(Identifier.fromNamespaceAndPath(MOD_ID, "main"));
        menuKey = KeyMappingHelper.registerKeyMapping(new KeyMapping("key.cocovis.menu", KEY_RIGHT_SHIFT, category));
        editKey = KeyMappingHelper.registerKeyMapping(new KeyMapping("key.cocovis.edit_hud", KEY_K, category));

        ClientTickEvents.START_CLIENT_TICK.register(client -> {
            while (menuKey.consumeClick()) {
                if (client.gui.screen() == null) {
                    client.gui.setScreen(new VisualsScreen());
                }
            }
            while (editKey.consumeClick()) {
                if (client.gui.screen() == null) {
                    client.gui.setScreen(new HudEditor());
                }
            }
        });
        ClientTickEvents.END_CLIENT_TICK.register(DiamondTracker::tick);

        HudElementRegistry.attachElementBefore(
                VanillaHudElements.CHAT,
                Identifier.fromNamespaceAndPath(MOD_ID, "hud"),
                (graphics, tickCounter) -> {
                    Minecraft mc = Minecraft.getInstance();
                    if (mc.player == null || mc.gui.screen() instanceof HudEditor) {
                        return;
                    }
                    InfoHud.render(graphics, mc);
                    EffectsHud.render(graphics, mc);
                    DiamondHud.render(graphics, mc);
                });

        ItemTooltipCallback.EVENT.register(PriceTooltip::onTooltip);

        LOGGER.info("CocoVis 0.0.4 loaded");
    }
}
