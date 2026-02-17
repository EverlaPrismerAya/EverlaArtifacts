package net.everla.everlaartifacts.generic.handlers.data_driven;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import net.minecraft.world.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

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
     * 监听事件给予真实的不可破坏
     */

    @SubscribeEvent
    public static void onLivingAttack(LivingAttackEvent event) {
        ItemStack stack = event.getEntity().getMainHandItem();
        if (EverlastingUsable(stack)) {
            addUnbreakableTag(stack);
        }
    }
    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event){
        for(ItemStack armor : event.getEntity().getArmorSlots()){
            if (EverlastingUsable(armor)){
                addUnbreakableTag(armor);
            } else return;
        }
    }

    @SubscribeEvent
    public static void ItemRightClick(PlayerInteractEvent.RightClickItem event){
        ItemStack stack = event.getItemStack();
        if (EverlastingUsable(stack)) {
            addUnbreakableTag(stack);
        }
    }
    /**
     * 监听物品提示事件，为符合条件的物品添加视觉上的不可破坏属性
     */
    @SubscribeEvent
    public static void onItemTooltip(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();
        if (EverlastingUsable(stack)) {
            // 添加不可破坏NBT标签
            addUnbreakableTag(stack);
        }
    }
    // 检测是否符合要求
    public static boolean EverlastingUsable(ItemStack stack){
        if (hasEverlastingTag(stack) && hasDurability(stack) && !hasUnbreakableTag(stack)) {
            return true;
        } return false;
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