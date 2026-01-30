package net.everla.everlaartifacts.enchantment;

import net.minecraft.world.item.enchantment.EnchantmentCategory;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.tags.ItemTags;
import net.minecraft.resources.ResourceLocation;

public class TPAuraEnchantment extends Enchantment {
	private static final EnchantmentCategory ENCHANTMENT_CATEGORY = EnchantmentCategory.create("everlaartifacts_tp_aura", item -> Ingredient.of(ItemTags.create(new ResourceLocation("minecraft:swords"))).test(new ItemStack(item)));

	public TPAuraEnchantment() {
		super(Enchantment.Rarity.RARE, ENCHANTMENT_CATEGORY, new EquipmentSlot[]{EquipmentSlot.MAINHAND});
	}

	@Override
	public int getMinCost(int level) {
		return 1 + level * 10;
	}

	@Override
	public int getMaxCost(int level) {
		return 6 + level * 10;
	}

	@Override
	public boolean isTreasureOnly() {
		return true;
	}

	@Override
	public boolean isDiscoverable() {
		return false;
	}
	
	@Override
	public int getMaxLevel() {
		return 1; // 设置最大等级为1
	}
	
	@Override
	public boolean canApplyAtEnchantingTable(ItemStack stack) {
		return this.category.canEnchant(stack.getItem());
	}
}