package io.github.jimbozoomer.jugcraft.concordance.courier;

import io.github.jimbozoomer.jugcraft.concordance.Saved;
import io.github.jimbozoomer.jugcraft.party.JugcraftParties;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.core.UUIDUtil;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.Containers;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

/**
 * The Courier Post (roadmap step 18): where requests are filed and delivered. Its owner's party files requests here;
 * Clockwork Porters bound to it fetch from the containers round it ({@value Couriers#SOURCE_RADIUS} blocks) into its
 * nine slots, which hoppers below may empty (nothing may be put in from outside: deliveries arrive only through the
 * ledger). Breaking it cancels the requests bound for it: their cargo is taken back to where it came from.
 */
public class CourierPostBlockEntity extends BlockEntity implements WorldlyContainer {
	public static final int SLOTS = 9;
	private static final int[] ALL = {0, 1, 2, 3, 4, 5, 6, 7, 8};

	private NonNullList<ItemStack> items = NonNullList.withSize(SLOTS, ItemStack.EMPTY);
	private @Nullable UUID owner;

	public CourierPostBlockEntity(BlockPos pos, BlockState state) {
		super(Couriers.POST_ENTITY, pos, state);
	}

	public @Nullable UUID owner() {
		return owner;
	}

	public void setOwner(@Nullable UUID owner) {
		this.owner = owner;
		setChanged();
	}

	/** Whether {@code player} (or their party) may file requests here and bind couriers to it. */
	public boolean mayUse(UUID player) {
		return owner == null || owner.equals(player) || JugcraftParties.sameParty(owner, player);
	}

	/** Broken: the requests bound for it are cancelled (cargo goes back), and what it holds drops once. */
	@Override
	public void preRemoveSideEffects(BlockPos pos, BlockState state) {
		if (level instanceof ServerLevel server) {
			CourierLedger.of(server.getServer()).destinationGone(Couriers.place(server, pos), server.getGameTime());
			Containers.dropContents(server, pos, this);
		}
	}

	// ---------------------------------------------------------------- hoppers below take deliveries out; nothing goes in from outside

	@Override
	public int[] getSlotsForFace(Direction side) {
		return side == Direction.DOWN ? ALL : new int[0];
	}

	@Override
	public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) {
		return false;
	}

	@Override
	public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
		return side == Direction.DOWN;
	}

	@Override
	public int getContainerSize() {
		return SLOTS;
	}

	@Override
	public boolean isEmpty() {
		return items.stream().allMatch(ItemStack::isEmpty);
	}

	@Override
	public ItemStack getItem(int slot) {
		return items.get(slot);
	}

	@Override
	public ItemStack removeItem(int slot, int count) {
		ItemStack taken = ContainerHelper.removeItem(items, slot, count);
		if (!taken.isEmpty()) {
			setChanged();
		}
		return taken;
	}

	@Override
	public ItemStack removeItemNoUpdate(int slot) {
		return ContainerHelper.takeItem(items, slot);
	}

	@Override
	public void setItem(int slot, ItemStack stack) {
		items.set(slot, stack);
		setChanged();
	}

	@Override
	public boolean stillValid(Player player) {
		return false;
	}

	@Override
	public void clearContent() {
		items.clear();
	}

	/** How many slots are empty (for Jade and the history line). */
	public int freeSlots() {
		int free = 0;
		for (ItemStack stack : items) {
			if (stack.isEmpty()) {
				free++;
			}
		}
		return free;
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		items = NonNullList.withSize(SLOTS, ItemStack.EMPTY);
		ContainerHelper.loadAllItems(input, items);
		owner = input.read("owner", UUIDUtil.CODEC).orElse(null);
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		Saved.stamp(output, 1);
		ContainerHelper.saveAllItems(output, items);
		if (owner != null) {
			output.store("owner", UUIDUtil.CODEC, owner);
		}
	}
}
