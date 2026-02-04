package net.everla.everlaartifacts;

import net.everla.everlaartifacts.init.EverlaartifactsModMobEffects;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Mod.EventBusSubscriber
public class VenusShellActiveAttackHandler {
    
    // 冷却时间标记（每0.9秒最多触发一次）
    private static final Map<UUID, Long> LAST_TRIGGER_TIME = new ConcurrentHashMap<>();
    private static final long COOLDOWN_TICKS = 18; // 0.9秒 = 18 ticks
    
    /**
     * 监听实体受到伤害事件，处理Venus Shell Active的附加魔法伤害
     */
    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        // 只在服务端处理
        if (event.getEntity().level().isClientSide()) {
            return;
        }
        
        // 检查伤害来源是否为玩家
        Entity source = event.getSource().getEntity();
        if (!(source instanceof Player player)) {
            return;
        }
        
        // 检查玩家是否具有Venus Shell Active效果
        if (!player.hasEffect(EverlaartifactsModMobEffects.VENUS_SHELL_ACTIVE.get())) {
            return;
        }
        
        // 检查冷却时间
        UUID playerUUID = player.getUUID();
        long currentTime = player.level().getGameTime();
        Long lastTrigger = LAST_TRIGGER_TIME.get(playerUUID);
        
        if (lastTrigger != null && (currentTime - lastTrigger) < COOLDOWN_TICKS) {
            return; // 冷却中，不触发
        }
        
        // 记录触发时间
        LAST_TRIGGER_TIME.put(playerUUID, currentTime);
        
        // 获取目标实体
        LivingEntity target = event.getEntity();
        
        // 计算基于护甲值的魔法伤害（32.8%护甲值）
        double armorValue = getPlayerArmorValue(player);
        double magicDamage = armorValue * 0.328;
        
        // 施加魔法伤害（绕过无敌帧）
        applyMagicDamage(player, target, magicDamage);
        
        // 在目标位置生成末地烛粒子
        spawnEndRodParticles((ServerLevel) player.level(), target);
    }
    
    /**
     * 获取玩家护甲值，如果没有则按1处理
     */
    private static double getPlayerArmorValue(Player player) {
        var armorAttribute = player.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.ARMOR);
        return armorAttribute != null ? Math.max(armorAttribute.getValue(), 1.0) : 1.0;
    }
    
    /**
     * 施加魔法伤害并绕过无敌帧
     */
    private static void applyMagicDamage(Player player, LivingEntity target, double damage) {
        // 使用魔法伤害源
        var damageSource = player.damageSources().magic();
        
        // 绕过无敌帧
        int originalInvulnerableTime = target.invulnerableTime;
        target.invulnerableTime = 0;
        
        // 造成伤害
        target.hurt(damageSource, (float) damage);
        
        // 恢复无敌帧时间
        target.invulnerableTime = originalInvulnerableTime;
    }
    
    /**
     * 在目标位置生成末地烛粒子（服务端调用）
     */
    private static void spawnEndRodParticles(ServerLevel level, LivingEntity target) {
        var pos = target.position();
        
        // 生成末地烛粒子效果
        level.sendParticles(
            ParticleTypes.END_ROD,
            pos.x(),
            pos.y() + target.getBbHeight() / 2.0,
            pos.z(),
            15, // 粒子数量
            0.3, 0.3, 0.3, // 扩散范围
            0.1 // 粒子速度
        );
    }
    
    /**
     * 清理玩家数据（防止内存泄漏）
     */
    @SubscribeEvent
    public static void onPlayerClone(net.minecraftforge.event.entity.player.PlayerEvent.Clone event) {
        if (event.isWasDeath()) {
            LAST_TRIGGER_TIME.remove(event.getOriginal().getUUID());
        }
    }
}