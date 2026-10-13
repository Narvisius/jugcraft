package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.concordance.Alchemy;
import io.github.jimbozoomer.jugcraft.concordance.Authority;
import io.github.jimbozoomer.jugcraft.concordance.Brew;
import io.github.jimbozoomer.jugcraft.concordance.ConcordanceEffects;
import io.github.jimbozoomer.jugcraft.concordance.ConcordanceProgress;
import io.github.jimbozoomer.jugcraft.concordance.JugcraftConcordance;
import io.github.jimbozoomer.jugcraft.concordance.RateGate;
import io.github.jimbozoomer.jugcraft.concordance.effect.Cause;
import io.github.jimbozoomer.jugcraft.concordance.effect.EffectKind;
import io.github.jimbozoomer.jugcraft.concordance.effect.EffectSpec;
import io.github.jimbozoomer.jugcraft.concordance.effect.Intent;
import io.github.jimbozoomer.jugcraft.concordance.effect.Ledger;
import io.github.jimbozoomer.jugcraft.concordance.effect.Stacking;
import io.github.jimbozoomer.jugcraft.concordance.rules.ResearchState;
import io.github.jimbozoomer.jugcraft.concordance.spire.ConcordSpire;
import io.github.jimbozoomer.jugcraft.concordance.spire.SpireHeartBlockEntity;
import io.github.jimbozoomer.jugcraft.concordance.spirits.ClockworkPorterEntity;
import io.github.jimbozoomer.jugcraft.concordance.spirits.Workers;
import io.github.jimbozoomer.jugcraft.concordance.sympathy.Sympathy;
import io.github.jimbozoomer.jugcraft.concordance.sympathy.TaglockItem;
import io.github.jimbozoomer.jugcraft.concordance.worker.Body;
import io.github.jimbozoomer.jugcraft.concordance.worker.Status;
import io.github.jimbozoomer.jugcraft.concordance.worker.WorkerDefinition;
import io.github.jimbozoomer.jugcraft.config.JugcraftConfig;
import java.util.List;
import java.util.UUID;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.fabricmc.fabric.api.transfer.v1.item.ItemStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Roadmap step 28, permissions and hostile use (docs/features/arcane-concordance-authority.md): an indirect magical
 * action cannot make a change its player could not make with their own hands. A stand-in protection mod
 * ({@link TestClaims}) claims blocks and creatures for someone else, as a claim mod would, and each test asks the
 * Concordance's indirect routes to change or harm them: a spell's light, a spell's and a forged salve's harm, a link
 * taken by touch, a porter's trip, a device whose owner is away (and the same with the server's explicit option that
 * lets a stand-in answer for them), a hopper under a Spire Heart, and a link used by someone who did not take it.
 */
public class ConcordanceAuthorityGameTests {
	private static final long NIGHT = 18000L;
	private static final UUID NEIGHBOUR = new UUID(28L, 1L);
	private static final UUID AWAY = new UUID(28L, 2L);
	private static final EffectSpec LIGHT = EffectSpec.of(EffectKind.ILLUMINATION, Intent.HELPFUL, 0, 640);
	private static final EffectSpec SEAR = new EffectSpec(EffectKind.DAMAGE, Intent.HARMFUL, 2, 0, null, Stacking.STRONGEST, "spell_power:arcane");

	private static ServerPlayer player(GameTestHelper helper) {
		ServerPlayer player = helper.makeMockServerPlayerInLevel();
		player.setGameMode(GameType.SURVIVAL);
		BlockPos absolute = helper.absolutePos(new BlockPos(0, 2, 0));
		player.snapTo(absolute.getX() + 0.5, absolute.getY(), absolute.getZ() + 0.5, 0.0F, 0.0F);
		RateGate.forget(player.getUUID());
		return player;
	}

