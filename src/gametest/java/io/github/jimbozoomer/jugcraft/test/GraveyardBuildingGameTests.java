package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.agriculture.Epitaph;
import io.github.jimbozoomer.jugcraft.agriculture.Epitaphs;
import io.github.jimbozoomer.jugcraft.agriculture.GraveyardBuildingBlock;
import io.github.jimbozoomer.jugcraft.agriculture.HeadstoneBlock;
import io.github.jimbozoomer.jugcraft.agriculture.HeadstoneBlockEntity;
import io.github.jimbozoomer.jugcraft.agriculture.JugcraftAgriculture;
import java.util.List;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * In-game tests for the graveyard pack's buildings (pack 3): the family mausoleum places whole from its item (85
 * blocks) leaving its doorway and room open, or not at all; its family name and its crypt fronts each take their own
 * inscription, checked on the server like an epitaph; it weathers as one, its lamp lights its room, and breaking any
 * block of it drops it once with every inscription, which comes back when it is placed again. The other buildings
 * place whole, the gateway's lanterns give light, and the pack's data loads.
 */
public class GraveyardBuildingGameTests {
	private static final String MAUSOLEUM = "family_mausoleum";

	private static Block block(String id) {
		return JugcraftAgriculture.block(id);
	}

	private static Item item(String id) {
		return JugcraftAgriculture.item(id);
	}

	private static void floor(GameTestHelper helper) {
		for (int x = 0; x <= 7; x++) {
			for (int z = 0; z <= 7; z++) {
				helper.setBlock(new BlockPos(x, 1, z), Blocks.STONE);
			}
		}
	}

	private static ServerPlayer player(GameTestHelper helper, BlockPos standAt, ItemStack held) {
		ServerPlayer player = helper.makeMockServerPlayerInLevel();
		player.setGameMode(GameType.SURVIVAL);
		stand(helper, player, standAt);
		player.setItemInHand(InteractionHand.MAIN_HAND, held);
		return player;
	}

	private static void stand(GameTestHelper helper, ServerPlayer player, BlockPos at) {
		BlockPos absolute = helper.absolutePos(at);
		player.setPos(absolute.getX() + 0.5, absolute.getY(), absolute.getZ() + 0.5);
	}

