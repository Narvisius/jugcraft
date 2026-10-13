package io.github.jimbozoomer.jugcraft.machine.form;

import io.github.jimbozoomer.jugcraft.chemistry.FluidMachineSpec;
import io.github.jimbozoomer.jugcraft.chemistry.FluidRecipe;
import io.github.jimbozoomer.jugcraft.chemistry.FluidRecipes;
import io.github.jimbozoomer.jugcraft.chemistry.FluidTank;
import io.github.jimbozoomer.jugcraft.chemistry.FluidTanks;
import io.github.jimbozoomer.jugcraft.energy.EnergyNetworks;
import io.github.jimbozoomer.jugcraft.energy.EnergyStorage;
import io.github.jimbozoomer.jugcraft.energy.SimpleEnergyStorage;
import io.github.jimbozoomer.jugcraft.fluid.FluidNetworks;
import io.github.jimbozoomer.jugcraft.logistics.ItemNetworks;
import io.github.jimbozoomer.jugcraft.machine.MachineBlock;
import io.github.jimbozoomer.jugcraft.machine.MachineUpgrades;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import net.fabricmc.fabric.api.menu.v1.ExtendedMenuProvider;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.item.ContainerStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.base.CombinedStorage;
import net.fabricmc.fabric.api.transfer.v1.storage.base.FilteringStorage;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.Containers;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

/**
 * The controller of a formed industrial machine (see {@link MachineForm}). It alone holds the machine's energy, tanks,
 * slots and running batches, and it alone works; every other part routes interaction here.
 *
 * <p>Work is atomic and durable: a batch starts only when its inputs, its tool and room for every result are all
 * there; its inputs then move into a {@link WorkLane} (escrow) and its results are reserved, so another lane or an
 * outside transfer can neither borrow the spent inputs nor fill the reserved room. Paid ticks advance it; finishing
 * moves the results out in one step. A restart, unload or recipe reload continues the same batch from its saved
 * progress. Cancelling returns only the untransformed inputs, never the energy spent. Breaking the machine drops its
 * slots and the escrowed inputs once, and the reserved results are never made.
 *
 * <p>A generator form ({@link MachineForm#generator()}) runs no batches: each tick it burns fuel from its one tank, a
 * millibucket at a time, into JE in hand, makes as much of it into power as its store has room for (up to the fuel's
 * rate), and pushes its power out of its power ports. Energy not made yet stays in hand, so nothing is lost or made
 * twice.
 *
 * <p>The block entity is deliberately not itself a container: its slots are reachable only through its screen and its
 * ports, so no generic container lookup can reach a tool socket or an output through a closed face.
 */
public class FormMachineBlockEntity extends BlockEntity implements ExtendedMenuProvider<Identifier> {
	/**
	 * Ticks between periodic structure checks, besides the check a part asks for when a neighbour changes; one check
	 * examines at most {@link MachineForm#MAX_POSITIONS} positions.
	 */
	public static final int CHECK_INTERVAL = 40;
	/** Ticks between pushes out of the output ports, and the most each port pushes at a time. */
	public static final int PUSH_INTERVAL = 4;
	public static final int PUSH_MB = 1_000;
	public static final int PUSH_ITEMS = 16;
	/** The most lanes a profile can have (the bulk profile's four). */
	public static final int MAX_LANES = 4;
	/**
	 * Ticks the machine stays lit after it last worked, so a moment's pause between batches does not relight every
	 * part.
	 */
	public static final int LIT_HOLD = 20;

	// The menu's synced data. Values are sent as 16 bits, so energy and capacity are split in halves.
	public static final int DATA_ENERGY_LOW = 0;
	public static final int DATA_ENERGY_HIGH = 1;
	public static final int DATA_CAPACITY_LOW = 2;
	public static final int DATA_CAPACITY_HIGH = 3;
	/** Five values: see {@link MachineStatus#data}. */
	public static final int DATA_STATUS = 4;
	public static final int DATA_PAUSED = DATA_STATUS + MachineStatus.DATA_VALUES;
	/** JE per tick one running lane pays now; for a generator, the JE it made on its last tick. */
	public static final int DATA_LANE_USE = DATA_PAUSED + 1;
	/** One bit per tool socket whose tool a running batch needs (it cannot be taken out). */
	public static final int DATA_LOCKS = DATA_LANE_USE + 1;
	/** Two values per lane: paid ticks and the ticks the batch needs (0 when the lane is free). */
	public static final int DATA_LANES = DATA_LOCKS + 1;
	/** Two values per tank: the fluid's registry id and millibuckets. */
	public static final int DATA_TANKS = DATA_LANES + 2 * MAX_LANES;
	/** One value per tank: millibuckets reserved for running batches. */
	public static final int DATA_RESERVED = DATA_TANKS + 2 * FluidMachineSpec.MAX_TANKS;
	public static final int DATA_COUNT = DATA_RESERVED + FluidMachineSpec.MAX_TANKS;

	/** Menu buttons. */
	public static final int BUTTON_PAUSE = 0;
	public static final int BUTTON_CANCEL = 1;

	private final MachineForm form;
	private final SimpleEnergyStorage energy;
	private final SimpleContainer slots;
	/** The slots' contents (the container's own list). */
	private final NonNullList<ItemStack> items;
	private final FluidTanks tanks;
	private final @Nullable WorkLane[] lanes;
	private MachineStatus status = MachineStatus.IDLE;
	/** Null while formed; otherwise what is wrong with the structure. */
	private @Nullable MachineStatus structure;
	private boolean paused;
	private long nextCheck;
	/** The game time of the last paid tick: a batch started long after it starts cold. Not saved. */
	private long lastWorked = Long.MIN_VALUE / 2;
	/** The machine shows lit until this game time (see {@link #LIT_HOLD}). Not saved. */
	private long litUntil = Long.MIN_VALUE / 2;
	/** A generator's JE in hand: fuel already burnt and not yet made into power. */
	private long burn;
	/** JE a tick the fuel in hand makes (its {@link FormFuel#jePerTick}). */
	private int burnRate;
	/** JE a generator made on its last tick. Not saved. */
	private int made;
	private long lanePrice;
	/** The family's recipe list this machine filtered last, and the result (rebuilt after a reload). */
	private @Nullable List<FluidRecipes.Entry> recipeSource;
	private List<FluidRecipes.Entry> recipes = List.of();

