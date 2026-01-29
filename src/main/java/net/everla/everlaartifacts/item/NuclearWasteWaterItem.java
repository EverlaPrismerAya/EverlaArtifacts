
package net.everla.everlaartifacts.item;

import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.BucketItem;

import net.everla.everlaartifacts.init.EverlaartifactsModFluids;

public class NuclearWasteWaterItem extends BucketItem {
	public NuclearWasteWaterItem() {
		super(EverlaartifactsModFluids.NUCLEAR_WASTE_WATER, new Item.Properties().craftRemainder(Items.BUCKET).stacksTo(1).rarity(Rarity.COMMON));
	}
}
