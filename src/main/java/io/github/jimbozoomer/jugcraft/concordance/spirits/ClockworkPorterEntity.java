package io.github.jimbozoomer.jugcraft.concordance.spirits;

import com.geckolib.animation.RawAnimation;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.jimbozoomer.jugcraft.concordance.Authority;
import io.github.jimbozoomer.jugcraft.concordance.ConcordanceProgress;
import io.github.jimbozoomer.jugcraft.concordance.LeyPylonBlockEntity;
import io.github.jimbozoomer.jugcraft.concordance.logistics.Request;
import io.github.jimbozoomer.jugcraft.concordance.rules.Evidence;
import io.github.jimbozoomer.jugcraft.concordance.worker.Body;
import io.github.jimbozoomer.jugcraft.concordance.worker.Status;
import io.github.jimbozoomer.jugcraft.concordance.worker.WorkerDefinition;
import io.github.jimbozoomer.jugcraft.party.JugcraftParties;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

/**
 * The Clockwork Porter (roadmap step 17), a construct: its {@link Body} sets what it can do. It carries up to its
 * definition's load from a source container to a target container, set with a Porter Key; each trip spends Ley Charge,
 * drawn at its source from a Ley Pylon its keeper's party may use, and wears it, and copper repairs it. It says why it
 * stops: no route, waiting for resources (an empty source), blocked by access (a container its keeper may not open),
 * full (the target), no energy, needs repair, unable to navigate, or a destination unloaded or in another dimension.
 * Its courier decision is spirits/CourierWork, shared with the workshop hand (Concordance Chunk D, D-08).
 */
public class ClockworkPorterEntity extends WorkerEntity<ClockworkPorterEntity> {
	public static final String DEFINITION = "jugcraft:clockwork_porter";
	public static final double REACH = 2.0;
	/** How far round its source a pylon may stand to fuel it. */
	public static final int PYLON_REACH = 3;
	private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("animation.clockwork_porter.idle");
	private static final RawAnimation WALKING = RawAnimation.begin().thenLoop("animation.clockwork_porter.walking");
	private static final RawAnimation WORKING = RawAnimation.begin().thenLoop("animation.clockwork_porter.working");
	private static final RawAnimation BROKEN = RawAnimation.begin().thenLoop("animation.clockwork_porter.broken");
	private static final RawAnimation WAITING = RawAnimation.begin().thenLoop("animation.clockwork_porter.waiting");

	/** A route: take from {@code source}, deliver to {@code target}, both in {@code dimension}. */
	public record Route(BlockPos source, BlockPos target, String dimension) {
		public static final Codec<Route> CODEC = RecordCodecBuilder.create(i -> i.group(
				BlockPos.CODEC.fieldOf("source").forGetter(Route::source), BlockPos.CODEC.fieldOf("target").forGetter(Route::target),
				Codec.STRING.fieldOf("dimension").forGetter(Route::dimension)).apply(i, Route::new));
	}

	private static final Codec<Body> BODY_CODEC = RecordCodecBuilder.create(i -> i.group(
			Codec.INT.fieldOf("integrity").forGetter(Body::integrity), Codec.LONG.fieldOf("energy").forGetter(Body::energy))
			.apply(i, Body::new));

	private Body body = new Body(0, 0L);
	private @Nullable Route route;
	/** Roadmap step 18: the Courier Post it serves (instead of a route), and the request it holds a claim on (0: none). */
	private @Nullable GlobalPos post;
	/** Its courier work: the claim's id, and the decision it shares with the workshop hand (Concordance Chunk D, D-08). */
	private final CourierWork courier = new CourierWork(this, REACH);
	private final List<ItemStack> carried = new ArrayList<>();

	public ClockworkPorterEntity(EntityType<? extends PathfinderMob> type, Level level) {
		super(type, level);
	}

