
package net.everla.everlaartifacts.common.item.weird_thing;

import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.Item;

public class TokyoTicketItem extends Item {
	public TokyoTicketItem() {
		super(new Item.Properties().stacksTo(64).rarity(Rarity.RARE));
	}
}