	private static void floor(GameTestHelper helper) {
		for (int x = 0; x <= 7; x++) {
			for (int z = 0; z <= 7; z++) {
				helper.setBlock(new BlockPos(x, 1, z), Blocks.STONE);
			}
		}
	}

	/** One application of {@code spec}, as {@code actor} for the person {@code behind}. */
	private static ConcordanceEffects.Result apply(GameTestHelper helper, @Nullable Entity actor, @Nullable UUID behind, EffectSpec spec,
			Object target) {
		Cause cause = Cause.of(behind, Cause.Origin.SPELL, "test:authority", ConcordanceEffects.nextSerial());
		ConcordanceEffects.Context context = new ConcordanceEffects.Context(helper.getLevel(), cause, actor, new Ledger(new Ledger.Limits(4, 64, 0)),
				"0/0/0", Vec3.atCenterOf(helper.absolutePos(new BlockPos(0, 2, 0))));
		return target instanceof BlockPos pos ? ConcordanceEffects.apply(context, spec, helper.absolutePos(pos))
				: ConcordanceEffects.apply(context, spec, (Mob) target);
	}

	/** Runs {@code check} with the server's absent-owner option on, then puts it back. */
	private static void withAbsentOwners(Runnable check) {
		JugcraftConfig.setOption(Authority.ABSENT_OPTION, true);
		try {
			check.run();
		} finally {
			JugcraftConfig.setOption(Authority.ABSENT_OPTION, false);
		}
	}

	/**
	 * A spell's light goes in the open and in its caster's own claim, never in a neighbour's; a light whose caster is
	 * away waits, unless the server lets a stand-in answer for them, and then the neighbour's claim still refuses it.
	 */
	@GameTest(maxTicks = 20)
	public void lightGoesOnlyWhereItsCasterCouldBuild(GameTestHelper helper) {
		floor(helper);
		ServerPlayer caster = player(helper);
		BlockPos theirs = new BlockPos(2, 3, 2);
		BlockPos mine = new BlockPos(4, 3, 2);
		BlockPos open = new BlockPos(6, 3, 2);
		try (TestClaims claims = TestClaims.open()) {
			claims.block(helper.absolutePos(theirs), NEIGHBOUR).block(helper.absolutePos(mine), caster.getUUID());
			helper.assertValueEqual(apply(helper, caster, caster.getUUID(), LIGHT, theirs), ConcordanceEffects.Result.NOT_ALLOWED,
					"no light in a neighbour's claim");
			helper.assertTrue(helper.getBlockState(theirs).isAir(), "and nothing placed there");
			helper.assertTrue(apply(helper, caster, caster.getUUID(), LIGHT, mine).applied()
					&& helper.getBlockState(mine).is(JugcraftConcordance.LUMEN_MOTE), "light in the caster's own claim");
			helper.assertTrue(apply(helper, caster, caster.getUUID(), LIGHT, open).applied(), "and in the open");
			BlockPos waiting = new BlockPos(6, 3, 4);
			helper.assertValueEqual(apply(helper, null, AWAY, LIGHT, waiting), ConcordanceEffects.Result.NOT_ALLOWED,
					"a light whose caster is away waits for them");
			claims.block(helper.absolutePos(new BlockPos(2, 3, 4)), NEIGHBOUR).block(helper.absolutePos(new BlockPos(4, 3, 4)), AWAY);
			withAbsentOwners(() -> {
				helper.assertTrue(apply(helper, null, AWAY, LIGHT, waiting).applied(), "with the server's option, a stand-in answers: light in the open");
				helper.assertValueEqual(apply(helper, null, AWAY, LIGHT, new BlockPos(2, 3, 4)), ConcordanceEffects.Result.NOT_ALLOWED,
						"but never in a neighbour's claim, which judges the stand-in as the caster");
				helper.assertTrue(apply(helper, null, AWAY, LIGHT, new BlockPos(4, 3, 4)).applied(), "and in the caster's own claim");
			});
		}
		helper.succeed();
	}

