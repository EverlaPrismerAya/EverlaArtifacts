package net.everla.everlaartifacts.server.handlers.difficulty;

import net.everla.everlaartifacts.EverlaartifactsMod;
import net.everla.everlaartifacts.common.game_rules.EnableLunaticMode;
import net.everla.everlaartifacts.server.network.DifficultySyncPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.GameRules;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 难度同步处理器
 * 负责在各种时机向客户端发送难度状态同步包
 */
@Mod.EventBusSubscriber(modid = "everlaartifacts")
public class DifficultySyncHandler {
    
    /**
     * 服务器启动时同步难度状态
     * 特殊种子世界下确保启用月狂模式游戏规则
     */
    @SubscribeEvent
    public static void onServerStarted(ServerStartedEvent event) {
        MinecraftServer server = event.getServer();
        
        // 检查是否为特殊种子世界
        if (WorldSeedChecker.isSpecialSeedWorld()) {
            GameRules gameRules = server.getGameRules();
            boolean isLunaticModeEnabled = gameRules.getBoolean(EnableLunaticMode.ENABLE_LUNATIC_MODE);
            
            // 仅在非困难难度时执行难度设置和锁定
            if (server.getWorldData().getDifficulty() != Difficulty.HARD) {
                server.setDifficulty(net.minecraft.world.Difficulty.HARD, true);
                server.setDifficultyLocked(true);
            }
            
            // 如果月狂模式未启用，则启用它
            if (!isLunaticModeEnabled) {
                gameRules.getRule(EnableLunaticMode.ENABLE_LUNATIC_MODE).set(true, server);
            }
        }
        
        syncAllPlayers(server);
    }
    
    /**
     * 玩家登录时同步难度状态
     * 特殊种子世界下确保启用月狂模式游戏规则
     */
    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer serverPlayer) {
            MinecraftServer server = serverPlayer.server;
            
            // 检查是否为特殊种子世界
            if (WorldSeedChecker.isSpecialSeedWorld()) {
                GameRules gameRules = server.getGameRules();
                boolean isLunaticModeEnabled = gameRules.getBoolean(EnableLunaticMode.ENABLE_LUNATIC_MODE);

                // 仅在非困难难度时执行难度设置和锁定
                if (server.getWorldData().getDifficulty() != Difficulty.HARD) {
                    server.setDifficulty(net.minecraft.world.Difficulty.HARD, true);
                    server.setDifficultyLocked(true);
                }

                // 如果月狂模式未启用，则启用它
                if (!isLunaticModeEnabled) {
                    gameRules.getRule(EnableLunaticMode.ENABLE_LUNATIC_MODE).set(true, server);
                }
            }
            
            syncSinglePlayer(serverPlayer);
        }
    }
    
    /**
     * 玩家切换维度时同步难度状态（确保跨维度一致性）
     */
    @SubscribeEvent
    public static void onPlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer serverPlayer) {
            // 延迟一小段时间发送，确保维度切换完成
            EverlaartifactsMod.queueServerWork(5, () -> syncSinglePlayer(serverPlayer));
        }
    }
    
    /**
     * 向单个玩家发送难度同步包
     */
    public static void syncSinglePlayer(ServerPlayer player) {
        if (player == null || player.server == null) {
            return;
        }
        
        MinecraftServer server = player.server;
        Difficulty currentDifficulty = server.getWorldData().getDifficulty();
        GameRules gameRules = server.getGameRules();
        boolean isLunaticMode = gameRules.getBoolean(EnableLunaticMode.ENABLE_LUNATIC_MODE) 
                              && currentDifficulty == Difficulty.HARD;
        boolean isSpecialSeedWorld = WorldSeedChecker.isSpecialSeedWorld();
        
        DifficultySyncPacket.sendToClient(player, currentDifficulty, isLunaticMode, isSpecialSeedWorld);
    }
    
    /**
     * 向所有在线玩家发送难度同步包
     */
    public static void syncAllPlayers(MinecraftServer server) {
        if (server == null) {
            return;
        }
        
        Difficulty currentDifficulty = server.getWorldData().getDifficulty();
        GameRules gameRules = server.getGameRules();
        boolean isLunaticMode = gameRules.getBoolean(EnableLunaticMode.ENABLE_LUNATIC_MODE) 
                              && currentDifficulty == Difficulty.HARD;
        boolean isSpecialSeedWorld = WorldSeedChecker.isSpecialSeedWorld();
        
        // 向所有在线玩家发送同步包
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            DifficultySyncPacket.sendToClient(player, currentDifficulty, isLunaticMode, isSpecialSeedWorld);
        }
    }
    
    /**
     * 当游戏规则发生变化时调用此方法进行同步
     * 可以在其他地方调用，比如命令处理器中
     */
    public static void onGameRuleChanged(MinecraftServer server) {
        syncAllPlayers(server);
    }
    
    /**
     * 当难度通过命令等方式直接更改时调用此方法进行同步
     */
    public static void onDifficultyChanged(MinecraftServer server) {
        syncAllPlayers(server);
    }
}