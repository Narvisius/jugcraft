package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.agriculture.JugcraftAgriculture;
import io.github.jimbozoomer.jugcraft.agriculture.Werewolves;
import io.github.jimbozoomer.jugcraft.config.JugcraftConfig;
import io.github.jimbozoomer.jugcraft.lair.HowlingTotemRite;
import io.github.jimbozoomer.jugcraft.lair.JugcraftLairs;
import io.github.jimbozoomer.jugcraft.lair.Lair;
import io.github.jimbozoomer.jugcraft.lair.Lairs;
import io.github.jimbozoomer.jugcraft.lair.MoonlitRing;
import io.github.jimbozoomer.jugcraft.lair.MoonlitRing.Bell;
import io.github.jimbozoomer.jugcraft.lair.WardenBellBlock;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Registry;
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
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * The Moonlit Ring and the Howling Totem (docs/features/moonlit-ring.md), as far as a game-test server can show them. Its
 * world is built from the flat preset alone, so it has no lair dimension: the ring's own files load (dimension type,
 * biome and template), the totem's checks hold (night, werewolf country, the open sky, woodland floor underfoot and no
 * wolfsbane near), a totem that can open no instance is not used up, the totem is crafted from a werewolf's pelt, bones
 * and silver, and the ring's fixtures keep their rules: its bells ring, swing and rest, its leaves stand. Everything that
 * needs the ring itself (the mist rising, coming to in the camp, following, ringing a bell there, the Grey Mist, the
 * instance closing) runs in a real world, in {@link MoonlitRingClientGameTests}.
 */
public class MoonlitRingGameTests {
	private static final Lair RING = Lair.MOONLIT_RING;
	private static final BlockPos STAND = new BlockPos(2, 2, 2);

	private static ServerPlayer player(GameTestHelper helper, BlockPos standAt, double up) {
		ServerPlayer player = helper.makeMockServerPlayerInLevel();
		player.setGameMode(GameType.SURVIVAL);
		BlockPos absolute = helper.absolutePos(standAt);
		player.snapTo(absolute.getX() + 0.5, absolute.getY() + up, absolute.getZ() + 0.5, 180.0F, 0.0F);
		return player;
	}

	/** The Moonlit Ring's dimension type and biome load from their data pack files. */
	@GameTest
	public void moonlitRingDimensionFilesLoad(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		helper.assertTrue(level.registryAccess().lookupOrThrow(Registries.DIMENSION_TYPE).containsKey(RING.dimension.identifier()),
				"No dimension type " + RING.dimension.identifier());
		helper.assertTrue(level.registryAccess().lookupOrThrow(Registries.BIOME).containsKey(RING.dimension.identifier()),
				"No biome " + RING.dimension.identifier());
		helper.succeed();
	}

	/**
	 * The template loads in the running game with the Warden Bells, the Moonwood Leaves, the giant oaks' trunks, the
	 * clearing's moss, the camp's werewolf rugs and the Grey Mist (DataVersion the game's), at the size
	 * tools/moonlit_ring.py gives Lair.MOONLIT_RING, and with nothing in it that changes by itself once placed: no vanilla
	 * leaves, no vines and no grass blocks.
	 */
	@GameTest
	public void moonlitRingTemplateLoads(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		StructureTemplate template = Lairs.template(level, RING);
		helper.assertTrue(template != null, "No template " + RING.template);
		helper.assertTrue(template.getSize().equals(new Vec3i(RING.width, RING.height, RING.length)),
				"The template is " + template.getSize() + ", not the Lair's size");
		Identifier path = Identifier.fromNamespaceAndPath(RING.template.getNamespace(), "structure/" + RING.template.getPath() + ".nbt");
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
		for (String wanted : List.of("jugcraft:warden_bell", "jugcraft:moonwood_leaves", "jugcraft:lair_exit", "minecraft:dark_oak_log",
				"minecraft:moss_block", "jugcraft:werewolf_rug")) {
			helper.assertTrue(names.contains(wanted), "The template has no " + wanted + ": " + names);
		}
		for (String name : names) {
			helper.assertTrue(name.equals("jugcraft:moonwood_leaves") || !name.endsWith("_leaves") && !name.equals("minecraft:vine")
					&& !name.equals("minecraft:grass_block"), "The template has " + name + ", which would change once placed");
		}
		helper.succeed();
	}

