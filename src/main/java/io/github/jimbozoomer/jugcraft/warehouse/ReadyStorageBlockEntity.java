package io.github.jimbozoomer.jugcraft.warehouse;

import com.mojang.serialization.Codec;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Containers;
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
 * Ready Storage (part W2; a vertical lift module): its dispatch hatch and pick cell. A pallet drone sets an order's
 * pallet on the roller bed (Planner PUT_BED); the shutter rolls up, the rollers run the pallet into the pick cell, the
 * shutter comes down, the cell's gantry lifts off the cases the order needs one by one (a case the order needs only
 * part of is split: lifted, part taken, put back), the shutter goes up again and what is left rolls back out onto the
 * bed for a pallet drone to take back to its place (an empty pallet goes to a station, or into Ready Storage itself
 * if no station has room). All of it runs on one timeline from the moment the pallet is set down, so the client
 * animates it from that tick and the plan; the server only acts at its end. Its contents are the warehouse's Ready
 * Storage pool, kept by the terminal (every Ready Storage of a warehouse shares it).
 */
public class ReadyStorageBlockEntity extends BlockEntity implements PieceEntity, AnalogSource {
	public static final int IDLE = 0, WORKING = 1, BED = 2;
	/** The timeline's steps (ticks): the shutter, the rollers, a case lifted off, a case split. */
	public static final int SHUTTER = 20, ROLL = 28, CYCLE = 40, SPLIT = 80;
	/** How long an empty pallet waits on the bed for a station before Ready Storage takes it in. */
	public static final int EMPTY_WAIT = 300;
	private int state = IDLE;
	private long start;
	private @Nullable PalletLoad pallet;
	/** How many to take off each case (by its index on the pallet; 0: leave it). */
	private List<Integer> take = new ArrayList<>();
	/** Whether the pallet rolls back out afterwards (else it goes up into Ready Storage: emptied, no station free). */
	private boolean keep = true;
	private int order;
	private ItemStack orderItem = ItemStack.EMPTY;
	private long orderAmount;
	private @Nullable BlockPos from;
	private @Nullable BlockPos terminal;
	private int modules = 1;
	/** A pallet drone is on its way to take the pallet off the bed (it isn't taken in meanwhile): till this tick. */
	private long claimed;

	public ReadyStorageBlockEntity(BlockPos pos, BlockState state) {
		super(JugcraftWarehouse.READY_STORAGE_ENTITY, pos, state);
	}

	public int state() {
		return state;
	}

	public long start() {
		return start;
	}

	public @Nullable PalletLoad pallet() {
		return pallet;
	}

	public List<Integer> take() {
		return take;
	}

	public boolean keep() {
		return keep;
	}

	public int orderId() {
		return order;
	}

	public ItemStack orderItem() {
		return orderItem;
	}

	public long orderAmount() {
		return orderAmount;
	}

	public @Nullable BlockPos from() {
		return from;
	}

	public @Nullable BlockPos terminalPos() {
		return terminal;
	}

	public int modules() {
		return modules;
	}

	public Direction facing() {
		BlockState s = getBlockState();
		return s.getBlock() instanceof PieceBlock ? s.getValue(PieceBlock.FACING) : Direction.NORTH;
	}

	/** Its hatch is free for the next pallet. */
	public boolean free() {
		return state == IDLE;
	}

	/** A pallet is waiting on the bed for a pallet drone. */
	public boolean waiting() {
		return state == BED && pallet != null;
	}

	void claim(long until) {
		claimed = until;
	}

	/** How many modules stand here (it and the extensions on it): the terminal counts its slots by this. */
	void setModules(int modules, BlockPos terminal) {
		if (modules != this.modules || !terminal.equals(this.terminal)) {
			this.modules = modules;
			this.terminal = terminal.immutable();
			sync();
		}
	}

	// ------------------------------------------------------------------ the timeline

	/** The cases lifted off in turn (their indices: the top layer first), and whether each is only split. */
	public List<int[]> picks() {
		List<int[]> out = new ArrayList<>();
		if (pallet == null) {
			return out;
		}
		for (int i = pallet.size() - 1; i >= 0; i--) {
			int n = i < take.size() ? take.get(i) : 0;
			if (n > 0) {
				out.add(new int[] {i, n < pallet.cases().get(i).count() ? 1 : 0});
			}
		}
		return out;
	}

