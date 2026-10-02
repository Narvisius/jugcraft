package io.github.jimbozoomer.jugcraft.solar;

import io.github.jimbozoomer.jugcraft.energy.EnergyNetworks;
import io.github.jimbozoomer.jugcraft.energy.SimpleEnergyStorage;
import java.util.EnumSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/** Makes power while the sun is up and the sky above it is open; half in rain, none at night. */
public class SolarTrackerBlockEntity extends BlockEntity {
	/** JE per tick in full sun. */
	public static final long OUTPUT = 20;
	public static final long CAPACITY = 40_000;
	/** JE per tick it can push into cables. */
	public static final long MAX_OUTPUT = 256;

	final SimpleEnergyStorage energy = new SimpleEnergyStorage(CAPACITY, 0, MAX_OUTPUT, this::setChanged);

	public SolarTrackerBlockEntity(BlockPos pos, BlockState state) {
		super(JugcraftSolar.SOLAR_TRACKER_ENTITY, pos, state);
	}

	public SimpleEnergyStorage energy() {
		return energy;
	}

	void serverTick(ServerLevel level, BlockPos pos) {
		if (level.isBrightOutside() && level.canSeeSky(pos.above()) && energy.getAmount() < energy.getCapacity()) {
			energy.setAmount(energy.getAmount() + (level.isRaining() ? OUTPUT / 2 : OUTPUT));
		}
		EnergyNetworks.pushToNeighbors(level, pos, energy, MAX_OUTPUT, EnumSet.allOf(Direction.class));
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
