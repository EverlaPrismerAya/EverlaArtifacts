
package net.everla.everlaartifacts.potion;

import net.minecraft.world.entity.ai.attributes.AttributeMap;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffect;

import net.everla.everlaartifacts.procedures.GenshinStartSoundPlayProcedure;
import net.everla.everlaartifacts.procedures.GenshinStartMobEffectProcedure;

public class GenshinStartMobEffect extends MobEffect {
	public GenshinStartMobEffect() {
		super(MobEffectCategory.HARMFUL, -1);
	}

	@Override
	public void addAttributeModifiers(LivingEntity entity, AttributeMap attributeMap, int amplifier) {
		super.addAttributeModifiers(entity, attributeMap, amplifier);
		GenshinStartSoundPlayProcedure.execute(entity);
	}

	@Override
	public void applyEffectTick(LivingEntity entity, int amplifier) {
		GenshinStartMobEffectProcedure.execute(entity);
	}

	@Override
	public boolean isDurationEffectTick(int duration, int amplifier) {
		return true;
	}
}
