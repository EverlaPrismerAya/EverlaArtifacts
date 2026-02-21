package net.everla.everlaartifacts.server.network;

import net.everla.everlaartifacts.EverlaartifactsMod;
import net.everla.everlaartifacts.client.screens.BloodBlossomScreenOverlay;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

public class BloodBlossomEntityPacket {
    private final List<EntityData> entities;

    public static class EntityData {
        public final UUID uuid; // 实体UUID
        public final double x, y, z; // 世界坐标
        
        public EntityData(UUID uuid, double x, double y, double z) {
            this.uuid = uuid;
            this.x = x;
            this.y = y;
            this.z = z;
        }
    }

    public BloodBlossomEntityPacket(List<EntityData> entities) {
        this.entities = entities;
    }

    public BloodBlossomEntityPacket(FriendlyByteBuf buf) {
        int count = buf.readInt();
        this.entities = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            UUID uuid = buf.readUUID();
            double x = buf.readDouble();
            double y = buf.readDouble();
            double z = buf.readDouble();
            this.entities.add(new EntityData(uuid, x, y, z));
        }
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeInt(entities.size());
        for (EntityData data : entities) {
            buf.writeUUID(data.uuid);
            buf.writeDouble(data.x);
            buf.writeDouble(data.y);
            buf.writeDouble(data.z);
        }
    }

    public static void handle(BloodBlossomEntityPacket packet, Supplier<NetworkEvent.Context> ctx) {
        if (ctx.get().getDirection() == NetworkDirection.PLAY_TO_SERVER) {
            ctx.get().setPacketHandled(true);
            return;
        }

        ctx.get().enqueueWork(() -> {
            // 在客户端处理接收到的数据
            // 逐个更新实体位置
            for (BloodBlossomEntityPacket.EntityData data : packet.entities) {
                BloodBlossomScreenOverlay.addWorldPosition(data.uuid, data.x, data.y, data.z);
            }
            
            // 清理长时间未更新的实体位置（包括死亡或离开范围的实体）
            BloodBlossomScreenOverlay.cleanupOldPositions();
        });

        ctx.get().setPacketHandled(true);
    }

    public static void sendToClient(ServerPlayer player, List<EntityData> entities) {
        EverlaartifactsMod.PACKET_HANDLER.send(PacketDistributor.PLAYER.with(() -> player), new BloodBlossomEntityPacket(entities));
    }
}