package net.everla.everlaartifacts.server.handlers.effects;

import net.everla.everlaartifacts.init.EverlaartifactsModMobEffects;
import net.everla.everlaartifacts.init.EverlaartifactsModParticleTypes;
import net.minecraft.client.Minecraft;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.WeakHashMap;
import java.util.Map;

@Mod.EventBusSubscriber(value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class HomaActiveAndBloodBlossomParticleHandler {

    private static final Minecraft minecraft = Minecraft.getInstance();
    // 缓存上次处理时间，防止同一实体在短时间内被重复处理
    private static final Map<Entity, Long> lastProcessed = new WeakHashMap<>();
    
    // 限制处理距离，只处理玩家附近一定范围内的实体
    private static final double MAX_RENDER_DISTANCE = 32.0;
    private static final double MAX_RENDER_DISTANCE_SQUARED = MAX_RENDER_DISTANCE * MAX_RENDER_DISTANCE;

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END || minecraft.level == null || minecraft.player == null) {
            return;
        }

        Level level = minecraft.level;
        long currentTime = level.getGameTime();
        
        // 降低粒子生成频率至原来的一半（每2个tick处理一次，而不是每个tick）
        if (currentTime % 2 != 0) {
            return;
        }
        
        // 获取玩家位置，用于距离检查
        double playerX = minecraft.player.getX();
        double playerY = minecraft.player.getY();
        double playerZ = minecraft.player.getZ();

        // 获取一个足够大的搜索区域，包含所有可能的实体
        java.util.List<LivingEntity> entities = level.getEntitiesOfClass(LivingEntity.class, 
            new net.minecraft.world.phys.AABB(
                playerX - MAX_RENDER_DISTANCE, playerY - MAX_RENDER_DISTANCE, playerZ - MAX_RENDER_DISTANCE,
                playerX + MAX_RENDER_DISTANCE, playerY + MAX_RENDER_DISTANCE, playerZ + MAX_RENDER_DISTANCE
            )
        );

        // 遍历所有LivingEntity类型的实体，为每个有相应状态的实体生成粒子
        for (LivingEntity livingEntity : entities) {
            // 检查实体是否在玩家渲染距离内
            double distanceSquared = livingEntity.distanceToSqr(playerX, playerY, playerZ);
            if (distanceSquared > MAX_RENDER_DISTANCE_SQUARED) {
                continue; // 距离太远，跳过处理
            }

            // 检查是否已经有处理记录，如果有且时间间隔小于一定tick数，则跳过
            Long lastTime = lastProcessed.get(livingEntity);
            if (lastTime != null && currentTime - lastTime < 4) { // 每4个tick处理一次同一实体，降低频率
                continue;
            }

            boolean hasEffect = false;

            // 检查是否具有HomaActive状态效果并生成对应粒子
            if (livingEntity.hasEffect(EverlaartifactsModMobEffects.HOMA_ACTIVE.get())) {
                // 每个tick为HomaActive实体生成1个FireButterfly粒子和1个GoldButterfly粒子
                spawnParticlesAroundEntity(level, livingEntity, EverlaartifactsModParticleTypes.FIRE_BUTTERFLY.get());
                spawnParticlesAroundEntity(level, livingEntity, EverlaartifactsModParticleTypes.GOLD_BUTTERFLY.get());
                hasEffect = true;
            }

            // 检查是否具有BloodBlossom状态效果并生成对应粒子
            if (livingEntity.hasEffect(EverlaartifactsModMobEffects.BLOOD_BLOSSOM.get())) {
                // 每个tick为BloodBlossom实体生成2个FireButterfly粒子
                spawnParticlesAroundEntity(level, livingEntity, EverlaartifactsModParticleTypes.FIRE_BUTTERFLY.get());
                spawnParticlesAroundEntity(level, livingEntity, EverlaartifactsModParticleTypes.FIRE_BUTTERFLY.get());
                hasEffect = true;
            }

            // 如果实体有任何一种效果，更新其最后处理时间
            if (hasEffect) {
                lastProcessed.put(livingEntity, currentTime);
            }
        }
    }

    private static void spawnParticlesAroundEntity(Level level, LivingEntity entity, ParticleOptions particleData) {
        // 获取实体的边界框（碰撞箱）
        double minX = entity.getX() - entity.getBbWidth() / 2;
        double minY = entity.getY();
        double minZ = entity.getZ() - entity.getBbWidth() / 2;
        double maxX = entity.getX() + entity.getBbWidth() / 2;
        double maxY = entity.getY() + entity.getBbHeight();
        double maxZ = entity.getZ() + entity.getBbWidth() / 2;

        // 在实体边界框内的随机位置生成粒子
        double x = minX + level.random.nextDouble() * (maxX - minX);
        double y = minY + level.random.nextDouble() * (maxY - minY);
        double z = minZ + level.random.nextDouble() * (maxZ - minZ);

        // 添加轻微的速度分量，让粒子看起来更生动
        double vx = (level.random.nextDouble() - 0.5) * 0.1;
        double vy = (level.random.nextDouble() - 0.5) * 0.1;
        double vz = (level.random.nextDouble() - 0.5) * 0.1;

        // 生成粒子
        level.addParticle(particleData, x, y, z, vx, vy, vz);
    }
}