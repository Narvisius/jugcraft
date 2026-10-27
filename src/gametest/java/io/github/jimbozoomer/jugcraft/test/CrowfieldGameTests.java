package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.agriculture.JugcraftAgriculture;
import io.github.jimbozoomer.jugcraft.agriculture.ScarecrowBlockEntity;
import io.github.jimbozoomer.jugcraft.agriculture.TallDecorationBlock;
import io.github.jimbozoomer.jugcraft.config.JugcraftConfig;
import io.github.jimbozoomer.jugcraft.lair.Crowfield;
import io.github.jimbozoomer.jugcraft.lair.Crowfield.Hill;
import io.github.jimbozoomer.jugcraft.lair.CrowfieldCornBlock;
import io.github.jimbozoomer.jugcraft.lair.HarvestCrownRite;
import io.github.jimbozoomer.jugcraft.lair.HowlingTotemRite;
import io.github.jimbozoomer.jugcraft.lair.JugcraftLairs;
import io.github.jimbozoomer.jugcraft.lair.KingsGourdBlock;
import io.github.jimbozoomer.jugcraft.lair.Lair;
import io.github.jimbozoomer.jugcraft.lair.Lairs;
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
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * The Crowfield and the Harvest Crown (docs/features/crowfield.md), as far as a game-test server can show them. Its world
 * is built from the flat preset alone, so it has no lair dimension: the field's own files load (dimension type, biome and
 * template), the crown's checks hold (a scarecrow, night, the season, a head on it, the open sky over that head and the
 * harvest round it), a crown that can open no field is not used up, the crown is crafted from heirloom pumpkins, gold
 * and hay, and the field's fixtures keep their rules: its gourds are picked only while they ripen, its corn stands.
 * Everything that needs the field itself (the mist rising, coming to in the farmyard, following, picking a gourd there,
 * the Grey Mist, the instance closing) runs in a real world, in {@link CrowfieldClientGameTests}.
 */
public class CrowfieldGameTests {
	private static final Lair FIELD = Lair.CROWFIELD;
	/** Where the test's scarecrow stands (its lower half), with room round it for the harvest. */
	private static final BlockPos SCARECROW = new BlockPos(4, 2, 4);

	private static ServerPlayer player(GameTestHelper helper, BlockPos standAt) {
		ServerPlayer player = helper.makeMockServerPlayerInLevel();
		player.setGameMode(GameType.SURVIVAL);
		BlockPos absolute = helper.absolutePos(standAt);
		player.snapTo(absolute.getX() + 0.5, absolute.getY(), absolute.getZ() + 0.5, 180.0F, 0.0F);
		return player;
	}

	/** A scarecrow with its lower half at {@code at}, on grass; returns its upper half, which wears the head. */
	private static ScarecrowBlockEntity scarecrow(GameTestHelper helper, BlockPos at) {
		helper.setBlock(at.below(), Blocks.GRASS_BLOCK.defaultBlockState());
		Block scarecrow = JugcraftAgriculture.block("scarecrow");
		helper.setBlock(at, scarecrow.defaultBlockState().setValue(TallDecorationBlock.HALF, DoubleBlockHalf.LOWER));
		helper.setBlock(at.above(), scarecrow.defaultBlockState().setValue(TallDecorationBlock.HALF, DoubleBlockHalf.UPPER));
		return helper.getBlockEntity(at.above(), ScarecrowBlockEntity.class);
	}

	/** {@code count} blocks of harvest round the scarecrow, at its height, two to three blocks out. */
	private static void harvest(GameTestHelper helper, int count) {
		List<BlockState> crops = List.of(Blocks.PUMPKIN.defaultBlockState(), Blocks.JACK_O_LANTERN.defaultBlockState(),
				JugcraftAgriculture.block("white_pumpkin").defaultBlockState(), JugcraftAgriculture.block("warty_gourd").defaultBlockState(),
				Blocks.CARVED_PUMPKIN.defaultBlockState(), JugcraftAgriculture.block("kabocha_pumpkin").defaultBlockState(),
				JugcraftAgriculture.block("butternut_squash").defaultBlockState(), Blocks.PUMPKIN.defaultBlockState());
		int[][] spots = {{-2, -2}, {0, -3}, {2, -2}, {3, 0}, {2, 2}, {0, 3}, {-2, 2}, {-3, 0}};
		for (int i = 0; i < count; i++) {
			BlockPos at = SCARECROW.offset(spots[i][0], 0, spots[i][1]);
			helper.setBlock(at.below(), Blocks.GRASS_BLOCK.defaultBlockState());
			helper.setBlock(at, crops.get(i % crops.size()));
		}
	}

