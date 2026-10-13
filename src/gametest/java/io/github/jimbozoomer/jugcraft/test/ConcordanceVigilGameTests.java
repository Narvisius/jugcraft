package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.concordance.ConcordanceProgress;
import io.github.jimbozoomer.jugcraft.concordance.RateGate;
import io.github.jimbozoomer.jugcraft.concordance.crimson.Exhaustion;
import io.github.jimbozoomer.jugcraft.concordance.crimson.Growth;
import io.github.jimbozoomer.jugcraft.concordance.crimson.OfferingState;
import io.github.jimbozoomer.jugcraft.concordance.crimson.Offerings;
import io.github.jimbozoomer.jugcraft.concordance.rules.FocusPool;
import io.github.jimbozoomer.jugcraft.concordance.rules.ResearchState;
import io.github.jimbozoomer.jugcraft.concordance.vigil.CrimsonChaliceItem;
import io.github.jimbozoomer.jugcraft.concordance.vigil.ThornheartBladeItem;
import io.github.jimbozoomer.jugcraft.concordance.vigil.Vigil;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.level.GameType;

/**
 * Roadmap step 16, Crimson resources and living equipment (docs/features/arcane-concordance-vitae.md), on a real
 * server. Every test passes its own game time where time matters, so none waits: an offering turns health into Vitae
 * and adds exhaustion; healing to full does not reset it (the next offering yields less); an offering never goes below
 * the floor or into a full chalice; exhaustion clears only with time; a Crimson Surge turns Vitae into Focus once a
 * minute and never beyond full; the Thornheart Blade grows by varied deeds, not by one repeated, gains its attack bonus
 * only while it has vigor and records mastery; a real kill with it counts; and Vitae, growth and offering state survive
 * a save.
 */
public class ConcordanceVigilGameTests {
	private static ServerPlayer adept(GameTestHelper helper) {
		ServerPlayer player = helper.makeMockServerPlayerInLevel();
		player.setGameMode(GameType.SURVIVAL);
		ConcordanceProgress.grant(player, "jugcraft:first_light", ResearchState.UNDERSTOOD);
		ConcordanceProgress.grant(player, Vigil.RESEARCH, ResearchState.UNDERSTOOD);
		RateGate.forget(player.getUUID());
		return player;
	}

	private static ItemStack chalice(int vitae) {
		ItemStack stack = new ItemStack(Vigil.CRIMSON_CHALICE);
		CrimsonChaliceItem.setVitae(stack, vitae);
		return stack;
	}

	// ---------------------------------------------------------------- offerings

	/**
	 * An offering takes 4 health for 4 Vitae and adds exhaustion; not again at once; healed to full and offering again a
	 * little later, the exhaustion is still there, and the third offering yields half.
	 */
	@GameTest(maxTicks = 20)
	public void healingDoesNotResetTheOffering(GameTestHelper helper) {
		ServerPlayer adept = adept(helper);
		adept.setHealth(20.0F);
		ItemStack held = chalice(0);
		long now = 1_000_000L;
		helper.assertTrue(CrimsonChaliceItem.use(adept, held, ItemStack.EMPTY, false, now).equals(InteractionResult.SUCCESS),
				"A Crimson Rites adept offers");
		helper.assertTrue(adept.getHealth() == 16.0F && CrimsonChaliceItem.vitae(held) == 4, "4 health for 4 Vitae: health "
				+ adept.getHealth() + ", Vitae " + CrimsonChaliceItem.vitae(held));
		helper.assertTrue(Vigil.offering(adept).exhaustion().current(now) == 3, "and 3 points of exhaustion");
		helper.assertTrue(Vigil.offer(adept, held, now + 10).outcome() == Offerings.Outcome.TOO_SOON && adept.getHealth() == 16.0F,
				"Not again at once, and no health taken");
		adept.setHealth(20.0F);
		helper.assertTrue(Vigil.offer(adept, held, now + 200).vitae() == 4 && Vigil.offering(adept).exhaustion().current(now + 200) == 6,
				"Healed to full, the second offering still counts the first's exhaustion");
		adept.setHealth(20.0F);
		Offerings.Attempt third = Vigil.offer(adept, held, now + 400);
		helper.assertTrue(third.outcome() == Offerings.Outcome.OFFERED && third.vitae() == 2 && CrimsonChaliceItem.vitae(held) == 10,
				"The third, at 6 points, yields half: " + third.vitae());
		helper.succeed();
	}

