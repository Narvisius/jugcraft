package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.agriculture.JugcraftAgriculture;
import io.github.jimbozoomer.jugcraft.diagonal.DiagonalConnections;
import io.github.jimbozoomer.jugcraft.diagonal.DiagonalConnections.Diagonal;
import io.github.jimbozoomer.jugcraft.diagonal.DiagonalWallBlock;
import io.github.jimbozoomer.jugcraft.diagonal.DiagonalWalls;
import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.List;
import javax.management.ObjectName;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CrossCollisionBlock;
import net.minecraft.world.level.block.FenceBlock;
import net.minecraft.world.level.block.IronBarsBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.WallBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.WallSide;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Diagonal connections (docs/features/diagonal-connections.md): fences, panes, bars and walls a diagonal step apart join
 * across an open corner, from either end; a straight connection or a block in the corner keeps them apart; breaking one
 * lets the other go; only kinds that join straight join diagonally; rotation and mirroring turn the diagonals; the arms
 * show in the outline and the collision, and alike blocks share them; a wall that joins diagonally becomes its
 * diagonal wall, which players get as the vanilla wall and which turns back into it; a wall running on along a diagonal
 * drops its post unless something above calls for one; every fence and bars block has the properties, starting false,
 * and every vanilla wall has a diagonal wall; and the block states are counted and weighed in the log.
 */
public class DiagonalConnectionsGameTests {
	private static final Logger LOGGER = LoggerFactory.getLogger("jugcraft-diagonal-tests");
	/** A block and the one a north-east step from it (x + 1, z - 1). */
	private static final BlockPos FIRST = new BlockPos(2, 1, 3);
	private static final BlockPos NORTH_EAST = new BlockPos(3, 1, 2);
	/** The two blocks between them: north of the first (west of the other) and east of it (south of the other). */
	private static final BlockPos NORTH_CORNER = new BlockPos(2, 1, 2);
	private static final BlockPos EAST_CORNER = new BlockPos(3, 1, 3);
	/** The block a north-east step beyond NORTH_EAST, so FIRST, NORTH_EAST and this run straight on along a diagonal. */
	private static final BlockPos BEYOND = new BlockPos(4, 1, 1);
	private static final List<Direction> SIDES = List.of(Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST);

	private static void floor(GameTestHelper helper) {
		for (int x = 0; x <= 6; x++) {
			for (int z = 0; z <= 6; z++) {
				helper.setBlock(new BlockPos(x, 0, z), Blocks.STONE);
			}
		}
	}

	private static ServerPlayer player(GameTestHelper helper) {
		ServerPlayer player = helper.makeMockServerPlayerInLevel();
		player.setGameMode(GameType.SURVIVAL);
		BlockPos absolute = helper.absolutePos(new BlockPos(0, 1, 6));
		player.setPos(absolute.getX() + 0.5, absolute.getY(), absolute.getZ() + 0.5);
		return player;
	}

	/** The player places `item` on the floor at `pos`, clicking the top of the block below, as a player does. */
	private static void place(GameTestHelper helper, ServerPlayer player, Item item, BlockPos pos) {
		ItemStack stack = new ItemStack(item);
		player.setItemInHand(InteractionHand.MAIN_HAND, stack);
		BlockPos below = helper.absolutePos(pos.below());
		player.gameMode.useItemOn(player, helper.getLevel(), stack, InteractionHand.MAIN_HAND,
				new BlockHitResult(Vec3.atCenterOf(below).relative(Direction.UP, 0.5), Direction.UP, below, false));
	}

	private static void set(GameTestHelper helper, BlockPos pos, Block block) {
		helper.setBlock(pos, block.defaultBlockState());
	}