	public static AttributeSupplier.Builder createAttributes() {
		return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 24.0).add(Attributes.MOVEMENT_SPEED, 0.22).add(Attributes.FOLLOW_RANGE, 32.0)
				.add(Attributes.ARMOR, 4.0);
	}

	@Override
	public String kind() {
		return "construct";
	}

	public Body body() {
		return body;
	}

	public void setBody(Body body) {
		this.body = body;
	}

	public @Nullable Route route() {
		return route;
	}

	public void setRoute(@Nullable Route route) {
		leavePost();
		this.route = route;
		arrived();
	}

	public @Nullable GlobalPos post() {
		return post;
	}

	/** The request it holds a claim on, or 0. */
	public long task() {
		return courier.task();
	}

	/**
	 * Binds it to a Courier Post (roadmap step 18): it takes that post's requests instead of following a route. Refused
	 * (false) while it still carries a route's load, so nothing it holds is left without a place to go.
	 */
	public boolean setPost(@Nullable GlobalPos post) {
		if (!carried.isEmpty()) {
			return false;
		}
		leavePost();
		this.route = null;
		this.post = post;
		arrived();
		return true;
	}

	/** Gives up its claim, if any: the ledger keeps whatever was in transit, stranded at the post. */
	private void leavePost() {
		if (level() instanceof ServerLevel server) {
			courier.release(server, "reassigned");
		} else {
			courier.forget();
		}
		post = null;
	}

	public int carriedCount() {
		return carried.stream().mapToInt(ItemStack::getCount).sum();
	}

	public static WorkerDefinition.@Nullable Construct terms() {
		return Workers.catalog().get(DEFINITION, WorkerDefinition.Construct.class);
	}

	/** Who may change its route, repair it or take from it: its keeper's party. */
	public boolean mayServe(Player player) {
		return owner == null || owner.equals(player.getUUID()) || JugcraftParties.sameParty(owner, player.getUUID());
	}

	@Override
	public Status think(ServerLevel level, long time, long gameTime) {
		Status next = decide(level, gameTime);
		setStatus(next);
		return next;
	}

	private Status decide(ServerLevel level, long gameTime) {
		WorkerDefinition.Construct terms = terms();
		if (!Workers.enabled() || terms == null) {
			stop();
			return Status.DISABLED;
		}
		if (body.integrity() <= 0) {
			stop();
			return Status.NEEDS_REPAIR;
		}
		// Roadmap step 28: it works as its owner, only where they could reach by hand, and only while they are here
		// (unless the server lets a stand-in answer for absent owners). Only the owner present is credited.
		if (Authority.answering(level, owner) == null) {
			stop();
			return Status.OWNER_OFFLINE;
		}
		ServerPlayer keeper = Authority.present(level, owner);
		if (post != null) {
			return courier.serve(level, post, courierTerms(terms), keeper, gameTime);
		}
		if (route == null) {
			stop();
			return Status.IDLE;
		}
		if (!level.dimension().identifier().toString().equals(route.dimension())) {
			stop();
			return Status.OTHER_DIMENSION;
		}
		if (carried.isEmpty()) {
			return fetch(level, terms, keeper, gameTime);
		}
		return deliver(level, terms, keeper, gameTime);
	}

	private Status fetch(ServerLevel level, WorkerDefinition.Construct terms, @Nullable ServerPlayer keeper, long gameTime) {
		BlockPos source = route.source();
		if (!level.isLoaded(source)) {
			stop();
			return Status.DESTINATION_UNLOADED;
		}
		if (!near(centre(source), REACH + 0.5)) {
			return walkTo(centre(source), 1.0F, 1, gameTime) ? Status.TRAVELLING : Status.CANNOT_NAVIGATE;
		}
		arrived();
		refuel(level, terms, source);
		if (body.ready(terms) != null) {
			return body.ready(terms);
		}
		Container container = CourierWork.container(level, source);
		if (container == null) {
			return Status.WAITING_FOR_RESOURCES;
		}
		if (!Authority.mayChangeFor(level, owner, source)) {
			return Status.BLOCKED_BY_ACCESS;
		}
		int room = terms.carry();
		for (int slot = 0; slot < container.getContainerSize() && room > 0; slot++) {
			ItemStack stack = container.getItem(slot);
			if (stack.isEmpty()) {
				continue;
			}
			ItemStack taken = container.removeItem(slot, Math.min(room, stack.getCount()));
			room -= taken.getCount();
			carried.add(taken);
		}
		container.setChanged();
		return carried.isEmpty() ? Status.WAITING_FOR_RESOURCES : Status.WORKING;
	}

	private Status deliver(ServerLevel level, WorkerDefinition.Construct terms, @Nullable ServerPlayer keeper, long gameTime) {
		BlockPos target = route.target();
		if (!level.isLoaded(target)) {
			stop();
			return Status.DESTINATION_UNLOADED;
		}
		if (!near(centre(target), REACH + 0.5)) {
			return walkTo(centre(target), 1.0F, 1, gameTime) ? Status.TRAVELLING : Status.CANNOT_NAVIGATE;
		}
		arrived();
		Container container = CourierWork.container(level, target);
		if (container == null) {
			return Status.FULL;
		}
		if (!Authority.mayChangeFor(level, owner, target)) {
			return Status.BLOCKED_BY_ACCESS;
		}
		List<ItemStack> left = new ArrayList<>();
		for (ItemStack stack : carried) {
			insert(container, stack);
			if (!stack.isEmpty()) {
				left.add(stack);
			}
		}
		container.setChanged();
		carried.clear();
		carried.addAll(left);
		if (!carried.isEmpty()) {
			return Status.FULL;
		}
		body = body.trip(terms);
		if (keeper != null) {
			ConcordanceProgress.record(keeper, new Evidence.Practiced(Workers.ACTIVITY, "construct"));
		}
		return Status.WORKING;
	}

	// ---------------------------------------------------------------- courier work (roadmap step 18)

	/**
	 * The porter's part of the shared courier decision (Concordance Chunk D, D-08: CourierWork, extracted from here): its
	 * body must be ready for a new claim; it serves every request at its post, from any container round it, with no cap of
	 * its own; it fuels at a pylon there; each delivery is a trip that wears it and records a construct's service for its
	 * keeper, present; its post gone, it forgets it and is idle.
	 */
	private CourierWork.Terms courierTerms(WorkerDefinition.Construct terms) {
		return new CourierWork.Terms() {
			@Override
			public @Nullable Status ready() {
				return body.ready(terms);
			}

			@Override
			public @Nullable Status refuses(Request request) {
				return null;
			}

			@Override
			public boolean reaches(BlockPos pos) {
				return true;
			}

			@Override
			public int load() {
				return Integer.MAX_VALUE;
			}

			@Override
			public void atPost(ServerLevel level, BlockPos at) {
				refuel(level, terms, at);
			}

			@Override
			public void delivered(ServerLevel level, @Nullable ServerPlayer keeper, boolean finished, long now) {
				body = body.trip(terms);
				if (keeper != null) {
					ConcordanceProgress.record(keeper, new Evidence.Practiced(Workers.ACTIVITY, "construct"));
				}
			}

			@Override
			public Status postGone() {
				post = null;
				return Status.IDLE;
			}
		};
	}

	/** Draws Ley Charge from a pylon near its source that its keeper's party may use, as much as it has room for. */
	private void refuel(ServerLevel level, WorkerDefinition.Construct terms, BlockPos around) {
		long room = body.room(terms);
		if (room <= 0 || owner == null) {
			return;
		}
		Set<UUID> party = new java.util.HashSet<>(JugcraftParties.partyMembers(owner));
		party.add(owner);
		for (BlockPos pos : BlockPos.betweenClosed(around.offset(-PYLON_REACH, -1, -PYLON_REACH), around.offset(PYLON_REACH, 1, PYLON_REACH))) {
			if (level.isLoaded(pos) && level.getBlockEntity(pos) instanceof LeyPylonBlockEntity pylon && pylon.lends(party)) {
				long take = Math.min(room, pylon.ley());
				if (take > 0 && pylon.draw(level, take)) {
					body = body.charged(terms, take);
					return;
				}
			}
		}
	}

	/** Puts as much of {@code stack} into {@code container} as it takes; {@code stack} keeps the rest. */
	static void insert(Container container, ItemStack stack) {
		for (int slot = 0; slot < container.getContainerSize() && !stack.isEmpty(); slot++) {
			if (!container.canPlaceItem(slot, stack)) {
				continue;
			}
			ItemStack in = container.getItem(slot);
			int limit = Math.min(container.getMaxStackSize(), stack.getMaxStackSize());
			if (in.isEmpty()) {
				container.setItem(slot, stack.split(Math.min(stack.getCount(), limit)));
			} else if (ItemStack.isSameItemSameComponents(in, stack) && in.getCount() < limit) {
				int moved = Math.min(stack.getCount(), limit - in.getCount());
				in.grow(moved);
				stack.shrink(moved);
			}
		}
	}

	/** Its keeper's party mends it with its repair item, one at a time. */
	@Override
	protected InteractionResult mobInteract(Player player, InteractionHand hand) {
		ItemStack held = player.getItemInHand(hand);
		WorkerDefinition.Construct terms = terms();
		Identifier repair = terms == null ? null : Identifier.tryParse(terms.repairItem());
		if (repair != null && !held.isEmpty() && BuiltInRegistries.ITEM.getKey(held.getItem()).equals(repair)) {
			if (player instanceof ServerPlayer server && mayServe(server) && body.integrity() < terms.integrity()) {
				body = body.repaired(terms);
				held.consume(1, server);
				server.sendSystemMessage(describe());
			}
			return InteractionResult.SUCCESS;
		}
		return super.mobInteract(player, hand);
	}

	@Override
	public Component describe() {
		WorkerDefinition.Construct terms = terms();
		return Component.translatable("message.jugcraft.concordance.workers.construct", super.describe(), body.integrity(),
				terms == null ? 0 : terms.integrity(), body.energy(), terms == null ? 0 : terms.energy(), carriedCount());
	}

	/** Killed or dismantled: what it carried drops; it is struck off its keeper's roster. */
	@Override
	public void remove(RemovalReason reason) {
		if (reason.shouldDestroy() && level() instanceof ServerLevel server) {
			for (ItemStack stack : carried) {
				Block.popResource(server, blockPosition(), stack);
			}
			carried.clear();
			if (owner != null) {
				WorkerRoster.of(server.getServer()).remove(owner, getUUID());
			}
			// Its cargo was never aboard it but in the ledger: released, it waits at the post for another courier.
			courier.release(server, "worker_removed");
		}
		super.remove(reason);
	}

	@Override
	protected RawAnimation animation(Status status) {
		return switch (status) {
			case TRAVELLING, RETURNING -> WALKING;
			case WORKING -> WORKING;
			case NEEDS_REPAIR, NO_ENERGY, DISABLED -> BROKEN;
			case WAITING_FOR_RESOURCES, BLOCKED_BY_ACCESS, CANNOT_NAVIGATE, FULL, DESTINATION_UNLOADED, OWNER_OFFLINE -> WAITING;
			default -> IDLE;
		};
	}

	@Override
	protected void addAdditionalSaveData(ValueOutput output) {
		super.addAdditionalSaveData(output);
		output.store("body", BODY_CODEC, body);
		if (route != null) {
			output.store("route", Route.CODEC, route);
		}
		if (post != null) {
			output.store("post", GlobalPos.CODEC, post);
		}
		output.putLong("task", courier.task());
		output.store("carried", ItemStack.CODEC.listOf(), List.copyOf(carried));
	}

	@Override
	protected void readAdditionalSaveData(ValueInput input) {
		super.readAdditionalSaveData(input);
		body = input.read("body", BODY_CODEC).orElse(new Body(0, 0L));
		route = input.read("route", Route.CODEC).orElse(null);
		post = input.read("post", GlobalPos.CODEC).orElse(null);
		courier.restore(input.getLongOr("task", 0L));
		carried.clear();
		input.read("carried", ItemStack.CODEC.listOf()).ifPresent(list -> list.stream().filter(stack -> !stack.isEmpty()).limit(64)
				.forEach(carried::add));
	}
}
