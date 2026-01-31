package net.everla.everlaartifacts.potion;

import net.minecraft.world.entity.ai.attributes.AttributeMap;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.item.ItemStack;

import java.util.List;

import net.everla.everlaartifacts.procedures.GenshinStartSoundPlayProcedure;
import net.everla.everlaartifacts.procedures.GenshinStartMobEffectProcedure;

public class GenshinStartMobEffect extends MobEffect {
	public GenshinStartMobEffect() {
		super(MobEffectCategory.HARMFUL, -1);
	}

	@Override
	public void addAttributeModifiers(LivingEntity entity, AttributeMap attributeMap, int amplifier) {
		super.addAttributeModifiers(entity, attributeMap, amplifier);
		GenshinStartSoundPlayProcedure.execute(entity);
	}

	@Override
	public void applyEffectTick(LivingEntity entity, int amplifier) {
		GenshinStartMobEffectProcedure.execute(entity);
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