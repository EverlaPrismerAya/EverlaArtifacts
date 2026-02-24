package net.everla.everlaartifacts.common.entity.projectiles;

import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.Level;

import java.util.UUID;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.everla.everlaartifacts.init.EverlaartifactsModEntities;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Danmaku弹幕投射物实体
 * 用于Watari Nina的弹幕攻击系统
 * 具有瞬时动量（加速度）和常态动量（飞行速度）特性
 */
public class DanmakuEntity extends Entity implements OwnableEntity {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(DanmakuEntity.class);
    
    // NBT标签键
    public static final String ROTATION_ANGLE_KEY = "RotationAngle";
    public static final String IS_CLOCKWISE_KEY = "IsClockwise";
    public static final String SPAWNED_BY_WATARI_NINA_KEY = "SpawnedByWatariNina";
    public static final String ACCELERATION_POWER_KEY = "acceleration_power";
    public static final String SPAWN_TIME_KEY = "SpawnTime";
    public static final String INITIAL_VELOCITY_X_KEY = "InitialVelX";
    public static final String INITIAL_VELOCITY_Y_KEY = "InitialVelY";
    public static final String INITIAL_VELOCITY_Z_KEY = "InitialVelZ";
    public static final String CONSTANT_VELOCITY_X_KEY = "ConstantVelX";
    public static final String CONSTANT_VELOCITY_Y_KEY = "ConstantVelY";
    public static final String CONSTANT_VELOCITY_Z_KEY = "ConstantVelZ";
    public static final String ACCELERATION_DURATION_KEY = "AccelerationDuration";
    public static final String HAS_ACCELERATED_KEY = "HasAccelerated";
    public static final String LIFETIME_KEY = "Lifetime";
    public static final String MAX_LIFETIME_KEY = "MaxLifetime";
    
    // 物理参数常量
    private static final double POWER_THRESHOLD = 0.01; // Power矢量阈值
    private static final int DEFAULT_ACCELERATION_DURATION = 20; // 默认加速持续时间（刻）
    private static final int DEFAULT_MAX_LIFETIME = 100; // 默认最大存在时间（刻）
    
    // 公共构造函数，用于实体注册系统
    public DanmakuEntity(EntityType<? extends DanmakuEntity> type, Level level) {
        super(type, level);
    }
    
    // 私有构造函数，用于有射手的情况
    private DanmakuEntity(Level level, LivingEntity shooter, double xPower, double yPower, double zPower) {
        super(EverlaartifactsModEntities.DANMAKU.get(), level);
        this.setDeltaMovement(xPower, yPower, zPower);
        if (shooter != null) {
            this.owner = shooter;
        }
    }
    
    // 私有构造函数，用于无射手的情况
    private DanmakuEntity(Level level, double xPower, double yPower, double zPower) {
        super(EverlaartifactsModEntities.DANMAKU.get(), level);
        this.setDeltaMovement(xPower, yPower, zPower);
    }
    
    // 工厂方法：创建有射手的Danmaku
    public static DanmakuEntity createWithShooter(Level level, LivingEntity shooter, double xPower, double yPower, double zPower) {
        DanmakuEntity danmaku = new DanmakuEntity(level, shooter, xPower, yPower, zPower);
        danmaku.initializeCommonProperties(level, xPower, yPower, zPower);
        return danmaku;
    }
    
    // 工厂方法：创建无射手的Danmaku
    public static DanmakuEntity createWithoutShooter(Level level, double xPower, double yPower, double zPower) {
        DanmakuEntity danmaku = new DanmakuEntity(level, xPower, yPower, zPower);
        danmaku.initializeCommonProperties(level, xPower, yPower, zPower);
        return danmaku;
    }
    
