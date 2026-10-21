package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.config.JugcraftConfig;
import io.github.jimbozoomer.jugcraft.lair.AltarKeystoneRite;
import io.github.jimbozoomer.jugcraft.lair.CydonianSanctum;
import io.github.jimbozoomer.jugcraft.lair.JugcraftLairs;
import io.github.jimbozoomer.jugcraft.lair.Lair;
import io.github.jimbozoomer.jugcraft.lair.LairRules;
import io.github.jimbozoomer.jugcraft.lair.Lairs;
import io.github.jimbozoomer.jugcraft.lair.StarDialBlock;
import io.github.jimbozoomer.jugcraft.lair.StarDialBlock.Stars;
import io.github.jimbozoomer.jugcraft.lair.StarDoorBlock;
import io.github.jimbozoomer.jugcraft.lair.StarDoorBlock.Figure;
import io.github.jimbozoomer.jugcraft.world.AdamOndiAhman;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
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
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * The Cydonian Sanctum and the Altar Keystone (docs/features/cydonian-sanctum.md), as far as a game-test server can show
 * them. Its world is built from the flat preset alone, so it has no lair dimension: the Sanctum's own files load (dimension
 * type, biome, template and the dust vents' damage), the keystone asks for night and uses nothing when refused or when no
 * instance can open, the star dials turn and the Star Door's figures burn as they are matched and the door crumbles open,
 * and the dust vents keep their rhythm and strike. Everything that needs the Sanctum itself (the altars kindling, stepping
 * through the red mist onto Mars, the thin air, the gallery and the crater, the Red Mist home) runs in a real world, in
 * {@link CydonianSanctumClientGameTests}.
 */
public class CydonianSanctumGameTests {
	private static final Lair SANCTUM = Lair.CYDONIAN_SANCTUM;
	private static final BlockPos ALTAR = new BlockPos(2, 2, 2);
	private static final BlockPos STAND = new BlockPos(4, 2, 2);
	/**
	 * A new player cannot be hurt until its client has loaded the world, or for 60 ticks; a mock player has no client, so
	 * a test that hurts one waits this long first.
	 */
	private static final int LOADING_TICKS = 80;

	private static ServerPlayer player(GameTestHelper helper, BlockPos standAt) {
		ServerPlayer player = helper.makeMockServerPlayerInLevel();
		player.setGameMode(GameType.SURVIVAL);
		BlockPos absolute = helper.absolutePos(standAt);
		player.snapTo(absolute.getX() + 0.5, absolute.getY(), absolute.getZ() + 0.5, 90.0F, 0.0F);
		return player;
	}

	/** An instance's origin for the test: the door's west column, floor row and plane fall on the test's own (1, 1, 1). */
	private static BlockPos origin(GameTestHelper helper) {
		return helper.absolutePos(new BlockPos(1, 1, 1)).offset(-CydonianSanctum.DOOR_WEST, -CydonianSanctum.STAND, -CydonianSanctum.DOOR_Z);
	}

	/** Builds the Star Door as the template has it, its figures dark, at the test's origin. */
	private static void door(GameTestHelper helper, BlockPos origin) {
		ServerLevel level = helper.getLevel();
		BlockState plain = JugcraftLairs.STAR_DOOR.defaultBlockState();
		for (BlockPos at : CydonianSanctum.doorCells(origin)) {
			level.setBlock(at, plain, Block.UPDATE_CLIENTS);
		}
		for (Figure figure : Figure.values()) {
			if (figure != Figure.PLAIN) {
				level.setBlock(CydonianSanctum.figure(origin, figure), plain.setValue(StarDoorBlock.FIGURE, figure), Block.UPDATE_CLIENTS);
			}
		}
	}

