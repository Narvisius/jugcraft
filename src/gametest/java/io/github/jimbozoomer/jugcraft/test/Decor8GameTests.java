package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.agriculture.BlackCatBlock;
import io.github.jimbozoomer.jugcraft.agriculture.BlackCatBlockEntity;
import io.github.jimbozoomer.jugcraft.agriculture.DecorationBlockEntity;
import io.github.jimbozoomer.jugcraft.agriculture.JugcraftAgriculture;
import io.github.jimbozoomer.jugcraft.agriculture.LabTableBlock;
import io.github.jimbozoomer.jugcraft.agriculture.MummySarcophagusBlock;
import io.github.jimbozoomer.jugcraft.agriculture.RavenPerchBlock;
import io.github.jimbozoomer.jugcraft.agriculture.SpecimenJarBlock;
import io.github.jimbozoomer.jugcraft.agriculture.TeslaCoilBlock;
import io.github.jimbozoomer.jugcraft.agriculture.TeslaCoilBlockEntity;
import io.github.jimbozoomer.jugcraft.energy.EnergyStorage;
import java.util.List;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.BlockItemStateProperties;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * In-game tests for the mad scientist and monsters: the Tesla Coil (power, running, arcs between coils), the Lab
 * Table (one prop, its patient sitting up on redstone), the Specimen Jar (specimens, light, keeping its specimen), the
 * Mummy Sarcophagus (opening by hand and by redstone, shutting again), the Raven (ruffling, flapping when used), the
 * Black Cat Figure (hissing at a runner, not at a walker, resting after), and that their data loads.
 */
public class Decor8GameTests {
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

	// ---------------------------------------------------------------- the tesla coil

	/**
	 * Offsets pack and unpack; a coil stands two blocks tall with its power on the lower half; switched on without
	 * power it doesn't run; with power it runs (light 8 on both halves) using 20 JE a tick, and two running coils three
	 * blocks apart throw arcs at each other; switched off it stops.
	 */
	@GameTest(maxTicks = 120)
	public void teslaCoilsArcOnPower(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		floor(helper);
		for (int[] offset : new int[][] {{3, 0, 0}, {-8, 2, 7}, {0, -1, -15}}) {
			int[] back = TeslaCoilBlockEntity.unpack(TeslaCoilBlockEntity.pack(offset[0], offset[1], offset[2]));
			helper.assertTrue(back[0] == offset[0] && back[1] == offset[1] && back[2] == offset[2], "An offset packs and unpacks");
		}
		ServerPlayer player = player(helper, new BlockPos(3, 2, 6), new ItemStack(item("tesla_coil"), 2));
		BlockPos a = new BlockPos(2, 2, 3);
		BlockPos b = new BlockPos(5, 2, 3);
		place(helper, player, a.below(), Direction.UP);
		place(helper, player, b.below(), Direction.UP);
		helper.assertTrue(helper.getBlockState(a).is(block("tesla_coil")) && helper.getBlockState(a.above()).getValue(TeslaCoilBlock.HALF) == DoubleBlockHalf.UPPER,
				"A coil stands two blocks tall");
		TeslaCoilBlockEntity coilA = helper.getBlockEntity(a, TeslaCoilBlockEntity.class);
		TeslaCoilBlockEntity coilB = helper.getBlockEntity(b, TeslaCoilBlockEntity.class);
		helper.assertTrue(EnergyStorage.SIDED.find(level, helper.absolutePos(a), Direction.NORTH) != null, "The network finds its power on the lower half");
		player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
		use(helper, player, a.above(), Direction.NORTH);
		use(helper, player, b, Direction.NORTH);
		helper.assertTrue(helper.getBlockState(a).getValue(TeslaCoilBlock.ENABLED) && helper.getBlockState(a.above()).getValue(TeslaCoilBlock.ENABLED),
				"Using either half switches it on");
		helper.runAfterDelay(3, () -> {
			helper.assertFalse(helper.getBlockState(a).getValue(TeslaCoilBlock.ACTIVE), "Without power it doesn't run");
			coilA.energy().setAmount(TeslaCoilBlockEntity.CAPACITY);
			coilB.energy().setAmount(TeslaCoilBlockEntity.CAPACITY);
			helper.runAfterDelay(10, () -> {
				helper.assertTrue(helper.getBlockState(a).getValue(TeslaCoilBlock.ACTIVE) && helper.getBlockState(a.above()).getLightEmission() == TeslaCoilBlock.LIGHT,
						"With power it runs and glows");
				long used = TeslaCoilBlockEntity.CAPACITY - coilA.energy().getAmount();
				helper.assertTrue(used >= 9 * TeslaCoilBlockEntity.USE && used <= 11 * TeslaCoilBlockEntity.USE, "using 20 JE a tick: " + used);
			});
			helper.runAfterDelay(10 + TeslaCoilBlockEntity.ARC_MIN + TeslaCoilBlockEntity.ARC_SPREAD + 5, () -> {
				for (TeslaCoilBlockEntity coil : List.of(coilA, coilB)) {
					helper.assertTrue(coil.arcTime() > 0 && (coil.arc() & TeslaCoilBlockEntity.INTO_AIR) == 0, "Each coil has arced to the other");
					int[] offset = TeslaCoilBlockEntity.unpack(coil.arc());
					helper.assertTrue(Math.abs(offset[0]) == 3 && offset[1] == 0 && offset[2] == 0, "three blocks along: " + offset[0]);
				}
				player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
				use(helper, player, a, Direction.NORTH);
				helper.runAfterDelay(2, () -> {
					helper.assertFalse(helper.getBlockState(a).getValue(TeslaCoilBlock.ACTIVE), "Switched off, it stops");
					helper.succeed();
				});
			});
		});
	}

