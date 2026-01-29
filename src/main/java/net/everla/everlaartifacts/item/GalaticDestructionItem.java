
package net.everla.everlaartifacts.item;

import net.minecraftforge.registries.ForgeRegistries;

import net.minecraft.world.item.RecordItem;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.Item;
import net.minecraft.resources.ResourceLocation;

public class GalaticDestructionItem extends RecordItem {
	public GalaticDestructionItem() {
		super(5, () -> ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("everlaartifacts:galatic_destruction")), new Item.Properties().stacksTo(1).rarity(Rarity.RARE), 6000);
	}
}
