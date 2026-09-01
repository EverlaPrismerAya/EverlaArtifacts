package net.everla.everlaartifacts.server.network;

import net.everla.everlaartifacts.EverlaartifactsMod;
import net.everla.everlaartifacts.server.PerformanceMetrics;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * 客户端→服务端网络包：上报「近视眼镜」当前应应用的攻击力修正
 * （generic.attack_damage 的 MULTIPLY_BASE 修饰符数值）。
 * <p>
 * 只传输要应用的属性结果，不传输分辨率等原始硬件信息。近视眼镜佩戴且
 * 运算结果有变动时才由客户端发送（见 {@code ClientPerformanceHandler}）。
 */
public class ClientGlassesBonusPacket {

    private final double damageBonus;

    public ClientGlassesBonusPacket(double damageBonus) {
        this.damageBonus = damageBonus;
    }

    public ClientGlassesBonusPacket(FriendlyByteBuf buffer) {
        this.damageBonus = buffer.readDouble();
    }

    public void encode(FriendlyByteBuf buffer) {
        buffer.writeDouble(damageBonus);
    }

    /** 服务端处理：存储该玩家应应用的攻击力修正。 */
    public static void handle(ClientGlassesBonusPacket packet, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player != null) {
                PerformanceMetrics.setPlayerGlassesBonus(player.getUUID(), packet.damageBonus);
            }
        });
        ctx.get().setPacketHandled(true);
    }

    /** 客户端将计算好的攻击力修正发送到服务器。 */
    public static void sendToServer(double damageBonus) {
        EverlaartifactsMod.PACKET_HANDLER.sendToServer(new ClientGlassesBonusPacket(damageBonus));
    }
}
