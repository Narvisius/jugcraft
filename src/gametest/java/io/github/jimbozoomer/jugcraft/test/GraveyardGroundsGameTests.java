package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.agriculture.GraveVaseBlock;
import io.github.jimbozoomer.jugcraft.agriculture.HeadstoneBlock;
import io.github.jimbozoomer.jugcraft.agriculture.JugcraftAgriculture;
import io.github.jimbozoomer.jugcraft.agriculture.LampPostBlock;
import io.github.jimbozoomer.jugcraft.agriculture.Seat;
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
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * In-game tests for the graveyard pack's grounds (pack 4): the kerbed grave, planted grave, open grave and memorial
 * bench place whole and the bench seats a player; the grave vase takes small flowers as bouquets of their colour,
 * calms the graves about it while they are fresh and wilts; the lamp post stands three blocks tall, lights by night
 * and goes out by day, and breaks as one; and the pack's data loads.
 */
public class GraveyardGroundsGameTests {
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
		BlockPos absolute = helper.absolutePos(standAt);
		player.setPos(absolute.getX() + 0.5, absolute.getY(), absolute.getZ() + 0.5);
		player.setItemInHand(InteractionHand.MAIN_HAND, held);
		return player;
	}

	private static InteractionResult use(GameTestHelper helper, ServerPlayer player, BlockPos pos, Direction side) {
		BlockPos absolute = helper.absolutePos(pos);
		BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(absolute).relative(side, 0.5), side, absolute, false);
		return player.gameMode.useItemOn(player, helper.getLevel(), player.getMainHandItem(), InteractionHand.MAIN_HAND, hit);
	}

	private static boolean earned(ServerPlayer player, String id) {
		AdvancementHolder advancement = player.level().getServer().getAdvancements().get(Jugcraft.id(id));
		return advancement != null && player.getAdvancements().getOrStartProgress(advancement).isDone();
	}

	/**
	 * Placed by a player looking south: the kerbed, planted and open graves run back two blocks, the bench runs a
	 * block to the placer's right (west); used with an empty hand, the bench sits the player down.
	 */
	@GameTest
	public void groundsPlaceWholeAndTheBenchSeats(GameTestHelper helper) {
		floor(helper);
		ServerPlayer keeper = player(helper, new BlockPos(1, 2, 0), ItemStack.EMPTY);
		int x = 1;
		for (String id : List.of("kerbed_grave", "planted_grave", "open_grave")) {
			keeper.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(item(id)));
			use(helper, keeper, new BlockPos(x, 1, 1), Direction.UP);
			helper.assertBlockPresent(block(id), new BlockPos(x, 2, 1));
			helper.assertBlockPresent(block(id), new BlockPos(x, 2, 2));
			x += 2;
		}
		keeper.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(item("memorial_bench")));
		use(helper, keeper, new BlockPos(6, 1, 5), Direction.UP);
		helper.assertBlockPresent(block("memorial_bench"), new BlockPos(6, 2, 5));
		helper.assertBlockPresent(block("memorial_bench"), new BlockPos(5, 2, 5));
		keeper.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
		use(helper, keeper, new BlockPos(5, 2, 5), Direction.UP);
		helper.assertTrue(keeper.getVehicle() instanceof Seat, "The bench sits the player down");
		helper.succeed();
	}

	/**
	 * A small flower in the grave vase makes a fresh bouquet of its colour (red for a poppy, mixed for a flower no
	 * colour lists), earning Flowers for the Dead; a stick does nothing; fresh flowers calm the graves within reach,
	 * wilted ones and those too far do not; enough random ticks wilt them; shears clear them.
	 */
	@GameTest
	public void graveVaseFlowersCalmTheGraves(GameTestHelper helper) {
		floor(helper);
		BlockPos vase = new BlockPos(2, 2, 2);
		helper.setBlock(vase, block(JugcraftAgriculture.GRAVE_VASE));
		ServerPlayer mourner = player(helper, new BlockPos(2, 2, 0), new ItemStack(Items.STICK));
		use(helper, mourner, vase, Direction.UP);
		helper.assertTrue(helper.getBlockState(vase).getValue(GraveVaseBlock.FLOWERS) == GraveVaseBlock.Bouquet.NONE, "A stick is no flower");
		mourner.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.POPPY, 2));
		use(helper, mourner, vase, Direction.UP);
		BlockState state = helper.getBlockState(vase);
		helper.assertTrue(state.getValue(GraveVaseBlock.FLOWERS) == GraveVaseBlock.Bouquet.RED && !state.getValue(GraveVaseBlock.WILTED),
				"A poppy makes fresh red flowers");
		helper.assertTrue(mourner.getMainHandItem().getCount() == 1, "One poppy goes in");
		helper.assertTrue(earned(mourner, "flowers_for_the_dead"), "Flowers for the Dead is earned");
		mourner.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.PINK_TULIP));
		use(helper, mourner, vase, Direction.UP);
		helper.assertTrue(helper.getBlockState(vase).getValue(GraveVaseBlock.FLOWERS) == GraveVaseBlock.Bouquet.MIXED, "A pink tulip makes a mixed bouquet");

		ServerLevel level = helper.getLevel();
		helper.assertTrue(GraveVaseBlock.calms(level, helper.absolutePos(new BlockPos(5, 2, 4))), "Fresh flowers calm a grave three blocks off");
		helper.assertTrue(!GraveVaseBlock.calms(level, helper.absolutePos(new BlockPos(6, 2, 6))), "but not one four blocks off");
		for (int tick = 0; tick < 400 && !helper.getBlockState(vase).getValue(GraveVaseBlock.WILTED); tick++) {
			helper.getBlockState(vase).randomTick(level, helper.absolutePos(vase), level.getRandom());
		}
		helper.assertTrue(helper.getBlockState(vase).getValue(GraveVaseBlock.WILTED), "In time the flowers wilt");
		helper.assertTrue(!GraveVaseBlock.calms(level, helper.absolutePos(new BlockPos(3, 2, 3))), "Wilted flowers calm nothing");
		mourner.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.SHEARS));
		use(helper, mourner, vase, Direction.UP);
		helper.assertTrue(helper.getBlockState(vase).getValue(GraveVaseBlock.FLOWERS) == GraveVaseBlock.Bouquet.NONE, "Shears clear the vase");
		helper.succeed();
	}

	/**
	 * The lamp post stands three blocks tall from its item, its lantern lit (light 15) while it is dark outside and
	 * out in daylight, checked by its own scheduled tick; breaking its lantern breaks it all, dropping one.
	 */
	@GameTest(maxTicks = 300)
	public void lampPostLightsByNight(GameTestHelper helper) {
		floor(helper);
		ServerLevel level = helper.getLevel();
		ServerPlayer lamplighter = player(helper, new BlockPos(3, 2, 0), new ItemStack(item(JugcraftAgriculture.LAMP_POST)));
		use(helper, lamplighter, new BlockPos(3, 1, 2), Direction.UP);
		for (int part = 0; part <= 2; part++) {
			BlockState state = helper.getBlockState(new BlockPos(3, 2 + part, 2));
			helper.assertTrue(state.is(block(JugcraftAgriculture.LAMP_POST)) && state.getValue(LampPostBlock.PART) == part, "Post part " + part);
		}
		BlockPos lantern = new BlockPos(3, 4, 2);
		boolean dark = !level.isBrightOutside();
		helper.assertTrue(helper.getBlockState(lantern).getValue(LampPostBlock.LIT) == dark, "Placed lit only if it is dark");
		helper.assertTrue(helper.getBlockState(lantern).getLightEmission() == (dark ? LampPostBlock.LIGHT : 0), "Its lantern gives light when lit");
		// Turned the wrong way, its own check sets it right.
		BlockState wrong = helper.getBlockState(lantern).setValue(LampPostBlock.LIT, !dark);
		level.setBlock(helper.absolutePos(lantern), wrong, Block.UPDATE_ALL);
		helper.getBlockState(lantern).tick(level, helper.absolutePos(lantern), level.getRandom());
		helper.assertTrue(helper.getBlockState(lantern).getValue(LampPostBlock.LIT) == dark && helper.getBlockState(new BlockPos(3, 2, 2))
				.getValue(LampPostBlock.LIT) == dark, "Its check lights or puts out the whole post");
		level.destroyBlock(helper.absolutePos(lantern), true);
		helper.assertBlockNotPresent(block(JugcraftAgriculture.LAMP_POST), new BlockPos(3, 2, 2));
		helper.assertBlockNotPresent(block(JugcraftAgriculture.LAMP_POST), new BlockPos(3, 3, 2));
		helper.succeed();
	}

	/** An open grave stirs spirits twice as often as another grave of its stage. */
	@GameTest
	public void openGravesStirMore(GameTestHelper helper) {
		HeadstoneBlock open = (HeadstoneBlock) block("open_grave");
		HeadstoneBlock kerbed = (HeadstoneBlock) block("kerbed_grave");
		helper.assertTrue(open.layout().stir() == HeadstoneBlock.OPEN_GRAVE_STIR && kerbed.layout().stir() == 1.0F, "Open graves stir twice as often");
		helper.succeed();
	}

	/** Every grounds recipe and loot table loads, the bouquets' tags and the advancement. */
	@GameTest
	public void groundsDataLoads(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		for (String id : List.of("kerbed_grave", "planted_grave", "memorial_bench", "open_grave", JugcraftAgriculture.GRAVE_VASE,
				JugcraftAgriculture.LAMP_POST)) {
			helper.assertTrue(level.recipeAccess().byKey(ResourceKey.create(Registries.RECIPE, Jugcraft.id(id))).isPresent(), id + " has a recipe");
			helper.assertTrue(level.getServer().reloadableRegistries().getLootTable(ResourceKey.create(Registries.LOOT_TABLE,
					Jugcraft.id("blocks/" + id))) != LootTable.EMPTY, id + "'s loot loads");
		}
		helper.assertTrue(level.getServer().getAdvancements().get(Jugcraft.id("flowers_for_the_dead")) != null, "Flowers for the Dead loads");
		helper.succeed();
	}
}
