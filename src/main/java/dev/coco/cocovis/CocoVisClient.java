package dev.coco.cocovis;

import com.mojang.blaze3d.platform.InputConstants;
import dev.coco.cocovis.gui.HudEditor;
import dev.coco.cocovis.gui.VisualsScreen;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;

public class CocoVisClient implements ClientModInitializer {
    public static final String ID = "cocovis";

    @Override
    public void onInitializeClient() {
        DiamondConfig.load();

        KeyMapping.Category category = KeyMapping.Category.register(
                Identifier.fromNamespaceAndPath(ID, "main"));

        KeyMapping openMenu = KeyMappingHelper.registerKeyMapping(
                new KeyMapping("key.cocovis.menu", InputConstants.KEY_RSHIFT, category));
        KeyMapping editHud = KeyMappingHelper.registerKeyMapping(
                new KeyMapping("key.cocovis.edit_hud", InputConstants.KEY_K, category));

        // служебные клавиши выбора ножа и клавиша осмотра: должны быть зарегистрированы до загрузки ресурсов
        Knives.register(category);

        ClientTickEvents.START_CLIENT_TICK.register(client -> Knives.apply());
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            DiamondTracker.tick(client);
            Knives.apply();
            while (Knives.inspectKey().consumeClick()) {
                if (client.gui.screen() == null) Knives.startInspect();
            }
            while (openMenu.consumeClick()) {
                if (client.gui.screen() == null) client.gui.setScreen(new VisualsScreen());
            }
            while (editHud.consumeClick()) {
                if (client.player != null && client.gui.screen() == null) client.gui.setScreen(new HudEditor());
            }
        });
        // при открытии любого экрана игра отпускает все клавиши — возвращаем состояние сразу
        ScreenEvents.AFTER_INIT.register((client, screen, scaledWidth, scaledHeight) -> Knives.apply());

        ItemTooltipCallback.EVENT.register(PriceTooltip::onTooltip);

        HudElementRegistry.attachElementBefore(
                VanillaHudElements.CHAT,
                Identifier.fromNamespaceAndPath(ID, "hud"),
                HudRenderer::render);
    }
}
