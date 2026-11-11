package io.github.jimbozoomer.jugcraft.warehouse;

import io.github.jimbozoomer.jugcraft.fluid.FluidNetworks;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.fabricmc.fabric.api.menu.v1.ExtendedMenuProvider;
import net.fabricmc.fabric.api.transfer.v1.context.ContainerItemContext;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.item.PlayerInventoryStorage;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageUtil;
import net.fabricmc.fabric.api.transfer.v1.storage.base.SingleSlotStorage;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
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
 * The Liquids Out panel (part W4) set in the outside wall in front of a Drum Emptying Station: the OUT flange (its left
 * column, bottom) pushes what the holding tank holds for orders sent to pipes, and the streams at their rates, into the
 * pipe on it (extractors may pull it too); the fill point (its middle) fills a bucket (or any liquid container) used on
 * it from what is ready for the fill point, the user's own orders first. Its screen (LIQUIDS OUT) lists the user's
 * liquid orders, fills all their empty containers at once, sets the flange's mode and the streams.
 */
public class LiquidsOutBlockEntity extends BlockEntity implements PieceEntity {
	/** Droplets the flange pushes at most each half second. */
	public static final long PUSH = Drums.droplets(10_000);
	private @Nullable BlockPos emptier;
	private long nextLink;
	private long pushedThisSecond;
	// An extractor's outermost transaction taking from the flange now (SettlingOut), and the tank's amount before it.
	private @Nullable TransactionContext settling;
	private @Nullable EmptierBlockEntity settlingTank;
	private long settlingBefore;
	// What the clients see.
	private boolean linked, flowing;
	private long flow;
	private FluidVariant flowLiquid = FluidVariant.blank();

	public LiquidsOutBlockEntity(BlockPos pos, BlockState state) {
		super(JugcraftWarehouse.LIQUIDS_OUT_ENTITY, pos, state);
	}

	public boolean linked() {
		return linked;
	}

	public boolean flowing() {
		return flowing;
	}

	/** What the flange sends (mB a second) and what. */
	public long flow() {
		return flow;
	}

	public FluidVariant flowLiquid() {
		return flowLiquid;
	}

	public @Nullable EmptierBlockEntity emptier() {
		return emptier != null && level != null && level.isLoaded(emptier) && level.getBlockEntity(emptier) instanceof EmptierBlockEntity e ? e : null;
	}

	public @Nullable BlockPos emptierPos() {
		return emptier;
	}

	public Direction facing() {
		BlockState s = getBlockState();
		return s.getBlock() instanceof PieceBlock ? s.getValue(PieceBlock.FACING) : Direction.NORTH;
	}

	/** The OUT flange's block (the left column, bottom). */
	public BlockPos flange() {
		return worldPosition.offset(WarehousePiece.LIQUIDS_OUT.offset(facing(), WarehousePiece.LIQUIDS_OUT.part(0, 0, 0)));
	}

	public @Nullable TerminalBlockEntity terminal() {
		if (level == null) {
			return null;
		}
		TerminalBlockEntity t = emptier != null ? Warehouses.terminalOf(level, emptier) : null;
		return t != null ? t : Warehouses.terminalOf(level, worldPosition);
	}

	private @Nullable BlockPos findEmptier() {
		if (level == null || !(getBlockState().getBlock() instanceof PieceBlock piece)) {
			return null;
		}
		Direction facing = facing();
		for (int p = 0; p < piece.piece.parts(); p++) {
			BlockPos behind = worldPosition.offset(piece.piece.offset(facing, p)).relative(facing);
			BlockState s = level.getBlockState(behind);
			if (s.getBlock() instanceof PieceBlock other && other.piece == WarehousePiece.DRUM_EMPTIER) {
				return other.origin(behind, s);
			}
		}
		return null;
	}

