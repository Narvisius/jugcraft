package io.github.jimbozoomer.jugcraft.test;

import com.mojang.authlib.GameProfile;
import io.github.jimbozoomer.jugcraft.agriculture.TrickOrTreat;
import io.github.jimbozoomer.jugcraft.concordance.ConcordanceEffects;
import io.github.jimbozoomer.jugcraft.concordance.effect.Cause;
import io.github.jimbozoomer.jugcraft.concordance.effect.EffectKind;
import io.github.jimbozoomer.jugcraft.concordance.effect.EffectSpec;
import io.github.jimbozoomer.jugcraft.concordance.effect.Intent;
import io.github.jimbozoomer.jugcraft.concordance.effect.Ledger;
import io.github.jimbozoomer.jugcraft.lair.Encounter;
import io.github.jimbozoomer.jugcraft.lair.JugcraftLairs;
import io.github.jimbozoomer.jugcraft.lair.LairRewards;
import io.github.jimbozoomer.jugcraft.lair.MoltenSlagBlock;
import io.github.jimbozoomer.jugcraft.lair.SluiceGateBlock;
import io.github.jimbozoomer.jugcraft.lair.TroughStoneBlock;
import io.github.jimbozoomer.jugcraft.lair.tyrant.CinderKiln;
import io.github.jimbozoomer.jugcraft.lair.tyrant.CinderTyrantEntity;
import io.github.jimbozoomer.jugcraft.lair.tyrant.CinderTyrantLoot;
import io.github.jimbozoomer.jugcraft.lair.tyrant.CinderlingEntity;
import io.github.jimbozoomer.jugcraft.lair.tyrant.FallingCinderEntity;
import io.github.jimbozoomer.jugcraft.lair.tyrant.JugcraftTyrant;
import io.github.jimbozoomer.jugcraft.lair.tyrant.MagmaGobEntity;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.fabricmc.fabric.api.entity.FakePlayer;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.RangedAttribute;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.phys.Vec3;

/**
 * The Cinder Tyrant (docs/features/cinder-tyrant.md), as far as a game-test server can show him: one put down on his own
 * (with no lair instance, over a kiln laid out from the corner of an arena the kiln's size, so his bowl, his crucible,
 * his channel and his sluices' places are all loaded; each test lays the floor, troughs and gates it needs) waits sunk in
 * his crucible until a player's blow wakes him, and crawls out; his health grows with the party, and the Molten Heart
 * shortens his cooldowns and widens his breath; hot he takes half a blow and no fire, quenched more, and his heat comes
 * back; his tail sweeps round his back and flanks and not before him; a Body Slam into a flooded trough quenches him; his
 * breath burns before him and Fire Resistance stops it, the Salamander Charm does not; the Lava Wave passes under whoever
 * is up a block or in the air, once a wave; the channel's surge spills over its banks and ebbs; the choke moves round the
 * sluices, never onto an open one; the Eruption; his gobs, cinders and fire patches; his Cinderlings, gutting out in water
 * and crumbling when he sinks; the Salamander Charm against the slag; and his loot is each participant's own. His fight
 * in the Cinder Kiln itself runs in a real world, in {@link CinderTyrantClientGameTests}. Since Concordance Chunk B
 * (B-01) his fight's credit is the lairs' shared one: support and the absent share his hoard once, a healer is credited
 * for mending his blows and his own fire but not his slag's heat or burning, a killing blow alone is paid, a tamed
 * creature's blow is its owner's, four players are each paid once, and a fight loaded awake keeps a fresh record. Since
 * B-02 his loot is what his JEI pages say.
 *
 * <p>He fights any player within 38 blocks of his bowl's centre; the arena keeps every other test's players farther off
 * than that. Each test sends him away before it ends, with his Cinderlings, and lets the slag ebb. His fight's record
 * reaches farther, {@value Encounter#REACH} blocks (lair/Encounter), than the arena keeps other tests (at least 43.5
 * blocks from his centre), so another test's player may take part in it while it is on: these tests assert exact counts
 * and experience shares only for a fight begun and ended within one synchronous block, where nothing else runs, and
 * otherwise only what their own players got.
 */
public class CinderTyrantGameTests {
	/** An arena of air the kiln's width and length (72 x 80), so everything of his fight is loaded and ticking. */
	private static final String KILN = "jugcraft-test:kiln_bowl";
	/**
	 * A new player cannot be hurt until its client has loaded the world, or for 60 ticks; a mock player has no client, so
	 * a test that hurts one waits this long first.
	 */
	private static final int LOADING_TICKS = 80;
	/** A creature hurt shrugs off a blow no harder than the last for 10 ticks after (vanilla's); a test waits this long between blows. */
	private static final int HURT_COOLDOWN = 12;
	/** How many shares a page test rolls: each count then lies far inside five standard deviations of its chance's mean. */
	private static final int SHARES = 400;

	/** Whether {@code count} of {@value #SHARES} shares lies within five standard deviations of {@code chance}'s mean. */
	private static boolean within(int count, double chance) {
		double mean = SHARES * chance;
		double deviation = 5.0 * Math.sqrt(SHARES * chance * (1.0 - chance));
		return count >= mean - deviation && count <= mean + deviation;
	}

	/** The origin of a kiln laid out from the arena's corner, a block under it: its crucible's floor on the arena's. */
	private static BlockPos origin(GameTestHelper helper) {
		return helper.absolutePos(new BlockPos(0, -1, 0));
	}

	/** He waits sunk in the crucible of the kiln laid out from {@code origin}. */
	private static CinderTyrantEntity waiting(GameTestHelper helper, BlockPos origin) {
		return CinderTyrantEntity.summon(helper.getLevel(), origin, null);
	}

	/** The bowl's cracked basalt under the template's cells {@code x0..x1}, {@code z0..z1}, for him and players to stand on. */
	private static void floor(ServerLevel level, BlockPos origin, int x0, int z0, int x1, int z1) {
		for (int x = x0; x <= x1; x++) {
			for (int z = z0; z <= z1; z++) {
				level.setBlockAndUpdate(origin.offset(x, CinderKiln.BOWL, z), JugcraftLairs.CRACKED_BASALT.defaultBlockState());
			}
		}
	}

	/** Trough stone, flooded or dry, in the bowl's floor under the template's cells {@code x0..x1}, {@code z0..z1}. */
	private static void trough(ServerLevel level, BlockPos origin, int x0, int z0, int x1, int z1, boolean flooded) {
		for (int x = x0; x <= x1; x++) {
			for (int z = z0; z <= z1; z++) {
				level.setBlockAndUpdate(origin.offset(x, CinderKiln.BOWL, z),
						JugcraftLairs.TROUGH_STONE.defaultBlockState().setValue(TroughStoneBlock.FLOODED, flooded));
			}
		}
	}

	/** A survival player standing {@code dx}, {@code dz} from his bowl's centre, on its floor. */
	private static ServerPlayer player(GameTestHelper helper, CinderTyrantEntity tyrant, double dx, double dz) {
		ServerPlayer player = helper.makeMockServerPlayerInLevel();
		player.setGameMode(GameType.SURVIVAL);
		Vec3 at = tyrant.centre().add(dx, 0.0, dz);
		player.snapTo(at.x, at.y, at.z, 0.0F, 0.0F);
		return player;
	}

	/** Puts him in the middle of his bowl on its floor, facing south. */
	private static void middle(CinderTyrantEntity tyrant) {
		Vec3 centre = tyrant.centre();
		tyrant.teleportTo(centre.x, tyrant.floorY(), centre.z);
		tyrant.setYRot(0.0F);
	}

	/** Checks {@code body}, then sends him away whether it passed or failed, so a failed test leaves no Tyrant fighting on. */
	private static void ending(ServerLevel level, CinderTyrantEntity tyrant, Runnable body) {
		try {
			body.run();
		} finally {
			for (UUID id : tyrant.cinderlings()) {
				if (level.getEntity(id) instanceof CinderlingEntity ling) {
					ling.discard();
				}
			}
			tyrant.ebb(level);
			tyrant.discard();
		}
	}

	/** {@code healer} mends {@code patient} 4 through the Concordance's effect boundary, as the Mend word does. */
	private static void mend(ServerLevel level, ServerPlayer healer, ServerPlayer patient) {
		Cause cause = Cause.of(healer.getUUID(), Cause.Origin.SPELL, "test:mend", ConcordanceEffects.nextSerial());
		ConcordanceEffects.apply(new ConcordanceEffects.Context(level, cause, healer, new Ledger(new Ledger.Limits(16, 256, 0)), "mend",
				healer.position()), EffectSpec.of(EffectKind.RESTORATION, Intent.HELPFUL, 4, 0), patient);
	}

