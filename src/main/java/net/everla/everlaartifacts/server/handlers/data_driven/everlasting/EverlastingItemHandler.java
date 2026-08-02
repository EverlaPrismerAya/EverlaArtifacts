package net.everla.everlaartifacts.server.handlers.data_driven.everlasting;

import net.minecraft.world.item.ItemStack;

/**
 * 永恒物品服务端功能委托。
 * <p>
 * 耐久消耗保护已迁移至 {@link net.everla.everlaartifacts.mixin.ItemStackEverlastingMixin}，
 * 该 mixin 直接在 {@code ItemStack.hurtAndBreak} 中取消带有
 * {@code everlaartifacts:everlasting} 标签物品的耐久损失。
 * <p>
 * 标签定义和工具提示由
 * {@link net.everla.everlaartifacts.common.handlers.data_driven.EverlastingItemHandler} 统一管理。
 * <p>
 * 此类保留供未来服务端扩展使用，目前仅转发 {@code isEverlasting} 检查。
 *
 * @see net.everla.everlaartifacts.common.handlers.data_driven.EverlastingItemHandler
 * @see net.everla.everlaartifacts.mixin.ItemStackEverlastingMixin
 */
public class EverlastingItemHandler {

    /**
     * 委托到通用处理器检查物品是否永恒。
     */
    public static boolean isEverlasting(ItemStack stack) {
        return net.everla.everlaartifacts.common.handlers.data_driven.EverlastingItemHandler.isEverlasting(stack);
    }

    /**
     * 清理已处理物品记录（已废弃 — mixin 实现不需要此方法）。
     * @deprecated mixin 实现不再需要 UUID 去重
     */
    @Deprecated
    public static void cleanupProcessedItems() {
        // No-op — mixin implementation doesn't need UUID deduplication
    }
}
