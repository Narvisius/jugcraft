package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.config.JugcraftConfig;
import io.github.jimbozoomer.jugcraft.lair.FenLampBlock;
import io.github.jimbozoomer.jugcraft.lair.HagStoneRite;
import io.github.jimbozoomer.jugcraft.lair.JugcraftLairs;
import io.github.jimbozoomer.jugcraft.lair.Lair;
import io.github.jimbozoomer.jugcraft.lair.LairBrazierBlock;
import io.github.jimbozoomer.jugcraft.lair.Lairs;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Registry;
import net.minecraft.core.Vec3i;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * The Lantern Fen and the Hag Stone (docs/features/lantern-fen.md), as far as a game-test server can show them. Its world
 * is built from the flat preset alone, so it has no lair dimension: the fen's own files load (dimension type, biome and
 * template), the stone's checks hold (night, a wetland, and shallow water or mud underfoot), a stone that can open no
 * instance is not used up, and the fen's lamps burn, go out and are lit again by a player's hand. Everything that needs the
 * fen itself (the mist rising, waking on the jetty, following, the Grey Mist, the instance closing) runs in a real world,
 * in {@link LanternFenClientGameTests}.
 */
public class LanternFenGameTests {
	private static final Lair FEN = Lair.LANTERN_FEN;
	private static final BlockPos STAND = new BlockPos(2, 2, 2);

	private static ServerPlayer player(GameTestHelper helper, BlockPos standAt, double up) {
		ServerPlayer player = helper.makeMockServerPlayerInLevel();
		player.setGameMode(GameType.SURVIVAL);
		BlockPos absolute = helper.absolutePos(standAt);
		player.snapTo(absolute.getX() + 0.5, absolute.getY() + up, absolute.getZ() + 0.5, 180.0F, 0.0F);
		return player;
	}

	/** The Lantern Fen's dimension type and biome load from their data pack files. */
	@GameTest
	public void lanternFenDimensionFilesLoad(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		helper.assertTrue(level.registryAccess().lookupOrThrow(Registries.DIMENSION_TYPE).containsKey(FEN.dimension.identifier()),
				"No dimension type " + FEN.dimension.identifier());
		helper.assertTrue(level.registryAccess().lookupOrThrow(Registries.BIOME).containsKey(FEN.dimension.identifier()),
				"No biome " + FEN.dimension.identifier());
		helper.succeed();
	}

	/**
	 * The template loads in the running game with the fen lamp, the Grey Mist, the boardwalks' willow planks and the water
	 * (DataVersion the game's), at the size tools/lantern_fen.py gives Lair.LANTERN_FEN, and with nothing in it that changes
	 * by itself once placed: no leaves and no vines.
	 */
	@GameTest
	public void lanternFenTemplateLoads(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		StructureTemplate template = Lairs.template(level, FEN);
		helper.assertTrue(template != null, "No template " + FEN.template);
		helper.assertTrue(template.getSize().equals(new Vec3i(FEN.width, FEN.height, FEN.length)),
				"The template is " + template.getSize() + ", not the Lair's size");
		Identifier path = Identifier.fromNamespaceAndPath(FEN.template.getNamespace(), "structure/" + FEN.template.getPath() + ".nbt");
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
		for (String wanted : List.of("jugcraft:fen_lamp", "jugcraft:lair_exit", "jugcraft:willow_planks", "minecraft:water",
				"jugcraft:bubbling_cauldron")) {
			helper.assertTrue(names.contains(wanted), "The template has no " + wanted + ": " + names);
		}
		for (String name : names) {
			helper.assertTrue(!name.endsWith("_leaves") && !name.equals("minecraft:vine"),
					"The template has " + name + ", which would change once placed");
		}
		helper.succeed();
	}

