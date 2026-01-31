package net.everla.everlaartifacts;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import net.everla.everlaartifacts.enchantment.MoneyBurnersCreedEnchantment;
import net.everla.everlaartifacts.enchantment.ScrapyardScroungerEnchantment;
import net.everla.everlaartifacts.init.EverlaartifactsModEnchantments;
import net.minecraft.network.chat.Component;
import net.everla.everlaartifacts.EverlaartifactsMod;

import java.util.WeakHashMap;
import java.util.Map;
import net.minecraft.nbt.CompoundTag;

@Mod.EventBusSubscriber(modid = "everlaartifacts", bus = Mod.EventBusSubscriber.Bus.FORGE)
public class PerformanceBasedThingsHandler {
    
    private static final String PERFORMANCE_SCORE_KEY = "PerformanceScore";
    
    // 缓存玩家的性能评分，减少重复计算
    private static final Map<Player, Double> performanceScoreCache = new WeakHashMap<>();
    private static final Map<ItemStack, Integer> moneyBurnersCreedCache = new WeakHashMap<>();
    private static final Map<ItemStack, Integer> scrapyardScroungerCache = new WeakHashMap<>();
    
    /**
     * 设置玩家的性能评分（存储在持久化数据中）
     * 
     * @param player 玩家实体
     * @param score 性能评分
     */
    public static void setPlayerPerformanceScore(Player player, double score) {
        if (player != null) {
            net.minecraft.nbt.CompoundTag persistentData = player.getPersistentData();
            if (persistentData != null) {
                persistentData.putDouble(PERFORMANCE_SCORE_KEY, score);
                
                // 清除缓存，因为评分已更新
                performanceScoreCache.remove(player);
                
                // 验证数据是否已保存
                double retrievedScore = persistentData.getDouble(PERFORMANCE_SCORE_KEY);
                
                // 检查是否确实存储了正确的值
                // 日志已被移除
            }
        }
    }
    
    /**
     * 获取玩家的性能评分（从持久化数据中读取）
     * 
     * @param player 玩家实体
     * @return 性能评分，如果没有记录则返回客户端的性能评分
     */
    public static double getPlayerPerformanceScore(Player player) {
        // 首先检查缓存
        Double cachedScore = performanceScoreCache.get(player);
        if (cachedScore != null) {
            return cachedScore;
        }
        
        double score = doGetPlayerPerformanceScore(player);
        
        // 缓存结果
        performanceScoreCache.put(player, score);
        
        return score;
    }
    
    private static double doGetPlayerPerformanceScore(Player player) {
        if (player != null && player.getPersistentData() != null) {
            if (player.getPersistentData().contains(PERFORMANCE_SCORE_KEY)) {
                double score = player.getPersistentData().getDouble(PERFORMANCE_SCORE_KEY);
                return score;
            }
        }
        
        // 检查服务器是否启用了强制使用真实性能的游戏规则
        if (player != null && player.level() != null && !player.level().isClientSide()) {
            // 在服务端检查游戏规则
            boolean forceUseTruePerformance = player.level().getGameRules().getBoolean(net.everla.everlaartifacts.game_rules.ForceUseTruePerformance.FORCE_USE_TRUE_PERFORMANCE);
            if (forceUseTruePerformance) {
                // 如果启用了强制使用真实性能，返回当前机器的真实性能评分
                double truePerformanceScore = net.everla.everlaartifacts.PerformanceMetrics.calculateTotalScore(
                    Runtime.getRuntime().availableProcessors(),
                    (int)(Runtime.getRuntime().maxMemory() / (1024 * 1024))
                );
                return truePerformanceScore;
            }
        }
        
        // 如果在服务端且玩家没有评分记录，则返回默认值
        if (!net.minecraftforge.fml.loading.FMLEnvironment.dist.isClient()) {
            return 50.0; // 服务端默认值
        }
        
        // 如果玩家没有评分记录，则返回客户端的性能评分
        if (net.minecraftforge.fml.loading.FMLEnvironment.dist.isClient()) {
            try {
                // 使用反射来安全地访问客户端类
                Class<?> minecraftClass = Class.forName("net.minecraft.client.Minecraft");
                Object mcInstance = minecraftClass.getMethod("getInstance").invoke(null);
                
                // 尝试通过字段获取玩家（Minecraft 1.20.1中通常是player字段）
                try {
                    Object mcPlayer = minecraftClass.getField("player").get(mcInstance);
                    if (mcPlayer != null) {
                        // 获取玩家UUID并比较
                        Object actualPlayerUUID = mcPlayer.getClass().getMethod("getUUID").invoke(mcPlayer);
                        if (actualPlayerUUID.equals(player.getUUID())) {
                            // 如果是当前客户端玩家，返回客户端的性能评分
                            double clientScore = net.everla.everlaartifacts.PerformanceMetrics.getClientPerformanceScore();
                            return clientScore;
                        }
                    }
                } catch (NoSuchFieldException e) {
                    // 如果字段不存在，尝试通过方法获取
                    try {
                        Object mcPlayer = minecraftClass.getMethod("player").invoke(mcInstance);
                        if (mcPlayer != null) {
                            // 获取玩家UUID并比较
                            Object actualPlayerUUID = mcPlayer.getClass().getMethod("getUUID").invoke(mcPlayer);
                            if (actualPlayerUUID.equals(player.getUUID())) {
                                // 如果是当前客户端玩家，返回客户端的性能评分
                                double clientScore = net.everla.everlaartifacts.PerformanceMetrics.getClientPerformanceScore();
                                return clientScore;
                            }
                        }
                    } catch (NoSuchMethodException ex) {
                        // 如果方法也不存在，跳过客户端检查，返回默认值
                    }
                }
            } catch (Exception e) {
                // 异常处理已被简化
            }
        }
        
        // 默认返回50.0（中性值）
        return 50.0;
    }
    