	/** The Cydonian Sanctum's dimension type and biome, and the dust vents' damage type, load from their data pack files. */
	@GameTest
	public void cydonianSanctumDimensionFilesLoad(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		helper.assertTrue(level.registryAccess().lookupOrThrow(Registries.DIMENSION_TYPE).containsKey(SANCTUM.dimension.identifier()),
				"No dimension type " + SANCTUM.dimension.identifier());
		helper.assertTrue(level.registryAccess().lookupOrThrow(Registries.BIOME).containsKey(SANCTUM.dimension.identifier()),
				"No biome " + SANCTUM.dimension.identifier());
		helper.assertTrue(level.registryAccess().lookupOrThrow(Registries.DAMAGE_TYPE).containsKey(CydonianSanctum.VENT_DAMAGE_TYPE),
				"No damage type " + CydonianSanctum.VENT_DAMAGE_TYPE.identifier());
		helper.succeed();
	}

	/**
	 * The template loads in the running game with the star dials, the Star Door, the dust vents, the star stones and the Red
	 * Mist (DataVersion the game's), at the size tools/cydonian_sanctum.py gives Lair.CYDONIAN_SANCTUM, and with nothing in
	 * it that changes by itself once placed.
	 */
	@GameTest
	public void cydonianSanctumTemplateLoads(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		StructureTemplate template = Lairs.template(level, SANCTUM);
		helper.assertTrue(template != null, "No template " + SANCTUM.template);
		helper.assertTrue(template.getSize().equals(new Vec3i(SANCTUM.width, SANCTUM.height, SANCTUM.length)),
				"The template is " + template.getSize() + ", not the Lair's size");
		Identifier path = Identifier.fromNamespaceAndPath(SANCTUM.template.getNamespace(), "structure/" + SANCTUM.template.getPath() + ".nbt");
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
		for (String wanted : List.of("jugcraft:star_dial", "jugcraft:star_door", "jugcraft:dust_vent", "jugcraft:star_stone",
				"jugcraft:red_mist", "minecraft:red_sand", "minecraft:light")) {
			helper.assertTrue(names.contains(wanted), "The template has no " + wanted + ": " + names);
		}
		for (String name : names) {
			helper.assertTrue(!name.endsWith("_leaves") && !name.equals("minecraft:vine") && !name.equals("minecraft:water")
					&& !name.equals("minecraft:lava") && !name.endsWith("fire"), "The template has " + name + ", which would change once placed");
		}
		helper.succeed();
	}

