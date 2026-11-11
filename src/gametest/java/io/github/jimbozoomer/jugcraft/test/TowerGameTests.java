package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.drone.DepotDisplayBlockEntity;
import io.github.jimbozoomer.jugcraft.drone.DroneSize;
import io.github.jimbozoomer.jugcraft.drone.DroneTerminalBlockEntity;
import io.github.jimbozoomer.jugcraft.drone.DroneTier;
import io.github.jimbozoomer.jugcraft.drone.HoloTableBlock;
import io.github.jimbozoomer.jugcraft.tower.HoloCommandTableBlock;
import io.github.jimbozoomer.jugcraft.drone.JugcraftDrones;
import io.github.jimbozoomer.jugcraft.tower.ExchangeStationBlock;
import io.github.jimbozoomer.jugcraft.tower.ExchangeStations;
import io.github.jimbozoomer.jugcraft.tower.HangarPadBlock;
import io.github.jimbozoomer.jugcraft.tower.JugcraftTower;
import io.github.jimbozoomer.jugcraft.tower.crew.CrewShip;
import io.github.jimbozoomer.jugcraft.tower.TowerCoreBlockEntity;
import io.github.jimbozoomer.jugcraft.tower.TowerData;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * In-game tests for the Tri-Pillar Drone Tower (docs/features/drone-tower-tripillar.md): its data, the core building
 * the foundation and the roof pads itself, tower tiers gating drone tiers, the finished tower holding its whole fleet,
 * and drones flying the next tier's tiles in from the blocks paid for it. Towers stand in {@link TriPillarArena}.
 */
public class TowerGameTests {
	@GameTest
	public void towerDoesNotClearExternalChunkLoads(GameTestHelper helper) {
		if (TestFocus.skip("tower")) {
			helper.succeed();
			return;
		}
		BlockPos core = helper.absolutePos(BlockPos.ZERO);
		var chunk = net.minecraft.world.level.ChunkPos.containing(core);
		ServerLevel level = helper.getLevel();
		boolean wasForced = level.getChunkSource().getForceLoadedChunks().contains(chunk.pack());
		level.setChunkForced(chunk.x(), chunk.z(), true);
		TowerCoreBlockEntity.keepLoaded(level, core, true);
		TowerCoreBlockEntity.keepLoaded(level, core, false);
		helper.assertTrue(level.getChunkSource().getForceLoadedChunks().contains(chunk.pack()), "Tower removal preserves external forced chunks");
		if (!wasForced) level.setChunkForced(chunk.x(), chunk.z(), false);
		helper.succeed();
	}

	/**
	 * The tower's holograms (tower/Holograms; Justin, 28 October 2026): the Hologram Table, Holo Command Table, Holo
	 * Projector, Big Map Table and Holo Panel Emitter (and the parts) have no recipe, no tool breaks them (as bedrock), no
	 * explosion or piston moves them, they're wither and dragon immune; in survival the item places nothing, in creative
	 * it places.
	 */
	@GameTest
	public void hologramsAreTheTowersOwn(GameTestHelper helper) {
		if (TestFocus.skip("tower")) {
			helper.succeed();
			return;
		}
		ServerLevel level = helper.getLevel();
		for (String id : io.github.jimbozoomer.jugcraft.tower.Holograms.BLOCKS) {
			var block = net.minecraft.core.registries.BuiltInRegistries.BLOCK.getValue(io.github.jimbozoomer.jugcraft.Jugcraft.id(id));
			BlockState state = block.defaultBlockState();
			helper.assertTrue(state.getDestroySpeed(level, BlockPos.ZERO) < 0, id + " can't be mined");
			helper.assertTrue(block.getExplosionResistance() >= 3_600_000F, id + " stands any explosion");
			helper.assertTrue(state.getPistonPushReaction() == net.minecraft.world.level.material.PushReaction.IMMOVEABLE, id + " no piston moves");
			helper.assertTrue(state.is(net.minecraft.tags.BlockTags.WITHER_IMMUNE) && state.is(net.minecraft.tags.BlockTags.DRAGON_IMMUNE),
					id + " is wither and dragon immune");
		}
		for (String id : io.github.jimbozoomer.jugcraft.tower.Holograms.ITEMS) {
			var item = net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(io.github.jimbozoomer.jugcraft.Jugcraft.id(id));
			helper.assertTrue(item instanceof io.github.jimbozoomer.jugcraft.tower.Holograms.TowerOnlyItem, id + "'s item places in creative only");
			helper.assertTrue(level.recipeAccess().byKey(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.RECIPE,
					io.github.jimbozoomer.jugcraft.Jugcraft.id(id))).isEmpty(), id + " has no recipe");
		}
		// a survival player's emitter places nothing; a creative player's does
		BlockPos floor = helper.absolutePos(new BlockPos(2, 1, 2));
		level.setBlock(floor, Blocks.STONE.defaultBlockState(), net.minecraft.world.level.block.Block.UPDATE_ALL);
		var emitter = io.github.jimbozoomer.jugcraft.test.TowerGameTests.modBlock("holo_emitter");
		var hit = new net.minecraft.world.phys.BlockHitResult(Vec3.atCenterOf(floor).add(0, 0.5, 0), Direction.UP, floor, false);
		for (GameType mode : new GameType[] {GameType.SURVIVAL, GameType.CREATIVE}) {
			Player player = helper.makeMockPlayer(mode);
			player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, new ItemStack(emitter));
			new ItemStack(emitter).useOn(new net.minecraft.world.item.context.UseOnContext(player, net.minecraft.world.InteractionHand.MAIN_HAND, hit));
			boolean placed = level.getBlockState(floor.above()).is(emitter);
			helper.assertTrue(placed == (mode == GameType.CREATIVE), mode + ": the emitter " + (placed ? "placed" : "didn't place"));
		}
		helper.succeed();
	}

	/**
	 * The Advanced Crafting Table (take 2 approved 26 October 2026; crafting/, tools/adv_crafting_table.py): its anchor
	 * put down places the whole table (3 wide, 2 high), the grid at the anchor. A 6 x 6 recipe (the test pack's: three
	 * sticks and a log in a row makes three ladders) matches anywhere on the grid and mirrored, not with an extra item
	 * or turned upright; making it takes one of each item; the T1 drone's own 6 x 6 recipe makes it; the grid's items
	 * drop when the table is broken.
	 */
	@GameTest
	public void advancedCraftingTableCraftsSixBySix(GameTestHelper helper) {
		if (TestFocus.skip("tower")) {
			helper.succeed();
			return;
		}
		ServerLevel level = helper.getLevel();
		var block = io.github.jimbozoomer.jugcraft.tower.RoomKit.BLOCKS.get("advanced_crafting_table");
		helper.assertTrue(block instanceof io.github.jimbozoomer.jugcraft.crafting.AdvancedCraftingTableBlock, "the table is a room kit piece");
		BlockPos anchor = helper.absolutePos(new BlockPos(1, 1, 1));
		BlockState state = block.defaultBlockState();
		level.setBlock(anchor, state, net.minecraft.world.level.block.Block.UPDATE_ALL);
		List<BlockPos> cells = block.cells(anchor, state);
		helper.assertTrue(cells.size() == 6, "the table is six blocks, not " + cells.size());
		for (BlockPos p : cells) {
			helper.assertTrue(level.getBlockState(p).is(block), "the table's block at " + p);
		}
		var table = (io.github.jimbozoomer.jugcraft.crafting.AdvancedCraftingTableBlockEntity) level.getBlockEntity(anchor);
		helper.assertTrue(table != null && level.getBlockEntity(cells.get(1)) == null, "the grid is at the anchor only");
		var stick = net.minecraft.world.item.Items.STICK;
		var log = net.minecraft.world.item.Items.OAK_LOG;
		var ladder = net.minecraft.world.item.Items.LADDER;
		// row 2, columns 1..4: S S S L
		for (int x = 1; x <= 3; x++) {
			table.setItem(2 * 6 + x, new ItemStack(stick, 2));
		}
		table.setItem(2 * 6 + 4, new ItemStack(log, 2));
		helper.assertTrue(table.result().is(ladder) && table.result().getCount() == 3, "three sticks and a log make ladders: " + table.result());
		table.setItem(5 * 6, new ItemStack(stick));
		helper.assertTrue(table.result().isEmpty(), "an extra stick spoils it");
		table.setItem(5 * 6, ItemStack.EMPTY);
		// mirrored (L S S S) lower down
		for (int x = 1; x <= 4; x++) {
			table.setItem(2 * 6 + x, ItemStack.EMPTY);
		}
		table.setItem(4 * 6 + 2, new ItemStack(log, 2));
		for (int x = 3; x <= 5; x++) {
			table.setItem(4 * 6 + x, new ItemStack(stick, 2));
		}
		helper.assertTrue(table.result().is(ladder), "it matches mirrored, anywhere on the grid");
		Player player = helper.makeMockPlayer(GameType.SURVIVAL);
		ItemStack made = table.craft(player);
		helper.assertTrue(made.is(ladder) && made.getCount() == 3, "making it gives three ladders: " + made);
		helper.assertTrue(table.getItem(4 * 6 + 2).getCount() == 1 && table.getItem(4 * 6 + 5).getCount() == 1, "it took one of each item");
		helper.assertTrue(table.result().is(ladder), "what is left still makes ladders");
		table.craft(player);
		helper.assertTrue(table.isEmpty() && table.result().isEmpty(), "the second takes the rest");
		// upright (a column) is not the recipe
		for (int y = 0; y < 3; y++) {
			table.setItem(y * 6, new ItemStack(stick));
		}
		table.setItem(3 * 6, new ItemStack(log));
		helper.assertTrue(table.result().isEmpty(), "turned upright it is not the recipe");
		// the real recipes (tools/advanced_crafting.py): the T1 Courier Quad laid out on the grid makes the drone
		for (int i = 0; i < 36; i++) {
			table.setItem(i, ItemStack.EMPTY);
		}
		String[] t1 = {"R....R", ".B..B.", "..FC..", "..PP..", ".B..B.", "R....R"};
		java.util.Map<Character, String> parts = java.util.Map.of('R', "rotor_assembly", 'B', "bronze_plate", 'F', "flight_controller",
				'C', "basic_circuit", 'P', "lead_acid_pack");
		for (int y = 0; y < 6; y++) {
			for (int x = 0; x < 6; x++) {
				char c = t1[y].charAt(x);
				if (c != '.') {
					table.setItem(y * 6 + x, new ItemStack(net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(
							io.github.jimbozoomer.jugcraft.Jugcraft.id(parts.get(c)))));
				}
			}
		}
		helper.assertTrue(table.result().is(net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(
				io.github.jimbozoomer.jugcraft.Jugcraft.id("drone_t1"))), "the T1 drone's 6 x 6 recipe makes it: " + table.result());
		for (int i = 0; i < 36; i++) {
			table.setItem(i, ItemStack.EMPTY);
		}
		// the tower's blocks are made here too (28 October 2026, tools/advanced_crafting.py convert): Reinforced Concrete's
		// 3 x 3 (concrete round a steel ingot, 8) is now its shape doubled across (16), and the crafting table doesn't make it
		var item = (java.util.function.Function<String, net.minecraft.world.item.Item>) id ->
				net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.parse(id));
		var concrete = item.apply("minecraft:gray_concrete");
		var steel = item.apply("jugcraft:steel_ingot");
		String[] wall = {"CCCCCC", "CCSSCC", "CCCCCC"};
		for (int y = 0; y < 3; y++) {
			for (int x = 0; x < 6; x++) {
				table.setItem((y + 1) * 6 + x, new ItemStack(wall[y].charAt(x) == 'S' ? steel : concrete));
			}
		}
		helper.assertTrue(table.result().is(item.apply("jugcraft:reinforced_concrete")) && table.result().getCount() == 16,
				"the doubled Reinforced Concrete makes 16: " + table.result());
		var crafting = net.minecraft.world.item.crafting.RecipeType.CRAFTING;
		var old = net.minecraft.world.item.crafting.CraftingInput.of(3, 3, List.of(new ItemStack(concrete), new ItemStack(concrete),
				new ItemStack(concrete), new ItemStack(concrete), new ItemStack(steel), new ItemStack(concrete), new ItemStack(concrete),
				new ItemStack(concrete), new ItemStack(concrete)));
		helper.assertTrue(level.recipeAccess().getRecipeFor(crafting, old, level).isEmpty(), "the crafting table no longer makes Reinforced Concrete");
		// the Advanced Workbench itself is still made on a crafting table (its own 3 x 3, unique)
		var plate = item.apply("jugcraft:steel_plate");
		var bench = net.minecraft.world.item.crafting.CraftingInput.of(3, 3, List.of(new ItemStack(plate), new ItemStack(item.apply("minecraft:glass_pane")),
				new ItemStack(plate), new ItemStack(item.apply("jugcraft:basic_circuit")), new ItemStack(item.apply("minecraft:crafting_table")),
				new ItemStack(item.apply("jugcraft:basic_circuit")), new ItemStack(plate), new ItemStack(item.apply("minecraft:redstone")), new ItemStack(plate)));
		var made2 = level.recipeAccess().getRecipeFor(crafting, bench, level).map(r -> r.value().assemble(bench)).orElse(ItemStack.EMPTY);
		helper.assertTrue(made2.is(block.asItem()), "the crafting table makes the Advanced Workbench: " + made2);
		for (int i = 0; i < 36; i++) {
			table.setItem(i, ItemStack.EMPTY);
		}
		for (int y = 0; y < 3; y++) {
			table.setItem(y * 6, new ItemStack(stick));
		}
		table.setItem(3 * 6, new ItemStack(log));
		// broken: the grid's items drop
		level.destroyBlock(cells.get(3), false);
		helper.assertTrue(level.getBlockState(anchor).isAir(), "the table comes down whole");
		var drops = level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class, new net.minecraft.world.phys.AABB(anchor).inflate(3));
		int sticks = drops.stream().filter(e -> e.getItem().is(stick)).mapToInt(e -> e.getItem().getCount()).sum();
		helper.assertTrue(sticks == 3, "its grid's items drop: " + sticks + " sticks");
		helper.succeed();
	}

	/**
	 * Work list item 20 (take 1 approved 26 October 2026, layout 37; the cargo area reworked in layout 40, take 2 approved
	 * 27 October; tools/tripillar_stations.py): the stations are part of the building. Read from the foundation's layout:
	 * both exchanges' masters where ExchangeStations binds them, facing east, their ports in the intake face with open air
	 * outside (the apron), a dark surround round them (no panel's pale edge beside their models); the intake room's door in
	 * the plane of its glass; the goods lift's landing door whole facing the cargo hall, no door at the top, its rails; the
	 * cargo hall: the Case Packer whole with its back to the room's wall (solid there), its infeed from the cargo exchange
	 * through that wall (layout 41), the pack spot, the drones' lane, the
	 * DISPATCH and SUPPLY rows, none of the old terminal, packager and crate pallets left in sight; the terminal and packager
	 * under the floor, the layout's; on the lift house's top the 3 x 3 platform with its pickup formed, the layout's
	 * "platform", a light on each corner, no platform left on the roof slab; nothing left where the yard was.
	 */
	@GameTest
	public void stationsStandInsideTheBuilding(GameTestHelper helper) {
		if (TestFocus.skip("tower")) {
			helper.succeed();
			return;
		}
		TowerData data = TowerData.get();
		java.util.Map<BlockPos, BlockState> placed = new java.util.HashMap<>();
		for (int[] b : data.foundation.place) {
			placed.put(new BlockPos(b[0], b[1], b[2]), data.state(b[3]));
		}
		java.util.function.BiPredicate<BlockPos, String> is = (p, id) -> placed.containsKey(p)
				&& net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(placed.get(p).getBlock()).getPath().equals(id);
		for (ExchangeStationBlock station : new ExchangeStationBlock[] {ExchangeStations.ENERGY, ExchangeStations.CARGO}) {
			var offset = ExchangeStations.masterOffset(station, TowerData.CURRENT_LAYOUT);
			BlockPos master = new BlockPos(offset.getX(), offset.getY(), offset.getZ());
			BlockState state = placed.get(master);
			helper.assertTrue(state != null && state.is(station) && state.getValue(ExchangeStationBlock.PART) == 0
					&& state.getValue(ExchangeStationBlock.FACING) == Direction.EAST && ExchangeStations.facing(station, TowerData.CURRENT_LAYOUT) == Direction.EAST,
					"the exchange's master stands where it binds, facing out of the intake face: " + state);
			BlockPos port = station.footprint(state).partPos(master, Direction.EAST, station.portPart(state));
			helper.assertTrue(port.getX() == 45 && !placed.containsKey(port.east()) && !placed.containsKey(port.east(2)),
					"its port is in the face (x 45) with the apron's open air outside: " + port);
		}
		helper.assertTrue(is.test(new BlockPos(45, 1, 0), "maintenance_door") && is.test(new BlockPos(46, 6, -5), "energy_port_plate")
				&& is.test(new BlockPos(46, 6, 5), "cargo_port_plate"), "the door between the stations and a plate over each port");
		for (int y = 3; y <= 9; y++) {
			helper.assertTrue(is.test(new BlockPos(45, y, 0), "carbon_composite_panel"), "the dark surround over the door between the stations at y " + y);
		}
		for (int z : new int[] {-9, -1, 1, 9}) {
			helper.assertTrue(is.test(new BlockPos(45, 6, z), "carbon_composite_panel"), "the dark surround over the stations at z " + z);
		}
		BlockState roomDoor = placed.get(new BlockPos(37, 1, -8));
		helper.assertTrue(roomDoor != null && is.test(new BlockPos(37, 1, -8), "steel_door") && is.test(new BlockPos(37, 1, -7), "framed_glass")
				&& roomDoor.toString().contains("facing=west") && placed.get(new BlockPos(37, 1, -7)).toString().contains("facing=west"),
				"the intake room's door faces as its glass does (in the glass's plane): " + roomDoor);
		BlockState anchor = placed.get(TriPillarArena.LIFT_GROUND);
		helper.assertTrue(anchor != null && anchor.getBlock() instanceof io.github.jimbozoomer.jugcraft.tower.GoodsLiftDoorBlock door
				&& door.isAnchor(anchor) && door.facingOf(anchor) == Direction.WEST, "the goods lift's landing door: " + anchor);
		var liftDoor = (io.github.jimbozoomer.jugcraft.tower.GoodsLiftDoorBlock) anchor.getBlock();
		java.util.Set<BlockPos> ground = new java.util.HashSet<>();
		for (BlockPos cell : liftDoor.cells(TriPillarArena.LIFT_GROUND, anchor)) {
			helper.assertTrue(placed.containsKey(cell) && placed.get(cell).is(liftDoor), "the door stands whole: " + cell);
			ground.add(cell.immutable());
		}
		// the ground door is 3 wide and 4 high (y 1-4): any other cell of a landing door would be a landing above
		for (java.util.Map.Entry<BlockPos, BlockState> e : placed.entrySet()) {
			helper.assertTrue(ground.contains(e.getKey()) || !e.getValue().is(liftDoor), "no landing door but the ground one: " + e.getKey());
			String id = net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(e.getValue().getBlock()).getPath();
			helper.assertTrue(!id.equals("crate_pallet"), "no crate pallets in the hall: " + e.getKey());
			helper.assertTrue(e.getKey().getY() < 0 || !id.equals("drone_depot_terminal") && !id.equals("cargo_packager"),
					"no terminal or packager in sight: " + e.getKey());
		}
		for (int y = 0; y <= 24; y++) {
			for (BlockPos p : new BlockPos[] {new BlockPos(39, y, -12), new BlockPos(39, y, -14)}) {
				helper.assertTrue(is.test(p, "lift_rail"), "the car's rails on the shaft's back wall: " + p + " " + placed.get(p));
			}
		}
		helper.assertTrue(is.test(TriPillarArena.TERMINAL, "drone_depot_terminal") && is.test(TriPillarArena.PACKAGER, "cargo_packager")
				&& java.util.Arrays.equals(data.terminal, new int[] {31, -1, -8}) && java.util.Arrays.equals(data.packager, new int[] {31, -1, -2})
				&& placed.containsKey(TriPillarArena.TERMINAL.above()) && placed.containsKey(TriPillarArena.PACKAGER.above()),
				"the terminal and the packager under the hall's floor, the layout's");
		// the cargo hall
		BlockState packer = placed.get(TriPillarArena.CASE_PACKER);
		helper.assertTrue(packer != null && packer.getBlock() instanceof io.github.jimbozoomer.jugcraft.tower.RoomKitBlock kit && kit.isAnchor(packer)
				&& packer.getValue(io.github.jimbozoomer.jugcraft.tower.RoomKitBlock.FACING_PROP) == Direction.WEST
				&& net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(packer.getBlock()).getPath().equals("cargo_case_packer"),
				"the Case Packer faces the hall: " + packer);
		var packerKit = (io.github.jimbozoomer.jugcraft.tower.RoomKitBlock) packer.getBlock();
		for (BlockPos cell : packerKit.cells(TriPillarArena.CASE_PACKER, packer)) {
			helper.assertTrue(placed.containsKey(cell) && placed.get(cell).is(packerKit), "the packer stands whole: " + cell);
		}
		for (int z = 1; z <= 5; z++) {
			helper.assertTrue(is.test(new BlockPos(37, 1, z), "wall_panel") && is.test(new BlockPos(37, 6, z), "wall_panel"),
					"the intake room's wall solid behind the packer at z " + z);
		}
		helper.assertTrue(is.test(TriPillarArena.PACK_SPOT, "warehouse_pack_spot"), "the pack spot");
		BlockState infeed = placed.get(TriPillarArena.PACKER_INFEED);
		helper.assertTrue(infeed != null && infeed.getBlock() instanceof io.github.jimbozoomer.jugcraft.tower.RoomKitBlock feed && feed.isAnchor(infeed)
				&& infeed.getValue(io.github.jimbozoomer.jugcraft.tower.RoomKitBlock.FACING_PROP) == Direction.WEST
				&& net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(infeed.getBlock()).getPath().equals("cargo_infeed"),
				"the packer's infeed conveyor over its hopper: " + infeed);
		var feedKit = (io.github.jimbozoomer.jugcraft.tower.RoomKitBlock) infeed.getBlock();
		java.util.List<BlockPos> feedCells = feedKit.cells(TriPillarArena.PACKER_INFEED, infeed);
		for (BlockPos cell : feedCells) {
			helper.assertTrue(placed.containsKey(cell) && placed.get(cell).is(feedKit), "the infeed stands whole: " + cell);
		}
		helper.assertTrue(feedCells.contains(new BlockPos(37, 4, 3)) && feedCells.contains(new BlockPos(39, 4, 3)),
				"it runs through the intake room's wall (x 37) to the cargo exchange's back (x 40): " + feedCells);
		for (int z = -14; z <= 3; z++) {
			helper.assertTrue(is.test(new BlockPos(33, 0, z), "warehouse_lane"), "the drones' lane at z " + z);
		}
		// on the lift house's top: the pickup over the car, its platform the layout's
		BlockPos seed = new BlockPos(data.platform[0], data.platform[1], data.platform[2]);
		helper.assertTrue(seed.equals(TriPillarArena.ROOF_PICKUP.below()) && is.test(seed, "landing_platform"),
				"the layout's platform is the landing platform under the pickup's middle: " + placed.get(seed));
		for (int dx = 0; dx < 3; dx++) {
			for (int dz = 0; dz < 3; dz++) {
				BlockPos plate = TriPillarArena.ROOF_PICKUP.offset(dx - 1, 0, dz - 1);
				helper.assertTrue(is.test(plate.below(), "landing_platform") && is.test(plate, "supply_pickup")
						&& placed.get(plate).getValue(io.github.jimbozoomer.jugcraft.drone.SupplyPickupBlock.PART) == io.github.jimbozoomer.jugcraft.drone.SupplyPickupBlock.part(dx, dz),
						"the pickup formed on its platform: " + plate + " " + placed.get(plate));
			}
		}
		for (BlockPos corner : new BlockPos[] {new BlockPos(36, 26, -15), new BlockPos(40, 26, -15), new BlockPos(36, 26, -11), new BlockPos(40, 26, -11)}) {
			helper.assertTrue(is.test(corner, "warning_light") && is.test(corner.below(), "steel_armor_plate"), "a light on the house's corner " + corner);
		}
		for (int x = 33; x <= 41; x++) {
			for (int z = -10; z <= -2; z++) {
				helper.assertTrue(!is.test(new BlockPos(x, 20, z), "landing_platform") && !is.test(new BlockPos(x, 21, z), "supply_pickup"),
						"no platform left on the roof slab: " + x + ", " + z);
			}
		}
		for (BlockPos p : placed.keySet()) {
			helper.assertTrue(p.getX() <= 52, "nothing stands east of the building where the yard was: " + p + " " + placed.get(p));
		}
		helper.succeed();
	}

	/**
	 * The roof plant and walkways (take 1 approved 26 October 2026, layout 36; tools/roof_plant_layout.py): tier 1 puts
	 * the plant on the control building's roof with the pads. Read from the layout: two rooftop air handlers and two dry
	 * coolers (each once, at its anchor), six exhaust fans, the crossover over the coolers' pipes, the comms mast from its
	 * base to its top on the stair house's west end, the RF cabinet; the painted walkway from the stair house's door.
	 */
	@GameTest
	public void roofPlantStandsOnTheRoof(GameTestHelper helper) {
		if (TestFocus.skip("tower")) {
			helper.succeed();
			return;
		}
		TowerData data = TowerData.get();
		java.util.Map<BlockPos, BlockState> placed = new java.util.HashMap<>();
		for (int[] b : data.tier(1).place) {
			placed.put(new BlockPos(b[0], b[1], b[2]), data.state(b[3]));
		}
		java.util.Map<String, Integer> anchors = new java.util.HashMap<>();
		for (BlockState s : placed.values()) {
			if (s.getBlock() instanceof io.github.jimbozoomer.jugcraft.tower.RoomKitBlock kit && kit.isAnchor(s)) {
				anchors.merge(net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(s.getBlock()).getPath(), 1, Integer::sum);
			}
		}
		helper.assertTrue(anchors.getOrDefault("rooftop_ahu", 0) == 2, "two rooftop air handlers: " + anchors);
		helper.assertTrue(anchors.getOrDefault("dry_cooler", 0) == 2, "two dry coolers: " + anchors);
		int fans = anchors.getOrDefault("roof_exhaust_fan", 0) + anchors.getOrDefault("roof_upblast_fan", 0)
				+ anchors.getOrDefault("kitchen_exhaust_fan", 0);
		helper.assertTrue(fans == 6, "six roof exhaust fans: " + anchors);
		helper.assertTrue(anchors.getOrDefault("pipe_crossover", 0) == 1 && anchors.getOrDefault("rf_cabinet", 0) == 1,
				"the crossover and the RF cabinet: " + anchors);
		java.util.function.BiPredicate<BlockPos, String> is = (p, id) -> placed.containsKey(p)
				&& net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(placed.get(p).getBlock()).getPath().equals(id);
		helper.assertTrue(is.test(new BlockPos(-18, 21, -30), "comms_mast_base") && is.test(new BlockPos(-18, 30, -30), "comms_mast_top"),
				"the comms mast from its base to its top at the stair house's west end");
		for (int y = 22; y < 30; y++) {
			BlockPos p = new BlockPos(-18, y, -30);
			helper.assertTrue(is.test(p, y == 22 || y == 24 ? "comms_mast_tie" : "comms_mast"), "the mast's section at y " + y + ": " + placed.get(p));
		}
		var walkway = io.github.jimbozoomer.jugcraft.warehouse.JugcraftWarehouse.WALKWAY;
		for (BlockPos p : new BlockPos[] {new BlockPos(-9, 20, -30), new BlockPos(-9, 20, -29)}) {
			helper.assertTrue(placed.containsKey(p) && placed.get(p).is(walkway), "the walkway out of the stair house's door at " + p + ": " + placed.get(p));
		}
		helper.succeed();
	}

	/**
	 * Work list item 21's last part (take 1 approved 26 October 2026, layout 35; tools/pillar_rooms.py): every room behind
	 * a pillar's maintenance doors off the roof and the tie decks is a drone service floor. Read from the layout as the
	 * tower stands at each tier: the walkway starts at the doors, a LiPo charging cabinet stands at a work station in the
	 * room (on the doors' inner side), and an extinguisher hangs on the wall by the doors.
	 */
	@GameTest
	public void pillarRoomsAreServiceFloors(GameTestHelper helper) {
		if (TestFocus.skip("tower")) {
			helper.succeed();
			return;
		}
		TowerData data = TowerData.get();
		java.util.Map<BlockPos, BlockState> placed = new java.util.HashMap<>();
		for (int[] b : data.foundation.place) {
			placed.put(new BlockPos(b[0], b[1], b[2]), data.state(b[3]));
		}
		var walkway = io.github.jimbozoomer.jugcraft.warehouse.JugcraftWarehouse.WALKWAY;
		int rooms = 0;
		for (int t = 1; t <= 9; t++) {
			for (int[] c : data.tier(t).clear) {
				placed.remove(new BlockPos(c[0], c[1], c[2]));
			}
			for (int[] b : data.tier(t).place) {
				placed.put(new BlockPos(b[0], b[1], b[2]), data.state(b[3]));
			}
			for (int[] d : TriPillarArena.MAINTENANCE_DOORS) {
				if (d[0] != t || t < 2) {
					continue;
				}
				rooms++;
				int ix = -d[5], iz = -d[6], y = d[7];
				String at = "tier " + t + "'s room behind the doors at " + d[1] + ", " + d[2];
				BlockState in = placed.get(new BlockPos(d[1] + ix, y, d[2] + iz)), in2 = placed.get(new BlockPos(d[3] + ix, y, d[4] + iz));
				helper.assertTrue(in != null && in.is(walkway) || in2 != null && in2.is(walkway), at + ": the walkway starts at the doors: " + in + " / " + in2);
				boolean lipo = false, ext = false;
				for (var e : placed.entrySet()) {
					BlockPos p = e.getKey();
					int along = (p.getX() - d[1]) * ix + (p.getZ() - d[2]) * iz;
					if (along < 1 || Math.abs(p.getX() - d[1]) > 20 || Math.abs(p.getZ() - d[2]) > 20) {
						continue;
					}
					String id = net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(e.getValue().getBlock()).getPath();
					lipo |= p.getY() == y + 1 && id.equals("lipo_cabinet");
					ext |= p.getY() == y + 2 && along <= 2 && Math.abs(p.getX() - d[1]) + Math.abs(p.getZ() - d[2]) <= 4 && id.equals("fire_extinguisher");
				}
				helper.assertTrue(lipo, at + ": a work station with its LiPo charging cabinet");
				helper.assertTrue(ext, at + ": an extinguisher on the wall by the doors");
			}
		}
		helper.assertTrue(rooms == 24, "the 24 rooms off the roof and the tie decks: " + rooms);
		helper.succeed();
	}

	/**
	 * Work list item 21 (take 1 approved 25 October 2026, layout 34): on every deck from tier 2's up, the lift core's door
	 * is one Glass Door in the middle of its south face (x 0, facing in: north) with framed glass either side, and every
	 * cell of the core's ring at the deck is solid (its glass on a steel sill: no slot round its foot, none under the
	 * door). Read from the layout: the tier that builds each deck's part of the core places them.
	 */
	@GameTest
	public void liftCoreDoorIsCentredOnEveryDeck(GameTestHelper helper) {
		if (TestFocus.skip("tower")) {
			helper.succeed();
			return;
		}
		TowerData data = TowerData.get();
		int[] lines = {0, 20, 36, 55, 76, 100, 130, 162, 198, 238};    // tools/tripillar_geometry.py TB: the tier lines
		var glassDoor = io.github.jimbozoomer.jugcraft.nuclear.PlantInterior.BLOCKS.get("glass_door");
		var framed = io.github.jimbozoomer.jugcraft.test.TowerGameTests.modBlock("framed_glass");
		// The tower as it stands at each tier: the foundation's and each tier's blocks, less what a tier clears.
		java.util.Map<BlockPos, BlockState> placed = new java.util.HashMap<>();
		for (int[] b : data.foundation.place) {
			placed.put(new BlockPos(b[0], b[1], b[2]), data.state(b[3]));
		}
		for (int t = 1; t <= 9; t++) {
			for (int[] c : data.tier(t).clear) {
				placed.remove(new BlockPos(c[0], c[1], c[2]));
			}
			for (int[] b : data.tier(t).place) {
				placed.put(new BlockPos(b[0], b[1], b[2]), data.state(b[3]));
			}
			if (t < 3) {
				continue;
			}
			int[] decks = t == 9 ? new int[] {lines[8], lines[9]} : new int[] {lines[t - 1]};
			for (int y : decks) {
				BlockState lower = placed.get(new BlockPos(0, y + 1, 4)), upper = placed.get(new BlockPos(0, y + 2, 4));
				helper.assertTrue(lower != null && lower.is(glassDoor) && upper != null && upper.is(glassDoor)
						&& lower.getValue(net.minecraft.world.level.block.DoorBlock.FACING) == Direction.NORTH
						&& lower.getValue(net.minecraft.world.level.block.DoorBlock.HALF) == net.minecraft.world.level.block.state.properties.DoubleBlockHalf.LOWER,
						"tier " + t + ": the lift core's glass door in the middle of its face at y " + (y + 1) + ": " + lower + " / " + upper);
				for (int x : new int[] {-1, 1}) {
					BlockState side = placed.get(new BlockPos(x, y + 1, 4));
					helper.assertTrue(side != null && side.is(framed), "tier " + t + ": framed glass beside the door at x " + x + ": " + side);
				}
				for (int x = -4; x <= 4; x++) {
					for (int z = -4; z <= 4; z++) {
						boolean in = Math.abs(x) + Math.abs(z) <= 6;
						boolean edge = in && (Math.abs(x) == 4 || Math.abs(z) == 4 || Math.abs(x) + Math.abs(z) == 6 || Math.abs(x) + Math.abs(z) == 5
								&& (Math.abs(x + 1) + Math.abs(z) > 6 || Math.abs(x - 1) + Math.abs(z) > 6 || Math.abs(x) + Math.abs(z + 1) > 6
										|| Math.abs(x) + Math.abs(z - 1) > 6));
						if (edge) {
							BlockState sill = placed.get(new BlockPos(x, y, z));
							helper.assertTrue(sill != null && sill.isSolidRender(),
									"tier " + t + ": the core's ring is solid at the deck, at " + x + ", " + y + ", " + z + ": " + sill);
						}
					}
				}
			}
		}
		helper.succeed();
	}

	/**
	 * The Tri-Pillar data: nine tiers on a foundation, real blocks only, capacities that rise with the docks (24 on the
	 * roof, 128 in all), tiles that cover tiers 2-9 exactly once, no ground pads, four tower pickups, and every tier
	 * dearer than the one before.
	 */
	@GameTest
	public void towerDataIsConsistent(GameTestHelper helper) {
		if (TestFocus.skip("tower")) {
			helper.succeed();
			return;
		}
		TowerData data = TowerData.get();
		helper.assertTrue(TowerData.CURRENT_LAYOUT == TowerData.TRIPILLAR_FLAT_TVS_LAYOUT && data == TowerData.get(TowerData.CURRENT_LAYOUT),
				"the current design is the Tri-Pillar");
		helper.assertTrue(data.tiers.size() == TowerData.TIERS, "9 tiers, got " + data.tiers.size());
		for (BlockState state : data.palette) {
			helper.assertTrue(!state.isAir(), "every tower block is a real block (an unknown ID reads as air)");
		}
		TowerData.Tier foundation = data.foundation;
		helper.assertTrue(foundation != null, "the Tri-Pillar has a foundation step");
		var foundationCost = data.cost(foundation);
		// paid for in the twelve main blocks only (TowerCosts.CHARGED, 26 October 2026); the holograms, the tower's own, free
		// (tower/Holograms, 28 October 2026)
		helper.assertTrue(foundationCost.getOrDefault(io.github.jimbozoomer.jugcraft.test.TowerGameTests.modBlock("reinforced_concrete").asItem(), 0) > 1000
				&& foundationCost.getOrDefault(JugcraftDrones.HOLO_TABLE.asItem(), 0) == 0
				&& foundationCost.getOrDefault(JugcraftTower.HOLO_COMMAND_TABLE.asItem(), 0) == 0
				&& foundationCost.getOrDefault(JugcraftDrones.TERMINAL.asItem(), 0) == 0,
				"the foundation costs its main blocks (thousands of concrete); its holograms and fittings come free (the holo "
						+ "tables, the terminal): " + foundationCost);
		for (var cost : foundationCost.entrySet()) {
			helper.assertTrue(cost.getKey() != net.minecraft.world.item.Items.AIR && cost.getValue() > 0
					&& io.github.jimbozoomer.jugcraft.tower.TowerCosts.charged(cost.getKey()), "the foundation costs charged items only: " + cost);
		}
		helper.assertTrue(foundationCost.size() <= io.github.jimbozoomer.jugcraft.tower.TowerCosts.CHARGED.size()
				&& foundationCost.getOrDefault(io.github.jimbozoomer.jugcraft.tower.ExchangeStations.ENERGY.asItem(), 0) == 0,
				"at most the twelve charged kinds; the exchange stations come free");
		helper.assertTrue(foundation.place.length > 0 && foundation.air.length > 0 && foundation.docks.isEmpty() && foundation.capacity == 0,
				"the foundation is the control building: blocks and rooms, no docks");
		int[] t = data.terminal;
		helper.assertTrue(Arrays.stream(foundation.place).anyMatch(b -> b[0] == t[0] && b[1] == t[1] && b[2] == t[2]
				&& data.state(b[3]).is(JugcraftDrones.TERMINAL)), "the foundation places the depot terminal where the core looks for it");
		int last = 0;
		int lastCost = 0;
		int lastBlocks = 0;
		int pickups = 0;
		for (TowerData.Tier tier : data.tiers) {
			helper.assertTrue(tier.capacity > last, "tier " + tier.number + " holds more drones than the one before");
			helper.assertTrue(last + tier.docks.size() == tier.capacity, "tier " + tier.number + ": " + tier.docks.size() + " new docks for "
					+ (tier.capacity - last) + " more drones");
			for (TowerData.Dock dock : tier.docks) {
				helper.assertTrue(dock.tier() == tier.number, "tier " + tier.number + "'s docks are for its own drones");
			}
			int cost = io.github.jimbozoomer.jugcraft.tower.TowerCosts.total(data.cost(tier));
			int blocks = 0;
			for (int[] b : tier.place) {
				BlockState placed = data.state(b[3]);
				if (io.github.jimbozoomer.jugcraft.tower.TowerCosts.charged(io.github.jimbozoomer.jugcraft.tower.TowerCosts.item(placed))) {
					blocks += io.github.jimbozoomer.jugcraft.tower.TowerCosts.count(placed);
				}
			}
			helper.assertTrue(cost == blocks, "tier " + tier.number + " costs exactly the charged items its blocks are placed from");
			if (tier.number == 1) {
				helper.assertTrue(tier.capacity == 24, "tier 1 holds 24 drones, got " + tier.capacity);
				for (TowerData.Dock dock : tier.docks) {
					helper.assertTrue(dock.size() == DroneSize.SMALL && dock.y() == TriPillarArena.ROOF_Y + 1,
							"the tier 1 docks are small pads on the roof, got " + dock);
				}
			} else {
				Set<Integer> covered = new HashSet<>();
				for (TowerData.Tile tile : tier.tiles) {
					helper.assertTrue(tile.blocks().length <= TowerData.TILE * TowerData.TILE, "a tile holds at most one 4x4 layer");
					for (int b : tile.blocks()) {
						helper.assertTrue(covered.add(b), "every block is in exactly one tile");
					}
				}
				helper.assertTrue(covered.size() == tier.place.length, "tiles cover all " + tier.place.length + " blocks of tier " + tier.number);
				// Tier cost is the blocks a tier uses: more of them every tier.
				helper.assertTrue(tier.place.length > lastBlocks, "tier " + tier.number + " uses more blocks than tier " + (tier.number - 1)
						+ ": " + tier.place.length + " after " + lastBlocks);
				helper.assertTrue(cost > lastCost, "tier " + tier.number + " costs more than tier " + (tier.number - 1)
						+ ": " + cost + " after " + lastCost);
			}
			for (TowerData.Pickup pickup : tier.pickups) {
				helper.assertTrue(pickup.tier() == tier.number, "a pickup belongs to its own tier");
				pickups++;
			}
			last = tier.capacity;
			lastCost = cost;
			lastBlocks = tier.place.length;
		}
		helper.assertTrue(last == 128 && data.capacity(TowerData.TIERS) == 128, "a finished tower holds 128 drones, got " + last);
		helper.assertTrue(data.pads.isEmpty(), "no ground pads: tier-1 drones dock on the roof");
		helper.assertTrue(pickups == 4, "four tower pickups (tiers 2, 4, 6 and 8), got " + pickups);
		helper.succeed();
	}

	/** A creative player gets the Creative Quick Start once; a survival player does not; the Field Manual has its pages. */
	@GameTest
	public void guideBooks(GameTestHelper helper) {
		if (TestFocus.skip("tower")) {
			helper.succeed();
			return;
		}
		net.minecraft.server.level.ServerPlayer player = helper.makeMockServerPlayerInLevel();
		// Joining may already have handed the book over (the test world is creative): start from a clean slate.
		player.removeTag(io.github.jimbozoomer.jugcraft.drone.GuideBooks.GIVEN_TAG);
		player.getInventory().clearContent();
		helper.assertFalse(io.github.jimbozoomer.jugcraft.drone.GuideBooks.giveCreativeGuide(player, GameType.SURVIVAL), "no creative book in survival");
		helper.assertTrue(io.github.jimbozoomer.jugcraft.drone.GuideBooks.giveCreativeGuide(player, GameType.CREATIVE), "a creative player gets the book");
		helper.assertTrue(player.getInventory().contains(new ItemStack(io.github.jimbozoomer.jugcraft.drone.GuideBooks.CREATIVE_GUIDE)),
				"the book is in the inventory");
		helper.assertFalse(io.github.jimbozoomer.jugcraft.drone.GuideBooks.giveCreativeGuide(player, GameType.CREATIVE), "only once");
		helper.assertTrue(io.github.jimbozoomer.jugcraft.drone.GuideBooks.MANUAL instanceof io.github.jimbozoomer.jugcraft.drone.GuideBooks.Book book
				&& book.bookId().equals("drone_tower_manual"), "the Field Manual is a guide book that opens its own pages");
		helper.assertTrue(helper.getLevel().getServer().getRecipeManager().getRecipes().stream()
				.anyMatch(holder -> holder.id().identifier().equals(io.github.jimbozoomer.jugcraft.Jugcraft.id("drone_tower_manual"))),
				"the Field Manual recipe (a book and a tier 1 drone) is loaded");
		helper.succeed();
	}

	/**
	 * Every dock of the finished tower, roof pads and hangars alike, stands on a formed hangar pad, and its drone has
	 * room there: a box as wide as the drone's size needs, two blocks high, with no block in it.
	 */
	@GameTest
	public void dronesFitTheirDocks(GameTestHelper helper) {
		if (TestFocus.skip("tower")) {
			helper.succeed();
			return;
		}
		TowerData data = TowerData.get();
		Map<BlockPos, BlockState> world = finishedTower(data);
		int docks = 0;
		for (TowerData.Tier tier : data.tiers) {
			for (TowerData.Dock dock : tier.docks) {
				docks++;
				int y = (int) Math.floor(dock.y());
				BlockPos under = new BlockPos((int) Math.floor(dock.x()), y - 1, (int) Math.floor(dock.z()));
				BlockState pad = world.get(under);
				helper.assertTrue(pad != null && pad.getBlock() instanceof HangarPadBlock && pad.getValue(HangarPadBlock.PART) > 0,
						"the tier " + dock.tier() + " dock at " + dock.x() + " " + dock.y() + " " + dock.z() + " stands on a formed hangar pad, found " + pad);
				double r = halfWidth(dock.size());
				int x0 = (int) Math.floor(dock.x() - r + 1e-6), x1 = (int) Math.ceil(dock.x() + r - 1e-6) - 1;
				int z0 = (int) Math.floor(dock.z() - r + 1e-6), z1 = (int) Math.ceil(dock.z() + r - 1e-6) - 1;
				for (int x = x0; x <= x1; x++) {
					for (int z = z0; z <= z1; z++) {
						for (int dy = 0; dy < 2; dy++) {
							BlockState in = world.get(new BlockPos(x, y + dy, z));
							helper.assertTrue(in == null, "a " + dock.size() + " drone docked at " + dock.x() + " " + dock.y() + " " + dock.z()
									+ " (tier " + dock.tier() + ") has room, but " + in + " is at " + x + " " + (y + dy) + " " + z);
						}
					}
				}
			}
		}
		helper.assertTrue(docks == data.capacity(TowerData.TIERS), "every drone of the finished tower has a dock, got " + docks);
		helper.succeed();
	}

	/**
	 * Every hangar of the finished tower has a clear way out: nothing in its door opening or in front of it out
	 * to six blocks, nor in the column its drones climb from the exit point.
	 */
	@GameTest
	public void hangarEntrancesAreClear(GameTestHelper helper) {
		if (TestFocus.skip("tower")) {
			helper.succeed();
			return;
		}
		TowerData data = TowerData.get();
		Set<BlockPos> world = finishedTower(data).keySet();
		int doors = 0;
		for (TowerData.Tier tier : data.tiers) {
			for (TowerData.Dock dock : tier.docks) {
				int[] d = dock.door();
				if (d == null) {
					continue;
				}
				doors++;
				for (int x = d[0]; x <= d[2]; x++) {
					for (int z = d[1]; z <= d[3]; z++) {
						for (int y = d[4]; y < d[4] + d[5]; y++) {
							for (int step = 0; step <= 6; step++) {
								BlockPos pos = new BlockPos(x + d[6] * step, y, z + d[7] * step);
								helper.assertTrue(!world.contains(pos), "a block at " + pos + " stands in front of the tier "
										+ dock.tier() + " hangar door at " + d[0] + " " + d[4] + " " + d[1]);
							}
						}
					}
				}
				int ex = (int) Math.floor(dock.exitX());
				int ez = (int) Math.floor(dock.exitZ());
				for (int y = (int) dock.y(); y < 230; y++) {
					BlockPos pos = new BlockPos(ex, y, ez);
					helper.assertTrue(!world.contains(pos), "a block at " + pos + " is in the climb out of the tier " + dock.tier()
							+ " hangar at " + d[0] + " " + d[4] + " " + d[1]);
				}
			}
		}
		helper.assertTrue(doors > 90, "the finished tower has its hangars, got " + doors);
		helper.succeed();
	}

	/**
	 * The lobby's two Drone Display Cases (work list item 1) stand whole: each master facing north with its drone's tier,
	 * every other cell of its 5x5x4 a part of it.
	 */
	/**
	 * The lobby's Crystal Tree (work list item 2) stands whole in place of the four small planters: its master, every cell
	 * of its planter, bench and trunk a part of it; the trunk and the planter's wall collide, the crown's air does not.
	 */
	/**
	 * The lobby's lounge (work list item 3) is the one west of the lobby: the Long Coffee Table whole, its middle on the
	 * corner of its master cell, each of its cells colliding with its 0.40-high top; two rows of six sofas facing each
	 * other over it; the two old lounges' small glass tables gone.
	 */
	static void assertLounge(GameTestHelper helper, ServerLevel level, TowerCoreBlockEntity core) {
		BlockPos master = TriPillarArena.at(core, TriPillarArena.LONG_COFFEE_TABLE);
		BlockState table = level.getBlockState(master);
		helper.assertTrue(table.getBlock() instanceof io.github.jimbozoomer.jugcraft.tower.LongCoffeeTableBlock
				&& table.getValue(io.github.jimbozoomer.jugcraft.tower.LongCoffeeTableBlock.FACING) == net.minecraft.core.Direction.NORTH,
				"the Long Coffee Table at " + TriPillarArena.LONG_COFFEE_TABLE + ", facing north: " + table);
		var footprint = io.github.jimbozoomer.jugcraft.tower.LongCoffeeTableBlock.footprint(net.minecraft.core.Direction.NORTH);
		helper.assertTrue(footprint.size() == 8, "the table covers 4 x 2 cells");
		for (net.minecraft.core.Vec3i o : footprint) {
			if (o.equals(net.minecraft.core.Vec3i.ZERO)) continue;
			BlockState part = level.getBlockState(master.offset(o));
			helper.assertTrue(part.getBlock() instanceof io.github.jimbozoomer.jugcraft.tower.LongCoffeeTablePartBlock
					&& io.github.jimbozoomer.jugcraft.tower.LongCoffeeTablePartBlock.masterPos(master.offset(o), part).equals(master),
					"the table has its part " + o + ": " + part);
			var shape = part.getCollisionShape(level, master.offset(o));
			helper.assertTrue(!shape.isEmpty() && Math.abs(shape.max(net.minecraft.core.Direction.Axis.Y) - 0.40) < 1e-6,
					"the table's part " + o + " is its top, 0.40 high: " + shape);
		}
		// the table's middle is the master's north-west corner: its top reaches 0.55 either side of it
		var own = table.getCollisionShape(level, master);
		helper.assertTrue(Math.abs(own.max(net.minecraft.core.Direction.Axis.Z) - 0.55) < 1e-6 && own.min(net.minecraft.core.Direction.Axis.Z) == 0,
				"the master cell holds the table's south-east quarter's corner: " + own);
		for (int r = 0; r < 2; r++) {
			BlockPos first = TriPillarArena.at(core, TriPillarArena.SOFA_ROWS[r]);
			for (int i = 0; i < 6; i++) {
				BlockPos at = first.offset(r == 0 ? -i : i, 0, 0);
				BlockState sofa = level.getBlockState(at);
				helper.assertTrue(sofa.is(io.github.jimbozoomer.jugcraft.test.TowerGameTests.modBlock("sofa"))
						&& sofa.getValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.HORIZONTAL_FACING)
								== (r == 0 ? net.minecraft.core.Direction.SOUTH : net.minecraft.core.Direction.NORTH),
						"a sofa of the lounge's " + (r == 0 ? "north" : "south") + " row at " + at + ": " + sofa);
			}
		}
		// (the restroom has stood on the west one's spot since the ground floor rooms: gone is no glass table, not air)
		for (BlockPos old : TriPillarArena.OLD_LOUNGE_TABLES) {
			BlockState there = level.getBlockState(TriPillarArena.at(core, old));
			helper.assertTrue(!there.is(io.github.jimbozoomer.jugcraft.test.TowerGameTests.modBlock("glass_coffee_table")),
					"the old lounges' small tables are gone: " + old + " " + there);
		}
	}

	/**
	 * The service bay (work list item 8): the T4 on its cradle faces north, so it lies side-on to the robot arm two blocks
	 * east of it; the arm faces west at it, and its cell collides with its base, riser and turret, a block high.
	 */
	static void assertServiceBay(GameTestHelper helper, ServerLevel level, TowerCoreBlockEntity core) {
		BlockPos standAt = TriPillarArena.at(core, TriPillarArena.SERVICE_STAND);
		BlockState stand = level.getBlockState(standAt);
		helper.assertTrue(stand.is(io.github.jimbozoomer.jugcraft.test.TowerGameTests.modBlock("drone_display_stand"))
				&& stand.getValue(io.github.jimbozoomer.jugcraft.tower.HallFixtureBlock.FACING) == net.minecraft.core.Direction.NORTH
				&& stand.getValue(io.github.jimbozoomer.jugcraft.tower.HallFixtureBlock.Stand.STYLE) == io.github.jimbozoomer.jugcraft.tower.HallFixtureBlock.Style.SERVICE
				&& stand.getValue(io.github.jimbozoomer.jugcraft.tower.HallFixtureBlock.Stand.TIER) == 4,
				"the service bay's T4 stands on its cradle facing north, side-on to the arm: " + stand);
		BlockPos armAt = TriPillarArena.at(core, TriPillarArena.SERVICE_ARM);
		BlockState arm = level.getBlockState(armAt);
		helper.assertTrue(arm.is(io.github.jimbozoomer.jugcraft.test.TowerGameTests.modBlock("drone_service_arm"))
				&& arm.getValue(io.github.jimbozoomer.jugcraft.tower.HallFixtureBlock.FACING) == net.minecraft.core.Direction.WEST,
				"the service arm faces west at the cradle: " + arm);
		var shape = arm.getCollisionShape(level, armAt);
		helper.assertTrue(!shape.isEmpty() && Math.abs(shape.max(net.minecraft.core.Direction.Axis.Y) - 1.0) < 1e-6
				&& Math.abs(shape.min(net.minecraft.core.Direction.Axis.X) - 2 / 16.0) < 1e-6
				&& Math.abs(shape.max(net.minecraft.core.Direction.Axis.Z) - 14 / 16.0) < 1e-6,
				"the arm's base and turret collide, a block high: " + shape);
	}

	/**
	 * The data hall (work list item 13): two rows of eight of the plant's racks facing each other across the cold aisle
	 * (row A at z -2 facing north, B at z 1 facing south), the aisle's floor perforated, its roof panels over it and a
	 * containment door whole at each end, shut and colliding across the aisle; the UPS module, CRAC-1, the reader glass
	 * doors in the glass wall and the DATA HALL light box whole; and no glass-door rack left behind the operator areas.
	 */
	static void assertDataHall(GameTestHelper helper, ServerLevel level, TowerCoreBlockEntity core) {
		var tower = JugcraftTower.BLOCKS;
		for (int side : new int[] {-1, 1}) {
			for (int z = 8; z <= 13; z++) {
				BlockPos old = TriPillarArena.at(core, new BlockPos(side * TriPillarArena.OLD_RACK_X, 1, z));
				helper.assertTrue(!level.getBlockState(old).is(tower.get("equipment_rack")), "no glass-door rack left at " + old);
			}
		}
		String[][] kinds = {{"server_rack", "server_rack", "san_rack", "network_rack", "server_rack", "server_rack", "san_rack", "server_rack"},
				{"server_rack", "san_rack", "server_rack", "server_rack", "network_rack", "san_rack", "server_rack", "server_rack"}};
		int[] rows = {TriPillarArena.DATA_HALL_ROW_A, TriPillarArena.DATA_HALL_ROW_B};
		net.minecraft.core.Direction[] facings = {net.minecraft.core.Direction.NORTH, net.minecraft.core.Direction.SOUTH};
		for (int r = 0; r < 2; r++) {
			for (int i = 0; i < 8; i++) {
				BlockPos at = TriPillarArena.at(core, new BlockPos(TriPillarArena.DATA_HALL_RACK_X[i], 1, rows[r]));
				BlockState rack = level.getBlockState(at);
				helper.assertTrue(rack.is(io.github.jimbozoomer.jugcraft.nuclear.PlantInterior.BLOCKS.get(kinds[r][i]))
						&& rack.getValue(net.minecraft.world.level.block.HorizontalDirectionalBlock.FACING) == facings[r]
						&& level.getBlockState(at.above()).is(rack.getBlock()),
						"a " + kinds[r][i] + " (two high) facing " + facings[r] + " in row " + (char) ('A' + r) + " at " + at + ": " + rack);
				helper.assertTrue(level.getBlockEntity(at) instanceof io.github.jimbozoomer.jugcraft.nuclear.LivePanelBlockEntity,
						"the rack at " + at + " is live (its lights, its number)");
				BlockPos floor = at.offset(0, -1, r == 0 ? 1 : -1);
				helper.assertTrue(level.getBlockState(floor).is(io.github.jimbozoomer.jugcraft.nuclear.PlantInterior.BLOCKS.get("access_floor_perforated")),
						"a perforated tile in front of the rack at " + at);
				BlockPos roof = at.offset(0, 2, r == 0 ? 1 : -1);
				BlockState panel = level.getBlockState(roof);
				helper.assertTrue(panel.is(tower.get("aisle_containment_roof"))
						&& panel.getValue(net.minecraft.world.level.block.HorizontalDirectionalBlock.FACING) == facings[r],
						"a roof panel over the aisle at " + roof + ": " + panel);
			}
		}
		for (BlockPos door : new BlockPos[] {TriPillarArena.DATA_HALL_DOOR_EAST, TriPillarArena.DATA_HALL_DOOR_WEST}) {
			BlockPos master = TriPillarArena.at(core, door);
			BlockState state = level.getBlockState(master);
			var facing = door == TriPillarArena.DATA_HALL_DOOR_EAST ? net.minecraft.core.Direction.EAST : net.minecraft.core.Direction.WEST;
			helper.assertTrue(state.getBlock() instanceof io.github.jimbozoomer.jugcraft.tower.AisleContainmentDoorBlock
					&& state.getValue(io.github.jimbozoomer.jugcraft.tower.AisleContainmentDoorBlock.FACING) == facing
					&& !state.getValue(io.github.jimbozoomer.jugcraft.tower.AisleContainmentDoorBlock.OPEN),
					"an Aisle Containment Door facing " + facing + ", shut, at " + door + ": " + state);
			for (net.minecraft.core.Vec3i o : io.github.jimbozoomer.jugcraft.tower.AisleContainmentDoorBlock.footprint(facing)) {
				BlockPos at = master.offset(o);
				BlockState cell = level.getBlockState(at);
				if (!o.equals(net.minecraft.core.Vec3i.ZERO)) {
					helper.assertTrue(cell.getBlock() instanceof io.github.jimbozoomer.jugcraft.tower.AisleContainmentDoorPartBlock
							&& io.github.jimbozoomer.jugcraft.tower.AisleContainmentDoorPartBlock.masterPos(at, cell).equals(master),
							"the door has its part " + o + ": " + cell);
				}
				var shape = cell.getCollisionShape(level, at);
				helper.assertTrue(!shape.isEmpty() && shape.max(net.minecraft.core.Direction.Axis.Y) > 0.99,
						"the shut door's cell " + o + " closes the aisle: " + shape);
			}
		}
		BlockState ups = level.getBlockState(TriPillarArena.at(core, TriPillarArena.DATA_HALL_UPS));
		helper.assertTrue(ups.is(io.github.jimbozoomer.jugcraft.nuclear.PlantInterior.BLOCKS.get("ups_module")), "the UPS module, the room's hub: " + ups);
		BlockState crac = level.getBlockState(TriPillarArena.at(core, TriPillarArena.DATA_HALL_CRAC));
		helper.assertTrue(crac.is(io.github.jimbozoomer.jugcraft.nuclear.PlantInterior.BLOCKS.get("crac_unit")), "CRAC-1 against the north-west wall: " + crac);
		for (int dz = 0; dz < 2; dz++) {
			BlockState door = level.getBlockState(TriPillarArena.at(core, TriPillarArena.DATA_HALL_ENTRY.offset(0, 0, dz)));
			helper.assertTrue(door.is(io.github.jimbozoomer.jugcraft.nuclear.PlantInterior.BLOCKS.get("glass_door"))
					&& door.getValue(io.github.jimbozoomer.jugcraft.nuclear.PlantDoorBlock.READER) && door.getValue(io.github.jimbozoomer.jugcraft.nuclear.PlantDoorBlock.PAIRED),
					"a reader glass door, paired, in the data hall's glass wall: " + door);
		}
		for (int dz = -1; dz <= 2; dz++) {
			BlockState sign = level.getBlockState(TriPillarArena.at(core, TriPillarArena.DATA_HALL_SIGN.offset(0, 0, dz)));
			helper.assertTrue(sign.is(tower.get("data_hall_sign")), "the DATA HALL light box over the doors, whole: " + sign);
		}
	}

	/**
	 * The desks (work list item 14): each row of three one bench desk, its desks joined to each other and to nothing else
	 * (their joins as the desks beside them make them), its monitors the role's first, middle and last pages from the
	 * operator's left, the row working its role; fifteen rows, every role but the last (MAP TABLE, for desks players set)
	 * worked by one, the tower's build by two (the west operators' and the small table's, by the tower's model); every
	 * desk pinned to the tower's terminal.
	 */
	static void assertDeskRows(GameTestHelper helper, ServerLevel level, TowerCoreBlockEntity core) {
		java.util.Set<Integer> roles = new java.util.HashSet<>();
		for (int[] row : TriPillarArena.DESK_ROWS) {
			for (int k = 0; k < 3; k++) {
				BlockPos at = TriPillarArena.at(core, new BlockPos(row[0] + k * row[2], 1, row[1] + k * row[3]));
				BlockState desk = level.getBlockState(at);
				helper.assertTrue(desk.getBlock() instanceof io.github.jimbozoomer.jugcraft.tower.ConsoleDeskBlock, "a desk at " + at + ": " + desk);
				helper.assertTrue(desk.getValue(io.github.jimbozoomer.jugcraft.tower.ConsoleDeskBlock.LEFT) == (k > 0)
						&& desk.getValue(io.github.jimbozoomer.jugcraft.tower.ConsoleDeskBlock.RIGHT) == (k < 2)
						&& desk == io.github.jimbozoomer.jugcraft.tower.ConsoleDeskBlock.joined(desk, level, at),
						"the desk at " + at + " is joined as desk " + (k + 1) + " of its row: " + desk);
				helper.assertTrue(io.github.jimbozoomer.jugcraft.tower.ConsoleDeskBlock.page(desk) == k, "the desk at " + at + " shows page " + (k + 1));
				helper.assertTrue(desk.getValue(io.github.jimbozoomer.jugcraft.tower.ConsoleDeskBlock.ROLE) == row[4],
						"the desk at " + at + " works " + io.github.jimbozoomer.jugcraft.tower.ConsoleDeskBlock.ROLES[row[4]] + ": " + desk);
				helper.assertTrue(level.getBlockEntity(at) instanceof io.github.jimbozoomer.jugcraft.drone.DepotDisplayBlockEntity display
						&& core.terminalPos().equals(display.terminal()), "the desk at " + at + " is pinned to the tower's terminal");
			}
			roles.add(row[4]);
		}
		helper.assertTrue(roles.equals(java.util.Set.of(3, 5, 7, 12, 13)), "the desk rows left since layout 45: reception, security, the build "
				+ "desk, dispatch and supply: " + roles);
		for (int[] row : TriPillarArena.OLD_OPERATOR_ROWS) {
			for (int dz = 0; dz < 3; dz++) {
				BlockState s = level.getBlockState(TriPillarArena.at(core, new BlockPos(row[0], 1, row[1] + dz)));
				helper.assertTrue(!(s.getBlock() instanceof io.github.jimbozoomer.jugcraft.tower.ConsoleDeskBlock),
						"no operator desk left at " + row[0] + ", " + (row[1] + dz) + " (layout 45): " + s);
			}
		}
	}

	/**
	 * The flight-ops trees (layout 45, Justin, 5 November 2026): the Ember Oak on the west floor and the Drift Isle on the
	 * east, each whole (its master and every cell of its footprint a part of it, pointing back at it), the Ember Oak's
	 * outer basalt ring at seat height, the Drift Isle's island solid two blocks up, its rock hanging solid under the
	 * middle, and the air under its rim clear.
	 */
	static void assertOpsTrees(GameTestHelper helper, ServerLevel level, TowerCoreBlockEntity core) {
		for (var kind : io.github.jimbozoomer.jugcraft.tower.OpsTreeBlock.Kind.values()) {
			BlockPos master = TriPillarArena.at(core, kind == io.github.jimbozoomer.jugcraft.tower.OpsTreeBlock.Kind.EMBER_OAK
					? TriPillarArena.EMBER_OAK : TriPillarArena.DRIFT_ISLE);
			BlockState m = level.getBlockState(master);
			helper.assertTrue(m.getBlock() instanceof io.github.jimbozoomer.jugcraft.tower.OpsTreeBlock tree && tree.kind == kind,
					"the " + kind.id + " at " + master + ": " + m);
			for (net.minecraft.core.Vec3i o : kind.footprint()) {
				if (o.equals(net.minecraft.core.Vec3i.ZERO)) continue;
				BlockState part = level.getBlockState(master.offset(o));
				helper.assertTrue(part.getBlock() instanceof io.github.jimbozoomer.jugcraft.tower.OpsTreePartBlock p && p.kind == kind
						&& io.github.jimbozoomer.jugcraft.tower.OpsTreePartBlock.masterPos(master.offset(o), part).equals(master),
						"the " + kind.id + " has its part " + o + ": " + part);
			}
		}
		BlockPos bed = TriPillarArena.at(core, TriPillarArena.EMBER_OAK).offset(4, 0, 0);
		double top = level.getBlockState(bed).getCollisionShape(level, bed).max(net.minecraft.core.Direction.Axis.Y);
		helper.assertTrue(top > 0.3 && top < 1.2, "the Ember Oak's bed of basalt columns stands seat to knee high: " + top);
		BlockPos isle = TriPillarArena.at(core, TriPillarArena.DRIFT_ISLE).above(2);
		helper.assertTrue(!level.getBlockState(isle).getCollisionShape(level, isle).isEmpty(), "the Drift Isle's island is solid");
		// r181 (Justin, 8 November 2026: "you can walk into the mountain thing that's down there"): its rock hangs solid
		// under its cell in the middle, where a head under it bumps it; under the rim it stays clear
		double low = level.getBlockState(isle).getCollisionShape(level, isle).min(net.minecraft.core.Direction.Axis.Y);
		helper.assertTrue(low < -0.6, "the Drift Isle's rock hangs solid under the island: lowest " + low);
		helper.assertTrue(!level.noCollision(new net.minecraft.world.phys.AABB(isle.getX() + 0.3, isle.getY() - 0.6, isle.getZ() + 0.3,
				isle.getX() + 0.7, isle.getY() - 0.4, isle.getZ() + 0.7)), "a head under the island's middle bumps its rock");
		helper.assertTrue(level.noCollision(new net.minecraft.world.phys.AABB(isle.getX() + 2.2, isle.getY() - 0.9, isle.getZ() + 0.3,
				isle.getX() + 2.4, isle.getY() - 0.1, isle.getZ() + 0.7)), "under the island's rim is clear");
		// r183, r184: each Ember Oak comes with the mesh of its full-bright parts (its bark plates' sides, its leaves; the
		// client bakes it as light) and of its leaves' flames
		for (int k = 0; k < 3; k++) {
			for (String name : new String[] {"ops_ember_oak_" + k, "ops_ember_oak_" + k + "_bright", "ops_ember_oak_" + k + "_flame"}) {
				helper.assertTrue(TowerGameTests.class.getClassLoader().getResource("assets/jugcraft/tower_mesh/" + name + ".bin.gz") != null,
						"the Ember Oak's mesh " + name + " is in the mod");
			}
		}
	}

	/**
	 * The Holo Projector (work list item 15): whole, 3 x 3 on the lobby floor past the gates, facing the entrance, each cell
	 * colliding with its slab and deck (the deck only over the inner 2.76 blocks); the six floating holo panels it replaced
	 * gone from the operator areas.
	 */
	static void assertHoloProjector(GameTestHelper helper, ServerLevel level, TowerCoreBlockEntity core) {
		var block = io.github.jimbozoomer.jugcraft.tower.HoloProjectorBlock.class;
		BlockPos master = TriPillarArena.at(core, TriPillarArena.HOLO_PROJECTOR);
		BlockState projector = level.getBlockState(master);
		helper.assertTrue(block.isInstance(projector.getBlock())
				&& projector.getValue(io.github.jimbozoomer.jugcraft.tower.HoloProjectorBlock.FACING) == net.minecraft.core.Direction.NORTH,
				"a Holo Projector facing north at " + TriPillarArena.HOLO_PROJECTOR + ": " + projector);
		helper.assertTrue(level.getBlockEntity(master) instanceof io.github.jimbozoomer.jugcraft.drone.DepotDisplayBlockEntity,
				"the projector has its block entity (its renderer draws the head)");
		var footprint = io.github.jimbozoomer.jugcraft.tower.HoloProjectorBlock.FOOTPRINT;
		helper.assertTrue(footprint.size() == 9, "the projector covers 3 x 3 cells");
		for (net.minecraft.core.Vec3i o : footprint) {
			BlockPos at = master.offset(o);
			BlockState cell = level.getBlockState(at);
			if (!o.equals(net.minecraft.core.Vec3i.ZERO)) {
				helper.assertTrue(cell.getBlock() instanceof io.github.jimbozoomer.jugcraft.tower.HoloProjectorPartBlock
						&& io.github.jimbozoomer.jugcraft.tower.HoloProjectorPartBlock.masterPos(at, cell).equals(master),
						"the projector has its part " + o + ": " + cell);
			}
			var shape = cell.getCollisionShape(level, at);
			helper.assertTrue(!shape.isEmpty() && Math.abs(shape.max(net.minecraft.core.Direction.Axis.Y) - io.github.jimbozoomer.jugcraft.tower.HoloProjectorBlock.DECK) < 1e-6
					&& shape.min(net.minecraft.core.Direction.Axis.X) < 1e-6 && shape.max(net.minecraft.core.Direction.Axis.X) > 1 - 1e-6
					&& shape.min(net.minecraft.core.Direction.Axis.Z) < 1e-6 && shape.max(net.minecraft.core.Direction.Axis.Z) > 1 - 1e-6,
					"the projector's cell " + o + " collides across the cell, its deck 0.26 high: " + shape);
			helper.assertTrue(level.getBlockState(at.above()).isAir() && level.getBlockState(at.above(5)).isAir(),
					"the air over the projector is clear for the head at " + at);
		}
		for (BlockPos old : TriPillarArena.OLD_EMITTERS) {
			BlockState s = level.getBlockState(TriPillarArena.at(core, old));
			helper.assertTrue(!s.is(io.github.jimbozoomer.jugcraft.test.TowerGameTests.modBlock("holo_emitter")), "no holo emitter left over the operator area at " + old + ": " + s);
		}
	}

	/**
	 * The Maintenance Doors (work list item 10) of what the tower has built (the foundation's riser rooms; the roof's and
	 * each deck's pillar doors once their tier stands): a pair in each doorway, pushed from outside into the pillar, the
	 * active (hinge-left) leaf on the left as you come up, both leaves paired, shut, with the badge reader, hooded out on the
	 * roof and the decks; the readers the tower owner's.
	 */
	static void assertMaintenanceDoors(GameTestHelper helper, ServerLevel level, TowerCoreBlockEntity core) {
		var door = io.github.jimbozoomer.jugcraft.tower.JugcraftTower.MAINTENANCE_DOOR;
		int pairs = 0, want = 0;
		for (int[] d : TriPillarArena.MAINTENANCE_DOORS) {
			if (d[0] == 0 ? core.foundationState() != 2 : d[0] > core.tier()) {
				continue;
			}
			want++;
			net.minecraft.core.Direction out = d[5] > 0 ? net.minecraft.core.Direction.EAST : d[5] < 0 ? net.minecraft.core.Direction.WEST
					: d[6] > 0 ? net.minecraft.core.Direction.SOUTH : net.minecraft.core.Direction.NORTH;
			net.minecraft.core.Direction facing = out.getOpposite(), left = facing.getCounterClockWise();
			BlockPos a = TriPillarArena.at(core, new BlockPos(d[1], d[7] + 1, d[2])), b = TriPillarArena.at(core, new BlockPos(d[3], d[7] + 1, d[4]));
			boolean aLeft = a.getX() * left.getStepX() + a.getZ() * left.getStepZ() > b.getX() * left.getStepX() + b.getZ() * left.getStepZ();
			BlockPos[] feet = aLeft ? new BlockPos[] {a, b} : new BlockPos[] {b, a};
			for (int k = 0; k < 2; k++) {
				var hinge = k == 0 ? net.minecraft.world.level.block.state.properties.DoorHingeSide.LEFT
						: net.minecraft.world.level.block.state.properties.DoorHingeSide.RIGHT;
				for (int h = 0; h < 2; h++) {
					BlockPos at = feet[k].above(h);
					BlockState s = level.getBlockState(at);
					helper.assertTrue(s.is(door) && s.getValue(net.minecraft.world.level.block.DoorBlock.FACING) == facing
							&& s.getValue(net.minecraft.world.level.block.DoorBlock.HINGE) == hinge
							&& s.getValue(net.minecraft.world.level.block.DoorBlock.HALF) == (h == 0
									? net.minecraft.world.level.block.state.properties.DoubleBlockHalf.LOWER
									: net.minecraft.world.level.block.state.properties.DoubleBlockHalf.UPPER)
							&& s.getValue(io.github.jimbozoomer.jugcraft.nuclear.PlantDoorBlock.PAIRED)
							&& s.getValue(io.github.jimbozoomer.jugcraft.nuclear.PlantDoorBlock.READER)
							&& s.getValue(io.github.jimbozoomer.jugcraft.tower.MaintenanceDoorBlock.HOOD) == (d[0] >= 2)
							&& !s.getValue(net.minecraft.world.level.block.DoorBlock.OPEN),
							"tier " + d[0] + "'s maintenance door at " + at + " (leaf " + hinge + ", facing " + facing + "): " + s);
				}
				helper.assertTrue(level.getBlockEntity(feet[k]) instanceof io.github.jimbozoomer.jugcraft.nuclear.PlantDoorBlockEntity e
						&& java.util.Objects.equals(e.owner(), core.owner()), "the badge reader of the door at " + feet[k] + " is the tower owner's");
			}
			pairs++;
		}
		helper.assertTrue(pairs == want && want >= 3, "every built pillar doorway has its maintenance door pair: " + pairs + " of " + want);
	}

	/**
	 * The neon light lines (work list item 11) of what the tower has built: no amber box or end rod left anywhere in it;
	 * the atrium's slab edge (the status ticker's) a neon line in steel, over the rotunda cut in concrete; the hall's floor
	 * line to the entrance flush in the epoxy along its run; a thin white tube up each corner of pillar A's foot, in the
	 * corner toward the chamfer; every line in its material's quadrant by position (TowerPanelBlock.at), grooved where it is
	 * open and every floor line joined to those beside it, as the blocks work it out (layout 28); the lone floor lines inset
	 * lights.
	 */
	/**
	 * The 3D neon (redesign 2) works itself out: a line beside other lines runs on, beside a plain wall ends, open where
	 * the air is, joins a line touching it only at a corner; floor lines join round a corner; nothing else changes.
	 */
	@GameTest(maxTicks = 20)
	public void neonLinesShapeThemselves(GameTestHelper helper) {
		if (TestFocus.skip("tower")) {
			helper.succeed();
			return;
		}
		ServerLevel level = helper.getLevel();
		net.minecraft.world.level.block.Block line = io.github.jimbozoomer.jugcraft.test.TowerGameTests.modBlock("neon_line_concrete");
		net.minecraft.world.level.block.Block concrete = io.github.jimbozoomer.jugcraft.test.TowerGameTests.modBlock("reinforced_concrete");
		BlockPos[] lines = {new BlockPos(2, 2, 3), new BlockPos(3, 2, 3), new BlockPos(4, 2, 3), new BlockPos(5, 2, 4)};
		helper.setBlock(new BlockPos(1, 2, 3), concrete);
		for (BlockPos p : lines) {
			helper.setBlock(p, line);
		}
		for (BlockPos p : lines) {
			BlockPos abs = helper.absolutePos(p);
			level.setBlock(abs, io.github.jimbozoomer.jugcraft.tower.NeonLineBlock.shaped(level.getBlockState(abs), level, abs), net.minecraft.world.level.block.Block.UPDATE_ALL);
		}
		BlockState mid = helper.getBlockState(new BlockPos(3, 2, 3));
		helper.assertTrue(mid.getValue(io.github.jimbozoomer.jugcraft.tower.NeonLineBlock.NORTH) == io.github.jimbozoomer.jugcraft.tower.NeonLineBlock.Side.OPEN
				&& mid.getValue(io.github.jimbozoomer.jugcraft.tower.NeonLineBlock.SOUTH) == io.github.jimbozoomer.jugcraft.tower.NeonLineBlock.Side.OPEN
				&& mid.getValue(io.github.jimbozoomer.jugcraft.tower.NeonLineBlock.EAST) == io.github.jimbozoomer.jugcraft.tower.NeonLineBlock.Side.LINE
				&& mid.getValue(io.github.jimbozoomer.jugcraft.tower.NeonLineBlock.WEST) == io.github.jimbozoomer.jugcraft.tower.NeonLineBlock.Side.LINE,
				"a line between lines, open both sides: " + mid);
		BlockState end = helper.getBlockState(new BlockPos(2, 2, 3));
		helper.assertTrue(end.getValue(io.github.jimbozoomer.jugcraft.tower.NeonLineBlock.WEST) == io.github.jimbozoomer.jugcraft.tower.NeonLineBlock.Side.WALL,
				"a line's end against concrete: " + end);
		BlockState corner = helper.getBlockState(new BlockPos(4, 2, 3));
		helper.assertTrue(corner.getValue(io.github.jimbozoomer.jugcraft.tower.NeonLineBlock.SE) && !corner.getValue(io.github.jimbozoomer.jugcraft.tower.NeonLineBlock.NE),
				"a line touching another only at its south-east corner joins it there: " + corner);
		helper.assertTrue(corner.getLightEmission() == JugcraftTower.NEON_LIGHT && !corner.canOcclude(), "lit, its neighbours drawn against it");
		net.minecraft.world.level.block.Block floor = io.github.jimbozoomer.jugcraft.test.TowerGameTests.modBlock("neon_floor_epoxy");
		for (BlockPos p : new BlockPos[] {new BlockPos(1, 1, 7), new BlockPos(2, 1, 7), new BlockPos(2, 1, 8)}) {
			level.setBlock(helper.absolutePos(p), io.github.jimbozoomer.jugcraft.tower.NeonFloorBlock.shaped(floor.defaultBlockState(), level,
					helper.absolutePos(p)), net.minecraft.world.level.block.Block.UPDATE_ALL);
		}
		BlockState bend = helper.getBlockState(new BlockPos(2, 1, 7));
		helper.assertTrue(bend.getValue(io.github.jimbozoomer.jugcraft.tower.NeonFloorBlock.WEST) && bend.getValue(io.github.jimbozoomer.jugcraft.tower.NeonFloorBlock.SOUTH)
				&& !bend.getValue(io.github.jimbozoomer.jugcraft.tower.NeonFloorBlock.NORTH) && !bend.getValue(io.github.jimbozoomer.jugcraft.tower.NeonFloorBlock.EAST),
				"a floor line turning a corner joins both its neighbours (as they were placed after it): " + bend);
		helper.succeed();
	}

	static void assertNeonLines(GameTestHelper helper, ServerLevel level, TowerCoreBlockEntity core) {
		TowerData data = core.layout();
		java.util.List<TowerData.Tier> steps = new java.util.ArrayList<>(java.util.List.of(data.foundation));
		for (int t = 1; t <= core.tier(); t++) {
			steps.add(data.tier(t));
		}
		var amber = io.github.jimbozoomer.jugcraft.test.TowerGameTests.modBlock("amber_light_strip");
		int lines = 0, tubes = 0, insets = 0;
		for (TowerData.Tier step : steps) {
			for (int[] b : step.place) {
				BlockPos at = core.getBlockPos().offset(b[0], b[1], b[2]);
				BlockState s = level.getBlockState(at);
				helper.assertTrue(!s.is(amber) && !s.is(net.minecraft.world.level.block.Blocks.END_ROD), "no amber box or end rod left at " + at + ": " + s);
				if (JugcraftTower.isNeonLine(s)) {
					lines++;
					helper.assertTrue(s == io.github.jimbozoomer.jugcraft.tower.TowerPanelBlock.at(s, at), "the neon line at " + at + " takes its phase by position: " + s);
					helper.assertTrue(s == io.github.jimbozoomer.jugcraft.tower.NeonLineBlock.shaped(s, level, at),
							"the neon line at " + at + " is grooved where it is open, as the blocks round it have it: " + s);
				}
				if (s.getBlock() instanceof io.github.jimbozoomer.jugcraft.tower.NeonFloorBlock) {
					helper.assertTrue(s == io.github.jimbozoomer.jugcraft.tower.NeonFloorBlock.shaped(s, level, at),
							"the floor line at " + at + " joins the floor lines beside it: " + s);
				}
				if (s.getBlock() instanceof io.github.jimbozoomer.jugcraft.tower.InsetFloorLightBlock) {
					insets++;
				}
				if (s.is(JugcraftTower.NEON_TUBE)) {
					tubes++;
				}
			}
		}
		helper.assertTrue(lines > 400 && tubes > 200, "the neon lines and tubes stand: " + lines + " lines, " + tubes + " tubes");
		BlockState edge = level.getBlockState(TriPillarArena.at(core, TriPillarArena.TICKER_EDGE));
		helper.assertTrue(edge.is(io.github.jimbozoomer.jugcraft.test.TowerGameTests.modBlock("neon_line_steel")) && edge.getLightEmission() == JugcraftTower.NEON_LIGHT,
				"the atrium's slab edge is a neon line in steel, lit: " + edge);
		BlockState floor = level.getBlockState(TriPillarArena.at(core, TriPillarArena.NEON_FLOOR_LINE));
		helper.assertTrue(floor.is(io.github.jimbozoomer.jugcraft.test.TowerGameTests.modBlock("neon_floor_epoxy"))
				&& floor.getValue(io.github.jimbozoomer.jugcraft.tower.NeonFloorBlock.NORTH) && floor.getValue(io.github.jimbozoomer.jugcraft.tower.NeonFloorBlock.SOUTH)
				&& !floor.getValue(io.github.jimbozoomer.jugcraft.tower.NeonFloorBlock.EAST) && !floor.getValue(io.github.jimbozoomer.jugcraft.tower.NeonFloorBlock.WEST),
				"the lobby's floor line runs on along z in the epoxy: " + floor);
		helper.assertTrue(insets > 0, "a floor line on its own is an inset light");
		for (int k = 0; k < 2; k++) {
			BlockPos tubeAt = TriPillarArena.at(core, TriPillarArena.NEON_TUBES[k]);
			for (int y = 0; y < 9; y++) {
				BlockState tube = level.getBlockState(tubeAt.above(y));
				helper.assertTrue(tube.is(JugcraftTower.NEON_TUBE)
						&& tube.getValue(io.github.jimbozoomer.jugcraft.tower.NeonTubeBlock.FACING) == net.minecraft.core.Direction.SOUTH
						&& tube.getValue(io.github.jimbozoomer.jugcraft.tower.NeonTubeBlock.SIDE) == (k == 0
								? net.minecraft.world.level.block.state.properties.DoorHingeSide.LEFT
								: net.minecraft.world.level.block.state.properties.DoorHingeSide.RIGHT)
						&& tube.getLightEmission() == JugcraftTower.NEON_TUBE_LIGHT,
						"a neon tube up pillar A's foot's corner at " + tubeAt.above(y) + ": " + tube);
			}
		}
	}

	/**
	 * The joined girders (work list item 12): each of the entrance canopy's posts one column of girder blocks, joined all
	 * the way up (no plate between two of them), standing on a base plate the cell's full width and under a cap plate.
	 */
	static void assertCanopyPosts(GameTestHelper helper, ServerLevel level, TowerCoreBlockEntity core) {
		var girder = io.github.jimbozoomer.jugcraft.tower.TowerGirderBlock.class;
		for (BlockPos post : TriPillarArena.CANOPY_POSTS) {
			BlockPos foot = TriPillarArena.at(core, post);
			int h = TriPillarArena.CANOPY_POST_HEIGHT;
			for (int y = 0; y < h; y++) {
				BlockState s = level.getBlockState(foot.above(y));
				helper.assertTrue(girder.isInstance(s.getBlock())
						&& s.getValue(io.github.jimbozoomer.jugcraft.tower.TowerGirderBlock.AXIS) == net.minecraft.core.Direction.Axis.Y
						&& s.getValue(io.github.jimbozoomer.jugcraft.tower.TowerGirderBlock.JOIN_LO) == (y > 0)
						&& s.getValue(io.github.jimbozoomer.jugcraft.tower.TowerGirderBlock.JOIN_HI) == (y < h - 1),
						"the canopy post at " + post + " is one column, joined but at its foot and head (height " + (y + 1) + "): " + s);
			}
			var base = level.getBlockState(foot).getCollisionShape(level, foot);
			helper.assertTrue(base.min(net.minecraft.core.Direction.Axis.Y) < 1e-6 && base.min(net.minecraft.core.Direction.Axis.X) < 1e-6
					&& base.max(net.minecraft.core.Direction.Axis.X) > 1 - 1e-6, "the post stands on its base plate: " + base);
			var middle = level.getBlockState(foot.above(4)).getCollisionShape(level, foot.above(4));
			helper.assertTrue(middle.min(net.minecraft.core.Direction.Axis.X) > 0.1 && middle.max(net.minecraft.core.Direction.Axis.X) < 0.9,
					"between its plates the post is its section, no wider: " + middle);
		}
	}

	/**
	 * The Cradle Struts (work list item 12) under the tier-2 supply cradle: the pair whole (its master on the chamfer at the
	 * left strut's foot, facing south, every other cell a part that knows its master and collides with what of the struts
	 * is in it), and no girder block left of the staircases they replaced.
	 */
	static void assertCradleStruts(GameTestHelper helper, ServerLevel level, TowerCoreBlockEntity core) {
		var block = io.github.jimbozoomer.jugcraft.tower.CradleStrutsBlock.class;
		BlockPos master = TriPillarArena.at(core, TriPillarArena.CRADLE_STRUTS);
		BlockState struts = level.getBlockState(master);
		helper.assertTrue(block.isInstance(struts.getBlock())
				&& struts.getValue(io.github.jimbozoomer.jugcraft.tower.CradleStrutsBlock.FACING) == net.minecraft.core.Direction.SOUTH,
				"the Cradle Struts facing south at " + TriPillarArena.CRADLE_STRUTS + ": " + struts);
		helper.assertTrue(level.getBlockEntity(master) instanceof io.github.jimbozoomer.jugcraft.drone.DepotDisplayBlockEntity,
				"the struts have their block entity (their renderer draws them)");
		var cells = io.github.jimbozoomer.jugcraft.tower.CradleStrutsBlock.CELLS;
		helper.assertTrue(cells.size() > 40, "the struts pass through their cells, got " + cells.size());
		for (int i = 0; i < cells.size(); i++) {
			BlockPos at = master.offset(io.github.jimbozoomer.jugcraft.tower.CradleStrutsBlock.offset(i, net.minecraft.core.Direction.SOUTH));
			BlockState cell = level.getBlockState(at);
			if (i > 0) {
				helper.assertTrue(cell.getBlock() instanceof io.github.jimbozoomer.jugcraft.tower.CradleStrutsPartBlock
						&& cell.getValue(io.github.jimbozoomer.jugcraft.tower.CradleStrutsPartBlock.INDEX) == i
						&& io.github.jimbozoomer.jugcraft.tower.CradleStrutsPartBlock.masterPos(at, cell).equals(master),
						"the struts have their cell " + i + " at " + at + ": " + cell);
			}
			helper.assertTrue(!cell.getCollisionShape(level, at).isEmpty() && cell.getCollisionShape(level, at).max(net.minecraft.core.Direction.Axis.Y) < 1 + 1e-6,
					"the struts' cell " + i + " collides with them: " + cell.getCollisionShape(level, at));
		}
		for (BlockPos p : BlockPos.betweenClosed(master.offset(-5, -1, -1), master.offset(3, 7, 7))) {
			helper.assertTrue(!level.getBlockState(p).is(io.github.jimbozoomer.jugcraft.test.TowerGameTests.modBlock("steel_girder")), "no girder staircase left under the cradle at " + p);
		}
	}

	/**
	 * The map room (work list item 16): the Big Map Table whole on the upper floor (its master, its block entity, every
	 * other cell a part that knows its master and collides with the table's top, no higher), pinned to the tower's
	 * terminal; the room's carbon walls and glass front, its glass double doors (hinges outer), the MAP ROOM sign over them on
	 * the band, never on the glass, and no ceiling panels over the map.
	 */
	static void assertMapRoom(GameTestHelper helper, ServerLevel level, TowerCoreBlockEntity core) {
		BlockPos master = TriPillarArena.at(core, TriPillarArena.BIG_MAP_TABLE);
		helper.assertTrue(level.getBlockState(master).getBlock() instanceof io.github.jimbozoomer.jugcraft.tower.BigMapTableBlock,
				"the Big Map Table at " + TriPillarArena.BIG_MAP_TABLE + ": " + level.getBlockState(master));
		helper.assertTrue(level.getBlockEntity(master) instanceof io.github.jimbozoomer.jugcraft.tower.BigMapTableBlockEntity table
				&& (core.terminal() == null || core.terminal().getBlockPos().equals(table.terminal())),
				"the table has its block entity, pinned to the tower's terminal");
		var cells = io.github.jimbozoomer.jugcraft.tower.BigMapTableBlock.CELLS;
		helper.assertTrue(cells.size() > 150 && cells.size() <= 169, "the table covers its octagon's cells, got " + cells.size());
		for (int i = 0; i < cells.size(); i++) {
			BlockPos at = master.offset(cells.get(i));
			BlockState cell = level.getBlockState(at);
			if (i > 0) {
				helper.assertTrue(cell.getBlock() instanceof io.github.jimbozoomer.jugcraft.tower.BigMapTablePartBlock
						&& cell.getValue(io.github.jimbozoomer.jugcraft.tower.BigMapTablePartBlock.INDEX) == i
						&& io.github.jimbozoomer.jugcraft.tower.BigMapTablePartBlock.masterPos(at, cell).equals(master),
						"the table has its cell " + i + " at " + at + ": " + cell);
			}
			var shape = cell.getCollisionShape(level, at);
			helper.assertTrue(!shape.isEmpty() && Math.abs(shape.max(net.minecraft.core.Direction.Axis.Y) - 0.86) < 1e-6,
					"the table's cell " + i + " collides up to its top (0.86): " + shape);
		}
		BlockPos sign = TriPillarArena.at(core, TriPillarArena.MAP_ROOM_SIGN);
		for (int p = 0; p < 4; p++) {
			BlockState s = level.getBlockState(sign.east(p));
			helper.assertTrue(s.is(io.github.jimbozoomer.jugcraft.test.TowerGameTests.modBlock("map_room_sign")) && s.getValue(net.minecraft.world.level.block.HorizontalDirectionalBlock.FACING)
					== net.minecraft.core.Direction.NORTH && s.toString().contains("part=" + p), "the MAP ROOM sign's part " + p + ": " + s);
			helper.assertTrue(level.getBlockState(sign.east(p).north()).is(io.github.jimbozoomer.jugcraft.test.TowerGameTests.modBlock("carbon_composite_panel")),
					"the sign hangs on the solid band, not on the glass");
		}
		// its doors: an automatic sliding pair (layout 47), the master the east cell, its outside the gallery (south)
		BlockPos door = TriPillarArena.at(core, TriPillarArena.MAP_ROOM_DOOR);
		BlockState mapDoor = level.getBlockState(door.east());
		helper.assertTrue(mapDoor.is(io.github.jimbozoomer.jugcraft.test.TowerGameTests.modBlock("automatic_sliding_door_glass"))
				&& mapDoor.getValue(io.github.jimbozoomer.jugcraft.tower.SlidingDoorBlock.FACING) == Direction.SOUTH, "the map room's sliding doors: " + mapDoor);
		BlockState mapPart = level.getBlockState(door);
		helper.assertTrue(mapPart.getBlock() instanceof io.github.jimbozoomer.jugcraft.tower.SlidingDoorPartBlock
				&& io.github.jimbozoomer.jugcraft.tower.SlidingDoorPartBlock.masterPos(door, mapPart).equals(door.east()), "the map room door's west cell: " + mapPart);
		for (BlockPos lamp : TriPillarArena.MAP_LAMPS_OUT) {
			helper.assertTrue(!level.getBlockState(TriPillarArena.at(core, lamp)).is(io.github.jimbozoomer.jugcraft.test.TowerGameTests.modBlock("ceiling_light_panel")),
					"no ceiling panel over the map at " + lamp);
		}
		for (BlockPos wall : TriPillarArena.MAP_ROOM_WALLS) {
			helper.assertTrue(level.getBlockState(TriPillarArena.at(core, wall)).is(io.github.jimbozoomer.jugcraft.test.TowerGameTests.modBlock("carbon_composite_panel")),
					"the map room's wall at " + wall);
		}
		helper.assertTrue(level.getBlockState(TriPillarArena.at(core, TriPillarArena.MAP_ROOM_GLASS)).is(io.github.jimbozoomer.jugcraft.test.TowerGameTests.modBlock("framed_glass")),
				"the map room's front is framed glass");
	}

	private static net.minecraft.world.level.block.Block block(String name) {
		return net.minecraft.core.registries.BuiltInRegistries.BLOCK.getValue(io.github.jimbozoomer.jugcraft.Jugcraft.id(name));
	}

	/**
	 * The ground floor's rooms (work list item 22, part A): the locker room's three shower stalls side by side against
	 * the east wall (off, facing east); the restroom's and washroom's toilets and the urinal; the security office's alarm
	 * console under the live screen; the cafe's live screen; the battery room's station battery and UPS module; each
	 * room's doors (pairs that open together; glass for security and the cafe, steel for the lockers and the batteries)
	 * with the wall whole over them (glass over a glass door) to the steel band; the plaques by the doors, those over
	 * glass doors on the band, never on the glass.
	 */
	/** The rooms' walls go on up from their steel band to the hall's ceiling in the dark carbon panel (layout 27). */
	static void assertWallsToCeiling(GameTestHelper helper, ServerLevel level, TowerCoreBlockEntity core) {
		for (int[] w : TriPillarArena.WALLS_TO_CEILING) {
			BlockPos at = TriPillarArena.at(core, new BlockPos(w[0], 0, w[1]));
			helper.assertTrue(level.getBlockState(at.atY(core.getBlockPos().getY() + w[2])).is(io.github.jimbozoomer.jugcraft.test.TowerGameTests.modBlock("steel_armor_plate")),
					"the band over the wall at " + w[0] + ", " + w[1]);
			for (int y = w[2] + 1; y < w[3]; y++) {
				BlockState s = level.getBlockState(at.atY(core.getBlockPos().getY() + y));
				helper.assertTrue(s.is(io.github.jimbozoomer.jugcraft.test.TowerGameTests.modBlock("carbon_composite_panel")), "the wall at " + w[0] + ", " + w[1] + " goes on up at y " + y + ": " + s);
			}
		}
	}

	/**
	 * The first-floor look-over (Justin, 10 November 2026; layout 49): a room's wall onto the hall is a faced wall, its
	 * concrete face to the hall, with the light line at the pillars' height running on from block to block (the locker
	 * room's north wall); a tiled room keeps its tiles behind it (the washroom's east wall); a corner is the pillars' own
	 * line, turning; the slot between the security office and the battery room is filled to the band and the skin, its
	 * end on the hall carrying the line; the battery room's old doorway is wall; the wedge beside the restrooms is filled
	 * and the stair's foot beside it clear; the line runs on along the stair's side; the goods lift's nook is closed; the
	 * benches and a plant stand; every faced line has the ends its neighbours give it, and every faced wall its place's
	 * phase.
	 */
	static void assertFirstFloorHallSide(GameTestHelper helper, ServerLevel level, TowerCoreBlockEntity core) {
		var facing = io.github.jimbozoomer.jugcraft.tower.FacedWallBlock.FACING;
		var inside = io.github.jimbozoomer.jugcraft.tower.FacedWallBlock.INSIDE;
		for (int y : new int[] {1, 2, 4}) {
			BlockState wall = level.getBlockState(TriPillarArena.at(core, new BlockPos(-35, y, 21)));
			helper.assertTrue(wall.is(block("faced_wall_concrete")) && wall.getValue(facing) == Direction.NORTH
					&& wall.getValue(inside) == io.github.jimbozoomer.jugcraft.tower.FacedWallBlock.Inside.PAINTED,
					"the locker room's wall faces the hall in concrete at y " + y + ": " + wall);
		}
		BlockState line = level.getBlockState(TriPillarArena.at(core, new BlockPos(-35, 3, 21)));
		helper.assertTrue(line.is(block("faced_line_concrete")) && line.getValue(io.github.jimbozoomer.jugcraft.tower.FacedLineBlock.FACING) == Direction.NORTH
				&& line.getValue(io.github.jimbozoomer.jugcraft.tower.FacedLineBlock.LEFT) && line.getValue(io.github.jimbozoomer.jugcraft.tower.FacedLineBlock.RIGHT)
				&& line.getLightEmission() == JugcraftTower.NEON_LIGHT, "the light line along the locker room's wall, running on both ways: " + line);
		Object[][] tiled = {{1, io.github.jimbozoomer.jugcraft.tower.FacedWallBlock.Inside.TILE}, {2, io.github.jimbozoomer.jugcraft.tower.FacedWallBlock.Inside.TILE_CAP},
				{4, io.github.jimbozoomer.jugcraft.tower.FacedWallBlock.Inside.PAINTED}};
		for (Object[] t : tiled) {
			BlockState wall = level.getBlockState(TriPillarArena.at(core, new BlockPos(-21, (Integer) t[0], -42)));
			helper.assertTrue(wall.is(block("faced_wall_concrete")) && wall.getValue(facing) == Direction.EAST && wall.getValue(inside) == t[1],
					"the washroom's wall onto the hall keeps its " + t[1] + " inside at y " + t[0] + ": " + wall);
		}
		BlockState corner = level.getBlockState(TriPillarArena.at(core, new BlockPos(-24, 3, 21)));
		helper.assertTrue(corner.is(block("neon_line_concrete"))
				&& corner.getValue(io.github.jimbozoomer.jugcraft.tower.NeonLineBlock.NORTH) == io.github.jimbozoomer.jugcraft.tower.NeonLineBlock.Side.OPEN
				&& corner.getValue(io.github.jimbozoomer.jugcraft.tower.NeonLineBlock.EAST) == io.github.jimbozoomer.jugcraft.tower.NeonLineBlock.Side.OPEN
				&& corner.getValue(io.github.jimbozoomer.jugcraft.tower.NeonLineBlock.SOUTH) == io.github.jimbozoomer.jugcraft.tower.NeonLineBlock.Side.LINE
				&& corner.getValue(io.github.jimbozoomer.jugcraft.tower.NeonLineBlock.WEST) == io.github.jimbozoomer.jugcraft.tower.NeonLineBlock.Side.LINE,
				"the locker room's corner turns the line, the faced lines either side lines to it: " + corner);
		// a line stops at glass: the security office's pier beside its glass front
		BlockState pier = level.getBlockState(TriPillarArena.at(core, new BlockPos(30, 3, 19)));
		helper.assertTrue(pier.is(block("neon_line_concrete"))
				&& pier.getValue(io.github.jimbozoomer.jugcraft.tower.NeonLineBlock.WEST) == io.github.jimbozoomer.jugcraft.tower.NeonLineBlock.Side.OPEN
				&& pier.getValue(io.github.jimbozoomer.jugcraft.tower.NeonLineBlock.NORTH) == io.github.jimbozoomer.jugcraft.tower.NeonLineBlock.Side.WALL,
				"the line stops at the security office's glass, it doesn't turn onto the reveal: " + pier);
		// the slot between the security office and the battery room, filled
		for (int x = 30; x <= 39; x++) {
			for (int y = 1; y <= 9; y++) {
				BlockState s = level.getBlockState(TriPillarArena.at(core, new BlockPos(x, y, 20)));
				boolean ok = y == 5 ? s.is(io.github.jimbozoomer.jugcraft.test.TowerGameTests.modBlock("steel_armor_plate"))
						: y > 5 ? s.is(io.github.jimbozoomer.jugcraft.test.TowerGameTests.modBlock("carbon_composite_panel"))
						: x == 30 && y == 3 ? s.is(block("neon_line_concrete")) : s.is(io.github.jimbozoomer.jugcraft.test.TowerGameTests.modBlock("reinforced_concrete"));
				helper.assertTrue(ok, "the slot beside the battery room is closed at " + x + ", " + y + ": " + s);
			}
		}
		for (int y = 1; y <= 2; y++) {
			helper.assertTrue(level.getBlockState(TriPillarArena.at(core, new BlockPos(36, y, 21))).is(block("painted_wall")),
					"the battery room's old doorway is wall at y " + y);
		}
		// the wedge beside the restrooms, and the stair's foot beside it
		helper.assertTrue(level.getBlockState(TriPillarArena.at(core, new BlockPos(-31, 2, -33))).is(io.github.jimbozoomer.jugcraft.test.TowerGameTests.modBlock("reinforced_concrete"))
				&& level.getBlockState(TriPillarArena.at(core, new BlockPos(-31, 3, -31))).is(block("neon_line_concrete")),
				"the wedge beside the restrooms is closed, the line on its face by the stair");
		helper.assertTrue(level.getBlockState(TriPillarArena.at(core, new BlockPos(-32, 1, -30))).isAir()
				&& level.getBlockState(TriPillarArena.at(core, new BlockPos(-32, 2, -30))).isAir(), "the stair's foot is clear");
		helper.assertTrue(level.getBlockState(TriPillarArena.at(core, new BlockPos(-24, 3, -29))).is(block("neon_line_concrete"))
				&& level.getBlockState(TriPillarArena.at(core, new BlockPos(-28, 3, -29))).is(io.github.jimbozoomer.jugcraft.test.TowerGameTests.modBlock("reinforced_concrete")),
				"the line along the stair's side, from where its wall is a block higher");
		// the goods lift's nook
		helper.assertTrue(level.getBlockState(TriPillarArena.at(core, new BlockPos(41, 2, -15))).is(io.github.jimbozoomer.jugcraft.test.TowerGameTests.modBlock("framed_glass"))
				&& level.getBlockState(TriPillarArena.at(core, new BlockPos(42, 2, -15))).is(io.github.jimbozoomer.jugcraft.test.TowerGameTests.modBlock("steel_armor_plate")),
				"the goods lift's nook is closed with its house's glass and a post");
		helper.assertTrue(level.getBlockState(TriPillarArena.at(core, new BlockPos(-35, 1, 20))).is(block("sofa"))
				&& level.getBlockState(TriPillarArena.at(core, new BlockPos(-33, 1, 20))).is(block("office_plant")), "a bench and a plant by the locker room");
		// every faced line as its neighbours shape it; every faced wall and line with its place's phase (the ground
		// floor's: the upper floor's are assertUpperFloorGallerySide's)
		TowerData data = TowerData.get();
		int lines = 0, walls = 0;
		for (int[] b : data.foundation.place) {
			if (b[1] > 4) continue;
			BlockPos at = core.getBlockPos().offset(b[0], b[1], b[2]);
			BlockState s = level.getBlockState(at);
			if (s.getBlock() instanceof io.github.jimbozoomer.jugcraft.tower.FacedLineBlock) {
				helper.assertTrue(s == io.github.jimbozoomer.jugcraft.tower.FacedLineBlock.shaped(s, level, at), "a faced line's ends as its neighbours give them at " + at + ": " + s);
				lines++;
			} else if (s.getBlock() instanceof io.github.jimbozoomer.jugcraft.tower.FacedWallBlock) {
				walls++;
			} else {
				continue;
			}
			helper.assertTrue(s == io.github.jimbozoomer.jugcraft.tower.TowerPanelBlock.at(s, at), "the concrete's phase where it stands at " + at + ": " + s);
		}
		helper.assertTrue(lines == 53 && walls == 160, "53 faced lines and 160 faced walls, got " + lines + " and " + walls);
	}

	/**
	 * The upper floor's walls (Justin, 10 November 2026; layout 50): a room's wall onto the gallery or a corridor is a
	 * faced wall with the light line three blocks up, on both sides of the west corridor; the pillars up there carry the
	 * same line at the same height; every faced line has the ends its neighbours give it and every faced wall and line
	 * its place's phase.
	 */
	static void assertUpperFloorGallerySide(GameTestHelper helper, ServerLevel level, TowerCoreBlockEntity core) {
		var facing = io.github.jimbozoomer.jugcraft.tower.FacedWallBlock.FACING;
		Object[][] sides = {{-35, Direction.WEST, 10}, {-38, Direction.EAST, 0}};       // the west corridor's two walls
		for (Object[] side : sides) {
			int x = (Integer) side[0], z = (Integer) side[2];
			for (int y : new int[] {12, 13, 15}) {
				BlockState wall = level.getBlockState(TriPillarArena.at(core, new BlockPos(x, y, z)));
				helper.assertTrue(wall.is(block("faced_wall_concrete")) && wall.getValue(facing) == side[1]
						&& wall.getValue(io.github.jimbozoomer.jugcraft.tower.FacedWallBlock.INSIDE) == io.github.jimbozoomer.jugcraft.tower.FacedWallBlock.Inside.PAINTED,
						"the upper corridor's wall faces it in concrete at " + x + ", " + y + ", " + z + ": " + wall);
			}
			BlockState line = level.getBlockState(TriPillarArena.at(core, new BlockPos(x, 14, z)));
			helper.assertTrue(line.is(block("faced_line_concrete")) && line.getValue(io.github.jimbozoomer.jugcraft.tower.FacedLineBlock.FACING) == side[1]
					&& line.getValue(io.github.jimbozoomer.jugcraft.tower.FacedLineBlock.LEFT) && line.getValue(io.github.jimbozoomer.jugcraft.tower.FacedLineBlock.RIGHT),
					"the light line along the upper corridor's wall at " + x + ", " + z + ": " + line);
		}
		BlockState pillar = level.getBlockState(TriPillarArena.at(core, new BlockPos(-24, 14, -14)));
		helper.assertTrue(pillar.is(block("neon_line_concrete")) && pillar.getLightEmission() == JugcraftTower.NEON_LIGHT,
				"a pillar carries the line on the upper floor: " + pillar);
		helper.assertTrue(level.getBlockState(TriPillarArena.at(core, new BlockPos(-24, 13, -14))).is(io.github.jimbozoomer.jugcraft.test.TowerGameTests.modBlock("reinforced_concrete")),
				"the pillar under its line is plain concrete");
		TowerData data = TowerData.get();
		int lines = 0, walls = 0, neon = 0;
		for (int[] b : data.foundation.place) {
			if (b[1] < 12 || b[1] > 15) continue;
			BlockPos at = core.getBlockPos().offset(b[0], b[1], b[2]);
			BlockState s = level.getBlockState(at);
			if (b[1] == 14 && s.is(block("neon_line_concrete"))) {
				neon++;
				helper.assertTrue(s == io.github.jimbozoomer.jugcraft.tower.NeonLineBlock.shaped(s, level, at), "an upper line as its neighbours shape it at " + at + ": " + s);
			}
			if (s.getBlock() instanceof io.github.jimbozoomer.jugcraft.tower.FacedLineBlock) {
				helper.assertTrue(b[1] == 14, "a faced line only at the line's height, not at " + at);
				helper.assertTrue(s == io.github.jimbozoomer.jugcraft.tower.FacedLineBlock.shaped(s, level, at), "a faced line's ends as its neighbours give them at " + at + ": " + s);
				lines++;
			} else if (s.getBlock() instanceof io.github.jimbozoomer.jugcraft.tower.FacedWallBlock) {
				walls++;
			} else {
				continue;
			}
			helper.assertTrue(s == io.github.jimbozoomer.jugcraft.tower.TowerPanelBlock.at(s, at), "the concrete's phase where it stands at " + at + ": " + s);
		}
		helper.assertTrue(lines == 189 && walls == 552 && neon == 133,
				"189 faced lines, 552 faced walls and 133 concrete lines (121 on the pillars) upstairs, got " + lines + ", " + walls + " and " + neon);
	}

	/** A room's wall, whichever way it is finished: paint, a concrete face onto the hall (with or without the line), or
	 * the plain concrete of a corner that shows on two sides. */
	static boolean walled(BlockState s) {
		return s.is(block("painted_wall")) || s.is(block("reinforced_concrete")) || s.is(block("neon_line_concrete"))
				|| s.getBlock() instanceof io.github.jimbozoomer.jugcraft.tower.FacedWallBlock
				|| s.getBlock() instanceof io.github.jimbozoomer.jugcraft.tower.FacedLineBlock;
	}

	static void assertGroundFloorRooms(GameTestHelper helper, ServerLevel level, TowerCoreBlockEntity core) {
		for (BlockPos p : TriPillarArena.SHOWER_STALLS) {
			BlockState stall = level.getBlockState(TriPillarArena.at(core, p));
			helper.assertTrue(stall.is(block("shower_stall")) && stall.getValue(net.minecraft.world.level.block.HorizontalDirectionalBlock.FACING) == Direction.EAST
					&& !stall.getValue(io.github.jimbozoomer.jugcraft.nuclear.RestroomBlocks.RUNNING), "a shower stall at " + p + ": " + stall);
		}
		for (BlockPos p : TriPillarArena.RESTROOM_TOILETS) {
			helper.assertTrue(level.getBlockState(TriPillarArena.at(core, p)).is(block("restroom_toilet")), "a toilet at " + p);
		}
		helper.assertTrue(level.getBlockState(TriPillarArena.at(core, TriPillarArena.RESTROOM_URINAL)).is(block("restroom_urinal")), "the urinal");
		BlockState console = level.getBlockState(TriPillarArena.at(core, TriPillarArena.SECURITY_CONSOLE));
		helper.assertTrue(console.is(block("sec_alarm_console")) && console.toString().contains("part=0"), "the alarm console: " + console);
		helper.assertTrue(level.getBlockState(TriPillarArena.at(core, TriPillarArena.SECURITY_SCREEN)).is(io.github.jimbozoomer.jugcraft.test.TowerGameTests.modBlock("control_screen")),
				"the security office's live screen");
		// the break area (layout 31): a Flat Screen TV on the wall (layout 51; the Lounge TV before), the long glass coffee
		// table in front of the sofa with a block to walk through between them; the control screen and the small table gone
		assertFlatTv(helper, level, core, TriPillarArena.CAFE_TV, Direction.EAST, 3, 2);
		BlockPos table = TriPillarArena.at(core, TriPillarArena.CAFE_TABLE);
		for (int dz = -1; dz <= 1; dz++) {
			BlockState t = level.getBlockState(table.offset(0, 0, dz));
			helper.assertTrue(t.is(block("long_glass_table")) && t.toString().contains("part=" + (dz + 1)) && t.toString().contains("facing=east"),
					"the long glass coffee table's part " + (dz + 1) + ": " + t);
			helper.assertTrue(level.getBlockState(table.offset(-1, 0, dz)).isAir() && level.getBlockState(table.offset(-1, 1, dz)).isAir(),
					"room to walk between the sofa and the table at " + table.offset(-1, 0, dz));
		}
		// the cafe's kitchen (layout 32): the upstairs kitchen kit's run along the west wall and its fridge freezer; none of
		// the plant's counters, sink, stove or fridges
		RoomKitGameTests.assertWhole(helper, level, TriPillarArena.at(core, TriPillarArena.CAFE_KITCHEN_RUN), "kitchen_run");
		RoomKitGameTests.assertWhole(helper, level, TriPillarArena.at(core, TriPillarArena.CAFE_FRIDGE), "fridge_freezer");
		for (BlockPos p : BlockPos.betweenClosed(TriPillarArena.at(core, new BlockPos(-23, 1, 31)), TriPillarArena.at(core, new BlockPos(-20, 3, 44)))) {
			BlockState s = level.getBlockState(p);
			for (String old : new String[] {"kitchen_counter", "coffee_counter", "microwave_counter", "kitchen_sink", "kitchen_stove", "fridge"}) {
				helper.assertTrue(!s.is(block(old)), "no plant " + old + " left in the cafe's kitchen: " + s + " at " + p);
			}
		}
		for (BlockPos p : BlockPos.betweenClosed(TriPillarArena.at(core, new BlockPos(18, 1, 34)), TriPillarArena.at(core, new BlockPos(23, 3, 40)))) {
			BlockState s = level.getBlockState(p);
			helper.assertTrue(!s.is(io.github.jimbozoomer.jugcraft.test.TowerGameTests.modBlock("control_screen")) && !s.is(block("glass_coffee_table")),
					"no control screen or small glass table left in the break area: " + s + " at " + p);
		}
		helper.assertTrue(level.getBlockState(TriPillarArena.at(core, TriPillarArena.STATION_BATTERY)).is(block("station_battery")), "a station battery");
		helper.assertTrue(level.getBlockState(TriPillarArena.at(core, TriPillarArena.UPS_MODULE)).is(block("ups_module")), "the UPS module");
		// the locker room (redesign 4): crew lockers, some standing open; no plant lockers with lab coats, no coat rack
		int crew = 0, lockers = 0;
		for (BlockPos p : BlockPos.betweenClosed(TriPillarArena.at(core, new BlockPos(-40, 1, 22)), TriPillarArena.at(core, new BlockPos(-25, 2, 44)))) {
			BlockState s = level.getBlockState(p);
			helper.assertTrue(!s.is(block("locker_open")) && !s.is(block("coat_rack")), "no lab-coat locker or coat rack in the locker room: " + s + " at " + p);
			if (s.is(block("crew_locker_open"))) crew++;
			if (s.is(block("locker"))) lockers++;
		}
		helper.assertTrue(crew >= 4 && lockers >= 20, "the locker room's crew lockers (" + crew + " open) and lockers (" + lockers + ")");
		// the rooms' doors: automatic sliding pairs (layout 47), the master where you walk in on your right, its outside the
		// side you come from, its second cell to your left; the wall whole over them
		Object[][] doors = {{TriPillarArena.SECURITY_DOOR, "automatic_sliding_door_glass", Direction.EAST},
				{TriPillarArena.CAFE_DOOR, "automatic_sliding_door_glass", Direction.SOUTH},
				{TriPillarArena.LOCKER_DOOR, "automatic_sliding_door_steel", Direction.SOUTH},
				{TriPillarArena.BATTERY_DOOR, "automatic_sliding_door_steel", Direction.EAST}};
		for (Object[] d : doors) {
			BlockPos leaf = TriPillarArena.at(core, (BlockPos) d[0]);
			Direction in = (Direction) d[2];
			BlockPos other = leaf.relative(in.getCounterClockWise());
			BlockState master = level.getBlockState(leaf);
			helper.assertTrue(master.is(block((String) d[1])) && master.getValue(io.github.jimbozoomer.jugcraft.tower.SlidingDoorBlock.FACING) == in.getOpposite()
					&& !master.getValue(io.github.jimbozoomer.jugcraft.tower.SlidingDoorBlock.OPEN), "a shut " + d[1] + " at " + leaf + ": " + master);
			for (int y = 0; y < 2; y++) {
				BlockState part = level.getBlockState(other.above(y));
				helper.assertTrue(part.getBlock() instanceof io.github.jimbozoomer.jugcraft.tower.SlidingDoorPartBlock
						&& io.github.jimbozoomer.jugcraft.tower.SlidingDoorPartBlock.masterPos(other.above(y), part).equals(leaf),
						"the door's cell at " + other.above(y) + ": " + part);
			}
			helper.assertTrue(level.getBlockState(leaf.above()).getBlock() instanceof io.github.jimbozoomer.jugcraft.tower.SlidingDoorPartBlock,
					"the door's cell over its master");
			for (BlockPos l : new BlockPos[] {leaf, other}) {
				for (int y = 2; y <= 4; y++) {
					BlockState over = level.getBlockState(l.above(y));
					// (over a steel door the wall is faced onto the hall since layout 49: the light line, then the faced wall)
					helper.assertTrue(y == 4 ? over.is(io.github.jimbozoomer.jugcraft.test.TowerGameTests.modBlock("steel_armor_plate"))
							: d[1].toString().endsWith("glass") ? over.is(io.github.jimbozoomer.jugcraft.test.TowerGameTests.modBlock("framed_glass"))
							: y == 2 ? over.getBlock() instanceof io.github.jimbozoomer.jugcraft.tower.FacedLineBlock
							: over.getBlock() instanceof io.github.jimbozoomer.jugcraft.tower.FacedWallBlock,
							"the wall whole over the door at " + l.above(y) + ": " + over);
				}
			}
		}
		Object[][] plaques = {{TriPillarArena.SECURITY_PLAQUE, "security"}, {TriPillarArena.CAFE_PLAQUE, "cafeteria"},
				{TriPillarArena.LOCKER_PLAQUE, "locker_room"}, {TriPillarArena.BATTERY_PLAQUE, "battery_room"}};
		for (Object[] q : plaques) {
			BlockPos at = TriPillarArena.at(core, (BlockPos) q[0]);
			BlockState plaque = level.getBlockState(at);
			helper.assertTrue(plaque.is(block("room_sign")) && plaque.toString().contains("label=" + q[1]), "the " + q[1] + " plaque: " + plaque);
			BlockState behind = level.getBlockState(at.relative(plaque.getValue(net.minecraft.world.level.block.HorizontalDirectionalBlock.FACING).getOpposite()));
			helper.assertTrue(!behind.isAir() && !behind.is(io.github.jimbozoomer.jugcraft.test.TowerGameTests.modBlock("framed_glass")), "the " + q[1] + " plaque is on a wall, not glass: " + behind);
		}
		assertFirstFloorHallSide(helper, level, core);
	}

	/**
	 * The upper floor's rooms (work list item 22, part B): the six Pilot Simulators whole, each flying its own scenario;
	 * the boardroom table whole; the upper restrooms gone (layout 38); the bottle filler; the wall screens whole, each
	 * with its display at its anchor pinned to the tower's terminal, the training room's showing the lesson; every room's plaque on a wall (never on glass); the hall's
	 * small table back with its desk row and chair, bearing the tower diorama ({@link #assertModelTable}).
	 */
	static void assertUpperFloorRooms(GameTestHelper helper, ServerLevel level, TowerCoreBlockEntity core) {
		assertUpperFloorGallerySide(helper, level, core);
		for (int i = 0; i < TriPillarArena.PILOT_SIMULATORS.length; i++) {
			BlockPos at = TriPillarArena.at(core, TriPillarArena.PILOT_SIMULATORS[i]);
			RoomKitGameTests.assertWhole(helper, level, at, "pilot_simulator");
			helper.assertTrue(level.getBlockState(at).toString().contains("sim=" + i), "simulator " + i + " flies scenario " + i + ": " + level.getBlockState(at));
		}
		RoomKitGameTests.assertWhole(helper, level, TriPillarArena.at(core, TriPillarArena.BOARDROOM_TABLE), "boardroom_table");
		RoomKitGameTests.assertWhole(helper, level, TriPillarArena.at(core, TriPillarArena.BOTTLE_FILLER), "bottle_filler");
		// no restrooms upstairs any more (layout 38): their floor is open gallery in front of the custodian's closet
		for (BlockPos p : BlockPos.betweenClosed(TriPillarArena.at(core, new BlockPos(-24, 12, -39)), TriPillarArena.at(core, new BlockPos(-12, 13, -32)))) {
			BlockState s = level.getBlockState(p);
			helper.assertTrue(s.isAir() || s.is(block("bottle_filler")), "the upper restrooms are gone, open floor at " + p + ": " + s);
		}
		// the custodian's closet walled over its door (layout 39: Justin, "missing the wall above the door")
		for (int y = 14; y <= 16; y++) {
			BlockState over = level.getBlockState(TriPillarArena.at(core, new BlockPos(-26, y, -40)));
			helper.assertTrue(!over.isAir() && over.isSolid(), "the wall over the custodian's door at y " + y + ": " + over);
		}
		BlockPos terminal = core.terminalPos();
		for (BlockPos p : TriPillarArena.WALL_SCREENS) {
			BlockPos at = TriPillarArena.at(core, p);
			BlockState screen = level.getBlockState(at);
			String id = screen.getBlock() instanceof io.github.jimbozoomer.jugcraft.tower.RoomKitBlock kit ? kit.spec.id() : "?";
			RoomKitGameTests.assertWhole(helper, level, at, id);
			helper.assertTrue(level.getBlockEntity(at) instanceof DepotDisplayBlockEntity display && terminal.equals(display.terminal()),
					"the wall screen at " + p + " is pinned to the tower's terminal");
		}
		BlockState lesson = level.getBlockState(TriPillarArena.at(core, TriPillarArena.LESSON_SCREEN));
		helper.assertTrue(lesson.toString().contains("role=" + io.github.jimbozoomer.jugcraft.tower.RoomKitDisplayBlock.LESSON),
				"the training room's screen shows the lesson: " + lesson);
		for (int i = 0; i < TriPillarArena.UPPER_PLAQUES.length; i++) {
			BlockPos at = TriPillarArena.at(core, TriPillarArena.UPPER_PLAQUES[i]);
			BlockState plaque = level.getBlockState(at);
			String label = TriPillarArena.UPPER_PLAQUE_LABELS[i];
			helper.assertTrue(plaque.is(block("room_sign")) && plaque.toString().contains("label=" + label), "the " + label + " plaque: " + plaque);
			BlockState behind = level.getBlockState(at.relative(plaque.getValue(net.minecraft.world.level.block.HorizontalDirectionalBlock.FACING).getOpposite()));
			helper.assertTrue(!behind.isAir() && !behind.is(io.github.jimbozoomer.jugcraft.test.TowerGameTests.modBlock("framed_glass")), "the " + label + " plaque is on a wall, not glass: " + behind);
		}
		assertModelTable(helper, level, core);
		assertFlatTv(helper, level, core, TriPillarArena.LOUNGE_TV, Direction.EAST, 3, 2);
		// no flight planning (layout 33): none of its pieces, nor its back wall, where it stood; open floor
		java.util.Set<String> planning = java.util.Set.of("dispatch_counter", "wx_kiosk", "chart_table", "chart_bin", "area_chart",
				"notam_board", "strip_board", "zone_clocks", "mfp_copier", "binder_shelf", "steel_filing_cabinet");
		for (BlockPos p : BlockPos.betweenClosed(TriPillarArena.at(core, new BlockPos(10, 12, 8)), TriPillarArena.at(core, new BlockPos(29, 15, 22)))) {
			BlockState s = level.getBlockState(p);
			String id = s.getBlock() instanceof io.github.jimbozoomer.jugcraft.tower.RoomKitBlock kit ? kit.spec.id() : "";
			helper.assertTrue(!planning.contains(id), "no flight planning piece left: " + s + " at " + p);
			helper.assertTrue(!(s.getBlock() instanceof io.github.jimbozoomer.jugcraft.tower.ConsoleDeskBlock), "no planning desk left: " + p);
		}
		for (int x = 12; x < 20; x++) {
			BlockState s = level.getBlockState(TriPillarArena.at(core, new BlockPos(x, 13, 22)));
			helper.assertTrue(s.isAir(), "flight planning's back wall is gone at x " + x + ": " + s);
		}
		for (int y = 13; y <= 14; y++) {
			for (int z = 36; z <= 38; z++) {
				BlockState s = level.getBlockState(TriPillarArena.at(core, new BlockPos(18, y, z)));
				helper.assertTrue(!s.is(block("ops_wall_screen")), "no ops wall screen over the lounge's media console: " + s);
			}
		}
		// the training room closed at both ends (layout 38): the north return wall from the front wall to the curtain
		// wall, the front wall on to the south-west offices' wall; the window bay at the south end carpeted, in the room
		for (int y = 12; y <= 15; y++) {
			for (int x = -41; x <= -38; x++) {
				BlockState s = level.getBlockState(TriPillarArena.at(core, new BlockPos(x, y, -17)));
				helper.assertTrue(walled(s), "the training room's north end walled at x " + x + ", y " + y + ": " + s);
			}
			for (int z = 18; z <= 22; z++) {
				BlockState s = level.getBlockState(TriPillarArena.at(core, new BlockPos(-38, y, z)));
				helper.assertTrue(walled(s), "the training room's front wall runs on at z " + z + ", y " + y + ": " + s);
			}
		}
		BlockState bay = level.getBlockState(TriPillarArena.at(core, new BlockPos(-40, 11, 19)));
		helper.assertTrue(bay.is(block("carpet_tile")), "the window bay is the training room's: " + bay);
		// the gallery round the simulator room's cut-back corner (fix 2): open gallery floor from the north side to the east,
		// and (layout 39) on down the room's east side, its glass one straight run at x -18 with nothing stepping out
		for (int[] c : new int[][] {{-16, 5}, {-16, 8}, {-15, 10}, {-13, 11}, {-12, 12}, {-17, 13}, {-15, 16}, {-14, 21}}) {
			BlockPos floor = TriPillarArena.at(core, new BlockPos(c[0], 11, c[1]));
			helper.assertTrue(level.getBlockState(floor).is(block("access_floor_tile")) && level.getBlockState(floor.above()).isAir()
					&& level.getBlockState(floor.above(2)).isAir(), "the gallery runs round the simulator room at " + floor);
		}
		for (int z = 6; z <= 21; z++) {
			BlockState glass = level.getBlockState(TriPillarArena.at(core, new BlockPos(-18, 14, z)));
			helper.assertTrue(glass.is(io.github.jimbozoomer.jugcraft.test.TowerGameTests.modBlock("framed_glass")) || glass.getBlock() instanceof net.minecraft.world.level.block.DoorBlock,
					"the simulator room's east glass runs straight at z " + z + ": " + glass);
		}
		for (BlockPos p : BlockPos.betweenClosed(TriPillarArena.at(core, new BlockPos(-34, 12, 6)), TriPillarArena.at(core, new BlockPos(-19, 12, 21)))) {
			helper.assertTrue(!level.getBlockState(p).is(block("waiting_bench")), "no waiting bench in the simulator room: " + p);
		}
	}

	/**
	 * A Flat Screen TV whole on its wall, {@code w} by {@code h}, its back to {@code facing}, with its controller (the
	 * bottom-left block as its viewer sees it) at {@code place}: every block of it says which ways the screen carries on,
	 * and the controller knows the screen's size (tv/FlatTvBlock, tv/TvBlockEntity).
	 */
	static void assertFlatTv(GameTestHelper helper, ServerLevel level, TowerCoreBlockEntity core, BlockPos place, Direction facing, int w, int h) {
		assertFlatTvAt(helper, level, TriPillarArena.at(core, place), facing, w, h, false);
	}

	/** {@link #assertFlatTv} with its controller at the world position {@code at}, hung ({@code stand} false) or standing. */
	static void assertFlatTvAt(GameTestHelper helper, ServerLevel level, BlockPos at, Direction facing, int w, int h, boolean stand) {
		Direction right = facing.getClockWise();
		for (int c = 0; c < w; c++) {
			for (int r = 0; r < h; r++) {
				BlockPos p = at.relative(right, c).above(r);
				BlockState s = level.getBlockState(p);
				helper.assertTrue(s.getBlock() instanceof io.github.jimbozoomer.jugcraft.tv.FlatTvBlock
						&& s.getValue(io.github.jimbozoomer.jugcraft.tv.FlatTvBlock.FACING) == facing
						&& s.getValue(io.github.jimbozoomer.jugcraft.tv.FlatTvBlock.LEFT) == (c > 0)
						&& s.getValue(io.github.jimbozoomer.jugcraft.tv.FlatTvBlock.RIGHT) == (c < w - 1)
						&& s.getValue(io.github.jimbozoomer.jugcraft.tv.FlatTvBlock.DOWN) == (r > 0)
						&& s.getValue(io.github.jimbozoomer.jugcraft.tv.FlatTvBlock.UP) == (r < h - 1)
						&& s.getValue(io.github.jimbozoomer.jugcraft.tv.FlatTvBlock.STAND) == stand, "the flat screen TV's block " + c + ", " + r + ": " + s);
			}
		}
		helper.assertTrue(level.getBlockEntity(at) instanceof io.github.jimbozoomer.jugcraft.tv.TvBlockEntity tv && tv.isController()
				&& tv.screenWidth() == w && tv.screenHeight() == h, "the flat screen TV at " + at + " is one " + w + " x " + h + " screen");
	}

	/**
	 * The atrium dome (layout 43, tools/atrium_dome.py): every Dome Shell cell where the data puts it, with its index; the
	 * feet's cells inside the sphere open (over the rotunda, under the feet's old soffits); the riser ladders up the feet's
	 * outer corners from the ground through the roof.
	 */
	static void assertAtriumDome(GameTestHelper helper, ServerLevel level, TowerCoreBlockEntity core) {
		io.github.jimbozoomer.jugcraft.tower.DomeShellData d = io.github.jimbozoomer.jugcraft.tower.DomeShellData.get();
		helper.assertTrue(d.count > 600, "the dome's data is there: " + d.count + " cells");
		for (int i = 0; i < d.count; i++) {
			BlockState s = level.getBlockState(core.getBlockPos().offset(d.x(i), d.y(i), d.z(i)));
			helper.assertTrue(s.getBlock() instanceof io.github.jimbozoomer.jugcraft.tower.DomeShellBlock
					&& s.getValue(io.github.jimbozoomer.jugcraft.tower.DomeShellBlock.INDEX) == i, "the dome's cell " + i + ": " + s);
		}
		for (BlockPos open : new BlockPos[] {new BlockPos(0, 15, 10), new BlockPos(8, 14, -4), new BlockPos(-8, 14, -4), new BlockPos(0, 12, 13)}) {
			helper.assertTrue(level.getBlockState(core.getBlockPos().offset(open)).isAir(), "inside the dome is open at " + open);
		}
		for (BlockPos foot : new BlockPos[] {TriPillarArena.RISER_LADDER, new BlockPos(21, 1, -14), new BlockPos(-6, 1, 23)}) {
			BlockPos at = TriPillarArena.at(core, foot);
			for (int y = 0; y < 20; y++) {
				helper.assertTrue(level.getBlockState(at.above(y)).is(io.github.jimbozoomer.jugcraft.nuclear.StorageTanks.LADDER),
						"the riser ladder up the foot's outer corner at " + at.above(y));
			}
		}
	}

	/**
	 * The wall panel runs' end corners turned outwards, mirrored (layout 42, Justin: "you're putting the corner ones down
	 * in one singular direction"): the intake face's (facing east) north end east, its south end south; the intake room's
	 * glass wall's (facing west) north end north, its south end west. And the intake room's two ends alike, dark beside
	 * the exchanges.
	 */
	static void assertPanelEnds(GameTestHelper helper, ServerLevel level, TowerCoreBlockEntity core) {
		int[][] ends = {{45, -10}, {45, 10}, {37, 1}, {37, 5}};
		String[] facing = {"east", "south", "north", "west"};
		for (int k = 0; k < ends.length; k++) {
			for (int y = 2; y <= 6; y++) {
				BlockState s = level.getBlockState(core.getBlockPos().offset(ends[k][0], y, ends[k][1]));
				helper.assertTrue(s.toString().contains("wall_panel") && s.toString().contains("shape=corner") && s.toString().contains("facing=" + facing[k]),
						"the panel run's end at x " + ends[k][0] + ", z " + ends[k][1] + " turns out (" + facing[k] + "): " + s);
			}
		}
		for (int z : new int[] {-10, 10}) {
			for (int x = 40; x <= 44; x++) {
				BlockState s = level.getBlockState(core.getBlockPos().offset(x, 3, z));
				helper.assertTrue(s.toString().contains("carbon_composite_panel"), "the intake room's end at z " + z + " dark by the exchange at x " + x + ": " + s);
			}
		}
	}

	/**
	 * The hall's tower diorama (layout 29, redesign 3; its own case since layout 44): the Tower Diorama Case whole, its
	 * anchor in the middle drawing the model (client/TowerDiorama), its desk row in front working the tower's build and
	 * the operator's chair behind it.
	 */
	static void assertModelTable(GameTestHelper helper, ServerLevel level, TowerCoreBlockEntity core) {
		BlockPos anchor = TriPillarArena.at(core, TriPillarArena.MODEL_TABLE);
		// the Tower Diorama Case (layout 44): whole, its anchor the middle of the 3 x 3 (its block entity draws the model)
		RoomKitGameTests.assertWhole(helper, level, anchor, "tower_diorama_case");
		helper.assertTrue(level.getBlockEntity(anchor) instanceof io.github.jimbozoomer.jugcraft.tower.RoomKitBlockEntity,
				"the diorama case's anchor has its block entity (it draws the model)");
		for (int dx = -1; dx <= 1; dx++) {
			for (int dz = -1; dz <= 1; dz++) {
				for (int dy = 0; dy < 4; dy++) {
					helper.assertTrue(level.getBlockState(anchor.offset(dx, dy, dz)).getBlock() instanceof io.github.jimbozoomer.jugcraft.tower.RoomKitBlock,
							"the diorama case fills its 3 x 3 x 4 at " + anchor.offset(dx, dy, dz));
				}
			}
		}
		for (int k = 0; k < 3; k++) {
			BlockState desk = level.getBlockState(TriPillarArena.at(core, TriPillarArena.MODEL_DESK.offset(k, 0, 0)));
			helper.assertTrue(desk.is(io.github.jimbozoomer.jugcraft.test.TowerGameTests.modBlock("console_desk"))
					&& desk.getValue(io.github.jimbozoomer.jugcraft.tower.ConsoleDeskBlock.ROLE) == 5, "the small table's desk " + (k + 1) + " works the build: " + desk);
		}
		BlockState chair = level.getBlockState(TriPillarArena.at(core, TriPillarArena.MODEL_CHAIR));
		helper.assertTrue(chair.is(io.github.jimbozoomer.jugcraft.test.TowerGameTests.modBlock("operator_chair")), "the small table's chair: " + chair);
	}

	/**
	 * A shower stall (work list item 22): you walk in between its partitions; turned on it runs (the water, the lever
	 * up), puts out whatever burns in it, and turns off by hand or by itself when its minute is up.
	 */
	@GameTest(structure = "jugcraft-test:drone_depot")
	public void showerStallRunsAndPutsOutFire(GameTestHelper helper) {
		if (TestFocus.skip("tower")) {
			helper.succeed();
			return;
		}
		ServerLevel level = helper.getLevel();
		var shower = (io.github.jimbozoomer.jugcraft.nuclear.RestroomBlocks.Shower) block("shower_stall");
		BlockPos at = helper.absolutePos(new BlockPos(7, 1, 7));
		level.setBlock(at, shower.defaultBlockState().setValue(net.minecraft.world.level.block.HorizontalDirectionalBlock.FACING, Direction.EAST),
				net.minecraft.world.level.block.Block.UPDATE_ALL);
		var shape = level.getBlockState(at).getCollisionShape(level, at);
		helper.assertTrue(!shape.isEmpty() && shape.max(Direction.Axis.Y) > 1.4, "its partitions collide, taller than a block: " + shape);
		var middle = new net.minecraft.world.phys.AABB(0.3, 0.1, 0.2, 0.7, 1.4, 0.8);
		helper.assertTrue(shape.toAabbs().stream().noneMatch(b -> b.intersects(middle)), "its middle is clear to stand in: " + shape);
		var pig = helper.spawnWithNoFreeWill(net.minecraft.world.entity.EntityTypes.PIG, new Vec3(7.5, 1.1, 7.5));
		pig.igniteForSeconds(10);
		helper.assertTrue(pig.isOnFire(), "the pig burns");
		shower.start(level, at, level.getBlockState(at));
		helper.assertTrue(level.getBlockState(at).getValue(io.github.jimbozoomer.jugcraft.nuclear.RestroomBlocks.RUNNING), "the shower runs");
		level.getBlockState(at).tick(level, at, level.getRandom());
		helper.assertTrue(!pig.isOnFire(), "the shower put the pig out");
		shower.stop(level, at, level.getBlockState(at));
		helper.assertTrue(!level.getBlockState(at).getValue(io.github.jimbozoomer.jugcraft.nuclear.RestroomBlocks.RUNNING), "turned off by hand");
		shower.start(level, at, level.getBlockState(at));
		io.github.jimbozoomer.jugcraft.nuclear.RestroomBlocks.runFor(level, at, 0);
		level.getBlockState(at).tick(level, at, level.getRandom());
		helper.assertTrue(!level.getBlockState(at).getValue(io.github.jimbozoomer.jugcraft.nuclear.RestroomBlocks.RUNNING), "off by itself when its time is up");
		helper.succeed();
	}

	/**
	 * A Maintenance Door pair (work list item 10): its badge reader keeps out a stranger from outside, lets its owner in;
	 * either leaf opens both, the closers shut both; the hooded upper half lights the doorstep; its leaves swing as part
	 * models 33 and 35 (nuclear/PlantDoors leafPart).
	 */
	@GameTest(maxTicks = 300)
	public void maintenanceDoorPairOpensTogetherForItsOwner(GameTestHelper helper) {
		if (TestFocus.skip("tower")) {
			helper.succeed();
			return;
		}
		ServerLevel level = helper.getLevel();
		var door = io.github.jimbozoomer.jugcraft.tower.JugcraftTower.MAINTENANCE_DOOR;
		var east = net.minecraft.core.Direction.EAST;
		BlockPos left = helper.absolutePos(new BlockPos(2, 1, 2)), right = left.south();
		for (BlockPos foot : new BlockPos[] {left, right}) {
			var hinge = foot.equals(left) ? net.minecraft.world.level.block.state.properties.DoorHingeSide.LEFT
					: net.minecraft.world.level.block.state.properties.DoorHingeSide.RIGHT;
			BlockState lower = door.defaultBlockState().setValue(net.minecraft.world.level.block.DoorBlock.FACING, east)
					.setValue(net.minecraft.world.level.block.DoorBlock.HINGE, hinge)
					.setValue(io.github.jimbozoomer.jugcraft.nuclear.PlantDoorBlock.READER, true)
					.setValue(io.github.jimbozoomer.jugcraft.tower.MaintenanceDoorBlock.HOOD, true);
			level.setBlock(foot, lower, net.minecraft.world.level.block.Block.UPDATE_ALL);
			level.setBlock(foot.above(), lower.setValue(net.minecraft.world.level.block.DoorBlock.HALF,
					net.minecraft.world.level.block.state.properties.DoubleBlockHalf.UPPER), net.minecraft.world.level.block.Block.UPDATE_ALL);
		}
		Player owner = helper.makeMockPlayer(GameType.SURVIVAL);
		Player stranger = helper.makeMockPlayer(GameType.SURVIVAL);
		for (BlockPos foot : new BlockPos[] {left, right}) {
			((io.github.jimbozoomer.jugcraft.nuclear.PlantDoorBlockEntity) level.getBlockEntity(foot)).setOwner(owner.getUUID());
		}
		for (Player p : new Player[] {owner, stranger}) {
			p.setPos(left.getX() - 1.5, left.getY(), left.getZ() + 1.0);
		}
		var hit = new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(left), net.minecraft.core.Direction.WEST, left, false);
		java.util.function.Predicate<BlockPos> open = foot -> level.getBlockState(foot).getValue(net.minecraft.world.level.block.DoorBlock.OPEN);
		helper.startSequence()
				.thenExecute(() -> {
					for (BlockPos foot : new BlockPos[] {left, right}) {
						BlockState s = level.getBlockState(foot);
						helper.assertTrue(s.is(door) && s.getValue(io.github.jimbozoomer.jugcraft.nuclear.PlantDoorBlock.PAIRED), "the leaves are a pair: " + s);
					}
					helper.assertTrue(io.github.jimbozoomer.jugcraft.nuclear.PlantDoors.leafPart(level.getBlockState(left)) == 33
							&& io.github.jimbozoomer.jugcraft.nuclear.PlantDoors.leafPart(level.getBlockState(right)) == 35,
							"the leaves swing as part models 33 and 35");
					helper.assertTrue(level.getBlockState(left.above()).getLightEmission() == io.github.jimbozoomer.jugcraft.tower.MaintenanceDoorBlock.HOOD_LIGHT
							&& level.getBlockState(left).getLightEmission() == 0, "the hood's downlight lights the doorstep");
					level.getBlockState(left).useWithoutItem(level, stranger, hit);
					helper.assertTrue(!open.test(left) && !open.test(right), "the badge reader keeps a stranger out");
					level.getBlockState(left).useWithoutItem(level, owner, hit);
					helper.assertTrue(open.test(left) && open.test(right), "its owner opens the pair: both leaves swing");
				})
				.thenWaitUntil(() -> helper.assertTrue(!open.test(left) && !open.test(right)
						&& !level.getBlockState(left).getValue(io.github.jimbozoomer.jugcraft.nuclear.PlantDoorBlock.MOVING),
						"the closers shut the pair again"))
				.thenSucceed();
	}

	/**
	 * Console Desks set down side by side, facing the same way, join into one bench desk (work list item 14): the ends'
	 * monitors turned in, the middle desk's keyboard; sneaking and using one with an empty hand turns its whole row to the
	 * next role; taking the middle one away leaves two desks on their own.
	 */
	@GameTest
	public void consoleDesksJoinIntoOneBenchAndChangeRoleAsARow(GameTestHelper helper) {
		if (TestFocus.skip("tower")) {
			helper.succeed();
			return;
		}
		ServerLevel level = helper.getLevel();
		var block = io.github.jimbozoomer.jugcraft.test.TowerGameTests.modBlock("console_desk");
		var desk = io.github.jimbozoomer.jugcraft.tower.ConsoleDeskBlock.class;
		BlockState north = block.defaultBlockState().setValue(net.minecraft.world.level.block.HorizontalDirectionalBlock.FACING, net.minecraft.core.Direction.NORTH);
		BlockPos a = helper.absolutePos(new BlockPos(2, 1, 2));
		for (int k = 0; k < 3; k++) {
			level.setBlock(a.east(k), north, net.minecraft.world.level.block.Block.UPDATE_ALL);
		}
		helper.startSequence()
				.thenWaitUntil(() -> {
					for (int k = 0; k < 3; k++) {
						BlockState s = level.getBlockState(a.east(k));
						helper.assertTrue(s.getValue(io.github.jimbozoomer.jugcraft.tower.ConsoleDeskBlock.LEFT) == (k > 0)
								&& s.getValue(io.github.jimbozoomer.jugcraft.tower.ConsoleDeskBlock.RIGHT) == (k < 2), "desk " + (k + 1) + " joined: " + s);
					}
				})
				.thenExecute(() -> {
					helper.assertTrue(io.github.jimbozoomer.jugcraft.tower.ConsoleDeskBlock.angle(false, true) > 0
							&& io.github.jimbozoomer.jugcraft.tower.ConsoleDeskBlock.angle(true, false) < 0
							&& io.github.jimbozoomer.jugcraft.tower.ConsoleDeskBlock.angle(true, true) == 0, "the ends' monitors turn in, the middle's faces ahead");
					Player player = helper.makeMockPlayer(GameType.SURVIVAL);
					player.setShiftKeyDown(true);
					BlockPos middle = a.east(1);
					level.getBlockState(middle).useWithoutItem(level, player,
							new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(middle), net.minecraft.core.Direction.UP, middle, false));
					for (int k = 0; k < 3; k++) {
						BlockState s = level.getBlockState(a.east(k));
						helper.assertTrue(desk.isInstance(s.getBlock()) && s.getValue(io.github.jimbozoomer.jugcraft.tower.ConsoleDeskBlock.ROLE) == 1,
								"the whole row turns to the next role: desk " + (k + 1) + " " + s);
					}
					helper.assertTrue(!level.getBlockState(middle).getShape(level, middle).isEmpty(), "a desk has its hitbox");
					level.removeBlock(middle, false);
				})
				.thenWaitUntil(() -> {
					BlockState left = level.getBlockState(a), right = level.getBlockState(a.east(2));
					helper.assertTrue(!left.getValue(io.github.jimbozoomer.jugcraft.tower.ConsoleDeskBlock.RIGHT)
							&& !right.getValue(io.github.jimbozoomer.jugcraft.tower.ConsoleDeskBlock.LEFT), "with the middle gone, each end stands alone");
					helper.assertTrue(io.github.jimbozoomer.jugcraft.tower.ConsoleDeskBlock.page(left) == 1, "a desk on its own shows its role's middle page");
				})
				.thenSucceed();
	}

	/**
	 * The workshop's parts wall (work list item 9): two Drone Parts Shelving units facing south to the service bay, stocked
	 * a and b, each whole (its 8 parts round and over its master), each cell colliding with the shelving's 0.62 depth against
	 * its back edge, the top row only up to 2.30; no storage rack left there.
	 */
	static void assertPartsShelving(GameTestHelper helper, ServerLevel level, TowerCoreBlockEntity core) {
		var block = io.github.jimbozoomer.jugcraft.tower.DronePartsShelvingBlock.class;
		for (int u = 0; u < 2; u++) {
			BlockPos master = TriPillarArena.at(core, TriPillarArena.PARTS_SHELVING[u]);
			BlockState shelving = level.getBlockState(master);
			helper.assertTrue(block.isInstance(shelving.getBlock())
					&& shelving.getValue(io.github.jimbozoomer.jugcraft.tower.DronePartsShelvingBlock.FACING) == net.minecraft.core.Direction.SOUTH
					&& shelving.getValue(io.github.jimbozoomer.jugcraft.tower.DronePartsShelvingBlock.STOCK)
							== io.github.jimbozoomer.jugcraft.tower.DronePartsShelvingBlock.Stock.values()[u],
					"a Drone Parts Shelving (stock " + (u == 0 ? "a" : "b") + ") facing south at " + TriPillarArena.PARTS_SHELVING[u] + ": " + shelving);
			var footprint = io.github.jimbozoomer.jugcraft.tower.DronePartsShelvingBlock.footprint(net.minecraft.core.Direction.SOUTH);
			helper.assertTrue(footprint.size() == 9, "the shelving covers 3 cells across and 3 high");
			for (net.minecraft.core.Vec3i o : footprint) {
				BlockPos at = master.offset(o);
				BlockState cell = level.getBlockState(at);
				if (!o.equals(net.minecraft.core.Vec3i.ZERO)) {
					helper.assertTrue(cell.getBlock() instanceof io.github.jimbozoomer.jugcraft.tower.DronePartsShelvingPartBlock
							&& io.github.jimbozoomer.jugcraft.tower.DronePartsShelvingPartBlock.masterPos(at, cell).equals(master),
							"the shelving has its part " + o + ": " + cell);
				}
				var shape = cell.getCollisionShape(level, at);
				double top = o.getY() == 2 ? 0.30 : 1.0;
				helper.assertTrue(!shape.isEmpty() && Math.abs(shape.max(net.minecraft.core.Direction.Axis.Y) - top) < 1e-6
						&& shape.min(net.minecraft.core.Direction.Axis.Z) < 0.05 && Math.abs(shape.max(net.minecraft.core.Direction.Axis.Z) - 0.645) < 1e-6,
						"the shelving's cell " + o + " collides with its depth against the back, " + top + " high: " + shape);
				helper.assertTrue(!cell.is(io.github.jimbozoomer.jugcraft.test.TowerGameTests.modBlock("storage_rack")), "no storage rack left on the parts wall at " + at);
			}
		}
	}

	static void assertCrystalTree(GameTestHelper helper, ServerLevel level, TowerCoreBlockEntity core) {
		BlockPos master = TriPillarArena.at(core, TriPillarArena.CRYSTAL_TREE);
		helper.assertTrue(level.getBlockState(master).getBlock() instanceof io.github.jimbozoomer.jugcraft.tower.CrystalTreeBlock,
				"the Crystal Tree at " + TriPillarArena.CRYSTAL_TREE + ": " + level.getBlockState(master));
		for (net.minecraft.core.Vec3i o : io.github.jimbozoomer.jugcraft.tower.CrystalTreeBlock.FOOTPRINT) {
			if (o.equals(net.minecraft.core.Vec3i.ZERO)) continue;
			BlockState part = level.getBlockState(master.offset(o));
			helper.assertTrue(part.getBlock() instanceof io.github.jimbozoomer.jugcraft.tower.CrystalTreePartBlock
					&& io.github.jimbozoomer.jugcraft.tower.CrystalTreePartBlock.masterPos(master.offset(o), part).equals(master),
					"the tree has its part " + o + ": " + part);
		}
		BlockPos trunk = master.above(2), wall = master.offset(3, 0, 0), crown = master.offset(2, 5, 0);
		helper.assertTrue(!level.getBlockState(trunk).getCollisionShape(level, trunk).isEmpty(), "its trunk is solid");
		helper.assertTrue(level.getBlockState(wall).getCollisionShape(level, wall).max(net.minecraft.core.Direction.Axis.Y) > 0.8,
				"its planter's wall stands 0.88 high");
		helper.assertTrue(level.getBlockState(crown).isAir(), "its crown is not solid");
		helper.assertTrue(level.getBlockState(TriPillarArena.at(core, TriPillarArena.OLD_PLANTER_TRUNK)).isAir(), "the small planters are gone");
	}

	static void assertDisplayCases(GameTestHelper helper, ServerLevel level, TowerCoreBlockEntity core) {
		for (int i = 0; i < TriPillarArena.DISPLAY_CASES.length; i++) {
			BlockPos master = TriPillarArena.at(core, TriPillarArena.DISPLAY_CASES[i]);
			BlockState c = level.getBlockState(master);
			helper.assertTrue(c.getBlock() instanceof io.github.jimbozoomer.jugcraft.tower.DroneDisplayCaseBlock
					&& c.getValue(io.github.jimbozoomer.jugcraft.tower.DroneDisplayCaseBlock.TIER) == TriPillarArena.DISPLAY_CASE_TIERS[i]
					&& c.getValue(io.github.jimbozoomer.jugcraft.tower.DroneDisplayCaseBlock.FACING) == net.minecraft.core.Direction.NORTH,
					"a display case showing T" + TriPillarArena.DISPLAY_CASE_TIERS[i] + " at " + TriPillarArena.DISPLAY_CASES[i] + ": " + c);
			for (net.minecraft.core.Vec3i o : io.github.jimbozoomer.jugcraft.tower.DroneDisplayCaseBlock.FOOTPRINT) {
				if (o.equals(net.minecraft.core.Vec3i.ZERO)) continue;
				BlockState part = level.getBlockState(master.offset(o));
				helper.assertTrue(part.getBlock() instanceof io.github.jimbozoomer.jugcraft.tower.DroneDisplayCasePartBlock
						&& io.github.jimbozoomer.jugcraft.tower.DroneDisplayCasePartBlock.masterPos(master.offset(o), part).equals(master),
						"the case at " + TriPillarArena.DISPLAY_CASES[i] + " has its part " + o + ": " + part);
			}
			BlockPos glass = master.offset(0, 1, -2), inside = master.offset(0, 1, 0);
			helper.assertTrue(!level.getBlockState(glass).getCollisionShape(level, glass).isEmpty()
					&& level.getBlockState(inside).getCollisionShape(level, inside).isEmpty(), "its glass is solid, the room under it clear");
		}
	}

	/**
	 * UPGRADE starts the foundation, which the core builds itself: the depot terminal with its ground pad, pickup and
	 * packager, the central holo table over the core and the master console. A second UPGRADE starts tier 1, and the
	 * core lays the roof pads: the tower is tier 1 and its terminal holds 24 drones.
	 */
	@GameTest(structure = TriPillarArena.ARENA, maxTicks = 1600, skyAccess = true)
	public void towerCoreBuildsTheFoundationThenTheRoofPads(GameTestHelper helper) {
		if (TestFocus.skip("tower")) {
			helper.succeed();
			return;
		}
		Player owner = helper.makeMockPlayer(GameType.CREATIVE);
		ServerLevel level = helper.getLevel();
		TowerCoreBlockEntity core = TriPillarArena.placeCore(helper);
		TriPillarArena.stock(core);
		helper.assertTrue(core.needsFoundation() && core.nextStep() == core.layout().foundation, "a new core builds its foundation first");
		core.tryUpgrade(owner);
		helper.assertTrue(core.foundationState() == 1 && core.buildingFoundation() && core.building() == 0,
				"UPGRADE started the foundation, not a tier");
		helper.startSequence()
				.thenWaitUntil(() -> helper.assertTrue(core.foundationState() == 2, "the foundation is built (progress " + core.buildProgress() + ")"))
				.thenExecute(() -> {
					helper.assertTrue(core.tier() == 0 && core.building() == 0, "the foundation is not a tier");
					DroneTerminalBlockEntity terminal = core.terminal();
					helper.assertTrue(terminal != null && terminal.getBlockPos().equals(TriPillarArena.at(core, TriPillarArena.TERMINAL)),
							"dispatch has its depot terminal");
					helper.assertTrue(owner.getUUID().equals(terminal.owner()), "the tower's owner owns the terminal");
					terminal.rescanNow();
					// the terminal finds the platform on the lift house through the tower (no pad of its own: the tower's drones
					// dock in its bays), and its drones can work from it
					helper.assertTrue(terminal.layout().status() == io.github.jimbozoomer.jugcraft.drone.PlatformLayout.Status.NO_PAD
							&& terminal.layout().pads().isEmpty() && terminal.layout().width() == 3 && terminal.layout().depth() == 3
							&& terminal.platformReady(),
							"the terminal found the lift house's 3x3 landing platform, got " + terminal.layout().status() + " "
									+ terminal.layout().width() + "x" + terminal.layout().depth());
					BlockPos pickup = TriPillarArena.at(core, TriPillarArena.ROOF_PICKUP).offset(-1, 0, -1);
					List<int[]> pickups = terminal.activePickups();
					helper.assertTrue(pickups.size() == 1 && pickups.get(0)[0] == pickup.getX() && pickups.get(0)[1] == pickup.getY()
							&& pickups.get(0)[2] == pickup.getZ(), "the roof's supply pickup is the depot's pickup");
					helper.assertTrue(terminal.packagerEntity() != null
							&& terminal.packagerEntity().getBlockPos().equals(TriPillarArena.at(core, TriPillarArena.PACKAGER)),
							"the packager under the cargo hall's floor is the depot's store (the goods lift carries its cases up)");
					BlockPos tablePos = TriPillarArena.at(core, TriPillarArena.CENTRAL_TABLE);
					BlockState table = level.getBlockState(tablePos);
					helper.assertTrue(table.is(JugcraftTower.HOLO_COMMAND_TABLE) && table.getValue(HoloCommandTableBlock.PART) == 0
							&& ((HoloCommandTableBlock) JugcraftTower.HOLO_COMMAND_TABLE).complete(level, tablePos),
							"the Holo Command Table stands whole over the core: " + table);
					BlockPos consolePos = TriPillarArena.at(core, TriPillarArena.MASTER_CONSOLE);
					BlockState console = level.getBlockState(consolePos);
					var consoleBlock = (io.github.jimbozoomer.jugcraft.tower.TowerControlConsoleBlock) JugcraftTower.TOWER_CONTROL_CONSOLE;
					helper.assertTrue(console.is(consoleBlock) && console.getValue(io.github.jimbozoomer.jugcraft.tower.TowerControlConsoleBlock.PART) == 0
							&& consoleBlock.complete(level, consolePos, net.minecraft.core.Direction.NORTH),
							"the hall has its Tower Control Console, whole: " + console);
					for (BlockPos chair : TriPillarArena.CONSOLE_CHAIRS) {
						BlockState c = level.getBlockState(TriPillarArena.at(core, chair));
						helper.assertTrue(c.getBlock() instanceof io.github.jimbozoomer.jugcraft.tower.ConsoleChairBlock
								&& c.getValue(io.github.jimbozoomer.jugcraft.tower.ConsoleChairBlock.PLATFORM), "a console chair on the raised floor at " + chair + ": " + c);
					}
					// the rotunda cut (item 19) and its video walls (item 7)
					for (BlockPos wallPos : TriPillarArena.VIDEO_WALLS) {
						BlockPos master = TriPillarArena.at(core, wallPos);
						BlockState wall = level.getBlockState(master);
						helper.assertTrue(wall.getBlock() instanceof io.github.jimbozoomer.jugcraft.tower.CurvedVideoWallBlock, "a video wall at " + wallPos + ": " + wall);
						int aim = wall.getValue(io.github.jimbozoomer.jugcraft.tower.CurvedVideoWallBlock.AIM);
						int[] v = io.github.jimbozoomer.jugcraft.tower.CurvedVideoWallBlock.AIMS[aim];
						helper.assertTrue(master.getX() + v[0] == core.getBlockPos().getX() && master.getZ() + v[1] == core.getBlockPos().getZ(), "the wall at " + wallPos + " is aimed at the core");
						for (net.minecraft.core.Vec3i o : io.github.jimbozoomer.jugcraft.tower.CurvedVideoWallBlock.footprint(aim)) {
							if (o.equals(net.minecraft.core.Vec3i.ZERO)) continue;
							BlockState part = level.getBlockState(master.offset(o));
							helper.assertTrue(part.getBlock() instanceof io.github.jimbozoomer.jugcraft.tower.CurvedVideoWallPartBlock
									&& io.github.jimbozoomer.jugcraft.tower.CurvedVideoWallPartBlock.masterPos(master.offset(o), part).equals(master),
									"the wall at " + wallPos + " has its part " + o + ": " + part);
						}
					}
					assertDisplayCases(helper, level, core);
					assertCrystalTree(helper, level, core);
					assertOpsTrees(helper, level, core);
					assertLounge(helper, level, core);
					assertServiceBay(helper, level, core);
					assertPartsShelving(helper, level, core);
					assertDataHall(helper, level, core);
					assertDeskRows(helper, level, core);
					assertHoloProjector(helper, level, core);
					assertMaintenanceDoors(helper, level, core);
					assertNeonLines(helper, level, core);
					assertCanopyPosts(helper, level, core);
					assertMapRoom(helper, level, core);
					assertGroundFloorRooms(helper, level, core);
					assertUpperFloorRooms(helper, level, core);
					assertWallsToCeiling(helper, level, core);
					BlockPos oldLadder = TriPillarArena.at(core, TriPillarArena.OLD_RISER_LADDER);
					for (int y = 4; y <= 17; y++) {
						helper.assertTrue(level.getBlockState(oldLadder.above(y)).isAir(),
								"the rotunda cut and the atrium dome take the old inner riser ladder: open at height " + (y + 1));
					}
					assertAtriumDome(helper, level, core);
					helper.assertTrue(level.getBlockState(TriPillarArena.at(core, TriPillarArena.CUT_EDGE)).is(io.github.jimbozoomer.jugcraft.test.TowerGameTests.modBlock("neon_line_concrete")),
							"the slab edge runs on its neon line, in concrete, over the cut");
					BlockState step = level.getBlockState(TriPillarArena.at(core, TriPillarArena.FLOOR_BACK_STEP));
					helper.assertTrue(step.getBlock() instanceof io.github.jimbozoomer.jugcraft.tower.RaisedFloorBlock
							&& step.getValue(io.github.jimbozoomer.jugcraft.tower.RaisedFloorBlock.NORTH)
							&& !step.getValue(io.github.jimbozoomer.jugcraft.tower.RaisedFloorBlock.SOUTH), "the raised floor steps down at its back only: " + step);
					helper.assertTrue(core.nextStep() == core.layout().tier(1), "next comes tier 1");
					core.tryUpgrade(owner);
					helper.assertTrue(core.building() == 1, "the second UPGRADE starts tier 1, the roof pads");
				})
				.thenWaitUntil(() -> helper.assertTrue(core.tier() == 1, "tier 1 built (progress " + core.buildProgress() + ")"))
				.thenExecute(() -> {
					DroneTerminalBlockEntity terminal = core.terminal();
					helper.assertTrue(terminal != null && terminal.tower() == core, "the terminal belongs to the tower");
					helper.assertTrue(terminal.maxTier() == 1 && terminal.maxDrones() == 24,
							"a tier 1 tower flies 24 T1 drones, got " + terminal.maxDrones() + " of T" + terminal.maxTier());
					TowerData.Tier roof = core.layout().tier(1);
					helper.assertTrue(TriPillarArena.countPlaced(level, core, roof) == roof.place.length, "every roof pad block is in");
				})
				.thenSucceed();
	}

	/**
	 * During the first layer (the foundation built, tier 1 not started), a block it still needs, used on the tower's
	 * terminal, goes into the core and is handled there (it never falls through to the screen); one it doesn't need is
	 * left alone.
	 */
	@GameTest(structure = TriPillarArena.ARENA, maxTicks = 100, skyAccess = true)
	public void blocksOnTheTerminalLoadTheTower(GameTestHelper helper) {
		if (TestFocus.skip("tower")) {
			helper.succeed();
			return;
		}
		Player owner = helper.makeMockPlayer(GameType.CREATIVE);
		TowerCoreBlockEntity core = TriPillarArena.build(helper, owner, 0);
		BlockPos terminal = core.terminalPos();
		helper.assertTrue(terminal.equals(TriPillarArena.at(core, TriPillarArena.TERMINAL)) && core.terminal() != null, "the dispatch terminal");
		helper.assertTrue(core.firstLayer(), "the foundation alone: still the first layer");
		// tier 1's blocks (its hangar pads) are what the first layer still needs
		var concrete = core.firstLayerCost().keySet().iterator().next();
		ItemStack stack = new ItemStack(concrete, 5);
		owner.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, stack);
		int before = core.stored(concrete);
		helper.assertTrue(core.room(concrete) >= 5, "tier 1 still needs " + concrete + ": room " + core.room(concrete));
		var hit = new net.minecraft.world.phys.BlockHitResult(Vec3.atCenterOf(terminal), Direction.UP, terminal, false);
		var result = helper.getLevel().getBlockState(terminal).useItemOn(stack, helper.getLevel(), owner, net.minecraft.world.InteractionHand.MAIN_HAND, hit);
		helper.assertTrue(result instanceof net.minecraft.world.InteractionResult.Success,
				"using a needed block on the terminal is handled there (no fall-through to opening the screen): " + result);
		helper.assertTrue(core.stored(concrete) == before + 5 && stack.isEmpty(), "the blocks went into the core: " + before + " -> " + core.stored(concrete));
		ItemStack dirt = new ItemStack(net.minecraft.world.item.Items.DIRT, 3);
		helper.assertTrue(!core.wants(dirt) && core.deposit(dirt) == 0 && dirt.getCount() == 3, "the core takes nothing the tower doesn't need");
		helper.succeed();
	}

	/** The instant-build and test commands exist in every build, for operators. */
	@GameTest
	public void droneTestCommandIsRegistered(GameTestHelper helper) {
		if (TestFocus.skip("tower")) {
			helper.succeed();
			return;
		}
		var node = helper.getLevel().getServer().getCommands().getDispatcher().getRoot().getChild("dronetest");
		helper.assertTrue(node != null && node.getChild("build") != null && node.getChild("stock") != null,
				"/dronetest build and /dronetest stock are registered");
		helper.succeed();
	}

	/**
	 * /dronetest repair (TowerCoreBlockEntity.repair; Justin, 10 November 2026: "a couple of blocks I destroyed on
	 * accident, so I'd like them to be back"): the tower's own blocks that are gone come back, the Cargo Exchange whole
	 * and bound to the depot again; a block someone put in the place of one of the tower's stays where it is.
	 */
	@GameTest(structure = TriPillarArena.ARENA, maxTicks = 100, skyAccess = true)
	public void repairPutsBackTheTowersMissingBlocks(GameTestHelper helper) {
		if (TestFocus.skip("tower")) {
			helper.succeed();
			return;
		}
		Player owner = helper.makeMockPlayer(GameType.CREATIVE);
		ServerLevel level = helper.getLevel();
		TowerCoreBlockEntity core = TriPillarArena.build(helper, owner, 1);
		BlockPos cargo = TriPillarArena.master(core, false);
		helper.assertTrue(level.getBlockEntity(cargo) instanceof io.github.jimbozoomer.jugcraft.tower.ExchangeStationBlockEntity station
				&& station.terminal() != null, "the Cargo Exchange is bound to the depot");
		// three plain blocks of the foundation's (its walls): two broken, one swapped for dirt
		TowerData data = core.layout();
		java.util.List<BlockPos> plain = new java.util.ArrayList<>();
		java.util.List<BlockState> were = new java.util.ArrayList<>();
		for (int[] b : data.foundation.place) {
			BlockPos at = core.getBlockPos().offset(b[0], b[1], b[2]);
			BlockState s = level.getBlockState(at);
			if (b[1] >= 2 && s.getBlock() == data.state(b[3]).getBlock() && level.getBlockEntity(at) == null && s.isCollisionShapeFullBlock(level, at)) {
				plain.add(at);
				were.add(s);
				if (plain.size() == 3) {
					break;
				}
			}
		}
		helper.assertTrue(plain.size() == 3, "three plain blocks of the tower's: " + plain);
		level.removeBlock(plain.get(0), false);
		level.removeBlock(plain.get(1), false);
		level.setBlock(plain.get(2), Blocks.DIRT.defaultBlockState(), net.minecraft.world.level.block.Block.UPDATE_ALL);
		level.removeBlock(cargo, false);
		helper.assertTrue(level.getBlockState(cargo).isAir(), "the Cargo Exchange is broken");
		var done = core.repair(level);
		helper.assertTrue(done != null && done.placed() >= 3 && done.blocked() >= 1, "the repair put blocks back and left the dirt: " + done);
		helper.assertTrue(level.getBlockState(plain.get(0)).is(were.get(0).getBlock()) && level.getBlockState(plain.get(1)).is(were.get(1).getBlock()),
				"the broken walls are back: " + level.getBlockState(plain.get(0)) + ", " + level.getBlockState(plain.get(1)));
		helper.assertTrue(level.getBlockState(plain.get(2)).is(Blocks.DIRT), "the block someone put in stays");
		helper.assertTrue(level.getBlockState(cargo).is(ExchangeStations.CARGO), "the Cargo Exchange is back: " + level.getBlockState(cargo));
		helper.assertTrue(level.getBlockEntity(cargo) instanceof io.github.jimbozoomer.jugcraft.tower.ExchangeStationBlockEntity station
				&& station.terminal() == core.terminal(), "the Cargo Exchange is bound to the depot again");
		helper.assertTrue(core.repair(level).placed() == 0, "a second repair has nothing to put back");
		helper.succeed();
	}

	/**
	 * The foundation's two exchanges beside the yard are bound once the tower is tier 1, and their marked ports feed the
	 * depot: power into the terminal, items into the packager.
	 */
	@GameTest(structure = TriPillarArena.ARENA, maxTicks = 100, skyAccess = true)
	public void exchangePortsFeedTheDepot(GameTestHelper helper) {
		if (TestFocus.skip("tower")) {
			helper.succeed();
			return;
		}
		Player owner = helper.makeMockPlayer(GameType.CREATIVE);
		ServerLevel level = helper.getLevel();
		TowerCoreBlockEntity core = TriPillarArena.build(helper, owner, 1);
		BlockPos energyMaster = TriPillarArena.master(core, true);
		BlockPos cargoMaster = TriPillarArena.master(core, false);
		helper.assertTrue(energyMaster.equals(TriPillarArena.at(core, TriPillarArena.ENERGY_MASTER))
				&& level.getBlockState(energyMaster).is(ExchangeStations.ENERGY), "the Energy Exchange stands in the intake face: " + level.getBlockState(energyMaster));
		helper.assertTrue(cargoMaster.equals(TriPillarArena.at(core, TriPillarArena.CARGO_MASTER))
				&& level.getBlockState(cargoMaster).is(ExchangeStations.CARGO), "the Cargo Exchange stands in the intake face: " + level.getBlockState(cargoMaster));
		BlockPos power = TriPillarArena.port(level, energyMaster);
		BlockPos cargo = TriPillarArena.port(level, cargoMaster);
		Direction powerSide = level.getBlockState(energyMaster).getValue(io.github.jimbozoomer.jugcraft.tower.ExchangeStationBlock.FACING);
		Direction cargoSide = level.getBlockState(cargoMaster).getValue(io.github.jimbozoomer.jugcraft.tower.ExchangeStationBlock.FACING);
		DroneTerminalBlockEntity terminal = core.terminal();
		terminal.rescanNow();
		helper.assertTrue(terminal.activePickups().size() == 1, "the exchanges leave the roof's pickup to the depot");
		var energy = io.github.jimbozoomer.jugcraft.energy.EnergyStorage.SIDED.find(level, power, powerSide);
		helper.assertTrue(energy != null, "the Energy Exchange's port exposes the depot's energy on its " + powerSide + " face");
		long before = terminal.energy().getAmount();
		try (var tx = net.fabricmc.fabric.api.transfer.v1.transaction.Transaction.openOuter()) {
			energy.insert(1000, tx);
			tx.commit();
		}
		helper.assertTrue(terminal.energy().getAmount() > before, "power put into the port reached the terminal");
		var items = net.fabricmc.fabric.api.transfer.v1.item.ItemStorage.SIDED.find(level, cargo, cargoSide);
		helper.assertTrue(items != null, "the Cargo Exchange's port exposes the depot's cargo store on its " + cargoSide + " face");
		try (var tx = net.fabricmc.fabric.api.transfer.v1.transaction.Transaction.openOuter()) {
			long moved = items.insert(net.fabricmc.fabric.api.transfer.v1.item.ItemVariant.of(net.minecraft.world.item.Items.STONE_BRICKS), 16, tx);
			tx.commit();
			helper.assertTrue(moved == 16, "16 stone bricks went in, moved " + moved);
		}
		helper.assertTrue(terminal.packagerEntity().count(stack -> stack.is(net.minecraft.world.item.Items.STONE_BRICKS)) >= 16,
				"the items reached the packager");
		helper.succeed();
	}

	/**
	 * The frame rate check's drones (r186, client/FpsCheck): it fills the hangars, and sends drones on round trips that
	 * carry nothing: they go out and come home, and nothing is placed where they hover or taken from the packager.
	 */
	@GameTest(structure = TriPillarArena.ARENA, maxTicks = 1500, skyAccess = true)
	public void frameRateCheckDronesFlyEmpty(GameTestHelper helper) {
		if (TestFocus.skip("tower")) {
			helper.succeed();
			return;
		}
		Player owner = helper.makeMockPlayer(GameType.CREATIVE);
		ServerLevel level = helper.getLevel();
		TowerCoreBlockEntity core = TriPillarArena.build(helper, owner, 1);
		DroneTerminalBlockEntity terminal = core.terminal();
		terminal.rescanNow();
		terminal.setTestSupplies(true);
		int before = terminal.fleet().size();
		int added = terminal.fillForCheck(400);
		helper.assertTrue(added > 0 && terminal.fleet().size() == before + added, "the hangars filled: added " + added);
		helper.assertTrue(terminal.fillForCheck(400) == 0, "full: no more fit");
		BlockPos to = level.getHeightmapPos(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING, core.getBlockPos().offset(18, 0, -30));
		var below = level.getBlockState(to.below());
		var at = level.getBlockState(to);
		int sent = terminal.flyForCheck(level, to, 3);
		helper.assertTrue(sent > 0 && terminal.scheduler().activeFlights() == sent, "drones went out: " + sent);
		helper.succeedWhen(() -> {
			helper.assertTrue(terminal.scheduler().activeFlights() == 0, "still flying: " + terminal.scheduler().activeFlights());
			helper.assertTrue(level.getBlockState(to).equals(at) && level.getBlockState(to.below()).equals(below), "nothing placed where they hovered");
			helper.assertTrue(terminal.fleet().size() == before + added, "every drone came home");
		});
	}

	/**
	 * The core alone starts a tower (no plinth): UPGRADE needs every block of the foundation in the core and pays exactly
	 * them. The core takes only what the tower still needs, from a hand, a whole inventory or a pipe, and no more.
	 */
	@GameTest(structure = TriPillarArena.ARENA, maxTicks = 100, skyAccess = true)
	public void upgradeNeedsTheFoundationsBlocks(GameTestHelper helper) {
		if (TestFocus.skip("tower")) {
			helper.succeed();
			return;
		}
		Player owner = helper.makeMockPlayer(GameType.SURVIVAL);
		ServerLevel level = helper.getLevel();
		BlockPos at = helper.absolutePos(TriPillarArena.CORE);
		level.setBlock(at, JugcraftTower.CORE.defaultBlockState(), 3);
		TowerCoreBlockEntity core = (TowerCoreBlockEntity) level.getBlockEntity(at);
		core.tryUpgrade(owner);
		helper.assertTrue(core.foundationState() == 0 && core.building() == 0, "an empty core: nothing starts");
		TowerData data = core.layout();
		var price = data.cost(data.foundation);
		var concrete = io.github.jimbozoomer.jugcraft.test.TowerGameTests.modBlock("reinforced_concrete").asItem();
		// A player's inventory: some of what the foundation needs, and something it doesn't.
		owner.getInventory().setItem(0, new ItemStack(concrete, 64));
		owner.getInventory().setItem(1, new ItemStack(net.minecraft.world.item.Items.DIRT, 64));
		int taken = core.depositFrom(owner);
		helper.assertTrue(taken == 64 && core.stored(concrete) == 64 && owner.getInventory().getItem(0).isEmpty()
				&& owner.getInventory().getItem(1).getCount() == 64, "LOAD takes the concrete and leaves the dirt: took " + taken);
		// A pipe or hopper fills it too, but never past what the tower still needs.
		var concreteRoom = core.room(concrete);
		try (var tx = net.fabricmc.fabric.api.transfer.v1.transaction.Transaction.openOuter()) {
			long moved = core.blockStorage().insert(net.fabricmc.fabric.api.transfer.v1.item.ItemVariant.of(concrete), concreteRoom + 500L, tx);
			tx.commit();
			helper.assertTrue(moved == concreteRoom && core.room(concrete) == 0, "a pipe fills the concrete up to what the tower needs: " + moved);
		}
		// Everything the foundation needs but one of its last kind of block: still nothing starts.
		var kinds = new java.util.ArrayList<>(price.entrySet());
		var last = kinds.getLast();
		for (var c : kinds) {
			put(core, c.getKey(), c.getValue() - core.stored(c.getKey()) - (c == last ? 1 : 0));
		}
		helper.assertTrue(core.stored(last.getKey()) == last.getValue() - 1, "one " + last.getKey() + " short");
		core.tryUpgrade(owner);
		helper.assertTrue(core.foundationState() == 0 && core.building() == 0, "one block short: nothing starts");
		put(core, last.getKey(), 1);
		var before = new java.util.HashMap<>(core.store());
		core.tryUpgrade(owner);
		helper.assertTrue(core.foundationState() == 1 && core.building() == 0, "every block in: the foundation starts");
		for (var c : price.entrySet()) {
			helper.assertTrue(core.stored(c.getKey()) == before.get(c.getKey()) - c.getValue(),
					"the foundation's " + c.getValue() + " " + c.getKey() + " were paid, " + core.stored(c.getKey()) + " left");
		}
		helper.succeed();
	}

	/**
	 * The core's placement warning, for a client that can't show it (a mock player): using the core on the ground places
	 * nothing the first time; using it on the same spot again places it. A different spot counts as a new first use.
	 */
	@GameTest(structure = TriPillarArena.ARENA, maxTicks = 40, skyAccess = true)
	public void coreAsksBeforeItIsPlaced(GameTestHelper helper) {
		if (TestFocus.skip("tower")) {
			helper.succeed();
			return;
		}
		ServerLevel level = helper.getLevel();
		Player owner = helper.makeMockPlayer(GameType.SURVIVAL);
		ItemStack core = new ItemStack(JugcraftTower.CORE.asItem(), 2);
		owner.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, core);
		BlockPos a = helper.absolutePos(TriPillarArena.CORE).below();
		BlockPos b = a.east(3);
		for (BlockPos p : BlockPos.betweenClosed(a.offset(-1, 1, -1), b.offset(1, 2, 1))) {
			level.setBlock(p, Blocks.AIR.defaultBlockState(), 3);
		}
		java.util.function.Function<BlockPos, net.minecraft.world.InteractionResult> use = ground -> core.useOn(
				new net.minecraft.world.item.context.UseOnContext(owner, net.minecraft.world.InteractionHand.MAIN_HAND,
						new net.minecraft.world.phys.BlockHitResult(Vec3.atCenterOf(ground).add(0, 0.5, 0), Direction.UP, ground, false)));
		java.util.function.Predicate<BlockPos> placed = ground -> level.getBlockEntity(ground) instanceof TowerCoreBlockEntity
				|| level.getBlockEntity(ground.above()) instanceof TowerCoreBlockEntity;
		use.apply(a);
		helper.assertTrue(!placed.test(a) && core.getCount() == 2, "the first use places nothing (it warns first)");
		use.apply(b);
		helper.assertTrue(!placed.test(b) && core.getCount() == 2, "another spot: a new first use, still nothing");
		var result = use.apply(b);
		helper.assertTrue(result instanceof net.minecraft.world.InteractionResult.Success && placed.test(b) && core.getCount() == 1,
				"the same spot again places the core: " + result);
		helper.assertTrue(!placed.test(a), "the first spot stays empty");
		helper.succeed();
	}

	/**
	 * The core's loading screen for the first layer takes only what the foundation and tier 1 still need, by shift-click
	 * or the PUT BLOCKS IN slot (the rest stays with the player); BUILD FIRST LAYER then builds the foundation and goes
	 * straight on to tier 1 from the blocks already in.
	 */
	@GameTest(structure = TriPillarArena.ARENA, maxTicks = 1600, skyAccess = true)
	public void firstLayerLoadsOnlyWhatItNeedsThenBuildsBoth(GameTestHelper helper) {
		if (TestFocus.skip("tower")) {
			helper.succeed();
			return;
		}
		Player owner = helper.makeMockPlayer(GameType.SURVIVAL);
		TowerCoreBlockEntity core = TriPillarArena.placeCore(helper);
		core.setOwner(owner.getUUID());
		TowerData data = core.layout();
		var cost = core.firstLayerCost();
		var tier1 = data.cost(data.tier(1));
		helper.assertTrue(core.firstLayer() && core.firstLayerSteps().size() == 2, "a new core's first layer: the foundation and tier 1");
		for (var c : tier1.entrySet()) {
			helper.assertTrue(cost.get(c.getKey()) == data.cost(data.foundation).getOrDefault(c.getKey(), 0) + c.getValue(),
					"the list adds tier 1's " + c.getKey() + " to the foundation's");
		}
		var menu = new io.github.jimbozoomer.jugcraft.tower.TowerLoadMenu(1, owner.getInventory(),
				new io.github.jimbozoomer.jugcraft.tower.TowerLoadMenu.Opening(core.getBlockPos(), false, core.getBlockPos()));
		// the smallest need: a few stacks fill it, the rest stays with the player
		var smallest = cost.entrySet().stream().min(java.util.Map.Entry.comparingByValue()).orElseThrow();
		var item = smallest.getKey();
		int need = smallest.getValue();
		int stacks = need / 64;
		for (int i = 0; i < stacks; i++) {
			owner.getInventory().setItem(0, new ItemStack(item, 64));
			menu.quickMoveStack(owner, 1 + 27);
			helper.assertTrue(owner.getInventory().getItem(0).isEmpty(), "shift-click put a whole stack in (" + core.stored(item) + " in)");
		}
		ItemStack carried = new ItemStack(item, 64);
		ItemStack left = menu.getSlot(0).safeInsert(carried);
		helper.assertTrue(core.stored(item) == need && left.getCount() == 64 - (need - stacks * 64),
				"the slot took only the " + (need - stacks * 64) + " still needed: " + core.stored(item) + " in, " + left.getCount() + " left");
		helper.assertTrue(menu.getSlot(0).safeInsert(new ItemStack(item, 5)).getCount() == 5 && core.stored(item) == need, "nothing extra goes in");
		ItemStack dirt = new ItemStack(net.minecraft.world.item.Items.DIRT, 7);
		helper.assertTrue(!menu.getSlot(0).mayPlace(dirt) && menu.getSlot(0).safeInsert(dirt).getCount() == 7, "nor anything the tower doesn't use");
		helper.assertTrue(!core.buildFirstLayer(owner) && core.foundationState() == 0, "BUILD with blocks short starts nothing");
		core.fillFirstLayer();
		for (var c : cost.entrySet()) {
			helper.assertTrue(core.stored(c.getKey()) == c.getValue() && core.room(c.getKey()) == 0, "everything in, exactly: " + c.getKey());
		}
		helper.assertTrue(core.buildFirstLayer(owner) && core.buildingFoundation() && core.firstLayerQueued(),
				"BUILD FIRST LAYER starts the foundation with tier 1 to follow");
		for (var c : tier1.entrySet()) {
			helper.assertTrue(core.stored(c.getKey()) == c.getValue(), "tier 1's " + c.getKey() + " wait in the core");
		}
		helper.startSequence()
				.thenWaitUntil(() -> helper.assertTrue(core.tier() == 1, "tier 1 built after the foundation (state " + core.foundationState()
						+ ", building " + core.building() + ", progress " + core.buildProgress() + ")"))
				.thenExecute(() -> {
					helper.assertTrue(!core.firstLayer() && !core.firstLayerQueued(), "the first layer is done");
					for (var c : tier1.keySet()) {
						helper.assertTrue(core.stored(c) == 0, "tier 1 paid its " + c);
					}
				})
				.thenSucceed();
	}

	/**
	 * After the first layer the core takes no blocks by hand, terminal or pipe: they go in through the Cargo Exchange, into
	 * the tower storage (10 double chests), up to what the rest of the tower needs; anything else still goes to the
	 * depot's packager.
	 */
	@GameTest(structure = TriPillarArena.ARENA, maxTicks = 100, skyAccess = true)
	public void afterTheFirstLayerTheCargoExchangeFillsTheStorage(GameTestHelper helper) {
		if (TestFocus.skip("tower")) {
			helper.succeed();
			return;
		}
		Player owner = helper.makeMockPlayer(GameType.SURVIVAL);
		ServerLevel level = helper.getLevel();
		TowerCoreBlockEntity core = TriPillarArena.build(helper, owner, 1);
		var concrete = io.github.jimbozoomer.jugcraft.test.TowerGameTests.modBlock("reinforced_concrete").asItem();
		helper.assertTrue(!core.firstLayer() && core.stillNeeded(concrete) > 0, "tier 1: the rest of the tower needs concrete");
		helper.assertTrue(core.room(concrete) == 0 && !core.wants(new ItemStack(concrete, 5)), "the core takes none by hand");
		ItemStack hand = new ItemStack(concrete, 5);
		owner.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, hand);
		var hit = new net.minecraft.world.phys.BlockHitResult(Vec3.atCenterOf(core.getBlockPos()), Direction.UP, core.getBlockPos(), false);
		level.getBlockState(core.getBlockPos()).useItemOn(hand, level, owner, net.minecraft.world.InteractionHand.MAIN_HAND, hit);
		helper.assertTrue(hand.getCount() == 5 && core.stored(concrete) == 0, "using concrete on the core puts none in");
		try (var tx = net.fabricmc.fabric.api.transfer.v1.transaction.Transaction.openOuter()) {
			helper.assertTrue(core.blockStorage().insert(net.fabricmc.fabric.api.transfer.v1.item.ItemVariant.of(concrete), 64, tx) == 0,
					"nor does a pipe on the core");
		}
		BlockPos cargoMaster = TriPillarArena.master(core, false);
		BlockPos cargo = TriPillarArena.port(level, cargoMaster);
		Direction side = level.getBlockState(cargoMaster).getValue(io.github.jimbozoomer.jugcraft.tower.ExchangeStationBlock.FACING);
		var port = net.fabricmc.fabric.api.transfer.v1.item.ItemStorage.SIDED.find(level, cargo, side);
		helper.assertTrue(port != null, "the Cargo Exchange takes items");
		try (var tx = net.fabricmc.fabric.api.transfer.v1.transaction.Transaction.openOuter()) {
			long moved = port.insert(net.fabricmc.fabric.api.transfer.v1.item.ItemVariant.of(concrete), 100, tx);
			tx.commit();
			helper.assertTrue(moved == 100 && core.stored(concrete) == 100, "100 concrete into the tower storage: moved " + moved
					+ ", stored " + core.stored(concrete));
		}
		var terminal = core.terminal();
		int bricksBefore = terminal.packagerEntity().count(s -> s.is(net.minecraft.world.item.Items.STONE_BRICKS));
		try (var tx = net.fabricmc.fabric.api.transfer.v1.transaction.Transaction.openOuter()) {
			port.insert(net.fabricmc.fabric.api.transfer.v1.item.ItemVariant.of(net.minecraft.world.item.Items.STONE_BRICKS), 16, tx);
			tx.commit();
		}
		helper.assertTrue(core.stored(net.minecraft.world.item.Items.STONE_BRICKS.asItem()) == 0
				&& terminal.packagerEntity().count(s -> s.is(net.minecraft.world.item.Items.STONE_BRICKS)) == bricksBefore + 16,
				"what the tower doesn't use goes to the depot's packager as before");
		// filling it: everything the rest of the tower needs is far more than 540 stacks; it stops at 540
		java.util.Set<net.minecraft.world.item.Item> kinds = new java.util.LinkedHashSet<>();
		for (int t = 2; t <= TowerData.TIERS; t++) {
			kinds.addAll(core.layout().cost(core.layout().tier(t)).keySet());
		}
		for (var item : kinds) {
			try (var tx = net.fabricmc.fabric.api.transfer.v1.transaction.Transaction.openOuter()) {
				core.storageInlet().insert(net.fabricmc.fabric.api.transfer.v1.item.ItemVariant.of(item), 1_000_000, tx);
				tx.commit();
			}
			helper.assertTrue(core.stored(item) <= core.stillNeeded(item), "never more " + item + " than the tower still needs");
		}
		helper.assertTrue(core.storageStacksUsed() == TowerCoreBlockEntity.STORAGE_STACKS,
				"the tower storage fills to its 540 stacks, got " + core.storageStacksUsed());
		try (var tx = net.fabricmc.fabric.api.transfer.v1.transaction.Transaction.openOuter()) {
			long more = port.insert(net.fabricmc.fabric.api.transfer.v1.item.ItemVariant.of(concrete), 64, tx);
			helper.assertTrue(core.storageRoom(concrete) == 0, "full: no more concrete for the storage (" + more + " went to the packager)");
		}
		helper.succeed();
	}

	/** Puts {@code n} of {@code item} into the core a stack at a time, as a player would. */
	private static void put(TowerCoreBlockEntity core, net.minecraft.world.item.Item item, int n) {
		while (n > 0) {
			int stack = Math.min(n, 64);
			int taken = core.deposit(new ItemStack(item, stack));
			if (taken <= 0) {
				throw new AssertionError("the core refused " + item + " it still needs");
			}
			n -= taken;
		}
	}

	/**
	 * A core placed on top of level ground sets itself into it (so the hall floor is level with the land); one set into a
	 * hole, or on a slope's step, stays where it was put.
	 */
	@GameTest(structure = TriPillarArena.ARENA, maxTicks = 40, skyAccess = true)
	public void coreSetsItselfIntoTheGround(GameTestHelper helper) {
		if (TestFocus.skip("tower")) {
			helper.succeed();
			return;
		}
		ServerLevel level = helper.getLevel();
		BlockPos ground = helper.absolutePos(TriPillarArena.CORE).below();
		BlockPos top = ground.above();
		for (BlockPos p : BlockPos.betweenClosed(top.offset(-1, 0, -1), top.offset(1, 1, 1))) {
			level.setBlock(p, Blocks.AIR.defaultBlockState(), 3);
		}
		level.setBlock(ground, Blocks.GRASS_BLOCK.defaultBlockState(), 3);
		helper.assertTrue(io.github.jimbozoomer.jugcraft.tower.TowerCoreBlock.sinks(level, top), "on top of flat ground the core sinks in");
		level.setBlock(top.east(), Blocks.DIRT.defaultBlockState(), 3);
		helper.assertTrue(!io.github.jimbozoomer.jugcraft.tower.TowerCoreBlock.sinks(level, top), "with ground beside it (in a hole) it stays");
		level.setBlock(top.east(), Blocks.AIR.defaultBlockState(), 3);
		level.setBlock(ground, Blocks.AIR.defaultBlockState(), 3);
		helper.assertTrue(!io.github.jimbozoomer.jugcraft.tower.TowerCoreBlock.sinks(level, top), "with nothing under it it stays");
		helper.succeed();
	}

	/** Tier 1 flies 24 T1 drones; tier 2 flies T2 drones too, 44 of them, and a T2 drone docks in a tier 2 hangar. */
	@GameTest(structure = TriPillarArena.ARENA, maxTicks = 200, skyAccess = true)
	public void towerTierGatesDroneTiers(GameTestHelper helper) {
		if (TestFocus.skip("tower")) {
			helper.succeed();
			return;
		}
		Player owner = helper.makeMockPlayer(GameType.CREATIVE);
		TowerCoreBlockEntity core = TriPillarArena.build(helper, owner, 1);
		DroneTerminalBlockEntity terminal = core.terminal();
		helper.assertTrue(terminal != null, "terminal built");
		helper.assertTrue(terminal.maxTier() == 1 && terminal.maxDrones() == 24, "tier 1: T1 drones, 24 of them (the roof pads)");
		helper.assertTrue(!terminal.linkDrone(owner, DroneTier.SURVEY_HEXACOPTER), "a T2 drone needs a tier 2 tower");
		helper.assertTrue(terminal.linkDrone(owner, DroneTier.COURIER_QUAD), "a T1 drone links");
		core.buildInstantly(helper.getLevel(), 2);
		helper.assertTrue(terminal.maxTier() == 2 && terminal.maxDrones() == 44, "tier 2: T2 drones, 44 of them");
		helper.assertTrue(terminal.linkDrone(owner, DroneTier.SURVEY_HEXACOPTER), "now a T2 drone links");
		helper.assertTrue(!terminal.linkDrone(owner, DroneTier.LIFTER_OCTOCOPTER), "but not T3");
		Vec3 hangar = terminal.dockOf(1);
		BlockPos at = core.getBlockPos();
		// any drone takes the lowest free dock that fits, up to the tower's tier (since 7 November 2026)
		double lowest = Double.MAX_VALUE;
		for (int t = 1; t <= 2; t++) {
			for (TowerData.Dock d : core.layout().tier(t).docks) {
				Vec3 v = new Vec3(at.getX() + d.x(), at.getY() + d.y(), at.getZ() + d.z());
				if (v.distanceToSqr(terminal.dockOf(0)) > 0.01) {
					lowest = Math.min(lowest, v.y);
				}
			}
		}
		helper.assertTrue((isDockOf(core.layout().tier(1), at, hangar) || isDockOf(core.layout().tier(2), at, hangar)) && hangar.y <= lowest + 0.01,
				"the T2 drone docks in the lowest free dock, at " + hangar + " (lowest " + lowest + ")");
		assertMaintenanceDoors(helper, helper.getLevel(), core);
		helper.succeed();
	}

	/**
	 * Drones put in the tower storage go to a free pad themselves (Justin, 7 November 2026: "whenever somebody puts drones
	 * inside the tower inventory, it'll drag them to the appropriate drone pad"), through the Cargo Exchange's way in as
	 * any pipe would put them; a drone past the tower's tier isn't taken.
	 */
	@GameTest(structure = TriPillarArena.ARENA, maxTicks = 200, skyAccess = true)
	public void dronesPutInTheTowerStorageGoToTheirPads(GameTestHelper helper) {
		if (TestFocus.skip("tower")) {
			helper.succeed();
			return;
		}
		Player owner = helper.makeMockPlayer(GameType.CREATIVE);
		TowerCoreBlockEntity core = TriPillarArena.build(helper, owner, 1);
		DroneTerminalBlockEntity terminal = core.terminal();
		helper.assertTrue(terminal != null && terminal.fleet().size() == 0, "a tier 1 tower with no drones");
		var t1 = io.github.jimbozoomer.jugcraft.drone.JugcraftDrones.DRONES.get(DroneTier.COURIER_QUAD);
		var t2 = io.github.jimbozoomer.jugcraft.drone.JugcraftDrones.DRONES.get(DroneTier.SURVEY_HEXACOPTER);
		long in, refused;
		try (var tx = net.fabricmc.fabric.api.transfer.v1.transaction.Transaction.openOuter()) {
			in = core.storageInlet().insert(net.fabricmc.fabric.api.transfer.v1.item.ItemVariant.of(t1), 3, tx);
			refused = core.storageInlet().insert(net.fabricmc.fabric.api.transfer.v1.item.ItemVariant.of(t2), 1, tx);
			tx.commit();
		}
		helper.assertTrue(in == 3, "three T1 drones go in, got " + in);
		helper.assertTrue(refused == 0, "a T2 drone is past a tier 1 tower");
		helper.startSequence()
				.thenWaitUntil(() -> helper.assertTrue(terminal.fleet().size() == 3, "the three drones are linked to the fleet: " + terminal.fleet().size()))
				.thenExecute(() -> {
					helper.assertTrue(core.stored(t1) == 0, "and gone from the storage");
					for (int i = 0; i < 3; i++) {
						helper.assertTrue(isDockOf(core.layout().tier(1), core.getBlockPos(), terminal.dockOf(i)), "drone " + i + " has a pad: " + terminal.dockOf(i));
					}
				})
				.thenSucceed();
	}

	/**
	 * The finished tower holds its whole fleet: 128 drones, each in a dock that fits it (any tier's, since 7 November
	 * 2026), every drone in a dock of its own. The 129th is refused. Its drones load at the four tower pickups.
	 */
	@GameTest(structure = TriPillarArena.FULL, maxTicks = 400, skyAccess = true)
	public void finishedTowerHoldsItsWholeFleet(GameTestHelper helper) {
		if (TestFocus.skip("tower")) {
			helper.succeed();
			return;
		}
		Player owner = helper.makeMockPlayer(GameType.CREATIVE);
		TowerCoreBlockEntity core = TriPillarArena.build(helper, owner, 9);
		BlockPos at = core.getBlockPos();
		TowerData data = core.layout();
		DroneTerminalBlockEntity terminal = core.terminal();
		helper.assertTrue(terminal != null && terminal.maxTier() == 9 && terminal.maxDrones() == 128, "tier 9: every drone tier, 128 drones");
		// From tier 2 on the drones load at the tower's own pickups; the ground cradle's pickup stays the packager's.
		List<int[]> pickups = terminal.activePickups();
		helper.assertTrue(pickups.size() == 4, "four tower pickups (tiers 2, 4, 6 and 8), got " + pickups.size());
		for (int t = 1; t <= TowerData.TIERS; t++) {
			for (TowerData.Pickup p : data.tier(t).pickups) {
				helper.assertTrue(pickups.stream().anyMatch(q -> q[0] == at.getX() + p.x() && q[1] == at.getY() + p.y() && q[2] == at.getZ() + p.z()),
						"the tier " + t + " pickup is in use");
			}
		}
		helper.assertTrue(terminal.pickupCorner() != null && terminal.packagerEntity() != null, "the ground pickup and packager are still found");
		int linked = 0;
		for (int t = 1; t <= TowerData.TIERS; t++) {
			DroneTier drone = DroneTier.byNumber(t).orElseThrow();
			TowerData.Tier tier = data.tier(t);
			for (int i = 0; i < tier.docks.size(); i++) {
				helper.assertTrue(terminal.linkDrone(owner, drone), "drone " + (linked + 1) + " (" + drone + ") finds a dock");
				Vec3 dock = terminal.dockOf(linked);
				boolean fits = false;
				for (int u = 1; u <= TowerData.TIERS && !fits; u++) {
					for (TowerData.Dock d : data.tier(u).docks) {
						fits |= d.size().ordinal() >= drone.size().ordinal() && Math.abs(at.getX() + d.x() - dock.x) < .01
								&& Math.abs(at.getY() + d.y() - dock.y) < .01 && Math.abs(at.getZ() + d.z() - dock.z) < .01;
					}
				}
				helper.assertTrue(fits, "T" + t + " drone uses a dock that fits it, got " + dock);
				linked++;
			}
		}
		helper.assertTrue(linked == 128, "128 drones linked, got " + linked);
		helper.assertTrue(!terminal.linkDrone(owner, DroneTier.COURIER_QUAD), "the 129th drone is refused");
		Set<String> spots = new HashSet<>();
		for (int i = 0; i < linked; i++) {
			Vec3 dock = terminal.dockOf(i);
			helper.assertTrue(spots.add(Math.round(dock.x * 2) + "," + Math.round(dock.y * 2) + "," + Math.round(dock.z * 2)),
					"every drone has its own dock (drone " + i + " at " + dock + ")");
		}
		helper.succeed();
	}

	/** The tower keeps its own chunks loaded from the start, and lets them go when the core is broken. */
	@GameTest(structure = TriPillarArena.ARENA, maxTicks = 100, skyAccess = true)
	public void towerKeepsItsChunksLoaded(GameTestHelper helper) {
		if (TestFocus.skip("tower")) {
			helper.succeed();
			return;
		}
		TowerCoreBlockEntity core = TriPillarArena.placeCore(helper);
		BlockPos pos = core.getBlockPos();
		long chunk = net.minecraft.world.level.ChunkPos.containing(pos).pack();
		helper.runAfterDelay(3, () -> {
			var tickets = helper.getLevel().getDataStorage().computeIfAbsent(net.minecraft.world.level.TicketStorage.TYPE);
			helper.assertTrue(tickets.getTickets(chunk).stream().anyMatch(ticket -> ticket.getType() == TowerCoreBlockEntity.CHUNK_TICKET), "the core's chunk has a tower ticket");
			helper.getLevel().setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
			helper.assertTrue(tickets.getTickets(chunk).stream().noneMatch(ticket -> ticket.getType() == TowerCoreBlockEntity.CHUNK_TICKET), "broken, the tower removes only its own ticket");
			helper.succeed();
		});
	}

	/** A drone flies to a loaded site far away over chunks that are not loaded: only the two ends must be. */
	@GameTest(maxTicks = 400, skyAccess = true)
	public void dronesFlyOverUnloadedChunks(GameTestHelper helper) {
		if (TestFocus.skip("tower")) {
			helper.succeed();
			return;
		}
		BlockPos from = helper.absolutePos(new BlockPos(5, 3, 5));
		BlockPos to = from.offset(40 * 16, 0, 0);
		net.minecraft.world.level.ChunkPos far = net.minecraft.world.level.ChunkPos.containing(to);
		helper.getLevel().setChunkForced(far.x(), far.z(), true);
		helper.succeedWhen(() -> {
			helper.assertTrue(helper.getLevel().isLoaded(to), "the far site's chunk has loaded");
			BlockPos middle = from.offset(20 * 16, 0, 0);
			if (!helper.getLevel().isLoaded(middle)) {
				double cruise = io.github.jimbozoomer.jugcraft.drone.DroneRoutes.cruiseHeight(helper.getLevel(),
						net.minecraft.world.phys.Vec3.atCenterOf(from), net.minecraft.world.phys.Vec3.atCenterOf(to));
				helper.getLevel().setChunkForced(far.x(), far.z(), false);
				helper.assertTrue(!Double.isNaN(cruise), "a route over unloaded chunks still gets a cruise height");
			} else {
				helper.getLevel().setChunkForced(far.x(), far.z(), false);
			}
		});
	}

	/**
	 * Several towers per player per dimension, each outside the build radius of the player's others (other players
	 * are not limited by it). Every display of the foundation is tied to the tower's own terminal: the central holo
	 * table, the map table and the master console among them.
	 */
	@GameTest(structure = TriPillarArena.ARENA, maxTicks = 100, skyAccess = true)
	public void oneTowerPerPlayerAndPinnedDisplays(GameTestHelper helper) {
		if (TestFocus.skip("tower")) {
			helper.succeed();
			return;
		}
		Player owner = helper.makeMockPlayer(GameType.CREATIVE);
		ServerLevel level = helper.getLevel();
		TowerCoreBlockEntity core = TriPillarArena.placeCore(helper);
		core.setOwner(owner.getUUID());
		io.github.jimbozoomer.jugcraft.tower.TowerRegistry.get(level).claim(owner.getUUID(), core.getBlockPos());
		int radius = TowerCoreBlockEntity.BUILD_RADIUS_CHUNKS;
		BlockPos inside = core.getBlockPos().offset(radius * 16, 0, 0);
		BlockPos outside = core.getBlockPos().offset((radius + 1) * 16, 0, 0);
		helper.assertTrue(!io.github.jimbozoomer.jugcraft.tower.TowerCoreBlock.mayPlace(level, owner, inside),
				"a second core inside the first one's " + radius + "-chunk build radius is refused");
		helper.assertTrue(io.github.jimbozoomer.jugcraft.tower.TowerCoreBlock.mayPlace(level, owner, outside),
				"a second core outside the build radius is allowed");
		io.github.jimbozoomer.jugcraft.tower.TowerRegistry.get(level).claim(owner.getUUID(), outside);
		helper.assertTrue(io.github.jimbozoomer.jugcraft.tower.TowerRegistry.get(level).coresOf(level, owner.getUUID()).size() == 2,
				"the player owns both towers");
		io.github.jimbozoomer.jugcraft.tower.TowerRegistry.get(level).release(outside);
		Player other = helper.makeMockPlayer(GameType.CREATIVE);
		helper.assertTrue(io.github.jimbozoomer.jugcraft.tower.TowerCoreBlock.mayPlace(level, other, core.getBlockPos().offset(32, 0, 0)),
				"another player may still place one nearby");
		core.buildInstantly(level, 1);
		BlockPos terminal = core.terminalPos();
		int displays = 0;
		int tables = 0;
		for (int[] b : core.layout().foundation.place) {
			if (level.getBlockEntity(core.getBlockPos().offset(b[0], b[1], b[2])) instanceof DepotDisplayBlockEntity display) {
				displays++;
				helper.assertTrue(terminal.equals(display.terminal()), "the " + display.getBlockState().getBlock() + " at " + display.getBlockPos()
						+ " is linked to the tower's own terminal, got " + display.terminal());
				if (display.getBlockState().getBlock() instanceof io.github.jimbozoomer.jugcraft.tower.RoomKitDisplayBlock) {
					tables++;
				}
			}
		}
		helper.assertTrue(tables >= TriPillarArena.WALL_SCREENS.length && displays > tables,
				"the foundation has its rooms' wall screens and other displays: " + tables + " of " + displays);
		for (BlockPos place : new BlockPos[] {TriPillarArena.CENTRAL_TABLE, TriPillarArena.LESSON_SCREEN, TriPillarArena.MASTER_CONSOLE}) {
			BlockPos pos = TriPillarArena.at(core, place);
			helper.assertTrue(level.getBlockEntity(pos) instanceof DepotDisplayBlockEntity display && terminal.equals(display.terminal()),
					"the " + level.getBlockState(pos).getBlock() + " at " + place + " is pinned to the tower's terminal");
		}
		helper.succeed();
	}

	/** Creative Drones fly from a tier 1 tower, draw no power at all and still build the next tier. */
	@GameTest(structure = TriPillarArena.ARENA, maxTicks = 2400, skyAccess = true)
	public void creativeDronesNeedNoPower(GameTestHelper helper) {
		if (TestFocus.skip("tower")) {
			helper.succeed();
			return;
		}
		Player owner = helper.makeMockPlayer(GameType.CREATIVE);
		TowerCoreBlockEntity core = TriPillarArena.build(helper, owner, 1);
		DroneTerminalBlockEntity terminal = core.terminal();
		for (int i = 0; i < 4; i++) {
			helper.assertTrue(terminal.linkDrone(owner, DroneTier.CREATIVE_DRONE), "creative drone " + i + " links to a tier 1 tower");
		}
		helper.assertTrue(terminal.fleet().standbyDraw() == 0 && terminal.fleet().workingDraw() == 0, "creative drones draw no power");
		terminal.energy().setAmount(0);
		TriPillarArena.stock(core);
		core.tryUpgrade(owner);
		helper.assertTrue(core.building() == 2, "tier 2 started");
		TowerData.Tier deck = core.layout().tier(2);
		int before = TriPillarArena.countPlaced(helper.getLevel(), core, deck);
		helper.succeedWhen(() -> helper.assertTrue(TriPillarArena.countPlaced(helper.getLevel(), core, deck) > before,
				"creative drones fly tier 2 in with an empty battery (status: " + terminal.status().stream().map(c -> c.getString()).toList() + ")"));
	}

	/** Stray blocks inside the control building and the pillar feet are cleared; a chest there is left alone. */
	@GameTest(structure = TriPillarArena.ARENA, maxTicks = 100, skyAccess = true)
	public void strayBlocksInsideAreCleared(GameTestHelper helper) {
		if (TestFocus.skip("tower")) {
			helper.succeed();
			return;
		}
		TowerCoreBlockEntity core = TriPillarArena.placeCore(helper);
		TowerData data = TowerData.get();
		TowerData.Tier foundation = data.foundation;
		helper.assertTrue(foundation.air.length > 1 && data.tier(2).air.length > 0, "the foundation and tier 2 list cells to clear");
		ServerLevel level = helper.getLevel();
		int[] room = foundation.air[foundation.air.length / 2];
		int[] chest = foundation.air[foundation.air.length / 2 + 1];
		int[] deck = data.tier(2).air[data.tier(2).air.length / 2];
		BlockPos roomPos = core.getBlockPos().offset(room[0], room[1], room[2]);
		BlockPos chestPos = core.getBlockPos().offset(chest[0], chest[1], chest[2]);
		BlockPos deckPos = core.getBlockPos().offset(deck[0], deck[1], deck[2]);
		level.setBlockAndUpdate(roomPos, Blocks.DIRT.defaultBlockState());
		level.setBlockAndUpdate(chestPos, Blocks.CHEST.defaultBlockState());
		level.setBlockAndUpdate(deckPos, Blocks.OAK_LOG.defaultBlockState());
		core.buildInstantly(level, 2);
		helper.assertTrue(core.tier() == 2, "built to tier 2");
		helper.assertTrue(level.getBlockState(roomPos).isAir(), "the dirt in the control building is cleared");
		helper.assertTrue(level.getBlockState(deckPos).isAir(), "the log inside the pillar feet is cleared");
		helper.assertTrue(level.getBlockState(chestPos).is(Blocks.CHEST), "a chest is never cleared");
		helper.succeed();
	}

	/** Tier 2 built, the Cradle Struts stand whole under its supply cradle (work list item 12). */
	@GameTest(structure = TriPillarArena.ARENA, maxTicks = 100, skyAccess = true)
	public void cradleStrutsStandUnderTheSupplyCradle(GameTestHelper helper) {
		if (TestFocus.skip("tower")) {
			helper.succeed();
			return;
		}
		TowerCoreBlockEntity core = TriPillarArena.placeCore(helper);
		ServerLevel level = helper.getLevel();
		core.buildInstantly(level, 2);
		helper.assertTrue(core.tier() == 2, "built to tier 2");
		assertCradleStruts(helper, level, core);
		assertCanopyPosts(helper, level, core);
		helper.succeed();
	}

	/** T1 drones on the roof fly tier 2's tiles in from the tower's modules. */
	@GameTest(structure = TriPillarArena.ARENA, maxTicks = 2400, skyAccess = true)
	public void dronesFlyTheNextTierIn(GameTestHelper helper) {
		if (TestFocus.skip("tower")) {
			helper.succeed();
			return;
		}
		Player owner = helper.makeMockPlayer(GameType.CREATIVE);
		TowerCoreBlockEntity core = TriPillarArena.build(helper, owner, 1);
		DroneTerminalBlockEntity terminal = core.terminal();
		terminal.energy().setAmount(DroneTerminalBlockEntity.ENERGY_CAPACITY);
		for (int i = 0; i < 4; i++) {
			helper.assertTrue(terminal.linkDrone(owner, DroneTier.COURIER_QUAD), "drone " + i + " links");
		}
		TriPillarArena.stock(core);
		core.tryUpgrade(owner);
		helper.assertTrue(core.building() == 2, "tier 2 started");
		TowerData.Tier deck = core.layout().tier(2);
		int before = TriPillarArena.countPlaced(helper.getLevel(), core, deck);
		helper.onEachTick(() -> terminal.energy().setAmount(DroneTerminalBlockEntity.ENERGY_CAPACITY));
		helper.succeedWhen(() -> {
			helper.assertTrue(core.buildProgress() > 0, "a drone flew a tile of the pillar feet in (status: "
					+ terminal.status().stream().map(c -> c.getString()).toList() + ")");
			helper.assertTrue(TriPillarArena.countPlaced(helper.getLevel(), core, deck) > before, "tier 2 blocks appeared");
		});
	}

	// ------------------------------------------------------------------ helpers

	/** The finished tower's blocks (relative to the core): the foundation, then every tier with what it clears. */
	private static Map<BlockPos, BlockState> finishedTower(TowerData data) {
		Map<BlockPos, BlockState> world = new HashMap<>();
		List<TowerData.Tier> steps = new java.util.ArrayList<>();
		if (data.foundation != null) {
			steps.add(data.foundation);
		}
		steps.addAll(data.tiers);
		for (TowerData.Tier step : steps) {
			for (int[] c : step.clear) {
				world.remove(new BlockPos(c[0], c[1], c[2]));
			}
			for (int[] b : step.place) {
				world.put(new BlockPos(b[0], b[1], b[2]), data.palette.get(b[3]));
			}
		}
		return world;
	}

	/** Half the width of the room a docked drone takes: four small drones share a 5x5 pad, two medium, one large. */
	private static double halfWidth(DroneSize size) {
		return switch (size) {
			case SMALL -> 1.0;
			case MEDIUM -> 1.5;
			case LARGE -> 2.5;
		};
	}

	/**
	 * Keeps a test's zombie in its test. From despawning: the game removes a monster on its next tick when the nearest
	 * player is more than 128 blocks off, and a player another test put in the world is enough for that. Which order the
	 * tests' batches run in differs from run to run, so the door gun's test lost its zombie on its first tick in three
	 * runs of eight (found 10 November 2026 with r204, once the test said what its "first hit" had been: no damage, from
	 * nothing; it had been put down to the morning sun, which this keeps off as well: a zombie burns by day, and a husk,
	 * which doesn't, stands taller than where only the door gun bears).
	 */
	private static void keepInTest(net.minecraft.world.entity.LivingEntity mob) {
		if (mob != null) {
			mob.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.FIRE_RESISTANCE, 1_000_000, 0, false, false));
			if (mob instanceof net.minecraft.world.entity.Mob m) {
				m.setPersistenceRequired();
			}
		}
	}

	/** True when {@code dock} (absolute) is one of {@code tier}'s docks of the tower whose core is at {@code core}. */
	private static boolean isDockOf(TowerData.Tier tier, BlockPos core, Vec3 dock) {
		return tier.docks.stream().anyMatch(d -> Math.abs(core.getX() + d.x() - dock.x) < .01 && Math.abs(core.getY() + d.y() - dock.y) < .01
				&& Math.abs(core.getZ() + d.z() - dock.z) < .01);
	}

	/**
	 * An Aisle Containment Door (work list item 13): placed, it fills its 2 x 2 cells; shut it closes the doorway (and
	 * walkers still plan their way through it); a player walking up to it opens it (the doorway clear to walk through, the
	 * header still there) and it slides shut again once they have gone; a walker who isn't a player and isn't hostile
	 * opens it too (the tower's technicians work in the aisle behind it).
	 */
	@GameTest(maxTicks = 300)
	public void aisleContainmentDoorOpensForSomeoneWalkingUp(GameTestHelper helper) {
		if (TestFocus.skip("tower")) {
			helper.succeed();
			return;
		}
		ServerLevel level = helper.getLevel();
		var block = (io.github.jimbozoomer.jugcraft.tower.AisleContainmentDoorBlock) JugcraftTower.AISLE_CONTAINMENT_DOOR;
		var facing = net.minecraft.core.Direction.SOUTH;
		BlockPos master = helper.absolutePos(new BlockPos(3, 1, 2));
		BlockState state = block.defaultBlockState().setValue(io.github.jimbozoomer.jugcraft.tower.AisleContainmentDoorBlock.FACING, facing);
		level.setBlock(master, state, net.minecraft.world.level.block.Block.UPDATE_ALL);
		block.setPlacedBy(level, master, state, null, net.minecraft.world.item.ItemStack.EMPTY);
		for (net.minecraft.core.Vec3i o : io.github.jimbozoomer.jugcraft.tower.AisleContainmentDoorBlock.footprint(facing)) {
			BlockState cell = level.getBlockState(master.offset(o));
			helper.assertTrue(o.equals(net.minecraft.core.Vec3i.ZERO) ? cell.is(block)
					: cell.getBlock() instanceof io.github.jimbozoomer.jugcraft.tower.AisleContainmentDoorPartBlock, "the door's cell " + o + ": " + cell);
			helper.assertTrue(!cell.getCollisionShape(level, master.offset(o)).isEmpty(), "shut, the cell " + o + " closes the doorway");
			helper.assertTrue(cell.isPathfindable(net.minecraft.world.level.pathfinder.PathComputationType.LAND), "walkers plan their way through " + o);
		}
		// facing south, the door's second cell is to the west (x - 1); someone 1.2 out in front of its middle
		var player = helper.makeMockServerPlayerInLevel();
		player.teleportTo(master.getX(), master.getY(), master.getZ() + 1.2);
		helper.startSequence()
				.thenWaitUntil(() -> helper.assertTrue(level.getBlockState(master).getValue(io.github.jimbozoomer.jugcraft.tower.AisleContainmentDoorBlock.OPEN),
						"the door opens for someone walking up"))
				.thenExecute(() -> {
					helper.assertTrue(level.getBlockState(master).getCollisionShape(level, master).isEmpty(), "open, the doorway is clear");
					BlockPos top = master.above();
					var header = level.getBlockState(top).getCollisionShape(level, top);
					helper.assertTrue(!header.isEmpty() && header.min(net.minecraft.core.Direction.Axis.Y) > 0.8, "open, only the header is left: " + header);
					player.teleportTo(master.getX() + 0.5, master.getY(), master.getZ() + 6.5);
				})
				.thenWaitUntil(() -> helper.assertTrue(!level.getBlockState(master).getValue(io.github.jimbozoomer.jugcraft.tower.AisleContainmentDoorBlock.OPEN),
						"the door slides shut once they have gone"))
				// and for the crew, as the sliding doors do (anyone who isn't hostile): the technicians work in the aisle
				.thenExecute(() -> helper.spawnWithNoFreeWill(net.minecraft.world.entity.EntityTypes.PIG,
						helper.relativeVec(new Vec3(master.getX(), master.getY(), master.getZ() + 1.2))))
				.thenWaitUntil(() -> helper.assertTrue(level.getBlockState(master).getValue(io.github.jimbozoomer.jugcraft.tower.AisleContainmentDoorBlock.OPEN),
						"the door opens for a walker who isn't a player"))
				.thenExecute(() -> {
					for (var pig : level.getEntitiesOfClass(net.minecraft.world.entity.animal.pig.Pig.class, new net.minecraft.world.phys.AABB(master).inflate(4))) {
						pig.discard();
					}
				})
				.thenSucceed();
	}

	/**
	 * An automatic sliding door (layout 47; tower/SlidingDoorBlock): placed, a room's double fills its 2 x 2 cells, each
	 * knowing its master; shut, its leaves close the doorway, open only the lining is left; a monster walking up leaves it
	 * shut, anyone else opens it and it slides shut again once they have gone; breaking any cell takes the door whole.
	 */
	@GameTest(maxTicks = 400)
	public void slidingDoorOpensForTheCrewNotForMonsters(GameTestHelper helper) {
		if (TestFocus.skip("tower")) {
			helper.succeed();
			return;
		}
		ServerLevel level = helper.getLevel();
		var block = (io.github.jimbozoomer.jugcraft.tower.SlidingDoorBlock) io.github.jimbozoomer.jugcraft.test.TowerGameTests.modBlock("automatic_sliding_door_steel");
		var facing = net.minecraft.core.Direction.SOUTH;          // its outside: the leaves hang on the south face
		BlockPos master = helper.absolutePos(new BlockPos(4, 1, 3));
		BlockState state = block.defaultBlockState().setValue(io.github.jimbozoomer.jugcraft.tower.SlidingDoorBlock.FACING, facing);
		level.setBlock(master, state, net.minecraft.world.level.block.Block.UPDATE_ALL);
		block.setPlacedBy(level, master, state, null, net.minecraft.world.item.ItemStack.EMPTY);
		var footprint = io.github.jimbozoomer.jugcraft.tower.SlidingDoorBlock.footprint(block.style, facing);
		helper.assertTrue(footprint.size() == 4, "a double room door is 2 x 2: " + footprint);
		for (net.minecraft.core.Vec3i o : footprint) {
			BlockPos at = master.offset(o);
			BlockState cell = level.getBlockState(at);
			helper.assertTrue(o.equals(net.minecraft.core.Vec3i.ZERO) ? cell.is(block)
					: cell.getBlock() instanceof io.github.jimbozoomer.jugcraft.tower.SlidingDoorPartBlock
							&& io.github.jimbozoomer.jugcraft.tower.SlidingDoorPartBlock.masterPos(at, cell).equals(master), "the door's cell " + o + ": " + cell);
			helper.assertTrue(cell.isPathfindable(net.minecraft.world.level.pathfinder.PathComputationType.LAND), "the crew plan their way through " + o);
		}
		// facing south, the second cell is to the west (the walker's left coming north)
		helper.assertTrue(level.getBlockState(master.west()).getBlock() instanceof io.github.jimbozoomer.jugcraft.tower.SlidingDoorPartBlock, "the second cell west");
		var shut = level.getBlockState(master).getCollisionShape(level, master);
		helper.assertTrue(!shut.isEmpty() && shut.max(net.minecraft.core.Direction.Axis.X) - shut.min(net.minecraft.core.Direction.Axis.X) > 0.9,
				"shut, the leaves close the doorway: " + shut);
		Vec3 front = Vec3.atBottomCenterOf(master).add(-0.5, 0, 1.4);       // 1.4 out from the middle of the doorway
		var zombie = helper.spawnWithNoFreeWill(net.minecraft.world.entity.EntityTypes.HUSK, helper.relativeVec(front));
		helper.startSequence()
				.thenIdle(30)
				.thenExecute(() -> {
					helper.assertTrue(!level.getBlockState(master).getValue(io.github.jimbozoomer.jugcraft.tower.SlidingDoorBlock.OPEN),
							"a monster walking up leaves it shut");
					zombie.discard();
					helper.spawnWithNoFreeWill(net.minecraft.world.entity.EntityTypes.PIG, helper.relativeVec(front));
				})
				.thenWaitUntil(() -> helper.assertTrue(level.getBlockState(master).getValue(io.github.jimbozoomer.jugcraft.tower.SlidingDoorBlock.OPEN),
						"the door opens for anyone else walking up"))
				.thenExecute(() -> {
					var open = level.getBlockState(master).getCollisionShape(level, master);
					helper.assertTrue(!open.isEmpty() && open.max(net.minecraft.core.Direction.Axis.X) - open.min(net.minecraft.core.Direction.Axis.X) < 0.07,
							"open, only the lining is left: " + open);
					var part = level.getBlockState(master.west()).getCollisionShape(level, master.west());
					helper.assertTrue(part.max(net.minecraft.core.Direction.Axis.X) - part.min(net.minecraft.core.Direction.Axis.X) < 0.07,
							"open, the second cell is clear but its lining: " + part);
					for (var pig : level.getEntitiesOfClass(net.minecraft.world.entity.animal.pig.Pig.class, new net.minecraft.world.phys.AABB(master).inflate(4))) {
						pig.discard();
					}
				})
				.thenWaitUntil(() -> helper.assertTrue(!level.getBlockState(master).getValue(io.github.jimbozoomer.jugcraft.tower.SlidingDoorBlock.OPEN),
						"the door slides shut once they have gone"))
				.thenExecute(() -> {
					level.destroyBlock(master.west().above(), false);
					for (net.minecraft.core.Vec3i o : footprint) {
						helper.assertTrue(level.getBlockState(master.offset(o)).isAir(), "broken, the door goes whole: " + o);
					}
				})
				.thenSucceed();
	}

	/**
	 * The Automatic Entrance Door (layout 47): 6 x 3 with its master at one end; mobs plan their way only through its
	 * doorway's middle cells (its sidelights' and header's cells no); shut, the doorway is closed, open its two rows are
	 * clear and the header row stays.
	 */
	@GameTest
	public void entranceDoorIsWholeAndOpensItsMiddle(GameTestHelper helper) {
		if (TestFocus.skip("tower")) {
			helper.succeed();
			return;
		}
		ServerLevel level = helper.getLevel();
		var block = (io.github.jimbozoomer.jugcraft.tower.SlidingDoorBlock) io.github.jimbozoomer.jugcraft.test.TowerGameTests.modBlock("automatic_entrance_door");
		var facing = net.minecraft.core.Direction.NORTH;
		BlockPos master = helper.absolutePos(new BlockPos(1, 1, 3));
		BlockState state = block.defaultBlockState().setValue(io.github.jimbozoomer.jugcraft.tower.SlidingDoorBlock.FACING, facing);
		level.setBlock(master, state, net.minecraft.world.level.block.Block.UPDATE_ALL);
		block.setPlacedBy(level, master, state, null, net.minecraft.world.item.ItemStack.EMPTY);
		var footprint = io.github.jimbozoomer.jugcraft.tower.SlidingDoorBlock.footprint(block.style, facing);
		helper.assertTrue(footprint.size() == 18, "the entrance is 6 x 3: " + footprint.size());
		for (net.minecraft.core.Vec3i o : footprint) {
			BlockPos at = master.offset(o);
			BlockState cell = level.getBlockState(at);
			boolean doorway = o.getY() < 2 && o.getX() >= 2 && o.getX() <= 3;
			helper.assertTrue(cell.isPathfindable(net.minecraft.world.level.pathfinder.PathComputationType.LAND) == doorway,
					"mobs plan their way through " + o + ": " + doorway);
		}
		BlockPos middle = master.east(2);
		helper.assertTrue(!level.getBlockState(middle).getCollisionShape(level, middle).isEmpty(), "shut, the doorway is closed");
		level.setBlock(master, level.getBlockState(master).setValue(io.github.jimbozoomer.jugcraft.tower.SlidingDoorBlock.OPEN, true),
				net.minecraft.world.level.block.Block.UPDATE_ALL);
		for (int dx = 2; dx <= 3; dx++) {
			for (int dy = 0; dy < 2; dy++) {
				BlockPos at = master.offset(dx, dy, 0);
				helper.assertTrue(level.getBlockState(at).getCollisionShape(level, at).isEmpty(), "open, the doorway is clear at " + dx + ", " + dy);
			}
			BlockPos head = master.offset(dx, 2, 0);
			helper.assertTrue(!level.getBlockState(head).getCollisionShape(level, head).isEmpty(), "open, the header stays at " + dx);
		}
		helper.succeed();
	}

	/**
	 * Cradle Struts (work list item 12): placed, the pair fills every cell its struts pass through (turned to its facing),
	 * each knowing its master and colliding with what of them is in it; breaking any cell takes the pair away whole. Girder
	 * blocks stacked into a column join (no plates between them) and a lone one stands on its plates.
	 */
	@GameTest(structure = "jugcraft-test:drone_depot")
	public void cradleStrutsArePlacedWholeAndGoWhole(GameTestHelper helper) {
		if (TestFocus.skip("tower")) {
			helper.succeed();
			return;
		}
		ServerLevel level = helper.getLevel();
		var block = (io.github.jimbozoomer.jugcraft.tower.CradleStrutsBlock) io.github.jimbozoomer.jugcraft.test.TowerGameTests.modBlock("cradle_struts");
		var east = net.minecraft.core.Direction.EAST;
		BlockPos master = helper.absolutePos(new BlockPos(4, 1, 4));
		BlockState state = block.defaultBlockState().setValue(io.github.jimbozoomer.jugcraft.tower.CradleStrutsBlock.FACING, east);
		level.setBlock(master, state, net.minecraft.world.level.block.Block.UPDATE_ALL);
		block.setPlacedBy(level, master, state, null, net.minecraft.world.item.ItemStack.EMPTY);
		var cells = io.github.jimbozoomer.jugcraft.tower.CradleStrutsBlock.CELLS;
		for (int i = 0; i < cells.size(); i++) {
			BlockPos at = master.offset(io.github.jimbozoomer.jugcraft.tower.CradleStrutsBlock.offset(i, east));
			BlockState cell = level.getBlockState(at);
			helper.assertTrue(i == 0 ? cell.is(block) : cell.getBlock() instanceof io.github.jimbozoomer.jugcraft.tower.CradleStrutsPartBlock
					&& io.github.jimbozoomer.jugcraft.tower.CradleStrutsPartBlock.masterPos(at, cell).equals(master), "the struts' cell " + i + ": " + cell);
			helper.assertTrue(!cell.getCollisionShape(level, at).isEmpty(), "the cell " + i + " collides with the struts");
		}
		// the pair rises 7 blocks over its foot, out along +x (facing east: the canonical -x +z turned three quarters)
		BlockPos head = master.offset(io.github.jimbozoomer.jugcraft.tower.CradleStrutsBlock.offset(cells.size() - 1, east));
		helper.assertTrue(head.getY() - master.getY() == 6 && head.getX() > master.getX(), "the heads are up and out at " + head);
		level.destroyBlock(head, false);
		for (int i = 0; i < cells.size(); i++) {
			BlockState cell = level.getBlockState(master.offset(io.github.jimbozoomer.jugcraft.tower.CradleStrutsBlock.offset(i, east)));
			helper.assertTrue(cell.isAir(), "broken, the struts leave nothing at cell " + i + ": " + cell);
		}
		// three girders stacked: the middle one joined both ways, the ends on their plates
		var girder = io.github.jimbozoomer.jugcraft.test.TowerGameTests.modBlock("steel_girder");
		BlockPos g = helper.absolutePos(new BlockPos(12, 1, 12));
		for (int y = 0; y < 3; y++) {
			level.setBlock(g.above(y), girder.defaultBlockState(), net.minecraft.world.level.block.Block.UPDATE_ALL);
		}
		for (int y = 0; y < 3; y++) {
			BlockState s = level.getBlockState(g.above(y));
			s = io.github.jimbozoomer.jugcraft.tower.TowerGirderBlock.joined(s, level, g.above(y));
			helper.assertTrue(s.getValue(io.github.jimbozoomer.jugcraft.tower.TowerGirderBlock.JOIN_LO) == (y > 0)
					&& s.getValue(io.github.jimbozoomer.jugcraft.tower.TowerGirderBlock.JOIN_HI) == (y < 2), "the girder column joins at height " + y + ": " + s);
		}
		BlockState bottom = level.getBlockState(g);
		helper.assertTrue(bottom.getValue(io.github.jimbozoomer.jugcraft.tower.TowerGirderBlock.JOIN_HI),
				"a girder set on another joins it (its neighbour's update): " + bottom);
		level.removeBlock(g.above(), false);
		helper.assertTrue(!level.getBlockState(g).getValue(io.github.jimbozoomer.jugcraft.tower.TowerGirderBlock.JOIN_HI),
				"the one over it gone, the girder ends on its cap plate again");
		helper.succeed();
	}

	/**
	 * A Big Map Table (work list item 16): placed, it fills its octagon's cells, each knowing its master and colliding with
	 * the table up to its top; breaking any cell takes the whole table away, none of its parts left behind.
	 */
	@GameTest(structure = "jugcraft-test:drone_depot")
	public void bigMapTableIsPlacedWholeAndGoesWhole(GameTestHelper helper) {
		if (TestFocus.skip("tower")) {
			helper.succeed();
			return;
		}
		ServerLevel level = helper.getLevel();
		var block = (io.github.jimbozoomer.jugcraft.tower.BigMapTableBlock) io.github.jimbozoomer.jugcraft.test.TowerGameTests.modBlock("big_map_table");
		BlockPos master = helper.absolutePos(new BlockPos(7, 1, 7));
		BlockState state = block.defaultBlockState();
		level.setBlock(master, state, net.minecraft.world.level.block.Block.UPDATE_ALL);
		block.setPlacedBy(level, master, state, null, net.minecraft.world.item.ItemStack.EMPTY);
		var cells = io.github.jimbozoomer.jugcraft.tower.BigMapTableBlock.CELLS;
		for (int i = 0; i < cells.size(); i++) {
			BlockPos at = master.offset(cells.get(i));
			BlockState cell = level.getBlockState(at);
			helper.assertTrue(i == 0 ? cell.is(block) : cell.getBlock() instanceof io.github.jimbozoomer.jugcraft.tower.BigMapTablePartBlock
					&& io.github.jimbozoomer.jugcraft.tower.BigMapTablePartBlock.masterPos(at, cell).equals(master), "the table's cell " + i + ": " + cell);
			helper.assertTrue(!cell.getCollisionShape(level, at).isEmpty(), "the cell " + i + " collides with the table");
		}
		// the chamfered corners are left free; the edges' middles are the table's
		helper.assertTrue(level.getBlockState(master.offset(6, 0, 6)).isAir() && !level.getBlockState(master.offset(6, 0, 0)).isAir(),
				"the octagon's corner cell is free, its east edge's middle is the table's");
		helper.assertTrue(level.getBlockEntity(master) instanceof io.github.jimbozoomer.jugcraft.tower.BigMapTableBlockEntity, "the table has its block entity");
		level.destroyBlock(master.offset(-6, 0, 1), false);
		for (int i = 0; i < cells.size(); i++) {
			BlockState cell = level.getBlockState(master.offset(cells.get(i)));
			helper.assertTrue(cell.isAir(), "broken, the table leaves nothing at cell " + i + ": " + cell);
		}
		helper.succeed();
	}

	/**
	 * The Big Map Table's map (tower/MapScan, work list item 16): read from the world itself, a column's ground is its
	 * top block's (a pillar its top), a block over open air a span of its own at its height over the ground under it (a
	 * floating island floats), glass is glass; and the map comes through its payload as it was.
	 */
	@GameTest(structure = "jugcraft-test:drone_depot", skyAccess = true)
	public void bigMapTableReadsTheWorldAtItsTrueHeights(GameTestHelper helper) {
		if (TestFocus.skip("tower")) {
			helper.succeed();
			return;
		}
		ServerLevel level = helper.getLevel();
		BlockPos centre = helper.absolutePos(new BlockPos(8, 1, 8));
		BlockPos pillar = helper.absolutePos(new BlockPos(3, 1, 3)), floating = helper.absolutePos(new BlockPos(12, 6, 4)),
				glass = helper.absolutePos(new BlockPos(4, 1, 12));
		var stone = net.minecraft.world.level.block.Blocks.STONE.defaultBlockState();
		// ground of its own, three thick: the test structure is open there down to the barrier the game lays under it with
		// air (sky lit from the side) below, and a floor over lit air is a span of its own on the map, not the ground
		for (int x = 1; x <= 15; x++) {
			for (int z = 1; z <= 15; z++) {
				for (int y = -2; y <= 0; y++) {
					level.setBlock(helper.absolutePos(new BlockPos(x, y, z)), stone, net.minecraft.world.level.block.Block.UPDATE_ALL);
				}
			}
		}
		for (int y = 0; y < 3; y++) {
			level.setBlock(pillar.above(y), stone, net.minecraft.world.level.block.Block.UPDATE_ALL);
		}
		level.setBlock(floating, stone, net.minecraft.world.level.block.Block.UPDATE_ALL);
		level.setBlock(glass, net.minecraft.world.level.block.Blocks.GLASS.defaultBlockState(), net.minecraft.world.level.block.Block.UPDATE_ALL);
		var scan = new io.github.jimbozoomer.jugcraft.tower.MapScan();
		var grid = scan.scanNow(level, centre, 8);
		helper.assertTrue(grid != null && grid.step == 1 && grid.n == 17, "a view of 8 is every block, 17 across: " + (grid == null ? null : grid.n));
		java.util.function.ToIntFunction<BlockPos> sample = p -> (p.getZ() - centre.getZ() + grid.n / 2) * grid.n + p.getX() - centre.getX() + grid.n / 2;
		int plain = sample.applyAsInt(helper.absolutePos(new BlockPos(8, 1, 3)));
		int floor = grid.ground[plain];
		helper.assertTrue(floor < pillar.getY() && grid.count[plain] == 0, "a plain column is its ground, under the test's floor level: " + floor);
		int p = sample.applyAsInt(pillar);
		helper.assertTrue(grid.ground[p] == pillar.getY() + 2 && grid.top[p] == net.minecraft.world.level.material.MapColor.STONE.id,
				"the pillar's column is its top, stone: " + grid.ground[p] + " " + grid.top[p]);
		int f = sample.applyAsInt(floating);
		int first = 0;
		for (int k = 0; k < f; k++) first += grid.count[k];
		helper.assertTrue(grid.count[f] == 1 && grid.y0[first] == floating.getY() && grid.y1[first] == floating.getY() + 1
				&& grid.ground[f] == floor, "the floating block floats at its height over the ground: " + grid.count[f] + " spans, ground " + grid.ground[f]);
		int g = sample.applyAsInt(glass);
		helper.assertTrue(grid.top[g] == io.github.jimbozoomer.jugcraft.tower.MapScan.GLASS && grid.ground[g] == glass.getY(), "glass shows as glass: " + grid.top[g]);
		helper.assertTrue(grid.ground[0] == Short.MIN_VALUE, "the grid's corner is out of the round view");
		var buf = new net.minecraft.network.RegistryFriendlyByteBuf(io.netty.buffer.Unpooled.buffer(), level.registryAccess());
		io.github.jimbozoomer.jugcraft.tower.MapScan.Payload.CODEC.encode(buf, new io.github.jimbozoomer.jugcraft.tower.MapScan.Payload(centre, grid));
		var back = io.github.jimbozoomer.jugcraft.tower.MapScan.Payload.CODEC.decode(buf);
		helper.assertTrue(java.util.Arrays.equals(back.grid().ground, grid.ground) && java.util.Arrays.equals(back.grid().y0, grid.y0)
				&& java.util.Arrays.equals(back.grid().top, grid.top) && back.grid().view == grid.view, "the map comes through its payload as it was");
		helper.succeed();
	}

	/**
	 * A Holo Projector (work list item 15): placed, it fills its 3 x 3 cells, each knowing its master and colliding with
	 * the slab and deck; breaking any cell of it takes the whole projector away, none of its parts left behind.
	 */
	@GameTest
	public void holoProjectorIsPlacedWholeAndGoesWhole(GameTestHelper helper) {
		if (TestFocus.skip("tower")) {
			helper.succeed();
			return;
		}
		ServerLevel level = helper.getLevel();
		var block = (io.github.jimbozoomer.jugcraft.tower.HoloProjectorBlock) io.github.jimbozoomer.jugcraft.test.TowerGameTests.modBlock("holo_projector");
		BlockPos master = helper.absolutePos(new BlockPos(3, 1, 3));
		BlockState state = block.defaultBlockState().setValue(io.github.jimbozoomer.jugcraft.tower.HoloProjectorBlock.FACING, net.minecraft.core.Direction.EAST);
		level.setBlock(master, state, net.minecraft.world.level.block.Block.UPDATE_ALL);
		block.setPlacedBy(level, master, state, null, net.minecraft.world.item.ItemStack.EMPTY);
		var footprint = io.github.jimbozoomer.jugcraft.tower.HoloProjectorBlock.FOOTPRINT;
		for (net.minecraft.core.Vec3i o : footprint) {
			BlockPos at = master.offset(o);
			BlockState cell = level.getBlockState(at);
			helper.assertTrue(o.equals(net.minecraft.core.Vec3i.ZERO) ? cell.is(block)
					: cell.getBlock() instanceof io.github.jimbozoomer.jugcraft.tower.HoloProjectorPartBlock
							&& io.github.jimbozoomer.jugcraft.tower.HoloProjectorPartBlock.masterPos(at, cell).equals(master), "the projector's cell " + o + ": " + cell);
			var shape = cell.getCollisionShape(level, at);
			helper.assertTrue(!shape.isEmpty() && Math.abs(shape.max(net.minecraft.core.Direction.Axis.Y) - io.github.jimbozoomer.jugcraft.tower.HoloProjectorBlock.DECK) < 1e-6,
					"the cell " + o + " collides with the slab and deck: " + shape);
		}
		helper.assertTrue(level.getBlockEntity(master) instanceof io.github.jimbozoomer.jugcraft.drone.DepotDisplayBlockEntity, "the projector has its block entity");
		// a corner part broken: the master goes, and with it the other parts
		level.destroyBlock(master.offset(1, 0, -1), false);
		for (net.minecraft.core.Vec3i o : footprint) {
			BlockState cell = level.getBlockState(master.offset(o));
			helper.assertTrue(cell.isAir(), "broken, the projector leaves nothing at " + o + ": " + cell);
		}
		helper.succeed();
	}

	/**
	 * Every creative tab builds (as opening the creative inventory does): no tab is handed an empty stack, such as a big
	 * piece's *_part block, which has no item (that crashed the game on 26 October 2026 when the inventory opened).
	 */
	@GameTest
	public void creativeTabsBuildWithoutEmptyStacks(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		net.minecraft.world.item.CreativeModeTabs.tryRebuildTabContents(level.enabledFeatures(), true, level.registryAccess());
		for (net.minecraft.world.item.CreativeModeTab tab : net.minecraft.world.item.CreativeModeTabs.allTabs()) {
			for (net.minecraft.world.item.ItemStack stack : tab.getDisplayItems()) {
				helper.assertTrue(!stack.isEmpty(), "an empty stack in the creative tab " + tab.getDisplayName().getString());
			}
		}
		helper.succeed();
	}

	/**
	 * The tower crew's posts (tools/tripillar_crew.py; Justin's take-2 crew sheet and the new-jobs sheet, 5 November
	 * 2026; the security detail outside and on the roof, 6 November): 41 of them, all brought in by the foundation, the
	 * manager alone in his job, eight in security; every chair post has its chair
	 * where the layout puts it (a room kit chair or a Pilot Simulator with the seat's point inside its cell) and a cell to
	 * stand in beside it, every other post its spots; every job more faces than posts (so a replacement looks new), every
	 * face's skin on the classpath, and only the janitors women (slim arms).
	 */
	@GameTest
	public void crewPostsAreSound(GameTestHelper helper) {
		if (TestFocus.skip("tower")) {
			helper.succeed();
			return;
		}
		TowerData data = TowerData.get();
		var posts = data.crew();
		helper.assertTrue(posts.size() == 47, "47 crew posts, got " + posts.size());
		java.util.Map<Long, BlockState> placed = new java.util.HashMap<>();
		for (int[] b : data.foundation.place) {
			placed.put(BlockPos.asLong(b[0], b[1], b[2]), data.state(b[3]));
		}
		java.util.Map<String, Integer> perJob = new java.util.HashMap<>();
		for (var post : posts) {
			perJob.merge(post.job(), 1, Integer::sum);
			helper.assertTrue(post.step() == 0, post.id() + ": the foundation brings it in");
			if (post.seated()) {
				BlockState chair = post.seat() == null ? null : placed.get(post.seat().asLong());
				boolean kit = post.seatAt() != null && chair != null && chair.getBlock() instanceof io.github.jimbozoomer.jugcraft.tower.RoomKitBlock
						&& BlockPos.containing(post.seatAt()).equals(post.seat());
				helper.assertTrue(chair != null && (kit || chair.getBlock() instanceof io.github.jimbozoomer.jugcraft.tower.ConsoleChairBlock
						|| chair.getBlock() instanceof io.github.jimbozoomer.jugcraft.tower.Sittable || chair.is(block("operator_chair"))),
						post.id() + ": a chair at its seat, got " + chair + (post.seatAt() == null ? "" : " (seat " + post.seatAt() + ")"));
				helper.assertTrue(post.stand() != null && !placed.containsKey(post.stand().above().asLong()), post.id() + ": room to stand beside the chair");
			} else {
				helper.assertTrue(!post.spots().isEmpty(), post.id() + ": somewhere to work");
			}
		}
		helper.assertTrue(perJob.getOrDefault("manager", 0) == 1 && perJob.getOrDefault("pilot", 0) == 4 && perJob.getOrDefault("engineer", 0) == 4
				&& perJob.getOrDefault("medic", 0) == 1, "the new jobs: 4 pilots, 4 engineers, a medic and the manager: " + perJob);
		helper.assertTrue(perJob.getOrDefault("security", 0) == 14, "fourteen in security (the crew list's C2: four inside, a patrol upstairs, two"
				+ " at the entrance, two at the inputs, three circuits outside, two on the roof): " + perJob);
		long outside = posts.stream().filter(io.github.jimbozoomer.jugcraft.tower.crew.CrewPost::outside).count();
		helper.assertTrue(outside == 3, "three walk circuits round the control building, got " + outside);
		// C2 (Justin, 9 November 2026: "make sure one security with a plasma gun is patrolling the first floor, one on the
		// second, and another on the roof"): a security patrol on each floor and the roof, and the guards facing out at the
		// entrance and the inputs
		for (int y : new int[] {1, 12, 21}) {
			helper.assertTrue(posts.stream().anyMatch(p -> p.job().equals("security") && p.kind().equals("patrol") && p.home().getY() == y),
					"a security patrol on the floor at y " + y);
		}
		for (String id : new String[] {"perimeter_west", "perimeter_east", "inputs_north", "inputs_south"}) {
			var guard = posts.stream().filter(p -> p.id().equals(id)).findFirst().orElse(null);
			helper.assertTrue(guard != null && guard.kind().equals("station") && guard.spots().size() == 1 && !Float.isNaN(guard.spots().getFirst().yaw()),
					id + ": a guard standing facing out");
		}
		// C5: each janitor has a cart parked in the custodian's closet upstairs, and a cell beside it to take it from
		for (var post : posts) {
			if (post.job().equals("janitor")) {
				BlockState cart = post.cart() == null ? null : placed.get(post.cart().asLong());
				helper.assertTrue(cart != null && cart.is(block("janitor_cart")) && post.cartStand() != null && !placed.containsKey(post.cartStand().asLong()),
						post.id() + ": its cart in the custodian's closet, got " + cart);
			}
		}
		// the outside patrols' spots: no part of the control building's ground floors within two cells
		java.util.Set<Long> footprint = new java.util.HashSet<>();
		for (int[] b : TowerData.get().foundation.place) {
			if (b[1] >= 0 && b[1] <= 4) {
				footprint.add(BlockPos.asLong(b[0], 0, b[2]));
			}
		}
		for (var post : posts) {
			if (post.outside()) {
				for (var s : post.spots()) {
					boolean clear = true;
					for (int dx = -2; dx <= 2; dx++) {
						for (int dz = -2; dz <= 2; dz++) {
							clear &= !footprint.contains(BlockPos.asLong(s.pos().getX() + dx, 0, s.pos().getZ() + dz));
						}
					}
					helper.assertTrue(clear, post.id() + ": " + s.pos() + " outside the control building");
				}
			}
		}
		for (var job : perJob.entrySet()) {
			var looks = io.github.jimbozoomer.jugcraft.tower.crew.CrewRoster.get().looks(job.getKey());
			helper.assertTrue(looks.size() > job.getValue(), job.getKey() + ": more faces than posts, got " + looks.size());
			for (var look : looks) {
				helper.assertTrue(TowerGameTests.class.getResource("/assets/jugcraft/textures/entity/tower_crew/" + look.skin() + ".png") != null,
						look.skin() + ": its skin is there");
				helper.assertTrue(look.slim() == job.getKey().equals("janitor"), look.skin() + ": the janitors are women, everyone else men");
			}
		}
		helper.succeed();
	}

	/** The drop ship flies for five or more, a whole job's staff, or the manager; not for fewer of a bigger staff. */
	@GameTest(structure = "jugcraft-test:drone_depot", maxTicks = 5)
	public void dropShipFliesForFiveAWholeStaffOrTheManager(GameTestHelper helper) {
		if (TestFocus.skip("tower")) {
			helper.succeed();
			return;
		}
		java.util.List<io.github.jimbozoomer.jugcraft.tower.crew.CrewPost> posts = new java.util.ArrayList<>();
		java.util.function.BiFunction<String, String, io.github.jimbozoomer.jugcraft.tower.crew.CrewPost> post = (id, job) ->
				new io.github.jimbozoomer.jugcraft.tower.crew.CrewPost(id, job, "station", "idle", null, null, null, java.util.List.of(), 0, null, 0F);
		for (int i = 0; i < 6; i++) {
			posts.add(post.apply("controller_" + i, "controller"));
		}
		for (int i = 0; i < 3; i++) {
			posts.add(post.apply("janitor_" + i, "janitor"));
		}
		posts.add(post.apply("manager", "manager"));
		java.util.function.Predicate<io.github.jimbozoomer.jugcraft.tower.crew.CrewPost> open = p -> true;
		var rule = (java.util.function.Function<java.util.List<io.github.jimbozoomer.jugcraft.tower.crew.CrewPost>, Boolean>) due ->
				io.github.jimbozoomer.jugcraft.tower.crew.TowerCrewCare.flies(due, posts, open);
		helper.assertTrue(!rule.apply(posts.subList(0, 3)), "three of six controllers wait for the next ship");
		helper.assertTrue(!rule.apply(posts.subList(6, 8)), "two of three janitors wait");
		helper.assertTrue(rule.apply(posts.subList(6, 9)), "all three janitors come");
		helper.assertTrue(rule.apply(posts.subList(9, 10)), "the manager comes alone");
		helper.assertTrue(rule.apply(java.util.List.of(posts.get(0), posts.get(1), posts.get(2), posts.get(6), posts.get(7))), "five of anyone come");
		helper.succeed();
	}

	/** The drop ship's timeline: in from out of sight, nose to the tower; turned round, down on its gear; the ramp down
	 * to the ground; lit and on watch while down; off back out of sight nose first. */
	@GameTest(structure = "jugcraft-test:drone_depot", maxTicks = 5)
	public void dropShipComesInLandsAndLeaves(GameTestHelper helper) {
		if (TestFocus.skip("tower")) {
			helper.succeed();
			return;
		}
		float yaw = 0.7F;
		var p = new io.github.jimbozoomer.jugcraft.tower.crew.CrewShipFlight.Pose();
		io.github.jimbozoomer.jugcraft.tower.crew.CrewShipFlight.pose(io.github.jimbozoomer.jugcraft.tower.crew.CrewShipFlight.ENTRY, 0, yaw, false, 3.55F, 300, p);
		helper.assertTrue(p.y >= 299 && Math.hypot(p.x, p.z) > 690 && p.entry > 0.99F, "comes in from far out and high, on fire: " + p.x + " " + p.y + " " + p.z);
		// it comes from beyond the landing point, away from the tower (its stern, once down, faces the tower)
		double sternX = Math.sin(yaw), sternZ = Math.cos(yaw);
		helper.assertTrue(p.x * sternX + p.z * sternZ < -600, "from the side away from the tower");
		io.github.jimbozoomer.jugcraft.tower.crew.CrewShipFlight.pose(io.github.jimbozoomer.jugcraft.tower.crew.CrewShipFlight.TOUCHDOWN, 50, yaw, false, 3.55F, 300, p);
		helper.assertTrue(Math.abs(p.y) < 1e-3 && Math.abs(p.yaw - yaw) < 1e-4 && p.gear > 0.99F && p.lights && p.guns, "down on its gear, turned round, lit, guns on watch");
		io.github.jimbozoomer.jugcraft.tower.crew.CrewShipFlight.pose(io.github.jimbozoomer.jugcraft.tower.crew.CrewShipFlight.UNLOAD, 0, yaw, false, io.github.jimbozoomer.jugcraft.tower.crew.CrewShipFlight.HINGE_Y, 300, p);
		float drop = io.github.jimbozoomer.jugcraft.tower.crew.CrewShipFlight.leafDrop(io.github.jimbozoomer.jugcraft.tower.crew.CrewShipFlight.HINGE_Y);
		helper.assertTrue(p.ramp > 0.99F && Math.abs(p.leaf - drop) < 1e-4 && drop > 0.3F && drop < 0.6F, "the ramp down: " + p.leaf);
		// the ramp swings down about its hinge from the shut back (leaning forward over the opening), never past the
		// ground, and its end comes down on the ground (the rework, 8 November 2026: no platform, no stern door)
		for (int k = 0; k <= io.github.jimbozoomer.jugcraft.tower.crew.CrewShipFlight.LENGTH[io.github.jimbozoomer.jugcraft.tower.crew.CrewShipFlight.OPEN]; k++) {
			var q = io.github.jimbozoomer.jugcraft.tower.crew.CrewShipFlight.pose(io.github.jimbozoomer.jugcraft.tower.crew.CrewShipFlight.OPEN, k, yaw, false, io.github.jimbozoomer.jugcraft.tower.crew.CrewShipFlight.HINGE_Y, 300, new io.github.jimbozoomer.jugcraft.tower.crew.CrewShipFlight.Pose());
			helper.assertTrue(q.leaf >= io.github.jimbozoomer.jugcraft.tower.crew.CrewShipFlight.SHUT - 1e-4 && q.leaf <= drop + 1e-4, "between shut and down, tick " + k + ": " + q.leaf);
		}
		var shut = io.github.jimbozoomer.jugcraft.tower.crew.CrewShipFlight.pose(io.github.jimbozoomer.jugcraft.tower.crew.CrewShipFlight.HOVER, 10, yaw, false, io.github.jimbozoomer.jugcraft.tower.crew.CrewShipFlight.HINGE_Y, 300, new io.github.jimbozoomer.jugcraft.tower.crew.CrewShipFlight.Pose());
		helper.assertTrue(Math.abs(shut.leaf - io.github.jimbozoomer.jugcraft.tower.crew.CrewShipFlight.SHUT) < 1e-5 && io.github.jimbozoomer.jugcraft.tower.crew.CrewShipFlight.rampZ(shut.leaf, io.github.jimbozoomer.jugcraft.tower.crew.CrewShipFlight.LEAF) < io.github.jimbozoomer.jugcraft.tower.crew.CrewShipFlight.HINGE, "shut: its end up and forward");
		double end = io.github.jimbozoomer.jugcraft.tower.crew.CrewShipFlight.rampY(drop, io.github.jimbozoomer.jugcraft.tower.crew.CrewShipFlight.LEAF);
		helper.assertTrue(Math.abs(end) < 1e-3 && Math.abs(io.github.jimbozoomer.jugcraft.tower.crew.CrewShipFlight.rampY(drop, 0) - (io.github.jimbozoomer.jugcraft.tower.crew.CrewShipFlight.G + 1.0)) < 0.05, "its end on the ground, its root level with the bay's floor: " + end);
		io.github.jimbozoomer.jugcraft.tower.crew.CrewShipFlight.pose(io.github.jimbozoomer.jugcraft.tower.crew.CrewShipFlight.TOUCHDOWN, 50, yaw, true, 2.63F, 300, p);
		helper.assertTrue(p.gear < 1e-3 && Math.abs(p.y) < 1e-3, "at the roof it holds its hover, gear up");
		io.github.jimbozoomer.jugcraft.tower.crew.CrewShipFlight.pose(io.github.jimbozoomer.jugcraft.tower.crew.CrewShipFlight.CLIMB, 240, yaw, false, 3.55F, 300, p);
		helper.assertTrue(p.y >= 299 && p.x * -sternX + p.z * -sternZ > 590 && !p.lights, "away nose first, high, the lights out");
		helper.succeed();
	}

	/** A drop ship set down to stay shoots a player who comes inside its keep-out ring; not one outside, even one who has
	 * hurt the crew (since 7 November 2026 its guns stay inside the ring while it's down: Justin's pick). */
	@GameTest(environment = "jugcraft-test:drop_ship_ring", structure = "jugcraft-test:drone_depot", maxTicks = 600)
	public void parkedDropShipShootsInsideItsRingAndWhoeverHurtsTheCrew(GameTestHelper helper) {
		if (TestFocus.skip("tower")) {
			helper.succeed();
			return;
		}
		clearStrays(helper);
		helper.assertTrue(io.github.jimbozoomer.jugcraft.tower.crew.CrewShipFlight.inZone(-28, 0) && !io.github.jimbozoomer.jugcraft.tower.crew.CrewShipFlight.inZone(-31, 0)
				&& io.github.jimbozoomer.jugcraft.tower.crew.CrewShipFlight.inZone(0, 39) && !io.github.jimbozoomer.jugcraft.tower.crew.CrewShipFlight.inZone(0, 41)
				&& io.github.jimbozoomer.jugcraft.tower.crew.CrewShipFlight.inZone(0, -39) && !io.github.jimbozoomer.jugcraft.tower.crew.CrewShipFlight.inZone(0, -41),
				"the ring: 10 blocks out past the wings, the nose and the ramp's foot");
		ServerLevel level = helper.getLevel();
		BlockPos land = helper.absolutePos(new BlockPos(4, 1, 4));
		keepLoaded(helper, land, 48, true);
		var ship = JugcraftTower.CREW_SHIP.create(level, net.minecraft.world.entity.EntitySpawnReason.TRIGGERED);
		helper.assertTrue(ship != null, "a ship");
		ship.park(net.minecraft.world.phys.Vec3.atBottomCenterOf(land), 0F, level.getGameTime());
		level.addFreshEntity(ship);
		var inside = survivalPlayer(helper);
		inside.snapTo(land.getX() - 18.5, land.getY() + 3, land.getZ() + 0.5, 0, 0);
		inside.setNoGravity(true);
		var outside = survivalPlayer(helper);
		outside.snapTo(land.getX() + 33.5, land.getY() + 3, land.getZ() + 0.5, 0, 0);
		outside.setNoGravity(true);
		float full = outside.getHealth();
		helper.startSequence()
				.thenWaitUntil(() -> helper.assertTrue(inside.getHealth() < inside.getMaxHealth() || !inside.isAlive(), "the player inside the ring is shot"))
				.thenExecute(() -> helper.assertTrue(outside.getHealth() == full, "the one outside, who did nothing, isn't"))
				.thenExecute(() -> ship.angerAt(outside, level.getGameTime() + 600))
				.thenIdle(100)
				.thenExecute(() -> helper.assertTrue(outside.getHealth() == full, "nor once they've hurt the crew: the guns stay inside the ring"))
				.thenExecute(() -> {
					ship.discard();
					leave(inside);
					leave(outside);
					keepLoaded(helper, land, 48, false);
				})
				.thenSucceed();
	}

	/**
	 * Walkers go round the mod's pieces instead of into them (Justin, 6 November 2026: "There's this dude who's just like
	 * bouncing up and down in front of a window"; tower/crew/CrewPaths, mixin/PiecePathMixin): to the game's path finder a
	 * Framed Glass window, a room kit chair and a service cradle are in the way, as a whole block is; a floor emitter (lower
	 * than a step) is still walked over; the game's own blocks are left as they were.
	 */
	@GameTest(structure = "jugcraft-test:drone_depot", maxTicks = 5)
	public void piecesAreInTheWayForWalkers(GameTestHelper helper) {
		if (TestFocus.skip("tower")) {
			helper.succeed();
			return;
		}
		var land = net.minecraft.world.level.pathfinder.PathComputationType.LAND;
		helper.assertTrue(!block("framed_glass").defaultBlockState().isPathfindable(land), "a Framed Glass window is in the way");
		helper.assertTrue(!block("task_chair").defaultBlockState().isPathfindable(land), "a chair is in the way");
		helper.assertTrue(!block("drone_display_stand").defaultBlockState().setValue(io.github.jimbozoomer.jugcraft.tower.HallFixtureBlock.Stand.STYLE,
				io.github.jimbozoomer.jugcraft.tower.HallFixtureBlock.Style.SERVICE).isPathfindable(land), "a service cradle is in the way");
		helper.assertTrue(block("holo_emitter").defaultBlockState().isPathfindable(land), "a floor emitter (a step high) is walked over");
		helper.assertTrue(!io.github.jimbozoomer.jugcraft.tower.crew.CrewPaths.obstacle(Blocks.ANVIL.defaultBlockState())
				&& !io.github.jimbozoomer.jugcraft.tower.crew.CrewPaths.obstacle(Blocks.OAK_FENCE.defaultBlockState()), "the game's own blocks are left to themselves");
		helper.succeed();
	}

	/**
	 * The service bay's lifted cradle is solid all round (Justin, 6 November 2026: "make the drone platform collidable.
	 * Don't be able to walk through this little drone platform"): it stops a player-sized box in the cell diagonally off
	 * its stand as well as straight across (mixin/LargeShapeEdgeMixin), a cell further out is clear, and the crew's path
	 * finder keeps out from under it (CrewPaths.underCradle).
	 */
	@GameTest(structure = "jugcraft-test:drone_depot", maxTicks = 5)
	public void serviceCradleIsSolidAtItsCorners(GameTestHelper helper) {
		if (TestFocus.skip("tower")) {
			helper.succeed();
			return;
		}
		ServerLevel level = helper.getLevel();
		BlockPos stand = helper.absolutePos(new BlockPos(5, 3, 5));
		for (BlockPos p : BlockPos.betweenClosed(stand.offset(-3, 0, -3), stand.offset(3, 2, 3))) {
			level.setBlock(p, Blocks.AIR.defaultBlockState(), net.minecraft.world.level.block.Block.UPDATE_ALL);
		}
		level.setBlock(stand, block("drone_display_stand").defaultBlockState()
				.setValue(io.github.jimbozoomer.jugcraft.tower.HallFixtureBlock.Stand.STYLE, io.github.jimbozoomer.jugcraft.tower.HallFixtureBlock.Style.SERVICE)
				.setValue(net.minecraft.world.level.block.HorizontalDirectionalBlock.FACING, Direction.NORTH), net.minecraft.world.level.block.Block.UPDATE_ALL);
		double x = stand.getX(), y = stand.getY(), z = stand.getZ();
		net.minecraft.world.phys.AABB corner = new net.minecraft.world.phys.AABB(x + 1.05, y + 0.05, z + 1.05, x + 1.45, y + 1.8, z + 1.45);
		net.minecraft.world.phys.AABB across = new net.minecraft.world.phys.AABB(x + 1.05, y + 0.05, z + 0.3, x + 1.45, y + 1.8, z + 0.7);
		helper.assertTrue(!level.noCollision(corner), "the cradle stops a player in the cell diagonally off its stand");
		helper.assertTrue(!level.noCollision(across), "and in the cell straight across");
		helper.assertTrue(level.noCollision(corner.move(1.2, 0, 0)), "a cell further out is clear");
		var crew = JugcraftTower.TOWER_CREW.create(level, net.minecraft.world.entity.EntitySpawnReason.TRIGGERED);
		helper.assertTrue(crew != null, "a crew member");
		crew.snapTo(x + 3.5, y, z + 3.5, 0, 0);
		var context = new net.minecraft.world.level.pathfinder.PathfindingContext(level, crew);
		helper.assertTrue(io.github.jimbozoomer.jugcraft.tower.crew.CrewPaths.underCradle(context, stand.getX() + 1, stand.getY(), stand.getZ() + 1)
				&& io.github.jimbozoomer.jugcraft.tower.crew.CrewPaths.underCradle(context, stand.getX() - 1, stand.getY(), stand.getZ()),
				"the crew keep out from under the cradle");
		helper.assertTrue(!io.github.jimbozoomer.jugcraft.tower.crew.CrewPaths.underCradle(context, stand.getX() + 2, stand.getY(), stand.getZ() + 2),
				"and walk two cells off it");
		helper.succeed();
	}

	/**
	 * The crew's path finder (Justin, 6 November 2026: one of the guards was "just jumping up and down outside of the
	 * pass-through thing"; tower/crew/CrewPaths): a speed gate's lane is a way through for them (its doors open for the
	 * crew from either side), the top of a chair is nowhere to stand, and a floor is somewhere to walk; and a crew member
	 * pressed against something hops at most twice before giving it up (TowerCrew.jumpFromGround).
	 */
	@GameTest(structure = "jugcraft-test:drone_depot", maxTicks = 5)
	public void crewWalkThroughSpeedGatesAndNotOverFurniture(GameTestHelper helper) {
		if (TestFocus.skip("tower")) {
			helper.succeed();
			return;
		}
		ServerLevel level = helper.getLevel();
		BlockPos lane = helper.absolutePos(new BlockPos(3, 3, 3)), chair = helper.absolutePos(new BlockPos(6, 3, 3));
		for (BlockPos p : BlockPos.betweenClosed(lane.offset(-1, 0, -1), chair.offset(1, 3, 1))) {
			level.setBlock(p, Blocks.AIR.defaultBlockState(), net.minecraft.world.level.block.Block.UPDATE_ALL);
		}
		for (BlockPos p : BlockPos.betweenClosed(lane.offset(-1, -1, -1), chair.offset(1, -1, 1))) {
			level.setBlock(p, Blocks.SMOOTH_STONE.defaultBlockState(), net.minecraft.world.level.block.Block.UPDATE_ALL);
		}
		level.setBlock(lane, io.github.jimbozoomer.jugcraft.nuclear.PlantInterior.BLOCKS.get("speed_gate").defaultBlockState(),
				net.minecraft.world.level.block.Block.UPDATE_ALL);
		level.setBlock(chair, block("task_chair").defaultBlockState(), net.minecraft.world.level.block.Block.UPDATE_ALL);
		var crew = JugcraftTower.TOWER_CREW.create(level, net.minecraft.world.entity.EntitySpawnReason.TRIGGERED);
		helper.assertTrue(crew != null, "a crew member");
		crew.snapTo(lane.getX() + 0.5, lane.getY(), lane.getZ() - 1.5, 0, 0);
		var context = new net.minecraft.world.level.pathfinder.PathfindingContext(level, crew);
		var evaluator = (net.minecraft.world.level.pathfinder.WalkNodeEvaluator) crew.getNavigation().getNodeEvaluator();
		var walkable = net.minecraft.world.level.pathfinder.PathType.WALKABLE;
		helper.assertTrue(evaluator.getPathType(context, lane.getX(), lane.getY(), lane.getZ()) == walkable, "the gate's lane is a way through for the crew");
		// shut, too, for someone two cells tall: the cell over the lane is head room, not the top of the gate to stand on
		// (10 November 2026: it was blocked there, so no walk was planned through shut gates, and the walk map had no way
		// in at the entrance)
		helper.assertTrue(!level.getBlockState(lane).getValue(io.github.jimbozoomer.jugcraft.nuclear.SpeedGateBlock.OPEN)
				&& !level.getBlockState(lane).getCollisionShape(level, lane).isEmpty(), "the gate is shut");
		helper.assertTrue(evaluator.getPathType(context, lane.getX(), lane.getY() + 1, lane.getZ()) == net.minecraft.world.level.pathfinder.PathType.OPEN,
				"over the lane is head room: " + evaluator.getPathType(context, lane.getX(), lane.getY() + 1, lane.getZ()));
		evaluator.prepare(new net.minecraft.world.level.PathNavigationRegion(level, lane.offset(-4, -2, -4), lane.offset(6, 4, 4)), crew);
		var whole = evaluator.getPathTypeOfMob(context, lane.getX(), lane.getY(), lane.getZ(), crew);
		evaluator.done();
		helper.assertTrue(whole == walkable, "the shut gate's lane is a way through for the whole of them: " + whole);
		helper.assertTrue(evaluator.getPathType(context, chair.getX(), chair.getY() + 1, chair.getZ()) != walkable, "the top of a chair is nowhere to stand");
		helper.assertTrue(evaluator.getPathType(context, chair.getX() + 1, chair.getY(), chair.getZ()) == walkable, "the floor beside it is");
		helper.succeed();
	}

	/**
	 * C1 (Justin, 8 November 2026: the crew "rarely go up to the second floor"): the mod's stairs are a way up for the crew,
	 * as the hall's concrete stairs to the upper floor must be. A crew member at the foot of a flight of reinforced concrete
	 * stairs finds a walk to the floor at its top, and each step is somewhere to stand.
	 */
	@GameTest(structure = "jugcraft-test:drone_depot", maxTicks = 60)
	public void crewWalkUpTheStairs(GameTestHelper helper) {
		if (TestFocus.skip("tower")) {
			helper.succeed();
			return;
		}
		ServerLevel level = helper.getLevel();
		BlockPos base = helper.absolutePos(new BlockPos(2, 1, 4));
		int rise = 4;
		// a clear room, a floor, a flight of stairs east up to a landing four high
		for (BlockPos p : BlockPos.betweenClosed(base.offset(-2, 0, -1), base.offset(rise + 4, rise + 3, 1))) {
			level.setBlock(p, Blocks.AIR.defaultBlockState(), net.minecraft.world.level.block.Block.UPDATE_ALL);
		}
		for (BlockPos p : BlockPos.betweenClosed(base.offset(-2, -1, -1), base.offset(rise + 4, -1, 1))) {
			level.setBlock(p, Blocks.SMOOTH_STONE.defaultBlockState(), net.minecraft.world.level.block.Block.UPDATE_ALL);
		}
		var stairs = net.minecraft.core.registries.BuiltInRegistries.BLOCK.getValue(io.github.jimbozoomer.jugcraft.Jugcraft.id("reinforced_concrete_stairs"));
		BlockState step = stairs.defaultBlockState().setValue(net.minecraft.world.level.block.StairBlock.FACING, net.minecraft.core.Direction.EAST);
		for (int i = 0; i < rise; i++) {
			level.setBlock(base.offset(i, i, 0), step, net.minecraft.world.level.block.Block.UPDATE_ALL);
			for (int y = 0; y < i; y++) {
				level.setBlock(base.offset(i, y, 0), Blocks.SMOOTH_STONE.defaultBlockState(), net.minecraft.world.level.block.Block.UPDATE_ALL);
			}
		}
		for (int x = rise; x <= rise + 3; x++) {
			for (int y = 0; y < rise; y++) {
				level.setBlock(base.offset(x, y, 0), Blocks.SMOOTH_STONE.defaultBlockState(), net.minecraft.world.level.block.Block.UPDATE_ALL);
			}
		}
		helper.assertFalse(io.github.jimbozoomer.jugcraft.tower.crew.CrewPaths.obstacle(step), "stairs aren't in a walker's way");
		var crew = JugcraftTower.TOWER_CREW.create(level, net.minecraft.world.entity.EntitySpawnReason.TRIGGERED);
		helper.assertTrue(crew != null, "a crew member");
		crew.snapTo(base.getX() - 1.5, base.getY(), base.getZ() + 0.5, 0, 0);
		crew.setNoAi(false);
		level.addFreshEntity(crew);
		BlockPos top = base.offset(rise + 2, rise, 0);
		helper.startSequence()
				.thenWaitUntil(() -> helper.assertTrue(crew.onGround(), "the crew member stands on the floor"))
				.thenExecute(() -> {
					var path = crew.getNavigation().createPath(top, 0);
					helper.assertTrue(path != null && path.canReach(), "a walk up the stairs to the floor at the top: "
							+ (path == null ? "none" : path.getEndNode()));
					crew.discard();
					helper.succeed();
				});
	}

	/**
	 * A landed drop ship is solid (Justin, 6 November 2026: "when I fly into the ship, I'm just flying through it"): its
	 * hull is there to stand against on both sides of its flank, its bay is open inside, and a mob walked at it from the
	 * side stops at the sponson instead of passing in; the floodlights include the ones pointing out all round, and the
	 * ramp's hinge is where the drawn ramp hangs (the stern).
	 */
	@GameTest(environment = "jugcraft-test:drop_ship_solid", structure = "jugcraft-test:drone_depot", maxTicks = 100)
	public void landedDropShipIsSolid(GameTestHelper helper) {
		if (TestFocus.skip("tower")) {
			helper.succeed();
			return;
		}
		clearStrays(helper);
		helper.assertTrue(io.github.jimbozoomer.jugcraft.tower.crew.CrewShip.LAMPS.length == 17
				&& Math.abs(io.github.jimbozoomer.jugcraft.tower.crew.CrewShipFlight.HINGE - 19.0F) < 1e-4,
				"17 floodlights (10 of them out all round); the ramp hangs at the bay floor's aft edge");
		ServerLevel level = helper.getLevel();
		BlockPos land = helper.absolutePos(new BlockPos(4, 1, 4));
		var ship = JugcraftTower.CREW_SHIP.create(level, net.minecraft.world.entity.EntitySpawnReason.TRIGGERED);
		helper.assertTrue(ship != null, "a ship");
		net.minecraft.world.phys.Vec3 at = net.minecraft.world.phys.Vec3.atBottomCenterOf(land);
		keepLoaded(helper, land, 26, true);
		ship.park(at, 0F, level.getGameTime());
		level.addFreshEntity(ship);
		var zombie = net.minecraft.world.entity.EntityTypes.ZOMBIE.create(level, net.minecraft.world.entity.EntitySpawnReason.TRIGGERED);
		keepInTest(zombie);
		helper.assertTrue(zombie != null, "a zombie");
		zombie.snapTo(at.x - 12.0, at.y + 4.0, at.z - 5.0, 0, 0);     // off the flank, clear of the door
		zombie.setNoAi(true);
		zombie.setNoGravity(true);
		level.addFreshEntity(zombie);
		helper.startSequence()
				.thenWaitUntil(() -> helper.assertTrue(io.github.jimbozoomer.jugcraft.tower.crew.CrewShipHull.solidAt(ship, at.add(-9.42, 5.0, -5)),
						"the port sponson's flank is solid"))
				.thenExecute(() -> {
					helper.assertTrue(io.github.jimbozoomer.jugcraft.tower.crew.CrewShipHull.solidAt(ship, at.add(9.42, 5.0, -5)), "and the starboard one's");
					helper.assertTrue(!io.github.jimbozoomer.jugcraft.tower.crew.CrewShipHull.solidAt(ship, at.add(0, 4.5, 5)), "the bay is open inside");
					zombie.move(net.minecraft.world.entity.MoverType.SELF, new net.minecraft.world.phys.Vec3(6, 0, 0));
					helper.assertTrue(zombie.getX() < at.x - 9.4 - 0.25, "the mob walked at the flank stops at it: x " + (zombie.getX() - at.x));
					zombie.discard();
					ship.discard();
					keepLoaded(helper, land, 26, false);
				})
				.thenSucceed();
	}

	/**
	 * A core set without a player placing it (by a command, or in a world from before the tower list) lists itself in its
	 * dimension's TowerRegistry once it loads, so its crew come, nothing spawns in it of itself and /crewship finds it
	 * (Justin, 6 November 2026: "no tower with a crew near ... Is it just not detecting the one that's already placed?");
	 * and a mob that is in it (walked in, or from a spawn egg) stays (Justin: "I want to be able to manually spawn mobs in
	 * here ... to test out security's working ... I shouldn't like just delete any mob that walks in there").
	 */
	@GameTest(structure = TriPillarArena.ARENA, maxTicks = 200, skyAccess = true)
	public void towerListsItselfAndLeavesMobsThatComeIn(GameTestHelper helper) {
		if (TestFocus.skip("tower")) {
			helper.succeed();
			return;
		}
		ServerLevel level = helper.getLevel();
		BlockPos at = helper.absolutePos(TriPillarArena.CORE);
		level.setBlock(at, JugcraftTower.CORE.defaultBlockState(), 3);
		var registry = io.github.jimbozoomer.jugcraft.tower.TowerRegistry.get(level);
		var zombie = held(level, net.minecraft.world.entity.EntityTypes.ZOMBIE, at.offset(6, 1, 6));
		var pig = held(level, net.minecraft.world.entity.EntityTypes.PIG, at.offset(-6, 1, 6));
		long start = level.getGameTime();
		helper.startSequence()
				.thenWaitUntil(() -> helper.assertTrue(registry.allCores().contains(at), "the core lists itself"))
				.thenExecute(() -> helper.assertTrue(registry.inTower(level, at.offset(6, 1, 6)), "the hall's floor is in the tower (nothing spawns there)"))
				.thenWaitUntil(() -> helper.assertTrue(level.getGameTime() - start > 100, "a while"))
				.thenExecute(() -> {
					helper.assertTrue(!zombie.isRemoved() && !pig.isRemoved(), "the mobs in the hall are still there");
					zombie.discard();
					pig.discard();
					registry.release(at);
				})
				.thenSucceed();
	}

	/** A mob of {@code type} at {@code at}, held still (no AI, no falling), as the world would have it (not kept). */
	private static <T extends net.minecraft.world.entity.Mob> T held(ServerLevel level, net.minecraft.world.entity.EntityType<T> type, BlockPos at) {
		T mob = type.create(level, net.minecraft.world.entity.EntitySpawnReason.NATURAL);
		mob.snapTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, 0, 0);
		mob.setNoAi(true);
		mob.setNoGravity(true);
		mob.setPersistenceRequired();        // not the game's despawning with no player near (which isn't the tower's doing)
		// nor burning up in the sun on the open arena
		mob.setItemSlot(net.minecraft.world.entity.EquipmentSlot.HEAD, new ItemStack(net.minecraft.world.item.Items.LEATHER_HELMET));
		level.addFreshEntity(mob);
		return mob;
	}

	/**
	 * C4 (Justin, 9 November 2026: "seated, security stay"): the crew a drop ship brings ride in seated (in a seat of their
	 * own at the bay's seat, still); called off, they get up and walk out along the aisle and down the ramp; its security
	 * then hold the ramp's foot, rifle ready, till it lifts off.
	 */
	// (in a batch of its own: its armed guard engages any hostile in range, other tests' too, and its ship's hull blocks shots)
	@GameTest(environment = "jugcraft-test:drop_ship_alone", structure = "jugcraft-test:drone_depot", maxTicks = 700)
	public void dropShipCrewRideInSeatedAndSecurityHoldTheRampTillLiftOff(GameTestHelper helper) {
		if (TestFocus.skip("tower")) {
			helper.succeed();
			return;
		}
		ServerLevel level = helper.getLevel();
		BlockPos land = helper.absolutePos(new BlockPos(4, 1, 4));
		var ship = JugcraftTower.CREW_SHIP.create(level, net.minecraft.world.entity.EntitySpawnReason.TRIGGERED);
		helper.assertTrue(ship != null, "a ship");
		var entrance = net.minecraft.world.phys.Vec3.atBottomCenterOf(land.north(6));
		var site = new io.github.jimbozoomer.jugcraft.tower.crew.CrewShipSite.Site(net.minecraft.world.phys.Vec3.atBottomCenterOf(land), 0F, false,
				io.github.jimbozoomer.jugcraft.tower.crew.CrewShipFlight.HINGE_Y, entrance);
		ship.setUp(land.offset(0, 0, 60), site, java.util.List.of(), java.util.List.of(), level.getGameTime());
		level.addFreshEntity(ship);
		ship.testPhase(io.github.jimbozoomer.jugcraft.tower.crew.CrewShipFlight.UNLOAD);
		ship.testHold(true);
		// the bay and the ramp reach well south of the test's own chunks: kept ticking for the walk off it
		java.util.function.Consumer<Boolean> force = on -> {
			for (int cx = (land.getX() - 24) >> 4; cx <= (land.getX() + 24) >> 4; cx++) {
				for (int cz = (land.getZ() - 24) >> 4; cz <= (land.getZ() + 48) >> 4; cz++) {
					level.setChunkForced(cx, cz, on);
				}
			}
		};
		force.accept(true);
		helper.assertTrue(io.github.jimbozoomer.jugcraft.tower.crew.CrewShip.SEATS.length == 40, "forty seats: " + io.github.jimbozoomer.jugcraft.tower.crew.CrewShip.SEATS.length);
		for (int i = 0; i < 46; i++) {
			for (int j = i + 1; j < 46; j++) {
				helper.assertTrue(ship.seatAt(i).distanceTo(ship.seatAt(j)) > 0.9, "places " + i + " and " + j + " apart");
			}
		}
		var guardPost = new io.github.jimbozoomer.jugcraft.tower.crew.CrewPost("test_guard", "security", "station", "idle", null, null, null,
				java.util.List.of(new io.github.jimbozoomer.jugcraft.tower.crew.CrewPost.Spot(new BlockPos(0, 0, -6), 0F)), 0, null, 0F);
		var crew = JugcraftTower.TOWER_CREW.create(level, net.minecraft.world.entity.EntitySpawnReason.TRIGGERED);
		helper.assertTrue(crew != null, "a crew member");
		BlockPos core = land;
		var hire = io.github.jimbozoomer.jugcraft.tower.crew.CrewRoster.get().hire("security", guardPost.id(), 0, core.asLong(), java.util.List.of());
		var seat = ship.seatAt(0);
		crew.snapTo(seat.x, seat.y, seat.z, ship.seatYaw(0), 0);
		crew.setUp(core, guardPost, hire);
		crew.setTestNoCore();
		crew.board(seat, ship.seatYaw(0), true, ship.getUUID());
		level.addFreshEntity(crew);
		crew.seatAboard();
		helper.assertTrue(crew.armed(), "security wear the power armour (and carry the rifle)");
		var foot = ship.wayOff().get(ship.wayOff().size() - 1);
		long[] since = {0};
		helper.startSequence()
				.thenIdle(20)
				.thenExecute(() -> {
					helper.assertTrue(crew.isPassenger() && crew.aboard(), "sits aboard");
					helper.assertTrue(crew.position().distanceTo(seat.subtract(0, 0.6, 0)) < 0.15, "in the seat: " + crew.position() + " vs " + seat);
					crew.disembark(ship.wayFrom(0), entrance, ship.getUUID());
					helper.assertTrue(!crew.isPassenger() && !crew.aboard() && crew.onRamp(), "up, and on the way off");
				})
				.thenWaitUntil(() -> helper.assertTrue(!crew.onRamp(), "off the ramp"))
				.thenExecute(() -> {
					helper.assertTrue(crew.position().distanceTo(foot) < 1.2, "past the ramp's foot: " + crew.position() + " vs " + foot);
					helper.assertTrue(crew.escorting(), "the ship's own security stand guard");
				})
				.thenWaitUntil(() -> {
					var spot = ship.escortSpot(level, 0);
					double dx = crew.getX() - spot.x, dz = crew.getZ() - spot.z;
					helper.assertTrue(dx * dx + dz * dz < 2.5 * 2.5 && crew.act() == io.github.jimbozoomer.jugcraft.tower.crew.TowerCrew.ACT_READY,
							"at the ramp's foot, rifle ready: " + crew.position() + " vs " + spot);
				})
				.thenIdle(60)
				.thenExecute(() -> {
					helper.assertTrue(crew.escorting() && crew.act() == io.github.jimbozoomer.jugcraft.tower.crew.TowerCrew.ACT_READY, "still on guard while it's down");
					ship.testHold(false);
					ship.testPhase(io.github.jimbozoomer.jugcraft.tower.crew.CrewShipFlight.LIFT);
				})
				.thenWaitUntil(() -> helper.assertTrue(crew.act() != io.github.jimbozoomer.jugcraft.tower.crew.TowerCrew.ACT_READY, "stood down once it lifts off"))
				.thenExecute(() -> {
					crew.discard();
					ship.discard();
					force.accept(false);
				})
				.thenSucceed();
	}

	/**
	 * C5 (Justin, 9 November 2026: "custodian room only"): a janitor's cart is parked in the custodian's closet; they fetch it
	 * before work (its blocks lifted, the cart in their hands) and park it back, as it stood, at bedtime.
	 */
	@GameTest(structure = "jugcraft-test:drone_depot", maxTicks = 600)
	public void janitorsFetchTheirCartsAndParkThemAtNight(GameTestHelper helper) {
		if (TestFocus.skip("tower")) {
			helper.succeed();
			return;
		}
		ServerLevel level = helper.getLevel();
		BlockPos core = helper.absolutePos(new BlockPos(1, 1, 1));
		for (int x = 1; x <= 9; x++) {
			for (int z = 1; z <= 10; z++) {
				level.setBlock(helper.absolutePos(new BlockPos(x, 0, z)), Blocks.STONE.defaultBlockState(), net.minecraft.world.level.block.Block.UPDATE_CLIENTS);
				for (int y = 1; y <= 3; y++) {
					level.setBlock(helper.absolutePos(new BlockPos(x, y, z)), Blocks.AIR.defaultBlockState(), net.minecraft.world.level.block.Block.UPDATE_CLIENTS);
				}
			}
		}
		BlockPos cart0 = helper.absolutePos(new BlockPos(6, 1, 3)), cart1 = cart0.west();
		BlockState cart = block("janitor_cart").defaultBlockState().setValue(net.minecraft.world.level.block.HorizontalDirectionalBlock.FACING, Direction.SOUTH);
		var part = (net.minecraft.world.level.block.state.properties.IntegerProperty) cart.getBlock().getStateDefinition().getProperty("part");
		int flags = net.minecraft.world.level.block.Block.UPDATE_CLIENTS | net.minecraft.world.level.block.Block.UPDATE_KNOWN_SHAPE;
		level.setBlock(cart0, cart.setValue(part, 0), flags);
		level.setBlock(cart1, cart.setValue(part, 1), flags);
		BlockState was0 = level.getBlockState(cart0), was1 = level.getBlockState(cart1);
		BlockPos stand = cart0.south(), bed = helper.absolutePos(new BlockPos(8, 1, 9));
		var sleep = new io.github.jimbozoomer.jugcraft.tower.crew.CrewPost.Rest(bed.subtract(core), net.minecraft.world.phys.Vec3.atBottomCenterOf(bed.subtract(core)),
				0F, "doze", bed.north().subtract(core), 12000, 0, -1);
		var post = new io.github.jimbozoomer.jugcraft.tower.crew.CrewPost("test_janitor", "janitor", "patrol", "push", "cart", null, null,
				java.util.List.of(new io.github.jimbozoomer.jugcraft.tower.crew.CrewPost.Spot(helper.absolutePos(new BlockPos(2, 1, 8)).subtract(core), Float.NaN),
						new io.github.jimbozoomer.jugcraft.tower.crew.CrewPost.Spot(helper.absolutePos(new BlockPos(8, 1, 7)).subtract(core), Float.NaN)),
				0, null, 0F, false, sleep, java.util.List.of(), cart0.subtract(core), stand.subtract(core));
		var crew = JugcraftTower.TOWER_CREW.create(level, net.minecraft.world.entity.EntitySpawnReason.TRIGGERED);
		helper.assertTrue(crew != null, "a janitor");
		BlockPos start = helper.absolutePos(new BlockPos(2, 1, 9));
		crew.snapTo(start.getX() + 0.5, start.getY(), start.getZ() + 0.5, 0, 0);
		crew.setUp(core, post, io.github.jimbozoomer.jugcraft.tower.crew.CrewRoster.get().hire("janitor", post.id(), 0, core.asLong(), java.util.List.of()));
		crew.setTestNoCore();
		crew.setTestClock(1000);
		level.addFreshEntity(crew);
		var state = io.github.jimbozoomer.jugcraft.tower.crew.TowerCrewState.get(level);
		state.hired(core, post.id(), crew.getUUID(), crew.skin());
		helper.startSequence()
				.thenExecute(() -> helper.assertTrue(crew.heldProp() != io.github.jimbozoomer.jugcraft.tower.crew.TowerCrew.PROP_CART, "no cart till they've fetched it"))
				.thenWaitUntil(() -> helper.assertTrue(crew.hasCart() && crew.heldProp() == io.github.jimbozoomer.jugcraft.tower.crew.TowerCrew.PROP_CART,
						"fetched the cart: " + crew.position()))
				.thenExecute(() -> {
					helper.assertTrue(level.getBlockState(cart0).isAir() && level.getBlockState(cart1).isAir(), "out of the closet (both its blocks)");
					crew.setTestClock(13000);                  // bedtime
				})
				.thenWaitUntil(() -> helper.assertTrue(!crew.hasCart() && level.getBlockState(cart0).equals(was0) && level.getBlockState(cart1).equals(was1),
						"parked back as it stood: " + level.getBlockState(cart0) + " / " + level.getBlockState(cart1)))
				.thenExecute(() -> {
					helper.assertTrue(crew.heldProp() != io.github.jimbozoomer.jugcraft.tower.crew.TowerCrew.PROP_CART, "not in their hands");
					crew.discard();
					state.forget(core);
				})
				.thenSucceed();
	}

	/** The drop ship's floodlights put light blocks in the air while it's down and take them all away as it lifts; its
	 * door gun burns a hostile mob on its side. */
	@GameTest(environment = "jugcraft-test:drop_ship_lights", structure = "jugcraft-test:drone_depot", maxTicks = 240)
	public void dropShipLightsTheGroundAndItsGunsShoot(GameTestHelper helper) {
		if (TestFocus.skip("tower")) {
			helper.succeed();
			return;
		}
		clearStrays(helper);
		ServerLevel level = helper.getLevel();
		BlockPos land = helper.absolutePos(new BlockPos(4, 1, 4));
		var ship = JugcraftTower.CREW_SHIP.create(level, net.minecraft.world.entity.EntitySpawnReason.TRIGGERED);
		helper.assertTrue(ship != null, "a ship");
		var site = new io.github.jimbozoomer.jugcraft.tower.crew.CrewShipSite.Site(net.minecraft.world.phys.Vec3.atBottomCenterOf(land), 0F, false,
				io.github.jimbozoomer.jugcraft.tower.crew.CrewShipFlight.HINGE_Y, net.minecraft.world.phys.Vec3.atBottomCenterOf(land.north(40)));
		ship.setUp(land.offset(0, 0, 60), site, java.util.List.of(), java.util.List.of(), level.getGameTime());
		level.addFreshEntity(ship);
		ship.testPhase(io.github.jimbozoomer.jugcraft.tower.crew.CrewShipFlight.UNLOAD);
		ship.testHold(true);                              // (down a while: the door guns fire only right out and spun up)
		keepLoaded(helper, land, 26, true);
		// a zombie off the port side (the ship faces north: port is west), held still in the air
		var zombie = net.minecraft.world.entity.EntityTypes.ZOMBIE.create(level, net.minecraft.world.entity.EntitySpawnReason.TRIGGERED);
		keepInTest(zombie);
		helper.assertTrue(zombie != null, "a zombie");
		zombie.snapTo(land.getX() - 16.5, land.getY() + 4, land.getZ() + 0.5, 0, 0);
		zombie.setNoAi(true);
		zombie.setNoGravity(true);
		level.addFreshEntity(zombie);
		float full = zombie.getHealth();
		java.util.List<BlockPos> placed = new java.util.ArrayList<>();
		helper.startSequence()
				.thenWaitUntil(() -> helper.assertTrue(!ship.lights().isEmpty(), "the floodlights' light is out"))
				.thenExecute(() -> {
					for (BlockPos p : ship.lights()) {
						helper.assertTrue(level.getBlockState(p).is(net.minecraft.world.level.block.Blocks.LIGHT), "a light block at " + p);
						placed.add(p.immutable());
					}
				})
				.thenWaitUntil(() -> helper.assertTrue(!zombie.isAlive() || zombie.getHealth() < full, "the port door gun hits the zombie"))
				.thenExecute(() -> {
					ship.testHold(false);
					ship.testPhase(io.github.jimbozoomer.jugcraft.tower.crew.CrewShipFlight.LIFT);
				})
				.thenWaitUntil(() -> helper.assertTrue(ship.lights().isEmpty(), "the lights come in as it lifts"))
				.thenExecute(() -> {
					// (only the ship's own: the next test's ship may be lighting the ground near)
					java.util.List<BlockPos> left = placed.stream().filter(p -> level.getBlockState(p).is(net.minecraft.world.level.block.Blocks.LIGHT)).toList();
					helper.assertTrue(left.isEmpty(), "no light blocks left behind: " + left);
					ship.discard();
					zombie.discard();
					keepLoaded(helper, land, 26, false);
				})
				.thenSucceed();
	}

	/**
	 * The drop ship list's last part (Justin, 9 November 2026): the side doors open first and then the door guns' booms
	 * run out (in, the other way round), a door gun stows down its bay; the door guns' barrels spin up before their first
	 * shot (the client turns them and plays the motor from the spin the server sends); and a player can sit in one of the
	 * bay's seats while it's down, stood up as it closes.
	 */
	@GameTest(environment = "jugcraft-test:drop_ship_guns", structure = "jugcraft-test:drone_depot", maxTicks = 400)
	public void dropShipDoorGunsSpinUpStowAndPlayersSit(GameTestHelper helper) {
		if (TestFocus.skip("tower")) {
			helper.succeed();
			return;
		}
		clearStrays(helper);
		// the side doors and the booms, in turn; the stowed aim
		helper.assertTrue(CrewShip.door(0.5F) == 1 && CrewShip.boom(0.5F) == 0 && CrewShip.boom(1) == 1 && CrewShip.door(0) == 0,
				"the door opens over the first half, the boom runs out over the second");
		helper.assertTrue(Math.abs(CrewShip.gunX(0, 0) - (-8.0F)) < 1e-4 && Math.abs(CrewShip.gunX(1, 1) - 10.65F) < 1e-4, "the booms in and out");
		helper.assertTrue(Math.abs(CrewShip.drawnYaw(0, Mth.HALF_PI, 0) + CrewShip.DOOR_GUN_STOW) < 1e-4
				&& Math.abs(CrewShip.drawnYaw(1, -Mth.HALF_PI, 0) - CrewShip.DOOR_GUN_STOW) < 1e-4
				&& Math.abs(CrewShip.drawnYaw(0, Mth.HALF_PI, 1) - Mth.HALF_PI) < 1e-4 && CrewShip.drawnPitch(0, 0.3F, 0) == 0,
				"a door gun stows down its bay, level, and aims as it likes once out");
		ServerLevel level = helper.getLevel();
		BlockPos land = helper.absolutePos(new BlockPos(4, 1, 4));
		for (int x = -26; x <= 26; x += 16) {             // (the guns' targets must stay loaded wherever the test stands)
			for (int z = -26; z <= 26; z += 16) {
				level.setChunkForced((land.getX() + x) >> 4, (land.getZ() + z) >> 4, true);
			}
		}
		CrewShip ship = JugcraftTower.CREW_SHIP.create(level, net.minecraft.world.entity.EntitySpawnReason.TRIGGERED);
		helper.assertTrue(ship != null, "a ship");
		var site = new io.github.jimbozoomer.jugcraft.tower.crew.CrewShipSite.Site(net.minecraft.world.phys.Vec3.atBottomCenterOf(land), 0F, false,
				io.github.jimbozoomer.jugcraft.tower.crew.CrewShipFlight.HINGE_Y, net.minecraft.world.phys.Vec3.atBottomCenterOf(land.north(40)));
		ship.setUp(land.offset(0, 0, 60), site, java.util.List.of(), java.util.List.of(), level.getGameTime());
		level.addFreshEntity(ship);
		ship.testPhase(io.github.jimbozoomer.jugcraft.tower.crew.CrewShipFlight.UNLOAD);
		ship.testHold(true);
		var zombie = net.minecraft.world.entity.EntityTypes.ZOMBIE.create(level, net.minecraft.world.entity.EntitySpawnReason.TRIGGERED);
		helper.assertTrue(zombie != null, "a zombie");
		keepInTest(zombie);
		// off the port quarter, where only the port door gun bears: abaft the port wing turret's arc (it took the zombie
		// too, with no barrels to spin up, and its bolt was there first as often as not: found with r196), inside the top
		// turret's dead ground, ahead of the bay turret's
		zombie.snapTo(land.getX() - 13.5, land.getY() + 4, land.getZ() + 11.0, 0, 0);
		zombie.setNoAi(true);
		zombie.setNoGravity(true);
		level.addFreshEntity(zombie);
		float full = zombie.getHealth();
		long[] spun = {-1}, hit = {-1};
		// (what the first hit was, for when it wasn't the door gun's: said in the failure. It was how the zombie's despawning
		// was found: keepInTest)
		String[] first = {"nothing"};
		// two players by the first seat (port, aft): one sits, the other can't sit in it too
		float[] s0 = CrewShip.place(0);
		net.minecraft.world.entity.player.Player a = helper.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
		net.minecraft.world.entity.player.Player b = helper.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
		helper.onEachTick(() -> {
			if (spun[0] < 0 && ship.spinning(0)) {
				spun[0] = level.getGameTime();
			}
			if (hit[0] < 0 && (!zombie.isAlive() || zombie.getHealth() < full)) {
				hit[0] = level.getGameTime();
				var source = zombie.getLastDamageSource();
				var by = source == null ? null : source.getEntity();
				first[0] = (full - zombie.getHealth()) + " damage (a door gun's bolt does " + CrewShip.GUN_DAMAGE[0] + ", a turret's "
						+ CrewShip.GUN_DAMAGE[2] + " or " + CrewShip.GUN_DAMAGE[4] + "), " + (source == null ? "from no source" : source.type().msgId() + " by "
						+ (by == null ? "nobody" : by == ship ? "this ship" : by.getType().toShortString() + " " + by.blockPosition().subtract(land)));
			}
		});
		helper.startSequence()
				.thenWaitUntil(() -> helper.assertTrue(hit[0] >= 0, "the port door gun hits the zombie"))
				.thenExecute(() -> helper.assertTrue(spun[0] >= 0 && hit[0] - spun[0] >= CrewShip.SPIN_UP,
						"its barrels spun up first (spinning at " + spun[0] + ", the first hit at " + hit[0] + ": " + first[0] + ")"))
				.thenExecute(() -> {
					net.minecraft.world.phys.Vec3 seat = ship.seatAt(0);
					for (var p : java.util.List.of(a, b)) {
						p.snapTo(seat.x + s0[3] * 1.0, seat.y - 1.0, seat.z + s0[4] * 1.0, 0, 0);
					}
					helper.assertTrue(ship.sit(a, 0), "a player sits in a free seat");
					helper.assertTrue(a.getVehicle() instanceof io.github.jimbozoomer.jugcraft.tower.SeatEntity, "on a seat");
					helper.assertTrue(!ship.sit(b, 0), "nobody sits in a taken seat");
					helper.assertTrue(!ship.sit(b, 3) || b.getVehicle() != null, "another seat's free");
				})
				.thenIdle(5)
				.thenExecute(() -> helper.assertTrue(a.isPassenger(), "still sitting while it's down"))
				.thenExecute(() -> {
					ship.testHold(false);
					ship.testPhase(io.github.jimbozoomer.jugcraft.tower.crew.CrewShipFlight.CLOSE);
				})
				.thenWaitUntil(() -> helper.assertTrue(!a.isPassenger() && !b.isPassenger(), "stood up as it closes"))
				.thenExecute(() -> helper.assertTrue(!ship.sit(a, 0), "no sitting once it's closing"))
				.thenExecute(() -> {
					ship.discard();
					zombie.discard();
					for (int x = -26; x <= 26; x += 16) {
						for (int z = -26; z <= 26; z += 16) {
							level.setChunkForced((land.getX() + x) >> 4, (land.getZ() + z) >> 4, false);
						}
					}
				})
				.thenSucceed();
	}

	/**
	 * A crew member sits at their post's chair (an Operator Chair here) and types; they can be killed, and their post then
	 * stands empty (tower/crew/TowerCrewState) until the tower hires again.
	 */
	@GameTest(structure = "jugcraft-test:drone_depot", maxTicks = 90)
	public void crewMemberSitsAtTheirChairAndCanBeKilled(GameTestHelper helper) {
		if (TestFocus.skip("tower")) {
			helper.succeed();
			return;
		}
		ServerLevel level = helper.getLevel();
		BlockPos core = helper.absolutePos(new BlockPos(1, 1, 1));
		BlockPos chair = helper.absolutePos(new BlockPos(7, 1, 7));
		// a floor under the chair, the rest's sofa and the way between (the structure's own is a level lower here)
		for (int x = 1; x <= 9; x++) {
			for (int z = 3; z <= 11; z++) {
				level.setBlock(helper.absolutePos(new BlockPos(x, 0, z)), Blocks.STONE.defaultBlockState(), net.minecraft.world.level.block.Block.UPDATE_CLIENTS);
			}
		}
		level.setBlock(chair, block("operator_chair").defaultBlockState()
				.setValue(net.minecraft.world.level.block.HorizontalDirectionalBlock.FACING, Direction.NORTH), net.minecraft.world.level.block.Block.UPDATE_ALL);
		var post = new io.github.jimbozoomer.jugcraft.tower.crew.CrewPost("test_chair", "controller", "seat", "type", null,
				chair.subtract(core), chair.south().subtract(core), java.util.List.of(), 0, null, 0F);
		var crew = JugcraftTower.TOWER_CREW.create(level, net.minecraft.world.entity.EntitySpawnReason.TRIGGERED);
		helper.assertTrue(crew != null, "a crew member");
		crew.snapTo(chair.getX() + 0.5, chair.getY(), chair.getZ() + 1.5, 0, 0);
		crew.setUp(core, post, io.github.jimbozoomer.jugcraft.tower.crew.CrewRoster.get().hire("controller", post.id(), 0, core.asLong(), java.util.List.of()));
		crew.setTestNoCore();
		level.addFreshEntity(crew);
		var state = io.github.jimbozoomer.jugcraft.tower.crew.TowerCrewState.get(level);
		state.hired(core, post.id(), crew.getUUID(), crew.skin());
		helper.startSequence()
				.thenWaitUntil(() -> helper.assertTrue(crew.isPassenger() && crew.act() == io.github.jimbozoomer.jugcraft.tower.crew.TowerCrew.ACT_TYPE,
						"sits at the chair, typing"))
				.thenExecute(() -> {
					crew.hurtServer(level, level.damageSources().generic(), 1000F);
					helper.assertTrue(!crew.isAlive(), "a crew member can be killed");
					var slot = state.slot(core, post.id());
					helper.assertTrue(slot != null && slot.crew().isEmpty() && slot.vacant() >= 0, "the post stands empty from then: " + slot);
					state.forget(core);
				})
				.thenSucceed();
	}

	/**
	 * Crew sit in the room kits' seats, which aren't players' seats, at the points their posts give: a pilot leaned back
	 * in a Pilot Simulator, flying (the seat stays under him), and an engineer at his office chair, typing.
	 */
	@GameTest(structure = "jugcraft-test:drone_depot", maxTicks = 90)
	public void crewSitInTheSimulatorsAndOfficeChairs(GameTestHelper helper) {
		if (TestFocus.skip("tower")) {
			helper.succeed();
			return;
		}
		ServerLevel level = helper.getLevel();
		BlockPos core = helper.absolutePos(new BlockPos(1, 1, 1));
		BlockPos sim = helper.absolutePos(new BlockPos(3, 1, 3));
		BlockState simState = block("pilot_simulator").defaultBlockState();
		var part = (net.minecraft.world.level.block.state.properties.IntegerProperty) simState.getBlock().getStateDefinition().getProperty("part");
		int[][] cells = {{0, 0, 0}, {1, 0, 0}, {0, 0, 1}, {1, 0, 1}, {0, 1, 0}, {1, 1, 0}, {0, 1, 1}, {1, 1, 1}};
		for (int i = 0; i < cells.length; i++) {
			level.setBlock(sim.offset(cells[i][0], cells[i][1], cells[i][2]), simState.setValue(part, i), net.minecraft.world.level.block.Block.UPDATE_CLIENTS);
		}
		BlockPos chair = helper.absolutePos(new BlockPos(9, 1, 9));
		level.setBlock(chair, block("task_chair").defaultBlockState(), net.minecraft.world.level.block.Block.UPDATE_ALL);
		var state = io.github.jimbozoomer.jugcraft.tower.crew.TowerCrewState.get(level);
		var pilotPost = new io.github.jimbozoomer.jugcraft.tower.crew.CrewPost("test_sim", "pilot", "seat", "pilot", null,
				sim.offset(1, 0, 1).subtract(core), sim.offset(1, 0, 2).subtract(core), java.util.List.of(), 0,
				new net.minecraft.world.phys.Vec3(sim.getX() + 1.0, sim.getY() + 9.5 / 16, sim.getZ() + 1.375).subtract(core.getX(), core.getY(), core.getZ()), 180F);
		var deskPost = new io.github.jimbozoomer.jugcraft.tower.crew.CrewPost("test_desk", "engineer", "seat", "type", null,
				chair.subtract(core), chair.south().subtract(core), java.util.List.of(), 0,
				new net.minecraft.world.phys.Vec3(chair.getX() + 0.5, chair.getY() + 8.2 / 16, chair.getZ() + 0.56).subtract(core.getX(), core.getY(), core.getZ()), 180F);
		java.util.List<io.github.jimbozoomer.jugcraft.tower.crew.TowerCrew> crew = new java.util.ArrayList<>();
		for (var post : java.util.List.of(pilotPost, deskPost)) {
			var c = JugcraftTower.TOWER_CREW.create(level, net.minecraft.world.entity.EntitySpawnReason.TRIGGERED);
			helper.assertTrue(c != null, "a crew member");
			BlockPos stand = core.offset(post.stand());
			c.snapTo(stand.getX() + 0.5, stand.getY(), stand.getZ() + 0.5, 0, 0);
			c.setUp(core, post, io.github.jimbozoomer.jugcraft.tower.crew.CrewRoster.get().hire(post.job(), post.id(), 0, core.asLong(), java.util.List.of()));
			c.setTestNoCore();
			level.addFreshEntity(c);
			state.hired(core, post.id(), c.getUUID(), c.skin());
			crew.add(c);
		}
		helper.startSequence()
				.thenWaitUntil(() -> helper.assertTrue(crew.get(0).isPassenger() && crew.get(0).act() == io.github.jimbozoomer.jugcraft.tower.crew.TowerCrew.ACT_PILOT,
						"the pilot sits in the simulator, flying"))
				.thenWaitUntil(() -> helper.assertTrue(crew.get(1).isPassenger() && crew.get(1).act() == io.github.jimbozoomer.jugcraft.tower.crew.TowerCrew.ACT_TYPE,
						"the engineer sits at his chair, typing"))
				.thenIdle(10)
				.thenExecute(() -> {
					var pilot = crew.get(0);
					helper.assertTrue(pilot.isPassenger() && Math.abs(pilot.getVehicle().getY() - (sim.getY() + 9.5 / 16)) < 0.01
							&& Math.abs(pilot.getVehicle().getZ() - (sim.getZ() + 1.375)) < 0.01, "the simulator's seat stays under him, where its cushion is");
					crew.forEach(c -> c.discard());
					state.forget(core);
				})
				.thenSucceed();
	}

	/**
	 * The crew's routines (Justin's night shift and breaks sheet, 5 November 2026; tools/tripillar_crew.py routines):
	 * sixteen posts keep the night watch (the security detail outside and on the roof among them) and every other has a
	 * sleep spot on its own floor, a rest seat or the crew bunk the foundation brings, from dusk to dawn; the two on
	 * their break all day and the detail outside take none, everyone else their breaks on a three-day rota (Justin, 6
	 * November 2026: a "schedule over like 3 days"; two or three over the three days and never two in a day since 10
	 * November 2026, his "fewer breaks": 88 where there were 175) at a cafe, dining or lounge seat
	 * on their floor, at other times from one day to the next; partners never away together; no seat
	 * anyone else's at the same time; the manager's office seats his alone. And a post's rest at a time of day is the
	 * one the routine gives (CrewPost.restAt). The same day's pick "Move staff up from the cafe": the second server waits
	 * on the crew lounge's tables upstairs and the second of the two with their mugs sits at the crew kitchen's island
	 * there, each under the id they had (so a tower keeps its people, who walk up).
	 */
	@GameTest
	public void crewRoutinesAreSound(GameTestHelper helper) {
		if (TestFocus.skip("tower")) {
			helper.succeed();
			return;
		}
		TowerData data = TowerData.get();
		var posts = data.crew();
		java.util.Map<Long, BlockState> placed = new java.util.HashMap<>();
		for (int[] b : data.foundation.place) {
			placed.put(BlockPos.asLong(b[0], b[1], b[2]), data.state(b[3]));
		}
		Set<String> sleepOn = Set.of("sofa", "cafe_chair", "crew_sofa", "leather_sofa", "recliner", "crew_bunk");
		Set<String> breakOn = Set.of("sofa", "cafe_chair", "crew_sofa", "leather_sofa", "recliner", "dining_chair", "bar_stool");
		java.util.function.Function<BlockPos, String> kind = pos -> {
			BlockState state = placed.get(pos.asLong());
			return state == null ? "nothing" : net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(state.getBlock()).getPath();
		};
		int night = 0, breaks = 0;
		java.util.Map<String, io.github.jimbozoomer.jugcraft.tower.crew.CrewPost> byId = new HashMap<>();
		java.util.List<String[]> booked = new java.util.ArrayList<>();
		for (var post : posts) {
			byId.put(post.id(), post);
			int floor = post.home().getY();
			if (post.night()) {
				night++;
				helper.assertTrue(post.sleep() == null, post.id() + ": on the night watch, no bed");
			} else {
				var sleep = post.sleep();
				helper.assertTrue(sleep != null && sleepOn.contains(kind.apply(sleep.seat())) && sleep.seat().getY() == floor,
						post.id() + ": a sleep spot on its floor, got " + (sleep == null ? null : kind.apply(sleep.seat()) + " " + sleep.seat()));
				helper.assertTrue(sleep.from() >= 12500 && sleep.from() < 14500 && sleep.to() >= 23000 && sleep.to() < 24000,
						post.id() + ": off after dusk, up before dawn: " + sleep.from() + "-" + sleep.to());
				helper.assertTrue(sleep.act().equals("lie") == kind.apply(sleep.seat()).equals("crew_bunk"), post.id() + ": lying down only in the bunk");
				helper.assertTrue(post.restAt(15000) == sleep && post.restAt(24000 * 3 + 2000) != sleep, post.id() + ": asleep at night, up by day");
			}
			boolean noBreaks = post.id().startsWith("cafe_break_") || post.outside() || post.id().startsWith("roof_") || post.id().startsWith("perimeter_")
					|| post.id().startsWith("inputs_") || post.id().equals("upper_patrol");
			helper.assertTrue(noBreaks ? post.breaks().isEmpty() : post.breaks().size() == 2 || post.breaks().size() == 3,
					post.id() + ": two or three breaks over the rota's three days (none for the two on theirs, nor the detail outside), got "
							+ post.breaks().size());
			breaks += post.breaks().size();
			int[] perDay = new int[io.github.jimbozoomer.jugcraft.tower.crew.CrewPost.CYCLE];
			for (var b : post.breaks()) {
				helper.assertTrue(b.days() >= 0 && b.days() < perDay.length, post.id() + ": each break on a day of the rota");
				perDay[b.days()]++;
			}
			helper.assertTrue(noBreaks || Arrays.stream(perDay).max().getAsInt() == 1, post.id() + ": never two breaks in a day: " + Arrays.toString(perDay));
			for (int k = 0; k < post.breaks().size(); k++) {
				var b = post.breaks().get(k);
				helper.assertTrue(b.from() >= 500 && b.to() <= 12500 && b.from() < b.to() && breakOn.contains(kind.apply(b.seat())) && b.seat().getY() == floor
						&& b.act().equals("mug"), post.id() + ": a break in the day at a seat on its floor, got " + kind.apply(b.seat()) + " " + b.from() + "-" + b.to());
				helper.assertTrue(post.restAt(b.from() + 1 + 24000L * b.days()) == b && post.restAt(b.from() + 1 + 24000L * (b.days() + 3)) == b,
						post.id() + ": on the break in its time, every third day");
				boolean inOffice = b.seat().getX() >= 26 && b.seat().getX() <= 37 && b.seat().getZ() >= -36 && b.seat().getZ() <= -23 && b.seat().getY() == 12;
				helper.assertTrue(inOffice == post.id().equals("manager"), post.id() + ": the manager's office seats are his alone");
				booked.add(new String[] {post.id(), String.valueOf(b.seat().asLong()), String.valueOf(b.from()), String.valueOf(b.to()), String.valueOf(b.days())});
			}
		}
		helper.assertTrue(night == 22, "twenty-two on the night watch (the security detail's new six with them), got " + night);
		helper.assertTrue(breaks == 88, "88 breaks over the rota's three days (half the 175 there were), got " + breaks);
		// never more than five away at once (there were eight), on any day of the three
		for (int day = 0; day < io.github.jimbozoomer.jugcraft.tower.crew.CrewPost.CYCLE; day++) {
			for (int t = 0; t < 12500; t += 25) {
				int away = 0;
				for (var post : posts) {
					var rest = post.restAt(24000L * day + t);
					away += rest != null && rest.act().equals("mug") && rest != post.sleep() ? 1 : 0;
				}
				helper.assertTrue(away <= 5, away + " on a break at once on day " + day + " at " + t);
			}
		}
		// the two who moved up from the cafe: a server round the crew lounge's dining tables, a technician with his mug on a
		// bar stool at the crew kitchen's island; the cafe keeps its cook, a server and the mechanic with his mug
		var server = byId.get("cafe_east");
		var sitter = byId.get("cafe_break_b");
		helper.assertTrue(server.job().equals("server") && server.kind().equals("patrol") && server.spots().size() == 4
				&& server.spots().stream().allMatch(s -> s.pos().getY() == 12 && s.pos().getZ() >= 31 && s.pos().getZ() <= 44 && Math.abs(s.pos().getX()) <= 18),
				"the second server waits on the crew lounge upstairs: " + server.spots());
		helper.assertTrue(sitter.job().equals("tech") && sitter.seated() && sitter.seat().getY() == 12 && sitter.seatAt() != null
				&& kind.apply(sitter.seat()).equals("bar_stool") && "mug".equals(sitter.prop()) && sitter.sleep() != null && sitter.sleep().seat().getY() == 12,
				"the second of the two with their mugs sits at the crew kitchen's island upstairs: " + kind.apply(sitter.seat()) + " " + sitter.seat());
		for (String id : new String[] {"kitchen", "cafe_west", "cafe_break_a"}) {
			helper.assertTrue(byId.get(id).home().getY() == 1, id + " stays in the cafe downstairs");
		}
		for (int i = 0; i < booked.size(); i++) {
			for (int j = i + 1; j < booked.size(); j++) {
				String[] a = booked.get(i), c = booked.get(j);
				boolean sameDays = a[4].equals("-1") || c[4].equals("-1") || a[4].equals(c[4]);
				boolean overlap = Integer.parseInt(a[2]) < Integer.parseInt(c[3]) && Integer.parseInt(c[2]) < Integer.parseInt(a[3]);
				helper.assertTrue(!(a[1].equals(c[1]) && sameDays && overlap), a[0] + " and " + c[0] + " on one seat at once");
			}
		}
		for (String[] pair : new String[][] {{"console_west", "console_east"}, {"console_director", "console_west"}, {"alarm_west", "alarm_east"},
				{"security_desk", "gates"}, {"sim_a", "sim_b"}, {"cad_west", "cad_east"}}) {
			for (int t = 0; t < 24000 * 3; t += 50) {
				var a = byId.get(pair[0]).restAt(t);
				var c = byId.get(pair[1]).restAt(t);
				helper.assertTrue(!(a != null && c != null && a.act().equals("mug") && c.act().equals("mug")), pair[0] + " and " + pair[1] + " both away at " + t);
			}
		}
		helper.succeed();
	}

	/** A crew post whose crew member works at an Operator Chair (core (1, 1, 1), the chair (7, 1, 7)) with a routine. */
	private static io.github.jimbozoomer.jugcraft.tower.crew.TowerCrew routineCrew(GameTestHelper helper, String id, boolean night,
			io.github.jimbozoomer.jugcraft.tower.crew.CrewPost.@org.jspecify.annotations.Nullable Rest sleep,
			java.util.List<io.github.jimbozoomer.jugcraft.tower.crew.CrewPost.Rest> breaks, long clock) {
		ServerLevel level = helper.getLevel();
		BlockPos core = helper.absolutePos(new BlockPos(1, 1, 1));
		BlockPos chair = helper.absolutePos(new BlockPos(7, 1, 7));
		level.setBlock(chair, block("operator_chair").defaultBlockState()
				.setValue(net.minecraft.world.level.block.HorizontalDirectionalBlock.FACING, Direction.NORTH), net.minecraft.world.level.block.Block.UPDATE_ALL);
		var post = new io.github.jimbozoomer.jugcraft.tower.crew.CrewPost(id, "controller", "seat", "type", null, chair.subtract(core),
				chair.south().subtract(core), java.util.List.of(), 0, null, 0F, night, sleep, breaks);
		var crew = JugcraftTower.TOWER_CREW.create(level, net.minecraft.world.entity.EntitySpawnReason.TRIGGERED);
		helper.assertTrue(crew != null, "a crew member");
		crew.snapTo(chair.getX() + 0.5, chair.getY(), chair.getZ() + 1.5, 0, 0);
		crew.setUp(core, post, io.github.jimbozoomer.jugcraft.tower.crew.CrewRoster.get().hire("controller", id, 0, core.asLong(), java.util.List.of()));
		crew.setTestNoCore();
		crew.setTestClock(clock);
		level.addFreshEntity(crew);
		return crew;
	}

	/** A rest on a sofa placed at {@code at} (structure coordinates), facing north, relative to the core at (1, 1, 1). */
	private static io.github.jimbozoomer.jugcraft.tower.crew.CrewPost.Rest sofaRest(GameTestHelper helper, BlockPos at, String act, int from, int to) {
		BlockPos core = helper.absolutePos(new BlockPos(1, 1, 1));
		BlockPos sofa = helper.absolutePos(at);
		helper.getLevel().setBlock(sofa, block("sofa").defaultBlockState()
				.setValue(net.minecraft.world.level.block.HorizontalDirectionalBlock.FACING, Direction.NORTH), net.minecraft.world.level.block.Block.UPDATE_ALL);
		return new io.github.jimbozoomer.jugcraft.tower.crew.CrewPost.Rest(sofa.subtract(core),
				new Vec3(sofa.getX() + 0.5 - core.getX(), sofa.getY() + 0.42 - core.getY(), sofa.getZ() + 0.5 - core.getZ()), 180F, act,
				sofa.north().subtract(core), from, to, -1);
	}

	/**
	 * Off shift at night a crew member leaves his chair for his sleep spot (a sofa here) and dozes there, still, facing
	 * the way it does; hurt, he wakes, stands up and stays up a while; in the morning he's back at his chair.
	 */
	@GameTest(structure = "jugcraft-test:drone_depot", maxTicks = 400)
	public void crewSleepAtNightAndWakeWhenHurt(GameTestHelper helper) {
		if (TestFocus.skip("tower")) {
			helper.succeed();
			return;
		}
		ServerLevel level = helper.getLevel();
		var sleep = sofaRest(helper, new BlockPos(3, 1, 7), "doze", 12500, 23000);
		var crew = routineCrew(helper, "test_sleeper", false, sleep, java.util.List.of(), 15000);
		BlockPos sofa = helper.absolutePos(new BlockPos(3, 1, 7));
		int T = io.github.jimbozoomer.jugcraft.tower.crew.TowerCrew.ACT_TYPE, Z = io.github.jimbozoomer.jugcraft.tower.crew.TowerCrew.ACT_DOZE;
		helper.startSequence()
				.thenWaitUntil(() -> helper.assertTrue(crew.isPassenger() && crew.act() == Z && crew.getVehicle().blockPosition().equals(sofa),
						"asleep on the sofa"))
				.thenIdle(5)
				.thenExecute(() -> {
					helper.assertTrue(Math.abs(net.minecraft.util.Mth.wrapDegrees(crew.getYRot() - 180F)) < 1F, "facing the way the sofa does");
					helper.assertTrue(crew.heldProp() == io.github.jimbozoomer.jugcraft.tower.crew.TowerCrew.PROP_NONE, "nothing in his hands asleep");
					crew.hurtServer(level, level.damageSources().generic(), 1F);
					helper.assertTrue(!crew.isPassenger() && !io.github.jimbozoomer.jugcraft.tower.crew.TowerCrew.asleep(crew.act()) && crew.rest() == null,
							"hurt, he wakes and stands up");
					crew.setTestClock(3000);
				})
				.thenWaitUntil(() -> helper.assertTrue(crew.isPassenger() && crew.act() == T && crew.getVehicle().blockPosition().equals(helper.absolutePos(new BlockPos(7, 1, 7))),
						"in the morning he's back at his chair, typing"))
				.thenExecute(() -> {
					crew.discard();
					io.github.jimbozoomer.jugcraft.tower.crew.TowerCrewState.get(level).forget(helper.absolutePos(new BlockPos(1, 1, 1)));
				})
				.thenSucceed();
	}

	/**
	 * On his break a crew member leaves his chair for the break's seat with a coffee and sits drinking it; when the break
	 * is over he goes back to his chair, the coffee left behind.
	 */
	@GameTest(structure = "jugcraft-test:drone_depot", maxTicks = 300)
	public void crewTakeTheirBreaksWithACoffee(GameTestHelper helper) {
		if (TestFocus.skip("tower")) {
			helper.succeed();
			return;
		}
		ServerLevel level = helper.getLevel();
		var brk = sofaRest(helper, new BlockPos(3, 1, 7), "mug", 1000, 2000);
		var crew = routineCrew(helper, "test_break", true, null, java.util.List.of(brk), 1500);
		BlockPos sofa = helper.absolutePos(new BlockPos(3, 1, 7));
		helper.startSequence()
				.thenWaitUntil(() -> helper.assertTrue(crew.isPassenger() && crew.act() == io.github.jimbozoomer.jugcraft.tower.crew.TowerCrew.ACT_MUG
						&& crew.heldProp() == io.github.jimbozoomer.jugcraft.tower.crew.TowerCrew.PROP_MUG && crew.getVehicle().blockPosition().equals(sofa),
						"on his break on the sofa with a coffee"))
				.thenExecute(() -> crew.setTestClock(2500))
				.thenWaitUntil(() -> helper.assertTrue(crew.isPassenger() && crew.act() == io.github.jimbozoomer.jugcraft.tower.crew.TowerCrew.ACT_TYPE
						&& crew.heldProp() == io.github.jimbozoomer.jugcraft.tower.crew.TowerCrew.PROP_NONE
						&& crew.getVehicle().blockPosition().equals(helper.absolutePos(new BlockPos(7, 1, 7))), "back at his chair after the break"))
				.thenExecute(() -> {
					crew.discard();
					io.github.jimbozoomer.jugcraft.tower.crew.TowerCrewState.get(level).forget(helper.absolutePos(new BlockPos(1, 1, 1)));
				})
				.thenSucceed();
	}

	/**
	 * A crew member whose post is a station (not a chair) gets up off the sofa he slept on when the morning comes, even
	 * when the time is set straight to morning, and walks back to work (Justin, 7 November 2026: after "time set day" they
	 * were "just sitting around on the couch in the middle of the day instead of doing jobs").
	 */
	@GameTest(structure = "jugcraft-test:drone_depot", maxTicks = 400)
	public void stationCrewGetUpOffTheirSleepSeatInTheMorning(GameTestHelper helper) {
		if (TestFocus.skip("tower")) {
			helper.succeed();
			return;
		}
		ServerLevel level = helper.getLevel();
		BlockPos core = helper.absolutePos(new BlockPos(1, 1, 1));
		var sleep = sofaRest(helper, new BlockPos(3, 1, 7), "doze", 12500, 23000);
		BlockPos spot = helper.absolutePos(new BlockPos(7, 0, 8));      // on the floor (the seats here stand a block over it)
		var post = new io.github.jimbozoomer.jugcraft.tower.crew.CrewPost("test_station_sleeper", "controller", "station", "type", null, null,
				spot.subtract(core), java.util.List.of(new io.github.jimbozoomer.jugcraft.tower.crew.CrewPost.Spot(spot.subtract(core), Float.NaN)), 0, null, 0F,
				false, sleep, java.util.List.of());
		var crew = JugcraftTower.TOWER_CREW.create(level, net.minecraft.world.entity.EntitySpawnReason.TRIGGERED);
		helper.assertTrue(crew != null, "a crew member");
		crew.snapTo(spot.getX() + 0.5, spot.getY(), spot.getZ() + 0.5, 0, 0);
		crew.setUp(core, post, io.github.jimbozoomer.jugcraft.tower.crew.CrewRoster.get().hire("controller", "test_station_sleeper", 0, core.asLong(),
				java.util.List.of()));
		crew.setTestNoCore();
		crew.setTestClock(15000);
		level.addFreshEntity(crew);
		BlockPos sofa = helper.absolutePos(new BlockPos(3, 1, 7));
		helper.startSequence()
				.thenWaitUntil(() -> helper.assertTrue(crew.isPassenger() && crew.getVehicle().blockPosition().equals(sofa), "asleep on the sofa at night"))
				.thenExecute(() -> crew.setTestClock(1000))      // the time set straight to morning
				.thenWaitUntil(() -> helper.assertTrue(!crew.isPassenger(), "up off the sofa in the morning"))
				.thenWaitUntil(() -> helper.assertTrue(crew.distanceToSqr(spot.getX() + 0.5, spot.getY(), spot.getZ() + 0.5) < 0.5,
						"and back at his station: " + crew.position()))
				.thenExecute(() -> {
					crew.discard();
					io.github.jimbozoomer.jugcraft.tower.crew.TowerCrewState.get(level).forget(core);
				})
				.thenSucceed();
	}

	/** A sleeper lying down (the crew bunk's way) lies still on the seat's point, his feet the way the rest faces. */
	@GameTest(structure = "jugcraft-test:drone_depot", maxTicks = 200)
	public void crewLieDownToSleep(GameTestHelper helper) {
		if (TestFocus.skip("tower")) {
			helper.succeed();
			return;
		}
		ServerLevel level = helper.getLevel();
		var sleep = sofaRest(helper, new BlockPos(3, 1, 7), "lie", 12500, 23000);
		var crew = routineCrew(helper, "test_lie", false, sleep, java.util.List.of(), 20000);
		helper.startSequence()
				.thenWaitUntil(() -> helper.assertTrue(crew.isPassenger() && crew.act() == io.github.jimbozoomer.jugcraft.tower.crew.TowerCrew.ACT_LIE,
						"lying down to sleep"))
				.thenIdle(5)
				.thenExecute(() -> {
					BlockPos core = helper.absolutePos(new BlockPos(1, 1, 1));
					Vec3 at = sleep.at().add(core.getX(), core.getY(), core.getZ());
					helper.assertTrue(crew.getVehicle().position().distanceTo(at) < 0.01, "on the seat's point");
					helper.assertTrue(Math.abs(net.minecraft.util.Mth.wrapDegrees(crew.getYRot() - sleep.yaw())) < 1F, "his feet the way the rest faces");
					crew.discard();
					io.github.jimbozoomer.jugcraft.tower.crew.TowerCrewState.get(level).forget(core);
				})
				.thenSucceed();
	}

	/**
	 * The night lighting (Justin's night lighting sheet, 5 November 2026; tools/tower_night_lights.py): every lamp the core
	 * switches at night is one of the tower's ceiling lights where the foundation puts it, on a crew floor, to night, low
	 * or off; the lamps right over the night watch's chairs at the console stay on, the manager's office goes dark.
	 */
	@GameTest
	public void nightLightsAreSound(GameTestHelper helper) {
		if (TestFocus.skip("tower")) {
			helper.succeed();
			return;
		}
		TowerData data = TowerData.get();
		java.util.Map<Long, BlockState> placed = new java.util.HashMap<>();
		for (int[] b : data.foundation.place) {
			placed.put(BlockPos.asLong(b[0], b[1], b[2]), data.state(b[3]));
		}
		int[][] lamps = data.nightLights();
		helper.assertTrue(lamps.length > 300, "the crew floors' lamps, got " + lamps.length);
		Set<Long> switched = new HashSet<>();
		int officeOff = 0;
		for (int[] l : lamps) {
			BlockState state = placed.get(BlockPos.asLong(l[0], l[1], l[2]));
			helper.assertTrue(state != null && state.getBlock() instanceof io.github.jimbozoomer.jugcraft.tower.TowerLightBlock,
					"a tower lamp at " + Arrays.toString(l) + ", got " + state);
			helper.assertTrue(l[1] <= 15 && l[3] != io.github.jimbozoomer.jugcraft.tower.TowerLightBlock.Mode.ON.ordinal(), "on a crew floor, not kept on: " + Arrays.toString(l));
			switched.add(BlockPos.asLong(l[0], l[1], l[2]));
			if (l[0] >= 27 && l[0] <= 36 && l[2] >= -35 && l[2] <= -24 && l[1] == 15 && l[3] == io.github.jimbozoomer.jugcraft.tower.TowerLightBlock.Mode.OFF.ordinal()) {
				officeOff++;
			}
		}
		helper.assertTrue(officeOff > 0, "the manager's office goes dark at night");
		int keptOn = 0;
		for (var e : placed.entrySet()) {
			BlockPos p = BlockPos.of(e.getKey());
			if (e.getValue().getBlock() instanceof io.github.jimbozoomer.jugcraft.tower.TowerLightBlock && p.getY() == 9
					&& Math.abs(p.getX()) <= 8 && p.getZ() >= -18 && p.getZ() <= -6 && !switched.contains(e.getKey())) {   // over the console's two chairs (-4/4, -10)
				keptOn++;
			}
		}
		helper.assertTrue(keptOn > 0, "lamps over the console stay on for the night watch");
		helper.succeed();
	}

	/**
	 * A tower lamp's night modes: placed it is on (light 15); night 12, low 10, off 0; only the tower's lamps are switched.
	 * The night runs from 20:00 to 05:00 (TowerNightLights.night).
	 */
	@GameTest(structure = "jugcraft-test:drone_depot", maxTicks = 20)
	public void towerLampsDimAtNight(GameTestHelper helper) {
		if (TestFocus.skip("tower")) {
			helper.succeed();
			return;
		}
		ServerLevel level = helper.getLevel();
		BlockPos lamp = helper.absolutePos(new BlockPos(5, 3, 5));
		level.setBlock(lamp, block("ceiling_light_panel").defaultBlockState(), net.minecraft.world.level.block.Block.UPDATE_ALL);
		helper.assertTrue(level.getBlockState(lamp).getLightEmission() == 15, "placed, a lamp is on");
		int[] want = {15, 12, 10, 0};
		for (var mode : io.github.jimbozoomer.jugcraft.tower.TowerLightBlock.Mode.values()) {
			io.github.jimbozoomer.jugcraft.tower.TowerNightLights.set(level, lamp, mode);
			helper.assertTrue(level.getBlockState(lamp).getLightEmission() == want[mode.ordinal()], mode + ": light " + want[mode.ordinal()]
					+ ", got " + level.getBlockState(lamp).getLightEmission());
		}
		BlockPos other = helper.absolutePos(new BlockPos(6, 3, 5));
		level.setBlock(other, Blocks.GLOWSTONE.defaultBlockState(), net.minecraft.world.level.block.Block.UPDATE_ALL);
		helper.assertTrue(!io.github.jimbozoomer.jugcraft.tower.TowerNightLights.set(level, other, io.github.jimbozoomer.jugcraft.tower.TowerLightBlock.Mode.OFF)
				&& level.getBlockState(other).is(Blocks.GLOWSTONE), "only the tower's lamps are switched");
		helper.assertTrue(!io.github.jimbozoomer.jugcraft.tower.TowerNightLights.night(13999) && io.github.jimbozoomer.jugcraft.tower.TowerNightLights.night(14000)
				&& io.github.jimbozoomer.jugcraft.tower.TowerNightLights.night(22999) && !io.github.jimbozoomer.jugcraft.tower.TowerNightLights.night(23000)
				&& io.github.jimbozoomer.jugcraft.tower.TowerNightLights.night(24000L * 7 + 15000), "night from 20:00 to 05:00, any day");
		helper.succeed();
	}

	/**
	 * How the crew greet a player (the greetings sheet; tower/crew/CrewGreeting): a wave; security and the gate guards
	 * salute; a nod with their hands full or seated at work; seated with a coffee, the coffee raised; asleep, nothing. The
	 * word for the time of day or, one in ten, a rude one; every word is in the language file.
	 */
	@GameTest
	public void crewGreetingsFitWhatTheyDo(GameTestHelper helper) {
		if (TestFocus.skip("tower")) {
			helper.succeed();
			return;
		}
		int A_IDLE = io.github.jimbozoomer.jugcraft.tower.crew.TowerCrew.ACT_IDLE;
		helper.assertTrue(io.github.jimbozoomer.jugcraft.tower.crew.CrewGreeting.job("gate_guard_2").equals("gate_guard")
				&& io.github.jimbozoomer.jugcraft.tower.crew.CrewGreeting.job("controller_10").equals("controller"), "the job from the skin");
		int[][] cases = {
				// {expected, act, prop, seated, guard}
				{io.github.jimbozoomer.jugcraft.tower.crew.CrewGreeting.WAVE, A_IDLE, 0, 0, 0},
				{io.github.jimbozoomer.jugcraft.tower.crew.CrewGreeting.SALUTE, A_IDLE, 0, 0, 1},
				{io.github.jimbozoomer.jugcraft.tower.crew.CrewGreeting.NOD, io.github.jimbozoomer.jugcraft.tower.crew.TowerCrew.ACT_CARRY,
						io.github.jimbozoomer.jugcraft.tower.crew.TowerCrew.PROP_TOOLBOX, 0, 0},
				{io.github.jimbozoomer.jugcraft.tower.crew.CrewGreeting.NOD, io.github.jimbozoomer.jugcraft.tower.crew.TowerCrew.ACT_TYPE, 0, 1, 0},
				{io.github.jimbozoomer.jugcraft.tower.crew.CrewGreeting.NOD, io.github.jimbozoomer.jugcraft.tower.crew.TowerCrew.ACT_TYPE, 0, 1, 1},
				{io.github.jimbozoomer.jugcraft.tower.crew.CrewGreeting.CHEERS, io.github.jimbozoomer.jugcraft.tower.crew.TowerCrew.ACT_MUG,
						io.github.jimbozoomer.jugcraft.tower.crew.TowerCrew.PROP_MUG, 1, 0},
				{io.github.jimbozoomer.jugcraft.tower.crew.CrewGreeting.NONE, io.github.jimbozoomer.jugcraft.tower.crew.TowerCrew.ACT_DOZE, 0, 1, 0},
				{io.github.jimbozoomer.jugcraft.tower.crew.CrewGreeting.NONE, io.github.jimbozoomer.jugcraft.tower.crew.TowerCrew.ACT_LIE, 0, 1, 1}};
		for (int[] c : cases) {
			int got = io.github.jimbozoomer.jugcraft.tower.crew.CrewGreeting.kind(c[4] == 1 ? "security" : "controller", c[1], c[2], c[3] == 1);
			helper.assertTrue(got == c[0], "greeting for " + Arrays.toString(c) + ": got " + got);
		}
		com.google.gson.JsonObject lang;
		try (var in = TowerGameTests.class.getResourceAsStream("/assets/jugcraft/lang/en_us.json")) {
			lang = com.google.gson.JsonParser.parseReader(new java.io.InputStreamReader(in, java.nio.charset.StandardCharsets.UTF_8)).getAsJsonObject();
		} catch (Exception e) {
			throw new IllegalStateException(e);
		}
		for (long t = 0; t < 24000; t += 500) {
			for (int roll = 0; roll < io.github.jimbozoomer.jugcraft.tower.crew.CrewGreeting.RUDE; roll++) {
				for (int pick = 0; pick < 16; pick++) {
					String key = io.github.jimbozoomer.jugcraft.tower.crew.CrewGreeting.line(t, roll, pick);
					helper.assertTrue(lang.has(key), "the word is in the language file: " + key);
					helper.assertTrue(key.contains(".rude.") == (roll == 0), "rude only on the roll: " + key);
				}
			}
		}
		helper.assertTrue(io.github.jimbozoomer.jugcraft.tower.crew.CrewGreeting.line(1000, 3, 0).contains(".morning.")
				&& io.github.jimbozoomer.jugcraft.tower.crew.CrewGreeting.line(9000, 3, 0).contains(".afternoon.")
				&& io.github.jimbozoomer.jugcraft.tower.crew.CrewGreeting.line(13000, 3, 0).contains(".evening.")
				&& io.github.jimbozoomer.jugcraft.tower.crew.CrewGreeting.line(18000, 3, 0).contains(".night."), "the word for the time of day");
		helper.succeed();
	}

	/**
	 * Walking their round, a crew member who meets a player head on steps aside to a free cell beside him and walks on
	 * once the player is past.
	 */
	@GameTest(structure = "jugcraft-test:drone_depot", maxTicks = 600)
	public void crewGiveWayToAPlayer(GameTestHelper helper) {
		if (TestFocus.skip("tower")) {
			helper.succeed();
			return;
		}
		ServerLevel level = helper.getLevel();
		BlockPos core = helper.absolutePos(new BlockPos(1, 1, 1));
		// a clear strip three wide, x 3-5, z 1-11, on a solid floor
		for (int x = 2; x <= 6; x++) {
			for (int z = 0; z <= 12; z++) {
				level.setBlock(helper.absolutePos(new BlockPos(x, 0, z)), Blocks.STONE.defaultBlockState(), net.minecraft.world.level.block.Block.UPDATE_CLIENTS);
				for (int y = 1; y <= 3; y++) {
					boolean wall = x == 2 || x == 6;
					level.setBlock(helper.absolutePos(new BlockPos(x, y, z)), wall ? Blocks.STONE.defaultBlockState() : Blocks.AIR.defaultBlockState(),
							net.minecraft.world.level.block.Block.UPDATE_CLIENTS);
				}
			}
		}
		BlockPos from = helper.absolutePos(new BlockPos(4, 1, 11)), to = helper.absolutePos(new BlockPos(4, 1, 1));
		var post = new io.github.jimbozoomer.jugcraft.tower.crew.CrewPost("test_way", "security", "patrol", "idle", null, null, null,
				java.util.List.of(new io.github.jimbozoomer.jugcraft.tower.crew.CrewPost.Spot(from.subtract(core), Float.NaN),
						new io.github.jimbozoomer.jugcraft.tower.crew.CrewPost.Spot(to.subtract(core), Float.NaN)), 0, null, 0F);
		var crew = JugcraftTower.TOWER_CREW.create(level, net.minecraft.world.entity.EntitySpawnReason.TRIGGERED);
		helper.assertTrue(crew != null, "a crew member");
		crew.snapTo(from.getX() + 0.5, from.getY(), from.getZ() + 0.5, 180, 0);
		crew.setUp(core, post, io.github.jimbozoomer.jugcraft.tower.crew.CrewRoster.get().hire("security", post.id(), 0, core.asLong(), java.util.List.of()));
		crew.setTestNoCore();
		level.addFreshEntity(crew);
		var player = helper.makeMockServerPlayerInLevel();
		BlockPos stand = helper.absolutePos(new BlockPos(4, 1, 5));
		player.snapTo(stand.getX() + 0.5, stand.getY(), stand.getZ() + 0.5, 0, 0);
		double lane = from.getX() + 0.5;
		helper.startSequence()
				.thenWaitUntil(() -> helper.assertTrue(Math.abs(crew.getX() - lane) > 0.6 && crew.distanceToSqr(player) < 9.0,
						"stepped aside for the player, got x " + crew.getX() + " (lane " + lane + ")"))
				.thenWaitUntil(() -> helper.assertTrue(crew.getZ() < player.getZ() - 1.0, "walked on past the player"))
				.thenExecute(() -> {
					crew.discard();
					player.discard();
					io.github.jimbozoomer.jugcraft.tower.crew.TowerCrewState.get(level).forget(core);
				})
				.thenSucceed();
	}

	/** A strip on solid floor, roofed (no sun on the mobs), x 2-6 walled, z 0-12; and a security officer at z 2 facing south. */
	/**
	 * A player in survival for the tests where it matters (the crew and the drop ship leave creative players alone): the
	 * game's in-level mock always says it is creative, even after setGameMode(SURVIVAL). Off the server with {@link #leave}.
	 */
	static net.minecraft.server.level.ServerPlayer survivalPlayer(GameTestHelper helper) {
		var profile = new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(), "tower-test-player");
		var server = helper.getLevel().getServer();
		var p = new net.minecraft.server.level.ServerPlayer(server, helper.getLevel(), profile, net.minecraft.server.level.ClientInformation.createDefault());
		var connection = new net.minecraft.network.Connection(net.minecraft.network.protocol.PacketFlow.SERVERBOUND);
		new io.netty.channel.embedded.EmbeddedChannel(connection);
		server.getPlayerList().placeNewPlayer(connection, p, net.minecraft.server.network.CommonListenerCookie.createInitial(profile, false));
		p.setGameMode(GameType.SURVIVAL);
		helper.assertTrue(!p.isCreative() && !p.isSpectator(), "a player in survival");
		return p;
	}

	/**
	 * A block of the mod by its id, from the game's registry. The tower's own map of blocks doesn't hold the plant
	 * interior's blocks the tower is built with (nuclear/PlantInterior: the sofas, the glass coffee tables, the storage
	 * racks, the framed glass), so asking it for those gave null and the checks on them could never pass.
	 */
	public static net.minecraft.world.level.block.Block modBlock(String id) {
		return net.minecraft.core.registries.BuiltInRegistries.BLOCK.getValue(io.github.jimbozoomer.jugcraft.Jugcraft.id(id));
	}

	static void leave(net.minecraft.server.level.ServerPlayer p) {
		p.level().getServer().getPlayerList().remove(p);
	}

	private static io.github.jimbozoomer.jugcraft.tower.crew.TowerCrew rifleRange(GameTestHelper helper, String job, BlockPos at) {
		ServerLevel level = helper.getLevel();
		for (int x = 2; x <= 6; x++) {
			for (int z = 0; z <= 12; z++) {
				level.setBlock(helper.absolutePos(new BlockPos(x, 0, z)), Blocks.STONE.defaultBlockState(), net.minecraft.world.level.block.Block.UPDATE_CLIENTS);
				level.setBlock(helper.absolutePos(new BlockPos(x, 4, z)), Blocks.STONE.defaultBlockState(), net.minecraft.world.level.block.Block.UPDATE_CLIENTS);
				for (int y = 1; y <= 3; y++) {
					boolean wall = x == 2 || x == 6;
					level.setBlock(helper.absolutePos(new BlockPos(x, y, z)), wall ? Blocks.STONE.defaultBlockState() : Blocks.AIR.defaultBlockState(),
							net.minecraft.world.level.block.Block.UPDATE_CLIENTS);
				}
			}
		}
		BlockPos core = helper.absolutePos(new BlockPos(1, 1, 1));
		BlockPos p = helper.absolutePos(at);
		var post = new io.github.jimbozoomer.jugcraft.tower.crew.CrewPost("test_" + job, job, "station", "idle", null, null, null,
				java.util.List.of(new io.github.jimbozoomer.jugcraft.tower.crew.CrewPost.Spot(p.subtract(core), 0F)), 0, null, 0F);
		var crew = JugcraftTower.TOWER_CREW.create(level, net.minecraft.world.entity.EntitySpawnReason.TRIGGERED);
		helper.assertTrue(crew != null, "a crew member");
		crew.snapTo(p.getX() + 0.5, p.getY(), p.getZ() + 0.5, 0, 0);
		crew.setUp(core, post, io.github.jimbozoomer.jugcraft.tower.crew.CrewRoster.get().hire(job, post.id(), 0, core.asLong(), java.util.List.of()));
		crew.setTestNoCore();
		level.addFreshEntity(crew);
		return crew;
	}

	/**
	 * For a test of a drop ship's guns (each runs in a batch of its own, so nothing else is going on): clears away whatever
	 * hostile mobs earlier tests left wandering within the guns' reach. A gun takes the nearest thing it may shoot: a stray
	 * nearer than the test's own target kept it busy, and the test failed now and then (found with r196).
	 */
	/**
	 * Keeps the chunks within {@code reach} blocks of {@code land} loaded and ticking ({@code on}), or lets them go. The
	 * tests stand at a random place each run, so whether what a ship's guns shoot at off beyond the test's own plot (and
	 * the bolts on their way) lay in a chunk that ticks was chance: the bolts were fired and never arrived, and these
	 * tests failed now and then (found with r196).
	 */
	static void keepLoaded(GameTestHelper helper, BlockPos land, int reach, boolean on) {
		ServerLevel level = helper.getLevel();
		for (int cx = (land.getX() - reach) >> 4; cx <= (land.getX() + reach) >> 4; cx++) {
			for (int cz = (land.getZ() - reach) >> 4; cz <= (land.getZ() + reach) >> 4; cz++) {
				level.setChunkForced(cx, cz, on);
			}
		}
	}

	static void clearStrays(GameTestHelper helper) {
		BlockPos at = helper.absolutePos(new BlockPos(4, 1, 4));
		for (var mob : helper.getLevel().getEntitiesOfClass(net.minecraft.world.entity.Mob.class, new net.minecraft.world.phys.AABB(at).inflate(160),
				m -> m instanceof net.minecraft.world.entity.monster.Enemy)) {
			mob.discard();
		}
	}

	private static net.minecraft.world.entity.monster.zombie.Husk husk(GameTestHelper helper, BlockPos at) {
		var husk = net.minecraft.world.entity.EntityTypes.HUSK.create(helper.getLevel(), net.minecraft.world.entity.EntitySpawnReason.TRIGGERED);
		helper.assertTrue(husk != null, "a husk");
		BlockPos p = helper.absolutePos(at);
		husk.snapTo(p.getX() + 0.5, p.getY(), p.getZ() + 0.5, 180, 0);
		husk.setNoAi(true);
		husk.setPersistenceRequired();
		helper.getLevel().addFreshEntity(husk);
		return husk;
	}

	/**
	 * Sound through walls (sound/SoundWalls) counts a block that doesn't fill its cell only where the line goes through
	 * it (Justin, 10 November 2026: the TV's sound cut standing by the upstairs sofa): over a slab, or past a TV's
	 * panel to the middle of its screen, nothing; through the slab, a third of a wall; a stone wall, one.
	 */
	@GameTest(maxTicks = 20)
	public void soundGoesOverWhatIsntInTheWay(GameTestHelper helper) {
		if (TestFocus.skip("tower")) {
			helper.succeed();
			return;
		}
		ServerLevel level = helper.getLevel();
		BlockPos floor = helper.absolutePos(new BlockPos(0, 1, 6));
		// a slab on the floor between the ear and the sound
		level.setBlock(floor.east(2), Blocks.OAK_SLAB.defaultBlockState(), net.minecraft.world.level.block.Block.UPDATE_ALL);
		net.minecraft.world.phys.Vec3 ear = new net.minecraft.world.phys.Vec3(floor.getX() + 0.5, floor.getY() + 0.75, floor.getZ() + 0.5);
		net.minecraft.world.phys.Vec3 sound = ear.add(4, 0, 0);
		double over = io.github.jimbozoomer.jugcraft.sound.SoundWalls.walls(level, ear, sound);
		double through = io.github.jimbozoomer.jugcraft.sound.SoundWalls.walls(level, ear.add(0, -0.5, 0), sound.add(0, -0.5, 0));
		helper.assertTrue(over < 0.02, "a sound over a slab is not muffled by it: " + over);
		helper.assertTrue(through > 0.25 && through < 0.5, "a sound through a slab is muffled a third: " + through);
		level.setBlock(floor.east(2), Blocks.STONE.defaultBlockState(), net.minecraft.world.level.block.Block.UPDATE_ALL);
		double wall = io.github.jimbozoomer.jugcraft.sound.SoundWalls.walls(level, ear, sound);
		helper.assertTrue(wall > 0.9, "a stone wall counts one: " + wall);
		// a Flat Screen TV three wide and two high on a wall (its back east), heard from a standing ear in front of it
		BlockPos tv = helper.absolutePos(new BlockPos(5, 1, 1));
		for (int dz = 0; dz < 3; dz++) {
			for (int dy = 0; dy < 2; dy++) {
				level.setBlock(tv.offset(1, dy, dz), Blocks.STONE.defaultBlockState(), net.minecraft.world.level.block.Block.UPDATE_ALL);
			}
		}
		net.minecraft.world.level.block.Block flat = net.minecraft.core.registries.BuiltInRegistries.BLOCK.getValue(io.github.jimbozoomer.jugcraft.Jugcraft.id("flat_tv"));
		for (int dz = 0; dz < 3; dz++) {
			for (int dy = 0; dy < 2; dy++) {
				level.setBlock(tv.offset(0, dy, dz), flat.defaultBlockState().setValue(io.github.jimbozoomer.jugcraft.tv.FlatTvBlock.FACING, Direction.EAST),
						net.minecraft.world.level.block.Block.UPDATE_ALL);
			}
		}
		assertFlatTvAt(helper, level, tv, Direction.EAST, 3, 2, false);
		// (client/tv/TvPlayers.middle: the glass's middle, 0.3 in front of the blocks' middle)
		net.minecraft.world.phys.Vec3 middle = new net.minecraft.world.phys.Vec3(tv.getX() + 0.5 - 0.3, tv.getY() + 1.0, tv.getZ() + 0.5 + 1.0);
		for (double dz : new double[] {-1.5, 0, 1.5}) {
			net.minecraft.world.phys.Vec3 standing = new net.minecraft.world.phys.Vec3(tv.getX() - 4.5, tv.getY() + 0.62, middle.z + dz);
			double heard = io.github.jimbozoomer.jugcraft.sound.SoundWalls.walls(level, standing, middle);
			helper.assertTrue(heard < 0.02, "the TV is heard plainly from in front of it (" + dz + "): " + heard);
		}
		helper.succeed();
	}

	/**
	 * The Lounge TV is retired (Justin, 10 November 2026: "I don't want to use that shitty TV at all, anywhere"): no
	 * recipe, not in the creative menu, and any in the world turns into a working Flat Screen TV (tower/RetiredBlocks).
	 * One put down becomes one at the end of the tick, two high with the row above clear and hung on the wall behind it;
	 * one already in a chunk (placed without its placing running, as an old world's are) is found when the chunk loads,
	 * here one high under a block and standing, nothing behind it.
	 */
	@GameTest(maxTicks = 40)
	public void aLoungeTvBecomesAFlatScreenTv(GameTestHelper helper) {
		if (TestFocus.skip("tower")) {
			helper.succeed();
			return;
		}
		ServerLevel level = helper.getLevel();
		var kit = io.github.jimbozoomer.jugcraft.tower.RoomKit.BLOCKS.get("lounge_tv");
		helper.assertTrue(kit != null && io.github.jimbozoomer.jugcraft.tower.RetiredBlocks.retired(kit), "the Lounge TV is retired");
		helper.assertTrue(level.getServer().getRecipeManager().getRecipes().stream()
				.noneMatch(r -> r.id().identifier().getPath().equals("lounge_tv")), "no recipe makes a Lounge TV");
		BlockState west = kit.defaultBlockState().setValue(io.github.jimbozoomer.jugcraft.tower.RoomKitBlock.FACING_PROP, Direction.WEST);
		// put down (its anchor; the rest comes with it), a wall behind it
		BlockPos placed = helper.absolutePos(new BlockPos(2, 1, 4));
		for (int dz = 0; dz < 3; dz++) {
			for (int dy = 0; dy < 2; dy++) {
				level.setBlock(placed.offset(1, dy, -dz), Blocks.STONE.defaultBlockState(), net.minecraft.world.level.block.Block.UPDATE_ALL);
			}
		}
		level.setBlock(placed, west, net.minecraft.world.level.block.Block.UPDATE_ALL);
		helper.assertTrue(level.getBlockState(placed.north(2)).is(kit), "the Lounge TV went down whole");
		// in the world already: every cell set without its placing
		BlockPos old = helper.absolutePos(new BlockPos(6, 1, 4));
		for (BlockPos cell : kit.cells(old, west)) {
			int i = kit.cells(old, west).indexOf(cell);
			level.setBlock(cell, kit.cellState(west, kit.spec.cells().get(i)), net.minecraft.world.level.block.Block.UPDATE_CLIENTS | net.minecraft.world.level.block.Block.UPDATE_SKIP_ON_PLACE);
		}
		level.setBlock(old.north().above(), Blocks.STONE.defaultBlockState(), net.minecraft.world.level.block.Block.UPDATE_ALL);
		helper.runAfterDelay(2, () -> {
			assertFlatTvAt(helper, level, placed.north(2), Direction.EAST, 3, 2, false);
			helper.assertTrue(level.getBlockState(old.north(2)).is(kit), "a Lounge TV already in the world waits for its chunk to load");
			// (its blocks may straddle two chunks: each scanned as it would be on loading)
			int found = 0;
			java.util.Set<Long> chunks = new java.util.HashSet<>();
			for (BlockPos cell : kit.cells(old, west)) {
				if (chunks.add(((long) (cell.getX() >> 4) << 32) ^ ((cell.getZ() >> 4) & 0xFFFFFFFFL))) {
					found += io.github.jimbozoomer.jugcraft.tower.RetiredBlocks.scan(level, level.getChunk(cell));
				}
			}
			helper.assertTrue(found >= 3, "its chunk's Lounge TV blocks are found: " + found);
			io.github.jimbozoomer.jugcraft.tower.RetiredBlocks.convertQueued(level);
			assertFlatTvAt(helper, level, old.north(2), Direction.EAST, 3, 1, true);
			helper.assertTrue(level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class, new net.minecraft.world.phys.AABB(placed).inflate(8)).isEmpty(),
					"nothing dropped");
			helper.succeed();
		});
	}

	/**
	 * A plasma bolt through a creature's part that is an entity of its own (Justin, 10 November 2026: "I shot this murmur
	 * ... and like the head. I guess it's two different entity parts ... it's not showing the shot that went through their
	 * head"): the Murmur's head takes the hit for it, and the tunnel is burned into the head, in the head's own frame (its
	 * own renderer draws it cut: client/PlasmaWoundsMesh's parts). A real bolt fired at the head hits it and hurts the
	 * Murmur.
	 */
	@GameTest(maxTicks = 200)
	public void plasmaBoltWoundsACreaturesOwnPart(GameTestHelper helper) {
		if (TestFocus.skip("tower")) {
			helper.succeed();
			return;
		}
		ServerLevel level = helper.getLevel();
		var type = io.github.jimbozoomer.jugcraft.menagerie.Menagerie.TYPES.get("beast_murmur");
		helper.assertTrue(type != null, "the Murmur");
		var murmur = type.create(level, net.minecraft.world.entity.EntitySpawnReason.TRIGGERED);
		helper.assertTrue(murmur != null, "a Murmur");
		BlockPos at = helper.absolutePos(new BlockPos(4, 1, 4));
		murmur.snapTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, 30, 0);
		murmur.setNoAi(true);
		murmur.setPersistenceRequired();
		level.addFreshEntity(murmur);
		java.util.function.Supplier<io.github.jimbozoomer.jugcraft.menagerie.RecoveredCreaturePart> head = () -> level.getEntitiesOfClass(
				io.github.jimbozoomer.jugcraft.menagerie.RecoveredCreaturePart.class, murmur.getBoundingBox().inflate(8),
				p -> p.parent() == murmur && p.model().endsWith("head")).stream().findFirst().orElse(null);
		float[] full = {0};
		helper.startSequence()
				.thenWaitUntil(() -> helper.assertTrue(head.get() != null, "the Murmur's head, an entity of its own"))
				.thenExecute(() -> {
					var h = head.get();
					helper.assertTrue(io.github.jimbozoomer.jugcraft.tower.crew.PlasmaWounds.partOf(h) == murmur, "the head is the Murmur's");
					Vec3 c = h.getBoundingBox().getCenter();
					var p = io.github.jimbozoomer.jugcraft.tower.crew.PlasmaWounds.hitPart(level, h, level.damageSources().generic(), c, new Vec3(1, 0, 0),
							io.github.jimbozoomer.jugcraft.tower.crew.PlasmaWounds.BOLT_RADIUS);
					helper.assertTrue(p != null && p.entity() == h.getId(), "the head's own wound, got " + (p == null ? null : p.entity() + " for " + h.getId()));
					// where it went in, in the head's frame (from its feet, its turn taken out): on its side, at its middle's height
					float yaw = h.getYRot() * Mth.DEG_TO_RAD;
					double ox = c.x - h.getX(), oz = c.z - h.getZ();
					double side = -h.getBbWidth() / 2 * 1.0001;
					helper.assertTrue(Math.abs(p.hy() - (c.y - h.getY())) < 1e-3 && Math.abs(p.dy()) < 1e-6
							&& Math.abs(p.dx() - Mth.cos(yaw)) < 1e-3 && Math.abs(p.dz() + Mth.sin(yaw)) < 1e-3,
							"the shot in the head's frame: " + p);
					helper.assertTrue(Math.abs((p.hx() * Mth.cos(yaw) - p.hz() * Mth.sin(yaw)) - (ox + side)) < 1e-3,
							"in where the shot met the head's side: " + p + " (side " + side + ")");
					full[0] = murmur.getHealth();
					// a real bolt at the head, from a shooter well off to one side
					var shooter = net.minecraft.world.entity.EntityTypes.ZOMBIE.create(level, net.minecraft.world.entity.EntitySpawnReason.TRIGGERED);
					helper.assertTrue(shooter != null, "a shooter");
					shooter.snapTo(c.x - 6, at.getY(), c.z, 0, 0);
					shooter.setNoAi(true);
					shooter.setPersistenceRequired();
					shooter.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.FIRE_RESISTANCE, 1_000_000, 0, false, false));
					level.addFreshEntity(shooter);
					var bolt = new io.github.jimbozoomer.jugcraft.tower.crew.PlasmaBolt(level, shooter, c.add(-1.5, 0, 0), h);
					bolt.shoot(1, 0, 0, io.github.jimbozoomer.jugcraft.tower.crew.PlasmaRifle.SPEED, 0);
					level.addFreshEntity(bolt);
				})
				.thenWaitUntil(() -> helper.assertTrue(murmur.getHealth() < full[0] || !murmur.isAlive(), "the bolt through its head hurt the Murmur"))
				.thenExecute(() -> {
					for (var e : level.getEntitiesOfClass(net.minecraft.world.entity.Entity.class, murmur.getBoundingBox().inflate(12),
							e -> !(e instanceof net.minecraft.world.entity.player.Player))) {
						e.discard();
					}
				})
				.thenSucceed();
	}

	/**
	 * The taller overworld (Justin, 11 November 2026: "can you change the build limit without messing up world generation?
	 * ... for stuff like this or player builds"; his pick, the top at y 591): the mod's dimension type puts the overworld's
	 * top at 591 (its floor at -64, its generation as the game's), a block goes in at the top, and the core fits as high as
	 * y 330 (its mast 261 over it), not 331.
	 */
	@GameTest(maxTicks = 20)
	public void overworldReachesY591AndTheCoreFitsUpTo330(GameTestHelper helper) {
		if (TestFocus.skip("tower")) {
			helper.succeed();
			return;
		}
		ServerLevel level = helper.getLevel();
		helper.assertTrue(level.getMinY() == -64 && level.getMaxY() == 591, "the overworld from y -64 to 591, got " + level.getMinY() + " to " + level.getMaxY());
		BlockPos top = helper.absolutePos(BlockPos.ZERO).atY(591);
		helper.assertTrue(level.setBlock(top, Blocks.STONE.defaultBlockState(), 3) && level.getBlockState(top).is(Blocks.STONE), "a block at the top, y 591");
		level.setBlock(top, Blocks.AIR.defaultBlockState(), 3);
		helper.assertTrue(io.github.jimbozoomer.jugcraft.tower.TowerCoreBlock.highest(level) == 330, "the core as high as y 330, got "
				+ io.github.jimbozoomer.jugcraft.tower.TowerCoreBlock.highest(level));
		helper.succeed();
	}

	/**
	 * Justin, 11 November 2026 (in survival at the entrance at night, a costumed villain and a skeleton on him, the guard
	 * beside the door doing nothing): "these are hostile mobs, let's kill them". The guard at the entrance (the real post,
	 * perimeter_west, in the real foundation) shoots a hostile mob that comes for a survival player on the grass in front.
	 */
	@GameTest(structure = TriPillarArena.ARENA, maxTicks = 600, skyAccess = true)
	public void entranceGuardShootsWhatComesForAPlayer(GameTestHelper helper) {
		if (TestFocus.skip("tower")) {
			helper.succeed();
			return;
		}
		ServerLevel level = helper.getLevel();
		Player owner = helper.makeMockPlayer(GameType.CREATIVE);
		TowerCoreBlockEntity tower = TriPillarArena.build(helper, owner, 0);
		BlockPos core = tower.getBlockPos();
		var post = TowerData.get().crew().stream().filter(p -> p.id().equals("perimeter_west")).findFirst().orElseThrow();
		BlockPos stand = core.offset(post.spots().getFirst().pos());
		// the world's ground in front of the entrance plaza (its floor at the core's level less one: a step down), out past
		// the arena's edge (the test's barrier wall there taken down)
		for (int x = -16; x <= 12; x++) {
			for (int z = -72; z <= -54; z++) {
				level.setBlock(core.offset(x, -1, z), Blocks.GRASS_BLOCK.defaultBlockState(), net.minecraft.world.level.block.Block.UPDATE_CLIENTS);
				for (int y = 0; y <= 12; y++) {
					if (level.getBlockState(core.offset(x, y, z)).is(Blocks.BARRIER)) {
						level.setBlock(core.offset(x, y, z), Blocks.AIR.defaultBlockState(), net.minecraft.world.level.block.Block.UPDATE_CLIENTS);
					}
				}
			}
		}
		var crew = JugcraftTower.TOWER_CREW.create(level, net.minecraft.world.entity.EntitySpawnReason.TRIGGERED);
		crew.snapTo(stand.getX() + 0.5, stand.getY(), stand.getZ() + 0.5, post.spots().getFirst().yaw(), 0);
		crew.setUp(core, post, io.github.jimbozoomer.jugcraft.tower.crew.CrewRoster.get().hire("security", post.id(), 0, core.asLong(), java.util.List.of()));
		level.addFreshEntity(crew);
		BlockPos ground = core.offset(-4, 0, -58);
		var player = survivalPlayer(helper);
		player.snapTo(ground.getX() + 0.5, ground.getY(), ground.getZ() + 0.5, 0, 0);
		player.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH).setBaseValue(1000);
		player.setHealth(1000);
		// what came for him: a zombie and a skeleton, and the costumed villains (creatures/scary) that roam at night
		java.util.List<net.minecraft.world.entity.EntityType<? extends net.minecraft.world.entity.Mob>> types = java.util.List.of(
				net.minecraft.world.entity.EntityTypes.ZOMBIE, net.minecraft.world.entity.EntityTypes.SKELETON,
				io.github.jimbozoomer.jugcraft.creatures.scary.ScaryMod.CAPTAIN_CUTLER, io.github.jimbozoomer.jugcraft.creatures.scary.ScaryMod.SPACE_KOOK,
				io.github.jimbozoomer.jugcraft.creatures.scary.ScaryMod.PHANTOM_SHADOW, io.github.jimbozoomer.jugcraft.creatures.scary.ScaryMod.BLACK_KNIGHT);
		java.util.List<net.minecraft.world.entity.Mob> mobs = new java.util.ArrayList<>();
		for (int i = 0; i < types.size(); i++) {
			var mob = types.get(i).create(level, net.minecraft.world.entity.EntitySpawnReason.TRIGGERED);
			mob.snapTo(ground.getX() - 5.5 + 2 * i, ground.getY(), ground.getZ() - 5.5, 0, 0);
			mob.setPersistenceRequired();
			mob.setItemSlot(net.minecraft.world.entity.EquipmentSlot.HEAD, new ItemStack(net.minecraft.world.item.Items.LEATHER_HELMET));
			level.addFreshEntity(mob);
			mob.setTarget(player);
			mobs.add(mob);
		}
		StringBuilder log = new StringBuilder();
		int[] ticks = {0};
		helper.startSequence()
				.thenWaitUntil(() -> {
					if (ticks[0]++ % 40 == 0) {
						log.append(String.format(" t%d: guard at %.1f %.1f %.1f target %s act %d nav %s;", ticks[0], crew.getX() - core.getX(), crew.getY() - core.getY(),
								crew.getZ() - core.getZ(), crew.getTarget() == null ? "none" : crew.getTarget().getType().toShortString(), crew.act(),
								crew.getNavigation().getPath() == null ? "none" : crew.getNavigation().getPath().getTarget().subtract(core).toShortString()));
					}
					StringBuilder left = new StringBuilder();
					for (var m : mobs) {
						if (m.getLastHurtByMob() != crew) {
							left.append(String.format(" %s at %.1f %.1f %.1f (seen %b, on %s)", m.getType().toShortString(), m.getX() - core.getX(), m.getY() - core.getY(),
									m.getZ() - core.getZ(), crew.hasLineOfSight(m), m.getTarget() == null ? "none" : m.getTarget().getType().toShortString()));
						}
					}
					if (!left.isEmpty() && ticks[0] % 100 == 50) {
						var m0 = mobs.stream().filter(net.minecraft.world.entity.Mob::isAlive).findFirst().orElse(null);
						if (m0 != null) {
							var hit = level.clip(new net.minecraft.world.level.ClipContext(crew.getEyePosition(), new net.minecraft.world.phys.Vec3(m0.getX(), m0.getEyeY(), m0.getZ()),
									net.minecraft.world.level.ClipContext.Block.COLLIDER, net.minecraft.world.level.ClipContext.Fluid.NONE, crew));
							log.append(" sight to " + m0.getType().toShortString() + " stopped at " + hit.getBlockPos().subtract(core).toShortString() + " "
									+ level.getBlockState(hit.getBlockPos()) + " " + hit.getType() + ";");
						}
					}
					helper.assertTrue(left.isEmpty(), "the guard shoots every one of them; not yet:" + left + " |" + log);
				})
				.thenExecute(() -> {
					leave(player);
					crew.discard();
					mobs.forEach(net.minecraft.world.entity.Entity::discard);
				})
				.thenSucceed();
	}

	/**
	 * The plasma rifle sheet: security (in power armour, so armed) see a hostile mob within range, raise the rifle and
	 * fire; the bolts hit it.
	 */
	@GameTest(structure = "jugcraft-test:drone_depot", maxTicks = 90)
	public void securityShootAHostileMob(GameTestHelper helper) {
		if (TestFocus.skip("tower")) {
			helper.succeed();
			return;
		}
		var crew = rifleRange(helper, "security", new BlockPos(4, 1, 2));
		helper.assertTrue(crew.armed(), "security wear the armour and carry the rifle");
		var husk = husk(helper, new BlockPos(4, 1, 10));
		float full = husk.getHealth();
		boolean[] aimed = {false};
		helper.startSequence()
				.thenWaitUntil(() -> {
					aimed[0] |= crew.act() == io.github.jimbozoomer.jugcraft.tower.crew.TowerCrew.ACT_AIM;
					helper.assertTrue(husk.getHealth() < full, "the husk hit by a bolt");
				})
				.thenExecute(() -> {
					helper.assertTrue(aimed[0], "aimed the rifle to fire");
					crew.discard();
					husk.discard();
					io.github.jimbozoomer.jugcraft.tower.crew.TowerCrewState.get(helper.getLevel()).forget(helper.absolutePos(new BlockPos(1, 1, 1)));
				})
				.thenSucceed();
	}

	/**
	 * The power armour's plasma blade (the power armour sheets, built 9 November 2026): what security are fighting gets
	 * within reach, so the blade comes out in place of the rifle (no bolt is fired with it out), its blows land, and it's
	 * stowed when the fight's over. (Which move at which distance: SquadGameTests.plasmaBladeThrustsAtArmsLengthSlashesClose.)
	 * The blade's tests run in a batch of their own: their husks stand a long while, and a drop ship's guns or another
	 * test's soldiers near would take them for targets.
	 */
	@GameTest(environment = "jugcraft-test:plasma_blade", structure = "jugcraft-test:drone_depot", maxTicks = 260)
	public void securityTakeTheBladeToWhatGetsClose(GameTestHelper helper) {
		if (TestFocus.skip("tower")) {
			helper.succeed();
			return;
		}
		ServerLevel level = helper.getLevel();
		var crew = rifleRange(helper, "security", new BlockPos(4, 1, 2));
		var husk = husk(helper, new BlockPos(4, 1, 5));          // 3 blocks off: 2.7 from its side, in reach
		husk.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH).setBaseValue(400);
		husk.setHealth(400);
		husk.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.KNOCKBACK_RESISTANCE).setBaseValue(1);
		bladeFight(helper, crew, husk, () -> crew.act(), () -> {
			crew.discard();
			io.github.jimbozoomer.jugcraft.tower.crew.TowerCrewState.get(level).forget(helper.absolutePos(new BlockPos(1, 1, 1)));
		});
	}

	/** {@code wearer} (what they're doing: {@code act}) against {@code husk} in reach: the blade out, no bolt with it out,
	 * its blows landing (a thrust's or a slash's damage, less the husk's armour), and stowed once the husk's gone; then
	 * {@code after}. */
	static void bladeFight(GameTestHelper helper, net.minecraft.world.entity.Mob wearer, net.minecraft.world.entity.LivingEntity husk,
			java.util.function.IntSupplier act, Runnable after) {
		ServerLevel level = helper.getLevel();
		wearer.setTarget(husk);
		float full = husk.getHealth();
		boolean[] seen = new boolean[2];                         // out, a bolt while it's out
		float[] last = {full};
		java.util.List<Float> blows = new java.util.ArrayList<>();
		helper.startSequence()
				.thenWaitUntil(() -> {
					boolean out = io.github.jimbozoomer.jugcraft.tower.crew.PlasmaBlade.out(act.getAsInt());
					seen[0] |= out;
					seen[1] |= out && !level.getEntitiesOfClass(io.github.jimbozoomer.jugcraft.tower.crew.PlasmaBolt.class, wearer.getBoundingBox().inflate(12),
							b -> b.getOwner() == wearer && b.tickCount < 2).isEmpty();
					if (husk.getHealth() < last[0] - 0.01F) {
						blows.add(last[0] - husk.getHealth());
						last[0] = husk.getHealth();
					}
					helper.assertTrue(seen[0] && blows.size() >= 3, "the blade out and three blows landed (out " + seen[0] + ", blows " + blows + ", doing "
							+ act.getAsInt() + ", target " + (wearer.getTarget() == husk) + ")");
				})
				.thenExecute(() -> {
					helper.assertTrue(!seen[1], "no bolt fired with the blade out");
					helper.assertTrue(blows.stream().allMatch(b -> b > 15 && b <= io.github.jimbozoomer.jugcraft.tower.crew.PlasmaBlade.THRUST_DAMAGE),
							"each blow a slash's or a thrust's: " + blows);
					helper.assertTrue(husk.getLastHurtByMob() == wearer, "by the blade's wearer");
					husk.discard();
				})
				.thenWaitUntil(() -> helper.assertTrue(!io.github.jimbozoomer.jugcraft.tower.crew.PlasmaBlade.out(act.getAsInt()),
						"the blade stowed once the fight's over"))
				.thenExecute(after)
				.thenSucceed();
	}

	/**
	 * A player who hurts one of the crew: the armed crew near take them as their target and fire; another player is
	 * left alone, and a bolt never hurts the crew.
	 */
	@GameTest(structure = "jugcraft-test:drone_depot", maxTicks = 90)
	public void securityFireBackAtAPlayer(GameTestHelper helper) {
		if (TestFocus.skip("tower")) {
			helper.succeed();
			return;
		}
		ServerLevel level = helper.getLevel();
		var guard = rifleRange(helper, "security", new BlockPos(4, 1, 2));
		var clerk = rifleRange(helper, "controller", new BlockPos(3, 1, 6));
		var player = survivalPlayer(helper);
		BlockPos stand = helper.absolutePos(new BlockPos(4, 1, 10));
		player.snapTo(stand.getX() + 0.5, stand.getY(), stand.getZ() + 0.5, 180, 0);
		var bystander = helper.makeMockServerPlayerInLevel();
		bystander.setGameMode(GameType.SURVIVAL);
		BlockPos aside = helper.absolutePos(new BlockPos(5, 1, 3));
		bystander.snapTo(aside.getX() + 0.5, aside.getY(), aside.getZ() + 0.5, 0, 0);
		clerk.hurtServer(level, level.damageSources().playerAttack(player), 1.0F);
		helper.assertTrue(guard.getTarget() == player, "security took the attacker as their target");
		float clerkHealth = clerk.getHealth();
		var box = new net.minecraft.world.phys.AABB(Vec3.atCenterOf(helper.absolutePos(new BlockPos(2, 0, 0))), Vec3.atCenterOf(helper.absolutePos(new BlockPos(6, 4, 12))));
		helper.startSequence()
				.thenWaitUntil(() -> helper.assertTrue(!level.getEntitiesOfClass(io.github.jimbozoomer.jugcraft.tower.crew.PlasmaBolt.class, box,
						b -> b.getOwner() == guard).isEmpty(), "security fired at the attacker"))
				.thenIdle(15)
				.thenExecute(() -> {
					helper.assertTrue(guard.getTarget() != bystander, "the other player left alone");
					helper.assertTrue(clerk.getHealth() >= clerkHealth, "the crew never hit by a bolt");
					guard.discard();
					clerk.discard();
					leave(player);
					bystander.discard();
					io.github.jimbozoomer.jugcraft.tower.crew.TowerCrewState.get(level).forget(helper.absolutePos(new BlockPos(1, 1, 1)));
				})
				.thenSucceed();
	}

	/**
	 * The creative-menu plasma rifle in a player's hands: a right-click fires a bolt and starts the cooldown; a player's bolt
	 * hits a crew member in its way (Justin, 8 November 2026: "they must be hittable and killable"), and a crew member's
	 * bolt passes another of the crew to hit the mob behind (no friendly fire).
	 */
	@GameTest(structure = "jugcraft-test:drone_depot", maxTicks = 100)
	public void plasmaBoltsHitTheCrewUnlessTheCrewFiredThem(GameTestHelper helper) {
		if (TestFocus.skip("tower")) {
			helper.succeed();
			return;
		}
		ServerLevel level = helper.getLevel();
		var clerk = rifleRange(helper, "controller", new BlockPos(4, 1, 6));
		clerk.setNoAi(true);
		var husk = husk(helper, new BlockPos(4, 1, 10));
		float full = husk.getHealth(), clerkFull = clerk.getHealth();
		var player = helper.makeMockServerPlayerInLevel();
		player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
		BlockPos stand = helper.absolutePos(new BlockPos(4, 1, 2));
		player.snapTo(stand.getX() + 0.5, stand.getY(), stand.getZ() + 0.5, 0, 0);
		player.setYRot(0);
		player.setXRot(0);
		ItemStack rifle = new ItemStack(io.github.jimbozoomer.jugcraft.tower.crew.PlasmaRifle.RIFLE);
		player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, rifle);
		// the trigger (the attack button: the NT-7 works like the guns)
		helper.assertTrue(io.github.jimbozoomer.jugcraft.tower.crew.PlasmaRifleItem.trigger(player) != null, "the rifle fired");
		helper.assertTrue(player.getCooldowns().isOnCooldown(rifle), "the rifle's cooldown started");
		helper.startSequence()
				.thenWaitUntil(() -> helper.assertTrue(clerk.getHealth() < clerkFull || clerk.isDeadOrDying(), "a player's bolt hits the crew member in its way"))
				.thenExecute(() -> {
					helper.assertTrue(husk.getHealth() >= full, "the husk behind untouched");
					player.discard();
					clerk.discard();
				})
				.thenExecute(() -> {
					// now one of the crew fires down the same range, another of them in the way
					var shooter = rifleRange(helper, "security", new BlockPos(4, 1, 2));
					shooter.setNoAi(true);
					var other = rifleRange(helper, "controller", new BlockPos(4, 1, 6));
					other.setNoAi(true);
					float otherFull = other.getHealth();
					Vec3 from = shooter.getEyePosition();
					io.github.jimbozoomer.jugcraft.tower.crew.PlasmaRifle.fire(level, shooter, from, husk.getBoundingBox().getCenter(), husk);
					helper.runAfterDelay(20, () -> {
						helper.assertTrue(other.getHealth() >= otherFull, "a crew member's bolt passes the crew");
						helper.assertTrue(husk.getHealth() < full, "and hits the husk behind");
						shooter.discard();
						other.discard();
						husk.discard();
						io.github.jimbozoomer.jugcraft.tower.crew.TowerCrewState.get(level).forget(helper.absolutePos(new BlockPos(1, 1, 1)));
						helper.succeed();
					});
				})
				.thenIdle(40);
	}

	/**
	 * The NT-7 (P6, 9 November 2026): right click aims down its sight (the item is used, at a gun's walking pace) without
	 * firing; the trigger fires exactly where the middle of the view is (Justin: "the holographic sight ... accurately
	 * lined up with where shots land"): the bolt flies straight from the muzzle to the first thing on the eye's line, no
	 * spread, aimed or from the hip, and hits it; it won't fire again until it's ready.
	 */
	@GameTest(structure = "jugcraft-test:drone_depot", maxTicks = 80)
	public void plasmaRifleShotsLandWhereThePlayerLooks(GameTestHelper helper) {
		if (TestFocus.skip("tower")) {
			helper.succeed();
			return;
		}
		ServerLevel level = helper.getLevel();
		var husk = husk(helper, new BlockPos(6, 1, 11));
		float full = husk.getHealth();
		var player = helper.makeMockServerPlayerInLevel();
		player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
		BlockPos stand = helper.absolutePos(new BlockPos(3, 1, 2));
		player.snapTo(stand.getX() + 0.5, stand.getY(), stand.getZ() + 0.5, 0, 0);
		// look at a point high on the husk's chest, off its middle (a shot that drifted would miss or land elsewhere)
		Vec3 eye = player.getEyePosition();
		Vec3 spot = husk.position().add(0.12, 1.35, 0.0);
		Vec3 d = spot.subtract(eye);
		player.setYRot((float) (Math.toDegrees(Math.atan2(-d.x, d.z))));
		player.setXRot((float) (-Math.toDegrees(Math.atan2(d.y, Math.sqrt(d.x * d.x + d.z * d.z)))));
		ItemStack rifle = new ItemStack(io.github.jimbozoomer.jugcraft.tower.crew.PlasmaRifle.RIFLE);
		player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, rifle);
		// right click aims: the rifle is used, nothing fires
		var result = rifle.getItem().use(level, player, net.minecraft.world.InteractionHand.MAIN_HAND);
		helper.assertTrue(result.consumesAction() && io.github.jimbozoomer.jugcraft.tower.crew.PlasmaRifleItem.aiming(player), "right click aims down the sight");
		helper.assertFalse(player.getCooldowns().isOnCooldown(rifle), "aiming doesn't fire");
		helper.assertTrue(rifle.has(net.minecraft.core.component.DataComponents.USE_EFFECTS)
				&& Math.abs(rifle.get(net.minecraft.core.component.DataComponents.USE_EFFECTS).speedMultiplier()
				- io.github.jimbozoomer.jugcraft.tower.crew.PlasmaRifleItem.AIM_SPEED) < 1e-4, "aimed, you walk at a gun's aiming pace");
		// where it goes: the eye's line meets the husk (near the spot looked at)
		Vec3 aim = io.github.jimbozoomer.jugcraft.tower.crew.PlasmaRifleItem.aimPoint(player, io.github.jimbozoomer.jugcraft.tower.crew.PlasmaRifleItem.REACH);
		helper.assertTrue(husk.getBoundingBox().inflate(0.01).contains(aim), "the eye's line meets the husk: " + aim);
		double off = aim.subtract(eye).normalize().cross(player.getLookAngle()).length();
		helper.assertTrue(off < 1e-6, "the aimed point is on the eye's line (off by " + off + ")");
		var bolt = io.github.jimbozoomer.jugcraft.tower.crew.PlasmaRifleItem.trigger(player);
		helper.assertTrue(bolt != null, "the trigger fires");
		Vec3 from = bolt.position();
		Vec3 want = aim.subtract(from).normalize();
		Vec3 got = bolt.getDeltaMovement().normalize();
		helper.assertTrue(got.subtract(want).length() < 1e-4, "the bolt flies straight at the aimed point, no spread (off by " + got.subtract(want).length() + ")");
		helper.assertTrue(io.github.jimbozoomer.jugcraft.tower.crew.PlasmaRifleItem.trigger(player) == null, "it won't fire again until it's ready");
		// from the hip too: the same line, from the gun's side
		player.stopUsingItem();
		Vec3 hipFrom = io.github.jimbozoomer.jugcraft.tower.crew.PlasmaRifleItem.muzzle(player, false, aim);
		helper.assertTrue(hipFrom.distanceTo(from) > 0.1, "from the hip the bolt starts at the gun's side");
		helper.startSequence()
				.thenWaitUntil(() -> helper.assertTrue(husk.getHealth() < full || husk.isDeadOrDying(), "the bolt hits where the player looked"))
				.thenExecute(() -> {
					player.discard();
					husk.discard();
					helper.succeed();
				});
	}

	/**
	 * Plasma cooks what it kills (Justin, 8 November 2026): a pig killed by a bolt drops cooked porkchops; meat cooks by its
	 * own recipes (a smoker's, so any mod's), anything that isn't food drops as it was.
	 */
	@GameTest(structure = "jugcraft-test:drone_depot", maxTicks = 60)
	public void plasmaCooksTheMeatOfWhatItKills(GameTestHelper helper) {
		if (TestFocus.skip("tower")) {
			helper.succeed();
			return;
		}
		ServerLevel level = helper.getLevel();
		ItemStack raw = new ItemStack(net.minecraft.world.item.Items.PORKCHOP, 3);
		ItemStack cooked = io.github.jimbozoomer.jugcraft.tower.crew.PlasmaWounds.cook(level, raw);
		helper.assertTrue(cooked.is(net.minecraft.world.item.Items.COOKED_PORKCHOP) && cooked.getCount() == 3, "porkchops cook: " + cooked);
		ItemStack hide = new ItemStack(net.minecraft.world.item.Items.LEATHER, 2);
		helper.assertTrue(io.github.jimbozoomer.jugcraft.tower.crew.PlasmaWounds.cook(level, hide).is(net.minecraft.world.item.Items.LEATHER), "leather stays leather");
		ItemStack ore = new ItemStack(net.minecraft.world.item.Items.RAW_IRON);
		helper.assertTrue(io.github.jimbozoomer.jugcraft.tower.crew.PlasmaWounds.cook(level, ore).is(net.minecraft.world.item.Items.RAW_IRON), "raw iron isn't food: as it was");
		var pig = net.minecraft.world.entity.EntityTypes.PIG.create(level, net.minecraft.world.entity.EntitySpawnReason.TRIGGERED);
		BlockPos p = helper.absolutePos(new BlockPos(4, 2, 6));
		pig.snapTo(p.getX() + 0.5, p.getY(), p.getZ() + 0.5, 0, 0);
		pig.setNoAi(true);
		level.addFreshEntity(pig);
		var player = helper.makeMockServerPlayerInLevel();
		player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
		BlockPos stand = helper.absolutePos(new BlockPos(4, 2, 2));
		player.snapTo(stand.getX() + 0.5, stand.getY(), stand.getZ() + 0.5, 0, 0);
		Vec3 from = player.getEyePosition();
		io.github.jimbozoomer.jugcraft.tower.crew.PlasmaRifle.fire(level, player, from, pig.getBoundingBox().getCenter(), pig);
		net.minecraft.world.phys.AABB around = new net.minecraft.world.phys.AABB(p).inflate(3);
		helper.startSequence()
				.thenWaitUntil(() -> helper.assertTrue(!pig.isAlive(), "the bolt kills the pig"))
				.thenWaitUntil(() -> {
					var items = level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class, around);
					helper.assertTrue(items.stream().anyMatch(i -> i.getItem().is(net.minecraft.world.item.Items.COOKED_PORKCHOP))
							&& items.stream().noneMatch(i -> i.getItem().is(net.minecraft.world.item.Items.PORKCHOP)), "it drops its porkchops cooked: " + items);
				})
				.thenExecute(() -> {
					for (var i : level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class, around)) {
						i.discard();
					}
					player.discard();
				})
				.thenSucceed();
	}

	/**
	 * The plasma baton sheet: a gate guard (wearing the holster, so carrying the baton) sees a hostile mob, draws the baton
	 * (lit as it comes out), goes to it and strikes; when it's dead they put the baton away and go back to work.
	 */
	@GameTest(structure = "jugcraft-test:drone_depot", maxTicks = 220)
	public void gateGuardDrawsTheBatonAndPutsItAway(GameTestHelper helper) {
		if (TestFocus.skip("tower")) {
			helper.succeed();
			return;
		}
		var crew = rifleRange(helper, "gate_guard", new BlockPos(4, 1, 2));
		helper.assertTrue(crew.batoned(), "gate guards wear the holster and carry the baton");
		helper.assertTrue(!crew.armed(), "gate guards carry no rifle");
		var husk = husk(helper, new BlockPos(4, 1, 7));
		float full = husk.getHealth();
		boolean[] drew = {false}, struck = {false};
		helper.startSequence()
				.thenWaitUntil(() -> {
					drew[0] |= crew.act() == io.github.jimbozoomer.jugcraft.tower.crew.TowerCrew.ACT_DRAW
							&& crew.heldProp() == io.github.jimbozoomer.jugcraft.tower.crew.TowerCrew.PROP_BATON;
					struck[0] |= crew.act() == io.github.jimbozoomer.jugcraft.tower.crew.TowerCrew.ACT_STRIKE;
					helper.assertTrue(husk.getHealth() < full, "the husk struck with the baton");
				})
				.thenExecute(() -> {
					helper.assertTrue(drew[0], "drew the baton first");
					helper.assertTrue(struck[0], "struck with it");
					helper.assertTrue(full - husk.getHealth() >= io.github.jimbozoomer.jugcraft.tower.crew.PlasmaBaton.DAMAGE * 0.9F
							|| husk.isDeadOrDying(), "a blow does the baton's damage (less the husk's bit of armour): " + (full - husk.getHealth()));
					husk.discard();
				})
				.thenWaitUntil(() -> helper.assertTrue(crew.act() == io.github.jimbozoomer.jugcraft.tower.crew.TowerCrew.ACT_IDLE
						&& crew.heldProp() != io.github.jimbozoomer.jugcraft.tower.crew.TowerCrew.PROP_BATON, "the baton put away"))
				.thenExecute(() -> {
					crew.discard();
					io.github.jimbozoomer.jugcraft.tower.crew.TowerCrewState.get(helper.getLevel()).forget(helper.absolutePos(new BlockPos(1, 1, 1)));
				})
				.thenSucceed();
	}

	/** A player who hurts one of the crew: a gate guard near takes them as their target and draws the baton; security too. */
	@GameTest(structure = "jugcraft-test:drone_depot", maxTicks = 60)
	public void gateGuardGoesForAPlayerWhoHurtsTheCrew(GameTestHelper helper) {
		if (TestFocus.skip("tower")) {
			helper.succeed();
			return;
		}
		ServerLevel level = helper.getLevel();
		var guard = rifleRange(helper, "gate_guard", new BlockPos(4, 1, 2));
		var clerk = rifleRange(helper, "controller", new BlockPos(3, 1, 6));
		var player = survivalPlayer(helper);
		BlockPos stand = helper.absolutePos(new BlockPos(4, 1, 10));
		player.snapTo(stand.getX() + 0.5, stand.getY(), stand.getZ() + 0.5, 180, 0);
		clerk.hurtServer(level, level.damageSources().playerAttack(player), 1.0F);
		helper.assertTrue(guard.getTarget() == player, "the gate guard took the attacker as their target");
		helper.startSequence()
				.thenWaitUntil(() -> helper.assertTrue(guard.heldProp() == io.github.jimbozoomer.jugcraft.tower.crew.TowerCrew.PROP_BATON,
						"the gate guard drew the baton"))
				.thenExecute(() -> {
					guard.discard();
					clerk.discard();
					leave(player);
					io.github.jimbozoomer.jugcraft.tower.crew.TowerCrewState.get(level).forget(helper.absolutePos(new BlockPos(1, 1, 1)));
				})
				.thenSucceed();
	}

	/** The creative-menu plasma baton in a player's hands: a diamond sword's blow (7: the fist's 1 and 6 more), and a sear on a hit. */
	@GameTest(structure = "jugcraft-test:drone_depot", maxTicks = 20)
	public void plasmaBatonHitsLikeASword(GameTestHelper helper) {
		if (TestFocus.skip("tower")) {
			helper.succeed();
			return;
		}
		ItemStack baton = new ItemStack(io.github.jimbozoomer.jugcraft.tower.crew.PlasmaBaton.BATON);
		double damage = baton.getOrDefault(net.minecraft.core.component.DataComponents.ATTRIBUTE_MODIFIERS,
				net.minecraft.world.item.component.ItemAttributeModifiers.EMPTY)
				.compute(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE, 1.0, net.minecraft.world.entity.EquipmentSlot.MAINHAND);
		helper.assertTrue(Math.abs(damage - io.github.jimbozoomer.jugcraft.tower.crew.PlasmaBaton.DAMAGE) < 0.01,
				"the baton hits for " + io.github.jimbozoomer.jugcraft.tower.crew.PlasmaBaton.DAMAGE + " (was " + damage + ")");
		var husk = husk(helper, new BlockPos(4, 1, 4));
		var player = helper.makeMockServerPlayerInLevel();
		baton.getItem().hurtEnemy(baton, husk, player);               // the sear's sound and sparks: no trouble
		husk.discard();
		player.discard();
		helper.succeed();
	}

	/**
	 * Plasma wounds: a bolt burns right through whatever it hits, thin (a husk) or bulky (an iron golem), from where it
	 * goes in on the near side, along the shot; the baton's gash lies across the blow.
	 */
	@GameTest(structure = "jugcraft-test:drone_depot", maxTicks = 20)
	public void plasmaBurnsRightThrough(GameTestHelper helper) {
		if (TestFocus.skip("tower")) {
			helper.succeed();
			return;
		}
		var husk = husk(helper, new BlockPos(4, 1, 4));
		var golem = net.minecraft.world.entity.EntityTypes.IRON_GOLEM.create(helper.getLevel(), net.minecraft.world.entity.EntitySpawnReason.TRIGGERED);
		helper.assertTrue(golem != null, "an iron golem");
		BlockPos g = helper.absolutePos(new BlockPos(4, 1, 8));
		golem.snapTo(g.getX() + 0.5, g.getY(), g.getZ() + 0.5, 0, 0);
		golem.yBodyRot = 0;
		husk.yBodyRot = 0;
		Vec3 way = new Vec3(0, 0, 1);
		var random = net.minecraft.util.RandomSource.create(7);
		for (var target : java.util.List.<net.minecraft.world.entity.LivingEntity>of(husk, golem)) {
			var p = io.github.jimbozoomer.jugcraft.tower.crew.PlasmaWounds.wound(target, target.getBoundingBox().getCenter(), way, false, random);
			helper.assertTrue(p != null && p.kind() == io.github.jimbozoomer.jugcraft.tower.crew.PlasmaWounds.THROUGH, "right through the " + target.getType());
			double half = target.getBbWidth() / 2;
			helper.assertTrue(Math.abs(p.hz() + half) < 0.01 && Math.abs(p.dz() - 1) < 0.01 && Math.abs(p.hx()) < 0.01,
					"in at the near side, along the shot (" + p.hx() + ", " + p.hz() + "; " + p.dz() + ")");
		}
		var gash = io.github.jimbozoomer.jugcraft.tower.crew.PlasmaWounds.wound(husk, husk.getBoundingBox().getCenter(), way, true, random);
		helper.assertTrue(gash != null && gash.kind() == io.github.jimbozoomer.jugcraft.tower.crew.PlasmaWounds.GASH && gash.length() > 0
				&& Math.abs(gash.ux() * gash.dx() + gash.uy() * gash.dy() + gash.uz() * gash.dz()) < 0.01, "the baton's gash lies across the blow");
		io.github.jimbozoomer.jugcraft.tower.crew.PlasmaWounds.burn(helper.getLevel(), husk, husk.getBoundingBox().getCenter(), way, false);   // no players near: nothing sent, no trouble
		husk.discard();
		golem.discard();
		helper.succeed();
	}
	/**
	 * What kills outright (the power armour sheets, take 7: "if it does something like cut a torso in half or a head is
	 * decimated that the creature simply dies"; the server judges it on its own measure of the creature,
	 * PlasmaWounds.Vitals): a slash from close enough that the blade goes right through its trunk; a swing through its neck;
	 * three bolts through its head (one isn't enough). Anything less it lives through: a slash that only reaches its
	 * chest, a bolt past its trunk through an arm. A creature with a boss's health, a warden and a player are never
	 * killed that way (marked guarded); and a blow that kills by its damage is marked fatal too.
	 */
	@GameTest(environment = "jugcraft-test:plasma_blade", structure = "jugcraft-test:drone_depot", maxTicks = 20)
	public void plasmaCutsThatSeverKillOrdinaryCreaturesOutright(GameTestHelper helper) {
		if (TestFocus.skip("tower")) {
			helper.succeed();
			return;
		}
		ServerLevel level = helper.getLevel();
		var random = net.minecraft.util.RandomSource.create(11);
		var by = husk(helper, new BlockPos(2, 1, 2));
		BlockPos at = helper.absolutePos(new BlockPos(2, 1, 2));
		by.snapTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, 0, 0);
		var source = by.damageSources().mobAttack(by);
		java.util.function.DoubleFunction<net.minecraft.world.entity.monster.zombie.Husk> victim = off -> {
			var v = husk(helper, new BlockPos(2, 1, 4));
			v.snapTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5 + off, 180, 0);       // facing the swordsman, `off` blocks from them
			v.yBodyRot = 180;
			v.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH).setBaseValue(150);
			v.setHealth(150);
			return v;
		};
		java.util.function.Function<net.minecraft.world.entity.LivingEntity, io.github.jimbozoomer.jugcraft.tower.crew.PlasmaWounds.Swing> slash = v ->
				io.github.jimbozoomer.jugcraft.tower.crew.PlasmaWounds.swing(by, v, null, io.github.jimbozoomer.jugcraft.tower.crew.PlasmaBlade.SLASH_TILT,
						io.github.jimbozoomer.jugcraft.tower.crew.PlasmaBlade.INNER, io.github.jimbozoomer.jugcraft.tower.crew.PlasmaBlade.OUTER,
						io.github.jimbozoomer.jugcraft.tower.crew.PlasmaBlade.SLASH_HALF, io.github.jimbozoomer.jugcraft.tower.crew.PlasmaBlade.EDGE);
		// a slash from 0.45 off its side: the blade right through its trunk
		var near = victim.apply(0.75);
		var p = io.github.jimbozoomer.jugcraft.tower.crew.PlasmaWounds.judge(level, near, source,
				io.github.jimbozoomer.jugcraft.tower.crew.PlasmaWounds.fan(near, slash.apply(near), random));
		helper.assertTrue(p != null && p.kind() == io.github.jimbozoomer.jugcraft.tower.crew.PlasmaWounds.FAN && p.fatal() && !p.guarded() && near.isDeadOrDying(),
				"a slash right through its trunk kills it at once (health " + near.getHealth() + ")");
		// the same slash from a block off its side: only into its chest
		var far = victim.apply(1.3);
		p = io.github.jimbozoomer.jugcraft.tower.crew.PlasmaWounds.judge(level, far, source,
				io.github.jimbozoomer.jugcraft.tower.crew.PlasmaWounds.fan(far, slash.apply(far), random));
		helper.assertTrue(p != null && !p.fatal() && far.isAlive() && far.getHealth() == 150, "a slash that only reaches its chest doesn't");
		// a level swing through its neck
		var neck = victim.apply(0.75);
		var level2 = io.github.jimbozoomer.jugcraft.tower.crew.PlasmaWounds.swing(by, neck, new Vec3(neck.getX(), neck.getY() + 1.44, neck.getZ()), 90F,
				io.github.jimbozoomer.jugcraft.tower.crew.PlasmaBlade.INNER, io.github.jimbozoomer.jugcraft.tower.crew.PlasmaBlade.OUTER, 55F, 1F);
		p = io.github.jimbozoomer.jugcraft.tower.crew.PlasmaWounds.judge(level, neck, source,
				io.github.jimbozoomer.jugcraft.tower.crew.PlasmaWounds.fan(neck, level2, random));
		helper.assertTrue(p != null && p.fatal() && neck.isDeadOrDying(), "a swing through its neck takes its head off: dead at once");
		// bolts through its head, from its side: one it lives through, three it doesn't
		var head = victim.apply(1.3);
		Vec3 across = new Vec3(1, 0, 0);
		p = io.github.jimbozoomer.jugcraft.tower.crew.PlasmaWounds.judge(level, head, source,
				io.github.jimbozoomer.jugcraft.tower.crew.PlasmaWounds.wound(head, new Vec3(head.getX(), head.getY() + 1.82, head.getZ() - 0.1), across, false, random));
		helper.assertTrue(p != null && !p.fatal() && head.isAlive(), "one bolt through its head it lives through");
		io.github.jimbozoomer.jugcraft.tower.crew.PlasmaWounds.judge(level, head, source,
				io.github.jimbozoomer.jugcraft.tower.crew.PlasmaWounds.wound(head, new Vec3(head.getX(), head.getY() + 1.60, head.getZ() + 0.12), across, false, random));
		p = io.github.jimbozoomer.jugcraft.tower.crew.PlasmaWounds.judge(level, head, source,
				io.github.jimbozoomer.jugcraft.tower.crew.PlasmaWounds.wound(head, new Vec3(head.getX(), head.getY() + 1.62, head.getZ() - 0.14), across, false, random));
		helper.assertTrue(p != null && p.fatal() && head.isDeadOrDying(), "three bolts through its head: more than half of it gone, dead at once");
		// a bolt through an arm, past its trunk
		var arm = victim.apply(1.3);
		p = io.github.jimbozoomer.jugcraft.tower.crew.PlasmaWounds.judge(level, arm, source,
				io.github.jimbozoomer.jugcraft.tower.crew.PlasmaWounds.wound(arm, new Vec3(arm.getX() + 0.38, arm.getY() + 1.1, arm.getZ()), new Vec3(0, 0, 1), false, random));
		helper.assertTrue(p != null && p.flags() == 0 && arm.isAlive() && arm.getHealth() == 150, "a bolt through an arm, past its trunk: it fights on");
		// a blow that kills by its damage is fatal too
		arm.setHealth(1);
		arm.hurtServer(level, source, 5);
		p = io.github.jimbozoomer.jugcraft.tower.crew.PlasmaWounds.judge(level, arm, source,
				io.github.jimbozoomer.jugcraft.tower.crew.PlasmaWounds.wound(arm, new Vec3(arm.getX() + 0.38, arm.getY() + 1.1, arm.getZ()), new Vec3(0, 0, 1), false, random));
		helper.assertTrue(p != null && p.fatal(), "a blow that kills by its damage is marked fatal");
		// one with a boss's health, a warden, a player: never outright
		var tough = victim.apply(0.75);
		tough.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH).setBaseValue(400);
		tough.setHealth(400);
		p = io.github.jimbozoomer.jugcraft.tower.crew.PlasmaWounds.judge(level, tough, source,
				io.github.jimbozoomer.jugcraft.tower.crew.PlasmaWounds.fan(tough, slash.apply(tough), random));
		helper.assertTrue(p != null && p.guarded() && !p.fatal() && tough.getHealth() == 400, "one with a boss's health is only gashed");
		var warden = net.minecraft.world.entity.EntityTypes.WARDEN.create(level, net.minecraft.world.entity.EntitySpawnReason.TRIGGERED);
		var player = helper.makeMockServerPlayerInLevel();
		helper.assertTrue(warden != null && io.github.jimbozoomer.jugcraft.tower.crew.PlasmaWounds.guarded(warden)
				&& io.github.jimbozoomer.jugcraft.tower.crew.PlasmaWounds.guarded(player) && !io.github.jimbozoomer.jugcraft.tower.crew.PlasmaWounds.guarded(far),
				"a warden and a player are guarded, a husk isn't");
		// the fan passes through what stands in it, not what stands beside or beyond it
		helper.assertTrue(slash.apply(far).meets(far.getBoundingBox()) && !slash.apply(far).meets(far.getBoundingBox().move(0, 0, 1.5))
				&& !slash.apply(far).meets(far.getBoundingBox().move(2.0, 0, 0)), "the swing meets what's in its path only");
		warden.discard();
		player.discard();
		for (var e : java.util.List.of(by, near, far, neck, head, arm, tough)) {
			e.discard();
		}
		helper.succeed();
	}
}
