package io.github.jimbozoomer.jugcraft.nuclear;

import io.github.jimbozoomer.jugcraft.tower.FurnitureBlock;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;

/**
 * Furniture that comes out different each time it is placed, the way paintings do: whiteboards with different
 * meetings' scribbles on them, framed prints, office plants. The variant is picked at random on placement.
 */
public class VariantFurnitureBlock extends FurnitureBlock {
	private static final Map<Integer, IntegerProperty> PROPERTIES = new HashMap<>();
	/** The variant count of the block being built (read while the block's state definition is made). */
	private static int building;
	private final IntegerProperty variant;
	private final int count;

	private int steamVariant = -1;
	private double steamX, steamY, steamZ;

	/** The variant with a hot coffee in it, and where the mug's middle is (model pixels, facing north). */
	public VariantFurnitureBlock steam(int variant, double x, double y, double z) {
		steamVariant = variant;
		steamX = x;
		steamY = y;
		steamZ = z;
		return this;
	}

	@Override
	public void animateTick(BlockState state, net.minecraft.world.level.Level level, net.minecraft.core.BlockPos pos,
			net.minecraft.util.RandomSource random) {
		if (state.getValue(variant) == steamVariant) {
			Steam.puff(level, pos, state.getValue(FACING), steamX, steamY, steamZ, random);
		}
	}

	public static IntegerProperty property(int count) {
		return PROPERTIES.computeIfAbsent(count, n -> IntegerProperty.create("variant", 0, n - 1));
	}

	public static VariantFurnitureBlock create(Properties properties, int count, double[]... boxes) {
		building = count;
		return new VariantFurnitureBlock(properties, count, boxes);
	}

	/** Before a subclass is made: its variant count (its state definition is made while it is built). */
	protected static void preparing(int count) {
		building = count;
	}

	protected VariantFurnitureBlock(Properties properties, int count, double[]... boxes) {
		super(properties, boxes);
		this.count = count;
		this.variant = property(count);
		registerDefaultState(defaultBlockState().setValue(variant, 0));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		super.createBlockStateDefinition(builder);
		builder.add(property(building));
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		return super.getStateForPlacement(context).setValue(variant, context.getLevel().getRandom().nextInt(count));
	}
}
