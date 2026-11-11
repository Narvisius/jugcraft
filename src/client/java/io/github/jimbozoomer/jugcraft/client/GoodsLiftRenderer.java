package io.github.jimbozoomer.jugcraft.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import io.github.jimbozoomer.jugcraft.client.warehouse.CargoHallRenderer;
import io.github.jimbozoomer.jugcraft.drone.DepotView;
import io.github.jimbozoomer.jugcraft.drone.DroneTerminalBlockEntity;
import io.github.jimbozoomer.jugcraft.tower.GoodsLiftDoorBlock;
import io.github.jimbozoomer.jugcraft.tower.RoomKitBlockEntity;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.model.geom.builders.UVPair;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import org.joml.Vector3fc;
import org.jspecify.annotations.Nullable;

/**
 * The goods lift's live parts (work list item 20; tower/GoodsLiftDoorBlock, tools/goods_lift.py), drawn for each landing
 * door's anchor by client/RoomKitDisplayRenderer: the door's two leaves (the upper rises into the housing, the lower
 * drops into the sill), its floor indicator (the car's floor and which way it goes), and, at the shaft's lowest door,
 * the car itself with its three mesh gates and, going up, two pallets of crates. The models are the door's own part
 * models past its cells (cut into blocks, jugcraft/goods_lift.json lists them), in the door's frame (facing north: the
 * door x 0..3, the car's well behind it, z 1..4), turned to the door's facing here.
 * <p>
 * It is look-only (the server load rule): no server work, no block entity ticking. The car runs its round while the
 * depot the door is pinned to has drones out (the terminal's view, as the desks read it): loaded up from the lowest
 * door to the highest, a wait while it is unloaded, empty down again; with nothing out it waits at the bottom, shut.
 */
final class GoodsLiftRenderer {
	/** The see-through faces (the vision panels' wired glass), drawn after the solid ones ({@link #SOLID_TYPE}). */
	private static final RenderType RENDER_TYPE = RenderTypes.entityTranslucentCull(TextureAtlas.LOCATION_BLOCKS);
	private static final RenderType SOLID_TYPE = RenderTypes.entityCutoutCull(TextureAtlas.LOCATION_BLOCKS);
	/** How far up or down a shaft another landing door is looked for (blocks), and how often the shaft is read again. */
	private static final int REACH = 48, RESCAN = 40;
	/** The car's height (blocks, its floor to its roof): at the top of a well with no door there, its roof is under the ceiling. */
	private static final int CAR_HEIGHT = 4;
	/** The round (ticks): at the bottom open (the crates are loaded half way), shut, up, open, at the top (unloaded half way), shut, down, open. */
	private static final int LOAD = 200, DOOR = 30, RUN = 300, DWELL = 200;
	private static final int T_SHUT1 = LOAD, T_UP = T_SHUT1 + DOOR, T_OPEN2 = T_UP + RUN, T_TOP = T_OPEN2 + DOOR, T_SHUT2 = T_TOP + DWELL,
			T_DOWN = T_SHUT2 + DOOR, T_OPEN1 = T_DOWN + RUN, ROUND = T_OPEN1 + DOOR;
	private static final int AMBER = 0xFF9628;
	private static final Map<String, int[][]> PARTS = new HashMap<>();
	private static float leafDrop = 19.5F, leafRise = 20F, gateRise = 24F;
	private static float[] indicator = {17, 41.8F, 31, 46.4F, 0.2F};
	private static final Map<Integer, Cached> QUADS = new HashMap<>();
	private static final Map<RoomKitBlockEntity, Shaft> SHAFTS = new WeakHashMap<>();
	private static boolean loaded;

	private record Cached(BlockStateModel model, List<BakedQuad> quads) {
	}

	/**
	 * A door's shaft as last read: the lowest and highest landing door's anchors, this door's landing among them; with no
	 * door over the lowest, the car's floor at the top of its run (under the well's ceiling: the Tri-Pillar's lift house,
	 * whose top is the supply pickup, layout 40), and the cargo hall the lowest door serves (client/warehouse/CargoHallRenderer).
	 */
	private static final class Shaft {
		long read = Long.MIN_VALUE;
		BlockPos bottom, top;
		Direction bottomFacing = Direction.NORTH, topFacing = Direction.NORTH;
		int head = Integer.MIN_VALUE;
		CargoHallRenderer.@Nullable Hall hall;
		/** The round's clock (the lowest door's): the tick its round began, whether one is under way, how many it has run. */
		long start;
		boolean running;
		long rounds;

