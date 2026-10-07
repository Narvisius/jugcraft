package io.github.jimbozoomer.jugcraft.agriculture;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * The Gargoyle Rainspout's clock: once every {@value GargoyleRainspoutBlock#CHECK_TICKS} ticks it looks at the weather
 * and starts or stops pouring; while it pours, every {@value GargoyleRainspoutBlock#FILL_TICKS} ticks it fills the
 * cauldron under its mouth by a level. How long it has poured is saved.
 */
public class GargoyleRainspoutBlockEntity extends BlockEntity {
	private int poured;

	public GargoyleRainspoutBlockEntity(BlockPos pos, BlockState state) {
		super(JugcraftAgriculture.GARGOYLE_RAINSPOUT_ENTITY, pos, state);
	}

	public void serverTick(ServerLevel level, BlockPos pos, BlockState state) {
		boolean pouring = state.getValue(GargoyleRainspoutBlock.POURING);
		if (pouring && ++poured >= GargoyleRainspoutBlock.FILL_TICKS) {
			poured = 0;
			GargoyleRainspoutBlock.pour(level, pos, state.getValue(GargoyleRainspoutBlock.FACING));
			setChanged();
		}
		if (Math.floorMod(level.getGameTime() + pos.asLong(), GargoyleRainspoutBlock.CHECK_TICKS) != 0) {
			return;
		}
		boolean raining = GargoyleRainspoutBlock.rainingOver(level, pos);
		if (raining != pouring) {
			level.setBlock(pos, state.setValue(GargoyleRainspoutBlock.POURING, raining), Block.UPDATE_ALL);
			if (!raining) {
				poured = 0;
				setChanged();
			}
		}
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		poured = input.getIntOr("poured", 0);
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		output.putInt("poured", poured);
	}
}
