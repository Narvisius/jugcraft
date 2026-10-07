package io.github.jimbozoomer.jugcraft.agriculture;

import java.util.stream.IntStream;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * The Iron-Bound Coffin's {@value #SLOTS} slots and its lock: the wards of the Skeleton Key it is locked to, or 0. Locked,
 * it opens only for someone holding that key, and shows hoppers and pipes no slots on any side.
 */
public class IronBoundCoffinBlockEntity extends CoffinBlockEntity implements WorldlyContainer {
	public static final int SLOTS = 54;
	private static final int[] ALL = IntStream.range(0, SLOTS).toArray();
	private static final int[] NONE = new int[0];

	private int lock;

	public IronBoundCoffinBlockEntity(BlockPos pos, BlockState state) {
		super(JugcraftAgriculture.IRON_BOUND_COFFIN_ENTITY, pos, state, SLOTS);
	}

	public int lock() {
		return lock;
	}

	public void setLock(int wards) {
		lock = Math.max(0, wards);
		setChanged();
	}

	/** Locked, it opens only for someone holding its key (vanilla's own lock, if any, is checked first). */
	@Override
	public boolean canOpen(Player player) {
		if (!super.canOpen(player)) {
			return false;
		}
		if (lock != 0 && !player.isSpectator() && !SkeletonKeyItem.holdsKey(player, lock)) {
			sendChestLockedNotifications(Vec3.atCenterOf(worldPosition), player, getDisplayName());
			return false;
		}
		return true;
	}

	@Override
	public int[] getSlotsForFace(Direction side) {
		return lock == 0 ? ALL : NONE;
	}

	@Override
	public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) {
		return lock == 0;
	}

	@Override
	public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
		return lock == 0;
	}

	@Override
	protected Component getDefaultName() {
		return Component.translatable("container.jugcraft.iron_bound_coffin");
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		lock = input.getIntOr("lock", 0);
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		if (lock != 0) {
			output.putInt("lock", lock);
		}
	}
}
