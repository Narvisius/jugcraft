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

/** Turns KE into JE (see {@link DynamoBlock}); its block sets its {@link Stats}. */
public class DynamoBlockEntity extends BlockEntity implements KineticConsumer {
	/** The copper-wound dynamo's buffer. */
	public static final long CAPACITY = 8_000;
	/** KE per tick the copper-wound dynamo can take, and JE per tick it pushes out. */
	public static final long RATE = 128;
	/** JE the copper-wound dynamo makes per 100 KE. */
	public static final int EFFICIENCY_PERCENT = 75;

	/** Buffer (JE), KE taken and JE pushed per tick, and JE made per 100 KE. */
	public record Stats(long capacity, long rate, int efficiencyPercent) {
	}

	public static final Stats COPPER = new Stats(CAPACITY, RATE, EFFICIENCY_PERCENT);
	/** Rare-earth magnets: four times the rate, and far less lost. */
	public static final Stats MAGNET = new Stats(32_000, 512, 95);

	private final Stats stats;
	final SimpleEnergyStorage energy;
	/** Hundredths of a JE carried over between ticks, so small inputs are not rounded away. */
	private int remainder;
	/** KE taken so far in game tick {@link #takenTick}: a network may offer power twice a tick (an even share, then
	 * what is left), and several sources may drive one dynamo, but it takes at most its rate a tick in all. */
	private long takenThisTick;
	private long takenTick = -1;

	public DynamoBlockEntity(BlockPos pos, BlockState state) {
		super(JugcraftKinetics.DYNAMO_ENTITY, pos, state);
		stats = state.getBlock() instanceof DynamoBlock dynamo ? dynamo.stats() : COPPER;
		energy = new SimpleEnergyStorage(stats.capacity(), 0, stats.rate(), this::setChanged);
	}

	public Stats stats() {
		return stats;
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
		long take = Math.min(Math.min(maxAmount, stats.rate() - takenThisTick), room * 100 / stats.efficiencyPercent());
		if (take <= 0) {
			return 0;
		}
		takenThisTick += take;
		long hundredths = take * stats.efficiencyPercent() + remainder;
		energy.setAmount(Math.min(energy.getCapacity(), energy.getAmount() + hundredths / 100));
		remainder = (int) (hundredths % 100);
		setChanged();
		return take;
	}

	void serverTick(ServerLevel level, BlockPos pos) {
		if (energy.getAmount() > 0) {
			EnergyNetworks.pushToNeighbors(level, pos, energy, stats.rate(), EnumSet.allOf(Direction.class));
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
