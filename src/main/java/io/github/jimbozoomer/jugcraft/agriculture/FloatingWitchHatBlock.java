package io.github.jimbozoomer.jugcraft.agriculture;

import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

/**
 * A Floating Witch Hat: a pointed black hat with an orange band, floating upside up where it is placed with a candle
 * hanging lit inside it ({@link #LIT}, light {@value #LIGHT}; lit and snuffed like a candle, {@link CandleLighting}).
 * It bobs {@value #BOB} pixels and turns slowly round, each hat out of step with its neighbours (drawn by the client,
 * client/FloatingWitchHatRenderer.java). It needs nothing to stand on and nothing walks into it.
 */
public class FloatingWitchHatBlock extends BaseEntityBlock {
	public static final int LIGHT = 10;
	public static final float BOB = 1.5F;
	public static final int BOB_TICKS = 100;
	public static final int TURN_TICKS = 600;
	public static final BooleanProperty LIT = BlockStateProperties.LIT;
	private static final VoxelShape SHAPE = Block.box(2.0, 3.0, 2.0, 14.0, 16.0, 14.0);

	public FloatingWitchHatBlock(Properties properties) {
		super(properties);
		registerDefaultState(stateDefinition.any().setValue(LIT, false));
	}

	public static int light(BlockState state) {
		return state.getValue(LIT) ? LIGHT : 0;
	}

	/** How far (pixels) the hat at {@code pos} has risen or fallen at {@code time}. */
	public static float bob(BlockPos pos, double time) {
		float phase = (pos.hashCode() & 0xFF) * 0.37F;
		return BOB * Mth.sin((float) (time % BOB_TICKS / BOB_TICKS * Mth.TWO_PI) + phase);
	}

	/** How far round (degrees) the hat at {@code pos} has turned at {@code time}. */
	public static float turn(BlockPos pos, double time) {
		return (float) ((time % TURN_TICKS / TURN_TICKS * 360.0 + (pos.hashCode() & 0xFF) * 1.4) % 360.0);
	}

	@Override
	public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new DecorationBlockEntity(JugcraftAgriculture.FLOATING_HAT_ENTITY, pos, state);
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return SHAPE;
	}

	@Override
	protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand,
			BlockHitResult hit) {
		return CandleLighting.light(stack, state, level, pos, player, hand, LIT);
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		return CandleLighting.snuff(state, level, pos, player, LIT);
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(LIT);
	}
}
