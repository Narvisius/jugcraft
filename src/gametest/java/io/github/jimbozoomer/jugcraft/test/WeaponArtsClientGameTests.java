package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.client.arms.ArmsMotion;
import io.github.jimbozoomer.jugcraft.weapons.JugcraftArms;
import io.github.jimbozoomer.jugcraft.weapons.WeaponArts;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.CameraType;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.AABB;

/**
 * Client game test for the weapon arts (Arms V, batch 48), end to end: the use key, pressed with each Arms V arm, goes
 * to the server, which works the art (weapons/WeaponArts) on real foes with real physics, while this client plays the
 * art's animation (client/arms/ArmsMotion, told by WeaponArtPayload). Each art is caught from behind in its
 * stride, and its result read back from the server: the cyclone's three turns on four husks about the player, the iaido
 * dash carrying the player and its cut on the husks passed, the leap rising and slamming with no fall damage on level
 * ground, the flurry's jabs, the crescent through two husks in line, and the chain hauling a pig in to be reaped. Two
 * arts are seen in first person too, with the hotbar's cooldown (CI job {@code client}).
 */
public class WeaponArtsClientGameTests implements FabricClientGameTest {
	private static final float HEALTH = 100.0F;

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
			server.runCommand("fill %d %d %d %d %d %d minecraft:smooth_stone".formatted(x - 12, y - 1, z - 18, x + 12, y - 1, z + 8));
			server.runCommand("fill %d %d %d %d %d %d minecraft:air".formatted(x - 12, y, z - 18, x + 12, y + 8, z + 8));
			server.runCommand("gamemode survival @p");
			// From behind: the foes ahead stay in view past the player (a camera in front would sit among them).
			context.runOnClient(client -> client.options.setCameraType(CameraType.THIRD_PERSON_BACK));
			context.waitTicks(20);

			// Cyclone: four husks about the player, two blocks off; three turns strike each of them three times.
			ready(context, server, x, y, z, "steel_twinblade");
			List<double[]> about = List.of(new double[] {0, -2}, new double[] {0, 2}, new double[] {2, 0}, new double[] {-2, 0});
			for (double[] at : about) {
				husk(server, x + 0.5 + at[0], y, z + 0.5 + at[1]);
			}
			context.waitTicks(10);
			float base = attack(server);
			use(context);
			context.waitTicks(9);
			int phase = context.computeOnClient(client -> ArmsMotion.artPhase(client.player));
			context.takeScreenshot("jugcraft_art_cyclone");
			context.waitTicks(25);
			List<Float> cyclone = healths(server, "husk", x, y, z);
			Jugcraft.LOGGER.info("[weapon arts] cyclone of {} a hit: husks {}; the client played phase {}", base * JugcraftArms.CYCLONE_SHARE,
					cyclone, phase);
			float turns = base * JugcraftArms.CYCLONE_SHARE * JugcraftArms.CYCLONE_HITS;
			check(cyclone.size() == 4 && cyclone.stream().allMatch(health -> near(HEALTH - health, turns)) && phase == 0,
					"The cyclone took " + cyclone + " from 100, not " + turns + " each; client phase " + phase);

			// Iaido: two husks in line ahead and one aside; the dash carries the player past them and the cut lands after.
			ready(context, server, x, y, z + 6, "steel_nodachi");
			husk(server, x + 0.5, y, z + 6 - 2.5);
			husk(server, x + 1.0, y, z + 6 - 4.5);
			husk(server, x + 5.5, y, z + 6 - 3.5);
			context.waitTicks(10);
			base = attack(server);
			double startZ = context.computeOnClient(client -> client.player.getZ());
			float startHealth = context.computeOnClient(client -> client.player.getHealth());
			use(context);
			context.waitTicks(6);
			context.takeScreenshot("jugcraft_art_iaido_dash");
			context.waitTicks(8);
			context.takeScreenshot("jugcraft_art_iaido_cut");
			context.waitTicks(15);
			double travelled = startZ - context.computeOnClient(client -> client.player.getZ());
			List<Float> iaido = healths(server, "husk", x, y, z + 6);
			float endHealth = context.computeOnClient(client -> client.player.getHealth());
			float cutBlow = base * JugcraftArms.IAIDO_SHARE;
			Jugcraft.LOGGER.info("[weapon arts] iaido: travelled {} blocks, husks {} (cut {}), the player's health {} -> {}", travelled, iaido,
					cutBlow, startHealth, endHealth);
			long cut = iaido.stream().filter(health -> near(HEALTH - health, cutBlow)).count();
			long spared = iaido.stream().filter(health -> health == HEALTH).count();
			check(travelled > 3.0 && cut == 2 && spared == 1 && endHealth >= startHealth,
					"Iaido travelled " + travelled + " and left " + iaido + "; health " + startHealth + " -> " + endHealth);

