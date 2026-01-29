
package net.everla.everlaartifacts.item;

import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.Item;

public class NanjingTicketItem extends Item {
	public NanjingTicketItem() {
		super(new Item.Properties().stacksTo(64).rarity(Rarity.RARE));
	}
}
