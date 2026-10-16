package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.lair.DischargeBreakerBlock;
import io.github.jimbozoomer.jugcraft.lair.FoundryTrench;
import io.github.jimbozoomer.jugcraft.lair.FoundryTrench.Pylon;
import io.github.jimbozoomer.jugcraft.lair.GroundTerminalBlock;
import io.github.jimbozoomer.jugcraft.lair.JugcraftLairs;
import io.github.jimbozoomer.jugcraft.lair.PylonCrownBlock;
import io.github.jimbozoomer.jugcraft.lair.dreadnought.GroundingStrap;
import io.github.jimbozoomer.jugcraft.lair.dreadnought.HotRivetEntity;
import io.github.jimbozoomer.jugcraft.lair.dreadnought.IronDreadnoughtEntity;
import io.github.jimbozoomer.jugcraft.lair.dreadnought.IronDreadnoughtEntity.Action;
import io.github.jimbozoomer.jugcraft.lair.dreadnought.IronDreadnoughtEntity.Attack;
import io.github.jimbozoomer.jugcraft.lair.dreadnought.IronDreadnoughtEntity.Phase;
import io.github.jimbozoomer.jugcraft.lair.dreadnought.IronDreadnoughtLoot;
import io.github.jimbozoomer.jugcraft.lair.dreadnought.JugcraftDreadnought;
import io.github.jimbozoomer.jugcraft.lair.dreadnought.MortarShellEntity;
import io.github.jimbozoomer.jugcraft.lair.dreadnought.SparkerEntity;
import java.util.List;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * The Iron Dreadnought (docs/features/iron-dreadnought.md), as far as a game-test server can show him: one put down on his
 * own (with no lair instance, over a trench laid out from near the corner of an arena the pit's size, so his gantry, his
 * pit and its pylons are all loaded; each test lays the floor and fixtures it needs) stands dormant in his gantry until a
 * player who may fight him stands on the pit's floor, then steps down onto it; his health grows with the party; his shut
 * plating turns most of every blow, his open vents none; his charge lights the pylon nearest his foe, and throwing its
 * breaker overloads him, while left unrouted it runs down the lit lane; at half health he tears a plate and fouls a
 * breaker, which three uses knock loose, and at a fifth Red Line jams his vents open; his Piston Punch, Rivet Fan, Trench
 * Charge (into a wall, he is stunned), Coil Arc, Mortar, Blowdown and Sparkers; the Grounding Strap halves electric harm;
 * he keeps to his pit; left alone he goes back to his gantry; and his loot is each participant's own. His fight in the
 * Foundry Trench itself runs in a real world, in {@link IronDreadnoughtClientGameTests}.
 *
 * <p>He scales for players within 42 blocks of the pit's middle and fights those in his pit and gantry; the arena keeps
 * every other test's players farther off than that. Each test sends him back to his gantry, with his Sparkers, rivets and
 * shells, and takes him away.
 */
public class IronDreadnoughtGameTests {
	/** An arena of air a little bigger than the trench's pit and gantry (76 x 76), the pit's middle in its middle. */
	private static final String PIT = "jugcraft-test:foundry_pit";
	/**
	 * A new player cannot be hurt until its client has loaded the world, or for 60 ticks; a mock player has no client, so
	 * a test that hurts one waits this long first.
	 */
	private static final int LOADING_TICKS = 80;
	/** A creature hurt shrugs off a blow no harder than the last for 10 ticks after (vanilla's); a test waits this long between blows. */
	private static final int HURT_COOLDOWN = 12;
	/** A tick or two past his waking, when he has stepped down onto the pit's floor. */
	private static final int AWAKE = IronDreadnoughtEntity.WAKE_TICKS + 5;
	/** How far over the pit's floor the ground round the pit is. */
	private static final double GROUND_UP = FoundryTrench.GROUND - FoundryTrench.STAND;
	/** The way each pylon's breaker faces (into the pit), in the order of {@link Pylon}. */
	private static final Direction[] BREAKER_FACES = {Direction.EAST, Direction.WEST, Direction.NORTH};

	/** The origin of a trench laid out from near the arena's corner: its pit's floor two blocks over the arena's. */
	private static BlockPos origin(GameTestHelper helper) {
		return helper.absolutePos(new BlockPos(3, -3, 2));
	}

	/** He stands dormant in the gantry of the trench laid out from {@code origin}. */
	private static IronDreadnoughtEntity waiting(GameTestHelper helper, BlockPos origin) {
		return IronDreadnoughtEntity.summon(helper.getLevel(), origin, null);
	}

	/** {@code state} in every cell of the template's box from (x0, y, z0) to (x1, y, z1). */
	private static void fill(ServerLevel level, BlockPos origin, int x0, int y, int z0, int x1, int z1, BlockState state) {
		for (int x = x0; x <= x1; x++) {
			for (int z = z0; z <= z1; z++) {
				level.setBlock(origin.offset(x, y, z), state, Block.UPDATE_CLIENTS);
			}
		}
	}

	/** The trench's floor and fixtures: the paving under the pit and the gantry, and each pylon's breaker, crown and terminal. */
	private static void trench(ServerLevel level, BlockPos origin) {
		BlockState paving = Blocks.STONE_BRICKS.defaultBlockState();
		fill(level, origin, FoundryTrench.PIT_WEST, FoundryTrench.PAVE, FoundryTrench.PIT_NORTH, FoundryTrench.PIT_EAST, FoundryTrench.PIT_SOUTH, paving);
		fill(level, origin, FoundryTrench.GANTRY_WEST, FoundryTrench.PAVE, FoundryTrench.GANTRY_NORTH, FoundryTrench.GANTRY_EAST,
				FoundryTrench.GANTRY_SOUTH, paving);
		for (Pylon pylon : Pylon.values()) {
			level.setBlock(pylon.breaker(origin), JugcraftLairs.DISCHARGE_BREAKER.defaultBlockState()
					.setValue(HorizontalDirectionalBlock.FACING, BREAKER_FACES[pylon.ordinal()]), Block.UPDATE_CLIENTS);
			level.setBlock(pylon.crown(origin), JugcraftLairs.PYLON_CROWN.defaultBlockState(), Block.UPDATE_CLIENTS);
			level.setBlock(pylon.terminal(origin), JugcraftLairs.GROUND_TERMINAL.defaultBlockState(), Block.UPDATE_CLIENTS);
		}
	}

	/** A survival player standing at the template's ({@code x}, {@code z}), {@code up} blocks over the pit's floor. */
	private static ServerPlayer player(GameTestHelper helper, BlockPos origin, double x, double up, double z) {
		ServerPlayer player = helper.makeMockServerPlayerInLevel();
		player.setGameMode(GameType.SURVIVAL);
		player.snapTo(origin.getX() + x, origin.getY() + FoundryTrench.STAND + up, origin.getZ() + z, 0.0F, 0.0F);
		return player;
	}

	private static ServerPlayer player(GameTestHelper helper, BlockPos origin, double x, double z) {
		return player(helper, origin, x, 0.0, z);
	}

