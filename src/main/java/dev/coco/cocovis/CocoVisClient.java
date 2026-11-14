package dev.coco.cocovis;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CocoVisClient implements ClientModInitializer {
    public static final String MOD_ID = "cocovis";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitializeClient() {
        CocoVisConfig.load();

        HudElementRegistry.attachElementBefore(
                VanillaHudElements.CHAT,
                Identifier.fromNamespaceAndPath(MOD_ID, "diamonds"),
                (graphics, tickCounter) -> DiamondHud.render(graphics));

        ItemTooltipCallback.EVENT.register(PriceTooltip::onTooltip);

        LOGGER.info("CocoVis 0.2.0 loaded: clan nametags, diamond HUD, price tooltips, motion blur");
    }
}
