package dev.coco.cocovis.mixin;

import dev.coco.cocovis.KnifeAnimations;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Отменяет стандартный поворот руки при ударе, пока в руке наш нож (удар рисуем сами в ItemInHandRendererMixin).
 * Вставка не зависит от списка параметров метода, поэтому не ломается, если в 26.3 у него другая подпись.
 * Это отдельный миксин: если он не применится, хват и осмотр продолжат работать.
 */
@Pseudo
@Mixin(targets = {
        "net.minecraft.client.renderer.FirstPersonHandsAndItemsRenderer",
        "net.minecraft.client.renderer.item.FirstPersonHandsAndItemsRenderer",
        "net.minecraft.client.renderer.ItemInHandRenderer"
})
public abstract class ItemSwingCancelMixin {
    @Inject(method = "applyItemArmAttackTransform", at = @At("HEAD"), cancellable = true, require = 0)
    private void cocovis$cancelSwing(CallbackInfo ci) {
        if (KnifeAnimations.ownsSwing()) {
            KnifeAnimations.noteSwingHook();
            ci.cancel();
        }
    }
}
