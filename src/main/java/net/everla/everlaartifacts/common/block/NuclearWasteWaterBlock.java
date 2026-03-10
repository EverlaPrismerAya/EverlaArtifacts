package net.everla.everlaartifacts.common.block;

import net.minecraft.commands.CommandSource;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.Level;
import net.minecraft.world.entity.Entity;
import net.minecraft.core.BlockPos;

import net.everla.everlaartifacts.init.EverlaartifactsModFluids;
import net.minecraftforge.fml.ModList;

public class NuclearWasteWaterBlock extends LiquidBlock {
	public NuclearWasteWaterBlock() {
		super(() -> EverlaartifactsModFluids.NUCLEAR_WASTE_WATER.get(),
				BlockBehaviour.Properties.of().mapColor(MapColor.WATER).strength(3600f).noCollission().noLootTable().liquid().pushReaction(PushReaction.DESTROY).sound(SoundType.EMPTY).replaceable());
	}

	@Override
	public void entityInside(BlockState blockstate, Level world, BlockPos pos, Entity entity) {
		super.entityInside(blockstate, world, pos, entity);
		radiateEntity(entity);
	}

	private static void radiateEntity(Entity entity) {
		if (entity == null)
			return;
		Entity _entity = entity;
		if (!_entity.level().isClientSide() && _entity.getServer() != null && _entity instanceof LivingEntity) {
			if (ModList.get().isLoaded("mekanism")) {
				_entity.getServer().getCommands().performPrefixedCommand(new CommandSourceStack(CommandSource.NULL, _entity.position(), _entity.getRotationVector(), _entity.level() instanceof ServerLevel ? (ServerLevel) _entity.level() : null, 4,
						_entity.getName().getString(), _entity.getDisplayName(), _entity.level().getServer(), _entity), "mek radiation addEntity @s 0.0000001");
			} else {
				// 检查冷却时间（每 2 秒执行一次）
				long currentTime = _entity.level().getGameTime();
				CompoundTag nbt = _entity.getPersistentData();
				String cooldownKey = "everlaartifacts:vanillaNuclearWaterCooldown";
				
				if (!nbt.contains(cooldownKey) || currentTime - nbt.getLong(cooldownKey) >= 40) { // 40 tick = 2 秒
					((LivingEntity) _entity).addEffect(new MobEffectInstance(MobEffects.WITHER, 100, 0));
					nbt.putLong(cooldownKey, currentTime);
				}
			}
		}
	}
}