	// ---------------------------------------------------------------- the lab table

	/**
	 * The table lies away from the player, foot where they aimed and head beyond; a redstone block by the head sits the
	 * patient up and taking it away lays it down; the lean reaches 70 degrees and no further, and the twitching keeps to
	 * night; breaking the head breaks the table and drops it once.
	 */
	@GameTest(maxTicks = 40)
	public void theLabTablePatientSitsUp(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		floor(helper);
		ServerPlayer player = player(helper, new BlockPos(3, 2, 6), new ItemStack(item("lab_table")));
		player.setYRot(180.0F); // looking north
		BlockPos foot = new BlockPos(3, 2, 4);
		BlockPos head = foot.north();
		place(helper, player, foot.below(), Direction.UP);
		helper.assertTrue(helper.getBlockState(foot).is(block("lab_table")) && helper.getBlockState(foot).getValue(LabTableBlock.PART) == BedPart.FOOT
				&& helper.getBlockState(head).getValue(LabTableBlock.PART) == BedPart.HEAD
				&& helper.getBlockState(foot).getValue(LabTableBlock.FACING) == Direction.NORTH, "Foot where aimed, head beyond");
		helper.assertTrue(helper.getBlockEntity(foot, DecorationBlockEntity.class) != null, "The patient is drawn from the foot");
		helper.setBlock(head.west(), Blocks.REDSTONE_BLOCK);
		helper.assertTrue(helper.getBlockState(foot).getValue(LabTableBlock.POWERED), "A redstone block by the head sits the patient up");
		helper.setBlock(head.west(), Blocks.AIR);
		helper.assertFalse(helper.getBlockState(foot).getValue(LabTableBlock.POWERED), "Without it, the patient lies down");
		helper.assertTrue(LabTableBlock.lean(0.0F, true, 20.0F) == LabTableBlock.SIT_DEGREES && LabTableBlock.lean(LabTableBlock.SIT_DEGREES, false, 1.0F)
				== LabTableBlock.SIT_DEGREES - LabTableBlock.SIT_SPEED, "It sits up to 70 degrees and lies back slowly");
		BlockPos at = helper.absolutePos(foot);
		int twitches = 0;
		for (int t = 0; t < LabTableBlock.TWITCH_PERIOD; t++) {
			helper.assertFalse(LabTableBlock.twitching(at, false, t), "By day it lies still");
			twitches += LabTableBlock.twitching(at, true, t) ? 1 : 0;
		}
		helper.assertTrue(twitches == LabTableBlock.TWITCH_TICKS, "At night it twitches " + LabTableBlock.TWITCH_TICKS + " ticks a period: " + twitches);
		level.destroyBlock(helper.absolutePos(head), true);
		helper.runAfterDelay(3, () -> {
			helper.assertBlockNotPresent(block("lab_table"), foot);
			helper.assertTrue(dropped(helper, item("lab_table")) == 1, "Breaking the head breaks the table, dropping it once");
			helper.succeed();
		});
	}

	// ---------------------------------------------------------------- the specimen jar

