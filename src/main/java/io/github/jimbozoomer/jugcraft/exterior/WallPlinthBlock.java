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
 * A precast concrete plinth panel's block at the foot of a wall (tools/exterior_kit.py wall_plinth): ROW lower (on
 * the ground) or upper (under the cladding, with the drip flashing); SHAPE plain, a sealant joint at a column line, a
 * downpipe (its shoe over a gully on the lower row), the sprinkler inlet (lower row), or a corner. Placed on another
 * it is the upper row. Sneaking and using it with an empty hand changes its shape.
 */
public class WallPlinthBlock extends HorizontalDirectionalBlock {
	public static final EnumProperty<Row> ROW = EnumProperty.create("row", Row.class);
	public static final EnumProperty<Shape> SHAPE = EnumProperty.create("shape", Shape.class);

	public enum Row implements StringRepresentable {
		LOWER("lower"), UPPER("upper");

		private final String name;

		Row(String name) {
			this.name = name;
		}

		@Override
		public String getSerializedName() {
			return name;
		}
	}

	public enum Shape implements StringRepresentable {
		PLAIN("plain"), JOINT("joint"), PIPE("pipe"), INLET("inlet"), CORNER("corner");

		private final String name;

		Shape(String name) {
			this.name = name;
		}

		@Override
		public String getSerializedName() {
			return name;
		}
	}

	public WallPlinthBlock(Properties properties) {
		super(properties);
		registerDefaultState(stateDefinition.any().setValue(FACING, Direction.SOUTH).setValue(ROW, Row.UPPER).setValue(SHAPE, Shape.PLAIN));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(FACING, ROW, SHAPE);
	}

	@Override
	public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
		boolean onPlinth = context.getLevel().getBlockState(context.getClickedPos().below()).getBlock() instanceof WallPlinthBlock;
		return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite()).setValue(ROW, onPlinth ? Row.UPPER : Row.LOWER);
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
