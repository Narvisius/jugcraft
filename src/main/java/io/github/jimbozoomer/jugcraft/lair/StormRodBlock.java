package io.github.jimbozoomer.jugcraft.lair;

import java.util.Locale;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A Storm Rod on the Crown of Thunder's summit (docs/features/crown-of-thunder.md): a rod of weathered copper three blocks
 * tall on a stone plinth, built in {@link Part parts} from the plinth up, its tip drawn to a point. A lair fixture that
 * cannot be broken or used. {@link #CHARGED} (the rod the storm is gathering on), it burns storm-blue, gives light
 * (tools/lairs.py ROD_LIGHT: more toward its tip) and crackles.
 */
public class StormRodBlock extends Block {
	/** The parts of a rod, from the plinth up (tools/lairs.py ROD_PARTS). */
	public enum Part implements StringRepresentable {
		BASE(6), MIDDLE(9), TIP(12);

		/** The light this part gives while charged. */
		public final int light;

		Part(int light) {
			this.light = light;
		}

		@Override
		public String getSerializedName() {
			return name().toLowerCase(Locale.ROOT);
		}
	}

	public static final EnumProperty<Part> PART = EnumProperty.create("part", Part.class);
	public static final BooleanProperty CHARGED = BooleanProperty.create("charged");
	private static final VoxelShape SHAFT = Block.box(5, 0, 5, 11, 16, 11);
	private static final VoxelShape TIP = Shapes.or(Block.box(5, 0, 5, 11, 10, 11), Block.box(6, 10, 6, 10, 14, 10),
			Block.box(7, 14, 7, 9, 16, 9));

	public StormRodBlock(Properties properties) {
		super(properties);
		registerDefaultState(stateDefinition.any().setValue(PART, Part.MIDDLE).setValue(CHARGED, false));
	}

	/** The light the rod gives in {@code state}. */
	public static int light(BlockState state) {
		return state.getValue(CHARGED) ? state.getValue(PART).light : 0;
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return state.getValue(PART) == Part.TIP ? TIP : SHAFT;
	}

	/** Charges or calms the rod part at {@code pos}; whether it was a rod and it changed. */
	public static boolean setCharged(Level level, BlockPos pos, boolean charged) {
		BlockState state = level.getBlockState(pos);
		if (!(state.getBlock() instanceof StormRodBlock) || state.getValue(CHARGED) == charged) {
			return false;
		}
		level.setBlock(pos, state.setValue(CHARGED, charged), Block.UPDATE_ALL);
		return true;
	}

	@Override
	public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
		if (state.getValue(CHARGED)) {
			int sparks = state.getValue(PART) == Part.TIP ? 3 : 1;
			for (int i = 0; i < sparks; i++) {
				level.addParticle(ParticleTypes.ELECTRIC_SPARK, pos.getX() + 0.3 + random.nextDouble() * 0.4, pos.getY()
						+ random.nextDouble(), pos.getZ() + 0.3 + random.nextDouble() * 0.4, 0.0, 0.0, 0.0);
			}
		}
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(PART, CHARGED);
	}
}
