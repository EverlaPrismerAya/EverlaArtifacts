package net.everla.everlaartifacts.procedures;

import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.Entity;

public class GenshinStartMobEffectProcedure {
	public static void execute(Entity entity) {
		if (entity == null || !(entity instanceof Mob mob))
			return;
		
		// 三行核心逻辑，实现「完全免疫反击」：
		// 1. 停止寻路（冻结移动目标）
		mob.getNavigation().stop();
		
		// 2. 清除攻击目标（丢失仇恨）
		mob.setTarget(null);
		
		// 3. 清除伤害来源（关键！防止受击后自动反击）
		//    原版AI逻辑：if (lastHurtByMob != null) → 反击
		//    清除此字段后，生物受击时无法找到"该打谁"，彻底免疫反击
		mob.setLastHurtByMob(null);
	}
}