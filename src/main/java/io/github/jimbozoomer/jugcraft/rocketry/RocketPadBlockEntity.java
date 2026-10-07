package io.github.jimbozoomer.jugcraft.rocketry;

import java.util.ArrayList;
import java.util.List;
import net.fabricmc.fabric.api.menu.v1.ExtendedMenuProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.projectile.FireworkRocketEntity;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.Fireworks;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

/**
 * A rocket pad (batch 39, docs/features/rocket-post.md): nine cargo slots, a slot for a delivery rocket and one for a
 * flight plan naming the pad to deliver to. {@link #launch} (the screen's button or a redstone pulse) loads the cargo
 * into the rocket and sends it off; {@link RocketPost} lands it at the target pad, whose cargo slots take it in.
 * Hoppers and pipes load cargo (and rockets) from the top and sides and unload cargo from the bottom.
 */
public class RocketPadBlockEntity extends BaseContainerBlockEntity implements WorldlyContainer, ExtendedMenuProvider<BlockPos> {
	public static final int CARGO = 9;
	public static final int ROCKET = CARGO;
	public static final int PLAN = CARGO + 1;
	public static final int SLOTS = CARGO + 2;
	public static final int DATA_RESULT = 0;
	public static final int DATA_COUNT = 1;

	private static final int[] CARGO_SLOTS = {0, 1, 2, 3, 4, 5, 6, 7, 8};
	private static final int[] LOAD_SLOTS = {0, 1, 2, 3, 4, 5, 6, 7, 8, ROCKET};

	/** What the last launch attempt came to, shown on the pad's screen. */
	public enum Result {
		NONE, LAUNCHED, NO_ROCKET, NO_PLAN, NO_CARGO, SAME_PAD, OTHER_DIMENSION, TOO_FAR, NO_PAD, NO_SKY;

		public Component message() {
			return Component.translatable("screen.jugcraft.rocket_pad." + name().toLowerCase(java.util.Locale.ROOT));
		}
	}

	private NonNullList<ItemStack> items = NonNullList.withSize(SLOTS, ItemStack.EMPTY);
	private Result result = Result.NONE;

	private final ContainerData data = new ContainerData() {
		@Override
		public int get(int index) {
			return index == DATA_RESULT ? result.ordinal() : 0;
		}

		@Override
		public void set(int index, int value) {
		}

		@Override
		public int getCount() {
			return DATA_COUNT;
		}
	};

	public RocketPadBlockEntity(BlockPos pos, BlockState state) {
		super(JugcraftRocketry.ROCKET_PAD_ENTITY, pos, state);
	}

	/** Whether nothing above the pad blocks a rocket going up (or coming down). */
	public static boolean openSky(ServerLevel level, BlockPos pos) {
		return level.getHeight(Heightmap.Types.MOTION_BLOCKING, pos.getX(), pos.getZ()) <= pos.getY() + 1;
	}

	/** Loads the cargo into the rocket and sends it to the flight plan's pad, or says why it cannot. */
	public Result launch(ServerLevel level) {
		result = check(level);
		if (result == Result.LAUNCHED) {
			GlobalPos target = FlightPlanItem.target(items.get(PLAN));
			List<ItemStack> cargo = new ArrayList<>();
			for (int slot = 0; slot < CARGO; slot++) {
				if (!items.get(slot).isEmpty()) {
					cargo.add(items.get(slot));
					items.set(slot, ItemStack.EMPTY);
				}
			}
			items.get(ROCKET).shrink(1);
			RocketPost.send(level.getServer(), target.dimension(), target.pos(), cargo,
					Math.sqrt(target.pos().distSqr(worldPosition)));
			ItemStack firework = new ItemStack(Items.FIREWORK_ROCKET);
			firework.set(DataComponents.FIREWORKS, new Fireworks(2, List.of()));
			level.addFreshEntity(new FireworkRocketEntity(level, worldPosition.getX() + 0.5, worldPosition.getY() + 1.0,
					worldPosition.getZ() + 0.5, firework));
			level.playSound(null, worldPosition, SoundEvents.FIREWORK_ROCKET_LAUNCH, SoundSource.BLOCKS, 2.0F, 0.6F);
		}
		setChanged();
		return result;
	}

