package io.github.jimbozoomer.jugcraft.client.warehouse;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import io.github.jimbozoomer.jugcraft.drone.DepotView;
import io.github.jimbozoomer.jugcraft.tower.RoomKit;
import io.github.jimbozoomer.jugcraft.tower.RoomKitBlock;
import io.github.jimbozoomer.jugcraft.warehouse.IntakeBlockEntity;
import io.github.jimbozoomer.jugcraft.warehouse.JugcraftWarehouse;
import io.github.jimbozoomer.jugcraft.warehouse.Kinematics;
import io.github.jimbozoomer.jugcraft.warehouse.WarehouseLayout;
import io.github.jimbozoomer.jugcraft.warehouse.WarehousePiece;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

/**
 * The Tri-Pillar's cargo hall (work list item 20; tools/tripillar_stations.py, layout 40), look-only (the server load
 * rule: nothing ticks, nothing is sent): drawn with the goods lift's lowest landing door (client/GoodsLiftRenderer), on
 * the lift's round, wherever a Case Packer (the room kit's cargo_case_packer) and a pack spot (warehouse_pack_spot) stand
 * near such a door.
 * <p>
 * The cargo exchange's goods ride its infeed belt (the room kit's cargo_infeed, layout 41) out of the exchange, over the
 * intake room's aisle and through the wall into the packer's hopper; the packer's cases run along its conveyor to the stop; the stacker drone beside the pack spot lifts them one by one
 * onto the pallet the pallet drone holds there; the drone carries the full pallet up its lane to the lift's landing and
 * waits. When the lift's doors open it drives the pallet into the car (the near spot), backs out, takes the empty pallet
 * the car brought down (the far spot) and carries it back to the pack spot, while the car takes the full one up (to the
 * supply pickup on the lift house's top, where it is unloaded and an empty one sent down). With no drones out the lift
 * waits at the bottom and so does the hall: the drone at the landing with a full pallet, the stacker folded.
 */
public final class CargoHallRenderer {
	/** How far from the door the pack spot and the packer are looked for (blocks, each way). */
	private static final int REACH = 20;
	/** The lift's round (GoodsLiftRenderer: its bottom doors open 0..200, shut by 230, up 230..530, down 790..1090, open again 1090..1120). */
	private static final int ROUND = 1120;
	/** The drone's speed (blocks a tick) and its forks' heights (px): carrying, and set down (the pallet on the floor). */
	private static final double SPEED = 0.12;
	private static final float CARRY = 6F, DOWN = 0.2F;
	/**
	 * The car's two pallet spots in the door's frame (blocks: x across the door, z into the car), and how far ahead of the
	 * drone's middle its pallet's middle is (both 8 px into theirs).
	 */
	private static final double SPOT_Z = 2.625, SPOT_NEAR = 0.875, SPOT_FAR = 2.125, AHEAD = Kinematics.CARRIED[2] / 16.0;
	/** Where the drone waits, in front of the door (blocks, frame z). */
	private static final double WAIT_Z = -2.5;
	/** The stacker: a case every PICK ticks from START, eight a pallet. */
	private static final int START = 330, PICK = 50, CASES = 8;
	private static final String[] KINDS = {"case_carton", "case_carton_w"};
	/** The infeed's goods: one on every GOODS ticks while the lift runs, at BELT px a tick, from the exchange's hood to the spout's. */
	private static final int GOODS = 50;
	private static final double BELT = 0.4, BELT_FROM = 61, BELT_TO = 4, BELT_X = 8, BELT_Y = 22.9;
	private static final String[] GOODS_KINDS = {"case_sack", "case_crate"};

	private CargoHallRenderer() {
	}

	/** The hall a lift's lowest door serves, as last read (world blocks). */
	public static final class Hall {
		final BlockPos door;
		final double floorY;
		/** The drone's middle: waiting before the near and far spots, in the car at each, at the pack spot. */
		final double[] waitNear, waitFar, inNear, inFar, pack;
		/** The pallets' middles in the car (at its floor). */
		final double[] carNear, carFar;
		final double[] stacker;
		final float droneYaw, stackerYaw;
		final BlockPos packer;
		final Direction packerFacing;
		final double[] stopTop;
		final BlockPos packerLight;
		/** The packer's infeed (its spout's cell, facing), if it has one. */
		@Nullable BlockPos infeed;
		Direction infeedFacing = Direction.NORTH;

