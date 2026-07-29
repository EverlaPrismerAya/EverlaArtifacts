package net.everla.everlaartifacts.common.effects;

import net.minecraft.commands.CommandSource;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeMap;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.item.ItemStack;

import java.util.List;

public class GenshinStartMobEffect extends MobEffect {
	public GenshinStartMobEffect() {
		super(MobEffectCategory.HARMFUL, -1);
	}

	@Override
	public void addAttributeModifiers(LivingEntity entity, AttributeMap attributeMap, int amplifier) {
		super.addAttributeModifiers(entity, attributeMap, amplifier);
		handleGenshinStartSoundPlay(entity);
	}

	@Override
	public void applyEffectTick(LivingEntity entity, int amplifier) {
		handleGenshinStartMobEffect(entity);
	}

	@Override
	public boolean isDurationEffectTick(int duration, int amplifier) {
		return true;
	}

	@Override
	public List<ItemStack> getCurativeItems() {
		return List.of(); // 返回空列表，防止被牛奶等物品治愈
	}

	private static void handleGenshinStartMobEffect(Entity entity) {
		if (entity == null || !(entity instanceof Mob mob))
			return;
		mob.setTarget(null);
		mob.setLastHurtByMob(null);
		mob.setLastHurtByPlayer(null);
	}

	private static void handleGenshinStartSoundPlay(Entity entity) {
		if (entity == null)
			return;
		{
			Entity _ent = entity;
			if (!_ent.level().isClientSide() && _ent.getServer() != null) {
				_ent.getServer().getCommands().performPrefixedCommand(new CommandSourceStack(CommandSource.NULL, _ent.position(), _ent.getRotationVector(), _ent.level() instanceof ServerLevel ? (ServerLevel) _ent.level() : null, 4,
						_ent.getName().getString(), _ent.getDisplayName(), _ent.level().getServer(), _ent), "playsound everlaartifacts:genshin_start_sound master @s");
			}
		}
	}
}