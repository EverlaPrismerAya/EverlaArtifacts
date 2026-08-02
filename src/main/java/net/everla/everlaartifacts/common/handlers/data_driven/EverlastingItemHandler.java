package net.everla.everlaartifacts.common.handlers.data_driven;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 永恒物品处理器
 * <p>
 * 为具有 {@code everlaartifacts:everlasting} 标签的物品提供耐久保护。
 * 物品耐久消耗通过 {@link net.everla.everlaartifacts.mixin.ItemStackEverlastingMixin}
 * 在源头直接取消，不再需要添加 {@code Unbreakable:1b} NBT 标签。
 * <p>
 * 此类仅保留标签常量定义和工具提示显示。
 */
@Mod.EventBusSubscriber(modid = "everlaartifacts")
public class EverlastingItemHandler {

    /** 永恒标签 — 带有此标签的物品永远不会消耗耐久 */
    public static final TagKey<Item> EVERLASTING_TAG = TagKey.create(
            Registries.ITEM,
            new ResourceLocation("everlaartifacts", "everlasting")
    );

    /**
     * 检查物品是否具有永恒标签且有耐久度。
     * 供 mixin 和其他需要判断永恒的代码使用。
     */
    public static boolean isEverlasting(ItemStack stack) {
        if (stack.isEmpty()) return false;
        if (stack.getItem().getMaxDamage(stack) <= 0) return false;
        return stack.is(EVERLASTING_TAG);
    }

    /**
     * 在工具提示中显示永恒标记。
     * 不再修改物品 NBT — 耐久保护由 mixin 在源头处理。
     */
    @SubscribeEvent
    public static void onItemTooltip(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();
        if (isEverlasting(stack)) {
            event.getToolTip().add(Component.translatable("tooltip.everlaartifacts.everlasting")
                    .withStyle(ChatFormatting.LIGHT_PURPLE));
        }
    }
}
