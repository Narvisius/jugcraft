package io.github.jimbozoomer.jugcraft.exterior;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.Nullable;

/**
 * An insulated metal wall panel, one block (1 m) high (tools/exterior_kit.py wall_panel): FACING is where its outer
 * face looks; TONE its skin (silver, anthracite, or the anthracite trapezoidal sheet); SHAPE plain, a joint cover at a
 * column line, the column too (a steel H-column on its liner inside), a downpipe in front (on a column line), an LED
 * wall pack, or a corner. A wall pack is LIT at night: it looks at the sky every few seconds, as a photocell does.
 * Sneaking and using it with an empty hand changes its shape.
 */
public class WallPanelBlock extends HorizontalDirectionalBlock {
	public static final EnumProperty<Tone> TONE = EnumProperty.create("tone", Tone.class);
	public static final EnumProperty<Shape> SHAPE = EnumProperty.create("shape", Shape.class);
	public static final BooleanProperty LIT = BlockStateProperties.LIT;
	/** How often (ticks) a wall pack checks the daylight. */
	static final int PHOTOCELL = 100;

	public enum Tone implements StringRepresentable {
		SILVER("silver"), ANTHRACITE("anthracite"), TRAPEZOID("trapezoid");

		private final String name;

		Tone(String name) {
			this.name = name;
		}

		@Override
		public String getSerializedName() {
			return name;
		}
	}

	public enum Shape implements StringRepresentable {
		PLAIN("plain"), JOINT("joint"), COLUMN("column"), PIPE("pipe"), LIGHT("light"), CORNER("corner");

		private final String name;

		Shape(String name) {
			this.name = name;
		}

		@Override
		public String getSerializedName() {
			return name;
		}
	}

	public WallPanelBlock(Properties properties) {
		super(properties);
		registerDefaultState(stateDefinition.any().setValue(FACING, Direction.SOUTH).setValue(TONE, Tone.SILVER).setValue(SHAPE, Shape.PLAIN)
				.setValue(LIT, false));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(FACING, TONE, SHAPE, LIT);
	}

	/** The outer face toward whoever places it (they stand outside the wall). */
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
			level.setBlock(pos, state.setValue(SHAPE, next).setValue(LIT, next == Shape.LIGHT && !level.isBrightOutside()), Block.UPDATE_ALL);
			ExteriorShapes.shapeMessage(player, next.getSerializedName());
			if (next == Shape.LIGHT) {
				level.scheduleTick(pos, this, PHOTOCELL);
			}
		}
		return InteractionResult.SUCCESS;
	}

	@Override
	protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
		if (state.getValue(SHAPE) == Shape.LIGHT && !level.isClientSide()) {
			level.scheduleTick(pos, this, 1 + level.getRandom().nextInt(PHOTOCELL));
		}
	}

	/** The wall pack's photocell: on when it is dark outside, checked every few seconds. */
	@Override
	protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
		if (state.getValue(SHAPE) != Shape.LIGHT) {
			return;
		}
		boolean dark = !level.isBrightOutside();
		if (state.getValue(LIT) != dark) {
			level.setBlock(pos, state.setValue(LIT, dark), Block.UPDATE_ALL);
		}
		level.scheduleTick(pos, this, PHOTOCELL);
	}

	@Override
	protected int getLightDampening(BlockState state) {
		return 15;
	}
}
