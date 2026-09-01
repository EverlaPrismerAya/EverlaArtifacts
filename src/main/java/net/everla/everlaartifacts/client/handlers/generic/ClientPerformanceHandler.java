package net.everla.everlaartifacts.client.handlers.generic;

import net.everla.everlaartifacts.EverlaartifactsMod;
import net.everla.everlaartifacts.common.handlers.enchantment.PerformanceBasedThingsHandler;
import net.everla.everlaartifacts.common.item.DeepSeekItem;
import net.everla.everlaartifacts.common.item.GamingCattleItem;
import net.everla.everlaartifacts.common.item.GlassesItem;
import net.everla.everlaartifacts.init.EverlaartifactsModItems;
import net.everla.everlaartifacts.server.PerformanceMetrics;
import net.everla.everlaartifacts.common.config.EverlaArtifactsConfig;
import net.everla.everlaartifacts.server.network.ClientHardwareInfoPacket;
import net.everla.everlaartifacts.server.network.ClientModCountPacket;
import net.everla.everlaartifacts.server.network.ClientPerformanceReportPacket;
import net.everla.everlaartifacts.server.network.ClientDeepSeekBonusPacket;
import net.everla.everlaartifacts.server.network.ClientGamingCattleEffectPacket;
import net.everla.everlaartifacts.server.network.ClientGlassesBonusPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import top.theillusivec4.curios.api.CuriosApi;

@Mod.EventBusSubscriber(modid = "everlaartifacts", value = Dist.CLIENT)
public class ClientPerformanceHandler {

    // FPS 遥测：每 40 刻（2秒）采样一次，仅在佩戴对应饰品且运算结果有变动时上报
    private static final int FPS_REPORT_INTERVAL_TICKS = 40;
    private static int fpsReportTickCounter = 0;
    private static double currentFps = 0.0;
    // 上一渲染帧的墙钟时间（纳秒），用于计算真实帧间隔
    private static long lastRenderNanos = 0L;

    // 各饰品「上次已发送」的属性结果缓存：未佩戴或结果未变时不重发
    private static Double lastSentGlassesBonus = null;
    private static Double lastSentDeepSeekBonus = null;
    private static Integer lastSentGamingCattleMask = null;

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

    // 每 40 刻对采样值求平均后，按饰品分别上报「应应用的属性结果」到服务端：
    // 仅在佩戴对应饰品，且运算结果相对上次上报有变动时才发送（不传输原始硬件信息）
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

        // 读取当前窗口分辨率并缓存（近视眼镜等 Tooltip 使用），无论是否上报都要更新
        com.mojang.blaze3d.platform.Window window = mc.getWindow();
        if (window != null) {
            int w = window.getWidth();
            int h = window.getHeight();
            if (w > 0 && h > 0) {
                PerformanceMetrics.setLatestClientWindowSize(w, h);
            }
        }

        Player player = mc.player;

        try {
            // 近视眼镜：仅佩戴且攻击力修正有变动时上报
            if (hasTrinketEquipped(player, EverlaartifactsModItems.GLASSES.get())) {
                int w = PerformanceMetrics.getLatestClientWindowWidth();
                int h = PerformanceMetrics.getLatestClientWindowHeight();
                double bonus = GlassesItem.calculateDamageMultiplier(w, h) - 1.0;
                if (lastSentGlassesBonus == null || lastSentGlassesBonus != bonus) {
                    lastSentGlassesBonus = bonus;
                    ClientGlassesBonusPacket.sendToServer(bonus);
                }
            } else {
                lastSentGlassesBonus = null;
            }

            // DeepSeek 之戒：仅佩戴且攻击力修正有变动时上报
            if (hasTrinketEquipped(player, EverlaartifactsModItems.DEEPSEEK.get())) {
                int cpuLoad = PerformanceMetrics.getLatestClientCpuLoad();
                double bonus = DeepSeekItem.calculateDamageMultiplier(cpuLoad) - 1.0;
                if (lastSentDeepSeekBonus == null || lastSentDeepSeekBonus != bonus) {
                    lastSentDeepSeekBonus = bonus;
                    ClientDeepSeekBonusPacket.sendToServer(bonus);
                }
            } else {
                lastSentDeepSeekBonus = null;
            }

            // 电竞牛头：仅佩戴且效果掩码有变动时上报
            if (hasTrinketEquipped(player, EverlaartifactsModItems.GAMING_CATTLE.get())) {
                int mask = GamingCattleItem.targetEffectMask(currentFps);
                if (lastSentGamingCattleMask == null || lastSentGamingCattleMask != mask) {
                    lastSentGamingCattleMask = mask;
                    ClientGamingCattleEffectPacket.sendToServer(mask);
                }
            } else {
                lastSentGamingCattleMask = null;
            }
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

        // 重置 FPS 遥测状态与各饰品上报缓存，避免断线重连后携带旧数据
        fpsReportTickCounter = 0;
        currentFps = 0.0;
        lastRenderNanos = 0L;
        lastSentGlassesBonus = null;
        lastSentDeepSeekBonus = null;
        lastSentGamingCattleMask = null;

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

    private static boolean curiosLoaded = false;
    private static boolean curiosChecked = false;

    /** 懒加载并缓存 Curios 是否已加载（客户端侧） */
    private static boolean isCuriosLoaded() {
        if (!curiosChecked) {
            curiosChecked = true;
            try {
                ModList modList = ModList.get();
                curiosLoaded = modList != null && modList.isLoaded("curios");
            } catch (Exception e) {
                curiosLoaded = false;
            }
        }
        return curiosLoaded;
    }

    /**
     * 判定本地玩家是否佩戴了指定饰品：原版头盔/副手槽位兜底，或 Curios 饰品栏。
     */
    private static boolean hasTrinketEquipped(Player player, Item item) {
        if (player == null) {
            return false;
        }
        if (player.getItemBySlot(EquipmentSlot.HEAD).getItem() == item) {
            return true;
        }
        if (player.getOffhandItem().getItem() == item) {
            return true;
        }
        if (isCuriosLoaded()) {
            return hasInCurios(player, item);
        }
        return false;
    }

    /** 仅当 Curios 加载时调用，避免引用不存在的类 */
    private static boolean hasInCurios(Player player, Item item) {
        return CuriosApi.getCuriosInventory(player)
                .resolve()
                .map(inventory -> inventory.isEquipped(item))
                .orElse(false);
    }
}
