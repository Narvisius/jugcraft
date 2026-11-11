package io.github.jimbozoomer.jugcraft.warehouse;

import java.util.List;
import net.fabricmc.fabric.api.menu.v1.ExtendedMenuProvider;
import net.fabricmc.fabric.api.transfer.v1.context.ContainerItemContext;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.item.ContainerStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageUtil;
import net.fabricmc.fabric.api.transfer.v1.storage.base.InsertionOnlyStorage;
import net.fabricmc.fabric.api.transfer.v1.storage.base.SingleSlotStorage;
import net.fabricmc.fabric.api.transfer.v1.transaction.base.SnapshotParticipant;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

/**
 * The Liquids In panel (part W4) set in the outside wall in front of a Drum Filling Station: four flanged pipe inlets
 * (LIQ 1 to LIQ 4, on its side blocks' faces), the tanker coupling (its middle block, low) and the funnel buckets are
 * tipped into (its TIP BUCKETS slots: filled buckets, drums, any liquid container; TIP ALL empties them into the
 * station's buffer tank, the empties stay for their owner to take back). What comes in goes into the station's buffer
 * tank: one liquid at a time (the others wait); gases and lava are turned away. It keeps each inlet's flow for its
 * lamps and screens.
 */
public class LiquidsInBlockEntity extends BlockEntity implements PieceEntity {
	/** LIQ 1..4 and the tanker coupling. */
	public static final int INLETS = 5, TANKER = 4, SLOTS = 9;
	private final Inlet[] inlets = new Inlet[INLETS];
	private @Nullable BlockPos filler;
	private long nextLink;
	/** Part W5: its warehouse's LIQUIDS IN OFF is on (a Signal Box): nothing comes in (as last looked, each second). */
	private boolean off;
	private final SimpleContainer funnel = new SimpleContainer(SLOTS) {
		@Override
		public void setChanged() {
			super.setChanged();
			LiquidsInBlockEntity.this.setChanged();
		}
	};
	// What the clients see.
	private final long[] rate = new long[INLETS];
	private final boolean[] waits = new boolean[INLETS];
	private final FluidVariant[] last = new FluidVariant[INLETS];
	private boolean linked;

	public LiquidsInBlockEntity(BlockPos pos, BlockState state) {
		super(JugcraftWarehouse.LIQUIDS_IN_ENTITY, pos, state);
		for (int i = 0; i < INLETS; i++) {
			inlets[i] = new Inlet();
			last[i] = FluidVariant.blank();
		}
	}

	/** Which inlet a part of the panel is (LIQ 1 bottom left, LIQ 2 bottom right, LIQ 3 top left, LIQ 4 top right, the
	 * tanker coupling the middle's bottom), or -1. */
	public static int inletOf(WarehousePiece piece, int part) {
		int c = piece.column(part), r = piece.row(part);
		if (c == 1) {
			return r == 0 ? TANKER : -1;
		}
		return r * 2 + (c == 0 ? 0 : 1);
	}

	public @Nullable Storage<FluidVariant> inlet(int i) {
		return i >= 0 && i < INLETS ? inlets[i] : null;
	}

	public long rate(int i) {
		return rate[i];
	}

	public boolean waits(int i) {
		return waits[i];
	}

	public FluidVariant last(int i) {
		return last[i];
	}

	public boolean linked() {
		return linked;
	}

	public SimpleContainer funnel() {
		return funnel;
	}

	public @Nullable FillerBlockEntity filler() {
		return filler != null && level != null && level.isLoaded(filler) && level.getBlockEntity(filler) instanceof FillerBlockEntity f ? f : null;
	}

	public Direction facing() {
		BlockState s = getBlockState();
		return s.getBlock() instanceof PieceBlock ? s.getValue(PieceBlock.FACING) : Direction.NORTH;
	}

	/** Whether a pipe is joined to an inlet's face (outside). */
	public boolean piped(int inlet) {
		if (level == null || !(getBlockState().getBlock() instanceof PieceBlock piece)) {
			return false;
		}
		int part = inlet == TANKER ? piece.piece.part(1, 0, 0) : piece.piece.part(inlet % 2 == 0 ? 0 : 2, inlet / 2, 0);
		BlockPos at = worldPosition.offset(piece.piece.offset(facing(), part)).relative(facing().getOpposite());
		return level.getBlockState(at).getBlock() instanceof io.github.jimbozoomer.jugcraft.fluid.FluidPipeBlock;
	}

