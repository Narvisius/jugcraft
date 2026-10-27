package io.github.jimbozoomer.jugcraft.concordance.spirits;

import io.github.jimbozoomer.jugcraft.concordance.Authority;
import io.github.jimbozoomer.jugcraft.concordance.ComposeText;
import io.github.jimbozoomer.jugcraft.concordance.RateGate;
import io.github.jimbozoomer.jugcraft.concordance.courier.CourierPostBlockEntity;
import io.github.jimbozoomer.jugcraft.concordance.diagnostic.Code;
import io.github.jimbozoomer.jugcraft.concordance.diagnostic.Diagnostic;
import io.github.jimbozoomer.jugcraft.concordance.worker.Agreement;
import io.github.jimbozoomer.jugcraft.concordance.worker.Pacts;
import io.github.jimbozoomer.jugcraft.concordance.worker.Status;
import io.github.jimbozoomer.jugcraft.concordance.worker.WorkerDefinition;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.Nullable;

/**
 * The Spirit Anchor's block (roadmap step 17). An empty hand: someone who understands Binding Arts and may seal here (its
 * owner or their party, where they may build; an anchor placed before owners were recorded, whoever may build there) seals
 * an agreement, or is told the one reason not; its holder, sneaking, suspends or resumes it, and calls another shade when
 * theirs is lost; anyone reads its terms and how its spirit is. One whose Bound Will does not keep it (a copy) says another
 * agreement covers it; one whose Bound Will this version cannot read says so, and the agreement waits (Concordance Chunk
 * D, D-02). Breaking it releases the agreement ({@link SpiritAnchorBlockEntity#preRemoveSideEffects}). Since Concordance
 * Chunk D (D-08) a Porter Key binds an anchor that keeps no agreement to its holder's Courier Post (sneaking: unbinds it);
 * one so bound seals a Workshop Hand, only for one who has mastered Stolas's Regard.
 */
