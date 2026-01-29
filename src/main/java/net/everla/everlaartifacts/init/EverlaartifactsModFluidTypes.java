
/*
 * MCreator note: This file will be REGENERATED on each build.
 */
package net.everla.everlaartifacts.init;

import net.minecraftforge.registries.RegistryObject;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.fluids.FluidType;

import net.everla.everlaartifacts.fluid.types.NuclearWasteWaterFluidType;
import net.everla.everlaartifacts.EverlaartifactsMod;

public class EverlaartifactsModFluidTypes {
	public static final DeferredRegister<FluidType> REGISTRY = DeferredRegister.create(ForgeRegistries.Keys.FLUID_TYPES, EverlaartifactsMod.MODID);
	public static final RegistryObject<FluidType> NUCLEAR_WASTE_WATER_TYPE = REGISTRY.register("nuclear_waste_water", () -> new NuclearWasteWaterFluidType());
}
