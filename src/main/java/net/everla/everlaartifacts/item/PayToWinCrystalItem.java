
package net.everla.everlaartifacts.item;

import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.Item;

public class PayToWinCrystalItem extends Item {
	public PayToWinCrystalItem() {
		super(new Item.Properties().stacksTo(64).fireResistant().rarity(Rarity.EPIC));
	}
}
