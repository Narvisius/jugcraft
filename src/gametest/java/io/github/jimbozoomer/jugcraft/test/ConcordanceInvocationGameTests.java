package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.concordance.ComposedSpells;
import io.github.jimbozoomer.jugcraft.concordance.ConcordanceData;
import io.github.jimbozoomer.jugcraft.concordance.ConcordanceEffects;
import io.github.jimbozoomer.jugcraft.concordance.ConcordanceProgress;
import io.github.jimbozoomer.jugcraft.concordance.ConcordanceSpells;
import io.github.jimbozoomer.jugcraft.concordance.Invocations;
import io.github.jimbozoomer.jugcraft.concordance.JugcraftConcordance;
import io.github.jimbozoomer.jugcraft.concordance.RateGate;
import io.github.jimbozoomer.jugcraft.concordance.Tunings;
import io.github.jimbozoomer.jugcraft.concordance.compose.Authored;
import io.github.jimbozoomer.jugcraft.concordance.compose.Compiler;
import io.github.jimbozoomer.jugcraft.concordance.compose.Instrument;
import io.github.jimbozoomer.jugcraft.concordance.compose.Plan;
import io.github.jimbozoomer.jugcraft.concordance.effect.Cause;
import io.github.jimbozoomer.jugcraft.concordance.effect.Ledger;
import io.github.jimbozoomer.jugcraft.concordance.rules.ConcordanceRules;
import io.github.jimbozoomer.jugcraft.concordance.rules.Definitions;
import io.github.jimbozoomer.jugcraft.concordance.rules.FocusPool;
import io.github.jimbozoomer.jugcraft.concordance.rules.Knowledge;
import io.github.jimbozoomer.jugcraft.concordance.rules.ResearchState;
import io.github.jimbozoomer.jugcraft.party.JugcraftParties;
import io.github.jimbozoomer.jugcraft.party.PartyManager;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.spell_engine.api.spell.Spell;
import net.spell_engine.api.spell.registry.SpellRegistry;
import net.spell_engine.internals.SpellExecution;
import net.spell_engine.internals.casting.SpellCast;
import net.spell_engine.internals.casting.SpellCaster;
import net.spell_engine.internals.container.SpellContainerSource;
import net.spell_engine.internals.target.SpellTarget;
import net.spell_power.api.SpellSchools;

/**
 * Roadmap step 10, authored invocations: six invocations, one for each role, load as compositions that fit the
 * Initiate's Wand; research, not the instrument, decides which a player can cast; each is cast through Spell Engine and
 * runs through the shared effect boundary with its own presentation; tunings change numbers within the instrument's
 * limits and are charged by the server's rules, never the item's; Spell Power scales only the damage invocation; and an
 * invocation's cooldown is never shorter than its composition's.
 */
public class ConcordanceInvocationGameTests {
	private static final String FIRST_LIGHT = "jugcraft:first_light";
	private static final String WAND = "jugcraft:initiate_wand";
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

