package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.agriculture.AuraCandleBlock;
import io.github.jimbozoomer.jugcraft.agriculture.AuraCandleBlockEntity;
import io.github.jimbozoomer.jugcraft.agriculture.AuraCandleItem;
import io.github.jimbozoomer.jugcraft.agriculture.CandleMix;
import io.github.jimbozoomer.jugcraft.agriculture.CandleScent;
import io.github.jimbozoomer.jugcraft.agriculture.CandleWax;
import io.github.jimbozoomer.jugcraft.agriculture.JugcraftAgriculture;
import io.github.jimbozoomer.jugcraft.agriculture.WaxPotBlockEntity;
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
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * In-game tests for the chandlery: wax melting over heat and setting without it, what a pot's molten wax takes (dyes
 * mixed, two scents at most, a brightener and an extender), dipping (string starts a candle, a warm candle loses its
 * layer, a cool one gains one, a third scent muddles it), the aura (effects within the radius only, stronger when
 * bright; warding and revealing on mobs; harvest growing crops), lighting, burning out, dropping part-burned, and data.
 */
public class ChandleryGameTests {
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

	private static ServerPlayer player(GameTestHelper helper, BlockPos standAt, ItemStack held) {
		ServerPlayer player = helper.makeMockServerPlayerInLevel();
		player.setGameMode(GameType.SURVIVAL);
		BlockPos absolute = helper.absolutePos(standAt);
		player.setPos(absolute.getX() + 0.5, absolute.getY(), absolute.getZ() + 0.5);
		player.setItemInHand(InteractionHand.MAIN_HAND, held);
		return player;
	}

