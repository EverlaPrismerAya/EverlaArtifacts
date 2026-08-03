package net.everla.everlaartifacts.common.enchantment;

import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.enchantment.EnchantmentCategory;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.tags.ItemTags;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

import net.everla.everlaartifacts.init.EverlaartifactsModEnchantments;

public class WindBreathEnchantment extends Enchantment {
	private static final EnchantmentCategory ENCHANTMENT_CATEGORY = EnchantmentCategory.create("everlaartifacts_wind_breath", item -> Ingredient.of(ItemTags.create(new ResourceLocation("minecraft:swords"))).test(new ItemStack(item)));

	public WindBreathEnchantment() {
		super(Enchantment.Rarity.VERY_RARE, ENCHANTMENT_CATEGORY, new EquipmentSlot[]{EquipmentSlot.MAINHAND});
	}

	@Override
	public int getMinCost(int level) {
		return 15 + (level - 1) * 9;
	}

	@Override
	public int getMaxCost(int level) {
		return 65 + (level - 1) * 9;
	}

	@Override
	public int getMaxLevel() {
		return 3;
	}

	public boolean isTreasureOnly() {
		return true;
	}

	@Override
	public boolean isTradeable() {
		return false;
	}

	@Override
	public boolean isDiscoverable() {
		return true;
	}

	@Override
	protected boolean checkCompatibility(Enchantment enchantment) {
		return super.checkCompatibility(enchantment)
			&& enchantment != Enchantments.FIRE_ASPECT
			&& enchantment != EverlaartifactsModEnchantments.LIVE_WIRE.get();
	}
}