package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.config.JugcraftConfig;
import io.github.jimbozoomer.jugcraft.lair.JugcraftLairs;
import io.github.jimbozoomer.jugcraft.lair.KilnSealRite;
import io.github.jimbozoomer.jugcraft.lair.Lair;
import io.github.jimbozoomer.jugcraft.lair.Lairs;
import io.github.jimbozoomer.jugcraft.lair.MoltenSlagBlock;
import io.github.jimbozoomer.jugcraft.lair.SluiceGateBlock;
import io.github.jimbozoomer.jugcraft.lair.TroughStoneBlock;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
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
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.animal.pig.Pig;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.phys.Vec3;

/**
 * The Cinder Kiln and the Kiln Seal (docs/features/cinder-kiln.md), as far as a game-test server can show them. Its world
 * is built from the flat preset alone, so it has no lair dimension: the kiln's own files load (dimension type, biome and
 * template), the seal's checks hold, a seal that can open no instance is not used up, the slag burns whom it should and
 * no one else, a flooded trough puts out the burning, and a sluice gate floods its own trough, drains it and fills again.
 * Everything that needs the kiln itself (sinking in through the magma, the vent, following, the arch's Grey Mist, the
 * instance closing) runs in a real world, in {@link CinderKilnClientGameTests}.
 */
public class CinderKilnGameTests {
	private static final Lair KILN = Lair.CINDER_KILN;
	private static final BlockPos STAND = new BlockPos(2, 2, 2);

	private static ServerPlayer player(GameTestHelper helper, BlockPos standAt) {
		ServerPlayer player = helper.makeMockServerPlayerInLevel();
		player.setGameMode(GameType.SURVIVAL);
		BlockPos absolute = helper.absolutePos(standAt);
		player.snapTo(absolute.getX() + 0.5, absolute.getY(), absolute.getZ() + 0.5, 180.0F, 0.0F);
		return player;
	}

	/** The Cinder Kiln's dimension type and biome load from their data pack files. */
	@GameTest
	public void cinderKilnDimensionFilesLoad(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		helper.assertTrue(level.registryAccess().lookupOrThrow(Registries.DIMENSION_TYPE).containsKey(KILN.dimension.identifier()),
				"No dimension type " + KILN.dimension.identifier());
		helper.assertTrue(level.registryAccess().lookupOrThrow(Registries.BIOME).containsKey(KILN.dimension.identifier()),
				"No biome " + KILN.dimension.identifier());
		helper.succeed();
	}

	/**
	 * The template loads in the running game with every one of the kiln's own blocks and the Grey Mist (DataVersion the
	 * game's), at the size tools/cinder_kiln.py gives Lair.CINDER_KILN.
	 */
	@GameTest
	public void cinderKilnTemplateLoads(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		StructureTemplate template = Lairs.template(level, KILN);
		helper.assertTrue(template != null, "No template " + KILN.template);
		helper.assertTrue(template.getSize().equals(new Vec3i(KILN.width, KILN.height, KILN.length)),
				"The template is " + template.getSize() + ", not the Lair's size");
		Identifier path = Identifier.fromNamespaceAndPath(KILN.template.getNamespace(), "structure/" + KILN.template.getPath() + ".nbt");
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
		for (String wanted : List.of("kiln_brick", "cracked_basalt", "molten_slag", "trough_stone", "sluice_gate", "lair_exit")) {
			helper.assertTrue(names.contains("jugcraft:" + wanted), "The template has no " + wanted + ": " + names);
		}
		helper.succeed();
	}

