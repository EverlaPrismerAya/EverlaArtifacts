
package net.everla.everlaartifacts.item;

import net.minecraftforge.registries.ForgeRegistries;

import net.minecraft.world.item.RecordItem;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.Item;
import net.minecraft.resources.ResourceLocation;

public class Hardest2BeItem extends RecordItem {
	public Hardest2BeItem() {
		super(7, () -> ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("everlaartifacts:hardest_2_be")), new Item.Properties().stacksTo(1).rarity(Rarity.RARE), 4060);
	}
}