	private static String describe(BlockState state) {
		List<String> joined = new ArrayList<>();
		for (Direction direction : SIDES) {
			if (state.getBlock() instanceof WallBlock) {
				WallSide side = state.getValue(WallBlock.PROPERTY_BY_DIRECTION.get(direction));
				if (side != WallSide.NONE) {
					joined.add(direction.getName() + "=" + side.getSerializedName());
				}
			} else if (state.hasProperty(CrossCollisionBlock.PROPERTY_BY_DIRECTION.get(direction))
					&& state.getValue(CrossCollisionBlock.PROPERTY_BY_DIRECTION.get(direction))) {
				joined.add(direction.getName());
			}
		}
		if (state.hasProperty(WallBlock.UP) && state.getValue(WallBlock.UP)) {
			joined.add("post");
		}
		if (state.hasProperty(DiagonalWallBlock.TALL) && state.getValue(DiagonalWallBlock.TALL)) {
			joined.add("tall");
		}
		for (Diagonal diagonal : Diagonal.ALL) {
			if (DiagonalConnections.hasDiagonals(state) && state.getValue(diagonal.property)) {
				joined.add(diagonal.property.getName());
			}
		}
		return BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString() + joined;
	}

	private static Block vanilla(String id) {
		return BuiltInRegistries.BLOCK.getValue(Identifier.withDefaultNamespace(id));
	}

	private static boolean post(GameTestHelper helper, BlockPos pos) {
		return helper.getBlockState(pos).getValue(WallBlock.UP);
	}

	private static boolean tall(GameTestHelper helper, BlockPos pos) {
		BlockState state = helper.getBlockState(pos);
		return state.hasProperty(DiagonalWallBlock.TALL) && state.getValue(DiagonalWallBlock.TALL);
	}

	/** Whether the first block and its north-east neighbour are joined to each other and to nothing else diagonally. */
	private static boolean joinedPair(GameTestHelper helper) {
		BlockState first = helper.getBlockState(FIRST);
		BlockState other = helper.getBlockState(NORTH_EAST);
		return DiagonalConnections.mask(first) == 1 << Diagonal.NORTH_EAST.ordinal()
				&& DiagonalConnections.mask(other) == 1 << Diagonal.SOUTH_WEST.ordinal();
	}

	private static boolean contains(VoxelShape shape, double x, double y, double z) {
		return shape.toAabbs().stream().anyMatch(box -> box.contains(x, y, z));
	}

	/**
	 * A player places two oak fences a diagonal step apart. The second one's placement joins it to the first, and the
	 * first is told and joins back. Its outline and collision reach its north-east corner and not the others.
	 */
	@GameTest
	public void placedFencesJoinDiagonally(GameTestHelper helper) {
		floor(helper);
		ServerPlayer player = player(helper);
		place(helper, player, Items.OAK_FENCE, FIRST);
		place(helper, player, Items.OAK_FENCE, NORTH_EAST);
		BlockState first = helper.getBlockState(FIRST);
		BlockState other = helper.getBlockState(NORTH_EAST);
		LOGGER.info("Placed diagonally: {} and {}", describe(first), describe(other));
		helper.assertTrue(first.is(Blocks.OAK_FENCE) && other.is(Blocks.OAK_FENCE), "Both fences are placed");
		helper.assertTrue(joinedPair(helper), "The fences join diagonally: " + describe(first) + ", " + describe(other));

		ServerLevel level = helper.getLevel();
		BlockPos absolute = helper.absolutePos(FIRST);
		VoxelShape outline = first.getShape(level, absolute);
		VoxelShape collision = first.getCollisionShape(level, absolute);
		helper.assertTrue(contains(outline, 0.9, 0.6, 0.1) && contains(collision, 0.9, 1.2, 0.1),
				"The outline and collision reach the north-east corner: " + outline.bounds() + ", " + collision.bounds());
		helper.assertTrue(!contains(outline, 0.1, 0.6, 0.9) && !contains(collision, 0.1, 0.6, 0.9) && !contains(collision, 0.1, 0.6, 0.1),
				"No arm reaches the other corners");
		helper.succeed();
	}

