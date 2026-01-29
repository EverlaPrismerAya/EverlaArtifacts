package net.everla.everlaartifacts.procedures;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.effect.MobEffectInstance;

import net.everla.everlaartifacts.init.EverlaartifactsModMobEffects;

public class HomaTickFuncProcedure {
	public static void execute(Entity entity) {
		if (entity == null)
			return;
		if (entity instanceof LivingEntity _entity && !_entity.level().isClientSide())
			_entity.addEffect(new MobEffectInstance(EverlaartifactsModMobEffects.HOMA_PASSIVE.get(), 5, 0, true, true));
	}
}
