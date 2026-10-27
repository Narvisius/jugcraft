package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.concordance.ConcordanceProgress;
import io.github.jimbozoomer.jugcraft.concordance.CrucibleBlockEntity;
import io.github.jimbozoomer.jugcraft.concordance.CrucibleStandBlockEntity;
import io.github.jimbozoomer.jugcraft.concordance.JugcraftConcordance;
import io.github.jimbozoomer.jugcraft.concordance.KindledLanternItem;
import io.github.jimbozoomer.jugcraft.concordance.LanternCharge;
import io.github.jimbozoomer.jugcraft.concordance.LumenSconceBlock;
import io.github.jimbozoomer.jugcraft.concordance.LumenSconceBlockEntity;
import io.github.jimbozoomer.jugcraft.concordance.RateGate;
import io.github.jimbozoomer.jugcraft.concordance.alchemy.Band;
import io.github.jimbozoomer.jugcraft.concordance.alchemy.Heat;
import io.github.jimbozoomer.jugcraft.concordance.diagnostic.Code;
import io.github.jimbozoomer.jugcraft.concordance.diagnostic.Diagnostic;
import io.github.jimbozoomer.jugcraft.energy.EnergyStorage;
import java.util.List;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemStorage;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;

/**
 * Roadmap step 29, interactions and economic loops (docs/features/arcane-concordance-economy.md), for the light the
 * Concordance burns: a measure begun is a measure spent. A Kindled Lantern put out, or a Lumen Sconce taken down,
 * keeps only the measures it had not begun, so lighting and putting out again (or breaking and placing again) never
 * gives light for nothing, and costs no more than a measure when done at once. The other loops are tested where their
 * systems are (the spire's field, the garden, a ritual and a porter sharing a pylon) and audited by check_economy. Since
 * Concordance Chunk C (C-05), a Crucible Stand turns JE into a crucible's heat and nothing else.
 */
public class ConcordanceEconomyGameTests {
	private static ServerPlayer player(GameTestHelper helper) {
		ServerPlayer player = TestPlayers.mock(helper);
		player.setGameMode(GameType.SURVIVAL);
		return player;
	}

	/**
	 * A lantern lit for 50 ticks and put out keeps one measure fewer; lit and put out in the same tick it keeps them all;
	 * five rounds of 50 lit ticks spend five measures, where burning 250 ticks without a break would have spent one.
	 */
	@GameTest(maxTicks = 20)
	public void aLanternPutOutHasSpentTheMeasureItBegan(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		ServerPlayer player = player(helper);
		ItemStack lantern = new ItemStack(JugcraftConcordance.KINDLED_LANTERN);
		player.setItemInHand(InteractionHand.MAIN_HAND, lantern);
		long now = ConcordanceProgress.now(level);
		KindledLanternItem.set(lantern, 10, now - 50, true);
		helper.assertTrue(KindledLanternItem.remaining(lantern, now) == 10, "Burning its first measure, it still shows ten");
		lantern.getItem().use(level, player, InteractionHand.MAIN_HAND);
		helper.assertTrue(!KindledLanternItem.lit(lantern) && KindledLanternItem.remaining(lantern, now) == 9,
				"Put out, the measure it began is spent: " + KindledLanternItem.remaining(lantern, now));
		lantern.getItem().use(level, player, InteractionHand.MAIN_HAND);
		lantern.getItem().use(level, player, InteractionHand.MAIN_HAND);
		helper.assertTrue(!KindledLanternItem.lit(lantern) && KindledLanternItem.remaining(lantern, now) == 9,
				"Lit and put out in the same tick, nothing burnt");
		for (int round = 0; round < 5; round++) {
			KindledLanternItem.set(lantern, KindledLanternItem.remaining(lantern, now), now - 50, true);
			lantern.getItem().use(level, player, InteractionHand.MAIN_HAND);
		}
		helper.assertTrue(KindledLanternItem.remaining(lantern, now) == 4,
				"Five rounds of 50 lit ticks spent five measures, never fewer than burning straight on: " + KindledLanternItem.remaining(lantern, now));
		LanternCharge charge = new LanternCharge(10, 1000L);
		helper.assertTrue(charge.kept(1000L, true) == 10 && charge.kept(1001L, true) == 9
				&& charge.kept(1000L + KindledLanternItem.BURN_TICKS, true) == 9 && charge.kept(1001L + KindledLanternItem.BURN_TICKS, true) == 8
				&& charge.kept(5000L, false) == 10, "A begun measure counts as spent; an unlit lantern keeps all");
		helper.succeed();
	}

