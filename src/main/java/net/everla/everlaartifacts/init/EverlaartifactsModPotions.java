
/*
 *    MCreator note: This file will be REGENERATED on each build.
 */
package net.everla.everlaartifacts.init;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraftforge.common.brewing.BrewingRecipeRegistry;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import net.everla.everlaartifacts.EverlaartifactsMod;
import net.everla.everlaartifacts.common.recipes.ModBrewingRecipe;

public class EverlaartifactsModPotions {
	public static final DeferredRegister<Potion> POTIONS = DeferredRegister.create(ForgeRegistries.POTIONS, EverlaartifactsMod.MODID);
	public static final RegistryObject<Potion> NUCLEAR_WASTE_WATER_BOTTLE = POTIONS.register("nuclear_waste_water_bottle", () -> new Potion(new MobEffectInstance(EverlaartifactsModMobEffects.NUCLEAR_WATER_RADIATION.get(), 4444, 0, true, true)));
	public static final RegistryObject<Potion> FLASHBOMB = POTIONS.register("flashbomb", () -> new Potion(new MobEffectInstance(EverlaartifactsModMobEffects.GENSHIN_START.get(), 300, 0, false, true)));
	public static void init() {
		potionBrewing(PotionUtils.setPotion(new ItemStack(Items.POTION), Potions.NIGHT_VISION),EverlaartifactsModPotions.FLASHBOMB.get(),Items.GLOWSTONE);
		potionBrewing(PotionUtils.setPotion(new ItemStack(Items.GLASS_BOTTLE), Potions.EMPTY),EverlaartifactsModPotions.NUCLEAR_WASTE_WATER_BOTTLE.get(),EverlaartifactsModItems.NUCLEAR_WASTE_WATER_BUCKET.get());
	}
	private static void potionBrewing(ItemStack inputPot, Potion pot, Item item) {
		BrewingRecipeRegistry.addRecipe(new ModBrewingRecipe(inputPot, Ingredient.of(item), PotionUtils.setPotion(new ItemStack(Items.POTION), pot)));
	}
	public static void registers(IEventBus eventBus) {
		POTIONS.register(eventBus);
	}
}
