
package net.everla.everlaartifacts.item;

import net.minecraftforge.registries.ForgeRegistries;

import net.minecraft.world.item.RecordItem;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.Item;
import net.minecraft.resources.ResourceLocation;

public class YamatoItem extends RecordItem {
	public YamatoItem() {
		super(15, () -> ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("everlaartifacts:yamato")), new Item.Properties().stacksTo(1).rarity(Rarity.RARE), 11580);
	}
}
