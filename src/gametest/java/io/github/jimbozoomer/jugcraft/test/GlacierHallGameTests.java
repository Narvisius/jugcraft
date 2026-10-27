package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.agriculture.HalloweenSeason;
import io.github.jimbozoomer.jugcraft.config.JugcraftConfig;
import io.github.jimbozoomer.jugcraft.lair.FrostHornRite;
import io.github.jimbozoomer.jugcraft.lair.JugcraftLairs;
import io.github.jimbozoomer.jugcraft.lair.Lair;
import io.github.jimbozoomer.jugcraft.lair.Lairs;
import io.github.jimbozoomer.jugcraft.lair.SpindleRite;
import io.github.jimbozoomer.jugcraft.lair.TrampledSnowBlock;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.Vec3i;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SnowLayerBlock;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.phys.Vec3;

/**
 * The Glacier Hall and the Frost Horn (docs/features/glacier-hall.md), as far as a game-test server can show them. Its
 * world is built from the flat preset alone, so it has no lair dimension: the hall's own files load (dimension type,
 * biome and template), the horn's checks hold and keep no season, a horn that can open no instance is not used up, every
 * refusal keeps and rests the horn, it is answered on any of its ground and on Peaceful, and the hall's snow and ice are
 * what its fight needs (slick glare ice, the trampled snow's half steps). Everything that needs the hall itself
 * (falling in through the snow, the whirl, following, the arch's Grey Mist, the instance closing) runs in a real world, in
 * {@link GlacierHallClientGameTests}.
 */
public class GlacierHallGameTests {
	private static final Lair HALL = Lair.GLACIER_HALL;
	private static final BlockPos STAND = new BlockPos(2, 2, 2);

	private static Block block(String id) {
		return BuiltInRegistries.BLOCK.getValue(Jugcraft.id(id));
	}

	private static ServerPlayer player(GameTestHelper helper, BlockPos standAt) {
		ServerPlayer player = helper.makeMockServerPlayerInLevel();
		player.setGameMode(GameType.SURVIVAL);
		BlockPos absolute = helper.absolutePos(standAt);
		player.snapTo(absolute.getX() + 0.5, absolute.getY(), absolute.getZ() + 0.5, 180.0F, 0.0F);
		return player;
	}

	/** The Glacier Hall's dimension type and biome load from their data pack files. */
	@GameTest
	public void glacierHallDimensionFilesLoad(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		helper.assertTrue(level.registryAccess().lookupOrThrow(Registries.DIMENSION_TYPE).containsKey(HALL.dimension.identifier()),
				"No dimension type " + HALL.dimension.identifier());
		helper.assertTrue(level.registryAccess().lookupOrThrow(Registries.BIOME).containsKey(HALL.dimension.identifier()),
				"No biome " + HALL.dimension.identifier());
		helper.succeed();
	}

	/**
	 * The template loads in the running game with every one of the hall's own blocks and the Grey Mist (DataVersion the
	 * game's), at the size tools/glacier_hall.py gives Lair.GLACIER_HALL.
	 */
	@GameTest
	public void glacierHallTemplateLoads(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		StructureTemplate template = Lairs.template(level, HALL);
		helper.assertTrue(template != null, "No template " + HALL.template);
		helper.assertTrue(template.getSize().equals(new Vec3i(HALL.width, HALL.height, HALL.length)),
				"The template is " + template.getSize() + ", not the Lair's size");
		Identifier path = Identifier.fromNamespaceAndPath(HALL.template.getNamespace(), "structure/" + HALL.template.getPath() + ".nbt");
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
		for (String wanted : List.of("drift_snow", "trampled_snow", "glare_ice", "giant_icicle", "mammoth_tusk", "frozen_hoard",
				"lair_exit")) {
			helper.assertTrue(names.contains("jugcraft:" + wanted), "The template has no " + wanted + ": " + names);
		}
		helper.succeed();
	}

