package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.concordance.ConcordanceData;
import io.github.jimbozoomer.jugcraft.concordance.ConcordanceEffects;
import io.github.jimbozoomer.jugcraft.concordance.CrucibleBlockEntity;
import io.github.jimbozoomer.jugcraft.concordance.JugcraftConcordance;
import io.github.jimbozoomer.jugcraft.concordance.LumenMoteBlock;
import io.github.jimbozoomer.jugcraft.concordance.balance.Benchmark;
import io.github.jimbozoomer.jugcraft.concordance.effect.Cause;
import io.github.jimbozoomer.jugcraft.concordance.effect.EffectKind;
import io.github.jimbozoomer.jugcraft.concordance.effect.EffectSpec;
import io.github.jimbozoomer.jugcraft.concordance.effect.Intent;
import io.github.jimbozoomer.jugcraft.concordance.effect.Ledger;
import io.github.jimbozoomer.jugcraft.concordance.effect.Stacking;
import io.github.jimbozoomer.jugcraft.concordance.rime.Rime;
import io.github.jimbozoomer.jugcraft.concordance.rime.Rimebind;
import io.github.jimbozoomer.jugcraft.concordance.strata.Brace;
import io.github.jimbozoomer.jugcraft.lair.JugcraftLairs;
import io.github.jimbozoomer.jugcraft.lair.LairVisit;
import io.github.jimbozoomer.jugcraft.lair.Lairs;
import io.github.jimbozoomer.jugcraft.lair.tatterlace.JugcraftTatterlace;
import io.github.jimbozoomer.jugcraft.lair.tyrant.JugcraftTyrant;
import io.github.jimbozoomer.jugcraft.lair.vesperine.JugcraftVesperine;
import io.github.jimbozoomer.jugcraft.lair.yeti.JugcraftYeti;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Roadmap step 9, the shared effect boundary ({@link ConcordanceEffects}): the same effect does the same thing whatever
 * delivers it, every application is attributed to its cause (triggered ones too), friendly fire, tolerance and
 * protection apply alike, lasting effects stack and expire by one rule, an event's ledger counts each application once,
 * and the block operations act only where the actor may. Since Concordance Chunk B, unit 3: Rimebind's thaw lets no
 * creature be bound for more than 3 seconds in 5, the lair keepers bear it as resistant, a Brace takes half of the next
 * harmful push or blow and is gone when that tick ends (one's own Flashstep neither feels nor spends it, and no lair
 * mist or exit is stopped by it), both survive a save, and Preserve holds a crucible, never removes a block and acts
 * only where its caster could build.
 */
public class ConcordanceEffectGameTests {
	private static final EffectSpec SEAR = new EffectSpec(EffectKind.DAMAGE, Intent.HARMFUL, 4, 0, null, Stacking.STRONGEST, "spell_power:arcane");
	private static final EffectSpec DAZZLE = new EffectSpec(EffectKind.STATUS, Intent.HARMFUL, 0, 60, "minecraft:slowness",
			Stacking.STRONGEST, null);
	private static final EffectSpec WARD = EffectSpec.of(EffectKind.PROTECTION, Intent.HELPFUL, 4, 200);
	private static final String WAND = "jugcraft:initiate_wand";
	private static final UUID NEIGHBOUR = UUID.fromString("00000000-0000-0000-0000-00000000b707");
	private static final UUID AWAY = UUID.fromString("00000000-0000-0000-0000-00000000b708");

	private static void floor(GameTestHelper helper) {
		for (int x = 0; x <= 7; x++) {
			for (int z = 0; z <= 7; z++) {
				helper.setBlock(new BlockPos(x, 1, z), Blocks.STONE);
			}
		}
	}

	private static ServerPlayer player(GameTestHelper helper, BlockPos standAt) {
		ServerPlayer player = TestPlayers.mock(helper);
		player.setGameMode(GameType.SURVIVAL);
		BlockPos absolute = helper.absolutePos(standAt);
		player.snapTo(absolute.getX() + 0.5, absolute.getY(), absolute.getZ() + 0.5, 0.0F, 0.0F);
		return player;
	}

	private static Ledger open() {
		return new Ledger(new Ledger.Limits(16, 256, 0));
	}

	private static ConcordanceEffects.Context context(GameTestHelper helper, Cause cause, @Nullable Entity actor, Ledger ledger, String step) {
		return new ConcordanceEffects.Context(helper.getLevel(), cause, actor, ledger, step, Vec3.atCenterOf(helper.absolutePos(new BlockPos(0, 2, 0))));
	}

	private static Cause cause(@Nullable Entity actor, Cause.Origin origin) {
		return Cause.of(actor == null ? null : actor.getUUID(), origin, "test:" + origin.id, ConcordanceEffects.nextSerial());
	}

	/** The first effect of an invocation's composed form on the Initiate's Wand: what its cast applies. */
	private static EffectSpec invoked(String id) {
		return ConcordanceData.rules().authored(id, WAND).plan().root().steps().getFirst().effect();
	}

	/** {@code spec} on {@code target} as {@code actor}'s own new event (a new cast). */
	private static ConcordanceEffects.Result cast(GameTestHelper helper, ServerPlayer actor, EffectSpec spec, LivingEntity target) {
		return ConcordanceEffects.apply(context(helper, cause(actor, Cause.Origin.INVOCATION), actor, open(), "0/0/0"), spec, target);
	}

	private static ConcordanceEffects.Result cast(GameTestHelper helper, ServerPlayer actor, EffectSpec spec, BlockPos at) {
		return ConcordanceEffects.apply(context(helper, cause(actor, Cause.Origin.INVOCATION), actor, open(), "0/0/0"), spec,
				helper.absolutePos(at));
	}