	private final ContainerData data = new ContainerData() {
		@Override
		public int get(int index) {
			if (index >= DATA_STATUS && index < DATA_STATUS + MachineStatus.DATA_VALUES) {
				return status.data(index - DATA_STATUS);
			}
			if (index >= DATA_LANES && index < DATA_TANKS) {
				int lane = (index - DATA_LANES) / 2;
				WorkLane work = lane < lanes.length ? lanes[lane] : null;
				if (work == null) {
					return 0;
				}
				return (index - DATA_LANES) % 2 == 0 ? work.progress() : required(work);
			}
			if (index >= DATA_TANKS && index < DATA_RESERVED) {
				return index - DATA_TANKS < 2 * tanks.size() ? tanks.data(index - DATA_TANKS) : 0;
			}
			if (index >= DATA_RESERVED && index < DATA_COUNT) {
				int tank = index - DATA_RESERVED;
				int inputs = form.tanks().inputTanks().size();
				return tank >= inputs && tank < tanks.size() ? reservedMb(tank - inputs) : 0;
			}
			return switch (index) {
				case DATA_ENERGY_LOW -> (int) (energy.getAmount() & 0xFFFF);
				case DATA_ENERGY_HIGH -> (int) ((energy.getAmount() >>> 16) & 0xFFFF);
				case DATA_CAPACITY_LOW -> (int) (energy.getCapacity() & 0xFFFF);
				case DATA_CAPACITY_HIGH -> (int) ((energy.getCapacity() >>> 16) & 0xFFFF);
				case DATA_PAUSED -> paused ? 1 : 0;
				case DATA_LANE_USE -> (int) Math.min(0xFFFF, form.generator() ? made : lanePrice);
				case DATA_LOCKS -> locks();
				default -> 0;
			};
		}

		@Override
		public void set(int index, int value) {
			// Server-authoritative: clients never write machine state.
		}

		@Override
		public int getCount() {
			return DATA_COUNT;
		}
	};

	public FormMachineBlockEntity(BlockPos pos, BlockState state) {
		super(((FormMachineBlock) state.getBlock()).entityType(), pos, state);
		this.form = ((FormMachineBlock) state.getBlock()).form();
		// A generator only gives power out; every other form only takes it in.
		this.energy = form.generator()
				? new SimpleEnergyStorage(form.family().capacity, 0, form.family().maxOutput, this::setChanged)
				: new SimpleEnergyStorage(form.family().capacity, form.family().maxInput, 0, this::setChanged);
		this.slots = new SimpleContainer(form.containerSize()) {
			@Override
			public boolean canPlaceItem(int slot, ItemStack stack) {
				return FormMachineBlockEntity.this.canPlaceItem(slot, stack);
			}

			@Override
			public boolean stillValid(Player player) {
				return Container.stillValidBlockEntity(FormMachineBlockEntity.this, player);
			}

			@Override
			public void setChanged() {
				super.setChanged();
				FormMachineBlockEntity.this.setChanged();
			}
		};
		this.items = slots.getItems();
		this.tanks = new FluidTanks(form.tanks(), (tank, variant) -> form.generator() ? form.fuel(variant.getFluid()) != null
				: level instanceof ServerLevel server && usesFluid(recipes(server.getServer()), tank, variant), this::setChanged);
		this.lanes = new WorkLane[form.profile().lanes()];
	}

	public MachineForm form() {
		return form;
	}

	public FluidTanks tanks() {
		return tanks;
	}

	/** The machine's slots: inputs, outputs, tool sockets, then upgrades (see {@link MachineForm}). */
	public SimpleContainer container() {
		return slots;
	}

	public EnergyStorage energy() {
		return energy;
	}

	public MachineStatus status() {
		return status;
	}

	public boolean paused() {
		return paused;
	}

	/** The running batch in lane {@code lane}, or null. */
	public @Nullable WorkLane lane(int lane) {
		return lanes[lane];
	}

	/**
	 * The recipes this form runs: its family's recipes whose capability it declares, in id order (so which of two
	 * complete recipes starts never depends on load order).
	 */
	public List<FluidRecipes.Entry> recipes(MinecraftServer server) {
		List<FluidRecipes.Entry> all = FluidRecipes.entries(server, form.family());
		if (all != recipeSource) {
			List<FluidRecipes.Entry> runs = new ArrayList<>();
			for (FluidRecipes.Entry entry : all) {
				if (form.runs(entry.recipe()) && (entry.recipe().tool().isEmpty() || !form.sockets().isEmpty())) {
					runs.add(entry);
				}
			}
			runs.sort(java.util.Comparator.comparing(entry -> entry.id().toString()));
			recipes = List.copyOf(runs);
			recipeSource = all;
		}
		return recipes;
	}

	// ------------------------------------------------------------------ ticking

