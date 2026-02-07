
package net.everla.everlaartifacts.item;

import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.level.Level;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;

import net.everla.everlaartifacts.init.EverlaartifactsModItems;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Mod.EventBusSubscriber(modid = "everlaartifacts")
public class ProcedureSwordItem extends SwordItem {
	// 记录已触发特殊奖励的实体UUID，防止重复触发
	private static final ConcurrentHashMap<UUID, Boolean> TRIGGERED_ENTITIES = new ConcurrentHashMap<>();

	public ProcedureSwordItem() {
		super(new Tier() {
			public int getUses() {
				return 3389;
			}

			public float getSpeed() {
				return 12f;
			}

			public float getAttackDamageBonus() {
				return 8f;
			}

			public int getLevel() {
				return 0;
			}

			public int getEnchantmentValue() {
				return 15;
			}

			public Ingredient getRepairIngredient() {
				return Ingredient.of(new ItemStack(EverlaartifactsModItems.AURIC_INGOT.get()));
			}
		}, 3, -2.4f, new Item.Properties().fireResistant().rarity(Rarity.EPIC));
	}
	@Override
	public boolean onLeftClickEntity(ItemStack stack, Player player, Entity entity) {
		var level = player.level();

		if (!level.isClientSide && level instanceof ServerLevel serverLevel && entity instanceof LivingEntity victim) {
			// 检查实体是否被命名为"Procedure"
			if (victim.hasCustomName() && "Procedure".equals(victim.getCustomName().getString())) {
				// 安全检查：防止重复触发
				UUID entityUUID = victim.getUUID();
				if (TRIGGERED_ENTITIES.putIfAbsent(entityUUID, Boolean.TRUE) != null) {
					// 该实体已触发过，使用普通攻击逻辑
					return super.onLeftClickEntity(stack, player, entity);
				}

				// 完整的秒杀逻辑，仿照Infinity Sword
				DamageSource damageSource = player.damageSources().playerAttack(player);

				// 处理不同类型的受害者
				if (victim instanceof net.minecraft.world.entity.boss.enderdragon.EnderDragon dragon) {
					// 末影龙特殊处理
					dragon.hurt(dragon.head, damageSource, Float.MAX_VALUE);
				} else if (victim instanceof Player pvp) {
					// PvP处理
					this.hurt(victim, damageSource, Float.MAX_VALUE);
				} else {
					// 普通生物处理
					this.hurt(victim, damageSource, Float.MAX_VALUE);
				}

				// 特殊奖励处理（在死亡前执行）
				// 给予99点经验值
				player.giveExperiencePoints(99);

				// 从战利品表掉落10次物品
				try {
					java.lang.reflect.Method dropMethod = LivingEntity.class.getDeclaredMethod("dropFromLootTable", DamageSource.class, boolean.class);
					dropMethod.setAccessible(true);
					for (int i = 0; i < 10; i++) {
						dropMethod.invoke(victim, damageSource, true);
					}
				} catch (Exception e) {
					System.out.println("Failed to drop loot: " + e.getMessage());
				}

				// 播放Deltarune爆炸音效
				serverLevel.playSound(null, victim.getX(), victim.getY(), victim.getZ(),
						net.minecraft.sounds.SoundEvent.createVariableRangeEvent(
								net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("everlaartifacts", "deltarune_explosion")
						), net.minecraft.sounds.SoundSource.HOSTILE, 1.0F, 1.0F);

				// 死亡后处理
				if (victim.isDeadOrDying()) {
					victim.setHealth(0);
					this.die(victim, damageSource);
					player.killedEntity(serverLevel, victim);
				}

				return true;
			}
		}

		// 如果不是名为"Procedure"的实体，则使用普通剑的攻击逻辑
		return super.onLeftClickEntity(stack, player, entity);
	}


