package io.github.jimbozoomer.jugcraft.powerline;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * An invisible part of a power line structure: the pole, a leg, the fence or the equipment of a box. It belongs to
 * the anchor that placed it; breaking it breaks the whole structure (dropping its item once), and using it is
 * using the structure.
 */
public class LineFrameBlock extends Block {
	private static final VoxelShape POLE = Block.box(5, 0, 5, 11, 16, 11);
	private static final VoxelShape LEG = Block.box(4, 0, 4, 12, 16, 12);
	private static final VoxelShape FENCE = Block.box(0, 0, 0, 16, 16, 16);

	public LineFrameBlock(Properties properties) {
		super(properties);
	}

	static VoxelShape shapeFor(LineKind kind, BlockPos offset) {
		return switch (kind.family) {
			case WOOD, MONOPOLE, SUPERCONDUCTING -> POLE;
			// A tower's anchor is the empty ground between its legs: nothing to see or hit there.
			case LATTICE, UHV, UHVDC -> offset.equals(BlockPos.ZERO) ? Shapes.empty() : LEG;
			case BOX -> kind == LineKind.SERVICE_PEDESTAL ? Block.box(3, 0, 3, 13, 16, 13) : FENCE;
		};
	}

	@Override
	protected RenderShape getRenderShape(BlockState state) {
		return RenderShape.INVISIBLE;
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		if (level instanceof Level l) {
			BlockPos anchor = LineRegistry.of(l).anchorOf(pos);
			LineKind kind = anchor == null ? null : LineRegistry.of(l).kindAt(anchor);
			if (kind != null) {
				return shapeFor(kind, pos.subtract(anchor));
			}
		}
		return POLE;
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		BlockPos anchor = LineRegistry.of(level).anchorOf(pos);
		return anchor == null ? InteractionResult.PASS : LinePlacement.useStructure(level, anchor, player);
	}

	@Override
	protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand,
			BlockHitResult hit) {
		return InteractionResult.PASS;
	}

	@Override
	public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
		BlockPos anchor = LineRegistry.of(level).anchorOf(pos);
		if (!level.isClientSide() && anchor != null && !anchor.equals(pos)) {
			// Breaking any part breaks the structure; the anchor drops the item (none in creative).
			level.destroyBlock(anchor, !player.preventsBlockDrops(), player);
		}
		return super.playerWillDestroy(level, pos, state, player);
	}

	@Override
	protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel level, BlockPos pos, boolean movedByPiston) {
		super.affectNeighborsAfterRemoval(state, level, pos, movedByPiston);
		if (LinePlacement.CLEARING.get()) {
			return;
		}
		// Removed some other way (an explosion, a command): the structure goes too, dropping its item.
		BlockPos anchor = LineRegistry.of(level).anchorOf(pos);
		if (anchor != null && level.getBlockState(anchor).getBlock() instanceof LineAnchorBlock) {
			level.destroyBlock(anchor, true);
		}
	}
}
