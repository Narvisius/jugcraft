package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.concordance.ConcordanceProgress;
import io.github.jimbozoomer.jugcraft.concordance.RateGate;
import io.github.jimbozoomer.jugcraft.concordance.courier.CourierLedger;
import io.github.jimbozoomer.jugcraft.concordance.courier.CourierPostBlockEntity;
import io.github.jimbozoomer.jugcraft.concordance.courier.Couriers;
import io.github.jimbozoomer.jugcraft.concordance.logistics.Logistics;
import io.github.jimbozoomer.jugcraft.concordance.logistics.Place;
import io.github.jimbozoomer.jugcraft.concordance.logistics.Request;
import io.github.jimbozoomer.jugcraft.concordance.logistics.RequestState;
import io.github.jimbozoomer.jugcraft.concordance.rules.ResearchState;
import io.github.jimbozoomer.jugcraft.concordance.spirits.ClockworkPorterEntity;
import io.github.jimbozoomer.jugcraft.concordance.spirits.Workers;
import io.github.jimbozoomer.jugcraft.concordance.worker.Body;
import io.github.jimbozoomer.jugcraft.concordance.worker.Status;
import io.github.jimbozoomer.jugcraft.concordance.worker.WorkerDefinition;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.phys.Vec3;

/**
 * Roadmap step 18, logistics with reservations and accountable transit (docs/features/arcane-concordance-logistics.md),
 * on a real server. Each test drives couriers' decisions with {@code think} at chosen times, standing them within reach
 * of the post and chests so no walking is needed, and audits after every step that each item is in a container or in
 * the ledger's cargo, exactly once ({@link CourierLedger#problems} and counted totals).
 */
public class ConcordanceLogisticsGameTests {
	private static final long NIGHT = 18000L;

	private static ServerPlayer keeper(GameTestHelper helper) {
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
}