		/** How far the car runs (blocks): to the highest door, else to the top of the well; 0 none. */
		int travel() {
			return top != null ? top.getY() - bottom.getY() : head != Integer.MIN_VALUE ? head - bottom.getY() : 0;
		}
	}

	/** What one door draws this frame. */
	static final class Lift {
		boolean active;
		Direction facing = Direction.NORTH;
		float open;
		String floor = "G";
		int arrow;
		/** The car (only at the lowest door): its floor's height over this door (blocks), its gates' lift, the load. */
		boolean car;
		float carY, gateFront, gateLeft, gateRight;
		boolean loaded;
		int carLight;
		/** The cargo hall the lowest door serves, drawn with it. */
		final CargoHallRenderer.Frame hall = new CargoHallRenderer.Frame();
	}

	private GoodsLiftRenderer() {
	}

	// ------------------------------------------------------------------ reading the shaft and the round

	static void extract(RoomKitBlockEntity entity, BlockState block, Level level, float partialTick, Font font, Lift out) {
		out.active = false;
		out.hall.active = false;
		if (!(block.getBlock() instanceof GoodsLiftDoorBlock door) || !door.isAnchor(block)) {
			return;
		}
		load();
		BlockPos pos = entity.getBlockPos();
		Shaft shaft = SHAFTS.computeIfAbsent(entity, e -> new Shaft());
		long now = level.getGameTime();
		if (now - shaft.read >= RESCAN || shaft.bottom == null) {
			read(level, pos, door.facingOf(block), shaft);
			shaft.read = now;
		}
		Shaft clock = shaft;
		if (!pos.equals(shaft.bottom)) {
			// the round is the lowest door's: share its clock
			for (Map.Entry<RoomKitBlockEntity, Shaft> e : SHAFTS.entrySet()) {
				if (e.getKey().getBlockPos().equals(shaft.bottom)) {
					clock = e.getValue();
					break;
				}
			}
		}
		if (clock == shaft && pos.equals(shaft.bottom)) {
			boolean busy = busy(level, entity);
			long t = now - shaft.start;
			if (shaft.running && (t >= ROUND || t >= T_OPEN1 && !busy)) {
				if (busy && t >= ROUND) {
					shaft.start += ROUND;
					shaft.rounds++;
				} else if (!busy) {
					shaft.running = false;
				}
			}
			if (!shaft.running && busy && shaft.travel() > 0) {
				shaft.running = true;
				shaft.start = now - T_OPEN1;          // the doors open on the car waiting at the bottom
			}
		}
		double t = clock.running ? now - clock.start + partialTick : -1;
		int travel = shaft.travel();
		// where the car is (blocks over the lowest door) and which door is open how far
		double car;
		float bottomOpen = 0, topOpen = 0;
		boolean loaded;
		int arrow = 0;
		if (t < 0) {
			car = 0;
			loaded = false;
		} else if (t < T_SHUT1) {
			car = 0;
			bottomOpen = 1;
			loaded = t >= LOAD / 2.0;
		} else if (t < T_UP) {
			car = 0;
			bottomOpen = (float) (1 - (t - T_SHUT1) / DOOR);
			loaded = true;
		} else if (t < T_OPEN2) {
			car = travel * ease((t - T_UP) / RUN);
			loaded = true;
			arrow = 1;
		} else if (t < T_TOP) {
			car = travel;
			topOpen = (float) ((t - T_OPEN2) / DOOR);
			loaded = true;
		} else if (t < T_SHUT2) {
			car = travel;
			topOpen = 1;
			loaded = t < T_TOP + DWELL / 2.0;
		} else if (t < T_DOWN) {
			car = travel;
			topOpen = (float) (1 - (t - T_SHUT2) / DOOR);
			loaded = false;
		} else if (t < T_OPEN1) {
			car = travel * (1 - ease((t - T_DOWN) / RUN));
			loaded = false;
			arrow = -1;
		} else {
			car = 0;
			bottomOpen = (float) ((t - T_OPEN1) / DOOR);
			loaded = false;
		}
		boolean bottom = pos.equals(shaft.bottom);
		out.active = true;
		out.facing = door.facingOf(block);
		out.open = bottom ? bottomOpen : pos.equals(shaft.top) ? topOpen : 0;
		out.floor = car > travel / 2.0 && travel > 0 ? "R" : "G";
		out.arrow = arrow;
		out.car = bottom;
		if (bottom) {
			out.carY = (float) car;
			out.loaded = loaded;
			// the gate on the side of the open door lifts with it; the top door's side as seen from the car
			out.gateFront = bottomOpen * gateRise;
			out.gateLeft = out.gateRight = 0;
			if (shaft.top != null && topOpen > 0) {
				int side = side(out.facing, shaft.topFacing);
				if (side == 0) {
					out.gateFront = topOpen * gateRise;
				} else if (side < 0) {
					out.gateLeft = topOpen * gateRise;
				} else {
					out.gateRight = topOpen * gateRise;
				}
			}
			out.carLight = LightCoordsUtil.getLightCoords(level, pos.above((int) Math.round(car)));
			if (shaft.hall != null) {
				CargoHallRenderer.extract(shaft.hall, level, t, clock.rounds, car, out.carLight, view(level, entity), font, out.hall);
			}
		}
	}

