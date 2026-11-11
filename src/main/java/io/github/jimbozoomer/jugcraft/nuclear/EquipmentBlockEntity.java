package io.github.jimbozoomer.jugcraft.nuclear;

import io.github.jimbozoomer.jugcraft.chemistry.FluidTank;
import io.github.jimbozoomer.jugcraft.energy.EnergyNetworks;
import io.github.jimbozoomer.jugcraft.energy.EnergyStorage;
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
	private boolean sourceSet, coreSourceSet;
	/**
	 * A core at full power gives off this much (mSv/h a block from its middle) through its vessel: neutrons and gamma
	 * while it fissions, then its fission products' decay gamma, falling away after shutdown. The vessel's steel and
	 * the primary shield round it stop most of it; on the operating floor a counter reads a few mSv/h, outside the
	 * containment nothing.
	 */
	public static final double CORE_RADIATION = 4_000;
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
	/** The circulating water pump houses whose controllers its conduit passes (towers and pump houses build). */
	private List<BlockPos> cwHouses = List.of();

	/** A condenser's circulating water pump houses (their controllers) on its conduit. */
	public List<BlockPos> cwHouses() {
		return cwHouses;
	}
	/** A condenser's circulating water pumps, and its cooling towers (round 34: the plant overview screen shows them). */
	public int cwPumpCount() {
		return cwPumps.size() + (level instanceof ServerLevel server ? WaterWorks.circPumps(server, cwHouses) : 0);
	}

	public List<BlockPos> towerList() {
		return towers;
	}

	/** Each circulating water pump moves the water for half a standard unit's heat. */
	public static final double PUMP_CAPACITY = 0.5;
	/** The pumps' and fans' power, taken from the unit's output while cooling towers carry its heat. */
	public static final double HOUSE_LOAD = 0.03;
	/** Last tick's cooling tower share (for the house load). */
	private double lastTowerShare;
	private int intakeVersion = -1;
	// Main transformer.
	public final @Nullable SimpleEnergyStorage output;
	// The 480 V load centre (nuclear/Switchgear.java) or motor control centre (MotorControlCenter.java): its bus's charge,
	// its breakers (a bit each, closed when set),
	// each breaker's flow last tick (JE/t: the main's what came in, a feeder's what went out), synced for the meters.
	public final @Nullable SimpleEnergyStorage bus;
	public int breakers = Switchgear.DEFAULT_BREAKERS;
	public final int[] flows = new int[Switchgear.BREAKERS];
	long incoming;
	/** A load centre's racking, springs, trip units, amps and transformer (nuclear/LoadCentre.java); null otherwise. */
	public final LoadCentre.@Nullable State lc;
	private int shown;
	private @Nullable EnergyStorage incomingView;
	private final EnergyStorage[] feederViews = new EnergyStorage[Switchgear.BREAKERS];
	private List<BlockPos> outletCells = List.of();
	// Control console (and the remote shutdown panel: the reactor it found).
	public @Nullable BlockPos linked;
	/**
	 * A remote shutdown panel finds the nearest reactor within this many blocks itself (no link card): the plant it
	 * stands in. It copies the reactor's state into its own {@link #core} for its screen and lamps.
	 */
	public static final int PANEL_RANGE = 256;
	private int panelVersion = -1;
	/** A remote shutdown panel: its reactor was there (loaded) this tick (synced). */
	public boolean panelOnline;

	public EquipmentBlockEntity(BlockPos pos, BlockState state) {
		super(JugcraftNuclear.EQUIPMENT_ENTITY, pos, state);
		EquipmentKind kind = kind(state);
		water = kind.cooled()
				? new FluidTank(CONDENSER_TANK, true, v -> v.getFluid() == Fluids.WATER, this::setChanged) : null;
		long room = kind == EquipmentKind.SMALL_TURBINE ? ReactorCore.Design.SMALL_MODULAR.ratedJe() * 2 : ReactorCore.RATED_JE * 2;
		output = kind.powerOut() ? new SimpleEnergyStorage(room, 0, room, this::setChanged) : null;
		bus = kind == EquipmentKind.SWITCHGEAR_LINEUP
				? new SimpleEnergyStorage(Switchgear.CAPACITY, Switchgear.INCOMING_RATE, Switchgear.OVERLOAD_RATE * 10, this::setChanged)
				: kind == EquipmentKind.MCC_LINEUP
				? new SimpleEnergyStorage(MotorControlCenter.CAPACITY, MotorControlCenter.INCOMING_RATE, MotorControlCenter.FEEDER_RATE * 10, this::setChanged)
				: null;
		breakers = defaultBreakers(kind);
		lc = kind == EquipmentKind.SWITCHGEAR_LINEUP ? new LoadCentre.State() : null;
		core.design = kind.design();
	}

	/** A power board's breakers as it is built: a load centre's or a motor control centre's (round 26). */
	static int defaultBreakers(EquipmentKind kind) {
		return kind == EquipmentKind.MCC_LINEUP ? MotorControlCenter.DEFAULT_BREAKERS : Switchgear.DEFAULT_BREAKERS;
	}

	/** A power board's bus capacity (JE): a load centre's or a motor control centre's. */
	public long busCapacity() {
		return kind() == EquipmentKind.MCC_LINEUP ? MotorControlCenter.CAPACITY : Switchgear.CAPACITY;
	}

	/** The board's incoming side as a cable sees it (one object, so a network counts it once). */
	EnergyStorage incomingView() {
		if (incomingView == null) {
			incomingView = kind() == EquipmentKind.MCC_LINEUP
					? new Switchgear.Incoming(this, MotorControlCenter.CAPACITY, MotorControlCenter.INCOMING_RATE)
					: new Switchgear.Incoming(this, Switchgear.CAPACITY, Switchgear.INCOMING_RATE);
		}
		return incomingView;
	}

	/** A board feeder's terminal as a cable sees it. */
	EnergyStorage feederView(int f) {
		if (feederViews[f] == null) {
			feederViews[f] = kind() == EquipmentKind.MCC_LINEUP
					? new Switchgear.Feeder(this, f, MotorControlCenter.CAPACITY, MotorControlCenter.FEEDER_RATE)
					: new Switchgear.Feeder(this, f, Switchgear.CAPACITY, Switchgear.OVERLOAD_RATE);
		}
		return feederViews[f];
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
		if (kind == EquipmentKind.EMERGENCY_DIESEL_GENERATOR) {
			DieselFuel.tick(level, this, level.getGameTime());
			WaterWorks.tickDiesel(level, this, level.getGameTime());
			StartingAir.tick(level, this, level.getGameTime());
			DieselRoom.tick(level, this, level.getGameTime());
			Co2System.tick(level, this, level.getGameTime());
		}
		if (kind.startingAir()) {
			StartingAir.tickPiece(level, this, level.getGameTime());
		}
		if (kind.dieselRoom()) {
			DieselRoom.tickPiece(level, this, level.getGameTime());
		}
		if (kind == EquipmentKind.REMOTE_SHUTDOWN_PANEL) {
			tickShutdownPanel(level);
		}
		if (kind.turbine()) {
			TurbineHall.tick(level, this, level.getGameTime());
		}
		if (kind == EquipmentKind.OVERHEAD_CRANE) {
			HallCrane.tick(level, this, level.getGameTime());
		}
		if (kind.liveBoard()) {
			ControlBoard.tick(level, this);
		}
		if (kind.board()) {
			if (kind == EquipmentKind.MCC_LINEUP) {
				MotorControlCenter.tick(level, this);
			} else {
				Switchgear.tick(level, this);
			}
			// Its meters are synced each second while what they show changes.
			if (++ticks % 20 == 0 && bus != null) {
				int now = java.util.Objects.hash(breakers, bus.getAmount() * 1000 / busCapacity(), java.util.Arrays.hashCode(flows),
						lc == null ? 0 : lc.hash());
				if (now != shown) {
					shown = now;
					changed();
				}
			}
		}
	}

	/**
	 * A remote shutdown panel: keeps to the nearest reactor (looked for again whenever equipment changes, and every
	 * five seconds), and copies its state for the screen, the lamps and the panel's window.
	 */
	private void tickShutdownPanel(ServerLevel level) {
		boolean dirty = false;
		EquipmentRegistry registry = EquipmentRegistry.of(level);
		if (panelVersion != registry.version() || linked == null || level.getGameTime() % 100 == 0) {
			panelVersion = registry.version();
			BlockPos found = nearestReactor(registry);
			// A reactor in an unloaded chunk is not in the registry: keep to it rather than lose it.
			if (found == null && linked != null && !level.isLoaded(linked)) {
				found = linked;
			}
			if (!java.util.Objects.equals(found, linked)) {
				linked = found;
				dirty = true;
			}
		}
		boolean online = linked != null && level.isLoaded(linked) && level.getBlockEntity(linked) instanceof EquipmentBlockEntity vessel
				&& vessel.kind().reactor();
		if (online) {
			core.copyStatus(((EquipmentBlockEntity) level.getBlockEntity(linked)).core);
		}
		if (online != panelOnline) {
			panelOnline = online;
			dirty = true;
		}
		if (dirty || ++ticks % SYNC_INTERVAL == 0) {
			changed();
		}
	}

	/** The reactor nearest this panel within {@link #PANEL_RANGE} blocks, or null. */
	public @Nullable BlockPos nearestReactor(EquipmentRegistry registry) {
		Vec3 here = middle();
		BlockPos best = null;
		double bestDist = (double) PANEL_RANGE * PANEL_RANGE;
		for (java.util.Map.Entry<BlockPos, EquipmentRegistry.Entry> e : registry.anchors().entrySet()) {
			if (!e.getValue().kind().reactor()) {
				continue;
			}
			EquipmentKind k = e.getValue().kind();
			BlockPos middle = e.getKey().offset(EquipmentKind.toWorld(new BlockPos(k.wide / 2, k.tall / 2, k.deep / 2), e.getValue().facing()));
			double d = Vec3.atCenterOf(middle).distanceToSqr(here);
			if (d < bestDist || d == bestDist && best != null && e.getKey().compareTo(best) < 0) {
				bestDist = d;
				best = e.getKey().immutable();
			}
		}
		return best;
	}

	/** A live control board's state (round 27, ControlBoard): its snapshot of the plant, its windows, its recorders. */
	private ControlBoard.@Nullable State board;
	/** A reactor's electrical plant as its vessel last found it (PlantPower; server, not saved). */
	private PlantPower.@Nullable State plantPower;

	public PlantPower.State power() {
		if (plantPower == null) {
			plantPower = new PlantPower.State();
		}
		return plantPower;
	}

	public ControlBoard.State board() {
		if (board == null) {
			board = new ControlBoard.State();
		}
		return board;
	}

	/** A remote shutdown panel: finds its reactor now (used when it is used before it has ticked). */
	public void linkShutdownPanel(ServerLevel level) {
		panelVersion = -1;
		tickShutdownPanel(level);
	}

	private void tickVessel(ServerLevel level) {
		EquipmentRegistry registry = EquipmentRegistry.of(level);
		// (a transformer yard is found through its site, which comes and goes with its chunk: looked for again while missing)
		if (unit == null || unitVersion != registry.version() || unit.transformers.isEmpty() && level.getGameTime() % 100 == 0) {
			unit = ReactorUnit.find(level, getBlockPos());
			unitVersion = registry.version();
			missing = unit.missing();
			loops = unit.loops();
		}
		ReactorUnit u = unit;
		double towers = towerShare(level, u);
		lastTowerShare = towers;
		ControlBoard.synchronize(core, u.steamToPower(), level.getGameTime());
		// Diesels make real power: the grid, the plant's buses and the safety buses, the diesels starting on them.
		PlantPower.tick(level, this, u);
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
				} else if (level.getBlockEntity(t) instanceof YardBlockEntity yard) {
					// The transformer yard (YardBlockEntity): its main transformers out to the switchyard, its unit
					// auxiliary transformers to the plant's own load centres.
					electric -= yard.take(electric);
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
		// Without circulating water pumps the towers stand idle; each pump moves water for part of the heat (a circ pump
		// house's pumps delivering count as many).
		java.util.Set<BlockPos> pumps = new HashSet<>();
		java.util.Set<BlockPos> houses = new HashSet<>();
		for (BlockPos c : u.condensers) {
			if (level.getBlockEntity(c) instanceof EquipmentBlockEntity condenser) {
				pumps.addAll(condenser.cwPumps);
				houses.addAll(condenser.cwHouses);
			}
		}
		want = Math.min(want, (pumps.size() + WaterWorks.circPumps(level, new ArrayList<>(houses))) * PUMP_CAPACITY);
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

	/** An open reactor (refuelling) shines its spent fuel out of the top; a working core shines through its vessel. */
	private void updateRadiation(ServerLevel level) {
		double glow = Math.max(0, core.fission) + Math.max(0, core.decay);
		if (!core.fuel.isEmpty() && glow > 0.001) {
			RadiationSources.set(level, coreMiddle(), CORE_RADIATION * glow, RadiationKind.NUCLEAR);
			coreSourceSet = true;
		} else if (coreSourceSet) {
			RadiationSources.clear(level, coreMiddle());
			coreSourceSet = false;
		}
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

	/**
	 * A hidden vessel's core and head where its building shows them (its site's controller sets them: nuclear/
	 * ReactorBuildingBlockEntity), for its radiation; null: at the vessel's own body.
	 */
	public @Nullable BlockPos shownCore, shownTop;

	/** Where a hidden vessel's building shows its core and head (its radiation comes from there from now on). */
	public void showAt(ServerLevel level, BlockPos core, BlockPos top) {
		if (core.equals(shownCore) && top.equals(shownTop)) {
			return;
		}
		if (coreSourceSet) {
			RadiationSources.clear(level, coreMiddle());
			coreSourceSet = false;
		}
		if (sourceSet) {
			RadiationSources.clear(level, centre());
			sourceSet = false;
		}
		shownCore = core.immutable();
		shownTop = top.immutable();
	}

	/** The middle of the vessel (the core). */
	public BlockPos coreMiddle() {
		if (shownCore != null) return shownCore;
		EquipmentKind k = kind();
		return getBlockPos().offset(EquipmentKind.toWorld(new BlockPos(k.wide / 2, k.tall / 2, k.deep / 2), facing()));
	}

	/** The top middle of the vessel (where the head comes off). */
	public BlockPos centre() {
		if (shownTop != null) return shownTop;
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
		// The medium plant's towers (towers and pump houses build): the circulating water comes from their basins, the
		// make-up the lake intake keeps them topped up with; a basin above low-low gives the conduit its water.
		for (BlockPos t : towers) {
			if (supply >= NuclearPipeBlock.CONDUIT_RATE) break;
			if (level.isLoaded(t) && level.getBlockEntity(t) instanceof CoolingTowerBlockEntity tower && tower.formed()
					&& tower.tower() == CoolingTowerBlock.Tower.MEDIUM && tower.basin() >= CoolingTowerBlockEntity.LOW_LOW) {
				supply = NuclearPipeBlock.CONDUIT_RATE;
			}
		}
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
		Set<BlockPos> foundHouses = new java.util.LinkedHashSet<>();
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
				} else if (level.getBlockState(next).getBlock() instanceof WaterWorksBlock
						&& level.getBlockState(next).getValue(WaterWorksBlock.WORKS) == WaterWorksBlock.Works.CIRC) {
					foundHouses.add(next.immutable());
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
		cwHouses = List.copyOf(foundHouses);
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
		EquipmentRegistry.of(level).add(getBlockPos(), kind(), facing(), EquipmentAnchorBlock.hidden(getBlockState()));
	}

	@Override
	public void preRemoveSideEffects(BlockPos pos, BlockState state) {
		super.preRemoveSideEffects(pos, state);
		if (level instanceof ServerLevel server) {
			if (sourceSet) {
				RadiationSources.clear(server, centre());
			}
			if (coreSourceSet) {
				RadiationSources.clear(server, coreMiddle());
			}
			EquipmentPlacement.clearFrames(server, pos, kind(state), state.getValue(EquipmentAnchorBlock.FACING));
		}
	}

	@Override
	public void setRemoved() {
		super.setRemoved();
		if (level != null) {
			EquipmentRegistry.of(level).remove(getBlockPos());
			// Unloaded (or gone): its radiation goes with it until it ticks again.
			if (level instanceof ServerLevel server && (sourceSet || coreSourceSet)) {
				RadiationSources.clear(server, centre());
				RadiationSources.clear(server, coreMiddle());
				sourceSet = coreSourceSet = false;
			}
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
		if (kind() == EquipmentKind.REMOTE_SHUTDOWN_PANEL) {
			core.saveStatus(out);
			out.putBoolean("online", panelOnline);
		}
		if (kind().liveBoard()) {
			board().save(out);
		}
		if (bus != null) {
			out.putIntArray("flows", flows);
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

	/**
	 * An emergency diesel generator: running (started from its panel), and the game time it was started (or stopped). A
	 * control room HVAC machine (round 26, #11): running as its HVAC panel has it (HvacRoom), and since when.
	 */
	public boolean running;
	public long runningSince;
	/** An emergency diesel generator: game ticks run before this run (its panel's hour meter: 1,000 ticks an hour). */
	public long runTicks;
	/** An emergency diesel generator's fuel (DieselFuel): its day tank (server), fuel burned not yet drawn (droplets),
	 * whether it ran dry or found no fuel to start on, and its day tank's level (0..1, -1 none: synced, for the panels). */
	public @Nullable BlockPos dayTank;
	public double burn;
	public boolean fuelOut;
	public float dayLevel = -1;
	/**
	 * An emergency diesel generator's jacket water over its normal running temperature (degrees C: WaterWorks), climbing
	 * while it runs without its service water train. Synced (its panels' gauges and HIGH JACKET WATER TEMP).
	 */
	public float jwExcess;
	/**
	 * Starting air (round 31, StartingAir): a diesel generator's air in its receivers (psig: synced), how many receivers
	 * it has (synced; none: its air isn't kept), and whether its last start failed for want of air (synced: FAIL TO
	 * START). A receiver's or compressor's pressure shown (psig: synced), a compressor the lead one. Server: what was
	 * last sent, the generator's pieces and when it last looked for them (its registry version then; -1 not yet).
	 */
	public double air = StartingAir.FULL, airSent = StartingAir.FULL;
	public int airReceivers;
	public boolean airFail, airLead;
	public java.util.List<BlockPos> airPieces = java.util.List.of();
	public int airVersion = -1;
	public long airScanned;
	/**
	 * Its room's panels (round 31 part 2, DieselPanels): a diesel generator's MODE ({@link DieselPanels#OFF},
	 * {@link DieselPanels#LOCAL}, {@link DieselPanels#AUTO}), its generator breaker (the game time it closed or is to close,
	 * -1 open), its breaker control switch's last turn (1 TRIP, 2 CLOSE) and when, its 86G lockout tripped, its emergency
	 * stop pushed in, its protection relays' targets (bit i dropped) and its engine panel's LAMP TEST (until). Synced.
	 */
	public int edgMode = DieselPanels.AUTO;
	/**
	 * Its room's own plant (round 31 part 3, DieselRoom): the pieces a diesel generator has found (server) and when it
	 * last looked (its registry version then; -1 not yet). Its CO2 system (Co2System): NORMAL, COUNTING, DISCHARGED or
	 * PURGING and since when (game time: synced), the station pulled and the room found (server).
	 */
	public java.util.List<BlockPos> roomPieces = java.util.List.of();
	public int roomVersion = -1;
	public long roomScanned;
	public int co2Phase = Co2System.NORMAL;
	public long co2At;
	public @Nullable BlockPos co2Station;
	public java.util.@Nullable Set<BlockPos> co2Room;
	/**
	 * A monorail (round 31 part 3, Monorail): its trolley's place along the beam and the hook's drop below its rest
	 * (blocks), each with where it set off from and when (game time). Synced.
	 */
	public double hoistX = 2, hoistXFrom = 2, hoistDrop, hoistDropFrom;
	public long hoistXAt, hoistDropAt;
	/**
	 * The turbine hall's overhead crane (turbine hall part 3, HallCrane): for each of its motions (HallCrane.BRIDGE along
	 * its runway, TROLLEY across the bridge, MAIN and AUX hooks' drop below their rest; blocks) where it set off from, when
	 * (game time), which way (-1, 0, 1), until when it runs (a held pendant button keeps putting that off) and where it
	 * must stop (the runway's end, the trolley's travel, the floor under a hook); whether its power is on (the pendant's
	 * STOP and START); the runway's ends either side of its anchor (blocks, for the client to lay the runway). Synced.
	 */
	public final double[] craneFrom = {0, HallCrane.TROLLEY_HOME, 0, 0}, craneStop = new double[4];
	public final long[] craneAt = new long[4], craneUntil = new long[4];
	public final int[] craneDir = new int[4];
	public boolean cranePower = true;
	public double craneLo, craneHi;
	public long breakerAt = -1, breakerSwAt;
	public int breakerSw, relayTargets;
	public boolean lockout, estop;
	public long lampTestUntil;

	/** Whether the generator breaker is closed at game time {@code now}. */
	public boolean breakerClosed(double now) {
		return running && breakerAt >= 0 && now >= breakerAt;
	}

	/** Seconds since the generator breaker closed (-1 open): the load sequencer's time. */
	public double breakerSeconds(double now) {
		return breakerClosed(now) ? (now - breakerAt) / 20.0 : -1;
	}

	/** Game ticks run in all, at game time {@code now}. */
	public long runTicksAt(long now) {
		return runTicks + (running ? Math.max(0, now - runningSince) : 0);
	}

	/**
	 * Starts or stops an emergency diesel generator (its panel, tests): the hour meter keeps the run's time. Its breaker
	 * closes by itself {@link DieselFuel#BREAKER} seconds after a start (Justin, round 31: a local start's too), and opens
	 * when it stops.
	 */
	public void setDieselRunning(long now, boolean run) {
		if (run != running) {
			breakerAt = run ? now + Math.round(DieselFuel.BREAKER * 20) : -1;
		}
		setRunning(now, run);
	}

	/** Starts or stops a machine that runs (a diesel generator, an HVAC machine), sent to the players when it changes. */
	public void setRunning(long now, boolean run) {
		if (run == running) {
			return;
		}
		if (running) {
			runTicks += Math.max(0, now - runningSince);
		}
		running = run;
		runningSince = now;
		changed();
	}
	/**
	 * The main turbine-generator (turbine hall part 1, TurbineHall): what its front standard, valves and turning gear show
	 * (synced): its speed (rpm), load (fraction of rated), {@link TurbineHall} flags, and until when its trip handle's guard
	 * is up (game time). Server: the registry version its reactor was found at. A piece of the hall's feed train or oil
	 * plant (part 2) shows its unit's plant in the same fields: the turbine's speed (a main feed pump: its own), the
	 * unit's load, the turbine's flags and its own (running, standing by, tripped...).
	 */
	public float turbineRpm, turbineLoad;
	public int turbineFlags;
	public long tripGuardUntil;
	public int reactorVersion = -1;
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
		if (runTicks > 0) {
			out.putLong("run_ticks", runTicks);
		}
		if (kind() == EquipmentKind.EMERGENCY_DIESEL_GENERATOR) {
			out.putBoolean("fuel_out", fuelOut);
			out.putFloat("day_level", dayLevel);
			out.putFloat("jw_excess", jwExcess);
			out.putDouble("air", air);
			out.putInt("air_receivers", airReceivers);
			out.putBoolean("air_fail", airFail);
			out.putInt("edg_mode", edgMode);
			out.putLong("breaker_at", breakerAt);
			out.putInt("breaker_sw", breakerSw);
			out.putLong("breaker_sw_at", breakerSwAt);
			out.putBoolean("lockout", lockout);
			out.putBoolean("estop", estop);
			out.putInt("relay_targets", relayTargets);
			out.putLong("lamp_test", lampTestUntil);
			out.putInt("co2_phase", co2Phase);
			out.putLong("co2_at", co2At);
		}
		if (kind() == EquipmentKind.TURBINE_GENERATOR || kind().feedTrain()) {
			// (Turbine hall part 2: the feed train's and oil plant's pieces show their unit's plant the same way: TurbineHall.)
			out.putFloat("turbine_rpm", turbineRpm);
			out.putFloat("turbine_load", turbineLoad);
			out.putInt("turbine_flags", turbineFlags);
			out.putLong("trip_guard", tripGuardUntil);
		}
		if (kind() == EquipmentKind.EDG_MONORAIL) {
			out.putDouble("hoist_x", hoistX);
			out.putDouble("hoist_x_from", hoistXFrom);
			out.putLong("hoist_x_at", hoistXAt);
			out.putDouble("hoist_drop", hoistDrop);
			out.putDouble("hoist_drop_from", hoistDropFrom);
			out.putLong("hoist_drop_at", hoistDropAt);
		}
		if (kind() == EquipmentKind.OVERHEAD_CRANE) {
			for (int k = 0; k < 4; k++) {
				out.putDouble("crane_from" + k, craneFrom[k]);
				out.putDouble("crane_stop" + k, craneStop[k]);
				out.putLong("crane_at" + k, craneAt[k]);
				out.putLong("crane_until" + k, craneUntil[k]);
				out.putInt("crane_dir" + k, craneDir[k]);
			}
			out.putBoolean("crane_power", cranePower);
			out.putDouble("crane_lo", craneLo);
			out.putDouble("crane_hi", craneHi);
		}
		if (kind().startingAir()) {
			out.putDouble("air", air);
		}
		if (bus != null) {
			out.putLong("bus", bus.getAmount());
			out.putInt("breakers", breakers);
		}
		if (lc != null) {
			lc.save(out);
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
		runTicks = in.getLongOr("run_ticks", 0L);
		fuelOut = in.getBooleanOr("fuel_out", false);
		dayLevel = in.getFloatOr("day_level", -1F);
		jwExcess = in.getFloatOr("jw_excess", 0F);
		boolean edg = getBlockState().getBlock() instanceof EquipmentAnchorBlock a && a.kind == EquipmentKind.EMERGENCY_DIESEL_GENERATOR;
		air = in.getDoubleOr("air", edg ? StartingAir.FULL : 0);
		airSent = air;
		airReceivers = in.getIntOr("air_receivers", 0);
		airFail = in.getBooleanOr("air_fail", false);
		edgMode = in.getIntOr("edg_mode", DieselPanels.AUTO);
		// (A generator saved running before round 31's breaker took its breaker as having closed five seconds in.)
		breakerAt = in.getLongOr("breaker_at", running ? runningSince + Math.round(DieselFuel.BREAKER * 20) : -1);
		breakerSw = in.getIntOr("breaker_sw", 0);
		breakerSwAt = in.getLongOr("breaker_sw_at", 0L);
		lockout = in.getBooleanOr("lockout", false);
		estop = in.getBooleanOr("estop", false);
		relayTargets = in.getIntOr("relay_targets", 0);
		lampTestUntil = in.getLongOr("lamp_test", 0L);
		co2Phase = in.getIntOr("co2_phase", Co2System.NORMAL);
		co2At = in.getLongOr("co2_at", 0L);
		turbineRpm = in.getFloatOr("turbine_rpm", 0F);
		turbineLoad = in.getFloatOr("turbine_load", 0F);
		turbineFlags = in.getIntOr("turbine_flags", 0);
		tripGuardUntil = in.getLongOr("trip_guard", 0L);
		hoistX = in.getDoubleOr("hoist_x", 2);
		hoistXFrom = in.getDoubleOr("hoist_x_from", hoistX);
		hoistXAt = in.getLongOr("hoist_x_at", 0L);
		hoistDrop = in.getDoubleOr("hoist_drop", 0);
		hoistDropFrom = in.getDoubleOr("hoist_drop_from", hoistDrop);
		hoistDropAt = in.getLongOr("hoist_drop_at", 0L);
		for (int k = 0; k < 4; k++) {
			craneFrom[k] = in.getDoubleOr("crane_from" + k, k == HallCrane.TROLLEY ? HallCrane.TROLLEY_HOME : 0);
			craneStop[k] = in.getDoubleOr("crane_stop" + k, craneFrom[k]);
			craneAt[k] = in.getLongOr("crane_at" + k, 0L);
			craneUntil[k] = in.getLongOr("crane_until" + k, 0L);
			craneDir[k] = in.getIntOr("crane_dir" + k, 0);
		}
		cranePower = in.getBooleanOr("crane_power", true);
		craneLo = in.getDoubleOr("crane_lo", 0);
		craneHi = in.getDoubleOr("crane_hi", 0);
		if (getBlockState().getBlock() instanceof EquipmentAnchorBlock anchor && anchor.kind.reactor()) {
			core.loadReadouts(in);
			missing = in.read("missing", Codec.STRING.listOf()).orElse(missing);
			loops = in.getIntOr("loops", loops);
			syncedWater = in.getIntOr("water", 0);
		}
		if (getBlockState().getBlock() instanceof EquipmentAnchorBlock anchor && anchor.kind == EquipmentKind.REMOTE_SHUTDOWN_PANEL) {
			core.loadStatus(in);
			panelOnline = in.getBooleanOr("online", false);
		}
		if (getBlockState().getBlock() instanceof EquipmentAnchorBlock anchor && anchor.kind.liveBoard() && in.child("board").isPresent()) {
			// Sent to the players (the server keeps its own: it is worked out again from the plant).
			ControlBoard.State loaded = ControlBoard.State.load(in);
			if (board != null) {
				loaded.copyLocal(board);
			}
			board = loaded;
		}
		if (water != null) {
			water.variant = net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant.of(Fluids.WATER);
			water.amount = Math.min(water.getCapacity(), in.getLongOr("water_droplets", 0));
			if (water.amount == 0) water.variant = net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant.blank();
		}
		if (output != null) {
			output.setAmount(Math.min(output.getCapacity(), in.getLongOr("energy", 0)));
		}
		if (bus != null) {
			bus.setAmount(in.getLongOr("bus", 0));
			breakers = in.getIntOr("breakers", defaultBreakers(kind()));
			in.getIntArray("flows").ifPresent(f -> System.arraycopy(f, 0, flows, 0, Math.min(f.length, flows.length)));
		}
		if (lc != null) {
			lc.load(in);
		}
	}

	@Override
	protected void saveAdditional(ValueOutput out) {
		super.saveAdditional(out);
		saveCommon(out);
		if (kind().reactor()) {
			core.save(out);
		}
		if (kind().liveBoard() && board != null) {
			// Its windows kept over a restart (an alarm already acknowledged does not sound again).
			board.save(out);
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