	public void serverTick(ServerLevel level, BlockPos pos, BlockState state) {
		long now = level.getGameTime();
		if (now >= nextCheck) {
			structure = checkStructure(level, pos, state);
			nextCheck = now + CHECK_INTERVAL;
		}
		MachineStatus next;
		made = 0;
		if (structure != null) {
			next = structure;
		} else if (form.generator()) {
			next = paused ? MachineStatus.PAUSED : generate();
			pushPower(level, pos, state);
		} else {
			if (now % PUSH_INTERVAL == 0) {
				push(level, pos, state);
			}
			next = paused ? MachineStatus.PAUSED : work(level.getServer(), now);
		}
		if (!next.equals(status)) {
			status = next;
			setChanged();
		}
		if (status.state().working()) {
			litUntil = now + LIT_HOLD;
		}
		boolean lit = now < litUntil;
		if (state.getValue(MachineBlock.LIT) != lit) {
			light(level, pos, state, lit);
		}
	}

	/**
	 * Lights or darkens every part, so lamps and strips anywhere on the machine show it working. Only the controller's
	 * change reaches its neighbours (and its light); the other parts just redraw.
	 */
	private void light(ServerLevel level, BlockPos pos, BlockState state, boolean lit) {
		Direction facing = state.getValue(MachineBlock.FACING);
		FormMachineBlock block = (FormMachineBlock) state.getBlock();
		for (int part = 1; part < form.footprint().size(); part++) {
			BlockPos at = form.footprint().partPos(pos, facing, part);
			if (!level.isLoaded(at)) {
				continue;
			}
			BlockState there = level.getBlockState(at);
			if (there.is(block) && there.getValue(MachineBlock.FACING) == facing && block.part(there) == part
					&& there.getValue(MachineBlock.LIT) != lit) {
				level.setBlock(at, there.setValue(MachineBlock.LIT, lit), Block.UPDATE_CLIENTS);
			}
		}
		level.setBlock(pos, state.setValue(MachineBlock.LIT, lit), Block.UPDATE_ALL);
	}

	/** Checks the structure on the next tick (a part's neighbour changed). */
	public void requestCheck() {
		nextCheck = 0;
	}

	/**
	 * Every part in place and every clearance free, or what is wrong. Examines each part and clearance position once;
	 * positions in unloaded chunks are taken as they were.
	 */
	public @Nullable MachineStatus checkStructure(ServerLevel level, BlockPos pos, BlockState state) {
		Direction facing = state.getValue(MachineBlock.FACING);
		FormMachineBlock block = (FormMachineBlock) state.getBlock();
		for (int part = 1; part < form.footprint().size(); part++) {
			BlockPos at = form.footprint().partPos(pos, facing, part);
			if (!level.isLoaded(at)) {
				continue;
			}
			BlockState there = level.getBlockState(at);
			if (!there.is(block) || there.getValue(MachineBlock.FACING) != facing || block.part(there) != part) {
				return MachineStatus.at(MachineLifecycle.UNFORMED, MachineStatus.Reason.PART_MISSING, form.partCell(part));
			}
		}
		for (int cell : form.clearanceCells()) {
			BlockPos at = form.cellPos(pos, facing, cell);
			if (level.isLoaded(at) && !level.getBlockState(at).canBeReplaced()) {
				return MachineStatus.at(MachineLifecycle.UNFORMED, MachineStatus.Reason.CLEARANCE_BLOCKED, cell);
			}
		}
		return null;
	}

	/** Starts what batches it can, pays and advances the running ones, and says what the machine is doing. */
	private MachineStatus work(MinecraftServer server, long now) {
		List<FluidRecipes.Entry> runs = recipes(server);
		MachineStatus blocked = null;
		for (int lane = 0; lane < lanes.length; lane++) {
			if (lanes[lane] == null) {
				Start start = start(runs, now);
				if (start.lane() == null) {
					blocked = start.status();
					break;
				}
				lanes[lane] = start.lane();
				setChanged();
			}
		}

		MachineUpgrades.Effect upgrades = upgrades();
		lanePrice = form.profile().lanePerTick(upgrades.use(form.family().usePerTick));
		WorkLane firstPaid = null;
		boolean starved = false;
		MachineStatus stuck = null;
		for (int index = 0; index < lanes.length; index++) {
			WorkLane lane = lanes[index];
			if (lane == null) {
				continue;
			}
			int required = upgrades.ticks(lane.duration());
			if (lane.progress() < required) {
				if (lane.tool() >= 0 && !form.sockets().get(lane.tool()).accepts(items.get(form.firstSocketSlot() + lane.tool()))) {
					stuck = MachineStatus.at(MachineLifecycle.WAITING_TOOL, MachineStatus.Reason.TOOL_REQUIRED, lane.tool());
					continue;
				}
				if (energy.getAmount() < lanePrice) {
					starved = true;
					continue;
				}
				energy.setAmount(energy.getAmount() - lanePrice);
				lane.advance(1);
				lastWorked = now;
				if (firstPaid == null) {
					firstPaid = lane;
				}
				setChanged();
			}
			if (lane.progress() >= required) {
				MachineStatus failure = finish(lane);
				if (failure == null) {
					lanes[index] = null;
				} else {
					stuck = failure;
				}
			}
		}

		if (stuck != null) {
			return stuck;
		}
		if (firstPaid != null) {
			return firstPaid.cold() && firstPaid.progress() <= form.warmupTicks() ? MachineStatus.WARMING : MachineStatus.PROCESSING;
		}
		if (starved) {
			return new MachineStatus(MachineLifecycle.WAITING_ENERGY, MachineStatus.Reason.NO_ENERGY, -1, 0,
					(int) Math.min(0xFFFF, lanePrice));
		}
		return blocked != null ? blocked : MachineStatus.IDLE;
	}