	/** How fast {@code entity} is moving across the ground. */
	private static double horizontal(Entity entity) {
		Vec3 motion = entity.getDeltaMovement();
		return Math.sqrt(motion.x * motion.x + motion.z * motion.z);
	}

	private static boolean near(double value, double expected) {
		return Math.abs(value - expected) < 1.0E-6;
	}

	/**
	 * Damage, a status and protection, each delivered as a spell, a potion, a weapon, a creature and a shrine, do exactly
	 * the same to five identical pigs; only who is credited differs.
	 */
	@GameTest(maxTicks = 20)
	public void oneEffectWhateverDeliversIt(GameTestHelper helper) {
		floor(helper);
		ServerPlayer caster = player(helper, new BlockPos(0, 2, 0));
		Mob creature = helper.spawnWithNoFreeWill(EntityTypes.PIG, new BlockPos(7, 2, 7));
		Cause.Origin[] origins = {Cause.Origin.SPELL, Cause.Origin.POTION, Cause.Origin.WEAPON, Cause.Origin.CREATURE, Cause.Origin.SHRINE};
		Entity[] actors = {caster, caster, caster, creature, null};
		List<Mob> pigs = new ArrayList<>();
		for (int i = 0; i < origins.length; i++) {
			Mob pig = helper.spawnWithNoFreeWill(EntityTypes.PIG, new BlockPos(1 + i, 2, 4));
			pigs.add(pig);
			Cause cause = cause(actors[i], origins[i]);
			Ledger ledger = open();
			ConcordanceEffects.Result hurt = ConcordanceEffects.apply(context(helper, cause, actors[i], ledger, "0/0/0"), SEAR, pig);
			ConcordanceEffects.Result slowed = ConcordanceEffects.apply(context(helper, cause, actors[i], ledger, "0/0/1"), DAZZLE, pig);
			ConcordanceEffects.Result warded = ConcordanceEffects.apply(context(helper, cause, actors[i], ledger, "0/0/2"), WARD, pig);
			helper.assertTrue(hurt.applied() && slowed.applied() && warded.applied(), origins[i].id + ": every effect applies: "
					+ hurt + ", " + slowed + ", " + warded);
		}
		float health = pigs.getFirst().getHealth();
		for (Mob pig : pigs) {
			MobEffectInstance slow = pig.getEffect(MobEffects.SLOWNESS);
			MobEffectInstance ward = pig.getEffect(MobEffects.ABSORPTION);
			helper.assertTrue(pig.getHealth() == health && health == pig.getMaxHealth() - 4.0F, "The same damage from every source: "
					+ pig.getHealth() + " vs " + health);
			helper.assertTrue(slow != null && slow.getAmplifier() == 0 && slow.getDuration() == 60, "The same status from every source: " + slow);
			helper.assertTrue(ward != null && ward.getAmplifier() == 0 && ward.getDuration() == 200, "The same protection from every source: " + ward);
		}
		for (int i = 0; i < 3; i++) {
			helper.assertTrue(pigs.get(i).getLastHurtByMob() == caster, origins[i].id + ": the caster is credited");
		}
		helper.assertTrue(pigs.get(3).getLastHurtByMob() == creature, "a creature's effect is credited to the creature");
		helper.assertTrue(pigs.get(4).getLastHurtByMob() == null, "a sourceless shrine credits nobody");
		// Absorption fills when the effect starts (vanilla may do that on its first tick): the same for every pig.
		helper.runAfterDelay(2, () -> {
			for (Mob pig : pigs) {
				helper.assertTrue(pig.getAbsorptionAmount() == 4.0F, "Every ward absorbs 4: " + pig.getAbsorptionAmount());
			}
			helper.succeed();
		});
	}

	/** Friendly fire, immunity, resistance and an effect triggered by another keep the same rules and the same cause. */
	@GameTest(maxTicks = 20)
	public void friendlyFireToleranceAndTriggeredCause(GameTestHelper helper) {
		floor(helper);
		ServerLevel level = helper.getLevel();
		ServerPlayer a = player(helper, new BlockPos(1, 2, 1));
		ServerPlayer b = player(helper, new BlockPos(4, 2, 1));
		Cause spell = cause(a, Cause.Origin.SPELL);
		ConcordanceEffects.Result self = ConcordanceEffects.apply(context(helper, spell, a, open(), "0/0/0"), SEAR, a);
		helper.assertTrue(self == ConcordanceEffects.Result.FRIENDLY, "A harmful effect never reaches its own caster: " + self);
		ConcordanceEffects.Result other = ConcordanceEffects.apply(context(helper, spell, a, open(), "0/0/0"), SEAR, b);
		// Refused as friendly fire exactly where the server's PvP rules forbid it (where allowed, a newly joined player's
		// spawn invulnerability may still turn the hit away, which is vanilla's rule, not this one).
		helper.assertTrue((other == ConcordanceEffects.Result.FRIENDLY) != a.canHarmPlayer(b) && other != ConcordanceEffects.Result.IMMUNE,
				"Another player is refused only where the server's PvP rules forbid: " + other + ", PvP " + a.canHarmPlayer(b));
		b.setGameMode(GameType.CREATIVE);
		ConcordanceEffects.Result creative = ConcordanceEffects.apply(context(helper, spell, a, open(), "0/0/1"), DAZZLE, b);
		helper.assertTrue(creative == ConcordanceEffects.Result.IMMUNE, "Creative players are immune to harm: " + creative);
		helper.assertTrue(ConcordanceEffects.apply(context(helper, spell, a, open(), "0/0/2"), WARD, b).applied(), "Help reaches them still");
		b.setGameMode(GameType.SURVIVAL);
		helper.assertTrue(ConcordanceEffects.mayHarm(null, b), "A sourceless effect (a shrine) obeys no friendly-fire rule but its own");

		Mob golem = helper.spawnWithNoFreeWill(EntityTypes.IRON_GOLEM, new BlockPos(6, 2, 6));
		Mob pig = helper.spawnWithNoFreeWill(EntityTypes.PIG, new BlockPos(2, 2, 6));
		helper.assertTrue(ConcordanceEffects.apply(context(helper, spell, a, open(), "0/0/3"), DAZZLE, golem).applied()
				&& golem.getEffect(MobEffects.SLOWNESS).getDuration() == 30, "An iron golem resists: half the time");
		helper.assertTrue(ConcordanceEffects.apply(context(helper, spell, a, open(), "0/0/3"), DAZZLE, pig).applied()
				&& pig.getEffect(MobEffects.SLOWNESS).getDuration() == 60, "A pig takes it all");

		Cause triggered = spell.triggered();
		Mob struck = helper.spawnWithNoFreeWill(EntityTypes.PIG, new BlockPos(5, 2, 4));
		helper.assertTrue(triggered != null && ConcordanceEffects.apply(context(helper, triggered, ConcordanceEffects.actor(level, triggered), open(),
				"1/0/0"), SEAR, struck).applied() && struck.getLastHurtByMob() == a, "A triggered effect is still the caster's");
		helper.succeed();
	}

