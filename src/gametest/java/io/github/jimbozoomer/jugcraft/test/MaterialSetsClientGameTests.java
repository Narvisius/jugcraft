package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;

/**
 * Client game test for the material sets (docs/features/material-sets.md): the redrawn ingots, nuggets, storage blocks,
 * ores, raw ores, raw blocks and bronze and steel tools, each shown beside vanilla's own so the owner can judge them in
 * game (CI job {@code client}).
 *
 * <p>Scenes, every camera facing north:
 * <ul>
 * <li>a wall of vanilla stone over vanilla deepslate with ores set in it: four groups, each vanilla's iron, copper, gold
 * or coal ore then three of ours (does an ore's base match the rock round it, and does its overlay show cut out?);</li>
 * <li>stone and deepslate let into the floor with ores in them (the top faces);</li>
 * <li>storage blocks in panels, vanilla's iron, gold and copper then three of ours, with the raw blocks on top;</li>
 * <li>item frames with vanilla's iron, gold and copper ingots (above) and nuggets (below) beside three of ours, each panel
 * whole and then close up: the ingot and nugget are drawn in vanilla's form from memory, and this is where that form is
 * judged;</li>
 * <li>frames of vanilla's iron and gold tools above our bronze and steel ones, and of vanilla's raw ores beside ours;</li>
 * <li>five survival inventories, ours beside vanilla's in the slots;</li>
 * <li>the steel pickaxe, bronze sword and bronze ingot in the hand.</li>
 * </ul>
 *
 * <p>Every id is looked up in the registry first, so a missing one fails the test. The log lists what was placed; only
 * the screenshots show how it renders.
 */
public class MaterialSetsClientGameTests implements FabricClientGameTest {
	/** The fourteen metals in tools/materials.py order; the block and ingot panels take them three at a time. */
	private static final List<String> METALS = List.of("tin", "zinc", "lead", "silver", "nickel", "tungsten", "uranium",
			"titanium", "bronze", "aluminum", "brass", "invar", "solder", "steel");
	/** The metals that are mined, so have a raw ore and a raw block. */
	private static final List<String> MINED = METALS.subList(0, 8);
	/** Our twelve ores, each in stone and in deepslate. */
	private static final List<String> ORES = List.of("tin", "zinc", "lead", "silver", "nickel", "tungsten", "uranium",
			"titanium", "salt", "phosphate", "lepidolite", "monazite");
	/** The vanilla ore that leads each group of three of ours on the ore wall (coal goes beside our minerals). */
	private static final List<String> VANILLA_ORES = List.of("iron", "copper", "gold", "coal");
	/** Vanilla's metals, set before ours in every panel and inventory row. */
	private static final List<String> VANILLA_METALS = List.of("iron", "gold", "copper");
	private static final List<String> TOOLS = List.of("sword", "pickaxe", "axe", "shovel", "hoe", "paxel");
	/** Our two tool sets, and for each the vanilla tier shown above it and our older-style paxel of that tier. */
	private static final List<String> TOOL_TIERS = List.of("bronze", "steel");
	private static final List<String> VANILLA_TOOL_TIERS = List.of("iron", "golden");
	private static final List<String> PAXEL_TIERS = List.of("iron", "gold");

	// The scenes' walls, as offsets north of the spawn point.
	private static final int ORE_WALL = -8;
	private static final int BLOCK_WALL = -18;
	private static final int INGOT_WALL = -28;
	private static final int TOOL_WALL = -38;
	/** From one panel's first column to the next: six columns used and two left between. */
	private static final int PANEL = 8;
	/** Panels of three of our metals each: fourteen metals make five. */
	private static final int PANELS = 5;
	/** The first column of the raw ore panels on the tool wall, east of the tools. */
	private static final int RAW_START = 15;

