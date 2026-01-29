
/*
 *    MCreator note: This file will be REGENERATED on each build.
 */
package net.everla.everlaartifacts.init;

import net.minecraftforge.registries.RegistryObject;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.DeferredRegister;

import net.minecraft.world.entity.decoration.PaintingVariant;

import net.everla.everlaartifacts.EverlaartifactsMod;

public class EverlaartifactsModPaintings {
	public static final DeferredRegister<PaintingVariant> REGISTRY = DeferredRegister.create(ForgeRegistries.PAINTING_VARIANTS, EverlaartifactsMod.MODID);
	public static final RegistryObject<PaintingVariant> TECHNOBLADE_STARING = REGISTRY.register("technoblade_staring", () -> new PaintingVariant(32, 32));
}