		Hall(BlockPos door, double floorY, double[] waitNear, double[] waitFar, double[] inNear, double[] inFar, double[] pack, double[] carNear,
				double[] carFar, double[] stacker, float droneYaw, float stackerYaw, BlockPos packer, Direction packerFacing, double[] stopTop,
				BlockPos packerLight) {
			this.door = door;
			this.floorY = floorY;
			this.waitNear = waitNear;
			this.waitFar = waitFar;
			this.inNear = inNear;
			this.inFar = inFar;
			this.pack = pack;
			this.carNear = carNear;
			this.carFar = carFar;
			this.stacker = stacker;
			this.droneYaw = droneYaw;
			this.stackerYaw = stackerYaw;
			this.packer = packer;
			this.packerFacing = packerFacing;
			this.stopTop = stopTop;
			this.packerLight = packerLight;
		}
	}

	/** What the hall draws this frame. */
	public static final class Frame {
		/** Whether there is a hall to draw (the lift's own load is not drawn then: the hall's pallets ride in the car). */
		public boolean active;
		@Nullable Hall hall;
		double droneX, droneZ;
		float droneLift;
		int droneLook = -1;
		boolean droneMoving;
		String droneJob = "";
		float battery;
		int droneLight;
		final float[] arm = new float[4];
		int armLook = -1;
		boolean armMoving;
		int stackerLight;
		int carNear = -1, carFar = -1;
		double carY;
		int carLight;
		final List<float[]> cases = new ArrayList<>();
		/** The goods on the infeed belt: their middles (x, y, z, world) and kinds. */
		final List<double[]> goods = new ArrayList<>();
		int goodsLight;
		int lamp;
		@Nullable Page hmi;
		int packerLight;
	}

	// ------------------------------------------------------------------ finding the hall

	/** The hall the lift door at {@code door} (its anchor, facing {@code facing}) serves, or null (no packer or pack spot near). */
	public static @Nullable Hall find(Level level, BlockPos door, Direction facing) {
		RoomKitBlock packerBlock = RoomKit.BLOCKS.get("cargo_case_packer");
		BlockPos spot = null, packer = null;
		double spotD = Double.MAX_VALUE, packerD = Double.MAX_VALUE;
		BlockPos.MutableBlockPos at = new BlockPos.MutableBlockPos();
		for (int dx = -REACH; dx <= REACH; dx++) {
			for (int dz = -REACH; dz <= REACH; dz++) {
				for (int dy = -1; dy <= 0; dy++) {
					at.set(door.getX() + dx, door.getY() + dy, door.getZ() + dz);
					if (!level.isLoaded(at)) {
						continue;
					}
					BlockState s = level.getBlockState(at);
					double d = dx * dx + dz * dz;
					if (dy == -1 && s.is(JugcraftWarehouse.PACK_SPOT) && d < spotD) {
						spot = at.immutable();
						spotD = d;
					} else if (dy == 0 && packerBlock != null && s.is(packerBlock) && packerBlock.isAnchor(s) && d < packerD) {
						packer = at.immutable();
						packerD = d;
					}
				}
			}
		}
		if (spot == null || packer == null) {
			return null;
		}
		BlockState packerState = level.getBlockState(packer);
		Direction kitFacing = packerState.hasProperty(RoomKitBlock.FACING_PROP) ? packerState.getValue(RoomKitBlock.FACING_PROP) : Direction.NORTH;
		Hall hall = hall(door, facing, spot, packer, kitFacing);
		// its infeed: a cargo_infeed's spout over the packer
		RoomKitBlock infeedBlock = RoomKit.BLOCKS.get("cargo_infeed");
		for (int dx = -3; dx <= 3 && infeedBlock != null && hall.infeed == null; dx++) {
			for (int dz = -3; dz <= 3 && hall.infeed == null; dz++) {
				for (int dy = 0; dy <= 3; dy++) {
					at.set(packer.getX() + dx, packer.getY() + dy, packer.getZ() + dz);
					BlockState s = level.isLoaded(at) ? level.getBlockState(at) : null;
					if (s != null && s.is(infeedBlock) && infeedBlock.isAnchor(s)) {
						hall.infeed = at.immutable();
						hall.infeedFacing = s.hasProperty(RoomKitBlock.FACING_PROP) ? s.getValue(RoomKitBlock.FACING_PROP) : Direction.NORTH;
						break;
					}
				}
			}
		}
		return hall;
	}