	/** A block in the corner joins both fences straight and keeps them apart; taken away, they join diagonally again. */
	@GameTest
	public void aBlockInTheCornerKeepsThemApart(GameTestHelper helper) {
		floor(helper);
		set(helper, FIRST, Blocks.OAK_FENCE);
		set(helper, NORTH_EAST, Blocks.OAK_FENCE);
		helper.assertTrue(joinedPair(helper), "Set side by side diagonally, the fences join: " + describe(helper.getBlockState(FIRST)));
		set(helper, NORTH_CORNER, Blocks.STONE);
		BlockState first = helper.getBlockState(FIRST);
		BlockState other = helper.getBlockState(NORTH_EAST);
		helper.assertTrue(first.getValue(CrossCollisionBlock.NORTH) && other.getValue(CrossCollisionBlock.WEST)
				&& DiagonalConnections.mask(first) == 0 && DiagonalConnections.mask(other) == 0,
				"With stone in the corner both join it and not each other: " + describe(first) + ", " + describe(other));
		set(helper, NORTH_CORNER, Blocks.AIR);
		helper.assertTrue(joinedPair(helper), "With the corner clear again they rejoin: " + describe(helper.getBlockState(FIRST)) + ", "
				+ describe(helper.getBlockState(NORTH_EAST)));
		helper.succeed();
	}

	/** A fence joined straight around the corner does not also join diagonally, so arms never cross a corner twice. */
	@GameTest
	public void aStraightCornerComesFirst(GameTestHelper helper) {
		floor(helper);
		ServerPlayer player = player(helper);
		place(helper, player, Items.SPRUCE_FENCE, FIRST);
		place(helper, player, Items.SPRUCE_FENCE, EAST_CORNER);
		place(helper, player, Items.SPRUCE_FENCE, NORTH_EAST);
		BlockState first = helper.getBlockState(FIRST);
		BlockState corner = helper.getBlockState(EAST_CORNER);
		BlockState other = helper.getBlockState(NORTH_EAST);
		helper.assertTrue(first.getValue(CrossCollisionBlock.EAST) && corner.getValue(CrossCollisionBlock.WEST)
				&& corner.getValue(CrossCollisionBlock.NORTH) && other.getValue(CrossCollisionBlock.SOUTH),
				"The fences join straight around the corner: " + describe(first) + ", " + describe(corner) + ", " + describe(other));
		helper.assertTrue(DiagonalConnections.mask(first) == 0 && DiagonalConnections.mask(other) == 0 && DiagonalConnections.mask(corner) == 0,
				"and not diagonally");
		helper.succeed();
	}

	/** Breaking one of a joined pair lets the other go. */
	@GameTest
	public void breakingOneLetsTheOtherGo(GameTestHelper helper) {
		floor(helper);
		set(helper, FIRST, Blocks.IRON_BARS);
		set(helper, NORTH_EAST, Blocks.IRON_BARS);
		helper.assertTrue(joinedPair(helper), "Iron bars join diagonally: " + describe(helper.getBlockState(FIRST)));
		helper.destroyBlock(NORTH_EAST);
		helper.assertTrue(DiagonalConnections.mask(helper.getBlockState(FIRST)) == 0,
				"Broken, its partner lets go: " + describe(helper.getBlockState(FIRST)));
		helper.succeed();
	}

