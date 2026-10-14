package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.lair.FenLampBlock;
import io.github.jimbozoomer.jugcraft.lair.JugcraftLairs;
import io.github.jimbozoomer.jugcraft.lair.hag.HagLikenessEntity;
import io.github.jimbozoomer.jugcraft.lair.hag.HagRootsEntity;
import io.github.jimbozoomer.jugcraft.lair.hag.HagsEye;
import io.github.jimbozoomer.jugcraft.lair.hag.JugcraftHag;
import io.github.jimbozoomer.jugcraft.lair.hag.LanternFen;
import io.github.jimbozoomer.jugcraft.lair.hag.MireHagEntity;
import io.github.jimbozoomer.jugcraft.lair.hag.MireHagLoot;
import io.github.jimbozoomer.jugcraft.lair.hag.PoisonBrewEntity;
import io.github.jimbozoomer.jugcraft.lair.hag.RootKnotEntity;
import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.animal.pig.Pig;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * The Mire Hag (docs/features/mire-hag.md), as far as a game-test server can show her: one put down on her own (with no
 * lair instance, over a fen laid out from the corner of an arena the fen's size, so her porch, her marsh and her lamps'
 * places are all loaded; each test lays the marsh's mud and the lamps it needs) waits on her porch until a player who may
 * fight her comes into the marsh, then steps down into the water with her Likenesses; her health grows with the party,
 * and the Last Light shortens her cooldowns; the lamps' light finds her and fades her Likenesses, and a lamp lit again
 * shows her and bursts the Likenesses in its light; her Likenesses walk round the lamps' light, never into it; a blow
 * bursts a Likeness and counts as taking part; her Clawing Swipe strikes before her; her Root Lanes burst along three
 * ways and hold fast, then let go; her Mire Grasp slows and her brew leaves a pool of venom; Lights Out puts out the two
 * lamps nearest her foe and sends the Root Knot on a third, which puts it out unless it is cut down; the Snuff and the
 * Likeness Exchange; the Last Light; left alone she climbs back onto her porch and the lamps burn again; her loot is
 * each participant's own; and the Hag's Eye shows what hides. Her fight in the Lantern Fen itself runs in a real world,
 * in {@link MireHagClientGameTests}.
 *
 * <p>She fights any player within 38 blocks of her marsh's middle; the arena keeps every other test's players farther off
 * than that. Each test sends her back to her porch, with her Likenesses, roots, hands, brews and knot, and takes her away.
 */
public class MireHagGameTests {
	/** An arena of air the fen's width and length (80 x 88), so everything of her fight is loaded and ticking. */
	private static final String FEN = "jugcraft-test:fen_marsh";
	/**
	 * A new player cannot be hurt until its client has loaded the world, or for 60 ticks; a mock player has no client, so
	 * a test that hurts one waits this long first.
	 */
	private static final int LOADING_TICKS = 80;
	/** How long the Likenesses' walk round the lamps' light is watched: long enough for both, as they were, to fade. */
	private static final int SWING_TICKS = 80;

	/** The origin of a fen laid out from the arena's corner, seven blocks under it: its mud bed a block over the arena's floor. */
	private static BlockPos origin(GameTestHelper helper) {
		return helper.absolutePos(new BlockPos(0, -7, 0));
	}

	/** She waits on the porch of the fen laid out from {@code origin}. */
	private static MireHagEntity waiting(GameTestHelper helper, BlockPos origin) {
		return MireHagEntity.summon(helper.getLevel(), origin, null);
	}

	/** The marsh's mud bed, out to a block past the marsh, for her, her Likenesses and players to wade on. */
	private static void marsh(ServerLevel level, BlockPos origin) {
		int radius = (int) LanternFen.MARSH_RADIUS + 1;
		int cx = (int) LanternFen.CENTRE_X;
		int cz = (int) LanternFen.CENTRE_Z;
		for (int dx = -radius; dx <= radius; dx++) {
			for (int dz = -radius; dz <= radius; dz++) {
				if (dx * dx + dz * dz <= radius * radius) {
					level.setBlock(origin.offset(cx + dx, LanternFen.MUD, cz + dz), Blocks.MUD.defaultBlockState(), Block.UPDATE_CLIENTS);
				}
			}
		}
	}

	/** The fen's four lamps, burning. */
	private static void lamps(ServerLevel level, MireHagEntity hag) {
		for (BlockPos lamp : hag.lamps()) {
			level.setBlockAndUpdate(lamp, JugcraftLairs.FEN_LAMP.defaultBlockState());
		}
	}

	/** A survival player standing {@code dx}, {@code dz} from her marsh's middle, on its mud. */
	private static ServerPlayer player(GameTestHelper helper, MireHagEntity hag, double dx, double dz) {
		ServerPlayer player = helper.makeMockServerPlayerInLevel();
		player.setGameMode(GameType.SURVIVAL);
		Vec3 at = hag.centre().add(dx, 0.0, dz);
		player.snapTo(at.x, at.y, at.z, 0.0F, 0.0F);
		return player;
	}

	/** Puts her in the middle of her marsh, facing south. */
	private static void middle(MireHagEntity hag) {
		Vec3 centre = hag.centre();
		hag.teleportTo(centre.x, centre.y, centre.z);
		hag.setYRot(0.0F);
	}

	/** Her Likeness walking first in her list, or null. */
	private static HagLikenessEntity likeness(ServerLevel level, MireHagEntity hag) {
		for (UUID id : hag.likenesses()) {
			if (level.getEntity(id) instanceof HagLikenessEntity found) {
				return found;
			}
		}
		return null;
	}

	/** Everywhere her fight reaches, with room round it. */
	private static AABB fen(BlockPos origin) {
		return new AABB(origin.getX() - 8, origin.getY(), origin.getZ() - 8, origin.getX() + 88, origin.getY() + 24, origin.getZ() + 96);
	}

	/** Checks {@code body}, then sends her back to her porch and takes her away whether it passed or failed. */
	private static void ending(ServerLevel level, MireHagEntity hag, Runnable body) {
		try {
			body.run();
		} finally {
			hag.reset(level);
			hag.discard();
		}
	}

	/**
	 * She waits on her porch, unhurt by anything, and a creative player's blow does not wake her; a player who may fight
	 * her coming into the marsh does: she looks up, unhurt while she wakes, and {@value MireHagEntity#WAKE_TICKS} ticks
	 * later she has come down into the water at full health, her {@value MireHagEntity#LIKENESSES} Likenesses round her.
	 */
	@GameTest(structure = FEN, padding = 8, maxTicks = 160)
	public void sheWaitsOnHerPorchAndAPlayerInTheMarshWakesHer(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		BlockPos origin = origin(helper);
		MireHagEntity hag = waiting(helper, origin);
		marsh(level, origin);
		ServerPlayer player = player(helper, hag, 0.0, -10.0);
		player.setGameMode(GameType.CREATIVE);
		helper.runAfterDelay(20, () -> {
			boolean waited = hag.phase() == MireHagEntity.Phase.WAITING && hag.position().distanceTo(hag.waiting()) < 0.01
					&& hag.action() == MireHagEntity.Action.STIR;
			boolean harmless = !hag.hurtServer(level, level.damageSources().playerAttack(player), 10.0F) && hag.getHealth() == hag.getMaxHealth();
			boolean slept = hag.phase() == MireHagEntity.Phase.WAITING;
			player.setGameMode(GameType.SURVIVAL);
			helper.runAfterDelay(12, () -> {
				boolean woken = hag.phase() == MireHagEntity.Phase.WAKING && hag.getHealth() == hag.getMaxHealth();
				boolean unhurt = !hag.hurtServer(level, level.damageSources().playerAttack(player), 10.0F);
				helper.runAfterDelay(MireHagEntity.WAKE_TICKS + 15, () -> {
					ending(level, hag, () -> {
						Vec3 woke = hag.woke();
						helper.assertTrue(waited, "She did not wait on her porch, stirring her cauldron");
						helper.assertTrue(harmless, "Waiting, she was harmed");
						helper.assertTrue(slept, "A creative player's blow woke her");
						helper.assertTrue(woken, "A player in the marsh did not wake her at full health");
						helper.assertTrue(unhurt, "Waking, she was harmed");
						helper.assertTrue(hag.phase() == MireHagEntity.Phase.LURE, "She did not come down to fight: " + hag.phase());
						// Down for up to 26 ticks by now, she has waded toward the player at most 3.9 blocks.
						helper.assertTrue(Math.abs(hag.getY() - woke.y) < 0.1 && Math.hypot(hag.getX() - woke.x, hag.getZ() - woke.z) < 4.5,
								"She did not come down into the water where she wakes but to " + hag.position());
						helper.assertTrue(hag.likenesses().size() == MireHagEntity.LIKENESSES, hag.likenesses().size() + " Likenesses walk with her, not "
								+ MireHagEntity.LIKENESSES);
						player.discard();
					});
					helper.succeed();
				});
			});
		});
	}

	/**
	 * Her health grows by half for each player after the first, at most two and a half times; a player's blow from afar
	 * wakes her, unhurt; poison and water never harm her. The Last Light makes every cooldown a third shorter, shows her,
	 * and leaves her no Likenesses.
	 */
	@GameTest(structure = FEN, padding = 8)
	public void herHealthGrowsWithThePartyAndTheLastLightHastensHer(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		MireHagEntity hag = waiting(helper, origin(helper));
		ServerPlayer far = player(helper, hag, 0.0, 30.0);
		ending(level, hag, () -> {
			helper.assertTrue(MireHagEntity.partyScale(1) == 1.0 && MireHagEntity.partyScale(2) == 1.5 && MireHagEntity.partyScale(4) == 2.5
					&& MireHagEntity.partyScale(9) == 2.5, "Her health scales with the party, at most 2.5 times");
			helper.assertTrue(!hag.canBeAffected(new MobEffectInstance(MobEffects.POISON, 100)) && hag.canBreatheUnderwater(),
					"Poison or water can harm her");
			helper.assertTrue(hag.cooldown(240) == 240, "Before the Last Light her cooldowns are their own");
			boolean blow = hag.hurtServer(level, level.damageSources().playerAttack(far), 10.0F);
			helper.assertTrue(!blow && hag.phase() == MireHagEntity.Phase.WAKING, "A player's blow from afar did not wake her, unhurt");
			hag.lastLight(level);
			helper.assertTrue(hag.phase() == MireHagEntity.Phase.LAST && hag.lantern() == MireHagEntity.Lantern.FLARE && hag.revealed(),
					"In the Last Light her lantern does not flare, or she is not shown");
			helper.assertTrue(hag.cooldown(240) == 160 && hag.cooldown(MireHagEntity.GLOBAL_COOLDOWN) == 13,
					"In the Last Light her cooldowns are not a third shorter: " + hag.cooldown(240));
			helper.assertTrue(hag.likenesses().isEmpty() && hag.returns().isEmpty() && hag.likenessCap() == 0,
					"In the Last Light Likenesses still walk with her, or will");
			far.discard();
		});
		helper.succeed();
	}

	/**
	 * Her rule, the light finds her: in the marsh's middle she is hidden; within a burning lamp's light she glows, and a
	 * Likeness in a lamp's light gutters and fades, forming again {@value MireHagEntity#LIKENESS_RETURN} ticks later. A
	 * lamp gone out and lit again by a player's hand shows her wherever she is, and bursts the Likeness in its light.
	 */
	@GameTest(structure = FEN, padding = 8, maxTicks = 160)
	public void theLampLightFindsHerAndHerLikenesses(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		BlockPos origin = origin(helper);
		MireHagEntity hag = waiting(helper, origin);
		marsh(level, origin);
		lamps(level, hag);
		ServerPlayer lighter = player(helper, hag, 0.0, 4.0);
		lighter.setGameMode(GameType.CREATIVE);
		hag.wake(level);
		helper.runAfterDelay(MireHagEntity.WAKE_TICKS + 15, () -> {
			boolean formed = hag.likenesses().size() == MireHagEntity.LIKENESSES;
			Vec3 centre = hag.centre();
			hag.teleportTo(centre.x, centre.y, centre.z);
			boolean hidden = !hag.revealed();
			BlockPos northWest = hag.lamps().get(0);
			BlockPos northEast = hag.lamps().get(1);
			HagLikenessEntity faded = likeness(level, hag);
			faded.teleportTo(northEast.getX() - 1.5, centre.y, northEast.getZ() + 2.5);
			hag.teleportTo(northWest.getX() + 2.5, centre.y, northWest.getZ() + 2.5);
			boolean lit = hag.revealed();
			long when = level.getGameTime();
			helper.runAfterDelay(7, () -> {
				boolean glowing = hag.hasEffect(MobEffects.GLOWING);
				boolean gone = faded.isRemoved();
				boolean comesBack = hag.returns().stream().anyMatch(time -> Math.abs(time - (when + MireHagEntity.LIKENESS_RETURN)) <= 8);
				// A lamp gone out, a Likeness in its dark, and a player's hand lights it again.
				BlockPos southWest = hag.lamps().get(2);
				FenLampBlock.snuff(level, southWest);
				hag.teleportTo(centre.x, centre.y, centre.z);
				hag.removeEffect(MobEffects.GLOWING);
				HagLikenessEntity other = likeness(level, hag);
				boolean another = other != null;
				if (another) {
					other.teleportTo(southWest.getX() + 2.5, centre.y, southWest.getZ() - 1.5);
				}
				BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(southWest), Direction.SOUTH, southWest, false);
				InteractionResult used = lighter.gameMode.useItemOn(lighter, level, ItemStack.EMPTY, InteractionHand.MAIN_HAND, hit);
				ending(level, hag, () -> {
					helper.assertTrue(formed, "Her Likenesses did not walk with her");
					helper.assertTrue(hidden, "In the marsh's middle, far from every lamp, she is shown");
					helper.assertTrue(lit && glowing, "In a lamp's light she does not glow");
					helper.assertTrue(gone, "A Likeness in a lamp's light did not fade");
					helper.assertTrue(comesBack, "The faded Likeness will not form again " + MireHagEntity.LIKENESS_RETURN + " ticks later: " + hag.returns());
					helper.assertTrue(another, "No other Likeness walked with her");
					helper.assertTrue(used.consumesAction() && MireHagEntity.burning(level, southWest), "A player's hand did not light the lamp again");
					helper.assertTrue(hag.revealTicks() == MireHagEntity.REVEAL_TICKS && hag.revealed(), "The lamp lit again did not show her");
					helper.assertTrue(other.isRemoved(), "The Likeness in the lamp's light did not burst as it was lit");
					lighter.discard();
				});
				helper.succeed();
			});
		});
	}

	/**
	 * Her Likenesses keep to the dark of their own accord. Woken with no foe, she comes down and they form round her; when
	 * she takes the player in the marsh's middle for her foe, their places swing round to either side of the marsh, past
	 * the two northern lamps, and they walk round those lamps' light, not through it: at no tick of the next
	 * {@value #SWING_TICKS} does one stand in a lamp's light, none fades, and both still walk with her.
	 */
	@GameTest(structure = FEN, padding = 8, maxTicks = 200)
	public void herLikenessesWalkRoundTheLampsLight(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		BlockPos origin = origin(helper);
		MireHagEntity hag = waiting(helper, origin);
		marsh(level, origin);
		lamps(level, hag);
		// A block west of the middle, so that her own way to the player passes clear of the lamps' light too: her Likeness
		// Exchange puts a Likeness where she stood.
		ServerPlayer foe = player(helper, hag, -1.0, 5.0);
		foe.setGameMode(GameType.CREATIVE);
		foe.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, 400, 4, false, false));
		hag.wake(level);
		int down = MireHagEntity.WAKE_TICKS + 10;
		boolean[] formed = {false};
		boolean[] lit = {false};
		helper.runAfterDelay(down, () -> {
			formed[0] = hag.likenesses().size() == MireHagEntity.LIKENESSES && hag.foe(level) == null;
			foe.setGameMode(GameType.SURVIVAL);
		});
		// Every tick of the walk is looked at. The looks are all set now, as the test begins: the server runs a test's
		// timed checks while going through its list of them, and a check that adds many more to that list breaks it.
		for (int tick = down + 1; tick <= down + SWING_TICKS; tick++) {
			helper.runAfterDelay(tick, () -> {
				for (UUID id : hag.likenesses()) {
					if (level.getEntity(id) instanceof HagLikenessEntity likeness && hag.inLampLight(level, likeness.position())) {
						lit[0] = true;
					}
				}
			});
		}
		helper.runAfterDelay(down + SWING_TICKS + 1, () -> {
			ending(level, hag, () -> {
				helper.assertTrue(formed[0], "Her Likenesses did not form round her before she had a foe");
				helper.assertTrue(hag.foe(level) == foe, "She did not take the player in the marsh for her foe");
				helper.assertTrue(!lit[0], "A Likeness walked into a lamp's light");
				helper.assertTrue(hag.likenesses().size() == MireHagEntity.LIKENESSES && hag.returns().isEmpty(), "A Likeness faded on its way: "
						+ hag.likenesses().size() + " walk with her and " + hag.returns().size() + " will form again");
				foe.discard();
			});
			helper.succeed();
		});
	}

	/**
	 * A Likeness looks as she does (her health, her lantern) and shrugs off what no one struck; a player's blow bursts it
	 * into marsh gas, counts as taking part in her fight, and it forms again later.
	 */
	@GameTest(structure = FEN, padding = 8, maxTicks = 120)
	public void aBlowBurstsALikenessAndCountsAsTakingPart(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		BlockPos origin = origin(helper);
		MireHagEntity hag = waiting(helper, origin);
		marsh(level, origin);
		ServerPlayer striker = player(helper, hag, 0.0, 6.0);
		striker.setGameMode(GameType.CREATIVE);
		hag.wake(level);
		helper.runAfterDelay(MireHagEntity.WAKE_TICKS + 15, () -> {
			HagLikenessEntity likeness = likeness(level, hag);
			boolean found = likeness != null;
			boolean mirrored = found && likeness.getMaxHealth() == hag.getMaxHealth() && likeness.getHealth() == hag.getHealth()
					&& likeness.lantern() == hag.lantern();
			boolean shrugged = found && !likeness.hurtServer(level, level.damageSources().magic(), 5.0F) && !likeness.isRemoved();
			long when = level.getGameTime();
			boolean struck = found && likeness.hurtServer(level, level.damageSources().playerAttack(striker), 5.0F);
			ending(level, hag, () -> {
				helper.assertTrue(found, "No Likeness walked with her");
				helper.assertTrue(mirrored, "A Likeness does not look as she does");
				helper.assertTrue(shrugged, "A Likeness burst at a hurt no one dealt");
				helper.assertTrue(struck && likeness.isRemoved() && !hag.likenesses().contains(likeness.getUUID()), "A blow did not burst the Likeness");
				helper.assertTrue(hag.returns().contains(when + MireHagEntity.LIKENESS_RETURN), "The burst Likeness will not form again: " + hag.returns());
				helper.assertTrue(hag.participants(level).contains(striker), "Striking a Likeness did not count as taking part");
				striker.discard();
			});
			helper.succeed();
		});
	}

	/**
	 * Her Clawing Swipe strikes a player before her and one forty-five degrees aside, and spares the one beside her, the
	 * one behind her and one out of reach. They wait out their loading in creative, where she pays them no heed.
	 */
	@GameTest(structure = FEN, padding = 8, maxTicks = 180)
	public void herClawingSwipeStrikesBeforeHer(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		MireHagEntity hag = waiting(helper, origin(helper));
		double aside = Math.toRadians(45.0);
		ServerPlayer front = player(helper, hag, 0.0, 2.5);
		ServerPlayer slant = player(helper, hag, 2.5 * Math.sin(aside), 2.5 * Math.cos(aside));
		ServerPlayer beside = player(helper, hag, -2.5, 0.0);
		ServerPlayer behind = player(helper, hag, 0.0, -2.5);
		ServerPlayer far = player(helper, hag, 0.0, 5.0);
		List<ServerPlayer> players = List.of(front, slant, beside, behind, far);
		players.forEach(player -> player.setGameMode(GameType.CREATIVE));
		helper.runAfterDelay(LOADING_TICKS, () -> {
			players.forEach(player -> player.setGameMode(GameType.SURVIVAL));
			ending(level, hag, () -> {
				middle(hag);  // facing south, towards the one in front
				int hit = hag.swipe(level);
				helper.assertTrue(hit == 2, "The swipe struck " + hit + " players, not 2");
				for (ServerPlayer struck : List.of(front, slant)) {
					helper.assertTrue(struck.getHealth() < struck.getMaxHealth(), "A player before her was not struck");
				}
				for (ServerPlayer spared : List.of(beside, behind, far)) {
					helper.assertTrue(spared.getHealth() == spared.getMaxHealth(), "A player beside, behind or out of reach was struck");
				}
				players.forEach(ServerPlayer::discard);
			});
			helper.succeed();
		});
	}

	/**
	 * Her Root Lanes: as the wind-up ends roots burst along three lanes, a clump every {@value MireHagEntity#CLUMP_STEP}
	 * blocks; a player on the middle lane and one on a side lane are struck and held fast (no speed left and no jump),
	 * one between the lanes and one past their end are spared; {@value MireHagEntity#ROOT_HOLD} ticks later the held are
	 * let go, and once the roots have stood and sunk they are gone.
	 */
	@GameTest(structure = FEN, padding = 8, maxTicks = 260)
	public void herRootLanesBurstAlongThreeWaysAndHoldFast(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		BlockPos origin = origin(helper);
		MireHagEntity hag = waiting(helper, origin);
		marsh(level, origin);
		double side = Math.toRadians(MireHagEntity.LANE_SPREAD);
		double between = Math.toRadians(MireHagEntity.LANE_SPREAD / 2.0);
		ServerPlayer middle = player(helper, hag, 0.0, 6.0);
		ServerPlayer onSide = player(helper, hag, 8.0 * Math.sin(side), 8.0 * Math.cos(side));
		ServerPlayer betweenLanes = player(helper, hag, 10.0 * Math.sin(between), 10.0 * Math.cos(between));
		ServerPlayer beyond = player(helper, hag, 0.0, MireHagEntity.LANE_LENGTH + 1.5);
		List<ServerPlayer> players = List.of(middle, onSide, betweenLanes, beyond);
		players.forEach(player -> player.setGameMode(GameType.CREATIVE));
		hag.wake(level);
		helper.runAfterDelay(LOADING_TICKS, () -> {
			players.forEach(player -> player.setGameMode(GameType.SURVIVAL));
			middle(hag);
			hag.begin(level, MireHagEntity.Attack.ROOT_LANES, middle);
			int burst = MireHagEntity.Attack.ROOT_LANES.windup + 2;
			helper.runAfterDelay(burst, () -> {
				int clumps = level.getEntitiesOfClass(HagRootsEntity.class, fen(origin)).size();
				int expected = MireHagEntity.LANES * (int) Math.floor(MireHagEntity.LANE_LENGTH / MireHagEntity.CLUMP_STEP + 1.0E-6);
				helper.assertTrue(clumps == expected, clumps + " clumps of roots burst, not " + expected);
				for (ServerPlayer held : List.of(middle, onSide)) {
					MobEffectInstance slowness = held.getEffect(MobEffects.SLOWNESS);
					helper.assertTrue(held.getHealth() < held.getMaxHealth() && HagRootsEntity.held(held) && slowness != null
							&& slowness.getAmplifier() == HagRootsEntity.HELD_AMPLIFIER, "A player on a lane was not struck and held fast");
				}
				for (ServerPlayer spared : List.of(betweenLanes, beyond)) {
					helper.assertTrue(spared.getHealth() == spared.getMaxHealth() && !HagRootsEntity.held(spared),
							"A player between the lanes or past their end was struck");
				}
			});
			helper.runAfterDelay(burst + MireHagEntity.ROOT_HOLD + 3, () -> {
				helper.assertTrue(!HagRootsEntity.held(middle) && !HagRootsEntity.held(onSide), "The roots did not let go");
			});
			helper.runAfterDelay(burst + MireHagEntity.Attack.ROOT_LANES.active + HagRootsEntity.SINK + 4, () -> {
				ending(level, hag, () -> {
					helper.assertTrue(level.getEntitiesOfClass(HagRootsEntity.class, fen(origin)).isEmpty(), "Her roots did not sink and go");
					players.forEach(ServerPlayer::discard);
				});
				helper.succeed();
			});
		});
	}

	/**
	 * Her Mire Grasp strikes the player on its mark and slows them, and spares one beside it; a flask of her brew flies
	 * onto its mark and shatters into a pool of venom that poisons the player standing in it; at most
	 * {@value MireHagEntity#MAX_POOLS} pools lie at once.
	 */
	@GameTest(structure = FEN, padding = 8, maxTicks = 200)
	public void herGraspSlowsAndHerBrewLeavesAPoolOfVenom(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		BlockPos origin = origin(helper);
		MireHagEntity hag = waiting(helper, origin);
		marsh(level, origin);
		ServerPlayer grasped = player(helper, hag, 0.0, 0.0);
		ServerPlayer clear = player(helper, hag, 3.0, 0.0);
		ServerPlayer brewed = player(helper, hag, -6.0, 4.0);
		List<ServerPlayer> players = List.of(grasped, clear, brewed);
		players.forEach(player -> player.setGameMode(GameType.CREATIVE));
		helper.runAfterDelay(LOADING_TICKS, () -> {
			players.forEach(player -> player.setGameMode(GameType.SURVIVAL));
			hag.mark(new Vec3(grasped.getX(), hag.surfaceY(), grasped.getZ()));
			int hit = hag.grasp(level);
			MobEffectInstance slowness = grasped.getEffect(MobEffects.SLOWNESS);
			boolean slowed = slowness != null && slowness.getAmplifier() == MireHagEntity.GRASP_SLOW_AMPLIFIER;
			boolean struck = grasped.getHealth() < grasped.getMaxHealth();
			boolean spared = clear.getHealth() == clear.getMaxHealth();
			// Lobbed while she wakes (she strikes no one then), it lands and poisons long before she could.
			PoisonBrewEntity brew = PoisonBrewEntity.lob(level, hag, brewed.position());
			helper.runAfterDelay(60, () -> {
				ending(level, hag, () -> {
					helper.assertTrue(hit == 1 && struck && slowed, "The grasp did not strike and slow the player on its mark: " + hit);
					helper.assertTrue(spared, "The grasp struck the player beside its mark");
					helper.assertTrue(brew.isRemoved(), "The flask did not shatter");
					helper.assertTrue(hag.pools().size() == 1 && Math.hypot(hag.pools().getFirst().at().x - brewed.getX(),
							hag.pools().getFirst().at().z - brewed.getZ()) < 1.0, "No pool of venom where the flask fell: " + hag.pools());
					helper.assertTrue(brewed.hasEffect(MobEffects.POISON), "The pool did not poison the player in it");
					for (int i = 0; i <= MireHagEntity.MAX_POOLS; i++) {
						hag.venomPool(level, hag.centre().add(i, 0.0, 0.0));
					}
					helper.assertTrue(hag.pools().size() == MireHagEntity.MAX_POOLS, hag.pools().size() + " pools lie at once, not at most "
							+ MireHagEntity.MAX_POOLS);
					players.forEach(ServerPlayer::discard);
				});
				helper.succeed();
			});
		});
	}

	/**
	 * Lights Out: she cackles, unhurt; the two lamps nearest her foe go out, and the Root Knot bursts from the ring and
	 * crawls on the lamp nearest after them. {@value MireHagEntity#DARK_TICKS} ticks later the Drowned Dark begins, and
	 * {@value MireHagEntity#DARK_LIKENESSES} Likenesses walk with her, or will.
	 */
	@GameTest(structure = FEN, padding = 8, maxTicks = 220)
	public void lightsOutPutsOutTwoLampsAndSendsTheRootKnot(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		BlockPos origin = origin(helper);
		MireHagEntity hag = waiting(helper, origin);
		marsh(level, origin);
		lamps(level, hag);
		ServerPlayer foe = player(helper, hag, -7.5, -5.5);  // near the north-west lamp, then the south-west one
		foe.setGameMode(GameType.CREATIVE);
		hag.wake(level);
		helper.runAfterDelay(LOADING_TICKS, () -> {
			foe.setGameMode(GameType.SURVIVAL);
			foe.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, 400, 4));
			List<BlockPos> lamps = hag.lamps();
			hag.setHealth(hag.getMaxHealth() * 0.5F);
			hag.lightsOut(level, foe);
			boolean cackling = hag.phase() == MireHagEntity.Phase.DARKENING && hag.action() == MireHagEntity.Action.CACKLE;
			boolean out = !MireHagEntity.burning(level, lamps.get(0)) && !MireHagEntity.burning(level, lamps.get(2))
					&& MireHagEntity.burning(level, lamps.get(1)) && MireHagEntity.burning(level, lamps.get(3));
			boolean knot = hag.knot() != null && level.getEntity(hag.knot()) instanceof RootKnotEntity crawling
					&& lamps.get(1).equals(crawling.lamp());
			boolean unhurt = !hag.hurtServer(level, level.damageSources().playerAttack(foe), 10.0F);
			helper.runAfterDelay(MireHagEntity.DARK_TICKS + 12, () -> {
				ending(level, hag, () -> {
					helper.assertTrue(cackling, "Lights Out did not begin with her cackling");
					helper.assertTrue(out, "Lights Out did not put out the two lamps nearest her foe, and those alone");
					helper.assertTrue(knot, "The Root Knot did not crawl on the lamp nearest after them");
					helper.assertTrue(unhurt, "Crying Lights Out, she was harmed");
					helper.assertTrue(hag.phase() == MireHagEntity.Phase.DROWNED, "The Drowned Dark did not begin: " + hag.phase());
					helper.assertTrue(hag.likenessCap() == MireHagEntity.DARK_LIKENESSES
							&& hag.likenesses().size() + hag.returns().size() == MireHagEntity.DARK_LIKENESSES,
							"In the Drowned Dark " + hag.likenesses().size() + " Likenesses walk and " + hag.returns().size() + " will, not "
									+ MireHagEntity.DARK_LIKENESSES);
					foe.discard();
				});
				helper.succeed();
			});
		});
	}

	/**
	 * The Root Knot bursts up on the ring {@value RootKnotEntity#START} blocks along it from its lamp and crawls toward it;
	 * let alone, it reaches the lamp, which goes out, and sinks.
	 */
	@GameTest(structure = FEN, padding = 8, maxTicks = 260)
	public void theRootKnotPutsOutItsLamp(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		BlockPos origin = origin(helper);
		MireHagEntity hag = waiting(helper, origin);
		marsh(level, origin);
		lamps(level, hag);
		hag.wake(level);
		helper.runAfterDelay(MireHagEntity.WAKE_TICKS + 5, () -> {
			BlockPos lamp = hag.lamps().get(1);
			RootKnotEntity knot = RootKnotEntity.burst(level, hag, lamp, true);
			Vec3 centre = hag.centre();
			boolean onRing = Math.abs(Math.hypot(knot.getX() - centre.x, knot.getZ() - centre.z) - RootKnotEntity.RING) < 0.01
					&& Math.abs(knot.getY() - (origin.getY() + LanternFen.STAND)) < 0.01;
			double from = knot.toLamp();
			helper.runAfterDelay(20, () -> helper.assertTrue(knot.isAlive() && knot.toLamp() < from && MireHagEntity.burning(level, lamp),
					"The Root Knot does not crawl toward its lamp"));
			int crawl = (int) Math.ceil((RootKnotEntity.START - RootKnotEntity.REACH) / RootKnotEntity.SPEED);
			helper.runAfterDelay(crawl + 30, () -> {
				ending(level, hag, () -> {
					helper.assertTrue(onRing, "The Root Knot did not burst up on the ring's boards");
					helper.assertTrue(knot.isRemoved(), "The Root Knot did not sink at its lamp");
					helper.assertTrue(!MireHagEntity.burning(level, lamp), "The Root Knot reached its lamp, and it burns on");
				});
				helper.succeed();
			});
		});
	}

	/** Cut down, the Root Knot dies short of its lamp, which burns on, and whoever cut it has taken part. */
	@GameTest(structure = FEN, padding = 8, maxTicks = 160)
	public void theRootKnotCutDownSparesItsLamp(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		BlockPos origin = origin(helper);
		MireHagEntity hag = waiting(helper, origin);
		marsh(level, origin);
		lamps(level, hag);
		ServerPlayer cutter = player(helper, hag, 10.0, -10.0);
		cutter.setGameMode(GameType.CREATIVE);
		hag.wake(level);
		helper.runAfterDelay(MireHagEntity.WAKE_TICKS + 5, () -> {
			BlockPos lamp = hag.lamps().get(1);
			RootKnotEntity knot = RootKnotEntity.burst(level, hag, lamp, false);
			knot.hurtServer(level, level.damageSources().playerAttack(cutter), RootKnotEntity.HEALTH + 10.0F);
			boolean cut = knot.isDeadOrDying();
			helper.runAfterDelay(30, () -> {
				ending(level, hag, () -> {
					helper.assertTrue(cut && knot.isRemoved(), "The Root Knot was not cut down");
					helper.assertTrue(MireHagEntity.burning(level, lamp), "Cut down, the Root Knot still put out its lamp");
					helper.assertTrue(hag.participants(level).contains(cutter), "Cutting the Root Knot down did not count as taking part");
					cutter.discard();
				});
				helper.succeed();
			});
		});
	}

	/**
	 * The Likeness Exchange: she trades places with a Likeness, whichever she picks. The Snuff: as its wind-up ends the
	 * lamp burning nearest her goes out.
	 */
	@GameTest(structure = FEN, padding = 8, maxTicks = 200)
	public void theLikenessExchangeAndTheSnuff(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		BlockPos origin = origin(helper);
		MireHagEntity hag = waiting(helper, origin);
		marsh(level, origin);
		lamps(level, hag);
		ServerPlayer foe = player(helper, hag, 0.0, 6.0);
		foe.setGameMode(GameType.CREATIVE);
		hag.wake(level);
		helper.runAfterDelay(LOADING_TICKS, () -> {
			foe.setGameMode(GameType.SURVIVAL);
			foe.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, 400, 4));
			List<HagLikenessEntity> walking = hag.likenesses().stream().map(level::getEntity).filter(HagLikenessEntity.class::isInstance)
					.map(HagLikenessEntity.class::cast).toList();
			boolean found = !walking.isEmpty();
			Vec3 hers = hag.position();
			List<Vec3> theirs = walking.stream().map(HagLikenessEntity::position).toList();
			boolean moved = found && hag.exchange(level);
			// She trades with one of them, whichever she picks: it now stands where she stood, and she where it stood.
			boolean traded = IntStream.range(0, walking.size()).anyMatch(i -> walking.get(i).position().distanceTo(hers) < 0.01
					&& hag.position().distanceTo(theirs.get(i)) < 0.01);
			middle(hag);
			BlockPos nearest = hag.nearestLitLamp(level, hag.position());
			hag.begin(level, MireHagEntity.Attack.SNUFF, foe);
			helper.runAfterDelay(MireHagEntity.Attack.SNUFF.windup + 3, () -> {
				ending(level, hag, () -> {
					helper.assertTrue(found, "No Likeness walked with her");
					helper.assertTrue(moved && traded, "She did not trade places with her Likeness");
					helper.assertTrue(nearest != null && !MireHagEntity.burning(level, nearest), "The Snuff did not put out the lamp nearest her");
					helper.assertTrue(hag.lamps().stream().filter(lamp -> MireHagEntity.burning(level, lamp)).count() == 3,
							"The Snuff put out more than one lamp");
					foe.discard();
				});
				helper.succeed();
			});
		});
	}

	/** The Last Light: every Likeness bursts and none comes back, and she glows. */
	@GameTest(structure = FEN, padding = 8, maxTicks = 160)
	public void theLastLightBurstsHerLikenessesAndShowsHer(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		BlockPos origin = origin(helper);
		MireHagEntity hag = waiting(helper, origin);
		marsh(level, origin);
		hag.wake(level);
		helper.runAfterDelay(MireHagEntity.WAKE_TICKS + 15, () -> {
			List<HagLikenessEntity> walking = hag.likenesses().stream().map(level::getEntity)
					.filter(HagLikenessEntity.class::isInstance).map(HagLikenessEntity.class::cast).toList();
			hag.lastLight(level);
			boolean burst = !walking.isEmpty() && walking.stream().allMatch(HagLikenessEntity::isRemoved);
			helper.runAfterDelay(25, () -> {
				ending(level, hag, () -> {
					helper.assertTrue(burst, "Her Likenesses did not burst in the Last Light");
					helper.assertTrue(hag.likenesses().isEmpty() && hag.returns().isEmpty(), "A Likeness came back in the Last Light");
					helper.assertTrue(hag.hasEffect(MobEffects.GLOWING), "In the Last Light she does not glow");
					helper.assertTrue(hag.lantern() == MireHagEntity.Lantern.FLARE, "In the Last Light her lantern does not flare");
				});
				helper.succeed();
			});
		});
	}

	/**
	 * Left alone {@value MireHagEntity#ABANDON_TICKS} ticks, she climbs back onto her porch, healed: her Likenesses burst,
	 * the lamps burn again, and who hurt her is forgotten.
	 */
	@GameTest(structure = FEN, padding = 8, maxTicks = 340)
	public void leftAloneSheClimbsBackOntoHerPorch(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		BlockPos origin = origin(helper);
		MireHagEntity hag = waiting(helper, origin);
		marsh(level, origin);
		lamps(level, hag);
		ServerPlayer gone = player(helper, hag, 0.0, 30.0);
		gone.setGameMode(GameType.CREATIVE);
		hag.wake(level);
		helper.runAfterDelay(MireHagEntity.WAKE_TICKS + 15, () -> {
			List<UUID> walking = hag.likenesses();
			hag.took(gone, 5.0F);
			hag.setHealth(hag.getMaxHealth() * 0.8F);
			FenLampBlock.snuff(level, hag.lamps().get(1));
			boolean counted = hag.participants(level).contains(gone);
			helper.runAfterDelay(MireHagEntity.ABANDON_TICKS + 20, () -> {
				ending(level, hag, () -> {
					helper.assertTrue(counted, "Whoever hurt her was not counted");
					helper.assertTrue(hag.phase() == MireHagEntity.Phase.WAITING && hag.position().distanceTo(hag.waiting()) < 0.1,
							"Left alone she did not climb back onto her porch: " + hag.phase() + " at " + hag.position());
					helper.assertTrue(hag.getHealth() == hag.getMaxHealth(), "Back on her porch she is not healed");
					helper.assertTrue(hag.lamps().stream().allMatch(lamp -> MireHagEntity.burning(level, lamp)), "The lamps do not all burn again");
					helper.assertTrue(hag.likenesses().isEmpty() && walking.stream().noneMatch(id -> level.getEntity(id) instanceof HagLikenessEntity),
							"Her Likenesses outlived her climbing back");
					helper.assertTrue(hag.participants(level).isEmpty(), "Who hurt her was not forgotten");
					gone.discard();
				});
				helper.succeed();
			});
		});
	}

	/**
	 * Her loot is each participant's own: everyone who struck her rolls for themselves (Hagmoss, and on a first kill always
	 * one of her trophies) into their own inventory, and a bystander who never struck her gets nothing. Afterwards Seen
	 * Through is theirs, and a later roll no longer counts as a first kill. As she falls her lantern goes out.
	 */
	@GameTest(structure = FEN, padding = 8, maxTicks = 60)
	public void herLootIsEachParticipantsOwn(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		MireHagEntity hag = waiting(helper, origin(helper));
		ServerPlayer first = player(helper, hag, 0.5, 3.0);
		ServerPlayer second = player(helper, hag, 2.5, 3.0);
		ServerPlayer bystander = player(helper, hag, -1.5, 3.0);
		hag.took(first, 10.0F);
		hag.took(second, 4.0F);
		ending(level, hag, () -> {
			List<ServerPlayer> participants = hag.participants(level);
			helper.assertTrue(participants.contains(first) && participants.contains(second), "Both who struck her take part");
			helper.assertFalse(participants.contains(bystander), "A bystander who never struck her does");
			helper.assertTrue(MireHagLoot.firstKill(first), "A newcomer has already won Seen Through");
			List<ItemStack> loot = MireHagLoot.roll(level, hag, first);
			int moss = loot.stream().filter(stack -> stack.is(JugcraftHag.HAGMOSS)).mapToInt(ItemStack::getCount).sum();
			helper.assertTrue(moss >= 3 && moss <= 6, moss + " Hagmoss, not 3 to 6");
			helper.assertTrue(loot.stream().filter(MireHagLoot::trophy).count() == 1, "A first kill always brings one of her trophies");
			hag.hurtServer(level, level.damageSources().genericKill(), Float.MAX_VALUE);
			helper.assertTrue(hag.isDeadOrDying() && hag.lantern() == MireHagEntity.Lantern.OUT, "She did not fall with her lantern going out");
			for (ServerPlayer player : List.of(first, second)) {
				helper.assertTrue(player.getInventory().countItem(JugcraftHag.HAGMOSS) >= 3, "A participant's loot did not go into their inventory");
				int trophies = player.getInventory().countItem(JugcraftHag.hagthorn()) + player.getInventory().countItem(JugcraftHag.bogfang());
				helper.assertTrue(trophies == 1, "A first kill brought " + trophies + " of her trophies, not 1");
				helper.assertFalse(MireHagLoot.firstKill(player), "Seen Through is not theirs");
			}
			helper.assertTrue(bystander.getInventory().countItem(JugcraftHag.HAGMOSS) == 0, "The bystander got loot");
			helper.assertTrue(MireHagLoot.firstKill(bystander), "The bystander won Seen Through");
			for (ServerPlayer player : List.of(first, second, bystander)) {
				player.discard();
			}
		});
		helper.succeed();
	}

	/**
	 * Her Likenesses keep their places round her, {@value MireHagEntity#LIKENESS_SPREAD} degrees apart on either side, and
	 * never out of the marsh nor in a lamp's light.
	 */
	@GameTest(structure = FEN, padding = 8)
	public void herLikenessesKeepTheirPlacesOutOfTheLight(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		BlockPos origin = origin(helper);
		MireHagEntity hag = waiting(helper, origin);
		lamps(level, hag);
		ending(level, hag, () -> {
			helper.assertTrue(MireHagEntity.slotAngle(1) == MireHagEntity.LIKENESS_SPREAD && MireHagEntity.slotAngle(2) == -MireHagEntity.LIKENESS_SPREAD
					&& MireHagEntity.slotAngle(3) == 2 * MireHagEntity.LIKENESS_SPREAD && MireHagEntity.slotAngle(4) == -2 * MireHagEntity.LIKENESS_SPREAD,
					"Her Likenesses' places are not " + MireHagEntity.LIKENESS_SPREAD + " degrees apart on either side");
			middle(hag);
			for (int slot = 1; slot <= MireHagEntity.DARK_LIKENESSES; slot++) {
				Vec3 spot = hag.likenessSpot(level, slot);
				helper.assertTrue(spot != null && !hag.inLampLight(level, spot)
						&& LanternFen.fromCentre(spot.x - origin.getX(), spot.z - origin.getZ()) <= LanternFen.MARSH_RADIUS - 1.0 + 1.0E-6,
						"From the marsh's middle, the place of Likeness " + slot + " is " + spot);
			}
			BlockPos lamp = hag.lamps().getFirst();
			hag.teleportTo(lamp.getX() + 2.5, hag.centre().y, lamp.getZ() + 2.5);
			for (int slot = 1; slot <= MireHagEntity.DARK_LIKENESSES; slot++) {
				Vec3 spot = hag.likenessSpot(level, slot);
				helper.assertTrue(spot == null || !hag.inLampLight(level, spot), "Beside a lamp, Likeness " + slot + "'s place is in its light: " + spot);
			}
		});
		helper.succeed();
	}

	/**
	 * The Hag's Eye held in the offhand: an invisible creature near its holder glows, and a visible one and an invisible one
	 * far off do not; the server looks again every {@value HagsEye#EVERY} ticks, and once the Eye is put away, no more. The
	 * creatures are made invisible by the Invisibility effect, as the game makes them: a creature's invisibility follows its
	 * effects whenever they change.
	 */
	@GameTest(structure = FEN, padding = 8, maxTicks = 100)
	public void theHagsEyeShowsWhatHides(GameTestHelper helper) {
		ServerPlayer seer = helper.makeMockServerPlayerInLevel();
		seer.setGameMode(GameType.CREATIVE);
		// Keep even the out-of-range subject inside this test's ticking arena. Teleporting it outside the small
		// default structure made its effect tick depend on whether a neighbouring test happened to load its chunk.
		BlockPos at = helper.absolutePos(new BlockPos(20, 1, 20));
		seer.snapTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, 0.0F, 0.0F);
		Pig hidden = helper.spawn(EntityTypes.PIG, new BlockPos(22, 1, 20));
		Pig plain = helper.spawn(EntityTypes.PIG, new BlockPos(20, 1, 22));
		Pig far = helper.spawn(EntityTypes.PIG, new BlockPos(40, 1, 20));
		List<Pig> pigs = List.of(hidden, plain, far);
		pigs.forEach(pig -> { pig.setNoGravity(true); pig.setNoAi(true); });
		for (Pig unseen : List.of(hidden, far)) {
			unseen.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, 1200, 0, false, false));
		}
		far.teleportTo(seer.getX() + HagsEye.RANGE + 4.0, seer.getY(), seer.getZ());
		// The effect makes them invisible on their next tick.
		helper.runAfterDelay(2, () -> {
			boolean hiding = hidden.isInvisible() && far.isInvisible() && !plain.isInvisible();
			boolean bare = !HagsEye.holds(seer);
			seer.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(JugcraftHag.HAGS_EYE));
			int seen = HagsEye.see(seer);
			boolean shown = hidden.hasEffect(MobEffects.GLOWING) && !plain.hasEffect(MobEffects.GLOWING) && !far.hasEffect(MobEffects.GLOWING);
			hidden.removeEffect(MobEffects.GLOWING);
			helper.runAfterDelay(HagsEye.EVERY + 2, () -> {
				boolean again = hidden.hasEffect(MobEffects.GLOWING) && hidden.isInvisible();
				seer.setItemInHand(InteractionHand.OFF_HAND, ItemStack.EMPTY);
				hidden.removeEffect(MobEffects.GLOWING);
				helper.runAfterDelay(HagsEye.EVERY + 2, () -> {
					helper.assertTrue(hiding, "The Invisibility effect did not hide the pigs");
					helper.assertTrue(bare, "With nothing in the offhand, the Eye is held");
					helper.assertTrue(seen == 1 && shown, "The Eye showed " + seen + " creatures, not the one invisible creature near");
					helper.assertTrue(again, "Held, the Eye was not looked through again on the server's tick");
					helper.assertTrue(!hidden.hasEffect(MobEffects.GLOWING) && hidden.isInvisible(), "Put away, the Eye still shows what hides");
					pigs.forEach(Pig::discard);
					seer.discard();
					helper.succeed();
				});
			});
		});
	}
}
