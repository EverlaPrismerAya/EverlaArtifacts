
package net.everla.everlaartifacts.item;

import net.minecraftforge.registries.ForgeRegistries;

import net.minecraft.world.item.RecordItem;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.Item;
import net.minecraft.resources.ResourceLocation;

public class ViscousDesperationItem extends RecordItem {
	public ViscousDesperationItem() {
		super(15, () -> ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("everlaartifacts:viscous_desperation")), new Item.Properties().stacksTo(1).rarity(Rarity.RARE), 2600);
	}
}
