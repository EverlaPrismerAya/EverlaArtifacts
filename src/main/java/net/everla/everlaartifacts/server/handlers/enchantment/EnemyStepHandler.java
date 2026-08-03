package net.everla.everlaartifacts.server.handlers.enchantment;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingFallEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraft.resources.ResourceLocation;

import net.everla.everlaartifacts.init.EverlaartifactsModEnchantments;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Mod.EventBusSubscriber(modid = "everlaartifacts")
public class EnemyStepHandler {

	/** 踩踏检测半径（方块） */
	private static final double STOMP_RADIUS = 2.2;
	/** 踩踏最小距离（避免玩家自身） */
	private static final double MIN_STOMP_DISTANCE = 0.5;
	/** 踩踏伤害值 */
	private static final float STOMP_DAMAGE = 7.0F;
	/** 弹起速度 */
	private static final double BOUNCE_VELOCITY = 1.0;
	/** 触发踩踏的最低下落速度（方块/tick） */
	private static final double FALLING_SPEED_THRESHOLD = 0.8;
	/** 摔落伤害减免基础值 */
	private static final float FALL_DAMAGE_REDUCTION_BASE = 9.0F;
	/** 摔落伤害减免每级增加值 */
	private static final float FALL_DAMAGE_REDUCTION_PER_LEVEL = 1.0F;

	private static final Map<UUID, Integer> STEP_COUNTERS = new ConcurrentHashMap<>();
	/** 上一 tick 玩家的 Y 坐标，用于计算下落速度 */
	private static final Map<UUID, Double> PREV_Y = new ConcurrentHashMap<>();

	// ── 摔落伤害减免（来自 enchantment JSON 的 damage_protection） ──────

	@SubscribeEvent(priority = EventPriority.LOW)
	public static void onLivingFall(LivingFallEvent event) {
		if (!(event.getEntity() instanceof Player player)) {
			return;
		}

		ItemStack boots = player.getItemBySlot(EquipmentSlot.FEET);
		int enemyStepLevel = EnchantmentHelper.getItemEnchantmentLevel(
			EverlaartifactsModEnchantments.ENEMY_STEP.get(), boots);

		if (enemyStepLevel <= 0) {
			return;
		}

		// 减免摔落距离（每点减免 = 少受 1 点摔落伤害）
		float reduction = FALL_DAMAGE_REDUCTION_BASE + (enemyStepLevel - 1) * FALL_DAMAGE_REDUCTION_PER_LEVEL;
		event.setDistance(Math.max(0.0F, event.getDistance() - reduction));
	}

	// ── 主动踩踏机制（来自 mcfunction） ──────────────────────────────────

	@SubscribeEvent
	public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
		if (event.phase != TickEvent.Phase.END) {
			return;
		}
		if (!(event.player instanceof ServerPlayer player)) {
			return;
		}
		if (player.isSpectator()) {
			STEP_COUNTERS.remove(player.getUUID());
			PREV_Y.remove(player.getUUID());
			return;
		}

		ItemStack boots = player.getItemBySlot(EquipmentSlot.FEET);
		int enemyStepLevel = EnchantmentHelper.getItemEnchantmentLevel(
			EverlaartifactsModEnchantments.ENEMY_STEP.get(), boots);

		if (enemyStepLevel <= 0) {
			STEP_COUNTERS.remove(player.getUUID());
			PREV_Y.remove(player.getUUID());
			return;
		}

		UUID uuid = player.getUUID();
		double currentY = player.getY();
		double prevY = PREV_Y.getOrDefault(uuid, currentY);
		double fallingSpeed = prevY - currentY; // 正值 = 下落

		// 玩家正在以足够速度下落 → 检查周围是否有可踩踏的生物
		if (fallingSpeed >= FALLING_SPEED_THRESHOLD) {
			Level level = player.level();
			AABB stompBox = player.getBoundingBox().inflate(STOMP_RADIUS);

			List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class, stompBox,
				e -> e != player && e.isAlive() && player.distanceTo(e) >= MIN_STOMP_DISTANCE);

			if (!targets.isEmpty()) {
				ServerLevel serverLevel = (ServerLevel) level;

				// 对范围内所有敌人造成伤害
				for (LivingEntity target : targets) {
					target.hurt(level.damageSources().playerAttack(player), STOMP_DAMAGE);
				}

				// 弹起玩家：无视当前下落速度，将垂直速度重置为向上弹起
				// 注意：必须用 hurtMarked（而非 hasImpulse）——hasImpulse 只广播给旁观者，
				// hurtMarked 才会通过 broadcastAndSend 把速度同步回玩家自己（本端）。
				Vec3 motion = player.getDeltaMovement();
				player.setDeltaMovement(motion.x, Math.max(motion.y, BOUNCE_VELOCITY), motion.z);
				player.hurtMarked = true;

				// 踩踏计数：1→"背!" 2→"背!" 3→"踩!" 循环
				int counter = STEP_COUNTERS.getOrDefault(uuid, 0) + 1;
				if (counter > 3) {
					counter = 1;
				}
				STEP_COUNTERS.put(uuid, counter);

				// 被踩踏生物脚下方块的粒子
				spawnStompParticles(serverLevel, targets);

				// 重落地音效
				serverLevel.playSound(null, player.getX(), player.getY(), player.getZ(),
					SoundEvents.GENERIC_BIG_FALL, SoundSource.PLAYERS, 0.8F, 0.9F);

				// 踩踏语音：1-2 → c.ogg（踩！），3 → b.ogg（背！）
				String soundName = counter <= 2 ? "enemy_step_c" : "enemy_step_b";
				serverLevel.playSound(null, player.getX(), player.getY(), player.getZ(),
					ForgeRegistries.SOUND_EVENTS.getValue(
						ResourceLocation.fromNamespaceAndPath("everlaartifacts", soundName)),
					SoundSource.PLAYERS, 1.0F, 1.0F);
			}
		}

		// 更新上一 tick 的 Y 坐标
		PREV_Y.put(uuid, currentY);
	}

	private static void spawnStompParticles(ServerLevel serverLevel, List<LivingEntity> targets) {
		for (LivingEntity target : targets) {
			BlockPos feetPos = target.getOnPos();
			BlockState blockState = serverLevel.getBlockState(feetPos);

			if (blockState.isAir()) {
				BlockPos below = feetPos.below();
				BlockState belowState = serverLevel.getBlockState(below);
				if (!belowState.isAir()) {
					blockState = belowState;
					feetPos = below;
				}
			}

			if (!blockState.isAir()) {
				// 中心喷发
				serverLevel.sendParticles(
					new BlockParticleOption(ParticleTypes.BLOCK, blockState),
					target.getX(),
					feetPos.getY() + 1.0,
					target.getZ(),
					40, 0.8, 0.4, 0.8, 0.15);

				// 环形扩散层
				for (int ring = 0; ring < 2; ring++) {
					double ringRadius = 0.6 + ring * 0.8;
					int particlesPerRing = 12 + ring * 8;
					for (int i = 0; i < particlesPerRing; i++) {
						double angle = (2 * Math.PI / particlesPerRing) * i;
						double px = target.getX() + Math.cos(angle) * ringRadius;
						double pz = target.getZ() + Math.sin(angle) * ringRadius;
						serverLevel.sendParticles(
							new BlockParticleOption(ParticleTypes.BLOCK, blockState),
							px,
							feetPos.getY() + 0.2 + ring * 0.4,
							pz,
							3, 0.3, 0.15, 0.3, 0.08);
					}
				}
			}
		}
	}
}
