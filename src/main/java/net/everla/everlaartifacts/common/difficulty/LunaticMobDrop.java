package net.everla.everlaartifacts.common.difficulty;

import net.everla.everlaartifacts.common.game_rules.EnableLunaticMode;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.WitherSkeleton;
import net.minecraft.world.entity.monster.Blaze;
import net.minecraft.world.entity.monster.EnderMan;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * 月狂模式下的生物战利品控制系统
 * 处理月狂模式下特定生物的掉落物调整
 */
@Mod.EventBusSubscriber(modid = "everlaartifacts")
public class LunaticMobDrop {
    
    private static final Random random = new Random();
    
    /**
     * 监听生物死亡掉落事件，动态修改月狂模式下的掉落物
     */
    @SubscribeEvent
    public static void onLivingDrops(LivingDropsEvent event) {
        // 只在服务端处理
        if (event.getEntity().level().isClientSide()) {
            return;
        }
        
        // 检查是否启用了月狂模式
        if (!isLunaticModeEnabled(event.getEntity().level())) {
            return;
        }
        
        // 处理特定生物的掉落物调整
        if (event.getEntity() instanceof WitherSkeleton) {
            handleWitherSkeletonDrops(event);
        } else if (event.getEntity() instanceof Blaze) {
            handleBlazeDrops(event);
        } else if (event.getEntity() instanceof EnderMan) {
            handleEndermanDrops(event);
        }
        
        // 可以在这里添加更多生物的掉落处理
        // 例如：
        // else if (event.getEntity() instanceof Blaze) {
        //     handleBlazeDrops(event);
        // }
    }
    
    /**
     * 检查当前世界是否启用了月狂模式
     * 
     * @param level 世界对象
     * @return 是否处于月狂模式
     */
    private static boolean isLunaticModeEnabled(net.minecraft.world.level.Level level) {
        // 检查世界难度是否为困难
        if (level.getDifficulty() != Difficulty.HARD) {
            return false;
        }
        
        // 检查月狂模式游戏规则是否启用
        GameRules gameRules = level.getGameRules();
        return gameRules.getBoolean(EnableLunaticMode.ENABLE_LUNATIC_MODE);
    }
    
    /**
     * 处理凋零骷髅的掉落物调整
     * 在月狂模式下将凋零骷髅头的掉落概率提升到33%
     * 
     * @param event 生物掉落事件
     */
    private static void handleWitherSkeletonDrops(LivingDropsEvent event) {
        List<ItemEntity> dropsToAdd = new ArrayList<>();
        
        // 检查是否已经掉落了凋零骷髅头
        boolean hasSkullDrop = false;
        for (ItemEntity drop : event.getDrops()) {
            if (drop.getItem().getItem() == Items.WITHER_SKELETON_SKULL) {
                hasSkullDrop = true;
                break;
            }
        }
        
        // 如果还没有掉落头颅，根据月狂模式概率决定是否添加
        if (!hasSkullDrop) {
            // 月狂模式下33%概率掉落凋零骷髅头
            if (random.nextFloat() < 0.33f) {
                ItemStack skullStack = new ItemStack(Items.WITHER_SKELETON_SKULL, 1);
                ItemEntity skullEntity = new ItemEntity(
                    event.getEntity().level(),
                    event.getEntity().getX(),
                    event.getEntity().getY(),
                    event.getEntity().getZ(),
                    skullStack
                );
                dropsToAdd.add(skullEntity);
            }
        }
        
        // 添加额外的掉落物（月狂模式专属）
        // 50%概率额外掉落1个煤炭
        if (random.nextFloat() < 0.5f) {
            ItemStack coalStack = new ItemStack(Items.COAL, 1);
            ItemEntity coalEntity = new ItemEntity(
                event.getEntity().level(),
                event.getEntity().getX(),
                event.getEntity().getY(),
                event.getEntity().getZ(),
                coalStack
            );
            dropsToAdd.add(coalEntity);
        }
        
        // 20%概率额外掉落2个骨头
        if (random.nextFloat() < 0.2f) {
            ItemStack boneStack = new ItemStack(Items.BONE, 2);
            ItemEntity boneEntity = new ItemEntity(
                event.getEntity().level(),
                event.getEntity().getX(),
                event.getEntity().getY(),
                event.getEntity().getZ(),
                boneStack
            );
            dropsToAdd.add(boneEntity);
        }
        
        // 将新掉落物添加到事件中
        event.getDrops().addAll(dropsToAdd);
    }
    