	/** Kept from harm (Resistance V) for the rest of a test. */
	private static void shield(ServerPlayer player) {
		player.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, 600, 4, false, false));
	}

	/** Puts him on the pit's floor at the template's ({@code x}, {@code z}), facing south. */
	private static void stand(IronDreadnoughtEntity dreadnought, double x, double z) {
		BlockPos origin = dreadnought.origin();
		dreadnought.teleportTo(origin.getX() + x, dreadnought.floorY(), origin.getZ() + z);
		dreadnought.setYRot(0.0F);
		dreadnought.setYBodyRot(0.0F);
		dreadnought.setYHeadRot(0.0F);
	}

	private static boolean breaker(ServerLevel level, IronDreadnoughtEntity dreadnought, Pylon pylon, BooleanProperty property) {
		return level.getBlockState(dreadnought.breaker(pylon)).getValue(property);
	}

	private static boolean crownLive(ServerLevel level, IronDreadnoughtEntity dreadnought, Pylon pylon) {
		return level.getBlockState(dreadnought.crown(pylon)).getValue(PylonCrownBlock.LIVE);
	}

	private static boolean terminalLive(ServerLevel level, IronDreadnoughtEntity dreadnought, Pylon pylon) {
		return level.getBlockState(dreadnought.terminal(pylon)).getValue(GroundTerminalBlock.LIVE);
	}

	/** Whether every pylon is dark and every breaker open, clear and unlit. */
	private static boolean fixturesAtRest(ServerLevel level, IronDreadnoughtEntity dreadnought) {
		for (Pylon pylon : Pylon.values()) {
			if (crownLive(level, dreadnought, pylon) || terminalLive(level, dreadnought, pylon)
					|| breaker(level, dreadnought, pylon, DischargeBreakerBlock.LIVE) || breaker(level, dreadnought, pylon, DischargeBreakerBlock.THROWN)
					|| breaker(level, dreadnought, pylon, DischargeBreakerBlock.FOULED)) {
				return false;
			}
		}
		return true;
	}

	/** {@code player} uses the block at {@code pos} with an empty hand, as a click on its top would. */
	private static InteractionResult use(ServerLevel level, ServerPlayer player, BlockPos pos) {
		BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false);
		return player.gameMode.useItemOn(player, level, ItemStack.EMPTY, InteractionHand.MAIN_HAND, hit);
	}

	/** Harm of {@code type} with no one behind it, as the world deals it. */
	private static DamageSource harm(ServerLevel level, ResourceKey<DamageType> type) {
		return new DamageSource(level.registryAccess().lookupOrThrow(Registries.DAMAGE_TYPE).getOrThrow(type));
	}

	/** Whether {@code player} was last hurt by electric harm. */
	private static boolean shocked(ServerPlayer player) {
		DamageSource last = player.getLastDamageSource();
		return last != null && last.is(GroundingStrap.ELECTRIC);
	}

	/** Everywhere his fight reaches, with room round it. */
	private static AABB pit(BlockPos origin) {
		return new AABB(origin.getX() - 3, origin.getY(), origin.getZ() - 2, origin.getX() + 73, origin.getY() + 19, origin.getZ() + 74);
	}

	/** Checks {@code body}, then sends him back to his gantry and takes him away whether it passed or failed. */
	private static void ending(ServerLevel level, IronDreadnoughtEntity dreadnought, Runnable body) {
		try {
			body.run();
		} finally {
			dreadnought.reset(level);
			dreadnought.discard();
		}
	}

	/**
	 * He stands dormant in his gantry, his eye dark, unhurt by anything; a player a block over the pit's floor does not
	 * wake him, nor does a creative player on it or their blow; a player who may fight him standing on the pit's floor
	 * does: his eye lights, he is unhurt while he wakes, and {@value IronDreadnoughtEntity#WAKE_TICKS} ticks later he has
	 * stepped down onto the pit's floor at full health, scaled for one.
	 */
	@GameTest(structure = PIT, padding = 8, maxTicks = 180)
	public void heStandsInHisGantryAndAPlayerOnThePitFloorWakesHim(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		BlockPos origin = origin(helper);
		trench(level, origin);
		IronDreadnoughtEntity dreadnought = waiting(helper, origin);
		ServerPlayer player = player(helper, origin, 35.5, 1.0, 50.5);
		helper.runAfterDelay(25, () -> {
			boolean raised = dreadnought.phase() == Phase.WAITING;
			boolean dormant = dreadnought.position().distanceTo(dreadnought.waiting()) < 0.01 && dreadnought.action() == Action.DORMANT
					&& !dreadnought.eyeLit();
			player.setGameMode(GameType.CREATIVE);
			player.snapTo(player.getX(), dreadnought.floorY(), player.getZ(), 0.0F, 0.0F);
			boolean harmless = !dreadnought.hurtServer(level, level.damageSources().playerAttack(player), 10.0F)
					&& dreadnought.getHealth() == dreadnought.getMaxHealth();
			helper.runAfterDelay(25, () -> {
				boolean slept = dreadnought.phase() == Phase.WAITING;
				player.setGameMode(GameType.SURVIVAL);
				helper.runAfterDelay(12, () -> {
					boolean woken = dreadnought.phase() == Phase.WAKING && dreadnought.eyeLit() && dreadnought.getHealth() == dreadnought.getMaxHealth();
					boolean unhurt = !dreadnought.hurtServer(level, level.damageSources().playerAttack(player), 10.0F);
					helper.runAfterDelay(IronDreadnoughtEntity.WAKE_TICKS + 5, () -> {
						ending(level, dreadnought, () -> {
							Vec3 woke = dreadnought.woke();
							helper.assertTrue(raised, "A player a block over the pit's floor woke him");
							helper.assertTrue(dormant, "He does not stand dormant in his gantry, his eye dark");
							helper.assertTrue(harmless, "Dormant, he was harmed");
							helper.assertTrue(slept, "A creative player on the pit's floor, or their blow, woke him");
							helper.assertTrue(woken, "A player on the pit's floor did not wake him at full health, his eye lit");
							helper.assertTrue(unhurt, "Waking, he was harmed");
							helper.assertTrue(dreadnought.getMaxHealth() == (float) (IronDreadnoughtEntity.HEALTH * IronDreadnoughtEntity.partyScale(1)),
									"Woken by one player, his health is " + dreadnought.getMaxHealth());
							helper.assertTrue(dreadnought.phase() == Phase.ADVANCE, "He did not step down to fight: " + dreadnought.phase());
							// Down for up to 17 ticks by now, he has walked toward the player at most 2.4 blocks.
							helper.assertTrue(Math.abs(dreadnought.getY() - dreadnought.floorY()) < 0.1
									&& Math.hypot(dreadnought.getX() - woke.x, dreadnought.getZ() - woke.z) < 4.0,
									"He did not step down onto the pit's floor where he wakes but to " + dreadnought.position());
							helper.assertTrue(fixturesAtRest(level, dreadnought), "A pylon is lit or a breaker is thrown as he wakes");
							player.discard();
						});
						helper.succeed();
					});
				});
			});
		});
	}

	/**
	 * His health grows by half for each player after the first, at most two and a half times; a player's blow from beyond
	 * the pit wakes him, unhurt; Wither and poison never take hold of him. Red Line jams his vents open, makes every
	 * cooldown a third shorter and his charges come every {@value IronDreadnoughtEntity#RED_DISCHARGE_EVERY} ticks; a blow
	 * then lands in full and counts as taking part.
	 */
	@GameTest(structure = PIT, padding = 8)
	public void hisHealthGrowsWithThePartyAndRedLineHastensHim(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		BlockPos origin = origin(helper);
		IronDreadnoughtEntity dreadnought = waiting(helper, origin);
		// On the ground beyond the pit's south end: near enough to be in his party, not on the pit's floor.
		ServerPlayer far = player(helper, origin, 35.5, GROUND_UP, 60.5);
		ServerPlayer other = player(helper, origin, 37.5, GROUND_UP, 60.5);
		ending(level, dreadnought, () -> {
			helper.assertTrue(IronDreadnoughtEntity.partyScale(1) == 1.0 && IronDreadnoughtEntity.partyScale(2) == 1.5
					&& IronDreadnoughtEntity.partyScale(4) == 2.5 && IronDreadnoughtEntity.partyScale(9) == 2.5, "His health scales with the party, at most 2.5 times");
			helper.assertTrue(!dreadnought.canBeAffected(new MobEffectInstance(MobEffects.WITHER, 100))
					&& !dreadnought.canBeAffected(new MobEffectInstance(MobEffects.POISON, 100)), "Wither or poison can take hold of him");
			helper.assertTrue(dreadnought.cooldown(240) == 240 && dreadnought.dischargeEvery() == IronDreadnoughtEntity.DISCHARGE_EVERY,
					"Before Red Line his cooldowns and his charges' gap are their own");
			boolean blow = dreadnought.hurtServer(level, level.damageSources().playerAttack(far), 10.0F);
			helper.assertTrue(!blow && dreadnought.phase() == Phase.WAKING, "A player's blow from beyond the pit did not wake him, unhurt");
			helper.assertTrue(dreadnought.getMaxHealth() == (float) (IronDreadnoughtEntity.HEALTH * IronDreadnoughtEntity.partyScale(2)),
					"Woken with two players near his trench, his health is " + dreadnought.getMaxHealth());
			dreadnought.redLine(level);
			helper.assertTrue(dreadnought.phase() == Phase.RED_LINE && dreadnought.ventsOpen(), "In Red Line his vents are not jammed open");
			helper.assertTrue(dreadnought.cooldown(240) == 160 && dreadnought.cooldown(IronDreadnoughtEntity.GLOBAL_COOLDOWN) == 13,
					"In Red Line his cooldowns are not a third shorter: " + dreadnought.cooldown(240));
			helper.assertTrue(dreadnought.dischargeEvery() == IronDreadnoughtEntity.RED_DISCHARGE_EVERY
					&& dreadnought.dischargeIn() <= IronDreadnoughtEntity.RED_DISCHARGE_EVERY, "In Red Line his charges do not come sooner");
			float before = dreadnought.getHealth();
			boolean hurt = dreadnought.hurtServer(level, level.damageSources().playerAttack(far), 10.0F);
			// In full, 10 less his armour's share is over 7; through shut plating it would be under 2.5.
			helper.assertTrue(hurt && before - dreadnought.getHealth() > 5.0F, "In Red Line a blow did not land in full: " + (before - dreadnought.getHealth()));
			helper.assertTrue(dreadnought.participants(level).contains(far) && !dreadnought.participants(level).contains(other),
					"Hurting him did not count as taking part, or standing by did");
			far.discard();
			other.discard();
		});
		helper.succeed();
	}

	/**
	 * His rule's other half: while his vents are shut his plating lets only {@value IronDreadnoughtEntity#PLATED} of a blow
	 * through; overloaded, his vents blow open for {@value IronDreadnoughtEntity#OVERLOAD_TICKS} ticks, he reels for the
	 * first {@value IronDreadnoughtEntity#OVERLOAD_STAGGER}, and a blow lands in full.
	 */
	@GameTest(structure = PIT, padding = 8, maxTicks = 140)
	public void shutHisPlatingTurnsMostOfABlowAndOpenVentsTakeItAll(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		BlockPos origin = origin(helper);
		trench(level, origin);
		IronDreadnoughtEntity dreadnought = waiting(helper, origin);
		ServerPlayer striker = player(helper, origin, 35.5, GROUND_UP, 60.5);
		striker.setGameMode(GameType.CREATIVE);
		dreadnought.wake(level);
		float[] plated = {0.0F};
		boolean[] shut = {false};
		helper.runAfterDelay(AWAKE, () -> {
			shut[0] = !dreadnought.ventsOpen();
			float before = dreadnought.getHealth();
			dreadnought.hurtServer(level, level.damageSources().playerAttack(striker), 20.0F);
			plated[0] = before - dreadnought.getHealth();
		});
		helper.runAfterDelay(AWAKE + HURT_COOLDOWN, () -> {
			dreadnought.overload(level);
			boolean open = dreadnought.ventsOpen() && dreadnought.overloadTicks() == IronDreadnoughtEntity.OVERLOAD_TICKS
					&& dreadnought.staggerTicks() == IronDreadnoughtEntity.OVERLOAD_STAGGER && dreadnought.action() == Action.REEL;
			float before = dreadnought.getHealth();
			dreadnought.hurtServer(level, level.damageSources().playerAttack(striker), 20.0F);
			float full = before - dreadnought.getHealth();
			ending(level, dreadnought, () -> {
				helper.assertTrue(shut[0], "Fighting, his vents are not shut");
				helper.assertTrue(plated[0] > 0.0F && plated[0] <= 20.0F * IronDreadnoughtEntity.PLATED + 1.0E-3F,
						"His shut plating let " + plated[0] + " of a blow of 20 through, not at most " + 20.0F * IronDreadnoughtEntity.PLATED);
				helper.assertTrue(open, "Overloaded, his vents did not blow open with him reeling");
				helper.assertTrue(full > 20.0F * IronDreadnoughtEntity.PLATED + 1.0F && full > 3.0F * plated[0],
						"With his vents open a blow of 20 did only " + full);
				helper.assertTrue(dreadnought.participants(level).contains(striker), "Hurting him did not count as taking part");
				striker.discard();
			});
			helper.succeed();
		});
	}

	/**
	 * His charge: when it comes he plants his legs and his coils crackle; the pylon nearest his foe lights (its crown and
	 * its breaker's lamp) and the others stay dark. A dark breaker thrown does nothing. The lit breaker thrown routes the
	 * discharge to ground: the breaker stays thrown and the terminal live, the pylon goes dark, the thrower takes part, and
	 * he overloads; {@value IronDreadnoughtEntity#SPRING_TICKS} ticks later the breaker springs open and the terminal
	 * goes dark, his vents still open.
	 */
	@GameTest(structure = PIT, padding = 8, maxTicks = 160)
	public void hisChargeLightsThePylonNearestHisFoeAndAThrownBreakerOverloadsHim(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		BlockPos origin = origin(helper);
		trench(level, origin);
		IronDreadnoughtEntity dreadnought = waiting(helper, origin);
		ServerPlayer foe = player(helper, origin, 22.5, 40.5);
		shield(foe);
		ServerPlayer other = player(helper, origin, 50.5, GROUND_UP, 30.5);
		other.setGameMode(GameType.CREATIVE);
		dreadnought.wake(level);
		helper.runAfterDelay(AWAKE, () -> dreadnought.dischargeIn(3));
		helper.runAfterDelay(AWAKE + 6, () -> {
			boolean charging = dreadnought.charging() > 0 && dreadnought.lit().equals(List.of(Pylon.WEST)) && dreadnought.action() == Action.BRACE
					&& dreadnought.coilsCrackling();
			boolean lamps = crownLive(level, dreadnought, Pylon.WEST) && breaker(level, dreadnought, Pylon.WEST, DischargeBreakerBlock.LIVE)
					&& !crownLive(level, dreadnought, Pylon.EAST) && !crownLive(level, dreadnought, Pylon.SOUTH)
					&& !breaker(level, dreadnought, Pylon.EAST, DischargeBreakerBlock.LIVE);
			use(level, other, dreadnought.breaker(Pylon.EAST));
			boolean darkHeld = !breaker(level, dreadnought, Pylon.EAST, DischargeBreakerBlock.THROWN) && dreadnought.charging() > 0;
			InteractionResult used = use(level, foe, dreadnought.breaker(Pylon.WEST));
			boolean routed = used.consumesAction() && breaker(level, dreadnought, Pylon.WEST, DischargeBreakerBlock.THROWN)
					&& terminalLive(level, dreadnought, Pylon.WEST) && !crownLive(level, dreadnought, Pylon.WEST)
					&& !breaker(level, dreadnought, Pylon.WEST, DischargeBreakerBlock.LIVE);
			boolean overloaded = dreadnought.charging() == 0 && dreadnought.lit().isEmpty()
					&& dreadnought.overloadTicks() == IronDreadnoughtEntity.OVERLOAD_TICKS && dreadnought.ventsOpen()
					&& dreadnought.staggerTicks() == IronDreadnoughtEntity.OVERLOAD_STAGGER && dreadnought.action() == Action.REEL
					&& dreadnought.dischargeIn() == IronDreadnoughtEntity.DISCHARGE_EVERY;
			boolean part = dreadnought.participants(level).contains(foe) && !dreadnought.participants(level).contains(other);
			helper.runAfterDelay(IronDreadnoughtEntity.SPRING_TICKS + 2, () -> {
				ending(level, dreadnought, () -> {
					helper.assertTrue(charging, "His charge did not come, planted and crackling, with the west pylon (the foe's nearest) lit: "
							+ dreadnought.lit());
					helper.assertTrue(lamps, "The lit pylon's crown and breaker lamp do not burn, or a dark one's does");
					helper.assertTrue(darkHeld, "A dark breaker threw, or ended the charge");
					helper.assertTrue(routed, "Throwing the lit breaker did not route the discharge to ground");
					helper.assertTrue(overloaded, "Routed, the discharge did not overload him");
					helper.assertTrue(part, "Throwing the lit breaker did not count as taking part, or a dark one did");
					helper.assertTrue(!breaker(level, dreadnought, Pylon.WEST, DischargeBreakerBlock.THROWN) && !terminalLive(level, dreadnought, Pylon.WEST),
							"The breaker did not spring open, or the terminal stayed live");
					helper.assertTrue(dreadnought.overloadTicks() > 0 && dreadnought.ventsOpen(), "His overload ended with the breaker's springing");
					foe.discard();
					other.discard();
				});
				helper.succeed();
			});
		});
	}

	/**
	 * Left unrouted, the discharge runs out down the lit pylon's lane: it strikes a player on the lane and one within
	 * {@value IronDreadnoughtEntity#DISCHARGE_REACH} blocks of it, and spares one farther off, one on a dark lane and one
	 * well above the floor; the Grounding Strap halves it. His plating holds (no overload), the pylon goes dark, and the
	 * next charge waits its full gap.
	 */
	@GameTest(structure = PIT, padding = 8, maxTicks = 140)
	public void leftUnroutedTheDischargeRunsDownTheLitLane(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		BlockPos origin = origin(helper);
		trench(level, origin);
		IronDreadnoughtEntity dreadnought = waiting(helper, origin);
		ServerPlayer lane = player(helper, origin, 35.5, 44.5);
		ServerPlayer strapped = player(helper, origin, 35.5, 47.5);
		ServerPlayer edge = player(helper, origin, 39.5, 44.5);
		ServerPlayer clear = player(helper, origin, 41.5, 44.5);
		ServerPlayer dark = player(helper, origin, 25.5, 36.5);
		ServerPlayer high = player(helper, origin, 35.5, 3.5, 42.5);
		List<ServerPlayer> players = List.of(lane, strapped, edge, clear, dark, high);
		players.forEach(player -> player.setGameMode(GameType.CREATIVE));
		strapped.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(JugcraftDreadnought.GROUNDING_STRAP));
		dreadnought.wake(level);
		helper.runAfterDelay(LOADING_TICKS, () -> {
			players.forEach(player -> player.setGameMode(GameType.SURVIVAL));
			ending(level, dreadnought, () -> {
				dreadnought.startCharge(level, lane);
				helper.assertTrue(dreadnought.lit().equals(List.of(Pylon.SOUTH)), "His charge lit " + dreadnought.lit() + ", not the south pylon");
				int hit = dreadnought.unrouted(level);
				helper.assertTrue(hit == 3, "The discharge struck " + hit + " players, not 3");
				float bare = lane.getMaxHealth() - lane.getHealth();
				float halved = strapped.getMaxHealth() - strapped.getHealth();
				helper.assertTrue(bare > 0.0F && edge.getHealth() < edge.getMaxHealth(), "A player on the lane or beside it was not struck");
				helper.assertTrue(shocked(lane), "The discharge is not electric harm");
				helper.assertTrue(halved > 0.0F && Math.abs(bare - 2.0F * halved) < 1.0E-3F, "The Grounding Strap took " + halved + " of " + bare
						+ ", not half");
				for (ServerPlayer spared : List.of(clear, dark, high)) {
					helper.assertTrue(spared.getHealth() == spared.getMaxHealth(), "A player off the lit lane, or above it, was struck");
				}
				helper.assertTrue(!dreadnought.ventsOpen() && dreadnought.overloadTicks() == 0, "Unrouted, the discharge overloaded him");
				helper.assertTrue(dreadnought.charging() == 0 && !crownLive(level, dreadnought, Pylon.SOUTH)
						&& !breaker(level, dreadnought, Pylon.SOUTH, DischargeBreakerBlock.LIVE), "Spent, the pylon did not go dark");
				helper.assertTrue(dreadnought.dischargeIn() == IronDreadnoughtEntity.DISCHARGE_EVERY, "The next charge does not wait its full gap");
				players.forEach(ServerPlayer::discard);
			});
			helper.succeed();
		});
	}

	/**
	 * The Torn Plate: at half health he halts, unhurt, and tears a plate from his hull, then fouls the breaker of the
	 * pylon nearest his foe; {@value IronDreadnoughtEntity#TORN_TICKS} ticks on, the Overcharge begins, his charges
	 * lighting two pylons. At a fifth of his health Red Line jams his vents open and hastens his charges.
	 */
	@GameTest(structure = PIT, padding = 8, maxTicks = 200)
	public void atHalfHealthHeTearsAPlateAndFoulsTheNearestBreaker(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		BlockPos origin = origin(helper);
		trench(level, origin);
		IronDreadnoughtEntity dreadnought = waiting(helper, origin);
		ServerPlayer foe = player(helper, origin, 48.5, 36.5);
		shield(foe);
		dreadnought.wake(level);
		helper.runAfterDelay(AWAKE, () -> dreadnought.setHealth(dreadnought.getMaxHealth() * IronDreadnoughtEntity.TORN_AT));
		boolean[] torn = {false};
		helper.runAfterDelay(AWAKE + 2, () -> torn[0] = dreadnought.phase() == Phase.TORN_PLATE && dreadnought.action() == Action.TEAR
				&& !dreadnought.hurtServer(level, level.damageSources().playerAttack(foe), 10.0F));
		helper.runAfterDelay(AWAKE + 2 + IronDreadnoughtEntity.TORN_TICKS + 4, () -> {
			boolean overcharge = dreadnought.phase() == Phase.OVERCHARGE && dreadnought.pylonsLit() == 2;
			Pylon fouled = dreadnought.fouled();
			boolean jammed = fouled == Pylon.EAST && breaker(level, dreadnought, Pylon.EAST, DischargeBreakerBlock.FOULED)
					&& !breaker(level, dreadnought, Pylon.WEST, DischargeBreakerBlock.FOULED) && !breaker(level, dreadnought, Pylon.SOUTH, DischargeBreakerBlock.FOULED);
			dreadnought.setHealth(dreadnought.getMaxHealth() * 0.19F);
			helper.runAfterDelay(2, () -> {
				ending(level, dreadnought, () -> {
					helper.assertTrue(torn[0], "At half health he did not halt, unhurt, tearing a plate");
					helper.assertTrue(overcharge, "After the Torn Plate the Overcharge did not begin with two pylons to a charge: " + dreadnought.phase());
					helper.assertTrue(jammed, "The plate fouled " + fouled + ", not the east breaker (nearest his foe)");
					helper.assertTrue(dreadnought.phase() == Phase.RED_LINE && dreadnought.ventsOpen(), "At a fifth of his health Red Line did not "
							+ "jam his vents open: " + dreadnought.phase());
					helper.assertTrue(dreadnought.dischargeIn() <= IronDreadnoughtEntity.RED_DISCHARGE_EVERY && dreadnought.cooldown(120) == 80,
							"In Red Line his charges and cooldowns are not hastened");
					foe.discard();
				});
				helper.succeed();
			});
		});
	}

	/**
	 * A fouled breaker will not throw: each use works at the plate jamming it, the worker takes part, and the third
	 * knocks it loose. In the Overcharge, whenever his vents shut with no breaker fouled, he fouls the one nearest his foe
	 * (none here: nearest him) again.
	 */
	@GameTest(structure = PIT, padding = 8, maxTicks = 220)
	public void aFouledBreakerIsKnockedLooseByThreeUses(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		BlockPos origin = origin(helper);
		trench(level, origin);
		IronDreadnoughtEntity dreadnought = waiting(helper, origin);
		ServerPlayer worker = player(helper, origin, 30.5, GROUND_UP, 60.5);
		worker.setGameMode(GameType.CREATIVE);
		dreadnought.wake(level);
		helper.runAfterDelay(AWAKE, () -> dreadnought.tearPlate(level));
		int over = AWAKE + IronDreadnoughtEntity.TORN_TICKS + 4;
		int rest = DischargeBreakerBlock.REST + 1;
		Pylon[] plate = {null};
		boolean[] first = {false};
		boolean[] second = {false};
		helper.runAfterDelay(over, () -> {
			plate[0] = dreadnought.fouled();
			helper.assertTrue(dreadnought.phase() == Phase.OVERCHARGE && plate[0] != null
					&& breaker(level, dreadnought, plate[0], DischargeBreakerBlock.FOULED), "The Torn Plate fouled no breaker");
			InteractionResult used = use(level, worker, dreadnought.breaker(plate[0]));
			first[0] = used.consumesAction() && dreadnought.clearUses() == 1 && breaker(level, dreadnought, plate[0], DischargeBreakerBlock.FOULED)
					&& !breaker(level, dreadnought, plate[0], DischargeBreakerBlock.THROWN);
		});
		helper.runAfterDelay(over + rest, () -> {
			use(level, worker, dreadnought.breaker(plate[0]));
			second[0] = dreadnought.clearUses() == 2 && breaker(level, dreadnought, plate[0], DischargeBreakerBlock.FOULED);
		});
		helper.runAfterDelay(over + 2 * rest, () -> {
			use(level, worker, dreadnought.breaker(plate[0]));
			boolean loose = dreadnought.fouled() == null && dreadnought.clearUses() == 0
					&& !breaker(level, dreadnought, plate[0], DischargeBreakerBlock.FOULED);
			boolean part = dreadnought.participants(level).contains(worker);
			dreadnought.stun(level);
			boolean stunned = dreadnought.stunTicks() == IronDreadnoughtEntity.STUN_TICKS && dreadnought.ventsOpen();
			helper.runAfterDelay(IronDreadnoughtEntity.STUN_TICKS + 3, () -> {
				Pylon again = dreadnought.fouled();
				Pylon nearest = dreadnought.nearestPylons(dreadnought, 1).getFirst();
				ending(level, dreadnought, () -> {
					helper.assertTrue(first[0], "The first use threw the fouled breaker, or did not count");
					helper.assertTrue(second[0], "The second use knocked the plate loose, or did not count");
					helper.assertTrue(loose, "Three uses did not knock the plate loose");
					helper.assertTrue(part, "Working at the fouled breaker did not count as taking part");
					helper.assertTrue(stunned, "Stunned, his vents are not open");
					helper.assertTrue(!dreadnought.ventsOpen() && again == nearest && breaker(level, dreadnought, nearest, DischargeBreakerBlock.FOULED),
							"His vents shutting in the Overcharge fouled " + again + ", not the breaker nearest him, " + nearest);
					worker.discard();
				});
				helper.succeed();
			});
		});
	}

	/**
	 * His Piston Punch strikes a player before him and one forty-five degrees aside, throwing them back, and spares the
	 * one beside him, the one behind him and one out of reach. They wait out their loading in creative, where he pays
	 * them no heed.
	 */
	@GameTest(structure = PIT, padding = 8, maxTicks = 180)
	public void hisPistonPunchStrikesBeforeHimAndThrowsBack(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		BlockPos origin = origin(helper);
		IronDreadnoughtEntity dreadnought = waiting(helper, origin);
		double aside = 3.0 * Math.sin(Math.toRadians(45.0));
		ServerPlayer front = player(helper, origin, 35.5, 33.5);
		ServerPlayer slant = player(helper, origin, 35.5 + aside, 30.5 + aside);
		ServerPlayer beside = player(helper, origin, 32.0, 30.5);
		ServerPlayer behind = player(helper, origin, 35.5, 27.5);
		ServerPlayer far = player(helper, origin, 35.5, 35.5);
		List<ServerPlayer> players = List.of(front, slant, beside, behind, far);
		players.forEach(player -> player.setGameMode(GameType.CREATIVE));
		helper.runAfterDelay(LOADING_TICKS, () -> {
			players.forEach(player -> player.setGameMode(GameType.SURVIVAL));
			ending(level, dreadnought, () -> {
				stand(dreadnought, 35.5, 30.5);  // facing south, towards the one in front
				int hit = dreadnought.punch(level);
				helper.assertTrue(hit == 2, "The punch struck " + hit + " players, not 2");
				for (ServerPlayer struck : List.of(front, slant)) {
					helper.assertTrue(struck.getHealth() < struck.getMaxHealth(), "A player before him was not struck");
				}
				helper.assertTrue(front.getDeltaMovement().z > 1.0, "The player before him was not thrown back: " + front.getDeltaMovement());
				for (ServerPlayer spared : List.of(beside, behind, far)) {
					helper.assertTrue(spared.getHealth() == spared.getMaxHealth(), "A player beside, behind or out of reach was struck");
				}
				players.forEach(ServerPlayer::discard);
			});
			helper.succeed();
		});
	}

	/**
	 * His Rivet Fan: {@value IronDreadnoughtEntity#RIVETS} hot rivets fly from his battery, the middle one straight at the
	 * player and the outermost {@value IronDreadnoughtEntity#FAN_STEP} degrees apart twice over each side; the middle one
	 * strikes the player, and the rest are gone by the end of their flight. The player stands a block over the pit's
	 * floor, so he does not wake.
	 */
	@GameTest(structure = PIT, padding = 8, maxTicks = 180)
	public void hisRivetFanFiresHotRivetsThatStrike(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		BlockPos origin = origin(helper);
		IronDreadnoughtEntity dreadnought = waiting(helper, origin);
		ServerPlayer mark = player(helper, origin, 35.5, 1.0, 40.5);
		mark.setGameMode(GameType.CREATIVE);
		boolean[] fan = {false};
		helper.runAfterDelay(LOADING_TICKS, () -> {
			mark.setGameMode(GameType.SURVIVAL);
			stand(dreadnought, 35.5, 26.5);
			Vec3 aim = mark.getBoundingBox().getCenter().subtract(dreadnought.battery()).normalize();
			int fired = dreadnought.fireRivets(level, mark);
			List<HotRivetEntity> rivets = level.getEntitiesOfClass(HotRivetEntity.class, pit(origin));
			double widest = 0.0;
			for (HotRivetEntity a : rivets) {
				for (HotRivetEntity b : rivets) {
					Vec3 va = a.velocity();
					Vec3 vb = b.velocity();
					widest = Math.max(widest, Math.toDegrees(Math.acos(Math.max(-1.0, Math.min(1.0,
							(va.x * vb.x + va.z * vb.z) / (Math.hypot(va.x, va.z) * Math.hypot(vb.x, vb.z)))))));
				}
			}
			fan[0] = fired == IronDreadnoughtEntity.RIVETS && rivets.size() == IronDreadnoughtEntity.RIVETS
					&& rivets.stream().anyMatch(rivet -> rivet.velocity().normalize().dot(aim) > 0.999)
					&& Math.abs(widest - (IronDreadnoughtEntity.RIVETS - 1) * IronDreadnoughtEntity.FAN_STEP) < 1.0;
		});
		helper.runAfterDelay(LOADING_TICKS + 20, () -> {
			boolean struck = mark.getHealth() < mark.getMaxHealth();
			helper.runAfterDelay(HotRivetEntity.LIFE, () -> {
				ending(level, dreadnought, () -> {
					helper.assertTrue(fan[0], "His battery did not fire " + IronDreadnoughtEntity.RIVETS + " rivets in a fan, the middle one at the player");
					helper.assertTrue(struck, "No rivet struck the player");
					helper.assertTrue(level.getEntitiesOfClass(HotRivetEntity.class, pit(origin)).isEmpty(), "His rivets outlived their flight");
					mark.discard();
				});
				helper.succeed();
			});
		});
	}

	/**
	 * His Trench Charge strikes a player he runs into and throws them aside, once only, and spares one ahead not yet
	 * touched.
	 */
	@GameTest(structure = PIT, padding = 8, maxTicks = 180)
	public void hisTrenchChargeStrikesWhoHeRunsIntoOnce(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		BlockPos origin = origin(helper);
		IronDreadnoughtEntity dreadnought = waiting(helper, origin);
		ServerPlayer struck = player(helper, origin, 35.5, 32.5);
		ServerPlayer ahead = player(helper, origin, 35.5, 35.5);
		List<ServerPlayer> players = List.of(struck, ahead);
		players.forEach(player -> player.setGameMode(GameType.CREATIVE));
		helper.runAfterDelay(LOADING_TICKS, () -> {
			players.forEach(player -> player.setGameMode(GameType.SURVIVAL));
			ending(level, dreadnought, () -> {
				stand(dreadnought, 35.5, 30.5);
				dreadnought.chargeWay(new Vec3(0.0, 0.0, 1.0));
				int hit = dreadnought.chargeHits(level);
				helper.assertTrue(hit == 1 && struck.getHealth() < struck.getMaxHealth(), "The charge struck " + hit + " players, not the one he runs into");
				helper.assertTrue(Math.abs(struck.getDeltaMovement().x) > 1.0, "The player he ran into was not thrown aside: " + struck.getDeltaMovement());
				helper.assertTrue(ahead.getHealth() == ahead.getMaxHealth(), "A player ahead, not yet touched, was struck");
				helper.assertTrue(dreadnought.chargeHits(level) == 0, "The charge struck the same player twice");
				players.forEach(ServerPlayer::discard);
			});
			helper.succeed();
		});
	}

	/**
	 * His Trench Charge in the fight: through the wind-up he lowers his ram toward his foe, then runs; into a wall he is
	 * stunned for {@value IronDreadnoughtEntity#STUN_TICKS} ticks, a vent knocked open, short of the wall.
	 */
	@GameTest(structure = PIT, padding = 8, maxTicks = 160)
	public void hisTrenchChargeIntoAWallStunsHim(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		BlockPos origin = origin(helper);
		trench(level, origin);
		BlockState wall = Blocks.STONE_BRICKS.defaultBlockState();
		for (int y = FoundryTrench.STAND; y <= FoundryTrench.STAND + 1; y++) {
			fill(level, origin, 30, y, 38, 41, 38, wall);
		}
		IronDreadnoughtEntity dreadnought = waiting(helper, origin);
		ServerPlayer foe = player(helper, origin, 35.5, 44.5);
		shield(foe);
		dreadnought.wake(level);
		Attack charge = Attack.TRENCH_CHARGE;
		helper.runAfterDelay(AWAKE, () -> {
			stand(dreadnought, 35.5, 30.5);
			dreadnought.begin(level, charge, foe);
		});
		boolean[] windup = {false};
		helper.runAfterDelay(AWAKE + charge.windup - 4, () -> windup[0] = dreadnought.attack() == charge
				&& dreadnought.action() == Action.CHARGE_WINDUP && Math.abs(dreadnought.getZ() - (origin.getZ() + 30.5)) < 0.3);
		helper.runAfterDelay(AWAKE + charge.windup + 16, () -> {
			ending(level, dreadnought, () -> {
				helper.assertTrue(windup[0], "He did not wind up his charge where he stood");
				helper.assertTrue(dreadnought.stunTicks() > 0 && dreadnought.ventsOpen() && dreadnought.action() == Action.REEL,
						"Charging into the wall, he was not stunned with a vent open: " + dreadnought.stunTicks());
				helper.assertTrue(dreadnought.getZ() > origin.getZ() + 32.0 && dreadnought.getZ() < origin.getZ() + 38.0,
						"He did not run up to the wall and stop short of it: " + dreadnought.position());
				helper.assertTrue(dreadnought.attack() == null, "Stunned, his charge ran on");
				foe.discard();
			});
			helper.succeed();
		});
	}

	/**
	 * His Coil Arc strikes the player and jumps to the nearest other player within
	 * {@value IronDreadnoughtEntity#ARC_JUMP} blocks of the last it struck, and on; it spares one too far from the last.
	 * It is electric harm.
	 */
	@GameTest(structure = PIT, padding = 8, maxTicks = 180)
	public void hisCoilArcJumpsBetweenPlayers(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		BlockPos origin = origin(helper);
		IronDreadnoughtEntity dreadnought = waiting(helper, origin);
		ServerPlayer target = player(helper, origin, 35.5, 38.5);
		ServerPlayer next = player(helper, origin, 38.5, 38.5);
		ServerPlayer last = player(helper, origin, 41.5, 38.5);
		ServerPlayer far = player(helper, origin, 48.5, 38.5);
		List<ServerPlayer> players = List.of(target, next, last, far);
		players.forEach(player -> player.setGameMode(GameType.CREATIVE));
		helper.runAfterDelay(LOADING_TICKS, () -> {
			players.forEach(player -> player.setGameMode(GameType.SURVIVAL));
			ending(level, dreadnought, () -> {
				stand(dreadnought, 35.5, 30.5);
				int struck = dreadnought.arc(level, target);
				helper.assertTrue(struck == 3, "The arc struck " + struck + " players, not 3");
				for (ServerPlayer hit : List.of(target, next, last)) {
					helper.assertTrue(hit.getHealth() < hit.getMaxHealth() && shocked(hit), "A player in the arc's chain was not struck by electric harm");
				}
				helper.assertTrue(far.getHealth() == far.getMaxHealth(), "The arc jumped farther than " + IronDreadnoughtEntity.ARC_JUMP + " blocks");
				players.forEach(ServerPlayer::discard);
			});
			helper.succeed();
		});
	}

	/**
	 * His Mortar: the shell flies {@value IronDreadnoughtEntity#SHELL_FLIGHT} ticks from the hatch on his back and lands
	 * on its mark, striking everyone within {@value IronDreadnoughtEntity#MORTAR_REACH} blocks of it and sparing one
	 * farther off. The players stand a little over the pit's floor, so he does not wake.
	 */
	@GameTest(structure = PIT, padding = 8, maxTicks = 160)
	public void hisMortarShellLandsOnItsMark(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		BlockPos origin = origin(helper);
		IronDreadnoughtEntity dreadnought = waiting(helper, origin);
		ServerPlayer on = player(helper, origin, 35.5, 0.6, 40.5);
		ServerPlayer near = player(helper, origin, 37.5, 0.6, 40.5);
		ServerPlayer out = player(helper, origin, 39.5, 0.6, 40.5);
		List<ServerPlayer> players = List.of(on, near, out);
		players.forEach(player -> player.setGameMode(GameType.CREATIVE));
		MortarShellEntity[] shell = {null};
		helper.runAfterDelay(LOADING_TICKS, () -> {
			players.forEach(player -> player.setGameMode(GameType.SURVIVAL));
			dreadnought.mark(new Vec3(origin.getX() + 35.5, dreadnought.floorY(), origin.getZ() + 40.5));
			shell[0] = MortarShellEntity.lob(level, dreadnought, dreadnought.mortarMuzzle(), dreadnought.mark(), IronDreadnoughtEntity.SHELL_FLIGHT);
		});
		boolean[] flying = {false};
		helper.runAfterDelay(LOADING_TICKS + IronDreadnoughtEntity.SHELL_FLIGHT - 4, () -> flying[0] = !shell[0].isRemoved()
				&& players.stream().allMatch(player -> player.getHealth() == player.getMaxHealth()));
		helper.runAfterDelay(LOADING_TICKS + IronDreadnoughtEntity.SHELL_FLIGHT + 4, () -> {
			ending(level, dreadnought, () -> {
				helper.assertTrue(flying[0], "The shell landed before its flight was over");
				helper.assertTrue(shell[0].isRemoved(), "The shell did not land");
				helper.assertTrue(on.getHealth() < on.getMaxHealth() && near.getHealth() < near.getMaxHealth(), "A player near the mark was not struck");
				helper.assertTrue(out.getHealth() == out.getMaxHealth(), "A player out of the shell's reach was struck");
				players.forEach(ServerPlayer::discard);
			});
			helper.succeed();
		});
	}

	/**
	 * His Blowdown scalds everyone within {@value IronDreadnoughtEntity#BLOWDOWN_REACH} blocks of his middle, slows them
	 * and pushes them back, and spares one farther off.
	 */
	@GameTest(structure = PIT, padding = 8, maxTicks = 180)
	public void hisBlowdownScaldsAndSlowsThoseClose(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		BlockPos origin = origin(helper);
		IronDreadnoughtEntity dreadnought = waiting(helper, origin);
		ServerPlayer close = player(helper, origin, 35.5, 34.0);
		ServerPlayer edge = player(helper, origin, 39.5, 30.5);
		ServerPlayer out = player(helper, origin, 35.5, 36.5);
		List<ServerPlayer> players = List.of(close, edge, out);
		players.forEach(player -> player.setGameMode(GameType.CREATIVE));
		helper.runAfterDelay(LOADING_TICKS, () -> {
			players.forEach(player -> player.setGameMode(GameType.SURVIVAL));
			ending(level, dreadnought, () -> {
				stand(dreadnought, 35.5, 30.5);
				int hit = dreadnought.blowdown(level);
				helper.assertTrue(hit == 2, "The blowdown struck " + hit + " players, not 2");
				for (ServerPlayer scalded : List.of(close, edge)) {
					helper.assertTrue(scalded.getHealth() < scalded.getMaxHealth() && scalded.hasEffect(MobEffects.SLOWNESS),
							"A player close to him was not scalded and slowed");
				}
				helper.assertTrue(close.getDeltaMovement().z > 0.5, "The player close to him was not pushed back: " + close.getDeltaMovement());
				helper.assertTrue(out.getHealth() == out.getMaxHealth() && !out.hasEffect(MobEffects.SLOWNESS), "A player out of reach was scalded");
				players.forEach(ServerPlayer::discard);
			});
			helper.succeed();
		});
	}

	/**
	 * His Sparkers: {@value IronDreadnoughtEntity#DROPPED} drop from his belly at a time, never more than
	 * {@value IronDreadnoughtEntity#SPARKER_CAP}. One near a player stops, crackles and discharges, an electric arc, and is
	 * spent; struck first a Sparker dies, and its striker takes part. When he goes back to his gantry his Sparkers burn
	 * out.
	 */
	@GameTest(structure = PIT, padding = 8, maxTicks = 200)
	public void hisSparkersDropCrackleAndDischarge(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		BlockPos origin = origin(helper);
		trench(level, origin);
		IronDreadnoughtEntity dreadnought = waiting(helper, origin);
		// At the pit's far end, out of reach of everything he does in the Advance.
		ServerPlayer foe = player(helper, origin, 35.5, 54.5);
		ServerPlayer striker = player(helper, origin, 30.5, GROUND_UP, 60.5);
		foe.setGameMode(GameType.CREATIVE);
		striker.setGameMode(GameType.CREATIVE);
		dreadnought.wake(level);
		SparkerEntity[] near = {null};
		boolean[] capped = {false};
		boolean[] killed = {false};
		helper.runAfterDelay(LOADING_TICKS, () -> {
			foe.setGameMode(GameType.SURVIVAL);
			int first = dreadnought.dropSparkers(level);
			int second = dreadnought.dropSparkers(level);
			int third = dreadnought.dropSparkers(level);
			capped[0] = first == IronDreadnoughtEntity.DROPPED && second == IronDreadnoughtEntity.DROPPED && third == 0
					&& dreadnought.sparkers().size() == IronDreadnoughtEntity.SPARKER_CAP;
			near[0] = SparkerEntity.drop(level, dreadnought, foe.position().add(1.0, 0.0, 0.0));
			if (level.getEntity(dreadnought.sparkers().getFirst()) instanceof SparkerEntity struck) {
				killed[0] = struck.hurtServer(level, level.damageSources().playerAttack(striker), 10.0F) && !struck.isAlive();
			}
		});
		helper.runAfterDelay(LOADING_TICKS + 34, () -> {
			boolean discharged = near[0].isRemoved() && foe.getHealth() < foe.getMaxHealth() && shocked(foe);
			boolean part = dreadnought.participants(level).contains(striker);
			int left = dreadnought.sparkers().size();
			dreadnought.reset(level);
			boolean burnt = level.getEntitiesOfClass(SparkerEntity.class, pit(origin), SparkerEntity::isAlive).isEmpty();
			ending(level, dreadnought, () -> {
				helper.assertTrue(capped[0], "His Sparkers did not drop two at a time up to his cap of " + IronDreadnoughtEntity.SPARKER_CAP);
				helper.assertTrue(killed[0], "Struck, a Sparker did not die");
				helper.assertTrue(part, "Striking a Sparker did not count as taking part");
				helper.assertTrue(left == IronDreadnoughtEntity.SPARKER_CAP - 1, "He counts " + left + " Sparkers, not the three left");
				helper.assertTrue(discharged, "The Sparker near the player did not discharge an electric arc and go");
				helper.assertTrue(burnt, "His Sparkers outlived his going back to his gantry");
				foe.discard();
				striker.discard();
			});
			helper.succeed();
		});
	}

	/**
	 * The Grounding Strap held in the offhand: electric harm (his arcs, lightning) does half to its holder, other harm
	 * the same; in the main hand it does nothing.
	 */
	@GameTest(maxTicks = 160)
	public void theGroundingStrapHalvesElectricHarm(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		ServerPlayer holder = helper.makeMockServerPlayerInLevel();
		ServerPlayer bare = helper.makeMockServerPlayerInLevel();
		BlockPos at = helper.absolutePos(new BlockPos(1, 1, 1));
		for (ServerPlayer player : List.of(holder, bare)) {
			player.setGameMode(GameType.SURVIVAL);
			player.snapTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, 0.0F, 0.0F);
		}
		DamageSource arc = harm(level, GroundingStrap.ARC);
		DamageSource lightning = harm(level, DamageTypes.LIGHTNING_BOLT);
		float[] losses = new float[8];
		helper.runAfterDelay(LOADING_TICKS, () -> {
			helper.assertTrue(!GroundingStrap.holds(holder), "With nothing in the offhand, the strap is held");
			holder.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(JugcraftDreadnought.GROUNDING_STRAP));
			helper.assertTrue(GroundingStrap.holds(holder), "In the offhand, the strap is not held");
			helper.assertTrue(arc.is(GroundingStrap.ELECTRIC) && lightning.is(GroundingStrap.ELECTRIC) && !level.damageSources().magic().is(GroundingStrap.ELECTRIC),
					"His arcs and lightning are not electric harm, or magic is");
			holder.hurtServer(level, arc, 4.0F);
			bare.hurtServer(level, arc, 4.0F);
			losses[0] = holder.getMaxHealth() - holder.getHealth();
			losses[1] = bare.getMaxHealth() - bare.getHealth();
		});
		helper.runAfterDelay(LOADING_TICKS + HURT_COOLDOWN, () -> {
			float holderBefore = holder.getHealth();
			float bareBefore = bare.getHealth();
			holder.hurtServer(level, lightning, 4.0F);
			bare.hurtServer(level, lightning, 4.0F);
			losses[2] = holderBefore - holder.getHealth();
			losses[3] = bareBefore - bare.getHealth();
		});
		helper.runAfterDelay(LOADING_TICKS + 2 * HURT_COOLDOWN, () -> {
			float holderBefore = holder.getHealth();
			float bareBefore = bare.getHealth();
			holder.hurtServer(level, level.damageSources().magic(), 2.0F);
			bare.hurtServer(level, level.damageSources().magic(), 2.0F);
			losses[4] = holderBefore - holder.getHealth();
			losses[5] = bareBefore - bare.getHealth();
			holder.setItemInHand(InteractionHand.OFF_HAND, ItemStack.EMPTY);
			holder.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(JugcraftDreadnought.GROUNDING_STRAP));
		});
		helper.runAfterDelay(LOADING_TICKS + 3 * HURT_COOLDOWN, () -> {
			boolean mainHand = !GroundingStrap.holds(holder);
			float before = holder.getHealth();
			holder.hurtServer(level, arc, 4.0F);
			losses[6] = before - holder.getHealth();
			helper.assertTrue(losses[1] > 0.0F && Math.abs(losses[0] * 2.0F - losses[1]) < 1.0E-3F, "Held, the strap let " + losses[0]
					+ " of an arc through where " + losses[1] + " hurt without it, not half");
			helper.assertTrue(losses[3] > 0.0F && Math.abs(losses[2] * 2.0F - losses[3]) < 1.0E-3F, "Held, the strap let " + losses[2]
					+ " of lightning through where " + losses[3] + " hurt without it, not half");
			helper.assertTrue(losses[5] > 0.0F && Math.abs(losses[4] - losses[5]) < 1.0E-3F, "Held, the strap changed other harm: " + losses[4]
					+ " where " + losses[5] + " hurt without it");
			helper.assertTrue(mainHand && Math.abs(losses[6] - losses[1]) < 1.0E-3F, "In the main hand, the strap still halved an arc: " + losses[6]);
			holder.discard();
			bare.discard();
			helper.succeed();
		});
	}

	/** Strayed farther than {@value IronDreadnoughtEntity#LEASH} blocks from the pit's middle, he is set back in it. */
	@GameTest(structure = PIT, padding = 8, maxTicks = 100)
	public void heKeepsToHisPit(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		BlockPos origin = origin(helper);
		trench(level, origin);
		IronDreadnoughtEntity dreadnought = waiting(helper, origin);
		dreadnought.wake(level);
		helper.runAfterDelay(AWAKE, () -> {
			Vec3 centre = dreadnought.centre();
			dreadnought.teleportTo(centre.x + IronDreadnoughtEntity.LEASH + 2.0, centre.y, centre.z);
			helper.runAfterDelay(2, () -> {
				ending(level, dreadnought, () -> helper.assertTrue(dreadnought.position().distanceTo(centre) <= IronDreadnoughtEntity.LEASH,
						"Strayed from his pit, he was not set back in it: " + dreadnought.position()));
				helper.succeed();
			});
		});
	}

	/**
	 * Left alone {@value IronDreadnoughtEntity#ABANDON_TICKS} ticks, he goes back to his gantry, healed: his Sparkers burn
	 * out, the pylons go dark, the breakers are cleared and open, his eye goes dark, and who hurt him is forgotten.
	 */
	@GameTest(structure = PIT, padding = 8, maxTicks = 320)
	public void leftAloneHeGoesBackToHisGantry(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		BlockPos origin = origin(helper);
		trench(level, origin);
		IronDreadnoughtEntity dreadnought = waiting(helper, origin);
		ServerPlayer gone = player(helper, origin, 35.5, GROUND_UP, 60.5);
		gone.setGameMode(GameType.CREATIVE);
		dreadnought.wake(level);
		helper.runAfterDelay(AWAKE, () -> {
			dreadnought.took(gone, 5.0F);
			dreadnought.setHealth(dreadnought.getMaxHealth() * 0.8F);
			dreadnought.foul(level, Pylon.WEST);
			dreadnought.startCharge(level, dreadnought);
			int dropped = dreadnought.dropSparkers(level);
			boolean counted = dreadnought.participants(level).contains(gone);
			boolean busy = dropped == IronDreadnoughtEntity.DROPPED && breaker(level, dreadnought, Pylon.WEST, DischargeBreakerBlock.FOULED)
					&& !fixturesAtRest(level, dreadnought);
			helper.runAfterDelay(IronDreadnoughtEntity.ABANDON_TICKS + 20, () -> {
				ending(level, dreadnought, () -> {
					helper.assertTrue(counted && busy, "Whoever hurt him was not counted, or his Sparkers and the fouled breaker were not set up");
					helper.assertTrue(dreadnought.phase() == Phase.WAITING && dreadnought.position().distanceTo(dreadnought.waiting()) < 0.1,
							"Left alone he did not go back to his gantry: " + dreadnought.phase() + " at " + dreadnought.position());
					helper.assertTrue(dreadnought.getHealth() == dreadnought.getMaxHealth() && !dreadnought.eyeLit(),
							"Back in his gantry he is not healed, or his eye burns");
					helper.assertTrue(fixturesAtRest(level, dreadnought) && dreadnought.fouled() == null, "Back in his gantry, a pylon is lit or a breaker "
							+ "thrown or fouled");
					helper.assertTrue(level.getEntitiesOfClass(SparkerEntity.class, pit(origin), SparkerEntity::isAlive).isEmpty()
							&& dreadnought.sparkers().isEmpty(), "His Sparkers outlived his going back");
					helper.assertTrue(dreadnought.participants(level).isEmpty(), "Who hurt him was not forgotten");
					gone.discard();
				});
				helper.succeed();
			});
		});
	}

	/**
	 * His loot is each participant's own: everyone who struck him, and one who only routed a discharge, rolls for
	 * themselves (Salvaged Coil, and on a first kill always one of his trophies) into their own inventory with a share of
	 * his experience, and a bystander who took no part gets nothing. Afterwards Grounded is theirs, and a later roll no
	 * longer counts as a first kill. As he falls his eye goes dark, the pylons are put to rest and, with no lair instance,
	 * no Grey Mist opens in the gantry.
	 */
	@GameTest(structure = PIT, padding = 8, maxTicks = 60)
	public void hisLootIsEachParticipantsOwn(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		BlockPos origin = origin(helper);
		trench(level, origin);
		IronDreadnoughtEntity dreadnought = waiting(helper, origin);
		ServerPlayer first = player(helper, origin, 33.5, GROUND_UP, 60.5);
		ServerPlayer second = player(helper, origin, 37.5, GROUND_UP, 60.5);
		ServerPlayer router = player(helper, origin, 19.5, GROUND_UP, 60.5);
		ServerPlayer bystander = player(helper, origin, 35.5, GROUND_UP, 62.5);
		dreadnought.took(first, 10.0F);
		dreadnought.took(second, 4.0F);
		dreadnought.route(level, Pylon.WEST, router);
		ending(level, dreadnought, () -> {
			List<ServerPlayer> participants = dreadnought.participants(level);
			helper.assertTrue(participants.contains(first) && participants.contains(second) && participants.contains(router),
					"Both who struck him and the one who routed a discharge take part");
			helper.assertFalse(participants.contains(bystander), "A bystander who took no part does");
			helper.assertTrue(IronDreadnoughtLoot.firstKill(first), "A newcomer has already won Grounded");
			List<ItemStack> loot = IronDreadnoughtLoot.roll(level, dreadnought, first);
			int coil = loot.stream().filter(stack -> stack.is(JugcraftDreadnought.SALVAGED_COIL)).mapToInt(ItemStack::getCount).sum();
			helper.assertTrue(coil >= 3 && coil <= 6, coil + " Salvaged Coil, not 3 to 6");
			helper.assertTrue(loot.stream().filter(IronDreadnoughtLoot::trophy).count() == 1, "A first kill always brings one of his trophies");
			dreadnought.hurtServer(level, level.damageSources().genericKill(), Float.MAX_VALUE);
			helper.assertTrue(dreadnought.isDeadOrDying() && !dreadnought.eyeLit(), "He did not fall with his eye going dark");
			helper.assertTrue(fixturesAtRest(level, dreadnought), "As he fell the pylons were not put to rest");
			for (BlockPos cell : FoundryTrench.exitCells()) {
				helper.assertFalse(level.getBlockState(cell.offset(origin)).is(JugcraftLairs.LAIR_EXIT), "With no lair instance Grey Mist opened in the gantry");
			}
			int share = IronDreadnoughtEntity.EXPERIENCE / 3;
			for (ServerPlayer player : List.of(first, second, router)) {
				helper.assertTrue(player.getInventory().countItem(JugcraftDreadnought.SALVAGED_COIL) >= 3, "A participant's loot did not go into their inventory");
				int trophies = player.getInventory().countItem(JugcraftDreadnought.dynamoHalberd())
						+ player.getInventory().countItem(JugcraftDreadnought.pistonHammer());
				helper.assertTrue(trophies == 1, "A first kill brought " + trophies + " of his trophies, not 1");
				helper.assertTrue(player.totalExperience >= share, "A participant got " + player.totalExperience + " experience, not " + share);
				helper.assertFalse(IronDreadnoughtLoot.firstKill(player), "Grounded is not theirs");
			}
			helper.assertTrue(bystander.getInventory().countItem(JugcraftDreadnought.SALVAGED_COIL) == 0 && bystander.totalExperience == 0,
					"The bystander got loot");
			helper.assertTrue(IronDreadnoughtLoot.firstKill(bystander), "The bystander won Grounded");
			for (ServerPlayer player : List.of(first, second, router, bystander)) {
				player.discard();
			}
		});
		helper.succeed();
	}
}