	/** The Crowfield's dimension type and biome load from their data pack files. */
	@GameTest
	public void crowfieldDimensionFilesLoad(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		helper.assertTrue(level.registryAccess().lookupOrThrow(Registries.DIMENSION_TYPE).containsKey(FIELD.dimension.identifier()),
				"No dimension type " + FIELD.dimension.identifier());
		helper.assertTrue(level.registryAccess().lookupOrThrow(Registries.BIOME).containsKey(FIELD.dimension.identifier()),
				"No biome " + FIELD.dimension.identifier());
		helper.succeed();
	}

	/**
	 * The template loads in the running game with the corn, the King's Gourds, the hay of his mound, his post, the farm
	 * gate, the pumpkins and heirlooms along the rows and the Grey Mist (DataVersion the game's), at the size
	 * tools/crowfield.py gives Lair.CROWFIELD, and with nothing in it that changes by itself once placed: no farmland, no
	 * stems, no vanilla leaves and no grass blocks.
	 */
	@GameTest
	public void crowfieldTemplateLoads(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		StructureTemplate template = Lairs.template(level, FIELD);
		helper.assertTrue(template != null, "No template " + FIELD.template);
		helper.assertTrue(template.getSize().equals(new Vec3i(FIELD.width, FIELD.height, FIELD.length)),
				"The template is " + template.getSize() + ", not the Lair's size");
		Identifier path = Identifier.fromNamespaceAndPath(FIELD.template.getNamespace(), "structure/" + FIELD.template.getPath() + ".nbt");
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
		for (String wanted : List.of("jugcraft:crowfield_corn", "jugcraft:kings_gourd", "jugcraft:lair_exit", "minecraft:hay_block",
				"minecraft:stripped_dark_oak_log", "minecraft:spruce_fence_gate", "minecraft:pumpkin", "minecraft:jack_o_lantern",
				"jugcraft:white_pumpkin")) {
			helper.assertTrue(names.contains(wanted), "The template has no " + wanted + ": " + names);
		}
		for (String name : names) {
			helper.assertTrue(!name.endsWith("_leaves") && !name.endsWith("_stem") && !name.equals("minecraft:farmland")
					&& !name.equals("minecraft:grass_block"), "The template has " + name + ", which would change once placed");
		}
		helper.succeed();
	}

