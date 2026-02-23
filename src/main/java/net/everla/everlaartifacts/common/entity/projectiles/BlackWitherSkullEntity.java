package net.everla.everlaartifacts.common.entity.projectiles;

import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractHurtingProjectile;
import net.minecraft.world.entity.projectile.WitherSkull;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.network.NetworkHooks;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * 黑色凋灵之首投射物实体
 * 用于Watari Nina的NoSpell1攻击
 */
public class BlackWitherSkullEntity extends AbstractHurtingProjectile {
    
    // NBT标签键
    public static final String ROTATION_ANGLE_KEY = "RotationAngle";
    public static final String IS_CLOCKWISE_KEY = "IsClockwise";
    public static final String SPAWNED_BY_WATARI_NINA_KEY = "SpawnedByWatariNina";
    
    public BlackWitherSkullEntity(EntityType<? extends BlackWitherSkullEntity> type, Level level) {
        super(type, level);
    }
    
    public BlackWitherSkullEntity(Level level, LivingEntity shooter, double xPower, double yPower, double zPower) {
        super(EntityType.WITHER_SKULL, shooter, xPower, yPower, zPower, level);
        this.setOwner(shooter);
        
        // 标记为Watari Nina生成的黑色凋灵之首
        this.getPersistentData().putBoolean(SPAWNED_BY_WATARI_NINA_KEY, true);
    }
    
    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }
    
    @Override
    protected float getInertia() {
        return 0.95F; // 更高的惯性，使弹道更稳定
    }
    
    @Override
    protected boolean shouldBurn() {
        return false; // 不燃烧
    }
    
    @Override
    protected ParticleOptions getTrailParticle() {
        return ParticleTypes.SMOKE; // 使用烟雾粒子轨迹
    }
    
    @Override
    protected void onHitEntity(EntityHitResult result) {
        super.onHitEntity(result);
        
        // 只有当是由Watari Nina生成时才造成伤害
        if (this.getPersistentData().getBoolean(SPAWNED_BY_WATARI_NINA_KEY)) {
            if (this.getOwner() instanceof LivingEntity owner) {
                result.getEntity().hurt(this.damageSources().mobProjectile(this, owner), 8.0F);
            }
        }
    }
    
    @Override
    protected void onHit(HitResult result) {
        super.onHit(result);
        
        // 碰撞后消失
        if (!this.level().isClientSide) {
            this.discard();
        }
    }
    
    /**
     * 设置旋转角度（用于切线飞行方向计算）
     * 
     * @param angle 旋转角度（弧度）
     */
    public void setRotationAngle(double angle) {
        this.getPersistentData().putDouble(ROTATION_ANGLE_KEY, angle);
    }
    
    /**
     * 获取旋转角度
     * 
     * @return 旋转角度（弧度）
     */
    public double getRotationAngle() {
        return this.getPersistentData().getDouble(ROTATION_ANGLE_KEY);
    }
    
    /**
     * 设置旋转方向
     * 
     * @param clockwise 是否顺时针
     */
    public void setClockwise(boolean clockwise) {
        this.getPersistentData().putBoolean(IS_CLOCKWISE_KEY, clockwise);
    }
    
    /**
     * 获取旋转方向
     * 
     * @return 是否顺时针
     */
    public boolean isClockwise() {
        return this.getPersistentData().getBoolean(IS_CLOCKWISE_KEY);
    }
    
    /**
     * 生成黑色凋灵之首
     * 
     * @param level 世界
     * @param shooter 发射者
     * @param xPower X方向力量
     * @param yPower Y方向力量
     * @param zPower Z方向力量
     * @return 黑色凋灵之首实体
     */
    public static BlackWitherSkullEntity shoot(Level level, LivingEntity shooter, double xPower, double yPower, double zPower) {
        BlackWitherSkullEntity skull = new BlackWitherSkullEntity(level, shooter, xPower, yPower, zPower);
        level.addFreshEntity(skull);
        return skull;
    }
    
    /**
     * 从指定位置和方向生成黑色凋灵之首
     * 
     * @param level 世界
     * @param x X坐标
     * @param y Y坐标
     * @param z Z坐标
     * @param xPower X方向力量
     * @param yPower Y方向力量
     * @param zPower Z方向力量
     * @return 黑色凋灵之首实体
     */
    public static BlackWitherSkullEntity shootFromPosition(Level level, double x, double y, double z, 
                                                          double xPower, double yPower, double zPower) {
        // 使用原版凋灵之首实体类型，但添加自定义标识
        net.minecraft.world.entity.projectile.WitherSkull vanillaSkull = new net.minecraft.world.entity.projectile.WitherSkull(EntityType.WITHER_SKULL, level);
        vanillaSkull.setPos(x, y, z);
        vanillaSkull.setDeltaMovement(xPower, yPower, zPower);
        vanillaSkull.getPersistentData().putBoolean(SPAWNED_BY_WATARI_NINA_KEY, true);
        level.addFreshEntity(vanillaSkull);
        return null; // 返回null因为我们使用的是原版实体
    }
}