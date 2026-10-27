package dev.coco.cocovis.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.coco.cocovis.KnifeAnimations;
import net.minecraft.world.entity.HumanoidArm;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Подключает анимации ножа к рендеру предмета в руке.
 * В 26.3 класс называется FirstPersonHandsAndItemsRenderer (раньше ItemInHandRenderer), поэтому цели заданы строками.
 * Pseudo + require = 0: если класса или метода в этой версии нет, вставка молча пропускается, и мод работает без своих анимаций.
 */
@Pseudo
@Mixin(targets = {
        "net.minecraft.client.renderer.FirstPersonHandsAndItemsRenderer",
        "net.minecraft.client.renderer.item.FirstPersonHandsAndItemsRenderer",
        "net.minecraft.client.renderer.ItemInHandRenderer"
})
public abstract class ItemInHandRendererMixin {
    @Inject(method = "applyItemArmTransform", at = @At("TAIL"), require = 0)
    private void cocovis$hold(PoseStack poseStack, HumanoidArm arm, float equipProgress, CallbackInfo ci) {
        KnifeAnimations.hold(poseStack, arm, equipProgress);
    }

    @Inject(method = "applyItemArmAttackTransform", at = @At("HEAD"), cancellable = true, require = 0)
    private void cocovis$attack(PoseStack poseStack, HumanoidArm arm, float swingProgress, CallbackInfo ci) {
        if (KnifeAnimations.attack(poseStack, arm, swingProgress)) ci.cancel();
    }
}
