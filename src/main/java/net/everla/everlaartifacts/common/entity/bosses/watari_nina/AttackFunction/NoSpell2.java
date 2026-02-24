package net.everla.everlaartifacts.common.entity.bosses.watari_nina.AttackFunction;

import net.everla.everlaartifacts.common.entity.bosses.watari_nina.AttackManager;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.everla.everlaartifacts.common.entity.projectiles.DanmakuEntity;
import org.joml.Vector3f;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * NoSpell2非符攻击实现（强化版）
 * 攻击机制：与NoSpell1类似，但弹幕初始速度更慢，常态速度更快，弹幕数量增加到9，每次发射旋转30度
 * 持续时间：30秒（600 ticks）
 * 触发条件：Boss的Attack值为2时执行
 */
public class NoSpell2 {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(NoSpell2.class);
    
    // 攻击参数常量
    private static final double TELEPORT_DISTANCE = 10.0; // 瞬移距离
    private static final double HEIGHT_ABOVE_GROUND = 3.0; // 距地面高度
    private static final double CIRCLE_RADIUS = 2.0; // 圆形弹幕半径
    private static final int DANMAKU_COUNT = 9; // 每次发射的弹幕数量（增加到9）
    private static final double ROTATION_INCREMENT = Math.toRadians(30); // 每次旋转30度（增加角度）
    private static final int SHOOT_INTERVAL = 5; // 发射间隔（ticks）
    private static final double DANMAKU_SPEED = 0.1; // 弹幕初始速度（更慢）
    private static final double DANMAKU_ACCELERATION_POWER = 0.3; // 弹幕常态速度（更快）
    private static final double ROTATION_SPEED = Math.toRadians(5); // 围绕目标旋转速度（每tick旋转5度）
    private static final double ORBIT_RADIUS = 10.0; // 围绕半径
    private static final double PLAYER_LOCK_RANGE = 128.0; // 玩家锁定范围（方块）
    
    // NBT标签键
    public static final String CURRENT_ANGLE_KEY = "NoSpell2CurrentAngle";
    public static final String IS_CLOCKWISE_KEY = "NoSpell2IsClockwise";
    public static final String LAST_SHOOT_TIME_KEY = "NoSpell2LastShootTime";
    public static final String SHOOT_COUNTER_KEY = "NoSpell2ShootCounter";
    public static final String ORBIT_ANGLE_KEY = "NoSpell2OrbitAngle";
    public static final String TARGET_POS_X_KEY = "NoSpell2TargetX";
    public static final String TARGET_POS_Y_KEY = "NoSpell2TargetY";
    public static final String TARGET_POS_Z_KEY = "NoSpell2TargetZ";
    public static final String ATTACK_ELAPSED_SECONDS_KEY = "NoSpell2ElapsedSeconds";
    public static final String ATTACK_PAUSED_KEY = "NoSpell2Paused";
    
