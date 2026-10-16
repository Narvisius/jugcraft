package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.building.FieldTelephoneBlock;
import io.github.jimbozoomer.jugcraft.building.Trenchworks;
import io.github.jimbozoomer.jugcraft.config.JugcraftConfig;
import io.github.jimbozoomer.jugcraft.control.Channels;
import io.github.jimbozoomer.jugcraft.lair.DischargeBreakerBlock;
import io.github.jimbozoomer.jugcraft.lair.FoundryTrench;
import io.github.jimbozoomer.jugcraft.lair.FoundryTrench.Pylon;
import io.github.jimbozoomer.jugcraft.lair.GroundTerminalBlock;
import io.github.jimbozoomer.jugcraft.lair.JugcraftLairs;
import io.github.jimbozoomer.jugcraft.lair.Lair;
import io.github.jimbozoomer.jugcraft.lair.Lairs;
import io.github.jimbozoomer.jugcraft.lair.OrdersRite;
import io.github.jimbozoomer.jugcraft.lair.PylonCrownBlock;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * The Foundry Trench and Sealed Orders (docs/features/foundry-trench.md), as far as a game-test server can show them. Its
 * world is built from the flat preset alone, so it has no lair dimension: the trench's own files load (dimension type,
 * biome and template), the orders' checks hold (a Field Telephone in a trench, under the open sky), orders that can open no
 * instance are not used up and never set the telephone's channel, and the trench's fixtures keep their rules. Everything
 * that needs the trench itself (going over the top, following down the open line, the Grey Mist, the instance closing)
 * runs in a real world, in {@link FoundryTrenchClientGameTests}.
 */
public class FoundryTrenchGameTests {
	private static final Lair TRENCH = Lair.FOUNDRY_TRENCH;
	/** A field telephone here, facing south; the player stands south of it. */
	private static final BlockPos PHONE = new BlockPos(3, 2, 3);
	private static final BlockPos STAND = new BlockPos(3, 2, 6);
	/** Where trench works are laid round the telephone, in this order (all within three blocks of it). */
	private static final BlockPos[] WORKS = {new BlockPos(2, 2, 3), new BlockPos(4, 2, 3), new BlockPos(2, 2, 2), new BlockPos(4, 2, 2)};
	/** The most ticks a test waits for the sky light to settle after laying or lifting a roof. */
	private static final int LIGHT_TICKS = 60;

	private static ServerPlayer player(GameTestHelper helper, BlockPos standAt) {
		ServerPlayer player = helper.makeMockServerPlayerInLevel();
		player.setGameMode(GameType.SURVIVAL);
		BlockPos absolute = helper.absolutePos(standAt);
		player.snapTo(absolute.getX() + 0.5, absolute.getY(), absolute.getZ() + 0.5, 180.0F, 0.0F);
		return player;
	}

	/**
	 * Runs {@code then} once {@code settled} holds, looking each tick: the sky light under a roof laid (or lifted) in a test
	 * is worked out by the light engine a tick or more later, not as the blocks are set, and the orders ask whether the sky
	 * can be seen. Fails if it has not settled within {@value #LIGHT_TICKS} ticks.
	 */
	private static void whenSettled(GameTestHelper helper, BooleanSupplier settled, String what, Runnable then) {
		whenSettled(helper, settled, what, then, LIGHT_TICKS);
	}

	private static void whenSettled(GameTestHelper helper, BooleanSupplier settled, String what, Runnable then, int left) {
		if (settled.getAsBoolean()) {
			then.run();
			return;
		}
		helper.assertTrue(left > 0, "The sky light never settled: " + what);
		helper.runAfterDelay(1, () -> whenSettled(helper, settled, what, then, left - 1));
	}

	/** Whether the sky can be seen from over the telephone. */
	private static boolean skyOver(GameTestHelper helper) {
		return helper.getLevel().canSeeSky(helper.absolutePos(PHONE).above());
	}

	/** Sets the telephone at PHONE, the first {@code works} of WORKS as sandbags (the rest cleared), and a roof over it or none. */
	private static void trench(GameTestHelper helper, int works, boolean roofed) {
		helper.setBlock(PHONE, Trenchworks.BLOCKS.get("field_telephone").defaultBlockState()
				.setValue(HorizontalDirectionalBlock.FACING, Direction.SOUTH));
		for (int i = 0; i < WORKS.length; i++) {
			helper.setBlock(WORKS[i], i < works ? Trenchworks.BLOCKS.get("sandbags").defaultBlockState() : Blocks.AIR.defaultBlockState());
		}
		helper.setBlock(PHONE.above(3), roofed ? Blocks.STONE.defaultBlockState() : Blocks.AIR.defaultBlockState());
	}

