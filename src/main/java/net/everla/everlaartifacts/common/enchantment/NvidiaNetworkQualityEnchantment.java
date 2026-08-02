package net.everla.everlaartifacts.common.enchantment;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentCategory;

/**
 * N卡网速快 (Nvidia Network Quality) — weapon enchantment that increases
 * attack speed.
 * <p>
 * Formula: level 1 = +20%, each additional level = +5%, capped at +50%.
 * <p>
 * Max level: 5 (40% at level 5). The 50% cap is enforced by the handler.
 */
public class NvidiaNetworkQualityEnchantment extends Enchantment {

    public NvidiaNetworkQualityEnchantment() {
        super(Rarity.VERY_RARE, EnchantmentCategory.WEAPON,
                new EquipmentSlot[]{EquipmentSlot.MAINHAND});
    }

    @Override
    public int getMinCost(int level) {
        return 10 + level * 10;
    }

    @Override
    public int getMaxCost(int level) {
        return getMinCost(level) + 20;
    }

    @Override
    public int getMaxLevel() {
        return 5;
    }
}
