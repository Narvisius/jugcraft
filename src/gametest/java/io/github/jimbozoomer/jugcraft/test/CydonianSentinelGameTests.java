package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.lair.CydonianSanctum;
import io.github.jimbozoomer.jugcraft.lair.JugcraftLairs;
import io.github.jimbozoomer.jugcraft.lair.sentinel.CydonianSentinelEntity;
import io.github.jimbozoomer.jugcraft.lair.sentinel.CydonianSentinelEntity.Attack;
import io.github.jimbozoomer.jugcraft.lair.sentinel.CydonianSentinelEntity.Wind;
import io.github.jimbozoomer.jugcraft.lair.sentinel.JugcraftSentinel;
import io.github.jimbozoomer.jugcraft.lair.sentinel.LowfallCore;
import io.github.jimbozoomer.jugcraft.lair.sentinel.QuakeSpikeEntity;
import io.github.jimbozoomer.jugcraft.lair.sentinel.RustShardEntity;
import io.github.jimbozoomer.jugcraft.lair.sentinel.SentinelLoot;
import io.github.jimbozoomer.jugcraft.lair.sentinel.SentinelMeteorEntity;
import io.github.jimbozoomer.jugcraft.lair.sentinel.ShardSentryEntity;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * The Cydonian Sentinel (docs/features/cydonian-sentinel.md), as far as a game-test server can show it: one put down on
 * its own (with no lair instance, over a Sanctum laid out from beyond the corner of an arena the crater's size, so its
 * plinth, its crater and its rim's wall are all loaded; each test lays the crater's floor and the standing stones it
 * needs) waits on its plinth until a player who may fight it stands on the crater's floor, then steps down, its core
 * lit; its health grows with the party, and the Red Giant shortens its cooldowns; as it charges its Core Lance its plates
 * open, its core takes double harm and a strong blow staggers it; the Red Storm scours whoever is not in the lee of rock,
 * and pushes them downwind; its Hammerfall, Shard Volley, Quake Line and lances (rock stops them); at six tenths it
 * cracks, unhurt, and its sentries rise, never more than four; at a quarter its shell bursts and the core floats free,
 * and its Starfall's meteors strike their marks and sink it, dimmed; it keeps to its crater; left alone it strides back
 * to its plinth; its loot is each participant's own; and the Lowfall Core keeps its holder falling slowly. Its fight in
 * the Cydonian Sanctum itself runs in a real world, in {@link CydonianSentinelClientGameTests}.
 *
 * <p>It fights any player within 38 blocks of its crater's middle; the arena keeps every other test's players farther
 * off than that. Each test sends it back to its plinth, with its sentries and its shards, spikes and meteors, and takes
 * it away.
 */
public class CydonianSentinelGameTests {
	/** An arena of air (78 x 78), as big as the crater with its rim's wall and its plinth. */
	private static final String ARENA = "jugcraft-test:crypt_hall";
	/**
	 * A new player cannot be hurt until its client has loaded the world, or for 60 ticks; a mock player has no client, so
	 * a test that hurts one waits this long first.
	 */
	private static final int LOADING_TICKS = 80;
	/** A creature hurt shrugs off a blow no harder than the last for 10 ticks after (vanilla's); a test waits this long between blows. */
	private static final int HURT_COOLDOWN = 12;
	/** A tick or two past its waking, when it has stepped down to the crater's floor with its core lit. */
	private static final int AWAKE = CydonianSentinelEntity.WAKE_TICKS + 5;

	/** The origin of a Sanctum laid out so its crater lies in the arena: the crater's floor two blocks over the arena's. */
	private static BlockPos origin(GameTestHelper helper) {
		return helper.absolutePos(new BlockPos(-5, 2 - CydonianSanctum.STAND, -14));
	}

	/** It waits on the plinth of the Sanctum laid out from {@code origin}. */
	private static CydonianSentinelEntity waiting(GameTestHelper helper, BlockPos origin) {
		return CydonianSentinelEntity.summon(helper.getLevel(), origin, null);
	}

	/**
	 * The crater's floor in every cell whose middle is within its radius: red sandstone, not the Sanctum's red sand, which
	 * here would have nothing under it and fall.
	 */
	private static void crater(ServerLevel level, BlockPos origin) {
		BlockState floor = Blocks.RED_SANDSTONE.defaultBlockState();
		int reach = (int) Math.ceil(CydonianSanctum.CRATER);
		int cx = (int) Math.floor(CydonianSanctum.CENTRE_X);
		int cz = (int) Math.floor(CydonianSanctum.CENTRE_Z);
		for (int x = cx - reach; x <= cx + reach; x++) {
			for (int z = cz - reach; z <= cz + reach; z++) {
				if (Math.hypot(x + 0.5 - CydonianSanctum.CENTRE_X, z + 0.5 - CydonianSanctum.CENTRE_Z) <= CydonianSanctum.CRATER) {
					level.setBlock(origin.offset(x, CydonianSanctum.PAVE, z), floor, Block.UPDATE_CLIENTS);
				}
			}
		}
	}

	/** The standing stone {@code stone} (an index of {@link CydonianSanctum#STONES}), as tall as the Sanctum's. */
	private static void stone(ServerLevel level, BlockPos origin, int stone) {
		BlockState rock = Blocks.RED_SANDSTONE.defaultBlockState();
		int[] cell = CydonianSanctum.STONES[stone];
		int half = CydonianSanctum.STONE_SIZE / 2;
		for (int x = cell[0] - half; x <= cell[0] + half; x++) {
			for (int z = cell[1] - half; z <= cell[1] + half; z++) {
				for (int y = CydonianSanctum.STAND; y < CydonianSanctum.STAND + CydonianSanctum.STONE_HEIGHT; y++) {
					level.setBlock(origin.offset(x, y, z), rock, Block.UPDATE_CLIENTS);
				}
			}
		}
	}

	/** A survival player standing on the crater's floor at the template's ({@code x}, {@code z}). */
	private static ServerPlayer player(GameTestHelper helper, BlockPos origin, double x, double z) {
		ServerPlayer player = helper.makeMockServerPlayerInLevel();
		player.setGameMode(GameType.SURVIVAL);
		player.snapTo(origin.getX() + x, origin.getY() + CydonianSanctum.STAND, origin.getZ() + z, 0.0F, 0.0F);
		return player;
	}

	/** The template's ({@code x}, {@code z}) on the crater's floor, in the world. */
	private static Vec3 at(BlockPos origin, double x, double z) {
		return new Vec3(origin.getX() + x, origin.getY() + CydonianSanctum.STAND, origin.getZ() + z);
	}

