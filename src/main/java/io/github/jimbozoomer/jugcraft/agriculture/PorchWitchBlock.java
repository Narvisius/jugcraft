package io.github.jimbozoomer.jugcraft.agriculture;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import org.jspecify.annotations.Nullable;

/**
 * The Animatronic Porch Witch: a green-faced witch two blocks tall in a tall pointed hat, stirring a bubbling pot of
 * green brew with a long wooden spoon, slowly, round and round. When someone walks up to her (within {@value #REACH}
 * blocks, after nobody was there) she throws her head back and cackles, stirring hard, her eyes glowing
 * ({@link #CACKLING}, for {@value #CACKLE_TICKS} ticks); then she won't again for {@value #COOLDOWN_TICKS}. Her head
 * follows the nearest player. {@link PorchWitchBlockEntity} watches for visitors on the server; her arm and head are
 * drawn by the client (client/PorchWitchRenderer.java).
 */
public class PorchWitchBlock extends TallDecorationBlock implements EntityBlock {
	public static final double REACH = 4.0;
	public static final int CACKLE_TICKS = 40;
	public static final int COOLDOWN_TICKS = 200;
	public static final int STIR_PERIOD = 60;
	public static final int FAST_STIR_PERIOD = 16;
	public static final double WATCH_RANGE = 8.0;
	public static final float MAX_TURN = 60.0F;
	/** How far her arm swings about her shoulder as she stirs (degrees, side to side and up and down). */
	public static final float STIR_YAW = 8.0F;
	public static final float STIR_PITCH = 5.0F;
	public static final BooleanProperty CACKLING = BooleanProperty.create("cackling");

	public PorchWitchBlock(Properties properties) {
		super(properties, Block.box(2.0, 0.0, 0.0, 15.0, 16.0, 16.0), Block.box(3.0, 0.0, 6.0, 13.0, 16.0, 16.0));
		registerDefaultState(defaultBlockState().setValue(CACKLING, false));
	}

	/** Where her spoon is in its round at {@code time}: {yaw, pitch} of her arm, in degrees; faster as she cackles. */
	public static float[] stir(boolean cackling, float time) {
		float t = time / (cackling ? FAST_STIR_PERIOD : STIR_PERIOD) * Mth.TWO_PI;
		return new float[] {STIR_YAW * Mth.sin(t), STIR_PITCH * Mth.cos(t)};
	}

	@Override
	public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return state.getValue(HALF) == DoubleBlockHalf.LOWER ? new PorchWitchBlockEntity(pos, state) : null;
	}

	@Override
	public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
		if (level.isClientSide() || type != JugcraftAgriculture.PORCH_WITCH_ENTITY) {
			return null;
		}
		return (tickLevel, pos, tickState, entity) -> ((PorchWitchBlockEntity) entity).serverTick((ServerLevel) tickLevel);
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		super.createBlockStateDefinition(builder);
		builder.add(CACKLING);
	}
}
