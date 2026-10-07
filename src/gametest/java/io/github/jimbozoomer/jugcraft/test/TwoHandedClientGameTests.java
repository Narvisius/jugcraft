package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.weapons.JugcraftArms;
import io.github.jimbozoomer.jugcraft.weapons.TwoHanded;
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
 * Client game test for two-handed swings (Arms III, batch 46), end to end: the attack key, pressed with a greatsword at a
 * husk in front, goes through client/arms/TwoHandedInput to the server, which slows the player and lands the blow at
 * the strike tick, not on the click; with a shield in the off hand the same press does nothing (CI job {@code client}).
 */
public class TwoHandedClientGameTests implements FabricClientGameTest {
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
			server.runCommand("fill %d %d %d %d %d %d minecraft:smooth_stone".formatted(x - 6, y - 1, z - 6, x + 6, y - 1, z + 6));
			server.runCommand("fill %d %d %d %d %d %d minecraft:air".formatted(x - 6, y, z - 6, x + 6, y + 5, z + 6));
			// The player looks north at a still husk two and a half blocks away, a steel greatsword in hand.
			server.runCommand(String.format(Locale.ROOT, "tp @p %.1f %d %.1f 180 10", x + 0.5, y, z + 0.5));
			server.runCommand(String.format(Locale.ROOT, "summon minecraft:husk %.1f %d %.1f {NoAI:1b,PersistenceRequired:1b,"
					+ "Rotation:[0f,0f],attributes:[{id:\"minecraft:armor\",base:0.0d},{id:\"minecraft:max_health\",base:100.0d}],"
					+ "Health:100.0f}", x + 0.5, y, z - 2.0));
			server.runCommand("item replace entity @p weapon.mainhand with jugcraft:steel_greatsword");
			context.runOnClient(client -> client.options.setCameraType(CameraType.FIRST_PERSON));
			context.waitTicks(40);

			float before = health(server, x, y, z);
			context.getInput().pressKey(options -> options.keyAttack);
			context.waitTicks(2);
			float early = health(server, x, y, z);
			boolean swinging = server.computeOnServer(minecraft -> TwoHanded.swinging(player(minecraft)));
			boolean slowed = server.computeOnServer(minecraft -> player(minecraft).getAttribute(Attributes.MOVEMENT_SPEED)
					.hasModifier(TwoHanded.SLOW));
			context.waitTicks(JugcraftArms.TWO_HANDED.get("greatsword").strike() - 4);
			context.takeScreenshot("jugcraft_two_handed_blow");
			context.waitTicks(6);
			float after = health(server, x, y, z);
			Jugcraft.LOGGER.info("[two handed] greatsword at a husk: health {} -> {} two ticks after the click -> {} after the "
					+ "strike; swinging {}, slowed {}", before, early, after, swinging, slowed);
			if (early != before || after >= before || !swinging || !slowed) {
				throw new AssertionError("The two-handed swing did not land late on the husk: " + before + " -> " + early + " -> "
						+ after + ", swinging " + swinging + ", slowed " + slowed);
			}

			// With a shield in the off hand there is no hand for the grip: the press does nothing.
			server.runCommand("item replace entity @p weapon.offhand with minecraft:shield");
			context.waitTicks(30);
			context.getInput().pressKey(options -> options.keyAttack);
			context.waitTicks(15);
			float shielded = health(server, x, y, z);
			Jugcraft.LOGGER.info("[two handed] with a shield in the off hand: health {} -> {}", after, shielded);
			context.takeScreenshot("jugcraft_two_handed_shield");
			if (shielded != after) {
				throw new AssertionError("A greatsword swung with a shield in the off hand: " + after + " -> " + shielded);
			}
		}
	}

	private static ServerPlayer player(MinecraftServer minecraft) {
		return minecraft.getPlayerList().getPlayers().getFirst();
	}

	/** The husk's health, on the server. */
	private static float health(TestServerContext server, int x, int y, int z) {
		return server.computeOnServer(minecraft -> minecraft.overworld().getEntitiesOfClass(LivingEntity.class,
				new AABB(x - 4, y - 2, z - 6, x + 5, y + 4, z + 2), entity -> BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).getPath().equals("husk"))
				.stream().findFirst().map(LivingEntity::getHealth).orElse(-1.0F));
	}
}
