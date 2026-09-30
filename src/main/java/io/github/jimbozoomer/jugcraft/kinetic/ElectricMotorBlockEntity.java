package io.github.jimbozoomer.jugcraft.kinetic;

import io.github.jimbozoomer.jugcraft.energy.SimpleEnergyStorage;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/** Turns JE into KE (see {@link ElectricMotorBlock}). */
public class ElectricMotorBlockEntity extends BlockEntity {
	public static final long CAPACITY = 8_000;
	/** JE per tick it can take from cables. */
	public static final long INPUT = 256;
	/** KE per tick it can put out. */
	public static final long OUTPUT = 96;
	/** KE made per 100 JE. */
	public static final int EFFICIENCY_PERCENT = 75;

	final SimpleEnergyStorage energy = new SimpleEnergyStorage(CAPACITY, INPUT, 0, this::setChanged);

	public ElectricMotorBlockEntity(BlockPos pos, BlockState state) {
		super(JugcraftKinetics.ELECTRIC_MOTOR_ENTITY, pos, state);
	}

	public SimpleEnergyStorage energy() {
		return energy;
	}

	void serverTick(ServerLevel level, BlockPos pos, BlockState state) {
		long offer = Math.min(OUTPUT, energy.getAmount() * EFFICIENCY_PERCENT / 100);
		long taken = offer > 0 ? KineticNetworks.push(level, pos, state.getValue(ElectricMotorBlock.FACING), offer) : 0;
		if (taken > 0) {
			// JE used, rounded up so the motor never makes power out of rounding.
			long used = (taken * 100 + EFFICIENCY_PERCENT - 1) / EFFICIENCY_PERCENT;
			energy.setAmount(Math.max(0, energy.getAmount() - used));
		}
		boolean turning = taken > 0;
		if (state.getValue(ShaftBlock.TURNING) != turning) {
			level.setBlock(pos, state.setValue(ShaftBlock.TURNING, turning), Block.UPDATE_CLIENTS);
		}
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		energy.setAmount(input.getLong("energy").orElse(0L));
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		output.putLong("energy", energy.getAmount());
	}
}
