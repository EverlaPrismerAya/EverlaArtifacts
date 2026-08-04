package net.everla.everlaartifacts.common.difficulty;

import net.everla.everlaartifacts.common.game_rules.EnableLunaticMode;
import net.everla.everlaartifacts.init.EverlaartifactsModItems;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 凋亡精粹掉落处理器
 * <p>
 * 在月狂模式下击败凋灵后，于凋灵死亡位置掉落5-10个凋亡精粹。
 * <p>
 * 数据驱动防护标签（explosion_resistant / fire_resistant）已迁移至 EverlaTweaker 模组，
 * 由 {@code net.everla.everlatweaker.common.handlers.data_driven.ProtectiveTagsHandler} 提供。
 */
@Mod.EventBusSubscriber(modid = "everlaartifacts")
public class WitherEssenceDropHandler {

    /** 月狂模式击败凋灵掉落的凋亡精粹数量下限 */
    private static final int MIN_DROPS = 5;
    /** 月狂模式击败凋灵掉落的凋亡精粹数量上限（含） */
    private static final int MAX_DROPS = 10;

    /**
     * 监听凋灵死亡事件，在月狂模式下生成凋亡精粹掉落物
     */
    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        // 只在服务端处理
        if (event.getEntity().level().isClientSide()) {
            return;
        }

        // 检查死亡的实体是否为凋灵
        if (!(event.getEntity() instanceof WitherBoss wither)) {
            return;
        }

        // 检查是否启用了月狂模式
        if (!isLunaticModeEnabled(wither.level())) {
            return;
        }

        spawnWitherEssence(wither);
    }

    /**
     * 在凋灵死亡位置生成5-10个凋亡精粹
     *
     * @param wither 已死亡的凋灵实体
     */
    private static void spawnWitherEssence(WitherBoss wither) {
        try {
            Level level = wither.level();
            // 5-10之间随机
            int count = MIN_DROPS + level.random.nextInt(MAX_DROPS - MIN_DROPS + 1);

            for (int i = 0; i < count; i++) {
                ItemStack essenceStack = new ItemStack(EverlaartifactsModItems.WITHER_ESSENCE.get(), 1);
                ItemEntity itemEntity = new ItemEntity(
                    level,
                    wither.getX(),
                    wither.getY(),
                    wither.getZ(),
                    essenceStack
                );

                // 给予随机水平速度使其散开，凋灵死亡爆炸不会摧毁它们（爆炸抗性标签）
                itemEntity.setDeltaMovement(
                    (level.random.nextDouble() - 0.5) * 0.3,
                    level.random.nextDouble() * 0.2 + 0.1,
                    (level.random.nextDouble() - 0.5) * 0.3
                );

                level.addFreshEntity(itemEntity);
            }
        } catch (Exception e) {
            System.out.println("[WitherEssenceDropHandler] 错误：生成凋亡精粹时发生异常: " + e.getMessage());
        }
    }

    /**
     * 检查当前世界是否启用了月狂模式
     *
     * @param level 世界对象
     * @return 是否处于月狂模式
     */
    private static boolean isLunaticModeEnabled(Level level) {
        // 检查世界难度是否为困难
        if (level.getDifficulty() != Difficulty.HARD) {
            return false;
        }

        // 检查月狂模式游戏规则是否启用
        GameRules gameRules = level.getGameRules();
        return gameRules.getBoolean(EnableLunaticMode.ENABLE_LUNATIC_MODE);
    }
}
