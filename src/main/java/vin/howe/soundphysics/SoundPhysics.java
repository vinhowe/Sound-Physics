package vin.howe.soundphysics;

import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.serializer.JanksonConfigSerializer;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.sound.SoundCategory;

import net.minecraft.sound.SoundEvent;
import net.minecraft.util.ActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import org.lwjgl.openal.AL10;
import org.lwjgl.openal.AL11;
import org.lwjgl.openal.ALC10;
import org.lwjgl.openal.EXTEfx;

import java.util.regex.Pattern;

@Environment(EnvType.CLIENT)
public class SoundPhysics implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        AutoConfig.register(SoundPhysicsConfig.class, JanksonConfigSerializer::new)
                .registerSaveListener((manager, data) -> {
                    applyConfigChanges();
                    return ActionResult.PASS;
                });
    }

    public static final String MOD_ID = "soundphysics";

    private static final Pattern rainPattern = Pattern.compile(".*rain.*");
    private static final Pattern stepPattern = Pattern.compile(".*step.*");
    private static final Pattern blockPattern = Pattern.compile(".*block.*");

    private static final String logPrefix = "[SOUND PHYSICS]";
    private static int auxFXSlot0;
    private static int auxFXSlot1;
    private static int auxFXSlot2;
    private static int auxFXSlot3;
    private static int reverb0;
    private static int reverb1;
    private static int reverb2;
    private static int reverb3;
    private static int directFilter0;
    private static int sendFilter0;
    private static int sendFilter1;
    private static int sendFilter2;
    private static int sendFilter3;

    private static MinecraftClient mc;

    private static SoundCategory lastSoundCategory;
    private static String lastSoundName;
    // THESE VARIABLES ARE CONSTANTLY ACCESSED AND USED BY ASM INJECTED CODE! DO
    // NOT REMOVE!
    // TODO: FIX THIS
    public static int attenuationModel = 1; //SoundSystemConfig.ATTENUATION_ROLLOFF;
    public static float globalRolloffFactor = SoundPhysicsConfig.General.rolloffFactor;
    public static float globalVolumeMultiplier = 4.0f;
    public static float globalReverbMultiplier = 0.7f * SoundPhysicsConfig.General.globalReverbGain;
    public static double soundDistanceAllowance = SoundPhysicsConfig.General.soundDistanceAllowance;

    /**
     * CALLED BY ASM INJECTED CODE!
     */
    public static void init() {
        setupEFX();
        mc = MinecraftClient.getInstance();
    }

    public static void applyConfigChanges() {
        globalRolloffFactor = SoundPhysicsConfig.General.rolloffFactor;
        globalReverbMultiplier = 0.7f * SoundPhysicsConfig.General.globalReverbGain;
        soundDistanceAllowance = SoundPhysicsConfig.General.soundDistanceAllowance;

        if (auxFXSlot0 != 0) {
            // Set the global reverb parameters and apply them to the effect and
            // effectslot
            setReverbParams(ReverbParams.getReverb0(), auxFXSlot0, reverb0);
            setReverbParams(ReverbParams.getReverb1(), auxFXSlot1, reverb1);
            setReverbParams(ReverbParams.getReverb2(), auxFXSlot2, reverb2);
            setReverbParams(ReverbParams.getReverb3(), auxFXSlot3, reverb3);
        }
    }

    private static void setupEFX() {
        // Get current context and device
        final long currentContext = ALC10.alcGetCurrentContext();
        final long currentDevice = ALC10.alcGetContextsDevice(currentContext);

        if (ALC10.alcIsExtensionPresent(currentDevice, "ALC_EXT_EFX")) {
            log("EFX Extension recognized.");
        } else {
            logError("EFX Extension not found on current device. Aborting.");
            return;
        }

        // Create auxiliary effect slots
        auxFXSlot0 = EXTEfx.alGenAuxiliaryEffectSlots();
        log("Aux slot " + auxFXSlot0 + " created");
        EXTEfx.alAuxiliaryEffectSloti(auxFXSlot0, EXTEfx.AL_EFFECTSLOT_AUXILIARY_SEND_AUTO, AL10.AL_TRUE);

        auxFXSlot1 = EXTEfx.alGenAuxiliaryEffectSlots();
        log("Aux slot " + auxFXSlot1 + " created");
        EXTEfx.alAuxiliaryEffectSloti(auxFXSlot1, EXTEfx.AL_EFFECTSLOT_AUXILIARY_SEND_AUTO, AL10.AL_TRUE);

        auxFXSlot2 = EXTEfx.alGenAuxiliaryEffectSlots();
        log("Aux slot " + auxFXSlot2 + " created");
        EXTEfx.alAuxiliaryEffectSloti(auxFXSlot2, EXTEfx.AL_EFFECTSLOT_AUXILIARY_SEND_AUTO, AL10.AL_TRUE);

        auxFXSlot3 = EXTEfx.alGenAuxiliaryEffectSlots();
        log("Aux slot " + auxFXSlot3 + " created");
        EXTEfx.alAuxiliaryEffectSloti(auxFXSlot3, EXTEfx.AL_EFFECTSLOT_AUXILIARY_SEND_AUTO, AL10.AL_TRUE);
        checkErrorLog("Failed creating auxiliary effect slots!");

        reverb0 = EXTEfx.alGenEffects();
        EXTEfx.alEffecti(reverb0, EXTEfx.AL_EFFECT_TYPE, EXTEfx.AL_EFFECT_EAXREVERB);
        checkErrorLog("Failed creating reverb effect slot 0!");
        reverb1 = EXTEfx.alGenEffects();
        EXTEfx.alEffecti(reverb1, EXTEfx.AL_EFFECT_TYPE, EXTEfx.AL_EFFECT_EAXREVERB);
        checkErrorLog("Failed creating reverb effect slot 1!");
        reverb2 = EXTEfx.alGenEffects();
        EXTEfx.alEffecti(reverb2, EXTEfx.AL_EFFECT_TYPE, EXTEfx.AL_EFFECT_EAXREVERB);
        checkErrorLog("Failed creating reverb effect slot 2!");
        reverb3 = EXTEfx.alGenEffects();
        EXTEfx.alEffecti(reverb3, EXTEfx.AL_EFFECT_TYPE, EXTEfx.AL_EFFECT_EAXREVERB);
        checkErrorLog("Failed creating reverb effect slot 3!");

        // Create filters
        directFilter0 = EXTEfx.alGenFilters();
        EXTEfx.alFilteri(directFilter0, EXTEfx.AL_FILTER_TYPE, EXTEfx.AL_FILTER_LOWPASS);

        sendFilter0 = EXTEfx.alGenFilters();
        EXTEfx.alFilteri(sendFilter0, EXTEfx.AL_FILTER_TYPE, EXTEfx.AL_FILTER_LOWPASS);

        sendFilter1 = EXTEfx.alGenFilters();
        EXTEfx.alFilteri(sendFilter1, EXTEfx.AL_FILTER_TYPE, EXTEfx.AL_FILTER_LOWPASS);

        sendFilter2 = EXTEfx.alGenFilters();
        EXTEfx.alFilteri(sendFilter2, EXTEfx.AL_FILTER_TYPE, EXTEfx.AL_FILTER_LOWPASS);

        sendFilter3 = EXTEfx.alGenFilters();
        EXTEfx.alFilteri(sendFilter3, EXTEfx.AL_FILTER_TYPE, EXTEfx.AL_FILTER_LOWPASS);
        checkErrorLog("Error creating lowpass filters!");

        applyConfigChanges();
    }

    /**
     * CALLED BY ASM INJECTED CODE!
     */
    public static void setLastSoundCategory(final SoundCategory sc) {
        lastSoundCategory = sc;
    }

    /**
     * CALLED BY ASM INJECTED CODE!
     */
    public static void setLastSoundName(final String name) {
        lastSoundName = name;
    }

    /**
     * CALLED BY ASM INJECTED CODE!
     */
    public static void onPlaySound(final float posX, final float posY, final float posZ, final int sourceID) {
        evaluateEnvironment(sourceID, posX, posY, posZ);
    }

    /**
     * CALLED BY ASM INJECTED CODE!
     */
    public static double calculateEntitySoundOffset(final Entity entity, final SoundEvent sound) {
        if (stepPattern.matcher(sound.getId().getPath()).matches()) {
            return 0;
        }

        return entity.getEyeHeight(entity.getPose());
    }

    @SuppressWarnings("deprecation")
    private static float getBlockReflectivity(final BlockPos blockPos) {
        final BlockState blockState = mc.world.getBlockState(blockPos);
        final Block block = blockState.getBlock();
        final BlockSoundGroup soundType = block.getSoundGroup(blockState);

        float reflectivity = 0.5f;

        if (soundType == BlockSoundGroup.STONE) {
            reflectivity = SoundPhysicsConfig.MaterialProperties.stoneReflectivity;
        } else if (soundType == BlockSoundGroup.WOOD) {
            reflectivity = SoundPhysicsConfig.MaterialProperties.woodReflectivity;
        } else if (soundType == BlockSoundGroup.GRASS) {
            reflectivity = SoundPhysicsConfig.MaterialProperties.groundReflectivity;
        } else if (soundType == BlockSoundGroup.CROP) {
            reflectivity = SoundPhysicsConfig.MaterialProperties.plantReflectivity;
        } else if (soundType == BlockSoundGroup.METAL) {
            reflectivity = SoundPhysicsConfig.MaterialProperties.metalReflectivity;
        } else if (soundType == BlockSoundGroup.GLASS) {
            reflectivity = SoundPhysicsConfig.MaterialProperties.glassReflectivity;
        } else if (soundType == BlockSoundGroup.WOOL) {
            reflectivity = SoundPhysicsConfig.MaterialProperties.clothReflectivity;
        } else if (soundType == BlockSoundGroup.SAND) {
            reflectivity = SoundPhysicsConfig.MaterialProperties.sandReflectivity;
        } else if (soundType == BlockSoundGroup.SNOW) {
            reflectivity = SoundPhysicsConfig.MaterialProperties.snowReflectivity;
        } else if (soundType == BlockSoundGroup.LADDER) {
            reflectivity = SoundPhysicsConfig.MaterialProperties.woodReflectivity;
        } else if (soundType == BlockSoundGroup.ANVIL) {
            reflectivity = SoundPhysicsConfig.MaterialProperties.metalReflectivity;
        }

        reflectivity *= SoundPhysicsConfig.General.globalBlockReflectance;

        return reflectivity;
    }

    private static Vec3d getNormalFromFacing(final Direction sideHit) {
        return new Vec3d(sideHit.getUnitVector());
    }

    private static Vec3d reflect(final Vec3d dir, final Vec3d normal) {
        final double dot2 = dir.dotProduct(normal) * 2;

        final double x = dir.x - dot2 * normal.x;
        final double y = dir.y - dot2 * normal.y;
        final double z = dir.z - dot2 * normal.z;

        return new Vec3d(x, y, z);
    }

    private static Vec3d offsetSoundByName(final double soundX, final double soundY, final double soundZ,
                                           final Vec3d playerPos, final String name, final SoundCategory category) {
        double offsetX = 0.0;
        double offsetY = 0.0;
        double offsetZ = 0.0;
        double offsetTowardsPlayer = 0.0;

        double tempNormX = 0;
        double tempNormY = 0;
        double tempNormZ = 0;

        if (soundY % 1.0 < 0.001 || stepPattern.matcher(name).matches()) {
            offsetY = 0.1;
        }

        if (category == SoundCategory.BLOCKS || blockPattern.matcher(name).matches()) {
            // The ray will probably hit the block that it's emitting from
            // before
            // escaping. Offset the ray start position towards the player by the
            // diagonal half length of a cube

            tempNormX = playerPos.x - soundX;
            tempNormY = playerPos.y - soundY;
            tempNormZ = playerPos.z - soundZ;
            final double length = Math.sqrt(tempNormX * tempNormX + tempNormY * tempNormY + tempNormZ * tempNormZ);
            tempNormX /= length;
            tempNormY /= length;
            tempNormZ /= length;
            // 0.867 > square root of 0.5^2 * 3
            offsetTowardsPlayer = 0.867;
            offsetX += tempNormX * offsetTowardsPlayer;
            offsetY += tempNormY * offsetTowardsPlayer;
            offsetZ += tempNormZ * offsetTowardsPlayer;
        }

        return new Vec3d(soundX + offsetX, soundY + offsetY, soundZ + offsetZ);
    }

    @SuppressWarnings("deprecation")
    private static void evaluateEnvironment(final int sourceID, final float posX, final float posY, final float posZ) {
        if (mc.player == null | mc.world == null | posY <= 0 | lastSoundCategory == SoundCategory.RECORDS
                | lastSoundCategory == SoundCategory.MUSIC) {
            // posY <= 0 as a condition has to be there: Ingame
            // menu clicks do have a player and world present
            setEnvironment(sourceID, 0.0f, 0.0f, 0.0f, 0.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f);
            return;
        }

        final boolean isRain = rainPattern.matcher(lastSoundName).matches();

        if (SoundPhysicsConfig.Performance.skipRainOcclusionTracing && isRain) {
            setEnvironment(sourceID, 0.0f, 0.0f, 0.0f, 0.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f);
            return;
        }

        float directCutoff = 1.0f;
        final float absorptionCoeff = SoundPhysicsConfig.General.globalBlockAbsorption * 3.0f;

        final Vec3d playerPos = new Vec3d(mc.player.getX(), mc.player.getY() + mc.player.getEyeHeight(mc.player.getPose()), mc.player.getY());
        final Vec3d soundPos = offsetSoundByName(posX, posY, posZ, playerPos, lastSoundName, lastSoundCategory);
        final Vec3d normalToPlayer = playerPos.subtract(soundPos).normalize();

        Vec3d rayOrigin = soundPos;

        float occlusionAccumulation = 0.0f;

        for (int i = 0; i < 10; i++) {
            final BlockHitResult rayHit = mc.world.raycast(new RaycastContext(rayOrigin, playerPos, RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, mc.player));

            if (rayHit == null) {
                break;
            }

            final BlockState blockHitState = mc.world.getBlockState(rayHit.getBlockPos());

            float blockOcclusion = 1.0f;

            if (blockHitState.isOpaqueFullCube(mc.world, rayHit.getBlockPos())) {
                // log("not a solid block!");
                blockOcclusion *= 0.15f;
            }

            occlusionAccumulation += blockOcclusion;

            rayOrigin = new Vec3d(rayHit.getPos().x + normalToPlayer.x * 0.1, rayHit.getPos().y + normalToPlayer.y * 0.1,
                    rayHit.getPos().z + normalToPlayer.z * 0.1);
        }

        directCutoff = (float) Math.exp(-occlusionAccumulation * absorptionCoeff);
        float directGain = (float) Math.pow(directCutoff, 0.1);

        // Calculate reverb parameters for this sound
        float sendGain0 = 0.0f;
        float sendGain1 = 0.0f;
        float sendGain2 = 0.0f;
        float sendGain3 = 0.0f;

        float sendCutoff0 = 1.0f;
        float sendCutoff1 = 1.0f;
        float sendCutoff2 = 1.0f;
        float sendCutoff3 = 1.0f;

        if (mc.player.isSubmergedInWater()) {
            directCutoff *= 1.0f - SoundPhysicsConfig.General.underwaterFilter;
        }

        if (isRain) {
            setEnvironment(sourceID, sendGain0, sendGain1, sendGain2, sendGain3, sendCutoff0, sendCutoff1, sendCutoff2,
                    sendCutoff3, directCutoff, directGain);
            return;
        }

        // Shoot rays around sound
        final float phi = 1.618033988f;
        final float gAngle = phi * (float) Math.PI * 2.0f;
        final float maxDistance = 256.0f;

        final int numRays = SoundPhysicsConfig.Performance.environmentEvaluationRays;
        final int rayBounces = 4;

        final float[] bounceReflectivityRatio = new float[rayBounces];

        float sharedAirspace = 0.0f;

        final float rcpTotalRays = 1.0f / (numRays * rayBounces);
        final float rcpPrimaryRays = 1.0f / numRays;

        for (int i = 0; i < numRays; i++) {
            final float fi = i;
            final float fiN = fi / numRays;
            final float longitude = gAngle * fi;
            final float latitude = (float) Math.asin(fiN * 2.0f - 1.0f);

            final Vec3d rayDir = new Vec3d(Math.cos(latitude) * Math.cos(longitude),
                    Math.cos(latitude) * Math.sin(longitude), Math.sin(latitude));

            final Vec3d rayStart = new Vec3d(soundPos.x, soundPos.y, soundPos.z);

            final Vec3d rayEnd = new Vec3d(rayStart.x + rayDir.x * maxDistance, rayStart.y + rayDir.y * maxDistance,
                    rayStart.z + rayDir.z * maxDistance);

            final BlockHitResult rayHit = mc.world.raycast(new RaycastContext(rayStart, rayEnd, RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, mc.player));

            if (rayHit != null) {
                final double rayLength = soundPos.distanceTo(rayHit.getPos());

                // Additional bounces
                BlockPos lastHitBlock = rayHit.getBlockPos();
                Vec3d lastHitPos = rayHit.getPos();
                Vec3d lastHitNormal = getNormalFromFacing(rayHit.getSide());
                Vec3d lastRayDir = rayDir;

                float totalRayDistance = (float) rayLength;

                // Secondary ray bounces
                for (int j = 0; j < rayBounces; j++) {
                    final Vec3d newRayDir = reflect(lastRayDir, lastHitNormal);
                    // Vec3d newRayDir = lastHitNormal;
                    final Vec3d newRayStart = new Vec3d(lastHitPos.x + lastHitNormal.x * 0.01,
                            lastHitPos.y + lastHitNormal.y * 0.01, lastHitPos.z + lastHitNormal.z * 0.01);
                    final Vec3d newRayEnd = new Vec3d(newRayStart.x + newRayDir.x * maxDistance,
                            newRayStart.y + newRayDir.y * maxDistance, newRayStart.z + newRayDir.z * maxDistance);

                    final BlockHitResult newRayHit = mc.world.raycast(new RaycastContext(newRayStart, newRayEnd, RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, mc.player));

                    float energyTowardsPlayer = 0.25f;
                    final float blockReflectivity = getBlockReflectivity(lastHitBlock);
                    energyTowardsPlayer *= blockReflectivity * 0.75f + 0.25f;

                    if (newRayHit == null) {
                        totalRayDistance += lastHitPos.distanceTo(playerPos);
                    } else {
                        final double newRayLength = lastHitPos.distanceTo(newRayHit.getPos());

                        bounceReflectivityRatio[j] += blockReflectivity;

                        totalRayDistance += newRayLength;

                        lastHitPos = newRayHit.getPos();
                        lastHitNormal = getNormalFromFacing(newRayHit.getSide());
                        lastRayDir = newRayDir;
                        lastHitBlock = newRayHit.getBlockPos();

                        // Cast one final ray towards the player. If it's
                        // unobstructed, then the sound source and the player
                        // share airspace.
                        if (!SoundPhysicsConfig.Performance.simplerSharedAirspaceSimulation || j == rayBounces - 1) {
                            final Vec3d finalRayStart = new Vec3d(lastHitPos.x + lastHitNormal.x * 0.01,
                                    lastHitPos.y + lastHitNormal.y * 0.01, lastHitPos.z + lastHitNormal.z * 0.01);

                            final BlockHitResult finalRayHit = mc.world.raycast(new RaycastContext(finalRayStart, playerPos, RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, mc.player));

                            if (finalRayHit == null) {
                                // log("Secondary ray hit the player!");
                                sharedAirspace += 1.0f;
                            }
                        }
                    }

                    final float reflectionDelay = (float) Math.max(totalRayDistance, 0.0) * 0.12f * blockReflectivity;

                    final float cross0 = 1.0f - MathHelper.clamp(Math.abs(reflectionDelay - 0.0f), 0.0f, 1.0f);
                    final float cross1 = 1.0f - MathHelper.clamp(Math.abs(reflectionDelay - 1.0f), 0.0f, 1.0f);
                    final float cross2 = 1.0f - MathHelper.clamp(Math.abs(reflectionDelay - 2.0f), 0.0f, 1.0f);
                    final float cross3 = MathHelper.clamp(reflectionDelay - 2.0f, 0.0f, 1.0f);

                    sendGain0 += cross0 * energyTowardsPlayer * 6.4f * rcpTotalRays;
                    sendGain1 += cross1 * energyTowardsPlayer * 12.8f * rcpTotalRays;
                    sendGain2 += cross2 * energyTowardsPlayer * 12.8f * rcpTotalRays;
                    sendGain3 += cross3 * energyTowardsPlayer * 12.8f * rcpTotalRays;

                    // Nowhere to bounce off of, stop bouncing!
                    if (newRayHit == null) {
                        break;
                    }
                }
            }

        }

        // log("total reflectivity ratio: " + totalReflectivityRatio);

        bounceReflectivityRatio[0] = bounceReflectivityRatio[0] / numRays;
        bounceReflectivityRatio[1] = bounceReflectivityRatio[1] / numRays;
        bounceReflectivityRatio[2] = bounceReflectivityRatio[2] / numRays;
        bounceReflectivityRatio[3] = bounceReflectivityRatio[3] / numRays;

        sharedAirspace *= 64.0f;

        if (SoundPhysicsConfig.Performance.simplerSharedAirspaceSimulation) {
            sharedAirspace *= rcpPrimaryRays;
        } else {
            sharedAirspace *= rcpTotalRays;
        }

        final float sharedAirspaceWeight0 = MathHelper.clamp(sharedAirspace / 20.0f, 0.0f, 1.0f);
        final float sharedAirspaceWeight1 = MathHelper.clamp(sharedAirspace / 15.0f, 0.0f, 1.0f);
        final float sharedAirspaceWeight2 = MathHelper.clamp(sharedAirspace / 10.0f, 0.0f, 1.0f);
        final float sharedAirspaceWeight3 = MathHelper.clamp(sharedAirspace / 10.0f, 0.0f, 1.0f);

        sendCutoff0 = (float) Math.exp(-occlusionAccumulation * absorptionCoeff * 1.0f) * (1.0f - sharedAirspaceWeight0)
                + sharedAirspaceWeight0;
        sendCutoff1 = (float) Math.exp(-occlusionAccumulation * absorptionCoeff * 1.0f) * (1.0f - sharedAirspaceWeight1)
                + sharedAirspaceWeight1;
        sendCutoff2 = (float) Math.exp(-occlusionAccumulation * absorptionCoeff * 1.5f) * (1.0f - sharedAirspaceWeight2)
                + sharedAirspaceWeight2;
        sendCutoff3 = (float) Math.exp(-occlusionAccumulation * absorptionCoeff * 1.5f) * (1.0f - sharedAirspaceWeight3)
                + sharedAirspaceWeight3;

        // attempt to preserve directionality when airspace is shared by
        // allowing some of the dry signal through but filtered
        final float averageSharedAirspace = (sharedAirspaceWeight0 + sharedAirspaceWeight1 + sharedAirspaceWeight2
                + sharedAirspaceWeight3) * 0.25f;
        directCutoff = Math.max((float) Math.pow(averageSharedAirspace, 0.5) * 0.2f, directCutoff);

        directGain = (float) Math.pow(directCutoff, 0.1);

        sendGain1 *= bounceReflectivityRatio[1];
        sendGain2 *= (float) Math.pow(bounceReflectivityRatio[2], 3.0);
        sendGain3 *= (float) Math.pow(bounceReflectivityRatio[3], 4.0);

        sendGain0 = MathHelper.clamp(sendGain0, 0.0f, 1.0f);
        sendGain1 = MathHelper.clamp(sendGain1, 0.0f, 1.0f);
        sendGain2 = MathHelper.clamp(sendGain2 * 1.05f - 0.05f, 0.0f, 1.0f);
        sendGain3 = MathHelper.clamp(sendGain3 * 1.05f - 0.05f, 0.0f, 1.0f);

        sendGain0 *= (float) Math.pow(sendCutoff0, 0.1);
        sendGain1 *= (float) Math.pow(sendCutoff1, 0.1);
        sendGain2 *= (float) Math.pow(sendCutoff2, 0.1);
        sendGain3 *= (float) Math.pow(sendCutoff3, 0.1);

        if (mc.player.isSubmergedInWater()) {
            sendCutoff0 *= 0.4f;
            sendCutoff1 *= 0.4f;
            sendCutoff2 *= 0.4f;
            sendCutoff3 *= 0.4f;
        }

        setEnvironment(sourceID, sendGain0, sendGain1, sendGain2, sendGain3, sendCutoff0, sendCutoff1, sendCutoff2,
                sendCutoff3, directCutoff, directGain);
    }

    private static void setEnvironment(final int sourceID, final float sendGain0, final float sendGain1,
                                       final float sendGain2, final float sendGain3, final float sendCutoff0, final float sendCutoff1,
                                       final float sendCutoff2, final float sendCutoff3, final float directCutoff, final float directGain) {
        // Set reverb send filter values and set source to send to all reverb fx
        // slots
        EXTEfx.alFilterf(sendFilter0, EXTEfx.AL_LOWPASS_GAIN, sendGain0);
        EXTEfx.alFilterf(sendFilter0, EXTEfx.AL_LOWPASS_GAINHF, sendCutoff0);
        AL11.alSource3i(sourceID, EXTEfx.AL_AUXILIARY_SEND_FILTER, auxFXSlot0, 0, sendFilter0);

        EXTEfx.alFilterf(sendFilter1, EXTEfx.AL_LOWPASS_GAIN, sendGain1);
        EXTEfx.alFilterf(sendFilter1, EXTEfx.AL_LOWPASS_GAINHF, sendCutoff1);
        AL11.alSource3i(sourceID, EXTEfx.AL_AUXILIARY_SEND_FILTER, auxFXSlot1, 1, sendFilter1);

//        EXTEfx.alFilterf(sendFilter2, EXTEfx.AL_LOWPASS_GAIN, sendGain2);
//        EXTEfx.alFilterf(sendFilter2, EXTEfx.AL_LOWPASS_GAINHF, sendCutoff2);
//        AL11.alSource3i(sourceID, EXTEfx.AL_AUXILIARY_SEND_FILTER, auxFXSlot2, 2, sendFilter2);
//
//        EXTEfx.alFilterf(sendFilter3, EXTEfx.AL_LOWPASS_GAIN, sendGain3);
//        EXTEfx.alFilterf(sendFilter3, EXTEfx.AL_LOWPASS_GAINHF, sendCutoff3);
//        AL11.alSource3i(sourceID, EXTEfx.AL_AUXILIARY_SEND_FILTER, auxFXSlot3, 3, sendFilter3);

//        EXTEfx.alFilterf(sendFilter0, EXTEfx.AL_LOWPASS_GAIN, sendGain2);
//        EXTEfx.alFilterf(sendFilter0, EXTEfx.AL_LOWPASS_GAINHF, sendCutoff2);
//        AL11.alSource3i(sourceID, EXTEfx.AL_AUXILIARY_SEND_FILTER, auxFXSlot2, 0, sendFilter2);
//
//        EXTEfx.alFilterf(sendFilter1, EXTEfx.AL_LOWPASS_GAIN, sendGain3);
//        EXTEfx.alFilterf(sendFilter1, EXTEfx.AL_LOWPASS_GAINHF, sendCutoff3);
//        AL11.alSource3i(sourceID, EXTEfx.AL_AUXILIARY_SEND_FILTER, auxFXSlot3, 1, sendFilter3);

        EXTEfx.alFilterf(directFilter0, EXTEfx.AL_LOWPASS_GAIN, directGain);
        EXTEfx.alFilterf(directFilter0, EXTEfx.AL_LOWPASS_GAINHF, directCutoff);
        AL10.alSourcei(sourceID, EXTEfx.AL_DIRECT_FILTER, directFilter0);
//
        AL10.alSourcef(sourceID, EXTEfx.AL_AIR_ABSORPTION_FACTOR, SoundPhysicsConfig.General.airAbsorption);
    }

    /**
     * Applies the parameters in the enum ReverbParams to the main reverb
     * effect.
     */
    protected static void setReverbParams(final ReverbParams r, final int auxFXSlot, final int reverbSlot) {
        EXTEfx.alEffectf(reverbSlot, EXTEfx.AL_EAXREVERB_DENSITY, r.density);
        checkErrorLog("Error while assigning reverb density: " + r.density);

        EXTEfx.alEffectf(reverbSlot, EXTEfx.AL_EAXREVERB_DIFFUSION, r.diffusion);
        checkErrorLog("Error while assigning reverb diffusion: " + r.diffusion);

        EXTEfx.alEffectf(reverbSlot, EXTEfx.AL_EAXREVERB_GAIN, r.gain);
        checkErrorLog("Error while assigning reverb gain: " + r.gain);

        EXTEfx.alEffectf(reverbSlot, EXTEfx.AL_EAXREVERB_GAINHF, r.gainHF);
        checkErrorLog("Error while assigning reverb gainHF: " + r.gainHF);

        EXTEfx.alEffectf(reverbSlot, EXTEfx.AL_EAXREVERB_DECAY_TIME, r.decayTime);
        checkErrorLog("Error while assigning reverb decayTime: " + r.decayTime);

        EXTEfx.alEffectf(reverbSlot, EXTEfx.AL_EAXREVERB_DECAY_HFRATIO, r.decayHFRatio);
        checkErrorLog("Error while assigning reverb decayHFRatio: " + r.decayHFRatio);

        EXTEfx.alEffectf(reverbSlot, EXTEfx.AL_EAXREVERB_REFLECTIONS_GAIN, r.reflectionsGain);
        checkErrorLog("Error while assigning reverb reflectionsGain: " + r.reflectionsGain);

        EXTEfx.alEffectf(reverbSlot, EXTEfx.AL_EAXREVERB_LATE_REVERB_GAIN, r.lateReverbGain);
        checkErrorLog("Error while assigning reverb lateReverbGain: " + r.lateReverbGain);

        EXTEfx.alEffectf(reverbSlot, EXTEfx.AL_EAXREVERB_LATE_REVERB_DELAY, r.lateReverbDelay);
        checkErrorLog("Error while assigning reverb lateReverbDelay: " + r.lateReverbDelay);

        EXTEfx.alEffectf(reverbSlot, EXTEfx.AL_EAXREVERB_AIR_ABSORPTION_GAINHF, r.airAbsorptionGainHF);
        checkErrorLog("Error while assigning reverb airAbsorptionGainHF: " + r.airAbsorptionGainHF);

        EXTEfx.alEffectf(reverbSlot, EXTEfx.AL_EAXREVERB_ROOM_ROLLOFF_FACTOR, r.roomRolloffFactor);
        checkErrorLog("Error while assigning reverb roomRolloffFactor: " + r.roomRolloffFactor);

        // Attach updated effect object
        EXTEfx.alAuxiliaryEffectSloti(auxFXSlot, EXTEfx.AL_EFFECTSLOT_EFFECT, reverbSlot);
    }

    public static void log(final String message) {
        System.out.println(logPrefix.concat(" : ").concat(message));
    }

    public static void logError(final String errorMessage) {
        System.out.println(logPrefix.concat(" [ERROR]: ").concat(errorMessage));
    }

    protected static boolean checkErrorLog(final String errorMessage) {
        final int error = AL10.alGetError();
        if (error == AL10.AL_NO_ERROR) {
            return false;
        }

        String errorName;

        switch (error) {
            case AL10.AL_INVALID_NAME:
                errorName = "AL_INVALID_NAME";
                break;
            case AL10.AL_INVALID_ENUM:
                errorName = "AL_INVALID_ENUM";
                break;
            case AL10.AL_INVALID_VALUE:
                errorName = "AL_INVALID_VALUE";
                break;
            case AL10.AL_INVALID_OPERATION:
                errorName = "AL_INVALID_OPERATION";
                break;
            case AL10.AL_OUT_OF_MEMORY:
                errorName = "AL_OUT_OF_MEMORY";
                break;
            default:
                errorName = Integer.toString(error);
                break;
        }

        logError(errorMessage + " OpenAL error " + errorName);
        return true;
    }
}
