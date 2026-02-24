package net.everla.everlaartifacts.common.entity.bosses.watari_nina;

import net.everla.everlaartifacts.common.entity.bosses.watari_nina.AttackFunction.NoSpell1;
import net.everla.everlaartifacts.common.entity.bosses.watari_nina.AttackFunction.NoSpell2;
import net.everla.everlaartifacts.common.entity.bosses.watari_nina.AttackFunction.SpellCard1;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WatariNinaEntity extends Monster {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(WatariNinaEntity.class);

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

    /**
     * 查找有效的目标玩家（排除观察者和创造模式玩家）
     * 增强版：支持更大的搜索范围和更好的目标选择逻辑
     * 
     * @return 最近的有效玩家，如果没有则返回null
     */
    private Player findValidTargetPlayer() {
        Player nearestPlayer = null;
        double nearestDistance = Double.MAX_VALUE;
        double primaryRangeSqr = 32.0 * 32.0; // 主要搜索范围：32方块
        double extendedRangeSqr = 64.0 * 64.0; // 扩展搜索范围：64方块
        
        // 第一轮：在主要范围内寻找
        for (Player player : this.level().players()) {
            // 排除死亡、创造模式、旁观模式的玩家
            if (player.isAlive() && !player.isCreative() && !player.isSpectator()) {
                double distance = player.distanceToSqr(this);
                // 优先考虑主要范围内的玩家
                if (distance <= primaryRangeSqr && distance < nearestDistance) {
                    nearestDistance = distance;
                    nearestPlayer = player;
                }
            }
        }
        
        // 如果在主要范围内找到了玩家，直接返回
        if (nearestPlayer != null) {
            return nearestPlayer;
        }
        
        // 第二轮：扩展搜索范围，但降低优先级
        for (Player player : this.level().players()) {
            if (player.isAlive() && !player.isCreative() && !player.isSpectator()) {
                double distance = player.distanceToSqr(this);
                // 在扩展范围内寻找，但优先级低于主要范围
                if (distance <= extendedRangeSqr && distance < nearestDistance) {
                    nearestDistance = distance;
                    nearestPlayer = player;
                }
            }
        }
        
        return nearestPlayer;
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
            if (AttackManager.getAttack(this) == 0) {
                NoSpell1.endAttack(this);
            } else if (AttackManager.getAttack(this) == 1) {
                SpellCard1.endAttack(this);
            } else if (AttackManager.getAttack(this) == 2) {
                NoSpell2.endAttack(this);
            }
        }
        
        // 检查是否应该复活（只有attack < 4时才复活）
        int currentAttack = AttackManager.getAttack(this);
        if (currentAttack < 4) {
            // 立即复活，无需延迟
            if (this.level() instanceof net.minecraft.server.level.ServerLevel serverLevel) {
                AttackManager.reviveBoss(this);
                // 立即移除死亡状态
                this.setHealth(this.getMaxHealth());
                this.revive();
            }
        } else {
            // attack为4时受到致死伤害才真正死亡
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
        
        // 初始化Attack数据
        AttackManager.initializeAttackData(this);
        
        // 检查攻击超时
        long currentTime = this.level().getGameTime();
        
        if (AttackManager.isAttacking(this)) {
            int currentAttack = AttackManager.getAttack(this);
            if (currentAttack == 0 && NoSpell1.shouldEndAttack(this, currentTime)) {
                NoSpell1.endAttack(this);
            } else if (currentAttack == 1 && SpellCard1.shouldEndAttack(this, currentTime)) {
                SpellCard1.endAttack(this);
            }
        }
        
        // 每10tick检查一次攻击状态
        if (this.tickCount % 10 == 0 && !AttackManager.isAttacking(this)) {
            int attackValue = AttackManager.getAttack(this);
            if (attackValue == 0) {
                startAttackIfPossible();
            } else if (attackValue == 1) {
                startSpellCard1IfPossible();
            } else if (attackValue == 2) {
                startSpellCard2IfPossible();
            }
        }
    }
    
    /**
     * 尝试开始NoSpell1攻击
     */
    private void startAttackIfPossible() {
        Player targetPlayer = findValidTargetPlayer();
        if (targetPlayer != null) {
            NoSpell1.startAttack(this, targetPlayer);
        }
    }
    
    /**
     * 尝试开始SpellCard1攻击
     */
    private void startSpellCard1IfPossible() {
        Player targetPlayer = findValidTargetPlayer();
        if (targetPlayer != null) {
            SpellCard1.startAttack(this, targetPlayer);
        }
    }
    
    /**
     * 尝试开始NoSpell2攻击
     */
    private void startSpellCard2IfPossible() {
        Player targetPlayer = findValidTargetPlayer();
        if (targetPlayer != null) {
            NoSpell2.startAttack(this, targetPlayer);
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
