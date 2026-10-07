package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.agriculture.BowlingScoreboardBlock;
import io.github.jimbozoomer.jugcraft.agriculture.BowlingScoreboardBlockEntity;
import io.github.jimbozoomer.jugcraft.agriculture.CandyBowlBlock;
import io.github.jimbozoomer.jugcraft.agriculture.CostumeRunwayBlock;
import io.github.jimbozoomer.jugcraft.agriculture.FortuneTellerTableBlock;
import io.github.jimbozoomer.jugcraft.agriculture.GhostBellBlock;
import io.github.jimbozoomer.jugcraft.agriculture.JudgesTableBlock;
import io.github.jimbozoomer.jugcraft.agriculture.JugcraftAgriculture;
import io.github.jimbozoomer.jugcraft.agriculture.JumpScareTrapBlock;
import io.github.jimbozoomer.jugcraft.agriculture.SkeletonPinBlock;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.JukeboxBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Client game test for the party games: a Costume Runway up to a Judges' Table with its round open and a witch-hatted
 * contestant on the runway; a bowling lane of ten Skeleton Pins (seven knocked down) and its Scoreboard; a
 * Monster Mash Dance Floor lit by a jukebox, with villagers on it; two Jump-Scare Traps (one gone off), the Ghost Bell
 * ringing, the Fortune Teller's Table mid-reading and a Candy Cache. Photographed by day and the dance floor at night
 * (CI job {@code client}).
 */
public class Decor11ClientGameTests implements FabricClientGameTest {
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
			server.runCommand("fill %d %d %d %d %d %d minecraft:grass_block".formatted(x - 8, y - 3, z - 20, x + 30, y - 1, z + 14));
			server.runCommand("fill %d %d %d %d %d %d minecraft:air".formatted(x - 8, y, z - 20, x + 30, y + 12, z + 14));
			context.waitTicks(10);
			server.runOnServer(minecraft -> build(minecraft.overworld(), origin));
			context.waitTicks(60);
			singleplayer.getConnection().waitForChunksRender();

