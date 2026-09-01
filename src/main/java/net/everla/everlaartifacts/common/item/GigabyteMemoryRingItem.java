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
 * 千兆内存之戒。
 * <p>
 * 根据佩戴者设备的内存与显存提供伤害与攻击速度加成：
 * <ul>
 *   <li>每 8GB 物理内存 → +2.5% 伤害</li>
 *   <li>每 1GB 显存 → +0.5% 伤害 与 +2% 攻击速度</li>
 * </ul>
 * Curios API 加载时作为戒指佩戴于饰品栏；未加载时放置于副手生效。
 * <p>
 * 加成公式集中在此类，供 {@code GigabyteMemoryRingHandler}（实际生效）
 * 与本类的 Tooltip（展示当前设备加成）共同使用。
 */
public class GigabyteMemoryRingItem extends Item {

	// 每 8GB 物理内存 → +2.5% 伤害
	public static final double DAMAGE_PER_8GB_RAM = 0.025;
	// 每 1GB 显存 → +0.5% 伤害
	public static final double DAMAGE_PER_GB_VRAM = 0.005;
	// 每 1GB 显存 → +2% 攻击速度
	public static final double SPEED_PER_GB_VRAM = 0.02;

	public GigabyteMemoryRingItem() {
		super(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC));
	}

	/**
	 * 由物理内存与显存计算伤害倍率。
	 *
	 * @param ramMB  物理内存容量（MB）
	 * @param vramMB 显存容量（MB）
	 * @return 伤害倍率（1.0 表示无加成）
	 */
	public static double calculateDamageMultiplier(int ramMB, int vramMB) {
		return 1.0 + (ramMB / 1000.0 / 8.0) * DAMAGE_PER_8GB_RAM
				+ (vramMB / 1000.0) * DAMAGE_PER_GB_VRAM;
	}

	/**
	 * 由显存计算攻击速度加成（作为 MULTIPLY_BASE 修饰符的值）。
	 *
	 * @param vramMB 显存容量（MB）
	 * @return 攻击速度加成（0.0 表示无加成）
	 */
	public static double calculateAttackSpeedBonus(int vramMB) {
		return (vramMB / 1000.0) * SPEED_PER_GB_VRAM;
	}

	@Override
	public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
		super.appendHoverText(stack, level, tooltip, flag);
		tooltip.add(Component.translatable("item.everlaartifacts.gigabyte_memory_ring.description_0"));

		// 在客户端显示当前设备硬件下的实际加成
		if (level != null && level.isClientSide()) {
			int ramMB = PerformanceMetrics.getCachedPhysicalMemoryMB();
			int vramMB = PerformanceMetrics.getCachedVramMB();
			double damageBonus = calculateDamageMultiplier(ramMB, vramMB) - 1.0;
			double speedBonus = calculateAttackSpeedBonus(vramMB);
			tooltip.add(Component.translatable("item.everlaartifacts.gigabyte_memory_ring.hardware",
					formatMemory(ramMB), formatMemory(vramMB)));
			tooltip.add(Component.translatable("item.everlaartifacts.gigabyte_memory_ring.bonus",
					formatPercent(damageBonus), formatPercent(speedBonus)));
		}
	}

	private static String formatMemory(int mb) {
		if (mb >= 1000) {
			return String.format("%.1fGB", mb / 1000.0);
		}
		return mb + "MB";
	}

	private static String formatPercent(double ratio) {
		return String.format("%+.2f%%", ratio * 100.0);
	}
}
