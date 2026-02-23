package net.everla.everlaartifacts.common.difficulty;

import net.everla.everlaartifacts.common.game_rules.EnableLunaticMode;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.WitherSkull;
import net.minecraft.world.level.GameRules;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.core.BlockPos;

/**
 * 凋灵特殊攻击处理类
 * 负责管理凋灵的所有特殊攻击行为，包括冲撞攻击和下砸技能
 */
public class LunaticWitherSpecialAttack {
    
    // 凋灵冲撞攻击相关常量
    private static final double HEALTH_CHARGE_THRESHOLD = 0.5; // 50%生命值阈值
    private static final int CHARGE_COOLDOWN_TICKS = 100; // 冷却
    private static final float CHARGE_DAMAGE = 36.0f; // 冲撞伤害
    private static final int CHARGE_WINDUP_TICKS = 15; // 前摇
    private static final int CHARGE_RECOVERY_TICKS = 20; // 后摇
    public static final String LAST_CHARGE_TIME_KEY = "LastChargeTime";
    public static final String IS_CHARGING_KEY = "IsCharging";
    private static final String LOCKED_CHARGE_DIRECTION_X = "LockedChargeDirX";
    private static final String LOCKED_CHARGE_DIRECTION_Y = "LockedChargeDirY";
    private static final String LOCKED_CHARGE_DIRECTION_Z = "LockedChargeDirZ";
    
    // 凋灵下砸技能相关常量
    private static final double HEALTH_SMASH_THRESHOLD = 0.5; // 50%生命值阈值
    private static final int SMASH_COOLDOWN_TICKS = 300; // 15秒 = 300 ticks
    private static final int SMASH_COUNT = 3; // 下砸3次
    private static final double SMASH_UPWARD_DISTANCE = 10.0; // 向上移动10方块
    private static final double SMASH_DOWNWARD_DISTANCE = 20.0; // 下砸20方块
    private static final int SMASH_RECOVERY_TICKS = 30; // 30刻后摇
    private static final float SMASH_EXPLOSION_POWER = 3.0f; // 爆炸强度
    // 移除基于时间的保护期常量，改为检查Invul标签
    public static final String LAST_SMASH_TIME_KEY = "LastSmashTime";
    public static final String IS_SMASHING_KEY = "IsSmashing";
    private static final String SMASH_PHASE_KEY = "SmashPhase";
    private static final String SMASH_TARGET_Y_KEY = "SmashTargetY";
    private static final String SMASH_CURRENT_COUNT_KEY = "SmashCurrentCount";
    public static final String SPAWN_TIME_KEY = "SpawnTime";
    public static final String SPECIAL_ATTACK_ENABLED_KEY = "SpecialAttackEnabled";
    
    /**
     * 检查并触发冲撞攻击
     * 
     * @param wither 凋灵实体
     */
    public static void checkAndTriggerChargeAttack(WitherBoss wither) {
        // 这个方法现在由processWitherChargeAttack统一处理
        // 保持空实现以维持API兼容性
    }
    
    /**
     * 处理凋灵下砸技能逻辑
     * 
     * @param wither 凋灵实体
     */
    public static void processWitherSmashAttack(WitherBoss wither) {
        // 检查是否启用了月狂或额外模式
        if (!isLunaticModeEnabled(wither.level())) {
            return;
        }
        
        // 检查特殊攻击是否已启用
        if (!isSpecialAttackEnabled(wither)) {
            return;
        }
        
        // 检查生命值是否小于等于50%
        float currentHealth = wither.getHealth();
        float maxHealth = wither.getMaxHealth();
        double healthPercentage = currentHealth / maxHealth;
        
        if (healthPercentage > HEALTH_SMASH_THRESHOLD) {
            return;
        }
        
        // 检查是否有有效目标
        LivingEntity target = wither.getTarget();
        if (!(target instanceof Player targetPlayer) || targetPlayer.isSpectator() || targetPlayer.isCreative()) {
            return; // 没有有效目标时不执行下砸
        }
        
        long currentTime = wither.level().getGameTime();
        long lastSmashTime = getLastSmashTime(wither);
        
        // 检查冷却时间
        if (currentTime - lastSmashTime < SMASH_COOLDOWN_TICKS) {
            return;
        }
        
        // 检查是否正在执行下砸
        if (isSmashing(wither)) {
            return;
        }
        
        // 开始下砸技能
        startWitherSmash(wither);
        
        // 更新最后下砸时间
        setLastSmashTime(wither, currentTime);
    }
    