    /**
     * 处理烈焰人的掉落物调整
     * 在月狂模式下保底掉落1个烈焰棒
     * 
     * @param event 生物掉落事件
     */
    private static void handleBlazeDrops(LivingDropsEvent event) {
        List<ItemEntity> dropsToAdd = new ArrayList<>();
        
        // 检查是否已经掉落了烈焰棒
        boolean hasRodDrop = false;
        for (ItemEntity drop : event.getDrops()) {
            if (drop.getItem().getItem() == Items.BLAZE_ROD) {
                hasRodDrop = true;
                break;
            }
        }
        
        // 如果还没有掉落烈焰棒，则保底掉落1个
        if (!hasRodDrop) {
            ItemStack rodStack = new ItemStack(Items.BLAZE_ROD, 1);
            ItemEntity rodEntity = new ItemEntity(
                event.getEntity().level(),
                event.getEntity().getX(),
                event.getEntity().getY(),
                event.getEntity().getZ(),
                rodStack
            );
            dropsToAdd.add(rodEntity);
        }
        
        // 额外奖励：30%概率额外掉落1个烈焰粉
        if (random.nextFloat() < 0.3f) {
            ItemStack powderStack = new ItemStack(Items.BLAZE_POWDER, 1);
            ItemEntity powderEntity = new ItemEntity(
                event.getEntity().level(),
                event.getEntity().getX(),
                event.getEntity().getY(),
                event.getEntity().getZ(),
                powderStack
            );
            dropsToAdd.add(powderEntity);
        }
        
        // 将新掉落物添加到事件中
        event.getDrops().addAll(dropsToAdd);
    }
    
    /**
     * 处理末影人的掉落物调整
     * 在月狂模式下保底掉落1个末影珍珠
     * 
     * @param event 生物掉落事件
     */
    private static void handleEndermanDrops(LivingDropsEvent event) {
        List<ItemEntity> dropsToAdd = new ArrayList<>();
        
        // 检查是否已经掉落了末影珍珠
        boolean hasPearlDrop = false;
        for (ItemEntity drop : event.getDrops()) {
            if (drop.getItem().getItem() == Items.ENDER_PEARL) {
                hasPearlDrop = true;
                break;
            }
        }
        
        // 如果还没有掉落末影珍珠，则保底掉落1个
        if (!hasPearlDrop) {
            ItemStack pearlStack = new ItemStack(Items.ENDER_PEARL, 1);
            ItemEntity pearlEntity = new ItemEntity(
                event.getEntity().level(),
                event.getEntity().getX(),
                event.getEntity().getY(),
                event.getEntity().getZ(),
                pearlStack
            );
            dropsToAdd.add(pearlEntity);
        }
        
        // 额外奖励：25%概率额外掉落1个末影珍珠
        if (random.nextFloat() < 0.25f) {
            ItemStack extraPearlStack = new ItemStack(Items.ENDER_PEARL, 1);
            ItemEntity extraPearlEntity = new ItemEntity(
                event.getEntity().level(),
                event.getEntity().getX(),
                event.getEntity().getY(),
                event.getEntity().getZ(),
                extraPearlStack
            );
            dropsToAdd.add(extraPearlEntity);
        }
        
        // 将新掉落物添加到事件中
        event.getDrops().addAll(dropsToAdd);
    }
    
    /**
     * 获取凋零骷髅在月狂模式下的头颅掉落概率
     * 
     * @param isLunaticMode 是否处于月狂模式
     * @return 掉落概率 (0.0 - 1.0)
     */
    public static float getWitherSkeletonHeadDropChance(boolean isLunaticMode) {
        if (isLunaticMode) {
            return 0.33f; // 月狂模式：33%概率
        } else {
            return 0.025f; // 普通模式：2.5%概率（原版默认值）
        }
    }
    
    /**
     * 获取特定生物在月狂模式下的额外掉落物信息
     * 
     * @param entityType 生物类型
     * @param isLunaticMode 是否处于月狂模式
     * @return 掉落物描述字符串
     */
    public static String getLunaticDropInfo(EntityType<?> entityType, boolean isLunaticMode) {
        if (!isLunaticMode) {
            return "普通掉落物";
        }
        
        if (entityType == EntityType.WITHER_SKELETON) {
            return "月狂模式：凋零骷髅头掉落率提升至33%，额外50%概率掉落煤炭，20%概率掉落骨头";
        } else if (entityType == EntityType.BLAZE) {
            return "月狂模式：保底掉落1个烈焰棒，30%概率额外掉落烈焰粉";
        } else if (entityType == EntityType.ENDERMAN) {
            return "月狂模式：保底掉落1个末影珍珠，25%概率额外掉落末影珍珠";
        }
        
        return "月狂模式：特殊掉落加成已启用";
    }
}