	/**
	 * A sconce taken down keeps only the measures it had not begun: placed with Radiance, then broken a few ticks later,
	 * its item holds one measure fewer, so breaking and placing it again is never a way round its burning.
	 */
	@GameTest(maxTicks = 40)
	public void aSconceTakenDownHasSpentTheMeasureItBegan(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		for (int x = 0; x <= 4; x++) {
			for (int z = 0; z <= 4; z++) {
				helper.setBlock(new BlockPos(x, 1, z), Blocks.STONE);
			}
		}
		ServerPlayer player = player(helper);
		BlockPos relative = new BlockPos(2, 2, 2);
		helper.setBlock(relative, JugcraftConcordance.LUMEN_SCONCE);
		BlockPos pos = helper.absolutePos(relative);
		JugcraftConcordance.LUMEN_SCONCE.setPlacedBy(level, pos, level.getBlockState(pos), player, ItemStack.EMPTY);
		LumenSconceBlockEntity sconce = helper.getBlockEntity(relative, LumenSconceBlockEntity.class);
		long poured = ConcordanceProgress.now(level);
		ItemStack lantern = new ItemStack(JugcraftConcordance.KINDLED_LANTERN);
		KindledLanternItem.set(lantern, 40, poured, false);
		LumenSconceBlock.exchange(level, pos, player, lantern, false);
		long held = sconce.remaining(poured);
		helper.assertTrue(held == LumenSconceBlockEntity.POUR && sconce.kept(poured) == held, "Poured, and taken down at once it keeps all");
		helper.assertTrue(sconce.kept(poured + 1) == held - 1 && sconce.kept(poured + LumenSconceBlockEntity.BURN_TICKS) == held - 1
				&& sconce.kept(poured + LumenSconceBlockEntity.BURN_TICKS + 1) == held - 2, "A begun measure counts as spent");
		helper.runAtTickTime(10, () -> {
			LanternCharge item = sconce.collectComponents().get(JugcraftConcordance.RADIANCE);
			helper.assertTrue(item != null && item.stored() == held - 1,
					"Broken after a few ticks, its item holds one measure fewer: " + item);
			helper.assertTrue(sconce.remaining(ConcordanceProgress.now(level)) == held, "though it still shows them all while it burns");
			helper.succeed();
		});
	}

	/**
	 * Concordance Chunk C, C-05: a Crucible Stand turns JE into heat and nothing else. Set to each heat, it pays exactly its
	 * rate on each tick it heats (8, 16 or 32 JE) until its store runs short, then nothing. Set off, under a crucible
	 * already hotter than it aims, or with nothing to heat, it pays nothing. Nothing comes back out: no JE on any face (its
	 * seat takes none), no fluid, no item, and the crucible's mixture is untouched.
	 */
	@GameTest(maxTicks = 20)
	public void aStandTurnsElectricityIntoHeatOnly(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		BlockPos at = new BlockPos(2, 2, 2);
		helper.setBlock(at.below(), JugcraftConcordance.CRUCIBLE_STAND);
		helper.setBlock(at, JugcraftConcordance.CRUCIBLE);
		CrucibleStandBlockEntity stand = helper.getBlockEntity(at.below(), CrucibleStandBlockEntity.class);
		CrucibleBlockEntity crucible = helper.getBlockEntity(at, CrucibleBlockEntity.class);
		ServerPlayer player = player(helper);
		for (Band band : List.of(Band.WARM, Band.HOT, Band.SEARING)) {
			RateGate.forget(player.getUUID());
			stand.useEmpty(player, false);
			long rate = CrucibleStandBlockEntity.rate(band);
			stand.energy.setAmount(rate * 10 + rate / 2);
			int paid = 0;
			for (int tick = 0; tick < 12; tick++) {
				paid += stand.heat(level) ? 1 : 0;
			}
			helper.assertTrue(stand.setting() == band && paid == 10 && stand.energy.getAmount() == rate / 2,
					band.id + ": JE drawn is " + rate + " times the " + paid + " heated ticks; " + stand.energy.getAmount() + " left, short of a tick");
		}
		crucible.setTemperature(Heat.STAND_SEARING + 1);
		stand.energy.setAmount(CrucibleStandBlockEntity.CAPACITY);
		helper.assertTrue(!stand.heat(level) && stand.energy.getAmount() == CrucibleStandBlockEntity.CAPACITY, "A crucible hotter than it aims costs nothing");
		crucible.setTemperature(Heat.AMBIENT);
		RateGate.forget(player.getUUID());
		stand.useEmpty(player, false);
		int idle = 0;
		for (int tick = 0; tick < 10; tick++) {
			idle += stand.heat(level) ? 1 : 0;
		}
		helper.assertTrue(stand.setting() == Band.COLD && idle == 0 && stand.energy.getAmount() == CrucibleStandBlockEntity.CAPACITY, "Set off, it pays nothing");
		helper.assertTrue(crucible.mixture().isEmpty(), "Heat alone puts nothing into a crucible");
		helper.setBlock(at, Blocks.AIR);
		RateGate.forget(player.getUUID());
		stand.useEmpty(player, false);
		for (int tick = 0; tick < 10; tick++) {
			idle += stand.heat(level) ? 1 : 0;
		}
		helper.assertTrue(stand.setting() == Band.WARM && idle == 0 && stand.energy.getAmount() == CrucibleStandBlockEntity.CAPACITY
				&& Diagnostic.of(Code.NO_CRUCIBLE).equals(stand.plan(level).reason()), "Set warm with nothing on it, it pays nothing");
		BlockPos standAt = helper.absolutePos(at.below());
		try (Transaction transaction = Transaction.openOuter()) {
			helper.assertTrue(!stand.energy.supportsExtraction() && stand.energy.extract(1_000, transaction) == 0, "Its store gives nothing back");
		}
		for (Direction side : Direction.values()) {
			EnergyStorage face = EnergyStorage.SIDED.find(level, standAt, side);
			helper.assertTrue(side == Direction.UP ? face == null : face == stand.energy && !face.supportsExtraction(),
					"The " + side + " face: " + (side == Direction.UP ? "its seat takes no JE" : "takes JE, gives none"));
		}
		helper.assertTrue(FluidStorage.SIDED.find(level, standAt, null) == null && ItemStorage.SIDED.find(level, standAt, null) == null,
				"It holds no fluid and no items to give");
		helper.assertTrue(level.getEntitiesOfClass(ItemEntity.class, new AABB(standAt).inflate(2.0)).isEmpty(), "and drops nothing");
		helper.succeed();
	}
}