    /**
     * 处理凋灵冲撞攻击逻辑
     * 只进行一次冲撞
     * 
     * @param wither 凋灵实体
     */
    public static void processWitherChargeAttack(WitherBoss wither) {
        // 检查是否启用了月狂或额外模式
        if (!isLunaticModeEnabled(wither.level())) {
            return;
        }
        
        // 检查特殊攻击是否已启用
        if (!isSpecialAttackEnabled(wither)) {
            return;
        }
        
        // 检查生命值是否大于50%
        float currentHealth = wither.getHealth();
        float maxHealth = wither.getMaxHealth();
        double healthPercentage = currentHealth / maxHealth;
        
        if (healthPercentage <= HEALTH_CHARGE_THRESHOLD) {
            return;
        }
        
        // 检查是否有有效目标
        LivingEntity target = wither.getTarget();
        if (!(target instanceof Player targetPlayer) || targetPlayer.isSpectator() || targetPlayer.isCreative()) {
            return; // 没有有效目标时不执行冲撞
        }
        
        long currentTime = wither.level().getGameTime();
        long lastChargeTime = getLastChargeTime(wither);
        
        // 检查冷却时间
        if (currentTime - lastChargeTime < CHARGE_COOLDOWN_TICKS) {
            return;
        }
        
        // 检查是否正在进行冲撞
        if (isCharging(wither)) {
            return; // 已经在冲撞中，不重复触发
        }
        
        // 执行单次冲撞
        executeSingleCharge(wither, targetPlayer);
        
        // 更新最后冲撞时间
        setLastChargeTime(wither, currentTime);
    }
    
    // === 私有辅助方法 ===
    
    /**
     * 检查是否启用了月狂或额外模式
     */
    private static boolean isLunaticModeEnabled(net.minecraft.world.level.Level level) {
        if (level.getDifficulty() != Difficulty.HARD) {
            return false;
        }
            
        GameRules gameRules = level.getGameRules();
        return gameRules.getBoolean(EnableLunaticMode.ENABLE_LUNATIC_MODE);
    }
    
    /**
     * 获取最后冲撞时间
     */
    private static long getLastChargeTime(WitherBoss wither) {
        CompoundTag persistentData = wither.getPersistentData();
        return persistentData.getLong(LAST_CHARGE_TIME_KEY);
    }
    
    /**
     * 设置最后冲撞时间
     */
    private static void setLastChargeTime(WitherBoss wither, long time) {
        CompoundTag persistentData = wither.getPersistentData();
        persistentData.putLong(LAST_CHARGE_TIME_KEY, time);
    }
    
    /**
     * 检查是否正在冲撞
     */
    private static boolean isCharging(WitherBoss wither) {
        CompoundTag persistentData = wither.getPersistentData();
        return persistentData.getBoolean(IS_CHARGING_KEY);
    }
    
    /**
     * 获取最后下砸时间
     */
    private static long getLastSmashTime(WitherBoss wither) {
        CompoundTag persistentData = wither.getPersistentData();
        return persistentData.getLong(LAST_SMASH_TIME_KEY);
    }
    
    /**
     * 检查是否正在下砸
     */
    private static boolean isSmashing(WitherBoss wither) {
        CompoundTag persistentData = wither.getPersistentData();
        return persistentData.getBoolean(IS_SMASHING_KEY);
    }
    
    // === 冲撞攻击相关方法（具体实现在下面）===
    
    /**
     * 执行单次冲撞
     */
    private static void executeSingleCharge(WitherBoss wither, Player targetPlayer) {
        // 前摇阶段
        startChargeWindup(wither, targetPlayer);
        
        // 延迟执行冲撞
        wither.level().getServer().tell(new net.minecraft.server.TickTask(
            wither.level().getServer().getTickCount() + CHARGE_WINDUP_TICKS,
            () -> {
                if (!wither.isRemoved() && !targetPlayer.isRemoved()) {
                    performChargeMovement(wither, targetPlayer);
                }
            }
        ));
        
        // 延迟执行后摇
        wither.level().getServer().tell(new net.minecraft.server.TickTask(
            wither.level().getServer().getTickCount() + CHARGE_WINDUP_TICKS + CHARGE_RECOVERY_TICKS,
            () -> {
                if (!wither.isRemoved()) {
                    endChargeRecovery(wither);
                }
            }
        ));
    }
    
