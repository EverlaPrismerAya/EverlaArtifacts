
package net.everla.everlaartifacts.item;

import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.Item;

public class BeijingTicketItem extends Item {
	public BeijingTicketItem() {
		super(new Item.Properties().stacksTo(64).rarity(Rarity.RARE));
	}
}
