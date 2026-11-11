package io.github.jimbozoomer.jugcraft.warehouse;

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
 * A Palletizing Station: its magazine of empty pallets (players load it with pallet items), and the pallet on its
 * build spot. The stack's bottom pallet is pushed out onto the turntable when the spot is clear (DISPENSING), stacker
 * drones stack cases on it (BUILDING), wrap or strap it when it is full or has waited long enough (WRAPPING: the
 * turntable turns it), and then it is READY for a pallet drone to take to a rack.
 */
public class StationBlockEntity extends BlockEntity implements PieceEntity {
	public static final int EMPTY = 0, DISPENSING = 1, BUILDING = 2, WRAPPING = 3, READY = 4, RETURNING = 5;
	/** How long its build spot stays clear before the next pallet is pushed out (a drone may bring an empty one back). */
	public static final int DWELL = 40;
	public static final int MAGAZINE = 12;
	public static final int DISPENSE_TICKS = 30;
	private int magazine;
	private @Nullable PalletLoad build;
	private int state = EMPTY;
	/** When the state began and (dispensing, wrapping) ends. */
	private long since, until;
	private long lastCase;
	/** A pallet drone is bringing an empty pallet back (it doesn't push one out meanwhile): till this tick. */
	private long incoming;
	/**
	 * Part W5: consolidating: a part pallet (B) stands on the build spot and the stacker moves another's cases onto it
	 * (no intake cases go on it, it isn't wrapped till that is done); since when.
	 */
	private boolean consolidating;
	private long consolidatingSince;
	/**
	 * Part W5: the rack place of the consolidation's other pallet (A), till a pallet drone sets out to bring it (then
	 * null: the stacker and that drone have their legs).
	 */
	private @Nullable BlockPos consolFrom;
	/**
	 * Part W5: the pallet on the build spot is stock taken out of the racks (a consolidation's): the books count it till a
	 * pallet drone takes it away again.
	 */
	private boolean restock;

	public StationBlockEntity(BlockPos pos, BlockState state) {
		super(JugcraftWarehouse.STATION_ENTITY, pos, state);
	}

	public int magazine() {
		return magazine;
	}

	/** Loads empty pallets into the magazine (as using pallets on it does); returns how many went in. */
	public int addPallets(int n) {
		int in = Math.max(0, Math.min(n, MAGAZINE - magazine));
		magazine += in;
		sync();
		return in;
	}

	public @Nullable PalletLoad build() {
		return build;
	}

	public int state() {
		return state;
	}

	public long since() {
		return since;
	}

	public long until() {
		return until;
	}

	/** It can take a case now (building, with room). */
	public boolean accepts() {
		return state == BUILDING && build != null && !build.full();
	}

	public boolean ready() {
		return state == READY && build != null;
	}

	/** Its pallet is done: full, or it has cases and has waited CLOSE_AFTER for more. */
	public boolean wantsWrap(long now) {
		return state == BUILDING && build != null && !consolidating && (build.full() || !build.isEmpty() && now - lastCase >= Planner.CLOSE_AFTER);
	}

	public boolean consolidating() {
		return consolidating;
	}

	/** Part W5: the consolidation's other pallet's place, while no drone has set out to bring it. */
	public @Nullable BlockPos consolFrom() {
		return consolidating ? consolFrom : null;
	}

	/** Part W5: the place kept for a consolidation here (from when its first pallet sets out till its second is taken). */
	public @Nullable BlockPos heldFor() {
		return consolFrom;
	}

	/** Part W5: its pallet is stock out of the racks (a consolidation's), counted by the terminal's books. */
	public boolean restock() {
		return restock && build != null;
	}

	/**
	 * Part W5: whether its build spot can be cleared for a consolidation (or an empty pallet brought back): clear, or an
	 * empty pallet on it that can go back into the magazine.
	 */
	public boolean canClear() {
		return !consolidating && (state == EMPTY || state == BUILDING && build != null && build.isEmpty() && magazine < MAGAZINE);
	}

	/**
	 * Part W5: a pallet drone is bringing a part pallet (B) to consolidate onto, the other's (A) place this: no pallet is
	 * pushed out till then (an empty one on the spot goes back into the magazine).
	 */
	void setConsolPending(BlockPos from, long until) {
		consolFrom = from.immutable();
		setIncoming(until);
	}

	/** Part W5: a pallet drone has set out to bring the consolidation's other pallet. */
	void takeConsolFrom() {
		consolFrom = null;
		consolidatingSince = level == null ? consolidatingSince : level.getGameTime();
		sync();
	}

