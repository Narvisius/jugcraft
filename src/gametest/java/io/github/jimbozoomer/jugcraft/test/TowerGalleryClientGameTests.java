package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.tower.TowerData;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;

/**
 * A catalogue of every block the Tri-Pillar Drone Tower is built from, for the owner's review: each block (or the
 * fitting it forms, copied out of the tower's own design data) stands alone on a light grey floor inside a black box,
 * shot from the front and, for fittings, from behind. Building materials are shown as a 3x3 sample wall or floor.
 * Focus word {@code gallery}; screenshots {@code gallery_NN_name_front/back}.
 */
public class TowerGalleryClientGameTests implements FabricClientGameTest {
	private static final int WALL = 0, FLOOR = 1, STAIRS = 2, REGION = 3, CEILING = 4, DRONE = 5;
	/** Each drone tier's size across (blocks), so its close-up frames it: the new life-size models. */
	private static final double[] DRONE_SIZE = {1.6, 2.0, 1.8, 2.7, 4.7, 4.4, 7.4, 7.2, 8.0};

	/** One picture: a sample of {@code kinds[0]}, or the tower's blocks of {@code kinds} (all when empty) inside {@code box}. */
	private record Shot(String name, int mode, int[] box, String... kinds) {
	}

	private static Shot sample(String kind, int mode) {
		return new Shot(kind, mode, null, kind);
	}

	private static Shot region(String name, int x0, int y0, int z0, int x1, int y1, int z1, String... kinds) {
		return new Shot(name, REGION, new int[] {x0, y0, z0, x1, y1, z1}, kinds);
	}