	/**
	 * Harm reaches only what its caster could strike: never a neighbour's claimed pig, always a monster, the caster's
	 * own pig; a forged salve that calls poison helpful is judged by the poison and refused too; a link is not taken
	 * from a creature the caster may not strike; and a caster who is away harms no creature that is not a monster
	 * (unless a stand-in answers) and never a player.
	 */
	@GameTest(maxTicks = 20)
	public void harmReachesOnlyWhatItsCasterCouldStrike(GameTestHelper helper) {
		floor(helper);
		ServerLevel level = helper.getLevel();
		ServerPlayer caster = player(helper);
		Mob claimed = helper.spawnWithNoFreeWill(EntityTypes.PIG, new BlockPos(2, 2, 2));
		Mob owned = helper.spawnWithNoFreeWill(EntityTypes.PIG, new BlockPos(4, 2, 2));
		Mob free = helper.spawnWithNoFreeWill(EntityTypes.PIG, new BlockPos(6, 2, 2));
		Mob zombie = helper.spawnWithNoFreeWill(EntityTypes.ZOMBIE, new BlockPos(2, 2, 6));
		try (TestClaims claims = TestClaims.open()) {
			claims.creature(claimed, NEIGHBOUR).creature(owned, caster.getUUID()).creature(zombie, NEIGHBOUR);
			helper.assertValueEqual(apply(helper, caster, caster.getUUID(), SEAR, claimed), ConcordanceEffects.Result.NOT_ALLOWED,
					"a neighbour's claimed pig is refused");
			helper.assertTrue(claimed.getHealth() == claimed.getMaxHealth(), "and unhurt");
			helper.assertTrue(apply(helper, caster, caster.getUUID(), SEAR, owned).applied(), "the caster's own pig may be struck");
			helper.assertTrue(apply(helper, caster, caster.getUUID(), SEAR, free).applied(), "and an unclaimed one");
			helper.assertTrue(apply(helper, caster, caster.getUUID(), SEAR, zombie).applied(), "a monster may always be fought");
			Brew forged = new Brew(List.of(new Brew.Dose("minecraft:poison", 0, 100, false)), "test:forged");
			helper.assertTrue(Alchemy.harmful(forged.effects().get(0)), "the server judges a dose by its status, not the item's word");
			helper.assertValueEqual(Alchemy.apply(level, forged, claimed, caster), 0, "so a forged salve does not reach the claimed pig");
			helper.assertTrue(!claimed.hasEffect(MobEffects.POISON), "which is not poisoned");
			Brew mending = new Brew(List.of(new Brew.Dose("minecraft:regeneration", 0, 100, false)), "test:mending");
			helper.assertValueEqual(Alchemy.apply(level, mending, claimed, caster), 1, "a helpful salve still may");
			ConcordanceProgress.grant(caster, "jugcraft:first_light", ResearchState.UNDERSTOOD);
			ConcordanceProgress.grant(caster, Sympathy.RESEARCH, ResearchState.UNDERSTOOD);
			helper.assertValueEqual(TaglockItem.bind(caster, level, new ItemStack(Sympathy.TAGLOCK), claimed), "not_allowed",
					"no link is taken from a creature its taker may not strike");
			Mob stray = helper.spawnWithNoFreeWill(EntityTypes.PIG, new BlockPos(6, 2, 6));
			helper.assertValueEqual(apply(helper, null, AWAY, SEAR, stray), ConcordanceEffects.Result.NOT_ALLOWED,
					"a caster away harms no creature that is not a monster");
			// Another zombie: one struck this tick is still invulnerable for a moment.
			Mob monster = helper.spawnWithNoFreeWill(EntityTypes.ZOMBIE, new BlockPos(4, 2, 6));
			helper.assertTrue(apply(helper, null, AWAY, SEAR, monster).applied(), "though a monster still may be");
			ServerPlayer bystander = player(helper);
			Cause away = Cause.of(AWAY, Cause.Origin.SPELL, "test:authority", ConcordanceEffects.nextSerial());
			ConcordanceEffects.Context context = new ConcordanceEffects.Context(level, away, null, new Ledger(new Ledger.Limits(4, 64, 0)), "0/1/0",
					bystander.position());
			helper.assertValueEqual(ConcordanceEffects.apply(context, SEAR, bystander), ConcordanceEffects.Result.FRIENDLY,
					"and never a player: nobody is here to answer to the PvP rules");
			withAbsentOwners(() -> {
				helper.assertTrue(apply(helper, null, AWAY, SEAR, stray).applied(), "with the server's option, the stand-in may strike an unclaimed pig");
				helper.assertValueEqual(apply(helper, null, AWAY, SEAR, claimed), ConcordanceEffects.Result.NOT_ALLOWED, "never a claimed one");
				ConcordanceEffects.Context again = new ConcordanceEffects.Context(level, away, null, new Ledger(new Ledger.Limits(4, 64, 0)), "0/2/0",
						bystander.position());
				helper.assertValueEqual(ConcordanceEffects.apply(again, SEAR, bystander), ConcordanceEffects.Result.FRIENDLY, "nor a player");
			});
		}
		helper.succeed();
	}

