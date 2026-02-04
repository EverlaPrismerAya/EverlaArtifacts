
package net.everla.everlaartifacts.potion;

import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffect;

public class VenusShellPassiveMobEffect extends MobEffect {
	public VenusShellPassiveMobEffect() {
		super(MobEffectCategory.BENEFICIAL, -103);
	}

	@Override
	public boolean isDurationEffectTick(int duration, int amplifier) {
		return true;
	}
}
