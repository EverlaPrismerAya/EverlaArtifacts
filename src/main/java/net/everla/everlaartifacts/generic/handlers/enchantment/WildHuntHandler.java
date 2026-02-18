package net.everla.everlaartifacts.generic.handlers.enchantment;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraftforge.event.entity.living.LivingHealEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.fml.common.Mod;

import net.everla.everlaartifacts.init.EverlaartifactsModEnchantments;

import java.util.UUID;
import java.util.Map;
import java.util.WeakHashMap;

@Mod.EventBusSubscriber(modid = "everlaartifacts", bus = Mod.EventBusSubscriber.Bus.FORGE)
public class WildHuntHandler {
    
    // NBT标签键
    private static final String WILD_HUNT_ACTIVE_KEY = "WildHuntActive";
    private static final String WILD_HUNT_START_TIME_KEY = "WildHuntStartTime";
    private static final String WILD_HUNT_MAX_HEALTH_REDUCTION_KEY = "WildHuntMaxHealthReduction";
    private static final String HYPER_LETHAL_DAMAGE_KEY = "HyperLethalDamage";
    
    // 常量
    private static final int WILD_HUNT_DURATION = 200; // 10秒 = 200 ticks
    private static final float IMMUNITY_THRESHOLD = 1.0f; // 免疫致命伤害的阈值
    private static final double DAMAGE_REDUCTION_PERCENTAGE = 0.45; // 45%免伤
    private static final double ADDITIONAL_REDUCTION_PERCENTAGE = 0.08; // 额外8%最大生命值减少
    private static final double MOVEMENT_SPEED_REDUCTION = 0.5; // 50%移动速度减少
    private static final double ATTACK_DAMAGE_REDUCTION = 0.5; // 50%攻击力减少
    
    // 属性修饰符UUID
    private static final java.util.UUID MOVEMENT_SPEED_UUID = java.util.UUID.fromString("763ded5c-2988-4f5c-ac3e-abb3f47266b0");
    private static final java.util.UUID ATTACK_DAMAGE_UUID = java.util.UUID.fromString("abc9f1ad-1bd3-418e-8744-4c5e4e4df215");
    private static final java.util.UUID MAX_HEALTH_REDUCTION_UUID = java.util.UUID.fromString("9ff0ee1b-b1a5-4bcf-a32c-689ac4adc584");
    
