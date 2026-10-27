package io.github.jimbozoomer.jugcraft.concordance.spirits;

import io.github.jimbozoomer.jugcraft.concordance.Authority;
import io.github.jimbozoomer.jugcraft.concordance.RateGate;
import io.github.jimbozoomer.jugcraft.styx.StyxConservatory;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

/**
 * Concordance Chunk D, D-04 (D-S6, D-S8, D-S15): the pact perch's block, a place marker for a pact (its
 * {@link PactPerchBlockEntity} holds no item). It is placed only by a player in person, where they may build (the world,
 * spawn protection, towns, claims: Authority.mayChange), never in Styx's home ({@link StyxConservatory#covers}); placed, it
 * records its owner. Used, it opens its screen ({@link PactMenu}). Broken or blown up, its pact is left without its perch,
 * never ended. Its ticker looks at its pact once a second (PactKeeping.tick).
 */
public class PactPerchBlock extends BaseEntityBlock {
	private static final VoxelShape SHAPE = Shapes.or(Block.box(3.0, 0.0, 3.0, 13.0, 2.0, 13.0), Block.box(7.0, 2.0, 7.0, 9.0, 14.0, 9.0),
			Block.box(2.0, 14.0, 7.0, 14.0, 16.0, 9.0));

	public PactPerchBlock(Properties properties) {
		super(properties);
	}

	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new PactPerchBlockEntity(pos, state);
	}

	@Override
	public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
		return level instanceof ServerLevel ? createTickerHelper(type, Workers.PERCH_ENTITY, PactPerchBlockEntity::serverTick) : null;
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return SHAPE;
	}

	@Override
	public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
		if (context.getLevel() instanceof ServerLevel level && !mayPlace(level, context.getPlayer(), context.getClickedPos())) {
			if (context.getPlayer() instanceof ServerPlayer player) {
				player.sendOverlayMessage(Component.translatable("message.jugcraft.concordance.pact.perch_refused"));
			}
			return null;
		}
		return defaultBlockState();
	}

	/** D-S15: only a player in person (never a stand-in or a machine), where they could break a block themselves, and never in Styx's home. */
	public static boolean mayPlace(ServerLevel level, @Nullable Player placer, BlockPos at) {
		return placer instanceof ServerPlayer player && !Authority.isStandIn(player) && Authority.mayChange(level, player, at)
				&& !StyxConservatory.covers(level, at);
	}

	/** Placed: the perch records who placed it (D-S8); a stand-in records nobody. */
	@Override
	public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
		super.setPlacedBy(level, pos, state, placer, stack);
		if (level instanceof ServerLevel && placer instanceof ServerPlayer player && !Authority.isStandIn(player)
				&& level.getBlockEntity(pos) instanceof PactPerchBlockEntity perch) {
			perch.holder().setOwner(player.getUUID());
		}
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		if (!(player instanceof ServerPlayer server) || !(level instanceof ServerLevel serverLevel)
				|| !(level.getBlockEntity(pos) instanceof PactPerchBlockEntity perch)) {
			return InteractionResult.SUCCESS;
		}
		if (Authority.isStandIn(server) || !RateGate.allow(server, "pact_open", PactMenu.OPEN_TICKS)) {
			return InteractionResult.FAIL;
		}
		PactMenu.open(server, serverLevel, perch);
		return InteractionResult.SUCCESS;
	}
}