	/**
	 * The totem's checks, changing nothing: at night in werewolf country, under the open sky (a barrier, which cannot be
	 * seen, roofs nothing) and on woodland floor (grass, podzol, moss, under a moss carpet or a snow layer) it is answered;
	 * by day, out of the woods, under a roof (even of leaves), on stone, or with wolfsbane growing within the werewolves'
	 * ward reach it is refused, and answered again once the roof or the wolfsbane is gone. On a moss carpet a player's feet are taken in its cell. Werewolf country is the
	 * forests and taigas and the Grove, not the plains, the desert or a swamp. No mist rises where nobody used a totem.
	 */
	@GameTest
	public void theTotemNeedsOpenWoodlandInWerewolfCountryAtNight(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		BlockPos feet = helper.absolutePos(STAND);
		helper.setBlock(STAND.below(), Blocks.GRASS_BLOCK.defaultBlockState());
		int top = level.getHeight(Heightmap.Types.MOTION_BLOCKING, feet.getX(), feet.getZ());
		helper.assertTrue(HowlingTotemRite.check(level, feet, true, true) == null, "On grass under the open sky at night in the woods "
				+ "the totem was refused: " + HowlingTotemRite.check(level, feet, true, true) + " (feet at y " + feet.getY()
				+ ", the column's highest blocking block " + level.getBlockState(feet.atY(top - 1)) + " at y " + (top - 1) + ")");
		// A barrier hides no moon: invisible, it roofs nothing.
		helper.setBlock(STAND.above(3), Blocks.BARRIER.defaultBlockState());
		helper.assertTrue(HowlingTotemRite.check(level, feet, true, true) == null, "Under a barrier the totem was refused");
		helper.assertTrue(HowlingTotemRite.check(level, feet, false, true) == HowlingTotemRite.Missing.NOT_NIGHT, "By day the totem was not refused");
		helper.assertTrue(HowlingTotemRite.check(level, feet, true, false) == HowlingTotemRite.Missing.NOT_WOODS,
				"Out of the woods the totem was not refused");
		helper.setBlock(STAND.above(3), Blocks.OAK_LEAVES.defaultBlockState().setValue(LeavesBlock.PERSISTENT, true));
		helper.assertTrue(HowlingTotemRite.check(level, feet, true, true) == HowlingTotemRite.Missing.ROOFED,
				"Under a roof of leaves the totem was not refused");
		helper.setBlock(STAND.above(3), Blocks.STONE.defaultBlockState());
		helper.assertTrue(HowlingTotemRite.check(level, feet, true, true) == HowlingTotemRite.Missing.ROOFED,
				"Under a roof of stone the totem was not refused");
		helper.setBlock(STAND.above(3), Blocks.AIR.defaultBlockState());
		for (Block ground : List.of(Blocks.PODZOL, Blocks.MOSS_BLOCK, Blocks.DIRT, Blocks.COARSE_DIRT)) {
			helper.setBlock(STAND.below(), ground.defaultBlockState());
			helper.assertTrue(HowlingTotemRite.check(level, feet, true, true) == null, "On " + ground + " the totem was refused: "
					+ HowlingTotemRite.check(level, feet, true, true));
		}
		helper.setBlock(STAND, Blocks.MOSS_CARPET.defaultBlockState());
		ServerPlayer onCarpet = player(helper, STAND, 1 / 16.0);
		helper.assertTrue(HowlingTotemRite.feet(onCarpet).equals(feet), "On a moss carpet the feet are not taken in its cell: "
				+ HowlingTotemRite.feet(onCarpet));
		helper.assertTrue(HowlingTotemRite.check(level, feet, true, true) == null, "On a moss carpet the totem was refused: "
				+ HowlingTotemRite.check(level, feet, true, true));
		helper.setBlock(STAND.below(), Blocks.GRASS_BLOCK.defaultBlockState());
		helper.setBlock(STAND, Blocks.SNOW.defaultBlockState());
		helper.assertTrue(HowlingTotemRite.check(level, feet, true, true) == null, "In a snow layer on grass the totem was refused: "
				+ HowlingTotemRite.check(level, feet, true, true));
		helper.setBlock(STAND, Blocks.AIR.defaultBlockState());
		helper.setBlock(STAND.below(), Blocks.STONE.defaultBlockState());
		helper.assertTrue(HowlingTotemRite.check(level, feet, true, true) == HowlingTotemRite.Missing.NOT_GROUND, "On stone the totem was not refused");
		helper.setBlock(STAND.below(), Blocks.GRASS_BLOCK.defaultBlockState());
		BlockPos ward = STAND.east(Werewolves.WARD_REACH - 1);
		helper.setBlock(ward.below(), Blocks.GRASS_BLOCK.defaultBlockState());
		helper.setBlock(ward, JugcraftAgriculture.block(Werewolves.WOLFSBANE).defaultBlockState());
		helper.assertTrue(HowlingTotemRite.check(level, feet, true, true) == HowlingTotemRite.Missing.WOLFSBANE,
				"With wolfsbane growing near the totem was not refused");
		helper.setBlock(ward, Blocks.AIR.defaultBlockState());
		helper.assertTrue(HowlingTotemRite.check(level, feet, true, true) == null, "With the wolfsbane gone the totem was refused: "
				+ HowlingTotemRite.check(level, feet, true, true));
		Registry<Biome> biomes = level.registryAccess().lookupOrThrow(Registries.BIOME);
		for (var woods : List.of(Biomes.FOREST, Biomes.DARK_FOREST, Biomes.BIRCH_FOREST, Biomes.TAIGA, Biomes.SNOWY_TAIGA, Biomes.GROVE)) {
			helper.assertTrue(biomes.getOrThrow(woods).is(Werewolves.HAUNTS), woods.identifier() + " is not werewolf country");
		}
		for (var open : List.of(Biomes.PLAINS, Biomes.DESERT, Biomes.SWAMP)) {
			helper.assertTrue(!biomes.getOrThrow(open).is(Werewolves.HAUNTS), open.identifier() + " is werewolf country");
		}
		helper.assertTrue(HowlingTotemRite.mist(level, feet) == null, "A mist rose where nobody used a totem");
		helper.succeed();
	}

