package io.github.jimbozoomer.jugcraft.nuclear;

import io.github.jimbozoomer.jugcraft.chemistry.FluidTank;
import io.github.jimbozoomer.jugcraft.energy.EnergyNetworks;
import io.github.jimbozoomer.jugcraft.energy.SimpleEnergyStorage;
import io.github.jimbozoomer.jugcraft.party.JugcraftParties;
import io.github.jimbozoomer.jugcraft.party.UseMode;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.UUIDUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import com.mojang.serialization.Codec;
import org.jspecify.annotations.Nullable;

/**
 * The block entity of every piece of nuclear equipment, at its anchor. What it holds depends on the kind: the reactor
 * vessel has the core ({@link ReactorCore}) and works out its unit ({@link ReactorUnit}); the condenser holds cooling
 * water; the main transformer holds the unit's electric output and sends it into cables and power line boxes; the
 * control console holds its link to a vessel. Pumps, steam generators and the turbine only need to exist.
 */
public class EquipmentBlockEntity extends BlockEntity {
	/** Condenser water (mB): a little over two seconds at full power, so supply has to keep up. */
	public static final int CONDENSER_TANK = 10_000;
	/** Ticks between syncs of the reactor's readouts to clients. */
	public static final int SYNC_INTERVAL = 10;
	/** Radioactivity of a spent assembly (mSv/h a block away), also on the item. */
	public static final float SPENT_RADIOACTIVITY = 20_000F;

	private @Nullable UUID owner;
	private UseMode mode = UseMode.PERSONAL;
	// Reactor vessel.
	public final ReactorCore core = new ReactorCore();
	private @Nullable ReactorUnit unit;
	private int unitVersion = -1;
	private int ticks;
	private boolean sourceSet;
	/** What the console lists as missing (synced). */
	public List<String> missing = List.of();
	public int loops;
	// Condenser.
	public final @Nullable FluidTank water;
	private List<BlockPos> intakeCells = List.of();
	/** Cooling tower controllers this condenser's conduit reaches. */
	private List<BlockPos> towers = List.of();
	/** Circulating water pumps on this condenser's conduit (the towers need them to move the water). */
	private List<BlockPos> cwPumps = List.of();
	/** Each circulating water pump moves the water for half a standard unit's heat. */
	public static final double PUMP_CAPACITY = 0.5;
	/** The pumps' and fans' power, taken from the unit's output while cooling towers carry its heat. */
	public static final double HOUSE_LOAD = 0.03;
	/** Last tick's cooling tower share (for the house load). */
	private double lastTowerShare;
	private int intakeVersion = -1;
	// Main transformer.
	public final @Nullable SimpleEnergyStorage output;
	private List<BlockPos> outletCells = List.of();
	// Control console.
	public @Nullable BlockPos linked;

	public EquipmentBlockEntity(BlockPos pos, BlockState state) {
		super(JugcraftNuclear.EQUIPMENT_ENTITY, pos, state);
		EquipmentKind kind = kind(state);
		water = kind.cooled()
				? new FluidTank(CONDENSER_TANK, true, v -> v.getFluid() == Fluids.WATER, this::setChanged) : null;
		long room = kind == EquipmentKind.SMALL_TURBINE ? ReactorCore.Design.SMALL_MODULAR.ratedJe() * 2 : ReactorCore.RATED_JE * 2;
		output = kind.powerOut() ? new SimpleEnergyStorage(room, 0, room, this::setChanged) : null;
		core.design = kind.design();
	}

	static EquipmentKind kind(BlockState state) {
		return ((EquipmentAnchorBlock) state.getBlock()).kind;
	}

	public EquipmentKind kind() {
		return kind(getBlockState());
	}

	public Direction facing() {
		return getBlockState().getValue(EquipmentAnchorBlock.FACING);
	}

	public @Nullable UUID owner() {
		return owner;
	}

	public void setOwner(@Nullable UUID owner) {
		this.owner = owner;
		setChanged();
	}

	public UseMode mode() {
		return mode;
	}

	public void toggleMode() {
		mode = mode == UseMode.PERSONAL ? UseMode.PARTY : UseMode.PERSONAL;
		changed();
	}

	/** The owner, a creative-mode player, or (in Party mode) anyone in the owner's party. */
	public boolean mayUse(Player player) {
		if (owner == null || owner.equals(player.getUUID()) || player.getAbilities().instabuild) {
			return true;
		}
		return mode == UseMode.PARTY && JugcraftParties.sameParty(owner, player.getUUID());
	}

	public @Nullable ReactorUnit unit() {
		return unit;
	}

	// ------------------------------------------------------------------ ticking

	public void serverTick(ServerLevel level) {
		EquipmentKind kind = kind();
		if (kind.reactor()) {
			tickVessel(level);
		}
		if (kind.cooled()) {
			tickCondenser(level);
		}
		if (kind.powerOut()) {
			tickTransformer(level);
		}
	}

