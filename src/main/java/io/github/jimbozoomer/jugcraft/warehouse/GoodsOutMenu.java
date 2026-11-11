package io.github.jimbozoomer.jugcraft.warehouse;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

/**
 * The Goods Out panel's screens (part W2; client/warehouse/GoodsOutScreen.java), one menu with three pages: the
 * pickup window's READY STORAGE (Ready Storage's slots, take only, MINE or ALL, a search, TAKE ALL; the player's
 * inventory under them), the kiosk's REQUEST (no slots: the stock, an amount, where to, the line) and the OUTPUT PORT
 * (its mode, buffer, streams; the player's inventory, to pick a stream's item). While it is open the server sends
 * its status (GoodsOutPayload) twice a second, and to the kiosk the terminal's stock.
 */
public class GoodsOutMenu extends AbstractContainerMenu {
	public static final int WIDTH = 352, HEIGHT = 280, VIEW_X = 13, VIEW_Y = 60, INV_X = 13, INV_Y = 197, HOTBAR_Y = 255;
	public static final int ROWS = 6, COLS = 9, VIEW = ROWS * COLS;
	public static final int SHOW_ALL = 0, SHOW_MINE = 1, TAKE_ALL = 2, UP = 3, DOWN = 4;
	public final BlockPos panel, terminal;
	public final int page;
	private final ReadyPool.View view = new ReadyPool.View(VIEW);
	private final Player player;
	private final int inventoryStart;
	private boolean mine;
	private int offset;
	private String search = "";
	private int ticks;

	/** What the client needs to open it: the panel, its terminal, which page. */
	public record Opening(BlockPos panel, BlockPos terminal, int page) {
		public static final StreamCodec<RegistryFriendlyByteBuf, Opening> STREAM_CODEC = StreamCodec.composite(
				BlockPos.STREAM_CODEC, Opening::panel, BlockPos.STREAM_CODEC, Opening::terminal, ByteBufCodecs.VAR_INT, Opening::page, Opening::new);
	}

	public GoodsOutMenu(int id, Inventory inventory, Opening opening) {
		super(JugcraftWarehouse.GOODS_OUT_MENU, id);
		this.panel = opening.panel();
		this.terminal = opening.terminal();
		this.page = opening.page();
		this.player = inventory.player;
		if (page == GoodsOutBlockEntity.WINDOW) {
			for (int r = 0; r < ROWS; r++) {
				for (int c = 0; c < COLS; c++) {
					addSlot(new Slot(view, r * COLS + c, VIEW_X + c * 18, VIEW_Y + r * 18) {
						@Override
						public boolean mayPlace(ItemStack stack) {
							return false;
						}
					});
				}
			}
		}
		inventoryStart = slots.size();
		if (page != GoodsOutBlockEntity.KIOSK) {
			for (int r = 0; r < 3; r++) {
				for (int c = 0; c < 9; c++) {
					addSlot(new Slot(inventory, 9 + r * 9 + c, INV_X + c * 18, INV_Y + r * 18));
				}
			}
			for (int c = 0; c < 9; c++) {
				addSlot(new Slot(inventory, c, INV_X + c * 18, HOTBAR_Y));
			}
		}
		if (player instanceof ServerPlayer && page == GoodsOutBlockEntity.WINDOW && player.level().getBlockEntity(panel) instanceof GoodsOutBlockEntity goods) {
			goods.viewer(true);
		}
		if (player instanceof ServerPlayer) {
			// So the first slot sync has the pool's stacks in it.
			refresh();
		}
	}

	public boolean mine() {
		return mine;
	}

	@Override
	public void removed(Player player) {
		super.removed(player);
		if (player instanceof ServerPlayer && page == GoodsOutBlockEntity.WINDOW && player.level().getBlockEntity(panel) instanceof GoodsOutBlockEntity goods) {
			goods.viewer(false);
		}
	}

	@Override
	public boolean stillValid(Player player) {
		return !player.isSpectator() && player.level().isLoaded(panel) && player.level().getBlockEntity(panel) instanceof GoodsOutBlockEntity
				&& player.distanceToSqr(Vec3.atCenterOf(panel)) <= 8 * 8;
	}

	void setSearch(String text) {
		search = text == null ? "" : text.trim().toLowerCase(Locale.ROOT);
		offset = 0;
	}

	private TerminalBlockEntity terminal() {
		return player.level().getBlockEntity(terminal) instanceof TerminalBlockEntity t ? t : null;
	}

	/** The pool's slots this screen shows (its filter), from the start. */
	private List<Integer> shown(ReadyPool pool) {
		List<Integer> out = new ArrayList<>();
		for (int i = 0; i < pool.size(); i++) {
			ItemStack s = pool.get(i);
			if (s.isEmpty() || mine && !pool.mine(i, player)) {
				continue;
			}
			if (!search.isEmpty() && !s.getHoverName().getString().toLowerCase(Locale.ROOT).contains(search)) {
				continue;
			}
			out.add(i);
		}
		return out;
	}

