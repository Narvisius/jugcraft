package io.github.jimbozoomer.jugcraft.warehouse;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import net.fabricmc.fabric.api.menu.v1.ExtendedMenuProvider;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
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
 * The Goods Out panel set in the outside wall in front of Ready Storage's access side (part W2): the pickup window
 * (its GOODS OUT screen: Ready Storage's slots; the tray slides out while someone has it open), the order kiosk (its
 * REQUEST screen; its own screen lists the open orders), the green beacon (lit while an order waits to be collected)
 * and the Output Port's OUT flange, which pushes the port's buffer into the pipe on it (orders sent to PIPES, the
 * streams at their rates) and lets extractors pull from it.
 */
public class GoodsOutBlockEntity extends BlockEntity implements PieceEntity {
	public static final int WINDOW = 0, KIOSK = 1, PORT = 2;
	/** Items the port pushes at most each half second. */
	public static final int PUSH = 32;
	private @Nullable BlockPos storage;
	private long nextLink;
	private int viewers;
	// What the clients see.
	private boolean trayOut, linked, ready, flowing, piped;
	private long traySince;
	private final List<KioskLine> kiosk = new ArrayList<>();
	private int flow;
	private int pushedThisSecond;

	/** An order on the kiosk's own screen: its number, item, amount, state. */
	public record KioskLine(int id, ItemStack item, long amount, int state) {
		static final com.mojang.serialization.Codec<KioskLine> CODEC = com.mojang.serialization.codecs.RecordCodecBuilder.create(i -> i.group(
				com.mojang.serialization.Codec.INT.fieldOf("id").forGetter(KioskLine::id), ItemStack.CODEC.fieldOf("item").forGetter(KioskLine::item),
				com.mojang.serialization.Codec.LONG.fieldOf("amount").forGetter(KioskLine::amount),
				com.mojang.serialization.Codec.INT.fieldOf("state").forGetter(KioskLine::state)).apply(i, KioskLine::new));
	}

	public GoodsOutBlockEntity(BlockPos pos, BlockState state) {
		super(JugcraftWarehouse.GOODS_OUT_ENTITY, pos, state);
	}

	public boolean trayOut() {
		return trayOut;
	}

	public long traySince() {
		return traySince;
	}

	public boolean linked() {
		return linked;
	}

	public boolean ready() {
		return ready;
	}

	public boolean flowing() {
		return flowing;
	}

	public boolean piped() {
		return piped;
	}

	public int flow() {
		return flow;
	}

	public List<KioskLine> kiosk() {
		return kiosk;
	}

	public @Nullable BlockPos storagePos() {
		return storage;
	}

	Direction facing() {
		BlockState s = getBlockState();
		return s.getBlock() instanceof PieceBlock ? s.getValue(PieceBlock.FACING) : Direction.NORTH;
	}

	/** The block of the panel with the OUT flange (its right column, bottom), and the way out of it (outside). */
	public BlockPos flange() {
		return worldPosition.offset(WarehousePiece.GOODS_OUT.offset(facing(), WarehousePiece.GOODS_OUT.part(2, 0, 0)));
	}

	public @Nullable TerminalBlockEntity terminal() {
		if (level == null) {
			return null;
		}
		TerminalBlockEntity t = storage != null ? Warehouses.terminalOf(level, storage) : null;
		return t != null ? t : Warehouses.terminalOf(level, worldPosition);
	}

	/** Ready Storage against the wall's inside behind the panel (any of its blocks behind any of the panel's). */
	private @Nullable BlockPos findStorage() {
		if (level == null || !(getBlockState().getBlock() instanceof PieceBlock piece)) {
			return null;
		}
		Direction facing = facing();
		for (int p = 0; p < piece.piece.parts(); p++) {
			BlockPos behind = worldPosition.offset(piece.piece.offset(facing, p)).relative(facing);
			BlockState s = level.getBlockState(behind);
			if (s.getBlock() instanceof PieceBlock other && other.piece == WarehousePiece.READY_STORAGE) {
				return other.origin(behind, s);
			}
		}
		return null;
	}

	void viewer(boolean opened) {
		viewers = Math.max(0, viewers + (opened ? 1 : -1));
		boolean out = viewers > 0;
		if (out != trayOut && level != null) {
			trayOut = out;
			traySince = level.getGameTime();
			sync();
		}
	}

	public void serverTick(ServerLevel level) {
		long now = level.getGameTime();
		if (now >= nextLink) {
			nextLink = now + 20;
			storage = findStorage();
		}
		TerminalBlockEntity t = terminal();
		if (now % 10 == 0 && t != null && t.portMode() != TerminalBlockEntity.PORT_OFF) {
			push(level, t);
		}
		if (now % 20 == 0) {
			boolean changed = false;
			if (viewers == 0 && trayOut) {
				trayOut = false;
				traySince = now;
				changed = true;
			}
			boolean l = storage != null && t != null;
			boolean r = false;
			List<KioskLine> lines = new ArrayList<>();
			if (t != null) {
				for (Order o : t.orders()) {
					if (Drums.isLiquid(o.item)) {
						continue;
					}
					if (o.state == Order.READY) {
						r = true;
					}
					if (o.live() && o.stream == 0 && lines.size() < 3) {
						lines.add(new KioskLine(o.id, o.item.copyWithCount(1), o.amount, o.state));
					}
				}
			}
			boolean pipe = level.getBlockState(flange().relative(facing().getOpposite())).getBlock() instanceof io.github.jimbozoomer.jugcraft.logistics.ItemPipeBlock;
			boolean fl = pushedThisSecond > 0;
			int rate = pushedThisSecond;
			pushedThisSecond = 0;
			changed |= l != linked || r != ready || fl != flowing || pipe != piped || rate != flow || !same(lines);
			linked = l;
			ready = r;
			flowing = fl;
			piped = pipe;
			flow = rate;
			if (!same(lines)) {
				kiosk.clear();
				kiosk.addAll(lines);
			}
			if (changed) {
				sync();
			}
		}
	}

