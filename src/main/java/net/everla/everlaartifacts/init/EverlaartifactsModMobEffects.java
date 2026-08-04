

package net.everla.everlaartifacts.init;

import net.minecraftforge.registries.RegistryObject;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.DeferredRegister;

import net.minecraft.world.effect.MobEffect;

import net.everla.everlaartifacts.common.effects.WaaoooOverlayMobEffect;
import net.everla.everlaartifacts.common.effects.VenusShellPassiveMobEffect;
import net.everla.everlaartifacts.common.effects.VenusShellActiveMobEffect;
import net.everla.everlaartifacts.common.effects.NuclearWaterRadiationMobEffect;
import net.everla.everlaartifacts.common.effects.HomaPassiveMobEffect;
import net.everla.everlaartifacts.common.effects.HomaActiveMobEffect;
import net.everla.everlaartifacts.common.effects.GenshinStartMobEffect;
import net.everla.everlaartifacts.common.effects.BloodBlossomMobEffect;
import net.everla.everlaartifacts.common.effects.BlitzkriegMobEffect;
import net.everla.everlaartifacts.common.effects.BedmicDestructionMobEffect;
import net.everla.everlaartifacts.common.effects.AmericanStyleCutOverlayMobEffect;
import net.everla.everlaartifacts.common.effects.CognitiveDisorderMobEffect;
import net.everla.everlaartifacts.common.effects.LethalPoisonMobEffect;
import net.everla.everlaartifacts.EverlaartifactsMod;

public class EverlaartifactsModMobEffects {
	public static final DeferredRegister<MobEffect> REGISTRY = DeferredRegister.create(ForgeRegistries.MOB_EFFECTS, EverlaartifactsMod.MODID);
	public static final RegistryObject<MobEffect> NUCLEAR_WATER_RADIATION = REGISTRY.register("nuclear_water_radiation", () -> new NuclearWaterRadiationMobEffect());
	public static final RegistryObject<MobEffect> BEDMIC_DESTRUCTION = REGISTRY.register("bedmic_destruction", () -> new BedmicDestructionMobEffect());
	public static final RegistryObject<MobEffect> GENSHIN_START = REGISTRY.register("genshin_start", () -> new GenshinStartMobEffect());
	public static final RegistryObject<MobEffect> AMERICAN_STYLE_CUT_OVERLAY = REGISTRY.register("american_style_cut_overlay", () -> new AmericanStyleCutOverlayMobEffect());
	public static final RegistryObject<MobEffect> WAAOOO_OVERLAY = REGISTRY.register("waaooo_overlay", () -> new WaaoooOverlayMobEffect());
	public static final RegistryObject<MobEffect> HOMA_PASSIVE = REGISTRY.register("homa_passive", () -> new HomaPassiveMobEffect());
	public static final RegistryObject<MobEffect> HOMA_ACTIVE = REGISTRY.register("homa_active", () -> new HomaActiveMobEffect());
	public static final RegistryObject<MobEffect> BLITZKRIEG = REGISTRY.register("blitzkrieg", () -> new BlitzkriegMobEffect());
	public static final RegistryObject<MobEffect> BLOOD_BLOSSOM = REGISTRY.register("blood_blossom", () -> new BloodBlossomMobEffect());
	public static final RegistryObject<MobEffect> VENUS_SHELL_PASSIVE = REGISTRY.register("venus_shell_passive", () -> new VenusShellPassiveMobEffect());
	public static final RegistryObject<MobEffect> VENUS_SHELL_ACTIVE = REGISTRY.register("venus_shell_active", () -> new VenusShellActiveMobEffect());
	public static final RegistryObject<MobEffect> COGNITIVE_DISORDER = REGISTRY.register("cognitive_disorder", () -> new CognitiveDisorderMobEffect());
	public static final RegistryObject<MobEffect> LETHAL_POISON = REGISTRY.register("lethal_poison", () -> new LethalPoisonMobEffect());
}
