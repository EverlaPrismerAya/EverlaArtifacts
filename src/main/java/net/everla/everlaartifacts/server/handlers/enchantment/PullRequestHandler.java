package net.everla.everlaartifacts.server.handlers.enchantment;

import net.everla.everlaartifacts.init.EverlaartifactsModEnchantments;
import net.everla.everlaartifacts.mixin.AbstractArrowAccessor;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.projectile.ThrownTrident;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.ProjectileImpactEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Comparator;
import java.util.List;

/**
 * 拉取请求（Pull Request）附魔的服务端逻辑。
 * <p>
 * 移植自 1.21 数据包 tcc:pull_request 的 hit_block + post_attack 触发器：
 * 三叉戟命中方块或生物时，把附近掉落物吸附并驮到三叉戟上一起带走（原数据包
 * 的 trigger.mcfunction + ride.mcfunction）。
 */
@Mod.EventBusSubscriber(modid = "everlaartifacts")
public class PullRequestHandler {

	/** 吸附半径（方块），对应原数据包 distance=..3 */
	private static final double PULL_RADIUS = 3.0;
	/** 单次最多吸附的掉落物数量，对应原数据包 limit=5 */
	private static final int MAX_ITEMS = 5;

	@SubscribeEvent
	public static void onProjectileImpact(ProjectileImpactEvent event) {
		if (!(event.getProjectile() instanceof ThrownTrident trident)) {
			return;
		}

		Level level = trident.level();
		if (level.isClientSide()) {
			return;
		}

		// 通过已有的 accessor mixin 拿到三叉戟实体上的物品，读取附魔
		ItemStack tridentStack = ((AbstractArrowAccessor) trident).everlaartifacts$invokeGetPickupItem();
		if (EnchantmentHelper.getItemEnchantmentLevel(
				EverlaartifactsModEnchantments.PULL_REQUEST.get(), tridentStack) <= 0) {
			return;
		}

		pullItems(trident, (ServerLevel) level);
	}

	private static void pullItems(ThrownTrident trident, ServerLevel level) {
		Vec3 center = trident.position();
		AABB box = trident.getBoundingBox().inflate(PULL_RADIUS);
		List<ItemEntity> items = level.getEntitiesOfClass(ItemEntity.class, box,
			e -> e.isAlive() && !e.isPassenger()
				&& e.position().distanceToSqr(center) <= PULL_RADIUS * PULL_RADIUS);

		items.sort(Comparator.comparingDouble(e -> e.position().distanceToSqr(center)));

		int count = Math.min(items.size(), MAX_ITEMS);
		for (int i = 0; i < count; i++) {
			ItemEntity item = items.get(i);
			item.setInvulnerable(true);
			// force=true：跳过 canRide 检查，让掉落物可以直接骑上三叉戟
			item.startRiding(trident, true);
		}
	}
}
