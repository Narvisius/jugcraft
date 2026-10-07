package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.agriculture.HangingPlantBlock;
import io.github.jimbozoomer.jugcraft.agriculture.JugcraftAgriculture;
import io.github.jimbozoomer.jugcraft.agriculture.Mandrakes;
import io.github.jimbozoomer.jugcraft.agriculture.WildGrassBlock;
import java.util.List;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.block.FlowerPotBlock;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.MultifaceBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * In-game tests for the graveyard flora: every plant is registered with its item, its flowers are small or tall flowers
 * with potted forms and dyes, the glowing ghost pipe gives light, and each stands where it should; withered grass and the
 * ghost fern drop only to shears and grow tall with bone meal; shroud moss hangs only from leaves or a sturdy underside,
 * bone meal lengthens it and it falls when what holds it goes; creeping ivy covers faces, shears take one for each face
 * and bone meal spreads it; the mandrake grows from its root, screams when pulled up ripe (sickening the bare-headed near
 * it, not those with something on their heads, and earning Mind Your Ears for a covered puller), not when unripe, nor a
 * wild one taken with shears; the mandrake root makes Flying Ointment; grave vases take the flora's colours; and the data
 * (loot, features, patches, the advancement) loads.
 */
public class GraveyardFloraGameTests {
	private static final TagKey<Item> SMALL_FLOWERS = TagKey.create(Registries.ITEM, Identifier.withDefaultNamespace("small_flowers"));
	private static final TagKey<Item> FLOWER_TAG = TagKey.create(Registries.ITEM, Identifier.withDefaultNamespace("flowers"));
	private static final List<String> FLOWERS = List.of("spider_lily", "snowdrop", "deadly_nightshade", "bleeding_heart", "ghost_pipe");
	private static final List<String> TALL_FLOWERS = List.of("black_rose", "foxglove", "funeral_lily", "asphodel");
	private static final List<String> PLANTS = List.of("spider_lily", "snowdrop", "deadly_nightshade", "bleeding_heart", "ghost_pipe",
			"black_rose", "foxglove", "funeral_lily", "asphodel", "withered_grass", "tall_withered_grass", "ghost_fern", "large_ghost_fern",
			"dead_mans_fingers", "grave_moss", "shroud_moss", "creeping_ivy");

	private static Block block(String id) {
		return JugcraftAgriculture.block(id);
	}

