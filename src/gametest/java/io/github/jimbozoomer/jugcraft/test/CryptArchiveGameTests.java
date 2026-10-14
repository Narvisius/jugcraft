package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.agriculture.JugcraftAgriculture;
import io.github.jimbozoomer.jugcraft.agriculture.LongDecorationBlock;
import io.github.jimbozoomer.jugcraft.agriculture.SarcophagusBlock;
import io.github.jimbozoomer.jugcraft.config.JugcraftConfig;
import io.github.jimbozoomer.jugcraft.lair.ArchiveLecternBlock;
import io.github.jimbozoomer.jugcraft.lair.ExhumationRite;
import io.github.jimbozoomer.jugcraft.lair.GraveSealBlock;
import io.github.jimbozoomer.jugcraft.lair.JugcraftLairs;
import io.github.jimbozoomer.jugcraft.lair.Lair;
import io.github.jimbozoomer.jugcraft.lair.Lairs;
import io.github.jimbozoomer.jugcraft.lair.NameStoneBlock;
import io.github.jimbozoomer.jugcraft.lair.NameStoneBlock.Glow;
import io.github.jimbozoomer.jugcraft.lair.NameStoneBlock.Rune;
import io.github.jimbozoomer.jugcraft.lair.PhylacteryBlock;
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
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * The Archive of Last Names and the Writ of Exhumation (docs/features/crypt-archive.md), as far as a game-test server can
 * show them. Its world is built from the flat preset alone, so it has no lair dimension: the archive's own files load
 * (dimension type, biome and template), the writ's checks hold (a stone sarcophagus, under a roof), a writ that can open no
 * instance is not used up and never opens the tomb's slots, and the archive's fixtures keep their rules. Everything that
 * needs the archive itself (the lid grinding aside, going down the stair, following, the Grey Mist, reading the scraped
 * names back, the instance closing) runs in a real world, in {@link CryptArchiveClientGameTests}.
 */
public class CryptArchiveGameTests {
	private static final Lair ARCHIVE = Lair.CRYPT_ARCHIVE;
	/** A deepslate sarcophagus lying north (foot to head) here, the player standing beside it. */
	private static final BlockPos HEAD = new BlockPos(2, 2, 2);
	private static final BlockPos FOOT = new BlockPos(2, 2, 3);
	private static final BlockPos STAND = new BlockPos(4, 2, 3);

	private static ServerPlayer player(GameTestHelper helper, BlockPos standAt) {
		ServerPlayer player = helper.makeMockServerPlayerInLevel();
		player.setGameMode(GameType.SURVIVAL);
		BlockPos absolute = helper.absolutePos(standAt);
		player.snapTo(absolute.getX() + 0.5, absolute.getY(), absolute.getZ() + 0.5, 90.0F, 0.0F);
		return player;
	}

	/** The most ticks a test waits for the sky light to settle after laying or lifting a roof. */
	private static final int LIGHT_TICKS = 60;

	/**
	 * Runs {@code then} once {@code settled} holds, looking each tick: the sky light under a roof laid (or lifted) in a test
	 * is worked out by the light engine a tick or more later, not as the blocks are set, and the writ asks whether the sky
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

	/** Whether the sky can be seen from over either half of the sarcophagus at HEAD and FOOT. */
	private static boolean skyOver(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		return level.canSeeSky(helper.absolutePos(HEAD).above()) || level.canSeeSky(helper.absolutePos(FOOT).above());
	}

	/** Lays a deepslate sarcophagus at HEAD and FOOT, and (if {@code roofed}) a roof of stone over both. */
	private static void tomb(GameTestHelper helper, boolean roofed) {
		BlockState foot = JugcraftAgriculture.block("deepslate_sarcophagus").defaultBlockState()
				.setValue(HorizontalDirectionalBlock.FACING, Direction.NORTH).setValue(LongDecorationBlock.PART, BedPart.FOOT);
		helper.setBlock(FOOT, foot);
		helper.setBlock(HEAD, foot.setValue(LongDecorationBlock.PART, BedPart.HEAD));
		for (int x = 1; x <= 3; x++) {
			for (int z = 1; z <= 4; z++) {
				helper.setBlock(new BlockPos(x, 5, z), roofed ? Blocks.STONE.defaultBlockState() : Blocks.AIR.defaultBlockState());
			}
		}
	}

