package io.github.jimbozoomer.jugcraft.concordance.garden;

import com.geckolib.animatable.GeoBlockEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.util.GeckoLibUtil;
import io.github.jimbozoomer.jugcraft.concordance.Authority;
import io.github.jimbozoomer.jugcraft.concordance.resource.Reservoir;
import io.github.jimbozoomer.jugcraft.concordance.resource.Transfers;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

/**
 * The Gleaner (roadmap step 14): the garden's collector. Every {@value #GLEAN_TICKS} ticks it harvests the nearest
 * ripe Greenwarden crop within {@value #REACH} blocks (and one above or below) into its {@value #SLOTS} slots, for
 * {@value #COST} Verdance a harvest, which it draws (up to {@value #DRAW} at a time, through the shared transfer rules)
 * from a Verdant Heart within {@value #HEART_REACH} blocks that its keeper's party may draw from. It harvests only
 * what all fits, never in a protected town, and never loads a chunk; hoppers and pipes take its harvest out.
 */
public class GleanerBlockEntity extends LivingDeviceBlockEntity implements GeoBlockEntity, WorldlyContainer {
	public static final int GLEAN_TICKS = 40;
	public static final int REACH = 3;
	public static final int SLOTS = 9;
	public static final int CAPACITY = 16;
	public static final int DRAW = 8;
	public static final int HEART_REACH = 4;
	public static final int COST = 1;
	private static final int[] ALL = IntStream.range(0, SLOTS).toArray();
	private static final UUID NO_ONE = new UUID(0L, 0L);
	private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("animation.gleaner.idle");
	private static final RawAnimation REACHING = RawAnimation.begin().thenLoop("animation.gleaner.reaching");

	private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
	private NonNullList<ItemStack> items = NonNullList.withSize(SLOTS, ItemStack.EMPTY);
	private long verdance;

	public GleanerBlockEntity(BlockPos pos, BlockState state) {
		super(Garden.GLEANER_ENTITY, pos, state);
	}

	public long verdance() {
		return verdance;
	}

	public Reservoir reservoir() {
		return new Reservoir(Garden.VERDANCE, verdance, CAPACITY);
	}

	void serverTick(ServerLevel level) {
		if (due(level, GLEAN_TICKS)) {
			glean(level);
		}
	}

	/** One pulse (or a test's): draw Verdance if short, then harvest one ripe crop; returns its status. */
	public String glean(ServerLevel level) {
		if (!Garden.enabled()) {
			status("disabled");
			return status;
		}
		if (!awake) {
			status("dormant");
			return status;
		}
		// Roadmap step 28: it harvests as its keeper, only where they could harvest by hand, and only while they are here
		// (unless the server lets a stand-in answer for absent owners); while it waits it draws nothing.
		ServerPlayer answering = Authority.answering(level, keeper);
		if (answering == null) {
			status("keeper_away");
			return status;
		}
		if (verdance < COST) {
			draw(level);
		}
		if (verdance < COST) {
			status("no_verdance");
			return status;
		}
		for (BlockPos pos : Garden.nearest(worldPosition, REACH, 1)) {
			if (!level.isLoaded(pos)) {
				continue;
			}
			BlockState state = level.getBlockState(pos);
			if (!(state.getBlock() instanceof OrganismCropBlock crop) || !crop.isMaxAge(state)
					|| !Authority.mayChange(level, answering, pos)) {
				continue;
			}
			List<ItemStack> harvest = crop.harvestYield(state);
			if (!fits(harvest)) {
				status("full");
				return status;
			}
			for (ItemStack stack : crop.harvest(level, pos.immutable(), state)) {
				insert(stack);
			}
			verdance -= COST;
			setChanged();
			status("working");
			sync();
			return status;
		}
		status("idle");
		return status;
	}

	/** Draws Verdance from the Verdant Hearts in reach, nearest first, until it has {@value #DRAW} more or none is left. */
	private void draw(ServerLevel level) {
		UUID actor = keeper == null ? NO_ONE : keeper;
		for (BlockPos pos : Garden.nearest(worldPosition, HEART_REACH, HEART_REACH)) {
			long want = Math.min(DRAW, CAPACITY - verdance);
			if (want <= 0) {
				return;
			}
			if (!level.isLoaded(pos) || !(level.getBlockEntity(pos) instanceof VerdantHeartBlockEntity heart)) {
				continue;
			}
			Transfers.Transfer transfer = heart.drawInto(reservoir(), ownership(), actor, want, level);
			if (transfer.outcome().moved()) {
				verdance = transfer.target().amount();
				setChanged();
				sync();
			}
		}
	}

	/** Whether every stack of {@code harvest} fits in its slots at once. */
	private boolean fits(List<ItemStack> harvest) {
		List<ItemStack> copy = new ArrayList<>();
		for (ItemStack stack : items) {
			copy.add(stack.copy());
		}
		for (ItemStack stack : harvest) {
			if (!insertInto(copy, stack.copy()).isEmpty()) {
				return false;
			}
		}
		return true;
	}

	private void insert(ItemStack stack) {
		insertInto(items, stack);
		setChanged();
	}

	/** Puts {@code stack} into {@code slots}, merging first; returns what did not fit. */
	private static ItemStack insertInto(List<ItemStack> slots, ItemStack stack) {
		for (int i = 0; i < slots.size() && !stack.isEmpty(); i++) {
			ItemStack slot = slots.get(i);
			if (!slot.isEmpty() && ItemStack.isSameItemSameComponents(slot, stack)) {
				int moved = Math.min(stack.getCount(), slot.getMaxStackSize() - slot.getCount());
				slot.grow(moved);
				stack.shrink(moved);
			}
		}
		for (int i = 0; i < slots.size() && !stack.isEmpty(); i++) {
			if (slots.get(i).isEmpty()) {
				slots.set(i, stack.copy());
				stack.setCount(0);
			}
		}
		return stack;
	}

	// ---------------------------------------------------------------- hoppers and pipes take the harvest out

	@Override
	public int[] getSlotsForFace(Direction side) {
		return ALL;
	}

	@Override
	public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) {
		return false;
	}

	@Override
	public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
		return true;
	}

	@Override
	public boolean canPlaceItem(int slot, ItemStack stack) {
		return false;
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

	// ---------------------------------------------------------------- saving and clients

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		items = NonNullList.withSize(SLOTS, ItemStack.EMPTY);
		ContainerHelper.loadAllItems(input, items);
		verdance = Math.clamp(input.getLongOr("verdance", 0L), 0L, CAPACITY);
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		ContainerHelper.saveAllItems(output, items);
		output.putLong("verdance", verdance);
	}

	@Override
	protected void clientData(CompoundTag tag) {
		tag.putLong("verdance", verdance);
	}

	@Override
	public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
		controllers.add(new AnimationController<GleanerBlockEntity>("main", 5,
				test -> test.setAndContinue(test.animatable().status().equals("working") ? REACHING : IDLE)));
	}

	@Override
	public AnimatableInstanceCache getAnimatableInstanceCache() {
		return cache;
	}
}