	/** Kinds that join straight join diagonally, and no others. */
	@GameTest
	public void onlyKindsThatJoinStraightJoinDiagonally(GameTestHelper helper) {
		floor(helper);
		Block cemetery = JugcraftAgriculture.block("cemetery_fence");
		Block aspen = BuiltInRegistries.BLOCK.getValue(Jugcraft.id("aspen_fence"));
		Block copperBars = BuiltInRegistries.BLOCK.getValue(Identifier.withDefaultNamespace("copper_bars"));
		Block redPane = BuiltInRegistries.BLOCK.getValue(Identifier.withDefaultNamespace("red_stained_glass_pane"));
		Object[][] pairs = {
				{Blocks.OAK_FENCE, Blocks.SPRUCE_FENCE, true}, {Blocks.OAK_FENCE, aspen, true}, {Blocks.OAK_FENCE, Blocks.NETHER_BRICK_FENCE, false},
				{cemetery, cemetery, true}, {cemetery, Blocks.OAK_FENCE, false}, {Blocks.GLASS_PANE, Blocks.IRON_BARS, true},
				{redPane, copperBars, true}, {Blocks.IRON_BARS, Blocks.OAK_FENCE, false},
				{Blocks.BAMBOO_FENCE, Blocks.BAMBOO_FENCE, true}, {vanilla("cobblestone_wall"), vanilla("mossy_stone_brick_wall"), true},
				{vanilla("tuff_brick_wall"), vanilla("tuff_brick_wall"), true}, {vanilla("cobblestone_wall"), Blocks.IRON_BARS, false},
				{vanilla("cobblestone_wall"), Blocks.OAK_FENCE, false}, {Blocks.GLASS_PANE, vanilla("andesite_wall"), false}};
		List<String> wrong = new ArrayList<>();
		for (Object[] pair : pairs) {
			set(helper, FIRST, Blocks.AIR);
			set(helper, NORTH_EAST, Blocks.AIR);
			set(helper, FIRST, (Block) pair[0]);
			set(helper, NORTH_EAST, (Block) pair[1]);
			boolean joined = joinedPair(helper);
			LOGGER.info("{} and {}: {}", describe(helper.getBlockState(FIRST)), describe(helper.getBlockState(NORTH_EAST)), joined);
			if (joined != (boolean) pair[2]) {
				wrong.add(describe(helper.getBlockState(FIRST)) + " / " + describe(helper.getBlockState(NORTH_EAST)) + " should join: " + pair[2]);
			}
		}
		helper.assertTrue(wrong.isEmpty(), "Pairs joined wrongly: " + wrong);
		helper.succeed();
	}

	/** Rotating and mirroring a fence or a wall (structures, the structure block) turn its diagonals with it. */
	@GameTest
	public void rotationAndMirroringTurnTheDiagonals(GameTestHelper helper) {
		for (Block block : List.of(Blocks.OAK_FENCE, DiagonalWalls.of(vanilla("cobblestone_wall")))) {
			BlockState northEast = block.defaultBlockState().setValue(DiagonalConnections.NORTH_EAST, true);
			Object[][] cases = {
					{northEast.rotate(Rotation.CLOCKWISE_90), Diagonal.SOUTH_EAST}, {northEast.rotate(Rotation.CLOCKWISE_180), Diagonal.SOUTH_WEST},
					{northEast.rotate(Rotation.COUNTERCLOCKWISE_90), Diagonal.NORTH_WEST}, {northEast.mirror(Mirror.FRONT_BACK), Diagonal.NORTH_WEST},
					{northEast.mirror(Mirror.LEFT_RIGHT), Diagonal.SOUTH_EAST}};
			for (Object[] check : cases) {
				BlockState state = (BlockState) check[0];
				Diagonal wanted = (Diagonal) check[1];
				helper.assertTrue(DiagonalConnections.mask(state) == 1 << wanted.ordinal(), describe(state) + " should have only " + wanted);
			}
		}
		helper.succeed();
	}

