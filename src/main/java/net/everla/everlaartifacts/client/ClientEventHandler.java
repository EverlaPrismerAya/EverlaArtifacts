package net.everla.everlaartifacts.client;

import net.everla.everlaartifacts.EverlaartifactsMod;
import net.everla.everlaartifacts.PerformanceBasedThingsHandler;
import net.everla.everlaartifacts.PerformanceMetrics;
import net.everla.everlaartifacts.config.EverlaArtifactsConfig;
import net.everla.everlaartifacts.network.ClientPerformanceReportPacket;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.NetworkDirection;

@Mod.EventBusSubscriber(modid = "everlaartifacts", value = Dist.CLIENT)
public class ClientEventHandler {
    
    // 使用 ClientPlayerNetworkEvent.LoggedInEvent 替代 PlayerEvent.PlayerLoggedInEvent
    @SubscribeEvent
    public static void onClientPlayerLogin(ClientPlayerNetworkEvent.LoggingIn event) {
        EverlaartifactsMod.LOGGER.info("客户端连接到服务器，开始发送性能评分数据");
        
        // 在客户端环境，处理性能评分
        try {
            // 获取连接对象
            Object connection = event.getConnection();
            
            // 简化逻辑：只要存在连接对象，就尝试发送性能报告到服务器
            if (connection != null) {
                // 存在连接，发送性能报告到服务器
                // 获取真实性能评分
                int realCPUCount = Runtime.getRuntime().availableProcessors();
                int realAllocatedMemory = (int)(Runtime.getRuntime().maxMemory() / (1024 * 1024));
                double realPerformanceScore = PerformanceMetrics.calculateTotalScore(realCPUCount, realAllocatedMemory);
                
                // 获取调试性能评分（如果启用）
                double debugPerformanceScore;
                if (EverlaArtifactsConfig.isPerformanceDebugMode()) {
                    int debugCPUCount = PerformanceMetrics.getClientCPUCount();
                    int debugMemorySize = PerformanceMetrics.getClientAllocatedMemory();
                    debugPerformanceScore = PerformanceMetrics.calculateTotalScore(debugCPUCount, debugMemorySize);
                } else {
                    // 如果调试模式未开启，调试性能信息同样使用真实信息
                    debugPerformanceScore = realPerformanceScore;
                }
                
                // 使用正确的方法发送网络包到服务器
                try {
                    ClientPerformanceReportPacket packet = new ClientPerformanceReportPacket(realPerformanceScore, debugPerformanceScore, realCPUCount, realAllocatedMemory);
                    EverlaartifactsMod.PACKET_HANDLER.sendToServer(packet);
                    EverlaartifactsMod.LOGGER.info("成功发送性能报告到服务器 - 真实评分: {:.2f}, 调试评分: {:.2f}, CPU核心数: {}, 内存: {}MB", 
                        realPerformanceScore, debugPerformanceScore, realCPUCount, realAllocatedMemory);
                } catch (Exception e) {
                    // 发送失败，忽略错误
                }
            } else {
                // 没有连接，直接在本地设置性能评分（单人游戏情况）
                double performanceScore = PerformanceMetrics.getClientPerformanceScore();
                // 日志已被移除
                if (event.getPlayer() instanceof net.minecraft.world.entity.player.Player) {
                    net.minecraft.world.entity.player.Player player = (net.minecraft.world.entity.player.Player) event.getPlayer();
                    PerformanceBasedThingsHandler.setPlayerPerformanceScore(player, performanceScore);
                }
            }
        } catch (Exception e) {
            // 处理错误，忽略错误
        }
    }
    
    // 添加玩家登出事件处理，用于重置客户端性能评分
    @SubscribeEvent
    public static void onClientPlayerLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        // 当客户端断开与服务器的连接时，重置从服务端接收的性能评分
        // 这样可以让客户端回到使用本地计算的性能评分
        PerformanceMetrics.resetClientPerformanceScore();
    }
    
    // 添加玩家克隆事件处理，用于处理玩家从存档加载的情况
    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone event) {
        // 当玩家在单人游戏中重生或从存档加载时，复制性能评分数据
        if (event.getOriginal() != null && event.getOriginal().getPersistentData() != null) {
            net.minecraft.nbt.CompoundTag originalData = event.getOriginal().getPersistentData();
            if (originalData.contains("PerformanceScore")) {
                double score = originalData.getDouble("PerformanceScore");
                PerformanceBasedThingsHandler.setPlayerPerformanceScore(event.getEntity(), score);
                // 日志已被移除
            }
        }
    }
}