	/**
	 * A porter takes only from a container its keeper could use by hand, asked at every trip: a neighbour's claimed
	 * chest blocks it, and the claim lifted it carries; with its keeper away it waits, and with the server's option a
	 * stand-in answers for them, whom the neighbour's claim still refuses.
	 */
	@GameTest(maxTicks = 20)
	public void aPorterTakesOnlyWhatItsKeeperCould(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		ServerPlayer keeper = player(helper);
		BlockPos source = new BlockPos(1, 1, 2);
		BlockPos target = new BlockPos(3, 1, 2);
		helper.setBlock(source, Blocks.CHEST);
		helper.setBlock(target, Blocks.CHEST);
		ChestBlockEntity from = (ChestBlockEntity) level.getBlockEntity(helper.absolutePos(source));
		from.setItem(0, new ItemStack(Items.COBBLESTONE, 5));
		WorkerDefinition.Construct terms = ClockworkPorterEntity.terms();
		String dimension = level.dimension().identifier().toString();
		long game = level.getGameTime() + 300_000L;
		ClockworkPorterEntity porter = porter(helper, keeper.getUUID(), terms);
		porter.setRoute(new ClockworkPorterEntity.Route(helper.absolutePos(source), helper.absolutePos(target), dimension));
		ClockworkPorterEntity orphan = porter(helper, AWAY, terms);
		orphan.setRoute(new ClockworkPorterEntity.Route(helper.absolutePos(source), helper.absolutePos(target), dimension));
		try (TestClaims claims = TestClaims.open()) {
			claims.block(helper.absolutePos(source), NEIGHBOUR);
			helper.assertValueEqual(porter.think(level, NIGHT, game), Status.BLOCKED_BY_ACCESS, "a neighbour's claimed chest blocks it");
			helper.assertTrue(porter.carriedCount() == 0 && from.getItem(0).getCount() == 5, "and it takes nothing");
			helper.assertValueEqual(orphan.think(level, NIGHT, game), Status.OWNER_OFFLINE, "its keeper away, it waits");
			withAbsentOwners(() -> helper.assertValueEqual(orphan.think(level, NIGHT, game + 20), Status.BLOCKED_BY_ACCESS,
					"a stand-in answers for its keeper, and the neighbour's claim refuses them too"));
		}
		helper.assertTrue(porter.think(level, NIGHT, game + 40) == Status.WORKING && porter.carriedCount() == 5, "the claim lifted, it carries");
		from.setItem(0, new ItemStack(Items.COBBLESTONE, 3));
		withAbsentOwners(() -> helper.assertTrue(orphan.think(level, NIGHT, game + 60) == Status.WORKING && orphan.carriedCount() == 3,
				"and with the server's option, so does the absent keeper's porter"));
		helper.succeed();
	}

