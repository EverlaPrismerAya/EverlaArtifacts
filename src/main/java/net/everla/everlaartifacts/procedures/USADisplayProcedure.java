package net.everla.everlaartifacts.procedures;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Entity;

import net.everla.everlaartifacts.init.EverlaartifactsModMobEffects;

public class USADisplayProcedure {
	public static boolean execute(Entity entity) {
		if (entity == null)
			return false;
		if ((entity instanceof LivingEntity _livEnt && _livEnt.hasEffect(EverlaartifactsModMobEffects.AMERICAN_STYLE_CUT_OVERLAY.get()) ? _livEnt.getEffect(EverlaartifactsModMobEffects.AMERICAN_STYLE_CUT_OVERLAY.get()).getDuration() : 0) > 0) {
			return true;
		}
		return false;
	}
}
