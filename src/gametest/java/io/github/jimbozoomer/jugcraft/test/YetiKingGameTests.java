package io.github.jimbozoomer.jugcraft.test;

import com.mojang.authlib.GameProfile;
import com.mojang.serialization.JsonOps;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.agriculture.Knitting;
import io.github.jimbozoomer.jugcraft.agriculture.Knitwear;
import io.github.jimbozoomer.jugcraft.agriculture.TrickOrTreat;
import io.github.jimbozoomer.jugcraft.concordance.ConcordanceCodecs;
import io.github.jimbozoomer.jugcraft.concordance.ConcordanceData;
import io.github.jimbozoomer.jugcraft.concordance.ConcordanceEffects;
import io.github.jimbozoomer.jugcraft.concordance.ConcordanceProgress;
import io.github.jimbozoomer.jugcraft.concordance.ConcordanceSpells;
import io.github.jimbozoomer.jugcraft.concordance.JugcraftConcordance;
import io.github.jimbozoomer.jugcraft.concordance.RateGate;
import io.github.jimbozoomer.jugcraft.concordance.balance.Benchmark;
import io.github.jimbozoomer.jugcraft.concordance.effect.Cause;
import io.github.jimbozoomer.jugcraft.concordance.effect.EffectKind;
import io.github.jimbozoomer.jugcraft.concordance.effect.EffectSpec;
import io.github.jimbozoomer.jugcraft.concordance.effect.Intent;
import io.github.jimbozoomer.jugcraft.concordance.effect.Ledger;
import io.github.jimbozoomer.jugcraft.concordance.rime.Rime;
import io.github.jimbozoomer.jugcraft.concordance.rime.RimeGear;
import io.github.jimbozoomer.jugcraft.concordance.rime.Rimebind;
import io.github.jimbozoomer.jugcraft.concordance.rules.FocusPool;
import io.github.jimbozoomer.jugcraft.concordance.rules.Knowledge;
import io.github.jimbozoomer.jugcraft.concordance.rules.ResearchState;
import io.github.jimbozoomer.jugcraft.concordance.strata.Brace;
import io.github.jimbozoomer.jugcraft.config.JugcraftConfig;
import io.github.jimbozoomer.jugcraft.guns.GunShots;
import io.github.jimbozoomer.jugcraft.guns.JugcraftGuns;
import io.github.jimbozoomer.jugcraft.lair.Encounter;
import io.github.jimbozoomer.jugcraft.lair.JugcraftLairs;
import io.github.jimbozoomer.jugcraft.lair.Lair;
import io.github.jimbozoomer.jugcraft.lair.LairRewards;
import io.github.jimbozoomer.jugcraft.lair.Lairs;
import io.github.jimbozoomer.jugcraft.lair.yeti.FallingIcicleEntity;
import io.github.jimbozoomer.jugcraft.lair.yeti.GlacialSpikeEntity;
import io.github.jimbozoomer.jugcraft.lair.yeti.GlacierHall;
import io.github.jimbozoomer.jugcraft.lair.yeti.HurledBoulderEntity;
import io.github.jimbozoomer.jugcraft.lair.yeti.JugcraftYeti;
import io.github.jimbozoomer.jugcraft.lair.yeti.YetiKingEntity;
import io.github.jimbozoomer.jugcraft.lair.yeti.YetiKingLoot;
import io.github.jimbozoomer.jugcraft.lair.yeti.YetiWhelpEntity;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.IntStream;
import net.fabricmc.fabric.api.entity.FakePlayer;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.RangedAttribute;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.spell_engine.api.spell.registry.SpellRegistry;
import net.spell_engine.internals.SpellExecution;
import net.spell_engine.internals.casting.SpellCast;
import net.spell_engine.internals.casting.SpellCaster;
import net.spell_engine.internals.container.SpellContainerSource;
import net.spell_engine.internals.target.SpellTarget;

/**
 * The Yeti King (docs/features/yeti-king.md), as far as a game-test server can show him: one put down on his own (with no
 * lair instance, over a hall laid out from the corner of an arena the hall's size, so his throne, his lake and its dens
 * are all loaded) waits on his throne until a player's blow wakes him, and leaps down onto his lake; his health grows
 * with the party, and the Fury of the Peaks shortens his cooldowns and widens his slam; Maul Swipe strikes in front and
 * not behind; his Ground Slam bares the drift snow round where he lands and never the trampled, and the snow drifts
 * back; an Avalanche Charge into an ice column stuns him, and stunned he takes a third more damage; his kin come out of
 * the dens, never more than four at once, and flee when he is left alone; his frost, and the Yeti Mitten against it; a
 * glacial spike throws a player up and an icicle shatters on its mark; and his loot is each participant's own. His fight
 * in the Glacier Hall itself runs in a real world, in {@link YetiKingClientGameTests}. Since Concordance Chunk B (B-01)
 * his fight's credit is the lairs' shared one: support and the absent share his loot once, a killing blow alone is paid,
 * a tamed creature's blow is its owner's, a charge baited into a column is each baiter's deed but no idle late
 * arrival's, four players are each paid once, and a fight loaded awake keeps a fresh record. Since B-02 his loot is what
 * his JEI pages say. Since B-04, his fight held to the completion evidence: the charge's column reach, its stops at the
 * dais, the ramp and a den wall, at his leash and when it grazes a column's foot (a run clear of the foot goes on), and
 * its fury chain; solo melee, a solo caster and a party of two solving the column loop; his lake as laid after a slam
 * and his reset, after his death and loaded again; his ice and kin gone on reset, death and unload; wool and leather
 * against his frost; and End-gun Levitation lifting neither him nor his kin. Since B-09 (unit 5): only a Brace lessens his
 * push, once a tick, and a friend's Brace is its caster's deed (not one's own, the bangle's or a forgotten caster's);
 * Rimebind holds one wind-up RIMEBOUND_WINDUP ticks longer once a thaw, and does not slow his steps; a column is
 * Rimekeeping's elective cold work for its baiter, the one kind it shares with a Frost boon arm's slow (B-P56).
 *
 * <p>He fights any player within 38 blocks of his lake's centre; the arena keeps every other test's players farther off
 * than that. Each test sends him away before it ends, with his whelps, and lets the snow drift back.
 */
public class YetiKingGameTests {
	/** An arena of air the hall's size (80 x 88), so everything of his fight is loaded and ticking. */
	private static final String HALL = "jugcraft-test:yeti_hall";
	/**
	 * A new player cannot be hurt until its client has loaded the world, or for 60 ticks; a mock player has no client, so
	 * a test that hurts one waits this long first.
	 */
	private static final int LOADING_TICKS = 80;
	/** How many shares a page test rolls: each count then lies far inside five standard deviations of its chance's mean. */
	private static final int SHARES = 400;

	/** Whether {@code count} of {@value #SHARES} shares lies within five standard deviations of {@code chance}'s mean. */
	private static boolean within(int count, double chance) {
		double mean = SHARES * chance;
		double deviation = 5.0 * Math.sqrt(SHARES * chance * (1.0 - chance));
		return count >= mean - deviation && count <= mean + deviation;
	}

	/** The origin of a hall laid out from the arena's corner, its lake's surface a block over the arena's floor. */
	private static BlockPos origin(GameTestHelper helper) {
		return helper.absolutePos(new BlockPos(0, -GlacierHall.LAKE, 0));
	}

	/** He waits on the throne of the hall laid out from {@code origin}. */
	private static YetiKingEntity waiting(GameTestHelper helper, BlockPos origin) {
		return YetiKingEntity.summon(helper.getLevel(), origin, null);
	}

	/** Stone at the lake's layer under the template's cells {@code x0..x1}, {@code z0..z1}, for players to stand on. */
	private static void floor(ServerLevel level, BlockPos origin, int x0, int z0, int x1, int z1) {
		for (int x = x0; x <= x1; x++) {
			for (int z = z0; z <= z1; z++) {
				level.setBlockAndUpdate(origin.offset(x, GlacierHall.LAKE, z), Blocks.STONE.defaultBlockState());
			}
		}
	}

	/** A survival player standing {@code dx}, {@code dz} from his lake's centre, on it. */
	private static ServerPlayer player(GameTestHelper helper, YetiKingEntity king, double dx, double dz) {
		ServerPlayer player = helper.makeMockServerPlayerInLevel();
		player.setGameMode(GameType.SURVIVAL);
		Vec3 at = king.centre().add(dx, 0.0, dz);
		player.snapTo(at.x, at.y, at.z, 0.0F, 0.0F);
		return player;
	}

	/** Checks {@code body}, then sends him away whether it passed or failed, so a failed test leaves no king fighting on. */
	private static void ending(ServerLevel level, YetiKingEntity king, Runnable body) {
		try {
			body.run();
		} finally {
			for (UUID id : king.whelps()) {
				if (level.getEntity(id) instanceof YetiWhelpEntity whelp) {
					whelp.discard();
				}
			}
			king.driftAll(level);
			king.discard();
		}
	}

	private static final YetiKingEntity.Attack CHARGE = YetiKingEntity.Attack.AVALANCHE_CHARGE;
	/** From this tick of a charge its run is over (its full line, his leash or a wall) and he is recovering. */
	private static final int RUN = CHARGE.windup + CHARGE.active;
	private static final double EPS = 1.0E-3;
	/** Nothing of the hall but a column stands this near a column's centre at his height (tools/glacier_hall.py). */
	private static final double NEAR_COLUMN = 7.0;
	/** How far each way from his lake's centre a test's lake is laid: past his fury's ring. */
	private static final int LAID = 12;
	/** Vanilla's frost slowness (mc263src LivingEntity.java:203,534-544). */
	private static final Identifier FROST_SLOW = Identifier.withDefaultNamespace("powder_snow");

	/** A mock player of {@code mode} at template (x, y, z) of the hall laid out from {@code origin} (y 5 stands on the lake). */
	private static ServerPlayer foeAt(GameTestHelper helper, BlockPos origin, double x, double y, double z, GameType mode) {
		ServerPlayer player = helper.makeMockServerPlayerInLevel();
		player.setGameMode(mode);
		player.snapTo(origin.getX() + x, origin.getY() + y, origin.getZ() + z, 0.0F, 0.0F);
		return player;
	}

	/** Puts him at template (x, z) on his lake and begins his Avalanche Charge at {@code foe} (its wind-up first). */
	private static void charge(ServerLevel level, YetiKingEntity king, double x, double z, ServerPlayer foe) {
		BlockPos o = king.origin();
		king.teleportTo(o.getX() + x, king.floorY(), o.getZ() + z);
		king.begin(level, CHARGE, foe);
	}

	private static boolean isAt(YetiKingEntity king, double x, double z) {
		BlockPos o = king.origin();
		return Math.abs(king.getX() - o.getX() - x) < EPS && Math.abs(king.getZ() - o.getZ() - z) < EPS && Math.abs(king.getY() - king.floorY()) < EPS;
	}

	private static String where(YetiKingEntity king) {
		BlockPos o = king.origin();
		return String.format(Locale.ROOT, "(%.4f, %.4f) in the template, %s, stunned %d", king.getX() - o.getX(), king.getZ() - o.getZ(),
				king.action(), king.stunnedTicks());
	}

	private static double fromColumn(YetiKingEntity king) {
		BlockPos o = king.origin();
		return GlacierHall.columnDistance(king.getX() - o.getX(), king.getZ() - o.getZ());
	}

	private static double fromCentre(YetiKingEntity king) {
		Vec3 centre = king.centre();
		return Math.hypot(king.getX() - centre.x, king.getZ() - centre.z);
	}

	/** His charge has run and he is recovering from it, not stunned. */
	private static boolean ran(YetiKingEntity king) {
		return king.attack() == CHARGE && king.attackTicks() >= RUN && king.stunnedTicks() == 0;
	}

	/** Wakes him and, once he is down on his lake hunting, runs {@code then}. */
	private static void hunting(GameTestHelper helper, YetiKingEntity king, Runnable then) {
		king.wake(helper.getLevel());
		helper.runAfterDelay(YetiKingEntity.WAKE_TICKS + 2, then);
	}

	/**
	 * The Glacier Hall itself over this arena, placed as Lairs.place places it; returns what stood in the four rows under
	 * the arena that its underside overwrites, for clearHall to put back.
	 */
	private static Map<BlockPos, BlockState> placeHall(GameTestHelper helper, BlockPos origin) {
		ServerLevel level = helper.getLevel();
		StructureTemplate template = Lairs.template(level, Lair.GLACIER_HALL);
		helper.assertTrue(template != null, "No Glacier Hall template");
		Map<BlockPos, BlockState> below = new HashMap<>();
		for (BlockPos at : BlockPos.betweenClosed(origin, origin.offset(Lair.GLACIER_HALL.width - 1, GlacierHall.LAKE - 1, Lair.GLACIER_HALL.length - 1))) {
			below.put(at.immutable(), level.getBlockState(at));
		}
		template.placeInWorld(level, origin, origin, new StructurePlaceSettings().setKnownShape(true), level.getRandom(), Block.UPDATE_CLIENTS);
		return below;
	}

	/** Takes the hall away again: air from the lake's layer to the vault (most of it above the 8-high arena), the rows below put back. */
	private static void clearHall(ServerLevel level, BlockPos origin, Map<BlockPos, BlockState> below) {
		Lair hall = Lair.GLACIER_HALL;
		for (BlockPos at : BlockPos.betweenClosed(origin.offset(0, GlacierHall.LAKE, 0), origin.offset(hall.width - 1, hall.height - 1, hall.length - 1))) {
			if (!level.getBlockState(at).isAir()) {
				level.setBlock(at, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
			}
		}
		below.forEach((at, state) -> level.setBlock(at, state, Block.UPDATE_CLIENTS));
	}

	/** A caster who has mastered First Light, the Initiate's Wand in hand (as ConcordanceBaselineGameTests' master). */
	private static void master(ServerPlayer player) {
		player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(JugcraftConcordance.INITIATE_WAND));
		ConcordanceProgress.grant(player, "jugcraft:first_light", ResearchState.MASTERED);
		SpellContainerSource.setDirty(player, ConcordanceSpells.SOURCE);
		SpellContainerSource.setDirty(player, "main_hand");
		SpellContainerSource.update(player);
	}

	/** {@code caster} steps to template (x, z) on the lake, faces him and casts the Lance of Dawn through Spell Engine. */
	private static void lance(ServerPlayer caster, YetiKingEntity king, double x, double z) {
		BlockPos o = king.origin();
		double px = o.getX() + x;
		double pz = o.getZ() + z;
		float yaw = (float) (Math.toDegrees(Math.atan2(king.getZ() - pz, king.getX() - px)) - 90.0);
		caster.snapTo(px, king.floorY(), pz, yaw, 0.0F);
		caster.setYHeadRot(yaw);
		caster.setYBodyRot(yaw);
		((SpellCaster.Player) caster).getCooldownManager().reset(null);
		RateGate.forget(caster.getUUID());
		ServerLevel level = caster.level();
		SpellExecution.performSpell(level, caster, SpellRegistry.from(level).get(Identifier.parse("jugcraft:lance")).orElseThrow(),
				SpellTarget.SearchResult.empty(), SpellCast.Action.RELEASE, 1.0F);
	}

	/** A lake round his lake's centre as the hall's is: drift snow, a trampled route three wide east-west, a 2x2 patch of glare ice. */
	private static Map<BlockPos, BlockState> lake(ServerLevel level, YetiKingEntity king) {
		BlockPos middle = BlockPos.containing(king.centre()).below();
		for (int dx = -LAID; dx <= LAID; dx++) {
			for (int dz = -LAID; dz <= LAID; dz++) {
				Block block = Math.abs(dz) <= 1 ? JugcraftLairs.TRAMPLED_SNOW
						: dx >= 3 && dx <= 4 && dz >= 3 && dz <= 4 ? JugcraftLairs.GLARE_ICE : JugcraftLairs.DRIFT_SNOW;
				level.setBlockAndUpdate(middle.offset(dx, 0, dz), block.defaultBlockState());
			}
		}
		Map<BlockPos, BlockState> laid = new HashMap<>();   // read back once all are placed
		for (int dx = -LAID; dx <= LAID; dx++) {
			for (int dz = -LAID; dz <= LAID; dz++) {
				BlockPos cell = middle.offset(dx, 0, dz);
				laid.put(cell, level.getBlockState(cell));
			}
		}
		return laid;
	}

	private static List<BlockPos> changed(ServerLevel level, Map<BlockPos, BlockState> laid) {
		return laid.entrySet().stream().filter(cell -> !level.getBlockState(cell.getKey()).equals(cell.getValue())).map(Map.Entry::getKey).toList();
	}

	/** Every changed cell was drift snow as laid and is glare ice now. */
	private static boolean onlyDriftBared(ServerLevel level, Map<BlockPos, BlockState> laid) {
		return changed(level, laid).stream().allMatch(cell -> laid.get(cell).is(JugcraftLairs.DRIFT_SNOW) && level.getBlockState(cell).is(JugcraftLairs.GLARE_ICE));
	}

	private static long trampledWithin(Map<BlockPos, BlockState> laid, Vec3 at, double radius) {
		return laid.entrySet().stream().filter(cell -> cell.getValue().is(JugcraftLairs.TRAMPLED_SNOW)
				&& Math.hypot(cell.getKey().getX() + 0.5 - at.x, cell.getKey().getZ() + 0.5 - at.z) <= radius).count();
	}

	/** This test's arena, with room below (whelps step off the den floors) and above (icicles, boulders). */
	private static AABB arena(BlockPos origin) {
		return new AABB(origin.getX(), origin.getY() - 8, origin.getZ(), origin.getX() + 80, origin.getY() + 32, origin.getZ() + 88);
	}

	/** Stone under both dens' floors, for his kin to come out onto. */
	private static void dens(ServerLevel level, BlockPos origin) {
		floor(level, origin, 3, 34, 8, 39);
		floor(level, origin, 71, 34, 76, 39);
	}

