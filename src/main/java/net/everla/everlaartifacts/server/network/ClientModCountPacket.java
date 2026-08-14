package net.everla.everlaartifacts.server.network;

import net.everla.everlaartifacts.EverlaartifactsMod;
import net.everla.everlaartifacts.common.item.AtmRingItem;
import net.everla.everlaartifacts.server.PerformanceMetrics;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * 客户端→服务端网络包：玩家进入游戏时上报本机安装的模组数，
 * 供 ATM 之戒按模组数计算最终伤害加成。
 */
public class ClientModCountPacket {

    private final int modCount;

    public ClientModCountPacket(int modCount) {
        this.modCount = modCount;
    }

    public ClientModCountPacket(FriendlyByteBuf buffer) {
        this.modCount = buffer.readInt();
    }

    public void encode(FriendlyByteBuf buffer) {
        buffer.writeInt(modCount);
    }

    /**
     * 服务端处理：存储该玩家的模组数。
     */
    public static void handle(ClientModCountPacket packet,
                              Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player != null) {
                PerformanceMetrics.setPlayerModCount(player.getUUID(), packet.modCount);
                EverlaartifactsMod.LOGGER.info(
                        "收到玩家 {} 的已安装模组数: {}",
                        player.getGameProfile().getName(), packet.modCount);
            }
        });
        ctx.get().setPacketHandled(true);
    }

    /**
     * 客户端统计本机安装模组数并发送到服务器。
     */
    public static void sendToServer() {
        int modCount = AtmRingItem.getInstalledModCount();
        EverlaartifactsMod.LOGGER.info("检测到本机已安装模组数: {}", modCount);
        EverlaartifactsMod.PACKET_HANDLER.sendToServer(new ClientModCountPacket(modCount));
    }
}