	/**
	 * A generator's tick: makes as much power as its store has room for, up to the rate of the fuel it burns, draining
	 * fuel a millibucket at a time as the JE in hand runs short. What is not made yet stays in hand for the next tick.
	 */
	private MachineStatus generate() {
		FluidTank tank = tanks.input(0);
		FormFuel fuel = tank.amount > 0 ? form.fuel(tank.variant.getFluid()) : null;
		if (tank.amount > 0 && fuel == null) {
			// Fuel taken off the form's list since it was saved: it stays in the tank, and nothing burns it.
			return MachineStatus.fluid(MachineLifecycle.WAITING_INPUT, MachineStatus.Reason.UNUSED_FLUID, 0, tank.variant.getFluid(),
					tank.millibuckets());
		}
		if (fuel == null && burn <= 0) {
			return MachineStatus.at(MachineLifecycle.IDLE, MachineStatus.Reason.NO_FUEL, 0);
		}
		long room = energy.getCapacity() - energy.getAmount();
		if (room <= 0) {
			return MachineStatus.of(MachineLifecycle.IDLE, MachineStatus.Reason.POWER_FULL);
		}
		int rate = fuel != null ? fuel.jePerTick() : burnRate > 0 ? burnRate : form.fuels().getFirst().jePerTick();
		long want = Math.min(rate, room);
		while (burn < want && fuel != null && tank.millibuckets() > 0) {
			tank.drain(1);
			burn += fuel.jePerMb();
			burnRate = fuel.jePerTick();
		}
		long make = Math.min(want, burn);
		if (make <= 0) {
			return MachineStatus.at(MachineLifecycle.IDLE, MachineStatus.Reason.NO_FUEL, 0);
		}
		burn -= make;
		energy.setAmount(energy.getAmount() + make);
		made = (int) make;
		setChanged();
		return MachineStatus.PROCESSING;
	}

	/** A generator's JE in hand: fuel burnt and not yet made into power. */
	public long burn() {
		return burn;
	}

	/** Pushes a generator's power out of its power ports into cables and machines, at most its family's output a tick. */
	private void pushPower(ServerLevel level, BlockPos pos, BlockState state) {
		Direction facing = state.getValue(MachineBlock.FACING);
		long budget = form.family().maxOutput;
		for (FormPort port : form.ports()) {
			if (port.kind() != FormPort.Kind.ENERGY_OUT || budget <= 0 || energy.getAmount() <= 0) {
				continue;
			}
			BlockPos at = form.cellPos(pos, facing, form.cellIndex(port.column(), port.row(), port.layer()));
			budget -= EnergyNetworks.pushToNeighbors(level, at, energy, budget, List.of(port.side().world(facing)));
		}
	}

	/** The ticks a batch needs with the upgrade cards now fitted. */
	private int required(WorkLane lane) {
		return upgrades().ticks(lane.duration());
	}

	public MachineUpgrades.Effect upgrades() {
		if (form.upgradeSlots() == 0) {
			return MachineUpgrades.Effect.NONE;
		}
		return MachineUpgrades.effect(items.subList(form.firstUpgradeSlot(), form.containerSize()));
	}

	private record Start(@Nullable WorkLane lane, MachineStatus status) {
	}

	/**
	 * Starts the first recipe whose inputs, tool and output room are all there; or says the first thing stopping one,
	 * preferring a recipe that has all its inputs (it then needs only a tool or room) over a list of missing inputs.
	 */
	private Start start(List<FluidRecipes.Entry> runs, long now) {
		List<ItemStack> inputs = items.subList(0, form.itemInputs());
		if (inputsEmpty()) {
			return new Start(null, MachineStatus.IDLE);
		}
		MachineStatus first = null;
		for (FluidRecipes.Entry entry : runs) {
			FluidRecipe recipe = entry.recipe();
			if (!recipe.itemsMatch(inputs) || !recipe.fluidsMatch(tanks)) {
				continue;
			}
			int tool = -1;
			if (recipe.tool().isPresent()) {
				tool = socketHolding(recipe.tool().get());
				if (tool < 0) {
					if (first == null) {
						first = toolStatus(recipe.tool().get());
					}
					continue;
				}
			}
			MachineStatus room = room(recipe);
			if (room != null) {
				if (first == null) {
					first = room;
				}
				continue;
			}
			return new Start(begin(entry.id(), recipe, tool, now), MachineStatus.PROCESSING);
		}
		return new Start(null, first != null ? first : missing(runs, inputs));
	}

	private boolean inputsEmpty() {
		for (int slot = 0; slot < form.itemInputs(); slot++) {
			if (!items.get(slot).isEmpty()) {
				return false;
			}
		}
		for (int tank = 0; tank < form.tanks().inputTanks().size(); tank++) {
			if (tanks.input(tank).amount > 0) {
				return false;
			}
		}
		return true;
	}

	/** The first socket holding a tool {@code tool} accepts, or -1. */
	private int socketHolding(Ingredient tool) {
		for (int socket = 0; socket < form.sockets().size(); socket++) {
			ItemStack held = items.get(form.firstSocketSlot() + socket);
			if (!held.isEmpty() && tool.test(held)) {
				return socket;
			}
		}
		return -1;
	}

	/** Which socket a missing tool goes in (the first that would take it) and what it is. */
	private MachineStatus toolStatus(Ingredient tool) {
		Optional<Holder<Item>> example = tool.items().findFirst();
		int socket = 0;
		if (example.isPresent()) {
			ItemStack stack = new ItemStack(example.get());
			for (int index = 0; index < form.sockets().size(); index++) {
				if (form.sockets().get(index).accepts(stack)) {
					socket = index;
					break;
				}
			}
			return MachineStatus.item(MachineLifecycle.WAITING_TOOL, MachineStatus.Reason.TOOL_REQUIRED, socket, example.get().value(), 1);
		}
		return MachineStatus.at(MachineLifecycle.WAITING_TOOL, MachineStatus.Reason.TOOL_REQUIRED, socket);
	}

