package vin.howe.soundphysics.mixin;

import vin.howe.soundphysics.SoundPhysics;
import net.minecraft.server.world.ServerWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(ServerWorld.class)
public abstract class ServerWorldMixin {
    @ModifyArg(method = {"playSound", "playSoundFromEntity"},
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/server/PlayerManager;sendToAround(Lnet/minecraft/entity/player/PlayerEntity;DDDDLnet/minecraft/util/registry/RegistryKey;Lnet/minecraft/network/Packet;)V"
            ),
            index = 4)
    private double multiplyDistanceByAllowance(double distance) {
        return distance * SoundPhysics.soundDistanceAllowance;
    }
}