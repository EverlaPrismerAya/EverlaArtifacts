package net.everla.everlaartifacts.server.network;

import net.everla.everlaartifacts.EverlaartifactsMod;
import net.everla.everlaartifacts.common.difficulty.DifficultyLevel;
import net.everla.everlaartifacts.common.game_rules.EnableLunaticMode;
import net.everla.everlaartifacts.server.handlers.DifficultySyncHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.Difficulty;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * 难度变更网络包
 * 用于客户端向服务端发送难度切换请求
 */
public class DifficultyChangePacket {
    private final DifficultyLevel newDifficulty;
    
    public DifficultyChangePacket(DifficultyLevel newDifficulty) {
        this.newDifficulty = newDifficulty;
    }
    
    public DifficultyChangePacket(FriendlyByteBuf buf) {
        this.newDifficulty = buf.readEnum(DifficultyLevel.class);
    }
    
    public void encode(FriendlyByteBuf buf) {
        buf.writeEnum(newDifficulty);
    }
    
    public static void handle(DifficultyChangePacket packet, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            // 在服务端处理难度变更
            net.minecraft.server.level.ServerPlayer player = ctx.get().getSender();
            if (player != null) {
                net.minecraft.server.MinecraftServer server = player.server;
                net.minecraft.world.level.GameRules gameRules = server.getGameRules();
                
                // 设置难度
                server.setDifficulty(packet.newDifficulty.toVanillaDifficulty(), true);
                
                // 处理月狂模式游戏规则
                if (packet.newDifficulty == DifficultyLevel.LUNATIC) {
                    // 切换到月狂模式：启用enableLunaticMode规则
                    gameRules.getRule(EnableLunaticMode.ENABLE_LUNATIC_MODE).set(true, server);
                } else {
                    // 切换到其他难度：禁用enableLunaticMode规则
                    gameRules.getRule(EnableLunaticMode.ENABLE_LUNATIC_MODE).set(false, server);
                }
                
                // 发送消息给所有玩家
                String playerName = player.getName().getString();
                String difficultyKey = switch (packet.newDifficulty) {
                    case EASY -> "difficulty.everlaartifacts.easy";
                    case NORMAL -> "difficulty.everlaartifacts.normal";
                    case HARD -> "difficulty.everlaartifacts.hard";
                    case LUNATIC -> "difficulty.everlaartifacts.lunatic";
                    default -> "difficulty.everlaartifacts.normal";
                };
                
                server.getPlayerList().broadcastSystemMessage(
                    net.minecraft.network.chat.Component.translatable(
                        "message.everlaartifacts.difficulty_change", 
                        playerName,
                        net.minecraft.network.chat.Component.translatable(difficultyKey)
                    ),
                    false
                );
                
                // 使用专门的同步处理器来通知所有玩家
                DifficultySyncHandler.onDifficultyChanged(server);
            }
        });
        ctx.get().setPacketHandled(true);
    }
}