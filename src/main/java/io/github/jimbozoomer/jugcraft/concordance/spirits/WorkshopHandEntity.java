package io.github.jimbozoomer.jugcraft.concordance.spirits;

import com.geckolib.animation.RawAnimation;
import io.github.jimbozoomer.jugcraft.concordance.Authority;
import io.github.jimbozoomer.jugcraft.concordance.ConcordanceProgress;
import io.github.jimbozoomer.jugcraft.concordance.courier.CourierLedger;
import io.github.jimbozoomer.jugcraft.concordance.courier.CourierPostBlockEntity;
import io.github.jimbozoomer.jugcraft.concordance.logistics.Place;
import io.github.jimbozoomer.jugcraft.concordance.logistics.Request;
import io.github.jimbozoomer.jugcraft.concordance.resource.BoundWill;
import io.github.jimbozoomer.jugcraft.concordance.rules.Evidence;
import io.github.jimbozoomer.jugcraft.concordance.rules.ResearchState;
import io.github.jimbozoomer.jugcraft.concordance.worker.Agreement;
import io.github.jimbozoomer.jugcraft.concordance.worker.Pacts;
import io.github.jimbozoomer.jugcraft.concordance.worker.Status;
import io.github.jimbozoomer.jugcraft.concordance.worker.WorkerDefinition;
import io.github.jimbozoomer.jugcraft.party.JugcraftParties;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

/**
 * The Workshop Hand (Concordance Chunk D, D-08, D-S2), a spirit: the Gathering Shade's kind, bound to carry rather than to
 * gather. It is sealed under an agreement at a Spirit Anchor bound to its holder's Courier Post, only by one who has
 * mastered Stolas's Regard ({@link #regarded}; research unlocks name no worker), and recorded as a Bound Will whose
 * beneficiaries are its holder and their party (the post's own rule). With the porter's own decision ({@link CourierWork})
 * it carries that post's exact requests through the courier ledger, an Alembic Crucible's own among them: only a
 * beneficiary's, bound for a place inside its area (both checked at each new claim; a load already claimed is finished),
 * taken only from containers inside its area that its holder and the requester could open, at most its definition's carry
 * a trip (stranded cargo beyond its carry, or bound back to a source beyond its area, it leaves to porters or its
 * requester's recovery). Its hold, its hours and its day's loads gate only a new claim: a claim it holds it finishes, as a
 * shade takes its load home. It holds only its claim's id, never the items:
 * ended, released, replaced or broken away it lets the claim go (agreement_ended), killed or discarded the same
 * (worker_removed), and the cargo waits at the post, in the ledger, for another courier or one recovery by its requester;
 * unloaded, offline or switched off it does nothing and the claim lapses by itself. Its service is a spirit's (the
 * Binding Arts' key "spirit"). It never loads a chunk or uses a portal.
 */
public class WorkshopHandEntity extends WorkerEntity<WorkshopHandEntity> implements AgreementHolder.Presence {
	public static final String DEFINITION = "jugcraft:workshop_hand";
	/** Its definition's work, which its agreement keeps: an anchor keeping a courier's agreement is a hand's. */
	public static final String WORK = "courier";
	/** Stolas's Regard (Concordance Chunk D, D-S12; unit 2's research, named here only): mastered, its holder may seal a hand. */
	public static final String REGARD = "jugcraft:stolas_regard";
	/** How near it must be to a container or its post: the porter's reach. */
	public static final double REACH = 2.0;
	private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("animation.workshop_hand.idle");
	private static final RawAnimation TRAVELLING = RawAnimation.begin().thenLoop("animation.workshop_hand.travelling");
	private static final RawAnimation WORKING = RawAnimation.begin().thenLoop("animation.workshop_hand.working");
	private static final RawAnimation SUSPENDED = RawAnimation.begin().thenLoop("animation.workshop_hand.suspended");
	private static final RawAnimation WAITING = RawAnimation.begin().thenLoop("animation.workshop_hand.waiting");

	private @Nullable BlockPos anchor;
	/** Its courier work: the claim's id (saved as task), and the decision it shares with the porter. */
	private final CourierWork courier = new CourierWork(this, REACH);

	public WorkshopHandEntity(EntityType<? extends PathfinderMob> type, Level level) {
		super(type, level);
	}

