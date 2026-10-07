package io.github.jimbozoomer.jugcraft.agriculture;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
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
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

/**
 * The Pumpkin Chunkin' Trebuchet: a wooden siege engine, two blocks tall, that throws pumpkins for distance in the
 * direction it faces. Use a pumpkin on it to load the sling (any in item tag {@code jugcraft:trebuchet_ammo}); use it
 * with an empty hand to let fly; sneak-use it to change the release angle ({@value TrebuchetBlockEntity#MIN_ANGLE}
 * to {@value TrebuchetBlockEntity#MAX_ANGLE} degrees). Unloaded, using it shows the board. The arm shows what it is
 * doing ({@link Arm}); after a throw it swings back in {@link #RESET_TICKS} ticks. See {@link TrebuchetBlockEntity}.
 */
public class TrebuchetBlock extends BaseEntityBlock {
	public static final EnumProperty<Direction> FACING = HorizontalDirectionalBlock.FACING;
	public static final EnumProperty<Arm> ARM = EnumProperty.create("arm", Arm.class);
	public static final int RESET_TICKS = 30;

	/** The arm: drawn back and empty, drawn back with a pumpkin in the sling, or swung over after a throw. */
	public enum Arm implements StringRepresentable {
		READY("ready"), LOADED("loaded"), RELEASED("released");

		private final String name;

		Arm(String name) {
			this.name = name;
		}

		@Override
		public String getSerializedName() {
			return name;
		}
	}

	/** The base and the two uprights, which stand across the throwing direction (models: tools/night_data.py). */
	private static final VoxelShape NORTH_SOUTH = Shapes.or(Block.box(0.0, 0.0, 0.0, 16.0, 4.0, 16.0), Block.box(1.0, 4.0, 7.0, 4.0, 21.0, 9.0),
			Block.box(12.0, 4.0, 7.0, 15.0, 21.0, 9.0));
	private static final VoxelShape EAST_WEST = Shapes.or(Block.box(0.0, 0.0, 0.0, 16.0, 4.0, 16.0), Block.box(7.0, 4.0, 1.0, 9.0, 21.0, 4.0),
			Block.box(7.0, 4.0, 12.0, 9.0, 21.0, 15.0));

	public TrebuchetBlock(Properties properties) {
		super(properties);
		registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(ARM, Arm.READY));
	}

	@Override
	public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new TrebuchetBlockEntity(pos, state);
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return state.getValue(FACING).getAxis() == Direction.Axis.Z ? NORTH_SOUTH : EAST_WEST;
	}

	/** It throws away from whoever places it. */
	@Override
	public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
		return defaultBlockState().setValue(FACING, context.getHorizontalDirection());
	}

	/** A pumpkin loads the empty sling; anything else counts as an empty hand. */
	@Override
	protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand,
			BlockHitResult hit) {
		if (!stack.is(TrebuchetBlockEntity.AMMO)) {
			return InteractionResult.TRY_WITH_EMPTY_HAND;
		}
		if (!(player instanceof ServerPlayer loader) || !(level.getBlockEntity(pos) instanceof TrebuchetBlockEntity trebuchet)) {
			return InteractionResult.SUCCESS;
		}
		if (state.getValue(ARM) != Arm.READY || !trebuchet.loaded().isEmpty()) {
			loader.sendOverlayMessage(Component.translatable(state.getValue(ARM) == Arm.RELEASED
					? "message.jugcraft.trebuchet.resetting" : "message.jugcraft.trebuchet.already_loaded"));
			return InteractionResult.SUCCESS;
		}
		trebuchet.load(stack);
		stack.consume(1, player);
		level.setBlock(pos, state.setValue(ARM, Arm.LOADED), Block.UPDATE_ALL);
		level.playSound(null, pos, SoundEvents.WOOD_PLACE, SoundSource.BLOCKS, 1.0F, 0.8F);
		level.gameEvent(player, GameEvent.BLOCK_CHANGE, pos);
		loader.sendOverlayMessage(Component.translatable("message.jugcraft.trebuchet.loaded", trebuchet.angle()));
		return InteractionResult.SUCCESS;
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		if (!(player instanceof ServerPlayer user) || !(level.getBlockEntity(pos) instanceof TrebuchetBlockEntity trebuchet)) {
			return InteractionResult.SUCCESS;
		}
		if (user.isSecondaryUseActive()) {
			user.sendOverlayMessage(Component.translatable("message.jugcraft.trebuchet.angle", trebuchet.nextAngle()));
			level.playSound(null, pos, SoundEvents.WOODEN_TRAPDOOR_CLOSE, SoundSource.BLOCKS, 0.6F, 1.4F);
			return InteractionResult.SUCCESS;
		}
		if (state.getValue(ARM) != Arm.LOADED) {
			trebuchet.show(user);
			return InteractionResult.SUCCESS;
		}
		fire(user, (ServerLevel) level, pos, state, trebuchet);
		return InteractionResult.SUCCESS;
	}

	/** Lets fly: the pumpkin leaves the sling, the arm swings over and comes back after {@link #RESET_TICKS}. */
	public static FlyingPumpkin fire(ServerPlayer player, ServerLevel level, BlockPos pos, BlockState state, TrebuchetBlockEntity trebuchet) {
		FlyingPumpkin pumpkin = trebuchet.fire(player);
		level.setBlock(pos, state.setValue(ARM, Arm.RELEASED), Block.UPDATE_ALL);
		level.scheduleTick(pos, state.getBlock(), RESET_TICKS);
		level.playSound(null, pos, SoundEvents.CROSSBOW_SHOOT, SoundSource.BLOCKS, 1.2F, 0.5F);
		level.playSound(null, pos, SoundEvents.WOOD_BREAK, SoundSource.BLOCKS, 0.8F, 0.6F);
		level.gameEvent(player, GameEvent.BLOCK_ACTIVATE, pos);
		return pumpkin;
	}

	/** The arm swings back after a throw. */
	@Override
	protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
		if (state.getValue(ARM) == Arm.RELEASED) {
			level.setBlock(pos, state.setValue(ARM, Arm.READY), Block.UPDATE_ALL);
			level.playSound(null, pos, SoundEvents.WOODEN_DOOR_CLOSE, SoundSource.BLOCKS, 0.7F, 0.6F);
		}
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
		builder.add(FACING, ARM);
	}
}
