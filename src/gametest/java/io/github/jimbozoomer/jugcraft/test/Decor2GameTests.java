package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.agriculture.CarvedPumpkinBlock;
import io.github.jimbozoomer.jugcraft.agriculture.CarvedPumpkinBlockEntity;
import io.github.jimbozoomer.jugcraft.agriculture.CarvingFace;
import io.github.jimbozoomer.jugcraft.agriculture.CarvingTemplates;
import io.github.jimbozoomer.jugcraft.agriculture.FloatingCandleBlock;
import io.github.jimbozoomer.jugcraft.agriculture.FloatingCandleBlockEntity;
import io.github.jimbozoomer.jugcraft.agriculture.GiantPumpkinBlock;
import io.github.jimbozoomer.jugcraft.agriculture.GiantPumpkinBlockEntity;
import io.github.jimbozoomer.jugcraft.agriculture.GiantPumpkinVineBlock;
import io.github.jimbozoomer.jugcraft.agriculture.JugcraftAgriculture;
import io.github.jimbozoomer.jugcraft.agriculture.LuminariaBlock;
import io.github.jimbozoomer.jugcraft.agriculture.PumpkinCarving;
import io.github.jimbozoomer.jugcraft.agriculture.SkeletonHandSconceBlock;
import io.github.jimbozoomer.jugcraft.agriculture.StringLightHookBlock;
import io.github.jimbozoomer.jugcraft.agriculture.StringLightHookBlockEntity;
import java.util.List;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.BlockItemStateProperties;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * In-game tests for the second five Halloween decorations: the Luminaria (lit and snuffed like a candle, dyed, keeps
 * its colour when broken, falls without a floor), Floating Candles (hang in the air, add up to four, light by the
 * candle, drop each candle), the Skeleton Hand Sconce (walls only, burning, snuffed and relit, falls with its wall),
 * Soul-Flame Carvings (a soul torch lights carved and giant pumpkins blue, at a soul torch's light, and comes back out;
 * kept when broken and saved), Bat Bunting (strung between hooks like the string lights, kept, dropped as itself),
 * and that their data loads.
 */
public class Decor2GameTests {
	private static Item item(String id) {
		return JugcraftAgriculture.item(id);
	}

	private static Block block(String id) {
		return JugcraftAgriculture.block(id);
	}

	private static void floor(GameTestHelper helper) {
		for (int x = 0; x <= 7; x++) {
			for (int z = 0; z <= 7; z++) {
				helper.setBlock(new BlockPos(x, 1, z), Blocks.STONE);
			}
		}
	}

	private static ServerPlayer player(GameTestHelper helper, BlockPos standAt, ItemStack held) {
		ServerPlayer player = helper.makeMockServerPlayerInLevel();
		player.setGameMode(GameType.SURVIVAL);
		BlockPos absolute = helper.absolutePos(standAt);
		player.setPos(absolute.getX() + 0.5, absolute.getY(), absolute.getZ() + 0.5);
		player.setItemInHand(InteractionHand.MAIN_HAND, held);
		return player;
	}

	private static BlockHitResult hit(GameTestHelper helper, BlockPos pos, Direction side) {
		BlockPos absolute = helper.absolutePos(pos);
		return new BlockHitResult(Vec3.atCenterOf(absolute).relative(side, 0.5), side, absolute, false);
	}

	private static InteractionResult use(GameTestHelper helper, ServerPlayer player, BlockPos pos, Direction side) {
		return player.gameMode.useItemOn(player, helper.getLevel(), player.getMainHandItem(), InteractionHand.MAIN_HAND, hit(helper, pos, side));
	}

	/** Places the held item against {@code side} of the block at {@code pos}, as a player's right click does. */
	private static InteractionResult place(GameTestHelper helper, ServerPlayer player, BlockPos pos, Direction side) {
		return player.getMainHandItem().useOn(new UseOnContext(player, InteractionHand.MAIN_HAND, hit(helper, pos, side)));
	}