	/** The hall's geometry: the lift door's anchor and facing, the pack spot, the packer's anchor and its (room kit) facing. */
	static Hall hall(BlockPos door, Direction facing, BlockPos spot, BlockPos packer, Direction kitFacing) {
		// the room kit faces its front; the warehouse piece faces the way its user looks (the opposite); its origin is its
		// front row's middle (WarehousePiece), the kit's cell 4: (-1, 0, -1) from the anchor as the kit faces north
		Direction whFacing = kitFacing.getOpposite();
		BlockPos origin = packer.offset(turn(-1, -1, kitFacing));
		WarehouseLayout layout = WarehouseLayout.get();
		double[] stopTop = WarehousePiece.toWorld(WarehousePiece.INTAKE, whFacing, origin, IntakeBlockEntity.slotX(0) + Kinematics.CASE / 2,
				layout.number("intake", "conveyor_y") + Kinematics.CASE_H, layout.number("intake", "conveyor_z"));
		double floorY = door.getY();
		double[] in = {-facing.getStepX(), -facing.getStepZ()};                 // from the landing into the car
		double[] waitNear = frame(door, facing, SPOT_NEAR, WAIT_Z), waitFar = frame(door, facing, SPOT_FAR, WAIT_Z);
		double[] carNear = frame(door, facing, SPOT_NEAR, SPOT_Z), carFar = frame(door, facing, SPOT_FAR, SPOT_Z);
		double[] inNear = {carNear[0] - in[0] * AHEAD, carNear[1] - in[1] * AHEAD}, inFar = {carFar[0] - in[0] * AHEAD, carFar[1] - in[1] * AHEAD};
		// along the door's width, from the landing toward the pack spot
		double[] width = {facing.getClockWise().getStepX(), facing.getClockWise().getStepZ()};
		double sx = spot.getX() + 0.5, sz = spot.getZ() + 0.5;
		double side = Math.signum((sx - waitNear[0]) * width[0] + (sz - waitNear[1]) * width[1]);
		double[] along = {width[0] * (side == 0 ? 1 : side), width[1] * (side == 0 ? 1 : side)};
		// the pallet held a px past the spot's middle (the stacker's arm reaches its far cases), the stacker beyond it, facing it
		double[] pallet = {sx + along[0] / 16, sz + along[1] / 16};
		double[] pack = {pallet[0] - in[0] * AHEAD, pallet[1] - in[1] * AHEAD};
		double reach = (15 + Kinematics.SD_BASE_Z - 8) / 16;
		double[] stacker = {pallet[0] + along[0] * reach, pallet[1] + along[1] * reach};
		float droneYaw = yaw(in[0], in[1]), stackerYaw = yaw(-along[0], -along[1]);
		BlockPos light = origin.relative(whFacing.getOpposite());
		return new Hall(door, floorY, waitNear, waitFar, inNear, inFar, pack, carNear, carFar, stacker, droneYaw, stackerYaw, origin, whFacing,
				stopTop, light);
	}

	/** A point in the door's frame (blocks: x across the door from its left, z from its front into the car) in the world. */
	private static double[] frame(BlockPos door, Direction facing, double fx, double fz) {
		BlockPos ux = turn(1, 0, facing), uz = turn(0, 1, facing);
		double ax = fx - 0.5, az = fz - 0.5;
		return new double[] {door.getX() + 0.5 + ux.getX() * ax + uz.getX() * az, door.getZ() + 0.5 + ux.getZ() * ax + uz.getZ() * az};
	}

	/** A cell's offset from a room kit piece's anchor (as it faces north) as it faces {@code d} (tower/RoomKitBlock.offset). */
	private static BlockPos turn(int dx, int dz, Direction d) {
		return switch (d) {
			case EAST -> new BlockPos(-dz, 0, dx);
			case SOUTH -> new BlockPos(-dx, 0, -dz);
			case WEST -> new BlockPos(dz, 0, -dx);
			default -> new BlockPos(dx, 0, dz);
		};
	}

	/** The warehouse drones' yaw facing (x, z): 0 south, 90 east, 180 north, -90 west. */
	private static float yaw(double x, double z) {
		return (float) Math.toDegrees(Math.atan2(x, z));
	}

	// ------------------------------------------------------------------ the round