	/**
	 * The seal's checks: not on basalt, only on a magma block; on magma here, in the test world's flat land, it is refused
	 * as no volcanic land; anywhere in the Nether, or in the Volcano or the Cinder Barrens of the Overworld, the land is
	 * volcanic, but not the Overworld's plains or the End. Checking changes nothing.
	 */
	@GameTest
	public void theSealNeedsMagmaInAVolcanicLand(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		BlockPos at = helper.absolutePos(STAND.below());
		helper.setBlock(STAND.below(), Blocks.BASALT.defaultBlockState());
		helper.assertTrue(KilnSealRite.check(level, at, true) == KilnSealRite.Missing.NOT_MAGMA, "The seal took on basalt");
		helper.setBlock(STAND.below(), Blocks.MAGMA_BLOCK.defaultBlockState());
		helper.assertTrue(KilnSealRite.check(level, at, true) == null, "The seal did not take on magma in a volcanic land: "
				+ KilnSealRite.check(level, at, true));
		helper.assertTrue(KilnSealRite.check(level, at) == KilnSealRite.Missing.NOT_VOLCANIC,
				"The seal took on magma in the test world's " + level.getBiome(at).getRegisteredName());
		Registry<Biome> biomes = level.registryAccess().lookupOrThrow(Registries.BIOME);
		Holder<Biome> plains = biomes.getOrThrow(Biomes.PLAINS);
		Holder<Biome> volcano = biomes.getOrThrow(ResourceKey.create(Registries.BIOME, Jugcraft.id("volcano")));
		Holder<Biome> barrens = biomes.getOrThrow(ResourceKey.create(Registries.BIOME, Jugcraft.id("cinder_barrens")));
		helper.assertTrue(KilnSealRite.volcanic(Level.NETHER, plains), "The Nether is not volcanic land");
		helper.assertTrue(KilnSealRite.volcanic(Level.OVERWORLD, volcano) && KilnSealRite.volcanic(Level.OVERWORLD, barrens),
				"The Volcano or the Cinder Barrens is not volcanic land");
		helper.assertTrue(!KilnSealRite.volcanic(Level.OVERWORLD, plains), "The Overworld's plains are volcanic land");
		helper.assertTrue(!KilnSealRite.volcanic(Level.END, volcano), "The End is volcanic land");
		helper.assertTrue(KilnSealRite.vent(level, at) == null, "A vent opened where nobody pressed a seal");
		helper.succeed();
	}

