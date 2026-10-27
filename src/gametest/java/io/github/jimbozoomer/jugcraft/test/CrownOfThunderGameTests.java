package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.config.JugcraftConfig;
import io.github.jimbozoomer.jugcraft.lair.CrownOfThunder;
import io.github.jimbozoomer.jugcraft.lair.CrownOfThunder.Rod;
import io.github.jimbozoomer.jugcraft.lair.JugcraftLairs;
import io.github.jimbozoomer.jugcraft.lair.Lair;
import io.github.jimbozoomer.jugcraft.lair.Lairs;
import io.github.jimbozoomer.jugcraft.lair.StormRodBlock;
import io.github.jimbozoomer.jugcraft.lair.ThunderVaneRite;
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
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * The Crown of Thunder and the Thunder Vane (docs/features/crown-of-thunder.md), as far as a game-test server can show
 * them. Its world is built from the flat preset alone, so it has no lair dimension: the Crown's own files load (dimension
 * type, biome and template), the vane's checks hold (a storm, the mountains, the height and the open sky), a vane that can
 * open no instance is not used up, the vane is crafted from feathers, a lightning rod, copper and a compass, and the
 * Crown's fixtures keep their rules: its Storm Rods charge, burn and calm, its nest is solid and its clouds are not.
 * Everything that needs the Crown itself (the mist tearing open, coming down on the terrace, following, the ledges, the
 * Grey Mist, the instance closing) runs in a real world, in {@link CrownOfThunderClientGameTests}.
 */
public class CrownOfThunderGameTests {
	private static final Lair CROWN = Lair.CROWN_OF_THUNDER;
	private static final BlockPos STAND = new BlockPos(2, 2, 2);

	/** A player standing at height {@code y} over {@code standAt}'s column (the vane wants {@value ThunderVaneRite#MIN_Y}). */
	private static ServerPlayer player(GameTestHelper helper, BlockPos standAt, int y) {
		ServerPlayer player = helper.makeMockServerPlayerInLevel();
		player.setGameMode(GameType.SURVIVAL);
		BlockPos absolute = helper.absolutePos(standAt);
		player.snapTo(absolute.getX() + 0.5, y, absolute.getZ() + 0.5, 180.0F, 0.0F);
		return player;
	}

	/** The Crown of Thunder's dimension type and biome load from their data pack files. */
	@GameTest
	public void crownOfThunderDimensionFilesLoad(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		helper.assertTrue(level.registryAccess().lookupOrThrow(Registries.DIMENSION_TYPE).containsKey(CROWN.dimension.identifier()),
				"No dimension type " + CROWN.dimension.identifier());
		helper.assertTrue(level.registryAccess().lookupOrThrow(Registries.BIOME).containsKey(CROWN.dimension.identifier()),
				"No biome " + CROWN.dimension.identifier());
		helper.succeed();
	}

	/**
	 * The template loads in the running game with the Storm Rods, the nest's woven branches, the storm clouds, the plinths'
	 * chiseled stone, the terrace's stairs and the Grey Mist (DataVersion the game's), at the size tools/crown_of_thunder.py
	 * gives Lair.CROWN_OF_THUNDER, and with nothing in it that changes by itself once placed: no leaves, no vines and no
	 * grass blocks.
	 */
	@GameTest
	public void crownOfThunderTemplateLoads(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		StructureTemplate template = Lairs.template(level, CROWN);
		helper.assertTrue(template != null, "No template " + CROWN.template);
		helper.assertTrue(template.getSize().equals(new Vec3i(CROWN.width, CROWN.height, CROWN.length)),
				"The template is " + template.getSize() + ", not the Lair's size");
		Identifier path = Identifier.fromNamespaceAndPath(CROWN.template.getNamespace(), "structure/" + CROWN.template.getPath() + ".nbt");
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
		for (String wanted : List.of("jugcraft:storm_rod", "jugcraft:roc_nest", "jugcraft:storm_cloud", "jugcraft:lair_exit",
				"minecraft:chiseled_stone_bricks", "minecraft:stone_brick_stairs", "minecraft:moss_block")) {
			helper.assertTrue(names.contains(wanted), "The template has no " + wanted + ": " + names);
		}
		for (String name : names) {
			helper.assertTrue(!name.endsWith("_leaves") && !name.equals("minecraft:vine") && !name.equals("minecraft:grass_block"),
					"The template has " + name + ", which would change once placed");
		}
		helper.succeed();
	}

