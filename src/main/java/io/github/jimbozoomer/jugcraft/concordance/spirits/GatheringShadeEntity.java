package io.github.jimbozoomer.jugcraft.concordance.spirits;

import com.geckolib.animation.RawAnimation;
import io.github.jimbozoomer.jugcraft.concordance.Authority;
import io.github.jimbozoomer.jugcraft.concordance.ConcordanceProgress;
import io.github.jimbozoomer.jugcraft.concordance.rules.Evidence;
import io.github.jimbozoomer.jugcraft.concordance.worker.Agreement;
import io.github.jimbozoomer.jugcraft.concordance.worker.Status;
import io.github.jimbozoomer.jugcraft.concordance.worker.WorkerDefinition;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Containers;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * The Gathering Shade (roadmap step 17), a spirit: it works only within its {@link Agreement}, kept by its Spirit
 * Anchor. Within the agreement's hours, area and daily quota it gathers dropped items round the anchor and delivers
 * them there; outside them it waits at the anchor and says why (outside its agreement, finished for the day,
 * suspended). It refuses items where its holder could not build (a protected town, spawn protection: "blocked by
 * access"), waits when its anchor is full, and stops when its anchor's chunk is unloaded ("destination unloaded"). It
 * never uses portals; released, it departs, leaving what it carried at the anchor. Since Concordance Chunk D (D-01, D-02)
 * it serves only while its anchor's Bound Will keeps the agreement there and names it as the anchor's spirit, and waits
 * while that Bound Will holds it (suspended by its holder, or for its upkeep). Killed, what it carried goes into its
 * anchor when that is loaded and keeps its holder's agreement, otherwise where it fell, and it is struck off its holder's
 * roster, which is how its anchor knows it is lost; released, ended or replaced, it leaves what it carried in its anchor
 * once and departs.
 */
public class GatheringShadeEntity extends WorkerEntity<GatheringShadeEntity> implements AgreementHolder.Presence {
	public static final String DEFINITION = "jugcraft:gathering_shade";
	/** How near it must be to an item or its anchor to take or deliver. */
	public static final double REACH = 1.6;
	private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("animation.gathering_shade.idle");
	private static final RawAnimation TRAVELLING = RawAnimation.begin().thenLoop("animation.gathering_shade.travelling");
	private static final RawAnimation WORKING = RawAnimation.begin().thenLoop("animation.gathering_shade.working");
	private static final RawAnimation SUSPENDED = RawAnimation.begin().thenLoop("animation.gathering_shade.suspended");
	private static final RawAnimation WAITING = RawAnimation.begin().thenLoop("animation.gathering_shade.waiting");

	private @Nullable BlockPos anchor;
	private final List<ItemStack> carried = new ArrayList<>();

	public GatheringShadeEntity(EntityType<? extends PathfinderMob> type, Level level) {
		super(type, level);
	}

	public static AttributeSupplier.Builder createAttributes() {
		return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 16.0).add(Attributes.MOVEMENT_SPEED, 0.28).add(Attributes.FOLLOW_RANGE, 24.0);
	}

	@Override
	public String kind() {
		return "spirit";
	}

	/**
	 * A shade joins its holder's roster only by its seal, its recall or its own decision as its anchor's current spirit
	 * (D-01, G5); a change of status never puts one replaced, switched off or away from its anchor back on it (the unit
	 * review's fix 2, completing correction 11).
	 */
	@Override
	protected boolean joinsRosterOnStatus() {
		return false;
	}

	public @Nullable BlockPos anchor() {
		return anchor;
	}

	public void setAnchor(BlockPos anchor) {
		this.anchor = anchor.immutable();
	}

	public int carriedCount() {
		return carried.stream().mapToInt(ItemStack::getCount).sum();
	}

	@Override
	public Status think(ServerLevel level, long time, long gameTime) {
		Status next = decide(level, time, gameTime);
		setStatus(next);
		return next;
	}

	private Status decide(ServerLevel level, long time, long gameTime) {
		WorkerDefinition.Spirit terms = Workers.catalog().get(DEFINITION, WorkerDefinition.Spirit.class);
		if (!Workers.enabled() || terms == null) {
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
		// Concordance Chunk D (D-01, D-02, D-S9): its anchor's terms count only as the world's Bound Will keeps them there.
		AgreementHolder.Standing standing = home == null ? AgreementHolder.Standing.NONE : home.holder().verify(level, gameTime);
		if (standing == AgreementHolder.Standing.UNREADABLE) {
			// Its Bound Will is kept as written by a version that cannot read it: it waits, and leaves nothing.
			stop();
			return Status.OUTSIDE_AGREEMENT;
		}
		if (standing != AgreementHolder.Standing.VALID || !getUUID().equals(home.spirit())) {
			// Released, ended elsewhere, a copy's, or another shade now answers its agreement (its holder called one while it
			// was away): it leaves what it carried at its anchor, once, and departs (D-S18).
			depart(level, home, anchor);
			return Status.FINISHED;
		}
		Agreement agreement = home.agreement();
		Status held = home.holder().held(level);
		// D-01 (G5): its anchor's own shade, sealed before its roster noted it at once or while that roster was full, notes
		// itself once there is room, so its anchor never takes it for lost while it lives (critique 11: only the anchor's
		// current spirit, never one replaced or switched off).
		if (!isRemoved() && owner != null) {
			WorkerRoster roster = WorkerRoster.of(level.getServer());
			if (!roster.workers(owner).containsKey(getUUID())) {
				roster.note(owner, getUUID(), kind(), status(), level.dimension().identifier().toString(), blockPosition());
			}
		}
		String dimension = level.dimension().identifier().toString();
		if (!dimension.equals(agreement.dimension())) {
			stop();
			return Status.OTHER_DIMENSION;
		}
		Vec3 home3 = centre(anchor.above());
		// What it carries goes home first, whatever else is true (it never keeps a holder's things).
		if (!carried.isEmpty() && (held != null || carriedCount() >= terms.carry() || agreement.permits(agreement.work(), dimension, agreement.x(),
				agreement.y(), agreement.z(), time, gameTime) != null || nearestItem(level, agreement, terms) == null)) {
			if (!near(home3, REACH + 1.0)) {
				return walkTo(home3, 1.0F, 1, gameTime) ? Status.RETURNING : Status.CANNOT_NAVIGATE;
			}
			arrived();
			int before = carriedCount();
			List<ItemStack> left = new ArrayList<>();
			for (ItemStack stack : carried) {
				ItemStack rest = home.deliver(stack);
				if (!rest.isEmpty()) {
					left.add(rest);
				}
			}
			carried.clear();
			carried.addAll(left);
			if (carriedCount() < before) {
				home.taskDone(gameTime);
				ServerPlayer holder = level.getServer().getPlayerList().getPlayer(agreement.holder());
				if (holder != null) {
					ConcordanceProgress.record(holder, new Evidence.Practiced(Workers.ACTIVITY, "spirit"));
				}
			}
			return carried.isEmpty() ? Status.IDLE : Status.FULL;
		}
		Status refused = held != null ? held : agreement.permits(agreement.work(), dimension, agreement.x(), agreement.y(), agreement.z(), time, gameTime);
		if (refused != null) {
			if (!near(home3, REACH + 1.0)) {
				walkTo(home3, 1.0F, 1, gameTime);
			} else {
				arrived();
			}
			return refused;
		}
		// Roadmap step 28: it takes only what its holder could pick up there, and only while they are here (unless the
		// server lets a stand-in answer for absent owners).
		if (Authority.answering(level, agreement.holder()) == null) {
			if (!near(home3, REACH + 1.0)) {
				walkTo(home3, 1.0F, 1, gameTime);
			} else {
				arrived();
			}
			return Status.OWNER_OFFLINE;
		}
		ItemEntity item = nearestItem(level, agreement, terms);
		if (item == null) {
			if (!near(home3, REACH + 1.0)) {
				walkTo(home3, 1.0F, 1, gameTime);
			}
			return blocked(level, agreement) ? Status.BLOCKED_BY_ACCESS : Status.IDLE;
		}
		if (!near(item.position(), REACH)) {
			return walkTo(item.position(), 1.0F, 0, gameTime) ? Status.TRAVELLING : Status.CANNOT_NAVIGATE;
		}
		arrived();
		ItemStack stack = item.getItem();
		int take = Math.min(stack.getCount(), terms.carry() - carriedCount());
		carried.add(stack.split(take));
		if (stack.isEmpty()) {
			item.discard();
		} else {
			item.setItem(stack);
		}
		return Status.WORKING;
	}

	/** The nearest item within its agreement's area that its holder may take (in loaded chunks only). */
	private @Nullable ItemEntity nearestItem(ServerLevel level, Agreement agreement, WorkerDefinition.Spirit terms) {
		AABB area = new AABB(agreement.x() - agreement.radius(), agreement.y() - agreement.radius(), agreement.z() - agreement.radius(),
				agreement.x() + agreement.radius() + 1, agreement.y() + agreement.radius() + 1, agreement.z() + agreement.radius() + 1);
		ServerPlayer holder = Authority.answering(level, agreement.holder());
		return level.getEntitiesOfClass(ItemEntity.class, area, item -> item.isAlive() && !item.hasPickUpDelay()
						&& Authority.mayChange(level, holder, item.blockPosition()))
				.stream().min(Comparator.comparingDouble(this::distanceToSqr)).orElse(null);
	}

	/** Whether items lie in its area that its holder may not take (the reason it is idle, if so). */
	private boolean blocked(ServerLevel level, Agreement agreement) {
		AABB area = new AABB(agreement.x() - agreement.radius(), agreement.y() - agreement.radius(), agreement.z() - agreement.radius(),
				agreement.x() + agreement.radius() + 1, agreement.y() + agreement.radius() + 1, agreement.z() + agreement.radius() + 1);
		ServerPlayer holder = Authority.answering(level, agreement.holder());
		return !level.getEntitiesOfClass(ItemEntity.class, area, item -> item.isAlive() && !item.hasPickUpDelay()
				&& !Authority.mayChange(level, holder, item.blockPosition())).isEmpty();
	}

	@Override
	public @Nullable BlockPos seat() {
		return anchor;
	}

	@Override
	public void leave(ServerLevel level, AgreementHolder.@Nullable Holds home, BlockPos at) {
		depart(level, home instanceof SpiritAnchorBlockEntity found ? found : null, at);
	}

	/** Released, ended or replaced: it leaves what it carried at its anchor, once, is struck off its holder's roster and departs. */
	public void depart(ServerLevel level, @Nullable SpiritAnchorBlockEntity home, BlockPos at) {
		unload(level, home, at);
		if (owner != null) {
			WorkerRoster.of(level.getServer()).remove(owner, getUUID());
		}
		discard();
	}

	/**
	 * What it carries goes home, once: into {@code home}'s slots when that anchor keeps its own holder's agreement (what does
	 * not fit drops at the anchor; critique 12: never into an anchor sealed again by someone else); otherwise all of it
	 * drops at {@code elsewhere}. It drops whatever the block-drop rule says (the unit review's fix 3: what it carried is
	 * its holder's, not something mined). Nothing is left aboard to leave again.
	 */
	private void unload(ServerLevel level, @Nullable SpiritAnchorBlockEntity home, BlockPos elsewhere) {
		boolean ours = home != null && home.agreement() != null && owner != null && owner.equals(home.agreement().holder());
		BlockPos drop = ours ? home.getBlockPos() : elsewhere;
		for (ItemStack stack : carried) {
			ItemStack rest = ours ? home.deliver(stack) : stack;
			if (!rest.isEmpty()) {
				Containers.dropItemStack(level, drop.getX(), drop.getY(), drop.getZ(), rest);
			}
		}
		carried.clear();
	}

	/**
	 * Killed (or discarded without departing): what it carried goes home once, into its anchor's slots when that anchor is
	 * loaded and keeps its holder's agreement, otherwise where it fell (even switched off, D-S16); and it is struck off its
	 * holder's roster, which is how its anchor knows it is lost (D-01, D-S18). Unloading is not this: an unloaded shade keeps
	 * its load and its place on the roster.
	 */
	@Override
	public void remove(RemovalReason reason) {
		if (reason.shouldDestroy() && level() instanceof ServerLevel server) {
			SpiritAnchorBlockEntity home = anchor != null && server.isLoaded(anchor)
					&& server.getBlockEntity(anchor) instanceof SpiritAnchorBlockEntity found ? found : null;
			unload(server, home, blockPosition());
			if (owner != null) {
				WorkerRoster.of(server.getServer()).remove(owner, getUUID());
			}
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
			case SUSPENDED, UPKEEP_HELD, OUTSIDE_AGREEMENT, FINISHED, DESTINATION_UNLOADED, DISABLED -> SUSPENDED;
			case WAITING_FOR_RESOURCES, BLOCKED_BY_ACCESS, CANNOT_NAVIGATE, FULL, OWNER_OFFLINE -> WAITING;
			default -> IDLE;
		};
	}

	@Override
	protected void addAdditionalSaveData(ValueOutput output) {
		super.addAdditionalSaveData(output);
		if (anchor != null) {
			output.store("anchor", BlockPos.CODEC, anchor);
		}
		output.store("carried", ItemStack.CODEC.listOf(), List.copyOf(carried));
	}

	@Override
	protected void readAdditionalSaveData(ValueInput input) {
		super.readAdditionalSaveData(input);
		anchor = input.read("anchor", BlockPos.CODEC).orElse(null);
		carried.clear();
		input.read("carried", ItemStack.CODEC.listOf()).ifPresent(list -> list.stream().filter(stack -> !stack.isEmpty()).limit(64)
				.forEach(carried::add));
	}
}
