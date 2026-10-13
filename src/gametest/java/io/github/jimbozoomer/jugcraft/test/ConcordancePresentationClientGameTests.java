package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.concordance.Alchemy;
import io.github.jimbozoomer.jugcraft.concordance.ConcordanceProgress;
import io.github.jimbozoomer.jugcraft.concordance.CrucibleBlockEntity;
import io.github.jimbozoomer.jugcraft.concordance.JugcraftConcordance;
import io.github.jimbozoomer.jugcraft.concordance.RateGate;
import io.github.jimbozoomer.jugcraft.concordance.rules.ResearchState;
import io.github.jimbozoomer.jugcraft.concordance.sign.Presentation;
import io.github.jimbozoomer.jugcraft.concordance.sign.Sign;
import io.github.jimbozoomer.jugcraft.concordance.sign.Signs;
import java.util.Locale;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

/**
 * Roadmap step 27 in a real client: presentation that shows what is really there. Four Alembic Crucibles in a row, each
 * worked by hand on the server: three parts of plain water; six (full); three parts tasting of Radiance (glowstone
 * stirred hot over a campfire); and one part made murky by a searing stir, kept searing over lava. The renderer stands
 * each liquid at its volume in its colour, and the searing one smokes (screenshot {@code jugcraft_concordance_crucibles}).
 * Then the server shows a shortage, a success and a danger over them, drawn at full intensity
 * ({@code jugcraft_concordance_signs_full}) and at minimal, where only the two warnings remain
 * ({@code jugcraft_concordance_signs_minimal}). CI job {@code client}.
 */
public class ConcordancePresentationClientGameTests implements FabricClientGameTest {
	@Override
	public void runTest(ClientGameTestContext context) {
		try (TestSingleplayerContext singleplayer = context.worldBuilder()
				.adjustSettings(creator -> creator.setGameMode(WorldCreationUiState.SelectedGameMode.CREATIVE)).create()) {
			singleplayer.getConnection().waitForChunksRender();
			context.runOnClient(client -> {
				if (!client.gui.hud.isHidden()) {
					client.gui.hud.toggle();
				}
			});
			BlockPos origin = context.computeOnClient(client -> BlockPos.containing(client.player.position()));
			int x = origin.getX();
			int y = origin.getY();
			int z = origin.getZ();
			TestServerContext server = singleplayer.getServer();
			server.runCommand("time set noon");
			server.runCommand("weather clear");
			server.runCommand("gamerule minecraft:send_command_feedback false");
			server.runCommand("fill %d %d %d %d %d %d minecraft:stone".formatted(x - 4, y - 3, z - 8, x + 12, y - 1, z + 6));
			server.runCommand("fill %d %d %d %d %d %d minecraft:air".formatted(x - 4, y, z - 8, x + 12, y + 8, z + 6));
			context.waitTicks(10);
			server.runOnServer(minecraft -> build(minecraft.overworld(), origin, minecraft.getPlayerList().getPlayers().get(0)));
			context.waitTicks(40);
			singleplayer.getConnection().waitForChunksRender();
			place(context, singleplayer, x + 4, y + 1, z + 1, 180, 35);
			context.takeScreenshot("jugcraft_concordance_crucibles");

			server.runOnServer(minecraft -> signs(minecraft.overworld(), origin));
			context.waitTicks(3);
			context.takeScreenshot("jugcraft_concordance_signs_full");
			// Let those particles die away, then the same signs drawn at minimal intensity.
			context.waitTicks(100);
			Presentation.Intensity was = context.computeOnClient(client -> Presentation.intensity());
			boolean calm = context.computeOnClient(client -> Presentation.reducedMotion());
			context.runOnClient(client -> Presentation.configure(Presentation.Intensity.MINIMAL, calm));
			server.runOnServer(minecraft -> signs(minecraft.overworld(), origin));
			context.waitTicks(3);
			context.takeScreenshot("jugcraft_concordance_signs_minimal");
			context.runOnClient(client -> Presentation.configure(was, calm));
		}
	}

