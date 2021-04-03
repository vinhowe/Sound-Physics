package vin.howe.soundphysics;

import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.Config;
import me.shedaniel.autoconfig.annotation.ConfigEntry;

@Config(name = SoundPhysics.MOD_ID)
public class SoundPhysicsConfig implements ConfigData {
    public static SoundPhysicsConfig getConfig() {
        return AutoConfig.getConfigHolder(SoundPhysicsConfig.class).getConfig();
    }

    public static final SoundPhysicsConfig instance;

    public static class General {
        public static float rolloffFactor = 1.0f;
        public static float globalReverbGain = 1.0f;
        public static float globalReverbBrightness = 1.0f;
        public static float soundDistanceAllowance = 4.0f;
        public static float globalBlockAbsorption = 1.0f;
        public static float globalBlockReflectance = 1.0f;
        public static float airAbsorption = 1.0f;
        public static float underwaterFilter = 0.8f;
    }

    @ConfigEntry.Category("general")
    @ConfigEntry.Gui.TransitiveObject
    public General general = new General();

    public static class Performance {
        public static boolean skipRainOcclusionTracing = true;
        public static int environmentEvaluationRays = 32;
        public static boolean simplerSharedAirspaceSimulation = false;
    }

    @ConfigEntry.Category("performance")
    @ConfigEntry.Gui.TransitiveObject
    public Performance performance = new Performance();

    public static class MaterialProperties {
        public static float stoneReflectivity = 0.95f;
        public static float woodReflectivity = 0.7f;
        public static float groundReflectivity = 0.3f;
        public static float plantReflectivity = 0.2f;
        public static float metalReflectivity = 0.97f;
        public static float glassReflectivity = 0.5f;
        public static float clothReflectivity = 0.25f;
        public static float sandReflectivity = 0.2f;
        public static float snowReflectivity = 0.2f;
    }

    @ConfigEntry.Category("block")
    @ConfigEntry.Gui.TransitiveObject
    public MaterialProperties materialProperties = new MaterialProperties();

    private static final String categoryGeneral = "General";
    private static final String categoryPerformance = "Performance";
    private static final String categoryMaterialProperties = "Material properties";

    static {
        instance = new SoundPhysicsConfig();
    }

    private SoundPhysicsConfig() {
    }

//    private void syncConfig() {
//        // General
//        rolloffFactor = this.forgeConfig.getFloat("Attenuation Factor", categoryGeneral, 1.0f, 0.2f, 1.0f,
//                "Affects how quiet a sound gets based on distance. Lower values mean distant sounds are louder. 1.0 is the physically correct value.");
//        globalReverbGain = this.forgeConfig.getFloat("Global Reverb Gain", categoryGeneral, 1.0f, 0.1f, 2.0f,
//                "The global volume of simulated reverberations.");
//        globalReverbBrightness = this.forgeConfig.getFloat("Global Reverb Brightness", categoryGeneral, 1.0f, 0.1f,
//                2.0f,
//                "The brightness of reverberation. Higher values result in more high frequencies in reverberation. Lower values give a more muffled sound to the reverb.");
//        globalBlockAbsorption = this.forgeConfig.getFloat("Global Block Absorption", categoryGeneral, 1.0f, 0.1f, 4.0f,
//                "The global amount of sound that will be absorbed when traveling through blocks.");
//        globalBlockReflectance = this.forgeConfig.getFloat("Global Block Reflectance", categoryGeneral, 1.0f, 0.1f,
//                4.0f,
//                "The global amount of sound reflectance energy of all blocks. Lower values result in more conservative reverb simulation with shorter reverb tails. Higher values result in more generous reverb simulation with higher reverb tails.");
//        soundDistanceAllowance = this.forgeConfig.getFloat("Sound Distance Allowance", categoryGeneral, 4.0f, 1.0f,
//                6.0f,
//                "Minecraft won't allow sounds to play past a certain distance. This parameter is a multiplier for how far away a sound source is allowed to be in order for it to actually play. Values too high can cause polyphony issues.");
//        airAbsorption = this.forgeConfig.getFloat("Air Absorption", categoryGeneral, 1.0f, 0.0f, 5.0f,
//                "A value controlling the amount that air absorbs high frequencies with distance. A value of 1.0 is physically correct for air with normal humidity and temperature. Higher values mean air will absorb more high frequencies with distance. 0 disables this effect.");
//        underwaterFilter = this.forgeConfig.getFloat("Underwater Filter", categoryGeneral, 0.8f, 0.0f, 1.0f,
//                "How much sound is filtered when the player is underwater. 0.0 means no filter. 1.0 means fully filtered.");
//
//        // performance
//        skipRainOcclusionTracing = this.forgeConfig.getBoolean("Skip Rain Occlusion Tracing", categoryPerformance, true,
//                "If true, rain sound sources won't trace for sound occlusion. This can help performance during rain.");
//        environmentEvaluationRays = this.forgeConfig.getInt("Environment Evaluation Rays", categoryPerformance, 32, 8,
//                64,
//                "The number of rays to trace to determine reverberation for each sound source. More rays provides more consistent tracing results but takes more time to calculate. Decrease this value if you experience lag spikes when sounds play.");
//        simplerSharedAirspaceSimulation = this.forgeConfig.getBoolean("Simpler Shared Airspace Simulation",
//                categoryPerformance, false,
//                "If true, enables a simpler technique for determining when the player and a sound source share airspace. Might sometimes miss recognizing shared airspace, but it's faster to calculate.");
//
//        // material properties
//        stoneReflectivity = this.forgeConfig.getFloat("Stone Reflectivity", categoryMaterialProperties, 0.95f, 0.0f,
//                1.0f, "Sound reflectivity for stone blocks.");
//        woodReflectivity = this.forgeConfig.getFloat("Wood Reflectivity", categoryMaterialProperties, 0.7f, 0.0f, 1.0f,
//                "Sound reflectivity for wooden blocks.");
//        groundReflectivity = this.forgeConfig.getFloat("Ground Reflectivity", categoryMaterialProperties, 0.3f, 0.0f,
//                1.0f, "Sound reflectivity for ground blocks (dirt, gravel, etc).");
//        plantReflectivity = this.forgeConfig.getFloat("Foliage Reflectivity", categoryMaterialProperties, 0.2f, 0.0f,
//                1.0f, "Sound reflectivity for foliage blocks (leaves, grass, etc.).");
//        metalReflectivity = this.forgeConfig.getFloat("Metal Reflectivity", categoryMaterialProperties, 0.97f, 0.0f,
//                1.0f, "Sound reflectivity for metal blocks.");
//        glassReflectivity = this.forgeConfig.getFloat("Glass Reflectivity", categoryMaterialProperties, 0.5f, 0.0f,
//                1.0f, "Sound reflectivity for glass blocks.");
//        clothReflectivity = this.forgeConfig.getFloat("Cloth Reflectivity", categoryMaterialProperties, 0.25f, 0.0f,
//                1.0f, "Sound reflectivity for cloth blocks (carpet, wool, etc).");
//        sandReflectivity = this.forgeConfig.getFloat("Sand Reflectivity", categoryMaterialProperties, 0.2f, 0.0f, 1.0f,
//                "Sound reflectivity for sand blocks.");
//        snowReflectivity = this.forgeConfig.getFloat("Snow Reflectivity", categoryMaterialProperties, 0.2f, 0.0f, 1.0f,
//                "Sound reflectivity for snow blocks.");
//
//        if (this.forgeConfig.hasChanged()) {
//            this.forgeConfig.save();
//            SoundPhysics.applyConfigChanges();
//        }
//    }

}
