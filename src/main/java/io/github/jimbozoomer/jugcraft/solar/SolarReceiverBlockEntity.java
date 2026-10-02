package io.github.jimbozoomer.jugcraft.solar;

import io.github.jimbozoomer.jugcraft.energy.EnergyNetworks;
import io.github.jimbozoomer.jugcraft.energy.SimpleEnergyStorage;
import io.github.jimbozoomer.jugcraft.fluid.FluidNetworks;
import java.util.EnumSet;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.fluid.base.SingleFluidStorage;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * The solar receiver: the boiler on top of a tower that a heliostat field aims at. While the sun is up it makes
 * {@link #JE_PER_HELIOSTAT} JE/t for each heliostat under open sky in its field (up to {@link #MAX_HELIOSTATS}),
 * boiling a millibucket of water for every {@link #JE_PER_MB} JE; half in rain, none at night or without water.
 * <p>
 * The field is the square {@link #FIELD_RADIUS} blocks round the receiver, from one to {@link #FIELD_DEPTH} blocks
 * below it, so the receiver must stand above its mirrors. It is counted every {@link #SCAN_INTERVAL} ticks.
 */
public class SolarReceiverBlockEntity extends BlockEntity {
	public static final int JE_PER_HELIOSTAT = 12;
	public static final int MAX_HELIOSTATS = 48;
	public static final int JE_PER_MB = 32;
	public static final int FIELD_RADIUS = 8;
	public static final int FIELD_DEPTH = 16;
	public static final int SCAN_INTERVAL = 100;
	public static final long CAPACITY = 100_000;
	public static final long MAX_OUTPUT = 1_024;
	/** Water tank (mB). */
	public static final int TANK = 8_000;

	final SimpleEnergyStorage energy = new SimpleEnergyStorage(CAPACITY, 0, MAX_OUTPUT, this::setChanged);
	final SingleFluidStorage water = new SingleFluidStorage() {
		@Override
		protected long getCapacity(FluidVariant variant) {
			return (long) TANK * FluidNetworks.DROPLETS_PER_MB;
		}

		@Override
		protected boolean canInsert(FluidVariant variant) {
			return variant.isOf(Fluids.WATER);
		}

		@Override
		protected void onFinalCommit() {
			setChanged();
		}
	};
	private int heliostats = -1;
	private int lastOutput;

	public SolarReceiverBlockEntity(BlockPos pos, BlockState state) {
		super(JugcraftSolar.SOLAR_RECEIVER_ENTITY, pos, state);
	}

	public SimpleEnergyStorage energy() {
		return energy;
	}

	public SingleFluidStorage water() {
		return water;
	}

	/** Heliostats counted at the last scan (-1 before the first). */
	public int heliostats() {
		return heliostats;
	}

	/** JE made last tick. */
	public int lastOutput() {
		return lastOutput;
	}

	/** Heliostats under open sky in the field below {@code pos}. */
	public static int countHeliostats(ServerLevel level, BlockPos pos) {
		int count = 0;
		BlockPos.MutableBlockPos at = new BlockPos.MutableBlockPos();
		for (int dy = 1; dy <= FIELD_DEPTH; dy++) {
			for (int dx = -FIELD_RADIUS; dx <= FIELD_RADIUS; dx++) {
				for (int dz = -FIELD_RADIUS; dz <= FIELD_RADIUS; dz++) {
					at.set(pos.getX() + dx, pos.getY() - dy, pos.getZ() + dz);
					if (level.getBlockState(at).is(JugcraftSolar.HELIOSTAT) && level.canSeeSky(at.above())) {
						count++;
					}
				}
			}
		}
		return count;
	}

	void serverTick(ServerLevel level, BlockPos pos) {
		if (heliostats < 0 || level.getGameTime() % SCAN_INTERVAL == 0) {
			heliostats = countHeliostats(level, pos);
		}
		int output = 0;
		if (level.isBrightOutside() && level.canSeeSky(pos.above())) {
			output = Math.min(heliostats, MAX_HELIOSTATS) * JE_PER_HELIOSTAT;
			if (level.isRaining()) {
				output /= 2;
			}
		}
		long boiled = (long) (output + JE_PER_MB - 1) / JE_PER_MB * FluidNetworks.DROPLETS_PER_MB;
		if (output > 0 && water.amount >= boiled && energy.getAmount() + output <= energy.getCapacity()) {
			water.amount -= boiled;
			energy.setAmount(energy.getAmount() + output);
			setChanged();
		} else {
			output = 0;
		}
		lastOutput = output;
		EnergyNetworks.pushToNeighbors(level, pos, energy, MAX_OUTPUT, EnumSet.allOf(Direction.class));
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		energy.setAmount(input.getLong("energy").orElse(0L));
		water.readValue(input);
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		output.putLong("energy", energy.getAmount());
		water.writeValue(output);
	}
}
