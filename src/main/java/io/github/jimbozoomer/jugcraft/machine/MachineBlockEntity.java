package io.github.jimbozoomer.jugcraft.machine;

import io.github.jimbozoomer.jugcraft.energy.EnergyNetworks;
import io.github.jimbozoomer.jugcraft.energy.EnergyStorage;
import io.github.jimbozoomer.jugcraft.energy.SimpleEnergyStorage;
import io.github.jimbozoomer.jugcraft.fluid.FluidNetworks;
import io.github.jimbozoomer.jugcraft.logistics.ItemNetworks;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import net.fabricmc.fabric.api.menu.v1.ExtendedMenuProvider;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.item.ContainerStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.StoragePreconditions;
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
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.item.crafting.SmeltingRecipe;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

/**
 * State and behavior for every Jugcraft machine. Each machine holds its own internal
 * battery ({@link #energy}); what it does each tick depends on its {@link MachineKind}.
 * All logic runs on the server; clients only see synced {@link ContainerData}.
 */
public class MachineBlockEntity extends BaseContainerBlockEntity implements WorldlyContainer, ExtendedMenuProvider<BlockPos> {
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
	public static final int DATA_COUNT = 11;
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

	private final MachineKind kind;
	private final SimpleEnergyStorage energy;
	private NonNullList<ItemStack> items;
	private int progress;
	private int maxProgress;
	private int burn;
	private int maxBurn;
	/** Arc furnace: structure complete. Wind turbine: rotor has room to turn. */
	private boolean formed;
	/** mB in the machine's tank: water for the steam generator and ore washer, lava for the geothermal generator. Saved as "water". */
	private int tank;
	/** Processors: which faces take input or give output, and whether results are pushed out. */
	private final SideConfig sides = new SideConfig();
	/** Wind turbine: whether the rotor check has run since loading. */
	private boolean windChecked;

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

	public MachineBlockEntity(BlockPos pos, BlockState state) {
		super(JugcraftMachines.MACHINE_ENTITY, pos, state);
		this.kind = ((MachineBlock) state.getBlock()).kind();
		this.items = NonNullList.withSize(kind.slots, ItemStack.EMPTY);
		// Producers only give energy out; consumers only take it in; the battery box does both.
		long insert = kind.isGenerator() ? 0 : kind.maxInput;
		long extract = kind.isProcessor() ? 0 : kind.maxOutput;
		this.energy = new SimpleEnergyStorage(kind.capacity, insert, extract, this::setChanged);
		this.inlet = switch (kind) {
			case STEAM_GENERATOR -> new TankInlet(Fluids.WATER, MachineKind.STEAM_TANK);
			case GEOTHERMAL_GENERATOR -> new TankInlet(Fluids.LAVA, MachineKind.GEOTHERMAL_TANK);
			case ORE_WASHER -> new TankInlet(Fluids.WATER, MachineKind.WASHER_TANK);
			default -> null;
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
		return inlet;
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

	/** The storage exposed on a side; the battery box only discharges through its front. */
	public @Nullable EnergyStorage energyFor(@Nullable Direction side) {
		if (kind != MachineKind.BATTERY_BOX || side == null) {
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
			case BATTERY_BOX -> tickBattery(level, pos, state);
			case GEOTHERMAL_GENERATOR -> tickGeothermal(level, pos, state);
			case WIND_TURBINE -> tickWind(level, pos, state);
			default -> tickProcessor(level, pos, state);
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
	 * Output grows with height above sea level and with rain or thunder. The rotor needs the blocks
	 * beside and above the top of the turbine clear; that is checked every few seconds, not every tick.
	 */
	private boolean tickWind(ServerLevel level, BlockPos pos, BlockState state) {
		Direction facing = state.getValue(MachineBlock.FACING);
		BlockPos top = kind.footprint().partPos(pos, facing, 2);
		if (!windChecked || level.getGameTime() % MachineKind.WIND_CHECK_INTERVAL == 0) {
			windChecked = true;
			formed = level.isEmptyBlock(top.above()) && level.isEmptyBlock(top.relative(facing.getClockWise()))
					&& level.isEmptyBlock(top.relative(facing.getCounterClockWise()));
		}
		int rate = 0;
		if (formed) {
			int height = Math.max(0, top.getY() - level.getSeaLevel());
			rate = Math.min(MachineKind.WIND_MAX_PER_TICK, MachineKind.WIND_BASE_PER_TICK + height / 4);
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

	private void refillWater(ServerLevel level, BlockPos pos) {
		if (tank < MachineKind.STEAM_TANK && level.getFluidState(pos.below()).isSourceOfType(Fluids.WATER)) {
			tank = Math.min(MachineKind.STEAM_TANK, tank + MachineKind.STEAM_SOURCE_REFILL);
			setChanged();
		}
		ItemStack bucket = items.get(SLOT_WATER_IN);
		ItemStack empties = items.get(SLOT_BUCKET_OUT);
		boolean emptySpace = empties.isEmpty() || (empties.is(Items.BUCKET) && empties.getCount() < empties.getMaxStackSize());
		if (bucket.is(Items.WATER_BUCKET) && tank <= MachineKind.STEAM_TANK - 1000 && emptySpace) {
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

	private boolean tickBattery(ServerLevel level, BlockPos pos, BlockState state) {
		Direction front = state.getValue(MachineBlock.FACING);
		EnergyNetworks.pushToNeighbors(level, pos, energy, kind.maxOutput, List.of(front));
		return false;
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

		maxProgress = result.get().ticks();
		if (energy.getAmount() < kind.usePerTick) {
			return false; // Keeps progress; resumes when power returns.
		}
		energy.setAmount(energy.getAmount() - kind.usePerTick);
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
		if (kind.isProcessor() && slot < kind.outputSlot() && !ItemStack.isSameItemSameComponents(items.get(slot), stack)) {
			progress = 0;
		}
		super.setItem(slot, stack);
	}

	@Override
	public boolean canPlaceItem(int slot, ItemStack stack) {
		if (kind == MachineKind.COAL_GENERATOR) {
			return GeneratorFuels.burnTicks(stack) > 0;
		}
		if (kind == MachineKind.STEAM_GENERATOR) {
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
		return kind.isProcessor() && slot < kind.outputSlot();
	}

	@Override
	public int[] getSlotsForFace(Direction side) {
		if (kind == MachineKind.COAL_GENERATOR) {
			return INPUT;
		}
		if (kind == MachineKind.STEAM_GENERATOR) {
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
		return canPlaceItem(slot, stack);
	}

	@Override
	public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
		if (kind == MachineKind.STEAM_GENERATOR) {
			return slot == SLOT_BUCKET_OUT;
		}
		return kind.isProcessor() && slot >= kind.outputSlot()
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
		items = NonNullList.withSize(kind.slots, ItemStack.EMPTY);
		ContainerHelper.loadAllItems(input, items);
		energy.setAmount(input.getLong("energy").orElse(0L));
		progress = input.getInt("progress").orElse(0);
		maxProgress = input.getInt("max_progress").orElse(0);
		burn = input.getInt("burn").orElse(0);
		maxBurn = input.getInt("max_burn").orElse(0);
		tank = input.getInt("water").orElse(0);
		sides.unpack(input.getInt("sides").orElse(SideConfig.defaults()));
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
	}
}
