package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.weapons.ArmVariants;
import java.util.List;
import java.util.Locale;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.CameraType;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.core.BlockPos;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.pig.Pig;
import net.minecraft.world.phys.AABB;

/**
 * Client game test for Arms VII (batch 56): every variant and pattern in frames on a wall (their icons) and the
 * variants on racks of armor stands (their 3D models), by daylight; trophies held from the front by day and the glowing
 * ones at midnight; one in first person; and a Glacier Maul's two-handed blow with the real attack key, its frost read
 * back from the server (CI job {@code client}).
 */
public class ArmsVIIClientGameTests implements FabricClientGameTest {
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
			server.runCommand("fill %d %d %d %d %d %d minecraft:smooth_stone".formatted(x - 12, y - 1, z - 18, x + 16, y - 1, z + 8));
			server.runCommand("fill %d %d %d %d %d %d minecraft:air".formatted(x - 12, y, z - 18, x + 16, y + 8, z + 8));
			context.waitTicks(10);

			// Every variant (eight a row) and the four patterns in frames on a wall.
			List<String> variants = List.copyOf(ArmVariants.ITEMS.keySet());
			server.runCommand("fill %d %d %d %d %d %d minecraft:spruce_planks".formatted(x - 1, y, z - 12, x + 10, y + 6, z - 12));
			for (int i = 0; i < variants.size(); i++) {
				frame(server, x + 1 + i % 8, y + 5 - i / 8, z - 11, variants.get(i));
			}
			for (int i = 0; i < ArmVariants.PATTERN_NAMES.size(); i++) {
				frame(server, x + 3 + i, y + 1, z - 11, ArmVariants.PATTERN_NAMES.get(i));
			}
			context.waitTicks(20);
			shoot(context, singleplayer, x + 4, y + 2, z - 6, 180, 8, "jugcraft_arms_vii_frames");
			server.runCommand("kill @e[type=minecraft:item_frame]");

			// The variants on racks of armor stands, sixteen at a time.
			for (int half = 0; half < 2; half++) {
				for (int i = 0; i < 16; i++) {
					int at = half * 16 + i;
					stand(server, x - 4.5 + (i % 8) * 1.6, y, z - 8.5 + (i / 8) * 2.5, variants.get(at));
				}
				context.waitTicks(20);
				shoot(context, singleplayer, x + 1, y + 2, z - 1, 180, 18, "jugcraft_arms_vii_rack_" + (half + 1));
				server.runCommand("kill @e[type=minecraft:armor_stand]");
			}

			// Trophies held, from the front, by day; then the glowing ones at midnight.
			context.runOnClient(client -> client.options.setCameraType(CameraType.THIRD_PERSON_FRONT));
			for (String trophy : List.of("glacier_maul", "cinderbrand", "hagthorn", "soulreaver", "dynamo_halberd", "tidebreaker")) {
				ready(context, server, x + 4, y, z, trophy);
				context.takeScreenshot("jugcraft_arms_vii_held_" + trophy);
			}
			server.runCommand("time set midnight");
			for (String glowing : List.of("runebound_nodachi", "magmaw", "bogfang")) {
				ready(context, server, x + 4, y, z, glowing);
				context.takeScreenshot("jugcraft_arms_vii_night_" + glowing);
			}
			context.runOnClient(client -> client.options.setCameraType(CameraType.FIRST_PERSON));
			ready(context, server, x + 4, y, z, "runebound_moonblade");
			// The GUI is hidden by this point in CI (an earlier test in the shard leaves it so), and hiding it also hides the
			// hand: one press of its toggle shows it for this shot, and a second puts it back as it was (26.3's Options has no
			// hideGui field to set), as ArmsMotionClientGameTests does.
			context.getInput().pressKey(options -> options.keyToggleGui);
			context.waitTicks(5);
			context.takeScreenshot("jugcraft_arms_vii_night_first_person");
			context.getInput().pressKey(options -> options.keyToggleGui);
			server.runCommand("time set noon");

