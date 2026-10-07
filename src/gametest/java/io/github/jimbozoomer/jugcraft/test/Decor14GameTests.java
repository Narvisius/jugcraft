package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.agriculture.CostumeTrunkBlock;
import io.github.jimbozoomer.jugcraft.agriculture.CostumeTrunkBlockEntity;
import io.github.jimbozoomer.jugcraft.agriculture.CostumedMobs;
import io.github.jimbozoomer.jugcraft.agriculture.JugcraftAgriculture;
import io.github.jimbozoomer.jugcraft.agriculture.TrickOrTreat;
import java.util.List;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.equipment.Equippable;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * In-game tests for costumes: the six outfits (worn on the head, trick-or-treat costumes and costume hats, worn by
 * costumed mobs), the Costume Trunk (packing costumes away, changing into the next, taking the last out, its lid, its
 * comparator signal and what it drops), and that their data loads.
 */
public class Decor14GameTests {
	private static Item item(String id) {
		return JugcraftAgriculture.item(id);
	}

	private static Block block(String id) {
		return JugcraftAgriculture.block(id);
	}

	private static void floor(GameTestHelper helper) {
		for (int x = 0; x <= 7; x++) {
			for (int z = 0; z <= 7; z++) {
				helper.setBlock(new BlockPos(x, 1, z), Blocks.STONE);
			}
		}
	}

	private static ServerPlayer player(GameTestHelper helper, BlockPos standAt, float yRot, ItemStack held) {
		ServerPlayer player = helper.makeMockServerPlayerInLevel();
		player.setGameMode(GameType.SURVIVAL);
		BlockPos absolute = helper.absolutePos(standAt);
		player.setPos(absolute.getX() + 0.5, absolute.getY(), absolute.getZ() + 0.5);
		player.setYRot(yRot);
		player.setItemInHand(InteractionHand.MAIN_HAND, held);
		return player;
	}

