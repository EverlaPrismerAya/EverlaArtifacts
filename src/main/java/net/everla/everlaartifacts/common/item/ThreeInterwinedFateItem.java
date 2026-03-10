
package net.everla.everlaartifacts.common.item;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.entity.LivingEntity;

import net.minecraft.world.level.LevelAccessor;
import net.minecraftforge.registries.ForgeRegistries;

public class ThreeInterwinedFateItem extends Item {
	public ThreeInterwinedFateItem() {
		super(new Item.Properties().stacksTo(16).fireResistant().rarity(Rarity.UNCOMMON).food((new FoodProperties.Builder()).nutrition(3).saturationMod(0.3f).alwaysEat().build()));
	}

	@Override
	public UseAnim getUseAnimation(ItemStack itemstack) {
		return UseAnim.SPEAR;
	}

	@Override
	public ItemStack finishUsingItem(ItemStack itemstack, Level world, LivingEntity entity) {
		ItemStack retval = super.finishUsingItem(itemstack, world, entity);
		double x = entity.getX();
		double y = entity.getY();
		double z = entity.getZ();
		handleThreeInterwinedFateEmotionalDamage(world, x, y, z, entity);
		return retval;
	}

	private static void handleThreeInterwinedFateEmotionalDamage(LevelAccessor world, double x, double y, double z, Entity entity) {
		if (entity == null)
			return;
		
		// 施加伤害
		entity.hurt(new DamageSource(world.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(DamageTypes.GENERIC)), 20);
		
		// 播放音效
		if (world instanceof Level _level) {
			if (!_level.isClientSide()) {
				_level.playSound(null, BlockPos.containing(x, y, z), ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("everlaartifacts:emotional_damage")), SoundSource.NEUTRAL, 1, 1);
			} else {
				_level.playLocalSound(x, y, z, ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("everlaartifacts:emotional_damage")), SoundSource.NEUTRAL, 1, 1, false);
			}
		}
		
		// 施加状态效果（仅服务端）
		if (entity instanceof LivingEntity livingEntity && !livingEntity.level().isClientSide()) {
			livingEntity.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 4800, 3, false, true));
			livingEntity.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 2400, 1, false, true));
			livingEntity.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 7200, 0, false, true));
			livingEntity.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 7200, 0, false, true));
		}
	}
}
