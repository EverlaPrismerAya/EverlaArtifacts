
package net.everla.everlaartifacts.item;

import net.minecraftforge.registries.ForgeRegistries;

import net.minecraft.world.item.RecordItem;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.Item;
import net.minecraft.resources.ResourceLocation;

public class KillTheMessengerItem extends RecordItem {
	public KillTheMessengerItem() {
		super(12, () -> ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("everlaartifacts:killthemessenger")), new Item.Properties().stacksTo(1).rarity(Rarity.RARE), 4180);
	}
}