	/** An offering never leaves the giver below the floor, and takes no health for a full chalice or from a novice. */
	@GameTest(maxTicks = 20)
	public void anOfferingKeepsItsLimits(GameTestHelper helper) {
		ServerPlayer adept = adept(helper);
		adept.setHealth(11.0F);
		ItemStack held = chalice(0);
		helper.assertTrue(Vigil.offer(adept, held, 2_000_000L).outcome() == Offerings.Outcome.TOO_WEAK && adept.getHealth() == 11.0F
				&& CrimsonChaliceItem.vitae(held) == 0, "Never below the floor of 8");
		adept.setHealth(20.0F);
		ItemStack full = chalice(Vigil.CHALICE_CAPACITY - 1);
		helper.assertTrue(Vigil.offer(adept, full, 2_000_000L).outcome() == Offerings.Outcome.FULL && adept.getHealth() == 20.0F,
				"No health taken for a chalice with no room");
		ServerPlayer novice = helper.makeMockServerPlayerInLevel();
		novice.setGameMode(GameType.SURVIVAL);
		novice.setHealth(20.0F);
		helper.assertTrue(!CrimsonChaliceItem.use(novice, chalice(0), ItemStack.EMPTY, false, 2_000_000L).equals(InteractionResult.SUCCESS)
				&& novice.getHealth() == 20.0F, "A novice cannot offer");
		helper.succeed();
	}

	/** Exhaustion at its limit refuses offerings; three recovery periods later one is allowed again, at a quarter. */
	@GameTest(maxTicks = 20)
	public void exhaustionClearsOnlyWithTime(GameTestHelper helper) {
		ServerPlayer adept = adept(helper);
		adept.setHealth(20.0F);
		long now = 3_000_000L;
		adept.setAttached(Vigil.OFFERING, new OfferingState(new Exhaustion(Exhaustion.MAX, now), now - 1000, Long.MIN_VALUE / 2));
		ItemStack held = chalice(0);
		helper.assertTrue(Vigil.offer(adept, held, now).outcome() == Offerings.Outcome.EXHAUSTED && adept.getHealth() == 20.0F,
				"At the limit, refused");
		adept.heal(20.0F);
		helper.assertTrue(Vigil.offer(adept, held, now + 100).outcome() == Offerings.Outcome.EXHAUSTED, "Healing changes nothing");
		long later = now + 3L * Exhaustion.RECOVERY_TICKS;
		Offerings.Attempt attempt = Vigil.offer(adept, held, later);
		helper.assertTrue(attempt.outcome() == Offerings.Outcome.OFFERED && attempt.vitae() == 1, "Three periods later, a quarter: " + attempt.vitae());
		helper.succeed();
	}

	/** A Crimson Surge: 6 Vitae for 6 Focus, not again within a minute, never beyond full Focus. */
	@GameTest(maxTicks = 20)
	public void aSurgeTurnsVitaeIntoFocus(GameTestHelper helper) {
		ServerPlayer adept = adept(helper);
		long now = helper.getLevel().getGameTime();
		adept.setAttached(io.github.jimbozoomer.jugcraft.concordance.JugcraftConcordance.FOCUS, new FocusPool(5, now));
		ItemStack held = chalice(12);
		helper.assertTrue(Vigil.surge(adept, held, now).equals("surged") && CrimsonChaliceItem.vitae(held) == 6
				&& ConcordanceProgress.focus(adept).current(now) == 11, "6 Vitae became 6 Focus");
		helper.assertTrue(Vigil.surge(adept, held, now + 100).equals("surge_too_soon") && CrimsonChaliceItem.vitae(held) == 6, "Not again within a minute");
		adept.setAttached(io.github.jimbozoomer.jugcraft.concordance.JugcraftConcordance.FOCUS, new FocusPool(FocusPool.MAX, now));
		helper.assertTrue(Vigil.surge(adept, held, now + Vigil.SURGE_COOLDOWN).equals("surge_full") && CrimsonChaliceItem.vitae(held) == 6,
				"Never beyond full Focus");
		helper.succeed();
	}

	// ---------------------------------------------------------------- living equipment