	/**
	 * A player places two cobblestone walls a diagonal step apart: they join, each keeps its post (each is an end), and
	 * the arm is in the outline (low, as a low wall side) and the collision (as high as a wall's). A third wall placed on
	 * along the diagonal makes a straight run: the middle one drops its post, as a straight wall does, and its ends keep
	 * theirs. Breaking an end gives the middle its post back.
	 */
	@GameTest
	public void wallsJoinDiagonallyAndRunWithoutPosts(GameTestHelper helper) {
		floor(helper);
		ServerPlayer player = player(helper);
		place(helper, player, vanilla("cobblestone_wall").asItem(), FIRST);
		place(helper, player, vanilla("cobblestone_wall").asItem(), NORTH_EAST);
		BlockState first = helper.getBlockState(FIRST);
		BlockState other = helper.getBlockState(NORTH_EAST);
		LOGGER.info("Walls placed diagonally: {} and {}", describe(first), describe(other));
		helper.assertTrue(joinedPair(helper), "The walls join diagonally: " + describe(first) + ", " + describe(other));
		helper.assertTrue(first.getBlock() instanceof DiagonalWallBlock && other.getBlock() instanceof DiagonalWallBlock,
				"Joined, both are diagonal walls: " + describe(first) + ", " + describe(other));
		helper.assertTrue(post(helper, FIRST) && post(helper, NORTH_EAST), "Two walls joined diagonally are ends, with posts");

		ServerLevel level = helper.getLevel();
		BlockPos absolute = helper.absolutePos(FIRST);
		VoxelShape outline = first.getShape(level, absolute);
		VoxelShape collision = first.getCollisionShape(level, absolute);
		helper.assertTrue(contains(outline, 0.9, 0.8, 0.1) && !contains(outline, 0.9, 0.9, 0.1) && contains(collision, 0.9, 1.4, 0.1),
				"The arm reaches the north-east corner, low in the outline and wall-high in the collision: " + outline.bounds() + ", "
						+ collision.bounds());
		helper.assertTrue(!contains(outline, 0.1, 0.5, 0.9) && !contains(collision, 0.1, 0.5, 0.1) && !contains(collision, 0.9, 0.5, 0.9),
				"No arm reaches the other corners");

		place(helper, player, vanilla("cobblestone_wall").asItem(), BEYOND);
		BlockState middle = helper.getBlockState(NORTH_EAST);
		LOGGER.info("A diagonal run of walls: {}, {}, {}", describe(helper.getBlockState(FIRST)), describe(middle),
				describe(helper.getBlockState(BEYOND)));
		helper.assertTrue(DiagonalConnections.mask(middle) == (1 << Diagonal.NORTH_EAST.ordinal() | 1 << Diagonal.SOUTH_WEST.ordinal()),
				"The middle wall joins both ways: " + describe(middle));
		helper.assertTrue(!middle.getValue(WallBlock.UP) && post(helper, FIRST) && post(helper, BEYOND),
				"The middle of the run has no post and its ends do: " + describe(middle));

		helper.destroyBlock(BEYOND);
		helper.assertTrue(post(helper, NORTH_EAST) && DiagonalConnections.mask(helper.getBlockState(NORTH_EAST)) == 1 << Diagonal.SOUTH_WEST.ordinal(),
				"With an end broken the middle is an end, with its post: " + describe(helper.getBlockState(NORTH_EAST)));
		helper.destroyBlock(FIRST);
		helper.assertTrue(helper.getBlockState(NORTH_EAST).is(vanilla("cobblestone_wall")),
				"With no diagonal left it is the vanilla wall again: " + describe(helper.getBlockState(NORTH_EAST)));
		helper.succeed();
	}

	/**
	 * A stone over the middle of a diagonal run of walls makes it tall, with no post, as a block over a straight wall
	 * does; a wall above with a post raises its post and leaves it low (its arms are not covered); with nothing above it
	 * is low again with no post. A stone in the corner joins walls straight and keeps them apart, as it does fences.
	 */
	@GameTest
	public void wallPostsFollowWhatIsAbove(GameTestHelper helper) {
		floor(helper);
		set(helper, FIRST, vanilla("cobblestone_wall"));
		set(helper, NORTH_EAST, vanilla("cobblestone_wall"));
		set(helper, BEYOND, vanilla("cobblestone_wall"));
		helper.assertTrue(!post(helper, NORTH_EAST), "The middle of the run has no post: " + describe(helper.getBlockState(NORTH_EAST)));
		set(helper, NORTH_EAST.above(), Blocks.STONE);
		helper.assertTrue(tall(helper, NORTH_EAST) && !post(helper, NORTH_EAST),
				"Stone above makes the middle tall, with no post: " + describe(helper.getBlockState(NORTH_EAST)));
		set(helper, NORTH_EAST.above(), vanilla("andesite_wall"));
		helper.assertTrue(post(helper, NORTH_EAST.above()) && post(helper, NORTH_EAST) && !tall(helper, NORTH_EAST),
				"A wall with a post above raises the post and leaves it low: " + describe(helper.getBlockState(NORTH_EAST)));
		set(helper, NORTH_EAST.above(), Blocks.AIR);
		helper.assertTrue(!post(helper, NORTH_EAST) && !tall(helper, NORTH_EAST),
				"With nothing above it is low with no post: " + describe(helper.getBlockState(NORTH_EAST)));

		set(helper, BEYOND, Blocks.AIR);
		set(helper, NORTH_CORNER, Blocks.STONE);
		BlockState first = helper.getBlockState(FIRST);
		BlockState other = helper.getBlockState(NORTH_EAST);
		helper.assertTrue(first.getValue(WallBlock.NORTH) != WallSide.NONE && other.getValue(WallBlock.WEST) != WallSide.NONE
				&& DiagonalConnections.mask(first) == 0 && DiagonalConnections.mask(other) == 0,
				"With stone in the corner both walls join it and not each other: " + describe(first) + ", " + describe(other));
		helper.succeed();
	}

