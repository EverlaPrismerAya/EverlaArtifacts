package net.everla.everlaartifacts.server.handlers.items.brackets_blade;

import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import net.everla.everlaartifacts.init.EverlaartifactsModItems;
import net.everla.everlaartifacts.common.difficulty.DifficultyLevel;
import net.everla.everlaartifacts.server.handlers.difficulty.WorldSeedChecker;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Mod.EventBusSubscriber(modid = "everlaartifacts")
public class BracketsBladeAttributeHandler {
    // 属性缓存（避免频繁更新）
    private static final Map<UUID, Double> BRACKET_BONUS_CACHE = new ConcurrentHashMap<>();
    
    // 用于控制属性更新频率的计数器
    private static final Map<UUID, Integer> playerUpdateCounter = new ConcurrentHashMap<>();

    // 修饰符 UUID 生成器
    private static UUID getBracketBonusUUID(UUID playerUUID) {
        return UUID.nameUUIDFromBytes(("brackets_blade_bonus_" + playerUUID).getBytes());
    }

    /**
     * 每 tick 检查玩家状态并更新属性
     * 仅在服务端运行（客户端自动同步）
     * 优化：每5个tick更新一次，减少性能开销
     */
    @SubscribeEvent
    public static void onLivingTick(LivingEvent.LivingTickEvent event) {
        if (event.getEntity().level().isClientSide()) return;
        if (!(event.getEntity() instanceof Player player)) return;

        // 每5个tick更新一次，减少性能开销
        UUID playerUUID = player.getUUID();
        int currentTick = playerUpdateCounter.getOrDefault(playerUUID, 0) + 1;
        playerUpdateCounter.put(playerUUID, currentTick);
        
        if (currentTick % 5 != 0) {
            return;
        }

        updateBracketsBladeBonus(player);
    }

    /**
     * 更新BracketsBlade伤害加成：基于自定义名称中"「」"符号对的数量
     * 在Extra难度下实现特殊机制：
     * - 小于等于8对：每减少1对，伤害提升10点
     * - 等于8对（默认）：无加成
     * - 大于8对：每多1对，伤害降低1点
     */
    private static void updateBracketsBladeBonus(Player player) {
        UUID uuid = player.getUUID();
        AttributeInstance attackDamage = player.getAttribute(Attributes.ATTACK_DAMAGE);
        if (attackDamage == null) return;

        UUID modifierUUID = getBracketBonusUUID(uuid);
        
        // 检查主手是否持有BracketsBlade
        ItemStack mainHand = player.getMainHandItem();
        boolean isHoldingBracketsBlade = mainHand.getItem() == EverlaartifactsModItems.BRACKETS_BLADE.get();

        if (isHoldingBracketsBlade) {
            // 获取物品的自定义名称
            String customName = mainHand.hasCustomHoverName() ? 
                mainHand.getHoverName().getString() : "";
            
            // 计算"「」"符号对的数量（默认8对，如果未重命名则使用默认值）
            int bracketPairs = calculateBracketPairs(customName);
            
            // 如果物品没有自定义名称，则默认按8对括号处理
            if (!mainHand.hasCustomHoverName()) {
                bracketPairs = 8;  // 默认8对括号
            }
            
            // 检查是否为Extra难度
            boolean isExtraDifficulty = WorldSeedChecker.isSpecialSeedWorld() || 
                (player.level().getServer() != null && 
                 WorldSeedChecker.getCurrentWorldDifficulty(player.level().getServer()) == DifficultyLevel.EXTRA);
            
            double damageBonus = 0.0;
            
            if (isExtraDifficulty) {
                // Extra难度下的特殊机制
                if (bracketPairs <= 8) {
                    // 小于等于8对：每减少1对，伤害提升10点
                    damageBonus = (8 - bracketPairs) * 10.0;
                } else {
                    // 大于8对：每多1对，伤害降低1点
                    damageBonus = -(bracketPairs - 8) * 1.0;
                }
                // 8对时无加成（damageBonus = 0）
            } else {
                // 非Extra难度下保持原有机制：每对0.5点伤害
                damageBonus = bracketPairs * 0.5;
            }
            
            // 只有当值变化时才更新属性（避免不必要的计算）
            if (!BRACKET_BONUS_CACHE.containsKey(uuid) || 
                Math.abs(BRACKET_BONUS_CACHE.get(uuid) - damageBonus) > 0.01) {
                
                attackDamage.removeModifier(modifierUUID);
                if (Math.abs(damageBonus) > 0.01) { // 只有当伤害加成不为0时才添加修饰符
                    AttributeModifier modifier = new AttributeModifier(
                        modifierUUID, 
                        "Brackets Blade Bonus", 
                        damageBonus, 
                        AttributeModifier.Operation.ADDITION
                    );
                    attackDamage.addTransientModifier(modifier);
                }
                BRACKET_BONUS_CACHE.put(uuid, damageBonus);
            }
        } else {
            // 不持有BracketsBlade时移除修饰符
            attackDamage.removeModifier(modifierUUID);
            BRACKET_BONUS_CACHE.remove(uuid);
        }
    }

    /**
     * 计算自定义名称中"「」"符号对的数量
     * 
     * @param customName 自定义名称
     * @return 符号对的数量
     */
    private static int calculateBracketPairs(String customName) {
        if (customName == null || customName.isEmpty()) {
            return 0;
        }
        
        int leftCount = 0;   // 「 的数量
        int rightCount = 0;  // 」 的数量
        
        // 统计左右括号的数量
        for (char c : customName.toCharArray()) {
            if (c == '「') {
                leftCount++;
            } else if (c == '」') {
                rightCount++;
            }
        }
        
        // 取较小值作为配对数量
        return Math.min(leftCount, rightCount);
    }

    /**
     * 玩家死亡时清理缓存（防止内存泄漏）
     */
    @SubscribeEvent
    public static void onPlayerClone(net.minecraftforge.event.entity.player.PlayerEvent.Clone event) {
        if (event.isWasDeath()) {
            UUID oldUUID = event.getOriginal().getUUID();
            BRACKET_BONUS_CACHE.remove(oldUUID);
        }
    }
}