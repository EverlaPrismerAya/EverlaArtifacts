
package net.everla.everlaartifacts.item;

import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.Item;

public class PayToWinShard1Item extends Item {
	public PayToWinShard1Item() {
		super(new Item.Properties().stacksTo(64).fireResistant().rarity(Rarity.COMMON));
	}
}