	/** Null when every result of {@code recipe} fits beside what is stored and reserved; otherwise the blocked output. */
	private @Nullable MachineStatus room(FluidRecipe recipe) {
		int inputs = form.tanks().inputTanks().size();
		Map<Integer, Integer> fluidNeeds = new HashMap<>();
		for (int i = 0; i < recipe.fluidResults().size(); i++) {
			int out = recipe.resultTank(i);
			FluidRecipe.FluidAmount result = recipe.fluidResults().get(i);
			if (out >= form.tanks().outputTanks().size()) {
				return MachineStatus.at(MachineLifecycle.OUTPUT_BLOCKED, MachineStatus.Reason.OUTPUT_FULL, inputs + out);
			}
			FluidTank tank = tanks.output(out);
			Fluid reserved = reservedFluid(out);
			if (!tank.variant.isBlank() && !tank.variant.isOf(result.fluid())) {
				return MachineStatus.fluid(MachineLifecycle.OUTPUT_BLOCKED, MachineStatus.Reason.OUTPUT_OTHER_FLUID, inputs + out,
						tank.variant.getFluid(), 0);
			}
			if (reserved != null && reserved != result.fluid()) {
				return MachineStatus.fluid(MachineLifecycle.OUTPUT_BLOCKED, MachineStatus.Reason.OUTPUT_OTHER_FLUID, inputs + out, reserved, 0);
			}
			int need = fluidNeeds.merge(out, result.amount(), Integer::sum);
			if (tank.millibuckets() + reservedMb(out) + need > tank.capacityMb()) {
				return MachineStatus.at(MachineLifecycle.OUTPUT_BLOCKED, MachineStatus.Reason.OUTPUT_FULL, inputs + out);
			}
		}
		for (int i = 0; i < recipe.results().size(); i++) {
			int slot = form.firstOutputSlot() + i;
			if (i >= form.itemOutputs()) {
				return MachineStatus.at(MachineLifecycle.OUTPUT_BLOCKED, MachineStatus.Reason.OUTPUT_SLOT_FULL, slot);
			}
			ItemStack result = recipe.results().get(i).create();
			ItemStack held = items.get(slot);
			ItemStack reserved = ItemStack.EMPTY;
			int reservedCount = 0;
			for (WorkLane lane : lanes) {
				if (lane != null) {
					for (WorkLane.ItemPart part : lane.itemResults()) {
						if (part.slot() == slot) {
							reserved = part.stack();
							reservedCount += part.stack().getCount();
						}
					}
				}
			}
			boolean other = !held.isEmpty() && !ItemStack.isSameItemSameComponents(held, result)
					|| !reserved.isEmpty() && !ItemStack.isSameItemSameComponents(reserved, result);
			int limit = Math.min(slots.getMaxStackSize(), result.getMaxStackSize());
			if (other || held.getCount() + reservedCount + result.getCount() > limit) {
				return MachineStatus.at(MachineLifecycle.OUTPUT_BLOCKED, MachineStatus.Reason.OUTPUT_SLOT_FULL, slot);
			}
		}
		return null;
	}

	/** Millibuckets running batches have reserved in output tank {@code out}. */
	public int reservedMb(int out) {
		int total = 0;
		for (WorkLane lane : lanes) {
			if (lane != null) {
				for (WorkLane.FluidPart part : lane.fluidResults()) {
					if (part.tank() == out) {
						total += part.mb();
					}
				}
			}
		}
		return total;
	}

	private @Nullable Fluid reservedFluid(int out) {
		for (WorkLane lane : lanes) {
			if (lane != null) {
				for (WorkLane.FluidPart part : lane.fluidResults()) {
					if (part.tank() == out) {
						return part.fluid().getFluid();
					}
				}
			}
		}
		return null;
	}

	/** Moves the inputs of {@code recipe} into a new lane and snapshots its results. */
	private WorkLane begin(Identifier id, FluidRecipe recipe, int tool, long now) {
		List<WorkLane.ItemPart> taken = new ArrayList<>();
		for (int slot = 0; slot < recipe.items().size(); slot++) {
			int count = recipe.items().get(slot).count();
			ItemStack stack = items.get(slot);
			taken.add(new WorkLane.ItemPart(slot, stack.copyWithCount(count)));
			stack.shrink(count);
		}
		List<WorkLane.FluidPart> drained = new ArrayList<>();
		for (int i = 0; i < recipe.fluids().size(); i++) {
			int amount = recipe.fluids().get(i).amount();
			FluidTank tank = tanks.input(i);
			drained.add(new WorkLane.FluidPart(i, tank.variant, amount));
			tank.drain(amount);
		}
		List<WorkLane.ItemPart> itemResults = new ArrayList<>();
		for (int i = 0; i < recipe.results().size(); i++) {
			itemResults.add(new WorkLane.ItemPart(form.firstOutputSlot() + i, recipe.results().get(i).create()));
		}
		List<WorkLane.FluidPart> fluidResults = new ArrayList<>();
		for (int i = 0; i < recipe.fluidResults().size(); i++) {
			FluidRecipe.FluidAmount result = recipe.fluidResults().get(i);
			fluidResults.add(new WorkLane.FluidPart(recipe.resultTank(i), FluidVariant.of(result.fluid()), result.amount()));
		}
		boolean cold = now - lastWorked > Math.max(1, form.warmupTicks());
		return new WorkLane(id, taken, drained, itemResults, fluidResults, recipe.time(), tool, cold, 0);
	}

