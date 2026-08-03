package net.everla.everlaartifacts.common.enchantment;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentCategory;
import net.minecraft.world.item.enchantment.Enchantments;

/**
 * 踩踩背 — boots enchantment that greatly reduces fall damage.
 * Conflicts with Feather Falling.
 * <p>
 * Max level: 1. Weight: 2 (between common and uncommon).
 */
public class EnemyStepEnchantment extends Enchantment {

	public EnemyStepEnchantment() {
		super(Rarity.UNCOMMON, EnchantmentCategory.ARMOR_FEET,
				new EquipmentSlot[]{EquipmentSlot.FEET});
	}

	@Override
	public int getMinCost(int level) {
		return 15 + (level - 1) * 18;
	}

	@Override
	public int getMaxCost(int level) {
		return 33 + (level - 1) * 18;
	}

	@Override
	public int getMaxLevel() {
		return 1;
	}

	@Override
	public boolean isTreasureOnly() {
		return false;
	}

	@Override
	public boolean isTradeable() {
		return true;
	}

	@Override
	public boolean isDiscoverable() {
		return true;
	}

	@Override
	protected boolean checkCompatibility(Enchantment enchantment) {
		return super.checkCompatibility(enchantment)
			&& enchantment != Enchantments.FALL_PROTECTION;
	}
}