	/** One stacking rule for every lasting effect; an event applies a step to a target once; lasting effects expire. */
	@GameTest(maxTicks = 40)
	public void lastingEffectsStackOnceAndExpire(GameTestHelper helper) {
		floor(helper);
		Mob pig = helper.spawnWithNoFreeWill(EntityTypes.PIG, new BlockPos(3, 2, 3));
		Cause shrine = cause(null, Cause.Origin.SHRINE);
		Ledger ledger = open();
		EffectSpec strong = new EffectSpec(EffectKind.STATUS, Intent.HARMFUL, 1, 100, "minecraft:slowness", Stacking.STRONGEST, null);
		EffectSpec weakLong = new EffectSpec(EffectKind.STATUS, Intent.HARMFUL, 0, 400, "minecraft:slowness", Stacking.STRONGEST, null);
		EffectSpec sameLonger = new EffectSpec(EffectKind.STATUS, Intent.HARMFUL, 1, 200, "minecraft:slowness", Stacking.STRONGEST, null);
		EffectSpec build = new EffectSpec(EffectKind.STATUS, Intent.HARMFUL, 0, 50, "minecraft:slowness", Stacking.ACCUMULATE, null);
		EffectSpec exclusive = new EffectSpec(EffectKind.STATUS, Intent.HARMFUL, 3, 999, "minecraft:slowness", Stacking.EXCLUSIVE, null);
		helper.assertTrue(ConcordanceEffects.apply(context(helper, shrine, null, ledger, "0/0/0"), strong, pig).applied(), "Slowness II applies");
		helper.assertTrue(ConcordanceEffects.apply(context(helper, shrine, null, ledger, "0/0/1"), weakLong, pig) == ConcordanceEffects.Result.KEPT
				&& pig.getEffect(MobEffects.SLOWNESS).getAmplifier() == 1 && pig.getEffect(MobEffects.SLOWNESS).getDuration() == 100,
				"A weaker effect changes nothing, however long");
		helper.assertTrue(ConcordanceEffects.apply(context(helper, shrine, null, ledger, "0/0/2"), sameLonger, pig).applied()
				&& pig.getEffect(MobEffects.SLOWNESS).getDuration() == 200, "As strong and longer: extended");
		helper.assertTrue(ConcordanceEffects.apply(context(helper, shrine, null, ledger, "0/0/3"), build, pig).applied()
				&& pig.getEffect(MobEffects.SLOWNESS).getAmplifier() == 2, "Accumulating adds a level");
		helper.assertTrue(ConcordanceEffects.apply(context(helper, shrine, null, ledger, "0/0/4"), exclusive, pig) == ConcordanceEffects.Result.KEPT,
				"An exclusive effect cannot be renewed while one lasts");
		helper.assertTrue(ConcordanceEffects.apply(context(helper, shrine, null, ledger, "0/0/2"), sameLonger, pig) == ConcordanceEffects.Result.LIMIT,
				"The same step reaches the same creature once in an event");
		EffectSpec glimpse = EffectSpec.of(EffectKind.DETECTION, Intent.HARMFUL, 0, 2);
		helper.assertTrue(ConcordanceEffects.apply(context(helper, shrine, null, ledger, "0/0/5"), glimpse, pig).applied()
				&& pig.hasEffect(MobEffects.GLOWING), "Detection makes it glow");
		helper.runAfterDelay(5, () -> {
			helper.assertFalse(pig.hasEffect(MobEffects.GLOWING), "and the glow expires by itself");
			helper.succeed();
		});
	}