public class SpiritAnchorBlock extends BaseEntityBlock {
	public SpiritAnchorBlock(Properties properties) {
		super(properties);
	}

	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new SpiritAnchorBlockEntity(pos, state);
	}

	/** Placed: the anchor records who placed it (Concordance Chunk D, D-01, D-S8); a stand-in placing it records nobody. */
	@Override
	public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
		super.setPlacedBy(level, pos, state, placer, stack);
		if (level instanceof ServerLevel && placer instanceof ServerPlayer player && !Authority.isStandIn(player)
				&& level.getBlockEntity(pos) instanceof SpiritAnchorBlockEntity anchor) {
			anchor.holder().setOwner(player.getUUID());
		}
	}

	/** D-08: a Porter Key is the key's to use on the anchor (it binds or unbinds the anchor's post), as on the post (CourierPostBlock:46-48). */
	@Override
	protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand,
			BlockHitResult hit) {
		return stack.is(Workers.PORTER_KEY) ? InteractionResult.PASS : super.useItemOn(stack, state, level, pos, player, hand, hit);
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		if (!(player instanceof ServerPlayer server) || !(level instanceof ServerLevel serverLevel)
				|| !(level.getBlockEntity(pos) instanceof SpiritAnchorBlockEntity anchor)) {
			return InteractionResult.SUCCESS;
		}
		if (!RateGate.allow(server, "workers", 4)) {
			return InteractionResult.FAIL;
		}
		use(server, serverLevel, anchor, player.isShiftKeyDown());
		return InteractionResult.SUCCESS;
	}

	/**
	 * One use of the anchor (tests call this directly; the block asks RateGate first). Returns the last line it told the
	 * player. A hand's anchor (bound to a post with no agreement, or keeping a courier's) asks only the Workshop Hand's
	 * definition, a shade's only the Gathering Shade's, so a data pack that drops one leaves the other's anchors working.
	 */
	public static Component use(ServerPlayer player, ServerLevel level, SpiritAnchorBlockEntity anchor, boolean sneaking) {
		if (!Workers.enabled()) {
			return overlay(player, Component.translatable("message.jugcraft.concordance.disabled"));
		}
		long now = level.getGameTime();
		AgreementHolder holder = anchor.holder();
		AgreementHolder.Standing standing = holder.verify(level, now);
		Agreement agreement = holder.agreement();
		boolean hand = agreement == null ? anchor.post() != null : WorkshopHandEntity.WORK.equals(agreement.work());
		WorkerDefinition.Spirit terms = hand ? WorkshopHandEntity.terms()
				: Workers.catalog().get(GatheringShadeEntity.DEFINITION, WorkerDefinition.Spirit.class);
		if (terms == null) {
			return overlay(player, Component.translatable("message.jugcraft.concordance.disabled"));
		}
		if (agreement == null) {
			if (!Workers.knows(player)) {
				return overlay(player, Component.translatable("message.jugcraft.concordance.workers.unknown"));
			}
			if (hand) {
				// Concordance Chunk D, D-08: an anchor bound to a Courier Post seals a Workshop Hand.
				return sealHand(player, level, anchor, terms, now);
			}
			AgreementHolder.Sealed sealed = anchor.seal(level, player, terms, now);
			return say(player, sealed.outcome() == Pacts.Outcome.DONE ? Component.translatable("message.jugcraft.concordance.workers.sealed",
					terms.radius(), Workers.clock(terms.from()), Workers.clock(terms.to()), terms.quota()) : AgreementHolder.told(sealed.outcome()));
		}
		if (standing == AgreementHolder.Standing.UNREADABLE) {
			return overlay(player, Component.translatable("message.jugcraft.concordance.workers.unreadable_record"));
		}
		if (standing != AgreementHolder.Standing.VALID) {
			return overlay(player, ComposeText.show(Diagnostic.of(Code.CONFLICTING_AGREEMENT).text()));
		}
		boolean holds = !Authority.isStandIn(player) && agreement.holder().equals(player.getUUID());
		if (sneaking && holds) {
			return overlay(player, AgreementHolder.told(holder.toggle(level, player, now)));
		}
		if (holds && holder.spiritLost(level)) {
			return say(player, AgreementHolder.told(holder.recall(level, player, now)));
		}
		Status state = holder.state(level);
		player.sendSystemMessage(Component.translatable("message.jugcraft.concordance.workers.agreement", agreement.radius(),
				Workers.clock(agreement.from()), Workers.clock(agreement.to()), agreement.done(now), agreement.quota(),
				Component.translatable(state == null ? "message.jugcraft.concordance.workers.is_active" : "compose.jugcraft.worker.status." + state.id)));
		GlobalPos post = anchor.post();
		if (post != null) {
			player.sendSystemMessage(handPost(level, post));
		}
		Entity spirit = anchor.spirit() == null ? null : level.getEntity(anchor.spirit());
		return say(player, spirit instanceof WorkerEntity<?> worker && !worker.isRemoved() ? worker.describe()
				: Component.translatable(holder.spiritLost(level) ? "message.jugcraft.concordance.workers.spirit_lost"
						: "message.jugcraft.concordance.workers.spirit_away"));
	}

	/**
	 * D-08: a Workshop Hand is sealed only for one who has mastered Stolas's Regard (D-S12; research unlocks name no worker),
	 * who may seal here (else the seal's own reason), and while the anchor's post is still one they may use, in this level,
	 * loaded and inside a hand's area; any refusal seals, records, spends and calls nothing.
	 */
	private static Component sealHand(ServerPlayer player, ServerLevel level, SpiritAnchorBlockEntity anchor, WorkerDefinition.Spirit hand, long now) {
		if (!WorkshopHandEntity.regarded(player)) {
			return overlay(player, Component.translatable("message.jugcraft.concordance.workers.hand_unknown"));
		}
		if (!anchor.holder().maySeal(level, player, true)) {
			return say(player, AgreementHolder.told(Pacts.Outcome.NOT_PERMITTED));
		}
		GlobalPos post = anchor.post();
		if (!WorkshopHandEntity.mayServe(level, anchor.getBlockPos(), post, player.getUUID(), hand.radius())) {
			return overlay(player, Component.translatable("message.jugcraft.concordance.workers.post_unusable", hand.radius()));
		}
		AgreementHolder.Sealed sealed = anchor.holder().seal(level, player, anchor.handSealing(hand), now);
		if (sealed.outcome() != Pacts.Outcome.DONE) {
			return say(player, AgreementHolder.told(sealed.outcome()));
		}
		BlockPos at = post.pos();
		return say(player, Component.translatable("message.jugcraft.concordance.workers.hand_sealed", at.getX(), at.getY(), at.getZ(), hand.radius(),
				Workers.clock(hand.from()), Workers.clock(hand.to()), hand.quota(), hand.carry()));
	}

	/** D-08: the Courier Post an anchor's Workshop Hand serves, for its holder and their party; or that the post is gone. */
	private static Component handPost(ServerLevel level, GlobalPos post) {
		BlockPos at = post.pos();
		boolean gone = level.dimension().equals(post.dimension()) && level.isLoaded(at) && !(level.getBlockEntity(at) instanceof CourierPostBlockEntity);
		return gone ? Component.translatable("message.jugcraft.concordance.workers.hand_no_post")
				: Component.translatable("message.jugcraft.concordance.workers.hand_post", at.getX(), at.getY(), at.getZ());
	}

	private static Component say(ServerPlayer player, Component line) {
		player.sendSystemMessage(line);
		return line;
	}

	private static Component overlay(ServerPlayer player, Component line) {
		player.sendOverlayMessage(line);
		return line;
	}
}