	private static double ease(double x) {
		x = Math.max(0, Math.min(1, x));
		return x * x * (3 - 2 * x);
	}

	/**
	 * Which side of the car (as the lowest door, facing {@code bottom}, sees it) a door facing {@code top} opens on: 0 the
	 * front, -1 the left (the model's -x), 1 the right; the back is the rails'.
	 */
	private static int side(Direction bottom, Direction top) {
		if (top == bottom) {
			return 0;
		}
		// the model's +x is the door's left turned to the facing: a door facing turned clockwise from the bottom's is on +x
		return top == bottom.getClockWise() ? 1 : -1;
	}

	/** Whether the depot the door is pinned to has drones out (its last view). */
	private static boolean busy(Level level, RoomKitBlockEntity entity) {
		DepotView view = view(level, entity);
		return view != null && view.flying() > 0;
	}

	/** The last view of the depot the door is pinned to, or null. */
	private static @Nullable DepotView view(Level level, RoomKitBlockEntity entity) {
		BlockPos link = entity.terminal();
		if (link == null || !level.isLoaded(link) || !(level.getBlockEntity(link) instanceof DroneTerminalBlockEntity terminal)) {
			return null;
		}
		return terminal.view();
	}

	/** The car's well behind a door (the 3 x 3 cells a door facing {@code f} at {@code anchor} opens into). */
	private static List<BlockPos> well(BlockPos anchor, Direction f) {
		List<BlockPos> out = new ArrayList<>();
		for (int dx = 0; dx < 3; dx++) {
			for (int dz = 1; dz < 4; dz++) {
				out.add(anchor.offset(turn(dx, dz, f)));
			}
		}
		return out;
	}

	/** A cell's offset from the anchor (the door facing north) as the door faces {@code d} (tower/RoomKitBlock.offset). */
	private static BlockPos turn(int dx, int dz, Direction d) {
		return switch (d) {
			case EAST -> new BlockPos(-dz, 0, dx);
			case SOUTH -> new BlockPos(-dx, 0, -dz);
			case WEST -> new BlockPos(dz, 0, -dx);
			default -> new BlockPos(dx, 0, dz);
		};
	}

