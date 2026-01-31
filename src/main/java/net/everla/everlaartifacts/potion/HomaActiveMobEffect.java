package net.everla.everlaartifacts.potion;

import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.item.ItemStack;

import java.util.List;

public class HomaActiveMobEffect extends MobEffect {
	public HomaActiveMobEffect() {
		super(MobEffectCategory.BENEFICIAL, -3407872);
	}

	@Override
	public boolean isDurationEffectTick(int duration, int amplifier) {
		return true;
	}
	
	@Override
	public boolean isBeneficial() {
		return false; // 设置为非有益效果，这样就不会被牛奶移除
	}
	
	@Override
	public List<ItemStack> getCurativeItems() {
		return List.of(); // 返回空列表，防止被牛奶等物品治愈
	}
}