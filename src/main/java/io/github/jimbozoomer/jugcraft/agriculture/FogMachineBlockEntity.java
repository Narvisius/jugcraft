package io.github.jimbozoomer.jugcraft.agriculture;

import io.github.jimbozoomer.jugcraft.energy.SimpleEnergyStorage;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * The Fog Machine's power and fog. On the server: switched on (by hand or by a redstone signal) and holding at least
 * {@value #USE} JE, it runs and uses that much a tick (buffer {@value #CAPACITY}, taking up to {@value #INPUT} a tick
 * from the network). On a client, a running machine within {@value #VIEW} blocks of a player puffs fog onto the
 * ground within its radius: at most {@value #PARTICLES_PER_TICK} puffs a tick a machine, and {@value #BUDGET} a tick
 * for all machines together, so fog never grows without bound. The fog is only particles: it changes no block,
 * hides nothing from the server and lets nobody through walls.
 */
public class FogMachineBlockEntity extends BlockEntity {
	public static final int USE = 16;
	public static final int CAPACITY = 4000;
	public static final int INPUT = 64;
	public static final int[] RADII = {4, 8, 12, 16};
	public static final int PARTICLES_PER_TICK = 6;
	public static final int BUDGET = 24;
	public static final int VIEW = 48;
	/** How often the server looks at the redstone signal again (also on every neighbour change). */
	public static final int PERIOD = 10;

	private static long budgetTick = Long.MIN_VALUE;
	private static int budgetLeft;

	final SimpleEnergyStorage energy = new SimpleEnergyStorage(CAPACITY, INPUT, 0, this::setChanged);
	private boolean powered;

	public FogMachineBlockEntity(BlockPos pos, BlockState state) {
		super(JugcraftAgriculture.FOG_MACHINE_ENTITY, pos, state);
	}

	public SimpleEnergyStorage energy() {
		return energy;
	}

	/** Whether it is switched on: by hand, or by a redstone signal. */
	public boolean switchedOn() {
		return getBlockState().getValue(FogMachineBlock.ENABLED) || powered;
	}

	/** Reads the redstone signal and works out whether it runs this tick, using the energy if so. */
	public boolean update(ServerLevel level) {
		powered = level.hasNeighborSignal(worldPosition);
		return run(level);
	}

	void serverTick(ServerLevel level) {
		if (Math.floorMod(level.getGameTime() + worldPosition.asLong(), PERIOD) == 0) {
			powered = level.hasNeighborSignal(worldPosition);
		}
		run(level);
	}

	private boolean run(ServerLevel level) {
		boolean running = switchedOn() && energy.getAmount() >= USE;
		if (running) {
			energy.setAmount(energy.getAmount() - USE);
			setChanged();
		}
		BlockState state = getBlockState();
		if (state.getValue(FogMachineBlock.RUNNING) != running) {
			level.setBlock(worldPosition, state.setValue(FogMachineBlock.RUNNING, running), Block.UPDATE_ALL);
		}
		return running;
	}

	/** Client side: puffs of fog on the ground round a running machine, within this tick's budget. */
	void clientTick(Level level, BlockState state) {
		if (!state.getValue(FogMachineBlock.RUNNING)) {
			return;
		}
		double cx = worldPosition.getX() + 0.5;
		double cz = worldPosition.getZ() + 0.5;
		if (level.getNearestPlayer(cx, worldPosition.getY(), cz, VIEW, false) == null) {
			return;
		}
		if (budgetTick != level.getGameTime()) {
			budgetTick = level.getGameTime();
			budgetLeft = BUDGET;
		}
		RandomSource random = level.getRandom();
		int radius = FogMachineBlock.radius(state);
		int puffs = Math.min(budgetLeft, Math.min(PARTICLES_PER_TICK, 2 + state.getValue(FogMachineBlock.RADIUS)));
		budgetLeft -= puffs;
		Direction front = state.getValue(FogMachineBlock.FACING);
		for (int i = 0; i < puffs; i++) {
			double x;
			double z;
			double vx;
			double vz;
			if (i == 0) {
				// One rolls out of the nozzle each tick.
				x = cx + front.getStepX() * 0.6;
				z = cz + front.getStepZ() * 0.6;
				vx = front.getStepX() * 0.06 + (random.nextDouble() - 0.5) * 0.02;
				vz = front.getStepZ() * 0.06 + (random.nextDouble() - 0.5) * 0.02;
			} else {
				double angle = random.nextDouble() * Math.PI * 2.0;
				double distance = Math.sqrt(random.nextDouble()) * radius;
				x = cx + Math.cos(angle) * distance;
				z = cz + Math.sin(angle) * distance;
				vx = Math.cos(angle) * 0.01 + (random.nextDouble() - 0.5) * 0.01;
				vz = Math.sin(angle) * 0.01 + (random.nextDouble() - 0.5) * 0.01;
			}
			double y = ground(level, x, z);
			if (!Double.isNaN(y)) {
				level.addParticle(JugcraftAgriculture.FOG, x, y + 0.15 + random.nextDouble() * 0.25, z, vx, 0.0, vz);
			}
		}
	}

	/** The top of the ground at (x, z), within a few blocks above or below the machine, or NaN if there is none. */
	private double ground(Level level, double x, double z) {
		BlockPos.MutableBlockPos at = new BlockPos.MutableBlockPos(Math.floor(x), worldPosition.getY() + 2, Math.floor(z));
		for (int i = 0; i < 7; i++, at.move(Direction.DOWN)) {
			BlockState here = level.getBlockState(at);
			if (!here.getCollisionShape(level, at).isEmpty() && level.getBlockState(at.above()).getCollisionShape(level, at.above()).isEmpty()) {
				return at.getY() + here.getCollisionShape(level, at).max(Direction.Axis.Y);
			}
		}
		return Double.NaN;
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
