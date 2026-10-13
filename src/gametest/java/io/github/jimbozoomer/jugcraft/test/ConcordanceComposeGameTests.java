package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.concordance.ComposedSpells;
import io.github.jimbozoomer.jugcraft.concordance.ConcordanceData;
import io.github.jimbozoomer.jugcraft.concordance.ConcordanceEffects;
import io.github.jimbozoomer.jugcraft.concordance.ConcordanceProgress;
import io.github.jimbozoomer.jugcraft.concordance.ConcordanceSpells;
import io.github.jimbozoomer.jugcraft.concordance.Inscription;
import io.github.jimbozoomer.jugcraft.concordance.JugcraftConcordance;
import io.github.jimbozoomer.jugcraft.concordance.RateGate;
import io.github.jimbozoomer.jugcraft.concordance.compose.Catalog;
import io.github.jimbozoomer.jugcraft.concordance.compose.Compiler;
import io.github.jimbozoomer.jugcraft.concordance.compose.Instrument;
import io.github.jimbozoomer.jugcraft.concordance.compose.Plan;
import io.github.jimbozoomer.jugcraft.concordance.compose.Text;
import io.github.jimbozoomer.jugcraft.concordance.effect.Cause;
import io.github.jimbozoomer.jugcraft.concordance.effect.Ledger;
import io.github.jimbozoomer.jugcraft.concordance.rules.FocusPool;
import io.github.jimbozoomer.jugcraft.concordance.rules.ResearchState;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.Mob;
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

/**
 * Roadmap step 8, the spell compiler: the grammar loads from data; compositions compile to the numbers the codex
 * states, or name their exact problem; a spell is composed and inscribed by command, cast through Spell Engine's
 * carrier spell and settled once; the server compiles the inscription again (a forged cost is ignored); and a running
 * spell stays inside its compiled limits however many creatures it meets.
 */
