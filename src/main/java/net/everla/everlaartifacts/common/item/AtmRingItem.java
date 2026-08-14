package net.everla.everlaartifacts.common.item;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraftforge.fml.ModList;

import java.util.List;
import java.util.Set;

/**
 * Allthemodium 之戒（ATM 之戒）。
 * <p>
 * 每安装一个模组提升 0.03% 最终伤害。Allthemodium 意为 "All The Mods"（全部模组）。
 * <p>
 * Curios API 加载时作为戒指佩戴于饰品栏；未加载时放置于副手生效。
 * <p>
 * 客户端在进入游戏时上报本机安装的模组数（见 {@code ClientModCountPacket}），
 * 服务端据此计算加成。加成的模组数统计逻辑集中在 {@link #getInstalledModCount()}。
 */
public class AtmRingItem extends Item {

	// 每安装一个模组 → +0.03% 最终伤害
	public static final double DAMAGE_PER_MOD = 0.0003;

	// 基础模组（非玩家安装的内容模组），不计入模组数
	private static final Set<String> BASE_MODS = Set.of("minecraft", "forge", "javafml", "mcp");

	public AtmRingItem() {
		super(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC));
	}

	/**
	 * 统计当前安装的内容模组数量（排除 Forge 生态基础模组）。
	 *
	 * @return 已安装模组数，获取失败时返回0
	 */
	public static int getInstalledModCount() {
		try {
			ModList modList = ModList.get();
			if (modList == null) {
				return 0;
			}
			return (int) modList.getMods().stream()
					.map(mi -> mi.getModId())
					.filter(id -> !BASE_MODS.contains(id))
					.distinct()
					.count();
		} catch (Exception e) {
			return 0;
		}
	}

	/**
	 * 由安装的模组数计算最终伤害倍率。
	 *
	 * @param modCount 已安装模组数
	 * @return 伤害倍率（1.0 表示无加成）
	 */
	public static double calculateDamageMultiplier(int modCount) {
		return 1.0 + modCount * DAMAGE_PER_MOD;
	}

	@Override
	public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
		super.appendHoverText(stack, level, tooltip, flag);
		// description_0 显示当前最终伤害提升（按本机安装的模组数实时计算）
		int modCount = getInstalledModCount();
		double damageBonus = calculateDamageMultiplier(modCount) - 1.0;
		tooltip.add(Component.translatable("item.everlaartifacts.atm_ring.description_0",
				String.format("%+.2f%%", damageBonus * 100.0), modCount));
	}
}