	private static void read(Level level, BlockPos pos, Direction facing, Shaft shaft) {
		List<BlockPos> mine = well(pos, facing);
		java.util.Set<Long> cells = new java.util.HashSet<>();
		for (BlockPos p : mine) {
			cells.add(BlockPos.asLong(p.getX(), 0, p.getZ()));
		}
		int minX = Integer.MAX_VALUE, maxX = Integer.MIN_VALUE, minZ = Integer.MAX_VALUE, maxZ = Integer.MIN_VALUE;
		for (BlockPos p : mine) {
			minX = Math.min(minX, p.getX());
			maxX = Math.max(maxX, p.getX());
			minZ = Math.min(minZ, p.getZ());
			maxZ = Math.max(maxZ, p.getZ());
		}
		shaft.bottom = pos;
		shaft.bottomFacing = facing;
		shaft.top = null;
		BlockPos.MutableBlockPos at = new BlockPos.MutableBlockPos();
		for (int dy = -REACH; dy <= REACH; dy++) {
			int y = pos.getY() + dy;
			if (dy == 0 || level.isOutsideBuildHeight(y)) {
				continue;
			}
			// the ring of cells round the well: a door's anchor there whose well is the same
			for (int x = minX - 1; x <= maxX + 1; x++) {
				for (int z = minZ - 1; z <= maxZ + 1; z++) {
					if (cells.contains(BlockPos.asLong(x, 0, z))) {
						continue;
					}
					at.set(x, y, z);
					if (!level.isLoaded(at)) {
						continue;
					}
					BlockState s = level.getBlockState(at);
					if (!(s.getBlock() instanceof GoodsLiftDoorBlock other) || !other.isAnchor(s)) {
						continue;
					}
					Direction f = other.facingOf(s);
					boolean same = true;
					for (BlockPos p : well(at, f)) {
						same &= cells.contains(BlockPos.asLong(p.getX(), 0, p.getZ()));
					}
					if (!same) {
						continue;
					}
					if (y < shaft.bottom.getY()) {
						shaft.bottom = at.immutable();
						shaft.bottomFacing = f;
					}
					if (y > pos.getY() && (shaft.top == null || y > shaft.top.getY())) {
						shaft.top = at.immutable();
						shaft.topFacing = f;
					}
				}
			}
		}
		if (!shaft.bottom.equals(pos)) {
			shaft.top = shaft.top == null || shaft.top.getY() < pos.getY() ? pos : shaft.top;
			if (shaft.top.equals(pos)) {
				shaft.topFacing = facing;
			}
		}
		shaft.head = Integer.MIN_VALUE;
		shaft.hall = null;
		if (shaft.bottom.equals(pos)) {
			if (shaft.top == null) {
				// no door over this one: the car runs up to the well's ceiling (its middle column's first block)
				BlockPos middle = mine.get(4);
				for (int dy = 1; dy <= REACH; dy++) {
					at.set(middle.getX(), pos.getY() + dy, middle.getZ());
					if (level.isOutsideBuildHeight(at.getY()) || !level.isLoaded(at)) {
						break;
					}
					if (!level.getBlockState(at).isAir()) {
						shaft.head = dy >= CAR_HEIGHT + 2 ? at.getY() - CAR_HEIGHT : Integer.MIN_VALUE;
						break;
					}
				}
			}
			shaft.hall = CargoHallRenderer.find(level, pos, facing);
		}
	}

	// ------------------------------------------------------------------ drawing

	static void submit(Lift lift, PoseStack poses, SubmitNodeCollector collector, Font font, int light) {
		if (!lift.active) {
			return;
		}
		CargoHallRenderer.submit(lift.hall, poses, collector, font);
		poses.pushPose();
		poses.translate(.5, 0, .5);
		poses.mulPose(new org.joml.Matrix4f().rotationY((float) ConsoleDeskRenderer.facingTurn(lift.facing)));
		poses.translate(-.5, 0, -.5);
		float open = lift.open;
		// the solid faces first, the see-through ones after (else glass drawn first hides what is behind it)
		for (boolean clear : new boolean[] {false, true}) {
			collector.submitCustomGeometry(poses, clear ? RENDER_TYPE : SOLID_TYPE, (p, consumer) -> {
				draw(p, consumer, "leaf_lower", 0, -leafDrop * open / 16F, 0, light, clear);
				draw(p, consumer, "leaf_upper", 0, leafRise * open / 16F, 0, light, clear);
				if (lift.car) {
					int cl = lift.carLight;
					draw(p, consumer, "car", 0, lift.carY, 0, cl, clear);
					draw(p, consumer, "gate_front", 0, lift.carY + lift.gateFront / 16F, 0, cl, clear);
					draw(p, consumer, "gate_left", 0, lift.carY + lift.gateLeft / 16F, 0, cl, clear);
					draw(p, consumer, "gate_right", 0, lift.carY + lift.gateRight / 16F, 0, cl, clear);
					if (lift.loaded && !lift.hall.active) {
						draw(p, consumer, "load", 0, lift.carY, 0, cl, clear);
					}
				}
			});
		}
		// the indicator: the car's floor and, while it moves, its arrow (amber, lit)
		float x0 = indicator[0], y0 = indicator[1], x1 = indicator[2], y1 = indicator[3], z = indicator[4];
		String text = lift.floor + (lift.arrow > 0 ? " ▲" : lift.arrow < 0 ? " ▼" : "");
		poses.pushPose();
		poses.translate((x0 + x1) / 2 / 16F, (y0 + y1) / 2 / 16F, z / 16F - 0.003F);
		poses.rotateDegrees(Axis.YP, 180);
		float k = (y1 - y0) * 0.7F / 16F / 8F;
		poses.scale(k, -k, k);
		var line = Texts.of(text);
		float w = font.width(line);
		collector.submitText(poses, -w / 2, -4F, line, false, Font.DisplayMode.POLYGON_OFFSET, LightCoordsUtil.FULL_BRIGHT,
				0xFF000000 | AMBER, 0, 0);
		poses.popPose();
		poses.popPose();
	}

