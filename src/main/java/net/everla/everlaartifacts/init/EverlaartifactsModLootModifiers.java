package net.everla.everlaartifacts.init;

import com.mojang.serialization.Codec;
import net.everla.everlaartifacts.EverlaartifactsMod;
import net.everla.everlaartifacts.common.loot.AddLootTableModifier;
import net.minecraftforge.common.loot.IGlobalLootModifier;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class EverlaartifactsModLootModifiers {
	public static final DeferredRegister<Codec<? extends IGlobalLootModifier>> REGISTRY =
		DeferredRegister.create(ForgeRegistries.Keys.GLOBAL_LOOT_MODIFIER_SERIALIZERS, EverlaartifactsMod.MODID);

	public static final RegistryObject<Codec<? extends IGlobalLootModifier>> ADD_LOOT =
		REGISTRY.register("add_loot", () -> AddLootTableModifier.CODEC);
}