	private static InteractionResult use(GameTestHelper helper, ServerPlayer player, BlockPos pos) {
		BlockPos absolute = helper.absolutePos(pos);
		BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(absolute).relative(Direction.UP, 0.5), Direction.UP, absolute, false);
		return player.gameMode.useItemOn(player, helper.getLevel(), player.getMainHandItem(), InteractionHand.MAIN_HAND, hit);
	}

	/** A pot at {@code pos} with {@code measures} of molten beeswax (honeycomb put in, then melted at once). */
	private static WaxPotBlockEntity moltenPot(GameTestHelper helper, BlockPos pos, int measures) {
		helper.setBlock(pos, block("wax_melting_pot"));
		WaxPotBlockEntity pot = helper.getBlockEntity(pos, WaxPotBlockEntity.class);
		ServerPlayer filler = player(helper, pos.south(2), new ItemStack(Items.HONEYCOMB, measures / CandleWax.BEESWAX.measures));
		for (int i = 0; i < measures / CandleWax.BEESWAX.measures; i++) {
			use(helper, filler, pos);
		}
		pot.meltAll();
		return pot;
	}

	private static AuraCandleBlockEntity candle(GameTestHelper helper, BlockPos pos, CandleMix mix, boolean lit) {
		helper.setBlock(pos, block("aura_candle").defaultBlockState().setValue(AuraCandleBlock.DIPS, mix.dips()).setValue(AuraCandleBlock.LIT, lit));
		AuraCandleBlockEntity candle = helper.getBlockEntity(pos, AuraCandleBlockEntity.class);
		candle.setMix(mix);
		return candle;
	}

	private static CandleMix scented(int dips, boolean bright, CandleScent... scents) {
		CandleMix mix = CandleMix.first(CandleWax.BEESWAX, 0xAA55FF, List.of(scents), bright, false);
		for (int i = 1; i < dips; i++) {
			mix = mix.dip(CandleWax.BEESWAX, 0xAA55FF, List.of(scents), bright, false);
		}
		return mix;
	}

	// ---------------------------------------------------------------- melting

	/** Honeycomb melts over a campfire and not on stone; a pot of beeswax takes no tallow. */
	@GameTest(maxTicks = 400)
	public void waxMeltsOverHeat(GameTestHelper helper) {
		floor(helper);
		BlockPos hot = new BlockPos(2, 3, 2);
		BlockPos cold = new BlockPos(5, 2, 5);
		helper.setBlock(hot.below(), Blocks.CAMPFIRE);
		helper.setBlock(hot, block("wax_melting_pot"));
		helper.setBlock(cold, block("wax_melting_pot"));
		ServerPlayer player = player(helper, new BlockPos(4, 2, 2), new ItemStack(Items.HONEYCOMB, 2));
		use(helper, player, hot);
		use(helper, player, cold);
		player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.ROTTEN_FLESH));
		use(helper, player, hot);
		WaxPotBlockEntity heated = helper.getBlockEntity(hot, WaxPotBlockEntity.class);
		WaxPotBlockEntity unheated = helper.getBlockEntity(cold, WaxPotBlockEntity.class);
		helper.assertTrue(heated.total() == 2 && player.getMainHandItem().getCount() == 1, "Beeswax goes in; tallow doesn't go in with it");
		helper.runAfterDelay(2 * WaxPotBlockEntity.MELT_TICKS + 20, () -> {
			helper.assertTrue(heated.molten() == 2, "Over the campfire the beeswax melted");
			helper.assertTrue(unheated.molten() == 0 && unheated.total() == 2, "On stone it stays set");
			helper.succeed();
		});
	}

	// ---------------------------------------------------------------- the mixture

	/** Molten wax takes dyes (mixed), two scents but no third, a brightener and an extender, each once. */
	@GameTest(maxTicks = 40)
	public void moltenWaxTakesAMixture(GameTestHelper helper) {
		floor(helper);
		BlockPos pos = new BlockPos(2, 2, 2);
		WaxPotBlockEntity pot = moltenPot(helper, pos, 4);
		ServerPlayer player = player(helper, new BlockPos(4, 2, 2), new ItemStack(BuiltInRegistries.ITEM.getValue(Identifier.parse("minecraft:red_dye"))));
		use(helper, player, pos);
		int red = pot.color();
		helper.assertTrue((red >> 16 & 0xFF) > (red & 0xFF), "Red dye makes it red");
		player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(BuiltInRegistries.ITEM.getValue(Identifier.parse("minecraft:blue_dye"))));
		use(helper, player, pos);
		int purple = pot.color();
		helper.assertTrue((purple >> 16 & 0xFF) > 40 && (purple & 0xFF) > 40, "Red and blue mix");
		for (Item scent : List.of(Items.SUGAR, Items.RABBIT_FOOT, Items.FEATHER)) {
			player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(scent));
			use(helper, player, pos);
		}
		helper.assertTrue(pot.scents().equals(List.of(CandleScent.SWIFTNESS, CandleScent.LEAPING)) && player.getMainHandItem().is(Items.FEATHER),
				"Two scents go in, not a third");
		player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.GLOWSTONE_DUST, 2));
		use(helper, player, pos);
		use(helper, player, pos);
		helper.assertTrue(pot.bright() && player.getMainHandItem().getCount() == 1, "Brightened once: the second dust stays in hand");
		player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.REDSTONE, 2));
		use(helper, player, pos);
		use(helper, player, pos);
		helper.assertTrue(pot.lasting() && player.getMainHandItem().getCount() == 1, "Extended once: the second redstone stays in hand");
		helper.succeed();
	}

	// ---------------------------------------------------------------- dipping

	/**
	 * String starts a one-layer candle of the pot's wax and scents; dipping it again while warm loses the layer; a cool
	 * candle gains one, its burn time adding up. Layering a third scent muddles a candle.
	 */
	@GameTest(maxTicks = 40)
	public void dippingBuildsLayers(GameTestHelper helper) {
		floor(helper);
		BlockPos pos = new BlockPos(2, 2, 2);
		WaxPotBlockEntity pot = moltenPot(helper, pos, 8);
		ServerPlayer player = player(helper, new BlockPos(4, 2, 2), new ItemStack(Items.SUGAR));
		use(helper, player, pos);
		player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.STRING));
		use(helper, player, pos);
		ItemStack candle = player.getMainHandItem();
		CandleMix first = candle.get(JugcraftAgriculture.CANDLE_MIX);
		helper.assertTrue(first != null && first.dips() == 1 && first.scents().equals(List.of(CandleScent.SWIFTNESS))
				&& first.burn() == CandleWax.BEESWAX.burnPerDip && pot.molten() == 7, "String starts a scented one-layer candle");
		helper.assertTrue(player.getCooldowns().isOnCooldown(candle), "A fresh candle is warm");
		use(helper, player, pos);
		helper.assertTrue(player.getMainHandItem().get(JugcraftAgriculture.CANDLE_MIX).dips() == 1 && pot.molten() == 6,
				"Dipped while warm, the layer slides off and its wax is lost");
		// Another hand, whose candle isn't warm.
		ServerPlayer cool = player(helper, new BlockPos(2, 2, 4), player.getMainHandItem().copy());
		use(helper, cool, pos);
		CandleMix second = cool.getMainHandItem().get(JugcraftAgriculture.CANDLE_MIX);
		helper.assertTrue(second.dips() == 2 && second.burn() == 2 * CandleWax.BEESWAX.burnPerDip && pot.molten() == 5, "A cool candle gains a layer");

		ServerPlayer muddler = player(helper, new BlockPos(0, 2, 2), AuraCandleItem.make(scented(1, false, CandleScent.MOONLIGHT, CandleScent.EMBER)));
		use(helper, muddler, pos);
		CandleMix muddled = muddler.getMainHandItem().get(JugcraftAgriculture.CANDLE_MIX);
		helper.assertTrue(muddled.muddled() && muddled.scents().size() == 3, "A third scent layered on muddles it");
		helper.succeed();
	}

	// ---------------------------------------------------------------- the aura

	/** A pulse gives players within the radius the scent's effect (level II when bright), and nobody outside it. */
	@GameTest(maxTicks = 40)
	public void aurasReachTheirRadius(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		floor(helper);
		BlockPos pos = new BlockPos(1, 2, 1);
		CandleMix mix = scented(1, false, CandleScent.SWIFTNESS, CandleScent.MOONLIGHT);
		candle(helper, pos, mix, true);
		ServerPlayer near = player(helper, new BlockPos(4, 2, 4), ItemStack.EMPTY);
		ServerPlayer far = player(helper, new BlockPos(7, 2, 7), ItemStack.EMPTY);
		AuraCandleBlockEntity.pulse(level, helper.absolutePos(pos), mix);
		helper.assertTrue(near.hasEffect(MobEffects.SPEED) && near.hasEffect(MobEffects.NIGHT_VISION), "Within five blocks: both effects");
		helper.assertTrue(!far.hasEffect(MobEffects.SPEED), "Six blocks off: none");
		AuraCandleBlockEntity.pulse(level, helper.absolutePos(pos), scented(1, true, CandleScent.SWIFTNESS));
		helper.assertTrue(near.getEffect(MobEffects.SPEED).getAmplifier() == 1, "Bright: Speed II");
		AuraCandleBlockEntity.pulse(level, helper.absolutePos(pos), scented(1, false, CandleScent.SWIFTNESS, CandleScent.LEAPING, CandleScent.TIDE));
		helper.assertTrue(!near.hasEffect(MobEffects.WATER_BREATHING), "A muddled candle has no aura");
		helper.succeed();
	}

	/** Warding slows and weakens hostile mobs; revealing makes creatures glow, but not players. */
	@GameTest(maxTicks = 40)
	public void wardingAndRevealingWorkOnMobs(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		floor(helper);
		BlockPos pos = new BlockPos(1, 2, 1);
		LivingEntity zombie = helper.spawnWithNoFreeWill(EntityTypes.ZOMBIE, new BlockPos(4, 2, 4));
		ServerPlayer player = player(helper, new BlockPos(3, 2, 1), ItemStack.EMPTY);
		AuraCandleBlockEntity.pulse(level, helper.absolutePos(pos), scented(2, false, CandleScent.WARDING, CandleScent.REVEALING));
		helper.assertTrue(zombie.hasEffect(MobEffects.SLOWNESS) && zombie.hasEffect(MobEffects.WEAKNESS), "Warding slows and weakens the zombie");
		helper.assertTrue(zombie.hasEffect(MobEffects.GLOWING) && !player.hasEffect(MobEffects.GLOWING), "Revealing shows the zombie, not the player");
		helper.succeed();
	}

	/** Harvest makes the crops round a candle grow. */
	@GameTest(maxTicks = 100)
	public void harvestGrowsCrops(GameTestHelper helper) {
		BlockPos centre = new BlockPos(4, 2, 4);
		for (int x = 1; x <= 7; x++) {
			for (int z = 1; z <= 7; z++) {
				helper.setBlock(new BlockPos(x, 1, z), Blocks.FARMLAND);
				helper.setBlock(new BlockPos(x, 2, z), Blocks.WHEAT);
			}
		}
		for (BlockPos light : List.of(new BlockPos(1, 2, 1), new BlockPos(7, 2, 7), new BlockPos(1, 2, 7), new BlockPos(7, 2, 1))) {
			helper.setBlock(light, Blocks.GLOWSTONE);
		}
		helper.setBlock(centre.below(), Blocks.STONE);
		CandleMix mix = scented(1, true, CandleScent.HARVEST);
		candle(helper, centre, mix, false);
		helper.runAtTickTime(40, () -> {
			ServerLevel level = helper.getLevel();
			for (int i = 0; i < 400; i++) {
				AuraCandleBlockEntity.pulse(level, helper.absolutePos(centre), mix);
			}
			int grown = 0;
			for (int x = 1; x <= 7; x++) {
				for (int z = 1; z <= 7; z++) {
					BlockState wheat = helper.getBlockState(new BlockPos(x, 2, z));
					grown += wheat.is(Blocks.WHEAT) ? wheat.getValue(CropBlock.AGE) : 0;
				}
			}
			helper.assertTrue(grown > 0, "The wheat round a harvest candle grew");
			helper.succeed();
		});
	}

	// ---------------------------------------------------------------- burning

	/** Flint and steel lights a candle; lit, it burns down and goes out for good; broken part-burned, it drops as it is. */
	@GameTest(maxTicks = 100)
	public void candlesBurnDown(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		floor(helper);
		BlockPos short_ = new BlockPos(2, 2, 2);
		BlockPos kept = new BlockPos(5, 2, 5);
		AuraCandleBlockEntity stub = candle(helper, short_, CandleMix.plain(CandleWax.TALLOW).withBurned(CandleWax.TALLOW.burnPerDip - 30), false);
		ServerPlayer player = player(helper, new BlockPos(2, 2, 4), new ItemStack(Items.FLINT_AND_STEEL));
		use(helper, player, short_);
		helper.assertTrue(helper.getBlockState(short_).getValue(AuraCandleBlock.LIT) && helper.getBlockState(short_).getLightEmission() == 8,
				"Flint and steel lights it (light 8 for one layer)");
		CandleMix mix = scented(3, false, CandleScent.EMBER).withBurned(500);
		candle(helper, kept, mix, false);
		level.destroyBlock(helper.absolutePos(kept), true);
		AABB area = new AABB(helper.absolutePos(BlockPos.ZERO)).expandTowards(8, 8, 8).inflate(2.0);
		List<ItemEntity> drops = level.getEntitiesOfClass(ItemEntity.class, area, entity -> entity.getItem().has(JugcraftAgriculture.CANDLE_MIX));
		helper.assertTrue(drops.size() == 1 && drops.get(0).getItem().get(JugcraftAgriculture.CANDLE_MIX).equals(mix),
				"Broken, it drops itself with its scents and burn");
		helper.runAfterDelay(60, () -> {
			helper.assertTrue(helper.getBlockState(short_).isAir(), "Burned down, it went out for good");
			helper.assertTrue(stub.isRemoved(), "Its block entity went with it");
			helper.succeed();
		});
	}

	// ---------------------------------------------------------------- data

	/** The pot's recipe and both loot tables load. */
	@GameTest
	public void chandleryDataLoads(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		helper.assertTrue(level.recipeAccess().byKey(ResourceKey.create(Registries.RECIPE, Jugcraft.id("wax_melting_pot"))).isPresent(),
				"The pot's recipe loads");
		for (String table : List.of("blocks/wax_melting_pot", "blocks/aura_candle")) {
			helper.assertTrue(level.getServer().reloadableRegistries().getLootTable(ResourceKey.create(Registries.LOOT_TABLE, Jugcraft.id(table)))
					!= LootTable.EMPTY, "Loot table " + table + " loads");
		}
		helper.succeed();
	}
}
