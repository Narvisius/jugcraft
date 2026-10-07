package io.github.jimbozoomer.jugcraft.agriculture;

import net.minecraft.core.BlockPos;
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
 * The Shadow Puppet Lamp: a candle on a turned wooden base under a three-sided paper shade with a bat, a cat and a
 * witch cut out of it. Lit like a candle ({@link #LIT}, light {@value #LIGHT}; see {@link CandleLighting}), the warm
 * shade turns slowly round, once every {@value #TURN_TICKS} ticks, and the three shapes go sliding over the walls of
 * the room, up to {@value #RANGE} blocks away. The shade and the shadows are drawn by the client
 * (client/ShadowPuppetLampRenderer.java), which looks along at most {@value #RANGE} blocks for each shadow's wall.
 */
public class ShadowPuppetLampBlock extends BaseEntityBlock {
	public static final int LIGHT = 12;
	public static final int TURN_TICKS = 240;
	public static final int RANGE = 6;
	public static final BooleanProperty LIT = BlockStateProperties.LIT;
	private static final VoxelShape SHAPE = Block.box(3.5, 0.0, 3.5, 12.5, 11.5, 12.5);

	public ShadowPuppetLampBlock(Properties properties) {
		super(properties);
		registerDefaultState(stateDefinition.any().setValue(LIT, false));
	}

	public static int light(BlockState state) {
		return state.getValue(LIT) ? LIGHT : 0;
	}

	/** How far round (degrees) the shade at {@code pos} has turned at {@code time} (ticks with the partial tick). */
	public static float turn(BlockPos pos, double time) {
		return (float) ((time / TURN_TICKS * 360.0 + (pos.hashCode() & 0xFF) * 1.4) % 360.0);
	}

	@Override
	public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new DecorationBlockEntity(JugcraftAgriculture.SHADOW_LAMP_ENTITY, pos, state);
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
