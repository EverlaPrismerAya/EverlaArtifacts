
package net.everla.everlaartifacts.potion;

import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffect;

public class HomaPassiveMobEffect extends MobEffect {
	public HomaPassiveMobEffect() {
		super(MobEffectCategory.BENEFICIAL, -39322);
		this.addAttributeModifier(Attributes.MAX_HEALTH, "ee45a5cd-8870-338f-a678-1ae53fb24810", 0.4, AttributeModifier.Operation.MULTIPLY_BASE);
	}

	@Override
	public boolean isDurationEffectTick(int duration, int amplifier) {
		return true;
	}
}