	/**
	 * The stone's checks, changing nothing: by day, or out of the wetlands, it is refused; at night in a wetland it takes
	 * standing on mud (in its cell, mud being lower than a whole block) or in shallow water, and not on stone, under the
	 * water or swimming over deep water. The wetlands are every swamp, Jugcraft's and vanilla's, the Floodplain and the
	 * Muskeg, not the plains, the desert or the Volcano. No mist rises where nobody looked through a stone.
	 */
	@GameTest
	public void theStoneNeedsShallowWaterOrMudInAWetlandAtNight(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		BlockPos feet = helper.absolutePos(STAND);
		helper.setBlock(STAND.below(), Blocks.MUD.defaultBlockState());
		ServerPlayer onMud = player(helper, STAND.below(), 14 / 16.0);
		helper.assertTrue(HagStoneRite.feet(onMud).equals(feet), "On mud, the feet are not taken in the cell over it: "
				+ HagStoneRite.feet(onMud));
		helper.assertTrue(HagStoneRite.check(level, feet, true, true) == null, "On mud at night in a wetland the stone was refused: "
				+ HagStoneRite.check(level, feet, true, true));
		helper.assertTrue(HagStoneRite.check(level, feet, false, true) == HagStoneRite.Missing.NOT_NIGHT, "By day the stone was not refused");
		helper.assertTrue(HagStoneRite.check(level, feet, true, false) == HagStoneRite.Missing.NOT_WETLAND,
				"Out of the wetlands the stone was not refused");
		helper.setBlock(STAND.below(), Blocks.STONE.defaultBlockState());
		helper.assertTrue(HagStoneRite.check(level, feet, true, true) == HagStoneRite.Missing.NOT_WET, "On stone the stone was not refused");
		helper.setBlock(STAND, Blocks.WATER.defaultBlockState());
		helper.assertTrue(HagStoneRite.check(level, feet, true, true) == null, "In shallow water the stone was refused: "
				+ HagStoneRite.check(level, feet, true, true));
		helper.setBlock(STAND.above(), Blocks.WATER.defaultBlockState());
		helper.assertTrue(HagStoneRite.check(level, feet, true, true) == HagStoneRite.Missing.NOT_WET, "Under the water the stone was not refused");
		helper.assertTrue(HagStoneRite.check(level, feet.above(), true, true) == HagStoneRite.Missing.NOT_WET,
				"Swimming over deep water the stone was not refused");
		Registry<Biome> biomes = level.registryAccess().lookupOrThrow(Registries.BIOME);
		for (String wet : List.of("bog", "quagmire", "sludge_mire", "hallowed_bog", "marsh", "fen", "dead_swamp", "lush_swamp",
				"swamp_woods", "wetland", "bayou", "floodplain", "muskeg")) {
			helper.assertTrue(biomes.getOrThrow(ResourceKey.create(Registries.BIOME, Jugcraft.id(wet))).is(HagStoneRite.LANDS),
					"jugcraft:" + wet + " is not a wetland for the stone");
		}
		helper.assertTrue(biomes.getOrThrow(Biomes.SWAMP).is(HagStoneRite.LANDS) && biomes.getOrThrow(Biomes.MANGROVE_SWAMP).is(HagStoneRite.LANDS),
				"Vanilla's swamps are not wetlands for the stone");
		helper.assertTrue(!biomes.getOrThrow(Biomes.PLAINS).is(HagStoneRite.LANDS) && !biomes.getOrThrow(Biomes.DESERT).is(HagStoneRite.LANDS)
				&& !biomes.getOrThrow(ResourceKey.create(Registries.BIOME, Jugcraft.id("volcano"))).is(HagStoneRite.LANDS),
				"The plains, the desert or the Volcano is a wetland for the stone");
		helper.assertTrue(HagStoneRite.mist(level, feet) == null, "A mist rose where nobody looked through a stone");
		helper.succeed();
	}