	/**
	 * The horn's checks: not by day; at night, not on grass or stone; at night on a snow block, packed or blue ice, Jugcraft's
	 * winter snow or in a snow layer, it is answered (this is the Overworld). Checking changes nothing.
	 */
	@GameTest
	public void theHornNeedsTheNightAndTheSnow(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		BlockPos at = helper.absolutePos(STAND);
		helper.setBlock(STAND.below(), Blocks.SNOW_BLOCK.defaultBlockState());
		helper.assertTrue(FrostHornRite.check(level, at, false) == FrostHornRite.Missing.NOT_NIGHT, "By day the horn is answered");
		helper.assertTrue(FrostHornRite.check(level, at, true) == null, "At night on snow the horn is not answered: "
				+ FrostHornRite.check(level, at, true));
		for (Block ground : List.of(Blocks.GRASS_BLOCK, Blocks.STONE)) {
			helper.setBlock(STAND.below(), ground.defaultBlockState());
			helper.assertTrue(FrostHornRite.check(level, at, true) == FrostHornRite.Missing.NO_SNOW, "The horn is answered on " + ground);
		}
		for (Block ground : List.of(Blocks.PACKED_ICE, Blocks.BLUE_ICE, block("seasonal_snow"))) {
			helper.setBlock(STAND.below(), ground.defaultBlockState());
			helper.assertTrue(FrostHornRite.check(level, at, true) == null, "The horn is not answered on " + ground);
		}
		helper.setBlock(STAND.below(), Blocks.GRASS_BLOCK.defaultBlockState());
		helper.setBlock(STAND, Blocks.SNOW.defaultBlockState().setValue(SnowLayerBlock.LAYERS, 2));
		helper.assertTrue(FrostHornRite.check(level, at, true) == null, "The horn is not answered standing in a snow layer");
		helper.assertTrue(FrostHornRite.whirl(level, at) == null, "A whirl opened where nobody blew");
		helper.succeed();
	}

	/**
	 * The horn keeps no season (Concordance Chunk B, B-02, as the Glacier Hall's dossier says): the Halloween event over
	 * and lairs.off_season off, the spindle waits for the season but the horn is answered at night on snow.
	 */
	@GameTest
	public void theHornIsNoHalloweenRite(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		BlockPos at = helper.absolutePos(STAND);
		helper.setBlock(STAND.below(), Blocks.SNOW_BLOCK.defaultBlockState());
		HalloweenSeason.Mode mode = HalloweenSeason.mode();
		String offSeason = JugcraftConfig.textOption("lairs.off_season");
		FrostHornRite.Missing horn;
		SpindleRite.Missing spindle;
		// Switched back at once: the server runs other tests.
		try {
			HalloweenSeason.setMode(HalloweenSeason.Mode.OFF);
			JugcraftConfig.setTextOption("lairs.off_season", "off");
			horn = FrostHornRite.check(level, at, true);
			spindle = SpindleRite.check(level, at, true);
		} finally {
			HalloweenSeason.setMode(mode);
			JugcraftConfig.setTextOption("lairs.off_season", offSeason);
		}
		helper.assertTrue(spindle == SpindleRite.Missing.OFF_SEASON, "Out of season the spindle should wait (the settings took): " + spindle);
		helper.assertTrue(horn == null, "Out of season the horn was refused: " + horn);
		helper.succeed();
	}

