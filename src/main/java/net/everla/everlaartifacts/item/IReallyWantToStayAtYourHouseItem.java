
package net.everla.everlaartifacts.item;

import net.minecraftforge.registries.ForgeRegistries;

import net.minecraft.world.item.RecordItem;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.Item;
import net.minecraft.resources.ResourceLocation;

public class IReallyWantToStayAtYourHouseItem extends RecordItem {
	public IReallyWantToStayAtYourHouseItem() {
		super(2, () -> ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("everlaartifacts:ireallywant2stayaturhouse")), new Item.Properties().stacksTo(1).rarity(Rarity.RARE), 4920);
	}
}