    /**
     * 开始NoSpell2攻击
     * 
     * @param boss Watari Nina实体
     * @param target 目标玩家
     */
    public static void startAttack(LivingEntity boss, Player target) {
        if (!(boss.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        
        // 瞬移到目标位置
        teleportToTarget(boss, target);
        
        // 初始化攻击参数
        initializeAttackParameters(boss, target);
        
        // 设置攻击状态
        AttackManager.setAttacking(boss, true);
        AttackManager.setAttackStartTime(boss, serverLevel.getGameTime());
        
        // 开始周期性发射
        scheduleNextShot(boss, target, serverLevel);
    }
    
    /**
     * 瞬移到目标附近
     * 
     * @param boss Watari Nina实体
     * @param target 目标玩家
     */
    private static void teleportToTarget(LivingEntity boss, Player target) {
        // 计算从Boss指向玩家的方向向量
        Vec3 direction = target.position().subtract(boss.position()).normalize();
        
        // 计算瞬移位置：玩家位置 - 方向 * 距离
        Vec3 teleportPos = target.position().subtract(direction.scale(TELEPORT_DISTANCE));
        
        // 寻找合适的地面高度
        BlockPos groundPos = findGroundPosition(boss.level(), teleportPos);
        double targetY = groundPos.getY() + HEIGHT_ABOVE_GROUND;
        
        // 执行瞬移
        boss.moveTo(teleportPos.x(), targetY, teleportPos.z(), 
                   boss.getYRot(), boss.getXRot());
        boss.hurtMarked = true;
    }
    
    /**
     * 寻找合适的地面位置
     * 
     * @param level 世界
     * @param pos 目标位置
     * @return 地面位置
     */
    private static BlockPos findGroundPosition(net.minecraft.world.level.Level level, Vec3 pos) {
        BlockPos blockPos = BlockPos.containing(pos);
        
        // 向下寻找固体方块
        while (blockPos.getY() > level.getMinBuildHeight() && 
               level.isEmptyBlock(blockPos)) {
            blockPos = blockPos.below();
        }
        
        // 如果找到了固体方块，返回其上方位置
        if (!level.isEmptyBlock(blockPos)) {
            return blockPos.above();
        }
        
        // 如果没找到，返回原始Y坐标
        return BlockPos.containing(pos.x(), 64, pos.z());
    }
    
    /**
     * 初始化攻击参数
     * 
     * @param boss Watari Nina实体
     * @param target 目标玩家
     */
    private static void initializeAttackParameters(LivingEntity boss, Player target) {
        boss.getPersistentData().putDouble(CURRENT_ANGLE_KEY, 0.0);
        boss.getPersistentData().putBoolean(IS_CLOCKWISE_KEY, false); // 初始为逆时针
        boss.getPersistentData().putLong(LAST_SHOOT_TIME_KEY, 0L);
        boss.getPersistentData().putInt(SHOOT_COUNTER_KEY, 0);
        boss.getPersistentData().putDouble(ORBIT_ANGLE_KEY, 0.0); // 初始轨道角度
        
        // 保存目标位置
        Vec3 targetPos = target.position();
        boss.getPersistentData().putDouble(TARGET_POS_X_KEY, targetPos.x);
        boss.getPersistentData().putDouble(TARGET_POS_Y_KEY, targetPos.y);
        boss.getPersistentData().putDouble(TARGET_POS_Z_KEY, targetPos.z);
    }
    
    /**
     * 调度下一次射击
     * 
     * @param boss Watari Nina实体
     * @param target 目标玩家
     * @param serverLevel 服务器世界
     */
    private static void scheduleNextShot(LivingEntity boss, Player target, ServerLevel serverLevel) {
        if (!(boss.level() instanceof ServerLevel)) {
            return;
        }
        
        // 立即执行当前射击
        if (!boss.isRemoved() && !target.isRemoved() && AttackManager.isAttacking(boss)) {
            // 更新Boss围绕目标的位置
            updateOrbitPosition(boss, target);
            
            // 发射Danmaku弹幕
            shootDanmaku(boss, target);
            
            // 检查是否应该继续攻击
            long currentTime = serverLevel.getGameTime();
            if (!shouldEndAttack(boss, currentTime)) {
                // 使用项目的标准延迟调度方法
                scheduleDelayedShot(boss, target, serverLevel);
            } else {
                // 结束攻击
                endAttack(boss);
            }
        } else {
            endAttack(boss);
        }
    }
    
    /**
     * 延迟调度下一次射击
     * 
     * @param boss Watari Nina实体
     * @param target 目标玩家
     * @param serverLevel 服务器世界
     */
    private static void scheduleDelayedShot(LivingEntity boss, Player target, ServerLevel serverLevel) {
        net.everla.everlaartifacts.EverlaartifactsMod.queueServerWork(SHOOT_INTERVAL, () -> {
            if (!boss.isRemoved() && !target.isRemoved() && AttackManager.isAttacking(boss)) {
                int currentAttack = AttackManager.getAttack(boss);
                if (currentAttack == 2) {
                    scheduleNextShot(boss, target, serverLevel);
                } else {
                    endAttack(boss);
                }
            } else {
                endAttack(boss);
            }
        });
    }
    
    /**
     * 发射Danmaku弹幕
     * 
     * @param boss Watari Nina实体
     * @param target 目标玩家
     */
    private static void shootDanmaku(LivingEntity boss, Player target) {
        // 更新旋转角度和方向
        updateRotationParameters(boss);
        
        // 获取当前参数
        double currentAngle = boss.getPersistentData().getDouble(CURRENT_ANGLE_KEY);
        boolean isClockwise = boss.getPersistentData().getBoolean(IS_CLOCKWISE_KEY);
        
        // 每次发射都重新获取最近的有效玩家并计算朝向
        Player nearestPlayer = getNearestValidPlayer(boss.level(), boss);
        if (nearestPlayer == null) {
            // 如果找不到有效玩家，检查是否应该暂停攻击
            if (shouldPauseAttack(boss)) {
                pauseAttack(boss);
                return;
            }
            nearestPlayer = target; // 如果找不到最近玩家，使用原目标
        } else {
            // 有有效目标，恢复攻击
            resumeAttack(boss);
        }
        
        // 更新Boss实体朝向，使其面向最近玩家
        updateBossOrientation(boss, nearestPlayer);
        
        // 计算Boss到玩家的方向向量
        Vec3 bossToPlayer = nearestPlayer.position().subtract(boss.position()).normalize();
        
        // 创建圆盘的坐标系
        Vec3 diskNormal = calculateDiskNormal(bossToPlayer);
        
        // 创建圆盘的两个正交基向量（在圆盘平面内）
        Vec3 diskRight, diskUp;
        if (Math.abs(diskNormal.y()) < 0.9) {
            // 使用Y轴作为参考创建右向量
            diskRight = diskNormal.cross(new Vec3(0, 1, 0)).normalize();
        } else {
            // 使用X轴作为参考创建右向量
            diskRight = diskNormal.cross(new Vec3(1, 0, 0)).normalize();
        }
        // 上向量 = 法向量 × 右向量
        diskUp = diskNormal.cross(diskRight).normalize();
        
        // 生成9枚Danmaku弹幕，均匀分布在圆周上
        for (int i = 0; i < DANMAKU_COUNT; i++) {
            // 计算在圆周上的基础角度（相对于圆盘）
            double circleAngle = (2 * Math.PI * i) / DANMAKU_COUNT;
            
            // 应用全局旋转（整个圆盘的旋转）
            double rotatedAngle = circleAngle + currentAngle;
            
            // 计算圆盘上的点位置（在圆盘平面上）
            Vec3 diskPoint = new Vec3(
                Math.cos(rotatedAngle) * CIRCLE_RADIUS,
                0,  // 在圆盘平面上
                Math.sin(rotatedAngle) * CIRCLE_RADIUS
            );
            
            // 将圆盘点转换到世界坐标系（应用圆盘朝向）
            Vec3 worldPosition = boss.position().add(
                diskRight.scale(diskPoint.x()).add(diskUp.scale(diskPoint.z()))
            );
            
            // 计算切线方向（相对于旋转后的圆盘）
            Vec3 tangentLocal = new Vec3(
                -Math.sin(rotatedAngle),  // 切线X分量
                0,                        // 切线Y分量
                Math.cos(rotatedAngle)    // 切线Z分量
            );
            
            // 将局部切线方向转换到世界坐标系
            Vec3 tangentWorld = diskRight.scale(tangentLocal.x()).add(diskUp.scale(tangentLocal.z()));
            
            // 根据旋转方向调整切线方向
            if (!isClockwise) {
                tangentWorld = tangentWorld.reverse();
            }
            
            // 发射Danmaku弹幕（使用最新获取的最近玩家位置）
            DanmakuEntity danmaku = DanmakuEntity.shootFromPosition(
                boss.level(),
                worldPosition.x(), worldPosition.y(), worldPosition.z(),
                tangentWorld.x() * DANMAKU_SPEED,
                tangentWorld.y() * DANMAKU_SPEED,
                tangentWorld.z() * DANMAKU_SPEED
            );
            
            // 设置Danmaku的特殊属性
            if (danmaku != null) {
                danmaku.getPersistentData().putBoolean("IsNoSpell2Danmaku", true);
                danmaku.setAccelerationPower(DANMAKU_ACCELERATION_POWER); // 设置较高的加速度
                danmaku.setConstantVelocity(
                    tangentWorld.x() * DANMAKU_SPEED,
                    tangentWorld.y() * DANMAKU_SPEED,
                    tangentWorld.z() * DANMAKU_SPEED
                );
            }
        }
        
        // 更新射击计数
        int shootCount = boss.getPersistentData().getInt(SHOOT_COUNTER_KEY);
        boss.getPersistentData().putInt(SHOOT_COUNTER_KEY, shootCount + 1);
    }
    
    /**
     * 获取最近的有效玩家（排除创造模式和旁观模式，限制128方块范围内）
     * 
     * @param level 世界
     * @param entity 参考实体
     * @return 最近的有效玩家，如果不存在则返回null
     */
    private static Player getNearestValidPlayer(net.minecraft.world.level.Level level, LivingEntity entity) {
        Player nearestPlayer = null;
        double nearestDistance = Double.MAX_VALUE;
        double maxRangeSqr = PLAYER_LOCK_RANGE * PLAYER_LOCK_RANGE; // 范围平方，用于比较
        
        for (Player player : level.players()) {
            // 排除死亡、创造模式、旁观模式的玩家
            if (player.isAlive() && !player.isCreative() && !player.isSpectator()) {
                double distance = player.distanceToSqr(entity);
                // 只考虑128方块范围内的玩家
                if (distance <= maxRangeSqr && distance < nearestDistance) {
                    nearestDistance = distance;
                    nearestPlayer = player;
                }
            }
        }
        
        return nearestPlayer;
    }
    
    /**
     * 计算圆盘法向量（垂直于发射平面）
     * 发射平面以玩家-Boss连线为轴旋转90度
     * 
     * @param bossToPlayer Boss到目标玩家的方向向量
     * @return 圆盘法向量（垂直于发射平面）
     */
    private static Vec3 calculateDiskNormal(Vec3 bossToPlayer) {
        // 原来的法向量（垂直于连线方向）
        Vec3 originalNormal = calculateOriginalNormal(bossToPlayer);
        
        // 以玩家-Boss连线为轴旋转90度
        double theta = Math.toRadians(90); // 90度
        double cosTheta = Math.cos(theta);
        double sinTheta = Math.sin(theta);
        
        // 计算点积 k·v
        double dotProduct = bossToPlayer.dot(originalNormal);
        
        // 计算叉积 k×v
        Vec3 crossProduct = bossToPlayer.cross(originalNormal);
        
        // 应用罗德里格斯公式
        Vec3 rotatedNormal = new Vec3(
            originalNormal.x * cosTheta + crossProduct.x * sinTheta + bossToPlayer.x * dotProduct * (1 - cosTheta),
            originalNormal.y * cosTheta + crossProduct.y * sinTheta + bossToPlayer.y * dotProduct * (1 - cosTheta),
            originalNormal.z * cosTheta + crossProduct.z * sinTheta + bossToPlayer.z * dotProduct * (1 - cosTheta)
        );
        
        return rotatedNormal.normalize();
    }
    
    /**
     * 计算原始法向量（垂直于Boss-玩家连线）
     * 
     * @param bossToPlayer Boss到目标玩家的方向向量
     * @return 原始法向量
     */
    private static Vec3 calculateOriginalNormal(Vec3 bossToPlayer) {
        Vec3 referenceVector;
        if (Math.abs(bossToPlayer.y()) < 0.9) {
            // 如果连线方向不接近垂直，使用Y轴作为参考
            referenceVector = new Vec3(0, 1, 0);
        } else {
            // 如果连线方向接近垂直，使用X轴作为参考
            referenceVector = new Vec3(1, 0, 0);
        }
        
        // 计算法向量：bossToPlayer × referenceVector
        return bossToPlayer.cross(referenceVector).normalize();
    }
    
    /**
     * 更新Boss实体朝向，使其面向指定玩家
     * 
     * @param boss Watari Nina实体
     * @param target 目标玩家
     */
    private static void updateBossOrientation(LivingEntity boss, Player target) {
        // 计算从Boss到玩家的方向向量
        Vec3 direction = target.position().subtract(boss.position());
        
        // 计算水平方向的角度（Yaw）
        double yaw = Math.atan2(direction.z, direction.x) * (180.0 / Math.PI) - 90.0;
        
        // 计算垂直方向的角度（Pitch）
        double horizontalDistance = Math.sqrt(direction.x * direction.x + direction.z * direction.z);
        double pitch = -Math.atan2(direction.y, horizontalDistance) * (180.0 / Math.PI);
        
        // 限制Pitch角度在合理范围内
        pitch = Math.max(-90.0, Math.min(90.0, pitch));
        
        // 更新Boss的朝向
        boss.setYRot((float) yaw);
        boss.setXRot((float) pitch);
        
        // 标记位置已更新
        boss.hurtMarked = true;
        
        // 如果Boss有身体部件，也需要更新它们的朝向
        if (boss instanceof net.minecraft.world.entity.Mob mob) {
            mob.yBodyRot = (float) yaw;
            mob.yHeadRot = (float) yaw;
        }
    }
    
    /**
     * 检查是否应该暂停攻击（无有效目标时）
     * 
     * @param boss Watari Nina实体
     * @return 是否应该暂停攻击
     */
    private static boolean shouldPauseAttack(LivingEntity boss) {
        // 检查是否已经有暂停标记
        return boss.getPersistentData().getBoolean(ATTACK_PAUSED_KEY);
    }
    
    /**
     * 暂停攻击并保存当前进度
     * 
     * @param boss Watari Nina实体
     */
    private static void pauseAttack(LivingEntity boss) {
        // 设置暂停状态
        boss.getPersistentData().putBoolean(ATTACK_PAUSED_KEY, true);
        
        // 保存当前已进行的秒数
        long startTime = AttackManager.getAttackStartTime(boss);
        long currentTime = boss.level().getGameTime();
        int elapsedSeconds = (int) ((currentTime - startTime) / 20); // 转换为秒
        boss.getPersistentData().putInt(ATTACK_ELAPSED_SECONDS_KEY, elapsedSeconds);
        
        LOGGER.info("NoSpell2攻击暂停，已进行{}秒", elapsedSeconds);
    }
    
    /**
     * 恢复攻击
     * 
     * @param boss Watari Nina实体
     */
    private static void resumeAttack(LivingEntity boss) {
        // 清除暂停状态
        boss.getPersistentData().putBoolean(ATTACK_PAUSED_KEY, false);
        
        LOGGER.info("NoSpell2攻击恢复");
    }
    
    /**
     * 更新Boss围绕目标的轨道位置
     * 
     * @param boss Watari Nina实体
     * @param target 目标玩家
     */
    private static void updateOrbitPosition(LivingEntity boss, Player target) {
        // 获取当前轨道角度
        double orbitAngle = boss.getPersistentData().getDouble(ORBIT_ANGLE_KEY);
        
        // 获取目标位置
        double targetX = boss.getPersistentData().getDouble(TARGET_POS_X_KEY);
        double targetY = boss.getPersistentData().getDouble(TARGET_POS_Y_KEY);
        double targetZ = boss.getPersistentData().getDouble(TARGET_POS_Z_KEY);
        
        // 逆时针旋转（角度增加）
        orbitAngle += ROTATION_SPEED;
        
        // 计算新的Boss位置（围绕目标逆时针旋转）
        double newX = targetX + Math.cos(orbitAngle) * ORBIT_RADIUS;
        double newZ = targetZ + Math.sin(orbitAngle) * ORBIT_RADIUS;
        double newY = targetY + HEIGHT_ABOVE_GROUND;
        
        // 寻找合适的地面高度
        BlockPos groundPos = findGroundPosition(boss.level(), new Vec3(newX, newY, newZ));
        newY = groundPos.getY() + HEIGHT_ABOVE_GROUND;
        
        // 更新Boss位置
        boss.moveTo(newX, newY, newZ, boss.getYRot(), boss.getXRot());
        boss.hurtMarked = true;
        
        // 更新轨道角度
        boss.getPersistentData().putDouble(ORBIT_ANGLE_KEY, orbitAngle);
        
        // 更新目标位置（如果玩家移动了）
        Vec3 currentTargetPos = target.position();
        boss.getPersistentData().putDouble(TARGET_POS_X_KEY, currentTargetPos.x);
        boss.getPersistentData().putDouble(TARGET_POS_Y_KEY, currentTargetPos.y);
        boss.getPersistentData().putDouble(TARGET_POS_Z_KEY, currentTargetPos.z);
    }
    
    /**
     * 更新旋转参数
     * 
     * @param boss Watari Nina实体
     */
    private static void updateRotationParameters(LivingEntity boss) {
        double currentAngle = boss.getPersistentData().getDouble(CURRENT_ANGLE_KEY);
        boolean isClockwise = boss.getPersistentData().getBoolean(IS_CLOCKWISE_KEY);
        
        // 更新角度
        currentAngle += ROTATION_INCREMENT;
        
        // 切换旋转方向
        isClockwise = !isClockwise;
        
        // 保存更新后的参数
        boss.getPersistentData().putDouble(CURRENT_ANGLE_KEY, currentAngle);
        boss.getPersistentData().putBoolean(IS_CLOCKWISE_KEY, isClockwise);
    }
    
    /**
     * 检查攻击是否应该结束
     * 
     * @param boss Watari Nina实体
     * @param currentTime 当前时间
     * @return 是否应该结束攻击
     */
    public static boolean shouldEndAttack(LivingEntity boss, long currentTime) {
        return AttackManager.isAttackTimedOut(boss, currentTime);
    }
    
    /**
     * 结束NoSpell2攻击
     * 
     * @param boss Watari Nina实体
     */
    public static void endAttack(LivingEntity boss) {
        // 设置attack值为3
        AttackManager.setAttack(boss, 3);
        AttackManager.setAttacking(boss, false);
        
        // 攻击结束时回满血
        healBossToFullHealth(boss);
        
        // 清理攻击参数
        boss.getPersistentData().remove(CURRENT_ANGLE_KEY);
        boss.getPersistentData().remove(IS_CLOCKWISE_KEY);
        boss.getPersistentData().remove(LAST_SHOOT_TIME_KEY);
        boss.getPersistentData().remove(SHOOT_COUNTER_KEY);
        boss.getPersistentData().remove(ORBIT_ANGLE_KEY);
        boss.getPersistentData().remove(TARGET_POS_X_KEY);
        boss.getPersistentData().remove(TARGET_POS_Y_KEY);
        boss.getPersistentData().remove(TARGET_POS_Z_KEY);
        boss.getPersistentData().remove(ATTACK_ELAPSED_SECONDS_KEY);
        boss.getPersistentData().remove(ATTACK_PAUSED_KEY);
    }
    
    /**
     * 将Boss回满血
     * 
     * @param boss Watari Nina实体
     */
    private static void healBossToFullHealth(LivingEntity boss) {
        // 获取Boss的最大生命值
        float maxHealth = boss.getMaxHealth();
        
        // 设置当前生命值为最大值
        boss.setHealth(maxHealth);
        
        LOGGER.info("Boss血量已回满: {}/{}", boss.getHealth(), maxHealth);
    }
}