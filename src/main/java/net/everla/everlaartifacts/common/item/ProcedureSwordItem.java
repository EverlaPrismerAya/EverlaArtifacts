package net.everla.everlaartifacts.common.item;
/**
 * 本类修改自如下开源软件/代码
 * https://github.com/Nova-Committee/Re-Avaritia ，采用 MIT 许可证。
 * 版权所有者：(c) 2024-2026 Nova-Committee
 */
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraftforge.fml.common.Mod;

import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.EnderDragonPart;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.level.Level;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.MinecraftServer;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraftforge.fml.ModList;

import net.everla.everlaartifacts.init.EverlaartifactsModItems;
import net.everla.everlaartifacts.init.EverlaartifactsModMobEffects;

import static net.minecraft.resources.ResourceLocation.fromNamespaceAndPath;
import static net.minecraft.sounds.SoundEvent.createVariableRangeEvent;
import static net.minecraft.sounds.SoundSource.HOSTILE;
import static net.minecraft.tags.DamageTypeTags.*;
import static net.minecraft.world.entity.EntityType.WITHER;
import static net.minecraft.world.entity.Pose.DYING;
import static net.minecraft.world.level.gameevent.GameEvent.ENTITY_DAMAGE;
import static net.minecraft.world.level.gameevent.GameEvent.ENTITY_DIE;

@Mod.EventBusSubscriber(modid = "everlaartifacts")
public class ProcedureSwordItem extends SwordItem {
	//大部分日志正常情况下应该注释掉
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

		if (!level.isClientSide && level instanceof ServerLevel serverLevel) {

			// 检查玩家是否具有认知错乱效果
			boolean hasCognitiveDisorder = player.hasEffect(EverlaartifactsModMobEffects.COGNITIVE_DISORDER.get());
			// 创建伤害源
			var damageSource = player.damageSources().playerAttack(player);
			// 特殊处理
			if (hasCognitiveDisorder){
				// 末影龙
				if (entity instanceof EnderDragonPart dragonPart) {
				EnderDragon dragon = dragonPart.parentMob;  // 获取真正的末影龙实体
					dragon.hurt(dragon.head, damageSource, 33550336.0F);
				}
				// 草飞混沌守卫
				// 检查Draconic Evolution模组是否加载
				if (ModList.get().isLoaded("draconicevolution")) {
					try {
						MinecraftServer server = serverLevel.getServer();
						//获取命令执行者
						CommandSourceStack commandSource = entity.createCommandSourceStack()
							.withSuppressedOutput()
							.withPermission(4)
							.withPosition(entity.position())
							.withLevel(serverLevel);
						//使用Data命令设置生命值为0而非SetHealth以绕过龙研反作弊检测
						String command = "execute as @e[type=draconicevolution:draconic_guardian,distance=..10] run data modify entity @s Health set value 0";
						server.getCommands().performPrefixedCommand(commandSource, command);
					} catch (Exception e) {
						System.out.println("Failed to run command: " + e.getMessage());
					}
				}
			}

			// 普通实体处理
			if (entity instanceof LivingEntity victim){

				// 检查实体是否被命名为"Procedure"或者玩家具有认知错乱效果
				if ((victim.hasCustomName() && "Procedure".equals(victim.getCustomName().getString())) || hasCognitiveDisorder) {
					//哦还活着
					if (victim.getHealth() > 0 ){
						// 特殊奖励处理
						// 经验值处理
						if (victim.getExperienceReward() > 0){
							int experienceReward = victim.getExperienceReward() * 10;
							// 掉落经验球
							int orbCount = Math.min(experienceReward, 10); // 最多单组生成个数
							int experiencePerOrb = experienceReward / orbCount;
							int remainingExperience = experienceReward % orbCount;

							for (int j = 0; j < orbCount; j++) {
								int currentExp = experiencePerOrb + (j < remainingExperience ? 1 : 0);
								ExperienceOrb orb = new ExperienceOrb(serverLevel, victim.getX(), victim.getY(), victim.getZ(), currentExp);
								serverLevel.addFreshEntity(orb);
							}
						}
						// 掉落物处理
						try {
							//反射获取战利品
							java.lang.reflect.Method dropMethod = LivingEntity.class.getDeclaredMethod("dropFromLootTable", DamageSource.class, boolean.class);
							dropMethod.setAccessible(true);
							for (int i = 0; i < 10; i++){
								//掉落战利品
								dropMethod.invoke(victim, damageSource, true);
								//下界之星是硬编码 所以说这里也硬编码
								if (victim.getType() == WITHER){
									ItemEntity netherStar = new ItemEntity(serverLevel, victim.getX(), victim.getY(), victim.getZ(), new ItemStack(Items.NETHER_STAR));
									netherStar.setPickUpDelay(10);
									serverLevel.addFreshEntity(netherStar);
								}
							}
						} catch (Exception e) {
							System.out.println("Failed to drop loot: " + e.getMessage());
						}

						// 播放Deltarune爆炸音效
						serverLevel.playSound(null, victim.getX(), victim.getY(), victim.getZ(),
								createVariableRangeEvent(
										fromNamespaceAndPath("everlaartifacts", "deltarune_explosion")
								), HOSTILE, 1.0F, 1.0F);

					}
					// 完整的秒杀逻辑，仿寰宇支配之剑
					// 处理不同类型的受害者
					if (victim instanceof Player pvp) {
						// PvP处理
						this.hurt(victim, damageSource, 33550336.0F);
					} else {
						// 普通生物处理
						this.hurt(victim, damageSource, 33550336.0F);
					}

					// 死亡后处理
					if (victim.isDeadOrDying()) {
						victim.setHealth(0);
						this.die(victim, damageSource);
						player.killedEntity(serverLevel, victim);
					}
					return true;
				}
			}
		}

		// 如果不符合，则使用普通剑的攻击逻辑
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
			victim.gameEvent(ENTITY_DAMAGE);
			victim.hurtDuration = 10;
			victim.hurtTime = victim.hurtDuration;

			Entity entity1 = pSource.getEntity();
			if (entity1 != null) {
				if (entity1 instanceof LivingEntity livingentity1) {
					if (!pSource.is(NO_ANGER)) {
						victim.setLastHurtByMob(livingentity1);
					}
				}

				if (entity1 instanceof Player player1) {
					victim.setLastHurtByPlayer(player1);
				} else if (entity1 instanceof TamableAnimal tamableEntity) {
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

			if (!pSource.is(NO_IMPACT)) {
				victim.hurtMarked = true;
			}

			if (entity1 != null && !pSource.is(IS_EXPLOSION)) {
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
					victim.gameEvent(ENTITY_DIE);
					this.createWitherRose(victim, livingentity);
				}

				victim.level().broadcastEntityEvent(victim, (byte) 3);
			}

			victim.setPose(DYING);
		}
	}

	/**
	 * 创建凋零玫瑰
	 */
	protected void createWitherRose(LivingEntity victim, LivingEntity pEntitySource) {
		if (!victim.level().isClientSide) {
			boolean flag = false;
			if (pEntitySource instanceof WitherBoss) {
				// 凋零boss击杀时生成凋零玫瑰
				flag = true;
			}

			if (!flag) {
				// 默认不生成凋零玫瑰
			}
		}
	}
}
