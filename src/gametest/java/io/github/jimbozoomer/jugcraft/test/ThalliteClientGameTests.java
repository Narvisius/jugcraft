package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.gear.TraitTooltips;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.BooleanSupplier;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

/**
 * Client game test for thallite, slice 1 (docs/features/thallite.md): its ore and material forms in game, each beside
 * vanilla's iron so the owner can judge them (CI job {@code client}).
 *
 * <p>Scenes, every camera facing north:
 * <ul>
 * <li>a wall of vanilla stone over vanilla deepslate with ores set in both: vanilla's iron, copper and gold, then thallite,
 * then Jugcraft's uranium (the other green ore) and tin, whole and close up on thallite and its neighbours;</li>
 * <li>stone and deepslate let into the floor with thallite ore beside vanilla's iron ore (the top faces);</li>
 * <li>vanilla's raw iron, iron nugget and iron ingot in frames above thallite's, and iron's and thallite's dust, plate and
 * washed ore; beside them against the wall, vanilla's iron block with its raw block on top, then thallite's;</li>
 * <li>the survival inventory, thallite's forms beside vanilla's iron in the slots;</li>
 * <li>the thallite ingot in the hand;</li>
 * <li>slice 2, the gear: thallite's five tools and the Earthbinding Template in frames above its plain and Earthbound
 * armor, then two armor stands, one in each set, from the front and from behind;</li>
 * <li>the Earthbound chestplate's tooltip in the survival inventory, folded and with its traits' details shown.</li>
 * </ul>
 *
 * <p>The ingot's lore line is read back through the client's language ("Green as a new shoot."). Every id is looked up in
 * the registry first, so a missing one fails the test; only the screenshots show how it renders.
 */
public class ThalliteClientGameTests implements FabricClientGameTest {
	/** The ores on the wall, in order: vanilla's three, thallite, then two of Jugcraft's. */
	private static final List<String> ORES = List.of("minecraft:iron", "minecraft:copper", "minecraft:gold", "jugcraft:thallite",
			"jugcraft:uranium", "jugcraft:tin");
	/** Vanilla's iron forms (frames, upper row) above thallite's (lower row). */
	private static final List<String> IRON_ITEMS = List.of("minecraft:raw_iron", "minecraft:iron_nugget", "minecraft:iron_ingot",
			"jugcraft:iron_dust", "jugcraft:iron_plate", "jugcraft:washed_iron_ore");
	private static final List<String> THALLITE_ITEMS = List.of("jugcraft:raw_thallite", "jugcraft:thallite_nugget",
			"jugcraft:thallite_ingot", "jugcraft:thallite_dust", "jugcraft:thallite_plate", "jugcraft:washed_thallite_ore");
	/** Storage blocks against the wall east of the frames, vanilla's iron then thallite's, raw blocks on top. */
	private static final List<String> BLOCKS = List.of("minecraft:iron_block", "jugcraft:thallite_block");
	private static final List<String> RAW_BLOCKS = List.of("minecraft:raw_iron_block", "jugcraft:raw_thallite_block");
	private static final String LORE = "Green as a new shoot.";

	/** Slice 2: the tools and template (frames, top row), then the plain and the Earthbound armor (rows below). */
	private static final List<String> GEAR_TOOLS = List.of("jugcraft:thallite_sword", "jugcraft:thallite_pickaxe",
			"jugcraft:thallite_axe", "jugcraft:thallite_shovel", "jugcraft:thallite_hoe", "jugcraft:earthbinding_template");
	private static final List<String> ARMOR = List.of("helmet", "chestplate", "leggings", "boots");
	private static final List<String> ARMOR_KEYS = List.of("head", "chest", "legs", "feet");
	private static final List<String> LOOKS = List.of("thallite", "earthbound_thallite");

	// The scenes' walls, as offsets north of the spawn point.
	private static final int ORE_WALL = -8;
	private static final int ITEM_WALL = -18;
	private static final int GEAR_WALL = -30;
	/** The armor stands' row, south of the gear wall. */
	private static final int STAND_ROW = -26;

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
			server.runCommand("fill %d %d %d %d %d %d minecraft:smooth_stone".formatted(x - 4, y - 1, z - 34, x + 18, y - 1, z + 8));
			server.runCommand("fill %d %d %d %d %d %d minecraft:air".formatted(x - 4, y, z - 34, x + 18, y + 8, z + 8));
			context.waitTicks(10);

			List<String> placed = new ArrayList<>();
			oreWall(server, x, y, z + ORE_WALL, placed);
			oreFloor(server, x, y, z, placed);
			itemWall(server, x, y, z + ITEM_WALL, placed);
			gearWall(server, x, y, z + GEAR_WALL, placed);
			Jugcraft.LOGGER.info("[thallite client] placed {} blocks and framed items: {}", placed.size(), placed);
			context.waitTicks(20);