	private static final Shot[] SHOTS = {
			sample("carbon_composite_panel", WALL), sample("reinforced_concrete", WALL), sample("steel_armor_plate", WALL),
			sample("framed_glass", WALL), sample("blast_glass", WALL), sample("access_floor_tile", FLOOR),
			sample("epoxy_floor", FLOOR), sample("dock_plating", FLOOR), sample("ceiling_tile", FLOOR),
			sample("hazard_plating", FLOOR), sample("landing_platform", FLOOR), sample("steel_armor_plate_slab", FLOOR),
			sample("hangar_pad", FLOOR), sample("reinforced_concrete_stairs", STAIRS),
			region("light_rod_strip", -26, 1, -15, -24, 9, -13),
			region("tower_ladder", -22, 1, -15, -20, 6, -13),
			region("steel_girder", -8, 1, -53, -8, 9, -53, "steel_girder"),
			region("cradle_struts", -26, 190, -20, -20, 197, -14, "cradle_struts", "cradle_struts_part"),
			new Shot("ceiling_light_panel", CEILING, new int[] {-52, 9, -2, -48, 10, 2}),
			new Shot("linear_ceiling_light", CEILING, new int[] {-22, 9, 0, -14, 10, 6}),
			region("roof_pad_7x7", -39, 20, 5, -33, 20, 11, "hangar_pad"),
			region("bay_pad_3x3", -18, 20, -16, -16, 20, -14, "hangar_pad"),
			region("yard_landing_pad", 46, 0, -22, 54, 1, -14, "landing_pad", "landing_platform"),
			region("supply_pickup", -27, 199, -21, -25, 199, -19, "supply_pickup"),
			region("warning_light", -30, 198, -24, -28, 199, -22),
			region("energy_exchange", 56, 1, -34, 61, 5, -26, "energy_exchange"),
			region("cargo_exchange", 53, 1, -4, 58, 5, 4, "cargo_exchange"),
			region("cargo_packager", 58, 1, -16, 60, 1, -14),
			region("drone_depot_terminal", 50, 1, -11, 50, 1, -11, "drone_depot_terminal"),
			region("tower_control_console", -5, 1, -10, 5, 1, -6, "tower_control_console"),
			region("console_chair", 0, 1, -11, 0, 1, -11, "console_chair"),
			region("raised_floor", -5, 1, -12, 5, 1, -11, "raised_floor"),
			region("operator_desk", -31, 1, 6, -29, 1, 10),
			region("curved_video_wall", -9, 1, 11, 9, 9, 16, "curved_video_wall", "curved_video_wall_part"),
			region("holo_command_table", -5, 1, -5, 5, 1, 5, "holo_command_table"),
			region("holo_projector", -1, 1, -34, 1, 1, -32, "holo_projector", "holo_projector_part"),
			region("big_map_table", -5, 12, -40, 7, 12, -28, "big_map_table", "big_map_table_part"),
			region("map_room_sign", -1, 17, -21, 2, 17, -21, "map_room_sign"),
			region("shower_stall", -25, 1, 34, -25, 1, 36, "shower_stall"),
			// the ground floor's rooms (layout 24, item 22 part A): each room's floor, walls and fittings, its ceiling left off
			region("room_security", -37, 0, -19, -28, 4, -3), region("room_cafe", -24, 0, 30, 24, 4, 44),
			region("room_lockers", -41, 0, 21, -24, 4, 44), region("room_battery", 24, 0, 21, 41, 4, 44),
			region("room_restroom", -29, 0, -45, -21, 4, -30),
			// the upper floor's rooms (layout 25, item 22 part B): each room's floor, walls and fittings, its ceiling left off
			region("room_briefing", 12, 11, -46, 27, 17, -21), region("room_manager", 25, 11, -40, 42, 17, -21),
			region("room_lab", 27, 11, -19, 53, 17, 7), region("room_first_aid", 29, 11, 5, 53, 17, 23),
			region("room_simulators", -36, 11, 4, -12, 17, 23),
			region("room_lounge", -20, 11, 28, 20, 17, 46), region("room_offices_se", 22, 11, 21, 40, 17, 46),
			region("room_offices_sw", -40, 11, 21, -22, 17, 46),
			region("room_training", -53, 11, -18, -36, 17, 18), region("room_meeting", -39, 11, -18, -27, 17, -4),
			region("room_restrooms", -27, 11, -45, -10, 17, -30),
			region("maintenance_door", 0, 1, 26, 1, 2, 26, "maintenance_door"),
			region("maintenance_door_hood", 9, 21, 18, 9, 22, 19, "maintenance_door"),
			region("data_hall", -51, 0, -10, -36, 5, 9, "aisle_containment_door", "aisle_containment_door_part", "aisle_containment_roof",
					"data_hall_sign"),
			region("parts_shelving", 21, 1, -36, 29, 3, -34, "drone_parts_shelving", "drone_parts_shelving_part"),
			region("drone_display_case", -17, 0, -41, -11, 4, -35, "drone_display_case", "drone_display_case_part", "neon_floor_epoxy"),
			region("neon_lines", -8, 0, 25, 8, 9, 27, "neon_tube", "neon_line_concrete", "maintenance_door", "reinforced_concrete"),
			region("neon_floor_line", -1, 0, -40, 1, 0, -34, "neon_floor_epoxy", "epoxy_floor"),
			region("service_bay", 24, 1, -33, 30, 4, -27, "drone_display_stand", "drone_service_arm"),
			region("info_totem", -7, 1, -42, -7, 1, -42, "tower_info_totem"),
			region("speed_gates", -5, 1, -42, 5, 2, -39, "speed_gate", "speed_gate_pedestal"),
			region("glass_door", -10, 21, -30, -10, 22, -29, "glass_door"),
			region("entrance_door", -3, 1, -45, 2, 3, -45, "automatic_entrance_door", "sliding_door_part"),
			region("sliding_door", 30, 1, 14, 30, 2, 15, "automatic_sliding_door_glass", "sliding_door_part"),
			region("crystal_tree", 10, 1, -31, 18, 4, -23, "crystal_tree", "crystal_tree_part"),
			region("lounge", -18, 1, -30, -11, 2, -25, "long_coffee_table", "long_coffee_table_part", "sofa"),
			region("cable_tray", -6, -1, -6, -4, 0, -4),
			new Shot("drone_t1", DRONE, null, "1"), new Shot("drone_t2", DRONE, null, "2"), new Shot("drone_t3", DRONE, null, "3"),
			new Shot("drone_t4", DRONE, null, "4"), new Shot("drone_t5", DRONE, null, "5"), new Shot("drone_t6", DRONE, null, "6"),
			new Shot("drone_t7", DRONE, null, "7"), new Shot("drone_t8", DRONE, null, "8"), new Shot("drone_t9", DRONE, null, "9"),
	};