	@Override
	public void runTest(ClientGameTestContext context) {
		try (TestSingleplayerContext singleplayer = context.worldBuilder()
				.adjustSettings(creator -> creator.setGameMode(WorldCreationUiState.SelectedGameMode.CREATIVE)).create()) {
			singleplayer.getConnection().waitForChunksRender();
			BlockPos origin = context.computeOnClient(client -> BlockPos.containing(client.player.position()));
			int x = origin.getX();
			int y = origin.getY();
			int z = origin.getZ();
			TestServerContext server = singleplayer.getServer();
			server.runCommand("time set noon");
			server.runCommand("weather clear");
			server.runCommand("gamerule minecraft:send_command_feedback false");
			server.runCommand("fill %d %d %d %d %d %d minecraft:smooth_stone".formatted(x - 4, y - 1, z - 42, x + 44, y - 1, z + 8));
			server.runCommand("fill %d %d %d %d %d %d minecraft:air".formatted(x - 4, y, z - 42, x + 44, y + 8, z + 8));
			context.waitTicks(10);

			List<String> placed = new ArrayList<>();
			oreWall(server, x, y, z + ORE_WALL, placed);
			oreFloor(server, x, y, z, placed);
			blockWall(server, x, y, z + BLOCK_WALL, placed);
			ingotWall(server, x, y, z + INGOT_WALL, placed);
			toolWall(server, x, y, z + TOOL_WALL, placed);
			Jugcraft.LOGGER.info("[material sets client] placed {} blocks and framed items: {}", placed.size(), placed);
			context.waitTicks(20);

			// Hide the HUD, hand and chat for the scenery shots, whatever an earlier test in this client left.
			context.runOnClient(client -> {
				if (!client.gui.hud.isHidden()) {
					client.gui.hud.toggle();
				}
			});

			// 1. The ore wall whole, then each group (vanilla's ore and three of ours, stone above, deepslate below) close up.
			shoot(context, singleplayer, x + 16.5, y + 1, front(z + ORE_WALL, 14), 0, "jugcraft_material_sets_ores_wall");
			for (int group = 0; group < VANILLA_ORES.size(); group++) {
				shoot(context, singleplayer, x + 4.5 + 8 * group, y + 1, front(z + ORE_WALL, 3.25), 0,
						"jugcraft_material_sets_ores_close_" + (group + 1));
			}
			// 2. The floor patches from above: the ores' top faces in stone and in deepslate.
			shoot(context, singleplayer, x + 12.5, y + 4, z + 3.5, 50, "jugcraft_material_sets_ores_floor");
			// 3. Storage blocks on the floor, raw blocks on top: a panel a shot, vanilla's three beside ours.
			for (int panel = 0; panel < PANELS; panel++) {
				shoot(context, singleplayer, x + PANEL * panel + 3.0, y, front(z + BLOCK_WALL, 3), 8,
						"jugcraft_material_sets_blocks_" + (panel + 1));
			}
			// 4. Ingots (upper row) and nuggets (lower row): each panel whole, then close up on its middle, vanilla's gold
			// and copper beside two of ours.
			for (int panel = 0; panel < PANELS; panel++) {
				shoot(context, singleplayer, x + PANEL * panel + 3.0, y, front(z + INGOT_WALL, 3), -8,
						"jugcraft_material_sets_ingots_" + (panel + 1));
				shoot(context, singleplayer, x + PANEL * panel + 3.0, y, front(z + INGOT_WALL, 1.6), -13,
						"jugcraft_material_sets_ingots_close_" + (panel + 1));
			}
			// 5. Tools, vanilla above ours: iron over bronze, then gold over steel; then three of each set a close-up.
			for (int set = 0; set < TOOL_TIERS.size(); set++) {
				shoot(context, singleplayer, x + 7 * set + 3.0, y + 1, front(z + TOOL_WALL, 3), -7,
						"jugcraft_material_sets_tools_" + (set + 1));
			}
			for (int part = 0; part < 2 * TOOL_TIERS.size(); part++) {
				shoot(context, singleplayer, x + 7 * (part / 2) + 3 * (part % 2) + 1.5, y + 1, front(z + TOOL_WALL, 1.5), -14,
						"jugcraft_material_sets_tools_close_" + (part + 1));
			}
			// 6. Raw ores: vanilla's raw iron, gold and copper beside three of ours a panel.
			for (int panel = 0; 3 * panel < MINED.size(); panel++) {
				shoot(context, singleplayer, x + RAW_START + PANEL * panel + 3.0, y + 1, front(z + TOOL_WALL, 3), 2,
						"jugcraft_material_sets_raw_" + (panel + 1));
			}

			context.runOnClient(client -> {
				if (client.gui.hud.isHidden()) {
					client.gui.hud.toggle();
				}
			});

			// 7. The survival inventory, the truest test of whether ours sit with vanilla's as one set.
			server.runCommand(String.format(Locale.ROOT, "tp @p %.1f %d %.1f 180 0", x + 0.5, y, z + 4.5));
			server.runCommand("gamemode survival @p");
			context.waitTicks(10);
			inventory(context, server, ingotSlots(), "jugcraft_material_sets_inventory_ingots");
			inventory(context, server, blockSlots(), "jugcraft_material_sets_inventory_blocks");
			inventory(context, server, rawSlots(), "jugcraft_material_sets_inventory_raw");
			inventory(context, server, oreSlots(), "jugcraft_material_sets_inventory_ores");
			inventory(context, server, toolSlots(), "jugcraft_material_sets_inventory_tools");
			server.runCommand("clear @p");
			server.runCommand("gamemode creative @p");

			// 8. In the hand, first person, the hotbar showing.
			context.runOnClient(client -> client.options.setCameraType(CameraType.FIRST_PERSON));
			for (String held : List.of("steel_pickaxe", "bronze_sword", "bronze_ingot")) {
				server.runCommand("item replace entity @p weapon.mainhand with " + id("jugcraft:" + held));
				context.waitTicks(30);
				context.takeScreenshot("jugcraft_material_sets_held_" + held);
			}
			server.runCommand("item replace entity @p weapon.mainhand with minecraft:air");
		}
	}

