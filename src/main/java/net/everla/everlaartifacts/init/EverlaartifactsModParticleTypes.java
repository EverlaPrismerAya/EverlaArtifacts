
/*
 *    MCreator note: This file will be REGENERATED on each build.
 */
package net.everla.everlaartifacts.init;

import net.minecraftforge.registries.RegistryObject;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.DeferredRegister;

import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.particles.ParticleType;

import net.everla.everlaartifacts.EverlaartifactsMod;

public class EverlaartifactsModParticleTypes {
	public static final DeferredRegister<ParticleType<?>> REGISTRY = DeferredRegister.create(ForgeRegistries.PARTICLE_TYPES, EverlaartifactsMod.MODID);
	public static final RegistryObject<SimpleParticleType> GOLD_BUTTERFLY = REGISTRY.register("gold_butterfly", () -> new SimpleParticleType(false));
	public static final RegistryObject<SimpleParticleType> FIRE_BUTTERFLY = REGISTRY.register("fire_butterfly", () -> new SimpleParticleType(false));
}
