package io.github.jimbozoomer.jugcraft.logistics;

import io.github.jimbozoomer.jugcraft.kinetic.KineticConsumer;
import io.github.jimbozoomer.jugcraft.kinetic.ShaftBlock;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.fabricmc.fabric.api.transfer.v1.item.ItemStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.item.base.SingleStackStorage;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * The items on one conveyor, each a whole stack at a progress from 0 (the back) to 1 (the front),
 * kept at least {@link #SPACING} apart, front first. The server moves them and hands them on; the
 * client moves them the same way between updates so they glide (see client/ConveyorRenderer).
 */
public class ConveyorBlockEntity extends BlockEntity implements KineticConsumer {
	/** Blocks per tick (2.5 blocks a second); the belt texture's ribs run at the same speed. */
	public static final double SPEED = 1 / 8.0;
	public static final int MAX_ITEMS = 4;
	public static final float SPACING = 1.0F / MAX_ITEMS;
	/** KE each conveyor in a run uses per tick; a drive with less leaves the whole run still. */
	public static final long KE_PER_CONVEYOR = 1;
	/** Most conveyors one drive runs. */
	public static final int MAX_RUN = 64;
	/** Ticks a conveyor keeps running after its run was last driven. */
	private static final int COAST = 2;
	private static final int PICKUP_INTERVAL = 4;

	/** One stack on the belt; {@code previous} is where it was a tick ago, for smooth drawing. */
	public static final class Entry {
		public ItemStack stack;
		public float progress;
		public float previous;

		Entry(ItemStack stack, float progress) {
			this.stack = stack;
			this.progress = progress;
			this.previous = progress;
		}
	}

	private final List<Entry> items = new ArrayList<>();
	private ItemStack inputStack = ItemStack.EMPTY;
	private long drivenUntil = Long.MIN_VALUE;
	/** The splitter's next output: 0 left, 1 straight on, 2 right. */
	private int nextOutput;

	/** Where pipes, hoppers and machines put items: a one-stack buffer at the back, moved onto the belt when there is room. */
	final SingleStackStorage input = new SingleStackStorage() {
		@Override
		protected ItemStack getStack() {
			return inputStack;
		}

		@Override
		protected void setStack(ItemStack stack) {
			inputStack = stack;
		}

		@Override
		public long extract(ItemVariant variant, long maxAmount, TransactionContext transaction) {
			return 0;
		}

		@Override
		protected void onFinalCommit() {
			setChanged();
		}
	};

	public ConveyorBlockEntity(BlockPos pos, BlockState state) {
		super(JugcraftLogistics.CONVEYOR_ENTITY, pos, state);
	}

	public List<Entry> items() {
		return items;
	}

	private Direction facing() {
		return getBlockState().getValue(ConveyorBlock.FACING);
	}

	// ------------------------------------------------------------------ power

	/** Drives this conveyor's whole run if the offer covers it; a run already driven this tick takes nothing more. */
	@Override
	public long acceptKinetic(Direction side, long maxAmount) {
		if (level == null) {
			return 0;
		}
		long now = level.getGameTime();
		if (drivenUntil >= now + COAST) {
			return 0;
		}
		List<ConveyorBlockEntity> run = run();
		long cost = run.size() * KE_PER_CONVEYOR;
		if (maxAmount < cost) {
			return 0;
		}
		for (ConveyorBlockEntity conveyor : run) {
			conveyor.drivenUntil = now + COAST;
		}
		return cost;
	}

	/** Every conveyor joined to this one, front to back or side-on, up to {@link #MAX_RUN}. */
	private List<ConveyorBlockEntity> run() {
		List<ConveyorBlockEntity> run = new ArrayList<>();
		Set<BlockPos> seen = new HashSet<>();
		ArrayDeque<ConveyorBlockEntity> queue = new ArrayDeque<>();
		queue.add(this);
		seen.add(worldPosition);
		while (!queue.isEmpty() && run.size() < MAX_RUN) {
			ConveyorBlockEntity conveyor = queue.poll();
			run.add(conveyor);
			BlockPos pos = conveyor.worldPosition;
			for (Direction direction : Direction.Plane.HORIZONTAL) {
				BlockPos next = pos.relative(direction);
				if (seen.contains(next) || !(level.getBlockEntity(next) instanceof ConveyorBlockEntity other)) {
					continue;
				}
				// Joined if either feeds the other.
				if (direction == conveyor.facing() || other.facing() == direction.getOpposite()) {
					seen.add(next);
					queue.add(other);
				}
			}
		}
		return run;
	}

	// ------------------------------------------------------------------ ticking

	void serverTick(ServerLevel level, BlockPos pos, BlockState state) {
		long now = level.getGameTime();
		boolean running = now <= drivenUntil;
		if (state.getValue(ShaftBlock.TURNING) != running) {
			state = state.setValue(ShaftBlock.TURNING, running);
			level.setBlock(pos, state, Block.UPDATE_CLIENTS);
		}
		boolean changed = false;
		if (now % PICKUP_INTERVAL == 0) {
			changed = pickUpDroppedItems(level, pos);
		}
		if (running) {
			if (!inputStack.isEmpty() && roomAt(0)) {
				insertSorted(new Entry(inputStack, 0));
				inputStack = ItemStack.EMPTY;
				changed = true;
			}
			advance();
			if (!items.isEmpty() && items.get(0).progress >= 1) {
				Entry front = items.get(0);
				int before = front.stack.getCount();
				front.stack = handOff(level, pos, state, front.stack);
				if (front.stack.isEmpty()) {
					items.remove(0);
				}
				changed |= front.stack.isEmpty() || front.stack.getCount() != before;
			}
		}
		if (changed) {
			sync();
		}
	}

	/** Between updates the client moves items on its own, stopping where the server would stop them. */
	void clientTick(BlockState state) {
		for (Entry entry : items) {
			entry.previous = entry.progress;
		}
		if (state.getValue(ShaftBlock.TURNING)) {
			advance();
		}
	}

	private void advance() {
		for (int i = 0; i < items.size(); i++) {
			Entry entry = items.get(i);
			entry.previous = entry.progress;
			float limit = i == 0 ? 1 : items.get(i - 1).progress - SPACING;
			entry.progress = Math.max(entry.progress, Math.min((float) (entry.progress + SPEED), limit));
		}
	}

	/**
	 * Hands the front stack on: into the conveyor or inventory ahead (or, for a splitter, left, ahead and
	 * right in turn), or onto the ground if the way ahead is open. Returns what is left.
	 */
	private ItemStack handOff(ServerLevel level, BlockPos pos, BlockState state, ItemStack stack) {
		Direction facing = state.getValue(ConveyorBlock.FACING);
		boolean splitter = state.getBlock() instanceof ConveyorBlock block && block.isSplitter();
		Direction[] outputs = splitter
				? new Direction[] {facing.getCounterClockWise(), facing, facing.getClockWise()}
				: new Direction[] {facing};
		for (int attempt = 0; attempt < outputs.length; attempt++) {
			int index = (nextOutput + attempt) % outputs.length;
			Direction out = outputs[index];
			ItemStack left = offer(level, pos.relative(out), out, stack);
			if (left.getCount() != stack.getCount()) {
				if (splitter) {
					nextOutput = (index + 1) % outputs.length;
				}
				return left;
			}
		}
		// Nothing takes it: off the front end, if nothing is in the way.
		BlockPos ahead = pos.relative(facing);
		if (level.getBlockState(ahead).getCollisionShape(level, ahead).isEmpty()) {
			Vec3 at = Vec3.atCenterOf(pos).add(facing.getStepX() * 0.6, -0.1, facing.getStepZ() * 0.6);
			ItemEntity dropped = new ItemEntity(level, at.x, at.y, at.z, stack,
					facing.getStepX() * SPEED, 0, facing.getStepZ() * SPEED);
			dropped.setDefaultPickUpDelay();
			level.addFreshEntity(dropped);
			return ItemStack.EMPTY;
		}
		return stack;
	}

	/** Puts as much of the stack as fits into the block at {@code target}, entered from its {@code travel.getOpposite()} side. */
	private static ItemStack offer(ServerLevel level, BlockPos target, Direction travel, ItemStack stack) {
		if (level.getBlockEntity(target) instanceof ConveyorBlockEntity next) {
			Direction nextFacing = next.facing();
			if (nextFacing == travel.getOpposite()) {
				return stack;
			}
			// Straight on at the back; from the side, onto the middle.
			return next.accept(stack, nextFacing == travel ? 0 : 0.5F) ? ItemStack.EMPTY : stack;
		}
		Storage<ItemVariant> storage = ItemStorage.SIDED.find(level, target, travel.getOpposite());
		if (storage == null) {
			return stack;
		}
		try (Transaction transaction = Transaction.openOuter()) {
			long inserted = storage.insert(ItemVariant.of(stack), stack.getCount(), transaction);
			if (inserted <= 0) {
				return stack;
			}
			transaction.commit();
			ItemStack left = stack.copy();
			left.shrink((int) inserted);
			return left;
		}
	}

	/** Takes a whole stack onto the belt at {@code progress}, if there is room there. */
	public boolean accept(ItemStack stack, float progress) {
		if (stack.isEmpty() || !roomAt(progress)) {
			return false;
		}
		insertSorted(new Entry(stack.copy(), progress));
		sync();
		return true;
	}

	private boolean roomAt(float progress) {
		if (items.size() >= MAX_ITEMS) {
			return false;
		}
		for (Entry entry : items) {
			if (Math.abs(entry.progress - progress) < SPACING - 1e-4F) {
				return false;
			}
		}
		return true;
	}

	private void insertSorted(Entry entry) {
		int index = 0;
		while (index < items.size() && items.get(index).progress > entry.progress) {
			index++;
		}
		items.add(index, entry);
	}

	/** Item entities resting on the belt go onto it where they lie. */
	private boolean pickUpDroppedItems(ServerLevel level, BlockPos pos) {
		boolean changed = false;
		Direction facing = facing();
		AABB area = new AABB(pos.getX(), pos.getY() + 0.2, pos.getZ(), pos.getX() + 1, pos.getY() + 0.8, pos.getZ() + 1);
		for (ItemEntity item : level.getEntitiesOfClass(ItemEntity.class, area, ItemEntity::isAlive)) {
			Vec3 offset = item.position().subtract(Vec3.atCenterOf(pos));
			float progress = (float) Math.max(0, Math.min(0.9, 0.5 + offset.x * facing.getStepX() + offset.z * facing.getStepZ()));
			if (roomAt(progress)) {
				insertSorted(new Entry(item.getItem().copy(), progress));
				item.discard();
				changed = true;
			}
		}
		return changed;
	}

	/** Spills everything on the belt when it is broken. */
	@Override
	public void preRemoveSideEffects(BlockPos pos, BlockState state) {
		super.preRemoveSideEffects(pos, state);
		if (level != null && !level.isClientSide()) {
			for (Entry entry : items) {
				Block.popResource(level, pos, entry.stack);
			}
			if (!inputStack.isEmpty()) {
				Block.popResource(level, pos, inputStack);
			}
		}
		items.clear();
		inputStack = ItemStack.EMPTY;
	}

	private void sync() {
		setChanged();
		if (level != null) {
			BlockState state = getBlockState();
			level.sendBlockUpdated(worldPosition, state, state, Block.UPDATE_CLIENTS);
		}
	}

	// ------------------------------------------------------------------ saving and syncing

	@Override
	public Packet<ClientGamePacketListener> getUpdatePacket() {
		return ClientboundBlockEntityDataPacket.create(this);
	}

	@Override
	public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
		return saveWithoutMetadata(registries);
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		NonNullList<ItemStack> stacks = NonNullList.withSize(MAX_ITEMS + 1, ItemStack.EMPTY);
		ContainerHelper.loadAllItems(input, stacks);
		// Keep the client's in-between positions for stacks it already shows, so an update does not make them jump.
		List<Entry> old = new ArrayList<>(items);
		items.clear();
		for (int i = 0; i < MAX_ITEMS; i++) {
			if (!stacks.get(i).isEmpty()) {
				float progress = input.getInt("progress" + i).orElse(0) / 1000.0F;
				Entry entry = new Entry(stacks.get(i), progress);
				for (Entry shown : old) {
					if (Math.abs(shown.progress - progress) < SPACING / 2 && ItemStack.isSameItemSameComponents(shown.stack, entry.stack)) {
						entry.previous = shown.previous;
						entry.progress = Math.max(progress, shown.progress);
					}
				}
				insertSorted(entry);
			}
		}
		inputStack = stacks.get(MAX_ITEMS);
		nextOutput = input.getInt("next_output").orElse(0);
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		NonNullList<ItemStack> stacks = NonNullList.withSize(MAX_ITEMS + 1, ItemStack.EMPTY);
		for (int i = 0; i < items.size(); i++) {
			stacks.set(i, items.get(i).stack);
			output.putInt("progress" + i, Math.round(items.get(i).progress * 1000));
		}
		stacks.set(MAX_ITEMS, inputStack);
		ContainerHelper.saveAllItems(output, stacks);
		output.putInt("next_output", nextOutput);
	}

	/** Used by the level's item lookup: everything goes in at the back of the belt. */
	Storage<ItemVariant> storage() {
		return input;
	}
}