			// The ingot's lore line, as the client's language gives it.
			String lore = context.computeOnClient(client -> Component.translatable("tooltip.jugcraft.thallite_ingot").getString());
			Jugcraft.LOGGER.info("[thallite client] the ingot's lore line: {}", lore);
			if (!LORE.equals(lore)) {
				throw new AssertionError("Thallite client test: the ingot's lore line reads \"" + lore + "\", not \"" + LORE + "\"");
			}

			// Hide the HUD, hand and chat for the scenery shots, whatever an earlier test in this client left.
			context.runOnClient(client -> {
				if (!client.gui.hud.isHidden()) {
					client.gui.hud.toggle();
				}
			});

			// 1. The ore wall whole, then close up on thallite between gold and uranium (stone above, deepslate below).
			shoot(context, singleplayer, x + 6.5, y + 1, front(z + ORE_WALL, 8), 0, "jugcraft_thallite_ores_wall");
			shoot(context, singleplayer, x + 7.5, y + 1, front(z + ORE_WALL, 3.25), 0, "jugcraft_thallite_ores_close");
			// 2. The floor patches from above: the ore's top faces in stone and in deepslate.
			shoot(context, singleplayer, x + 6.5, y + 3, z + 2.5, 55, "jugcraft_thallite_ores_floor");
			// 3. Vanilla's iron forms above thallite's in frames, then the storage and raw blocks: the whole wall, the frames
			// close up three at a time, and the blocks.
			shoot(context, singleplayer, x + 5.0, y, front(z + ITEM_WALL, 5), 2, "jugcraft_thallite_items");
			shoot(context, singleplayer, x + 1.5, y, front(z + ITEM_WALL, 1.6), -14, "jugcraft_thallite_items_close_1");
			shoot(context, singleplayer, x + 4.5, y, front(z + ITEM_WALL, 1.6), -14, "jugcraft_thallite_items_close_2");
			shoot(context, singleplayer, x + 9.0, y, front(z + ITEM_WALL, 2.6), 12, "jugcraft_thallite_blocks");
			// 3b. Slice 2: the gear's icons in frames, then the two sets worn, from the front and from behind.
			shoot(context, singleplayer, x + 2.5, y + 1, front(z + GEAR_WALL, 3.4), 8, "jugcraft_thallite_gear_icons");
			stands(server, x, y, z + STAND_ROW);
			context.waitTicks(20);
			shoot(context, singleplayer, x + 9.0, y, z + STAND_ROW + 3.5, 6, "jugcraft_thallite_gear_worn");
			shootBack(context, singleplayer, x + 9.0, y, z + STAND_ROW - 2.5, "jugcraft_thallite_gear_worn_back");

			context.runOnClient(client -> {
				if (client.gui.hud.isHidden()) {
					client.gui.hud.toggle();
				}
			});

			// 4. The survival inventory: vanilla's iron forms in one row, thallite's in the row below.
			server.runCommand(String.format(Locale.ROOT, "tp @p %.1f %d %.1f 180 0", x + 0.5, y, z + 4.5));
			server.runCommand("gamemode survival @p");
			context.waitTicks(10);
			inventory(context, server, "jugcraft_thallite_inventory");
			gearTooltip(context, server);
			server.runCommand("clear @p");
			server.runCommand("gamemode creative @p");

