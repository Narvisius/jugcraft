package io.github.jimbozoomer.jugcraft.building;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

/**
 * A searchlight (batch 50): a lamp drum on a turning yoke that throws a long beam (drawn by
 * client/SearchlightRenderer). Use it to turn it a sixteenth of a turn, sneak and use it to tilt it up another 15
 * degrees (up to 75, then back level). It lights its surroundings at {@value Trenchworks#SEARCHLIGHT_LIGHT}; a redstone
 * signal switches it off. Its aim is part of the block state.
 */
public class SearchlightBlock extends Block implements EntityBlock {
	public static final IntegerProperty YAW = IntegerProperty.create("yaw", 0, Trenchworks.SEARCHLIGHT_YAWS - 1);
	public static final IntegerProperty TILT = IntegerProperty.create("tilt", 0, Trenchworks.SEARCHLIGHT_TILTS);
	public static final BooleanProperty LIT = BooleanProperty.create("lit");
	private static final VoxelShape SHAPE = Block.box(2, 0, 2, 14, 16, 14);

	public SearchlightBlock(Properties properties) {
		super(properties);
		registerDefaultState(stateDefinition.any().setValue(YAW, 0).setValue(TILT, 1).setValue(LIT, true));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(YAW, TILT, LIT);
	}

	/** Placed pointing the way the player faces. */
	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		int yaw = Math.floorMod(Math.round(context.getRotation() / (360.0F / Trenchworks.SEARCHLIGHT_YAWS)), Trenchworks.SEARCHLIGHT_YAWS);
		return defaultBlockState().setValue(YAW, yaw).setValue(LIT, !context.getLevel().hasNeighborSignal(context.getClickedPos()));
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return SHAPE;
	}

	@Override
	protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighborBlock, Orientation orientation,
			boolean movedByPiston) {
		boolean lit = !level.hasNeighborSignal(pos);
		if (lit != state.getValue(LIT)) {
			level.setBlock(pos, state.setValue(LIT, lit), Block.UPDATE_ALL);
		}
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		if (!level.isClientSide()) {
			BlockState turned = player.isShiftKeyDown()
					? state.setValue(TILT, (state.getValue(TILT) + 1) % (Trenchworks.SEARCHLIGHT_TILTS + 1))
					: state.setValue(YAW, (state.getValue(YAW) + 1) % Trenchworks.SEARCHLIGHT_YAWS);
			level.setBlock(pos, turned, Block.UPDATE_ALL);
			level.playSound(null, pos, SoundEvents.IRON_TRAPDOOR_CLOSE, SoundSource.BLOCKS, 0.5F, 1.4F);
			player.sendOverlayMessage(Component.translatable("message.jugcraft.searchlight.aim",
					turned.getValue(YAW) * 360 / Trenchworks.SEARCHLIGHT_YAWS, turned.getValue(TILT) * 15));
		}
		return InteractionResult.SUCCESS;
	}

	@Override
	public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new Entity(pos, state);
	}

	/** Holds nothing: it lets client/SearchlightRenderer draw the turning head and the beam. */
	public static class Entity extends BlockEntity {
		public Entity(BlockPos pos, BlockState state) {
			super(Trenchworks.SEARCHLIGHT_ENTITY, pos, state);
		}
	}
}
