package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.agriculture.CornMaze;
import io.github.jimbozoomer.jugcraft.agriculture.CornMazeGateBlock;
import io.github.jimbozoomer.jugcraft.agriculture.CornMazeGateBlockEntity;
import io.github.jimbozoomer.jugcraft.agriculture.JugcraftAgriculture;
import io.github.jimbozoomer.jugcraft.agriculture.MazeCornBlock;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * In-game tests for the corn maze: carved mazes are perfect (one way through, every square reachable) and the same seed
 * gives the same maze; a gate plants its maze (a kernel a stalk, the walls three tall, the finish post at the exit), not
 * with too few kernels and not twice; a run along the way through is timed and boarded with a ribbon and A-maze-ing; a
 * shortcut, flying or leaving voids a run; maze corn stands on solid ground, blocks the way and gives back its kernel.
 *
 * <p>Each test plants a tiny maze (7 by 7), inside its own test area, with its gate on the area's near edge.
 */
public class MazeGameTests {
	private static final BlockPos GATE = new BlockPos(3, 2, 0);

	private static void floor(GameTestHelper helper) {
		for (int x = 0; x <= 7; x++) {
			for (int z = 0; z <= 7; z++) {
				helper.setBlock(new BlockPos(x, 1, z), Blocks.GRASS_BLOCK);
			}
		}
	}

	/** A gate at {@link #GATE} facing into the area, set to a tiny maze. */
	private static CornMazeGateBlockEntity gate(GameTestHelper helper) {
		helper.setBlock(GATE, JugcraftAgriculture.block("corn_maze_gate").defaultBlockState().setValue(CornMazeGateBlock.FACING, Direction.SOUTH));
		CornMazeGateBlockEntity gate = helper.getBlockEntity(GATE, CornMazeGateBlockEntity.class);
		while (gate.size() != 0) {
			gate.nextSize();
		}
		return gate;
	}

	private static ServerPlayer player(GameTestHelper helper, BlockPos standAt, ItemStack held) {
		ServerPlayer player = helper.makeMockServerPlayerInLevel();
		player.setGameMode(GameType.SURVIVAL);
		BlockPos absolute = helper.absolutePos(standAt);
		player.setPos(absolute.getX() + 0.5, absolute.getY(), absolute.getZ() + 0.5);
		player.setItemInHand(InteractionHand.MAIN_HAND, held);
		return player;
	}

	private static void use(GameTestHelper helper, ServerPlayer player, BlockPos pos) {
		BlockPos absolute = helper.absolutePos(pos);
		player.gameMode.useItemOn(player, helper.getLevel(), player.getMainHandItem(), InteractionHand.MAIN_HAND,
				new BlockHitResult(Vec3.atCenterOf(absolute), Direction.NORTH, absolute, false));
	}

	/** The way through {@code maze}, square by square from the entrance to the exit. */
	private static List<int[]> solve(CornMaze maze) {
		int side = maze.side();
		Map<Integer, Integer> from = new HashMap<>();
		Deque<int[]> queue = new ArrayDeque<>();
		int start = maze.cells() * side;
		int end = maze.cells() * side + side - 1;
		from.put(start, -1);
		queue.add(new int[] {maze.cells(), 0});
		int[][] steps = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
		while (!queue.isEmpty()) {
			int[] here = queue.poll();
			for (int[] step : steps) {
				int x = here[0] + step[0];
				int y = here[1] + step[1];
				if (x >= 0 && y >= 0 && x < side && y < side && !maze.wall(x, y) && !from.containsKey(x * side + y)) {
					from.put(x * side + y, here[0] * side + here[1]);
					queue.add(new int[] {x, y});
				}
			}
		}
		List<int[]> path = new ArrayList<>();
		for (Integer at = end; at != null && at >= 0; at = from.get(at)) {
			path.add(new int[] {at / side, at % side});
		}
		Collections.reverse(path);
		return path;
	}

	/** The first seed from 1 up whose tiny maze winds: its way through is at least ten steps. */
	private static long windingSeed() {
		for (long seed = 1; ; seed++) {
			if (CornMaze.carve(CornMaze.CELLS[0], seed).shortest() >= 10) {
				return seed;
			}
		}
	}

