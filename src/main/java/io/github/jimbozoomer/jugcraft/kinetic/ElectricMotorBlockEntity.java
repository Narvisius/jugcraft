package io.github.jimbozoomer.jugcraft.kinetic;

import io.github.jimbozoomer.jugcraft.energy.SimpleEnergyStorage;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/** Turns JE into KE (see {@link ElectricMotorBlock}); its block sets its {@link Stats}. */
public class ElectricMotorBlockEntity extends BlockEntity {
	/** The copper-wound motor's buffer. */
	public static final long CAPACITY = 8_000;
	/** JE per tick the copper-wound motor can take from cables. */
	public static final long INPUT = 256;
	/** KE per tick the copper-wound motor can put out. */
	public static final long OUTPUT = 96;
	/** KE the copper-wound motor makes per 100 JE. */
	public static final int EFFICIENCY_PERCENT = 75;

	/** Buffer (JE), JE taken and KE put out per tick, and KE made per 100 JE. */
	public record Stats(long capacity, long input, long output, int efficiencyPercent) {
	}

	public static final Stats COPPER = new Stats(CAPACITY, INPUT, OUTPUT, EFFICIENCY_PERCENT);
	/** Rare-earth magnets: four times the power, and far less lost. */
	public static final Stats MAGNET = new Stats(32_000, 1_024, 384, 95);

	private final Stats stats;
	final SimpleEnergyStorage energy;

	public ElectricMotorBlockEntity(BlockPos pos, BlockState state) {
		super(JugcraftKinetics.ELECTRIC_MOTOR_ENTITY, pos, state);
		stats = state.getBlock() instanceof ElectricMotorBlock motor ? motor.stats() : COPPER;
		energy = new SimpleEnergyStorage(stats.capacity(), stats.input(), 0, this::setChanged);
	}

	public Stats stats() {
		return stats;
	}

	public SimpleEnergyStorage energy() {
		return energy;
	}

	void serverTick(ServerLevel level, BlockPos pos, BlockState state) {
		long offer = Math.min(stats.output(), energy.getAmount() * stats.efficiencyPercent() / 100);
		long taken = offer > 0 ? KineticNetworks.push(level, pos, state.getValue(ElectricMotorBlock.FACING), offer) : 0;
		if (taken > 0) {
			// JE used, rounded up so the motor never makes power out of rounding.
			long used = (taken * 100 + stats.efficiencyPercent() - 1) / stats.efficiencyPercent();
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
