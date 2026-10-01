package io.github.jimbozoomer.jugcraft.machine;

import io.github.jimbozoomer.jugcraft.chemistry.FluidFuels;
import io.github.jimbozoomer.jugcraft.chemistry.FluidMachineSpec;
import io.github.jimbozoomer.jugcraft.chemistry.FluidRecipe;
import io.github.jimbozoomer.jugcraft.chemistry.FluidRecipes;
import io.github.jimbozoomer.jugcraft.chemistry.FluidTank;
import io.github.jimbozoomer.jugcraft.chemistry.FluidTanks;
import io.github.jimbozoomer.jugcraft.chemistry.GasFluid;
import io.github.jimbozoomer.jugcraft.chemistry.OilReservoirs;
import io.github.jimbozoomer.jugcraft.chemistry.PetroFluids;
import io.github.jimbozoomer.jugcraft.energy.EnergyNetworks;
import io.github.jimbozoomer.jugcraft.energy.EnergyStorage;
import io.github.jimbozoomer.jugcraft.energy.SimpleEnergyStorage;
import io.github.jimbozoomer.jugcraft.fluid.FluidNetworks;
import io.github.jimbozoomer.jugcraft.kinetic.KineticConsumer;
import io.github.jimbozoomer.jugcraft.kinetic.KineticNetworks;
import io.github.jimbozoomer.jugcraft.logistics.ItemNetworks;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.IntFunction;
import net.fabricmc.fabric.api.menu.v1.ExtendedMenuProvider;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.fluid.base.SingleFluidStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ContainerStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.StoragePreconditions;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageUtil;
import net.fabricmc.fabric.api.transfer.v1.storage.base.CombinedStorage;
import net.fabricmc.fabric.api.transfer.v1.storage.base.InsertionOnlyStorage;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
import net.fabricmc.fabric.api.transfer.v1.transaction.base.SnapshotParticipant;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.item.crafting.SmeltingRecipe;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

/**
 * State and behavior for every Jugcraft machine. Each machine holds its own internal
 * battery ({@link #energy}); what it does each tick depends on its {@link MachineKind}.
 * All logic runs on the server; clients only see synced {@link ContainerData}.
 */
public class MachineBlockEntity extends BaseContainerBlockEntity implements WorldlyContainer, ExtendedMenuProvider<BlockPos>, KineticConsumer {
	// Container data is synced to clients as 16-bit values, so energy and capacity (which exceed
	// 32,767) are split into low and high halves; see MachineMenu#energy and #capacity.
	public static final int DATA_ENERGY_LOW = 0;
	public static final int DATA_ENERGY_HIGH = 1;
	public static final int DATA_CAPACITY_LOW = 2;
	public static final int DATA_CAPACITY_HIGH = 3;
	public static final int DATA_PROGRESS = 4;
	public static final int DATA_MAX_PROGRESS = 5;
	public static final int DATA_BURN = 6;
	public static final int DATA_MAX_BURN = 7;
	public static final int DATA_FORMED = 8;
	/** mB in the machine's fluid tank (steam generator and ore washer water, geothermal lava). */
	public static final int DATA_TANK = 9;
	/** Processors' side configuration, packed (see {@link SideConfig#pack()}). */
	public static final int DATA_SIDES = 10;
	/** Fluid processors: two values per tank from here (see {@link FluidTanks#data}): the fluid's id, then mB. */
	public static final int DATA_TANKS = 11;
	public static final int DATA_COUNT = DATA_TANKS + 2 * FluidMachineSpec.MAX_TANKS;
	/** Fluid processors push their output tanks into pipes and neighbouring tanks every this many ticks... */
	public static final int FLUID_PUSH_INTERVAL = 4;
	/** ...up to this many mB from each output tank. */
	public static final int FLUID_PUSH_MB = 1_000;
	/** Ticks between ejects, and items pushed out per eject. */
	public static final int EJECT_INTERVAL = 8;
	public static final int EJECT_ITEMS = 16;

	/** Steam generator slots. */
	public static final int SLOT_FUEL = 0;
	public static final int SLOT_WATER_IN = 1;
	public static final int SLOT_BUCKET_OUT = 2;

	private static final int[] NO_SLOTS = {};
	private static final int[] INPUT = {0};

	private static final int[] STEAM_TOP = {SLOT_WATER_IN};
	private static final int[] STEAM_SIDES = {SLOT_FUEL};
	private static final int[] STEAM_BOTTOM = {SLOT_BUCKET_OUT};
	private static final RecipeManager.CachedCheck<SingleRecipeInput, SmeltingRecipe> SMELTING =
			RecipeManager.createCheck(RecipeType.SMELTING);
	private static final RecipeManager.CachedCheck<CraftingInput, CraftingRecipe> CRAFTING =
			RecipeManager.createCheck(RecipeType.CRAFTING);

	private final MachineKind kind;
	private final SimpleEnergyStorage energy;
	private NonNullList<ItemStack> items;
	private int progress;
	private int maxProgress;
	private int burn;
	private int maxBurn;
	/**
	 * Arc furnace: structure complete. Wind turbine: rotor has room to turn. Water wheel: flowing water at the
	 * wheel. Cobblestone generator: water and lava beside it.
	 */
	private boolean formed;
	/** mB in the machine's tank: water for the steam generator and ore washer, lava for the geothermal generator. Saved as "water". */
	private int tank;
	/** Processors: which faces take input or give output, and whether results are pushed out. */
	private final SideConfig sides = new SideConfig();
	/** Ore drill: the next block to check, counted from the top layer below the drill (see {@link OreDrilling}). */
	private int cursor;
	/** Wind turbine, water wheel, cobblestone generator: whether the surroundings check has run since loading. */
	private boolean checked;
	/** Water wheel: JE per tick from the water found at the last check. */
	private int wheelRate;

