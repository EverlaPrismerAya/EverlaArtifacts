package net.everla.everlaartifacts.potion;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffect;

// 移除对旧Procedure的依赖，使用新的Handler
public class BedmicDestructionMobEffect extends MobEffect {
	public BedmicDestructionMobEffect() {
		super(MobEffectCategory.HARMFUL, -16711783);
	}

	@Override
	public void applyEffectTick(LivingEntity entity, int amplifier) {
		// 效果处理现在由新的Handler在server/handlers/effects/BedmicDestructionHandler.java中处理
		// 这里不需要任何实现
	}

	@Override
	public boolean isDurationEffectTick(int duration, int amplifier) {
		// 返回true以确保效果持续触发，但实际逻辑由Handler处理
		return true;
	}
}