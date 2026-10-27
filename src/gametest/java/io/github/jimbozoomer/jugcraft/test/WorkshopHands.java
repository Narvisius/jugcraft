package io.github.jimbozoomer.jugcraft.test;

import io.github.jimbozoomer.jugcraft.concordance.ConcordanceProgress;
import io.github.jimbozoomer.jugcraft.concordance.CrucibleBlockEntity;
import io.github.jimbozoomer.jugcraft.concordance.JugcraftConcordance;
import io.github.jimbozoomer.jugcraft.concordance.RateGate;
import io.github.jimbozoomer.jugcraft.concordance.alchemy.Formula;
import io.github.jimbozoomer.jugcraft.concordance.courier.CourierLedger;
import io.github.jimbozoomer.jugcraft.concordance.courier.CourierPostBlockEntity;
import io.github.jimbozoomer.jugcraft.concordance.courier.Couriers;
import io.github.jimbozoomer.jugcraft.concordance.diagnostic.Code;
import io.github.jimbozoomer.jugcraft.concordance.diagnostic.Diagnostic;
import io.github.jimbozoomer.jugcraft.concordance.logistics.Place;
import io.github.jimbozoomer.jugcraft.concordance.logistics.Request;
import io.github.jimbozoomer.jugcraft.concordance.rules.ResearchState;
import io.github.jimbozoomer.jugcraft.concordance.spirits.BoundWills;
import io.github.jimbozoomer.jugcraft.concordance.spirits.SpiritAnchorBlock;
import io.github.jimbozoomer.jugcraft.concordance.spirits.SpiritAnchorBlockEntity;
import io.github.jimbozoomer.jugcraft.concordance.spirits.Workers;
import io.github.jimbozoomer.jugcraft.concordance.spirits.WorkshopHandEntity;
import io.github.jimbozoomer.jugcraft.concordance.worker.Agreement;
import io.github.jimbozoomer.jugcraft.concordance.worker.Pacts;
import io.github.jimbozoomer.jugcraft.concordance.worker.WorkerDefinition;
import java.util.List;
import java.util.UUID;
import java.util.function.Predicate;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Concordance Chunk D, unit 4 (D-08): the workshop hand's fixtures, shared by the logistics, worker, authority and
 * persistence suites. A hand is made as a player makes one: a Courier Post, a Spirit Anchor placed by its owner and bound
 * to the post with a Porter Key, and a seal by a holder who has mastered Stolas's Regard (granted by its id; unit 2 owns
 * the entry). The post (2,1,2), its chest (1,1,2) and the crucible (3,1,2) are within reach of where the hand stands
 * (2.5,1,3.5), so no walking is needed; the anchor is at (2,1,5). Every check audits the courier ledger and the Bound
 * Wills and counts what it follows exactly once: in containers, carried by requests, or loose.
 */
final class WorkshopHands {
	static final BlockPos POST = new BlockPos(2, 1, 2);
	static final BlockPos CHEST = new BlockPos(1, 1, 2);
	static final BlockPos CRUCIBLE = new BlockPos(3, 1, 2);
	static final BlockPos ANCHOR = new BlockPos(2, 1, 5);
	static final String SEALED = "message.jugcraft.concordance.workers.hand_sealed";
	static final String UNKNOWN = "message.jugcraft.concordance.workers.hand_unknown";
	static final String TWO_GLOW = "water 2; add minecraft:glowstone_dust jugcraft:raw; add minecraft:glowstone_dust jugcraft:raw; stir hot; stir hot";
	static final Predicate<ItemStack> PLAIN = stack -> stack.is(Items.GLOWSTONE_DUST);
	/** A neighbour whose claims the tests set (never online). */
	static final UUID NEIGHBOUR = new UUID(0xD08L, 1L);

	private WorkshopHands() {
	}

	/** The common workshop: its holder's post, a chest of ten glowstone, a crucible asking for two, and their hand; nothing claimed. */
	record Workshop(ServerPlayer holder, CourierPostBlockEntity post, ChestBlockEntity chest, CrucibleBlockEntity crucible, long request,
			SpiritAnchorBlockEntity anchor, WorkshopHandEntity hand) {
	}

	static Workshop workshop(GameTestHelper helper) {
		ServerPlayer holder = holder(helper);
		CourierPostBlockEntity post = post(helper, holder, POST);
		ChestBlockEntity chest = chest(helper, CHEST, 10);
		CrucibleBlockEntity crucible = crucible(helper, CRUCIBLE, holder);
		WorkshopHandEntity hand = hand(helper, holder, ANCHOR, POST);
		return new Workshop(holder, post, chest, crucible, crucible.request(), helper.getBlockEntity(ANCHOR, SpiritAnchorBlockEntity.class), hand);
	}

