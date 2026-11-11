package io.github.jimbozoomer.jugcraft.warehouse;

import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
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
 * Part W5: the Warehouse Status Board, a big display on a wall (client/warehouse/StatusBoardRenderer.java draws it):
 * the live floor map of its warehouse (its terminal's MapPayload, sent to the players near it when the map changes,
 * and the drones from the terminal's fleet) and the day's numbers. Using it shows the next floor (or all in turn).
 */
public class StatusBoardBlockEntity extends BlockEntity implements PieceEntity {
	/** The floor shown (-1: each in turn, ten seconds a floor). */
	private int floor = -1;
	private @Nullable BlockPos terminal;
	private int sentVersion = -1;
	private long sentAt;
	/**
	 * The day's numbers it shows (synced when they change): places used and in all, orders open and ready, drones working
	 * and charging, JE a tick drawn, stored %, counted today %, what housekeeping is doing, the floors.
	 */
	private int placesUsed, placesTotal, ordersOpen, ordersReady, dronesWork, dronesCharge, storedPct, countedPct, floors = 1;
	private long draw;
	private String house = "";

	public StatusBoardBlockEntity(BlockPos pos, BlockState state) {
		super(JugcraftWarehouse.STATUS_BOARD_ENTITY, pos, state);
	}

	public int floor() {
		return floor;
	}

	public @Nullable BlockPos terminalPos() {
		return terminal;
	}

	/** Its numbers: places used, places, orders open, ready, drones working, charging, stored %, counted today %, floors. */
	public int[] stats() {
		return new int[] {placesUsed, placesTotal, ordersOpen, ordersReady, dronesWork, dronesCharge, storedPct, countedPct, floors};
	}

	public long draw() {
		return draw;
	}

	public String house() {
		return house;
	}

	public void serverTick(ServerLevel level) {
		long now = level.getGameTime();
		if (now % 40 != 7) {
			return;
		}
		TerminalBlockEntity t = Warehouses.terminalNear(level, worldPosition);
		BlockPos tp = t == null ? null : t.getBlockPos();
		if (!java.util.Objects.equals(tp, terminal)) {
			terminal = tp;
			sentVersion = -1;
			sync();
		}
		if (t == null) {
			return;
		}
		// The day's numbers.
		int open = 0, ready = 0, work = 0, charge = 0;
		for (Order o : t.orders()) {
			if (o.open()) {
				open++;
			}
			if (o.state == Order.READY) {
				ready++;
			}
		}
		for (Drone d : t.drones()) {
			if (d.busy(now) && d.job != Drone.HOME) {
				work++;
			} else if (d.docked && d.battery < 0.999F) {
				charge++;
			}
		}
		int[] next = {t.placesUsed(), t.placesTotal(), open, ready, work, charge, t.storedPercent(), t.countedPercent(now), Math.max(1, t.network().floors.size())};
		int[] was = {placesUsed, placesTotal, ordersOpen, ordersReady, dronesWork, dronesCharge, storedPct, countedPct, floors};
		if (!java.util.Arrays.equals(next, was) || t.drawNow() != draw || !t.house().equals(house)) {
			placesUsed = next[0];
			placesTotal = next[1];
			ordersOpen = next[2];
			ordersReady = next[3];
			dronesWork = next[4];
			dronesCharge = next[5];
			storedPct = next[6];
			countedPct = next[7];
			floors = next[8];
			draw = t.drawNow();
			house = t.house();
			sync();
		}
		MapPayload map = t.map(level);
		// The map to the players near when it changed (and now and then, for anyone who came in meanwhile).
		if (map.version() != sentVersion || now - sentAt >= 200) {
			sentVersion = map.version();
			sentAt = now;
			for (ServerPlayer player : PlayerLookup.tracking(level, worldPosition)) {
				ServerPlayNetworking.send(player, map);
			}
		}
	}

	private void sync() {
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
		TerminalBlockEntity t = Warehouses.terminalNear(level, worldPosition);
		int floors = t == null ? 1 : Math.max(1, t.network().floors.size());
		floor = floor + 1 >= floors ? -1 : floor + 1;
		player.sendOverlayMessage(Component.translatable(floor < 0 ? "message.jugcraft.warehouse.board_all" : "message.jugcraft.warehouse.board_floor",
				floor + 1));
		sync();
		return InteractionResult.SUCCESS;
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
		floor = input.getIntOr("floor", -1);
		terminal = input.read("terminal", BlockPos.CODEC).orElse(null);
		int[] st = input.read("stats", com.mojang.serialization.Codec.INT.listOf()).map(l -> l.stream().mapToInt(Integer::intValue).toArray())
				.orElse(new int[9]);
		if (st.length == 9) {
			placesUsed = st[0];
			placesTotal = st[1];
			ordersOpen = st[2];
			ordersReady = st[3];
			dronesWork = st[4];
			dronesCharge = st[5];
			storedPct = st[6];
			countedPct = st[7];
			floors = Math.max(1, st[8]);
		}
		draw = input.getLongOr("draw", 0);
		house = input.getStringOr("house", "");
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		output.putInt("floor", floor);
		output.store("stats", com.mojang.serialization.Codec.INT.listOf(), java.util.Arrays.stream(stats()).boxed().toList());
		output.putLong("draw", draw);
		output.putString("house", house);
		if (terminal != null) {
			output.store("terminal", BlockPos.CODEC, terminal);
		}
	}
}