	/** Light, use, harvest and quench act only on what suits them, only where the actor may, and as the actor. */
	@GameTest(maxTicks = 20)
	public void blockOperationsActAsTheActor(GameTestHelper helper) {
		floor(helper);
		ServerLevel level = helper.getLevel();
		ServerPlayer player = player(helper, new BlockPos(0, 2, 0));
		Cause spell = cause(player, Cause.Origin.SPELL);
		Ledger ledger = open();
		EffectSpec light = EffectSpec.of(EffectKind.ILLUMINATION, Intent.HELPFUL, 0, 640);
		BlockPos air = helper.absolutePos(new BlockPos(2, 3, 2));
		helper.assertTrue(ConcordanceEffects.apply(context(helper, spell, player, ledger, "0/0/0"), light, air).applied(), "Light in open air");
		BlockState mote = level.getBlockState(air);
		helper.assertTrue(mote.is(JugcraftConcordance.LUMEN_MOTE) && mote.getValue(LumenMoteBlock.AGE) == 8, "a mote of 8 steps (640 ticks): " + mote);
		helper.assertTrue(ConcordanceEffects.apply(context(helper, spell, player, ledger, "0/0/1"), light, helper.absolutePos(new BlockPos(2, 1, 2)))
				== ConcordanceEffects.Result.NOTHING, "Never in stone");

		BlockPos lever = new BlockPos(4, 2, 4);
		helper.setBlock(lever, Blocks.LEVER.defaultBlockState().setValue(BlockStateProperties.ATTACH_FACE, AttachFace.FLOOR));
		EffectSpec use = EffectSpec.of(EffectKind.INTERACTION, Intent.HELPFUL, 0, 0);
		helper.assertTrue(ConcordanceEffects.apply(context(helper, cause(null, Cause.Origin.SHRINE), null, open(), "0/0/0"), use,
				helper.absolutePos(lever)) == ConcordanceEffects.Result.NOT_ALLOWED, "Only a player's effect uses a block");
		helper.assertTrue(ConcordanceEffects.apply(context(helper, spell, player, ledger, "0/0/2"), use, helper.absolutePos(lever)).applied()
				&& helper.getBlockState(lever).getValue(BlockStateProperties.POWERED), "A player's effect pulls the lever");

		CropBlock wheat = (CropBlock) Blocks.WHEAT;
		BlockPos ripe = new BlockPos(5, 2, 5);
		BlockPos green = new BlockPos(6, 2, 5);
		helper.setBlock(ripe.below(), Blocks.FARMLAND);
		helper.setBlock(ripe, wheat.getStateForAge(wheat.getMaxAge()));
		helper.setBlock(green.below(), Blocks.FARMLAND);
		helper.setBlock(green, wheat.getStateForAge(3));
		EffectSpec reap = EffectSpec.of(EffectKind.HARVESTING, Intent.HELPFUL, 0, 0);
		helper.assertTrue(ConcordanceEffects.apply(context(helper, spell, player, ledger, "0/0/3"), reap, helper.absolutePos(ripe)).applied(),
				"Ripe wheat is gathered");
		BlockState after = helper.getBlockState(ripe);
		helper.assertTrue(after.isAir() || after.is(Blocks.WHEAT) && after.getValue(BlockStateProperties.AGE_7) == 0,
				"and replanted from its own seed when it dropped one (the kama's rule): " + after);
		helper.assertTrue(ConcordanceEffects.apply(context(helper, spell, player, ledger, "0/0/3"), reap, helper.absolutePos(green))
				== ConcordanceEffects.Result.NOTHING, "Green wheat is left");

		BlockPos fire = new BlockPos(3, 2, 6);
		helper.setBlock(fire, Blocks.FIRE);
		EffectSpec quench = EffectSpec.of(EffectKind.ALTERATION, Intent.HELPFUL, 0, 0);
		helper.assertTrue(ConcordanceEffects.apply(context(helper, spell, player, ledger, "0/0/4"), quench, helper.absolutePos(fire)).applied()
				&& helper.getBlockState(fire).isAir(), "Fire is put out");

		player.setGameMode(GameType.ADVENTURE);
		helper.assertTrue(ConcordanceEffects.apply(context(helper, spell, player, ledger, "0/0/5"), light, helper.absolutePos(new BlockPos(5, 4, 2)))
				== ConcordanceEffects.Result.NOT_ALLOWED, "Not where the actor may not build");
		helper.succeed();
	}

	// ---------------------------------------------------------------- Chunk B, unit 3: Rimebind

	/**
	 * Rimebind binds a creature for 3 s, a fifth slower, and starts a hidden thaw of 5 s; while it thaws, Rimebind is
	 * refused (immune, nothing changed); once the binding is over it still thaws; once the thaw is over it can be bound
	 * again.
	 */
	@GameTest(maxTicks = 120)
	public void rimebindThawsBeforeItBindsAgain(GameTestHelper helper) {
		floor(helper);
		ServerPlayer caster = player(helper, new BlockPos(0, 2, 0));
		Mob pig = helper.spawnWithNoFreeWill(EntityTypes.PIG, new BlockPos(3, 2, 3));
		EffectSpec grip = invoked("jugcraft:rimebind");
		double free = pig.getAttributeValue(Attributes.MOVEMENT_SPEED);
		helper.assertTrue(cast(helper, caster, grip, pig).applied(), "Rimebind takes");
		MobEffectInstance bound = pig.getEffect(Rimebind.RIMEBOUND);
		MobEffectInstance thaw = pig.getEffect(Rimebind.RIME_THAW);
		helper.assertTrue(bound != null && bound.getDuration() == 60 && thaw != null && thaw.getDuration() == 100,
				"Bound for 3 s, thawing for 5: " + bound + " " + thaw);
		helper.assertTrue(!thaw.isVisible() && !thaw.showIcon(), "The thaw is hidden: " + thaw);
		helper.assertTrue(Math.abs(pig.getAttributeValue(Attributes.MOVEMENT_SPEED) - free * (1.0 - Rimebind.SLOW)) < 1.0E-9,
				"A fifth slower: " + pig.getAttributeValue(Attributes.MOVEMENT_SPEED) + " of " + free);
		helper.assertTrue(cast(helper, caster, grip, pig) == ConcordanceEffects.Result.IMMUNE
				&& pig.getEffect(Rimebind.RIMEBOUND).getDuration() == 60 && pig.getEffect(Rimebind.RIME_THAW).getDuration() == 100,
				"A second Rimebind while it thaws is refused and changes nothing");
		helper.runAfterDelay(62, () -> {
			helper.assertTrue(!pig.hasEffect(Rimebind.RIMEBOUND) && pig.hasEffect(Rimebind.RIME_THAW)
					&& Math.abs(pig.getAttributeValue(Attributes.MOVEMENT_SPEED) - free) < 1.0E-9, "After 3 s it is free, but still thawing");
			helper.assertTrue(cast(helper, caster, grip, pig) == ConcordanceEffects.Result.IMMUNE, "and Rimebind is still refused");
		});
		helper.runAfterDelay(102, () -> {
			helper.assertTrue(!pig.hasEffect(Rimebind.RIME_THAW) && cast(helper, caster, grip, pig).applied(), "Thawed, it can be bound again");
			helper.succeed();
		});
	}

