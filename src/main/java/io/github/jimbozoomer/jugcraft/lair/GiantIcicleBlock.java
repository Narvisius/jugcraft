package io.github.jimbozoomer.jugcraft.lair;

import java.util.Locale;
import net.minecraft.core.BlockPos;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A Giant Icicle hanging from the Glacier Hall's vault (docs/features/glacier-hall.md), hung in {@link Part parts} from
 * the vault down: its base, as many middles as it is long, and its tip, each narrower than the last.
 */
public class GiantIcicleBlock extends Block {
	/** The parts of an icicle, from the vault down (tools/lairs.py ICICLE_PARTS). */
	public enum Part implements StringRepresentable {
		BASE, MIDDLE, TIP;

		@Override
		public String getSerializedName() {
			return name().toLowerCase(Locale.ROOT);
		}
	}

	public static final EnumProperty<Part> PART = EnumProperty.create("part", Part.class);
	private static final VoxelShape BASE = Shapes.or(Block.box(1, 8, 1, 15, 16, 15), Block.box(2, 0, 2, 14, 8, 14));
	private static final VoxelShape MIDDLE = Shapes.or(Block.box(3, 8, 3, 13, 16, 13), Block.box(4, 0, 4, 12, 8, 12));
	private static final VoxelShape TIP = Shapes.or(Block.box(5, 9, 5, 11, 16, 11), Block.box(6, 3, 6, 10, 9, 10),
			Block.box(7, 0, 7, 9, 3, 9));

	public GiantIcicleBlock(Properties properties) {
		super(properties);
		registerDefaultState(stateDefinition.any().setValue(PART, Part.MIDDLE));
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return switch (state.getValue(PART)) {
			case BASE -> BASE;
			case MIDDLE -> MIDDLE;
			case TIP -> TIP;
		};
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(PART);
	}
}
