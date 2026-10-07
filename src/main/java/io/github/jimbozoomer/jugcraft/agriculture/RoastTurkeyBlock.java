package io.github.jimbozoomer.jugcraft.agriculture;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

/**
 * A roast turkey on its platter, placed on a table like a pie: {@value #SERVINGS} servings. A hungry player using it eats a
 * serving ({@value #NUTRITION} hunger); a Carving Knife carves a slice off to take away (Carving the Bird). First the
 * drumsticks go, then the breast; the last serving leaves the carcass's bones (a bone, for bone meal) and takes the
 * platter. Only a whole turkey can be picked up again.
 */
public class RoastTurkeyBlock extends Block {
	public static final int SERVINGS = 6;
	public static final int NUTRITION = 3;
	public static final float SATURATION = 0.6F;
	public static final EnumProperty<Direction> FACING = HorizontalDirectionalBlock.FACING;
	public static final IntegerProperty BITES = IntegerProperty.create("bites", 0, SERVINGS - 1);
	private static final VoxelShape WHOLE = Block.box(1.0, 0.0, 1.0, 15.0, 7.0, 15.0);
	private static final VoxelShape CARVED = Block.box(1.0, 0.0, 1.0, 15.0, 5.0, 15.0);

	public RoastTurkeyBlock(Properties properties) {
		super(properties);
		registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(BITES, 0));
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return state.getValue(BITES) < 3 ? WHOLE : CARVED;
	}

	@Override
	public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
		return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
	}

	/** A Carving Knife carves a slice to take away. */
	@Override
	protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand,
			BlockHitResult hit) {
		if (!(stack.getItem() instanceof CarvingKnifeItem)) {
			return InteractionResult.TRY_WITH_EMPTY_HAND;
		}
		if (!level.isClientSide()) {
			ItemStack slice = new ItemStack(JugcraftAgriculture.item("turkey_slice"));
			if (!player.getInventory().add(slice)) {
				Block.popResource(level, pos, slice);
			}
			level.playSound(null, pos, SoundEvents.HONEY_BLOCK_SLIDE, SoundSource.BLOCKS, 0.6F, 1.2F);
			if (player instanceof ServerPlayer server) {
				TrickOrTreat.award(server, "carving_the_bird");
			}
			takeServing(level, pos, state, player);
		}
		return InteractionResult.SUCCESS;
	}

	/** A hungry player eats the next serving. */
	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		if (!player.canEat(false)) {
			return InteractionResult.PASS;
		}
		if (level.isClientSide()) {
			return InteractionResult.SUCCESS;
		}
		player.getFoodData().eat(NUTRITION, SATURATION);
		level.playSound(null, pos, SoundEvents.GENERIC_EAT.value(), SoundSource.PLAYERS, 1.0F, 1.0F);
		level.gameEvent(player, GameEvent.EAT, pos);
		takeServing(level, pos, state, player);
		return InteractionResult.SUCCESS;
	}

	private static void takeServing(Level level, BlockPos pos, BlockState state, Player player) {
		int bites = state.getValue(BITES);
		if (bites + 1 >= SERVINGS) {
			level.removeBlock(pos, false);
			Block.popResource(level, pos, new ItemStack(BuiltInRegistries.ITEM.getValue(Identifier.withDefaultNamespace("bone"))));
			level.gameEvent(player, GameEvent.BLOCK_DESTROY, pos);
		} else {
			level.setBlock(pos, state.setValue(BITES, bites + 1), Block.UPDATE_ALL);
		}
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
	protected boolean hasAnalogOutputSignal(BlockState state) {
		return true;
	}

	/** Servings left: 15 for a whole turkey, down by a sixth a serving. */
	@Override
	protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos, Direction direction) {
		return (SERVINGS - state.getValue(BITES)) * 15 / SERVINGS;
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
