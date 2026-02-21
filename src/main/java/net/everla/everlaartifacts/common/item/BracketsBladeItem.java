
package net.everla.everlaartifacts.common.item;

import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.Ingredient;

import net.everla.everlaartifacts.init.EverlaartifactsModItems;

public class BracketsBladeItem extends SwordItem {
	public BracketsBladeItem() {
		super(new Tier() {
			public int getUses() {
				return 3389;
			}

			public float getSpeed() {
				return 4f;
			}

			public float getAttackDamageBonus() {
				return 5f;
			}

			public int getLevel() {
				return 0;
			}

			public int getEnchantmentValue() {
				return 15;
			}

			public Ingredient getRepairIngredient() {
				return Ingredient.of(new ItemStack(EverlaartifactsModItems.AURIC_INGOT.get()));
			}
		}, 3, -2.4f, new Item.Properties().fireResistant().rarity(Rarity.EPIC));
	}
}