	/** Puts it on the crater's floor at the template's ({@code x}, {@code z}), facing south. */
	private static void stand(CydonianSentinelEntity sentinel, double x, double z) {
		BlockPos origin = sentinel.origin();
		sentinel.teleportTo(origin.getX() + x, sentinel.floorY(), origin.getZ() + z);
		sentinel.setYRot(0.0F);
	}

	private static boolean hurt(ServerPlayer player) {
		return player.getHealth() < player.getMaxHealth();
	}

	/** Its Shard Sentries floating now. */
	private static List<ShardSentryEntity> sentries(ServerLevel level, CydonianSentinelEntity sentinel) {
		List<ShardSentryEntity> out = new ArrayList<>();
		for (UUID id : sentinel.sentries()) {
			if (level.getEntity(id) instanceof ShardSentryEntity found) {
				out.add(found);
			}
		}
		return out;
	}

	/** Everywhere its fight reaches, with room round it. */
	private static AABB crater(BlockPos origin) {
		return new AABB(origin.getX() + 4, origin.getY(), origin.getZ() + 10, origin.getX() + 70, origin.getY() + 40, origin.getZ() + 82);
	}

	/** Checks {@code body}, then sends it back to its plinth and takes it away whether it passed or failed. */
	private static void ending(ServerLevel level, CydonianSentinelEntity sentinel, Runnable body) {
		try {
			body.run();
		} finally {
			sentinel.reset(level);
			sentinel.discard();
		}
	}

	/**
	 * It waits on its plinth, its core dark, unhurt by anything, and a creative player's blow does not wake it; a player
	 * who may fight it standing on the crater's floor does: its core flares, it is unhurt while it wakes, and
	 * {@value CydonianSentinelEntity#WAKE_TICKS} ticks later it has stepped down to the crater's floor at full health, its
	 * core lit, its shell whole.
	 */
	@GameTest(structure = ARENA, padding = 8, maxTicks = 160)
	public void itWaitsOnItsPlinthAndAPlayerInTheCraterWakesIt(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		BlockPos origin = origin(helper);
		crater(level, origin);
		CydonianSentinelEntity sentinel = waiting(helper, origin);
		ServerPlayer player = player(helper, origin, 36.5, 40.5);
		player.setGameMode(GameType.CREATIVE);
		helper.runAfterDelay(20, () -> {
			boolean waited = sentinel.phase() == CydonianSentinelEntity.Phase.WAITING && sentinel.position().distanceTo(sentinel.waiting()) < 0.01
					&& sentinel.action() == CydonianSentinelEntity.Action.STATUE && sentinel.glow() == CydonianSentinelEntity.Glow.DARK;
			boolean harmless = !sentinel.hurtServer(level, level.damageSources().playerAttack(player), 10.0F)
					&& sentinel.getHealth() == sentinel.getMaxHealth();
			boolean slept = sentinel.phase() == CydonianSentinelEntity.Phase.WAITING;
			player.setGameMode(GameType.SURVIVAL);
			helper.runAfterDelay(12, () -> {
				boolean woken = sentinel.phase() == CydonianSentinelEntity.Phase.WAKING && sentinel.getHealth() == sentinel.getMaxHealth()
						&& sentinel.glow() == CydonianSentinelEntity.Glow.FLARE;
				boolean unhurt = !sentinel.hurtServer(level, level.damageSources().playerAttack(player), 10.0F);
				// Woken, it has no foe to fight: it only walks.
				player.setGameMode(GameType.CREATIVE);
				helper.runAfterDelay(CydonianSentinelEntity.WAKE_TICKS + 15, () -> {
					ending(level, sentinel, () -> {
						Vec3 woke = sentinel.woke();
						helper.assertTrue(waited, "It did not wait on its plinth, its core dark");
						helper.assertTrue(harmless, "Waiting, it was harmed");
						helper.assertTrue(slept, "A creative player's blow woke it");
						helper.assertTrue(woken, "A player on the crater's floor did not wake it at full health, its core flaring");
						helper.assertTrue(unhurt, "Waking, it was harmed");
						helper.assertTrue(sentinel.getMaxHealth() == (float) (CydonianSentinelEntity.HEALTH * CydonianSentinelEntity.partyScale(1)),
								"Woken by one player, its health is " + sentinel.getMaxHealth());
						helper.assertTrue(sentinel.phase() == CydonianSentinelEntity.Phase.SENTINEL, "It did not step down to fight: " + sentinel.phase());
						// Down for up to 25 ticks by now, it has strided toward the crater's middle at most 3 blocks.
						helper.assertTrue(Math.abs(sentinel.getY() - sentinel.floorY()) < 0.1 && Math.hypot(sentinel.getX() - woke.x, sentinel.getZ() - woke.z) < 6.0,
								"It did not step down to the crater's floor where it wakes but to " + sentinel.position().subtract(Vec3.atLowerCornerOf(origin))
										+ " in its Sanctum (the floor at y " + CydonianSanctum.STAND + ")");
						helper.assertTrue(sentinel.glow() == CydonianSentinelEntity.Glow.LIT && sentinel.form() == CydonianSentinelEntity.Form.SHELL
								&& !sentinel.platesOpen(), "Stepped down, its core is not lit in its shut shell: " + sentinel.glow());
						player.discard();
					});
					helper.succeed();
				});
			});
		});
	}