	@Override
	public void runTest(ClientGameTestContext context) {
		if (TestFocus.skip("gallery") || GuideScreenshotGameTests.active()) {
			return;
		}
		context.runOnClient(client -> client.getWindow().setWindowed(1280, 720));
		try (TestSingleplayerContext singleplayer = context.worldBuilder()
				.adjustSettings(creator -> creator.setGameMode(WorldCreationUiState.SelectedGameMode.CREATIVE)).create()) {
			singleplayer.getConnection().waitForChunksRender();
			TestServerContext server = singleplayer.getServer();
			BlockPos origin = context.computeOnClient(client -> BlockPos.containing(client.player.position())).offset(0, 0, 24);
			server.runCommand("time set noon");
			server.runCommand("weather clear");
			server.runOnServer(minecraft -> {
				var player = minecraft.getPlayerList().getPlayers().getFirst();
				player.getAbilities().flying = true;
				player.onUpdateAbilities();
			});
			context.runOnClient(client -> {
				if (!client.gui.hud.isHidden()) {
					client.gui.hud.toggle();
				}
			});
			int index = 0;
			for (Shot shot : SHOTS) {
				index++;
				double[] cams = server.computeOnServer(minecraft -> build(minecraft.overworld(), origin, shot));
				if (cams == null) {
					System.out.println("[Jugcraft gallery] nothing to show for " + shot.name());
					continue;
				}
				String base = "gallery_%02d_%s".formatted(index, shot.name());
				photo(context, singleplayer, server, origin, cams, 0, base + (shot.mode() == REGION ? "_front" : ""));
				if (shot.mode() == REGION) {
					photo(context, singleplayer, server, origin, cams, 6, base + "_back");
				}
			}
		}
	}

	private static void photo(ClientGameTestContext context, TestSingleplayerContext singleplayer, TestServerContext server,
			BlockPos origin, double[] cams, int at, String name) {
		server.runCommand("time set noon");
		server.runCommand("tp @p %.3f %.3f %.3f facing %.3f %.3f %.3f".formatted(
				origin.getX() + cams[at], origin.getY() + cams[at + 1] - 1.62, origin.getZ() + cams[at + 2],
				origin.getX() + cams[12], origin.getY() + cams[13], origin.getZ() + cams[14]));
		context.waitTicks(12);
		singleplayer.getConnection().waitForChunksRender();
		context.waitTicks(8);
		context.takeScreenshot(name);
	}

	/** Every block of the tower's design (foundation and tiers), by position relative to the core. */
	private static Map<BlockPos, BlockState> towerBlocks;

	private static synchronized Map<BlockPos, BlockState> tower() {
		if (towerBlocks == null) {
			TowerData data = TowerData.get(TowerData.CURRENT_LAYOUT);
			towerBlocks = new HashMap<>();
			List<TowerData.Tier> steps = new ArrayList<>(data.tiers);
			if (data.foundation != null) {
				steps.addFirst(data.foundation);
			}
			for (TowerData.Tier step : steps) {
				for (int[] b : step.place) {
					towerBlocks.put(new BlockPos(b[0], b[1], b[2]), data.state(b[3]));
				}
			}
		}
		return towerBlocks;
	}

	private static Block block(String kind) {
		String id = kind.contains(":") ? kind : (kind.endsWith("leaves") || kind.equals("moss_block") || kind.equals("stripped_birch_log")
				? "minecraft:" : "jugcraft:") + kind;
		return BuiltInRegistries.BLOCK.getValue(Identifier.parse(id));
	}