	/** Finishes an empty pallet's way back into the magazine now (a drone is setting a pallet on the spot). */
	private void finishReturn() {
		if (state == RETURNING) {
			magazine = Math.min(MAGAZINE, magazine + 1);
			state = EMPTY;
		}
	}

	/** Part W5: a pallet drone set a part pallet on the empty build spot to consolidate onto (Planner CONSOL_SET). */
	boolean receiveBuild(PalletLoad load, long now) {
		incoming = 0;
		finishReturn();
		if (state != EMPTY || build != null) {
			consolFrom = null;
			return false;
		}
		build = load.copy();
		state = BUILDING;
		since = now;
		lastCase = now;
		restock = true;
		consolidating = true;
		consolidatingSince = now;
		sync();
		return true;
	}

	/** Part W5: the stacker has moved the other pallet's cases on (Planner CONSOL_DONE): it is wrapped and put away now. */
	void consolidated(long now) {
		consolidating = false;
		consolFrom = null;
		lastCase = now - Planner.CLOSE_AFTER;
		sync();
	}

	Direction facing() {
		BlockState s = getBlockState();
		return s.getBlock() instanceof PieceBlock ? s.getValue(PieceBlock.FACING) : Direction.NORTH;
	}

	/** The top middle of case slot i on the pallet (world blocks). */
	public double[] slotTop(int i) {
		int layer = Math.min(1, i / 4);
		double[] spot = Kinematics.SPOTS[i % 4];
		double tableY = WarehouseLayout.get().number("station", "table_y");
		double y = tableY + Kinematics.PALLET_H + layer * (Kinematics.CASE_H + 0.05) + Kinematics.CASE_H;
		return WarehousePiece.toWorld(WarehousePiece.STATION, facing(), worldPosition, spot[0] + Kinematics.CASE / 2, y, 16 + spot[1] + Kinematics.CASE / 2);
	}

	/**
	 * Its build spot is clear (or holds an empty pallet that can go back in first) and its magazine has room: a drone may
	 * bring an empty pallet back to it.
	 */
	public boolean takesBack() {
		return !consolidating && (state == EMPTY && magazine < MAGAZINE || state == RETURNING && magazine + 1 < MAGAZINE
				|| state == BUILDING && build != null && build.isEmpty() && magazine + 1 < MAGAZINE);
	}

	/** A drone is bringing a pallet to the build spot: nothing is pushed out till then, an empty pallet on it goes back in. */
	void setIncoming(long until) {
		this.incoming = until;
		if (state == BUILDING && build != null && build.isEmpty() && magazine < MAGAZINE) {
			build = null;
			state = RETURNING;
			since = level == null ? 0 : level.getGameTime();
			this.until = since + DISPENSE_TICKS;
			sync();
		}
	}

	/** A pallet drone set an empty pallet on the build spot (Planner PUT_STATION): it goes back into the magazine. */
	boolean receivePallet(long now) {
		incoming = 0;
		finishReturn();
		if (state != EMPTY || magazine >= MAGAZINE) {
			return false;
		}
		state = RETURNING;
		since = now;
		until = now + DISPENSE_TICKS;
		sync();
		return true;
	}

	public void serverTick(ServerLevel level) {
		long now = level.getGameTime();
		if (state == RETURNING && now >= until) {
			magazine++;
			state = EMPTY;
			since = now;
			level.playSound(null, worldPosition, SoundEvents.CHAIN_PLACE, SoundSource.BLOCKS, 0.5F, 0.7F);
			sync();
		} else if (state == EMPTY && magazine > 0 && now >= incoming && now - since >= DWELL
				&& WarehousePower.ok(level, worldPosition, WarehousePower.Circuit.STATIONS)) {
			magazine--;
			state = DISPENSING;
			since = now;
			until = now + DISPENSE_TICKS;
			level.playSound(null, worldPosition, SoundEvents.CHAIN_PLACE, SoundSource.BLOCKS, 0.5F, 0.8F);
			sync();
		} else if (state == DISPENSING && now >= until) {
			state = BUILDING;
			build = new PalletLoad();
			since = now;
			lastCase = now;
			sync();
		} else if (state == WRAPPING && now >= until) {
			state = READY;
			since = now;
			sync();
		} else if (consolidating && (state != BUILDING || now - consolidatingSince >= (consolFrom != null ? 2400 : 6000))) {
			// (Its other pallet never came: what is on it is wrapped and put away as it is; the warehouse waits a while
			// before trying again.)
			if (consolFrom != null) {
				TerminalBlockEntity t = Warehouses.terminalOf(level, worldPosition);
				if (t != null) {
					t.consolidationFailed(now);
				}
			}
			consolidating = false;
			consolFrom = null;
			lastCase = now - Planner.CLOSE_AFTER;
			sync();
		} else if (!consolidating && consolFrom != null && now > incoming) {
			// (The first pallet never came.)
			consolFrom = null;
			sync();
		}
	}

