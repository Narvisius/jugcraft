package io.github.jimbozoomer.jugcraft.test;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;

/**
 * Client game test for Steampunk and Kaiser Armor (docs/features/steampunk-and-kaiser-armor.md): six armor stands in a
 * row, left to right from the front, in vanilla copper, bronze, Steampunk, vanilla iron, steel and Kaiser armor, so
 * each Jugcraft set stands beside the vanilla armor of its colour; shot from the front and from behind (Steampunk's
 * boiler) by day with the HUD hidden. Then all 24 pieces and the two patterns in frames on a wall, in the stands'
 * order (their icons). Before any shot the server checks that each stand wears its four pieces and that its
 * chestplate is drawn from its set's equipment asset; a wrong stand fails the test. CI job {@code client}.
 * <p>Bronze and steel armor wear the knight armor's 3D models (KnightArmorClientGameTests,
 * docs/features/knight-armor.md), so in these shots they stand beside Steampunk and Kaiser in their own look.
 */
public class ArmorSetsClientGameTests implements FabricClientGameTest {
	/** Each set's ID prefix and equipment asset, in the stands' order. */
	private static final String[] SETS = {"minecraft:copper", "jugcraft:bronze", "jugcraft:steampunk", "minecraft:iron", "jugcraft:steel",
			"jugcraft:kaiser"};
	private static final String[] PIECES = {"helmet", "chestplate", "leggings", "boots"};
	/** The armor stand's equipment keys and slots for each piece. */
	private static final String[] KEYS = {"head", "chest", "legs", "feet"};
	private static final EquipmentSlot[] SLOTS = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
	private static final String[] PATTERNS = {"jugcraft:steampunk_pattern", "jugcraft:kaiser_pattern"};
	/** The row of stands, two blocks apart, and how far the camera stands from it. */
	private static final double ROW_Z = -6.5;
	private static final int CAMERA_DISTANCE = 6;

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
			server.runCommand("time set 6000");
			server.runCommand("weather clear");
			server.runCommand("gamerule minecraft:send_command_feedback false");
			server.runCommand("gamerule minecraft:advance_time false");
			server.runCommand("fill %d %d %d %d %d %d minecraft:grass_block".formatted(x - 8, y - 3, z - 18, x + 14, y - 1, z + 6));
			server.runCommand("fill %d %d %d %d %d %d minecraft:air".formatted(x - 8, y, z - 18, x + 14, y + 10, z + 6));
			context.waitTicks(10);
			// Hide the HUD, hand and chat whatever state an earlier test left them in, and put them back at the end.
			boolean hudWasHidden = context.computeOnClient(client -> client.gui.hud.isHidden());
			setHudHidden(context, true);

			// The six stands, facing south (towards the front camera), arms shown so the pauldrons do too.
			for (int i = 0; i < SETS.length; i++) {
				StringBuilder equipment = new StringBuilder();
				for (int p = 0; p < PIECES.length; p++) {
					equipment.append(p == 0 ? "" : ",").append("%s:{id:\"%s_%s\",count:1}".formatted(KEYS[p], SETS[i], PIECES[p]));
				}
				server.runCommand(String.format(Locale.ROOT, "summon minecraft:armor_stand %.1f %d %.1f {ShowArms:1b,NoBasePlate:1b,"
						+ "Rotation:[0f,0f],equipment:{%s}}", standX(x, i), y, z + ROW_Z, equipment));
			}
			context.waitTicks(20);
			String wrong = server.computeOnServer(minecraft -> checkStands(minecraft.overworld(),
					new AABB(x - 6, y - 1, z + ROW_Z - 2, x + 12, y + 3, z + ROW_Z + 2)));
			check(wrong.isEmpty(), "The armor stands are not dressed as the shots need: " + wrong);

			// From the front (south, looking north), then from behind (north, looking south).
			shoot(context, singleplayer, standX(x, 2) + 1.0, y, z + ROW_Z + CAMERA_DISTANCE, 180, 10, "jugcraft_armor_sets_front");
			shoot(context, singleplayer, standX(x, 2) + 1.0, y, z + ROW_Z - CAMERA_DISTANCE, 0, 10, "jugcraft_armor_sets_back");
			server.runCommand("kill @e[type=minecraft:armor_stand]");