	/** How many of his trophies {@code player} holds. */
	private static int trophies(ServerPlayer player) {
		return player.getInventory().countItem(JugcraftTyrant.cinderbrand()) + player.getInventory().countItem(JugcraftTyrant.magmaw());
	}

	/**
	 * Sunk in his crucible he takes no harm; a player's blow wakes him at full health, and over {@value
	 * CinderTyrantEntity#WAKE_TICKS} ticks he rises out of the slag and crawls out over the crucible's rim onto the floor.
	 */
	@GameTest(structure = KILN, padding = 8, maxTicks = 120)
	public void aBlowWakesHimAndHeCrawlsOut(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		BlockPos origin = origin(helper);
		CinderTyrantEntity tyrant = waiting(helper, origin);
		floor(level, origin, 31, 30, 40, 42);
		ServerPlayer player = player(helper, tyrant, 0.5, 3.0);
		boolean sunk = tyrant.phase() == CinderTyrantEntity.Phase.WAITING && tyrant.position().distanceTo(tyrant.sunk()) < 0.01
				&& tyrant.action() == CinderTyrantEntity.Action.SUNK;
		boolean harmless = !tyrant.hurtServer(level, level.damageSources().playerAttack(player), 10.0F);
		boolean woken = tyrant.phase() == CinderTyrantEntity.Phase.WAKING && tyrant.getHealth() == tyrant.getMaxHealth();
		helper.runAfterDelay(CinderTyrantEntity.WAKE_TICKS + 3, () -> {
			ending(level, tyrant, () -> {
				player.discard();
				Vec3 out = CinderKiln.out().add(origin.getX(), origin.getY(), origin.getZ());
				helper.assertTrue(sunk, "He did not wait sunk in his crucible");
				helper.assertTrue(harmless, "Sunk, he was harmed");
				helper.assertTrue(woken, "A blow did not wake him at full health");
				helper.assertTrue(tyrant.phase() == CinderTyrantEntity.Phase.KILN, "He did not crawl out to fight: " + tyrant.phase());
				helper.assertTrue(Math.abs(tyrant.getY() - tyrant.floorY()) < 0.1, "He is not on the floor but at " + tyrant.getY());
				helper.assertTrue(Math.hypot(tyrant.getX() - out.x, tyrant.getZ() - out.z) < 2.0, "He did not crawl out over the rim but to " + tyrant.position());
			});
			helper.succeed();
		});
	}

	/**
	 * His health grows by half for each player after the first, at most two and a half times; the Molten Heart makes every
	 * cooldown a third shorter and his breath wider.
	 */
	@GameTest(structure = KILN, padding = 8)
	public void hisHealthAndTheMoltenHeart(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		CinderTyrantEntity tyrant = waiting(helper, origin(helper));
		ending(level, tyrant, () -> {
			helper.assertTrue(CinderTyrantEntity.partyScale(1) == 1.0 && CinderTyrantEntity.partyScale(2) == 1.5
					&& CinderTyrantEntity.partyScale(4) == 2.5 && CinderTyrantEntity.partyScale(9) == 2.5, "His health scales with the party, at most 2.5 times");
			helper.assertTrue(tyrant.cooldown(240) == 240 && tyrant.breathHalfAngle() == CinderTyrantEntity.BREATH_HALF_ANGLE,
					"Before the Molten Heart his cooldowns and his breath are their own");
			tyrant.startHeart(level);
			helper.assertTrue(tyrant.molten() && tyrant.phase() == CinderTyrantEntity.Phase.HEART, "No Molten Heart");
			helper.assertTrue(tyrant.cooldown(240) == 160 && tyrant.cooldown(CinderTyrantEntity.GLOBAL_COOLDOWN) == 13,
					"In the Molten Heart his cooldowns are not a third shorter: " + tyrant.cooldown(240));
			helper.assertTrue(tyrant.breathHalfAngle() == CinderTyrantEntity.HEART_BREATH_HALF_ANGLE, "In the Molten Heart his breath is no wider");
		});
		helper.succeed();
	}

	/**
	 * His rule, the heat is his: hot, a blow does him half its damage, and fire none; quenched, a blow does a quarter more and
	 * he crawls at half his pace; {@value CinderTyrantEntity#QUENCH_TICKS} ticks later his seams flare back over {@value
	 * CinderTyrantEntity#REHEAT_TICKS} and he is hot again.
	 */
	@GameTest(structure = KILN, padding = 8, maxTicks = 300)
	public void theHeatIsHis(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		BlockPos origin = origin(helper);
		CinderTyrantEntity tyrant = waiting(helper, origin);
		floor(level, origin, 30, 28, 41, 43);
		tyrant.wake(level);
		helper.runAfterDelay(CinderTyrantEntity.WAKE_TICKS + 2, () -> {
			float before = tyrant.getHealth();
			tyrant.hurtServer(level, level.damageSources().magic(), 10.0F);
			float hot = before - tyrant.getHealth();
			boolean fireproof = !tyrant.hurtServer(level, level.damageSources().hotFloor(), 10.0F);
			tyrant.quench(level);
			boolean quenched = tyrant.heat() == CinderTyrantEntity.Heat.QUENCHED && tyrant.quenchedTicks() == CinderTyrantEntity.QUENCH_TICKS;
			boolean slow = tyrant.speed() == CinderTyrantEntity.SPEED * CinderTyrantEntity.QUENCH_SPEED;
			// The next blow waits out his hurt cooldown: a blow within it does only what it adds to the last.
			helper.runAfterDelay(HURT_COOLDOWN, () -> {
				float warm = tyrant.getHealth();
				tyrant.hurtServer(level, level.damageSources().magic(), 10.0F);
				float cold = warm - tyrant.getHealth();
				helper.runAfterDelay(CinderTyrantEntity.QUENCH_TICKS + 5 - HURT_COOLDOWN, () -> {
					CinderTyrantEntity.Heat flaring = tyrant.heat();
					helper.runAfterDelay(CinderTyrantEntity.REHEAT_TICKS, () -> {
						ending(level, tyrant, () -> {
							helper.assertTrue(Math.abs(hot - 10.0 * CinderTyrantEntity.HOT_TAKEN) < 0.01, "Hot, he took " + hot + " of 10, not half");
							helper.assertTrue(fireproof, "Fire harmed him");
							helper.assertTrue(quenched, "He was not quenched");
							helper.assertTrue(slow, "Quenched, he does not crawl at half his pace");
							helper.assertTrue(Math.abs(cold - 10.0 * CinderTyrantEntity.QUENCH_TAKEN) < 0.01, "Quenched, he took " + cold + " of 10, not a quarter more");
							helper.assertTrue(flaring == CinderTyrantEntity.Heat.REHEATING, "After his quench his seams did not flare back: " + flaring);
							helper.assertTrue(tyrant.heat() == CinderTyrantEntity.Heat.HOT && tyrant.taken() == CinderTyrantEntity.HOT_TAKEN,
									"He is not hot again: " + tyrant.heat());
							helper.assertTrue(tyrant.speed() == CinderTyrantEntity.SPEED, "Hot again, he does not crawl at his pace");
						});
						helper.succeed();
					});
				});
			});
		});
	}

	/**
	 * His Tail Sweep strikes a player behind him and one beside him, and sets them burning, and spares the one before his
	 * head. They wait out their loading in creative, where he pays them no heed.
	 */
	@GameTest(structure = KILN, padding = 8, maxTicks = 180)
	public void hisTailSweepsRoundHisBackNotBeforeHim(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		BlockPos origin = origin(helper);
		CinderTyrantEntity tyrant = waiting(helper, origin);
		floor(level, origin, 30, 32, 41, 43);
		ServerPlayer front = player(helper, tyrant, 0.0, 3.5);
		ServerPlayer behind = player(helper, tyrant, 0.0, -3.5);
		ServerPlayer beside = player(helper, tyrant, 3.5, 0.0);
		List<ServerPlayer> players = List.of(front, behind, beside);
		players.forEach(player -> player.setGameMode(GameType.CREATIVE));
		helper.runAfterDelay(LOADING_TICKS, () -> {
			players.forEach(player -> player.setGameMode(GameType.SURVIVAL));
			ending(level, tyrant, () -> {
				middle(tyrant);  // facing south, towards the one in front
				int hit = tyrant.sweep(level);
				helper.assertTrue(hit == 2, "The sweep struck " + hit + " players, not 2");
				helper.assertTrue(front.getHealth() == front.getMaxHealth() && !front.isOnFire(), "The one before his head was not spared");
				for (ServerPlayer struck : List.of(behind, beside)) {
					helper.assertTrue(struck.getHealth() < struck.getMaxHealth() && struck.isOnFire(), "A player behind or beside him was not struck and set burning");
				}
				players.forEach(ServerPlayer::discard);
			});
			helper.succeed();
		});
	}

