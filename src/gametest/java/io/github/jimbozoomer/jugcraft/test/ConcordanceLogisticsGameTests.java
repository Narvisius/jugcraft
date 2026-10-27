package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.concordance.Alchemy;
import io.github.jimbozoomer.jugcraft.concordance.Authority;
import io.github.jimbozoomer.jugcraft.concordance.ConcordanceData;
import io.github.jimbozoomer.jugcraft.concordance.ConcordanceProgress;
import io.github.jimbozoomer.jugcraft.concordance.CrucibleBlockEntity;
import io.github.jimbozoomer.jugcraft.concordance.JugcraftConcordance;
import io.github.jimbozoomer.jugcraft.concordance.RateGate;
import io.github.jimbozoomer.jugcraft.concordance.Reagent;
import io.github.jimbozoomer.jugcraft.concordance.alchemy.Formula;
import io.github.jimbozoomer.jugcraft.concordance.alchemy.Operation;
import io.github.jimbozoomer.jugcraft.concordance.courier.CourierLedger;
import io.github.jimbozoomer.jugcraft.concordance.courier.CourierPostBlockEntity;
import io.github.jimbozoomer.jugcraft.concordance.courier.Couriers;
import io.github.jimbozoomer.jugcraft.concordance.diagnostic.Code;
import io.github.jimbozoomer.jugcraft.concordance.diagnostic.Diagnostic;
import io.github.jimbozoomer.jugcraft.concordance.logistics.Event;
import io.github.jimbozoomer.jugcraft.concordance.logistics.Logistics;
import io.github.jimbozoomer.jugcraft.concordance.logistics.Place;
import io.github.jimbozoomer.jugcraft.concordance.logistics.Request;
import io.github.jimbozoomer.jugcraft.concordance.logistics.RequestState;
import io.github.jimbozoomer.jugcraft.concordance.resource.BoundWill;
import io.github.jimbozoomer.jugcraft.concordance.rules.ConcordanceRules;
import io.github.jimbozoomer.jugcraft.concordance.rules.ResearchState;
import io.github.jimbozoomer.jugcraft.concordance.spirits.AgreementHolder;
import io.github.jimbozoomer.jugcraft.concordance.spirits.BoundWills;
import io.github.jimbozoomer.jugcraft.concordance.spirits.ClockworkPorterEntity;
import io.github.jimbozoomer.jugcraft.concordance.spirits.PactHistory;
import io.github.jimbozoomer.jugcraft.concordance.spirits.PactMenu;
import io.github.jimbozoomer.jugcraft.concordance.spirits.Roster;
import io.github.jimbozoomer.jugcraft.concordance.spirits.RosterRequestPayload;
import io.github.jimbozoomer.jugcraft.concordance.spirits.RosterStatePayload;
import io.github.jimbozoomer.jugcraft.concordance.spirits.SpiritAnchorBlockEntity;
import io.github.jimbozoomer.jugcraft.concordance.spirits.WorkerRoster;
import io.github.jimbozoomer.jugcraft.concordance.spirits.Workers;
import io.github.jimbozoomer.jugcraft.concordance.spirits.WorkshopHandEntity;
import io.github.jimbozoomer.jugcraft.concordance.worker.Body;
import io.github.jimbozoomer.jugcraft.concordance.worker.Navigation;
import io.github.jimbozoomer.jugcraft.concordance.worker.Pacts;
import io.github.jimbozoomer.jugcraft.concordance.worker.Status;
import io.github.jimbozoomer.jugcraft.concordance.worker.WorkerDefinition;
import io.github.jimbozoomer.jugcraft.config.JugcraftConfig;
import io.github.jimbozoomer.jugcraft.party.JugcraftParties;
import io.github.jimbozoomer.jugcraft.party.PartyManager;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.function.Supplier;
import local.peepo.CompanionStatus;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.item.ItemStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
/**
 * Roadmap step 18, logistics with reservations and accountable transit (docs/features/arcane-concordance-logistics.md),
 * on a real server. Each test drives couriers' decisions with {@code think} at chosen times, standing them within reach
 * of the post and chests so no walking is needed, and audits after every step that each item is in a container or in
 * the ledger's cargo, exactly once ({@link CourierLedger#problems} and counted totals). Since Concordance Chunk C (C-06),
 * an Alembic Crucible files its own requests: one demand, its formula's shortage, served by a porter and a Peepo without
 * bringing it twice, through every failure case of plan 03:193.
 * Since Concordance Chunk D (D-08) a workshop hand, a spirit sealed at a Spirit Anchor bound to its holder's post, serves
 * the same requests with the same courier decision, within its sealed terms and for its beneficiaries, through every
 * failure case of plan 03:193.
 */
public class ConcordanceLogisticsGameTests {
	private static final long NIGHT = 18000L;

