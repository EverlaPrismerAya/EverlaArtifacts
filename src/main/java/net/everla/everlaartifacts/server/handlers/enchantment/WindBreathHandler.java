package net.everla.everlaartifacts.server.handlers.enchantment;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import net.everla.everlaartifacts.EverlaartifactsMod;
import net.everla.everlaartifacts.init.EverlaartifactsModEnchantments;

@Mod.EventBusSubscriber(modid = "everlaartifacts")
public class WindBreathHandler {

	/** 风息爆炸半径（方块） */
	private static final double EXPLOSION_RADIUS = 4.0;
	/** 击退基础倍率 */
	private static final double KNOCKBACK_BASE = 1;
	/** 击退每级额外倍率 */
	private static final double KNOCKBACK_PER_LEVEL = 0.75;
	/** 额外冰冻伤害基础值 */
	private static final float DAMAGE_BASE = 4.0F;
	/** 额外冰冻伤害每级增加值 */
	private static final float DAMAGE_PER_LEVEL = 3.0F;

	/**
	 * 在 LivingHurtEvent 中将冰冻伤害合并到物理伤害中，
	 * 确保只有一次 hurt() 调用，物理伤害 + 冰冻伤害合并计算，且不影响无敌帧。
	 * 优先级设为 LOW，在其他附魔处理完之后追加。
	 */
	@SubscribeEvent(priority = EventPriority.LOW)
	public static void onLivingHurt(LivingHurtEvent event) {
		if (!(event.getSource().getEntity() instanceof Player player)) {
			return;
		}

		ItemStack mainHand = player.getMainHandItem();
		int windBreathLevel = EnchantmentHelper.getItemEnchantmentLevel(
			EverlaartifactsModEnchantments.WIND_BREATH.get(), mainHand);

		if (windBreathLevel <= 0) {
			return;
		}

		// 将冰冻伤害追加到原版物理伤害上
		float extraDamage = DAMAGE_BASE + (windBreathLevel - 1) * DAMAGE_PER_LEVEL;
		event.setAmount(event.getAmount() + extraDamage);
	}

	/**
	 * 在 AttackEntityEvent 中检测风息附魔，不取消原版攻击（保证横扫之刃正常运作），
	 * 将风压吸引、粒子、音效延迟到本 tick 末尾执行。
	 */
	@SubscribeEvent
	public static void onPlayerAttack(AttackEntityEvent event) {
		Player player = event.getEntity();
		if (!(event.getTarget() instanceof LivingEntity target)) {
			return;
		}

		Level level = player.level();
		if (level.isClientSide()) {
			return;
		}

		ItemStack mainHand = player.getMainHandItem();
		int windBreathLevel = EnchantmentHelper.getItemEnchantmentLevel(
			EverlaartifactsModEnchantments.WIND_BREATH.get(), mainHand);

		if (windBreathLevel <= 0) {
			return;
		}

		// 不取消事件，让原版攻击（物理伤害 + 横扫之刃等）正常进行
		// 将风压吸引、粒子、音效延迟到本 tick 末尾
		ServerLevel serverLevel = (ServerLevel) level;
		int levelCopy = windBreathLevel;
		EverlaartifactsMod.queueServerWork(1, () ->
			applyWindBreathEffects(target, levelCopy, serverLevel));
	}

	private static void applyWindBreathEffects(LivingEntity target, int level, ServerLevel serverLevel) {
		// === 1. 风息吸引：将半径内所有存活生物拉向目标位置 ===
		double knockbackMultiplier = KNOCKBACK_BASE + (level - 1) * KNOCKBACK_PER_LEVEL;
		Vec3 targetPos = target.position();
		AABB explosionBox = target.getBoundingBox().inflate(EXPLOSION_RADIUS);

		for (LivingEntity nearbyEntity : serverLevel.getEntitiesOfClass(LivingEntity.class, explosionBox,
				LivingEntity::isAlive)) {
			Vec3 direction = targetPos.subtract(nearbyEntity.position());
			double distance = direction.length();
			if (distance > 0.01) {
				double falloff = Math.max(0.0, 1.0 - distance / EXPLOSION_RADIUS);
				Vec3 knockbackVec = direction.normalize().scale(knockbackMultiplier * falloff);
				nearbyEntity.setDeltaMovement(nearbyEntity.getDeltaMovement().add(knockbackVec));
				nearbyEntity.hurtMarked = true;
			}
		}

		// === 2. 白色烟雾粒子（即使目标已死亡也生成） ===
		spawnWindParticles(serverLevel, target);

		// === 3. 风爆音效（即使目标已死亡也播放） ===
		serverLevel.playSound(null, target.getX(), target.getY(), target.getZ(),
			SoundEvents.GENERIC_EXPLODE,
			SoundSource.PLAYERS, 0.6F, 1.5F);
	}

	private static void spawnWindParticles(ServerLevel serverLevel, LivingEntity target) {
		// 目标位置大量白色烟雾
		serverLevel.sendParticles(
			ParticleTypes.CLOUD,
			target.getX(),
			target.getY() + target.getBbHeight() * 0.5,
			target.getZ(),
			40,   // 粒子数量
			1.5,  // x轴扩散
			1.0,  // y轴扩散
			1.5,  // z轴扩散
			0.05  // 粒子速度
		);

		// 目标周围环形+径向粒子，营造风涡效果
		for (int ring = 0; ring < 3; ring++) {
			double ringRadius = 0.8 + ring * 1.0;
			int particlesPerRing = 8 + ring * 4;
			for (int i = 0; i < particlesPerRing; i++) {
				double angle = (2 * Math.PI / particlesPerRing) * i;
				double px = target.getX() + Math.cos(angle) * ringRadius;
				double pz = target.getZ() + Math.sin(angle) * ringRadius;
				serverLevel.sendParticles(
					ParticleTypes.CLOUD,
					px,
					target.getY() + 0.2 + ring * 0.3,
					pz,
					2,
					0.15, 0.1, 0.15,
					0.02
				);
			}
		}
	}
}