	/**
	 * Works out the hall for this frame: {@code t} the lift's round (ticks, -1 while it waits), {@code rounds} how many
	 * rounds it has run (the cases' colour), {@code carY} its car's floor over the door, {@code view} the depot's last view.
	 */
	public static void extract(Hall hall, Level level, double t, long rounds, double carY, int carLight, @Nullable DepotView view, Font font,
			Frame out) {
		out.active = true;
		out.hall = hall;
		out.carY = carY;
		out.carLight = carLight;
		boolean running = t >= 0;
		int kind = (int) (rounds % 2);
		// the drone: its middle, forks and load through the round
		Drone d = drone(hall, running ? t : -1, kind);
		out.droneX = d.x;
		out.droneZ = d.z;
		out.droneLift = d.lift;
		out.droneLook = d.look;
		out.droneMoving = d.moving;
		out.droneJob = d.job;
		// its battery: down a little each round, charged again at the landing (look-only)
		out.battery = (float) (0.92 - 0.06 * ((running ? t : 0) / ROUND));
		out.droneLight = LightCoordsUtil.getLightCoords(level, BlockPos.containing(d.x, hall.floorY + 0.5, d.z));
		// the car's pallets: the full one (packed last round) in from when the drone sets it down until it is taken off at the
		// top; the empty one down from the top until the drone lifts it out
		out.carNear = running && t >= 50 && t < 660 ? full(1 - kind) : -1;
		out.carFar = !running || t < 132 || t >= 660 ? 0 : -1;
		// the stacker and the cases
		arm(hall, running ? t : -1, kind, out);
		out.stackerLight = LightCoordsUtil.getLightCoords(level, BlockPos.containing(hall.stacker[0], hall.floorY + 0.5, hall.stacker[1]));
		out.packerLight = LightCoordsUtil.getLightCoords(level, hall.packerLight);
		out.lamp = running ? 1 : 0;
		int queued = cases(running ? t : -1, rounds, out.cases);
		goods(hall, running ? rounds * ROUND + t : 30, out.goods);
		if (hall.infeed != null) {
			out.goodsLight = LightCoordsUtil.getLightCoords(level, hall.infeed.above());
		}
		Page page = new Page(font);
		page.title(150, "CASE PACKER", "CARGO");
		boolean packing = running && t >= START && t < START + CASES * PICK;
		page.add(0, 16, 1, "STATUS", Page.GREY).right(150, 16, 1, packing ? "PACKING" : running ? "RUNNING" : "IDLE",
				packing || running ? Page.GREEN : Page.CYAN);
		page.add(0, 30, 1, "ON CONVEYOR", Page.GREY).right(150, 30, 1, queued + " / " + IntakeBlockEntity.CONVEYOR, Page.WHITE);
		DepotView.CargoView cargo = view == null ? null : view.cargo;
		page.add(0, 44, 1, "IN STORE", Page.GREY).right(150, 44, 1, cargo == null ? "-" : cargo.unlimited() ? "UNLIMITED" : String.valueOf(cargo.items()),
				Page.WHITE);
		page.add(0, 58, 1, "DRONES OUT", Page.GREY).right(150, 58, 1, view == null ? "-" : String.valueOf(view.flying()), Page.WHITE);
		if (packing) {
			page.bar(0, 74, 1, 18, (t - START) / (double) (CASES * PICK), Page.GREEN);
		}
		out.hmi = page;
	}

	private static int full(int kind) {
		return kind == 0 ? CASES : 8 + CASES;
	}

	/** The pallet look with n cases of a kind (WhQuads.loadQuads: 0 the bare pallet, 1..8 cartons, 9..16 white cartons). */
	private static int load(int kind, int n) {
		return n <= 0 ? 0 : kind == 0 ? n : 8 + n;
	}

	private record Drone(double x, double z, float lift, int look, boolean moving, String job) {
	}

