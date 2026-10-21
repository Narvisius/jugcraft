package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import java.util.Locale;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;

/**
 * Client game test for titanium gear (docs/features/titanium-gear.md): a steel and a titanium knight on armor stands by
 * day, from the front and three-quarter, then the ten titanium icons in frames on a wall (tools above, armor below).
 * The server checks the titanium stand wears its four pieces before the shots. CI job {@code client}.
 */
public class TitaniumGearClientGameTests implements FabricClientGameTest {
	private static final String[] PIECES = {"helmet", "chestplate", "leggings", "boots"};
	private static final String[] KEYS = {"head", "chest", "legs", "feet"};
	private static final EquipmentSlot[] SLOTS = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
	private static final String[] TOOLS = {"sword", "pickaxe", "axe", "shovel", "hoe", "paxel"};

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
			server.runCommand("gamerule minecraft:advance_time false");
			server.runCommand("gamerule minecraft:spawn_mobs false");
			server.runCommand("fill %d %d %d %d %d %d minecraft:grass_block".formatted(x - 6, y - 1, z - 14, x + 8, y - 1, z + 6));
			server.runCommand("fill %d %d %d %d %d %d minecraft:air".formatted(x - 6, y, z - 14, x + 8, y + 6, z + 6));
			context.waitTicks(10);
			boolean hudWasHidden = context.computeOnClient(client -> client.gui.hud.isHidden());
			setHudHidden(context, true);

			// Steel on the left, titanium on the right, facing south (towards the camera).
			double rowZ = z - 4.5;
			summon(server, x - 0.5, y, rowZ, "steel");
			summon(server, x + 1.5, y, rowZ, "titanium");
			context.waitTicks(15);
			String worn = server.computeOnServer(minecraft -> {
				StringBuilder wrong = new StringBuilder();
				for (LivingEntity stand : minecraft.overworld().getEntitiesOfClass(LivingEntity.class,
						new AABB(x + 1, y - 1, rowZ - 1, x + 2, y + 3, rowZ + 1), entity -> type(entity).equals("armor_stand"))) {
					for (int p = 0; p < PIECES.length; p++) {
						String id = BuiltInRegistries.ITEM.getKey(stand.getItemBySlot(SLOTS[p]).getItem()).getPath();
						if (!id.equals("titanium_" + PIECES[p])) {
							wrong.append(id).append(" on the ").append(KEYS[p]).append("; ");
						}
					}
					return wrong.toString();
				}
				return "no titanium stand";
			});
			Jugcraft.LOGGER.info("[titanium gear client] the titanium stand: {}", worn.isEmpty() ? "dressed" : worn);
			check(worn.isEmpty(), "The titanium stand is not dressed: " + worn);
			shoot(context, singleplayer, x + 1.0, y, rowZ + 3.2, 10, "jugcraft_titanium_armor_front");
			server.runCommand("execute as @e[type=minecraft:armor_stand] at @s run tp @s ~ ~ ~ -45 0");
			shoot(context, singleplayer, x + 1.0, y, rowZ + 3.2, 10, "jugcraft_titanium_armor_three_quarter");
			server.runCommand("kill @e[type=minecraft:armor_stand]");

			// Ten icons in frames: five tools and the paxel above, four armor pieces below.
			server.runCommand("fill %d %d %d %d %d %d minecraft:spruce_planks".formatted(x - 1, y, z - 12, x + 5, y + 3, z - 12));
			for (int t = 0; t < TOOLS.length; t++) {
				frame(server, x + t, y + 2, z - 11, "titanium_" + TOOLS[t]);
			}
			for (int p = 0; p < PIECES.length; p++) {
				frame(server, x + p, y + 1, z - 11, "titanium_" + PIECES[p]);
			}
			context.waitTicks(20);
			int frames = server.computeOnServer(minecraft -> minecraft.overworld().getEntitiesOfClass(Entity.class,
					new AABB(x - 1, y, z - 12, x + 6, y + 4, z - 10), entity -> type(entity).equals("item_frame")).size());
			check(frames == TOOLS.length + PIECES.length, frames + " item frames on the wall");
			// Center the six-tool row and step back so the new rightmost paxel is fully visible.
			shoot(context, singleplayer, x + 3.0, y, z - 6.5, -9, "jugcraft_titanium_gear_icons");
			setHudHidden(context, hudWasHidden);
		}
	}

	/** An armor stand with arms at (x, y, z), facing south, in {@code metal}'s four pieces. */
	private static void summon(TestServerContext server, double x, int y, double z, String metal) {
		StringBuilder equipment = new StringBuilder();
		for (int p = 0; p < PIECES.length; p++) {
			equipment.append(p == 0 ? "" : ",").append("%s:{id:\"jugcraft:%s_%s\",count:1}".formatted(KEYS[p], metal, PIECES[p]));
		}
		server.runCommand(String.format(Locale.ROOT,
				"summon minecraft:armor_stand %.1f %d %.1f {ShowArms:1b,NoBasePlate:1b,Rotation:[0f,0f],equipment:{%s}}", x, y, z, equipment));
	}

	private static String type(Entity entity) {
		return BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).getPath();
	}

	private static void frame(TestServerContext server, int x, int y, int z, String item) {
		server.runCommand("summon minecraft:item_frame %d %d %d {Facing:3b,Fixed:1b,Item:{id:\"jugcraft:%s\",count:1}}".formatted(x, y, z, item));
	}

	/** Stands the camera at (x, y, z) looking north and {@code pitch} down, waits for the world to draw, and takes the shot. */
	private static void shoot(ClientGameTestContext context, TestSingleplayerContext singleplayer, double x, int y, double z, int pitch,
			String name) {
		singleplayer.getServer().runCommand(String.format(Locale.ROOT, "tp @p %.2f %d %.2f 180 %d", x, y, z, pitch));
		context.waitTicks(40);
		singleplayer.getConnection().waitForChunksRender();
		context.takeScreenshot(name);
	}

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
