package net.everla.everlaartifacts.server.handlers.data_driven.everlasting;

import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.common.util.*;

import net.minecraft.world.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

import net.everla.everlaartifacts.EverlaartifactsMod;

import java.util.List;

/**
 * 永恒物品处理器
 * 为具有 everlaartifacts:everlasting 标签的物品自动添加 Unbreakable:1b NBT 标签
 * 并在物品提示中显示相关信息
 */
@Mod.EventBusSubscriber(modid = "everlaartifacts")
public class EverlastingItemHandler {
    
    // 定义永恒标签
    public static final TagKey<Item> EVERLASTING_TAG = TagKey.create(
        Registries.ITEM, 
        new ResourceLocation("everlaartifacts", "everlasting")
    );
    
    /**
     * 监听物品提示事件，为符合条件的物品添加不可破坏属性
     */
    @SubscribeEvent
    public static void onItemTooltip(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();
        
        // 检查物品是否具有永恒标签、有耐久度且尚未添加不可破坏标签
        if (hasEverlastingTag(stack) && hasDurability(stack) && !hasUnbreakableTag(stack)) {
            // 添加不可破坏NBT标签
            addUnbreakableTag(stack);
        }
    }
    
    /**
     * 检查物品是否具有永恒标签
     */
    private static boolean hasEverlastingTag(ItemStack stack) {
        if (stack.isEmpty()) return false;
        
        // 检查物品是否在everlasting标签中
        return stack.is(EVERLASTING_TAG);
    }
    
    /**
     * 检查物品是否具有耐久度（可以损坏）
     */
    private static boolean hasDurability(ItemStack stack) {
        if (stack.isEmpty()) return false;
        
        // 检查物品的最大耐久度是否大于0
        return stack.getItem().getMaxDamage(stack) > 0;
    }
    
    /**
     * 检查物品是否已经具有不可破坏标签
     */
    private static boolean hasUnbreakableTag(ItemStack stack) {
        if (stack.isEmpty()) return false;
        
        CompoundTag tag = stack.getTag();
        return tag != null && tag.getBoolean("Unbreakable");
    }
    
    /**
     * 为物品添加不可破坏标签
     */
    private static void addUnbreakableTag(ItemStack stack) {
        if (!stack.hasTag()) {
            stack.setTag(new CompoundTag());
        }
        
        CompoundTag tag = stack.getTag();
        if (tag != null) {
            // 设置Unbreakable为true
            tag.putBoolean("Unbreakable", true);
        }
    }
}