	/**
	 * The pallet drone through the round (it faces the car all the way, its wheels taking it sideways along its lane):
	 * 5..38 in with the full pallet, 38..50 down, 50..83 out, 83..99 across, 99..132 in under the empty one, 132..144 up,
	 * 144..177 out (clear of the doors before they shut at 200), then down its lane to the pack spot (177..316), down
	 * (316..328), loaded by the stacker (330..730), up (730..742), back up the lane (742..871) and waiting.
	 */
	private static Drone drone(Hall h, double t, int kind) {
		int fullLook = full(kind), lastFull = full(1 - kind);
		if (t < 0) {
			return new Drone(h.waitNear[0], h.waitNear[1], CARRY, fullLook, false, "AT LIFT");    // the last round's pallet
		}
		double[] wn = h.waitNear, wf = h.waitFar, in = h.inNear, inf = h.inFar, pk = h.pack;
		if (t < 5) {
			return at(wn, CARRY, lastFull, false, "LOADING LIFT");
		}
		if (t < 38) {
			return move(wn, in, t - 5, 33, CARRY, lastFull, "LOADING LIFT");
		}
		if (t < 50) {
			return at(in, lerp(CARRY, DOWN, (t - 38) / 12), lastFull, true, "LOADING LIFT");
		}
		if (t < 83) {
			return move(in, wn, t - 50, 33, DOWN, -1, "LOADING LIFT");
		}
		if (t < 99) {
			return move(wn, wf, t - 83, 16, DOWN, -1, "LOADING LIFT");
		}
		if (t < 132) {
			return move(wf, inf, t - 99, 33, DOWN, -1, "EMPTY PALLET");
		}
		if (t < 144) {
			return at(inf, lerp(DOWN, CARRY, (t - 132) / 12), 0, true, "EMPTY PALLET");
		}
		if (t < 177) {
			return move(inf, wf, t - 144, 33, CARRY, 0, "EMPTY PALLET");
		}
		double lane = dist(wf, pk) / SPEED;
		if (t < 177 + lane) {
			return move(wf, pk, t - 177, lane, CARRY, 0, "TO PACK SPOT");
		}
		double down = 177 + lane;
		if (t < down + 12) {
			return at(pk, lerp(CARRY, DOWN, (t - down) / 12), 0, true, "PACKING");
		}
		int done = packed(t);
		if (t < START + CASES * PICK) {
			return at(pk, DOWN, load(kind, done), false, "PACKING");
		}
		double up = START + CASES * PICK;
		if (t < up + 12) {
			return at(pk, lerp(DOWN, CARRY, (t - up) / 12), fullLook, true, "TO LIFT");
		}
		double back = dist(pk, wn) / SPEED;
		if (t < up + 12 + back) {
			return move(pk, wn, t - up - 12, back, CARRY, fullLook, "TO LIFT");
		}
		return at(wn, CARRY, fullLook, false, "AT LIFT");
	}

	private static Drone at(double[] p, float lift, int look, boolean moving, String job) {
		return new Drone(p[0], p[1], lift, look, moving, job);
	}

	/** From a to b, {@code u} ticks into a move of {@code ticks}: speeding up and slowing down over 10 ticks at each end. */
	private static Drone move(double[] a, double[] b, double u, double ticks, float lift, int look, String job) {
		double e = ramp(u / ticks, Math.min(0.5, 10 / ticks));
		return new Drone(a[0] + (b[0] - a[0]) * e, a[1] + (b[1] - a[1]) * e, lift, look, true, job);
	}

	/** How far along (0..1) a move is at f of its time, speeding up for the first k of it and slowing down for the last. */
	private static double ramp(double f, double k) {
		f = Math.max(0, Math.min(1, f));
		double v = 1 / (1 - k);
		return f < k ? v * f * f / (2 * k) : f > 1 - k ? 1 - v * (1 - f) * (1 - f) / (2 * k) : v * (f - k / 2);
	}

	/** How many cases the stacker has set on the pallet by t. */
	private static int packed(double t) {
		if (t < START) {
			return 0;
		}
		// case i is let go of 46 ticks into its pick
		return (int) Math.max(0, Math.min(CASES, Math.floor((t - START - 46) / PICK) + 1));
	}

