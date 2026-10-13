package io.github.jimbozoomer.jugcraft.concordance.reliquary;

import com.geckolib.animatable.GeoBlockEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.util.GeckoLibUtil;
import io.github.jimbozoomer.jugcraft.concordance.ConcordanceProgress;
import io.github.jimbozoomer.jugcraft.concordance.Saved;
import io.github.jimbozoomer.jugcraft.concordance.effect.Cause;
import io.github.jimbozoomer.jugcraft.concordance.relic.Context;
import io.github.jimbozoomer.jugcraft.concordance.relic.Mode;
import io.github.jimbozoomer.jugcraft.concordance.relic.RelicDefinition;
import io.github.jimbozoomer.jugcraft.concordance.relic.RelicState;
import io.github.jimbozoomer.jugcraft.concordance.relic.Relics;
import io.github.jimbozoomer.jugcraft.concordance.rules.Evidence;
import io.github.jimbozoomer.jugcraft.party.JugcraftParties;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.UUIDUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Prediction;
import net.minecraft.world.Containers;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * The Reliquary Shrine (roadmap step 20): it holds one installed relic and works it in the installed context, every
 * {@value Reliquary#CHECK_TICKS} ticks, for its owner's party (allies) or against hostile creatures around it. It charges
 * that relic from the Ley Pylons within {@value Reliquary#PYLON_REACH} blocks ({@value Reliquary#RECHARGE_PER_SECOND} a
 * second, whole Ley Charges only, so nothing is wasted) and says what it is doing or why it is not (for Jade and for an
 * empty hand). Only its owner and their party may install, recharge at it or take its relic back.
 */
public class ReliquaryShrineBlockEntity extends BlockEntity implements GeoBlockEntity {
	private static final RawAnimation EMPTY = RawAnimation.begin().thenLoop("animation.reliquary_shrine.empty");
	private static final RawAnimation HOLDING = RawAnimation.begin().thenLoop("animation.reliquary_shrine.holding");
	private static final RawAnimation WORKING = RawAnimation.begin().thenLoop("animation.reliquary_shrine.working");

	private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
	private ItemStack relic = ItemStack.EMPTY;
	private @Nullable UUID owner;
	/** What it last did: "working", "empty", "disabled", or why its relic does nothing (a reason key). */
	private String status = "empty";
	/** Whether it holds a relic: on the client, what the server sent (the relic itself is not sent). */
	private boolean holding;

	public ReliquaryShrineBlockEntity(BlockPos pos, BlockState state) {
		super(Reliquary.SHRINE_ENTITY, pos, state);
	}

	public ItemStack relic() {
		return relic;
	}

	public String status() {
		return status;
	}

	public @Nullable UUID owner() {
		return owner;
	}

	public void setOwner(@Nullable UUID owner) {
		this.owner = owner;
		setChanged();
	}

	/** Whether {@code player} may use it: no owner yet, its owner, or their party. */
	public boolean permits(ServerPlayer player) {
		return owner == null || owner.equals(player.getUUID()) || JugcraftParties.sameParty(owner, player.getUUID());
	}

	/** The holder its installed relics count against (its owner, or no one). */
	private UUID user() {
		return owner == null ? Relics.NO_OWNER : owner;
	}

	void serverTick(ServerLevel level) {
		if (Math.floorMod(level.getGameTime() + worldPosition.asLong(), Reliquary.CHECK_TICKS) == 0) {
			work(level);
		}
	}

	/** One check: charge from the pylons, then pulse if it may. Returns the status it leaves (tests call this). */
	public String work(ServerLevel level) {
		String next;
		if (!Reliquary.enabled()) {
			next = "disabled";
		} else if (relic.isEmpty()) {
			next = "empty";
		} else {
			RelicDefinition definition = Reliquary.definition(relic);
			next = definition == null ? "unknown" : pulse(level, definition);
		}
		if (!next.equals(status)) {
			status = next;
			setChanged();
			sync();
		}
		return status;
	}

	private String pulse(ServerLevel level, RelicDefinition definition) {
		long now = level.getGameTime();
		RelicState state = Reliquary.state(relic);
		// Charge first: whole Ley Charges only, never past full, at most RECHARGE_PER_SECOND a check.
		int ley = Math.min(Reliquary.RECHARGE_PER_SECOND, definition.capacity() - state.charge()) / Reliquary.CHARGE_PER_LEY;
		if (ley > 0) {
			long drawn = Reliquary.draw(level, Reliquary.pylons(level, worldPosition, user()), ley);
			if (drawn > 0) {
				state = state.charged((int) drawn * Reliquary.CHARGE_PER_LEY, definition.capacity());
				relic.set(Reliquary.RELIC, state);
				setChanged();
			}
		}
		Relics.Situation situation = new Relics.Situation(user(), level.dimension().identifier().toString(),
				Reliquary.openSky(level, worldPosition.above()), Reliquary.night(level), RelicState.FRESH.lastPulse(), false, now);
		Relics.Verdict verdict = Relics.check(definition, Context.INSTALLED, state, situation);
		if (verdict.resting()) {
			return "working";
		}
		if (!verdict.allowed()) {
			return verdict.reason();
		}
		Mode mode = verdict.mode();
		if (!Reliquary.INSTALLED.allows(user(), now)) {
			return "budget";
		}
		Vec3 center = Vec3.atCenterOf(worldPosition);
		List<LivingEntity> targets = Reliquary.targets(level, mode, center, user(), null);
		if (targets.isEmpty()) {
			return "no_target";
		}
		ServerPlayer actor = owner == null ? null : level.getServer().getPlayerList().getPlayer(owner);
		if (Reliquary.give(level, mode, targets, Cause.Origin.SHRINE, user(), actor, center) == 0) {
			return "kept";
		}
		Reliquary.INSTALLED.take(user(), now);
		RelicState next = state.spent(mode.cost(), now);
		if (definition.owned() && next.owner() == null && owner != null) {
			next = next.bound(owner);
		}
		relic.set(Reliquary.RELIC, next);
		setChanged();
		if (actor != null) {
			ConcordanceProgress.record(actor, new Evidence.Practiced(Reliquary.ACTIVITY, Context.INSTALLED.id));
		}
		return "working";
	}

	/** A relic used on it: installed if it can be and the shrine is empty; otherwise recharged from the pylons. */
	public void useWith(ServerPlayer player, ServerLevel level, ItemStack stack) {
		if (!Reliquary.enabled()) {
			player.sendOverlayMessage(Component.translatable("message.jugcraft.concordance.disabled"));
			return;
		}
		if (!Reliquary.knows(player)) {
			player.sendOverlayMessage(Component.translatable("message.jugcraft.concordance.relic.unknown"));
			return;
		}
		if (!permits(player)) {
			player.sendOverlayMessage(Component.translatable("message.jugcraft.concordance.relic.not_yours"));
			return;
		}
		RelicDefinition definition = Reliquary.definition(stack);
		if (definition == null) {
			player.sendOverlayMessage(Component.translatable("compose.jugcraft.relic.reason.unknown"));
			return;
		}
		if (owner == null) {
			setOwner(player.getUUID());
		}
		if (relic.isEmpty() && definition.installable()) {
			install(level, stack.split(1));
			player.sendSystemMessage(Component.translatable("message.jugcraft.concordance.relic.installed", relic.getHoverName()));
			return;
		}
		if (relic.isEmpty()) {
			// It cannot be installed: say so, with where it does work, and recharge it all the same.
			player.sendSystemMessage(Component.translatable("message.jugcraft.concordance.relic.status", stack.getHoverName(),
					Reliquary.context(Context.INSTALLED), Component.translatable("message.jugcraft.concordance.relic.cannot",
							Component.translatable("compose.jugcraft.relic.reason.cannot_install", Reliquary.contexts(definition.contexts())))));
		}
		Reliquary.recharge(player, level, worldPosition, stack, definition);
	}

	/** An empty hand: its owner's party takes the relic back; anyone else (or an empty shrine) is told what it is doing. */
	public void useEmpty(ServerPlayer player, ServerLevel level) {
		if (!relic.isEmpty() && permits(player)) {
			ItemStack taken = relic;
			install(level, ItemStack.EMPTY);
			player.getInventory().placeItemBackInInventory(taken, Prediction.SERVER_ONLY);
			player.sendSystemMessage(Component.translatable("message.jugcraft.concordance.relic.taken", taken.getHoverName()));
			return;
		}
		player.sendSystemMessage(describe());
	}

	/** What it holds and does, in a line: "Wardlight Lantern, installed: working (watch, charge 40)" or why not. */
	public Component describe() {
		if (relic.isEmpty()) {
			return Component.translatable("message.jugcraft.concordance.relic.shrine_empty");
		}
		RelicDefinition definition = Reliquary.definition(relic);
		Mode mode = definition == null ? null : definition.mode(Context.INSTALLED);
		return Reliquary.line(relic, new Reliquary.Outcome(definition, Context.INSTALLED, mode,
				status.equals("working") ? "" : status, 0));
	}

	/**
	 * Puts {@code stack} in (or, empty, takes the relic out), lights the shrine to match and checks it at once, so an
	 * installed relic starts working (or says why not) as it goes in.
	 */
	private void install(ServerLevel level, ItemStack stack) {
		relic = stack;
		holding = !stack.isEmpty();
		setChanged();
		BlockState state = getBlockState();
		if (state.getValue(ReliquaryShrineBlock.LIT) != holding) {
			level.setBlock(worldPosition, state.setValue(ReliquaryShrineBlock.LIT, holding), Block.UPDATE_ALL);
		}
		status = "";
		work(level);
	}

	/** Operators and tests: installs {@code stack} as it is (no checks). */
	public void setRelic(ServerLevel level, ItemStack stack) {
		install(level, stack);
	}

	/** Broken: its relic drops. */
	@Override
	public void preRemoveSideEffects(BlockPos pos, BlockState state) {
		if (level instanceof ServerLevel && !relic.isEmpty()) {
			Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), relic);
			relic = ItemStack.EMPTY;
		}
	}

	// ---------------------------------------------------------------- saving and clients

	private void sync() {
		if (level instanceof ServerLevel server) {
			server.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
		}
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		relic = input.read("relic", ItemStack.CODEC).orElse(ItemStack.EMPTY);
		owner = input.read("owner", UUIDUtil.CODEC).orElse(null);
		status = input.getStringOr("status", "empty");
		// Clients read the update tag here too: it carries "holding" and no relic.
		holding = input.getBooleanOr("holding", !relic.isEmpty());
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		Saved.stamp(output, 1);
		if (!relic.isEmpty()) {
			output.store("relic", ItemStack.CODEC, relic);
		}
		if (owner != null) {
			output.store("owner", UUIDUtil.CODEC, owner);
		}
		output.putString("status", status);
	}

	@Override
	public Packet<ClientGamePacketListener> getUpdatePacket() {
		return ClientboundBlockEntityDataPacket.create(this);
	}

	/** Clients get whether it holds a relic and its status (for its animation); never its owner. */
	@Override
	public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
		CompoundTag tag = new CompoundTag();
		tag.putBoolean("holding", !relic.isEmpty());
		tag.putString("status", status);
		return tag;
	}

	@Override
	public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
		controllers.add(new AnimationController<ReliquaryShrineBlockEntity>("main", 10, test -> {
			ReliquaryShrineBlockEntity shrine = test.animatable();
			return test.setAndContinue(!shrine.holding ? EMPTY : shrine.status.equals("working") ? WORKING : HOLDING);
		}));
	}

	@Override
	public AnimatableInstanceCache getAnimatableInstanceCache() {
		return cache;
	}
}
