package net.everla.everlaartifacts;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import net.everla.everlaartifacts.item.HomaStaffItem;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.WeakHashMap;

@Mod.EventBusSubscriber(modid = "everlaartifacts")
public class HomaMysteryArtHandler {
    // 冷却时间：15秒 = 300 tick
    private static final long MYSTERY_ART_COOLDOWN = 300L;
    private static final String COOLDOWN_KEY = "everlaartifacts:homa_mystery_art_cooldown";
    
    // 伤害倍率
    private static final float BASE_DAMAGE_MULTIPLIER = 4.94f;   // 494%
    private static final float LOW_HP_DAMAGE_MULTIPLIER = 6.17f; // 617% (<30% HP)
    
    // 治疗倍率
    private static final float BASE_HEAL_MULTIPLIER = 0.1020f;   // 10.20%
    private static final float LOW_HP_HEAL_MULTIPLIER = 0.1360f; // 13.60% (<30% HP)
    
    // 范围参数：高4格（-1~+3），半径5格的圆柱体
    private static final double RADIUS = 5.0;
    private static final double HEIGHT_BOTTOM = 1.0; // 向下扩展1格
    private static final double HEIGHT_TOP = 3.0;    // 向上3格
    private static final double TOTAL_HEIGHT = HEIGHT_BOTTOM + HEIGHT_TOP; // 4格
    
    // 粒子特效参数
    private static final int FLAME_PARTICLES = 100; // 增加火焰粒子数量
    private static final int SMOKE_PARTICLES = 80;  // 增加烟雾粒子数量
    private static final int SPARK_PARTICLES = 50;  // 增加火花粒子数量
    private static final int CENTER_BURST_PARTICLES = 40; // 增加中心爆发粒子
    private static final int GROUND_RUNE_PARTICLES = 32; // 增加地面符文粒子
    private static final int FIRE_WAVE_PARTICLES = 60; // 新增：火焰波纹粒子
    
    // 冷却完成提示跟踪
    private static final Set<UUID> NOTIFIED_PLAYERS = new HashSet<>();
    
    // 用于控制冷却检查频率的计数器
    private static final java.util.Map<java.util.UUID, Integer> playerTickCounter = new java.util.WeakHashMap<>();

    @SubscribeEvent
    public static void onPlayerAttack(AttackEntityEvent event) {
        Player player = event.getEntity();
        Level level = player.level();
        
        if (level.isClientSide() || !(level instanceof ServerLevel serverLevel)) {
            return;
        }
        
        if (!player.isCrouching() || !(player.getMainHandItem().getItem() instanceof HomaStaffItem)) {
            return;
        }
        
        long currentTime = serverLevel.getGameTime();
        long lastUsed = player.getPersistentData().getLong(COOLDOWN_KEY);
        
        // 冷却中：允许普通攻击
        if (currentTime < lastUsed) {
            return;
        }
        
        // 冷却就绪：触发终结技
        event.setCanceled(true);
        NOTIFIED_PLAYERS.remove(player.getUUID());
        executeMysteryArt(player, serverLevel);
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        
        Player player = event.player;
        Level level = player.level();
        
        if (level.isClientSide() || !(player.getMainHandItem().getItem() instanceof HomaStaffItem)) {
            return;
        }
        
        // 每10个tick检查一次冷却状态，减少性能开销
        UUID playerUUID = player.getUUID();
        int currentTick = playerTickCounter.getOrDefault(playerUUID, 0) + 1;
        playerTickCounter.put(playerUUID, currentTick);
        
        if (currentTick % 10 != 0) { // 每10个tick检查一次
            return;
        }
        
        long currentTime = level.getGameTime();
        long cooldownEnd = player.getPersistentData().getLong(COOLDOWN_KEY);
        
        if (currentTime >= cooldownEnd && !NOTIFIED_PLAYERS.contains(player.getUUID())) {
            sendCooldownReadyActionBar(player);
            NOTIFIED_PLAYERS.add(player.getUUID());
        }
    }

    private static void sendCooldownReadyActionBar(Player player) {
        player.displayClientMessage(
            Component.translatable("item.everlaartifacts.homa_staff.cooldown_ready"),
            true
        );
    }