	/**
	 * The stacker's arm: folded until the drone is down at the pack spot, then for each case to the conveyor's stop (down,
	 * grip, up), across to its place on the pallet (down, let go, up), and folded again. The cups' path is worked out in
	 * the drone's own frame and the arm reaches it (Kinematics.reachLocal) each frame.
	 */
	private static void arm(Hall h, double t, int kind, Frame out) {
		out.armLook = -1;
		out.armMoving = false;
		double[] rest = {8, 22, 14};
		double[] stop = local(h, h.stopTop[0], h.stopTop[1] - h.floorY, h.stopTop[2]);
		double[] point = rest;
		if (t >= START && t < START + CASES * PICK + 12) {
			int i = (int) Math.min(CASES - 1, (t - START) / PICK);
			double u = t - START - i * PICK;
			double[] slot = slot(h, i);
			// over the stop, as over a place: a little up and in (the stop is near the arm's reach)
			double[] overStop = {stop[0] + (8 - stop[0]) * 0.15, stop[1] + 3, stop[2] + (Kinematics.SD_BASE_Z - stop[2]) * 0.15};
			// over its place: a little up and in toward the drone (the far places are at the arm's reach)
			double[] overSlot = {slot[0] + (8 - slot[0]) * 0.15, slot[1] + 2, slot[2] + (Kinematics.SD_BASE_Z - slot[2]) * 0.15};
			double[] from = i == 0 ? rest : over(h, i - 1);
			if (t >= START + CASES * PICK) {
				point = swing(over(h, CASES - 1), rest, (t - START - CASES * PICK) / 12);
			} else if (u < 10) {
				point = swing(from, overStop, u / 10);
			} else if (u < 16) {
				point = lerp3(overStop, stop, (u - 10) / 6);
			} else if (u < 20) {
				point = stop;
			} else if (u < 26) {
				point = lerp3(stop, overStop, (u - 20) / 6);
			} else if (u < 38) {
				point = swing(overStop, overSlot, (u - 26) / 12);
			} else if (u < 44) {
				point = lerp3(overSlot, slot, (u - 38) / 6);
			} else if (u < 46) {
				point = slot;
			} else {
				point = lerp3(slot, overSlot, (u - 46) / 4);
			}
			out.armLook = u >= 18 && u < 46 ? kind : -1;
			out.armMoving = true;
		}
		double[] q = Kinematics.reachLocal(point[0], point[1], point[2]);
		if (q == null) {
			q = Kinematics.reachLocal(rest[0], rest[1], rest[2]);
		}
		if (q != null) {
			for (int k = 0; k < 4; k++) {
				out.arm[k] = (float) q[k];
			}
		}
	}

	/** Over case place i (as the arm comes away from it). */
	private static double[] over(Hall h, int i) {
		double[] slot = slot(h, i);
		return new double[] {slot[0] + (8 - slot[0]) * 0.15, slot[1] + 2, slot[2] + (Kinematics.SD_BASE_Z - slot[2]) * 0.15};
	}

	/** The top middle of case place i on the pallet the drone holds at the pack spot, in the stacker's frame (px). */
	private static double[] slot(Hall h, int i) {
		double[] top = Kinematics.heldTop(h.pack[0], h.pack[1], h.floorY, h.droneYaw, DOWN, i);
		return local(h, top[0], top[1] - h.floorY, top[2]);
	}

	/** A world point (y over the floor, blocks) in the stacker's own frame (px: x to its left, y up, z the way it faces). */
	private static double[] local(Hall h, double x, double y, double z) {
		double a = Math.toRadians(h.stackerYaw);
		double dx = x - h.stacker[0], dz = z - h.stacker[1];
		// its left (+x) is (cos a, -sin a), its front (+z) is (sin a, cos a) in the world
		return new double[] {8 + 16 * (dx * Math.cos(a) - dz * Math.sin(a)), 16 * y, 8 + 16 * (dx * Math.sin(a) + dz * Math.cos(a))};
	}

	/**
	 * The cases on the packer's conveyor (its own px along it, the kind): four queued to the stop; when the stacker takes the
	 * one at the stop the rest run up a place and a new one comes on at the start. Returns how many are on it.
	 */
	private static int cases(double t, long rounds, List<float[]> out) {
		out.clear();
		float pitch = (float) Kinematics.CASE + 0.4F;
		int taken = 0;
		double since = 1e9;
		if (t < 0) {
			taken = CASES;                                // waiting: the next pallet's cases queued
		} else if (t >= START) {
			// case i is lifted off 18 ticks into its pick
			taken = (int) Math.max(0, Math.min(CASES, Math.floor((t - START - 18) / PICK) + 1));
			if (taken > 0) {
				since = t - (START + (taken - 1) * PICK + 18);
			}
		}
		long first = rounds * CASES + taken;              // the serial of the case at the stop (its kind by its pallet)
		for (int i = 0; i < 4; i++) {
			float x;
			if (taken == 0) {
				x = IntakeBlockEntity.slotX(i);
			} else if (i < 3) {
				x = (float) Math.min(IntakeBlockEntity.slotX(i), IntakeBlockEntity.slotX(i + 1) + IntakeBlockEntity.SPEED * since);
			} else {
				float start = (float) WarehouseLayout.get().number("intake", "start_x");
				x = (float) Math.min(IntakeBlockEntity.slotX(3), start + IntakeBlockEntity.SPEED * since);
			}
			// the case at the stop is on the stacker's cups from when it is taken until the next is there
			out.add(new float[] {x, (int) (((first + i) / CASES) % 2)});
		}
		return out.size();
	}