	private static ServerPlayer keeper(GameTestHelper helper) {
		ServerPlayer player = TestPlayers.mock(helper);
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

	/** A post at (2,1,2) with chests at (1,1,2) and (3,1,2), and a fuelled courier between them, bound to the post. */
	private static ClockworkPorterEntity courier(GameTestHelper helper, ServerPlayer keeper, BlockPos post) {
		ServerLevel level = helper.getLevel();
		WorkerDefinition.Construct terms = ClockworkPorterEntity.terms();
		ClockworkPorterEntity porter = Workers.CLOCKWORK_PORTER.create(level, EntitySpawnReason.MOB_SUMMONED);
		porter.setOwner(keeper.getUUID());
		porter.setBody(new Body(terms.integrity(), terms.energy()));
		place(helper, porter, 2.5, 1.0, 3.5);
		level.addFreshEntity(porter);
		helper.assertTrue(porter.setPost(GlobalPos.of(level.dimension(), helper.absolutePos(post))), "The porter takes the post");
		return porter;
	}

	private static CourierPostBlockEntity post(GameTestHelper helper, ServerPlayer keeper, BlockPos at) {
		helper.setBlock(at, Couriers.COURIER_POST);
		CourierPostBlockEntity post = (CourierPostBlockEntity) helper.getLevel().getBlockEntity(helper.absolutePos(at));
		post.setOwner(keeper.getUUID());
		return post;
	}

	private static int count(net.minecraft.world.Container container, net.minecraft.world.item.Item item) {
		int count = 0;
		for (int slot = 0; slot < container.getContainerSize(); slot++) {
			if (container.getItem(slot).is(item)) {
				count += container.getItem(slot).getCount();
			}
		}
		return count;
	}

	private static void audit(GameTestHelper helper, CourierLedger ledger, String when) {
		helper.assertTrue(ledger.problems().isEmpty(), when + ": the ledger and its cargo agree: " + ledger.problems());
	}

	// ---------------------------------------------------------------- an Alembic Crucible files its own requests (Concordance Chunk C, C-06)

	private static final String GLOW = "minecraft:glowstone_dust";
	private static final Operation.Add RAW = new Operation.Add(GLOW, "jugcraft:raw");
	private static final Operation.Add GROUND_ADD = new Operation.Add(GLOW, "jugcraft:ground");
	private static final Reagent GROUND_GLOW = new Reagent(GLOW, "jugcraft:ground");
	private static final Predicate<ItemStack> PLAIN = stack -> stack.is(Items.GLOWSTONE_DUST);
	private static final Predicate<ItemStack> GROUND = stack -> stack.is(JugcraftConcordance.REAGENT_ITEM) && GROUND_GLOW.equals(stack.get(JugcraftConcordance.REAGENT));
	private static final String ONE_GLOW = "water 1; add minecraft:glowstone_dust jugcraft:raw; stir hot; stir hot";
	private static final String TWO_GLOW = "water 2; add minecraft:glowstone_dust jugcraft:raw; add minecraft:glowstone_dust jugcraft:raw; stir hot; stir hot";
	private static final String TWO_GROUND = "water 2; add minecraft:glowstone_dust jugcraft:ground; add minecraft:glowstone_dust jugcraft:ground; stir hot; stir hot";
	/** A formula's setter who is never online (anOfflineSetterPausesTheCourier). */
	private static final UUID AWAY = new UUID(31L, 7L);

	private static ServerPlayer alembist(GameTestHelper helper) {
		ServerPlayer player = keeper(helper);
		ConcordanceProgress.grant(player, Alchemy.RESEARCH, ResearchState.UNDERSTOOD);
		return player;
	}

	private static CrucibleBlockEntity crucible(GameTestHelper helper, BlockPos at) {
		helper.setBlock(at, JugcraftConcordance.CRUCIBLE);
		return helper.getBlockEntity(at, CrucibleBlockEntity.class);
	}

	private static ChestBlockEntity chest(GameTestHelper helper, BlockPos at) {
		helper.setBlock(at, Blocks.CHEST);
		return helper.getBlockEntity(at, ChestBlockEntity.class);
	}

	private static ItemStack ground(int count) {
		ItemStack reagent = new ItemStack(JugcraftConcordance.REAGENT_ITEM, count);
		reagent.set(JugcraftConcordance.REAGENT, GROUND_GLOW);
		return reagent;
	}

	private static void pour(CrucibleBlockEntity crucible, int parts) {
		try (Transaction transaction = Transaction.openOuter()) {
			crucible.water.insert(FluidVariant.of(Fluids.WATER), parts * FluidConstants.BOTTLE, transaction);
			transaction.commit();
		}
	}

	/** As a player: a written formula used on the crucible; its user is recorded as the setter. */
	private static void setFormula(GameTestHelper helper, CrucibleBlockEntity crucible, ServerPlayer player, String formula) {
		ItemStack written = new ItemStack(JugcraftConcordance.FORMULA_ITEM);
		written.set(JugcraftConcordance.FORMULA, formula);
		RateGate.forget(player.getUUID());
		player.setItemInHand(InteractionHand.MAIN_HAND, written);
		helper.assertTrue(crucible.use(player, helper.getLevel(), written, InteractionHand.MAIN_HAND).consumesAction() && crucible.program() != null
				&& player.getUUID().equals(crucible.formulaOwner()), "The formula is set and its setter recorded: " + formula);
	}

	/** Set by setter, water poured: two pulses reach its first ingredient, which it asks the couriers for; the request. */
	private static long waiting(GameTestHelper helper, CrucibleBlockEntity crucible, ServerPlayer setter, String formula) {
		setFormula(helper, crucible, setter, formula);
		pour(crucible, 2);
		Diagnostic poured = crucible.automate(helper.getLevel());
		Diagnostic waits = crucible.automate(helper.getLevel());
		helper.assertTrue(poured == null && waits != null && waits.code() == Code.MISSING_INGREDIENT && crucible.request() > 0L,
				"It waits for its glowstone and has asked the couriers: " + waits + ", request " + crucible.request());
		return crucible.request();
	}

	private static int matching(Container container, Predicate<ItemStack> which) {
		int count = 0;
		for (int slot = 0; slot < container.getContainerSize(); slot++) {
			if (which.test(container.getItem(slot))) {
				count += container.getItem(slot).getCount();
			}
		}
		return count;
	}

	private static long bound(CourierLedger ledger, Place destination) {
		return ledger.ledger().requests().stream().filter(r -> r.ticket().destination().equals(destination)).count();
	}

	/** After every step (C-06): the ledger agrees with its cargo, and what {@code which} matches is in a container or carried by {@code ids}, {@code n} in all. */
	private static void conserved(GameTestHelper helper, CourierLedger ledger, List<Long> ids, Predicate<ItemStack> which, int n, String when, Container... containers) {
		int held = 0;
		for (Container container : containers) {
			held += matching(container, which);
		}
		int carried = 0;
		for (long id : ids) {
			Request request = ledger.ledger().get(id);
			carried += request == null ? 0 : request.progress().carried();
		}
		helper.assertTrue(ledger.problems().isEmpty(), when + ": the ledger and its cargo agree: " + ledger.problems());
		helper.assertTrue(held + carried == n, when + ": " + held + " held and " + carried + " carried, not " + n);
	}

	/** The kinds of {@code id}'s events in a post's history, oldest first. */
	private static List<String> story(CourierLedger ledger, Place post, long id) {
		return ledger.ledger().history(post).stream().filter(e -> e.request() == id).map(e -> e.kind()).toList();
	}

	/**
	 * {@code during} under {@code rules}, at once, the rules put back whatever happens (C-P28): ConcordanceData's rules are
	 * every test's, and the server runs other tests, so they are never left swapped past this call (check_diagnostics).
	 */
	private static <T> T under(ConcordanceRules rules, Supplier<T> during) {
		ConcordanceRules was = ConcordanceData.rules();
		ConcordanceData.replace(rules);
		try {
			return during.get();
		} finally {
			ConcordanceData.replace(was);
		}
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
	 * A request is filed for exactly an item, claimed, reserved at the chest that has it, picked up into the ledger,
	 * delivered to the post and closed; the history tells each step; the named ingots in the same chest are untouched.
	 */
	@GameTest(maxTicks = 20)
	public void aRequestIsFetchedAndDelivered(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		ServerPlayer keeper = keeper(helper);
		CourierPostBlockEntity post = post(helper, keeper, new BlockPos(2, 1, 2));
		helper.setBlock(new BlockPos(1, 1, 2), Blocks.CHEST);
		ChestBlockEntity chest = (ChestBlockEntity) level.getBlockEntity(helper.absolutePos(new BlockPos(1, 1, 2)));
		chest.setItem(0, new ItemStack(Items.IRON_INGOT, 40));
		ItemStack named = new ItemStack(Items.IRON_INGOT, 5);
		named.set(DataComponents.CUSTOM_NAME, Component.literal("Heirloom"));
		chest.setItem(1, named);
		ClockworkPorterEntity porter = courier(helper, keeper, new BlockPos(2, 1, 2));
		CourierLedger ledger = CourierLedger.of(level.getServer());
		Couriers.request(keeper, level, post, new ItemStack(Items.IRON_INGOT), 32);
		Place at = Couriers.place(level, post.getBlockPos());
		Request request = ledger.ledger().requests().stream().filter(r -> r.ticket().post().equals(at)).findFirst().orElse(null);
		helper.assertTrue(request != null && request.ticket().wanted() == 32 && request.state() == RequestState.OPEN, "Filed: open");
		long id = request.id();
		long now = level.getGameTime() + 400_000L;
		porter.think(level, NIGHT, now);
		helper.assertTrue(porter.task() == id && ledger.ledger().get(id).progress().reserved() == 32, "Claimed and reserved: "
				+ ledger.ledger().get(id).progress());
		porter.think(level, NIGHT, now + 20);
		helper.assertTrue(ledger.ledger().get(id).state() == RequestState.IN_TRANSIT && count(chest, Items.IRON_INGOT) == 13,
				"Picked up 32 plain ingots; the 5 named ones stay: " + count(chest, Items.IRON_INGOT));
		audit(helper, ledger, "in transit");
		porter.think(level, NIGHT, now + 40);
		helper.assertTrue(ledger.ledger().get(id) == null && count(post, Items.IRON_INGOT) == 32 && porter.task() == 0L,
				"Delivered to the post and closed");
		helper.assertTrue(chest.getItem(1).has(DataComponents.CUSTOM_NAME), "The named ingots were never taken");
		java.util.List<String> kinds = ledger.ledger().history(at).stream().map(e -> e.kind()).toList();
		helper.assertTrue(kinds.containsAll(java.util.List.of("filed", "claimed", "reserved", "picked_up", "done")), "The history tells it: " + kinds);
		audit(helper, ledger, "done");
		helper.succeed();
	}

	/** Two couriers and two requests for more than the chest holds: one claim each, never the same items twice. */
	@GameTest(maxTicks = 20)
	public void simultaneousRequestsNeverClaimTheSameItems(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		ServerPlayer keeper = keeper(helper);
		CourierPostBlockEntity post = post(helper, keeper, new BlockPos(2, 1, 2));
		helper.setBlock(new BlockPos(1, 1, 2), Blocks.CHEST);
		ChestBlockEntity chest = (ChestBlockEntity) level.getBlockEntity(helper.absolutePos(new BlockPos(1, 1, 2)));
		chest.setItem(0, new ItemStack(Items.COBBLESTONE, 40));
		ClockworkPorterEntity one = courier(helper, keeper, new BlockPos(2, 1, 2));
		ClockworkPorterEntity two = courier(helper, keeper, new BlockPos(2, 1, 2));
		CourierLedger ledger = CourierLedger.of(level.getServer());
		Couriers.request(keeper, level, post, new ItemStack(Items.COBBLESTONE), 30);
		Couriers.request(keeper, level, post, new ItemStack(Items.COBBLESTONE), 30);
		long now = level.getGameTime() + 500_000L;
		one.think(level, NIGHT, now);
		two.think(level, NIGHT, now);
		helper.assertTrue(one.task() != 0L && two.task() != 0L && one.task() != two.task(), "Each courier holds a different request");
		int reserved = ledger.ledger().get(one.task()).progress().reserved() + ledger.ledger().get(two.task()).progress().reserved();
		helper.assertTrue(reserved == 40, "Together they reserve the chest's 40, not 60: " + reserved);
		// The server-wide ledger also holds cargo from other tests. These synchronous decisions must add exactly
		// our chest's forty items while leaving that existing cargo accounted for.
		int carriedBefore = ledger.ledger().carried().values().stream().mapToInt(Integer::intValue).sum();
		one.think(level, NIGHT, now + 20);
		two.think(level, NIGHT, now + 20);
		int carried = ledger.ledger().get(one.task()).progress().carried() + ledger.ledger().get(two.task()).progress().carried();
		int carriedAfter = ledger.ledger().carried().values().stream().mapToInt(Integer::intValue).sum();
		helper.assertTrue(count(chest, Items.COBBLESTONE) == 0 && carried == 40 && carriedAfter - carriedBefore == 40,
				"40 picked up by these requests, no more: requests=" + carried + ", ledger increase=" + (carriedAfter - carriedBefore));
		audit(helper, ledger, "both carrying");
		helper.succeed();
	}

	/**
	 * A full post takes what fits and the rest stays in transit; the courier removed, its cargo is stranded at the post,
	 * not lost; another courier takes it up; a restart of the ledger (saved and loaded) keeps every count.
	 */
	@GameTest(maxTicks = 20)
	public void fullStorageRemovalAndRestartLoseNothing(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		ServerPlayer keeper = keeper(helper);
		CourierPostBlockEntity post = post(helper, keeper, new BlockPos(2, 1, 2));
		for (int slot = 0; slot < CourierPostBlockEntity.SLOTS - 1; slot++) {
			post.setItem(slot, new ItemStack(Items.DIRT, 64));
		}
		post.setItem(CourierPostBlockEntity.SLOTS - 1, new ItemStack(Items.SAND, 54));
		helper.setBlock(new BlockPos(1, 1, 2), Blocks.CHEST);
		ChestBlockEntity chest = (ChestBlockEntity) level.getBlockEntity(helper.absolutePos(new BlockPos(1, 1, 2)));
		chest.setItem(0, new ItemStack(Items.SAND, 30));
		ClockworkPorterEntity first = courier(helper, keeper, new BlockPos(2, 1, 2));
		CourierLedger ledger = CourierLedger.of(level.getServer());
		Couriers.request(keeper, level, post, new ItemStack(Items.SAND), 30);
		long now = level.getGameTime() + 600_000L;
		first.think(level, NIGHT, now);
		long id = first.task();
		first.think(level, NIGHT, now + 20);
		first.think(level, NIGHT, now + 40);
		helper.assertTrue(count(post, Items.SAND) == 64 && ledger.ledger().get(id).progress().carried() == 20
				&& first.status() == Status.WORKING, "The post takes 10; 20 stay in transit: " + ledger.ledger().get(id).progress());
		helper.assertTrue(first.think(level, NIGHT, now + 60) == Status.FULL, "It says the post is full");
		audit(helper, ledger, "full");
		first.discard();
		helper.assertTrue(ledger.ledger().get(id).state() == RequestState.STRANDED && ledger.ledger().get(id).progress().carried() == 20,
				"Its courier removed: 20 stranded at the post, not dropped and not lost");
		// The ledger saved and loaded again (a restart): the same request, the same cargo.
		TagValueOutput saved = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, level.registryAccess());
		saved.store("ledger", CourierLedger.CODEC, ledger);
		CourierLedger restarted = TagValueInput.create(ProblemReporter.DISCARDING, level.registryAccess(), saved.buildResult())
				.read("ledger", CourierLedger.CODEC).orElseThrow();
		helper.assertTrue(restarted.ledger().get(id) != null && restarted.ledger().get(id).state() == RequestState.STRANDED
				&& restarted.ledger().get(id).progress().carried() == 20 && restarted.item(id) != null && restarted.problems().isEmpty(),
				"After a restart the stranded cargo is there, counted once");
		for (int slot = 0; slot < CourierPostBlockEntity.SLOTS - 1; slot++) {
			post.setItem(slot, ItemStack.EMPTY);
		}
		ClockworkPorterEntity second = courier(helper, keeper, new BlockPos(2, 1, 2));
		second.think(level, NIGHT, now + 80);
		helper.assertTrue(second.task() == id && ledger.ledger().get(id).progress().aboard(), "Another courier takes it up at the post");
		second.think(level, NIGHT, now + 100);
		helper.assertTrue(ledger.ledger().get(id) == null && count(post, Items.SAND) == 84, "and delivers the rest: all 30 arrived, once");
		audit(helper, ledger, "taken up and delivered");
		helper.succeed();
	}

	/** Cancelling with cargo in transit takes it back to its chest; recovering stranded cargo hands it to its requester. */
	@GameTest(maxTicks = 20)
	public void cancellingAndRecoveringGiveItemsBack(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		ServerPlayer keeper = keeper(helper);
		CourierPostBlockEntity post = post(helper, keeper, new BlockPos(2, 1, 2));
		helper.setBlock(new BlockPos(1, 1, 2), Blocks.CHEST);
		ChestBlockEntity chest = (ChestBlockEntity) level.getBlockEntity(helper.absolutePos(new BlockPos(1, 1, 2)));
		chest.setItem(0, new ItemStack(Items.OAK_LOG, 16));
		ClockworkPorterEntity porter = courier(helper, keeper, new BlockPos(2, 1, 2));
		CourierLedger ledger = CourierLedger.of(level.getServer());
		Couriers.request(keeper, level, post, new ItemStack(Items.OAK_LOG), 16);
		long now = level.getGameTime() + 700_000L;
		porter.think(level, NIGHT, now);
		long id = porter.task();
		porter.think(level, NIGHT, now + 20);
		helper.assertTrue(count(chest, Items.OAK_LOG) == 0 && ledger.cancel(id, keeper.getUUID(), now + 30) == Logistics.Outcome.DONE
				&& ledger.ledger().get(id).state() == RequestState.RETURNING, "Cancelled in transit: it is taken back");
		porter.think(level, NIGHT, now + 40);
		helper.assertTrue(ledger.ledger().get(id) == null && count(chest, Items.OAK_LOG) == 16 && count(post, Items.OAK_LOG) == 0,
				"All 16 back in the chest, none at the post");
		// Stranded cargo with no courier: its requester recovers it, once.
		chest.setItem(0, new ItemStack(Items.OAK_LOG, 8));
		Couriers.request(keeper, level, post, new ItemStack(Items.OAK_LOG), 8);
		porter.think(level, NIGHT, now + 60);
		long lost = porter.task();
		porter.think(level, NIGHT, now + 80);
		porter.discard();
		helper.assertTrue(ledger.recover(lost, keeper, now + 90) == Logistics.Outcome.DONE
				&& keeper.getInventory().countItem(Items.OAK_LOG) == 8, "The requester recovers the 8 stranded logs");
		helper.assertTrue(ledger.recover(lost, keeper, now + 91) == Logistics.Outcome.NOT_FOUND && keeper.getInventory().countItem(Items.OAK_LOG) == 8,
				"and never twice");
		audit(helper, ledger, "recovered");
		helper.succeed();
	}

	/** Breaking the post while a courier carries for it sends the cargo back to its chest. */
	@GameTest(maxTicks = 20)
	public void aBrokenPostSendsItsCargoBack(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		ServerPlayer keeper = keeper(helper);
		CourierPostBlockEntity post = post(helper, keeper, new BlockPos(2, 1, 2));
		helper.setBlock(new BlockPos(1, 1, 2), Blocks.CHEST);
		ChestBlockEntity chest = (ChestBlockEntity) level.getBlockEntity(helper.absolutePos(new BlockPos(1, 1, 2)));
		chest.setItem(0, new ItemStack(Items.BRICK, 12));
		ClockworkPorterEntity porter = courier(helper, keeper, new BlockPos(2, 1, 2));
		CourierLedger ledger = CourierLedger.of(level.getServer());
		Couriers.request(keeper, level, post, new ItemStack(Items.BRICK), 12);
		long now = level.getGameTime() + 800_000L;
		porter.think(level, NIGHT, now);
		long id = porter.task();
		porter.think(level, NIGHT, now + 20);
		helper.setBlock(new BlockPos(2, 1, 2), Blocks.AIR);
		helper.assertTrue(ledger.ledger().get(id) != null && ledger.ledger().get(id).state() == RequestState.RETURNING,
				"The post broken: its request turns back");
		helper.assertTrue(ledger.ledger().get(id).progress().carried() == 12 && ledger.problems().isEmpty(), "with all 12 still counted");
		helper.succeed();
	}

	/**
	 * Concordance Chunk C, C-06 (C-D4, C-P13): a crucible waiting for its formula's first ingredient files one request, as
	 * its formula's setter, at the post, for exactly what it lacks (two ground glowstone; the plain glowstone in its buffer
	 * is the wrong preparation and never stands in), and no other while that one is open. A porter brings them; while a
	 * courier holds the request the Peepo port asks for none of it (C-P50); a restart keeps the request, its exact reagent
	 * and its cargo; cargo already carried is delivered even after the shortage falls below it (C-P51: one put in by hand,
	 * which the crucible takes, so one ground glowstone is left over); the formula goes on.
	 */
	@GameTest(maxTicks = 20)
	public void aCrucibleAsksOnceForItsExactShortage(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		ServerPlayer setter = alembist(helper);
		CourierPostBlockEntity post = post(helper, setter, new BlockPos(2, 1, 2));
		ChestBlockEntity chest = chest(helper, new BlockPos(1, 1, 2));
		chest.setItem(0, ground(10));
		chest.setItem(1, new ItemStack(Items.GLOWSTONE_DUST, 7));
		CrucibleBlockEntity crucible = crucible(helper, new BlockPos(3, 1, 2));
		setFormula(helper, crucible, setter, TWO_GROUND);
		pour(crucible, 2);
		crucible.setItem(0, new ItemStack(Items.GLOWSTONE_DUST, 3));
		CourierLedger ledger = CourierLedger.of(level.getServer());
		Place here = Couriers.place(level, crucible.getBlockPos());
		Place at = Couriers.place(level, post.getBlockPos());
		helper.assertTrue(crucible.automate(level) == null && crucible.step() == 1 && crucible.request() == -1L, "The water poured; nothing asked yet");
		Diagnostic wrong = crucible.automate(level);
		long id = crucible.request();
		Request filed = ledger.ledger().get(id);
		helper.assertTrue(wrong != null && wrong.code() == Code.WRONG_PREPARATION && filed != null && filed.ticket().wanted() == 2
				&& filed.ticket().requester().equals(setter.getUUID()) && filed.ticket().post().equals(at) && filed.ticket().destination().equals(here)
				&& ItemVariant.of(ground(1)).equals(ledger.item(id)), "Its setter's post is asked for exactly two ground glowstone: " + wrong + ", " + filed);
		helper.assertTrue(crucible.companion.needed(ground(1)) == 2 && crucible.courierRemainder(GROUND_ADD) == 0,
				"No courier holds it yet, so a Peepo would still bring both");
		long next = ledger.ledger().nextId();
		for (int i = 0; i < 3; i++) {
			crucible.automate(level);
		}
		helper.assertTrue(crucible.request() == id && bound(ledger, here) == 1 && ledger.ledger().nextId() == next,
				"Three more pulses ask nothing more while it is open");
		conserved(helper, ledger, List.of(id), GROUND, 10, "filed", chest, crucible);
		conserved(helper, ledger, List.of(id), PLAIN, 10, "filed (plain)", chest, crucible);
		ClockworkPorterEntity porter = courier(helper, setter, new BlockPos(2, 1, 2));
		long now = level.getGameTime() + 900_000L;
		porter.think(level, NIGHT, now);
		helper.assertTrue(porter.task() == id && ledger.ledger().get(id).progress().reserved() == 2 && crucible.companion.needed(ground(1)) == 0,
				"Claimed and reserved: a Peepo now brings none of it");
		conserved(helper, ledger, List.of(id), GROUND, 10, "claimed", chest, crucible);
		porter.think(level, NIGHT, now + 20);
		helper.assertTrue(ledger.ledger().get(id).state() == RequestState.IN_TRANSIT && matching(chest, GROUND) == 8 && matching(chest, PLAIN) == 7,
				"Two ground glowstone picked up; the plain ones stay");
		conserved(helper, ledger, List.of(id), GROUND, 10, "in transit", chest, crucible);
		// The contract's "server restarts" row (critique correction 6): the ledger saved and loaded keeps the crucible's request.
		TagValueOutput saved = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, level.registryAccess());
		saved.store("ledger", CourierLedger.CODEC, ledger);
		CourierLedger restarted = TagValueInput.create(ProblemReporter.DISCARDING, level.registryAccess(), saved.buildResult())
				.read("ledger", CourierLedger.CODEC).orElseThrow();
		helper.assertTrue(restarted.ledger().get(id) != null && restarted.ledger().get(id).progress().carried() == 2
				&& ItemVariant.of(ground(1)).equals(restarted.item(id)) && restarted.ledger().get(id).ticket().destination().equals(here)
				&& restarted.problems().isEmpty(), "A restart keeps its request, its exact reagent and its cargo");
		// C-P51 (the unit review): cargo already carried is always delivered, though the request now asks more than is lacked.
		crucible.setItem(1, ground(1));
		CrucibleBlockEntity.Shortage fell = crucible.shortage();
		helper.assertTrue(fell != null && fell.count() == 1, "One put in by hand: it lacks one, not two: " + fell);
		helper.assertTrue(crucible.automate(level) == null && crucible.step() == 2 && crucible.request() == id
				&& ledger.ledger().get(id).state() == RequestState.IN_TRANSIT && ledger.ledger().get(id).progress().carried() == 2,
				"It takes the one put in and keeps the request its courier carries for");
		conserved(helper, ledger, List.of(id), GROUND, 10, "carried, the shortage fallen (the one put in, used)", chest, crucible);
		porter.think(level, NIGHT, now + 40);
		helper.assertTrue(ledger.ledger().get(id) == null && matching(crucible, GROUND) == 2 && matching(crucible, PLAIN) == 3 && porter.task() == 0L,
				"Delivered into its buffer and closed");
		conserved(helper, ledger, List.of(id), GROUND, 10, "delivered", chest, crucible);
		helper.assertTrue(crucible.automate(level) == null && crucible.step() == 3 && crucible.request() == -1L && bound(ledger, here) == 0
				&& matching(crucible, GROUND) == 1 && matching(crucible, PLAIN) == 3,
				"It goes on, forgetting the closed request, and takes the second; one is left over and the plain glowstone is untouched");
		helper.assertValueEqual(story(ledger, at, id), List.of("filed", "claimed", "reserved", "picked_up", "done"), "the post's history of it");
		helper.succeed();
	}

