package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.config.JugcraftConfig;
import io.github.jimbozoomer.jugcraft.lair.JugcraftLairs;
import io.github.jimbozoomer.jugcraft.lair.Lair;
import io.github.jimbozoomer.jugcraft.lair.Lairs;
import io.github.jimbozoomer.jugcraft.lair.LastRites;
import io.github.jimbozoomer.jugcraft.lair.MistGateEntity;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CandleBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * The lairs and the Hollow Acre (docs/features/hollow-acre.md), as far as a game-test server can show them. Its world
 * is built from the flat preset alone, so it has no lair dimension: the lair's own files load (dimension type, biome and
 * template), the Last Rites' checks hold, and rites that can open no instance use nothing. Everything that needs the
 * Hollow Acre itself (instances, coming and going, the rules, the edges, Grave Goods, closing) runs in a real world, in
 * {@link LairClientGameTests}.
 */
public class LairGameTests {
	private static final Lair ACRE = Lair.HOLLOW_ACRE;

	private static ServerPlayer player(GameTestHelper helper, BlockPos standAt, float yaw, float pitch) {
		ServerPlayer player = helper.makeMockServerPlayerInLevel();
		player.setGameMode(GameType.SURVIVAL);
		BlockPos absolute = helper.absolutePos(standAt);
		player.snapTo(absolute.getX() + 0.5, absolute.getY(), absolute.getZ() + 0.5, yaw, pitch);
		return player;
	}

	private static Block block(String id) {
		return BuiltInRegistries.BLOCK.getValue(Jugcraft.id(id));
	}

	/** The Hollow Acre's dimension type and biome load from their data pack files. */
	@GameTest
	public void hollowAcreDimensionFilesLoad(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		helper.assertTrue(level.registryAccess().lookupOrThrow(Registries.DIMENSION_TYPE).containsKey(ACRE.dimension.identifier()),
				"No dimension type " + ACRE.dimension.identifier());
		helper.assertTrue(level.registryAccess().lookupOrThrow(Registries.BIOME).containsKey(ACRE.dimension.identifier()),
				"No biome " + ACRE.dimension.identifier());
		helper.assertFalse(Lairs.isLair(level), "The Overworld is no lair");
		helper.succeed();
	}

	/**
	 * The template loads in the running game with its blocks (DataVersion the game's), at the size tools/hollow_acre.py
	 * gives Lair.HOLLOW_ACRE.
	 */
	@GameTest
	public void hollowAcreTemplateLoads(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		StructureTemplate template = Lairs.template(level, ACRE);
		helper.assertTrue(template != null, "No template " + ACRE.template);
		helper.assertTrue(template.getSize().equals(new Vec3i(ACRE.width, ACRE.height, ACRE.length)),
				"The template is " + template.getSize() + ", not the Lair's size");
		Identifier path = Identifier.fromNamespaceAndPath(ACRE.template.getNamespace(), "structure/" + ACRE.template.getPath() + ".nbt");
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
		for (String wanted : List.of("jugcraft:lych_gate", "jugcraft:lair_exit", "jugcraft:bone_throne", "jugcraft:blighted_soil",
				"jugcraft:lair_brazier", "jugcraft:black_wheat", "jugcraft:gothic_headstone")) {
			helper.assertTrue(names.contains(wanted), "The template has no " + wanted + ": " + names);
		}
		helper.succeed();
	}