	/** A survival player with the Initiate's Wand, standing on {@code standAt} and facing south (+Z). */
	private static ServerPlayer player(GameTestHelper helper, BlockPos standAt) {
		ServerPlayer player = helper.makeMockServerPlayerInLevel();
		player.setGameMode(GameType.SURVIVAL);
		BlockPos absolute = helper.absolutePos(standAt);
		player.snapTo(absolute.getX() + 0.5, absolute.getY(), absolute.getZ() + 0.5, 0.0F, 0.0F);
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

	private static void learn(ServerPlayer player, ResearchState state) {
		ConcordanceProgress.grant(player, FIRST_LIGHT, state);
		refreshSpells(player);
	}

	private static void command(ServerPlayer player, String command) {
		RateGate.forget(player.getUUID());
		player.level().getServer().getCommands().performPrefixedCommand(player.createCommandSourceStack().withSuppressedOutput(), command);
	}

	/**
	 * First Light's six invocations load, one per role, and every invocation (Ember's too) is a composition that fits the
	 * wand with every tuning; none does what another does; each costs at least its composition; its spell is a
	 * Concordance spell whose one impact is jugcraft:invocation. First Light teaches three at understood and all six at
	 * mastered, at the mastered costs.
	 */
	@GameTest(maxTicks = 20)
	public void sixInvocationsOneForEachRole(GameTestHelper helper) {
		ConcordanceRules rules = ConcordanceData.rules();
		long firstLight = rules.invocations().values().stream().filter(invocation -> invocation.research().equals(FIRST_LIGHT)).count();
		helper.assertTrue(rules.problems().isEmpty() && firstLight == 6, "First Light's six invocations load cleanly: "
				+ rules.invocations().keySet() + " " + rules.problems());
		Set<Definitions.Role> roles = EnumSet.noneOf(Definitions.Role.class);
		Set<String> signatures = new HashSet<>();
		Instrument wand = rules.catalog().instruments().get(WAND);
		for (Definitions.Invocation invocation : rules.invocations().values()) {
			roles.add(invocation.role());
			Authored form = rules.authored(invocation.id(), WAND);
			helper.assertTrue(form != null && form.tunings().keySet().equals(new HashSet<>(invocation.tunings())),
					invocation.id() + " fits the wand with every tuning");
			Plan plan = form.plan();
			helper.assertTrue(signatures.add(plan.signature()), invocation.id() + " does something no other invocation does");
			helper.assertTrue(ConcordanceRules.mayCost(invocation.focus(), invocation.masteredFocus(), plan.focus()),
					invocation.id() + " costs at least its composition");
			for (Plan tuned : form.tunings().values()) {
				helper.assertTrue(tuned.signature().equals(plan.signature()) && tuned.capacity() <= wand.capacity()
						&& tuned.limits().targets() <= wand.targets() && tuned.limits().work() <= wand.work(),
						invocation.id() + " stays itself and inside the wand when tuned: " + tuned.text());
			}
			Holder<Spell> spell = spell(helper.getLevel(), invocation.spell());
			helper.assertTrue(spell.is(ConcordanceSpells.SPELLS), invocation.id() + " is a Concordance spell");
		}
		helper.assertTrue(roles.equals(EnumSet.allOf(Definitions.Role.class)), "Damage, defense, movement, support, investigation and utility: " + roles);

		ServerPlayer player = player(helper, new BlockPos(1, 2, 1));
		learn(player, ResearchState.UNDERSTOOD);
		Map<String, Integer> understood = ConcordanceProgress.knowledge(player).invocations();
		helper.assertTrue(understood.equals(Map.of("jugcraft:kindle", 4, "jugcraft:aegis", 5, "jugcraft:revelation", 3)),
				"Understanding First Light teaches Kindle, Dawn Aegis and Revelation: " + understood);
		learn(player, ResearchState.MASTERED);
		Map<String, Integer> mastered = ConcordanceProgress.knowledge(player).invocations();
		helper.assertTrue(mastered.equals(Map.of("jugcraft:kindle", 3, "jugcraft:aegis", 4, "jugcraft:revelation", 2, "jugcraft:lance", 4,
				"jugcraft:flashstep", 3, "jugcraft:lanternward", 6)), "Mastery teaches the other three, and every cost falls: " + mastered);
		helper.succeed();
	}

	/**
	 * An instrument grants nothing by itself: without research a wand casts nothing, and with First Light understood
	 * the Lance (taught at mastery) is still refused and costs nothing.
	 */
	@GameTest(maxTicks = 20)
	public void anInstrumentGrantsNoSpells(GameTestHelper helper) {
		floor(helper);
		ServerPlayer player = player(helper, new BlockPos(1, 2, 1));
		Mob target = helper.spawnWithNoFreeWill(EntityTypes.VILLAGER, new BlockPos(1, 2, 5));
		refreshSpells(player);
		cast(player, "jugcraft:lance");
		helper.assertTrue(target.getHealth() == target.getMaxHealth() && ConcordanceProgress.currentFocus(player) == FocusPool.MAX,
				"A wand without research casts nothing");
		learn(player, ResearchState.UNDERSTOOD);
		cast(player, "jugcraft:lance");
		helper.assertTrue(target.getHealth() == target.getMaxHealth() && ConcordanceProgress.currentFocus(player) == FocusPool.MAX,
				"Understanding is not mastery: the Lance is still refused");
		helper.succeed();
	}

	/**
	 * Lance of Dawn: arcane damage to the first creature on its line, 5 at no Spell Power; 4 points of arcane Spell
	 * Power above the base add 2 (0.5 each). Armour does not reduce it (Spell Power's damage bypasses armour). Its Focus
	 * is the mastered cost, taken once.
	 */
	@GameTest(maxTicks = 40)
	public void lanceStrikesAndScalesWithSpellPower(GameTestHelper helper) {
		floor(helper);
		ServerPlayer player = player(helper, new BlockPos(1, 2, 1));
		Mob target = helper.spawnWithNoFreeWill(EntityTypes.VILLAGER, new BlockPos(1, 2, 5));
		learn(player, ResearchState.MASTERED);
		float before = target.getHealth();
		cast(player, "jugcraft:lance");
		helper.assertTrue(before - target.getHealth() == 5.0F, "The Lance deals 5: " + before + " -> " + target.getHealth());
		helper.assertTrue(ConcordanceProgress.currentFocus(player) == FocusPool.MAX - 4, "It costs the mastered 4 Focus: "
				+ ConcordanceProgress.currentFocus(player));

		player.getAttribute(SpellSchools.ARCANE.attributeEntry).addTransientModifier(
				new AttributeModifier(Jugcraft.id("test_arcane_power"), 4.0, AttributeModifier.Operation.ADD_VALUE));
		helper.assertTrue(Invocations.powerAboveBase(player, "spell_power:arcane") == 4.0, "4 arcane Spell Power above the base: "
				+ Invocations.powerAboveBase(player, "spell_power:arcane"));
		helper.runAfterDelay(HURT_IMMUNITY_TICKS, () -> {
			float healthy = target.getHealth();
			cast(player, "jugcraft:lance");
			helper.assertTrue(healthy - target.getHealth() == 7.0F, "With it the Lance deals 7: " + healthy + " -> " + target.getHealth());
			helper.succeed();
		});
	}

	/**
	 * Dawn Aegis shields its caster (8 absorption for 10 seconds). Tuned with Prolong by command, it lasts 20 seconds
	 * and costs one more Focus; Intensify needs mastery and is refused; a modifier it does not offer is refused.
	 */
	@GameTest(maxTicks = 20)
	public void aegisShieldsAndIsTuned(GameTestHelper helper) {
		ServerPlayer player = player(helper, new BlockPos(1, 2, 1));
		learn(player, ResearchState.UNDERSTOOD);
		cast(player, "jugcraft:aegis");
		MobEffectInstance shell = player.getEffect(MobEffects.ABSORPTION);
		helper.assertTrue(shell != null && shell.getAmplifier() == 1 && shell.getDuration() == 200
				&& ConcordanceProgress.currentFocus(player) == FocusPool.MAX - 5, "Dawn Aegis gives 8 absorption for 10 s for 5 Focus: " + shell);

		command(player, "jugcraft concordance tune jugcraft:aegis jugcraft:intensify");
		helper.assertTrue(Invocations.tunings(player.getMainHandItem()).get("jugcraft:aegis") == null, "Intensify needs mastery");
		command(player, "jugcraft concordance tune jugcraft:aegis jugcraft:widen");
		helper.assertTrue(Invocations.tunings(player.getMainHandItem()).get("jugcraft:aegis") == null, "Aegis does not offer Widen");
		command(player, "jugcraft concordance tune jugcraft:aegis jugcraft:prolong");
		Tunings.Tuning tuning = Invocations.tunings(player.getMainHandItem()).get("jugcraft:aegis");
		helper.assertTrue(tuning != null && tuning.modifier().equals("jugcraft:prolong") && tuning.focus() == 1,
				"Prolong is set on the wand at +1 Focus: " + tuning);

		player.removeEffect(MobEffects.ABSORPTION);
		ConcordanceProgress.setFocus(player, FocusPool.MAX);
		cast(player, "jugcraft:aegis");
		shell = player.getEffect(MobEffects.ABSORPTION);
		helper.assertTrue(shell != null && shell.getAmplifier() == 1 && shell.getDuration() == 400
				&& ConcordanceProgress.currentFocus(player) == FocusPool.MAX - 6, "Prolonged, it lasts 20 s for 6 Focus: " + shell + " "
				+ ConcordanceProgress.currentFocus(player));

		command(player, "jugcraft concordance tune jugcraft:aegis clear");
		helper.assertTrue(Invocations.tunings(player.getMainHandItem()).get("jugcraft:aegis") == null, "and the tuning can be cleared");
		helper.succeed();
	}

	/**
	 * The item's record of a tuning is never trusted: a forged tuning claiming Intensify costs nothing extra is
	 * charged the server's +2, and one naming a modifier the Lance does not offer is ignored.
	 */
	@GameTest(maxTicks = 40)
	public void forgedTuningsAreChargedByTheServer(GameTestHelper helper) {
		floor(helper);
		ServerPlayer player = player(helper, new BlockPos(1, 2, 1));
		Mob target = helper.spawnWithNoFreeWill(EntityTypes.VILLAGER, new BlockPos(1, 2, 5));
		learn(player, ResearchState.MASTERED);
		ItemStack wand = player.getMainHandItem();
		wand.set(JugcraftConcordance.TUNINGS, Tunings.EMPTY.with("jugcraft:lance", new Tunings.Tuning("jugcraft:intensify", 0)));
		float before = target.getHealth();
		cast(player, "jugcraft:lance");
		helper.assertTrue(before - target.getHealth() == 7.0F && ConcordanceProgress.currentFocus(player) == FocusPool.MAX - 6,
				"Intensified it deals 7 and costs the real 4 + 2: " + (before - target.getHealth()) + ", "
						+ ConcordanceProgress.currentFocus(player));
		wand.set(JugcraftConcordance.TUNINGS, Tunings.EMPTY.with("jugcraft:lance", new Tunings.Tuning("jugcraft:widen", 0)));
		helper.runAfterDelay(HURT_IMMUNITY_TICKS, () -> {
			ConcordanceProgress.setFocus(player, FocusPool.MAX);
			float healthy = target.getHealth();
			cast(player, "jugcraft:lance");
			helper.assertTrue(healthy - target.getHealth() == 5.0F && ConcordanceProgress.currentFocus(player) == FocusPool.MAX - 4,
					"A modifier the Lance does not offer is ignored: " + (healthy - target.getHealth()) + ", "
							+ ConcordanceProgress.currentFocus(player));
			helper.succeed();
		});
	}

	/**
	 * A cooldown is never shorter than the composition's: Kindle tuned with Extend costs 5 Focus as a composition, so
	 * its cooldown is 35 ticks although Spell Engine's own is 30.
	 */
	@GameTest(maxTicks = 20)
	public void cooldownIsNeverShorterThanTheComposition(GameTestHelper helper) {
		floor(helper);
		helper.setBlock(new BlockPos(1, 2, 5), Blocks.STONE);
		helper.setBlock(new BlockPos(1, 3, 5), Blocks.STONE);
		ServerPlayer player = player(helper, new BlockPos(1, 2, 1));
		learn(player, ResearchState.MASTERED);
		player.getMainHandItem().set(JugcraftConcordance.TUNINGS, Tunings.EMPTY.with("jugcraft:kindle", new Tunings.Tuning("jugcraft:extend", 1)));
		Plan tuned = ConcordanceData.rules().authored("jugcraft:kindle", WAND).plan("jugcraft:extend");
		cast(player, "jugcraft:kindle");
		int left = ((SpellCaster.Player) player).getCooldownManager().getCooldownDuration(spell(helper.getLevel(), "jugcraft:kindle"));
		helper.assertTrue(helper.getBlockState(new BlockPos(1, 3, 4)).is(JugcraftConcordance.LUMEN_MOTE), "Kindle lit the wall's open side");
		helper.assertTrue(tuned.cooldown() == 35 && left == 35 && ConcordanceProgress.currentFocus(player) == FocusPool.MAX - 4,
				"Extended Kindle cools down for its composition's 35 ticks and costs 3 + 1: " + left);
		helper.succeed();
	}

	/** Flashstep carries its caster forward, the way they face, at 1.2 blocks a tick. */
	@GameTest(maxTicks = 20)
	public void flashstepCarriesForward(GameTestHelper helper) {
		ServerPlayer player = player(helper, new BlockPos(1, 2, 1));
		learn(player, ResearchState.MASTERED);
		player.setDeltaMovement(0.0, 0.0, 0.0);
		cast(player, "jugcraft:flashstep");
		var motion = player.getDeltaMovement();
		helper.assertTrue(Math.abs(motion.z - 1.2) < 1.0E-6 && Math.abs(motion.x) < 1.0E-6 && motion.y > 0.0
				&& ConcordanceProgress.currentFocus(player) == FocusPool.MAX - 3, "Flashstep pushes south at 1.2: " + motion);
		helper.succeed();
	}

	/**
	 * Revelation makes the creatures round its caster glow, at most the six nearest whatever the crowd, under a ledger
	 * of six targets and six work: the invocation runs inside the same limits as a composed spell.
	 */
	@GameTest(maxTicks = 40)
	public void revelationStaysInsideItsLimits(GameTestHelper helper) {
		floor(helper);
		// In the middle of the structure, so its six-block sweep stays clear of the tests beside it.
		ServerPlayer player = player(helper, new BlockPos(4, 2, 4));
		learn(player, ResearchState.UNDERSTOOD);
		List<Mob> crowd = new ArrayList<>();
		for (BlockPos pos : List.of(new BlockPos(2, 2, 4), new BlockPos(6, 2, 4), new BlockPos(4, 2, 2), new BlockPos(4, 2, 6),
				new BlockPos(2, 2, 2), new BlockPos(6, 2, 6), new BlockPos(2, 2, 6), new BlockPos(6, 2, 2))) {
			crowd.add(helper.spawnWithNoFreeWill(EntityTypes.VILLAGER, pos));
		}
		helper.runAfterDelay(SETTLE_TICKS, () -> {
			Plan plan = ConcordanceData.rules().authored("jugcraft:revelation", WAND).plan();
			Ledger ledger = new Ledger(plan.limits());
			ComposedSpells.Outcome outcome = ComposedSpells.perform(helper.getLevel(), player, plan,
					Cause.of(player.getUUID(), Cause.Origin.INVOCATION, "jugcraft:revelation", ConcordanceEffects.nextSerial()), ledger);
			long glowing = crowd.stream().filter(mob -> mob.hasEffect(MobEffects.GLOWING)).count();
			helper.assertTrue(outcome.applied() && glowing == 6 && ledger.targets() == 6 && ledger.work() == 6
					&& !player.hasEffect(MobEffects.GLOWING), "Six of eight glow, under six targets and six work: " + glowing + " "
					+ ledger.targets() + "/" + ledger.work());
			// Cast through Spell Engine it costs its 3 Focus once.
			crowd.forEach(LivingEntity::removeAllEffects);
			cast(player, "jugcraft:revelation");
			helper.assertTrue(crowd.stream().filter(mob -> mob.hasEffect(MobEffects.GLOWING)).count() == 6
					&& ConcordanceProgress.currentFocus(player) == FocusPool.MAX - 3, "Cast, it reveals six for 3 Focus");
			helper.succeed();
		});
	}

	/**
	 * Lanternward mends and wards its caster and their party within four blocks, and nobody else: not a stranger
	 * standing as close, nor a villager.
	 */
	@GameTest(maxTicks = 20)
	public void lanternwardWardsOnlyTheParty(GameTestHelper helper) {
		floor(helper);
		ServerPlayer caster = player(helper, new BlockPos(1, 2, 1));
		ServerPlayer friend = player(helper, new BlockPos(3, 2, 1));
		ServerPlayer stranger = player(helper, new BlockPos(1, 2, 3));
		Mob villager = helper.spawnWithNoFreeWill(EntityTypes.VILLAGER, new BlockPos(2, 2, 3));
		PartyManager parties = JugcraftParties.manager();
		long now = System.currentTimeMillis();
		try {
			helper.assertTrue(parties.invite(caster.getUUID(), friend.getUUID(), now) == PartyManager.Result.OK
					&& parties.accept(friend.getUUID(), now) == PartyManager.Result.OK, "The caster and their friend form a party");
			learn(caster, ResearchState.MASTERED);
			for (LivingEntity each : List.of(caster, friend, stranger, villager)) {
				each.setHealth(10.0F);
			}
			cast(caster, "jugcraft:lanternward");
			for (LivingEntity warded : List.of(caster, friend)) {
				MobEffectInstance shell = warded.getEffect(MobEffects.ABSORPTION);
				helper.assertTrue(warded.getHealth() == 14.0F && shell != null && shell.getAmplifier() == 0,
						warded.getName().getString() + " is mended by 4 and warded: " + warded.getHealth() + " " + shell);
			}
			for (LivingEntity other : List.of(stranger, villager)) {
				helper.assertTrue(other.getHealth() == 10.0F && !other.hasEffect(MobEffects.ABSORPTION),
						"Lanternward passes over " + other.getName().getString());
			}
			helper.assertTrue(ConcordanceProgress.currentFocus(caster) == FocusPool.MAX - 6, "It costs the mastered 6 Focus");
		} finally {
			parties.adminDisband(caster.getUUID());
		}
		helper.succeed();
	}

	/**
	 * Players cannot compose with an invocation's own words, so an invocation is no back door to more than the
	 * grammar allows: the Lance's beam, written by a master, is refused.
	 */
	@GameTest(maxTicks = 20)
	public void authoredWordsAreNotComposable(GameTestHelper helper) {
		Instrument wand = ConcordanceData.rules().catalog().instruments().get(WAND);
		Compiler.Compilation forged = Compiler.compile("ray struck lance_beam", ConcordanceData.rules().catalog(),
				research -> ResearchState.MASTERED, wand);
		helper.assertTrue(!forged.ok() && forged.problems().stream().anyMatch(text -> text.key().equals("problem.authored")),
				"lance_beam belongs to the Lance: " + forged.problems());
		Knowledge none = Knowledge.EMPTY;
		helper.assertTrue(none.invocationCost("jugcraft:lance") < 0, "and knowing nothing teaches nothing");
		helper.succeed();
	}
}