	public static AttributeSupplier.Builder createAttributes() {
		return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 16.0).add(Attributes.MOVEMENT_SPEED, 0.28).add(Attributes.FOLLOW_RANGE, 32.0);
	}

	public static WorkerDefinition.@Nullable Spirit terms() {
		return Workers.catalog().get(DEFINITION, WorkerDefinition.Spirit.class);
	}

	/** Whether {@code player} may seal a Workshop Hand: Stolas's Regard mastered (D-S12), asked by the anchor before it seals. */
	public static boolean regarded(Player player) {
		return ConcordanceProgress.knowledge(player).state(REGARD).atLeast(ResearchState.MASTERED);
	}

	/** Whether {@code place} is within {@code radius} of {@code anchor} on every axis (a hand's area, Agreement.within's rule). */
	public static boolean within(BlockPos anchor, BlockPos place, int radius) {
		return Math.abs(place.getX() - anchor.getX()) <= radius && Math.abs(place.getY() - anchor.getY()) <= radius
				&& Math.abs(place.getZ() - anchor.getZ()) <= radius;
	}

	/**
	 * Whether {@code post} may be the Courier Post of a hand at {@code anchor} for {@code user} now (D-08): in this level,
	 * inside a hand's area, loaded (nothing is loaded to ask) and a post {@code user} may use.
	 */
	public static boolean mayServe(ServerLevel level, BlockPos anchor, @Nullable GlobalPos post, UUID user, int radius) {
		return post != null && level.dimension().equals(post.dimension()) && within(anchor, post.pos(), radius) && level.isLoaded(post.pos())
				&& level.getBlockEntity(post.pos()) instanceof CourierPostBlockEntity found && found.mayUse(user);
	}

	/** Why a hand under {@code record} and {@code agreement} does not serve {@code request}: IDLE, not a beneficiary's; OUTSIDE_AGREEMENT, bound beyond its area; else null. */
	static @Nullable Status refusal(BoundWill record, Agreement agreement, Request request) {
		UUID requester = request.ticket().requester();
		if (!Pacts.benefits(record, requester, JugcraftParties.sameParty(record.holder(), requester))) {
			return Status.IDLE;
		}
		Place to = request.ticket().destination();
		return to.dimension().equals(agreement.dimension()) && agreement.within(to.x(), to.y(), to.z()) ? null : Status.OUTSIDE_AGREEMENT;
	}

	@Override
	public String kind() {
		return "spirit";
	}

	/**
	 * A hand joins its holder's roster only by its seal, its recall or its own decision as its anchor's current hand (the
	 * shade's rule, unit 1's review fix 2); a change of status never puts one replaced, or loaded while the Concordance is off
	 * or its anchor unloaded, back on it (Concordance Chunk D, D-07; the hand inherited true until then).
	 */
	@Override
	protected boolean joinsRosterOnStatus() {
		return false;
	}

	public @Nullable BlockPos anchor() {
		return anchor;
	}

	public void setAnchor(BlockPos at) {
		anchor = at.immutable();
	}

	/** The request it holds a claim on, or 0. */
	public long task() {
		return courier.task();
	}

	/** What its claim has aboard (the ledger's count; it holds none itself). */
	public int carriedCount() {
		long task = courier.task();
		if (task == 0L || !(level() instanceof ServerLevel server)) {
			return 0;
		}
		Request request = CourierLedger.of(server.getServer()).ledger().get(task);
		return request != null && getUUID().equals(request.progress().worker()) && request.progress().aboard() ? request.progress().carried() : 0;
	}

	@Override
	public @Nullable BlockPos seat() {
		return anchor;
	}

	@Override
	public Status think(ServerLevel level, long time, long gameTime) {
		Status next = decide(level, time, gameTime);
		setStatus(next);
		return next;
	}

	private Status decide(ServerLevel level, long time, long gameTime) {
		WorkerDefinition.Spirit definition = terms();
		if (!Workers.enabled() || definition == null) {
			// D-S16: switched off it holds: no ledger step; a claim it holds lapses by itself, its cargo waiting at the post.
			stop();
			return Status.DISABLED;
		}
		if (anchor == null) {
			stop();
			return Status.OUTSIDE_AGREEMENT;
		}
		if (!level.isLoaded(anchor)) {
			stop();
			return Status.DESTINATION_UNLOADED;
		}
		SpiritAnchorBlockEntity home = level.getBlockEntity(anchor) instanceof SpiritAnchorBlockEntity found ? found : null;
		// D-S9: its anchor's terms count only as the world's Bound Will keeps them there.
		AgreementHolder.Standing standing = home == null ? AgreementHolder.Standing.NONE : home.holder().verify(level, gameTime);
		if (standing == AgreementHolder.Standing.UNREADABLE) {
			stop();
			return Status.OUTSIDE_AGREEMENT;
		}
		if (standing != AgreementHolder.Standing.VALID || !getUUID().equals(home.spirit())) {
			// Released, ended elsewhere, a copy's, or another hand now answers its agreement: it lets its claim go and departs.
			leave(level, home, anchor);
			return Status.FINISHED;
		}
		Agreement agreement = home.agreement();
		// D-01 (G5): its anchor's own hand keeps its roster place, so it is never taken for lost while it lives.
		if (!isRemoved() && owner != null) {
			WorkerRoster roster = WorkerRoster.of(level.getServer());
			if (!roster.workers(owner).containsKey(getUUID())) {
				roster.note(owner, getUUID(), kind(), status(), level.dimension().identifier().toString(), blockPosition());
			}
		}
		if (!level.dimension().identifier().toString().equals(agreement.dimension())) {
			stop();
			return Status.OTHER_DIMENSION;
		}
		// Roadmap step 28: it works as its holder, only while they are here (unless the server lets a stand-in answer for
		// absent owners); away, it renews nothing and its claim lapses by itself. Only its holder, present, is credited.
		if (Authority.answering(level, owner) == null) {
			stop();
			return Status.OWNER_OFFLINE;
		}
		GlobalPos post = home.post();
		if (post == null) {
			stop();
			return Status.OUTSIDE_AGREEMENT;
		}
		BoundWill record = home.holder().record(level);
		return courier.serve(level, post, courierTerms(level, home, agreement, record, definition, time, gameTime),
				Authority.present(level, owner), gameTime);
	}

	/**
	 * The hand's part of the shared courier decision: a new claim only while its Bound Will holds nothing and within its
	 * hours and day's loads (the shade's rule, Agreement.permits); only its beneficiaries' requests bound inside its area;
	 * sources inside its area; at most its definition's carry a trip, so cargo stranded at the post beyond its carry (a
	 * porter's, which has no cap of its own) and cargo to be taken back to a source beyond its area are left to porters or
	 * to their requester's one recovery (D-P82); a load delivered in full is one of its day's tasks, and each delivery a
	 * spirit's service for its holder, present. Its post gone, it waits outside its agreement.
	 */
	private CourierWork.Terms courierTerms(ServerLevel level, SpiritAnchorBlockEntity home, Agreement agreement, BoundWill record,
			WorkerDefinition.Spirit definition, long time, long gameTime) {
		return new CourierWork.Terms() {
			@Override
			public @Nullable Status ready() {
				Status held = home.holder().held(level);
				return held != null ? held : agreement.permits(agreement.work(), agreement.dimension(), agreement.x(), agreement.y(), agreement.z(), time, gameTime);
			}

			@Override
			public @Nullable Status refuses(Request request) {
				// D-P82: never more than its carry aboard, and never cargo bound back to a source beyond its area.
				if (request.progress().carried() > definition.carry()) {
					return Status.IDLE;
				}
				Place source = request.progress().source();
				if (request.progress().returning() && source != null
						&& !(source.dimension().equals(agreement.dimension()) && agreement.within(source.x(), source.y(), source.z()))) {
					return Status.IDLE;
				}
				return refusal(record, agreement, request);
			}

			@Override
			public boolean reaches(BlockPos pos) {
				return agreement.within(pos.getX(), pos.getY(), pos.getZ());
			}

			@Override
			public int load() {
				return definition.carry();
			}

			@Override
			public void atPost(ServerLevel where, BlockPos post) {
			}

			@Override
			public void delivered(ServerLevel where, @Nullable ServerPlayer keeper, boolean finished, long now) {
				if (finished) {
					home.holder().taskDone(where, now);
				}
				if (keeper != null) {
					ConcordanceProgress.record(keeper, new Evidence.Practiced(Workers.ACTIVITY, "spirit"));
				}
			}

			@Override
			public Status postGone() {
				return Status.OUTSIDE_AGREEMENT;
			}
		};
	}

	/** Its agreement ended, broken away, released or answered by another: it lets its claim go (the cargo stranded at the post), is struck off and departs. */
	@Override
	public void leave(ServerLevel level, AgreementHolder.@Nullable Holds home, BlockPos at) {
		courier.release(level, "agreement_ended");
		if (owner != null) {
			WorkerRoster.of(level.getServer()).remove(owner, getUUID());
		}
		discard();
	}

	/** Killed or discarded: it lets its claim go (the cargo waits at the post, never dropped) and is struck off, so its anchor knows it is lost (D-S18). Unloading is not this. */
	@Override
	public void remove(RemovalReason reason) {
		if (reason.shouldDestroy() && level() instanceof ServerLevel server) {
			if (owner != null) {
				WorkerRoster.of(server.getServer()).remove(owner, getUUID());
			}
			courier.release(server, "worker_removed");
		}
		super.remove(reason);
	}

	@Override
	public Component describe() {
		return Component.translatable("message.jugcraft.concordance.workers.spirit", super.describe(), carriedCount());
	}

	@Override
	protected RawAnimation animation(Status status) {
		return switch (status) {
			case TRAVELLING, RETURNING -> TRAVELLING;
			case WORKING -> WORKING;
			case SUSPENDED, UPKEEP_HELD, OUTSIDE_AGREEMENT, FINISHED, DISABLED -> SUSPENDED;
			case WAITING_FOR_RESOURCES, BLOCKED_BY_ACCESS, CANNOT_NAVIGATE, FULL, DESTINATION_UNLOADED, OWNER_OFFLINE, OTHER_DIMENSION -> WAITING;
			default -> IDLE;
		};
	}

	@Override
	protected void addAdditionalSaveData(ValueOutput output) {
		super.addAdditionalSaveData(output);
		if (anchor != null) {
			output.store("anchor", BlockPos.CODEC, anchor);
		}
		output.putLong("task", courier.task());
	}

	@Override
	protected void readAdditionalSaveData(ValueInput input) {
		super.readAdditionalSaveData(input);
		anchor = input.read("anchor", BlockPos.CODEC).orElse(null);
		courier.restore(input.getLongOr("task", 0L));
	}
}