	/**
	 * The crown's checks, changing nothing: a scarecrow wearing a head, its head under the open sky (a barrier, which
	 * cannot be seen, roofs nothing), at night and in season, with at least {@value HarvestCrownRite#HARVEST_MIN} pumpkins
	 * or gourds round it, is answered; anything but a scarecrow, by day, off-season, bare-headed, under a roof or among too
	 * little harvest it is refused, and answered again once the head is on, the roof gone or the harvest in. A pumpkin
	 * {@value HarvestCrownRite#HARVEST_RISE} blocks up still counts, one higher does not. No mist rises where nobody used a
	 * crown.
	 */
	@GameTest
	public void theCrownNeedsAHeadedScarecrowAmongTheHarvestAtNight(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		BlockPos at = helper.absolutePos(SCARECROW);
		BlockPos feet = helper.absolutePos(SCARECROW.south(2));
		ScarecrowBlockEntity scarecrow = scarecrow(helper, SCARECROW);
		harvest(helper, HarvestCrownRite.HARVEST_MIN);
		helper.assertTrue(HarvestCrownRite.check(level, at, feet, true, true) == HarvestCrownRite.Missing.BARE,
				"A bare-headed scarecrow was not refused: " + HarvestCrownRite.check(level, at, feet, true, true));
		scarecrow.setHead(new ItemStack(Items.CARVED_PUMPKIN));
		helper.assertTrue(HarvestCrownRite.harvest(level, at) == HarvestCrownRite.HARVEST_MIN, "The harvest counted "
				+ HarvestCrownRite.harvest(level, at) + ", not " + HarvestCrownRite.HARVEST_MIN);
		helper.assertTrue(HarvestCrownRite.check(level, at, feet, true, true) == null, "A headed scarecrow among the harvest at night "
				+ "was refused: " + HarvestCrownRite.check(level, at, feet, true, true));
		helper.assertTrue(HarvestCrownRite.check(level, at.above(), feet, true, true) == null, "Its upper half's place was refused, "
				+ "though the crown is checked from the lower half");
		helper.setBlock(SCARECROW.above(4), Blocks.BARRIER.defaultBlockState());
		helper.assertTrue(HarvestCrownRite.check(level, at, feet, true, true) == null, "Under a barrier the crown was refused");
		helper.setBlock(SCARECROW.above(4), Blocks.STONE.defaultBlockState());
		helper.assertTrue(HarvestCrownRite.check(level, at, feet, true, true) == HarvestCrownRite.Missing.ROOFED,
				"Under a roof the crown was not refused");
		helper.setBlock(SCARECROW.above(4), Blocks.AIR.defaultBlockState());
		helper.assertTrue(HarvestCrownRite.check(level, at, feet, false, true) == HarvestCrownRite.Missing.NOT_NIGHT,
				"By day the crown was not refused");
		helper.assertTrue(HarvestCrownRite.check(level, at, feet, true, false) == HarvestCrownRite.Missing.OFF_SEASON,
				"Off-season the crown was not refused");
		helper.assertTrue(HarvestCrownRite.check(level, at.east(), feet, true, true) == HarvestCrownRite.Missing.NOT_SCARECROW,
				"On no scarecrow the crown was not refused");
		// One pumpkin gone: too little harvest. Back two blocks up it counts again; three up it does not.
		BlockPos last = SCARECROW.offset(-3, 0, 0);
		helper.setBlock(last, Blocks.AIR.defaultBlockState());
		helper.assertTrue(HarvestCrownRite.check(level, at, feet, true, true) == HarvestCrownRite.Missing.NO_HARVEST,
				"Among too little harvest the crown was not refused");
		helper.setBlock(last.above(HarvestCrownRite.HARVEST_RISE), Blocks.PUMPKIN.defaultBlockState());
		helper.assertTrue(HarvestCrownRite.check(level, at, feet, true, true) == null, "A pumpkin " + HarvestCrownRite.HARVEST_RISE
				+ " blocks up did not count");
		helper.setBlock(last.above(HarvestCrownRite.HARVEST_RISE), Blocks.AIR.defaultBlockState());
		helper.setBlock(last.above(HarvestCrownRite.HARVEST_RISE + 1), Blocks.PUMPKIN.defaultBlockState());
		helper.assertTrue(HarvestCrownRite.check(level, at, feet, true, true) == HarvestCrownRite.Missing.NO_HARVEST,
				"A pumpkin " + (HarvestCrownRite.HARVEST_RISE + 1) + " blocks up counted");
		TagKey<Block> harvestTag = HarvestCrownRite.HARVEST;
		for (String crop : List.of("white_pumpkin", "jarrahdale_pumpkin", "cinderella_pumpkin", "red_kuri_pumpkin", "kabocha_pumpkin",
				"hand_carved_pumpkin", "butternut_squash", "acorn_squash", "warty_gourd", "bottle_gourd", "giant_pumpkin")) {
			helper.assertTrue(JugcraftAgriculture.block(crop).defaultBlockState().is(harvestTag), crop + " is not harvest to the crown");
		}
		for (Block crop : List.of(Blocks.PUMPKIN, Blocks.CARVED_PUMPKIN, Blocks.JACK_O_LANTERN)) {
			helper.assertTrue(crop.defaultBlockState().is(harvestTag), crop + " is not harvest to the crown");
		}
		helper.assertTrue(!Blocks.MELON.defaultBlockState().is(harvestTag) && !Blocks.HAY_BLOCK.defaultBlockState().is(harvestTag),
				"Melons or hay are harvest to the crown");
		helper.assertTrue(HarvestCrownRite.mist(level, feet) == null, "A mist rose where nobody used a crown");
		helper.succeed();
	}

