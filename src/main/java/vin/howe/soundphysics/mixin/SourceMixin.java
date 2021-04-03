package vin.howe.soundphysics.mixin;

import vin.howe.soundphysics.SoundPhysics;
import net.minecraft.client.sound.Source;
import org.lwjgl.openal.AL10;
import org.lwjgl.openal.AL11;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Source.class)
public abstract class SourceMixin {
    @Shadow
    @Final
    private int pointer;

    @Inject(method = "play", at = @At("HEAD"))
    private void prependPlay(CallbackInfo callbackInfo) {
        float[] positionArray = new float[3];
        AL10.alGetSourcefv(pointer, AL10.AL_POSITION, positionArray);
        SoundPhysics.onPlaySound(positionArray[0], positionArray[1], positionArray[2], pointer);
    }

    /**
     * @author vinhowe
     */
    @Overwrite
    public void setAttenuation(float f) {
        // TODO: Determine if this is the right distance model or if we should go with the default
        AL10.alSourcei(this.pointer, AL10.AL_DISTANCE_MODEL, AL11.AL_LINEAR_DISTANCE);
        AL10.alSourcef(this.pointer, AL10.AL_MAX_DISTANCE, f);
        AL10.alSourcef(this.pointer, AL10.AL_ROLLOFF_FACTOR, SoundPhysics.globalRolloffFactor);
        // TODO: Determine what to do with this
        AL10.alSourcef(this.pointer, AL10.AL_REFERENCE_DISTANCE, 0.0F);
    }
}