	/**
	 * The goods on the infeed's belt at {@code g} (the lift's clock over all its rounds; stopped while it waits): one comes
	 * out of the exchange every {@link #GOODS} ticks and rides to the spout's hood, sacks and crates in turn.
	 */
	private static void goods(Hall h, double g, List<double[]> out) {
		out.clear();
		if (h.infeed == null) {
			return;
		}
		double ride = (BELT_FROM - BELT_TO) / BELT;
		for (long k = (long) Math.floor((g - ride) / GOODS); k * GOODS <= g; k++) {
			double age = g - k * GOODS;
			if (age < 0 || age >= ride) {
				continue;
			}
			double pz = BELT_FROM - BELT * age;
			double[] w = kitPoint(h.infeed, h.infeedFacing, BELT_X, BELT_Y, pz);
			out.add(new double[] {w[0], w[1], w[2], Math.floorMod(k, 2)});
		}
	}

	/** A point in a room kit piece's own pixels (facing north from its anchor's cell) in the world, as it faces {@code f}. */
	private static double[] kitPoint(BlockPos anchor, Direction f, double px, double py, double pz) {
		BlockPos ux = turn(1, 0, f), uz = turn(0, 1, f);
		double ax = px / 16 - 0.5, az = pz / 16 - 0.5;
		return new double[] {anchor.getX() + 0.5 + ux.getX() * ax + uz.getX() * az, anchor.getY() + py / 16,
				anchor.getZ() + 0.5 + ux.getZ() * ax + uz.getZ() * az};
	}

	private static double dist(double[] a, double[] b) {
		return Math.hypot(b[0] - a[0], b[1] - a[1]);
	}

	private static float lerp(float a, float b, double f) {
		return (float) (a + (b - a) * Math.max(0, Math.min(1, f)));
	}

	/** From a to b round the arm's base (its turn, reach and height each eased along): the cups swing, not cut across. */
	private static double[] swing(double[] a, double[] b, double f) {
		double e = ease(f);
		double ax = a[0] - Kinematics.SD_BASE_X, az = a[2] - Kinematics.SD_BASE_Z, bx = b[0] - Kinematics.SD_BASE_X, bz = b[2] - Kinematics.SD_BASE_Z;
		double ra = Math.hypot(ax, az), rb = Math.hypot(bx, bz), ta = Math.atan2(ax, az), tb = Math.atan2(bx, bz);
		double turn = Math.IEEEremainder(tb - ta, 2 * Math.PI);
		double r = ra + (rb - ra) * e, th = ta + turn * e;
		return new double[] {Kinematics.SD_BASE_X + r * Math.sin(th), a[1] + (b[1] - a[1]) * e, Kinematics.SD_BASE_Z + r * Math.cos(th)};
	}

	private static double[] lerp3(double[] a, double[] b, double f) {
		double e = ease(f);
		return new double[] {a[0] + (b[0] - a[0]) * e, a[1] + (b[1] - a[1]) * e, a[2] + (b[2] - a[2]) * e};
	}

	private static double ease(double x) {
		x = Math.max(0, Math.min(1, x));
		return x * x * (3 - 2 * x);
	}

	// ------------------------------------------------------------------ drawing

