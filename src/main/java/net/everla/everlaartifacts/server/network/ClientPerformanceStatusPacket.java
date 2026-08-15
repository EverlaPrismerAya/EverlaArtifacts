package net.everla.everlaartifacts.server.network;

import net.everla.everlaartifacts.EverlaartifactsMod;
import net.everla.everlaartifacts.server.PerformanceMetrics;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * 客户端→服务端网络包：每 40 刻上报一次实时性能数据（当前 FPS、CPU 利用率与窗口分辨率），
 * 供电竞牛头（FPS 状态效果）、DeepSeek 之戒（CPU 利用率加成）与近视眼镜（分辨率加成）使用。
 * <p>
 * 将实时数据合并为一个包，减少传输开销。
 */
public class ClientPerformanceStatusPacket {

    private final double fps;
    private final int cpuLoadPercent;
    private final int windowWidth;
    private final int windowHeight;

    public ClientPerformanceStatusPacket(double fps, int cpuLoadPercent, int windowWidth, int windowHeight) {
        this.fps = fps;
        this.cpuLoadPercent = cpuLoadPercent;
        this.windowWidth = windowWidth;
        this.windowHeight = windowHeight;
    }

    public ClientPerformanceStatusPacket(FriendlyByteBuf buffer) {
        this.fps = buffer.readDouble();
        this.cpuLoadPercent = buffer.readInt();
        this.windowWidth = buffer.readInt();
        this.windowHeight = buffer.readInt();
    }

    public void encode(FriendlyByteBuf buffer) {
        buffer.writeDouble(fps);
        buffer.writeInt(cpuLoadPercent);
        buffer.writeInt(windowWidth);
        buffer.writeInt(windowHeight);
    }

    /**
     * 服务端处理：存储该玩家的 FPS、CPU 利用率与窗口分辨率。
     */
    public static void handle(ClientPerformanceStatusPacket packet,
                              Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player != null) {
                PerformanceMetrics.setPlayerFps(player.getUUID(), packet.fps);
                PerformanceMetrics.setPlayerCpuLoad(player.getUUID(), packet.cpuLoadPercent);
                PerformanceMetrics.setPlayerWindowSize(player.getUUID(), packet.windowWidth, packet.windowHeight);
            }
        });
        ctx.get().setPacketHandled(true);
    }

    /**
     * 客户端将当前 FPS、CPU 利用率与窗口分辨率合并发送到服务器。
     *
     * @param fps            当前 FPS
     * @param cpuLoadPercent 当前 CPU 利用率（百分比）
     * @param windowWidth    窗口宽度（像素）
     * @param windowHeight   窗口高度（像素）
     */
    public static void sendToServer(double fps, int cpuLoadPercent, int windowWidth, int windowHeight) {
        EverlaartifactsMod.PACKET_HANDLER.sendToServer(
                new ClientPerformanceStatusPacket(fps, cpuLoadPercent, windowWidth, windowHeight));
    }
}
