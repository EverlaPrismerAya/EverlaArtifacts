package net.everla.everlaartifacts.common.enchantment;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentCategory;

/**
 * 音爆（Sonic Boom）— 弓专用宝藏附魔，可在古城箱子中发现。
 * <p>
 * 移植自 1.21 数据包附魔 tcc:sonic_blast（minecraft:projectile_spawned 触发器），
 * 但视觉上改用音爆粒子，不创建弹射物实体。
 * <ul>
 *   <li>最大等级 5；1 级伤害 4 点，每级额外 +1.5 点（默认值，均可配置）</li>
 *   <li>默认可穿透方块与无生命实体，不可穿透生物（命中第一个生物即停）</li>
 *   <li>若配置为可穿透生物，则对路径上每个生物都造成伤害（每个生物只受伤一次）</li>
 *   <li>音爆永远不对无生命实体造成伤害（即使穿过它们也不会造成影响）</li>
 *   <li>最大射程 20 格（可配置）</li>
 * </ul>
 * <p>
 * 实际逻辑见 {@code server/handlers/enchantment/SonicBoomHandler}。
 */
public class SonicBoomEnchantment extends Enchantment {

	public SonicBoomEnchantment() {
		super(Rarity.VERY_RARE, EnchantmentCategory.BOW,
			new EquipmentSlot[]{EquipmentSlot.MAINHAND});
	}

	@Override
	public int getMinCost(int level) {
		return 20 + (level - 1) * 9;
	}

	@Override
	public int getMaxCost(int level) {
		return 50 + (level - 1) * 9;
	}

	@Override
	public int getMaxLevel() {
		return 5;
	}

	@Override
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
}