	/**
	 * The Last Rites' checks, in order: a grave within reach, night, enough lit candles (a block of candles counting each
	 * of them), and a wreath on the grave. Each refusal names what is missing and changes nothing.
	 */
	@GameTest(structure = "jugcraft-test:arms_arena", padding = 8, maxAttempts = 5, requiredSuccesses = 5)
	public void theLastRitesNeedEveryStep(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		// Keep both range probes and their search volumes inside our 16x16 empty fixture. The old
		// at.offset(20, 0, 0) probe could find a neighbouring test's grave, depending on suite order.
		BlockPos grave = new BlockPos(6, 1, 6);
		helper.setBlock(grave, block("gothic_headstone").defaultBlockState());
		BlockPos at = helper.absolutePos(new BlockPos(6, 1, 4));
		helper.assertTrue(LastRites.check(level, helper.absolutePos(grave.offset(LastRites.GRAVE_RANGE + 1, 0, 0)), true)
				.missing() == LastRites.Missing.NO_GRAVE,
				"No grave within reach");
		helper.assertTrue(LastRites.check(level, helper.absolutePos(grave.offset(LastRites.GRAVE_RANGE, 0, 0)), false)
				.missing() == LastRites.Missing.NOT_NIGHT, "A grave exactly at the range limit must be found");
		helper.assertTrue(LastRites.check(level, at, false).missing() == LastRites.Missing.NOT_NIGHT, "Not by day");
		BlockState lit = Blocks.CANDLE.defaultBlockState().setValue(CandleBlock.LIT, true);
		helper.setBlock(grave.offset(-2, 0, 0), lit);
		helper.setBlock(grave.offset(2, 0, 0), lit);
		helper.setBlock(grave.offset(0, 0, 2), Blocks.CANDLE.defaultBlockState());  // unlit: it does not count
		LastRites.Ring ring = LastRites.check(level, at, true);
		helper.assertTrue(ring.missing() == LastRites.Missing.CANDLES && ring.candles() == 2, "Two lit candles are too few: " + ring);
		helper.setBlock(grave.offset(0, 0, 2), lit.setValue(CandleBlock.CANDLES, 2));
		ring = LastRites.check(level, at, true);
		helper.assertTrue(ring.missing() == LastRites.Missing.NO_WREATH && ring.candles() == 4, "Four candles, but no wreath: " + ring);
		helper.setBlock(grave.offset(0, 0, 1), JugcraftLairs.MOURNING_WREATH.defaultBlockState());
		ring = LastRites.check(level, at, true);
		helper.assertTrue(ring.missing() == null && helper.absolutePos(grave).equals(ring.grave()), "Every step done: " + ring);
		helper.assertBlockPresent(JugcraftLairs.MOURNING_WREATH, grave.offset(0, 0, 1));
		helper.succeed();
	}

	/**
	 * Rites done in full, but no instance can open (a game-test server has no lair dimension, and the one slot
	 * {@code lairs.instances} allows is taken if it has): the knell says the lairs are full, and nothing is used up and
	 * nobody moves.
	 */
	@GameTest
	public void theRitesUseNothingWhenNoLairCanOpen(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		String instances = JugcraftConfig.textOption("lairs.instances");
		try {
			JugcraftConfig.setTextOption("lairs.instances", "1");
			Lairs.open(level.getServer(), ACRE);  // null here; takes the only slot where there is a Hollow Acre
			BlockPos grave = new BlockPos(3, 1, 3);
			helper.setBlock(grave, block("gothic_headstone").defaultBlockState());
			BlockState lit = Blocks.CANDLE.defaultBlockState().setValue(CandleBlock.LIT, true).setValue(CandleBlock.CANDLES, 2);
			helper.setBlock(new BlockPos(1, 1, 3), lit);
			helper.setBlock(new BlockPos(5, 1, 3), lit);
			helper.setBlock(new BlockPos(3, 1, 4), JugcraftLairs.MOURNING_WREATH.defaultBlockState());
			ServerPlayer ringer = player(helper, new BlockPos(3, 1, 1), 0.0F, 0.0F);
			Vec3 stood = ringer.position();
			LastRites.Ring ring = LastRites.ring(level, ringer, true);
			helper.assertTrue(ring.missing() == LastRites.Missing.FULL && ring.instance() == null, "The rites opened a lair: " + ring);
			helper.assertBlockPresent(JugcraftLairs.MOURNING_WREATH, new BlockPos(3, 1, 4));
			helper.assertTrue(ringer.level() == level && ringer.position().distanceTo(stood) < 1.0E-6 && ringer.getAttached(Lairs.VISIT) == null,
					"The ringer was moved");
			helper.assertTrue(level.getEntitiesOfClass(MistGateEntity.class, new AABB(helper.absolutePos(grave)).inflate(3)).isEmpty(),
					"A gate opened");
		} finally {
			JugcraftConfig.setTextOption("lairs.instances", instances);
			Lairs.reset();
		}
		helper.succeed();
	}
}
