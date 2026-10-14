package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.lair.CryptArchive;
import io.github.jimbozoomer.jugcraft.lair.GraveSealBlock;
import io.github.jimbozoomer.jugcraft.lair.JugcraftLairs;
import io.github.jimbozoomer.jugcraft.lair.NameStoneBlock;
import io.github.jimbozoomer.jugcraft.lair.NameStoneBlock.Glow;
import io.github.jimbozoomer.jugcraft.lair.NameStoneBlock.Rune;
import io.github.jimbozoomer.jugcraft.lair.PhylacteryBlock;
import io.github.jimbozoomer.jugcraft.lair.lich.CryptLichEntity;
import io.github.jimbozoomer.jugcraft.lair.lich.CryptLichLoot;
import io.github.jimbozoomer.jugcraft.lair.lich.EmptiedPhylactery;
import io.github.jimbozoomer.jugcraft.lair.lich.JugcraftLich;
import io.github.jimbozoomer.jugcraft.lair.lich.LichBonesEntity;
import io.github.jimbozoomer.jugcraft.lair.lich.RestlessDeadEntity;
import io.github.jimbozoomer.jugcraft.lair.lich.SoulBoltEntity;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * The Crypt Lich (docs/features/crypt-lich.md), as far as a game-test server can show him: one put down on his own (with
 * no lair instance, over an archive laid out from near the corner of an arena the hall's size, so his throne, his hall,
 * its alcoves and its fixtures' places are all loaded; each test lays the floors and fixtures it needs) waits on his
 * throne until a player who may fight him stands on the hall's floor, then comes down the apse's steps with his chains
 * burning and a name lit; his health grows with the party, and the Last Name shortens his cooldowns; his chains turn every
 * blow aside until the lit names' seals are broken, in their order, and then he is exposed until they reforge; a dark
 * name's seal raises a Restless Dead from that alcove's sarcophagus, as Raise the Dead raises them from the floor graves
 * nearest his foe, never more than four; the Erasure scrapes a name, and reading it back at the lectern (or waiting it
 * out) cuts it again; his Grave Sweep, Bone Lanes, Soul Bolt (struck back, it staggers or hurts him), Soul Drain (a
 * pillar breaks it) and Grave Pull (its dark part spares); he keeps to his hall; left alone he climbs back to his throne;
 * his loot is each participant's own; and the Emptied Phylactery keeps Wither off. His fight in the Archive of Last Names
 * itself runs in a real world, in {@link CryptLichClientGameTests}.
 *
 * <p>He fights any player within 38 blocks of his dais; the arena keeps every other test's players farther off than
 * that. Each test sends him back to his throne, with his dead, his bolts and his bones, and takes him away.
 */
public class CryptLichGameTests {
	/** An arena of air a little bigger than the archive's hall and alcoves (78 x 78), so everything of his fight is loaded. */
	private static final String HALL = "jugcraft-test:crypt_hall";
	/**
	 * A new player cannot be hurt until its client has loaded the world, or for 60 ticks; a mock player has no client, so
	 * a test that hurts one waits this long first.
	 */
	private static final int LOADING_TICKS = 80;
	/** A creature hurt shrugs off a blow no harder than the last for 10 ticks after (vanilla's); a test waits this long between blows. */
	private static final int HURT_COOLDOWN = 12;
	/** A tick or two past his waking, when he has come down to the hall's floor with his chains burning. */
	private static final int AWAKE = CryptLichEntity.WAKE_TICKS + 5;

	/** The origin of an archive laid out from near the arena's corner: its hall's floor two blocks over the arena's. */
	private static BlockPos origin(GameTestHelper helper) {
		return helper.absolutePos(new BlockPos(6, -3, 4));
	}

	/** He waits on the throne of the archive laid out from {@code origin}. */
	private static CryptLichEntity waiting(GameTestHelper helper, BlockPos origin) {
		return CryptLichEntity.summon(helper.getLevel(), origin, null);
	}

	/** {@code state} in every cell of the template's box from (x0, y, z0) to (x1, y, z1). */
	private static void fill(ServerLevel level, BlockPos origin, int x0, int y, int z0, int x1, int z1, BlockState state) {
		for (int x = x0; x <= x1; x++) {
			for (int z = z0; z <= z1; z++) {
				level.setBlock(origin.offset(x, y, z), state, Block.UPDATE_CLIENTS);
			}
		}
	}

	/**
	 * The archive's floors and fixtures: the hall's floor, the two alcoves' and the throne's apse; the plinth on the dais
	 * with the phylactery on it; each name's grave seal and name-stone, facing out of its alcove's back wall; and the
	 * Lectern of Last Names.
	 */
	private static void archive(ServerLevel level, BlockPos origin) {
		BlockState floor = Blocks.DEEPSLATE_TILES.defaultBlockState();
		int under = CryptArchive.STAND - 1;
		fill(level, origin, CryptArchive.HALL_WEST, under, CryptArchive.HALL_NORTH, CryptArchive.HALL_EAST, CryptArchive.HALL_SOUTH, floor);
		fill(level, origin, 6, under, 33, 11, 35, floor);
		fill(level, origin, 53, under, 33, 58, 35, floor);
		fill(level, origin, 26, CryptArchive.APSE_STAND - 1, 5, 38, 13, floor);
		BlockPos plinth = origin.offset(CryptArchive.PHYLACTERY).below();
		level.setBlock(plinth, Blocks.POLISHED_BLACKSTONE.defaultBlockState(), Block.UPDATE_CLIENTS);
		level.setBlock(origin.offset(CryptArchive.PHYLACTERY), JugcraftLairs.PHYLACTERY.defaultBlockState(), Block.UPDATE_CLIENTS);
		Direction[] faces = {Direction.EAST, Direction.SOUTH, Direction.WEST};
		for (Rune rune : Rune.values()) {
			Direction face = faces[rune.ordinal()];
			level.setBlock(CryptArchive.seal(origin, rune), JugcraftLairs.GRAVE_SEAL.defaultBlockState()
					.setValue(HorizontalDirectionalBlock.FACING, face).setValue(GraveSealBlock.RUNE, rune), Block.UPDATE_CLIENTS);
			level.setBlock(CryptArchive.stone(origin, rune), JugcraftLairs.NAME_STONE.defaultBlockState()
					.setValue(HorizontalDirectionalBlock.FACING, face).setValue(NameStoneBlock.RUNE, rune), Block.UPDATE_CLIENTS);
		}
		level.setBlock(origin.offset(CryptArchive.LECTERN), JugcraftLairs.ARCHIVE_LECTERN.defaultBlockState()
				.setValue(HorizontalDirectionalBlock.FACING, Direction.SOUTH), Block.UPDATE_CLIENTS);
	}

	/** A survival player standing at the template's ({@code x}, {@code z}), {@code up} blocks over the hall's floor. */
	private static ServerPlayer player(GameTestHelper helper, BlockPos origin, double x, double up, double z) {
		ServerPlayer player = helper.makeMockServerPlayerInLevel();
		player.setGameMode(GameType.SURVIVAL);
		player.snapTo(origin.getX() + x, origin.getY() + CryptArchive.STAND + up, origin.getZ() + z, 0.0F, 0.0F);
		return player;
	}

	private static ServerPlayer player(GameTestHelper helper, BlockPos origin, double x, double z) {
		return player(helper, origin, x, 0.0, z);
	}

	/** Puts him on the hall's floor at the template's ({@code x}, {@code z}), facing south. */
	private static void stand(CryptLichEntity lich, double x, double z) {
		BlockPos origin = lich.origin();
		lich.teleportTo(origin.getX() + x, lich.floorY(), origin.getZ() + z);
		lich.setYRot(0.0F);
	}

	private static Glow glow(ServerLevel level, CryptLichEntity lich, Rune rune) {
		return level.getBlockState(lich.stone(rune)).getValue(NameStoneBlock.GLOW);
	}

	private static boolean broken(ServerLevel level, CryptLichEntity lich, Rune rune) {
		return level.getBlockState(lich.seal(rune)).getValue(GraveSealBlock.BROKEN);
	}

	private static boolean cracked(ServerLevel level, CryptLichEntity lich) {
		return level.getBlockState(lich.phylactery()).getValue(PhylacteryBlock.CRACKED);
	}

	/** {@code player} uses the block at {@code pos} with an empty hand, as a click on its {@code face} would. */
	private static InteractionResult use(ServerLevel level, ServerPlayer player, BlockPos pos, Direction face) {
		BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(pos), face, pos, false);
		return player.gameMode.useItemOn(player, level, ItemStack.EMPTY, InteractionHand.MAIN_HAND, hit);
	}

	/** His Restless Dead walking now. */
	private static List<RestlessDeadEntity> risen(ServerLevel level, CryptLichEntity lich) {
		List<RestlessDeadEntity> out = new ArrayList<>();
		for (UUID id : lich.dead()) {
			if (level.getEntity(id) instanceof RestlessDeadEntity found) {
				out.add(found);
			}
		}
		return out;
	}

	/** The Wither effect's own harm, as it lands on whoever it has taken hold of. */
	private static DamageSource withering(ServerLevel level) {
		return new DamageSource(level.registryAccess().lookupOrThrow(Registries.DAMAGE_TYPE).getOrThrow(DamageTypes.WITHER));
	}

	/** Everywhere his fight reaches, with room round it. */
	private static AABB hall(BlockPos origin) {
		return new AABB(origin.getX() - 6, origin.getY(), origin.getZ() - 4, origin.getX() + 72, origin.getY() + 17, origin.getZ() + 74);
	}

	/** Checks {@code body}, then sends him back to his throne and takes him away whether it passed or failed. */
	private static void ending(ServerLevel level, CryptLichEntity lich, Runnable body) {
		try {
			body.run();
		} finally {
			lich.reset(level);
			lich.discard();
		}
	}

	/**
	 * He waits on his throne, unhurt by anything, and a creative player's blow does not wake him; a player who may fight
	 * him standing on the hall's floor does: his crown lights, he is unhurt while he wakes, and
	 * {@value CryptLichEntity#WAKE_TICKS} ticks later he has come down to the hall's floor at full health, his chains
	 * burning: all three seals whole, one name lit and the others dark.
	 */
	@GameTest(structure = HALL, padding = 8, maxTicks = 160)
	public void heWaitsOnHisThroneAndAPlayerOnTheFloorWakesHim(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		BlockPos origin = origin(helper);
		archive(level, origin);
		CryptLichEntity lich = waiting(helper, origin);
		ServerPlayer player = player(helper, origin, 32.5, 44.5);
		player.setGameMode(GameType.CREATIVE);
		helper.runAfterDelay(20, () -> {
			boolean waited = lich.phase() == CryptLichEntity.Phase.WAITING && lich.position().distanceTo(lich.waiting()) < 0.01
					&& lich.action() == CryptLichEntity.Action.THRONE && lich.crown() == CryptLichEntity.Crown.DARK;
			boolean harmless = !lich.hurtServer(level, level.damageSources().playerAttack(player), 10.0F) && lich.getHealth() == lich.getMaxHealth();
			boolean slept = lich.phase() == CryptLichEntity.Phase.WAITING;
			player.setGameMode(GameType.SURVIVAL);
			helper.runAfterDelay(12, () -> {
				boolean woken = lich.phase() == CryptLichEntity.Phase.WAKING && lich.getHealth() == lich.getMaxHealth()
						&& lich.crown() == CryptLichEntity.Crown.LIT;
				boolean unhurt = !lich.hurtServer(level, level.damageSources().playerAttack(player), 10.0F);
				helper.runAfterDelay(CryptLichEntity.WAKE_TICKS + 15, () -> {
					ending(level, lich, () -> {
						Vec3 woke = lich.woke();
						helper.assertTrue(waited, "He did not wait on his throne, his crown dark");
						helper.assertTrue(harmless, "Waiting, he was harmed");
						helper.assertTrue(slept, "A creative player's blow woke him");
						helper.assertTrue(woken, "A player on the hall's floor did not wake him at full health, his crown lit");
						helper.assertTrue(unhurt, "Waking, he was harmed");
						helper.assertTrue(lich.getMaxHealth() == (float) (CryptLichEntity.HEALTH * CryptLichEntity.partyScale(1)),
								"Woken by one player, his health is " + lich.getMaxHealth());
						helper.assertTrue(lich.phase() == CryptLichEntity.Phase.WARDED_KING, "He did not come down to fight: " + lich.phase());
						// Down for up to 25 ticks by now, he has glided toward the player at most 4 blocks.
						helper.assertTrue(Math.abs(lich.getY() - lich.floorY()) < 0.1 && Math.hypot(lich.getX() - woke.x, lich.getZ() - woke.z) < 6.0,
								"He did not come down to the hall's floor where he wakes but to " + lich.position());
						Rune lit = lich.lit();
						helper.assertTrue(lich.ward() == CryptLichEntity.Ward.CHAINED && lich.warded() && lit != null && lich.order().size() == 3
								&& lit == lich.order().getFirst(), "His chains do not burn with the first name lit: " + lich.ward() + ", " + lit);
						for (Rune rune : Rune.values()) {
							helper.assertTrue(!broken(level, lich, rune), "A seal is broken as he wakes: " + rune);
							helper.assertTrue(glow(level, lich, rune) == (rune == lit ? Glow.LIT : Glow.DARK), "The name " + rune + " is "
									+ glow(level, lich, rune) + " as he wakes");
						}
						helper.assertTrue(!cracked(level, lich), "The phylactery is cracked as he wakes");
						player.discard();
					});
					helper.succeed();
				});
			});
		});
	}

	/**
	 * His health grows by half for each player after the first, at most two and a half times; a player's blow from afar
	 * wakes him, unhurt; Wither and poison never take hold of him. The Last Name makes every cooldown a third shorter,
	 * breaks his chains for good and flares his crown, and a blow hurts him and counts as taking part.
	 */
	@GameTest(structure = HALL, padding = 8)
	public void hisHealthGrowsWithThePartyAndTheLastNameHastensHim(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		BlockPos origin = origin(helper);
		CryptLichEntity lich = waiting(helper, origin);
		ServerPlayer far = player(helper, origin, 32.5, 56.5);
		ServerPlayer other = player(helper, origin, 34.5, 56.5);
		ending(level, lich, () -> {
			helper.assertTrue(CryptLichEntity.partyScale(1) == 1.0 && CryptLichEntity.partyScale(2) == 1.5 && CryptLichEntity.partyScale(4) == 2.5
					&& CryptLichEntity.partyScale(9) == 2.5, "His health scales with the party, at most 2.5 times");
			helper.assertTrue(!lich.canBeAffected(new MobEffectInstance(MobEffects.WITHER, 100))
					&& !lich.canBeAffected(new MobEffectInstance(MobEffects.POISON, 100)), "Wither or poison can take hold of him");
			helper.assertTrue(lich.cooldown(240) == 240, "Before the Last Name his cooldowns are their own");
			boolean blow = lich.hurtServer(level, level.damageSources().playerAttack(far), 10.0F);
			helper.assertTrue(!blow && lich.phase() == CryptLichEntity.Phase.WAKING, "A player's blow from afar did not wake him, unhurt");
			helper.assertTrue(lich.getMaxHealth() == (float) (CryptLichEntity.HEALTH * CryptLichEntity.partyScale(2)),
					"Woken with two players near his hall, his health is " + lich.getMaxHealth());
			lich.lastName(level);
			helper.assertTrue(lich.phase() == CryptLichEntity.Phase.LAST_NAME && lich.ward() == CryptLichEntity.Ward.GONE && !lich.warded()
					&& lich.crown() == CryptLichEntity.Crown.FLARE, "In the Last Name his chains still hold, or his crown does not flare");
			helper.assertTrue(lich.cooldown(240) == 160 && lich.cooldown(CryptLichEntity.GLOBAL_COOLDOWN) == 13,
					"In the Last Name his cooldowns are not a third shorter: " + lich.cooldown(240));
			float before = lich.getHealth();
			boolean hurt = lich.hurtServer(level, level.damageSources().playerAttack(far), 10.0F);
			helper.assertTrue(hurt && lich.getHealth() < before, "In the Last Name a blow did not hurt him");
			helper.assertTrue(lich.participants(level).contains(far) && !lich.participants(level).contains(other),
					"Hurting him did not count as taking part, or standing by did");
			far.discard();
			other.discard();
		});
		helper.succeed();
	}

	/**
	 * His rule, the phylactery: while his chains burn every blow is turned aside. A player's hand on the lit name's seal
	 * breaks it and counts as taking part; the next name lights, and the seals are broken in their order. With all three
	 * broken the phylactery cracks and he is exposed, staggered at first, and a blow hurts him.
	 */
	@GameTest(structure = HALL, padding = 8, maxTicks = 120)
	public void hisChainsTurnBlowsAsideUntilTheLitSealsBreak(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		BlockPos origin = origin(helper);
		archive(level, origin);
		CryptLichEntity lich = waiting(helper, origin);
		ServerPlayer breaker = player(helper, origin, 32.5, 50.5);
		breaker.setGameMode(GameType.CREATIVE);
		lich.wake(level);
		helper.runAfterDelay(AWAKE, () -> {
			ending(level, lich, () -> {
				helper.assertTrue(lich.phase() == CryptLichEntity.Phase.WARDED_KING && lich.warded(), "He is not warded as he fights");
				boolean aside = !lich.hurtServer(level, level.damageSources().playerAttack(breaker), 10.0F) && lich.getHealth() == lich.getMaxHealth();
				helper.assertTrue(aside, "While his chains burn a blow hurt him");
				List<Rune> order = lich.order();
				Rune first = order.get(0);
				InteractionResult used = use(level, breaker, lich.seal(first), Direction.UP);
				helper.assertTrue(used.consumesAction() && broken(level, lich, first) && glow(level, lich, first) == Glow.DARK,
						"A player's hand on the lit name's seal did not break it");
				helper.assertTrue(lich.lit() == order.get(1) && glow(level, lich, order.get(1)) == Glow.LIT, "The next name did not light");
				helper.assertTrue(lich.participants(level).contains(breaker), "Breaking a seal did not count as taking part");
				helper.assertTrue(lich.warded() && !lich.hurtServer(level, level.damageSources().playerAttack(breaker), 10.0F),
						"With a chain left he is not warded");
				lich.breakSeal(level, order.get(1));
				lich.breakSeal(level, order.get(2));
				helper.assertTrue(lich.ward() == CryptLichEntity.Ward.EXPOSED && !lich.warded() && lich.lit() == null,
						"With all three seals broken he is not exposed: " + lich.ward());
				helper.assertTrue(cracked(level, lich), "With all three seals broken the phylactery did not crack");
				helper.assertTrue(lich.staggerTicks() == CryptLichEntity.STAGGER_TICKS && lich.action() == CryptLichEntity.Action.STAGGER,
						"Exposed, he is not staggered: " + lich.staggerTicks());
				boolean hurt = lich.hurtServer(level, level.damageSources().playerAttack(breaker), 10.0F);
				helper.assertTrue(hurt && lich.getHealth() < lich.getMaxHealth(), "Exposed, a blow did not hurt him");
				breaker.discard();
			});
			helper.succeed();
		});
	}

	/**
	 * Raise the Dead: {@value CryptLichEntity#RAISED} Restless Dead climb out of the floor graves nearest his foe. A
	 * player's hand on a dark name's seal holds and raises one from that alcove's sarcophagus. Never more than
	 * {@value CryptLichEntity#DEAD_CAP} walk at once. A Restless Dead is unhurt while it climbs out; afterwards a blow on it
	 * counts as taking part, and when he climbs back to his throne his dead crumble.
	 */
	@GameTest(structure = HALL, padding = 8, maxTicks = 140)
	public void theDeadRiseFromTheGravesAndADarkSeal(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		BlockPos origin = origin(helper);
		archive(level, origin);
		CryptLichEntity lich = waiting(helper, origin);
		ServerPlayer foe = player(helper, origin, 16.5, 18.5);
		foe.setGameMode(GameType.CREATIVE);
		lich.wake(level);
		helper.runAfterDelay(AWAKE, () -> {
			List<Integer> graves = lich.graves(foe.position(), CryptLichEntity.RAISED);
			int raised = lich.raiseAt(level, foe);
			List<RestlessDeadEntity> first = risen(level, lich);
			boolean atGraves = raised == CryptLichEntity.RAISED && graves.equals(List.of(0, 2)) && first.size() == 2
					&& first.get(0).position().distanceTo(CryptArchive.grave(graves.get(0)).add(origin.getX(), origin.getY(), origin.getZ())) < 0.01
					&& first.get(1).position().distanceTo(CryptArchive.grave(graves.get(1)).add(origin.getX(), origin.getY(), origin.getZ())) < 0.01;
			boolean climbing = first.stream().allMatch(RestlessDeadEntity::climbing)
					&& !first.get(0).hurtServer(level, level.damageSources().playerAttack(foe), 5.0F);
			Rune dark = lich.order().get(2);
			InteractionResult used = use(level, foe, lich.seal(dark), Direction.UP);
			Vec3 foot = CryptArchive.sarcophagusDead(dark).add(origin.getX(), origin.getY(), origin.getZ());
			boolean held = used.consumesAction() && !broken(level, lich, dark) && lich.lit() == lich.order().getFirst();
			boolean fromAlcove = lich.dead().size() == 3 && risen(level, lich).stream().anyMatch(dead -> dead.position().distanceTo(foot) < 0.01);
			lich.raise(level, lich.centre().add(4.0, 0.0, 0.0));
			boolean capped = lich.dead().size() == CryptLichEntity.DEAD_CAP && lich.raise(level, lich.centre().add(-4.0, 0.0, 0.0)) == null
					&& lich.raiseAt(level, foe) == 0 && lich.dead().size() == CryptLichEntity.DEAD_CAP;
			helper.runAfterDelay(RestlessDeadEntity.CLIMB + 5, () -> {
				RestlessDeadEntity struck = first.getFirst();
				boolean out = !struck.climbing() && struck.hurtServer(level, level.damageSources().playerAttack(foe), 5.0F);
				boolean part = lich.participants(level).contains(foe);
				List<RestlessDeadEntity> walking = risen(level, lich);
				lich.reset(level);
				ending(level, lich, () -> {
					helper.assertTrue(atGraves, raised + " dead rose at graves " + graves + ", not two at the graves nearest the player");
					helper.assertTrue(climbing, "A Restless Dead was hurt while it climbed out");
					helper.assertTrue(held, "A dark name's seal broke, or the lit name changed");
					helper.assertTrue(fromAlcove, "No Restless Dead climbed out of the dark name's sarcophagus at " + foot);
					helper.assertTrue(capped, "More than " + CryptLichEntity.DEAD_CAP + " dead walk at once: " + lich.dead().size());
					helper.assertTrue(out, "Climbed out, a Restless Dead was not hurt by a blow");
					helper.assertTrue(part, "Striking a Restless Dead did not count as taking part");
					helper.assertTrue(walking.size() == CryptLichEntity.DEAD_CAP && walking.stream().allMatch(RestlessDeadEntity::isRemoved)
							&& lich.dead().isEmpty(), "His dead did not crumble as he climbed back to his throne");
					foe.discard();
				});
				helper.succeed();
			});
		});
	}

	/**
	 * Exposed, he stays so for {@value CryptLichEntity#EXPOSED_TICKS} ticks; then the chains reforge over
	 * {@value CryptLichEntity#REFORGE_TICKS} ticks, turning blows aside again; reforged, every seal is whole, the
	 * phylactery is whole, and a name lights again. A player who may fight him (and is kept from harm) stays near, so he is
	 * not left alone.
	 */
	@GameTest(structure = HALL, padding = 8, maxTicks = 380)
	public void exposedTheChainsReforgeAfterAWhile(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		BlockPos origin = origin(helper);
		archive(level, origin);
		CryptLichEntity lich = waiting(helper, origin);
		ServerPlayer foe = player(helper, origin, 35.5, 44.5);
		foe.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, 400, 4, false, false));
		lich.wake(level);
		boolean[] exposed = {false};
		boolean[] reforging = {false};
		helper.runAfterDelay(AWAKE, () -> {
			for (int i = 0; i < 3; i++) {
				lich.breakSeal(level, lich.lit());
			}
			exposed[0] = lich.ward() == CryptLichEntity.Ward.EXPOSED && cracked(level, lich)
					&& List.of(Rune.values()).stream().allMatch(rune -> broken(level, lich, rune));
		});
		helper.runAfterDelay(AWAKE + CryptLichEntity.EXPOSED_TICKS + 5, () -> {
			reforging[0] = lich.ward() == CryptLichEntity.Ward.REFORGING && lich.warded()
					&& !lich.hurtServer(level, level.damageSources().playerAttack(foe), 10.0F);
		});
		helper.runAfterDelay(AWAKE + CryptLichEntity.EXPOSED_TICKS + CryptLichEntity.REFORGE_TICKS + 8, () -> {
			ending(level, lich, () -> {
				helper.assertTrue(exposed[0], "Breaking the three seals did not expose him with the phylactery cracked");
				helper.assertTrue(reforging[0], "After " + CryptLichEntity.EXPOSED_TICKS + " ticks his chains did not begin to reforge");
				helper.assertTrue(lich.phase() == CryptLichEntity.Phase.WARDED_KING, "He is not fighting on: " + lich.phase());
				helper.assertTrue(lich.ward() == CryptLichEntity.Ward.CHAINED && lich.lit() != null && glow(level, lich, lich.lit()) == Glow.LIT,
						"His chains did not reforge with a name lit: " + lich.ward());
				for (Rune rune : Rune.values()) {
					helper.assertTrue(!broken(level, lich, rune), "Reforged, the seal of " + rune + " is still broken");
				}
				helper.assertTrue(!cracked(level, lich), "Reforged, the phylactery is still cracked");
				foe.discard();
			});
			helper.succeed();
		});
	}

	/**
	 * The Erasure: at half health he shrieks, unhurt; the chains reforge at once, the name that comes last scraped from its
	 * stone; {@value CryptLichEntity#ERASE_TICKS} ticks later the Unwritten begins. When the scraped name's turn comes its
	 * seal holds, until a player reads the names back at the lectern: it burns again, the reader takes part, and its seal
	 * breaks.
	 */
	@GameTest(structure = HALL, padding = 8, maxTicks = 160)
	public void theErasureScrapesANameAndTheLecternReadsItBack(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		BlockPos origin = origin(helper);
		archive(level, origin);
		CryptLichEntity lich = waiting(helper, origin);
		ServerPlayer reader = player(helper, origin, 32.5, 54.5);
		reader.setGameMode(GameType.CREATIVE);
		lich.wake(level);
		helper.runAfterDelay(AWAKE, () -> lich.setHealth(lich.getMaxHealth() * CryptLichEntity.ERASE_AT));
		helper.runAfterDelay(AWAKE + 2, () -> {
			List<Rune> order = lich.order();
			Rune scraped = lich.scraped();
			boolean shrieking = lich.phase() == CryptLichEntity.Phase.ERASURE && lich.action() == CryptLichEntity.Action.SHRIEK;
			boolean scrapedLast = scraped != null && scraped == order.get(2) && glow(level, lich, scraped) == Glow.ERASED;
			boolean reforged = lich.ward() == CryptLichEntity.Ward.CHAINED && lich.lit() == order.get(0) && glow(level, lich, order.get(0)) == Glow.LIT;
			boolean unhurt = !lich.hurtServer(level, level.damageSources().playerAttack(reader), 10.0F);
			helper.runAfterDelay(CryptLichEntity.ERASE_TICKS + 3, () -> {
				boolean unwritten = lich.phase() == CryptLichEntity.Phase.UNWRITTEN;
				lich.breakSeal(level, order.get(0));
				lich.breakSeal(level, order.get(1));
				boolean turn = lich.lit() == scraped && glow(level, lich, scraped) == Glow.ERASED;
				InteractionResult tried = use(level, reader, lich.seal(scraped), Direction.UP);
				boolean holds = tried.consumesAction() && !broken(level, lich, scraped) && lich.warded();
				InteractionResult read = use(level, reader, lich.lectern(), Direction.SOUTH);
				boolean restored = read.consumesAction() && lich.scraped() == null && glow(level, lich, scraped) == Glow.LIT;
				boolean answered = lich.sealUsed(level, lich.seal(scraped), level.getBlockState(lich.seal(scraped)), reader);
				ending(level, lich, () -> {
					helper.assertTrue(shrieking, "At half health he did not shriek the Erasure");
					helper.assertTrue(scrapedLast, "The name that comes last was not scraped from its stone: " + scraped + " of " + order);
					helper.assertTrue(reforged, "The chains did not reforge at once with the first name lit");
					helper.assertTrue(unhurt, "Shrieking the Erasure, he was harmed");
					helper.assertTrue(unwritten, "The Unwritten did not begin after " + CryptLichEntity.ERASE_TICKS + " ticks");
					helper.assertTrue(turn, "When its turn came the scraped name was not lit, blank");
					helper.assertTrue(holds, "The scraped name's seal broke before it was read back");
					helper.assertTrue(restored, "Reading the names back at the lectern did not cut the scraped name again");
					helper.assertTrue(lich.participants(level).contains(reader), "Reading the names back did not count as taking part");
					helper.assertTrue(answered && broken(level, lich, scraped) && lich.ward() == CryptLichEntity.Ward.EXPOSED,
							"Read back, the name's seal did not break and expose him");
					reader.discard();
				});
				helper.succeed();
			});
		});
	}

	/**
	 * A scraped name left lit for {@value CryptLichEntity#SCRAPED_TICKS} ticks burns back by itself. At a fifth of his
	 * health the Last Name begins: his phylactery shatters, every name goes dark and his chains are gone for good.
	 */
	@GameTest(structure = HALL, padding = 8, maxTicks = 160)
	public void aScrapedNameBurnsBackAndTheLastNameBreaksTheChains(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		BlockPos origin = origin(helper);
		archive(level, origin);
		CryptLichEntity lich = waiting(helper, origin);
		ServerPlayer striker = player(helper, origin, 32.5, 54.5);
		striker.setGameMode(GameType.CREATIVE);
		lich.wake(level);
		helper.runAfterDelay(AWAKE, () -> lich.erase(level));
		helper.runAfterDelay(AWAKE + CryptLichEntity.ERASE_TICKS + 3, () -> {
			List<Rune> order = lich.order();
			Rune scraped = lich.scraped();
			lich.breakSeal(level, order.get(0));
			lich.breakSeal(level, order.get(1));
			boolean waits = lich.lit() == scraped && glow(level, lich, scraped) == Glow.ERASED;
			lich.scrapedSince(level.getGameTime() - CryptLichEntity.SCRAPED_TICKS - 1);
			helper.runAfterDelay(2, () -> {
				boolean burnt = lich.scraped() == null && lich.lit() == scraped && glow(level, lich, scraped) == Glow.LIT;
				lich.setHealth(lich.getMaxHealth() * CryptLichEntity.LAST_AT);
				helper.runAfterDelay(2, () -> {
					ending(level, lich, () -> {
						helper.assertTrue(waits, "The scraped name was not lit, blank, in its turn");
						helper.assertTrue(burnt, "Left lit " + CryptLichEntity.SCRAPED_TICKS + " ticks, the scraped name did not burn back");
						helper.assertTrue(lich.phase() == CryptLichEntity.Phase.LAST_NAME && lich.ward() == CryptLichEntity.Ward.GONE,
								"At a fifth of his health the Last Name did not begin: " + lich.phase() + ", " + lich.ward());
						helper.assertTrue(cracked(level, lich) && lich.crown() == CryptLichEntity.Crown.FLARE,
								"In the Last Name the phylactery is whole, or his crown does not flare");
						for (Rune rune : Rune.values()) {
							helper.assertTrue(glow(level, lich, rune) == Glow.DARK, "In the Last Name the name " + rune + " still burns");
						}
						float before = lich.getHealth();
						helper.assertTrue(lich.hurtServer(level, level.damageSources().playerAttack(striker), 4.0F) && lich.getHealth() < before,
								"In the Last Name a blow did not hurt him");
						striker.discard();
					});
					helper.succeed();
				});
			});
		});
	}

	/**
	 * His Grave Sweep strikes a player before him and one forty-five degrees aside, throwing them back, and spares the one
	 * beside him, the one behind him and one out of reach. They wait out their loading in creative, where he pays them no
	 * heed.
	 */
	@GameTest(structure = HALL, padding = 8, maxTicks = 180)
	public void hisGraveSweepStrikesBeforeHimAndThrowsBack(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		BlockPos origin = origin(helper);
		CryptLichEntity lich = waiting(helper, origin);
		double aside = 2.5 * Math.sin(Math.toRadians(45.0));
		ServerPlayer front = player(helper, origin, 32.5, 27.0);
		ServerPlayer slant = player(helper, origin, 32.5 + aside, 24.5 + aside);
		ServerPlayer beside = player(helper, origin, 30.0, 24.5);
		ServerPlayer behind = player(helper, origin, 32.5, 22.0);
		ServerPlayer far = player(helper, origin, 32.5, 29.5);
		List<ServerPlayer> players = List.of(front, slant, beside, behind, far);
		players.forEach(player -> player.setGameMode(GameType.CREATIVE));
		helper.runAfterDelay(LOADING_TICKS, () -> {
			players.forEach(player -> player.setGameMode(GameType.SURVIVAL));
			ending(level, lich, () -> {
				stand(lich, 32.5, 24.5);  // facing south, towards the one in front
				int hit = lich.sweep(level);
				helper.assertTrue(hit == 2, "The sweep struck " + hit + " players, not 2");
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
	 * His Bone Lanes: he calls the names whose lanes lie nearest his foe (one while his chains hold, two after); bones burst
	 * up each called lane, striking and tossing up a player on it, sparing one between the lanes and one on a lane not
	 * called. In the fight, as the wind-up ends a clump of bones bursts every {@value CryptLichEntity#CLUMP_STEP} blocks
	 * down the called lane, and once they have stood and sunk they are gone.
	 */
	@GameTest(structure = HALL, padding = 8, maxTicks = 220)
	public void hisBoneLanesBurstUpTheCalledLanes(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		BlockPos origin = origin(helper);
		archive(level, origin);
		CryptLichEntity lich = waiting(helper, origin);
		ServerPlayer west = player(helper, origin, 20.5, 34.5);
		ServerPlayer north = player(helper, origin, 32.5, 22.5);
		ServerPlayer east = player(helper, origin, 44.5, 34.5);
		ServerPlayer between = player(helper, origin, 20.5, 30.5);
		List<ServerPlayer> players = List.of(west, north, east, between);
		players.forEach(player -> player.setGameMode(GameType.CREATIVE));
		lich.wake(level);
		CryptLichEntity.Attack lanes = CryptLichEntity.Attack.BONE_LANES;
		helper.runAfterDelay(LOADING_TICKS, () -> {
			players.forEach(player -> player.setGameMode(GameType.SURVIVAL));
			lich.callLanes(level, west, 2);
			List<Rune> called = lich.lanes();
			int hit = lich.bones(level);
			helper.assertTrue(called.equals(List.of(Rune.SPADE, Rune.CROWN)), "He did not call the two lanes nearest the player: " + called);
			helper.assertTrue(hit == 2, "The bones struck " + hit + " players, not 2");
			for (ServerPlayer struck : List.of(west, north)) {
				helper.assertTrue(struck.getHealth() < struck.getMaxHealth() && Math.abs(struck.getDeltaMovement().y - CryptLichEntity.LANE_TOSS) < 1.0E-6,
						"A player on a called lane was not struck and tossed up");
			}
			for (ServerPlayer spared : List.of(east, between)) {
				helper.assertTrue(spared.getHealth() == spared.getMaxHealth(), "A player between the lanes or on a lane not called was struck");
			}
			// Kept from harm while the bones burst in the fight.
			players.forEach(player -> player.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, 400, 4, false, false)));
			lich.begin(level, lanes, west);
			boolean one = lich.lanes().equals(List.of(Rune.SPADE));
			helper.runAfterDelay(lanes.windup + 2, () -> {
				List<LichBonesEntity> clumps = level.getEntitiesOfClass(LichBonesEntity.class, hall(origin));
				double[] line = CryptArchive.laneLine(Rune.SPADE);
				int expected = (int) Math.floor(Math.hypot(line[2] - line[0], line[3] - line[1]) / CryptLichEntity.CLUMP_STEP + 1.0E-6) + 1;
				helper.assertTrue(one, "While his chains hold he called " + lich.lanes() + ", not the one lane nearest");
				helper.assertTrue(clumps.size() == expected, clumps.size() + " clumps of bones burst, not " + expected);
				helper.assertTrue(clumps.stream().allMatch(clump -> Math.abs(clump.getZ() - (origin.getZ() + line[1])) < 0.01
						&& Math.abs(clump.getY() - lich.floorY()) < 0.01), "The bones did not burst down the middle of the called lane");
			});
			helper.runAfterDelay(lanes.windup + lanes.active + LichBonesEntity.SINK + 6, () -> {
				ending(level, lich, () -> {
					helper.assertTrue(level.getEntitiesOfClass(LichBonesEntity.class, hall(origin)).isEmpty(), "His bones did not sink and go");
					players.forEach(ServerPlayer::discard);
				});
				helper.succeed();
			});
		});
	}

	/**
	 * His Soul Bolt drifts from his hand at the player and strikes them, and Wither takes hold. Struck back by a player, a
	 * bolt turns and flies at him: while his chains hold it staggers him for {@value CryptLichEntity#BOLT_STAGGER} ticks,
	 * unhurt, and counts as taking part; exposed, it hurts him.
	 */
	@GameTest(structure = HALL, padding = 8, maxTicks = 260)
	public void hisSoulBoltWithersAndStruckBackStaggersOrHurtsHim(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		BlockPos origin = origin(helper);
		archive(level, origin);
		CryptLichEntity lich = waiting(helper, origin);
		// Over the hall's floor (not standing on it), so it does not wake him: he looses the bolt from his throne.
		ServerPlayer mark = player(helper, origin, 32.5, 6.0, 20.5);
		ServerPlayer reflector = player(helper, origin, 32.5, 24.5);
		mark.setGameMode(GameType.CREATIVE);
		reflector.setGameMode(GameType.CREATIVE);
		SoulBoltEntity[] bolts = new SoulBoltEntity[2];
		helper.runAfterDelay(LOADING_TICKS, () -> {
			mark.setGameMode(GameType.SURVIVAL);
			bolts[0] = SoulBoltEntity.loose(level, lich, mark);
		});
		helper.runAfterDelay(LOADING_TICKS + 45, () -> {
			boolean struck = bolts[0].isRemoved() && mark.getHealth() < mark.getMaxHealth() && mark.hasEffect(MobEffects.WITHER);
			helper.assertTrue(struck, "The Soul Bolt did not strike the player and wither them");
			mark.setGameMode(GameType.CREATIVE);
			lich.wake(level);
		});
		helper.runAfterDelay(LOADING_TICKS + 45 + AWAKE, () -> {
			stand(lich, 32.5, 18.5);
			bolts[1] = SoulBoltEntity.loose(level, lich, reflector);
			boolean turned = bolts[1].hurtServer(level, level.damageSources().playerAttack(reflector), 1.0F) && bolts[1].reflected();
			helper.assertTrue(turned, "Struck by a player, the bolt did not turn");
		});
		helper.runAfterDelay(LOADING_TICKS + 45 + AWAKE + 4, () -> {
			ending(level, lich, () -> {
				helper.assertTrue(bolts[1].isRemoved(), "The struck-back bolt did not reach him");
				helper.assertTrue(lich.staggerTicks() == CryptLichEntity.BOLT_STAGGER && lich.action() == CryptLichEntity.Action.STAGGER
						&& lich.getHealth() == lich.getMaxHealth(), "Warded, the struck-back bolt did not stagger him, unhurt: " + lich.staggerTicks());
				helper.assertTrue(lich.participants(level).contains(reflector), "Striking his bolt back did not count as taking part");
				lich.expose(level);
				SoulBoltEntity bolt = SoulBoltEntity.loose(level, lich, reflector);
				boolean hurt = lich.boltReturned(level, bolt, reflector);
				bolt.discard();
				helper.assertTrue(hurt && lich.getHealth() < lich.getMaxHealth(), "Exposed, the struck-back bolt did not hurt him");
				mark.discard();
				reflector.discard();
			});
			helper.succeed();
		});
	}

	/**
	 * His Soul Drain takes health from the player and heals him as much; a pillar between them breaks the beam and the
	 * attack ends.
	 */
	@GameTest(structure = HALL, padding = 8, maxTicks = 140)
	public void hisSoulDrainHealsHimAndAPillarBreaksIt(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		BlockPos origin = origin(helper);
		archive(level, origin);
		CryptLichEntity lich = waiting(helper, origin);
		ServerPlayer drained = player(helper, origin, 32.5, 28.5);
		drained.setGameMode(GameType.CREATIVE);
		lich.wake(level);
		helper.runAfterDelay(LOADING_TICKS, () -> {
			drained.setGameMode(GameType.SURVIVAL);
			stand(lich, 32.5, 20.5);
			lich.setHealth(lich.getMaxHealth() - 10.0F);
			float before = lich.getHealth();
			boolean seen = lich.sees(level, drained);
			float took = lich.drainOnce(level, drained);
			boolean healed = took > 0.0F && Math.abs(lich.getHealth() - (before + took)) < 1.0E-3 && drained.getHealth() < drained.getMaxHealth();
			for (int y = 0; y <= 2; y++) {
				level.setBlock(origin.offset(32, CryptArchive.STAND + y, 24), Blocks.DEEPSLATE_TILES.defaultBlockState(), Block.UPDATE_CLIENTS);
			}
			boolean hidden = !lich.sees(level, drained);
			lich.begin(level, CryptLichEntity.Attack.SOUL_DRAIN, drained);
			boolean began = lich.attack() == CryptLichEntity.Attack.SOUL_DRAIN;
			helper.runAfterDelay(3, () -> {
				ending(level, lich, () -> {
					helper.assertTrue(seen, "With nothing between them he does not see the player");
					helper.assertTrue(healed, "The Soul Drain took " + took + " and did not heal him as much");
					helper.assertTrue(hidden, "With a pillar between them he still sees the player");
					helper.assertTrue(began && lich.attack() == null && lich.action() != CryptLichEntity.Action.DRAIN,
							"The pillar did not break the Soul Drain: " + lich.attack());
					drained.discard();
				});
				helper.succeed();
			});
		});
	}

	/**
	 * His Grave Pull drags every player within {@value CryptLichEntity#PULL_RADIUS} blocks of him toward him but the one in
	 * the ring's dark part, and spares one out of reach; then bones burst round his feet, striking whoever is within
	 * {@value CryptLichEntity#PULL_REACH} blocks.
	 */
	@GameTest(structure = HALL, padding = 8, maxTicks = 180)
	public void hisGravePullSparesTheDarkPartAndBurstsAtHisFeet(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		BlockPos origin = origin(helper);
		CryptLichEntity lich = waiting(helper, origin);
		ServerPlayer north = player(helper, origin, 32.5, 20.5);
		ServerPlayer close = player(helper, origin, 30.5, 24.5);
		ServerPlayer dark = player(helper, origin, 36.5, 24.5);
		ServerPlayer far = player(helper, origin, 32.5, 31.5);
		List<ServerPlayer> players = List.of(north, close, dark, far);
		players.forEach(player -> player.setGameMode(GameType.CREATIVE));
		helper.runAfterDelay(LOADING_TICKS, () -> {
			players.forEach(player -> player.setGameMode(GameType.SURVIVAL));
			ending(level, lich, () -> {
				stand(lich, 32.5, 24.5);
				lich.pullDark(0.0);  // its dark part due east of him
				helper.assertTrue(lich.inDark(0.0) && lich.inDark(30.0) && lich.inDark(-30.0) && !lich.inDark(31.0) && !lich.inDark(180.0),
						"The ring's dark part is not the " + CryptLichEntity.PULL_DARK + " degrees round east");
				int dragged = lich.pull(level);
				helper.assertTrue(dragged == 2, "The pull dragged " + dragged + " players, not 2");
				helper.assertTrue(north.getDeltaMovement().z > 0.0 && close.getDeltaMovement().x > 0.0, "A player in the ring was not dragged toward him");
				helper.assertTrue(dark.getDeltaMovement().horizontalDistance() < 1.0E-6 && far.getDeltaMovement().horizontalDistance() < 1.0E-6,
						"The player in the dark part, or out of reach, was dragged");
				int hit = lich.pullBurst(level);
				helper.assertTrue(hit == 1 && close.getHealth() < close.getMaxHealth(), "The bones at his feet struck " + hit + " players, not the one close");
				for (ServerPlayer spared : List.of(north, dark, far)) {
					helper.assertTrue(spared.getHealth() == spared.getMaxHealth(), "A player out of the bones' reach was struck");
				}
				helper.assertTrue(level.getEntitiesOfClass(LichBonesEntity.class, hall(origin)).size() == 6, "Six clumps of bones did not burst round his feet");
				players.forEach(ServerPlayer::discard);
			});
			helper.succeed();
		});
	}

	/** Strayed farther than {@value CryptLichEntity#LEASH} blocks from his dais, he is drawn back to it. */
	@GameTest(structure = HALL, padding = 8, maxTicks = 100)
	public void heKeepsToHisHall(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		BlockPos origin = origin(helper);
		archive(level, origin);
		CryptLichEntity lich = waiting(helper, origin);
		lich.wake(level);
		helper.runAfterDelay(AWAKE, () -> {
			Vec3 centre = lich.centre();
			lich.teleportTo(centre.x + CryptLichEntity.LEASH + 5.0, centre.y, centre.z);
			helper.runAfterDelay(2, () -> {
				ending(level, lich, () -> helper.assertTrue(lich.position().distanceTo(centre) <= CryptLichEntity.LEASH,
						"Strayed from his dais, he did not glide back: " + lich.position()));
				helper.succeed();
			});
		});
	}

	/**
	 * Left alone {@value CryptLichEntity#ABANDON_TICKS} ticks, he climbs back to his throne, healed: his dead crumble, the
	 * seals mend, the names go dark, the phylactery is whole, and who hurt him is forgotten.
	 */
	@GameTest(structure = HALL, padding = 8, maxTicks = 320)
	public void leftAloneHeClimbsBackToHisThrone(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		BlockPos origin = origin(helper);
		archive(level, origin);
		CryptLichEntity lich = waiting(helper, origin);
		ServerPlayer gone = player(helper, origin, 32.5, 60.5);
		gone.setGameMode(GameType.CREATIVE);
		lich.wake(level);
		helper.runAfterDelay(AWAKE, () -> {
			lich.took(gone, 5.0F);
			lich.setHealth(lich.getMaxHealth() * 0.8F);
			lich.breakSeal(level, lich.lit());
			RestlessDeadEntity risen = lich.raise(level, lich.centre().add(4.0, 0.0, 4.0));
			boolean counted = lich.participants(level).contains(gone);
			helper.runAfterDelay(CryptLichEntity.ABANDON_TICKS + 20, () -> {
				ending(level, lich, () -> {
					helper.assertTrue(counted, "Whoever hurt him was not counted");
					helper.assertTrue(lich.phase() == CryptLichEntity.Phase.WAITING && lich.position().distanceTo(lich.waiting()) < 0.1,
							"Left alone he did not climb back to his throne: " + lich.phase() + " at " + lich.position());
					helper.assertTrue(lich.getHealth() == lich.getMaxHealth() && lich.crown() == CryptLichEntity.Crown.DARK,
							"Back on his throne he is not healed, or his crown burns");
					for (Rune rune : Rune.values()) {
						helper.assertTrue(!broken(level, lich, rune) && glow(level, lich, rune) == Glow.DARK, "Back on his throne, the seal or name of "
								+ rune + " is not mended and dark");
					}
					helper.assertTrue(!cracked(level, lich), "Back on his throne, the phylactery is cracked");
					helper.assertTrue(risen != null && risen.isRemoved() && lich.dead().isEmpty(), "His dead outlived his climbing back");
					helper.assertTrue(lich.participants(level).isEmpty(), "Who hurt him was not forgotten");
					gone.discard();
				});
				helper.succeed();
			});
		});
	}

	/**
	 * His loot is each participant's own: everyone who struck him, and a reader who only read the names back, rolls for
	 * themselves (Grave Dust, and on a first kill always one of his trophies) into their own inventory with a share of his
	 * experience, and a bystander who took no part gets nothing. Afterwards Nameless is theirs, and a later roll no longer
	 * counts as a first kill. As he falls his crown goes dark and, with no lair instance, the phylactery is left cracked.
	 */
	@GameTest(structure = HALL, padding = 8, maxTicks = 60)
	public void hisLootIsEachParticipantsOwn(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		BlockPos origin = origin(helper);
		archive(level, origin);
		CryptLichEntity lich = waiting(helper, origin);
		ServerPlayer first = player(helper, origin, 30.5, 40.5);
		ServerPlayer second = player(helper, origin, 34.5, 40.5);
		ServerPlayer reader = player(helper, origin, 32.5, 54.5);
		ServerPlayer bystander = player(helper, origin, 32.5, 44.5);
		lich.took(first, 10.0F);
		lich.took(second, 4.0F);
		lich.lecternRead(level, reader, List.of());
		ending(level, lich, () -> {
			List<ServerPlayer> participants = lich.participants(level);
			helper.assertTrue(participants.contains(first) && participants.contains(second) && participants.contains(reader),
					"Both who struck him and the reader take part");
			helper.assertFalse(participants.contains(bystander), "A bystander who took no part does");
			helper.assertTrue(CryptLichLoot.firstKill(first), "A newcomer has already won Nameless");
			List<ItemStack> loot = CryptLichLoot.roll(level, lich, first);
			int dust = loot.stream().filter(stack -> stack.is(JugcraftLich.GRAVE_DUST)).mapToInt(ItemStack::getCount).sum();
			helper.assertTrue(dust >= 3 && dust <= 6, dust + " Grave Dust, not 3 to 6");
			helper.assertTrue(loot.stream().filter(CryptLichLoot::trophy).count() == 1, "A first kill always brings one of his trophies");
			lich.hurtServer(level, level.damageSources().genericKill(), Float.MAX_VALUE);
			helper.assertTrue(lich.isDeadOrDying() && lich.crown() == CryptLichEntity.Crown.DARK, "He did not fall with his crown going dark");
			helper.assertTrue(cracked(level, lich), "With no lair instance the phylactery is not left cracked");
			int share = CryptLichEntity.EXPERIENCE / 3;
			for (ServerPlayer player : List.of(first, second, reader)) {
				helper.assertTrue(player.getInventory().countItem(JugcraftLich.GRAVE_DUST) >= 3, "A participant's loot did not go into their inventory");
				int trophies = player.getInventory().countItem(JugcraftLich.soulreaver()) + player.getInventory().countItem(JugcraftLich.gravewarden());
				helper.assertTrue(trophies == 1, "A first kill brought " + trophies + " of his trophies, not 1");
				helper.assertTrue(player.totalExperience >= share, "A participant got " + player.totalExperience + " experience, not " + share);
				helper.assertFalse(CryptLichLoot.firstKill(player), "Nameless is not theirs");
			}
			helper.assertTrue(bystander.getInventory().countItem(JugcraftLich.GRAVE_DUST) == 0 && bystander.totalExperience == 0, "The bystander got loot");
			helper.assertTrue(CryptLichLoot.firstKill(bystander), "The bystander won Nameless");
			for (ServerPlayer player : List.of(first, second, reader, bystander)) {
				player.discard();
			}
		});
		helper.succeed();
	}

	/**
	 * The Emptied Phylactery held in the offhand: Wither neither hurts its holder nor takes hold of them (the server takes
	 * it off on its tick), while other harm still does; put away, Wither hurts again.
	 */
	@GameTest(maxTicks = 120)
	public void theEmptiedPhylacteryKeepsWitherOff(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		ServerPlayer holder = helper.makeMockServerPlayerInLevel();
		holder.setGameMode(GameType.SURVIVAL);
		BlockPos at = helper.absolutePos(new BlockPos(1, 1, 1));
		holder.snapTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, 0.0F, 0.0F);
		helper.runAfterDelay(LOADING_TICKS, () -> {
			boolean bare = !EmptiedPhylactery.holds(holder);
			holder.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(JugcraftLich.EMPTIED_PHYLACTERY));
			boolean held = EmptiedPhylactery.holds(holder);
			boolean spared = !holder.hurtServer(level, withering(level), 2.0F) && holder.getHealth() == holder.getMaxHealth();
			holder.addEffect(new MobEffectInstance(MobEffects.WITHER, 200, 0));
			helper.runAfterDelay(3, () -> {
				boolean cleansed = !holder.hasEffect(MobEffects.WITHER);
				boolean other = holder.hurtServer(level, level.damageSources().magic(), 1.0F) && holder.getHealth() < holder.getMaxHealth();
				holder.setItemInHand(InteractionHand.OFF_HAND, ItemStack.EMPTY);
				helper.runAfterDelay(HURT_COOLDOWN, () -> {
					float before = holder.getHealth();
					boolean withered = holder.hurtServer(level, withering(level), 2.0F) && holder.getHealth() < before;
					helper.assertTrue(bare, "With nothing in the offhand, the phylactery is held");
					helper.assertTrue(held, "In the offhand, the phylactery is not held");
					helper.assertTrue(spared, "Held, the phylactery did not keep Wither's harm off");
					helper.assertTrue(cleansed, "Held, the phylactery did not take Wither off on the server's tick");
					helper.assertTrue(other, "Held, the phylactery kept other harm off too");
					helper.assertTrue(withered, "Put away, the phylactery still keeps Wither off");
					holder.discard();
					helper.succeed();
				});
			});
		});
	}
}
