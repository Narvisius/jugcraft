package io.github.jimbozoomer.jugcraft.concordance.spirits;

import io.github.jimbozoomer.jugcraft.concordance.RateGate;
import io.github.jimbozoomer.jugcraft.concordance.worker.Agreement;
import io.github.jimbozoomer.jugcraft.concordance.worker.WorkerDefinition;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * The Spirit Anchor's block (roadmap step 17). An empty hand: someone who understands Binding Arts seals an agreement
 * here (a Gathering Shade is called); its holder, sneaking, suspends or resumes it; anyone may read its terms and how
 * its spirit is doing. Breaking it releases the agreement ({@link SpiritAnchorBlockEntity#preRemoveSideEffects}).
 */
public class SpiritAnchorBlock extends BaseEntityBlock {
	public SpiritAnchorBlock(Properties properties) {
		super(properties);
	}

	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new SpiritAnchorBlockEntity(pos, state);
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

	/** One use of the anchor (tests call this directly). */
	public static void use(ServerPlayer player, ServerLevel level, SpiritAnchorBlockEntity anchor, boolean sneaking) {
		Agreement agreement = anchor.agreement();
		if (agreement == null) {
			WorkerDefinition.Spirit terms = Workers.catalog().get(GatheringShadeEntity.DEFINITION, WorkerDefinition.Spirit.class);
			if (!Workers.enabled() || terms == null) {
				player.sendOverlayMessage(Component.translatable("message.jugcraft.concordance.disabled"));
			} else if (!Workers.knows(player)) {
				player.sendOverlayMessage(Component.translatable("message.jugcraft.concordance.workers.unknown"));
			} else if (anchor.seal(level, player, terms, level.getGameTime()) != null) {
				player.sendSystemMessage(Component.translatable("message.jugcraft.concordance.workers.sealed", terms.radius(),
						Workers.clock(terms.from()), Workers.clock(terms.to()), terms.quota()));
			}
			return;
		}
		if (sneaking && agreement.holder().equals(player.getUUID())) {
			boolean suspended = anchor.toggleSuspended();
			player.sendOverlayMessage(Component.translatable(suspended ? "message.jugcraft.concordance.workers.suspended"
					: "message.jugcraft.concordance.workers.resumed"));
			return;
		}
		player.sendSystemMessage(Component.translatable("message.jugcraft.concordance.workers.agreement", agreement.radius(),
				Workers.clock(agreement.from()), Workers.clock(agreement.to()), agreement.done(level.getGameTime()), agreement.quota(),
				Component.translatable(agreement.suspended() ? "message.jugcraft.concordance.workers.is_suspended"
						: "message.jugcraft.concordance.workers.is_active")));
		Entity spirit = anchor.spirit() == null ? null : level.getEntity(anchor.spirit());
		if (spirit instanceof GatheringShadeEntity shade) {
			player.sendSystemMessage(shade.describe());
		} else {
			player.sendSystemMessage(Component.translatable("message.jugcraft.concordance.workers.spirit_away"));
		}
	}
}
