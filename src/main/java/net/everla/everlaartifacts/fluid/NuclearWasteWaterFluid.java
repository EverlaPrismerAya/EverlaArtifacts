
package net.everla.everlaartifacts.fluid;

import net.minecraftforge.fluids.ForgeFlowingFluid;

import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.ParticleOptions;

import net.everla.everlaartifacts.init.EverlaartifactsModItems;
import net.everla.everlaartifacts.init.EverlaartifactsModFluids;
import net.everla.everlaartifacts.init.EverlaartifactsModFluidTypes;
import net.everla.everlaartifacts.init.EverlaartifactsModBlocks;

public abstract class NuclearWasteWaterFluid extends ForgeFlowingFluid {
	public static final ForgeFlowingFluid.Properties PROPERTIES = new ForgeFlowingFluid.Properties(() -> EverlaartifactsModFluidTypes.NUCLEAR_WASTE_WATER_TYPE.get(), () -> EverlaartifactsModFluids.NUCLEAR_WASTE_WATER.get(),
			() -> EverlaartifactsModFluids.FLOWING_NUCLEAR_WASTE_WATER.get()).explosionResistance(3600f).bucket(() -> EverlaartifactsModItems.NUCLEAR_WASTE_WATER_BUCKET.get())
			.block(() -> (LiquidBlock) EverlaartifactsModBlocks.NUCLEAR_WASTE_WATER.get());

	private NuclearWasteWaterFluid() {
		super(PROPERTIES);
	}

	@Override
	public ParticleOptions getDripParticle() {
		return ParticleTypes.BUBBLE_COLUMN_UP;
	}

	public static class Source extends NuclearWasteWaterFluid {
		public int getAmount(FluidState state) {
			return 8;
		}

		public boolean isSource(FluidState state) {
			return true;
		}
	}

	public static class Flowing extends NuclearWasteWaterFluid {
		protected void createFluidStateDefinition(StateDefinition.Builder<Fluid, FluidState> builder) {
			super.createFluidStateDefinition(builder);
			builder.add(LEVEL);
		}

		public int getAmount(FluidState state) {
			return state.getValue(LEVEL);
		}

		public boolean isSource(FluidState state) {
			return false;
		}
	}
}
