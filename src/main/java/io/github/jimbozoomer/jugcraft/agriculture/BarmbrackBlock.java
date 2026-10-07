package io.github.jimbozoomer.jugcraft.agriculture;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

/**
 * The Barmbrack: an Irish fruit loaf for Halloween, placed like a cake and eaten a slice at a time ({@value #SLICES}
 * slices, each {@value #NUTRITION} hunger), by a hungry player using it. One slice, picked when the loaf is placed
 * ({@link BarmbrackBlockEntity}), hides the Barmbrack Ring: whoever eats it finds the ring (the gold nugget baked into
 * the loaf) and is told they will marry within the year. Every other slice tells a smaller fortune ({@link #FORTUNES}).
 */
public class BarmbrackBlock extends BaseEntityBlock {
	public static final int SLICES = 6;
	public static final int NUTRITION = 2;
	public static final float SATURATION = 0.4F;
	public static final List<String> FORTUNES = List.of("coin", "pea", "stick", "cloth", "crumbs");
	public static final EnumProperty<Direction> FACING = HorizontalDirectionalBlock.FACING;
	public static final IntegerProperty BITES = IntegerProperty.create("bites", 0, SLICES - 1);

	public BarmbrackBlock(Properties properties) {
		super(properties);
		registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(BITES, 0));
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		double cut = 1.0 + state.getValue(BITES) * 14.0 / SLICES;
		return switch (state.getValue(FACING)) {
			case EAST -> Block.box(4.0, 0.0, cut, 12.0, 7.0, 15.0);
			case SOUTH -> Block.box(1.0, 0.0, 4.0, 16.0 - cut, 7.0, 12.0);
			case WEST -> Block.box(4.0, 0.0, 1.0, 12.0, 7.0, 16.0 - cut);
			default -> Block.box(cut, 0.0, 4.0, 15.0, 7.0, 12.0);
		};
	}

	@Override
	public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new BarmbrackBlockEntity(pos, state);
	}

	@Override
	public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
		return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
	}

	@Override
	public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
		super.setPlacedBy(level, pos, state, placer, stack);
		if (!level.isClientSide() && level.getBlockEntity(pos) instanceof BarmbrackBlockEntity loaf) {
			loaf.hideRing(level.getRandom());
		}
	}

	/** A hungry player eats the next slice, and finds what it hides. */
	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		if (!player.canEat(false)) {
			return InteractionResult.PASS;
		}
		if (level.isClientSide() || !(level.getBlockEntity(pos) instanceof BarmbrackBlockEntity loaf)) {
			return InteractionResult.SUCCESS;
		}
		int slice = state.getValue(BITES);
		player.getFoodData().eat(NUTRITION, SATURATION);
		level.playSound(null, pos, SoundEvents.GENERIC_EAT.value(), SoundSource.PLAYERS, 1.0F, 1.0F);
		level.gameEvent(player, GameEvent.EAT, pos);
		if (loaf.ringIn(slice, level.getRandom())) {
			ItemStack ring = new ItemStack(JugcraftAgriculture.item("barmbrack_ring"));
			if (!player.getInventory().add(ring)) {
				Block.popResource(level, pos, ring);
			}
			player.sendOverlayMessage(Component.translatable("message.jugcraft.barmbrack.ring"));
			level.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1.0F, 1.4F);
		} else {
			player.sendOverlayMessage(Component.translatable("message.jugcraft.barmbrack." + FORTUNES.get(level.getRandom().nextInt(FORTUNES.size()))));
		}
		if (slice + 1 >= SLICES) {
			level.removeBlock(pos, false);
			level.gameEvent(player, GameEvent.BLOCK_DESTROY, pos);
		} else {
			level.setBlock(pos, state.setValue(BITES, slice + 1), Block.UPDATE_ALL);
		}
		return InteractionResult.SUCCESS;
	}

	@Override
	protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
		return level.getBlockState(pos.below()).isSolid();
	}

	@Override
	protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos, Direction direction,
			BlockPos neighborPos, BlockState neighbor, RandomSource random) {
		return direction == Direction.DOWN && !state.canSurvive(level, pos) ? Blocks.AIR.defaultBlockState() : state;
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
		builder.add(FACING, BITES);
	}
}
