package net.everla.everlaartifacts.server.network;

import net.everla.everlaartifacts.EverlaartifactsMod;
import net.everla.everlaartifacts.server.PerformanceMetrics;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * 客户端→服务端网络包：上报「电竞牛头」当前应施加的状态效果掩码。
 * <p>
 * 只传输要应用的效果集合（位掩码，见 {@code GamingCattleItem} 的位定义），
 * 不传输 FPS 等原始硬件信息。电竞牛头佩戴且效果掩码有变动时才由客户端发送
 * （见 {@code ClientPerformanceHandler}）。
 */
public class ClientGamingCattleEffectPacket {

    private final int effectMask;

    public ClientGamingCattleEffectPacket(int effectMask) {
        this.effectMask = effectMask;
    }

    public ClientGamingCattleEffectPacket(FriendlyByteBuf buffer) {
        this.effectMask = buffer.readInt();
    }

    public void encode(FriendlyByteBuf buffer) {
        buffer.writeInt(effectMask);
    }

    /** 服务端处理：存储该玩家应施加的状态效果掩码。 */
    public static void handle(ClientGamingCattleEffectPacket packet, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player != null) {
                PerformanceMetrics.setPlayerGamingCattleMask(player.getUUID(), packet.effectMask);
            }
        });
        ctx.get().setPacketHandled(true);
    }

    /** 客户端将计算好的效果掩码发送到服务器。 */
    public static void sendToServer(int effectMask) {
        EverlaartifactsMod.PACKET_HANDLER.sendToServer(new ClientGamingCattleEffectPacket(effectMask));
    }
}