	private void tickVessel(ServerLevel level) {
		EquipmentRegistry registry = EquipmentRegistry.of(level);
		if (unit == null || unitVersion != registry.version()) {
			unit = ReactorUnit.find(level, getBlockPos());
			unitVersion = registry.version();
			missing = unit.missing();
			loops = unit.loops();
		}
		ReactorUnit u = unit;
		double towers = towerShare(level, u);
		lastTowerShare = towers;
		long electric = core.tick(new ReactorCore.Plant(u.cooling(), u.steamToPower(), u.formed(), u.generatorLimit(), towers),
				need -> drawWater(level, u, need));
		if (towers > 0 && electric > 0) {
			// The circulating water pumps and the towers' fans run on the plant's own power.
			electric = Math.round(electric * (1 - HOUSE_LOAD * towers));
			core.electric = electric;
		}
		if (electric > 0) {
			for (BlockPos t : u.transformers) {
				if (level.getBlockEntity(t) instanceof EquipmentBlockEntity transformer && transformer.output != null) {
					long room = transformer.output.getCapacity() - transformer.output.getAmount();
					long put = Math.min(room, electric);
					transformer.output.setAmount(transformer.output.getAmount() + put);
					electric -= put;
					if (electric <= 0) break;
				}
			}
		}
		updateRadiation(level);
		if (++ticks % SYNC_INTERVAL == 0) {
			changed();
		} else {
			setChanged();
		}
	}

	/**
	 * The share of this core's heat that cooling towers take this tick: the towers on its condensers' conduits lend
	 * capacity (in standard units of heat), shared with any other reactor on the same towers.
	 */
	private double towerShare(ServerLevel level, ReactorUnit u) {
		double scale = core.design.ratedJe() / (double) ReactorCore.RATED_JE;
		double want = Math.max(0.001, Math.min(1, core.heat())) * scale;
		double got = 0;
		java.util.Set<BlockPos> seen = new HashSet<>();
		// Without circulating water pumps the towers stand idle; each pump moves water for part of the heat.
		java.util.Set<BlockPos> pumps = new HashSet<>();
		for (BlockPos c : u.condensers) {
			if (level.getBlockEntity(c) instanceof EquipmentBlockEntity condenser) pumps.addAll(condenser.cwPumps);
		}
		want = Math.min(want, pumps.size() * PUMP_CAPACITY);
		if (want <= 0) return 0;
		for (BlockPos c : u.condensers) {
			if (!(level.getBlockEntity(c) instanceof EquipmentBlockEntity condenser)) continue;
			for (BlockPos t : condenser.towers) {
				if (got >= want) break;
				if (seen.add(t) && level.getBlockEntity(t) instanceof CoolingTowerBlockEntity tower) {
					got += tower.claim(level.getGameTime(), want - got);
				}
			}
		}
		return Math.min(1, got / (Math.max(0.001, Math.min(1, core.heat())) * scale));
	}

	private int drawWater(ServerLevel level, ReactorUnit u, int need) {
		int got = 0;
		for (BlockPos c : u.condensers) {
			if (got >= need) break;
			if (level.getBlockEntity(c) instanceof EquipmentBlockEntity condenser && condenser.water != null) {
				int take = Math.min(need - got, condenser.water.millibuckets());
				condenser.water.drain(take);
				got += take;
			}
		}
		return got;
	}

	/** An open reactor (refuelling) shines its spent fuel out of the top; a closed one is shielded. */
	private void updateRadiation(ServerLevel level) {
		BlockPos centre = centre();
		if (core.refuelOpen && !core.fuel.isEmpty()) {
			double strength = core.spentCount() * SPENT_RADIOACTIVITY + (core.fuel.size() - core.spentCount()) * 2;
			RadiationSources.set(level, centre, strength, RadiationKind.NUCLEAR);
			sourceSet = true;
		} else if (sourceSet) {
			RadiationSources.clear(level, centre);
			sourceSet = false;
		}
	}

	/** The top middle of the vessel (where the head comes off). */
	public BlockPos centre() {
		EquipmentKind k = kind();
		return getBlockPos().offset(EquipmentKind.toWorld(new BlockPos(k.wide / 2, k.tall - 1, k.deep / 2), facing()));
	}

	private void tickCondenser(ServerLevel level) {
		if (water == null) return;
		EquipmentRegistry registry = EquipmentRegistry.of(level);
		if (intakeVersion != registry.version() || level.getGameTime() % 100 == 0) {
			intakeVersion = registry.version();
			intakeCells = conduitIntakes(level, registry);
		}
		int sources = 0;
		for (BlockPos cell : intakeCells) {
			if (level.getFluidState(cell).is(FluidTags.WATER) && level.getFluidState(cell).isSource()) {
				sources++;
			}
		}
		int supply = Math.min(NuclearPipeBlock.CONDUIT_RATE, sources * NuclearPipeBlock.INTAKE_PER_SOURCE);
		int room = water.capacityMb() - water.millibuckets();
		if (supply > 0 && room > 0) {
			water.fill(Fluids.WATER, Math.min(room, supply));
		}
	}