	/**
	 * C-06 (C-P13): every way its formula ends turns the crucible's request back: cleared by its setter; replaced by another
	 * formula, its carried cargo going back to the chest (the same formula set again by the same player keeps it); set by
	 * another player; or refused by the rules in force, and asked again once they allow it. Broken, the crucible takes its
	 * last request with it. Nothing is lost or doubled at any step.
	 */
	@GameTest(maxTicks = 20)
	public void aClearedOrReplacedFormulaCancelsItsRequest(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		ServerPlayer setter = alembist(helper);
		ServerPlayer other = alembist(helper);
		post(helper, setter, new BlockPos(2, 1, 2));
		ChestBlockEntity chest = chest(helper, new BlockPos(1, 1, 2));
		chest.setItem(0, new ItemStack(Items.GLOWSTONE_DUST, 10));
		CrucibleBlockEntity cleared = crucible(helper, new BlockPos(3, 1, 2));
		CrucibleBlockEntity replaced = crucible(helper, new BlockPos(3, 1, 3));
		CrucibleBlockEntity taken = crucible(helper, new BlockPos(3, 1, 1));
		CrucibleBlockEntity refused = crucible(helper, new BlockPos(1, 1, 3));
		Container[] all = {chest, cleared, replaced, taken, refused};
		CourierLedger ledger = CourierLedger.of(level.getServer());
		List<Long> ids = new ArrayList<>();
		long first = waiting(helper, cleared, setter, TWO_GLOW);
		ids.add(first);
		cleared.useEmpty(setter, true);
		helper.assertTrue(ledger.ledger().get(first) == null && cleared.request() == -1L && cleared.program() == null && cleared.formulaOwner() == null,
				"Cleared by its setter: its request is turned back and its setter forgotten");
		conserved(helper, ledger, ids, PLAIN, 10, "cleared", all);
		long second = waiting(helper, replaced, setter, TWO_GLOW);
		ids.add(second);
		ClockworkPorterEntity porter = courier(helper, setter, new BlockPos(2, 1, 2));
		long now = level.getGameTime() + 1_000_000L;
		porter.think(level, NIGHT, now);
		porter.think(level, NIGHT, now + 20);
		helper.assertTrue(porter.task() == second && ledger.ledger().get(second).progress().carried() == 2 && matching(chest, PLAIN) == 8,
				"A courier carries two for it");
		conserved(helper, ledger, ids, PLAIN, 10, "carried", all);
		setFormula(helper, replaced, setter, TWO_GLOW);
		helper.assertTrue(replaced.request() == second && ledger.ledger().get(second).state() == RequestState.IN_TRANSIT,
				"The same formula set again by its setter keeps the request");
		conserved(helper, ledger, ids, PLAIN, 10, "set again", all);
		setFormula(helper, replaced, setter, ONE_GLOW);
		helper.assertTrue(replaced.request() == -1L && ledger.ledger().get(second) != null && ledger.ledger().get(second).state() == RequestState.RETURNING
				&& ledger.ledger().get(second).progress().carried() == 2, "Replaced: the two carried are taken back");
		conserved(helper, ledger, ids, PLAIN, 10, "returning", all);
		porter.think(level, NIGHT, now + 40);
		helper.assertTrue(ledger.ledger().get(second) == null && matching(chest, PLAIN) == 10 && matching(replaced, PLAIN) == 0,
				"Back in the chest, none in the crucible");
		conserved(helper, ledger, ids, PLAIN, 10, "returned", all);
		long third = waiting(helper, taken, setter, TWO_GLOW);
		ids.add(third);
		setFormula(helper, taken, other, TWO_GLOW);
		helper.assertTrue(ledger.ledger().get(third) == null && taken.request() == -1L && other.getUUID().equals(taken.formulaOwner()),
				"Set by another player: the first setter's request is turned back");
		conserved(helper, ledger, ids, PLAIN, 10, "taken", all);
		long fourth = waiting(helper, refused, setter, TWO_GLOW);
		ids.add(fourth);
		Diagnostic incompatible = under(ConcordanceRules.EMPTY, () -> refused.automate(level));
		helper.assertTrue(incompatible != null && incompatible.code() == Code.INCOMPATIBLE && ledger.ledger().get(fourth) == null && refused.request() == -1L,
				"A formula the rules refuse turns its request back: " + incompatible);
		Diagnostic again = refused.automate(level);
		long fifth = refused.request();
		ids.add(fifth);
		helper.assertTrue(again != null && again.code() == Code.MISSING_INGREDIENT && fifth > 0L && fifth != fourth
				&& bound(ledger, Couriers.place(level, refused.getBlockPos())) == 1, "The rules restored, it asks again, once: " + again + ", request " + fifth);
		conserved(helper, ledger, ids, PLAIN, 10, "asked again", all);
		helper.setBlock(new BlockPos(1, 1, 3), Blocks.AIR);
		helper.assertTrue(ledger.ledger().get(fifth) == null, "Broken, the crucible takes its request with it");
		helper.succeed();
	}

	/**
	 * C-06 (C-P13, C-P52): with its setter's sixteen requests open, the crucible files none, says why (the count, what, and
	 * when it asks again) and backs off 200 ticks, doubling to 1,200; a Peepo reads that as missing input and may still
	 * bring it. Room again, it waits out the backoff and its own pulse files the request, once.
	 */
	@GameTest(maxTicks = 300)
	public void aFullCourierQuotaWaitsAndBacksOff(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		ServerPlayer setter = alembist(helper);
		CourierPostBlockEntity post = post(helper, setter, new BlockPos(2, 1, 2));
		CrucibleBlockEntity crucible = crucible(helper, new BlockPos(3, 1, 2));
		setFormula(helper, crucible, setter, TWO_GLOW);
		pour(crucible, 2);
		CourierLedger ledger = CourierLedger.of(level.getServer());
		Place at = Couriers.place(level, post.getBlockPos());
		Place here = Couriers.place(level, crucible.getBlockPos());
		List<Long> fillers = new ArrayList<>();
		for (int i = 0; i < Logistics.MAX_OPEN_PER_PLAYER; i++) {
			Logistics.Filed filler = ledger.file(setter.getUUID(), at, at, new ItemStack(Items.COBBLESTONE), 1, level.registryAccess(), level.getGameTime());
			helper.assertTrue(filler.outcome() == Logistics.Outcome.DONE, "Filler " + i + " filed: " + filler);
			fillers.add(filler.id());
		}
		helper.assertTrue(crucible.automate(level) == null && crucible.step() == 1, "The water poured");
		long next = ledger.ledger().nextId();
		Diagnostic quota = Diagnostic.courierQuota(2, RAW.describe(), CrucibleBlockEntity.FILING_BACKOFF / 20);
		long start = level.getGameTime();
		Diagnostic full = crucible.automate(level);
		helper.assertTrue(quota.equals(full) && quota.equals(crucible.waiting()) && crucible.request() == -1L && ledger.ledger().nextId() == next
				&& ledger.ledger().openFor(setter.getUUID()) == Logistics.MAX_OPEN_PER_PLAYER,
				"Its setter's sixteen are open: it says so and files nothing: " + full);
		helper.assertTrue(crucible.companion.status() == CompanionStatus.NO_INPUT && crucible.companion.needed(new ItemStack(Items.GLOWSTONE_DUST)) == 2,
				"A Peepo reads the quota wait as missing input and may still bring both");
		helper.assertTrue(ledger.cancel(fillers.getFirst(), setter.getUUID(), level.getGameTime()) == Logistics.Outcome.DONE
				&& ledger.ledger().openFor(setter.getUUID()) == Logistics.MAX_OPEN_PER_PLAYER - 1, "One of the sixteen cancelled");
		Diagnostic still = crucible.automate(level);
		helper.assertTrue(quota.equals(still) && crucible.request() == -1L && ledger.ledger().nextId() == next,
				"Room again, it waits out its backoff: " + still);
		helper.assertTrue(CrucibleBlockEntity.backoffAfter(0) == 200 && CrucibleBlockEntity.backoffAfter(200) == 400
				&& CrucibleBlockEntity.backoffAfter(400) == 800 && CrucibleBlockEntity.backoffAfter(800) == 1200
				&& CrucibleBlockEntity.backoffAfter(1200) == 1200, "The backoff doubles from 200 ticks to 1,200");
		helper.succeedWhen(() -> {
			long asked = crucible.request();
			Request open = ledger.ledger().get(asked);
			helper.assertTrue(asked > 0L && open != null && open.ticket().wanted() == 2 && open.ticket().destination().equals(here)
					&& open.ticket().filed() >= start + CrucibleBlockEntity.FILING_BACKOFF
					&& open.ticket().filed() < start + CrucibleBlockEntity.FILING_BACKOFF + CrucibleBlockEntity.AUTOMATION_TICKS
					&& ledger.ledger().openFor(setter.getUUID()) == Logistics.MAX_OPEN_PER_PLAYER
					&& crucible.waiting() != null && crucible.waiting().code() == Code.MISSING_INGREDIENT,
					"Its own pulse files once the backoff is over: request " + asked + ", " + open);
			// Broken, the crucible turns its request back and forgets its formula, so it files nothing more (the unit review).
			ledger.destinationGone(at, level.getGameTime());
			helper.setBlock(new BlockPos(3, 1, 2), Blocks.AIR);
		});
	}

	/**
	 * C-06: breaking a crucible a courier carries for turns its request back (destination_gone): the carried cargo goes
	 * back to its chest, never dropped; the crucible's own slots drop once, as before.
	 */
	@GameTest(maxTicks = 20)
	public void aBrokenCrucibleTurnsItsRequestBack(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		ServerPlayer setter = alembist(helper);
		post(helper, setter, new BlockPos(2, 1, 2));
		ChestBlockEntity chest = chest(helper, new BlockPos(1, 1, 2));
		chest.setItem(0, new ItemStack(Items.GLOWSTONE_DUST, 10));
		BlockPos at = new BlockPos(3, 1, 2);
		CrucibleBlockEntity crucible = crucible(helper, at);
		crucible.setItem(1, new ItemStack(Items.SPIDER_EYE, 3));
		long id = waiting(helper, crucible, setter, TWO_GLOW);
		CourierLedger ledger = CourierLedger.of(level.getServer());
		ClockworkPorterEntity porter = courier(helper, setter, new BlockPos(2, 1, 2));
		long now = level.getGameTime() + 1_200_000L;
		porter.think(level, NIGHT, now);
		porter.think(level, NIGHT, now + 20);
		helper.assertTrue(ledger.ledger().get(id).progress().carried() == 2 && matching(chest, PLAIN) == 8, "A courier carries two for it");
		conserved(helper, ledger, List.of(id), PLAIN, 10, "carried", chest, crucible);
		helper.destroyBlock(at);
		Request back = ledger.ledger().get(id);
		helper.assertTrue(back != null && back.state() == RequestState.RETURNING && back.progress().carried() == 2
				&& "destination_gone".equals(back.progress().note()), "Broken: its request turns back with the two carried: " + back);
		int eyes = 0;
		int glowstone = 0;
		for (ItemEntity item : level.getEntitiesOfClass(ItemEntity.class, new AABB(helper.absolutePos(at)).inflate(3.0))) {
			eyes += item.getItem().is(Items.SPIDER_EYE) ? item.getItem().getCount() : 0;
			glowstone += item.getItem().is(Items.GLOWSTONE_DUST) ? item.getItem().getCount() : 0;
		}
		helper.assertTrue(eyes == 3 && glowstone == 0, "Its slots drop once; the carried glowstone is not dropped: " + eyes + " eyes, " + glowstone + " glowstone");
		conserved(helper, ledger, List.of(id), PLAIN, 10, "broken", chest);
		porter.think(level, NIGHT, now + 40);
		helper.assertTrue(ledger.ledger().get(id) == null && matching(chest, PLAIN) == 10, "The two are back in the chest");
		conserved(helper, ledger, List.of(id), PLAIN, 10, "returned", chest);
		helper.succeed();
	}

	/**
	 * C-06, 03:193's "destination fills": a full crucible takes what fits; the rest stays aboard, noted "full", and the
	 * courier says so; with room again it is delivered, once. Never into its bottle or output slots.
	 */
	@GameTest(maxTicks = 20)
	public void aCourierFillsACrucibleUntilItIsFull(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		ServerPlayer setter = alembist(helper);
		CourierPostBlockEntity post = post(helper, setter, new BlockPos(2, 1, 2));
		ChestBlockEntity chest = chest(helper, new BlockPos(1, 1, 2));
		chest.setItem(0, new ItemStack(Items.GLOWSTONE_DUST, 10));
		CrucibleBlockEntity crucible = crucible(helper, new BlockPos(3, 1, 2));
		long id = waiting(helper, crucible, setter, TWO_GLOW);
		CourierLedger ledger = CourierLedger.of(level.getServer());
		Place at = Couriers.place(level, post.getBlockPos());
		ClockworkPorterEntity porter = courier(helper, setter, new BlockPos(2, 1, 2));
		long now = level.getGameTime() + 1_400_000L;
		porter.think(level, NIGHT, now);
		porter.think(level, NIGHT, now + 20);
		helper.assertTrue(ledger.ledger().get(id).progress().carried() == 2, "A courier carries two for it");
		for (int slot = 0; slot < 4; slot++) {
			crucible.setItem(slot, new ItemStack(Items.SPIDER_EYE, 64));
		}
		crucible.setItem(4, new ItemStack(Items.GLOWSTONE_DUST, 63));
		conserved(helper, ledger, List.of(id), PLAIN, 73, "filled", chest, crucible);
		helper.assertTrue(porter.think(level, NIGHT, now + 40) == Status.WORKING, "It delivers what fits");
		Request part = ledger.ledger().get(id);
		helper.assertTrue(part != null && part.progress().delivered() == 1 && part.progress().carried() == 1 && part.progress().aboard()
				&& "full".equals(part.progress().note()) && matching(crucible, PLAIN) == 64, "One fitted; one stays aboard, noted full: " + part);
		conserved(helper, ledger, List.of(id), PLAIN, 73, "one fitted", chest, crucible);
		helper.assertTrue(porter.think(level, NIGHT, now + 60) == Status.FULL && ledger.ledger().get(id).progress().carried() == 1,
				"Full: it says so and keeps the rest");
		helper.assertTrue(crucible.getItem(CrucibleBlockEntity.BOTTLE_SLOT).isEmpty() && crucible.getItem(CrucibleBlockEntity.OUTPUT_SLOT).isEmpty(),
				"Nothing went into its bottle or output slots");
		conserved(helper, ledger, List.of(id), PLAIN, 73, "full", chest, crucible);
		crucible.setItem(0, ItemStack.EMPTY);
		helper.assertTrue(porter.think(level, NIGHT, now + 80) == Status.WORKING && ledger.ledger().get(id) == null && matching(crucible, PLAIN) == 65,
				"Room again: the last one delivered and the request closed");
		conserved(helper, ledger, List.of(id), PLAIN, 73, "delivered", chest, crucible);
		List<Event> events = ledger.ledger().history(at).stream().filter(e -> e.request() == id).toList();
		helper.assertValueEqual(events.stream().map(Event::kind).toList(), List.of("filed", "claimed", "reserved", "picked_up", "delivered", "done"),
				"the post's history of it");
		helper.assertTrue("full".equals(events.get(4).note()), "the part delivery is noted full: " + events.get(4));
		helper.succeed();
	}

	/**
	 * C-06, the unit review: a crucible whose buffer is full of other things files nothing, since a porter could only wait
	 * at it, FULL, holding its post's queue; one slot emptied, its next pulse files at once (no backoff), for what fits.
	 */
	@GameTest(maxTicks = 20)
	public void aCrucibleWithNoRoomAsksForNothing(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		ServerPlayer setter = alembist(helper);
		post(helper, setter, new BlockPos(2, 1, 2));
		CrucibleBlockEntity crucible = crucible(helper, new BlockPos(3, 1, 2));
		setFormula(helper, crucible, setter, TWO_GLOW);
		pour(crucible, 2);
		for (int slot = 0; slot < CrucibleBlockEntity.BUFFER_SLOTS; slot++) {
			crucible.setItem(slot, new ItemStack(Items.SPIDER_EYE));
		}
		CourierLedger ledger = CourierLedger.of(level.getServer());
		Place here = Couriers.place(level, crucible.getBlockPos());
		helper.assertTrue(crucible.automate(level) == null && crucible.step() == 1, "The water poured");
		long next = ledger.ledger().nextId();
		Diagnostic full = crucible.automate(level);
		helper.assertTrue(full != null && full.code() == Code.MISSING_INGREDIENT && crucible.request() == -1L && ledger.ledger().nextId() == next
				&& bound(ledger, here) == 0, "Five slots of spider eyes: it waits and files nothing: " + full);
		crucible.setItem(0, ItemStack.EMPTY);
		Diagnostic room = crucible.automate(level);
		long id = crucible.request();
		Request filed = ledger.ledger().get(id);
		helper.assertTrue(room != null && room.code() == Code.MISSING_INGREDIENT && id > 0L && filed != null && filed.ticket().wanted() == 2
				&& filed.ticket().requester().equals(setter.getUUID()) && filed.ticket().destination().equals(here) && bound(ledger, here) == 1,
				"One slot emptied, its next pulse files at once: " + room + ", " + filed);
		crucible.useEmpty(setter, true);
		helper.assertTrue(ledger.ledger().get(id) == null && crucible.request() == -1L, "Cleared, it leaves no request in the shared ledger");
		helper.succeed();
	}

