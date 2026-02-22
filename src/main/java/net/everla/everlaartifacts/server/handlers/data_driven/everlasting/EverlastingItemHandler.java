package net.everla.everlaartifacts.server.handlers.data_driven.everlasting;

import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.EventPriority;
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
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.Level;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;

import net.everla.everlaartifacts.EverlaartifactsMod;

import java.util.List;
import java.util.Set;
import java.util.HashSet;
import java.util.UUID;

/**
 * 增强版永恒物品处理器
 * 为具有 everlaartifacts:everlasting 标签的物品自动添加 Unbreakable:1b NBT 标签
 * 并在物品因使用而损失耐久时立即添加不可破坏属性
 * 支持三种触发事件：
 * 1. 攻击实体时 (LivingAttackEvent/LivingHurtEvent)
 * 2. 破坏方块时 (BlockEvent.BreakEvent)
 * 3. 右击使用时 (PlayerInteractEvent.RightClickItem)
 */
@Mod.EventBusSubscriber(modid = "everlaartifacts")
public class EverlastingItemHandler {
    
    // 定义永恒标签
    public static final TagKey<Item> EVERLASTING_TAG = TagKey.create(
        Registries.ITEM, 
        ResourceLocation.fromNamespaceAndPath("everlaartifacts", "everlasting")
    );
    
    // 记录已处理过的物品UUID，防止重复处理
    private static final Set<UUID> PROCESSED_ITEMS = new HashSet<>();
    
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
     * 监听实体攻击事件 - 当玩家攻击实体时触发
     * 优先级设为 LOWEST，确保在其他处理逻辑之后执行
     */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onLivingAttack(LivingAttackEvent event) {
        // 只处理玩家发起的攻击
        if (!(event.getSource().getEntity() instanceof Player player)) {
            return;
        }
        
        // 检查玩家手中的物品
        ItemStack mainHand = player.getMainHandItem();
        ItemStack offHand = player.getOffhandItem();
        
        // 处理主手物品
        processItemForEverlasting(mainHand, player);
        
        // 处理副手物品（如果不同的话）
        if (!ItemStack.matches(mainHand, offHand)) {
            processItemForEverlasting(offHand, player);
        }
    }
    
    /**
     * 监听实体受伤事件 - 当玩家造成伤害时触发
     * 作为 LivingAttackEvent 的补充，确保覆盖更多情况
     */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onLivingHurt(LivingHurtEvent event) {
        // 只处理玩家发起的伤害
        if (!(event.getSource().getEntity() instanceof Player player)) {
            return;
        }
        
        // 检查玩家手中的物品
        ItemStack mainHand = player.getMainHandItem();
        ItemStack offHand = player.getOffhandItem();
        
        // 处理主手物品
        processItemForEverlasting(mainHand, player);
        
        // 处理副手物品（如果不同的话）
        if (!ItemStack.matches(mainHand, offHand)) {
            processItemForEverlasting(offHand, player);
        }
    }
    
    /**
     * 监听方块破坏事件 - 当玩家破坏方块时触发
     */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        Player player = event.getPlayer();
        
        // 检查玩家手中的物品
        ItemStack mainHand = player.getMainHandItem();
        ItemStack offHand = player.getOffhandItem();
        
        // 处理主手物品
        processItemForEverlasting(mainHand, player);
        
        // 处理副手物品（如果不同的话）
        if (!ItemStack.matches(mainHand, offHand)) {
            processItemForEverlasting(offHand, player);
        }
    }
    
    /**
     * 监听右键点击物品事件 - 当玩家右键使用物品时触发
     */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        Player player = event.getEntity();
        ItemStack stack = event.getItemStack();
        
        // 处理右键使用的物品
        processItemForEverlasting(stack, player);
    }
    
    /**
     * 处理单个物品的永恒化逻辑
     */
    private static void processItemForEverlasting(ItemStack stack, Player player) {
        if (stack.isEmpty()) {
            return;
        }
        
        // 生成物品的唯一标识（基于物品类型和标签）
        UUID itemUUID = getItemUniqueIdentifier(stack);
        
        // 检查是否已经处理过这个物品
        if (PROCESSED_ITEMS.contains(itemUUID)) {
            return;
        }
        
        // 检查物品是否符合永恒化条件
        if (hasEverlastingTag(stack) && hasDurability(stack) && !hasUnbreakableTag(stack)) {
            // 添加不可破坏标签
            addUnbreakableTag(stack);
            
            // 记录已处理
            PROCESSED_ITEMS.add(itemUUID);
            
            // 可选：发送提示消息给玩家（仅服务端）
            if (player instanceof ServerPlayer serverPlayer) {
                // 这里可以添加提示消息逻辑
                // serverPlayer.sendSystemMessage(Component.literal("物品已获得永恒属性！"));
            }
        }
    }
    
    /**
     * 生成物品的唯一标识符
     * 结合物品类型、损害值和现有NBT来创建唯一ID
     */
    private static UUID getItemUniqueIdentifier(ItemStack stack) {
        StringBuilder identifier = new StringBuilder();
        identifier.append(ForgeRegistries.ITEMS.getKey(stack.getItem()).toString());
        identifier.append(":").append(stack.getDamageValue());
        
        // 如果有NBT标签，添加其哈希值
        if (stack.hasTag()) {
            identifier.append(":").append(stack.getTag().hashCode());
        }
        
        return UUID.nameUUIDFromBytes(identifier.toString().getBytes());
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
    
    /**
     * 清理已处理物品记录（可选：定期清理以防止内存泄漏）
     */
    public static void cleanupProcessedItems() {
        PROCESSED_ITEMS.clear();
    }
}