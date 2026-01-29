
package net.everla.everlaartifacts.potion;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffect;

import net.everla.everlaartifacts.procedures.BedmicDestructionCheckIfPlayerLookAtBedProcedure;

public class BedmicDestructionMobEffect extends MobEffect {
	public BedmicDestructionMobEffect() {
		super(MobEffectCategory.HARMFUL, -16711783);
	}

	@Override
	public void applyEffectTick(LivingEntity entity, int amplifier) {
		BedmicDestructionCheckIfPlayerLookAtBedProcedure.execute(entity);
	}

	@Override
	public boolean isDurationEffectTick(int duration, int amplifier) {
		return true;
	}
}
