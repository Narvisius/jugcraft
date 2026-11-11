package io.github.jimbozoomer.jugcraft.warehouse;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * What the warehouse's Java needs from its models (tools/warehouse_data.py writes assets/jugcraft/warehouse.json): the
 * pieces' hitboxes (worked out from their boxes), the parts' and loads' numbers, and the dimensions the drones and the
 * renderers work by (where a case sits on the intake's conveyor, the stacker's arm lengths, the pallet drone's mast).
 * Read once from the mod's own jar, on whichever side asks first.
 */
public final class WarehouseLayout {
	private static WarehouseLayout instance;
	private final JsonObject root;
	private final Map<String, Map<Direction, VoxelShape[]>> pieceShapes = new HashMap<>();
	private final Map<String, Map<Direction, VoxelShape>> rackShapes = new HashMap<>();
	private final Map<String, Map<Direction, VoxelShape>> plainShapes = new HashMap<>();
	public final List<String> parts;
	public final List<String> loadKinds;
	public final List<String> caseKinds;
	public final int maxCases;

	private WarehouseLayout(JsonObject root) {
		this.root = root;
		this.parts = strings(root.getAsJsonArray("parts"));
		this.loadKinds = strings(root.getAsJsonArray("load_kinds"));
		this.caseKinds = strings(root.getAsJsonArray("case_kinds"));
		this.maxCases = root.get("max_cases").getAsInt();
		for (Map.Entry<String, JsonElement> e : root.getAsJsonObject("pieces").entrySet()) {
			JsonArray parts = e.getValue().getAsJsonObject().getAsJsonArray("parts");
			Map<Direction, VoxelShape[]> byFacing = new EnumMap<>(Direction.class);
			for (Direction facing : Direction.Plane.HORIZONTAL) {
				VoxelShape[] shapes = new VoxelShape[parts.size()];
				for (int p = 0; p < shapes.length; p++) {
					shapes[p] = shape(parts.get(p).getAsJsonArray(), facing);
				}
				byFacing.put(facing, shapes);
			}
			pieceShapes.put(e.getKey(), byFacing);
		}
		for (Map.Entry<String, JsonElement> e : root.getAsJsonObject("rack").entrySet()) {
			Map<Direction, VoxelShape> byFacing = new EnumMap<>(Direction.class);
			for (Direction facing : Direction.Plane.HORIZONTAL) {
				byFacing.put(facing, shape(e.getValue().getAsJsonArray(), facing));
			}
			rackShapes.put(e.getKey(), byFacing);
		}
		for (String name : new String[] {"dock", "conduit", "trunking", "end_barrier", "crossing_beacon", "aisle_sign", "bollard"}) {
			Map<Direction, VoxelShape> byFacing = new EnumMap<>(Direction.class);
			for (Direction facing : Direction.Plane.HORIZONTAL) {
				byFacing.put(facing, shape(root.getAsJsonArray(name), facing));
			}
			plainShapes.put(name, byFacing);
		}
	}

	public static WarehouseLayout get() {
		if (instance == null) {
			try (InputStream in = WarehouseLayout.class.getResourceAsStream("/assets/jugcraft/warehouse.json")) {
				if (in == null) {
					throw new IllegalStateException("assets/jugcraft/warehouse.json is missing");
				}
				instance = new WarehouseLayout(JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject());
			} catch (java.io.IOException e) {
				Jugcraft.LOGGER.error("Could not read the warehouse layout", e);
				throw new IllegalStateException(e);
			}
		}
		return instance;
	}

	public VoxelShape piece(String name, Direction facing, int part) {
		VoxelShape[] shapes = pieceShapes.get(name).get(facing);
		return shapes[Math.min(part, shapes.length - 1)];
	}

	public VoxelShape rack(boolean left, boolean right, String level, boolean top, Direction facing) {
		return rackShapes.get((left ? "1" : "0") + (right ? "1" : "0") + "_" + level + (top ? "1" : "0")).get(facing);
	}

	public VoxelShape plain(String name, Direction facing) {
		return plainShapes.get(name).get(facing);
	}

	/** A section of the layout (pallet, pallet_drone, stacker_drone, intake, station, terminal, goods_in, charger...). */
	public JsonObject section(String name) {
		return root.getAsJsonObject(name);
	}

	public JsonArray array(String name) {
		return root.getAsJsonArray(name);
	}

	public double number(String section, String key) {
		return root.getAsJsonObject(section).get(key).getAsDouble();
	}

	public int part(String name) {
		int i = parts.indexOf(name);
		if (i < 0) {
			throw new IllegalArgumentException("No warehouse part " + name);
		}
		return i;
	}

	private static List<String> strings(JsonArray array) {
		List<String> out = new java.util.ArrayList<>();
		array.forEach(e -> out.add(e.getAsString()));
		return out;
	}

	private static VoxelShape shape(JsonArray boxes, Direction facing) {
		VoxelShape shape = Shapes.empty();
		for (JsonElement e : boxes) {
			JsonArray b = e.getAsJsonArray();
			double[] box = new double[6];
			for (int i = 0; i < 6; i++) {
				box[i] = b.get(i).getAsDouble();
			}
			shape = Shapes.or(shape, turned(box, facing));
		}
		return shape.isEmpty() ? Shapes.empty() : shape.optimize();
	}

	/** A box given for a block facing north (its front south), turned to {@code facing} as the blockstate turns its model. */
	public static VoxelShape turned(double[] b, Direction facing) {
		double x0 = b[0], z0 = b[2], x1 = b[3], z1 = b[5];
		int turns = switch (facing) {
			case EAST -> 1;
			case SOUTH -> 2;
			case WEST -> 3;
			default -> 0;
		};
		for (int i = 0; i < turns; i++) {
			double nx0 = 16 - z1, nx1 = 16 - z0, nz0 = x0, nz1 = x1;
			x0 = nx0;
			x1 = nx1;
			z0 = nz0;
			z1 = nz1;
		}
		return Block.box(x0, b[1], z0, x1, b[4], z1);
	}
}
