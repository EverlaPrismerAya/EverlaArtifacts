package net.everla.everlaartifacts.common.enchantment;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentCategory;

/**
 * Escalation of Freedom（自由升级）— 铠甲位诅咒附魔（宝藏附魔，仅可从战利品获取）。
 * <p>
 * 一场「免费」的升级，实则全是代价：
 * <ul>
 *   <li>造成的最终伤害降低 50%</li>
 *   <li>受到的最终伤害增加 100%</li>
 *   <li>最终护甲值降低 30%</li>
 *   <li>受到的火焰类伤害提升 100%</li>
 *   <li>受到的击退随机为 100%~500%</li>
 *   <li>摔落伤害增加 400%</li>
 *   <li>溺水伤害增加 2000%</li>
 * </ul>
 * 另带隐藏诅咒：与「绑定诅咒」相同，生存模式无法从铠甲槽取下（仅创造可取下），
 * 见 {@code mixin/EnchantmentHelperBindingMixin}。
 * <p>
 * 实际逻辑见 {@code server/handlers/enchantment/EscalationOfFreedomHandler}。
 */
public class EscalationOfFreedomEnchantment extends Enchantment {
	private static final EnchantmentCategory ENCHANTMENT_CATEGORY = EnchantmentCategory.ARMOR;

	public EscalationOfFreedomEnchantment() {
		super(Enchantment.Rarity.VERY_RARE, ENCHANTMENT_CATEGORY,
			new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET});
	}

	@Override
	public int getMinCost(int level) {
		return 30;
	}

	@Override
	public int getMaxCost(int level) {
		return 50;
	}

	@Override
	public int getMaxLevel() {
		return 1;
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
		return false;
	}

	@Override
	public boolean isCurse() {
		return true;
	}
}