			// A Glacier Maul's blow with the attack key on a still pig ahead: it is chilled (Slowness, Frost's length).
			server.runCommand("gamemode survival @p");
			server.runCommand(String.format(Locale.ROOT, "summon minecraft:pig %.1f %d %.1f {NoAI:1b,PersistenceRequired:1b}", x + 4.5, y, z - 1.5));
			ready(context, server, x + 4, y, z, "glacier_maul");
			server.runCommand(String.format(Locale.ROOT, "tp @p %.1f %d %.1f 180 35", x + 4.5, y, z + 0.5));
			context.waitTicks(30);
			context.getInput().pressKey(options -> options.keyAttack);
			// A maul is two-handed: its blow lands as the swing comes round, some ticks after the click.
			context.waitTicks(8);
			context.takeScreenshot("jugcraft_arms_vii_frost_blow");
			context.waitTicks(17);
			int[] slowed = server.computeOnServer(minecraft -> {
				List<Pig> pigs = minecraft.overworld().getEntitiesOfClass(Pig.class, new AABB(x - 4, y - 2, z - 8, x + 12, y + 4, z + 6));
				if (pigs.isEmpty()) {
					return new int[] {-1, -1};
				}
				LivingEntity pig = pigs.getFirst();
				MobEffectInstance slow = pig.getEffect(MobEffects.SLOWNESS);
				return slow == null ? new int[] {0, (int) pig.getHealth()} : new int[] {slow.getDuration(), slow.getAmplifier() + 1};
			});
			Jugcraft.LOGGER.info("[arms vii client] glacier maul's blow: the pig's slowness {} ticks, level {}", slowed[0], slowed[1]);
			check(slowed[0] > 0 && slowed[0] <= ArmVariants.FROST_TICKS && slowed[1] == ArmVariants.FROST_AMPLIFIER + 1,
					"The Glacier Maul's blow left the pig slowed " + slowed[0] + " ticks at level " + slowed[1]);
		}
	}

	private static void ready(ClientGameTestContext context, TestServerContext server, int x, int y, int z, String arm) {
		server.runCommand(String.format(Locale.ROOT, "tp @p %.1f %d %.1f 180 0", x + 0.5, y, z + 0.5));
		server.runCommand("item replace entity @p weapon.mainhand with jugcraft:" + arm);
		context.waitTicks(20);
	}

	private static void stand(TestServerContext server, double x, int y, double z, String item) {
		server.runCommand(String.format(Locale.ROOT, "summon minecraft:armor_stand %.1f %d %.1f {ShowArms:1b,NoBasePlate:1b,Rotation:[0f,0f],"
				+ "equipment:{mainhand:{id:\"jugcraft:%s\",count:1}}}", x, y, z, item));
	}

	private static void frame(TestServerContext server, int x, int y, int z, String item) {
		server.runCommand("summon minecraft:item_frame %d %d %d {Facing:3b,Fixed:1b,Item:{id:\"jugcraft:%s\",count:1}}".formatted(x, y, z, item));
	}

	private static void shoot(ClientGameTestContext context, TestSingleplayerContext singleplayer, int x, int y, int z, int yaw, int pitch,
			String name) {
		TestServerContext server = singleplayer.getServer();
		server.runCommand("setblock %d %d %d minecraft:barrier".formatted(x, y - 1, z));
		server.runCommand(String.format(Locale.ROOT, "tp @p %.1f %d %.1f %d %d", x + 0.5, y, z + 0.5, yaw, pitch));
		context.waitTicks(40);
		singleplayer.getConnection().waitForChunksRender();
		context.takeScreenshot(name);
		server.runCommand("setblock %d %d %d minecraft:air".formatted(x, y - 1, z));
	}

	private static void check(boolean ok, String message) {
		if (!ok) {
			throw new AssertionError(message);
		}
	}
}
