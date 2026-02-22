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
    private final boolean isSpecialSeedWorld;

    public DifficultySyncPacket(String difficultyName, boolean isLunaticMode, boolean isSpecialSeedWorld) {
        this.difficultyName = difficultyName;
        this.isLunaticMode = isLunaticMode;
        this.isSpecialSeedWorld = isSpecialSeedWorld;
    }

    public DifficultySyncPacket(FriendlyByteBuf buffer) {
        this.difficultyName = buffer.readUtf(32767);
        this.isLunaticMode = buffer.readBoolean();
        this.isSpecialSeedWorld = buffer.readBoolean();
    }

    public void encode(FriendlyByteBuf buffer) {
        buffer.writeUtf(this.difficultyName);
        buffer.writeBoolean(this.isLunaticMode);
        buffer.writeBoolean(this.isSpecialSeedWorld);
    }

    public static void handle(DifficultySyncPacket packet, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            // 在客户端处理
            Minecraft mc = Minecraft.getInstance();
            if (mc.player != null) {
                // 更新客户端的难度显示状态
                EverlaartifactsMod.setClientDifficulty(packet.difficultyName, packet.isLunaticMode);
                
                // 设置特殊种子世界标识
                setClientSpecialSeedWorld(packet.isSpecialSeedWorld);
            }
        });
        ctx.get().setPacketHandled(true);
    }

    public static void sendToClient(net.minecraft.server.level.ServerPlayer player, Difficulty difficulty, boolean isLunaticMode, boolean isSpecialSeedWorld) {
        String difficultyName = difficulty.name();
        net.minecraftforge.network.PacketDistributor.PacketTarget target = net.minecraftforge.network.PacketDistributor.PLAYER.with(() -> player);
        EverlaartifactsMod.PACKET_HANDLER.send(target, new DifficultySyncPacket(difficultyName, isLunaticMode, isSpecialSeedWorld));
    }
    
    /**
     * 兼容旧版本的方法
     */
    public static void sendToClient(net.minecraft.server.level.ServerPlayer player, Difficulty difficulty, boolean isLunaticMode) {
        sendToClient(player, difficulty, isLunaticMode, false);
    }
    
    /**
     * 设置客户端特殊种子世界标识
     */
    private static void setClientSpecialSeedWorld(boolean isSpecialSeedWorld) {
        // 这里可以通过反射或其他方式设置客户端的特殊种子标识
        // 暂时通过系统属性传递
        System.setProperty("everlaartifacts.special_seed_world", String.valueOf(isSpecialSeedWorld));
    }
    
    /**
     * 客户端检查是否为特殊种子世界
     */
    public static boolean isClientSpecialSeedWorld() {
        String value = System.getProperty("everlaartifacts.special_seed_world", "false");
        return Boolean.parseBoolean(value);
    }
}