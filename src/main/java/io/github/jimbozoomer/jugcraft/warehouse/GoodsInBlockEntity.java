package io.github.jimbozoomer.jugcraft.warehouse;

import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.base.InsertionOnlyStorage;
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
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

/**
 * The Goods In panel set in the outside wall: its four pipe inlets (IN 1 to IN 4, on its side blocks' faces) and the
 * drop-off hatch feed the Intake Case Packer standing against the wall's inside behind it. It keeps each inlet's flow
 * (what came in last, items a second, backed up when the intake's bay was full) for its lamps and the GOODS IN screen.
 */
public class GoodsInBlockEntity extends BlockEntity implements PieceEntity {
	public static final int INLETS = 4;
	private final Inlet[] inlets = new Inlet[INLETS];
	private @Nullable BlockPos intake;
	private long nextLink;
	// What the clients see (lamps, the hatch's screen).
	private final int[] rate = new int[INLETS];
	private final boolean[] backed = new boolean[INLETS];
	private final ItemStack[] last = new ItemStack[INLETS];
	private boolean linked;
	/** Items waiting in the intake's bay, cases on its way, items stored in the warehouse (for the hatch's screen). */
	private int waiting, cases;
	private long stored;

	public GoodsInBlockEntity(BlockPos pos, BlockState state) {
		super(JugcraftWarehouse.GOODS_IN_ENTITY, pos, state);
		for (int i = 0; i < INLETS; i++) {
			inlets[i] = new Inlet();
			last[i] = ItemStack.EMPTY;
		}
	}

	/** Which inlet a part of the panel is (IN 1 bottom left, IN 2 bottom right, IN 3 top left, IN 4 top right), or -1. */
	public static int inletOf(WarehousePiece piece, int part) {
		int c = piece.column(part), r = piece.row(part);
		if (c == 1) {
			return -1;
		}
		return r * 2 + (c == 0 ? 0 : 1);
	}

	public @Nullable InsertionOnlyStorage<ItemVariant> inlet(int i) {
		return i >= 0 && i < INLETS ? inlets[i] : null;
	}

	public int rate(int i) {
		return rate[i];
	}

	public boolean backed(int i) {
		return backed[i];
	}

	public ItemStack last(int i) {
		return last[i];
	}

	public boolean linked() {
		return linked;
	}

	public int waiting() {
		return waiting;
	}

	public int cases() {
		return cases;
	}

	public long stored() {
		return stored;
	}

	public @Nullable BlockPos intakePos() {
		return intake;
	}

	public @Nullable IntakeBlockEntity intake() {
		return intake != null && level != null && level.isLoaded(intake) && level.getBlockEntity(intake) instanceof IntakeBlockEntity in ? in : null;
	}

	Direction facing() {
		BlockState s = getBlockState();
		return s.getBlock() instanceof PieceBlock ? s.getValue(PieceBlock.FACING) : Direction.NORTH;
	}

	/** The intake against the wall's inside behind the panel (any of its blocks behind any of the panel's). */
	private @Nullable BlockPos findIntake() {
		if (level == null || !(getBlockState().getBlock() instanceof PieceBlock piece)) {
			return null;
		}
		Direction facing = facing();
		for (int p = 0; p < piece.piece.parts(); p++) {
			BlockPos behind = worldPosition.offset(piece.piece.offset(facing, p)).relative(facing);
			BlockState s = level.getBlockState(behind);
			if (s.getBlock() instanceof PieceBlock other && other.piece == WarehousePiece.INTAKE) {
				return other.origin(behind, s);
			}
		}
		return null;
	}

