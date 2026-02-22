package net.everla.everlaartifacts.common.difficulty;

import net.everla.everlaartifacts.server.handlers.difficulty.WorldSeedChecker;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Extra难度伤害增强处理器
 * 当世界为Extra难度时，玩家受到的所有类型伤害提升50%
 */
@Mod.EventBusSubscriber(modid = "everlaartifacts")
public class ExtraDamageHandler {
    
    /**
     * 监听生物受伤事件，在Extra难度下增强玩家受到的伤害
     */
    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onLivingHurt(LivingHurtEvent event) {
        // 只在服务端处理
        if (event.getEntity().level().isClientSide()) {
            return;
        }
        
        // 只处理玩家受到的伤害
        if (!(event.getEntity() instanceof Player)) {
            return;
        }
        
        // 检查是否为Extra难度世界
        if (!isExtraDifficultyWorld(event.getEntity().level())) {
            return;
        }
        
        // 获取原始伤害值
        float originalDamage = event.getAmount();
        
        // 增加50%伤害
        float extraDamage = originalDamage * 1.5f;
        
        // 设置增强后的伤害值
        event.setAmount(extraDamage);
    }
    
    /**
     * 检查当前世界是否为Extra难度
     * 
     * @param level 世界对象
     * @return 如果是Extra难度返回true，否则返回false
     */
    private static boolean isExtraDifficultyWorld(net.minecraft.world.level.Level level) {
        // 检查是否为特殊种子世界
        if (!WorldSeedChecker.isSpecialSeedWorld()) {
            return false;
        }
        
        // 检查服务器难度是否为困难（Extra难度的基础）
        if (level.getDifficulty() != Difficulty.HARD) {
            return false;
        }
        
        // 检查当前难度级别是否为EXTRA
        DifficultyLevel currentDifficulty = WorldSeedChecker.getCurrentWorldDifficulty(level.getServer());
        return currentDifficulty == DifficultyLevel.EXTRA;
    }
}