	/**
	 * The keystone's checks, changing nothing: by day it is refused; at night in the Overworld it takes. Set by a player's
	 * hand when no instance can open (a game-test server has no lair dimension), the Sanctum is full: the keystone is kept
	 * and rested, no kindling starts, nobody moves and no mist opens. The lairs' switch and Adam-ondi-Ahman's each stop it.
	 */
	@GameTest(maxTicks = 100)
	public void theKeystoneWantsNightAndUsesNothingWhenRefused(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		helper.setBlock(ALTAR, AdamOndiAhman.ANCIENT_ALTAR.defaultBlockState());
		BlockPos altar = helper.absolutePos(ALTAR);
		ServerPlayer player = player(helper, STAND);
		ItemStack keystones = new ItemStack(JugcraftLairs.ALTAR_KEYSTONE, 2);
		player.setItemInHand(InteractionHand.MAIN_HAND, keystones);
		helper.assertTrue(AltarKeystoneRite.check(level, altar, false) == AltarKeystoneRite.Missing.NOT_NIGHT, "By day the keystone was not refused");
		helper.assertTrue(AltarKeystoneRite.check(level, altar, true) == null, "At night the keystone was refused: "
				+ AltarKeystoneRite.check(level, altar, true));
		AltarKeystoneRite.Setting day = AltarKeystoneRite.set(level, player, altar, player.getMainHandItem(), false);
		helper.assertTrue(day.missing() == AltarKeystoneRite.Missing.NOT_NIGHT && day.instance() == null, "By day the keystone did something");
		AltarKeystoneRite.Setting night = AltarKeystoneRite.set(level, player, altar, player.getMainHandItem(), true);
		helper.assertTrue(night.missing() == AltarKeystoneRite.Missing.FULL && night.instance() == null,
				"With no Sanctum to open, the keystone was not told it is full: " + night.missing());
		helper.assertTrue(player.getMainHandItem().getCount() == 2, "A refused keystone was used up: " + player.getMainHandItem());
		helper.assertTrue(!AltarKeystoneRite.kindling(level, altar), "A refused keystone kindled the altar");
		// By the player's own hand: whatever the hour, it is refused or full, and nothing is spent.
		Vec3 stood = player.position();
		BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(altar), Direction.UP, altar, false);
		InteractionResult used = player.gameMode.useItemOn(player, level, player.getMainHandItem(), InteractionHand.MAIN_HAND, hit);
		helper.assertTrue(used.consumesAction(), "Setting the keystone was not taken as the use: " + used);
		helper.assertTrue(player.getMainHandItem().getCount() == 2, "The keystone was used up: " + player.getMainHandItem());
		helper.assertTrue(player.getCooldowns().isOnCooldown(player.getMainHandItem()), "The keystone was not rested after its setting");
		helper.assertTrue(player.position().equals(stood), "The player was moved");
		helper.assertTrue(AltarKeystoneRite.mist(level, altar) == null && !AltarKeystoneRite.kindling(level, altar), "A mist opened");
		for (String feature : List.of(JugcraftLairs.FEATURE, AdamOndiAhman.FEATURE)) {
			boolean was = JugcraftConfig.isFeatureEnabled(feature);
			try {
				JugcraftConfig.setFeatureEnabled(feature, false);
				helper.assertTrue(AltarKeystoneRite.check(level, altar, true) == AltarKeystoneRite.Missing.DISABLED,
						"With " + feature + " off the keystone still took");
			} finally {
				JugcraftConfig.setFeatureEnabled(feature, was);
			}
		}
		helper.assertTrue(AltarKeystoneRite.useBlock(player, level, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atCenterOf(altar.below()),
				Direction.UP, altar.below(), false)) == InteractionResult.PASS, "The keystone was heard on a block that is no altar");
		helper.succeed();
	}

	/**
	 * A star dial turns through its constellations in order and back round; the Star Door's figures burn while a dial shows
	 * them and go dark again when none does, the door standing; when all three burn, every block of it is marked opening and
	 * crumbles away, the top row first, and a figure is never lit again.
	 */
	@GameTest(maxTicks = 120)
	public void theDialsLightTheStarDoorAndOpenIt(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		BlockPos dial = new BlockPos(7, 2, 3);
		helper.setBlock(dial, JugcraftLairs.STAR_DIAL.defaultBlockState().setValue(HorizontalDirectionalBlock.FACING, Direction.WEST)
				.setValue(StarDialBlock.STARS, Stars.PLOUGH));
		ServerPlayer player = player(helper, new BlockPos(6, 2, 3));
		for (Stars stars : List.of(Stars.HUNTER, Stars.LYRE, Stars.SWAN, Stars.SCORPION, Stars.CROWN, Stars.PLOUGH)) {
			helper.assertTrue(StarDialBlock.turn(level, helper.absolutePos(dial), player) == stars, "The dial did not turn to " + stars);
		}
		helper.assertTrue(JugcraftLairs.STAR_DIAL.defaultBlockState().is(LairRules.FIXTURES), "A star dial is not a fixture players may use");
		BlockPos origin = origin(helper);
		door(helper, origin);
		helper.assertTrue(!CydonianSanctum.match(level, origin, List.of(Stars.HUNTER, Stars.SWAN, Stars.PLOUGH), player),
				"The door opened with two figures matched");
		helper.assertTrue(lit(level, origin, Figure.HUNTER) && lit(level, origin, Figure.SWAN) && !lit(level, origin, Figure.CROWN),
				"The matched figures do not burn, or the third does");
		helper.assertTrue(level.getBlockState(CydonianSanctum.figure(origin, Figure.HUNTER)).getLightEmission() == StarDoorBlock.LIGHT,
				"A lit figure gives no light");
		CydonianSanctum.match(level, origin, List.of(Stars.LYRE, Stars.SWAN, Stars.PLOUGH), player);
		helper.assertTrue(!lit(level, origin, Figure.HUNTER) && lit(level, origin, Figure.SWAN), "The Hunter burns with no dial on it");
		helper.assertTrue(CydonianSanctum.match(level, origin, List.of(Stars.CROWN, Stars.SWAN, Stars.HUNTER), player),
				"All three matched did not open the door");
		for (BlockPos at : CydonianSanctum.doorCells(origin)) {
			helper.assertTrue(level.getBlockState(at).getValue(StarDoorBlock.OPENING), "A block of the door is not opening at " + at);
		}
		helper.assertTrue(!CydonianSanctum.match(level, origin, List.of(Stars.CROWN, Stars.SWAN, Stars.HUNTER), player),
				"An opening door opened again");
		helper.assertTrue(!CydonianSanctum.doorOpen(level, origin), "The door was gone at once");
		helper.succeedWhen(() -> helper.assertTrue(CydonianSanctum.doorOpen(level, origin), "The Star Door has not crumbled away"));
	}

	private static boolean lit(ServerLevel level, BlockPos origin, Figure figure) {
		BlockState state = level.getBlockState(CydonianSanctum.figure(origin, figure));
		return state.getBlock() instanceof StarDoorBlock && state.getValue(StarDoorBlock.LIT);
	}

	/**
	 * The dust vents' rhythm: each line hisses for its tell and then puffs, neighbouring lines half a period apart, never
	 * two neighbours together. A puff strikes a player standing on it for its damage, once (a second blow in the same puff
	 * is spared), and throws them south, back towards the gate chamber. Off Mars the thin air is not given, and a player
	 * keeps their own fall damage.
	 */
	@GameTest(maxTicks = LOADING_TICKS + 40)
	public void theDustVentsKeepTheirRhythmAndStrike(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		int lines = CydonianSanctum.VENT_LINES.length;
		for (int line = 0; line < lines; line++) {
			int start = CydonianSanctum.VENT_PHASES[line];
			helper.assertTrue(!CydonianSanctum.puffing(line, start), "Line " + line + " puffs with no tell");
			helper.assertTrue(CydonianSanctum.puffing(line, start + CydonianSanctum.VENT_HISS), "Line " + line + " does not puff after its tell");
			helper.assertTrue(!CydonianSanctum.puffing(line, start + CydonianSanctum.VENT_HISS + CydonianSanctum.VENT_PUFF),
					"Line " + line + " puffs too long");
			helper.assertTrue(CydonianSanctum.puffing(line, start + CydonianSanctum.VENT_HISS + CydonianSanctum.VENT_PERIOD),
					"Line " + line + " does not puff again a period later");
		}
		for (long now = 0; now < CydonianSanctum.VENT_PERIOD; now++) {
			for (int line = 0; line + 1 < lines; line++) {
				helper.assertTrue(!(CydonianSanctum.puffing(line, now) && CydonianSanctum.puffing(line + 1, now)),
						"Lines " + line + " and " + (line + 1) + " puff together at " + now);
			}
		}
		ServerPlayer player = player(helper, STAND);
		helper.runAfterDelay(LOADING_TICKS, () -> {
			player.setHealth(20.0F);
			double z = player.getDeltaMovement().z;
			helper.assertTrue(CydonianSanctum.puff(level, player), "A puff did not strike the player");
			helper.assertTrue(Math.abs(player.getHealth() - (20.0F - CydonianSanctum.VENT_DAMAGE)) < 0.01F, "A puff did not deal "
					+ CydonianSanctum.VENT_DAMAGE + ": health " + player.getHealth());
			helper.assertTrue(player.getDeltaMovement().z > z, "A puff did not throw the player south");
			helper.assertTrue(!CydonianSanctum.puff(level, player), "A second blow of the same puff struck");
			CydonianSanctum.air(player);
			helper.assertTrue(!CydonianSanctum.inSanctum(player) && !player.hasEffect(MobEffects.JUMP_BOOST),
					"Off Mars the player was given the thin air's jump");
			helper.assertTrue(player.getAttributeValue(Attributes.FALL_DAMAGE_MULTIPLIER) == 1.0, "Off Mars the player's falls were softened");
			player.discard();
			helper.succeed();
		});
	}
}
