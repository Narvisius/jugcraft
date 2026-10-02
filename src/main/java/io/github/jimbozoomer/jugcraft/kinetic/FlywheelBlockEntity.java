package io.github.jimbozoomer.jugcraft.kinetic;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * Stores rotation (see {@link FlywheelBlock}): it takes KE in through any face but its front, keeps it in its spinning
 * wheel, and drives what its front faces with it, up to {@link #RATE} KE a tick each way. Bearing friction takes a
 * ten-thousandth of what it holds every tick, so a flywheel never makes power and runs down when left alone.
 */
public class FlywheelBlockEntity extends BlockEntity implements KineticConsumer {
	/** KE the wheel holds when spinning at full speed. */
	public static final long CAPACITY = 2_000_000;
	/** KE a tick it takes in, and the most it gives out. */
	public static final long RATE = 2_048;
	/** Friction: every tick the wheel loses this fraction (1 / FRICTION_DIVISOR) of what it holds, at least 1 KE. */
	public static final long FRICTION_DIVISOR = 10_000;

	private long stored;

	public FlywheelBlockEntity(BlockPos pos, BlockState state) {
		super(JugcraftKinetics.FLYWHEEL_ENTITY, pos, state);
	}

	public long stored() {
		return stored;
	}

	/** For game tests only. */
	public void setStored(long stored) {
		this.stored = Math.max(0, Math.min(CAPACITY, stored));
		setChanged();
	}

	@Override
	public long acceptKinetic(Direction side, long maxAmount) {
		if (side == getBlockState().getValue(FlywheelBlock.FACING)) {
			return 0;
		}
		long taken = Math.min(Math.min(maxAmount, RATE), CAPACITY - stored);
		if (taken > 0) {
			stored += taken;
			setChanged();
		}
		return Math.max(0, taken);
	}

	void serverTick(ServerLevel level, BlockPos pos, BlockState state) {
		if (stored > 0) {
			long taken = KineticNetworks.push(level, pos, state.getValue(FlywheelBlock.FACING), Math.min(stored, RATE));
			stored -= taken;
			stored = Math.max(0, stored - Math.max(1, stored / FRICTION_DIVISOR));
			setChanged();
		}
		boolean turning = stored > 0;
		if (state.getValue(ShaftBlock.TURNING) != turning) {
			level.setBlock(pos, state.setValue(ShaftBlock.TURNING, turning), Block.UPDATE_CLIENTS);
		}
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		stored = Math.max(0, Math.min(CAPACITY, input.getLong("stored").orElse(0L)));
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		output.putLong("stored", stored);
	}
}