	public void serverTick(ServerLevel level) {
		long now = level.getGameTime();
		if (now >= nextLink) {
			nextLink = now + 20;
			intake = findIntake();
		}
		if (now % 20 == 0) {
			IntakeBlockEntity in = intake();
			boolean changed = linked != (in != null);
			linked = in != null;
			int w = 0, c = 0;
			long st = stored;
			if (in != null) {
				for (int i = 0; i < in.bay().getContainerSize(); i++) {
					w += in.bay().getItem(i).getCount();
				}
				c = in.conveyor().size() + (in.packing() == null ? 0 : 1);
				TerminalBlockEntity terminal = Warehouses.terminalOf(level, in.getBlockPos());
				st = terminal == null ? 0 : terminal.itemsIn();
			}
			changed |= w != waiting || c != cases || st != stored;
			waiting = w;
			cases = c;
			stored = st;
			for (int i = 0; i < INLETS; i++) {
				Inlet inlet = inlets[i];
				int r = inlet.count;
				boolean b = inlet.refused > 0 && inlet.count == 0;
				inlet.count = 0;
				inlet.refused = 0;
				if (r != rate[i] || b != backed[i] || !ItemStack.isSameItemSameComponents(inlet.last, last[i])) {
					changed = true;
				}
				rate[i] = r;
				backed[i] = b;
				last[i] = inlet.last.copy();
			}
			if (changed) {
				setChanged();
				level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
			}
		}
	}

	@Override
	public InteractionResult use(Level level, BlockPos clicked, Player player, ItemStack stack, InteractionHand hand) {
		if (level.isClientSide()) {
			return InteractionResult.SUCCESS;
		}
		intake = findIntake();
		IntakeBlockEntity in = intake();
		if (in == null) {
			player.sendOverlayMessage(Component.translatable("message.jugcraft.warehouse.no_intake"));
			return InteractionResult.SUCCESS;
		}
		player.openMenu(new net.fabricmc.fabric.api.menu.v1.ExtendedMenuProvider<GoodsInMenu.Opening>() {
			@Override
			public Component getDisplayName() {
				return Component.translatable("container.jugcraft.goods_in");
			}

			@Override
			public net.minecraft.world.inventory.AbstractContainerMenu createMenu(int id, net.minecraft.world.entity.player.Inventory inventory, Player p) {
				return new GoodsInMenu(id, inventory, in.bay(), in.getBlockPos(), worldPosition);
			}

			@Override
			public GoodsInMenu.Opening getScreenOpeningData(net.minecraft.server.level.ServerPlayer p) {
				return new GoodsInMenu.Opening(in.getBlockPos(), worldPosition);
			}
		});
		return InteractionResult.SUCCESS;
	}

	/** An inlet: what goes in goes on into the intake's bay; it counts what came and what was turned away. */
	private final class Inlet implements InsertionOnlyStorage<ItemVariant> {
		int count, refused;
		ItemStack last = ItemStack.EMPTY;

		@Override
		public long insert(ItemVariant resource, long maxAmount, TransactionContext transaction) {
			IntakeBlockEntity in = intake();
			if (in == null) {
				return 0;
			}
			long moved = in.input().insert(resource, maxAmount, transaction);
			transaction.addOuterCloseCallback(result -> {
				if (result.wasCommitted()) {
					if (moved > 0) {
						count += (int) moved;
						last = resource.toStack(1);
					}
					if (moved < maxAmount) {
						refused++;
					}
				}
			});
			return moved;
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
		waiting = input.getIntOr("waiting", 0);
		cases = input.getIntOr("cases", 0);
		stored = input.getLongOr("stored", 0);
		for (int i = 0; i < INLETS; i++) {
			rate[i] = input.getIntOr("rate" + i, 0);
			backed[i] = input.getBooleanOr("backed" + i, false);
			last[i] = input.read("last" + i, ItemStack.CODEC).orElse(ItemStack.EMPTY);
		}
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		output.putBoolean("linked", linked);
		output.putInt("waiting", waiting);
		output.putInt("cases", cases);
		output.putLong("stored", stored);
		for (int i = 0; i < INLETS; i++) {
			output.putInt("rate" + i, rate[i]);
			output.putBoolean("backed" + i, backed[i]);
			if (!last[i].isEmpty()) {
				output.store("last" + i, ItemStack.CODEC, last[i]);
			}
		}
	}
}
