package net.everla.everlaartifacts.server.handlers.items.pot_of_pain;

import net.minecraftforge.fml.common.Mod;

import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.server.MinecraftServer;

import net.everla.everlaartifacts.server.handlers.difficulty.WorldSeedChecker;
import net.everla.everlaartifacts.common.difficulty.DifficultyLevel;

import net.everla.everlaartifacts.init.EverlaartifactsModMobEffects;
import net.everla.everlaartifacts.EverlaartifactsMod;

import java.util.List;

@Mod.EventBusSubscriber
public class PotOfPainEffectHandler {
    
    // 常量定义
    private static final int LONG_DURATION = 39600; // 22分钟
    private static final int SHORT_DURATION = 20;   // 1秒
    private static final int MAX_AMPLIFIER = 255;
    private static final int HARM_AMPLIFIER = 1;
    private static final int REGEN_AMPLIFIER = 1;
    private static final int OVERLAY_AMPLIFIER = 255;
    private static final int DELAY_TICKS = 60; // 3秒延迟
    private static final int HEALTH_BOOST_DURATION = 24000; // 20分钟 (20 * 60 * 20)
    private static final int HEALTH_BOOST_AMPLIFIER = 76;   // 等级77
    
    // 痛苦效果列表
    private static final List<MobEffectData> PAIN_EFFECTS = List.of(
        new MobEffectData(MobEffects.BLINDNESS, LONG_DURATION, MAX_AMPLIFIER),
        new MobEffectData(MobEffects.DARKNESS, LONG_DURATION, MAX_AMPLIFIER),
        new MobEffectData(MobEffects.HUNGER, LONG_DURATION, MAX_AMPLIFIER),
        new MobEffectData(MobEffects.DIG_SLOWDOWN, LONG_DURATION, MAX_AMPLIFIER),
        new MobEffectData(MobEffects.CONFUSION, LONG_DURATION, MAX_AMPLIFIER),
        new MobEffectData(MobEffects.POISON, LONG_DURATION, MAX_AMPLIFIER),
        new MobEffectData(MobEffects.MOVEMENT_SLOWDOWN, LONG_DURATION, MAX_AMPLIFIER),
        new MobEffectData(MobEffects.UNLUCK, LONG_DURATION, MAX_AMPLIFIER),
        new MobEffectData(MobEffects.WEAKNESS, LONG_DURATION, MAX_AMPLIFIER),
        new MobEffectData(MobEffects.WITHER, LONG_DURATION, MAX_AMPLIFIER),
        new MobEffectData(MobEffects.HARM, LONG_DURATION, HARM_AMPLIFIER)
    );
    
    /**
     * 处理痛苦之锅的效果应用
     * @param world 世界对象
     * @param entity 目标实体
     */
    public static void handlePotOfPainEffect(LevelAccessor world, Entity entity) {
        // 参数验证
        if (entity == null || world == null) {
            return;
        }
        
        // 检查是否为存活实体且在服务端
        if (!(entity instanceof LivingEntity livingEntity) || livingEntity.level().isClientSide()) {
            return;
        }
        
        // 检查是否为特殊种子世界且难度为EXTRA
        boolean isExtraDifficulty = false;
        if (entity.level() != null && !entity.level().isClientSide()) {
            MinecraftServer server = entity.level().getServer();
            if (server != null) {
                isExtraDifficulty = WorldSeedChecker.isSpecialSeedWorld() && 
                    WorldSeedChecker.getCurrentWorldDifficulty(server) == DifficultyLevel.EXTRA;
            }
        }
        
        // 立即应用再生效果
        applyRegenerationEffect(livingEntity);
        
        // 根据难度决定应用哪种效果
        if (isExtraDifficulty) {
            // Extra难度下应用Health Boost效果
            scheduleHealthBoostEffect(livingEntity);
        } else {
            // 普通难度下应用痛苦效果
            schedulePainEffects(livingEntity);
        }
    }
    
    /**
     * 应用再生效果
     */
    private static void applyRegenerationEffect(LivingEntity entity) {
        entity.addEffect(new MobEffectInstance(
            MobEffects.REGENERATION, 
            LONG_DURATION, 
            REGEN_AMPLIFIER, 
            false, 
            true
        ));
    }
    
    /**
     * 调度痛苦效果的应用
     */
    private static void schedulePainEffects(LivingEntity entity) {
        EverlaartifactsMod.queueServerWork(DELAY_TICKS, () -> {
            // 先应用视觉覆盖效果
            applyVisualOverlay(entity);
            
            // 批量应用所有痛苦效果
            applyPainEffects(entity);
        });
    }
    
    /**
     * 调度Health Boost效果的应用（仅在Extra难度下）
     */
    private static void scheduleHealthBoostEffect(LivingEntity entity) {
        EverlaartifactsMod.queueServerWork(DELAY_TICKS, () -> {
            // 应用Health Boost效果
            applyHealthBoostEffect(entity);
        });
    }
    
    /**
     * 应用视觉覆盖效果
     */
    private static void applyVisualOverlay(LivingEntity entity) {
        entity.addEffect(new MobEffectInstance(
            EverlaartifactsModMobEffects.WAAOOO_OVERLAY.get(),
            SHORT_DURATION,
            OVERLAY_AMPLIFIER,
            false,
            true
        ));
    }
    
    /**
     * 批量应用所有痛苦效果
     */
    private static void applyPainEffects(LivingEntity entity) {
        for (MobEffectData effectData : PAIN_EFFECTS) {
            entity.addEffect(new MobEffectInstance(
                effectData.effect(),
                effectData.duration(),
                effectData.amplifier(),
                false,
                true
            ));
        }
    }
    
    /**
     * 应用Health Boost效果（仅在Extra难度下）
     */
    private static void applyHealthBoostEffect(LivingEntity entity) {
        entity.addEffect(new MobEffectInstance(
            MobEffects.HEALTH_BOOST,
            HEALTH_BOOST_DURATION,
            HEALTH_BOOST_AMPLIFIER,
            false,
            true
        ));
    }
    
    /**
     * 效果数据记录类
     */
    private record MobEffectData(
        net.minecraft.world.effect.MobEffect effect,
        int duration,
        int amplifier
    ) {}
}