	/**
	 * His Body Slam lands: a player within {@value CinderTyrantEntity#SLAM_RADIUS} blocks is struck, one farther off only
	 * thrown back. Landing in a flooded trough quenches him; he is in it when its stone is under him or within
	 * {@value CinderTyrantEntity#QUENCH_MARGIN} of his feet, and not a hair beyond that, nor over dry trough stone.
	 */
	@GameTest(structure = KILN, padding = 8, maxTicks = 180)
	public void aSlamInAFloodedTroughQuenchesHim(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		BlockPos origin = origin(helper);
		CinderTyrantEntity tyrant = waiting(helper, origin);
		floor(level, origin, 28, 28, 43, 48);
		trough(level, origin, 28, 41, 43, 43, true);   // flooded: three rows from z 41
		trough(level, origin, 28, 31, 43, 33, false);  // dry
		ServerPlayer near = player(helper, tyrant, 2.0, 5.0);
		ServerPlayer far = player(helper, tyrant, 4.0, 5.0);
		List<ServerPlayer> players = List.of(near, far);
		players.forEach(player -> player.setGameMode(GameType.CREATIVE));
		helper.runAfterDelay(LOADING_TICKS, () -> {
			players.forEach(player -> player.setGameMode(GameType.SURVIVAL));
			ending(level, tyrant, () -> {
				double x = origin.getX() + CinderKiln.BOWL_X;
				double z0 = origin.getZ() + 41.0;
				tyrant.teleportTo(x, tyrant.floorY(), z0 - 1.3 - 0.4);
				helper.assertTrue(!tyrant.inFloodedTrough(level), "Short of the trough's edge by more than his margin, he is in it");
				tyrant.teleportTo(x, tyrant.floorY(), z0 - 1.3 - 0.2);
				helper.assertTrue(tyrant.inFloodedTrough(level), "Within his margin of the trough's edge, he is not in it");
				tyrant.teleportTo(x, tyrant.floorY(), origin.getZ() + 32.5);
				helper.assertTrue(!tyrant.inFloodedTrough(level), "Over dry trough stone, he is in a flooded trough");
				tyrant.teleportTo(x, tyrant.floorY(), origin.getZ() + 42.5);
				int hit = tyrant.slamLands(level);
				helper.assertTrue(hit == 1, "The slam struck " + hit + " players, not 1");
				helper.assertTrue(near.getHealth() < near.getMaxHealth(), "The player beside where he landed was not struck");
				helper.assertTrue(far.getHealth() == far.getMaxHealth() && far.getDeltaMovement().horizontalDistance() > 0.1,
						"The player farther off was struck, or not thrown back");
				helper.assertTrue(tyrant.heat() == CinderTyrantEntity.Heat.QUENCHED && tyrant.quenchedTicks() == CinderTyrantEntity.QUENCH_TICKS,
						"Landing in the flooded trough did not quench him");
				players.forEach(ServerPlayer::discard);
			});
			helper.succeed();
		});
	}