			// Leap slam: husks where the leap comes down; the player rises, lands and takes no fall damage.
			ready(context, server, x, y, z + 6, "steel_earthbreaker");
			for (double[] at : List.of(new double[] {-1.5, -4}, new double[] {1.5, -4}, new double[] {0, -6.5})) {
				husk(server, x + 0.5 + at[0], y, z + 6 + 0.5 + at[1]);
			}
			context.waitTicks(10);
			startHealth = context.computeOnClient(client -> client.player.getHealth());
			double startY = context.computeOnClient(client -> client.player.getY());
			use(context);
			double peak = startY;
			boolean landed = false;
			for (int tick = 1; tick <= 50 && !landed; tick++) {
				context.waitTicks(1);
				peak = Math.max(peak, context.computeOnClient(client -> client.player.getY()));
				if (tick == 6) {
					context.takeScreenshot("jugcraft_art_leap_air");
				}
				landed = tick > JugcraftArms.LEAP_MIN_AIR && !server.computeOnServer(minecraft -> WeaponArts.active(player(minecraft)));
			}
			context.waitTicks(2);
			context.takeScreenshot("jugcraft_art_leap_slam");
			context.waitTicks(10);
			List<Float> leap = healths(server, "husk", x, y, z + 6);
			endHealth = context.computeOnClient(client -> client.player.getHealth());
			Jugcraft.LOGGER.info("[weapon arts] leap slam: rose {} blocks, landed {}, husks {}, the player's health {} -> {}", peak - startY,
					landed, leap, startHealth, endHealth);
			check(peak - startY > 1.5 && landed && leap.stream().anyMatch(health -> health < HEALTH) && endHealth >= startHealth,
					"The leap rose " + (peak - startY) + ", landed " + landed + ", left " + leap + "; health " + startHealth + " -> " + endHealth);

			// Flurry: one husk ahead takes every jab and the finish.
			ready(context, server, x, y, z, "steel_katar");
			husk(server, x + 0.5, y, z + 0.5 - 2.0);
			context.waitTicks(10);
			base = attack(server);
			use(context);
			context.waitTicks(9);
			context.takeScreenshot("jugcraft_art_flurry");
			context.waitTicks(20);
			List<Float> flurry = healths(server, "husk", x, y, z);
			float jabs = base * (JugcraftArms.FLURRY_JABS * JugcraftArms.FLURRY_SHARE + JugcraftArms.FLURRY_FINISH);
			Jugcraft.LOGGER.info("[weapon arts] flurry: husk {} (expected {} off)", flurry, jabs);
			check(flurry.size() == 1 && near(HEALTH - flurry.getFirst(), jabs), "The flurry left " + flurry + ", not " + (HEALTH - jabs));

			// Crescent: two husks in line take the wave (the second for less), one aside does not.
			ready(context, server, x, y, z + 6, "steel_moonblade");
			husk(server, x + 0.5, y, z + 6 + 0.5 - 4.0);
			husk(server, x + 0.5, y, z + 6 + 0.5 - 8.0);
			husk(server, x + 6.5, y, z + 6 + 0.5 - 5.0);
			context.waitTicks(10);
			base = attack(server);
			use(context);
			context.waitTicks(9);
			context.takeScreenshot("jugcraft_art_crescent");
			context.waitTicks(20);
			List<Float> wave = healths(server, "husk", x, y, z + 6);
			float one = base * JugcraftArms.CRESCENT_SHARE;
			float two = one * (1.0F - JugcraftArms.CRESCENT_FADE);
			Jugcraft.LOGGER.info("[weapon arts] crescent: husks {} (expected {} and {} off, one spared)", wave, one, two);
			check(wave.stream().filter(health -> near(HEALTH - health, one)).count() == 1
					&& wave.stream().filter(health -> near(HEALTH - health, two)).count() == 1
					&& wave.stream().filter(health -> health == HEALTH).count() == 1, "The crescent left " + wave);

