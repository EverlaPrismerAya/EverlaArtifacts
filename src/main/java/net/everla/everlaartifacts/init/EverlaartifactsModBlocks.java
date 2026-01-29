
/*
 *    MCreator note: This file will be REGENERATED on each build.
 */
package net.everla.everlaartifacts.init;

import net.minecraftforge.registries.RegistryObject;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.DeferredRegister;

import net.minecraft.world.level.block.Block;

import net.everla.everlaartifacts.block.NuclearWasteWaterBlock;
import net.everla.everlaartifacts.block.DeepslateAuricOreBlock;
import net.everla.everlaartifacts.block.AuricScrapBlockBlock;
import net.everla.everlaartifacts.EverlaartifactsMod;

public class EverlaartifactsModBlocks {
	public static final DeferredRegister<Block> REGISTRY = DeferredRegister.create(ForgeRegistries.BLOCKS, EverlaartifactsMod.MODID);
	public static final RegistryObject<Block> DEEPSLATE_AURIC_ORE = REGISTRY.register("deepslate_auric_ore", () -> new DeepslateAuricOreBlock());
	public static final RegistryObject<Block> AURIC_SCRAP_BLOCK = REGISTRY.register("auric_scrap_block", () -> new AuricScrapBlockBlock());
	public static final RegistryObject<Block> NUCLEAR_WASTE_WATER = REGISTRY.register("nuclear_waste_water", () -> new NuclearWasteWaterBlock());
	// Start of user code block custom blocks
	// End of user code block custom blocks
}