    /**
     * 清除玩家的性能评分缓存（例如当玩家离开服务器时）
     * 
     * @param player 玩家实体
     */
    public static void clearPlayerPerformanceScoreCache(Player player) {
        performanceScoreCache.remove(player);
    }
    
    /**
     * LivingDamageEvent处理器，用于调整攻击伤害
     * 
     * @param event 伤害事件
     */
    @SubscribeEvent
    public static void onLivingDamage(LivingDamageEvent event) {
        Entity sourceEntity = event.getSource().getEntity();
        
        // 检查攻击来源是否为玩家
        if (sourceEntity instanceof Player attacker) {
            // 检查玩家是否持有带有MoneyBurnersCreed或ScrapyardScrounger附魔的武器
            ItemStack weapon = attacker.getItemBySlot(EquipmentSlot.MAINHAND);
            
            if (weapon.isEmpty()) {
                weapon = attacker.getItemBySlot(EquipmentSlot.OFFHAND);
            }
            
            // 检查武器是否有MoneyBurnersCreed附魔
            int moneyBurnersCreedLevel = EnchantmentHelper.getItemEnchantmentLevel(
                EverlaartifactsModEnchantments.MONEY_BURNERS_CREED.get(), weapon);
                
            // 检查武器是否有ScrapyardScrounger附魔
            int scrapyardScroungerLevel = EnchantmentHelper.getItemEnchantmentLevel(
                EverlaartifactsModEnchantments.SCRAPYARD_SCROUNGER.get(), weapon);
                
            if (moneyBurnersCreedLevel > 0 || scrapyardScroungerLevel > 0) {
                // 伤害计算功能将使用玩家持久数据中的性能评分
                double performanceScore = getPlayerPerformanceScore(attacker);
                
                double damageMultiplier = 1.0;
                
                // 根据不同的附魔类型计算伤害增加量
                if (moneyBurnersCreedLevel > 0) {
                    // MoneyBurnersCreed: (评分-50)*2%
                    damageMultiplier = 1.0 + ((performanceScore - 50.0) * 0.02);
                } else if (scrapyardScroungerLevel > 0) {
                    // ScrapyardScrounger: (50-评分)*2%
                    damageMultiplier = 1.0 + ((50.0 - performanceScore) * 0.02);
                }
                
                // 应用伤害调整
                float originalDamage = event.getAmount();
                float newDamage = (float) (originalDamage * damageMultiplier);
                
                event.setAmount(newDamage);
            }
        }
    }
    
    /**
     * 为带有MoneyBurnersCreed或ScrapyardScrounger附魔的物品添加工具提示，显示当前伤害加成
     * 
     * @param event 物品工具提示事件
     */
    @SubscribeEvent
    public static void onItemTooltip(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();
        
        if (stack.isEmpty()) {
            return;
        }
        
        // 检查物品是否有MoneyBurnersCreed附魔
        int moneyBurnersCreedLevel = EnchantmentHelper.getItemEnchantmentLevel(
            EverlaartifactsModEnchantments.MONEY_BURNERS_CREED.get(), stack);
        
        // 检查物品是否有ScrapyardScrounger附魔
        int scrapyardScroungerLevel = EnchantmentHelper.getItemEnchantmentLevel(
            EverlaartifactsModEnchantments.SCRAPYARD_SCROUNGER.get(), stack);
        
        if (moneyBurnersCreedLevel > 0 || scrapyardScroungerLevel > 0) {
            // 物品提示将使用来自服务端的网络包
            double performanceScore = 50.0; // 默认值
            
            // 在客户端环境下，始终使用从服务端网络包接收的性能评分
            if (net.minecraftforge.fml.loading.FMLEnvironment.dist.isClient()) {
                performanceScore = net.everla.everlaartifacts.PerformanceMetrics.getClientPerformanceScore();
            }
            
            double damageBonusPercentage = 0.0;
            
            if (moneyBurnersCreedLevel > 0) {
                // MoneyBurnersCreed: (评分-50)*2%
                damageBonusPercentage = (performanceScore - 50.0) * 2.0;
            } else if (scrapyardScroungerLevel > 0) {
                // ScrapyardScrounger: (50-评分)*2%
                damageBonusPercentage = (50.0 - performanceScore) * 2.0;
            }
            
            // 添加伤害加成信息到工具提示
            String bonusText = String.format("%.2f", damageBonusPercentage);
            
            // 确定附魔名称
            String enchantmentName;
            if (moneyBurnersCreedLevel > 0) {
                enchantmentName = Component.translatable("enchantment.everlaartifacts.money_burners_creed").getString();
            } else {
                enchantmentName = Component.translatable("enchantment.everlaartifacts.scrapyard_scrounger").getString();
            }
            
            // 使用组件构建工具提示，确保占位符被正确替换
            event.getToolTip().add(Component.translatable("enchantment.everlaartifacts.displaytext.performance_based_damage_bonus", 
                enchantmentName,
                bonusText + "%")
                .withStyle(net.minecraft.ChatFormatting.GOLD));
        }
    }
}