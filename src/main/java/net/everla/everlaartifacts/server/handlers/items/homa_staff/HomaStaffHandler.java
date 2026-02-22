package net.everla.everlaartifacts.server.handlers.items.homa_staff;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.network.chat.Component;

import net.everla.everlaartifacts.init.EverlaartifactsModMobEffects;
import net.everla.everlaartifacts.common.difficulty.DifficultyLevel;
import net.everla.everlaartifacts.server.handlers.difficulty.WorldSeedChecker;
import net.everla.everlaartifacts.server.handlers.commands.EverlaKillHandler;
import net.everla.everlaartifacts.EverlaartifactsMod;

public class HomaStaffHandler {
	// 存储需要监控的玩家及其监控开始时间
	private static final java.util.Map<LivingEntity, Long> PLAYER_DEATH_WATCH_LIST = new java.util.WeakHashMap<>();
	
	public static void handleHomaStaffActivation(Entity entity, ItemStack itemstack) {
		if (entity == null)
			return;
		
		// 检查是否为Extra难度
		if (entity.level() != null && !entity.level().isClientSide()) {
			// 检查是否为特殊种子世界且难度为EXTRA
			if (WorldSeedChecker.isSpecialSeedWorld() && 
			    WorldSeedChecker.getCurrentWorldDifficulty(entity.level().getServer()) == DifficultyLevel.EXTRA) {
				
				// 对使用者造成巨额伤害（创造模式下不执行）
				if (entity instanceof LivingEntity livingEntity && !(entity instanceof Player player && player.isCreative())) {
					// 计算伤害：最大生命值 × 7777777%
					float maxHealth = livingEntity.getMaxHealth();
					float damage = maxHealth * 77777.77f; // 7777777% = 77777.77倍
					
					// 限制伤害不超过Integer.MAX_VALUE
					if (damage > 2147483647.0f) {
						damage = 2147483647.0f;
					}
					
					// 创建自定义伤害源
					DamageSource homaOverburnDamage = createHomaOverburnDamageSource(livingEntity);
					
					// 绕过无敌帧
					int originalInvulnerableTime = livingEntity.invulnerableTime;
					livingEntity.invulnerableTime = 0;
					
					// 造成伤害
					livingEntity.hurt(homaOverburnDamage, damage);
					
					// 恢复无敌帧时间
					livingEntity.invulnerableTime = originalInvulnerableTime;
					
					// 向玩家发送本地化消息（创造模式下不发送）
					if (entity instanceof Player player) {
						player.sendSystemMessage(Component.translatable("chat.everlaartifacts.extra.homa_staff"));
						
						// 添加到死亡监控列表
						PLAYER_DEATH_WATCH_LIST.put(livingEntity, System.currentTimeMillis());
						
						// 调度死亡检查任务
						scheduleDeathCheck(livingEntity);
					}
				}
			} else {
				// 减少生命值（保留30%的生命值，最少保留1点）（创造模式下不执行）
				if (!(entity instanceof Player player && player.isCreative())) {
					if ((entity instanceof LivingEntity _livEnt ? _livEnt.getHealth() : -1) * 0.3 >= 1) {
						if (entity instanceof LivingEntity _entity)
							_entity.setHealth((float) ((entity instanceof LivingEntity _livEnt ? _livEnt.getHealth() : -1) * 0.7));
					} else {
						if (entity instanceof LivingEntity _entity)
							_entity.setHealth(1);
					}
				}
			}
		}
		// 添加Homa Active状态效果
		if (entity instanceof LivingEntity _entity && !_entity.level().isClientSide())
			_entity.addEffect(new MobEffectInstance(EverlaartifactsModMobEffects.HOMA_ACTIVE.get(), 180, 0, true, true));
		
		// 设置物品冷却
		if (entity instanceof Player _player)
			_player.getCooldowns().addCooldown(itemstack.getItem(), 320);
		
		// 播放firecharge.use音效
		entity.level().playSound(null, entity.getX(), entity.getY(), entity.getZ(), 
			SoundEvents.FIRECHARGE_USE, SoundSource.PLAYERS, 1.0F, 1.0F);
	}
	public static void handleHomaPassiveEffect(Entity entity) {
		if (entity == null)
			return;
		
		// 添加Homa Passive状态效果
		if (entity instanceof LivingEntity _entity && !_entity.level().isClientSide())
			_entity.addEffect(new MobEffectInstance(EverlaartifactsModMobEffects.HOMA_PASSIVE.get(), 5, 0, true, true));
	}
	
	/**
	 * 创建homa_overburn自定义伤害源
	 */
	private static DamageSource createHomaOverburnDamageSource(LivingEntity entity) {
		// 创建基于数据包的homa_overburn伤害源
		Holder<DamageType> damageType = entity.level().registryAccess()
			.registryOrThrow(Registries.DAMAGE_TYPE)
			.getHolder(ResourceKey.create(Registries.DAMAGE_TYPE, 
				ResourceLocation.fromNamespaceAndPath("everlaartifacts", "homa_overburn")))
			.orElseThrow();
		return new DamageSource(damageType);
	}
	
	/**
	 * 调度死亡检查任务
	 * @param player 要监控的玩家
	 */
	private static void scheduleDeathCheck(LivingEntity player) {
		// 20刻后检查玩家是否还活着
		EverlaartifactsMod.queueServerWork(20, () -> {
			// 检查玩家是否仍在监控列表中且仍然存活
			if (PLAYER_DEATH_WATCH_LIST.containsKey(player) && player.isAlive()) {
				// 发送"诶你怎么不死啊？"消息
				if (player instanceof Player p && !p.isCreative()) {
					p.sendSystemMessage(Component.translatable("chat.everlaartifacts.extra.homa_staff.why"));
				}
				
				// 5刻后执行愤怒惩罚
				EverlaartifactsMod.queueServerWork(5, () -> {
					// 再次检查玩家状态
					if (PLAYER_DEATH_WATCH_LIST.containsKey(player) && player.isAlive()) {
						// 发送"死！"消息 并杀死玩家
						if (player instanceof Player p && !p.isCreative()) {
							p.sendSystemMessage(Component.translatable("chat.everlaartifacts.extra.homa_staff.enrage"));
							Component deathMessage = Component.translatable(
									"text.everlaartifacts.homa_kill",
									p.getDisplayName()
							);
							EverlaKillHandler.killPlayer(
									p,
									"everlaartifacts:homa_kill",
									deathMessage,
									ResourceLocation.tryParse("everlaartifacts:deltarune_explosion")
							);
						}
						
						// 从监控列表中移除
						PLAYER_DEATH_WATCH_LIST.remove(player);
					} else {
						// 玩家已经死亡，从监控列表中移除
						PLAYER_DEATH_WATCH_LIST.remove(player);
					}
				});
			} else {
				// 玩家已经死亡，从监控列表中移除
				PLAYER_DEATH_WATCH_LIST.remove(player);
			}
		});
	}
}