	/**
	 * The blade grows by varied deeds: fifty kills of one kind in a day pay 8 and leave it dormant; enduring and slaying
	 * different things wake it; fed, it has its stage's attack bonus as an attribute modifier, and a blow spends vigor;
	 * days of varied use grow it to its second stage, which masters Crimson Rites.
	 */
	@GameTest(maxTicks = 20)
	public void theBladeGrowsByVariedDeedsOnly(GameTestHelper helper) {
		ServerPlayer adept = adept(helper);
		ItemStack blade = new ItemStack(Vigil.THORNHEART_BLADE);
		long day = 10L * Growth.WINDOW_TICKS;
		for (int i = 0; i < 50; i++) {
			Vigil.deed(adept, blade, Growth.Deed.SLAY, "minecraft:zombie", day + i);
		}
		helper.assertTrue(Vigil.growth(blade).points(Growth.Deed.SLAY) == 8 && Vigil.growth(blade).stage() == 0,
				"Fifty zombies in a day pay 8, and it stays dormant: " + Vigil.growth(blade).points());
		for (String harm : new String[] {"minecraft:mob_attack", "minecraft:arrow", "minecraft:fall"}) {
			Vigil.deed(adept, blade, Growth.Deed.ENDURE, harm, day + 100);
		}
		for (String kind : new String[] {"minecraft:skeleton", "minecraft:spider"}) {
			Vigil.deed(adept, blade, Growth.Deed.SLAY, kind, day + 200);
		}
		helper.assertTrue(Vigil.growth(blade).stage() == 1, "Enduring and slaying different things wake it: " + Vigil.growth(blade).points());
		helper.assertTrue(ThornheartBladeItem.bonus(blade) == 0.0, "Without vigor its powers sleep");
		ItemStack held = chalice(10);
		helper.assertTrue(Vigil.nourish(adept, blade, held, day + 300) == Vigil.NOURISH_VITAE && Vigil.growth(blade).vigor() > 0,
				"Fed from the chalice");
		ItemAttributeModifiers modifiers = blade.get(DataComponents.ATTRIBUTE_MODIFIERS);
		helper.assertTrue(ThornheartBladeItem.bonus(blade) == Vigil.STAGE_DAMAGE && modifiers != null
				&& modifiers.modifiers().stream().anyMatch(entry -> entry.modifier().id().equals(ThornheartBladeItem.GROWTH_MODIFIER)),
				"Fed and awake, it carries its stage's attack bonus");
		Zombie target = helper.spawnWithNoFreeWill(EntityTypes.ZOMBIE, new BlockPos(2, 1, 2));
		int vigor = Vigil.growth(blade).vigor();
		blade.getItem().hurtEnemy(blade, target, adept);
		helper.assertTrue(Vigil.growth(blade).vigor() == vigor - Vigil.BLOW_VIGOR, "A blow spends vigor");
		for (int d = 0; d < 6; d++) {
			long t = (20L + d) * Growth.WINDOW_TICKS;
			for (String kind : new String[] {"minecraft:zombie", "minecraft:husk", "minecraft:creeper"}) {
				Vigil.deed(adept, blade, Growth.Deed.SLAY, kind, t);
			}
			for (String harm : new String[] {"minecraft:mob_attack", "minecraft:arrow", "minecraft:explosion"}) {
				Vigil.deed(adept, blade, Growth.Deed.ENDURE, harm, t);
			}
			Vigil.nourish(adept, blade, chalice(10), t);
		}
		helper.assertTrue(Vigil.growth(blade).stage() >= 2, "Days of varied use grow it: " + Vigil.growth(blade).points());
		helper.assertTrue(Vigil.state(adept) == ResearchState.MASTERED, "Its second stage masters Crimson Rites: " + Vigil.state(adept));
		helper.succeed();
	}

	/** A real kill with the blade in hand counts as a slaying deed (the death event, not a call in the test). */
	@GameTest(maxTicks = 20)
	public void aKillWithTheBladeCounts(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		ServerPlayer adept = adept(helper);
		ItemStack blade = new ItemStack(Vigil.THORNHEART_BLADE);
		adept.setItemInHand(InteractionHand.MAIN_HAND, blade);
		Zombie target = helper.spawnWithNoFreeWill(EntityTypes.ZOMBIE, new BlockPos(2, 1, 2));
		target.hurtServer(level, level.damageSources().playerAttack(adept), 1000.0F);
		ItemStack wielded = adept.getMainHandItem();
		helper.assertTrue(!target.isAlive() && Vigil.growth(wielded).points(Growth.Deed.SLAY) == Growth.DIMINISHING.get(0),
				"The kill counted: " + Vigil.growth(wielded).points());
		helper.succeed();
	}

	// ---------------------------------------------------------------- saving

	/** Vitae, a blade's growth and a player's offering state survive a save exactly. */
	@GameTest(maxTicks = 20)
	public void theVigilKeepsWhatItHoldsThroughASave(GameTestHelper helper) {
		ServerPlayer adept = adept(helper);
		ItemStack blade = new ItemStack(Vigil.THORNHEART_BLADE);
		Vigil.deed(adept, blade, Growth.Deed.SLAY, "minecraft:zombie", 50_000L);
		Vigil.nourish(adept, blade, chalice(10), 50_000L);
		Growth growth = Vigil.growth(blade);
		Tag saved = Vigil.GROWTH_CODEC.encodeStart(NbtOps.INSTANCE, growth).getOrThrow();
		helper.assertTrue(Vigil.GROWTH_CODEC.parse(NbtOps.INSTANCE, saved).getOrThrow().equals(growth), "A blade's growth is saved exactly");
		OfferingState state = new OfferingState(new Exhaustion(7, 1234L), 999L, 555L);
		Tag offering = Vigil.OFFERING_CODEC.encodeStart(NbtOps.INSTANCE, state).getOrThrow();
		helper.assertTrue(Vigil.OFFERING_CODEC.parse(NbtOps.INSTANCE, offering).getOrThrow().equals(state), "Offering state is saved exactly");
		ItemStack held = chalice(17);
		var ops = helper.getLevel().registryAccess().createSerializationContext(NbtOps.INSTANCE);
		ItemStack copy = ItemStack.CODEC.parse(ops, ItemStack.CODEC.encodeStart(ops, held).getOrThrow()).getOrThrow();
		helper.assertTrue(CrimsonChaliceItem.vitae(copy) == 17, "The chalice keeps its Vitae");
		helper.succeed();
	}
}
