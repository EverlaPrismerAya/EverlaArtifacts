
package net.everla.everlaartifacts.block;

import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.Level;
import net.minecraft.world.entity.Entity;
import net.minecraft.core.BlockPos;

import net.everla.everlaartifacts.procedures.NuclearWasteWaterRadiationProcedure;
import net.everla.everlaartifacts.init.EverlaartifactsModFluids;

public class NuclearWasteWaterBlock extends LiquidBlock {
	public NuclearWasteWaterBlock() {
		super(() -> EverlaartifactsModFluids.NUCLEAR_WASTE_WATER.get(),
				BlockBehaviour.Properties.of().mapColor(MapColor.WATER).strength(3600f).noCollission().noLootTable().liquid().pushReaction(PushReaction.DESTROY).sound(SoundType.EMPTY).replaceable());
	}

	@Override
	public void entityInside(BlockState blockstate, Level world, BlockPos pos, Entity entity) {
		super.entityInside(blockstate, world, pos, entity);
		NuclearWasteWaterRadiationProcedure.execute(entity);
	}
}