	/**
	 * A diagonal run of walls two high: the lower walls are tall, their sides and arms rising to meet the walls above, and
	 * the middle has no post, as a tall straight wall has none; its ends keep theirs. The upper walls are low. Its outline
	 * reaches the top of the block along the arm. Taking the upper middle away lowers the lower middle, whose arms are no
	 * longer all covered, and gives it back its post.
	 */
	@GameTest
	public void twoHighDiagonalWallsAreTall(GameTestHelper helper) {
		floor(helper);
		Block mossy = vanilla("mossy_stone_brick_wall");
		for (BlockPos pos : List.of(FIRST, NORTH_EAST, BEYOND)) {
			set(helper, pos, mossy);
			set(helper, pos.above(), mossy);
		}
		LOGGER.info("A diagonal run two high: lower {}, {}, {}; upper {}, {}, {}", describe(helper.getBlockState(FIRST)),
				describe(helper.getBlockState(NORTH_EAST)), describe(helper.getBlockState(BEYOND)), describe(helper.getBlockState(FIRST.above())),
				describe(helper.getBlockState(NORTH_EAST.above())), describe(helper.getBlockState(BEYOND.above())));
		helper.assertTrue(tall(helper, FIRST) && tall(helper, NORTH_EAST) && tall(helper, BEYOND), "The lower walls are tall");
		helper.assertTrue(!post(helper, NORTH_EAST) && post(helper, FIRST) && post(helper, BEYOND),
				"The lower middle has no post and the lower ends have theirs");
		helper.assertTrue(!tall(helper, FIRST.above()) && !tall(helper, NORTH_EAST.above()) && !tall(helper, BEYOND.above()),
				"The upper walls are low");
		BlockState middle = helper.getBlockState(NORTH_EAST);
		VoxelShape outline = middle.getShape(helper.getLevel(), helper.absolutePos(NORTH_EAST));
		helper.assertTrue(contains(outline, 0.9, 0.95, 0.1), "The tall arm reaches the top of the block: " + outline.bounds());

		set(helper, NORTH_EAST.above(), Blocks.AIR);
		helper.assertTrue(!tall(helper, NORTH_EAST) && !post(helper, NORTH_EAST),
				"With the wall above gone the middle is low, a low run with no post: " + describe(helper.getBlockState(NORTH_EAST)));
		helper.assertTrue(!tall(helper, FIRST), "The lower end, its arm no longer under a wall, is low: " + describe(helper.getBlockState(FIRST)));
		helper.succeed();
	}

