package io.github.jimbozoomer.jugcraft.warehouse;

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
 * A Pallet Lift landing (part W3; tools/warehouse_w3.py lift_landing), at its origin block (the front row's middle
 * column, at the bottom). Every landing has its transfer conveyor's BED out front (a pallet drone sets a pallet on it,
 * or takes one off), with where that pallet is bound (a pallet place, or a Ready Storage for an order).
 *
 * <p>The lowest landing of a column is the lift's controller: it finds the landings over it (through the shaft
 * sections), and runs the carriage. Idle, it looks for a bed holding a pallet bound for another floor whose landing
 * there has a free bed; it goes to that floor, opens the gate, rolls the pallet onto the carriage, shuts the gate, goes
 * to the other floor (the counterweight going the other way, the chains running over the drive's sprockets), opens the
 * gate, rolls the pallet out onto that floor's bed and shuts the gate, for a pallet drone there to take on. Its state
 * goes to the players (the carriage's height is worked out from when it set off), for client/warehouse/LiftRenderer.java.
 */
public class LiftBlockEntity extends BlockEntity implements PieceEntity {
	/** The controller's phases. */
	public static final int IDLE = 0, MOVING = 1, OPENING = 2, LOADING = 3, UNLOADING = 4, CLOSING = 5;
	/** How long the gate takes to open or shut, a pallet to roll on or off (ticks); the carriage's speed (blocks a tick). */
	public static final int GATE = 20, ROLL = 30;
	public static final double SPEED = 0.08;
	/** How often a controller looks for its landings again (ticks). */
	private static final int RESCAN = 100;

	// ---- every landing: its bed.
	private @Nullable PalletLoad bed;
	private @Nullable BlockPos bedDest;
	private int bedOrder;
	/** A drone has booked the bed (to put a pallet on it or take one off) till this tick. */
	private long bedClaim;
	// ---- the controller.
	private final List<Integer> floors = new ArrayList<>();
	private long scanAt;
	private int phase = IDLE;
	private long phaseStart;
	/** Where the carriage is (its deck's floor: the landing's y), where it set off from and is going (world y). */
	private double carFrom, carTo;
	/** The landing (index) it is at or going to, and the one the pallet it carries goes to. */
	private int at, goal = -1;
	private @Nullable PalletLoad car;
	private @Nullable BlockPos carDest;
	private int carOrder;
	private int lastDir;

	public LiftBlockEntity(BlockPos pos, BlockState state) {
		super(JugcraftWarehouse.LIFT_ENTITY, pos, state);
		carFrom = carTo = pos.getY();
	}

	// ------------------------------------------------------------------ the bed

	public @Nullable PalletLoad bed() {
		return bed;
	}

	public @Nullable BlockPos bedDest() {
		return bedDest;
	}

	public int bedOrder() {
		return bedOrder;
	}

	/** Whether a drone may set a pallet on the bed now. */
	public boolean bedFree(long now) {
		return bed == null && bedClaim < now && !rolling(now);
	}

	/** Whether the bed's pallet is there to take (not being rolled onto the carriage, not booked by a drone). */
	public boolean bedReady(long now) {
		return bed != null && bedClaim < now && !rolling(now);
	}

	public void claimBed(long until) {
		bedClaim = until;
		setChanged();
	}

	/** Whether this landing's conveyor is rolling a pallet on or off the carriage now. */
	private boolean rolling(long now) {
		LiftBlockEntity c = controller();
		if (c == null || c.phase != LOADING && c.phase != UNLOADING) {
			return false;
		}
		return c.landingIndex(worldPosition.getY()) == c.at;
	}

	public void putBed(PalletLoad load, @Nullable BlockPos dest, int order) {
		bed = load.copy();
		bedDest = dest == null ? null : dest.immutable();
		bedOrder = order;
		bedClaim = 0;
		sync();
	}

	public @Nullable PalletLoad takeBed() {
		PalletLoad out = bed;
		bed = null;
		bedDest = null;
		bedOrder = 0;
		bedClaim = 0;
		sync();
		return out;
	}

	// ------------------------------------------------------------------ the column

	private Direction facing() {
		BlockState s = getBlockState();
		return s.getBlock() instanceof PieceBlock ? s.getValue(PieceBlock.FACING) : Direction.NORTH;
	}

	/** The landing origin under this one's column, if any (then that, or one lower, is the controller). */
	public @Nullable LiftBlockEntity controller() {
		if (level == null) {
			return null;
		}
		BlockPos p = worldPosition;
		LiftBlockEntity found = this;
		for (int k = 0; k < 512 && p.getY() > level.getMinY(); k++) {
			BlockPos down = p.below();
			BlockState s = level.getBlockState(down);
			if (!(s.getBlock() instanceof PieceBlock piece) || s.getValue(PieceBlock.FACING) != facing()
					|| piece.piece != WarehousePiece.LIFT_SHAFT && piece.piece != WarehousePiece.LIFT_LANDING) {
				break;
			}
			p = down;
			if (piece.piece == WarehousePiece.LIFT_LANDING && piece.isOrigin(s) && level.getBlockEntity(down) instanceof LiftBlockEntity lift) {
				found = lift;
			}
		}
		return found;
	}

	public boolean isController() {
		return controller() == this;
	}

	/** The landings' heights (world y of their origins), lowest first; the controller's. */
	public List<Integer> floors() {
		return floors;
	}

	private void scan() {
		floors.clear();
		floors.add(worldPosition.getY());
		BlockPos p = worldPosition;
		Direction f = facing();
		for (int k = 0; k < 512 && level != null && p.getY() < level.getMaxY(); k++) {
			BlockPos up = p.above();
			BlockState s = level.getBlockState(up);
			if (!(s.getBlock() instanceof PieceBlock piece) || s.getValue(PieceBlock.FACING) != f
					|| piece.piece != WarehousePiece.LIFT_SHAFT && piece.piece != WarehousePiece.LIFT_LANDING) {
				break;
			}
			p = up;
			if (piece.piece == WarehousePiece.LIFT_LANDING && piece.isOrigin(s)) {
				floors.add(up.getY());
			}
		}
	}

	public int landingIndex(int y) {
		return floors.indexOf(y);
	}

	/** The landing a place (or Ready Storage) at y is served by: the highest at or under it. */
	public int floorOf(int y) {
		int best = -1;
		for (int i = 0; i < floors.size(); i++) {
			if (floors.get(i) <= y) {
				best = i;
			}
		}
		return Math.max(0, best);
	}

	private @Nullable LiftBlockEntity landing(int i) {
		if (level == null || i < 0 || i >= floors.size()) {
			return null;
		}
		BlockPos p = new BlockPos(worldPosition.getX(), floors.get(i), worldPosition.getZ());
		return level.getBlockEntity(p) instanceof LiftBlockEntity l ? l : null;
	}

	// ------------------------------------------------------------------ the controller's tick

	public void serverTick(ServerLevel level) {
		long now = level.getGameTime();
		if (scanAt <= now) {
			scanAt = now + RESCAN;
			if (!isController()) {
				return;
			}
			int was = floors.size();
			scan();
			if (was != floors.size()) {
				sync();
			}
		}
		if (floors.isEmpty() || controller() != this) {
			return;
		}
		long t = now - phaseStart;
		switch (phase) {
			case IDLE -> choose(level, now);
			case MOVING -> {
				if (now >= arrival()) {
					set(OPENING, now);
				}
			}
			case OPENING -> {
				if (t >= GATE) {
					LiftBlockEntity l = landing(at);
					if (car == null && l != null && l.bed != null) {
						set(LOADING, now);
					} else if (car != null && l != null && l.bed == null) {
						set(UNLOADING, now);
					} else {
						set(CLOSING, now);
					}
				}
			}
			case LOADING -> {
				if (t >= ROLL) {
					LiftBlockEntity l = landing(at);
					if (l != null && l.bed != null) {
						car = l.bed.copy();
						carDest = l.bedDest;
						carOrder = l.bedOrder;
						l.takeBed();
					}
					set(CLOSING, now);
				}
			}
			case UNLOADING -> {
				if (t >= ROLL) {
					LiftBlockEntity l = landing(at);
					if (l != null && car != null && l.bed == null) {
						l.putBed(car, carDest, carOrder);
						car = null;
						carDest = null;
						carOrder = 0;
						goal = -1;
					}
					set(CLOSING, now);
				}
			}
			case CLOSING -> {
				if (t >= GATE) {
					if (car != null && goal >= 0 && goal != at) {
						move(goal, now);
					} else {
						set(IDLE, now);
					}
				}
			}
			default -> set(IDLE, now);
		}
	}

	/** Part W5: whether its carriage is running (for the LIFTS circuit's draw). */
	public boolean moving() {
		return phase != IDLE;
	}

	/** Idle: a pallet to carry (to a floor whose bed is free), or one on the carriage still to put down. */
	private void choose(ServerLevel level, long now) {
		// Part W5: no new trip while the LIFTS circuit is off or shed (one under way finishes).
		if (!WarehousePower.ok(level, worldPosition, WarehousePower.Circuit.LIFTS)) {
			return;
		}
		if (car != null) {
			int dest = carDest == null ? at : floorOf(carDest.getY());
			LiftBlockEntity there = landing(dest);
			if (there != null && there.bed == null && there.bedClaim < now) {
				goal = dest;
				if (dest == at) {
					set(OPENING, now);
				} else {
					move(dest, now);
				}
			}
			return;
		}
		int best = -1, bestDist = Integer.MAX_VALUE;
		for (int i = 0; i < floors.size(); i++) {
			LiftBlockEntity l = landing(i);
			if (l == null || l.bed == null || l.bedClaim >= now || l.bedDest == null) {
				continue;
			}
			int dest = floorOf(l.bedDest.getY());
			LiftBlockEntity there = landing(dest);
			if (dest == i || there == null || there.bed != null && there != l) {
				continue;
			}
			int dist = Math.abs(i - at);
			if (dist < bestDist) {
				best = i;
				bestDist = dist;
			}
		}
		if (best < 0) {
			return;
		}
		LiftBlockEntity from = landing(best);
		goal = floorOf(from.bedDest.getY());
		if (best == at) {
			set(OPENING, now);
		} else {
			move(best, now);
		}
		// The carriage will stop at "best", load, and then go on to goal.
	}

	private void move(int to, long now) {
		carFrom = carY(now);
		carTo = floors.get(to);
		lastDir = carTo > carFrom ? 1 : -1;
		at = to;
		set(MOVING, now);
		if (level != null) {
			level.playSound(null, worldPosition, SoundEvents.PISTON_EXTEND, SoundSource.BLOCKS, 0.5F, 0.6F);
		}
	}

	private void set(int phase, long now) {
		if (phase != MOVING) {
			carFrom = carTo = carY(now);
		}
		this.phase = phase;
		this.phaseStart = now;
		if (level != null && (phase == OPENING || phase == CLOSING)) {
			level.playSound(null, new BlockPos(worldPosition.getX(), floors.get(Math.min(at, floors.size() - 1)), worldPosition.getZ()),
					SoundEvents.IRON_DOOR_OPEN, SoundSource.BLOCKS, 0.6F, phase == OPENING ? 0.8F : 0.7F);
		}
		sync();
	}

	private long arrival() {
		return phaseStart + (long) Math.ceil(Math.abs(carTo - carFrom) / SPEED);
	}

	/** The carriage's deck height (world y) at a time. */
	public double carY(long now) {
		if (phase != MOVING) {
			return carTo;
		}
		double d = Math.min(Math.abs(carTo - carFrom), (now - phaseStart) * SPEED);
		return carFrom + Math.signum(carTo - carFrom) * d;
	}

	// ------------------------------------------------------------------ for the renderer and the screens

	public int phase() {
		return phase;
	}

	public long phaseStart() {
		return phaseStart;
	}

	public int at() {
		return at;
	}

	public int goal() {
		return goal;
	}

	public int direction() {
		return phase == MOVING ? lastDir : 0;
	}

	public @Nullable PalletLoad car() {
		return car;
	}

	public @Nullable BlockPos carDest() {
		return carDest;
	}

	public int carOrder() {
		return carOrder;
	}

	/** How far open this landing's gate is (0 shut, 1 up) at a time, from its controller. */
	public float gate(long now, float partial) {
		LiftBlockEntity c = controller();
		if (c == null || c.floors.isEmpty() || c.landingIndex(worldPosition.getY()) != c.at) {
			return 0;
		}
		float t = now - c.phaseStart + partial;
		return switch (c.phase) {
			case OPENING -> Math.min(1, t / GATE);
			case LOADING, UNLOADING -> 1;
			case CLOSING -> Math.max(0, 1 - t / GATE);
			default -> 0;
		};
	}

	/** The floor's number of a landing (1 the lowest). */
	public int floorNumber() {
		LiftBlockEntity c = controller();
		return c == null ? 1 : Math.max(0, c.landingIndex(worldPosition.getY())) + 1;
	}

	// ------------------------------------------------------------------ the piece

	@Override
	public InteractionResult use(Level level, BlockPos clicked, Player player, ItemStack stack, InteractionHand hand) {
		if (!level.isClientSide()) {
			LiftBlockEntity c = controller();
			if (c != null) {
				String what = c.phase == IDLE ? (c.car == null ? "idle" : "holding a pallet") : c.phase == MOVING ? "moving" : "at the gate";
				player.sendOverlayMessage(Component.translatable("message.jugcraft.warehouse.lift", "Pallet Lift",
						Math.min(c.at, Math.max(0, c.floors.size() - 1)) + 1, what));
			}
		}
		return InteractionResult.SUCCESS;
	}

	/** Broken: what is on its bed (and, the controller, on the carriage) drops. */
	@Override
	public void dropContents() {
		if (level == null) {
			return;
		}
		for (PalletLoad load : new PalletLoad[] {bed, car}) {
			if (load == null) {
				continue;
			}
			for (ItemStack s : load.stacks()) {
				Containers.dropItemStack(level, worldPosition.getX() + 0.5, worldPosition.getY() + 0.5, worldPosition.getZ() + 0.5, s);
			}
			Containers.dropItemStack(level, worldPosition.getX() + 0.5, worldPosition.getY() + 0.5, worldPosition.getZ() + 0.5,
					new ItemStack(JugcraftWarehouse.PALLET));
		}
		bed = null;
		car = null;
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
		CompoundTag tag = new CompoundTag();
		tag.putInt("bedLook", bed == null ? -1 : bed.look());
		tag.putInt("carLook", car == null ? -1 : car.look());
		tag.putInt("phase", phase);
		tag.putLong("start", phaseStart);
		tag.putDouble("from", carFrom);
		tag.putDouble("to", carTo);
		tag.putInt("at", at);
		tag.putInt("goal", goal);
		tag.putInt("dir", lastDir);
		tag.putIntArray("floors", floors.stream().mapToInt(Integer::intValue).toArray());
		return tag;
	}

	// The client keeps the looks only (the renderer draws the pallets by their look).
	private int bedLook = -1, carLook = -1;

	public int bedLook() {
		return bed != null ? bed.look() : bedLook;
	}

	public int carLook() {
		return car != null ? car.look() : carLook;
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		bed = input.read("bed", PalletLoad.CODEC).filter(l -> !l.isEmpty()).orElse(null);
		car = input.read("car", PalletLoad.CODEC).filter(l -> !l.isEmpty()).orElse(null);
		bedDest = input.read("bedDest", BlockPos.CODEC).orElse(null);
		carDest = input.read("carDest", BlockPos.CODEC).orElse(null);
		bedOrder = input.getIntOr("bedOrder", 0);
		carOrder = input.getIntOr("carOrder", 0);
		bedLook = input.getIntOr("bedLook", -1);
		carLook = input.getIntOr("carLook", -1);
		phase = input.getIntOr("phase", IDLE);
		phaseStart = input.getLongOr("start", 0L);
		carFrom = input.getDoubleOr("from", worldPosition.getY());
		carTo = input.getDoubleOr("to", worldPosition.getY());
		at = input.getIntOr("at", 0);
		goal = input.getIntOr("goal", -1);
		lastDir = input.getIntOr("dir", 0);
		int[] f = input.getIntArray("floors").orElse(new int[0]);
		floors.clear();
		for (int y : f) {
			floors.add(y);
		}
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		if (bed != null) {
			output.store("bed", PalletLoad.CODEC, bed);
		}
		if (car != null) {
			output.store("car", PalletLoad.CODEC, car);
		}
		if (bedDest != null) {
			output.store("bedDest", BlockPos.CODEC, bedDest);
		}
		if (carDest != null) {
			output.store("carDest", BlockPos.CODEC, carDest);
		}
		output.putInt("bedOrder", bedOrder);
		output.putInt("carOrder", carOrder);
		output.putInt("phase", phase);
		output.putLong("start", phaseStart);
		output.putDouble("from", carFrom);
		output.putDouble("to", carTo);
		output.putInt("at", at);
		output.putInt("goal", goal);
		output.putInt("dir", lastDir);
		output.putIntArray("floors", floors.stream().mapToInt(Integer::intValue).toArray());
	}
}