public class ConcordanceComposeGameTests {
	private static final String FIRST_LIGHT = "jugcraft:first_light";
	/**
	 * Ticks to let newly spawned creatures settle before a spell searches for them: tests run in freshly loaded chunks,
	 * where an area search may not see a creature in its first tick (a reference to it always does).
	 */
	private static final int SETTLE_TICKS = 10;

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
		// A living entity looks where its head turns; a mock player's head is not turned by snapTo.
		player.setYHeadRot(0.0F);
		player.setYBodyRot(0.0F);
		player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(JugcraftConcordance.INITIATE_WAND));
		RateGate.forget(player.getUUID());
		return player;
	}

	private static Compiler.Knows knows(ResearchState state) {
		return research -> FIRST_LIGHT.equals(research) ? state : ResearchState.NONE;
	}

	private static Instrument wand() {
		return ConcordanceData.rules().catalog().instrumentFor("jugcraft:initiate_wand");
	}

	private static boolean names(Compiler.Compilation compiled, String key) {
		return !compiled.ok() && compiled.problems().stream().map(Text::key).anyMatch(key::equals);
	}

	private static Holder<Spell> composed(ServerLevel level) {
		return SpellRegistry.from(level).get(Identifier.parse(ComposedSpells.SPELL)).orElseThrow();
	}

	private static void refreshSpells(ServerPlayer player) {
		SpellContainerSource.setDirty(player, ConcordanceSpells.SOURCE);
		SpellContainerSource.setDirty(player, "main_hand");
		SpellContainerSource.update(player);
	}

	private static void cast(ServerLevel level, ServerPlayer player) {
		SpellExecution.performSpell(level, player, composed(level), SpellTarget.SearchResult.empty(), SpellCast.Action.RELEASE, 1.0F);
	}

	private static void command(ServerPlayer player, String command) {
		player.level().getServer().getCommands().performPrefixedCommand(player.createCommandSourceStack().withSuppressedOutput(), command);
	}

	/** The grammar loads whole, the codex examples cost what the codex says, and invalid spells name their problem. */
	@GameTest(maxTicks = 20)
	public void grammarLoadsAndNamesProblems(GameTestHelper helper) {
		Catalog catalog = ConcordanceData.rules().catalog();
		Instrument wand = wand();
		// First Light's grammar; later research adds its own words (Hearthbinding's five, all its invocations' own).
		long firstLight = catalog.components().values().stream()
				.filter(component -> component.requires().research().equals("jugcraft:first_light")).count();
		helper.assertTrue(firstLight == 23 && wand != null && wand.capacity() == 8 && wand.targets() == 6
				&& wand.work() == 48 && wand.branches() == 1 && wand.duration() == 1200,
				"First Light's 23 components (6 of them invocations' own) and the Initiate's Wand load: " + catalog.components().keySet() + " "
						+ wand);
		Object[][] examples = {
			{"touch struck light", ResearchState.UNDERSTOOD, 2, 2, 20},
			{"here struck ward", ResearchState.UNDERSTOOD, 3, 3, 25},
			{"ray struck sear then here creatures dazzle", ResearchState.MASTERED, 8, 8, 50},
		};
		for (Object[] example : examples) {
			Compiler.Compilation compiled = Compiler.compile((String) example[0], catalog, knows((ResearchState) example[1]), wand);
			Plan plan = compiled.plan();
			helper.assertTrue(plan != null && plan.focus() == (int) example[2] && plan.capacity() == (int) example[3]
					&& plan.cooldown() == (int) example[4], example[0] + ": Focus, capacity and cooldown as the codex says: "
					+ (plan == null ? compiled.problems() : plan.focus() + "/" + plan.capacity() + "/" + plan.cooldown()));
		}
		helper.assertTrue(names(Compiler.compile("ray struck sear", catalog, knows(ResearchState.UNDERSTOOD), wand), "problem.research"),
				"Ray and Sear need First Light mastered");
		helper.assertTrue(names(Compiler.compile("touch creatures light", catalog, knows(ResearchState.MASTERED), wand), "problem.selection"),
				"Light acts on blocks, which creatures does not choose");
		helper.assertTrue(names(Compiler.compile("here struck sear", catalog, knows(ResearchState.MASTERED), wand), "problem.harms_caster"),
				"Here struck sear would harm the caster");
		helper.assertTrue(names(Compiler.compile("ray+extend creatures sear+intensify pulse", catalog, knows(ResearchState.MASTERED), wand),
				"problem.capacity"), "Too much for the wand");
		helper.assertTrue(names(Compiler.compile("touch struck light+prolong", catalog, knows(ResearchState.MASTERED), wand), "problem.duration"),
				"Prolonged light outlasts what the wand allows");
		helper.assertTrue(names(Compiler.compile("struck touch light", catalog, knows(ResearchState.MASTERED), wand), "problem.expected"),
				"Words out of order");
		helper.succeed();
	}

	/**
	 * Compose by command, inscribe on the wand, cast through Spell Engine: the light is set, the plan's Focus taken once
	 * and its cooldown started. A spell that finds nothing to act on costs nothing; a forged cost on the item is ignored
	 * (the server compiles the text again); a spell the caster's research no longer allows is refused.
	 */
	@GameTest(maxTicks = 40)
	public void inscribeAndCastThroughSpellEngine(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		floor(helper);
		helper.setBlock(new BlockPos(1, 2, 4), Blocks.STONE);
		helper.setBlock(new BlockPos(1, 3, 4), Blocks.STONE);
		ServerPlayer player = player(helper, new BlockPos(1, 2, 1));
		command(player, "jugcraft concordance compose inscribe touch struck light");
		helper.assertTrue(ComposedSpells.inscription(player.getMainHandItem()) == null, "Without First Light nothing can be inscribed");

		ConcordanceProgress.grant(player, FIRST_LIGHT, ResearchState.UNDERSTOOD);
		RateGate.forget(player.getUUID());
		command(player, "jugcraft concordance compose inscribe touch struck light");
		Inscription inscription = ComposedSpells.inscription(player.getMainHandItem());
		helper.assertTrue(inscription != null && inscription.text().equals("touch struck light") && inscription.focus() == 2
				&& inscription.cooldown() == 20, "The wand carries the spell, its Focus and cooldown: " + inscription);

		refreshSpells(player);
		cast(level, player);
		BlockPos lit = helper.absolutePos(new BlockPos(1, 3, 3));
		helper.assertTrue(level.getBlockState(lit).is(JugcraftConcordance.LUMEN_MOTE), "Touch struck light lights the open side of the wall");
		helper.assertTrue(ConcordanceProgress.currentFocus(player) == FocusPool.MAX - 2, "It takes its 2 Focus once: "
				+ ConcordanceProgress.currentFocus(player));
		SpellCaster.Player caster = (SpellCaster.Player) player;
		helper.assertTrue(caster.getCooldownManager().isCoolingDown(composed(level)), "and starts its cooldown");

		// Nothing to act on: no cost, no cooldown.
		caster.getCooldownManager().reset(null);
		RateGate.forget(player.getUUID());
		command(player, "jugcraft concordance compose inscribe touch struck reveal");
		ConcordanceProgress.setFocus(player, FocusPool.MAX);
		refreshSpells(player);
		cast(level, player);
		helper.assertTrue(ConcordanceProgress.currentFocus(player) == FocusPool.MAX && !caster.getCooldownManager().isCoolingDown(composed(level)),
				"Revealing nothing costs nothing and starts no cooldown");

		// A forged inscription: the server compiles the text and charges what it really costs. (A villager is tall
		// enough to meet the line from the caster's eyes.)
		ConcordanceProgress.grant(player, FIRST_LIGHT, ResearchState.MASTERED);
		Mob villager = helper.spawnWithNoFreeWill(EntityTypes.VILLAGER, new BlockPos(1, 2, 3));
		helper.runAfterDelay(SETTLE_TICKS, () -> {
			ConcordanceProgress.setFocus(player, FocusPool.MAX);
			player.getMainHandItem().set(JugcraftConcordance.INSCRIPTION, new Inscription("touch struck sear", 1, 0));
			refreshSpells(player);
			cast(level, player);
			helper.assertTrue(villager.getHealth() == villager.getMaxHealth() - 4.0F && ConcordanceProgress.currentFocus(player) == FocusPool.MAX - 3,
					"A forged Focus cost is ignored: Sear costs its 3: " + ConcordanceProgress.currentFocus(player) + ", villager " + villager.getHealth());

			// Research taken away: the same inscription no longer compiles, and the cast is refused.
			caster.getCooldownManager().reset(null);
			ConcordanceProgress.reset(player);
			ConcordanceProgress.grant(player, FIRST_LIGHT, ResearchState.UNDERSTOOD);
			ConcordanceProgress.setFocus(player, FocusPool.MAX);
			float before = villager.getHealth();
			refreshSpells(player);
			cast(level, player);
			helper.assertTrue(villager.getHealth() == before && ConcordanceProgress.currentFocus(player) == FocusPool.MAX,
					"Without Sear's research the inscription does nothing and costs nothing");
			RateGate.forget(player.getUUID());
			command(player, "jugcraft concordance compose clear");
			helper.assertTrue(ComposedSpells.inscription(player.getMainHandItem()) == null, "compose clear wipes it");
			helper.succeed();
		});
	}

	/**
	 * A spell that pulses among ten pigs reaches only as many as it compiled to (four), whatever wanders in later, and
	 * never spends more work than compiled.
	 */
	@GameTest(maxTicks = 100)
	public void compiledLimitsHoldAtRuntime(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		floor(helper);
		ServerPlayer player = player(helper, new BlockPos(3, 2, 0));
		ConcordanceProgress.grant(player, FIRST_LIGHT, ResearchState.MASTERED);
		List<Mob> pigs = new ArrayList<>();
		for (int i = 0; i < 10; i++) {
			pigs.add(helper.spawnWithNoFreeWill(EntityTypes.PIG, new BlockPos(1 + i % 5, 2, 2 + i / 5)));
		}
		Compiler.Compilation compiled = ComposedSpells.compile(player, wand(), "here creatures dazzle pulse");
		Plan plan = compiled.plan();
		helper.assertTrue(plan != null && plan.limits().targets() == 4 && plan.limits().work() == 12, "Compiled: 4 targets, 12 work: "
				+ (plan == null ? compiled.problems() : plan.limits()));
		helper.runAfterDelay(SETTLE_TICKS, () -> pulseAmongPigs(helper, level, player, plan, pigs));
	}

	private static void pulseAmongPigs(GameTestHelper helper, ServerLevel level, ServerPlayer player, Plan plan, List<Mob> pigs) {
		Ledger ledger = new Ledger(plan.limits());
		boolean applied = ComposedSpells.cast(level, player, plan, Cause.of(player.getUUID(), Cause.Origin.SPELL, ComposedSpells.SPELL,
				ConcordanceEffects.nextSerial()), ledger);
		helper.assertTrue(applied && pigs.stream().filter(pig -> pig.hasEffect(MobEffects.SLOWNESS)).count() == 4
				&& !player.hasEffect(MobEffects.SLOWNESS) && ComposedSpells.lingering(player.getUUID()) == 1,
				"The first pulse dazzles the four nearest pigs, never the caster, and two more pulses wait: "
						+ pigs.stream().filter(pig -> pig.hasEffect(MobEffects.SLOWNESS)).count() + " dazzled");
		// Lift the dazzled pigs out of reach: later pulses meet new pigs, but the spell has spent its four targets.
		List<Mob> first = pigs.stream().filter(pig -> pig.hasEffect(MobEffects.SLOWNESS)).toList();
		for (Mob pig : first) {
			pig.setNoGravity(true);
			pig.teleportTo(pig.getX(), pig.getY() + 30.0, pig.getZ());
		}
		helper.runAfterDelay(50, () -> {
			helper.assertTrue(pigs.stream().filter(pig -> !first.contains(pig)).noneMatch(pig -> pig.hasEffect(MobEffects.SLOWNESS)),
					"No fifth pig is ever dazzled");
			helper.assertTrue(ledger.targets() == 4 && ledger.work() <= plan.limits().work() && ComposedSpells.lingering(player.getUUID()) == 0,
					"The ledger stayed within the plan: " + ledger.targets() + " targets, " + ledger.work() + " work; nothing lingers");
			helper.succeed();
		});
	}

	/** A branch starts where its spell landed, shares its ledger and is still the caster's. */
	@GameTest(maxTicks = 40)
	public void branchesKeepTheirCause(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		floor(helper);
		ServerPlayer player = player(helper, new BlockPos(1, 2, 0));
		ConcordanceProgress.grant(player, FIRST_LIGHT, ResearchState.MASTERED);
		// Close in front, as in the inscription test: a villager is tall enough to meet the line from the eyes.
		Mob target = helper.spawnWithNoFreeWill(EntityTypes.VILLAGER, new BlockPos(1, 2, 2));
		Mob beside = helper.spawnWithNoFreeWill(EntityTypes.VILLAGER, new BlockPos(2, 2, 3));
		Compiler.Compilation compiled = ComposedSpells.compile(player, wand(), "touch struck sear then here creatures dazzle");
		Plan plan = compiled.plan();
		helper.assertTrue(plan != null && plan.limits().branches() == 1 && plan.limits().targets() == 5, "Compiled: one branch, five targets: "
				+ (plan == null ? compiled.problems() : plan.limits()));
		helper.runAfterDelay(SETTLE_TICKS, () -> {
			ComposedSpells.Impact landing = ComposedSpells.deliver(level, player, plan.root(), null);
			Ledger ledger = new Ledger(plan.limits());
			helper.assertTrue(ComposedSpells.cast(level, player, plan, Cause.of(player.getUUID(), Cause.Origin.SPELL, ComposedSpells.SPELL,
					ConcordanceEffects.nextSerial()), ledger), "It takes effect: the touch struck " + landing.struck() + " (the villager is "
					+ target.getUUID() + ") at " + landing.point() + "; looking " + player.getViewVector(1.0F));
			helper.assertTrue(target.getLastHurtByMob() == player && target.getHealth() == target.getMaxHealth() - 4.0F,
					"Sear strikes the villager touched, credited to the caster");
			helper.assertTrue(target.hasEffect(MobEffects.SLOWNESS) && beside.hasEffect(MobEffects.SLOWNESS) && !player.hasEffect(MobEffects.SLOWNESS),
					"then the branch dazzles the creatures where it landed, never the caster");
			helper.assertTrue(ledger.branches() == 1 && ledger.targets() <= plan.limits().targets() && ledger.work() <= plan.limits().work(),
					"one branch taken, within the plan's limits");
			helper.succeed();
		});
	}
}