	/**
	 * Blocks whose shapes and arms are alike share their shapes with arms: two different walls joined the same way have
	 * the same collision shape object, and so do two wooden fences. The arms are worked out once, not per block.
	 */
	@GameTest
	public void alikeShapesAreShared(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		BlockPos pos = helper.absolutePos(FIRST);
		List<String> unshared = new ArrayList<>();
		for (Block[] pair : new Block[][] {{DiagonalWalls.of(vanilla("cobblestone_wall")), DiagonalWalls.of(vanilla("andesite_wall"))},
				{Blocks.OAK_FENCE, Blocks.SPRUCE_FENCE}}) {
			VoxelShape[] shapes = new VoxelShape[2];
			for (int i = 0; i < 2; i++) {
				BlockState state = pair[i].defaultBlockState().setValue(DiagonalConnections.NORTH_EAST, true)
						.setValue(DiagonalConnections.SOUTH_WEST, true);
				shapes[i] = state.getCollisionShape(level, pos);
			}
			if (shapes[0] != shapes[1]) {
				unshared.add(pair[0] + " / " + pair[1]);
			}
		}
		helper.assertTrue(unshared.isEmpty(), "These do not share their shapes: " + unshared);
		helper.succeed();
	}

	/**
	 * A diagonal wall is the vanilla wall to players and to other blocks: a vanilla wall beside it joins it, it is a wall
	 * by tag, it is named, picked and dropped as the vanilla wall. When its last diagonal goes it is the vanilla wall
	 * again, and its sides rise to the block above as a vanilla wall's do.
	 */
	@GameTest
	public void aDiagonalWallIsTheVanillaWallToPlayers(GameTestHelper helper) {
		floor(helper);
		Block cobblestone = vanilla("cobblestone_wall");
		BlockPos south = FIRST.south();
		set(helper, south, cobblestone);
		set(helper, FIRST, cobblestone);
		set(helper, NORTH_EAST, cobblestone);
		BlockState first = helper.getBlockState(FIRST);
		LOGGER.info("A wall joined south and north-east: {}; its southern neighbour: {}", describe(first), describe(helper.getBlockState(south)));
		helper.assertTrue(first.getBlock() instanceof DiagonalWallBlock && DiagonalConnections.mask(first) == 1 << Diagonal.NORTH_EAST.ordinal()
				&& first.getValue(CrossCollisionBlock.SOUTH), "It is a diagonal wall, joined south and north-east: " + describe(first));
		helper.assertTrue(helper.getBlockState(south).getValue(WallBlock.NORTH) != WallSide.NONE,
				"The vanilla wall to its south joins it: " + describe(helper.getBlockState(south)));
		List<ItemStack> drops = Block.getDrops(first, helper.getLevel(), helper.absolutePos(FIRST), null);
		helper.assertTrue(first.is(BlockTags.WALLS) && first.getBlock().asItem() == cobblestone.asItem()
				&& first.getBlock().getName().equals(cobblestone.getName()) && drops.size() == 1 && drops.getFirst().is(cobblestone.asItem()),
				"It is a wall named, picked and dropped as the cobblestone wall: " + drops);

		set(helper, FIRST.above(), Blocks.STONE);
		set(helper, NORTH_EAST, Blocks.AIR);
		BlockState back = helper.getBlockState(FIRST);
		LOGGER.info("With its diagonal gone, under stone: {}", describe(back));
		helper.assertTrue(back.is(cobblestone) && back.getValue(WallBlock.SOUTH) == WallSide.TALL,
				"It is the vanilla wall again, its side tall under the stone: " + describe(back));
		helper.succeed();
	}

	/**
	 * Logs the block states and what they weigh: the game's block states in all, those of fences and bars with diagonals,
	 * of vanilla's walls and of the diagonal walls, and the heap after a full collection with the classes that hold block
	 * states. Nothing is asserted: these are numbers for the feature record.
	 */
	@GameTest
	public void theStatesDiagonalsAddAreCounted(GameTestHelper helper) {
		int withDiagonals = 0;
		int walls = 0;
		int diagonalWalls = 0;
		for (Block block : BuiltInRegistries.BLOCK) {
			int states = block.getStateDefinition().getPossibleStates().size();
			if (block instanceof DiagonalWallBlock) {
				diagonalWalls += states;
			} else if (DiagonalConnections.hasDiagonals(block.defaultBlockState())) {
				withDiagonals += states;
			}
			if (block instanceof WallBlock) {
				walls += states;
			}
		}
		LOGGER.info("Block states: {} in all, {} in fences and bars with diagonals, {} in vanilla's walls, {} in diagonal walls",
				Block.BLOCK_STATE_REGISTRY.size(), withDiagonals, walls, diagonalWalls);
		LOGGER.info("Heap after a full collection: {} MB; {}", heapAfterCollection() / (1024 * 1024), histogram());
		helper.succeed();
	}

