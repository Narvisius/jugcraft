package io.github.jimbozoomer.jugcraft.agriculture;

import java.util.Locale;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
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
 * The Grimoire Stand: a carved stand holding an old spellbook open toward whoever placed it. Use it to turn to the
 * next spread ({@link Spread}): each is a different "spell", named as the page turns. Its runes give a faint glow
 * (light {@value #LIGHT}) and, at night, motes rise from the pages (drawn by clients only).
 */
public class GrimoireStandBlock extends Block {
	public static final int LIGHT = 3;
	public static final EnumProperty<Direction> FACING = HorizontalDirectionalBlock.FACING;
	public static final EnumProperty<Spread> PAGE = EnumProperty.create("page", Spread.class);
	private static final VoxelShape SHAPE = Shapes.or(Block.box(3.0, 0.0, 3.0, 13.0, 2.0, 13.0), Block.box(6.0, 2.0, 6.0, 10.0, 11.0, 10.0),
			Block.box(1.0, 11.0, 2.0, 15.0, 15.0, 14.0));

	/** The book's spreads, each a spell with its own drawings. */
	public enum Spread implements StringRepresentable {
		MOONS, BATS, BREW, PUMPKIN;

		public Spread next() {
			return values()[(ordinal() + 1) % values().length];
		}

		@Override
		public String getSerializedName() {
			return name().toLowerCase(Locale.ROOT);
		}
	}

	public GrimoireStandBlock(Properties properties) {
		super(properties);
		registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(PAGE, Spread.MOONS));
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return SHAPE;
	}

	@Override
	public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
		return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
	}

	/** Turns to the next spread and names its spell. */
	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		if (!level.isClientSide()) {
			Spread next = state.getValue(PAGE).next();
			level.setBlock(pos, state.setValue(PAGE, next), Block.UPDATE_ALL);
			level.playSound(null, pos, SoundEvents.BOOK_PAGE_TURN, SoundSource.BLOCKS, 1.0F, 1.0F);
			level.gameEvent(player, GameEvent.BLOCK_CHANGE, pos);
			player.sendOverlayMessage(Component.translatable("message.jugcraft.grimoire." + next.getSerializedName()));
		}
		return InteractionResult.SUCCESS;
	}

	@Override
	public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
		if (random.nextInt(4) == 0 && MourningAngelBlock.night(level)) {
			level.addParticle(ParticleTypes.ENCHANT, pos.getX() + 0.3 + random.nextDouble() * 0.4, pos.getY() + 1.0, pos.getZ() + 0.3
					+ random.nextDouble() * 0.4, 0.0, 0.3, 0.0);
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
		builder.add(FACING, PAGE);
	}
}
