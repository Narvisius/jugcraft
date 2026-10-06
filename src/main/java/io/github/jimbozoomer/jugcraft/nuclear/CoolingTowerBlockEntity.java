package io.github.jimbozoomer.jugcraft.nuclear;

import io.github.jimbozoomer.jugcraft.blueprint.Blueprint;
import io.github.jimbozoomer.jugcraft.blueprint.SurveyStakeBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.ProblemReporter;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * A cooling tower's controller. Every few seconds it checks its tower against the tower's blueprint (a spread of the
 * blueprint's blocks must be in place around it); a formed tower lends its capacity to the reactors whose condensers
 * reach it by conduit, a share of a standard unit's heat each tick, and steams in proportion to what it took.
 */
public class CoolingTowerBlockEntity extends BlockEntity {
	/** Blocks of the tower's blueprint checked each time (spread over the whole tower). */
	public static final int SAMPLES = 160;
	/** Ticks between checks of the tower. */
	public static final int CHECK_INTERVAL = 100;

	private boolean formed;
	/** Share of capacity used, averaged over the last second (synced: the plume follows it). */
	private float load;
	private long claimTick = Long.MIN_VALUE;
	private double used;
	private double usedSum;
	private int usedTicks;

	public CoolingTowerBlockEntity(BlockPos pos, BlockState state) {
		super(JugcraftNuclear.COOLING_TOWER_ENTITY, pos, state);
	}

	public CoolingTowerBlock.Tower tower() {
		return getBlockState().getValue(CoolingTowerBlock.TOWER);
	}

	public boolean formed() {
		return formed;
	}

	public float load() {
		return load;
	}

	/**
	 * Takes up to {@code want} of this tower's capacity for this tick (in standard units of reactor heat). Returns what
	 * it could give: nothing unless the tower is formed, and never more than its capacity among all its users.
	 */
	public double claim(long gameTime, double want) {
		if (!formed || want <= 0) {
			return 0;
		}
		if (gameTime != claimTick) {
			claimTick = gameTime;
			used = 0;
		}
		double give = Math.max(0, Math.min(want, tower().capacity - used));
		used += give;
		usedSum += give;
		return give;
	}

	/** Rotation of the tower from its blueprint as drawn (the controller faces south in the file). */
	public static Rotation rotationOf(Direction facing) {
		return switch (facing) {
			case WEST -> Rotation.CLOCKWISE_90;
			case NORTH -> Rotation.CLOCKWISE_180;
			case EAST -> Rotation.COUNTERCLOCKWISE_90;
			default -> Rotation.NONE;
		};
	}

	public void serverTick(ServerLevel level) {
		long now = level.getGameTime();
		if (Math.floorMod(now + getBlockPos().hashCode(), CHECK_INTERVAL) == 0) {
			boolean was = formed;
			formed = check(level);
			if (was != formed) {
				sync();
			}
		}
		if (++usedTicks >= 20) {
			float next = (float) Math.min(1, usedSum / usedTicks / tower().capacity);
			usedSum = 0;
			usedTicks = 0;
			if (Math.abs(next - load) > 0.04F || (next == 0) != (load == 0)) {
				load = next;
				sync();
			}
		}
	}

	/** Checks the tower now (normally every few seconds); for the game tests. */
	public boolean checkNow(ServerLevel level) {
		formed = check(level);
		sync();
		return formed;
	}

	/** Is the tower around this controller built? Compares a spread of its blueprint's blocks with the world. */
	private boolean check(ServerLevel level) {
		Blueprint blueprint = Blueprint.get(tower().blueprint, false);
		if (blueprint == null) {
			return false;
		}
		Rotation rotation = rotationOf(getBlockState().getValue(CoolingTowerBlock.FACING));
		int self = controllerCell(blueprint);
		if (self < 0) {
			return false;
		}
		BlockPos selfOffset = blueprint.offset(self, rotation);
		int checked = 0, matched = 0;
		int step = Math.max(1, blueprint.size() / SAMPLES);
		for (int i = step / 2; i < blueprint.size(); i += step) {
			BlockState wanted = blueprint.state(i, rotation);
			if (!wanted.getFluidState().isEmpty()) {
				continue;
			}
			BlockPos at = getBlockPos().offset(blueprint.offset(i, rotation).subtract(selfOffset));
			if (!level.isLoaded(at)) {
				continue;
			}
			checked++;
			if (SurveyStakeBlockEntity.matches(level.getBlockState(at), wanted)) {
				matched++;
			}
		}
		return checked >= SAMPLES / 3 && matched >= checked * 0.9;
	}

	private static int controllerCell(Blueprint blueprint) {
		for (int i = 0; i < blueprint.size(); i++) {
			if (blueprint.state(i, Rotation.NONE).getBlock() instanceof CoolingTowerBlock) {
				return i;
			}
		}
		return -1;
	}

	/** Client: the plume, from the top of the tower (or each fan), as strong as the tower is busy. */
	public void clientTick(Level level) {
		if (load <= 0.01F || !formed) {
			return;
		}
		RandomSource random = level.getRandom();
		CoolingTowerBlock.Tower tower = tower();
		Direction facing = getBlockState().getValue(CoolingTowerBlock.FACING);
		// The fans run along the bank's length: across the controller's facing.
		Direction along = facing.getClockWise();
		double perTick = tower == CoolingTowerBlock.Tower.MECHANICAL ? 0.35 : 2.5;
		for (int fan : tower.fans) {
			double count = perTick * load;
			while (count > 0 && (count >= 1 || random.nextDouble() < count)) {
				count -= 1;
				double angle = random.nextDouble() * Math.PI * 2, r = Math.sqrt(random.nextDouble()) * tower.plumeRadius;
				double x = getBlockPos().getX() + 0.5 + along.getStepX() * fan + Math.cos(angle) * r;
				double z = getBlockPos().getZ() + 0.5 + along.getStepZ() * fan + Math.sin(angle) * r;
				double y = getBlockPos().getY() + tower.plumeHeight + 1;
				level.addAlwaysVisibleParticle(JugcraftNuclear.COOLING_PLUME, true, x, y, z, 0.03, 0.25 + random.nextDouble() * 0.1, 0.01);
			}
		}
	}

	private void sync() {
		setChanged();
		if (level != null && !level.isClientSide()) {
			level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
		}
	}

	@Override
	public Packet<ClientGamePacketListener> getUpdatePacket() {
		return ClientboundBlockEntityDataPacket.create(this);
	}

	@Override
	public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
		TagValueOutput out = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, registries);
		saveAdditional(out);
		return out.buildResult();
	}

	@Override
	protected void loadAdditional(ValueInput in) {
		super.loadAdditional(in);
		formed = in.getBooleanOr("formed", false);
		load = in.getFloatOr("load", 0);
	}

	@Override
	protected void saveAdditional(ValueOutput out) {
		super.saveAdditional(out);
		out.putBoolean("formed", formed);
		out.putFloat("load", load);
	}
}
