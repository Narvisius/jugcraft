package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.concordance.ConcordanceData;
import io.github.jimbozoomer.jugcraft.concordance.ConcordanceEffects;
import io.github.jimbozoomer.jugcraft.concordance.ConcordanceProgress;
import io.github.jimbozoomer.jugcraft.concordance.ConcordanceSpells;
import io.github.jimbozoomer.jugcraft.concordance.JugcraftConcordance;
import io.github.jimbozoomer.jugcraft.concordance.RateGate;
import io.github.jimbozoomer.jugcraft.concordance.balance.Baselines;
import io.github.jimbozoomer.jugcraft.concordance.balance.Benchmark;
import io.github.jimbozoomer.jugcraft.concordance.effect.Cause;
import io.github.jimbozoomer.jugcraft.concordance.effect.EffectSpec;
import io.github.jimbozoomer.jugcraft.concordance.effect.Ledger;
import io.github.jimbozoomer.jugcraft.concordance.rules.ConcordanceRules;
import io.github.jimbozoomer.jugcraft.concordance.rules.Definitions;
import io.github.jimbozoomer.jugcraft.concordance.rules.FocusPool;
import io.github.jimbozoomer.jugcraft.concordance.rules.ResearchState;
import io.github.jimbozoomer.jugcraft.concordance.sign.Presentation;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.spell_engine.api.spell.Spell;
import net.spell_engine.api.spell.registry.SpellRegistry;
import net.spell_engine.internals.SpellExecution;
import net.spell_engine.internals.casting.SpellCast;
import net.spell_engine.internals.casting.SpellCaster;
import net.spell_engine.internals.container.SpellContainerSource;
import net.spell_engine.internals.target.SpellTarget;

/**
 * Roadmap step 11, combat and progression baselines. The benchmark ({@code concordance/balance}) runs its roster
 * through five encounter shapes over the rules and Spell Engine spells this server loaded, and its acceptance checks
 * must hold. The rest calibrate the model against the server: armour against blows but not spells, absorption and
 * hurt immunity, how far a push carries, the cooldowns and Focus a cast really costs, and that presentation options
 * change nothing a server decides.
 */
public class ConcordanceBaselineGameTests {
	private static final String FIRST_LIGHT = "jugcraft:first_light";
	/** Ticks to wait out vanilla's hurt immunity (a second hit within 10 ticks counts only for what it exceeds). */
	private static final int HURT_IMMUNITY_TICKS = 12;

	private static void floor(GameTestHelper helper) {
		for (int x = 0; x <= 7; x++) {
			for (int z = 0; z <= 7; z++) {
				helper.setBlock(new BlockPos(x, 1, z), Blocks.STONE);
			}
		}
	}

