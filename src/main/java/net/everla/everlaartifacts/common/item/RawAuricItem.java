
package net.everla.everlaartifacts.common.item;

import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.Item;

public class RawAuricItem extends Item {
	public RawAuricItem() {
		super(new Item.Properties().stacksTo(64).fireResistant().rarity(Rarity.UNCOMMON));
	}
}
