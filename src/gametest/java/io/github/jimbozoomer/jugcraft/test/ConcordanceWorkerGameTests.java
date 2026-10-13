package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.concordance.ConcordanceProgress;
import io.github.jimbozoomer.jugcraft.concordance.LeyPylonBlockEntity;
import io.github.jimbozoomer.jugcraft.concordance.JugcraftConcordance;
import io.github.jimbozoomer.jugcraft.concordance.RateGate;
import io.github.jimbozoomer.jugcraft.concordance.rules.ResearchState;
import io.github.jimbozoomer.jugcraft.concordance.sign.Sign;
import io.github.jimbozoomer.jugcraft.concordance.spirits.BondingCharmItem;
import io.github.jimbozoomer.jugcraft.concordance.spirits.BoundWills;
import io.github.jimbozoomer.jugcraft.concordance.spirits.ClockworkPorterEntity;
import io.github.jimbozoomer.jugcraft.concordance.spirits.GatheringShadeEntity;
import io.github.jimbozoomer.jugcraft.concordance.spirits.HearthlingEntity;
import io.github.jimbozoomer.jugcraft.concordance.spirits.SpiritAnchorBlock;
import io.github.jimbozoomer.jugcraft.concordance.spirits.SpiritAnchorBlockEntity;
import io.github.jimbozoomer.jugcraft.concordance.spirits.WorkerEntity;
import io.github.jimbozoomer.jugcraft.concordance.spirits.WorkerRoster;
import io.github.jimbozoomer.jugcraft.concordance.spirits.Workers;
import io.github.jimbozoomer.jugcraft.concordance.worker.Body;
import io.github.jimbozoomer.jugcraft.concordance.worker.Bond;
import io.github.jimbozoomer.jugcraft.concordance.worker.Status;
import io.github.jimbozoomer.jugcraft.concordance.worker.WorkerDefinition;
import java.util.UUID;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.phys.Vec3;

/**
 * Roadmap step 17, spirits, familiars and constructs (docs/features/arcane-concordance-workers.md), on a real server.
 * Each test drives a worker's decisions with {@code think} at times it chooses, standing the worker within reach so no
 * walking is needed: a familiar binds one to a person, mends them by its bond and only so often, and waits when its
 * person is offline; a spirit's agreement is sealed as a Bound Will, it gathers and delivers within its terms, says it
 * is outside them at noon and suspended when suspended, and breaking its anchor releases it; a porter says it has no
 * route, is waiting for resources, carries and delivers, spends energy and wear, is full, needs repair and is fuelled
 * by a pylon; and each worker's model survives a save.
 */
public class ConcordanceWorkerGameTests {
	private static final long NIGHT = 18000L;
	private static final long NOON = 6000L;

	private static ServerPlayer binder(GameTestHelper helper) {
		ServerPlayer player = helper.makeMockServerPlayerInLevel();
		player.setGameMode(GameType.SURVIVAL);
		ConcordanceProgress.grant(player, "jugcraft:first_light", ResearchState.UNDERSTOOD);
		ConcordanceProgress.grant(player, Workers.RESEARCH, ResearchState.UNDERSTOOD);
		RateGate.forget(player.getUUID());
		return player;
	}

	private static void place(GameTestHelper helper, Entity entity, double x, double y, double z) {
		Vec3 at = helper.absoluteVec(new Vec3(x, y, z));
		entity.snapTo(at.x, at.y, at.z, 0.0F, 0.0F);
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
		helper.assertTrue(anchor.agreement().suspended() && shade.think(level, NIGHT, game + 60) == Status.SUSPENDED, "Suspended, it says so");
		helper.setBlock(at, Blocks.AIR);
		helper.assertTrue(shade.isRemoved() && BoundWills.of(level.getServer()).ledger().heldBy(holder.getUUID()).isEmpty(),
				"The anchor broken, the agreement is released and the shade departs");
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

	/** A familiar's bond, a porter's body, route and load, and a shade's anchor survive a save. */
	@GameTest(maxTicks = 20)
	public void workersKeepTheirModelsThroughASave(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		HearthlingEntity familiar = Workers.HEARTHLING.create(level, EntitySpawnReason.LOAD);
		familiar.setOwner(new UUID(17L, 5L));
		familiar.setBond(new Bond(42, 3L, 7, 1000L, 2000L));
		TagValueOutput out = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, level.registryAccess());
		familiar.saveWithoutId(out);
		HearthlingEntity copy = Workers.HEARTHLING.create(level, EntitySpawnReason.LOAD);
		copy.load(TagValueInput.create(ProblemReporter.DISCARDING, level.registryAccess(), out.buildResult()));
		helper.assertTrue(copy.bond().equals(familiar.bond()) && familiar.owner().equals(copy.owner()), "The bond and its person are saved");
		ClockworkPorterEntity porter = Workers.CLOCKWORK_PORTER.create(level, EntitySpawnReason.LOAD);
		porter.setBody(new Body(30, 12L));
		porter.setRoute(new ClockworkPorterEntity.Route(new BlockPos(1, 2, 3), new BlockPos(4, 5, 6), "minecraft:overworld"));
		TagValueOutput saved = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, level.registryAccess());
		porter.saveWithoutId(saved);
		ClockworkPorterEntity porterCopy = Workers.CLOCKWORK_PORTER.create(level, EntitySpawnReason.LOAD);
		porterCopy.load(TagValueInput.create(ProblemReporter.DISCARDING, level.registryAccess(), saved.buildResult()));
		helper.assertTrue(porterCopy.body().equals(porter.body()) && porterCopy.route().equals(porter.route()), "The body and route are saved");
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
}
