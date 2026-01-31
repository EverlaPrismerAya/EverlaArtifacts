
package net.everla.everlaartifacts.enchantment;

import net.minecraft.world.item.enchantment.EnchantmentCategory;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.entity.EquipmentSlot;

public class DeutschEnchantment extends Enchantment {
	private static final EnchantmentCategory ENCHANTMENT_CATEGORY = EnchantmentCategory.ARMOR_HEAD;

	public DeutschEnchantment() {
		super(Enchantment.Rarity.RARE, ENCHANTMENT_CATEGORY, new EquipmentSlot[]{EquipmentSlot.HEAD});
	}

	@Override
	public int getMinCost(int level) {
		return 6 + level * 10;
	}

	@Override
	public int getMaxCost(int level) {
		return 12 + level * 10;
	}

	@Override
	public int getMaxLevel() {
		return 3;
	}

	@Override
	public boolean isTreasureOnly() {
		return true;
	}

	@Override
	public boolean isTradeable() {
		return false;
	}
}
