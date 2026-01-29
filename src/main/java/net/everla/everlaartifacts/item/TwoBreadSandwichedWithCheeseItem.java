
package net.everla.everlaartifacts.item;

import net.minecraft.world.level.Level;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.network.chat.Component;

import net.everla.everlaartifacts.procedures.TwoBreadSandwichedWithCheeseSoundProcedure;

import java.util.List;

public class TwoBreadSandwichedWithCheeseItem extends Item {
	public TwoBreadSandwichedWithCheeseItem() {
		super(new Item.Properties().stacksTo(64).rarity(Rarity.RARE).food((new FoodProperties.Builder()).nutrition(8).saturationMod(0.9f).build()));
	}

	@Override
	public void appendHoverText(ItemStack itemstack, Level level, List<Component> list, TooltipFlag flag) {
		super.appendHoverText(itemstack, level, list, flag);
		list.add(Component.translatable("item.everlaartifacts.two_bread_sandwiched_with_cheese.description_0"));
	}

	@Override
	public ItemStack finishUsingItem(ItemStack itemstack, Level world, LivingEntity entity) {
		ItemStack retval = super.finishUsingItem(itemstack, world, entity);
		double x = entity.getX();
		double y = entity.getY();
		double z = entity.getZ();
		TwoBreadSandwichedWithCheeseSoundProcedure.execute(world, x, y, z);
		return retval;
	}
}
