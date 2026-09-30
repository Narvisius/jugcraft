package io.github.jimbozoomer.jugcraft.machine;

import io.github.jimbozoomer.jugcraft.energy.EnergyNetworks;
import io.github.jimbozoomer.jugcraft.energy.EnergyStorage;
import io.github.jimbozoomer.jugcraft.energy.SimpleEnergyStorage;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import net.fabricmc.fabric.api.menu.v1.ExtendedMenuProvider;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
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
	public static final int DATA_WATER = 9;
	public static final int DATA_COUNT = 10;

	/** Steam generator slots. */
	public static final int SLOT_FUEL = 0;
	public static final int SLOT_WATER_IN = 1;
	public static final int SLOT_BUCKET_OUT = 2;

	private static final int[] NO_SLOTS = {};
	private static final int[] INPUT = {0};
	private static final int[] OUTPUT = {1};
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
	private boolean formed;
	private int water;

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
				case DATA_WATER -> water;
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
	}

	public MachineKind kind() {
		return kind;
	}

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
			case ELECTRIC_FURNACE, CRUSHER, ARC_FURNACE -> tickProcessor(level, pos, state);
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

	private boolean tickSteam(ServerLevel level, BlockPos pos) {
		refillWater(level, pos);
		boolean room = energy.getAmount() < energy.getCapacity();
		boolean hasWater = water >= MachineKind.STEAM_WATER_PER_TICK;
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
			water -= MachineKind.STEAM_WATER_PER_TICK;
			energy.setAmount(energy.getAmount() + MachineKind.STEAM_PER_TICK);
			setChanged();
		}
		EnergyNetworks.pushToNeighbors(level, pos, energy, kind.maxOutput, EnumSet.allOf(Direction.class));
		return boiling;
	}

	private void refillWater(ServerLevel level, BlockPos pos) {
		if (water < MachineKind.STEAM_TANK && level.getFluidState(pos.below()).isSourceOfType(Fluids.WATER)) {
			water = Math.min(MachineKind.STEAM_TANK, water + MachineKind.STEAM_SOURCE_REFILL);
			setChanged();
		}
		ItemStack bucket = items.get(SLOT_WATER_IN);
		ItemStack empties = items.get(SLOT_BUCKET_OUT);
		boolean emptySpace = empties.isEmpty() || (empties.is(Items.BUCKET) && empties.getCount() < empties.getMaxStackSize());
		if (bucket.is(Items.WATER_BUCKET) && water <= MachineKind.STEAM_TANK - 1000 && emptySpace) {
			bucket.shrink(1);
			if (empties.isEmpty()) {
				items.set(SLOT_BUCKET_OUT, new ItemStack(Items.BUCKET));
			} else {
				empties.grow(1);
			}
			water += 1000;
			setChanged();
		}
	}

	private boolean tickBattery(ServerLevel level, BlockPos pos, BlockState state) {
		Direction front = state.getValue(MachineBlock.FACING);
		EnergyNetworks.pushToNeighbors(level, pos, energy, kind.maxOutput, List.of(front));
		return false;
	}

	private boolean tickProcessor(ServerLevel level, BlockPos pos, BlockState state) {
		if (kind == MachineKind.ARC_FURNACE && level.getGameTime() % 20 == 0) {
			formed = ArcFurnaceStructure.isFormed(level, pos, state.getValue(MachineBlock.FACING),
					JugcraftMachines.ARC_FURNACE_CASING);
		}
		if (kind == MachineKind.ARC_FURNACE && !formed) {
			progress = 0;
			return false;
		}

		ItemStack input = items.get(0);
		Optional<Result> result = input.isEmpty() ? Optional.empty() : findResult(level, input);
		if (result.isEmpty() || !canOutput(result.get().stack())) {
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
			ItemStack output = items.get(1);
			if (output.isEmpty()) {
				items.set(1, result.get().stack().copy());
			} else {
				output.grow(result.get().stack().getCount());
			}
			input.shrink(1);
		}
		setChanged();
		return true;
	}

	private record Result(ItemStack stack, int ticks) {
	}

	private Optional<Result> findResult(ServerLevel level, ItemStack input) {
		if (kind == MachineKind.ELECTRIC_FURNACE) {
			SingleRecipeInput recipeInput = new SingleRecipeInput(input);
			return SMELTING.getRecipeFor(recipeInput, level)
					.map(holder -> new Result(holder.value().assemble(recipeInput), MachineKind.ELECTRIC_FURNACE_TICKS));
		}
		return MachineRecipes.find(kind, input).map(recipe -> new Result(recipe.output(), recipe.ticks()));
	}

	private boolean canOutput(ItemStack result) {
		ItemStack output = items.get(1);
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
		if (kind.isProcessor() && slot == 0 && !ItemStack.isSameItemSameComponents(items.get(0), stack)) {
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
		return kind.isProcessor() && slot == 0;
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
		return side == Direction.DOWN ? OUTPUT : INPUT;
	}

	@Override
	public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) {
		return canPlaceItem(slot, stack);
	}

	@Override
	public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
		if (kind == MachineKind.STEAM_GENERATOR) {
			return slot == SLOT_BUCKET_OUT;
		}
		return kind.isProcessor() && slot == 1;
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
		water = input.getInt("water").orElse(0);
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
		output.putInt("water", water);
	}
}