    // 初始化共同属性的方法
    private void initializeCommonProperties(Level level, double xPower, double yPower, double zPower) {
        // 标记为Watari Nina生成的Danmaku
        this.getPersistentData().putBoolean(SPAWNED_BY_WATARI_NINA_KEY, true);
        
        // 设置初始参数
        setAccelerationPower(0.3);
        setSpawnTime(level.getGameTime());
        setAccelerationDuration(DEFAULT_ACCELERATION_DURATION);
        setHasAccelerated(false);
        setMaxLifetime(DEFAULT_MAX_LIFETIME);
        setLifetime(0);
        
        // 保存初始速度向量
        setInitialVelocity(xPower, yPower, zPower);
        setConstantVelocity(xPower, yPower, zPower); // 默认常态速度等于初始速度
    }
    
    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }
    
    @Override
    public boolean isOnFire() {
        return false; // 不燃烧
    }
    
    @Override
    public boolean isNoGravity() {
        return true; // 无重力影响
    }
    
    protected ParticleOptions getTrailParticle() {
        return ParticleTypes.SMOKE; // 使用烟雾粒子轨迹
    }
    
    // 添加惯性控制
    protected float getInertia() {
        return 1.0F; // 更高的惯性，使弹道更稳定
    }
    
    /**
     * 更新实体朝向以匹配运动方向
     */
    private void updateRotationToMatchMovement() {
        Vec3 motion = this.getDeltaMovement();
        if (motion.lengthSqr() > 0.001) { // 只有当有明显运动时才更新朝向
            // 计算水平方向的角度（Yaw）
            double yaw = Math.atan2(motion.z, motion.x) * (180.0 / Math.PI) - 90.0;
            
            // 计算垂直方向的角度（Pitch）
            double horizontalDistance = Math.sqrt(motion.x * motion.x + motion.z * motion.z);
            double pitch = -Math.atan2(motion.y, horizontalDistance) * (180.0 / Math.PI);
            
            // 限制Pitch角度在合理范围内
            pitch = Math.max(-90.0, Math.min(90.0, pitch));
            
            // 更新实体朝向
            this.setYRot((float) yaw);
            this.setXRot((float) pitch);
            
            // 标记位置已更新
            this.hurtMarked = true;
        }
    }
    
    protected void onEntityHit(EntityHitResult result) {
        // 调试日志
        LOGGER.debug("Danmaku实体碰撞检测 - SPAWNED_BY_WATARI_NINA_KEY: {}, 是否包含SpellCard1DanmakuDamage: {}", 
            this.getPersistentData().getBoolean(SPAWNED_BY_WATARI_NINA_KEY),
            this.getPersistentData().contains("SpellCard1DanmakuDamage"));
        
        // 只有当是由Watari Nina生成时才造成伤害
        if (this.getPersistentData().getBoolean(SPAWNED_BY_WATARI_NINA_KEY)) {
            // 检查是否有SpellCard1自定义伤害
            if (this.getPersistentData().contains("SpellCard1DanmakuDamage")) {
                float customDamage = this.getPersistentData().getFloat("SpellCard1DanmakuDamage");
                result.getEntity().hurt(this.damageSources().magic(), customDamage);
                LOGGER.debug("SpellCard1弹幕造成伤害: {} 点", customDamage);
            } else {
                // 默认伤害
                LOGGER.debug("使用默认伤害24.0F");
                Entity ownerEntity = this.getOwner();
                if (ownerEntity instanceof LivingEntity livingOwner) {
                    result.getEntity().hurt(this.damageSources().mobProjectile(this, livingOwner), 24.0F);
                } else {
                    // 如果没有owner或owner不是LivingEntity，使用通用伤害源
                    result.getEntity().hurt(this.damageSources().magic(), 24.0F);
                }
            }
        } else {
            LOGGER.debug("不是Watari Nina生成的弹幕，不造成伤害");
        }
        // 碰撞后消失
        if (!this.level().isClientSide) {
            this.discard();
        }
    }
    
    @Override
    public void tick() {
        super.tick();
        
        // 处理投射物运动
        if (!this.level().isClientSide) {
            Vec3 vec3 = this.getDeltaMovement();
            // 应用惯性
            this.setDeltaMovement(vec3.scale(this.getInertia()));
            // 更新实体朝向以匹配运动方向
            updateRotationToMatchMovement();
        }
        
        // 处理碰撞检测
        HitResult hitresult = ProjectileUtil.getHitResultOnMoveVector(this, this::canHitEntity);
        if (hitresult.getType() != HitResult.Type.MISS) {
            this.onHit(hitresult);
        }
        
        // 更新位置
        this.checkInsideBlocks();
        Vec3 vec31 = this.getDeltaMovement();
        this.setPos(this.getX() + vec31.x, this.getY() + vec31.y, this.getZ() + vec31.z);
        
        // 处理加速逻辑
        handleAcceleration();
        
        // 更新存在时间
        updateLifetimeInternal();
        
        // 检查生命周期
        checkLifetimeInternal();
    }
    
    /**
     * 更新存在时间
     */
    private void updateLifetimeInternal() {
        if (this.level().isClientSide || !this.getPersistentData().getBoolean(SPAWNED_BY_WATARI_NINA_KEY)) {
            return;
        }
        
        // 增加存在时间
        int currentLifetime = getLifetime();
        setLifetime(currentLifetime + 1);
    }
    
    /**
     * 检查Danmaku生命周期
     */
    private void checkLifetimeInternal() {
        // 只在服务端检查
        if (this.level().isClientSide || !this.getPersistentData().getBoolean(SPAWNED_BY_WATARI_NINA_KEY)) {
            return;
        }
        
        // 检查是否存在时间超限
        if (isLifetimeExceeded()) {
            this.discard();
            return;
        }
        
        // 检查Power矢量是否趋近于零
        if (isPowerNearlyZero()) {
            this.discard();
        }
    }
    
    /**
     * 检查存在时间是否超限
     * 
     * @return 如果存在时间超过最大值则返回true
     */
    private boolean isLifetimeExceeded() {
        return getLifetime() >= getMaxLifetime();
    }
    
    /**
     * 检查Power矢量各分量绝对值是否均小于阈值
     * 
     * @return 如果Power矢量趋近于零则返回true
     */
    private boolean isPowerNearlyZero() {
        // 从原版NBT读取Power矢量
        net.minecraft.nbt.CompoundTag nbt = this.saveWithoutId(new net.minecraft.nbt.CompoundTag());
        
        if (nbt.contains("Power", net.minecraft.nbt.Tag.TAG_LIST)) {
            net.minecraft.nbt.ListTag powerList = nbt.getList("Power", net.minecraft.nbt.Tag.TAG_DOUBLE);
            
            if (powerList.size() >= 3) {
                double x = powerList.getDouble(0);
                double y = powerList.getDouble(1);
                double z = powerList.getDouble(2);
                
                // 检查各分量绝对值是否均小于阈值
                return Math.abs(x) < POWER_THRESHOLD && Math.abs(y) < POWER_THRESHOLD && Math.abs(z) < POWER_THRESHOLD;
            }
        }
        
        return false;
    }
    
    /**
     * 处理瞬时动量（加速度）逻辑
     */
    private void handleAcceleration() {
        if (this.level().isClientSide || !this.getPersistentData().getBoolean(SPAWNED_BY_WATARI_NINA_KEY)) {
            return;
        }
        
        // 检查是否已经完成加速
        if (this.getPersistentData().getBoolean(HAS_ACCELERATED_KEY)) {
            return;
        }
        
        long spawnTime = getSpawnTime();
        long currentTime = this.level().getGameTime();
        int accelerationDuration = getAccelerationDuration();
        
        // 检查是否仍在加速阶段
        if (currentTime - spawnTime < accelerationDuration) {
            // 应用加速度
            double accelerationPower = getAccelerationPower();
            Vec3 constantVelocity = getConstantVelocity();
            
            // 增加当前速度
            this.setDeltaMovement(
                this.getDeltaMovement().x + constantVelocity.x * accelerationPower,
                this.getDeltaMovement().y + constantVelocity.y * accelerationPower,
                this.getDeltaMovement().z + constantVelocity.z * accelerationPower
            );
        } else {
            // 加速结束，设置为常态速度
            Vec3 constantVelocity = getConstantVelocity();
            this.setDeltaMovement(constantVelocity);
            setHasAccelerated(true);
        }
    }
    
    // Owner相关字段和方法
    private LivingEntity owner;
    
    @Override
    public LivingEntity getOwner() {
        return this.owner;
    }
    
    @Override
    public UUID getOwnerUUID() {
        return this.owner != null ? this.owner.getUUID() : null;
    }
    
    // 实体基本方法实现
    @Override
    protected void defineSynchedData() {
        // 不需要同步数据
    }
    
    @Override
    protected void readAdditionalSaveData(net.minecraft.nbt.CompoundTag compound) {
        // 从NBT读取生成时间
        if (compound.contains(SPAWN_TIME_KEY)) {
            setSpawnTime(compound.getLong(SPAWN_TIME_KEY));
        }
        if (compound.contains(ACCELERATION_DURATION_KEY)) {
            setAccelerationDuration(compound.getInt(ACCELERATION_DURATION_KEY));
        }
        if (compound.contains(HAS_ACCELERATED_KEY)) {
            setHasAccelerated(compound.getBoolean(HAS_ACCELERATED_KEY));
        }
        if (compound.contains(LIFETIME_KEY)) {
            setLifetime(compound.getInt(LIFETIME_KEY));
        }
        if (compound.contains(MAX_LIFETIME_KEY)) {
            setMaxLifetime(compound.getInt(MAX_LIFETIME_KEY));
        }
        
        // 读取速度向量
        if (compound.contains(INITIAL_VELOCITY_X_KEY) && 
            compound.contains(INITIAL_VELOCITY_Y_KEY) && 
            compound.contains(INITIAL_VELOCITY_Z_KEY)) {
            setInitialVelocity(
                compound.getDouble(INITIAL_VELOCITY_X_KEY),
                compound.getDouble(INITIAL_VELOCITY_Y_KEY),
                compound.getDouble(INITIAL_VELOCITY_Z_KEY)
            );
        }
        
        if (compound.contains(CONSTANT_VELOCITY_X_KEY) && 
            compound.contains(CONSTANT_VELOCITY_Y_KEY) && 
            compound.contains(CONSTANT_VELOCITY_Z_KEY)) {
            setConstantVelocity(
                compound.getDouble(CONSTANT_VELOCITY_X_KEY),
                compound.getDouble(CONSTANT_VELOCITY_Y_KEY),
                compound.getDouble(CONSTANT_VELOCITY_Z_KEY)
            );
        }
    }
    
    @Override
    protected void addAdditionalSaveData(net.minecraft.nbt.CompoundTag compound) {
        // 保存生成时间到NBT
        compound.putLong(SPAWN_TIME_KEY, getSpawnTime());
        compound.putInt(ACCELERATION_DURATION_KEY, getAccelerationDuration());
        compound.putBoolean(HAS_ACCELERATED_KEY, hasAccelerated());
        compound.putInt(LIFETIME_KEY, getLifetime());
        compound.putInt(MAX_LIFETIME_KEY, getMaxLifetime());
        
        // 保存速度向量
        Vec3 initialVel = getInitialVelocity();
        compound.putDouble(INITIAL_VELOCITY_X_KEY, initialVel.x);
        compound.putDouble(INITIAL_VELOCITY_Y_KEY, initialVel.y);
        compound.putDouble(INITIAL_VELOCITY_Z_KEY, initialVel.z);
        
        Vec3 constantVel = getConstantVelocity();
        compound.putDouble(CONSTANT_VELOCITY_X_KEY, constantVel.x);
        compound.putDouble(CONSTANT_VELOCITY_Y_KEY, constantVel.y);
        compound.putDouble(CONSTANT_VELOCITY_Z_KEY, constantVel.z);
    }
    
    // 投射物特有方法
    protected boolean canHitEntity(Entity entity) {
        return !entity.isSpectator() && entity.isPickable();
    }
    
    protected void onHit(HitResult result) {
        HitResult.Type hitresult$type = result.getType();
        if (hitresult$type == HitResult.Type.ENTITY) {
            this.onEntityHit((EntityHitResult)result);
        } else if (hitresult$type == HitResult.Type.BLOCK) {
            // 碰撞到方块后消失
            if (!this.level().isClientSide) {
                this.discard();
            }
        }
    }
    
    // Getter和Setter方法
    public void setRotationAngle(double angle) {
        this.getPersistentData().putDouble(ROTATION_ANGLE_KEY, angle);
    }
    
    public double getRotationAngle() {
        return this.getPersistentData().getDouble(ROTATION_ANGLE_KEY);
    }
    
    public void setClockwise(boolean clockwise) {
        this.getPersistentData().putBoolean(IS_CLOCKWISE_KEY, clockwise);
    }
    
    public boolean isClockwise() {
        return this.getPersistentData().getBoolean(IS_CLOCKWISE_KEY);
    }
    
    public double getAccelerationPower() {
        return this.getPersistentData().getDouble(ACCELERATION_POWER_KEY);
    }
    
    public void setAccelerationPower(double power) {
        this.getPersistentData().putDouble(ACCELERATION_POWER_KEY, power);
    }
    
    public long getSpawnTime() {
        return this.getPersistentData().getLong(SPAWN_TIME_KEY);
    }
    
    public void setSpawnTime(long time) {
        this.getPersistentData().putLong(SPAWN_TIME_KEY, time);
    }
    
    public void setInitialVelocity(double x, double y, double z) {
        this.getPersistentData().putDouble(INITIAL_VELOCITY_X_KEY, x);
        this.getPersistentData().putDouble(INITIAL_VELOCITY_Y_KEY, y);
        this.getPersistentData().putDouble(INITIAL_VELOCITY_Z_KEY, z);
    }
    
    public Vec3 getInitialVelocity() {
        return new Vec3(
            this.getPersistentData().getDouble(INITIAL_VELOCITY_X_KEY),
            this.getPersistentData().getDouble(INITIAL_VELOCITY_Y_KEY),
            this.getPersistentData().getDouble(INITIAL_VELOCITY_Z_KEY)
        );
    }
    
    public void setConstantVelocity(double x, double y, double z) {
        this.getPersistentData().putDouble(CONSTANT_VELOCITY_X_KEY, x);
        this.getPersistentData().putDouble(CONSTANT_VELOCITY_Y_KEY, y);
        this.getPersistentData().putDouble(CONSTANT_VELOCITY_Z_KEY, z);
    }
    
    public Vec3 getConstantVelocity() {
        return new Vec3(
            this.getPersistentData().getDouble(CONSTANT_VELOCITY_X_KEY),
            this.getPersistentData().getDouble(CONSTANT_VELOCITY_Y_KEY),
            this.getPersistentData().getDouble(CONSTANT_VELOCITY_Z_KEY)
        );
    }
    
    public int getAccelerationDuration() {
        return this.getPersistentData().getInt(ACCELERATION_DURATION_KEY);
    }
    
    public void setAccelerationDuration(int duration) {
        this.getPersistentData().putInt(ACCELERATION_DURATION_KEY, duration);
    }
    
    public boolean hasAccelerated() {
        return this.getPersistentData().getBoolean(HAS_ACCELERATED_KEY);
    }
    
    public void setHasAccelerated(boolean accelerated) {
        this.getPersistentData().putBoolean(HAS_ACCELERATED_KEY, accelerated);
    }
    
    public int getLifetime() {
        return this.getPersistentData().getInt(LIFETIME_KEY);
    }
    
    public void setLifetime(int lifetime) {
        this.getPersistentData().putInt(LIFETIME_KEY, lifetime);
    }
    
    public int getMaxLifetime() {
        return this.getPersistentData().getInt(MAX_LIFETIME_KEY);
    }
    
    public void setMaxLifetime(int maxLifetime) {
        this.getPersistentData().putInt(MAX_LIFETIME_KEY, maxLifetime);
    }
    
    /**
     * 生成Danmaku弹幕
     * 
     * @param level 世界
     * @param shooter 发射者
     * @param xPower X方向力量
     * @param yPower Y方向力量
     * @param zPower Z方向力量
     * @return Danmaku实体
     */
    public static DanmakuEntity shoot(Level level, LivingEntity shooter, double xPower, double yPower, double zPower) {
        DanmakuEntity danmaku = DanmakuEntity.createWithShooter(level, shooter, xPower, yPower, zPower);
        level.addFreshEntity(danmaku);
        return danmaku;
    }
    
    /**
     * 从指定位置和方向生成Danmaku弹幕
     * 
     * @param level 世界
     * @param x X坐标
     * @param y Y坐标
     * @param z Z坐标
     * @param xPower X方向力量
     * @param yPower Y方向力量
     * @param zPower Z方向力量
     * @return Danmaku实体
     */
    public static DanmakuEntity shootFromPosition(Level level, double x, double y, double z, 
                                                 double xPower, double yPower, double zPower) {
        DanmakuEntity danmaku = DanmakuEntity.createWithoutShooter(level, xPower, yPower, zPower);
        danmaku.setPos(x, y, z);
        level.addFreshEntity(danmaku);
        return danmaku;
    }
}