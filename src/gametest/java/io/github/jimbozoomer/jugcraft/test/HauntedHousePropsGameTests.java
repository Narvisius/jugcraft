package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.agriculture.FlyingEyeballBlock;
import io.github.jimbozoomer.jugcraft.agriculture.JugcraftAgriculture;
import io.github.jimbozoomer.jugcraft.agriculture.Midway;
import io.github.jimbozoomer.jugcraft.agriculture.MonsterHeadBlock;
import io.github.jimbozoomer.jugcraft.agriculture.PillarCandleBlock;
import io.github.jimbozoomer.jugcraft.agriculture.PlushBlock;
import java.util.List;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
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
import net.minecraft.world.level.block.CandleBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * In-game tests for Halloween decorations batch 16, the haunted house's props: the flying eyeball hovers without
 * anything holding it or anything colliding with it, and has the block entity its renderer draws; pillar candles cluster
 * up to four, light from flint and steel with a flame over each wick, and give back each candle; a spider web covers the
 * faces it is placed on and gives one for each; the monster's head wakes and glows while powered; the harvest plushes are
 * midway prizes; and the data loads.
 */
public class HauntedHousePropsGameTests {
	private static Block block(String id) {
		return JugcraftAgriculture.block(id);
	}

	private static Item item(String id) {
		return JugcraftAgriculture.item(id);
	}

	private static ServerPlayer player(GameTestHelper helper, BlockPos standAt, ItemStack held) {
		ServerPlayer player = helper.makeMockServerPlayerInLevel();
		player.setGameMode(GameType.SURVIVAL);
		BlockPos absolute = helper.absolutePos(standAt);
		player.setPos(absolute.getX() + 0.5, absolute.getY(), absolute.getZ() + 0.5);
		player.setItemInHand(InteractionHand.MAIN_HAND, held);
		return player;
	}

