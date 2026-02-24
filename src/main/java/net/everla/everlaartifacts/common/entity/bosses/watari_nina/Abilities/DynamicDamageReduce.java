package net.everla.everlaartifacts.common.entity.bosses.watari_nina.Abilities;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.*;

@Mod.EventBusSubscriber
public class DynamicDamageReduce {
    
    // 动态减伤相关常量
    private static final int DPS_CALCULATION_WINDOW = 600; // 30秒 = 600 ticks (WatariNina挑战限时)
    private static final double MAX_DAMAGE_REDUCTION = 0.95; // 最大95%减伤
    private static final double ANTI_OHK_THRESHOLD = 0.3; // 防秒杀阈值：单次伤害不超过最大生命值的30%
    
    // DPS跟踪数据结构 - 使用Boss实体UUID+玩家UUID作为复合键
    private static final Map<String, List<DamageRecord>> entityPlayerDamageHistory = new HashMap<>();
    private static final Map<String, Long> entityPlayerLastDamageTime = new HashMap<>();
    
    // 清理计数器
    private static int cleanupCounter = 0;
    private static final int CLEANUP_INTERVAL = 100; // 每100个tick清理一次
    
    /**
     * 监听生物受伤事件，处理Watari Nina的动态减伤和防秒杀逻辑
     */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onLivingHurt(LivingHurtEvent event) {
        // 只在服务端处理
        if (event.getEntity().level().isClientSide()) {
            return;
        }
        
        // 检查受伤实体是否为Watari Nina
        if (!(event.getEntity() instanceof net.everla.everlaartifacts.common.entity.bosses.watari_nina.WatariNinaEntity)) {
            return;
        }
        
        // 处理动态减伤和防秒杀逻辑
        processDynamicDamageReduction(event);
    }
    
    /**
     * 处理动态减伤和防秒杀逻辑
     * 
     * @param event 伤害事件
     */
    private static void processDynamicDamageReduction(LivingHurtEvent event) {
        LivingEntity watariNina = event.getEntity();
        DamageSource damageSource = event.getSource();
        
        // 只处理玩家造成的伤害
        if (!(damageSource.getEntity() instanceof Player player)) {
            return;
        }
        
        UUID playerUUID = player.getUUID();
        UUID entityUUID = watariNina.getUUID();
        float originalDamage = event.getAmount();
        long currentTime = watariNina.level().getGameTime();
        
        // 记录伤害历史（按实体隔离）
        recordPlayerDamage(entityUUID, playerUUID, originalDamage, currentTime);
        
        // 计算玩家当前DPS（按实体隔离）
        double currentDPS = calculatePlayerDPS(entityUUID, playerUUID, currentTime);
        
        // 计算目标有效DPS (总血量/30秒)
        double targetDPS = calculateTargetDPS(watariNina);
        
        // 计算减伤率
        double reductionRate = calculateDamageReduction(currentDPS, targetDPS);
        
        // 应用动态减伤
        float reducedDamage = (float) (originalDamage * (1.0 - reductionRate));
        
        // 应用防秒杀机制
        float antiOHKDamage = applyAntiOHK(reducedDamage, watariNina);
        
        // 设置最终伤害值
        event.setAmount(antiOHKDamage);
        
        // 更新最后一次伤害时间（按实体隔离）
        entityPlayerLastDamageTime.put(getEntityPlayerKey(entityUUID, playerUUID), currentTime);
    }
    
    /**
     * 生成实体-玩家复合键
     * 
     * @param entityUUID 实体UUID
     * @param playerUUID 玩家UUID
     * @return 复合键字符串
     */
    private static String getEntityPlayerKey(UUID entityUUID, UUID playerUUID) {
        return entityUUID.toString() + ":" + playerUUID.toString();
    }
    
    /**
     * 记录玩家伤害历史（按实体隔离）
     * 
     * @param entityUUID 实体UUID
     * @param playerUUID 玩家UUID
     * @param damage 伤害值
     * @param timestamp 时间戳
     */
    private static void recordPlayerDamage(UUID entityUUID, UUID playerUUID, float damage, long timestamp) {
        String key = getEntityPlayerKey(entityUUID, playerUUID);
        entityPlayerDamageHistory.computeIfAbsent(key, k -> new ArrayList<>())
            .add(new DamageRecord(damage, timestamp));
    }
    
