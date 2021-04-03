package vin.howe.soundphysics.mixin;

import vin.howe.soundphysics.SoundPhysics;
import net.minecraft.client.sound.SoundEngine;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SoundEngine.class)
public abstract class SoundEngineMixin {
    @Inject(method = "init", at = @At("TAIL"))
    private void appendInit(CallbackInfo callbackInfo) {
        SoundPhysics.init();
    }
}
