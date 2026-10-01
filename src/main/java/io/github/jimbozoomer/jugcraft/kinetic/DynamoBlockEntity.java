package io.github.jimbozoomer.jugcraft.kinetic;

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

/** Turns KE into JE (see {@link DynamoBlock}). */
public class DynamoBlockEntity extends BlockEntity implements KineticConsumer {
	public static final long CAPACITY = 8_000;
	/** KE per tick it can take, and JE per tick it pushes out. */
	public static final long RATE = 128;
	/** JE made per 100 KE. */
	public static final int EFFICIENCY_PERCENT = 75;

	final SimpleEnergyStorage energy = new SimpleEnergyStorage(CAPACITY, 0, RATE, this::setChanged);
	/** Hundredths of a JE carried over between ticks, so small inputs are not rounded away. */
	private int remainder;
	/** KE taken so far in game tick {@link #takenTick}: a network may offer power twice a tick (an even share, then
	 * what is left), and several sources may drive one dynamo, but it takes at most {@link #RATE} a tick in all. */
	private long takenThisTick;
	private long takenTick = -1;

	public DynamoBlockEntity(BlockPos pos, BlockState state) {
		super(JugcraftKinetics.DYNAMO_ENTITY, pos, state);
	}

	public SimpleEnergyStorage energy() {
		return energy;
	}

	@Override
	public long acceptKinetic(Direction side, long maxAmount) {
		long now = level == null ? 0 : level.getGameTime();
		if (now != takenTick) {
			takenTick = now;
			takenThisTick = 0;
		}
		long room = energy.getCapacity() - energy.getAmount();
		long take = Math.min(Math.min(maxAmount, RATE - takenThisTick), room * 100 / EFFICIENCY_PERCENT);
		if (take <= 0) {
			return 0;
		}
		takenThisTick += take;
		long hundredths = take * EFFICIENCY_PERCENT + remainder;
		energy.setAmount(Math.min(energy.getCapacity(), energy.getAmount() + hundredths / 100));
		remainder = (int) (hundredths % 100);
		setChanged();
		return take;
	}

	void serverTick(ServerLevel level, BlockPos pos) {
		if (energy.getAmount() > 0) {
			EnergyNetworks.pushToNeighbors(level, pos, energy, RATE, EnumSet.allOf(Direction.class));
		}
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		energy.setAmount(input.getLong("energy").orElse(0L));
		remainder = input.getInt("remainder").orElse(0);
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		output.putLong("energy", energy.getAmount());
		output.putInt("remainder", remainder);
	}
}
