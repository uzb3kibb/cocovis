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
 * Хват, выхват, удар и осмотр ножа: подключается после того, как игра поставила предмет в точку руки.
 * В 26.3 класс называется FirstPersonHandsAndItemsRenderer (раньше ItemInHandRenderer), поэтому цели заданы строками.
 * Pseudo + require = 0: если класса или метода в этой версии нет, вставка молча пропускается.
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
}
