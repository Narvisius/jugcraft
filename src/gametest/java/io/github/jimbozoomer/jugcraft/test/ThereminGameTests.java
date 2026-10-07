package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.agriculture.JugcraftAgriculture;
import io.github.jimbozoomer.jugcraft.agriculture.ThereminBlock;
import io.github.jimbozoomer.jugcraft.agriculture.ThereminBlockEntity;
import java.util.List;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * In-game tests for the Theremin: its pitch rises over two octaves and its comparator reading from 0 to 15 as a creature
 * comes from the edge of its range to the antenna; it senses the nearest player whether it plays or not; an empty hand
 * switches it on and off and a redstone signal plays it too; a player close to a playing theremin earns Good Vibrations;
 * and the data loads.
 */
public class ThereminGameTests {
	private static final BlockPos THEREMIN = new BlockPos(1, 2, 1);

	private static ServerPlayer player(GameTestHelper helper, Vec3 at) {
		ServerPlayer player = helper.makeMockServerPlayerInLevel();
		player.setGameMode(GameType.SURVIVAL);
		player.setPos(at.x, at.y, at.z);
		return player;
	}

	private static boolean earned(ServerPlayer player, String id) {
		AdvancementHolder advancement = player.level().getServer().getAdvancements().get(Jugcraft.id(id));
		return advancement != null && player.getAdvancements().getOrStartProgress(advancement).isDone();
	}

	private static ThereminBlockEntity theremin(GameTestHelper helper) {
		helper.setBlock(THEREMIN, JugcraftAgriculture.block("theremin").defaultBlockState().setValue(ThereminBlock.FACING, Direction.NORTH));
		return helper.getBlockEntity(THEREMIN, ThereminBlockEntity.class);
	}

	/** Where to stand {@code blocks} from the antenna, straight out along +z from it (the player's middle at its height). */
	private static Vec3 away(GameTestHelper helper, double blocks) {
		Vec3 antenna = ThereminBlockEntity.antenna(helper.absolutePos(THEREMIN), Direction.NORTH);
		return new Vec3(antenna.x, antenna.y - 0.9, antenna.z + blocks);
	}

	/** Two octaves from the edge of its range to the antenna, rising all the way; 15 at the antenna, 0 past the edge. */
	@GameTest
	public void pitchAndSignalFollowDistance(GameTestHelper helper) {
		helper.assertTrue(Math.abs(ThereminBlockEntity.pitchFor(ThereminBlockEntity.RANGE) - ThereminBlockEntity.LOW) < 1e-4
				&& Math.abs(ThereminBlockEntity.pitchFor(0.0) - ThereminBlockEntity.HIGH) < 1e-4, "Its pitch runs from LOW to HIGH");
		float last = 0.0F;
		for (double d = ThereminBlockEntity.RANGE; d >= 0.0; d -= 0.5) {
			float pitch = ThereminBlockEntity.pitchFor(d);
			helper.assertTrue(pitch > last, "Higher at " + d + " than further off");
			last = pitch;
		}
		helper.assertTrue(ThereminBlockEntity.signalFor(0.0) == 15 && ThereminBlockEntity.signalFor(ThereminBlockEntity.RANGE) == 0
				&& ThereminBlockEntity.signalFor(-1.0) == 0 && ThereminBlockEntity.signalFor(ThereminBlockEntity.RANGE + 1) == 0
				&& ThereminBlockEntity.signalFor(4.0) == 8, "15 at the antenna, 8 halfway, 0 at the edge and past it");
		helper.succeed();
	}