	public void serverTick(ServerLevel level) {
		long now = level.getGameTime();
		if (now >= nextLink) {
			nextLink = now + 20;
			emptier = findEmptier();
		}
		TerminalBlockEntity t = terminal();
		if (now % 10 == 0 && t != null && t.liquidMode() != TerminalBlockEntity.PORT_OFF) {
			push(level, t);
		}
		if (now % 20 == 0) {
			boolean l = emptier() != null && t != null;
			long rate = Drums.mb(pushedThisSecond);
			pushedThisSecond = 0;
			boolean fl = rate > 0;
			boolean changed = l != linked || fl != flowing || rate != flow;
			linked = l;
			flowing = fl;
			flow = rate;
			EmptierBlockEntity e = emptier();
			FluidVariant liquid = e == null ? FluidVariant.blank() : e.liquid();
			if (!liquid.equals(flowLiquid)) {
				flowLiquid = liquid;
				changed = true;
			}
			if (changed) {
				sync();
			}
		}
	}

	/**
	 * What may come out of the tank for some shares, as a storage to take from (no more than cap droplets, and no more
	 * than they hold): the shares are let go of once it has been taken (settle).
	 */
	final class Out implements SingleSlotStorage<FluidVariant> {
		final EmptierBlockEntity e;
		final List<EmptierBlockEntity.Share> shares;
		final long before, cap;

		Out(EmptierBlockEntity e, List<EmptierBlockEntity.Share> shares, long cap) {
			this.e = e;
			this.shares = shares;
			long held = 0;
			for (EmptierBlockEntity.Share s : shares) {
				held += s.amount;
			}
			this.before = e.tank.amount;
			this.cap = Math.min(cap, held);
		}

		private long left() {
			return Math.max(0, cap - (before - e.tank.amount));
		}

		@Override
		public long insert(FluidVariant resource, long maxAmount, TransactionContext transaction) {
			return 0;
		}

		@Override
		public long extract(FluidVariant resource, long maxAmount, TransactionContext transaction) {
			long n = Math.min(maxAmount, left());
			return n <= 0 ? 0 : e.tank.extract(resource, n, transaction);
		}

		@Override
		public boolean isResourceBlank() {
			return e.tank.isResourceBlank() || left() <= 0;
		}

		@Override
		public FluidVariant getResource() {
			return e.tank.variant;
		}

		@Override
		public long getAmount() {
			return Math.min(left(), e.tank.amount);
		}

		@Override
		public long getCapacity() {
			return e.tank.getCapacity();
		}

		/** Lets go of the shares for what was taken; returns it (droplets). */
		long settle() {
			long taken = before - e.tank.amount;
			if (taken > 0) {
				e.taken(shares, taken);
			}
			return taken;
		}
	}

	/** The flange pushes the tank's pipe orders' liquid into the pipe on it freely, the streams' at their rates. */
	private void push(ServerLevel level, TerminalBlockEntity t) {
		EmptierBlockEntity e = emptier();
		if (e == null || e.tank.isResourceBlank()) {
			return;
		}
		BlockPos at = flange();
		List<Direction> out = List.of(facing().getOpposite());
		long moved = 0;
		Out orders = new Out(e, ordersOnly(e.sharesFor(Order.PIPES, null, -1)), PUSH);
		FluidNetworks.pushToNeighbors(level, at, orders, PUSH, out);
		moved += orders.settle();
		if (t.liquidMode() == TerminalBlockEntity.PORT_STREAMS) {
			List<Order.Stream> streams = t.liquidStreams();
			for (int k = 0; k < streams.size(); k++) {
				Order.Stream st = streams.get(k);
				long max = Math.min(PUSH - moved, Drums.droplets((long) st.allowance));
				if (max <= 0) {
					continue;
				}
				Out stream = new Out(e, e.sharesFor(Order.PIPES, null, TerminalBlockEntity.LIQUID_STREAM + k + 1), max);
				FluidNetworks.pushToNeighbors(level, at, stream, max, out);
				long n = stream.settle();
				st.allowance = Math.max(0, st.allowance - Drums.mb(n));
				moved += n;
			}
		}
		if (moved > 0) {
			pushedThisSecond += moved;
			t.setChanged();
		}
	}