	/** A survival player his blows cannot end a test early for (Resistance V, as TatterlaceGameTests does). */
	private static ServerPlayer shielded(GameTestHelper helper, YetiKingEntity king, double dx, double dz) {
		ServerPlayer player = player(helper, king, dx, dz);
		player.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, 1200, 4));
		return player;
	}

	/** A survival player standing on stone at {@code at} in the arena. */
	private static ServerPlayer onStone(GameTestHelper helper, BlockPos at) {
		helper.setBlock(at.below(), Blocks.STONE.defaultBlockState());
		ServerPlayer player = helper.makeMockServerPlayerInLevel();
		player.setGameMode(GameType.SURVIVAL);
		BlockPos feet = helper.absolutePos(at);
		player.snapTo(feet.getX() + 0.5, feet.getY(), feet.getZ() + 0.5, 0.0F, 0.0F);
		return player;
	}

	private static String firstFew(List<BlockPos> cells) {
		return cells.subList(0, Math.min(5, cells.size())).toString();
	}

	/** End-gun Levitation as GunShots gives it to what a bullet hurt: the game's own, at its first level, from the shooter. */
	private static boolean levitate(LivingEntity target, ServerPlayer shooter) {
		return target.addEffect(new MobEffectInstance(MobEffects.LEVITATION, GunShots.LIFT_TICKS, 0), shooter);
	}

	/** {@code healer} mends {@code patient} 4 through the Concordance's effect boundary, as the Mend word does. */
	private static void mend(ServerLevel level, ServerPlayer healer, ServerPlayer patient) {
		Cause cause = Cause.of(healer.getUUID(), Cause.Origin.SPELL, "test:mend", ConcordanceEffects.nextSerial());
		ConcordanceEffects.apply(new ConcordanceEffects.Context(level, cause, healer, new Ledger(new Ledger.Limits(16, 256, 0)), "mend",
				healer.position()), EffectSpec.of(EffectKind.RESTORATION, Intent.HELPFUL, 4, 0), patient);
	}

	/** How many of his trophies {@code player} holds. */
	private static int trophies(ServerPlayer player) {
		return player.getInventory().countItem(JugcraftYeti.glacierMaul()) + player.getInventory().countItem(JugcraftYeti.rimeclaw());
	}

	/** {@code player}'s deeds in {@code fight} (none without a part). */
	private static int deeds(Encounter fight, ServerPlayer player) {
		Encounter.Share share = fight.share(player);
		return share == null ? 0 : share.deeds();
	}

	/** A survival player north of the north-west column (26.5, 23.5), {@code x} across the hall (floor 24..29, 10..14). */
	private static ServerPlayer northOfTheColumn(GameTestHelper helper, YetiKingEntity king, BlockPos origin, double x) {
		ServerPlayer player = helper.makeMockServerPlayerInLevel();
		player.setGameMode(GameType.SURVIVAL);
		player.snapTo(origin.getX() + x, king.floorY(), origin.getZ() + 12.0, 0.0F, 0.0F);
		return player;
	}

	/** Puts him 9.5 blocks south of the north-west column and sets him charging at {@code foe}, beyond it (as a charge test does). */
	private static void chargeAt(ServerLevel level, YetiKingEntity king, BlockPos origin, ServerPlayer foe) {
		king.teleportTo(origin.getX() + 26.5, king.floorY(), origin.getZ() + 33.0);
		king.begin(level, YetiKingEntity.Attack.AVALANCHE_CHARGE, foe);
	}

	// ---------------------------------------------------------------- Concordance Chunk B, unit 5 (B-09): the counters

	/** East of his lake's centre, past its edge (GlacierHall.LAKE_RADIUS 20.5): players here never wake him. */
	private static final double OFF_LAKE = 26.0;

	/** A survival player {@code dx}, {@code dz} from where he stands off the lake. */
	private static ServerPlayer offLake(GameTestHelper helper, YetiKingEntity king, double dx, double dz) {
		return player(helper, king, OFF_LAKE + dx, dz);
	}

	/** He lands his Ground Slam off the lake (a waiting king is put back on his throne each tick, so each slam puts him there first). */
	private static int slam(ServerLevel level, YetiKingEntity king) {
		Vec3 centre = king.centre();
		king.teleportTo(centre.x + OFF_LAKE, king.floorY(), centre.z);
		return king.slamLands(level);
	}

	/** How {@code player}'s motion changed since {@code before}: across the ground, and up. */
	private static double[] moved(ServerPlayer player, Vec3 before) {
		Vec3 change = player.getDeltaMovement().subtract(before);
		return new double[] {Math.hypot(change.x, change.z), change.y};
	}

	private static boolean near(double a, double b) {
		return Math.abs(a - b) < 1.0E-9;
	}

	/** Puts {@code player} back at {@code at}, still: a mock player keeps the motion a push gave it, so the next is measured from rest. */
	private static void back(ServerPlayer player, Vec3 at) {
		player.snapTo(at.x, at.y, at.z, 0.0F, 0.0F);
		player.setDeltaMovement(Vec3.ZERO);
	}

	/** Knockback resistance 1 from elsewhere (as netherite or Rooted might give): it must change nothing of his push. */
	private static void steadfast(ServerPlayer player) {
		player.getAttribute(Attributes.KNOCKBACK_RESISTANCE).addTransientModifier(
				new AttributeModifier(Jugcraft.id("test_steadfast"), 1.0, AttributeModifier.Operation.ADD_VALUE));
	}

	/** The first effect of an invocation's composed form on the Initiate's Wand: what its cast applies. */
	private static EffectSpec invoked(String id) {
		return ConcordanceData.rules().authored(id, "jugcraft:initiate_wand").plan().root().steps().getFirst().effect();
	}

	/** {@code spec} on {@code target} as {@code caster}'s own new cast through the effect boundary (the test places them). */
	private static ConcordanceEffects.Result cast(ServerPlayer caster, EffectSpec spec, LivingEntity target) {
		Cause cause = Cause.of(caster.getUUID(), Cause.Origin.INVOCATION, "test:yeti_counter", ConcordanceEffects.nextSerial());
		return ConcordanceEffects.apply(new ConcordanceEffects.Context(caster.level(), cause, caster,
				new Ledger(new Ledger.Limits(16, 256, 0)), "0/0/0", caster.position()), spec, target);
	}

	/** One telegraph of his: his own tick count as a wind-up began, and as its strike came (-1 until then). */
	private static final class Telegraph {
		long begun = -1;
		long struck = -1;
	}

	/** Begins his Maul Swipe at {@code foe} and notes the tick it began. */
	private static void swipeAt(ServerLevel level, YetiKingEntity king, ServerPlayer foe, Telegraph telegraph) {
		king.begin(level, YetiKingEntity.Attack.MAUL_SWIPE, foe);
		telegraph.begun = king.tickCount;
		telegraph.struck = -1;
	}

	/**
	 * Waiting on his throne he takes no harm; a player's blow wakes him at full health, and over {@value
	 * YetiKingEntity#WAKE_TICKS} ticks he roars and leaps down onto his lake below the dais, to hunt.
	 */
	@GameTest(structure = HALL, padding = 8, maxTicks = 120)
	public void aBlowWakesHimAndHeLeapsDown(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		BlockPos origin = origin(helper);
		YetiKingEntity king = waiting(helper, origin);
		floor(level, origin, 37, 34, 42, 39);
		ServerPlayer player = player(helper, king, 0.5, 0.5);
		boolean throned = king.phase() == YetiKingEntity.Phase.WAITING && king.position().distanceTo(king.throne()) < 0.01;
		boolean harmless = !king.hurtServer(level, level.damageSources().playerAttack(player), 10.0F);
		boolean woken = king.phase() == YetiKingEntity.Phase.WAKING && king.getHealth() == king.getMaxHealth();
		helper.runAfterDelay(YetiKingEntity.WAKE_TICKS + 3, () -> {
			ending(level, king, () -> {
				player.discard();
				Vec3 below = GlacierHall.below().add(origin.getX(), origin.getY(), origin.getZ());
				helper.assertTrue(throned, "He did not wait on his throne");
				helper.assertTrue(harmless, "Waiting, he was harmed");
				helper.assertTrue(woken, "A blow did not wake him at full health");
				helper.assertTrue(king.phase() == YetiKingEntity.Phase.HUNT, "He did not come down to hunt: " + king.phase());
				helper.assertTrue(Math.abs(king.getY() - king.floorY()) < 0.1, "He is not on his lake but at " + king.getY());
				helper.assertTrue(Math.hypot(king.getX() - below.x, king.getZ() - below.z) < 2.0, "He did not land below his dais but at " + king.position());
			});
			helper.succeed();
		});
	}

	/**
	 * His health grows by half for each player after the first, at most two and a half times; the Fury of the Peaks makes
	 * every cooldown a third shorter and his slam bare a wider ring.
	 */
	@GameTest(structure = HALL, padding = 8)
	public void hisHealthAndHisFury(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		YetiKingEntity king = waiting(helper, origin(helper));
		ending(level, king, () -> {
			helper.assertTrue(YetiKingEntity.partyScale(1) == 1.0 && YetiKingEntity.partyScale(2) == 1.5 && YetiKingEntity.partyScale(4) == 2.5
					&& YetiKingEntity.partyScale(9) == 2.5, "His health scales with the party, at most 2.5 times");
			helper.assertTrue(king.cooldown(240) == 240 && king.ring() == YetiKingEntity.RING, "Calm, his cooldowns and his ring are their own");
			king.startFury(level);
			helper.assertTrue(king.furious() && king.phase() == YetiKingEntity.Phase.FURY, "No Fury of the Peaks");
			helper.assertTrue(king.cooldown(240) == 160 && king.cooldown(YetiKingEntity.GLOBAL_COOLDOWN) == 13,
					"In his fury his cooldowns are not a third shorter: " + king.cooldown(240));
			helper.assertTrue(king.ring() == YetiKingEntity.FURY_RING, "In his fury his slam does not bare a wider ring");
		});
		helper.succeed();
	}

	/**
	 * Maul Swipe strikes a player in front of him and not one directly behind. The two wait out their loading in
	 * creative, where he pays them no heed.
	 */
	@GameTest(structure = HALL, padding = 8, maxTicks = 180)
	public void hisSwipeStrikesInFrontNotBehind(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		BlockPos origin = origin(helper);
		YetiKingEntity king = waiting(helper, origin);
		floor(level, origin, 35, 31, 44, 42);
		ServerPlayer front = player(helper, king, 0.5, 3.0);
		ServerPlayer behind = player(helper, king, 0.5, -3.0);
		front.setGameMode(GameType.CREATIVE);
		behind.setGameMode(GameType.CREATIVE);
		helper.runAfterDelay(LOADING_TICKS, () -> {
			front.setGameMode(GameType.SURVIVAL);
			behind.setGameMode(GameType.SURVIVAL);
			ending(level, king, () -> {
				Vec3 centre = king.centre();
				king.teleportTo(centre.x, king.floorY(), centre.z);
				king.setYRot(0.0F);  // facing south, towards the one in front
				int hit = king.swipe(level);
				helper.assertTrue(hit == 1, "The swipe struck " + hit + " players, not 1");
				helper.assertTrue(front.getHealth() < front.getMaxHealth(), "The one in front is not struck");
				helper.assertTrue(behind.getHealth() == behind.getMaxHealth(), "The one behind him is not spared");
			});
			helper.succeed();
		});
	}

	/**
	 * His Ground Slam blasts the drift snow bare to glare ice within {@value YetiKingEntity#RING} blocks of where he lands,
	 * never the trampled snow nor the snow beyond; {@value YetiKingEntity#GLARE_TICKS} ticks later it has drifted back.
	 */
	@GameTest(structure = HALL, padding = 8, maxTicks = 300)
	public void hisSlamBaresTheDriftSnowAndItDriftsBack(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		YetiKingEntity king = waiting(helper, origin(helper));
		Vec3 at = king.centre();
		BlockPos middle = BlockPos.containing(at).below();
		// Drift snow round the lake's centre, a row of trampled snow across it.
		for (int x = -9; x <= 9; x++) {
			for (int z = -9; z <= 9; z++) {
				level.setBlockAndUpdate(middle.offset(x, 0, z), (z == 0 ? JugcraftLairs.TRAMPLED_SNOW : JugcraftLairs.DRIFT_SNOW).defaultBlockState());
			}
		}
		List<BlockPos> cells = king.bare(level, at, YetiKingEntity.RING);
		boolean bared = !cells.isEmpty() && cells.stream().allMatch(cell -> level.getBlockState(cell).is(JugcraftLairs.GLARE_ICE));
		boolean inRing = cells.stream().allMatch(cell -> Math.hypot(cell.getX() + 0.5 - at.x, cell.getZ() + 0.5 - at.z) <= YetiKingEntity.RING);
		boolean trampledKept = IntStream.rangeClosed(-9, 9).allMatch(x -> level.getBlockState(middle.offset(x, 0, 0)).is(JugcraftLairs.TRAMPLED_SNOW));
		boolean beyondKept = level.getBlockState(middle.offset(9, 0, 9)).is(JugcraftLairs.DRIFT_SNOW);
		int patches = king.baredPatches();
		helper.runAfterDelay(YetiKingEntity.GLARE_TICKS + 5, () -> {
			ending(level, king, () -> {
				helper.assertTrue(bared, "The drift snow round where he lands was not bared to glare ice");
				helper.assertTrue(inRing, "He bared snow outside his ring");
				helper.assertTrue(trampledKept, "He bared the trampled snow");
				helper.assertTrue(beyondKept, "He bared the snow beyond his ring");
				helper.assertTrue(patches == 1, patches + " bare patches, not 1");
				helper.assertTrue(cells.stream().allMatch(cell -> level.getBlockState(cell).is(JugcraftLairs.DRIFT_SNOW)),
						"The snow did not drift back over the bare ice");
				helper.assertTrue(king.baredPatches() == 0, "A patch is still bare");
				helper.assertTrue(IntStream.rangeClosed(-9, 9).allMatch(x -> level.getBlockState(middle.offset(x, 0, 0)).is(JugcraftLairs.TRAMPLED_SNOW)),
						"The snow drifting back covered the trampled snow");
			});
			helper.succeed();
		});
	}

	/**
	 * An Avalanche Charge at a foe beyond an ice column stops at the column: he is stunned for {@value
	 * YetiKingEntity#STUN_TICKS} ticks, and meanwhile takes a third more damage.
	 */
	@GameTest(structure = HALL, padding = 8, maxTicks = 160)
	public void aChargeIntoAColumnStunsHim(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		BlockPos origin = origin(helper);
		YetiKingEntity king = waiting(helper, origin);
		// The north-west column stands at (26.5, 23.5): he charges north at a foe beyond it.
		floor(level, origin, 24, 10, 29, 14);
		ServerPlayer foe = helper.makeMockServerPlayerInLevel();
		foe.setGameMode(GameType.SURVIVAL);
		foe.snapTo(origin.getX() + 26.5, king.floorY(), origin.getZ() + 12.0, 0.0F, 0.0F);
		king.wake(level);
		helper.runAfterDelay(YetiKingEntity.WAKE_TICKS + 2, () -> {
			YetiKingEntity.Phase hunting = king.phase();
			king.teleportTo(origin.getX() + 26.5, king.floorY(), origin.getZ() + 33.0);
			king.begin(level, YetiKingEntity.Attack.AVALANCHE_CHARGE, foe);
			helper.runAfterDelay(YetiKingEntity.Attack.AVALANCHE_CHARGE.windup + 12, () -> {
				ending(level, king, () -> {
					helper.assertTrue(hunting == YetiKingEntity.Phase.HUNT, "He was not hunting: " + hunting);
					helper.assertTrue(king.stunnedTicks() > 0 && king.action() == YetiKingEntity.Action.STUNNED,
							"Charging into the column did not stun him: " + king.action());
					double fromColumn = Math.hypot(king.getX() - origin.getX() - 26.5, king.getZ() - origin.getZ() - 23.5);
					helper.assertTrue(fromColumn > GlacierHall.COLUMN_RADIUS && king.getZ() - origin.getZ() > 23.5,
							"He ran through the column: " + fromColumn + " from it");
					float before = king.getHealth();
					king.hurtServer(level, level.damageSources().indirectMagic(foe, foe), 6.0F);
					float taken = before - king.getHealth();
					helper.assertTrue(Math.abs(taken - 6.0 * YetiKingEntity.STUN_TAKEN) < 0.01, "Stunned, he took " + taken + " of 6, not a third more");
					foe.discard();
				});
				helper.succeed();
			});
		});
	}

	/**
	 * His kin climb out of the dens in the side walls, never more than {@value YetiKingEntity#MAX_WHELPS} at once; left
	 * alone he climbs back onto his throne, healed, and they flee into the snow.
	 */
	@GameTest(structure = HALL, padding = 8, maxTicks = 60)
	public void hisKinComeFromTheDensAndFleeWhenHeIsLeftAlone(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		BlockPos origin = origin(helper);
		YetiKingEntity king = waiting(helper, origin);
		floor(level, origin, 3, 34, 8, 39);
		floor(level, origin, 71, 34, 76, 39);
		king.wake(level);
		int called = king.callKin(level, YetiKingEntity.KIN);
		int more = king.callKin(level, 6);
		List<UUID> kin = king.whelps();
		List<Vec3> dens = GlacierHall.dens().stream().map(den -> den.add(origin.getX(), origin.getY(), origin.getZ())).toList();
		boolean atDens = kin.stream().allMatch(id -> level.getEntity(id) instanceof YetiWhelpEntity whelp
				&& dens.stream().anyMatch(den -> whelp.position().distanceTo(den) < 1.5));
		king.setHealth(king.getMaxHealth() * 0.5F);
		king.reset(level);
		helper.runAfterDelay(3, () -> {
			ending(level, king, () -> {
				helper.assertTrue(called == YetiKingEntity.KIN, called + " whelps answered his call, not " + YetiKingEntity.KIN);
				helper.assertTrue(more == YetiKingEntity.MAX_WHELPS - YetiKingEntity.KIN && kin.size() == YetiKingEntity.MAX_WHELPS,
						"More than " + YetiKingEntity.MAX_WHELPS + " whelps came at once: " + kin.size());
				helper.assertTrue(atDens, "A whelp did not come out of a den");
				for (UUID id : kin) {
					helper.assertTrue(!(level.getEntity(id) instanceof YetiWhelpEntity whelp) || whelp.isRemoved(), "A whelp did not flee");
				}
				helper.assertTrue(king.phase() == YetiKingEntity.Phase.WAITING && king.getHealth() == king.getMaxHealth(),
						"Left alone he is not back on his throne, healed");
				helper.assertTrue(king.position().distanceTo(king.throne()) < 0.1, "He is not on his throne but at " + king.position());
			});
			helper.succeed();
		});
	}

	/**
	 * His frost: the King's Roar's blizzard chills a player to just short of freezing them through, his Frost Breath
	 * stacks past it; a Yeti Mitten in the offhand keeps its wearer warm, whatever frost they gather.
	 */
	@GameTest(maxTicks = 20)
	public void hisFrostAndTheYetiMitten(GameTestHelper helper) {
		ServerPlayer bare = helper.makeMockServerPlayerInLevel();
		ServerPlayer warm = helper.makeMockServerPlayerInLevel();
		bare.setGameMode(GameType.SURVIVAL);
		warm.setGameMode(GameType.SURVIVAL);
		warm.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(JugcraftYeti.YETI_MITTEN));
		YetiKingEntity.chill(bare, YetiKingEntity.BLIZZARD_FROST, false);
		YetiKingEntity.chill(warm, YetiKingEntity.BLIZZARD_FROST, false);
		int blizzard = bare.getTicksFrozen();
		boolean shortOfFreezing = !bare.isFullyFrozen();
		int warmBlizzard = warm.getTicksFrozen();
		YetiKingEntity.chill(bare, bare.getTicksRequiredToFreeze(), true);
		boolean frozenThrough = bare.isFullyFrozen();
		warm.setTicksFrozen(100);
		helper.runAfterDelay(2, () -> {
			helper.assertTrue(blizzard == YetiKingEntity.BLIZZARD_FROST && shortOfFreezing, "The blizzard did not chill to just short of freezing: " + blizzard);
			helper.assertTrue(frozenThrough, "His breath's frost did not stack past freezing");
			helper.assertTrue(warmBlizzard == 0, "The mitten's wearer was chilled");
			helper.assertTrue(warm.getTicksFrozen() == 0, "The mitten did not keep its wearer warm");
			bare.discard();
			warm.discard();
			helper.succeed();
		});
	}

	/**
	 * Winter Brine (Concordance Chunk B, B-08): a player who drank a draught of Frostward is not chilled by his blizzard or
	 * his breath, and frost gathered before is gone at their next tick; once Frostward ends (milk, or its time) his frost
	 * lands again. No mitten is held; the Mitten stays the lasting answer.
	 */
	@GameTest(maxTicks = 20)
	public void aFrostwardDrinkerIsNotChilledByTheYeti(GameTestHelper helper) {
		ServerPlayer bare = helper.makeMockServerPlayerInLevel();
		ServerPlayer warded = helper.makeMockServerPlayerInLevel();
		bare.setGameMode(GameType.SURVIVAL);
		warded.setGameMode(GameType.SURVIVAL);
		BlockPos at = helper.absolutePos(new BlockPos(1, 2, 1));
		warded.snapTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, 0.0F, 0.0F);
		ItemStack brine = winterBrine();
		brine.getItem().finishUsingItem(brine, helper.getLevel(), warded);
		helper.assertTrue(io.github.jimbozoomer.jugcraft.concordance.rime.Frostward.warded(warded)
				&& !warded.getOffhandItem().is(JugcraftYeti.YETI_MITTEN), "The draught gave Frostward, and no mitten is held");
		YetiKingEntity.chill(bare, YetiKingEntity.BLIZZARD_FROST, false);
		YetiKingEntity.chill(warded, YetiKingEntity.BLIZZARD_FROST, false);
		helper.assertTrue(warded.getTicksFrozen() == 0, "His blizzard chilled a Frostward drinker: " + warded.getTicksFrozen());
		helper.assertTrue(bare.getTicksFrozen() == YetiKingEntity.BLIZZARD_FROST, "His blizzard did not chill a player without it");
		YetiKingEntity.chill(warded, warded.getTicksRequiredToFreeze(), true);
		helper.assertTrue(warded.getTicksFrozen() == 0 && !warded.isFullyFrozen(), "His breath's frost took on a Frostward drinker");
		warded.setTicksFrozen(100);
		warded.baseTick(); // a mock player has no client to tick it (JugcraftGameTests.scubaTankKeepsAirUnderWater)
		helper.assertTrue(warded.getTicksFrozen() == 0, "Frost gathered before was not cleared at the next tick: " + warded.getTicksFrozen());
		warded.removeEffect(io.github.jimbozoomer.jugcraft.concordance.rime.Frostward.FROSTWARD);
		YetiKingEntity.chill(warded, YetiKingEntity.BLIZZARD_FROST, false);
		helper.assertTrue(warded.getTicksFrozen() == YetiKingEntity.BLIZZARD_FROST, "Once Frostward ends, his frost lands again");
		bare.discard();
		warded.discard();
		helper.succeed();
	}

	/** A draught of Winter Brine as the crucible bottles it: Frostward I for 975 ticks (three snowballs, stirred cold three times). */
	private static ItemStack winterBrine() {
		ItemStack draught = new ItemStack(io.github.jimbozoomer.jugcraft.concordance.JugcraftConcordance.DRAUGHT);
		draught.set(io.github.jimbozoomer.jugcraft.concordance.JugcraftConcordance.BREW, new io.github.jimbozoomer.jugcraft.concordance.Brew(
				List.of(new io.github.jimbozoomer.jugcraft.concordance.Brew.Dose("jugcraft:frostward", 0, 975, false)), "jugcraft:frostward@0"));
		return draught;
	}

	/**
	 * The Rimekeeper's travel kit against his frost (Concordance Chunk B, B-10): his frost is his own, so a player in a kit
	 * piece is still chilled by his blizzard and frozen through by his breath (B-P23: the kit is no boss immunity), but
	 * cannot freeze, as a bare player can. What vanilla then does needs a ticking body: on a stone floor two pigs, one in
	 * Winterguard's Helmet, are each frozen 400 ticks' worth once (melting two a tick keeps them frozen through for 130
	 * ticks, standing in for his breath renewing it). After 90 ticks the dressed pig is unhurt and the bare one has taken
	 * at least two of vanilla's freezing blows; both are slowed by the frost, as far as frost slows.
	 */
	@GameTest(maxTicks = 120)
	public void aKitWearerIsSlowedButNotHurtByHisFrost(GameTestHelper helper) {
		ServerPlayer kit = helper.makeMockServerPlayerInLevel();
		ServerPlayer bare = helper.makeMockServerPlayerInLevel();
		kit.setGameMode(GameType.SURVIVAL);
		bare.setGameMode(GameType.SURVIVAL);
		kit.setItemSlot(net.minecraft.world.entity.EquipmentSlot.HEAD,
				new ItemStack(io.github.jimbozoomer.jugcraft.concordance.rime.RimeGear.ARMOR.get("nereids_hood")));
		YetiKingEntity.chill(kit, YetiKingEntity.BLIZZARD_FROST, false);
		helper.assertTrue(kit.getTicksFrozen() == YetiKingEntity.BLIZZARD_FROST, "His blizzard still chills a kit wearer: " + kit.getTicksFrozen());
		YetiKingEntity.chill(kit, kit.getTicksRequiredToFreeze(), true);
		helper.assertTrue(kit.isFullyFrozen(), "His breath still freezes a kit wearer through: " + kit.getTicksFrozen());
		helper.assertTrue(!kit.canFreeze() && bare.canFreeze(), "A kit wearer cannot freeze; a bare player can");
		for (int x = 0; x <= 4; x++) {
			for (int z = 0; z <= 2; z++) {
				helper.setBlock(new BlockPos(x, 1, z), net.minecraft.world.level.block.Blocks.STONE);
			}
		}
		net.minecraft.world.entity.Mob dressed = helper.spawnWithNoFreeWill(net.minecraft.world.entity.EntityTypes.PIG, new BlockPos(1, 2, 1));
		net.minecraft.world.entity.Mob plain = helper.spawnWithNoFreeWill(net.minecraft.world.entity.EntityTypes.PIG, new BlockPos(3, 2, 1));
		dressed.setItemSlot(net.minecraft.world.entity.EquipmentSlot.HEAD,
				new ItemStack(io.github.jimbozoomer.jugcraft.concordance.rime.RimeGear.ARMOR.get("winterguards_helmet")));
		dressed.setTicksFrozen(400);
		plain.setTicksFrozen(400);
		helper.runAfterDelay(90, () -> {
			helper.assertTrue(dressed.isFullyFrozen() && plain.isFullyFrozen(), "Both pigs are still frozen through: "
					+ dressed.getTicksFrozen() + ", " + plain.getTicksFrozen());
			helper.assertTrue(dressed.getHealth() == dressed.getMaxHealth(), "Frozen through in the kit, no freezing blow: " + dressed.getHealth());
			helper.assertTrue(plain.getHealth() <= plain.getMaxHealth() - 2.0F, "Frozen through bare, a blow every two seconds: " + plain.getHealth());
			net.minecraft.resources.Identifier slow = net.minecraft.resources.Identifier.withDefaultNamespace("powder_snow");
			for (net.minecraft.world.entity.Mob pig : List.of(dressed, plain)) {
				net.minecraft.world.entity.ai.attributes.AttributeModifier modifier = pig
						.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED).getModifier(slow);
				helper.assertTrue(modifier != null && Math.abs(modifier.amount() + 0.05) < 1.0E-6, "The frost slows both: " + modifier);
			}
			kit.discard();
			bare.discard();
			dressed.discard();
			plain.discard();
			helper.succeed();
		});
	}

	/**
	 * A glacial spike bursting up under a player strikes them and throws them up; an icicle shaken from the vault falls
	 * onto its mark and shatters, striking the player standing there.
	 */
	@GameTest(structure = HALL, padding = 8, maxTicks = 140)
	public void aSpikeThrowsAPlayerUpAndAnIcicleShatters(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		BlockPos origin = origin(helper);
		YetiKingEntity king = waiting(helper, origin);
		floor(level, origin, 32, 34, 42, 39);
		ServerPlayer struck = player(helper, king, 0.5, 0.5);
		ServerPlayer under = player(helper, king, -4.5, 0.5);
		// They wait out their loading in creative, where he pays them no heed; once they can be struck he may wake, but
		// waking he strikes no one until the icicle has long since landed.
		struck.setGameMode(GameType.CREATIVE);
		under.setGameMode(GameType.CREATIVE);
		helper.runAfterDelay(LOADING_TICKS, () -> {
			struck.setGameMode(GameType.SURVIVAL);
			under.setGameMode(GameType.SURVIVAL);
			float before = struck.getHealth();
			GlacialSpikeEntity.burst(level, king, struck.position());
			boolean thrown = struck.getHealth() < before && struck.getDeltaMovement().y > 0.0;
			float underBefore = under.getHealth();
			FallingIcicleEntity icicle = FallingIcicleEntity.drop(level, king, under.position());
			helper.runAfterDelay((int) Math.ceil(FallingIcicleEntity.HEIGHT / FallingIcicleEntity.FALL_SPEED) + 4, () -> {
				ending(level, king, () -> {
					helper.assertTrue(thrown, "The spike did not strike the player and throw them up");
					helper.assertTrue(icicle.isRemoved(), "The icicle did not shatter");
					helper.assertTrue(under.getHealth() < underBefore, "The icicle did not strike the player under it");
					struck.discard();
					under.discard();
				});
				helper.succeed();
			});
		});
	}

	/**
	 * His loot is each participant's own: everyone who struck him rolls for themselves (Yeti Fur, and on a first kill
	 * always one of his trophies) into their own inventory, and a bystander who never struck him gets nothing. Afterwards
	 * Abominable is theirs, and a later roll no longer counts as a first kill.
	 */
	@GameTest(structure = HALL, padding = 8)
	public void hisLootIsEachParticipantsOwn(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		BlockPos origin = origin(helper);
		YetiKingEntity king = waiting(helper, origin);
		floor(level, origin, 36, 34, 43, 41);
		ServerPlayer first = player(helper, king, 0.5, 3.0);
		ServerPlayer second = player(helper, king, 2.5, 3.0);
		ServerPlayer bystander = player(helper, king, -1.5, 3.0);
		king.took(first, 10.0F);
		king.took(second, 4.0F);
		ending(level, king, () -> {
			List<ServerPlayer> participants = king.participants(level);
			helper.assertTrue(participants.contains(first) && participants.contains(second), "Both who struck him take part");
			helper.assertFalse(participants.contains(bystander), "A bystander who never struck him does not");
			helper.assertTrue(YetiKingLoot.firstKill(first), "A newcomer has not yet won Abominable");
			List<ItemStack> loot = YetiKingLoot.roll(level, king, first);
			int fur = loot.stream().filter(stack -> stack.is(JugcraftYeti.YETI_FUR)).mapToInt(ItemStack::getCount).sum();
			helper.assertTrue(fur >= 4 && fur <= 8, fur + " Yeti Fur, not 4 to 8");
			helper.assertTrue(loot.stream().filter(YetiKingLoot::trophy).count() == 1, "A first kill always brings one of his trophies");
			YetiKingLoot.reward(level, king, participants);
			for (ServerPlayer player : List.of(first, second)) {
				helper.assertTrue(player.getInventory().countItem(JugcraftYeti.YETI_FUR) >= 4, "Each participant's loot went into their own inventory");
				int trophies = player.getInventory().countItem(JugcraftYeti.glacierMaul()) + player.getInventory().countItem(JugcraftYeti.rimeclaw());
				helper.assertTrue(trophies == 1, "Each first kill brought its own trophy, not " + trophies);
				helper.assertFalse(YetiKingLoot.firstKill(player), "Abominable is theirs now");
			}
			helper.assertTrue(bystander.getInventory().countItem(JugcraftYeti.YETI_FUR) == 0, "The bystander got nothing");
			helper.assertTrue(YetiKingLoot.firstKill(bystander), "The bystander has not won Abominable");
			for (ServerPlayer player : List.of(first, second, bystander)) {
				player.discard();
			}
		});
		helper.succeed();
	}

	/**
	 * Concordance Chunk B: support shares his loot. A healer who mended 4 of what his fight did is paid; a striker who
	 * died in it is kept their share and handed it on their feet; one who left the game in it has it kept in the world
	 * and handed over on joining; one who walked away alive, a bystander and the one he only hurt are not paid; the
	 * experience is shared among those paid, now or later; and his fall reported again pays no one twice.
	 */
	@GameTest(structure = HALL, padding = 8)
	public void supportSharesHisLootOnce(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		BlockPos origin = origin(helper);
		YetiKingEntity king = waiting(helper, origin);
		floor(level, origin, 34, 34, 45, 41);
		ServerPlayer fighter = player(helper, king, 0.5, 3.0);
		ServerPlayer healer = player(helper, king, 2.5, 3.0);
		ServerPlayer bystander = player(helper, king, -1.5, 3.0);
		ServerPlayer wanderer = player(helper, king, 4.5, 3.0);
		ServerPlayer fallen = player(helper, king, -3.5, 3.0);
		// Away from the game: a player the server's list never held, as one who has left is not held.
		FakePlayer away = FakePlayer.get(level, new GameProfile(UUID.randomUUID(), "yeti_away"));
		Encounter fight = king.encounter();
		fight.begin();
		UUID receipt = fight.fight();
		LairRewards.Data data = LairRewards.Data.of(level.getServer());
		ending(level, king, () -> {
			try {
				// His fight hurt the fighter 6 (as his blow would have been measured); the healer mends 4 of it.
				fighter.setHealth(fighter.getMaxHealth() - 6.0F);
				fight.hurt(fighter, 6.0F);
				mend(level, healer, fighter);
				king.took(wanderer, 2.0F);
				king.took(fallen, 2.0F);
				king.took(away, 2.0F);
				// A real death: Fabric's AFTER_DEATH reaches Encounter's listener (the killing blow itself is not real).
				fallen.setHealth(0.0F);
				fallen.die(level.damageSources().genericKill());
				// Leaving is marked as Fabric's LEAVE would mark it: a test player cannot disconnect.
				fight.left(away.getUUID());
				wanderer.snapTo(wanderer.getX() + 100.0, wanderer.getY(), wanderer.getZ(), 0.0F, 0.0F);
				helper.assertTrue(fight.excused(fallen.getUUID()), "Dying in his fight did not excuse the fallen striker");
				List<UUID> qualified = fight.qualified();
				helper.assertTrue(qualified.containsAll(List.of(healer.getUUID(), wanderer.getUUID(), fallen.getUUID(), away.getUUID()))
						&& !qualified.contains(fighter.getUUID()) && !qualified.contains(bystander.getUUID()),
						"The healer and the three strikers should qualify, the one he only hurt and the bystander not: " + qualified);
				int experience = healer.totalExperience;
				king.hurtServer(level, level.damageSources().genericKill(), Float.MAX_VALUE);
				int fur = healer.getInventory().countItem(JugcraftYeti.YETI_FUR);
				int share = YetiKingEntity.EXPERIENCE / 3;
				helper.assertTrue(fur >= 4 && trophies(healer) == 1, "The healer was not paid, first-kill trophy and all: " + fur + " Yeti Fur");
				helper.assertTrue(healer.totalExperience - experience == share,
						"300 was not shared among the three paid: " + (healer.totalExperience - experience));
				LairRewards.Owed owed = fallen.getAttached(LairRewards.OWED);
				helper.assertTrue(owed != null && owed.due().size() == 1 && owed.due().getFirst().boss().equals(YetiKingLoot.BOSS)
						&& owed.due().getFirst().fight().equals(receipt) && fallen.getInventory().countItem(JugcraftYeti.YETI_FUR) == 0,
						"The fallen striker's share was not kept for them: " + owed);
				List<LairRewards.Due> kept = data.kept(away.getUUID());
				helper.assertTrue(kept.size() == 1 && kept.getFirst().boss().equals(YetiKingLoot.BOSS) && kept.getFirst().fight().equals(receipt)
						&& kept.getFirst().experience() == share, "The share of one who left the game was not kept in the world: " + kept);
				for (ServerPlayer unpaid : List.of(wanderer, bystander, fighter)) {
					helper.assertTrue(unpaid.getInventory().countItem(JugcraftYeti.YETI_FUR) == 0,
							"Paid: the one who walked away, the bystander or the one he only hurt");
				}
				// Back on their feet far off (AFTER_RESPAWN calls handOver); joining (JOIN calls welcome).
				fallen.snapTo(fallen.getX() - 100.0, fallen.getY(), fallen.getZ(), 0.0F, 0.0F);
				fallen.setHealth(fallen.getMaxHealth());
				helper.assertTrue(LairRewards.handOver(fallen) && fallen.getInventory().countItem(JugcraftYeti.YETI_FUR) >= 4
						&& !fallen.hasAttached(LairRewards.OWED), "Back on their feet, the fallen striker was not handed their share by his payer");
				LairRewards.welcome(away);
				helper.assertTrue(away.getInventory().countItem(JugcraftYeti.YETI_FUR) >= 4 && data.kept(away.getUUID()).isEmpty(),
						"Joining again, the one who left was not handed their share, or the world still keeps it");
				int fallenFur = fallen.getInventory().countItem(JugcraftYeti.YETI_FUR);
				king.die(level.damageSources().genericKill());
				helper.assertTrue(healer.getInventory().countItem(JugcraftYeti.YETI_FUR) == fur && trophies(healer) == 1
						&& fallen.getInventory().countItem(JugcraftYeti.YETI_FUR) == fallenFur && !fallen.hasAttached(LairRewards.OWED)
						&& data.kept(away.getUUID()).isEmpty(), "His fall reported again paid someone twice");
			} finally {
				data.take(away.getUUID());
				List.of(fighter, healer, bystander, wanderer, fallen).forEach(ServerPlayer::discard);
			}
		});
		helper.succeed();
	}

	/**
	 * Concordance Chunk B: a player whose only part in his fight is the blow that fells him has taken part before his
	 * fall is paid, and is paid: Yeti Fur, a first kill's trophy and Abominable.
	 */
	@GameTest(structure = HALL, padding = 8, maxTicks = 120)
	public void aKillingBlowAloneIsPaid(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		BlockPos origin = origin(helper);
		YetiKingEntity king = waiting(helper, origin);
		floor(level, origin, 34, 34, 45, 41);
		king.wake(level);
		helper.runAfterDelay(YetiKingEntity.WAKE_TICKS + 2, () -> {
			ServerPlayer striker = player(helper, king, 0.5, 3.0);
			Encounter fight = king.encounter();
			ending(level, king, () -> {
				try {
					helper.assertTrue(king.phase() == YetiKingEntity.Phase.HUNT && fight.underway() && fight.share(striker) == null,
							"He was not hunting in a fight under way, or the striker already had a part: " + king.phase());
					boolean felled = king.hurtServer(level, level.damageSources().playerAttack(striker), king.getMaxHealth() * 4.0F);
					Encounter.Share share = fight.share(striker);
					helper.assertTrue(felled && king.isDeadOrDying() && share != null && share.qualifies(),
							"The blow that felled him was not the striker's part in his fight");
					helper.assertTrue(striker.getInventory().countItem(JugcraftYeti.YETI_FUR) >= 4 && trophies(striker) == 1,
							"The striker whose only blow felled him was not paid");
					helper.assertFalse(YetiKingLoot.firstKill(striker), "Abominable is not the striker's");
				} finally {
					striker.discard();
				}
			});
			helper.succeed();
		});
	}

	/**
	 * Concordance Chunk B: a tamed wolf's bite on him, or on one of his whelps, is its owner's part in his fight, and
	 * each owner is paid when he falls.
	 */
	@GameTest(structure = HALL, padding = 8, maxTicks = 120)
	public void aPetsBlowIsItsOwners(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		BlockPos origin = origin(helper);
		YetiKingEntity king = waiting(helper, origin);
		floor(level, origin, 34, 34, 45, 41);
		king.wake(level);
		helper.runAfterDelay(YetiKingEntity.WAKE_TICKS + 2, () -> {
			ServerPlayer kingOwner = player(helper, king, 0.5, 3.0);
			ServerPlayer whelpOwner = player(helper, king, 2.5, 3.0);
			var kingWolf = helper.spawnWithNoFreeWill(EntityTypes.WOLF, new BlockPos(38, 1, 40));
			var whelpWolf = helper.spawnWithNoFreeWill(EntityTypes.WOLF, new BlockPos(41, 1, 40));
			kingWolf.tame(kingOwner);
			whelpWolf.tame(whelpOwner);
			Encounter fight = king.encounter();
			ending(level, king, () -> {
				try {
					helper.assertTrue(king.callKin(level, 1) == 1, "No whelp came out of a den");
					YetiWhelpEntity whelp = (YetiWhelpEntity) level.getEntity(king.whelps().getFirst());
					boolean bitten = whelp.hurtServer(level, level.damageSources().mobAttack(whelpWolf), 2.0F);
					Encounter.Share whelpShare = fight.share(whelpOwner);
					helper.assertTrue(bitten && whelpShare != null && whelpShare.damage() == 2.0F && whelpShare.qualifies(),
							"A wolf's bite on his whelp was not its owner's part");
					boolean bitHim = king.hurtServer(level, level.damageSources().mobAttack(kingWolf), 4.0F);
					Encounter.Share kingShare = fight.share(kingOwner);
					helper.assertTrue(bitHim && kingShare != null && kingShare.damage() == 4.0F && kingShare.qualifies(),
							"A wolf's bite on him was not its owner's part");
					king.hurtServer(level, level.damageSources().genericKill(), Float.MAX_VALUE);
					helper.assertTrue(kingOwner.getInventory().countItem(JugcraftYeti.YETI_FUR) >= 4
							&& whelpOwner.getInventory().countItem(JugcraftYeti.YETI_FUR) >= 4, "A wolf's owner was not paid");
				} finally {
					kingWolf.discard();
					whelpWolf.discard();
					kingOwner.discard();
					whelpOwner.discard();
				}
			});
			helper.succeed();
		});
	}

	/**
	 * Concordance Chunk B: his charge aimed at a player who already has a part in his fight (here a ward on a friend,
	 * short of qualifying) and ending in an ice column is that player's deed, once a fight; a second baiter earns their
	 * own on his next charge, and both qualify by it alone.
	 */
	@GameTest(structure = HALL, padding = 8, maxTicks = 240)
	public void twoBaitersEachEarnTheirDeed(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		BlockPos origin = origin(helper);
		YetiKingEntity king = waiting(helper, origin);
		floor(level, origin, 24, 10, 29, 14);
		ServerPlayer first = northOfTheColumn(helper, king, origin, 26.5);
		ServerPlayer second = northOfTheColumn(helper, king, origin, 25.5);
		int run = YetiKingEntity.Attack.AVALANCHE_CHARGE.windup + 12;
		Encounter fight = king.encounter();
		king.wake(level);
		helper.runAfterDelay(YetiKingEntity.WAKE_TICKS + 2, () -> {
			// Each wards the other: a part in his fight, short of qualifying.
			fight.warded(first, second, 2.0F, 600);
			fight.warded(second, first, 2.0F, 600);
			Encounter.Share firstPart = fight.share(first);
			Encounter.Share secondPart = fight.share(second);
			boolean shortOfQualifying = firstPart != null && !firstPart.qualifies() && secondPart != null && !secondPart.qualifies();
			chargeAt(level, king, origin, first);
			helper.runAfterDelay(run, () -> {
				boolean firstStunned = king.stunnedTicks() > 0;
				int firstDeeds = deeds(fight, first);
				int secondThen = deeds(fight, second);
				// Once he has reeled, and before his own pause between attacks is up, he charges the other.
				helper.runAfterDelay(YetiKingEntity.STUN_TICKS + 4, () -> {
					chargeAt(level, king, origin, second);
					helper.runAfterDelay(run, () -> {
						ending(level, king, () -> {
							try {
								helper.assertTrue(shortOfQualifying, "A ward on a friend did not give each a part short of qualifying");
								helper.assertTrue(firstStunned && king.stunnedTicks() > 0, "A charge did not end in the column");
								helper.assertTrue(firstDeeds == 1 && secondThen == 0,
										"The first charge was not the first baiter's deed alone: " + firstDeeds + " and " + secondThen);
								helper.assertTrue(deeds(fight, first) == 1 && deeds(fight, second) == 1,
										"Each baiter did not earn their own deed: " + deeds(fight, first) + " and " + deeds(fight, second));
								helper.assertTrue(fight.share(first).damage() == 0.0F && fight.share(second).damage() == 0.0F,
										"A baiter struck him");
								helper.assertTrue(fight.qualified().containsAll(List.of(first.getUUID(), second.getUUID())),
										"The deed alone did not qualify each baiter");
								helper.assertFalse(fight.deed(first, "column " + first.getUUID()), "A baiter's deed counted twice in one fight");
							} finally {
								first.discard();
								second.discard();
							}
						});
						helper.succeed();
					});
				});
			});
		});
	}

	/**
	 * Concordance Chunk B: a late arrival with no part in his fight, though his fight has struck them, whom he charges into
	 * a column (as when he turns on the nearest player) earns no deed and no reward, and no one else gets the deed.
	 */
	@GameTest(structure = HALL, padding = 8, maxTicks = 160)
	public void anIdleLateArrivalEarnsNothing(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		BlockPos origin = origin(helper);
		YetiKingEntity king = waiting(helper, origin);
		floor(level, origin, 24, 10, 29, 14);
		floor(level, origin, 34, 34, 45, 41);
		ServerPlayer fighter = player(helper, king, 0.5, 3.0);
		fighter.setGameMode(GameType.CREATIVE);  // he pays them no heed
		int run = YetiKingEntity.Attack.AVALANCHE_CHARGE.windup + 12;
		Encounter fight = king.encounter();
		king.wake(level);
		helper.runAfterDelay(YetiKingEntity.WAKE_TICKS + 2, () -> {
			king.took(fighter, 5.0F);
			ServerPlayer late = northOfTheColumn(helper, king, origin, 26.5);
			fight.hurt(late, 4.0F);  // struck by his fight, but no part in it
			boolean idle = fight.share(late) == null;
			chargeAt(level, king, origin, late);
			helper.runAfterDelay(run, () -> {
				ending(level, king, () -> {
					try {
						helper.assertTrue(idle, "The late arrival already had a part in his fight");
						helper.assertTrue(king.stunnedTicks() > 0 && king.action() == YetiKingEntity.Action.STUNNED,
								"His charge at the late arrival did not end in the column: " + king.action());
						helper.assertTrue(fight.share(late) == null && !fight.qualified().contains(late.getUUID()),
								"A late arrival with no part earned a deed by where his charge ended");
						helper.assertTrue(deeds(fight, fighter) == 0, "The deed went to someone else");
						king.hurtServer(level, level.damageSources().genericKill(), Float.MAX_VALUE);
						helper.assertTrue(late.getInventory().countItem(JugcraftYeti.YETI_FUR) == 0, "The idle late arrival was paid");
						helper.assertTrue(fighter.getInventory().countItem(JugcraftYeti.YETI_FUR) >= 4, "The one who struck him was not paid");
					} finally {
						fighter.discard();
						late.discard();
					}
				});
				helper.succeed();
			});
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
	@GameTest(structure = HALL, padding = 8)
	public void fourPlayersAreEachPaidOnce(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		BlockPos origin = origin(helper);
		YetiKingEntity king = waiting(helper, origin);
		floor(level, origin, 34, 34, 45, 41);
		List<ServerPlayer> party = List.of(player(helper, king, -3.5, 3.0), player(helper, king, -1.5, 3.0), player(helper, king, 0.5, 3.0),
				player(helper, king, 2.5, 3.0));
		king.wake(level);
		ending(level, king, () -> {
			try {
				helper.assertTrue(Math.abs(king.getMaxHealth() - capped(YetiKingEntity.HEALTH * YetiKingEntity.partyScale(4))) < 0.01,
						"Woken by four, his health is not " + YetiKingEntity.partyScale(4) + " times his own, at most the game's cap: " + king.getMaxHealth());
				int[] before = new int[party.size()];
				for (int i = 0; i < party.size(); i++) {
					king.took(party.get(i), 5.0F);
					before[i] = party.get(i).totalExperience;
				}
				king.hurtServer(level, level.damageSources().genericKill(), Float.MAX_VALUE);
				int[] gained = new int[party.size()];
				for (int i = 0; i < party.size(); i++) {
					ServerPlayer player = party.get(i);
					gained[i] = player.totalExperience - before[i];
					helper.assertTrue(player.getInventory().countItem(JugcraftYeti.YETI_FUR) >= 4 && trophies(player) == 1
							&& gained[i] == YetiKingEntity.EXPERIENCE / 4, "A player of the four was not paid once: " + gained[i] + " experience");
				}
				king.die(level.damageSources().genericKill());
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
	 * Concordance Chunk B: his fight's record is not saved. Saved awake and loaded again, he holds no one's part, and on
	 * his first ticks he begins a fresh record, so who takes part from then on is counted.
	 */
	@GameTest(structure = HALL, padding = 8, maxTicks = 40)
	public void aFightLoadedAwakeIsRecordedAfresh(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		BlockPos origin = origin(helper);
		YetiKingEntity king = waiting(helper, origin);
		floor(level, origin, 34, 34, 45, 41);
		ServerPlayer striker = player(helper, king, 0.5, 3.0);
		striker.setGameMode(GameType.CREATIVE);  // he pays them no heed
		king.wake(level);
		king.took(striker, 4.0F);
		boolean before = king.encounter().qualified().contains(striker.getUUID());
		// Saved awake and loaded again, as his chunk unloading and loading would.
		TagValueOutput out = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, level.registryAccess());
		king.saveWithoutId(out);
		king.discard();
		YetiKingEntity loaded = new YetiKingEntity(JugcraftYeti.YETI_KING, level);
		loaded.load(TagValueInput.create(ProblemReporter.DISCARDING, level.registryAccess(), out.buildResult()));
		loaded.setUUID(UUID.randomUUID());
		level.addFreshEntity(loaded);
		boolean unsaved = loaded.encounter().share(striker) == null && !loaded.encounter().underway();
		helper.runAfterDelay(3, () -> {
			ending(level, loaded, () -> {
				try {
					helper.assertTrue(before && unsaved, "His record was saved with him, or the striker never had a part");
					helper.assertTrue(loaded.phase() == YetiKingEntity.Phase.HUNT, "Loaded waking, he does not hunt: " + loaded.phase());
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
	 * share holds 4 to 8 Yeti Fur and has a 15% chance of one of his two trophies (7.5% each, never both), a 25% chance of
	 * the Yeti Mitten and a 20% chance of his Crown. A player who has already won Abominable (so no first-kill trophy)
	 * rolls {@value #SHARES} shares: none holds both trophies, and each count lies within five standard deviations of its
	 * chance (a pool's chance ignored, or his trophies split into two pools, falls outside). check_acquisition holds the
	 * same table to the pages' words; the first kill's trophy is hisLootIsEachParticipantsOwn's.
	 */
	@GameTest(maxTicks = 20)
	public void hisLootIsWhatHisPagesSay(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		ServerPlayer veteran = helper.makeMockServerPlayerInLevel();
		TrickOrTreat.award(veteran, YetiKingLoot.ADVANCEMENT);
		helper.assertFalse(YetiKingLoot.firstKill(veteran), "A player who has won Abominable rolls no first-kill trophy");
		Vec3 origin = helper.absoluteVec(new Vec3(1.5, 2.0, 1.5));
		int maul = 0;
		int rimeclaw = 0;
		int mitten = 0;
		int crown = 0;
		try {
			for (int share = 0; share < SHARES; share++) {
				List<ItemStack> loot = YetiKingLoot.roll(level, origin, veteran);
				int fur = loot.stream().filter(stack -> stack.is(JugcraftYeti.YETI_FUR)).mapToInt(ItemStack::getCount).sum();
				helper.assertTrue(fur >= 4 && fur <= 8, fur + " Yeti Fur in a share, not 4 to 8");
				long trophies = loot.stream().filter(YetiKingLoot::trophy).count();
				helper.assertTrue(trophies <= 1, "A share held " + trophies + " trophies: his two share one pool");
				maul += (int) loot.stream().filter(stack -> stack.is(JugcraftYeti.glacierMaul())).count();
				rimeclaw += (int) loot.stream().filter(stack -> stack.is(JugcraftYeti.rimeclaw())).count();
				mitten += (int) loot.stream().filter(stack -> stack.is(JugcraftYeti.YETI_MITTEN)).count();
				crown += (int) loot.stream().filter(stack -> stack.is(JugcraftYeti.YETI_KING_CROWN)).count();
			}
		} finally {
			veteran.discard();
		}
		helper.assertTrue(within(maul + rimeclaw, 0.15), (maul + rimeclaw) + " trophies in " + SHARES + " shares, not about 15%");
		helper.assertTrue(within(maul, 0.075) && within(rimeclaw, 0.075), maul + " Glacier Mauls and " + rimeclaw + " Rimeclaws, not about 7.5% each");
		helper.assertTrue(within(mitten, 0.25), mitten + " Yeti Mittens, not about 25%");
		helper.assertTrue(within(crown, 0.2), crown + " Crowns, not about 20%");
		helper.succeed();
	}

	// ---------------------------------------------------------------- B-04: the charge and the column loop

	/**
	 * The column's reach, both sides of it, every time: passing 5.6 from the north-west column's centre he runs his full
	 * line, and again to the very same spot; passing 4.8 from it he stops 5.32 short, stunned (a step that would bring
	 * him within 5.2 of a column's centre stuns him).
	 */
	@GameTest(structure = HALL, padding = 8, maxTicks = 220)
	public void aGrazingChargeStunsHimOnlyWithinAColumnsReach(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		BlockPos origin = origin(helper);
		YetiKingEntity king = waiting(helper, origin);
		ServerPlayer foe = foeAt(helper, origin, 20.9, 5.0, 12.0, GameType.SURVIVAL);
		hunting(helper, king, () -> {
			charge(level, king, 20.9, 32.8, foe);
			helper.runAfterDelay(45, () -> {
				boolean passed = ran(king) && isAt(king, 20.9, 16.0);
				String firstEnd = where(king);
				Vec3 end = king.position();
				charge(level, king, 20.9, 32.8, foe);
				helper.runAfterDelay(45, () -> {
					boolean again = ran(king) && king.position().equals(end);
					String secondEnd = where(king);
					foe.snapTo(origin.getX() + 21.7, origin.getY() + 5.0, origin.getZ() + 12.0, 0.0F, 0.0F);
					charge(level, king, 21.7, 32.8, foe);
					helper.runAfterDelay(31, () -> {
						try {
							ending(level, king, () -> {
								helper.assertTrue(passed, "Passing 5.6 from the column he did not run his full line to (20.9, 16.0): " + firstEnd);
								helper.assertTrue(again, "The same charge again did not end where the first did, " + end + ": " + secondEnd);
								helper.assertTrue(king.stunnedTicks() > 0 && king.action() == YetiKingEntity.Action.STUNNED && king.attack() == null
										&& isAt(king, 21.7, 25.8) && fromColumn(king) > GlacierHall.COLUMN_RADIUS + GlacierHall.COLUMN_FOOT,
										"Passing 4.8 from the column he was not stunned at (21.7, 25.8): " + where(king));
							});
						} finally {
							foe.discard();
						}
						helper.succeed();
					});
				});
			});
		});
	}

	/**
	 * In the hall itself he never climbs (no gravity, so never on the ground, and only what is on the ground steps up):
	 * north at the dais he stops against its lowest step, south at the ramp against its first half step, and at the west
	 * den's corner he slides along the wall until his leash ends the run; never inside a block.
	 */
	@GameTest(structure = HALL, padding = 8, maxTicks = 260)
	public void inTheHallHisChargeStopsAtTheDaisTheRampAndTheDensWall(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		BlockPos origin = origin(helper);
		Map<BlockPos, BlockState> below = placeHall(helper, origin);
		YetiKingEntity king = waiting(helper, origin);
		ServerPlayer onDais = foeAt(helper, origin, 39.5, 7.0, 12.5, GameType.SURVIVAL);
		ServerPlayer onRamp = foeAt(helper, origin, 39.5, 11.0, 70.0, GameType.SURVIVAL);
		ServerPlayer byDen = foeAt(helper, origin, 12.5, 5.0, 30.5, GameType.SURVIVAL);
		hunting(helper, king, () -> {
			charge(level, king, 39.5, 30.0, onDais);
			helper.runAfterDelay(45, () -> {
				boolean dais = ran(king) && isAt(king, 39.5, 17.0) && level.noCollision(king);
				String daisEnd = where(king);
				charge(level, king, 39.5, 44.0, onRamp);
				helper.runAfterDelay(45, () -> {
					boolean ramp = ran(king) && isAt(king, 39.5, 58.0) && level.noCollision(king);
					String rampEnd = where(king);
					charge(level, king, 24.0, 33.0, byDen);
					helper.runAfterDelay(45, () -> {
						try {
							ending(level, king, () -> {
								helper.assertTrue(dais, "Charging north at the dais he did not stop against its lowest step at (39.5, 17.0): " + daisEnd);
								helper.assertTrue(ramp, "Charging south at the ramp he did not stop against its half step at (39.5, 58.0): " + rampEnd);
								helper.assertTrue(ran(king) && isAt(king, 12.0, 29.7286) && level.noCollision(king) && fromCentre(king) <= YetiKingEntity.LEASH - 1.0,
										"At the west den's corner he did not slide along the wall to his leash at (12.0, 29.7286): " + where(king));
							});
						} finally {
							clearHall(level, origin, below);
							onDais.discard();
							onRamp.discard();
							byDen.discard();
						}
						helper.succeed();
					});
				});
			});
		});
	}

	/** A glancing charge past the north-west column on its south side: if his body meets its foot he is stunned, not slid round it. */
	@GameTest(structure = HALL, padding = 8, maxTicks = 160)
	public void aChargeThatMeetsAColumnSouthOfItStunsHim(GameTestHelper helper) {
		grazing(helper, 16.09, 22.68, 32.47, 34.15, "south", false);
	}

	/**
	 * Past the same column's east side his line comes within a block of a column's reach (about 5.3 from its centre) but his
	 * body clears its foot: nothing stops him, so he runs his line on, unstunned (a glancing run is stunned only where it
	 * would meet the foot).
	 */
	@GameTest(structure = HALL, padding = 8, maxTicks = 160)
	public void aChargeThatClearsAColumnsFootRunsOnUnstunned(GameTestHelper helper) {
		grazing(helper, 36.33, 27.0, 23.47, 11.68, "east", true);
	}

	/**
	 * In the placed hall, his charge from template (x, z) at a foe at (foeX, foeZ), passing the north-west column on its
	 * {@code side}; if {@code clears}, his body clears its foot and he must run on unstunned.
	 */
	private static void grazing(GameTestHelper helper, double x, double z, double foeX, double foeZ, String side, boolean clears) {
		ServerLevel level = helper.getLevel();
		BlockPos origin = origin(helper);
		Map<BlockPos, BlockState> below = placeHall(helper, origin);
		YetiKingEntity king = waiting(helper, origin);
		ServerPlayer foe = foeAt(helper, origin, foeX, 5.0, foeZ, GameType.SURVIVAL);
		AtomicReference<String> met = new AtomicReference<>();
		helper.onEachTick(() -> {
			if (met.get() == null && king.attack() == CHARGE && king.horizontalCollision && fromColumn(king) < NEAR_COLUMN) {
				met.set(where(king));
			}
		});
		hunting(helper, king, () -> {
			charge(level, king, x, z, foe);
			helper.runAfterDelay(45, () -> {
				try {
					ending(level, king, () -> {
						helper.assertTrue(met.get() == null || king.stunnedTicks() > 0, "Passing the north-west column on its " + side
								+ " side his body met its foot at " + met.get() + " and he slid on unstunned, to " + where(king));
						helper.assertTrue(level.noCollision(king), "He ended inside the column: " + where(king));
						helper.assertTrue(king.stunnedTicks() > 0 ? fromColumn(king) > GlacierHall.COLUMN_RADIUS + GlacierHall.COLUMN_FOOT
								: ran(king) && fromColumn(king) > NEAR_COLUMN, "Neither stunned short of the column nor run clear of it: " + where(king));
						if (clears) {
							helper.assertTrue(met.get() == null && king.stunnedTicks() == 0, "Passing the north-west column on its " + side
									+ " side clear of its foot he was stopped (met it at " + met.get() + "): " + where(king));
						}
					});
				} finally {
					clearHall(level, origin, below);
					foe.discard();
				}
				helper.succeed();
			});
		});
	}

	/**
	 * His leash ends a charge the same way each run: out to it he stops 28.8 from the centre; from it, outward, on his first
	 * step (and the attack ends with its recovery); from it, inward, he runs his full line; a charge never carries him past
	 * it (so his bound back to the lake never has to).
	 */
	@GameTest(structure = HALL, padding = 8, maxTicks = 240)
	public void aChargeEndsAtHisLeashTheSameEveryRun(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		BlockPos origin = origin(helper);
		YetiKingEntity king = waiting(helper, origin);
		ServerPlayer foe = foeAt(helper, origin, 75.0, 5.0, 36.5, GameType.SURVIVAL);
		double[] farthest = {0.0};
		helper.onEachTick(() -> farthest[0] = Math.max(farthest[0], fromCentre(king)));
		hunting(helper, king, () -> {
			charge(level, king, 55.0, 36.5, foe);
			helper.runAfterDelay(42, () -> {
				boolean out = ran(king) && isAt(king, 68.3, 36.5) && fromCentre(king) <= YetiKingEntity.LEASH - 1.0;
				String outEnd = where(king);
				charge(level, king, 69.0, 36.5, foe);
				helper.runAfterDelay(22, () -> {
					boolean stopped = ran(king) && isAt(king, 69.0, 36.5);
					String stoppedAt = where(king);
					helper.runAfterDelay(10, () -> {
						boolean over = king.attack() == null;
						foe.snapTo(origin.getX() + 45.0, origin.getY() + 5.0, origin.getZ() + 36.5, 0.0F, 0.0F);
						charge(level, king, 69.0, 36.5, foe);
						helper.runAfterDelay(45, () -> {
							try {
								ending(level, king, () -> {
									helper.assertTrue(out, "Charging out he did not stop at his leash, (68.3, 36.5): " + outEnd);
									helper.assertTrue(stopped, "Charging outward from his leash he did not stop on his first step: " + stoppedAt);
									helper.assertTrue(over, "A charge stopped on its first step did not end with its recovery");
									helper.assertTrue(ran(king) && isAt(king, 52.2, 36.5), "Charging inward from his leash he did not run his full line to (52.2, 36.5): "
											+ where(king));
									helper.assertTrue(farthest[0] <= YetiKingEntity.LEASH + 1.0E-6, "A charge carried him " + farthest[0] + " from the centre");
								});
							} finally {
								foe.discard();
							}
							helper.succeed();
						});
					});
				});
			});
		});
	}

	/** In his fury a missed charge is followed at once by exactly one more, then his recovery. */
	@GameTest(structure = HALL, padding = 8, maxTicks = 180)
	public void inHisFuryAMissedChargeIsFollowedByExactlyOneMore(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		BlockPos origin = origin(helper);
		YetiKingEntity king = waiting(helper, origin);
		ServerPlayer foe = foeAt(helper, origin, 71.0, 5.0, 36.5, GameType.SURVIVAL);
		hunting(helper, king, () -> {
			king.startFury(level);
			charge(level, king, 45.0, 36.5, foe);
			helper.runAfterDelay(30, () -> {
				boolean running = king.attack() == CHARGE && king.action() == YetiKingEntity.Action.CHARGE;
				String runningAt = where(king);
				helper.runAfterDelay(18, () -> {
					boolean chained = king.attack() == CHARGE && king.attackTicks() >= 1 && king.attackTicks() < CHARGE.windup
							&& king.action() == YetiKingEntity.Action.CHARGE_WINDUP && isAt(king, 61.8, 36.5);
					String chainedAt = where(king) + ", tick " + king.attackTicks();
					helper.runAfterDelay(22, () -> {
						boolean second = ran(king) && isAt(king, 68.1, 36.5);
						String secondEnd = where(king);
						helper.runAfterDelay(12, () -> {
							try {
								ending(level, king, () -> {
									helper.assertTrue(running, "His first charge was not running at its 30th tick: " + runningAt);
									helper.assertTrue(chained, "Having run his full line to (61.8, 36.5) he did not wind up again at once: " + chainedAt);
									helper.assertTrue(second, "The second charge did not end at his leash, (68.1, 36.5): " + secondEnd);
									helper.assertTrue(king.attack() == null && king.action() == YetiKingEntity.Action.IDLE, "A third charge followed: " + where(king));
								});
							} finally {
								foe.discard();
							}
							helper.succeed();
						});
					});
				});
			});
		});
	}

	/** In his fury a bait who moves during the chained wind-up leads the second charge into a column; a stun never chains. */
	@GameTest(structure = HALL, padding = 8, maxTicks = 220)
	public void inHisFuryABaitLeadsTheChainedChargeIntoAColumn(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		BlockPos origin = origin(helper);
		YetiKingEntity king = waiting(helper, origin);
		ServerPlayer bait = foeAt(helper, origin, 39.5, 5.0, 56.0, GameType.SURVIVAL);
		hunting(helper, king, () -> {
			king.startFury(level);
			charge(level, king, 39.5, 36.5, bait);
			helper.runAfterDelay(48, () -> {
				boolean chained = king.attack() == CHARGE && king.action() == YetiKingEntity.Action.CHARGE_WINDUP && isAt(king, 39.5, 53.3);
				String chainedAt = where(king);
				bait.snapTo(origin.getX() + 60.0, origin.getY() + 5.0, origin.getZ() + 44.5, 0.0F, 0.0F);
				helper.runAfterDelay(26, () -> {
					boolean stunned = king.stunnedTicks() > 0 && king.action() == YetiKingEntity.Action.STUNNED && king.attack() == null
							&& isAt(king, 47.2188, 49.9865);
					String stunnedAt = where(king) + ", " + fromColumn(king) + " from a column";
					// His stun (from about the 68th tick) is over by the 129th; his global cooldown keeps him from any attack
					// until about the 141st: between, a stun that chained would show as an attack begun.
					helper.runAfterDelay(60, () -> {
						try {
							ending(level, king, () -> {
								helper.assertTrue(chained, "The chained wind-up did not start at (39.5, 53.3): " + chainedAt);
								helper.assertTrue(stunned, "The chained charge was not stunned at (47.2188, 49.9865), 5.30 from the south-east column: " + stunnedAt);
								helper.assertTrue(king.stunnedTicks() == 0 && king.attack() == null, "Out of his stun he began an attack at once (a stun chained): "
										+ where(king));
							});
						} finally {
							bait.discard();
						}
						helper.succeed();
					});
				});
			});
		});
	}

	/**
	 * Solo melee: baited from beyond a column, he crashes into it, and a blow while he reels takes a third more before armour
	 * than the same blow after (his armour 10 applies to a player's blow: Benchmark.afterArmour).
	 */
	@GameTest(structure = HALL, padding = 8, maxTicks = 200)
	public void soloMeleeBaitsAChargeIntoAColumnAndStrikesHimReeling(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		BlockPos origin = origin(helper);
		YetiKingEntity king = waiting(helper, origin);
		ServerPlayer bait = foeAt(helper, origin, 26.5, 5.0, 16.5, GameType.SURVIVAL);
		hunting(helper, king, () -> {
			charge(level, king, 26.5, 33.0, bait);
			helper.runAfterDelay(28, () -> {
				boolean reeling = king.stunnedTicks() > 0 && isAt(king, 26.5, 28.8);
				String reelingAt = where(king);
				bait.snapTo(origin.getX() + 24.5, origin.getY() + 5.0, origin.getZ() + 28.8, -90.0F, 0.0F);
				float before = king.getHealth();
				king.hurtServer(level, level.damageSources().playerAttack(bait), 6.0F);
				float stunnedBlow = before - king.getHealth();
				helper.runAfterDelay(62, () -> {
					try {
						ending(level, king, () -> {
							int armour = king.getArmorValue();
							int toughness = (int) king.getAttributeValue(Attributes.ARMOR_TOUGHNESS);
							boolean recovered = king.stunnedTicks() == 0 && king.action() != YetiKingEntity.Action.STUNNED;
							float healthy = king.getHealth();
							king.hurtServer(level, level.damageSources().playerAttack(bait), 6.0F);
							float plainBlow = healthy - king.getHealth();
							double stunnedExpected = Benchmark.afterArmour(6.0 * YetiKingEntity.STUN_TAKEN, armour, toughness);
							double plainExpected = Benchmark.afterArmour(6.0, armour, toughness);
							helper.assertTrue(reeling, "Baited from beyond the column he was not stunned at (26.5, 28.8): " + reelingAt);
							helper.assertTrue(Math.abs(stunnedBlow - stunnedExpected) < 0.01, "Reeling, a blow of 6 took " + stunnedBlow + ", not " + stunnedExpected);
							helper.assertTrue(recovered, "His stun did not end: " + where(king));
							helper.assertTrue(Math.abs(plainBlow - plainExpected) < 0.01 && stunnedBlow > plainBlow,
									"Recovered, a blow of 6 took " + plainBlow + ", not " + plainExpected);
							Encounter.Share share = king.encounter().share(bait);
							helper.assertTrue(share != null && share.damage() > 0.0F && king.participants(level).contains(bait), "The bait's blows were not credited");
						});
					} finally {
						bait.discard();
					}
					helper.succeed();
				});
			});
		});
	}

	/** Solo caster: the bait is the caster, who steps clear and lances him reeling for a third more (the Lance bypasses armour). */
	@GameTest(structure = HALL, padding = 8, maxTicks = 160)
	public void soloCasterBaitsAChargeIntoAColumnAndLancesHimReeling(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		BlockPos origin = origin(helper);
		YetiKingEntity king = waiting(helper, origin);
		ServerPlayer caster = foeAt(helper, origin, 26.5, 5.0, 16.5, GameType.SURVIVAL);
		master(caster);
		hunting(helper, king, () -> {
			charge(level, king, 26.5, 33.0, caster);
			helper.runAfterDelay(28, () -> {
				try {
					ending(level, king, () -> {
						helper.assertTrue(king.stunnedTicks() > 0 && isAt(king, 26.5, 28.8), "Baited from beyond the column he was not stunned: " + where(king));
						float before = king.getHealth();
						lance(caster, king, 34.5, 28.8);
						float lanced = before - king.getHealth();
						helper.assertTrue(Math.abs(lanced - 5.0 * YetiKingEntity.STUN_TAKEN) < 0.01, "Reeling, the Lance took " + lanced);
						helper.assertTrue(ConcordanceProgress.currentFocus(caster) == FocusPool.MAX - 4, "The Lance's Focus: " + ConcordanceProgress.currentFocus(caster));
						Encounter.Share share = king.encounter().share(caster);
						helper.assertTrue(share != null && share.damage() > 0.0F, "The Lance was not credited to its caster");
					});
				} finally {
					caster.discard();
				}
				helper.succeed();
			});
		});
	}

	/**
	 * A party of two: the bait side-steps 3 blocks during the wind-up, the charge runs on into the column, trampling the caster
	 * standing on its line once; the bait strikes him reeling, then the caster lances him; both are credited.
	 */
	@GameTest(structure = HALL, padding = 8, maxTicks = 300)
	public void aPartyOfTwoSolvesTheColumnLoop(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		BlockPos origin = origin(helper);
		YetiKingEntity king = waiting(helper, origin);
		ServerPlayer bait = foeAt(helper, origin, 29.5, 5.0, 16.5, GameType.CREATIVE);
		ServerPlayer caster = foeAt(helper, origin, 26.5, 5.0, 31.0, GameType.CREATIVE);
		master(caster);
		helper.runAfterDelay(LOADING_TICKS, () -> {   // only a loaded player can be hurt
			bait.setGameMode(GameType.SURVIVAL);
			caster.setGameMode(GameType.SURVIVAL);
			hunting(helper, king, () -> {
				charge(level, king, 26.5, 33.0, bait);
				helper.runAfterDelay(5, () -> bait.snapTo(origin.getX() + 26.5, origin.getY() + 5.0, origin.getZ() + 16.5, 0.0F, 0.0F));
				helper.runAfterDelay(28, () -> {
					boolean reeling = king.stunnedTicks() > 0 && isAt(king, 26.5, 28.8);
					String reelingAt = where(king);
					float trampled = caster.getHealth();
					boolean baitWhole = bait.getHealth() == bait.getMaxHealth();
					bait.snapTo(origin.getX() + 24.5, origin.getY() + 5.0, origin.getZ() + 28.8, -90.0F, 0.0F);
					float before = king.getHealth();
					king.hurtServer(level, level.damageSources().playerAttack(bait), 6.0F);
					float struck = before - king.getHealth();
					helper.runAfterDelay(12, () -> {
						try {
							ending(level, king, () -> {
								float healthy = king.getHealth();
								lance(caster, king, 34.5, 28.8);
								float lanced = healthy - king.getHealth();
								double expected = Benchmark.afterArmour(6.0 * YetiKingEntity.STUN_TAKEN, king.getArmorValue(),
										(int) king.getAttributeValue(Attributes.ARMOR_TOUGHNESS));
								helper.assertTrue(reeling, "The side-stepping bait did not lead him into the column: " + reelingAt);
								helper.assertTrue(trampled < caster.getMaxHealth() && baitWhole, "The caster on his line was not trampled, or the bait was");
								helper.assertTrue(Math.abs(struck - expected) < 0.01, "Reeling, the bait's blow of 6 took " + struck + ", not " + expected);
								helper.assertTrue(Math.abs(lanced - 5.0 * YetiKingEntity.STUN_TAKEN) < 0.01 && caster.getHealth() == trampled,
										"Reeling, the Lance took " + lanced + "; or the caster was struck again");
								Encounter.Share baitShare = king.encounter().share(bait);
								Encounter.Share casterShare = king.encounter().share(caster);
								helper.assertTrue(baitShare != null && baitShare.damage() > 0.0F && casterShare != null && casterShare.damage() > 0.0F
										&& king.participants(level).containsAll(List.of(bait, caster)), "The party was not both credited");
							});
						} finally {
							bait.discard();
							caster.discard();
						}
						helper.succeed();
					});
				});
			});
		});
	}

	// ---------------------------------------------------------------- B-04: safe footing, cleanup and frost

	/**
	 * His Hunt slam bares only drift snow round where his foe stood, beside the trampled route; left alone, his reset drifts it
	 * all back (before its own timer would), his kin flee, and the lake is exactly as laid.
	 */
	@GameTest(structure = HALL, padding = 8, maxTicks = 320)
	public void leftAloneAfterHisSlamHisLakeIsAsItWas(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		BlockPos origin = origin(helper);
		YetiKingEntity king = waiting(helper, origin);
		Map<BlockPos, BlockState> laid = lake(level, king);
		dens(level, origin);
		ServerPlayer foe = shielded(helper, king, 0.5, 6.0);
		YetiKingEntity.Attack slam = YetiKingEntity.Attack.GROUND_SLAM;
		hunting(helper, king, () -> {
			YetiKingEntity.Phase hunted = king.phase();
			Vec3 centre = king.centre();
			king.teleportTo(centre.x, king.floorY(), centre.z);
			king.callKin(level, YetiKingEntity.KIN);
			List<UUID> kin = king.whelps();
			Vec3 stood = foe.position();
			king.begin(level, slam, foe);
			helper.runAfterDelay(slam.windup + slam.active + 2, () -> {
				Vec3 landed = king.position();
				int bared = changed(level, laid).size();
				boolean onlyDrift = onlyDriftBared(level, laid);
				long route = trampledWithin(laid, landed, YetiKingEntity.RING);
				int patches = king.baredPatches();
				foe.setGameMode(GameType.CREATIVE);   // no longer one he may fight: he is left alone
				helper.runAfterDelay(YetiKingEntity.ABANDON_TICKS + 10, () -> {
					try {
						ending(level, king, () -> {
							List<BlockPos> left = changed(level, laid);
							helper.assertTrue(hunted == YetiKingEntity.Phase.HUNT, "He was not hunting: " + hunted);
							helper.assertTrue(Math.hypot(landed.x - stood.x, landed.z - stood.z) < 0.5, "He did not come down where his foe stood: " + landed);
							helper.assertTrue(patches == 1 && bared > 0, "His slam bared " + bared + " cells in " + patches + " patches");
							helper.assertTrue(route > 0, "No trampled snow lay in his ring");
							helper.assertTrue(onlyDrift, "His slam bared more than drift snow");
							helper.assertTrue(king.phase() == YetiKingEntity.Phase.WAITING, "Left alone he is not back on his throne: " + king.phase());
							helper.assertTrue(king.baredPatches() == 0 && left.isEmpty(), "Left alone, his lake is not as laid at " + firstFew(left));
							for (UUID id : kin) {
								helper.assertTrue(!(level.getEntity(id) instanceof YetiWhelpEntity whelp) || whelp.isRemoved(), "A whelp outlived his reset");
							}
						});
					} finally {
						foe.discard();
					}
					helper.succeed();
				});
			});
		});
	}

	/** Left alone, his standing spikes, a block of ice in the air and falling icicles are all gone at once. */
	@GameTest(structure = HALL, padding = 8, maxTicks = 180)
	public void leftAloneHisIceIsGoneAtOnce(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		BlockPos origin = origin(helper);
		YetiKingEntity king = waiting(helper, origin);
		floor(level, origin, 37, 42, 42, 47);   // the near one, 8 south of the centre
		floor(level, origin, 37, 14, 42, 19);   // the far one, 20 north
		ServerPlayer near = shielded(helper, king, 0.5, 8.0);
		ServerPlayer far = shielded(helper, king, 0.5, -20.0);
		YetiKingEntity.Attack spikes = YetiKingEntity.Attack.GLACIAL_SPIKES;
		YetiKingEntity.Attack boulder = YetiKingEntity.Attack.BOULDER_THROW;
		YetiKingEntity.Attack icicles = YetiKingEntity.Attack.ICICLE_FALL;
		hunting(helper, king, () -> {
			Vec3 centre = king.centre();
			king.teleportTo(centre.x, king.floorY(), centre.z);
			king.begin(level, spikes, near);
			helper.runAfterDelay(spikes.windup + 1, () -> {
				king.begin(level, boulder, far);   // the spikes raised still stand (a spike lives 24 ticks)
				helper.runAfterDelay(boulder.windup + 1, () -> {   // the block is some 22 ticks in the air to the far one
					List<GlacialSpikeEntity> standing = level.getEntitiesOfClass(GlacialSpikeEntity.class, arena(origin));
					List<HurledBoulderEntity> flying = level.getEntitiesOfClass(HurledBoulderEntity.class, arena(origin));
					king.reset(level);
					boolean spikesGone = standing.stream().allMatch(Entity::isRemoved);
					boolean boulderGone = flying.stream().allMatch(Entity::isRemoved);
					king.wake(level);   // the same tick, before his own check for intruders could
					helper.runAfterDelay(YetiKingEntity.WAKE_TICKS + 2, () -> {
						king.teleportTo(centre.x, king.floorY(), centre.z);
						king.begin(level, icicles, near);
						helper.runAfterDelay(icicles.windup + 3, () -> {   // one or two icicles falling (a 14-tick fall)
							List<FallingIcicleEntity> falling = level.getEntitiesOfClass(FallingIcicleEntity.class, arena(origin));
							king.reset(level);
							try {
								ending(level, king, () -> {
									helper.assertTrue(!standing.isEmpty() && !flying.isEmpty() && !falling.isEmpty(),
											standing.size() + " spikes, " + flying.size() + " blocks of ice, " + falling.size() + " icicles as he was left alone");
									helper.assertTrue(spikesGone, "A glacial spike outlived his reset");
									helper.assertTrue(boulderGone, "A block of ice in the air outlived his reset");
									helper.assertTrue(falling.stream().allMatch(Entity::isRemoved), "A falling icicle outlived his reset");
									helper.assertTrue(king.phase() == YetiKingEntity.Phase.WAITING, "He is not back on his throne: " + king.phase());
								});
							} finally {
								near.discard();
								far.discard();
							}
							helper.succeed();
						});
					});
				});
			});
		});
	}

	/** His fury's wider ring and a second overlapping slam bare only drift snow; as he falls the lake comes back exactly, his kin and spikes gone. */
	@GameTest(structure = HALL, padding = 8, maxTicks = 120)
	public void whenHeFallsHisSnowDriftsBackAndHisKinAndIceAreGone(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		BlockPos origin = origin(helper);
		YetiKingEntity king = waiting(helper, origin);
		Map<BlockPos, BlockState> laid = lake(level, king);
		dens(level, origin);
		ServerPlayer foe = shielded(helper, king, 0.5, 6.0);
		YetiKingEntity.Attack spikes = YetiKingEntity.Attack.GLACIAL_SPIKES;
		hunting(helper, king, () -> {
			Vec3 centre = king.centre();
			king.teleportTo(centre.x, king.floorY(), centre.z);
			king.startFury(level);
			double ring = king.ring();
			king.slamLands(level);
			boolean wider = changed(level, laid).stream().anyMatch(cell -> Math.hypot(cell.getX() + 0.5 - centre.x, cell.getZ() + 0.5 - centre.z) > YetiKingEntity.RING);
			long route = trampledWithin(laid, centre, YetiKingEntity.FURY_RING);
			king.teleportTo(centre.x + 6.0, king.floorY(), centre.z);
			king.slamLands(level);   // a second slam over half the first
			int patches = king.baredPatches();
			boolean onlyDrift = onlyDriftBared(level, laid);
			king.teleportTo(centre.x, king.floorY(), centre.z);
			king.callKin(level, YetiKingEntity.KIN);
			List<UUID> kin = king.whelps();
			king.begin(level, spikes, foe);   // spikes have no owner of their own: only his dismissal takes them
			helper.runAfterDelay(spikes.windup + 3, () -> {
				List<GlacialSpikeEntity> standing = level.getEntitiesOfClass(GlacialSpikeEntity.class, arena(origin));
				king.hurtServer(level, level.damageSources().genericKill(), Float.MAX_VALUE);   // as YetiKingClientGameTests fells him
				List<BlockPos> left = changed(level, laid);
				int patchesLeft = king.baredPatches();
				boolean spikesGone = standing.stream().allMatch(Entity::isRemoved);
				boolean kinGone = kin.stream().allMatch(id -> !(level.getEntity(id) instanceof YetiWhelpEntity whelp) || whelp.isRemoved());
				helper.runAfterDelay(25, () -> {
					try {
						ending(level, king, () -> {
							helper.assertTrue(ring == YetiKingEntity.FURY_RING && wider, "In his fury his slam bared no wider a ring");
							helper.assertTrue(route > 0, "No trampled snow lay in his fury's ring");
							helper.assertTrue(patches == 2 && onlyDrift, patches + " patches, or more than drift snow bared");
							helper.assertTrue(!standing.isEmpty() && spikesGone, standing.size() + " spikes stood; one outlived him");
							helper.assertTrue(kin.size() == YetiKingEntity.KIN && kinGone, "His kin outlived him");
							helper.assertTrue(!king.isAlive(), "He did not fall");
							helper.assertTrue(patchesLeft == 0 && left.isEmpty(), "As he fell his lake is not as laid at " + firstFew(left));
						});
					} finally {
						foe.discard();
					}
					helper.succeed();
				});
			});
		});
	}

	/**
	 * Loaded again mid-fight (an emulated unload: each entity offered to save(), then removed as a chunk's unloading removes
	 * it): only he is written (his whelps, blocks of ice, icicles and spikes never can be); loaded, he is the king he was,
	 * hunting, and on his first tick the snow he had bared drifts back; none of his kin or ice comes back. The real unload
	 * is in YetiKingClientGameTests.
	 */
	@GameTest(structure = HALL, padding = 8, maxTicks = 100)
	public void loadedAgainMidFightHeDriftsHisSnowBackAndBringsNoKin(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		BlockPos origin = origin(helper);
		YetiKingEntity king = waiting(helper, origin);
		Map<BlockPos, BlockState> laid = lake(level, king);
		dens(level, origin);
		ServerPlayer foe = shielded(helper, king, 0.5, 6.0);
		hunting(helper, king, () -> {
			Vec3 centre = king.centre();
			king.teleportTo(centre.x, king.floorY(), centre.z);
			king.slamLands(level);
			king.callKin(level, YetiKingEntity.KIN);
			king.begin(level, YetiKingEntity.Attack.BOULDER_THROW, foe);   // some 8 ticks of flight
			helper.runAfterDelay(YetiKingEntity.Attack.BOULDER_THROW.windup + 1, () -> {
				List<Entity> inside = level.getEntitiesOfClass(Entity.class, arena(origin), entity -> !(entity instanceof Player));
				Map<Entity, CompoundTag> written = new HashMap<>();
				for (Entity entity : inside) {
					TagValueOutput out = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, level.registryAccess());
					if (entity.save(out)) {
						written.put(entity, out.buildResult());
					}
				}
				long whelps = inside.stream().filter(YetiWhelpEntity.class::isInstance).count();
				long boulders = inside.stream().filter(HurledBoulderEntity.class::isInstance).count();
				float health = king.getHealth();
				inside.forEach(entity -> entity.remove(Entity.RemovalReason.UNLOADED_TO_CHUNK));
				CompoundTag his = written.get(king);
				YetiKingEntity loaded = new YetiKingEntity(JugcraftYeti.YETI_KING, level);
				boolean added = false;
				if (his != null) {
					loaded.load(TagValueInput.create(ProblemReporter.DISCARDING, level.registryAccess(), his));
					added = level.addFreshEntity(loaded);
				}
				boolean entered = added;
				int patchesLoaded = loaded.baredPatches();   // before his first tick
				boolean bareAsLoaded = !changed(level, laid).isEmpty();
				helper.runAfterDelay(3, () -> {
					try {
						ending(level, loaded, () -> {
							List<BlockPos> left = changed(level, laid);
							helper.assertTrue(his != null, "He was not written with his hall's chunks");
							helper.assertTrue(written.size() == 1, "Written with him: " + written.keySet());
							helper.assertTrue(!JugcraftYeti.YETI_WHELP.canSerialize() && !JugcraftYeti.HURLED_BOULDER.canSerialize()
									&& !JugcraftYeti.FALLING_ICICLE.canSerialize() && !JugcraftYeti.GLACIAL_SPIKE.canSerialize(),
									"His whelps, blocks of ice, icicles or spikes may be written with a chunk (each is noSave)");
							helper.assertTrue(whelps == YetiKingEntity.KIN && boulders == 1, whelps + " whelps and " + boulders + " blocks of ice as it unloaded");
							helper.assertTrue(entered, "The level refused him loaded again (his UUID still known after the unload)");
							helper.assertTrue(loaded.getUUID().equals(king.getUUID()) && loaded.getHealth() == health, "Loaded, he is not the king he was");
							helper.assertTrue(patchesLoaded == 1 && bareAsLoaded, "What he had bared did not come back with him");
							helper.assertTrue(loaded.phase() == YetiKingEntity.Phase.HUNT, "Loaded, he is not hunting but " + loaded.phase());
							helper.assertTrue(loaded.baredPatches() == 0 && left.isEmpty(), "Loaded, the snow did not drift back at " + firstFew(left));
							helper.assertTrue(loaded.whelps().isEmpty() && level.getEntitiesOfClass(YetiWhelpEntity.class, arena(origin)).isEmpty()
									&& level.getEntitiesOfClass(HurledBoulderEntity.class, arena(origin)).isEmpty(), "His kin or his ice came back with him");
						});
					} finally {
						foe.discard();
					}
					helper.succeed();
				});
			});
		});
	}

	/**
	 * Loaded again while his whelps and his ice stay where they were, in chunks that did not unload (his chunk's unload
	 * alone, emulated as in loadedAgainMidFightHeDriftsHisSnowBackAndBringsNoKin): he counts none of them his, so each is
	 * gone on its next tick instead of serving him again. Before, an icicle or a block of ice found him again by his UUID
	 * and fell or flew on, a spike stood out its time and a whelp served on; YetiKingClientGameTests found ice so after a
	 * real unload.
	 */
	@GameTest(structure = HALL, padding = 8, maxTicks = 100)
	public void loadedAgainHeCountsNoKinOrIceFromBefore(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		BlockPos origin = origin(helper);
		YetiKingEntity king = waiting(helper, origin);
		dens(level, origin);
		ServerPlayer foe = shielded(helper, king, 0.5, 6.0);
		hunting(helper, king, () -> {
			king.callKin(level, YetiKingEntity.KIN);
			Vec3 centre = king.centre();
			GlacialSpikeEntity spike = GlacialSpikeEntity.burst(level, king, new Vec3(centre.x + 6.0, king.floorY(), centre.z - 6.0));
			FallingIcicleEntity icicle = FallingIcicleEntity.drop(level, king, new Vec3(centre.x - 6.0, king.floorY(), centre.z - 6.0));
			HurledBoulderEntity boulder = HurledBoulderEntity.hurl(level, king, foe);   // some 8 ticks of flight
			List<YetiWhelpEntity> kin = level.getEntitiesOfClass(YetiWhelpEntity.class, arena(origin));
			TagValueOutput out = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, level.registryAccess());
			boolean written = king.save(out);
			king.remove(Entity.RemovalReason.UNLOADED_TO_CHUNK);   // his chunk alone; the rest stay loaded
			YetiKingEntity loaded = new YetiKingEntity(JugcraftYeti.YETI_KING, level);
			loaded.load(TagValueInput.create(ProblemReporter.DISCARDING, level.registryAccess(), out.buildResult()));
			boolean added = written && level.addFreshEntity(loaded);
			helper.runAfterDelay(2, () -> {
				try {
					ending(level, loaded, () -> {
						helper.assertTrue(added, "He was not loaded again");
						helper.assertTrue(kin.size() == YetiKingEntity.KIN, kin.size() + " whelps were out as he unloaded");
						helper.assertTrue(kin.stream().allMatch(Entity::isRemoved) && loaded.whelps().isEmpty(),
								"A whelp from before he unloaded served him again");
						helper.assertTrue(spike.isRemoved() && icicle.isRemoved() && boulder.isRemoved(), "His ice from before he unloaded went on: spike "
								+ !spike.isRemoved() + ", icicle " + !icicle.isRemoved() + ", block of ice " + !boulder.isRemoved());
					});
				} finally {
					kin.forEach(Entity::discard);
					foe.discard();
				}
				helper.succeed();
			});
		});
	}

	/**
	 * Against his frost: wool (knitwear) and leather stop the freeze's damage (vanilla's canFreeze) but not his frost (chill
	 * ignores worn gear) nor its slowness, which they share with bare skin and thaw alike; only the Yeti Mitten stops both.
	 */
	@GameTest(maxTicks = 120)
	public void woolAndLeatherStopTheFreezeButNotHisFrost(GameTestHelper helper) {
		ServerPlayer bare = onStone(helper, new BlockPos(1, 2, 1));
		ServerPlayer leather = onStone(helper, new BlockPos(3, 2, 1));
		ServerPlayer wool = onStone(helper, new BlockPos(5, 2, 1));
		ServerPlayer mitten = onStone(helper, new BlockPos(1, 2, 4));
		leather.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.LEATHER_CHESTPLATE));
		wool.setItemSlot(EquipmentSlot.CHEST, Knitting.garment(Knitwear.SWEATER, Knitting.UNDYED));
		mitten.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(JugcraftYeti.YETI_MITTEN));
		List<ServerPlayer> all = List.of(bare, leather, wool, mitten);
		boolean[] canFreeze = {bare.canFreeze(), leather.canFreeze(), wool.canFreeze(), mitten.canFreeze()};
		for (ServerPlayer player : all) {
			YetiKingEntity.chill(player, YetiKingEntity.BLIZZARD_FROST, false);
		}
		int[] blizzard = all.stream().mapToInt(ServerPlayer::getTicksFrozen).toArray();
		helper.runAfterDelay(LOADING_TICKS, () -> {   // only a loaded player can be hurt
			try {
				for (ServerPlayer player : all) {
					YetiKingEntity.chill(player, player.getTicksRequiredToFreeze() * 2, true);   // his breath, stacked to its cap
				}
				boolean through = bare.isFullyFrozen() && leather.isFullyFrozen() && wool.isFullyFrozen() && !mitten.isFreezing();
				float[] health = new float[all.size()];
				for (int i = 0; i < all.size(); i++) {
					ServerPlayer player = all.get(i);
					health[i] = player.getHealth();
					player.tickCount += Math.floorMod(-player.tickCount, 40);   // onto vanilla's freeze beat
					player.aiStep();                                            // mock players do not tick by themselves
				}
				AttributeModifier[] slow = all.stream().map(player -> player.getAttribute(Attributes.MOVEMENT_SPEED).getModifier(FROST_SLOW))
						.toArray(AttributeModifier[]::new);
				helper.assertTrue(blizzard[0] == YetiKingEntity.BLIZZARD_FROST && blizzard[1] == YetiKingEntity.BLIZZARD_FROST
						&& blizzard[2] == YetiKingEntity.BLIZZARD_FROST && blizzard[3] == 0, "His frost: " + Arrays.toString(blizzard));
				helper.assertTrue(canFreeze[0] && !canFreeze[1] && !canFreeze[2] && canFreeze[3], "Freeze-immune: " + Arrays.toString(canFreeze));
				helper.assertTrue(through, "His breath did not freeze bare skin, leather and wool through");
				helper.assertTrue(health[0] - bare.getHealth() == 1.0F, "Frozen through, bare skin took " + (health[0] - bare.getHealth()));
				helper.assertTrue(leather.getHealth() == health[1] && wool.getHealth() == health[2] && mitten.getHealth() == health[3],
						"Leather, wool or the mitten's wearer was harmed by the freeze");
				helper.assertTrue(slow[0] != null && slow[1] != null && slow[2] != null && slow[0].amount() < 0.0
						&& slow[1].amount() == slow[0].amount() && slow[2].amount() == slow[0].amount(), "Not slowed alike: " + Arrays.toString(slow));
				helper.assertTrue(slow[3] == null, "The mitten's wearer was slowed");
				helper.assertTrue(leather.getTicksFrozen() == bare.getTicksFrozen() && wool.getTicksFrozen() == bare.getTicksFrozen(), "Leather or wool thawed faster");
			} finally {
				all.forEach(ServerPlayer::discard);
			}
			helper.succeed();
		});
	}

	/**
	 * End-gun Levitation (GunShots gives the game's own, {@value GunShots#LIFT_TICKS} ticks at its first level, from the
	 * shooter, to whatever a bullet hurts) lifts neither him nor his kin: hunting he stays on his lake and his whelps on
	 * their dens' floors for the whole spell and after it, and the End gun's shot that fells him floats him no higher as he
	 * dies. The whelps are given the spell alone, as GunShots gives it after a hit: a bullet's knockback (he has none) is
	 * any blow's, not the spell's. The shooter is in creative, so neither he nor his kin go for anyone; and his whelps are
	 * held where they came out (no speed: the spell's upward push does not depend on it), so a whelp strolling off its
	 * den's one-block floor and jumping back cannot pass for a rise.
	 */
	@GameTest(structure = HALL, padding = 8, maxTicks = 160)
	public void endGunLevitationLiftsNeitherHimNorHisKin(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		BlockPos origin = origin(helper);
		YetiKingEntity king = waiting(helper, origin);
		dens(level, origin);
		ServerPlayer shooter = player(helper, king, 0.5, 6.0);
		shooter.setGameMode(GameType.CREATIVE);   // no one he or his kin may fight
		DamageSource bullet = new DamageSource(level.registryAccess().lookupOrThrow(Registries.DAMAGE_TYPE).getOrThrow(JugcraftGuns.BULLET),
				shooter, shooter);
		// Him (0) and his whelps, from when the spell is given: where each stood, and how far above it each has been since.
		LivingEntity[] watched = new LivingEntity[1 + YetiKingEntity.KIN];
		double[] stood = new double[watched.length];
		double[] rose = new double[watched.length];
		boolean[] dying = {false};
		double[] roseDying = {0.0};
		helper.onEachTick(() -> {
			for (int i = 0; i < watched.length; i++) {
				if (watched[i] != null && !watched[i].isRemoved()) {
					rose[i] = Math.max(rose[i], watched[i].getY() - stood[i]);
				}
			}
			if (dying[0] && !king.isRemoved()) {
				roseDying[0] = Math.max(roseDying[0], king.getY() - king.floorY());
			}
		});
		hunting(helper, king, () -> {
			YetiKingEntity.Phase hunted = king.phase();
			int called = king.callKin(level, YetiKingEntity.KIN);
			for (UUID id : king.whelps()) {
				if (level.getEntity(id) instanceof YetiWhelpEntity whelp) {
					whelp.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(0.0);
				}
			}
			helper.runAfterDelay(5, () -> {   // his whelps stand on the dens' floors
				boolean[] took = new boolean[watched.length];
				boolean[] kept = new boolean[watched.length];
				watched[0] = king;
				stood[0] = king.floorY();
				took[0] = levitate(king, shooter);
				kept[0] = king.hasEffect(MobEffects.LEVITATION);
				List<UUID> kin = king.whelps();
				for (int i = 0; i < kin.size() && i + 1 < watched.length; i++) {
					if (level.getEntity(kin.get(i)) instanceof YetiWhelpEntity whelp) {
						watched[i + 1] = whelp;
						stood[i + 1] = whelp.getY();
						took[i + 1] = levitate(whelp, shooter);
						kept[i + 1] = whelp.hasEffect(MobEffects.LEVITATION);
					}
				}
				helper.runAfterDelay(GunShots.LIFT_TICKS + 10, () -> {
					watched[0] = null;
					YetiKingEntity.Phase spelled = king.phase();
					// The End gun's shot that fells him, as GunShots lands it: the bullet, then the spell if it hurt.
					boolean felled = king.hurtServer(level, bullet, king.getMaxHealth() * 4.0F);
					boolean tookDying = felled && levitate(king, shooter);
					dying[0] = true;
					helper.runAfterDelay(25, () -> {
						try {
							ending(level, king, () -> {
								helper.assertTrue(hunted == YetiKingEntity.Phase.HUNT && spelled == YetiKingEntity.Phase.HUNT,
										"He was not hunting throughout: " + hunted + ", then " + spelled);
								helper.assertTrue(called == YetiKingEntity.KIN && watched[1] != null && watched[2] != null, called + " whelps came out of the dens");
								for (int i = 0; i < watched.length; i++) {
									helper.assertTrue(!took[i] && !kept[i], (i == 0 ? "He" : "A whelp") + " took End-gun Levitation (given: " + took[i]
											+ ", held after: " + kept[i] + ")");
								}
								helper.assertTrue(rose[0] <= 1.0E-6, "Hunting under Levitation he rose " + rose[0] + " off his lake (the spell "
										+ (took[0] ? "took" : "was refused") + ")");
								for (int i = 1; i < watched.length; i++) {
									helper.assertTrue(rose[i] <= 1.0E-3, "Under Levitation a whelp rose " + rose[i] + " off its den's floor (the spell "
											+ (took[i] ? "took" : "was refused") + ")");
								}
								helper.assertTrue(felled && !king.isAlive(), "The End gun's shot did not fell him");
								helper.assertTrue(!tookDying, "Felled by an End gun, he took its Levitation");
								helper.assertTrue(roseDying[0] <= 1.0E-6, "Felled by an End gun, he rose " + roseDying[0] + " off his lake as he died");
							});
						} finally {
							shooter.discard();
						}
						helper.succeed();
					});
				});
			});
		});
	}

	/**
	 * Concordance Chunk B (B-09, B-P20, B-P30): only a Brace lessens his push. His slam's 1.4 across and 0.4 up is the same
	 * on a player with knockback resistance 1 from elsewhere, and half on a braced one, whoever put the Brace on (a friend,
	 * they themselves, the Water Bangle); a second push that tick is braced too; the next tick the Brace is gone.
	 */
	@GameTest(structure = HALL, padding = 8, maxTicks = 20)
	public void aBracedPlayerIsPushedHalfAsFarOnce(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		BlockPos origin = origin(helper);
		YetiKingEntity king = waiting(helper, origin);
		ServerPlayer bare = offLake(helper, king, 4.5, 0.0);
		ServerPlayer friend = offLake(helper, king, -4.5, 0.0);
		ServerPlayer own = offLake(helper, king, 0.0, 4.5);
		ServerPlayer wearer = offLake(helper, king, 0.0, -4.5);
		ServerPlayer caster = offLake(helper, king, 0.0, 12.0);
		List<ServerPlayer> pushed = List.of(bare, friend, own, wearer);
		List<ServerPlayer> braces = List.of(friend, own, wearer);
		Map<ServerPlayer, Vec3> stand = new HashMap<>();
		pushed.forEach(player -> stand.put(player, player.position()));
		steadfast(bare);
		steadfast(friend);
		EffectSpec brace = invoked("jugcraft:brace");
		boolean braced = cast(caster, brace, friend).applied() && cast(own, brace, own).applied() && RimeGear.brace(wearer).applied();
		double resisting = bare.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE);
		Map<ServerPlayer, double[]> first = new HashMap<>();
		Map<ServerPlayer, double[]> second = new HashMap<>();
		Map<ServerPlayer, Vec3> before = new HashMap<>();
		pushed.forEach(player -> before.put(player, player.getDeltaMovement()));
		slam(level, king);
		pushed.forEach(player -> {
			first.put(player, moved(player, before.get(player)));
			before.put(player, player.getDeltaMovement());
		});
		slam(level, king);
		pushed.forEach(player -> second.put(player, moved(player, before.get(player))));
		boolean heldOn = braces.stream().allMatch(player -> player.hasEffect(Brace.BRACED));
		double half = 1.0 - Brace.RESISTANCE;
		helper.runAfterDelay(1, () -> {
			ending(level, king, () -> {
				try {
					helper.assertTrue(braced, "A friend's, one's own and the Water Bangle's Brace did not all take");
					helper.assertTrue(near(resisting, 1.0), "The bare player does not have knockback resistance 1: " + resisting);
					for (Map<ServerPlayer, double[]> push : List.of(first, second)) {
						helper.assertTrue(near(push.get(bare)[0], YetiKingEntity.SLAM_KNOCKBACK) && near(push.get(bare)[1], 0.4),
								"Knockback resistance from elsewhere lessened his push: " + Arrays.toString(push.get(bare)));
						for (ServerPlayer player : braces) {
							helper.assertTrue(near(push.get(player)[0], YetiKingEntity.SLAM_KNOCKBACK * half) && near(push.get(player)[1], 0.4 * half),
									"A braced player was not pushed half as far, both pushes of the tick: " + Arrays.toString(push.get(player)));
						}
					}
					helper.assertTrue(heldOn, "A spent Brace did not brace the rest of its tick");
					helper.assertTrue(braces.stream().noneMatch(player -> player.hasEffect(Brace.BRACED)), "A spent Brace outlasted its tick");
					pushed.forEach(player -> back(player, stand.get(player)));
					slam(level, king);
					for (ServerPlayer player : pushed) {
						double[] whole = moved(player, Vec3.ZERO);
						helper.assertTrue(near(whole[0], YetiKingEntity.SLAM_KNOCKBACK) && near(whole[1], 0.4),
								"With its Brace gone, a player was not pushed the whole way: " + Arrays.toString(whole));
					}
				} finally {
					List.of(bare, friend, own, wearer, caster).forEach(ServerPlayer::discard);
				}
			});
			helper.succeed();
		});
	}

	/**
	 * Concordance Chunk B (B-09, B-P12, B-P30): a blow of his and the push it brings are braced once, not twice. His swipe
	 * pushes a bare player 1 across and 0.3 up and a braced one 0.5 and 0.15, not a quarter; a glacial spike lifts a bare
	 * player 0.8 and a braced one 0.4. Both have knockback resistance 1 from elsewhere, so the blow's own knockback moves
	 * neither and only his push is measured.
	 */
	@GameTest(structure = HALL, padding = 8, maxTicks = 140)
	public void hisBlowAndSpikeAreBracedOnceNotTwice(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		BlockPos origin = origin(helper);
		YetiKingEntity king = waiting(helper, origin);
		ServerPlayer bare = offLake(helper, king, 1.5, 2.5);
		ServerPlayer braced = offLake(helper, king, -1.5, 2.5);
		ServerPlayer caster = offLake(helper, king, 0.0, 12.0);
		List<ServerPlayer> all = List.of(bare, braced, caster);
		all.forEach(player -> player.setGameMode(GameType.CREATIVE));   // their loading waited out, as hisSwipeStrikesInFrontNotBehind
		EffectSpec brace = invoked("jugcraft:brace");
		double half = 1.0 - Brace.RESISTANCE;
		helper.runAfterDelay(LOADING_TICKS, () -> {
			all.forEach(player -> player.setGameMode(GameType.SURVIVAL));
			steadfast(bare);
			steadfast(braced);
			boolean first = cast(caster, brace, braced).applied();
			Vec3 centre = king.centre();
			king.teleportTo(centre.x + OFF_LAKE, king.floorY(), centre.z);
			king.setYRot(0.0F);   // facing south, towards both
			Vec3 bareBefore = bare.getDeltaMovement();
			Vec3 bracedBefore = braced.getDeltaMovement();
			int hit = king.swipe(level);
			double[] bareSwipe = moved(bare, bareBefore);
			double[] bracedSwipe = moved(braced, bracedBefore);
			helper.runAfterDelay(20, () -> {
				for (ServerPlayer player : List.of(bare, braced)) {
					player.setHealth(player.getMaxHealth());
					player.damageCooldownTime = 0;
				}
				boolean second = cast(caster, brace, braced).applied();
				Vec3 bareLift = bare.getDeltaMovement();
				GlacialSpikeEntity.burst(level, king, bare.position());
				double[] bareSpike = moved(bare, bareLift);
				Vec3 bracedLift = braced.getDeltaMovement();
				GlacialSpikeEntity.burst(level, king, braced.position());
				double[] bracedSpike = moved(braced, bracedLift);
				ending(level, king, () -> {
					try {
						helper.assertTrue(first && second, "Brace did not take on the braced player");
						helper.assertTrue(hit == 2, "His swipe struck " + hit + " players, not 2");
						helper.assertTrue(near(bareSwipe[0], YetiKingEntity.SWIPE_KNOCKBACK) && near(bareSwipe[1], 0.3),
								"The bare player's push changed: " + Arrays.toString(bareSwipe));
						helper.assertTrue(near(bracedSwipe[0], YetiKingEntity.SWIPE_KNOCKBACK * half) && near(bracedSwipe[1], 0.3 * half),
								"A blow and its push were not braced once (half, not a quarter): " + Arrays.toString(bracedSwipe));
						helper.assertTrue(near(bareSpike[0], 0.0) && near(bareSpike[1], YetiKingEntity.SPIKE_LIFT),
								"A spike did not lift the bare player " + YetiKingEntity.SPIKE_LIFT + ": " + Arrays.toString(bareSpike));
						helper.assertTrue(near(bracedSpike[0], 0.0) && near(bracedSpike[1], YetiKingEntity.SPIKE_LIFT * half),
								"A spike did not lift the braced player half as high: " + Arrays.toString(bracedSpike));
					} finally {
						all.forEach(ServerPlayer::discard);
					}
				});
				helper.succeed();
			});
		});
	}

	/**
	 * Concordance Chunk B (B-09, B-P22, B-P55): a Brace one player put on another that meets his push is the caster's deed,
	 * one for each push it meets (two in one tick are one), with no part needed before; a Brace on oneself, the Water
	 * Bangle's and one whose caster was forgotten (as a relog forgets it) credit no one, and being braced earns nothing.
	 */
	@GameTest(structure = HALL, padding = 8, maxTicks = 20)
	public void aBraceOnAFriendIsTheCastersDeed(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		BlockPos origin = origin(helper);
		YetiKingEntity king = waiting(helper, origin);
		ServerPlayer friend = offLake(helper, king, -4.5, 0.0);
		ServerPlayer own = offLake(helper, king, 4.5, 0.0);
		ServerPlayer relogged = offLake(helper, king, 0.0, 4.5);
		ServerPlayer wearer = offLake(helper, king, 0.0, -4.5);
		ServerPlayer caster = offLake(helper, king, 0.0, 12.0);
		Vec3 friendStands = friend.position();
		Encounter fight = king.encounter();
		fight.begin();
		EffectSpec brace = invoked("jugcraft:brace");
		boolean braced = cast(caster, brace, friend).applied() && cast(caster, brace, relogged).applied()
				&& cast(own, brace, own).applied() && RimeGear.brace(wearer).applied();
		relogged.removeAttached(Brace.BRACER);   // what a relog leaves: the Brace kept, its caster forgotten (not a real relog)
		boolean casters = caster.getUUID().equals(Brace.caster(friend)) && own.getUUID().equals(Brace.caster(own))
				&& Brace.caster(wearer) == null && Brace.caster(relogged) == null && !Brace.BRACER.isPersistent();
		boolean noPart = fight.share(caster) == null;
		long a = level.getGameTime();
		slam(level, king);
		slam(level, king);
		int deedsA = deeds(fight, caster);
		boolean qualifiedA = fight.qualified().contains(caster.getUUID());
		boolean keyedA = !fight.deed(caster, "brace " + friend.getUUID() + " " + a);
		boolean noBracedPart = List.of(friend, own, wearer, relogged).stream().allMatch(player -> fight.share(player) == null);
		helper.runAfterDelay(1, () -> {
			ending(level, king, () -> {
				try {
					helper.assertTrue(braced && casters, "The Braces and their casters are not as set (the caster unsaved)");
					helper.assertTrue(noPart, "The caster had a part before any push");
					helper.assertTrue(deedsA == 1 && qualifiedA, "Two pushes in a tick on a friend's Brace were not one deed that qualifies: " + deedsA);
					helper.assertTrue(keyedA, "The Brace deed is not keyed by the braced player and the tick");
					helper.assertTrue(noBracedPart, "One's own, the bangle's or a forgotten caster's Brace, or being braced, earned a part");
					helper.assertTrue(!friend.hasEffect(Brace.BRACED) && cast(caster, brace, friend).applied(), "The friend could not be braced again");
					back(friend, friendStands);
					long b = level.getGameTime();
					slam(level, king);
					helper.assertTrue(deeds(fight, caster) == 2 && !fight.deed(caster, "brace " + friend.getUUID() + " " + b),
							"A Brace meeting a push the next tick was not a second deed: " + deeds(fight, caster));
					slam(level, king);
					helper.assertTrue(deeds(fight, caster) == 2, "A second push in that tick was another deed");
					helper.assertTrue(fight.share(own) == null, "A Brace on oneself earned a part");
				} finally {
					fight.end();
					List.of(friend, own, relogged, wearer, caster).forEach(ServerPlayer::discard);
				}
			});
			helper.succeed();
		});
	}

	/**
	 * Concordance Chunk B (B-09, B-P21, B-P54): a binding that takes holds his wind-up RIMEBOUND_WINDUP ticks longer, once a
	 * thaw: bound, his Maul Swipe strikes 16 ticks after it begins, not 12; while he thaws no one binds him and his next
	 * swipe strikes on time; bound again while he reels, the hold waits out his stun and holds the next wind-up.
	 */
	@GameTest(structure = HALL, padding = 8, maxTicks = 220)
	public void aRimeboundKingWindsUpLongerOnceAThaw(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		BlockPos origin = origin(helper);
		YetiKingEntity king = waiting(helper, origin);
		ServerPlayer foe = foeAt(helper, origin, 39.5, 5.0, 60.0, GameType.SURVIVAL);   // his target, far beyond his swipe
		ServerPlayer caster = foeAt(helper, origin, 70.0, 5.0, 36.5, GameType.CREATIVE);
		EffectSpec grip = invoked("jugcraft:rimebind");
		int windup = YetiKingEntity.Attack.MAUL_SWIPE.windup;
		Telegraph bound = new Telegraph();
		Telegraph own = new Telegraph();
		Telegraph reeled = new Telegraph();
		helper.onEachTick(() -> {
			for (Telegraph telegraph : List.of(bound, own, reeled)) {
				if (telegraph.begun >= 0 && telegraph.struck < 0 && king.action() == YetiKingEntity.Action.SWIPE) {
					telegraph.struck = king.tickCount;
				}
			}
		});
		hunting(helper, king, () -> {
			boolean took = cast(caster, grip, king).applied();
			MobEffectInstance rimed = king.getEffect(Rimebind.RIMEBOUND);
			MobEffectInstance thaw = king.getEffect(Rimebind.RIME_THAW);
			boolean halved = took && rimed != null && rimed.getDuration() == 30 && thaw != null && thaw.getDuration() == 70;
			swipeAt(level, king, foe, bound);
			helper.runAfterDelay(30, () -> {
				boolean shown = king.rimeHold() == 0 && king.attack() == null;
				ConcordanceEffects.Result thawing = cast(caster, grip, king);
				swipeAt(level, king, foe, own);
				helper.runAfterDelay(26, () -> {
					king.stun(level);
					helper.runAfterDelay(24, () -> {
						boolean thawed = !king.hasEffect(Rimebind.RIME_THAW);
						boolean again = cast(caster, grip, king).applied();
						boolean reeling = king.stunnedTicks() > 0;
						helper.runAfterDelay(40, () -> {
							boolean waited = king.rimeHold() == YetiKingEntity.RIMEBOUND_WINDUP && king.stunnedTicks() == 0 && king.attack() == null;
							swipeAt(level, king, foe, reeled);
							helper.runAfterDelay(20, () -> {
								ending(level, king, () -> {
									try {
										helper.assertTrue(halved, "Bound, he was not held 30 ticks with a thaw of 70");
										helper.assertTrue(bound.struck - bound.begun == windup + YetiKingEntity.RIMEBOUND_WINDUP,
												"Bound, his swipe struck " + (bound.struck - bound.begun) + " ticks after it began, not " + (windup + YetiKingEntity.RIMEBOUND_WINDUP));
										helper.assertTrue(shown, "The hold outlasted the wind-up it held");
										helper.assertTrue(thawing == ConcordanceEffects.Result.IMMUNE, "While he thaws Rimebind took him: " + thawing);
										helper.assertTrue(own.struck - own.begun == windup, "Thawing, his swipe was held: " + (own.struck - own.begun));
										helper.assertTrue(thawed && again && reeling, "Thawed and reeling, he was not bound again");
										helper.assertTrue(waited, "The hold did not wait out his stun for his next wind-up");
										helper.assertTrue(reeled.struck - reeled.begun == windup + YetiKingEntity.RIMEBOUND_WINDUP,
												"After his stun his swipe was not held: " + (reeled.struck - reeled.begun));
										helper.assertTrue(king.rimeHold() == 0, "Shown, the hold is not gone");
									} finally {
										foe.discard();
										caster.discard();
									}
								});
								helper.succeed();
							});
						});
					});
				});
			});
		});
	}

	/**
	 * Concordance Chunk B (B-09; settles yeti-king.md:143): Rimebind's slow does not hold his steps. Bound as his charge's
	 * run begins, his speed attribute a fifth down, seven steps on he is where an unbound run puts him (eight steps of
	 * CHARGE_SPEED from 45.0); and that binding, past his wind-up, waits for his next one (B-P54).
	 */
	@GameTest(structure = HALL, padding = 8, maxTicks = 120)
	public void aRimeboundChargeKeepsItsPace(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		BlockPos origin = origin(helper);
		YetiKingEntity king = waiting(helper, origin);
		ServerPlayer foe = foeAt(helper, origin, 75.0, 5.0, 36.5, GameType.SURVIVAL);
		ServerPlayer caster = foeAt(helper, origin, 45.0, 5.0, 60.0, GameType.CREATIVE);
		EffectSpec grip = invoked("jugcraft:rimebind");
		boolean[] bound = {false, false, false};   // cast, took, still bound when seen
		double[] seen = {Double.NaN, Double.NaN, -1.0};   // his x in the template, his speed, his hold
		helper.onEachTick(() -> {
			if (king.attack() != CHARGE) {
				return;
			}
			if (king.attackTicks() == CHARGE.windup && !bound[0]) {
				bound[0] = true;
				bound[1] = cast(caster, grip, king).applied();
			} else if (king.attackTicks() == CHARGE.windup + 7 && Double.isNaN(seen[0])) {
				seen[0] = king.getX() - origin.getX();
				seen[1] = king.getAttributeValue(Attributes.MOVEMENT_SPEED);
				seen[2] = king.rimeHold();
				bound[2] = king.hasEffect(Rimebind.RIMEBOUND);
			}
		});
		hunting(helper, king, () -> {
			charge(level, king, 45.0, 36.5, foe);
			helper.runAfterDelay(40, () -> {
				ending(level, king, () -> {
					try {
						helper.assertTrue(bound[1] && bound[2], "Rimebind did not take as his run began, or he was not bound seven steps on");
						helper.assertTrue(Math.abs(seen[1] - king.getAttributeBaseValue(Attributes.MOVEMENT_SPEED) * (1.0 - Rimebind.SLOW)) < EPS,
								"Bound, his speed attribute was not a fifth down: " + seen[1]);
						helper.assertTrue(Math.abs(seen[0] - (45.0 + 8 * YetiKingEntity.CHARGE_SPEED)) < EPS,
								"Bound, his charge did not keep its pace: at " + seen[0] + ", not " + (45.0 + 8 * YetiKingEntity.CHARGE_SPEED));
						helper.assertTrue(seen[2] == YetiKingEntity.RIMEBOUND_WINDUP, "A binding past his wind-up did not wait for his next: " + seen[2]);
					} finally {
						foe.discard();
						caster.discard();
					}
				});
				helper.succeed();
			});
		});
	}

	// ---------------------------------------------------------------- unit 5 (B-09, B-P56): the column's cold work

	/** Rimekeeping's elective kind of cold work as knowledge keeps it: the column's, and a Frost boon arm's slow's (B-P56). */
	private static final String ELECTIVE = "practice:" + Rime.ACTIVITY + ":" + Rime.KEY_ELECTIVE;

	/** {@code player}'s Rimekeeping cold work (its evidence keys), as ConcordanceWinterGameTests reads it. */
	private static List<String> rimework(ServerPlayer player) {
		return ConcordanceProgress.knowledge(player).progress(Rime.RESEARCH).evidence().keySet().stream()
				.filter(key -> key.startsWith("practice:" + Rime.ACTIVITY + ":")).toList();
	}

	/**
	 * Concordance Chunk B (B-09, B-P16, B-P56; 02:137): his charge broken on an ice column is Rimekeeping's elective cold
	 * work for the player its column deed names, if they understand Rimekeeping: counted once, kept through a save, standing
	 * for one kind. It is the kind a Frost boon arm's slow is too, so after either the other adds nothing, and with
	 * Rimebind's restraint it is two kinds, not three: every mastery still needs a Preserve. One who has only encountered
	 * Rimekeeping earns the deed and learns nothing; with the Concordance off nobody learns from it.
	 */
	@GameTest(structure = HALL, padding = 8, maxTicks = 240)
	public void aColumnIsElectiveRimeworkForItsBaiterOnce(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		BlockPos origin = origin(helper);
		YetiKingEntity king = waiting(helper, origin);
		floor(level, origin, 24, 10, 29, 14);
		ServerPlayer keeper = northOfTheColumn(helper, king, origin, 26.5);
		ServerPlayer novice = northOfTheColumn(helper, king, origin, 25.5);
		ConcordanceProgress.grant(keeper, "jugcraft:first_light", ResearchState.UNDERSTOOD);
		ConcordanceProgress.grant(keeper, Rime.RESEARCH, ResearchState.UNDERSTOOD);
		ConcordanceProgress.grant(novice, "jugcraft:first_light", ResearchState.UNDERSTOOD);
		ConcordanceProgress.grant(novice, Rime.RESEARCH, ResearchState.ENCOUNTERED);
		int run = YetiKingEntity.Attack.AVALANCHE_CHARGE.windup + 12;
		Encounter fight = king.encounter();
		king.wake(level);
		helper.runAfterDelay(YetiKingEntity.WAKE_TICKS + 2, () -> {
			fight.warded(keeper, novice, 2.0F, 600);   // each a part short of qualifying, as twoBaitersEachEarnTheirDeed
			fight.warded(novice, keeper, 2.0F, 600);
			chargeAt(level, king, origin, keeper);
			helper.runAfterDelay(run, () -> {
				boolean firstStunned = king.stunnedTicks() > 0;
				List<String> learned = rimework(keeper);
				int keeperDeeds = deeds(fight, keeper);
				boolean oneKind = ConcordanceProgress.knowledge(keeper).state(Rime.RESEARCH) == ResearchState.UNDERSTOOD;
				helper.runAfterDelay(YetiKingEntity.STUN_TICKS + 4, () -> {
					chargeAt(level, king, origin, novice);
					helper.runAfterDelay(run, () -> {
						ending(level, king, () -> {
							boolean on = JugcraftConfig.isFeatureEnabled(JugcraftConcordance.FEATURE);
							// A hostile creature at normal tolerance for a Frost boon arm's slow to take on (Rime.frostBoon).
							LivingEntity zombie = helper.spawnWithNoFreeWill(EntityTypes.ZOMBIE, new BlockPos(28, 1, 13));
							try {
								helper.assertTrue(firstStunned && king.stunnedTicks() > 0, "A charge did not end in the column");
								helper.assertTrue(learned.equals(List.of(ELECTIVE)) && keeperDeeds == 1,
										"The keeper's column was not one deed and the elective kind: " + learned + ", " + keeperDeeds);
								helper.assertTrue(oneKind, "One kind mastered Rimekeeping");
								helper.assertTrue(deeds(fight, novice) == 1 && rimework(novice).isEmpty(),
										"One who only encountered Rimekeeping: deed " + deeds(fight, novice) + ", learned " + rimework(novice));
								helper.assertTrue(!Rime.column(keeper) && rimework(keeper).equals(List.of(ELECTIVE)), "The column counted twice");
								helper.assertTrue(!Rime.frostBoon(keeper, zombie) && rimework(keeper).equals(List.of(ELECTIVE)),
										"After the column a Frost boon arm's slow added a kind: " + rimework(keeper));
								Knowledge knowledge = ConcordanceProgress.knowledge(keeper);
								Knowledge loaded = ConcordanceCodecs.KNOWLEDGE.parse(JsonOps.INSTANCE,
										ConcordanceCodecs.KNOWLEDGE.encodeStart(JsonOps.INSTANCE, knowledge).getOrThrow()).getOrThrow();
								helper.assertTrue(loaded.equals(knowledge) && loaded.progress(Rime.RESEARCH).evidence().containsKey(ELECTIVE),
										"The elective kind does not survive a save");
								ConcordanceProgress.grant(novice, Rime.RESEARCH, ResearchState.UNDERSTOOD);
								JugcraftConfig.setFeatureEnabled(JugcraftConcordance.FEATURE, false);
								boolean offTaught = Rime.column(novice);
								JugcraftConfig.setFeatureEnabled(JugcraftConcordance.FEATURE, on);
								helper.assertTrue(!offTaught && rimework(novice).isEmpty(), "With the Concordance off the column taught something");
								// B-P56: once a Frost boon arm's slow has recorded the elective, the column adds nothing; with
								// Rimebind's restraint that is two kinds, and without a Preserve Rimekeeping is not mastered.
								helper.assertTrue(Rime.frostBoon(novice, zombie) && rimework(novice).equals(List.of(ELECTIVE)),
										"A Frost boon arm's slow was not the elective kind: " + rimework(novice));
								helper.assertTrue(!Rime.column(novice) && rimework(novice).equals(List.of(ELECTIVE)),
										"After a Frost boon arm's slow the column added a kind: " + rimework(novice));
								helper.assertTrue(Rime.practise(novice, Rime.KEY_RESTRAINT)
										&& ConcordanceProgress.knowledge(novice).state(Rime.RESEARCH) == ResearchState.UNDERSTOOD,
										"Rimebind and the elective mastered Rimekeeping without a Preserve (B-P56)");
								helper.assertTrue(Rime.practise(keeper, Rime.KEY_PRESERVE_COLD)
										&& ConcordanceProgress.knowledge(keeper).state(Rime.RESEARCH) == ResearchState.UNDERSTOOD, "Two kinds are not three");
								helper.assertTrue(Rime.practise(keeper, Rime.KEY_PRESERVE_HEATED)
										&& ConcordanceProgress.knowledge(keeper).state(Rime.RESEARCH) == ResearchState.MASTERED,
										"The column did not stand for one kind");
							} finally {
								JugcraftConfig.setFeatureEnabled(JugcraftConcordance.FEATURE, on);
								zombie.discard();
								keeper.discard();
								novice.discard();
							}
						});
						helper.succeed();
					});
				});
			});
		});
	}
}
