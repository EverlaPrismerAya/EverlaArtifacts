
package net.everla.everlaartifacts.item;

import net.minecraftforge.registries.ForgeRegistries;

import net.minecraft.world.item.RecordItem;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.Item;
import net.minecraft.resources.ResourceLocation;

public class GirlgotoloveItem extends RecordItem {
	public GirlgotoloveItem() {
		super(6, () -> ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("everlaartifacts:girl_go_to_love")), new Item.Properties().stacksTo(1).rarity(Rarity.RARE), 4880);
	}
}
