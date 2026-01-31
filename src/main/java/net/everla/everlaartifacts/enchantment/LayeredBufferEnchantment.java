package net.everla.everlaartifacts.enchantment;

import net.minecraft.world.item.enchantment.EnchantmentCategory;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;

public class LayeredBufferEnchantment extends Enchantment {
	private static final String LAYERED_BUFFER_DAMAGE_KEY = "LayeredBufferDamage";
	private static final EnchantmentCategory ENCHANTMENT_CATEGORY = EnchantmentCategory.ARMOR_CHEST;

	public LayeredBufferEnchantment() {
		super(Enchantment.Rarity.RARE, ENCHANTMENT_CATEGORY, EquipmentSlot.values());
	}

	@Override
	public int getMinCost(int level) {
		return 3 + level * 4;
	}

	@Override
	public int getMaxCost(int level) {
		return 8 + level * 4;
	}

	@Override
	public int getMaxLevel() {
		return 10;
	}

	@Override
	public boolean isTreasureOnly() {
		return true;
	}
	
	@Override
	public boolean isCurse() {
		return false;
	}
}