    private static void moveToSameYLevel(WitherBoss wither, Player player) {
        // 获取玩家的Y坐标
        double targetY = player.getY();
        
        // 设置凋灵的Y坐标（保持XZ位置不变）
        wither.moveTo(
            wither.getX(),
            targetY,
            wither.getZ(),
            wither.getYRot(),
            wither.getXRot()
        );
        
        // 标记位置已更新
        wither.hurtMarked = true;
    }
    

    
    private static void startChargeWindup(WitherBoss wither, Player target) {
        if (wither.isRemoved() || target.isRemoved()) {
            return;
        }
        
        // 计算并锁定冲撞方向
        net.minecraft.world.phys.Vec3 direction = target.position().subtract(wither.position()).normalize();
        setLockedChargeDirection(wither, direction);
        
        // 设置正在冲撞标记
        setIsCharging(wither, true);
        
        // 前摇期间停止移动
        wither.setDeltaMovement(0, 0, 0);
        
        // 开始三轮齐射
        startThreeRoundBarrage(wither, target, direction);
    }
    
    private static void endChargeRecovery(WitherBoss wither) {
        if (wither.isRemoved()) {
            return;
        }
        
        // 清除正在冲撞标记
        setIsCharging(wither, false);
    }
    
    private static void performChargeMovement(WitherBoss wither, Player target) {
        if (wither.isRemoved() || target.isRemoved()) {
            return;
        }
        
        // 检查是否仍在冲撞状态
        if (!isCharging(wither)) {
            return;
        }
        
        // 使用锁定的冲撞方向
        net.minecraft.world.phys.Vec3 direction = getLockedChargeDirection(wither);
        
        // 设置凋灵的移动方向（冲撞）
        wither.setDeltaMovement(direction.scale(9.0)); // 提高冲撞速度
        
        // 检查碰撞
        checkChargeCollision(wither, target);
        
        // 冲撞过程中不再生成额外的凋灵之首
    }
    
    private static void checkChargeCollision(WitherBoss wither, Player target) {
        // 检查距离是否足够近以造成伤害
        double distance = wither.distanceTo(target);
        if (distance < 2.5) { // 2.5格内的碰撞距离
            // 造成冲撞伤害
            target.hurt(wither.damageSources().mobAttack(wither), CHARGE_DAMAGE);
            
            // 击退效果
            net.minecraft.world.phys.Vec3 knockback = target.position().subtract(wither.position()).normalize().scale(1.2);
            target.setDeltaMovement(knockback);
        }
    }
    
    /**
     * 开始三轮齐射
     * 从凋灵两边头颅处瞄准玩家，进行三轮齐射
     * 前两轮为黑色，最后一轮为蓝色
     */
    private static void startThreeRoundBarrage(WitherBoss wither, Player target, net.minecraft.world.phys.Vec3 direction) {
        // 第一轮齐射（黑色）
        spawnBarrageRound(wither, target, direction, 0, false, 0);  // 立即执行
        
        // 第二轮齐射（黑色）
        spawnBarrageRound(wither, target, direction, 1, false, 5);
        
        // 第三轮齐射（蓝色）
        spawnBarrageRound(wither, target, direction, 2, true, 10);
    }
    
    /**
     * 生成一轮齐射
     * 
     * @param wither 凋灵实体
     * @param target 目标玩家
     * @param direction 冲撞方向
     * @param roundIndex 轮次索引
     * @param isBlue 是否为蓝色
     * @param delay 延迟ticks
     */
    private static void spawnBarrageRound(WitherBoss wither, Player target, net.minecraft.world.phys.Vec3 direction, 
                                        int roundIndex, boolean isBlue, int delay) {
        if (delay == 0) {
            // 立即执行
            spawnLeftRightSkulls(wither, target, direction, isBlue);
        } else {
            // 延迟执行
            wither.level().getServer().tell(new net.minecraft.server.TickTask(
                wither.level().getServer().getTickCount() + delay,
                () -> {
                    if (!wither.isRemoved() && !target.isRemoved()) {
                        spawnLeftRightSkulls(wither, target, direction, isBlue);
                    }
                }
            ));
        }
    }
    