	/** Moves a finished batch's results out of the lane, all or nothing; null when done, else what blocks it. */
	private @Nullable MachineStatus finish(WorkLane lane) {
		int inputs = form.tanks().inputTanks().size();
		Map<Integer, Integer> fluidOut = new HashMap<>();
		for (WorkLane.FluidPart part : lane.fluidResults()) {
			FluidTank tank = tanks.output(part.tank());
			int total = fluidOut.merge(part.tank(), part.mb(), Integer::sum);
			if (!tank.variant.isBlank() && !tank.variant.equals(part.fluid()) || tank.millibuckets() + total > tank.capacityMb()) {
				return MachineStatus.at(MachineLifecycle.OUTPUT_BLOCKED, MachineStatus.Reason.OUTPUT_FULL, inputs + part.tank());
			}
		}
		for (WorkLane.ItemPart part : lane.itemResults()) {
			ItemStack held = items.get(part.slot());
			if (!held.isEmpty() && (!ItemStack.isSameItemSameComponents(held, part.stack())
					|| held.getCount() + part.stack().getCount() > Math.min(slots.getMaxStackSize(), held.getMaxStackSize()))) {
				return MachineStatus.at(MachineLifecycle.OUTPUT_BLOCKED, MachineStatus.Reason.OUTPUT_SLOT_FULL, part.slot());
			}
		}
		for (WorkLane.FluidPart part : lane.fluidResults()) {
			FluidTank tank = tanks.output(part.tank());
			if (tank.variant.isBlank()) {
				tank.variant = part.fluid();
			}
			tank.amount += (long) part.mb() * FluidNetworks.DROPLETS_PER_MB;
		}
		for (WorkLane.ItemPart part : lane.itemResults()) {
			ItemStack held = items.get(part.slot());
			if (held.isEmpty()) {
				items.set(part.slot(), part.stack().copy());
			} else {
				held.grow(part.stack().getCount());
			}
		}
		setChanged();
		return null;
	}

	/**
	 * Explains why no batch can start: an input nothing here uses, or else the first shortfall of the recipe the
	 * present inputs most likely belong to: the one needing the fewest empty tanks and slots filled, then the fewest
	 * topped up.
	 */
	private MachineStatus missing(List<FluidRecipes.Entry> runs, List<ItemStack> inputs) {
		for (int tank = 0; tank < form.tanks().inputTanks().size(); tank++) {
			FluidTank held = tanks.input(tank);
			if (held.amount > 0 && !usesFluid(runs, tank, held.variant)) {
				return MachineStatus.fluid(MachineLifecycle.WAITING_INPUT, MachineStatus.Reason.UNUSED_FLUID, tank, held.variant.getFluid(),
						held.millibuckets());
			}
		}
		for (int slot = 0; slot < inputs.size(); slot++) {
			ItemStack held = inputs.get(slot);
			if (!held.isEmpty() && !usesItem(runs, slot, held)) {
				return MachineStatus.item(MachineLifecycle.WAITING_INPUT, MachineStatus.Reason.UNUSED_ITEM, slot, held.getItem(), held.getCount());
			}
		}
		FluidRecipe best = null;
		int bestEmpty = Integer.MAX_VALUE;
		int bestShort = Integer.MAX_VALUE;
		for (FluidRecipes.Entry entry : runs) {
			FluidRecipe recipe = entry.recipe();
			if (!consistent(recipe, inputs)) {
				continue;
			}
			int empty = 0;
			int topUp = 0;
			for (int tank = 0; tank < recipe.fluids().size(); tank++) {
				int have = tanks.input(tank).millibuckets();
				if (have == 0) {
					empty++;
				} else if (have < recipe.fluids().get(tank).amount()) {
					topUp++;
				}
			}
			for (int slot = 0; slot < recipe.items().size(); slot++) {
				int have = inputs.get(slot).getCount();
				if (inputs.get(slot).isEmpty()) {
					empty++;
				} else if (have < recipe.items().get(slot).count()) {
					topUp++;
				}
			}
			if (empty < bestEmpty || empty == bestEmpty && topUp < bestShort) {
				best = recipe;
				bestEmpty = empty;
				bestShort = topUp;
			}
		}
		if (best != null) {
			FluidRecipe recipe = best;
			for (int tank = 0; tank < recipe.fluids().size(); tank++) {
				FluidRecipe.FluidAmount need = recipe.fluids().get(tank);
				FluidTank held = tanks.input(tank);
				int have = held.variant.isOf(need.fluid()) ? held.millibuckets() : 0;
				if (have < need.amount()) {
					return MachineStatus.fluid(MachineLifecycle.WAITING_INPUT, MachineStatus.Reason.MISSING_FLUID, tank, need.fluid(),
							need.amount() - have);
				}
			}
			for (int slot = 0; slot < recipe.items().size(); slot++) {
				FluidRecipe.ItemPart part = recipe.items().get(slot);
				ItemStack held = inputs.get(slot);
				int have = part.ingredient().test(held) ? held.getCount() : 0;
				if (have < part.count()) {
					Item item = !held.isEmpty() ? held.getItem()
							: part.ingredient().items().findFirst().map(Holder::value).orElse(held.getItem());
					return MachineStatus.item(MachineLifecycle.WAITING_INPUT, MachineStatus.Reason.MISSING_ITEM, slot, item, part.count() - have);
				}
			}
		}
		return MachineStatus.of(MachineLifecycle.WAITING_INPUT, MachineStatus.Reason.NONE);
	}

	/** Whether everything in the input slots and tanks belongs where {@code recipe} would use it. */
	private boolean consistent(FluidRecipe recipe, List<ItemStack> inputs) {
		for (int tank = 0; tank < form.tanks().inputTanks().size(); tank++) {
			FluidTank held = tanks.input(tank);
			if (held.amount > 0 && (tank >= recipe.fluids().size() || !held.variant.isOf(recipe.fluids().get(tank).fluid()))) {
				return false;
			}
		}
		for (int slot = 0; slot < inputs.size(); slot++) {
			ItemStack held = inputs.get(slot);
			if (!held.isEmpty() && (slot >= recipe.items().size() || !recipe.items().get(slot).ingredient().test(held))) {
				return false;
			}
		}
		return true;
	}