	private static Item item(String id) {
		return JugcraftAgriculture.item(id);
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

	private static boolean earned(ServerPlayer player, String id) {
		AdvancementHolder advancement = player.level().getServer().getAdvancements().get(Jugcraft.id(id));
		return advancement != null && player.getAdvancements().getOrStartProgress(advancement).isDone();
	}

	private static List<ItemStack> drops(GameTestHelper helper, BlockPos pos, ItemStack tool, ServerPlayer player) {
		ServerLevel level = helper.getLevel();
		BlockPos absolute = helper.absolutePos(pos);
		return Block.getDrops(level.getBlockState(absolute), level, absolute, null, player, tool);
	}

	private static int count(List<ItemStack> stacks, Item item) {
		return stacks.stream().filter(s -> s.is(item)).mapToInt(ItemStack::getCount).sum();
	}

	private static void floor(GameTestHelper helper, Block ground) {
		for (int x = 0; x < 8; x++) {
			for (int z = 0; z < 8; z++) {
				helper.setBlock(new BlockPos(x, 1, z), ground);
			}
		}
	}

	/**
	 * Every plant has its block and item; the flowers are small flowers with potted forms and a dye each (tall ones two),
	 * the tall flowers are flowers; the ghost pipe glows; and each stands on grass (dead man's fingers on stone too).
	 */
	@GameTest
	public void floraIsRegisteredAndStands(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		floor(helper, Blocks.GRASS_BLOCK);
		for (String plant : PLANTS) {
			helper.assertTrue(item(plant) instanceof BlockItem held && held.getBlock() == block(plant), plant + " has its own item");
		}
		for (String flower : FLOWERS) {
			helper.assertTrue(new ItemStack(item(flower)).is(SMALL_FLOWERS), flower + " is a small flower");
			helper.assertTrue(block("potted_" + flower) instanceof FlowerPotBlock pot && pot.getPotted() == block(flower), flower + " can be potted");
		}
		for (String flower : TALL_FLOWERS) {
			helper.assertTrue(new ItemStack(item(flower)).is(FLOWER_TAG), flower + " is a flower");
		}
		for (String recipe : List.of("red_dye_from_spider_lily", "black_dye_from_black_rose", "magenta_dye_from_foxglove", "purple_dye_from_deadly_nightshade")) {
			helper.assertTrue(level.getServer().getRecipeManager().byKey(ResourceKey.create(Registries.RECIPE, Jugcraft.id(recipe))).isPresent(),
					recipe + " loads");
		}
		helper.assertTrue(block("ghost_pipe").defaultBlockState().getLightEmission() == 6, "The ghost pipe glows");
		BlockPos at = new BlockPos(1, 2, 1);
		for (String plant : List.of("spider_lily", "snowdrop", "deadly_nightshade", "bleeding_heart", "ghost_pipe", "withered_grass", "ghost_fern",
				"dead_mans_fingers", "grave_moss")) {
			helper.assertTrue(block(plant).defaultBlockState().canSurvive(level, helper.absolutePos(at)), plant + " stands on grass");
		}
		helper.setBlock(new BlockPos(5, 1, 5), Blocks.STONE);
		helper.assertTrue(block("dead_mans_fingers").defaultBlockState().canSurvive(level, helper.absolutePos(new BlockPos(5, 2, 5))),
				"Dead man's fingers stand on stone");
		helper.succeed();
	}

	/** Withered grass and the ghost fern: nothing by hand, themselves to shears; bone meal grows each into its tall form. */
	@GameTest
	public void grassesGrowTallAndNeedShears(GameTestHelper helper) {
		floor(helper, Blocks.GRASS_BLOCK);
		ServerPlayer keeper = player(helper, new BlockPos(0, 2, 0), new ItemStack(Items.BONE_MEAL, 8));
		for (String[] pair : new String[][] {{"withered_grass", "tall_withered_grass"}, {"ghost_fern", "large_ghost_fern"}}) {
			BlockPos pos = pair[0].equals("withered_grass") ? new BlockPos(2, 2, 2) : new BlockPos(5, 2, 5);
			helper.setBlock(pos, block(pair[0]));
			helper.assertTrue(((WildGrassBlock) block(pair[0])).tall() == block(pair[1]), pair[0] + " grows into " + pair[1]);
			helper.assertTrue(count(drops(helper, pos, ItemStack.EMPTY, keeper), item(pair[0])) == 0, pair[0] + " drops nothing by hand");
			helper.assertTrue(count(drops(helper, pos, new ItemStack(Items.SHEARS), keeper), item(pair[0])) == 1, "Shears take " + pair[0]);
			keeper.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.BONE_MEAL, 8));
			helper.useBlock(pos, keeper, hit(helper, pos, Direction.UP));
			BlockState lower = helper.getBlockState(pos);
			BlockState upper = helper.getBlockState(pos.above());
			helper.assertTrue(lower.is(block(pair[1])) && lower.getValue(DoublePlantBlock.HALF) == DoubleBlockHalf.LOWER
					&& upper.is(block(pair[1])) && upper.getValue(DoublePlantBlock.HALF) == DoubleBlockHalf.UPPER, "Bone meal grows " + pair[1]);
			helper.assertTrue(count(drops(helper, pos, new ItemStack(Items.SHEARS), keeper), item(pair[1])) == 1, "Shears take " + pair[1]);
		}
		helper.succeed();
	}

	/**
	 * Shroud moss hangs from leaves (not in open air); placed under itself the block above stops being a tip; bone meal
	 * lengthens the strand at its tip; shears take it and a hand doesn't; and the strand falls when the leaves go.
	 */
	@GameTest(maxTicks = 40)
	public void shroudMossHangs(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		Block moss = block("shroud_moss");
		BlockPos leaves = new BlockPos(3, 6, 3);
		helper.setBlock(leaves, Blocks.OAK_LEAVES.defaultBlockState().setValue(LeavesBlock.PERSISTENT, true));
		helper.assertTrue(!moss.defaultBlockState().canSurvive(level, helper.absolutePos(new BlockPos(5, 5, 5))), "Shroud moss won't hang in open air");
		helper.assertTrue(moss.defaultBlockState().canSurvive(level, helper.absolutePos(leaves.below())), "It hangs from leaves");
		helper.setBlock(leaves.below(), moss.defaultBlockState().setValue(HangingPlantBlock.TIP, true));
		ServerPlayer keeper = player(helper, new BlockPos(0, 2, 0), new ItemStack(Items.BONE_MEAL, 8));
		helper.useBlock(leaves.below(), keeper, hit(helper, leaves.below(), Direction.NORTH));
		helper.assertTrue(helper.getBlockState(leaves.below(2)).is(moss) && helper.getBlockState(leaves.below(2)).getValue(HangingPlantBlock.TIP),
				"Bone meal lengthens it by a block, a tip");
		helper.assertTrue(!helper.getBlockState(leaves.below()).getValue(HangingPlantBlock.TIP), "and the block above it is a tip no more");
		helper.assertTrue(count(drops(helper, leaves.below(), ItemStack.EMPTY, keeper), item("shroud_moss")) == 0, "A hand takes nothing");
		helper.assertTrue(count(drops(helper, leaves.below(), new ItemStack(Items.SHEARS), keeper), item("shroud_moss")) == 1, "Shears take it");
		helper.setBlock(leaves, Blocks.AIR);
		helper.runAfterDelay(10, () -> {
			helper.assertTrue(!helper.getBlockState(leaves.below()).is(moss) && !helper.getBlockState(leaves.below(2)).is(moss),
					"With the leaves gone the strand falls");
			helper.succeed();
		});
	}

	/** Creeping ivy on two faces gives two to shears (none by hand), and bone meal spreads it to another face. */
	@GameTest
	public void creepingIvyCoversFaces(GameTestHelper helper) {
		Block ivy = block("creeping_ivy");
		for (int y = 1; y < 5; y++) {
			for (int x = 1; x < 6; x++) {
				helper.setBlock(new BlockPos(x, y, 2), Blocks.STONE);
			}
		}
		helper.setBlock(new BlockPos(3, 1, 3), Blocks.STONE);
		BlockPos at = new BlockPos(3, 2, 3);
		BlockState state = ivy.defaultBlockState().setValue(MultifaceBlock.getFaceProperty(Direction.NORTH), true)
				.setValue(MultifaceBlock.getFaceProperty(Direction.DOWN), true);
		helper.setBlock(at, state);
		helper.assertTrue(helper.getBlockState(at).is(ivy), "Ivy holds on to a wall and a floor");
		ServerPlayer keeper = player(helper, new BlockPos(6, 2, 6), new ItemStack(Items.BONE_MEAL, 16));
		helper.assertTrue(count(drops(helper, at, ItemStack.EMPTY, keeper), item("creeping_ivy")) == 0, "A hand takes no ivy");
		helper.assertTrue(count(drops(helper, at, new ItemStack(Items.SHEARS), keeper), item("creeping_ivy")) == 2, "Shears take one for each face");
		int before = countIvy(helper);
		for (int i = 0; i < 8 && countIvy(helper) == before; i++) {
			helper.useBlock(at, keeper, hit(helper, at, Direction.NORTH));
		}
		helper.assertTrue(countIvy(helper) > before, "Bone meal spreads the ivy");
		helper.succeed();
	}

	private static int countIvy(GameTestHelper helper) {
		int faces = 0;
		for (BlockPos pos : BlockPos.betweenClosed(new BlockPos(0, 1, 0), new BlockPos(7, 6, 7))) {
			BlockState state = helper.getBlockState(pos);
			if (state.is(block("creeping_ivy"))) {
				for (Direction side : Direction.values()) {
					if (state.getValue(MultifaceBlock.getFaceProperty(side))) {
						faces++;
					}
				}
			}
		}
		return faces;
	}

	/** The root plants the crop, and the crop grows to its last age with bone meal. */
	@GameTest
	public void mandrakesGrowFromTheirRoots(GameTestHelper helper) {
		floor(helper, Blocks.FARMLAND);
		helper.assertTrue(item(Mandrakes.ROOT) instanceof BlockItem root && root.getBlock() == block(Mandrakes.CROP), "A mandrake root plants a mandrake");
		BlockPos at = new BlockPos(3, 2, 3);
		helper.setBlock(at, block(Mandrakes.CROP));
		ServerPlayer farmer = player(helper, new BlockPos(0, 2, 0), new ItemStack(Items.BONE_MEAL, 32));
		CropBlock crop = (CropBlock) block(Mandrakes.CROP);
		for (int i = 0; i < 24 && !crop.isMaxAge(helper.getBlockState(at)); i++) {
			helper.useBlock(at, farmer, hit(helper, at, Direction.UP));
		}
		helper.assertTrue(crop.isMaxAge(helper.getBlockState(at)), "Bone meal ripens it");
		helper.assertTrue(Mandrakes.screams(helper.getBlockState(at)), "A ripe mandrake screams when pulled");
		helper.assertTrue(!Mandrakes.screams(crop.getStateForAge(3)), "An unripe one doesn't");
		helper.succeed();
	}

	/**
	 * Pulling up a ripe mandrake: it drops its roots; the bare-headed puller and a bare-headed neighbour are sickened, a
	 * helmeted neighbour isn't, nor anyone beyond its reach; a puller in a helmet is spared and earns Mind Your Ears.
	 */
	@GameTest
	public void ripeMandrakesScream(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		floor(helper, Blocks.FARMLAND);
		CropBlock crop = (CropBlock) block(Mandrakes.CROP);
		BlockPos first = new BlockPos(2, 2, 2);
		helper.setBlock(first, crop.getStateForAge(crop.getMaxAge()));
		ServerPlayer puller = player(helper, new BlockPos(2, 2, 3), ItemStack.EMPTY);
		ServerPlayer bare = player(helper, new BlockPos(5, 2, 5), ItemStack.EMPTY);
		ServerPlayer helmeted = player(helper, new BlockPos(6, 2, 2), ItemStack.EMPTY);
		helmeted.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.LEATHER_HELMET));
		puller.gameMode.destroyBlock(helper.absolutePos(first));
		helper.assertTrue(helper.getBlockState(first).isAir(), "It comes up");
		AABB around = new AABB(helper.absolutePos(first)).inflate(2);
		int roots = level.getEntitiesOfClass(ItemEntity.class, around).stream().filter(e -> e.getItem().is(item(Mandrakes.ROOT)))
				.mapToInt(e -> e.getItem().getCount()).sum();
		helper.assertTrue(roots >= 1, "It drops its roots, not " + roots);
		helper.assertTrue(puller.hasEffect(MobEffects.NAUSEA) && bare.hasEffect(MobEffects.NAUSEA), "The bare-headed are sickened");
		helper.assertTrue(!helmeted.hasEffect(MobEffects.NAUSEA), "A helmet covers the ears");
		helper.assertTrue(!earned(puller, Mandrakes.ADVANCEMENT), "Pulled bare-headed: no advancement");

		BlockPos second = new BlockPos(6, 2, 6);
		helper.setBlock(second, crop.getStateForAge(crop.getMaxAge()));
		helmeted.gameMode.destroyBlock(helper.absolutePos(second));
		helper.assertTrue(!helmeted.hasEffect(MobEffects.NAUSEA), "A covered puller is spared");
		helper.assertTrue(earned(helmeted, Mandrakes.ADVANCEMENT), "and earns Mind Your Ears");

		// Beyond its reach nobody hears it.
		ServerPlayer far = player(helper, new BlockPos(2, 2, 2), ItemStack.EMPTY);
		BlockPos origin = helper.absolutePos(first);
		far.setPos(origin.getX() + Mandrakes.SCREAM_RADIUS + 3.5, origin.getY(), origin.getZ() + 0.5);
		Mandrakes.scream(level, origin, helmeted);
		helper.assertTrue(!far.hasEffect(MobEffects.NAUSEA), "Beyond " + Mandrakes.SCREAM_RADIUS + " blocks nobody is sickened");
		helper.succeed();
	}

	/** A wild mandrake pulled by hand screams and gives roots; taken with shears it comes whole and quietly. */
	@GameTest
	public void wildMandrakesScreamUnlessSheared(GameTestHelper helper) {
		floor(helper, Blocks.GRASS_BLOCK);
		BlockPos byHand = new BlockPos(2, 2, 2);
		BlockPos sheared = new BlockPos(5, 2, 5);
		helper.setBlock(byHand, block(Mandrakes.WILD));
		helper.setBlock(sheared, block(Mandrakes.WILD));
		ServerPlayer gatherer = player(helper, new BlockPos(2, 2, 3), new ItemStack(Items.SHEARS));
		helper.assertTrue(count(drops(helper, sheared, new ItemStack(Items.SHEARS), gatherer), item(Mandrakes.WILD)) == 1,
				"Shears take the wild plant whole");
		helper.assertTrue(count(drops(helper, byHand, ItemStack.EMPTY, gatherer), item(Mandrakes.ROOT)) >= 1, "By hand it gives roots");
		gatherer.gameMode.destroyBlock(helper.absolutePos(sheared));
		helper.assertTrue(!gatherer.hasEffect(MobEffects.NAUSEA), "Sheared, it makes no sound");
		gatherer.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
		gatherer.gameMode.destroyBlock(helper.absolutePos(byHand));
		helper.assertTrue(gatherer.hasEffect(MobEffects.NAUSEA), "Pulled by hand, it screams");
		helper.succeed();
	}

	/**
	 * The mandrake root is a Flying Ointment ingredient; grave vases take the flora's flowers by colour; every loot table,
	 * feature and vanilla-biome patch loads; the advancement loads.
	 */
	@GameTest
	public void floraDataLoads(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		helper.assertTrue(new ItemStack(item(Mandrakes.ROOT)).is(TagKey.create(Registries.ITEM, Jugcraft.id("hex/flying"))),
				"A mandrake root makes Flying Ointment");
		for (String[] pair : new String[][] {{"spider_lily", "red"}, {"snowdrop", "white"}, {"ghost_pipe", "white"}, {"deadly_nightshade", "purple"}}) {
			helper.assertTrue(new ItemStack(item(pair[0])).is(TagKey.create(Registries.ITEM, Jugcraft.id("grave_flowers/" + pair[1]))),
					pair[0] + " makes a " + pair[1] + " bouquet");
		}
		helper.assertTrue(new ItemStack(item("bleeding_heart")).is(TagKey.create(Registries.ITEM, Jugcraft.id("grave_flowers"))),
				"A grave vase takes any of them");
		for (String plant : PLANTS) {
			helper.assertTrue(level.getServer().reloadableRegistries().getLootTable(ResourceKey.create(Registries.LOOT_TABLE,
					Jugcraft.id("blocks/" + plant))) != LootTable.EMPTY, plant + "'s loot loads");
			helper.assertTrue(level.registryAccess().lookupOrThrow(Registries.FEATURE).get(
					ResourceKey.create(Registries.FEATURE, Jugcraft.id(plant))).isPresent(), plant + "'s feature loads");
		}
		for (String table : List.of("blocks/mandrake_crop", "blocks/wild_mandrake", "blocks/potted_spider_lily")) {
			helper.assertTrue(level.getServer().reloadableRegistries().getLootTable(ResourceKey.create(Registries.LOOT_TABLE, Jugcraft.id(table)))
					!= LootTable.EMPTY, table + " loads");
		}
		for (String placed : List.of("patch_ghost_pipe", "patch_ghost_fern", "patch_spider_lily", "patch_snowdrop", "patch_black_rose",
				"patch_wild_mandrake", "flora_shroud_moss", "flora_creeping_ivy", "flora_withered_grass", "flora_asphodel")) {
			helper.assertTrue(level.registryAccess().lookupOrThrow(Registries.PLACED_FEATURE).get(
					ResourceKey.create(Registries.PLACED_FEATURE, Jugcraft.id(placed))).isPresent(), placed + " loads");
		}
		helper.assertTrue(level.getServer().getAdvancements().get(Jugcraft.id(Mandrakes.ADVANCEMENT)) != null, "Mind Your Ears loads");
		helper.succeed();
	}
}