	/**
	 * The vane's checks, changing nothing: in a storm, in the mountains, at a height of {@value ThunderVaneRite#MIN_Y} or
	 * more, under the open sky (a barrier, which cannot be seen, roofs nothing) it is answered; in fair weather, out of the
	 * mountains, a block too low, or under a roof it is refused, and answered again once the roof is gone. The mountains
	 * are vanilla's peaks, slopes and windswept hills and Jugcraft's high country, not the plains, the desert, the ocean
	 * or a swamp. No mist tears open where nobody raised a vane.
	 */
	@GameTest
	public void theVaneNeedsAStormHighInTheMountains(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		BlockPos high = helper.absolutePos(STAND).atY(ThunderVaneRite.MIN_Y);
		BlockPos roof = high.above(3);
		try {
			helper.assertTrue(ThunderVaneRite.check(level, high, true, true) == null, "High in the mountains in a storm the vane was "
					+ "refused: " + ThunderVaneRite.check(level, high, true, true));
			helper.assertTrue(ThunderVaneRite.check(level, high, false, true) == ThunderVaneRite.Missing.FAIR,
					"In fair weather the vane was not refused");
			helper.assertTrue(ThunderVaneRite.check(level, high, true, false) == ThunderVaneRite.Missing.NOT_MOUNTAINS,
					"Out of the mountains the vane was not refused");
			helper.assertTrue(ThunderVaneRite.check(level, high.below(), true, true) == ThunderVaneRite.Missing.LOW,
					"A block below " + ThunderVaneRite.MIN_Y + " the vane was not refused");
			level.setBlock(roof, Blocks.BARRIER.defaultBlockState(), Block.UPDATE_ALL);
			helper.assertTrue(ThunderVaneRite.check(level, high, true, true) == null, "Under a barrier the vane was refused");
			level.setBlock(roof, Blocks.STONE.defaultBlockState(), Block.UPDATE_ALL);
			helper.assertTrue(ThunderVaneRite.check(level, high, true, true) == ThunderVaneRite.Missing.ROOFED,
					"Under a roof the vane was not refused");
			level.setBlock(roof, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
			helper.assertTrue(ThunderVaneRite.check(level, high, true, true) == null, "With the roof gone the vane was refused: "
					+ ThunderVaneRite.check(level, high, true, true));
		} finally {
			level.setBlock(roof, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
		}
		Registry<Biome> biomes = level.registryAccess().lookupOrThrow(Registries.BIOME);
		for (var mountains : List.of(Biomes.JAGGED_PEAKS, Biomes.FROZEN_PEAKS, Biomes.STONY_PEAKS, Biomes.SNOWY_SLOPES,
				Biomes.WINDSWEPT_HILLS, ResourceKey.create(Registries.BIOME, Jugcraft.id("highland")),
				ResourceKey.create(Registries.BIOME, Jugcraft.id("karst_pinnacles")))) {
			helper.assertTrue(biomes.getOrThrow(mountains).is(ThunderVaneRite.LANDS), mountains.identifier() + " is not the mountains");
		}
		for (var low : List.of(Biomes.PLAINS, Biomes.DESERT, Biomes.OCEAN, Biomes.SWAMP)) {
			helper.assertTrue(!biomes.getOrThrow(low).is(ThunderVaneRite.LANDS), low.identifier() + " is the mountains");
		}
		helper.assertTrue(ThunderVaneRite.mist(level, high) == null, "A mist tore open where nobody raised a vane");
		helper.succeed();
	}

	/**
	 * The vane raised in full when no instance can open (a game-test server has no lair dimension, and the one slot
	 * {@code lairs.instances} allows is taken if it has): the Crown is full, the vane is kept, nobody moves and no mist
	 * tears open. In fair weather it is refused the same way. Raised from the hand, it is rested after its use, answered or
	 * not.
	 */
	@GameTest
	public void theVaneUsesNothingWhenNoCrownCanOpen(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		String instances = JugcraftConfig.textOption("lairs.instances");
		try {
			JugcraftConfig.setTextOption("lairs.instances", "1");
			Lairs.open(level.getServer(), CROWN);  // null here; takes the only slot where there is a Crown of Thunder
			ServerPlayer player = player(helper, STAND, ThunderVaneRite.MIN_Y);
			player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(JugcraftLairs.THUNDER_VANE, 2));
			Vec3 stood = player.position();
			ThunderVaneRite.Raising raising = ThunderVaneRite.raise(level, player, player.getMainHandItem(), true, true);
			helper.assertTrue(raising.missing() == ThunderVaneRite.Missing.FULL && raising.instance() == null, "The Crown was not full: " + raising);
			helper.assertTrue(player.getMainHandItem().getCount() == 2, "The vane was used up: " + player.getMainHandItem());
			helper.assertTrue(player.level() == level && player.position().distanceTo(stood) < 1.0E-6 && player.getAttached(Lairs.VISIT) == null,
					"The player was moved");
			helper.assertTrue(ThunderVaneRite.mist(level, BlockPos.containing(stood)) == null, "A mist tore open into no Crown");
			helper.assertTrue(ThunderVaneRite.raise(level, player, player.getMainHandItem(), false, true).missing() == ThunderVaneRite.Missing.FAIR
					&& player.getMainHandItem().getCount() == 2, "In fair weather the vane was not refused, or was used up");
			InteractionResult used = player.gameMode.useItem(player, level, player.getMainHandItem(), InteractionHand.MAIN_HAND);
			helper.assertTrue(used.consumesAction() && player.getMainHandItem().getCount() == 2
					&& player.getCooldowns().isOnCooldown(player.getMainHandItem()), "Raised from the hand, the vane was used up or not rested: " + used);
		} finally {
			JugcraftConfig.setTextOption("lairs.instances", instances);
			Lairs.reset();
		}
		helper.succeed();
	}

	/** A Thunder Vane is crafted from two feathers, a lightning rod, three copper ingots and a compass. */
	@GameTest
	public void theVaneIsCraftedFromFeathersARodCopperAndACompass(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		ItemStack feather = new ItemStack(Items.FEATHER);
		ItemStack copper = new ItemStack(Items.COPPER_INGOT);
		CraftingInput grid = CraftingInput.of(3, 3, List.of(feather, new ItemStack(net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(Identifier.withDefaultNamespace("lightning_rod"))), feather.copy(),
				copper, new ItemStack(Items.COMPASS), copper.copy(), ItemStack.EMPTY, copper.copy(), ItemStack.EMPTY));
		ItemStack made = level.getServer().getRecipeManager().getRecipeFor(RecipeType.CRAFTING, grid, level)
				.orElseThrow(() -> helper.assertionException("Feathers, a lightning rod, copper and a compass craft nothing")).value().assemble(grid);
		helper.assertTrue(made.is(JugcraftLairs.THUNDER_VANE) && made.getCount() == 1, "Feathers, a lightning rod, copper and a compass made " + made);
		helper.succeed();
	}

	/**
	 * A Storm Rod stood base, middle and tip charges and calms: charged, each part burns at its own light (more toward the
	 * tip), and a rod charged or calm already does not change; calm, it gives none. Its tip is drawn to a point. A rod
	 * charged in an instance is found by its place, and calmed, none is. Like the other lair-only blocks the rods, the nest
	 * and the clouds cannot be broken and have no item, and none is a fixture a player may use; the nest is stood on, the
	 * clouds are passed through. The summit, the ledges and the terrace are where the arrival, the middle and the rods say.
	 */
	@GameTest
	public void theStormRodsChargeAndCalm(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		BlockPos base = new BlockPos(1, 2, 1);
		StormRodBlock.Part[] parts = StormRodBlock.Part.values();
		for (int i = 0; i < parts.length; i++) {
			helper.setBlock(base.above(i), JugcraftLairs.STORM_ROD.defaultBlockState().setValue(StormRodBlock.PART, parts[i]));
		}
		BlockPos origin = helper.absolutePos(base).subtract(CrownOfThunder.RODS[Rod.NORTH.ordinal()]);
		helper.assertTrue(Rod.NORTH.base(origin).equals(helper.absolutePos(base)) && Rod.NORTH.parts(origin).size() == CrownOfThunder.ROD_HEIGHT,
				"The north rod is not found by its place");
		helper.assertTrue(CrownOfThunder.charged(level, origin) == null, "A rod was charged before any was");
		for (int i = 0; i < parts.length; i++) {
			helper.assertTrue(helper.getBlockState(base.above(i)).getLightEmission() == 0, "A calm rod's " + parts[i] + " gives light");
		}
		helper.assertTrue(CrownOfThunder.charge(level, origin, Rod.NORTH, true), "The north rod did not charge");
		helper.assertTrue(!CrownOfThunder.charge(level, origin, Rod.NORTH, true), "A charged rod charged again");
		helper.assertTrue(CrownOfThunder.charged(level, origin) == Rod.NORTH, "The charged rod is not found: " + CrownOfThunder.charged(level, origin));
		int last = 0;
		for (int i = 0; i < parts.length; i++) {
			BlockState state = helper.getBlockState(base.above(i));
			helper.assertTrue(state.getValue(StormRodBlock.CHARGED) && state.getLightEmission() == parts[i].light && parts[i].light > last,
					"The charged rod's " + parts[i] + " gives light " + state.getLightEmission() + ", not " + parts[i].light);
			last = parts[i].light;
		}
		helper.assertTrue(CrownOfThunder.charge(level, origin, Rod.NORTH, false) && CrownOfThunder.charged(level, origin) == null
				&& helper.getBlockState(base).getLightEmission() == 0, "The north rod did not calm");
		helper.assertTrue(!StormRodBlock.setCharged(level, helper.absolutePos(base.east()), true), "Air was charged");
		// The shaft is six pixels across all the way up; the tip narrows to a point, so its top corner is empty.
		VoxelShape corner = Shapes.box(5.0 / 16, 15.0 / 16, 5.0 / 16, 6.0 / 16, 1.0, 6.0 / 16);
		VoxelShape tip = helper.getBlockState(base.above(2)).getShape(level, helper.absolutePos(base.above(2)), CollisionContext.empty());
		VoxelShape shaft = helper.getBlockState(base).getShape(level, helper.absolutePos(base), CollisionContext.empty());
		helper.assertTrue(Shapes.joinIsNotEmpty(shaft, corner, BooleanOp.AND) && !Shapes.joinIsNotEmpty(tip, corner, BooleanOp.AND)
				&& tip.max(Direction.Axis.Y) == 1.0 && tip.bounds().getXsize() == shaft.bounds().getXsize(),
				"The rod's tip is not drawn to a point from the shaft's width");

		TagKey<Block> fixtures = TagKey.create(Registries.BLOCK, Jugcraft.id("lair_fixtures"));
		BlockPos at = helper.absolutePos(base);
		for (Block block : List.of(JugcraftLairs.STORM_ROD, JugcraftLairs.ROC_NEST, JugcraftLairs.STORM_CLOUD)) {
			helper.assertTrue(block.defaultBlockState().getDestroySpeed(level, at) < 0.0F, block + " can be broken");
			helper.assertTrue(block.asItem() == Items.AIR, block + " has an item");
			helper.assertTrue(!block.defaultBlockState().is(fixtures), block + " is a lair fixture");
		}
		BlockPos nest = new BlockPos(4, 2, 4);
		BlockPos cloud = new BlockPos(4, 2, 1);
		helper.setBlock(nest, JugcraftLairs.ROC_NEST.defaultBlockState());
		helper.setBlock(cloud, JugcraftLairs.STORM_CLOUD.defaultBlockState());
		helper.assertTrue(!helper.getBlockState(nest).getCollisionShape(level, helper.absolutePos(nest)).isEmpty(), "The nest cannot be stood on");
		helper.assertTrue(helper.getBlockState(cloud).getCollisionShape(level, helper.absolutePos(cloud)).isEmpty(), "The clouds cannot be passed through");

		helper.assertTrue(CrownOfThunder.onTerrace(CROWN.arrival.x, CROWN.arrival.z) && !CrownOfThunder.onSummit(CROWN.arrival.x, CROWN.arrival.z),
				"The arrival is not on the terrace");
		helper.assertTrue(CrownOfThunder.onSummit(CrownOfThunder.CENTRE_X, CrownOfThunder.CENTRE_Z), "The middle is not on the summit");
		for (Rod rod : Rod.values()) {
			helper.assertTrue(CrownOfThunder.onSummit(rod.x(), rod.z()), "The " + rod.title() + " rod is not on the summit");
		}
		helper.assertTrue(CrownOfThunder.onLedge(CrownOfThunder.CENTRE_X + 20.5, CrownOfThunder.CENTRE_Z)
				&& CrownOfThunder.onLedge(CrownOfThunder.CENTRE_X - 20.5, CrownOfThunder.CENTRE_Z)
				&& !CrownOfThunder.onLedge(CrownOfThunder.CENTRE_X, CrownOfThunder.CENTRE_Z - 20.5)
				&& !CrownOfThunder.onLedge(CrownOfThunder.CENTRE_X + 10.0, CrownOfThunder.CENTRE_Z),
				"The ledges are not due east and due west, below the summit's edge");
		helper.succeed();
	}
}