    /**
     * 从凋灵左右两侧发射凋灵之首
     * 
     * @param wither 凋灵实体
     * @param target 目标玩家
     * @param direction 冲撞方向
     * @param isBlue 是否为蓝色
     */
    private static void spawnLeftRightSkulls(WitherBoss wither, Player target, net.minecraft.world.phys.Vec3 direction, boolean isBlue) {
        // 计算凋灵左右两侧的位置
        net.minecraft.world.phys.Vec3 rightDirection = new net.minecraft.world.phys.Vec3(-direction.z, 0, direction.x).normalize();
        net.minecraft.world.phys.Vec3 leftDirection = rightDirection.scale(-1);
        
        // 左侧头颅位置
        net.minecraft.world.phys.Vec3 leftOffset = leftDirection.scale(1.0).add(0, 2.5, 0);
        spawnSkullFromPosition(wither, target, leftOffset, isBlue);
        
        // 右侧头颅位置
        net.minecraft.world.phys.Vec3 rightOffset = rightDirection.scale(1.0).add(0, 2.5, 0);
        spawnSkullFromPosition(wither, target, rightOffset, isBlue);
    }
    
    /**
     * 从指定位置发射凋灵之首
     * 
     * @param wither 凋灵实体
     * @param target 目标玩家
     * @param offset 发射位置偏移
     * @param isBlue 是否为蓝色
     */
    private static void spawnSkullFromPosition(WitherBoss wither, Player target, net.minecraft.world.phys.Vec3 offset, boolean isBlue) {
        // 计算瞄准方向（从发射位置指向玩家）
        net.minecraft.world.phys.Vec3 aimDirection = target.position().subtract(
            wither.position().add(offset)
        ).normalize();
        
        // 创建凋灵之首
        WitherSkull skull = new WitherSkull(
            net.minecraft.world.entity.EntityType.WITHER_SKULL, 
            wither.level()
        );
        
        // 设置位置
        skull.moveTo(
            wither.getX() + offset.x,
            wither.getY() + offset.y,
            wither.getZ() + offset.z
        );
        
        // 设置初速度
        skull.setDeltaMovement(0, 0, 0);
        
        // 设置颜色
        skull.setDangerous(isBlue);
        
        // 设置发射者
        skull.setOwner(wither);
        
        // 设置目标方向
        skull.xPower = aimDirection.x * 0.1;
        skull.yPower = aimDirection.y * 0.1;
        skull.zPower = aimDirection.z * 0.1;
        
        // 添加到世界
        wither.level().addFreshEntity(skull);
    }

    private static void setLockedChargeDirection(WitherBoss wither, net.minecraft.world.phys.Vec3 direction) {
        CompoundTag persistentData = wither.getPersistentData();
        persistentData.putDouble(LOCKED_CHARGE_DIRECTION_X, direction.x);
        persistentData.putDouble(LOCKED_CHARGE_DIRECTION_Y, direction.y);
        persistentData.putDouble(LOCKED_CHARGE_DIRECTION_Z, direction.z);
    }
    
    private static net.minecraft.world.phys.Vec3 getLockedChargeDirection(WitherBoss wither) {
        CompoundTag persistentData = wither.getPersistentData();
        double x = persistentData.getDouble(LOCKED_CHARGE_DIRECTION_X);
        double y = persistentData.getDouble(LOCKED_CHARGE_DIRECTION_Y);
        double z = persistentData.getDouble(LOCKED_CHARGE_DIRECTION_Z);
        return new net.minecraft.world.phys.Vec3(x, y, z);
    }
    
    private static void setIsCharging(WitherBoss wither, boolean charging) {
        CompoundTag persistentData = wither.getPersistentData();
        persistentData.putBoolean(IS_CHARGING_KEY, charging);
    }
    
    // === 下砸技能相关方法（具体实现在下面）===
    
    private static void startWitherSmash(WitherBoss wither) {
        // 设置正在下砸标记
        setIsSmashing(wither, true);
        
        // 重置下砸计数器
        setSmashCurrentCount(wither, 0);
        
        // 开始第一次下砸
        executeNextSmash(wither);
    }
    