	private static long heapAfterCollection() {
		Runtime runtime = Runtime.getRuntime();
		for (int i = 0; i < 3; i++) {
			System.gc();
		}
		return runtime.totalMemory() - runtime.freeMemory();
	}

	/** The class histogram's lines for block states and their caches, and its total (a full collection runs first). */
	private static String histogram() {
		try {
			String text = (String) ManagementFactory.getPlatformMBeanServer().invoke(new ObjectName("com.sun.management:type=DiagnosticCommand"),
					"gcClassHistogram", new Object[] {null}, new String[] {String[].class.getName()});
			List<String> lines = new ArrayList<>();
			for (String line : text.split("\\R")) {
				String trimmed = line.trim();
				if (trimmed.endsWith(".level.block.state.BlockState") || trimmed.endsWith("BlockStateBase$Cache") || trimmed.startsWith("Total")) {
					lines.add(trimmed.replaceAll("\\s+", " "));
				}
			}
			return String.join("; ", lines);
		} catch (Exception exception) {
			return "no class histogram: " + exception;
		}
	}

	/**
	 * Every fence and bars block has the four properties and starts with them false. Walls keep their own states, without
	 * the properties, and each (vanilla's and Jugcraft's) has a diagonal wall that starts with none and is a wall by tag.
	 * Every block in the tag is a fence, bars block or wall (vanilla's 14 fences, 17 panes, 9 bars and 32 walls,
	 * Jugcraft's 15 fences, the cedar's since the tree roster's batch 1, and 1 wall: 88).
	 */
	@GameTest
	public void everyFenceBarsBlockAndWallHasDiagonals(GameTestHelper helper) {
		List<String> problems = new ArrayList<>();
		int tagged = 0;
		for (Block block : BuiltInRegistries.BLOCK) {
			BlockState state = block.defaultBlockState();
			boolean crossing = block instanceof FenceBlock || block instanceof IronBarsBlock;
			if (crossing && (!DiagonalConnections.hasDiagonals(state) || DiagonalConnections.mask(state) != 0)) {
				problems.add(BuiltInRegistries.BLOCK.getKey(block) + " lacks diagonals or starts joined");
			}
			// Every wall, vanilla's and Jugcraft's own (the bastion concrete wall, batch 55), has a diagonal wall.
			if (block instanceof WallBlock && (DiagonalConnections.hasDiagonals(state) || DiagonalWalls.of(block) == null)) {
				problems.add(BuiltInRegistries.BLOCK.getKey(block) + " has diagonal properties or no diagonal wall");
			}
			if (block instanceof DiagonalWallBlock && (DiagonalConnections.mask(state) != 0 || !state.is(BlockTags.WALLS))) {
				problems.add(BuiltInRegistries.BLOCK.getKey(block) + " starts joined or is not a wall by tag");
			}
			if (state.is(DiagonalConnections.TAG)) {
				tagged++;
				if (!crossing && !(block instanceof WallBlock)) {
					problems.add(BuiltInRegistries.BLOCK.getKey(block) + " is tagged but is no fence, bars or wall");
				}
			}
		}
		LOGGER.info("{} blocks join diagonally, {} diagonal walls; problems: {}", tagged, DiagonalWalls.all().size(), problems);
		helper.assertTrue(problems.isEmpty(), "Problems: " + problems);
		helper.assertTrue(tagged == 88 && DiagonalWalls.all().size() == 33, "88 blocks join diagonally and 33 walls have diagonal walls, not "
				+ tagged + " and " + DiagonalWalls.all().size());
		helper.succeed();
	}
}
