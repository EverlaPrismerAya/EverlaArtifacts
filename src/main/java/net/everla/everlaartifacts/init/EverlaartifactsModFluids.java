
/*
 * MCreator note: This file will be REGENERATED on each build.
 */
package net.everla.everlaartifacts.init;

import net.minecraftforge.registries.RegistryObject;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.api.distmarker.Dist;

import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ItemBlockRenderTypes;

import net.everla.everlaartifacts.fluid.NuclearWasteWaterFluid;
import net.everla.everlaartifacts.EverlaartifactsMod;

public class EverlaartifactsModFluids {
	public static final DeferredRegister<Fluid> REGISTRY = DeferredRegister.create(ForgeRegistries.FLUIDS, EverlaartifactsMod.MODID);
	public static final RegistryObject<FlowingFluid> NUCLEAR_WASTE_WATER = REGISTRY.register("nuclear_waste_water", () -> new NuclearWasteWaterFluid.Source());
	public static final RegistryObject<FlowingFluid> FLOWING_NUCLEAR_WASTE_WATER = REGISTRY.register("flowing_nuclear_waste_water", () -> new NuclearWasteWaterFluid.Flowing());

	@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
	public static class FluidsClientSideHandler {
		@SubscribeEvent
		public static void clientSetup(FMLClientSetupEvent event) {
			ItemBlockRenderTypes.setRenderLayer(NUCLEAR_WASTE_WATER.get(), RenderType.translucent());
			ItemBlockRenderTypes.setRenderLayer(FLOWING_NUCLEAR_WASTE_WATER.get(), RenderType.translucent());
		}
	}
}
