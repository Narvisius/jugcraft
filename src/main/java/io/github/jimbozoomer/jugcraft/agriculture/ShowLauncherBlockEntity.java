package io.github.jimbozoomer.jugcraft.agriculture;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.Containers;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.FireworkRocketEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * The Show Launcher's nine tubes, three rows of three, each holding up to {@value #TUBE_CAPACITY} rockets of one kind
 * (spooky fireworks or vanilla firework rockets). A show fires them in the launcher's {@link ShowLauncherBlock.Mode}:
 * one every {@value #SEQUENCE_TICKS} ticks round the tubes in turn, a row of three every {@value #VOLLEY_TICKS} ticks,
 * or one from every tube at once. A show runs until the tubes are empty or it is stopped. Rockets leave the tubes fanned
 * out from the middle. It is a container: hoppers may load rockets from any side and unload them from below.
 */
public class ShowLauncherBlockEntity extends BlockEntity implements WorldlyContainer {
	public static final int TUBES = 9;
	public static final int TUBE_CAPACITY = 16;
	public static final int SEQUENCE_TICKS = 10;
	public static final int VOLLEY_TICKS = 20;
	/** How far a rocket leans out from the middle tube, per tube (blocks per tick); a vanilla rocket's, which speeds up sideways. */
	public static final double LEAN = 0.1;
	public static final double VANILLA_LEAN = 0.003;
	private static final int[] SLOTS = {0, 1, 2, 3, 4, 5, 6, 7, 8};

	private final NonNullList<ItemStack> tubes = NonNullList.withSize(TUBES, ItemStack.EMPTY);
	private boolean running;
	private int next;
	private int wait;

	public ShowLauncherBlockEntity(BlockPos pos, BlockState state) {
		super(JugcraftAgriculture.SHOW_LAUNCHER_ENTITY, pos, state);
	}

	public boolean running() {
		return running;
	}

	public ItemStack tube(int tube) {
		return tubes.get(tube);
	}

	/** How many tubes hold a rocket. */
	public int loadedTubes() {
		int count = 0;
		for (ItemStack tube : tubes) {
			count += tube.isEmpty() ? 0 : 1;
		}
		return count;
	}

	/** How many rockets are in all the tubes. */
	public int rockets() {
		int count = 0;
		for (ItemStack tube : tubes) {
			count += tube.getCount();
		}
		return count;
	}

	/** Loads as many of {@code stack} as there is room for (matching tubes first, then empty ones); returns how many. */
	public int load(ItemStack stack) {
		if (!SpookyFireworkItem.rocket(stack)) {
			return 0;
		}
		int left = stack.getCount();
		for (int pass = 0; pass < 2 && left > 0; pass++) {
			for (int i = 0; i < TUBES && left > 0; i++) {
				ItemStack tube = tubes.get(i);
				if (pass == 0 && !tube.isEmpty() && ItemStack.isSameItemSameComponents(tube, stack)) {
					int moved = Math.min(left, TUBE_CAPACITY - tube.getCount());
					tube.grow(moved);
					left -= moved;
				} else if (pass == 1 && tube.isEmpty()) {
					int moved = Math.min(left, TUBE_CAPACITY);
					tubes.set(i, stack.copyWithCount(moved));
					left -= moved;
				}
			}
		}
		int loaded = stack.getCount() - left;
		if (loaded > 0) {
			changed();
		}
		return loaded;
	}

	/** Starts a show, if there is anything to fire. */
	public boolean start() {
		if (rockets() == 0) {
			return false;
		}
		running = true;
		next = 0;
		wait = 0;
		changed();
		return true;
	}

	public void stop() {
		running = false;
		changed();
	}

	public void serverTick(ServerLevel level) {
		if (!running) {
			return;
		}
		if (wait > 0) {
			wait--;
			return;
		}
		ShowLauncherBlock.Mode mode = getBlockState().getValue(ShowLauncherBlock.MODE);
		switch (mode) {
			case SEQUENCE -> {
				for (int i = 0; i < TUBES; i++) {
					int tube = (next + i) % TUBES;
					if (!tubes.get(tube).isEmpty()) {
						fire(level, tube);
						next = tube + 1;
						break;
					}
				}
				wait = SEQUENCE_TICKS - 1;
			}
			case VOLLEY -> {
				for (int i = 0; i < 3; i++) {
					int row = (next + i) % 3;
					boolean fired = false;
					for (int column = 0; column < 3; column++) {
						if (!tubes.get(row * 3 + column).isEmpty()) {
							fire(level, row * 3 + column);
							fired = true;
						}
					}
					if (fired) {
						next = row + 1;
						break;
					}
				}
				wait = VOLLEY_TICKS - 1;
			}
			case FINALE -> {
				for (int tube = 0; tube < TUBES; tube++) {
					if (!tubes.get(tube).isEmpty()) {
						fire(level, tube);
					}
				}
				running = false;
			}
		}
		if (rockets() == 0) {
			running = false;
		}
		changed();
	}

	/** Fires one rocket from {@code tube}, leaning out from the middle of the launcher. */
	private void fire(ServerLevel level, int tube) {
		ItemStack rocket = tubes.get(tube).split(1);
		Direction facing = getBlockState().getValue(ShowLauncherBlock.FACING);
		Direction across = facing.getClockWise();
		int column = tube % 3 - 1;
		int row = 1 - tube / 3;
		double x = worldPosition.getX() + 0.5 + (across.getStepX() * column + facing.getStepX() * row) * 0.3125;
		double z = worldPosition.getZ() + 0.5 + (across.getStepZ() * column + facing.getStepZ() * row) * 0.3125;
		double y = worldPosition.getY() + 1.0;
		double leanX = across.getStepX() * column + facing.getStepX() * row;
		double leanZ = across.getStepZ() * column + facing.getStepZ() * row;
		if (rocket.getItem() instanceof SpookyFireworkItem) {
			level.addFreshEntity(new SpookyRocket(level, x, y, z, rocket, new Vec3(leanX * LEAN, 0.05, leanZ * LEAN), false));
		} else {
			FireworkRocketEntity vanilla = new FireworkRocketEntity(level, x, y, z, rocket);
			vanilla.setDeltaMovement(leanX * VANILLA_LEAN, 0.05, leanZ * VANILLA_LEAN);
			level.addFreshEntity(vanilla);
		}
	}

	@Override
	public void preRemoveSideEffects(BlockPos pos, BlockState state) {
		if (level != null) {
			Containers.dropContents(level, pos, tubes);
		}
	}

	private void changed() {
		setChanged();
		if (level != null) {
			level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
			level.updateNeighbourForOutputSignal(worldPosition, getBlockState().getBlock());
		}
	}

	// ---------------------------------------------------------------- container (hoppers)

	@Override
	public int getContainerSize() {
		return TUBES;
	}

	@Override
	public boolean isEmpty() {
		return rockets() == 0;
	}

	@Override
	public ItemStack getItem(int slot) {
		return tubes.get(slot);
	}

	@Override
	public ItemStack removeItem(int slot, int amount) {
		ItemStack taken = ContainerHelper.removeItem(tubes, slot, amount);
		if (!taken.isEmpty()) {
			changed();
		}
		return taken;
	}

	@Override
	public ItemStack removeItemNoUpdate(int slot) {
		return ContainerHelper.takeItem(tubes, slot);
	}

	@Override
	public void setItem(int slot, ItemStack stack) {
		tubes.set(slot, stack);
		stack.limitSize(getMaxStackSize(stack));
		changed();
	}

	@Override
	public int getMaxStackSize() {
		return TUBE_CAPACITY;
	}

	@Override
	public boolean canPlaceItem(int slot, ItemStack stack) {
		ItemStack tube = tubes.get(slot);
		return SpookyFireworkItem.rocket(stack) && (tube.isEmpty() || ItemStack.isSameItemSameComponents(tube, stack));
	}

	@Override
	public boolean stillValid(Player player) {
		return Container.stillValidBlockEntity(this, player);
	}

	@Override
	public void clearContent() {
		tubes.clear();
		changed();
	}

	@Override
	public int[] getSlotsForFace(Direction side) {
		return SLOTS;
	}

	@Override
	public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) {
		return canPlaceItem(slot, stack);
	}

	@Override
	public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
		return side == Direction.DOWN;
	}

	// ---------------------------------------------------------------- saving

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		List<ItemStack> saved = input.read("tubes", ItemStack.OPTIONAL_CODEC.listOf()).orElse(List.of());
		for (int i = 0; i < TUBES; i++) {
			tubes.set(i, i < saved.size() ? saved.get(i) : ItemStack.EMPTY);
		}
		running = input.getBooleanOr("running", false);
		next = input.getIntOr("next", 0);
		wait = input.getIntOr("wait", 0);
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		output.store("tubes", ItemStack.OPTIONAL_CODEC.listOf(), new ArrayList<>(tubes));
		output.putBoolean("running", running);
		output.putInt("next", next);
		output.putInt("wait", wait);
	}

	@Override
	public Packet<ClientGamePacketListener> getUpdatePacket() {
		return ClientboundBlockEntityDataPacket.create(this);
	}

	@Override
	public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
		return saveCustomOnly(registries);
	}
}