	/** Every size of maze has one way through and every open square reachable; the same seed carves the same maze. */
	@GameTest(maxTicks = 20)
	public void carvedMazesArePerfect(GameTestHelper helper) {
		for (int cells : CornMaze.CELLS) {
			for (long seed = 1; seed <= 20; seed++) {
				CornMaze maze = CornMaze.carve(cells, seed);
				int open = 0;
				for (int x = 0; x < maze.side(); x++) {
					for (int y = 0; y < maze.side(); y++) {
						open += maze.wall(x, y) ? 0 : 1;
					}
				}
				// A perfect maze of n cells has n - 1 passages between them, plus the entrance and the exit.
				helper.assertTrue(open == cells * cells + (cells * cells - 1) + 2, "A " + cells + "-cell maze opens " + open + " squares");
				List<int[]> way = solve(maze);
				helper.assertTrue(way.size() == maze.shortest() + 1 && maze.shortest() >= maze.side() - 1, "It has a way through");
				helper.assertTrue(CornMaze.carve(cells, seed).walls().size() == maze.walls().size(), "The same seed carves the same maze");
			}
		}
		helper.succeed();
	}

	/**
	 * Too few kernels plant nothing; enough plant the maze, a kernel a stalk: three-tall maze corn on every wall square,
	 * the way through left open and the finish post at the exit; a planted maze isn't planted again.
	 */
	@GameTest(maxTicks = 40)
	public void aGatePlantsItsMaze(GameTestHelper helper) {
		floor(helper);
		CornMazeGateBlockEntity gate = gate(helper);
		ServerPlayer player = player(helper, new BlockPos(3, 2, 7), new ItemStack(JugcraftAgriculture.item("corn_kernels"), 5));
		use(helper, player, GATE);
		helper.assertTrue(!gate.planted() && player.getMainHandItem().getCount() == 5, "Five kernels aren't enough for a maze");
		player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(JugcraftAgriculture.item("corn_kernels"), 64));
		use(helper, player, GATE);
		CornMaze maze = gate.maze();
		int walls = maze.walls().size();
		helper.assertTrue(gate.planted() && player.getMainHandItem().getCount() == 64 - walls, "A kernel for each of the " + walls + " stalks");
		use(helper, player, GATE);
		helper.assertTrue(player.getMainHandItem().getCount() == 64 - walls, "A planted maze isn't planted again");
		helper.runAfterDelay(3, () -> {
			BlockPos gateAt = helper.absolutePos(GATE);
			for (int x = 0; x < maze.side(); x++) {
				for (int y = 0; y < maze.side(); y++) {
					BlockPos square = maze.at(gateAt, Direction.SOUTH, x, y);
					BlockState state = helper.getLevel().getBlockState(square);
					if (maze.wall(x, y)) {
						helper.assertTrue(state.is(JugcraftAgriculture.block("maze_corn")) && state.getValue(MazeCornBlock.SECTION) == 0
								&& helper.getLevel().getBlockState(square.above(2)).getValue(MazeCornBlock.SECTION) == 2, "Corn three tall at " + x + ", " + y);
					} else {
						helper.assertFalse(state.is(JugcraftAgriculture.block("maze_corn")), "The way is open at " + x + ", " + y);
					}
				}
			}
			helper.assertTrue(helper.getLevel().getBlockState(maze.finish(gateAt, Direction.SOUTH)).is(JugcraftAgriculture.block("corn_maze_finish")),
					"The finish post stands at the exit");
			helper.succeed();
		});
	}

	/**
	 * A runner who walks the way through is timed and boarded (first place, a first prize ribbon, A-maze-ing); one who cuts
	 * straight to the finish took a shortcut and isn't; one who flies, or leaves the maze, has the run voided.
	 */
	@GameTest(maxTicks = 200)
	public void runsAreTimedAndJudged(GameTestHelper helper) {
		floor(helper);
		ServerLevel level = helper.getLevel();
		CornMazeGateBlockEntity gate = gate(helper);
		long seed = windingSeed();
		gate.plant(level, seed, gate.survey(level, seed));
		CornMaze maze = gate.maze();
		List<int[]> way = solve(maze);
		BlockPos gateAt = helper.absolutePos(GATE);
		ServerPlayer runner = player(helper, GATE, ItemStack.EMPTY);
		int[] tick = {0};
		int walkFrom = 3;
		helper.onEachTick(() -> {
			int step = tick[0]++ - walkFrom;
			if (step >= 0 && step < way.size()) {
				BlockPos square = maze.at(gateAt, Direction.SOUTH, way.get(step)[0], way.get(step)[1]);
				runner.setPos(square.getX() + 0.5, square.getY(), square.getZ() + 0.5);
			}
		});
		helper.runAfterDelay(walkFrom + way.size() + 3, () -> {
			helper.assertTrue(gate.board().size() == 1 && gate.board().get(0).runner().equals(runner.getUUID()),
					"Walking the way through puts the runner on the board");
			int ticks = gate.board().get(0).ticks();
			helper.assertTrue(ticks >= way.size() - 3 && ticks <= way.size() + 2, "in about the time it took: " + ticks + " for " + way.size() + " steps");
			helper.assertTrue(runner.getInventory().countItem(JugcraftAgriculture.item("first_prize_ribbon")) == 1, "with a first prize ribbon");
			AdvancementHolder amazing = level.getServer().getAdvancements().get(Jugcraft.id("amazing"));
			helper.assertTrue(amazing != null && runner.getAdvancements().getOrStartProgress(amazing).isDone(), "and A-maze-ing");

			// Straight from the gate to the finish: a shortcut.
			long now = level.getGameTime();
			gate.start(runner, now);
			BlockPos finish = maze.finish(gateAt, Direction.SOUTH);
			runner.setPos(finish.getX() + 0.5, finish.getY(), finish.getZ() + 0.5);
			gate.track(level, now + 1);
			helper.assertTrue(gate.finish(runner, now + 1) == -1 && gate.board().get(0).ticks() == ticks, "A shortcut doesn't count");

			// Flying, the run is void.
			runner.setPos(gateAt.getX() + 0.5, gateAt.getY(), gateAt.getZ() + 0.5);
			gate.start(runner, now + 2);
			runner.getAbilities().flying = true;
			gate.track(level, now + 3);
			helper.assertFalse(gate.running(runner.getUUID()), "Flying voids the run");
			runner.getAbilities().flying = false;

			// Out of the maze, the run is void.
			gate.start(runner, now + 4);
			runner.setPos(gateAt.getX() + 0.5, gateAt.getY(), gateAt.getZ() - 6.5);
			gate.track(level, now + 5);
			helper.assertFalse(gate.running(runner.getUUID()), "Leaving the maze voids the run");
			helper.succeed();
		});
	}

	/** Maze corn stands three tall on solid ground and blocks the way; breaking the bottom brings it down and gives back a kernel. */
	@GameTest(maxTicks = 40)
	public void mazeCornIsAWall(GameTestHelper helper) {
		floor(helper);
		BlockPos base = new BlockPos(4, 2, 4);
		BlockState corn = JugcraftAgriculture.block("maze_corn").defaultBlockState();
		for (int section = 0; section < 3; section++) {
			helper.setBlock(base.above(section), corn.setValue(MazeCornBlock.SECTION, section));
		}
		ServerLevel level = helper.getLevel();
		helper.assertFalse(helper.getBlockState(base).getCollisionShape(level, helper.absolutePos(base)).isEmpty(), "Maze corn can't be walked through");
		level.destroyBlock(helper.absolutePos(base), true);
		helper.assertTrue(helper.getBlockState(base.above()).isAir() && helper.getBlockState(base.above(2)).isAir(), "Breaking the bottom brings it down");
		helper.assertTrue(level.recipeAccess().byKey(ResourceKey.create(Registries.RECIPE, Jugcraft.id("corn_maze_gate"))).isPresent()
				&& level.getServer().reloadableRegistries().getLootTable(ResourceKey.create(Registries.LOOT_TABLE, Jugcraft.id("blocks/maze_corn")))
						!= LootTable.EMPTY, "The gate's recipe and maze corn's loot load");
		helper.succeedWhen(() -> {
			int kernels = 0;
			for (ItemEntity item : level.getEntitiesOfClass(ItemEntity.class, new AABB(helper.absolutePos(base)).inflate(3.0))) {
				kernels += item.getItem().is(JugcraftAgriculture.item("corn_kernels")) ? item.getItem().getCount() : 0;
			}
			helper.assertTrue(kernels == 1, "One kernel back: " + kernels);
		});
	}
}
