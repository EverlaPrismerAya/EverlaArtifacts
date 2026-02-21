package net.everla.everlaartifacts.server.tracker;

import net.everla.everlaartifacts.init.EverlaartifactsModMobEffects;
import net.everla.everlaartifacts.server.network.BloodBlossomEntityPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import java.util.*;

@Mod.EventBusSubscriber
public class BloodBlossomEntityTracker {
    private static final Logger LOGGER = LogManager.getLogger(BloodBlossomEntityTracker.class);
    private static int tickCounter = 0;
    private static final int TICK_INTERVAL = 5; // 每5个tick执行一次

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }

        tickCounter++;
        if (tickCounter % TICK_INTERVAL != 0) {
            return;
        }

        // 遍历所有服务器玩家并处理他们的BloodBlossom实体
        for (ServerLevel level : net.minecraftforge.server.ServerLifecycleHooks.getCurrentServer().getAllLevels()) {
            for (ServerPlayer player : level.players()) {
                processPlayer(player);
            }
        }
    }

    private static void processPlayer(ServerPlayer player) {
        // 获取玩家附近10格内的所有实体
        List<Entity> nearbyEntities = player.level().getEntities(player, player.getBoundingBox().inflate(10.0));

        // 过滤出具有BloodBlossom效果的LivingEntity，并按距离排序
        List<BloodBlossomEntityPacket.EntityData> bloodBlossomEntities = new ArrayList<>();
        for (Entity entity : nearbyEntities) {
            if (entity instanceof LivingEntity livingEntity) {
                // 检查实体是否存活
                if (livingEntity.isAlive()) {
                    MobEffectInstance effect = livingEntity.getEffect(EverlaartifactsModMobEffects.BLOOD_BLOSSOM.get());
                    if (effect != null) {
                        double distance = player.distanceTo(livingEntity);
                        if (distance <= 10.0) { // 确保距离在10格以内
                            // 发送实体UUID和世界坐标，客户端负责插值
                            bloodBlossomEntities.add(new BloodBlossomEntityPacket.EntityData(
                                livingEntity.getUUID(), // 添加实体UUID
                                livingEntity.getX(),
                                livingEntity.getY() + livingEntity.getBbHeight() * 0.5, // 在实体中部
                                livingEntity.getZ()
                            ));
                        }
                    }
                }
            }
        }

        // 只保留最近的10个实体
        bloodBlossomEntities.sort(Comparator.comparingDouble(
            data -> data.x * data.x + data.y * data.y + data.z * data.z));
        
        if (bloodBlossomEntities.size() > 10) {
            bloodBlossomEntities = bloodBlossomEntities.subList(0, 10);
        }

        // 发送数据到客户端（包含实体UUID和世界坐标）
        BloodBlossomEntityPacket.sendToClient(player, bloodBlossomEntities);
    }
}