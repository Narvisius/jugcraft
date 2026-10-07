package io.github.jimbozoomer.jugcraft.agriculture;

import java.util.Locale;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

/**
 * A Silhouette Window: a framed pane of orange paper with a black cut-out ({@link Design}: a bat, a black cat or a
 * witch on her broom). Set it in a window: when a lamp, torch or jack o'lantern lights the far side (block light
 * {@value #GLOW_LIGHT} or more, and brighter than the near side), the near side glows. Each client works this out
 * from the light it already has (client/SilhouetteWindowRenderer.java). Sneak-use it with an empty hand for the next
 * design.
 */
public class SilhouetteWindowBlock extends BaseEntityBlock {
	public static final int GLOW_LIGHT = 8;
	public static final EnumProperty<Direction> FACING = HorizontalDirectionalBlock.FACING;
	public static final EnumProperty<Design> DESIGN = EnumProperty.create("design", Design.class);
	private static final VoxelShape ALONG_X = Block.box(0.0, 0.0, 7.0, 16.0, 16.0, 9.0);
	private static final VoxelShape ALONG_Z = Block.box(7.0, 0.0, 0.0, 9.0, 16.0, 16.0);

	/** The cut-outs. */
	public enum Design implements StringRepresentable {
		BAT, CAT, WITCH;

		public Design next() {
			return values()[(ordinal() + 1) % values().length];
		}

		@Override
		public String getSerializedName() {
			return name().toLowerCase(Locale.ROOT);
		}
	}

	public SilhouetteWindowBlock(Properties properties) {
		super(properties);
		registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(DESIGN, Design.BAT));
	}

	@Override
	public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new DecorationBlockEntity(JugcraftAgriculture.SILHOUETTE_WINDOW_ENTITY, pos, state);
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return state.getValue(FACING).getAxis() == Direction.Axis.Z ? ALONG_X : ALONG_Z;
	}

	@Override
	public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
		return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
	}

	/**
	 * Whether the side of the window facing {@code side} glows: the block light on the other side is bright enough, and
	 * brighter than on this side (light seeps through the pane, so only the far side's lamp counts).
	 */
	public static boolean glows(Level level, BlockPos pos, Direction side) {
		int far = light(level, pos.relative(side.getOpposite()));
		int near = light(level, pos.relative(side));
		return far >= GLOW_LIGHT && far > near;
	}

	/** The block light at {@code pos}, or what a lamp there gives off (a solid lamp's own cell may hold no light). */
	public static int light(Level level, BlockPos pos) {
		return Math.max(level.getBrightness(LightLayer.BLOCK, pos), level.getBlockState(pos).getLightEmission());
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		if (!player.isSecondaryUseActive()) {
			return InteractionResult.PASS;
		}
		if (!level.isClientSide()) {
			level.setBlock(pos, state.setValue(DESIGN, state.getValue(DESIGN).next()), Block.UPDATE_ALL);
			level.playSound(null, pos, SoundEvents.BOOK_PAGE_TURN, SoundSource.BLOCKS, 0.8F, 1.2F);
			level.gameEvent(player, GameEvent.BLOCK_CHANGE, pos);
		}
		return InteractionResult.SUCCESS;
	}

	@Override
	protected BlockState rotate(BlockState state, Rotation rotation) {
		return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
	}

	@Override
	protected BlockState mirror(BlockState state, Mirror mirror) {
		return state.rotate(mirror.getRotation(state.getValue(FACING)));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(FACING, DESIGN);
	}
}
