
package net.everla.everlaartifacts.item;

import net.minecraftforge.registries.ForgeRegistries;

import net.minecraft.world.item.RecordItem;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.Item;
import net.minecraft.resources.ResourceLocation;

public class ElectricalStickBoneItem extends RecordItem {
	public ElectricalStickBoneItem() {
		super(2, () -> ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("everlaartifacts:electrical_stick_bone")), new Item.Properties().stacksTo(1).rarity(Rarity.RARE), 2140);
	}
}
