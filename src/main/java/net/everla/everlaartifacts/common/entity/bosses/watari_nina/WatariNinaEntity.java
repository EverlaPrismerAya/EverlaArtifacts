package net.everla.everlaartifacts.common.entity.bosses.watari_nina;

import net.everla.everlaartifacts.common.entity.bosses.watari_nina.AttackFunction.NoSpell1;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.nbt.CompoundTag;

public class WatariNinaEntity extends Monster {

    private final ServerBossEvent bossInfo = new ServerBossEvent(this.getDisplayName(), ServerBossEvent.BossBarColor.YELLOW, ServerBossEvent.BossBarOverlay.PROGRESS);

    public WatariNinaEntity(EntityType<? extends Monster> type, Level world) {
        super(type, world);
        this.xpReward = 50;
    }
    
    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 300.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.35D)
                .add(Attributes.ARMOR, 0.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 3000.0D);
    }

    @Override
    public void setNoGravity(boolean ignored){
        super.setNoGravity(true);
    }

    @Override
    public boolean ignoreExplosion() {
        return true;
    }
    @Override
    public boolean causeFallDamage(float l, float d, DamageSource source) {
        return false;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    // BOSS血条
    @Override
    public void startSeenByPlayer(ServerPlayer player) {
        super.startSeenByPlayer(player);
        this.bossInfo.addPlayer(player);
    }

    @Override
    public void stopSeenByPlayer(ServerPlayer player) {
        super.stopSeenByPlayer(player);
        this.bossInfo.removePlayer(player);
    }

    @Override
    public void customServerAiStep() {
        super.customServerAiStep();
        this.bossInfo.setProgress(this.getHealth() / this.getMaxHealth());
    }
    
    /**
     * 实体死亡时的清理逻辑
     * 可以在这里添加特殊掉落或其他逻辑
     */
    @Override
    public void die(DamageSource source) {
        super.die(source);
        
        // 如果正在攻击，结束当前攻击
        if (AttackManager.isAttacking(this)) {
            NoSpell1.endAttack(this);
        }
        
        // 检查是否应该复活
        if (AttackManager.shouldRevive(this)) {
            // 延迟复活，给玩家一些反应时间
            if (this.level() instanceof net.minecraft.server.level.ServerLevel serverLevel) {
                serverLevel.getServer().tell(new net.minecraft.server.TickTask(
                    serverLevel.getServer().getTickCount() + 100, // 5秒后复活
                    () -> {
                        if (!this.isRemoved()) {
                            AttackManager.reviveBoss(this);
                        }
                    }
                ));
            }
        } else {
            // Attack超过限制，彻底死亡
            AttackManager.cleanupAttackData(this);
        }
    }
    
    @Override
    public void tick() {
        super.tick();
        
        // 只在服务端处理攻击逻辑
        if (this.level().isClientSide()) {
            return;
        }
        
        // 初始化Attack数据（如果尚未初始化）
        AttackManager.initializeAttackData(this);
        
        // 检查攻击超时
        long currentTime = this.level().getGameTime();
        if (NoSpell1.shouldEndAttack(this, currentTime)) {
            NoSpell1.endAttack(this);
        }
        
        // 如果没有在攻击且Attack为0，则开始NoSpell1攻击
        if (!AttackManager.isAttacking(this) && AttackManager.getAttack(this) == 0) {
            startAttackIfPossible();
        }
    }
    
    /**
     * 尝试开始攻击
     */
    private void startAttackIfPossible() {
        // 寻找最近的玩家作为目标
        Player nearestPlayer = this.level().getNearestPlayer(this, 32.0);
        
        if (nearestPlayer != null && !nearestPlayer.isSpectator() && !nearestPlayer.isCreative()) {
            // 开始NoSpell1攻击
            NoSpell1.startAttack(this, nearestPlayer);
        }
    }
    
    @Override
    public void addAdditionalSaveData(CompoundTag compound) {
        super.addAdditionalSaveData(compound);
        // 保存Attack数据
        compound.putInt("Attack", AttackManager.getAttack(this));
        compound.putBoolean("IsAttacking", AttackManager.isAttacking(this));
        compound.putLong("AttackStartTime", AttackManager.getAttackStartTime(this));
    }
    
    @Override
    public void readAdditionalSaveData(CompoundTag compound) {
        super.readAdditionalSaveData(compound);
        // 读取Attack数据
        if (compound.contains("Attack")) {
            AttackManager.setAttack(this, compound.getInt("Attack"));
        }
        if (compound.contains("IsAttacking")) {
            AttackManager.setAttacking(this, compound.getBoolean("IsAttacking"));
        }
        if (compound.contains("AttackStartTime")) {
            AttackManager.setAttackStartTime(this, compound.getLong("AttackStartTime"));
        }
    }

}