	/**
	 * Clears the stage, builds the shot on it and returns the camera positions (front x,y,z, then back x,y,z) and the
	 * point both look at (index 12), relative to {@code origin}; null when the design has none of the blocks.
	 */
	private static double[] build(ServerLevel level, BlockPos origin, Shot shot) {
		Map<BlockPos, BlockState> blocks = new HashMap<>();
		int flags = Block.UPDATE_ALL;
		if (shot.mode() == DRONE) {
			// A drone of the tier on a display stand: it turns slowly over the stand, full size.
			int tier = Integer.parseInt(shot.kinds()[0]);
			blocks.put(BlockPos.ZERO, block("drone_display_stand").defaultBlockState()
					.setValue(io.github.jimbozoomer.jugcraft.tower.HallFixtureBlock.Stand.TIER, tier));
		} else if (shot.mode() == WALL || shot.mode() == FLOOR || shot.mode() == STAIRS) {
			BlockState state = block(shot.kinds()[0]).defaultBlockState();
			for (int a = -1; a <= 1; a++) {
				for (int b = 0; b <= 2; b++) {
					switch (shot.mode()) {
						case WALL -> blocks.put(new BlockPos(a, b, 0), state);
						case FLOOR -> blocks.put(new BlockPos(a, 0, b - 1), state);
						default -> {
							// Three steps up northward, three wide.
							blocks.put(new BlockPos(a, b, -b), state.hasProperty(net.minecraft.world.level.block.StairBlock.FACING)
									? state.setValue(net.minecraft.world.level.block.StairBlock.FACING, Direction.NORTH) : state);
						}
					}
				}
			}
		} else {
			List<String> kinds = List.of(shot.kinds());
			int[] box = shot.box();
			Direction facing = null;
			for (var entry : tower().entrySet()) {
				BlockPos p = entry.getKey();
				if (p.getX() < box[0] || p.getY() < box[1] || p.getZ() < box[2] || p.getX() > box[3] || p.getY() > box[4] || p.getZ() > box[5]) {
					continue;
				}
				String kind = BuiltInRegistries.BLOCK.getKey(entry.getValue().getBlock()).getPath();
				if (!kinds.isEmpty() && !kinds.contains(kind)) {
					continue;
				}
				blocks.put(p, entry.getValue());
				if (facing == null && (kinds.isEmpty() || kind.equals(kinds.getFirst()))) {
					facing = horizontalFacing(entry.getValue());
				}
			}
			if (blocks.isEmpty()) {
				return null;
			}
			// Turn the fitting so its front faces the camera (south); shift it onto the stage.
			Rotation turn = Rotation.NONE;
			if (shot.mode() == REGION && facing != null) {
				for (Rotation r : Rotation.values()) {
					if (r.rotate(facing) == Direction.SOUTH) {
						turn = r;
					}
				}
			}
			Map<BlockPos, BlockState> turned = new HashMap<>();
			for (var e : blocks.entrySet()) {
				turned.put(e.getKey().rotate(turn), e.getValue().rotate(turn));
			}
			int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE, minZ = Integer.MAX_VALUE;
			int maxX = Integer.MIN_VALUE, maxZ = Integer.MIN_VALUE;
			for (BlockPos p : turned.keySet()) {
				minX = Math.min(minX, p.getX());
				minY = Math.min(minY, p.getY());
				minZ = Math.min(minZ, p.getZ());
				maxX = Math.max(maxX, p.getX());
				maxZ = Math.max(maxZ, p.getZ());
			}
			int sx = -(minX + maxX) / 2, sy = (shot.mode() == CEILING ? 4 : 0) - minY, sz = -(minZ + maxZ) / 2;
			blocks.clear();
			for (var e : turned.entrySet()) {
				blocks.put(e.getKey().offset(sx, sy, sz), e.getValue());
			}
			// Exactly as the tower has them (joined glass, formed parts): no neighbour shape updates.
			flags = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE;
		}
		double[] lo = {Double.MAX_VALUE, Double.MAX_VALUE, Double.MAX_VALUE}, hi = {-Double.MAX_VALUE, -Double.MAX_VALUE, -Double.MAX_VALUE};
		for (BlockPos p : blocks.keySet()) {
			lo[0] = Math.min(lo[0], p.getX());
			lo[1] = Math.min(lo[1], p.getY());
			lo[2] = Math.min(lo[2], p.getZ());
			hi[0] = Math.max(hi[0], p.getX() + 1);
			hi[1] = Math.max(hi[1], p.getY() + 1);
			hi[2] = Math.max(hi[2], p.getZ() + 1);
		}
		if (shot.name().equals("holo_projector")) {
			// the hologram is no block: frame the air it fills too (the crown some 7.5 blocks over the floor, the ring of
			// words 3.4 out from the middle)
			hi[1] = lo[1] + 8;
			for (int a : new int[] {0, 2}) {
				lo[a] -= 2;
				hi[a] += 2;
			}
		}
		double cx = (lo[0] + hi[0]) / 2, cy = (lo[1] + hi[1]) / 2, cz = (lo[2] + hi[2]) / 2;
		double radius = Math.sqrt(Math.pow(hi[0] - lo[0], 2) + Math.pow(hi[1] - lo[1], 2) + Math.pow(hi[2] - lo[2], 2)) / 2;
		if (shot.mode() == DRONE) {
			double size = DRONE_SIZE[Integer.parseInt(shot.kinds()[0]) - 1];
			radius = size / 2 + 0.4;
			cy = 0.4 + size * 0.18;
		}
		double d = Math.max(3.2, Math.min(15, radius * 1.9 + 1.2));
		double pitch = Math.toRadians(shot.mode() == FLOOR ? 40 : 24), yaw = Math.toRadians(32);
		double[] cams = new double[15];
		double h = d * Math.cos(pitch);
		cams[0] = cx + h * Math.sin(yaw);
		cams[1] = cy + d * Math.sin(pitch);
		cams[2] = cz + h * Math.cos(yaw);
		cams[6] = cx - h * Math.sin(yaw);
		cams[7] = cams[1];
		cams[8] = cz - h * Math.cos(yaw);
		if (shot.mode() == CEILING) {
			cams[0] = cx + 2.5;
			cams[1] = 1.4;
			cams[2] = cz + 5.5;
		}
		cams[12] = cx;
		cams[13] = cy;
		cams[14] = cz;
		// The stage: clear, a light grey floor, black walls round it.
		int r = (int) Math.ceil(Math.max(h + 3, Math.max(hi[0] - lo[0], hi[2] - lo[2]) / 2 + 4));
		BlockState air = Blocks.AIR.defaultBlockState();
		for (int x = -r - 1; x <= r + 1; x++) {
			for (int z = -r - 1; z <= r + 1; z++) {
				for (int y = 24; y >= 0; y--) {
					BlockPos at = origin.offset(x, y, z);
					if (!level.getBlockState(at).isAir()) {
						level.setBlock(at, air, Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE | Block.UPDATE_SUPPRESS_DROPS);
					}
				}
				boolean wall = Math.abs(x) == r + 1 || Math.abs(z) == r + 1;
				level.setBlock(origin.offset(x, -1, z), (wall ? block("minecraft:black_concrete") : block("minecraft:light_gray_concrete")).defaultBlockState(), Block.UPDATE_CLIENTS);
				if (wall) {
					for (int y = 0; y <= 18; y++) {
						level.setBlock(origin.offset(x, y, z), block("minecraft:black_concrete").defaultBlockState(), Block.UPDATE_CLIENTS);
					}
				}
			}
		}
		level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,
				new net.minecraft.world.phys.AABB(origin).inflate(r + 4, 30, r + 4)).forEach(net.minecraft.world.entity.Entity::discard);
		for (var e : blocks.entrySet()) {
			level.setBlock(origin.offset(e.getKey()), e.getValue(), flags);
		}
		return cams;
	}

	private static Direction horizontalFacing(BlockState state) {
		for (Property<?> property : state.getProperties()) {
			if (property.getName().equals("facing") && state.getValue(property) instanceof Direction direction && direction.getAxis().isHorizontal()) {
				return direction;
			}
		}
		return null;
	}
}
