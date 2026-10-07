package io.github.jimbozoomer.jugcraft.agriculture;

import java.util.EnumMap;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * The Web Drape (Halloween decorations batch 19, the Spider's Larder): a great sagging web curtain two blocks wide and two
 * tall, glinting with dew, hung across a doorway or a corner. It is one prop of four blocks, placed facing the player
 * from the block aimed at to their right and up. Nothing collides with it, but walking through it slows you by
 * {@link #SLOW}, less than a cobweb does.
 */
public class WebDrapeBlock extends MultiDecorationBlock {
	private static final int[][] CELLS = {{0, 0}, {1, 0}, {0, 1}, {1, 1}};
	public static final IntegerProperty PART = IntegerProperty.create("part", 0, CELLS.length - 1);
	/** How much of their speed whoever walks through it keeps (a cobweb's is 0.25, 0.05, 0.25). */
	public static final Vec3 SLOW = new Vec3(0.6, 0.75, 0.6);
	private static final Map<Direction, VoxelShape> SHAPES = new EnumMap<>(Direction.class);

	static {
		for (Direction facing : Direction.Plane.HORIZONTAL) {
			SHAPES.put(facing, LongDecorationBlock.turned(new double[] {0.0, 0.0, 7.0, 16.0, 16.0, 9.0}, facing));
		}
	}

	public WebDrapeBlock(Properties properties) {
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

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return SHAPES.get(state.getValue(FACING));
	}

	@Override
	protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return Shapes.empty();
	}

	@Override
	protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity, InsideBlockEffectApplier effects, boolean pastEdges) {
		entity.makeStuckInBlock(state, SLOW);
	}

	/** It has no lanterns to light. */
	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		return InteractionResult.PASS;
	}
}