	/** The filling station against the wall's inside behind the panel. */
	private @Nullable BlockPos findFiller() {
		if (level == null || !(getBlockState().getBlock() instanceof PieceBlock piece)) {
			return null;
		}
		Direction facing = facing();
		for (int p = 0; p < piece.piece.parts(); p++) {
			BlockPos behind = worldPosition.offset(piece.piece.offset(facing, p)).relative(facing);
			BlockState s = level.getBlockState(behind);
			if (s.getBlock() instanceof PieceBlock other && other.piece == WarehousePiece.DRUM_FILLER) {
				return other.origin(behind, s);
			}
		}
		return null;
	}

	public void serverTick(ServerLevel level) {
		long now = level.getGameTime();
		if (now >= nextLink) {
			nextLink = now + 20;
			filler = findFiller();
			TerminalBlockEntity t = Warehouses.terminalOf(level, worldPosition);
			off = t != null && t.liquidsInOff();
		}
		if (now % 20 == 0) {
			boolean l = filler() != null;
			boolean changed = l != linked;
			linked = l;
			for (int i = 0; i < INLETS; i++) {
				Inlet inlet = inlets[i];
				long r = Drums.mb(inlet.count);
				boolean w = inlet.refused > 0 && inlet.count == 0;
				inlet.count = 0;
				inlet.refused = 0;
				if (r != rate[i] || w != waits[i] || !inlet.last.equals(last[i])) {
					changed = true;
				}
				rate[i] = r;
				waits[i] = w;
				last[i] = inlet.last;
			}
			if (changed) {
				sync();
			}
		}
	}

	/** Tips every container in the funnel's slots into the filling station; returns what went in (mB). */
	public long tipAll(@Nullable Player player) {
		FillerBlockEntity f = filler();
		if (f == null || off) {
			return 0;
		}
		long moved = 0;
		ContainerStorage slots = ContainerStorage.of(funnel, null);
		for (int i = 0; i < SLOTS; i++) {
			// (A stack of containers is tipped one at a time, the emptied ones going into free slots.)
			for (int k = 0; k < 64 && !funnel.getItem(i).isEmpty(); k++) {
				Storage<FluidVariant> held = FluidStorage.ITEM.find(funnel.getItem(i), new FunnelSlot(slots, i));
				if (held == null) {
					break;
				}
				long n;
				try (Transaction t = Transaction.openOuter()) {
					n = StorageUtil.move(held, f.buffer, v -> true, Long.MAX_VALUE, t);
					t.commit();
				}
				if (n <= 0) {
					break;
				}
				f.counted(n);
				moved += n;
			}
		}
		if (moved > 0 && level != null) {
			level.playSound(null, worldPosition, SoundEvents.BUCKET_EMPTY, SoundSource.BLOCKS, 0.8F, 1.0F);
			if (player != null) {
				player.sendOverlayMessage(Component.translatable("message.jugcraft.warehouse.tipped", Drums.buckets(Drums.mb(moved))));
			}
		}
		setChanged();
		return Drums.mb(moved);
	}

	@Override
	public InteractionResult use(Level level, BlockPos clicked, Player player, ItemStack stack, InteractionHand hand) {
		if (level.isClientSide()) {
			return InteractionResult.SUCCESS;
		}
		filler = findFiller();
		if (filler() == null) {
			player.sendOverlayMessage(Component.translatable("message.jugcraft.warehouse.no_filler"));
			return InteractionResult.SUCCESS;
		}
		BlockPos panel = worldPosition, at = filler;
		player.openMenu(new ExtendedMenuProvider<LiquidsInMenu.Opening>() {
			@Override
			public Component getDisplayName() {
				return Component.translatable("container.jugcraft.liquids_in");
			}

			@Override
			public AbstractContainerMenu createMenu(int id, Inventory inventory, Player p) {
				return new LiquidsInMenu(id, inventory, funnel, panel, at);
			}

			@Override
			public LiquidsInMenu.Opening getScreenOpeningData(ServerPlayer p) {
				return new LiquidsInMenu.Opening(panel, at);
			}
		});
		return InteractionResult.SUCCESS;
	}

