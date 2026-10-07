package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.agriculture.BlackLightBlock;
import io.github.jimbozoomer.jugcraft.agriculture.DecorationBlockEntity;
import io.github.jimbozoomer.jugcraft.agriculture.FloatingWitchHatBlock;
import io.github.jimbozoomer.jugcraft.agriculture.GlowPaintBlock;
import io.github.jimbozoomer.jugcraft.agriculture.JugcraftAgriculture;
import io.github.jimbozoomer.jugcraft.agriculture.MiniPumpkinStackBlock;
import io.github.jimbozoomer.jugcraft.agriculture.ShadowPuppetLampBlock;
import io.github.jimbozoomer.jugcraft.agriculture.WitchFireBrazierBlock;
import java.util.List;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * In-game tests for lighting and glow: the Black Light (on a wall, by hand and by redstone, how far its glow reaches),
 * Glow Paint (on walls, floors and ceilings, its designs), the Witch Fire Brazier (dyed flames, put out and lit again),
 * the Shadow Puppet Lamp, the Mini Pumpkin Stack and the Floating Witch Hat (lit and snuffed like candles, turning and
 * bobbing), and that their data loads.
 */
public class Decor10GameTests {
	private static Item item(String id) {
		return JugcraftAgriculture.item(id);
	}

	private static Block block(String id) {
		return JugcraftAgriculture.block(id);
	}