	/** A stacker sets a case on the pallet (Planner PLACE_CASE). */
	public boolean addCase(CaseLoad c, long now) {
		if (!accepts()) {
			return false;
		}
		build.add(c);
		lastCase = now;
		sync();
		return true;
	}

	/** A stacker starts wrapping (or strapping) the pallet: the turntable turns it till then. */
	public void startWrap(long now, int ticks) {
		if (state == BUILDING && build != null && !build.isEmpty()) {
			state = WRAPPING;
			since = now;
			until = now + ticks;
			sync();
		}
	}

	/** A pallet drone lifts the finished pallet off (Planner PICK_PALLET). */
	public @Nullable PalletLoad takePallet(long now) {
		if (state != READY || build == null) {
			return null;
		}
		PalletLoad out = build;
		build = null;
		restock = false;
		state = EMPTY;
		since = now;
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
		if (level.isClientSide()) {
			return InteractionResult.SUCCESS;
		}
		if (stack.is(JugcraftWarehouse.PALLET)) {
			int room = MAGAZINE - magazine;
			if (room <= 0) {
				player.sendOverlayMessage(Component.translatable("message.jugcraft.warehouse.magazine_full"));
				return InteractionResult.SUCCESS;
			}
			int n = Math.min(room, player.isSecondaryUseActive() ? stack.getCount() : 1);
			magazine += n;
			if (!player.getAbilities().instabuild) {
				stack.shrink(n);
			}
			level.playSound(null, worldPosition, SoundEvents.WOOD_PLACE, SoundSource.BLOCKS, 0.8F, 1.0F);
			sync();
		}
		player.sendOverlayMessage(Component.translatable("message.jugcraft.warehouse.pallets", magazine));
		return InteractionResult.SUCCESS;
	}

	@Override
	public void dropContents() {
		if (level == null) {
			return;
		}
		double x = worldPosition.getX() + 0.5, y = worldPosition.getY() + 0.5, z = worldPosition.getZ() + 0.5;
		int pallets = magazine + (build != null || state == DISPENSING || state == RETURNING ? 1 : 0);
		if (build != null) {
			for (ItemStack s : build.stacks()) {
				Containers.dropItemStack(level, x, y, z, s);
			}
		}
		while (pallets > 0) {
			int n = Math.min(64, pallets);
			Containers.dropItemStack(level, x, y, z, new ItemStack(JugcraftWarehouse.PALLET, n));
			pallets -= n;
		}
		magazine = 0;
		build = null;
		restock = false;
		state = EMPTY;
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
		magazine = input.getIntOr("magazine", 0);
		build = input.read("build", PalletLoad.CODEC).orElse(null);
		state = input.getIntOr("state", EMPTY);
		since = input.getLongOr("since", 0);
		until = input.getLongOr("until", 0);
		lastCase = input.getLongOr("last_case", 0);
		incoming = input.getLongOr("incoming", 0);
		consolidating = input.getBooleanOr("consolidating", false);
		consolidatingSince = input.getLongOr("consolidating_since", 0);
		consolFrom = input.read("consol_from", BlockPos.CODEC).orElse(null);
		restock = input.getBooleanOr("restock", false);
		if (build == null && (state == BUILDING || state == WRAPPING || state == READY)) {
			state = EMPTY;
		}
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		output.putInt("magazine", magazine);
		if (build != null) {
			output.store("build", PalletLoad.CODEC, build);
		}
		output.putInt("state", state);
		output.putLong("since", since);
		output.putLong("until", until);
		output.putLong("last_case", lastCase);
		output.putLong("incoming", incoming);
		output.putBoolean("consolidating", consolidating);
		output.putLong("consolidating_since", consolidatingSince);
		if (consolFrom != null) {
			output.store("consol_from", BlockPos.CODEC, consolFrom);
		}
		output.putBoolean("restock", restock);
	}
}