    /**
     * 计算玩家当前DPS（按实体隔离）
     * 
     * @param entityUUID 实体UUID
     * @param playerUUID 玩家UUID
     * @param currentTime 当前时间
     * @return 玩家当前DPS
     */
    private static double calculatePlayerDPS(UUID entityUUID, UUID playerUUID, long currentTime) {
        String key = getEntityPlayerKey(entityUUID, playerUUID);
        List<DamageRecord> damageRecords = entityPlayerDamageHistory.get(key);
        if (damageRecords == null || damageRecords.isEmpty()) {
            return 0.0;
        }
        
        // 清理过期记录
        damageRecords.removeIf(record -> currentTime - record.timestamp > DPS_CALCULATION_WINDOW);
        
        if (damageRecords.isEmpty()) {
            return 0.0;
        }
        
        // 计算窗口期内的总伤害
        double totalDamage = damageRecords.stream()
            .mapToDouble(record -> record.damage)
            .sum();
        
        // 计算实际时间窗口长度（ticks）
        long timeWindow = Math.min(DPS_CALCULATION_WINDOW, 
            currentTime - damageRecords.get(0).timestamp);
        
        // 转换为秒并计算DPS
        double timeInSeconds = timeWindow / 20.0;
        return timeInSeconds > 0 ? totalDamage / timeInSeconds : 0.0;
    }
    
    /**
     * 计算目标有效DPS
     * 
     * @param watariNina Watari Nina实体
     * @return 目标DPS (总血量/30秒)
     */
    private static double calculateTargetDPS(LivingEntity watariNina) {
        // Watari Nina基础生命值为300，但可能被其他模组修改
        float maxHealth = watariNina.getMaxHealth();
        // 目标：15秒内击败Watari Nina
        return maxHealth / 15.0;
    }
    
    /**
     * 计算动态减伤率
     * 公式：y = min(0.95, 1-R/x) 当x>R时，否则为0
     * 
     * @param currentDPS 当前DPS
     * @param targetDPS 目标DPS
     * @return 减伤率 (0-0.95)
     */
    private static double calculateDamageReduction(double currentDPS, double targetDPS) {
        if (currentDPS <= targetDPS) {
            return 0.0; // 不超过目标DPS时不减伤
        }
        
        // 计算减伤率：1 - R/x
        double reduction = 1.0 - (targetDPS / currentDPS);
        
        // 限制最大减伤率为95%
        return Math.min(reduction, MAX_DAMAGE_REDUCTION);
    }
    
    /**
     * 应用防秒杀机制
     * 
     * @param damage 伤害值
     * @param watariNina Watari Nina实体
     * @return 应用防秒杀后的伤害值
     */
    private static float applyAntiOHK(float damage, LivingEntity watariNina) {
        float maxHealth = watariNina.getMaxHealth();
        float maxAllowedDamage = maxHealth * (float) ANTI_OHK_THRESHOLD;
        
        // 限制单次伤害不超过最大生命值的30%
        return Math.min(damage, maxAllowedDamage);
    }
    
    /**
     * 服务器tick事件监听器，用于定期清理过期数据
     */
    /*
    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        
        cleanupCounter++;
        if (cleanupCounter >= CLEANUP_INTERVAL) {
            cleanupExpiredData(event.getServer().getTickCount());
            cleanupCounter = 0;
        }
    }
    */
    
    /**
     * 清理过期的伤害记录和时间数据（按实体隔离）
     * 
     * @param currentTime 当前服务器tick时间
     */
    private static void cleanupExpiredData(long currentTime) {
        // 清理长时间未活动的实体-玩家数据
        entityPlayerLastDamageTime.entrySet().removeIf(entry -> 
            currentTime - entry.getValue() > DPS_CALCULATION_WINDOW * 2);
        
        // 清理对应的伤害记录
        entityPlayerDamageHistory.entrySet().removeIf(entry -> 
            !entityPlayerLastDamageTime.containsKey(entry.getKey()));
        
        // 清理空的伤害记录列表
        entityPlayerDamageHistory.values().removeIf(List::isEmpty);
    }
    
    /**
     * 伤害记录数据类
     */
    private static class DamageRecord {
        final float damage;
        final long timestamp;
        
        DamageRecord(float damage, long timestamp) {
            this.damage = damage;
            this.timestamp = timestamp;
        }
    }
    
    /**
     * 测试用方法：清理所有测试数据
     * 注意：仅用于测试环境
     */
    public static void clearTestData() {
        entityPlayerDamageHistory.clear();
        entityPlayerLastDamageTime.clear();
        cleanupCounter = 0;
    }
}