	/**
	 * The crown used in full when no field can open (a game-test server has no lair dimension, and the one slot
	 * {@code lairs.instances} allows is taken if it has): the field is full, the crown is kept, nobody moves and no mist
	 * rises; the scarecrow keeps its head and the pumpkins stay. By day it is refused the same way. Used on the scarecrow
	 * from the hand, it is rested after its use, answered or not; used on anything else, it is kept and not rested.
	 */
	@GameTest
	public void theCrownUsesNothingWhenNoFieldCanOpen(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		String instances = JugcraftConfig.textOption("lairs.instances");
		try {
			JugcraftConfig.setTextOption("lairs.instances", "1");
			Lairs.open(level.getServer(), FIELD);  // null here; takes the only slot where there is a Crowfield
			ScarecrowBlockEntity scarecrow = scarecrow(helper, SCARECROW);
			scarecrow.setHead(new ItemStack(Items.CARVED_PUMPKIN));
			harvest(helper, HarvestCrownRite.HARVEST_MIN);
			BlockPos stand = SCARECROW.south(2);
			helper.setBlock(stand.below(), Blocks.GRASS_BLOCK.defaultBlockState());
			ServerPlayer player = player(helper, stand);
			player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(JugcraftLairs.HARVEST_CROWN, 2));
			Vec3 stood = player.position();
			BlockPos at = helper.absolutePos(SCARECROW);
			HarvestCrownRite.Crowning crowning = HarvestCrownRite.crown(level, player, at, player.getMainHandItem(), true, true);
			helper.assertTrue(crowning.missing() == HarvestCrownRite.Missing.FULL && crowning.instance() == null, "The field was not full: "
					+ crowning);
			helper.assertTrue(player.getMainHandItem().getCount() == 2, "The crown was used up: " + player.getMainHandItem());
			helper.assertTrue(player.level() == level && player.position().distanceTo(stood) < 1.0E-6 && player.getAttached(Lairs.VISIT) == null,
					"The player was moved");
			helper.assertTrue(HarvestCrownRite.mist(level, HowlingTotemRite.feet(player)) == null, "A mist rose into no field");
			helper.assertTrue(!scarecrow.head().isEmpty() && HarvestCrownRite.harvest(level, at) == HarvestCrownRite.HARVEST_MIN,
					"The scarecrow lost its head or the harvest was touched");
			helper.assertTrue(HarvestCrownRite.crown(level, player, at, player.getMainHandItem(), false, true).missing()
					== HarvestCrownRite.Missing.NOT_NIGHT && player.getMainHandItem().getCount() == 2, "By day the crown was not refused, or was used up");
			BlockPos stone = SCARECROW.east(1).north(2);
			helper.setBlock(stone, Blocks.STONE.defaultBlockState());
			BlockPos rock = helper.absolutePos(stone);
			InteractionResult onStone = player.gameMode.useItemOn(player, level, player.getMainHandItem(), InteractionHand.MAIN_HAND,
					new BlockHitResult(Vec3.atCenterOf(rock), Direction.UP, rock, false));
			helper.assertTrue(player.getMainHandItem().getCount() == 2 && !player.getCooldowns().isOnCooldown(player.getMainHandItem()),
					"Used on stone, the crown was used up or rested: " + onStone);
			InteractionResult used = player.gameMode.useItemOn(player, level, player.getMainHandItem(), InteractionHand.MAIN_HAND,
					new BlockHitResult(Vec3.atCenterOf(at), Direction.SOUTH, at, false));
			helper.assertTrue(used.consumesAction() && player.getMainHandItem().getCount() == 2
					&& player.getCooldowns().isOnCooldown(player.getMainHandItem()), "Used on the scarecrow, the crown was used up or not rested: " + used);
			helper.assertTrue(!scarecrow.head().isEmpty() && scarecrow.head().is(Items.CARVED_PUMPKIN),
					"Using the crown on the scarecrow changed its head");
		} finally {
			JugcraftConfig.setTextOption("lairs.instances", instances);
			Lairs.reset();
		}
		helper.succeed();
	}

	/** A Harvest Crown is crafted from three heirloom pumpkins (any, mixed), two gold ingots and a hay bale. */
	@GameTest
	public void theCrownIsCraftedFromHeirloomsGoldAndHay(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		List<List<String>> rows = List.of(List.of("white_pumpkin", "white_pumpkin", "white_pumpkin"),
				List.of("jarrahdale_pumpkin", "cinderella_pumpkin", "red_kuri_pumpkin"), List.of("kabocha_pumpkin", "white_pumpkin", "cinderella_pumpkin"));
		for (List<String> row : rows) {
			ItemStack gold = new ItemStack(Items.GOLD_INGOT);
			CraftingInput grid = CraftingInput.of(3, 2, List.of(new ItemStack(JugcraftAgriculture.item(row.get(0))),
					new ItemStack(JugcraftAgriculture.item(row.get(1))), new ItemStack(JugcraftAgriculture.item(row.get(2))), gold, new ItemStack(Items.HAY_BLOCK),
					gold.copy()));
			ItemStack made = level.getServer().getRecipeManager().getRecipeFor(RecipeType.CRAFTING, grid, level)
					.orElseThrow(() -> helper.assertionException(row + ", gold and hay craft nothing")).value().assemble(grid);
			helper.assertTrue(made.is(JugcraftLairs.HARVEST_CROWN) && made.getCount() == 1, row + ", gold and hay made " + made);
		}
		CraftingInput plain = CraftingInput.of(3, 2, List.of(new ItemStack(Items.PUMPKIN), new ItemStack(Items.PUMPKIN), new ItemStack(Items.PUMPKIN),
				new ItemStack(Items.GOLD_INGOT), new ItemStack(Items.HAY_BLOCK), new ItemStack(Items.GOLD_INGOT)));
		helper.assertTrue(level.getServer().getRecipeManager().getRecipeFor(RecipeType.CRAFTING, plain, level)
				.map(recipe -> !recipe.value().assemble(plain).is(JugcraftLairs.HARVEST_CROWN)).orElse(true), "Plain pumpkins made a Harvest Crown");
		helper.succeed();
	}

	/**
	 * A King's Gourd used by a player's hand while it ripens is picked: only its vine is left. Green, lit or already picked,
	 * using it changes nothing. Each stage's light is the table's. Like every lair fixture it cannot be broken, has no item
	 * and is in {@code #jugcraft:lair_fixtures}. Crowfield corn cannot be broken either, has no item, is no fixture, is seen
	 * through and is not walked through, and stands in tassel on top. Each gourd is found by its place in an instance, the
	 * ring of hills goes round in order, and the field, the farmyard and the nearest hill are where the layout puts them.
	 */
	@GameTest
	public void theKingsGourdsArePickedOnlyWhileTheyRipen(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		BlockPos gourd = new BlockPos(3, 2, 1);
		BlockPos at = helper.absolutePos(gourd);
		ServerPlayer player = player(helper, new BlockPos(3, 2, 3));
		BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(at), Direction.SOUTH, at, false);
		int[] lights = {0, 4, 8, 13, 0};
		for (int stage = KingsGourdBlock.GREEN; stage <= KingsGourdBlock.PICKED; stage++) {
			BlockState state = JugcraftLairs.KINGS_GOURD.defaultBlockState().setValue(HorizontalDirectionalBlock.FACING, Direction.SOUTH)
					.setValue(KingsGourdBlock.STAGE, stage);
			helper.setBlock(gourd, state);
			helper.assertTrue(helper.getBlockState(gourd).getLightEmission() == lights[stage], "Stage " + stage + " gives light "
					+ helper.getBlockState(gourd).getLightEmission() + ", not " + lights[stage]);
			InteractionResult used = player.gameMode.useItemOn(player, level, ItemStack.EMPTY, InteractionHand.MAIN_HAND, hit);
			int after = helper.getBlockState(gourd).getValue(KingsGourdBlock.STAGE);
			boolean ripening = stage == KingsGourdBlock.SWELLING || stage == KingsGourdBlock.RIPENING;
			helper.assertTrue(used.consumesAction() && after == (ripening ? KingsGourdBlock.PICKED : stage), "Stage " + stage + " used by a hand "
					+ "became stage " + after + ": " + used);
		}
		helper.setBlock(gourd, JugcraftLairs.KINGS_GOURD.defaultBlockState().setValue(KingsGourdBlock.STAGE, KingsGourdBlock.RIPENING));
		helper.assertTrue(KingsGourdBlock.pick(level, at) && !KingsGourdBlock.pick(level, at), "A ripening gourd was not picked once");
		helper.assertTrue(KingsGourdBlock.setStage(level, at, KingsGourdBlock.SWELLING)
				&& helper.getBlockState(gourd).getValue(KingsGourdBlock.STAGE) == KingsGourdBlock.SWELLING, "A picked gourd's hill did not sprout again");
		TagKey<Block> fixtures = TagKey.create(Registries.BLOCK, Jugcraft.id("lair_fixtures"));
		for (Block block : List.of(JugcraftLairs.KINGS_GOURD, JugcraftLairs.CROWFIELD_CORN)) {
			helper.assertTrue(block.defaultBlockState().getDestroySpeed(level, at) < 0.0F, block + " can be broken");
			helper.assertTrue(block.asItem() == Items.AIR, block + " has an item");
		}
		helper.assertTrue(JugcraftLairs.KINGS_GOURD.defaultBlockState().is(fixtures), "The King's Gourds are not lair fixtures");
		helper.assertTrue(!JugcraftLairs.CROWFIELD_CORN.defaultBlockState().is(fixtures), "Crowfield corn is a lair fixture");
		BlockPos corn = new BlockPos(5, 2, 5);
		helper.setBlock(corn, JugcraftLairs.CROWFIELD_CORN.defaultBlockState());
		helper.setBlock(corn.above(), JugcraftLairs.CROWFIELD_CORN.defaultBlockState().setValue(CrowfieldCornBlock.TOP, true));
		helper.assertTrue(!helper.getBlockState(corn).canOcclude(), "Crowfield corn hides what is behind it");
		helper.assertTrue(!helper.getBlockState(corn).getCollisionShape(level, helper.absolutePos(corn)).isEmpty(),
				"Crowfield corn can be walked through");
		helper.assertTrue(helper.getBlockState(corn.above()).getValue(CrowfieldCornBlock.TOP), "Crowfield corn stands in no tassel");
		BlockPos origin = FIELD.origin(0);
		for (Hill hill : Hill.values()) {
			helper.assertTrue(Crowfield.hillAt(origin, hill.at(origin)) == hill, "The " + hill.title() + " hill is not found by its place");
			BlockPos cell = Crowfield.HILLS[hill.ordinal()];
			helper.assertTrue(Crowfield.nearestHill(cell.getX() + 0.5, cell.getZ() + 0.5) == hill, "The " + hill.title() + " hill is not nearest itself");
			helper.assertTrue(hill.next().previous() == hill && Math.abs(cell.getX() - Crowfield.HILLS[hill.next().ordinal()].getX())
					+ Math.abs(cell.getZ() - Crowfield.HILLS[hill.next().ordinal()].getZ()) == Crowfield.HILL_SPACING,
					"The " + hill.title() + " hill's neighbour round the ring is not " + Crowfield.HILL_SPACING + " away");
			helper.assertTrue(Crowfield.inField(cell.getX() + 0.5, cell.getZ() + 0.5), "The " + hill.title() + " hill is not in the field");
		}
		helper.assertTrue(Crowfield.hillAt(origin, origin) == null, "A gourd was found where none grows");
		helper.assertTrue(Crowfield.inField(Crowfield.CENTRE_X, Crowfield.CENTRE_Z) && !Crowfield.inYard(Crowfield.CENTRE_X, Crowfield.CENTRE_Z),
				"The field's centre is not in the field, or is in the farmyard");
		helper.assertTrue(Crowfield.inYard(FIELD.arrival.x, FIELD.arrival.z) && !Crowfield.inField(FIELD.arrival.x, FIELD.arrival.z),
				"The arrival is not in the farmyard");
		helper.succeed();
	}
}