	private static List<EmptierBlockEntity.Share> ordersOnly(List<EmptierBlockEntity.Share> shares) {
		List<EmptierBlockEntity.Share> out = new ArrayList<>();
		for (EmptierBlockEntity.Share s : shares) {
			if (s.stream == 0) {
				out.add(s);
			}
		}
		return out;
	}

	/** For extractors on the OUT flange: the pipe orders' liquid in the tank, to take from. */
	public @Nullable Storage<FluidVariant> output() {
		TerminalBlockEntity t = terminal();
		EmptierBlockEntity e = emptier();
		if (t == null || e == null || t.liquidMode() == TerminalBlockEntity.PORT_OFF) {
			return null;
		}
		return new SettlingOut(e);
	}

	/**
	 * An extractor's view of the flange: what it takes comes off the pipe orders' shares once its outermost transaction
	 * is committed, by what the tank really lost in it (a pipe's trial extracts in aborted nested transactions don't
	 * count), and no more than the shares hold is let out in one transaction however many extracts it makes.
	 */
	private final class SettlingOut implements SingleSlotStorage<FluidVariant> {
		private final EmptierBlockEntity e;

		SettlingOut(EmptierBlockEntity e) {
			this.e = e;
		}

		/** The pipe orders' shares less what has gone in the outer transaction now taking from the flange (if any). */
		private long left() {
			long held = 0;
			for (EmptierBlockEntity.Share s : ordersOnly(e.sharesFor(Order.PIPES, null, -1))) {
				held += s.amount;
			}
			long gone = settling != null && settlingTank == e ? Math.max(0, settlingBefore - e.tank.amount) : 0;
			return Math.max(0, held - gone);
		}

		@Override
		public long insert(FluidVariant resource, long maxAmount, TransactionContext transaction) {
			return 0;
		}

		@Override
		public long extract(FluidVariant resource, long maxAmount, TransactionContext transaction) {
			TransactionContext outer = transaction.getOpenTransaction(0);
			if (settling != outer) {
				settling = outer;
				settlingTank = e;
				settlingBefore = e.tank.amount;
				transaction.addOuterCloseCallback(result -> {
					long taken = settlingBefore - e.tank.amount;
					settling = null;
					settlingTank = null;
					if (result.wasCommitted() && taken > 0) {
						e.taken(ordersOnly(e.sharesFor(Order.PIPES, null, -1)), taken);
						pushedThisSecond += taken;
					}
				});
			}
			long n = Math.min(maxAmount, left());
			return n <= 0 ? 0 : e.tank.extract(resource, n, transaction);
		}

		@Override
		public boolean isResourceBlank() {
			return getAmount() <= 0;
		}

		@Override
		public FluidVariant getResource() {
			return e.tank.variant;
		}

		@Override
		public long getAmount() {
			return Math.min(left(), e.tank.amount);
		}

		@Override
		public long getCapacity() {
			return e.tank.getCapacity();
		}
	}

	/** Fills a container from what is ready for the fill point (the owner's own orders first); returns droplets. */
	public long fill(Storage<FluidVariant> into, @Nullable UUID owner) {
		EmptierBlockEntity e = emptier();
		if (e == null || e.tank.isResourceBlank()) {
			return 0;
		}
		Out out = new Out(e, e.sharesFor(Order.WINDOW, owner, -1), Long.MAX_VALUE);
		try (Transaction t = Transaction.openOuter()) {
			StorageUtil.move(out, into, v -> true, Long.MAX_VALUE, t);
			t.commit();
		}
		return out.settle();
	}

