package io.github.jimbozoomer.jugcraft.concordance;

import io.github.jimbozoomer.jugcraft.concordance.resource.Transfers;
import io.github.jimbozoomer.jugcraft.concordance.sign.Presentation;
import io.github.jimbozoomer.jugcraft.config.JugcraftConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

/**
 * The Lumen Sconce: a brass lamp-stand that burns Radiance for a steady light 15. Use it with a Kindled Lantern to pour
 * Radiance in; sneak to draw it back out (its owner only); use it empty-handed to read its charge. The Radiance, the
 * clock and the rules live in {@link LumenSconceBlockEntity}.
 */
public class LumenSconceBlock extends BaseEntityBlock {
	public static final BooleanProperty LIT = BooleanProperty.create("lit");
	private static final VoxelShape SHAPE = Block.box(4.0, 0.0, 4.0, 12.0, 13.0, 12.0);

	public LumenSconceBlock(Properties properties) {
		super(properties);
		registerDefaultState(stateDefinition.any().setValue(LIT, false));
	}

	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new LumenSconceBlockEntity(pos, state);
	}

	@Override
	public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
		super.setPlacedBy(level, pos, state, placer, stack);
		if (level.getBlockEntity(pos) instanceof LumenSconceBlockEntity sconce) {
			if (placer instanceof Player player) {
				sconce.setOwner(player.getUUID());
			}
			sconce.placed(level);
		}
	}

	@Override
	protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
		if (level.getBlockEntity(pos) instanceof LumenSconceBlockEntity sconce) {
			sconce.refresh(level, ConcordanceProgress.now(level));
		}
	}

	/** A Kindled Lantern used on the sconce pours into it. (Sneaking, the lantern's own use draws: {@link #exchange}.) */
	@Override
	protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
			InteractionHand hand, BlockHitResult hit) {
		if (!stack.is(JugcraftConcordance.KINDLED_LANTERN)) {
			return super.useItemOn(stack, state, level, pos, player, hand, hit);
		}
		return exchange(level, pos, player, stack, false);
	}

	/**
	 * Pours Radiance from {@code lantern} into the sconce at {@code pos}, or with {@code drawing} draws it back out, on
	 * the server, and tells the player what happened. Vanilla skips a block's own use when a player sneaks with an item
	 * in hand, so the sneaking draw arrives through {@link KindledLanternItem#useOn}.
	 */
	public static InteractionResult exchange(Level level, BlockPos pos, Player player, ItemStack lantern, boolean drawing) {
		if (!(player instanceof ServerPlayer server) || !(level instanceof ServerLevel serverLevel)
				|| !(level.getBlockEntity(pos) instanceof LumenSconceBlockEntity sconce)) {
			return InteractionResult.SUCCESS;
		}
		if (!JugcraftConfig.isFeatureEnabled(JugcraftConcordance.FEATURE)) {
			server.sendOverlayMessage(Component.translatable("message.jugcraft.concordance.disabled"));
			return InteractionResult.FAIL;
		}
		if (!RateGate.allow(server, "sconce", 10)) {
			return InteractionResult.FAIL;
		}
		Transfers.Transfer transfer = drawing ? sconce.draw(serverLevel, server.getUUID(), lantern)
				: sconce.pour(serverLevel, server.getUUID(), lantern);
		server.sendOverlayMessage(message(transfer, drawing, sconce.remaining(ConcordanceProgress.now(serverLevel))));
		if (transfer.outcome().moved()) {
			level.playSound(null, pos, drawing ? JugcraftConcordance.LANTERN_IGNITE_SOUND : JugcraftConcordance.KINDLE_SOUND,
					SoundSource.BLOCKS, 0.6F, drawing ? 0.9F : 1.2F);
		}
		return InteractionResult.SUCCESS;
	}

	private static Component message(Transfers.Transfer transfer, boolean drawing, long held) {
		String key = switch (transfer.outcome()) {
			case MOVED, PARTIAL -> drawing ? "drawn" : "poured";
			case FULL -> drawing ? "lantern_full" : "full";
			case NOTHING_TO_MOVE -> drawing ? "sconce_empty" : "lantern_empty";
			case NOT_PERMITTED -> "not_owner";
			case RATE_LIMITED -> "settling";
			case WRONG_TYPE, NOT_PORTABLE -> "wrong_type";
		};
		return Component.translatable("message.jugcraft.concordance.sconce." + key, transfer.received(), held,
				LumenSconceBlockEntity.CAPACITY);
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		if (!level.isClientSide() && level.getBlockEntity(pos) instanceof LumenSconceBlockEntity sconce) {
			player.sendOverlayMessage(Component.translatable("message.jugcraft.concordance.sconce.status",
					sconce.remaining(ConcordanceProgress.now(level)), LumenSconceBlockEntity.CAPACITY));
		}
		return InteractionResult.SUCCESS;
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return SHAPE;
	}

	@Override
	protected boolean isPathfindable(BlockState state, PathComputationType type) {
		return false;
	}

	/** A slow glimmer over the lens while it burns (client only; fewer with reduced motion). */
	@Override
	public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
		if (!state.getValue(LIT) || !Presentation.ambient(random, 6, 24)) {
			return;
		}
		level.addParticle(ParticleTypes.END_ROD, pos.getX() + 0.5, pos.getY() + 0.9, pos.getZ() + 0.5, 0.0, 0.01, 0.0);
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(LIT);
	}
}
