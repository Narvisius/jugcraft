package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.concordance.ConcordanceData;
import io.github.jimbozoomer.jugcraft.concordance.ConcordanceEffects;
import io.github.jimbozoomer.jugcraft.concordance.ConcordanceProgress;
import io.github.jimbozoomer.jugcraft.concordance.ConcordanceSpells;
import io.github.jimbozoomer.jugcraft.concordance.Examination;
import io.github.jimbozoomer.jugcraft.concordance.Invocations;
import io.github.jimbozoomer.jugcraft.concordance.JugcraftConcordance;
import io.github.jimbozoomer.jugcraft.concordance.RateGate;
import io.github.jimbozoomer.jugcraft.concordance.effect.Cause;
import io.github.jimbozoomer.jugcraft.concordance.effect.EffectKind;
import io.github.jimbozoomer.jugcraft.concordance.effect.EffectSpec;
import io.github.jimbozoomer.jugcraft.concordance.effect.Intent;
import io.github.jimbozoomer.jugcraft.concordance.effect.Ledger;
import io.github.jimbozoomer.jugcraft.concordance.effect.Stacking;
import io.github.jimbozoomer.jugcraft.concordance.ember.Ember;
import io.github.jimbozoomer.jugcraft.concordance.rules.ConcordanceRules;
import io.github.jimbozoomer.jugcraft.concordance.rules.Definitions;
import io.github.jimbozoomer.jugcraft.concordance.rules.FocusPool;
import io.github.jimbozoomer.jugcraft.concordance.rules.ResearchState;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.spell_engine.api.spell.Spell;
import net.spell_engine.api.spell.registry.SpellRegistry;
import net.spell_engine.internals.SpellExecution;
import net.spell_engine.internals.casting.SpellCast;
import net.spell_engine.internals.casting.SpellCaster;
import net.spell_engine.internals.container.SpellContainerSource;
import net.spell_engine.internals.target.SpellTarget;
import net.spell_power.api.SpellSchools;

/**
 * Ember, part 1 (docs/features/arcane-concordance-ember.md): Hearthbinding begins with things that hold fire and,
 * understood, teaches three invocations (a fourth at mastery); Hearthspark kindles a hearth only where its caster could
 * and never places fire, and each kind of hearth it kindles counts once towards mastery; Cinderbolt burns with fire Spell
 * Power and leaves its target smouldering; Smoulder goes out in water and does no harm through Fire Resistance;
 * Hearthguard gives Fire Resistance; Hearthflare burns the creatures round its caster and never the caster.
 */
public class ConcordanceEmberGameTests {
	private static final String FIRST_LIGHT = "jugcraft:first_light";
	private static final String HEARTHBINDING = "jugcraft:hearthbinding";
	private static final List<String> EMBER = List.of("jugcraft:hearthspark", "jugcraft:hearthguard", "jugcraft:cinderbolt",
			"jugcraft:hearthflare");
	/** Hearthspark's own word: an alteration in Ember's school (data/jugcraft/concordance/component/kindling.json). */
	private static final EffectSpec KINDLING = new EffectSpec(EffectKind.ALTERATION, Intent.HELPFUL, 0, 0, null, Stacking.STRONGEST,
			Ember.SCHOOL);
	private static final UUID NEIGHBOUR = UUID.fromString("00000000-0000-0000-0000-00000000e3b1");
	/** Ticks to let newly spawned creatures settle before an area search (see ConcordanceComposeGameTests). */
	private static final int SETTLE_TICKS = 10;
	/** Ticks to wait out vanilla's hurt immunity (a second hit within 10 ticks counts only for what it exceeds). */
	private static final int HURT_IMMUNITY_TICKS = 12;

	private static void floor(GameTestHelper helper) {
		for (int x = 0; x <= 7; x++) {
			for (int z = 0; z <= 7; z++) {
				helper.setBlock(new BlockPos(x, 1, z), Blocks.STONE);
			}
		}
	}

