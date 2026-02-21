
/*
 *	MCreator note: This file will be REGENERATED on each build.
 */
package net.everla.everlaartifacts.init;

import net.minecraftforge.registries.RegistryObject;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.DeferredRegister;

import net.minecraft.world.item.enchantment.Enchantment;

import net.everla.everlaartifacts.common.enchantment.WildHuntEnchantment;
import net.everla.everlaartifacts.common.enchantment.TPAuraEnchantment;
import net.everla.everlaartifacts.common.enchantment.SteadfastEnchantment;
import net.everla.everlaartifacts.common.enchantment.ScrapyardScroungerEnchantment;
import net.everla.everlaartifacts.common.enchantment.MoneyBurnersCreedEnchantment;
import net.everla.everlaartifacts.common.enchantment.LiveWireEnchantment;
import net.everla.everlaartifacts.common.enchantment.LayeredBufferEnchantment;
import net.everla.everlaartifacts.common.enchantment.DeutschEnchantment;
import net.everla.everlaartifacts.EverlaartifactsMod;

public class EverlaartifactsModEnchantments {
	public static final DeferredRegister<Enchantment> REGISTRY = DeferredRegister.create(ForgeRegistries.ENCHANTMENTS, EverlaartifactsMod.MODID);
	public static final RegistryObject<Enchantment> STEADFAST = REGISTRY.register("steadfast", () -> new SteadfastEnchantment());
	public static final RegistryObject<Enchantment> TP_AURA = REGISTRY.register("tp_aura", () -> new TPAuraEnchantment());
	public static final RegistryObject<Enchantment> DEUTSCH = REGISTRY.register("deutsch", () -> new DeutschEnchantment());
	public static final RegistryObject<Enchantment> LAYERED_BUFFER = REGISTRY.register("layered_buffer", () -> new LayeredBufferEnchantment());
	public static final RegistryObject<Enchantment> SCRAPYARD_SCROUNGER = REGISTRY.register("scrapyard_scrounger", () -> new ScrapyardScroungerEnchantment());
	public static final RegistryObject<Enchantment> MONEY_BURNERS_CREED = REGISTRY.register("money_burners_creed", () -> new MoneyBurnersCreedEnchantment());
	public static final RegistryObject<Enchantment> LIVE_WIRE = REGISTRY.register("live_wire", () -> new LiveWireEnchantment());
	public static final RegistryObject<Enchantment> WILD_HUNT = REGISTRY.register("wild_hunt", () -> new WildHuntEnchantment());
}