	private static boolean usesFluid(List<FluidRecipes.Entry> runs, int tank, FluidVariant variant) {
		for (FluidRecipes.Entry entry : runs) {
			List<FluidRecipe.FluidAmount> fluids = entry.recipe().fluids();
			if (tank < fluids.size() && variant.isOf(fluids.get(tank).fluid())) {
				return true;
			}
		}
		return false;
	}

	private static boolean usesItem(List<FluidRecipes.Entry> runs, int slot, ItemStack stack) {
		for (FluidRecipes.Entry entry : runs) {
			List<FluidRecipe.ItemPart> parts = entry.recipe().items();
			if (slot < parts.size() && parts.get(slot).ingredient().test(stack)) {
				return true;
			}
		}
		return false;
	}

	// ------------------------------------------------------------------ controls

	/** Whether socket {@code socket} holds the tool of a running batch, so it cannot be taken out. */
	public boolean socketLocked(int socket) {
		for (WorkLane lane : lanes) {
			if (lane != null && lane.tool() == socket) {
				return true;
			}
		}
		return false;
	}

	/** One bit per socket holding a running batch's tool. */
	private int locks() {
		int bits = 0;
		for (WorkLane lane : lanes) {
			if (lane != null && lane.tool() >= 0 && lane.tool() < 16) {
				bits |= 1 << lane.tool();
			}
		}
		return bits;
	}

	/** Pauses or resumes the machine (running batches keep their escrow and progress). */
	public void togglePause() {
		paused = !paused;
		setChanged();
	}

	/**
	 * Cancels every running batch whose inputs fit back where they came from, returning those untransformed inputs
	 * (all of a batch's, or none of them). Energy already spent is not returned. Returns {lanes cancelled, lanes kept}.
	 */
	public int[] cancel() {
		int cancelled = 0;
		int kept = 0;
		for (int index = lanes.length - 1; index >= 0; index--) {
			WorkLane lane = lanes[index];
			if (lane == null) {
				continue;
			}
			if (returnInputs(lane)) {
				lanes[index] = null;
				cancelled++;
			} else {
				kept++;
			}
		}
		if (cancelled > 0) {
			setChanged();
		}
		return new int[] {cancelled, kept};
	}

	private boolean returnInputs(WorkLane lane) {
		Map<Integer, Long> fluidIn = new HashMap<>();
		for (WorkLane.FluidPart part : lane.fluids()) {
			FluidTank tank = tanks.input(part.tank());
			if (!tank.variant.isBlank() && !tank.variant.equals(part.fluid())) {
				return false;
			}
			long total = fluidIn.merge(part.tank(), (long) part.mb() * FluidNetworks.DROPLETS_PER_MB, Long::sum);
			if (tank.amount + total > tank.getCapacity()) {
				return false;
			}
		}
		Map<Integer, Integer> itemIn = new HashMap<>();
		for (WorkLane.ItemPart part : lane.inputs()) {
			ItemStack held = items.get(part.slot());
			int total = itemIn.merge(part.slot(), part.stack().getCount(), Integer::sum);
			if (!held.isEmpty() && (!ItemStack.isSameItemSameComponents(held, part.stack())
					|| held.getCount() + total > Math.min(slots.getMaxStackSize(), held.getMaxStackSize()))) {
				return false;
			}
		}
		for (WorkLane.FluidPart part : lane.fluids()) {
			FluidTank tank = tanks.input(part.tank());
			if (tank.variant.isBlank()) {
				tank.variant = part.fluid();
			}
			tank.amount += (long) part.mb() * FluidNetworks.DROPLETS_PER_MB;
		}
		for (WorkLane.ItemPart part : lane.inputs()) {
			ItemStack held = items.get(part.slot());
			if (held.isEmpty()) {
				items.set(part.slot(), part.stack().copy());
			} else {
				held.grow(part.stack().getCount());
			}
		}
		return true;
	}

	/** A menu button: 0 pauses or resumes, 1 cancels the running batches. Only the server applies them. */
	public boolean clickButton(ServerPlayer player, int button) {
		if (button == BUTTON_PAUSE) {
			togglePause();
			return true;
		}
		if (button == BUTTON_CANCEL) {
			int[] result = cancel();
			player.sendOverlayMessage(result[1] > 0
					? Component.translatable("message.jugcraft.form.cancel_blocked", result[0], result[1])
					: Component.translatable("message.jugcraft.form.cancelled", result[0]));
			return true;
		}
		return false;
	}

	/** 0 when nothing runs, else rising with the share of lanes running (for a generator, of its full rate made). */
	public int comparatorSignal() {
		if (form.generator()) {
			return made <= 0 ? 0 : Math.min(15, 1 + 14 * made / Math.max(made, burnRate));
		}
		int running = 0;
		for (WorkLane lane : lanes) {
			if (lane != null) {
				running++;
			}
		}
		return running == 0 ? 0 : Math.min(15, 1 + running * 14 / lanes.length);
	}

	// ------------------------------------------------------------------ ports

	/** The tank a fluid port reaches: input ports only fill, output ports only drain. */
	public @Nullable Storage<FluidVariant> fluidPort(FormPort port) {
		return switch (port.kind()) {
			case FLUID_IN -> tanks.input(port.target());
			case FLUID_OUT -> tanks.output(port.target());
			default -> null;
		};
	}

	/** The slots an item port reaches: input ports only insert (what the recipes use), output ports only extract. */
	public @Nullable Storage<ItemVariant> itemPort(FormPort port) {
		if (!port.kind().item()) {
			return null;
		}
		ContainerStorage container = ContainerStorage.of(slots, null);
		boolean input = port.kind() == FormPort.Kind.ITEM_IN;
		int first = input ? 0 : form.firstOutputSlot();
		int count = input ? form.itemInputs() : form.itemOutputs();
		List<Storage<ItemVariant>> reached = new ArrayList<>();
		for (int slot = 0; slot < count; slot++) {
			if (port.reaches(slot)) {
				reached.add(container.getSlot(first + slot));
			}
		}
		Storage<ItemVariant> combined = new CombinedStorage<>(reached);
		return input ? FilteringStorage.insertOnlyOf(combined) : FilteringStorage.extractOnlyOf(combined);
	}