	/** Fills every empty container in a player's inventory it can (FILL MY BUCKETS); returns droplets. */
	public long fillAll(Player player) {
		EmptierBlockEntity e = emptier();
		if (e == null) {
			return 0;
		}
		long total = 0;
		PlayerInventoryStorage inv = PlayerInventoryStorage.of(player);
		for (int i = 0; i < player.getInventory().getContainerSize() && i < 36; i++) {
			for (int tries = 0; tries < 64; tries++) {
				ItemStack s = player.getInventory().getItem(i);
				if (s.isEmpty()) {
					break;
				}
				Storage<FluidVariant> held = FluidStorage.ITEM.find(s, ContainerItemContext.ofPlayerSlot(player, inv.getSlots().get(i)));
				if (held == null) {
					break;
				}
				long n = fill(held, player.getUUID());
				if (n <= 0) {
					break;
				}
				total += n;
			}
		}
		if (total > 0) {
			done(player, total);
		}
		return total;
	}

	private void done(Player player, long droplets) {
		if (level != null) {
			level.playSound(null, worldPosition, SoundEvents.BUCKET_FILL, SoundSource.BLOCKS, 0.8F, 1.0F);
		}
		EmptierBlockEntity e = emptier();
		player.sendOverlayMessage(Component.translatable("message.jugcraft.warehouse.filled_bucket",
				Drums.buckets(Drums.mb(droplets)) + " " + (e == null || e.liquid().isBlank() ? Drums.name(flowLiquid).getString() : Drums.name(e.liquid()).getString())));
	}

	@Override
	public InteractionResult use(Level level, BlockPos clicked, Player player, ItemStack stack, InteractionHand hand) {
		if (level.isClientSide()) {
			return InteractionResult.SUCCESS;
		}
		emptier = findEmptier();
		TerminalBlockEntity t = terminal();
		if (emptier == null || t == null) {
			player.sendOverlayMessage(Component.translatable(emptier == null ? "message.jugcraft.warehouse.no_emptier" : "message.jugcraft.warehouse.no_terminal"));
			return InteractionResult.SUCCESS;
		}
		BlockState s = level.getBlockState(clicked);
		int column = s.getBlock() instanceof PieceBlock piece ? piece.piece.column(piece.part(s)) : 1;
		// The fill point: a liquid container held under the nozzle is filled.
		Storage<FluidVariant> held = stack.isEmpty() ? null : FluidStorage.ITEM.find(stack, ContainerItemContext.forPlayerInteraction(player, hand));
		if (column == 1 && held != null) {
			long n = fill(held, player.getUUID());
			if (n > 0) {
				done(player, n);
			} else {
				player.sendOverlayMessage(Component.translatable("message.jugcraft.warehouse.nothing_to_fill"));
			}
			return InteractionResult.SUCCESS;
		}
		BlockPos panel = worldPosition, terminal = t.getBlockPos();
		player.openMenu(new ExtendedMenuProvider<LiquidsOutMenu.Opening>() {
			@Override
			public Component getDisplayName() {
				return Component.translatable("container.jugcraft.liquids_out");
			}

			@Override
			public AbstractContainerMenu createMenu(int id, Inventory inventory, Player p) {
				return new LiquidsOutMenu(id, inventory, new LiquidsOutMenu.Opening(panel, terminal));
			}

			@Override
			public LiquidsOutMenu.Opening getScreenOpeningData(ServerPlayer p) {
				return new LiquidsOutMenu.Opening(panel, terminal);
			}
		});
		return InteractionResult.SUCCESS;
	}

	private void sync() {
		setChanged();
		if (level != null) {
			level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
		}
	}

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
		linked = input.getBooleanOr("linked", false);
		emptier = input.read("emptier", BlockPos.CODEC).orElse(null);
		flowing = input.getBooleanOr("flowing", false);
		flow = input.getLongOr("flow", 0);
		flowLiquid = input.read("flow_liquid", FluidVariant.CODEC).orElseGet(FluidVariant::blank);
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		output.putBoolean("linked", linked);
		if (emptier != null) {
			output.store("emptier", BlockPos.CODEC, emptier);
		}
		output.putBoolean("flowing", flowing);
		output.putLong("flow", flow);
		if (!flowLiquid.isBlank()) {
			output.store("flow_liquid", FluidVariant.CODEC, flowLiquid);
		}
	}
}
