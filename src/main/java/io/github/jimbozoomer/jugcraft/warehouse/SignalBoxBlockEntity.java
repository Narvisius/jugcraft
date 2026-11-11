package io.github.jimbozoomer.jugcraft.warehouse;

import java.util.ArrayList;
import java.util.List;
import net.fabricmc.fabric.api.menu.v1.ExtendedMenuProvider;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.minecraft.core.BlockPos;
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
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

/**
 * Part W5: the Warehouse Signal Box, a remote I/O box on a wall: one rule a box. OUTPUT: the warehouse's state as a
 * redstone signal from every side (stock of an item below or above a number, the stock's level, an order ready, the
 * fleet busy, low power, the rack places full, the fire alarm). INPUT: a redstone signal into it does something while
 * it lasts (pause the fleet, hold the streams, stop the intake, stop the liquids coming in, consolidate now). Using it
 * with an item in hand sets its item; otherwise its screen opens (client/warehouse/SignalBoxScreen.java).
 */
public class SignalBoxBlockEntity extends BlockEntity implements PieceEntity, ExtendedMenuProvider<BlockPos> {
	public static final int OUTPUT = 0, INPUT = 1;
	public static final String[] OUT_RULES = {"STOCK BELOW", "STOCK ABOVE", "STOCK LEVEL", "ORDER READY", "FLEET BUSY", "LOW POWER", "PLACES FULL",
			"FIRE ALARM"};
	public static final int STOCK_BELOW = 0, STOCK_ABOVE = 1, STOCK_LEVEL = 2, ORDER_READY = 3, FLEET_BUSY = 4, LOW_POWER = 5, PLACES_FULL = 6,
			FIRE_ALARM = 7;
	public static final String[] IN_RULES = {"PAUSE FLEET", "STREAMS OFF", "INTAKE OFF", "LIQUIDS IN OFF", "CONSOLIDATE NOW"};
	public static final int PAUSE_FLEET = 0, STREAMS_OFF = 1, INTAKE_OFF = 2, LIQUIDS_IN_OFF = 3, CONSOLIDATE_NOW = 4;
	private int mode = OUTPUT, rule = STOCK_BELOW;
	private ItemStack item = ItemStack.EMPTY;
	private long threshold = 512;
	// The state (synced): the signal it gives, the one it gets, linked to a terminal, what it counts now.
	private int output;
	private boolean input, linked;
	private long value;
	private @Nullable BlockPos terminal;
	/** Who set it up (it works for a warehouse they may use: no one else's). */
	private java.util.@Nullable UUID owner;
	/** The fire panels near its warehouse (found now and then). */
	private final List<BlockPos> firePanels = new ArrayList<>();
	private long fireScanAt;

	public SignalBoxBlockEntity(BlockPos pos, BlockState state) {
		super(JugcraftWarehouse.SIGNAL_BOX_ENTITY, pos, state);
	}

	public int mode() {
		return mode;
	}

	public int rule() {
		return rule;
	}

	public ItemStack item() {
		return item;
	}

	public long threshold() {
		return threshold;
	}

	public int output() {
		return output;
	}

	public boolean input() {
		return input;
	}

	public boolean linked() {
		return linked;
	}

	/** What it counts now (the item's stock), for its screen. */
	public long value() {
		return value;
	}

	public @Nullable TerminalBlockEntity terminal() {
		TerminalBlockEntity t = level == null ? null : Warehouses.terminalNear(level, worldPosition);
		return t != null && owner != null && !t.mayUse(owner) ? null : t;
	}

	/** Who set it up (its placer; whoever changes its rule). */
	public void setOwner(java.util.UUID id) {
		if (!id.equals(owner)) {
			owner = id;
			setChanged();
		}
	}

	/** The rule's words, for its little screen and the terminal's CONTROL page. */
	public String describe() {
		if (mode == INPUT) {
			return "IN  " + IN_RULES[Math.max(0, Math.min(IN_RULES.length - 1, rule))];
		}
		String r = OUT_RULES[Math.max(0, Math.min(OUT_RULES.length - 1, rule))];
		if (rule <= STOCK_LEVEL) {
			String what = item.isEmpty() ? "?" : item.getHoverName().getString();
			return what + (rule == STOCK_BELOW ? " < " : rule == STOCK_ABOVE ? " > " : " of ") + threshold;
		}
		return r;
	}

	public void serverTick(ServerLevel level) {
		long now = level.getGameTime();
		if (now % 10 != 3) {
			return;
		}
		TerminalBlockEntity t = terminal();
		BlockPos tp = t == null ? null : t.getBlockPos();
		boolean changed = !java.util.Objects.equals(tp, terminal) || (t != null) != linked;
		terminal = tp;
		linked = t != null;
		int out = 0;
		boolean in = level.hasNeighborSignal(worldPosition);
		long val = 0;
		if (t != null) {
			if (mode == OUTPUT) {
				switch (rule) {
					case STOCK_BELOW, STOCK_ABOVE, STOCK_LEVEL -> {
						TerminalBlockEntity.Stock s = item.isEmpty() ? null : t.stock().get(ItemVariant.of(item));
						val = s == null ? 0 : s.count;
						if (!item.isEmpty()) {
							out = switch (rule) {
								case STOCK_BELOW -> val < threshold ? 15 : 0;
								case STOCK_ABOVE -> val > threshold ? 15 : 0;
								default -> (int) Math.min(15, Math.round(15.0 * val / Math.max(1, threshold)));
							};
						}
					}
					case ORDER_READY -> out = t.orders().stream().anyMatch(o -> o.state == Order.READY) ? 15 : 0;
					case FLEET_BUSY -> out = t.drones().stream().anyMatch(d -> d.busy(now)) ? 15 : 0;
					case LOW_POWER -> out = t.lowPower() ? 15 : 0;
					case PLACES_FULL -> out = t.placesTotal() > 0 && t.placesUsed() >= t.placesTotal() ? 15 : 0;
					case FIRE_ALARM -> out = fire(level, t, now) ? 15 : 0;
					default -> {
					}
				}
			} else if (in) {
				t.signalIn(worldPosition, rule, now);
			}
		}
		if (out != output) {
			output = out;
			changed = true;
			level.updateNeighborsAt(worldPosition, getBlockState().getBlock());
		}
		if (in != input || val != value) {
			input = in;
			value = val;
			changed = true;
		}
		if (changed) {
			sync();
		}
	}

