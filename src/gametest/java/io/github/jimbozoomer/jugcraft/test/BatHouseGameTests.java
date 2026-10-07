package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.agriculture.BatHouseBlock;
import io.github.jimbozoomer.jugcraft.agriculture.BatHouseBlockEntity;
import io.github.jimbozoomer.jugcraft.agriculture.JugcraftAgriculture;
import io.github.jimbozoomer.jugcraft.chemistry.FertilizerItem;
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
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * In-game tests for the Bat House: at dusk its roosting bats fly out (with a chance of a new one moving in first), tagged
 * as its bats, and anyone near earns Night Shift; at dawn the nearest bats come in to roost, up to its room, each leaving
 * a guano that piles up on its ledge; an empty hand scoops the guano up; comparators read the bats; guano fertilizes a
 * 3x3 patch of crops; and the recipes load.
 */
public class BatHouseGameTests {
	private static ServerPlayer player(GameTestHelper helper, BlockPos standAt) {
		ServerPlayer player = helper.makeMockServerPlayerInLevel();
		player.setGameMode(GameType.SURVIVAL);
		BlockPos absolute = helper.absolutePos(standAt);
		player.setPos(absolute.getX() + 0.5, absolute.getY(), absolute.getZ() + 0.5);
		return player;
	}

	private static boolean earned(ServerPlayer player, String id) {
		AdvancementHolder advancement = player.level().getServer().getAdvancements().get(Jugcraft.id(id));
		return advancement != null && player.getAdvancements().getOrStartProgress(advancement).isDone();
	}

	private static BatHouseBlockEntity house(GameTestHelper helper, BlockPos pos) {
		helper.setBlock(pos.south(), Blocks.OAK_PLANKS);
		helper.setBlock(pos, JugcraftAgriculture.block("bat_house").defaultBlockState().setValue(BatHouseBlock.FACING, Direction.NORTH));
		return helper.getBlockEntity(pos, BatHouseBlockEntity.class);
	}

	private static List<? extends Entity> bats(GameTestHelper helper, BlockPos pos, int range) {
		return helper.getLevel().getEntities(EntityTypes.BAT, new AABB(helper.absolutePos(pos)).inflate(range), Entity::isAlive);
	}

	/**
	 * Dusk lets the roosting bats out, tagged as the house's, and anyone near earns Night Shift; a house with room may gain
	 * a bat first. Dawn brings the nearest bats back in, up to four, each leaving a guano, which shows on the ledge.
	 */
	@GameTest(maxTicks = 20)
	public void batsFlyOutAtDuskAndRoostAtDawn(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		BlockPos pos = new BlockPos(3, 3, 3);
		BatHouseBlockEntity house = house(helper, pos);
		ServerPlayer watcher = player(helper, new BlockPos(3, 2, 1));
		house.set(3, 0);
		int out = house.release(level);
		List<? extends Entity> flying = bats(helper, pos, 4);
		helper.assertTrue(out == 3 && house.residents() == 0 && flying.size() == 3
				&& flying.stream().allMatch(bat -> bat.entityTags().contains(BatHouseBlockEntity.TAG)), "Three tagged bats fly out: " + flying.size());
		helper.assertTrue(earned(watcher, "night_shift"), "and the watcher earns Night Shift");
		for (int i = 0; i < 3; i++) {
			Entity wild = EntityTypes.BAT.create(level, EntitySpawnReason.MOB_SUMMONED);
			BlockPos at = helper.absolutePos(new BlockPos(1 + i, 4, 6));
			wild.setPos(at.getX() + 0.5, at.getY(), at.getZ() + 0.5);
			level.addFreshEntity(wild);
		}
		int came = house.dawn(level);
		helper.assertTrue(came == BatHouseBlockEntity.CAPACITY && house.residents() == BatHouseBlockEntity.CAPACITY
				&& bats(helper, pos, 8).size() == 2, "At dawn the four nearest bats come in, wild ones too: " + came);
		helper.assertTrue(house.guano() == BatHouseBlockEntity.CAPACITY
				&& helper.getBlockState(pos).getValue(BatHouseBlock.GUANO) == BatHouseBlock.guanoLevel(BatHouseBlockEntity.CAPACITY),
				"Each leaves a guano, which shows on the ledge");
		BlockPos absolute = helper.absolutePos(pos);
		helper.assertTrue(level.getBlockState(absolute).getAnalogOutputSignal(level, absolute, Direction.NORTH) == 15, "A full house reads 15");

		BatHouseBlockEntity empty = house(helper, new BlockPos(6, 3, 3));
		RandomSource always = RandomSource.create(1L);
		int gained = 0;
		for (int night = 0; night < 8; night++) {
			gained += empty.dusk(level, always);
		}
		helper.assertTrue(gained > 0, "An empty house gains bats at dusk while mobs spawn: " + gained);
		for (Entity bat : bats(helper, new BlockPos(4, 3, 3), 12)) {
			bat.discard();
		}
		helper.succeed();
	}

	/** An empty hand scoops up the guano; guano fertilizes the crops in a 3x3 patch, and makes phosphate. */
	@GameTest(maxTicks = 20)
	public void guanoIsScoopedAndFertilizes(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		BlockPos pos = new BlockPos(3, 3, 6);
		BatHouseBlockEntity house = house(helper, pos);
		house.set(2, 7);
		ServerPlayer player = player(helper, new BlockPos(3, 2, 4));
		player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
		BlockPos absolute = helper.absolutePos(pos);
		player.gameMode.useItemOn(player, level, ItemStack.EMPTY, InteractionHand.MAIN_HAND,
				new BlockHitResult(Vec3.atCenterOf(absolute), Direction.NORTH, absolute, false));
		helper.assertTrue(player.getInventory().countItem(JugcraftAgriculture.item("bat_guano")) == 7 && house.guano() == 0
				&& helper.getBlockState(pos).getValue(BatHouseBlock.GUANO) == 0, "An empty hand scoops up the seven guano");

		for (int x = 0; x <= 4; x++) {
			for (int z = 0; z <= 2; z++) {
				helper.setBlock(new BlockPos(x, 1, z), Blocks.FARMLAND);
				helper.setBlock(new BlockPos(x, 2, z), Blocks.WHEAT);
			}
		}
		int grown = FertilizerItem.fertilize(level, helper.absolutePos(new BlockPos(1, 2, 1)), JugcraftAgriculture.GUANO_RADIUS,
				JugcraftAgriculture.GUANO_DOSES);
		helper.assertTrue(grown == 9, "Guano grows the nine crops of a 3x3 patch: " + grown);
		helper.assertTrue(helper.getBlockState(new BlockPos(4, 2, 1)).getValue(CropBlock.AGE) == 0, "and none beyond it");
		for (String recipe : List.of("bat_house", "phosphate_from_bat_guano")) {
			helper.assertTrue(level.recipeAccess().byKey(ResourceKey.create(Registries.RECIPE, Jugcraft.id(recipe))).isPresent(), recipe + " loads");
		}
		helper.succeed();
	}
}