	/** The Archive of Last Names' dimension type and biome load from their data pack files. */
	@GameTest
	public void cryptArchiveDimensionFilesLoad(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		helper.assertTrue(level.registryAccess().lookupOrThrow(Registries.DIMENSION_TYPE).containsKey(ARCHIVE.dimension.identifier()),
				"No dimension type " + ARCHIVE.dimension.identifier());
		helper.assertTrue(level.registryAccess().lookupOrThrow(Registries.BIOME).containsKey(ARCHIVE.dimension.identifier()),
				"No biome " + ARCHIVE.dimension.identifier());
		helper.succeed();
	}

	/**
	 * The template loads in the running game with the name-stones, the grave seals, the lectern, the phylactery, the
	 * sarcophagi, the throne, the soul lanterns and the Grey Mist (DataVersion the game's), at the size
	 * tools/crypt_archive.py gives Lair.CRYPT_ARCHIVE, and with nothing in it that changes by itself once placed.
	 */
	@GameTest
	public void cryptArchiveTemplateLoads(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		StructureTemplate template = Lairs.template(level, ARCHIVE);
		helper.assertTrue(template != null, "No template " + ARCHIVE.template);
		helper.assertTrue(template.getSize().equals(new Vec3i(ARCHIVE.width, ARCHIVE.height, ARCHIVE.length)),
				"The template is " + template.getSize() + ", not the Lair's size");
		Identifier path = Identifier.fromNamespaceAndPath(ARCHIVE.template.getNamespace(), "structure/" + ARCHIVE.template.getPath() + ".nbt");
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
		for (String wanted : List.of("jugcraft:name_stone", "jugcraft:grave_seal", "jugcraft:archive_lectern", "jugcraft:phylactery",
				"jugcraft:deepslate_sarcophagus", "jugcraft:bone_throne", "minecraft:soul_lantern", "jugcraft:lair_exit")) {
			helper.assertTrue(names.contains(wanted), "The template has no " + wanted + ": " + names);
		}
		for (String name : names) {
			helper.assertTrue(!name.endsWith("_leaves") && !name.equals("minecraft:vine") && !name.equals("minecraft:water")
					&& !name.equals("minecraft:lava") && !name.endsWith("fire"), "The template has " + name + ", which would change once placed");
		}
		helper.succeed();
	}

	/**
	 * The writ's checks, changing nothing: served on a stone sarcophagus under a roof, it takes; under the open sky over
	 * either half it is refused. Either half finds the head. The three stone sarcophagi are tombs it is served on; a coffin
	 * is not. No mist rises where nobody served a writ. The lid grinds aside and shut on both halves. Each check under a
	 * roof laid or lifted waits for the sky light to settle ({@link #whenSettled}).
	 */
	@GameTest(maxTicks = 200)
	public void theWritNeedsAStoneSarcophagusUnderARoof(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		tomb(helper, true);
		BlockPos head = helper.absolutePos(HEAD);
		BlockPos foot = helper.absolutePos(FOOT);
		helper.assertTrue(ExhumationRite.head(level.getBlockState(foot), foot).equals(head)
				&& ExhumationRite.head(level.getBlockState(head), head).equals(head), "Either half does not find the head");
		for (String stone : List.of("stone_brick", "deepslate", "blackstone")) {
			helper.assertTrue(JugcraftAgriculture.block(stone + "_sarcophagus").defaultBlockState().is(ExhumationRite.TOMBS),
					"The " + stone + " sarcophagus is not a tomb for the writ");
		}
		helper.assertTrue(!JugcraftAgriculture.block("coffin").defaultBlockState().is(ExhumationRite.TOMBS), "A coffin is a tomb for the writ");
		helper.assertTrue(ExhumationRite.mist(level, head) == null, "A mist rose where nobody served a writ");
		whenSettled(helper, () -> !skyOver(helper), "under the roof", () -> {
			helper.assertTrue(ExhumationRite.check(level, head) == null, "Under a roof the writ was refused: " + ExhumationRite.check(level, head));
			helper.setBlock(new BlockPos(2, 5, 3), Blocks.AIR.defaultBlockState());
			whenSettled(helper, () -> level.canSeeSky(foot.above()), "over the foot", () -> {
				helper.assertTrue(ExhumationRite.check(level, head) == ExhumationRite.Missing.OPEN_SKY,
						"With the sky over its foot the writ was not refused");
				tomb(helper, false);
				whenSettled(helper, () -> level.canSeeSky(head.above()), "over the head", () -> {
					helper.assertTrue(ExhumationRite.check(level, head) == ExhumationRite.Missing.OPEN_SKY,
							"Under the open sky the writ was not refused");
					helper.assertTrue(ExhumationRite.setLid(level, head, true) && level.getBlockState(head).getValue(SarcophagusBlock.OPEN)
							&& level.getBlockState(foot).getValue(SarcophagusBlock.OPEN), "The lid did not grind aside on both halves");
					helper.assertTrue(!ExhumationRite.setLid(level, head, true), "An open lid was opened again");
					helper.assertTrue(ExhumationRite.setLid(level, head, false) && !level.getBlockState(head).getValue(SarcophagusBlock.OPEN)
							&& !level.getBlockState(foot).getValue(SarcophagusBlock.OPEN), "The lid did not grind shut on both halves");
					helper.succeed();
				});
			});
		});
	}

