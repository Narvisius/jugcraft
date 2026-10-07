package io.github.jimbozoomer.jugcraft.agriculture;

import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
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
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

/**
 * The Rocking Chair: a high-backed wooden chair on curved runners, with a faded velvet cushion. Use it to sit in it
 * (one sitter; sneak to get up) and it rocks gently under you. At night, with nobody in it, it rocks on its own and
 * creaks. The chair is drawn by the client (client/RockingChairRenderer.java), so it can rock; nothing on the server
 * moves.
 */
public class RockingChairBlock extends BaseEntityBlock implements Seat.Sittable {
	public static final double SEAT_HEIGHT = 9.0 / 16.0;
	/** How far it rocks either way, in degrees: on its own at night, and under a sitter. */
	public static final float HAUNTED_ROCK = 7.0F;
	public static final float SITTER_ROCK = 4.0F;
	/** Ticks for one rock back and forth. */
	public static final int ROCK_PERIOD = 50;
	public static final EnumProperty<Direction> FACING = HorizontalDirectionalBlock.FACING;
	/** By the direction it faces: the seat, and the back behind it. */
	private static final Map<Direction, VoxelShape> SHAPES = Map.of(
			Direction.NORTH, Shapes.or(Block.box(1.0, 0.0, 1.0, 15.0, 9.0, 15.0), Block.box(1.0, 9.0, 12.0, 15.0, 16.0, 15.0)),
			Direction.SOUTH, Shapes.or(Block.box(1.0, 0.0, 1.0, 15.0, 9.0, 15.0), Block.box(1.0, 9.0, 1.0, 15.0, 16.0, 4.0)),
			Direction.WEST, Shapes.or(Block.box(1.0, 0.0, 1.0, 15.0, 9.0, 15.0), Block.box(12.0, 9.0, 1.0, 15.0, 16.0, 15.0)),
			Direction.EAST, Shapes.or(Block.box(1.0, 0.0, 1.0, 15.0, 9.0, 15.0), Block.box(1.0, 9.0, 1.0, 4.0, 16.0, 15.0)));

	public RockingChairBlock(Properties properties) {
		super(properties);
		registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
	}

	@Override
	public double seatHeight(BlockState state) {
		return SEAT_HEIGHT;
	}

	@Override
	public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new DecorationBlockEntity(JugcraftAgriculture.ROCKING_CHAIR_ENTITY, pos, state);
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return SHAPES.get(state.getValue(FACING));
	}

	/** It faces whoever placed it, so they can sit straight down. */
	@Override
	public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
		return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		if (player.isSecondaryUseActive()) {
			return InteractionResult.PASS;
		}
		if (level instanceof ServerLevel server && !Seat.sit(server, pos, state, player)) {
			return InteractionResult.PASS;
		}
		return InteractionResult.SUCCESS;
	}

	/** How far the chair leans at {@code time} (ticks, with the partial tick), in degrees; 0 when it is still. */
	public static float rock(BlockPos pos, boolean occupied, boolean night, float time) {
		float most = occupied ? SITTER_ROCK : night ? HAUNTED_ROCK : 0.0F;
		if (most == 0.0F) {
			return 0.0F;
		}
		float phase = (pos.hashCode() & 0xFF) / 256.0F;
		return most * (float) Math.sin((time / ROCK_PERIOD + phase) * Math.PI * 2);
	}

	/** At night an empty chair creaks now and then as it rocks (heard by each client, near it). */
	@Override
	public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
		if (random.nextInt(6) == 0 && MourningAngelBlock.night(level) && Seat.at(level, pos).isEmpty()) {
			level.playLocalSound(pos.getX() + 0.5, pos.getY() + 0.3, pos.getZ() + 0.5, SoundEvents.WOOD_STEP, SoundSource.BLOCKS, 0.5F,
					0.5F + random.nextFloat() * 0.15F, false);
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
		builder.add(FACING);
	}
}
