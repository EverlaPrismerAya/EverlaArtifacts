package net.everla.everlaartifacts.potion;

import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.item.ItemStack;

import java.util.List;

public class BlitzkriegMobEffect extends MobEffect {
	public BlitzkriegMobEffect() {
		super(MobEffectCategory.BENEFICIAL, -6750208);
		this.addAttributeModifier(Attributes.ARMOR, "a3ddd41f-3db4-3261-91ba-9f7bd9b037a2", 0.3, AttributeModifier.Operation.MULTIPLY_TOTAL);
		this.addAttributeModifier(Attributes.ATTACK_DAMAGE, "5af2da4d-1b66-3c7c-be33-e4cadfdb667c", 0.3, AttributeModifier.Operation.MULTIPLY_TOTAL);
		this.addAttributeModifier(Attributes.ATTACK_SPEED, "8fefc297-4486-3a39-aa12-f88a456f68f8", 0.3, AttributeModifier.Operation.MULTIPLY_TOTAL);
		this.addAttributeModifier(Attributes.MOVEMENT_SPEED, "7c84d078-7373-3006-b492-567ff528bcb4", 0.3, AttributeModifier.Operation.MULTIPLY_BASE);
		this.addAttributeModifier(Attributes.KNOCKBACK_RESISTANCE, "4112d660-25d9-35e4-8af2-922d75e8fd4c", 3, AttributeModifier.Operation.MULTIPLY_TOTAL);
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