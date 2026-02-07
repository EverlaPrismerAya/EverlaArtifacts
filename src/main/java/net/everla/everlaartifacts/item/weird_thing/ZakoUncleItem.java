
package net.everla.everlaartifacts.item.weird_thing;

import net.minecraft.world.level.Level;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.entity.LivingEntity;

import net.everla.everlaartifacts.client.handlers.items.zako_uncle.ZakoUncleSoundHandler;

public class ZakoUncleItem extends Item {
	public ZakoUncleItem() {
		super(new Item.Properties().stacksTo(64).rarity(Rarity.COMMON).food((new FoodProperties.Builder()).nutrition(10).saturationMod(0.7f).meat().build()));
	}

	@Override
	public ItemStack finishUsingItem(ItemStack itemstack, Level world, LivingEntity entity) {
		ItemStack retval = super.finishUsingItem(itemstack, world, entity);
		// 只在客户端执行音效播放
		if (world.isClientSide()) {
			double x = entity.getX();
			double y = entity.getY();
			double z = entity.getZ();
			ZakoUncleSoundHandler.playZakoUncleSound(x, y, z);
		}
		return retval;
	}
}