	/**
	 * The water blocks touching the circulating water conduit that reaches this condenser (and, as it goes, the cooling
	 * tower controllers on the conduit or touching the condenser).
	 */
	private List<BlockPos> conduitIntakes(ServerLevel level, EquipmentRegistry registry) {
		Set<BlockPos> seen = new HashSet<>();
		java.util.ArrayDeque<BlockPos> queue = new java.util.ArrayDeque<>();
		Set<BlockPos> foundTowers = new java.util.LinkedHashSet<>();
		Set<BlockPos> foundPumps = new java.util.LinkedHashSet<>();
		for (BlockPos cell : ReactorUnit.outside(registry, getBlockPos())) {
			if (ReactorUnit.isPipe(level, cell, PipeKind.WATER) && seen.add(cell)) {
				queue.add(cell);
			} else if (level.getBlockState(cell).getBlock() instanceof CoolingTowerBlock) {
				foundTowers.add(cell.immutable());
			}
		}
		Set<BlockPos> water = new HashSet<>();
		while (!queue.isEmpty() && seen.size() <= ReactorUnit.MAX_PIPES) {
			BlockPos pipe = queue.poll();
			for (Direction d : Direction.values()) {
				BlockPos next = pipe.relative(d);
				if (ReactorUnit.isPipe(level, next, PipeKind.WATER)) {
					if (seen.add(next)) queue.add(next);
				} else if (level.getBlockState(next).getBlock() instanceof CoolingTowerBlock) {
					foundTowers.add(next.immutable());
				} else if (registry.anchorOf(next) instanceof BlockPos pump && registry.at(pump) != null
						&& registry.at(pump).kind() == EquipmentKind.CIRCULATING_WATER_PUMP) {
					foundPumps.add(pump);
				} else if (level.getFluidState(next).is(FluidTags.WATER)) {
					water.add(next.immutable());
				}
			}
		}
		towers = List.copyOf(foundTowers);
		cwPumps = List.copyOf(foundPumps);
		return new ArrayList<>(water);
	}

	private void tickTransformer(ServerLevel level) {
		if (output == null || output.getAmount() <= 0) return;
		if (outletCells.isEmpty() || level.getGameTime() % 200 == 0) {
			outletCells = outlets();
		}
		for (BlockPos cell : outletCells) {
			if (output.getAmount() <= 0) break;
			EnergyNetworks.pushToNeighbors(level, cell, output, output.getAmount(), EnumSet.of(Direction.NORTH, Direction.SOUTH, Direction.EAST,
					Direction.WEST, Direction.DOWN));
		}
	}

	/** The bottom layer of the transformer: its cable terminals. */
	private List<BlockPos> outlets() {
		List<BlockPos> out = new ArrayList<>();
		EquipmentKind k = kind();
		for (BlockPos local : k.solidCells()) {
			if (local.getY() == 0) {
				out.add(getBlockPos().offset(EquipmentKind.toWorld(local, facing())));
			}
		}
		return out;
	}

	// ------------------------------------------------------------------ fuel

	/** Loads up to {@code count} fresh assemblies; returns how many went in (none unless open for refuelling). */
	public int loadAssemblies(int count) {
		if (!core.refuelOpen) return 0;
		int n = Math.max(0, Math.min(count, core.design.size() - core.fuel.size()));
		for (int i = 0; i < n; i++) {
			core.fuel.add(0.0);
		}
		if (n > 0) changed();
		return n;
	}

	/**
	 * Takes out up to {@code max} assemblies, the most used first, all of one kind: spent ones (any burnup, or all of
	 * them if the fuel is damaged) before fresh ones. Returns the item stack (empty if none or not open).
	 */
	public net.minecraft.world.item.ItemStack takeAssemblies(int max) {
		if (!core.refuelOpen || core.fuel.isEmpty()) return net.minecraft.world.item.ItemStack.EMPTY;
		core.fuel.sort(java.util.Comparator.reverseOrder());
		boolean damaged = core.fuelDamaged;
		boolean spent = damaged || core.fuel.get(0) >= 0.01;
		int n = 0;
		while (n < max && !core.fuel.isEmpty() && (damaged || (core.fuel.get(0) >= 0.01) == spent)) {
			core.fuel.remove(0);
			n++;
		}
		if (core.fuel.isEmpty()) core.fuelDamaged = false;
		changed();
		net.minecraft.world.item.ItemStack stack = new net.minecraft.world.item.ItemStack(spent ? JugcraftNuclear.SPENT_FUEL : JugcraftNuclear.FUEL_ASSEMBLY, n);
		if (damaged) stack.set(JugcraftNuclear.BURNUP, 2F);
		return stack;
	}

