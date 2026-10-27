package io.github.jimbozoomer.jugcraft.test;

import com.mojang.authlib.GameProfile;
import io.github.jimbozoomer.jugcraft.concordance.Alchemy;
import io.github.jimbozoomer.jugcraft.concordance.Authority;
import io.github.jimbozoomer.jugcraft.concordance.Brew;
import io.github.jimbozoomer.jugcraft.concordance.ConcordanceEffects;
import io.github.jimbozoomer.jugcraft.concordance.ConcordanceProgress;
import io.github.jimbozoomer.jugcraft.concordance.JugcraftConcordance;
import io.github.jimbozoomer.jugcraft.concordance.RateGate;
import io.github.jimbozoomer.jugcraft.concordance.courier.CourierLedger;
import io.github.jimbozoomer.jugcraft.concordance.courier.CourierPostBlockEntity;
import io.github.jimbozoomer.jugcraft.concordance.courier.Couriers;
import io.github.jimbozoomer.jugcraft.concordance.effect.Cause;
import io.github.jimbozoomer.jugcraft.concordance.effect.EffectKind;
import io.github.jimbozoomer.jugcraft.concordance.effect.EffectSpec;
import io.github.jimbozoomer.jugcraft.concordance.effect.Intent;
import io.github.jimbozoomer.jugcraft.concordance.effect.Ledger;
import io.github.jimbozoomer.jugcraft.concordance.effect.Stacking;
import io.github.jimbozoomer.jugcraft.concordance.resource.BoundWill;
import io.github.jimbozoomer.jugcraft.concordance.rules.ResearchState;
import io.github.jimbozoomer.jugcraft.concordance.sky.AstrolabeItem;
import io.github.jimbozoomer.jugcraft.concordance.spire.ConcordSpire;
import io.github.jimbozoomer.jugcraft.concordance.spire.SpireHeartBlockEntity;
import io.github.jimbozoomer.jugcraft.concordance.spirits.AgreementHolder;
import io.github.jimbozoomer.jugcraft.concordance.spirits.BoundWills;
import io.github.jimbozoomer.jugcraft.concordance.spirits.ClockworkPorterEntity;
import io.github.jimbozoomer.jugcraft.concordance.spirits.GatheringShadeEntity;
import io.github.jimbozoomer.jugcraft.concordance.spirits.PactKeeping;
import io.github.jimbozoomer.jugcraft.concordance.spirits.PactMenu;
import io.github.jimbozoomer.jugcraft.concordance.spirits.PactPerchBlock;
import io.github.jimbozoomer.jugcraft.concordance.spirits.PactPerchBlockEntity;
import io.github.jimbozoomer.jugcraft.concordance.spirits.PactRequestPayload;
import io.github.jimbozoomer.jugcraft.concordance.spirits.PactStatePayload;
import io.github.jimbozoomer.jugcraft.concordance.spirits.SpiritAnchorBlock;
import io.github.jimbozoomer.jugcraft.concordance.spirits.SpiritAnchorBlockEntity;
import io.github.jimbozoomer.jugcraft.concordance.spirits.StarReckoning;
import io.github.jimbozoomer.jugcraft.concordance.spirits.StolasEntity;
import io.github.jimbozoomer.jugcraft.concordance.spirits.Workers;
import io.github.jimbozoomer.jugcraft.concordance.spirits.WorkshopHandEntity;
import io.github.jimbozoomer.jugcraft.concordance.sympathy.Sympathy;
import io.github.jimbozoomer.jugcraft.concordance.sympathy.TaglockItem;
import io.github.jimbozoomer.jugcraft.concordance.worker.Body;
import io.github.jimbozoomer.jugcraft.concordance.worker.Pacts;
import io.github.jimbozoomer.jugcraft.concordance.worker.Status;
import io.github.jimbozoomer.jugcraft.concordance.worker.WorkerDefinition;
import io.github.jimbozoomer.jugcraft.config.JugcraftConfig;
import io.github.jimbozoomer.jugcraft.party.JugcraftParties;
import io.github.jimbozoomer.jugcraft.party.PartyManager;
import io.github.jimbozoomer.jugcraft.styx.StyxConservatory;
import io.github.jimbozoomer.jugcraft.styx.StyxState;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import net.fabricmc.fabric.api.entity.FakePlayer;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.fabricmc.fabric.api.transfer.v1.item.ItemStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Roadmap step 28, permissions and hostile use (docs/features/arcane-concordance-authority.md): an indirect magical
 * action cannot make a change its player could not make with their own hands. A stand-in protection mod
 * ({@link TestClaims}) claims blocks and creatures for someone else, as a claim mod would, and each test asks the
 * Concordance's indirect routes to change or harm them: a spell's light, a spell's and a forged salve's harm, a link
 * taken by touch, a porter's trip, a device whose owner is away (and the same with the server's explicit option that
 * lets a stand-in answer for them), a hopper under a Spire Heart, and a link used by someone who did not take it; and a
 * Spirit Anchor sealed only by its owner's side where they may build, never by a stand-in (Concordance Chunk D, D-01),
 * and a pact no change of owner moves (D-02), at a pact perch too; and a pact perch placed only where its placer may
 * build, never in Styx's home (D-04).
 */