	/**
	 * Deepslate in the lower two rows and stone in the four above, 33 wide; in every second column an ore in each band:
	 * four groups, each vanilla's ore then three of ours.
	 */
	private static void oreWall(TestServerContext server, int x, int y, int wall, List<String> placed) {
		server.runCommand("fill %d %d %d %d %d %d minecraft:deepslate".formatted(x, y, wall, x + 32, y + 1, wall));
		server.runCommand("fill %d %d %d %d %d %d minecraft:stone".formatted(x, y + 2, wall, x + 32, y + 5, wall));
		for (int group = 0; group < VANILLA_ORES.size(); group++) {
			for (int i = 0; i < 4; i++) {
				String namespace = i == 0 ? "minecraft:" : "jugcraft:";
				String ore = i == 0 ? VANILLA_ORES.get(group) : ORES.get(3 * group + i - 1);
				int column = x + 1 + 8 * group + 2 * i;
				setblock(server, column, y + 3, wall, namespace + ore + "_ore", placed);
				setblock(server, column, y + 1, wall, namespace + "deepslate_" + ore + "_ore", placed);
			}
		}
	}

	/** A patch of stone and one of deepslate let into the floor, each with vanilla's iron ore and two of ours. */
	private static void oreFloor(TestServerContext server, int x, int y, int z, List<String> placed) {
		server.runCommand("fill %d %d %d %d %d %d minecraft:stone".formatted(x + 5, y - 1, z - 3, x + 11, y - 1, z - 1));
		server.runCommand("fill %d %d %d %d %d %d minecraft:deepslate".formatted(x + 13, y - 1, z - 3, x + 19, y - 1, z - 1));
		List<String> stone = List.of("minecraft:iron_ore", "jugcraft:tin_ore", "jugcraft:lepidolite_ore");
		List<String> deep = List.of("minecraft:deepslate_iron_ore", "jugcraft:deepslate_tin_ore", "jugcraft:deepslate_lepidolite_ore");
		for (int i = 0; i < stone.size(); i++) {
			setblock(server, x + 6 + 2 * i, y - 1, z - 2, stone.get(i), placed);
			setblock(server, x + 14 + 2 * i, y - 1, z - 2, deep.get(i), placed);
		}
	}

	/**
	 * Storage blocks on the floor in five panels, each vanilla's iron, gold and copper blocks then three of ours (two in
	 * the last); on the first three, vanilla's raw blocks then three of ours (two in the third) on top.
	 */
	private static void blockWall(TestServerContext server, int x, int y, int wall, List<String> placed) {
		for (int panel = 0; panel < PANELS; panel++) {
			List<String> storage = panel(METALS, panel, "minecraft:%s_block", "jugcraft:%s_block");
			List<String> raw = panel(MINED, panel, "minecraft:raw_%s_block", "jugcraft:raw_%s_block");
			for (int i = 0; i < storage.size(); i++) {
				setblock(server, x + PANEL * panel + i, y, wall, storage.get(i), placed);
			}
			for (int i = 0; i < raw.size(); i++) {
				setblock(server, x + PANEL * panel + i, y + 1, wall, raw.get(i), placed);
			}
		}
	}

	/** Five panels of frames, each vanilla's iron, gold and copper then three of ours (two in the last); ingots above. */
	private static void ingotWall(TestServerContext server, int x, int y, int wall, List<String> placed) {
		server.runCommand("fill %d %d %d %d %d %d minecraft:spruce_planks".formatted(x - 1, y, wall, x + PANEL * PANELS - 1, y + 4, wall));
		for (int panel = 0; panel < PANELS; panel++) {
			List<String> ingots = panel(METALS, panel, "minecraft:%s_ingot", "jugcraft:%s_ingot");
			List<String> nuggets = panel(METALS, panel, "minecraft:%s_nugget", "jugcraft:%s_nugget");
			for (int i = 0; i < ingots.size(); i++) {
				frame(server, x + PANEL * panel + i, y + 2, wall + 1, ingots.get(i), placed);
				frame(server, x + PANEL * panel + i, y + 1, wall + 1, nuggets.get(i), placed);
			}
		}
	}