	/** A survival player with the Initiate's Wand and First Light mastered, facing south (+Z). */
	private static ServerPlayer master(GameTestHelper helper, BlockPos standAt) {
		ServerPlayer player = helper.makeMockServerPlayerInLevel();
		player.setGameMode(GameType.SURVIVAL);
		BlockPos absolute = helper.absolutePos(standAt);
		player.snapTo(absolute.getX() + 0.5, absolute.getY(), absolute.getZ() + 0.5, 0.0F, 0.0F);
		player.setYHeadRot(0.0F);
		player.setYBodyRot(0.0F);
		player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(JugcraftConcordance.INITIATE_WAND));
		RateGate.forget(player.getUUID());
		ConcordanceProgress.grant(player, FIRST_LIGHT, ResearchState.MASTERED);
		SpellContainerSource.setDirty(player, ConcordanceSpells.SOURCE);
		SpellContainerSource.setDirty(player, "main_hand");
		SpellContainerSource.update(player);
		return player;
	}

	private static Holder<Spell> spell(ServerLevel level, String id) {
		return SpellRegistry.from(level).get(Identifier.parse(id)).orElseThrow();
	}

	private static void cast(ServerPlayer player, String id) {
		ServerLevel level = player.level();
		((SpellCaster.Player) player).getCooldownManager().reset(null);
		SpellExecution.performSpell(level, player, spell(level, id), SpellTarget.SearchResult.empty(), SpellCast.Action.RELEASE, 1.0F);
	}

	/** Spell Engine's cast and cooldown times for every Concordance invocation and the carrier, as this server loaded them. */
	private static Map<String, Baselines.Timing> timings(ServerLevel level) {
		List<String> spells = new ArrayList<>();
		for (Definitions.Invocation invocation : ConcordanceData.rules().invocations().values()) {
			spells.add(invocation.spell());
		}
		spells.add("jugcraft:composed");
		Map<String, Baselines.Timing> timings = new HashMap<>();
		for (String id : spells) {
			Spell spell = spell(level, id).value();
			float cast = spell.active == null || spell.active.cast == null ? 0.0F : spell.active.cast.duration;
			timings.put(id, new Baselines.Timing(Math.round(cast * 20.0F), Math.round(spell.cost.cooldown.duration * 20.0F)));
		}
		return timings;
	}

	private static Baselines.Source source(ServerLevel level) {
		return new Baselines.Source(ConcordanceData.rules(), Baselines.WAND, timings(level));
	}

	private static EffectSpec firstEffect(String invocation) {
		return ConcordanceData.rules().authored(invocation, Baselines.WAND).plan().root().steps().getFirst().effect();
	}

	private static ConcordanceEffects.Context context(ServerLevel level, ServerPlayer actor, Vec3 origin) {
		return new ConcordanceEffects.Context(level, Cause.of(actor.getUUID(), Cause.Origin.INVOCATION, "test:calibration",
				ConcordanceEffects.nextSerial()), actor, new Ledger(new Ledger.Limits(4, 16, 0)), "0/0/0", origin);
	}

	/**
	 * The benchmark over this server's rules and spells: the full roster runs through the five encounters, its table
	 * is written to the log, and the acceptance checks hold (no early ability trivializes every encounter; several
	 * kits succeed at each; the utility character keeps survival options; Spell Power raises only the Lance).
	 */
	@GameTest(maxTicks = 20)
	public void baselinesHoldOnTheLoadedRules(GameTestHelper helper) {
		ConcordanceRules rules = ConcordanceData.rules();
		helper.assertTrue(rules.problems().isEmpty(), "The rules load clean: " + rules.problems());
		Baselines.Report report = Baselines.report(source(helper.getLevel()));
		Jugcraft.LOGGER.info("Arcane Concordance baselines (roadmap step 11), from this server's rules and spells:\n{}", report.table());
		helper.assertTrue(report.outcomes().size() == 8 + rules.invocations().size() + 1 && report.encounters().size() == 5,
				"Eight kits, one character per invocation and the utility kit without its shield, in five encounters: " + report.outcomes().size());
		helper.assertTrue(report.accepted(), "The step 11 acceptance checks hold: " + report.failures());
		helper.succeed();
	}

	/**
	 * Armour reduces a blow exactly as the model computes it, and does not reduce the Lance (Spell Power's damage
	 * bypasses armour), as the model assumes: a husk in an iron chestplate takes the model's share of 6 from a zombie's
	 * blow and the full 5 from the Lance.
	 */
	@GameTest(maxTicks = 40)
	public void armourReducesBlowsButNotTheLance(GameTestHelper helper) {
		floor(helper);
		ServerLevel level = helper.getLevel();
		ServerPlayer player = master(helper, new BlockPos(1, 2, 1));
		Mob husk = helper.spawnWithNoFreeWill(EntityTypes.HUSK, new BlockPos(1, 2, 5));
		// Behind the husk on the Lance's line: its blow knocks the husk along the line, not out of it.
		Mob zombie = helper.spawnWithNoFreeWill(EntityTypes.ZOMBIE, new BlockPos(1, 2, 7));
		husk.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.IRON_CHESTPLATE));
		helper.runAfterDelay(2, () -> {
			int armour = husk.getArmorValue();
			int toughness = (int) husk.getAttributeValue(Attributes.ARMOR_TOUGHNESS);
			helper.assertTrue(armour >= 8, "The husk wears its chestplate: armour " + armour);
			float before = husk.getHealth();
			husk.hurtServer(level, level.damageSources().mobAttack(zombie), 6.0F);
			double blow = before - husk.getHealth();
			double model = Benchmark.afterArmour(6.0, armour, toughness);
			helper.assertTrue(Math.abs(blow - model) < 1.0E-3, "A blow of 6 against armour " + armour + " deals " + blow + "; the model says "
					+ model);
			helper.runAfterDelay(HURT_IMMUNITY_TICKS, () -> {
				float healthy = husk.getHealth();
				cast(player, "jugcraft:lance");
				helper.assertTrue(healthy - husk.getHealth() == 5.0F, "Armour does not reduce the Lance: " + (healthy - husk.getHealth())
						+ " (the husk is at " + husk.position() + ", the player at " + player.position() + ")");
				helper.succeed();
			});
		});
	}

	/**
	 * Absorption takes damage before health, and hurt immunity lets a hit within 10 ticks count only for what it
	 * exceeds the last: a villager shielded by Dawn Aegis's effect and struck for 6, 4 and 9 at once ends with the
	 * health and absorption the model computes.
	 */
	@GameTest(maxTicks = 20)
	public void absorptionAndHurtImmunityMatchTheModel(GameTestHelper helper) {
		floor(helper);
		ServerLevel level = helper.getLevel();
		ServerPlayer player = master(helper, new BlockPos(1, 2, 1));
		Mob villager = helper.spawnWithNoFreeWill(EntityTypes.VILLAGER, new BlockPos(4, 2, 4));
		Mob zombie = helper.spawnWithNoFreeWill(EntityTypes.ZOMBIE, new BlockPos(6, 2, 6));
		ConcordanceEffects.Result shielded = ConcordanceEffects.apply(context(level, player, villager.position()), firstEffect("jugcraft:aegis"), villager);
		helper.assertTrue(shielded.applied() && villager.getAbsorptionAmount() == 8.0F, "Dawn Aegis's shell absorbs 8: " + shielded + " "
				+ villager.getAbsorptionAmount());
		for (float hit : new float[] {6.0F, 4.0F, 9.0F}) {
			villager.hurtServer(level, level.damageSources().mobAttack(zombie), hit);
		}
		double[] model = Benchmark.afterHits(villager.getMaxHealth(), 8.0, villager.getArmorValue(),
				(int) villager.getAttributeValue(Attributes.ARMOR_TOUGHNESS), 6.0, 4.0, 9.0);
		helper.assertTrue(Math.abs(villager.getHealth() - model[0]) < 1.0E-3 && Math.abs(villager.getAbsorptionAmount() - model[1]) < 1.0E-3,
				"Hits of 6, 4 and 9 leave health " + villager.getHealth() + " and absorption " + villager.getAbsorptionAmount() + "; the model says "
						+ model[0] + " and " + model[1]);
		helper.succeed();
	}

	/**
	 * A push carries a creature as far as the model says: Flashstep's 1.2 blocks a tick, on flat ground, carries a pig
	 * at least the model's reach and not more than 30% beyond it.
	 */
	@GameTest(maxTicks = 40)
	public void aPushCarriesAsFarAsTheModelSays(GameTestHelper helper) {
		floor(helper);
		ServerLevel level = helper.getLevel();
		ServerPlayer player = master(helper, new BlockPos(6, 2, 6));
		LivingEntity pig = helper.spawn(EntityTypes.PIG, new BlockPos(3, 2, 0));
		pig.setYRot(0.0F);
		pig.setYHeadRot(0.0F);
		pig.setYBodyRot(0.0F);
		pig.setXRot(0.0F);
		Vec3 start = pig.position();
		EffectSpec flash = firstEffect("jugcraft:flashstep");
		ConcordanceEffects.Result pushed = ConcordanceEffects.apply(context(level, player, start), flash, pig);
		helper.assertTrue(pushed.applied(), "Flashstep's push moves the pig: " + pushed);
		// Nearly all of a push's travel is in its first ten ticks; measuring then leaves the pig's own wandering little time.
		helper.runAfterDelay(12, () -> {
			double carried = Math.hypot(pig.getX() - start.x, pig.getZ() - start.z);
			double model = flash.magnitude() / 10.0 * Benchmark.PUSH_REACH;
			Jugcraft.LOGGER.info("Arcane Concordance calibration: a push of {} blocks a tick carried a pig {} blocks (model {})",
					flash.magnitude() / 10.0, carried, model);
			helper.assertTrue(carried >= model && carried <= model * Benchmark.PUSH_TOLERANCE, "The push carried the pig " + carried
					+ " blocks; the model says " + model + " (up to " + model * Benchmark.PUSH_TOLERANCE + ")");
			helper.succeed();
		});
	}

	/**
	 * A real cast costs the Focus and starts the cooldown the model uses: the Lance and Dawn Aegis cast through Spell
	 * Engine by a master, compared with the abilities the benchmark builds from the same rules and spells.
	 */
	@GameTest(maxTicks = 20)
	public void castsCostWhatTheModelSays(GameTestHelper helper) {
		floor(helper);
		ServerLevel level = helper.getLevel();
		ServerPlayer player = master(helper, new BlockPos(1, 2, 1));
		helper.spawnWithNoFreeWill(EntityTypes.VILLAGER, new BlockPos(1, 2, 5));
		Baselines.Source source = source(level);
		for (String invocation : List.of("jugcraft:lance", "jugcraft:aegis")) {
			Benchmark.Ability ability = source.invocation(invocation, null, ResearchState.MASTERED, 0.0);
			ConcordanceProgress.setFocus(player, FocusPool.MAX);
			cast(player, invocation);
			int left = ((SpellCaster.Player) player).getCooldownManager().getCooldownDuration(spell(level, invocation));
			int spent = FocusPool.MAX - ConcordanceProgress.currentFocus(player);
			helper.assertTrue(ability != null && left == ability.cooldownTicks() && spent == ability.focus(), invocation + " cost " + spent
					+ " Focus and " + left + " ticks of cooldown; the model has " + (ability == null ? "nothing" : ability.focus() + " and "
					+ ability.cooldownTicks()));
		}
		helper.succeed();
	}

	/**
	 * Presentation options change nothing the server decides: with reduced motion and the visual intensity at every
	 * setting (the display settings shared code can see, roadmap steps 11 and 27), Kindle sets the same light and Dawn
	 * Aegis the same shell, at the same cost.
	 */
	@GameTest(maxTicks = 20)
	public void presentationOptionsChangeNoOutcome(GameTestHelper helper) {
		floor(helper);
		helper.setBlock(new BlockPos(1, 2, 5), Blocks.STONE);
		helper.setBlock(new BlockPos(1, 3, 5), Blocks.STONE);
		ServerPlayer player = master(helper, new BlockPos(1, 2, 1));
		BlockPos lit = new BlockPos(1, 3, 4);
		List<String> results = new ArrayList<>();
		Presentation.Intensity wasIntensity = Presentation.intensity();
		boolean wasCalm = Presentation.reducedMotion();
		try {
			for (Presentation.Intensity intensity : Presentation.Intensity.values()) {
				for (boolean calm : new boolean[] {false, true}) {
					Presentation.configure(intensity, calm);
					helper.setBlock(lit, Blocks.AIR);
					player.removeEffect(MobEffects.ABSORPTION);
					ConcordanceProgress.setFocus(player, FocusPool.MAX);
					cast(player, "jugcraft:kindle");
					BlockState mote = helper.getBlockState(lit);
					cast(player, "jugcraft:aegis");
					MobEffectInstance shell = player.getEffect(MobEffects.ABSORPTION);
					results.add(mote + " " + (shell == null ? "none" : shell.getAmplifier() + "/" + shell.getDuration()) + " "
							+ ConcordanceProgress.currentFocus(player));
				}
			}
		} finally {
			Presentation.configure(wasIntensity, wasCalm);
		}
		helper.assertTrue(results.stream().distinct().count() == 1 && results.get(0).contains("lumen_mote"),
				"Every intensity, with reduced motion off and on, gives the same light, shell and Focus: " + results);
		helper.succeed();
	}
}