	/** Pushes output tanks and output slots out of their ports, into pipes or the storage beyond. */
	private void push(ServerLevel level, BlockPos pos, BlockState state) {
		Direction facing = state.getValue(MachineBlock.FACING);
		Set<BlockPos> self = null;
		boolean moved = false;
		for (FormPort port : form.ports()) {
			BlockPos at = form.cellPos(pos, facing, form.cellIndex(port.column(), port.row(), port.layer()));
			Direction side = port.side().world(facing);
			if (port.kind() == FormPort.Kind.FLUID_OUT) {
				FluidTank tank = tanks.output(port.target());
				if (tank.amount > 0) {
					moved |= FluidNetworks.pushToNeighbors(level, at, tank, PUSH_MB * FluidNetworks.DROPLETS_PER_MB, List.of(side)) > 0;
				}
			} else if (port.kind() == FormPort.Kind.ITEM_OUT && hasOutputItems()) {
				if (self == null) {
					self = new HashSet<>();
					for (int part = 0; part < form.footprint().size(); part++) {
						self.add(form.footprint().partPos(pos, facing, part).immutable());
					}
				}
				moved |= ItemNetworks.push(level, at, side, itemPort(port), PUSH_ITEMS, self) > 0;
			}
		}
		if (moved) {
			setChanged();
		}
	}

	private boolean hasOutputItems() {
		for (int slot = form.firstOutputSlot(); slot < form.firstSocketSlot(); slot++) {
			if (!items.get(slot).isEmpty()) {
				return true;
			}
		}
		return false;
	}

	// ------------------------------------------------------------------ slots and screen

	@Override
	public Component getDisplayName() {
		return getBlockState().getBlock().getName();
	}

	/**
	 * Inputs take what this form's recipes use there; outputs take nothing; a socket takes one tool it accepts; the
	 * upgrade slots take upgrade cards.
	 */
	public boolean canPlaceItem(int slot, ItemStack stack) {
		if (slot < form.firstOutputSlot()) {
			return !(level instanceof ServerLevel server) || usesItem(recipes(server.getServer()), slot, stack);
		}
		if (slot < form.firstSocketSlot()) {
			return false;
		}
		if (slot < form.firstUpgradeSlot()) {
			return form.sockets().get(slot - form.firstSocketSlot()).accepts(stack) && items.get(slot).isEmpty();
		}
		return MachineUpgrades.isUpgrade(stack);
	}

	@Override
	public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
		return new FormMachineMenu(containerId, inventory, this, data);
	}

	/** The client builds the screen's layout from the form, found by its id. */
	@Override
	public Identifier getScreenOpeningData(ServerPlayer player) {
		return form.id();
	}

	/** Breaking the machine drops its slots and every lane's untransformed inputs, once each. */
	@Override
	public void preRemoveSideEffects(BlockPos pos, BlockState state) {
		super.preRemoveSideEffects(pos, state);
		if (level == null) {
			return;
		}
		Containers.dropContents(level, pos, slots);
		for (int index = 0; index < lanes.length; index++) {
			WorkLane lane = lanes[index];
			if (lane != null) {
				for (WorkLane.ItemPart part : lane.inputs()) {
					Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), part.stack().copy());
				}
				lanes[index] = null;
			}
		}
	}

	// ------------------------------------------------------------------ saving

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		items.clear();
		ContainerHelper.loadAllItems(input, items);
		energy.setAmount(input.getLongOr("energy", 0L));
		paused = input.getBooleanOr("paused", false);
		burn = Math.max(0, input.getLongOr("burn", 0L));
		burnRate = Math.max(0, input.getIntOr("burn_rate", 0));
		List<String> roles = form.tankRoles();
		for (int tank = 0; tank < roles.size(); tank++) {
			FluidTank held = tanks.tank(tank);
			held.variant = FluidVariant.blank();
			held.amount = 0;
			Optional<ValueInput> saved = input.child("tank_" + roles.get(tank));
			for (Map.Entry<String, String> alias : form.roleAliases().entrySet()) {
				if (saved.isEmpty() && alias.getValue().equals(roles.get(tank))) {
					saved = input.child("tank_" + alias.getKey());
				}
			}
			if (saved.isPresent()) {
				held.variant = saved.get().read("fluid", FluidVariant.CODEC).orElseGet(FluidVariant::blank);
				held.amount = held.variant.isBlank() ? 0 : Math.max(0, saved.get().getLongOr("amount", 0L));
			}
		}
		for (int lane = 0; lane < lanes.length; lane++) {
			lanes[lane] = input.read("lane" + lane, WorkLane.CODEC).orElse(null);
		}
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		ContainerHelper.saveAllItems(output, items);
		output.putInt("form_version", form.version());
		output.putLong("energy", energy.getAmount());
		output.putBoolean("paused", paused);
		if (burn > 0) {
			output.putLong("burn", burn);
			output.putInt("burn_rate", burnRate);
		}
		List<String> roles = form.tankRoles();
		for (int tank = 0; tank < roles.size(); tank++) {
			FluidTank held = tanks.tank(tank);
			if (held.amount > 0 && !held.variant.isBlank()) {
				ValueOutput saved = output.child("tank_" + roles.get(tank));
				saved.store("fluid", FluidVariant.CODEC, held.variant);
				saved.putLong("amount", held.amount);
			}
		}
		for (int lane = 0; lane < lanes.length; lane++) {
			if (lanes[lane] != null) {
				output.store("lane" + lane, WorkLane.CODEC, lanes[lane]);
			}
		}
	}
}
