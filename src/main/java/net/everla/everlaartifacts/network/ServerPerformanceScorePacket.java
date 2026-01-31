package net.everla.everlaartifacts.network;

import net.everla.everlaartifacts.PerformanceBasedThingsHandler;
import net.everla.everlaartifacts.EverlaartifactsMod;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.fml.loading.FMLEnvironment;

import java.util.function.Supplier;

public class ServerPerformanceScorePacket {
    private double performanceScore;

    public ServerPerformanceScorePacket(double performanceScore) {
        this.performanceScore = performanceScore;
    }

    public ServerPerformanceScorePacket(FriendlyByteBuf buffer) {
        this.performanceScore = buffer.readDouble();
    }

    public void encode(FriendlyByteBuf buffer) {
        buffer.writeDouble(performanceScore);
    }

    public static void handle(ServerPerformanceScorePacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            try {
                // 在客户端处理接收到的性能评分
                if (net.minecraftforge.fml.loading.FMLEnvironment.dist.isClient()) {
                    // 保存性能评分到客户端的存储中
                    net.everla.everlaartifacts.PerformanceMetrics.setClientPerformanceScore(packet.performanceScore);
                }
            } catch (Exception e) {
                // 处理错误，忽略错误
            }
        });
        context.setPacketHandled(true);
    }
}