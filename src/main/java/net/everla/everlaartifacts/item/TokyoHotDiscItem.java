
package net.everla.everlaartifacts.item;

import net.minecraftforge.registries.ForgeRegistries;

import net.minecraft.world.item.RecordItem;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.Item;
import net.minecraft.resources.ResourceLocation;

public class TokyoHotDiscItem extends RecordItem {
	public TokyoHotDiscItem() {
		super(0, () -> ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("everlaartifacts:tokyo_hot")), new Item.Properties().stacksTo(1).rarity(Rarity.RARE), 3140);
	}
}