	private void refresh() {
		TerminalBlockEntity t = terminal();
		if (t == null || page != GoodsOutBlockEntity.WINDOW) {
			return;
		}
		List<Integer> shown = shown(t.pool());
		int rows = (shown.size() + COLS - 1) / COLS;
		offset = Math.max(0, Math.min(offset, rows - ROWS));
		int[] index = new int[VIEW];
		for (int k = 0; k < VIEW; k++) {
			int j = offset * COLS + k;
			index[k] = j < shown.size() ? shown.get(j) : -1;
		}
		view.show(t.pool(), index, t::setChanged);
	}

	@Override
	public ItemStack quickMoveStack(Player player, int index) {
		Slot slot = slots.get(index);
		if (index >= inventoryStart || !slot.hasItem()) {
			return ItemStack.EMPTY;
		}
		ItemStack stack = slot.getItem();
		ItemStack copy = stack.copy();
		if (!moveItemStackTo(stack, inventoryStart, slots.size(), true)) {
			return ItemStack.EMPTY;
		}
		if (stack.isEmpty()) {
			slot.setByPlayer(ItemStack.EMPTY);
		} else {
			slot.setChanged();
		}
		return copy;
	}

	@Override
	public boolean clickMenuButton(Player player, int id) {
		if (!(player instanceof ServerPlayer) || !stillValid(player) || page != GoodsOutBlockEntity.WINDOW) {
			return false;
		}
		switch (id) {
			case SHOW_ALL -> {
				mine = false;
				offset = 0;
			}
			case SHOW_MINE -> {
				mine = true;
				offset = 0;
			}
			case UP -> offset = Math.max(0, offset - 1);
			case DOWN -> offset++;
			case TAKE_ALL -> {
				TerminalBlockEntity t = terminal();
				if (t != null) {
					ReadyPool pool = t.pool();
					for (int i = 0; i < pool.size(); i++) {
						if (pool.get(i).isEmpty() || !pool.mine(i, player)) {
							continue;
						}
						ItemStack s = pool.get(i);
						moveItemStackTo(s, inventoryStart, slots.size(), true);
						if (s.isEmpty()) {
							pool.set(i, ItemStack.EMPTY);
						}
					}
					t.setChanged();
				}
			}
			default -> {
				return false;
			}
		}
		refresh();
		broadcastChanges();
		return true;
	}

	@Override
	public void broadcastChanges() {
		if (player instanceof ServerPlayer server) {
			refresh();
			super.broadcastChanges();
			if (ticks++ % 10 == 0) {
				TerminalBlockEntity t = terminal();
				if (t != null) {
					ServerPlayNetworking.send(server, status(server, t));
					if (page == GoodsOutBlockEntity.KIOSK && ticks % 20 == 1) {
						ServerPlayNetworking.send(server, t.snapshot(server.level().getGameTime()));
					}
				}
			}
			return;
		}
		super.broadcastChanges();
	}

	private GoodsOutPayload status(ServerPlayer server, TerminalBlockEntity t) {
		long now = server.level().getGameTime();
		int mineStacks = 0;
		ReadyPool pool = t.pool();
		for (int i = 0; i < pool.size(); i++) {
			if (!pool.get(i).isEmpty() && pool.mine(i, server)) {
				mineStacks++;
			}
		}
		boolean trayOut = false, piped = false;
		int flow = 0;
		if (server.level().getBlockEntity(panel) instanceof GoodsOutBlockEntity goods) {
			trayOut = goods.trayOut();
			piped = goods.piped();
			flow = goods.flow();
		}
		String next = "";
		for (Order o : t.orders()) {
			if (o.open() && o.dest == Order.WINDOW && o.stream == 0 && !Drums.isLiquid(o.item)) {
				next = "#" + o.id + " " + Order.STATES[o.state];
				break;
			}
		}
		List<GoodsOutPayload.StreamLine> streams = new ArrayList<>();
		for (Order.Stream st : t.streams()) {
			streams.add(new GoodsOutPayload.StreamLine(st.item, st.rate, st.reserve, st.held));
		}
		List<TerminalPayload.OrderLine> lines = new ArrayList<>();
		for (TerminalPayload.OrderLine o : t.orderLines(now)) {
			if (!Drums.isLiquid(o.item())) {
				lines.add(o);
			}
		}
		return new GoodsOutPayload(panel, lines, pool.used(), pool.capacity(), mineStacks, trayOut, next, t.portMode(),
				t.port().stacks(), streams, flow, piped, server.level().getOverworldClockTime(), mine, (offset));
	}
}
