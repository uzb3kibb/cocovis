package dev.coco.cocovis.mixin;

import dev.coco.cocovis.SkyState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Minecraft 26.3 stores client clock values in ClientClockManager.ClientClockInstance.
 * The old ClientClockManager#getTotalTicks(Holder) hook no longer applies here:
 * ClientLevel reads ClientClockInstance#totalTicks() instead.
 *
 * This only changes the client-side clock value. The server's clock remains untouched.
 */
@Mixin(targets = "net.minecraft.client.ClientClockManager$ClientClockInstance")
public abstract class ClientClockManagerMixin {

    @Inject(method = "totalTicks()J", at = @At("HEAD"), cancellable = true)
    private void cocovis$overrideSkyTime(CallbackInfoReturnable<Long> cir) {
        if (SkyState.active()) {
            cir.setReturnValue(SkyState.ticks());
        }
    }
}
