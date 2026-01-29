
package net.everla.everlaartifacts.item;

import net.minecraftforge.registries.ForgeRegistries;

import net.minecraft.world.item.RecordItem;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.Item;
import net.minecraft.resources.ResourceLocation;

public class TheonlythingiknowforrealItem extends RecordItem {
	public TheonlythingiknowforrealItem() {
		super(9, () -> ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("everlaartifacts:the_only_thing_i_know_for_real")), new Item.Properties().stacksTo(1).rarity(Rarity.RARE), 2840);
	}
}
