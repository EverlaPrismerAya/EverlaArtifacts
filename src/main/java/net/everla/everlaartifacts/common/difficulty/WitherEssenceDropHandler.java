package net.everla.everlaartifacts.common.difficulty;

import net.everla.everlaartifacts.common.game_rules.EnableLunaticMode;
import net.everla.everlaartifacts.init.EverlaartifactsModItems;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
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
 * 同时定义凋亡精粹掉落物形态下的数据驱动防护标签（供
 * {@link net.everla.everlaartifacts.mixin.ItemEntityEverlastingMixin} 检查使用）：
 * <ul>
 *   <li>{@code everlaartifacts:explosion_resistant} — 免疫爆炸伤害</li>
 *   <li>{@code everlaartifacts:fire_resistant} — 免疫火焰与岩浆</li>
 * </ul>
 */
@Mod.EventBusSubscriber(modid = "everlaartifacts")
public class WitherEssenceDropHandler {

    /** 爆炸抗性标签 — 带有此标签的物品实体不会被爆炸摧毁 */
    public static final TagKey<Item> EXPLOSION_RESISTANT_TAG = TagKey.create(
            Registries.ITEM,
            new ResourceLocation("everlaartifacts", "explosion_resistant")
    );

    /** 火焰抗性标签 — 带有此标签的物品实体不会在火焰/岩浆中燃烧 */
    public static final TagKey<Item> FIRE_RESISTANT_TAG = TagKey.create(
            Registries.ITEM,
            new ResourceLocation("everlaartifacts", "fire_resistant")
    );

    /** 月狂模式击败凋灵掉落的凋亡精粹数量下限 */
    private static final int MIN_DROPS = 5;
    /** 月狂模式击败凋灵掉落的凋亡精粹数量上限（含） */
    private static final int MAX_DROPS = 10;

    /**
     * 检查物品栈是否带有爆炸抗性标签。
     * 供 mixin 判断掉落物是否免疫爆炸伤害使用。
     */
    public static boolean isExplosionResistant(ItemStack stack) {
        if (stack.isEmpty()) return false;
        return stack.is(EXPLOSION_RESISTANT_TAG);
    }

    /**
     * 检查物品栈是否带有火焰抗性标签。
     * 供 mixin 判断掉落物是否免疫火焰/岩浆伤害使用。
     */
    public static boolean isFireResistant(ItemStack stack) {
        if (stack.isEmpty()) return false;
        return stack.is(FIRE_RESISTANT_TAG);
    }

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
