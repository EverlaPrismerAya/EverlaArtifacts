
package net.everla.everlaartifacts.item;

import net.minecraftforge.registries.ForgeRegistries;

import net.minecraft.world.item.RecordItem;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.Item;
import net.minecraft.resources.ResourceLocation;

public class LonelyManBrokenheartedSongItem extends RecordItem {
	public LonelyManBrokenheartedSongItem() {
		super(0, () -> ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("everlaartifacts:lonely_man_brokenhearted_song")), new Item.Properties().stacksTo(1).rarity(Rarity.RARE), 1900);
	}
}
