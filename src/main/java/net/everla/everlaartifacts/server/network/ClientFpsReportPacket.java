package net.everla.everlaartifacts.server.network;

import net.everla.everlaartifacts.EverlaartifactsMod;
import net.everla.everlaartifacts.server.PerformanceMetrics;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * 客户端→服务端网络包：周期性上报玩家的平均 FPS，用于性能遥测。
 * <p>
 * 客户端每 40 刻（2 秒）对最近采样的 FPS 求平均后发送一次，
 * 服务端按玩家 UUID 存入 {@link PerformanceMetrics}。
 */
public class ClientFpsReportPacket {

    private final double averageFps;

    public ClientFpsReportPacket(double averageFps) {
        this.averageFps = averageFps;
    }

    public ClientFpsReportPacket(FriendlyByteBuf buffer) {
        this.averageFps = buffer.readDouble();
    }

    public void encode(FriendlyByteBuf buffer) {
        buffer.writeDouble(averageFps);
    }

    /**
     * 服务端处理：存储该玩家的平均 FPS。
     */
    public static void handle(ClientFpsReportPacket packet,
                              Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player != null) {
                PerformanceMetrics.setPlayerFps(player.getUUID(), packet.averageFps);
            }
        });
        ctx.get().setPacketHandled(true);
    }

    /**
     * 客户端将平均 FPS 发送到服务器。
     *
     * @param averageFps 平均 FPS
     */
    public static void sendToServer(double averageFps) {
        EverlaartifactsMod.PACKET_HANDLER.sendToServer(new ClientFpsReportPacket(averageFps));
    }
}