	public int pickTicks() {
		int t = 0;
		for (int[] p : picks()) {
			t += p[1] == 1 ? SPLIT : CYCLE;
		}
		return t;
	}

	/** Ticks from the start: the shutter up, rolled in, shutter down; then the picks; then up, out, down (if it comes back out). */
	public int duration() {
		return SHUTTER + ROLL + SHUTTER + pickTicks() + (keep ? SHUTTER + ROLL + SHUTTER : 0);
	}

	public long end() {
		return start + duration();
	}

	// ------------------------------------------------------------------ the server

	/**
	 * A pallet drone set an order's pallet on the bed (Planner PUT_BED): works out what comes off it (the order's item,
	 * up to what it still needs and what there is room for) and starts the timeline.
	 */
	void begin(PalletLoad load, Order o, @Nullable BlockPos place, long room, boolean stationFree, long now) {
		pallet = load.copy();
		take = new ArrayList<>();
		long need = Math.min(o.remaining(), room);
		for (int i = pallet.size() - 1; i >= 0; i--) {
			CaseLoad c = pallet.cases().get(i);
			int n = 0;
			if (need > 0 && c.sameItem(o.item)) {
				n = (int) Math.min(c.count(), need);
				need -= n;
			}
			take.add(0, n);
		}
		int left = 0;
		for (int i = 0; i < pallet.size(); i++) {
			if (take.get(i) < pallet.cases().get(i).count()) {
				left++;
			}
		}
		keep = left > 0 || stationFree;
		order = o.id;
		orderItem = o.item.copy();
		orderAmount = o.amount;
		from = place;
		state = WORKING;
		start = now;
		if (level != null) {
			level.playSound(null, worldPosition, SoundEvents.IRON_TRAPDOOR_OPEN, SoundSource.BLOCKS, 0.6F, 0.7F);
		}
		sync();
	}

	/** Part W5: a comparator by it reads how full the warehouse's Ready Storage slots are (0 empty, 15 full). */
	@Override
	public int analog() {
		return analogNow;
	}

	private int analogNow, lastAnalog = -1;

	public void serverTick(ServerLevel level) {
		long now = level.getGameTime();
		if (now % 20 == 0) {
			TerminalBlockEntity t = Warehouses.terminalOf(level, worldPosition);
			int size = t == null ? 0 : t.pool().size(), used = t == null ? 0 : t.pool().used();
			analogNow = size <= 0 || used <= 0 ? 0 : Math.min(15, 1 + 14 * used / size);
			if (analogNow != lastAnalog) {
				lastAnalog = analogNow;
				PieceBlock.analogChanged(level, worldPosition, getBlockState());
			}
		}
		if (state == WORKING && now >= end()) {
			finish(level, now);
		} else if (state == BED && pallet != null && pallet.isEmpty() && now - start >= EMPTY_WAIT && now >= claimed) {
			// No station took the empty pallet: it goes into Ready Storage as a pallet.
			TerminalBlockEntity t = Warehouses.terminalOf(level, worldPosition);
			if (t != null && t.pool().add(new ItemStack(JugcraftWarehouse.PALLET), 1, null, 0) == 1) {
				t.unreserve(from);
				pallet = null;
				from = null;
				state = IDLE;
				t.setChanged();
				sync();
			}
		}
	}

	/** The timeline's end: what was lifted off goes to Ready Storage (or the port's buffer), the order moves on. */
	private void finish(ServerLevel level, long now) {
		TerminalBlockEntity t = Warehouses.terminalOf(level, worldPosition);
		if (t != null && pallet != null) {
			t.picked(this, pallet, take, order, now);
		}
		List<CaseLoad> left = new ArrayList<>();
		if (pallet != null) {
			for (CaseLoad c : pallet.cases()) {
				if (c.count() > 0) {
					left.add(c);
				}
			}
			pallet = new PalletLoad(left);
		}
		take = new ArrayList<>();
		if (pallet != null && (keep || !pallet.isEmpty())) {
			state = BED;
			start = now;
		} else if (pallet != null && t != null && t.pool().add(new ItemStack(JugcraftWarehouse.PALLET), 1, null, 0) == 1) {
			// Emptied, and no station to take it: it goes up into Ready Storage as a pallet.
			t.unreserve(from);
			pallet = null;
			from = null;
			state = IDLE;
		} else {
			// (No room even for that: it comes back out and waits on the bed.)
			state = pallet == null ? IDLE : BED;
			start = now;
		}
		sync();
	}

