package net.everla.everlaartifacts.server.handlers.effects;

import net.everla.everlaartifacts.init.EverlaartifactsModMobEffects;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.living.MobEffectEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Mod.EventBusSubscriber(modid = "everlaartifacts")
public class BloodBlossomDamageHandler {
    private static final Map<UUID, Long> lastDamageTime = new HashMap<>(); // 记录上次伤害时间，防止刷新重置
    private static final int DAMAGE_INTERVAL_TICKS = 80; // 4秒 = 80 ticks (20 ticks per second)
    private static final int CHECK_INTERVAL = 5; // 每5个tick检查一次，减少性能占用
    private static final Map<UUID, Integer> tickCounter = new ConcurrentHashMap<>(); // 为每个实体维护独立的检查计数器

    /**
     * 当实体获得状态效果时触发
     */
    @SubscribeEvent
    public static void onLivingEffectAdded(MobEffectEvent.Added event) {
        if (event.getEntity().level().isClientSide()) {
            return;
        }
        
        // 检查是否是BloodBlossom效果 - 使用正确的API
        if (event.getEffectInstance().getEffect() == EverlaartifactsModMobEffects.BLOOD_BLOSSOM.get()) {
            UUID entityUUID = event.getEntity().getUUID();
            long currentTime = event.getEntity().level().getGameTime();
            
            // 如果实体之前没有记录伤害时间，说明是第一次应用效果，设置当前时间为初始时间
            // 这样可以确保第一次伤害会在完整间隔后发生，而不是立即发生
            if (!lastDamageTime.containsKey(entityUUID)) {
                lastDamageTime.put(entityUUID, currentTime);
            }
            // 如果实体已有记录伤害时间，说明是效果刷新，不更新时间，确保伤害冷却不会重置
            
            // 初始化该实体的检查计数器
            tickCounter.put(entityUUID, 0);
        }
    }

    /**
     * 每tick检查具有BloodBlossom效果的实体并应用伤害
     */
    @SubscribeEvent
    public static void onLivingTick(LivingEvent.LivingTickEvent event) {
        LivingEntity entity = event.getEntity();
        
        // 只在服务端运行
        if (entity.level().isClientSide()) {
            return;
        }
        
        // 检查实体是否具有BloodBlossom效果
        if (!entity.hasEffect(EverlaartifactsModMobEffects.BLOOD_BLOSSOM.get())) {
            // 如果实体不再具有BloodBlossom效果，清理相关计数器
            UUID entityUUID = entity.getUUID();
            tickCounter.remove(entityUUID);
            return;
        }

        UUID entityUUID = entity.getUUID();
        
        // 更新该实体的检查计数器
        int currentCounter = tickCounter.getOrDefault(entityUUID, 0) + 1;
        tickCounter.put(entityUUID, currentCounter);
        
        // 只有在达到检查间隔时才执行伤害逻辑，减少性能占用
        if (currentCounter % CHECK_INTERVAL != 0) {
            return;
        }

        long currentTime = entity.level().getGameTime();

        // 检查是否达到伤害间隔
        long lastDamage = lastDamageTime.getOrDefault(entityUUID, currentTime); // 如果没有记录，则使用当前时间，确保首次伤害等待完整间隔
        if (currentTime - lastDamage >= DAMAGE_INTERVAL_TICKS) {
            applyBloodBlossomDamage(entity);
            lastDamageTime.put(entityUUID, currentTime); // 只有造成伤害后才更新时间
        }
    }

    /**
     * 对实体应用BloodBlossom伤害
     */
    private static void applyBloodBlossomDamage(LivingEntity entity) {
        CompoundTag entityData = entity.getPersistentData();
        
        UUID attackerUUID;
        double attackerCurrentAttackDamage;
        double attackerCurrentMaxHealth;

        // 检查是否有所需的持久数据，如果没有则使用默认值
        if (!entityData.contains("AttackedByUUID") || 
            !entityData.contains("AttackerCurrentAttackDamage") || 
            !entityData.contains("AttackerCurrentMaxHealth")) {
            // 默认值：1攻击力，1最大生命值
            attackerUUID = entity.getUUID(); // 使用实体自身UUID作为默认
            attackerCurrentAttackDamage = 1.0;
            attackerCurrentMaxHealth = 1.0;
        } else {
            // 获取存储的数据
            attackerUUID = entityData.getUUID("AttackedByUUID");
            attackerCurrentAttackDamage = entityData.getDouble("AttackerCurrentAttackDamage");
            attackerCurrentMaxHealth = entityData.getDouble("AttackerCurrentMaxHealth");
        }

        // 计算伤害: 115%攻击力 + 100%最大生命值
        double damageAmount = (attackerCurrentAttackDamage * 1.15) + (attackerCurrentMaxHealth * 1.0);

        // 创建伤害源 - 从攻击者位置对目标造成伤害
        DamageSource damageSource = createBloodBlossomDamageSource(entity, attackerUUID);

        // 应用伤害，绕过无敌帧机制
        if (damageAmount > 0) {
            // 设置伤害的伤害延迟为0，绕过无敌帧
            entity.invulnerableTime = 0;
            entity.hurt(damageSource, (float) damageAmount);
        }
    }

    /**
     * 创建BloodBlossom伤害源
     */
    private static DamageSource createBloodBlossomDamageSource(LivingEntity target, UUID attackerUUID) {
        // 尝试找到攻击者玩家
        Player attacker = null;
        if (target.level() instanceof net.minecraft.server.level.ServerLevel) {
            net.minecraft.server.level.ServerLevel serverLevel = (net.minecraft.server.level.ServerLevel) target.level();
            Entity attackerEntity = serverLevel.getEntity(attackerUUID);
            if (attackerEntity instanceof Player) {
                attacker = (Player) attackerEntity;
            }
        }

        // 如果找不到攻击者，使用普通的魔法伤害源
        if (attacker != null) {
            return target.damageSources().indirectMagic(attacker, null);
        } else {
            return target.damageSources().magic();
        }
    }

    /**
     * 当实体死亡时清理记录的时间
     */
    @SubscribeEvent
    public static void onLivingDeath(net.minecraftforge.event.entity.living.LivingDeathEvent event) {
        if (!event.getEntity().level().isClientSide()) {
            UUID entityUUID = event.getEntity().getUUID();
            lastDamageTime.remove(entityUUID);
            tickCounter.remove(entityUUID);
        }
    }

    /**
     * 当玩家离开世界时清理记录
     */
    @SubscribeEvent
    public static void onPlayerLoggedOut(net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent event) {
        UUID entityUUID = event.getEntity().getUUID();
        lastDamageTime.remove(entityUUID);
        tickCounter.remove(entityUUID);
    }
}