
/*
 *	MCreator note: This file will be REGENERATED on each build.
 */
package net.everla.everlaartifacts.init;

import net.minecraftforge.registries.RegistryObject;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.DeferredRegister;

import net.minecraft.world.item.enchantment.Enchantment;

import net.everla.everlaartifacts.enchantment.SteadfastEnchantment;
import net.everla.everlaartifacts.EverlaartifactsMod;

public class EverlaartifactsModEnchantments {
	public static final DeferredRegister<Enchantment> REGISTRY = DeferredRegister.create(ForgeRegistries.ENCHANTMENTS, EverlaartifactsMod.MODID);
	public static final RegistryObject<Enchantment> STEADFAST = REGISTRY.register("steadfast", () -> new SteadfastEnchantment());
}