	private static Item vanilla(String id) {
		return BuiltInRegistries.ITEM.getValue(Identifier.fromNamespaceAndPath("minecraft", id));
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

	private static BlockHitResult hit(GameTestHelper helper, BlockPos pos, Direction side) {
		BlockPos absolute = helper.absolutePos(pos);
		return new BlockHitResult(Vec3.atCenterOf(absolute).relative(side, 0.5), side, absolute, false);
	}

	private static InteractionResult use(GameTestHelper helper, ServerPlayer player, BlockPos pos, Direction side) {
		return player.gameMode.useItemOn(player, helper.getLevel(), player.getMainHandItem(), InteractionHand.MAIN_HAND, hit(helper, pos, side));
	}

	private static InteractionResult place(GameTestHelper helper, ServerPlayer player, BlockPos pos, Direction side) {
		return player.getMainHandItem().useOn(new UseOnContext(player, InteractionHand.MAIN_HAND, hit(helper, pos, side)));
	}

	private static int dropped(GameTestHelper helper, Item item) {
		AABB area = new AABB(helper.absolutePos(BlockPos.ZERO)).expandTowards(8, 8, 8).inflate(2.0);
		return helper.getLevel().getEntitiesOfClass(ItemEntity.class, area, entity -> entity.getItem().is(item)).stream()
				.mapToInt(entity -> entity.getItem().getCount()).sum();
	}

	private static int light(GameTestHelper helper, BlockPos pos) {
		return helper.getBlockState(pos).getLightEmission();
	}

	// ---------------------------------------------------------------- the black light

	/**
	 * A black light hangs on the wall it was placed against, facing out, off; used, it shines (light 6), and used again
	 * goes off; a redstone signal turns it on while it lasts; its glow is full within half its range and gone at its
	 * edge; without its wall it drops.
	 */
	@GameTest(maxTicks = 40)
	public void blackLightsShine(GameTestHelper helper) {
		floor(helper);
		BlockPos wall = new BlockPos(3, 2, 4);
		BlockPos pos = new BlockPos(3, 2, 3);
		helper.setBlock(wall, Blocks.STONE);
		ServerPlayer player = player(helper, new BlockPos(3, 2, 0), 0.0F, new ItemStack(item("black_light")));
		place(helper, player, wall, Direction.NORTH);
		helper.assertTrue(helper.getBlockState(pos).is(block("black_light")) && helper.getBlockState(pos).getValue(BlackLightBlock.FACING) == Direction.NORTH
				&& !BlackLightBlock.shining(helper.getBlockState(pos)), "It hangs on its wall facing out, off");
		player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
		use(helper, player, pos, Direction.NORTH);
		helper.assertTrue(light(helper, pos) == BlackLightBlock.LIGHT, "Used, it shines");
		use(helper, player, pos, Direction.NORTH);
		helper.assertTrue(light(helper, pos) == 0, "Used again, it goes off");
		helper.setBlock(pos.west(), Blocks.REDSTONE_BLOCK);
		helper.assertTrue(helper.getBlockState(pos).getValue(BlackLightBlock.POWERED) && light(helper, pos) == BlackLightBlock.LIGHT,
				"A redstone signal turns it on");
		helper.setBlock(pos.west(), Blocks.AIR);
		helper.assertTrue(light(helper, pos) == 0, "and it goes off when the signal does");
		helper.assertTrue(BlackLightBlock.glow(0.0) == 1.0F && BlackLightBlock.glow(BlackLightBlock.RANGE / 2) == 1.0F
				&& Math.abs(BlackLightBlock.glow(BlackLightBlock.RANGE * 0.75) - 0.5F) < 1e-6 && BlackLightBlock.glow(BlackLightBlock.RANGE) == 0.0F
				&& BlackLightBlock.glow(BlackLightBlock.RANGE + 3) == 0.0F, "Its glow is full within half its range and gone at its edge");
		helper.setBlock(wall, Blocks.AIR);
		helper.runAfterDelay(3, () -> {
			helper.assertBlockNotPresent(block("black_light"), pos);
			helper.assertTrue(dropped(helper, item("black_light")) == 1, "Without its wall it drops once");
			helper.succeed();
		});
	}

	// ---------------------------------------------------------------- glow paint

	/**
	 * Glow paint goes on a wall, the floor and a ceiling, facing out from each, a skull first; used, it is painted over
	 * with the next design; it glows a little by itself; it needs a face behind it, and goes with it.
	 */
	@GameTest(maxTicks = 40)
	public void glowPaintGoesOnAnyFace(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		floor(helper);
		BlockPos wall = new BlockPos(5, 2, 5);
		BlockPos ceiling = new BlockPos(1, 4, 6);
		helper.setBlock(wall, Blocks.STONE);
		helper.setBlock(ceiling, Blocks.STONE);
		ServerPlayer player = player(helper, new BlockPos(5, 2, 1), 0.0F, new ItemStack(item("glow_paint"), 8));
		place(helper, player, wall, Direction.NORTH);
		place(helper, player, new BlockPos(2, 1, 2), Direction.UP);
		place(helper, player, ceiling, Direction.DOWN);
		BlockPos onWall = wall.north();
		helper.assertTrue(helper.getBlockState(onWall).getValue(GlowPaintBlock.FACING) == Direction.NORTH
				&& helper.getBlockState(new BlockPos(2, 2, 2)).getValue(GlowPaintBlock.FACING) == Direction.UP
				&& helper.getBlockState(ceiling.below()).getValue(GlowPaintBlock.FACING) == Direction.DOWN, "It goes on a wall, the floor and a ceiling");
		helper.assertTrue(helper.getBlockState(onWall).getValue(GlowPaintBlock.DESIGN) == GlowPaintBlock.Design.SKULL && light(helper, onWall) == 1,
				"A skull first, glowing a little by itself");
		helper.assertTrue(helper.getBlockEntity(onWall, DecorationBlockEntity.class) != null, "Its glow is drawn from its block entity");
		player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
		for (GlowPaintBlock.Design next : new GlowPaintBlock.Design[] {GlowPaintBlock.Design.BAT, GlowPaintBlock.Design.SPIDER, GlowPaintBlock.Design.WEB,
				GlowPaintBlock.Design.HAND, GlowPaintBlock.Design.EYE, GlowPaintBlock.Design.SKULL}) {
			use(helper, player, onWall, Direction.NORTH);
			helper.assertTrue(helper.getBlockState(onWall).getValue(GlowPaintBlock.DESIGN) == next, "Used, it is painted over with a " + next);
		}
		helper.assertFalse(block("glow_paint").defaultBlockState().setValue(GlowPaintBlock.FACING, Direction.NORTH)
				.canSurvive(level, helper.absolutePos(new BlockPos(6, 4, 2))), "It needs a face behind it");
		helper.setBlock(wall, Blocks.AIR);
		helper.runAfterDelay(3, () -> {
			helper.assertBlockNotPresent(block("glow_paint"), onWall);
			helper.assertTrue(dropped(helper, item("glow_paint")) == 1, "and goes with it, dropping once");
			helper.succeed();
		});
	}

	// ---------------------------------------------------------------- the witch fire brazier

	/**
	 * A brazier is placed burning orange (light 15); a green dye turns its flame green and is used up, the same dye
	 * again does nothing; a shovel puts it out; flint and steel lights it again and is worn; broken, it drops once.
	 */
	@GameTest(maxTicks = 40)
	public void witchFireTakesDye(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		floor(helper);
		BlockPos pos = new BlockPos(3, 2, 3);
		ServerPlayer player = player(helper, new BlockPos(3, 2, 6), 180.0F, new ItemStack(item("witch_fire_brazier")));
		place(helper, player, pos.below(), Direction.UP);
		helper.assertTrue(helper.getBlockState(pos).getValue(WitchFireBrazierBlock.FLAME) == WitchFireBrazierBlock.Flame.ORANGE
				&& light(helper, pos) == WitchFireBrazierBlock.LIGHT, "Placed, it burns orange");
		helper.assertTrue(helper.getBlockEntity(pos, DecorationBlockEntity.class) != null, "Its flames are drawn from its block entity");
		player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(vanilla("green_dye"), 2));
		use(helper, player, pos, Direction.SOUTH);
		helper.assertTrue(helper.getBlockState(pos).getValue(WitchFireBrazierBlock.FLAME) == WitchFireBrazierBlock.Flame.GREEN
				&& player.getMainHandItem().getCount() == 1, "A green dye turns it green and is used up");
		use(helper, player, pos, Direction.SOUTH);
		helper.assertTrue(player.getMainHandItem().getCount() == 1, "The same dye again does nothing");
		player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(vanilla("iron_shovel")));
		use(helper, player, pos, Direction.SOUTH);
		helper.assertTrue(!helper.getBlockState(pos).getValue(WitchFireBrazierBlock.LIT) && light(helper, pos) == 0, "A shovel puts it out");
		player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.FLINT_AND_STEEL));
		use(helper, player, pos, Direction.SOUTH);
		helper.assertTrue(helper.getBlockState(pos).getValue(WitchFireBrazierBlock.LIT) && player.getMainHandItem().getDamageValue() == 1
				&& helper.getBlockState(pos).getValue(WitchFireBrazierBlock.FLAME) == WitchFireBrazierBlock.Flame.GREEN,
				"Flint and steel lights it again, still green, and is worn");
		level.destroyBlock(helper.absolutePos(pos), true);
		helper.runAfterDelay(3, () -> {
			helper.assertTrue(dropped(helper, item("witch_fire_brazier")) == 1, "Broken, it drops once");
			helper.succeed();
		});
	}

	// ---------------------------------------------------------------- candle-lit things

	/**
	 * The shadow puppet lamp and the floating witch hat are placed out, the mini pumpkin stack lit; flint and steel or a
	 * fire charge lights each (lamp 12, pumpkins 12, hat 10) and an empty hand snuffs it; the lamp's shade goes round once
	 * in its time; the hat bobs no more than its bob.
	 */
	@GameTest(maxTicks = 40)
	public void candleLitThingsLightAndSnuff(GameTestHelper helper) {
		floor(helper);
		BlockPos lamp = new BlockPos(1, 2, 3);
		BlockPos pumpkins = new BlockPos(3, 2, 3);
		BlockPos hat = new BlockPos(5, 2, 3);
		ServerPlayer player = player(helper, new BlockPos(3, 2, 6), 180.0F, new ItemStack(item("shadow_puppet_lamp")));
		place(helper, player, lamp.below(), Direction.UP);
		player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(item("mini_pumpkin_stack")));
		place(helper, player, pumpkins.below(), Direction.UP);
		player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(item("floating_witch_hat")));
		place(helper, player, hat.below(), Direction.UP);
		helper.assertTrue(light(helper, lamp) == 0 && light(helper, hat) == 0 && light(helper, pumpkins) == MiniPumpkinStackBlock.LIGHT
				&& helper.getBlockState(pumpkins).getValue(MiniPumpkinStackBlock.FACING) == Direction.SOUTH, "Placed: the lamp and hat out, the pumpkins lit");
		player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.FLINT_AND_STEEL));
		use(helper, player, lamp, Direction.SOUTH);
		player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.FIRE_CHARGE, 2));
		use(helper, player, hat, Direction.SOUTH);
		helper.assertTrue(light(helper, lamp) == ShadowPuppetLampBlock.LIGHT && light(helper, hat) == FloatingWitchHatBlock.LIGHT
				&& player.getMainHandItem().getCount() == 1, "Flint and steel and a fire charge light them");
		player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
		for (BlockPos pos : List.of(lamp, pumpkins, hat)) {
			use(helper, player, pos, Direction.SOUTH);
			helper.assertTrue(light(helper, pos) == 0, "An empty hand snuffs it");
		}
		BlockPos at = helper.absolutePos(lamp);
		for (int t = 0; t < 2000; t += 37) {
			float turn = ShadowPuppetLampBlock.turn(at, t);
			float round = ((ShadowPuppetLampBlock.turn(at, t + ShadowPuppetLampBlock.TURN_TICKS) - turn) % 360.0F + 360.0F) % 360.0F;
			helper.assertTrue(turn >= 0.0F && turn < 360.0F && (round < 0.01F || round > 359.99F), "The shade goes round once in its time");
			helper.assertTrue(Math.abs(FloatingWitchHatBlock.bob(helper.absolutePos(hat), t)) <= FloatingWitchHatBlock.BOB, "The hat bobs no more than its bob");
		}
		helper.succeed();
	}

	/** The recipes and loot tables load. */
	@GameTest
	public void lightingDataLoads(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		for (String id : List.of("black_light", "glow_paint", "witch_fire_brazier", "shadow_puppet_lamp", "mini_pumpkin_stack", "floating_witch_hat")) {
			helper.assertTrue(level.recipeAccess().byKey(ResourceKey.create(Registries.RECIPE, Jugcraft.id(id))).isPresent(), "Recipe " + id + " loads");
			LootTable table = level.getServer().reloadableRegistries().getLootTable(ResourceKey.create(Registries.LOOT_TABLE, Jugcraft.id("blocks/" + id)));
			helper.assertTrue(table != LootTable.EMPTY, "The " + id + " drops itself");
		}
		helper.succeed();
	}
}