	/**
	 * Tools: vanilla iron (and our older-style iron paxel) above our bronze six, a column's gap, vanilla gold (and our
	 * gold paxel) above our steel six. East of them, raw ores in three panels: vanilla's raw iron, gold and copper then
	 * three of ours (two in the last).
	 */
	private static void toolWall(TestServerContext server, int x, int y, int wall, List<String> placed) {
		server.runCommand("fill %d %d %d %d %d %d minecraft:spruce_planks".formatted(x - 1, y, wall, x + RAW_START + 2 * PANEL + 6, y + 4, wall));
		for (int set = 0; set < TOOL_TIERS.size(); set++) {
			List<String> vanilla = vanillaTools(set);
			for (int i = 0; i < TOOLS.size(); i++) {
				frame(server, x + 7 * set + i, y + 3, wall + 1, vanilla.get(i), placed);
				frame(server, x + 7 * set + i, y + 2, wall + 1, "jugcraft:" + TOOL_TIERS.get(set) + "_" + TOOLS.get(i), placed);
			}
		}
		for (int panel = 0; 3 * panel < MINED.size(); panel++) {
			List<String> raws = panel(MINED, panel, "minecraft:raw_%s", "jugcraft:raw_%s");
			for (int i = 0; i < raws.size(); i++) {
				frame(server, x + RAW_START + PANEL * panel + i, y + 2, wall + 1, raws.get(i), placed);
			}
		}
	}

	/** Vanilla's sword, pickaxe, axe, shovel and hoe of tool set {@code set}'s tier, then our paxel of that tier. */
	private static List<String> vanillaTools(int set) {
		List<String> tools = new ArrayList<>();
		for (String tool : TOOLS) {
			tools.add(tool.equals("paxel") ? "jugcraft:" + PAXEL_TIERS.get(set) + "_paxel"
					: "minecraft:" + VANILLA_TOOL_TIERS.get(set) + "_" + tool);
		}
		return tools;
	}

	/**
	 * Vanilla's iron, gold and copper in the {@code vanilla} pattern, then panel {@code panel}'s three of {@code ours} in
	 * the {@code jugcraft} pattern; empty once {@code ours} has run out.
	 */
	private static List<String> panel(List<String> ours, int panel, String vanilla, String jugcraft) {
		List<String> ids = new ArrayList<>();
		if (3 * panel >= ours.size()) {
			return ids;
		}
		for (String metal : VANILLA_METALS) {
			ids.add(vanilla.formatted(metal));
		}
		for (String metal : ours.subList(3 * panel, Math.min(ours.size(), 3 * panel + 3))) {
			ids.add(jugcraft.formatted(metal));
		}
		return ids;
	}

	/** Rows of nine: vanilla's iron ingots and nuggets beside eight of ours, then gold and copper beside the other six. */
	private static List<String> ingotSlots() {
		List<String> slots = new ArrayList<>();
		for (String form : List.of("_ingot", "_nugget")) {
			slots.add("minecraft:iron" + form);
			for (String metal : METALS.subList(0, 8)) {
				slots.add("jugcraft:" + metal + form);
			}
		}
		for (String form : List.of("_ingot", "_nugget")) {
			slots.add("minecraft:gold" + form);
			slots.add("minecraft:copper" + form);
			for (String metal : METALS.subList(8, 14)) {
				slots.add("jugcraft:" + metal + form);
			}
			pad(slots);
		}
		return slots;
	}

	/** Storage blocks: three rows, each vanilla's iron, gold and copper blocks then six of ours (two in the last). */
	private static List<String> blockSlots() {
		return rows(VANILLA_METALS, METALS, "minecraft:%s_block", "jugcraft:%s_block");
	}

	/** Raw ores, then raw blocks: each row vanilla's three then six of ours (two in the second). */
	private static List<String> rawSlots() {
		List<String> slots = rows(VANILLA_METALS, MINED, "minecraft:raw_%s", "jugcraft:raw_%s");
		slots.addAll(rows(VANILLA_METALS, MINED, "minecraft:raw_%s_block", "jugcraft:raw_%s_block"));
		return slots;
	}

	/** The ores as items (the two-layer model in the slot): stone, then deepslate, each row vanilla's three then six of ours. */
	private static List<String> oreSlots() {
		List<String> vanilla = VANILLA_ORES.subList(0, 3);
		List<String> slots = rows(vanilla, ORES, "minecraft:%s_ore", "jugcraft:%s_ore");
		slots.addAll(rows(vanilla, ORES, "minecraft:deepslate_%s_ore", "jugcraft:deepslate_%s_ore"));
		return slots;
	}