	/** Whether a fire panel of its warehouse is in alarm (or has released). */
	private boolean fire(ServerLevel level, TerminalBlockEntity t, long now) {
		if (now >= fireScanAt) {
			fireScanAt = now + 200;
			firePanels.clear();
			BlockPos c = t.getBlockPos();
			for (int cx = (c.getX() >> 4) - 3; cx <= (c.getX() >> 4) + 3; cx++) {
				for (int cz = (c.getZ() >> 4) - 3; cz <= (c.getZ() >> 4) + 3; cz++) {
					if (!level.hasChunk(cx, cz)) {
						continue;
					}
					LevelChunk chunk = level.getChunk(cx, cz);
					for (BlockEntity be : chunk.getBlockEntities().values()) {
						if (be instanceof io.github.jimbozoomer.jugcraft.nuclear.LivePanelBlockEntity
								&& io.github.jimbozoomer.jugcraft.nuclear.SpreadingRoomFire.kind(be.getBlockState()) == 0) {
							firePanels.add(be.getBlockPos());
						}
					}
				}
			}
		}
		for (BlockPos p : firePanels) {
			if (level.isLoaded(p) && level.getBlockEntity(p) instanceof io.github.jimbozoomer.jugcraft.nuclear.LivePanelBlockEntity panel
					&& (panel.csrPhase == io.github.jimbozoomer.jugcraft.nuclear.SpreadingRoomFire.ALARM
					|| panel.csrPhase == io.github.jimbozoomer.jugcraft.nuclear.SpreadingRoomFire.RELEASED)) {
				return true;
			}
		}
		return false;
	}

	// ------------------------------------------------------------------ set by its screen (SignalBoxMenu)

	public void setMode(int m) {
		mode = m == INPUT ? INPUT : OUTPUT;
		rule = 0;
		sync();
	}

	public void setRule(int r) {
		rule = Math.max(0, Math.min((mode == INPUT ? IN_RULES : OUT_RULES).length - 1, r));
		sync();
	}

	public void setThreshold(long n) {
		threshold = Math.max(0, Math.min(1_000_000_000L, n));
		sync();
	}

	public void setItem(ItemStack stack) {
		item = stack.isEmpty() ? ItemStack.EMPTY : stack.copyWithCount(1);
		sync();
	}

	void sync() {
		setChanged();
		if (level != null) {
			level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
		}
	}

	@Override
	public InteractionResult use(Level level, BlockPos clicked, Player player, ItemStack stack, InteractionHand hand) {
		if (level.isClientSide()) {
			return InteractionResult.SUCCESS;
		}
		if (!stack.isEmpty() && mode == OUTPUT && rule <= STOCK_LEVEL) {
			setOwner(player.getUUID());
			setItem(stack);
			player.sendOverlayMessage(Component.translatable("message.jugcraft.warehouse.signal_item", stack.getHoverName()));
			return InteractionResult.SUCCESS;
		}
		player.openMenu(this);
		return InteractionResult.SUCCESS;
	}

	@Override
	public Component getDisplayName() {
		return Component.translatable("container.jugcraft.warehouse_signal_box");
	}

	@Override
	public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
		return new SignalBoxMenu(id, inventory, worldPosition);
	}

	@Override
	public BlockPos getScreenOpeningData(ServerPlayer player) {
		return worldPosition;
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
		mode = input.getIntOr("mode", OUTPUT);
		rule = input.getIntOr("rule", STOCK_BELOW);
		item = input.read("item", ItemStack.CODEC).orElse(ItemStack.EMPTY);
		threshold = input.getLongOr("threshold", 512);
		output = input.getIntOr("output", 0);
		this.input = input.getBooleanOr("input", false);
		linked = input.getBooleanOr("linked", false);
		value = input.getLongOr("value", 0);
		owner = input.read("owner", net.minecraft.core.UUIDUtil.CODEC).orElse(null);
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		output.putInt("mode", mode);
		output.putInt("rule", rule);
		if (!item.isEmpty()) {
			output.store("item", ItemStack.CODEC, item);
		}
		output.putLong("threshold", threshold);
		output.putInt("output", this.output);
		output.putBoolean("input", input);
		output.putBoolean("linked", linked);
		output.putLong("value", value);
		if (owner != null) {
			output.store("owner", net.minecraft.core.UUIDUtil.CODEC, owner);
		}
	}
}
