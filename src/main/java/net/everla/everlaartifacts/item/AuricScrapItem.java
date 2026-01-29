
package net.everla.everlaartifacts.item;

import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.Item;

public class AuricScrapItem extends Item {
	public AuricScrapItem() {
		super(new Item.Properties().stacksTo(64).fireResistant().rarity(Rarity.RARE));
	}
}