	/**
	 * 造成伤害的核心方法
	 */
	public boolean hurt(LivingEntity victim, DamageSource pSource, float pAmount) {
		if (victim.level().isClientSide) {
			return false;
		} else if (victim.isDeadOrDying()) {
			return false;
		} else {
			if (victim.isSleeping() && !victim.level().isClientSide) {
				victim.stopSleeping();
			}

			victim.setNoActionTime(0);
			victim.walkAnimation.setSpeed(1.5F);
			victim.invulnerableTime = 20;
			victim.getCombatTracker().recordDamage(pSource, pAmount);
			victim.setHealth(victim.getHealth() - pAmount);
			victim.gameEvent(net.minecraft.world.level.gameevent.GameEvent.ENTITY_DAMAGE);
			victim.hurtDuration = 10;
			victim.hurtTime = victim.hurtDuration;

			Entity entity1 = pSource.getEntity();
			if (entity1 != null) {
				if (entity1 instanceof LivingEntity livingentity1) {
					if (!pSource.is(net.minecraft.tags.DamageTypeTags.NO_ANGER)) {
						victim.setLastHurtByMob(livingentity1);
					}
				}

				if (entity1 instanceof Player player1) {
					victim.setLastHurtByPlayer(player1);
				} else if (entity1 instanceof net.minecraft.world.entity.TamableAnimal tamableEntity) {
					if (tamableEntity.isTame()) {
						LivingEntity livingentity2 = tamableEntity.getOwner();
						if (livingentity2 instanceof Player player2) {
							victim.setLastHurtByPlayer(player2);
						} else {
							victim.setLastHurtByPlayer(null);
						}
					}
				}
			}

			victim.level().broadcastDamageEvent(victim, pSource);

			if (!pSource.is(net.minecraft.tags.DamageTypeTags.NO_IMPACT)) {
				victim.hurtMarked = true;
			}

			if (entity1 != null && !pSource.is(net.minecraft.tags.DamageTypeTags.IS_EXPLOSION)) {
				double d0 = entity1.getX() - victim.getX();
				double d1;
				for (d1 = entity1.getZ() - victim.getZ(); d0 * d0 + d1 * d1 < 1.0E-4D; d1 = (Math.random() - Math.random()) * 0.01D) {
					d0 = (Math.random() - Math.random()) * 0.01D;
				}

				victim.knockback(0.4F, d0, d1);
				victim.indicateDamage(d0, d1);
			}

			if (victim.isDeadOrDying()) {
				this.die(victim, pSource);
			} else {
				SoundEvent soundevent = SoundEvents.GENERIC_HURT;
				victim.playSound(soundevent, 2F, victim.getVoicePitch());
			}

			if (victim instanceof ServerPlayer) {
				CriteriaTriggers.ENTITY_HURT_PLAYER.trigger((ServerPlayer) victim, pSource, pAmount, pAmount, false);
			}

			if (entity1 instanceof ServerPlayer) {
				CriteriaTriggers.PLAYER_HURT_ENTITY.trigger((ServerPlayer) entity1, victim, pSource, pAmount, pAmount, false);
			}

			return true;
		}
	}

	/**
	 * 执行死亡逻辑
	 */
	public void die(LivingEntity victim, DamageSource pDamageSource) {
		if (!victim.isRemoved() && !victim.isDeadOrDying()) {
			Entity entity = pDamageSource.getEntity();
			LivingEntity livingentity = victim.getKillCredit();

			// 授予击杀分数
			if (livingentity != null) {
				livingentity.awardKillScore(victim, 1, pDamageSource);
			}

			if (victim.isSleeping()) {
				victim.stopSleeping();
			}

			if (!victim.level().isClientSide && victim.hasCustomName()) {
				// 记录命名实体死亡日志
				System.out.println("Named entity died: " + victim.getCustomName().getString());
			}

			// 标记为死亡
			victim.setHealth(0);
			victim.getCombatTracker().recheckStatus();
			Level level = victim.level();

			if (level instanceof ServerLevel serverlevel) {
				if (entity == null || entity.killedEntity(serverlevel, victim)) {
					victim.gameEvent(net.minecraft.world.level.gameevent.GameEvent.ENTITY_DIE);
					this.createWitherRose(victim, livingentity);
				}

				victim.level().broadcastEntityEvent(victim, (byte) 3);
			}

			victim.setPose(net.minecraft.world.entity.Pose.DYING);
		}
	}

	/**
	 * 创建凋零玫瑰
	 */
	protected void createWitherRose(LivingEntity victim, LivingEntity pEntitySource) {
		if (!victim.level().isClientSide) {
			boolean flag = false;
			if (pEntitySource instanceof net.minecraft.world.entity.boss.wither.WitherBoss) {
				// 凋零boss击杀时生成凋零玫瑰
				flag = true;
			}

			if (!flag) {
				// 默认不生成凋零玫瑰
			}
		}
	}
	
	/**
	 * 监听实体死亡事件，清理UUID记录防止内存泄漏
	 */
	@SubscribeEvent
	public static void onLivingDeath(LivingDeathEvent event) {
		// 在实体死亡时移除其UUID记录
		UUID entityUUID = event.getEntity().getUUID();
		TRIGGERED_ENTITIES.remove(entityUUID);
	}
}
