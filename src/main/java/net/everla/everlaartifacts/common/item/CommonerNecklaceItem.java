package net.everla.everlaartifacts.common.item;

import net.everla.everlaartifacts.server.PerformanceMetrics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * 平民项链。
 * <p>
 * 根据佩戴者显卡的显存容量决定攻击力：
 * <ul>
 *   <li>≤4G → +10%（最高）</li>
 *   <li>6G → +7%</li>
 *   <li>8G → +5%</li>
 *   <li>10G → 无加成</li>
 *   <li>≥16G → -10%（最低）</li>
 * </ul>
 * 锚点之间按分段线性插值，超范围钳制。
 * <p>
 * 低于 8G 的显卡：作为垃圾佬，你不在意他人的眼光，受到的伤害降低 10%；
 * 高于 10G 的显卡：你无法理解平民，受到的伤害增加 10%。
 * <p>
 * Curios API 加载时作为项链佩戴于饰品栏；未加载时放置于副手生效。
 * <p>
 * 攻击力通过对 {@code generic.attack_damage} 添加 MULTIPLY_BASE 属性修饰符实现
 * （见 {@code CommonerNecklaceHandler}），本类同时提供 Tooltip 展示当前显卡与加成。
 */
public class CommonerNecklaceItem extends Item {

	// 显存锚点（GB，按 1024 MiB = 1G 换算）
	public static final double VRAM_4G = 4.0;
	public static final double VRAM_6G = 6.0;
	public static final double VRAM_8G = 8.0;
	public static final double VRAM_10G = 10.0;
	public static final double VRAM_16G = 16.0;

	// 对应锚点的攻击力加成（正数为增伤，负数为减伤）
	public static final double BONUS_4G = 0.10;
	public static final double BONUS_6G = 0.07;
	public static final double BONUS_8G = 0.05;
	public static final double BONUS_10G = 0.00;
	public static final double BONUS_16G = -0.10;

	public CommonerNecklaceItem() {
		super(new Item.Properties().stacksTo(1).rarity(Rarity.COMMON));
	}

	/**
	 * 由显存容量（GB）计算攻击力倍率。
	 * <p>
	 * 锚点间分段线性插值：4G→+10%，6G→+7%，8G→+5%，10G→0%，16G→-10%；
	 * 低于 4G 钳制为 +10%，高于 16G 钳制为 -10%。
	 *
	 * @param vramGB 显存容量（GB）
	 * @return 攻击力倍率（1.0 表示无加成）
	 */
	public static double calculateDamageMultiplier(double vramGB) {
		double bonus;
		if (vramGB <= VRAM_4G) {
			bonus = BONUS_4G;
		} else if (vramGB <= VRAM_6G) {
			bonus = lerp(vramGB, VRAM_4G, VRAM_6G, BONUS_4G, BONUS_6G);
		} else if (vramGB <= VRAM_8G) {
			bonus = lerp(vramGB, VRAM_6G, VRAM_8G, BONUS_6G, BONUS_8G);
		} else if (vramGB <= VRAM_10G) {
			bonus = lerp(vramGB, VRAM_8G, VRAM_10G, BONUS_8G, BONUS_10G);
		} else if (vramGB <= VRAM_16G) {
			bonus = lerp(vramGB, VRAM_10G, VRAM_16G, BONUS_10G, BONUS_16G);
		} else {
			bonus = BONUS_16G;
		}
		return 1.0 + bonus;
	}

	/**
	 * 由显存容量（GB）计算受到的伤害倍率。
	 * <p>
	 * 低于 8G（垃圾佬）：受到伤害 ×0.9；高于 10G（无法理解平民）：受到伤害 ×1.1；
	 * 8G~10G 之间无特殊效果。
	 *
	 * @param vramGB 显存容量（GB）
	 * @return 受到的伤害倍率（1.0 表示无特殊效果）
	 */
	public static double calculateDefenseMultiplier(double vramGB) {
		if (vramGB < VRAM_8G) {
			return 0.90;
		}
		if (vramGB > VRAM_10G) {
			return 1.10;
		}
		return 1.0;
	}

	@Override
	public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
		super.appendHoverText(stack, level, tooltip, flag);
		tooltip.add(Component.translatable("item.everlaartifacts.commoner_necklace.description_0"));

		// 在客户端显示当前显卡与攻击力修正、特殊效果
		if (level != null && level.isClientSide()) {
			int vramMB = PerformanceMetrics.getCachedVramMB();
			String gpuName = PerformanceMetrics.getClientGpuName();
			String bonusText;
			String specialKey;
			if (vramMB <= 0) {
				// 未检测到显存时按无加成、无特殊效果处理
				bonusText = "+0.00%";
				specialKey = "item.everlaartifacts.commoner_necklace.description_2_normal";
			} else {
				double vramGB = vramMB / 1024.0;
				double bonus = calculateDamageMultiplier(vramGB) - 1.0;
				bonusText = formatPercent(bonus);
				double defense = calculateDefenseMultiplier(vramGB);
				if (defense < 1.0) {
					specialKey = "item.everlaartifacts.commoner_necklace.description_2_junk";
				} else if (defense > 1.0) {
					specialKey = "item.everlaartifacts.commoner_necklace.description_2_rich";
				} else {
					specialKey = "item.everlaartifacts.commoner_necklace.description_2_normal";
				}
			}
			tooltip.add(Component.translatable("item.everlaartifacts.commoner_necklace.description_1",
					gpuName, bonusText));
			tooltip.add(Component.translatable(specialKey));
		}
	}

	private static double lerp(double vramGB, double a, double b, double bonusA, double bonusB) {
		double ratio = (vramGB - a) / (b - a);
		return bonusA + (bonusB - bonusA) * ratio;
	}

	private static String formatPercent(double ratio) {
		return String.format("%+.2f%%", ratio * 100.0);
	}
}
