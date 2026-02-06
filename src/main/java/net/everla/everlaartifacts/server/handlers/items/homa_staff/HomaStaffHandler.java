package net.everla.everlaartifacts.server.handlers.items.homa_staff;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;

import net.everla.everlaartifacts.init.EverlaartifactsModMobEffects;

public class HomaStaffHandler {
	public static void handleHomaStaffActivation(Entity entity, ItemStack itemstack) {
		if (entity == null)
			return;
		
		// 减少生命值（保留30%的生命值，最少保留1点）
		if ((entity instanceof LivingEntity _livEnt ? _livEnt.getHealth() : -1) * 0.3 >= 1) {
			if (entity instanceof LivingEntity _entity)
				_entity.setHealth((float) ((entity instanceof LivingEntity _livEnt ? _livEnt.getHealth() : -1) * 0.7));
		} else {
			if (entity instanceof LivingEntity _entity)
				_entity.setHealth(1);
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
}