	private static InteractionResult use(GameTestHelper helper, ServerPlayer player, BlockPos pos, Direction side) {
		BlockPos absolute = helper.absolutePos(pos);
		BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(absolute).relative(side, 0.5), side, absolute, false);
		return player.gameMode.useItemOn(player, helper.getLevel(), player.getMainHandItem(), InteractionHand.MAIN_HAND, hit);
	}

	private static List<ItemEntity> itemsAround(GameTestHelper helper, BlockPos pos) {
		return helper.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(helper.absolutePos(pos)).inflate(5.0));
	}

	/**
	 * Where grid cell (gx, gy, gz) of the mausoleum (its design grid: x across from its west side, y up, z back from its
	 * front) stands when its part 0 is at {@code master} and it faces north (placed by someone looking south).
	 */
	private static BlockPos cell(BlockPos master, int gx, int gy, int gz) {
		return master.offset(gx - 2, gy, gz);
	}

	private static BlockPos placeMausoleum(GameTestHelper helper, ServerPlayer mason) {
		mason.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(item(MAUSOLEUM)));
		use(helper, mason, new BlockPos(4, 1, 1), Direction.UP);
		return new BlockPos(4, 2, 1);
	}

	/**
	 * Placed by a player looking south, the mausoleum stands five blocks wide, four tall and five deep behind its front
	 * step, every one of its 85 blocks part of it, facing north; its doorway and the room behind it are left open. With a
	 * block where its roof goes, it is not placed at all.
	 */
	@GameTest
	public void mausoleumPlacesWhole(GameTestHelper helper) {
		floor(helper);
		ServerPlayer mason = player(helper, new BlockPos(4, 2, 0), ItemStack.EMPTY);
		BlockPos master = placeMausoleum(helper, mason);
		GraveyardBuildingBlock mausoleum = (GraveyardBuildingBlock) block(MAUSOLEUM);
		int[][] cells = mausoleum.layout().cells();
		helper.assertTrue(cells.length == 85, "The mausoleum has 85 blocks, not " + cells.length);
		BlockPos absolute = helper.absolutePos(master);
		for (int part = 0; part < cells.length; part++) {
			BlockPos pos = mausoleum.partPos(absolute, Direction.NORTH, part);
			BlockState state = helper.getLevel().getBlockState(pos);
			helper.assertTrue(state.is(mausoleum) && mausoleum.part(state) == part && state.getValue(HeadstoneBlock.FACING) == Direction.NORTH,
					"Part " + part + " stands at " + pos);
		}
		helper.assertBlockPresent(Blocks.AIR, cell(master, 2, 0, 1));
		helper.assertBlockPresent(Blocks.AIR, cell(master, 2, 1, 1));
		for (int gx = 1; gx <= 3; gx++) {
			for (int gz = 2; gz <= 3; gz++) {
				helper.assertBlockPresent(Blocks.AIR, cell(master, gx, 0, gz));
				helper.assertBlockPresent(Blocks.AIR, cell(master, gx, 1, gz));
			}
		}
		helper.assertTrue(helper.getBlockState(cell(master, 0, 3, 4)).is(mausoleum), "Its roof reaches the back corner");
		// A block in the way of its roof: nothing is placed.
		for (int part = 0; part < cells.length; part++) {
			helper.getLevel().removeBlock(mausoleum.partPos(absolute, Direction.NORTH, part), false);
		}
		helper.setBlock(cell(master, 4, 3, 4), Blocks.STONE);
		placeMausoleum(helper, mason);
		helper.assertBlockNotPresent(mausoleum, master);
		helper.succeed();
	}

	/**
	 * The chisel on a crypt front opens a session for that crypt's own inscription, and on any other block of the
	 * mausoleum for the family name over the door; each is cut only with its own session, and none beyond those it
	 * has. A named Name Tag cuts the first line of the inscription of the block it is used on.
	 */
	@GameTest
	public void inscriptionsAreCutWhereTheChiselIsUsed(GameTestHelper helper) {
		floor(helper);
		ServerPlayer mason = player(helper, new BlockPos(4, 2, 0), ItemStack.EMPTY);
		BlockPos master = placeMausoleum(helper, mason);
		ServerLevel level = helper.getLevel();
		BlockPos absolute = helper.absolutePos(master);
		HeadstoneBlockEntity stone = helper.getBlockEntity(master, HeadstoneBlockEntity.class);
		// Inside, by the west wall: the top crypt of its front bay is inscription 1, the lowest of its back bay 6.
		stand(helper, mason, new BlockPos(3, 2, 3));
		mason.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(item(Epitaphs.CHISEL)));
		use(helper, mason, cell(master, 0, 2, 2), Direction.EAST);
		helper.assertTrue(Epitaphs.engrave(mason, absolute, 2, List.of("WRONG")) == Epitaphs.Result.NO_SESSION,
				"A session for one crypt cuts no other");
		use(helper, mason, cell(master, 0, 2, 2), Direction.EAST);
		helper.assertTrue(Epitaphs.engrave(mason, absolute, 1, List.of("ELIZA HALLOWAY", "1801 - 1866")) == Epitaphs.Result.ENGRAVED,
				"The crypt's inscription is cut");
		use(helper, mason, cell(master, 0, 0, 3), Direction.EAST);
		helper.assertTrue(Epitaphs.engrave(mason, absolute, 6, List.of("JOHN HALLOWAY", "1799 - 1870")) == Epitaphs.Result.ENGRAVED,
				"The back bay's lowest crypt is the sixth");
		helper.assertTrue(stone.inscription(1).lines().equals(List.of("ELIZA HALLOWAY", "1801 - 1866"))
				&& stone.inscription(6).lines().get(0).equals("JOHN HALLOWAY") && stone.epitaph().isBlank(), "Each crypt keeps its own");
		// No session reaches past the inscriptions the mausoleum has.
		Epitaphs.startSession(mason, absolute, 40, helper.absolutePos(cell(master, 0, 2, 2)));
		helper.assertTrue(Epitaphs.engrave(mason, absolute, 40, List.of("NOWHERE")) == Epitaphs.Result.NOT_A_HEADSTONE,
				"There is no inscription 40");
		// The family name over the door, cut from the portico.
		stand(helper, mason, new BlockPos(4, 2, 0));
		use(helper, mason, cell(master, 1, 2, 0), Direction.NORTH);
		helper.assertTrue(Epitaphs.engrave(mason, absolute, 0, List.of("HALLOWAY")) == Epitaphs.Result.ENGRAVED, "The family name is cut");
		ItemStack tag = new ItemStack(Items.NAME_TAG);
		tag.set(DataComponents.CUSTOM_NAME, Component.literal("THE HALLOWAYS"));
		mason.setItemInHand(InteractionHand.MAIN_HAND, tag);
		use(helper, mason, cell(master, 3, 2, 0), Direction.NORTH);
		helper.assertTrue(stone.epitaph().lines().equals(List.of("THE HALLOWAYS")), "A named tag cuts the family name");
		helper.assertTrue(stone.inscription(1).lines().get(0).equals("ELIZA HALLOWAY"), "and leaves the crypts alone");
		helper.succeed();
	}

	/**
	 * Bone meal on its roof ages every block of the mausoleum; its sanctuary lamp gives light; breaking a block of
	 * its roof breaks it all and drops it once, keeping its family name and its crypts' inscriptions, which come back
	 * when it is placed again.
	 */
	@GameTest
	public void mausoleumWeathersLightsAndIsKept(GameTestHelper helper) {
		floor(helper);
		ServerPlayer mason = player(helper, new BlockPos(4, 2, 0), ItemStack.EMPTY);
		BlockPos master = placeMausoleum(helper, mason);
		GraveyardBuildingBlock mausoleum = (GraveyardBuildingBlock) block(MAUSOLEUM);
		HeadstoneBlockEntity stone = helper.getBlockEntity(master, HeadstoneBlockEntity.class);
		stone.engrave(Epitaph.of(List.of("HALLOWAY")));
		stone.engrave(3, Epitaph.of(List.of("MARY HALLOWAY")));
		mason.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.BONE_MEAL, 4));
		use(helper, mason, cell(master, 2, 3, 0), Direction.NORTH);
		BlockPos absolute = helper.absolutePos(master);
		for (int part = 0; part < mausoleum.layout().cells().length; part++) {
			BlockState state = helper.getLevel().getBlockState(mausoleum.partPos(absolute, Direction.NORTH, part));
			helper.assertTrue(HeadstoneBlock.stage(state) == 1, "Every block ages together, part " + part);
		}
		helper.assertTrue(helper.getBlockState(cell(master, 2, 2, 3)).getLightEmission() == 10, "The sanctuary lamp burns over the room");
		helper.getLevel().destroyBlock(helper.absolutePos(cell(master, 4, 3, 4)), true);
		helper.assertBlockNotPresent(mausoleum, master);
		helper.assertBlockNotPresent(mausoleum, cell(master, 0, 0, 4));
		List<ItemEntity> drops = itemsAround(helper, cell(master, 2, 1, 2)).stream().filter(e -> e.getItem().is(item(MAUSOLEUM))).toList();
		helper.assertTrue(drops.size() == 1 && drops.get(0).getItem().getCount() == 1, "It drops once");
		ItemStack kept = drops.get(0).getItem();
		helper.assertTrue(kept.get(JugcraftAgriculture.EPITAPH) != null && kept.get(JugcraftAgriculture.INSCRIPTIONS) != null
				&& kept.get(JugcraftAgriculture.INSCRIPTIONS).get(2).lines().equals(List.of("MARY HALLOWAY")), "The item keeps every inscription");
		mason.setItemInHand(InteractionHand.MAIN_HAND, kept.copy());
		use(helper, mason, new BlockPos(4, 1, 1), Direction.UP);
		HeadstoneBlockEntity again = helper.getBlockEntity(master, HeadstoneBlockEntity.class);
		helper.assertTrue(again.epitaph().lines().equals(List.of("HALLOWAY")) && again.inscription(3).lines().equals(List.of("MARY HALLOWAY")),
				"Placed again, its inscriptions come back");
		helper.succeed();
	}

	/**
	 * The columbarium and the lych gate place whole, the gate's passage left free under its tie beams; each of the
	 * columbarium's niches cuts its own inscription, and the rest of it the one on its frieze.
	 */
	@GameTest
	public void otherBuildingsPlaceWhole(GameTestHelper helper) {
		floor(helper);
		ServerPlayer mason = player(helper, new BlockPos(1, 2, 0), new ItemStack(item("columbarium")));
		use(helper, mason, new BlockPos(1, 1, 1), Direction.UP);
		GraveyardBuildingBlock columbarium = (GraveyardBuildingBlock) block("columbarium");
		BlockPos absolute = helper.absolutePos(new BlockPos(1, 2, 1));
		for (int part = 0; part < columbarium.layout().cells().length; part++) {
			helper.assertTrue(helper.getLevel().getBlockState(columbarium.partPos(absolute, Direction.NORTH, part)).is(columbarium),
					"Columbarium part " + part);
		}
		// Looking south, its right is west: its niches' top row runs from x 2 (the viewer's left) to x 0.
		helper.assertTrue(columbarium.layout().slot(columbarium.part(helper.getBlockState(new BlockPos(2, 3, 1)))) == 1
				&& columbarium.layout().slot(columbarium.part(helper.getBlockState(new BlockPos(0, 2, 1)))) == 6
				&& columbarium.layout().slot(columbarium.part(helper.getBlockState(new BlockPos(1, 4, 1)))) == 0, "Each niche its own inscription");

		// Looking south, the lych gate runs east from its right-hand wall: walls at x 3 and 6, the passage between.
		ServerPlayer builder = player(helper, new BlockPos(3, 2, 3), new ItemStack(item("lych_gate")));
		use(helper, builder, new BlockPos(3, 1, 4), Direction.UP);
		GraveyardBuildingBlock gate = (GraveyardBuildingBlock) block("lych_gate");
		for (int y = 2; y <= 5; y++) {
			helper.assertBlockPresent(gate, new BlockPos(3, y, 4));
			helper.assertBlockPresent(gate, new BlockPos(6, y, 5));
		}
		for (int x = 4; x <= 5; x++) {
			for (int z = 4; z <= 5; z++) {
				helper.assertBlockPresent(Blocks.AIR, new BlockPos(x, 2, z));
				helper.assertBlockPresent(Blocks.AIR, new BlockPos(x, 3, z));
				helper.assertBlockPresent(gate, new BlockPos(x, 4, z));
			}
		}
		helper.succeed();
	}

	/** The cemetery gateway places whole with its opening left free, and its lanterns give light. */
	@GameTest
	public void gatewayPlacesWholeAndLights(GameTestHelper helper) {
		floor(helper);
		ServerPlayer mason = player(helper, new BlockPos(2, 2, 1), new ItemStack(item("cemetery_gateway")));
		use(helper, mason, new BlockPos(2, 1, 3), Direction.UP);
		GraveyardBuildingBlock gateway = (GraveyardBuildingBlock) block("cemetery_gateway");
		// Looking south, it runs east from its right-hand pier: the piers stand at x 2 and x 6, the opening between.
		for (int y = 2; y <= 5; y++) {
			helper.assertBlockPresent(gateway, new BlockPos(6, y, 3));
			helper.assertBlockPresent(gateway, new BlockPos(2, y, 3));
		}
		for (int x = 3; x <= 5; x++) {
			helper.assertBlockPresent(Blocks.AIR, new BlockPos(x, 2, 3));
			helper.assertBlockPresent(Blocks.AIR, new BlockPos(x, 3, 3));
			helper.assertBlockPresent(gateway, new BlockPos(x, 5, 3));
		}
		helper.assertTrue(helper.getBlockState(new BlockPos(6, 5, 3)).getLightEmission() == 14
				&& helper.getBlockState(new BlockPos(2, 5, 3)).getLightEmission() == 14, "Its lanterns burn");
		helper.succeed();
	}

	/** Every building's recipe and loot, and the Bronze Mausoleum Door's, load; the door opens by hand. */
	@GameTest
	public void buildingDataLoads(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		for (String id : List.of(MAUSOLEUM, "lych_gate", "cemetery_gateway", "columbarium", JugcraftAgriculture.MAUSOLEUM_DOOR)) {
			helper.assertTrue(level.recipeAccess().byKey(ResourceKey.create(Registries.RECIPE, Jugcraft.id(id))).isPresent(), id + " has a recipe");
			helper.assertTrue(level.getServer().reloadableRegistries().getLootTable(ResourceKey.create(Registries.LOOT_TABLE,
					Jugcraft.id("blocks/" + id))) != LootTable.EMPTY, id + "'s loot loads");
		}
		helper.setBlock(new BlockPos(1, 1, 1), Blocks.STONE);
		ServerPlayer visitor = player(helper, new BlockPos(1, 2, 0), new ItemStack(item(JugcraftAgriculture.MAUSOLEUM_DOOR)));
		use(helper, visitor, new BlockPos(1, 1, 1), Direction.UP);
		helper.assertBlockPresent(block(JugcraftAgriculture.MAUSOLEUM_DOOR), new BlockPos(1, 3, 1));
		visitor.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
		BlockPos door = helper.absolutePos(new BlockPos(1, 2, 1));
		BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(door), Direction.NORTH, door, false);
		visitor.gameMode.useItemOn(visitor, level, ItemStack.EMPTY, InteractionHand.MAIN_HAND, hit);
		helper.assertTrue(helper.getBlockState(new BlockPos(1, 2, 1)).getValue(net.minecraft.world.level.block.DoorBlock.OPEN), "It opens by hand");
		helper.succeed();
	}
}