	/**
	 * C-06, 03:193's "access removed": the crucible files only at a post its setter may use, never a nearer one of someone
	 * else's; when its post is barred to its setter, it turns the request back and asks no post it may not use.
	 */
	@GameTest(maxTicks = 20)
	public void aPostTheSetterMayNotUseIsNeverAsked(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		ServerPlayer setter = alembist(helper);
		ServerPlayer stranger = keeper(helper);
		CourierPostBlockEntity theirs = post(helper, stranger, new BlockPos(2, 1, 2));
		CourierPostBlockEntity mine = post(helper, setter, new BlockPos(2, 1, 6));
		CrucibleBlockEntity crucible = crucible(helper, new BlockPos(3, 1, 2));
		BlockPos at = crucible.getBlockPos();
		Place here = Couriers.place(level, at);
		helper.assertTrue(Couriers.nearestPost(level, at, setter.getUUID(), CrucibleBlockEntity.POST_REACH) == mine
				&& Couriers.nearestPost(level, at, stranger.getUUID(), CrucibleBlockEntity.POST_REACH) == theirs,
				"The nearest post each may use: the setter's own, though the stranger's is nearer");
		CourierLedger ledger = CourierLedger.of(level.getServer());
		long k = ledger.ledger().nextId();
		long id = waiting(helper, crucible, setter, TWO_GLOW);
		Place minePlace = Couriers.place(level, mine.getBlockPos());
		helper.assertTrue(ledger.ledger().get(id).ticket().post().equals(minePlace) && ledger.ledger().nextId() == k + 1,
				"It asked at its setter's post, once");
		mine.setOwner(stranger.getUUID());
		Diagnostic barred = crucible.automate(level);
		helper.assertTrue(ledger.ledger().get(id) == null && crucible.request() == -1L && bound(ledger, here) == 0
				&& barred != null && barred.code() == Code.MISSING_INGREDIENT, "Its post barred to its setter: the request is turned back: " + barred);
		crucible.automate(level);
		helper.assertTrue(crucible.request() == -1L && ledger.ledger().nextId() == k + 1, "and no post it may not use is asked");
		helper.assertValueEqual(story(ledger, minePlace, id), List.of("filed", "cancelled"), "the post's history of it");
		helper.succeed();
	}

