package io.github.jimbozoomer.jugcraft.powerline;

import io.github.jimbozoomer.jugcraft.chemistry.FluidTank;
import io.github.jimbozoomer.jugcraft.chemistry.PetroFluids;
import io.github.jimbozoomer.jugcraft.energy.EnergyNetworks;
import io.github.jimbozoomer.jugcraft.energy.EnergyStorage;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
import net.fabricmc.fabric.api.transfer.v1.transaction.base.SnapshotParticipant;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

/**
 * The anchor of one power line structure: which kind it is, the structures its wires run to, and how loaded its
 * line is (synced, for the wire sheen and the electrocution damage). A box also holds the energy passing through
 * it: what cables put in waits to go out over the line, what the line brings waits to go out into cables, so the
 * two directions never mix. A Unit Substation also has a nitrogen tank, for superconducting lines.
 */
public class LineAnchorBlockEntity extends BlockEntity {
	/** A box holds this many ticks of its capacity each way. */
	private static final int BUFFER_TICKS = 2;
	public static final int MAX_CONNECTIONS = 4;
	public static final int NITROGEN_TANK_MB = 8_000;

	private final List<BlockPos> connections = new ArrayList<>();
	private final BoxEnergy energy;
	public final @Nullable FluidTank nitrogen;
	/** How full the line through it ran last time it was measured, 0 to 255. */
	private int load;
	/** A Service Pedestal's meter: the energy it has delivered into cables (JE), and what the clients last saw. */
	private long delivered, deliveredShown;
	/** kWh a JE is (1 JE/t is about 2.3 kW; a tick is 0.05 s). */
	public static final double KWH_PER_JE = 2.3 * 0.05 / 3600;
	/**
	 * Its frame cells (offsets from the anchor) as placed: since power lines round 2 a tower turns to its line and stands
	 * as its tangent or angle body, so its legs move when its wires change (LineGeometry.frames). Null until known.
	 */
	private @Nullable List<BlockPos> frames;
	/** Structures whose frame waits to be moved to where they stand (loaded from before round 2, placed by a blueprint, boxes). */
	static final java.util.Set<LineAnchorBlockEntity> RELAY = java.util.Collections.newSetFromMap(new java.util.WeakHashMap<>());

	public LineAnchorBlockEntity(BlockPos pos, BlockState state) {
		super(JugcraftPowerLines.ANCHOR_ENTITY, pos, state);
		LineKind kind = kind(state);
		this.energy = kind.isBox() ? new BoxEnergy(kind.capacity * BUFFER_TICKS) : null;
		this.nitrogen = kind == LineKind.UNIT_SUBSTATION
				? new FluidTank(NITROGEN_TANK_MB, true, v -> PetroFluids.NITROGEN != null && v.isOf(PetroFluids.NITROGEN.fluid()), this::setChanged)
				: null;
	}

	private static LineKind kind(BlockState state) {
		return state.getBlock() instanceof LineAnchorBlock block ? block.kind : LineKind.WOODEN_POLE;
	}

	public LineKind kind() {
		return kind(getBlockState());
	}

	public List<BlockPos> connections() {
		return connections;
	}

	public @Nullable EnergyStorage energy() {
		return energy;
	}

	public long waitingToSend() {
		return energy == null ? 0 : energy.in;
	}

	public long roomToReceive() {
		return energy == null ? 0 : energy.capacity - energy.out;
	}

	/** The line moving energy: take up to {@code amount} from what waits to be sent. */
	long takeToSend(long amount) {
		long taken = Math.min(amount, energy.in);
		energy.in -= taken;
		return taken;
	}

	/** The line delivering energy into what waits to go out into cables. */
	long receive(long amount) {
		long accepted = Math.min(amount, energy.capacity - energy.out);
		energy.out += accepted;
		return accepted;
	}

	public int load() {
		return load;
	}

	/** What a Service Pedestal's meter reads: the energy it has delivered into cables (JE). */
	public long delivered() {
		return delivered;
	}

	void setLoad(int value) {
		value = Math.max(0, Math.min(255, value));
		if (Math.abs(value - load) > 6 || (value == 0) != (load == 0)) {
			load = value;
			changed();
		}
	}

	public boolean connect(BlockPos other) {
		if (connections.contains(other) || connections.size() >= MAX_CONNECTIONS || other.equals(getBlockPos())) {
			return false;
		}
		connections.add(other.immutable());
		relayFrames();
		changed();
		return true;
	}

	public void disconnect(BlockPos other) {
		if (connections.remove(other)) {
			relayFrames();
			changed();
		}
	}

