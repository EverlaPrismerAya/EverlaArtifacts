package net.everla.everlaartifacts.client.handlers.effects.waaooo;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.event.entity.living.MobEffectEvent;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.sounds.SoundSource;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.client.Minecraft;

import net.everla.everlaartifacts.init.EverlaartifactsModMobEffects;

@Mod.EventBusSubscriber(value = Dist.CLIENT)
public class WaaoooEffectHandler {
    
    /**
     * 检查实体是否具有WAAOOO视觉效果
     * @param entity 目标实体
     * @return 如果实体具有WAAOOO效果且持续时间大于0则返回true
     */
    public static boolean hasWaaoooDisplay(Entity entity) {
        if (entity == null)
            return false;
        if ((entity instanceof LivingEntity livingEntity && livingEntity.hasEffect(EverlaartifactsModMobEffects.WAAOOO_OVERLAY.get()) 
             ? livingEntity.getEffect(EverlaartifactsModMobEffects.WAAOOO_OVERLAY.get()).getDuration() : 0) > 0) {
            return true;
        }
        return false;
    }
    
    /**
     * 在状态效果添加时播放WAAOOO音效
     * @param event 状态效果添加事件
     */
    @SubscribeEvent
    @OnlyIn(Dist.CLIENT)
    public static void onMobEffectAdded(MobEffectEvent.Added event) {
        if (event.getEffectInstance().getEffect() == EverlaartifactsModMobEffects.WAAOOO_OVERLAY.get()) {
            Entity entity = event.getEntity();
            if (entity != null) {
                playWaaoooSound(entity);
            }
        }
    }
    
    /**
     * 为实体播放WAAOOO音效（纯客户端实现）
     * @param entity 目标实体
     */
    @OnlyIn(Dist.CLIENT)
    private static void playWaaoooSound(Entity entity) {
        if (entity == null) return;
        
        // 在客户端播放音效
        Minecraft mc = Minecraft.getInstance();
        if (mc.level != null && mc.player != null) {
            SoundEvent soundEvent = SoundEvent.createVariableRangeEvent(
                ResourceLocation.fromNamespaceAndPath("everlaartifacts", "waaooo")
            );
            mc.level.playSound(
                mc.player, 
                entity.getX(), 
                entity.getY(), 
                entity.getZ(), 
                soundEvent, 
                SoundSource.PLAYERS, 
                1.0F, 
                1.0F
            );
        }
    }
    
    // 移除了handleWaaoooEffect方法，因为现在通过事件自动处理
}