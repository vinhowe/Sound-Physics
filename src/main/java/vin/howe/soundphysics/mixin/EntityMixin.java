package vin.howe.soundphysics.mixin;

import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(Entity.class)
public abstract class EntityMixin {
    // TODO: Figure this out
//    @Shadow private Vec3d pos;
//
//    @Inject(
//            method = "playSound",
//            at = @At(
//                    value = "INVOKE",
//                    target = "Lnet/minecraft/world/World;playSound(Lnet/minecraft/entity/player/PlayerEntity;DDDLnet/minecraft/sound/SoundEvent;Lnet/minecraft/sound/SoundCategory;FF)V"
//            )
//    )
//    private void playSound(SoundEvent sound, float volume, float pitch, CallbackInfo ci) {
//        // This is an incredibly hacky way to do this and is anything but stable
//        this.pos.add(0, SoundPhysics.calculateEntitySoundOffset(((Entity) (Object) this), sound), 0);
//    }
}
