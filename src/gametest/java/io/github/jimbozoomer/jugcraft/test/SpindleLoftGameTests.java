package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.agriculture.SpinningWheelBlockEntity;
import io.github.jimbozoomer.jugcraft.config.JugcraftConfig;
import io.github.jimbozoomer.jugcraft.lair.JugcraftLairs;
import io.github.jimbozoomer.jugcraft.lair.Lair;
import io.github.jimbozoomer.jugcraft.lair.Lairs;
import io.github.jimbozoomer.jugcraft.lair.SpindleRite;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
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
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * The Spindle Loft and its ritual (docs/features/spindle-loft.md), as far as a game-test server can show them. Its world
 * is built from the flat preset alone, so it has no lair dimension: the loft's own files load (dimension type, biome and
 * template), the Cursed Spindle's checks hold, a spindle that can open no instance is not used up, and a wheel that is no
 * gate still spins wool. Everything that needs the loft itself (going in by the wheel, following, the needle's eye, the
 * fall through the doily, the wheel calming when the instance closes) runs in a real world, in
 * {@link SpindleLoftClientGameTests}.
 */
public class SpindleLoftGameTests {
	private static final Lair LOFT = Lair.SPINDLE_LOFT;
	private static final BlockPos WHEEL = new BlockPos(2, 1, 2);

	private static Block block(String id) {
		return BuiltInRegistries.BLOCK.getValue(Jugcraft.id(id));
	}

	private static ServerPlayer player(GameTestHelper helper, BlockPos standAt) {
		ServerPlayer player = helper.makeMockServerPlayerInLevel();
		player.setGameMode(GameType.SURVIVAL);
		BlockPos absolute = helper.absolutePos(standAt);
		player.snapTo(absolute.getX() + 0.5, absolute.getY(), absolute.getZ() + 0.5, 180.0F, 30.0F);
		return player;
	}

	/** A Spinning Wheel at {@link #WHEEL}, and a use of it from the south. */
	private static BlockHitResult wheel(GameTestHelper helper) {
		helper.setBlock(WHEEL, block("spinning_wheel").defaultBlockState());
		BlockPos at = helper.absolutePos(WHEEL);
		return new BlockHitResult(Vec3.atCenterOf(at).relative(Direction.SOUTH, 0.5), Direction.SOUTH, at, false);
	}

	/** The Spindle Loft's dimension type and biome load from their data pack files. */
	@GameTest
	public void spindleLoftDimensionFilesLoad(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		helper.assertTrue(level.registryAccess().lookupOrThrow(Registries.DIMENSION_TYPE).containsKey(LOFT.dimension.identifier()),
				"No dimension type " + LOFT.dimension.identifier());
		helper.assertTrue(level.registryAccess().lookupOrThrow(Registries.BIOME).containsKey(LOFT.dimension.identifier()),
				"No biome " + LOFT.dimension.identifier());
		helper.succeed();
	}

	/**
	 * The template loads in the running game with every one of the loft's own blocks (DataVersion the game's), at the
	 * size tools/spindle_loft.py gives Lair.SPINDLE_LOFT.
	 */
	@GameTest
	public void spindleLoftTemplateLoads(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		StructureTemplate template = Lairs.template(level, LOFT);
		helper.assertTrue(template != null, "No template " + LOFT.template);
		helper.assertTrue(template.getSize().equals(new Vec3i(LOFT.width, LOFT.height, LOFT.length)),
				"The template is " + template.getSize() + ", not the Lair's size");
		Identifier path = Identifier.fromNamespaceAndPath(LOFT.template.getNamespace(), "structure/" + LOFT.template.getPath() + ".nbt");
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
		for (String wanted : List.of("doily_lace", "spool_wood", "spool_thread", "pincushion", "pincushion_seam", "pincushion_leaf",
				"needle_steel", "pin_shaft", "measuring_tape", "thimble_metal", "taut_thread", "grimy_skylight", "lair_exit")) {
			helper.assertTrue(names.contains("jugcraft:" + wanted), "The template has no " + wanted + ": " + names);
		}
		helper.succeed();
	}

	/** The spindle's checks: only at night (by day it is too early for sleep), and only in the Overworld (which this is). */
	@GameTest
	public void theSpindleNeedsTheNight(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		wheel(helper);
		BlockPos at = helper.absolutePos(WHEEL);
		helper.assertTrue(SpindleRite.check(level, at, false) == SpindleRite.Missing.NOT_NIGHT, "By day the spindle works");
		helper.assertTrue(SpindleRite.check(level, at, true) == null, "At night the spindle does not work: " + SpindleRite.check(level, at, true));
		helper.assertTrue(SpindleRite.gate(level, at) == null, "A wheel nobody used is a gate");
		helper.succeed();
	}

