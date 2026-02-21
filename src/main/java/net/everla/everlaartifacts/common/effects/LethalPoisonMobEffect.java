package net.everla.everlaartifacts.common.effects;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.item.ItemStack;

import java.util.List;

public class LethalPoisonMobEffect extends MobEffect {
	public LethalPoisonMobEffect() {
		super(MobEffectCategory.HARMFUL, 8388736); // 暗紫色粒子效果 (0x800080)
	}

	@Override
	public void applyEffectTick(LivingEntity entity, int amplifier) {
		// 安全检查：跳过创造模式玩家
		if (entity instanceof net.minecraft.world.entity.player.Player player && (player.isCreative() || player.isSpectator())) {
			return;
		}
		
		// 每1刻(0.05秒)减少0.05点生命值
		if (!entity.level().isClientSide) {
			float currentHealth = entity.getHealth();
			
			// 如果生命值为1点或更低，使用致命伤害直接杀死
			if (currentHealth <= 1.0f) {
				entity.hurt(entity.damageSources().genericKill(), Float.MAX_VALUE);
			} else {
				// 否则正常减少生命值
				float newHealth = Math.max(0.0f, currentHealth - 0.05f);
				entity.setHealth(newHealth);
			}
		}
	}

	@Override
	public boolean isDurationEffectTick(int duration, int amplifier) {
		// 每1刻执行一次效果 (1 tick = 0.05 seconds)
		return duration % 1 == 0;
	}

	@Override
	public List<ItemStack> getCurativeItems() {
		return List.of(); // 返回空列表，防止被牛奶等物品治愈
	}
}