	/** What repeatedSlowDoesNotPermanentlyImmobilize watches, tick by tick. */
	private static final class Watch {
		int ticks;
		int bound;
		int run;
		int longest;
		int gap;
		int shortestGap = Integer.MAX_VALUE;
		boolean was;
		boolean ever;
		final List<String> wrong = new ArrayList<>();
	}

	/**
	 * Three casters Rimebind one pig every tick for 15 seconds (09:96; Rimebind only, B-P10): it is never bound more than
	 * 3 s at a stretch, never bound again within 2 s of going free, bound about 3 s in every 5 in all, slowed by a fifth
	 * exactly while bound and not at all otherwise, and never given vanilla Slowness.
	 */
	@GameTest(maxTicks = 320)
	public void repeatedSlowDoesNotPermanentlyImmobilize(GameTestHelper helper) {
		floor(helper);
		List<ServerPlayer> casters = List.of(player(helper, new BlockPos(0, 2, 0)), player(helper, new BlockPos(7, 2, 0)),
				player(helper, new BlockPos(0, 2, 7)));
		Mob pig = helper.spawnWithNoFreeWill(EntityTypes.PIG, new BlockPos(4, 2, 4));
		EffectSpec grip = invoked("jugcraft:rimebind");
		double free = pig.getAttributeValue(Attributes.MOVEMENT_SPEED);
		Watch watch = new Watch();
		helper.onEachTick(() -> {
			if (watch.ticks >= 300) {
				return;
			}
			for (ServerPlayer caster : casters) {
				cast(helper, caster, grip, pig);
			}
			boolean bound = pig.hasEffect(Rimebind.RIMEBOUND);
			double speed = pig.getAttributeValue(Attributes.MOVEMENT_SPEED);
			if (Math.abs(speed - (bound ? free * (1.0 - Rimebind.SLOW) : free)) > 1.0E-9 && watch.wrong.size() < 5) {
				watch.wrong.add("tick " + watch.ticks + ": speed " + speed + (bound ? " bound" : " free"));
			}
			if (pig.hasEffect(MobEffects.SLOWNESS) && watch.wrong.size() < 5) {
				watch.wrong.add("tick " + watch.ticks + ": vanilla Slowness");
			}
			if (bound) {
				if (!watch.was && watch.ever) {
					watch.shortestGap = Math.min(watch.shortestGap, watch.gap);
				}
				watch.ever = true;
				watch.bound++;
				watch.run++;
				watch.longest = Math.max(watch.longest, watch.run);
			} else {
				if (watch.was) {
					watch.gap = 0;
				}
				watch.gap++;
				watch.run = 0;
			}
			watch.was = bound;
			watch.ticks++;
		});
		helper.runAfterDelay(305, () -> {
			helper.assertTrue(watch.ticks == 300 && watch.wrong.isEmpty(), "Slowed only by Rimebind, a fifth, while bound: " + watch.wrong);
			helper.assertTrue(watch.longest <= 61, "Never bound more than 3 s at a stretch: " + watch.longest);
			helper.assertTrue(watch.shortestGap != Integer.MAX_VALUE && watch.shortestGap >= 39, "Free at least 2 s between bindings: "
					+ watch.shortestGap);
			helper.assertTrue(watch.bound >= 150 && watch.bound <= 182, "Bound about 3 s in every 5: " + watch.bound + " of 300 ticks");
			helper.succeed();
		});
	}

	/**
	 * The lair keepers (B-P8): the Yeti King and the Cinder Tyrant bear harmful control as resistant; Vesperine and
	 * Madame Tatterlace are unchanged. A resistant iron golem is bound half as long, and thaws sooner.
	 */
	@GameTest(maxTicks = 20)
	public void onlyTheYetiAndTheTyrantResist(GameTestHelper helper) {
		for (EntityType<?> keeper : List.<EntityType<?>>of(JugcraftYeti.YETI_KING, JugcraftTyrant.CINDER_TYRANT)) {
			helper.assertTrue(keeper.builtInRegistryHolder().is(ConcordanceEffects.RESISTANT) && !keeper.builtInRegistryHolder().is(ConcordanceEffects.IMMUNE),
					keeper + " is resistant");
		}
		for (EntityType<?> other : List.<EntityType<?>>of(JugcraftVesperine.VESPERINE, JugcraftTatterlace.TATTERLACE)) {
			helper.assertTrue(!other.builtInRegistryHolder().is(ConcordanceEffects.RESISTANT) && !other.builtInRegistryHolder().is(ConcordanceEffects.IMMUNE),
					other + " is neither resistant nor immune");
		}
		floor(helper);
		ServerPlayer caster = player(helper, new BlockPos(0, 2, 0));
		Mob golem = helper.spawnWithNoFreeWill(EntityTypes.IRON_GOLEM, new BlockPos(4, 2, 4));
		helper.assertTrue(cast(helper, caster, invoked("jugcraft:rimebind"), golem).applied(), "The golem is bound");
		helper.assertTrue(golem.getEffect(Rimebind.RIMEBOUND).getDuration() == 30 && golem.getEffect(Rimebind.RIME_THAW).getDuration() == 70,
				"for half the time, and thaws 2 s after: " + golem.getEffect(Rimebind.RIMEBOUND) + " " + golem.getEffect(Rimebind.RIME_THAW));
		helper.succeed();
	}

