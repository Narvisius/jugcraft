package io.github.jimbozoomer.jugcraft.test;

import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import io.github.jimbozoomer.jugcraft.concordance.Brew;
import io.github.jimbozoomer.jugcraft.concordance.ConcordanceEffects;
import io.github.jimbozoomer.jugcraft.concordance.JugcraftConcordance;
import io.github.jimbozoomer.jugcraft.concordance.RateGate;
import io.github.jimbozoomer.jugcraft.concordance.effect.Cause;
import io.github.jimbozoomer.jugcraft.concordance.effect.EffectKind;
import io.github.jimbozoomer.jugcraft.concordance.effect.EffectSpec;
import io.github.jimbozoomer.jugcraft.concordance.effect.Intent;
import io.github.jimbozoomer.jugcraft.concordance.effect.Ledger;
import io.github.jimbozoomer.jugcraft.concordance.effect.Stacking;
import io.github.jimbozoomer.jugcraft.concordance.rime.Frostward;
import io.github.jimbozoomer.jugcraft.lair.Encounter;
import io.github.jimbozoomer.jugcraft.lair.LairRewards;
import io.github.jimbozoomer.jugcraft.lair.tyrant.CinderTyrantLoot;
import io.github.jimbozoomer.jugcraft.lair.tyrant.JugcraftTyrant;
import io.github.jimbozoomer.jugcraft.lair.vesperine.JugcraftVesperine;
import io.github.jimbozoomer.jugcraft.lair.vesperine.VesperineLoot;
import io.github.jimbozoomer.jugcraft.lair.yeti.JugcraftYeti;
import io.github.jimbozoomer.jugcraft.lair.yeti.YetiKingLoot;
import java.util.List;
import java.util.UUID;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.phys.Vec3;

/**
 * Concordance Chunk A (A-05, A-06): a lair fight's credit for support, and its rewards' lifecycle, with a pig standing
 * in for the boss (so no boss acts). Healing counts only the fight's own hurt, healed in someone else, up to what the
 * fight did and nothing healed otherwise since; a ward counts only what it soaked of the fight's blows; overhealing,
 * healing yourself, a friend's blow, a fall and a ward that soaks nothing count for nothing; a tamed creature's blow
 * is its owner's; a fight left forgets who took part; one who left the game and came back is paid like anyone else;
 * and a reward kept for a player is paid once, whether it waited with them or in the world while they were away, and
 * survives a save either way, and is paid by its boss's payer. The bosses' own deeds and falls are in
 * {@link VesperineGameTests} and {@link TatterlaceGameTests}, and since Concordance Chunk B {@link YetiKingGameTests} and
 * {@link CinderTyrantGameTests}. Since Concordance Chunk C (C-02), a Stillroot Poultice put on a friend in the fight
 * credits its helper with each heal its Regeneration gives (the patient's own tick run by hand: a mock player has no
 * client to tick it), and nothing else that heals, nor a poultice on oneself, earns that credit.
 */
public class LairEncounterGameTests {
	/**
	 * A new player cannot be hurt until its client has loaded the world, or for 60 ticks; a mock player has no client,
	 * so a test that hurts one waits this long first.
	 */
	private static final int LOADING_TICKS = 80;
	private static final EffectSpec MEND = EffectSpec.of(EffectKind.RESTORATION, Intent.HELPFUL, 4, 0);
	private static final EffectSpec WARD = EffectSpec.of(EffectKind.PROTECTION, Intent.HELPFUL, 4, 200);