	/** Silent, it still senses: a player two blocks off reads 12; moved to six, 4; beyond its range, nothing. */
	@GameTest(maxTicks = 40)
	public void itSensesTheNearest(GameTestHelper helper) {
		ThereminBlockEntity theremin = theremin(helper);
		ServerPlayer player = player(helper, away(helper, 2.0));
		helper.runAfterDelay(ThereminBlockEntity.SENSE_TICKS + 1, () -> {
			helper.assertTrue(theremin.signal() == ThereminBlockEntity.signalFor(theremin.distance()) && theremin.signal() >= 11,
					"Two blocks off it reads high: " + theremin.signal() + " at " + theremin.distance());
			Vec3 further = away(helper, 6.0);
			player.setPos(further.x, further.y, further.z);
			helper.runAfterDelay(ThereminBlockEntity.SENSE_TICKS + 1, () -> {
				helper.assertTrue(theremin.signal() >= 3 && theremin.signal() <= 5, "Six blocks off it reads low: " + theremin.signal());
				Vec3 gone = away(helper, 40.0);
				player.setPos(gone.x, gone.y, gone.z);
				helper.runAfterDelay(ThereminBlockEntity.SENSE_TICKS + 1, () -> {
					// Out of range it no longer hears this player (another test's player could still be near it).
					helper.assertTrue(theremin.signal() == ThereminBlockEntity.signalFor(theremin.distance())
							&& theremin.distance() <= ThereminBlockEntity.RANGE, "Out of range, it doesn't hear the player: " + theremin.distance());
					helper.succeed();
				});
			});
		});
	}

	/** An empty hand switches it on and off; a redstone signal plays it too; a player close to it earns Good Vibrations. */
	@GameTest(maxTicks = 40)
	public void itPlaysWhenSwitchedOnOrPowered(GameTestHelper helper) {
		theremin(helper);
		ServerPlayer player = player(helper, away(helper, 1.5));
		BlockPos absolute = helper.absolutePos(THEREMIN);
		player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
		player.gameMode.useItemOn(player, helper.getLevel(), ItemStack.EMPTY, InteractionHand.MAIN_HAND,
				new BlockHitResult(Vec3.atCenterOf(absolute), Direction.NORTH, absolute, false));
		helper.assertTrue(ThereminBlock.playing(helper.getBlockState(THEREMIN)), "An empty hand switches it on");
		helper.runAfterDelay(ThereminBlockEntity.SENSE_TICKS + 1, () -> {
			helper.assertTrue(earned(player, "good_vibrations"), "Playing it close earns Good Vibrations");
			player.gameMode.useItemOn(player, helper.getLevel(), ItemStack.EMPTY, InteractionHand.MAIN_HAND,
					new BlockHitResult(Vec3.atCenterOf(absolute), Direction.NORTH, absolute, false));
			helper.assertTrue(!ThereminBlock.playing(helper.getBlockState(THEREMIN)), "And off again");
			helper.setBlock(THEREMIN.east(), Blocks.REDSTONE_BLOCK.defaultBlockState());
			helper.runAfterDelay(2, () -> {
				helper.assertTrue(helper.getBlockState(THEREMIN).getValue(ThereminBlock.POWERED) && ThereminBlock.playing(helper.getBlockState(THEREMIN)),
						"A redstone signal plays it");
				helper.setBlock(THEREMIN.east(), Blocks.AIR.defaultBlockState());
				helper.runAfterDelay(2, () -> {
					helper.assertTrue(!ThereminBlock.playing(helper.getBlockState(THEREMIN)), "Unpowered and switched off, it is silent");
					helper.succeed();
				});
			});
		});
	}

	/** The recipe, loot table and advancement load. */
	@GameTest
	public void thereminDataLoads(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		helper.assertTrue(level.recipeAccess().byKey(ResourceKey.create(Registries.RECIPE, Jugcraft.id("theremin"))).isPresent(),
				"The recipe loads (with the machines feature on)");
		helper.assertTrue(level.getServer().reloadableRegistries().getLootTable(ResourceKey.create(Registries.LOOT_TABLE,
				Jugcraft.id("blocks/theremin"))) != LootTable.EMPTY, "The loot table loads");
		for (String id : List.of("good_vibrations")) {
			helper.assertTrue(level.getServer().getAdvancements().get(Jugcraft.id(id)) != null, id + " loads");
		}
		helper.succeed();
	}
}