	/**
	 * C-06, 03:193's "target chunk unloads": a courier carrying for a crucible whose chunk is not loaded waits there,
	 * DESTINATION_UNLOADED, with no ledger step but its lease renewed; a restart keeps the cargo; cancelled, it goes back.
	 */
	@GameTest(maxTicks = 20)
	public void anUnloadedCrucibleWaitsAndLosesNothing(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		ServerPlayer setter = alembist(helper);
		CourierPostBlockEntity post = post(helper, setter, new BlockPos(2, 1, 2));
		ChestBlockEntity chest = chest(helper, new BlockPos(1, 1, 2));
		chest.setItem(0, new ItemStack(Items.GLOWSTONE_DUST, 10));
		BlockPos away = helper.absolutePos(new BlockPos(3, 1, 2)).offset(100_000, 0, 0);
		helper.assertTrue(!level.isLoaded(away), "The crucible's chunk is not loaded");
		CourierLedger ledger = CourierLedger.of(level.getServer());
		Place at = Couriers.place(level, post.getBlockPos());
		// Stands for a request the crucible filed while it was loaded.
		Logistics.Filed filed = ledger.file(setter.getUUID(), at, Couriers.place(level, away), CrucibleBlockEntity.sample(RAW), 2, level.registryAccess(),
				level.getGameTime());
		helper.assertTrue(filed.outcome() == Logistics.Outcome.DONE, "Filed: " + filed);
		long id = filed.id();
		ClockworkPorterEntity porter = courier(helper, setter, new BlockPos(2, 1, 2));
		long now = level.getGameTime() + 1_300_000L;
		porter.think(level, NIGHT, now);
		porter.think(level, NIGHT, now + 20);
		helper.assertTrue(ledger.ledger().get(id).state() == RequestState.IN_TRANSIT && ledger.ledger().get(id).progress().carried() == 2
				&& matching(chest, PLAIN) == 8, "Two picked up for it");
		conserved(helper, ledger, List.of(id), PLAIN, 10, "in transit", chest);
		for (int i = 1; i <= 2; i++) {
			long then = now + 20 + 20L * i;
			Status waits = porter.think(level, NIGHT, then);
			Request held = ledger.ledger().get(id);
			helper.assertTrue(waits == Status.DESTINATION_UNLOADED && held.progress().carried() == 2 && held.progress().delivered() == 0
					&& held.progress().aboard() && held.progress().lease() == then + Logistics.LEASE_TICKS,
					"It waits with the cargo aboard, its claim renewed: " + waits + ", " + held.progress());
			conserved(helper, ledger, List.of(id), PLAIN, 10, "unloaded " + i, chest);
		}
		TagValueOutput saved = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, level.registryAccess());
		saved.store("ledger", CourierLedger.CODEC, ledger);
		CourierLedger restarted = TagValueInput.create(ProblemReporter.DISCARDING, level.registryAccess(), saved.buildResult())
				.read("ledger", CourierLedger.CODEC).orElseThrow();
		helper.assertTrue(restarted.ledger().get(id) != null && restarted.ledger().get(id).state() == RequestState.IN_TRANSIT
				&& restarted.ledger().get(id).progress().carried() == 2 && ItemVariant.of(new ItemStack(Items.GLOWSTONE_DUST)).equals(restarted.item(id))
				&& restarted.problems().isEmpty(), "A restart keeps the cargo, counted once");
		helper.assertTrue(ledger.cancel(id, setter.getUUID(), now + 70) == Logistics.Outcome.DONE && ledger.ledger().get(id).state() == RequestState.RETURNING,
				"Cancelled: it is taken back");
		porter.think(level, NIGHT, now + 80);
		helper.assertTrue(ledger.ledger().get(id) == null && matching(chest, PLAIN) == 10, "All ten back in the chest");
		conserved(helper, ledger, List.of(id), PLAIN, 10, "returned", chest);
		helper.succeed();
	}

	/**
	 * C-06, 03:193's "entity dies": a porter discarded with cargo aboard strands it, counted, never dropped; the Peepo port
	 * no longer counts it (no courier holds it); its requester recovers it exactly once; the crucible then asks again.
	 */
	@GameTest(maxTicks = 20)
	public void aDiscardedPorterStrandsItsCargoOnce(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		ServerPlayer setter = alembist(helper);
		post(helper, setter, new BlockPos(2, 1, 2));
		ChestBlockEntity chest = chest(helper, new BlockPos(1, 1, 2));
		chest.setItem(0, new ItemStack(Items.GLOWSTONE_DUST, 10));
		BlockPos at = new BlockPos(3, 1, 2);
		CrucibleBlockEntity crucible = crucible(helper, at);
		long id = waiting(helper, crucible, setter, TWO_GLOW);
		CourierLedger ledger = CourierLedger.of(level.getServer());
		ClockworkPorterEntity porter = courier(helper, setter, new BlockPos(2, 1, 2));
		long now = level.getGameTime() + 1_500_000L;
		porter.think(level, NIGHT, now);
		porter.think(level, NIGHT, now + 20);
		helper.assertTrue(ledger.ledger().get(id).progress().carried() == 2, "A courier carries two for it");
		BlockPos where = porter.blockPosition();
		porter.discard();
		Request stranded = ledger.ledger().get(id);
		helper.assertTrue(stranded != null && stranded.state() == RequestState.STRANDED && stranded.progress().carried() == 2
				&& "worker_removed".equals(stranded.progress().note()), "Its courier gone: the two are stranded, counted: " + stranded);
		int dropped = 0;
		for (ItemEntity item : level.getEntitiesOfClass(ItemEntity.class, new AABB(where).inflate(3.0))) {
			dropped += item.getItem().is(Items.GLOWSTONE_DUST) ? item.getItem().getCount() : 0;
		}
		helper.assertTrue(dropped == 0, "Nothing dropped where it stood: " + dropped);
		helper.assertTrue(crucible.courierRemainder(RAW) == 0 && crucible.companion.needed(new ItemStack(Items.GLOWSTONE_DUST)) == 2,
				"No courier holds it: a Peepo may bring both");
		conserved(helper, ledger, List.of(id), PLAIN, 10, "stranded", chest, crucible);
		helper.assertTrue(ledger.recover(id, setter, now + 30) == Logistics.Outcome.DONE && setter.getInventory().countItem(Items.GLOWSTONE_DUST) == 2
				&& ledger.ledger().get(id) == null, "Its requester recovers the two");
		helper.assertTrue(ledger.recover(id, setter, now + 31) == Logistics.Outcome.NOT_FOUND && setter.getInventory().countItem(Items.GLOWSTONE_DUST) == 2,
				"and never twice");
		conserved(helper, ledger, List.of(id), PLAIN, 10, "recovered", chest, crucible, setter.getInventory());
		long k = ledger.ledger().nextId();
		Diagnostic waits = crucible.automate(level);
		long again = crucible.request();
		helper.assertTrue(waits != null && waits.code() == Code.MISSING_INGREDIENT && again > 0L && again != id && ledger.ledger().nextId() == k + 1
				&& ledger.ledger().get(again).ticket().wanted() == 2, "The request closed, it asks again for both: " + waits + ", request " + again);
		helper.setBlock(at, Blocks.AIR);
		helper.assertTrue(ledger.ledger().get(again) == null, "Broken, its request goes with it");
		helper.succeed();
	}

	/**
	 * C-06, 03:193's "owner logs out" (C-P53): a crucible whose setter is away files nothing, unless the server lets absent
	 * owners' devices act; a request filed for a setter who is away is served only while someone answers for them: the
	 * porter waits, OWNER_OFFLINE, and the Peepo port may still bring it all; with the stand-in, it is fetched.
	 */
	@GameTest(maxTicks = 20)
	public void anOfflineSetterPausesTheCourier(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		ServerPlayer keeper = keeper(helper);
		PartyManager parties = JugcraftParties.manager();
		long ms = System.currentTimeMillis();
		try {
			helper.assertTrue(parties.invite(keeper.getUUID(), AWAY, ms) == PartyManager.Result.OK && parties.accept(AWAY, ms) == PartyManager.Result.OK,
					"The keeper and the absent setter form a party");
			post(helper, keeper, new BlockPos(2, 1, 2));
			ChestBlockEntity chest = chest(helper, new BlockPos(1, 1, 2));
			chest.setItem(0, new ItemStack(Items.GLOWSTONE_DUST, 10));
			CrucibleBlockEntity crucible = crucible(helper, new BlockPos(3, 1, 2));
			crucible.follow(Formula.parse(TWO_GLOW), AWAY);
			pour(crucible, 2);
			CourierLedger ledger = CourierLedger.of(level.getServer());
			long next = ledger.ledger().nextId();
			Diagnostic poured = crucible.automate(level);
			Diagnostic away = crucible.automate(level);
			helper.assertTrue(poured == null && away != null && away.code() == Code.MISSING_INGREDIENT && crucible.request() == -1L
					&& ledger.ledger().nextId() == next, "Away, with the option off, it files nothing: " + away);
			withAbsentOwners(() -> crucible.automate(level));
			long id = crucible.request();
			Request open = ledger.ledger().get(id);
			helper.assertTrue(id > 0L && open != null && open.ticket().requester().equals(AWAY) && ledger.ledger().nextId() == next + 1,
					"With the server's option, it files as its absent setter: " + open);
			ClockworkPorterEntity porter = courier(helper, keeper, new BlockPos(2, 1, 2));
			long now = level.getGameTime() + 1_600_000L;
			for (long then : new long[] {now, now + 20}) {
				helper.assertTrue(porter.think(level, NIGHT, then) == Status.OWNER_OFFLINE && porter.task() == 0L
						&& ledger.ledger().get(id).state() == RequestState.OPEN && matching(chest, PLAIN) == 10
						&& crucible.companion.needed(new ItemStack(Items.GLOWSTONE_DUST)) == 2,
						"Its setter away, the porter waits and a Peepo may bring both");
				conserved(helper, ledger, List.of(id), PLAIN, 10, "offline", chest, crucible);
			}
			withAbsentOwners(() -> {
				helper.assertTrue(porter.think(level, NIGHT, now + 40) == Status.TRAVELLING && crucible.companion.needed(new ItemStack(Items.GLOWSTONE_DUST)) == 0,
						"A stand-in answers: the porter claims it, and a Peepo brings none");
				conserved(helper, ledger, List.of(id), PLAIN, 10, "claimed", chest, crucible);
				porter.think(level, NIGHT, now + 60);
				helper.assertTrue(ledger.ledger().get(id).progress().carried() == 2 && matching(chest, PLAIN) == 8, "picks up two");
				conserved(helper, ledger, List.of(id), PLAIN, 10, "carried", chest, crucible);
				porter.think(level, NIGHT, now + 80);
				helper.assertTrue(ledger.ledger().get(id) == null && matching(crucible, PLAIN) == 2 && matching(chest, PLAIN) == 8, "and delivers them");
				conserved(helper, ledger, List.of(id), PLAIN, 10, "delivered", chest, crucible);
			});
		} finally {
			parties.adminDisband(keeper.getUUID());
		}
		helper.succeed();
	}

	// ---------------------------------------------------------------- a workshop hand serves the post (Concordance Chunk D, D-08)

	/**
	 * D-08, 03:193's "destination fills": a full crucible takes what fits; the rest stays aboard, noted full, and the hand
	 * says FULL; with room again it is delivered once, never into the bottle or output slots. While it holds the request a
	 * Peepo asks for none of it; the load delivered in full is one of its day's tasks, the part delivery none.
	 */
	@GameTest(maxTicks = 20)
	public void aHandFillsACrucibleUntilItIsFull(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		WorkshopHands.Workshop shop = WorkshopHands.workshop(helper);
		WorkshopHandEntity hand = shop.hand();
		CrucibleBlockEntity crucible = shop.crucible();
		long id = shop.request();
		List<Long> ids = List.of(id);
		CourierLedger ledger = CourierLedger.of(level.getServer());
		long now = WorkshopHands.morning(level, 2_100_000L);
		long in = WorkshopHands.inHours();
		helper.assertTrue(hand.think(level, in, now) == Status.TRAVELLING && hand.task() == id && ledger.ledger().get(id).progress().reserved() == 2
				&& crucible.courierRemainder(RAW) == 2 && crucible.companion.needed(new ItemStack(Items.GLOWSTONE_DUST)) == 0,
				"The hand claims the crucible's request and reserves two: a Peepo now brings none of it");
		WorkshopHands.conserved(helper, ids, PLAIN, 10, 0, "claimed", shop.chest(), crucible);
		helper.assertTrue(hand.think(level, in, now + 20) == Status.WORKING && ledger.ledger().get(id).progress().carried() == 2
				&& matching(shop.chest(), PLAIN) == 8 && hand.carriedCount() == 2, "It picks up two");
		WorkshopHands.conserved(helper, ids, PLAIN, 10, 0, "carried", shop.chest(), crucible);
		for (int slot = 0; slot < 4; slot++) {
			crucible.setItem(slot, new ItemStack(Items.SPIDER_EYE, 64));
		}
		crucible.setItem(4, new ItemStack(Items.GLOWSTONE_DUST, 63));
		WorkshopHands.conserved(helper, ids, PLAIN, 73, 0, "filled", shop.chest(), crucible);
		helper.assertTrue(hand.think(level, in, now + 40) == Status.WORKING, "It delivers what fits");
		Request part = ledger.ledger().get(id);
		helper.assertTrue(part != null && part.progress().delivered() == 1 && part.progress().carried() == 1 && part.progress().aboard()
				&& "full".equals(part.progress().note()) && matching(crucible, PLAIN) == 64 && shop.anchor().agreement().done(now + 40) == 0,
				"One fitted; one stays aboard, noted full; no load done yet: " + part);
		WorkshopHands.conserved(helper, ids, PLAIN, 73, 0, "one fitted", shop.chest(), crucible);
		helper.assertTrue(hand.think(level, in, now + 60) == Status.FULL && ledger.ledger().get(id).progress().carried() == 1,
				"Full: it says so and keeps the rest aboard");
		helper.assertTrue(crucible.getItem(CrucibleBlockEntity.BOTTLE_SLOT).isEmpty() && crucible.getItem(CrucibleBlockEntity.OUTPUT_SLOT).isEmpty(),
				"Nothing went into its bottle or output slots");
		WorkshopHands.conserved(helper, ids, PLAIN, 73, 0, "full", shop.chest(), crucible);
		crucible.setItem(0, ItemStack.EMPTY);
		helper.assertTrue(hand.think(level, in, now + 80) == Status.WORKING && ledger.ledger().get(id) == null && matching(crucible, PLAIN) == 65
				&& shop.anchor().agreement().done(now + 80) == 1, "Room again: the last one delivered, the request closed, one load counted");
		WorkshopHands.conserved(helper, ids, PLAIN, 73, 0, "delivered", shop.chest(), crucible);
		helper.assertValueEqual(WorkshopHands.story(helper, WorkshopHands.POST, id), List.of("filed", "claimed", "reserved", "picked_up", "delivered", "done"),
				"the post's history of it");
		helper.succeed();
	}

	/**
	 * D-08, 03:193's "player removes access": a source its holder may no longer open (a neighbour's claim) releases the claim
	 * reserved there, blocked, and is never reserved again; a post its holder may no longer use is served by none of its
	 * hands; access restored, the request is served once.
	 */
	@GameTest(maxTicks = 20)
	public void aHandTakesNothingWhereItsHolderMayNot(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		WorkshopHands.Workshop shop = WorkshopHands.workshop(helper);
		WorkshopHandEntity hand = shop.hand();
		long id = shop.request();
		List<Long> ids = List.of(id);
		CourierLedger ledger = CourierLedger.of(level.getServer());
		long now = WorkshopHands.morning(level, 2_200_000L);
		long in = WorkshopHands.inHours();
		helper.assertTrue(hand.think(level, in, now) == Status.TRAVELLING && ledger.ledger().get(id).progress().reserved() == 2, "Claimed and reserved at the chest");
		WorkshopHands.conserved(helper, ids, PLAIN, 10, 0, "reserved", shop.chest(), shop.crucible());
		long retry = now + 20 + Logistics.RETRY_TICKS;
		try (TestClaims claims = TestClaims.open()) {
			claims.block(helper.absolutePos(WorkshopHands.CHEST), WorkshopHands.NEIGHBOUR);
			Status blocked = hand.think(level, in, now + 20);
			Request released = ledger.ledger().get(id);
			helper.assertTrue(blocked == Status.BLOCKED_BY_ACCESS && hand.task() == 0L && released.state() == RequestState.OPEN
					&& released.progress().reserved() == 0 && "blocked".equals(released.progress().note()) && matching(shop.chest(), PLAIN) == 10,
					"A neighbour's claim over its chest: the hand gives up its claim, blocked, and takes nothing: " + released);
			WorkshopHands.conserved(helper, ids, PLAIN, 10, 0, "blocked", shop.chest(), shop.crucible());
			Status nowhere = hand.think(level, in, retry);
			helper.assertTrue(nowhere == Status.WAITING_FOR_RESOURCES && hand.task() == 0L && "no_source".equals(ledger.ledger().get(id).progress().note())
					&& matching(shop.chest(), PLAIN) == 10, "Tried again, it reserves nowhere its holder may not open: " + nowhere);
			WorkshopHands.conserved(helper, ids, PLAIN, 10, 0, "no source", shop.chest(), shop.crucible());
		}
		long barred = retry + Logistics.RETRY_TICKS;
		shop.post().setOwner(WorkshopHands.NEIGHBOUR);
		helper.assertTrue(hand.think(level, in, barred) == Status.BLOCKED_BY_ACCESS && hand.task() == 0L && ledger.ledger().get(id).progress().worker() == null,
				"Its post barred to its holder, the hand serves none of its requests");
		WorkshopHands.conserved(helper, ids, PLAIN, 10, 0, "post barred", shop.chest(), shop.crucible());
		shop.post().setOwner(shop.holder().getUUID());
		helper.assertTrue(hand.think(level, in, barred + 20) == Status.TRAVELLING && hand.task() == id, "Access restored, it claims the request again");
		WorkshopHands.conserved(helper, ids, PLAIN, 10, 0, "claimed again", shop.chest(), shop.crucible());
		helper.assertTrue(hand.think(level, in, barred + 40) == Status.WORKING && matching(shop.chest(), PLAIN) == 8, "picks up two");
		WorkshopHands.conserved(helper, ids, PLAIN, 10, 0, "picked up", shop.chest(), shop.crucible());
		helper.assertTrue(hand.think(level, in, barred + 60) == Status.WORKING && ledger.ledger().get(id) == null && matching(shop.crucible(), PLAIN) == 2,
				"and delivers them, once");
		WorkshopHands.conserved(helper, ids, PLAIN, 10, 0, "delivered", shop.chest(), shop.crucible());
		helper.assertValueEqual(WorkshopHands.story(helper, WorkshopHands.POST, id),
				List.of("filed", "claimed", "reserved", "released", "claimed", "released", "claimed", "reserved", "picked_up", "done"), "the post's history of it");
		helper.succeed();
	}

	/**
	 * D-08, 03:193's "path fails three times": a hand that cannot reach the crucible gives up its claim at the third failed
	 * walk; the cargo is stranded at the post, counted, never dropped; a Peepo may bring it all; its requester recovers it once.
	 */
	@GameTest(maxTicks = 20)
	public void aHandThatCannotFindItsWayStrandsItsCargoOnce(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		ServerPlayer holder = WorkshopHands.holder(helper);
		WorkshopHands.post(helper, holder, WorkshopHands.POST);
		ChestBlockEntity chest = WorkshopHands.chest(helper, WorkshopHands.CHEST, 10);
		CrucibleBlockEntity crucible = WorkshopHands.crucible(helper, new BlockPos(6, 1, 2), holder);
		long id = crucible.request();
		List<Long> ids = List.of(id);
		WorkshopHandEntity hand = WorkshopHands.hand(helper, holder, WorkshopHands.ANCHOR, WorkshopHands.POST);
		CourierLedger ledger = CourierLedger.of(level.getServer());
		long now = WorkshopHands.morning(level, 2_300_000L);
		long in = WorkshopHands.inHours();
		helper.assertValueEqual(hand.think(level, in, now), Status.TRAVELLING, "The hand claims the crucible's request");
		WorkshopHands.conserved(helper, ids, PLAIN, 10, 0, "claimed", chest, crucible);
		helper.assertTrue(hand.think(level, in, now + 20) == Status.WORKING && ledger.ledger().get(id).progress().carried() == 2,
				"It picks up two for a crucible four blocks off, which it must walk to");
		WorkshopHands.conserved(helper, ids, PLAIN, 10, 0, "carried", chest, crucible);
		for (int attempt = 1; attempt <= Navigation.GIVE_UP; attempt++) {
			long then = now + 20 + 20L * attempt;
			hand.getBrain().setMemory(MemoryModuleType.CANT_REACH_WALK_TARGET_SINCE, then);
			helper.assertTrue(hand.getBrain().hasMemoryValue(MemoryModuleType.CANT_REACH_WALK_TARGET_SINCE), "Its brain keeps failed walk " + attempt);
			Status walking = hand.think(level, in, then);
			boolean last = attempt == Navigation.GIVE_UP;
			helper.assertTrue(last ? walking == Status.CANNOT_NAVIGATE && hand.task() == 0L : walking == Status.TRAVELLING && hand.task() == id,
					"Failed walk " + attempt + " of " + Navigation.GIVE_UP + ": " + walking);
			WorkshopHands.conserved(helper, ids, PLAIN, 10, 0, "failed walk " + attempt, chest, crucible);
		}
		Request stranded = ledger.ledger().get(id);
		helper.assertTrue(stranded.state() == RequestState.STRANDED && stranded.progress().carried() == 2 && "cannot_navigate".equals(stranded.progress().note()),
				"The third failed walk gives up the claim: the two are stranded at the post, counted, never dropped: " + stranded);
		helper.assertTrue(crucible.courierRemainder(RAW) == 0 && crucible.companion.needed(new ItemStack(Items.GLOWSTONE_DUST)) == 2,
				"No courier holds it: a Peepo may bring both");
		helper.assertTrue(ledger.recover(id, holder, now + 100) == Logistics.Outcome.DONE && holder.getInventory().countItem(Items.GLOWSTONE_DUST) == 2,
				"Its requester recovers the two");
		helper.assertTrue(ledger.recover(id, holder, now + 101) == Logistics.Outcome.NOT_FOUND && holder.getInventory().countItem(Items.GLOWSTONE_DUST) == 2,
				"and never twice");
		WorkshopHands.conserved(helper, ids, PLAIN, 10, 0, "recovered", chest, crucible, holder.getInventory());
		helper.succeed();
	}

	/**
	 * D-08, 03:193's "target chunk unloads": a hand carrying for a place whose chunk is not loaded waits, DESTINATION_UNLOADED,
	 * no ledger step but its lease renewed; a restart keeps the cargo; cancelled, it goes back. Sealed under test terms whose
	 * area reaches that far, so only the unloaded chunk stands in the way.
	 */
	@GameTest(maxTicks = 20)
	public void anUnloadedDestinationKeepsTheHandWaiting(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		ServerPlayer holder = WorkshopHands.holder(helper);
		CourierPostBlockEntity post = WorkshopHands.post(helper, holder, WorkshopHands.POST);
		ChestBlockEntity chest = WorkshopHands.chest(helper, WorkshopHands.CHEST, 10);
		BlockPos away = helper.absolutePos(WorkshopHands.CRUCIBLE).offset(100_000, 0, 0);
		helper.assertTrue(!level.isLoaded(away), "The destination's chunk is not loaded");
		CourierLedger ledger = CourierLedger.of(level.getServer());
		Logistics.Filed filed = ledger.file(holder.getUUID(), Couriers.place(level, post.getBlockPos()), Couriers.place(level, away), CrucibleBlockEntity.sample(RAW), 2,
				level.registryAccess(), level.getGameTime());
		helper.assertTrue(filed.outcome() == Logistics.Outcome.DONE, "Stands for a request filed while the place was loaded: " + filed);
		long id = filed.id();
		List<Long> ids = List.of(id);
		long now = WorkshopHands.morning(level, 2_400_000L);
		WorkerDefinition.Spirit terms = WorkshopHands.terms();
		WorkshopHandEntity hand = WorkshopHands.handUnder(helper, holder,
				new WorkerDefinition.Spirit(WorkshopHandEntity.DEFINITION, terms.work(), 100_008, terms.from(), terms.to(), terms.quota(), terms.carry()), now);
		SpiritAnchorBlockEntity anchor = helper.getBlockEntity(WorkshopHands.ANCHOR, SpiritAnchorBlockEntity.class);
		long in = WorkshopHands.inHours();
		helper.assertValueEqual(hand.think(level, in, now), Status.TRAVELLING, "The hand claims the request");
		WorkshopHands.conserved(helper, ids, PLAIN, 10, 0, "claimed", chest);
		hand.think(level, in, now + 20);
		helper.assertTrue(ledger.ledger().get(id).state() == RequestState.IN_TRANSIT && ledger.ledger().get(id).progress().carried() == 2
				&& matching(chest, PLAIN) == 8, "Two picked up for it");
		WorkshopHands.conserved(helper, ids, PLAIN, 10, 0, "in transit", chest);
		for (int i = 1; i <= 2; i++) {
			long then = now + 20 + 20L * i;
			Status waits = hand.think(level, in, then);
			Request held = ledger.ledger().get(id);
			helper.assertTrue(waits == Status.DESTINATION_UNLOADED && held.progress().carried() == 2 && held.progress().delivered() == 0
					&& held.progress().aboard() && held.progress().lease() == then + Logistics.LEASE_TICKS,
					"It waits with the cargo aboard, its claim renewed, no step taken: " + waits + ", " + held.progress());
			WorkshopHands.conserved(helper, ids, PLAIN, 10, 0, "unloaded " + i, chest);
		}
		TagValueOutput saved = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, level.registryAccess());
		saved.store("ledger", CourierLedger.CODEC, ledger);
		CourierLedger restarted = TagValueInput.create(ProblemReporter.DISCARDING, level.registryAccess(), saved.buildResult())
				.read("ledger", CourierLedger.CODEC).orElseThrow();
		helper.assertTrue(restarted.ledger().get(id) != null && restarted.ledger().get(id).state() == RequestState.IN_TRANSIT
				&& restarted.ledger().get(id).progress().carried() == 2 && ItemVariant.of(new ItemStack(Items.GLOWSTONE_DUST)).equals(restarted.item(id))
				&& restarted.problems().isEmpty(), "A restart keeps the cargo, counted once");
		helper.assertTrue(ledger.cancel(id, holder.getUUID(), now + 70) == Logistics.Outcome.DONE && ledger.ledger().get(id).state() == RequestState.RETURNING,
				"Cancelled by its requester: it is taken back");
		WorkshopHands.conserved(helper, ids, PLAIN, 10, 0, "cancelled", chest);
		hand.think(level, in, now + 80);
		helper.assertTrue(ledger.ledger().get(id) == null && matching(chest, PLAIN) == 10 && anchor.agreement().done(now + 80) == 0,
				"All ten back in the chest; no load counted");
		WorkshopHands.conserved(helper, ids, PLAIN, 10, 0, "returned", chest);
		helper.assertValueEqual(anchor.holder().end(level, holder, now + 90), AgreementHolder.Ended.ENDED, "Its far-reaching test terms end with the test");
		helper.succeed();
	}

	/**
	 * D-08, 03:193's "entity dies" (E4, D-S18): a killed hand lets its claim go; the cargo is stranded at the post, counted,
	 * nothing dropped; it is struck off its holder's roster and its agreement stands; its holder calls another under the same
	 * Bound Will at no cost, which takes the cargo up and delivers it once.
	 */
	@GameTest(maxTicks = 20)
	public void aKilledHandStrandsItsCargoOnce(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		WorkshopHands.Workshop shop = WorkshopHands.workshop(helper);
		WorkshopHandEntity hand = shop.hand();
		CrucibleBlockEntity crucible = shop.crucible();
		long id = shop.request();
		List<Long> ids = List.of(id);
		CourierLedger ledger = CourierLedger.of(level.getServer());
		UUID holder = shop.holder().getUUID();
		UUID record = shop.anchor().agreement().record();
		long now = WorkshopHands.morning(level, 2_500_000L);
		long in = WorkshopHands.inHours();
		helper.assertValueEqual(hand.think(level, in, now), Status.TRAVELLING, "The hand claims the crucible's request");
		WorkshopHands.conserved(helper, ids, PLAIN, 10, 0, "claimed", shop.chest(), crucible);
		hand.think(level, in, now + 20);
		helper.assertTrue(ledger.ledger().get(id).progress().carried() == 2, "The hand carries two for the crucible");
		WorkshopHands.conserved(helper, ids, PLAIN, 10, 0, "carried", shop.chest(), crucible);
		UUID first = hand.getUUID();
		hand.remove(Entity.RemovalReason.KILLED);
		Request stranded = ledger.ledger().get(id);
		helper.assertTrue(stranded != null && stranded.state() == RequestState.STRANDED && stranded.progress().carried() == 2
				&& "worker_removed".equals(stranded.progress().note()), "Killed, its claim is let go: the two are stranded at the post, counted: " + stranded);
		WorkshopHands.conserved(helper, ids, PLAIN, 10, 0, "killed (nothing dropped where it fell)", shop.chest(), crucible);
		helper.assertTrue(!WorkerRoster.of(level.getServer()).workers(holder).containsKey(first) && shop.anchor().holder().spiritLost(level)
				&& BoundWills.of(level.getServer()).ledger().get(record) != null, "It is struck off its holder's roster; its agreement stands");
		helper.assertTrue(crucible.courierRemainder(RAW) == 0 && crucible.companion.needed(new ItemStack(Items.GLOWSTONE_DUST)) == 2,
				"No courier holds it: a Peepo may bring both");
		helper.assertValueEqual(shop.anchor().holder().recall(level, shop.holder(), now + 30), AgreementHolder.Recall.RECALLED,
				"Its holder calls another hand under the same agreement, at no cost");
		WorkshopHandEntity second = level.getEntity(shop.anchor().spirit()) instanceof WorkshopHandEntity found ? found : null;
		helper.assertTrue(second != null && !first.equals(second.getUUID()) && record.equals(shop.anchor().agreement().record())
				&& BoundWills.of(level.getServer()).ledger().heldBy(holder).size() == 1, "A new hand, the same Bound Will");
		WorkshopHands.stand(helper, second, 2.5, 1.0, 3.5);
		helper.assertTrue(second.think(level, in, now + 40) == Status.WORKING && second.task() == id && ledger.ledger().get(id).progress().aboard()
				&& crucible.companion.needed(new ItemStack(Items.GLOWSTONE_DUST)) == 0, "It takes the stranded two up at the post: a Peepo again brings none");
		WorkshopHands.conserved(helper, ids, PLAIN, 10, 0, "taken up", shop.chest(), crucible);
		helper.assertTrue(second.think(level, in, now + 60) == Status.WORKING && ledger.ledger().get(id) == null && matching(crucible, PLAIN) == 2,
				"and delivers them, once");
		WorkshopHands.conserved(helper, ids, PLAIN, 10, 0, "delivered", shop.chest(), crucible);
		helper.assertValueEqual(WorkshopHands.story(helper, WorkshopHands.POST, id),
				List.of("filed", "claimed", "reserved", "picked_up", "stranded", "claimed", "taken_up", "done"), "the post's history of it");
		helper.succeed();
	}

	/**
	 * D-08, 03:193's "owner logs out": its holder away, the hand takes no step and renews nothing; the lease runs out and the
	 * cargo waits at the post, counted, nothing lost or owed; its holder back, it takes the cargo up and delivers it once.
	 */
	@GameTest(maxTicks = 20)
	public void anAbsentHolderStopsTheHand(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		WorkshopHands.Workshop shop = WorkshopHands.workshop(helper);
		WorkshopHandEntity hand = shop.hand();
		CrucibleBlockEntity crucible = shop.crucible();
		long id = shop.request();
		List<Long> ids = List.of(id);
		CourierLedger ledger = CourierLedger.of(level.getServer());
		long now = WorkshopHands.morning(level, 2_600_000L);
		long in = WorkshopHands.inHours();
		helper.assertValueEqual(hand.think(level, in, now), Status.TRAVELLING, "The hand claims the crucible's request");
		WorkshopHands.conserved(helper, ids, PLAIN, 10, 0, "claimed", shop.chest(), crucible);
		hand.think(level, in, now + 20);
		Request carrying = ledger.ledger().get(id);
		helper.assertTrue(carrying.progress().carried() == 2 && carrying.progress().lease() == now + 20 + Logistics.LEASE_TICKS, "It carries two, its claim renewed");
		WorkshopHands.conserved(helper, ids, PLAIN, 10, 0, "carried", shop.chest(), crucible);
		UUID holder = shop.holder().getUUID();
		TestPlayers.leave(shop.holder());
		helper.assertTrue(hand.think(level, in, now + 40) == Status.OWNER_OFFLINE && hand.task() == id && ledger.ledger().get(id).progress().equals(carrying.progress()),
				"Its holder logged out, it waits: no ledger step, its claim not renewed");
		WorkshopHands.conserved(helper, ids, PLAIN, 10, 0, "holder away", shop.chest(), crucible);
		ledger.ledger().lapse(now + 21 + Logistics.LEASE_TICKS);
		Request lapsed = ledger.ledger().get(id);
		helper.assertTrue(lapsed.state() == RequestState.STRANDED && lapsed.progress().carried() == 2 && "lapsed".equals(lapsed.progress().note()),
				"Its lease run out, the two wait at the post, counted: nothing lost and nothing owed: " + lapsed);
		WorkshopHands.conserved(helper, ids, PLAIN, 10, 0, "lapsed", shop.chest(), crucible);
		helper.assertValueEqual(hand.think(level, in, now + 240), Status.OWNER_OFFLINE, "still away, it still waits");
		WorkshopHands.conserved(helper, ids, PLAIN, 10, 0, "still away", shop.chest(), crucible);
		ServerPlayer back = TestPlayers.again(helper, holder);
		helper.assertTrue(holder.equals(back.getUUID()) && hand.think(level, in, now + 260) == Status.WORKING && hand.task() == id
				&& ledger.ledger().get(id).progress().aboard(), "Its holder back, it takes the stranded two up at the post");
		WorkshopHands.conserved(helper, ids, PLAIN, 10, 0, "taken up", shop.chest(), crucible);
		helper.assertTrue(hand.think(level, in, now + 280) == Status.WORKING && ledger.ledger().get(id) == null && matching(crucible, PLAIN) == 2,
				"and delivers them, once");
		WorkshopHands.conserved(helper, ids, PLAIN, 10, 0, "delivered", shop.chest(), crucible);
		helper.assertValueEqual(WorkshopHands.story(helper, WorkshopHands.POST, id),
				List.of("filed", "claimed", "reserved", "picked_up", "stranded", "claimed", "taken_up", "done"), "the post's history of it");
		helper.succeed();
	}

	/**
	 * D-08, 03:193's "source is broken": the chest it reserved at is broken (its contents fall where it stood, vanilla's
	 * rule); the hand gives up its claim, source_gone, and after its rest reserves at another chest and delivers once.
	 */
	@GameTest(maxTicks = 20)
	public void aBrokenSourceReleasesTheHandsClaim(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		WorkshopHands.Workshop shop = WorkshopHands.workshop(helper);
		WorkshopHandEntity hand = shop.hand();
		CrucibleBlockEntity crucible = shop.crucible();
		long id = shop.request();
		List<Long> ids = List.of(id);
		CourierLedger ledger = CourierLedger.of(level.getServer());
		long now = WorkshopHands.morning(level, 2_700_000L);
		long in = WorkshopHands.inHours();
		helper.assertTrue(hand.think(level, in, now) == Status.TRAVELLING && ledger.ledger().get(id).progress().reserved() == 2, "Claimed and reserved at the chest");
		WorkshopHands.conserved(helper, ids, PLAIN, 10, 0, "reserved", shop.chest(), crucible);
		helper.setBlock(WorkshopHands.CHEST, Blocks.AIR);
		WorkshopHands.conserved(helper, ids, PLAIN, 10, 10, "the chest broken: its ten lie where it stood", crucible);
		Status gone = hand.think(level, in, now + 20);
		Request released = ledger.ledger().get(id);
		helper.assertTrue(gone == Status.WAITING_FOR_RESOURCES && hand.task() == 0L && released.state() == RequestState.OPEN && released.progress().reserved() == 0
				&& released.progress().source() == null && "source_gone".equals(released.progress().note()),
				"Its source broken, it gives up its claim, source_gone: " + released);
		WorkshopHands.conserved(helper, ids, PLAIN, 10, 10, "released", crucible);
		BlockPos spareAt = new BlockPos(1, 1, 3);
		ChestBlockEntity spare = WorkshopHands.chest(helper, spareAt, 10);
		WorkshopHands.conserved(helper, ids, PLAIN, 20, 10, "another chest", spare, crucible);
		long retry = now + 20 + Logistics.RETRY_TICKS;
		helper.assertTrue(hand.think(level, in, retry) == Status.TRAVELLING && hand.task() == id && ledger.ledger().get(id).progress().reserved() == 2
				&& Couriers.place(level, helper.absolutePos(spareAt)).equals(ledger.ledger().get(id).progress().source()),
				"After its rest it claims again and reserves at the other chest");
		WorkshopHands.conserved(helper, ids, PLAIN, 20, 10, "reserved at the other chest", spare, crucible);
		helper.assertTrue(hand.think(level, in, retry + 20) == Status.WORKING && matching(spare, PLAIN) == 8, "picks up two");
		WorkshopHands.conserved(helper, ids, PLAIN, 20, 10, "picked up", spare, crucible);
		helper.assertTrue(hand.think(level, in, retry + 40) == Status.WORKING && ledger.ledger().get(id) == null && matching(crucible, PLAIN) == 2,
				"and delivers them, once");
		WorkshopHands.conserved(helper, ids, PLAIN, 20, 10, "delivered", spare, crucible);
		helper.succeed();
	}

	/**
	 * D-08 (E5): two hands of two party members bound to one post, in one tick: one claims the crucible's request; the other
	 * finds nothing to take and may neither claim (CLAIMED), reserve nor pick up for it (NOT_YOURS); one reservation, counted
	 * once by the crucible; the first killed, the second takes the cargo up and delivers it once.
	 */
	@GameTest(maxTicks = 20)
	public void twoHandsCannotHoldOneRequest(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		ServerPlayer holder = WorkshopHands.holder(helper);
		ServerPlayer friend = WorkshopHands.holder(helper);
		PartyManager parties = JugcraftParties.manager();
		long ms = System.currentTimeMillis();
		try {
			helper.assertTrue(parties.invite(holder.getUUID(), friend.getUUID(), ms) == PartyManager.Result.OK
					&& parties.accept(friend.getUUID(), ms) == PartyManager.Result.OK, "The holder and a friend form a party");
			WorkshopHands.post(helper, holder, WorkshopHands.POST);
			ChestBlockEntity chest = WorkshopHands.chest(helper, WorkshopHands.CHEST, 10);
			CrucibleBlockEntity crucible = WorkshopHands.crucible(helper, WorkshopHands.CRUCIBLE, holder);
			long id = crucible.request();
			List<Long> ids = List.of(id);
			WorkshopHandEntity mine = WorkshopHands.hand(helper, holder, WorkshopHands.ANCHOR, WorkshopHands.POST);
			WorkshopHandEntity theirs = WorkshopHands.hand(helper, friend, new BlockPos(4, 1, 5), WorkshopHands.POST);
			WorkshopHands.stand(helper, theirs, 3.5, 1.0, 3.5);
			CourierLedger ledger = CourierLedger.of(level.getServer());
			long now = WorkshopHands.morning(level, 2_800_000L);
			long in = WorkshopHands.inHours();
			Status first = mine.think(level, in, now);
			Status second = theirs.think(level, in, now);
			helper.assertTrue(first == Status.TRAVELLING && mine.task() == id && second == Status.IDLE && theirs.task() == 0L,
					"Two hands of one post in one tick: one claims the crucible's request, the other finds nothing to take");
			Storage<ItemVariant> source = ItemStorage.SIDED.find(level, chest.getBlockPos(), null);
			helper.assertTrue(ledger.claim(id, theirs.getUUID(), now) == Logistics.Outcome.CLAIMED
					&& ledger.reserve(id, theirs.getUUID(), Couriers.place(level, chest.getBlockPos()), source, now) == Logistics.Outcome.NOT_YOURS
					&& ledger.pickUp(id, theirs.getUUID(), source, now) == Logistics.Outcome.NOT_YOURS,
					"The claim is exclusive: CLAIMED for the second, and it may neither reserve nor pick up for it");
			helper.assertTrue(mine.getUUID().equals(ledger.ledger().get(id).progress().worker()) && ledger.ledger().get(id).progress().reserved() == 2
					&& crucible.courierRemainder(RAW) == 2 && crucible.companion.needed(new ItemStack(Items.GLOWSTONE_DUST)) == 0,
					"One reservation of two, counted once by the crucible; a Peepo brings none");
			WorkshopHands.conserved(helper, ids, PLAIN, 10, 0, "claimed", chest, crucible);
			mine.think(level, in, now + 20);
			helper.assertTrue(theirs.think(level, in, now + 20) == Status.IDLE && theirs.task() == 0L && ledger.ledger().get(id).progress().carried() == 2
					&& matching(chest, PLAIN) == 8, "Carried by the first; the second still holds nothing");
			WorkshopHands.conserved(helper, ids, PLAIN, 10, 0, "carried", chest, crucible);
			mine.remove(Entity.RemovalReason.KILLED);
			helper.assertTrue(ledger.ledger().get(id).state() == RequestState.STRANDED && ledger.ledger().get(id).progress().worker() == null,
					"The first gone, its claim let go: the cargo waits at the post");
			WorkshopHands.conserved(helper, ids, PLAIN, 10, 0, "stranded", chest, crucible);
			helper.assertTrue(theirs.think(level, in, now + 40) == Status.WORKING && theirs.task() == id && ledger.ledger().get(id).progress().aboard(),
					"The second takes it up");
			WorkshopHands.conserved(helper, ids, PLAIN, 10, 0, "taken up", chest, crucible);
			helper.assertTrue(theirs.think(level, in, now + 60) == Status.WORKING && ledger.ledger().get(id) == null && matching(crucible, PLAIN) == 2,
					"and delivers it, once");
			WorkshopHands.conserved(helper, ids, PLAIN, 10, 0, "delivered", chest, crucible);
			helper.assertValueEqual(WorkshopHands.story(helper, WorkshopHands.POST, id),
					List.of("filed", "claimed", "reserved", "picked_up", "stranded", "claimed", "taken_up", "done"), "the post's history of it");
		} finally {
			parties.adminDisband(holder.getUUID());
		}
		helper.succeed();
	}

	/**
	 * D-08 (E4, D-S6): its holder ends the agreement with cargo aboard: the hand departs and lets its claim go,
	 * agreement_ended; the cargo is stranded at the post, nothing dropped; the Bound Will goes, the departure is kept; ending
	 * again finds nothing and moves nothing; the requester recovers the cargo exactly once.
	 */
	@GameTest(maxTicks = 20)
	public void endingWithCargoAboardStrandsItOnce(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		WorkshopHands.Workshop shop = WorkshopHands.workshop(helper);
		WorkshopHandEntity hand = shop.hand();
		CrucibleBlockEntity crucible = shop.crucible();
		ServerPlayer holder = shop.holder();
		long id = shop.request();
		List<Long> ids = List.of(id);
		CourierLedger ledger = CourierLedger.of(level.getServer());
		UUID record = shop.anchor().agreement().record();
		long now = WorkshopHands.morning(level, 2_900_000L);
		long in = WorkshopHands.inHours();
		helper.assertValueEqual(hand.think(level, in, now), Status.TRAVELLING, "The hand claims the crucible's request");
		WorkshopHands.conserved(helper, ids, PLAIN, 10, 0, "claimed", shop.chest(), crucible);
		hand.think(level, in, now + 20);
		helper.assertTrue(ledger.ledger().get(id).progress().carried() == 2 && ledger.ledger().get(id).progress().aboard(), "The hand carries two");
		WorkshopHands.conserved(helper, ids, PLAIN, 10, 0, "carried", shop.chest(), crucible);
		UUID handId = hand.getUUID();
		helper.assertValueEqual(shop.anchor().holder().end(level, holder, now + 30), AgreementHolder.Ended.ENDED, "Its holder ends the agreement with the cargo aboard");
		Request stranded = ledger.ledger().get(id);
		helper.assertTrue(hand.isRemoved() && stranded != null && stranded.state() == RequestState.STRANDED && stranded.progress().carried() == 2
				&& "agreement_ended".equals(stranded.progress().note()), "The hand departs and lets its claim go: the two wait at the post: " + stranded);
		WorkshopHands.conserved(helper, ids, PLAIN, 10, 0, "ended (nothing dropped)", shop.chest(), crucible);
		List<Pacts.Departure> history = PactHistory.of(level.getServer()).history(holder.getUUID());
		helper.assertTrue(shop.anchor().agreement() == null && BoundWills.of(level.getServer()).ledger().get(record) == null
				&& !WorkerRoster.of(level.getServer()).workers(holder.getUUID()).containsKey(handId) && !history.isEmpty()
				&& history.getLast().record().equals(record) && Pacts.ENDED.equals(history.getLast().reason())
				&& WorkshopHandEntity.DEFINITION.equals(history.getLast().counterpart()), "Its Bound Will gone, the hand off the roster, the departure kept");
		helper.assertTrue(crucible.courierRemainder(RAW) == 0 && crucible.companion.needed(new ItemStack(Items.GLOWSTONE_DUST)) == 2,
				"No courier holds it: a Peepo may bring both");
		helper.assertTrue(shop.anchor().holder().end(level, holder, now + 31) == AgreementHolder.Ended.NOTHING
				&& AgreementHolder.end(level, null, holder, record, Pacts.ENDED, now + 31) == AgreementHolder.Ended.NOTHING
				&& ledger.ledger().get(id).progress().equals(stranded.progress()), "Ending again finds nothing and moves nothing");
		helper.assertTrue(ledger.recover(id, holder, now + 40) == Logistics.Outcome.DONE && holder.getInventory().countItem(Items.GLOWSTONE_DUST) == 2,
				"Its requester recovers the two");
		helper.assertTrue(ledger.recover(id, holder, now + 41) == Logistics.Outcome.NOT_FOUND && holder.getInventory().countItem(Items.GLOWSTONE_DUST) == 2,
				"and never twice");
		WorkshopHands.conserved(helper, ids, PLAIN, 10, 0, "recovered", shop.chest(), crucible, holder.getInventory());
		helper.assertValueEqual(WorkshopHands.story(helper, WorkshopHands.POST, id), List.of("filed", "claimed", "reserved", "picked_up", "stranded", "recovered"),
				"the post's history of it");
		helper.succeed();
	}

	/**
	 * D-08 (E1, D-S2): a hand serves its holder and its holder's party, recorded on its Bound Will: a friend's request is
	 * served; a stranger cannot even ask at the post. Who benefits is asked at each new claim (critique 5): a load claimed
	 * before its requester left the party is finished, while their request still open is never touched (IDLE: left to other
	 * couriers) and the holder's own is taken past it.
	 */
	@GameTest(maxTicks = 20)
	public void aHandServesOnlyItsBeneficiaries(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		ServerPlayer holder = WorkshopHands.holder(helper);
		ServerPlayer friend = WorkshopHands.taught(TestPlayers.survivor(helper), null);
		ServerPlayer stranger = WorkshopHands.taught(TestPlayers.survivor(helper), null);
		PartyManager parties = JugcraftParties.manager();
		long ms = System.currentTimeMillis();
		try {
			helper.assertTrue(parties.invite(holder.getUUID(), friend.getUUID(), ms) == PartyManager.Result.OK
					&& parties.accept(friend.getUUID(), ms) == PartyManager.Result.OK, "The holder and a friend form a party");
			CourierPostBlockEntity post = WorkshopHands.post(helper, holder, WorkshopHands.POST);
			ChestBlockEntity chest = WorkshopHands.chest(helper, WorkshopHands.CHEST, 10);
			WorkshopHandEntity hand = WorkshopHands.hand(helper, holder, WorkshopHands.ANCHOR, WorkshopHands.POST);
			SpiritAnchorBlockEntity anchor = helper.getBlockEntity(WorkshopHands.ANCHOR, SpiritAnchorBlockEntity.class);
			BoundWill will = BoundWills.of(level.getServer()).ledger().get(anchor.agreement().record());
			helper.assertTrue(Pacts.PARTY.equals(will.pact().beneficiaries()) && Pacts.benefits(will, friend.getUUID(), true)
					&& !Pacts.benefits(will, stranger.getUUID(), false), "Its Bound Will names its beneficiaries: the holder and the holder's party");
			CourierLedger ledger = CourierLedger.of(level.getServer());
			long next = ledger.ledger().nextId();
			Couriers.request(stranger, level, post, new ItemStack(Items.GLOWSTONE_DUST), 2);
			helper.assertTrue(ledger.ledger().nextId() == next, "A stranger cannot ask at the holder's post at all");
			Couriers.request(friend, level, post, new ItemStack(Items.GLOWSTONE_DUST), 2);
			long theirs = WorkshopHands.lastFiled(helper, WorkshopHands.POST);
			List<Long> ids = new ArrayList<>(List.of(theirs));
			long now = WorkshopHands.morning(level, 3_000_000L);
			long in = WorkshopHands.inHours();
			helper.assertTrue(hand.think(level, in, now) == Status.TRAVELLING && hand.task() == theirs, "The friend's request is served: the party benefits");
			WorkshopHands.conserved(helper, ids, PLAIN, 10, 0, "the friend's claimed", chest, post);
			helper.assertValueEqual(hand.think(level, in, now + 20), Status.WORKING, "it picks up");
			WorkshopHands.conserved(helper, ids, PLAIN, 10, 0, "the friend's picked up", chest, post);
			helper.assertTrue(hand.think(level, in, now + 40) == Status.WORKING && ledger.ledger().get(theirs) == null && matching(post, PLAIN) == 2,
					"delivered to the post");
			WorkshopHands.conserved(helper, ids, PLAIN, 10, 0, "the friend served", chest, post);
			Couriers.request(friend, level, post, new ItemStack(Items.GLOWSTONE_DUST), 2);
			long claimed = WorkshopHands.lastFiled(helper, WorkshopHands.POST);
			ids.add(claimed);
			helper.assertTrue(hand.think(level, in, now + 50) == Status.TRAVELLING && hand.task() == claimed, "The friend's next request is claimed");
			WorkshopHands.conserved(helper, ids, PLAIN, 10, 0, "the friend's next claimed", chest, post);
			Couriers.request(friend, level, post, new ItemStack(Items.GLOWSTONE_DUST), 2);
			long left = WorkshopHands.lastFiled(helper, WorkshopHands.POST);
			ids.add(left);
			helper.assertTrue(parties.leave(friend.getUUID()) == PartyManager.Result.OK && !JugcraftParties.sameParty(holder.getUUID(), friend.getUUID()),
					"The friend leaves the party, one request of theirs claimed and one still open");
			helper.assertTrue(hand.think(level, in, now + 54) == Status.WORKING && hand.task() == claimed
					&& ledger.ledger().get(claimed).progress().carried() == 2, "A load claimed before they left is finished: it picks up");
			WorkshopHands.conserved(helper, ids, PLAIN, 10, 0, "the claimed load picked up", chest, post);
			helper.assertTrue(hand.think(level, in, now + 57) == Status.WORKING && ledger.ledger().get(claimed) == null && matching(post, PLAIN) == 4,
					"and delivers");
			WorkshopHands.conserved(helper, ids, PLAIN, 10, 0, "the claimed load delivered", chest, post);
			helper.assertTrue(hand.think(level, in, now + 60) == Status.IDLE && hand.task() == 0L
					&& ledger.ledger().get(left).state() == RequestState.OPEN && ledger.ledger().get(left).progress().worker() == null,
					"No longer a beneficiary: the hand leaves it to other couriers and takes nothing");
			WorkshopHands.conserved(helper, ids, PLAIN, 10, 0, "a former member's request skipped", chest, post);
			Couriers.request(holder, level, post, new ItemStack(Items.GLOWSTONE_DUST), 2);
			long mine = WorkshopHands.lastFiled(helper, WorkshopHands.POST);
			ids.add(mine);
			helper.assertTrue(hand.think(level, in, now + 80) == Status.TRAVELLING && hand.task() == mine, "Its holder's own request is taken past it");
			WorkshopHands.conserved(helper, ids, PLAIN, 10, 0, "the holder's claimed", chest, post);
			helper.assertValueEqual(hand.think(level, in, now + 100), Status.WORKING, "it picks up");
			WorkshopHands.conserved(helper, ids, PLAIN, 10, 0, "the holder's picked up", chest, post);
			helper.assertTrue(hand.think(level, in, now + 120) == Status.WORKING && ledger.ledger().get(mine) == null && matching(post, PLAIN) == 6
					&& ledger.ledger().get(left).progress().worker() == null, "and delivered; the former member's is never touched");
			WorkshopHands.conserved(helper, ids, PLAIN, 10, 0, "the holder served", chest, post);
			helper.assertTrue(ledger.cancel(left, friend.getUUID(), now + 130) == Logistics.Outcome.DONE && ledger.ledger().get(left) == null,
					"Its requester may still cancel it");
			WorkshopHands.conserved(helper, ids, PLAIN, 10, 0, "cancelled", chest, post);
		} finally {
			parties.adminDisband(holder.getUUID());
		}
		helper.succeed();
	}

	/**
	 * D-08: a hand fetches only from containers inside its area (here a test area of 3 blocks), never one a porter of the
	 * post would use beyond it, and reserves at most its carry a trip: a request for 100 is two loads of 64 and 36.
	 */
	@GameTest(maxTicks = 20)
	public void aHandTakesOnlyFromInsideItsArea(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		ServerPlayer holder = WorkshopHands.holder(helper);
		CourierPostBlockEntity post = WorkshopHands.post(helper, holder, WorkshopHands.POST);
		ChestBlockEntity near = chest(helper, WorkshopHands.CHEST);
		ChestBlockEntity far = chest(helper, new BlockPos(6, 1, 6));
		far.setItem(0, new ItemStack(Items.IRON_INGOT, 64));
		far.setItem(1, new ItemStack(Items.IRON_INGOT, 64));
		long now = WorkshopHands.morning(level, 3_100_000L);
		WorkerDefinition.Spirit terms = WorkshopHands.terms();
		WorkshopHandEntity hand = WorkshopHands.handUnder(helper, holder,
				new WorkerDefinition.Spirit(WorkshopHandEntity.DEFINITION, terms.work(), 3, terms.from(), terms.to(), terms.quota(), terms.carry()), now);
		SpiritAnchorBlockEntity anchor = helper.getBlockEntity(WorkshopHands.ANCHOR, SpiritAnchorBlockEntity.class);
		Couriers.request(holder, level, post, new ItemStack(Items.IRON_INGOT), 100);
		long id = WorkshopHands.lastFiled(helper, WorkshopHands.POST);
		List<Long> ids = List.of(id);
		Predicate<ItemStack> iron = stack -> stack.is(Items.IRON_INGOT);
		CourierLedger ledger = CourierLedger.of(level.getServer());
		long in = WorkshopHands.inHours();
		helper.assertTrue(hand.think(level, in, now) == Status.WAITING_FOR_RESOURCES && hand.task() == 0L
				&& ledger.ledger().get(id).progress().worker() == null && "no_source".equals(ledger.ledger().get(id).progress().note())
				&& count(far, Items.IRON_INGOT) == 128, "Only a chest beyond its area has iron: it reserves nowhere, the far chest untouched");
		WorkshopHands.conserved(helper, ids, iron, 128, 0, "nothing reserved", near, far, post);
		near.setItem(0, new ItemStack(Items.IRON_INGOT, 64));
		near.setItem(1, new ItemStack(Items.IRON_INGOT, 36));
		long retry = now + Logistics.RETRY_TICKS;
		helper.assertTrue(hand.think(level, in, retry) == Status.TRAVELLING && hand.task() == id && ledger.ledger().get(id).progress().reserved() == terms.carry()
				&& Couriers.place(level, near.getBlockPos()).equals(ledger.ledger().get(id).progress().source()),
				"Iron inside its area: it reserves there, its carry and no more: " + ledger.ledger().get(id).progress());
		WorkshopHands.conserved(helper, ids, iron, 228, 0, "reserved", near, far, post);
		helper.assertTrue(hand.think(level, in, retry + 20) == Status.WORKING && ledger.ledger().get(id).progress().carried() == 64
				&& count(near, Items.IRON_INGOT) == 36, "It picks up 64");
		WorkshopHands.conserved(helper, ids, iron, 228, 0, "picked up", near, far, post);
		helper.assertTrue(hand.think(level, in, retry + 40) == Status.WORKING && count(post, Items.IRON_INGOT) == 64 && ledger.ledger().get(id) != null
				&& ledger.ledger().get(id).progress().worker() == null && ledger.ledger().get(id).progress().delivered() == 64
				&& anchor.agreement().done(retry + 40) == 1, "It delivers 64 and lets the request go for its next load: one load counted");
		WorkshopHands.conserved(helper, ids, iron, 228, 0, "first load delivered", near, far, post);
		helper.assertTrue(hand.think(level, in, retry + 60) == Status.TRAVELLING && ledger.ledger().get(id).progress().reserved() == 36
				&& Couriers.place(level, near.getBlockPos()).equals(ledger.ledger().get(id).progress().source()) && count(far, Items.IRON_INGOT) == 128,
				"The next load: the rest at the near chest, never the far one");
		WorkshopHands.conserved(helper, ids, iron, 228, 0, "second load reserved", near, far, post);
		helper.assertTrue(ledger.cancel(id, holder.getUUID(), retry + 70) == Logistics.Outcome.DONE && ledger.ledger().get(id) == null, "Cancelled");
		WorkshopHands.conserved(helper, ids, iron, 228, 0, "cancelled", near, far, post);
		helper.succeed();
	}

	/**
	 * D-08 (D-P82, the unit review): cargo stranded at the post beyond a hand's carry (a porter's, which reserves with no cap
	 * of its own) is never taken up by the hand, so it never carries more than its carry a trip: it is left to porters or
	 * to its requester's one recovery, while a stranded load within its carry, behind it, is taken up and delivered once.
	 */
	@GameTest(maxTicks = 20)
	public void aHandLeavesStrandedCargoBeyondItsCarry(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		ServerPlayer holder = WorkshopHands.holder(helper);
		CourierPostBlockEntity post = WorkshopHands.post(helper, holder, WorkshopHands.POST);
		ChestBlockEntity chest = chest(helper, WorkshopHands.CHEST);
		chest.setItem(0, new ItemStack(Items.IRON_INGOT, 64));
		chest.setItem(1, new ItemStack(Items.IRON_INGOT, 64));
		chest.setItem(2, new ItemStack(Items.IRON_INGOT, 64));
		WorkshopHandEntity hand = WorkshopHands.hand(helper, holder, WorkshopHands.ANCHOR, WorkshopHands.POST);
		SpiritAnchorBlockEntity anchor = helper.getBlockEntity(WorkshopHands.ANCHOR, SpiritAnchorBlockEntity.class);
		int carry = WorkshopHands.terms().carry();
		Couriers.request(holder, level, post, new ItemStack(Items.IRON_INGOT), carry + 1);
		long over = WorkshopHands.lastFiled(helper, WorkshopHands.POST);
		Couriers.request(holder, level, post, new ItemStack(Items.IRON_INGOT), carry);
		long within = WorkshopHands.lastFiled(helper, WorkshopHands.POST);
		List<Long> ids = List.of(over, within);
		Predicate<ItemStack> iron = stack -> stack.is(Items.IRON_INGOT);
		CourierLedger ledger = CourierLedger.of(level.getServer());
		long now = WorkshopHands.morning(level, 3_900_000L);
		long in = WorkshopHands.inHours();
		WorkshopHands.conserved(helper, ids, iron, 192, 0, "asked", chest, post, holder.getInventory());
		UUID porter = new UUID(0xD08L, 2L);
		Storage<ItemVariant> source = ItemStorage.SIDED.find(level, chest.getBlockPos(), null);
		Place from = Couriers.place(level, chest.getBlockPos());
		for (long id : ids) {
			helper.assertTrue(ledger.claim(id, porter, now) == Logistics.Outcome.DONE && ledger.reserve(id, porter, from, source, now) == Logistics.Outcome.DONE
					&& ledger.pickUp(id, porter, source, now) == Logistics.Outcome.DONE
					&& ledger.release(id, porter, "worker_removed", false, now) == Logistics.Outcome.DONE,
					"A porter, with no cap of its own, carries request " + id + " whole and is killed: its cargo is stranded at the post");
			WorkshopHands.conserved(helper, ids, iron, 192, 0, "stranded " + id, chest, post, holder.getInventory());
		}
		helper.assertTrue(ledger.ledger().get(over).progress().carried() == carry + 1 && ledger.ledger().get(within).progress().carried() == carry
				&& ledger.ledger().get(over).state() == RequestState.STRANDED && ledger.ledger().get(within).state() == RequestState.STRANDED,
				"One stranded load beyond the hand's carry, and one within it behind it");
		helper.assertTrue(hand.think(level, in, now + 20) == Status.WORKING && hand.task() == within && ledger.ledger().get(within).progress().aboard()
				&& ledger.ledger().get(over).progress().worker() == null, "The hand passes the load beyond its carry and takes up the one within it");
		WorkshopHands.conserved(helper, ids, iron, 192, 0, "the load within its carry taken up", chest, post, holder.getInventory());
		helper.assertTrue(hand.think(level, in, now + 40) == Status.WORKING && ledger.ledger().get(within) == null && count(post, Items.IRON_INGOT) == carry
				&& anchor.agreement().done(now + 40) == 1, "and delivers it, once: one load counted");
		WorkshopHands.conserved(helper, ids, iron, 192, 0, "the load within its carry delivered", chest, post, holder.getInventory());
		Status idle = hand.think(level, in, now + 60);
		Request left = ledger.ledger().get(over);
		helper.assertTrue(idle == Status.IDLE && hand.task() == 0L && left.progress().worker() == null && left.progress().carried() == carry + 1
				&& !left.progress().aboard(), "The load beyond its carry it never takes: it is idle and the load waits, untouched: " + idle);
		WorkshopHands.conserved(helper, ids, iron, 192, 0, "the load beyond its carry left", chest, post, holder.getInventory());
		helper.assertTrue(ledger.recover(over, holder, now + 70) == Logistics.Outcome.DONE && holder.getInventory().countItem(Items.IRON_INGOT) == carry + 1,
				"Its requester recovers it, once");
		WorkshopHands.conserved(helper, ids, iron, 192, 0, "recovered", chest, post, holder.getInventory());
		helper.assertValueEqual(WorkshopHands.story(helper, WorkshopHands.POST, over), List.of("filed", "claimed", "reserved", "picked_up", "stranded", "recovered"),
				"the post's history of the load beyond its carry: the hand never claimed it");
		helper.succeed();
	}

	/**
	 * D-08 (D-P76, D-P82, the unit review): cargo a hand holds whose source lies beyond its area (here a test area of 3
	 * blocks) goes back into the post when its requester cancels it, never to that source; cargo to be taken back to such a
	 * source, stranded, the hand never claims (it is left to porters or to its requester's one recovery), so it never
	 * claims cargo it could put back only into a post that may be full.
	 */
	@GameTest(maxTicks = 20)
	public void aHandTakesCargoBackOnlyInsideItsArea(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		ServerPlayer holder = WorkshopHands.holder(helper);
		CourierPostBlockEntity post = WorkshopHands.post(helper, holder, WorkshopHands.POST);
		ChestBlockEntity far = chest(helper, new BlockPos(6, 1, 6));
		far.setItem(0, new ItemStack(Items.IRON_INGOT, 20));
		long now = WorkshopHands.morning(level, 4_000_000L);
		WorkerDefinition.Spirit terms = WorkshopHands.terms();
		WorkshopHandEntity hand = WorkshopHands.handUnder(helper, holder,
				new WorkerDefinition.Spirit(WorkshopHandEntity.DEFINITION, terms.work(), 3, terms.from(), terms.to(), terms.quota(), terms.carry()), now);
		Couriers.request(holder, level, post, new ItemStack(Items.IRON_INGOT), 10);
		long held = WorkshopHands.lastFiled(helper, WorkshopHands.POST);
		Couriers.request(holder, level, post, new ItemStack(Items.IRON_INGOT), 10);
		long back = WorkshopHands.lastFiled(helper, WorkshopHands.POST);
		List<Long> ids = List.of(held, back);
		Predicate<ItemStack> iron = stack -> stack.is(Items.IRON_INGOT);
		CourierLedger ledger = CourierLedger.of(level.getServer());
		long in = WorkshopHands.inHours();
		WorkshopHands.conserved(helper, ids, iron, 20, 0, "asked", far, post, holder.getInventory());
		UUID porter = new UUID(0xD08L, 3L);
		Storage<ItemVariant> source = ItemStorage.SIDED.find(level, far.getBlockPos(), null);
		Place from = Couriers.place(level, far.getBlockPos());
		for (long id : ids) {
			helper.assertTrue(ledger.claim(id, porter, now) == Logistics.Outcome.DONE && ledger.reserve(id, porter, from, source, now) == Logistics.Outcome.DONE
					&& ledger.pickUp(id, porter, source, now) == Logistics.Outcome.DONE
					&& ledger.release(id, porter, "worker_removed", false, now) == Logistics.Outcome.DONE,
					"A porter fetches request " + id + " from the chest beyond the hand's area and is killed: its cargo is stranded at the post");
			WorkshopHands.conserved(helper, ids, iron, 20, 0, "stranded " + id, far, post, holder.getInventory());
		}
		helper.assertTrue(hand.think(level, in, now + 20) == Status.WORKING && hand.task() == held && ledger.ledger().get(held).progress().aboard()
				&& from.equals(ledger.ledger().get(held).progress().source()), "The hand takes up the first stranded load, its source beyond the hand's area");
		WorkshopHands.conserved(helper, ids, iron, 20, 0, "taken up", far, post, holder.getInventory());
		helper.assertTrue(ledger.cancel(held, holder.getUUID(), now + 30) == Logistics.Outcome.DONE && ledger.ledger().get(held).progress().returning()
				&& hand.getUUID().equals(ledger.ledger().get(held).progress().worker()), "Its requester cancels it while the hand holds it: it is to be taken back");
		WorkshopHands.conserved(helper, ids, iron, 20, 0, "cancelled aboard", far, post, holder.getInventory());
		Status returned = hand.think(level, in, now + 40);
		helper.assertTrue(returned == Status.RETURNING && ledger.ledger().get(held) == null && count(post, Items.IRON_INGOT) == 10
				&& count(far, Items.IRON_INGOT) == 0, "Its source beyond its area, the hand puts the cargo into the post, never into that chest: " + returned);
		WorkshopHands.conserved(helper, ids, iron, 20, 0, "put into the post", far, post, holder.getInventory());
		helper.assertTrue(ledger.cancel(back, holder.getUUID(), now + 50) == Logistics.Outcome.DONE && ledger.ledger().get(back).progress().returning()
				&& ledger.ledger().get(back).progress().worker() == null, "The other stranded load is cancelled: it is to be taken back to the chest beyond the area");
		WorkshopHands.conserved(helper, ids, iron, 20, 0, "the other cancelled", far, post, holder.getInventory());
		Status idle = hand.think(level, in, now + 60);
		helper.assertTrue(idle == Status.IDLE && hand.task() == 0L && ledger.ledger().get(back).progress().worker() == null
				&& ledger.ledger().get(back).progress().carried() == 10, "Cargo bound back beyond its area the hand never claims: it is idle: " + idle);
		WorkshopHands.conserved(helper, ids, iron, 20, 0, "the other left", far, post, holder.getInventory());
		helper.assertTrue(ledger.recover(back, holder, now + 70) == Logistics.Outcome.DONE && holder.getInventory().countItem(Items.IRON_INGOT) == 10,
				"Its requester recovers it, once");
		WorkshopHands.conserved(helper, ids, iron, 20, 0, "recovered", far, post, holder.getInventory());
		helper.assertValueEqual(WorkshopHands.story(helper, WorkshopHands.POST, back),
				List.of("filed", "claimed", "reserved", "picked_up", "stranded", "returning", "recovered"), "the post's history of it: the hand never claimed it");
		helper.succeed();
	}

	/**
	 * Concordance Chunk D, D-07 (08:69-70, E4): the roster recovers only cargo the courier ledger holds stranded at a post,
	 * never anything a container holds, loaded or not. A request reserved at a container whose chunk is not loaded, and one
	 * whose cargo is aboard a courier waiting on an unloaded destination, each say why they wait and offer cancel, not
	 * recovery; a recovery forced on either moves nothing; the reserved one cancelled closes, nothing handed over; once its
	 * courier is gone the four stranded come to their requester once, from the ledger, and the chest keeps its six.
	 */
	@GameTest(maxTicks = 20)
	public void recoveryNeverTakesCargoOutOfAnUnloadedContainer(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		ServerPlayer keeper = keeper(helper);
		CourierPostBlockEntity post = post(helper, keeper, new BlockPos(2, 1, 2));
		ChestBlockEntity chest = chest(helper, new BlockPos(1, 1, 2));
		chest.setItem(0, new ItemStack(Items.GLOWSTONE_DUST, 10));
		BlockPos farSource = helper.absolutePos(new BlockPos(1, 1, 2)).offset(100_000, 0, 0);
		BlockPos farDestination = helper.absolutePos(new BlockPos(3, 1, 2)).offset(100_000, 0, 32);
		helper.assertTrue(!level.isLoaded(farSource) && !level.isLoaded(farDestination), "Neither far place is loaded");
		CourierLedger ledger = CourierLedger.of(level.getServer());
		Place at = Couriers.place(level, post.getBlockPos());
		long now = level.getGameTime() + 1_700_000L;
		Logistics.Filed first = ledger.file(keeper.getUUID(), at, at, new ItemStack(Items.GLOWSTONE_DUST), 3, level.registryAccess(), now);
		long reserved = first.id();
		UUID away = UUID.randomUUID();
		helper.assertTrue(first.outcome() == Logistics.Outcome.DONE && ledger.claim(reserved, away, now) == Logistics.Outcome.DONE
				&& ledger.ledger().reserve(reserved, away, Couriers.place(level, farSource), 6, now) == Logistics.Outcome.DONE
				&& ledger.ledger().get(reserved).progress().reserved() == 3, "Three reserved at a container whose chunk is not loaded");
		Logistics.Filed second = ledger.file(keeper.getUUID(), at, Couriers.place(level, farDestination), new ItemStack(Items.GLOWSTONE_DUST), 4,
				level.registryAccess(), now);
		long aboard = second.id();
		ClockworkPorterEntity porter = courier(helper, keeper, new BlockPos(2, 1, 2));
		porter.think(level, NIGHT, now);
		porter.think(level, NIGHT, now + 20);
		helper.assertTrue(second.outcome() == Logistics.Outcome.DONE && porter.task() == aboard && ledger.ledger().get(aboard).progress().carried() == 4
				&& ledger.ledger().get(aboard).progress().aboard() && matching(chest, PLAIN) == 6, "A porter picks up four for the unloaded destination");
		helper.assertTrue(porter.think(level, NIGHT, now + 40) == Status.DESTINATION_UNLOADED, "and waits with them aboard");
		List<Long> ids = List.of(reserved, aboard);
		conserved(helper, ledger, ids, PLAIN, 10, "aboard", chest);
		RosterStatePayload state = ConcordanceWorkerGameTests.roster(keeper);
		RosterStatePayload.Row reservedRow = ConcordanceWorkerGameTests.rowOf(state, Roster.rowOf(reserved));
		RosterStatePayload.Row aboardRow = ConcordanceWorkerGameTests.rowOf(state, Roster.rowOf(aboard));
		helper.assertTrue(reservedRow != null && !reservedRow.loaded() && reservedRow.icon() == RosterStatePayload.UNLOADED
				&& ConcordanceWorkerGameTests.key(reservedRow.waiting()).equals("message.jugcraft.concordance.roster.waiting.reserved_unloaded")
				&& reservedRow.offers(RosterRequestPayload.CANCEL) && !reservedRow.offers(RosterRequestPayload.RECOVER),
				"The reserved request says its container is not loaded and offers no recovery: " + reservedRow);
		helper.assertTrue(aboardRow != null && !aboardRow.loaded() && aboardRow.icon() == RosterStatePayload.UNLOADED
				&& ConcordanceWorkerGameTests.key(aboardRow.waiting()).equals("message.jugcraft.concordance.roster.waiting.destination_unloaded")
				&& aboardRow.offers(RosterRequestPayload.CANCEL) && !aboardRow.offers(RosterRequestPayload.RECOVER),
				"The request aboard says its destination is not loaded and offers no recovery: " + aboardRow);
		helper.assertTrue(ConcordanceWorkerGameTests.answered(ConcordanceWorkerGameTests.act(keeper, reservedRow, RosterRequestPayload.RECOVER), PactMenu.NOT_OFFERED)
				&& Roster.act(keeper, Roster.REQUEST, Roster.rowOf(reserved), RosterRequestPayload.RECOVER, now + 50).reason().equals("wrong_state")
				&& Roster.act(keeper, Roster.REQUEST, Roster.rowOf(aboard), RosterRequestPayload.RECOVER, now + 50).reason().equals("wrong_state"),
				"A recovery asked of either is refused, by the screen and by the ledger");
		helper.assertTrue(keeper.getInventory().countItem(Items.GLOWSTONE_DUST) == 0 && matching(chest, PLAIN) == 6
				&& ledger.ledger().get(reserved).progress().reserved() == 3 && away.equals(ledger.ledger().get(reserved).progress().worker())
				&& ledger.ledger().get(aboard).progress().carried() == 4 && ledger.ledger().get(aboard).progress().aboard(), "and nothing moved");
		conserved(helper, ledger, ids, PLAIN, 10, "recovery refused", chest);
		RosterStatePayload cancelled = ConcordanceWorkerGameTests.act(keeper, reservedRow, RosterRequestPayload.CANCEL);
		helper.assertTrue(ConcordanceWorkerGameTests.answered(cancelled, "cancelled") && ledger.ledger().get(reserved) == null
				&& keeper.getInventory().countItem(Items.GLOWSTONE_DUST) == 0,
				"Cancelled from the roster, the reserved request closes; nothing comes out of the unloaded container");
		porter.discard();
		Request stranded = ledger.ledger().get(aboard);
		helper.assertTrue(stranded != null && stranded.state() == RequestState.STRANDED && stranded.progress().carried() == 4,
				"Its courier gone, the four are stranded at the post");
		RosterStatePayload.Row strandedRow = ConcordanceWorkerGameTests.rowOf(ConcordanceWorkerGameTests.roster(keeper), Roster.rowOf(aboard));
		helper.assertTrue(strandedRow != null && strandedRow.icon() == RosterStatePayload.STRANDED && strandedRow.offers(RosterRequestPayload.RECOVER),
				"The roster now offers their recovery");
		RosterStatePayload recovered = ConcordanceWorkerGameTests.act(keeper, strandedRow, RosterRequestPayload.RECOVER);
		helper.assertTrue(ConcordanceWorkerGameTests.answered(recovered, "recovered") && keeper.getInventory().countItem(Items.GLOWSTONE_DUST) == 4
				&& ledger.ledger().get(aboard) == null && matching(chest, PLAIN) == 6, "The requester recovers the four, from the ledger; the chest keeps its six");
		helper.assertTrue(ConcordanceWorkerGameTests.answered(ConcordanceWorkerGameTests.act(keeper, strandedRow, RosterRequestPayload.RECOVER), Roster.NOT_LISTED)
				&& Roster.act(keeper, Roster.REQUEST, Roster.rowOf(aboard), RosterRequestPayload.RECOVER, now + 60).reason().equals("not_found")
				&& keeper.getInventory().countItem(Items.GLOWSTONE_DUST) == 4, "and never twice");
		conserved(helper, ledger, ids, PLAIN, 10, "recovered", chest, keeper.getInventory());
		helper.succeed();
	}

	/**
	 * Concordance Chunk D, D-07 with D-08 (E4): the roster shows a workshop hand as its holder's agreement, the request it
	 * carries as its task, and the crucible's request as its own row (cancel only while aboard); ended from the roster, the
	 * hand departs and lets its claim go, the cargo stranded at the post; the roster then offers that cargo's recovery, which
	 * hands the two over once, from the ledger.
	 */
	@GameTest(maxTicks = 20)
	public void theRosterEndsAHandAndRecoversItsCargoOnce(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		WorkshopHands.Workshop shop = WorkshopHands.workshop(helper);
		WorkshopHandEntity hand = shop.hand();
		ServerPlayer holder = shop.holder();
		long id = shop.request();
		List<Long> ids = List.of(id);
		CourierLedger ledger = CourierLedger.of(level.getServer());
		UUID record = shop.anchor().agreement().record();
		long now = WorkshopHands.morning(level, 3_300_000L);
		long in = WorkshopHands.inHours();
		hand.think(level, in, now);
		hand.think(level, in, now + 20);
		helper.assertTrue(ledger.ledger().get(id).progress().carried() == 2 && ledger.ledger().get(id).progress().aboard(), "The hand carries two");
		WorkshopHands.conserved(helper, ids, PLAIN, 10, 0, "carried", shop.chest(), shop.crucible());
		RosterStatePayload state = ConcordanceWorkerGameTests.roster(holder);
		RosterStatePayload.Row handRow = ConcordanceWorkerGameTests.rowOf(state, record);
		RosterStatePayload.Row requestRow = ConcordanceWorkerGameTests.rowOf(state, Roster.rowOf(id));
		helper.assertTrue(handRow != null && !handRow.request() && "jugcraft:workshop_hand".equals(handRow.portrait())
				&& WorkshopHands.key(handRow.kind()).equals("message.jugcraft.concordance.workers.kind.spirit")
				&& WorkshopHands.key(handRow.name()).equals("entity.jugcraft.workshop_hand")
				&& WorkshopHands.key(handRow.service()).equals("message.jugcraft.concordance.roster.service.courier")
				&& WorkshopHands.key(handRow.task()).equals("message.jugcraft.concordance.roster.task.carrying")
				&& Long.valueOf(id).equals(ConcordanceWorkerGameTests.arg(handRow.task(), 0))
				&& handRow.offers(RosterRequestPayload.SUSPEND) && handRow.offers(RosterRequestPayload.END),
				"The hand is its holder's agreement, carrying the request: " + handRow);
		helper.assertTrue(ConcordanceWorkerGameTests.rowOf(state, hand.getUUID()) == null, "and has no second row");
		helper.assertTrue(requestRow != null && requestRow.icon() == RosterStatePayload.IN_TRANSIT && requestRow.offers(RosterRequestPayload.CANCEL)
				&& !requestRow.offers(RosterRequestPayload.RECOVER), "The crucible's request, aboard: cancel only: " + requestRow);
		RosterStatePayload ended = ConcordanceWorkerGameTests.act(holder, handRow, RosterRequestPayload.END);
		Request stranded = ledger.ledger().get(id);
		helper.assertTrue(ConcordanceWorkerGameTests.answered(ended, "ended") && hand.isRemoved() && stranded != null
				&& stranded.state() == RequestState.STRANDED && stranded.progress().carried() == 2 && "agreement_ended".equals(stranded.progress().note())
				&& BoundWills.of(level.getServer()).ledger().get(record) == null, "Ended from the roster: the hand departs, the two wait at the post");
		WorkshopHands.conserved(helper, ids, PLAIN, 10, 0, "ended (nothing dropped)", shop.chest(), shop.crucible());
		RosterStatePayload.Row strandedRow = ConcordanceWorkerGameTests.rowOf(ended, Roster.rowOf(id));
		helper.assertTrue(ConcordanceWorkerGameTests.rowOf(ended, record) == null && strandedRow != null && strandedRow.icon() == RosterStatePayload.STRANDED
				&& strandedRow.offers(RosterRequestPayload.RECOVER), "The roster now offers the stranded cargo's recovery");
		RosterStatePayload recovered = ConcordanceWorkerGameTests.act(holder, strandedRow, RosterRequestPayload.RECOVER);
		helper.assertTrue(ConcordanceWorkerGameTests.answered(recovered, "recovered") && holder.getInventory().countItem(Items.GLOWSTONE_DUST) == 2
				&& ledger.ledger().get(id) == null, "Its requester recovers the two");
		helper.assertTrue(ConcordanceWorkerGameTests.answered(ConcordanceWorkerGameTests.act(holder, strandedRow, RosterRequestPayload.RECOVER), Roster.NOT_LISTED)
				&& holder.getInventory().countItem(Items.GLOWSTONE_DUST) == 2, "and never twice");
		WorkshopHands.conserved(helper, ids, PLAIN, 10, 0, "recovered", shop.chest(), shop.crucible(), holder.getInventory());
		helper.succeed();
	}
}