	/**
	 * The writ served by a player's hand when no instance can open (a game-test server has no lair dimension, and the one
	 * slot {@code lairs.instances} allows is taken if it has): the archive is full, the writ is kept and rested, the tomb's
	 * slots are not opened, its lid stays shut, nobody moves and no mist rises. Under the open sky it is refused the same way. Each waits for the sky
	 * light to settle under the roof laid, and lifted ({@link #whenSettled}).
	 */
	@GameTest(maxTicks = 200)
	public void theWritUsesNothingWhenNoArchiveCanOpen(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		tomb(helper, true);
		whenSettled(helper, () -> !skyOver(helper), "under the roof", () -> {
			ServerPlayer player = player(helper, STAND);
			player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(JugcraftLairs.EXHUMATION_WRIT, 2));
			BlockPos foot = helper.absolutePos(FOOT);
			String instances = JugcraftConfig.textOption("lairs.instances");
			try {
				JugcraftConfig.setTextOption("lairs.instances", "1");
				Lairs.open(level.getServer(), ARCHIVE);  // null here; takes the only slot where there is an archive
				Vec3 stood = player.position();
				BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(foot), Direction.UP, foot, false);
				InteractionResult used = player.gameMode.useItemOn(player, level, player.getMainHandItem(), InteractionHand.MAIN_HAND, hit);
				helper.assertTrue(used.consumesAction(), "Serving the writ was not taken as the use: " + used);
				helper.assertTrue(player.containerMenu == player.inventoryMenu, "The writ opened the tomb's slots");
				helper.assertTrue(player.getMainHandItem().getCount() == 2, "The writ was used up: " + player.getMainHandItem());
				helper.assertTrue(player.getCooldowns().isOnCooldown(player.getMainHandItem()), "The writ was not rested after its serving");
				helper.assertTrue(!helper.getBlockState(HEAD).getValue(SarcophagusBlock.OPEN), "The lid ground aside into no archive");
				helper.assertTrue(player.level() == level && player.position().distanceTo(stood) < 1.0E-6 && player.getAttached(Lairs.VISIT) == null,
						"The player was moved");
				helper.assertTrue(ExhumationRite.mist(level, helper.absolutePos(HEAD)) == null, "A mist rose into no archive");
				ExhumationRite.Serving full = ExhumationRite.serve(level, player, foot, player.getMainHandItem());
				helper.assertTrue(full.missing() == ExhumationRite.Missing.FULL && full.instance() == null, "The archive was not full: " + full);
			} finally {
				JugcraftConfig.setTextOption("lairs.instances", instances);
				Lairs.reset();
			}
			tomb(helper, false);
			whenSettled(helper, () -> skyOver(helper), "under the open sky", () -> {
				helper.assertTrue(ExhumationRite.serve(level, player, foot, player.getMainHandItem()).missing() == ExhumationRite.Missing.OPEN_SKY
						&& player.getMainHandItem().getCount() == 2, "Under the open sky the writ was not refused, or was used up");
				helper.succeed();
			});
		});
	}

	/**
	 * The archive's fixtures keep their rules. A name-stone gives light 9 lit and none dark or erased, and changes only when
	 * told to; a phylactery gives light 10 whole and none cracked. None of the four can be broken or has an item. The grave
	 * seals and the lectern are lair fixtures, which a player may use: with no Crypt Lich to bind it, a seal stays whole when
	 * used, and the lectern, with no name-stones about it, reads and changes nothing. The name-stones and the phylactery
	 * are not fixtures.
	 */
	@GameTest
	public void theArchiveFixturesKeepTheirRules(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		BlockPos stone = new BlockPos(1, 2, 1);
		helper.setBlock(stone, JugcraftLairs.NAME_STONE.defaultBlockState().setValue(NameStoneBlock.RUNE, Rune.QUILL));
		BlockPos at = helper.absolutePos(stone);
		helper.assertTrue(helper.getBlockState(stone).getValue(NameStoneBlock.GLOW) == Glow.DARK
				&& helper.getBlockState(stone).getLightEmission() == 0, "A name-stone is not dark when placed");
		helper.assertTrue(NameStoneBlock.setGlow(level, at, Glow.LIT) && helper.getBlockState(stone).getLightEmission() == NameStoneBlock.LIGHT,
				"A lit name-stone does not burn at " + NameStoneBlock.LIGHT);
		helper.assertTrue(!NameStoneBlock.setGlow(level, at, Glow.LIT), "A lit name-stone was lit again");
		helper.assertTrue(NameStoneBlock.setGlow(level, at, Glow.ERASED) && helper.getBlockState(stone).getLightEmission() == 0
				&& helper.getBlockState(stone).getValue(NameStoneBlock.RUNE) == Rune.QUILL, "A scraped name-stone gives light or loses its rune");
		BlockPos urn = new BlockPos(5, 2, 1);
		helper.setBlock(urn, JugcraftLairs.PHYLACTERY.defaultBlockState());
		helper.assertTrue(helper.getBlockState(urn).getLightEmission() == PhylacteryBlock.LIGHT, "A whole phylactery does not burn at "
				+ PhylacteryBlock.LIGHT);
		helper.assertTrue(PhylacteryBlock.setCracked(level, helper.absolutePos(urn), true) && helper.getBlockState(urn).getLightEmission() == 0,
				"A cracked phylactery gives light");
		TagKey<Block> fixtures = TagKey.create(Registries.BLOCK, Jugcraft.id("lair_fixtures"));
		for (Block block : List.of(JugcraftLairs.NAME_STONE, JugcraftLairs.GRAVE_SEAL, JugcraftLairs.ARCHIVE_LECTERN, JugcraftLairs.PHYLACTERY)) {
			helper.assertTrue(block.defaultBlockState().getDestroySpeed(level, at) < 0.0F, block + " can be broken");
			helper.assertTrue(block.asItem() == Items.AIR, block + " has an item");
		}
		helper.assertTrue(JugcraftLairs.GRAVE_SEAL.defaultBlockState().is(fixtures) && JugcraftLairs.ARCHIVE_LECTERN.defaultBlockState().is(fixtures),
				"The grave seals or the lectern are not lair fixtures");
		helper.assertTrue(!JugcraftLairs.NAME_STONE.defaultBlockState().is(fixtures) && !JugcraftLairs.PHYLACTERY.defaultBlockState().is(fixtures),
				"A name-stone or the phylactery is a lair fixture");
		BlockPos seal = new BlockPos(3, 3, 1);
		helper.setBlock(seal, JugcraftLairs.GRAVE_SEAL.defaultBlockState().setValue(HorizontalDirectionalBlock.FACING, Direction.SOUTH));
		ServerPlayer player = player(helper, new BlockPos(3, 2, 3));
		BlockPos sealAt = helper.absolutePos(seal);
		InteractionResult used = player.gameMode.useItemOn(player, level, ItemStack.EMPTY, InteractionHand.MAIN_HAND,
				new BlockHitResult(Vec3.atCenterOf(sealAt), Direction.SOUTH, sealAt, false));
		helper.assertTrue(used.consumesAction() && !helper.getBlockState(seal).getValue(GraveSealBlock.BROKEN),
				"With no Lich to bind it, the seal broke or the use was not taken: " + used);
		helper.assertTrue(GraveSealBlock.setBroken(level, sealAt, true) && helper.getBlockState(seal).getValue(GraveSealBlock.BROKEN)
				&& GraveSealBlock.setBroken(level, sealAt, false), "A seal does not break and mend when told to");
		BlockPos lectern = new BlockPos(5, 2, 4);
		helper.setBlock(lectern, JugcraftLairs.ARCHIVE_LECTERN.defaultBlockState());
		helper.assertTrue(ArchiveLecternBlock.read(level, helper.absolutePos(lectern), player).isEmpty()
				&& helper.getBlockState(stone).getValue(NameStoneBlock.GLOW) == Glow.ERASED,
				"The lectern read a name back where no name-stone of the archive lies");
		helper.succeed();
	}
}