	/**
	 * Its health grows by half for each player after the first, at most two and a half times; a player's blow from afar
	 * wakes it, unhurt; Wither and poison never take hold of it. The Red Giant makes every cooldown a third shorter and its
	 * storms come oftener, and a blow hurts it and counts as taking part.
	 */
	@GameTest(structure = ARENA, padding = 8)
	public void itsHealthGrowsWithThePartyAndTheRedGiantHastensIt(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		BlockPos origin = origin(helper);
		CydonianSentinelEntity sentinel = waiting(helper, origin);
		ServerPlayer far = player(helper, origin, 36.5, 66.5);
		ServerPlayer other = player(helper, origin, 38.5, 66.5);
		ending(level, sentinel, () -> {
			helper.assertTrue(CydonianSentinelEntity.partyScale(1) == 1.0 && CydonianSentinelEntity.partyScale(2) == 1.5
					&& CydonianSentinelEntity.partyScale(4) == 2.5 && CydonianSentinelEntity.partyScale(9) == 2.5, "Its health scales with the party, at most 2.5 times");
			helper.assertTrue(!sentinel.canBeAffected(new MobEffectInstance(MobEffects.WITHER, 100))
					&& !sentinel.canBeAffected(new MobEffectInstance(MobEffects.POISON, 100)), "Wither or poison can take hold of it");
			helper.assertTrue(sentinel.cooldown(240) == 240 && sentinel.stormEvery() == CydonianSentinelEntity.STORM_EVERY,
					"Before the Red Giant its cooldowns and storms are their own");
			boolean blow = sentinel.hurtServer(level, level.damageSources().playerAttack(far), 10.0F);
			helper.assertTrue(!blow && sentinel.phase() == CydonianSentinelEntity.Phase.WAKING, "A player's blow from afar did not wake it, unhurt");
			helper.assertTrue(sentinel.getMaxHealth() == (float) (CydonianSentinelEntity.HEALTH * CydonianSentinelEntity.partyScale(2)),
					"Woken with two players by its crater, its health is " + sentinel.getMaxHealth());
			sentinel.redGiant(level);
			helper.assertTrue(sentinel.phase() == CydonianSentinelEntity.Phase.RED_GIANT && sentinel.glow() == CydonianSentinelEntity.Glow.GIANT,
					"The Red Giant did not burn white-hot: " + sentinel.glow());
			helper.assertTrue(sentinel.cooldown(240) == 160 && sentinel.cooldown(CydonianSentinelEntity.GLOBAL_COOLDOWN) == 13,
					"In the Red Giant its cooldowns are not a third shorter: " + sentinel.cooldown(240));
			helper.assertTrue(sentinel.stormEvery() == CydonianSentinelEntity.STORM_EVERY_GIANT, "In the Red Giant its storms come every "
					+ sentinel.stormEvery() + " ticks");
			float before = sentinel.getHealth();
			boolean hurt = sentinel.hurtServer(level, level.damageSources().playerAttack(far), 10.0F);
			helper.assertTrue(hurt && sentinel.getHealth() < before, "In the Red Giant a blow did not hurt it");
			helper.assertTrue(sentinel.participants(level).contains(far) && !sentinel.participants(level).contains(other),
					"Hurting it did not count as taking part, or standing by did");
			far.discard();
			other.discard();
		});
		helper.succeed();
	}

