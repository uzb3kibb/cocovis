package dev.coco.cocovis.mixin;

import dev.coco.cocovis.SkyState;
import net.minecraft.core.Holder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 26.1+ keeps the time of day in world clocks. Overriding the client clock only changes the sky
 * you see, the server keeps its own time. Both injections are optional (require = 0): only the one
 * matching the real signature is applied, so a different signature can never crash the game.
 */
@Mixin(targets = "net.minecraft.client.ClientClockManager")
public abstract class ClientClockManagerMixin {

    @Inject(method = "getTotalTicks(Lnet/minecraft/core/Holder;)J", at = @At("HEAD"), cancellable = true, require = 0)
    private void cocovis$time(Holder<?> clock, CallbackInfoReturnable<Long> cir) {
        if (SkyState.active()) {
            cir.setReturnValue(SkyState.ticks());
        }
    }

    @Inject(method = "getTotalTicks(Lnet/minecraft/core/Holder$Reference;)J", at = @At("HEAD"), cancellable = true, require = 0)
    private void cocovis$timeRef(Holder.Reference<?> clock, CallbackInfoReturnable<Long> cir) {
        if (SkyState.active()) {
            cir.setReturnValue(SkyState.ticks());
        }
    }
}
