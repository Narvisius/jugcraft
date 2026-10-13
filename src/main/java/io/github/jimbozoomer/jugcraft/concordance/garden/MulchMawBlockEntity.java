package io.github.jimbozoomer.jugcraft.concordance.garden;

import com.geckolib.animatable.GeoBlockEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.util.GeckoLibUtil;
import io.github.jimbozoomer.jugcraft.concordance.ecology.Mulch;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

/**
 * The Mulch Maw (roadmap step 14): the garden's consumer. It holds one stack of plant matter (fed by hand or by any
 * hopper or pipe, never taken back out) and every {@value #DIGEST_TICKS} ticks eats one, adding its worth in quarters
 * of a nutrient ({@code #jugcraft:mulch/*}) to what it has digested; every {@value Mulch#QUARTERS} quarters become a
 * nutrient for the poorest awake Verdant Bed within {@value #REACH} blocks. While every bed in reach is full it eats
 * nothing more, so nothing it digests is lost, and nothing it eats gives back the nutrients its growing cost.
 */
public class MulchMawBlockEntity extends LivingDeviceBlockEntity implements GeoBlockEntity, WorldlyContainer {
	public static final int DIGEST_TICKS = 40;
	public static final int REACH = 3;
	private static final int[] SLOTS = {0};
	private static final RawAnimation CLOSED = RawAnimation.begin().thenLoop("animation.mulch_maw.closed");
	private static final RawAnimation CHEWING = RawAnimation.begin().thenLoop("animation.mulch_maw.chewing");

	private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
	private ItemStack food = ItemStack.EMPTY;
	/** Quarters of a nutrient digested and not yet given to a bed. */
	private int held;

	public MulchMawBlockEntity(BlockPos pos, BlockState state) {
		super(Garden.MAW_ENTITY, pos, state);
	}

	public int held() {
		return held;
	}

	public ItemStack food() {
		return food;
	}

	void serverTick(ServerLevel level) {
		if (due(level, DIGEST_TICKS)) {
			digest(level);
		}
	}

	/** One pulse (or a test's): give whole nutrients it holds, then eat one food if it can; returns its status. */
	public String digest(ServerLevel level) {
		if (!Garden.enabled()) {
			status("disabled");
			return status;
		}
		if (!awake) {
			status("dormant");
			return status;
		}
		List<VerdantBedBlockEntity> beds = Garden.bedsAround(level, worldPosition, REACH);
		give(beds);
		if (held >= Mulch.QUARTERS) {
			status("beds_full");
			return status;
		}
		int value = Garden.mulch(food);
		if (food.isEmpty() || !Mulch.mayEat(held, value)) {
			status(food.isEmpty() ? "hungry" : "idle");
			return status;
		}
		food.shrink(1);
		if (food.isEmpty()) {
			food = ItemStack.EMPTY;
		}
		held = Mulch.digest(held, value);
		setChanged();
		give(beds);
		status("working");
		return status;
	}

	private void give(List<VerdantBedBlockEntity> beds) {
		while (held >= Mulch.QUARTERS && Garden.nourish(beds, 1) == 1) {
			held -= Mulch.QUARTERS;
			setChanged();
		}
	}

	/** Takes as much of {@code stack} as fits beside what it holds (the same food only); returns how many. */
	public int feed(ItemStack stack) {
		if (Garden.mulch(stack) == 0 || !food.isEmpty() && !ItemStack.isSameItemSameComponents(food, stack)) {
			return 0;
		}
		int room = (food.isEmpty() ? stack.getMaxStackSize() : food.getMaxStackSize() - food.getCount());
		int taken = Math.min(room, stack.getCount());
		if (taken > 0) {
			if (food.isEmpty()) {
				food = stack.copyWithCount(taken);
			} else {
				food.grow(taken);
			}
			setChanged();
		}
		return taken;
	}

	// ---------------------------------------------------------------- hoppers and pipes put food in; nothing takes it out

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
		return false;
	}

	@Override
	public boolean canPlaceItem(int slot, ItemStack stack) {
		return slot == 0 && Garden.mulch(stack) > 0 && (food.isEmpty() || ItemStack.isSameItemSameComponents(food, stack));
	}

	@Override
	public int getContainerSize() {
		return 1;
	}

	@Override
	public boolean isEmpty() {
		return food.isEmpty();
	}

	@Override
	public ItemStack getItem(int slot) {
		return slot == 0 ? food : ItemStack.EMPTY;
	}

	@Override
	public ItemStack removeItem(int slot, int count) {
		if (slot != 0 || food.isEmpty()) {
			return ItemStack.EMPTY;
		}
		ItemStack taken = food.split(count);
		setChanged();
		return taken;
	}

	@Override
	public ItemStack removeItemNoUpdate(int slot) {
		ItemStack taken = food;
		food = ItemStack.EMPTY;
		return slot == 0 ? taken : ItemStack.EMPTY;
	}

	@Override
	public void setItem(int slot, ItemStack stack) {
		if (slot == 0) {
			food = stack;
			setChanged();
		}
	}

	@Override
	public boolean stillValid(Player player) {
		return false;
	}

	@Override
	public void clearContent() {
		food = ItemStack.EMPTY;
	}

	// ---------------------------------------------------------------- saving and clients

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		food = input.read("food", ItemStack.CODEC).orElse(ItemStack.EMPTY);
		held = Math.clamp(input.getIntOr("held", 0), 0, Mulch.MAX_HELD);
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		if (!food.isEmpty()) {
			output.store("food", ItemStack.CODEC, food);
		}
		output.putInt("held", held);
	}

	@Override
	protected void clientData(CompoundTag tag) {
		tag.putInt("held", held);
	}

	@Override
	public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
		controllers.add(new AnimationController<MulchMawBlockEntity>("main", 5,
				test -> test.setAndContinue(test.animatable().status().equals("working") ? CHEWING : CLOSED)));
	}

	@Override
	public AnimatableInstanceCache getAnimatableInstanceCache() {
		return cache;
	}
}