public class ConcordanceAuthorityGameTests {
	private static final long NIGHT = 18000L;
	private static final UUID NEIGHBOUR = new UUID(28L, 1L);
	private static final UUID AWAY = new UUID(28L, 2L);
	private static final EffectSpec LIGHT = EffectSpec.of(EffectKind.ILLUMINATION, Intent.HELPFUL, 0, 640);
	private static final EffectSpec SEAR = new EffectSpec(EffectKind.DAMAGE, Intent.HARMFUL, 2, 0, null, Stacking.STRONGEST, "spell_power:arcane");

	private static ServerPlayer player(GameTestHelper helper) {
		ServerPlayer player = TestPlayers.mock(helper);
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

	private static ServerPlayer spiritbinder(GameTestHelper helper) {
		ServerPlayer player = player(helper);
		ConcordanceProgress.grant(player, "jugcraft:first_light", ResearchState.UNDERSTOOD);
		ConcordanceProgress.grant(player, Workers.RESEARCH, ResearchState.UNDERSTOOD);
		return player;
	}

	private static String key(@Nullable Component line) {
		return line != null && line.getContents() instanceof TranslatableContents contents ? contents.getKey() : "";
	}

	private static int held(ServerLevel level, ServerPlayer player) {
		return BoundWills.of(level.getServer()).ledger().heldBy(player.getUUID()).size();
	}

	/** An anchor at {@code at}, placed by {@code placer} (null: set without a placer, as one from before owners). */
	private static SpiritAnchorBlockEntity placed(GameTestHelper helper, BlockPos at, @Nullable ServerPlayer placer) {
		helper.setBlock(at, Workers.SPIRIT_ANCHOR);
		BlockPos absolute = helper.absolutePos(at);
		if (placer != null) {
			Workers.SPIRIT_ANCHOR.setPlacedBy(helper.getLevel(), absolute, helper.getLevel().getBlockState(absolute), placer, new ItemStack(Workers.SPIRIT_ANCHOR));
		}
		return helper.getBlockEntity(at, SpiritAnchorBlockEntity.class);
	}

	private static WorkerDefinition.Spirit shadeTerms() {
		return Workers.catalog().get(GatheringShadeEntity.DEFINITION, WorkerDefinition.Spirit.class);
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

	/**
	 * D-01 (D-S8, E1): a Spirit Anchor records who placed it; outside any claim a stranger may not seal at it and is told why;
	 * its owner's party seals for themself; one set without a placer is sealed by whoever may change it (a neighbour's claim
	 * refuses it).
	 */
	@GameTest(maxTicks = 20)
	public void onlyTheOwnersSideSealsAnAnchor(GameTestHelper helper) {
		floor(helper);
		ServerLevel level = helper.getLevel();
		ServerPlayer owner = spiritbinder(helper);
		ServerPlayer stranger = spiritbinder(helper);
		ServerPlayer member = spiritbinder(helper);
		SpiritAnchorBlockEntity anchor = placed(helper, new BlockPos(2, 2, 2), owner);
		helper.assertTrue(owner.getUUID().equals(anchor.owner()), "Placed, the anchor records who placed it");
		helper.assertValueEqual(key(SpiritAnchorBlock.use(stranger, level, anchor, false)), "message.jugcraft.concordance.workers.seal_not_permitted",
				"Outside any claim, a stranger is told they may not seal at another's anchor");
		helper.assertTrue(anchor.agreement() == null && held(level, stranger) == 0 && level.getEntitiesOfClass(GatheringShadeEntity.class, helper.getBounds(),
				shade -> stranger.getUUID().equals(shade.owner())).isEmpty(), "and nothing is sealed or called");
		PartyManager parties = JugcraftParties.manager();
		long ms = System.currentTimeMillis();
		try {
			helper.assertTrue(parties.invite(owner.getUUID(), member.getUUID(), ms) == PartyManager.Result.OK
					&& parties.accept(member.getUUID(), ms) == PartyManager.Result.OK, "The owner and a member form a party");
			helper.assertValueEqual(key(SpiritAnchorBlock.use(member, level, anchor, false)), "message.jugcraft.concordance.workers.sealed", "The owner's party may seal");
			helper.assertTrue(member.getUUID().equals(anchor.agreement().holder()) && held(level, member) == 1 && held(level, owner) == 0,
					"for themself: the member holds it, not the owner");
		} finally {
			parties.adminDisband(owner.getUUID());
		}
		SpiritAnchorBlockEntity legacy = placed(helper, new BlockPos(5, 2, 2), null);
		helper.assertTrue(legacy.owner() == null, "An anchor set without a placer, as one from before Chunk D, has no owner");
		try (TestClaims claims = TestClaims.open()) {
			claims.block(helper.absolutePos(new BlockPos(5, 2, 2)), NEIGHBOUR);
			helper.assertValueEqual(key(SpiritAnchorBlock.use(stranger, level, legacy, false)), "message.jugcraft.concordance.workers.seal_not_permitted",
					"In a neighbour's claim no one else may seal it");
			helper.assertTrue(legacy.agreement() == null && held(level, stranger) == 0, "and nothing is sealed");
		}
		helper.assertValueEqual(key(SpiritAnchorBlock.use(stranger, level, legacy, false)), "message.jugcraft.concordance.workers.sealed",
				"The claim lifted, whoever may change it seals it, as before Chunk D");
		helper.succeed();
	}

	/**
	 * D-01 (D-S8, E1): a stand-in never seals, even with the owner's identity or the absent-owner option; it neither suspends
	 * nor ends, nor calls a lost shade again (the unit review's fix 5); placing an anchor it records no owner.
	 */
	@GameTest(maxTicks = 20)
	public void aStandInNeverSeals(GameTestHelper helper) {
		floor(helper);
		ServerLevel level = helper.getLevel();
		ServerPlayer owner = spiritbinder(helper);
		SpiritAnchorBlockEntity anchor = placed(helper, new BlockPos(2, 2, 2), owner);
		FakePlayer standIn = FakePlayer.get(level, new GameProfile(owner.getUUID(), "[Concordance]"));
		long now = level.getGameTime();
		helper.assertValueEqual(anchor.seal(level, standIn, shadeTerms(), now).outcome(), Pacts.Outcome.NOT_PERMITTED, "A stand-in never seals, even with the owner's identity");
		helper.assertTrue(anchor.agreement() == null && held(level, owner) == 0, "nothing is sealed");
		withAbsentOwners(() -> helper.assertValueEqual(anchor.seal(level, standIn, shadeTerms(), now).outcome(), Pacts.Outcome.NOT_PERMITTED,
				"not even when the server lets stand-ins answer for absent owners"));
		helper.assertValueEqual(anchor.seal(level, owner, shadeTerms(), now).outcome(), Pacts.Outcome.DONE, "Its owner seals");
		SpiritAnchorBlock.use(standIn, level, anchor, true);
		helper.assertTrue(anchor.holder().held(level) == null, "A stand-in's sneaking use suspends nothing");
		helper.assertTrue(anchor.holder().toggle(level, standIn, now) == AgreementHolder.Toggle.NOT_HOLDER
				&& anchor.holder().end(level, standIn, now) == AgreementHolder.Ended.NOT_PERMITTED && anchor.agreement() != null,
				"and it neither suspends nor ends the agreement");
		UUID lost = anchor.spirit();
		level.getEntity(lost).remove(Entity.RemovalReason.KILLED);
		helper.assertTrue(anchor.holder().spiritLost(level), "Its shade killed, its anchor knows it is lost");
		helper.assertTrue(anchor.holder().recall(level, standIn, now) == AgreementHolder.Recall.NOT_HOLDER && lost.equals(anchor.spirit())
				&& anchor.holder().spiritLost(level), "A stand-in with the owner's identity never calls another: nothing is called");
		helper.assertTrue(placed(helper, new BlockPos(5, 2, 2), standIn).owner() == null, "A stand-in placing an anchor records no owner");
		helper.succeed();
	}

	/**
	 * D-02 (D-S23, E1): the holder leaving the party, or the anchor changing hands, never moves a pact; the owner cannot end
	 * or suspend another's, by the anchor or by its Bound Will alone; nobody else benefits. At a pact perch too (D-04, D-05):
	 * its new owner reads, does and is served nothing of another's pact, whose holder is still served there.
	 */
	@GameTest(maxTicks = 20)
	public void aChangeOfOwnerMovesNoPact(GameTestHelper helper) {
		floor(helper);
		ServerLevel level = helper.getLevel();
		ServerPlayer owner = spiritbinder(helper);
		ServerPlayer holder = spiritbinder(helper);
		ServerPlayer stranger = spiritbinder(helper);
		SpiritAnchorBlockEntity anchor = placed(helper, new BlockPos(2, 2, 2), owner);
		PartyManager parties = JugcraftParties.manager();
		long ms = System.currentTimeMillis();
		long now = level.getGameTime();
		try {
			helper.assertTrue(parties.invite(owner.getUUID(), holder.getUUID(), ms) == PartyManager.Result.OK
					&& parties.accept(holder.getUUID(), ms) == PartyManager.Result.OK, "The owner and the holder form a party");
			helper.assertValueEqual(anchor.seal(level, stranger, shadeTerms(), now).outcome(), Pacts.Outcome.NOT_PERMITTED, "A stranger may not seal at it");
			helper.assertValueEqual(anchor.seal(level, holder, shadeTerms(), now).outcome(), Pacts.Outcome.DONE, "The owner's party may");
			helper.assertValueEqual(parties.leave(holder.getUUID()), PartyManager.Result.OK, "The holder leaves the party");
		} finally {
			parties.adminDisband(owner.getUUID());
		}
		UUID record = anchor.agreement().record();
		BoundWills wills = BoundWills.of(level.getServer());
		helper.assertTrue(holder.getUUID().equals(wills.ledger().get(record).holder()) && anchor.holder().verify(level, now) == AgreementHolder.Standing.VALID,
				"Leaving the party moves the pact to nobody");
		helper.assertValueEqual(anchor.holder().end(level, owner, now), AgreementHolder.Ended.NOT_HOLDER, "The anchor's owner cannot end another's pact");
		anchor.holder().setOwner(stranger.getUUID()); // the place changes hands
		helper.assertTrue(holder.getUUID().equals(wills.ledger().get(record).holder()) && anchor.holder().end(level, stranger, now) == AgreementHolder.Ended.NOT_HOLDER,
				"A new owner of the place neither takes nor ends it");
		helper.assertValueEqual(anchor.holder().toggle(level, holder, now), AgreementHolder.Toggle.SUSPENDED, "Its holder still keeps it");
		helper.assertTrue(AgreementHolder.toggle(level, owner, record, now) == AgreementHolder.Toggle.NOT_HOLDER
				&& AgreementHolder.toggle(level, holder, record, now) == AgreementHolder.Toggle.RESUMED,
				"By its Bound Will alone, as from the roster, the owner suspends nothing and its holder resumes it");
		helper.assertTrue(!Pacts.benefits(wills.ledger().get(record), owner.getUUID(), JugcraftParties.sameParty(holder.getUUID(), owner.getUUID())),
				"and nobody else benefits from it");
		// Concordance Chunk D, D-04 (D-S23): at a pact perch too, the land changing hands moves no pact; its new owner reads only
		// that another's pact is kept there, and every action of theirs there is refused.
		ServerPlayer keeper = ConcordanceWorkerGameTests.regarded(helper);
		BlockPos perchAt = new BlockPos(5, 2, 5);
		PactPerchBlockEntity perch = ConcordanceWorkerGameTests.stolasPact(helper, keeper, perchAt, now);
		UUID pact = perch.holder().agreement().record();
		perch.holder().setOwner(stranger.getUUID());
		ConcordanceWorkerGameTests.ready(level, stranger);
		ConcordanceWorkerGameTests.beside(helper, stranger, perchAt);
		PactMenu menu = ConcordanceWorkerGameTests.open(helper, stranger, perch);
		helper.assertTrue(menu.lastSent().mode() == PactStatePayload.CLOSED
				&& ConcordanceWorkerGameTests.keys(menu.lastSent().notes()).contains(ConcordanceWorkerGameTests.PACT_KEY + "reading.taken"),
				"The perch's new owner reads only that another's pact is kept there");
		for (int action : List.of(PactRequestPayload.ACCEPT, PactRequestPayload.SUSPEND, PactRequestPayload.END, PactRequestPayload.SEAT)) {
			PactStatePayload answer = ConcordanceWorkerGameTests.send(stranger, menu, action, 0);
			helper.assertTrue(answer != null && answer.result().isPresent() && !answer.result().get().done()
					&& keeper.getUUID().equals(wills.ledger().get(pact).holder()) && wills.ledger().get(pact).pact().hold() == BoundWill.Hold.NONE,
					"and action " + action + " of theirs is refused");
		}
		stranger.closeContainer();
		helper.assertTrue(wills.ledger().heldBy(stranger.getUUID()).isEmpty(), "Nothing became theirs");
		// Concordance Chunk D, D-05 (D-S23): nor is its new owner served there; its holder still is.
		ConcordanceWorkerGameTests.observed(level, keeper, ConcordanceWorkerGameTests.FULL_MOON);
		ConcordanceWorkerGameTests.holding(keeper, AstrolabeItem.CAPACITY);
		ConcordanceWorkerGameTests.holding(stranger, AstrolabeItem.CAPACITY);
		ConcordanceWorkerGameTests.beside(helper, stranger, perchAt);
		helper.assertTrue(StarReckoning.reckon(level, stranger, perch.holder(), ConcordanceWorkerGameTests.FULL_MOON, 24006L, now) == Pacts.Reckoning.NOT_HOLDER
				&& PactKeeping.suspend(level, stranger, perch.holder(), now) == AgreementHolder.Toggle.NOT_HOLDER
				&& perch.holder().seat(level, stranger, StolasEntity.COUNTERPART, now) == AgreementHolder.Seat.NOTHING
				&& keeper.getUUID().equals(wills.ledger().get(pact).holder()), "The perch's new owner neither uses, suspends nor re-seats another's pact");
		ConcordanceWorkerGameTests.beside(helper, keeper, perchAt);
		helper.assertValueEqual(StarReckoning.reckon(level, keeper, perch.holder(), ConcordanceWorkerGameTests.FULL_MOON, 24006L, now), Pacts.Reckoning.RECALLED,
				"Its holder is still served there");
		helper.succeed();
	}

	private static InteractionResult placePerch(GameTestHelper helper, ServerPlayer player, BlockPos at) {
		ItemStack perch = new ItemStack(Workers.PACT_PERCH_ITEM);
		player.setItemInHand(InteractionHand.MAIN_HAND, perch);
		BlockPos below = helper.absolutePos(at.below());
		return perch.useOn(new UseOnContext(player, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atCenterOf(below).add(0.0, 0.5, 0.0), Direction.UP, below, false)));
	}

	/** D-04 (D-S15, D-S8): a perch is placed only where its placer may build (a neighbour's claim refuses it), never in Styx's home, never by a stand-in or a machine; placed, it records its owner. */
	@GameTest(maxTicks = 20)
	public void aPerchIsPlacedOnlyWhereItsPlacerMayBuild(GameTestHelper helper) {
		floor(helper);
		ServerLevel level = helper.getLevel();
		ServerPlayer placer = player(helper);
		BlockPos claimed = new BlockPos(2, 2, 2);
		BlockPos open = new BlockPos(4, 2, 2);
		BlockPos styx = new BlockPos(6, 2, 6);
		try (TestClaims claims = TestClaims.open()) {
			claims.block(helper.absolutePos(claimed), NEIGHBOUR);
			helper.assertTrue(!placePerch(helper, placer, claimed).consumesAction() && !helper.getBlockState(claimed).is(Workers.PACT_PERCH), "In a neighbour's claim it is refused");
		}
		helper.assertTrue(placePerch(helper, placer, open).consumesAction() && helper.getBlockState(open).is(Workers.PACT_PERCH)
				&& placer.getUUID().equals(helper.getBlockEntity(open, PactPerchBlockEntity.class).holder().owner()), "Where they may build it stands, recording its owner");
		StyxState state = StyxState.get(level);
		Optional<BlockPos> origin = state.origin;
		int layout = state.layout;
		boolean townHome = state.townHome;
		try {
			state.origin = Optional.of(helper.absolutePos(styx).offset(-10, -1, -10));
			state.layout = StyxConservatory.CURRENT_LAYOUT;
			state.townHome = false;
			helper.assertTrue(StyxConservatory.covers(level, helper.absolutePos(styx)) && !StyxConservatory.covers(level, helper.absolutePos(styx).offset(-20, 0, 0)),
					"Styx's home covers its footprint and nothing beside it");
			helper.assertTrue(!placePerch(helper, placer, styx).consumesAction() && !helper.getBlockState(styx).is(Workers.PACT_PERCH), "In Styx's home it is refused");
		} finally {
			state.origin = origin;
			state.layout = layout;
			state.townHome = townHome;
		}
		FakePlayer standIn = FakePlayer.get(level, new GameProfile(placer.getUUID(), "[Concordance]"));
		BlockPos spot = helper.absolutePos(new BlockPos(6, 2, 2));
		helper.assertTrue(!PactPerchBlock.mayPlace(level, standIn, spot) && !PactPerchBlock.mayPlace(level, null, spot) && PactPerchBlock.mayPlace(level, placer, spot),
				"A stand-in or a machine never places one; its player may");
		helper.succeed();
	}

	private static ServerPlayer hexer(GameTestHelper helper) {
		ServerPlayer player = player(helper);
		ConcordanceProgress.grant(player, "jugcraft:first_light", ResearchState.UNDERSTOOD);
		ConcordanceProgress.grant(player, Sympathy.RESEARCH, ResearchState.UNDERSTOOD);
		ConcordanceProgress.setFocus(player, 20);
		return player;
	}

	/**
	 * Concordance Chunk D, D-08 (E1, D-S8): a workshop hand serves only a post its holder may use, bound by its anchor's owner's
	 * side. A stranger's key is never set to another's post; one the owner handed over binds no stranger's anchor; a stranger
	 * binds or seals nothing at the owner's anchor, nor does a stand-in with the owner's identity, nor the owner under a
	 * neighbour's claim; a key used on an anchor never takes it for a route's container. Only the owner's binding and seal give
	 * a hand, and it serves the owner's request.
	 */
	@GameTest(maxTicks = 20)
	public void aStrangerCannotBindAHandToAnotherPlayersPost(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		ServerPlayer owner = WorkshopHands.holder(helper);
		ServerPlayer stranger = WorkshopHands.holder(helper);
		CourierPostBlockEntity post = WorkshopHands.post(helper, owner, WorkshopHands.POST);
		ChestBlockEntity chest = WorkshopHands.chest(helper, WorkshopHands.CHEST, 10);
		Couriers.request(owner, level, post, new ItemStack(Items.GLOWSTONE_DUST), 2);
		long id = WorkshopHands.lastFiled(helper, WorkshopHands.POST);
		List<Long> ids = List.of(id);
		GlobalPos owners = GlobalPos.of(level.dimension(), helper.absolutePos(WorkshopHands.POST));
		BlockPos strangersAnchorAt = new BlockPos(4, 1, 5);
		BlockPos strangersPostAt = new BlockPos(6, 1, 2);
		SpiritAnchorBlockEntity strangersAnchor = WorkshopHands.anchor(helper, stranger, strangersAnchorAt);
		WorkshopHands.post(helper, stranger, strangersPostAt);
		helper.assertTrue(WorkshopHands.keyFor(helper, stranger, WorkshopHands.POST).get(Couriers.KEY_POST) == null,
				"A stranger's key is never set to another player's post");
		ItemStack handed = WorkshopHands.keyFor(helper, owner, WorkshopHands.POST);
		helper.assertTrue(owners.equals(handed.get(Couriers.KEY_POST)) && WorkshopHands.use(helper, stranger, handed, strangersAnchorAt) == InteractionResult.FAIL
				&& strangersAnchor.post() == null && handed.get(Workers.PORTER_ROUTE) == null,
				"A key the owner handed over binds no stranger's anchor to the owner's post, and an anchor is never taken for a route");
		WorkshopHands.conserved(helper, ids, WorkshopHands.PLAIN, 10, 0, "a handed key refused", chest, post);
		SpiritAnchorBlockEntity ownersAnchor = WorkshopHands.anchor(helper, owner, WorkshopHands.ANCHOR);
		helper.assertTrue(WorkshopHands.use(helper, stranger, WorkshopHands.keyFor(helper, stranger, strangersPostAt), WorkshopHands.ANCHOR) == InteractionResult.FAIL
				&& WorkshopHands.use(helper, stranger, handed, WorkshopHands.ANCHOR) == InteractionResult.FAIL && ownersAnchor.post() == null,
				"A stranger binds the owner's anchor to no post, theirs or the owner's");
		FakePlayer standIn = FakePlayer.get(level, new GameProfile(owner.getUUID(), "[Concordance]"));
		helper.assertTrue(WorkshopHands.use(helper, standIn, handed, WorkshopHands.ANCHOR) == InteractionResult.FAIL && ownersAnchor.post() == null,
				"nor does a stand-in with the owner's identity");
		try (TestClaims claims = TestClaims.open()) {
			claims.block(helper.absolutePos(WorkshopHands.ANCHOR), NEIGHBOUR);
			helper.assertTrue(WorkshopHands.use(helper, owner, handed, WorkshopHands.ANCHOR) == InteractionResult.FAIL && ownersAnchor.post() == null,
					"Under a neighbour's claim, not even its owner binds it");
		}
		WorkshopHands.conserved(helper, ids, WorkshopHands.PLAIN, 10, 0, "nothing bound", chest, post);
		helper.assertTrue(WorkshopHands.use(helper, owner, handed, WorkshopHands.ANCHOR) == InteractionResult.SUCCESS && owners.equals(ownersAnchor.post()),
				"Its owner binds it to their post" + WorkshopHands.why(helper, owner, WorkshopHands.ANCHOR, WorkshopHands.POST));
		helper.assertTrue(WorkshopHands.use(helper, stranger, WorkshopHands.keyFor(helper, stranger, strangersPostAt), WorkshopHands.ANCHOR) == InteractionResult.FAIL
				&& owners.equals(ownersAnchor.post()), "A stranger cannot re-bind it");
		helper.assertValueEqual(WorkshopHands.key(WorkshopHands.seal(helper, stranger, ownersAnchor)), "message.jugcraft.concordance.workers.seal_not_permitted",
				"nor seal a hand at it, and is told why");
		helper.assertTrue(ownersAnchor.agreement() == null && BoundWills.of(level.getServer()).ledger().heldBy(stranger.getUUID()).isEmpty(),
				"Nothing is recorded for anyone");
		WorkshopHands.conserved(helper, ids, WorkshopHands.PLAIN, 10, 0, "nothing sealed", chest, post);
		helper.assertValueEqual(WorkshopHands.key(WorkshopHands.seal(helper, owner, ownersAnchor)), WorkshopHands.SEALED, "Its owner seals a workshop hand");
		WorkshopHandEntity hand = (WorkshopHandEntity) level.getEntity(ownersAnchor.spirit());
		WorkshopHands.stand(helper, hand, 2.5, 1.0, 3.5);
		CourierLedger ledger = CourierLedger.of(level.getServer());
		long now = WorkshopHands.morning(level, 3_700_000L);
		long in = WorkshopHands.inHours();
		helper.assertTrue(hand.think(level, in, now) == Status.TRAVELLING && hand.task() == id, "and it takes its owner's request");
		WorkshopHands.conserved(helper, ids, WorkshopHands.PLAIN, 10, 0, "claimed", chest, post);
		helper.assertValueEqual(hand.think(level, in, now + 20), Status.WORKING, "it picks up");
		WorkshopHands.conserved(helper, ids, WorkshopHands.PLAIN, 10, 0, "picked up", chest, post);
		helper.assertTrue(hand.think(level, in, now + 40) == Status.WORKING && ledger.ledger().get(id) == null
				&& WorkshopHands.matching(post, WorkshopHands.PLAIN) == 2, "delivering it, once");
		WorkshopHands.conserved(helper, ids, WorkshopHands.PLAIN, 10, 0, "delivered", chest, post);
		helper.succeed();
	}
}
