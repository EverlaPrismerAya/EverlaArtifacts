package net.everla.everlaartifacts.client.handlers.generic;

import net.everla.everlaartifacts.EverlaartifactsMod;
import net.everla.everlaartifacts.common.handlers.enchantment.PerformanceBasedThingsHandler;
import net.everla.everlaartifacts.server.PerformanceMetrics;
import net.everla.everlaartifacts.common.config.EverlaArtifactsConfig;
import net.everla.everlaartifacts.server.network.ClientHardwareInfoPacket;
import net.everla.everlaartifacts.server.network.ClientModCountPacket;
import net.everla.everlaartifacts.server.network.ClientPerformanceReportPacket;
import net.everla.everlaartifacts.server.network.ClientPerformanceStatusPacket;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = "everlaartifacts", value = Dist.CLIENT)
public class ClientPerformanceHandler {

    // FPS 遥测：每 40 刻（2秒）将当前帧率直接上报一次
    private static final int FPS_REPORT_INTERVAL_TICKS = 40;
    private static int fpsReportTickCounter = 0;
    private static double currentFps = 0.0;
    // 上一渲染帧的墙钟时间（纳秒），用于计算真实帧间隔
    private static long lastRenderNanos = 0L;

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
                
                // 发送网络包到服务器
                try {
                    ClientPerformanceReportPacket packet = new ClientPerformanceReportPacket(realPerformanceScore, debugPerformanceScore, realCPUCount, realAllocatedMemory);
                    EverlaartifactsMod.PACKET_HANDLER.sendToServer(packet);
                    EverlaartifactsMod.LOGGER.info("成功发送性能报告到服务器 - 真实评分: {}, 调试评分: {}, CPU核心数: {}, 内存: {}MB",
                        realPerformanceScore, debugPerformanceScore, realCPUCount, realAllocatedMemory);
                } catch (Exception e) {
                    // 发送失败，忽略错误
                    // 什么时候出bug什么时候来草窝
                }

                // 发送设备硬件信息（物理内存容量与显存容量）到服务器，用于性能遥测
                try {
                    ClientHardwareInfoPacket.sendToServer();
                } catch (Exception e) {
                    // 发送失败，忽略错误
                }

                // 发送本机已安装模组数到服务器（ATM之戒按模组数加成）
                try {
                    ClientModCountPacket.sendToServer();
                } catch (Exception e) {
                    // 发送失败，忽略错误
                }

                // 启动 CPU 温度检测线程（DeepSeek之戒使用）
                try {
                    CpuLoadDetector.start();
                } catch (Exception e) {
                    // 启动失败，忽略错误
                }
            } else {
                // 没有连接，直接在本地设置性能评分（单人游戏情况）
                double performanceScore = PerformanceMetrics.getClientPerformanceScore();
                if (event.getPlayer() instanceof net.minecraft.world.entity.player.Player) {
                    net.minecraft.world.entity.player.Player player = (net.minecraft.world.entity.player.Player) event.getPlayer();
                    PerformanceBasedThingsHandler.setPlayerPerformanceScore(player, performanceScore);
                }
            }
        } catch (Exception e) {
            // 处理错误，忽略错误
        }
    }
    
    // 每个渲染帧用墙钟时间测量真实帧间隔（包含帧率上限的睡眠），计算真实 FPS
    @SubscribeEvent
    public static void onRenderTick(TickEvent.RenderTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        // 仅在进入世界（有玩家且已连接服务器）时采样
        if (mc.level == null || mc.player == null || mc.getConnection() == null) {
            // 离开世界时重置，防止恢复世界后首个帧间隔被算成超大值
            lastRenderNanos = 0L;
            return;
        }
        long now = System.nanoTime();
        if (lastRenderNanos != 0L) {
            long deltaNanos = now - lastRenderNanos;
            // 跳过超过 1 秒的间隔（暂停/最小化/长卡顿），不更新当前帧率
            if (deltaNanos < 1_000_000_000L) {
                currentFps = 1_000_000_000.0 / deltaNanos;
            }
        }
        lastRenderNanos = now;
    }

    // 每 40 刻（2秒）对采样值求平均后通过网络包上报到服务端
    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null || mc.getConnection() == null) {
            return;
        }

        fpsReportTickCounter++;
        if (fpsReportTickCounter < FPS_REPORT_INTERVAL_TICKS) {
            return;
        }
        fpsReportTickCounter = 0;

        // 读取当前窗口分辨率并缓存（近视眼镜 tooltip 使用）
        com.mojang.blaze3d.platform.Window window = mc.getWindow();
        if (window != null) {
            int w = window.getWidth();
            int h = window.getHeight();
            if (w > 0 && h > 0) {
                PerformanceMetrics.setLatestClientWindowSize(w, h);
            }
        }

        // 每 40 刻合并上报一次当前 FPS、CPU 利用率与窗口分辨率（减少传输开销）
        try {
            ClientPerformanceStatusPacket.sendToServer(currentFps,
                    PerformanceMetrics.getLatestClientCpuLoad(),
                    PerformanceMetrics.getLatestClientWindowWidth(),
                    PerformanceMetrics.getLatestClientWindowHeight());
        } catch (Exception e) {
            // 发送失败，忽略错误
        }
    }

    // 添加玩家登出事件处理，用于重置客户端性能评分
    @SubscribeEvent
    public static void onClientPlayerLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        // 当客户端断开与服务器的连接时，重置从服务端接收的性能评分
        // 这样可以让客户端回到使用本地计算的性能评分
        PerformanceMetrics.resetClientPerformanceScore();

        // 重置 FPS 遥测状态，避免断线重连后携带旧数据
        fpsReportTickCounter = 0;
        currentFps = 0.0;
        lastRenderNanos = 0L;

        // 停止 CPU 温度检测线程
        CpuLoadDetector.stop();
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
            }
        }
    }
}