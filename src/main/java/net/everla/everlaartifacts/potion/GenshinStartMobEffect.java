package net.everla.everlaartifacts.potion;

import net.minecraft.world.entity.ai.attributes.AttributeMap;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.item.ItemStack;

import java.util.List;

import net.everla.everlaartifacts.client.handlers.effects.genshin_start.GenshinStartSoundPlayHandler;
import net.everla.everlaartifacts.server.handlers.effects.GenshinStartMobEffectHandler;

public class GenshinStartMobEffect extends MobEffect {
	public GenshinStartMobEffect() {
		super(MobEffectCategory.HARMFUL, -1);
	}

	@Override
	public void addAttributeModifiers(LivingEntity entity, AttributeMap attributeMap, int amplifier) {
		super.addAttributeModifiers(entity, attributeMap, amplifier);
		GenshinStartSoundPlayHandler.handleGenshinStartSoundPlay(entity);
	}

	@Override
	public void applyEffectTick(LivingEntity entity, int amplifier) {
		GenshinStartMobEffectHandler.handleGenshinStartMobEffect(entity);
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