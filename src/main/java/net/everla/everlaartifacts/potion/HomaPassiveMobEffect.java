package net.everla.everlaartifacts.potion;

import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.item.ItemStack;

import java.util.List;

public class HomaPassiveMobEffect extends MobEffect {
	public HomaPassiveMobEffect() {
		super(MobEffectCategory.BENEFICIAL, -39322);
		this.addAttributeModifier(Attributes.MAX_HEALTH, "ee45a5cd-8870-338f-a678-1ae53fb24810", 0.4, AttributeModifier.Operation.MULTIPLY_BASE);
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