
package net.everla.everlaartifacts.potion;

import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffect;

public class VenusShellActiveMobEffect extends MobEffect {
	public VenusShellActiveMobEffect() {
		super(MobEffectCategory.BENEFICIAL, -13408768);
	}

	@Override
	public boolean isDurationEffectTick(int duration, int amplifier) {
		return true;
	}
}
