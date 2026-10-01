package io.github.jimbozoomer.jugcraft.fluid;

import io.github.jimbozoomer.jugcraft.energy.SimpleEnergyStorage;
import java.util.EnumSet;
import java.util.Set;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.fluid.base.SingleFluidStorage;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageUtil;
import net.fabricmc.fabric.api.transfer.v1.storage.base.FilteringStorage;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * The electric pump. Each tick with enough energy it:
 * <ol>
 * <li>pulls up to {@link #PUMP_MB_PER_TICK} mB from below: a fluid storage (such as a tank),
 * or a water/lava source block;</li>
 * <li>pushes its buffer out of its top and four sides, into pipes or adjacent fluid storages.</li>
 * </ol>
 * It uses energy only on ticks where it actually moved fluid. Its buffer can be drained by
 * others but not filled from outside, so fluid only ever flows upward through it.
 */
public class ElectricPumpBlockEntity extends BlockEntity {
	/** A pump's numbers: JE capacity, input and use per tick, mB pumped per tick, and its buffer in buckets. */
	public record Tier(long energyCapacity, long energyInput, long energyPerTick, long pumpMbPerTick, long bufferBuckets) {
		/** The bronze-age electric pump. */
		public static final Tier ELECTRIC = new Tier(4_000, 64, 8, 100, 4);
		/** The steel-tier heavy pump, for refinery flows. */
		public static final Tier HEAVY = new Tier(32_000, 512, 40, 1_000, 16);
	}

	public static final long ENERGY_CAPACITY = Tier.ELECTRIC.energyCapacity();
	public static final long ENERGY_INPUT = Tier.ELECTRIC.energyInput();
	public static final long ENERGY_PER_TICK = Tier.ELECTRIC.energyPerTick();
	public static final long PUMP_MB_PER_TICK = Tier.ELECTRIC.pumpMbPerTick();
	public static final long BUFFER = Tier.ELECTRIC.bufferBuckets() * FluidConstants.BUCKET;
	/** Ticks between lava source pulls: lava sources are consumed, one bucket each. */
	public static final int LAVA_COOLDOWN = 20;

	private static final Set<Direction> OUTPUTS = EnumSet.complementOf(EnumSet.of(Direction.DOWN));

	private final Tier tier;
	private final long pumpRate;
	private final long bufferCapacity;
	final SimpleEnergyStorage energy;
	final SingleFluidStorage buffer;
	private final Storage<FluidVariant> exposed;
	private int lavaCooldown;

	public ElectricPumpBlockEntity(BlockPos pos, BlockState state) {
		super(JugcraftFluids.PUMP_ENTITY, pos, state);
		this.tier = state.getBlock() instanceof ElectricPumpBlock pump ? pump.tier() : Tier.ELECTRIC;
		this.pumpRate = tier.pumpMbPerTick() * FluidNetworks.DROPLETS_PER_MB;
		this.bufferCapacity = tier.bufferBuckets() * FluidConstants.BUCKET;
		this.energy = new SimpleEnergyStorage(tier.energyCapacity(), tier.energyInput(), 0, this::setChanged);
		this.buffer = SingleFluidStorage.withFixedCapacity(bufferCapacity, this::setChanged);
		this.exposed = FilteringStorage.extractOnlyOf(buffer);
	}

	public Tier tier() {
		return tier;
	}

	public Storage<FluidVariant> fluidFor(Direction side) {
		return exposed;
	}

	public SimpleEnergyStorage energy() {
		return energy;
	}

	public long bufferedMb() {
		return buffer.amount / FluidNetworks.DROPLETS_PER_MB;
	}

	void serverTick(ServerLevel level, BlockPos pos) {
		if (lavaCooldown > 0) {
			lavaCooldown--;
		}
		if (energy.getAmount() < tier.energyPerTick()) {
			return;
		}
		long moved = pull(level, pos.below());
		if (buffer.amount > 0) {
			moved += FluidNetworks.pushToNeighbors(level, pos, buffer, pumpRate, OUTPUTS);
		}
		if (moved > 0) {
			energy.setAmount(energy.getAmount() - tier.energyPerTick());
			setChanged();
		}
	}

	/** Fills the buffer from below; returns droplets taken. */
	private long pull(ServerLevel level, BlockPos below) {
		long room = bufferCapacity - buffer.amount;
		if (room <= 0) {
			return 0;
		}
		Storage<FluidVariant> storage = FluidStorage.SIDED.find(level, below, Direction.UP);
		if (storage != null) {
			return StorageUtil.move(storage, buffer, variant -> true, Math.min(room, pumpRate), null);
		}

		BlockState state = level.getBlockState(below);
		FluidState fluid = level.getFluidState(below);
		if (!(state.getBlock() instanceof LiquidBlock) || !fluid.isSource()) {
			return 0;
		}
		if (fluid.getType() == Fluids.WATER) {
			// Water is treated as a spring: the source block is not removed, matching the steam generator.
			return insert(Fluids.WATER, Math.min(room, pumpRate));
		}
		if (fluid.getType() == Fluids.LAVA && lavaCooldown == 0 && room >= FluidConstants.BUCKET) {
			long taken = insert(Fluids.LAVA, FluidConstants.BUCKET);
			if (taken == FluidConstants.BUCKET) {
				level.setBlockAndUpdate(below, Blocks.AIR.defaultBlockState());
				lavaCooldown = LAVA_COOLDOWN;
			}
			return taken;
		}
		return 0;
	}

	private long insert(Fluid fluid, long amount) {
		try (Transaction transaction = Transaction.openOuter()) {
			long inserted = buffer.insert(FluidVariant.of(fluid), amount, transaction);
			if (inserted == amount) {
				transaction.commit();
				return inserted;
			}
			return 0;
		}
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		energy.setAmount(input.getLong("energy").orElse(0L));
		buffer.readValue(input);
		lavaCooldown = input.getInt("lava_cooldown").orElse(0);
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		output.putLong("energy", energy.getAmount());
		buffer.writeValue(output);
		output.putInt("lava_cooldown", lavaCooldown);
	}
}