	private Result check(ServerLevel level) {
		if (!items.get(ROCKET).is(JugcraftRocketry.DELIVERY_ROCKET)) {
			return Result.NO_ROCKET;
		}
		GlobalPos target = FlightPlanItem.target(items.get(PLAN));
		if (target == null) {
			return Result.NO_PLAN;
		}
		boolean any = false;
		for (int slot = 0; slot < CARGO; slot++) {
			any |= !items.get(slot).isEmpty();
		}
		if (!any) {
			return Result.NO_CARGO;
		}
		if (!target.dimension().equals(level.dimension())) {
			return Result.OTHER_DIMENSION;
		}
		if (target.pos().equals(worldPosition)) {
			return Result.SAME_PAD;
		}
		if (target.pos().distSqr(worldPosition) > (double) RocketPost.RANGE * RocketPost.RANGE) {
			return Result.TOO_FAR;
		}
		// A loaded target must still be a pad; an unloaded one is checked when the rocket lands.
		if (level.isLoaded(target.pos()) && !(level.getBlockEntity(target.pos()) instanceof RocketPadBlockEntity)) {
			return Result.NO_PAD;
		}
		if (!openSky(level, worldPosition)) {
			return Result.NO_SKY;
		}
		return Result.LAUNCHED;
	}

	public Result lastResult() {
		return result;
	}

	/** Puts landed cargo into the cargo slots; returns what did not fit. */
	public ItemStack receive(ItemStack stack) {
		for (int slot = 0; slot < CARGO && !stack.isEmpty(); slot++) {
			ItemStack held = items.get(slot);
			if (!held.isEmpty() && ItemStack.isSameItemSameComponents(held, stack)) {
				int moved = Math.min(stack.getCount(), held.getMaxStackSize() - held.getCount());
				if (moved > 0) {
					held.grow(moved);
					stack.shrink(moved);
				}
			}
		}
		for (int slot = 0; slot < CARGO && !stack.isEmpty(); slot++) {
			if (items.get(slot).isEmpty()) {
				items.set(slot, stack.split(stack.getMaxStackSize()));
			}
		}
		setChanged();
		return stack;
	}

	// ---------------------------------------------------------------- inventory

	@Override
	protected Component getDefaultName() {
		return Component.translatable("container.jugcraft.rocket_pad");
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
		return SLOTS;
	}

	@Override
	public boolean canPlaceItem(int slot, ItemStack stack) {
		if (slot == ROCKET) {
			return stack.is(JugcraftRocketry.DELIVERY_ROCKET);
		}
		if (slot == PLAN) {
			return stack.is(JugcraftRocketry.FLIGHT_PLAN);
		}
		return true;
	}

	@Override
	public int[] getSlotsForFace(Direction side) {
		return side == Direction.DOWN ? CARGO_SLOTS : LOAD_SLOTS;
	}

	/** Rockets go only into the rocket slot, so a hopper of rockets does not fill the cargo with them. */
	@Override
	public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) {
		if (side == Direction.DOWN) {
			return false;
		}
		return slot == ROCKET ? canPlaceItem(slot, stack) : !stack.is(JugcraftRocketry.DELIVERY_ROCKET);
	}

	@Override
	public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
		return side == Direction.DOWN && slot < CARGO;
	}

	// ---------------------------------------------------------------- menu

	@Override
	protected AbstractContainerMenu createMenu(int containerId, Inventory inventory) {
		return new RocketPadMenu(containerId, inventory, this, data);
	}

	@Override
	public BlockPos getScreenOpeningData(ServerPlayer player) {
		return worldPosition;
	}

	// ---------------------------------------------------------------- saving

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		items = NonNullList.withSize(SLOTS, ItemStack.EMPTY);
		ContainerHelper.loadAllItems(input, items);
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		ContainerHelper.saveAllItems(output, items);
	}
}