	/**
	 * Its weak point: as it charges its Core Lance its chest plates open and its armour does not count, and a blow on its
	 * core does double harm; a light blow does not stagger it, but one of {@value CydonianSentinelEntity#STAGGER_DAMAGE}
	 * or more in the wind-up does: the lance fails and it reels, its plates open, for
	 * {@value CydonianSentinelEntity#STAGGER_TICKS} ticks; then its plates swing shut and its armour is whole again.
	 */
	@GameTest(structure = ARENA, padding = 8, maxTicks = 220)
	public void itsCoreLanceOpensItsPlatesAndAStrongBlowStaggersIt(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		BlockPos origin = origin(helper);
		crater(level, origin);
		CydonianSentinelEntity sentinel = waiting(helper, origin);
		ServerPlayer foe = player(helper, origin, 36.5, 36.5);
		foe.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, 400, 4, false, false));
		sentinel.wake(level);
		helper.runAfterDelay(AWAKE, () -> {
			sentinel.begin(level, Attack.CORE_LANCE, foe);
			boolean opened = sentinel.platesOpen() && sentinel.glow() == CydonianSentinelEntity.Glow.OPEN
					&& sentinel.getAttributeValue(Attributes.ARMOR) == 0.0 && sentinel.action() == CydonianSentinelEntity.Action.LANCE_WINDUP;
			float before = sentinel.getHealth();
			boolean light = sentinel.hurtServer(level, level.damageSources().playerAttack(foe), 2.0F)
					&& Math.abs(before - sentinel.getHealth() - 2.0F * CydonianSentinelEntity.CORE_MULTIPLIER) < 1.0E-3F
					&& sentinel.staggerTicks() == 0 && sentinel.attack() == Attack.CORE_LANCE;
			helper.runAfterDelay(HURT_COOLDOWN, () -> {
				boolean windingUp = sentinel.attack() == Attack.CORE_LANCE && sentinel.attackTicks() < Attack.CORE_LANCE.windup;
				float mid = sentinel.getHealth();
				boolean strong = sentinel.hurtServer(level, level.damageSources().playerAttack(foe), CydonianSentinelEntity.STAGGER_DAMAGE)
						&& Math.abs(mid - sentinel.getHealth() - CydonianSentinelEntity.STAGGER_DAMAGE * CydonianSentinelEntity.CORE_MULTIPLIER) < 1.0E-3F;
				boolean staggered = sentinel.staggerTicks() == CydonianSentinelEntity.STAGGER_TICKS && sentinel.attack() == null
						&& sentinel.action() == CydonianSentinelEntity.Action.STAGGER && sentinel.platesOpen();
				helper.runAfterDelay(CydonianSentinelEntity.STAGGER_TICKS + 5, () -> {
					ending(level, sentinel, () -> {
						helper.assertTrue(opened, "Charging its Core Lance, its plates did not open, its armour gone");
						helper.assertTrue(light, "A light blow on its open core did not do double harm, or staggered it");
						helper.assertTrue(windingUp, "Its lance's wind-up ended too soon: " + sentinel.attackTicks());
						helper.assertTrue(strong, "A strong blow on its open core did not do double harm");
						helper.assertTrue(staggered, "A strong blow in the wind-up did not stagger it: " + sentinel.staggerTicks());
						helper.assertTrue(sentinel.staggerTicks() == 0 && !sentinel.platesOpen()
								&& sentinel.getAttributeValue(Attributes.ARMOR) == CydonianSentinelEntity.ARMOR
								&& sentinel.glow() == CydonianSentinelEntity.Glow.LIT, "After its stagger its plates did not shut, its armour whole");
						foe.discard();
					});
					helper.succeed();
				});
			});
		});
	}

	/**
	 * A Core Lance broken off before its end (its storm called, or another attack begun) shuts its plates, and its armour
	 * counts again, as the lance's own end would: its core is open only while it charges and fires.
	 */
	@GameTest(structure = ARENA, padding = 8, maxTicks = 160)
	public void aBrokenOffLanceShutsItsPlates(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		BlockPos origin = origin(helper);
		crater(level, origin);
		CydonianSentinelEntity sentinel = waiting(helper, origin);
		ServerPlayer foe = player(helper, origin, 36.5, 36.5);
		foe.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, 400, 4, false, false));
		sentinel.wake(level);
		helper.runAfterDelay(AWAKE, () -> {
			ending(level, sentinel, () -> {
				sentinel.begin(level, Attack.CORE_LANCE, foe);
				helper.assertTrue(sentinel.platesOpen(), "Charging its Core Lance, its plates did not open");
				sentinel.callStorm(level, Wind.NORTH);
				helper.assertTrue(!sentinel.platesOpen() && sentinel.attack() == null
						&& sentinel.getAttributeValue(Attributes.ARMOR) == CydonianSentinelEntity.ARMOR,
						"Its storm called mid-lance, its plates stayed open or its armour gone");
				sentinel.calm(level);
				sentinel.begin(level, Attack.CORE_LANCE, foe);
				sentinel.begin(level, Attack.HAMMERFALL, foe);
				helper.assertTrue(!sentinel.platesOpen() && sentinel.attack() == Attack.HAMMERFALL
						&& sentinel.getAttributeValue(Attributes.ARMOR) == CydonianSentinelEntity.ARMOR,
						"Another attack begun mid-lance, its plates stayed open or its armour gone");
				foe.discard();
			});
			helper.succeed();
		});
	}

	/** Damage must not add vanilla's random knockback on top of the storm's announced wind. */
	@GameTest(structure = ARENA, padding = 8, maxTicks = 120)
	public void stormDamageOnlyAddsTheDeclaredWindImpulse(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		BlockPos origin = origin(helper);
		crater(level, origin);
		CydonianSentinelEntity sentinel = waiting(helper, origin);
		ServerPlayer open = player(helper, origin, 36.5, 50.5);
		open.setGameMode(GameType.CREATIVE);
		helper.runAfterDelay(LOADING_TICKS, () -> {
			ending(level, sentinel, () -> {
				open.setGameMode(GameType.SURVIVAL);
				for (Wind wind : Wind.values()) {
					sentinel.callStorm(level, wind);
					for (int seed = 0; seed < 12; seed++) {
						Vec3 before = seed % 2 == 0 ? Vec3.ZERO : new Vec3(-0.12, 0.07, 0.09);
						open.setDeltaMovement(before);
						open.setHealth(open.getMaxHealth());
						open.damageCooldownTime = 0;
						open.getRandom().setSeed(seed);
						int hit = sentinel.scour(level);
						Vec3 expected = before.add(wind.blows().scale(CydonianSentinelEntity.STORM_PUSH));
						helper.assertTrue(hit == 1 && hurt(open), "Storm damage missing for " + wind + ", seed " + seed);
						helper.assertTrue(open.getDeltaMovement().distanceTo(expected) < 0.000001,
								"Storm added random knockback for " + wind + ", seed " + seed + ": "
								+ open.getDeltaMovement() + " instead of " + expected);
					}
				}
				open.discard();
			});
			helper.succeed();
		});
	}

	/**
	 * Its rule, the Red Storm. Rock within {@value CydonianSentinelEntity#LEE} blocks upwind shelters: a player beside a
	 * standing stone is sheltered from the one wind the stone stands against, and from no other. Called from the west, it
	 * plants its arms for {@value CydonianSentinelEntity#STORM_TELL} ticks; then the storm blows, and its scouring hurts a
	 * player in the open and pushes them downwind, and spares one in the stone's lee; after
	 * {@value CydonianSentinelEntity#STORM_TICKS} ticks it passes.
	 */
	@GameTest(structure = ARENA, padding = 8, maxTicks = 320)
	public void theRedStormScoursTheUnshelteredAndAStoneShelters(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		BlockPos origin = origin(helper);
		crater(level, origin);
		stone(level, origin, 0);  // cells 43 to 45 by 37 to 39
		CydonianSentinelEntity sentinel = waiting(helper, origin);
		// Its foe, kept from harm, in the stone's lee from the west: it fights them, and the storm spares them.
		ServerPlayer east = player(helper, origin, 47.5, 38.5);
		east.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, 600, 4, false, false));
		ServerPlayer west = player(helper, origin, 40.5, 38.5);
		ServerPlayer north = player(helper, origin, 44.5, 34.5);
		ServerPlayer south = player(helper, origin, 44.5, 42.5);
		ServerPlayer open = player(helper, origin, 36.5, 50.5);
		List<ServerPlayer> others = List.of(west, north, south, open);
		others.forEach(player -> player.setGameMode(GameType.CREATIVE));
		sentinel.wake(level);
		helper.runAfterDelay(LOADING_TICKS, () -> {
			// Who is sheltered from which wind: each beside the stone from the wind it stands against.
			List<String> wrong = new ArrayList<>();
			for (Wind wind : Wind.values()) {
				for (ServerPlayer player : List.of(east, west, north, south, open)) {
					boolean expected = player == east && wind == Wind.WEST || player == west && wind == Wind.EAST
							|| player == north && wind == Wind.SOUTH || player == south && wind == Wind.NORTH;
					if (sentinel.sheltered(level, player, wind) != expected) {
						wrong.add(wind + " at " + player.position());
					}
				}
			}
			sentinel.callStorm(level, Wind.WEST);
			boolean told = sentinel.storm() == CydonianSentinelEntity.Storm.TELL && sentinel.wind() == Wind.WEST
					&& sentinel.action() == CydonianSentinelEntity.Action.PLANT;
			helper.runAfterDelay(CydonianSentinelEntity.STORM_TELL + 2, () -> {
				boolean blowing = sentinel.storm() == CydonianSentinelEntity.Storm.BLOWING;
				open.setGameMode(GameType.SURVIVAL);
				int hit = sentinel.scour(level);
				boolean scoured = hit == 1 && hurt(open) && open.getDeltaMovement().x > CydonianSentinelEntity.STORM_PUSH - 0.05 && !hurt(east);
				String scourEvidence = "hits=" + hit + ", open health=" + open.getHealth() + "/" + open.getMaxHealth()
						+ ", open motion=" + open.getDeltaMovement() + ", sheltered health=" + east.getHealth() + "/" + east.getMaxHealth();
				open.setGameMode(GameType.CREATIVE);
				helper.runAfterDelay(CydonianSentinelEntity.STORM_TICKS, () -> {
					ending(level, sentinel, () -> {
						helper.assertTrue(wrong.isEmpty(), "Sheltered wrongly from: " + wrong);
						helper.assertTrue(told, "Called from the west, it did not plant its arms: " + sentinel.storm() + ", " + sentinel.action());
						helper.assertTrue(blowing, "After its tell the storm did not blow");
						helper.assertTrue(scoured, "The storm must hurt and push the open player downwind, sparing the sheltered player: " + scourEvidence);
						helper.assertTrue(sentinel.storm() == CydonianSentinelEntity.Storm.CALM && sentinel.phase() == CydonianSentinelEntity.Phase.SENTINEL,
								"After " + CydonianSentinelEntity.STORM_TICKS + " ticks the storm did not pass: " + sentinel.storm());
						List.of(east, west, north, south, open).forEach(ServerPlayer::discard);
					});
					helper.succeed();
				});
			});
		});
	}

	/**
	 * Hammerfall: its fists slam the floor {@value CydonianSentinelEntity#HAMMER_AHEAD} blocks before it, striking
	 * whoever is within {@value CydonianSentinelEntity#HAMMER_REACH} of where they land and throwing them back; whoever is
	 * within {@value CydonianSentinelEntity#HAMMER_KNOCK} is thrown back unhurt, and anyone farther is untouched.
	 */
	@GameTest(structure = ARENA, padding = 8, maxTicks = 140)
	public void itsHammerfallStrikesBeforeItAndThrowsBack(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		BlockPos origin = origin(helper);
		crater(level, origin);
		CydonianSentinelEntity sentinel = waiting(helper, origin);
		// It stands at (36.5, 40.5) facing south: its fists land at (36.5, 42.5).
		ServerPlayer front = player(helper, origin, 36.5, 44.5);
		ServerPlayer aside = player(helper, origin, 39.0, 42.5);
		ServerPlayer thrown = player(helper, origin, 36.5, 47.0);
		ServerPlayer far = player(helper, origin, 36.5, 50.0);
		List<ServerPlayer> players = List.of(front, aside, thrown, far);
		players.forEach(player -> player.setGameMode(GameType.CREATIVE));
		helper.runAfterDelay(LOADING_TICKS, () -> {
			players.forEach(player -> player.setGameMode(GameType.SURVIVAL));
			ending(level, sentinel, () -> {
				stand(sentinel, 36.5, 40.5);
				int hit = sentinel.hammer(level);
				helper.assertTrue(hit == 2 && hurt(front) && hurt(aside), "The Hammerfall struck " + hit + " players, not the 2 by its fists");
				helper.assertTrue(front.getDeltaMovement().z > 1.0, "The player before it was not thrown back: " + front.getDeltaMovement());
				helper.assertTrue(!hurt(thrown) && thrown.getDeltaMovement().z > 0.3, "The player farther off was struck, or not thrown back: "
						+ thrown.getDeltaMovement());
				helper.assertTrue(!hurt(far) && far.getDeltaMovement().lengthSqr() < 1.0E-6, "A player out of its reach was touched");
				players.forEach(ServerPlayer::discard);
			});
			helper.succeed();
		});
	}

	/**
	 * Shard Volley: {@value CydonianSentinelEntity#VOLLEY_SHARDS} Rust Shards fan out at its foe, and the middle one
	 * strikes them; a player well aside is untouched.
	 */
	@GameTest(structure = ARENA, padding = 8, maxTicks = 140)
	public void itsShardVolleyFansOutAtItsFoe(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		BlockPos origin = origin(helper);
		crater(level, origin);
		CydonianSentinelEntity sentinel = waiting(helper, origin);
		ServerPlayer mark = player(helper, origin, 36.5, 48.5);
		ServerPlayer aside = player(helper, origin, 30.5, 48.5);
		List<ServerPlayer> players = List.of(mark, aside);
		players.forEach(player -> player.setGameMode(GameType.CREATIVE));
		helper.runAfterDelay(LOADING_TICKS, () -> {
			players.forEach(player -> player.setGameMode(GameType.SURVIVAL));
			stand(sentinel, 36.5, 40.5);
			int loosed = sentinel.volley(level, mark);
			List<RustShardEntity> shards = level.getEntitiesOfClass(RustShardEntity.class, crater(origin));
			boolean fanned = loosed == CydonianSentinelEntity.VOLLEY_SHARDS && shards.size() == CydonianSentinelEntity.VOLLEY_SHARDS
					&& shards.stream().allMatch(shard -> shard.damage() == CydonianSentinelEntity.VOLLEY_DAMAGE);
			helper.runAfterDelay(20, () -> {
				ending(level, sentinel, () -> {
					helper.assertTrue(fanned, "Its volley loosed " + shards.size() + " shards, not " + CydonianSentinelEntity.VOLLEY_SHARDS);
					helper.assertTrue(hurt(mark), "No shard of its volley struck its foe");
					helper.assertTrue(!hurt(aside), "A shard struck a player well aside");
					players.forEach(ServerPlayer::discard);
				});
				helper.succeed();
			});
		});
	}

	/**
	 * Quake Line: the spikes burst up along the line from its feet, striking and tossing up a player on it, and sparing one
	 * beside it and one past its end. In the fight, as the wind-up ends a spike bursts up every
	 * {@value CydonianSentinelEntity#QUAKE_STEP} blocks down the line, and once they have stood and sunk they are gone.
	 */
	@GameTest(structure = ARENA, padding = 8, maxTicks = 220)
	public void itsQuakeLineBurstsUpAlongTheLine(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		BlockPos origin = origin(helper);
		crater(level, origin);
		CydonianSentinelEntity sentinel = waiting(helper, origin);
		ServerPlayer foe = player(helper, origin, 36.5, 30.5);
		foe.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, 600, 4, false, false));
		ServerPlayer on = player(helper, origin, 36.5, 50.5);
		ServerPlayer beside = player(helper, origin, 39.5, 46.5);
		ServerPlayer beyond = player(helper, origin, 36.5, 57.5);
		List<ServerPlayer> players = List.of(on, beside, beyond);
		players.forEach(player -> player.setGameMode(GameType.CREATIVE));
		sentinel.wake(level);
		Attack quake = Attack.QUAKE_LINE;
		helper.runAfterDelay(LOADING_TICKS, () -> {
			players.forEach(player -> player.setGameMode(GameType.SURVIVAL));
			sentinel.quakeLine(at(origin, 36.5, 40.5), new Vec3(0.0, 0.0, 1.0));
			int hit = sentinel.quakeStrike(level);
			boolean struck = hit == 1 && hurt(on) && Math.abs(on.getDeltaMovement().y - CydonianSentinelEntity.QUAKE_TOSS) < 1.0E-6
					&& !hurt(beside) && !hurt(beyond);
			players.forEach(player -> player.setGameMode(GameType.CREATIVE));
			sentinel.begin(level, quake, foe);
			int spikes = (int) Math.floor(CydonianSentinelEntity.QUAKE_LENGTH / CydonianSentinelEntity.QUAKE_STEP);
			helper.runAfterDelay(quake.windup + 2, () -> {
				int burst = level.getEntitiesOfClass(QuakeSpikeEntity.class, crater(origin)).size();
				helper.runAfterDelay(quake.active + QuakeSpikeEntity.SINK + 5, () -> {
					ending(level, sentinel, () -> {
						helper.assertTrue(struck, "The spikes struck " + hit + " players, not the one on the line (tossed up)");
						helper.assertTrue(burst == spikes, burst + " spikes burst up down the line, not " + spikes);
						helper.assertTrue(level.getEntitiesOfClass(QuakeSpikeEntity.class, crater(origin)).isEmpty(), "Its spikes outlived their sinking");
						List.of(foe, on, beside, beyond).forEach(ServerPlayer::discard);
					});
					helper.succeed();
				});
			});
		});
	}

	/**
	 * Its lances burn along their aim: a player in the beam is burned, one aside is not, and a standing stone stops the
	 * beam, sparing whoever is behind it.
	 */
	@GameTest(structure = ARENA, padding = 8, maxTicks = 140)
	public void itsLanceBurnsAlongItsAimAndRockStopsIt(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		BlockPos origin = origin(helper);
		crater(level, origin);
		stone(level, origin, 0);  // cells 43 to 45 by 37 to 39
		CydonianSentinelEntity sentinel = waiting(helper, origin);
		ServerPlayer on = player(helper, origin, 41.5, 38.5);
		ServerPlayer behind = player(helper, origin, 47.5, 38.5);
		ServerPlayer aside = player(helper, origin, 36.5, 44.5);
		List<ServerPlayer> players = List.of(on, behind, aside);
		players.forEach(player -> player.setGameMode(GameType.CREATIVE));
		helper.runAfterDelay(LOADING_TICKS, () -> {
			players.forEach(player -> player.setGameMode(GameType.SURVIVAL));
			ending(level, sentinel, () -> {
				stand(sentinel, 36.5, 38.5);
				Vec3 core = sentinel.core();
				Vec3 way = behind.getBoundingBox().getCenter().subtract(core).normalize();
				double reach = sentinel.reach(level, core, way);
				int hit = sentinel.beam(level, core, way);
				helper.assertTrue(reach < core.distanceTo(behind.getBoundingBox().getCenter()) - 1.0, "The stone did not stop its beam: it reached " + reach);
				helper.assertTrue(hit == 1 && hurt(on), "Its beam burned " + hit + " players, not the one in it");
				helper.assertTrue(!hurt(behind), "Its beam burned through the stone");
				helper.assertTrue(!hurt(aside), "Its beam burned a player aside");
				players.forEach(ServerPlayer::discard);
			});
			helper.succeed();
		});
	}

	/**
	 * At {@value CydonianSentinelEntity#CRACK_AT} of its health the Shell Cracks: it roars, unhurt, and
	 * {@value CydonianSentinelEntity#SENTRIES_RISE} Shard Sentries rise from the small craters; then the Red Storm. Never
	 * more than {@value CydonianSentinelEntity#SENTRY_CAP} float at once. A sentry is unhurt while it rises; afterwards a
	 * blow on it counts as taking part, and when the Sentinel strides back to its plinth its sentries crumble.
	 */
	@GameTest(structure = ARENA, padding = 8, maxTicks = 240)
	public void atSixTenthsTheShellCracksAndItsSentriesRise(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		BlockPos origin = origin(helper);
		crater(level, origin);
		CydonianSentinelEntity sentinel = waiting(helper, origin);
		ServerPlayer foe = player(helper, origin, 36.5, 36.5);
		foe.setGameMode(GameType.CREATIVE);
		sentinel.wake(level);
		helper.runAfterDelay(AWAKE, () -> {
			sentinel.setHealth(sentinel.getMaxHealth() * CydonianSentinelEntity.CRACK_AT - 1.0F);
			helper.runAfterDelay(2, () -> {
				boolean cracked = sentinel.phase() == CydonianSentinelEntity.Phase.CRACKING && sentinel.action() == CydonianSentinelEntity.Action.ROAR
						&& !sentinel.hurtServer(level, level.damageSources().playerAttack(foe), 10.0F);
				List<ShardSentryEntity> risen = sentries(level, sentinel);
				List<Integer> pits = sentinel.pits(sentinel.centre(), CydonianSentinelEntity.SENTRIES_RISE);
				boolean fromPits = risen.size() == CydonianSentinelEntity.SENTRIES_RISE && risen.stream().allMatch(sentry -> pits.stream()
						.anyMatch(pit -> Math.hypot(sentry.getX() - sentinel.pit(pit).x, sentry.getZ() - sentinel.pit(pit).z) < 0.01));
				boolean rising = !risen.isEmpty() && risen.stream().allMatch(ShardSentryEntity::rising)
						&& !risen.getFirst().hurtServer(level, level.damageSources().playerAttack(foe), 4.0F);
				sentinel.raise(level, sentinel.centre().add(4.0, 0.0, 0.0));
				sentinel.raise(level, sentinel.centre().add(-4.0, 0.0, 0.0));
				boolean capped = sentinel.sentries().size() == CydonianSentinelEntity.SENTRY_CAP
						&& sentinel.raise(level, sentinel.centre().add(0.0, 0.0, 4.0)) == null;
				helper.runAfterDelay(Math.max(CydonianSentinelEntity.CRACK_TICKS, ShardSentryEntity.RISE) + 5, () -> {
					ShardSentryEntity struck = risen.isEmpty() ? null : risen.getFirst();
					boolean out = struck != null && !struck.rising() && struck.hurtServer(level, level.damageSources().playerAttack(foe), 4.0F);
					boolean part = sentinel.participants(level).contains(foe);
					boolean storming = sentinel.phase() == CydonianSentinelEntity.Phase.RED_STORM;
					List<ShardSentryEntity> floating = sentries(level, sentinel);
					sentinel.reset(level);
					ending(level, sentinel, () -> {
						helper.assertTrue(cracked, "At six tenths its shell did not crack, unhurt: " + sentinel.phase());
						helper.assertTrue(fromPits, risen.size() + " sentries rose, not two over the small craters " + pits);
						helper.assertTrue(rising, "A sentry was hurt while it rose");
						helper.assertTrue(capped, "More than " + CydonianSentinelEntity.SENTRY_CAP + " sentries float at once: " + sentinel.sentries().size());
						helper.assertTrue(out, "Risen, a sentry was not hurt by a blow");
						helper.assertTrue(part, "Striking a sentry did not count as taking part");
						helper.assertTrue(storming, "After the Shell Cracks the Red Storm did not begin");
						helper.assertTrue(floating.size() == CydonianSentinelEntity.SENTRY_CAP && floating.stream().allMatch(ShardSentryEntity::isRemoved)
								&& sentinel.sentries().isEmpty(), "Its sentries did not crumble as it strode back to its plinth");
						foe.discard();
					});
					helper.succeed();
				});
			});
		});
	}

	/**
	 * The Unbound Core: its shell bursts apart over {@value CydonianSentinelEntity#BURST_TICKS} ticks, unhurt, and the core
	 * floats free over the floor, small, its armour {@value CydonianSentinelEntity#CORE_ARMOR}. The Starfall's meteors
	 * strike their marks, hurting a player by one and sparing one afar; in the fight it marks the floor by each player in
	 * its crater, its meteors fall onto the marks, and then it sinks, dimmed, for the attack's recovery, and rises again.
	 * In the Red Giant it marks two places by each player.
	 */
	@GameTest(structure = ARENA, padding = 8, maxTicks = 360)
	public void unboundItsCoreFloatsFreeAndTheStarfallSinksIt(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		BlockPos origin = origin(helper);
		crater(level, origin);
		CydonianSentinelEntity sentinel = waiting(helper, origin);
		ServerPlayer foe = player(helper, origin, 36.5, 40.5);
		foe.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, 1000, 4, false, false));
		ServerPlayer near = player(helper, origin, 30.5, 50.5);
		ServerPlayer far = player(helper, origin, 44.5, 50.5);
		List<ServerPlayer> others = List.of(near, far);
		others.forEach(player -> player.setGameMode(GameType.CREATIVE));
		sentinel.wake(level);
		Attack starfall = Attack.STARFALL;
		helper.runAfterDelay(AWAKE, () -> {
			sentinel.burst(level);
			boolean bursting = sentinel.phase() == CydonianSentinelEntity.Phase.BURSTING
					&& !sentinel.hurtServer(level, level.damageSources().playerAttack(foe), 10.0F);
			helper.runAfterDelay(CydonianSentinelEntity.BURST_TICKS + 20, () -> {
				boolean unbound = sentinel.phase() == CydonianSentinelEntity.Phase.UNBOUND && sentinel.form() == CydonianSentinelEntity.Form.CORE
						&& sentinel.getBbHeight() == 1.5F && sentinel.getAttributeValue(Attributes.ARMOR) == CydonianSentinelEntity.CORE_ARMOR
						&& sentinel.glow() == CydonianSentinelEntity.Glow.UNBOUND;
				double hover = sentinel.getY() - sentinel.floorY();
				others.forEach(player -> player.setGameMode(GameType.SURVIVAL));
				sentinel.marks(List.of(at(origin, 30.5, 51.5)));
				int struck = sentinel.starfallStrike(level);
				boolean meteor = struck == 1 && hurt(near) && !hurt(far);
				others.forEach(player -> player.setGameMode(GameType.CREATIVE));
				sentinel.begin(level, starfall, foe);
				List<Vec3> marks = sentinel.marks();
				boolean marked = marks.size() == CydonianSentinelEntity.STARFALL_MARKS && marks.stream()
						.allMatch(mark -> Math.hypot(mark.x - foe.getX(), mark.z - foe.getZ()) <= CydonianSentinelEntity.MARK_SPREAD + 1.0E-6);
				helper.runAfterDelay(starfall.windup - CydonianSentinelEntity.METEOR_FALL + 2, () -> {
					int falling = level.getEntitiesOfClass(SentinelMeteorEntity.class, crater(origin)).size();
					helper.runAfterDelay(CydonianSentinelEntity.METEOR_FALL, () -> {
						boolean sank = sentinel.sunk() && sentinel.glow() == CydonianSentinelEntity.Glow.DIM
								&& sentinel.action() == CydonianSentinelEntity.Action.SUNK;
						helper.runAfterDelay(starfall.recovery + 5, () -> {
							boolean rose = !sentinel.sunk() && sentinel.glow() == CydonianSentinelEntity.Glow.UNBOUND;
							sentinel.redGiant(level);
							sentinel.markStarfall(level);
							int giantMarks = sentinel.marks().size();
							ending(level, sentinel, () -> {
								helper.assertTrue(bursting, "As its shell burst it was hurt, or did not burst");
								helper.assertTrue(unbound, "Unbound, its core is not free, small and lightly armoured: " + sentinel.form() + ", "
										+ sentinel.getBbHeight() + ", " + sentinel.getAttributeValue(Attributes.ARMOR));
								helper.assertTrue(Math.abs(hover - CydonianSentinelEntity.CORE_HOVER) < 0.3, "Unbound, it floats " + hover + " over the floor");
								helper.assertTrue(meteor, "The meteors struck " + struck + " players, not the one by the mark");
								helper.assertTrue(marked, "Its Starfall marked " + marks + ", not one place by its foe");
								helper.assertTrue(falling == CydonianSentinelEntity.STARFALL_MARKS, falling + " meteors fell, not one a mark");
								helper.assertTrue(sank, "After its Starfall it did not sink, dimmed");
								helper.assertTrue(rose, "After the Starfall's recovery it did not rise again, lit");
								helper.assertTrue(giantMarks == CydonianSentinelEntity.GIANT_MARKS, "In the Red Giant it marked " + giantMarks + " places by its foe");
								List.of(foe, near, far).forEach(ServerPlayer::discard);
							});
							helper.succeed();
						});
					});
				});
			});
		});
	}

	/** Strayed farther than {@value CydonianSentinelEntity#LEASH} blocks from its crater's middle, it strides back. */
	@GameTest(structure = ARENA, padding = 8, maxTicks = 100)
	public void itKeepsToItsCrater(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		BlockPos origin = origin(helper);
		crater(level, origin);
		CydonianSentinelEntity sentinel = waiting(helper, origin);
		sentinel.wake(level);
		helper.runAfterDelay(AWAKE, () -> {
			Vec3 centre = sentinel.centre();
			sentinel.teleportTo(centre.x + CydonianSentinelEntity.LEASH + 5.0, centre.y, centre.z);
			helper.runAfterDelay(2, () -> {
				ending(level, sentinel, () -> helper.assertTrue(sentinel.position().distanceTo(centre) <= CydonianSentinelEntity.LEASH,
						"Strayed from its crater, it did not stride back: " + sentinel.position()));
				helper.succeed();
			});
		});
	}

	/**
	 * Left alone {@value CydonianSentinelEntity#ABANDON_TICKS} ticks, even unbound, it strides back to its plinth and
	 * reforms, healed: its shell whole and its core dark, its sentries crumbled, and who hurt it forgotten.
	 */
	@GameTest(structure = ARENA, padding = 8, maxTicks = 400)
	public void leftAloneItStridesBackToItsPlinth(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		BlockPos origin = origin(helper);
		crater(level, origin);
		CydonianSentinelEntity sentinel = waiting(helper, origin);
		ServerPlayer gone = player(helper, origin, 36.5, 60.5);
		gone.setGameMode(GameType.CREATIVE);
		sentinel.wake(level);
		helper.runAfterDelay(AWAKE, () -> {
			sentinel.took(gone, 5.0F);
			sentinel.setHealth(sentinel.getMaxHealth() * 0.8F);
			ShardSentryEntity risen = sentinel.raise(level, sentinel.centre().add(4.0, 0.0, 4.0));
			sentinel.burst(level);
			boolean counted = sentinel.participants(level).contains(gone);
			helper.runAfterDelay(CydonianSentinelEntity.BURST_TICKS + CydonianSentinelEntity.ABANDON_TICKS + 20, () -> {
				ending(level, sentinel, () -> {
					helper.assertTrue(counted, "Whoever hurt it was not counted");
					helper.assertTrue(sentinel.phase() == CydonianSentinelEntity.Phase.WAITING && sentinel.position().distanceTo(sentinel.waiting()) < 0.1,
							"Left alone it did not stride back to its plinth: " + sentinel.phase() + " at " + sentinel.position());
					helper.assertTrue(sentinel.getHealth() == sentinel.getMaxHealth() && sentinel.glow() == CydonianSentinelEntity.Glow.DARK,
							"Back on its plinth it is not healed, or its core burns");
					helper.assertTrue(sentinel.form() == CydonianSentinelEntity.Form.SHELL && sentinel.getBbHeight() == 4.0F
							&& sentinel.getAttributeValue(Attributes.ARMOR) == CydonianSentinelEntity.ARMOR, "Back on its plinth its shell did not reform");
					helper.assertTrue(risen != null && risen.isRemoved() && sentinel.sentries().isEmpty(), "Its sentries outlived its striding back");
					helper.assertTrue(sentinel.participants(level).isEmpty(), "Who hurt it was not forgotten");
					gone.discard();
				});
				helper.succeed();
			});
		});
	}

	/**
	 * Its loot is each participant's own: everyone who struck it rolls for themselves (Cydonian Shards, and on a first kill
	 * always one of its trophies) into their own inventory with a share of its experience, and a bystander who took no part
	 * gets nothing. Afterwards Red Planet is theirs, and a later roll no longer counts as a first kill. As it falls its core
	 * gutters out and the Red Mist opens in the crater's middle: the way home.
	 */
	@GameTest(structure = ARENA, padding = 8, maxTicks = 60)
	public void itsLootIsEachParticipantsOwn(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		BlockPos origin = origin(helper);
		CydonianSentinelEntity sentinel = waiting(helper, origin);
		ServerPlayer first = player(helper, origin, 34.5, 40.5);
		ServerPlayer second = player(helper, origin, 38.5, 40.5);
		ServerPlayer bystander = player(helper, origin, 36.5, 52.5);
		sentinel.took(first, 10.0F);
		sentinel.took(second, 4.0F);
		ending(level, sentinel, () -> {
			List<ServerPlayer> participants = sentinel.participants(level);
			helper.assertTrue(participants.contains(first) && participants.contains(second), "Both who struck it take part");
			helper.assertFalse(participants.contains(bystander), "A bystander who took no part does");
			helper.assertTrue(SentinelLoot.firstKill(first), "A newcomer has already won Red Planet");
			List<ItemStack> loot = SentinelLoot.roll(level, sentinel, first);
			int shards = loot.stream().filter(stack -> stack.is(JugcraftSentinel.CYDONIAN_SHARD)).mapToInt(ItemStack::getCount).sum();
			helper.assertTrue(shards >= 4 && shards <= 8, shards + " Cydonian Shards, not 4 to 8");
			helper.assertTrue(loot.stream().filter(SentinelLoot::trophy).count() == 1, "A first kill always brings one of its trophies");
			sentinel.hurtServer(level, level.damageSources().genericKill(), Float.MAX_VALUE);
			helper.assertTrue(sentinel.isDeadOrDying() && sentinel.glow() == CydonianSentinelEntity.Glow.DIM, "It did not fall with its core guttering out");
			for (BlockPos cell : CydonianSanctum.craterMistCells(origin)) {
				helper.assertTrue(level.getBlockState(cell).is(JugcraftLairs.RED_MIST), "No Red Mist opened in the crater's middle at " + cell);
			}
			int share = CydonianSentinelEntity.EXPERIENCE / 2;
			for (ServerPlayer player : List.of(first, second)) {
				helper.assertTrue(player.getInventory().countItem(JugcraftSentinel.CYDONIAN_SHARD) >= 4, "A participant's loot did not go into their inventory");
				int trophies = player.getInventory().countItem(JugcraftSentinel.corelance()) + player.getInventory().countItem(JugcraftSentinel.redstarCleaver());
				helper.assertTrue(trophies == 1, "A first kill brought " + trophies + " of its trophies, not 1");
				helper.assertTrue(player.totalExperience >= share, "A participant got " + player.totalExperience + " experience, not " + share);
				helper.assertFalse(SentinelLoot.firstKill(player), "Red Planet is not theirs");
			}
			helper.assertTrue(bystander.getInventory().countItem(JugcraftSentinel.CYDONIAN_SHARD) == 0 && bystander.totalExperience == 0, "The bystander got loot");
			helper.assertTrue(SentinelLoot.firstKill(bystander), "The bystander won Red Planet");
			List.of(first, second, bystander).forEach(ServerPlayer::discard);
		});
		helper.succeed();
	}

	/**
	 * The Lowfall Core held in the offhand: the server keeps its holder falling slowly (Slow Falling, quietly, renewed on
	 * its tick); put away, it is not renewed.
	 */
	@GameTest(maxTicks = 120)
	public void theLowfallCoreKeepsItsHolderFallingSlowly(GameTestHelper helper) {
		ServerPlayer holder = helper.makeMockServerPlayerInLevel();
		holder.setGameMode(GameType.SURVIVAL);
		BlockPos at = helper.absolutePos(new BlockPos(1, 1, 1));
		holder.snapTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, 0.0F, 0.0F);
		boolean bare = !LowfallCore.holds(holder) && !holder.hasEffect(MobEffects.SLOW_FALLING);
		holder.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(JugcraftSentinel.LOWFALL_CORE));
		helper.runAfterDelay(LowfallCore.REFRESH + 2, () -> {
			MobEffectInstance slow = holder.getEffect(MobEffects.SLOW_FALLING);
			boolean falling = LowfallCore.holds(holder) && slow != null && slow.getAmplifier() == 0 && slow.isAmbient() && !slow.isVisible();
			holder.setItemInHand(InteractionHand.OFF_HAND, ItemStack.EMPTY);
			holder.removeEffect(MobEffects.SLOW_FALLING);
			helper.runAfterDelay(LowfallCore.REFRESH * 2 + 2, () -> {
				boolean lapsed = !LowfallCore.holds(holder) && !holder.hasEffect(MobEffects.SLOW_FALLING);
				helper.assertTrue(bare, "With nothing in the offhand, the core is held");
				helper.assertTrue(falling, "Held, the core did not keep its holder falling slowly, quietly: " + slow);
				helper.assertTrue(lapsed, "Put away, the core still renews Slow Falling");
				holder.discard();
				helper.succeed();
			});
		});
	}
}