			// Chain lash: a pig seven blocks ahead is caught, hauled in and reaped (looking a little down, to aim at it). The
			// pig cannot walk (no movement speed), so it stays where the chain is aimed: one left to wander can step half a
			// block off the chain's line before it is thrown (a CI run caught that), and the chain is a ray, not a cone. It
			// keeps its AI, so the haul still moves it (a NoAI mob is not moved by a push).
			ready(context, server, x, y, z + 6, "steel_kusarigama");
			server.runCommand(String.format(Locale.ROOT, "tp @p %.1f %d %.1f 180 12", x + 0.5, y, z + 6 + 0.5));
			server.runCommand(String.format(Locale.ROOT, "summon minecraft:pig %.1f %d %.1f {PersistenceRequired:1b,"
					+ "attributes:[{id:\"minecraft:max_health\",base:100.0d},{id:\"minecraft:movement_speed\",base:0.0d}],"
					+ "Health:100.0f}", x + 0.5, y, z + 6 + 0.5 - 7.0));
			context.waitTicks(10);
			base = attack(server);
			double before = distanceToPig(server);
			use(context);
			context.waitTicks(6);
			context.takeScreenshot("jugcraft_art_chain_lash");
			// Measured as the reap lands, before the reap's own knockback throws the pig back.
			context.waitTicks(JugcraftArms.LASH_REAP - 6);
			double after = distanceToPig(server);
			context.waitTicks(14);
			List<Float> pig = healths(server, "pig", x, y, z + 6);
			float chain = base * (JugcraftArms.LASH_SHARE + JugcraftArms.LASH_REAP_SHARE);
			Jugcraft.LOGGER.info("[weapon arts] chain lash: the pig from {} to {} blocks, health {} (expected {} off)", before, after, pig, chain);
			check(after < before - 2.0 && pig.size() == 1 && near(HEALTH - pig.getFirst(), chain),
					"The chain left the pig " + after + " blocks off (from " + before + ") at " + pig);

			// First person: the flurry again once its cooldown is out, and the cyclone; the hotbar shows the cooldown.
			server.runCommand("kill @e[type=minecraft:pig]");
			context.runOnClient(client -> client.options.setCameraType(CameraType.FIRST_PERSON));
			for (String arm : List.of("steel_katar", "steel_twinblade")) {
				ready(context, server, x, y, z, arm);
				context.waitTicks(25);
				use(context);
				context.waitTicks(9);
				context.takeScreenshot("jugcraft_art_" + arm.substring("steel_".length()) + "_first_person");
				context.waitTicks(30);
			}
		}
	}

	/** Back at (x, y, z) facing north, the husks gone, the arm in hand and charged. */
	private static void ready(ClientGameTestContext context, TestServerContext server, int x, int y, int z, String arm) {
		server.runCommand("kill @e[type=minecraft:husk]");
		server.runCommand(String.format(Locale.ROOT, "tp @p %.1f %d %.1f 180 0", x + 0.5, y, z + 0.5));
		server.runCommand("item replace entity @p weapon.mainhand with jugcraft:" + arm);
		context.waitTicks(20);
	}

	private static void use(ClientGameTestContext context) {
		context.getInput().pressKey(options -> options.keyUse);
	}

	private static void husk(TestServerContext server, double x, int y, double z) {
		server.runCommand(String.format(Locale.ROOT, "summon minecraft:husk %.1f %d %.1f {NoAI:1b,PersistenceRequired:1b,"
				+ "attributes:[{id:\"minecraft:armor\",base:0.0d},{id:\"minecraft:max_health\",base:100.0d}],Health:100.0f}", x, y, z));
	}

	private static ServerPlayer player(MinecraftServer minecraft) {
		return minecraft.getPlayerList().getPlayers().getFirst();
	}

	/** The player's attack damage with the arm in hand, on the server. */
	private static float attack(TestServerContext server) {
		return server.computeOnServer(minecraft -> (float) player(minecraft).getAttributeValue(Attributes.ATTACK_DAMAGE));
	}

	/** The health of every mob of this type about (x, y, z), nearest the north first. */
	private static List<Float> healths(TestServerContext server, String type, int x, int y, int z) {
		return server.computeOnServer(minecraft -> {
			List<LivingEntity> found = new ArrayList<>(minecraft.overworld().getEntitiesOfClass(LivingEntity.class,
					new AABB(x - 12, y - 2, z - 18, x + 12, y + 6, z + 8),
					entity -> BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).getPath().equals(type)));
			found.sort(Comparator.comparingDouble(LivingEntity::getZ));
			return found.stream().map(LivingEntity::getHealth).toList();
		});
	}

	private static double distanceToPig(TestServerContext server) {
		return server.computeOnServer(minecraft -> {
			ServerPlayer player = player(minecraft);
			return minecraft.overworld().getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(16.0),
					entity -> BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).getPath().equals("pig"))
					.stream().mapToDouble(player::distanceTo).min().orElse(-1.0);
		});
	}

	private static boolean near(float a, float b) {
		return Math.abs(a - b) < 0.01F;
	}

	private static void check(boolean ok, String message) {
		if (!ok) {
			throw new AssertionError(message);
		}
	}
}
