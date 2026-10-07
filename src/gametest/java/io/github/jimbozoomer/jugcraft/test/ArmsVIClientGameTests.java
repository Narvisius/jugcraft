package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.client.arms.ArmsMotion;
import io.github.jimbozoomer.jugcraft.weapons.JugcraftArms;
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
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;

/**
 * Client game test for Arms VI (batch 55), with the real use key: the kit on a rack and in frames; a longbow drawn
 * (first person and from the front) and loosed; an arbalest wound, shown loaded and fired; a tower shield and a heater
 * shield raised (first person and from the front); the katana's seven cuts on husks, caught mid-art from behind with
 * their arcs in the air and read back from the server; and the brazier mace's flame, and fire it lights on the ground
 * (CI job {@code client}).
 */
public class ArmsVIClientGameTests implements FabricClientGameTest {
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
			server.runCommand("fill %d %d %d %d %d %d minecraft:smooth_stone".formatted(x - 12, y - 1, z - 18, x + 16, y - 1, z + 8));
			server.runCommand("fill %d %d %d %d %d %d minecraft:air".formatted(x - 12, y, z - 18, x + 16, y + 8, z + 8));
			context.waitTicks(10);

			// The kit on a rack of armor stands facing south, and the batch's arms in frames on a wall behind.
			List<String> kit = List.copyOf(JugcraftArms.KIT.keySet());
			List<String> blades = List.of("bronze_katana", "steel_katana", "bronze_brazier_mace", "steel_brazier_mace");
			server.runCommand("fill %d %d %d %d %d %d minecraft:spruce_planks".formatted(x - 1, y, z - 12, x + 14, y + 5, z - 12));
			for (int i = 0; i < kit.size(); i++) {
				stand(server, x + 1.5 + i * 1.5, y, z - 8.5, kit.get(i));
				frame(server, x + 1 + i, y + 3, z - 11, kit.get(i));
			}
			for (int i = 0; i < blades.size(); i++) {
				frame(server, x + 3 + i, y + 4, z - 11, blades.get(i));
			}
			context.waitTicks(20);
			context.getInput().pressKey(options -> options.keyToggleGui);
			shoot(context, singleplayer, x + 6, y + 1, z - 2, 180, 12, "jugcraft_arms_vi_kit");
			context.getInput().pressKey(options -> options.keyToggleGui);
			server.runCommand("kill @e[type=minecraft:armor_stand]");
			server.runCommand("kill @e[type=minecraft:item_frame]");

			// The longbow: drawn in first person and from the front, then loosed.
			server.runCommand("gamemode survival @p");
			server.runCommand("give @p minecraft:arrow 32");
			ready(context, server, x + 6, y, z, "steel_longbow");
			context.getInput().holdKey(options -> options.keyUse);
			context.waitTicks(30);
			boolean drawing = context.computeOnClient(client -> client.player.isUsingItem());
			context.takeScreenshot("jugcraft_arms_vi_longbow_drawn");
			context.runOnClient(client -> client.options.setCameraType(CameraType.THIRD_PERSON_FRONT));
			context.waitTicks(5);
			context.takeScreenshot("jugcraft_arms_vi_longbow_front");
			context.getInput().releaseKey(options -> options.keyUse);
			context.waitTicks(3);
			int loosed = arrows(server, x, y, z);
			Jugcraft.LOGGER.info("[arms vi client] longbow: drawing {}, arrows in flight after the release {}", drawing, loosed);
			check(drawing && loosed >= 1, "The longbow was not drawn (" + drawing + ") or loosed no arrow (" + loosed + ")");

