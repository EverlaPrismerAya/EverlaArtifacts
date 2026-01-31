package net.everla.everlaartifacts;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import net.everla.everlaartifacts.init.EverlaartifactsModEnchantments;
import net.everla.everlaartifacts.init.EverlaartifactsModMobEffects;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.WeakHashMap;

@Mod.EventBusSubscriber(modid = "everlaartifacts", bus = Mod.EventBusSubscriber.Bus.FORGE)
public class DeutschHandler {
    
    // 追踪玩家Blitzkrieg效果的结束时间
    private static final java.util.Map<java.util.UUID, Long> blitzkriegEndTimeMap = new ConcurrentHashMap<>();
    // 追踪玩家是否正在经历负面效果期
    private static final java.util.Map<java.util.UUID, Boolean> isInNegativeEffectPeriod = new ConcurrentHashMap<>();
    
    // 用于控制tick事件频率的计数器
    private static final java.util.Map<java.util.UUID, Integer> playerTickCounter = new ConcurrentHashMap<>();
    
    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        DamageSource source = event.getSource();
        var trueSource = source.getEntity();
        
        // 检查伤害来源是否为玩家
        if (!(trueSource instanceof Player player)) {
            return;
        }
        
        var target = event.getEntity();
        
        // 检查玩家是否佩戴了等级为3的Deutsch附魔的头盔
        int deutschLevel = EnchantmentHelper.getEnchantmentLevel(
            EverlaartifactsModEnchantments.DEUTSCH.get(), player);
        
        UUID playerId = player.getUUID();
        
        if (deutschLevel == 3 && target != player) { // 确保是等级3且不是自残
            // 检查玩家是否正处于负面效果期间
            boolean negativeEffectActive = isInNegativeEffectPeriod.getOrDefault(playerId, false);
            
            // 重要修复：即使在负面效果期间，也不应该给予Blitzkrieg效果
            // 同时也要检查玩家是否已经有Blitzkrieg效果
            boolean hasBlitzkriegEffect = player.hasEffect(EverlaartifactsModMobEffects.BLITZKRIEG.get());
            
            // 只有在没有负面效果且没有Blitzkrieg效果时才给予Blitzkrieg效果
            if (!negativeEffectActive && !hasBlitzkriegEffect) {
                // 给玩家添加Blitzkrieg状态效果（30秒，等级1）
                MobEffectInstance blitzkriegEffect = new MobEffectInstance(
                    EverlaartifactsModMobEffects.BLITZKRIEG.get(), 
                    600, // 30秒 * 20 ticks/秒 = 600 ticks
                    0); // 等级1 (内部表示为0)
                
                player.addEffect(blitzkriegEffect);
                
                // 记录Blitzkrieg效果的预计结束时间（当前时间 + 600 ticks）
                long currentTime = player.level().getGameTime();
                blitzkriegEndTimeMap.put(playerId, currentTime + 600L);
            }
        }
    }
    
    // 监控玩家状态，当Blitzkrieg效果结束时添加负面效果
    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        
        Player player = event.player;
        UUID playerId = player.getUUID();
        
        // 每5个tick执行一次，减少性能开销
        int currentTick = playerTickCounter.getOrDefault(playerId, 0) + 1;
        playerTickCounter.put(playerId, currentTick);
        
        if (currentTick % 5 != 0) { // 每5个tick执行一次
            return;
        }
        
        long currentTime = player.level().getGameTime();
        
        // 检查玩家是否正处于负面效果期间
        boolean negativeEffectActive = isInNegativeEffectPeriod.getOrDefault(playerId, false);
        
        if (negativeEffectActive) {
            // 如果在负面效果期间，检查负面效果是否已结束
            boolean stillHasNegativeEffects = player.hasEffect(MobEffects.MOVEMENT_SLOWDOWN) || 
                                              player.hasEffect(MobEffects.HUNGER) || 
                                              player.hasEffect(MobEffects.WEAKNESS);
            
            if (!stillHasNegativeEffects) {
                // 负面效果已结束，清除标记
                isInNegativeEffectPeriod.remove(playerId);
            }
        } else {
            // 检查Blitzkrieg效果是否应该结束
            Long expectedEndTime = blitzkriegEndTimeMap.get(playerId);
            
            if (expectedEndTime != null && currentTime >= expectedEndTime) {
                // 检查玩家是否仍然有Blitzkrieg效果
                boolean stillHasBlitzkrieg = player.hasEffect(EverlaartifactsModMobEffects.BLITZKRIEG.get());
                
                if (!stillHasBlitzkrieg) {
                    // Blitzkrieg效果已结束，添加负面效果
                    // 添加负面效果
                    player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 1200, 2)); // 缓慢3 (等级2+1)
                    player.addEffect(new MobEffectInstance(MobEffects.HUNGER, 800, 1)); // 饥饿2 (等级1+1)  
                    player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 1200, 2)); // 虚弱3 (等级2+1)
                    
                    // 标记玩家进入负面效果期间，防止在此期间再次获得Blitzkrieg
                    isInNegativeEffectPeriod.put(playerId, true);
                    
                    // 移除Blitzkrieg结束时间记录
                    blitzkriegEndTimeMap.remove(playerId);
                }
            } else if (expectedEndTime != null && currentTime < expectedEndTime) {
                // 检查玩家是否意外失去了Blitzkrieg效果（例如被牛奶清除等）
                boolean stillHasBlitzkrieg = player.hasEffect(EverlaartifactsModMobEffects.BLITZKRIEG.get());
                
                if (!stillHasBlitzkrieg) {
                    // 玩家提前失去了Blitzkrieg效果，立即添加负面效果
                    player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 1200, 2)); // 缓慢3
                    player.addEffect(new MobEffectInstance(MobEffects.HUNGER, 800, 1)); // 饥饿2
                    player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 1200, 2)); // 虚弱3
                    
                    // 标记玩家进入负面效果期间
                    isInNegativeEffectPeriod.put(playerId, true);
                    
                    // 移除Blitzkrieg结束时间记录
                    blitzkriegEndTimeMap.remove(playerId);
                }
            }
        }
    }
    
    // 处理玩家重生事件，重置状态
    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        Player player = event.getEntity();
        UUID playerId = player.getUUID();
        
        // 重置玩家的状态标记，避免死亡时的异常状态延续到重生后
        isInNegativeEffectPeriod.remove(playerId);
        blitzkriegEndTimeMap.remove(playerId);
    }
    
    // 处理玩家加入世界事件，重置状态以避免异常
    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        Player player = event.getEntity();
        UUID playerId = player.getUUID();
        
        // 清理可能存在的异常状态标记
        isInNegativeEffectPeriod.remove(playerId);
        blitzkriegEndTimeMap.remove(playerId);
    }
}