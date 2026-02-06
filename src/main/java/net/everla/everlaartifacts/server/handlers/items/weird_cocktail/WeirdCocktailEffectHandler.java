package net.everla.everlaartifacts.server.handlers.items.weird_cocktail;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.effect.MobEffectInstance;

import net.everla.everlaartifacts.init.EverlaartifactsModMobEffects;

/**
 * 奇异鸡尾酒效果处理器
 * 处理奇异鸡尾酒被饮用时应用的各种药水效果
 */
public class WeirdCocktailEffectHandler {
    
    /**
     * 应用奇异鸡尾酒的复合效果
     * @param entity 受影响的实体
     */
    public static void applyWeirdCocktailEffects(Entity entity) {
        if (entity == null || !(entity instanceof LivingEntity livingEntity) || entity.level().isClientSide()) {
            return;
        }
        
        // 应用所有药水效果
        livingEntity.addEffect(new MobEffectInstance(EverlaartifactsModMobEffects.BEDMIC_DESTRUCTION.get(), 6000, 0, false, true));
        livingEntity.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 3600, 9, false, true));
        livingEntity.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 3600, 1, false, true));
        livingEntity.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 1200, 2, false, true));
        livingEntity.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 1200, 14, false, true));
    }
}