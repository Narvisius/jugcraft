package io.github.jimbozoomer.jugcraft.test;

import com.mojang.serialization.JsonOps;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.agriculture.PreserveJarItem;
import io.github.jimbozoomer.jugcraft.compat.jade.ConcordanceDataProvider;
import io.github.jimbozoomer.jugcraft.concordance.Alchemy;
import io.github.jimbozoomer.jugcraft.concordance.ConcordanceCodecs;
import io.github.jimbozoomer.jugcraft.concordance.ConcordanceData;
import io.github.jimbozoomer.jugcraft.concordance.ConcordanceEffects;
import io.github.jimbozoomer.jugcraft.concordance.ConcordanceProgress;
import io.github.jimbozoomer.jugcraft.concordance.ConcordanceSpells;
import io.github.jimbozoomer.jugcraft.concordance.CrucibleBlockEntity;
import io.github.jimbozoomer.jugcraft.concordance.Examination;
import io.github.jimbozoomer.jugcraft.concordance.JugcraftConcordance;
import io.github.jimbozoomer.jugcraft.concordance.RateGate;
import io.github.jimbozoomer.jugcraft.concordance.Rituals;
import io.github.jimbozoomer.jugcraft.concordance.alchemy.Axis;
import io.github.jimbozoomer.jugcraft.concordance.alchemy.Band;
import io.github.jimbozoomer.jugcraft.concordance.alchemy.Vector;
import io.github.jimbozoomer.jugcraft.concordance.assay.Assaying;
import io.github.jimbozoomer.jugcraft.concordance.effect.Cause;
import io.github.jimbozoomer.jugcraft.concordance.effect.EffectSpec;
import io.github.jimbozoomer.jugcraft.concordance.effect.Ledger;
import io.github.jimbozoomer.jugcraft.concordance.rime.Rime;
import io.github.jimbozoomer.jugcraft.concordance.rime.RimeSurvey;
import io.github.jimbozoomer.jugcraft.concordance.rules.ConcordanceRules;
import io.github.jimbozoomer.jugcraft.concordance.rules.Definitions;
import io.github.jimbozoomer.jugcraft.concordance.rules.Evidence;
import io.github.jimbozoomer.jugcraft.concordance.rules.FocusPool;
import io.github.jimbozoomer.jugcraft.concordance.rules.Knowledge;
import io.github.jimbozoomer.jugcraft.concordance.rules.ResearchEngine;
import io.github.jimbozoomer.jugcraft.concordance.rules.ResearchState;
import io.github.jimbozoomer.jugcraft.concordance.strata.Brace;
import io.github.jimbozoomer.jugcraft.concordance.strata.Strata;
import io.github.jimbozoomer.jugcraft.config.JugcraftConfig;
import io.github.jimbozoomer.jugcraft.lair.Lair;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.spell_engine.api.spell.Spell;
import net.spell_engine.api.spell.registry.SpellRegistry;
import net.spell_engine.internals.SpellExecution;
import net.spell_engine.internals.casting.SpellCast;
import net.spell_engine.internals.casting.SpellCaster;
import net.spell_engine.internals.container.SpellContainerSource;
import net.spell_engine.internals.target.SpellTarget;

/**
 * Concordance Chunk B, unit 3 (docs/features/arcane-concordance-rime-strata.md): Rimekeeping and Stratawrighting, after
 * First Light, and what they teach.
 * <ul>
 * <li>Rimekeeping begins with snow or ice and is observed against fire, in either order; it is understood at the bench,
 * by arriving in the Glacier Hall or by bottling a Rime brew by hand, and mastered by three kinds of cold work, each
 * counted once. It teaches Rimebind and Preserve.</li>
 * <li>Stratawrighting begins with natural stone or raw Thallite (the same examination begins Assay) and needs no Thallite
 * at all; it is mastered by three ways of anchoring, with an ally, a ritual and a blow taken in Earthbound Thallite as
 * electives. It teaches Brace.</li>
 * <li>Preserve holds an Alembic Crucible in its band for a minute (restarted, never added to, by a recast) and nothing
 * else: no inventory, no food, no machine. It is paid once and counts once.</li>
 * <li>What is learned survives a save, and an older knowledge relearns none of it.</li>
 * </ul>
 * The boundary's side of Rimebind and Brace (thaw, tolerance, spending, saves) is in {@link ConcordanceEffectGameTests}.
 */
public class ConcordanceRimeStrataGameTests {
	private static final String FIRST_LIGHT = "jugcraft:first_light";
	private static final String WAND = "jugcraft:initiate_wand";
	private static final String BENCH = "jugcraft:lampwright_bench";
	private static final List<String> RIME_SPELLS = List.of("jugcraft:rimebind", "jugcraft:preserve");
	private static final List<String> STRATA_SPELLS = List.of("jugcraft:brace");
	/** A new player cannot be hurt until its client has loaded the world, or for 60 ticks (see LairEncounterGameTests). */
	private static final int LOADING_TICKS = 80;
	/**
	 * From (1, 2, 1) facing south, the eye (1.62 up) looks down to a point 0.4 above the foot of the block two south,
	 * just inside its north face: Preserve's touch meets a crucible, a chest or a furnace standing at (1, 2, 3).
	 */
	private static final float AIM = (float) Math.toDegrees(Math.atan2(1.62 - 0.4, 1.56));

	private static void floor(GameTestHelper helper) {
		for (int x = 0; x <= 7; x++) {
			for (int z = 0; z <= 7; z++) {
				helper.setBlock(new BlockPos(x, 1, z), Blocks.STONE);
			}
		}
	}

