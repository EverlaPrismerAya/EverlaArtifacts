package net.everla.everlaartifacts.client.handlers.items.brackets_blade;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import net.everla.everlaartifacts.init.EverlaartifactsModItems;
import net.everla.everlaartifacts.server.network.DifficultySyncPacket;

@Mod.EventBusSubscriber(value = Dist.CLIENT, modid = "everlaartifacts")
public class BracketsBladeTooltipHandler {

    @SubscribeEvent
    public static void onItemTooltip(ItemTooltipEvent event) {
        ItemStack itemStack = event.getItemStack();
        
        // 检查是否是BracketsBlade
        if (itemStack.getItem() == EverlaartifactsModItems.BRACKETS_BLADE.get()) {
            String customName = itemStack.hasCustomHoverName() ? 
                itemStack.getHoverName().getString() : "";
            
            // 计算"「」"符号对的数量
            int bracketPairs = calculateBracketPairs(customName);
            
            // 如果物品没有自定义名称，则默认按8对括号处理
            if (!itemStack.hasCustomHoverName()) {
                bracketPairs = 8;  // 默认8对括号
            }
            
            // 检查是否为Extra难度（通过客户端同步包）
            boolean isExtraDifficulty = DifficultySyncPacket.isClientSpecialSeedWorld();
            
            String tooltipKey;
            Object[] tooltipArgs;
            ChatFormatting color;
            
            if (isExtraDifficulty) {
                // Extra难度下的特殊机制
                double damageBoost;
                if (bracketPairs <= 8) {
                    // 小于等于8对：每减少1对，伤害提升10点
                    damageBoost = (8 - bracketPairs) * 10.0;
                    tooltipKey = "item.brackets_blade.extra_damage_boost_positive";
                    color = ChatFormatting.GREEN;
                } else {
                    // 大于8对：每多1对，伤害降低1点
                    damageBoost = -(bracketPairs - 8) * 1.0;
                    tooltipKey = "item.brackets_blade.extra_damage_boost_negative";
                    color = ChatFormatting.RED;
                }
                // 8对时显示无加成
                if (bracketPairs == 8) {
                    tooltipKey = "item.brackets_blade.extra_damage_boost_neutral";
                    tooltipArgs = new Object[]{bracketPairs};
                    color = ChatFormatting.YELLOW;
                } else {
                    tooltipArgs = new Object[]{String.format("%.1f", Math.abs(damageBoost)), bracketPairs};
                }
            } else {
                // 非Extra难度下保持原有机制
                double damageBoost = bracketPairs * 0.5; // 每对括号增加0.5点伤害
                tooltipKey = "item.brackets_blade.damage_boost";
                tooltipArgs = new Object[]{String.format("%.1f", damageBoost), bracketPairs};
                color = ChatFormatting.BLUE;
            }
            
            event.getToolTip().add(Component.translatable(tooltipKey, tooltipArgs)
                .withStyle(color));
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
}