	/** The hand's definition: its hours leave part of the day out, and its area reaches this layout's farthest block (4). */
	static WorkerDefinition.Spirit terms() {
		WorkerDefinition.Spirit terms = WorkshopHandEntity.terms();
		if (terms == null || terms.from() == terms.to() || terms.radius() < 4) {
			throw new IllegalStateException("the workshop hand's definition does not fit these tests: " + terms);
		}
		return terms;
	}

	/** A world-clock time in the hand's hours (their first tick). */
	static long inHours() {
		return terms().from();
	}

	/** One outside them (the tick their end excludes). */
	static long outOfHours() {
		return terms().to();
	}

	/** The start of the game day after {@code ahead} ticks from now, so a test's quota reads all fall on one day. */
	static long morning(ServerLevel level, long ahead) {
		return (level.getGameTime() + ahead) / Agreement.DAY * Agreement.DAY + Agreement.DAY;
	}

	/** {@code player} taught First Light, the Binding Arts and Stolas's Regard to {@code regard} (null: never met), nothing of theirs rate-limited. */
	static ServerPlayer taught(ServerPlayer player, @Nullable ResearchState regard) {
		ConcordanceProgress.grant(player, "jugcraft:first_light", ResearchState.UNDERSTOOD);
		ConcordanceProgress.grant(player, Workers.RESEARCH, ResearchState.UNDERSTOOD);
		if (regard != null) {
			ConcordanceProgress.grant(player, WorkshopHandEntity.REGARD, regard);
		}
		RateGate.forget(player.getUUID());
		return player;
	}

	/** An ordinary survival player (TestPlayers.survivor) who has mastered Stolas's Regard. */
	static ServerPlayer holder(GameTestHelper helper) {
		return taught(TestPlayers.survivor(helper), ResearchState.MASTERED);
	}

	static CourierPostBlockEntity post(GameTestHelper helper, ServerPlayer owner, BlockPos at) {
		helper.setBlock(at, Couriers.COURIER_POST);
		CourierPostBlockEntity post = helper.getBlockEntity(at, CourierPostBlockEntity.class);
		post.setOwner(owner.getUUID());
		return post;
	}

	static ChestBlockEntity chest(GameTestHelper helper, BlockPos at, int glowstone) {
		helper.setBlock(at, Blocks.CHEST);
		ChestBlockEntity chest = helper.getBlockEntity(at, ChestBlockEntity.class);
		chest.setItem(0, new ItemStack(Items.GLOWSTONE_DUST, glowstone));
		return chest;
	}

	/** A crucible following TWO_GLOW for {@code setter}, its water poured: two pulses and it has asked its setter's post for two glowstone (C-06). */
	static CrucibleBlockEntity crucible(GameTestHelper helper, BlockPos at, ServerPlayer setter) {
		helper.setBlock(at, JugcraftConcordance.CRUCIBLE);
		CrucibleBlockEntity crucible = helper.getBlockEntity(at, CrucibleBlockEntity.class);
		crucible.follow(Formula.parse(TWO_GLOW), setter.getUUID());
		try (Transaction transaction = Transaction.openOuter()) {
			crucible.water.insert(FluidVariant.of(Fluids.WATER), 2 * FluidConstants.BOTTLE, transaction);
			transaction.commit();
		}
		Diagnostic poured = crucible.automate(helper.getLevel());
		Diagnostic waits = crucible.automate(helper.getLevel());
		helper.assertTrue(poured == null && waits != null && waits.code() == Code.MISSING_INGREDIENT && crucible.request() > 0L,
				"The crucible waits for its glowstone and has asked its setter's post: " + waits + ", request " + crucible.request());
		return crucible;
	}

	/** A Spirit Anchor at {@code at}, placed by {@code owner}, who is recorded as its owner (D-S8). */
	static SpiritAnchorBlockEntity anchor(GameTestHelper helper, ServerPlayer owner, BlockPos at) {
		helper.setBlock(at, Workers.SPIRIT_ANCHOR);
		ServerLevel level = helper.getLevel();
		BlockPos absolute = helper.absolutePos(at);
		Workers.SPIRIT_ANCHOR.setPlacedBy(level, absolute, level.getBlockState(absolute), owner, new ItemStack(Workers.SPIRIT_ANCHOR));
		return helper.getBlockEntity(at, SpiritAnchorBlockEntity.class);
	}