	// ---------------------------------------------------------------- Chunk B, unit 3: Brace

	/**
	 * A zombie's blow knocks a bare pig back 0.4 and a braced one 0.2, half as far (as the benchmark counts it). The
	 * braced pig is still braced for the rest of that tick and not after; with its hurt cooldown cleared, the next blow
	 * knocks it the whole 0.4.
	 */
	@GameTest(maxTicks = 20)
	public void braceHoldsTheWholeBlow(GameTestHelper helper) {
		floor(helper);
		ServerLevel level = helper.getLevel();
		ServerPlayer caster = player(helper, new BlockPos(0, 2, 0));
		Mob bare = helper.spawnWithNoFreeWill(EntityTypes.PIG, new BlockPos(2, 2, 4));
		Mob braced = helper.spawnWithNoFreeWill(EntityTypes.PIG, new BlockPos(5, 2, 4));
		Mob zombie = helper.spawnWithNoFreeWill(EntityTypes.ZOMBIE, new BlockPos(3, 2, 1));
		helper.assertTrue(cast(helper, caster, invoked("jugcraft:brace"), braced).applied(), "The pig is braced");
		bare.setDeltaMovement(Vec3.ZERO);
		braced.setDeltaMovement(Vec3.ZERO);
		bare.hurtServer(level, level.damageSources().mobAttack(zombie), 1.0F);
		braced.hurtServer(level, level.damageSources().mobAttack(zombie), 1.0F);
		double whole = horizontal(bare);
		double held = horizontal(braced);
		helper.assertTrue(near(whole, 0.4) && near(held, 0.2), "Knocked back 0.4 bare and 0.2 braced: " + whole + ", " + held);
		helper.assertTrue(near(held / whole, 1.0 - Brace.RESISTANCE) && near(1.0 - Brace.RESISTANCE, 1.0 - Benchmark.BRACE_RESISTANCE),
				"Half, as the benchmark counts it: " + held / whole);
		helper.assertTrue(braced.hasEffect(Brace.BRACED), "Spent, it still braces the rest of the tick");
		helper.runAfterDelay(1, () -> {
			helper.assertTrue(!braced.hasEffect(Brace.BRACED), "and is gone after it");
			braced.damageCooldownTime = 0;
			braced.setDeltaMovement(Vec3.ZERO);
			braced.hurtServer(level, level.damageSources().mobAttack(zombie), 1.0F);
			helper.assertTrue(near(horizontal(braced), 0.4), "The next blow knocks it the whole way: " + horizontal(braced));
			helper.succeed();
		});
	}

	/**
	 * A scripted push may ask for the Brace before the blow lands (the Yeti King's, B-09): it is spent but still braces the
	 * blow that follows in the same tick, and asking again in that tick gives the same; the next tick it is gone.
	 */
	@GameTest(maxTicks = 20)
	public void braceIsTakenBeforeTheBlow(GameTestHelper helper) {
		floor(helper);
		ServerLevel level = helper.getLevel();
		ServerPlayer caster = player(helper, new BlockPos(0, 2, 0));
		Mob pig = helper.spawnWithNoFreeWill(EntityTypes.PIG, new BlockPos(4, 2, 4));
		Mob zombie = helper.spawnWithNoFreeWill(EntityTypes.ZOMBIE, new BlockPos(4, 2, 1));
		helper.assertTrue(cast(helper, caster, invoked("jugcraft:brace"), pig).applied(), "The pig is braced");
		helper.assertTrue(near(Brace.spend(pig), 0.5), "Asked first, the Brace lets half through");
		pig.setDeltaMovement(Vec3.ZERO);
		pig.hurtServer(level, level.damageSources().mobAttack(zombie), 1.0F);
		helper.assertTrue(near(horizontal(pig), 0.2), "and the blow after it is still braced: " + horizontal(pig));
		helper.assertTrue(near(Brace.spend(pig), 0.5), "Asked again in the same tick, the same");
		helper.runAfterDelay(1, () -> {
			helper.assertTrue(near(Brace.spend(pig), 1.0) && !pig.hasEffect(Brace.BRACED), "The next tick it is gone");
			helper.succeed();
		});
	}