	/** The Foundry Trench's dimension type and biome load from their data pack files. */
	@GameTest
	public void foundryTrenchDimensionFilesLoad(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		helper.assertTrue(level.registryAccess().lookupOrThrow(Registries.DIMENSION_TYPE).containsKey(TRENCH.dimension.identifier()),
				"No dimension type " + TRENCH.dimension.identifier());
		helper.assertTrue(level.registryAccess().lookupOrThrow(Registries.BIOME).containsKey(TRENCH.dimension.identifier()),
				"No biome " + TRENCH.dimension.identifier());
		helper.succeed();
	}

	/**
	 * The template loads in the running game with the pylons' lattices and crowns, the breakers, the ground terminals, the
	 * trench works, the dugout's telephone, the searchlights and the Grey Mist (DataVersion the game's), at the size
	 * tools/foundry_trench.py gives Lair.FOUNDRY_TRENCH, and with nothing in it that changes by itself once placed (the
	 * stacks' signal fires burn on, in a dimension with no weather).
	 */
	@GameTest
	public void foundryTrenchTemplateLoads(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		StructureTemplate template = Lairs.template(level, TRENCH);
		helper.assertTrue(template != null, "No template " + TRENCH.template);
		helper.assertTrue(template.getSize().equals(new Vec3i(TRENCH.width, TRENCH.height, TRENCH.length)),
				"The template is " + template.getSize() + ", not the Lair's size");
		Identifier path = Identifier.fromNamespaceAndPath(TRENCH.template.getNamespace(), "structure/" + TRENCH.template.getPath() + ".nbt");
		CompoundTag tag;
		try (InputStream in = level.getServer().getResourceManager().getResourceOrThrow(path).open()) {
			tag = NbtIo.readCompressed(in, NbtAccounter.unlimitedHeap());
		} catch (IOException e) {
			throw helper.assertionException("Could not read " + path + ": " + e);
		}
		int file = tag.getIntOr("DataVersion", -1);
		StructureTemplate loaded = new StructureTemplate();
		loaded.load(level.registryAccess().lookupOrThrow(Registries.BLOCK), tag);
		CompoundTag saved = loaded.save(new CompoundTag());
		helper.assertTrue(file == saved.getIntOr("DataVersion", -1), "The template's DataVersion " + file + " is not the game's");
		ListTag palette = saved.getListOrEmpty("palette");
		List<String> names = new ArrayList<>();
		for (int i = 0; i < palette.size(); i++) {
			names.add(palette.getCompoundOrEmpty(i).getStringOr("id", "?"));
		}
		for (String wanted : List.of("jugcraft:pylon_lattice", "jugcraft:pylon_crown", "jugcraft:discharge_breaker", "jugcraft:ground_terminal",
				"jugcraft:sandbags", "jugcraft:duckboard", "jugcraft:field_telephone", "jugcraft:searchlight", "jugcraft:lair_exit")) {
			helper.assertTrue(names.contains(wanted), "The template has no " + wanted + ": " + names);
		}
		for (String name : names) {
			helper.assertTrue(!name.endsWith("_leaves") && !name.equals("minecraft:vine") && !name.equals("minecraft:water")
					&& !name.equals("minecraft:lava") && !name.equals("minecraft:fire") && !name.equals("minecraft:soul_fire"),
					"The template has " + name + ", which would change once placed");
		}
		helper.succeed();
	}

	/**
	 * The orders' checks, changing nothing: on a telephone under the open sky they need four trench-works blocks round it,
	 * and take with four; under a roof they are refused. Sandbags, timber revetment, duckboards and their slabs and stairs
	 * are trench works; stone is not. No line is open where nobody used orders. The roof's check waits for the sky light to
	 * settle ({@link #whenSettled}).
	 */
	@GameTest(maxTicks = 200)
	public void theOrdersNeedATelephoneInAnOpenTrench(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		BlockPos phone = helper.absolutePos(PHONE);
		TagKey<Block> works = TagKey.create(Registries.BLOCK, Jugcraft.id("trench_works"));
		for (String id : List.of("sandbags", "sandbags_slab", "sandbags_stairs", "timber_revetment", "timber_revetment_slab",
				"timber_revetment_stairs", "duckboard")) {
			helper.assertTrue(Trenchworks.BLOCKS.get(id).defaultBlockState().is(works), id + " is not a trench work");
		}
		helper.assertTrue(!Blocks.STONE.defaultBlockState().is(works), "Stone is a trench work");
		trench(helper, 3, false);
		helper.assertTrue(OrdersRite.line(level, phone) == null, "A line was open where nobody used orders");
		whenSettled(helper, () -> skyOver(helper), "under the open sky", () -> {
			helper.assertTrue(OrdersRite.trenchWorks(level, phone) == 3 && OrdersRite.check(level, phone) == OrdersRite.Missing.NO_TRENCH,
					"With three trench works round it the telephone was in a trench: " + OrdersRite.check(level, phone));
			trench(helper, 4, false);
			helper.assertTrue(OrdersRite.trenchWorks(level, phone) == 4 && OrdersRite.check(level, phone) == null,
					"In an open trench the orders were refused: " + OrdersRite.check(level, phone));
			trench(helper, 4, true);
			whenSettled(helper, () -> !skyOver(helper), "under the roof", () -> {
				helper.assertTrue(OrdersRite.check(level, phone) == OrdersRite.Missing.ROOFED, "Under a roof the orders were not refused");
				helper.succeed();
			});
		});
	}

