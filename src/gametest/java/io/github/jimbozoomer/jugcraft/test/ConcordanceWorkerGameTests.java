package io.github.jimbozoomer.jugcraft.test;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.authlib.GameProfile;
import io.github.jimbozoomer.jugcraft.Jugcraft;
import io.github.jimbozoomer.jugcraft.concordance.Authority;
import io.github.jimbozoomer.jugcraft.concordance.ConcordanceData;
import io.github.jimbozoomer.jugcraft.concordance.ConcordanceProgress;
import io.github.jimbozoomer.jugcraft.concordance.CrucibleBlockEntity;
import io.github.jimbozoomer.jugcraft.concordance.LeyPylonBlockEntity;
import io.github.jimbozoomer.jugcraft.concordance.JugcraftConcordance;
import io.github.jimbozoomer.jugcraft.concordance.RateGate;
import io.github.jimbozoomer.jugcraft.concordance.Saved;
import io.github.jimbozoomer.jugcraft.concordance.celestial.Pattern;
import io.github.jimbozoomer.jugcraft.concordance.courier.CourierLedger;
import io.github.jimbozoomer.jugcraft.concordance.courier.CourierPostBlockEntity;
import io.github.jimbozoomer.jugcraft.concordance.courier.Couriers;
import io.github.jimbozoomer.jugcraft.concordance.journal.Journal;
import io.github.jimbozoomer.jugcraft.concordance.journal.JournalSection;
import io.github.jimbozoomer.jugcraft.concordance.logistics.Logistics;
import io.github.jimbozoomer.jugcraft.concordance.logistics.Place;
import io.github.jimbozoomer.jugcraft.concordance.logistics.RequestState;
import io.github.jimbozoomer.jugcraft.concordance.resource.BoundWill;
import io.github.jimbozoomer.jugcraft.concordance.resource.BoundWillLedger;
import io.github.jimbozoomer.jugcraft.concordance.rules.ConcordanceRules;
import io.github.jimbozoomer.jugcraft.concordance.rules.Evidence;
import io.github.jimbozoomer.jugcraft.concordance.rules.ResearchState;
import io.github.jimbozoomer.jugcraft.concordance.sign.Sign;
import io.github.jimbozoomer.jugcraft.concordance.sky.AstralClaims;
import io.github.jimbozoomer.jugcraft.concordance.sky.AstrolabeItem;
import io.github.jimbozoomer.jugcraft.concordance.sky.ObservatoryBlockEntity;
import io.github.jimbozoomer.jugcraft.concordance.sky.Sky;
import io.github.jimbozoomer.jugcraft.concordance.spirits.AgreementHolder;
import io.github.jimbozoomer.jugcraft.concordance.spirits.BondingCharmItem;
import io.github.jimbozoomer.jugcraft.concordance.spirits.BoundWills;
import io.github.jimbozoomer.jugcraft.concordance.spirits.ClockworkPorterEntity;
import io.github.jimbozoomer.jugcraft.concordance.spirits.GatheringShadeEntity;
import io.github.jimbozoomer.jugcraft.concordance.spirits.HearthlingEntity;
import io.github.jimbozoomer.jugcraft.concordance.spirits.PactHistory;
import io.github.jimbozoomer.jugcraft.concordance.spirits.PactKeeping;
import io.github.jimbozoomer.jugcraft.concordance.spirits.PactMenu;
import io.github.jimbozoomer.jugcraft.concordance.spirits.PactPerchBlockEntity;
import io.github.jimbozoomer.jugcraft.concordance.spirits.PactReading;
import io.github.jimbozoomer.jugcraft.concordance.spirits.PactRequestPayload;
import io.github.jimbozoomer.jugcraft.concordance.spirits.PactSessions;
import io.github.jimbozoomer.jugcraft.concordance.spirits.PactStatePayload;
import io.github.jimbozoomer.jugcraft.concordance.spirits.PorterKeyItem;
import io.github.jimbozoomer.jugcraft.concordance.spirits.Roster;
import io.github.jimbozoomer.jugcraft.concordance.spirits.RosterRequestPayload;
import io.github.jimbozoomer.jugcraft.concordance.spirits.RosterStatePayload;
import io.github.jimbozoomer.jugcraft.concordance.spirits.SpiritAnchorBlock;
import io.github.jimbozoomer.jugcraft.concordance.spirits.SpiritAnchorBlockEntity;
import io.github.jimbozoomer.jugcraft.concordance.spirits.StarReckoning;
import io.github.jimbozoomer.jugcraft.concordance.spirits.StolasEntity;
import io.github.jimbozoomer.jugcraft.concordance.spirits.StolasRegard;
import io.github.jimbozoomer.jugcraft.concordance.spirits.WorkerEntity;
import io.github.jimbozoomer.jugcraft.concordance.spirits.WorkerRoster;
import io.github.jimbozoomer.jugcraft.concordance.spirits.Workers;
import io.github.jimbozoomer.jugcraft.concordance.spirits.WorkshopHandEntity;
import io.github.jimbozoomer.jugcraft.concordance.worker.Agreement;
import io.github.jimbozoomer.jugcraft.concordance.worker.Body;
import io.github.jimbozoomer.jugcraft.concordance.worker.Bond;
import io.github.jimbozoomer.jugcraft.concordance.worker.PactCatalog;
import io.github.jimbozoomer.jugcraft.concordance.worker.PactDefinition;
import io.github.jimbozoomer.jugcraft.concordance.worker.Pacts;
import io.github.jimbozoomer.jugcraft.concordance.worker.Status;
import io.github.jimbozoomer.jugcraft.concordance.worker.WorkerDefinition;
import io.github.jimbozoomer.jugcraft.config.JugcraftConfig;
import io.github.jimbozoomer.jugcraft.party.JugcraftParties;
import io.github.jimbozoomer.jugcraft.party.PartyManager;
import io.github.jimbozoomer.jugcraft.styx.JugcraftStyx;
import io.netty.buffer.Unpooled;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.IntStream;
import local.peepo.MobTransportCrate;
import net.fabricmc.fabric.api.entity.FakePlayer;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.animal.cow.Cow;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Roadmap step 17, spirits, familiars and constructs (docs/features/arcane-concordance-workers.md), on a real server.
 * Each test drives a worker's decisions with {@code think} at times it chooses, standing the worker within reach so no
 * walking is needed: a familiar binds one to a person, mends them by its bond and only so often, and waits when its
 * person is offline; a spirit's agreement is sealed as a Bound Will, it gathers and delivers within its terms, says it
 * is outside them at noon and suspended when suspended, and breaking its anchor releases it; a porter says it has no
 * route, is waiting for resources, carries and delivers, spends energy and wear, is full, needs repair and is fuelled
 * by a pylon; and each worker's model survives a save. Concordance Chunk D, unit 1: a killed shade leaves its load once
 * and its holder calls another under the same agreement; a sealed shade is on the roster at once; a refused seal says why
 * and spends nothing; only the holder ends an agreement, once; a familiar fades only for its person's own play time; and
 * one pact record's upkeep warns, gives grace, suspends and resumes, never owing time away or switched off, while no one
 * binds another and no place keeps two. Concordance Chunk D, D-04: Stolas explains before anyone agrees; a stale terms
 * revision, a refusal and a second negotiation spend nothing; accepting seals once and spends once; one pact a holder;
 * nobody accepts for another; a visitor without a pact or a live conversation leaves; crates and ordinary damage cannot
 * take him; a broken definition is reported and sealed pacts keep their terms; the screen's two messages are bounded.
 * Concordance Chunk D, D-05: Star-reckoning refuses with its reasons and recalls through the one recall; the first
 * obligation masters Stolas's Regard once; his upkeep warns, gives grace, suspends and resumes on his own offerings, never
 * owing time away, switched off or answered by a stand-in; ending returns what was paid ahead once to a living holder;
 * breaking his perch ends nothing and the pact is seated again; a lost Stolas is called back at no cost.
 */
public class ConcordanceWorkerGameTests {
	private static final long NIGHT = 18000L;
	private static final long NOON = 6000L;
	private static final long DAY = Agreement.DAY;
	private static final long PERIOD = 7 * DAY;
	/** Gentle's numbers on the vanilla #minecraft:wool tag (D-04 brings Stolas's own tag). */
	private static final BoundWill.Upkeep UPKEEP = Pacts.terms("minecraft:wool", 1, PERIOD, DAY, 4);

	private static ServerPlayer binder(GameTestHelper helper) {
		ServerPlayer player = TestPlayers.mock(helper);
		player.setGameMode(GameType.SURVIVAL);
		return taught(player);
	}

	/** {@code player} taught the Binding Arts, with no action of theirs rate-limited. */
	private static ServerPlayer taught(ServerPlayer player) {
		ConcordanceProgress.grant(player, "jugcraft:first_light", ResearchState.UNDERSTOOD);
		ConcordanceProgress.grant(player, Workers.RESEARCH, ResearchState.UNDERSTOOD);
		RateGate.forget(player.getUUID());
		return player;
	}

	private static void place(GameTestHelper helper, Entity entity, double x, double y, double z) {
		Vec3 at = helper.absoluteVec(new Vec3(x, y, z));
		entity.snapTo(at.x, at.y, at.z, 0.0F, 0.0F);
	}

	private static WorkerDefinition.Spirit terms() {
		return Workers.catalog().get(GatheringShadeEntity.DEFINITION, WorkerDefinition.Spirit.class);
	}

	static String key(@Nullable Component line) {
		return line != null && line.getContents() instanceof TranslatableContents contents ? contents.getKey() : "";
	}

	static @Nullable Object arg(@Nullable Component line, int index) {
		return line != null && line.getContents() instanceof TranslatableContents contents && contents.getArgs().length > index ? contents.getArgs()[index] : null;
	}

	/** Drops {@code count} wheat on {@code shade} and has it gather at night; returns what it then carries. */
	private static int gather(ServerLevel level, GatheringShadeEntity shade, long game, int count) {
		ItemEntity dropped = new ItemEntity(level, shade.getX(), shade.getY(), shade.getZ(), new ItemStack(Items.WHEAT, count));
		dropped.setPickUpDelay(0);
		level.addFreshEntity(dropped);
		shade.think(level, NIGHT, game);
		return shade.carriedCount();
	}

	private static int wheat(SpiritAnchorBlockEntity anchor) {
		return anchor.contents().stream().filter(stack -> stack.is(Items.WHEAT)).mapToInt(ItemStack::getCount).sum();
	}

	private static int loose(GameTestHelper helper, Item item) {
		return helper.getLevel().getEntitiesOfClass(ItemEntity.class, helper.getBounds(), entity -> entity.isAlive() && entity.getItem().is(item))
				.stream().mapToInt(entity -> entity.getItem().getCount()).sum();
	}

	private static int shades(GameTestHelper helper, UUID holder) {
		return helper.getLevel().getEntitiesOfClass(GatheringShadeEntity.class, helper.getBounds(), s -> s.isAlive() && holder.equals(s.owner())).size();
	}

	private static int held(ServerLevel level, UUID holder) {
		return BoundWills.of(level.getServer()).ledger().heldBy(holder).size();
	}

	private static SpiritAnchorBlockEntity anchor(GameTestHelper helper, BlockPos at) {
		helper.setBlock(at, Workers.SPIRIT_ANCHOR);
		return helper.getBlockEntity(at, SpiritAnchorBlockEntity.class);
	}

	static void beside(GameTestHelper helper, ServerPlayer player, BlockPos at) {
		Vec3 to = helper.absoluteVec(new Vec3(at.getX() + 0.5, at.getY(), at.getZ() + 2.5));
		player.snapTo(to.x, to.y, to.z, 0.0F, 0.0F);
	}

	/** An anchor at {@code at} sealed for {@code holder} at {@code now}, with {@code upkeep}; its holder stands beside it. */
	private static SpiritAnchorBlockEntity sealed(GameTestHelper helper, ServerPlayer holder, BlockPos at, BoundWill.@Nullable Upkeep upkeep, long now) {
		SpiritAnchorBlockEntity anchor = anchor(helper, at);
		beside(helper, holder, at);
		helper.assertValueEqual(anchor.holder().seal(helper.getLevel(), holder, anchor.sealing(terms(), upkeep), now).outcome(), Pacts.Outcome.DONE,
				"the agreement is sealed");
		return anchor;
	}

	private static BoundWill will(ServerLevel level, SpiritAnchorBlockEntity anchor) {
		return BoundWills.of(level.getServer()).ledger().get(anchor.agreement().record());
	}

	private static BoundWill.Upkeep upkeep(ServerLevel level, SpiritAnchorBlockEntity anchor) {
		return will(level, anchor).pact().upkeep();
	}

	static int escrowed(ServerLevel level, UUID record) {
		return BoundWills.of(level.getServer()).escrow(record).stream().mapToInt(ItemStack::getCount).sum();
	}

	private static int wool(ServerPlayer player) {
		return player.getInventory().countItem(Items.WOOL.white());
	}

	/** Looks at the pact every 1,000 ticks (less than ABSENT_TICKS) from {@code from} to {@code to}; returns what happened. */
	private static List<Pacts.Event> stay(ServerLevel level, SpiritAnchorBlockEntity anchor, long from, long to) {
		return stay(level, anchor.holder(), from, to);
	}

	/** The same for any holder block (Concordance Chunk D, unit 2: the pact perch too). */
	static List<Pacts.Event> stay(ServerLevel level, AgreementHolder holder, long from, long to) {
		List<Pacts.Event> events = new ArrayList<>();
		for (long t = from; t <= to; t += 1_000L) {
			Pacts.Event event = holder.look(level, t);
			if (event != Pacts.Event.NONE) {
				events.add(event);
			}
		}
		return events;
	}

	/** The Hearthling {@code person}'s Bonding Charm binds where they stand. */
	private static HearthlingEntity bindFamiliar(GameTestHelper helper, ServerPlayer person) {
		ServerLevel level = helper.getLevel();
		helper.assertTrue(BondingCharmItem.use(person, level, false).equals(InteractionResult.SUCCESS), "The charm binds a Hearthling");
		UUID id = WorkerRoster.of(level.getServer()).familiar(person.getUUID());
		helper.assertTrue(id != null && level.getEntity(id) instanceof HearthlingEntity, "Its person's roster holds it");
		return (HearthlingEntity) level.getEntity(id);
	}

	private static HearthlingEntity loadFamiliar(ServerLevel level, CompoundTag saved) {
		HearthlingEntity copy = Workers.HEARTHLING.create(level, EntitySpawnReason.LOAD);
		copy.load(TagValueInput.create(ProblemReporter.DISCARDING, level.registryAccess(), saved));
		return copy;
	}

	/** Runs {@code during} with the Concordance switched off, then puts the switch back, in the same tick. */
	static void off(Runnable during) {
		boolean on = JugcraftConfig.isFeatureEnabled(JugcraftConcordance.FEATURE);
		JugcraftConfig.setFeatureEnabled(JugcraftConcordance.FEATURE, false);
		try {
			during.run();
		} finally {
			JugcraftConfig.setFeatureEnabled(JugcraftConcordance.FEATURE, on);
		}
	}

	// ---------------------------------------------------------------- familiars

	/**
	 * A Bonding Charm binds one Hearthling; with a bond it mends its badly hurt person once, not again within its
	 * cooldown; a second charm use recalls rather than binds; sneaking releases it; and one whose person is offline
	 * waits and says so.
	 */
	@GameTest(maxTicks = 20)
	public void aFamiliarMendsByItsBond(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		ServerPlayer person = binder(helper);
		helper.assertTrue(BondingCharmItem.use(person, level, false).equals(InteractionResult.SUCCESS), "The charm binds a Hearthling");
		UUID id = WorkerRoster.of(level.getServer()).familiar(person.getUUID());
		helper.assertTrue(id != null && level.getEntity(id) instanceof HearthlingEntity, "Its person's roster holds it");
		HearthlingEntity familiar = (HearthlingEntity) level.getEntity(id);
		helper.assertTrue(BondingCharmItem.use(person, level, false).equals(InteractionResult.SUCCESS)
				&& WorkerRoster.of(level.getServer()).familiar(person.getUUID()).equals(id), "Used again it recalls the same familiar, never a second");
		long game = level.getGameTime() + 100_000L;
		helper.assertTrue(familiar.think(level, NIGHT, game) != Status.SUPPORTING, "Unbonded, it does not mend");
		familiar.setBond(new Bond(Bond.FIRST, 0L, 0, Long.MIN_VALUE / 2, game));
		person.setHealth(5.0F);
		helper.assertTrue(familiar.think(level, NIGHT, game + 20) == Status.SUPPORTING && person.hasEffect(MobEffects.REGENERATION),
				"Bonded, it mends its badly hurt person");
		person.removeAllEffects();
		helper.assertTrue(familiar.think(level, NIGHT, game + 40) != Status.SUPPORTING && !person.hasEffect(MobEffects.REGENERATION),
				"Not again within its cooldown");
		helper.assertTrue(BondingCharmItem.use(person, level, true).equals(InteractionResult.SUCCESS) && familiar.isRemoved()
				&& WorkerRoster.of(level.getServer()).familiar(person.getUUID()) == null, "Sneaking releases it");
		HearthlingEntity orphan = Workers.HEARTHLING.create(level, EntitySpawnReason.MOB_SUMMONED);
		UUID absent = new UUID(17L, 99L);
		orphan.setOwner(absent);
		place(helper, orphan, 2.5, 1.0, 2.5);
		level.addFreshEntity(orphan);
		WorkerRoster.of(level.getServer()).note(absent, orphan.getUUID(), "familiar", Status.IDLE, "minecraft:overworld", orphan.blockPosition());
		helper.assertTrue(orphan.think(level, NIGHT, game) == Status.OWNER_OFFLINE, "Its person offline, it waits and says so");
		helper.succeed();
	}

