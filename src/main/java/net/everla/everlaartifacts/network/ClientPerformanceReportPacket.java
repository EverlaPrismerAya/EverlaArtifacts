package net.everla.everlaartifacts.network;

import net.everla.everlaartifacts.PerformanceMetrics;
import net.everla.everlaartifacts.EverlaartifactsMod;
import net.everla.everlaartifacts.PerformanceBasedThingsHandler;
import net.everla.everlaartifacts.config.EverlaArtifactsConfig;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.fml.loading.FMLEnvironment;

import java.util.function.Supplier;

public class ClientPerformanceReportPacket {
    private double realPerformanceScore;
    private double debugPerformanceScore;
    private int cpuCount;
    private int allocatedMemory;

    public ClientPerformanceReportPacket(double realPerformanceScore, double debugPerformanceScore, int cpuCount, int allocatedMemory) {
        this.realPerformanceScore = realPerformanceScore;
        this.debugPerformanceScore = debugPerformanceScore;
        this.cpuCount = cpuCount;
        this.allocatedMemory = allocatedMemory;
    }

    public ClientPerformanceReportPacket(FriendlyByteBuf buffer) {
        this.realPerformanceScore = buffer.readDouble();
        this.debugPerformanceScore = buffer.readDouble();
        this.cpuCount = buffer.readInt();
        this.allocatedMemory = buffer.readInt();
    }

    public void encode(FriendlyByteBuf buffer) {
        buffer.writeDouble(realPerformanceScore);
        buffer.writeDouble(debugPerformanceScore);
        buffer.writeInt(cpuCount);
        buffer.writeInt(allocatedMemory);
    }

    public static void handle(ClientPerformanceReportPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            try {
                // 在服务端处理接收到的性能报告
                // 日志已被移除
                
                // 获取当前网络上下文中的玩家
                if (context.getSender() != null) {
                    // 根据游戏规则决定使用哪个性能评分
                    double performanceScoreToUse;
                    boolean forceUseTruePerformance = context.getSender().level().getGameRules().getBoolean(net.everla.everlaartifacts.game_rules.ForceUseTruePerformance.FORCE_USE_TRUE_PERFORMANCE);
                    
                    if (forceUseTruePerformance) {
                        // 如果启用了强制使用真实性能，使用真实性能评分
                        performanceScoreToUse = packet.realPerformanceScore;
                    } else {
                        // 否则根据调试模式决定使用哪个性能评分
                        if (EverlaArtifactsConfig.isPerformanceDebugMode()) {
                            performanceScoreToUse = packet.debugPerformanceScore;
                        } else {
                            // 如果调试模式未开启，则使用真实信息
                            performanceScoreToUse = packet.realPerformanceScore;
                        }
                    }
                    
                    // 将性能评分存储到玩家的持久化数据中
                    PerformanceBasedThingsHandler.setPlayerPerformanceScore(context.getSender(), performanceScoreToUse);
                    
                    // 同时向客户端发送性能评分，用于物品提示显示
                    EverlaartifactsMod.sendPerformanceScoreToClient(context.getSender(), performanceScoreToUse);
                } else {
                    // 无法获取发送性能报告的玩家信息，忽略
                }
                
            } catch (Exception e) {
                // 处理错误，忽略错误
            }
        });
        context.setPacketHandled(true);
    }
}