			shoot(context, singleplayer, x + 13, y + 4, z + 2, 180, 18, "jugcraft_party_games");
			shoot(context, singleplayer, x + 4, y + 2, z - 5, 165, 15, "jugcraft_costume_contest");
			shoot(context, singleplayer, x + 9, y + 2, z - 4, 180, 15, "jugcraft_pumpkin_bowling");
			shoot(context, singleplayer, x + 9, y + 1, z - 8, 180, 8, "jugcraft_bowling_scoreboard");
			shoot(context, singleplayer, x + 14, y + 1, z - 1, 180, 12, "jugcraft_jump_scare_traps");
			read(server, x, y, z);
			shoot(context, singleplayer, x + 23, y + 1, z - 3, 180, 20, "jugcraft_ghost_bell_and_fortune_table");
			read(server, x, y, z);
			shoot(context, singleplayer, x + 23, y + 1, z - 5, 180, 60, "jugcraft_fortune_reading");
			server.runCommand("time set midnight");
			shoot(context, singleplayer, x + 17, y + 3, z - 5, 180, 30, "jugcraft_dance_floor_night");
			shoot(context, singleplayer, x + 13, y + 4, z + 2, 180, 18, "jugcraft_party_games_night");
		}
	}

	/** Stands the camera at (x, y, z) looking along yaw and pitch, and waits for the world to draw. */
	private static void place(ClientGameTestContext context, TestSingleplayerContext singleplayer, int x, int y, int z, int yaw, int pitch) {
		TestServerContext server = singleplayer.getServer();
		server.runCommand("setblock %d %d %d minecraft:barrier".formatted(x, y - 1, z));
		server.runCommand(String.format(Locale.ROOT, "tp @p %.1f %d %.1f %d %d", x + 0.5, y, z + 0.5, yaw, pitch));
		context.waitTicks(20);
		singleplayer.getConnection().waitForChunksRender();
	}

	private static void shoot(ClientGameTestContext context, TestSingleplayerContext singleplayer, int x, int y, int z, int yaw, int pitch,
			String name) {
		place(context, singleplayer, x, y, z, yaw, pitch);
		context.waitTicks(20);
		context.takeScreenshot(name);
	}

	/** Has the fortune teller's table read a fortune: the Pumpkin, and the planchette off to YES. */
	private static void read(TestServerContext server, int x, int y, int z) {
		server.runOnServer(minecraft -> {
			BlockPos table = new BlockPos(x + 23, y, z - 6);
			minecraft.overworld().blockEvent(table, JugcraftAgriculture.block("fortune_teller_table"), FortuneTellerTableBlock.READ,
					FortuneTellerTableBlock.reading(2, 0));
		});
	}

	private static void set(ServerLevel level, BlockPos pos, BlockState state) {
		level.setBlock(pos, state, Block.UPDATE_ALL);
	}

	private static Entity vanilla(ServerLevel level, String id) {
		return BuiltInRegistries.ENTITY_TYPE.getValue(Identifier.withDefaultNamespace(id)).create(level, EntitySpawnReason.COMMAND);
	}

	private static BlockState state(String id) {
		return JugcraftAgriculture.block(id).defaultBlockState();
	}

	private static void build(ServerLevel level, BlockPos origin) {
		int x = origin.getX();
		int y = origin.getY();
		int z = origin.getZ();
		// The costume contest: a runway up to the judges' table, its round open, and a contestant in a witch's hat.
		for (int dz = -12; dz <= -4; dz++) {
			set(level, new BlockPos(x + 2, y, z + dz), state("costume_runway").setValue(CostumeRunwayBlock.AXIS, Direction.Axis.Z));
		}
		set(level, new BlockPos(x + 2, y, z - 13), state("judges_table").setValue(JudgesTableBlock.FACING, Direction.SOUTH).setValue(JudgesTableBlock.OPEN, true));
		if (vanilla(level, "armor_stand") instanceof LivingEntity stand) {
			stand.setItemSlot(EquipmentSlot.HEAD, new ItemStack(JugcraftAgriculture.item("witch_hat")));
			stand.snapTo(x + 2.5, y + 0.07, z - 7.5, 0.0F, 0.0F);
			level.addFreshEntity(stand);
		}
		// A bowling lane: ten pins in a triangle, seven knocked down by the first ball, and the scoreboard behind them.
		int[][] pins = {{9, -9}, {8, -10}, {10, -10}, {7, -11}, {9, -11}, {11, -11}, {6, -12}, {8, -12}, {10, -12}, {12, -12}};
		List<BlockPos> lane = new ArrayList<>();
		for (int[] pin : pins) {
			BlockPos pos = new BlockPos(x + pin[0], y, z + pin[1]);
			set(level, pos, state("skeleton_pin").setValue(SkeletonPinBlock.FACING, Direction.SOUTH));
			lane.add(pos);
		}
		BlockPos boardPos = new BlockPos(x + 9, y, z - 13);
		set(level, boardPos, state("bowling_scoreboard").setValue(BowlingScoreboardBlock.FACING, Direction.SOUTH));
		if (level.getBlockEntity(boardPos) instanceof BowlingScoreboardBlockEntity board) {
			board.newGame(level);
			for (int i = 0; i < 7; i++) {
				SkeletonPinBlock.knock(level, lane.get(i), i % 2 == 0 ? Direction.NORTH : Direction.EAST);
			}
			board.rolled(level);
		}
		// The Monster Mash Dance Floor, five by five, a jukebox playing beside it and two villagers on it.
		for (int dx = 15; dx <= 19; dx++) {
			for (int dz = -14; dz <= -10; dz++) {
				set(level, new BlockPos(x + dx, y - 1, z + dz), state("dance_floor"));
			}
		}
		set(level, new BlockPos(x + 17, y - 1, z - 15), Blocks.JUKEBOX.defaultBlockState().setValue(JukeboxBlock.HAS_RECORD, true));
		for (int[] at : new int[][] {{16, -12}, {18, -11}}) {
			if (vanilla(level, "villager") instanceof Mob villager) {
				villager.snapTo(x + at[0] + 0.5, y, z + at[1] + 0.5, at[0] * 40.0F, 0.0F);
				villager.setNoAi(true);
				level.addFreshEntity(villager);
			}
		}
		// Along the front: two jump-scare traps (the right one gone off), the ghost bell ringing, the fortune teller's
		// table and a candy cache. The cameras keep out of the ready trap's reach.
		set(level, new BlockPos(x + 15, y, z - 6), state("jump_scare_trap").setValue(JumpScareTrapBlock.FACING, Direction.SOUTH)
				.setValue(JumpScareTrapBlock.PHASE, JumpScareTrapBlock.Phase.POPPED));
		set(level, new BlockPos(x + 20, y, z - 6), state("ghost_bell").setValue(GhostBellBlock.FACING, Direction.SOUTH).setValue(GhostBellBlock.RINGING, true));
		set(level, new BlockPos(x + 23, y, z - 6), state("fortune_teller_table").setValue(FortuneTellerTableBlock.FACING, Direction.SOUTH));
		set(level, new BlockPos(x + 26, y, z - 6), state("candy_cache").setValue(CandyBowlBlock.FACING, Direction.SOUTH));
		set(level, new BlockPos(x + 13, y, z - 6), state("jump_scare_trap").setValue(JumpScareTrapBlock.FACING, Direction.SOUTH));
	}
}
