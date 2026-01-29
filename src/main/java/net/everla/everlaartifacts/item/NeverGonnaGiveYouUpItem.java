
package net.everla.everlaartifacts.item;

import net.minecraftforge.registries.ForgeRegistries;

import net.minecraft.world.item.RecordItem;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.Item;
import net.minecraft.resources.ResourceLocation;

public class NeverGonnaGiveYouUpItem extends RecordItem {
	public NeverGonnaGiveYouUpItem() {
		super(0, () -> ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("everlaartifacts:never_gonna_give_you_up")), new Item.Properties().stacksTo(1).rarity(Rarity.RARE), 4240);
	}
}