	/**
	 * His Kiln Breath burns a player before him and sets them burning; Fire Resistance stops it, and a Salamander Charm does
	 * not. A player forty degrees aside it misses; in the Molten Heart, sweeping wider, it does not.
	 */
	@GameTest(structure = KILN, padding = 8, maxTicks = 180)
	public void hisBreathBurnsBeforeHim(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		BlockPos origin = origin(helper);
		CinderTyrantEntity tyrant = waiting(helper, origin);
		floor(level, origin, 28, 34, 43, 46);
		ServerPlayer front = player(helper, tyrant, 0.0, 4.0);
		ServerPlayer resistant = player(helper, tyrant, 1.0, 5.5);
		ServerPlayer charmed = player(helper, tyrant, -1.0, 5.5);
		double aside = Math.toRadians(40.0);
		ServerPlayer wide = player(helper, tyrant, 5.0 * Math.sin(aside), 5.0 * Math.cos(aside));
		List<ServerPlayer> players = List.of(front, resistant, charmed, wide);
		players.forEach(player -> player.setGameMode(GameType.CREATIVE));
		resistant.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 600));
		charmed.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(JugcraftTyrant.SALAMANDER_CHARM));
		helper.runAfterDelay(LOADING_TICKS, () -> {
			players.forEach(player -> player.setGameMode(GameType.SURVIVAL));
			ending(level, tyrant, () -> {
				middle(tyrant);
				int hit = tyrant.breathe(level);
				helper.assertTrue(hit == 2, "His breath burned " + hit + " players, not 2");
				helper.assertTrue(front.getHealth() < front.getMaxHealth() && front.isOnFire(), "The player before him was not burned");
				helper.assertTrue(charmed.getHealth() < charmed.getMaxHealth(), "The Salamander Charm turned his breath aside");
				helper.assertTrue(resistant.getHealth() == resistant.getMaxHealth(), "Fire Resistance did not stop his breath");
				helper.assertTrue(wide.getHealth() == wide.getMaxHealth(), "His breath reached forty degrees aside");
				tyrant.startHeart(level);
				tyrant.breathe(level);
				helper.assertTrue(wide.getHealth() < wide.getMaxHealth(), "In the Molten Heart his breath did not reach forty degrees aside");
				players.forEach(ServerPlayer::discard);
			});
			helper.succeed();
		});
	}

	/**
	 * The Lava Wave burns a player it passes on the floor, once a wave; one standing a block up and one in the air it
	 * passes under.
	 */
	@GameTest(structure = KILN, padding = 8, maxTicks = 180)
	public void theLavaWavePassesUnderThoseUpOrInTheAir(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		BlockPos origin = origin(helper);
		CinderTyrantEntity tyrant = waiting(helper, origin);
		floor(level, origin, 26, 28, 45, 47);
		// A step of basalt a block up, as a shelf or the crucible's rim, six blocks east of the centre.
		level.setBlockAndUpdate(origin.offset(41, CinderKiln.BOWL + 1, 37), Blocks.BASALT.defaultBlockState());
		ServerPlayer grounded = player(helper, tyrant, 0.0, 6.0);
		ServerPlayer up = player(helper, tyrant, 6.0, 0.0);
		ServerPlayer leaping = player(helper, tyrant, -6.0, 0.0);
		List<ServerPlayer> players = List.of(grounded, up, leaping);
		players.forEach(player -> player.setGameMode(GameType.CREATIVE));
		helper.runAfterDelay(LOADING_TICKS, () -> {
			players.forEach(player -> player.setGameMode(GameType.SURVIVAL));
			ending(level, tyrant, () -> {
				middle(tyrant);
				up.snapTo(up.getX(), tyrant.floorY() + 1.0, up.getZ(), 0.0F, 0.0F);
				leaping.snapTo(leaping.getX(), tyrant.floorY() + 0.8, leaping.getZ(), 0.0F, 0.0F);
				grounded.setOnGround(true);
				up.setOnGround(true);
				leaping.setOnGround(false);
				tyrant.waveFrom(new Vec3(tyrant.getX(), tyrant.floorY(), tyrant.getZ()));
				int first = tyrant.rollWave(level, 0.0, CinderTyrantEntity.WAVE_REACH);
				int again = tyrant.rollWave(level, 0.0, CinderTyrantEntity.WAVE_REACH);
				helper.assertTrue(first == 1 && again == 0, "The wave burned " + first + " players, then " + again + ", not 1 and none");
				helper.assertTrue(grounded.getHealth() < grounded.getMaxHealth() && grounded.isOnFire(), "The wave did not burn the player on the floor");
				helper.assertTrue(up.getHealth() == up.getMaxHealth(), "The wave reached the player a block up");
				helper.assertTrue(leaping.getHealth() == leaping.getMaxHealth(), "The wave reached the player in the air");
				players.forEach(ServerPlayer::discard);
			});
			helper.succeed();
		});
	}

	/**
	 * The heat channel's surge, announced, spills {@value CinderTyrantEntity#SURGE_WARN} ticks later: every cell of floor
	 * within {@value CinderTyrantEntity#SURGE_SPILL} blocks of the channel with open air over it is slag, but not the
	 * crucible, nor a cell with a block on it, nor floor farther off; {@value CinderTyrantEntity#SURGE_TICKS} ticks later it
	 * ebbs, and every cell is what it was, cracked basalt in the bowl and rough basalt beyond it.
	 */
	@GameTest(structure = KILN, padding = 8, maxTicks = 200)
	public void theChannelSurgesOverItsBanksAndEbbs(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		BlockPos origin = origin(helper);
		CinderTyrantEntity tyrant = waiting(helper, origin);
		Map<BlockPos, BlockState> laid = new LinkedHashMap<>();
		for (int x = CinderKiln.RUN_WEST - 4; x <= CinderKiln.RUN_EAST + 4; x++) {
			for (int z = CinderKiln.CHANNEL_NORTH; z <= CinderKiln.CHANNEL_SOUTH; z++) {
				BlockState state = CinderKiln.inChannel(x, z) ? JugcraftLairs.MOLTEN_SLAG.defaultBlockState()
						: CinderKiln.inBowl(x, z) ? JugcraftLairs.CRACKED_BASALT.defaultBlockState() : Blocks.BASALT.defaultBlockState();
				BlockPos cell = origin.offset(x, CinderKiln.BOWL, z);
				level.setBlockAndUpdate(cell, state);
				laid.put(cell, state);
			}
		}
		BlockPos covered = origin.offset(CinderKiln.RUN_WEST - 2, CinderKiln.BOWL, 18);
		level.setBlockAndUpdate(covered.above(), Blocks.BASALT.defaultBlockState());
		tyrant.startSurge(level);
		boolean held = tyrant.surged().isEmpty();
		helper.runAfterDelay(CinderTyrantEntity.SURGE_WARN + 3, () -> {
			Set<BlockPos> spilled = new HashSet<>(tyrant.surged());
			for (Map.Entry<BlockPos, BlockState> entry : laid.entrySet()) {
				BlockPos cell = entry.getKey();
				int x = cell.getX() - origin.getX();
				int z = cell.getZ() - origin.getZ();
				boolean bank = x >= CinderKiln.RUN_WEST - CinderTyrantEntity.SURGE_SPILL && x <= CinderKiln.RUN_EAST + CinderTyrantEntity.SURGE_SPILL
						&& !CinderKiln.inChannel(x, z) && !CinderKiln.inCrucible(x, z) && !cell.equals(covered);
				helper.assertTrue(spilled.contains(cell) == bank, "The surge " + (bank ? "did not spill over" : "spilled over") + " " + cell);
				helper.assertTrue(!bank || level.getBlockState(cell).is(JugcraftLairs.MOLTEN_SLAG), "The surge's cell " + cell + " is not slag");
			}
			helper.assertTrue(level.getBlockState(covered).is(JugcraftLairs.CRACKED_BASALT), "The slag spilled under a block standing on the floor");
		});
		helper.runAfterDelay(CinderTyrantEntity.SURGE_WARN + CinderTyrantEntity.SURGE_TICKS + 6, () -> {
			ending(level, tyrant, () -> {
				helper.assertTrue(held, "The surge spilled before its warning");
				helper.assertTrue(tyrant.surged().isEmpty(), "The surge has not ebbed");
				for (Map.Entry<BlockPos, BlockState> entry : laid.entrySet()) {
					helper.assertTrue(level.getBlockState(entry.getKey()) == entry.getValue(),
							"After the ebb, " + entry.getKey() + " is " + level.getBlockState(entry.getKey()) + ", not " + entry.getValue());
				}
			});
			helper.succeed();
		});
	}

	/**
	 * In the Eruption the choke moves round the sluices, west, east and south, never onto an open one, and with the others
	 * open it stays where it is; a choked gate will not turn, and a gate choked while open closes and its trough drains.
	 * When the fight resets, every gate is ready and every trough dry. Each gate here is its wheel over a frame block on
	 * trough stone, at the kiln's wheels.
	 */
	@GameTest(structure = KILN, padding = 8, maxTicks = 40)
	public void theChokeMovesRoundTheSluices(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		BlockPos origin = origin(helper);
		CinderTyrantEntity tyrant = waiting(helper, origin);
		List<BlockPos> wheels = tyrant.wheels();
		BlockState gate = JugcraftLairs.SLUICE_GATE.defaultBlockState().setValue(SluiceGateBlock.FACING, Direction.NORTH);
		for (BlockPos wheel : wheels) {
			level.setBlockAndUpdate(wheel, gate.setValue(SluiceGateBlock.PART, SluiceGateBlock.Part.WHEEL));
			level.setBlockAndUpdate(wheel.below(), gate.setValue(SluiceGateBlock.PART, SluiceGateBlock.Part.FRAME));
			for (Direction way : List.of(Direction.NORTH, Direction.SOUTH)) {
				level.setBlockAndUpdate(wheel.below(2).relative(way), JugcraftLairs.TROUGH_STONE.defaultBlockState());
			}
			level.setBlockAndUpdate(wheel.below(2), JugcraftLairs.TROUGH_STONE.defaultBlockState());
		}
		ending(level, tyrant, () -> {
			helper.assertTrue(SluiceGateBlock.turn(level, wheels.get(1), null) == SluiceGateBlock.Turn.OPENED, "The east sluice did not open");
			int first = tyrant.moveChoke(level);
			helper.assertTrue(first == 0 && flow(level, wheels.get(0)) == SluiceGateBlock.Flow.CHOKED, "The choke did not fall on the west sluice first");
			int second = tyrant.moveChoke(level);
			helper.assertTrue(second == 2 && flow(level, wheels.get(2)) == SluiceGateBlock.Flow.CHOKED,
					"The choke did not pass over the open east sluice to the south one: " + second);
			helper.assertTrue(flow(level, wheels.get(0)) == SluiceGateBlock.Flow.READY, "The west sluice was not cleared as the choke moved on");
			helper.assertTrue(flow(level, wheels.get(1)) == SluiceGateBlock.Flow.OPEN, "The open east sluice was choked");
			helper.assertTrue(SluiceGateBlock.turn(level, wheels.get(2), null) == SluiceGateBlock.Turn.CHOKED, "A choked sluice turned");
			helper.assertTrue(SluiceGateBlock.turn(level, wheels.get(0), null) == SluiceGateBlock.Turn.OPENED, "The cleared west sluice did not turn");
			helper.assertTrue(tyrant.moveChoke(level) == 2 && flow(level, wheels.get(2)) == SluiceGateBlock.Flow.CHOKED,
					"With the other two open, the choke did not stay where it was");
			helper.assertTrue(SluiceGateBlock.choke(level, wheels.get(1), true) && flow(level, wheels.get(1)) == SluiceGateBlock.Flow.CHOKED
					&& !flooded(level, wheels.get(1)), "A sluice choked while open did not close and drain");
			tyrant.reset(level);
			for (BlockPos wheel : wheels) {
				helper.assertTrue(flow(level, wheel) == SluiceGateBlock.Flow.READY && !flooded(level, wheel), "After the reset, " + wheel + " is not ready and dry");
			}
			helper.assertTrue(tyrant.choked() == -1, "After the reset a sluice is still counted choked");
		});
		helper.succeed();
	}

	private static SluiceGateBlock.Flow flow(ServerLevel level, BlockPos wheel) {
		return level.getBlockState(wheel).getValue(SluiceGateBlock.FLOW);
	}

	/** Whether the trough under the test's gate at {@code wheel} is flooded. */
	private static boolean flooded(ServerLevel level, BlockPos wheel) {
		return level.getBlockState(wheel.below(2)).getValue(TroughStoneBlock.FLOODED);
	}

	/**
	 * At half his health, the Eruption: he leaps up onto the forge's lip, unhurt while it lasts, roars as the channel surges
	 * and two Cinderlings crawl out on its banks, and {@value CinderTyrantEntity#ERUPT_TICKS} ticks later comes back down
	 * into the bowl to fight on.
	 */
	@GameTest(structure = KILN, padding = 8, maxTicks = 220)
	public void atHalfHealthTheEruption(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		BlockPos origin = origin(helper);
		CinderTyrantEntity tyrant = waiting(helper, origin);
		floor(level, origin, 30, 14, 41, 42);
		tyrant.wake(level);
		helper.runAfterDelay(CinderTyrantEntity.WAKE_TICKS + 2, () -> {
			tyrant.setHealth(tyrant.getMaxHealth() * 0.5F);
			tyrant.startEruption(level);
			boolean unhurt = !tyrant.hurtServer(level, level.damageSources().magic(), 10.0F);
			helper.runAfterDelay(CinderTyrantEntity.ERUPT_LEAP + 3, () -> {
				Vec3 lip = CinderKiln.lip().add(origin.getX(), origin.getY(), origin.getZ());
				helper.assertTrue(unhurt, "Erupting, he was harmed");
				helper.assertTrue(tyrant.phase() == CinderTyrantEntity.Phase.ERUPTING && tyrant.position().distanceTo(lip) < 0.5
						&& tyrant.action() == CinderTyrantEntity.Action.ROAR, "He is not roaring on the forge's lip but at " + tyrant.position());
				List<UUID> lings = tyrant.cinderlings();
				helper.assertTrue(lings.size() == CinderTyrantEntity.ERUPT_CINDERLINGS, lings.size() + " Cinderlings crawled out, not "
						+ CinderTyrantEntity.ERUPT_CINDERLINGS);
				for (boolean west : List.of(true, false)) {
					Vec3 bank = CinderKiln.bank(west).add(origin.getX(), origin.getY(), origin.getZ());
					helper.assertTrue(lings.stream().anyMatch(id -> level.getEntity(id) instanceof CinderlingEntity ling
							&& Math.hypot(ling.getX() - bank.x, ling.getZ() - bank.z) < 2.0), "No Cinderling crawled out on the " + (west ? "west" : "east") + " bank");
				}
			});
			helper.runAfterDelay(CinderTyrantEntity.ERUPT_LEAP + CinderTyrantEntity.SURGE_WARN + 3,
					() -> helper.assertTrue(!tyrant.surged().isEmpty(), "The channel did not surge as he roared"));
			helper.runAfterDelay(2 * CinderTyrantEntity.ERUPT_LEAP + CinderTyrantEntity.ERUPT_TICKS + 3, () -> {
				ending(level, tyrant, () -> {
					Vec3 out = CinderKiln.out().add(origin.getX(), origin.getY(), origin.getZ());
					helper.assertTrue(tyrant.phase() == CinderTyrantEntity.Phase.ERUPTION, "He did not come down to fight on: " + tyrant.phase());
					helper.assertTrue(Math.abs(tyrant.getY() - tyrant.floorY()) < 0.1 && Math.hypot(tyrant.getX() - out.x, tyrant.getZ() - out.z) < 2.0,
							"He did not come down into the bowl but to " + tyrant.position());
					float before = tyrant.getHealth();
					tyrant.hurtServer(level, level.damageSources().magic(), 10.0F);
					helper.assertTrue(tyrant.getHealth() < before, "Down from the lip, he cannot be harmed");
				});
				helper.succeed();
			});
		});
	}

	/**
	 * A gob of magma he spits arcs onto its mark and bursts there, striking the player on it and leaving a patch of fire;
	 * a cinder falls onto its mark and bursts the same way; a player standing in a fire patch burns.
	 */
	@GameTest(structure = KILN, padding = 8, maxTicks = 200)
	public void hisGobsAndCindersBurstAndLeaveFire(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		BlockPos origin = origin(helper);
		CinderTyrantEntity tyrant = waiting(helper, origin);
		floor(level, origin, 26, 30, 45, 48);
		ServerPlayer spat = player(helper, tyrant, 0.0, 7.0);
		ServerPlayer under = player(helper, tyrant, -5.0, 3.0);
		ServerPlayer beside = player(helper, tyrant, 5.0, 3.0);
		List<ServerPlayer> players = List.of(spat, under, beside);
		players.forEach(player -> player.setGameMode(GameType.CREATIVE));
		helper.runAfterDelay(LOADING_TICKS, () -> {
			players.forEach(player -> player.setGameMode(GameType.SURVIVAL));
			middle(tyrant);
			MagmaGobEntity gob = MagmaGobEntity.spit(level, tyrant, new Vec3(spat.getX(), tyrant.floorY(), spat.getZ()));
			FallingCinderEntity cinder = FallingCinderEntity.drop(level, tyrant, new Vec3(under.getX(), tyrant.floorY(), under.getZ()));
			helper.runAfterDelay(30, () -> {
				ending(level, tyrant, () -> {
					helper.assertTrue(gob.isRemoved() && cinder.isRemoved(), "The gob or the cinder did not burst");
					helper.assertTrue(spat.getHealth() < spat.getMaxHealth(), "The gob did not strike the player on its mark");
					helper.assertTrue(under.getHealth() < under.getMaxHealth(), "The cinder did not strike the player under it");
					helper.assertTrue(tyrant.patches().size() >= 2, "No patches of fire where they burst: " + tyrant.patches().size());
					float before = beside.getHealth();
					tyrant.firePatch(level, new Vec3(beside.getX() - 0.5, tyrant.floorY(), beside.getZ()));
					tyrant.scorch(level);
					helper.assertTrue(beside.getHealth() < before && beside.isOnFire(), "Standing in a patch of fire did not burn the player");
					players.forEach(ServerPlayer::discard);
				});
				helper.succeed();
			});
		});
	}

	/**
	 * His Mantle Shed brings out Cinderlings, never more than {@value CinderTyrantEntity#MAX_CINDERLINGS} at once; one on a
	 * flooded trough gutters out; when he sinks back into his crucible the rest crumble to ash.
	 */
	@GameTest(structure = KILN, padding = 8, maxTicks = 120)
	public void hisCinderlingsGutterInWaterAndCrumbleWhenHeSinks(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		BlockPos origin = origin(helper);
		CinderTyrantEntity tyrant = waiting(helper, origin);
		floor(level, origin, 28, 28, 43, 46);
		trough(level, origin, 41, 44, 43, 46, true);
		tyrant.wake(level);
		helper.runAfterDelay(CinderTyrantEntity.WAKE_TICKS + 2, () -> {
			middle(tyrant);
			int shed = tyrant.shed(level, 6);
			int more = tyrant.shed(level, 2);
			List<UUID> lings = tyrant.cinderlings();
			CinderlingEntity wet = (CinderlingEntity) level.getEntity(lings.getFirst());
			wet.teleportTo(origin.getX() + 42.5, tyrant.floorY(), origin.getZ() + 45.5);
			helper.runAfterDelay(10, () -> {
				boolean guttered = wet.isRemoved();
				tyrant.reset(level);
				ending(level, tyrant, () -> {
					helper.assertTrue(shed == CinderTyrantEntity.MAX_CINDERLINGS && more == 0, "More than " + CinderTyrantEntity.MAX_CINDERLINGS
							+ " Cinderlings came at once: " + shed + " and " + more);
					helper.assertTrue(guttered, "A Cinderling on a flooded trough did not gutter out");
					for (UUID id : lings) {
						helper.assertTrue(!(level.getEntity(id) instanceof CinderlingEntity ling) || ling.isRemoved(), "A Cinderling outlived his sinking back");
					}
					helper.assertTrue(tyrant.phase() == CinderTyrantEntity.Phase.WAITING && tyrant.getHealth() == tyrant.getMaxHealth()
							&& tyrant.position().distanceTo(tyrant.sunk()) < 0.1, "Left alone he did not sink back into his crucible, healed");
				});
				helper.succeed();
			});
		});
	}

	/**
	 * A Salamander Charm held in the offhand: the kiln's slag does not harm its holder nor set them burning, and no hot
	 * ground hurts them; without it, both do.
	 */
	@GameTest(maxTicks = 120)
	public void theSalamanderCharmWardsOffHotGround(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		ServerPlayer bare = helper.makeMockServerPlayerInLevel();
		ServerPlayer warded = helper.makeMockServerPlayerInLevel();
		bare.setGameMode(GameType.CREATIVE);
		warded.setGameMode(GameType.CREATIVE);
		warded.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(JugcraftTyrant.SALAMANDER_CHARM));
		helper.runAfterDelay(LOADING_TICKS, () -> {
			bare.setGameMode(GameType.SURVIVAL);
			warded.setGameMode(GameType.SURVIVAL);
			helper.assertTrue(MoltenSlagBlock.harms(bare) && !MoltenSlagBlock.harms(warded), "The slag does not spare the charm's holder alone");
			boolean bareHurt = bare.hurtServer(level, level.damageSources().hotFloor(), 1.0F);
			boolean wardedHurt = warded.hurtServer(level, level.damageSources().hotFloor(), 1.0F);
			helper.assertTrue(bareHurt && bare.getHealth() < bare.getMaxHealth(), "Hot ground did not hurt a player without the charm");
			helper.assertTrue(!wardedHurt && warded.getHealth() == warded.getMaxHealth(), "Hot ground hurt the charm's holder");
			bare.discard();
			warded.discard();
			helper.succeed();
		});
	}

	/**
	 * His loot is each participant's own: everyone who struck him rolls for themselves (Tyrant Scales, and on a first kill
	 * always one of his trophies) into their own inventory, and a bystander who never struck him gets nothing. Afterwards
	 * Tempered is theirs, and a later roll no longer counts as a first kill. As he falls the crucible's slag cools to
	 * obsidian.
	 */
	@GameTest(structure = KILN, padding = 8)
	public void hisLootIsEachParticipantsOwn(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		BlockPos origin = origin(helper);
		CinderTyrantEntity tyrant = waiting(helper, origin);
		floor(level, origin, 31, 36, 40, 44);
		BlockPos slag = BlockPos.containing(CinderKiln.CRUCIBLE_X, CinderKiln.BOWL, CinderKiln.CRUCIBLE_Z).offset(origin);
		level.setBlockAndUpdate(slag, JugcraftLairs.MOLTEN_SLAG.defaultBlockState());
		ServerPlayer first = player(helper, tyrant, 0.5, 3.0);
		ServerPlayer second = player(helper, tyrant, 2.5, 3.0);
		ServerPlayer bystander = player(helper, tyrant, -1.5, 3.0);
		tyrant.took(first, 10.0F);
		tyrant.took(second, 4.0F);
		ending(level, tyrant, () -> {
			List<ServerPlayer> participants = tyrant.participants(level);
			helper.assertTrue(participants.contains(first) && participants.contains(second), "Both who struck him take part");
			helper.assertFalse(participants.contains(bystander), "A bystander who never struck him does not");
			helper.assertTrue(CinderTyrantLoot.firstKill(first), "A newcomer has not yet won Tempered");
			List<ItemStack> loot = CinderTyrantLoot.roll(level, tyrant, first);
			int scales = loot.stream().filter(stack -> stack.is(JugcraftTyrant.TYRANT_SCALE)).mapToInt(ItemStack::getCount).sum();
			helper.assertTrue(scales >= 3 && scales <= 6, scales + " Tyrant Scales, not 3 to 6");
			helper.assertTrue(loot.stream().filter(CinderTyrantLoot::trophy).count() == 1, "A first kill always brings one of his trophies");
			CinderTyrantLoot.reward(level, tyrant, participants);
			for (ServerPlayer player : List.of(first, second)) {
				helper.assertTrue(player.getInventory().countItem(JugcraftTyrant.TYRANT_SCALE) >= 3, "Each participant's loot went into their own inventory");
				int trophies = player.getInventory().countItem(JugcraftTyrant.cinderbrand()) + player.getInventory().countItem(JugcraftTyrant.magmaw());
				helper.assertTrue(trophies == 1, "Each first kill brought its own trophy, not " + trophies);
				helper.assertFalse(CinderTyrantLoot.firstKill(player), "Tempered is theirs now");
			}
			helper.assertTrue(bystander.getInventory().countItem(JugcraftTyrant.TYRANT_SCALE) == 0, "The bystander got nothing");
			helper.assertTrue(CinderTyrantLoot.firstKill(bystander), "The bystander has not won Tempered");
			helper.assertTrue(tyrant.coolCrucible(level) >= 1 && level.getBlockState(slag).is(Blocks.OBSIDIAN), "The crucible's slag did not cool to obsidian");
			for (ServerPlayer player : List.of(first, second, bystander)) {
				player.discard();
			}
		});
		helper.succeed();
	}

	/**
	 * Concordance Chunk B: support shares his hoard. A healer who mended 4 of what his Tail Sweep did is paid when he
	 * falls; a striker still down as he falls has their share kept and handed over once on their feet, by his payer; one
	 * who left the game in his fight has it kept in the world's record; one who walked away alive, a bystander and the one
	 * he only hurt are not paid; and his fall reported again pays no one twice. Death and leaving are marked, as Chunk A's
	 * tests mark them (the Yeti King's test makes a real death).
	 */
	@GameTest(structure = KILN, padding = 8, maxTicks = 160)
	public void supportSharesHisHoardOnce(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		BlockPos origin = origin(helper);
		CinderTyrantEntity tyrant = waiting(helper, origin);
		floor(level, origin, 28, 30, 43, 48);
		ServerPlayer patient = player(helper, tyrant, 0.0, -3.5);  // behind him: his tail's
		ServerPlayer healer = player(helper, tyrant, 0.0, 7.0);  // before his head, past his tail's reach
		ServerPlayer bystander = player(helper, tyrant, -3.0, 7.0);
		ServerPlayer wanderer = player(helper, tyrant, 3.0, 7.0);
		ServerPlayer fallen = player(helper, tyrant, 1.5, 8.5);
		// Away from the game: a player the server's list never held, as one who has left is not held.
		FakePlayer away = FakePlayer.get(level, new GameProfile(UUID.randomUUID(), "tyrant_away"));
		List<ServerPlayer> players = List.of(patient, healer, bystander, wanderer, fallen);
		players.forEach(player -> player.setGameMode(GameType.CREATIVE));  // he pays them no heed while they load
		helper.runAfterDelay(LOADING_TICKS - CinderTyrantEntity.WAKE_TICKS - 2, () -> tyrant.wake(level));
		helper.runAfterDelay(LOADING_TICKS, () -> {
			players.forEach(player -> player.setGameMode(GameType.SURVIVAL));
			LairRewards.Data data = LairRewards.Data.of(level.getServer());
			ending(level, tyrant, () -> {
				try {
					Encounter fight = tyrant.encounter();
					UUID receipt = fight.fight();
					helper.assertTrue(tyrant.fighting() && fight.underway(), "Awake, his fight is not on: " + tyrant.phase());
					middle(tyrant);
					helper.assertTrue(tyrant.sweep(level) == 1, "His tail did not strike only the one behind him");
					float lost = patient.getMaxHealth() - patient.getHealth();
					mend(level, healer, patient);
					Encounter.Share mended = fight.share(healer);
					helper.assertTrue(lost >= Encounter.HEALING && mended != null && mended.healing() == Encounter.HEALING && mended.qualifies(),
							"Mending 4 of his blow (" + lost + ") did not qualify the healer: " + (mended == null ? "nothing" : mended.healing()));
					tyrant.took(wanderer, 2.0F);
					tyrant.took(fallen, 2.0F);
					tyrant.took(away, 2.0F);
					fight.excuse(fallen.getUUID());  // as dying in his reach does (Encounter's AFTER_DEATH)
					fight.left(away.getUUID());  // as leaving the game there does (Encounter's LEAVE): a test player cannot disconnect
					wanderer.snapTo(wanderer.getX() + 100.0, wanderer.getY(), wanderer.getZ(), 0.0F, 0.0F);
					fallen.setHealth(0.0F);  // still down as he falls (on their feet again below, in this tick)
					List<UUID> qualified = fight.qualified();
					helper.assertTrue(qualified.containsAll(List.of(healer.getUUID(), wanderer.getUUID(), fallen.getUUID(), away.getUUID()))
							&& !qualified.contains(patient.getUUID()) && !qualified.contains(bystander.getUUID()),
							"The healer and the three strikers should qualify, the one he only hurt and the bystander not: " + qualified);
					int experience = healer.totalExperience;
					tyrant.hurtServer(level, level.damageSources().genericKill(), Float.MAX_VALUE);
					helper.assertTrue(!tyrant.isAlive() && !fight.underway(), "He did not fall");
					int scales = healer.getInventory().countItem(JugcraftTyrant.TYRANT_SCALE);
					helper.assertTrue(scales >= 3 && trophies(healer) == 1 && healer.totalExperience > experience && !CinderTyrantLoot.firstKill(healer),
							"The healer was not paid in full: " + scales + " Tyrant Scales, " + trophies(healer) + " trophies");
					for (ServerPlayer unpaid : List.of(patient, bystander, wanderer)) {
						helper.assertTrue(unpaid.getInventory().countItem(JugcraftTyrant.TYRANT_SCALE) == 0 && CinderTyrantLoot.firstKill(unpaid),
								"Paid: the one he only hurt, the bystander or the one who walked away");
					}
					LairRewards.Owed owed = fallen.getAttached(LairRewards.OWED);
					helper.assertTrue(owed != null && owed.due().size() == 1 && owed.due().getFirst().boss().equals(CinderTyrantLoot.BOSS)
							&& owed.due().getFirst().fight().equals(receipt) && fallen.getInventory().countItem(JugcraftTyrant.TYRANT_SCALE) == 0,
							"The share of one still down as he fell was not kept for them: " + owed);
					List<LairRewards.Due> kept = data.kept(away.getUUID());
					helper.assertTrue(kept.size() == 1 && kept.getFirst().boss().equals(CinderTyrantLoot.BOSS) && kept.getFirst().fight().equals(receipt),
							"The share of one who left the game was not kept in the world: " + kept);
					// Back on their feet (AFTER_RESPAWN calls handOver).
					fallen.setHealth(fallen.getMaxHealth());
					helper.assertTrue(LairRewards.handOver(fallen) && fallen.getInventory().countItem(JugcraftTyrant.TYRANT_SCALE) >= 3
							&& !fallen.hasAttached(LairRewards.OWED) && !CinderTyrantLoot.firstKill(fallen),
							"Back on their feet, the fallen striker was not handed their share by his payer");
					int fallenScales = fallen.getInventory().countItem(JugcraftTyrant.TYRANT_SCALE);
					tyrant.die(level.damageSources().genericKill());
					helper.assertTrue(healer.getInventory().countItem(JugcraftTyrant.TYRANT_SCALE) == scales && trophies(healer) == 1
							&& fallen.getInventory().countItem(JugcraftTyrant.TYRANT_SCALE) == fallenScales && !fallen.hasAttached(LairRewards.OWED)
							&& data.kept(away.getUUID()).size() == 1 && bystander.getInventory().countItem(JugcraftTyrant.TYRANT_SCALE) == 0,
							"His fall reported again paid someone twice");
				} finally {
					data.take(away.getUUID());
					players.forEach(ServerPlayer::discard);
				}
			});
			helper.succeed();
		});
	}

	/**
	 * Concordance Chunk B: a player whose only part in his fight is the blow that fells him has taken part before his
	 * fall is paid (it gets there before hurtServer's own credit), and is paid: Tyrant Scales and Tempered.
	 */
	@GameTest(structure = KILN, padding = 8, maxTicks = 80)
	public void aKillingBlowAloneIsPaid(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		BlockPos origin = origin(helper);
		CinderTyrantEntity tyrant = waiting(helper, origin);
		floor(level, origin, 31, 30, 40, 44);
		ServerPlayer striker = player(helper, tyrant, 0.5, 6.0);
		striker.setGameMode(GameType.CREATIVE);  // he pays them no heed; they are paid all the same
		tyrant.wake(level);
		helper.runAfterDelay(CinderTyrantEntity.WAKE_TICKS + 3, () -> {
			ending(level, tyrant, () -> {
				try {
					helper.assertTrue(tyrant.fighting() && tyrant.encounter().underway(), "He did not crawl out to fight: " + tyrant.phase());
					helper.assertTrue(tyrant.encounter().share(striker) == null, "The striker already had a part before the last blow");
					// Hot he takes half of 10, and his armour leaves about 3.5 of that: more than the 1 he has left.
					tyrant.setHealth(1.0F);
					boolean felled = tyrant.hurtServer(level, level.damageSources().playerAttack(striker), 10.0F);
					Encounter.Share share = tyrant.encounter().share(striker);
					helper.assertTrue(felled && !tyrant.isAlive() && share != null && share.qualifies(),
							"The blow that felled him was not the striker's part in his fight");
					helper.assertTrue(striker.getInventory().countItem(JugcraftTyrant.TYRANT_SCALE) >= 3 && trophies(striker) == 1,
							"The striker whose only blow felled him was not paid");
					helper.assertFalse(CinderTyrantLoot.firstKill(striker), "Tempered is not the striker's");
				} finally {
					striker.discard();
				}
			});
			helper.succeed();
		});
	}

	/**
	 * Concordance Chunk B: a tamed wolf's bite on one of his Cinderlings, or on him (the felling bite too), is its owner's
	 * part in his fight; a wild wolf's is no one's. Both owners are paid when he falls.
	 */
	@GameTest(structure = KILN, padding = 8, maxTicks = 80)
	public void aPetsBlowIsItsOwners(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		BlockPos origin = origin(helper);
		CinderTyrantEntity tyrant = waiting(helper, origin);
		floor(level, origin, 28, 28, 43, 46);
		ServerPlayer keeper = player(helper, tyrant, -2.0, 6.0);
		ServerPlayer finisher = player(helper, tyrant, 2.0, 6.0);
		List<ServerPlayer> owners = List.of(keeper, finisher);
		owners.forEach(player -> player.setGameMode(GameType.CREATIVE));  // he pays them no heed
		tyrant.wake(level);
		helper.runAfterDelay(CinderTyrantEntity.WAKE_TICKS + 3, () -> {
			var biting = helper.spawnWithNoFreeWill(EntityTypes.WOLF, new BlockPos(2, 2, 2));
			var finishing = helper.spawnWithNoFreeWill(EntityTypes.WOLF, new BlockPos(4, 2, 2));
			var wild = helper.spawnWithNoFreeWill(EntityTypes.WOLF, new BlockPos(6, 2, 2));
			biting.tame(keeper);
			finishing.tame(finisher);
			Encounter fight = tyrant.encounter();
			ending(level, tyrant, () -> {
				try {
					helper.assertTrue(tyrant.fighting() && fight.underway(), "He did not crawl out to fight: " + tyrant.phase());
					helper.assertTrue(tyrant.shed(level, 1) == 1, "No Cinderling crawled off him");
					CinderlingEntity ling = (CinderlingEntity) level.getEntity(tyrant.cinderlings().getFirst());
					boolean bitten = ling.hurtServer(level, level.damageSources().mobAttack(biting), 2.0F);
					Encounter.Share kept = fight.share(keeper);
					helper.assertTrue(bitten && kept != null && kept.damage() == 2.0F && kept.qualifies(),
							"A tamed wolf's bite on his Cinderling was not its owner's part");
					List<UUID> before = fight.qualified();
					tyrant.damageCooldownTime = 0;
					helper.assertTrue(tyrant.hurtServer(level, level.damageSources().mobAttack(wild), 4.0F), "The wild wolf's bite did not land");
					helper.assertTrue(fight.qualified().equals(before) && fight.share(finisher) == null, "A wild wolf's bite was credited to someone");
					// The finishing bite: hot he takes half of 4, and his armour leaves about 1.3, more than the 1 he has left.
					tyrant.setHealth(1.0F);
					tyrant.damageCooldownTime = 0;
					tyrant.hurtServer(level, level.damageSources().mobAttack(finishing), 4.0F);
					helper.assertTrue(!tyrant.isAlive(), "The finishing wolf's bite did not fell him");
					for (ServerPlayer owner : owners) {
						helper.assertTrue(owner.getInventory().countItem(JugcraftTyrant.TYRANT_SCALE) >= 3 && !CinderTyrantLoot.firstKill(owner),
								"An owner whose wolf fought him was not paid");
					}
				} finally {
					biting.discard();
					finishing.discard();
					wild.discard();
					owners.forEach(ServerPlayer::discard);
				}
			});
			helper.succeed();
		});
	}

	/**
	 * Concordance Chunk B: of his harm, his blows and his own fire are the fight's hurt (here his Kiln Breath), and a healer
	 * who mends them is credited; his slag's heat and burning are no creature's (proposed default B-P5), and mending them
	 * earns nothing.
	 */
	@GameTest(structure = KILN, padding = 8, maxTicks = 160)
	public void healingHisSlagAndBurningEarnsNothing(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		BlockPos origin = origin(helper);
		CinderTyrantEntity tyrant = waiting(helper, origin);
		floor(level, origin, 28, 30, 43, 46);
		ServerPlayer patient = player(helper, tyrant, 0.0, 4.0);  // before his head: his breath's
		ServerPlayer healer = player(helper, tyrant, -6.0, -3.0);  // behind his flank, out of his breath
		List<ServerPlayer> players = List.of(patient, healer);
		players.forEach(player -> player.setGameMode(GameType.CREATIVE));  // he pays them no heed while they load
		helper.runAfterDelay(LOADING_TICKS - CinderTyrantEntity.WAKE_TICKS - 2, () -> tyrant.wake(level));
		helper.runAfterDelay(LOADING_TICKS, () -> {
			players.forEach(player -> player.setGameMode(GameType.SURVIVAL));
			ending(level, tyrant, () -> {
				try {
					Encounter fight = tyrant.encounter();
					helper.assertTrue(tyrant.fighting() && fight.underway(), "Awake, his fight is not on: " + tyrant.phase());
					middle(tyrant);
					// The slag's heat and burning (no attacker, so not scaled by difficulty): 1 and 1, three times.
					for (int i = 0; i < 3; i++) {
						patient.damageCooldownTime = 0;
						MoltenSlagBlock.burn(level, patient);
						patient.damageCooldownTime = 0;
						patient.hurtServer(level, level.damageSources().onFire(), 1.0F);
					}
					float scorched = patient.getMaxHealth() - patient.getHealth();
					helper.assertTrue(scorched >= Encounter.HEALING, "The slag and burning did not hurt the patient: " + scorched);
					mend(level, healer, patient);
					Encounter.Share none = fight.share(healer);
					helper.assertTrue(none == null || (none.healing() == 0.0F && !none.qualifies()),
							"Mending what his slag and burning did was credited: " + (none == null ? "nothing" : none.healing()));
					patient.heal(patient.getMaxHealth());
					// His flame passes the hurt cooldown, so both pulses land.
					helper.assertTrue(tyrant.breathe(level) == 1 && tyrant.breathe(level) == 1, "His breath did not burn the patient twice");
					mend(level, healer, patient);
					Encounter.Share share = fight.share(healer);
					helper.assertTrue(share != null && share.healing() == Encounter.HEALING && share.qualifies(),
							"Mending what his breath did was not credited: " + (share == null ? "nothing" : share.healing()));
				} finally {
					players.forEach(ServerPlayer::discard);
				}
			});
			helper.succeed();
		});
	}

	/** The game caps a creature's health at its max_health attribute's top (1024 in 26.3): a party's scale stops there. */
	private static double capped(double health) {
		return Attributes.MAX_HEALTH.value() instanceof RangedAttribute ranged ? Math.min(health, ranged.getMaxValue()) : health;
	}

	/**
	 * Concordance Chunk B and the party cap: woken by four players his health is two and a half times his own, and when
	 * he falls each of the four is paid once, a first kill's trophy and a quarter of the experience; his fall reported
	 * again pays no one twice.
	 */
	@GameTest(structure = KILN, padding = 8)
	public void fourPlayersAreEachPaidOnce(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		BlockPos origin = origin(helper);
		CinderTyrantEntity tyrant = waiting(helper, origin);
		floor(level, origin, 28, 30, 43, 44);
		List<ServerPlayer> party = List.of(player(helper, tyrant, -3.0, 6.0), player(helper, tyrant, -1.0, 6.0), player(helper, tyrant, 1.0, 6.0),
				player(helper, tyrant, 3.0, 6.0));
		tyrant.wake(level);
		ending(level, tyrant, () -> {
			try {
				helper.assertTrue(Math.abs(tyrant.getMaxHealth() - capped(CinderTyrantEntity.HEALTH * CinderTyrantEntity.partyScale(4))) < 0.01,
						"Woken by four, his health is not " + CinderTyrantEntity.partyScale(4) + " times his own, at most the game's cap: " + tyrant.getMaxHealth());
				int[] before = new int[party.size()];
				for (int i = 0; i < party.size(); i++) {
					tyrant.took(party.get(i), 5.0F);
					before[i] = party.get(i).totalExperience;
				}
				tyrant.hurtServer(level, level.damageSources().genericKill(), Float.MAX_VALUE);
				int[] gained = new int[party.size()];
				for (int i = 0; i < party.size(); i++) {
					ServerPlayer player = party.get(i);
					gained[i] = player.totalExperience - before[i];
					helper.assertTrue(player.getInventory().countItem(JugcraftTyrant.TYRANT_SCALE) >= 3 && trophies(player) == 1
							&& gained[i] == CinderTyrantEntity.EXPERIENCE / 4, "A player of the four was not paid once: " + gained[i] + " experience");
				}
				tyrant.die(level.damageSources().genericKill());
				for (int i = 0; i < party.size(); i++) {
					ServerPlayer player = party.get(i);
					helper.assertTrue(trophies(player) == 1 && player.totalExperience - before[i] == gained[i], "His fall reported again paid someone twice");
				}
			} finally {
				party.forEach(ServerPlayer::discard);
			}
		});
		helper.succeed();
	}

	/**
	 * Concordance Chunk B: his fight's record is not saved. Saved while waking and loaded again, he holds no one's part,
	 * lands to fight, and on his first ticks begins a fresh record, so who takes part from then on is counted.
	 */
	@GameTest(structure = KILN, padding = 8, maxTicks = 40)
	public void aFightLoadedAwakeIsRecordedAfresh(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		BlockPos origin = origin(helper);
		CinderTyrantEntity tyrant = waiting(helper, origin);
		floor(level, origin, 31, 30, 40, 44);
		ServerPlayer striker = player(helper, tyrant, 0.5, 6.0);
		striker.setGameMode(GameType.CREATIVE);  // he pays them no heed
		tyrant.wake(level);
		tyrant.took(striker, 4.0F);
		boolean before = tyrant.encounter().qualified().contains(striker.getUUID());
		// Saved awake and loaded again, as his chunk unloading and loading would.
		TagValueOutput out = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, level.registryAccess());
		tyrant.saveWithoutId(out);
		tyrant.discard();
		CinderTyrantEntity loaded = new CinderTyrantEntity(JugcraftTyrant.CINDER_TYRANT, level);
		loaded.load(TagValueInput.create(ProblemReporter.DISCARDING, level.registryAccess(), out.buildResult()));
		loaded.setUUID(UUID.randomUUID());
		level.addFreshEntity(loaded);
		boolean unsaved = loaded.encounter().share(striker) == null && !loaded.encounter().underway();
		helper.runAfterDelay(3, () -> {
			ending(level, loaded, () -> {
				try {
					helper.assertTrue(before && unsaved, "His record was saved with him, or the striker never had a part");
					helper.assertTrue(loaded.phase() == CinderTyrantEntity.Phase.KILN, "Loaded waking, he did not land to fight: " + loaded.phase());
					helper.assertTrue(loaded.encounter().underway() && loaded.encounter().share(striker) == null,
							"Loaded awake, he did not begin a fresh record");
					loaded.took(striker, 2.0F);
					helper.assertTrue(loaded.encounter().qualified().contains(striker.getUUID()), "A blow after loading was not counted");
				} finally {
					striker.discard();
				}
			});
			helper.succeed();
		});
	}

	/**
	 * His JEI pages (tools/acquisition.py; Concordance Chunk B, B-02) say what the server rolls, with the default data: each
	 * share holds 3 to 6 Tyrant Scales and has a 15% chance of one of his two trophies (7.5% each, never both), a 25%
	 * chance of the Salamander Charm and a 20% chance of his Crest. A player who has already won Tempered (so no first-kill
	 * trophy) rolls {@value #SHARES} shares: none holds both trophies, and each count lies within five standard deviations
	 * of its chance (a pool's chance ignored, or his trophies split into two pools, falls outside). check_acquisition holds
	 * the same table to the pages' words; the first kill's trophy is hisLootIsEachParticipantsOwn's.
	 */
	@GameTest(maxTicks = 20)
	public void hisLootIsWhatHisPagesSay(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		ServerPlayer veteran = helper.makeMockServerPlayerInLevel();
		TrickOrTreat.award(veteran, CinderTyrantLoot.ADVANCEMENT);
		helper.assertFalse(CinderTyrantLoot.firstKill(veteran), "A player who has won Tempered rolls no first-kill trophy");
		Vec3 origin = helper.absoluteVec(new Vec3(1.5, 2.0, 1.5));
		int cinderbrand = 0;
		int magmaw = 0;
		int charm = 0;
		int crest = 0;
		try {
			for (int share = 0; share < SHARES; share++) {
				List<ItemStack> loot = CinderTyrantLoot.roll(level, origin, veteran);
				int scales = loot.stream().filter(stack -> stack.is(JugcraftTyrant.TYRANT_SCALE)).mapToInt(ItemStack::getCount).sum();
				helper.assertTrue(scales >= 3 && scales <= 6, scales + " Tyrant Scales in a share, not 3 to 6");
				long trophies = loot.stream().filter(CinderTyrantLoot::trophy).count();
				helper.assertTrue(trophies <= 1, "A share held " + trophies + " trophies: his two share one pool");
				cinderbrand += (int) loot.stream().filter(stack -> stack.is(JugcraftTyrant.cinderbrand())).count();
				magmaw += (int) loot.stream().filter(stack -> stack.is(JugcraftTyrant.magmaw())).count();
				charm += (int) loot.stream().filter(stack -> stack.is(JugcraftTyrant.SALAMANDER_CHARM)).count();
				crest += (int) loot.stream().filter(stack -> stack.is(JugcraftTyrant.TYRANT_CREST)).count();
			}
		} finally {
			veteran.discard();
		}
		helper.assertTrue(within(cinderbrand + magmaw, 0.15), (cinderbrand + magmaw) + " trophies in " + SHARES + " shares, not about 15%");
		helper.assertTrue(within(cinderbrand, 0.075) && within(magmaw, 0.075), cinderbrand + " Cinderbrands and " + magmaw + " Magmaws, not about 7.5% each");
		helper.assertTrue(within(charm, 0.25), charm + " Salamander Charms, not about 25%");
		helper.assertTrue(within(crest, 0.2), crest + " Crests, not about 20%");
		helper.succeed();
	}
}
