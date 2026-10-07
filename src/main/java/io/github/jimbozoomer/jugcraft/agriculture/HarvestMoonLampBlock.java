package io.github.jimbozoomer.jugcraft.agriculture;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

/**
 * The Harvest Moon Lamp (Halloween decorations batch 19): a great glowing moon two blocks across on a gilded stand, facing
 * whoever placed it, lit at light {@value #LIGHT} (use it to switch it). Its face shows tonight's moon phase, from full to
 * new, and a comparator reads it: 15 at full moon down to 0 at new ({@link #brightness}), which matters on werewolf
 * nights. The face is drawn by the client (client/HarvestMoonLampRenderer.java).
 */
public class HarvestMoonLampBlock extends MultiDecorationBlock implements EntityBlock {
	public static final int LIGHT = 15;
	private static final int[][] CELLS = {{0, 0}, {1, 0}, {0, 1}, {1, 1}};
	public static final IntegerProperty PART = IntegerProperty.create("part", 0, CELLS.length - 1);
	private static final java.util.Map<Direction, VoxelShape> SHAPES = new java.util.EnumMap<>(Direction.class);

	static {
		for (Direction facing : Direction.Plane.HORIZONTAL) {
			SHAPES.put(facing, LongDecorationBlock.turned(new double[] {0.0, 0.0, 5.0, 16.0, 16.0, 11.0}, facing));
		}
	}

	public HarvestMoonLampBlock(Properties properties) {
		super(properties);
	}

	@Override
	public int[][] cells() {
		return CELLS;
	}

	@Override
	public IntegerProperty partProperty() {
		return PART;
	}

	public static int light(BlockState state) {
		return state.getValue(LIT) ? LIGHT : 0;
	}

	/** How much of the moon is lit at {@code phase} (0 full to 4 new and back), as a signal: 15, 11, 8, 4, 0, 4, 8, 11. */
	public static int brightness(int phase) {
		return Math.round(15.0F * Math.abs(4 - Math.floorMod(phase, 8)) / 4.0F);
	}

	/** Tonight's phase on the overworld clock. */
	public static int phase(Level level) {
		return GrandfatherClockBlock.moonPhase(level.getOverworldClockTime());
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return SHAPES.get(state.getValue(FACING));
	}

	@Override
	protected boolean hasAnalogOutputSignal(BlockState state) {
		return true;
	}

	@Override
	protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos, Direction direction) {
		return brightness(phase(level));
	}

	@Override
	public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return part(state) == MASTER ? new DecorationBlockEntity(JugcraftAgriculture.MOON_LAMP_ENTITY, pos, state) : null;
	}
}