	private static InteractionResult use(GameTestHelper helper, ServerPlayer player, BlockPos on, Direction face) {
		BlockPos absolute = helper.absolutePos(on);
		BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(absolute).relative(face, 0.5), face, absolute, false);
		return player.gameMode.useItemOn(player, helper.getLevel(), player.getMainHandItem(), InteractionHand.MAIN_HAND, hit);
	}

	private static void floor(GameTestHelper helper) {
		for (int x = 0; x < 8; x++) {
			for (int z = 0; z < 8; z++) {
				helper.setBlock(new BlockPos(x, 1, z), Blocks.STONE);
			}
		}
	}

	private static int count(List<ItemStack> drops, Item item) {
		return drops.stream().filter(s -> s.is(item)).mapToInt(ItemStack::getCount).sum();
	}

	/** The eyeball hovers: nothing collides with it, it keeps no support, it has its block entity, and it bobs within bounds. */
	@GameTest
	public void flyingEyeballsHover(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		floor(helper);
		ServerPlayer keeper = player(helper, new BlockPos(3, 2, 0), new ItemStack(item("flying_eyeball")));
		use(helper, keeper, new BlockPos(3, 1, 3), Direction.UP);
		BlockPos at = new BlockPos(3, 2, 3);
		BlockState eyeball = helper.getBlockState(at);
		helper.assertTrue(eyeball.is(block("flying_eyeball")), "A flying eyeball is placed");
		helper.assertTrue(eyeball.getCollisionShape(level, helper.absolutePos(at)).isEmpty(), "Nothing collides with it");
		helper.assertTrue(level.getBlockEntity(helper.absolutePos(at)) != null
				&& level.getBlockEntity(helper.absolutePos(at)).getType() == JugcraftAgriculture.FLYING_EYEBALL_ENTITY, "It has the block entity it is drawn by");
		helper.setBlock(new BlockPos(3, 1, 3), Blocks.AIR);
		helper.assertTrue(helper.getBlockState(at).is(block("flying_eyeball")), "It stays without anything under it");
		for (int t = 0; t < FlyingEyeballBlock.BOB_TICKS; t += 5) {
			float bob = FlyingEyeballBlock.bob(helper.absolutePos(at), t);
			helper.assertTrue(Math.abs(bob) <= FlyingEyeballBlock.HOVER_PIXELS + 1e-4, "It bobs no further than it should: " + bob);
		}
		helper.assertTrue(count(Block.getDrops(eyeball, level, helper.absolutePos(at), null), item("flying_eyeball")) == 1, "It drops itself");
		helper.succeed();
	}

	/** Pillar candles cluster to four, light from flint and steel with a flame over each wick, and drop each candle. */
	@GameTest
	public void pillarCandlesClusterAndLight(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		floor(helper);
		BlockPos at = new BlockPos(3, 2, 3);
		ServerPlayer chandler = player(helper, new BlockPos(3, 2, 0), new ItemStack(item("ivory_pillar_candle"), 6));
		use(helper, chandler, new BlockPos(3, 1, 3), Direction.UP);
		for (int i = 0; i < 5; i++) {
			use(helper, chandler, at, Direction.UP);
		}
		BlockState candles = helper.getBlockState(at);
		helper.assertTrue(candles.is(block("ivory_pillar_candle")) && candles.getValue(CandleBlock.CANDLES) == 4, "They cluster to four and no more");
		helper.assertTrue(chandler.getMainHandItem().getCount() == 2, "Only four were used");
		chandler.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.FLINT_AND_STEEL));
		use(helper, chandler, at, Direction.UP);
		candles = helper.getBlockState(at);
		helper.assertTrue(candles.getValue(CandleBlock.LIT), "Flint and steel lights them");
		helper.assertTrue(candles.getLightEmission() == 3 * 4, "Four lit candles give light 12: " + candles.getLightEmission());
		for (int n = 1; n <= 4; n++) {
			List<Vec3> flames = PillarCandleBlock.flames(n);
			helper.assertTrue(flames.size() == n, n + " candles have " + n + " flames");
			for (int i = 0; i < n; i++) {
				double top = PillarCandleBlock.LAYOUT[n - 1][i][3] / 16;
				helper.assertTrue(flames.get(i).y > top && flames.get(i).y <= 1.0, "Each flame stands over its wick, within the block: " + flames.get(i));
			}
		}
		helper.assertTrue(count(Block.getDrops(candles, level, helper.absolutePos(at), null), item("ivory_pillar_candle")) == 4, "They give back four");
		helper.setBlock(new BlockPos(5, 2, 5), block("black_pillar_candle"));
		helper.assertTrue(helper.getBlockState(new BlockPos(5, 2, 5)).getLightEmission() == 0, "Unlit, a black candle gives no light");
		helper.succeed();
	}

	/** A spider web covers the faces it is placed on, gives one for each, and nothing collides with it. */
	@GameTest
	public void spiderWebsCoverFaces(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		floor(helper);
		helper.setBlock(new BlockPos(3, 2, 4), Blocks.STONE);
		helper.setBlock(new BlockPos(3, 3, 3), Blocks.STONE);
		ServerPlayer spinner = player(helper, new BlockPos(3, 2, 0), new ItemStack(item("spider_web"), 2));
		use(helper, spinner, new BlockPos(3, 2, 4), Direction.NORTH);
		BlockPos at = new BlockPos(3, 2, 3);
		BlockState web = helper.getBlockState(at);
		helper.assertTrue(web.is(block("spider_web")) && web.getValue(BlockStateProperties.SOUTH), "It covers the wall it was hung on: " + web);
		use(helper, spinner, new BlockPos(3, 3, 3), Direction.DOWN);
		web = helper.getBlockState(at);
		helper.assertTrue(web.getValue(BlockStateProperties.SOUTH) && web.getValue(BlockStateProperties.UP), "A second web covers the ceiling too: " + web);
		helper.assertTrue(web.getCollisionShape(level, helper.absolutePos(at)).isEmpty(), "Nothing collides with it");
		helper.assertTrue(count(Block.getDrops(web, level, helper.absolutePos(at), null), item("spider_web")) == 2, "It gives one for each face");
		helper.succeed();
	}

	/** The monster's head faces its builder; powered it wakes and glows, and sleeps again when the signal goes. */
	@GameTest
	public void monsterHeadsWake(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		floor(helper);
		ServerPlayer builder = player(helper, new BlockPos(3, 2, 0), new ItemStack(item("monster_head")));
		use(helper, builder, new BlockPos(3, 1, 3), Direction.UP);
		BlockPos at = new BlockPos(3, 2, 3);
		BlockState head = helper.getBlockState(at);
		helper.assertTrue(head.is(block("monster_head")) && head.getValue(MonsterHeadBlock.FACING) == builder.getDirection().getOpposite(),
				"It faces its builder: " + head);
		helper.assertTrue(!head.getValue(MonsterHeadBlock.POWERED) && head.getLightEmission() == 0, "It starts asleep and dark");
		helper.setBlock(at.east(), Blocks.REDSTONE_BLOCK);
		head = helper.getBlockState(at);
		helper.assertTrue(head.getValue(MonsterHeadBlock.POWERED) && head.getLightEmission() == MonsterHeadBlock.LIGHT, "Powered, it wakes and its eyes glow");
		helper.setBlock(at.east(), Blocks.AIR);
		head = helper.getBlockState(at);
		helper.assertTrue(!head.getValue(MonsterHeadBlock.POWERED) && head.getLightEmission() == 0, "Unpowered, it sleeps");
		helper.assertTrue(count(Block.getDrops(head, level, helper.absolutePos(at), null), item("monster_head")) == 1, "It drops itself");
		helper.succeed();
	}

	/** The five harvest plushes are midway prizes: plush blocks in the prize list. */
	@GameTest
	public void harvestPlushesArePrizes(GameTestHelper helper) {
		List<String> prizes = Midway.PLUSHES.stream().map(Midway.Plush::id).toList();
		for (String id : List.of("owl_plush", "hedgehog_plush", "acorn_plush", "corn_plush", "maple_leaf_plush")) {
			helper.assertTrue(prizes.contains(id), id + " is a midway prize");
			helper.assertTrue(block(id) instanceof PlushBlock, id + " is a plush");
		}
		helper.succeed();
	}

	/** Recipes and loot tables load. */
	@GameTest
	public void propDataLoads(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		for (String id : List.of("flying_eyeball", "ivory_pillar_candle", "black_pillar_candle", "spider_web", "monster_head")) {
			helper.assertTrue(level.recipeAccess().byKey(ResourceKey.create(Registries.RECIPE, Jugcraft.id(id))).isPresent(), id + " has a recipe");
			helper.assertTrue(level.getServer().reloadableRegistries().getLootTable(ResourceKey.create(Registries.LOOT_TABLE,
					Jugcraft.id("blocks/" + id))) != LootTable.EMPTY, id + "'s loot loads");
		}
		helper.succeed();
	}
}
