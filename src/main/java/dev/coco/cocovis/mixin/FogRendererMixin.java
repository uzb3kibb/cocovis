package dev.coco.cocovis.mixin;

import dev.coco.cocovis.CocoVisConfig;
import dev.coco.cocovis.SkyState;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.fog.FogRenderer;
import net.minecraft.world.level.material.FogType;
import org.joml.Vector4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Applies the active sky tint to atmospheric fog so the horizon blends into the sky dome. */
@Mixin(FogRenderer.class)
public abstract class FogRendererMixin {
    @Inject(
            method = "computeFogColor(Lnet/minecraft/client/Camera;FLnet/minecraft/client/multiplayer/ClientLevel;IFLorg/joml/Vector4f;)V",
            at = @At("TAIL"),
            require = 0
    )
    private void cocovis$tintAtmosphericHorizon(
            Camera camera,
            float partialTicks,
            ClientLevel level,
            int renderDistance,
            float darkenWorldAmount,
            Vector4f fogColor,
            CallbackInfo ci
    ) {
        if (!SkyState.colorActive() || camera.getFluidInCamera() != FogType.NONE) {
            return;
        }

        int rgb = SkyState.color();
        float strength = Math.max(0, Math.min(100, CocoVisConfig.get().skyColorIntensity)) / 100f;
        float r = ((rgb >> 16) & 255) / 255f;
        float g = ((rgb >> 8) & 255) / 255f;
        float b = (rgb & 255) / 255f;

        // Blend, don't replace: keeps the original fog's lighting and darkness.
        fogColor.set(
                fogColor.x + (r - fogColor.x) * strength,
                fogColor.y + (g - fogColor.y) * strength,
                fogColor.z + (b - fogColor.z) * strength,
                fogColor.w
        );
    }
}
