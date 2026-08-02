package net.everla.everlaartifacts.common.enchantment;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentCategory;

/**
 * 中国人能飞 — treasure enchantment for chest armour.
 * <p>
 * Grants creative-style flight (at half horizontal speed) to players whose
 * client language is a zh variant (zh_cn, zh_tw, zh_hk, etc.).
 * <p>
 * Max level: 1. Treasure-only (not obtainable from enchanting table).
 */
public class ChineseCanFlyEnchantment extends Enchantment {

    public ChineseCanFlyEnchantment() {
        super(Rarity.RARE, EnchantmentCategory.ARMOR_CHEST,
                new EquipmentSlot[]{EquipmentSlot.CHEST});
    }

    @Override
    public int getMinCost(int level) {
        return 30;
    }

    @Override
    public int getMaxCost(int level) {
        return 60;
    }

    @Override
    public int getMaxLevel() {
        return 1;
    }

    @Override
    public boolean isTreasureOnly() {
        return true;
    }

    @Override
    public boolean isTradeable() {
        return true;
    }

    @Override
    public boolean isDiscoverable() {
        return true;
    }
}
