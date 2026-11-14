package dev.coco.cocovis.mixin;

import dev.coco.cocovis.ClanNametag;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EntityRenderer.class)
public abstract class EntityRendererMixin<T extends Entity, S extends EntityRenderState> {

    @Inject(method = "extractRenderState", at = @At("TAIL"))
    private void cocovis$clanNametag(T entity, S state, float partialTick, CallbackInfo ci) {
        if (state.nameTag == null || !(entity instanceof Player player)) {
            return;
        }
        Component tag = ClanNametag.build(player, state.nameTag);
        if (tag != null) {
            state.nameTag = tag;
        }
    }
}