	/**
	 * The horn blown in full when no instance can open (a game-test server has no lair dimension, and the one slot
	 * {@code lairs.instances} allows is taken if it has): the hall is full, the horn is kept, and nobody moves or is
	 * frosted. Off the snow it is refused the same way.
	 */
	@GameTest
	public void theHornUsesNothingWhenNoLairCanOpen(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		String instances = JugcraftConfig.textOption("lairs.instances");
		try {
			JugcraftConfig.setTextOption("lairs.instances", "1");
			Lairs.open(level.getServer(), HALL);  // null here; takes the only slot where there is a Glacier Hall
			helper.setBlock(STAND.below(), Blocks.SNOW_BLOCK.defaultBlockState());
			ServerPlayer player = player(helper, STAND);
			player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(JugcraftLairs.FROST_HORN, 2));
			Vec3 stood = player.position();
			FrostHornRite.Call call = FrostHornRite.blow(level, player, player.getMainHandItem(), true);
			helper.assertTrue(call.missing() == FrostHornRite.Missing.FULL && call.instance() == null, "The hall was not full: " + call);
			helper.assertTrue(player.getMainHandItem().getCount() == 2, "The horn was used up: " + player.getMainHandItem());
			helper.assertTrue(player.level() == level && player.position().distanceTo(stood) < 1.0E-6 && player.getAttached(Lairs.VISIT) == null,
					"The player was moved");
			helper.assertTrue(player.getTicksFrozen() == 0, "The player was frosted");
			helper.assertTrue(FrostHornRite.whirl(level, helper.absolutePos(STAND)) == null, "A whirl opened into no hall");
			helper.setBlock(STAND.below(), Blocks.STONE.defaultBlockState());
			helper.assertTrue(FrostHornRite.blow(level, player, player.getMainHandItem(), true).missing() == FrostHornRite.Missing.NO_SNOW
					&& player.getMainHandItem().getCount() == 2, "Blown off the snow, the horn was not refused, or was used up");
		} finally {
			JugcraftConfig.setTextOption("lairs.instances", instances);
			Lairs.reset();
		}
		helper.succeed();
	}

	/**
	 * Every refusal keeps the horn: by day, with the lairs switched off, and through the item itself off the snow, which still
	 * rests it (glacier-hall.md: "Every blow, answered or not, rests the horn"); nothing opens, moves or frosts. Each case
	 * returns before Lairs.open, so it holds whether or not this server has the hall's dimension.
	 */
	@GameTest
	public void everyRefusalKeepsTheHorn(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		helper.setBlock(STAND.below(), Blocks.SNOW_BLOCK.defaultBlockState());
		ServerPlayer player = player(helper, STAND);
		player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(JugcraftLairs.FROST_HORN, 2));
		Vec3 stood = player.position();
		int halls = Lairs.open(HALL).size();
		FrostHornRite.Call day = FrostHornRite.blow(level, player, player.getMainHandItem(), false);
		helper.assertTrue(day.missing() == FrostHornRite.Missing.NOT_NIGHT && day.instance() == null, "By day: " + day);
		untouched(helper, level, player, stood, halls, "By day");
		boolean lairs = JugcraftConfig.isFeatureEnabled(JugcraftLairs.FEATURE);
		try {
			JugcraftConfig.setFeatureEnabled(JugcraftLairs.FEATURE, false);
			FrostHornRite.Call off = FrostHornRite.blow(level, player, player.getMainHandItem(), true);
			helper.assertTrue(off.missing() == FrostHornRite.Missing.DISABLED && off.instance() == null, "Switched off: " + off);
			untouched(helper, level, player, stood, halls, "Switched off");
		} finally {
			JugcraftConfig.setFeatureEnabled(JugcraftLairs.FEATURE, lairs);
		}
		// Through the item, off the snow: refused whatever this server's hour (NOT_NIGHT or NO_SNOW), kept, and rested.
		helper.setBlock(STAND.below(), Blocks.STONE.defaultBlockState());
		helper.assertTrue(!player.getCooldowns().isOnCooldown(player.getMainHandItem()), "The horn was resting before it was blown");
		helper.assertTrue(JugcraftLairs.FROST_HORN.use(level, player, InteractionHand.MAIN_HAND) == InteractionResult.SUCCESS, "Blowing did not succeed");
		helper.assertTrue(FrostHornRite.check(level, helper.absolutePos(STAND), !level.isBrightOutside()) != null, "The horn was answered on stone");
		helper.assertTrue(player.getCooldowns().isOnCooldown(player.getMainHandItem()), "A refused blow did not rest the horn");
		untouched(helper, level, player, stood, halls, "Through the item, on stone");
		helper.succeed();
	}

	/** Nothing happened: the horn kept (two), the player where they stood, unfrosted, no visit, no hall or whirl opened. */
	private static void untouched(GameTestHelper helper, ServerLevel level, ServerPlayer player, Vec3 stood, int halls, String why) {
		helper.assertTrue(player.getMainHandItem().is(JugcraftLairs.FROST_HORN) && player.getMainHandItem().getCount() == 2, why + ": the horn was used up");
		helper.assertTrue(player.level() == level && player.position().distanceTo(stood) < 1.0E-6 && player.getAttached(Lairs.VISIT) == null,
				why + ": the player was moved");
		helper.assertTrue(player.getTicksFrozen() == 0, why + ": the player was frosted");
		helper.assertTrue(Lairs.open(HALL).size() == halls && FrostHornRite.whirl(level, helper.absolutePos(STAND)) == null, why + ": a hall or whirl opened");
	}

	/**
	 * At night the horn is answered on every block of #jugcraft:frost_horn_ground, and on Peaceful (yeti-king.md: the hall then
	 * opens empty). That it is answered all year, out of the Witching Season with lairs.off_season off, is
	 * {@link #theHornIsNoHalloweenRite}. The difficulty is set and restored within this one tick.
	 */
	@GameTest
	public void theHornIsAnsweredOnAnyOfItsGroundAndOnPeaceful(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		MinecraftServer server = level.getServer();
		BlockPos at = helper.absolutePos(STAND);
		int grounds = 0;
		for (Holder<Block> ground : BuiltInRegistries.BLOCK.getTagOrEmpty(FrostHornRite.GROUND)) {
			helper.setBlock(STAND.below(), ground.value().defaultBlockState());
			helper.assertTrue(FrostHornRite.check(level, at, true) == null, "At night the horn is not answered on " + ground.value());
			grounds++;
		}
		helper.assertTrue(grounds > 0, "#jugcraft:frost_horn_ground is empty");
		helper.setBlock(STAND.below(), Blocks.SNOW_BLOCK.defaultBlockState());
		Difficulty difficulty = level.getDifficulty();
		FrostHornRite.Missing peaceful;
		// Switched back at once: the server runs other tests.
		try {
			server.setDifficulty(Difficulty.PEACEFUL, true);
			peaceful = FrostHornRite.check(level, at, true);
		} finally {
			server.setDifficulty(difficulty, true);
		}
		helper.assertTrue(peaceful == null, "On Peaceful the horn is refused: " + peaceful);
		helper.succeed();
	}

	/**
	 * The hall's snow and ice: glare ice is as slick as blue ice and the snow is not; trampled snow is a whole block, or a
	 * half one for the snow ramp's half steps; the lair-only blocks cannot be broken and have no item.
	 */
	@GameTest
	public void theHallsSnowAndIce(GameTestHelper helper) {
		helper.assertTrue(JugcraftLairs.GLARE_ICE.getFriction() == Blocks.BLUE_ICE.getFriction(),
				"Glare ice's friction " + JugcraftLairs.GLARE_ICE.getFriction() + " is not blue ice's");
		helper.assertTrue(JugcraftLairs.DRIFT_SNOW.getFriction() == Blocks.SNOW_BLOCK.getFriction()
				&& JugcraftLairs.TRAMPLED_SNOW.getFriction() == Blocks.SNOW_BLOCK.getFriction(), "The hall's snow is slick");
		helper.setBlock(STAND, JugcraftLairs.TRAMPLED_SNOW.defaultBlockState().setValue(TrampledSnowBlock.HEIGHT, 1));
		BlockPos at = helper.absolutePos(STAND);
		double half = helper.getLevel().getBlockState(at).getCollisionShape(helper.getLevel(), at).max(Direction.Axis.Y);
		helper.assertTrue(Math.abs(half - 0.5) < 1.0E-6, "A half block of trampled snow is " + half + " high");
		helper.setBlock(STAND, JugcraftLairs.TRAMPLED_SNOW.defaultBlockState());
		double whole = helper.getLevel().getBlockState(at).getCollisionShape(helper.getLevel(), at).max(Direction.Axis.Y);
		helper.assertTrue(Math.abs(whole - 1.0) < 1.0E-6, "A whole block of trampled snow is " + whole + " high");
		for (Block fixture : List.of(JugcraftLairs.DRIFT_SNOW, JugcraftLairs.TRAMPLED_SNOW, JugcraftLairs.GLARE_ICE, JugcraftLairs.GIANT_ICICLE,
				JugcraftLairs.MAMMOTH_TUSK, JugcraftLairs.FROZEN_HOARD)) {
			helper.assertTrue(fixture.defaultBlockState().getDestroySpeed(helper.getLevel(), at) < 0.0F, fixture + " can be broken");
			helper.assertTrue(fixture.asItem() == Items.AIR, fixture + " has an item");
		}
		helper.succeed();
	}
}
