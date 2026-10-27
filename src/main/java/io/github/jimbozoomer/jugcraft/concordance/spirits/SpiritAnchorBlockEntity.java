package io.github.jimbozoomer.jugcraft.concordance.spirits;

import io.github.jimbozoomer.jugcraft.concordance.Saved;
import io.github.jimbozoomer.jugcraft.concordance.resource.BoundWill;
import io.github.jimbozoomer.jugcraft.concordance.worker.Agreement;
import io.github.jimbozoomer.jugcraft.concordance.worker.Pacts;
import io.github.jimbozoomer.jugcraft.concordance.worker.WorkerDefinition;
import io.github.jimbozoomer.jugcraft.party.UseMode;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.NonNullList;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.Containers;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

/**
 * The Spirit Anchor (roadmap step 17): where a spirit's agreement is kept and what it delivers to. Its agreement is kept
 * through an {@link AgreementHolder} (Concordance Chunk D, unit 1): placed, it records its owner, and its owner or their
 * party seal a Gathering Shade's agreement here, recorded as a Bound Will for its holder ({@link BoundWills}): the one
 * authority for whether it stands, where it is kept and whether it is suspended (Concordance Chunk D, D-02). The anchor
 * holds the terms as sealed here and a copy of the day's tasks, and nine slots the shade fills and hoppers below may
 * empty. A copy of an anchor elsewhere is inert. Its holder may suspend or resume the agreement and calls another shade
 * when theirs is lost (AgreementHolder.end ends it without breaking the anchor); breaking the anchor with the Concordance
 * on releases it: the Bound Will is released, the shade leaves its load here, and everything here drops where the anchor
 * stood. Since Concordance Chunk D (D-08) it may be bound to a Courier Post (an optional post): one so bound seals a
 * Workshop Hand.
 */
public class SpiritAnchorBlockEntity extends BlockEntity implements WorldlyContainer, AgreementHolder.Holds {
	public static final int SLOTS = 9;
	private static final int[] ALL = {0, 1, 2, 3, 4, 5, 6, 7, 8};
	/** G9: new records name their counterpart with its namespace; older ones keep "gathering_shade". */
	public static final String COUNTERPART = GatheringShadeEntity.DEFINITION;

	private NonNullList<ItemStack> items = NonNullList.withSize(SLOTS, ItemStack.EMPTY);
	private final AgreementHolder holder = new AgreementHolder(this);
	private @Nullable GlobalPos post;

	public SpiritAnchorBlockEntity(BlockPos pos, BlockState state) {
		super(Workers.ANCHOR_ENTITY, pos, state);
	}

	@Override
	public AgreementHolder holder() {
		return holder;
	}

	public @Nullable Agreement agreement() {
		return holder.agreement();
	}

	public @Nullable UUID spirit() {
		return holder.spirit();
	}

	public @Nullable UUID owner() {
		return holder.owner();
	}

	/** The spirit's own record of a finished task (its Bound Will keeps the day's count; the anchor a copy). */
	public void taskDone(long gameTime) {
		if (level instanceof ServerLevel server) {
			holder.taskDone(server, gameTime);
		}
	}

	/**
	 * What the anchor seals: a Gathering Shade under {@code terms}, for its holder alone, sealed by its owner or their party,
	 * at no cost, with {@code upkeep} (none in play: the anchor's definition has none, D-P9; tests seal with test terms).
	 */
	public AgreementHolder.Sealing sealing(WorkerDefinition.Spirit terms, BoundWill.@Nullable Upkeep upkeep) {
		return new AgreementHolder.Sealing(GatheringShadeEntity.DEFINITION, COUNTERPART, AgreementHolder.service(terms), upkeep, UseMode.PERSONAL,
				false, true, null, null);
	}

	/** Seals a Gathering Shade's agreement for {@code player} under {@code terms}: DONE, or the one reason not. */
	public AgreementHolder.Sealed seal(ServerLevel level, ServerPlayer player, WorkerDefinition.Spirit terms, long gameTime) {
		return holder.seal(level, player, sealing(terms, null), gameTime);
	}

	/** The Courier Post this anchor is bound to (D-08), or null: a Workshop Hand sealed here carries that post's requests. */
	public @Nullable GlobalPos post() {
		return post;
	}

	/** Binds the anchor to {@code next}, or unbinds it (null). Only PorterKeyItem.bind calls this, having asked who may and where. */
	void setPost(@Nullable GlobalPos next) {
		post = next;
		setChanged();
	}

	/**
	 * What the anchor seals for a Workshop Hand (D-08, D-S2): its own agreement and counterpart, its terms, no upkeep, for
	 * its holder and their party (the Courier Post's rule, recorded on the Bound Will), any number a holder, sealed by the
	 * owner or their party, at no cost.
	 */
	public AgreementHolder.Sealing handSealing(WorkerDefinition.Spirit terms) {
		return new AgreementHolder.Sealing(WorkshopHandEntity.DEFINITION, WorkshopHandEntity.DEFINITION, AgreementHolder.service(terms), null,
				UseMode.PARTY, false, true, null, null);
	}

	/** Calls this anchor's presence for {@code who}: a Gathering Shade, or (D-08) a Workshop Hand; nothing for any other counterpart (critique 7). */
	@Override
	public @Nullable WorkerEntity<?> call(ServerLevel level, UUID who, String counterpart) {
		String wanted = Pacts.counterpart(counterpart);
		BlockPos pos = getBlockPos();
		WorkerEntity<?> called;
		if (COUNTERPART.equals(wanted)) {
			GatheringShadeEntity shade = Workers.GATHERING_SHADE.create(level, EntitySpawnReason.MOB_SUMMONED);
			if (shade == null) {
				return null;
			}
			shade.setAnchor(pos);
			called = shade;
		} else if (WorkshopHandEntity.DEFINITION.equals(wanted)) {
			WorkshopHandEntity hand = Workers.WORKSHOP_HAND.create(level, EntitySpawnReason.MOB_SUMMONED);
			if (hand == null) {
				return null;
			}
			hand.setAnchor(pos);
			called = hand;
		} else {
			return null;
		}
		called.setOwner(who);
		called.snapTo(pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5, 0.0F, 0.0F);
		return level.addFreshEntity(called) ? called : null;
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

	/** Broken: an agreement that counts here is released (with the Concordance on) and everything here, its shade's load among it, drops once. */
	@Override
	public void preRemoveSideEffects(BlockPos pos, BlockState state) {
		if (level instanceof ServerLevel server) {
			holder.broken(server, server.getGameTime());
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
		holder.load(input);
		post = input.read("post", GlobalPos.CODEC).orElse(null);
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		Saved.stamp(output, 1);
		ContainerHelper.saveAllItems(output, items);
		holder.save(output);
		if (post != null) {
			output.store("post", GlobalPos.CODEC, post);
		}
	}
}