	/**
	 * A shrine's harmful push of 12 moves a braced pig 0.6, half, and spends the Brace. A creature's own Flashstep (a
	 * helpful push, its actor its target) carries a braced pig the whole 1.2 and leaves the Brace on (B-P12).
	 */
	@GameTest(maxTicks = 20)
	public void harmfulPushesSpendTheBraceAndHelpfulOnesDoNot(GameTestHelper helper) {
		floor(helper);
		ServerLevel level = helper.getLevel();
		ServerPlayer caster = player(helper, new BlockPos(0, 2, 0));
		Mob pushed = helper.spawnWithNoFreeWill(EntityTypes.PIG, new BlockPos(2, 2, 4));
		Mob stepping = helper.spawnWithNoFreeWill(EntityTypes.PIG, new BlockPos(5, 2, 4));
		EffectSpec bracing = invoked("jugcraft:brace");
		helper.assertTrue(cast(helper, caster, bracing, pushed).applied() && cast(helper, caster, bracing, stepping).applied(), "Both pigs are braced");
		EffectSpec shove = EffectSpec.of(EffectKind.MOVEMENT, Intent.HARMFUL, 12, 0);
		ConcordanceEffects.Context shrine = new ConcordanceEffects.Context(level, cause(null, Cause.Origin.SHRINE), null, open(), "0/0/0",
				Vec3.atCenterOf(helper.absolutePos(new BlockPos(2, 2, 1))));
		pushed.setDeltaMovement(Vec3.ZERO);
		helper.assertTrue(ConcordanceEffects.apply(shrine, shove, pushed).applied() && near(horizontal(pushed), 0.6),
				"A harmful push of 12 moves a braced pig 0.6: " + horizontal(pushed));
		EffectSpec flash = invoked("jugcraft:flashstep");
		ConcordanceEffects.Context own = new ConcordanceEffects.Context(level, cause(stepping, Cause.Origin.CREATURE), stepping, open(), "0/0/0",
				stepping.position());
		stepping.setDeltaMovement(Vec3.ZERO);
		helper.assertTrue(ConcordanceEffects.apply(own, flash, stepping).applied() && near(horizontal(stepping), 1.2),
				"Its own Flashstep carries a braced pig the whole 1.2: " + horizontal(stepping));
		helper.assertTrue(near(stepping.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE), Brace.RESISTANCE),
				"and its Brace is still on it: " + stepping.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE));
		helper.runAfterDelay(1, () -> {
			helper.assertTrue(!pushed.hasEffect(Brace.BRACED), "The harmful push spent its Brace");
			helper.assertTrue(stepping.hasEffect(Brace.BRACED), "Its own step did not");
			helper.succeed();
		});
	}

	/**
	 * A Brace stops no lair's own way out (09:96): a braced survival player in a lair visit who uses the Grey Mist is sent
	 * exactly home and the visit ends; still braced, since going home is no blow.
	 */
	@GameTest(maxTicks = 20)
	public void braceDoesNotCancelBoundaryEnforcement(GameTestHelper helper) {
		floor(helper);
		ServerLevel level = helper.getLevel();
		ServerPlayer player = player(helper, new BlockPos(1, 2, 1));
		helper.assertTrue(cast(helper, player, invoked("jugcraft:brace"), player).applied(), "The player braces");
		BlockPos home = helper.absolutePos(new BlockPos(5, 2, 5));
		Vec3 back = new Vec3(home.getX() + 0.5, home.getY(), home.getZ() + 0.5);
		player.setAttached(Lairs.VISIT, new LairVisit("glacier_hall", 0, UUID.randomUUID(), level.dimension().identifier().toString(), back.x,
				back.y, back.z, 90.0F, 0.0F));
		BlockPos exit = new BlockPos(1, 2, 2);
		helper.setBlock(exit, JugcraftLairs.LAIR_EXIT);
		BlockPos at = helper.absolutePos(exit);
		level.getBlockState(at).useWithoutItem(level, player, new BlockHitResult(Vec3.atCenterOf(at), Direction.NORTH, at, false));
		helper.assertTrue(player.position().distanceTo(back) < 1.0E-6 && player.getAttached(Lairs.VISIT) == null,
				"The Grey Mist sends a braced player exactly home: " + player.position() + " " + player.getAttached(Lairs.VISIT));
		helper.runAfterDelay(1, () -> {
			helper.assertTrue(player.hasEffect(Brace.BRACED), "Going home is no blow: still braced");
			helper.succeed();
		});
	}

	/**
	 * Bound and braced, a pig is saved and loaded into a new pig: all three effects come back the same (time, level,
	 * whether shown), with their attribute changes, and the copy still thaws: Rimebind is refused.
	 */
	@GameTest(maxTicks = 20)
	public void rimeAndBraceSurviveASave(GameTestHelper helper) {
		floor(helper);
		ServerLevel level = helper.getLevel();
		ServerPlayer caster = player(helper, new BlockPos(0, 2, 0));
		Mob pig = helper.spawnWithNoFreeWill(EntityTypes.PIG, new BlockPos(3, 2, 3));
		EffectSpec grip = invoked("jugcraft:rimebind");
		helper.assertTrue(cast(helper, caster, grip, pig).applied() && cast(helper, caster, invoked("jugcraft:brace"), pig).applied(),
				"The pig is bound and braced");
		TagValueOutput saved = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, level.registryAccess());
		pig.saveWithoutId(saved);
		Mob copy = EntityTypes.PIG.create(level, EntitySpawnReason.COMMAND);
		helper.assertTrue(copy != null, "A new pig");
		copy.load(TagValueInput.create(ProblemReporter.DISCARDING, level.registryAccess(), saved.buildResult()));
		for (Holder<MobEffect> effect : List.of(Rimebind.RIMEBOUND, Rimebind.RIME_THAW, Brace.BRACED)) {
			MobEffectInstance was = pig.getEffect(effect);
			MobEffectInstance is = copy.getEffect(effect);
			helper.assertTrue(was != null && is != null && is.getDuration() == was.getDuration() && is.getAmplifier() == was.getAmplifier()
					&& is.isVisible() == was.isVisible() && is.showIcon() == was.showIcon(), effect.getRegisteredName() + " survives the save: "
							+ was + " -> " + is);
		}
		AttributeModifier slow = copy.getAttribute(Attributes.MOVEMENT_SPEED).getModifier(Jugcraft.id("rimebound"));
		AttributeModifier steady = copy.getAttribute(Attributes.KNOCKBACK_RESISTANCE).getModifier(Jugcraft.id("braced"));
		helper.assertTrue(slow != null && Math.abs(slow.amount() + Rimebind.SLOW) < 1.0E-9 && steady != null
				&& Math.abs(steady.amount() - Brace.RESISTANCE) < 1.0E-9, "and so do their attribute changes: " + slow + " " + steady);
		helper.assertTrue(cast(helper, caster, grip, copy) == ConcordanceEffects.Result.IMMUNE, "The copy still thaws: Rimebind is refused");
		helper.succeed();
	}

	// ---------------------------------------------------------------- Chunk B, unit 3: Preserve

	/**
	 * Preserve removes nothing and changes nothing but a crucible's hold: fire, a lit and an unlit campfire, a chest,
	 * stone and air are left exactly as they were. The crucible is held, the same block entity, its bottles still in it.
	 */
	@GameTest(maxTicks = 20)
	public void preserveNeverRemovesABlock(GameTestHelper helper) {
		floor(helper);
		ServerLevel level = helper.getLevel();
		ServerPlayer player = player(helper, new BlockPos(0, 2, 0));
		EffectSpec preserve = invoked("jugcraft:preserve");
		BlockPos crucibleAt = new BlockPos(2, 2, 2);
		helper.setBlock(crucibleAt, JugcraftConcordance.CRUCIBLE);
		CrucibleBlockEntity crucible = (CrucibleBlockEntity) level.getBlockEntity(helper.absolutePos(crucibleAt));
		crucible.setItem(CrucibleBlockEntity.BOTTLE_SLOT, new ItemStack(Items.GLASS_BOTTLE, 3));
		helper.setBlock(new BlockPos(4, 2, 2), Blocks.FIRE);
		helper.setBlock(new BlockPos(5, 2, 2), Blocks.CAMPFIRE);
		helper.setBlock(new BlockPos(6, 2, 2), Blocks.CAMPFIRE.defaultBlockState().setValue(CampfireBlock.LIT, false));
		helper.setBlock(new BlockPos(4, 2, 4), Blocks.CHEST);
		helper.setBlock(new BlockPos(5, 2, 4), Blocks.STONE);
		helper.setBlock(new BlockPos(6, 2, 4), Blocks.AIR);
		for (BlockPos at : List.of(new BlockPos(4, 2, 2), new BlockPos(5, 2, 2), new BlockPos(6, 2, 2), new BlockPos(4, 2, 4), new BlockPos(5, 2, 4),
				new BlockPos(6, 2, 4))) {
			BlockState before = helper.getBlockState(at);
			ConcordanceEffects.Result result = cast(helper, player, preserve, at);
			helper.assertTrue(result == ConcordanceEffects.Result.NOTHING && helper.getBlockState(at) == before,
					"Preserve leaves " + before + " exactly as it was: " + result + ", " + helper.getBlockState(at));
		}
		long now = level.getGameTime();
		helper.assertTrue(cast(helper, player, preserve, crucibleAt).applied(), "The crucible is held");
		helper.assertTrue(helper.getBlockState(crucibleAt).is(JugcraftConcordance.CRUCIBLE) && level.getBlockEntity(helper.absolutePos(crucibleAt)) == crucible
				&& crucible.getItem(CrucibleBlockEntity.BOTTLE_SLOT).getCount() == 3 && crucible.held(now) == Rime.HOLD_TICKS,
				"the same crucible, its bottles in it, held for a minute: " + crucible.held(now));
		helper.succeed();
	}

	/**
	 * Preserve holds a crucible only where its caster could change it by hand: not in a neighbour's claim (though one in
	 * the open beside it is held), not for a caster who is away, and not in adventure mode.
	 */
	@GameTest(maxTicks = 20)
	public void preserveIsRefusedWhereItsCasterMayNotBuild(GameTestHelper helper) {
		floor(helper);
		ServerLevel level = helper.getLevel();
		ServerPlayer player = player(helper, new BlockPos(0, 2, 0));
		EffectSpec preserve = invoked("jugcraft:preserve");
		BlockPos theirsAt = new BlockPos(2, 2, 2);
		BlockPos freeAt = new BlockPos(5, 2, 2);
		helper.setBlock(theirsAt, JugcraftConcordance.CRUCIBLE);
		helper.setBlock(freeAt, JugcraftConcordance.CRUCIBLE);
		CrucibleBlockEntity theirs = (CrucibleBlockEntity) level.getBlockEntity(helper.absolutePos(theirsAt));
		CrucibleBlockEntity free = (CrucibleBlockEntity) level.getBlockEntity(helper.absolutePos(freeAt));
		try (TestClaims claims = TestClaims.open()) {
			claims.block(helper.absolutePos(theirsAt), NEIGHBOUR);
			helper.assertTrue(cast(helper, player, preserve, theirsAt) == ConcordanceEffects.Result.NOT_ALLOWED && theirs.heldUntil() == 0L,
					"Not in a neighbour's claim");
			helper.assertTrue(cast(helper, player, preserve, freeAt).applied(), "but in the open beside it");
		}
		long now = level.getGameTime();
		free.hold(now, 0);
		ConcordanceEffects.Context away = new ConcordanceEffects.Context(level, Cause.of(AWAY, Cause.Origin.INVOCATION, "jugcraft:preserve",
				ConcordanceEffects.nextSerial()), null, open(), "0/0/0", Vec3.atCenterOf(helper.absolutePos(freeAt)));
		helper.assertTrue(ConcordanceEffects.apply(away, preserve, helper.absolutePos(freeAt)) == ConcordanceEffects.Result.NOT_ALLOWED
				&& free.heldUntil() == 0L, "Not for a caster who is away");
		player.setGameMode(GameType.ADVENTURE);
		helper.assertTrue(cast(helper, player, preserve, freeAt) == ConcordanceEffects.Result.NOT_ALLOWED && free.heldUntil() == 0L,
				"Not in adventure mode");
		helper.succeed();
	}
}
