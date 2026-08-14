package net.everla.everlaartifacts.server.network;

import net.everla.everlaartifacts.EverlaartifactsMod;
import net.everla.everlaartifacts.server.PerformanceMetrics;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * 客户端→服务端网络包：在玩家进入游戏时上报设备硬件信息
 * （物理内存容量与显存容量），用于性能遥测。
 * <p>
 * 与 {@link ClientPerformanceReportPacket}（性能评分）相互独立，
 * 仅负责传送内存/显存容量数据。
 */
public class ClientHardwareInfoPacket {

    private final int physicalMemoryMB;
    private final int vramMB;

    public ClientHardwareInfoPacket(int physicalMemoryMB, int vramMB) {
        this.physicalMemoryMB = physicalMemoryMB;
        this.vramMB = vramMB;
    }

    public ClientHardwareInfoPacket(FriendlyByteBuf buffer) {
        this.physicalMemoryMB = buffer.readInt();
        this.vramMB = buffer.readInt();
    }

    public void encode(FriendlyByteBuf buffer) {
        buffer.writeInt(physicalMemoryMB);
        buffer.writeInt(vramMB);
    }

    /**
     * 服务端处理：存储客户端上报的硬件信息（全局 + 按玩家）并记录日志。
     */
    public static void handle(ClientHardwareInfoPacket packet,
                              Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player != null) {
                PerformanceMetrics.receiveClientHardwareInfo(packet.physicalMemoryMB, packet.vramMB);
                PerformanceMetrics.setPlayerHardwareInfo(player, packet.physicalMemoryMB, packet.vramMB);
                EverlaartifactsMod.LOGGER.info(
                        "收到玩家 {} 的硬件信息 - 物理内存: {}MB, 显存: {}MB",
                        player.getGameProfile().getName(), packet.physicalMemoryMB, packet.vramMB);
            }
        });
        ctx.get().setPacketHandled(true);
    }

    /**
     * 客户端检测设备硬件信息（物理内存与显存容量）并发送到服务器。
     * <p>
     * 同时将检测结果缓存到本机，供千兆内存之戒等 Tooltip 展示当前硬件加成。
     */
    public static void sendToServer() {
        int physicalMemoryMB = PerformanceMetrics.detectPhysicalMemoryMB();
        int vramMB = PerformanceMetrics.detectVramMB();
        PerformanceMetrics.cacheClientHardware(physicalMemoryMB, vramMB);
        EverlaartifactsMod.LOGGER.info("检测到设备硬件信息 - 物理内存: {}MB, 显存: {}MB", physicalMemoryMB, vramMB);
        EverlaartifactsMod.PACKET_HANDLER.sendToServer(
                new ClientHardwareInfoPacket(physicalMemoryMB, vramMB));
    }
}