	/** Takes off one case's items (the terminal, as it carries out the picks): the case keeps the rest. */
	CaseLoad lessen(int index, int n) {
		CaseLoad c = pallet.cases().get(index);
		CaseLoad out = new CaseLoad(c.item(), c.count() - n);
		pallet.cases().set(index, out);
		return out;
	}

	/** A pallet drone takes the pallet off the bed (Planner TAKE_BED). */
	@Nullable PalletLoad takeBed() {
		if (state != BED || pallet == null) {
			return null;
		}
		PalletLoad out = pallet;
		pallet = null;
		state = IDLE;
		from = null;
		sync();
		return out;
	}

	private void sync() {
		setChanged();
		if (level != null) {
			level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
		}
	}

	@Override
	public InteractionResult use(Level level, BlockPos clicked, Player player, ItemStack stack, InteractionHand hand) {
		if (!level.isClientSide()) {
			TerminalBlockEntity t = Warehouses.terminalOf(level, worldPosition);
			if (t == null) {
				player.sendOverlayMessage(Component.translatable("message.jugcraft.warehouse.no_terminal"));
			} else {
				player.sendOverlayMessage(Component.translatable("message.jugcraft.warehouse.ready_storage", t.pool().used(), t.pool().capacity()));
			}
		}
		return InteractionResult.SUCCESS;
	}

	@Override
	public void dropContents() {
		if (level == null || pallet == null) {
			return;
		}
		TerminalBlockEntity t = Warehouses.terminalOf(level, worldPosition);
		if (t != null) {
			t.unreserve(from);
		}
		double x = worldPosition.getX() + 0.5, y = worldPosition.getY() + 0.5, z = worldPosition.getZ() + 0.5;
		for (ItemStack s : pallet.stacks()) {
			Containers.dropItemStack(level, x, y, z, s);
		}
		Containers.dropItemStack(level, x, y, z, new ItemStack(JugcraftWarehouse.PALLET));
		pallet = null;
		state = IDLE;
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
		return saveWithoutMetadata(registries);
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		state = input.getIntOr("state", IDLE);
		start = input.getLongOr("start", 0);
		pallet = input.read("pallet", PalletLoad.CODEC).orElse(null);
		take = new ArrayList<>(input.read("take", Codec.INT.listOf()).orElse(List.of()));
		keep = input.getBooleanOr("keep", true);
		order = input.getIntOr("order", 0);
		orderItem = input.read("order_item", ItemStack.OPTIONAL_CODEC).orElse(ItemStack.EMPTY);
		orderAmount = input.getLongOr("order_amount", 0);
		from = input.read("from", BlockPos.CODEC).orElse(null);
		terminal = input.read("terminal", BlockPos.CODEC).orElse(null);
		modules = input.getIntOr("modules", 1);
		if (pallet == null && state != IDLE) {
			state = IDLE;
		}
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		output.putInt("state", state);
		output.putLong("start", start);
		if (pallet != null) {
			output.store("pallet", PalletLoad.CODEC, pallet);
		}
		output.store("take", Codec.INT.listOf(), take);
		output.putBoolean("keep", keep);
		output.putInt("order", order);
		if (!orderItem.isEmpty()) {
			output.store("order_item", ItemStack.OPTIONAL_CODEC, orderItem);
		}
		output.putLong("order_amount", orderAmount);
		if (from != null) {
			output.store("from", BlockPos.CODEC, from);
		}
		if (terminal != null) {
			output.store("terminal", BlockPos.CODEC, terminal);
		}
		output.putInt("modules", modules);
	}
}