    private static void executeMysteryArt(Player player, ServerLevel level) {
        boolean isLowHp = (player.getHealth() <= player.getMaxHealth() * 0.3f);
        float damageMultiplier = isLowHp ? LOW_HP_DAMAGE_MULTIPLIER : BASE_DAMAGE_MULTIPLIER;
        float healMultiplier = isLowHp ? LOW_HP_HEAL_MULTIPLIER : BASE_HEAL_MULTIPLIER;
        
        double baseAttack = player.getAttributeValue(Attributes.ATTACK_DAMAGE);
        double damage = baseAttack * damageMultiplier;
        
        // ===== 核心修改：范围向下扩展1格 =====
        Vec3 center = player.position();
        AABB detectionBox = new AABB(
            center.x - RADIUS, 
            center.y - HEIGHT_BOTTOM, // 向下扩展1格（原为center.y）
            center.z - RADIUS,
            center.x + RADIUS, 
            center.y + HEIGHT_TOP,   // 向上3格（保持不变）
            center.z + RADIUS
        );
        
        java.util.List<LivingEntity> entities = level.getEntitiesOfClass(
            LivingEntity.class,
            detectionBox,
            entity -> entity != player && entity.isAlive() && !entity.isInvulnerable()
        );
        
        int hitCount = 0;
        for (LivingEntity entity : entities) {
            double dx = entity.getX() - center.x;
            double dz = entity.getZ() - center.z;
            double horizontalDist = Math.sqrt(dx * dx + dz * dz);
            
            if (horizontalDist <= RADIUS) {
                if (entity.hurt(level.damageSources().playerAttack(player), (float) damage)) {
                    hitCount++;
                }
            }
        }
        
        // 音效
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
            SoundEvents.BLAZE_SHOOT, SoundSource.PLAYERS, 1.2f, 0.7f);
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
            SoundEvents.FIRECHARGE_USE, SoundSource.PLAYERS, 1.0f, 1.8f);
        
        // 粒子特效（适配新范围）
        spawnMysteryArtParticles(player, level, center, hitCount > 0);
        
        // 治疗
        if (hitCount > 0) {
            double healAmount = player.getMaxHealth() * healMultiplier * hitCount;
            player.heal((float) healAmount);
            
            level.sendParticles(ParticleTypes.HEART,
                player.getX(), player.getY() + 1.0, player.getZ(),
                8 + hitCount * 2, 0.6, 0.8, 0.6, 0.05);
        }
        
        player.getPersistentData().putLong(COOLDOWN_KEY, level.getGameTime() + MYSTERY_ART_COOLDOWN);
    }

    private static void spawnMysteryArtParticles(Player player, ServerLevel level, Vec3 center, boolean hasHit) {
        double radius = hasHit ? RADIUS : RADIUS * 0.7;
        
        // 1. 火焰漩涡主效果（营造野火燎原的感觉）
        for (int i = 0; i < FLAME_PARTICLES; i++) {
            double angle = level.random.nextDouble() * Math.PI * 2;
            double dist = radius * (0.7 + level.random.nextDouble() * 0.3);
            double x = center.x + Math.cos(angle) * dist;
            double z = center.z + Math.sin(angle) * dist;
            double y = center.y - HEIGHT_BOTTOM + level.random.nextDouble() * TOTAL_HEIGHT; // 从y-1到y+3
            
            // 添加随机速度，使火焰看起来更动态
            double vx = (level.random.nextDouble() - 0.5) * 0.3;
            double vy = level.random.nextDouble() * 0.2;
            double vz = (level.random.nextDouble() - 0.5) * 0.3;
            
            level.sendParticles(ParticleTypes.FLAME,
                x, y, z,
                1, vx, vy, vz, 0.01);
        }
        
        // 2. 烟雾扩散效果（营造蔓延感）
        for (int i = 0; i < SMOKE_PARTICLES; i++) {
            double angle = level.random.nextDouble() * Math.PI * 2;
            double dist = radius * level.random.nextDouble();
            double x = center.x + Math.cos(angle) * dist;
            double z = center.z + Math.sin(angle) * dist;
            double y = center.y - HEIGHT_BOTTOM + 0.1; // 从y-1高度开始
            
            // 烟雾向上扩散
            double vx = (level.random.nextDouble() - 0.5) * 0.2;
            double vy = 0.3 + level.random.nextDouble() * 0.4; // 向上速度更大
            double vz = (level.random.nextDouble() - 0.5) * 0.2;
            
            level.sendParticles(ParticleTypes.SMOKE,
                x, y, z,
                1, vx, vy, vz, 0.05);
        }
        
        // 3. 火花飞溅效果（营造火焰扩散感）
        for (int i = 0; i < SPARK_PARTICLES; i++) {
            double angle = level.random.nextDouble() * Math.PI * 2;
            double dist = radius * 0.5 * level.random.nextDouble();
            double x = center.x + Math.cos(angle) * dist;
            double z = center.z + Math.sin(angle) * dist;
            double y = center.y - 0.5 + level.random.nextDouble() * 2.0; // y-0.5 到 y+1.5
            
            // 火花随机扩散
            double vx = (level.random.nextDouble() - 0.5) * 0.5;
            double vy = (level.random.nextDouble() - 0.2) * 0.4;
            double vz = (level.random.nextDouble() - 0.5) * 0.5;
            
            level.sendParticles(ParticleTypes.CRIMSON_SPORE, // 使用更暖色调的粒子
                x, y, z,
                1, vx, vy, vz, 0.02);
        }
        
        // 4. 玩家位置中心爆发（增强爆发感）
        for (int i = 0; i < CENTER_BURST_PARTICLES; i++) {
            double angle = level.random.nextDouble() * Math.PI * 2;
            double pitch = (level.random.nextDouble() - 0.5) * Math.PI / 4;
            double speed = 0.8 + level.random.nextDouble() * 0.6; // 增加速度
            
            double x = Math.cos(pitch) * Math.cos(angle) * speed;
            double y = Math.sin(pitch) * speed + 0.3;
            double z = Math.cos(pitch) * Math.sin(angle) * speed;
            
            level.sendParticles(ParticleTypes.FLAME,
                player.getX(), player.getY() + 1.2, player.getZ(),
                1, x, y, z, 0.02); // 增加随机偏移
        }
        
        // 5. 地面火焰蔓延效果（营造野火燎原感）
        for (int i = 0; i < GROUND_RUNE_PARTICLES; i++) {
            double angle = i * (Math.PI * 2 / GROUND_RUNE_PARTICLES);
            double x = center.x + Math.cos(angle) * (radius * 0.95);
            double z = center.z + Math.sin(angle) * (radius * 0.95);
            double y = center.y - HEIGHT_BOTTOM + 0.05; // 精确位于y-1高度
            
            // 地面火焰向上蔓延
            double vx = (level.random.nextDouble() - 0.5) * 0.1;
            double vy = level.random.nextDouble() * 0.3;
            double vz = (level.random.nextDouble() - 0.5) * 0.1;
            
            level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME,
                x, y, z,
                1, vx, vy, vz, 0.02);
        }
        
        // 6. 火焰波纹扩散效果（新增：营造野火向外蔓延的感觉）
        for (int i = 0; i < FIRE_WAVE_PARTICLES; i++) {
            // 创建同心圆波纹效果
            double waveRadius = level.random.nextDouble() * radius;
            double angle = level.random.nextDouble() * Math.PI * 2;
            
            double x = center.x + Math.cos(angle) * waveRadius;
            double z = center.z + Math.sin(angle) * waveRadius;
            double y = center.y - HEIGHT_BOTTOM + 0.1; // 稍高于地面
            
            // 波纹向外扩散的速度
            double outwardSpeed = 0.4 + level.random.nextDouble() * 0.3;
            double vx = Math.cos(angle) * outwardSpeed;
            double vy = 0.1 + level.random.nextDouble() * 0.2;
            double vz = Math.sin(angle) * outwardSpeed;
            
            level.sendParticles(ParticleTypes.FLAME,
                x, y, z,
                1, vx, vy, vz, 0.015);
        }
        
        // 7. 高空火焰雨效果（营造从天而降的火焰感觉）
        for (int i = 0; i < 20; i++) {
            double offsetX = (level.random.nextDouble() - 0.5) * RADIUS * 2;
            double offsetZ = (level.random.nextDouble() - 0.5) * RADIUS * 2;
            double x = center.x + offsetX;
            double z = center.z + offsetZ;
            double y = center.y + HEIGHT_TOP; // 从最高点开始
            
            // 火焰向下坠落
            double vx = (level.random.nextDouble() - 0.5) * 0.2;
            double vy = -0.5 - level.random.nextDouble() * 0.5; // 向下速度
            double vz = (level.random.nextDouble() - 0.5) * 0.2;
            
            level.sendParticles(ParticleTypes.FALLING_LAVA,
                x, y, z,
                1, vx, vy, vz, 0.01);
        }
    }
}