	private static List<ItemEntity> drops(GameTestHelper helper, Item item) {
		AABB area = new AABB(helper.absolutePos(BlockPos.ZERO)).expandTowards(8, 8, 8).inflate(2.0);
		return helper.getLevel().getEntitiesOfClass(ItemEntity.class, area, entity -> entity.getItem().is(item));
	}

	private static int dropped(GameTestHelper helper, Item item) {
		return drops(helper, item).stream().mapToInt(entity -> entity.getItem().getCount()).sum();
	}

	// ---------------------------------------------------------------- the luminaria

	/**
	 * Placed on the floor it is a white paper bag, dark. Flint and steel lights it (light 10, wearing the flint);
	 * an empty hand snuffs it; a dye colours it (one dye used) and the same dye again does nothing. It falls when its
	 * floor goes, dropping a bag that keeps its colour, and that bag placed again is the same colour.
	 */
	@GameTest
	public void theLuminariaLightsAndTakesADye(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		floor(helper);
		BlockPos stand = new BlockPos(3, 1, 3);
		BlockPos pos = stand.above();
		ServerPlayer player = player(helper, new BlockPos(3, 2, 6), new ItemStack(item("luminaria")));
		place(helper, player, stand, Direction.UP);
		BlockState bag = helper.getBlockState(pos);
		helper.assertTrue(bag.is(block("luminaria")) && bag.getValue(LuminariaBlock.COLOR) == DyeColor.WHITE && !bag.getValue(LuminariaBlock.LIT)
				&& bag.getLightEmission() == 0, "A new bag is white and dark: " + bag);

		player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.FLINT_AND_STEEL));
		helper.assertTrue(use(helper, player, pos, Direction.UP).consumesAction(), "Flint and steel lights it");
		helper.assertTrue(helper.getBlockState(pos).getValue(LuminariaBlock.LIT) && helper.getBlockState(pos).getLightEmission() == LuminariaBlock.LIGHT,
				"Lit, it gives light " + LuminariaBlock.LIGHT);
		helper.assertTrue(player.getMainHandItem().getDamageValue() == 1, "Lighting wears the flint and steel");
		player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
		use(helper, player, pos, Direction.UP);
		helper.assertTrue(!helper.getBlockState(pos).getValue(LuminariaBlock.LIT), "An empty hand snuffs it");

		player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(BuiltInRegistries.ITEM.getValue(Identifier.withDefaultNamespace("orange_dye")), 2));
		use(helper, player, pos, Direction.UP);
		helper.assertTrue(helper.getBlockState(pos).getValue(LuminariaBlock.COLOR) == DyeColor.ORANGE && player.getMainHandItem().getCount() == 1,
				"Orange dye colours it orange, using one dye");
		use(helper, player, pos, Direction.UP);
		helper.assertTrue(player.getMainHandItem().getCount() == 1, "The same dye again does nothing");

		level.destroyBlock(helper.absolutePos(stand), false);
		helper.assertBlockNotPresent(block("luminaria"), pos);
		List<ItemEntity> dropped = drops(helper, item("luminaria"));
		helper.assertTrue(dropped.size() == 1, "It falls with its floor and drops once");
		ItemStack drop = dropped.get(0).getItem().copy();
		BlockItemStateProperties state = drop.get(DataComponents.BLOCK_STATE);
		helper.assertTrue(state != null && state.apply(block("luminaria").defaultBlockState()).getValue(LuminariaBlock.COLOR) == DyeColor.ORANGE,
				"The dropped bag keeps its colour: " + state);
		BlockPos other = new BlockPos(5, 1, 5);
		player.setItemInHand(InteractionHand.MAIN_HAND, drop);
		place(helper, player, other, Direction.UP);
		helper.assertTrue(helper.getBlockState(other.above()).is(block("luminaria"))
				&& helper.getBlockState(other.above()).getValue(LuminariaBlock.COLOR) == DyeColor.ORANGE, "Placed again, it is still orange");
		helper.succeed();
	}

	// ---------------------------------------------------------------- floating candles

	/**
	 * A Floating Candle stays where it is placed in the air, through block updates; more go in up to four; lit, they
	 * give 3 light a candle; an empty hand snuffs them all; broken, they drop one item a candle. Their bob stays
	 * within a pixel.
	 */
	@GameTest
	public void floatingCandlesHangInTheAir(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		floor(helper);
		BlockPos post = new BlockPos(3, 2, 3);
		helper.setBlock(post, Blocks.OAK_FENCE);
		BlockPos pos = post.east();
		ServerPlayer player = player(helper, new BlockPos(3, 2, 6), new ItemStack(item("floating_candle"), 6));
		place(helper, player, post, Direction.EAST);
		helper.assertTrue(helper.getBlockState(pos).is(block("floating_candle")), "It is placed beside the post, in the air");
		helper.assertTrue(helper.getBlockEntity(pos, FloatingCandleBlockEntity.class) != null, "with its block entity for drawing");
		helper.setBlock(post, Blocks.AIR);
		helper.assertTrue(helper.getBlockState(pos).is(block("floating_candle")), "It floats on when the post is gone");

		for (int i = 0; i < FloatingCandleBlock.MAX; i++) {
			place(helper, player, pos, Direction.UP);
		}
		helper.assertTrue(helper.getBlockState(pos).getValue(FloatingCandleBlock.CANDLES) == FloatingCandleBlock.MAX,
				"More candles join it, up to " + FloatingCandleBlock.MAX + ": " + helper.getBlockState(pos));
		helper.assertTrue(player.getMainHandItem().getCount() == 6 - FloatingCandleBlock.MAX - 1,
				"Four candles in the block, and the fifth went above it");

		player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.FIRE_CHARGE, 2));
		use(helper, player, pos, Direction.UP);
		BlockState lit = helper.getBlockState(pos);
		helper.assertTrue(lit.getValue(FloatingCandleBlock.LIT) && lit.getLightEmission() == FloatingCandleBlock.LIGHT_PER_CANDLE * FloatingCandleBlock.MAX,
				"A fire charge lights them, " + FloatingCandleBlock.LIGHT_PER_CANDLE + " light a candle: " + lit.getLightEmission());
		helper.assertTrue(player.getMainHandItem().getCount() == 1, "using up the fire charge");
		player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
		use(helper, player, pos, Direction.UP);
		helper.assertTrue(!helper.getBlockState(pos).getValue(FloatingCandleBlock.LIT), "An empty hand snuffs them");

		for (long time = 0; time < FloatingCandleBlock.BOB_TICKS; time += 7) {
			for (int i = 0; i < FloatingCandleBlock.MAX; i++) {
				float bob = FloatingCandleBlock.bob(helper.absolutePos(pos), i, time, 0.5F);
				helper.assertTrue(Math.abs(bob) <= FloatingCandleBlock.BOB / 16.0F + 1.0E-6F, "A candle bobs at most a pixel: " + bob);
			}
		}

		level.destroyBlock(helper.absolutePos(pos), true);
		helper.succeedWhen(() -> helper.assertTrue(dropped(helper, item("floating_candle")) == FloatingCandleBlock.MAX,
				"Breaking them drops each candle: " + dropped(helper, item("floating_candle"))));
	}

	// ---------------------------------------------------------------- the skeleton hand sconce

	/**
	 * The sconce will not stand on an open floor; on the side of a block it reaches out, burning at light 14. An empty
	 * hand snuffs it, flint and steel lights it again, and it falls (dropping) when its wall goes.
	 */
	@GameTest
	public void theSconceHangsOnAWallAndBurns(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		floor(helper);
		ServerPlayer player = player(helper, new BlockPos(3, 2, 0), new ItemStack(item("skeleton_hand_sconce"), 2));
		place(helper, player, new BlockPos(1, 1, 6), Direction.UP);
		helper.assertBlockNotPresent(block("skeleton_hand_sconce"), new BlockPos(1, 2, 6));
		helper.assertTrue(player.getMainHandItem().getCount() == 2, "It will not stand on an open floor");

		BlockPos wall = new BlockPos(3, 2, 4);
		helper.setBlock(wall, Blocks.STONE_BRICKS);
		BlockPos pos = wall.north();
		place(helper, player, wall, Direction.NORTH);
		BlockState sconce = helper.getBlockState(pos);
		helper.assertTrue(sconce.is(block("skeleton_hand_sconce")) && sconce.getValue(SkeletonHandSconceBlock.FACING) == Direction.NORTH
				&& sconce.getValue(SkeletonHandSconceBlock.LIT) && sconce.getLightEmission() == SkeletonHandSconceBlock.LIGHT,
				"On the wall it reaches out, burning at light " + SkeletonHandSconceBlock.LIGHT + ": " + sconce);

		player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
		use(helper, player, pos, Direction.NORTH);
		helper.assertTrue(!helper.getBlockState(pos).getValue(SkeletonHandSconceBlock.LIT) && helper.getBlockState(pos).getLightEmission() == 0,
				"An empty hand snuffs it");
		player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.FLINT_AND_STEEL));
		use(helper, player, pos, Direction.NORTH);
		helper.assertTrue(helper.getBlockState(pos).getValue(SkeletonHandSconceBlock.LIT), "Flint and steel lights it again");

		level.destroyBlock(helper.absolutePos(wall), false);
		helper.succeedWhen(() -> {
			helper.assertBlockNotPresent(block("skeleton_hand_sconce"), pos);
			helper.assertTrue(dropped(helper, item("skeleton_hand_sconce")) == 1, "It falls with its wall");
		});
	}

	// ---------------------------------------------------------------- soul-flame carvings

	/**
	 * A soul torch lights a carved pumpkin blue: lit, soul, at most a soul torch's light (10) where a torch gives the
	 * carving's whole glow. An empty hand gives the soul torch back. Broken while soul-lit, it drops with its soul
	 * flame, and placed again it is still soul-lit.
	 */
	@GameTest
	public void aSoulTorchLightsACarvingBlue(GameTestHelper helper) {
		floor(helper);
		BlockPos pos = new BlockPos(3, 2, 3);
		helper.setBlock(pos, block("hand_carved_pumpkin").defaultBlockState().setValue(CarvedPumpkinBlock.FACING, Direction.NORTH));
		PumpkinCarving carving = PumpkinCarving.BLANK.withFace(0, CarvingTemplates.ALL.get(0).face());
		helper.getBlockEntity(pos, CarvedPumpkinBlockEntity.class).setCarving(carving, null);
		helper.setBlock(pos, helper.getBlockState(pos).setValue(CarvedPumpkinBlock.GLOW, carving.glow()));
		int glow = carving.glow();
		helper.assertTrue(glow > CarvedPumpkinBlock.SOUL_LIGHT, "The test carving should glow brighter than a soul torch: " + glow);

		ServerPlayer player = player(helper, new BlockPos(3, 2, 1), new ItemStack(Items.SOUL_TORCH, 2));
		use(helper, player, pos, Direction.NORTH);
		BlockState soul = helper.getBlockState(pos);
		helper.assertTrue(soul.getValue(CarvedPumpkinBlock.LIT) && soul.getValue(CarvedPumpkinBlock.SOUL)
				&& soul.getLightEmission() == CarvedPumpkinBlock.SOUL_LIGHT, "A soul torch lights it blue at light " + CarvedPumpkinBlock.SOUL_LIGHT + ": " + soul);
		helper.assertTrue(player.getMainHandItem().getCount() == 1, "using one soul torch");

		player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
		use(helper, player, pos, Direction.NORTH);
		helper.assertTrue(!helper.getBlockState(pos).getValue(CarvedPumpkinBlock.LIT) && !helper.getBlockState(pos).getValue(CarvedPumpkinBlock.SOUL),
				"An empty hand takes it out");
		helper.assertTrue(player.getInventory().countItem(Items.SOUL_TORCH) == 1 && player.getInventory().countItem(Items.TORCH) == 0,
				"and the soul torch comes back, not a torch");

		player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.TORCH));
		use(helper, player, pos, Direction.NORTH);
		BlockState candle = helper.getBlockState(pos);
		helper.assertTrue(candle.getValue(CarvedPumpkinBlock.LIT) && !candle.getValue(CarvedPumpkinBlock.SOUL) && candle.getLightEmission() == glow,
				"A torch still gives the carving's whole glow: " + candle);

		helper.setBlock(pos, helper.getBlockState(pos).setValue(CarvedPumpkinBlock.SOUL, true));
		CarvedPumpkinBlockEntity pumpkin = helper.getBlockEntity(pos, CarvedPumpkinBlockEntity.class);
		List<ItemStack> drops = Block.getDrops(helper.getBlockState(pos), helper.getLevel(), helper.absolutePos(pos), pumpkin);
		BlockItemStateProperties state = drops.size() == 1 ? drops.get(0).get(DataComponents.BLOCK_STATE) : null;
		helper.assertTrue(state != null && state.apply(block("hand_carved_pumpkin").defaultBlockState()).getValue(CarvedPumpkinBlock.SOUL),
				"Broken, it keeps its soul flame: " + drops);
		BlockPos other = new BlockPos(6, 1, 6);
		player.setItemInHand(InteractionHand.MAIN_HAND, drops.get(0));
		place(helper, player, other, Direction.UP);
		BlockState placed = helper.getBlockState(other.above());
		helper.assertTrue(placed.is(block("hand_carved_pumpkin")) && placed.getValue(CarvedPumpkinBlock.SOUL) && placed.getLightEmission() == CarvedPumpkinBlock.SOUL_LIGHT,
				"Placed again, it is still soul-lit: " + placed);
		helper.succeed();
	}

	/**
	 * A soul torch lights a carved, full-grown giant pumpkin blue: every block gives at most a soul torch's light;
	 * its soul flame survives a save and load; an empty hand gives the soul torch back.
	 */
	@GameTest
	public void aSoulTorchLightsAGiantPumpkinBlue(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		BlockPos vinePos = new BlockPos(1, 2, 2);
		helper.setBlock(vinePos.below(), Blocks.FARMLAND.defaultBlockState().setValue(BlockStateProperties.MOISTURE, 7));
		for (int x = 2; x <= 4; x++) {
			for (int z = 1; z <= 3; z++) {
				helper.setBlock(new BlockPos(x, 1, z), Blocks.GRASS_BLOCK);
			}
		}
		GiantPumpkinVineBlock vine = (GiantPumpkinVineBlock) block("giant_pumpkin_vine");
		helper.setBlock(vinePos, vine.defaultBlockState().setValue(GiantPumpkinVineBlock.AGE, GiantPumpkinVineBlock.MAX_AGE));
		helper.assertTrue(vine.growFruit(level, helper.absolutePos(vinePos), Direction.EAST), "A grown vine should set a fruit");
		helper.getBlockEntity(vinePos.east(), GiantPumpkinBlockEntity.class).feed(level, GiantPumpkinBlockEntity.GROW_TO_TWO, level.getRandom());
		GiantPumpkinBlockEntity giant = GiantPumpkinBlock.master(level, helper.absolutePos(vinePos.east()), helper.getBlockState(vinePos.east()));
		giant.feed(level, GiantPumpkinBlockEntity.GROW_TO_THREE - GiantPumpkinBlockEntity.GROW_TO_TWO, level.getRandom());
		giant = GiantPumpkinBlock.master(level, helper.absolutePos(vinePos.east()), helper.getBlockState(vinePos.east()));
		helper.assertTrue(giant != null && giant.fullGrown(), "The pumpkin should be full grown");
		giant.setFace(Direction.SOUTH, CarvingFace.scale(CarvingTemplates.ALL.get(0).face(), GiantPumpkinBlockEntity.FACE_SIZE), null);
		int glow = giant.glow();

		// The middle of its south side, worked out from the master block (absolute positions throughout).
		BlockPos side = giant.getBlockPos().offset(1, 1, 2);
		ServerPlayer player = helper.makeMockServerPlayerInLevel();
		player.setGameMode(GameType.SURVIVAL);
		player.setPos(side.getX() + 0.5, side.getY(), side.getZ() + 2.5);
		player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.SOUL_TORCH));
		BlockHitResult face = new BlockHitResult(Vec3.atCenterOf(side).relative(Direction.SOUTH, 0.5), Direction.SOUTH, side, false);
		helper.assertTrue(level.getBlockState(side).is(block("giant_pumpkin")), "The south side is part of the pumpkin: " + level.getBlockState(side));
		player.gameMode.useItemOn(player, level, player.getMainHandItem(), InteractionHand.MAIN_HAND, face);
		int expected = Math.min(glow, CarvedPumpkinBlock.SOUL_LIGHT);
		helper.assertTrue(giant.lit() && giant.soul(), "A soul torch lights it with a soul flame");
		for (BlockPos part : BlockPos.betweenClosed(giant.getBlockPos(), giant.getBlockPos().offset(2, 2, 2))) {
			helper.assertTrue(level.getBlockState(part).getLightEmission() == expected,
					"Every block gives " + expected + " (the carving's " + glow + ", at most a soul torch's): " + level.getBlockState(part));
		}

		CompoundTag saved = giant.saveWithoutMetadata(level.registryAccess());
		// World saves add this reserved field after the custom data. It must not
		// overwrite the pumpkin's persistent contest identity.
		saved.putString("id", "jugcraft:giant_pumpkin");
		GiantPumpkinBlockEntity copy = new GiantPumpkinBlockEntity(giant.getBlockPos(), giant.getBlockState());
		copy.loadWithComponents(TagValueInput.create(ProblemReporter.DISCARDING, level.registryAccess(), saved));
		helper.assertTrue(copy.lit() && copy.soul(), "Its soul flame is saved");
		helper.assertTrue(copy.id().equals(giant.id()), "World metadata must preserve the pumpkin's identity");
		CompoundTag legacy = saved.copy();
		legacy.remove("pumpkin_id");
		legacy.put("id", UUIDUtil.CODEC.encodeStart(NbtOps.INSTANCE, giant.id()).getOrThrow());
		GiantPumpkinBlockEntity old = new GiantPumpkinBlockEntity(giant.getBlockPos(), giant.getBlockState());
		old.loadWithComponents(TagValueInput.create(ProblemReporter.DISCARDING, level.registryAccess(), legacy));
		helper.assertTrue(old.id().equals(giant.id()) && old.soul(), "A legacy UUID tag must still load");
		legacy.putString("id", "jugcraft:giant_pumpkin");
		GiantPumpkinBlockEntity missing = new GiantPumpkinBlockEntity(giant.getBlockPos(), giant.getBlockState());
		missing.loadWithComponents(TagValueInput.create(ProblemReporter.DISCARDING, level.registryAccess(), legacy));
		helper.assertTrue(missing.id() != null && missing.soul(), "Older world metadata must retain other pumpkin state");

		player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
		player.gameMode.useItemOn(player, level, player.getMainHandItem(), InteractionHand.MAIN_HAND, face);
		helper.assertTrue(!giant.lit() && !giant.soul() && player.getInventory().countItem(Items.SOUL_TORCH) == 1,
				"An empty hand gives the soul torch back");
		helper.assertTrue(level.getBlockState(giant.getBlockPos()).getLightEmission() == 0, "and it goes dark");
		helper.succeed();
	}

	// ---------------------------------------------------------------- bat bunting

	/**
	 * Bat Bunting is strung between String Light Hooks by the same rules as the string lights (one used), the hook
	 * remembers it is bunting through a save and load, and taking it down or breaking the hook drops bunting, not
	 * lights; a hook can hold lights while bunting runs to it.
	 */
	@GameTest
	public void batBuntingIsStrungBetweenHooks(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		floor(helper);
		BlockPos a = new BlockPos(1, 2, 1);
		BlockPos b = new BlockPos(6, 2, 1);
		BlockPos c = new BlockPos(6, 2, 6);
		BlockState hook = block("string_light_hook").defaultBlockState().setValue(StringLightHookBlock.FACE, AttachFace.FLOOR);
		for (BlockPos pos : List.of(a, b, c)) {
			helper.setBlock(pos, hook);
		}
		ServerPlayer player = player(helper, new BlockPos(3, 2, 3), new ItemStack(item("bat_bunting"), 2));
		use(helper, player, a, Direction.UP);
		use(helper, player, b, Direction.UP);
		StringLightHookBlockEntity first = helper.getBlockEntity(a, StringLightHookBlockEntity.class);
		helper.assertTrue(helper.absolutePos(b).equals(first.link()) && first.strand() == StringLightHookBlockEntity.Strand.BUNTING,
				"Bunting runs from the first hook to the second");
		helper.assertTrue(player.getMainHandItem().getCount() == 1, "using one bunting");

		player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(item("jack_o_lantern_string_lights")));
		use(helper, player, b, Direction.UP);
		use(helper, player, c, Direction.UP);
		StringLightHookBlockEntity second = helper.getBlockEntity(b, StringLightHookBlockEntity.class);
		helper.assertTrue(helper.absolutePos(c).equals(second.link()) && second.strand() == StringLightHookBlockEntity.Strand.LIGHTS,
				"The second hook holds string lights on to the third");

		CompoundTag saved = first.saveWithoutMetadata(level.registryAccess());
		StringLightHookBlockEntity copy = new StringLightHookBlockEntity(first.getBlockPos(), first.getBlockState());
		copy.loadWithComponents(TagValueInput.create(ProblemReporter.DISCARDING, level.registryAccess(), saved));
		helper.assertTrue(copy.strand() == StringLightHookBlockEntity.Strand.BUNTING && first.link().equals(copy.link()), "It stays bunting when saved");

		player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
		player.setShiftKeyDown(true);
		use(helper, player, a, Direction.UP);
		helper.assertTrue(first.link() == null && dropped(helper, item("bat_bunting")) == 1 && dropped(helper, item("jack_o_lantern_string_lights")) == 0,
				"Taken down, it drops bunting");
		player.setShiftKeyDown(false);

		player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(item("bat_bunting")));
		use(helper, player, c, Direction.UP);
		use(helper, player, a, Direction.UP);
		helper.assertTrue(helper.getBlockEntity(c, StringLightHookBlockEntity.class).strand() == StringLightHookBlockEntity.Strand.BUNTING,
				"A hook with lights running to it holds bunting of its own");
		level.destroyBlock(helper.absolutePos(c), true);
		helper.succeedWhen(() -> helper.assertTrue(dropped(helper, item("bat_bunting")) == 2,
				"Breaking a hook drops its bunting: " + dropped(helper, item("bat_bunting"))));
	}

	/** The recipes, loot tables and tool tag load. */
	@GameTest
	public void decorations2DataLoads(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		for (String id : List.of("luminaria", "floating_candle", "skeleton_hand_sconce", "bat_bunting")) {
			helper.assertTrue(level.recipeAccess().byKey(ResourceKey.create(Registries.RECIPE, Jugcraft.id(id))).isPresent(), "Recipe " + id + " loads");
		}
		for (String id : List.of("luminaria", "floating_candle", "skeleton_hand_sconce")) {
			LootTable table = level.getServer().reloadableRegistries().getLootTable(ResourceKey.create(Registries.LOOT_TABLE, Jugcraft.id("blocks/" + id)));
			helper.assertTrue(table != LootTable.EMPTY, "The " + id + " drops itself");
		}
		helper.assertTrue(block("skeleton_hand_sconce").defaultBlockState().is(BlockTags.MINEABLE_WITH_PICKAXE), "A pickaxe fits the sconce");
		helper.succeed();
	}
}
