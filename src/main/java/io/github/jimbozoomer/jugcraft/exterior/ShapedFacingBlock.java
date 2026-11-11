package io.github.jimbozoomer.jugcraft.exterior;

import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

/**
 * The exterior kit's thinner pieces, turned by FACING (their outer side to +z): the Door Canopy (in the cell outside a
 * wall over a door; PART single, or left, middle and right along a wider one) and the Steel Stair Landing (checker
 * plate on its channels; EDGE: the trimmer and the yellow nosing along its facing side, where the flights meet it).
 */
public class ShapedFacingBlock extends HorizontalDirectionalBlock {
	public static final EnumProperty<Part> PART = EnumProperty.create("part", Part.class);
	public static final BooleanProperty EDGE = BooleanProperty.create("edge");

	public enum Kind {
		CANOPY, LANDING
	}

	public enum Part implements StringRepresentable {
		SINGLE("single"), LEFT("left"), MID("mid"), RIGHT("right");

		private final String name;

		Part(String name) {
			this.name = name;
		}

		@Override
		public String getSerializedName() {
			return name;
		}
	}

	private static Kind building;
	private final Kind kind;
	private final Map<Direction, VoxelShape> shapes;

	private static Properties prepare(Properties properties, Kind kind) {
		building = kind;
		return properties;
	}

	public ShapedFacingBlock(Kind kind, Properties properties) {
		super(prepare(properties, kind));
		this.kind = kind;
		this.shapes = ExteriorShapes.byFacing(f -> kind == Kind.CANOPY ? ExteriorShapes.outer(f, new double[] {0, 1.2, 0, 16, 3.4, 14})
				: ExteriorShapes.outer(f, new double[] {0, 11, 0, 16, 16, 16}));
		BlockState state = stateDefinition.any().setValue(FACING, Direction.SOUTH);
		registerDefaultState(kind == Kind.CANOPY ? state.setValue(PART, Part.SINGLE) : state.setValue(EDGE, false));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(FACING);
		builder.add(building == Kind.CANOPY ? PART : EDGE);
	}

	/** A canopy faces out from the wall it is put against; a landing's nosing toward the placer. */
	@Override
	public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
		Direction look = context.getHorizontalDirection();
		return defaultBlockState().setValue(FACING, look.getOpposite());
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return shapes.get(state.getValue(FACING));
	}

	public Kind kind() {
		return kind;
	}
}