	/**
	 * The seal pressed in full when no instance can open (a game-test server has no lair dimension, and the one slot
	 * {@code lairs.instances} allows is taken if it has): the kiln is full, the seal is kept, nobody moves and no vent
	 * opens. On basalt it is refused the same way.
	 */
	@GameTest
	public void theSealUsesNothingWhenNoKilnCanOpen(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		String instances = JugcraftConfig.textOption("lairs.instances");
		try {
			JugcraftConfig.setTextOption("lairs.instances", "1");
			Lairs.open(level.getServer(), KILN);  // null here; takes the only slot where there is a Cinder Kiln
			helper.setBlock(STAND.below(), Blocks.MAGMA_BLOCK.defaultBlockState());
			BlockPos magma = helper.absolutePos(STAND.below());
			ServerPlayer player = player(helper, STAND);
			player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(JugcraftLairs.KILN_SEAL, 2));
			Vec3 stood = player.position();
			KilnSealRite.Press press = KilnSealRite.press(level, player, magma, player.getMainHandItem(), true);
			helper.assertTrue(press.missing() == KilnSealRite.Missing.FULL && press.instance() == null, "The kiln was not full: " + press);
			helper.assertTrue(player.getMainHandItem().getCount() == 2, "The seal was used up: " + player.getMainHandItem());
			helper.assertTrue(player.level() == level && player.position().distanceTo(stood) < 1.0E-6 && player.getAttached(Lairs.VISIT) == null,
					"The player was moved");
			helper.assertTrue(KilnSealRite.vent(level, magma) == null, "A vent opened into no kiln");
			helper.setBlock(STAND.below(), Blocks.BASALT.defaultBlockState());
			helper.assertTrue(KilnSealRite.press(level, player, magma, player.getMainHandItem(), true).missing() == KilnSealRite.Missing.NOT_MAGMA
					&& player.getMainHandItem().getCount() == 2, "Pressed into basalt, the seal was not refused, or was used up");
		} finally {
			JugcraftConfig.setTextOption("lairs.instances", instances);
			Lairs.reset();
		}
		helper.succeed();
	}

	/**
	 * The slag burns a pig standing in it (hurt and set burning), but not a pig under Fire Resistance; it harms a player in
	 * survival and not one in creative; feet sink into it, and a trough's stone is sunk lower still. A flooded trough puts
	 * out a burning player who stands in it; a dry one does not. The kiln's blocks cannot be broken and have no item.
	 */
	@GameTest
	public void theKilnsSlagAndTroughs(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		BlockPos slag = helper.absolutePos(STAND.below());
		helper.setBlock(STAND.below(), JugcraftLairs.MOLTEN_SLAG.defaultBlockState());
		Pig pig = helper.spawn(EntityTypes.PIG, STAND);
		float health = pig.getHealth();
		JugcraftLairs.MOLTEN_SLAG.stepOn(level, slag, level.getBlockState(slag), pig);
		helper.assertTrue(pig.getHealth() < health && pig.getRemainingFireTicks() > 0, "The slag did not burn a pig standing in it");
		Pig warded = helper.spawn(EntityTypes.PIG, STAND.east());
		warded.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 200));
		float wardedHealth = warded.getHealth();
		JugcraftLairs.MOLTEN_SLAG.stepOn(level, slag, level.getBlockState(slag), warded);
		helper.assertTrue(warded.getHealth() == wardedHealth && warded.getRemainingFireTicks() <= 0,
				"The slag burned a pig under Fire Resistance");
		ServerPlayer player = player(helper, STAND.north());
		helper.assertTrue(MoltenSlagBlock.harms(player), "The slag does not harm a player in survival");
		player.setGameMode(GameType.CREATIVE);
		helper.assertTrue(!MoltenSlagBlock.harms(player), "The slag harms a player in creative");
		player.setGameMode(GameType.SURVIVAL);
		double sunk = level.getBlockState(slag).getCollisionShape(level, slag).max(Direction.Axis.Y);
		helper.assertTrue(Math.abs(sunk - MoltenSlagBlock.TOP / 16.0) < 1.0E-6, "Feet stand " + sunk + " up in the slag");

		BlockPos trough = helper.absolutePos(STAND.north().below());
		BlockState dry = JugcraftLairs.TROUGH_STONE.defaultBlockState();
		helper.setBlock(STAND.north().below(), dry);
		double top = level.getBlockState(trough).getCollisionShape(level, trough).max(Direction.Axis.Y);
		helper.assertTrue(Math.abs(top - TroughStoneBlock.TOP / 16.0) < 1.0E-6, "A trough's stone is " + top + " high");
		player.setRemainingFireTicks(100);
		JugcraftLairs.TROUGH_STONE.stepOn(level, trough, dry, player);
		helper.assertTrue(player.getRemainingFireTicks() > 0, "A dry trough put out a burning player");
		BlockState flooded = dry.setValue(TroughStoneBlock.FLOODED, true);
		helper.setBlock(STAND.north().below(), flooded);
		JugcraftLairs.TROUGH_STONE.stepOn(level, trough, flooded, player);
		helper.assertTrue(player.getRemainingFireTicks() <= 0, "A flooded trough did not put out a burning player");
		for (Block fixture : List.of(JugcraftLairs.KILN_BRICK, JugcraftLairs.CRACKED_BASALT, JugcraftLairs.MOLTEN_SLAG,
				JugcraftLairs.TROUGH_STONE, JugcraftLairs.SLUICE_GATE)) {
			helper.assertTrue(fixture.defaultBlockState().getDestroySpeed(level, slag) < 0.0F, fixture + " can be broken");
			helper.assertTrue(fixture.asItem() == Items.AIR, fixture + " has an item");
		}
		helper.succeed();
	}

	/**
	 * A sluice gate (two posts, three panels, a lintel and its wheel) over a trough of trough stone running north from under
	 * it, and beside it a strip of trough stone it does not touch. Turned by its wheel, the gate opens and floods its own
	 * trough, not the other strip, and turned again it only says it is open. After its flood the gate closes, the trough
	 * drains and the gate fills again, refusing a turn; then it is ready again.
	 */
	@GameTest(maxTicks = 700)
	public void aSluiceFloodsItsTroughThenFillsAgain(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		List<BlockPos> gate = new ArrayList<>();
		List<BlockPos> trough = new ArrayList<>();
		List<BlockPos> apart = new ArrayList<>();
		BlockState frame = JugcraftLairs.SLUICE_GATE.defaultBlockState().setValue(SluiceGateBlock.FACING, Direction.NORTH);
		for (int y = 2; y <= 3; y++) {
			for (int x = 1; x <= 5; x++) {
				SluiceGateBlock.Part part = x == 1 || x == 5 ? SluiceGateBlock.Part.FRAME : SluiceGateBlock.Part.PANEL;
				helper.setBlock(new BlockPos(x, y, 5), frame.setValue(SluiceGateBlock.PART, part));
				gate.add(new BlockPos(x, y, 5));
			}
		}
		for (int x = 1; x <= 5; x++) {
			helper.setBlock(new BlockPos(x, 4, 5), frame);
			gate.add(new BlockPos(x, 4, 5));
		}
		BlockPos wheel = new BlockPos(1, 2, 4);
		helper.setBlock(wheel, frame.setValue(SluiceGateBlock.PART, SluiceGateBlock.Part.WHEEL));
		gate.add(wheel);
		for (int x = 2; x <= 4; x++) {
			for (int z = 1; z <= 5; z++) {
				helper.setBlock(new BlockPos(x, 1, z), JugcraftLairs.TROUGH_STONE.defaultBlockState());
				trough.add(new BlockPos(x, 1, z));
			}
		}
		for (int x = 6; x <= 7; x++) {
			for (int z = 1; z <= 3; z++) {
				helper.setBlock(new BlockPos(x, 1, z), JugcraftLairs.TROUGH_STONE.defaultBlockState());
				apart.add(new BlockPos(x, 1, z));
			}
		}
		SluiceGateBlock.Turn turn = SluiceGateBlock.turn(level, helper.absolutePos(wheel), null);
		helper.assertTrue(turn == SluiceGateBlock.Turn.OPENED, "Turning a ready gate did not open it: " + turn);
		assertFlow(helper, gate, SluiceGateBlock.Flow.OPEN);
		assertFlooded(helper, trough, true, "The gate's trough");
		assertFlooded(helper, apart, false, "The strip the gate does not touch");
		helper.assertTrue(SluiceGateBlock.turn(level, helper.absolutePos(new BlockPos(3, 3, 5)), null) == SluiceGateBlock.Turn.ALREADY_OPEN,
				"An open gate turned again");
		helper.runAtTickTime(SluiceGateBlock.FLOOD_TICKS + 10, () -> {
			assertFlow(helper, gate, SluiceGateBlock.Flow.FILLING);
			assertFlooded(helper, trough, false, "The gate's trough, after its flood,");
			helper.assertTrue(SluiceGateBlock.turn(level, helper.absolutePos(wheel), null) == SluiceGateBlock.Turn.FILLING,
					"A filling gate turned");
		});
		helper.runAtTickTime(SluiceGateBlock.FLOOD_TICKS + SluiceGateBlock.REFILL_TICKS + 20, () -> {
			assertFlow(helper, gate, SluiceGateBlock.Flow.READY);
			assertFlooded(helper, trough, false, "The gate's trough, ready again,");
			helper.succeed();
		});
	}

	private static void assertFlow(GameTestHelper helper, List<BlockPos> gate, SluiceGateBlock.Flow flow) {
		for (BlockPos at : gate) {
			BlockState state = helper.getBlockState(at);
			helper.assertTrue(state.is(JugcraftLairs.SLUICE_GATE) && state.getValue(SluiceGateBlock.FLOW) == flow,
					"The gate at " + at + " is " + state + ", not " + flow);
		}
	}

	private static void assertFlooded(GameTestHelper helper, List<BlockPos> cells, boolean flooded, String what) {
		for (BlockPos at : cells) {
			BlockState state = helper.getBlockState(at);
			helper.assertTrue(state.is(JugcraftLairs.TROUGH_STONE) && state.getValue(TroughStoneBlock.FLOODED) == flooded,
					what + " at " + at + " is " + state + ", not " + (flooded ? "flooded" : "dry"));
		}
	}
}
