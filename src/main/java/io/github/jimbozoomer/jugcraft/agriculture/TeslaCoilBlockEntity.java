package io.github.jimbozoomer.jugcraft.agriculture;

import io.github.jimbozoomer.jugcraft.energy.SimpleEnergyStorage;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

/**
 * The Tesla Coil's power and arcs, on its lower half. On the server: switched on and holding at least {@value #USE} JE,
 * it runs and uses that much a tick (buffer {@value #CAPACITY}, taking up to {@value #INPUT} a tick from the network).
 * Every {@value #ARC_MIN} to {@value #ARC_MIN} + {@value #ARC_SPREAD} ticks a running coil throws an arc to another
 * running coil within {@value #RANGE} blocks (one at random), or into the air above it if there is none: a block event
 * tells the clients where, and they draw it for {@value #ARC_TICKS} ticks. The arcs are harmless; nothing is struck.
 * Running coils are kept in a set per level, so finding a partner never searches blocks.
 */
public class TeslaCoilBlockEntity extends BlockEntity {
	public static final int USE = 20;
	public static final int CAPACITY = 4000;
	public static final int INPUT = 64;
	public static final int RANGE = 8;
	public static final int ARC_MIN = 15;
	public static final int ARC_SPREAD = 25;
	public static final int ARC_TICKS = 6;
	/** The block event that carries an arc (its parameter is the packed offset, see {@link #pack}). */
	public static final int ARC = 1;
	/** Set in a packed offset when the arc goes into the air rather than to another coil. */
	public static final int INTO_AIR = 1 << 15;
	private static final Map<Level, Set<BlockPos>> RUNNING = new WeakHashMap<>();

	final SimpleEnergyStorage energy = new SimpleEnergyStorage(CAPACITY, INPUT, 0, this::setChanged);
	private long nextArc;
	private int arc;
	private long arcTime = Long.MIN_VALUE;

	public TeslaCoilBlockEntity(BlockPos pos, BlockState state) {
		super(JugcraftAgriculture.TESLA_COIL_ENTITY, pos, state);
	}

	public SimpleEnergyStorage energy() {
		return energy;
	}

	/** An offset of up to 15 blocks each way, packed into one block event parameter. */
	public static int pack(int dx, int dy, int dz) {
		return (dx + 16 & 31) | (dy + 16 & 31) << 5 | (dz + 16 & 31) << 10;
	}

	/** The offset in a packed parameter: {dx, dy, dz}. */
	public static int[] unpack(int packed) {
		return new int[] {(packed & 31) - 16, (packed >> 5 & 31) - 16, (packed >> 10 & 31) - 16};
	}

	/** The last arc a client was told of (packed) and the game time it struck. */
	public int arc() {
		return arc;
	}

	public long arcTime() {
		return arcTime;
	}

	void serverTick(ServerLevel level) {
		BlockState state = getBlockState();
		boolean running = state.getValue(TeslaCoilBlock.ENABLED) && energy.getAmount() >= USE;
		if (running) {
			energy.setAmount(energy.getAmount() - USE);
			setChanged();
		}
		if (state.getValue(TeslaCoilBlock.ACTIVE) != running) {
			TeslaCoilBlock.setActive(level, worldPosition, state, running);
		}
		Set<BlockPos> coils = RUNNING.computeIfAbsent(level, l -> new HashSet<>());
		if (!running) {
			coils.remove(worldPosition);
			return;
		}
		coils.add(worldPosition.immutable());
		long time = level.getGameTime();
		if (time < nextArc) {
			return;
		}
		RandomSource random = level.getRandom();
		nextArc = time + ARC_MIN + random.nextInt(ARC_SPREAD);
		BlockPos partner = partner(level, coils, random);
		int packed = partner != null
				? pack(partner.getX() - worldPosition.getX(), partner.getY() - worldPosition.getY(), partner.getZ() - worldPosition.getZ())
				: pack(random.nextInt(5) - 2, 1 + random.nextInt(2), random.nextInt(5) - 2) | INTO_AIR;
		level.blockEvent(worldPosition, state.getBlock(), ARC, packed);
		level.playSound(null, worldPosition.above(), SoundEvents.LIGHTNING_BOLT_IMPACT, SoundSource.BLOCKS, 0.25F, 1.6F + random.nextFloat() * 0.4F);
	}

	/** The running coils within {@code range} blocks of {@code pos}, from the per-level set (no blocks are searched). */
	public static List<TeslaCoilBlockEntity> runningNear(Level level, BlockPos pos, int range) {
		Set<BlockPos> coils = RUNNING.get(level);
		List<TeslaCoilBlockEntity> near = new ArrayList<>();
		if (coils == null) {
			return near;
		}
		for (BlockPos coil : coils) {
			if (coil.distSqr(pos) <= (double) range * range && level.isLoaded(coil) && level.getBlockEntity(coil) instanceof TeslaCoilBlockEntity running) {
				near.add(running);
			}
		}
		return near;
	}

	/** Another running coil within range, picked at random; forgets coils that are gone. */
	private @Nullable BlockPos partner(ServerLevel level, Set<BlockPos> coils, RandomSource random) {
		List<BlockPos> near = new ArrayList<>();
		for (Iterator<BlockPos> it = coils.iterator(); it.hasNext(); ) {
			BlockPos other = it.next();
			if (other.equals(worldPosition) || other.distSqr(worldPosition) > RANGE * RANGE) {
				continue;
			}
			if (!level.isLoaded(other) || !(level.getBlockEntity(other) instanceof TeslaCoilBlockEntity coil)
					|| !coil.getBlockState().getValue(TeslaCoilBlock.ACTIVE)) {
				it.remove();
				continue;
			}
			near.add(other);
		}
		return near.isEmpty() ? null : near.get(random.nextInt(near.size()));
	}

	/** A client hears of an arc: where it went and when. */
	@Override
	public boolean triggerEvent(int id, int param) {
		if (id == ARC) {
			arc = param;
			arcTime = level != null ? level.getGameTime() : 0;
			return true;
		}
		return super.triggerEvent(id, param);
	}

	@Override
	public void setRemoved() {
		super.setRemoved();
		// Server levels only: the map is the server's, and client block entities are removed on the render thread.
		if (level != null && !level.isClientSide() && RUNNING.containsKey(level)) {
			RUNNING.get(level).remove(worldPosition);
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
