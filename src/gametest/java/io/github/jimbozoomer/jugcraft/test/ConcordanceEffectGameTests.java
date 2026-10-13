package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.concordance.ConcordanceEffects;
import io.github.jimbozoomer.jugcraft.concordance.JugcraftConcordance;
import io.github.jimbozoomer.jugcraft.concordance.LumenMoteBlock;
import io.github.jimbozoomer.jugcraft.concordance.effect.Cause;
import io.github.jimbozoomer.jugcraft.concordance.effect.EffectKind;
import io.github.jimbozoomer.jugcraft.concordance.effect.EffectSpec;
import io.github.jimbozoomer.jugcraft.concordance.effect.Intent;
import io.github.jimbozoomer.jugcraft.concordance.effect.Ledger;
import io.github.jimbozoomer.jugcraft.concordance.effect.Stacking;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Roadmap step 9, the shared effect boundary ({@link ConcordanceEffects}): the same effect does the same thing whatever
 * delivers it, every application is attributed to its cause (triggered ones too), friendly fire, tolerance and
 * protection apply alike, lasting effects stack and expire by one rule, an event's ledger counts each application once,
 * and the block operations act only where the actor may.
 */
public class ConcordanceEffectGameTests {
	private static final EffectSpec SEAR = new EffectSpec(EffectKind.DAMAGE, Intent.HARMFUL, 4, 0, null, Stacking.STRONGEST, "spell_power:arcane");
	private static final EffectSpec DAZZLE = new EffectSpec(EffectKind.STATUS, Intent.HARMFUL, 0, 60, "minecraft:slowness",
			Stacking.STRONGEST, null);
	private static final EffectSpec WARD = EffectSpec.of(EffectKind.PROTECTION, Intent.HELPFUL, 4, 200);

	private static void floor(GameTestHelper helper) {
		for (int x = 0; x <= 7; x++) {
			for (int z = 0; z <= 7; z++) {
				helper.setBlock(new BlockPos(x, 1, z), Blocks.STONE);
			}
		}
	}

	private static ServerPlayer player(GameTestHelper helper, BlockPos standAt) {
		ServerPlayer player = helper.makeMockServerPlayerInLevel();
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
}
