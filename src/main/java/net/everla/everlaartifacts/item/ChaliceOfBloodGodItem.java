
package net.everla.everlaartifacts.item;

import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.api.distmarker.Dist;

import net.minecraft.world.level.Level;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.network.chat.Component;

import net.everla.everlaartifacts.procedures.ChaliceOfBloodGodRegenerateProcedure;
import net.everla.everlaartifacts.init.EverlaartifactsModItems;

import java.util.List;

public class ChaliceOfBloodGodItem extends Item {
	public ChaliceOfBloodGodItem() {
		super(new Item.Properties().stacksTo(1).fireResistant().rarity(Rarity.EPIC).food((new FoodProperties.Builder()).nutrition(20).saturationMod(1.6f).build()));
	}

	@Override
	public UseAnim getUseAnimation(ItemStack itemstack) {
		return UseAnim.DRINK;
	}

	@Override
	public boolean hasCraftingRemainingItem() {
		return true;
	}

	@Override
	public ItemStack getCraftingRemainingItem(ItemStack itemstack) {
		return new ItemStack(this);
	}

	@Override
	@OnlyIn(Dist.CLIENT)
	public boolean isFoil(ItemStack itemstack) {
		return true;
	}

	@Override
	public void appendHoverText(ItemStack itemstack, Level level, List<Component> list, TooltipFlag flag) {
		super.appendHoverText(itemstack, level, list, flag);
		list.add(Component.translatable("item.everlaartifacts.chalice_of_blood_god.description_0"));
		list.add(Component.translatable("item.everlaartifacts.chalice_of_blood_god.description_1"));
		list.add(Component.translatable("item.everlaartifacts.chalice_of_blood_god.description_2"));
		list.add(Component.translatable("item.everlaartifacts.chalice_of_blood_god.description_3"));
		list.add(Component.translatable("item.everlaartifacts.chalice_of_blood_god.description_4"));
		list.add(Component.translatable("item.everlaartifacts.chalice_of_blood_god.description_5"));
		list.add(Component.translatable("item.everlaartifacts.chalice_of_blood_god.description_6"));
		list.add(Component.translatable("item.everlaartifacts.chalice_of_blood_god.description_7"));
		list.add(Component.translatable("item.everlaartifacts.chalice_of_blood_god.description_8"));
		list.add(Component.translatable("item.everlaartifacts.chalice_of_blood_god.description_9"));
		list.add(Component.translatable("item.everlaartifacts.chalice_of_blood_god.description_10"));
		list.add(Component.translatable("item.everlaartifacts.chalice_of_blood_god.description_11"));
		list.add(Component.translatable("item.everlaartifacts.chalice_of_blood_god.description_12"));
		list.add(Component.translatable("item.everlaartifacts.chalice_of_blood_god.description_13"));
		list.add(Component.translatable("item.everlaartifacts.chalice_of_blood_god.description_14"));
		list.add(Component.translatable("item.everlaartifacts.chalice_of_blood_god.description_15"));
	}

	@Override
	public ItemStack finishUsingItem(ItemStack itemstack, Level world, LivingEntity entity) {
		ItemStack retval = new ItemStack(EverlaartifactsModItems.CHALICE_OF_BLOOD_GOD.get());
		super.finishUsingItem(itemstack, world, entity);
		double x = entity.getX();
		double y = entity.getY();
		double z = entity.getZ();
		ChaliceOfBloodGodRegenerateProcedure.execute(entity);
		if (itemstack.isEmpty()) {
			return retval;
		} else {
			if (entity instanceof Player player && !player.getAbilities().instabuild) {
				if (!player.getInventory().add(retval))
					player.drop(retval, false);
			}
			return itemstack;
		}
	}
}