	/** Draws the hall; the pose at the door's block (its corner), the world's axes. */
	public static void submit(Frame f, PoseStack pose, SubmitNodeCollector collector, Font font) {
		Hall h = f.hall;
		if (!f.active || h == null) {
			return;
		}
		BlockPos o = h.door;
		// the pallet drone
		pose.pushPose();
		pose.translate(f.droneX - o.getX(), h.floorY - o.getY(), f.droneZ - o.getZ());
		pose.rotateDegrees(Axis.YP, h.droneYaw);
		pose.scale(1 / 16F, 1 / 16F, 1 / 16F);
		pose.translate(-8, 0, -8);
		long ticks = net.minecraft.client.Minecraft.getInstance().level == null ? 0 : net.minecraft.client.Minecraft.getInstance().level.getGameTime();
		WhQuads.Tint tint = new WhQuads.Tint(f.droneMoving ? 0xFF64E68C : 0xFF5ADCF0, f.droneMoving && (ticks / 10) % 2 == 0);
		DroneRenderer.palletDrone(pose, collector, f.droneLift, 0, 0, f.droneLook, false, f.droneLight, tint, "PD-01", f.droneJob, f.battery, font);
		pose.popPose();
		// the stacker
		pose.pushPose();
		pose.translate(h.stacker[0] - o.getX(), h.floorY - o.getY(), h.stacker[1] - o.getZ());
		pose.rotateDegrees(Axis.YP, h.stackerYaw);
		pose.scale(1 / 16F, 1 / 16F, 1 / 16F);
		pose.translate(-8, 0, -8);
		WhQuads.Tint arm = new WhQuads.Tint(f.armMoving ? 0xFF64E68C : 0xFF5ADCF0, f.armMoving && (ticks / 10) % 2 == 0);
		DroneRenderer.stacker(pose, collector, f.arm[0], f.arm[1], f.arm[2], f.arm[3], 0, f.armLook, f.stackerLight, arm);
		pose.popPose();
		// the goods on the infeed belt, square to it
		if (h.infeed != null && !f.goods.isEmpty()) {
			BlockPos run = turn(0, 1, h.infeedFacing);
			float beltYaw = yaw(run.getX(), run.getZ());
			for (double[] g : f.goods) {
				pose.pushPose();
				pose.translate(g[0] - o.getX(), g[1] - o.getY(), g[2] - o.getZ());
				pose.rotateDegrees(Axis.YP, beltYaw);
				pose.scale(1 / 16F, 1 / 16F, 1 / 16F);
				pose.translate(-Kinematics.CASE / 2, 0, -Kinematics.CASE / 2);
				WhQuads.submitPart(collector, pose, GOODS_KINDS[(int) g[3]], f.goodsLight, WhQuads.Tint.NONE);
				pose.popPose();
			}
		}
		// the pallets in the car
		pallet(pose, collector, h, h.carNear, f.carY, f.carNear, f.carLight);
		pallet(pose, collector, h, h.carFar, f.carY, f.carFar, f.carLight);
		// the packer's conveyor, its stack light and its HMI (as the warehouse's Intake Case Packer: IntakeRenderer)
		pose.pushPose();
		pose.translate(h.packer.getX() - o.getX(), h.packer.getY() - o.getY(), h.packer.getZ() - o.getZ());
		Page.pieceFrame(pose, h.packerFacing, WarehousePiece.INTAKE);
		WarehouseLayout layout = WarehouseLayout.get();
		float cy = (float) layout.number("intake", "conveyor_y"), cz = (float) layout.number("intake", "conveyor_z") - (float) Kinematics.CASE / 2;
		for (float[] c : f.cases) {
			pose.pushPose();
			pose.translate(c[0], cy, cz);
			WhQuads.submitPart(collector, pose, KINDS[(int) c[1]], f.packerLight, WhQuads.Tint.NONE);
			pose.popPose();
		}
		var stack = layout.section("intake").getAsJsonObject("stack");
		String[] lamps = {"lamp_blue", "lamp_green"};
		pose.pushPose();
		pose.translate(stack.get("x").getAsFloat() - 8, stack.get("y0").getAsFloat() + f.lamp * stack.get("step").getAsFloat(), stack.get("z").getAsFloat() - 8);
		WhQuads.submitPart(collector, pose, lamps[f.lamp], f.packerLight, WhQuads.Tint.NONE);
		pose.popPose();
		if (f.hmi != null) {
			var hmi = layout.section("intake").getAsJsonObject("hmi");
			float x0 = hmi.get("x0").getAsFloat(), x1 = hmi.get("x1").getAsFloat(), y1 = hmi.get("y1").getAsFloat();
			f.hmi.draw(pose, collector, x0 + 0.3F, y1 - 0.3F, hmi.get("z").getAsFloat(), (x1 - x0 - 0.6F) / 150F);
		}
		pose.popPose();
	}

	/** A pallet (load look, -1 none) in the car, its middle at (x, z), on the car's floor. */
	private static void pallet(PoseStack pose, SubmitNodeCollector collector, Hall h, double[] at, double carY, int look, int light) {
		if (look < 0) {
			return;
		}
		BlockPos o = h.door;
		pose.pushPose();
		pose.translate(at[0] - o.getX(), h.floorY + carY - o.getY(), at[1] - o.getZ());
		pose.rotateDegrees(Axis.YP, h.droneYaw);
		pose.scale(1 / 16F, 1 / 16F, 1 / 16F);
		pose.translate(-8, 0, -8);
		WhQuads.submit(collector, pose, WhQuads.loadQuads(look), light, WhQuads.Tint.NONE);
		pose.popPose();
	}
}