	/** Replaces the connection list (used when a support is upgraded in place). */
	void setConnections(List<BlockPos> list) {
		connections.clear();
		connections.addAll(list);
		relayFrames();
		changed();
	}

	/** Its frame cells (offsets from the anchor). */
	public List<BlockPos> frames() {
		return frames != null ? frames : kind().legacyFrames();
	}

	/** Its layout with its wires as they are (how it faces, which body, each wire's conductor points). */
	public LineGeometry.Layout layout() {
		return LineGeometry.layout(kind(), getBlockPos(), connections, LineWires.kinds(level), LineGeometry.boxYaw(getBlockState()));
	}

	/** Notes the frame cells its placing put down (LinePlacement). */
	void setFrames(List<BlockPos> cells) {
		frames = List.copyOf(cells);
		if (level != null) {
			LineRegistry.of(level).add(getBlockPos(), kind(), frames);
		}
		setChanged();
	}

	/**
	 * Moves its frame to where its body now stands (server): the cells it no longer fills are cleared, the new ones
	 * filled where nothing else stands.
	 */
	void relayFrames() {
		if (!(level instanceof ServerLevel server)) {
			return;
		}
		BlockPos pos = getBlockPos();
		List<BlockPos> have = frames();
		List<BlockPos> want = LineGeometry.frames(layout());
		if (frames != null && want.equals(have)) {
			return;
		}
		LineRegistry registry = LineRegistry.of(server);
		List<BlockPos> stale = new ArrayList<>();
		for (BlockPos f : have) {
			if (!want.contains(f)) {
				stale.add(f);
			}
		}
		LinePlacement.clearFrames(server, pos, stale);
		List<BlockPos> placed = new ArrayList<>();
		registry.add(pos, kind(), want);
		for (BlockPos f : want) {
			BlockPos p = pos.offset(f);
			if (server.getBlockState(p).is(JugcraftPowerLines.FRAME) && have.contains(f)) {
				placed.add(f);
			} else if (!server.isOutsideBuildHeight(p) && server.getBlockState(p).canBeReplaced()) {
				server.setBlock(p, JugcraftPowerLines.FRAME.defaultBlockState(), Block.UPDATE_ALL);
				placed.add(f);
			}
		}
		frames = List.copyOf(placed);
		registry.add(pos, kind(), frames);
		setChanged();
	}

	public void serverTick(ServerLevel level) {
		if (energy != null && energy.out > 0) {
			// What the line brought goes out into the cables and machines touching the box.
			long moved = EnergyNetworks.pushToNeighbors(level, getBlockPos(), energy.outlet, energy.out, EnumSet.allOf(Direction.class));
			if (moved > 0 && kind() == LineKind.SERVICE_PEDESTAL) {
				delivered += moved;
				setChanged();
			}
		}
		if (delivered != deliveredShown && level.getGameTime() % 20 == (getBlockPos().asLong() & 15)
				&& (long) (delivered * KWH_PER_JE) != (long) (deliveredShown * KWH_PER_JE)) {
			// The meter's display counts whole kWh: told to the clients when it moves on, at most once a second.
			deliveredShown = delivered;
			changed();
		}
		if (level.getGameTime() % 100 == (getBlockPos().asLong() & 63)) {
			// Forget wires to structures that are gone (for example broken while this chunk was unloaded).
			for (BlockPos other : List.copyOf(connections)) {
				if (level.isLoaded(other) && !(level.getBlockEntity(other) instanceof LineAnchorBlockEntity)) {
					disconnect(other);
					LineNetworks.invalidate(level);
				}
			}
		}
	}

	void changed() {
		setChanged();
		if (level != null && !level.isClientSide()) {
			level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
		}
	}

	@Override
	public void setLevel(Level level) {
		super.setLevel(level);
		LineRegistry.of(level).add(getBlockPos(), kind(), frames());
		if ((frames == null || kind().isBox()) && !level.isClientSide()) {
			// (a box saved before it turned to its line: its yard moves when it next loads)
			RELAY.add(this);
		}
	}

	/** Broken: its frame goes, and the structures it was wired to forget it (unless it is being upgraded in place). */
	@Override
	public void preRemoveSideEffects(BlockPos pos, BlockState state) {
		super.preRemoveSideEffects(pos, state);
		if (!(level instanceof ServerLevel server)) {
			return;
		}
		LinePlacement.clearFrames(server, pos, LineRegistry.of(server).framesOf(pos));
		if (!LinePlacement.UPGRADING.get()) {
			for (BlockPos other : connections) {
				if (server.isLoaded(other) && server.getBlockEntity(other) instanceof LineAnchorBlockEntity partner) {
					partner.disconnect(pos);
				}
			}
		}
		LineNetworks.invalidate(server);
	}

