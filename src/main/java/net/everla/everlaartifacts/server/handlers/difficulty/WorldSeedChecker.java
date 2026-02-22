package net.everla.everlaartifacts.server.handlers.difficulty;

import net.everla.everlaartifacts.common.difficulty.DifficultyLevel;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraft.server.MinecraftServer;

/**
 * 世界种子检测处理器
 * 检测特定种子并设置相应的难度
 */
@Mod.EventBusSubscriber(modid = "everlaartifacts")
public class WorldSeedChecker {
    
    // 特殊种子数组
    private static final long[] SPECIAL_SEEDS = {
        -940744962L,
        1621740318L,
        -735842714L,
        756225734L,
        -520265986L,
        -229414808L,
        -1798062548L,
        901677282L,
        -1105065250L,
        901677283L,
        -1005358338L,
        1879957094L,
        1686353694L
    };
    
    // 标记是否检测到特殊种子
    private static boolean isSpecialSeedWorld = false;
    
    /**
     * 服务器启动时检测世界种子
     */
    @SubscribeEvent
    public static void onServerStarted(ServerStartedEvent event) {
        MinecraftServer server = event.getServer();
        long worldSeed = server.overworld().getSeed();
        
        // 检查是否为特殊种子
        if (isSpecialSeed(worldSeed)) {
            isSpecialSeedWorld = true;
            
            // 设置服务端难度为困难（Extra难度的基础）

            
            // 同步难度状态给所有玩家
            DifficultySyncHandler.syncAllPlayers(server);
        } else {
            isSpecialSeedWorld = false;
        }
    }
    
    /**
     * 检查指定种子是否为特殊种子
     * 
     * @param seed 要检查的种子值
     * @return 如果是特殊种子返回true，否则返回false
     */
    public static boolean isSpecialSeed(long seed) {
        for (long specialSeed : SPECIAL_SEEDS) {
            if (seed == specialSeed) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * 检查当前是否为特殊种子世界
     */
    public static boolean isSpecialSeedWorld() {
        return isSpecialSeedWorld;
    }
    
    /**
     * 获取当前世界的难度级别
     * 如果是特殊种子世界，返回EXTRA难度
     */
    public static DifficultyLevel getCurrentWorldDifficulty(MinecraftServer server) {
        if (isSpecialSeedWorld) {
            return DifficultyLevel.EXTRA;
        }
        
        // 正常情况下根据服务器难度返回对应级别
        net.minecraft.world.Difficulty vanillaDifficulty = server.getWorldData().getDifficulty();
        return DifficultyLevel.fromVanillaDifficulty(vanillaDifficulty);
    }
}