			// The icons: one column per set in the stands' order, helmet at the top, then a column with the two patterns.
			server.runCommand("fill %d %d %d %d %d %d minecraft:spruce_planks".formatted(x - 2, y, z - 11, x + 7, y + 5, z - 11));
			for (int i = 0; i < SETS.length; i++) {
				for (int p = 0; p < PIECES.length; p++) {
					frame(server, x - 1 + i, y + 4 - p, z - 10, SETS[i] + "_" + PIECES[p]);
				}
			}
			for (int i = 0; i < PATTERNS.length; i++) {
				frame(server, x + 6, y + 3 - i, z - 10, PATTERNS[i]);
			}
			context.waitTicks(20);
			server.runCommand("setblock %d %d %d minecraft:barrier".formatted(x + 2, y + 1, z - 6));
			shoot(context, singleplayer, x + 2.5, y + 2, z - 5.5, 180, 8, "jugcraft_armor_sets_icons");
			server.runCommand("setblock %d %d %d minecraft:air".formatted(x + 2, y + 1, z - 6));
			setHudHidden(context, hudWasHidden);
		}
	}

	/** The x of stand {@code i}: two blocks apart, centred on a block. */
	private static double standX(int x, int i) {
		return x - 2.5 + 2 * i;
	}

	/**
	 * What is wrong with the stands in {@code around}, or an empty string: there must be one per set, and from west to
	 * east each must wear its set's four pieces with a chestplate drawn from that set's equipment asset.
	 */
	private static String checkStands(ServerLevel level, AABB around) {
		List<ArmorStand> stands = new ArrayList<>(level.getEntitiesOfClass(ArmorStand.class, around));
		if (stands.size() != SETS.length) {
			return stands.size() + " armor stands, not " + SETS.length;
		}
		stands.sort(Comparator.comparingDouble(ArmorStand::getX));
		List<String> wrong = new ArrayList<>();
		for (int i = 0; i < SETS.length; i++) {
			ArmorStand stand = stands.get(i);
			for (int p = 0; p < PIECES.length; p++) {
				String worn = BuiltInRegistries.ITEM.getKey(stand.getItemBySlot(SLOTS[p]).getItem()).toString();
				if (!worn.equals(SETS[i] + "_" + PIECES[p])) {
					wrong.add("stand " + (i + 1) + " wears " + worn + " on its " + KEYS[p] + ", not " + SETS[i] + "_" + PIECES[p]);
				}
			}
			ItemStack chest = stand.getItemBySlot(EquipmentSlot.CHEST);
			var equippable = chest.get(DataComponents.EQUIPPABLE);
			String asset = equippable == null ? "nothing" : equippable.assetId().map(key -> key.identifier().toString()).orElse("nothing");
			if (!asset.equals(SETS[i])) {
				wrong.add("stand " + (i + 1) + "'s chestplate is drawn from " + asset + ", not " + SETS[i]);
			}
		}
		return String.join("; ", wrong);
	}

	private static void frame(TestServerContext server, int x, int y, int z, String item) {
		server.runCommand("summon minecraft:item_frame %d %d %d {Facing:3b,Fixed:1b,Item:{id:\"%s\",count:1}}".formatted(x, y, z, item));
	}

	/** Stands the camera at (x, y, z) looking along yaw and pitch, waits for the world to draw, and takes the shot. */
	private static void shoot(ClientGameTestContext context, TestSingleplayerContext singleplayer, double x, int y, double z, int yaw,
			int pitch, String name) {
		singleplayer.getServer().runCommand(String.format(Locale.ROOT, "tp @p %.1f %d %.1f %d %d", x, y, z, yaw, pitch));
		context.waitTicks(40);
		singleplayer.getConnection().waitForChunksRender();
		context.takeScreenshot(name);
	}

	/** Hides or shows the HUD, hand and chat (F1) whatever state an earlier test left it in, as DroneClientGameTests does. */
	private static void setHudHidden(ClientGameTestContext context, boolean hidden) {
		context.runOnClient(client -> {
			if (client.gui.hud.isHidden() != hidden) {
				client.gui.hud.toggle();
			}
		});
	}

	private static void check(boolean ok, String message) {
		if (!ok) {
			throw new AssertionError(message);
		}
	}
}