	/** Vanilla iron tools (and our iron paxel), our bronze six, our steel six, then vanilla gold (and our gold paxel). */
	private static List<String> toolSlots() {
		List<String> slots = new ArrayList<>(vanillaTools(0));
		pad(slots);
		for (String tier : TOOL_TIERS) {
			for (String tool : TOOLS) {
				slots.add("jugcraft:" + tier + "_" + tool);
			}
			pad(slots);
		}
		slots.addAll(vanillaTools(1));
		return slots;
	}

	/**
	 * Rows of nine, each starting with all of {@code vanilla} in {@code vanillaPattern} and filled out with the next of
	 * {@code ours} in {@code ourPattern}, until ours run out; the last row is padded.
	 */
	private static List<String> rows(List<String> vanilla, List<String> ours, String vanillaPattern, String ourPattern) {
		List<String> slots = new ArrayList<>();
		int next = 0;
		while (next < ours.size()) {
			for (String name : vanilla) {
				slots.add(vanillaPattern.formatted(name));
			}
			while (slots.size() % 9 != 0 && next < ours.size()) {
				slots.add(ourPattern.formatted(ours.get(next++)));
			}
			pad(slots);
		}
		return slots;
	}

	/** Leaves the rest of the inventory row empty (null), so the next item starts a new row. */
	private static void pad(List<String> slots) {
		while (slots.size() % 9 != 0) {
			slots.add(null);
		}
	}

	/**
	 * Fills the inventory in reading order (the three main rows, then the hotbar; null leaves a slot empty), opens it and
	 * takes a screenshot.
	 */
	private static void inventory(ClientGameTestContext context, TestServerContext server, List<String> slots, String name) {
		if (slots.size() > 36) {
			throw new AssertionError("Material sets client test: " + name + " needs " + slots.size() + " slots, more than 36");
		}
		server.runCommand("clear @p");
		for (int i = 0; i < slots.size(); i++) {
			if (slots.get(i) == null) {
				continue;
			}
			String slot = i < 27 ? "inventory." + i : "hotbar." + (i - 27);
			server.runCommand("item replace entity @p %s with %s".formatted(slot, id(slots.get(i))));
		}
		context.waitTicks(10);
		context.setScreen(() -> new InventoryScreen(Minecraft.getInstance().player));
		context.waitTicks(10);
		context.takeScreenshot(name);
		context.setScreen(() -> null);
		context.waitTicks(5);
	}

	/** The id, after checking it is a registered item: a missing one fails the test rather than leaving a gap. */
	private static String id(String id) {
		if (!BuiltInRegistries.ITEM.containsKey(Identifier.parse(id))) {
			throw new AssertionError("Material sets client test: no item " + id);
		}
		return id;
	}

	private static void setblock(TestServerContext server, int x, int y, int z, String block, List<String> placed) {
		if (!BuiltInRegistries.BLOCK.containsKey(Identifier.parse(block))) {
			throw new AssertionError("Material sets client test: no block " + block);
		}
		server.runCommand("setblock %d %d %d %s".formatted(x, y, z, block));
		placed.add(block);
	}

	/** An item frame at (x, y, z), hung facing south on the wall block north of it, holding {@code item}. */
	private static void frame(TestServerContext server, int x, int y, int z, String item, List<String> placed) {
		server.runCommand("summon minecraft:item_frame %d %d %d {Facing:3b,Fixed:1b,Item:{id:\"%s\",count:1}}".formatted(x, y, z, id(item)));
		placed.add(item);
	}

	/** The z a camera stands at to be {@code distance} blocks south of the face of the wall at {@code wall}. */
	private static double front(int wall, double distance) {
		return wall + 1 + distance;
	}

	/** From (x, y, z), standing on a barrier (or on the floor, which the barrier keeps), looking north at {@code pitch}. */
	private static void shoot(ClientGameTestContext context, TestSingleplayerContext singleplayer, double x, int y, double z,
			int pitch, String name) {
		TestServerContext server = singleplayer.getServer();
		server.runCommand("setblock %d %d %d minecraft:barrier keep".formatted((int) Math.floor(x), y - 1, (int) Math.floor(z)));
		server.runCommand(String.format(Locale.ROOT, "tp @p %.2f %d %.2f 180 %d", x, y, z, pitch));
		context.waitTicks(40);
		singleplayer.getConnection().waitForChunksRender();
		context.takeScreenshot(name);
	}
}
