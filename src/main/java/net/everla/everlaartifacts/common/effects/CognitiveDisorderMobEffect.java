package net.everla.everlaartifacts.common.effects;

import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.item.ItemStack;

import java.util.List;

public class CognitiveDisorderMobEffect extends MobEffect {
	public CognitiveDisorderMobEffect() {
		super(MobEffectCategory.HARMFUL, 9498512); // 浅绿色粒子效果 (0x90EE90)
	}

	@Override
	public boolean isDurationEffectTick(int duration, int amplifier) {
		return true;
	}

	@Override
	public List<ItemStack> getCurativeItems() {
		return List.of(); // 返回空列表，防止被牛奶等物品治愈
	}
}