package net.everla.everlaartifacts.common.enchantment;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentCategory;

/**
 * A卡音质高 (AMD Sound Quality) — weapon enchantment that applies debuffs
 * on hit and deals bonus damage to Wardens.
 * <p>
 * Effects on attack:
 * <ul>
 *   <li>Players: Slowness V, Weakness V, Mining Fatigue V (5 ticks)</li>
 *   <li>Mobs: GenshinStart effect (5 ticks)</li>
 *   <li>Wardens: additionally removes 30 HP via setHealth</li>
 * </ul>
 * <p>
 * Max level: 1 (the debuff strength does not scale with level).
 */
public class AmdSoundQualityEnchantment extends Enchantment {

    public AmdSoundQualityEnchantment() {
        super(Rarity.RARE, EnchantmentCategory.WEAPON,
                new EquipmentSlot[]{EquipmentSlot.MAINHAND});
    }

    @Override
    public int getMinCost(int level) {
        return 25;
    }

    @Override
    public int getMaxCost(int level) {
        return 50;
    }

    @Override
    public int getMaxLevel() {
        return 1;
    }
}