	@Override
	public void setRemoved() {
		super.setRemoved();
		if (level != null) {
			LineRegistry.of(level).remove(getBlockPos(), kind());
			if (!level.isClientSide()) {
				LineNetworks.invalidate(level);
			}
		}
	}

	@Override
	public Packet<ClientGamePacketListener> getUpdatePacket() {
		return ClientboundBlockEntityDataPacket.create(this);
	}

	@Override
	public net.minecraft.nbt.CompoundTag getUpdateTag(net.minecraft.core.HolderLookup.Provider registries) {
		net.minecraft.world.level.storage.TagValueOutput output = net.minecraft.world.level.storage.TagValueOutput.createWithContext(
				net.minecraft.util.ProblemReporter.DISCARDING, registries);
		output.store("connections", BlockPos.CODEC.listOf(), List.copyOf(connections));
		output.putInt("load", load);
		output.store("frames", BlockPos.CODEC.listOf(), frames());
		if (delivered > 0) {
			output.putLong("delivered", delivered);
		}
		return output.buildResult();
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		connections.clear();
		connections.addAll(input.read("connections", BlockPos.CODEC.listOf()).orElse(List.of()));
		load = input.getIntOr("load", 0);
		delivered = input.getLongOr("delivered", 0L);
		deliveredShown = delivered;
		java.util.Optional<List<BlockPos>> cells = input.read("frames", BlockPos.CODEC.listOf());
		if (cells.isPresent()) {
			frames = List.copyOf(cells.get());
		}
		if (level != null && level.isClientSide()) {
			LineRegistry.of(level).add(getBlockPos(), kind(), frames());
		}
		if (energy != null) {
			energy.in = Math.min(energy.capacity, input.getLongOr("energy_in", 0L));
			energy.out = Math.min(energy.capacity, input.getLongOr("energy_out", 0L));
		}
		if (nitrogen != null) {
			nitrogen.readValue(input);
		}
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		output.store("connections", BlockPos.CODEC.listOf(), List.copyOf(connections));
		output.putInt("load", load);
		if (delivered > 0) {
			output.putLong("delivered", delivered);
		}
		if (frames != null) {
			output.store("frames", BlockPos.CODEC.listOf(), frames);
		}
		if (energy != null) {
			output.putLong("energy_in", energy.in);
			output.putLong("energy_out", energy.out);
		}
		if (nitrogen != null) {
			nitrogen.writeValue(output);
		}
	}

	/**
	 * A box's energy as cables see it: inserting fills what waits to be sent over the line; extracting takes from
	 * what the line delivered.
	 */
	final class BoxEnergy extends SnapshotParticipant<long[]> implements EnergyStorage {
		final long capacity;
		long in;
		long out;
		/** The side the box pushes its delivered energy out of (extract only, so it never takes its own push back). */
		final EnergyStorage outlet = new EnergyStorage() {
			@Override
			public boolean supportsInsertion() {
				return false;
			}

			@Override
			public long insert(long maxAmount, TransactionContext transaction) {
				return 0;
			}

			@Override
			public boolean supportsExtraction() {
				return true;
			}

			@Override
			public long extract(long maxAmount, TransactionContext transaction) {
				return BoxEnergy.this.extract(maxAmount, transaction);
			}

			@Override
			public long getAmount() {
				return out;
			}

			@Override
			public long getCapacity() {
				return capacity;
			}
		};

		BoxEnergy(long capacity) {
			this.capacity = capacity;
		}

		@Override
		public boolean supportsInsertion() {
			return true;
		}

		@Override
		public long insert(long maxAmount, TransactionContext transaction) {
			long accepted = Math.min(maxAmount, capacity - in);
			if (accepted > 0) {
				updateSnapshots(transaction);
				in += accepted;
			}
			return Math.max(0, accepted);
		}

		@Override
		public boolean supportsExtraction() {
			return true;
		}

		@Override
		public long extract(long maxAmount, TransactionContext transaction) {
			long removed = Math.min(maxAmount, out);
			if (removed > 0) {
				updateSnapshots(transaction);
				out -= removed;
			}
			return Math.max(0, removed);
		}

		@Override
		public long getAmount() {
			return in + out;
		}

		@Override
		public long getCapacity() {
			return capacity * 2;
		}

		@Override
		protected long[] createSnapshot() {
			return new long[] {in, out};
		}

		@Override
		protected void readSnapshot(long[] snapshot) {
			in = snapshot[0];
			out = snapshot[1];
		}

		@Override
		protected void onFinalCommit() {
			setChanged();
		}
	}
}