	private boolean same(List<KioskLine> lines) {
		if (lines.size() != kiosk.size()) {
			return false;
		}
		for (int i = 0; i < lines.size(); i++) {
			KioskLine a = lines.get(i), b = kiosk.get(i);
			if (a.id() != b.id() || a.state() != b.state()) {
				return false;
			}
		}
		return true;
	}

	/** The port pushes its buffer into the pipe on the OUT flange: orders' items freely, streams' items at their rates. */
	private void push(ServerLevel level, TerminalBlockEntity t) {
		PortBuffer port = t.port();
		if (port.used() == 0) {
			return;
		}
		List<Order.Stream> streams = t.streams();
		BlockPos at = flange();
		Set<BlockPos> exclude = Set.of(worldPosition, at);
		// Orders' items first (anything no stream is for), then each stream's, no more than it has earned.
		Storage<ItemVariant> orders = port.output(v -> {
			for (Order.Stream st : streams) {
				if (v.matches(st.item)) {
					return false;
				}
			}
			return true;
		});
		long moved = io.github.jimbozoomer.jugcraft.logistics.ItemNetworks.push(level, at, facing().getOpposite(), orders, PUSH, exclude);
		for (Order.Stream st : streams) {
			long max = Math.min(PUSH - moved, (long) st.allowance);
			if (max <= 0) {
				continue;
			}
			long out = io.github.jimbozoomer.jugcraft.logistics.ItemNetworks.push(level, at, facing().getOpposite(), port.output(v -> v.matches(st.item)), max, exclude);
			st.allowance = Math.max(0, st.allowance - out);
			moved += out;
		}
		if (moved > 0) {
			port.tidy();
			pushedThisSecond += (int) moved;
			t.setChanged();
		}
	}

	/** For extractors on the OUT flange: the port's buffer, to take from. */
	public @Nullable Storage<ItemVariant> output() {
		TerminalBlockEntity t = terminal();
		if (t == null || t.portMode() == TerminalBlockEntity.PORT_OFF) {
			return null;
		}
		PortBuffer port = t.port();
		return port.output(v -> true);
	}

	@Override
	public InteractionResult use(Level level, BlockPos clicked, Player player, ItemStack stack, InteractionHand hand) {
		if (level.isClientSide()) {
			return InteractionResult.SUCCESS;
		}
		storage = findStorage();
		TerminalBlockEntity t = terminal();
		if (storage == null || t == null) {
			player.sendOverlayMessage(Component.translatable(storage == null ? "message.jugcraft.warehouse.no_ready_storage" : "message.jugcraft.warehouse.no_terminal"));
			return InteractionResult.SUCCESS;
		}
		BlockState s = level.getBlockState(clicked);
		int column = s.getBlock() instanceof PieceBlock piece ? piece.piece.column(piece.part(s)) : 1;
		open(player, column == 0 ? KIOSK : column == 2 && piece(s, clicked) ? PORT : WINDOW);
		return InteractionResult.SUCCESS;
	}

	/** The right column's bottom block is the OUT flange's (its top block is the window's side). */
	private boolean piece(BlockState s, BlockPos clicked) {
		return s.getBlock() instanceof PieceBlock piece && piece.piece.row(piece.part(s)) == 0;
	}

	/** Opens one of the panel's screens for a player (also from the screens' tabs). */
	public void open(Player player, int page) {
		TerminalBlockEntity t = terminal();
		if (t == null) {
			return;
		}
		BlockPos panel = worldPosition, terminal = t.getBlockPos();
		player.openMenu(new ExtendedMenuProvider<GoodsOutMenu.Opening>() {
			@Override
			public Component getDisplayName() {
				return Component.translatable(page == KIOSK ? "container.jugcraft.goods_out_kiosk" : page == PORT ? "container.jugcraft.output_port"
						: "container.jugcraft.goods_out");
			}

			@Override
			public AbstractContainerMenu createMenu(int id, Inventory inventory, Player p) {
				return new GoodsOutMenu(id, inventory, new GoodsOutMenu.Opening(panel, terminal, page));
			}

			@Override
			public GoodsOutMenu.Opening getScreenOpeningData(ServerPlayer p) {
				return new GoodsOutMenu.Opening(panel, terminal, page);
			}
		});
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
		trayOut = input.getBooleanOr("tray_out", false);
		traySince = input.getLongOr("tray_since", 0);
		linked = input.getBooleanOr("linked", false);
		ready = input.getBooleanOr("ready", false);
		flowing = input.getBooleanOr("flowing", false);
		piped = input.getBooleanOr("piped", false);
		flow = input.getIntOr("flow", 0);
		kiosk.clear();
		kiosk.addAll(input.read("kiosk", KioskLine.CODEC.listOf()).orElse(List.of()));
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		output.putBoolean("tray_out", trayOut);
		output.putLong("tray_since", traySince);
		output.putBoolean("linked", linked);
		output.putBoolean("ready", ready);
		output.putBoolean("flowing", flowing);
		output.putBoolean("piped", piped);
		output.putInt("flow", flow);
		output.store("kiosk", KioskLine.CODEC.listOf(), kiosk);
	}
}
