package net.everla.everlaartifacts.server.network;

import net.everla.everlaartifacts.EverlaartifactsMod;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.Difficulty;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class DifficultySyncPacket {
    private final String difficultyName;
    private final boolean isLunaticMode;

    public DifficultySyncPacket(String difficultyName, boolean isLunaticMode) {
        this.difficultyName = difficultyName;
        this.isLunaticMode = isLunaticMode;
    }

    public DifficultySyncPacket(FriendlyByteBuf buffer) {
        this.difficultyName = buffer.readUtf(32767);
        this.isLunaticMode = buffer.readBoolean();
    }

    public void encode(FriendlyByteBuf buffer) {
        buffer.writeUtf(this.difficultyName);
        buffer.writeBoolean(this.isLunaticMode);
    }

    public static void handle(DifficultySyncPacket packet, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            // 在客户端处理
            Minecraft mc = Minecraft.getInstance();
            if (mc.player != null) {
                // 更新客户端的难度显示状态
                EverlaartifactsMod.setClientDifficulty(packet.difficultyName, packet.isLunaticMode);
            }
        });
        ctx.get().setPacketHandled(true);
    }

    public static void sendToClient(net.minecraft.server.level.ServerPlayer player, Difficulty difficulty, boolean isLunaticMode) {
        String difficultyName = difficulty.name();
        net.minecraftforge.network.PacketDistributor.PacketTarget target = net.minecraftforge.network.PacketDistributor.PLAYER.with(() -> player);
        EverlaartifactsMod.PACKET_HANDLER.send(target, new DifficultySyncPacket(difficultyName, isLunaticMode));
    }
}