    private static void executeNextSmash(WitherBoss wither) {
        int currentCount = getSmashCurrentCount(wither);
        
        // 检查是否已完成所有下砸
        if (currentCount >= SMASH_COUNT) {
            endWitherSmash(wither);
            return;
        }
        
        // 检查实体状态
        if (wither.isRemoved()) {
            endWitherSmash(wither);
            return;
        }
        
        // 检查目标是否仍然有效
        LivingEntity target = wither.getTarget();
        if (!(target instanceof Player targetPlayer) || targetPlayer.isSpectator() || targetPlayer.isCreative()) {
            endWitherSmash(wither);
            return;
        }
        
        // 设置下砸阶段为上升阶段
        setSmashPhase(wither, 0); // 0 = 上升阶段
        
        // 计算目标上升高度
        double targetY = wither.getY() + SMASH_UPWARD_DISTANCE;
        setSmashTargetY(wither, targetY);
        
        // 开始上升
        performSmashRise(wither);
    }
    
    private static void performSmashRise(WitherBoss wither) {
        double targetY = getSmashTargetY(wither);
        double currentY = wither.getY();
        
        // 检查是否已达到目标高度
        if (currentY >= targetY - 0.5) {
            // 开始下降阶段
            setSmashPhase(wither, 1); // 1 = 下降阶段
            performSmashFall(wither);
            return;
        }
        
        // 向上移动
        wither.setDeltaMovement(0, 0.8, 0); // 向上速度
        
        // 延迟下一tick继续上升
        wither.level().getServer().tell(new net.minecraft.server.TickTask(
            wither.level().getServer().getTickCount() + 1,
            () -> performSmashRise(wither)
        ));
    }
    
    private static void performSmashFall(WitherBoss wither) {
        // 向下加速
        wither.setDeltaMovement(0, -1.2, 0); // 向下速度
        
        // 检查是否撞击到地面
        BlockPos blockPos = wither.blockPosition();
        net.minecraft.world.level.block.state.BlockState blockState = wither.level().getBlockState(blockPos.below());
        
        if (!blockState.isAir()) {
            // 撞击到方块，产生爆炸
            createSmashExplosion(wither);
            
            // 进入后摇阶段
            setSmashPhase(wither, 2); // 2 = 后摇阶段
            performSmashRecovery(wither);
            return;
        }
        
        // 检查是否超过最大下降距离
        double startY = getSmashTargetY(wither) - SMASH_UPWARD_DISTANCE;
        if (wither.getY() < startY - SMASH_DOWNWARD_DISTANCE) {
            // 下降距离过远仍未撞击到方块，跳过后摇
            endWitherSmash(wither);
            return;
        }
        
        // 延迟下一tick继续下降
        wither.level().getServer().tell(new net.minecraft.server.TickTask(
            wither.level().getServer().getTickCount() + 1,
            () -> performSmashFall(wither)
        ));
    }
    
    private static void createSmashExplosion(WitherBoss wither) {
        // 在凋灵位置创建爆炸
        wither.level().explode(
            wither,
            wither.getX(),
            wither.getY(),
            wither.getZ(),
            SMASH_EXPLOSION_POWER,
            net.minecraft.world.level.Level.ExplosionInteraction.BLOCK
        );
    }
    
    private static void performSmashRecovery(WitherBoss wither) {
        // 停止移动
        wither.setDeltaMovement(0, 0, 0);
        
        // 增加下砸计数
        int currentCount = getSmashCurrentCount(wither);
        setSmashCurrentCount(wither, currentCount + 1);
        
        // 延迟到后摇结束，然后执行下一次下砸
        wither.level().getServer().tell(new net.minecraft.server.TickTask(
            wither.level().getServer().getTickCount() + SMASH_RECOVERY_TICKS,
            () -> executeNextSmash(wither)
        ));
    }
    
    private static void endWitherSmash(WitherBoss wither) {
        // 清除下砸状态
        setIsSmashing(wither, false);
        setSmashPhase(wither, -1);
        setSmashTargetY(wither, 0);
    }
    
    private static void setLastSmashTime(WitherBoss wither, long time) {
        CompoundTag persistentData = wither.getPersistentData();
        persistentData.putLong(LAST_SMASH_TIME_KEY, time);
    }
    