			// The arbalest: wound, shown loaded in first person, then fired.
			context.runOnClient(client -> client.options.setCameraType(CameraType.FIRST_PERSON));
			server.runCommand("kill @e[type=minecraft:arrow]");
			ready(context, server, x + 6, y, z, "steel_arbalest");
			context.getInput().holdKey(options -> options.keyUse);
			context.waitTicks(15);
			context.takeScreenshot("jugcraft_arms_vi_arbalest_winding");
			context.waitTicks(20);
			context.getInput().releaseKey(options -> options.keyUse);
			context.waitTicks(5);
			boolean loaded = context.computeOnClient(client -> CrossbowItem.isCharged(client.player.getMainHandItem()));
			context.takeScreenshot("jugcraft_arms_vi_arbalest_loaded");
			use(context);
			context.waitTicks(3);
			boolean spent = context.computeOnClient(client -> !CrossbowItem.isCharged(client.player.getMainHandItem()));
			int bolts = arrows(server, x, y, z);
			Jugcraft.LOGGER.info("[arms vi client] arbalest: loaded {}, then fired {} with {} bolt(s) in flight", loaded, spent, bolts);
			check(loaded && spent && bolts >= 1, "The arbalest did not load (" + loaded + ") or fire (" + spent + ", " + bolts + ")");

			// Shields in first person, beside vanilla's for comparison: each in the main hand, idle, then raised.
			for (String shield : List.of("minecraft:shield", "jugcraft:steel_heater_shield", "jugcraft:steel_tower_shield")) {
				server.runCommand("item replace entity @p weapon.mainhand with " + shield);
				context.waitTicks(20);
				String name = shield.substring(shield.indexOf(':') + 1);
				context.takeScreenshot("jugcraft_arms_vi_first_person_" + name);
				context.getInput().holdKey(options -> options.keyUse);
				context.waitTicks(15);
				context.takeScreenshot("jugcraft_arms_vi_first_person_" + name + "_raised");
				context.getInput().releaseKey(options -> options.keyUse);
				context.waitTicks(5);
			}

			// Shields: a tower shield raised in the main hand, then a heater shield in the off hand beside a katana.
			ready(context, server, x + 6, y, z, "steel_tower_shield");
			context.getInput().holdKey(options -> options.keyUse);
			context.waitTicks(15);
			boolean tower = context.computeOnClient(client -> client.player.isBlocking());
			context.takeScreenshot("jugcraft_arms_vi_tower_shield_block");
			context.runOnClient(client -> client.options.setCameraType(CameraType.THIRD_PERSON_FRONT));
			context.waitTicks(5);
			context.takeScreenshot("jugcraft_arms_vi_tower_shield_front");
			context.getInput().releaseKey(options -> options.keyUse);
			server.runCommand("item replace entity @p weapon.mainhand with air");
			server.runCommand("item replace entity @p weapon.offhand with jugcraft:bronze_heater_shield");
			context.waitTicks(10);
			context.getInput().holdKey(options -> options.keyUse);
			context.waitTicks(10);
			boolean heater = context.computeOnClient(client -> client.player.isBlocking());
			context.takeScreenshot("jugcraft_arms_vi_heater_shield_front");
			context.getInput().releaseKey(options -> options.keyUse);
			server.runCommand("item replace entity @p weapon.mainhand with jugcraft:bronze_katana");
			context.waitTicks(10);
			context.takeScreenshot("jugcraft_arms_vi_katana_and_heater");
			Jugcraft.LOGGER.info("[arms vi client] shields: tower blocking {}, heater blocking {}", tower, heater);
			check(tower && heater, "A shield did not block: tower " + tower + ", heater " + heater);
			server.runCommand("item replace entity @p weapon.offhand with air");

