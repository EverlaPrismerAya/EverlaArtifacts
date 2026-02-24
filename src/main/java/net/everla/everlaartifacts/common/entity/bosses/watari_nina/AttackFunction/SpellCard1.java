package net.everla.everlaartifacts.common.entity.bosses.watari_nina.AttackFunction;


import net.everla.everlaartifacts.common.entity.bosses.watari_nina.AttackManager;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.everla.everlaartifacts.common.entity.projectiles.DanmakuEntity;
import org.joml.Vector3f;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * SpellCard1符卡攻击实现
 * 攻击机制：在玩家脚下生成警戒线，随后产生光柱并生成环绕的蓝色Danmaku弹幕
 * 持续时间：60秒
 * 触发条件：Boss的Attack值为1时执行
 */
public class SpellCard1 {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(SpellCard1.class);
    
    // 攻击参数常量
    private static final int DURATION_SECONDS = 60; // 持续时间（秒）
    private static final int ATTACK_INTERVAL = 20; // 攻击间隔（刻）
    private static final double WARNING_RANGE = 3.0; // 警戒线范围（方块）
    private static final double LIGHT_PILLAR_RADIUS = 1.5; // 光柱半径（增大）
    private static final float LIGHT_PILLAR_DAMAGE = 32.0F; // 光柱伤害
    private static final int LIGHT_PILLAR_PARTICLE_INTERVAL = 5; // 光柱粒子显示间隔（刻）
    private static final int LIGHT_PILLAR_DAMAGE_INTERVAL = 10; // 光柱伤害判断间隔（刻）
    private static final int LIGHT_PILLAR_LIFETIME = 40; // 光柱存在时间（刻）
    private static final int WARNING_DELAY_TICKS = 15; // 预警时间（0.75秒 = 15刻）
    private static final double PILLAR_SPAWN_RANGE = 5.0; // 光柱生成范围（方块）
    private static final float ORBITING_DANMAKU_DAMAGE = 16.0F; // 环绕弹幕伤害
    private static final int ORBITING_DANMAKU_LIFETIME = 100; // 环绕弹幕存在时间（刻）
    private static final int MIN_ORBIT_RADIUS = 2; // 最小环绕半径
    private static final int MAX_ORBIT_RADIUS = 4; // 最大环绕半径
    private static final int DANMAKU_COUNT_PER_WAVE = 3; // 每波生成的弹幕数量
    private static final double SPIRAL_DESCENT_SPEED = 0.1; // 螺旋下降速度
    private static final double ROTATION_SPEED_PER_TICK = 0.3; // 每刻旋转速度（弧度）
    private static final double HEIGHT_DECREMENT_PER_TICK = 0.05; // 每刻下降高度
    
    // NBT标签键
    public static final String SPELL_START_TIME_KEY = "SpellCard1StartTime";
    public static final String LAST_ATTACK_TIME_KEY = "SpellCard1LastAttackTime";
    public static final String ATTACK_WAVE_KEY = "SpellCard1AttackWave";
    public static final String BOSS_FLOATING_HEIGHT_KEY = "SpellCard1FloatingHeight";
    
    // 弹幕螺旋下降相关标签键
    public static final String SPIRAL_CURRENT_ANGLE_KEY = "SpellCard1SpiralAngle";
    public static final String SPIRAL_CURRENT_HEIGHT_KEY = "SpellCard1SpiralHeight";
    public static final String SPIRAL_CENTER_X_KEY = "SpellCard1SpiralCenterX";
    public static final String SPIRAL_CENTER_Y_KEY = "SpellCard1SpiralCenterY";
    public static final String SPIRAL_CENTER_Z_KEY = "SpellCard1SpiralCenterZ";
    
    // 粒子颜色配置 - 淡蓝色光柱
    private static final DustParticleOptions BLUE_DUST = new DustParticleOptions(new Vector3f(0.5f, 0.8f, 1.0f), 1.5f);
    private static final DustParticleOptions RED_DUST = new DustParticleOptions(new Vector3f(1.0f, 0.5f, 0.5f), 1.0f);
    
