package net.everla.everlaartifacts.server.handlers.effects.genshin_start;

import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.Entity;

public class GenshinStartMobEffectHandler {
	public static void handleGenshinStartMobEffect(Entity entity) {
		if (entity == null || !(entity instanceof Mob mob))
			return;
		mob.getNavigation().stop();
		mob.setTarget(null);
		mob.setLastHurtByMob(null);
	}
}