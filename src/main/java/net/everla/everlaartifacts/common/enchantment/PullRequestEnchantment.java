package net.everla.everlaartifacts.common.enchantment;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentCategory;

/**
 * 拉取请求 — trident-only enchantment.
 * 三叉戟命中方块或生物时，把半径 3 格内的掉落物吸附并驮在三叉戟上带走。
 * <p>
 * 移植自 1.21 数据包附魔 tcc:pull_request（minecraft:hit_block + minecraft:post_attack 触发器）。
 * Max level: 1. Weight: 5 (uncommon).
 */
public class PullRequestEnchantment extends Enchantment {

	public PullRequestEnchantment() {
		super(Rarity.UNCOMMON, EnchantmentCategory.TRIDENT,
			new EquipmentSlot[]{EquipmentSlot.MAINHAND});
	}

	@Override
	public int getMinCost(int level) {
		return 12 + (level - 1) * 7;
	}

	@Override
	public int getMaxCost(int level) {
		return 24;
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
}