	/** Stands the camera at (x, y, z) looking along yaw and pitch, and waits for the world to draw. */
	private static void place(ClientGameTestContext context, TestSingleplayerContext singleplayer, int x, int y, int z, int yaw, int pitch) {
		TestServerContext server = singleplayer.getServer();
		server.runCommand("fill %d %d %d %d %d %d minecraft:barrier replace minecraft:air".formatted(x, y - 1, z, x, y - 1, z));
		server.runCommand(String.format(Locale.ROOT, "tp @p %.1f %d %.1f %d %d", x + 0.5, y, z + 0.5, yaw, pitch));
		context.waitTicks(20);
		singleplayer.getConnection().waitForChunksRender();
	}

	/** The crucibles stand on the floor; what heats each is set into the floor beneath it, so the lava cannot spread. */
	private static BlockPos pot(BlockPos origin, int index) {
		return origin.offset(1 + 2 * index, 0, -3);
	}

	/** The four crucibles, worked by hand by a player who understands the Alembic Arts. */
	private static void build(ServerLevel level, BlockPos origin, ServerPlayer player) {
		ConcordanceProgress.grant(player, "jugcraft:first_light", ResearchState.UNDERSTOOD);
		ConcordanceProgress.grant(player, Alchemy.RESEARCH, ResearchState.UNDERSTOOD);
		Block[] beneath = {Blocks.STONE, Blocks.STONE, Blocks.CAMPFIRE, Blocks.LAVA};
		for (int i = 0; i < beneath.length; i++) {
			BlockPos pot = pot(origin, i);
			level.setBlock(pot.below(), beneath[i].defaultBlockState(), Block.UPDATE_ALL);
			level.setBlock(pot, JugcraftConcordance.CRUCIBLE.defaultBlockState(), Block.UPDATE_ALL);
		}
		CrucibleBlockEntity plain = crucible(level, origin, 0);
		use(plain, player, level, new ItemStack(Items.WATER_BUCKET));
		CrucibleBlockEntity full = crucible(level, origin, 1);
		use(full, player, level, new ItemStack(Items.WATER_BUCKET));
		use(full, player, level, new ItemStack(Items.WATER_BUCKET));
		CrucibleBlockEntity radiant = crucible(level, origin, 2);
		radiant.setTemperature(120);
		use(radiant, player, level, new ItemStack(Items.WATER_BUCKET));
		use(radiant, player, level, new ItemStack(Items.GLOWSTONE_DUST, 2));
		use(radiant, player, level, new ItemStack(Items.STICK));
		CrucibleBlockEntity murky = crucible(level, origin, 3);
		murky.setTemperature(220);
		use(murky, player, level, PotionContents.createItemStack(Items.POTION, Potions.WATER));
		use(murky, player, level, new ItemStack(Items.GLOWSTONE_DUST));
		use(murky, player, level, new ItemStack(Items.STICK));
	}

	private static CrucibleBlockEntity crucible(ServerLevel level, BlockPos origin, int index) {
		return (CrucibleBlockEntity) level.getBlockEntity(pot(origin, index));
	}

	private static void use(CrucibleBlockEntity crucible, ServerPlayer player, ServerLevel level, ItemStack held) {
		RateGate.forget(player.getUUID());
		player.setItemInHand(InteractionHand.MAIN_HAND, held);
		crucible.use(player, level, held, InteractionHand.MAIN_HAND);
	}

	/** A shortage over the plain water, a success over the full crucible, and a danger over the searing one. */
	private static void signs(ServerLevel level, BlockPos origin) {
		Signs.show(level, pot(origin, 0), Sign.WANT);
		Signs.show(level, pot(origin, 1), Sign.DONE);
		Signs.show(level, pot(origin, 3), Sign.PERIL);
	}
}
