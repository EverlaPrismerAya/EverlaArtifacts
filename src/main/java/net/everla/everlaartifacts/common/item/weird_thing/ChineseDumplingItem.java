package net.everla.everlaartifacts.common.item.weird_thing;

import net.minecraft.world.level.Level;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.entity.LivingEntity;

import net.everla.everlaartifacts.server.handlers.items.chinese_dumpling.ChineseDumplingAnnoyingSoundHandler;

public class ChineseDumplingItem extends Item {
	public ChineseDumplingItem() {
		super(new Item.Properties().stacksTo(64).rarity(Rarity.UNCOMMON).food((new FoodProperties.Builder()).nutrition(4).saturationMod(0.5f).build()));
	}

	@Override
	public ItemStack finishUsingItem(ItemStack itemstack, Level world, LivingEntity entity) {
		ItemStack retval = super.finishUsingItem(itemstack, world, entity);
		double x = entity.getX();
		double y = entity.getY();
		double z = entity.getZ();
		ChineseDumplingAnnoyingSoundHandler.handleChineseDumplingAnnoyingSound(world, x, y, z);
		return retval;
	}
}