	/**
	 * D-S17: logged out for days, the familiar unloaded and loaded again meanwhile, its bond does not fade, though its person
	 * comes back 20 blocks away; nor does it fade for days they play while the Concordance is switched off (D-S16).
	 */
	@GameTest(maxTicks = 20)
	public void aFamiliarDoesNotFadeWhileItsPersonIsAway(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		ServerPlayer person = taught(TestPlayers.survivor(helper));
		place(helper, person, 2.5, 1.0, 2.5);
		HearthlingEntity familiar = bindFamiliar(helper, person);
		TestPlayers.play(person, 5_000);
		long game = level.getGameTime() + 400_000L;
		familiar.setBond(new Bond(50, Long.MIN_VALUE, 0, Long.MIN_VALUE / 2, 0L, Bond.UNSEEN));
		familiar.think(level, NIGHT, game);
		helper.assertTrue(familiar.bond().strength() == 51 && familiar.bond().seenPlayed() == 5_000L,
				"Together, the bond grows and counts its person's play time from here: " + familiar.bond());
		UUID id = person.getUUID();
		TestPlayers.leave(person);
		helper.assertTrue(familiar.think(level, NIGHT, game + Bond.DAY) == Status.OWNER_OFFLINE && familiar.bond().strength() == 51,
				"Its person logged out, it waits and says so, its bond unchanged");
		TagValueOutput saved = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, level.registryAccess());
		familiar.saveWithoutId(saved);
		familiar.remove(Entity.RemovalReason.UNLOADED_TO_CHUNK);
		HearthlingEntity reloaded = Workers.HEARTHLING.create(level, EntitySpawnReason.LOAD);
		reloaded.load(TagValueInput.create(ProblemReporter.DISCARDING, level.registryAccess(), saved.buildResult()));
		level.addFreshEntity(reloaded);
		ServerPlayer back = TestPlayers.again(helper, id);
		place(helper, back, 22.5, 1.0, 2.5);
		helper.assertTrue(TestPlayers.played(back) == 5_000L && back.distanceTo(reloaded) >= 19.0F,
				"The same person comes back 20 blocks away, with their own play time: " + TestPlayers.played(back));
		reloaded.think(level, NIGHT, game + 3 * Bond.DAY);
		helper.assertValueEqual(reloaded.bond().strength(), 51, "the bond after three days logged out, the familiar unloaded meanwhile");
		off(() -> {
			TestPlayers.play(back, 2 * (int) Bond.DAY);
			helper.assertTrue(reloaded.think(level, NIGHT, game + 4 * Bond.DAY) == Status.DISABLED && reloaded.bond().strength() == 51,
					"Switched off, it waits and says so, its bond unchanged");
		});
		reloaded.think(level, NIGHT, game + 5 * Bond.DAY);
		helper.assertValueEqual(reloaded.bond().strength(), 51, "the bond after two days played while the Concordance was switched off");
		helper.succeed();
	}

	/**
	 * D-S17: days its person plays away fade it by 4 each, once, in this level or another; back beside it, that day fades it
	 * before the visit counts.
	 */
	@GameTest(maxTicks = 20)
	public void aFamiliarFadesWhileItsPersonPlaysElsewhere(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		ServerPlayer person = taught(TestPlayers.survivor(helper));
		place(helper, person, 2.5, 1.0, 2.5);
		HearthlingEntity familiar = bindFamiliar(helper, person);
		long game = level.getGameTime() + 500_000L;
		familiar.setBond(new Bond(50, Long.MIN_VALUE, 0, Long.MIN_VALUE / 2, 0L, Bond.UNSEEN));
		familiar.think(level, NIGHT, game);
		helper.assertValueEqual(familiar.bond().strength(), 51, "Together, the bond grows");
		place(helper, person, 22.5, 1.0, 2.5);
		TestPlayers.play(person, 3 * (int) Bond.DAY);
		familiar.think(level, NIGHT, game + 40);
		helper.assertValueEqual(familiar.bond().strength(), 51 - 3 * Bond.NEGLECT, "three days played 20 blocks away fade it by 12");
		familiar.think(level, NIGHT, game + 60);
		helper.assertValueEqual(familiar.bond().strength(), 51 - 3 * Bond.NEGLECT, "once");
		ServerLevel nether = level.getServer().getLevel(Level.NETHER);
		helper.assertTrue(nether != null, "The test server has a Nether");
		person.teleport(new TeleportTransition(nether, new Vec3(0.5, 64.0, 0.5), Vec3.ZERO, 0.0F, 0.0F, TeleportTransition.DO_NOTHING));
		helper.assertTrue(person.level() == nether && familiar.think(level, NIGHT, game + 80) == Status.OTHER_DIMENSION
				&& familiar.bond().strength() == 51 - 3 * Bond.NEGLECT, "Its person in another dimension, it waits and says so");
		TestPlayers.play(person, (int) Bond.DAY);
		person.teleport(new TeleportTransition(level, helper.absoluteVec(new Vec3(3.5, 1.0, 2.5)), Vec3.ZERO, 0.0F, 0.0F, TeleportTransition.DO_NOTHING));
		familiar.think(level, NIGHT, game + 400);
		helper.assertValueEqual(familiar.bond().strength(), 51 - 4 * Bond.NEGLECT + Bond.GAIN,
				"a day played in another dimension, then back beside it: that day fades it before the visit counts");
		helper.succeed();
	}

	// ---------------------------------------------------------------- spirits

	/**
	 * A Spirit Anchor seals an agreement as a Bound Will and calls a Gathering Shade; at night it gathers a dropped item
	 * and delivers it to the anchor; at noon it is outside its agreement; suspended, it says so; the anchor broken, the
	 * Bound Will is released and the shade departs.
	 */
	@GameTest(maxTicks = 20)
	public void aSpiritKeepsToItsAgreement(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		ServerPlayer holder = binder(helper);
		BlockPos at = new BlockPos(3, 1, 3);
		helper.setBlock(at, Workers.SPIRIT_ANCHOR);
		SpiritAnchorBlockEntity anchor = (SpiritAnchorBlockEntity) level.getBlockEntity(helper.absolutePos(at));
		SpiritAnchorBlock.use(holder, level, anchor, false);
		helper.assertTrue(anchor.agreement() != null && anchor.spirit() != null, "Sealed");
		helper.assertTrue(BoundWills.of(level.getServer()).ledger().heldBy(holder.getUUID()).size() == 1, "as a Bound Will its holder holds");
		GatheringShadeEntity shade = (GatheringShadeEntity) level.getEntity(anchor.spirit());
		long game = level.getGameTime() + 200_000L;
		ItemEntity dropped = new ItemEntity(level, shade.getX(), shade.getY(), shade.getZ(), new ItemStack(Items.WHEAT, 3));
		dropped.setPickUpDelay(0);
		level.addFreshEntity(dropped);
		helper.assertTrue(shade.think(level, NIGHT, game) == Status.WORKING && shade.carriedCount() == 3, "At night it gathers: " + shade.status());
		Status delivered = shade.think(level, NIGHT, game + 20);
		helper.assertTrue(shade.carriedCount() == 0 && anchor.contents().stream().anyMatch(stack -> stack.is(Items.WHEAT) && stack.getCount() == 3),
				"and delivers to its anchor: " + delivered);
		helper.assertTrue(anchor.agreement().done(game + 20) == 1, "one task of its quota");
		helper.assertTrue(shade.think(level, NOON, game + 40) == Status.OUTSIDE_AGREEMENT, "At noon it is outside its agreement");
		SpiritAnchorBlock.use(holder, level, anchor, true);
		helper.assertTrue(anchor.holder().held(level) == Status.SUSPENDED && !anchor.agreement().suspended()
				&& shade.think(level, NIGHT, game + 60) == Status.SUSPENDED, "Suspended on its Bound Will, the one authority, it says so");
		helper.setBlock(at, Blocks.AIR);
		helper.assertTrue(shade.isRemoved() && BoundWills.of(level.getServer()).ledger().heldBy(holder.getUUID()).isEmpty(),
				"The anchor broken, the agreement is released and the shade departs");
		helper.succeed();
	}

	/**
	 * D-01 (D-S18, G1): killed, a shade's load goes into its loaded anchor once; it is struck off; its agreement stays and its
	 * anchor knows it is lost; a second removal leaves nothing more.
	 */
	@GameTest(maxTicks = 40)
	public void aKilledShadeLeavesItsLoadOnce(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		ServerPlayer holder = binder(helper);
		SpiritAnchorBlockEntity anchor = anchor(helper, new BlockPos(3, 1, 3));
		SpiritAnchorBlock.use(holder, level, anchor, false);
		UUID first = anchor.spirit();
		GatheringShadeEntity shade = (GatheringShadeEntity) level.getEntity(first);
		helper.assertValueEqual(gather(level, shade, level.getGameTime() + 200_000L, 3), 3, "At night it gathers three");
		shade.kill(level);
		helper.runAfterDelay(25, () -> {
			helper.assertTrue(shade.isRemoved(), "Killed, it is gone once its death is over");
			helper.assertTrue(wheat(anchor) == 3 && loose(helper, Items.WHEAT) == 0 && shade.carriedCount() == 0,
					"What it carried is in its anchor, none loose: " + wheat(anchor) + ", " + loose(helper, Items.WHEAT));
			helper.assertTrue(!WorkerRoster.of(level.getServer()).workers(holder.getUUID()).containsKey(first), "It is struck off its holder's roster");
			helper.assertTrue(anchor.agreement() != null && first.equals(anchor.spirit()) && anchor.holder().spiritLost(level)
					&& held(level, holder.getUUID()) == 1, "Its agreement and Bound Will stay, and its anchor knows it is lost");
			shade.remove(Entity.RemovalReason.KILLED);
			helper.assertTrue(wheat(anchor) == 3 && loose(helper, Items.WHEAT) == 0, "A second removal leaves nothing more");
			helper.succeed();
		});
	}

	/**
	 * D-01 (D-S18, D-S16): its anchor not loaded, a killed shade's load drops where it fell, once, even switched off and with
	 * the block-drop rule off (the unit review's fix 3: its holder's things are not something mined).
	 */
	@GameTest(maxTicks = 20)
	public void aKilledShadeAwayFromItsAnchorDropsWhereItFell(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		ServerPlayer holder = binder(helper);
		BlockPos at = new BlockPos(1, 1, 1);
		SpiritAnchorBlockEntity anchor = anchor(helper, at);
		SpiritAnchorBlock.use(holder, level, anchor, false);
		GatheringShadeEntity shade = (GatheringShadeEntity) level.getEntity(anchor.spirit());
		helper.assertValueEqual(gather(level, shade, level.getGameTime() + 200_000L, 2), 2, "It gathers two");
		place(helper, shade, 6.5, 1.0, 6.5);
		shade.setAnchor(helper.absolutePos(at).offset(100_000, 0, 0)); // as if its anchor's chunk were not loaded
		BlockPos fell = shade.blockPosition();
		GameRules rules = level.getGameRules();
		boolean drops = rules.get(GameRules.BLOCK_DROPS);
		rules.set(GameRules.BLOCK_DROPS, false, level.getServer());
		try {
			off(() -> shade.remove(Entity.RemovalReason.KILLED));
		} finally {
			rules.set(GameRules.BLOCK_DROPS, drops, level.getServer());
		}
		int near = level.getEntitiesOfClass(ItemEntity.class, new AABB(fell).inflate(1.5), item -> item.isAlive() && item.getItem().is(Items.WHEAT))
				.stream().mapToInt(item -> item.getItem().getCount()).sum();
		helper.assertTrue(near == 2 && wheat(anchor) == 0 && shade.carriedCount() == 0,
				"Its anchor not loaded, its load drops where it fell, once, even switched off and with blocks dropping nothing: " + near);
		helper.assertTrue(!WorkerRoster.of(level.getServer()).workers(holder.getUUID()).containsKey(shade.getUUID()) && anchor.holder().spiritLost(level),
				"It is struck off its holder's roster, and its anchor knows it is lost");
		helper.succeed();
	}

	/**
	 * D-01 (D-S18): a lost shade is reported; switched off nothing is called; its holder's next use calls another, at no
	 * cost, and its Bound Will names it; a full roster refuses; an agreement whose Bound Will is gone is forgotten (D-02, D-P7).
	 */
	@GameTest(maxTicks = 20)
	public void aLostShadeIsCalledAgainUnderItsAgreement(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		ServerPlayer holder = binder(helper);
		ServerPlayer passer = binder(helper);
		UUID id = holder.getUUID();
		BlockPos at = new BlockPos(3, 1, 3);
		SpiritAnchorBlockEntity anchor = anchor(helper, at);
		SpiritAnchorBlock.use(holder, level, anchor, false);
		UUID record = anchor.agreement().record();
		UUID first = anchor.spirit();
		WorkerRoster roster = WorkerRoster.of(level.getServer());
		BoundWills wills = BoundWills.of(level.getServer());
		level.getEntity(first).remove(Entity.RemovalReason.KILLED);
		helper.assertTrue(anchor.holder().spiritLost(level), "Killed, its anchor knows its shade is lost");
		helper.assertValueEqual(key(SpiritAnchorBlock.use(passer, level, anchor, false)), "message.jugcraft.concordance.workers.spirit_lost",
				"and says so to anyone who reads it");
		helper.assertTrue(first.equals(anchor.spirit()) && shades(helper, id) == 0, "who calls nothing");
		off(() -> helper.assertValueEqual(key(SpiritAnchorBlock.use(holder, level, anchor, false)), "message.jugcraft.concordance.disabled",
				"Switched off, nothing is called"));
		helper.assertTrue(first.equals(anchor.spirit()) && shades(helper, id) == 0, "and nothing changes");
		helper.assertValueEqual(key(SpiritAnchorBlock.use(holder, level, anchor, false)), "message.jugcraft.concordance.workers.spirit_recalled",
				"Its holder's next use calls another");
		UUID second = anchor.spirit();
		helper.assertTrue(second != null && !second.equals(first) && level.getEntity(second) instanceof GatheringShadeEntity shade
				&& id.equals(shade.owner()) && helper.absolutePos(at).equals(shade.anchor()), "a new shade for the same holder at the same anchor");
		helper.assertTrue(record.equals(anchor.agreement().record()) && held(level, id) == 1, "under the same agreement and Bound Will, at no cost");
		helper.assertTrue(second.equals(wills.ledger().get(record).pact().presence()), "the Bound Will names its new presence");
		helper.assertTrue(roster.workers(id).containsKey(second) && !roster.workers(id).containsKey(first), "on the roster at once, in the lost one's place");
		helper.assertValueEqual(key(SpiritAnchorBlock.use(holder, level, anchor, false)), "message.jugcraft.concordance.workers.spirit",
				"Used again, it reads its shade: no second call");
		helper.assertTrue(second.equals(anchor.spirit()) && shades(helper, id) == 1, "one shade");
		level.getEntity(second).remove(Entity.RemovalReason.KILLED);
		String dimension = level.dimension().identifier().toString();
		for (int i = 0; i < WorkerRoster.MAX_PER_PLAYER; i++) {
			roster.note(id, UUID.randomUUID(), "construct", Status.IDLE, dimension, helper.absolutePos(at));
		}
		Component crowded = SpiritAnchorBlock.use(holder, level, anchor, false);
		helper.assertTrue(key(crowded).equals("message.jugcraft.concordance.workers.recall_roster_full")
				&& Integer.valueOf(WorkerRoster.MAX_PER_PLAYER).equals(arg(crowded, 0)), "A full roster refuses the call and says so: " + crowded);
		helper.assertTrue(second.equals(anchor.spirit()) && shades(helper, id) == 0 && anchor.holder().spiritLost(level), "still lost; nothing called");
		List.copyOf(roster.workers(id).keySet()).forEach(worker -> roster.remove(id, worker));
		wills.release(record, id);
		helper.assertValueEqual(key(SpiritAnchorBlock.use(holder, level, anchor, false)), "message.jugcraft.concordance.workers.sealed",
				"An agreement whose Bound Will is gone is forgotten, and the anchor may be sealed again");
		helper.assertTrue(!record.equals(anchor.agreement().record()) && held(level, id) == 1, "under a new Bound Will");
		helper.succeed();
	}

	/**
	 * D-01 (D-S18, critique 22): unloaded is not lost; one missing from the roster is, and is replaced; the old one, loading
	 * again, takes no roster room while switched off, and leaves its load in the anchor once and departs.
	 */
	@GameTest(maxTicks = 20)
	public void anUnloadedShadeReplacedLeavesOnceAndDeliversOnce(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		ServerPlayer holder = binder(helper);
		UUID id = holder.getUUID();
		SpiritAnchorBlockEntity anchor = anchor(helper, new BlockPos(3, 1, 3));
		SpiritAnchorBlock.use(holder, level, anchor, false);
		GatheringShadeEntity old = (GatheringShadeEntity) level.getEntity(anchor.spirit());
		long game = level.getGameTime() + 200_000L;
		helper.assertValueEqual(gather(level, old, game, 3), 3, "It gathers three");
		TagValueOutput saved = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, level.registryAccess());
		old.saveWithoutId(saved);
		old.remove(Entity.RemovalReason.UNLOADED_TO_CHUNK);
		WorkerRoster roster = WorkerRoster.of(level.getServer());
		helper.assertTrue(roster.workers(id).containsKey(old.getUUID()) && !anchor.holder().spiritLost(level),
				"Unloaded is not lost: it keeps its place on the roster");
		helper.assertValueEqual(key(SpiritAnchorBlock.use(holder, level, anchor, false)), "message.jugcraft.concordance.workers.spirit_away",
				"and its anchor says it is away, calling nothing");
		helper.assertTrue(old.getUUID().equals(anchor.spirit()) && shades(helper, id) == 0, "nothing called");
		roster.remove(id, old.getUUID()); // as one sealed before its roster note, or while the roster was full
		helper.assertValueEqual(key(SpiritAnchorBlock.use(holder, level, anchor, false)), "message.jugcraft.concordance.workers.spirit_recalled",
				"Missing from the roster it is lost, and its holder calls another");
		UUID second = anchor.spirit();
		GatheringShadeEntity back = Workers.GATHERING_SHADE.create(level, EntitySpawnReason.LOAD);
		back.load(TagValueInput.create(ProblemReporter.DISCARDING, level.registryAccess(), saved.buildResult()));
		helper.assertTrue(back.getUUID().equals(old.getUUID()) && back.carriedCount() == 3, "The old shade loads again with its load");
		off(() -> helper.assertTrue(back.think(level, NIGHT, game + 20) == Status.DISABLED && !roster.workers(id).containsKey(old.getUUID()),
				"Switched off, it waits and takes no roster room (the unit review's fix 2)"));
		helper.assertTrue(back.think(level, NIGHT, game + 20) == Status.FINISHED && back.isRemoved() && back.carriedCount() == 0,
				"finds another in its place and leaves");
		helper.assertValueEqual(wheat(anchor), 3, "leaving its load in its anchor");
		back.remove(Entity.RemovalReason.KILLED);
		helper.assertTrue(wheat(anchor) == 3 && loose(helper, Items.WHEAT) == 0, "once: nothing more on a second removal");
		helper.assertTrue(second.equals(anchor.spirit()) && roster.workers(id).containsKey(second) && !roster.workers(id).containsKey(old.getUUID())
				&& held(level, id) == 1 && shades(helper, id) == 1, "the new shade keeps the agreement, alone");
		helper.succeed();
	}

	/** D-01 (G5): a sealed shade is on its holder's roster before it has thought; one missing from it notes itself when it next thinks. */
	@GameTest(maxTicks = 20)
	public void aSealedShadeIsOnTheRosterAtOnce(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		ServerPlayer holder = binder(helper);
		SpiritAnchorBlockEntity anchor = anchor(helper, new BlockPos(3, 1, 3));
		SpiritAnchorBlock.use(holder, level, anchor, false);
		GatheringShadeEntity shade = (GatheringShadeEntity) level.getEntity(anchor.spirit());
		WorkerRoster roster = WorkerRoster.of(level.getServer());
		WorkerRoster.Entry entry = roster.workers(holder.getUUID()).get(shade.getUUID());
		helper.assertTrue(entry != null && entry.kind().equals("spirit") && entry.status().equals(Status.IDLE.id)
				&& entry.dimension().equals(level.dimension().identifier().toString()) && entry.pos() == shade.blockPosition().asLong(),
				"Sealed, the shade is on its holder's roster before it has thought: " + entry);
		roster.remove(holder.getUUID(), shade.getUUID());
		helper.assertTrue(shade.think(level, NIGHT, level.getGameTime() + 200_000L) == Status.IDLE
				&& roster.workers(holder.getUUID()).containsKey(shade.getUUID()), "One missing from it notes itself when it next thinks");
		helper.succeed();
	}

	/**
	 * D-01 (D-S11, G5): a full roster refuses the seal before anything is recorded and says why; with room it seals and the
	 * shade takes the last place at once.
	 */
	@GameTest(maxTicks = 20)
	public void aFullRosterRefusesTheSeal(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		ServerPlayer holder = binder(helper);
		UUID id = holder.getUUID();
		BlockPos at = new BlockPos(3, 1, 3);
		SpiritAnchorBlockEntity anchor = anchor(helper, at);
		WorkerRoster roster = WorkerRoster.of(level.getServer());
		String dimension = level.dimension().identifier().toString();
		for (int i = 0; i < WorkerRoster.MAX_PER_PLAYER; i++) {
			roster.note(id, UUID.randomUUID(), "construct", Status.IDLE, dimension, helper.absolutePos(at));
		}
		Component crowded = SpiritAnchorBlock.use(holder, level, anchor, false);
		helper.assertTrue(key(crowded).equals("message.jugcraft.concordance.workers.seal_roster_full")
				&& Integer.valueOf(WorkerRoster.MAX_PER_PLAYER).equals(arg(crowded, 0)), "A full roster refuses the seal and says why: " + crowded);
		helper.assertTrue(anchor.agreement() == null && anchor.spirit() == null && held(level, id) == 0
				&& roster.workers(id).size() == WorkerRoster.MAX_PER_PLAYER && shades(helper, id) == 0, "before anything is recorded or called");
		roster.remove(id, roster.workers(id).keySet().iterator().next());
		helper.assertValueEqual(key(SpiritAnchorBlock.use(holder, level, anchor, false)), "message.jugcraft.concordance.workers.sealed", "With room, it seals");
		helper.assertTrue(anchor.spirit() != null && roster.workers(id).containsKey(anchor.spirit())
				&& roster.workers(id).size() == WorkerRoster.MAX_PER_PLAYER && held(level, id) == 1, "and the shade takes the last place at once");
		helper.succeed();
	}

	/**
	 * D-01 (G4, D-S13): a holder with the most Bound Wills is told so and nothing is sealed; a seal its sealer cannot pay for
	 * is refused before anything is called, and one they can pay for is paid once, once sealed; a second seal at a kept
	 * anchor is TAKEN and changes nothing.
	 */
	@GameTest(maxTicks = 20)
	public void aFullHolderIsToldWhy(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		ServerPlayer holder = binder(helper);
		UUID id = holder.getUUID();
		SpiritAnchorBlockEntity anchor = anchor(helper, new BlockPos(3, 1, 3));
		BoundWills wills = BoundWills.of(level.getServer());
		List<UUID> kept = new ArrayList<>();
		for (int i = 0; i < BoundWillLedger.MAX_PER_HOLDER; i++) {
			BoundWill will = new BoundWill(UUID.randomUUID(), "jugcraft:test_pact", "jugcraft:test_spirit", id, 0L, false);
			wills.seal(will);
			kept.add(will.id());
		}
		Component full = SpiritAnchorBlock.use(holder, level, anchor, false);
		helper.assertTrue(key(full).equals("message.jugcraft.concordance.workers.seal_holder_full")
				&& Integer.valueOf(BoundWillLedger.MAX_PER_HOLDER).equals(arg(full, 0)), "Holding the most Bound Wills, they are told so: " + full);
		helper.assertTrue(anchor.agreement() == null && anchor.spirit() == null && held(level, id) == BoundWillLedger.MAX_PER_HOLDER
				&& WorkerRoster.of(level.getServer()).workers(id).isEmpty() && shades(helper, id) == 0, "and nothing is sealed, recorded or called");
		kept.forEach(will -> wills.release(will, id));
		long now = level.getGameTime();
		int[] paid = {0};
		AgreementHolder.Sealing plain = anchor.sealing(terms(), null);
		AgreementHolder.Sealing dear = new AgreementHolder.Sealing(plain.agreement(), plain.counterpart(), plain.service(), plain.upkeep(),
				plain.beneficiaries(), plain.onePerHolder(), plain.partySeals(), player -> false, player -> paid[0]++);
		helper.assertValueEqual(anchor.holder().seal(level, holder, dear, now).outcome(), Pacts.Outcome.CANNOT_AFFORD, "A seal they cannot pay for is refused");
		helper.assertTrue(paid[0] == 0 && anchor.agreement() == null && held(level, id) == 0 && shades(helper, id) == 0
				&& AgreementHolder.told(Pacts.Outcome.CANNOT_AFFORD).getContents() instanceof TranslatableContents why
				&& why.getKey().equals("message.jugcraft.concordance.workers.seal_cannot_afford"), "and says so, before anything is spent, recorded or called");
		SpiritAnchorBlockEntity other = anchor(helper, new BlockPos(5, 1, 5));
		AgreementHolder.Sealing paying = new AgreementHolder.Sealing(plain.agreement(), plain.counterpart(), plain.service(), plain.upkeep(),
				plain.beneficiaries(), plain.onePerHolder(), plain.partySeals(), player -> true, player -> paid[0]++);
		helper.assertTrue(other.holder().seal(level, holder, paying, now).outcome() == Pacts.Outcome.DONE && paid[0] == 1 && held(level, id) == 1,
				"One they can pay for is sealed and paid once");
		helper.assertValueEqual(key(SpiritAnchorBlock.use(holder, level, anchor, false)), "message.jugcraft.concordance.workers.sealed", "With fewer, it seals");
		ServerPlayer stranger = binder(helper);
		UUID spirit = anchor.spirit();
		helper.assertValueEqual(anchor.seal(level, stranger, terms(), level.getGameTime()).outcome(), Pacts.Outcome.TAKEN,
				"A second seal at a kept anchor is refused as taken");
		helper.assertTrue(spirit.equals(anchor.spirit()) && id.equals(anchor.agreement().holder()) && held(level, stranger.getUUID()) == 0
				&& paid[0] == 1, "and changes nothing");
		helper.succeed();
	}

	/**
	 * D-01 (G6, E1, E4): neither another player, nor a stand-in, nor its holder while switched off ends it; its holder ends it
	 * without breaking the anchor, once; the anchor may be sealed again.
	 */
	@GameTest(maxTicks = 20)
	public void theHolderEndsWithoutBreaking(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		ServerPlayer holder = binder(helper);
		ServerPlayer other = binder(helper);
		BlockPos at = new BlockPos(3, 1, 3);
		SpiritAnchorBlockEntity anchor = anchor(helper, at);
		SpiritAnchorBlock.use(holder, level, anchor, false);
		GatheringShadeEntity shade = (GatheringShadeEntity) level.getEntity(anchor.spirit());
		UUID spirit = shade.getUUID();
		UUID record = anchor.agreement().record();
		long now = level.getGameTime();
		helper.assertValueEqual(gather(level, shade, now + 200_000L, 3), 3, "It gathers three");
		helper.assertValueEqual(anchor.holder().end(level, other, now), AgreementHolder.Ended.NOT_HOLDER, "Another player cannot end it");
		FakePlayer standIn = FakePlayer.get(level, new GameProfile(holder.getUUID(), "[Concordance]"));
		helper.assertValueEqual(anchor.holder().end(level, standIn, now), AgreementHolder.Ended.NOT_PERMITTED, "nor a stand-in with its holder's identity");
		off(() -> helper.assertValueEqual(anchor.holder().end(level, holder, now), AgreementHolder.Ended.DISABLED, "Switched off, not even its holder"));
		helper.assertTrue(anchor.agreement() != null && !shade.isRemoved() && shade.carriedCount() == 3 && held(level, holder.getUUID()) == 1,
				"So far nothing has changed");
		helper.assertValueEqual(anchor.holder().end(level, holder, now), AgreementHolder.Ended.ENDED, "Its holder ends it without breaking the anchor");
		helper.assertTrue(anchor.agreement() == null && anchor.spirit() == null && held(level, holder.getUUID()) == 0 && shade.isRemoved()
				&& !WorkerRoster.of(level.getServer()).workers(holder.getUUID()).containsKey(spirit), "its Bound Will released, its shade gone and off the roster");
		helper.assertTrue(wheat(anchor) == 3 && loose(helper, Items.WHEAT) == 0 && helper.getBlockState(at).is(Workers.SPIRIT_ANCHOR),
				"what it carried left in the anchor, which stays");
		helper.assertValueEqual(anchor.holder().end(level, holder, now), AgreementHolder.Ended.NOTHING, "A second end finds nothing");
		helper.assertTrue(AgreementHolder.end(level, null, holder, record, Pacts.ENDED, now) == AgreementHolder.Ended.NOTHING
				&& wheat(anchor) == 3 && loose(helper, Items.WHEAT) == 0, "and ending the record again returns nothing more");
		helper.assertValueEqual(key(SpiritAnchorBlock.use(holder, level, anchor, false)), "message.jugcraft.concordance.workers.sealed", "The anchor may be sealed again");
		helper.succeed();
	}

	/**
	 * D-02 (E2, D-S3): due with none held, a warning and a day of grace; its end, suspended for upkeep (nothing taken, nothing
	 * accrues, its holder's word cannot lift it); an offering resumes it at once, even in the tick of a ticker's look, a whole
	 * period from then (critique 3: the time held is never charged); paid ahead, one period at a time.
	 */
	@GameTest(maxTicks = 20)
	public void upkeepWarnsThenSuspendsAfterGraceAndResumes(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		ServerPlayer holder = binder(helper);
		BoundWills wills = BoundWills.of(level.getServer());
		long t0 = level.getGameTime() + 1_000_000L;
		SpiritAnchorBlockEntity anchor = sealed(helper, holder, new BlockPos(3, 1, 3), UPKEEP, t0);
		GatheringShadeEntity shade = (GatheringShadeEntity) level.getEntity(anchor.spirit());
		UUID id = anchor.agreement().record();
		helper.assertTrue(upkeep(level, anchor).due() == t0 + PERIOD && !upkeep(level, anchor).warned(), "Sealed, its first offering falls due a period on");
		helper.assertTrue(stay(level, anchor, t0 + 1_000L, t0 + PERIOD - 1_000L).isEmpty(), "Nothing falls due before then");
		long t1 = t0 + PERIOD;
		helper.assertValueEqual(anchor.holder().look(level, t1), Pacts.Event.WARNED, "Due with none held: a warning");
		helper.assertTrue(upkeep(level, anchor).graceUntil() == t1 + DAY && Pacts.status(will(level, anchor)) == Status.UPKEEP_DUE
				&& anchor.holder().held(level) == null && shade.think(level, NIGHT, t1) != Status.UPKEEP_HELD, "and a day of grace, the service still on");
		helper.assertTrue(wills.problems().isEmpty(), "warned, the records are sound: " + wills.problems());
		helper.assertTrue(stay(level, anchor, t1 + 1_000L, t1 + DAY - 1_000L).isEmpty() && anchor.holder().held(level) == null, "Within its grace, nothing more");
		helper.assertValueEqual(anchor.holder().look(level, t1 + DAY), Pacts.Event.SUSPENDED, "The grace over, it is suspended for upkeep");
		helper.assertTrue(anchor.holder().held(level) == Status.UPKEEP_HELD && shade.think(level, NIGHT, t1 + DAY) == Status.UPKEEP_HELD, "and its shade says so");
		helper.assertTrue(anchor.isEmpty() && escrowed(level, id) == 0 && wills.problems().isEmpty(), "Nothing is taken, from the anchor or anyone");
		helper.assertTrue(stay(level, anchor, t1 + DAY + 1_000L, t1 + 6 * DAY).isEmpty() && upkeep(level, anchor).due() == t0 + PERIOD
				&& will(level, anchor).pact().hold() == BoundWill.Hold.UPKEEP, "Held, nothing accrues and nothing more falls due");
		helper.assertValueEqual(key(SpiritAnchorBlock.use(holder, level, anchor, true)), "message.jugcraft.concordance.workers.held_for_upkeep",
				"Its holder's word does not lift an upkeep hold");
		helper.assertTrue(will(level, anchor).pact().hold() == BoundWill.Hold.UPKEEP, "it stays held");
		long t2 = t1 + 6 * DAY + 500L;
		helper.assertTrue(anchor.holder().look(level, t2) == Pacts.Event.NONE && upkeep(level, anchor).lookedAt() == t2,
				"A ticker's look in the offering's tick (the unit review's fix 7)");
		ItemStack offered = new ItemStack(Items.WOOL.white(), 6);
		helper.assertValueEqual(anchor.holder().deposit(level, holder, offered, t2), 4, "An offering fills its escrow to its cap");
		helper.assertTrue(offered.getCount() == 2 && anchor.holder().held(level) == null && escrowed(level, id) == 3
				&& upkeep(level, anchor).due() == t2 + PERIOD && !upkeep(level, anchor).warned(),
				"takes one period and resumes it at once, the next a whole period on: " + upkeep(level, anchor));
		helper.assertTrue(shade.think(level, NIGHT, t2) != Status.UPKEEP_HELD, "its shade works again");
		helper.assertTrue(stay(level, anchor, t2 + 1_000L, t2 + PERIOD - 1_000L).isEmpty(), "Paid ahead, nothing falls due early");
		helper.assertValueEqual(anchor.holder().look(level, t2 + PERIOD), Pacts.Event.PAID, "At the next period one offering is taken");
		helper.assertTrue(escrowed(level, id) == 2 && upkeep(level, anchor).due() == t2 + 2 * PERIOD && wills.problems().isEmpty(),
				"one only, the next a period on");
		// D-05 (E2 on Stolas): his own terms; the compact paid the first period; warned, he serves through the grace; held for
		// upkeep he says so and reckons nothing; his own offerings resume both.
		ServerPlayer keeper = regarded(helper);
		long s0 = dayStart(level, 200);
		PactPerchBlockEntity perch = stolasPact(helper, keeper, new BlockPos(6, 1, 6), s0);
		UUID pact = perch.holder().agreement().record();
		observed(level, keeper, HARVEST_MOON);
		ItemStack charged = holding(keeper, AstrolabeItem.CAPACITY);
		BoundWill.Upkeep terms = pactOf(level, perch).pact().upkeep();
		helper.assertTrue(terms.offering().equals("jugcraft:stolas_offerings") && terms.count() == 1 && terms.period() == PERIOD_7 && terms.grace() == Pacts.DAY
				&& terms.cap() == 4, "Stolas's upkeep: one of his offerings a week, a day's grace, four ahead at most: " + terms);
		helper.assertTrue(terms.due() == s0 + PERIOD_7 && escrowed(level, pact) == 0, "The compact paid the first: nothing paid ahead, the first offering a week on");
		helper.assertTrue(stay(level, perch.holder(), s0 + 1_000L, s0 + PERIOD_7 - 1_000L).isEmpty(), "Nothing falls due before then");
		long s1 = s0 + PERIOD_7;
		helper.assertValueEqual(perch.holder().look(level, s1), Pacts.Event.WARNED, "Due with none held, a warning");
		helper.assertTrue(pactOf(level, perch).pact().upkeep().graceUntil() == s1 + Pacts.DAY && stolas(level, perch).think(level, NOON, s1) == Status.UPKEEP_DUE
				&& WorkerEntity.sign(Status.UPKEEP_DUE) == Sign.WANT, "a day of grace; Stolas says an offering is due, with a sign of want");
		helper.assertValueEqual(reckon(level, keeper, perch, HARVEST_MOON, s1, s1), Pacts.Reckoning.RECALLED, "Within the grace he still reckons");
		helper.assertTrue(stay(level, perch.holder(), s1 + 1_000L, s1 + Pacts.DAY - 1_000L).isEmpty(), "Nothing more within the grace");
		helper.assertValueEqual(perch.holder().look(level, s1 + Pacts.DAY), Pacts.Event.SUSPENDED, "The grace over, it is suspended for upkeep");
		int spent = AstrolabeItem.CAPACITY - Sky.catalog().pattern(HARVEST_MOON).recallCost();
		helper.assertTrue(stolas(level, perch).think(level, NOON, s1 + Pacts.DAY) == Status.UPKEEP_HELD
				&& reckon(level, keeper, perch, HARVEST_MOON, s1 + Pacts.DAY, s1 + Pacts.DAY) == Pacts.Reckoning.UPKEEP_HELD
				&& AstrolabeItem.charge(charged) == spent && escrowed(level, pact) == 0, "Stolas says so, reckons nothing, and nothing is taken");
		helper.assertValueEqual(PactKeeping.resume(level, keeper, perch.holder(), s1 + Pacts.DAY), AgreementHolder.Toggle.HELD_FOR_UPKEEP, "The holder's word does not lift it");
		give(keeper, new ItemStack(Items.AMETHYST_SHARD, 6));
		long s2 = s1 + Pacts.DAY + 500L;
		helper.assertValueEqual(PactKeeping.deposit(level, keeper, perch.holder(), Items.AMETHYST_SHARD, s2), 4, "Six offered: four go in, the most it holds");
		helper.assertTrue(shards(keeper) == 2 && escrowed(level, pact) == 3 && pactOf(level, perch).pact().hold() == BoundWill.Hold.NONE
				&& pactOf(level, perch).pact().upkeep().due() == s2 + PERIOD_7 && !pactOf(level, perch).pact().upkeep().warned(),
				"one taken for the period; resumed at once, the next a week on");
		helper.assertTrue(stolas(level, perch).think(level, NOON, s2) != Status.UPKEEP_HELD
				&& reckon(level, keeper, perch, HARVEST_MOON, s2 + Pacts.DAY, s2 + Pacts.DAY) == Pacts.Reckoning.RECALLED, "Stolas serves again");
		helper.assertTrue(stay(level, perch.holder(), s2 + 1_000L, s2 + PERIOD_7 - 1_000L).isEmpty() && perch.holder().look(level, s2 + PERIOD_7) == Pacts.Event.PAID
				&& escrowed(level, pact) == 2 && pactOf(level, perch).pact().upkeep().due() == s2 + 2 * PERIOD_7 && BoundWills.of(level.getServer()).problems().isEmpty(),
				"Paid ahead, one offering a week, no sooner");
		helper.succeed();
	}

	/**
	 * D-02 (E2, D-S3, critique 2a): a holder away while warned (from the moment of the warning, so the grace left is all of
	 * it, D-P50), or away past both due date and grace, is warned again at their first look back with the whole grace; never
	 * suspended at once, even when they suspend and resume it before that look. D-05: the same on Stolas, an offering
	 * resuming it.
	 */
	@GameTest(maxTicks = 20)
	public void aReturnAfterAbsenceGetsTheFullGrace(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		ServerPlayer holder = taught(TestPlayers.survivor(helper));
		UUID id = holder.getUUID();
		long t0 = level.getGameTime() + 1_000_000L;
		BlockPos first = new BlockPos(2, 1, 2);
		SpiritAnchorBlockEntity anchor = sealed(helper, holder, first, UPKEEP, t0);
		helper.assertTrue(stay(level, anchor, t0 + 1_000L, t0 + PERIOD - 1_000L).isEmpty(), "Nothing due before its period");
		long t1 = t0 + PERIOD;
		helper.assertTrue(anchor.holder().look(level, t1) == Pacts.Event.WARNED && upkeep(level, anchor).graceUntil() == t1 + DAY, "Warned, with a day of grace");
		TestPlayers.leave(holder);
		helper.assertTrue(stay(level, anchor, t1 + 1_000L, t1 + 10 * DAY).isEmpty() && anchor.holder().held(level) == null
				&& upkeep(level, anchor).graceUntil() == t1 + DAY, "Its holder away ten days, nothing is looked at: no suspension");
		ServerPlayer back = taught(TestPlayers.again(helper, id));
		beside(helper, back, first);
		long t2 = t1 + 10 * DAY + 500L;
		helper.assertValueEqual(anchor.holder().look(level, t2), Pacts.Event.WARNED, "Back, they are warned again");
		helper.assertTrue(upkeep(level, anchor).graceUntil() == t2 + DAY && anchor.holder().held(level) == null, "with the whole day of grace from their return");
		helper.assertTrue(stay(level, anchor, t2 + 1_000L, t2 + DAY - 1_000L).isEmpty(), "and nothing within it");
		helper.assertValueEqual(anchor.holder().look(level, t2 + DAY), Pacts.Event.SUSPENDED, "Still unpaid at its end, it is suspended; nothing is lost");
		BlockPos second = new BlockPos(5, 1, 2);
		long t3 = t2 + 2 * DAY;
		SpiritAnchorBlockEntity other = sealed(helper, back, second, UPKEEP, t3);
		helper.assertTrue(stay(level, other, t3 + 1_000L, t3 + DAY).isEmpty(), "A second agreement, a day attended");
		TestPlayers.leave(back);
		ServerPlayer again = taught(TestPlayers.again(helper, id));
		beside(helper, again, second);
		long tr = t3 + 21 * DAY;
		helper.assertValueEqual(other.holder().look(level, tr), Pacts.Event.WARNED, "Away past its due date and past a grace, the first look back warns");
		helper.assertTrue(upkeep(level, other).graceUntil() == tr + DAY && other.holder().held(level) == null, "with the whole grace: never suspended at once");
		UUID otherRecord = other.agreement().record();
		TestPlayers.leave(again);
		ServerPlayer later = taught(TestPlayers.again(helper, id));
		beside(helper, later, second);
		long ts = tr + 3 * DAY; // away past that grace too
		helper.assertTrue(AgreementHolder.toggle(level, later, otherRecord, ts) == AgreementHolder.Toggle.SUSPENDED
				&& AgreementHolder.toggle(level, later, otherRecord, ts + 100L) == AgreementHolder.Toggle.RESUMED,
				"Back, before any look, they suspend it and resume it");
		helper.assertTrue(other.holder().look(level, ts + 120L) == Pacts.Event.WARNED && upkeep(level, other).graceUntil() == ts + 120L + DAY
				&& other.holder().held(level) == null,
				"The absence before their hold still counts: warned again with the whole grace, never suspended at once (the unit review's fix 1)");
		// D-05 (E2 on Stolas): away ten game days with nothing paid ahead, the holder returns to a warning and a whole day's
		// grace; still unpaid at its end, the pact is suspended; a Stolas Starflower resumes it.
		ServerPlayer keeper = regarded(helper);
		UUID keeperId = keeper.getUUID();
		BlockPos perchAt = new BlockPos(6, 1, 6);
		long s0 = level.getGameTime() + 6_000_000L;
		PactPerchBlockEntity perch = stolasPact(helper, keeper, perchAt, s0);
		helper.assertTrue(stay(level, perch.holder(), s0 + 1_000L, s0 + PERIOD_7 - 1_000L).isEmpty() && perch.holder().look(level, s0 + PERIOD_7) == Pacts.Event.WARNED,
				"Warned when the first offering falls due");
		TestPlayers.leave(keeper);
		helper.assertTrue(stay(level, perch.holder(), s0 + PERIOD_7 + 1_000L, s0 + PERIOD_7 + 10 * Pacts.DAY).isEmpty() && perch.holder().held(level) == null,
				"Away ten game days, nothing is looked at: no suspension");
		ServerPlayer home = TestPlayers.again(helper, keeperId);
		RateGate.forget(keeperId);
		beside(helper, home, perchAt);
		long sr = s0 + PERIOD_7 + 10 * Pacts.DAY + 500L;
		helper.assertValueEqual(perch.holder().look(level, sr), Pacts.Event.WARNED, "Back, they are warned again");
		helper.assertTrue(pactOf(level, perch).pact().upkeep().graceUntil() == sr + Pacts.DAY && stolas(level, perch).think(level, NOON, sr) == Status.UPKEEP_DUE,
				"with the whole day of grace from their return");
		helper.assertTrue(stay(level, perch.holder(), sr + 1_000L, sr + Pacts.DAY - 1_000L).isEmpty() && perch.holder().look(level, sr + Pacts.DAY) == Pacts.Event.SUSPENDED,
				"Still unpaid at its end: suspended; nothing is lost");
		give(home, new ItemStack(starflower(), 1));
		helper.assertTrue(PactKeeping.deposit(level, home, perch.holder(), starflower(), sr + Pacts.DAY + 500L) == 1 && perch.holder().held(level) == null,
				"A Stolas Starflower resumes it");
		helper.succeed();
	}

	/**
	 * D-05 (D-S3, as the unit review proposes): leaving for a minute never renews a warning. Warned, then away beyond 64
	 * blocks for 61 seconds after every 1,000 ticks present, the holder is warned again at each return with the grace they
	 * had left, its end moved on by the time away (never charged); after one game day present the pact is suspended.
	 */
	@GameTest(maxTicks = 20)
	public void shortAbsencesNeverRenewTheGrace(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		ServerPlayer keeper = regarded(helper);
		BlockPos at = new BlockPos(3, 1, 3);
		long s0 = level.getGameTime() + 1_000_000L;
		PactPerchBlockEntity perch = stolasPact(helper, keeper, at, s0);
		helper.assertTrue(stay(level, perch.holder(), s0 + 1_000L, s0 + PERIOD_7 - 1_000L).isEmpty(), "Nothing falls due before its week");
		long now = s0 + PERIOD_7;
		helper.assertValueEqual(perch.holder().look(level, now), Pacts.Event.WARNED, "Due with none paid ahead: a warning and a day of grace");
		Vec3 far = helper.absoluteVec(new Vec3(at.getX() + 70.5, at.getY(), at.getZ() + 0.5));
		long present = 0L;
		int returns = 0;
		Pacts.Event seen = Pacts.Event.NONE;
		while (seen == Pacts.Event.NONE && present < 2 * Pacts.DAY) {
			for (int look = 0; look < 5 && seen == Pacts.Event.NONE; look++) {
				now += 200L;
				present += 200L;
				seen = perch.holder().look(level, now);
			}
			if (seen != Pacts.Event.NONE) {
				break;
			}
			keeper.snapTo(far.x, far.y, far.z, 0.0F, 0.0F);
			helper.assertValueEqual(perch.holder().look(level, now + 600L), Pacts.Event.NONE, "Away beyond 64 blocks, nothing is looked at");
			now += 1_220L;
			beside(helper, keeper, at);
			BoundWill.Upkeep before = pactOf(level, perch).pact().upkeep();
			helper.assertValueEqual(perch.holder().look(level, now), Pacts.Event.WARNED, "Back after 61 seconds, warned again");
			helper.assertValueEqual(pactOf(level, perch).pact().upkeep().graceUntil(), before.graceUntil() + (now - before.lookedAt()),
					"with the grace they had left, its end moved on by the time away");
			returns++;
		}
		helper.assertTrue(seen == Pacts.Event.SUSPENDED && present == Pacts.DAY && returns == 23
				&& pactOf(level, perch).pact().hold() == BoundWill.Hold.UPKEEP,
				"After one game day present the pact is suspended for upkeep: " + seen + " after " + present + " ticks present and " + returns + " returns");
		helper.succeed();
	}

	/**
	 * D-02 (E2, critique 2b): logged out, online beyond 64 blocks, in another dimension, or away while the server lets
	 * stand-ins answer for absent owners, nothing is looked at; back, at most one period falls due, and a due date that
	 * passed while nobody looked becomes the moment of return (critique 3): no debt. With no ticker in unit 1, "unloaded" is
	 * only a gap between looks here; D-04's perch tests unload the place itself (critique 16).
	 */
	@GameTest(maxTicks = 20)
	public void absentAndUnloadedTimeIsNeverOwed(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		ServerPlayer holder = taught(TestPlayers.survivor(helper));
		UUID id = holder.getUUID();
		BlockPos at = new BlockPos(3, 1, 3);
		long t0 = level.getGameTime() + 1_000_000L;
		SpiritAnchorBlockEntity anchor = sealed(helper, holder, at, UPKEEP, t0);
		UUID record = anchor.agreement().record();
		helper.assertValueEqual(anchor.holder().deposit(level, holder, new ItemStack(Items.WOOL.white(), 4), t0), 4, "Four periods paid ahead");
		TestPlayers.leave(holder);
		for (long t = t0 + DAY; t <= t0 + 30 * DAY; t += DAY) {
			helper.assertValueEqual(anchor.holder().look(level, t), Pacts.Event.NONE, "Logged out, nothing is looked at");
		}
		helper.assertValueEqual(escrowed(level, record), 4, "and nothing is taken");
		ServerPlayer back = taught(TestPlayers.again(helper, id));
		beside(helper, back, at);
		long tr = t0 + 30 * DAY + 500L;
		helper.assertValueEqual(anchor.holder().look(level, tr), Pacts.Event.PAID, "Back after thirty days, one period falls due");
		helper.assertTrue(escrowed(level, record) == 3 && upkeep(level, anchor).due() == tr + PERIOD, "one offering, never four; the next a period from now");
		long tu = tr + 21 * DAY; // nobody looks for three periods, as if the anchor were unloaded
		helper.assertValueEqual(anchor.holder().look(level, tu), Pacts.Event.PAID, "Unlooked at for three periods, one falls due");
		helper.assertTrue(escrowed(level, record) == 2 && upkeep(level, anchor).due() == tu + PERIOD, "one offering");
		Vec3 far = helper.absoluteVec(new Vec3(at.getX() + 70.5, at.getY(), at.getZ() + 0.5));
		back.snapTo(far.x, far.y, far.z, 0.0F, 0.0F);
		helper.assertTrue(stay(level, anchor, tu + 1_000L, tu + 3 * PERIOD).isEmpty() && escrowed(level, record) == 2,
				"Online but beyond 64 blocks, nothing is looked at");
		beside(helper, back, at);
		long tw = tu + 3 * PERIOD + 500L;
		helper.assertValueEqual(anchor.holder().look(level, tw), Pacts.Event.PAID, "Back near it, one period falls due");
		helper.assertTrue(escrowed(level, record) == 1 && upkeep(level, anchor).due() == tw + PERIOD && BoundWills.of(level.getServer()).problems().isEmpty(),
				"one offering for three periods away: no debt");
		long tx = tw + 2 * PERIOD - 1_000L; // no look since tw: an absence across tw + PERIOD
		helper.assertValueEqual(anchor.holder().look(level, tx), Pacts.Event.PAID, "Back just before the next due date, one offering");
		helper.assertTrue(upkeep(level, anchor).due() == tx + PERIOD && anchor.holder().look(level, tw + 2 * PERIOD) == Pacts.Event.NONE,
				"the next a period from the return, not 1,000 ticks later");
		BoundWill kept = will(level, anchor);
		ServerLevel nether = level.getServer().getLevel(Level.NETHER);
		helper.assertTrue(nether != null, "The test server has a Nether");
		back.teleport(new TeleportTransition(nether, new Vec3(0.5, 64.0, 0.5), Vec3.ZERO, 0.0F, 0.0F, TeleportTransition.DO_NOTHING));
		long tn = tx + PERIOD + 500L;
		helper.assertTrue(back.level() == nether && anchor.holder().look(level, tn) == Pacts.Event.NONE && will(level, anchor).equals(kept),
				"Its holder in another dimension, nothing is looked at");
		boolean answering = JugcraftConfig.option(Authority.ABSENT_OPTION);
		JugcraftConfig.setOption(Authority.ABSENT_OPTION, true);
		try {
			TestPlayers.leave(back);
			helper.assertTrue(anchor.holder().look(level, tn + PERIOD) == Pacts.Event.NONE && will(level, anchor).equals(kept),
					"Logged out while stand-ins may answer for absent owners, nothing is looked at: a stand-in never pays or is warned");
		} finally {
			JugcraftConfig.setOption(Authority.ABSENT_OPTION, answering);
		}
		// D-05 (E2 on Stolas): logged out, the perch unloaded (no look: its ticker runs only while loaded; modelled as a gap
		// between looks, not a chunk-load listener), online 100 blocks away, or in another level: nothing is looked at; back,
		// at most one offering each time.
		ServerPlayer keeper = regarded(helper);
		UUID keeperId = keeper.getUUID();
		BlockPos perchAt = new BlockPos(6, 1, 6);
		long s0 = level.getGameTime() + 9_000_000L;
		PactPerchBlockEntity perch = stolasPact(helper, keeper, perchAt, s0);
		UUID pact = perch.holder().agreement().record();
		give(keeper, new ItemStack(Items.AMETHYST_SHARD, 4));
		helper.assertValueEqual(PactKeeping.deposit(level, keeper, perch.holder(), Items.AMETHYST_SHARD, s0), 4, "Four paid ahead");
		TestPlayers.leave(keeper);
		for (long t = s0 + Pacts.DAY; t <= s0 + 30 * Pacts.DAY; t += Pacts.DAY) {
			helper.assertValueEqual(perch.holder().look(level, t), Pacts.Event.NONE, "Logged out, nothing is looked at");
			PactKeeping.tick(level, perch.holder(), perch.getBlockPos(), t);
		}
		helper.assertValueEqual(escrowed(level, pact), 4, "and nothing is taken");
		ServerPlayer stolasBack = TestPlayers.again(helper, keeperId);
		beside(helper, stolasBack, perchAt);
		long stolasTr = s0 + 30 * Pacts.DAY + 500L;
		helper.assertTrue(perch.holder().look(level, stolasTr) == Pacts.Event.PAID && escrowed(level, pact) == 3
				&& pactOf(level, perch).pact().upkeep().due() == stolasTr + PERIOD_7, "Back after thirty days: one offering, the next a week from now");
		long stolasTu = stolasTr + 21 * Pacts.DAY;
		helper.assertTrue(perch.holder().look(level, stolasTu) == Pacts.Event.PAID && escrowed(level, pact) == 2, "The perch unloaded three weeks: one offering");
		Vec3 stolasFar = helper.absoluteVec(new Vec3(perchAt.getX() + 100.5, perchAt.getY(), perchAt.getZ() + 0.5));
		stolasBack.snapTo(stolasFar.x, stolasFar.y, stolasFar.z, 0.0F, 0.0F);
		helper.assertTrue(stay(level, perch.holder(), stolasTu + 1_000L, stolasTu + 3 * PERIOD_7).isEmpty() && escrowed(level, pact) == 2,
				"Online 100 blocks away, nothing is looked at");
		beside(helper, stolasBack, perchAt);
		long stolasTw = stolasTu + 3 * PERIOD_7 + 500L;
		helper.assertTrue(perch.holder().look(level, stolasTw) == Pacts.Event.PAID && escrowed(level, pact) == 1, "Back near it: one offering for three weeks away");
		ServerLevel stolasNether = level.getServer().getLevel(Level.NETHER);
		stolasBack.teleport(new TeleportTransition(stolasNether, new Vec3(0.5, 64.0, 0.5), Vec3.ZERO, 0.0F, 0.0F, TeleportTransition.DO_NOTHING));
		helper.assertTrue(stolasBack.level() == stolasNether && stay(level, perch.holder(), stolasTw + 1_000L, stolasTw + 3 * PERIOD_7).isEmpty()
				&& escrowed(level, pact) == 1, "In another level, nothing is looked at");
		stolasBack.teleport(new TeleportTransition(level, helper.absoluteVec(new Vec3(perchAt.getX() + 0.5, perchAt.getY(), perchAt.getZ() + 2.5)), Vec3.ZERO,
				0.0F, 0.0F, TeleportTransition.DO_NOTHING));
		long stolasTn = stolasTw + 3 * PERIOD_7 + 500L;
		helper.assertTrue(perch.holder().look(level, stolasTn) == Pacts.Event.PAID && escrowed(level, pact) == 0 && BoundWills.of(level.getServer()).problems().isEmpty(),
				"Back: one offering; no debt for any of it");
		helper.succeed();
	}

	/**
	 * D-02 (E1, E4, D-S6): its holder's party pays nothing into it; its holder's party, a stand-in and a holder who is not
	 * alive cannot end it; the living holder ends it and the escrow comes back once.
	 */
	@GameTest(maxTicks = 20)
	public void onlyTheHolderEnds(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		ServerPlayer holder = binder(helper);
		ServerPlayer friend = binder(helper);
		PartyManager parties = JugcraftParties.manager();
		long ms = System.currentTimeMillis();
		try {
			helper.assertTrue(parties.invite(holder.getUUID(), friend.getUUID(), ms) == PartyManager.Result.OK
					&& parties.accept(friend.getUUID(), ms) == PartyManager.Result.OK, "The holder and a friend form a party");
			long now = level.getGameTime() + 1_000_000L;
			SpiritAnchorBlockEntity anchor = sealed(helper, holder, new BlockPos(3, 1, 3), UPKEEP, now);
			UUID record = anchor.agreement().record();
			BoundWills wills = BoundWills.of(level.getServer());
			helper.assertValueEqual(anchor.holder().deposit(level, holder, new ItemStack(Items.WOOL.white(), 2), now), 2, "Two periods paid ahead");
			helper.assertTrue(anchor.holder().deposit(level, friend, new ItemStack(Items.WOOL.white(), 1), now) == 0 && escrowed(level, record) == 2,
					"Their party pays nothing into it (the unit review's fix 5)");
			helper.assertValueEqual(anchor.holder().end(level, friend, now), AgreementHolder.Ended.NOT_HOLDER, "Their party may not end it");
			FakePlayer standIn = FakePlayer.get(level, new GameProfile(holder.getUUID(), "[Concordance]"));
			helper.assertValueEqual(anchor.holder().end(level, standIn, now), AgreementHolder.Ended.NOT_PERMITTED, "nor a stand-in with the holder's identity");
			holder.setHealth(0.0F);
			helper.assertValueEqual(anchor.holder().end(level, holder, now), AgreementHolder.Ended.NOT_PERMITTED, "nor a holder who is not alive");
			helper.assertTrue(wool(holder) == 0 && escrowed(level, record) == 2 && wills.ledger().get(record) != null,
					"the escrow is never handed to a dead holder; it stays with the record");
			holder.setHealth(holder.getMaxHealth());
			helper.assertValueEqual(anchor.holder().end(level, holder, now), AgreementHolder.Ended.ENDED, "Its living holder ends it");
			helper.assertTrue(wool(holder) == 2 && wills.ledger().get(record) == null && wills.escrow(record).isEmpty() && anchor.agreement() == null,
					"and the escrow comes back into their inventory, once");
		} finally {
			parties.adminDisband(holder.getUUID());
		}
		helper.succeed();
	}

	/**
	 * D-02 (E5, D-S10): two seals in one tick: one DONE, one TAKEN, one shade; a record kept here by a block that forgot it
	 * is left without its place, still its holder's; no place keeps two.
	 */
	@GameTest(maxTicks = 20)
	public void twoPlayersCannotBothSealOnePlace(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		ServerPlayer first = binder(helper);
		ServerPlayer second = binder(helper);
		SpiritAnchorBlockEntity anchor = anchor(helper, new BlockPos(3, 1, 3));
		long now = level.getGameTime();
		int before = level.getEntitiesOfClass(GatheringShadeEntity.class, helper.getBounds(), Entity::isAlive).size();
		helper.assertValueEqual(anchor.seal(level, first, terms(), now).outcome(), Pacts.Outcome.DONE, "The first seals");
		AgreementHolder.Sealed late = anchor.seal(level, second, terms(), now);
		helper.assertTrue(late.outcome() == Pacts.Outcome.TAKEN && late.presence() == null, "the second, in the same tick, is refused as taken and calls nothing");
		helper.assertTrue(held(level, first.getUUID()) == 1 && held(level, second.getUUID()) == 0
				&& level.getEntitiesOfClass(GatheringShadeEntity.class, helper.getBounds(), Entity::isAlive).size() == before + 1, "One Bound Will, one shade");
		UUID kept = anchor.agreement().record();
		anchor.holder().clear(); // as a block replaced without its side effects: the record is still kept at this place
		helper.assertValueEqual(anchor.seal(level, second, terms(), now + 1).outcome(), Pacts.Outcome.DONE, "A block that knows no terms may be sealed");
		BoundWill orphan = BoundWills.of(level.getServer()).ledger().get(kept);
		helper.assertTrue(orphan != null && first.getUUID().equals(orphan.holder()) && !Pacts.seated(orphan) && !Pacts.unclaimed(orphan)
				&& orphan.pact().presence() == null, "the record it replaces stays its holder's, without its place");
		helper.assertTrue(BoundWills.of(level.getServer()).problems().isEmpty(), "and no place keeps two");
		helper.succeed();
	}

	/** D-02 (E1): a stand-in binds nobody; the actor is always the holder; no transfer or update gives a record away; another's sneaking use only reads it. */
	@GameTest(maxTicks = 20)
	public void noOneBindsAnotherPlayer(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		ServerPlayer holder = binder(helper);
		ServerPlayer actor = binder(helper);
		SpiritAnchorBlockEntity anchor = anchor(helper, new BlockPos(3, 1, 3));
		FakePlayer standIn = FakePlayer.get(level, new GameProfile(holder.getUUID(), "[Concordance]"));
		long now = level.getGameTime();
		helper.assertValueEqual(anchor.seal(level, standIn, terms(), now).outcome(), Pacts.Outcome.NOT_PERMITTED, "A stand-in never seals for anyone");
		helper.assertTrue(held(level, holder.getUUID()) == 0 && anchor.agreement() == null, "and binds nobody");
		helper.assertValueEqual(anchor.seal(level, actor, terms(), now).outcome(), Pacts.Outcome.DONE, "A player seals");
		BoundWills wills = BoundWills.of(level.getServer());
		BoundWill record = wills.ledger().get(anchor.agreement().record());
		helper.assertTrue(actor.getUUID().equals(record.holder()) && held(level, holder.getUUID()) == 0 && !record.transferable(),
				"for themself alone, and the record cannot change hands");
		helper.assertValueEqual(wills.ledger().transfer(record.id(), actor.getUUID(), holder.getUUID()).outcome(), BoundWillLedger.Outcome.NOT_TRANSFERABLE,
				"no transfer moves it");
		helper.assertValueEqual(wills.update(record.withHolder(holder.getUUID())).outcome(), BoundWillLedger.Outcome.NOT_HOLDER, "and no update gives it away");
		helper.assertValueEqual(key(SpiritAnchorBlock.use(holder, level, anchor, true)), "message.jugcraft.concordance.workers.spirit",
				"Another player's sneaking use only reads it");
		helper.assertTrue(anchor.holder().toggle(level, holder, now) == AgreementHolder.Toggle.NOT_HOLDER
				&& wills.ledger().get(record.id()).pact().hold() == BoundWill.Hold.NONE, "and suspends nothing");
		helper.succeed();
	}

	/**
	 * D-02 (E2, D-S16): switched off, nothing falls due or is taken, its shade waits, nothing is suspended, ended or paid in,
	 * and breaking an anchor releases nothing (critique 5: its record, escrow and shade stay, and no departure is kept); on
	 * again, at most one period at the first look, and a record whose anchor was broken meanwhile is still its holder's to end.
	 */
	@GameTest(maxTicks = 20)
	public void theConcordanceOffHoldsEveryClock(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		ServerPlayer holder = binder(helper);
		BoundWills wills = BoundWills.of(level.getServer());
		long t0 = level.getGameTime() + 1_000_000L;
		BlockPos brokenAt = new BlockPos(6, 1, 3);
		SpiritAnchorBlockEntity other = sealed(helper, holder, brokenAt, UPKEEP, t0);
		GatheringShadeEntity otherShade = (GatheringShadeEntity) level.getEntity(other.spirit());
		UUID otherRecord = other.agreement().record();
		helper.assertValueEqual(other.holder().deposit(level, holder, new ItemStack(Items.WOOL.white(), 1), t0), 1, "One period paid ahead at a second anchor");
		BlockPos at = new BlockPos(3, 1, 3);
		SpiritAnchorBlockEntity anchor = sealed(helper, holder, at, UPKEEP, t0);
		GatheringShadeEntity shade = (GatheringShadeEntity) level.getEntity(anchor.spirit());
		UUID record = anchor.agreement().record();
		helper.assertValueEqual(anchor.holder().deposit(level, holder, new ItemStack(Items.WOOL.white(), 1), t0), 1, "One period paid ahead");
		helper.assertTrue(stay(level, anchor, t0 + 1_000L, t0 + PERIOD - 1_000L).isEmpty(), "Nothing due yet");
		BoundWill before = will(level, anchor);
		BoundWill otherBefore = wills.ledger().get(otherRecord);
		int departures = PactHistory.of(level.getServer()).history(holder.getUUID()).size();
		off(() -> {
			helper.assertTrue(stay(level, anchor, t0 + PERIOD, t0 + 3 * PERIOD).isEmpty() && will(level, anchor).equals(before)
					&& escrowed(level, record) == 1, "Switched off, nothing falls due and nothing is taken");
			helper.assertTrue(shade.think(level, NIGHT, t0 + PERIOD) == Status.DISABLED, "its shade waits, switched off");
			helper.assertValueEqual(key(SpiritAnchorBlock.use(holder, level, anchor, true)), "message.jugcraft.concordance.disabled", "its holder's word waits");
			helper.assertValueEqual(anchor.holder().end(level, holder, t0 + PERIOD), AgreementHolder.Ended.DISABLED, "nothing ends");
			helper.assertValueEqual(anchor.holder().deposit(level, holder, new ItemStack(Items.WOOL.white(), 1), t0 + PERIOD), 0, "and nothing is paid in");
			helper.assertTrue(will(level, anchor).equals(before) && escrowed(level, record) == 1, "the record and its escrow untouched");
			helper.setBlock(brokenAt, Blocks.AIR);
			helper.assertTrue(otherBefore.equals(wills.ledger().get(otherRecord)) && escrowed(level, otherRecord) == 1 && !otherShade.isRemoved()
					&& PactHistory.of(level.getServer()).history(holder.getUUID()).size() == departures,
					"Broken while switched off, an anchor releases nothing: its record, escrow and shade stay, and no departure is kept");
		});
		helper.assertTrue(wills.problems().isEmpty(), "and the records stay sound: " + wills.problems());
		long back = t0 + 3 * PERIOD + 500L;
		helper.assertValueEqual(anchor.holder().look(level, back), Pacts.Event.PAID, "On again, one period falls due at the first look");
		helper.assertTrue(escrowed(level, record) == 0 && upkeep(level, anchor).due() == back + PERIOD, "one offering only, the next a period from now");
		helper.assertTrue(AgreementHolder.end(level, null, holder, otherRecord, Pacts.ENDED, back) == AgreementHolder.Ended.ENDED && wool(holder) == 1
				&& wills.ledger().get(otherRecord) == null, "The record whose anchor was broken while switched off is still its holder's to end, escrow and all");
		// D-05 (D-S16, E2 on Stolas): switched off, nothing falls due or is taken; Stolas waits; nothing is reckoned, paid,
		// suspended, ended, seated or kept; a lost Stolas is not called; on again, one offering, and he returns at no cost.
		ServerPlayer keeper = regarded(helper);
		long s0 = dayStart(level, 400);
		PactPerchBlockEntity perch = stolasPact(helper, keeper, new BlockPos(6, 1, 6), s0);
		UUID pact = perch.holder().agreement().record();
		give(keeper, new ItemStack(Items.AMETHYST_SHARD, 2));
		helper.assertValueEqual(PactKeeping.deposit(level, keeper, perch.holder(), Items.AMETHYST_SHARD, s0), 2, "Two paid ahead");
		observed(level, keeper, FULL_MOON);
		ItemStack charged = holding(keeper, AstrolabeItem.CAPACITY);
		ObservatoryBlockEntity observatory = observatory(helper, new BlockPos(0, 1, 6));
		StolasEntity stolas = stolas(level, perch);
		BoundWill kept = pactOf(level, perch);
		off(() -> {
			helper.assertTrue(stay(level, perch.holder(), s0 + PERIOD_7, s0 + 3 * PERIOD_7).isEmpty() && pactOf(level, perch).equals(kept) && escrowed(level, pact) == 2,
					"Switched off, nothing falls due and nothing is taken");
			helper.assertValueEqual(stolas.think(level, NOON, s0 + PERIOD_7), Status.DISABLED, "Stolas waits, switched off");
			give(keeper, new ItemStack(Items.AMETHYST_SHARD, 1));
			helper.assertTrue(reckon(level, keeper, perch, FULL_MOON, Pacts.DAY + 6000L, s0 + PERIOD_7) == Pacts.Reckoning.DISABLED
					&& AstrolabeItem.charge(charged) == AstrolabeItem.CAPACITY && !recalledEver(level, keeper, FULL_MOON)
					&& PactKeeping.deposit(level, keeper, perch.holder(), Items.AMETHYST_SHARD, s0 + PERIOD_7) == 0 && shards(keeper) == 1
					&& PactKeeping.suspend(level, keeper, perch.holder(), s0 + PERIOD_7) == AgreementHolder.Toggle.DISABLED
					&& key(PactKeeping.end(level, keeper, perch.holder(), StolasEntity.COUNTERPART, s0 + PERIOD_7).said()).equals("message.jugcraft.concordance.disabled")
					&& perch.holder().seat(level, keeper, StolasEntity.COUNTERPART, s0 + PERIOD_7) == AgreementHolder.Seat.DISABLED,
					"nothing is reckoned, paid, suspended, ended or seated");
			helper.assertTrue(observatory.observe(keeper, level, NIGHT_ZERO, s0 + PERIOD_7, ONLY_MOON) == 0 && regard(keeper) == ResearchState.UNDERSTOOD,
					"no observation keeps the first obligation");
			stolas.remove(Entity.RemovalReason.KILLED);
			PactKeeping.tick(level, perch.holder(), perch.getBlockPos(), s0 + PERIOD_7);
			helper.assertTrue(stolas(level, perch) == null && pactOf(level, perch).equals(kept) && escrowed(level, pact) == 2,
					"lost while switched off, he is not called back; the record and escrow are untouched");
		});
		long stolasBack = s0 + 3 * PERIOD_7 + 500L;
		helper.assertTrue(perch.holder().look(level, stolasBack) == Pacts.Event.PAID && escrowed(level, pact) == 1, "On again, one offering at the first look");
		PactKeeping.tick(level, perch.holder(), perch.getBlockPos(), stolasBack + 20);
		helper.assertTrue(stolas(level, perch) != null && reckon(level, keeper, perch, FULL_MOON, Pacts.DAY + 6000L, stolasBack + 40) == Pacts.Reckoning.RECALLED,
				"and Stolas, called back at no cost, reckons again");
		helper.succeed();
	}

	// ---------------------------------------------------------------- constructs

	/**
	 * A porter between two chests: no route, idle; an empty source, waiting for resources; it carries and delivers,
	 * spending energy and wear; a full target, full; worn out, it needs repair and copper mends it; out of energy, a
	 * pylon by its source fuels it.
	 */
	@GameTest(maxTicks = 20)
	public void aPorterSaysWhyItStops(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		ServerPlayer keeper = binder(helper);
		BlockPos source = new BlockPos(1, 1, 2);
		BlockPos target = new BlockPos(3, 1, 2);
		helper.setBlock(source, Blocks.CHEST);
		helper.setBlock(target, Blocks.CHEST);
		WorkerDefinition.Construct terms = ClockworkPorterEntity.terms();
		ClockworkPorterEntity porter = Workers.CLOCKWORK_PORTER.create(level, EntitySpawnReason.MOB_SUMMONED);
		porter.setOwner(keeper.getUUID());
		porter.setBody(new Body(terms.integrity(), terms.energy()));
		place(helper, porter, 2.5, 1.0, 2.5);
		level.addFreshEntity(porter);
		long game = level.getGameTime() + 300_000L;
		helper.assertTrue(porter.think(level, NIGHT, game) == Status.IDLE, "No route: idle");
		porter.setRoute(new ClockworkPorterEntity.Route(helper.absolutePos(source), helper.absolutePos(target), level.dimension().identifier().toString()));
		helper.assertTrue(porter.think(level, NIGHT, game + 20) == Status.WAITING_FOR_RESOURCES, "An empty source: waiting for resources");
		ChestBlockEntity from = (ChestBlockEntity) level.getBlockEntity(helper.absolutePos(source));
		ChestBlockEntity to = (ChestBlockEntity) level.getBlockEntity(helper.absolutePos(target));
		from.setItem(0, new ItemStack(Items.COBBLESTONE, 5));
		helper.assertTrue(porter.think(level, NIGHT, game + 40) == Status.WORKING && porter.carriedCount() == 5, "It takes the load");
		helper.assertTrue(porter.think(level, NIGHT, game + 60) == Status.WORKING && to.getItem(0).getCount() == 5
				&& porter.body().energy() == terms.energy() - terms.tripEnergy() && porter.body().integrity() == terms.integrity() - terms.wear(),
				"and delivers it, spending energy and wear: " + porter.body());
		for (int slot = 0; slot < to.getContainerSize(); slot++) {
			to.setItem(slot, new ItemStack(Items.DIRT, 64));
		}
		from.setItem(0, new ItemStack(Items.COBBLESTONE, 3));
		porter.think(level, NIGHT, game + 80);
		helper.assertTrue(porter.think(level, NIGHT, game + 100) == Status.FULL && porter.carriedCount() == 3, "A full target: full, and it keeps its load");
		porter.setBody(new Body(0, terms.energy()));
		helper.assertTrue(porter.think(level, NIGHT, game + 120) == Status.NEEDS_REPAIR, "Worn out: needs repair");
		keeper.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, new ItemStack(Items.COPPER_INGOT, 2));
		keeper.interactOn(porter, net.minecraft.world.InteractionHand.MAIN_HAND, porter.position());
		helper.assertTrue(porter.body().integrity() == terms.repairAmount() && keeper.getMainHandItem().getCount() == 1, "Copper mends it");
		ClockworkPorterEntity empty = Workers.CLOCKWORK_PORTER.create(level, EntitySpawnReason.MOB_SUMMONED);
		empty.setOwner(keeper.getUUID());
		empty.setBody(new Body(terms.integrity(), 0L));
		place(helper, empty, 2.5, 1.0, 2.5);
		level.addFreshEntity(empty);
		empty.setRoute(new ClockworkPorterEntity.Route(helper.absolutePos(source), helper.absolutePos(target), level.dimension().identifier().toString()));
		helper.assertTrue(empty.think(level, NIGHT, game) == Status.NO_ENERGY, "Unfuelled, with no pylon: no energy");
		helper.setBlock(new BlockPos(1, 1, 4), JugcraftConcordance.LEY_PYLON);
		LeyPylonBlockEntity pylon = (LeyPylonBlockEntity) level.getBlockEntity(helper.absolutePos(new BlockPos(1, 1, 4)));
		pylon.setLey(level, 40);
		empty.think(level, NIGHT, game + 20);
		helper.assertTrue(empty.body().energy() == 40 && pylon.ley() == 0, "A pylon by its source fuels it: " + empty.body());
		helper.succeed();
	}

	// ---------------------------------------------------------------- saving

	/**
	 * A familiar's bond (with the play time it counted to, D-S17; one saved before reads it as unknown, and an unreadable one
	 * keeps the rest), a porter's body and route, and a shade's anchor, holder and load (G10) survive a save.
	 */
	@GameTest(maxTicks = 20)
	public void workersKeepTheirModelsThroughASave(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		HearthlingEntity familiar = Workers.HEARTHLING.create(level, EntitySpawnReason.LOAD);
		familiar.setOwner(new UUID(17L, 5L));
		familiar.setBond(new Bond(42, 3L, 7, 1000L, 2000L, 96_000L));
		TagValueOutput out = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, level.registryAccess());
		familiar.saveWithoutId(out);
		HearthlingEntity copy = Workers.HEARTHLING.create(level, EntitySpawnReason.LOAD);
		copy.load(TagValueInput.create(ProblemReporter.DISCARDING, level.registryAccess(), out.buildResult()));
		helper.assertTrue(copy.bond().equals(familiar.bond()) && familiar.owner().equals(copy.owner()), "The bond and its person are saved");
		CompoundTag bondSaved = out.buildResult();
		helper.assertTrue(bondSaved.getCompoundOrEmpty("bond").getLongOr("seen_played", Bond.UNSEEN) == 96_000L && bondSaved.getIntOr(Saved.VERSION, 0) == 1,
				"The play time it counted to is saved, still at version 1");
		Bond rest = new Bond(42, 3L, 7, 1000L, 2000L);
		CompoundTag older = bondSaved.copy();
		older.getCompoundOrEmpty("bond").remove("seen_played");
		helper.assertTrue(loadFamiliar(level, older).bond().equals(rest), "A bond saved before reads the rest unchanged, its play time not yet counted");
		CompoundTag unreadable = bondSaved.copy();
		unreadable.getCompoundOrEmpty("bond").putString("seen_played", "soon");
		helper.assertTrue(loadFamiliar(level, unreadable).bond().equals(rest), "One whose play time cannot be read keeps the rest of its bond");
		CompoundTag forged = bondSaved.copy();
		forged.getCompoundOrEmpty("bond").putLong("seen_played", -50L);
		helper.assertTrue(loadFamiliar(level, forged).bond().equals(rest), "and one below none reads as none");
		helper.assertTrue(rest.neglected(5 * Bond.DAY).equals(new Bond(42, 3L, 7, 1000L, 2000L, 5 * Bond.DAY))
				&& new Bond(42, 3L, 7, 1000L, 2000L, 96_000L).neglected(10_000L).equals(new Bond(42, 3L, 7, 1000L, 2000L, 10_000L)),
				"A first count, and a play time that went back, only start the count: nothing is owed for time it never counted");
		ClockworkPorterEntity porter = Workers.CLOCKWORK_PORTER.create(level, EntitySpawnReason.LOAD);
		porter.setBody(new Body(30, 12L));
		porter.setRoute(new ClockworkPorterEntity.Route(new BlockPos(1, 2, 3), new BlockPos(4, 5, 6), "minecraft:overworld"));
		TagValueOutput saved = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, level.registryAccess());
		porter.saveWithoutId(saved);
		ClockworkPorterEntity porterCopy = Workers.CLOCKWORK_PORTER.create(level, EntitySpawnReason.LOAD);
		porterCopy.load(TagValueInput.create(ProblemReporter.DISCARDING, level.registryAccess(), saved.buildResult()));
		helper.assertTrue(porterCopy.body().equals(porter.body()) && porterCopy.route().equals(porter.route()), "The body and route are saved");
		GatheringShadeEntity shade = Workers.GATHERING_SHADE.create(level, EntitySpawnReason.LOAD);
		shade.setOwner(new UUID(17L, 6L));
		shade.setAnchor(new BlockPos(7, 8, 9));
		TagValueOutput shadeOut = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, level.registryAccess());
		shade.saveWithoutId(shadeOut);
		CompoundTag laden = shadeOut.buildResult();
		laden.put("carried", ItemStack.CODEC.listOf().encodeStart(level.registryAccess().createSerializationContext(NbtOps.INSTANCE),
				List.of(new ItemStack(Items.WHEAT, 5))).getOrThrow());
		GatheringShadeEntity shadeCopy = Workers.GATHERING_SHADE.create(level, EntitySpawnReason.LOAD);
		shadeCopy.load(TagValueInput.create(ProblemReporter.DISCARDING, level.registryAccess(), laden));
		TagValueOutput again = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, level.registryAccess());
		shadeCopy.saveWithoutId(again);
		GatheringShadeEntity shadeBack = Workers.GATHERING_SHADE.create(level, EntitySpawnReason.LOAD);
		shadeBack.load(TagValueInput.create(ProblemReporter.DISCARDING, level.registryAccess(), again.buildResult()));
		helper.assertTrue(new BlockPos(7, 8, 9).equals(shadeBack.anchor()) && new UUID(17L, 6L).equals(shadeBack.owner())
				&& shadeBack.carriedCount() == 5, "A shade's anchor, holder and load are saved: " + shadeBack.carriedCount());
		helper.succeed();
	}

	// ---------------------------------------------------------------- what it shows (roadmap step 27)

	/**
	 * A porter shows a shortage over itself when it comes to lack something (an empty source), once: wavering between
	 * working and wanting within {@code WorkerEntity.SIGN_TICKS} shows no second sign, and a working porter shows none.
	 * Its body never claims progress it is not making: on its way but not moving, it is shown standing still.
	 */
	@GameTest(maxTicks = 20)
	public void aPorterShowsOnceWhatItLacks(GameTestHelper helper) {
		SignWatch signs = new SignWatch(helper);
		ServerLevel level = helper.getLevel();
		ServerPlayer keeper = binder(helper);
		BlockPos source = new BlockPos(1, 1, 2);
		BlockPos target = new BlockPos(3, 1, 2);
		helper.setBlock(source, Blocks.CHEST);
		helper.setBlock(target, Blocks.CHEST);
		WorkerDefinition.Construct terms = ClockworkPorterEntity.terms();
		ClockworkPorterEntity porter = Workers.CLOCKWORK_PORTER.create(level, EntitySpawnReason.MOB_SUMMONED);
		porter.setOwner(keeper.getUUID());
		porter.setBody(new Body(terms.integrity(), terms.energy()));
		place(helper, porter, 2.5, 1.0, 2.5);
		level.addFreshEntity(porter);
		long game = level.getGameTime() + 300_000L;
		porter.setRoute(new ClockworkPorterEntity.Route(helper.absolutePos(source), helper.absolutePos(target), level.dimension().identifier().toString()));
		helper.assertTrue(porter.think(level, NIGHT, game) == Status.WAITING_FOR_RESOURCES, "An empty source: waiting for resources");
		helper.assertValueEqual((long) signs.of(Sign.WANT).size(), 1L, "the lack shows over the porter");
		porter.think(level, NIGHT, game + 20);
		helper.assertValueEqual((long) signs.of(Sign.WANT).size(), 1L, "still waiting: no second sign");
		ChestBlockEntity from = (ChestBlockEntity) level.getBlockEntity(helper.absolutePos(source));
		from.setItem(0, new ItemStack(Items.COBBLESTONE, 5));
		helper.assertTrue(porter.think(level, NIGHT, game + 40) == Status.WORKING, "It takes the load");
		from.setItem(0, ItemStack.EMPTY);
		porter.think(level, NIGHT, game + 60);
		porter.think(level, NIGHT, game + 80);
		helper.assertTrue(signs.of(Sign.WANT).size() == 1 && signs.of(Sign.DONE).isEmpty(),
				"Wavering between working and wanting within the sign's interval shows no more: " + signs.all());
		helper.assertTrue(WorkerEntity.shown(Status.TRAVELLING, false) == Status.IDLE && WorkerEntity.shown(Status.TRAVELLING, true) == Status.TRAVELLING
				&& WorkerEntity.shown(Status.FOLLOWING, false) == Status.IDLE && WorkerEntity.shown(Status.WORKING, false) == Status.WORKING,
				"On its way but not moving, a worker is shown still; working, it is shown working");
		signs.close();
		helper.succeed();
	}

	// ---------------------------------------------------------------- the workshop hand (Concordance Chunk D, D-08)

	/**
	 * D-08 (D-S2, D-S23): a workshop hand keeps to the terms sealed at its anchor (narrow test terms: 3 blocks, its hours, 2
	 * loads a day). Outside its hours it takes nothing and says OUTSIDE_AGREEMENT, though a claim it holds is carried to its
	 * end; a request for a crucible outside its area is refused with the same reason and left open while one inside is taken
	 * past it; its day's loads done it says FINISHED; the next day it serves again. Suspended by its holder (critique 4), it
	 * finishes the load it has claimed but claims nothing new, saying SUSPENDED (held is asked before its day's loads);
	 * resumed, it serves again. Nothing moves twice.
	 */
	@GameTest(maxTicks = 20)
	public void aRequestOutsideTheHoursOrAreaIsRefusedWithItsReason(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		ServerPlayer holder = WorkshopHands.holder(helper);
		CourierPostBlockEntity post = WorkshopHands.post(helper, holder, WorkshopHands.POST);
		ChestBlockEntity chest = WorkshopHands.chest(helper, WorkshopHands.CHEST, 10);
		long now = WorkshopHands.morning(level, 3_200_000L);
		WorkerDefinition.Spirit terms = WorkshopHands.terms();
		WorkshopHandEntity hand = WorkshopHands.handUnder(helper, holder,
				new WorkerDefinition.Spirit(WorkshopHandEntity.DEFINITION, terms.work(), 3, terms.from(), terms.to(), 2, terms.carry()), now);
		SpiritAnchorBlockEntity anchor = helper.getBlockEntity(WorkshopHands.ANCHOR, SpiritAnchorBlockEntity.class);
		CourierLedger ledger = CourierLedger.of(level.getServer());
		CrucibleBlockEntity inside = WorkshopHands.crucible(helper, WorkshopHands.CRUCIBLE, holder); // 3 blocks from the anchor
		long first = inside.request();
		List<Long> ids = new ArrayList<>(List.of(first));
		long in = WorkshopHands.inHours();
		long out = WorkshopHands.outOfHours();
		helper.assertTrue(hand.think(level, out, now) == Status.OUTSIDE_AGREEMENT && hand.status() == Status.OUTSIDE_AGREEMENT && hand.task() == 0L
				&& ledger.ledger().get(first).state() == RequestState.OPEN, "Outside its hours it takes nothing and says why");
		WorkshopHands.conserved(helper, ids, WorkshopHands.PLAIN, 10, 0, "outside its hours", chest, inside, post);
		helper.assertTrue(hand.think(level, in, now + 20) == Status.TRAVELLING && hand.task() == first, "In its hours it takes the crucible's request");
		WorkshopHands.conserved(helper, ids, WorkshopHands.PLAIN, 10, 0, "claimed", chest, inside, post);
		helper.assertTrue(hand.think(level, out, now + 40) == Status.WORKING && ledger.ledger().get(first).progress().carried() == 2,
				"A claim it holds is carried on past its hours: it picks up");
		WorkshopHands.conserved(helper, ids, WorkshopHands.PLAIN, 10, 0, "picked up past its hours", chest, inside, post);
		helper.assertTrue(hand.think(level, out, now + 60) == Status.WORKING && ledger.ledger().get(first) == null
				&& WorkshopHands.matching(inside, WorkshopHands.PLAIN) == 2 && anchor.agreement().done(now + 60) == 1, "and delivers: one of its two loads today");
		WorkshopHands.conserved(helper, ids, WorkshopHands.PLAIN, 10, 0, "the first delivered", chest, inside, post);
		CrucibleBlockEntity outside = WorkshopHands.crucible(helper, new BlockPos(1, 1, 1), holder); // 4 blocks from the anchor
		long far = outside.request();
		ids.add(far);
		helper.assertTrue(hand.think(level, in, now + 80) == Status.OUTSIDE_AGREEMENT && hand.task() == 0L && ledger.ledger().get(far).state() == RequestState.OPEN,
				"A request for a crucible outside its area is refused with the same reason and left open for another courier");
		WorkshopHands.conserved(helper, ids, WorkshopHands.PLAIN, 10, 0, "outside its area", chest, inside, outside, post);
		Couriers.request(holder, level, post, new ItemStack(Items.GLOWSTONE_DUST), 2);
		long near = WorkshopHands.lastFiled(helper, WorkshopHands.POST);
		ids.add(near);
		helper.assertTrue(hand.think(level, in, now + 100) == Status.TRAVELLING && hand.task() == near && ledger.ledger().get(far).progress().worker() == null,
				"One inside its area is taken past it");
		WorkshopHands.conserved(helper, ids, WorkshopHands.PLAIN, 10, 0, "the second claimed", chest, inside, outside, post);
		helper.assertValueEqual(hand.think(level, in, now + 120), Status.WORKING, "it picks up");
		WorkshopHands.conserved(helper, ids, WorkshopHands.PLAIN, 10, 0, "the second picked up", chest, inside, outside, post);
		helper.assertTrue(hand.think(level, in, now + 140) == Status.WORKING && ledger.ledger().get(near) == null
				&& WorkshopHands.matching(post, WorkshopHands.PLAIN) == 2 && anchor.agreement().done(now + 140) == 2, "and delivered: its two loads for the day");
		WorkshopHands.conserved(helper, ids, WorkshopHands.PLAIN, 10, 0, "the second delivered", chest, inside, outside, post);
		Couriers.request(holder, level, post, new ItemStack(Items.GLOWSTONE_DUST), 2);
		long late = WorkshopHands.lastFiled(helper, WorkshopHands.POST);
		ids.add(late);
		helper.assertTrue(hand.think(level, in, now + 160) == Status.FINISHED && hand.task() == 0L && ledger.ledger().get(late).state() == RequestState.OPEN,
				"Its loads done, it says FINISHED and takes nothing more today");
		WorkshopHands.conserved(helper, ids, WorkshopHands.PLAIN, 10, 0, "finished", chest, inside, outside, post);
		long tomorrow = now + Agreement.DAY;
		helper.assertTrue(hand.think(level, in, tomorrow) == Status.TRAVELLING && hand.task() == late, "The next day it serves again");
		WorkshopHands.conserved(helper, ids, WorkshopHands.PLAIN, 10, 0, "the next day claimed", chest, inside, outside, post);
		helper.assertValueEqual(hand.think(level, in, tomorrow + 20), Status.WORKING, "it picks up");
		WorkshopHands.conserved(helper, ids, WorkshopHands.PLAIN, 10, 0, "the next day picked up", chest, inside, outside, post);
		helper.assertTrue(hand.think(level, in, tomorrow + 40) == Status.WORKING && ledger.ledger().get(late) == null
				&& WorkshopHands.matching(post, WorkshopHands.PLAIN) == 4, "and delivers");
		helper.assertTrue(ledger.ledger().get(far).state() == RequestState.OPEN && ledger.ledger().get(far).progress().worker() == null,
				"It never took the request outside its area");
		WorkshopHands.conserved(helper, ids, WorkshopHands.PLAIN, 10, 0, "the next day", chest, inside, outside, post);
		// Critique 4 (D-P75): a hold stops new claims only; the load it has claimed it finishes.
		Couriers.request(holder, level, post, new ItemStack(Items.GLOWSTONE_DUST), 2);
		long kept = WorkshopHands.lastFiled(helper, WorkshopHands.POST);
		ids.add(kept);
		helper.assertTrue(hand.think(level, in, tomorrow + 60) == Status.TRAVELLING && hand.task() == kept, "Its second load today is claimed");
		WorkshopHands.conserved(helper, ids, WorkshopHands.PLAIN, 10, 0, "claimed before the hold", chest, inside, outside, post);
		helper.assertValueEqual(anchor.holder().toggle(level, holder, tomorrow + 70), AgreementHolder.Toggle.SUSPENDED, "Its holder suspends it");
		helper.assertTrue(hand.think(level, in, tomorrow + 80) == Status.WORKING && ledger.ledger().get(kept).progress().carried() == 2,
				"Suspended, it still picks up the load it had claimed");
		WorkshopHands.conserved(helper, ids, WorkshopHands.PLAIN, 10, 0, "picked up while suspended", chest, inside, outside, post);
		helper.assertTrue(hand.think(level, in, tomorrow + 100) == Status.WORKING && ledger.ledger().get(kept) == null
				&& WorkshopHands.matching(post, WorkshopHands.PLAIN) == 6, "and delivers it");
		WorkshopHands.conserved(helper, ids, WorkshopHands.PLAIN, 10, 0, "delivered while suspended", chest, inside, outside, post);
		Couriers.request(holder, level, post, new ItemStack(Items.GLOWSTONE_DUST), 2);
		long waiting = WorkshopHands.lastFiled(helper, WorkshopHands.POST);
		ids.add(waiting);
		helper.assertTrue(hand.think(level, in, tomorrow + 120) == Status.SUSPENDED && hand.task() == 0L
				&& ledger.ledger().get(waiting).state() == RequestState.OPEN && ledger.ledger().get(waiting).progress().worker() == null,
				"but claims nothing new, and says it is suspended (before its day's loads, now done)");
		WorkshopHands.conserved(helper, ids, WorkshopHands.PLAIN, 10, 0, "suspended", chest, inside, outside, post);
		helper.assertValueEqual(anchor.holder().toggle(level, holder, tomorrow + 130), AgreementHolder.Toggle.RESUMED, "Its holder resumes it");
		helper.assertTrue(hand.think(level, in, tomorrow + Agreement.DAY) == Status.TRAVELLING && hand.task() == waiting
				&& ledger.ledger().get(waiting).progress().reserved() == 2, "Resumed, it claims the waiting request on its next day");
		WorkshopHands.conserved(helper, ids, WorkshopHands.PLAIN, 10, 0, "resumed", chest, inside, outside, post);
		helper.succeed();
	}

	/**
	 * D-08 (D-S12, D-S2): binding asks no mastery, but a workshop hand is sealed only once Stolas's Regard is mastered: never
	 * met or understood, the anchor names the reason and records, calls and notes nothing. Mastered, its own agreement is on
	 * its Bound Will (its terms, for the holder and their party) and it is on the roster at once. An anchor bound to no post
	 * still seals a shade. Mastery is asked at sealing only: the sealed hand serves on.
	 */
	@GameTest(maxTicks = 20)
	public void theHandNeedsStolasRegardMastered(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		ServerPlayer holder = WorkshopHands.taught(TestPlayers.survivor(helper), null);
		CourierPostBlockEntity post = WorkshopHands.post(helper, holder, WorkshopHands.POST);
		ChestBlockEntity chest = WorkshopHands.chest(helper, WorkshopHands.CHEST, 10);
		Couriers.request(holder, level, post, new ItemStack(Items.GLOWSTONE_DUST), 2);
		long id = WorkshopHands.lastFiled(helper, WorkshopHands.POST);
		List<Long> ids = List.of(id);
		SpiritAnchorBlockEntity anchor = WorkshopHands.anchor(helper, holder, WorkshopHands.ANCHOR);
		helper.assertTrue(WorkshopHands.use(helper, holder, WorkshopHands.keyFor(helper, holder, WorkshopHands.POST), WorkshopHands.ANCHOR) == InteractionResult.SUCCESS
				&& anchor.post() != null, "Binding an anchor to one's own post asks no mastery" + WorkshopHands.why(helper, holder, WorkshopHands.ANCHOR, WorkshopHands.POST));
		for (ResearchState regard : new ResearchState[] {ResearchState.NONE, ResearchState.UNDERSTOOD}) {
			if (regard != ResearchState.NONE) {
				ConcordanceProgress.grant(holder, WorkshopHandEntity.REGARD, regard);
			}
			helper.assertValueEqual(WorkshopHands.key(WorkshopHands.seal(helper, holder, anchor)), WorkshopHands.UNKNOWN,
					"Stolas's Regard " + regard.id() + ": no hand, and the reason is named");
			helper.assertTrue(anchor.agreement() == null && anchor.spirit() == null && held(level, holder.getUUID()) == 0
					&& level.getEntitiesOfClass(WorkshopHandEntity.class, helper.getBounds(), Entity::isAlive).isEmpty()
					&& WorkerRoster.of(level.getServer()).workers(holder.getUUID()).isEmpty(), "Nothing is recorded, called or noted");
			WorkshopHands.conserved(helper, ids, WorkshopHands.PLAIN, 10, 0, "refused at " + regard.id(), chest, post);
		}
		ConcordanceProgress.grant(holder, WorkshopHandEntity.REGARD, ResearchState.MASTERED);
		helper.assertValueEqual(WorkshopHands.key(WorkshopHands.seal(helper, holder, anchor)), WorkshopHands.SEALED, "Mastered, its holder seals a workshop hand");
		WorkshopHandEntity hand = level.getEntity(anchor.spirit()) instanceof WorkshopHandEntity found ? found : null;
		BoundWill will = BoundWills.of(level.getServer()).ledger().get(anchor.agreement().record());
		WorkerDefinition.Spirit terms = WorkshopHands.terms();
		helper.assertTrue(hand != null && holder.getUUID().equals(hand.owner()) && helper.absolutePos(WorkshopHands.ANCHOR).equals(hand.seat())
				&& WorkshopHandEntity.DEFINITION.equals(will.agreement()) && WorkshopHandEntity.DEFINITION.equals(will.counterpart()) && !will.transferable()
				&& Couriers.place(level, anchor.getBlockPos()).equals(will.pact().place()) && Pacts.PARTY.equals(will.pact().beneficiaries())
				&& will.pact().upkeep() == null && new BoundWill.Service("courier", terms.radius(), terms.from(), terms.to(), terms.quota()).equals(will.pact().service()),
				"Its own agreement on its Bound Will: the hand's terms, for its holder and their party: " + will);
		WorkerRoster.Entry noted = WorkerRoster.of(level.getServer()).workers(holder.getUUID()).get(hand.getUUID());
		helper.assertTrue(noted != null && "spirit".equals(noted.kind()), "On its holder's roster at once, as a spirit");
		SpiritAnchorBlockEntity plain = WorkshopHands.anchor(helper, holder, new BlockPos(4, 1, 5));
		helper.assertValueEqual(WorkshopHands.key(WorkshopHands.seal(helper, holder, plain)), "message.jugcraft.concordance.workers.sealed",
				"An anchor bound to no post still seals a Gathering Shade");
		helper.assertTrue(level.getEntity(plain.spirit()) instanceof GatheringShadeEntity, "a shade, not a hand");
		ConcordanceProgress.grant(holder, WorkshopHandEntity.REGARD, ResearchState.UNDERSTOOD);
		WorkshopHands.stand(helper, hand, 2.5, 1.0, 3.5);
		long now = WorkshopHands.morning(level, 3_300_000L);
		long in = WorkshopHands.inHours();
		helper.assertTrue(hand.think(level, in, now) == Status.TRAVELLING && hand.task() == id, "Mastery is asked at sealing only: the sealed hand serves on");
		WorkshopHands.conserved(helper, ids, WorkshopHands.PLAIN, 10, 0, "claimed", chest, post);
		helper.assertValueEqual(hand.think(level, in, now + 20), Status.WORKING, "it picks up");
		WorkshopHands.conserved(helper, ids, WorkshopHands.PLAIN, 10, 0, "picked up", chest, post);
		helper.assertTrue(hand.think(level, in, now + 40) == Status.WORKING && CourierLedger.of(level.getServer()).ledger().get(id) == null
				&& WorkshopHands.matching(post, WorkshopHands.PLAIN) == 2, "and delivers");
		WorkshopHands.conserved(helper, ids, WorkshopHands.PLAIN, 10, 0, "delivered", chest, post);
		BlockPos thirdAt = new BlockPos(6, 1, 5);
		SpiritAnchorBlockEntity third = WorkshopHands.anchor(helper, holder, thirdAt);
		helper.assertTrue(WorkshopHands.use(helper, holder, WorkshopHands.keyFor(helper, holder, WorkshopHands.POST), thirdAt) == InteractionResult.SUCCESS
				&& WorkshopHands.UNKNOWN.equals(WorkshopHands.key(WorkshopHands.seal(helper, holder, third))) && third.agreement() == null,
				"but no new hand is sealed without mastery");
		helper.succeed();
	}

	/**
	 * D-08 (D-S16): switched off, a workshop hand carrying for a crucible waits, DISABLED, its claim, cargo and lease
	 * untouched; no anchor is bound, nothing sealed, no agreement ended; on again within its lease, it delivers once.
	 */
	@GameTest(maxTicks = 20)
	public void theConcordanceOffHoldsTheHand(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		WorkshopHands.Workshop shop = WorkshopHands.workshop(helper);
		WorkshopHandEntity hand = shop.hand();
		CrucibleBlockEntity crucible = shop.crucible();
		ServerPlayer holder = shop.holder();
		long id = shop.request();
		List<Long> ids = List.of(id);
		CourierLedger ledger = CourierLedger.of(level.getServer());
		BlockPos spareAt = new BlockPos(4, 1, 5);
		SpiritAnchorBlockEntity spare = WorkshopHands.anchor(helper, holder, spareAt);
		ItemStack portersKey = WorkshopHands.keyFor(helper, holder, WorkshopHands.POST);
		long now = WorkshopHands.morning(level, 3_400_000L);
		long in = WorkshopHands.inHours();
		helper.assertValueEqual(hand.think(level, in, now), Status.TRAVELLING, "The hand claims the crucible's request");
		WorkshopHands.conserved(helper, ids, WorkshopHands.PLAIN, 10, 0, "claimed", shop.chest(), crucible);
		hand.think(level, in, now + 20);
		var carrying = ledger.ledger().get(id);
		helper.assertTrue(carrying.state() == RequestState.IN_TRANSIT && carrying.progress().carried() == 2, "The hand carries two for the crucible");
		WorkshopHands.conserved(helper, ids, WorkshopHands.PLAIN, 10, 0, "carried", shop.chest(), crucible);
		BoundWill will = BoundWills.of(level.getServer()).ledger().get(shop.anchor().agreement().record());
		off(() -> {
			helper.assertTrue(hand.think(level, in, now + 40) == Status.DISABLED && hand.task() == id
					&& ledger.ledger().get(id).progress().equals(carrying.progress()), "Switched off, it waits: its claim, cargo and lease untouched");
			WorkshopHands.conserved(helper, ids, WorkshopHands.PLAIN, 10, 0, "switched off", shop.chest(), crucible);
			helper.assertTrue(WorkshopHands.use(helper, holder, portersKey, spareAt) == InteractionResult.FAIL && spare.post() == null, "No anchor is bound to a post");
			helper.assertValueEqual(key(SpiritAnchorBlock.use(holder, level, spare, false)), "message.jugcraft.concordance.disabled", "nothing is sealed");
			helper.assertValueEqual(shop.anchor().holder().end(level, holder, now + 40), AgreementHolder.Ended.DISABLED, "and no agreement ends");
			helper.assertTrue(will.equals(BoundWills.of(level.getServer()).ledger().get(will.id())) && !hand.isRemoved() && spare.agreement() == null,
					"Its Bound Will and its hand untouched");
		});
		helper.assertTrue(hand.think(level, in, now + 60) == Status.WORKING && ledger.ledger().get(id) == null
				&& WorkshopHands.matching(crucible, WorkshopHands.PLAIN) == 2, "On again within its lease, it delivers once");
		WorkshopHands.conserved(helper, ids, WorkshopHands.PLAIN, 10, 0, "delivered", shop.chest(), crucible);
		helper.succeed();
	}

	/**
	 * D-08 (D-P71): a workshop hand's delivery is a spirit's service, so the Binding Arts' "each kind once" is unchanged: a
	 * familiar's and the hand's are two of the three, and exactly those two are recorded; a spirit's again adds nothing; a
	 * construct's masters them.
	 */
	@GameTest(maxTicks = 20)
	public void aHandsServiceIsASpiritsService(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		WorkshopHands.Workshop shop = WorkshopHands.workshop(helper);
		ServerPlayer holder = shop.holder();
		WorkshopHandEntity hand = shop.hand();
		ConcordanceProgress.record(holder, new Evidence.Practiced(Workers.ACTIVITY, "familiar"));
		List<Long> ids = List.of(shop.request());
		long now = WorkshopHands.morning(level, 3_500_000L);
		long in = WorkshopHands.inHours();
		helper.assertValueEqual(hand.think(level, in, now), Status.TRAVELLING, "The hand claims the crucible's request");
		WorkshopHands.conserved(helper, ids, WorkshopHands.PLAIN, 10, 0, "claimed", shop.chest(), shop.crucible());
		helper.assertValueEqual(hand.think(level, in, now + 20), Status.WORKING, "it picks up");
		WorkshopHands.conserved(helper, ids, WorkshopHands.PLAIN, 10, 0, "picked up", shop.chest(), shop.crucible());
		helper.assertTrue(hand.think(level, in, now + 40) == Status.WORKING && CourierLedger.of(level.getServer()).ledger().get(shop.request()) == null
				&& shop.anchor().agreement().done(now + 40) == 1, "The hand delivers the crucible's two: one load");
		WorkshopHands.conserved(helper, ids, WorkshopHands.PLAIN, 10, 0, "delivered", shop.chest(), shop.crucible());
		helper.assertValueEqual(ConcordanceProgress.knowledge(holder).progress(Workers.RESEARCH).evidence().keySet(),
				Set.of("practice:" + Workers.ACTIVITY + ":familiar", "practice:" + Workers.ACTIVITY + ":spirit"),
				"the hand's delivery recorded exactly a spirit's service");
		helper.assertValueEqual(ConcordanceProgress.knowledge(holder).state(Workers.RESEARCH), ResearchState.UNDERSTOOD,
				"A familiar's and the hand's service: two of the three kinds");
		helper.assertTrue(!ConcordanceProgress.record(holder, new Evidence.Practiced(Workers.ACTIVITY, "spirit")).recorded(),
				"A spirit's again adds nothing: the hand's was a spirit's");
		helper.assertValueEqual(ConcordanceProgress.knowledge(holder).state(Workers.RESEARCH), ResearchState.UNDERSTOOD,
				"and the Binding Arts stay understood");
		ConcordanceProgress.record(holder, new Evidence.Practiced(Workers.ACTIVITY, "construct"));
		helper.assertValueEqual(ConcordanceProgress.knowledge(holder).state(Workers.RESEARCH), ResearchState.MASTERED,
				"A construct's makes the three: the Binding Arts are mastered, as before the hand");
		helper.succeed();
	}

	/**
	 * D-08 (D-P73): an anchor is bound only while it keeps no agreement, only with the Concordance on, only by a key that names
	 * a post in its level inside a hand's area; sneaking unbinds; sealed, it is neither unbound nor bound anew; a copied anchor
	 * binds nothing (CONFLICTING_AGREEMENT).
	 */
	@GameTest(maxTicks = 20)
	public void anAnchorIsBoundOnlyWhileItKeepsNoAgreement(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		ServerPlayer owner = WorkshopHands.holder(helper);
		WorkshopHands.post(helper, owner, WorkshopHands.POST);
		SpiritAnchorBlockEntity anchor = WorkshopHands.anchor(helper, owner, WorkshopHands.ANCHOR);
		ItemStack mine = WorkshopHands.keyFor(helper, owner, WorkshopHands.POST);
		GlobalPos bound = GlobalPos.of(level.dimension(), helper.absolutePos(WorkshopHands.POST));
		PorterKeyItem.Binding none = PorterKeyItem.bind(owner, level, anchor, new ItemStack(Workers.PORTER_KEY), false);
		helper.assertTrue(!none.changed() && key(none.told()).equals("message.jugcraft.concordance.workers.key_no_post") && anchor.post() == null,
				"A key that names no post binds nothing, and says how");
		ItemStack nether = new ItemStack(Workers.PORTER_KEY);
		nether.set(Couriers.KEY_POST, GlobalPos.of(Level.NETHER, helper.absolutePos(WorkshopHands.POST)));
		PorterKeyItem.Binding elsewhere = PorterKeyItem.bind(owner, level, anchor, nether, false);
		helper.assertTrue(!elsewhere.changed() && key(elsewhere.told()).equals("message.jugcraft.concordance.workers.post_unusable") && anchor.post() == null
				&& WorkshopHandEntity.within(anchor.getBlockPos(), anchor.getBlockPos().offset(12, -12, 12), 12)
				&& !WorkshopHandEntity.within(anchor.getBlockPos(), anchor.getBlockPos().offset(13, 0, 0), 12),
				"A post in another dimension, or past a hand's 12 blocks, binds nothing");
		// Critique 6: a hand's 12 blocks through bind itself, with posts of the owner's own straight above the anchor (the space
		// over this test's structure is no other test's), removed again.
		BlockPos tooFar = new BlockPos(2, 14, 5);
		BlockPos atTheEdge = new BlockPos(2, 13, 5);
		WorkshopHands.post(helper, owner, tooFar);
		WorkshopHands.post(helper, owner, atTheEdge);
		PorterKeyItem.Binding past = PorterKeyItem.bind(owner, level, anchor, WorkshopHands.keyFor(helper, owner, tooFar), false);
		helper.assertTrue(!past.changed() && key(past.told()).equals("message.jugcraft.concordance.workers.post_unusable") && anchor.post() == null,
				"Their own post 13 blocks above the anchor, past a hand's 12, binds nothing");
		PorterKeyItem.Binding edge = PorterKeyItem.bind(owner, level, anchor, WorkshopHands.keyFor(helper, owner, atTheEdge), false);
		helper.assertTrue(edge.changed() && key(edge.told()).equals("message.jugcraft.concordance.workers.post_bound")
				&& GlobalPos.of(level.dimension(), helper.absolutePos(atTheEdge)).equals(anchor.post()), "One 12 blocks above, at its edge, binds it");
		helper.assertTrue(PorterKeyItem.bind(owner, level, anchor, mine, true).changed() && anchor.post() == null, "and sneaking unbinds it again");
		helper.setBlock(tooFar, Blocks.AIR);
		helper.setBlock(atTheEdge, Blocks.AIR);
		off(() -> helper.assertTrue(key(PorterKeyItem.bind(owner, level, anchor, mine, false).told()).equals("message.jugcraft.concordance.disabled")
				&& anchor.post() == null, "Switched off, nothing is bound"));
		PorterKeyItem.Binding first = PorterKeyItem.bind(owner, level, anchor, mine, false);
		helper.assertTrue(first.changed() && key(first.told()).equals("message.jugcraft.concordance.workers.post_bound") && bound.equals(anchor.post()),
				"Its owner binds it to their post");
		PorterKeyItem.Binding unbound = PorterKeyItem.bind(owner, level, anchor, mine, true);
		helper.assertTrue(unbound.changed() && key(unbound.told()).equals("message.jugcraft.concordance.workers.post_unbound") && anchor.post() == null,
				"sneaking unbinds it");
		helper.assertTrue(PorterKeyItem.bind(owner, level, anchor, mine, false).changed() && bound.equals(anchor.post()), "and it binds again");
		helper.assertValueEqual(WorkshopHands.key(WorkshopHands.seal(helper, owner, anchor)), WorkshopHands.SEALED, "A hand is sealed there");
		PorterKeyItem.Binding sealedUnbind = PorterKeyItem.bind(owner, level, anchor, mine, true);
		PorterKeyItem.Binding sealedRebind = PorterKeyItem.bind(owner, level, anchor, mine, false);
		helper.assertTrue(!sealedUnbind.changed() && !sealedRebind.changed() && key(sealedUnbind.told()).equals("message.jugcraft.concordance.workers.post_sealed")
				&& key(sealedRebind.told()).equals("message.jugcraft.concordance.workers.post_sealed") && bound.equals(anchor.post()),
				"Sealed, it is neither unbound nor bound anew");
		helper.setBlock(new BlockPos(5, 1, 5), Workers.SPIRIT_ANCHOR);
		SpiritAnchorBlockEntity copy = helper.getBlockEntity(new BlockPos(5, 1, 5), SpiritAnchorBlockEntity.class);
		copy.loadWithComponents(TagValueInput.create(ProblemReporter.DISCARDING, level.registryAccess(), anchor.saveWithoutMetadata(level.registryAccess())));
		PorterKeyItem.Binding copied = PorterKeyItem.bind(owner, level, copy, mine, true);
		helper.assertTrue(!copied.changed() && key(copied.told()).equals("compose.jugcraft.diagnostic.conflicting_agreement") && bound.equals(copy.post()),
				"A copied anchor binds and unbinds nothing: another agreement covers it");
		helper.succeed();
	}

	// ---------------------------------------------------------------- Concordance Chunk D, unit 2: the pact hall
	// Package-private: the Sky, Sharing, Persistence and Authority suites use them; no new helper file, since a
	// shared helper file would select every client test class (tools/select_client_tests.py).

	static final String PACT_KEY = "message.jugcraft.concordance.pact.";
	static final String FULL_MOON = "jugcraft:full_moon";
	static final String HARVEST_MOON = "jugcraft:harvest_moon";
	static final String LANTERN_STAR = "jugcraft:lantern_star";
	static final String WINTER_CROWN = "jugcraft:winter_crown";
	static final String ECHO_COMET = "jugcraft:echo_comet";
	/** Midnight of world day 0 (the full moon is up), and a sky in which only the full moon is seen (as the Sky tests'). */
	static final long NIGHT_ZERO = 18000L;
	static final Function<Pattern, @Nullable String> ONLY_MOON = pattern -> pattern.id().equals(FULL_MOON) ? null : "no_sky";

	static PactDefinition stolasOffer() {
		PactDefinition stolas = PactPerchBlockEntity.definition();
		if (stolas == null) {
			throw new IllegalStateException("Stolas's offer did not load");
		}
		return stolas;
	}

	/** The Binding Arts and Stolas's Regard understood, the Lantern Star observed, no action rate-limited. */
	static ServerPlayer ready(ServerLevel level, ServerPlayer player) {
		ConcordanceProgress.grant(player, "jugcraft:first_light", ResearchState.UNDERSTOOD);
		ConcordanceProgress.grant(player, Workers.RESEARCH, ResearchState.UNDERSTOOD);
		ConcordanceProgress.grant(player, StolasRegard.RESEARCH, ResearchState.UNDERSTOOD);
		AstralClaims.of(level.getServer()).observe(player.getUUID(), LANTERN_STAR, level.getGameTime());
		RateGate.forget(player.getUUID());
		return player;
	}

	/** An ordinary survival player who understands Stolas's Regard (and the Binding Arts it needs), nothing rate-limited. */
	static ServerPlayer regarded(GameTestHelper helper) {
		ServerPlayer player = TestPlayers.survivor(helper);
		ConcordanceProgress.grant(player, "jugcraft:first_light", ResearchState.UNDERSTOOD);
		ConcordanceProgress.grant(player, Workers.RESEARCH, ResearchState.UNDERSTOOD);
		ConcordanceProgress.grant(player, StolasRegard.RESEARCH, ResearchState.UNDERSTOOD);
		RateGate.forget(player.getUUID());
		return player;
	}

	/** {@code times} compacts' materials, and an astrolabe holding {@code resonance}. */
	static void pocket(ServerPlayer player, int times, int resonance) {
		for (PactDefinition.Cost cost : stolasOffer().compact()) {
			player.getInventory().add(new ItemStack(BuiltInRegistries.ITEM.getValue(Identifier.parse(cost.item())), cost.count() * times));
		}
		ItemStack astrolabe = new ItemStack(Sky.ASTROLABE);
		AstrolabeItem.setCharge(astrolabe, resonance);
		player.getInventory().add(astrolabe);
	}

	/** Whether {@code player} carries exactly {@code times} compacts' materials. */
	static boolean pocketed(ServerPlayer player, int times) {
		return pocketed(player, times, stolasOffer());
	}

	/** Whether {@code player} carries exactly {@code times} of {@code offer}'s compacts' materials (an offer read before the rules change). */
	static boolean pocketed(ServerPlayer player, int times, PactDefinition offer) {
		for (PactDefinition.Cost cost : offer.compact()) {
			if (count(player, cost.item()) != cost.count() * times) {
				return false;
			}
		}
		return true;
	}

	static int count(ServerPlayer player, String item) {
		return player.getInventory().countItem(BuiltInRegistries.ITEM.getValue(Identifier.parse(item)));
	}

	static int resonance(ServerPlayer player) {
		Inventory inventory = player.getInventory();
		int held = 0;
		for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
			if (inventory.getItem(slot).is(Sky.ASTROLABE)) {
				held += AstrolabeItem.charge(inventory.getItem(slot));
			}
		}
		return held;
	}

	/** A perch at {@code at} placed by {@code owner} (null: set without a placer, as a command or structure sets one). */
	static PactPerchBlockEntity perch(GameTestHelper helper, BlockPos at, @Nullable ServerPlayer owner) {
		helper.setBlock(at, Workers.PACT_PERCH);
		BlockPos absolute = helper.absolutePos(at);
		if (owner != null) {
			Workers.PACT_PERCH.setPlacedBy(helper.getLevel(), absolute, helper.getLevel().getBlockState(absolute), owner, new ItemStack(Workers.PACT_PERCH_ITEM));
		}
		return helper.getBlockEntity(at, PactPerchBlockEntity.class);
	}

	static PactMenu open(GameTestHelper helper, ServerPlayer player, PactPerchBlockEntity perch) {
		PactMenu.open(player, helper.getLevel(), perch);
		helper.assertTrue(player.containerMenu instanceof PactMenu, "The perch's screen opens");
		return (PactMenu) player.containerMenu;
	}

	static @Nullable PactStatePayload request(ServerPlayer player, int containerId, int sequence, int action, int choice, int revision) {
		RateGate.forget(player.getUUID());
		return PactMenu.receive(player, new PactRequestPayload(containerId, sequence, action, choice, revision));
	}

	/** As a screen sends: the next sequence and the revision of the state it last read. */
	static @Nullable PactStatePayload send(ServerPlayer player, PactMenu menu, int action, int choice) {
		return request(player, menu.containerId, menu.lastSequence() + 1, action, choice, menu.lastSent().revision());
	}

	/** A perch at {@code at} placed by {@code player}, who stands beside it, opens it and accepts through the menu. */
	static PactMenu acceptedAt(GameTestHelper helper, ServerPlayer player, BlockPos at) {
		PactPerchBlockEntity perch = perch(helper, at, player);
		beside(helper, player, at);
		PactMenu menu = open(helper, player, perch);
		helper.assertTrue(answered(send(player, menu, PactRequestPayload.ACCEPT, 0), PactMenu.SEALED), "The pact is sealed");
		return menu;
	}

	/** Stolas's terms as the perch seals them, the compact left out (these tests are about what follows acceptance). */
	static AgreementHolder.Sealing stolasTerms() {
		return PactPerchBlockEntity.sealing(stolasOffer(), null, null);
	}

	static AgreementHolder.Sealing withService(AgreementHolder.Sealing sealing, BoundWill.Service service) {
		return new AgreementHolder.Sealing(sealing.agreement(), sealing.counterpart(), service, sealing.upkeep(), sealing.beneficiaries(),
				sealing.onePerHolder(), sealing.partySeals(), sealing.affords(), sealing.pay());
	}

	/** Stolas's pact sealed for {@code holder} at a perch they placed at {@code at}, at game time {@code now}; they stand beside it. */
	static PactPerchBlockEntity stolasPact(GameTestHelper helper, ServerPlayer holder, BlockPos at, long now) {
		PactPerchBlockEntity perch = perch(helper, at, holder);
		beside(helper, holder, at);
		helper.assertValueEqual(perch.holder().seal(helper.getLevel(), holder, stolasTerms(), now).outcome(), Pacts.Outcome.DONE,
				"Stolas's pact is sealed at the perch");
		return perch;
	}

	static BoundWill pactOf(ServerLevel level, PactPerchBlockEntity perch) {
		return BoundWills.of(level.getServer()).ledger().get(perch.holder().agreement().record());
	}

	static @Nullable StolasEntity stolas(ServerLevel level, PactPerchBlockEntity perch) {
		UUID id = perch.holder().spirit();
		return id != null && level.getEntity(id) instanceof StolasEntity found && !found.gone() ? found : null;
	}

	static @Nullable BoundWill recordOf(ServerLevel level, ServerPlayer holder) {
		List<BoundWill> held = List.copyOf(BoundWills.of(level.getServer()).ledger().heldBy(holder.getUUID()));
		return held.size() == 1 ? held.getFirst() : null;
	}

	static List<StolasEntity> stolases(GameTestHelper helper) {
		return helper.getLevel().getEntitiesOfClass(StolasEntity.class, helper.getBounds(), found -> found.isAlive() && !found.gone());
	}

	static boolean answered(@Nullable PactStatePayload state, String reason) {
		return state != null && state.result().isPresent() && state.result().get().reason().equals(reason);
	}

	static String said(@Nullable PactStatePayload state) {
		return state == null ? "" : key(state.result().map(PactStatePayload.Result::message).orElse(null));
	}

	static List<String> keys(List<Component> lines) {
		return lines.stream().map(ConcordanceWorkerGameTests::key).toList();
	}

	static @Nullable Component term(PactStatePayload state, String key) {
		return state.terms().stream().filter(term -> term.key().equals(key)).map(PactStatePayload.Term::value).findFirst().orElse(null);
	}

	/** Stolas's offer as shipped (data/jugcraft/concordance/pact/stolas.json), read from the server's data. */
	static JsonObject shipped(ServerLevel level) {
		try (InputStream in = level.getServer().getResourceManager().getResourceOrThrow(Jugcraft.id("concordance/pact/stolas.json")).open();
				Reader reader = new InputStreamReader(in, StandardCharsets.UTF_8)) {
			return JsonParser.parseReader(reader).getAsJsonObject();
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	// D-05's helpers: Star-reckoning, the first obligation and upkeep on Stolas's own pact.

	static final long PERIOD_7 = 7 * Pacts.DAY;
	/** How often each player's pact recorded its service, heard as the Conclave hears it (every record, repeats included). */
	private static final Map<UUID, Integer> SERVED = new ConcurrentHashMap<>();

	static {
		ConcordanceProgress.listen((player, evidence, result) -> {
			if (evidence instanceof Evidence.Practiced practiced && StolasRegard.ACTIVITY.equals(practiced.activity())) {
				SERVED.merge(player.getUUID(), 1, Integer::sum);
			}
		});
	}

	static int served(ServerPlayer player) {
		return SERVED.getOrDefault(player.getUUID(), 0);
	}

	static ResearchState regard(ServerPlayer player) {
		return ConcordanceProgress.knowledge(player).state(StolasRegard.RESEARCH);
	}

	/** Early in a game day, {@code days} game days after now (the quota counts game days). */
	static long dayStart(ServerLevel level, int days) {
		return (Math.floorDiv(level.getGameTime(), Pacts.DAY) + days) * Pacts.DAY + 1_000L;
	}

	static ItemStack holding(ServerPlayer player, int charge) {
		ItemStack held = astrolabe(charge);
		player.setItemInHand(InteractionHand.MAIN_HAND, held);
		return held;
	}

	static ItemStack astrolabe(int charge) {
		ItemStack stack = new ItemStack(Sky.ASTROLABE);
		AstrolabeItem.setCharge(stack, charge);
		return stack;
	}

	static void observed(ServerLevel level, ServerPlayer player, String... patterns) {
		for (String pattern : patterns) {
			AstralClaims.of(level.getServer()).observe(player.getUUID(), pattern, 0L);
		}
	}

	static Pacts.Reckoning reckon(ServerLevel level, ServerPlayer player, PactPerchBlockEntity perch, String pattern, long time, long gameTime) {
		return StarReckoning.reckon(level, player, perch.holder(), pattern, time, gameTime);
	}

	static boolean recalledEver(ServerLevel level, ServerPlayer player, String pattern) {
		return AstralClaims.of(level.getServer()).ledger(player.getUUID()).claimed("recall:" + pattern);
	}

	static int done(ServerLevel level, PactPerchBlockEntity perch, long gameTime) {
		return Pacts.done(pactOf(level, perch).pact(), gameTime);
	}

	static int shards(ServerPlayer player) {
		return player.getInventory().countItem(Items.AMETHYST_SHARD);
	}

	static void give(ServerPlayer player, ItemStack stack) {
		player.getInventory().add(stack);
	}

	static Item starflower() {
		return JugcraftStyx.FLOWERS.get("stolas_starflower").asItem();
	}

	static ObservatoryBlockEntity observatory(GameTestHelper helper, BlockPos at) {
		helper.setBlock(at, Sky.OBSERVATORY);
		return helper.getBlockEntity(at, ObservatoryBlockEntity.class);
	}

	static int looseNear(ServerLevel level, AABB box, Item item) {
		return level.getEntitiesOfClass(ItemEntity.class, box, entity -> entity.isAlive() && entity.getItem().is(item)).stream()
				.mapToInt(entity -> entity.getItem().getCount()).sum();
	}

	static boolean earned(ServerPlayer player, Identifier id) {
		AdvancementHolder advancement = player.level().getServer().getAdvancements().get(id);
		return advancement != null && player.getAdvancements().getOrStartProgress(advancement).isDone();
	}

	// ---------------------------------------------------------------- Concordance Chunk D, D-04: the pact perch, Stolas and his offer

	/** D-04 (D-S7, D-S14, D-S15): ready, at their own perch, its owner is sent Stolas's three beats and every term at revision 1; Stolas arrives as a visitor bound to no one; nothing is stored, noted or spent; asking to hear the terms answers with the state alone. */
	@GameTest(maxTicks = 20)
	public void stolasExplainsBeforeAgreeing(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		PactDefinition stolas = stolasOffer();
		ServerPlayer owner = ready(level, TestPlayers.survivor(helper));
		pocket(owner, 1, AstrolabeItem.CAPACITY);
		BlockPos at = new BlockPos(3, 1, 3);
		PactPerchBlockEntity perch = perch(helper, at, owner);
		beside(helper, owner, at);
		PactMenu menu = open(helper, owner, perch);
		PactStatePayload state = menu.lastSent();
		helper.assertTrue(state != null && state.containerId() == menu.containerId && state.revision() == 1
				&& state.mode() == PactStatePayload.NEGOTIATE && state.result().isEmpty(), "Opened, the screen is sent its first state at revision 1: " + state);
		helper.assertValueEqual(keys(state.beats()), stolas.beats().stream().map(beat -> PACT_KEY + "stolas.beat." + beat).toList(), "his three beats, in order");
		helper.assertValueEqual(state.terms().stream().map(PactStatePayload.Term::key).toList(), PactStatePayload.TERMS,
				"every term, in order, before anything is agreed");
		helper.assertTrue(Integer.valueOf(stolas.radius()).equals(arg(term(state, "area"), 0))
				&& Integer.valueOf(stolas.quota()).equals(arg(term(state, "quota"), 0))
				&& Long.valueOf(stolas.period() / Agreement.DAY).equals(arg(term(state, "upkeep"), 2))
				&& Integer.valueOf(stolas.cap()).equals(arg(term(state, "upkeep"), 3))
				&& Long.valueOf(stolas.grace() / Agreement.DAY).equals(arg(term(state, "grace"), 0))
				&& Integer.valueOf(stolas.resonance()).equals(arg(term(state, "compact"), 0))
				&& state.resonance() == stolas.resonance() && state.compact().size() == stolas.compact().size(),
				"with the definition's numbers: the area, the quota, the period, the cap, the grace, the compact");
		helper.assertTrue(state.offers(PactRequestPayload.ACCEPT) && state.offers(PactRequestPayload.DECLINE) && state.notes().isEmpty()
				&& state.patterns().isEmpty() && state.offerings().isEmpty(), "Ready, they may accept or decline; nothing is missing");
		List<StolasEntity> visitors = stolases(helper);
		helper.assertTrue(visitors.size() == 1 && visitors.getFirst().owner() == null && visitors.getFirst().getUUID().equals(perch.visitor())
				&& helper.absolutePos(at).equals(visitors.getFirst().perch()), "Stolas arrives at the perch: a visitor, bound to no one");
		helper.assertValueEqual(visitors.getFirst().think(level, 0L, level.getGameTime()), Status.IDLE, "and stays while the conversation is open");
		helper.assertTrue(BoundWills.of(level.getServer()).ledger().heldBy(owner.getUUID()).isEmpty() && perch.holder().agreement() == null
				&& WorkerRoster.of(level.getServer()).workers(owner.getUUID()).isEmpty(), "Nothing is stored: no Bound Will, no agreement, no roster entry");
		helper.assertTrue(pocketed(owner, 1) && resonance(owner) == AstrolabeItem.CAPACITY && !owner.hasInfiniteMaterials(),
				"and a survival player has spent nothing");
		PactStatePayload heard = send(owner, menu, PactRequestPayload.CHOOSE, 0);
		helper.assertTrue(heard != null && heard.result().isEmpty() && heard.revision() == 1 && menu.lastSequence() == 1,
				"Asking to hear the terms answers with the state alone");
		helper.succeed();
	}

	/** D-04 (D-S14; the revision counts the offer): a revision the server never showed is refused as changed, the terms sent again; a reload that changes the offer under an open screen refuses acceptance of the old terms the same way; accepting the terms now shown seals at their cost, once. Nothing is agreed or spent before. */
	@GameTest(maxTicks = 20)
	public void aStaleTermsRevisionIsRefused(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		PactDefinition stolas = stolasOffer();
		ServerPlayer owner = ready(level, TestPlayers.survivor(helper));
		pocket(owner, 1, AstrolabeItem.CAPACITY);
		BlockPos at = new BlockPos(3, 1, 3);
		PactPerchBlockEntity perch = perch(helper, at, owner);
		beside(helper, owner, at);
		PactMenu menu = open(helper, owner, perch);
		List<PactStatePayload.Term> read = menu.lastSent().terms();
		PactStatePayload never = request(owner, menu.containerId, 1, PactRequestPayload.ACCEPT, 0, 0);
		helper.assertTrue(answered(never, PactMenu.CHANGED) && never.revision() == 1 && never.terms().equals(read),
				"A revision the server never showed: changed, the terms sent again");
		helper.assertTrue(perch.holder().agreement() == null && pocketed(owner, 1) && resonance(owner) == AstrolabeItem.CAPACITY, "nothing agreed or spent");
		ConcordanceRules was = ConcordanceData.rules();
		JsonObject dearer = shipped(level);
		dearer.getAsJsonObject("compact").addProperty("resonance", stolas.resonance() + 4);
		try {
			ConcordanceData.replace(was.withPacts(PactCatalog.parse(Map.of(PactDefinition.STOLAS, dearer), was.research().keySet())));
			PactStatePayload refused = send(owner, menu, PactRequestPayload.ACCEPT, 0);
			helper.assertTrue(answered(refused, PactMenu.CHANGED) && refused.revision() == 2
					&& Integer.valueOf(stolas.resonance() + 4).equals(arg(term(refused, "compact"), 0)),
					"The offer changed under the screen: accepting what was read is refused as changed, the new terms sent");
			helper.assertTrue(perch.holder().agreement() == null && pocketed(owner, 1) && resonance(owner) == AstrolabeItem.CAPACITY, "nothing agreed or spent");
			PactStatePayload accepted = send(owner, menu, PactRequestPayload.ACCEPT, 0);
			helper.assertTrue(answered(accepted, PactMenu.SEALED) && accepted.result().get().done() && pocketed(owner, 0)
					&& resonance(owner) == AstrolabeItem.CAPACITY - stolas.resonance() - 4, "Accepting the terms now shown seals at their cost, once");
		} finally {
			ConcordanceData.replace(was);
		}
		helper.succeed();
	}

	/** D-04 (D-S13, E1): no star observed, one resonance short, declining, or Stolas's Regard not begun: each is refused with its reason (every step named), nothing is taken, called or sealed. Survival players. */
	@GameTest(maxTicks = 20)
	public void aRefusalSpendsNothing(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		PactDefinition stolas = stolasOffer();
		ServerPlayer owner = TestPlayers.survivor(helper);
		ConcordanceProgress.grant(owner, "jugcraft:first_light", ResearchState.UNDERSTOOD);
		ConcordanceProgress.grant(owner, Workers.RESEARCH, ResearchState.UNDERSTOOD);
		ConcordanceProgress.grant(owner, stolas.research(), ResearchState.UNDERSTOOD);
		RateGate.forget(owner.getUUID());
		pocket(owner, 1, stolas.resonance() - 1);
		BlockPos at = new BlockPos(2, 1, 2);
		PactPerchBlockEntity perch = perch(helper, at, owner);
		beside(helper, owner, at);
		PactMenu menu = open(helper, owner, perch);
		PactStatePayload opened = menu.lastSent();
		helper.assertTrue(keys(opened.notes()).contains(PACT_KEY + "missing.observation") && !opened.offers(PactRequestPayload.ACCEPT)
				&& perch.visitor() == null && stolases(helper).isEmpty(), "No star observed: the screen names the sky step; Stolas does not come");
		helper.assertTrue(answered(send(owner, menu, PactRequestPayload.ACCEPT, 0), PactMenu.NO_OBSERVATION), "Accepting anyway: no observation");
		AstralClaims.of(level.getServer()).observe(owner.getUUID(), LANTERN_STAR, level.getGameTime());
		helper.assertTrue(answered(send(owner, menu, PactRequestPayload.ACCEPT, 0), Pacts.Outcome.CANNOT_AFFORD.id),
				"A star observed, one resonance short: the compact cannot be paid");
		helper.assertTrue(pocketed(owner, 1) && resonance(owner) == stolas.resonance() - 1 && stolases(helper).isEmpty()
				&& BoundWills.of(level.getServer()).ledger().heldBy(owner.getUUID()).isEmpty(), "Nothing is taken, called or sealed");
		PactStatePayload declined = send(owner, menu, PactRequestPayload.DECLINE, 0);
		helper.assertTrue(answered(declined, PactMenu.DECLINED) && owner.containerMenu == owner.inventoryMenu
				&& pocketed(owner, 1) && resonance(owner) == stolas.resonance() - 1, "Declining closes the conversation and spends nothing");
		ServerPlayer novice = TestPlayers.survivor(helper);
		ConcordanceProgress.grant(novice, "jugcraft:first_light", ResearchState.UNDERSTOOD);
		ConcordanceProgress.grant(novice, Workers.RESEARCH, ResearchState.UNDERSTOOD);
		AstralClaims.of(level.getServer()).observe(novice.getUUID(), LANTERN_STAR, level.getGameTime());
		RateGate.forget(novice.getUUID());
		pocket(novice, 1, AstrolabeItem.CAPACITY);
		BlockPos theirs = new BlockPos(5, 1, 2);
		PactPerchBlockEntity own = perch(helper, theirs, novice);
		beside(helper, novice, theirs);
		PactMenu learning = open(helper, novice, own);
		helper.assertTrue(Collections.frequency(keys(learning.lastSent().notes()), PACT_KEY + "missing.step") == 3,
				"Stolas's Regard not begun: each of its three steps is named");
		helper.assertTrue(answered(send(novice, learning, PactRequestPayload.ACCEPT, 0), PactMenu.MISSING_EVIDENCE)
				&& pocketed(novice, 1) && resonance(novice) == AstrolabeItem.CAPACITY, "accepting is refused for the evidence; nothing spent");
		helper.assertTrue(!owner.hasInfiniteMaterials() && !novice.hasInfiniteMaterials(), "both ordinary survival players");
		helper.succeed();
	}

	/** D-04 (D-S13, E5): accepting spends the compact once in the call that seals; one Bound Will, its terms copied, the visitor its presence on the roster as a pact, the first offering due a period on; accepting again seals and spends nothing more. */
	@GameTest(maxTicks = 20)
	public void acceptingSealsOnceAndSpendsOnce(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		PactDefinition stolas = stolasOffer();
		ServerPlayer owner = ready(level, TestPlayers.survivor(helper));
		pocket(owner, 2, AstrolabeItem.CAPACITY);
		BlockPos at = new BlockPos(3, 1, 3);
		PactPerchBlockEntity perch = perch(helper, at, owner);
		beside(helper, owner, at);
		PactMenu menu = open(helper, owner, perch);
		UUID visitor = perch.visitor();
		long now = level.getGameTime();
		PactStatePayload sealed = send(owner, menu, PactRequestPayload.ACCEPT, 0);
		helper.assertTrue(answered(sealed, PactMenu.SEALED) && sealed.mode() == PactStatePayload.KEEP
				&& sealed.terms().stream().map(PactStatePayload.Term::key).toList().equals(PactStatePayload.TERMS), "Accepted: sealed, now kept here, its terms shown");
		helper.assertTrue(pocketed(owner, 1) && resonance(owner) == AstrolabeItem.CAPACITY - stolas.resonance(), "The compact is spent once");
		BoundWill record = recordOf(level, owner);
		helper.assertTrue(record != null && PactDefinition.STOLAS.equals(record.counterpart()) && stolas.agreement().equals(record.agreement()) && !record.transferable()
				&& Couriers.place(level, helper.absolutePos(at)).equals(record.pact().place()) && visitor.equals(record.pact().presence())
				&& new BoundWill.Service(stolas.work(), stolas.radius(), stolas.from(), stolas.to(), stolas.quota()).equals(record.pact().service())
				&& record.pact().upkeep().due() == now + stolas.period() && stolas.offering().equals(record.pact().upkeep().offering())
				&& Pacts.PERSONAL.equals(record.pact().beneficiaries()), "One Bound Will: kept at this perch, its terms copied, the first offering due a period on");
		StolasEntity presence = (StolasEntity) level.getEntity(visitor);
		helper.assertTrue(presence != null && owner.getUUID().equals(presence.owner()) && perch.visitor() == null && stolases(helper).size() == 1
				&& "pact".equals(WorkerRoster.of(level.getServer()).workers(owner.getUUID()).get(visitor).kind()),
				"The visitor stays as the pact's one presence, on its holder's roster as a pact");
		helper.assertTrue(presence.think(level, 0L, now) == Status.IDLE && record.id().equals(presence.record()), "and knows its record");
		PactStatePayload again = send(owner, menu, PactRequestPayload.ACCEPT, 0);
		helper.assertTrue(answered(again, Pacts.Outcome.TAKEN.id) && said(again).equals("message.jugcraft.concordance.workers.seal_taken")
				&& pocketed(owner, 1) && resonance(owner) == AstrolabeItem.CAPACITY - stolas.resonance()
				&& BoundWills.of(level.getServer()).ledger().heldBy(owner.getUUID()).size() == 1, "Accepting again seals nothing and spends nothing");
		helper.assertTrue(BoundWills.of(level.getServer()).problems().isEmpty(), "The records are sound: " + BoundWills.of(level.getServer()).problems());
		helper.succeed();
	}

	/** D-04 (D-S10, D-S15, E5): one Stolas pact a holder: at a second perch of theirs Stolas does not come and accepting is refused, nothing spent; another holder keeps their own. */
	@GameTest(maxTicks = 20)
	public void oneStolasPactPerHolder(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		ServerPlayer owner = ready(level, TestPlayers.survivor(helper));
		pocket(owner, 2, AstrolabeItem.CAPACITY);
		acceptedAt(helper, owner, new BlockPos(2, 1, 2));
		int left = resonance(owner);
		BlockPos second = new BlockPos(6, 1, 2);
		PactPerchBlockEntity other = perch(helper, second, owner);
		beside(helper, owner, second);
		PactMenu menu = open(helper, owner, other);
		helper.assertTrue(keys(menu.lastSent().notes()).contains(PACT_KEY + "missing.held") && !menu.lastSent().offers(PactRequestPayload.ACCEPT)
				&& other.visitor() == null && stolases(helper).size() == 1, "At a second perch: already held; Stolas does not come");
		PactStatePayload refused = send(owner, menu, PactRequestPayload.ACCEPT, 0);
		helper.assertTrue(answered(refused, Pacts.Outcome.ALREADY_HELD.id) && said(refused).equals("message.jugcraft.concordance.workers.seal_already_held")
				&& pocketed(owner, 1) && resonance(owner) == left && BoundWills.of(level.getServer()).ledger().heldBy(owner.getUUID()).size() == 1,
				"Accepting there is refused, nothing spent; one pact");
		ServerPlayer neighbour = ready(level, TestPlayers.survivor(helper));
		pocket(neighbour, 1, AstrolabeItem.CAPACITY);
		acceptedAt(helper, neighbour, new BlockPos(2, 1, 6));
		helper.assertTrue(recordOf(level, neighbour) != null && stolases(helper).size() == 2, "Any number of holders keep their own");
		helper.succeed();
	}

	/** D-04 (E1, D-S8, D-S14): a party member at the owner's perch is told only its owner may invite; Stolas does not come; accepting is not permitted, nothing spent; requests for another menu, an answered sequence, out of bounds, after closing, or from a stand-in are dropped. */
	@GameTest(maxTicks = 20)
	public void anotherPlayerCannotAcceptForTheHolder(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		ServerPlayer owner = ready(level, TestPlayers.survivor(helper));
		ServerPlayer member = ready(level, TestPlayers.survivor(helper));
		pocket(member, 1, AstrolabeItem.CAPACITY);
		PartyManager parties = JugcraftParties.manager();
		long ms = System.currentTimeMillis();
		try {
			helper.assertTrue(parties.invite(owner.getUUID(), member.getUUID(), ms) == PartyManager.Result.OK
					&& parties.accept(member.getUUID(), ms) == PartyManager.Result.OK, "The owner and a member form a party");
			BlockPos at = new BlockPos(3, 1, 3);
			PactPerchBlockEntity perch = perch(helper, at, owner);
			beside(helper, member, at);
			PactMenu menu = open(helper, member, perch);
			helper.assertTrue(keys(menu.lastSent().notes()).contains(PACT_KEY + "missing.owner") && perch.visitor() == null,
					"Only the perch's owner may invite: Stolas does not come for a party member");
			PactStatePayload refused = send(member, menu, PactRequestPayload.ACCEPT, 0);
			helper.assertTrue(answered(refused, Pacts.Outcome.NOT_PERMITTED.id) && said(refused).equals("message.jugcraft.concordance.workers.seal_not_permitted")
					&& pocketed(member, 1) && resonance(member) == AstrolabeItem.CAPACITY, "Accepting is not permitted; nothing spent");
			int revision = refused.revision();
			int answeredAt = refused.result().get().sequence();
			helper.assertTrue(request(member, menu.containerId + 1, 50, PactRequestPayload.ACCEPT, 0, revision) == null, "A request naming another menu's container is dropped");
			helper.assertTrue(request(member, menu.containerId, answeredAt, PactRequestPayload.ACCEPT, 0, revision) == null, "one repeating an answered sequence is dropped");
			helper.assertTrue(request(member, menu.containerId, 60, PactRequestPayload.ACTIONS.size(), 0, revision) == null
					&& request(member, menu.containerId, 61, PactRequestPayload.ACCEPT, PactStatePayload.MAX_CHOICES, revision) == null, "out of bounds is dropped");
			int closed = menu.containerId;
			member.closeContainer();
			helper.assertTrue(request(member, closed, 70, PactRequestPayload.ACCEPT, 0, revision) == null, "after closing, it names nothing");
			FakePlayer standIn = FakePlayer.get(level, new GameProfile(owner.getUUID(), "[Concordance]"));
			helper.assertTrue(PactMenu.receive(standIn, new PactRequestPayload(closed, 1, PactRequestPayload.ACCEPT, 0, 1)) == null, "and a stand-in is never answered");
			helper.assertTrue(BoundWills.of(level.getServer()).ledger().heldBy(member.getUUID()).isEmpty()
					&& BoundWills.of(level.getServer()).ledger().heldBy(owner.getUUID()).isEmpty() && perch.holder().agreement() == null, "No one was bound");
		} finally {
			parties.adminDisband(owner.getUUID());
		}
		helper.succeed();
	}

	/**
	 * D-04 (D-S8, E5): two conversations at one perch share one visitor; its owner's acceptance seals, and the other player,
	 * not its owner, is not permitted and spends nothing (a second acceptance by a permitted player is TAKEN:
	 * acceptingSealsOnceAndSpendsOnce). A perch no player placed (set by a machine or a command) seals for nobody: Stolas
	 * does not come and accepting is not permitted (the unit review).
	 */
	@GameTest(maxTicks = 20)
	public void twoNegotiationsAtOnePerchSealOnce(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		ServerPlayer first = ready(level, TestPlayers.survivor(helper));
		ServerPlayer second = ready(level, TestPlayers.survivor(helper));
		pocket(first, 1, AstrolabeItem.CAPACITY);
		pocket(second, 1, AstrolabeItem.CAPACITY);
		BlockPos at = new BlockPos(3, 1, 3);
		PactPerchBlockEntity perch = perch(helper, at, first);
		beside(helper, first, at);
		beside(helper, second, at);
		PactMenu one = open(helper, first, perch);
		PactMenu two = open(helper, second, perch);
		helper.assertTrue(stolases(helper).size() == 1 && perch.visitor() != null && keys(two.lastSent().notes()).contains(PACT_KEY + "missing.owner"),
				"Two conversations at one perch: one visitor, and the player who is not its owner told so");
		helper.assertTrue(answered(send(first, one, PactRequestPayload.ACCEPT, 0), PactMenu.SEALED), "Its owner accepts: sealed");
		PactStatePayload late = send(second, two, PactRequestPayload.ACCEPT, 0);
		helper.assertTrue(answered(late, Pacts.Outcome.NOT_PERMITTED.id) && said(late).equals("message.jugcraft.concordance.workers.seal_not_permitted"),
				"The other player is not permitted");
		helper.assertTrue(pocketed(second, 1) && resonance(second) == AstrolabeItem.CAPACITY
				&& BoundWills.of(level.getServer()).ledger().heldBy(second.getUUID()).isEmpty() && pocketed(first, 0)
				&& recordOf(level, first) != null && stolases(helper).size() == 1, "Only the owner spent and holds; one Stolas");
		BlockPos built = new BlockPos(6, 1, 6);
		PactPerchBlockEntity unowned = perch(helper, built, null);
		beside(helper, second, built);
		PactMenu three = open(helper, second, unowned);
		helper.assertTrue(unowned.holder().owner() == null && unowned.visitor() == null && stolases(helper).size() == 1
				&& keys(three.lastSent().notes()).contains(PACT_KEY + "missing.owner") && !three.lastSent().offers(PactRequestPayload.ACCEPT),
				"At a perch no player placed, Stolas does not come");
		PactStatePayload refused = send(second, three, PactRequestPayload.ACCEPT, 0);
		helper.assertTrue(answered(refused, Pacts.Outcome.NOT_PERMITTED.id) && pocketed(second, 1) && resonance(second) == AstrolabeItem.CAPACITY
				&& unowned.holder().agreement() == null && BoundWills.of(level.getServer()).ledger().heldBy(second.getUUID()).isEmpty(),
				"and it seals for nobody, spending nothing");
		second.closeContainer();
		helper.assertTrue(BoundWills.of(level.getServer()).problems().isEmpty(), "no place keeps two");
		helper.succeed();
	}

	/** D-04 (D-S7, D-S9): a visitor with no pact leaves when its conversation ends: declined, closed, timed out, its player gone, or after a restart (no session is saved; a visitor saved with his chunk is no perch's visitor when he loads, and leaves, with the Concordance on or off). Nothing is recorded or noted. */
	@GameTest(maxTicks = 20)
	public void aVisitingStolasLeavesWithoutARecord(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		long now = level.getGameTime();
		record Visit(ServerPlayer player, PactPerchBlockEntity perch, PactMenu menu, StolasEntity visitor) {
		}
		Function<BlockPos, Visit> visit = at -> {
			ServerPlayer player = ready(level, TestPlayers.mock(helper));
			PactPerchBlockEntity perch = perch(helper, at, player);
			beside(helper, player, at);
			PactMenu menu = open(helper, player, perch);
			StolasEntity visitor = (StolasEntity) level.getEntity(perch.visitor());
			helper.assertTrue(visitor != null && visitor.think(level, 0L, now) == Status.IDLE && !visitor.gone(), "Stolas waits while it is open");
			return new Visit(player, perch, menu, visitor);
		};
		Visit declined = visit.apply(new BlockPos(1, 1, 1));
		send(declined.player(), declined.menu(), PactRequestPayload.DECLINE, 0);
		helper.assertTrue(declined.visitor().think(level, 0L, now) == Status.FINISHED && declined.visitor().gone(), "Declined: he leaves");
		Visit closed = visit.apply(new BlockPos(4, 1, 1));
		closed.player().closeContainer();
		helper.assertTrue(closed.visitor().think(level, 0L, now) == Status.FINISHED && closed.visitor().gone(), "Closed: he leaves");
		Visit idle = visit.apply(new BlockPos(7, 1, 1));
		helper.assertTrue(idle.visitor().think(level, 0L, now + PactSessions.TIMEOUT_TICKS) == Status.IDLE, "Within its time he waits");
		helper.assertTrue(idle.visitor().think(level, 0L, now + PactSessions.TIMEOUT_TICKS + 1) == Status.FINISHED && idle.visitor().gone(), "Timed out: he leaves");
		Visit gone = visit.apply(new BlockPos(1, 1, 5));
		TestPlayers.leave(gone.player());
		helper.assertTrue(gone.visitor().think(level, 0L, now) == Status.FINISHED && gone.visitor().gone(), "Its player gone: he leaves");
		Visit saved = visit.apply(new BlockPos(5, 1, 5));
		CompoundTag perchSave = saved.perch().saveWithoutMetadata(level.registryAccess());
		TagValueOutput out = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, level.registryAccess());
		saved.visitor().saveWithoutId(out);
		CompoundTag body = out.buildResult();
		helper.assertTrue(!perchSave.contains("visitor") && body.contains("perch") && !body.contains("record") && !body.contains("owner"),
				"Neither the perch nor Stolas saves a visit: " + body);
		saved.perch().loadWithComponents(TagValueInput.create(ProblemReporter.DISCARDING, level.registryAccess(), perchSave));
		StolasEntity reloaded = Workers.STOLAS.create(level, EntitySpawnReason.LOAD);
		reloaded.load(TagValueInput.create(ProblemReporter.DISCARDING, level.registryAccess(), body));
		helper.assertTrue(saved.perch().visitor() == null && reloaded.think(level, 0L, now) == Status.FINISHED && reloaded.gone()
				&& saved.visitor().think(level, 0L, now) == Status.FINISHED, "After a restart the perch knows no visitor, and he leaves");
		StolasEntity loadedOff = Workers.STOLAS.create(level, EntitySpawnReason.LOAD);
		loadedOff.load(TagValueInput.create(ProblemReporter.DISCARDING, level.registryAccess(), body));
		off(() -> helper.assertTrue(loadedOff.think(level, 0L, now) == Status.FINISHED && loadedOff.gone(),
				"Loaded while the Concordance is off, a visitor leaves all the same"));
		for (Visit each : List.of(declined, closed, idle, gone, saved)) {
			helper.assertTrue(BoundWills.of(level.getServer()).ledger().heldBy(each.player().getUUID()).isEmpty()
					&& WorkerRoster.of(level.getServer()).workers(each.player().getUUID()).isEmpty() && each.perch().holder().agreement() == null, "Nothing recorded or noted");
		}
		helper.succeed();
	}

	/** D-04 (D-S7): a transport crate refuses Stolas, presence or visitor (MISC), where it takes a cow; he stays. */
	@GameTest(maxTicks = 20)
	public void aCrateRefusesStolas(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		ServerPlayer owner = ready(level, TestPlayers.mock(helper));
		acceptedAt(helper, owner, new BlockPos(2, 1, 2));
		StolasEntity presence = (StolasEntity) level.getEntity(recordOf(level, owner).pact().presence());
		ServerPlayer guest = ready(level, TestPlayers.mock(helper));
		BlockPos other = new BlockPos(5, 1, 2);
		PactPerchBlockEntity perch = perch(helper, other, guest);
		beside(helper, guest, other);
		open(helper, guest, perch);
		StolasEntity visitor = (StolasEntity) level.getEntity(perch.visitor());
		ServerPlayer carrier = TestPlayers.mock(helper);
		beside(helper, carrier, new BlockPos(3, 1, 2));
		ItemStack crate = new ItemStack(MobTransportCrate.WOOD);
		carrier.setItemInHand(InteractionHand.MAIN_HAND, crate);
		helper.assertTrue(Workers.STOLAS.getCategory() == MobCategory.MISC && !MobTransportCrate.friendly(presence) && !MobTransportCrate.friendly(visitor),
				"Stolas is no calm farm creature to a crate");
		helper.assertTrue(!MobTransportCrate.capture(carrier, crate, presence) && !MobTransportCrate.capture(carrier, crate, visitor)
				&& !presence.isRemoved() && !visitor.isRemoved() && MobTransportCrate.count(crate) == 0, "It takes neither the presence nor the visitor");
		Cow cow = EntityTypes.COW.create(level, EntitySpawnReason.MOB_SUMMONED);
		Vec3 by = helper.absoluteVec(new Vec3(3.5, 1.0, 4.5));
		cow.snapTo(by.x, by.y, by.z, 0.0F, 0.0F);
		level.addFreshEntity(cow);
		helper.assertTrue(MobTransportCrate.capture(carrier, crate, cow) && MobTransportCrate.count(crate) == 1, "where it takes a cow");
		helper.succeed();
	}

	/** D-04 (D-S7, 03:146): no ordinary damage harms Stolas, and harming him ends nothing; only damage past invulnerability hurts him. */
	@GameTest(maxTicks = 20)
	public void stolasTakesNoOrdinaryDamage(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		ServerPlayer owner = ready(level, TestPlayers.mock(helper));
		acceptedAt(helper, owner, new BlockPos(3, 1, 3));
		BoundWill record = recordOf(level, owner);
		StolasEntity presence = (StolasEntity) level.getEntity(record.pact().presence());
		float health = presence.getHealth();
		for (DamageSource source : List.of(level.damageSources().generic(), level.damageSources().playerAttack(owner), level.damageSources().fall(),
				level.damageSources().inFire(), level.damageSources().magic())) {
			helper.assertTrue(!presence.hurtServer(level, source, 1000.0F) && presence.getHealth() == health && presence.isAlive(), "unharmed by " + source.getMsgId());
		}
		helper.assertTrue(presence.fireImmune() && presence.hurtServer(level, level.damageSources().fellOutOfWorld(), 1.0F) && presence.getHealth() < health,
				"only damage past invulnerability hurts him");
		helper.assertTrue(record.equals(BoundWills.of(level.getServer()).ledger().get(record.id())), "and his pact is untouched");
		helper.succeed();
	}

	/** D-04 (D-S23, 11:59): with stolas.json invalid, then missing, the reload reports it; new invitations are refused with the reason (Stolas does not come; nothing spent); a sealed pact keeps the terms copied at sealing. */
	@GameTest(maxTicks = 20)
	public void aBrokenPactDefinitionIsReported(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		ServerPlayer holder = ready(level, TestPlayers.mock(helper));
		BlockPos kept = new BlockPos(2, 1, 2);
		acceptedAt(helper, holder, kept);
		holder.closeContainer();
		PactPerchBlockEntity keptPerch = helper.getBlockEntity(kept, PactPerchBlockEntity.class);
		BoundWill record = recordOf(level, holder);
		ServerPlayer newcomer = ready(level, TestPlayers.survivor(helper));
		pocket(newcomer, 1, AstrolabeItem.CAPACITY);
		BlockPos fresh = new BlockPos(6, 1, 2);
		PactPerchBlockEntity freshPerch = perch(helper, fresh, newcomer);
		ConcordanceRules was = ConcordanceData.rules();
		// Read while the shipped offer is loaded: the loop below runs with it refused or missing.
		PactDefinition offer = stolasOffer();
		JsonObject invalid = shipped(level);
		invalid.addProperty("surprise", true);
		PactCatalog broken = PactCatalog.parse(Map.of(PactDefinition.STOLAS, invalid), was.research().keySet());
		helper.assertTrue(broken.get(PactDefinition.STOLAS) == null && broken.problems().stream().anyMatch(p -> p.startsWith("pact jugcraft:stolas:") && p.contains("surprise")),
				"An invalid definition is refused, naming its fault: " + broken.problems());
		for (PactCatalog catalog : List.of(broken, PactCatalog.parse(Map.of(), was.research().keySet()))) {
			try {
				ConcordanceData.replace(was.withPacts(catalog));
				helper.assertTrue(ConcordanceData.rules().problems().stream().anyMatch(p -> p.startsWith("pact jugcraft:stolas")), "the reload reports it with the rules' problems");
				beside(helper, newcomer, fresh);
				PactMenu menu = open(helper, newcomer, freshPerch);
				helper.assertTrue(menu.lastSent().mode() == PactStatePayload.CLOSED && keys(menu.lastSent().notes()).contains(PACT_KEY + "unavailable")
						&& freshPerch.visitor() == null, "New invitations are refused with the reason; Stolas does not come");
				helper.assertTrue(answered(send(newcomer, menu, PactRequestPayload.ACCEPT, 0), PactMenu.NO_DEFINITION)
						&& pocketed(newcomer, 1, offer) && BoundWills.of(level.getServer()).ledger().heldBy(newcomer.getUUID()).isEmpty(), "accepting spends nothing");
				newcomer.closeContainer();
				beside(helper, holder, kept);
				PactMenu theirs = open(helper, holder, keptPerch);
				helper.assertTrue(theirs.lastSent().mode() == PactStatePayload.KEEP
						&& theirs.lastSent().terms().stream().map(PactStatePayload.Term::key).toList().equals(PactStatePayload.TERMS)
						&& record.equals(BoundWills.of(level.getServer()).ledger().get(record.id()))
						&& keptPerch.holder().verify(level, level.getGameTime()) == AgreementHolder.Standing.VALID, "A sealed pact keeps the terms it copied and goes on");
				holder.closeContainer();
			} finally {
				ConcordanceData.replace(was);
			}
		}
		helper.succeed();
	}

	/**
	 * D-04 (D-S14): the pact screen's two messages are bounded both ways. A state's lists are cut to their limits (an empty
	 * stack dropped) before anything is sent, and it reads back as written; a request is its five numbers and nothing more;
	 * a term's key past its bound cannot be encoded, so it is never sent; a choice's id, which data names, encodes at any
	 * length data may give it (the unit review: a long offering id never drops its player).
	 */
	@GameTest(maxTicks = 20)
	public void thePactScreensMessagesAreBounded(GameTestHelper helper) {
		RegistryAccess registries = helper.getLevel().registryAccess();
		List<Component> many = IntStream.range(0, 14).<Component>mapToObj(i -> Component.literal("line " + i)).toList();
		List<PactStatePayload.Term> terms = IntStream.range(0, 20)
				.mapToObj(i -> new PactStatePayload.Term("term_" + i, Component.literal("value " + i))).toList();
		List<PactStatePayload.Choice> choices = IntStream.range(0, 12)
				.mapToObj(i -> new PactStatePayload.Choice("jugcraft:choice_" + i, Component.literal("choice " + i), i, i % 2 == 0)).toList();
		PactStatePayload state = new PactStatePayload(7, 3, PactStatePayload.NEGOTIATE, Component.literal("Stolas"), many, terms, many,
				choices, choices, List.of(new ItemStack(Items.FEATHER, 2), ItemStack.EMPTY), 6,
				PactStatePayload.mask(PactRequestPayload.ACCEPT, PactRequestPayload.DECLINE),
				Optional.of(new PactStatePayload.Result(4, PactMenu.SEALED, true, Component.literal("sealed"))));
		helper.assertTrue(state.beats().size() == PactStatePayload.MAX_BEATS && state.terms().size() == PactStatePayload.MAX_TERMS
				&& state.notes().size() == PactStatePayload.MAX_NOTES && state.patterns().size() == PactStatePayload.MAX_CHOICES
				&& state.offerings().size() == PactStatePayload.MAX_CHOICES && state.compact().size() == 1,
				"A state's lists are cut to their limits and an empty stack is dropped");
		RegistryFriendlyByteBuf buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), registries);
		PactStatePayload.CODEC.encode(buffer, state);
		PactStatePayload back = PactStatePayload.CODEC.decode(buffer);
		helper.assertTrue(back.containerId() == 7 && back.revision() == 3 && back.mode() == PactStatePayload.NEGOTIATE
				&& back.beats().equals(state.beats()) && back.terms().equals(state.terms()) && back.patterns().equals(state.patterns())
				&& back.resonance() == 6 && back.offers(PactRequestPayload.ACCEPT) && !back.offers(PactRequestPayload.END)
				&& ItemStack.matches(back.compact().getFirst(), state.compact().getFirst())
				&& back.result().orElseThrow().equals(state.result().orElseThrow()) && buffer.readableBytes() == 0,
				"A state reads back as written");
		RegistryFriendlyByteBuf request = new RegistryFriendlyByteBuf(Unpooled.buffer(), registries);
		PactRequestPayload asked = new PactRequestPayload(7, 9, PactRequestPayload.RECALL, 1, 3);
		PactRequestPayload.CODEC.encode(request, asked);
		helper.assertTrue(PactRequestPayload.CODEC.decode(request).equals(asked) && request.readableBytes() == 0,
				"A request is its five numbers and nothing more");
		helper.assertTrue(PactRequestPayload.ACTIONS.size() == 9 && "seat".equals(PactRequestPayload.ACTIONS.get(PactRequestPayload.SEAT))
				&& PactStatePayload.TERMS.size() == 14, "The actions keep their indexes; the terms page has its fourteen terms");
		boolean refused = false;
		try {
			PactStatePayload.CODEC.encode(new RegistryFriendlyByteBuf(Unpooled.buffer(), registries), new PactStatePayload(7, 3,
					PactStatePayload.KEEP, Component.literal("Stolas"), List.of(),
					List.of(new PactStatePayload.Term("k".repeat(PactStatePayload.MAX_KEY + 1), Component.empty())), List.of(), List.of(),
					List.of(), List.of(), 0, 0, Optional.empty()));
		} catch (RuntimeException expected) {
			refused = true;
		}
		helper.assertTrue(refused, "A key past its bound cannot be encoded, so it is never sent");
		PactStatePayload.Choice longId = new PactStatePayload.Choice("jugcraft:" + "o".repeat(200), Component.literal("a long offering"), 3, true);
		RegistryFriendlyByteBuf wide = new RegistryFriendlyByteBuf(Unpooled.buffer(), registries);
		PactStatePayload.CODEC.encode(wide, new PactStatePayload(7, 3, PactStatePayload.KEEP, Component.literal("Stolas"), List.of(), List.of(),
				List.of(), List.of(longId), List.of(longId), List.of(), 0, 0, Optional.empty()));
		PactStatePayload read = PactStatePayload.CODEC.decode(wide);
		helper.assertTrue(read.patterns().equals(List.of(longId)) && read.offerings().equals(List.of(longId)) && wide.readableBytes() == 0,
				"A choice's id longer than a key, as data may name one, is sent and read back");
		helper.succeed();
	}

	// ---------------------------------------------------------------- Concordance Chunk D, D-05: Star-reckoning, the first obligation and keeping

	/** D-05 (D-S12): an observation at an observatory while the pact is active keeps its first obligation: pact_service, once, which masters Stolas's Regard; nothing before a pact, while it is suspended, while its perch, broken with the Concordance off, keeps nothing of it, again, or for a second pact. */
	@GameTest(maxTicks = 20)
	public void theObligationMastersTheEntryOnce(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		ServerPlayer keeper = regarded(helper);
		ObservatoryBlockEntity observatory = observatory(helper, new BlockPos(0, 1, 6));
		long g0 = level.getGameTime() + 1_000_000L;
		helper.assertTrue(observatory.observe(keeper, level, NIGHT_ZERO, g0, ONLY_MOON) == 1 && regard(keeper) == ResearchState.UNDERSTOOD && served(keeper) == 0,
				"Before any pact, an observation keeps nothing");
		PactPerchBlockEntity perch = stolasPact(helper, keeper, new BlockPos(3, 1, 3), g0);
		helper.assertValueEqual(PactKeeping.suspend(level, keeper, perch.holder(), g0), AgreementHolder.Toggle.SUSPENDED, "Its holder suspends the pact");
		helper.assertTrue(observatory.observe(keeper, level, NIGHT_ZERO, g0 + 20, ONLY_MOON) == 1 && regard(keeper) == ResearchState.UNDERSTOOD && served(keeper) == 0,
				"Suspended, an observation keeps nothing");
		helper.assertValueEqual(PactKeeping.resume(level, keeper, perch.holder(), g0 + 40), AgreementHolder.Toggle.RESUMED, "and resumes it");
		UUID pact = perch.holder().agreement().record();
		off(() -> helper.setBlock(new BlockPos(3, 1, 3), Blocks.AIR));
		BoundWill left = BoundWills.of(level.getServer()).ledger().get(pact);
		helper.assertTrue(Pacts.serving(left) && AgreementHolder.stranded(level.getServer(), left),
				"Broken while switched off, its record still names the vanished place, which keeps nothing of it");
		helper.assertTrue(observatory.observe(keeper, level, NIGHT_ZERO, g0 + 50, ONLY_MOON) == 1 && regard(keeper) == ResearchState.UNDERSTOOD && served(keeper) == 0,
				"Its perch broken while the Concordance was off, the pact keeps nothing there: an observation keeps nothing");
		PactPerchBlockEntity seated = perch(helper, new BlockPos(6, 1, 3), keeper);
		beside(helper, keeper, new BlockPos(6, 1, 3));
		helper.assertValueEqual(seated.holder().seat(level, keeper, StolasEntity.COUNTERPART, g0 + 55), AgreementHolder.Seat.SEATED, "Seated at a new perch");
		helper.assertTrue(observatory.observe(keeper, level, NIGHT_ZERO, g0 + 60, ONLY_MOON) == 1 && regard(keeper) == ResearchState.MASTERED && served(keeper) == 1,
				"Active, the observation keeps the first obligation: Stolas's Regard is mastered");
		helper.assertTrue(ConcordanceProgress.knowledge(keeper).progress(StolasRegard.RESEARCH).evidence().containsKey(StarReckoning.OBLIGATION_KEY)
				&& StarReckoning.kept(keeper) && earned(keeper, ConcordanceProgress.advancementFor(StolasRegard.RESEARCH, ResearchState.MASTERED)),
				"recorded as the practice jugcraft:pact_service of jugcraft:stolas, with its advancement");
		helper.assertTrue(observatory.observe(keeper, level, NIGHT_ZERO + 8 * Pacts.DAY, g0 + 80, ONLY_MOON) == 1 && served(keeper) == 1,
				"A second observation records nothing more: the Conclave hears it once");
		helper.assertValueEqual(seated.holder().end(level, keeper, g0 + 100), AgreementHolder.Ended.ENDED, "Ended");
		stolasPact(helper, keeper, new BlockPos(5, 1, 5), g0 + 120);
		helper.assertTrue(observatory.observe(keeper, level, NIGHT_ZERO, g0 + 140, ONLY_MOON) == 1 && served(keeper) == 1, "nor for a second pact");
		helper.succeed();
	}

	/**
	 * D-05 (D-S1, D-S9): suspended by its holder or for upkeep, the pact recalls nothing and spends nothing; suspend and resume
	 * are idempotent; the time suspended is not owed; only an offering lifts an upkeep hold.
	 */
	@GameTest(maxTicks = 20)
	public void aSuspendedPactRecallsNothing(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		ServerPlayer keeper = regarded(helper);
		long g0 = dayStart(level, 100);
		PactPerchBlockEntity perch = stolasPact(helper, keeper, new BlockPos(3, 1, 3), g0);
		observed(level, keeper, FULL_MOON, HARVEST_MOON);
		ItemStack held = holding(keeper, AstrolabeItem.CAPACITY);
		helper.assertValueEqual(PactKeeping.suspend(level, keeper, perch.holder(), g0), AgreementHolder.Toggle.SUSPENDED, "Suspended");
		long since = pactOf(level, perch).pact().holdSince();
		helper.assertTrue(PactKeeping.suspend(level, keeper, perch.holder(), g0 + 10) == AgreementHolder.Toggle.SUSPENDED && pactOf(level, perch).pact().holdSince() == since,
				"Suspending again changes nothing");
		helper.assertTrue(reckon(level, keeper, perch, FULL_MOON, Pacts.DAY + 6000, g0) == Pacts.Reckoning.SUSPENDED && AstrolabeItem.charge(held) == AstrolabeItem.CAPACITY
				&& !recalledEver(level, keeper, FULL_MOON) && done(level, perch, g0) == 0, "Suspended, Stolas reckons nothing and nothing is spent");
		helper.assertValueEqual(stolas(level, perch).think(level, NOON, g0), Status.SUSPENDED, "and he says so");
		helper.assertTrue(PactKeeping.resume(level, keeper, perch.holder(), g0 + 20) == AgreementHolder.Toggle.RESUMED
				&& PactKeeping.resume(level, keeper, perch.holder(), g0 + 30) == AgreementHolder.Toggle.RESUMED, "Resumed, and resuming again is no toggle");
		long due = pactOf(level, perch).pact().upkeep().due();
		helper.assertValueEqual(due, g0 + PERIOD_7 + 20, "the time suspended is not owed: the first offering falls due that much later");
		helper.assertValueEqual(reckon(level, keeper, perch, FULL_MOON, Pacts.DAY + 6000, g0 + 40), Pacts.Reckoning.RECALLED, "Active, he reckons");
		helper.assertTrue(stay(level, perch.holder(), g0 + 1_000L, due - 1_000L).isEmpty() && perch.holder().look(level, due) == Pacts.Event.WARNED
				&& stay(level, perch.holder(), due + 1_000L, due + Pacts.DAY - 1_000L).isEmpty()
				&& perch.holder().look(level, due + Pacts.DAY) == Pacts.Event.SUSPENDED, "Unpaid past its grace, it is held for its upkeep");
		long h = due + Pacts.DAY;
		int charge = AstrolabeItem.charge(held);
		helper.assertTrue(reckon(level, keeper, perch, HARVEST_MOON, h, h) == Pacts.Reckoning.UPKEEP_HELD && AstrolabeItem.charge(held) == charge,
				"Held for upkeep, he reckons nothing");
		helper.assertTrue(PactKeeping.suspend(level, keeper, perch.holder(), h) == AgreementHolder.Toggle.HELD_FOR_UPKEEP
				&& PactKeeping.resume(level, keeper, perch.holder(), h) == AgreementHolder.Toggle.HELD_FOR_UPKEEP, "and the holder's word neither adds to nor lifts it");
		give(keeper, new ItemStack(Items.AMETHYST_SHARD, 1));
		helper.assertTrue(PactKeeping.deposit(level, keeper, perch.holder(), Items.AMETHYST_SHARD, h + 500L) == 1 && perch.holder().held(level) == null
				&& reckon(level, keeper, perch, HARVEST_MOON, h + 600L, h + 600L) == Pacts.Reckoning.RECALLED, "An offering resumes it, and he reckons again");
		helper.succeed();
	}

	/** D-05 (D-S1): outside its hours or area (or in another level), the day's quota spent, no astrolabe or too little resonance: each refused with its reason, nothing spent; a second astrolabe holding enough pays. */
	@GameTest(maxTicks = 20)
	public void theQuotaAndTheAreaRefuseWithReasons(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		ServerPlayer keeper = regarded(helper);
		BlockPos at = new BlockPos(3, 1, 3);
		long g0 = dayStart(level, 50);
		PactPerchBlockEntity perch = perch(helper, at, keeper);
		beside(helper, keeper, at);
		BoundWill.Service night = new BoundWill.Service(StarReckoning.WORK, 8, 13000, 23000, 1);
		helper.assertValueEqual(perch.holder().seal(level, keeper, withService(stolasTerms(), night), g0).outcome(), Pacts.Outcome.DONE,
				"A pact whose hours are the night (test terms; Stolas's own are any hour)");
		observed(level, keeper, FULL_MOON, LANTERN_STAR);
		ItemStack held = holding(keeper, AstrolabeItem.CAPACITY);
		long noon = Pacts.DAY + 6000;
		long midnight = Pacts.DAY + 18000;
		helper.assertTrue(reckon(level, keeper, perch, FULL_MOON, noon, g0) == Pacts.Reckoning.OUTSIDE_HOURS && AstrolabeItem.charge(held) == AstrolabeItem.CAPACITY,
				"Outside its hours: refused, nothing spent");
		helper.assertValueEqual(key(StarReckoning.told(Pacts.Reckoning.OUTSIDE_HOURS, null, night)), "message.jugcraft.concordance.pact.reckoning.outside_hours", "and told so");
		place(helper, keeper, at.getX() + 12.5, at.getY(), at.getZ() + 0.5);
		helper.assertValueEqual(reckon(level, keeper, perch, FULL_MOON, midnight, g0), Pacts.Reckoning.OUTSIDE_AREA, "Twelve blocks off: outside its area");
		helper.assertValueEqual(Pacts.serves(pactOf(level, perch), new Place("minecraft:the_nether", 0, 0, 0), midnight, g0), Pacts.Reckoning.OUTSIDE_AREA,
				"In another level: outside its area");
		beside(helper, keeper, at);
		helper.assertTrue(reckon(level, keeper, perch, FULL_MOON, midnight, g0) == Pacts.Reckoning.RECALLED && done(level, perch, g0) == 1, "At the perch at night: recalled");
		int after = AstrolabeItem.charge(held);
		helper.assertTrue(reckon(level, keeper, perch, LANTERN_STAR, midnight + 1000, g0 + 1000) == Pacts.Reckoning.QUOTA_SPENT
				&& reckon(level, keeper, perch, LANTERN_STAR, midnight + 8 * Pacts.DAY, g0 + 2000) == Pacts.Reckoning.QUOTA_SPENT
				&& AstrolabeItem.charge(held) == after, "The day's quota spent, whatever the clock says");
		long g1 = g0 + Pacts.DAY;
		keeper.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
		helper.assertTrue(reckon(level, keeper, perch, LANTERN_STAR, midnight + Pacts.DAY, g1) == Pacts.Reckoning.NO_ASTROLABE && done(level, perch, g1) == 0,
				"The next game day, no astrolabe: refused");
		ItemStack poor = holding(keeper, 5);
		helper.assertTrue(reckon(level, keeper, perch, LANTERN_STAR, midnight + Pacts.DAY, g1) == Pacts.Reckoning.TOO_LITTLE && AstrolabeItem.charge(poor) == 5,
				"Too little resonance for its recall: refused");
		ItemStack rich = astrolabe(AstrolabeItem.CAPACITY);
		keeper.getInventory().setItem(9, rich);
		helper.assertTrue(reckon(level, keeper, perch, LANTERN_STAR, midnight + Pacts.DAY, g1) == Pacts.Reckoning.RECALLED && AstrolabeItem.charge(poor) == 5
				&& AstrolabeItem.charge(rich) == AstrolabeItem.CAPACITY - Sky.catalog().pattern(LANTERN_STAR).recallCost(), "The astrolabe that holds enough pays");
		helper.succeed();
	}

	/** D-05 (E2, D-S3): with the absent-owner option on, a stand-in answers protection questions for the holder but never for upkeep; it pays, reckons, ends and suspends nothing. */
	@GameTest(maxTicks = 20)
	public void aStandInNeverAnswersForUpkeep(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		ServerPlayer keeper = regarded(helper);
		UUID id = keeper.getUUID();
		BlockPos at = new BlockPos(3, 1, 3);
		long s0 = level.getGameTime() + 1_000_000L;
		PactPerchBlockEntity perch = stolasPact(helper, keeper, at, s0);
		UUID record = perch.holder().agreement().record();
		give(keeper, new ItemStack(Items.AMETHYST_SHARD, 2));
		helper.assertValueEqual(PactKeeping.deposit(level, keeper, perch.holder(), Items.AMETHYST_SHARD, s0), 2, "Two periods paid ahead");
		BoundWill.Upkeep before = pactOf(level, perch).pact().upkeep();
		TestPlayers.leave(keeper);
		boolean was = JugcraftConfig.option(Authority.ABSENT_OPTION);
		JugcraftConfig.setOption(Authority.ABSENT_OPTION, true);
		try {
			ServerPlayer answering = Authority.answering(level, id);
			helper.assertTrue(answering != null && Authority.isStandIn(answering), "A stand-in answers protection questions for the absent holder");
			for (long t = s0 + Pacts.DAY; t <= s0 + 3 * PERIOD_7; t += Pacts.DAY) {
				helper.assertValueEqual(perch.holder().look(level, t), Pacts.Event.NONE, "but never for upkeep");
				PactKeeping.tick(level, perch.holder(), perch.getBlockPos(), t);
			}
			helper.assertTrue(escrowed(level, record) == 2 && pactOf(level, perch).pact().upkeep().equals(before), "Nothing is taken, warned or moved");
			ItemStack offered = new ItemStack(Items.AMETHYST_SHARD, 1);
			helper.assertTrue(perch.holder().deposit(level, answering, offered, s0) == 0 && offered.getCount() == 1 && escrowed(level, record) == 2
					&& reckon(level, answering, perch, FULL_MOON, Pacts.DAY + 6000, s0 + 100) == Pacts.Reckoning.NOT_HOLDER
					&& perch.holder().end(level, answering, s0 + 100) == AgreementHolder.Ended.NOT_PERMITTED
					&& PactKeeping.suspend(level, answering, perch.holder(), s0 + 100) == AgreementHolder.Toggle.NOT_HOLDER,
					"and it pays (a real offering in hand), reckons, ends and suspends nothing for them");
		} finally {
			JugcraftConfig.setOption(Authority.ABSENT_OPTION, was);
		}
		ServerPlayer back = TestPlayers.again(helper, id);
		beside(helper, back, at);
		long tr = s0 + 3 * PERIOD_7 + 500L;
		helper.assertTrue(perch.holder().look(level, tr) == Pacts.Event.PAID && escrowed(level, record) == 1, "Back in person, one period falls due: one offering");
		helper.succeed();
	}

	/** D-05 (E4, D-S6): its living holder ends the pact: what was paid ahead comes back once (into the inventory, the rest at their feet); Stolas departs and is struck off; who, why and how many told; the reason and place kept once; ending again returns nothing; the perch stays. */
	@GameTest(maxTicks = 20)
	public void endingReturnsTheEscrowOnce(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		ServerPlayer keeper = regarded(helper);
		BlockPos at = new BlockPos(3, 1, 3);
		long g0 = level.getGameTime() + 1_000_000L;
		PactPerchBlockEntity perch = stolasPact(helper, keeper, at, g0);
		UUID record = perch.holder().agreement().record();
		StolasEntity stolas = stolas(level, perch);
		give(keeper, new ItemStack(Items.AMETHYST_SHARD, 3));
		give(keeper, new ItemStack(starflower(), 1));
		helper.assertTrue(PactKeeping.deposit(level, keeper, perch.holder(), Items.AMETHYST_SHARD, g0) == 3
				&& PactKeeping.deposit(level, keeper, perch.holder(), starflower(), g0) == 1 && escrowed(level, record) == 4, "Four offerings paid ahead, the most it holds");
		Inventory inventory = keeper.getInventory();
		for (int slot = 0; slot < Inventory.INVENTORY_SIZE; slot++) {
			inventory.setItem(slot, slot == 0 ? new ItemStack(Items.AMETHYST_SHARD, 63) : new ItemStack(Items.STONE, 64));
		}
		Component said = PactKeeping.end(level, keeper, perch.holder(), StolasEntity.COUNTERPART, g0 + 100).said();
		helper.assertTrue(key(said).equals("message.jugcraft.concordance.pact.departed") && Integer.valueOf(4).equals(arg(said, 2)),
				"Its holder ends it, told who departs, why and what came back: " + said);
		AABB feet = keeper.getBoundingBox().inflate(3.0);
		helper.assertTrue(inventory.countItem(Items.AMETHYST_SHARD) == 64 && looseNear(level, feet, Items.AMETHYST_SHARD) == 2 && looseNear(level, feet, starflower()) == 1,
				"What was paid ahead comes back once: what fits into the inventory, the rest at their feet");
		BoundWills wills = BoundWills.of(level.getServer());
		helper.assertTrue(wills.ledger().get(record) == null && wills.escrow(record).isEmpty() && stolas.gone()
				&& !WorkerRoster.of(level.getServer()).workers(keeper.getUUID()).containsKey(stolas.getUUID()) && perch.holder().agreement() == null,
				"The record and its escrow go; Stolas departs and is struck off");
		List<Pacts.Departure> history = PactHistory.of(level.getServer()).history(keeper.getUUID());
		helper.assertTrue(history.size() == 1 && history.getFirst().record().equals(record) && Pacts.ENDED.equals(history.getFirst().reason())
				&& Couriers.place(level, helper.absolutePos(at)).equals(history.getFirst().place()), "The departure is kept once, with its reason and place: " + history);
		helper.assertTrue(perch.holder().end(level, keeper, g0 + 200) == AgreementHolder.Ended.NOTHING
				&& AgreementHolder.end(level, null, keeper, record, Pacts.ENDED, g0 + 200) == AgreementHolder.Ended.NOTHING
				&& inventory.countItem(Items.AMETHYST_SHARD) == 64 && looseNear(level, feet, Items.AMETHYST_SHARD) == 2
				&& PactHistory.of(level.getServer()).history(keeper.getUUID()).size() == 1, "Ending again returns nothing more");
		helper.assertTrue(helper.getBlockState(at).is(Workers.PACT_PERCH), "and the perch stays");
		helper.succeed();
	}

	/** D-05 (E4, D-S6): a holder who is not alive is never handed what was paid ahead, at the perch or by record; it stays with the record until they live. */
	@GameTest(maxTicks = 20)
	public void aDeadHolderIsNeverHandedTheEscrow(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		ServerPlayer keeper = regarded(helper);
		long g0 = level.getGameTime() + 1_000_000L;
		PactPerchBlockEntity perch = stolasPact(helper, keeper, new BlockPos(3, 1, 3), g0);
		UUID record = perch.holder().agreement().record();
		give(keeper, new ItemStack(Items.AMETHYST_SHARD, 2));
		helper.assertValueEqual(PactKeeping.deposit(level, keeper, perch.holder(), Items.AMETHYST_SHARD, g0), 2, "Two paid ahead");
		keeper.setHealth(0.0F);
		helper.assertTrue(key(PactKeeping.end(level, keeper, perch.holder(), StolasEntity.COUNTERPART, g0).said()).equals("message.jugcraft.concordance.workers.end_not_permitted")
				&& AgreementHolder.end(level, null, keeper, record, Pacts.ENDED, g0) == AgreementHolder.Ended.NOT_PERMITTED, "Not alive, they cannot end it, here or by record");
		helper.assertTrue(shards(keeper) == 0 && escrowed(level, record) == 2 && BoundWills.of(level.getServer()).ledger().get(record) != null
				&& PactHistory.of(level.getServer()).history(keeper.getUUID()).isEmpty(), "what was paid ahead stays with the record");
		keeper.setHealth(keeper.getMaxHealth());
		helper.assertTrue(perch.holder().end(level, keeper, g0) == AgreementHolder.Ended.ENDED && shards(keeper) == 2, "Alive again, they end it and it comes back");
		helper.succeed();
	}

	/** D-05 (D-S6, E4): breaking the perch ends nothing: the pact is without its perch, its holder, terms and escrow kept, no departure; Stolas leaves and is struck off; nothing is reckoned or kept as an obligation; ended from the roster, the escrow comes back once. */
	@GameTest(maxTicks = 20)
	public void aBrokenPerchLeavesThePactWithoutItsPerch(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		ServerPlayer keeper = regarded(helper);
		BlockPos at = new BlockPos(2, 1, 2);
		long g0 = dayStart(level, 100);
		PactPerchBlockEntity perch = stolasPact(helper, keeper, at, g0);
		UUID record = perch.holder().agreement().record();
		give(keeper, new ItemStack(Items.AMETHYST_SHARD, 2));
		helper.assertValueEqual(PactKeeping.deposit(level, keeper, perch.holder(), Items.AMETHYST_SHARD, g0), 2, "Two paid ahead");
		observed(level, keeper, FULL_MOON);
		holding(keeper, AstrolabeItem.CAPACITY);
		StolasEntity stolas = stolas(level, perch);
		BoundWill before = pactOf(level, perch);
		helper.setBlock(at, Blocks.AIR);
		BoundWill kept = BoundWills.of(level.getServer()).ledger().get(record);
		helper.assertTrue(kept != null && keeper.getUUID().equals(kept.holder()) && !Pacts.seated(kept) && kept.pact().presence() == null
				&& kept.pact().upkeep().equals(before.pact().upkeep()) && kept.pact().service().equals(before.pact().service()) && escrowed(level, record) == 2,
				"Broken, the perch ends nothing: the pact is without its perch, its terms and escrow kept");
		helper.assertTrue(stolas.gone() && !WorkerRoster.of(level.getServer()).workers(keeper.getUUID()).containsKey(stolas.getUUID())
				&& looseNear(level, new AABB(helper.absolutePos(at)).inflate(2.0), Items.AMETHYST_SHARD) == 0
				&& PactHistory.of(level.getServer()).history(keeper.getUUID()).isEmpty(), "Stolas leaves the world; nothing drops; nothing departed");
		PactPerchBlockEntity other = perch(helper, new BlockPos(5, 1, 5), keeper);
		beside(helper, keeper, new BlockPos(5, 1, 5));
		helper.assertValueEqual(StarReckoning.reckon(level, keeper, other.holder(), FULL_MOON, Pacts.DAY + 6000, g0), Pacts.Reckoning.NOTHING,
				"Without its perch, Stolas reckons nowhere");
		ObservatoryBlockEntity observatory = observatory(helper, new BlockPos(0, 1, 6));
		helper.assertTrue(observatory.observe(keeper, level, NIGHT_ZERO, g0 + 20, ONLY_MOON) == 1 && regard(keeper) == ResearchState.UNDERSTOOD,
				"and an observation keeps no obligation");
		helper.assertTrue(AgreementHolder.end(level, null, keeper, record, Pacts.ENDED, g0 + 40) == AgreementHolder.Ended.ENDED && shards(keeper) == 2
				&& PactHistory.of(level.getServer()).history(keeper.getUUID()).getFirst().place() == null, "Ended from the roster, the escrow comes back once");
		helper.succeed();
	}

	/** D-05 (D-S6, D-S8): a pact without its perch is seated again only by its holder at a perch they own: the same record, terms, today's count, upkeep clock and escrow, Stolas called back at no cost; the time it waited is not owed; one broken while switched off is seated too, through the perch's screen. */
	@GameTest(maxTicks = 20)
	public void aPactIsSeatedAgainAtANewPerch(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		ServerPlayer keeper = regarded(helper);
		ServerPlayer stranger = regarded(helper);
		long g0 = dayStart(level, 100);
		PactPerchBlockEntity first = stolasPact(helper, keeper, new BlockPos(2, 1, 2), g0);
		UUID record = first.holder().agreement().record();
		observed(level, keeper, FULL_MOON, LANTERN_STAR);
		holding(keeper, AstrolabeItem.CAPACITY);
		helper.assertValueEqual(reckon(level, keeper, first, FULL_MOON, Pacts.DAY + 6000, g0), Pacts.Reckoning.RECALLED, "Today's reckoning made");
		give(keeper, new ItemStack(Items.AMETHYST_SHARD, 2));
		helper.assertValueEqual(PactKeeping.deposit(level, keeper, first.holder(), Items.AMETHYST_SHARD, g0), 2, "Two paid ahead");
		BoundWill before = pactOf(level, first);
		helper.setBlock(new BlockPos(2, 1, 2), Blocks.AIR);
		PactPerchBlockEntity theirs = perch(helper, new BlockPos(5, 1, 2), stranger);
		beside(helper, keeper, new BlockPos(5, 1, 2));
		helper.assertTrue(theirs.holder().seat(level, keeper, StolasEntity.COUNTERPART, g0 + 100) == AgreementHolder.Seat.NOT_PERMITTED
				&& theirs.holder().seat(level, stranger, StolasEntity.COUNTERPART, g0 + 100) == AgreementHolder.Seat.NOTHING && theirs.holder().agreement() == null,
				"Not at another's perch; and its owner has no pact to seat");
		PactPerchBlockEntity mine = perch(helper, new BlockPos(5, 1, 5), keeper);
		beside(helper, keeper, new BlockPos(5, 1, 5));
		helper.assertValueEqual(mine.holder().seat(level, FakePlayer.get(level, new GameProfile(keeper.getUUID(), "[Concordance]")), StolasEntity.COUNTERPART, g0 + 150),
				AgreementHolder.Seat.NOT_HOLDER, "Never by a stand-in");
		helper.assertValueEqual(mine.holder().seat(level, keeper, StolasEntity.COUNTERPART, g0 + 200), AgreementHolder.Seat.SEATED, "Its holder seats it at a perch they own");
		BoundWill after = BoundWills.of(level.getServer()).ledger().get(record);
		helper.assertTrue(after != null && Couriers.place(level, helper.absolutePos(new BlockPos(5, 1, 5))).equals(after.pact().place())
				&& after.pact().presence().equals(mine.holder().spirit()) && stolas(level, mine) != null
				&& WorkerRoster.of(level.getServer()).workers(keeper.getUUID()).containsKey(mine.holder().spirit()), "the same record, Stolas called back and on the roster");
		helper.assertTrue(after.pact().service().equals(before.pact().service()) && after.pact().upkeep().equals(before.pact().upkeep())
				&& after.pact().window() == before.pact().window() && after.pact().doneToday() == before.pact().doneToday() && escrowed(level, record) == 2
				&& mine.holder().agreement().record().equals(record), "with its terms, today's count, upkeep clock and escrow");
		helper.assertValueEqual(reckon(level, keeper, mine, LANTERN_STAR, Pacts.DAY + 7000, g0 + 300), Pacts.Reckoning.QUOTA_SPENT, "Today's quota came with it");
		helper.assertValueEqual(perch(helper, new BlockPos(2, 1, 5), keeper).holder().seat(level, keeper, StolasEntity.COUNTERPART, g0 + 400), AgreementHolder.Seat.NOTHING,
				"Seated, it is seated nowhere else");
		long back = g0 + 10 * Pacts.DAY;
		helper.assertTrue(mine.holder().look(level, back) == Pacts.Event.PAID && escrowed(level, record) == 1
				&& pactOf(level, mine).pact().upkeep().due() == back + PERIOD_7, "The time it waited is not owed: one offering, the next a week on");
		BlockPos mineAt = new BlockPos(5, 1, 5);
		off(() -> helper.setBlock(mineAt, Blocks.AIR));
		helper.assertTrue(Pacts.seated(BoundWills.of(level.getServer()).ledger().get(record)), "Broken while switched off, its record still names the vanished place");
		PactPerchBlockEntity again = perch(helper, new BlockPos(6, 1, 2), keeper);
		beside(helper, keeper, new BlockPos(6, 1, 2));
		PactMenu menu = open(helper, keeper, again);
		helper.assertTrue(menu.lastSent().mode() == PactStatePayload.SEAT && menu.lastSent().offers(PactRequestPayload.SEAT)
				&& menu.lastSent().offers(PactRequestPayload.END) && !menu.lastSent().offers(PactRequestPayload.ACCEPT),
				"At a perch of theirs keeping nothing, the screen offers to seat the pact here, or to end it");
		PactStatePayload seated = send(keeper, menu, PactRequestPayload.SEAT, 0);
		helper.assertTrue(answered(seated, AgreementHolder.Seat.SEATED.id) && seated.result().get().done() && seated.mode() == PactStatePayload.KEEP
				&& again.holder().agreement() != null && again.holder().agreement().record().equals(record) && BoundWills.of(level.getServer()).problems().isEmpty(),
				"yet, its old place keeping nothing of it, it is seated again through the perch's screen");
		keeper.closeContainer();
		helper.succeed();
	}

	/** D-05 (D-S7): a lost Stolas never ends the pact: the perch calls him back at no cost while his holder is near and the Concordance on; not when they are far or it is off. */
	@GameTest(maxTicks = 20)
	public void aLostPresenceIsCalledBackAndEndsNothing(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		ServerPlayer keeper = regarded(helper);
		BlockPos at = new BlockPos(3, 1, 3);
		long g0 = level.getGameTime() + 1_000_000L;
		PactPerchBlockEntity perch = stolasPact(helper, keeper, at, g0);
		UUID record = perch.holder().agreement().record();
		give(keeper, new ItemStack(Items.AMETHYST_SHARD, 1));
		PactKeeping.deposit(level, keeper, perch.holder(), Items.AMETHYST_SHARD, g0);
		StolasEntity first = stolas(level, perch);
		first.remove(Entity.RemovalReason.KILLED);
		helper.assertTrue(first.isRemoved() && perch.holder().spiritLost(level) && BoundWills.of(level.getServer()).ledger().get(record) != null
				&& escrowed(level, record) == 1 && PactHistory.of(level.getServer()).history(keeper.getUUID()).isEmpty(), "Lost, he ends nothing");
		PactKeeping.tick(level, perch.holder(), perch.getBlockPos(), g0 + 20);
		StolasEntity second = stolas(level, perch);
		helper.assertTrue(second != null && !second.getUUID().equals(first.getUUID()) && second.getUUID().equals(pactOf(level, perch).pact().presence())
				&& WorkerRoster.of(level.getServer()).workers(keeper.getUUID()).containsKey(second.getUUID()) && escrowed(level, record) == 1 && shards(keeper) == 0,
				"His holder near, the perch calls him back under the same record, at no cost");
		second.remove(Entity.RemovalReason.KILLED);
		place(helper, keeper, at.getX() + 100.5, at.getY(), at.getZ() + 0.5);
		PactKeeping.tick(level, perch.holder(), perch.getBlockPos(), g0 + 40);
		helper.assertTrue(stolas(level, perch) == null && perch.holder().spiritLost(level), "His holder far away, nobody is called");
		beside(helper, keeper, at);
		off(() -> PactKeeping.tick(level, perch.holder(), perch.getBlockPos(), g0 + 60));
		helper.assertTrue(stolas(level, perch) == null, "Switched off, nobody is called");
		PactKeeping.tick(level, perch.holder(), perch.getBlockPos(), g0 + 80);
		helper.assertTrue(stolas(level, perch) != null && BoundWills.of(level.getServer()).ledger().get(record) != null
				&& PactHistory.of(level.getServer()).history(keeper.getUUID()).isEmpty(), "Back near it, he returns; the pact never ended");
		helper.succeed();
	}

	// ---------------------------------------------------------------- Concordance Chunk D, D-06: Stolas's body and his leaving

	/**
	 * D-06 (D-S7): leaving, Stolas makes his departing gesture and is gone when it ends: struck off at once, never saved
	 * meanwhile, never taken for the perch's visitor or presence again; a new invitation summons a new Stolas. A declined
	 * visitor and an ended pact's presence alike. He listens while the screen is open; nothing of this is saved.
	 */
	@GameTest(maxTicks = 80)
	public void aDepartingStolasIsNeverAVisitorAgain(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		long now = level.getGameTime();
		ServerPlayer guest = ready(level, TestPlayers.mock(helper));
		BlockPos at = new BlockPos(1, 1, 1);
		PactPerchBlockEntity perch = perch(helper, at, guest);
		beside(helper, guest, at);
		PactMenu menu = open(helper, guest, perch);
		StolasEntity visitor = (StolasEntity) level.getEntity(perch.visitor());
		helper.assertTrue(visitor != null && visitor.think(level, 0L, now) == Status.IDLE && visitor.listening() && !visitor.gone()
				&& visitor.equals(perch.stolas(level)), "Invited, he waits at the perch, listening");
		send(guest, menu, PactRequestPayload.DECLINE, 0);
		helper.assertTrue(visitor.think(level, 0L, now) == Status.FINISHED && visitor.departing() && !visitor.isRemoved() && visitor.gone()
				&& !visitor.shouldBeSaved() && perch.stolas(level) == null && "depart".equals(visitor.cues().getLast()),
				"Declined, he departs: gone for the perch, not saved, still in the world for his gesture, its sound with it: " + visitor.cues());
		helper.assertTrue(!visitor.listening() && visitor.leaving(),
				"Leaving, he listens no more, and his departure is synced: one who starts watching him now sees him gone (the unit review)");
		helper.assertTrue(visitor.think(level, 0L, now) == Status.FINISHED && visitor.departing(), "Thinking again changes nothing");
		open(helper, guest, perch);
		StolasEntity second = (StolasEntity) level.getEntity(perch.visitor());
		helper.assertTrue(second != null && !second.equals(visitor) && !second.gone() && second.equals(perch.stolas(level))
				&& "arrive".equals(second.cues().getLast()), "A new invitation summons a new Stolas, never the departing one; he alights");
		ServerPlayer keeper = ready(level, TestPlayers.survivor(helper));
		PactPerchBlockEntity home = stolasPact(helper, keeper, new BlockPos(5, 1, 5), now);
		StolasEntity presence = stolas(level, home);
		TagValueOutput out = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, level.registryAccess());
		presence.saveWithoutId(out);
		CompoundTag body = out.buildResult();
		helper.assertTrue(!body.contains("listening") && !body.contains("departing") && !body.contains("cues"), "Nothing of his presentation is saved: " + body);
		helper.assertTrue(PactKeeping.end(level, keeper, home.holder(), StolasEntity.COUNTERPART, now).ended() == AgreementHolder.Ended.ENDED
				&& presence.departing() && !presence.isRemoved() && !presence.shouldBeSaved() && home.stolas(level) == null
				&& !WorkerRoster.of(level.getServer()).workers(keeper.getUUID()).containsKey(presence.getUUID())
				&& "depart".equals(presence.cues().getLast()), "Ended, his presence departs at once: struck off, never saved; heard to go");
		helper.runAfterDelay(StolasEntity.Gesture.DEPART.ticks + 2, () -> {
			helper.assertTrue(visitor.isRemoved() && presence.isRemoved() && !second.isRemoved(),
					"Their departures end with them gone; the new visitor, its screen still open, stays");
			helper.succeed();
		});
	}

	/**
	 * D-06 (the unit review): a departure ends on time where his entity does not tick (his chunk tracked but not ticking, or
	 * unloaded around him, since a departing Stolas is never saved): the server's sweep discards him when it ends, never
	 * before. The sweep runs here at explicit game times, with no tick between.
	 */
	@GameTest(maxTicks = 20)
	public void aDepartureEndsWithoutHisTicks(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		ServerPlayer guest = ready(level, TestPlayers.mock(helper));
		BlockPos at = new BlockPos(1, 1, 1);
		PactPerchBlockEntity perch = perch(helper, at, guest);
		beside(helper, guest, at);
		open(helper, guest, perch);
		StolasEntity visitor = (StolasEntity) level.getEntity(perch.visitor());
		helper.assertTrue(visitor != null && visitor.listening() && !visitor.leaving() && !visitor.departing(), "Invited, he listens at the perch");
		long now = level.getGameTime();
		visitor.leave(level, null, perch.getBlockPos());
		helper.assertTrue(visitor.departing() && visitor.leaving() && !visitor.listening() && !visitor.isRemoved() && !visitor.shouldBeSaved(),
				"Leaving, he listens no more, his departure synced and never saved");
		StolasEntity.sweep(now + StolasEntity.Gesture.DEPART.ticks - 1);
		helper.assertTrue(!visitor.isRemoved(), "The sweep never ends a departure before its gesture's length");
		StolasEntity.sweep(now + StolasEntity.Gesture.DEPART.ticks);
		helper.assertTrue(visitor.isRemoved() && visitor.getRemovalReason() == Entity.RemovalReason.DISCARDED,
				"Its length over, the sweep discards him, whether or not he ticked: " + visitor.getRemovalReason());
		helper.succeed();
	}

	// D-06: his cues (the server fires each with its sound), his one presence a pact, and Jade's readings.

	static int offering(PactStatePayload state, String item) {
		for (int i = 0; i < state.offerings().size(); i++) {
			if (state.offerings().get(i).id().equals(item)) {
				return i;
			}
		}
		return -1;
	}

	/** D-06 (03:159): which gesture answers which action (none for the menu's own answers), which loop each state shows, every cue's sound registered. */
	@GameTest(maxTicks = 20)
	public void stolasGesturesAndSoundsFollowTheirTable(GameTestHelper helper) {
		record Reaction(int action, String reason, boolean done, StolasEntity.@Nullable Gesture gesture) {
		}
		for (Reaction row : List.of(new Reaction(PactRequestPayload.ACCEPT, PactMenu.SEALED, true, StolasEntity.Gesture.ASSENT),
				new Reaction(PactRequestPayload.ACCEPT, Pacts.Outcome.CANNOT_AFFORD.id, false, StolasEntity.Gesture.REFUSE),
				new Reaction(PactRequestPayload.ACCEPT, PactMenu.MISSING_EVIDENCE, false, StolasEntity.Gesture.REFUSE),
				new Reaction(PactRequestPayload.ACCEPT, Pacts.Outcome.TAKEN.id, false, StolasEntity.Gesture.REFUSE),
				new Reaction(PactRequestPayload.ACCEPT, PactMenu.NOT_OFFERED, false, null),
				new Reaction(PactRequestPayload.RECALL, Pacts.Reckoning.RECALLED.id, true, StolasEntity.Gesture.INSPECT),
				new Reaction(PactRequestPayload.RECALL, Pacts.Reckoning.QUOTA_SPENT.id, false, StolasEntity.Gesture.REFUSE),
				new Reaction(PactRequestPayload.RECALL, PactMenu.NOT_OFFERED, false, null),
				new Reaction(PactRequestPayload.DEPOSIT, PactMenu.DEPOSITED, true, StolasEntity.Gesture.INSPECT),
				new Reaction(PactRequestPayload.DEPOSIT, PactMenu.NOTHING_DEPOSITED, false, null),
				new Reaction(PactRequestPayload.RESUME, "resumed", true, StolasEntity.Gesture.ASSENT),
				new Reaction(PactRequestPayload.RESUME, "held_for_upkeep", false, null),
				new Reaction(PactRequestPayload.SUSPEND, "suspended", true, null),
				new Reaction(PactRequestPayload.DECLINE, PactMenu.DECLINED, true, null),
				new Reaction(PactRequestPayload.END, "ended", true, null),
				new Reaction(PactRequestPayload.SEAT, AgreementHolder.Seat.SEATED.id, true, null),
				new Reaction(PactRequestPayload.CHOOSE, "", true, null))) {
			helper.assertTrue(StolasEntity.reaction(row.action(), row.reason(), row.done()) == row.gesture(),
					PactRequestPayload.ACTIONS.get(row.action()) + " answered " + row.reason() + " is answered by " + row.gesture());
		}
		record Shown(Status status, boolean listening, StolasEntity.Loop loop) {
		}
		for (Shown row : List.of(new Shown(Status.IDLE, false, StolasEntity.Loop.IDLE), new Shown(Status.IDLE, true, StolasEntity.Loop.LISTEN),
				new Shown(Status.SUSPENDED, false, StolasEntity.Loop.SUSPENDED), new Shown(Status.SUSPENDED, true, StolasEntity.Loop.LISTEN),
				new Shown(Status.UPKEEP_HELD, false, StolasEntity.Loop.SUSPENDED), new Shown(Status.UPKEEP_DUE, false, StolasEntity.Loop.ASTIR),
				new Shown(Status.OWNER_OFFLINE, false, StolasEntity.Loop.WAITING), new Shown(Status.DESTINATION_UNLOADED, false, StolasEntity.Loop.WAITING),
				new Shown(Status.OUTSIDE_AGREEMENT, false, StolasEntity.Loop.WAITING), new Shown(Status.DISABLED, true, StolasEntity.Loop.WAITING),
				new Shown(Status.FINISHED, false, StolasEntity.Loop.WAITING))) {
			helper.assertTrue(StolasEntity.loop(row.status(), row.listening()) == row.loop(),
					"while " + row.status().id + (row.listening() ? ", listening" : "") + ": " + row.loop());
		}
		for (String id : StolasEntity.SOUND_IDS) {
			helper.assertTrue(BuiltInRegistries.SOUND_EVENT.containsKey(Jugcraft.id("stolas." + id)), "jugcraft:stolas." + id + " is registered");
		}
		for (StolasEntity.Gesture gesture : StolasEntity.Gesture.values()) {
			helper.assertTrue(StolasEntity.SOUND_IDS.contains(gesture.id), gesture.id + " has its sound");
		}
		helper.succeed();
	}

	/** D-06 (03:159, 08:145): each gesture is fired by the server's own action, once, with its sound: he alights as the invitation opens (his listening not cued over it), refuses a refused acceptance, assents to a sealed one, inspects an offering paid ahead (a second inspection in the same moment is not repeated), shows suspension and listening by his loop, and departs when the pact ends. */
	@GameTest(maxTicks = 20)
	public void stolasAnswersEachActionWithItsGesture(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		ServerPlayer owner = ready(level, TestPlayers.survivor(helper));
		BlockPos at = new BlockPos(2, 1, 2);
		PactPerchBlockEntity perch = perch(helper, at, owner);
		beside(helper, owner, at);
		PactMenu menu = open(helper, owner, perch);
		StolasEntity stolas = (StolasEntity) level.getEntity(perch.visitor());
		helper.assertTrue(stolas != null && stolas.cues().equals(List.of("arrive")) && stolas.listening(),
				"The invitation opens: he alights and listens (his loop; no second sound over his arrival): " + (stolas == null ? "none" : stolas.cues()));
		helper.assertTrue(answered(send(owner, menu, PactRequestPayload.ACCEPT, 0), Pacts.Outcome.CANNOT_AFFORD.id) && "refuse".equals(stolas.cues().getLast()),
				"Accepting without the compact: refused, and he refuses: " + stolas.cues());
		pocket(owner, 1, AstrolabeItem.CAPACITY);
		helper.assertTrue(answered(send(owner, menu, PactRequestPayload.ACCEPT, 0), PactMenu.SEALED) && stolas.equals(stolas(level, perch))
				&& "assent".equals(stolas.cues().getLast()), "Sealed: the visitor is the presence, and he assents: " + stolas.cues());
		give(owner, new ItemStack(Items.AMETHYST_SHARD, 3));
		send(owner, menu, PactRequestPayload.CHOOSE, 0);
		helper.assertTrue(answered(send(owner, menu, PactRequestPayload.DEPOSIT, offering(menu.lastSent(), "minecraft:amethyst_shard")), PactMenu.DEPOSITED)
				&& "inspect".equals(stolas.cues().getLast()), "An offering paid ahead: he inspects it: " + stolas.cues());
		int shown = stolas.cues().size();
		give(owner, new ItemStack(Items.AMETHYST_SHARD, 1));
		send(owner, menu, PactRequestPayload.CHOOSE, 0);
		helper.assertTrue(answered(send(owner, menu, PactRequestPayload.DEPOSIT, offering(menu.lastSent(), "minecraft:amethyst_shard")), PactMenu.DEPOSITED)
				&& stolas.cues().size() == shown, "A second offering the same moment: no second inspection (one of a kind every CUE_TICKS)");
		helper.assertTrue(answered(send(owner, menu, PactRequestPayload.SUSPEND, 0), "suspended") && stolas.cues().size() == shown
				&& stolas.think(level, 0L, level.getGameTime()) == Status.SUSPENDED && stolas.listening()
				&& StolasEntity.loop(stolas.status(), stolas.listening()) == StolasEntity.Loop.LISTEN, "Suspended: no gesture; the screen open, he listens");
		owner.closeContainer();
		stolas.think(level, 0L, level.getGameTime());
		helper.assertTrue(!stolas.listening() && StolasEntity.loop(stolas.status(), false) == StolasEntity.Loop.SUSPENDED, "Closed, his loop is the suspended pact's");
		Component said = PactKeeping.end(level, owner, perch.holder(), StolasEntity.COUNTERPART, level.getGameTime()).said();
		helper.assertTrue(key(said).equals(PACT_KEY + "departed") && stolas.departing() && "depart".equals(stolas.cues().getLast()),
				"Ended: told who departs, and he departs: " + stolas.cues());
		helper.succeed();
	}

	/** D-06 (D-S7, E4): Stolas discarded (as a server tool removes an entity) ends nothing: record, terms, escrow and clock stay, nothing departs; his holder away, nobody is called; back near, he returns under the same record, alighting, at no cost. */
	@GameTest(maxTicks = 20)
	public void aRemovedStolasNeverEndsThePact(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		ServerPlayer keeper = ready(level, TestPlayers.survivor(helper));
		BlockPos at = new BlockPos(3, 1, 3);
		long g0 = level.getGameTime();
		PactPerchBlockEntity perch = stolasPact(helper, keeper, at, g0);
		UUID record = perch.holder().agreement().record();
		give(keeper, new ItemStack(Items.AMETHYST_SHARD, 2));
		PactKeeping.deposit(level, keeper, perch.holder(), Items.AMETHYST_SHARD, g0);
		BoundWill before = pactOf(level, perch);
		StolasEntity first = stolas(level, perch);
		first.discard();
		BoundWill kept = BoundWills.of(level.getServer()).ledger().get(record);
		helper.assertTrue(kept != null && kept.pact().upkeep().due() == before.pact().upkeep().due() && kept.pact().service().equals(before.pact().service())
				&& kept.pact().hold() == before.pact().hold() && escrowed(level, record) == 2 && PactHistory.of(level.getServer()).history(keeper.getUUID()).isEmpty()
				&& perch.holder().spiritLost(level), "Discarded, he ends nothing; he is lost");
		place(helper, keeper, at.getX() + 100.5, at.getY(), at.getZ() + 0.5);
		PactKeeping.tick(level, perch.holder(), perch.getBlockPos(), g0 + 20);
		helper.assertTrue(stolas(level, perch) == null, "His holder away, nobody is called");
		beside(helper, keeper, at);
		PactKeeping.tick(level, perch.holder(), perch.getBlockPos(), g0 + 40);
		StolasEntity second = stolas(level, perch);
		helper.assertTrue(second != null && second.getUUID().equals(pactOf(level, perch).pact().presence()) && second.cues().equals(List.of("arrive"))
				&& escrowed(level, record) == 2 && shards(keeper) == 0 && pactOf(level, perch).pact().upkeep().due() == before.pact().upkeep().due(),
				"Back near, he returns under the same record, alighting, at no cost; the clock untouched");
		helper.succeed();
	}

	/** D-06 (D-S7, E5): one presence a pact: looks, openings and a recall call no second while he serves or is merely unloaded; struck off he is called once; the old one loaded again finds another in his place and departs. */
	@GameTest(maxTicks = 20)
	public void onePresencePerPact(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		ServerPlayer keeper = ready(level, TestPlayers.survivor(helper));
		BlockPos at = new BlockPos(3, 1, 3);
		long g0 = level.getGameTime();
		PactPerchBlockEntity perch = stolasPact(helper, keeper, at, g0);
		StolasEntity first = stolas(level, perch);
		for (int i = 1; i <= 3; i++) {
			PactKeeping.tick(level, perch.holder(), perch.getBlockPos(), g0 + 20L * i);
			RateGate.forget(keeper.getUUID());
			PactMenu.open(keeper, level, perch);
			keeper.closeContainer();
		}
		helper.assertTrue(level.getEntitiesOfClass(StolasEntity.class, helper.getBounds(), s -> !s.gone()).size() == 1 && first.equals(stolas(level, perch))
				&& perch.holder().recall(level, keeper, g0 + 100) == AgreementHolder.Recall.NOT_LOST, "Serving, he is the only one");
		TagValueOutput saved = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, level.registryAccess());
		first.saveWithoutId(saved);
		first.remove(Entity.RemovalReason.UNLOADED_TO_CHUNK);
		PactKeeping.tick(level, perch.holder(), perch.getBlockPos(), g0 + 120);
		helper.assertTrue(!perch.holder().spiritLost(level) && level.getEntitiesOfClass(StolasEntity.class, helper.getBounds(), s -> !s.gone()).isEmpty()
				&& perch.holder().recall(level, keeper, g0 + 120) == AgreementHolder.Recall.NOT_LOST, "Unloaded is not lost: no second is called");
		WorkerRoster.of(level.getServer()).remove(keeper.getUUID(), first.getUUID());
		PactKeeping.tick(level, perch.holder(), perch.getBlockPos(), g0 + 140);
		StolasEntity second = stolas(level, perch);
		helper.assertTrue(second != null && !second.getUUID().equals(first.getUUID()), "Struck off, he is lost and one is called back");
		StolasEntity back = Workers.STOLAS.create(level, EntitySpawnReason.LOAD);
		back.load(TagValueInput.create(ProblemReporter.DISCARDING, level.registryAccess(), saved.buildResult()));
		helper.assertTrue(back.getUUID().equals(first.getUUID()) && back.think(level, 0L, g0 + 160) == Status.FINISHED && back.gone()
				&& "depart".equals(back.cues().getLast()), "The old one, loaded again, finds another in his place and departs");
		helper.assertTrue(second.equals(stolas(level, perch)) && WorkerRoster.of(level.getServer()).workers(keeper.getUUID()).size() == 1,
				"One presence, one roster entry, one pact");
		helper.succeed();
	}

	/** D-06 (D-S7, 02:151, 03:146, E4): killing Stolas is never the fastest route: only damage past invulnerability can; his death ends, returns and drops nothing; he is called back at no cost; only ending returns what was paid ahead, once. */
	@GameTest(maxTicks = 60)
	public void killingStolasIsNeverTheFastestRoute(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		ServerPlayer keeper = ready(level, TestPlayers.survivor(helper));
		BlockPos at = new BlockPos(3, 1, 3);
		long g0 = level.getGameTime();
		PactPerchBlockEntity perch = stolasPact(helper, keeper, at, g0);
		UUID record = perch.holder().agreement().record();
		give(keeper, new ItemStack(Items.AMETHYST_SHARD, 2));
		PactKeeping.deposit(level, keeper, perch.holder(), Items.AMETHYST_SHARD, g0);
		holding(keeper, AstrolabeItem.CAPACITY);
		int resonance = resonance(keeper);
		StolasEntity first = stolas(level, perch);
		helper.assertTrue(!first.hurtServer(level, level.damageSources().playerAttack(keeper), Float.MAX_VALUE) && first.isAlive(), "A blow does nothing");
		helper.assertTrue(first.hurtServer(level, level.damageSources().fellOutOfWorld(), Float.MAX_VALUE) && first.isDeadOrDying(), "Only damage past invulnerability kills him");
		helper.runAfterDelay(25, () -> {
			AABB around = new AABB(helper.absolutePos(at)).inflate(4.0);
			helper.assertTrue(first.isRemoved() && BoundWills.of(level.getServer()).ledger().get(record) != null && escrowed(level, record) == 2
					&& shards(keeper) == 0 && resonance(keeper) == resonance && PactHistory.of(level.getServer()).history(keeper.getUUID()).isEmpty()
					&& looseNear(level, around, Items.AMETHYST_SHARD) == 0 && looseNear(level, around, Items.FEATHER) == 0
					&& level.getEntitiesOfClass(ExperienceOrb.class, around).isEmpty(), "Killed, he ends, returns and drops nothing");
			long now = level.getGameTime();
			PactKeeping.tick(level, perch.holder(), perch.getBlockPos(), now);
			StolasEntity second = stolas(level, perch);
			helper.assertTrue(second != null && !second.getUUID().equals(first.getUUID()) && shards(keeper) == 0 && resonance(keeper) == resonance
					&& escrowed(level, record) == 2, "His holder near, he is called back at no cost");
			helper.assertTrue(PactKeeping.end(level, keeper, perch.holder(), StolasEntity.COUNTERPART, now).ended() == AgreementHolder.Ended.ENDED
					&& shards(keeper) == 2 && escrowed(level, record) == 0, "Only ending returns what was paid ahead");
			helper.assertTrue(PactKeeping.end(level, keeper, perch.holder(), StolasEntity.COUNTERPART, now).ended() != AgreementHolder.Ended.ENDED
					&& shards(keeper) == 2 && PactHistory.of(level.getServer()).history(keeper.getUUID()).size() == 1, "and only once");
			helper.succeed();
		});
	}

	/** D-06 (G11): Jade's readings say whether an agreement is kept, with whom, its service and state, the next offering or the grace, why its presence waits, a copy or a visit, for a perch, a Spirit Anchor and Stolas; no player named, no escrow, nothing changed. */
	@GameTest(maxTicks = 20)
	public void theJadeReadingsNameNoPlayerAndChangeNothing(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		ServerPlayer keeper = ready(level, TestPlayers.survivor(helper));
		ServerPlayer neighbour = ready(level, TestPlayers.survivor(helper));
		ServerPlayer guest = ready(level, TestPlayers.survivor(helper));
		long g0 = level.getGameTime();
		List<CompoundTag> readings = new ArrayList<>();
		PactPerchBlockEntity perch = stolasPact(helper, keeper, new BlockPos(2, 1, 2), g0);
		BoundWill sealed = pactOf(level, perch);
		CompoundTag kept = PactReading.holder(level, perch, g0);
		CompoundTag his = PactReading.stolas(level, stolas(level, perch), g0);
		readings.addAll(List.of(kept, his));
		helper.assertTrue("kept".equals(kept.getStringOr("standing", "")) && StolasEntity.COUNTERPART.equals(kept.getStringOr("counterpart", ""))
				&& StarReckoning.WORK.equals(kept.getStringOr("service", "")) && "active".equals(kept.getStringOr("state", ""))
				&& kept.getIntOr("due_days", -1) == 7 && !kept.contains("grace_days") && !kept.contains("waiting") && sealed.equals(pactOf(level, perch)),
				"Kept: Stolas, Star-reckoning, active, next offering in 7 game days; reading changed nothing: " + kept);
		helper.assertTrue("kept".equals(his.getStringOr("standing", "")) && his.getIntOr("due_days", -1) == 7 && !his.contains("counterpart"), "Stolas: " + his);
		long due = sealed.pact().upkeep().due();
		stay(level, perch.holder(), g0 + 1_000L, due - 1_000L);
		helper.assertTrue(perch.holder().look(level, due) == Pacts.Event.WARNED, "Nothing paid ahead: warned when due");
		CompoundTag warned = PactReading.holder(level, perch, due);
		readings.add(warned);
		helper.assertTrue("upkeep_due".equals(warned.getStringOr("state", "")) && warned.getIntOr("grace_days", -1) == 1 && !warned.contains("due_days"), "Warned: " + warned);
		stay(level, perch.holder(), due + 1_000L, due + Pacts.DAY - 1_000L);
		helper.assertTrue(perch.holder().look(level, due + Pacts.DAY) == Pacts.Event.SUSPENDED, "The grace over, held");
		CompoundTag held = PactReading.holder(level, perch, due + Pacts.DAY);
		readings.add(held);
		helper.assertTrue("upkeep_held".equals(held.getStringOr("state", "")) && !held.contains("due_days") && !held.contains("grace_days"), "Held: " + held);
		SpiritAnchorBlockEntity anchor = sealed(helper, keeper, new BlockPos(6, 1, 2), null, g0);
		CompoundTag gathering = PactReading.holder(level, anchor, g0);
		readings.add(gathering);
		helper.assertTrue("kept".equals(gathering.getStringOr("standing", "")) && Pacts.counterpart(SpiritAnchorBlockEntity.COUNTERPART).equals(gathering.getStringOr("counterpart", ""))
				&& "gather".equals(gathering.getStringOr("service", "")) && "active".equals(gathering.getStringOr("state", "")) && !gathering.contains("due_days"),
				"The anchor keeps a Gathering Shade's agreement, gathering, no upkeep: " + gathering);
		((WorkerEntity<?>) level.getEntity(anchor.spirit())).remove(Entity.RemovalReason.KILLED);
		CompoundTag lost = PactReading.holder(level, anchor, g0);
		readings.add(lost);
		helper.assertTrue(PactReading.LOST.equals(lost.getStringOr("waiting", "")), "Its shade killed, lost: " + lost);
		PactPerchBlockEntity theirs = stolasPact(helper, neighbour, new BlockPos(2, 1, 6), g0);
		place(helper, neighbour, 100.5, 1.0, 100.5);
		helper.assertTrue(stolas(level, theirs).think(level, 0L, g0 + 20) == Status.OWNER_OFFLINE, "Its holder away, he waits");
		CompoundTag away = PactReading.holder(level, theirs, g0 + 20);
		readings.add(away);
		helper.assertTrue(Status.OWNER_OFFLINE.id.equals(away.getStringOr("waiting", "")), "The perch says why: " + away);
		PactPerchBlockEntity copy = perch(helper, new BlockPos(4, 1, 4), keeper);
		copy.loadWithComponents(TagValueInput.create(ProblemReporter.DISCARDING, level.registryAccess(), perch.saveWithoutMetadata(level.registryAccess())));
		CompoundTag copied = PactReading.holder(level, copy, g0);
		readings.add(copied);
		helper.assertTrue("copy".equals(copied.getStringOr("standing", "")) && copy.stolas(level) == null,
				"A copied perch reads as a copy, and the original's Stolas is never the copy's (critique 6): " + copied);
		BlockPos visited = new BlockPos(6, 1, 6);
		PactPerchBlockEntity fresh = perch(helper, visited, guest);
		beside(helper, guest, visited);
		open(helper, guest, fresh);
		CompoundTag visiting = PactReading.holder(level, fresh, g0);
		readings.add(visiting);
		helper.assertTrue("none".equals(visiting.getStringOr("standing", "")) && visiting.getBooleanOr("visiting", false), "Visiting: " + visiting);
		CompoundTag switchedOff = new CompoundTag();
		off(() -> switchedOff.merge(PactReading.holder(level, perch, g0)));
		helper.assertTrue("disabled".equals(switchedOff.getStringOr("standing", "")), "Switched off, it says so");
		for (CompoundTag reading : readings) {
			String text = reading.toString();
			helper.assertTrue(PactReading.KEYS.containsAll(reading.keySet()), "Only PactReading.KEYS: " + text);
			for (ServerPlayer player : List.of(keeper, neighbour, guest)) {
				helper.assertTrue(!text.contains(player.getUUID().toString()) && !text.contains(player.getName().getString()), "No player named: " + text);
			}
		}
		BoundWills.of(level.getServer()).release(perch.holder().agreement().record(), keeper.getUUID());
		CompoundTag released = PactReading.holder(level, perch, g0);
		helper.assertTrue("none".equals(released.getStringOr("standing", "")) && !released.getBooleanOr("visiting", false)
				&& perch.holder().agreement() != null,
				"Its record gone, the reading says none, no visit (its presence is not its visitor, critique 7), and forgets nothing: " + released);
		helper.assertTrue(perch.holder().verify(level, g0) == AgreementHolder.Standing.NONE && perch.holder().agreement() == null, "only verifying forgets");
		helper.succeed();
	}

	// ---------------------------------------------------------------- Concordance Chunk D, D-07: the roster

	/** {@code player}'s roster as the key opens it, its gate forgotten first: the state the server answered. */
	static RosterStatePayload roster(ServerPlayer player) {
		RateGate.forget(player.getUUID());
		RosterStatePayload state = Roster.receive(player, new RosterRequestPayload(Roster.lastSequence(player) + 1, RosterRequestPayload.OPEN,
				RosterRequestPayload.NO_ROW, 0));
		if (state == null) {
			throw new IllegalStateException("The roster did not open for " + player.getName().getString());
		}
		return state;
	}

	/** As the screen sends {@code action} on {@code row}: the next sequence and the revision it showed. */
	static @Nullable RosterStatePayload act(ServerPlayer player, RosterStatePayload.@Nullable Row row, int action) {
		RateGate.forget(player.getUUID());
		if (row == null) {
			throw new IllegalStateException("No such row on the roster");
		}
		return Roster.receive(player, new RosterRequestPayload(Roster.lastSequence(player) + 1, action, row.id(), row.revision()));
	}

	static RosterStatePayload.@Nullable Row rowOf(@Nullable RosterStatePayload state, UUID id) {
		return state == null ? null : state.rows().stream().filter(row -> row.id().equals(id)).findFirst().orElse(null);
	}

	static boolean answered(@Nullable RosterStatePayload state, String reason) {
		return state != null && state.result().isPresent() && state.result().get().reason().equals(reason);
	}

	static @Nullable Component result(@Nullable RosterStatePayload state) {
		return state == null ? null : state.result().map(PactStatePayload.Result::message).orElse(null);
	}

	/**
	 * D-07 (E1, E4, D-S11): the roster lists only its reader's own: their agreement, its shade in the same row, nothing of
	 * another's; suspended and resumed from it (a second suspend not offered, so nothing toggles back), ended from it: the
	 * escrow back once, the shade gone and off the roster, the anchor forgets it; the other's agreement, escrow and shade are
	 * untouched and still on their own roster.
	 */
	@GameTest(maxTicks = 20)
	public void theRosterEndsOnlyWhatTheReaderHolds(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		ServerPlayer reader = binder(helper);
		ServerPlayer other = binder(helper);
		long now = level.getGameTime() + 4_400_000L;
		SpiritAnchorBlockEntity mine = sealed(helper, reader, new BlockPos(1, 1, 1), UPKEEP, now);
		SpiritAnchorBlockEntity theirs = sealed(helper, other, new BlockPos(5, 1, 1), UPKEEP, now);
		UUID record = mine.agreement().record();
		UUID otherRecord = theirs.agreement().record();
		UUID shade = mine.spirit();
		UUID otherShade = theirs.spirit();
		beside(helper, reader, new BlockPos(1, 1, 1));
		helper.assertValueEqual(mine.holder().deposit(level, reader, new ItemStack(Items.WOOL.white(), 2), now), 2, "The reader pays two ahead");
		helper.assertValueEqual(theirs.holder().deposit(level, other, new ItemStack(Items.WOOL.white(), 1), now), 1, "The other pays one ahead");
		RosterStatePayload state = roster(reader);
		List<UUID> ids = state.rows().stream().map(RosterStatePayload.Row::id).toList();
		helper.assertValueEqual(ids, List.of(record), "The reader's roster: their own agreement, its shade in the same row, nothing of the other's");
		RosterStatePayload.Row row = state.rows().getFirst();
		helper.assertTrue(!row.request() && "jugcraft:gathering_shade".equals(row.portrait())
				&& key(row.kind()).equals("message.jugcraft.concordance.workers.kind.spirit")
				&& key(row.name()).equals("entity.jugcraft.gathering_shade") && key(row.service()).equals("message.jugcraft.concordance.roster.service.gather")
				&& row.loaded() && row.icon() == RosterStatePayload.ACTIVE && row.offers(RosterRequestPayload.SUSPEND) && row.offers(RosterRequestPayload.END)
				&& !row.offers(RosterRequestPayload.RESUME) && !row.offers(RosterRequestPayload.CANCEL) && !row.offers(RosterRequestPayload.RECOVER),
				"Its row: a spirit gathering, loaded, active; suspend and end offered: " + row);
		BoundWills wills = BoundWills.of(level.getServer());
		RosterStatePayload suspended = act(reader, row, RosterRequestPayload.SUSPEND);
		helper.assertTrue(answered(suspended, "suspended") && wills.ledger().get(record).pact().hold() == BoundWill.Hold.HOLDER
				&& wills.ledger().get(otherRecord).pact().hold() == BoundWill.Hold.NONE, "Suspended from the roster; the other's is not");
		RosterStatePayload.Row held = rowOf(suspended, record);
		helper.assertTrue(held != null && held.offers(RosterRequestPayload.RESUME) && !held.offers(RosterRequestPayload.SUSPEND)
				&& held.icon() == RosterStatePayload.SUSPENDED, "Now it offers resume, not suspend");
		RosterStatePayload again = act(reader, held, RosterRequestPayload.SUSPEND);
		helper.assertTrue(answered(again, PactMenu.NOT_OFFERED) && wills.ledger().get(record).pact().hold() == BoundWill.Hold.HOLDER,
				"A second suspend is not offered: nothing toggles back");
		RosterStatePayload resumed = act(reader, rowOf(again, record), RosterRequestPayload.RESUME);
		helper.assertTrue(answered(resumed, "resumed") && wills.ledger().get(record).pact().hold() == BoundWill.Hold.NONE, "and resumed");
		RosterStatePayload ended = act(reader, rowOf(resumed, record), RosterRequestPayload.END);
		helper.assertTrue(answered(ended, "ended") && wool(reader) == 2 && wills.ledger().get(record) == null && wills.escrow(record).isEmpty(),
				"Ended from the roster: the escrow comes back once: " + key(result(ended)));
		Entity gone = level.getEntity(shade);
		helper.assertTrue((gone == null || gone.isRemoved()) && !WorkerRoster.of(level.getServer()).workers(reader.getUUID()).containsKey(shade)
				&& mine.agreement() == null && PactHistory.of(level.getServer()).history(reader.getUUID()).size() == 1 && ended.rows().isEmpty(),
				"Its shade departs, off the roster; the anchor forgets it; one departure kept; the roster is empty");
		Entity still = level.getEntity(otherShade);
		helper.assertTrue(wills.ledger().get(otherRecord) != null && escrowed(level, otherRecord) == 1 && wool(other) == 0 && still != null
				&& !still.isRemoved() && theirs.agreement() != null && PactHistory.of(level.getServer()).history(other.getUUID()).isEmpty()
				&& rowOf(roster(other), otherRecord) != null, "The other's agreement, escrow and shade are untouched, and on their own roster");
		helper.succeed();
	}

	/**
	 * D-07 (E1, 08:99): another player naming the holder's record or request, with its true revision, is answered not_listed;
	 * below the listing the authorities refuse as well (not_holder, not_yours); a party member's roster lists none of it and
	 * may not end it; a stand-in with the holder's identity is dropped and ends nothing; nothing of the holder's changes; the
	 * holder cancels their own request.
	 */
	@GameTest(maxTicks = 20)
	public void aRequestNamingAnotherPlayersRecordIsRefused(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		ServerPlayer holder = binder(helper);
		ServerPlayer other = binder(helper);
		long now = level.getGameTime() + 4_500_000L;
		SpiritAnchorBlockEntity anchor = sealed(helper, holder, new BlockPos(3, 1, 3), UPKEEP, now);
		UUID record = anchor.agreement().record();
		helper.assertValueEqual(anchor.holder().deposit(level, holder, new ItemStack(Items.WOOL.white(), 2), now), 2, "Two periods paid ahead");
		CourierLedger ledger = CourierLedger.of(level.getServer());
		Place post = Couriers.place(level, helper.absolutePos(new BlockPos(1, 1, 1)));
		Logistics.Filed filed = ledger.file(holder.getUUID(), post, post, new ItemStack(Items.STONE), 4, level.registryAccess(), now);
		helper.assertTrue(filed.outcome() == Logistics.Outcome.DONE, "The holder files a request");
		RosterStatePayload mine = roster(holder);
		RosterStatePayload.Row theirs = rowOf(mine, record);
		RosterStatePayload.Row theirRequest = rowOf(mine, Roster.rowOf(filed.id()));
		helper.assertTrue(theirs != null && theirRequest != null, "Both are on the holder's roster");
		helper.assertTrue(theirRequest.request() && "minecraft:stone".equals(theirRequest.portrait()) && !theirs.request(),
				"A request's row is drawn as its item; the record's is not a request");
		helper.assertTrue(roster(other).rows().isEmpty(), "Another player's roster lists nothing of the holder's");
		for (int action : new int[] {RosterRequestPayload.SUSPEND, RosterRequestPayload.END}) {
			helper.assertTrue(answered(act(other, theirs, action), Roster.NOT_LISTED), "Named by another, not listed: " + RosterRequestPayload.ACTIONS.get(action));
		}
		for (int action : new int[] {RosterRequestPayload.CANCEL, RosterRequestPayload.RECOVER}) {
			helper.assertTrue(answered(act(other, theirRequest, action), Roster.NOT_LISTED), "Named by another, not listed: " + RosterRequestPayload.ACTIONS.get(action));
		}
		helper.assertValueEqual(Roster.act(other, Roster.RECORD, record, RosterRequestPayload.END, now).reason(), "not_holder", "Ending another's record");
		helper.assertValueEqual(Roster.act(other, Roster.RECORD, record, RosterRequestPayload.SUSPEND, now).reason(), "not_holder", "suspending it");
		helper.assertValueEqual(Roster.act(other, Roster.REQUEST, Roster.rowOf(filed.id()), RosterRequestPayload.CANCEL, now).reason(), "not_yours",
				"cancelling another's request");
		helper.assertValueEqual(Roster.act(other, Roster.REQUEST, Roster.rowOf(filed.id()), RosterRequestPayload.RECOVER, now).reason(), "not_yours",
				"recovering it");
		PartyManager parties = JugcraftParties.manager();
		long ms = System.currentTimeMillis();
		try {
			helper.assertTrue(parties.invite(holder.getUUID(), other.getUUID(), ms) == PartyManager.Result.OK
					&& parties.accept(other.getUUID(), ms) == PartyManager.Result.OK, "They form a party");
			helper.assertTrue(roster(other).rows().isEmpty()
					&& Roster.act(other, Roster.RECORD, record, RosterRequestPayload.END, now).reason().equals("not_holder"),
					"A party member's roster lists none of it, and they may not end it");
		} finally {
			parties.adminDisband(holder.getUUID());
		}
		FakePlayer standIn = FakePlayer.get(level, new GameProfile(holder.getUUID(), "[Concordance]"));
		helper.assertTrue(Roster.receive(standIn, new RosterRequestPayload(1, RosterRequestPayload.OPEN, RosterRequestPayload.NO_ROW, 0)) == null
				&& Roster.receive(standIn, new RosterRequestPayload(Roster.lastSequence(holder) + 1, RosterRequestPayload.END, record, theirs.revision())) == null
				&& Roster.act(standIn, Roster.RECORD, record, RosterRequestPayload.END, now).reason().equals(Roster.NOT_PERMITTED),
				"A stand-in with the holder's identity is dropped and ends nothing");
		BoundWills wills = BoundWills.of(level.getServer());
		helper.assertTrue(wills.ledger().get(record) != null && wills.ledger().get(record).pact().hold() == BoundWill.Hold.NONE && escrowed(level, record) == 2
				&& wool(other) == 0 && ledger.ledger().get(filed.id()) != null && ledger.ledger().get(filed.id()).state() == RequestState.OPEN,
				"Nothing of the holder's was ended, suspended, cancelled or handed over");
		helper.assertTrue(answered(act(holder, theirRequest, RosterRequestPayload.CANCEL), "cancelled") && ledger.ledger().get(filed.id()) == null,
				"Its own requester cancels it from the roster");
		helper.succeed();
	}

	/**
	 * D-07 (D-S11, critique 17): with the roster's 32 full, a pact whose perch was broken and a record from before Chunk D not yet
	 * claimed are still listed, first: without its place, or unclaimed; each offers only its end. Kept at the perch, the pact's
	 * row carries Stolas and offers suspend. A construct not loaded is drawn as its kind's creature.
	 */
	@GameTest(maxTicks = 20)
	public void theRosterListsAPactWithoutItsPerch(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		ServerPlayer holder = regarded(helper);
		long now = level.getGameTime() + 4_100_000L;
		BlockPos at = new BlockPos(2, 1, 2);
		PactPerchBlockEntity perch = stolasPact(helper, holder, at, now);
		UUID record = perch.holder().agreement().record();
		StolasEntity stolas = stolas(level, perch);
		RosterStatePayload kept = roster(holder);
		RosterStatePayload.Row seated = rowOf(kept, record);
		helper.assertTrue(stolas != null && seated != null && rowOf(kept, stolas.getUUID()) == null
				&& key(seated.kind()).equals("message.jugcraft.concordance.workers.kind.pact") && key(seated.name()).equals("entity.jugcraft.stolas")
				&& "jugcraft:stolas".equals(seated.portrait())
				&& seated.loaded() && seated.offers(RosterRequestPayload.SUSPEND) && seated.offers(RosterRequestPayload.END),
				"Kept at the perch: one row, Stolas in it, suspend and end offered: " + seated);
		helper.setBlock(at, Blocks.AIR);
		helper.assertTrue(stolas.gone() && BoundWills.of(level.getServer()).ledger().get(record).pact().place() == null,
				"The perch broken, the pact is without it; Stolas leaves");
		WorkerRoster roster = WorkerRoster.of(level.getServer());
		String dimension = level.dimension().identifier().toString();
		while (roster.workers(holder.getUUID()).size() < WorkerRoster.MAX_PER_PLAYER) {
			roster.note(holder.getUUID(), UUID.randomUUID(), "construct", Status.IDLE, dimension, helper.absolutePos(at));
		}
		UUID legacy = UUID.randomUUID();
		helper.assertValueEqual(BoundWills.of(level.getServer()).seal(new BoundWill(legacy, GatheringShadeEntity.DEFINITION, "gathering_shade",
				holder.getUUID(), now + 1, false)).outcome(), BoundWillLedger.Outcome.DONE, "A record from before Chunk D, not yet claimed");
		RosterStatePayload state = roster(holder);
		helper.assertTrue(state.rows().size() == WorkerRoster.MAX_PER_PLAYER + 2 && state.more() == 0
				&& state.rows().get(0).id().equals(record) && state.rows().get(1).id().equals(legacy),
				"With the roster's 32 full, both records are listed, first: " + state.rows().size());
		RosterStatePayload.Row orphan = state.rows().get(0);
		RosterStatePayload.Row unclaimed = state.rows().get(1);
		helper.assertTrue(orphan.icon() == RosterStatePayload.WITHOUT_PLACE && key(orphan.place()).equals("message.jugcraft.concordance.roster.place.none")
				&& key(orphan.waiting()).equals("message.jugcraft.concordance.roster.waiting.without_place") && !orphan.loaded()
				&& orphan.offers(RosterRequestPayload.END) && !orphan.offers(RosterRequestPayload.SUSPEND) && !orphan.offers(RosterRequestPayload.RESUME),
				"The pact without its perch: without its place, only its end offered: " + orphan);
		helper.assertTrue(unclaimed.icon() == RosterStatePayload.UNCLAIMED && "jugcraft:gathering_shade".equals(unclaimed.portrait())
				&& key(unclaimed.name()).equals("entity.jugcraft.gathering_shade")
				&& unclaimed.offers(RosterRequestPayload.END) && !unclaimed.offers(RosterRequestPayload.SUSPEND),
				"The unclaimed record: only its end offered: " + unclaimed);
		RosterStatePayload.Row construct = state.rows().get(2);
		helper.assertTrue(!construct.loaded() && "jugcraft:clockwork_porter".equals(construct.portrait()) && construct.actions() == 0,
				"A construct not loaded is drawn as its kind's creature and offers nothing: " + construct);
		helper.succeed();
	}

	/**
	 * D-07 (E4, D-S6, D-S16): a pact without its perch, ended from the roster, gives its escrow back once to its living
	 * reader: switched off it is listed but nothing is offered or ended, and the screen's end is answered disabled; a reader
	 * who is not alive ends nothing and is handed nothing; alive, it ends, the two offerings come back once and the departure
	 * is kept with no place; ended again (an old row, or by its id) it finds nothing; the journal's pacts section names the
	 * departure.
	 */
	@GameTest(maxTicks = 20)
	public void endingAnOrphanReturnsItsEscrowOnce(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		ServerPlayer holder = regarded(helper);
		long now = level.getGameTime() + 4_200_000L;
		BlockPos at = new BlockPos(2, 1, 2);
		PactPerchBlockEntity perch = stolasPact(helper, holder, at, now);
		UUID record = perch.holder().agreement().record();
		helper.assertValueEqual(perch.holder().deposit(level, holder, new ItemStack(Items.AMETHYST_SHARD, 2), now), 2, "Two offerings paid ahead");
		helper.setBlock(at, Blocks.AIR);
		BoundWills wills = BoundWills.of(level.getServer());
		helper.assertTrue(wills.ledger().get(record).pact().place() == null && escrowed(level, record) == 2, "Without its perch, the escrow kept");
		RosterStatePayload.Row orphan = rowOf(roster(holder), record);
		helper.assertTrue(orphan != null && key(orphan.confirm()).equals("message.jugcraft.concordance.roster.confirm.end")
				&& Integer.valueOf(2).equals(arg(orphan.confirm(), 1)), "Its confirmation says two offerings come back");
		off(() -> {
			RosterStatePayload closed = roster(holder);
			RosterStatePayload.Row offRow = rowOf(closed, record);
			helper.assertTrue(!closed.enabled() && offRow != null && offRow.actions() == 0 && offRow.icon() == RosterStatePayload.DISABLED
					&& answered(act(holder, offRow, RosterRequestPayload.END), PactMenu.DISABLED)
					&& Roster.act(holder, Roster.RECORD, record, RosterRequestPayload.END, level.getGameTime()).reason().equals(PactMenu.DISABLED)
					&& BoundWills.of(level.getServer()).ledger().get(record) != null && escrowed(level, record) == 2,
					"Switched off, it is listed, nothing is offered, the screen's end is answered disabled and nothing ends (D-S16, D-P110)");
		});
		holder.setHealth(0.0F);
		RosterStatePayload refused = act(holder, orphan, RosterRequestPayload.END);
		helper.assertTrue(answered(refused, Roster.NOT_PERMITTED) && wills.ledger().get(record) != null && escrowed(level, record) == 2
				&& holder.getInventory().countItem(Items.AMETHYST_SHARD) == 0, "A reader who is not alive ends nothing and is handed nothing");
		holder.setHealth(holder.getMaxHealth());
		RosterStatePayload ended = act(holder, rowOf(refused, record), RosterRequestPayload.END);
		helper.assertTrue(answered(ended, "ended") && key(result(ended)).equals("message.jugcraft.concordance.pact.departed")
				&& Integer.valueOf(2).equals(arg(result(ended), 2)), "Ended: Stolas's departure, two offerings back: " + result(ended));
		helper.assertTrue(wills.ledger().get(record) == null && wills.escrow(record).isEmpty() && holder.getInventory().countItem(Items.AMETHYST_SHARD) == 2
				&& rowOf(ended, record) == null, "The record and its escrow go; the two come back once; it is off the roster");
		List<Pacts.Departure> history = PactHistory.of(level.getServer()).history(holder.getUUID());
		helper.assertTrue(history.size() == 1 && history.getFirst().record().equals(record) && Pacts.ENDED.equals(history.getFirst().reason())
				&& history.getFirst().place() == null, "One departure kept, ended, with no place: " + history);
		helper.assertTrue(answered(act(holder, orphan, RosterRequestPayload.END), Roster.NOT_LISTED)
				&& Roster.act(holder, Roster.RECORD, record, RosterRequestPayload.END, now).reason().equals("nothing")
				&& holder.getInventory().countItem(Items.AMETHYST_SHARD) == 2 && PactHistory.of(level.getServer()).history(holder.getUUID()).size() == 1
				&& wills.problems().isEmpty(), "Ended again it finds nothing, and nothing more comes back");
		JournalSection pacts = Journal.build(holder).stream().filter(section -> section.id().equals("pacts")).findFirst().orElse(null);
		helper.assertTrue(pacts != null && !pacts.lines().isEmpty() && key(pacts.lines().getLast().text()).equals("journal.jugcraft.pacts.departed")
				&& arg(pacts.lines().getLast().text(), 0) instanceof Component who && key(who).equals("entity.jugcraft.stolas")
				&& arg(pacts.lines().getLast().text(), 2) instanceof Component why && key(why).equals("message.jugcraft.concordance.workers.departed.ended")
				&& pacts.lines().stream().noneMatch(line -> key(line.text()).equals("journal.jugcraft.pacts.kept")),
				"The journal's pacts section names the departure and keeps no pact");
		helper.succeed();
	}

	/**
	 * D-07 (08:99): an end on a row read before its escrow changed is answered changed, nothing ended or handed back, with the
	 * row again at its new revision; a request not newer than the last is dropped; read afresh, it ends and both come back once.
	 */
	@GameTest(maxTicks = 20)
	public void aStaleRosterRevisionIsRefused(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		ServerPlayer holder = binder(helper);
		long now = level.getGameTime() + 4_300_000L;
		SpiritAnchorBlockEntity anchor = sealed(helper, holder, new BlockPos(3, 1, 3), UPKEEP, now);
		UUID record = anchor.agreement().record();
		BoundWills wills = BoundWills.of(level.getServer());
		helper.assertValueEqual(anchor.holder().deposit(level, holder, new ItemStack(Items.WOOL.white(), 1), now), 1, "One period paid ahead");
		RosterStatePayload.Row seen = rowOf(roster(holder), record);
		helper.assertTrue(seen != null && Integer.valueOf(1).equals(arg(seen.confirm(), 1)), "Read: one offering would come back");
		helper.assertValueEqual(anchor.holder().deposit(level, holder, new ItemStack(Items.WOOL.white(), 1), now), 1, "Then a second is paid ahead");
		RosterStatePayload stale = act(holder, seen, RosterRequestPayload.END);
		helper.assertTrue(answered(stale, PactMenu.CHANGED) && wills.ledger().get(record) != null && escrowed(level, record) == 2 && wool(holder) == 0,
				"An end on what was read before is refused: nothing ended, nothing handed back");
		RosterStatePayload.Row fresh = rowOf(stale, record);
		helper.assertTrue(fresh != null && fresh.revision() != seen.revision() && Integer.valueOf(2).equals(arg(fresh.confirm(), 1)),
				"The answer reads it again: two would come back");
		RateGate.forget(holder.getUUID());
		helper.assertTrue(Roster.receive(holder, new RosterRequestPayload(Roster.lastSequence(holder), RosterRequestPayload.END, record, fresh.revision())) == null
				&& wills.ledger().get(record) != null, "A request not newer than the last is dropped");
		RosterStatePayload ended = act(holder, fresh, RosterRequestPayload.END);
		helper.assertTrue(answered(ended, "ended") && wool(holder) == 2 && wills.ledger().get(record) == null, "Read afresh, it ends; both come back, once");
		helper.succeed();
	}

	/**
	 * D-07 (critique 17): 32 Bound Wills, 16 courier requests and 32 workers are 80: the roster sends 64, records first in the
	 * order sealed, then requests oldest first, then workers, and counts the 16 left out; a request is drawn as its item and a
	 * construct not loaded as its kind's creature; the 64 cross whole; no payload holds a 65th.
	 */
	@GameTest(maxTicks = 20)
	public void theRosterStopsAt64Rows(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		ServerPlayer reader = binder(helper);
		UUID me = reader.getUUID();
		long now = level.getGameTime() + 4_600_000L;
		BoundWills wills = BoundWills.of(level.getServer());
		List<UUID> records = new ArrayList<>();
		for (int i = 0; i < BoundWillLedger.MAX_PER_HOLDER; i++) {
			BoundWill.Pact pact = new BoundWill.Pact(null, BoundWill.Hold.NONE, now, Pacts.PERSONAL, null, null);
			BoundWill record = new BoundWill(UUID.randomUUID(), GatheringShadeEntity.DEFINITION, GatheringShadeEntity.DEFINITION, me, now + i, false, pact);
			helper.assertTrue(wills.seal(record).outcome() == BoundWillLedger.Outcome.DONE, "record " + i);
			records.add(record.id());
		}
		CourierLedger ledger = CourierLedger.of(level.getServer());
		Place post = Couriers.place(level, helper.absolutePos(new BlockPos(1, 1, 1)));
		List<Long> requests = new ArrayList<>();
		for (int i = 0; i < Logistics.MAX_OPEN_PER_PLAYER; i++) {
			Logistics.Filed filed = ledger.file(me, post, post, new ItemStack(Items.STONE), 1, level.registryAccess(), now);
			helper.assertTrue(filed.outcome() == Logistics.Outcome.DONE, "request " + i + ": " + filed);
			requests.add(filed.id());
		}
		WorkerRoster roster = WorkerRoster.of(level.getServer());
		String dimension = level.dimension().identifier().toString();
		for (int i = 0; i < WorkerRoster.MAX_PER_PLAYER; i++) {
			roster.note(me, UUID.randomUUID(), "construct", Status.IDLE, dimension, helper.absolutePos(new BlockPos(1, 1, 1)));
		}
		try {
			RosterStatePayload state = roster(reader);
			int all = BoundWillLedger.MAX_PER_HOLDER + Logistics.MAX_OPEN_PER_PLAYER + WorkerRoster.MAX_PER_PLAYER;
			helper.assertTrue(state.rows().size() == RosterStatePayload.MAX_ROWS && state.more() == all - RosterStatePayload.MAX_ROWS,
					"At most 64 rows, the rest counted: " + state.rows().size() + ", " + state.more());
			List<UUID> ids = state.rows().stream().map(RosterStatePayload.Row::id).toList();
			int r = records.size();
			int q = requests.size();
			helper.assertValueEqual(ids.subList(0, r), records, "Every record first, in the order sealed");
			helper.assertValueEqual(ids.subList(r, r + q), requests.stream().map(Roster::rowOf).toList(), "then every request, oldest first");
			List<UUID> workers = new ArrayList<>(roster.workers(me).keySet());
			helper.assertValueEqual(ids.subList(r + q, RosterStatePayload.MAX_ROWS), workers.subList(0, RosterStatePayload.MAX_ROWS - r - q),
					"then the roster's workers as far as room allows");
			helper.assertTrue(state.rows().get(r).request() && "minecraft:stone".equals(state.rows().get(r).portrait())
					&& !state.rows().get(r + q).request() && "jugcraft:clockwork_porter".equals(state.rows().get(r + q).portrait()),
					"A request is drawn as its item, a construct not loaded as its kind's creature");
			RosterStatePayload.Row first = state.rows().getFirst();
			helper.assertTrue(first.offers(RosterRequestPayload.END) && !first.offers(RosterRequestPayload.SUSPEND) && first.icon() == RosterStatePayload.WITHOUT_PLACE,
					"A record without its place offers only its end");
			RegistryFriendlyByteBuf buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), level.registryAccess());
			RosterStatePayload.CODEC.encode(buffer, state);
			RosterStatePayload received = RosterStatePayload.CODEC.decode(buffer);
			helper.assertTrue(received.rows().size() == RosterStatePayload.MAX_ROWS && received.more() == state.more()
					&& received.rows().getFirst().id().equals(first.id()) && received.rows().getFirst().revision() == first.revision()
					&& received.rows().getFirst().actions() == first.actions() && received.rows().getFirst().portrait().equals(first.portrait())
					&& received.rows().getFirst().icon() == first.icon(), "The 64 rows cross whole");
			List<RosterStatePayload.Row> many = new ArrayList<>(state.rows());
			many.add(first);
			helper.assertValueEqual(new RosterStatePayload(0, true, many, 0, Optional.empty()).rows().size(), RosterStatePayload.MAX_ROWS,
					"No payload holds a 65th row");
		} finally {
			requests.forEach(id -> ledger.cancel(id, me, now));
		}
		helper.succeed();
	}

	/**
	 * D-07, closing D-08's As built (its last item; unit 1's review fix 2, for the hand): a workshop hand unloaded and
	 * replaced, loading again while the Concordance is off, takes no roster place by a change of status; on again, it finds
	 * another hand in its place and leaves, and the new hand keeps the agreement and its roster place.
	 */
	@GameTest(maxTicks = 20)
	public void aReplacedHandLoadingWhileSwitchedOffStaysOffTheRoster(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		WorkshopHands.Workshop shop = WorkshopHands.workshop(helper);
		ServerPlayer holder = shop.holder();
		UUID id = holder.getUUID();
		WorkshopHandEntity old = shop.hand();
		UUID first = old.getUUID();
		WorkerRoster roster = WorkerRoster.of(level.getServer());
		TagValueOutput saved = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, level.registryAccess());
		old.saveWithoutId(saved);
		old.remove(Entity.RemovalReason.UNLOADED_TO_CHUNK);
		helper.assertTrue(roster.workers(id).containsKey(first) && !shop.anchor().holder().spiritLost(level),
				"Unloaded is not lost: the hand keeps its roster place");
		roster.remove(id, first); // as one sealed before its roster note, or while the roster was full
		long now = WorkshopHands.morning(level, 3_500_000L);
		helper.assertValueEqual(shop.anchor().holder().recall(level, holder, now), AgreementHolder.Recall.RECALLED,
				"Missing from the roster it is lost, and its holder calls another hand");
		UUID second = shop.anchor().spirit();
		WorkshopHandEntity back = Workers.WORKSHOP_HAND.create(level, EntitySpawnReason.LOAD);
		back.load(TagValueInput.create(ProblemReporter.DISCARDING, level.registryAccess(), saved.buildResult()));
		helper.assertTrue(back.getUUID().equals(first) && second != null && !first.equals(second), "The old hand loads again, a new one in its place");
		long in = WorkshopHands.inHours();
		off(() -> helper.assertTrue(back.think(level, in, now + 20) == Status.DISABLED && !roster.workers(id).containsKey(first),
				"Switched off, it waits and takes no roster place (joinsRosterOnStatus false, as the shade's and Stolas's)"));
		helper.assertTrue(back.think(level, in, now + 40) == Status.FINISHED && back.isRemoved() && !roster.workers(id).containsKey(first),
				"On again, it finds another hand in its place and leaves, off the roster");
		helper.assertTrue(second.equals(shop.anchor().spirit()) && roster.workers(id).containsKey(second)
				&& BoundWills.of(level.getServer()).ledger().heldBy(id).size() == 1, "The new hand keeps the agreement and its roster place");
		helper.succeed();
	}
}
