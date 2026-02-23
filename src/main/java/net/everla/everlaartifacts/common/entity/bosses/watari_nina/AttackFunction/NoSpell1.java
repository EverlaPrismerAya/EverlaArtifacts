package net.everla.everlaartifacts.common.entity.bosses.watari_nina.AttackFunction;

import net.everla.everlaartifacts.common.entity.projectiles.BlackWitherSkullEntity;
import net.everla.everlaartifacts.common.entity.bosses.watari_nina.AttackManager;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

/**
 * NoSpell1攻击实现
 * 攻击机制：BOSS瞬移到玩家附近距玩家10方块，距地面3方块的随机位置，
 * 于最近玩家-boss为点连线，以boss为圆心形成的半径为4的圆产生6枚均匀分布的
 * 由boss发射的黑色凋灵之首，随后沿切线的逆时针方向飞出。
 * 下次发射转为顺时针且发射角度旋转20度，以此类推，持续30秒。
 */
public class NoSpell1 {
    
    // 攻击参数常量
    private static final double TELEPORT_DISTANCE = 10.0; // 瞬移距离
    private static final double HEIGHT_ABOVE_GROUND = 3.0; // 距地面高度
    private static final double CIRCLE_RADIUS = 4.0; // 圆形弹幕半径
    private static final int SKULL_COUNT = 6; // 每次发射的凋灵之首数量
    private static final double ROTATION_INCREMENT = Math.toRadians(20); // 每次旋转20度
    private static final int SHOOT_INTERVAL = 10; // 发射间隔（ticks）
    private static final double SKULL_SPEED = 0.8; // 凋灵之首速度
    
    // NBT标签键
    public static final String CURRENT_ANGLE_KEY = "NoSpell1CurrentAngle";
    public static final String IS_CLOCKWISE_KEY = "NoSpell1IsClockwise";
    public static final String LAST_SHOOT_TIME_KEY = "NoSpell1LastShootTime";
    public static final String SHOOT_COUNTER_KEY = "NoSpell1ShootCounter";
    
    /**
     * 开始NoSpell1攻击
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
        initializeAttackParameters(boss);
        
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
     */
    private static void initializeAttackParameters(LivingEntity boss) {
        boss.getPersistentData().putDouble(CURRENT_ANGLE_KEY, 0.0);
        boss.getPersistentData().putBoolean(IS_CLOCKWISE_KEY, false); // 初始为逆时针
        boss.getPersistentData().putLong(LAST_SHOOT_TIME_KEY, 0L);
        boss.getPersistentData().putInt(SHOOT_COUNTER_KEY, 0);
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
        
        serverLevel.getServer().tell(new net.minecraft.server.TickTask(
            serverLevel.getServer().getTickCount() + SHOOT_INTERVAL,
            () -> {
                if (!boss.isRemoved() && !target.isRemoved() && AttackManager.isAttacking(boss)) {
                    // 发射凋灵之首
                    shootWitherSkulls(boss, target);
                    
                    // 调度下一次射击
                    scheduleNextShot(boss, target, serverLevel);
                }
            }
        ));
    }
    
    /**
     * 发射凋灵之首弹幕
     * 
     * @param boss Watari Nina实体
     * @param target 目标玩家
     */
    private static void shootWitherSkulls(LivingEntity boss, Player target) {
        // 更新旋转角度和方向
        updateRotationParameters(boss);
        
        // 获取当前参数
        double currentAngle = boss.getPersistentData().getDouble(CURRENT_ANGLE_KEY);
        boolean isClockwise = boss.getPersistentData().getBoolean(IS_CLOCKWISE_KEY);
        
        // 计算Boss到玩家的方向向量
        Vec3 bossToPlayer = target.position().subtract(boss.position()).normalize();
        
        // 生成6枚凋灵之首，均匀分布在圆周上
        for (int i = 0; i < SKULL_COUNT; i++) {
            // 计算在圆周上的角度
            double circleAngle = (2 * Math.PI * i) / SKULL_COUNT;
            
            // 计算切线方向
            Vec3 tangentDirection = calculateTangentDirection(bossToPlayer, circleAngle, isClockwise);
            
            // 计算发射位置（Boss位置 + 圆周上的点）
            Vec3 spawnOffset = new Vec3(
                Math.cos(circleAngle) * CIRCLE_RADIUS,
                0,
                Math.sin(circleAngle) * CIRCLE_RADIUS
            );
            Vec3 spawnPos = boss.position().add(spawnOffset);
            
            // 发射凋灵之首
            BlackWitherSkullEntity.shootFromPosition(
                boss.level(),
                spawnPos.x(), spawnPos.y(), spawnPos.z(),
                tangentDirection.x() * SKULL_SPEED,
                tangentDirection.y() * SKULL_SPEED,
                tangentDirection.z() * SKULL_SPEED
            );
        }
        
        // 更新射击计数
        int shootCount = boss.getPersistentData().getInt(SHOOT_COUNTER_KEY);
        boss.getPersistentData().putInt(SHOOT_COUNTER_KEY, shootCount + 1);
    }
    
    /**
     * 计算切线方向
     * 
     * @param baseDirection 基础方向向量
     * @param circleAngle 圆周角度
     * @param clockwise 是否顺时针
     * @return 切线方向向量
     */
    private static Vec3 calculateTangentDirection(Vec3 baseDirection, double circleAngle, boolean clockwise) {
        // 创建垂直于基础方向的向量
        Vec3 perpendicular;
        if (Math.abs(baseDirection.y()) < 0.9) {
            // 基础方向主要在水平面，使用Y轴作为参考
            perpendicular = baseDirection.cross(new Vec3(0, 1, 0)).normalize();
        } else {
            // 基础方向接近垂直，使用X轴作为参考
            perpendicular = baseDirection.cross(new Vec3(1, 0, 0)).normalize();
        }
        
        // 根据旋转角度调整切线方向
        double rotationAngle = clockwise ? circleAngle : -circleAngle;
        
        // 旋转perpendicular向量
        double cosAngle = Math.cos(rotationAngle);
        double sinAngle = Math.sin(rotationAngle);
        
        Vec3 rotated = new Vec3(
            perpendicular.x() * cosAngle - perpendicular.z() * sinAngle,
            perpendicular.y(),
            perpendicular.x() * sinAngle + perpendicular.z() * cosAngle
        );
        
        return rotated.normalize();
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
     * 结束NoSpell1攻击
     * 
     * @param boss Watari Nina实体
     */
    public static void endAttack(LivingEntity boss) {
        AttackManager.setAttacking(boss, false);
        
        // 清理攻击参数
        boss.getPersistentData().remove(CURRENT_ANGLE_KEY);
        boss.getPersistentData().remove(IS_CLOCKWISE_KEY);
        boss.getPersistentData().remove(LAST_SHOOT_TIME_KEY);
        boss.getPersistentData().remove(SHOOT_COUNTER_KEY);
    }
}
