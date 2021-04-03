package vin.howe.soundphysics.mixin;

import vin.howe.soundphysics.SoundPhysics;
import net.minecraft.client.sound.SoundInstance;
import net.minecraft.client.sound.SoundSystem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SoundSystem.class)
public abstract class SoundSystemMixin {

    @Inject(method = "play(Lnet/minecraft/client/sound/SoundInstance;)V", at = @At("TAIL"))
    private void recordLastSoundCategory(SoundInstance soundInstance, CallbackInfo callbackInfo) {
        SoundPhysics.setLastSoundCategory(soundInstance.getCategory());
        SoundPhysics.setLastSoundName(soundInstance.getId().getPath());
        // TODO: What are we actually doing here?
    }

    @Redirect(
            method = "play(Lnet/minecraft/client/sound/SoundInstance;)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/sound/SoundSystem;getAdjustedVolume(Lnet/minecraft/client/sound/SoundInstance;)F"
            )
    )
    private float getAdjustedVolume(SoundSystem soundSystem, SoundInstance soundInstance) {
        return ((IMixinSoundSystem) soundSystem).callGetAdjustedVolume(soundInstance) * SoundPhysics.globalVolumeMultiplier;
    }
}