	private final ContainerData data = new ContainerData() {
		@Override
		public int get(int index) {
			return switch (index) {
				case DATA_ENERGY_LOW -> (int) (energy.getAmount() & 0xFFFF);
				case DATA_ENERGY_HIGH -> (int) ((energy.getAmount() >>> 16) & 0xFFFF);
				case DATA_CAPACITY_LOW -> (int) (energy.getCapacity() & 0xFFFF);
				case DATA_CAPACITY_HIGH -> (int) ((energy.getCapacity() >>> 16) & 0xFFFF);
				case DATA_PROGRESS -> progress;
				case DATA_MAX_PROGRESS -> maxProgress;
				case DATA_BURN -> burn;
				case DATA_MAX_BURN -> maxBurn;
				case DATA_FORMED -> formed ? 1 : 0;
				case DATA_TANK -> tank;
				case DATA_SIDES -> sides.pack();
				default -> tanks != null && index >= DATA_TANKS && index < DATA_TANKS + 2 * tanks.size()
						? tanks.data(index - DATA_TANKS) : 0;
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

	public MachineBlockEntity(BlockPos pos, BlockState state) {
		super(JugcraftMachines.MACHINE_ENTITY, pos, state);
		this.kind = ((MachineBlock) state.getBlock()).kind();
		this.items = NonNullList.withSize(kind.containerSize(), ItemStack.EMPTY);
		// Producers only give energy out; consumers only take it in; the battery box does both.
		long insert = kind.isGenerator() ? 0 : kind.maxInput;
		long extract = kind.isProcessor() ? 0 : kind.maxOutput;
		this.energy = new SimpleEnergyStorage(kind.capacity, insert, extract, this::setChanged);
		this.reservoir = switch (kind) {
			case STEEL_TANK -> SingleFluidStorage.withFixedCapacity(MachineKind.STEEL_TANK_CAPACITY * FluidNetworks.DROPLETS_PER_MB, this::setChanged);
			case GAS_HOLDER -> gasReservoir(MachineKind.GAS_HOLDER_CAPACITY * FluidNetworks.DROPLETS_PER_MB);
			default -> null;
		};
		this.inlet = switch (kind) {
			case STEAM_GENERATOR -> new TankInlet(Fluids.WATER, MachineKind.STEAM_TANK);
			case LARGE_STEAM_ENGINE -> new TankInlet(Fluids.WATER, MachineKind.LARGE_ENGINE_TANK);
			case GEOTHERMAL_GENERATOR -> new TankInlet(Fluids.LAVA, MachineKind.GEOTHERMAL_TANK);
			case ORE_WASHER -> new TankInlet(Fluids.WATER, MachineKind.WASHER_TANK);
			default -> null;
		};
		FluidMachineSpec spec = kind.fluidSpec();
		// Input tanks only take fluids this machine's recipes use in that tank (only the server knows the recipes).
		this.tanks = spec == null ? null : new FluidTanks(spec, (tank, variant) -> level instanceof ServerLevel server
				&& acceptsFluid(server, tank, variant), this::setChanged);
	}

	/**
	 * Which fluids input tank {@code tank} takes from outside: what this machine burns or pumps down, or else what its
	 * recipes use in that tank.
	 */
	private boolean acceptsFluid(ServerLevel server, int tank, FluidVariant variant) {
		return switch (kind) {
			case FRACKING_RIG -> variant.isOf(PetroFluids.FRACKING_FLUID.source());
			case DIESEL_GENERATOR -> tank == 0 && FluidFuels.jePerMb(kind, variant.getFluid()) > 0;
			case DIESEL_ENGINE, FUEL_CELL -> tank == 0 && FluidFuels.jePerMb(kind, variant.getFluid()) > 0;
			case GAS_TURBINE -> tank == 0 ? FluidFuels.jePerMb(kind, variant.getFluid()) > 0
					: variant.isOf(PetroFluids.LUBRICANT.source());
			default -> FluidRecipes.usesFluid(server.getServer(), kind, tank, variant);
		};
	}

	public MachineKind kind() {
		return kind;
	}

	public SideConfig sides() {
		return sides;
	}

	/** A side-configuration button on the machine's screen (see {@link SideConfig#click}). */
	public boolean clickSideButton(int button) {
		if (!kind.isProcessor() || !sides.click(button)) {
			return false;
		}
		setChanged();
		return true;
	}

	/**
	 * Fluid exposed on a side (the same on every side and every part): the steam generator and ore
	 * washer take water and the geothermal generator takes lava into their tanks. Other machines have none.
	 */
	public @Nullable Storage<FluidVariant> fluidFor(@Nullable Direction side) {
		if (tanks != null) {
			return tanks.exposed();
		}
		return reservoir != null ? reservoir : inlet;
	}

	/** The gas holder's store: one gas at a time, and nothing that is not a gas (liquids belong in the steel tank). */
	private SingleFluidStorage gasReservoir(long capacity) {
		return new SingleFluidStorage() {
			@Override
			protected long getCapacity(FluidVariant variant) {
				return capacity;
			}

			@Override
			protected boolean canInsert(FluidVariant variant) {
				return variant.getFluid() instanceof GasFluid;
			}

			@Override
			protected void onFinalCommit() {
				setChanged();
			}
		};
	}

	/** A fluid processor's tanks, or null for other machines. */
	public @Nullable FluidTanks tanks() {
		return tanks;
	}

	/**
	 * Lets pumps, pipes and buckets fill the machine's tank with one fluid. The tank counts whole
	 * millibuckets, so insertion is rounded down to multiples of {@link FluidNetworks#DROPLETS_PER_MB}.
	 */
	private final class TankInlet extends SnapshotParticipant<Integer> implements InsertionOnlyStorage<FluidVariant> {
		private final Fluid fluid;
		private final int capacity;

		TankInlet(Fluid fluid, int capacity) {
			this.fluid = fluid;
			this.capacity = capacity;
		}

		@Override
		public long insert(FluidVariant resource, long maxAmount, TransactionContext transaction) {
			StoragePreconditions.notBlankNotNegative(resource, maxAmount);
			if (!resource.isOf(fluid)) {
				return 0;
			}
			long millibuckets = Math.min(maxAmount / FluidNetworks.DROPLETS_PER_MB, capacity - tank);
			if (millibuckets <= 0) {
				return 0;
			}
			updateSnapshots(transaction);
			tank += (int) millibuckets;
			return millibuckets * FluidNetworks.DROPLETS_PER_MB;
		}

		@Override
		protected Integer createSnapshot() {
			return tank;
		}

		@Override
		protected void readSnapshot(Integer snapshot) {
			tank = snapshot;
		}

		@Override
		protected void onFinalCommit() {
			setChanged();
		}
	}

	private final @Nullable TankInlet inlet;
	/** Fluid processors' input and output tanks (see {@link MachineKind#fluidSpec()}). */
	private final @Nullable FluidTanks tanks;
	/** The steel tank's fluid: a full storage (pumps and buckets can also take it out). */
	private final @Nullable SingleFluidStorage reservoir;

	public @Nullable SingleFluidStorage reservoir() {
		return reservoir;
	}

	/** The storage exposed on a side; the battery box only discharges through its front. */
	public @Nullable EnergyStorage energyFor(@Nullable Direction side) {
		if (!kind.isBattery() || side == null) {
			return energy;
		}
		boolean front = side == getBlockState().getValue(MachineBlock.FACING);
		return new EnergyStorage() {
			@Override
			public boolean supportsInsertion() {
				return !front;
			}

			@Override
			public long insert(long maxAmount, TransactionContext transaction) {
				return front ? 0 : energy.insert(maxAmount, transaction);
			}

			@Override
			public boolean supportsExtraction() {
				return front;
			}

			@Override
			public long extract(long maxAmount, TransactionContext transaction) {
				return front ? energy.extract(maxAmount, transaction) : 0;
			}

			@Override
			public long getAmount() {
				return energy.getAmount();
			}

			@Override
			public long getCapacity() {
				return energy.getCapacity();
			}
		};
	}

	// ------------------------------------------------------------------ ticking

	public void serverTick(ServerLevel level, BlockPos pos, BlockState state) {
		boolean active = switch (kind) {
			case COAL_GENERATOR -> tickGenerator(level, pos);
			case SOLAR_PANEL -> tickSolar(level, pos);
			case STEAM_GENERATOR -> tickSteam(level, pos);
			case LARGE_STEAM_ENGINE -> tickLargeEngine(level, pos, state);
			case BATTERY_BOX, CAPACITOR_BANK, LITHIUM_BATTERY_BANK -> tickBattery(level, pos, state);
			case STEEL_TANK, GAS_HOLDER -> false;
			case GEOTHERMAL_GENERATOR -> tickGeothermal(level, pos, state);
			case WIND_TURBINE -> tickWind(level, pos, state);
			case ORE_DRILL -> tickDrill(level, pos, state);
			case CROP_HARVESTER -> tickHarvester(level, pos, state);
			case COBBLESTONE_GENERATOR -> tickCobble(level, pos, state);
			case WATER_WHEEL -> tickWaterWheel(level, pos, state);
			case AUTO_CRAFTER -> tickCrafter(level, pos, state);
			case PUMPJACK -> tickPumpjack(level, pos, state);
			case FRACKING_RIG -> tickFrackingRig(level, pos, state);
			case DIESEL_GENERATOR -> tickFluidGenerator(level, pos, state, MachineKind.DIESEL_OUTPUT);
			case GAS_TURBINE -> tickFluidGenerator(level, pos, state, MachineKind.TURBINE_OUTPUT);
			case DIESEL_ENGINE -> tickDieselEngine(level, pos, state);
			case FUEL_CELL -> tickFluidGenerator(level, pos, state, MachineKind.FUEL_CELL_OUTPUT);
			default -> kind.isFluidProcessor() ? tickFluidProcessor(level, pos, state) : tickProcessor(level, pos, state);
		};
		if (state.getValue(MachineBlock.LIT) != active) {
			level.setBlock(pos, state.setValue(MachineBlock.LIT, active), 3);
		}
	}

	private boolean tickGenerator(ServerLevel level, BlockPos pos) {
		boolean room = energy.getAmount() < energy.getCapacity();
		if (burn <= 0 && room) {
			ItemStack fuel = items.get(0);
			int ticks = GeneratorFuels.burnTicks(fuel);
			if (ticks > 0) {
				fuel.shrink(1);
				burn = ticks;
				maxBurn = ticks;
				setChanged();
			}
		}
		boolean burning = burn > 0 && room;
		if (burning) {
			burn--;
			energy.setAmount(energy.getAmount() + MachineKind.GENERATION_PER_TICK);
			setChanged();
		}
		EnergyNetworks.pushToNeighbors(level, pos, energy, kind.maxOutput, EnumSet.allOf(Direction.class));
		return burning;
	}

	private boolean tickSolar(ServerLevel level, BlockPos pos) {
		// Checked every tick but only reads the sky and weather: no scanning.
		boolean sunlit = level.isBrightOutside() && level.canSeeSky(pos.above());
		if (sunlit && energy.getAmount() < energy.getCapacity()) {
			int rate = level.isRaining() ? MachineKind.SOLAR_PER_TICK / 2 : MachineKind.SOLAR_PER_TICK;
			energy.setAmount(energy.getAmount() + rate);
			setChanged();
		}
		EnergyNetworks.pushToNeighbors(level, pos, energy, kind.maxOutput, EnumSet.allOf(Direction.class));
		return false;
	}

	private boolean tickGeothermal(ServerLevel level, BlockPos pos, BlockState state) {
		boolean running = tank >= MachineKind.GEOTHERMAL_LAVA_PER_TICK
				&& energy.getAmount() + MachineKind.GEOTHERMAL_PER_TICK <= energy.getCapacity();
		if (running) {
			tank -= MachineKind.GEOTHERMAL_LAVA_PER_TICK;
			energy.setAmount(energy.getAmount() + MachineKind.GEOTHERMAL_PER_TICK);
			setChanged();
		}
		pushFromAllParts(level, pos, state);
		return running;
	}

	/**
	 * Output grows with height above sea level and with rain or thunder. The rotor turns in front of
	 * the top block and sweeps a 7x7 square there, which must be clear (air); that is checked every
	 * few seconds, not every tick. The rotor is drawn by the client (LIT means it is turning).
	 */
	private boolean tickWind(ServerLevel level, BlockPos pos, BlockState state) {
		Direction facing = state.getValue(MachineBlock.FACING);
		BlockPos top = kind.footprint().partPos(pos, facing, kind.footprint().size() - 1);
		if (!checked || level.getGameTime() % MachineKind.WIND_CHECK_INTERVAL == 0) {
			checked = true;
			formed = rotorClear(level, top.relative(facing), facing);
		}
		int rate = 0;
		if (formed) {
			int height = Math.max(0, top.getY() - level.getSeaLevel());
			rate = Math.min(MachineKind.WIND_MAX_PER_TICK, MachineKind.WIND_BASE_PER_TICK + height / 2);
			if (level.isThundering()) {
				rate *= 2;
			} else if (level.isRaining()) {
				rate = rate * 3 / 2;
			}
		}
		if (rate > 0 && energy.getAmount() < energy.getCapacity()) {
			energy.setAmount(energy.getAmount() + rate);
			setChanged();
		}
		pushFromAllParts(level, pos, state);
		return rate > 0;
	}

	/** Whether the square the rotor sweeps, centered on {@code hub}, is all air. */
	private static boolean rotorClear(ServerLevel level, BlockPos hub, Direction facing) {
		Direction side = facing.getClockWise();
		int reach = MachineKind.WIND_ROTOR_REACH;
		for (int across = -reach; across <= reach; across++) {
			for (int up = -reach; up <= reach; up++) {
				if (!level.isEmptyBlock(hub.relative(side, across).above(up))) {
					return false;
				}
			}
		}
		return true;
	}

	/**
	 * Pushes results out of every face set to output (from every block of a multi-block machine),
	 * into item pipes or straight into adjacent inventories.
	 */
	private void eject(ServerLevel level, BlockPos pos, BlockState state) {
		Direction facing = state.getValue(MachineBlock.FACING);
		ContainerStorage inventory = ContainerStorage.of(this, null);
		List<Storage<ItemVariant>> outputs = new ArrayList<>();
		for (int slot = kind.outputSlot(); slot < kind.slots; slot++) {
			if (!items.get(slot).isEmpty()) {
				outputs.add(inventory.getSlot(slot));
			}
		}
		if (outputs.isEmpty()) {
			return;
		}
		Storage<ItemVariant> output = new CombinedStorage<>(outputs);
		Footprint footprint = kind.footprint();
		// Never hand results back to this machine through a pipe that touches another of its blocks.
		Set<BlockPos> self = new HashSet<>();
		for (int part = 0; part < footprint.size(); part++) {
			self.add(footprint.partPos(pos, facing, part).immutable());
		}
		long budget = EJECT_ITEMS;
		for (int part = 0; part < footprint.size() && budget > 0; part++) {
			BlockPos partPos = footprint.partPos(pos, facing, part);
			for (Direction side : Direction.values()) {
				BlockPos neighbor = partPos.relative(side);
				if (!sides.mode(side, facing).output() || MachineBlock.machineAt(level, neighbor, level.getBlockState(neighbor)) == this) {
					continue;
				}
				budget -= ItemNetworks.push(level, partPos, side, output, budget, self);
				if (budget <= 0) {
					return;
				}
			}
		}
	}

	/** Generators that fill several blocks can feed cables touching any of their parts. */
	private void pushFromAllParts(ServerLevel level, BlockPos pos, BlockState state) {
		Footprint footprint = kind.footprint();
		Direction facing = state.getValue(MachineBlock.FACING);
		for (int part = 0; part < footprint.size(); part++) {
			EnergyNetworks.pushToNeighbors(level, footprint.partPos(pos, facing, part), energy, kind.maxOutput,
					EnumSet.allOf(Direction.class));
		}
	}

	private boolean tickSteam(ServerLevel level, BlockPos pos) {
		refillWater(level, pos);
		boolean room = energy.getAmount() < energy.getCapacity();
		boolean hasWater = tank >= MachineKind.STEAM_WATER_PER_TICK;
		if (burn <= 0 && room && hasWater) {
			ItemStack fuel = items.get(SLOT_FUEL);
			int ticks = GeneratorFuels.steamBurnTicks(fuel);
			if (ticks > 0) {
				fuel.shrink(1);
				burn = ticks;
				maxBurn = ticks;
				setChanged();
			}
		}
		boolean boiling = burn > 0 && room && hasWater;
		if (boiling) {
			burn--;
			tank -= MachineKind.STEAM_WATER_PER_TICK;
			energy.setAmount(energy.getAmount() + MachineKind.STEAM_PER_TICK);
			setChanged();
		}
		EnergyNetworks.pushToNeighbors(level, pos, energy, kind.maxOutput, EnumSet.allOf(Direction.class));
		return boiling;
	}

	/**
	 * The large steam engine: like the steam generator, but it turns a shaft out of the back of its upper
	 * right back block ({@link MachineKind#LARGE_ENGINE_OUTPUT} KE/t), burning fuel four times as fast. It
	 * burns only while something on the shaft line takes the power.
	 */
	private boolean tickLargeEngine(ServerLevel level, BlockPos pos, BlockState state) {
		refillWater(level, pos);
		boolean hasWater = tank >= MachineKind.LARGE_ENGINE_WATER_PER_TICK;
		if (burn <= 0 && hasWater) {
			ItemStack fuel = items.get(SLOT_FUEL);
			int ticks = GeneratorFuels.steamBurnTicks(fuel);
			if (ticks > 0) {
				fuel.shrink(1);
				burn = ticks;
				maxBurn = ticks;
				setChanged();
			}
		}
		if (burn <= 0 || !hasWater) {
			return false;
		}
		Direction facing = facing(state);
		BlockPos output = kind.footprint().partPos(pos, facing, MachineKind.LARGE_ENGINE_OUTPUT_PART);
		long taken = KineticNetworks.push(level, output, facing.getOpposite(), MachineKind.LARGE_ENGINE_OUTPUT);
		if (taken <= 0) {
			return false;
		}
		burn = Math.max(0, burn - MachineKind.LARGE_ENGINE_BURN_PER_TICK);
		tank -= MachineKind.LARGE_ENGINE_WATER_PER_TICK;
		setChanged();
		return true;
	}

	/**
	 * The diesel engine: keeps up to {@link MachineKind#DIESEL_ENGINE_OUTPUT} KE in hand ({@link #burn}) by burning
	 * fuel from its tank a millibucket at a time ({@link FluidFuels#jePerMb}), and pushes it out of the back of its
	 * upper right back block into a shaft line. Only what the line takes is spent, so an idle engine burns nothing.
	 */
	private boolean tickDieselEngine(ServerLevel level, BlockPos pos, BlockState state) {
		int output = MachineKind.DIESEL_ENGINE_OUTPUT;
		maxBurn = output;
		maxProgress = output;
		if (!sides.redstone().allows(poweredByRedstone(level, pos, state))) {
			return false;
		}
		FluidTank fuel = tanks.input(0);
		while (burn < output && fuel.millibuckets() > 0) {
			int value = FluidFuels.jePerMb(kind, fuel.variant.getFluid());
			if (value <= 0) {
				break;
			}
			fuel.drain(1);
			burn += value;
			setChanged();
		}
		progress = Math.min(burn, output);
		if (burn <= 0) {
			return false;
		}
		Direction facing = facing(state);
		BlockPos shaft = kind.footprint().partPos(pos, facing, MachineKind.DIESEL_ENGINE_OUTPUT_PART);
		long taken = KineticNetworks.push(level, shaft, facing.getOpposite(), Math.min(burn, output));
		if (taken <= 0) {
			return false;
		}
		burn -= (int) taken;
		setChanged();
		return true;
	}

	private void refillWater(ServerLevel level, BlockPos pos) {
		int capacity = kind.tankCapacity();
		if (tank < capacity && level.getFluidState(pos.below()).isSourceOfType(Fluids.WATER)) {
			tank = Math.min(capacity, tank + MachineKind.STEAM_SOURCE_REFILL);
			setChanged();
		}
		ItemStack bucket = items.get(SLOT_WATER_IN);
		ItemStack empties = items.get(SLOT_BUCKET_OUT);
		boolean emptySpace = empties.isEmpty() || (empties.is(Items.BUCKET) && empties.getCount() < empties.getMaxStackSize());
		if (bucket.is(Items.WATER_BUCKET) && tank <= capacity - 1000 && emptySpace) {
			bucket.shrink(1);
			if (empties.isEmpty()) {
				items.set(SLOT_BUCKET_OUT, new ItemStack(Items.BUCKET));
			} else {
				empties.grow(1);
			}
			tank += 1000;
			setChanged();
		}
	}

	/** Batteries give power out of their front face only; the capacitor bank out of the front of all four blocks. */
	private boolean tickBattery(ServerLevel level, BlockPos pos, BlockState state) {
		Direction front = state.getValue(MachineBlock.FACING);
		Footprint footprint = kind.footprint();
		long budget = kind.maxOutput;
		for (int part = 0; part < footprint.size() && budget > 0; part++) {
			budget -= EnergyNetworks.pushToNeighbors(level, footprint.partPos(pos, facing(state), part), energy, budget, List.of(front));
		}
		return false;
	}

	private static Direction facing(BlockState state) {
		return state.getValue(MachineBlock.FACING);
	}

	private boolean tickProcessor(ServerLevel level, BlockPos pos, BlockState state) {
		if (sides.eject() && level.getGameTime() % EJECT_INTERVAL == 0) {
			eject(level, pos, state);
		}
		if (kind == MachineKind.ORE_WASHER && tank < MachineKind.WASHER_TANK
				&& level.getFluidState(pos.below()).isSourceOfType(Fluids.WATER)) {
			// A water source below is a spring, as for the steam generator: it is never used up.
			tank = Math.min(MachineKind.WASHER_TANK, tank + MachineKind.WASHER_SOURCE_REFILL);
			setChanged();
		}
		if (kind == MachineKind.ARC_FURNACE && level.getGameTime() % 20 == 0) {
			formed = ArcFurnaceStructure.isFormed(level, pos, state.getValue(MachineBlock.FACING),
					JugcraftMachines.ARC_FURNACE_CASING);
		}
		if (kind == MachineKind.ARC_FURNACE && !formed) {
			progress = 0;
			return false;
		}

		Optional<Result> result = findResult(level);
		boolean water = kind != MachineKind.ORE_WASHER || tank >= MachineKind.WASHER_WATER_PER_OPERATION;
		if (result.isEmpty() || !water || !canOutput(result.get().stack()) || !byproductsFit(result.get().byproducts())) {
			if (progress != 0) {
				progress = 0;
				setChanged();
			}
			return false;
		}

		if (!sides.redstone().allows(poweredByRedstone(level, pos, state))) {
			return false; // Keeps progress; resumes when the redstone condition is met.
		}
		MachineUpgrades.Effect upgrades = upgrades();
		maxProgress = upgrades.ticks(result.get().ticks());
		long use = upgrades.use(kind.usePerTick);
		if (energy.getAmount() < use) {
			return false; // Keeps progress; resumes when power returns.
		}
		energy.setAmount(energy.getAmount() - use);
		progress++;
		if (progress >= maxProgress) {
			progress = 0;
			int out = kind.outputSlot();
			ItemStack output = items.get(out);
			if (output.isEmpty()) {
				items.set(out, result.get().stack().copy());
			} else {
				output.grow(result.get().stack().getCount());
			}
			int[] take = result.get().take();
			for (int slot = 0; slot < take.length; slot++) {
				items.get(slot).shrink(take[slot]);
			}
			for (MachineRecipe.Byproduct byproduct : result.get().byproducts()) {
				if (byproduct.enabled() && level.getRandom().nextFloat() < byproduct.chance()) {
					addByproduct(byproduct.result().create());
				}
			}
			if (kind == MachineKind.ORE_WASHER) {
				tank -= MachineKind.WASHER_WATER_PER_OPERATION;
			}
		}
		setChanged();
		return true;
	}

	/**
	 * A fluid processor: runs the first {@link FluidRecipe} whose items and fluids are present and whose results have
	 * room, at {@link MachineKind#usePerTick} JE a tick, and pushes its output tanks out of its outer faces.
	 */
	private boolean tickFluidProcessor(ServerLevel level, BlockPos pos, BlockState state) {
		pushFluids(level, pos, state);
		List<ItemStack> inputs = items.subList(0, kind.outputSlot());
		Optional<FluidRecipe> found = FluidRecipes.find(level.getServer(), kind, inputs, tanks);
		if (found.isEmpty() || !found.get().fluidResultsFit(tanks) || !itemResultsFit(found.get())) {
			if (progress != 0) {
				progress = 0;
				setChanged();
			}
			return false;
		}
		if (!sides.redstone().allows(poweredByRedstone(level, pos, state))) {
			return false;
		}
		FluidRecipe recipe = found.get();
		maxProgress = recipe.time();
		if (energy.getAmount() < kind.usePerTick) {
			return false; // Keeps progress; resumes when power returns.
		}
		energy.setAmount(energy.getAmount() - kind.usePerTick);
		if (++progress >= maxProgress) {
			progress = 0;
			for (int slot = 0; slot < recipe.items().size(); slot++) {
				items.get(slot).shrink(recipe.items().get(slot).count());
			}
			for (int i = 0; i < recipe.fluids().size(); i++) {
				tanks.input(i).drain(recipe.fluids().get(i).amount());
			}
			for (int i = 0; i < recipe.fluidResults().size(); i++) {
				tanks.output(i).fill(recipe.fluidResults().get(i).fluid(), recipe.fluidResults().get(i).amount());
			}
			for (int i = 0; i < recipe.results().size(); i++) {
				ItemStack result = recipe.results().get(i).create();
				ItemStack held = items.get(kind.outputSlot() + i);
				if (held.isEmpty()) {
					items.set(kind.outputSlot() + i, result);
				} else {
					held.grow(result.getCount());
				}
			}
		}
		setChanged();
		return true;
	}

	/**
	 * The pumpjack: while the chunk under its wellhead (the master block) holds conventional oil, it pumps
	 * {@link MachineKind#PUMPJACK_RATE} mB of crude oil a tick into its tank, at its JE per tick. {@link #formed} says
	 * whether there is oil to pump (checked once a second); the progress arrow shows the pump's stroke.
	 */
	private boolean tickPumpjack(ServerLevel level, BlockPos pos, BlockState state) {
		pushFluids(level, pos, state);
		ChunkPos chunk = ChunkPos.containing(pos);
		if (!checked || level.getGameTime() % MachineKind.SOURCE_CHECK_INTERVAL == 0) {
			checked = true;
			OilReservoirs.Reservoir reservoir = OilReservoirs.get(level, chunk);
			boolean oil = reservoir.kind() == OilReservoirs.Kind.CONVENTIONAL && !reservoir.isDry();
			if (oil != formed) {
				formed = oil;
				setChanged();
			}
		}
		FluidTank tank = tanks.output(0);
		if (!formed || !tank.fits(PetroFluids.CRUDE_OIL.source(), MachineKind.PUMPJACK_RATE)
				|| !sides.redstone().allows(poweredByRedstone(level, pos, state)) || energy.getAmount() < kind.usePerTick) {
			return false;
		}
		int pumped = OilReservoirs.extract(level, chunk, OilReservoirs.Kind.CONVENTIONAL, MachineKind.PUMPJACK_RATE);
		if (pumped <= 0) {
			formed = false;
			setChanged();
			return false;
		}
		energy.setAmount(energy.getAmount() - kind.usePerTick);
		tank.fill(PetroFluids.CRUDE_OIL.source(), pumped);
		maxProgress = PUMPJACK_STROKE;
		progress = (progress + 1) % PUMPJACK_STROKE;
		setChanged();
		return true;
	}

	/**
	 * The fracking rig: while the chunk under its master block holds shale oil, each powered tick it pumps
	 * {@link MachineKind#FRACK_FLUID_PER_TICK} mB of fracking fluid down, frees {@link MachineKind#FRACK_OIL_PER_TICK} mB
	 * from the reservoir (three quarters crude oil, a quarter refinery gas) and returns
	 * {@link MachineKind#FRACK_FLOWBACK_PER_TICK} mB of flowback water. {@link #formed} says whether there is shale
	 * under it (checked once a second).
	 */
	private boolean tickFrackingRig(ServerLevel level, BlockPos pos, BlockState state) {
		pushFluids(level, pos, state);
		ChunkPos chunk = ChunkPos.containing(pos);
		if (!checked || level.getGameTime() % MachineKind.SOURCE_CHECK_INTERVAL == 0) {
			checked = true;
			OilReservoirs.Reservoir reservoir = OilReservoirs.get(level, chunk);
			boolean shale = reservoir.kind() == OilReservoirs.Kind.SHALE && !reservoir.isDry();
			if (shale != formed) {
				formed = shale;
				setChanged();
			}
		}
		int oilShare = MachineKind.FRACK_OIL_PER_TICK * 3 / 4;
		int gasShare = MachineKind.FRACK_OIL_PER_TICK - oilShare;
		FluidTank fluid = tanks.input(0);
		if (!formed || !fluid.has(PetroFluids.FRACKING_FLUID.source(), MachineKind.FRACK_FLUID_PER_TICK)
				|| !tanks.output(0).fits(PetroFluids.CRUDE_OIL.source(), oilShare)
				|| !tanks.output(1).fits(PetroFluids.REFINERY_GAS.fluid(), gasShare)
				|| !tanks.output(2).fits(PetroFluids.FLOWBACK_WATER.source(), MachineKind.FRACK_FLOWBACK_PER_TICK)
				|| !sides.redstone().allows(poweredByRedstone(level, pos, state)) || energy.getAmount() < kind.usePerTick) {
			return false;
		}
		int freed = OilReservoirs.extract(level, chunk, OilReservoirs.Kind.SHALE, MachineKind.FRACK_OIL_PER_TICK);
		if (freed <= 0) {
			formed = false;
			setChanged();
			return false;
		}
		energy.setAmount(energy.getAmount() - kind.usePerTick);
		fluid.drain(MachineKind.FRACK_FLUID_PER_TICK);
		int oil = freed * 3 / 4;
		tanks.output(0).fill(PetroFluids.CRUDE_OIL.source(), oil);
		if (freed > oil) {
			tanks.output(1).fill(PetroFluids.REFINERY_GAS.fluid(), freed - oil);
		}
		tanks.output(2).fill(PetroFluids.FLOWBACK_WATER.source(), MachineKind.FRACK_FLOWBACK_PER_TICK);
		maxProgress = PUMPJACK_STROKE;
		progress = (progress + 1) % PUMPJACK_STROKE;
		setChanged();
		return true;
	}

	/**
	 * A fluid-burning generator: makes {@code output} JE every tick it has room, burning fuel from tank 0 a millibucket
	 * at a time as the JE it holds in hand ({@link #burn}) runs low; each mB is worth {@link FluidFuels#jePerMb}. Pushes
	 * power out of every block. The progress bar shows the JE in hand. A second input tank (the gas turbine) holds
	 * lubricant: it will not run dry, and running uses 1 mB every {@link FluidFuels#LUBRICANT_TICKS} game ticks.
	 */
	private boolean tickFluidGenerator(ServerLevel level, BlockPos pos, BlockState state, int output) {
		boolean running = false;
		boolean room = energy.getAmount() + output <= energy.getCapacity();
		FluidTank lubricant = tanks.spec().inputTanks().size() > 1 ? tanks.input(1) : null;
		boolean oiled = lubricant == null || lubricant.millibuckets() > 0;
		if (room && oiled && sides.redstone().allows(poweredByRedstone(level, pos, state))) {
			FluidTank fuel = tanks.input(0);
			while (burn < output && fuel.millibuckets() > 0) {
				int value = FluidFuels.jePerMb(kind, fuel.variant.getFluid());
				if (value <= 0) {
					break;
				}
				fuel.drain(1);
				burn += value;
			}
			if (burn >= output) {
				burn -= output;
				energy.setAmount(energy.getAmount() + output);
				running = true;
				if (lubricant != null && level.getGameTime() % FluidFuels.LUBRICANT_TICKS == 0) {
					lubricant.drain(1);
				}
			}
			maxBurn = output;
			maxProgress = output;
			progress = Math.min(burn, output);
			setChanged();
		}
		pushFromAllParts(level, pos, state);
		return running;
	}

	/** Ticks per stroke of the pumpjack, for its screen's progress arrow. */
	private static final int PUMPJACK_STROKE = 40;

	/** Whether every item result of the recipe fits its output slot. */
	private boolean itemResultsFit(FluidRecipe recipe) {
		FluidMachineSpec spec = kind.fluidSpec();
		if (recipe.results().size() > spec.itemOutputs()) {
			return false;
		}
		for (int i = 0; i < recipe.results().size(); i++) {
			ItemStack result = recipe.results().get(i).create();
			ItemStack held = items.get(kind.outputSlot() + i);
			if (!held.isEmpty() && !(ItemStack.isSameItemSameComponents(held, result)
					&& held.getCount() + result.getCount() <= Math.min(getMaxStackSize(), held.getMaxStackSize()))) {
				return false;
			}
		}
		return true;
	}

	/** Every {@link #FLUID_PUSH_INTERVAL} ticks, pushes the output tanks out of the machine's outer faces. */
	private void pushFluids(ServerLevel level, BlockPos pos, BlockState state) {
		if (tanks == null || level.getGameTime() % FLUID_PUSH_INTERVAL != 0) {
			return;
		}
		Footprint footprint = kind.footprint();
		Direction facing = facing(state);
		List<BlockPos> parts = new ArrayList<>();
		for (int part = 0; part < footprint.size(); part++) {
			parts.add(footprint.partPos(pos, facing, part));
		}
		Set<BlockPos> inside = new HashSet<>(parts);
		List<FluidTanks.Port> ports = new ArrayList<>();
		for (BlockPos part : parts) {
			List<Direction> outer = new ArrayList<>();
			for (Direction side : Direction.values()) {
				if (!inside.contains(part.relative(side))) {
					outer.add(side);
				}
			}
			ports.add(new FluidTanks.Port(part, outer));
		}
		// Some machines give each output from its own height only (the distillation tower's draw-offs).
		IntFunction<List<FluidTanks.Port>> portsFor = tank -> {
			int layer = kind.outputLayer(tank);
			return layer < 0 ? ports : ports.stream().filter(port -> port.pos().getY() - pos.getY() == layer).toList();
		};
		if (tanks.pushOutputs(level, portsFor, FLUID_PUSH_MB * FluidNetworks.DROPLETS_PER_MB)) {
			setChanged();
		}
	}

	/**
	 * The ore drill: finds the next ore below it (one layer of the 9x9 column per tick), then mines it
	 * over {@link MachineKind#DRILL_TICKS} powered ticks. The ore block goes into the first result slot
	 * with room and the hole is filled with stone (see {@link OreDrilling}); a full drill waits.
	 */
	private boolean tickDrill(ServerLevel level, BlockPos pos, BlockState state) {
		if (sides.eject() && level.getGameTime() % EJECT_INTERVAL == 0) {
			eject(level, pos, state);
		}
		BlockPos target = OreDrilling.target(pos, cursor, level.getMinY());
		if (target == null) {
			return false; // Past the bottom of the world: everything in reach is mined.
		}
		BlockState ore = level.getBlockState(target);
		if (!OreDrilling.isOre(ore)) {
			// Look further; mining has not started on this block.
			for (int step = 0; step < MachineKind.DRILL_SCAN_PER_TICK; step++) {
				cursor++;
				target = OreDrilling.target(pos, cursor, level.getMinY());
				if (target == null || OreDrilling.isOre(level.getBlockState(target))) {
					break;
				}
			}
			progress = 0;
			setChanged();
			return false;
		}
		ItemStack mined = new ItemStack(ore.getBlock().asItem());
		if (resultSlotFor(mined) < 0 || !sides.redstone().allows(poweredByRedstone(level, pos, state))) {
			return false;
		}
		MachineUpgrades.Effect upgrades = upgrades();
		maxProgress = upgrades.ticks(MachineKind.DRILL_TICKS);
		long use = upgrades.use(kind.usePerTick);
		if (energy.getAmount() < use) {
			return false;
		}
		energy.setAmount(energy.getAmount() - use);
		if (++progress >= maxProgress) {
			progress = 0;
			int slot = resultSlotFor(mined);
			if (items.get(slot).isEmpty()) {
				items.set(slot, mined);
			} else {
				items.get(slot).grow(1);
			}
			level.setBlock(target, OreDrilling.filler(ore), 3);
			cursor++;
		}
		setChanged();
		return true;
	}

	/**
	 * The crop harvester: works through the 9x9 field in front of it (at its own height, where crops on farmland sit),
	 * and each ripe crop it finds takes {@link MachineKind#HARVEST_TICKS} powered ticks to harvest. The crop's drops go
	 * into the result slots, less one seed, which it plants again; when the drops would not all fit, it waits.
	 */
	private boolean tickHarvester(ServerLevel level, BlockPos pos, BlockState state) {
		if (sides.eject() && level.getGameTime() % EJECT_INTERVAL == 0) {
			eject(level, pos, state);
		}
		if (!sides.redstone().allows(poweredByRedstone(level, pos, state))) {
			return false;
		}
		BlockPos target = null;
		for (int step = 0; step < MachineKind.HARVEST_SCAN_PER_TICK; step++) {
			BlockPos at = harvestTarget(pos, facing(state), cursor);
			if (level.isLoaded(at) && isRipe(level.getBlockState(at))) {
				target = at;
				break;
			}
			cursor = (cursor + 1) % HARVEST_AREA;
		}
		if (target == null) {
			if (progress != 0) {
				progress = 0;
				setChanged();
			}
			return false;
		}
		MachineUpgrades.Effect upgrades = upgrades();
		maxProgress = upgrades.ticks(MachineKind.HARVEST_TICKS);
		long use = upgrades.use(kind.usePerTick);
		if (progress < maxProgress) {
			if (energy.getAmount() < use) {
				return false;
			}
			energy.setAmount(energy.getAmount() - use);
			progress++;
		}
		if (progress >= maxProgress) {
			BlockState crop = level.getBlockState(target);
			List<ItemStack> drops = new ArrayList<>(Block.getDrops(crop, level, target, null));
			Item seed = crop.getBlock().asItem();
			boolean replant = false;
			for (ItemStack drop : drops) {
				if (!replant && drop.is(seed)) {
					drop.shrink(1);
					replant = true;
				}
			}
			if (!storeAll(drops)) {
				setChanged();
				return false; // Full: wait with the harvest ready.
			}
			level.setBlock(target, replant && crop.getBlock() instanceof CropBlock block ? block.getStateForAge(0)
					: Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
			progress = 0;
			cursor = (cursor + 1) % HARVEST_AREA;
		}
		setChanged();
		return true;
	}

	private static final int HARVEST_AREA = (2 * MachineKind.HARVEST_RADIUS + 1) * (2 * MachineKind.HARVEST_RADIUS + 1);

	/** Field block {@code index} of a harvester at {@code pos} facing {@code front}: the 9x9 square starting in front. */
	static BlockPos harvestTarget(BlockPos pos, Direction front, int index) {
		int side = 2 * MachineKind.HARVEST_RADIUS + 1;
		int across = index % side - MachineKind.HARVEST_RADIUS;
		int forward = index / side + 1;
		return pos.relative(front, forward).relative(front.getClockWise(), across);
	}

	private static boolean isRipe(BlockState state) {
		return state.getBlock() instanceof CropBlock crop && crop.isMaxAge(state);
	}

	/** Puts every stack into the result slots, or changes nothing and returns false if they would not all fit. */
	private boolean storeAll(List<ItemStack> stacks) {
		int first = kind.outputSlot();
		List<ItemStack> slots = new ArrayList<>();
		for (int slot = first; slot < kind.slots; slot++) {
			slots.add(items.get(slot).copy());
		}
		for (ItemStack stack : stacks) {
			ItemStack left = stack.copy();
			for (int i = 0; i < slots.size() && !left.isEmpty(); i++) {
				ItemStack held = slots.get(i);
				int limit = Math.min(getMaxStackSize(), left.getMaxStackSize());
				if (held.isEmpty()) {
					int moved = Math.min(limit, left.getCount());
					slots.set(i, left.copyWithCount(moved));
					left.shrink(moved);
				} else if (ItemStack.isSameItemSameComponents(held, left)) {
					int moved = Math.min(limit - held.getCount(), left.getCount());
					if (moved > 0) {
						held.grow(moved);
						left.shrink(moved);
					}
				}
			}
			if (!left.isEmpty()) {
				return false;
			}
		}
		for (int i = 0; i < slots.size(); i++) {
			items.set(first + i, slots.get(i));
		}
		return true;
	}

	/**
	 * The cobblestone generator: with water and lava touching it (checked every second; neither is used up), it
	 * makes one cobblestone per {@link MachineKind#COBBLE_TICKS} powered ticks, like a vanilla cobblestone generator.
	 */
	private boolean tickCobble(ServerLevel level, BlockPos pos, BlockState state) {
		if (sides.eject() && level.getGameTime() % EJECT_INTERVAL == 0) {
			eject(level, pos, state);
		}
		if (!checked || level.getGameTime() % MachineKind.SOURCE_CHECK_INTERVAL == 0) {
			checked = true;
			boolean water = false;
			boolean lava = false;
			for (Direction side : Direction.values()) {
				FluidState fluid = level.getFluidState(pos.relative(side));
				water |= fluid.is(FluidTags.WATER);
				lava |= fluid.is(FluidTags.LAVA);
			}
			formed = water && lava;
		}
		ItemStack cobble = new ItemStack(Items.COBBLESTONE);
		if (!formed || !canOutput(cobble) || !sides.redstone().allows(poweredByRedstone(level, pos, state))) {
			return false;
		}
		MachineUpgrades.Effect upgrades = upgrades();
		maxProgress = upgrades.ticks(MachineKind.COBBLE_TICKS);
		long use = upgrades.use(kind.usePerTick);
		if (energy.getAmount() < use) {
			return false;
		}
		energy.setAmount(energy.getAmount() - use);
		if (++progress >= maxProgress) {
			progress = 0;
			ItemStack output = items.get(kind.outputSlot());
			if (output.isEmpty()) {
				items.set(kind.outputSlot(), cobble);
			} else {
				output.grow(1);
			}
		}
		setChanged();
		return true;
	}

	/**
	 * The water wheel turns in the column of blocks on its right (seen from the front), beside both of its
	 * blocks. Each block of flowing water there gives {@link MachineKind#WATER_WHEEL_FLOWING} JE/t, falling
	 * water {@link MachineKind#WATER_WHEEL_FALLING}; still (source) water does not turn it. Checked every second.
	 */
	private boolean tickWaterWheel(ServerLevel level, BlockPos pos, BlockState state) {
		if (!checked || level.getGameTime() % MachineKind.SOURCE_CHECK_INTERVAL == 0) {
			checked = true;
			Direction wheelSide = facing(state).getCounterClockWise();
			Footprint footprint = kind.footprint();
			int rate = 0;
			for (int part = 0; part < footprint.size(); part++) {
				FluidState fluid = level.getFluidState(footprint.partPos(pos, facing(state), part).relative(wheelSide));
				if (fluid.is(FluidTags.WATER) && !fluid.isSource()) {
					rate += fluid.getValue(FlowingFluid.FALLING) ? MachineKind.WATER_WHEEL_FALLING : MachineKind.WATER_WHEEL_FLOWING;
				}
			}
			wheelRate = rate;
			formed = rate > 0;
		}
		if (wheelRate > 0 && energy.getAmount() < energy.getCapacity()) {
			energy.setAmount(Math.min(energy.getCapacity(), energy.getAmount() + wheelRate));
			setChanged();
		}
		pushFromAllParts(level, pos, state);
		return wheelRate > 0;
	}

	/**
	 * The auto-crafter crafts the vanilla (or data pack) crafting recipe laid out in its 3x3 grid. Each grid
	 * slot keeps its last item as the pattern, so it crafts only while every filled slot holds at least two.
	 * The result goes to the output slot and container remainders (empty buckets, bottles) to the slot above.
	 */
	private boolean tickCrafter(ServerLevel level, BlockPos pos, BlockState state) {
		if (sides.eject() && level.getGameTime() % EJECT_INTERVAL == 0) {
			eject(level, pos, state);
		}
		List<ItemStack> pattern = new ArrayList<>(GRID);
		boolean any = false;
		boolean stocked = true;
		for (int slot = 0; slot < GRID; slot++) {
			ItemStack stack = items.get(slot);
			pattern.add(stack.isEmpty() ? ItemStack.EMPTY : stack.copyWithCount(1));
			any |= !stack.isEmpty();
			stocked &= stack.isEmpty() || stack.getCount() >= 2;
		}
		if (!any || !stocked) {
			return false; // Keeps progress: more ingredients may be on the way.
		}
		CraftingInput input = CraftingInput.of(3, 3, pattern);
		Optional<RecipeHolder<CraftingRecipe>> recipe = CRAFTING.getRecipeFor(input, level);
		if (recipe.isEmpty()) {
			progress = 0;
			return false;
		}
		ItemStack result = recipe.get().value().assemble(input);
		ItemStack remainder = ItemStack.EMPTY;
		for (ItemStack left : recipe.get().value().getRemainingItems(input)) {
			if (left.isEmpty()) {
				continue;
			}
			if (remainder.isEmpty()) {
				remainder = left.copy();
			} else if (ItemStack.isSameItemSameComponents(remainder, left)) {
				remainder.grow(left.getCount());
			} else {
				return false; // Two kinds of remainder: not supported, and nothing is ever lost.
			}
		}
		if (result.isEmpty() || !canOutput(result) || (!remainder.isEmpty() && byproductSlotFor(remainder) < 0)
				|| !sides.redstone().allows(poweredByRedstone(level, pos, state))) {
			return false;
		}
		MachineUpgrades.Effect upgrades = upgrades();
		maxProgress = upgrades.ticks(MachineKind.CRAFT_TICKS);
		long use = upgrades.use(kind.usePerTick);
		if (energy.getAmount() < use) {
			return false;
		}
		energy.setAmount(energy.getAmount() - use);
		if (++progress >= maxProgress) {
			progress = 0;
			ItemStack output = items.get(kind.outputSlot());
			if (output.isEmpty()) {
				items.set(kind.outputSlot(), result.copy());
			} else {
				output.grow(result.getCount());
			}
			for (int slot = 0; slot < GRID; slot++) {
				items.get(slot).shrink(1);
			}
			if (!remainder.isEmpty()) {
				addByproduct(remainder);
			}
		}
		setChanged();
		return true;
	}

	/** Slots in the auto-crafter's pattern grid. */
	public static final int GRID = 9;

	/** The ore drill's first result slot (output, then the two extra slots) with room for {@code stack}, or -1. */
	private int resultSlotFor(ItemStack stack) {
		for (int slot = kind.outputSlot(); slot < kind.slots; slot++) {
			ItemStack held = items.get(slot);
			if (held.isEmpty() || (ItemStack.isSameItemSameComponents(held, stack)
					&& held.getCount() + stack.getCount() <= Math.min(getMaxStackSize(), held.getMaxStackSize()))) {
				return slot;
			}
		}
		return -1;
	}

	/**
	 * Kinetic power from a shaft or gearbox counts as JE for machines that use power (not generators or
	 * batteries), up to the machine's input rate. It reaches the machine through any of its blocks.
	 */
	@Override
	public long acceptKinetic(Direction side, long maxAmount) {
		if (!kind.usesPower() || kind.isGenerator() || kind.isBattery()) {
			return 0;
		}
		long take = Math.min(Math.min(maxAmount, kind.maxInput), energy.getCapacity() - energy.getAmount());
		if (take <= 0) {
			return 0;
		}
		energy.setAmount(energy.getAmount() + take);
		setChanged();
		return take;
	}

	/** The cards in this machine's upgrade slots. */
	public MachineUpgrades.Effect upgrades() {
		if (kind.upgradeSlots() == 0) {
			return MachineUpgrades.Effect.NONE;
		}
		return MachineUpgrades.effect(items.subList(kind.slots, kind.containerSize()));
	}

	/** Whether any block of the machine receives a redstone signal. */
	private boolean poweredByRedstone(ServerLevel level, BlockPos pos, BlockState state) {
		Footprint footprint = kind.footprint();
		Direction facing = state.getValue(MachineBlock.FACING);
		for (int part = 0; part < footprint.size(); part++) {
			if (level.hasNeighborSignal(footprint.partPos(pos, facing, part))) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Comparator output: stored energy for generators and the battery box; for processors, how full
	 * their input, output and byproduct slots are (as vanilla containers do). Upgrade slots don't count.
	 */
	public int comparatorSignal() {
		if (tanks != null) {
			return tanks.comparatorSignal();
		}
		if (reservoir != null) {
			return StorageUtil.getRedstoneSignal(reservoir);
		}
		if (!kind.isProcessor()) {
			long capacity = energy.getCapacity();
			if (capacity <= 0 || energy.getAmount() <= 0) {
				return 0;
			}
			return 1 + (int) (energy.getAmount() * 14 / capacity);
		}
		float fill = 0;
		for (int slot = 0; slot < kind.slots; slot++) {
			ItemStack stack = items.get(slot);
			if (!stack.isEmpty()) {
				fill += stack.getCount() / (float) Math.min(getMaxStackSize(), stack.getMaxStackSize());
			}
		}
		if (fill <= 0) {
			return 0;
		}
		return 1 + (int) (fill / kind.slots * 14);
	}

	/**
	 * A matched recipe: what it makes, how long it takes, how many items it takes from each input slot
	 * and what else it may make.
	 */
	private record Result(ItemStack stack, int ticks, int[] take, List<MachineRecipe.Byproduct> byproducts) {
		Result(ItemStack stack, int ticks, int[] take) {
			this(stack, ticks, take, List.of());
		}
	}

	private static final int[] TAKE_ONE = {1};

	private Optional<Result> findResult(ServerLevel level) {
		if (kind.isMultiInput()) {
			List<ItemStack> inputs = items.subList(0, kind.outputSlot());
			return MachineRecipes.findMulti(level, kind, inputs).map(match -> new Result(
					match.recipe().output().create(), match.recipe().time(), match.take()));
		}
		ItemStack input = items.get(0);
		if (input.isEmpty()) {
			return Optional.empty();
		}
		if (kind == MachineKind.ELECTRIC_FURNACE) {
			SingleRecipeInput recipeInput = new SingleRecipeInput(input);
			return SMELTING.getRecipeFor(recipeInput, level)
					.map(holder -> new Result(holder.value().assemble(recipeInput), MachineKind.ELECTRIC_FURNACE_TICKS, TAKE_ONE));
		}
		return MachineRecipes.find(level, kind, input)
				.map(recipe -> new Result(recipe.output().create(), recipe.time(), TAKE_ONE, recipe.byproducts()));
	}

	/** Whether every byproduct this operation might make has room, so none is ever lost. */
	private boolean byproductsFit(List<MachineRecipe.Byproduct> byproducts) {
		for (MachineRecipe.Byproduct byproduct : byproducts) {
			if (byproduct.enabled() && byproductSlotFor(byproduct.result().create()) < 0) {
				return false;
			}
		}
		return true;
	}

	/** The first byproduct slot that can take all of {@code stack}, or -1. */
	private int byproductSlotFor(ItemStack stack) {
		for (int slot = kind.outputSlot() + 1; slot < kind.slots; slot++) {
			ItemStack held = items.get(slot);
			if (held.isEmpty() || (ItemStack.isSameItemSameComponents(held, stack)
					&& held.getCount() + stack.getCount() <= Math.min(getMaxStackSize(), held.getMaxStackSize()))) {
				return slot;
			}
		}
		return -1;
	}

	private void addByproduct(ItemStack stack) {
		int slot = byproductSlotFor(stack);
		if (slot < 0) {
			return; // Unreachable: byproductsFit checked before the operation finished.
		}
		if (items.get(slot).isEmpty()) {
			items.set(slot, stack);
		} else {
			items.get(slot).grow(stack.getCount());
		}
	}

	private boolean canOutput(ItemStack result) {
		ItemStack output = items.get(kind.outputSlot());
		if (output.isEmpty()) {
			return true;
		}
		return ItemStack.isSameItemSameComponents(output, result)
				&& output.getCount() + result.getCount() <= Math.min(getMaxStackSize(), output.getMaxStackSize());
	}

	// ------------------------------------------------------------------ inventory

	@Override
	protected Component getDefaultName() {
		return Component.translatable("container.jugcraft." + kind.id);
	}

	@Override
	protected NonNullList<ItemStack> getItems() {
		return items;
	}

	@Override
	protected void setItems(NonNullList<ItemStack> items) {
		this.items = items;
	}

	@Override
	public int getContainerSize() {
		return items.size();
	}

	@Override
	public void setItem(int slot, ItemStack stack) {
		// A different input restarts processing, so progress cannot carry over to another recipe.
		if ((kind.isProcessor() || kind.isFluidProcessor()) && slot < kind.outputSlot()
				&& !ItemStack.isSameItemSameComponents(items.get(slot), stack)) {
			progress = 0;
		}
		super.setItem(slot, stack);
	}

	@Override
	public boolean canPlaceItem(int slot, ItemStack stack) {
		if (kind.isFluidProcessor()) {
			// Only the server knows the recipes; on the client the menu's slots accept anything and the server decides.
			return slot < kind.outputSlot() && (!(level instanceof ServerLevel server)
					|| FluidRecipes.usesItem(server.getServer(), kind, slot, stack));
		}
		if (kind == MachineKind.COAL_GENERATOR) {
			return GeneratorFuels.burnTicks(stack) > 0;
		}
		if (kind.isBoiler()) {
			return switch (slot) {
				case SLOT_FUEL -> GeneratorFuels.steamBurnTicks(stack) > 0;
				case SLOT_WATER_IN -> stack.is(Items.WATER_BUCKET);
				default -> false;
			};
		}
		if (kind.isMultiInput()) {
			// Only the server knows the recipes; on the client the menu's slots accept anything and the server decides.
			return slot < kind.outputSlot() && (!(level instanceof ServerLevel server)
					|| MachineRecipes.isMultiIngredient(server.getServer(), kind, stack));
		}
		if (slot >= kind.slots) {
			return MachineUpgrades.isUpgrade(stack); // Upgrade slots: menus only; no face exposes them.
		}
		return kind.isProcessor() && slot < kind.outputSlot();
	}

	@Override
	public int[] getSlotsForFace(Direction side) {
		if (kind.isFluidProcessor()) {
			int[] all = new int[kind.slots];
			for (int slot = 0; slot < all.length; slot++) {
				all[slot] = slot;
			}
			return all;
		}
		if (kind == MachineKind.COAL_GENERATOR) {
			return INPUT;
		}
		if (kind.isBoiler()) {
			return switch (side) {
				case UP -> STEAM_TOP;
				case DOWN -> STEAM_BOTTOM;
				default -> STEAM_SIDES;
			};
		}
		if (!kind.isProcessor()) {
			return NO_SLOTS;
		}
		SideConfig.Mode mode = sides.mode(side, getBlockState().getValue(MachineBlock.FACING));
		int inputs = mode.input() ? kind.outputSlot() : 0;
		int outputs = mode.output() ? kind.slots - kind.outputSlot() : 0;
		int[] slots = new int[inputs + outputs];
		for (int slot = 0; slot < inputs; slot++) {
			slots[slot] = slot;
		}
		for (int i = 0; i < outputs; i++) {
			slots[inputs + i] = kind.outputSlot() + i;
		}
		return slots;
	}

	@Override
	public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) {
		if (kind.isProcessor() && side != null && !sides.mode(side, getBlockState().getValue(MachineBlock.FACING)).input()) {
			return false;
		}
		if (kind == MachineKind.AUTO_CRAFTER && slot < GRID) {
			// Automation only tops up pattern slots already holding that item; players set the pattern by hand.
			ItemStack held = items.get(slot);
			return !held.isEmpty() && ItemStack.isSameItemSameComponents(held, stack);
		}
		return canPlaceItem(slot, stack);
	}

	@Override
	public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
		if (kind.isFluidProcessor()) {
			return slot >= kind.outputSlot() && slot < kind.slots;
		}
		if (kind.isBoiler()) {
			return slot == SLOT_BUCKET_OUT;
		}
		return kind.isProcessor() && slot >= kind.outputSlot() && slot < kind.slots
				&& sides.mode(side, getBlockState().getValue(MachineBlock.FACING)).output();
	}

	// ------------------------------------------------------------------ menu

	@Override
	protected AbstractContainerMenu createMenu(int containerId, Inventory inventory) {
		return new MachineMenu(JugcraftMachines.menuType(kind), kind, containerId, inventory, this, data);
	}

	@Override
	public BlockPos getScreenOpeningData(ServerPlayer player) {
		return worldPosition;
	}

	// ------------------------------------------------------------------ saving

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		items = NonNullList.withSize(kind.containerSize(), ItemStack.EMPTY);
		ContainerHelper.loadAllItems(input, items);
		energy.setAmount(input.getLong("energy").orElse(0L));
		progress = input.getInt("progress").orElse(0);
		maxProgress = input.getInt("max_progress").orElse(0);
		burn = input.getInt("burn").orElse(0);
		maxBurn = input.getInt("max_burn").orElse(0);
		tank = input.getInt("water").orElse(0);
		sides.unpack(input.getInt("sides").orElse(SideConfig.defaults()));
		cursor = input.getInt("cursor").orElse(0);
		if (reservoir != null) {
			reservoir.readValue(input);
		}
		if (tanks != null) {
			tanks.load(input);
		}
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		ContainerHelper.saveAllItems(output, items);
		output.putLong("energy", energy.getAmount());
		output.putInt("progress", progress);
		output.putInt("max_progress", maxProgress);
		output.putInt("burn", burn);
		output.putInt("max_burn", maxBurn);
		output.putInt("water", tank);
		output.putInt("sides", sides.pack());
		if (kind == MachineKind.ORE_DRILL || kind == MachineKind.CROP_HARVESTER) {
			output.putInt("cursor", cursor);
		}
		if (reservoir != null) {
			reservoir.writeValue(output);
		}
		if (tanks != null) {
			tanks.save(output);
		}
	}
}
