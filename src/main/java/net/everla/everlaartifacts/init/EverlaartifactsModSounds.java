
package net.everla.everlaartifacts.init;

import net.minecraftforge.registries.RegistryObject;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.DeferredRegister;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.resources.ResourceLocation;

import net.everla.everlaartifacts.EverlaartifactsMod;

public class EverlaartifactsModSounds {
	public static final DeferredRegister<SoundEvent> REGISTRY = DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, EverlaartifactsMod.MODID);
	public static final RegistryObject<SoundEvent> WAAOOO = REGISTRY.register("waaooo", () -> SoundEvent.createVariableRangeEvent(new ResourceLocation("everlaartifacts", "waaooo")));
	public static final RegistryObject<SoundEvent> ANNOYINGDUMPLING = REGISTRY.register("annoyingdumpling", () -> SoundEvent.createVariableRangeEvent(new ResourceLocation("everlaartifacts", "annoyingdumpling")));
	public static final RegistryObject<SoundEvent> TWOBREADSANDWICHEDWITHCHEESE = REGISTRY.register("twobreadsandwichedwithcheese", () -> SoundEvent.createVariableRangeEvent(new ResourceLocation("everlaartifacts", "twobreadsandwichedwithcheese")));
	public static final RegistryObject<SoundEvent> EMOTIONAL_DAMAGE = REGISTRY.register("emotional_damage", () -> SoundEvent.createVariableRangeEvent(new ResourceLocation("everlaartifacts", "emotional_damage")));
	public static final RegistryObject<SoundEvent> EAGLE_SOUND = REGISTRY.register("eagle_sound", () -> SoundEvent.createVariableRangeEvent(new ResourceLocation("everlaartifacts", "eagle_sound")));
	public static final RegistryObject<SoundEvent> XELOC_BAD_APPLE = REGISTRY.register("xeloc_bad_apple", () -> SoundEvent.createVariableRangeEvent(new ResourceLocation("everlaartifacts", "xeloc_bad_apple")));
	public static final RegistryObject<SoundEvent> FIVE_WATT = REGISTRY.register("five_watt", () -> SoundEvent.createVariableRangeEvent(new ResourceLocation("everlaartifacts", "five_watt")));
	public static final RegistryObject<SoundEvent> PLACEHOLDER_SND = REGISTRY.register("placeholder_snd", () -> SoundEvent.createVariableRangeEvent(new ResourceLocation("everlaartifacts", "placeholder_snd")));
	public static final RegistryObject<SoundEvent> GENSHIN_START_SOUND = REGISTRY.register("genshin_start_sound", () -> SoundEvent.createVariableRangeEvent(new ResourceLocation("everlaartifacts", "genshin_start_sound")));
	public static final RegistryObject<SoundEvent> DELTARUNE_EXPLOSION = REGISTRY.register("deltarune_explosion", () -> SoundEvent.createVariableRangeEvent(new ResourceLocation("everlaartifacts", "deltarune_explosion")));
	public static final RegistryObject<SoundEvent> AURIC_STRIKE = REGISTRY.register("auric_strike", () -> SoundEvent.createVariableRangeEvent(new ResourceLocation("everlaartifacts", "auric_strike")));
	public static final RegistryObject<SoundEvent> LIVE_WIRE = REGISTRY.register("live_wire", () -> SoundEvent.createVariableRangeEvent(new ResourceLocation("everlaartifacts", "live_wire")));
	public static final RegistryObject<SoundEvent> LIVE_WIRE_HOMA_STAFF = REGISTRY.register("live_wire_homa_staff", () -> SoundEvent.createVariableRangeEvent(new ResourceLocation("everlaartifacts", "live_wire_homa_staff")));
	public static final RegistryObject<SoundEvent> DIFFICULT_SWITCH = REGISTRY.register("difficult_switch", () -> SoundEvent.createVariableRangeEvent(new ResourceLocation("everlaartifacts", "difficult_switch")));
	public static final RegistryObject<SoundEvent> ENEMY_STEP_B = REGISTRY.register("enemy_step_b", () -> SoundEvent.createVariableRangeEvent(new ResourceLocation("everlaartifacts", "enemy_step_b")));
	public static final RegistryObject<SoundEvent> ENEMY_STEP_C = REGISTRY.register("enemy_step_c", () -> SoundEvent.createVariableRangeEvent(new ResourceLocation("everlaartifacts", "enemy_step_c")));
}