    /**
     * 开始SpellCard1攻击
     * 
     * @param boss Watari Nina实体
     * @param target 目标玩家
     */
    public static void startAttack(LivingEntity boss, Player target) {
        if (!(boss.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        
        // 将Boss悬浮到玩家头顶5方块处
        teleportBossAbovePlayer(boss, target);
        
        // 初始化攻击参数
        initializeAttackParameters(boss, serverLevel);
        
        // 设置攻击状态
        AttackManager.setAttacking(boss, true);
        AttackManager.setAttackStartTime(boss, serverLevel.getGameTime());
        
        // 开始周期性攻击
        scheduleNextAttack(boss, target, serverLevel);
    }
    
    /**
     * 将Boss悬浮到玩家头顶指定高度
     * 
     * @param boss Watari Nina实体
     * @param target 目标玩家
     */
    private static void teleportBossAbovePlayer(LivingEntity boss, Player target) {
        Vec3 playerPos = target.position();
        double floatingHeight = 5.0; // 悬浮高度为5方块
        
        // 计算悬浮位置
        double newX = playerPos.x;
        double newY = playerPos.y + floatingHeight;
        double newZ = playerPos.z;
        
        // 执行瞬移
        boss.moveTo(newX, newY, newZ, boss.getYRot(), boss.getXRot());
        boss.hurtMarked = true;
        
        // 保存悬浮高度信息
        boss.getPersistentData().putDouble(BOSS_FLOATING_HEIGHT_KEY, floatingHeight);
        
        LOGGER.debug("Boss已悬浮到玩家头顶{}方块处", floatingHeight);
    }
    
    /**
     * 初始化攻击参数
     * 
     * @param boss Watari Nina实体
     * @param serverLevel 服务器世界
     */
    private static void initializeAttackParameters(LivingEntity boss, ServerLevel serverLevel) {
        boss.getPersistentData().putLong(SPELL_START_TIME_KEY, serverLevel.getGameTime());
        boss.getPersistentData().putLong(LAST_ATTACK_TIME_KEY, 0L);
        boss.getPersistentData().putInt(ATTACK_WAVE_KEY, 0);
    }
    
    /**
     * 调度下一次攻击
     * 
     * @param boss Watari Nina实体
     * @param target 目标玩家
     * @param serverLevel 服务器世界
     */
    private static void scheduleNextAttack(LivingEntity boss, Player target, ServerLevel serverLevel) {
        if (!(boss.level() instanceof ServerLevel)) {
            return;
        }
        
        // 立即执行当前攻击
        if (!boss.isRemoved() && !target.isRemoved() && AttackManager.isAttacking(boss)) {
            // 执行攻击逻辑
            executeAttackWave(boss, target, serverLevel);
            
            // 检查是否应该继续攻击
            long currentTime = serverLevel.getGameTime();
            if (!shouldEndAttack(boss, currentTime)) {
                scheduleDelayedAttack(boss, target, serverLevel);
            } else {
                endAttack(boss);
            }
        } else {
            endAttack(boss);
        }
    }
    
    /**
     * 延迟调度下一次攻击
     * 
     * @param boss Watari Nina实体
     * @param target 目标玩家
     * @param serverLevel 服务器世界
     */
    private static void scheduleDelayedAttack(LivingEntity boss, Player target, ServerLevel serverLevel) {
        net.everla.everlaartifacts.EverlaartifactsMod.queueServerWork(ATTACK_INTERVAL, () -> {
            if (!boss.isRemoved() && !target.isRemoved() && AttackManager.isAttacking(boss)) {
                int currentAttack = AttackManager.getAttack(boss);
                if (currentAttack == 1) {
                    scheduleNextAttack(boss, target, serverLevel);
                } else {
                    endAttack(boss);
                }
            } else {
                endAttack(boss);
            }
        });
    }
    
    /**
     * 执行攻击波
     * 
     * @param boss Watari Nina实体
     * @param target 目标玩家
     * @param serverLevel 服务器世界
     */
    private static void executeAttackWave(LivingEntity boss, Player target, ServerLevel serverLevel) {
        // 更新攻击波数
        int attackWave = boss.getPersistentData().getInt(ATTACK_WAVE_KEY);
        boss.getPersistentData().putInt(ATTACK_WAVE_KEY, attackWave + 1);
        
        // 每次攻击波都将Boss瞬移至玩家头顶
        teleportBossAbovePlayer(boss, target);
        
        // 在玩家周围半径3的圆内随机选择预警位置
        Vec3 warningPos = getRandomPositionInWarningCircle(target, serverLevel);
        
        // 显示预警粒子效果
        showWarningParticlesAtPosition(warningPos, serverLevel);
        
        // 延迟生成光柱（15刻预警时间）
        scheduleDelayedLightPillar(warningPos, serverLevel, boss, attackWave);
        
        // 生成环绕的蓝色弹幕
        spawnOrbitingSkulls(warningPos, serverLevel, attackWave);
    }
    
    /**
     * 调度延迟光柱生成
     * 
     * @param warningPos 预警位置
     * @param serverLevel 服务器世界
     * @param boss Boss实体
     * @param attackWave 攻击波数
     */
    private static void scheduleDelayedLightPillar(Vec3 warningPos, ServerLevel serverLevel, LivingEntity boss, int attackWave) {
        net.everla.everlaartifacts.EverlaartifactsMod.queueServerWork(WARNING_DELAY_TICKS, () -> {
            if (!boss.isRemoved() && AttackManager.isAttacking(boss)) {
                // 在预警位置生成光柱
                startLightPillarProcessing(warningPos, serverLevel, boss);
            }
        });
    }
    
    /**
     * 获取玩家周围预警圆内的随机位置（半径3）
     * 
     * @param player 目标玩家
     * @param serverLevel 服务器世界
     * @return 预警位置
     */
    private static Vec3 getRandomPositionInWarningCircle(Player player, ServerLevel serverLevel) {
        RandomSource random = serverLevel.random;
        Vec3 playerPos = player.position();
        
        // 在玩家周围半径3的圆内生成随机位置
        double angle = random.nextDouble() * 2 * Math.PI;
        double distance = random.nextDouble() * WARNING_RANGE; // 使用WARNING_RANGE = 3.0
        
        double x = playerPos.x + Math.cos(angle) * distance;
        double z = playerPos.z + Math.sin(angle) * distance;
        double y = playerPos.y; // 保持Y坐标与玩家相同
        
        return new Vec3(x, y, z);
    }
    
    /**
     * 获取玩家附近的随机位置（用于光柱生成）
     * 
     * @param player 目标玩家
     * @param serverLevel 服务器世界
     * @return 随机位置
     */
    private static Vec3 getRandomPositionNearPlayer(Player player, ServerLevel serverLevel) {
        RandomSource random = serverLevel.random;
        Vec3 playerPos = player.position();
        
        // 在玩家周围PILLAR_SPAWN_RANGE范围内生成随机位置
        double angle = random.nextDouble() * 2 * Math.PI;
        double distance = random.nextDouble() * PILLAR_SPAWN_RANGE;
        
        double x = playerPos.x + Math.cos(angle) * distance;
        double z = playerPos.z + Math.sin(angle) * distance;
        double y = playerPos.y; // 保持Y坐标与玩家相同
        
        return new Vec3(x, y, z);
    }
    
    /**
     * 在指定位置显示预警粒子效果
     * 
     * @param position 预警位置
     * @param serverLevel 服务器世界
     */
    private static void showWarningParticlesAtPosition(Vec3 position, ServerLevel serverLevel) {
        RandomSource random = serverLevel.random;
        
        // 在指定位置周围生成红色预警粒子
        for (int i = 0; i < 30; i++) {
            double angle = random.nextDouble() * 2 * Math.PI;
            double radius = 0.5 + random.nextDouble() * 2.5; // 半径0.5-3范围内
            
            double x = position.x + Math.cos(angle) * radius;
            double z = position.z + Math.sin(angle) * radius;
            double y = position.y + 0.1 + random.nextDouble() * 0.5; // 略高于地面
            
            serverLevel.sendParticles(
                RED_DUST,
                x, y, z,
                1,
                0, 0, 0,
                0
            );
        }
        
        LOGGER.debug("在位置{}显示预警粒子", position);
    }
    
    /**
     * 显示警戒线粒子效果（旧方法，保留兼容性）
     * 
     * @param target 目标玩家
     * @param serverLevel 服务器世界
     */
    private static void showWarningParticles(Player target, ServerLevel serverLevel) {
        Vec3 playerPos = target.position();
        RandomSource random = serverLevel.random;
        
        // 在玩家周围生成红色警戒线粒子
        for (int i = 0; i < 50; i++) {
            double angle = random.nextDouble() * 2 * Math.PI;
            double radius = WARNING_RANGE + (random.nextDouble() - 0.5) * 0.5; // 正负3格范围内
            
            double x = playerPos.x + Math.cos(angle) * radius;
            double z = playerPos.z + Math.sin(angle) * radius;
            double y = playerPos.y + 0.1; // 略高于地面
            
            serverLevel.sendParticles(
                RED_DUST,
                x, y, z,
                1,
                0, 0, 0,
                0
            );
        }
    }
    
    /**
     * 启动光柱处理（分离粒子和伤害逻辑）
     * 
     * @param position 光柱位置
     * @param serverLevel 服务器世界
     * @param boss Boss实体
     */
    private static void startLightPillarProcessing(Vec3 position, ServerLevel serverLevel, LivingEntity boss) {
        // 记录光柱生成时间
        long spawnTime = serverLevel.getGameTime();
        
        // 启动粒子显示循环（每5刻执行一次）
        scheduleLightPillarParticles(position, serverLevel, spawnTime, boss);
        
        // 启动伤害判断循环（每10刻执行一次）
        scheduleLightPillarDamage(position, serverLevel, spawnTime, boss);
    }
    
    /**
     * 调度光柱粒子显示
     * 
     * @param position 光柱位置
     * @param serverLevel 服务器世界
     * @param spawnTime 生成时间
     * @param boss Boss实体
     */
    private static void scheduleLightPillarParticles(Vec3 position, ServerLevel serverLevel, long spawnTime, LivingEntity boss) {
        net.everla.everlaartifacts.EverlaartifactsMod.queueServerWork(LIGHT_PILLAR_PARTICLE_INTERVAL, () -> {
            if (!boss.isRemoved() && AttackManager.isAttacking(boss)) {
                long currentTime = serverLevel.getGameTime();
                long lifetime = currentTime - spawnTime;
                
                // 如果光柱仍在生命周期内，继续显示粒子
                if (lifetime < LIGHT_PILLAR_LIFETIME) {
                    showLightPillarParticles(position, serverLevel);
                    // 继续调度下一次粒子显示
                    scheduleLightPillarParticles(position, serverLevel, spawnTime, boss);
                }
            }
        });
    }
    
    /**
     * 调度光柱伤害判断
     * 
     * @param position 光柱位置
     * @param serverLevel 服务器世界
     * @param spawnTime 生成时间
     * @param boss Boss实体
     */
    private static void scheduleLightPillarDamage(Vec3 position, ServerLevel serverLevel, long spawnTime, LivingEntity boss) {
        net.everla.everlaartifacts.EverlaartifactsMod.queueServerWork(LIGHT_PILLAR_DAMAGE_INTERVAL, () -> {
            if (!boss.isRemoved() && AttackManager.isAttacking(boss)) {
                long currentTime = serverLevel.getGameTime();
                long lifetime = currentTime - spawnTime;
                
                // 如果光柱仍在生命周期内，继续伤害判断
                if (lifetime < LIGHT_PILLAR_LIFETIME) {
                    dealLightPillarDamage(position, serverLevel);
                    // 继续调度下一次伤害判断
                    scheduleLightPillarDamage(position, serverLevel, spawnTime, boss);
                }
            }
        });
    }
    
    /**
     * 显示光柱粒子效果
     * 
     * @param position 光柱位置
     * @param serverLevel 服务器世界
     */
    private static void showLightPillarParticles(Vec3 position, ServerLevel serverLevel) {
        RandomSource random = serverLevel.random;
        
        // 生成淡蓝色高亮光柱粒子
        for (int i = 0; i < 80; i++) { // 增加粒子数量使光柱更明显
            double angle = random.nextDouble() * 2 * Math.PI;
            double radius = LIGHT_PILLAR_RADIUS * random.nextDouble();
            double height = random.nextDouble() * 8; // 显著增加光柱高度到8格
            
            double x = position.x + Math.cos(angle) * radius;
            double y = position.y + height;
            double z = position.z + Math.sin(angle) * radius;
            
            serverLevel.sendParticles(
                BLUE_DUST,
                x, y, z,
                1,
                0, 0, 0,
                0
            );
        }
    }
    
    /**
     * 处理光柱伤害
     * 
     * @param position 光柱位置
     * @param serverLevel 服务器世界
     */
    private static void dealLightPillarDamage(Vec3 position, ServerLevel serverLevel) {
        // 查找范围内的实体
        double damageRadius = LIGHT_PILLAR_RADIUS + 1.0;
        java.util.List<net.minecraft.world.entity.Entity> entities = serverLevel.getEntities(
            null,
            net.minecraft.world.phys.AABB.ofSize(position, damageRadius * 2, 8, damageRadius * 2)
        );
        
        boolean hasDamagedPlayer = false;
        
        for (net.minecraft.world.entity.Entity entity : entities) {
            if (entity instanceof LivingEntity livingEntity) {
                double distance = entity.position().distanceTo(position);
                if (distance <= LIGHT_PILLAR_RADIUS) {
                    // 对所有生物造成伤害，包括玩家
                    livingEntity.hurt(serverLevel.damageSources().magic(), LIGHT_PILLAR_DAMAGE);
                    
                    // 记录是否伤害了玩家
                    if (entity instanceof Player) {
                        hasDamagedPlayer = true;
                    }
                }
            }
        }
        
        // 如果伤害了玩家，记录日志
        if (hasDamagedPlayer) {
            LOGGER.debug("光柱在位置{}造成了玩家伤害", position);
        }
    }
    
    /**
     * 生成螺旋下降的蓝色弹幕
     * 
     * @param centerPos 中心位置（光柱位置）
     * @param serverLevel 服务器世界
     * @param waveIndex 攻击波索引
     */
    private static void spawnOrbitingSkulls(Vec3 centerPos, ServerLevel serverLevel, int waveIndex) {
        RandomSource random = serverLevel.random;
        
        for (int i = 0; i < DANMAKU_COUNT_PER_WAVE; i++) {
            // 随机参数
            double orbitRadius = MIN_ORBIT_RADIUS + random.nextDouble() * (MAX_ORBIT_RADIUS - MIN_ORBIT_RADIUS);
            double startAngle = random.nextDouble() * 2 * Math.PI;
            double startHeight = 3.0 + random.nextDouble() * 2.0; // 起始高度3-5方块
            
            // 计算初始位置
            double x = centerPos.x + Math.cos(startAngle) * orbitRadius;
            double y = centerPos.y + startHeight;
            double z = centerPos.z + Math.sin(startAngle) * orbitRadius;
            
            // 创建Danmaku弹幕
            DanmakuEntity danmaku = DanmakuEntity.createWithoutShooter(serverLevel, 0, 0, 0);
            danmaku.setPos(x, y, z);
            
            // 调试：确认Watari Nina标记已设置
            LOGGER.debug("创建SpellCard1弹幕，SPAWNED_BY_WATARI_NINA_KEY: {}", 
                danmaku.getPersistentData().getBoolean("SpawnedByWatariNina"));
            
            // 设置自定义属性用于螺旋下降
            danmaku.getPersistentData().putBoolean("IsBlueSpiralDanmaku", true);
            danmaku.getPersistentData().putDouble(SPIRAL_CENTER_X_KEY, centerPos.x);
            danmaku.getPersistentData().putDouble(SPIRAL_CENTER_Y_KEY, centerPos.y);
            danmaku.getPersistentData().putDouble(SPIRAL_CENTER_Z_KEY, centerPos.z);
            danmaku.getPersistentData().putDouble(SPIRAL_CURRENT_ANGLE_KEY, startAngle);
            danmaku.getPersistentData().putDouble(SPIRAL_CURRENT_HEIGHT_KEY, startHeight);
            danmaku.getPersistentData().putDouble("OrbitRadius", orbitRadius);
            danmaku.getPersistentData().putInt("SpawnTime", (int)serverLevel.getGameTime());
            danmaku.getPersistentData().putInt("WaveIndex", waveIndex);
            
            // 设置伤害和存在时间
            danmaku.setDeltaMovement(0, 0, 0); // 初始静止
            danmaku.setAccelerationPower(0.05); // 设置很低的加速度
            danmaku.setConstantVelocity(0, 0, 0); // 螺旋状态下速度由逻辑控制
            
            // 设置弹幕伤害（对玩家造成伤害）
            danmaku.getPersistentData().putFloat("SpellCard1DanmakuDamage", ORBITING_DANMAKU_DAMAGE);
            
            serverLevel.addFreshEntity(danmaku);
            
            // 启动螺旋下降逻辑
            startSpiralDescentMotion(danmaku, serverLevel);
        }
    }
    
    /**
     * 启动Danmaku的螺旋下降运动
     * 
     * @param danmaku Danmaku实体
     * @param serverLevel 服务器世界
     */
    private static void startSpiralDescentMotion(DanmakuEntity danmaku, ServerLevel serverLevel) {
        // 调度螺旋下降逻辑
        scheduleSpiralMotionUpdate(danmaku, serverLevel, 0);
        LOGGER.debug("启动Danmaku螺旋下降运动: {}", danmaku.getId());
    }
    
    /**
     * 调度螺旋运动更新
     * 
     * @param danmaku Danmaku实体
     * @param serverLevel 服务器世界
     * @param tickCount 已执行的tick数
     */
    private static void scheduleSpiralMotionUpdate(DanmakuEntity danmaku, ServerLevel serverLevel, int tickCount) {
        // 每tick更新一次位置
        net.everla.everlaartifacts.EverlaartifactsMod.queueServerWork(1, () -> {
            if (!danmaku.isRemoved() && danmaku.isAlive()) {
                // 更新螺旋位置
                updateSpiralPosition(danmaku);
                
                // 继续调度下一tick
                if (tickCount < ORBITING_DANMAKU_LIFETIME) {
                    scheduleSpiralMotionUpdate(danmaku, serverLevel, tickCount + 1);
                }
            }
        });
    }
    
    /**
     * 更新弹幕的螺旋位置
     * 
     * @param danmaku Danmaku实体
     */
    private static void updateSpiralPosition(DanmakuEntity danmaku) {
        // 获取存储的参数
        double centerX = danmaku.getPersistentData().getDouble(SPIRAL_CENTER_X_KEY);
        double centerY = danmaku.getPersistentData().getDouble(SPIRAL_CENTER_Y_KEY);
        double centerZ = danmaku.getPersistentData().getDouble(SPIRAL_CENTER_Z_KEY);
        double currentAngle = danmaku.getPersistentData().getDouble(SPIRAL_CURRENT_ANGLE_KEY);
        double currentHeight = danmaku.getPersistentData().getDouble(SPIRAL_CURRENT_HEIGHT_KEY);
        double orbitRadius = danmaku.getPersistentData().getDouble("OrbitRadius");
        
        // 更新角度和高度
        currentAngle += ROTATION_SPEED_PER_TICK;
        currentHeight -= HEIGHT_DECREMENT_PER_TICK;
        
        // 计算新位置
        double newX = centerX + Math.cos(currentAngle) * orbitRadius;
        double newY = centerY + currentHeight;
        double newZ = centerZ + Math.sin(currentAngle) * orbitRadius;
        
        // 计算位置变化向量
        Vec3 oldPos = danmaku.position();
        Vec3 newPos = new Vec3(newX, newY, newZ);
        Vec3 movement = newPos.subtract(oldPos);
        
        // 设置运动向量以支持碰撞检测
        danmaku.setDeltaMovement(movement);
        
        // 更新位置
        danmaku.setPos(newX, newY, newZ);
        
        // 保存更新后的参数
        danmaku.getPersistentData().putDouble(SPIRAL_CURRENT_ANGLE_KEY, currentAngle);
        danmaku.getPersistentData().putDouble(SPIRAL_CURRENT_HEIGHT_KEY, currentHeight);
        
        // 更新朝向
        updateDanmakuOrientation(danmaku, centerX, centerY, centerZ);
    }
    
    /**
     * 更新弹幕朝向（面向中心点）
     * 
     * @param danmaku Danmaku实体
     * @param centerX 中心X坐标
     * @param centerY 中心Y坐标
     * @param centerZ 中心Z坐标
     */
    private static void updateDanmakuOrientation(DanmakuEntity danmaku, double centerX, double centerY, double centerZ) {
        // 计算从弹幕指向中心的方向向量
        Vec3 direction = new Vec3(centerX, centerY, centerZ).subtract(danmaku.position()).normalize();
        
        // 计算水平方向的角度（Yaw）
        double yaw = Math.atan2(direction.z, direction.x) * (180.0 / Math.PI) - 90.0;
        
        // 计算垂直方向的角度（Pitch）
        double horizontalDistance = Math.sqrt(direction.x * direction.x + direction.z * direction.z);
        double pitch = -Math.atan2(direction.y, horizontalDistance) * (180.0 / Math.PI);
        
        // 限制Pitch角度在合理范围内
        pitch = Math.max(-90.0, Math.min(90.0, pitch));
        
        // 更新弹幕朝向
        danmaku.setYRot((float) yaw);
        danmaku.setXRot((float) pitch);
        danmaku.hurtMarked = true;
    }
    
    /**
     * 启动Danmaku的环绕运动（旧方法，保留兼容性）
     * 
     * @param danmaku Danmaku实体
     * @param serverLevel 服务器世界
     */
    private static void startOrbitingMotion(DanmakuEntity danmaku, ServerLevel serverLevel) {
        // 这里可以添加环绕运动的tick逻辑
        // 由于这是简化的实现，实际环绕运动需要在实体的tick方法中处理
        LOGGER.debug("启动Danmaku环绕运动: {}", danmaku.getId());
    }
    
    /**
     * 检查攻击是否应该结束
     * 
     * @param boss Watari Nina实体
     * @param currentTime 当前时间
     * @return 是否应该结束攻击
     */
    public static boolean shouldEndAttack(LivingEntity boss, long currentTime) {
        long startTime = boss.getPersistentData().getLong(SPELL_START_TIME_KEY);
        long elapsedTicks = currentTime - startTime;
        long elapsedSeconds = elapsedTicks / 20;
        
        return elapsedSeconds >= DURATION_SECONDS;
    }
    
    /**
     * 结束SpellCard1攻击
     * 
     * @param boss Watari Nina实体
     */
    public static void endAttack(LivingEntity boss) {
        // 设置attack值为2
        AttackManager.setAttack(boss, 2);
        AttackManager.setAttacking(boss, false);
        
        // 攻击结束时回满血
        healBossToFullHealth(boss);
        
        // 清理攻击参数
        boss.getPersistentData().remove(SPELL_START_TIME_KEY);
        boss.getPersistentData().remove(LAST_ATTACK_TIME_KEY);
        boss.getPersistentData().remove(ATTACK_WAVE_KEY);
        boss.getPersistentData().remove(BOSS_FLOATING_HEIGHT_KEY);
    }
    

    
    /**
     * 将Boss回满血
     * 
     * @param boss Watari Nina实体
     */
    private static void healBossToFullHealth(LivingEntity boss) {
        float maxHealth = boss.getMaxHealth();
        boss.setHealth(maxHealth);
        LOGGER.info("Boss血量已回满: {}/{}", boss.getHealth(), maxHealth);
    }
}