	/** A jar glows (light 7) with an eye; sneak-use turns through the specimens; broken, it keeps its specimen. */
	@GameTest(maxTicks = 40)
	public void specimenJarsKeepTheirSpecimens(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		floor(helper);
		BlockPos pos = new BlockPos(3, 2, 3);
		ServerPlayer player = player(helper, new BlockPos(3, 2, 6), new ItemStack(item("specimen_jar")));
		place(helper, player, pos.below(), Direction.UP);
		helper.assertTrue(helper.getBlockState(pos).getValue(SpecimenJarBlock.SPECIMEN) == SpecimenJarBlock.Specimen.EYE
				&& helper.getBlockState(pos).getLightEmission() == SpecimenJarBlock.LIGHT, "It glows, with an eye in it");
		player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
		player.setShiftKeyDown(true);
		for (SpecimenJarBlock.Specimen next : new SpecimenJarBlock.Specimen[] {SpecimenJarBlock.Specimen.TENTACLE, SpecimenJarBlock.Specimen.PUMPKIN,
				SpecimenJarBlock.Specimen.BRAIN, SpecimenJarBlock.Specimen.EYE, SpecimenJarBlock.Specimen.TENTACLE}) {
			use(helper, player, pos, Direction.UP);
			helper.assertTrue(helper.getBlockState(pos).getValue(SpecimenJarBlock.SPECIMEN) == next, "Sneak-use puts in the " + next);
		}
		player.setShiftKeyDown(false);
		for (int t = 0; t < SpecimenJarBlock.BOB_TICKS; t += 3) {
			helper.assertTrue(Math.abs(SpecimenJarBlock.bob(helper.absolutePos(pos), t)) <= SpecimenJarBlock.BOB, "It bobs no more than its bob");
		}
		level.destroyBlock(helper.absolutePos(pos), true);
		helper.runAfterDelay(3, () -> {
			List<ItemEntity> jars = drops(helper, item("specimen_jar"));
			helper.assertTrue(jars.size() == 1, "Broken, it drops once");
			BlockItemStateProperties state = jars.get(0).getItem().get(DataComponents.BLOCK_STATE);
			helper.assertTrue(state != null && state.apply(block("specimen_jar").defaultBlockState()).getValue(SpecimenJarBlock.SPECIMEN)
					== SpecimenJarBlock.Specimen.TENTACLE, "and keeps its tentacle: " + state);
			helper.succeed();
		});
	}

	// ---------------------------------------------------------------- the mummy sarcophagus

	/** Used, it opens on both halves and shuts again after its time; a rising redstone signal opens it too. */
	@GameTest(maxTicks = 300)
	public void theSarcophagusOpensAndShuts(GameTestHelper helper) {
		floor(helper);
		ServerPlayer player = player(helper, new BlockPos(3, 2, 6), new ItemStack(item("mummy_sarcophagus")));
		player.setYRot(180.0F);
		BlockPos lower = new BlockPos(3, 2, 3);
		place(helper, player, lower.below(), Direction.UP);
		helper.assertTrue(helper.getBlockState(lower).is(block("mummy_sarcophagus")) && helper.getBlockState(lower.above()).is(block("mummy_sarcophagus"))
				&& helper.getBlockState(lower).getValue(MummySarcophagusBlock.FACING) == Direction.SOUTH, "It stands two tall, facing the player");
		helper.assertTrue(helper.getBlockEntity(lower, DecorationBlockEntity.class) != null, "The lid and mummy are drawn from the lower half");
		player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
		use(helper, player, lower.above(), Direction.SOUTH);
		helper.assertTrue(helper.getBlockState(lower).getValue(MummySarcophagusBlock.OPEN) && helper.getBlockState(lower.above()).getValue(MummySarcophagusBlock.OPEN),
				"Used, it opens");
		helper.assertTrue(MummySarcophagusBlock.approach(0.0F, MummySarcophagusBlock.LID_DEGREES, MummySarcophagusBlock.LID_SPEED) == MummySarcophagusBlock.LID_SPEED,
				"The lid swings a little a tick");
		helper.runAfterDelay(MummySarcophagusBlock.OPEN_TICKS + 2, () -> {
			helper.assertFalse(helper.getBlockState(lower).getValue(MummySarcophagusBlock.OPEN) || helper.getBlockState(lower.above()).getValue(MummySarcophagusBlock.OPEN),
					"After its time it shuts");
			helper.setBlock(lower.east(), Blocks.REDSTONE_BLOCK);
			helper.assertTrue(helper.getBlockState(lower).getValue(MummySarcophagusBlock.OPEN), "A redstone signal opens it");
			helper.runAfterDelay(MummySarcophagusBlock.OPEN_TICKS + 2, () -> {
				helper.assertFalse(helper.getBlockState(lower).getValue(MummySarcophagusBlock.OPEN), "and it shuts again, though the power stays");
				helper.succeed();
			});
		});
	}