	private static ServerPlayer player(GameTestHelper helper, int x, int z) {
		ServerPlayer player = TestPlayers.mock(helper);
		player.setGameMode(GameType.SURVIVAL);
		BlockPos at = helper.absolutePos(new BlockPos(x, 2, z));
		player.snapTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, 0.0F, 0.0F);
		return player;
	}

	/** A pig that stands in for a boss, and its fight, begun. */
	private static Encounter fight(Mob boss) {
		Encounter fight = new Encounter(boss, boss::position);
		fight.begin();
		return fight;
	}

	/** {@code helper} applies {@code spec} to {@code patient} through the Concordance's effect boundary. */
	private static ConcordanceEffects.Result help(GameTestHelper helper, ServerPlayer from, LivingEntity patient, EffectSpec spec) {
		Cause cause = Cause.of(from.getUUID(), Cause.Origin.SPELL, "test:support", ConcordanceEffects.nextSerial());
		ConcordanceEffects.Context context = new ConcordanceEffects.Context(helper.getLevel(), cause, from,
				new Ledger(new Ledger.Limits(16, 256, 0)), "support", from.position());
		return ConcordanceEffects.apply(context, spec, patient);
	}

	/**
	 * A blow from {@code source} (the fight, a friend or nobody) to {@code player}, taken whole: the hurt cooldown (26.3's
	 * public {@code damageCooldownTime}) is cleared first, so blows in the same tick all land.
	 */
	private static void blow(ServerLevel level, ServerPlayer player, DamageSource source, float amount) {
		player.damageCooldownTime = 0;
		player.hurtServer(level, source, amount);
	}

	/** A Stillroot Poultice as the crucible bottles it (C-02): a salve of Regeneration II for 450 ticks, nothing harmful. */
	private static ItemStack poultice() {
		return salve(Encounter.POULTICE_STATUS + "@1", new Brew.Dose(Encounter.POULTICE_STATUS, 1, 450, false));
	}

	private static ItemStack salve(String outcome, Brew.Dose... doses) {
		ItemStack salve = new ItemStack(JugcraftConcordance.SALVE);
		salve.set(JugcraftConcordance.BREW, new Brew(List.of(doses), outcome));
		return salve;
	}

	/** {@code from} puts {@code salve} on {@code patient} with a use on them, as a player does (its rate limit cleared). */
	private static void apply(ServerPlayer from, LivingEntity patient, ItemStack salve) {
		RateGate.forget(from.getUUID());
		from.setItemInHand(InteractionHand.MAIN_HAND, salve);
		salve.getItem().interactLivingEntity(salve, from, patient, InteractionHand.MAIN_HAND);
	}

	/** {@code ticks} of the patient's own tick, where effects tick, run by hand: a mock player has no client to tick it. */
	private static void live(ServerPlayer patient, int ticks) {
		for (int i = 0; i < ticks; i++) {
			patient.baseTick();
		}
	}

	private static float healing(Encounter fight, ServerPlayer player) {
		Encounter.Share share = fight.share(player);
		return share == null ? 0.0F : share.healing();
	}

	/**
	 * Runs {@code steps} a tick apart, so each tick's end reads the fight's poultices between them, then succeeds. The
	 * fight ends and the boss goes once the last step has run, or as soon as one fails.
	 */
	private static void timeline(GameTestHelper helper, Encounter fight, Mob boss, Runnable... steps) {
		step(helper, fight, boss, steps, 0);
	}

	private static void step(GameTestHelper helper, Encounter fight, Mob boss, Runnable[] steps, int at) {
		boolean last = at == steps.length - 1;
		boolean ran = false;
		try {
			steps[at].run();
			ran = true;
		} finally {
			if (last || !ran) {
				fight.end();
				boss.discard();
			}
		}
		if (last) {
			helper.succeed();
		} else {
			helper.runAfterDelay(1, () -> step(helper, fight, boss, steps, at + 1));
		}
	}

	/**
	 * The fight hurts a player (6, as the world's difficulty scales a creature's blow); a healer mends them 4 and 4 again,
	 * credited only up to the fight's hurt. Healing them past full health, healing what a friend's blow or a fall did, and
	 * healing yourself (even the fight's own hurt) earn nothing. The healer qualifies; the hurt player does not.
	 */
	@GameTest(maxTicks = 120)
	public void healingCountsOnlyTheFightsHurt(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		Mob boss = helper.spawnWithNoFreeWill(EntityTypes.PIG, new BlockPos(2, 2, 2));
		Encounter fight = fight(boss);
		ServerPlayer patient = player(helper, 1, 1);
		ServerPlayer healer = player(helper, 3, 1);
		ServerPlayer friend = player(helper, 1, 3);
		helper.runAfterDelay(LOADING_TICKS, () -> {
			try {
				blow(level, patient, level.damageSources().mobAttack(boss), 6.0F);
				float lost = patient.getMaxHealth() - patient.getHealth();
				help(helper, healer, patient, MEND);
				Encounter.Share once = fight.share(healer);
				helper.assertTrue(lost >= Encounter.HEALING && once != null && once.healing() == Math.min(4.0F, lost),
						"The fight's hurt (" + lost + ") healed is credited: " + (once == null ? "nothing" : once.healing()));
				help(helper, healer, patient, MEND);
				help(helper, healer, patient, MEND);
				blow(level, patient, level.damageSources().playerAttack(friend), 4.0F);
				float beforeFall = patient.getHealth();
				blow(level, patient, level.damageSources().fall(), 6.0F);
				helper.assertTrue(patient.getHealth() < beforeFall, "The fall lands");
				helper.assertTrue(help(helper, healer, patient, MEND).applied(), "The healer mends what the fall and a friend did");
				helper.assertTrue(help(helper, patient, patient, MEND).applied(), "and the patient mends themselves the rest");
				Encounter.Share share = fight.share(healer);
				helper.assertTrue(share != null && share.healing() == Math.min(12.0F, lost) && share.qualifies(),
						"Only the fight's " + lost + " is credited, whatever else is healed: " + (share == null ? "nothing" : share.healing()));
				// Healing yourself the fight's own hurt earns nothing either.
				blow(level, patient, level.damageSources().mobAttack(boss), 6.0F);
				helper.assertTrue(help(helper, patient, patient, MEND).applied(), "The patient mends the fight's hurt themselves");
				Encounter.Share hurt = fight.share(patient);
				helper.assertTrue(hurt == null || !hurt.qualifies(), "Being hurt, and healing yourself, qualifies no one");
				helper.assertTrue(fight.qualified().contains(healer.getUUID()) && !fight.qualified().contains(patient.getUUID())
						&& !fight.qualified().contains(friend.getUUID()), "The healer qualifies, no one else here");
			} finally {
				fight.end();
				boss.discard();
			}
			helper.succeed();
		});
	}

	/**
	 * What the fight hurt but something else healed since (rest, food, a potion: here the player's own healing) is no
	 * longer the fight's: healing a fall that came after it earns nothing.
	 */
	@GameTest(maxTicks = 120)
	public void healingWhatRestHealedEarnsNothing(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		Mob boss = helper.spawnWithNoFreeWill(EntityTypes.PIG, new BlockPos(2, 2, 2));
		Encounter fight = fight(boss);
		ServerPlayer patient = player(helper, 1, 1);
		ServerPlayer healer = player(helper, 3, 1);
		helper.runAfterDelay(LOADING_TICKS, () -> {
			try {
				blow(level, patient, level.damageSources().mobAttack(boss), 6.0F);
				helper.assertTrue(patient.getHealth() < patient.getMaxHealth(), "The fight hurt them");
				patient.heal(patient.getMaxHealth());
				blow(level, patient, level.damageSources().fall(), 4.0F);
				helper.assertTrue(help(helper, healer, patient, MEND).applied(), "The fall is healed");
				Encounter.Share share = fight.share(healer);
				helper.assertTrue(share == null || (share.healing() == 0.0F && !share.qualifies()),
						"Healing a fall after the fight's hurt was healed otherwise earns nothing: " + (share == null ? "nothing" : share.healing()));
			} finally {
				fight.end();
				boss.discard();
			}
			helper.succeed();
		});
	}

	/**
	 * A ward is credited only for what it soaks of the fight's blows: part of its 4 in one blow and the rest in the next,
	 * so its caster qualifies. A ward no blow reaches earns nothing; a ward on yourself, nothing.
	 */
	@GameTest(maxTicks = 120)
	public void aWardIsCreditedForWhatItSoaks(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		Mob boss = helper.spawnWithNoFreeWill(EntityTypes.PIG, new BlockPos(2, 2, 2));
		Encounter fight = fight(boss);
		ServerPlayer patient = player(helper, 1, 1);
		ServerPlayer warder = player(helper, 3, 1);
		ServerPlayer idle = player(helper, 1, 3);
		ServerPlayer untouched = player(helper, 3, 3);
		helper.runAfterDelay(LOADING_TICKS, () -> {
			help(helper, warder, patient, WARD);
			help(helper, idle, untouched, WARD);
			help(helper, patient, patient, WARD);
			// Absorption fills on the effect's first tick.
			helper.runAfterDelay(2, () -> {
				try {
					blow(level, patient, level.damageSources().mobAttack(boss), 3.0F);
					Encounter.Share first = fight.share(warder);
					helper.assertTrue(first != null && first.warding() > 0.0F && first.warding() <= 4.0F, "Some soaked of the first blow: "
							+ (first == null ? "nothing" : first.warding()));
					blow(level, patient, level.damageSources().mobAttack(boss), 3.0F);
					Encounter.Share share = fight.share(warder);
					Encounter.Share none = fight.share(idle);
					helper.assertTrue(share != null && share.warding() == 4.0F && share.qualifies(), "and the rest of the 4 in the next: "
							+ (share == null ? "nothing" : share.warding()));
					helper.assertTrue(none == null || (none.warding() == 0.0F && !none.qualifies()), "A ward no blow reaches earns nothing");
					helper.assertTrue(!fight.qualified().contains(patient.getUUID()), "A ward on yourself earns nothing");
				} finally {
					fight.end();
					boss.discard();
				}
				helper.succeed();
			});
		});
	}

	/** A blow is its striker's: a player's own, or their tamed wolf's; a wild wolf's and a fall's are no one's. */
	@GameTest
	public void aTamedCreaturesBlowIsItsOwners(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		ServerPlayer owner = player(helper, 1, 1);
		var wolf = helper.spawnWithNoFreeWill(EntityTypes.WOLF, new BlockPos(3, 2, 1));
		var wild = helper.spawnWithNoFreeWill(EntityTypes.WOLF, new BlockPos(3, 2, 3));
		wolf.tame(owner);
		try {
			helper.assertTrue(Encounter.striker(level.damageSources().playerAttack(owner)) == owner, "A player's own blow is theirs");
			helper.assertTrue(Encounter.striker(level.damageSources().mobAttack(wolf)) == owner, "Their tamed wolf's blow is theirs");
			helper.assertTrue(Encounter.striker(level.damageSources().mobAttack(wild)) == null, "A wild wolf's blow is no one's");
			helper.assertTrue(Encounter.striker(level.damageSources().fall()) == null, "A fall is no one's");
		} finally {
			wolf.discard();
			wild.discard();
		}
		helper.succeed();
	}

	/**
	 * A fight left (the boss went back to its seat) forgets everyone's part, so a fall reported after it pays no one. A
	 * fight's fall is claimed once however often it is reported, and the next fight is a new one, claimed in its turn.
	 */
	@GameTest
	public void aLeftFightPaysNoOne(GameTestHelper helper) {
		Mob boss = helper.spawnWithNoFreeWill(EntityTypes.PIG, new BlockPos(2, 2, 2));
		Encounter fight = fight(boss);
		ServerPlayer striker = player(helper, 1, 1);
		try {
			fight.damage(striker, 4.0F);
			fight.excuse(striker.getUUID());
			fight.left(striker.getUUID());
			UUID first = fight.fight();
			fight.abandon();
			helper.assertTrue(fight.qualified().isEmpty() && !fight.excused(striker.getUUID()) && !fight.away(striker.getUUID())
					&& !fight.underway(),
					"Left, the fight forgets who took part");
			helper.assertTrue(fight.claim() && !fight.claim(), "Its fall is claimed once");
			fight.begin();
			helper.assertTrue(!fight.fight().equals(first) && fight.underway(), "The next fight is a new one");
			fight.damage(striker, 4.0F);
			helper.assertTrue(fight.qualified().contains(striker.getUUID()) && fight.claim() && !fight.claim() && !fight.underway(),
					"and is claimed once in its turn");
		} finally {
			fight.end();
			boss.discard();
		}
		helper.succeed();
	}

	/**
	 * One who left the game in the fight and came back before the boss fell is paid like anyone else (here, alive and
	 * out of its reach: nothing); one who died in it is paid wherever they stand. The experience is shared among those
	 * paid.
	 */
	@GameTest
	public void oneWhoCameBackIsPaidLikeAnyone(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		Mob boss = helper.spawnWithNoFreeWill(EntityTypes.PIG, new BlockPos(2, 2, 2));
		Encounter fight = fight(boss);
		ServerPlayer back = player(helper, 1, 1);
		ServerPlayer fell = player(helper, 3, 1);
		try {
			fight.damage(back, 2.0F);
			fight.damage(fell, 2.0F);
			fight.left(back.getUUID());
			fight.excuse(fell.getUUID());
			back.snapTo(back.getX() + 200.0, back.getY(), back.getZ(), 0.0F, 0.0F);
			fell.snapTo(fell.getX() - 200.0, fell.getY(), fell.getZ(), 0.0F, 0.0F);
			Vec3 centre = boss.position();
			helper.assertTrue(fight.claim(), "The fall is claimed");
			int before = fell.totalExperience;
			List<ServerPlayer> paid = LairRewards.pay(level, VesperineLoot.BOSS, fight, centre, centre, 300);
			helper.assertTrue(paid.equals(List.of(fell)) && fell.getInventory().countItem(JugcraftVesperine.REAPER_SHADE) >= 3,
					"One who died in the fight is paid wherever they stand: " + paid);
			helper.assertTrue(back.getInventory().countItem(JugcraftVesperine.REAPER_SHADE) == 0,
					"One who came back and walked away alive is not");
			helper.assertTrue(fell.totalExperience - before == 300, "The experience goes to those paid: " + (fell.totalExperience - before));
		} finally {
			fight.end();
			boss.discard();
		}
		helper.succeed();
	}

	/**
	 * A reward kept for a player is paid once. In the world's record while they were away (kept once for a fight,
	 * however often it was owed), it is handed over when they join; with them (as for a player who fell), when they are
	 * alive. Neither is paid twice.
	 */
	@GameTest(maxTicks = 20)
	public void aKeptRewardIsPaidOnce(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		ServerPlayer away = player(helper, 1, 1);
		ServerPlayer fallen = player(helper, 3, 1);
		Vec3 origin = Vec3.atCenterOf(helper.absolutePos(new BlockPos(2, 2, 2)));
		LairRewards.Data data = LairRewards.Data.of(level.getServer());
		UUID fight = UUID.randomUUID();
		data.owe(away.getUUID(), new LairRewards.Due(VesperineLoot.BOSS, fight, origin, 50));
		data.owe(away.getUUID(), new LairRewards.Due(VesperineLoot.BOSS, fight, origin, 50));
		helper.assertTrue(data.kept(away.getUUID()).size() == 1, "One reward is kept a fight, however often it is owed");
		LairRewards.welcome(away);
		int shades = away.getInventory().countItem(JugcraftVesperine.REAPER_SHADE);
		helper.assertTrue(shades >= 3 && data.kept(away.getUUID()).isEmpty() && !away.hasAttached(LairRewards.OWED),
				"Joining, they are handed it, and the world keeps it no more: " + shades + " Reaper's Shade");
		LairRewards.welcome(away);
		helper.assertTrue(away.getInventory().countItem(JugcraftVesperine.REAPER_SHADE) == shades, "Joining again pays nothing more");

		LairRewards.owe(fallen, new LairRewards.Due(VesperineLoot.BOSS, UUID.randomUUID(), origin, 50));
		helper.assertTrue(fallen.hasAttached(LairRewards.OWED) && LairRewards.handOver(fallen)
				&& fallen.getInventory().countItem(JugcraftVesperine.REAPER_SHADE) >= 3 && !fallen.hasAttached(LairRewards.OWED),
				"A reward kept with a player is handed over once they are alive");
		helper.assertTrue(!LairRewards.handOver(fallen), "and only once");
		helper.succeed();
	}

	/**
	 * What is kept survives a save (A-08's last step). A reward kept with a player is saved with them and kept through
	 * death: written out and loaded into the same player joining again, it is owed once. The world's record, written and
	 * read back, keeps it for the same player. Both read their saves from before versions.
	 */
	@GameTest(maxTicks = 20)
	public void keptRewardsSurviveASave(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		ServerPlayer fallen = player(helper, 1, 1);
		Vec3 origin = Vec3.atCenterOf(helper.absolutePos(new BlockPos(2, 2, 2)));
		LairRewards.Due due = new LairRewards.Due(VesperineLoot.BOSS, UUID.randomUUID(), origin, 50);
		helper.assertTrue(LairRewards.OWED.isPersistent() && LairRewards.OWED.copyOnDeath(),
				"What a player is owed is saved with them and kept through death");
		LairRewards.owe(fallen, due);
		TagValueOutput out = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, level.registryAccess());
		fallen.saveWithoutId(out);
		// The same player joining again: a new player loaded from that save, as the player list loads one on joining.
		ServerPlayer again = new ServerPlayer(level.getServer(), level, fallen.getGameProfile(), fallen.clientInformation());
		again.load(TagValueInput.create(ProblemReporter.DISCARDING, level.registryAccess(), out.buildResult()));
		// Making it bound the player's advancements to it; they go back to the player in the level.
		level.getServer().getPlayerList().getPlayerAdvancements(fallen);
		LairRewards.Owed owed = again.getAttached(LairRewards.OWED);
		helper.assertTrue(owed != null && owed.due().equals(List.of(due)), "Saved and loaded, the player is owed it once: " + owed);
		JsonElement owedSaved = LairRewards.Owed.CODEC.encodeStart(JsonOps.INSTANCE, owed).getOrThrow();
		helper.assertTrue(LairRewards.Owed.CODEC.parse(JsonOps.INSTANCE, owedSaved.getAsJsonObject().get("data")).getOrThrow().equals(owed),
				"What a player is owed reads its save from before versions");

		UUID away = UUID.randomUUID();
		LairRewards.Data data = LairRewards.Data.of(level.getServer());
		data.owe(away, due);
		try {
			JsonElement saved = LairRewards.Data.CODEC.encodeStart(JsonOps.INSTANCE, data).getOrThrow();
			helper.assertTrue(LairRewards.Data.CODEC.parse(JsonOps.INSTANCE, saved).getOrThrow().kept(away).equals(List.of(due)),
					"The world's record, written and read back, keeps it for the player away");
			helper.assertTrue(LairRewards.Data.CODEC.parse(JsonOps.INSTANCE, saved.getAsJsonObject().get("data")).getOrThrow()
					.kept(away).equals(List.of(due)), "The world's record reads its save from before versions");
		} finally {
			data.take(away);
		}
		helper.succeed();
	}

	/**
	 * Concordance Chunk B: a reward kept from {@code boss}'s fall is saved with the player and read back the same, and the
	 * boss's payer pays it once: at least {@code fewest} of {@code material}, and the experience share.
	 */
	private static void keptRewardIsSavedAndPaid(GameTestHelper helper, String boss, Item material, int fewest) {
		ServerLevel level = helper.getLevel();
		ServerPlayer fallen = player(helper, 1, 1);
		Vec3 origin = Vec3.atCenterOf(helper.absolutePos(new BlockPos(2, 2, 2)));
		LairRewards.Due due = new LairRewards.Due(boss, UUID.randomUUID(), origin, 75);
		LairRewards.owe(fallen, due);
		TagValueOutput out = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, level.registryAccess());
		fallen.saveWithoutId(out);
		ServerPlayer again = new ServerPlayer(level.getServer(), level, fallen.getGameProfile(), fallen.clientInformation());
		again.load(TagValueInput.create(ProblemReporter.DISCARDING, level.registryAccess(), out.buildResult()));
		// Making it bound the player's advancements to it; they go back to the player in the level.
		level.getServer().getPlayerList().getPlayerAdvancements(fallen);
		LairRewards.Owed owed = again.getAttached(LairRewards.OWED);
		helper.assertTrue(owed != null && owed.due().equals(List.of(due)), "Saved and loaded, it is not owed once under " + boss + ": " + owed);
		int before = fallen.totalExperience;
		helper.assertTrue(LairRewards.handOver(fallen) && fallen.getInventory().countItem(material) >= fewest
				&& fallen.totalExperience - before == 75 && !fallen.hasAttached(LairRewards.OWED), boss + "'s payer did not pay it");
		helper.assertTrue(!LairRewards.handOver(fallen), "It was paid twice");
		helper.succeed();
	}

	/** Concordance Chunk B: a kept Yeti King reward survives a save and is paid by his payer. */
	@GameTest(maxTicks = 20)
	public void aKeptYetiKingRewardIsSavedAndPaid(GameTestHelper helper) {
		keptRewardIsSavedAndPaid(helper, YetiKingLoot.BOSS, JugcraftYeti.YETI_FUR, 4);
	}

	/** Concordance Chunk B: a kept Cinder Tyrant reward survives a save and is paid by his payer. */
	@GameTest(maxTicks = 20)
	public void aKeptCinderTyrantRewardIsSavedAndPaid(GameTestHelper helper) {
		keptRewardIsSavedAndPaid(helper, CinderTyrantLoot.BOSS, JugcraftTyrant.TYRANT_SCALE, 3);
	}

	/**
	 * Concordance Chunk C (C-02, C-P8): a Stillroot Poultice put on a friend the fight hurt credits its helper with each
	 * heal its Regeneration gives, as it comes, one health a heal. Food before its first heal, or beside one, is not the
	 * poultice's; past the fight's hurt nothing more counts. The helper qualifies at its fourth heal; the patient never.
	 */
	@GameTest(maxTicks = 120)
	public void aPoulticeOnAFriendIsCreditedForWhatItHeals(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		Mob boss = helper.spawnWithNoFreeWill(EntityTypes.PIG, new BlockPos(2, 2, 2));
		Encounter fight = fight(boss);
		ServerPlayer patient = player(helper, 1, 1);
		ServerPlayer healer = player(helper, 3, 1);
		float[] lost = new float[1];
		float[] fed = new float[1];
		MobEffectInstance[] regeneration = new MobEffectInstance[1];
		helper.runAfterDelay(LOADING_TICKS, () -> timeline(helper, fight, boss, () -> {
			blow(level, patient, level.damageSources().mobAttack(boss), 10.0F);
			lost[0] = patient.getMaxHealth() - patient.getHealth();
			apply(healer, patient, poultice());
			regeneration[0] = patient.getEffect(MobEffects.REGENERATION);
			// Food, say: health the poultice did not give, before its first heal.
			patient.heal(1.0F);
			fed[0] = patient.getHealth();
		}, () -> {
			helper.assertTrue(lost[0] >= 6.0F && regeneration[0] != null && regeneration[0].getAmplifier() == 1 && regeneration[0].getDuration() == 450,
					"The fight hurt them (" + lost[0] + ") and the poultice gave Regeneration II for 450 ticks, untouched since: " + regeneration[0]);
			helper.assertTrue(healing(fight, healer) == 0.0F, "Food before the poultice's first heal is not its: " + healing(fight, healer));
			live(patient, 51);
		}, () -> {
			helper.assertTrue(patient.getHealth() - fed[0] == 3.0F && healing(fight, healer) == 3.0F && !fight.qualified().contains(healer.getUUID()),
					"Three heals, three credited, not yet enough: " + (patient.getHealth() - fed[0]) + ", " + healing(fight, healer));
			patient.heal(2.0F);
			live(patient, 25);
		}, () -> {
			helper.assertTrue(healing(fight, healer) == 4.0F && fight.qualified().contains(healer.getUUID())
					&& !fight.qualified().contains(patient.getUUID()),
					"Food beside a heal adds nothing past one health a heal; at the fourth the helper qualifies, the patient never: "
							+ healing(fight, healer));
			// To one tick before it runs out: fourteen heals more, if there is room for them.
			live(patient, 373);
		}, () -> {
			helper.assertTrue(patient.getHealth() == patient.getMaxHealth() && healing(fight, healer) == Math.max(4.0F, lost[0] - 3.0F),
					"Only the fight's hurt not yet healed is credited: " + healing(fight, healer) + " of " + lost[0]);
		}));
	}

	/** C-02: a poultice on yourself, by a use on yourself or sneaking, heals and earns no one anything. */
	@GameTest(maxTicks = 100)
	public void aPoulticeOnOneselfEarnsNothing(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		Mob boss = helper.spawnWithNoFreeWill(EntityTypes.PIG, new BlockPos(2, 2, 2));
		Encounter fight = fight(boss);
		ServerPlayer self = player(helper, 1, 1);
		ServerPlayer sneaker = player(helper, 3, 1);
		float[] was = new float[2];
		helper.runAfterDelay(LOADING_TICKS, () -> timeline(helper, fight, boss, () -> {
			blow(level, self, level.damageSources().mobAttack(boss), 10.0F);
			blow(level, sneaker, level.damageSources().mobAttack(boss), 10.0F);
			apply(self, self, poultice());
			RateGate.forget(sneaker.getUUID());
			ItemStack own = poultice();
			sneaker.setItemInHand(InteractionHand.MAIN_HAND, own);
			sneaker.setShiftKeyDown(true);
			own.getItem().use(level, sneaker, InteractionHand.MAIN_HAND);
			sneaker.setShiftKeyDown(false);
			helper.assertTrue(self.hasEffect(MobEffects.REGENERATION) && sneaker.hasEffect(MobEffects.REGENERATION), "Each put a poultice on themselves");
			was[0] = self.getHealth();
			was[1] = sneaker.getHealth();
			live(self, 26);
			live(sneaker, 26);
		}, () -> {
			helper.assertTrue(self.getHealth() - was[0] == 2.0F && sneaker.getHealth() - was[1] == 2.0F,
					"Each poultice healed twice: " + (self.getHealth() - was[0]) + ", " + (sneaker.getHealth() - was[1]));
			// Only this test's players: a fight in a neighbouring test may be in reach and credit its own.
			helper.assertTrue(healing(fight, self) == 0.0F && healing(fight, sneaker) == 0.0F && !fight.qualified().contains(self.getUUID())
					&& !fight.qualified().contains(sneaker.getUUID()),
					"A poultice on yourself earns nothing: " + healing(fight, self) + ", " + healing(fight, sneaker));
		}));
	}

	/**
	 * C-02: a poultice on a friend the fight did not hurt (whole, or hurt by a fall) earns nothing for what it heals; once
	 * the fight does hurt them, the same poultice's next heal is credited.
	 */
	@GameTest(maxTicks = 100)
	public void aPoulticeOnAnUnhurtFriendEarnsNothing(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		Mob boss = helper.spawnWithNoFreeWill(EntityTypes.PIG, new BlockPos(2, 2, 2));
		Encounter fight = fight(boss);
		ServerPlayer whole = player(helper, 1, 1);
		ServerPlayer fallen = player(helper, 1, 3);
		ServerPlayer healer = player(helper, 3, 1);
		float[] fell = new float[1];
		helper.runAfterDelay(LOADING_TICKS, () -> timeline(helper, fight, boss, () -> {
			blow(level, fallen, level.damageSources().fall(), 6.0F);
			fell[0] = fallen.getHealth();
			apply(healer, whole, poultice());
			apply(healer, fallen, poultice());
			live(whole, 26);
			live(fallen, 26);
		}, () -> {
			helper.assertTrue(fallen.getHealth() - fell[0] == 2.0F && whole.getHealth() == whole.getMaxHealth() && healing(fight, healer) == 0.0F
					&& !fight.qualified().contains(healer.getUUID()),
					"Healing a fall, or no one hurt, earns nothing: " + (fallen.getHealth() - fell[0]) + " healed, " + healing(fight, healer) + " credited");
			blow(level, fallen, level.damageSources().mobAttack(boss), 4.0F);
			// One heal, at 400 ticks left.
			live(fallen, 25);
		}, () -> {
			helper.assertTrue(healing(fight, healer) == 1.0F, "After the fight's blow, the poultice's next heal is credited: " + healing(fight, healer));
		}));
	}

	/**
	 * C-02: a second poultice that changes nothing (the same Regeneration, no longer) tells no one and earns nothing; one
	 * that lengthens it takes the note over, so its heals are the second helper's. Together they earn what was healed.
	 */
	@GameTest(maxTicks = 100)
	public void aSecondPoulticeAddsNoCredit(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		Mob boss = helper.spawnWithNoFreeWill(EntityTypes.PIG, new BlockPos(2, 2, 2));
		Encounter fight = fight(boss);
		ServerPlayer patient = player(helper, 1, 1);
		ServerPlayer healer = player(helper, 3, 1);
		ServerPlayer second = player(helper, 1, 3);
		float[] struck = new float[1];
		helper.runAfterDelay(LOADING_TICKS, () -> timeline(helper, fight, boss, () -> {
			blow(level, patient, level.damageSources().mobAttack(boss), 10.0F);
			struck[0] = patient.getHealth();
			apply(healer, patient, poultice());
			apply(second, patient, poultice());
			apply(healer, patient, poultice());
			MobEffectInstance regeneration = patient.getEffect(MobEffects.REGENERATION);
			helper.assertTrue(regeneration != null && regeneration.getAmplifier() == 1 && regeneration.getDuration() == 450,
					"Three poultices at once are one Regeneration II for 450 ticks: " + regeneration);
			live(patient, 26);
		}, () -> {
			helper.assertTrue(patient.getHealth() - struck[0] == 2.0F && healing(fight, healer) == 2.0F && healing(fight, second) == 0.0F,
					"Two heals, the first helper's: " + healing(fight, healer) + ", " + healing(fight, second));
			apply(second, patient, poultice());
			MobEffectInstance lengthened = patient.getEffect(MobEffects.REGENERATION);
			helper.assertTrue(lengthened != null && lengthened.getDuration() == 450, "The second poultice, later, lengthens it to 450: " + lengthened);
			// One heal, at 450 ticks left.
			live(patient, 25);
		}, () -> {
			helper.assertTrue(healing(fight, healer) == 2.0F && healing(fight, second) == 1.0F
					&& healing(fight, healer) + healing(fight, second) == patient.getHealth() - struck[0],
					"The lengthened poultice's heal is the second helper's, and together no more than was healed: " + healing(fight, healer) + ", "
							+ healing(fight, second) + " of " + (patient.getHealth() - struck[0]));
		}));
	}

	/**
	 * C-02: a poultice put on outside the fight's reach earns nothing, however long it heals inside; put on again inside,
	 * its heals are credited until the patient leaves the fight, and none after, back in reach or not.
	 */
	@GameTest(maxTicks = 110)
	public void aPoulticeOutsideTheFightEarnsNothing(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		Mob boss = helper.spawnWithNoFreeWill(EntityTypes.PIG, new BlockPos(2, 2, 2));
		Vec3 near = boss.position();
		Vec3 far = near.add(0.0, 0.0, Encounter.REACH * 10.0);
		Vec3[] centre = {near};
		Encounter fight = new Encounter(boss, () -> centre[0]);
		fight.begin();
		ServerPlayer patient = player(helper, 1, 1);
		ServerPlayer healer = player(helper, 3, 1);
		float[] struck = new float[1];
		helper.runAfterDelay(LOADING_TICKS, () -> timeline(helper, fight, boss, () -> {
			blow(level, patient, level.damageSources().mobAttack(boss), 10.0F);
			struck[0] = patient.getHealth();
			centre[0] = far;
			apply(healer, patient, poultice());
			centre[0] = near;
			live(patient, 26);
		}, () -> {
			helper.assertTrue(patient.getHealth() - struck[0] == 2.0F && healing(fight, healer) == 0.0F,
					"A poultice put on out of the fight's reach earns nothing: " + healing(fight, healer));
			// 424 ticks left lengthened to 450, now in the fight.
			apply(healer, patient, poultice());
			live(patient, 26);
		}, () -> {
			helper.assertTrue(healing(fight, healer) == 2.0F, "Put on again in the fight, its heals are credited: " + healing(fight, healer));
			// The patient leaves the fight for a heal (at 400 ticks left): the tick's end finds them out, and the note ends.
			centre[0] = far;
			live(patient, 25);
		}, () -> {
			// Back in reach for another heal (at 375): no note.
			centre[0] = near;
			live(patient, 25);
		}, () -> {
			helper.assertTrue(patient.getHealth() - struck[0] == 6.0F && healing(fight, healer) == 2.0F && !fight.qualified().contains(healer.getUUID()),
					"Heals out of the fight, and after, earn nothing: " + (patient.getHealth() - struck[0]) + " healed, " + healing(fight, healer) + " credited");
		}));
	}

	/**
	 * C-02: only a Stillroot Poultice earns its heals: not a Frostward salve, not a Regeneration salve carrying anything
	 * harmful (no poultice by name), not Regeneration given by a spell's status. A poultice on the same friend then is.
	 */
	@GameTest(maxTicks = 100)
	public void onlyAPoulticeMends(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		Mob boss = helper.spawnWithNoFreeWill(EntityTypes.PIG, new BlockPos(2, 2, 2));
		Encounter fight = fight(boss);
		ServerPlayer warded = player(helper, 1, 1);
		ServerPlayer fouled = player(helper, 1, 3);
		ServerPlayer spelled = player(helper, 3, 3);
		ServerPlayer healer = player(helper, 3, 1);
		float[] was = new float[2];
		helper.runAfterDelay(LOADING_TICKS, () -> timeline(helper, fight, boss, () -> {
			for (ServerPlayer patient : List.of(warded, fouled, spelled)) {
				blow(level, patient, level.damageSources().mobAttack(boss), 10.0F);
			}
			apply(healer, warded, salve("jugcraft:frostward@0", new Brew.Dose(Frostward.STATUS, 0, 975, false)));
			apply(healer, fouled, salve("minecraft:nausea@0+minecraft:regeneration@1", new Brew.Dose(Encounter.POULTICE_STATUS, 1, 450, false),
					new Brew.Dose("minecraft:nausea", 0, 100, true)));
			help(helper, healer, spelled, new EffectSpec(EffectKind.STATUS, Intent.HELPFUL, 1, 450, Encounter.POULTICE_STATUS, Stacking.STRONGEST, null));
			helper.assertTrue(Frostward.warded(warded) && fouled.hasEffect(MobEffects.REGENERATION) && spelled.hasEffect(MobEffects.REGENERATION),
					"Frostward, the fouled salve's Regeneration and the spell's all took");
			was[0] = fouled.getHealth();
			was[1] = spelled.getHealth();
			live(fouled, 26);
			live(spelled, 26);
		}, () -> {
			helper.assertTrue(fouled.getHealth() - was[0] == 2.0F && spelled.getHealth() - was[1] == 2.0F && healing(fight, healer) == 0.0F
					&& !fight.qualified().contains(healer.getUUID()),
					"Regeneration from no poultice earns nothing: " + (fouled.getHealth() - was[0]) + ", " + (spelled.getHealth() - was[1])
							+ " healed, " + healing(fight, healer) + " credited");
			apply(healer, warded, poultice());
			live(warded, 1);
		}, () -> {
			helper.assertTrue(healing(fight, healer) == 1.0F, "A poultice on the same friend is credited: " + healing(fight, healer));
		}));
	}

	/**
	 * C-02: what heals a poultice's patient between its heals is not its: a blow between two heals keeps the first (read
	 * as the blow lands) and leaves the next its own; the patient's own healing spells after it are read out, so a heal
	 * that finds them whole earns nothing.
	 */
	@GameTest(maxTicks = 100)
	public void blowsAndSpellsBetweenItsHealsAreNotThePoultices(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		Mob boss = helper.spawnWithNoFreeWill(EntityTypes.PIG, new BlockPos(2, 2, 2));
		Encounter fight = fight(boss);
		ServerPlayer patient = player(helper, 1, 1);
		ServerPlayer healer = player(helper, 3, 1);
		helper.runAfterDelay(LOADING_TICKS, () -> timeline(helper, fight, boss, () -> {
			blow(level, patient, level.damageSources().mobAttack(boss), 10.0F);
			apply(healer, patient, poultice());
			live(patient, 1);
			blow(level, patient, level.damageSources().mobAttack(boss), 3.0F);
			live(patient, 25);
		}, () -> {
			helper.assertTrue(healing(fight, healer) == 2.0F, "A heal before the blow and one after, each credited: " + healing(fight, healer));
			for (int i = 0; i < 5; i++) {
				help(helper, patient, patient, MEND);
			}
			helper.assertTrue(patient.getHealth() == patient.getMaxHealth(), "The patient mends themselves whole: " + patient.getHealth());
			// A heal at 400 ticks left, which finds them whole.
			live(patient, 25);
		}, () -> {
			helper.assertTrue(healing(fight, healer) == 2.0F && !fight.qualified().contains(healer.getUUID()) && !fight.qualified().contains(patient.getUUID()),
					"The patient's own healing is not the poultice's: " + healing(fight, healer));
		}));
	}

	/**
	 * C-02: vanilla Regeneration heals one health on each tick whose time left, before it counts down, is a multiple of
	 * 50 >> amplifier, as Encounter.regenerationHeals counts: Regeneration II for 450 ticks heals 18 times, I for 318 six
	 * times (so two diluted doses heal 6 + 6, against one whole dose's 18), III for 120 ten times.
	 */
	@GameTest(maxTicks = 20)
	public void regenerationHealsOnItsVanillaInterval(GameTestHelper helper) {
		ServerPlayer player = player(helper, 1, 1);
		for (int[] run : new int[][] {{1, 450, 18}, {0, 318, 6}, {2, 120, 10}}) {
			int amplifier = run[0];
			int ticks = run[1];
			int interval = 50 >> amplifier;
			player.removeAllEffects();
			player.setHealth(1.0F);
			player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, ticks, amplifier));
			int heals = 0;
			for (int i = 0; i < ticks; i++) {
				MobEffectInstance before = player.getEffect(MobEffects.REGENERATION);
				int left = before == null ? 0 : before.getDuration();
				float health = player.getHealth();
				player.baseTick();
				MobEffectInstance after = player.getEffect(MobEffects.REGENERATION);
				int now = after == null ? 0 : after.getDuration();
				float rose = player.getHealth() - health;
				helper.assertTrue(now == left - 1 && rose == (left % interval == 0 ? 1.0F : 0.0F)
						&& rose == Encounter.regenerationHeals(amplifier, now, left) * Encounter.REGENERATION_HEAL,
						"Regeneration " + (amplifier + 1) + " with " + left + " ticks left healed " + rose + " and has " + now + " left");
				heals += (int) rose;
			}
			helper.assertTrue(heals == run[2] && Encounter.regenerationHeals(amplifier, 0, ticks) == run[2] && !player.hasEffect(MobEffects.REGENERATION)
					&& player.getHealth() == 1.0F + heals, "Regeneration " + (amplifier + 1) + " for " + ticks + " ticks healed " + heals + ", not " + run[2]);
		}
		helper.succeed();
	}
}