    private static void setIsSmashing(WitherBoss wither, boolean smashing) {
        CompoundTag persistentData = wither.getPersistentData();
        persistentData.putBoolean(IS_SMASHING_KEY, smashing);
    }
    
    private static void setSmashPhase(WitherBoss wither, int phase) {
        CompoundTag persistentData = wither.getPersistentData();
        persistentData.putInt(SMASH_PHASE_KEY, phase);
    }
    
    private static void setSmashTargetY(WitherBoss wither, double targetY) {
        CompoundTag persistentData = wither.getPersistentData();
        persistentData.putDouble(SMASH_TARGET_Y_KEY, targetY);
    }
    
    private static void setSmashCurrentCount(WitherBoss wither, int count) {
        CompoundTag persistentData = wither.getPersistentData();
        persistentData.putInt(SMASH_CURRENT_COUNT_KEY, count);
    }
    
    private static int getSmashCurrentCount(WitherBoss wither) {
        CompoundTag persistentData = wither.getPersistentData();
        return persistentData.getInt(SMASH_CURRENT_COUNT_KEY);
    }
    
    private static double getSmashTargetY(WitherBoss wither) {
        CompoundTag persistentData = wither.getPersistentData();
        return persistentData.getDouble(SMASH_TARGET_Y_KEY);
    }
    
    private static int getSmashPhase(WitherBoss wither) {
        CompoundTag persistentData = wither.getPersistentData();
        return persistentData.getInt(SMASH_PHASE_KEY);
    }
    
    // === 生成相关方法 ===
    
    public static void setSpawnTime(WitherBoss wither, long spawnTime) {
        CompoundTag persistentData = wither.getPersistentData();
        persistentData.putLong(SPAWN_TIME_KEY, spawnTime);
    }
    
    public static void setSpecialAttackEnabled(WitherBoss wither, boolean enabled) {
        CompoundTag persistentData = wither.getPersistentData();
        persistentData.putBoolean(SPECIAL_ATTACK_ENABLED_KEY, enabled);
    }
    
    /**
     * 检查凋灵是否处于无敌状态（通过Invul NBT标签判断）
     * 
     * @param wither 凋灵实体
     * @return 是否处于无敌状态
     */
    private static boolean isWitherInvulnerable(WitherBoss wither) {
        // 检查凋灵的Invul NBT标签
        // Invul为数值型标签，0表示可攻击，其他值表示不可攻击
        CompoundTag entityData = wither.saveWithoutId(new CompoundTag());
        if (entityData.contains("Invul")) {
            int invulValue = entityData.getInt("Invul");
            return invulValue != 0; // Invul不为0时处于无敌状态
        }
        return false; // 如果没有Invul标签，默认认为可攻击
    }
    
    /**
     * 检查是否可以启用特殊攻击（基于Invul标签而非时间）
     * 
     * @param wither 凋灵实体
     * @return 是否可以启用特殊攻击
     */
    public static boolean canEnableSpecialAttack(WitherBoss wither) {
        // 当凋灵不再无敌时，可以启用特殊攻击
        return !isWitherInvulnerable(wither) && !wither.isRemoved();
    }
    
    /**
     * 检查特殊攻击是否已启用
     * 结合NBT存储的状态和Invul标签实时检查
     * 
     * @param wither 凋灵实体
     * @return 特殊攻击是否已启用
     */
    public static boolean isSpecialAttackEnabled(WitherBoss wither) {
        CompoundTag persistentData = wither.getPersistentData();
        // 明确检查键是否存在，如果不存在则返回false（表示禁用状态）
        if (!persistentData.contains(SPECIAL_ATTACK_ENABLED_KEY)) {
            return false;
        }
        
        boolean storedEnabled = persistentData.getBoolean(SPECIAL_ATTACK_ENABLED_KEY);
        
        // 如果存储的状态为true，还需要检查当前是否仍然满足启用条件
        if (storedEnabled) {
            // 检查凋灵是否仍然存在且不再无敌
            if (wither.isRemoved() || isWitherInvulnerable(wither)) {
                // 如果凋灵已被移除或重新进入无敌状态，禁用特殊攻击
                setSpecialAttackEnabled(wither, false);
                return false;
            }
            return true;
        }
        
        return false;
    }
}