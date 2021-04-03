package vin.howe.soundphysics.mixin;

import net.minecraft.client.sound.SoundInstance;
import net.minecraft.client.sound.SoundSystem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(SoundSystem.class)
public interface IMixinSoundSystem {
    @Invoker
    float callGetAdjustedVolume(SoundInstance instance);
}
