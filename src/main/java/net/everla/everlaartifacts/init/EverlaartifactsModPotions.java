
/*
 *    MCreator note: This file will be REGENERATED on each build.
 */
package net.everla.everlaartifacts.init;

import net.minecraftforge.registries.RegistryObject;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.DeferredRegister;

import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.effect.MobEffectInstance;

import net.everla.everlaartifacts.EverlaartifactsMod;

public class EverlaartifactsModPotions {
	public static final DeferredRegister<Potion> REGISTRY = DeferredRegister.create(ForgeRegistries.POTIONS, EverlaartifactsMod.MODID);
	public static final RegistryObject<Potion> NUCLEAR_WASTE_WATER_BOTTLE = REGISTRY.register("nuclear_waste_water_bottle", () -> new Potion(new MobEffectInstance(EverlaartifactsModMobEffects.NUCLEAR_WATER_RADIATION.get(), 4444, 0, true, true)));
	public static final RegistryObject<Potion> FLASHBOMB = REGISTRY.register("flashbomb", () -> new Potion(new MobEffectInstance(EverlaartifactsModMobEffects.GENSHIN_START.get(), 300, 0, false, true)));
}
