
package net.everla.everlaartifacts.common.item.weird_thing;

import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.entity.LivingEntity;

import net.minecraftforge.registries.ForgeRegistries;

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
			playZakoUncleSound(x, y, z);
		}
		return retval;
	}

	private static void playZakoUncleSound(double x, double y, double z) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.level != null && mc.player != null) {
			// 在客户端播放音效
			mc.level.playLocalSound(
					x, y, z,
					ForgeRegistries.SOUND_EVENTS.getValue(ResourceLocation.fromNamespaceAndPath("everlaartifacts", "gfbhurt")),
					SoundSource.NEUTRAL,
					1.0F, 1.0F,
					false
			);
		}
	}
}
