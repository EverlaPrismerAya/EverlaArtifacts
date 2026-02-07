
package net.everla.everlaartifacts.item.weird_thing.p2wshard;

import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.Item;

public class PayToWinShard2Item extends Item {
	public PayToWinShard2Item() {
		super(new Item.Properties().stacksTo(64).fireResistant().rarity(Rarity.UNCOMMON));
	}
}