			// 5. In the hand, first person, the hotbar showing.
			context.runOnClient(client -> client.options.setCameraType(CameraType.FIRST_PERSON));
			server.runCommand("item replace entity @p weapon.mainhand with " + id("jugcraft:thallite_ingot"));
			context.waitTicks(30);
			context.takeScreenshot("jugcraft_thallite_held_ingot");
			server.runCommand("item replace entity @p weapon.mainhand with minecraft:air");
		}
	}

	/** Deepslate in the lower two rows and stone in the four above, 13 wide; in every second column an ore in each band. */
	private static void oreWall(TestServerContext server, int x, int y, int wall, List<String> placed) {
		server.runCommand("fill %d %d %d %d %d %d minecraft:deepslate".formatted(x, y, wall, x + 12, y + 1, wall));
		server.runCommand("fill %d %d %d %d %d %d minecraft:stone".formatted(x, y + 2, wall, x + 12, y + 5, wall));
		for (int i = 0; i < ORES.size(); i++) {
			String ore = ORES.get(i);
			String namespace = ore.substring(0, ore.indexOf(':') + 1);
			String name = ore.substring(ore.indexOf(':') + 1);
			int column = x + 1 + 2 * i;
			setblock(server, column, y + 3, wall, namespace + name + "_ore", placed);
			setblock(server, column, y + 1, wall, namespace + "deepslate_" + name + "_ore", placed);
		}
	}

	/** A patch of stone and one of deepslate let into the floor, each with vanilla's iron ore and thallite ore. */
	private static void oreFloor(TestServerContext server, int x, int y, int z, List<String> placed) {
		server.runCommand("fill %d %d %d %d %d %d minecraft:stone".formatted(x + 2, y - 1, z - 3, x + 6, y - 1, z - 1));
		server.runCommand("fill %d %d %d %d %d %d minecraft:deepslate".formatted(x + 8, y - 1, z - 3, x + 12, y - 1, z - 1));
		setblock(server, x + 3, y - 1, z - 2, "minecraft:iron_ore", placed);
		setblock(server, x + 5, y - 1, z - 2, "jugcraft:thallite_ore", placed);
		setblock(server, x + 9, y - 1, z - 2, "minecraft:deepslate_iron_ore", placed);
		setblock(server, x + 11, y - 1, z - 2, "jugcraft:deepslate_thallite_ore", placed);
	}

	/**
	 * Frames on a planks wall: vanilla's raw iron, iron nugget, iron ingot and iron's dust, plate and washed ore above
	 * thallite's six. East of them, against the wall: vanilla's iron block and thallite's, each with its raw block on top.
	 */
	private static void itemWall(TestServerContext server, int x, int y, int wall, List<String> placed) {
		server.runCommand("fill %d %d %d %d %d %d minecraft:spruce_planks".formatted(x - 1, y, wall, x + 11, y + 4, wall));
		for (int i = 0; i < IRON_ITEMS.size(); i++) {
			frame(server, x + i, y + 2, wall + 1, IRON_ITEMS.get(i), placed);
			frame(server, x + i, y + 1, wall + 1, THALLITE_ITEMS.get(i), placed);
		}
		for (int i = 0; i < BLOCKS.size(); i++) {
			setblock(server, x + 8 + 2 * i, y, wall + 1, BLOCKS.get(i), placed);
			setblock(server, x + 8 + 2 * i, y + 1, wall + 1, RAW_BLOCKS.get(i), placed);
		}
	}

	/**
	 * Slice 2's frames on a planks wall: the five tools and the Earthbinding Template in the top row, then the plain
	 * armor (helmet to boots), then the Earthbound armor below it.
	 */
	private static void gearWall(TestServerContext server, int x, int y, int wall, List<String> placed) {
		server.runCommand("fill %d %d %d %d %d %d minecraft:spruce_planks".formatted(x - 1, y, wall, x + 12, y + 4, wall));
		for (int i = 0; i < GEAR_TOOLS.size(); i++) {
			frame(server, x + i, y + 3, wall + 1, GEAR_TOOLS.get(i), placed);
		}
		for (int look = 0; look < LOOKS.size(); look++) {
			for (int p = 0; p < ARMOR.size(); p++) {
				frame(server, x + p, y + 2 - look, wall + 1, "jugcraft:" + LOOKS.get(look) + "_" + ARMOR.get(p), placed);
			}
		}
	}

	/** Two armor stands facing south, arms shown, the plain set on the west one and the Earthbound set on the east one. */
	private static void stands(TestServerContext server, int x, int y, int row) {
		for (int look = 0; look < LOOKS.size(); look++) {
			StringBuilder equipment = new StringBuilder();
			for (int p = 0; p < ARMOR.size(); p++) {
				equipment.append(p == 0 ? "" : ",").append("%s:{id:\"%s\",count:1}".formatted(ARMOR_KEYS.get(p),
						id("jugcraft:" + LOOKS.get(look) + "_" + ARMOR.get(p))));
			}
			server.runCommand(String.format(Locale.ROOT, "summon minecraft:armor_stand %.1f %d %.1f {ShowArms:1b,NoBasePlate:1b,"
					+ "Rotation:[0f,0f],equipment:{%s}}", x + 8.0 + 2 * look, y, row + 0.5, equipment));
		}
	}

	/**
	 * The Earthbound chestplate's tooltip in the survival inventory, the real cursor over it in the first hotbar slot:
	 * folded (its traits' names and "Hold Shift"), then with the details shown (TraitTooltips.details set on, as
	 * TraitDetailsClientGameTests does, rather than holding the key).
	 */
	private static void gearTooltip(ClientGameTestContext context, TestServerContext server) {
		server.runCommand("item replace entity @p hotbar.0 with " + id("jugcraft:earthbound_thallite_chestplate"));
		context.waitTicks(10);
		context.setScreen(() -> new InventoryScreen(Minecraft.getInstance().player));
		context.waitTicks(5);
		double[] at = context.computeOnClient(client -> {
			Screen screen = client.gui.screen();
			// The survival inventory is 176 by 166 GUI units, centred; its first hotbar slot's corner is at (8, 142).
			double left = (screen.width - 176) / 2;
			double top = (screen.height - 166) / 2;
			double guiPerPixelX = MouseHandler.getScaledXPos(client.getWindow(), 1.0);
			double guiPerPixelY = MouseHandler.getScaledYPos(client.getWindow(), 1.0);
			return new double[] {(left + 8 + 8) / guiPerPixelX, (top + 142 + 8) / guiPerPixelY};
		});
		context.getInput().setCursorPos(at[0], at[1]);
		context.waitTicks(5);
		context.takeScreenshot("jugcraft_thallite_gear_tooltip");
		BooleanSupplier shift = context.computeOnClient(client -> TraitTooltips.details);
		try {
			context.runOnClient(client -> TraitTooltips.details = () -> true);
			context.waitTicks(3);
			context.takeScreenshot("jugcraft_thallite_gear_tooltip_details");
		} finally {
			context.runOnClient(client -> TraitTooltips.details = shift);
		}
		context.setScreen(() -> null);
		context.waitTicks(5);
	}

	/** Fills the inventory (iron's row, then thallite's), opens it and takes a screenshot. */
	private static void inventory(ClientGameTestContext context, TestServerContext server, String name) {
		List<String> slots = new ArrayList<>();
		for (List<String> row : List.of(ironRow(), thalliteRow())) {
			slots.addAll(row);
			while (slots.size() % 9 != 0) {
				slots.add(null);
			}
		}
		server.runCommand("clear @p");
		for (int i = 0; i < slots.size(); i++) {
			if (slots.get(i) != null) {
				server.runCommand("item replace entity @p inventory.%d with %s".formatted(i, id(slots.get(i))));
			}
		}
		server.runCommand("item replace entity @p hotbar.0 with " + id("jugcraft:thallite_ingot"));
		context.waitTicks(10);
		context.setScreen(() -> new InventoryScreen(Minecraft.getInstance().player));
		context.waitTicks(10);
		context.takeScreenshot(name);
		context.setScreen(() -> null);
		context.waitTicks(5);
	}

	/** Vanilla's iron ore, deepslate iron ore, raw iron, raw iron block, nugget, ingot and block, and iron's dust and plate. */
	private static List<String> ironRow() {
		return List.of("minecraft:iron_ore", "minecraft:deepslate_iron_ore", "minecraft:raw_iron", "minecraft:raw_iron_block",
				"minecraft:iron_nugget", "minecraft:iron_ingot", "minecraft:iron_block", "jugcraft:iron_dust", "jugcraft:iron_plate");
	}

	/** Thallite's forms in the same order. */
	private static List<String> thalliteRow() {
		return List.of("jugcraft:thallite_ore", "jugcraft:deepslate_thallite_ore", "jugcraft:raw_thallite",
				"jugcraft:raw_thallite_block", "jugcraft:thallite_nugget", "jugcraft:thallite_ingot", "jugcraft:thallite_block",
				"jugcraft:thallite_dust", "jugcraft:thallite_plate");
	}

	/** The id, after checking it is a registered item: a missing one fails the test rather than leaving a gap. */
	private static String id(String id) {
		if (!BuiltInRegistries.ITEM.containsKey(Identifier.parse(id))) {
			throw new AssertionError("Thallite client test: no item " + id);
		}
		return id;
	}

	private static void setblock(TestServerContext server, int x, int y, int z, String block, List<String> placed) {
		if (!BuiltInRegistries.BLOCK.containsKey(Identifier.parse(block))) {
			throw new AssertionError("Thallite client test: no block " + block);
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

	/** From (x, y, z), standing on a barrier (or on the floor, which the barrier keeps), looking south, a little down. */
	private static void shootBack(ClientGameTestContext context, TestSingleplayerContext singleplayer, double x, int y, double z,
			String name) {
		TestServerContext server = singleplayer.getServer();
		server.runCommand("setblock %d %d %d minecraft:barrier keep".formatted((int) Math.floor(x), y - 1, (int) Math.floor(z)));
		server.runCommand(String.format(Locale.ROOT, "tp @p %.2f %d %.2f 0 6", x, y, z));
		context.waitTicks(40);
		singleplayer.getConnection().waitForChunksRender();
		context.takeScreenshot(name);
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
