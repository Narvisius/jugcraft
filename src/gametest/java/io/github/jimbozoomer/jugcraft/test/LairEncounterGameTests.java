package io.github.jimbozoomer.jugcraft.test;

import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import io.github.jimbozoomer.jugcraft.concordance.ConcordanceEffects;
import io.github.jimbozoomer.jugcraft.concordance.effect.Cause;
import io.github.jimbozoomer.jugcraft.concordance.effect.EffectKind;
import io.github.jimbozoomer.jugcraft.concordance.effect.EffectSpec;
import io.github.jimbozoomer.jugcraft.concordance.effect.Intent;
import io.github.jimbozoomer.jugcraft.concordance.effect.Ledger;
import io.github.jimbozoomer.jugcraft.lair.Encounter;
import io.github.jimbozoomer.jugcraft.lair.LairRewards;
import io.github.jimbozoomer.jugcraft.lair.vesperine.JugcraftVesperine;
import io.github.jimbozoomer.jugcraft.lair.vesperine.VesperineLoot;
import java.util.List;
import java.util.UUID;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
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
 * survives a save either way. The bosses' own deeds and falls are in {@link VesperineGameTests} and
 * {@link TatterlaceGameTests}.
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
		ServerPlayer player = helper.makeMockServerPlayerInLevel();
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
}