	/**
	 * The rite done in full, but no instance can open (a game-test server has no lair dimension, and the one slot
	 * {@code lairs.instances} allows is taken if it has): the spindle says the loft is full, it is not used up, nobody
	 * moves or is blinded, and the wheel does not spin wild.
	 */
	@GameTest
	public void theSpindleUsesNothingWhenNoLairCanOpen(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		String instances = JugcraftConfig.textOption("lairs.instances");
		try {
			JugcraftConfig.setTextOption("lairs.instances", "1");
			Lairs.open(level.getServer(), LOFT);  // null here; takes the only slot where there is a Spindle Loft
			BlockHitResult hit = wheel(helper);
			ServerPlayer player = player(helper, WHEEL.south(2));
			player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(JugcraftLairs.CURSED_SPINDLE, 2));
			Vec3 stood = player.position();
			helper.assertTrue(SpindleRite.useBlock(player, level, InteractionHand.MAIN_HAND, hit) == InteractionResult.SUCCESS,
					"The spindle's use was not the rite's");
			helper.assertTrue(player.getMainHandItem().getCount() == 2, "The spindle was used up: " + player.getMainHandItem());
			helper.assertTrue(player.level() == level && player.position().distanceTo(stood) < 1.0E-6 && player.getAttached(Lairs.VISIT) == null,
					"The player was moved");
			helper.assertFalse(player.hasEffect(MobEffects.BLINDNESS), "The player fell asleep");
			SpinningWheelBlockEntity entity = (SpinningWheelBlockEntity) level.getBlockEntity(hit.getBlockPos());
			helper.assertFalse(entity.isWild(level.getGameTime()), "The wheel spins wild");
			helper.assertTrue(SpindleRite.prick(level, player, hit.getBlockPos(), player.getMainHandItem(), true).missing()
					== SpindleRite.Missing.FULL, "The loft was not full");
		} finally {
			JugcraftConfig.setTextOption("lairs.instances", instances);
			Lairs.reset();
		}
		helper.succeed();
	}

	/** A wheel that is no gate is the wheel it always was: an empty hand works its treadle, wool goes on its distaff. */
	@GameTest
	public void aWheelThatIsNoGateStillSpinsWool(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		BlockHitResult hit = wheel(helper);
		ServerPlayer player = player(helper, WHEEL.south(2));
		player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
		helper.assertTrue(SpindleRite.useBlock(player, level, InteractionHand.MAIN_HAND, hit) == InteractionResult.PASS,
				"An empty hand on a wheel that is no gate was taken by the rite");
		player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(BuiltInRegistries.ITEM.getValue(Identifier.withDefaultNamespace("white_wool"))));
		helper.assertTrue(SpindleRite.useBlock(player, level, InteractionHand.MAIN_HAND, hit) == InteractionResult.PASS,
				"Wool on the wheel was taken by the rite");
		helper.succeed();
	}

	/**
	 * A wheel that is a gate spins wild, three times as fast as the treadle drives it, until its time is up or it is calmed;
	 * then it turns on from where it stopped, never jumping back.
	 */
	@GameTest
	public void aWildWheelSpinsFastAndCalms(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		BlockHitResult hit = wheel(helper);
		SpinningWheelBlockEntity entity = (SpinningWheelBlockEntity) level.getBlockEntity(hit.getBlockPos());
		long now = level.getGameTime();
		float before = entity.spin(now, 0.0F);
		entity.spinWild(now, 100);
		helper.assertTrue(entity.isWild(now) && entity.isWild(now + 99) && !entity.isWild(now + 100), "The wheel is not wild for 100 ticks");
		float later = entity.spin(now + 10, 0.0F);
		helper.assertTrue(Math.abs(later - before - 10 * SpinningWheelBlockEntity.WILD_SPEED) < 1.0E-3F,
				"Ten wild ticks turned the wheel " + (later - before) + ", not " + 10 * SpinningWheelBlockEntity.WILD_SPEED);
		float ended = entity.spin(now + 100, 0.0F);
		helper.assertTrue(entity.spin(now + 200, 0.0F) == ended, "The wheel spins on after its wild spell");
		entity.spinWild(now + 200, 50);
		helper.assertTrue(entity.spin(now + 200, 0.0F) >= ended - 1.0F, "A second spell turned the wheel back");
		helper.succeed();
	}
}