	/** One of the live parts' models, its blocks at their offsets, moved by (dx, dy, dz) blocks; its glow kept. */
	private static void draw(PoseStack.Pose pose, VertexConsumer consumer, String key, float dx, float dy, float dz, int light, boolean clear) {
		int[][] parts = PARTS.get(key);
		if (parts == null) {
			return;
		}
		for (int[] part : parts) {
			float ox = part[1] + dx, oy = part[2] + dy, oz = part[3] + dz;
			for (BakedQuad quad : quads(part[0])) {
				if ((quad.materialInfo().layer() == net.minecraft.client.renderer.chunk.ChunkSectionLayer.TRANSLUCENT) != clear) {
					continue;
				}
				int emission = quad.materialInfo().lightEmission();
				int lit = emission > 0 ? LightCoordsUtil.lightCoordsWithEmission(light, emission) : light;
				Direction d = quad.direction();
				for (int i = 0; i < 4; i++) {
					Vector3fc v = quad.position(i);
					long uv = quad.packedUV(i);
					consumer.addVertex(pose, v.x() + ox, v.y() + oy, v.z() + oz).setColor(0xFFFFFFFF).setUv(UVPair.unpackU(uv), UVPair.unpackV(uv))
							.setOverlay(OverlayTexture.NO_OVERLAY).setLight(lit).setNormal(pose, d.getStepX(), d.getStepY(), d.getStepZ());
				}
			}
		}
	}

	private static List<BakedQuad> quads(int part) {
		Minecraft minecraft = Minecraft.getInstance();
		var block = io.github.jimbozoomer.jugcraft.tower.RoomKit.BLOCKS.get("goods_lift_door");
		if (!(block.property("part") instanceof IntegerProperty prop) || !prop.getPossibleValues().contains(part)) {
			return List.of();
		}
		BlockState state = block.defaultBlockState().setValue(prop, part)
				.setValue(io.github.jimbozoomer.jugcraft.tower.RoomKitBlock.FACING_PROP, Direction.NORTH);
		BlockStateModel model = minecraft.getModelManager().getBlockStateModelSet().get(state);
		Cached cached = QUADS.get(part);
		if (cached != null && cached.model() == model) {
			return cached.quads();
		}
		List<BlockStateModelPart> parts = new ArrayList<>();
		model.collectParts(RandomSource.create(42L), parts);
		List<BakedQuad> quads = new ArrayList<>();
		for (BlockStateModelPart p : parts) {
			for (Direction side : Direction.values()) {
				quads.addAll(p.getQuads(side));
			}
			quads.addAll(p.getQuads(null));
		}
		QUADS.put(part, new Cached(model, quads));
		return quads;
	}

	/** Reads jugcraft/goods_lift.json (tools/goods_lift.py): each live part's blocks, and how far things move. */
	private static void load() {
		if (loaded) {
			return;
		}
		loaded = true;
		try (InputStream in = GoodsLiftRenderer.class.getResourceAsStream("/jugcraft/goods_lift.json")) {
			if (in == null) {
				return;
			}
			JsonObject root = JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
			for (Map.Entry<String, JsonElement> e : root.getAsJsonObject("parts").entrySet()) {
				JsonArray a = e.getValue().getAsJsonArray();
				int[][] parts = new int[a.size()][];
				for (int i = 0; i < a.size(); i++) {
					JsonArray q = a.get(i).getAsJsonArray();
					parts[i] = new int[] {q.get(0).getAsInt(), q.get(1).getAsInt(), q.get(2).getAsInt(), q.get(3).getAsInt()};
				}
				PARTS.put(e.getKey(), parts);
			}
			leafDrop = root.get("leaf_drop").getAsFloat();
			leafRise = root.get("leaf_rise").getAsFloat();
			gateRise = root.get("gate_rise").getAsFloat();
			JsonArray ind = root.getAsJsonArray("indicator");
			for (int i = 0; i < 5; i++) {
				indicator[i] = ind.get(i).getAsFloat();
			}
		} catch (java.io.IOException | RuntimeException e) {
			io.github.jimbozoomer.jugcraft.Jugcraft.LOGGER.error("jugcraft/goods_lift.json could not be read", e);
		}
	}
}