	// ------------------------------------------------------------------ lifecycle and sync

	public void changed() {
		setChanged();
		if (level != null && !level.isClientSide()) {
			level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
		}
	}

	@Override
	public void setLevel(Level level) {
		super.setLevel(level);
		EquipmentRegistry.of(level).add(getBlockPos(), kind(), facing());
	}

	@Override
	public void preRemoveSideEffects(BlockPos pos, BlockState state) {
		super.preRemoveSideEffects(pos, state);
		if (level instanceof ServerLevel server) {
			if (sourceSet) {
				RadiationSources.clear(server, centre());
			}
			EquipmentPlacement.clearFrames(server, pos, kind(state), state.getValue(EquipmentAnchorBlock.FACING));
		}
	}

	@Override
	public void setRemoved() {
		super.setRemoved();
		if (level != null) {
			EquipmentRegistry.of(level).remove(getBlockPos());
		}
	}

	@Override
	public Packet<ClientGamePacketListener> getUpdatePacket() {
		return ClientboundBlockEntityDataPacket.create(this);
	}

	@Override
	public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
		TagValueOutput out = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, registries);
		saveCommon(out);
		if (kind().reactor()) {
			core.saveReadouts(out);
			out.store("missing", Codec.STRING.listOf(), missing);
			out.putInt("loops", loops);
			out.putInt("water", totalWater());
		}
		return out.buildResult();
	}

	/** Cooling water held in the unit's condensers (server side). */
	private int totalWater() {
		int sum = 0;
		if (unit != null && level != null) {
			for (BlockPos c : unit.condensers) {
				if (level.getBlockEntity(c) instanceof EquipmentBlockEntity condenser && condenser.water != null) {
					sum += condenser.water.millibuckets();
				}
			}
		}
		return sum;
	}

	/** Synced: water held in the unit's condensers (mB). */
	public int syncedWater;

	/** An emergency diesel generator: running (started from its panel), and the game time it was started. */
	public boolean running;
	public long runningSince;
	/** Client: the diesel generator's speed (0..1) and its shaft's turn as drawn, and when smoke was last made. */
	public float animSpeed, animAngle;
	public long animNanos, smokeTick;
	public int smokeY = Integer.MIN_VALUE;

	private void saveCommon(ValueOutput out) {
		if (owner != null) out.store("owner", UUIDUtil.CODEC, owner);
		out.putString("mode", mode.name());
		if (linked != null) out.store("linked", BlockPos.CODEC, linked);
		if (running) {
			out.putBoolean("running", true);
			out.putLong("running_since", runningSince);
		}
	}

	@Override
	protected void loadAdditional(ValueInput in) {
		super.loadAdditional(in);
		owner = in.read("owner", UUIDUtil.CODEC).orElse(null);
		try {
			mode = UseMode.valueOf(in.getStringOr("mode", "PERSONAL"));
		} catch (IllegalArgumentException e) {
			mode = UseMode.PERSONAL;
		}
		linked = in.read("linked", BlockPos.CODEC).orElse(null);
		running = in.getBooleanOr("running", false);
		runningSince = in.getLongOr("running_since", 0L);
		if (getBlockState().getBlock() instanceof EquipmentAnchorBlock anchor && anchor.kind.reactor()) {
			core.loadReadouts(in);
			missing = in.read("missing", Codec.STRING.listOf()).orElse(missing);
			loops = in.getIntOr("loops", loops);
			syncedWater = in.getIntOr("water", 0);
		}
		if (water != null) {
			water.variant = net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant.of(Fluids.WATER);
			water.amount = Math.min(water.getCapacity(), in.getLongOr("water_droplets", 0));
			if (water.amount == 0) water.variant = net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant.blank();
		}
		if (output != null) {
			output.setAmount(Math.min(output.getCapacity(), in.getLongOr("energy", 0)));
		}
	}

	@Override
	protected void saveAdditional(ValueOutput out) {
		super.saveAdditional(out);
		saveCommon(out);
		if (kind().reactor()) {
			core.save(out);
		}
		if (water != null) out.putLong("water_droplets", water.amount);
		if (output != null) out.putLong("energy", output.getAmount());
	}

	/** World position of the vessel's middle, for messages and sounds. */
	public Vec3 middle() {
		EquipmentKind k = kind();
		BlockPos local = new BlockPos(k.wide / 2, k.tall / 2, k.deep / 2);
		return Vec3.atCenterOf(getBlockPos().offset(EquipmentKind.toWorld(local, facing())));
	}
}