	/**
	 * Why {@code player} could or could not bind the anchor at {@code anchorAt} to the post at {@code postAt} now: each of
	 * PorterKeyItem.bind's conditions, read without changing anything, for a failed binding's message.
	 */
	static String why(GameTestHelper helper, ServerPlayer player, BlockPos anchorAt, BlockPos postAt) {
		ServerLevel level = helper.getLevel();
		BlockPos at = helper.absolutePos(anchorAt);
		SpiritAnchorBlockEntity anchor = level.getBlockEntity(at) instanceof SpiritAnchorBlockEntity found ? found : null;
		WorkerDefinition.Spirit terms = WorkshopHandEntity.terms();
		net.minecraft.core.GlobalPos post = net.minecraft.core.GlobalPos.of(level.dimension(), helper.absolutePos(postAt));
		net.minecraft.world.level.block.state.BlockState state = level.getBlockState(at);
		return " [enabled " + Workers.enabled() + ", terms " + terms + ", anchor " + (anchor != null)
				+ (anchor == null ? "" : ", owner " + player.getUUID().equals(anchor.holder().owner()) + " (" + anchor.holder().owner() + ")"
						+ ", standing " + anchor.holder().verify(level, level.getGameTime()) + ", maySeal " + anchor.holder().maySeal(level, player, true))
				+ ", creative " + player.isCreative() + ", standIn " + io.github.jimbozoomer.jugcraft.concordance.Authority.isStandIn(player)
				+ ", mayChange " + io.github.jimbozoomer.jugcraft.concordance.Authority.mayChange(level, player, at)
				+ ", mayInteract " + level.mayInteract(player, at) + ", mayUseItemAt " + player.mayUseItemAt(at, Direction.UP, ItemStack.EMPTY)
				+ ", town " + io.github.jimbozoomer.jugcraft.town.TownProtection.denies(player, level, at)
				+ ", breakAllowed " + net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents.BEFORE.invoker().beforeBlockBreak(level, player, at, state, level.getBlockEntity(at))
				+ ", mayServe " + (terms != null && WorkshopHandEntity.mayServe(level, at, post, player.getUUID(), terms.radius()))
				+ ", shift " + player.isShiftKeyDown() + "]";
	}

	/** A Porter Key {@code player} has used on the post at {@code post}: it names the post only if they may use it. */
	static ItemStack keyFor(GameTestHelper helper, ServerPlayer player, BlockPos post) {
		ItemStack key = new ItemStack(Workers.PORTER_KEY);
		use(helper, player, key, post);
		return key;
	}

