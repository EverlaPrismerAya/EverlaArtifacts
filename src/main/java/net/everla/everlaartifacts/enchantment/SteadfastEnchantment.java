package net.everla.everlaartifacts.enchantment;

import net.minecraft.world.item.enchantment.EnchantmentCategory;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.EquipmentSlot;

public class SteadfastEnchantment extends Enchantment {
    private static final EnchantmentCategory ENCHANTMENT_CATEGORY = 
        EnchantmentCategory.create("everlaartifacts_steadfast", 
            item -> Ingredient.of(new ItemStack(Items.SHIELD)).test(new ItemStack(item)));

    public SteadfastEnchantment() {
        super(Enchantment.Rarity.COMMON, ENCHANTMENT_CATEGORY, EquipmentSlot.values());
    }

    @Override
    public int getMinCost(int level) {
        return 1 + level * 10;
    }

    @Override
    public int getMaxCost(int level) {
        return 6 + level * 10;
    }

    @Override
    public int getMaxLevel() {
        return 3;
    }
}