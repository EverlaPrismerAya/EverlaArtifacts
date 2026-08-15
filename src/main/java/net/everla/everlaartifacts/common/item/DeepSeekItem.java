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
 * 深度求索（Deep Seek）之戒。
 * <p>
 * 基于使用者当前的 CPU 利用率提升伤害：40% 为基准线 0%，50% 时最高 +25%，
 * 20% 时最低 -25%（线性插值，超范围钳制）。
 * <p>
 * Curios API 加载时作为戒指佩戴于饰品栏；未加载时放置于副手生效。
 * <p>
 * CPU 利用率由客户端通过 JDK 检测并周期性上报（见 {@code ClientPerformanceStatusPacket}）。
 * 利用率不可用时按基准线 40%（0% 加成）处理。
 */
public class DeepSeekItem extends Item {

	// 基准线 40% 利用率 = 0% 加成
	public static final int BASELINE_LOAD = 40;
	// 最高 50% → +25%
	public static final int MAX_LOAD = 50;
	// 最低 20% → -25%
	public static final int MIN_LOAD = 20;
	public static final double MAX_BONUS = 0.25;
	public static final double MIN_BONUS = -0.25;

	public DeepSeekItem() {
		super(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC));
	}

	/**
	 * 由 CPU 利用率（百分比）计算伤害倍率。
	 * <p>
	 * 40% 以上每升高 1% 增加 2.5 个百分点（40→50 线性至 +25%）；
	 * 40% 以下每降低 1% 减少 1.25 个百分点（40→20 线性至 -25%）。
	 * 利用率不可用（负值）时按基准线 40% 处理。
	 *
	 * @param cpuLoadPercent CPU 利用率（百分比，0~100）
	 * @return 伤害倍率（1.0 表示无加成）
	 */
	public static double calculateDamageMultiplier(int cpuLoadPercent) {
		int load = cpuLoadPercent < 0 ? BASELINE_LOAD : cpuLoadPercent;
		double bonus;
		if (load >= BASELINE_LOAD) {
			bonus = (load - BASELINE_LOAD) * MAX_BONUS / (MAX_LOAD - BASELINE_LOAD);
		} else {
			bonus = (load - BASELINE_LOAD) * MIN_BONUS / (MIN_LOAD - BASELINE_LOAD);
		}
		bonus = Math.max(MIN_BONUS, Math.min(MAX_BONUS, bonus));
		return 1.0 + bonus;
	}

	@Override
	public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
		super.appendHoverText(stack, level, tooltip, flag);
		// description_0 显示当前 CPU 利用率下的实际伤害提升
		int load = PerformanceMetrics.getLatestClientCpuLoad();
		double damageBonus = calculateDamageMultiplier(load) - 1.0;
		tooltip.add(Component.translatable("item.everlaartifacts.deepseek.description_0",
				String.format("%+.2f%%", damageBonus * 100.0), load));
		tooltip.add(Component.translatable("item.everlaartifacts.deepseek.description_1"));
	}
}
