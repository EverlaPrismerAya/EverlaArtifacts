package net.everla.everlaartifacts.potion;

import net.everla.everlaartifacts.init.EverlaartifactsModParticleTypes;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;
import java.util.Random;

@Mod.EventBusSubscriber
public class BloodBlossomParticleHandler {

    private static final Random RANDOM = new Random();

    @SubscribeEvent
    public static void onLivingUpdate(LivingEvent.LivingTickEvent event) {
        LivingEntity entity = event.getEntity();

        // 检查实体是否具有BloodBlossom状态效果
        if (entity.hasEffect(net.everla.everlaartifacts.init.EverlaartifactsModMobEffects.BLOOD_BLOSSOM.get())) {
            // 每隔几tick生成一次粒子，以减少频率
            if (entity.level().isClientSide() || entity.level().getGameTime() % 4 == 0) {
                // 在实体边界框内随机位置生成粒子
                // 75% GoldButterfly, 25% FireButterfly
                if (RANDOM.nextDouble() < 0.75) {
                    generateParticleInBoundingBox(entity, EverlaartifactsModParticleTypes.GOLD_BUTTERFLY.get());
                } else {
                    generateParticleInBoundingBox(entity, EverlaartifactsModParticleTypes.FIRE_BUTTERFLY.get());
                }
            }
        }
    }

    private static void generateParticleInBoundingBox(LivingEntity entity, ParticleOptions particleData) {
        if (entity.level() instanceof ServerLevel serverLevel) {
            // 获取实体边界框
            double x = entity.getX();
            double y = entity.getY();
            double z = entity.getZ();
            
            double minX = x - entity.getBbWidth() / 2;
            double minY = y;
            double minZ = z - entity.getBbWidth() / 2;
            double maxX = x + entity.getBbWidth() / 2;
            double maxY = y + entity.getBbHeight();
            double maxZ = z + entity.getBbWidth() / 2;

            // 在边界框内随机位置生成粒子
            double particleX = minX + serverLevel.random.nextDouble() * (maxX - minX);
            double particleY = minY + serverLevel.random.nextDouble() * (maxY - minY);
            double particleZ = minZ + serverLevel.random.nextDouble() * (maxZ - minZ);

            // 添加轻微的速度分量
            double vx = (serverLevel.random.nextDouble() - 0.5) * 0.1;
            double vy = (serverLevel.random.nextDouble() - 0.5) * 0.1;
            double vz = (serverLevel.random.nextDouble() - 0.5) * 0.1;

            // 获取附近的玩家并发送粒子
            List<net.minecraft.server.level.ServerPlayer> players = serverLevel.getPlayers(
                player -> player.distanceToSqr(x, y, z) <= 64 * 64 // 64方块半径
            );

            for (net.minecraft.server.level.ServerPlayer player : players) {
                serverLevel.sendParticles(
                    player,
                    particleData,
                    true, // force
                    particleX, particleY, particleZ,
                    1, // count
                    vx, vy, vz, // offset
                    0.1 // speed
                );
            }
        }
    }
}