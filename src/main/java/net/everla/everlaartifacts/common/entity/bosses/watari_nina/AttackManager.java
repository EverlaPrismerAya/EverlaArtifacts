package net.everla.everlaartifacts.common.entity.bosses.watari_nina;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.LivingEntity;

/**
 * Watari Nina攻击管理系统
 * 负责管理Attack数据、复活机制和攻击状态切换
 */
public class AttackManager {
    
    // NBT标签键
    public static final String ATTACK_KEY = "Attack";
    public static final String IS_ATTACKING_KEY = "IsAttacking";
    public static final String ATTACK_START_TIME_KEY = "AttackStartTime";
    public static final String CURRENT_ATTACK_INDEX_KEY = "CurrentAttackIndex";
    
    // 攻击相关常量
    private static final int MAX_ATTACK_COUNT = 4; // Attack达到4时才真正死亡
    private static final int ATTACK_DURATION = 600; // 30秒 = 600 ticks
    
    /**
     * 获取当前Attack值
     * 
     * @param entity Watari Nina实体
     * @return 当前Attack值
     */
    public static int getAttack(LivingEntity entity) {
        CompoundTag persistentData = entity.getPersistentData();
        return persistentData.getInt(ATTACK_KEY);
    }
    
    /**
     * 设置Attack值
     * 
     * @param entity Watari Nina实体
     * @param attack Attack值
     */
    public static void setAttack(LivingEntity entity, int attack) {
        CompoundTag persistentData = entity.getPersistentData();
        persistentData.putInt(ATTACK_KEY, attack);
        
        // 如果正在攻击，结束当前攻击
        if (isAttacking(entity)) {
            setAttacking(entity, false);
            cleanupAttackSpecificData(entity);
        }
    }
    
    /**
     * 增加Attack值
     * 
     * @param entity Watari Nina实体
     */
    public static void incrementAttack(LivingEntity entity) {
        int currentAttack = getAttack(entity);
        setAttack(entity, currentAttack + 1);
    }
    
    /**
     * 清理特定攻击的数据
     * 
     * @param entity Watari Nina实体
     */
    private static void cleanupAttackSpecificData(LivingEntity entity) {
        CompoundTag persistentData = entity.getPersistentData();
        
        // 清理所有攻击相关数据
        persistentData.getAllKeys().removeIf(key -> 
            key.startsWith("NoSpell1") || key.startsWith("NoSpell2") || key.startsWith("SpellCard1"));
    }
    
    /**
     * 检查是否应该复活
     * 
     * @param entity Watari Nina实体
     * @return 如果应该复活返回true，否则返回false
     */
    public static boolean shouldRevive(LivingEntity entity) {
        int attack = getAttack(entity);
        return attack <= MAX_ATTACK_COUNT;
    }
    
    /**
     * 复活Boss并重置状态
     * 
     * @param entity Watari Nina实体
     */
    public static void reviveBoss(LivingEntity entity) {
        // 恢复满血
        entity.setHealth(entity.getMaxHealth());
        
        // 重置攻击状态
        setAttacking(entity, false);
        setAttackStartTime(entity, 0);
        setCurrentAttackIndex(entity, 0);
        
        // 增加Attack值
        incrementAttack(entity);
    }
    
    /**
     * 检查是否正在攻击
     * 
     * @param entity Watari Nina实体
     * @return 是否正在攻击
     */
    public static boolean isAttacking(LivingEntity entity) {
        CompoundTag persistentData = entity.getPersistentData();
        return persistentData.getBoolean(IS_ATTACKING_KEY);
    }
    
    /**
     * 设置攻击状态
     * 
     * @param entity Watari Nina实体
     * @param attacking 是否正在攻击
     */
    public static void setAttacking(LivingEntity entity, boolean attacking) {
        CompoundTag persistentData = entity.getPersistentData();
        persistentData.putBoolean(IS_ATTACKING_KEY, attacking);
    }
    
    /**
     * 获取攻击开始时间
     * 
     * @param entity Watari Nina实体
     * @return 攻击开始时间
     */
    public static long getAttackStartTime(LivingEntity entity) {
        CompoundTag persistentData = entity.getPersistentData();
        return persistentData.getLong(ATTACK_START_TIME_KEY);
    }
    
    /**
     * 设置攻击开始时间
     * 
     * @param entity Watari Nina实体
     * @param time 开始时间
     */
    public static void setAttackStartTime(LivingEntity entity, long time) {
        CompoundTag persistentData = entity.getPersistentData();
        persistentData.putLong(ATTACK_START_TIME_KEY, time);
    }
    
    /**
     * 获取当前攻击索引
     * 
     * @param entity Watari Nina实体
     * @return 当前攻击索引
     */
    public static int getCurrentAttackIndex(LivingEntity entity) {
        CompoundTag persistentData = entity.getPersistentData();
        return persistentData.getInt(CURRENT_ATTACK_INDEX_KEY);
    }
    
    /**
     * 设置当前攻击索引
     * 
     * @param entity Watari Nina实体
     * @param index 攻击索引
     */
    public static void setCurrentAttackIndex(LivingEntity entity, int index) {
        CompoundTag persistentData = entity.getPersistentData();
        persistentData.putInt(CURRENT_ATTACK_INDEX_KEY, index);
    }
    
    /**
     * 检查攻击是否超时
     * 
     * @param entity Watari Nina实体
     * @param currentTime 当前时间
     * @return 是否超时
     */
    public static boolean isAttackTimedOut(LivingEntity entity, long currentTime) {
        if (!isAttacking(entity)) {
            return false;
        }
        
        long startTime = getAttackStartTime(entity);
        return (currentTime - startTime) >= ATTACK_DURATION;
    }
    
    /**
     * 初始化Attack数据
     * 
     * @param entity Watari Nina实体
     */
    public static void initializeAttackData(LivingEntity entity) {
        CompoundTag persistentData = entity.getPersistentData();
        
        if (!persistentData.contains(ATTACK_KEY)) {
            persistentData.putInt(ATTACK_KEY, 0);
        }
        if (!persistentData.contains(IS_ATTACKING_KEY)) {
            persistentData.putBoolean(IS_ATTACKING_KEY, false);
        }
        if (!persistentData.contains(ATTACK_START_TIME_KEY)) {
            persistentData.putLong(ATTACK_START_TIME_KEY, 0);
        }
        if (!persistentData.contains(CURRENT_ATTACK_INDEX_KEY)) {
            persistentData.putInt(CURRENT_ATTACK_INDEX_KEY, 0);
        }
    }
    
    /**
     * 清理Attack数据
     * 
     * @param entity Watari Nina实体
     */
    public static void cleanupAttackData(LivingEntity entity) {
        CompoundTag persistentData = entity.getPersistentData();
        persistentData.remove(ATTACK_KEY);
        persistentData.remove(IS_ATTACKING_KEY);
        persistentData.remove(ATTACK_START_TIME_KEY);
        persistentData.remove(CURRENT_ATTACK_INDEX_KEY);
    }
}