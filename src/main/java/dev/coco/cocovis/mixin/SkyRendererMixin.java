package dev.coco.cocovis.mixin;

import dev.coco.cocovis.CocoVisConfig;
import dev.coco.cocovis.SkyState;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.SkyRenderer;
import net.minecraft.client.renderer.state.level.SkyRenderState;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import org.joml.Vector4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SkyRenderer.class)
public abstract class SkyRendererMixin {
    @Inject(method = "extractRenderState", at = @At("TAIL"))
    private void cocovis$tintSky(ClientLevel level, float partialTicks, Camera camera,
                                 SkyRenderState state, CallbackInfo ci) {
        if (!SkyState.colorActive() || state.skyColor == null) {
            return;
        }

        CocoVisConfig cfg = CocoVisConfig.get();
        int rgb = SkyState.color();
        float strength = Math.max(0f, Math.min(100, cfg.skyColorIntensity)) / 100f;
        Vector3fc original = state.skyColor;
        float r = ((rgb >> 16) & 255) / 255f;
        float g = ((rgb >> 8) & 255) / 255f;
        float b = (rgb & 255) / 255f;

        state.skyColor = new Vector3f(
                original.x() + (r - original.x()) * strength,
                original.y() + (g - original.y()) * strength,
                original.z() + (b - original.z()) * strength
        );

        if (state.sunriseAndSunsetColor != null) {
            float alpha = state.sunriseAndSunsetColor.w();
            state.sunriseAndSunsetColor = new Vector4f(r, g, b, alpha * strength);
        }
    }
}