	/**
	 * {@code player} uses {@code held} on the block at {@code at} from above, as a click does, with no rate limit in the way;
	 * the item's answer, any success read as SUCCESS (26.3's ItemStack.useOn hands back a new Success for an item's
	 * interaction, never the constant itself).
	 */
	static InteractionResult use(GameTestHelper helper, ServerPlayer player, ItemStack held, BlockPos at) {
		RateGate.forget(player.getUUID());
		BlockPos absolute = helper.absolutePos(at);
		ItemStack before = player.getItemInHand(InteractionHand.MAIN_HAND);
		player.setItemInHand(InteractionHand.MAIN_HAND, held);
		try {
			InteractionResult answer = held.useOn(new UseOnContext(player, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atCenterOf(absolute), Direction.UP, absolute, false)));
			return answer instanceof InteractionResult.Success ? InteractionResult.SUCCESS : answer;
		} finally {
			player.setItemInHand(InteractionHand.MAIN_HAND, before);
		}
	}

	/** {@code player}'s empty hand on {@code anchor}: what it answered. */
	static Component seal(GameTestHelper helper, ServerPlayer player, SpiritAnchorBlockEntity anchor) {
		return SpiritAnchorBlock.use(player, helper.getLevel(), anchor, false);
	}

	/** {@code holder}'s anchor at {@code anchorAt}, bound to the post at {@code postAt} with their key, their hand sealed there, standing by the post. */
	static WorkshopHandEntity hand(GameTestHelper helper, ServerPlayer holder, BlockPos anchorAt, BlockPos postAt) {
		SpiritAnchorBlockEntity anchor = anchor(helper, holder, anchorAt);
		helper.assertTrue(use(helper, holder, keyFor(helper, holder, postAt), anchorAt) == InteractionResult.SUCCESS
				&& net.minecraft.core.GlobalPos.of(helper.getLevel().dimension(), helper.absolutePos(postAt)).equals(anchor.post()),
				"The holder binds their anchor to the post" + why(helper, holder, anchorAt, postAt));
		helper.assertValueEqual(key(seal(helper, holder, anchor)), SEALED, "and seals a workshop hand there");
		return answered(helper, holder, anchor);
	}

	/** {@code holder}'s anchor at ANCHOR bound to the post at POST, a hand sealed there at {@code now} under test terms (a narrower or wider area), standing by the post. */
	static WorkshopHandEntity handUnder(GameTestHelper helper, ServerPlayer holder, WorkerDefinition.Spirit terms, long now) {
		ServerLevel level = helper.getLevel();
		SpiritAnchorBlockEntity anchor = anchor(helper, holder, ANCHOR);
		helper.assertTrue(use(helper, holder, keyFor(helper, holder, POST), ANCHOR) == InteractionResult.SUCCESS,
				"The holder binds their anchor to the post" + why(helper, holder, ANCHOR, POST));
		helper.assertValueEqual(anchor.holder().seal(level, holder, anchor.handSealing(terms), now).outcome(), Pacts.Outcome.DONE,
				"A hand sealed there under test terms");
		return answered(helper, holder, anchor);
	}

	private static WorkshopHandEntity answered(GameTestHelper helper, ServerPlayer holder, SpiritAnchorBlockEntity anchor) {
		WorkshopHandEntity hand = helper.getLevel().getEntity(anchor.spirit()) instanceof WorkshopHandEntity found ? found : null;
		helper.assertTrue(hand != null && holder.getUUID().equals(hand.owner()), "The hand answers its holder");
		stand(helper, hand, 2.5, 1.0, 3.5);
		return hand;
	}

	static void stand(GameTestHelper helper, Entity entity, double x, double y, double z) {
		Vec3 at = helper.absoluteVec(new Vec3(x, y, z));
		entity.snapTo(at.x, at.y, at.z, 0.0F, 0.0F);
	}

	static String key(@Nullable Component line) {
		return line != null && line.getContents() instanceof TranslatableContents contents ? contents.getKey() : "";
	}

	/** The newest request filed at the post at {@code postAt}. */
	static long lastFiled(GameTestHelper helper, BlockPos postAt) {
		Place at = Couriers.place(helper.getLevel(), helper.absolutePos(postAt));
		return CourierLedger.of(helper.getLevel().getServer()).ledger().requests().stream().filter(r -> r.ticket().post().equals(at))
				.mapToLong(Request::id).max().orElse(-1L);
	}

	/** The kinds of request {@code id}'s events in the history of the post at {@code postAt}, oldest first. */
	static List<String> story(GameTestHelper helper, BlockPos postAt, long id) {
		Place at = Couriers.place(helper.getLevel(), helper.absolutePos(postAt));
		return CourierLedger.of(helper.getLevel().getServer()).ledger().history(at).stream().filter(e -> e.request() == id).map(e -> e.kind()).toList();
	}

	static int matching(Container container, Predicate<ItemStack> which) {
		int count = 0;
		for (int slot = 0; slot < container.getContainerSize(); slot++) {
			if (which.test(container.getItem(slot))) {
				count += container.getItem(slot).getCount();
			}
		}
		return count;
	}

	static int loose(GameTestHelper helper, Predicate<ItemStack> which) {
		return helper.getLevel().getEntitiesOfClass(ItemEntity.class, helper.getBounds(), item -> item.isAlive() && which.test(item.getItem()))
				.stream().mapToInt(item -> item.getItem().getCount()).sum();
	}

	/** After every step: the ledger agrees with its cargo, the Bound Wills keep the pact rules, exactly {@code loose} lie loose, and held + carried by {@code ids} + loose is {@code n}. */
	static void conserved(GameTestHelper helper, List<Long> ids, Predicate<ItemStack> which, int n, int loose, String when, Container... containers) {
		MinecraftServer server = helper.getLevel().getServer();
		CourierLedger ledger = CourierLedger.of(server);
		int held = 0;
		for (Container container : containers) {
			held += matching(container, which);
		}
		int carried = 0;
		for (long id : ids) {
			Request request = ledger.ledger().get(id);
			carried += request == null ? 0 : request.progress().carried();
		}
		int lying = loose(helper, which);
		helper.assertTrue(ledger.problems().isEmpty(), when + ": the ledger and its cargo agree: " + ledger.problems());
		helper.assertTrue(BoundWills.of(server).problems().isEmpty(), when + ": the Bound Wills keep the pact rules: " + BoundWills.of(server).problems());
		helper.assertTrue(lying == loose, when + ": " + lying + " loose, not " + loose);
		helper.assertTrue(held + carried + lying == n, when + ": " + held + " held, " + carried + " carried and " + lying + " loose, not " + n);
	}
}