	private static InteractionResult use(GameTestHelper helper, ServerPlayer player, BlockPos pos, Direction side) {
		BlockPos absolute = helper.absolutePos(pos);
		BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(absolute).relative(side, 0.5), side, absolute, false);
		return player.gameMode.useItemOn(player, helper.getLevel(), player.getMainHandItem(), InteractionHand.MAIN_HAND, hit);
	}

	private static int dropped(GameTestHelper helper, Item item) {
		AABB area = new AABB(helper.absolutePos(BlockPos.ZERO)).expandTowards(8, 8, 8).inflate(2.0);
		return helper.getLevel().getEntitiesOfClass(ItemEntity.class, area, entity -> entity.getItem().is(item)).stream()
				.mapToInt(entity -> entity.getItem().getCount()).sum();
	}

	// ---------------------------------------------------------------- the outfits

	/** Every outfit is worn on the head, is a trick-or-treat costume and a costume hat, and costumed mobs wear it. */
	@GameTest
	public void outfitsAreCostumes(GameTestHelper helper) {
		for (String outfit : JugcraftAgriculture.OUTFITS) {
			ItemStack stack = new ItemStack(item(outfit));
			Equippable worn = stack.get(DataComponents.EQUIPPABLE);
			helper.assertTrue(worn != null && worn.slot() == EquipmentSlot.HEAD && worn.assetId().isPresent(), outfit + " is worn on the head");
			helper.assertTrue(stack.is(TrickOrTreat.COSTUMES) && stack.is(TrickOrTreat.COSTUME_HATS), outfit + " is a costume and a costume hat");
			helper.assertTrue(CostumedMobs.COSTUMES.contains(Jugcraft.id(outfit).toString()), "Costumed mobs wear " + outfit);
		}
		helper.succeed();
	}

	// ---------------------------------------------------------------- the trunk

	/**
	 * A costume in hand goes in (anything else doesn't); an empty hand changes into the costume at the front, packing the
	 * one worn at the back; sneaking takes the last one out; a helmet that isn't a costume stays on. The lid opens and
	 * falls shut; comparators read how full it is; it holds nine.
	 */
	@GameTest(maxTicks = 100)
	public void costumeTrunksKeepAndChangeOutfits(GameTestHelper helper) {
		floor(helper);
		BlockPos pos = new BlockPos(2, 2, 2);
		helper.setBlock(pos, block("costume_trunk").defaultBlockState().setValue(CostumeTrunkBlock.FACING, Direction.SOUTH));
		CostumeTrunkBlockEntity trunk = helper.getBlockEntity(pos, CostumeTrunkBlockEntity.class);
		ServerPlayer player = player(helper, new BlockPos(2, 2, 4), 180.0F, new ItemStack(Items.STICK));
		use(helper, player, pos, Direction.SOUTH);
		helper.assertTrue(trunk.count() == 0 && player.getMainHandItem().is(Items.STICK), "A stick isn't a costume");
		player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(item("vampire_cape")));
		use(helper, player, pos, Direction.SOUTH);
		player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(item("mummy_wraps")));
		use(helper, player, pos, Direction.SOUTH);
		helper.assertTrue(trunk.count() == 2 && player.getMainHandItem().isEmpty(), "Two outfits packed away");
		helper.assertTrue(helper.getBlockState(pos).getValue(CostumeTrunkBlock.OPEN), "The lid opens");

		use(helper, player, pos, Direction.SOUTH);
		helper.assertTrue(player.getItemBySlot(EquipmentSlot.HEAD).is(item("vampire_cape")) && trunk.count() == 1,
				"An empty hand puts on the first outfit");
		use(helper, player, pos, Direction.SOUTH);
		helper.assertTrue(player.getItemBySlot(EquipmentSlot.HEAD).is(item("mummy_wraps")) && trunk.count() == 1
				&& trunk.outfits().getFirst().is(item("vampire_cape")), "Changing again packs the cape and puts on the wraps");
		player.setShiftKeyDown(true);
		use(helper, player, pos, Direction.SOUTH);
		player.setShiftKeyDown(false);
		helper.assertTrue(player.getMainHandItem().is(item("vampire_cape")) && trunk.count() == 0, "Sneaking takes the last one out");

		player.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.IRON_HELMET));
		use(helper, player, pos, Direction.SOUTH);
		player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
		use(helper, player, pos, Direction.SOUTH);
		helper.assertTrue(player.getItemBySlot(EquipmentSlot.HEAD).is(Items.IRON_HELMET) && trunk.count() == 1, "A helmet stays on");

		for (String outfit : List.of("skeleton_suit", "werewolf_mask", "cat_ears_and_tail", "bat_wings", "witch_hat", "ghost_sheet",
				"scarecrow_hat", "mummy_wraps", "skeleton_suit")) {
			player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(item(outfit)));
			use(helper, player, pos, Direction.SOUTH);
		}
		helper.assertTrue(trunk.count() == CostumeTrunkBlockEntity.SLOTS && player.getMainHandItem().is(item("skeleton_suit")),
				"It holds nine, and refuses a tenth");
		helper.assertTrue(helper.getLevel().getBlockState(helper.absolutePos(pos)).getAnalogOutputSignal(helper.getLevel(), helper.absolutePos(pos),
				Direction.NORTH) == 15, "A full trunk gives comparators 15");
		helper.runAfterDelay(CostumeTrunkBlock.OPEN_TICKS + 5, () -> {
			helper.assertTrue(!helper.getBlockState(pos).getValue(CostumeTrunkBlock.OPEN), "The lid falls shut");
			helper.succeed();
		});
	}

	/** Broken, a trunk drops itself and spills its costumes. */
	@GameTest(maxTicks = 40)
	public void costumeTrunksSpillWhenBroken(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		floor(helper);
		BlockPos pos = new BlockPos(2, 2, 2);
		helper.setBlock(pos, block("costume_trunk"));
		CostumeTrunkBlockEntity trunk = helper.getBlockEntity(pos, CostumeTrunkBlockEntity.class);
		trunk.store(new ItemStack(item("bat_wings")));
		trunk.store(new ItemStack(Items.CARVED_PUMPKIN, 3));
		helper.assertTrue(trunk.count() == 2 && trunk.outfits().get(1).getCount() == 1, "One of each goes in, even from a stack");
		level.destroyBlock(helper.absolutePos(pos), true);
		helper.assertTrue(dropped(helper, item("costume_trunk")) == 1 && dropped(helper, item("bat_wings")) == 1
				&& dropped(helper, Items.CARVED_PUMPKIN) == 1, "The trunk and its costumes drop");
		helper.succeed();
	}

	// ---------------------------------------------------------------- data

	/** The recipes and the trunk's loot table load. */
	@GameTest
	public void costumesDataLoads(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		for (String id : List.of("vampire_cape", "mummy_wraps", "skeleton_suit", "werewolf_mask", "cat_ears_and_tail", "bat_wings", "costume_trunk")) {
			helper.assertTrue(level.recipeAccess().byKey(ResourceKey.create(Registries.RECIPE, Jugcraft.id(id))).isPresent(), "Recipe " + id + " loads");
		}
		helper.assertTrue(level.getServer().reloadableRegistries().getLootTable(ResourceKey.create(Registries.LOOT_TABLE,
				Jugcraft.id("blocks/costume_trunk"))) != LootTable.EMPTY, "The trunk's loot table loads");
		helper.succeed();
	}
}
