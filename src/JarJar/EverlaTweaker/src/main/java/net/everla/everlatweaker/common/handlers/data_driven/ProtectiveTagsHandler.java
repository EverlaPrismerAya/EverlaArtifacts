package net.everla.everlatweaker.common.handlers.data_driven;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * 数据驱动防护标签处理器
 * <p>
 * 提供物品掉落物形态下的防火/防爆标签判定，供
 * {@link net.everla.everlatweaker.mixin.ItemEntityEverlastingMixin} 使用。
 * <ul>
 *   <li>{@code everlatweaker:explosion_resistant} — 免疫爆炸伤害</li>
 *   <li>{@code everlatweaker:fire_resistant} — 免疫火焰与岩浆</li>
 * </ul>
 */
public final class ProtectiveTagsHandler {

    /** 爆炸抗性标签 — 带有此标签的物品实体不会被爆炸摧毁 */
    public static final TagKey<Item> EXPLOSION_RESISTANT_TAG = TagKey.create(
            Registries.ITEM,
            new ResourceLocation("everlatweaker", "explosion_resistant")
    );

    /** 火焰抗性标签 — 带有此标签的物品实体不会在火焰/岩浆中燃烧 */
    public static final TagKey<Item> FIRE_RESISTANT_TAG = TagKey.create(
            Registries.ITEM,
            new ResourceLocation("everlatweaker", "fire_resistant")
    );

    private ProtectiveTagsHandler() {
        // 工具类禁止实例化
    }

    /**
     * 检查物品栈是否带有爆炸抗性标签。
     * 供 mixin 判断掉落物是否免疫爆炸伤害使用。
     */
    public static boolean isExplosionResistant(ItemStack stack) {
        if (stack.isEmpty()) return false;
        return stack.is(EXPLOSION_RESISTANT_TAG);
    }

    /**
     * 检查物品栈是否带有火焰抗性标签。
     * 供 mixin 判断掉落物是否免疫火焰/岩浆伤害使用。
     */
    public static boolean isFireResistant(ItemStack stack) {
        if (stack.isEmpty()) return false;
        return stack.is(FIRE_RESISTANT_TAG);
    }
}