	// ---------------------------------------------------------------- the raven

	/** The raven ruffles for its ticks of each period; used, it flaps (the block event marks when). */
	@GameTest(maxTicks = 20)
	public void theRavenRufflesAndFlaps(GameTestHelper helper) {
		floor(helper);
		BlockPos pos = new BlockPos(3, 2, 3);
		ServerPlayer player = player(helper, new BlockPos(3, 2, 6), new ItemStack(item("raven_perch")));
		player.setYRot(180.0F); // looking north, at the perch
		place(helper, player, pos.below(), Direction.UP);
		helper.assertTrue(helper.getBlockState(pos).getValue(RavenPerchBlock.FACING) == Direction.SOUTH, "It faces the player");
		BlockPos at = helper.absolutePos(pos);
		int ruffles = 0;
		for (int t = 0; t < RavenPerchBlock.RUFFLE_PERIOD; t++) {
			ruffles += RavenPerchBlock.ruffling(at, t) ? 1 : 0;
		}
		helper.assertTrue(ruffles == RavenPerchBlock.RUFFLE_TICKS, "It ruffles " + RavenPerchBlock.RUFFLE_TICKS + " ticks a period: " + ruffles);
		player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
		use(helper, player, pos, Direction.SOUTH);
		long used = helper.getLevel().getGameTime();
		DecorationBlockEntity raven = helper.getBlockEntity(pos, DecorationBlockEntity.class);
		helper.succeedWhen(() -> helper.assertTrue(raven.marked() >= used, "Used, it flaps: " + raven.marked()));
	}

	// ---------------------------------------------------------------- the black cat

	/**
	 * A walking player beside the cat doesn't upset it; a sprinting one makes it hiss, and it settles after its time;
	 * then it rests, ignoring another runner until its cooldown is over. The tail is still while it hisses.
	 */
	@GameTest(maxTicks = 200)
	public void theBlackCatHissesAtRunners(GameTestHelper helper) {
		floor(helper);
		BlockPos pos = new BlockPos(3, 2, 3);
		helper.setBlock(pos, block("black_cat_figure"));
		BlackCatBlockEntity cat = helper.getBlockEntity(pos, BlackCatBlockEntity.class);
		ServerPlayer player = player(helper, new BlockPos(3, 2, 5), ItemStack.EMPTY);
		helper.runAfterDelay(BlackCatBlockEntity.PERIOD * 2, () -> {
			helper.assertFalse(helper.getBlockState(pos).getValue(BlackCatBlock.HISSING), "A walker doesn't upset it");
			player.setSprinting(true);
			helper.runAfterDelay(BlackCatBlockEntity.PERIOD * 2, () -> {
				helper.assertTrue(helper.getBlockState(pos).getValue(BlackCatBlock.HISSING), "A runner makes it hiss");
				helper.assertTrue(BlackCatBlock.swish(helper.absolutePos(pos), true, 13.0F) == 0.0F, "and its tail goes still");
				helper.runAfterDelay(BlackCatBlock.HISS_TICKS + BlackCatBlockEntity.PERIOD, () -> {
					helper.assertFalse(helper.getBlockState(pos).getValue(BlackCatBlock.HISSING), "It settles after its time");
					helper.assertFalse(cat.update(helper.getLevel()), "and rests, though the runner is still there");
					helper.runAfterDelay(BlackCatBlock.COOLDOWN_TICKS, () -> {
						helper.assertTrue(helper.getBlockState(pos).getValue(BlackCatBlock.HISSING), "Rested, it hisses again");
						helper.succeed();
					});
				});
			});
		});
	}

	/** The recipes and loot tables load. */
	@GameTest
	public void madScientistDataLoads(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		for (String id : List.of("tesla_coil", "lab_table", "specimen_jar", "mummy_sarcophagus", "raven_perch", "black_cat_figure")) {
			helper.assertTrue(level.recipeAccess().byKey(ResourceKey.create(Registries.RECIPE, Jugcraft.id(id))).isPresent(), "Recipe " + id + " loads");
			LootTable table = level.getServer().reloadableRegistries().getLootTable(ResourceKey.create(Registries.LOOT_TABLE, Jugcraft.id("blocks/" + id)));
			helper.assertTrue(table != LootTable.EMPTY, "The " + id + " drops itself");
		}
		helper.succeed();
	}
}