    // 实体状态追踪（内存缓存，用于性能优化）
    private static final Map<UUID, Boolean> entityInWildHuntMap = new WeakHashMap<>();
    private static final Map<UUID, Double> entityMaxHealthReductionMap = new WeakHashMap<>();
    private static final Map<UUID, Boolean> entityHyperLethalDamageMap = new WeakHashMap<>();
    
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onLivingHurt(LivingHurtEvent event) {
        if (event.getEntity().level().isClientSide()) {
            return;
        }
        
        LivingEntity entity = event.getEntity();
        ItemStack chestArmor = entity.getItemBySlot(EquipmentSlot.CHEST);
        int wildHuntLevel = EnchantmentHelper.getItemEnchantmentLevel(
            EverlaartifactsModEnchantments.WILD_HUNT.get(), chestArmor);
        
        if (wildHuntLevel <= 0) {
            return;
        }
        
        float maxHealth = entity.getMaxHealth();
        // 安全检查：如果实体最大生命值小于等于1，则附魔完全不生效
        if (maxHealth <= IMMUNITY_THRESHOLD) {
            return;
        }
        
        ServerLevel serverLevel = (ServerLevel) entity.level();
        UUID entityUUID = entity.getUUID();
        float originalDamage = event.getAmount();
        float currentHealth = entity.getHealth();
        
        // 检查是否已经处于狂猎状态
        boolean isInWildHunt = isEntityInWildHunt(entity);
        boolean isHyperLethalDamage = originalDamage >= 2147483647.0f;
        boolean isFatalDamage = (currentHealth - originalDamage) <= 0;
        boolean isLowHealth = currentHealth <= IMMUNITY_THRESHOLD;
        // 如果不是超高伤害
        if (!isHyperLethalDamage) {
            // 如果不在狂猎状态
            if (!isInWildHunt) {
                // 只在实体受到致死伤害或生命值<=1 并且不是Kill命令之类的超高伤害时激活狂猎
                if (isFatalDamage || isLowHealth) {
                    // 检查是否在无敌帧期间
                    if (entity.invulnerableTime > 0) {
                        // 如果在无敌帧期间，先取消当前伤害事件
                        event.setCanceled(true);
                        // 然后立即激活狂猎状态
                        activateWildHunt(entity, serverLevel);
                        return;
                    } else {
                        // 不在无敌帧期间，正常激活狂猎
                        activateWildHunt(entity, serverLevel);
                        event.setCanceled(true);
                        return;
                    }
                }
                // 如果是非致命伤害且生命值>1，让伤害正常处理
                else {
                    return; // 不做任何处理，让原版伤害系统处理
                }
            }
            // 如果在狂猎状态中
            else {
                long currentTime = serverLevel.getGameTime();
                long startTime = getEntityStartTime(entity);
                boolean isDurationExpired = (currentTime - startTime) >= WILD_HUNT_DURATION;

                // 应用45%免伤
                float reducedDamage = originalDamage * (float)(1.0 - DAMAGE_REDUCTION_PERCENTAGE);
                event.setAmount(reducedDamage);

                // 锁定生命值为1点
                entity.setHealth(IMMUNITY_THRESHOLD);

                // 减少最大生命值
                reduceEntityMaxHealth(entity, reducedDamage);

                // 如果持续时间结束且最大生命值大于1，则额外减少8%
                if (isDurationExpired && entity.getMaxHealth() > IMMUNITY_THRESHOLD) {
                    applyAdditionalHealthReduction(entity);
                }
            }
        } else {
            // 如果是超高伤害，则添加无视保护标签
            setHyperLethalDamageKey(entity, true);
        }
    }
    
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onLivingDeath(LivingDeathEvent event) {
        if (event.getEntity().level().isClientSide()) {
            return;
        }
        
        LivingEntity entity = event.getEntity();
        
        // 检查是否因最大生命值降到1以下或受到超高伤害而死亡
        if (entity.getMaxHealth() <= IMMUNITY_THRESHOLD || isEntityKilledByHyperLethal(entity)) {
            // 允许正常死亡，并清除所有狂猎数据
            cleanupEntityWildHuntData(entity);
            return;
        }
        
        // 检查是否处于狂猎状态且未受到超高伤害
        if (!isEntityKilledByHyperLethal(entity)) {
            if (isEntityInWildHunt(entity)) {
                // 取消死亡，因为狂猎状态下免疫致命伤害
                event.setCanceled(true);
                entity.setHealth(IMMUNITY_THRESHOLD);
                return;
            }

            // 基于死亡判断的绕过原版无敌帧机制
            ItemStack chestArmor = entity.getItemBySlot(EquipmentSlot.CHEST);
            int wildHuntLevel = EnchantmentHelper.getItemEnchantmentLevel(
                    EverlaartifactsModEnchantments.WILD_HUNT.get(), chestArmor);

            if (wildHuntLevel > 0 && entity.getMaxHealth() > IMMUNITY_THRESHOLD) {
                // 激活狂猎状态并取消死亡
                ServerLevel serverLevel = (ServerLevel) entity.level();
                activateWildHunt(entity, serverLevel);
                event.setCanceled(true);
                entity.setHealth(IMMUNITY_THRESHOLD);
            }
        }
    }
    
    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        Player player = event.getEntity();
        // 玩家登录时检查并恢复狂猎状态
        restoreEntityWildHuntState(player);
    }
    
    @SubscribeEvent
    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        Player player = event.getEntity();
        // 玩家退出时保留狂猎状态数据，不清除
        // 只移除临时属性修饰符，保留NBT数据用于下次登录时恢复
        removeTemporaryAttributeModifiers(player);
    }
    
    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        Player player = event.getEntity();
        // 玩家重生时完全清空所有狂猎状态数据
        cleanupAllEntityWildHuntData(player);
    }

    // 检查实体是否处于狂猎状态 是则完全阻止治疗
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onLivingHeal(LivingHealEvent event) {
        if (event.getEntity().level().isClientSide()) {
            return;
        }
        LivingEntity entity = event.getEntity();
        if (isEntityInWildHunt(entity)) {
            event.setCanceled(true);
        }
    }

    private static void activateWildHunt(LivingEntity entity, ServerLevel serverLevel) {
        UUID entityUUID = entity.getUUID();
        
        // 标记实体进入狂猎状态
        setEntityInWildHunt(entity, true);
        setEntityStartTime(entity, serverLevel.getGameTime());
        
        // 获取实体当前的最大生命值减少数据（保留之前的损伤）
        double existingReduction = getEntityMaxHealthReduction(entity);
        setEntityMaxHealthReduction(entity, existingReduction);
        // 确保内存缓存同步
        entityMaxHealthReductionMap.put(entityUUID, existingReduction);
        
        // 锁定生命值为1
        entity.setHealth(IMMUNITY_THRESHOLD);
    }
    
    private static void reduceEntityMaxHealth(LivingEntity entity, float damage) {
        UUID entityUUID = entity.getUUID();
        double currentReduction = entityMaxHealthReductionMap.getOrDefault(entityUUID, 0.0);
        double newReduction = currentReduction + damage;
        
        // 只有当减少量发生变化时才更新
        if (Math.abs(newReduction - currentReduction) > 0.001) {
            entityMaxHealthReductionMap.put(entityUUID, newReduction);
            
            // 同步更新NBT数据
            setEntityMaxHealthReduction(entity, newReduction);
            
            // 更新生命值控制属性
            updateMaxHealthModifier(entity, newReduction);
        }
    }
    
    private static void applyAdditionalHealthReduction(LivingEntity entity) {
        UUID entityUUID = entity.getUUID();
        double currentTotalReduction = entityMaxHealthReductionMap.getOrDefault(entityUUID, 0.0);
        
        // 计算额外减少量（基于实体当前总最大生命值）
        double currentEntityMaxHealth = entity.getMaxHealth();
        if (currentEntityMaxHealth > IMMUNITY_THRESHOLD) {
            double additionalReduction = currentEntityMaxHealth * ADDITIONAL_REDUCTION_PERCENTAGE;
            double newTotalReduction = currentTotalReduction + additionalReduction;
            
            // 只有当减少量发生变化时才更新
            if (Math.abs(newTotalReduction - currentTotalReduction) > 0.001) {
                entityMaxHealthReductionMap.put(entityUUID, newTotalReduction);
                
                // 同步更新NBT数据
                setEntityMaxHealthReduction(entity, newTotalReduction);
                
                // 更新生命值控制属性
                updateMaxHealthModifier(entity, newTotalReduction);
            }
        }
    }
    
    private static void spawnWildHuntParticles(LivingEntity entity, ServerLevel serverLevel) {
        // 在狂猎持续期间生成黑烟粒子
        serverLevel.sendParticles(
            ParticleTypes.SMOKE,
            entity.getX(),
            entity.getY() + entity.getBbHeight() / 2.0,
            entity.getZ(),
            3,
            entity.getBbWidth() / 4.0,
            entity.getBbHeight() / 4.0,
            entity.getBbWidth() / 4.0,
            0.02
        );
    }
    
    private static boolean isEntityInWildHunt(LivingEntity entity) {
        CompoundTag persistentData = entity.getPersistentData();
        return persistentData.getBoolean(WILD_HUNT_ACTIVE_KEY);
    }

    private static boolean isEntityKilledByHyperLethal(LivingEntity entity){
        CompoundTag persistentData = entity.getPersistentData();
        return persistentData.getBoolean(HYPER_LETHAL_DAMAGE_KEY);
    }
    
    private static void setEntityInWildHunt(LivingEntity entity, boolean active) {
        CompoundTag persistentData = entity.getPersistentData();
        persistentData.putBoolean(WILD_HUNT_ACTIVE_KEY, active);
        entityInWildHuntMap.put(entity.getUUID(), active);
    }

    private static void setHyperLethalDamageKey(LivingEntity entity, boolean active) {
        CompoundTag persistentData = entity.getPersistentData();
        persistentData.putBoolean(HYPER_LETHAL_DAMAGE_KEY, active);
        entityHyperLethalDamageMap.put(entity.getUUID(), active);
    }
    
    private static long getEntityStartTime(LivingEntity entity) {
        CompoundTag persistentData = entity.getPersistentData();
        return persistentData.getLong(WILD_HUNT_START_TIME_KEY);
    }
    
    private static void setEntityStartTime(LivingEntity entity, long time) {
        CompoundTag persistentData = entity.getPersistentData();
        persistentData.putLong(WILD_HUNT_START_TIME_KEY, time);
    }
    
    private static double getEntityMaxHealthReduction(LivingEntity entity) {
        CompoundTag persistentData = entity.getPersistentData();
        return persistentData.getDouble(WILD_HUNT_MAX_HEALTH_REDUCTION_KEY);
    }
    
    private static void setEntityMaxHealthReduction(LivingEntity entity, double reduction) {
        CompoundTag persistentData = entity.getPersistentData();
        persistentData.putDouble(WILD_HUNT_MAX_HEALTH_REDUCTION_KEY, reduction);
        entityMaxHealthReductionMap.put(entity.getUUID(), reduction);
    }
    
    
    private static void applyWildHuntAttributeModifiers(LivingEntity entity) {
        // 减少移动速度50%
        AttributeInstance movementSpeed = entity.getAttribute(Attributes.MOVEMENT_SPEED);
        if (movementSpeed != null) {
            // 先移除旧的修饰符
            movementSpeed.removeModifier(MOVEMENT_SPEED_UUID);
            // 添加新的减速修饰符
            AttributeModifier speedModifier = new AttributeModifier(
                MOVEMENT_SPEED_UUID, 
                "WildHuntMovementSlow", 
                -MOVEMENT_SPEED_REDUCTION, 
                AttributeModifier.Operation.MULTIPLY_TOTAL
            );
            movementSpeed.addTransientModifier(speedModifier);
        }
        
        // 减少攻击力50%（仅对可以攻击的实体）
        if (entity instanceof Player || entity instanceof Monster || entity instanceof PathfinderMob) {
            AttributeInstance attackDamage = entity.getAttribute(Attributes.ATTACK_DAMAGE);
            if (attackDamage != null) {
                // 先移除旧的修饰符
                attackDamage.removeModifier(ATTACK_DAMAGE_UUID);
                // 添加新的减攻修饰符
                AttributeModifier damageModifier = new AttributeModifier(
                    ATTACK_DAMAGE_UUID, 
                    "WildHuntAttackReduction", 
                    -ATTACK_DAMAGE_REDUCTION, 
                    AttributeModifier.Operation.MULTIPLY_TOTAL
                );
                attackDamage.addTransientModifier(damageModifier);
            }
        }
    }
    
    private static void removeWildHuntAttributeModifiers(LivingEntity entity) {
        // 移除移动速度修饰符
        AttributeInstance movementSpeed = entity.getAttribute(Attributes.MOVEMENT_SPEED);
        if (movementSpeed != null) {
            movementSpeed.removeModifier(MOVEMENT_SPEED_UUID);
        }
        
        // 移除攻击力修饰符（仅对可以攻击的实体）
        if (entity instanceof Player || entity instanceof Monster || entity instanceof PathfinderMob) {
            AttributeInstance attackDamage = entity.getAttribute(Attributes.ATTACK_DAMAGE);
            if (attackDamage != null) {
                attackDamage.removeModifier(ATTACK_DAMAGE_UUID);
            }
        }
        
        // 移除最大生命值减少修饰符
        AttributeInstance maxHealth = entity.getAttribute(Attributes.MAX_HEALTH);
        if (maxHealth != null) {
            maxHealth.removeModifier(MAX_HEALTH_REDUCTION_UUID);
        }
    }
    
    private static void removeTemporaryAttributeModifiers(LivingEntity entity) {
        // 只移除移动速度和攻击力减益修饰符
        AttributeInstance movementSpeed = entity.getAttribute(Attributes.MOVEMENT_SPEED);
        if (movementSpeed != null) {
            movementSpeed.removeModifier(MOVEMENT_SPEED_UUID);
        }
        
        // 移除攻击力修饰符（仅对可以攻击的实体）
        if (entity instanceof Player || entity instanceof Monster || entity instanceof PathfinderMob) {
            AttributeInstance attackDamage = entity.getAttribute(Attributes.ATTACK_DAMAGE);
            if (attackDamage != null) {
                attackDamage.removeModifier(ATTACK_DAMAGE_UUID);
            }
        }
    }
    
    private static void cleanupEntityWildHuntData(LivingEntity entity) {
        UUID entityUUID = entity.getUUID();
        
        // 移除所有属性修饰符
        removeWildHuntAttributeModifiers(entity);
        
        // 清除狂猎状态NBT数据（保留最大生命值减少数据）
        CompoundTag persistentData = entity.getPersistentData();
        persistentData.remove(WILD_HUNT_ACTIVE_KEY);
        persistentData.remove(WILD_HUNT_START_TIME_KEY);
        persistentData.remove(HYPER_LETHAL_DAMAGE_KEY);
        
        // 清除内存缓存
        entityInWildHuntMap.remove(entityUUID);
        entityMaxHealthReductionMap.remove(entityUUID);
        entityHyperLethalDamageMap.remove(entityUUID);
    }
    
    private static void clearMaxHealthReduction(LivingEntity entity) {
        UUID entityUUID = entity.getUUID();
        
        // 移除最大生命值减少修饰符
        AttributeInstance maxHealth = entity.getAttribute(Attributes.MAX_HEALTH);
        if (maxHealth != null) {
            maxHealth.removeModifier(MAX_HEALTH_REDUCTION_UUID);
        }
        
        // 清除最大生命值减少的NBT数据
        CompoundTag persistentData = entity.getPersistentData();
        persistentData.remove(WILD_HUNT_MAX_HEALTH_REDUCTION_KEY);
        
        // 清除内存缓存
        entityMaxHealthReductionMap.remove(entityUUID);
    }
    
    private static void cleanupAllEntityWildHuntData(LivingEntity entity) {
        UUID entityUUID = entity.getUUID();
        
        // 移除所有属性修饰符
        removeWildHuntAttributeModifiers(entity);
        
        // 清除所有狂猎相关的NBT数据
        CompoundTag persistentData = entity.getPersistentData();
        persistentData.remove(WILD_HUNT_ACTIVE_KEY);
        persistentData.remove(WILD_HUNT_START_TIME_KEY);
        persistentData.remove(WILD_HUNT_MAX_HEALTH_REDUCTION_KEY);
        persistentData.remove(HYPER_LETHAL_DAMAGE_KEY);
        
        // 清除所有内存缓存
        entityInWildHuntMap.remove(entityUUID);
        entityMaxHealthReductionMap.remove(entityUUID);
        entityHyperLethalDamageMap.remove(entityUUID);
    }
    
    private static void updateMaxHealthModifier(LivingEntity entity, double reductionAmount) {
        // 更新最大生命值减少修饰符
        AttributeInstance attribute = entity.getAttribute(Attributes.MAX_HEALTH);
        if (attribute != null) {
            // 移除可能存在的旧修饰符
            attribute.removeModifier(MAX_HEALTH_REDUCTION_UUID);
            
            // 添加新的负值修饰符
            if (reductionAmount > 0) {
                AttributeModifier modifier = new AttributeModifier(
                    MAX_HEALTH_REDUCTION_UUID,
                    "Wild Hunt Health Reduction",
                    -reductionAmount,
                    AttributeModifier.Operation.ADDITION
                );
                attribute.addTransientModifier(modifier);
            }
        }
    }
    
    private static void reapplyMaxHealthReductionModifier(LivingEntity entity) {
        // 从NBT数据中获取最大生命值减少量
        double reductionAmount = getEntityMaxHealthReduction(entity);
        // 直接更新生命值控制属性
        updateMaxHealthModifier(entity, reductionAmount);
    }
    
    private static void restoreEntityWildHuntState(LivingEntity entity) {
        if (entity.level().isClientSide()) {
            return;
        }
        
        // 检查实体是否有狂猎状态数据
        if (isEntityInWildHunt(entity)) {
            ServerLevel serverLevel = (ServerLevel) entity.level();
            UUID entityUUID = entity.getUUID();
            long startTime = getEntityStartTime(entity);
            long currentTime = serverLevel.getGameTime();
            boolean isDurationExpired = (currentTime - startTime) >= WILD_HUNT_DURATION;
            
            // 检查实体是否仍然穿着带有狂猎附魔的胸甲
            ItemStack chestArmor = entity.getItemBySlot(EquipmentSlot.CHEST);
            int wildHuntLevel = EnchantmentHelper.getItemEnchantmentLevel(
                EverlaartifactsModEnchantments.WILD_HUNT.get(), chestArmor);
            
            // 安全检查：如果实体最大生命值小于等于1，则清除所有数据
            if (entity.getMaxHealth() <= IMMUNITY_THRESHOLD) {
                cleanupAllEntityWildHuntData(entity);
                return;
            }
            
            // 如果狂猎状态已过期
            if (isDurationExpired) {
                // 结束狂猎状态但保留最大生命值减少
                setEntityInWildHunt(entity, false);
                // 移除临时属性修饰符
                removeTemporaryAttributeModifiers(entity);
                // 重新应用最大生命值减少修饰符
                reapplyMaxHealthReductionModifier(entity);
                
                // 如果当前最大生命值仍大于1，则应用额外8%减少
                if (entity.getMaxHealth() > IMMUNITY_THRESHOLD) {
                    applyAdditionalHealthReduction(entity);
                }
                
                // 恢复生命值到当前最大生命值
                entity.setHealth(entity.getMaxHealth());
            }
            // 如果仍在狂猎期间但脱下了盔甲
            else if (wildHuntLevel <= 0) {
                // 移除所有属性修饰符并清除数据
                removeWildHuntAttributeModifiers(entity);
                cleanupAllEntityWildHuntData(entity);
                // 强制杀死玩家
                if (entity instanceof Player player) {
                    player.hurt(player.damageSources().genericKill(), Float.MAX_VALUE);
                }
            }
            // 如果仍在狂猎期间且穿着盔甲
            else {
                // 恢复狂猎状态
                entityInWildHuntMap.put(entityUUID, true);
                // 恢复内存缓存中的最大生命值减少数据
                double reductionAmount = getEntityMaxHealthReduction(entity);
                entityMaxHealthReductionMap.put(entityUUID, reductionAmount);
                // 重新应用所有属性修饰符
                applyWildHuntAttributeModifiers(entity);
                // 重新应用最大生命值减少修饰符
                reapplyMaxHealthReductionModifier(entity);
                // 锁定生命值为1
                entity.setHealth(IMMUNITY_THRESHOLD);
            }
        }
        // 如果没有狂猎状态但有最大生命值减少数据
        else {
            double reductionAmount = getEntityMaxHealthReduction(entity);
            if (reductionAmount > 0) {
                // 重新应用最大生命值减少修饰符
                reapplyMaxHealthReductionModifier(entity);
            }
        }
    }
    
    // 检查实体是否在狂猎状态结束时脱下盔甲的处理器
    @SubscribeEvent
    public static void onLivingTick(LivingEvent.LivingTickEvent event) {
        if (event.getEntity().level().isClientSide()) {
            return;
        }
        
        LivingEntity entity = event.getEntity();
        ServerLevel serverLevel = (ServerLevel) entity.level();
        UUID entityUUID = entity.getUUID();
        long currentTick = serverLevel.getGameTime();
        
        // 检查是否处于狂猎状态
        boolean currentlyInWildHunt = isEntityInWildHunt(entity) && !isEntityKilledByHyperLethal(entity);
        
        // 添加安全检查：如果实体生命值过低但在狂猎状态中，强制恢复
        if (currentlyInWildHunt && entity.getHealth() <= 0) {
            entity.setHealth(IMMUNITY_THRESHOLD);
        }
        
        if (currentlyInWildHunt) {
            long startTime = getEntityStartTime(entity);
            boolean isDurationExpired = (currentTick - startTime) >= WILD_HUNT_DURATION;
            
            // 检查是否仍然穿着带有狂猎附魔的胸甲
            ItemStack chestArmor = entity.getItemBySlot(EquipmentSlot.CHEST);
            int wildHuntLevel = EnchantmentHelper.getItemEnchantmentLevel(
                EverlaartifactsModEnchantments.WILD_HUNT.get(), chestArmor);
            
            // 应用属性减益效果
            applyWildHuntAttributeModifiers(entity);
            
            // 每5刻强制锁定生命值为1
            entity.setHealth(IMMUNITY_THRESHOLD);
            
            // 生成持续的黑烟粒子
            spawnWildHuntParticles(entity, serverLevel);
            
            
            // 如果狂猎状态已结束
            if (isDurationExpired) {
                // 结束狂猎状态但保留最大生命值减少
                setEntityInWildHunt(entity, false);
                // 移除移动速度和攻击力减益修饰符
                removeTemporaryAttributeModifiers(entity);
                // 重新应用最大生命值减少修饰符
                reapplyMaxHealthReductionModifier(entity);
                
                // 如果当前最大生命值仍大于1，则应用额外8%减少
                if (entity.getMaxHealth() > IMMUNITY_THRESHOLD) {
                    applyAdditionalHealthReduction(entity);
                }
                
                // 恢复生命值到当前最大生命值（此时已经是减少后的最大生命值）
                entity.setHealth(entity.getMaxHealth());
            }
            // 如果在狂猎期间脱下盔甲，则立即死亡（仅对玩家）
            else if (!isDurationExpired && wildHuntLevel <= 0) {
                // 移除属性修饰符
                removeWildHuntAttributeModifiers(entity);
                // 强制杀死玩家
                if (entity instanceof Player player) {
                    player.hurt(player.damageSources().genericKill(), Float.MAX_VALUE);
                    cleanupEntityWildHuntData(player);
                }
            }
        } else {
            // 如果不在狂猎状态，确保移除属性修饰符
            removeTemporaryAttributeModifiers(entity);
        }
    }
}