	/**
	 * The totem used in full when no instance can open (a game-test server has no lair dimension, and the one slot
	 * {@code lairs.instances} allows is taken if it has): the ring is full, the totem is kept, nobody moves and no mist
	 * rises. By day it is refused the same way. Used from the hand, it is rested after its use, answered or not.
	 */
	@GameTest
	public void theTotemUsesNothingWhenNoRingCanOpen(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		String instances = JugcraftConfig.textOption("lairs.instances");
		try {
			JugcraftConfig.setTextOption("lairs.instances", "1");
			Lairs.open(level.getServer(), RING);  // null here; takes the only slot where there is a Moonlit Ring
			helper.setBlock(STAND.below(), Blocks.GRASS_BLOCK.defaultBlockState());
			ServerPlayer player = player(helper, STAND, 0.0);
			player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(JugcraftLairs.HOWLING_TOTEM, 2));
			Vec3 stood = player.position();
			HowlingTotemRite.Howl howl = HowlingTotemRite.howl(level, player, player.getMainHandItem(), true, true);
			helper.assertTrue(howl.missing() == HowlingTotemRite.Missing.FULL && howl.instance() == null, "The ring was not full: " + howl);
			helper.assertTrue(player.getMainHandItem().getCount() == 2, "The totem was used up: " + player.getMainHandItem());
			helper.assertTrue(player.level() == level && player.position().distanceTo(stood) < 1.0E-6 && player.getAttached(Lairs.VISIT) == null,
					"The player was moved");
			helper.assertTrue(HowlingTotemRite.mist(level, HowlingTotemRite.feet(player)) == null, "A mist rose into no ring");
			helper.assertTrue(HowlingTotemRite.howl(level, player, player.getMainHandItem(), false, true).missing() == HowlingTotemRite.Missing.NOT_NIGHT
					&& player.getMainHandItem().getCount() == 2, "By day the totem was not refused, or was used up");
			InteractionResult used = player.gameMode.useItem(player, level, player.getMainHandItem(), InteractionHand.MAIN_HAND);
			helper.assertTrue(used.consumesAction() && player.getMainHandItem().getCount() == 2
					&& player.getCooldowns().isOnCooldown(player.getMainHandItem()), "Used from the hand, the totem was used up or not rested: " + used);
		} finally {
			JugcraftConfig.setTextOption("lairs.instances", instances);
			Lairs.reset();
		}
		helper.succeed();
	}

	/** A Howling Totem is crafted from a werewolf's pelt (any kind's), three bones and two silver ingots. */
	@GameTest
	public void theTotemIsCraftedFromAPeltBonesAndSilver(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		for (String pelt : List.of("werewolf_pelt", "snow_werewolf_pelt", "shadow_werewolf_pelt")) {
			ItemStack silver = new ItemStack(BuiltInRegistries.ITEM.getValue(Jugcraft.id("silver_ingot")));
			ItemStack bone = new ItemStack(Items.BONE);
			CraftingInput grid = CraftingInput.of(3, 3, List.of(silver, new ItemStack(BuiltInRegistries.ITEM.getValue(Jugcraft.id(pelt))), silver.copy(),
					ItemStack.EMPTY, bone, ItemStack.EMPTY, bone.copy(), bone.copy(), ItemStack.EMPTY));
			ItemStack made = level.getServer().getRecipeManager().getRecipeFor(RecipeType.CRAFTING, grid, level)
					.orElseThrow(() -> helper.assertionException("A " + pelt + ", bones and silver craft nothing")).value().assemble(grid);
			helper.assertTrue(made.is(JugcraftLairs.HOWLING_TOTEM) && made.getCount() == 1, "A " + pelt + ", bones and silver made " + made);
		}
		helper.succeed();
	}

	/**
	 * A Warden Bell rung by a player's hand swings and goes on swinging for {@value WardenBellBlock#SWING_TICKS} ticks;
	 * rung again while it swings, nothing changes; then it comes to rest and rings again. Like every lair fixture it cannot
	 * be broken, has no item and is in {@code #jugcraft:lair_fixtures}. Moonwood Leaves cannot be broken either, have no
	 * item, are no fixture, are seen through and are not walked through. Each bell is found by its place in an instance, and
	 * each clearing round its bell.
	 */
	@GameTest(maxTicks = 120)
	public void theWardenBellsRingAndRest(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		BlockPos bell = new BlockPos(3, 3, 1);
		helper.setBlock(bell, JugcraftLairs.WARDEN_BELL.defaultBlockState().setValue(HorizontalDirectionalBlock.FACING, Direction.SOUTH));
		BlockPos at = helper.absolutePos(bell);
		ServerPlayer player = player(helper, new BlockPos(3, 2, 3), 0.0);
		BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(at), Direction.SOUTH, at, false);
		InteractionResult used = player.gameMode.useItemOn(player, level, ItemStack.EMPTY, InteractionHand.MAIN_HAND, hit);
		helper.assertTrue(used.consumesAction() && helper.getBlockState(bell).getValue(WardenBellBlock.RINGING),
				"A player's hand did not ring the bell: " + used);
		helper.assertTrue(!WardenBellBlock.ring(level, at), "A bell still swinging was rung again");
		helper.runAfterDelay(WardenBellBlock.SWING_TICKS - 5, () -> {
			helper.assertTrue(helper.getBlockState(bell).getValue(WardenBellBlock.RINGING), "The bell stopped swinging early");
			InteractionResult again = player.gameMode.useItemOn(player, level, ItemStack.EMPTY, InteractionHand.MAIN_HAND, hit);
			helper.assertTrue(again.consumesAction() && helper.getBlockState(bell).getValue(WardenBellBlock.RINGING), "Rung again while "
					+ "swinging, the bell changed or the use was not taken: " + again);
		});
		helper.runAfterDelay(WardenBellBlock.SWING_TICKS + 2, () -> {
			helper.assertTrue(!helper.getBlockState(bell).getValue(WardenBellBlock.RINGING), "The bell never came to rest");
			helper.assertTrue(WardenBellBlock.ring(level, at) && helper.getBlockState(bell).getValue(WardenBellBlock.RINGING),
					"At rest, the bell did not ring again");
			TagKey<Block> fixtures = TagKey.create(Registries.BLOCK, Jugcraft.id("lair_fixtures"));
			for (Block block : List.of(JugcraftLairs.WARDEN_BELL, JugcraftLairs.MOONWOOD_LEAVES)) {
				helper.assertTrue(block.defaultBlockState().getDestroySpeed(level, at) < 0.0F, block + " can be broken");
				helper.assertTrue(block.asItem() == Items.AIR, block + " has an item");
			}
			helper.assertTrue(JugcraftLairs.WARDEN_BELL.defaultBlockState().is(fixtures), "The bells are not lair fixtures");
			helper.assertTrue(!JugcraftLairs.MOONWOOD_LEAVES.defaultBlockState().is(fixtures), "Moonwood Leaves are a lair fixture");
			BlockPos leaves = new BlockPos(5, 2, 5);
			helper.setBlock(leaves, JugcraftLairs.MOONWOOD_LEAVES.defaultBlockState());
			helper.assertTrue(!helper.getBlockState(leaves).canOcclude(), "Moonwood Leaves hide what is behind them");
			helper.assertTrue(!helper.getBlockState(leaves).getCollisionShape(level, helper.absolutePos(leaves)).isEmpty(),
					"Moonwood Leaves can be walked through");
			BlockPos origin = RING.origin(0);
			for (Bell each : Bell.values()) {
				helper.assertTrue(MoonlitRing.bellAt(origin, each.at(origin)) == each, "The " + each.title() + " bell is not found by its place");
				helper.assertTrue(MoonlitRing.gladeAt(each.gladeX(), each.gladeZ()) == each
						&& each.inGlade(MoonlitRing.BELLS[each.ordinal()].getX() + 0.5, MoonlitRing.BELLS[each.ordinal()].getZ() + 0.5),
						"The " + each.title() + " bell's clearing is not round it");
			}
			helper.assertTrue(MoonlitRing.bellAt(origin, origin) == null, "A bell was found where none hangs");
			helper.assertTrue(MoonlitRing.inRing(MoonlitRing.CENTRE_X, MoonlitRing.CENTRE_Z) && !MoonlitRing.inCamp(MoonlitRing.CENTRE_X, MoonlitRing.CENTRE_Z)
					&& MoonlitRing.inCamp(RING.arrival.x, RING.arrival.z) && !MoonlitRing.inRing(RING.arrival.x, RING.arrival.z),
					"The clearing or the camp is not where the arrival and the centre say");
			helper.succeed();
		});
	}
}