	/**
	 * An inlet: what comes in goes on into the station's buffer tank; it counts what came and what was turned away (kept
	 * with the transaction: a pipe's trial inserts that are rolled back don't count).
	 */
	private final class Inlet extends SnapshotParticipant<Inlet.Counts> implements InsertionOnlyStorage<FluidVariant> {
		record Counts(long count, int refused, FluidVariant last, long fresh) {
		}

		long count;
		int refused;
		FluidVariant last = FluidVariant.blank();
		/** What came in since the last commit (droplets), told to the station's inflow on the commit. */
		long fresh;

		@Override
		public long insert(FluidVariant resource, long maxAmount, TransactionContext transaction) {
			FillerBlockEntity f = filler();
			if (f == null || off) {
				return 0;
			}
			long moved = f.buffer.insert(resource, maxAmount, transaction);
			updateSnapshots(transaction);
			count += moved;
			fresh += moved;
			last = resource;
			if (moved < maxAmount) {
				refused++;
			}
			return moved;
		}

		@Override
		protected Counts createSnapshot() {
			return new Counts(count, refused, last, fresh);
		}

		@Override
		protected void readSnapshot(Counts c) {
			count = c.count();
			refused = c.refused();
			last = c.last();
			fresh = c.fresh();
		}

		@Override
		protected void onFinalCommit() {
			FillerBlockEntity f = filler();
			if (f != null && fresh > 0) {
				f.counted(fresh);
			}
			fresh = 0;
		}
	}

	/**
	 * A funnel slot's container as a fluid item's context: an emptied container that can't stay in its slot (a stack
	 * of full ones) goes into another of the funnel's slots.
	 */
	private record FunnelSlot(ContainerStorage slots, int main) implements ContainerItemContext {
		@Override
		public SingleSlotStorage<ItemVariant> getMainSlot() {
			return slots.getSlot(main);
		}

		@Override
		public long insertOverflow(ItemVariant item, long maxAmount, TransactionContext transaction) {
			long n = 0;
			for (int j = 0; j < SLOTS && n < maxAmount; j++) {
				if (j != main) {
					n += slots.getSlot(j).insert(item, maxAmount - n, transaction);
				}
			}
			return n;
		}

		@Override
		public List<SingleSlotStorage<ItemVariant>> getAdditionalSlots() {
			return List.of();
		}
	}

	private void sync() {
		setChanged();
		if (level != null) {
			level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
		}
	}

	@Override
	public void dropContents() {
		if (level != null) {
			Containers.dropContents(level, worldPosition, funnel);
			funnel.clearContent();
		}
	}

	@Override
	public void preRemoveSideEffects(BlockPos pos, BlockState state) {
		dropContents();
	}

	@Override
	public Packet<ClientGamePacketListener> getUpdatePacket() {
		return ClientboundBlockEntityDataPacket.create(this);
	}

	@Override
	public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
		CompoundTag tag = saveWithoutMetadata(registries);
		tag.remove("Items");
		return tag;
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		linked = input.getBooleanOr("linked", false);
		filler = input.read("filler", BlockPos.CODEC).orElse(null);
		for (int i = 0; i < INLETS; i++) {
			rate[i] = input.getLongOr("rate" + i, 0);
			waits[i] = input.getBooleanOr("waits" + i, false);
			last[i] = input.read("last" + i, FluidVariant.CODEC).orElseGet(FluidVariant::blank);
		}
		NonNullList<ItemStack> items = NonNullList.withSize(SLOTS, ItemStack.EMPTY);
		ContainerHelper.loadAllItems(input, items);
		for (int i = 0; i < SLOTS; i++) {
			funnel.setItem(i, items.get(i));
		}
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		output.putBoolean("linked", linked);
		if (filler != null) {
			output.store("filler", BlockPos.CODEC, filler);
		}
		for (int i = 0; i < INLETS; i++) {
			output.putLong("rate" + i, rate[i]);
			output.putBoolean("waits" + i, waits[i]);
			if (!last[i].isBlank()) {
				output.store("last" + i, FluidVariant.CODEC, last[i]);
			}
		}
		NonNullList<ItemStack> items = NonNullList.withSize(SLOTS, ItemStack.EMPTY);
		for (int i = 0; i < SLOTS; i++) {
			items.set(i, funnel.getItem(i));
		}
		ContainerHelper.saveAllItems(output, items);
	}
}
