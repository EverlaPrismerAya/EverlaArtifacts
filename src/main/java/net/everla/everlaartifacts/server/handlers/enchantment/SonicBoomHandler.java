package net.everla.everlaartifacts.server.handlers.enchantment;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import net.everla.everlaartifacts.EverlaartifactsMod;
import net.everla.everlaartifacts.common.config.EverlaArtifactsConfig;
import net.everla.everlaartifacts.init.EverlaartifactsModEnchantments;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.ThrownTrident;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.player.ArrowLooseEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * 音爆（Sonic Boom）附魔的服务端逻辑。
 * <p>
 * 移植自 1.21 数据包附魔 tcc:sonic_blast 的
 * sonic_blast_trigger.mcfunction + actions/sonic_blast 系列函数。
 * 与守卫者音爆一致，音爆在发射瞬间即命中整条路径（hitscan），
 * 且<b>不创建弹射物实体</b>：视觉完全使用 {@link ParticleTypes#SONIC_BOOM}
 * 粒子沿路径一次性铺开。
 * <p>
 * 从拉弓者眼中沿视线方向逐步扫描，每次前进 {@value #STEP} 格：
 * <ul>
 *   <li>遇到完整方块且配置不允许穿透方块时停止</li>
 *   <li>播放音爆粒子特效（每两格一个，对应原数据包 distance=..1 的判定半径）</li>
 *   <li>对周围 {@value #BEAM_RADIUS} 格内的实体判定：
 *       <ul>
 *         <li>生物：造成伤害；若配置不可穿透生物则命中第一个即停，
 *             若可穿透则对路径上每个生物都造成伤害（每个生物只受伤一次）</li>
 *         <li>无生命实体：永不造成伤害，仅当配置不允许穿透时作为障碍挡停光束</li>
 *       </ul>
 *   </li>
 * </ul>
 * <p>
 * 触发方式：优先用 {@code ArrowLooseEvent} 取消箭并发射；若该事件被其它模组绕过
 * （整合包环境里原版弓的 releaseUsing 常被重写，事件根本不发），则退回用
 * {@code EntityJoinLevelEvent} 拦截音爆弓射出的箭。两条路径以同 tick 标记去重。
 * <p>
 * 伤害公式：基础伤害 +（音爆等级-1）× 每级增伤 + 力量附魔等级 × 力量增幅（可配置）。
 * 特殊：avaritia 无限之弓（{@code avaritia:infinity_bow}）无视蓄力时长直接发射，
 * 且伤害固定为 int 最大值。
 */
@Mod.EventBusSubscriber(modid = "everlaartifacts")
public class SonicBoomHandler {

	/** 扫描步长（格），兼顾方块/实体判定精度与粒子密度 */
	private static final double STEP = 0.5D;
	/** 光束伤害判定半径（格） */
	private static final double BEAM_RADIUS = 0.1D;

	/** 每个玩家最近一次发射音爆的 game time，用于两条触发路径去重 */
	private static final Map<UUID, Long> LAST_SONIC_BOOM_TICK = new HashMap<>();

	@SubscribeEvent
	public static void onArrowLoose(ArrowLooseEvent event) {
		Player player = event.getEntity();
		ItemStack bow = event.getBow();
		int enchantLevel = EnchantmentHelper.getItemEnchantmentLevel(
			EverlaartifactsModEnchantments.SONIC_BOOM.get(), bow);
		if (enchantLevel <= 0) {
			return;
		}
		// 尊重其它模组/事件的取消
		if (event.isCanceled()) {
			return;
		}
		// 拉弓力度不足则不发射（avaritia 无限之弓无视蓄力时长）
		if (!isInfinityBow(bow) && BowItem.getPowerForTime(event.getCharge()) < 0.5F) {
			event.cancel();
			return;
		}
		// 客户端与服务端都取消，避免客户端预测出一支普通箭
		event.setCanceled(true);
		if (event.getLevel().isClientSide()) {
			return;
		}
		// 原版箭被替换为音爆：手动消耗弹药并损耗弓耐久
		if (!player.getAbilities().instabuild) {
			ItemStack projectile = player.getProjectile(bow);
			boolean infinite = projectile.is(Items.ARROW)
				&& EnchantmentHelper.getItemEnchantmentLevel(Enchantments.INFINITY_ARROWS, bow) > 0;
			if (!infinite && !projectile.isEmpty()) {
				projectile.shrink(1);
			}
		}
		bow.hurtAndBreak(1, player, p -> p.broadcastBreakEvent(Player.getEquipmentSlotForItem(bow)));

		ServerLevel level = (ServerLevel) event.getLevel();
		int powerLevel = EnchantmentHelper.getItemEnchantmentLevel(Enchantments.POWER_ARROWS, bow);
		float damage = computeDamage(enchantLevel, powerLevel, bow);
		Vec3 dir = player.getViewVector(1.0F);
		Vec3 start = player.getEyePosition().add(dir.scale(0.3D));

		fireSonicBoom(level, start, dir, damage, player);
		markFired(level, player);
	}

	/**
	 * 兜底触发：当 ArrowLooseEvent 被其它模组绕过（整合包里原版弓的 releaseUsing 被
	 * 重写，事件根本不发）时，改为拦截从音爆弓射出的箭：取消箭的生成并立即发射音爆。
	 * 若 ArrowLooseEvent 路径已在本 tick 发射过音爆，则只取消箭、不重复发射。
	 */
	@SubscribeEvent
	public static void onArrowJoin(EntityJoinLevelEvent event) {
		if (!(event.getEntity() instanceof AbstractArrow arrow)) {
			return;
		}
		if (arrow instanceof ThrownTrident) {
			return; // 三叉戟不是弓射出的箭，忽略
		}
		if (!(arrow.getOwner() instanceof Player player)) {
			return;
		}
		ItemStack bow = getSonicBoomBow(player);
		if (bow.isEmpty()) {
			return;
		}
		int enchantLevel = EnchantmentHelper.getItemEnchantmentLevel(
			EverlaartifactsModEnchantments.SONIC_BOOM.get(), bow);
		if (enchantLevel <= 0) {
			return;
		}
		// 音爆弓射出的箭一律不进入世界（同时消除客户端预测的"幽灵箭"）
		event.setCanceled(true);
		if (event.getLevel().isClientSide()) {
			return;
		}
		ServerLevel level = (ServerLevel) event.getLevel();
		if (justFiredSameTick(level, player)) {
			return; // ArrowLooseEvent 已在本 tick 发射过音爆
		}
		int powerLevel = EnchantmentHelper.getItemEnchantmentLevel(Enchantments.POWER_ARROWS, bow);
		float damage = computeDamage(enchantLevel, powerLevel, bow);
		Vec3 dir = player.getViewVector(1.0F);
		Vec3 start = player.getEyePosition().add(dir.scale(0.3D));
		fireSonicBoom(level, start, dir, damage, player);
		markFired(level, player);
	}

	/** 瞬间命中：从 start 沿 dir 一次性扫描整条路径，如同守卫者音爆。 */
	private static void fireSonicBoom(ServerLevel level, Vec3 start, Vec3 dir, float damage, Player owner) {
		// 播放守卫者音爆音效
		level.playSound(null, start.x, start.y, start.z, SoundEvents.WARDEN_SONIC_BOOM, SoundSource.PLAYERS, 1.0F, 1.0F);

		double maxRange = EverlaArtifactsConfig.getSonicBoomRange();
		boolean penetrateBlocks = EverlaArtifactsConfig.isSonicBoomPenetrateBlocks();
		boolean penetrateLiving = EverlaArtifactsConfig.isSonicBoomPenetrateLiving();
		boolean penetrateNonLiving = EverlaArtifactsConfig.isSonicBoomPenetrateNonLiving();
		Holder<DamageType> sonicBoomType = level.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE)
			.getHolderOrThrow(DamageTypes.SONIC_BOOM);
		DamageSource damageSource = new DamageSource(sonicBoomType, owner);

		Set<UUID> alreadyHit = new HashSet<>();
		Vec3 pos = start;
		double traveled = 0.0D;
		int step = 0;
		while (traveled < maxRange) {
			pos = pos.add(dir.scale(STEP));
			traveled += STEP;
			step++;

			// 方块判定：遇到完整方块且不允许穿透时停止
			BlockPos blockPos = BlockPos.containing(pos);
			BlockState blockState = level.getBlockState(blockPos);
			if (blockState.isCollisionShapeFullBlock(level, blockPos)) {
				if (!penetrateBlocks) {
					break;
				}
			}

			// 音爆粒子特效：每两格一个（奇数步），避免过密
			if ((step & 1) == 1) {
				spawnParticles(level, pos);
			}

			// 实体判定
			AABB box = new AABB(
				pos.x - BEAM_RADIUS, pos.y - BEAM_RADIUS, pos.z - BEAM_RADIUS,
				pos.x + BEAM_RADIUS, pos.y + BEAM_RADIUS, pos.z + BEAM_RADIUS);
			List<Entity> nearby = level.getEntities(owner, box, e -> e.isAlive() && e != owner);
			for (Entity entity : nearby) {
				if (entity instanceof LivingEntity living) {
					// 每个生物只受伤一次
					if (alreadyHit.add(living.getUUID())) {
						living.hurt(damageSource, damage);
					}
					// 不可穿透生物：命中第一个即停
					if (!penetrateLiving) {
						return;
					}
				} else if (entity instanceof EndCrystal crystal) {
					// 末影水晶：音爆可以对其造成伤害（水晶受伤即被摧毁并爆炸）
					if (alreadyHit.add(crystal.getUUID())) {
						crystal.hurt(damageSource, damage);
					}
					// 水晶是实体障碍，光束命中后即停
					return;
				} else {
					// 其它无生命实体：音爆永远不伤害它们，仅作为障碍
					if (!penetrateNonLiving) {
						return;
					}
				}
			}
		}
	}

	private static void spawnParticles(ServerLevel level, Vec3 pos) {
		level.sendParticles(ParticleTypes.SONIC_BOOM, pos.x, pos.y, pos.z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
	}

	/** 返回玩家手中带音爆附魔的弓（主手优先），没有则返回空 ItemStack。 */
	private static ItemStack getSonicBoomBow(Player player) {
		ItemStack main = player.getMainHandItem();
		if (EnchantmentHelper.getItemEnchantmentLevel(
			EverlaartifactsModEnchantments.SONIC_BOOM.get(), main) > 0) {
			return main;
		}
		ItemStack off = player.getOffhandItem();
		if (EnchantmentHelper.getItemEnchantmentLevel(
			EverlaartifactsModEnchantments.SONIC_BOOM.get(), off) > 0) {
			return off;
		}
		return ItemStack.EMPTY;
	}

	/** 计算音爆伤害：基础 + 音爆等级 + 力量附魔每级加成（可配置）；avaritia 无限之弓固定为 int 最大值。 */
	private static float computeDamage(int sonicBoomLevel, int powerLevel, ItemStack bow) {
		if (isInfinityBow(bow)) {
			return Integer.MAX_VALUE;
		}
		return (float) (EverlaArtifactsConfig.getSonicBoomBaseDamage()
			+ (sonicBoomLevel - 1) * EverlaArtifactsConfig.getSonicBoomDamagePerLevel()
			+ powerLevel * EverlaArtifactsConfig.getSonicBoomPowerDamagePerLevel());
	}

	/** Avaritia 无限之弓硬编码检测：无视蓄力时长，伤害为 int 最大值。 */
	private static boolean isInfinityBow(ItemStack bow) {
		ResourceLocation key = ForgeRegistries.ITEMS.getKey(bow.getItem());
		return key != null && key.getNamespace().equals("avaritia") && key.getPath().equals("infinity_bow");
	}

	/** 玩家是否恰在本 tick 发射过音爆（用于 ArrowLooseEvent 与 EntityJoinLevelEvent 双路径去重）。 */
	private static boolean justFiredSameTick(ServerLevel level, Player player) {
		Long last = LAST_SONIC_BOOM_TICK.get(player.getUUID());
		return last != null && last == level.getGameTime();
	}

	private static void markFired(ServerLevel level, Player player) {
		LAST_SONIC_BOOM_TICK.put(player.getUUID(), level.getGameTime());
	}
}