			// Seven cuts: two husks ahead and one behind, caught from behind mid-art with the arcs in the air.
			context.runOnClient(client -> client.options.setCameraType(CameraType.THIRD_PERSON_BACK));
			ready(context, server, x, y, z, "steel_katana");
			husk(server, x + 0.5, y, z + 0.5 - 2.0);
			husk(server, x + 1.5, y, z + 0.5 - 2.0);
			husk(server, x + 0.5, y, z + 0.5 + 3.0);
			context.waitTicks(10);
			float base = attack(server);
			use(context);
			context.waitTicks(10);
			int phase = context.computeOnClient(client -> ArmsMotion.artPhase(client.player));
			context.takeScreenshot("jugcraft_arms_vi_seven_cuts");
			context.waitTicks(20);
			List<Float> cut = healths(server, x, y, z);
			float seven = base * JugcraftArms.CUTS_SHARE * JugcraftArms.CUTS_COUNT;
			Jugcraft.LOGGER.info("[arms vi client] seven cuts of {} each: husks {} (north first); the client played phase {}",
					base * JugcraftArms.CUTS_SHARE, cut, phase);
			check(cut.size() == 3 && near(HEALTH - cut.get(0), seven) && near(HEALTH - cut.get(1), seven) && cut.get(2) == HEALTH
					&& phase == 0, "Seven cuts left " + cut + " from 100, not " + seven + " on the two ahead; client phase " + phase);
			server.runCommand("kill @e[type=minecraft:husk]");

			// The brazier mace: its flame from the front, then fire lit on the ground ahead with the use key.
			context.runOnClient(client -> client.options.setCameraType(CameraType.THIRD_PERSON_FRONT));
			ready(context, server, x + 6, y, z, "steel_brazier_mace");
			context.waitTicks(10);
			context.takeScreenshot("jugcraft_arms_vi_brazier_mace_front");
			context.runOnClient(client -> client.options.setCameraType(CameraType.FIRST_PERSON));
			server.runCommand(String.format(Locale.ROOT, "tp @p %.1f %d %.1f 180 60", x + 6.5, y, z + 0.5));
			context.waitTicks(10);
			use(context);
			context.waitTicks(5);
			int fires = server.computeOnServer(minecraft -> {
				int count = 0;
				for (BlockPos pos : BlockPos.betweenClosed(x + 3, y, z - 4, x + 9, y, z + 2)) {
					if (minecraft.overworld().getBlockState(pos).is(Blocks.FIRE)) {
						count++;
					}
				}
				return count;
			});
			context.takeScreenshot("jugcraft_arms_vi_brazier_mace_fire");
			Jugcraft.LOGGER.info("[arms vi client] brazier mace: {} fire block(s) lit on the ground", fires);
			check(fires >= 1, "The brazier mace lit no fire on the ground");
		}
	}

	private static void ready(ClientGameTestContext context, TestServerContext server, int x, int y, int z, String arm) {
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

	private static ServerPlayer player(MinecraftServer minecraft) {
		return minecraft.getPlayerList().getPlayers().getFirst();
	}

	/** The player's attack damage with the arm in hand, on the server. */
	private static float attack(TestServerContext server) {
		return server.computeOnServer(minecraft -> (float) player(minecraft).getAttributeValue(Attributes.ATTACK_DAMAGE));
	}

	/** Arrows the player has shot about (x, y, z). */
	private static int arrows(TestServerContext server, int x, int y, int z) {
		return server.computeOnServer(minecraft -> minecraft.overworld().getEntitiesOfClass(AbstractArrow.class,
				new AABB(x - 12, y - 2, z - 40, x + 18, y + 12, z + 10), arrow -> arrow.getOwner() == player(minecraft)).size());
	}

	/** The health of every husk about (x, y, z), nearest the north first. */
	private static List<Float> healths(TestServerContext server, int x, int y, int z) {
		return server.computeOnServer(minecraft -> {
			List<LivingEntity> found = new ArrayList<>(minecraft.overworld().getEntitiesOfClass(LivingEntity.class,
					new AABB(x - 12, y - 2, z - 18, x + 12, y + 6, z + 8),
					entity -> BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).getPath().equals("husk")));
			found.sort(Comparator.comparingDouble(LivingEntity::getZ));
			return found.stream().map(LivingEntity::getHealth).toList();
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