	/** A survival player with the Initiate's Wand on {@code standAt}, facing south (+Z), looking down {@code pitch} degrees. */
	private static ServerPlayer player(GameTestHelper helper, BlockPos standAt, float pitch) {
		ServerPlayer player = TestPlayers.mock(helper);
		player.setGameMode(GameType.SURVIVAL);
		BlockPos absolute = helper.absolutePos(standAt);
		player.snapTo(absolute.getX() + 0.5, absolute.getY(), absolute.getZ() + 0.5, 0.0F, pitch);
		player.setYHeadRot(0.0F);
		player.setYBodyRot(0.0F);
		player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(JugcraftConcordance.INITIATE_WAND));
		RateGate.forget(player.getUUID());
		return player;
	}

	/** Moves {@code player} onto the block above {@code standAt}'s floor, standing on it. */
	private static void stand(GameTestHelper helper, ServerPlayer player, BlockPos standAt) {
		BlockPos absolute = helper.absolutePos(standAt);
		player.snapTo(absolute.getX() + 0.5, absolute.getY(), absolute.getZ() + 0.5, 0.0F, 0.0F);
		player.setOnGround(true);
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

	/** First Light understood (every entry's prerequisite), then {@code research} at {@code state}. */
	private static void learn(ServerPlayer player, String research, ResearchState state) {
		ConcordanceProgress.grant(player, FIRST_LIGHT, ResearchState.UNDERSTOOD);
		ConcordanceProgress.grant(player, research, state);
		refreshSpells(player);
	}

	private static ResearchState state(ServerPlayer player, String research) {
		return ConcordanceProgress.knowledge(player).state(research);
	}

	private static Map<String, Long> evidence(ServerPlayer player, String research) {
		return ConcordanceProgress.knowledge(player).progress(research).evidence();
	}

	/** The practice keys {@code player} holds for {@code activity} under {@code research}, in order. */
	private static List<String> practised(ServerPlayer player, String research, String activity) {
		return evidence(player, research).keySet().stream().filter(key -> key.startsWith("practice:" + activity + ":")).toList();
	}

	private static String key(String activity, String key) {
		return "practice:" + activity + ":" + key;
	}

	/** Sneak-use examination of {@code item}, with the half-second rest forgotten. */
	private static ResearchEngine.Result examine(ServerPlayer player, Item item) {
		Examination.forget(player.getUUID());
		return Examination.examine(player, player.level(), new ItemStack(item));
	}

	/** The first effect of an invocation's composed form on the Initiate's Wand: what its cast applies. */
	private static EffectSpec invoked(String id) {
		return ConcordanceData.rules().authored(id, WAND).plan().root().steps().getFirst().effect();
	}

	private static ConcordanceEffects.Context context(GameTestHelper helper, ServerPlayer actor) {
		Cause cause = Cause.of(actor.getUUID(), Cause.Origin.INVOCATION, "test:rime_strata", ConcordanceEffects.nextSerial());
		return new ConcordanceEffects.Context(helper.getLevel(), cause, actor, new Ledger(new Ledger.Limits(16, 256, 0)), "0/0/0",
				actor.position());
	}

	/** {@code spec} applied to {@code target} through the effect boundary, as {@code actor}'s own new event. */
	private static ConcordanceEffects.Result apply(GameTestHelper helper, ServerPlayer actor, EffectSpec spec, LivingEntity target) {
		return ConcordanceEffects.apply(context(helper, actor), spec, target);
	}

	private static ConcordanceEffects.Result apply(GameTestHelper helper, ServerPlayer actor, EffectSpec spec, BlockPos at) {
		return ConcordanceEffects.apply(context(helper, actor), spec, helper.absolutePos(at));
	}

	private static CrucibleBlockEntity crucible(GameTestHelper helper, BlockPos at, Block below) {
		helper.setBlock(at.below(), below);
		helper.setBlock(at, JugcraftConcordance.CRUCIBLE);
		return (CrucibleBlockEntity) helper.getLevel().getBlockEntity(helper.absolutePos(at));
	}

	/** Someone who understands First Light and the Alembic Arts, so may work a crucible by hand. */
	private static ServerPlayer alembist(GameTestHelper helper, BlockPos standAt) {
		ServerPlayer player = player(helper, standAt, 0.0F);
		ConcordanceProgress.grant(player, FIRST_LIGHT, ResearchState.UNDERSTOOD);
		ConcordanceProgress.grant(player, Alchemy.RESEARCH, ResearchState.UNDERSTOOD);
		return player;
	}

	private static void use(CrucibleBlockEntity crucible, ServerPlayer player, ItemStack held) {
		RateGate.forget(player.getUUID());
		player.setItemInHand(InteractionHand.MAIN_HAND, held);
		crucible.use(player, player.level(), held, InteractionHand.MAIN_HAND);
	}

	private static ItemStack waterBottle() {
		return PotionContents.createItemStack(Items.POTION, Potions.WATER);
	}

	private static Item item(String id) {
		return BuiltInRegistries.ITEM.getValue(Identifier.parse(id));
	}

	// ---------------------------------------------------------------- the entries and what they teach

	/**
	 * Both entries load cleanly as Practitioner research after First Light, each of its own Principle and tradition.
	 * Understanding Rimekeeping teaches exactly Rimebind and Preserve, and Stratawrighting exactly Brace, at the rules'
	 * costs; mastery lowers each by one.
	 */
	@GameTest(maxTicks = 20)
	public void rimeAndStrataTeachTheirInvocations(GameTestHelper helper) {
		ConcordanceRules rules = ConcordanceData.rules();
		helper.assertTrue(rules.problems().isEmpty(), "The rules load cleanly: " + rules.problems());
		List<Definitions.Requirement> afterFirstLight = List.of(new Definitions.Requirement(FIRST_LIGHT, ResearchState.UNDERSTOOD));
		Map<String, List<String>> principles = Map.of(Rime.RESEARCH, List.of("rime", "rimekeepers"), Strata.RESEARCH,
				List.of("strata", "stratawrights"));
		Map<String, List<String>> teaches = Map.of(Rime.RESEARCH, RIME_SPELLS, Strata.RESEARCH, STRATA_SPELLS);
		int x = 1;
		for (Map.Entry<String, List<String>> entry : teaches.entrySet()) {
			String research = entry.getKey();
			Definitions.Research definition = rules.research(research);
			List<String> principle = principles.get(research);
			helper.assertTrue(definition != null && definition.principle().equals(principle.get(0)) && definition.tradition().equals(principle.get(1))
					&& definition.stage().equals("practitioner") && definition.requires().equals(afterFirstLight),
					research + " is " + principle + " research for a Practitioner, after First Light: " + definition);
			Map<String, Integer> understoodCosts = new HashMap<>();
			Map<String, Integer> masteredCosts = new HashMap<>();
			for (String id : entry.getValue()) {
				Definitions.Invocation invocation = rules.invocations().get(id);
				helper.assertTrue(invocation != null && invocation.research().equals(research) && invocation.state() == ResearchState.UNDERSTOOD
						&& rules.authored(id, WAND) != null && spell(helper.getLevel(), invocation.spell()).is(ConcordanceSpells.SPELLS),
						id + " is taught by " + research + " at understood and fits the Initiate's Wand: " + invocation);
				helper.assertTrue(invocation.cost(ResearchState.MASTERED) == invocation.cost(ResearchState.UNDERSTOOD) - 1,
						id + " costs one less once mastered: " + invocation);
				understoodCosts.put(invocation.spell(), invocation.cost(ResearchState.UNDERSTOOD));
				masteredCosts.put(invocation.spell(), invocation.cost(ResearchState.MASTERED));
			}
			ServerPlayer player = player(helper, new BlockPos(x, 2, 1), 0.0F);
			x += 3;
			ConcordanceProgress.grant(player, FIRST_LIGHT, ResearchState.UNDERSTOOD);
			Map<String, Integer> before = ConcordanceProgress.knowledge(player).invocations();
			ConcordanceProgress.grant(player, research, ResearchState.UNDERSTOOD);
			Map<String, Integer> added = new HashMap<>(ConcordanceProgress.knowledge(player).invocations());
			before.keySet().forEach(added::remove);
			helper.assertTrue(added.equals(understoodCosts), "Understanding " + research + " teaches exactly " + understoodCosts + ": " + added);
			ConcordanceProgress.grant(player, research, ResearchState.MASTERED);
			Map<String, Integer> mastered = new HashMap<>(ConcordanceProgress.knowledge(player).invocations());
			mastered.keySet().retainAll(masteredCosts.keySet());
			helper.assertTrue(mastered.equals(masteredCosts), "Mastering " + research + " lowers their costs: " + mastered);
		}
		helper.succeed();
	}

	/**
	 * Before First Light a snowball teaches nothing. After it, fire first keeps its examination at nothing begun, and snow
	 * then observes Rimekeeping at once; snow again teaches nothing. Another player: ice begins it, packed ice is more of
	 * the same, and a torch observes it.
	 */
	@GameTest(maxTicks = 20)
	public void snowAndFireObserveRimekeepingInEitherOrder(GameTestHelper helper) {
		ServerPlayer player = player(helper, new BlockPos(1, 2, 1), 0.0F);
		examine(player, Items.SNOWBALL);
		helper.assertTrue(!ConcordanceProgress.knowledge(player).entries().containsKey(Rime.RESEARCH),
				"Before First Light a snowball teaches nothing: " + ConcordanceProgress.knowledge(player).entries());
		ConcordanceProgress.grant(player, FIRST_LIGHT, ResearchState.UNDERSTOOD);
		examine(player, Items.COAL);
		helper.assertTrue(state(player, Rime.RESEARCH) == ResearchState.NONE && evidence(player, Rime.RESEARCH).containsKey("examine:minecraft:coal"),
				"Fire first: nothing begun, but the examination is kept: " + evidence(player, Rime.RESEARCH));
		examine(player, Items.SNOWBALL);
		helper.assertTrue(state(player, Rime.RESEARCH) == ResearchState.OBSERVED, "Then snow observes it at once: " + state(player, Rime.RESEARCH));
		ResearchEngine.Result again = examine(player, Items.SNOWBALL);
		helper.assertTrue(again != null && !again.recorded(), "Snow again teaches nothing: " + again);

		ServerPlayer other = player(helper, new BlockPos(4, 2, 1), 0.0F);
		ConcordanceProgress.grant(other, FIRST_LIGHT, ResearchState.UNDERSTOOD);
		examine(other, Items.ICE);
		helper.assertTrue(state(other, Rime.RESEARCH) == ResearchState.ENCOUNTERED, "Ice begins Rimekeeping: " + state(other, Rime.RESEARCH));
		examine(other, Items.PACKED_ICE);
		helper.assertTrue(state(other, Rime.RESEARCH) == ResearchState.ENCOUNTERED, "Packed ice is more cold, not fire");
		examine(other, Items.TORCH);
		helper.assertTrue(state(other, Rime.RESEARCH) == ResearchState.OBSERVED, "A torch observes it: " + state(other, Rime.RESEARCH));
		helper.succeed();
	}

	/**
	 * Every route understands Rimekeeping: the bench; arriving in the Glacier Hall (not the Nether, and nothing while the
	 * Concordance is switched off), which needs First Light and may come before anything else, kept until snow and fire
	 * catch up; and a bottled dose that carries Rime.
	 */
	@GameTest(maxTicks = 20)
	public void eitherRouteUnderstandsRimekeeping(GameTestHelper helper) {
		ResourceKey<Level> hall = Lair.GLACIER_HALL.dimension;
		ServerPlayer student = player(helper, new BlockPos(1, 2, 1), 0.0F);
		learn(student, Rime.RESEARCH, ResearchState.OBSERVED);
		ConcordanceProgress.record(student, new Evidence.Studied("minecraft:snow_block", BENCH));
		helper.assertTrue(state(student, Rime.RESEARCH) == ResearchState.UNDERSTOOD, "Snow studied at the bench: " + state(student, Rime.RESEARCH));

		ServerPlayer traveller = player(helper, new BlockPos(3, 2, 1), 0.0F);
		learn(traveller, Rime.RESEARCH, ResearchState.OBSERVED);
		helper.assertTrue(!RimeSurvey.arrived(traveller, Level.NETHER) && state(traveller, Rime.RESEARCH) == ResearchState.OBSERVED,
				"The Nether is no winter survey");
		// The Concordance off (the lairs still open): arriving records nothing. Switched back at once; the server runs other tests.
		boolean on = JugcraftConfig.isFeatureEnabled(JugcraftConcordance.FEATURE);
		boolean switchedOff;
		JugcraftConfig.setFeatureEnabled(JugcraftConcordance.FEATURE, false);
		try {
			switchedOff = RimeSurvey.arrived(traveller, hall);
		} finally {
			JugcraftConfig.setFeatureEnabled(JugcraftConcordance.FEATURE, on);
		}
		helper.assertTrue(!switchedOff && state(traveller, Rime.RESEARCH) == ResearchState.OBSERVED
				&& practised(traveller, Rime.RESEARCH, RimeSurvey.ACTIVITY).isEmpty(),
				"With the Concordance off the hall teaches nothing: " + evidence(traveller, Rime.RESEARCH));
		helper.assertTrue(RimeSurvey.arrived(traveller, hall) && state(traveller, Rime.RESEARCH) == ResearchState.UNDERSTOOD,
				"Arriving in the Glacier Hall understands it: " + state(traveller, Rime.RESEARCH));

		ServerPlayer stranger = player(helper, new BlockPos(5, 2, 1), 0.0F);
		helper.assertTrue(!RimeSurvey.arrived(stranger, hall) && ConcordanceProgress.knowledge(stranger).entries().isEmpty(),
				"Without First Light the hall teaches nothing: " + ConcordanceProgress.knowledge(stranger).entries());

		ServerPlayer early = player(helper, new BlockPos(1, 2, 4), 0.0F);
		ConcordanceProgress.grant(early, FIRST_LIGHT, ResearchState.UNDERSTOOD);
		helper.assertTrue(RimeSurvey.arrived(early, hall) && state(early, Rime.RESEARCH) == ResearchState.NONE
				&& evidence(early, Rime.RESEARCH).containsKey(key(RimeSurvey.ACTIVITY, RimeSurvey.KEY_GLACIER_HALL)),
				"The hall first: nothing begun, but the survey is kept: " + evidence(early, Rime.RESEARCH));
		ConcordanceProgress.record(early, new Evidence.Examined("minecraft:snowball", 15));
		ConcordanceProgress.record(early, new Evidence.Examined("minecraft:coal", 15));
		helper.assertTrue(state(early, Rime.RESEARCH) == ResearchState.UNDERSTOOD, "Snow and fire then understand it at once: "
				+ state(early, Rime.RESEARCH));

		ServerPlayer brewer = player(helper, new BlockPos(3, 2, 4), 0.0F);
		learn(brewer, Rime.RESEARCH, ResearchState.OBSERVED);
		helper.assertTrue(!RimeSurvey.bottled(brewer, Vector.ZERO) && state(brewer, Rime.RESEARCH) == ResearchState.OBSERVED,
				"A dose with no Rime in it is no survey");
		helper.assertTrue(RimeSurvey.bottled(brewer, Vector.of(Map.of(Axis.RIME, 100L))) && state(brewer, Rime.RESEARCH) == ResearchState.UNDERSTOOD,
				"A dose that carries Rime is: " + state(brewer, Rime.RESEARCH));
		helper.succeed();
	}

	/**
	 * The workshop study: water, a snowball and a cold stir dissolve some Rime, and bottling it by hand understands
	 * Rimekeeping for an alembist who has observed it. A glowstone brew, bottled the same way, teaches nothing of Rime.
	 */
	@GameTest(maxTicks = 20)
	public void aRimeBrewBottledIsTheWorkshopStudy(GameTestHelper helper) {
		CrucibleBlockEntity cold = crucible(helper, new BlockPos(1, 2, 1), Blocks.STONE);
		CrucibleBlockEntity glow = crucible(helper, new BlockPos(4, 2, 1), Blocks.STONE);
		ServerPlayer brewer = alembist(helper, new BlockPos(1, 2, 3));
		ConcordanceProgress.grant(brewer, Rime.RESEARCH, ResearchState.OBSERVED);
		use(cold, brewer, waterBottle());
		use(cold, brewer, new ItemStack(Items.SNOWBALL));
		use(cold, brewer, new ItemStack(Items.STICK));
		helper.assertTrue(cold.mixture().dissolved().get(Axis.RIME) > 0, "A cold stir dissolves some of the snow's Rime: " + cold.mixture());
		use(cold, brewer, new ItemStack(Items.GLASS_BOTTLE));
		helper.assertTrue(state(brewer, Rime.RESEARCH) == ResearchState.UNDERSTOOD
				&& evidence(brewer, Rime.RESEARCH).containsKey(key(RimeSurvey.ACTIVITY, RimeSurvey.KEY_CRUCIBLE)),
				"Bottled by hand, a Rime brew is the workshop study: " + evidence(brewer, Rime.RESEARCH));

		ServerPlayer other = alembist(helper, new BlockPos(4, 2, 3));
		ConcordanceProgress.grant(other, Rime.RESEARCH, ResearchState.OBSERVED);
		use(glow, other, waterBottle());
		use(glow, other, new ItemStack(Items.GLOWSTONE_DUST));
		use(glow, other, new ItemStack(Items.STICK));
		use(glow, other, new ItemStack(Items.GLASS_BOTTLE));
		helper.assertTrue(state(other, Rime.RESEARCH) == ResearchState.OBSERVED && practised(other, Rime.RESEARCH, RimeSurvey.ACTIVITY).isEmpty(),
				"A glowstone brew teaches nothing of Rime: " + evidence(other, Rime.RESEARCH));
		helper.succeed();
	}

	/**
	 * Each kind of cold work counts once: Preserve on a cold crucible, Preserve on a heated one and Rimebind on a hostile
	 * creature master Rimekeeping, and nothing counts after that. Someone who has not understood it learns nothing.
	 */
	@GameTest(maxTicks = 20)
	public void variedColdWorkMastersRimekeeping(GameTestHelper helper) {
		ServerPlayer player = player(helper, new BlockPos(1, 2, 1), 0.0F);
		learn(player, Rime.RESEARCH, ResearchState.UNDERSTOOD);
		helper.assertTrue(Rime.practise(player, Rime.KEY_PRESERVE_COLD), "A cold Preserve is cold work");
		helper.assertTrue(!Rime.practise(player, Rime.KEY_PRESERVE_COLD), "The same kind again teaches nothing");
		helper.assertTrue(Rime.practise(player, Rime.KEY_PRESERVE_HEATED) && state(player, Rime.RESEARCH) == ResearchState.UNDERSTOOD,
				"Two kinds are not yet mastery");
		helper.assertTrue(Rime.practise(player, Rime.KEY_RESTRAINT) && state(player, Rime.RESEARCH) == ResearchState.MASTERED,
				"Three kinds master Rimekeeping: " + state(player, Rime.RESEARCH));
		helper.assertTrue(!Rime.practise(player, "test:another_kind"), "Once mastered, practice teaches nothing more");

		ServerPlayer novice = player(helper, new BlockPos(4, 2, 1), 0.0F);
		ConcordanceProgress.grant(novice, FIRST_LIGHT, ResearchState.UNDERSTOOD);
		helper.assertTrue(!Rime.practise(novice, Rime.KEY_PRESERVE_COLD) && evidence(novice, Rime.RESEARCH).isEmpty(),
				"Someone who has not understood Rimekeeping learns nothing by it");
		helper.succeed();
	}

	/**
	 * A cold crucible is one kind of cold work and a warm, hot or searing one another. Preserve on an empty crucible holds
	 * it but counts for nothing; with water in it at 20 it is cold work, and at 60 heated work.
	 */
	@GameTest(maxTicks = 20)
	public void preserveCountsTheCrucibleBand(GameTestHelper helper) {
		helper.assertTrue(Rime.preserveKey(Band.COLD).equals(Rime.KEY_PRESERVE_COLD) && Rime.preserveKey(Band.WARM).equals(Rime.KEY_PRESERVE_HEATED)
				&& Rime.preserveKey(Band.HOT).equals(Rime.KEY_PRESERVE_HEATED) && Rime.preserveKey(Band.SEARING).equals(Rime.KEY_PRESERVE_HEATED),
				"Cold is cold; warm, hot and searing are heated");
		floor(helper);
		BlockPos at = new BlockPos(4, 2, 4);
		CrucibleBlockEntity crucible = crucible(helper, at, Blocks.STONE);
		ServerPlayer keeper = alembist(helper, new BlockPos(1, 2, 1));
		ConcordanceProgress.grant(keeper, Rime.RESEARCH, ResearchState.UNDERSTOOD);
		EffectSpec preserve = invoked("jugcraft:preserve");
		helper.assertTrue(apply(helper, keeper, preserve, at).applied() && practised(keeper, Rime.RESEARCH, Rime.ACTIVITY).isEmpty(),
				"An empty crucible is held, but holding nothing is no cold work");
		use(crucible, keeper, waterBottle());
		helper.assertTrue(crucible.parts() == 1 && crucible.band() == Band.COLD, "Water at the air's heat: " + crucible.temperature());
		helper.assertTrue(apply(helper, keeper, preserve, at).applied()
				&& practised(keeper, Rime.RESEARCH, Rime.ACTIVITY).equals(List.of(key(Rime.ACTIVITY, Rime.KEY_PRESERVE_COLD))),
				"A cold crucible held is cold work: " + practised(keeper, Rime.RESEARCH, Rime.ACTIVITY));
		crucible.setTemperature(60);
		helper.assertTrue(apply(helper, keeper, preserve, at).applied()
				&& practised(keeper, Rime.RESEARCH, Rime.ACTIVITY).contains(key(Rime.ACTIVITY, Rime.KEY_PRESERVE_HEATED)),
				"A warm one is heated work: " + practised(keeper, Rime.RESEARCH, Rime.ACTIVITY));
		helper.succeed();
	}

	/**
	 * Rimebind on a villager or on a resistant iron golem binds them but restrains nothing hostile; on a zombie, a hostile
	 * creature at normal tolerance, it is restraint.
	 */
	@GameTest(maxTicks = 20)
	public void rimebindOnAHostileCreatureIsRestraint(GameTestHelper helper) {
		floor(helper);
		ServerPlayer keeper = player(helper, new BlockPos(1, 2, 1), 0.0F);
		learn(keeper, Rime.RESEARCH, ResearchState.UNDERSTOOD);
		Mob villager = helper.spawnWithNoFreeWill(EntityTypes.VILLAGER, new BlockPos(2, 2, 4));
		Mob golem = helper.spawnWithNoFreeWill(EntityTypes.IRON_GOLEM, new BlockPos(5, 2, 4));
		Mob zombie = helper.spawnWithNoFreeWill(EntityTypes.ZOMBIE, new BlockPos(3, 2, 6));
		EffectSpec grip = invoked("jugcraft:rimebind");
		helper.assertTrue(apply(helper, keeper, grip, villager).applied() && practised(keeper, Rime.RESEARCH, Rime.ACTIVITY).isEmpty(),
				"A villager is bound, but is no hostile creature");
		helper.assertTrue(apply(helper, keeper, grip, golem).applied() && practised(keeper, Rime.RESEARCH, Rime.ACTIVITY).isEmpty(),
				"A resistant golem is bound for less, and that is no restraint either");
		helper.assertTrue(apply(helper, keeper, grip, zombie).applied()
				&& practised(keeper, Rime.RESEARCH, Rime.ACTIVITY).equals(List.of(key(Rime.ACTIVITY, Rime.KEY_RESTRAINT))),
				"A zombie bound is restraint: " + practised(keeper, Rime.RESEARCH, Rime.ACTIVITY));
		helper.succeed();
	}

	// ---------------------------------------------------------------- Stratawrighting

	/**
	 * With Thallite switched off, granite, diorite and andesite observe Stratawrighting and tuff studied at the bench
	 * understands it; Braced on grass and on granite are two ways of anchoring; and a Brace holding against a zombie's blow
	 * is the third, which masters it. Nothing on the way names a Jugcraft item. (The switch is put back before the blow,
	 * as other tests run beside this one; nothing after it reads the switch.)
	 */
	@GameTest(maxTicks = 120)
	public void strataNeedsNoThallite(GameTestHelper helper) {
		floor(helper);
		helper.setBlock(new BlockPos(1, 1, 1), Blocks.GRASS_BLOCK);
		helper.setBlock(new BlockPos(4, 1, 1), Blocks.GRANITE);
		ServerLevel level = helper.getLevel();
		ServerPlayer player = player(helper, new BlockPos(1, 2, 1), 0.0F);
		EffectSpec bracing = invoked("jugcraft:brace");
		boolean thallite = JugcraftConfig.isFeatureEnabled("thallite");
		try {
			JugcraftConfig.setFeatureEnabled("thallite", false);
			ConcordanceProgress.grant(player, FIRST_LIGHT, ResearchState.UNDERSTOOD);
			for (Item stone : List.of(Items.GRANITE, Items.DIORITE, Items.ANDESITE)) {
				examine(player, stone);
			}
			helper.assertTrue(state(player, Strata.RESEARCH) == ResearchState.OBSERVED, "Three stones observe Stratawrighting: "
					+ state(player, Strata.RESEARCH));
			ConcordanceProgress.record(player, new Evidence.Studied("minecraft:tuff", BENCH));
			helper.assertTrue(state(player, Strata.RESEARCH) == ResearchState.UNDERSTOOD, "Tuff studied at the bench understands it");
			stand(helper, player, new BlockPos(1, 2, 1));
			helper.assertTrue(apply(helper, player, bracing, player).applied()
					&& evidence(player, Strata.RESEARCH).containsKey(key(Strata.ACTIVITY, Strata.KEY_LIVING)), "Braced on grass: living ground");
			player.removeAllEffects();
			stand(helper, player, new BlockPos(4, 2, 1));
			helper.assertTrue(apply(helper, player, bracing, player).applied()
					&& evidence(player, Strata.RESEARCH).containsKey(key(Strata.ACTIVITY, Strata.KEY_EARTHEN)), "Braced on granite: earthen ground");
		} finally {
			JugcraftConfig.setFeatureEnabled("thallite", thallite);
		}
		helper.runAfterDelay(LOADING_TICKS, () -> {
			Mob zombie = helper.spawnWithNoFreeWill(EntityTypes.ZOMBIE, new BlockPos(4, 2, 4));
			helper.assertTrue(player.hasEffect(Brace.BRACED), "The Brace still holds when the zombie strikes");
			player.damageCooldownTime = 0;
			player.hurtServer(level, level.damageSources().mobAttack(zombie), 1.0F);
			helper.assertTrue(evidence(player, Strata.RESEARCH).containsKey(key(Strata.ACTIVITY, Strata.KEY_HELD))
					&& state(player, Strata.RESEARCH) == ResearchState.MASTERED, "A Brace held against a blow masters it: "
							+ evidence(player, Strata.RESEARCH));
			helper.runAfterDelay(1, () -> {
				helper.assertTrue(!player.hasEffect(Brace.BRACED), "The blow spent the Brace");
				List<String> named = new ArrayList<>();
				ConcordanceProgress.knowledge(player).entries().values().forEach(progress -> progress.evidence().keySet().forEach(key -> {
					String specimen = key.startsWith("examine:") ? key.substring("examine:".length())
							: key.startsWith("study:" + BENCH + ":") ? key.substring(("study:" + BENCH + ":").length()) : "";
					if (specimen.startsWith(Jugcraft.MOD_ID + ":")) {
						named.add(key);
					}
				}));
				helper.assertTrue(named.isEmpty(), "Stratawrighting needs no Jugcraft item: " + named);
				helper.succeed();
			});
		});
	}

	/**
	 * Living ground is grass and its kin; earthen is natural stone and sand; worked blocks and the air are neither. A Brace
	 * on someone in the air is no anchoring.
	 */
	@GameTest(maxTicks = 20)
	public void braceReadsTheGroundItStandsOn(GameTestHelper helper) {
		helper.assertTrue(Strata.KEY_LIVING.equals(Strata.ground(Blocks.GRASS_BLOCK.defaultBlockState())), "Grass is living ground");
		helper.assertTrue(Strata.KEY_EARTHEN.equals(Strata.ground(Blocks.GRANITE.defaultBlockState()))
				&& Strata.KEY_EARTHEN.equals(Strata.ground(Blocks.SAND.defaultBlockState())), "Granite and sand are earthen");
		helper.assertTrue(Strata.ground(Blocks.OAK_PLANKS.defaultBlockState()) == null && Strata.ground(null) == null,
				"Planks and the air are no ground");
		ServerPlayer flyer = player(helper, new BlockPos(1, 4, 1), 0.0F);
		learn(flyer, Strata.RESEARCH, ResearchState.UNDERSTOOD);
		flyer.setOnGround(false);
		helper.assertTrue(apply(helper, flyer, invoked("jugcraft:brace"), flyer).applied(), "A Brace takes in the air");
		helper.assertTrue(practised(flyer, Strata.RESEARCH, Strata.ACTIVITY).isEmpty(), "but anchors nothing: "
				+ practised(flyer, Strata.RESEARCH, Strata.ACTIVITY));
		helper.succeed();
	}

	/**
	 * The electives: bracing another player on grass is an ally braced and living ground for the caster, and nothing for
	 * the one braced; a ritual completed is a foundation for each taking part; and a creature's blow taken on stone in
	 * Earthbound Thallite is Rooted holding. Without it, or with the Concordance switched off, the same blow is nothing.
	 */
	@GameTest(maxTicks = 120)
	public void anAllyARitualAndRootedThalliteAreElective(GameTestHelper helper) {
		floor(helper);
		helper.setBlock(new BlockPos(1, 1, 3), Blocks.GRASS_BLOCK);
		ServerLevel level = helper.getLevel();
		ServerPlayer a = player(helper, new BlockPos(1, 2, 1), 0.0F);
		ServerPlayer b = player(helper, new BlockPos(1, 2, 3), 0.0F);
		learn(a, Strata.RESEARCH, ResearchState.UNDERSTOOD);
		learn(b, Strata.RESEARCH, ResearchState.UNDERSTOOD);
		stand(helper, b, new BlockPos(1, 2, 3));
		helper.assertTrue(apply(helper, a, invoked("jugcraft:brace"), b).applied(), "A braces B");
		helper.assertTrue(practised(a, Strata.RESEARCH, Strata.ACTIVITY).equals(List.of(key(Strata.ACTIVITY, Strata.KEY_ALLY),
				key(Strata.ACTIVITY, Strata.KEY_LIVING))), "An ally braced on grass: " + practised(a, Strata.RESEARCH, Strata.ACTIVITY));
		helper.assertTrue(practised(b, Strata.RESEARCH, Strata.ACTIVITY).isEmpty(), "and nothing for the one braced");
		Rituals.completed(level, helper.absolutePos(new BlockPos(6, 2, 6)), "jugcraft:lumen_vigil", List.of(a));
		helper.assertTrue(evidence(a, Strata.RESEARCH).containsKey(key(Strata.ACTIVITY, Strata.KEY_FOUNDATION)), "A ritual completed is a foundation");

		ServerPlayer rooted = player(helper, new BlockPos(4, 2, 4), 0.0F);
		ServerPlayer bare = player(helper, new BlockPos(6, 2, 4), 0.0F);
		learn(rooted, Strata.RESEARCH, ResearchState.UNDERSTOOD);
		learn(bare, Strata.RESEARCH, ResearchState.UNDERSTOOD);
		rooted.setItemSlot(EquipmentSlot.FEET, new ItemStack(item("jugcraft:earthbound_thallite_boots")));
		helper.runAfterDelay(LOADING_TICKS, () -> {
			Mob zombie = helper.spawnWithNoFreeWill(EntityTypes.ZOMBIE, new BlockPos(5, 2, 6));
			// The Concordance off: the armour is worn, but a blow records nothing. Switched back at once.
			boolean on = JugcraftConfig.isFeatureEnabled(JugcraftConcordance.FEATURE);
			JugcraftConfig.setFeatureEnabled(JugcraftConcordance.FEATURE, false);
			try {
				rooted.setOnGround(true);
				rooted.damageCooldownTime = 0;
				rooted.hurtServer(level, level.damageSources().mobAttack(zombie), 1.0F);
			} finally {
				JugcraftConfig.setFeatureEnabled(JugcraftConcordance.FEATURE, on);
			}
			helper.assertTrue(practised(rooted, Strata.RESEARCH, Strata.ACTIVITY).isEmpty(),
					"With the Concordance off a blow taken is nothing: " + practised(rooted, Strata.RESEARCH, Strata.ACTIVITY));
			for (ServerPlayer struck : List.of(rooted, bare)) {
				struck.setOnGround(true);
				struck.damageCooldownTime = 0;
				struck.hurtServer(level, level.damageSources().mobAttack(zombie), 1.0F);
			}
			helper.assertTrue(practised(rooted, Strata.RESEARCH, Strata.ACTIVITY).equals(List.of(key(Strata.ACTIVITY, Strata.KEY_ROOTED))),
					"A blow taken on stone in Earthbound boots is Rooted holding: " + practised(rooted, Strata.RESEARCH, Strata.ACTIVITY));
			helper.assertTrue(practised(bare, Strata.RESEARCH, Strata.ACTIVITY).isEmpty(), "Without them it is nothing");
			helper.succeed();
		});
	}

	/**
	 * Bridging blocks are never specimens (sneak-use in the air examines one): cobblestone, stone and both deepslates.
	 * The four stones and raw Thallite are Stratawrighting's and Assay's; snow and ice are Rimekeeping's.
	 */
	@GameTest(maxTicks = 20)
	public void commonBuildingBlocksAreNoSpecimens(GameTestHelper helper) {
		for (Item block : List.of(Items.COBBLESTONE, Items.STONE, Items.COBBLED_DEEPSLATE, Items.DEEPSLATE)) {
			helper.assertTrue(!new ItemStack(block).is(JugcraftConcordance.SPECIMENS), block + " must not be a specimen");
		}
		for (Item stone : List.of(Items.GRANITE, Items.DIORITE, Items.ANDESITE, Items.TUFF, item("jugcraft:raw_thallite"))) {
			ItemStack stack = new ItemStack(stone);
			helper.assertTrue(stack.is(JugcraftConcordance.STRATA_SPECIMENS) && stack.is(Assaying.SPECIMENS),
					stone + " is Stratawrighting's and Assay's");
		}
		for (Item cold : List.of(Items.SNOWBALL, Items.BLUE_ICE)) {
			helper.assertTrue(new ItemStack(cold).is(JugcraftConcordance.RIME_SPECIMENS), cold + " is Rimekeeping's");
		}
		helper.succeed();
	}

	// ---------------------------------------------------------------- Preserve

	/**
	 * Preserve holds a crucible for 60 seconds (Jade shows the seconds), restarted by a recast. Held hot it stops at the
	 * band's foot while an unheld one beside it cools on; held cold over a fire it stops below warm. The hold over, the
	 * heat goes on moving.
	 */
	@GameTest(maxTicks = 120)
	public void preserveHoldsOneBandAndExpires(GameTestHelper helper) {
		floor(helper);
		ServerLevel level = helper.getLevel();
		boolean jade = FabricLoader.getInstance().isModLoaded("jade");
		ServerPlayer keeper = player(helper, new BlockPos(3, 2, 6), 0.0F);
		BlockPos heldAt = new BlockPos(1, 2, 1);
		BlockPos coldAt = new BlockPos(5, 2, 1);
		CrucibleBlockEntity held = crucible(helper, heldAt, Blocks.STONE);
		CrucibleBlockEntity control = crucible(helper, new BlockPos(3, 2, 1), Blocks.STONE);
		CrucibleBlockEntity cold = crucible(helper, coldAt, Blocks.CAMPFIRE);
		held.setTemperature(120);
		control.setTemperature(120);
		cold.setTemperature(30);
		EffectSpec preserve = invoked("jugcraft:preserve");
		long now = ConcordanceProgress.now(level);
		helper.assertTrue(apply(helper, keeper, preserve, heldAt).applied() && apply(helper, keeper, preserve, coldAt).applied(),
				"Preserve holds both crucibles");
		helper.assertTrue(held.held(now) == Rime.HOLD_TICKS && held.heldUntil() == now + Rime.HOLD_TICKS, "for a minute: " + held.held(now));
		if (jade) {
			int shown = ConcordanceDataProvider.snapshot(held, now).getIntOr("held", 0);
			helper.assertTrue(shown == 60, "Jade shows 60 seconds held: " + shown);
		}
		helper.runAfterDelay(1, () -> {
			long again = ConcordanceProgress.now(level);
			helper.assertTrue(apply(helper, keeper, preserve, heldAt).applied() && held.heldUntil() == again + Rime.HOLD_TICKS,
					"A recast restarts the minute, never adds to it: " + (held.heldUntil() - again));
		});
		helper.runAfterDelay(40, () -> {
			helper.assertTrue(held.temperature() == Band.HOT.from && held.band() == Band.HOT, "Held hot, it stops at the band's foot: "
					+ held.temperature());
			helper.assertTrue(cold.temperature() == Band.WARM.from - 1 && cold.band() == Band.COLD, "Held cold over a fire, it stops below warm: "
					+ cold.temperature());
			helper.assertTrue(control.temperature() >= 78 && control.temperature() <= 82 && control.band() == Band.WARM,
					"Unheld, the one beside it cools on (time is not stopped): " + control.temperature());
			held.hold(ConcordanceProgress.now(level), 5);
			helper.runAfterDelay(15, () -> {
				long later = ConcordanceProgress.now(level);
				helper.assertTrue(held.heldUntil() == 0L && held.temperature() < Band.HOT.from && held.band() == Band.WARM,
						"The hold over, it cools out of the band: " + held.heldUntil() + " " + held.temperature());
				if (jade) {
					helper.assertTrue(ConcordanceDataProvider.snapshot(held, later).getIntOr("held", 0) == 0, "and Jade shows no hold");
				}
				helper.succeed();
			});
		});
	}

	/**
	 * Preservation never wraps an inventory (09:96). Cast at a chest holding a jar of preserves it fizzles, costs nothing
	 * and changes nothing; a barrel, a hopper and a furnace are refused alike. Held, a crucible still takes what a hopper
	 * gives it; the jar still spoils and the furnace still burns.
	 */
	@GameTest(maxTicks = 60)
	public void preservationCannotWrapArbitraryInventories(GameTestHelper helper) {
		floor(helper);
		ServerLevel level = helper.getLevel();
		ServerPlayer keeper = player(helper, new BlockPos(1, 2, 1), AIM);
		learn(keeper, Rime.RESEARCH, ResearchState.UNDERSTOOD);
		BlockPos chestAt = new BlockPos(1, 2, 3);
		helper.setBlock(chestAt, Blocks.CHEST);
		ChestBlockEntity chest = helper.getBlockEntity(chestAt, ChestBlockEntity.class);
		ItemStack jar = new ItemStack(BuiltInRegistries.ITEM.stream().filter(item -> item instanceof PreserveJarItem).findFirst().orElseThrow());
		PreserveJarItem.cooked(jar, level.getGameTime() - PreserveJarItem.SPOIL_TICKS + 10);
		chest.setItem(0, jar);
		CompoundTag chestBefore = chest.saveWithoutMetadata(level.registryAccess());
		cast(keeper, "jugcraft:preserve");
		helper.assertTrue(ConcordanceProgress.currentFocus(keeper) == FocusPool.MAX, "Cast at a chest it fizzles and costs nothing: "
				+ ConcordanceProgress.currentFocus(keeper));
		helper.assertTrue(chest.saveWithoutMetadata(level.registryAccess()).equals(chestBefore), "and the chest is just as it was");

		EffectSpec preserve = invoked("jugcraft:preserve");
		BlockPos barrelAt = new BlockPos(4, 2, 1);
		BlockPos hopperAt = new BlockPos(4, 3, 4);
		BlockPos furnaceAt = new BlockPos(6, 2, 1);
		helper.setBlock(barrelAt, Blocks.BARREL);
		helper.setBlock(hopperAt.below(), Blocks.CHEST);
		helper.setBlock(hopperAt, Blocks.HOPPER);
		helper.setBlock(furnaceAt, Blocks.FURNACE);
		((Container) level.getBlockEntity(helper.absolutePos(barrelAt))).setItem(0, new ItemStack(Items.SNOWBALL, 4));
		((Container) level.getBlockEntity(helper.absolutePos(hopperAt))).setItem(0, new ItemStack(Items.SNOWBALL, 2));
		Container furnace = (Container) level.getBlockEntity(helper.absolutePos(furnaceAt));
		furnace.setItem(0, new ItemStack(Items.RAW_IRON));
		furnace.setItem(1, new ItemStack(Items.COAL));
		for (BlockPos at : List.of(barrelAt, hopperAt, furnaceAt)) {
			BlockEntity entity = level.getBlockEntity(helper.absolutePos(at));
			CompoundTag before = entity.saveWithoutMetadata(level.registryAccess());
			ConcordanceEffects.Result result = apply(helper, keeper, preserve, at);
			helper.assertTrue(result == ConcordanceEffects.Result.NOTHING && entity.saveWithoutMetadata(level.registryAccess()).equals(before),
					entity.getType() + " is no crucible: nothing held, nothing changed: " + result);
		}

		BlockPos crucibleAt = new BlockPos(6, 2, 4);
		CrucibleBlockEntity crucible = crucible(helper, crucibleAt, Blocks.STONE);
		helper.setBlock(crucibleAt.above(), Blocks.HOPPER);
		HopperBlockEntity feeder = helper.getBlockEntity(crucibleAt.above(), HopperBlockEntity.class);
		feeder.setItem(0, new ItemStack(Items.GLOWSTONE_DUST));
		helper.assertTrue(apply(helper, keeper, preserve, crucibleAt).applied(), "A crucible is held");
		helper.runAfterDelay(30, () -> {
			long later = level.getGameTime();
			helper.assertTrue(PreserveJarItem.spoiled(chest.getItem(0), later), "The jar spoiled as it would have: nothing keeps food");
			helper.assertTrue(helper.getBlockState(furnaceAt).getValue(BlockStateProperties.LIT), "The furnace burns on");
			boolean taken = false;
			for (int slot = 0; slot < CrucibleBlockEntity.BUFFER_SLOTS; slot++) {
				taken |= crucible.getItem(slot).is(Items.GLOWSTONE_DUST);
			}
			helper.assertTrue(feeder.isEmpty() && taken, "A held crucible still takes what the hopper gives it");
			helper.succeed();
		});
	}

	/**
	 * Cast through Spell Engine at a crucible with water in it, Preserve holds it for a minute for 3 Focus; a second cast
	 * pays again and restarts the hold, and counts for nothing more. Someone who has only understood First Light may hold
	 * a crucible (as the effect boundary allows anyone), but learns nothing of Rime by it.
	 */
	@GameTest(maxTicks = 20)
	public void preserveIsChargedOnceAndCountsOnce(GameTestHelper helper) {
		floor(helper);
		ServerLevel level = helper.getLevel();
		ServerPlayer keeper = player(helper, new BlockPos(1, 2, 1), AIM);
		learn(keeper, Rime.RESEARCH, ResearchState.UNDERSTOOD);
		ServerPlayer pourer = alembist(helper, new BlockPos(6, 2, 6));
		CrucibleBlockEntity crucible = crucible(helper, new BlockPos(1, 2, 3), Blocks.STONE);
		BlockPos otherAt = new BlockPos(5, 2, 3);
		CrucibleBlockEntity other = crucible(helper, otherAt, Blocks.STONE);
		use(crucible, pourer, waterBottle());
		use(other, pourer, waterBottle());
		helper.assertTrue(crucible.parts() == 1 && other.parts() == 1, "Both crucibles hold water");
		cast(keeper, "jugcraft:preserve");
		long now = level.getGameTime();
		helper.assertTrue(crucible.held(now) == Rime.HOLD_TICKS && ConcordanceProgress.currentFocus(keeper) == FocusPool.MAX - 3,
				"Preserve holds the crucible for a minute, for 3 Focus: " + crucible.held(now) + ", " + ConcordanceProgress.currentFocus(keeper));
		cast(keeper, "jugcraft:preserve");
		helper.assertTrue(ConcordanceProgress.currentFocus(keeper) == FocusPool.MAX - 6 && crucible.held(now) <= Rime.HOLD_TICKS,
				"A second cast pays again and restarts the hold: " + ConcordanceProgress.currentFocus(keeper) + ", " + crucible.held(now));
		helper.assertTrue(practised(keeper, Rime.RESEARCH, Rime.ACTIVITY).equals(List.of(key(Rime.ACTIVITY, Rime.KEY_PRESERVE_COLD))),
				"and counts once: " + practised(keeper, Rime.RESEARCH, Rime.ACTIVITY));
		ServerPlayer novice = player(helper, new BlockPos(5, 2, 1), 0.0F);
		ConcordanceProgress.grant(novice, FIRST_LIGHT, ResearchState.UNDERSTOOD);
		helper.assertTrue(apply(helper, novice, invoked("jugcraft:preserve"), otherAt).applied() && evidence(novice, Rime.RESEARCH).isEmpty(),
				"A novice may hold a crucible, but learns nothing of Rime by it");
		helper.succeed();
	}

	// ---------------------------------------------------------------- saves

	/**
	 * What a player learned of Rime and Strata, with an entry this version no longer knows, round-trips through the
	 * knowledge's codec unchanged; relearning keeps the unknown entry and teaches exactly Rimebind, Preserve and Brace at the
	 * rules' costs. A knowledge from before Chunk B (First Light only) relearns none of them.
	 */
	@GameTest(maxTicks = 20)
	public void rimeAndStrataResearchSurviveASave(GameTestHelper helper) {
		ConcordanceRules rules = ConcordanceData.rules();
		Map<String, Long> rime = Map.of("examine:minecraft:snowball", 15L, "examine:minecraft:coal", 15L,
				key(RimeSurvey.ACTIVITY, RimeSurvey.KEY_GLACIER_HALL), 0L, key(Rime.ACTIVITY, Rime.KEY_PRESERVE_COLD), 0L,
				key(Rime.ACTIVITY, Rime.KEY_PRESERVE_HEATED), 0L, key(Rime.ACTIVITY, Rime.KEY_RESTRAINT), 0L);
		Map<String, Long> strata = Map.of("examine:minecraft:granite", 15L, "examine:minecraft:diorite", 15L,
				"examine:minecraft:andesite", 15L, "study:" + BENCH + ":minecraft:tuff", 0L);
		Knowledge knowledge = new Knowledge(Knowledge.SCHEMA, Map.of(
				Rime.RESEARCH, new Knowledge.Progress(ResearchState.MASTERED, rime),
				Strata.RESEARCH, new Knowledge.Progress(ResearchState.UNDERSTOOD, strata),
				"jugcraft:retired_entry", new Knowledge.Progress(ResearchState.OBSERVED, Map.of("examine:minecraft:stick", 7L))), Map.of());
		Knowledge loaded = ConcordanceCodecs.KNOWLEDGE.parse(JsonOps.INSTANCE,
				ConcordanceCodecs.KNOWLEDGE.encodeStart(JsonOps.INSTANCE, knowledge).getOrThrow()).getOrThrow();
		helper.assertTrue(loaded.equals(knowledge), "Rime and Strata research round-trips through the codec: " + loaded);
		Knowledge relearned = ResearchEngine.relearn(loaded, rules);
		helper.assertTrue(relearned.entries().equals(knowledge.entries()), "Relearning keeps every entry, the unknown one too");
		Map<String, Integer> expected = Map.of(
				"jugcraft:rimebind", rules.invocations().get("jugcraft:rimebind").cost(ResearchState.MASTERED),
				"jugcraft:preserve", rules.invocations().get("jugcraft:preserve").cost(ResearchState.MASTERED),
				"jugcraft:brace", rules.invocations().get("jugcraft:brace").cost(ResearchState.UNDERSTOOD));
		helper.assertTrue(relearned.invocations().equals(expected), "and teaches exactly Rimebind, Preserve and Brace at the rules' costs: "
				+ relearned.invocations());
		Knowledge older = new Knowledge(Knowledge.SCHEMA, Map.of(FIRST_LIGHT, new Knowledge.Progress(ResearchState.UNDERSTOOD, Map.of())), Map.of());
		Map<String, Integer> olderSpells = ResearchEngine.relearn(older, rules).invocations();
		helper.assertTrue(RIME_SPELLS.stream().noneMatch(olderSpells::containsKey) && STRATA_SPELLS.stream().noneMatch(olderSpells::containsKey),
				"A knowledge from before Chunk B relearns none of them: " + olderSpells);
		helper.succeed();
	}
}