	/**
	 * The orders used by a player's hand when no instance can open (a game-test server has no lair dimension, and the one
	 * slot {@code lairs.instances} allows is taken if it has): the trench is full, the orders are kept and rested, the
	 * telephone's channel is not changed, nobody moves and no line opens. With no trench round the telephone they are
	 * refused the same way. A telephone used with an empty hand and no line open is the telephone's own business: it says
	 * its channel, and nobody moves.
	 */
	@GameTest(maxTicks = 200)
	public void theOrdersUseNothingWhenNoTrenchCanOpen(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		trench(helper, 4, false);
		whenSettled(helper, () -> skyOver(helper), "under the open sky", () -> {
			ServerPlayer player = player(helper, STAND);
			player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(JugcraftLairs.SEALED_ORDERS, 2));
			BlockPos phone = helper.absolutePos(PHONE);
			String instances = JugcraftConfig.textOption("lairs.instances");
			try {
				JugcraftConfig.setTextOption("lairs.instances", "1");
				Lairs.open(level.getServer(), TRENCH);  // null here; takes the only slot where there is a trench
				Vec3 stood = player.position();
				BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(phone), Direction.SOUTH, phone, false);
				InteractionResult used = player.gameMode.useItemOn(player, level, player.getMainHandItem(), InteractionHand.MAIN_HAND, hit);
				helper.assertTrue(used.consumesAction(), "Using the orders was not taken as the use: " + used);
				helper.assertTrue(player.getMainHandItem().getCount() == 2, "The orders were used up: " + player.getMainHandItem());
				helper.assertTrue(player.getCooldowns().isOnCooldown(player.getMainHandItem()), "The orders were not rested after their use");
				helper.assertTrue(helper.getBlockState(PHONE).getValue(Channels.CHANNEL) == DyeColor.WHITE, "The orders set the telephone's channel");
				helper.assertTrue(player.level() == level && player.position().distanceTo(stood) < 1.0E-6 && player.getAttached(Lairs.VISIT) == null,
						"The player was moved");
				helper.assertTrue(OrdersRite.line(level, phone) == null, "A line opened into no trench");
				OrdersRite.Call full = OrdersRite.call(level, player, phone, player.getMainHandItem());
				helper.assertTrue(full.missing() == OrdersRite.Missing.FULL && full.instance() == null, "The trench was not full: " + full);
			} finally {
				JugcraftConfig.setTextOption("lairs.instances", instances);
				Lairs.reset();
			}
			trench(helper, 0, false);
			helper.assertTrue(OrdersRite.call(level, player, phone, player.getMainHandItem()).missing() == OrdersRite.Missing.NO_TRENCH
					&& player.getMainHandItem().getCount() == 2, "With no trench round the telephone the orders were not refused, or were used up");
			player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
			Vec3 stood = player.position();
			InteractionResult rang = player.gameMode.useItemOn(player, level, ItemStack.EMPTY, InteractionHand.MAIN_HAND,
					new BlockHitResult(Vec3.atCenterOf(phone), Direction.SOUTH, phone, false));
			helper.assertTrue(rang.consumesAction() && player.position().distanceTo(stood) < 1.0E-6 && player.getAttached(Lairs.VISIT) == null,
					"The telephone used with an empty hand, no line open, did not answer as itself: " + rang);
			helper.assertTrue(helper.getBlockState(PHONE).getBlock() instanceof FieldTelephoneBlock, "The telephone is gone");
			helper.succeed();
		});
	}

	/**
	 * The trench's fixtures keep their rules. A crown gives light 12 live and none dark; a breaker's lamp 10 live; a
	 * terminal 8 live; each changes only when told to. None of the four can be broken or has an item. The breakers are lair
	 * fixtures, which a player may use: with no Dreadnought to route, a breaker used stays open and dark. The lattice, the
	 * crowns and the terminals are not fixtures. Each pylon is found by its breaker in an instance.
	 */
	@GameTest
	public void theTrenchFixturesKeepTheirRules(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		BlockPos crown = new BlockPos(1, 2, 1);
		helper.setBlock(crown, JugcraftLairs.PYLON_CROWN.defaultBlockState());
		BlockPos at = helper.absolutePos(crown);
		helper.assertTrue(helper.getBlockState(crown).getLightEmission() == 0, "A crown burns when placed");
		helper.assertTrue(PylonCrownBlock.setLive(level, at, true) && helper.getBlockState(crown).getLightEmission() == PylonCrownBlock.LIGHT,
				"A live crown does not burn at " + PylonCrownBlock.LIGHT);
		helper.assertTrue(!PylonCrownBlock.setLive(level, at, true), "A live crown was lit again");
		helper.assertTrue(PylonCrownBlock.setLive(level, at, false) && helper.getBlockState(crown).getLightEmission() == 0, "A crown did not go dark");
		BlockPos terminal = new BlockPos(5, 1, 1);
		helper.setBlock(terminal, JugcraftLairs.GROUND_TERMINAL.defaultBlockState());
		helper.assertTrue(helper.getBlockState(terminal).getLightEmission() == 0
				&& GroundTerminalBlock.setLive(level, helper.absolutePos(terminal), true)
				&& helper.getBlockState(terminal).getLightEmission() == GroundTerminalBlock.LIGHT, "A terminal does not burn at "
				+ GroundTerminalBlock.LIGHT + " live only");
		TagKey<Block> fixtures = TagKey.create(Registries.BLOCK, Jugcraft.id("lair_fixtures"));
		for (Block block : List.of(JugcraftLairs.PYLON_LATTICE, JugcraftLairs.PYLON_CROWN, JugcraftLairs.DISCHARGE_BREAKER,
				JugcraftLairs.GROUND_TERMINAL)) {
			helper.assertTrue(block.defaultBlockState().getDestroySpeed(level, at) < 0.0F, block + " can be broken");
			helper.assertTrue(block.asItem() == Items.AIR, block + " has an item");
		}
		helper.assertTrue(JugcraftLairs.DISCHARGE_BREAKER.defaultBlockState().is(fixtures), "The breakers are not lair fixtures");
		helper.assertTrue(!JugcraftLairs.PYLON_LATTICE.defaultBlockState().is(fixtures) && !JugcraftLairs.PYLON_CROWN.defaultBlockState().is(fixtures)
				&& !JugcraftLairs.GROUND_TERMINAL.defaultBlockState().is(fixtures), "A lattice, crown or terminal is a lair fixture");
		BlockPos breaker = new BlockPos(3, 2, 1);
		helper.setBlock(breaker, JugcraftLairs.DISCHARGE_BREAKER.defaultBlockState().setValue(HorizontalDirectionalBlock.FACING, Direction.SOUTH));
		BlockPos breakerAt = helper.absolutePos(breaker);
		ServerPlayer player = player(helper, new BlockPos(3, 2, 3));
		InteractionResult used = player.gameMode.useItemOn(player, level, ItemStack.EMPTY, InteractionHand.MAIN_HAND,
				new BlockHitResult(Vec3.atCenterOf(breakerAt), Direction.SOUTH, breakerAt, false));
		helper.assertTrue(used.consumesAction() && !helper.getBlockState(breaker).getValue(DischargeBreakerBlock.THROWN)
				&& !helper.getBlockState(breaker).getValue(DischargeBreakerBlock.LIVE),
				"With no Dreadnought to route, the breaker was thrown or lit, or the use was not taken: " + used);
		helper.assertTrue(DischargeBreakerBlock.set(level, breakerAt, DischargeBreakerBlock.LIVE, true)
				&& helper.getBlockState(breaker).getLightEmission() == DischargeBreakerBlock.LIGHT
				&& DischargeBreakerBlock.set(level, breakerAt, DischargeBreakerBlock.THROWN, true)
				&& DischargeBreakerBlock.set(level, breakerAt, DischargeBreakerBlock.FOULED, true)
				&& !DischargeBreakerBlock.set(level, breakerAt, DischargeBreakerBlock.FOULED, true),
				"A breaker does not light, throw and foul when told to, once");
		BlockPos origin = TRENCH.origin(0);
		for (Pylon pylon : Pylon.values()) {
			helper.assertTrue(FoundryTrench.breakerAt(origin, pylon.breaker(origin)) == pylon, "The " + pylon.title() + " pylon is not found by its breaker");
		}
		helper.assertTrue(FoundryTrench.breakerAt(origin, origin) == null, "A pylon was found where no breaker stands");
		helper.succeed();
	}
}