	/**
	 * The stone looked through in full when no instance can open (a game-test server has no lair dimension, and the one
	 * slot {@code lairs.instances} allows is taken if it has): the fen is full, the stone is kept, nobody moves and no mist
	 * rises. By day it is refused the same way.
	 */
	@GameTest
	public void theStoneUsesNothingWhenNoFenCanOpen(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		String instances = JugcraftConfig.textOption("lairs.instances");
		try {
			JugcraftConfig.setTextOption("lairs.instances", "1");
			Lairs.open(level.getServer(), FEN);  // null here; takes the only slot where there is a Lantern Fen
			helper.setBlock(STAND.below(), Blocks.MUD.defaultBlockState());
			helper.setBlock(STAND, Blocks.WATER.defaultBlockState());
			ServerPlayer player = player(helper, STAND.below(), 14 / 16.0);
			player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(JugcraftLairs.HAG_STONE, 2));
			Vec3 stood = player.position();
			HagStoneRite.Look look = HagStoneRite.look(level, player, player.getMainHandItem(), true, true);
			helper.assertTrue(look.missing() == HagStoneRite.Missing.FULL && look.instance() == null, "The fen was not full: " + look);
			helper.assertTrue(player.getMainHandItem().getCount() == 2, "The stone was used up: " + player.getMainHandItem());
			helper.assertTrue(player.level() == level && player.position().distanceTo(stood) < 1.0E-6 && player.getAttached(Lairs.VISIT) == null,
					"The player was moved");
			helper.assertTrue(HagStoneRite.mist(level, HagStoneRite.feet(player)) == null, "A mist rose into no fen");
			helper.assertTrue(HagStoneRite.look(level, player, player.getMainHandItem(), false, true).missing() == HagStoneRite.Missing.NOT_NIGHT
					&& player.getMainHandItem().getCount() == 2, "By day the stone was not refused, or was used up");
		} finally {
			JugcraftConfig.setTextOption("lairs.instances", instances);
			Lairs.reset();
		}
		helper.succeed();
	}

	/**
	 * A fen lamp burns at light 15, goes out (to no light) when put out, and a player lights it again with an empty hand; a
	 * lamp already burning or already out is left as it is. Like every lair fixture it cannot be broken, has no item and is
	 * in {@code #jugcraft:lair_fixtures}. The soul brazier still burns at 12.
	 */
	@GameTest
	public void theFenLampsBurnGoOutAndAreLitAgain(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		BlockPos lamp = new BlockPos(2, 2, 3);
		helper.setBlock(lamp, JugcraftLairs.FEN_LAMP.defaultBlockState());
		BlockPos at = helper.absolutePos(lamp);
		helper.assertTrue(helper.getBlockState(lamp).getValue(FenLampBlock.LIT) && helper.getBlockState(lamp).getLightEmission() == 15,
				"A fen lamp does not burn at 15 when placed");
		helper.assertTrue(!FenLampBlock.light(level, at), "A burning lamp was lit again");
		helper.assertTrue(FenLampBlock.snuff(level, at) && !helper.getBlockState(lamp).getValue(FenLampBlock.LIT)
				&& helper.getBlockState(lamp).getLightEmission() == 0, "Put out, the lamp still gives light");
		helper.assertTrue(!FenLampBlock.snuff(level, at), "A lamp already out was put out again");
		ServerPlayer player = player(helper, STAND, 0.0);
		BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(at), Direction.SOUTH, at, false);
		InteractionResult used = player.gameMode.useItemOn(player, level, ItemStack.EMPTY, InteractionHand.MAIN_HAND, hit);
		helper.assertTrue(used.consumesAction() && helper.getBlockState(lamp).getValue(FenLampBlock.LIT)
				&& helper.getBlockState(lamp).getLightEmission() == 15, "A player's hand did not light the lamp again: " + used);
		helper.assertTrue(JugcraftLairs.FEN_LAMP.defaultBlockState().getDestroySpeed(level, at) < 0.0F, "A fen lamp can be broken");
		helper.assertTrue(JugcraftLairs.FEN_LAMP.asItem() == Items.AIR, "A fen lamp has an item");
		TagKey<Block> fixtures = TagKey.create(Registries.BLOCK, Jugcraft.id("lair_fixtures"));
		helper.assertTrue(JugcraftLairs.FEN_LAMP.defaultBlockState().is(fixtures), "A fen lamp is not a lair fixture");
		helper.setBlock(lamp, JugcraftLairs.LAIR_BRAZIER.defaultBlockState());
		helper.assertTrue(helper.getBlockState(lamp).getLightEmission() == 12, "The soul brazier does not burn at 12");
		helper.setBlock(lamp, JugcraftLairs.LAIR_BRAZIER.defaultBlockState().setValue(LairBrazierBlock.LIT, false));
		helper.assertTrue(helper.getBlockState(lamp).getLightEmission() == 0, "A soul brazier gone out gives light");
		helper.succeed();
	}
}
