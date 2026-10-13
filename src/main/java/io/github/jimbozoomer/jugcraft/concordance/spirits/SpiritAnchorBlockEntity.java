package io.github.jimbozoomer.jugcraft.concordance.spirits;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.jimbozoomer.jugcraft.concordance.Saved;
import io.github.jimbozoomer.jugcraft.concordance.resource.BoundWill;
import io.github.jimbozoomer.jugcraft.concordance.resource.BoundWillLedger;
import io.github.jimbozoomer.jugcraft.concordance.worker.Agreement;
import io.github.jimbozoomer.jugcraft.concordance.worker.WorkerDefinition;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.core.UUIDUtil;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.Containers;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

/**
 * The Spirit Anchor (roadmap step 17): where a spirit's agreement is kept and what it delivers to. Sealing an agreement
 * here records a Bound Will for its holder ({@link BoundWills}) and calls a Gathering Shade; the anchor holds the
 * agreement's terms and the day's tasks (the one authoritative copy), and nine slots the shade fills and hoppers below
 * may empty. Its holder may suspend or resume the agreement; breaking the anchor releases it: the Bound Will is
 * released, the shade departs, and everything here, and anything the shade carried, drops where the anchor stood.
 */
public class SpiritAnchorBlockEntity extends BlockEntity implements WorldlyContainer {
	public static final int SLOTS = 9;
	private static final int[] ALL = {0, 1, 2, 3, 4, 5, 6, 7, 8};
	public static final Codec<Agreement> AGREEMENT_CODEC = RecordCodecBuilder.create(i -> i.group(
			UUIDUtil.CODEC.fieldOf("record").forGetter(Agreement::record), UUIDUtil.CODEC.fieldOf("holder").forGetter(Agreement::holder),
			Codec.STRING.fieldOf("work").forGetter(Agreement::work), Codec.STRING.fieldOf("dimension").forGetter(Agreement::dimension),
			Codec.INT.fieldOf("x").forGetter(Agreement::x), Codec.INT.fieldOf("y").forGetter(Agreement::y), Codec.INT.fieldOf("z").forGetter(Agreement::z),
			Codec.INT.fieldOf("radius").forGetter(Agreement::radius), Codec.INT.fieldOf("from").forGetter(Agreement::from),
			Codec.INT.fieldOf("to").forGetter(Agreement::to), Codec.INT.fieldOf("quota").forGetter(Agreement::quota),
			Codec.LONG.fieldOf("window").forGetter(Agreement::window), Codec.INT.fieldOf("done_today").forGetter(Agreement::doneToday),
			Codec.BOOL.fieldOf("suspended").forGetter(Agreement::suspended)).apply(i, Agreement::new));

	private NonNullList<ItemStack> items = NonNullList.withSize(SLOTS, ItemStack.EMPTY);
	private @Nullable Agreement agreement;
	private @Nullable UUID spirit;

	public SpiritAnchorBlockEntity(BlockPos pos, BlockState state) {
		super(Workers.ANCHOR_ENTITY, pos, state);
	}

	public @Nullable Agreement agreement() {
		return agreement;
	}

	public @Nullable UUID spirit() {
		return spirit;
	}

	/** The spirit's own record of a finished task (the anchor keeps the day's count). */
	public void taskDone(long gameTime) {
		if (agreement != null) {
			agreement = agreement.didTask(gameTime);
			setChanged();
		}
	}

	/**
	 * Seals an agreement with {@code holder} under {@code terms}: a Bound Will record, the terms, and a Gathering Shade
	 * called beside the anchor. Returns the shade, or null when one is already sealed here.
	 */
	public @Nullable GatheringShadeEntity seal(ServerLevel level, ServerPlayer holder, WorkerDefinition.Spirit terms, long gameTime) {
		if (agreement != null) {
			return null;
		}
		BlockPos pos = getBlockPos();
		BoundWill record = new BoundWill(UUID.randomUUID(), GatheringShadeEntity.DEFINITION, "gathering_shade", holder.getUUID(), gameTime, false);
		BoundWills wills = BoundWills.of(level.getServer());
		BoundWillLedger.Change change = wills.seal(record);
		if (change.outcome() != BoundWillLedger.Outcome.DONE) {
			return null;
		}
		GatheringShadeEntity shade = Workers.GATHERING_SHADE.create(level, EntitySpawnReason.MOB_SUMMONED);
		if (shade == null) {
			wills.release(record.id(), holder.getUUID());
			return null;
		}
		agreement = new Agreement(record.id(), holder.getUUID(), terms.work(), level.dimension().identifier().toString(), pos.getX(), pos.getY(), pos.getZ(),
				terms.radius(), terms.from(), terms.to(), terms.quota(), Long.MIN_VALUE, 0, false);
		shade.setOwner(holder.getUUID());
		shade.setAnchor(pos);
		shade.snapTo(pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5, 0.0F, 0.0F);
		level.addFreshEntity(shade);
		spirit = shade.getUUID();
		setChanged();
		return shade;
	}

	/** The holder suspends or resumes the agreement; returns whether it is now suspended. */
	public boolean toggleSuspended() {
		if (agreement == null) {
			return false;
		}
		agreement = agreement.suspended(!agreement.suspended());
		setChanged();
		return agreement.suspended();
	}

	/** Releases the agreement: its Bound Will is released and its shade departs (if it is loaded). */
	public void release(ServerLevel level) {
		if (agreement == null) {
			return;
		}
		BoundWills.of(level.getServer()).release(agreement.record(), agreement.holder());
		if (spirit != null) {
			Entity found = level.getEntity(spirit);
			if (found instanceof GatheringShadeEntity shade) {
				shade.depart(level, getBlockPos());
			}
			WorkerRoster.of(level.getServer()).remove(agreement.holder(), spirit);
		}
		agreement = null;
		spirit = null;
		setChanged();
	}

	/** Puts what fits of {@code stack} into the anchor; returns the rest. */
	public ItemStack deliver(ItemStack stack) {
		for (int i = 0; i < SLOTS && !stack.isEmpty(); i++) {
			ItemStack slot = items.get(i);
			if (!slot.isEmpty() && ItemStack.isSameItemSameComponents(slot, stack)) {
				int moved = Math.min(stack.getCount(), slot.getMaxStackSize() - slot.getCount());
				slot.grow(moved);
				stack.shrink(moved);
			}
		}
		for (int i = 0; i < SLOTS && !stack.isEmpty(); i++) {
			if (items.get(i).isEmpty()) {
				items.set(i, stack.copy());
				stack.setCount(0);
			}
		}
		setChanged();
		return stack;
	}

	/** Broken: the agreement is released and everything here drops once. */
	@Override
	public void preRemoveSideEffects(BlockPos pos, BlockState state) {
		if (level instanceof ServerLevel server) {
			release(server);
			Containers.dropContents(server, pos, this);
		}
	}

	// ---------------------------------------------------------------- hoppers below take the deliveries out

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

	public List<ItemStack> contents() {
		return List.copyOf(items);
	}

	// ---------------------------------------------------------------- saving

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		items = NonNullList.withSize(SLOTS, ItemStack.EMPTY);
		ContainerHelper.loadAllItems(input, items);
		agreement = input.read("agreement", AGREEMENT_CODEC).orElse(null);
		spirit = input.read("spirit", UUIDUtil.CODEC).orElse(null);
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		Saved.stamp(output, 1);
		ContainerHelper.saveAllItems(output, items);
		if (agreement != null) {
			output.store("agreement", AGREEMENT_CODEC, agreement);
		}
		if (spirit != null) {
			output.store("spirit", UUIDUtil.CODEC, spirit);
		}
	}
}
