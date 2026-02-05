package net.everla.everlaartifacts.client.handlers.items.brackets_blade;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import net.everla.everlaartifacts.init.EverlaartifactsModItems;

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
            
            // 显示当前括号数量带来的伤害加成
            double damageBoost = bracketPairs * 0.5; // 每对括号增加0.5点伤害
            
            event.getToolTip().add(Component.translatable("item.brackets_blade.damage_boost", 
                String.format("%.1f", damageBoost), bracketPairs)
                .withStyle(ChatFormatting.BLUE));
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