	/** A survival player with the Initiate's Wand on {@code standAt}, facing south (+Z), looking down {@code pitch} degrees. */
	private static ServerPlayer player(GameTestHelper helper, BlockPos standAt, float pitch) {
		ServerPlayer player = helper.makeMockServerPlayerInLevel();
		player.setGameMode(GameType.SURVIVAL);
		BlockPos absolute = helper.absolutePos(standAt);
		player.snapTo(absolute.getX() + 0.5, absolute.getY(), absolute.getZ() + 0.5, 0.0F, pitch);
		player.setYHeadRot(0.0F);
		player.setYBodyRot(0.0F);
		player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(JugcraftConcordance.INITIATE_WAND));
		RateGate.forget(player.getUUID());
		return player;
	}

	private static Holder<Spell> spell(ServerLevel level, String id) {
		return SpellRegistry.from(level).get(Identifier.parse(id)).orElseThrow();
	}

	private static void refreshSpells(ServerPlayer player) {
		SpellContainerSource.setDirty(player, ConcordanceSpells.SOURCE);
		SpellContainerSource.setDirty(player, "main_hand");
		SpellContainerSource.update(player);
	}

	private static void cast(ServerPlayer player, String id) {
		ServerLevel level = player.level();
		((SpellCaster.Player) player).getCooldownManager().reset(null);
		SpellExecution.performSpell(level, player, spell(level, id), SpellTarget.SearchResult.empty(), SpellCast.Action.RELEASE, 1.0F);
	}

	/** First Light understood (Hearthbinding's prerequisite), then Hearthbinding at {@code state}. */
	private static void learn(ServerPlayer player, ResearchState state) {
		ConcordanceProgress.grant(player, FIRST_LIGHT, ResearchState.UNDERSTOOD);
		ConcordanceProgress.grant(player, HEARTHBINDING, state);
		refreshSpells(player);
	}

	private static ResearchState hearthbinding(ServerPlayer player) {
		return ConcordanceProgress.knowledge(player).state(HEARTHBINDING);
	}

	private static ConcordanceEffects.Result kindle(GameTestHelper helper, ServerPlayer actor, BlockPos at) {
		Cause cause = Cause.of(actor.getUUID(), Cause.Origin.INVOCATION, "jugcraft:hearthspark", ConcordanceEffects.nextSerial());
		ConcordanceEffects.Context context = new ConcordanceEffects.Context(helper.getLevel(), cause, actor,
				new Ledger(new Ledger.Limits(16, 256, 0)), "0/0/0", actor.position());
		return ConcordanceEffects.apply(context, KINDLING, helper.absolutePos(at));
	}

	private static BlockState unlitCampfire() {
		return Blocks.CAMPFIRE.defaultBlockState().setValue(CampfireBlock.LIT, false);
	}

	private static boolean lit(GameTestHelper helper, BlockPos at) {
		BlockState state = helper.getBlockState(at);
		return state.hasProperty(BlockStateProperties.LIT) && state.getValue(BlockStateProperties.LIT);
	}

	/** No fire anywhere in the test's floor space: Ember kindles hearths and never places fire. */
	private static boolean noFire(GameTestHelper helper) {
		for (BlockPos pos : BlockPos.betweenClosed(new BlockPos(0, 2, 0), new BlockPos(7, 4, 7))) {
			if (helper.getBlockState(pos).is(Blocks.FIRE)) {
				return false;
			}
		}
		return true;
	}

	/**
	 * The four Ember invocations load cleanly, each fitting the Initiate's Wand and taught by Hearthbinding; understood,
	 * it teaches Hearthspark, Hearthguard and Cinderbolt beside First Light's three, and mastered adds Hearthflare and
	 * lowers each of its costs by one.
	 */
	@GameTest(maxTicks = 20)
	public void hearthbindingTeachesFourInvocations(GameTestHelper helper) {
		ConcordanceRules rules = ConcordanceData.rules();
		helper.assertTrue(rules.problems().isEmpty(), "The rules load cleanly: " + rules.problems());
		for (String id : EMBER) {
			Definitions.Invocation invocation = rules.invocations().get(id);
			helper.assertTrue(invocation != null && invocation.principle().equals("ember") && invocation.research().equals(HEARTHBINDING),
					id + " is Ember's, taught by Hearthbinding: " + invocation);
			helper.assertTrue(rules.authored(id, "jugcraft:initiate_wand") != null, id + " fits the Initiate's Wand");
			helper.assertTrue(spell(helper.getLevel(), id).is(ConcordanceSpells.SPELLS), id + " is a Concordance spell");
		}
		ServerPlayer player = player(helper, new BlockPos(1, 2, 1), 0.0F);
		learn(player, ResearchState.UNDERSTOOD);
		Map<String, Integer> understood = ConcordanceProgress.knowledge(player).invocations();
		helper.assertTrue(understood.equals(Map.of("jugcraft:kindle", 4, "jugcraft:aegis", 5, "jugcraft:revelation", 3,
				"jugcraft:hearthspark", 3, "jugcraft:hearthguard", 5, "jugcraft:cinderbolt", 5)),
				"Understanding Hearthbinding teaches Hearthspark, Hearthguard and Cinderbolt: " + understood);
		learn(player, ResearchState.MASTERED);
		Map<String, Integer> mastered = ConcordanceProgress.knowledge(player).invocations();
		helper.assertTrue(mastered.get("jugcraft:hearthflare") == 6 && mastered.get("jugcraft:hearthspark") == 2
				&& mastered.get("jugcraft:hearthguard") == 4 && mastered.get("jugcraft:cinderbolt") == 4,
				"Mastery teaches Hearthflare and lowers the costs: " + mastered);
		helper.succeed();
	}

	/**
	 * Hearthbinding begins only after First Light is understood; then examining coal encounters it, and three different
	 * things that hold fire (coal, charcoal, a torch) observe it. The same specimen twice counts once.
	 */
	@GameTest(maxTicks = 20)
	public void thingsThatHoldFireBeginHearthbinding(GameTestHelper helper) {
		ServerPlayer player = player(helper, new BlockPos(1, 2, 1), 0.0F);
		ServerLevel level = helper.getLevel();
		Examination.forget(player.getUUID());
		Examination.examine(player, level, new ItemStack(Items.COAL));
		helper.assertTrue(hearthbinding(player) == ResearchState.NONE, "Before First Light is understood, coal teaches nothing: "
				+ hearthbinding(player));
		ConcordanceProgress.grant(player, FIRST_LIGHT, ResearchState.UNDERSTOOD);
		for (ItemStack specimen : List.of(new ItemStack(Items.COAL), new ItemStack(Items.COAL), new ItemStack(Items.CHARCOAL))) {
			Examination.forget(player.getUUID());
			Examination.examine(player, level, specimen);
		}
		helper.assertTrue(hearthbinding(player) == ResearchState.ENCOUNTERED, "Coal twice and charcoal are two different things: "
				+ hearthbinding(player));
		Examination.forget(player.getUUID());
		Examination.examine(player, level, new ItemStack(Items.TORCH));
		helper.assertTrue(hearthbinding(player) == ResearchState.OBSERVED, "A torch is the third: observed: " + hearthbinding(player));
		helper.succeed();
	}

	/**
	 * Hearthspark, cast at an unlit campfire three blocks off, kindles it for 3 Focus and places no fire. Its word kindles
	 * only where the caster could: a neighbour's claimed campfire stays dark, as do a waterlogged one and plain stone.
	 */
	@GameTest(maxTicks = 20)
	public void hearthsparkKindlesWhereItsCasterMay(GameTestHelper helper) {
		floor(helper);
		BlockPos campfire = new BlockPos(1, 2, 4);
		helper.setBlock(campfire, unlitCampfire());
		// Looking down from the eye (1.62 above the feet) to low on the campfire, three blocks south: the line meets its
		// north face below its top (0.4375 high).
		ServerPlayer player = player(helper, new BlockPos(1, 2, 1), (float) Math.toDegrees(Math.atan2(1.62 - 0.1, 3.0)));
		learn(player, ResearchState.UNDERSTOOD);
		cast(player, "jugcraft:hearthspark");
		helper.assertTrue(lit(helper, campfire), "Hearthspark kindles the campfire: " + helper.getBlockState(campfire));
		helper.assertTrue(ConcordanceProgress.currentFocus(player) == FocusPool.MAX - 3, "for 3 Focus: "
				+ ConcordanceProgress.currentFocus(player));
		helper.assertTrue(noFire(helper), "and places no fire");

		BlockPos theirs = new BlockPos(4, 2, 2);
		BlockPos soaked = new BlockPos(4, 2, 4);
		BlockPos stone = new BlockPos(4, 2, 6);
		helper.setBlock(theirs, unlitCampfire());
		helper.setBlock(soaked, unlitCampfire().setValue(CampfireBlock.WATERLOGGED, true));
		helper.setBlock(stone, Blocks.STONE);
		try (TestClaims claims = TestClaims.open()) {
			claims.block(helper.absolutePos(theirs), NEIGHBOUR);
			helper.assertValueEqual(kindle(helper, player, theirs), ConcordanceEffects.Result.NOT_ALLOWED, "not in a neighbour's claim");
			helper.assertTrue(!lit(helper, theirs), "which stays dark");
		}
		helper.assertValueEqual(kindle(helper, player, soaked), ConcordanceEffects.Result.NOTHING, "a waterlogged campfire will not catch");
		helper.assertValueEqual(kindle(helper, player, stone), ConcordanceEffects.Result.NOTHING, "stone is no hearth");
		helper.assertTrue(helper.getBlockState(stone).is(Blocks.STONE) && noFire(helper), "and nothing else is set alight");
		helper.succeed();
	}

	/**
	 * Each kind of hearth kindled counts once: a campfire, a candle, another campfire (no more) and a candle cake master
	 * Hearthbinding and teach Hearthflare. Someone who has not understood Hearthbinding learns nothing by it.
	 */
	@GameTest(maxTicks = 20)
	public void eachKindOfHearthCountsOnce(GameTestHelper helper) {
		floor(helper);
		ServerPlayer player = player(helper, new BlockPos(0, 2, 0), 0.0F);
		ServerPlayer novice = player(helper, new BlockPos(0, 2, 7), 0.0F);
		ConcordanceProgress.grant(novice, FIRST_LIGHT, ResearchState.UNDERSTOOD);
		learn(player, ResearchState.UNDERSTOOD);
		helper.setBlock(new BlockPos(2, 2, 2), unlitCampfire());
		helper.setBlock(new BlockPos(3, 2, 2), Blocks.CANDLE);
		helper.setBlock(new BlockPos(4, 2, 2), unlitCampfire());
		helper.setBlock(new BlockPos(5, 2, 2), Blocks.CANDLE_CAKE);
		helper.setBlock(new BlockPos(2, 2, 5), unlitCampfire());

		helper.assertTrue(kindle(helper, novice, new BlockPos(2, 2, 5)).applied()
				&& hearthbinding(novice) == ResearchState.NONE, "A novice may kindle, but learns nothing by it");
		helper.assertTrue(kindle(helper, player, new BlockPos(2, 2, 2)).applied() && kindle(helper, player, new BlockPos(3, 2, 2)).applied(),
				"A campfire and a candle kindle");
		helper.assertTrue(hearthbinding(player) == ResearchState.UNDERSTOOD, "Two kinds are not yet mastery");
		helper.assertTrue(kindle(helper, player, new BlockPos(4, 2, 2)).applied() && hearthbinding(player) == ResearchState.UNDERSTOOD,
				"A second campfire counts for nothing more");
		helper.assertTrue(kindle(helper, player, new BlockPos(5, 2, 2)).applied() && lit(helper, new BlockPos(5, 2, 2)),
				"A candle cake kindles");
		helper.assertTrue(hearthbinding(player) == ResearchState.MASTERED
				&& ConcordanceProgress.knowledge(player).invocations().containsKey("jugcraft:hearthflare"),
				"Three kinds master Hearthbinding, and teach Hearthflare: " + hearthbinding(player));
		helper.succeed();
	}

	/**
	 * Cinderbolt: 3 fire damage to the first creature on its line, which then smoulders (alight within a tick), for 5
	 * Focus. With 4 points of fire Spell Power above the base it deals 5 (0.5 a point).
	 */
	@GameTest(maxTicks = 60)
	public void cinderboltBurnsAndGrowsWithFireSpellPower(GameTestHelper helper) {
		floor(helper);
		ServerPlayer player = player(helper, new BlockPos(1, 2, 1), 0.0F);
		Mob target = helper.spawnWithNoFreeWill(EntityTypes.VILLAGER, new BlockPos(1, 2, 5));
		learn(player, ResearchState.UNDERSTOOD);
		float before = target.getHealth();
		cast(player, "jugcraft:cinderbolt");
		helper.assertTrue(before - target.getHealth() == 3.0F, "Cinderbolt deals 3: " + before + " -> " + target.getHealth());
		helper.assertTrue(target.hasEffect(Ember.SMOULDER) && ConcordanceProgress.currentFocus(player) == FocusPool.MAX - 5,
				"leaves its target smouldering, for 5 Focus");
		helper.runAfterDelay(2, () -> {
			helper.assertTrue(target.getRemainingFireTicks() > 0, "Smouldering, the target is alight");
			// Put the fire out before the next cast, so burning cannot blur what the cast itself deals.
			target.removeEffect(Ember.SMOULDER);
			target.clearFire();
			player.getAttribute(SpellSchools.getSchool(Ember.SCHOOL).attributeEntry).addTransientModifier(
					new AttributeModifier(Jugcraft.id("test_fire_power"), 4.0, AttributeModifier.Operation.ADD_VALUE));
			helper.assertTrue(Invocations.powerAboveBase(player, "spell_power:fire") == 4.0, "4 fire Spell Power above the base: "
					+ Invocations.powerAboveBase(player, "spell_power:fire"));
			helper.runAfterDelay(HURT_IMMUNITY_TICKS, () -> {
				float healthy = target.getHealth();
				ConcordanceProgress.setFocus(player, FocusPool.MAX);
				cast(player, "jugcraft:cinderbolt");
				helper.assertTrue(healthy - target.getHealth() == 5.0F, "With it Cinderbolt deals 5: " + healthy + " -> " + target.getHealth());
				helper.succeed();
			});
		});
	}

	/** Smoulder goes out in water, ending itself; through Fire Resistance it burns but does no harm. */
	@GameTest(maxTicks = 60)
	public void smoulderAnswersToWaterAndFireResistance(GameTestHelper helper) {
		floor(helper);
		BlockPos pool = new BlockPos(2, 2, 2);
		// A one-block pool walled in, so the water cannot spread to the other villager.
		for (BlockPos wall : List.of(pool.north(), pool.south(), pool.east(), pool.west())) {
			helper.setBlock(wall, Blocks.STONE);
		}
		helper.setBlock(pool, Blocks.WATER);
		Mob soaked = helper.spawnWithNoFreeWill(EntityTypes.VILLAGER, pool);
		Mob warded = helper.spawnWithNoFreeWill(EntityTypes.VILLAGER, new BlockPos(5, 2, 5));
		soaked.addEffect(new MobEffectInstance(Ember.SMOULDER, 100));
		warded.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 200));
		warded.addEffect(new MobEffectInstance(Ember.SMOULDER, 100));
		float health = warded.getHealth();
		helper.runAfterDelay(30, () -> {
			helper.assertTrue(!soaked.hasEffect(Ember.SMOULDER) && soaked.getRemainingFireTicks() <= 0,
					"In water the smoulder goes out: " + soaked.getRemainingFireTicks());
			helper.assertTrue(warded.hasEffect(Ember.SMOULDER) && warded.getHealth() == health,
					"Through Fire Resistance it burns on but does no harm: " + warded.getHealth());
			helper.succeed();
		});
	}

	/** Hearthguard: Fire Resistance for 30 seconds on its caster, for 5 Focus. */
	@GameTest(maxTicks = 20)
	public void hearthguardBanksTheFire(GameTestHelper helper) {
		floor(helper);
		ServerPlayer player = player(helper, new BlockPos(1, 2, 1), 0.0F);
		learn(player, ResearchState.UNDERSTOOD);
		cast(player, "jugcraft:hearthguard");
		MobEffectInstance guard = player.getEffect(MobEffects.FIRE_RESISTANCE);
		helper.assertTrue(guard != null && guard.getDuration() > 590 && guard.getDuration() <= 600
				&& ConcordanceProgress.currentFocus(player) == FocusPool.MAX - 5, "Fire Resistance for 30 s, for 5 Focus: " + guard);
		helper.succeed();
	}

	/**
	 * Hearthflare: 4 fire damage and Smoulder to the creatures within 3 blocks of its caster, none to one 4 blocks off, and
	 * never to the caster; the mastered cost, 6 Focus.
	 */
	@GameTest(maxTicks = 40)
	public void hearthflareBurnsRoundItsCasterOnly(GameTestHelper helper) {
		floor(helper);
		ServerPlayer player = player(helper, new BlockPos(3, 2, 3), 0.0F);
		Mob near = helper.spawnWithNoFreeWill(EntityTypes.VILLAGER, new BlockPos(3, 2, 5));
		Mob beside = helper.spawnWithNoFreeWill(EntityTypes.VILLAGER, new BlockPos(5, 2, 3));
		Mob far = helper.spawnWithNoFreeWill(EntityTypes.VILLAGER, new BlockPos(3, 2, 7));
		learn(player, ResearchState.MASTERED);
		helper.runAfterDelay(SETTLE_TICKS, () -> {
			float health = player.getHealth();
			cast(player, "jugcraft:hearthflare");
			helper.assertTrue(near.getHealth() == near.getMaxHealth() - 4.0F && beside.getHealth() == beside.getMaxHealth() - 4.0F,
					"Both near villagers take 4: " + near.getHealth() + " " + beside.getHealth());
			helper.assertTrue(near.hasEffect(Ember.SMOULDER) && beside.hasEffect(Ember.SMOULDER), "and smoulder");
			helper.assertTrue(far.getHealth() == far.getMaxHealth() && !far.hasEffect(Ember.SMOULDER), "The far one is untouched");
			helper.assertTrue(player.getHealth() == health && !player.hasEffect(Ember.SMOULDER), "and so is the caster");
			helper.assertTrue(ConcordanceProgress.currentFocus(player) == FocusPool.MAX - 6, "for the mastered 6 Focus: "
					+ ConcordanceProgress.currentFocus(player));
			helper.succeed();
		});
	}
}
