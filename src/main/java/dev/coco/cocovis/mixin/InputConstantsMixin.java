package dev.coco.cocovis.mixin;

import com.mojang.blaze3d.platform.InputConstants;
import dev.coco.cocovis.KeyGuard;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Safety net: a bad keybind (negative or out of range) makes KeyMapping.setAll crash the game
 * when joining a world. Both injections are optional (require = 0): only the one that matches
 * the real method signature is applied.
 */
@Mixin(InputConstants.class)
public abstract class InputConstantsMixin {

    @Inject(method = "isKeyDown(I)Z", at = @At("HEAD"), cancellable = true, require = 0)
    private static void cocovis$guardKey(int key, CallbackInfoReturnable<Boolean> cir) {
        if (KeyGuard.invalid(key)) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "isKeyDown(JI)Z", at = @At("HEAD"), cancellable = true, require = 0)
    private static void cocovis$guardKeyWindow(long window, int key, CallbackInfoReturnable<Boolean> cir) {
        if (KeyGuard.invalid(key)) {
            cir.setReturnValue(false);
        }
    }
}
