package io.github.jimbozoomer.jugcraft.nuclear;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * An invisible part of a piece of nuclear equipment. It belongs to the anchor that placed it: using it is using the
 * equipment, and breaking it breaks the whole thing (its anchor drops the item once). Most are a whole block; a few
 * are half a block high, so a piece's steps can be walked up (EquipmentKind.frameShape).
 */
public class EquipmentFrameBlock extends Block {
	public enum Shape implements StringRepresentable {
		FULL("full"), HALF("half");

		private final String name;

		Shape(String name) {
			this.name = name;
		}

		@Override
		public String getSerializedName() {
			return name;
		}
	}

	public static final EnumProperty<Shape> SHAPE = EnumProperty.create("shape", Shape.class);
	private static final VoxelShape HALF = Block.box(0, 0, 0, 16, 8, 16);

	public EquipmentFrameBlock(Properties properties) {
		super(properties);
		registerDefaultState(stateDefinition.any().setValue(SHAPE, Shape.FULL));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(SHAPE);
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return state.getValue(SHAPE) == Shape.HALF ? HALF : Shapes.block();
	}

	@Override
	protected RenderShape getRenderShape(BlockState state) {
		return RenderShape.INVISIBLE;
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		BlockPos anchor = EquipmentRegistry.of(level).anchorOf(pos);
		return anchor == null ? InteractionResult.PASS : EquipmentPlacement.use(level, anchor, player, ItemStack.EMPTY, InteractionHand.MAIN_HAND);
	}

	@Override
	protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand,
			BlockHitResult hit) {
		BlockPos anchor = EquipmentRegistry.of(level).anchorOf(pos);
		return anchor == null ? InteractionResult.PASS : EquipmentPlacement.use(level, anchor, player, stack, hand);
	}

	@Override
	public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
		BlockPos anchor = EquipmentRegistry.of(level).anchorOf(pos);
		if (!level.isClientSide() && anchor != null && !anchor.equals(pos)) {
			level.destroyBlock(anchor, !player.preventsBlockDrops(), player);
		}
		return super.playerWillDestroy(level, pos, state, player);
	}

	@Override
	protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel level, BlockPos pos, boolean movedByPiston) {
		super.affectNeighborsAfterRemoval(state, level, pos, movedByPiston);
		if (EquipmentPlacement.CLEARING.get()) {
			return;
		}
		BlockPos anchor = EquipmentRegistry.of(level).anchorOf(pos);
		if (anchor != null && level.getBlockState(anchor).getBlock() instanceof EquipmentAnchorBlock) {
			level.destroyBlock(anchor, true);
		}
	}
}