	private static ClockworkPorterEntity porter(GameTestHelper helper, UUID owner, WorkerDefinition.Construct terms) {
		ServerLevel level = helper.getLevel();
		ClockworkPorterEntity porter = Workers.CLOCKWORK_PORTER.create(level, EntitySpawnReason.MOB_SUMMONED);
		porter.setOwner(owner);
		porter.setBody(new Body(terms.integrity(), terms.energy()));
		Vec3 at = helper.absoluteVec(new Vec3(2.5, 1.0, 2.5));
		porter.snapTo(at.x, at.y, at.z, 0.0F, 0.0F);
		level.addFreshEntity(porter);
		return porter;
	}

	/** A Spire Heart's store takes in through any face but gives out through none: a hopper beneath takes nothing. */
	@GameTest(maxTicks = 60)
	public void aSpireHeartsStoreCannotBeDrained(GameTestHelper helper) {
		BlockPos at = new BlockPos(2, 2, 2);
		helper.setBlock(at.below(), Blocks.HOPPER);
		helper.setBlock(at, ConcordSpire.HEART);
		SpireHeartBlockEntity heart = (SpireHeartBlockEntity) helper.getBlockEntity(at, SpireHeartBlockEntity.class);
		heart.setItem(0, new ItemStack(Items.GLOWSTONE_DUST, 16));
		for (Direction side : Direction.values()) {
			Storage<ItemVariant> store = ItemStorage.SIDED.find(helper.getLevel(), helper.absolutePos(at), side);
			try (Transaction transaction = Transaction.openOuter()) {
				helper.assertTrue(store != null && store.extract(ItemVariant.of(Items.GLOWSTONE_DUST), 16, transaction) == 0,
						"nothing comes out through its " + side + " face");
				helper.assertTrue(store.insert(ItemVariant.of(Items.GLOWSTONE_DUST), 1, transaction) == 1, "but upkeep goes in through it");
			}
		}
		helper.runAtTickTime(40, () -> {
			helper.assertTrue(heart.getItem(0).is(Items.GLOWSTONE_DUST) && heart.getItem(0).getCount() == 16,
					"the hopper beneath took nothing: " + heart.getItem(0));
			helper.succeed();
		});
	}

	/** A link is its maker's: someone else holding the taglock cannot curse through it (nothing is spent). */
	@GameTest(maxTicks = 20)
	public void aLinkIsItsMakersOwn(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		ServerPlayer maker = hexer(helper);
		ServerPlayer holder = hexer(helper);
		Mob zombie = helper.spawnWithNoFreeWill(EntityTypes.ZOMBIE, new BlockPos(4, 2, 4));
		ItemStack taglock = new ItemStack(Sympathy.TAGLOCK);
		helper.assertValueEqual(TaglockItem.bind(maker, level, taglock, zombie), "", "the maker takes a link");
		ItemStack cobweb = new ItemStack(Items.COBWEB, 2);
		helper.assertValueEqual(TaglockItem.curse(holder, level, taglock, cobweb), "not_yours", "handed on, it is not the holder's to use");
		helper.assertTrue(cobweb.getCount() == 2 && ConcordanceProgress.currentFocus(holder) == 20, "and nothing was spent");
		helper.assertValueEqual(TaglockItem.curse(maker, level, taglock, cobweb), "", "its maker still may");
		helper.succeed();
	}

	private static ServerPlayer hexer(GameTestHelper helper) {
		ServerPlayer player = player(helper);
		ConcordanceProgress.grant(player, "jugcraft:first_light", ResearchState.UNDERSTOOD);
		ConcordanceProgress.grant(player, Sympathy.RESEARCH, ResearchState.UNDERSTOOD);
		ConcordanceProgress.setFocus(player, 20);
		return player;
	}
}
