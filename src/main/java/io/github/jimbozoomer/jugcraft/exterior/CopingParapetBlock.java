package io.github.jimbozoomer.jugcraft.exterior;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.Nullable;

/**
 * The parapet round a flat roof (tools/exterior_kit.py parapet, block id {@code parapet}): the anthracite panel outside,
 * the membrane turned up its inside, the aluminium coping over it. SHAPE plain, a scupper (the roof's outlet through it
 * and the hopper head of a downpipe outside), or a corner. Sneaking and using it with an empty hand changes its shape.
 */
public class CopingParapetBlock extends HorizontalDirectionalBlock {
	public static final EnumProperty<Shape> SHAPE = EnumProperty.create("shape", Shape.class);

	public enum Shape implements StringRepresentable {
		PLAIN("plain"), SCUPPER("scupper"), CORNER("corner");

		private final String name;

		Shape(String name) {
			this.name = name;
		}

		@Override
		public String getSerializedName() {
			return name;
		}
	}

	public CopingParapetBlock(Properties properties) {
		super(properties);
		registerDefaultState(stateDefinition.any().setValue(FACING, Direction.SOUTH).setValue(SHAPE, Shape.PLAIN));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(FACING, SHAPE);
	}

	@Override
	public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
		return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		if (!player.isSecondaryUseActive()) {
			return InteractionResult.PASS;
		}
		if (!level.isClientSide()) {
			Shape next = Shape.values()[(state.getValue(SHAPE).ordinal() + 1) % Shape.values().length];
			level.setBlock(pos, state.setValue(SHAPE, next), Block.UPDATE_ALL);
			ExteriorShapes.shapeMessage(player, next.getSerializedName());
		}
		return InteractionResult.SUCCESS;
	}

	@Override
	protected int getLightDampening(BlockState state) {
		return 15;
	}
}
