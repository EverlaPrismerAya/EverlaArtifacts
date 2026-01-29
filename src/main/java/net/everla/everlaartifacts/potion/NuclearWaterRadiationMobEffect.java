
package net.everla.everlaartifacts.potion;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffect;

import net.everla.everlaartifacts.procedures.NuclearWaterRadiationBuffProcedure;

public class NuclearWaterRadiationMobEffect extends MobEffect {
	public NuclearWaterRadiationMobEffect() {
		super(MobEffectCategory.HARMFUL, -16751002);
	}

	@Override
	public void applyEffectTick(LivingEntity entity, int amplifier) {
		NuclearWaterRadiationBuffProcedure.execute(entity);
	}

	@Override
	public boolean isDurationEffectTick(int duration, int amplifier) {
		return true;
	}
}
