package io.github.jimbozoomer.jugcraft.exterior;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * What stands on a roof over an opening in its deck (tools/exterior_kit.py): the Flat Rooflight (daylight goes down
 * through it), the Smoke Vent (its twin flaps stand OPEN when the building's fire system trips: {@link #setOpen}; use
 * it to open or shut it by hand) and the Roof Hatch (use it to open it; open, the ladder under it climbs on through).
 */
public class RoofOpeningBlock extends Block {
	public static final BooleanProperty OPEN = BlockStateProperties.OPEN;

	public enum Kind {
		ROOFLIGHT, SMOKE_VENT, HATCH
	}

	/** The kerb (16 px is the roof's top; the kerbs stand to about 21) and the deck round the opening. */
	private static final VoxelShape KERB = Shapes.or(Block.box(0, 12.6, 0, 16, 16, 0.6), Block.box(0, 12.6, 15.4, 16, 16, 16),
			Block.box(0, 12.6, 0.6, 0.6, 16, 15.4), Block.box(15.4, 12.6, 0.6, 16, 16, 15.4), Block.box(0.6, 16, 0.6, 15.4, 20.4, 2.6),
			Block.box(0.6, 16, 13.4, 15.4, 20.4, 15.4), Block.box(0.6, 16, 2.6, 2.6, 20.4, 13.4), Block.box(13.4, 16, 2.6, 15.4, 20.4, 13.4));
	private static final VoxelShape COVERED = Shapes.or(KERB, Block.box(0.6, 12.6, 0.6, 15.4, 21.6, 15.4));
	private final Kind kind;

	public RoofOpeningBlock(Kind kind, Properties properties) {
		super(properties);
		this.kind = kind;
		registerDefaultState(stateDefinition.any().setValue(OPEN, false));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(OPEN);
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return COVERED;
	}

	/** An open hatch leaves its opening clear (a player fits through to the ladder). */
	@Override
	protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return kind == Kind.HATCH && state.getValue(OPEN) ? KERB : COVERED;
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		if (kind == Kind.ROOFLIGHT) {
			return InteractionResult.PASS;
		}
		if (!level.isClientSide()) {
			boolean open = !state.getValue(OPEN);
			level.setBlock(pos, state.setValue(OPEN, open), Block.UPDATE_ALL);
			level.playSound(null, pos, open ? SoundEvents.IRON_TRAPDOOR_OPEN : SoundEvents.IRON_TRAPDOOR_CLOSE, SoundSource.BLOCKS, 1.0F, 0.9F);
		}
		return InteractionResult.SUCCESS;
	}

	/** The fire system opens (or shuts) a smoke vent. */
	public static void setOpen(ServerLevel level, BlockPos pos, boolean open) {
		BlockState state = level.getBlockState(pos);
		if (state.getBlock() instanceof RoofOpeningBlock b && b.kind == Kind.SMOKE_VENT && state.getValue(OPEN) != open) {
			level.setBlock(pos, state.setValue(OPEN, open), Block.UPDATE_ALL);
			level.playSound(null, pos, SoundEvents.PISTON_EXTEND, SoundSource.BLOCKS, 0.8F, 0.7F);
		}
	}

	public Kind kind() {
		return kind;
	}

	@Override
	protected boolean propagatesSkylightDown(BlockState state) {
		return kind == Kind.ROOFLIGHT;
	}

	@Override
	protected int getLightDampening(BlockState state) {
		return kind == Kind.ROOFLIGHT ? 0 : 15;
	}

	@Override
	protected VoxelShape getOcclusionShape(BlockState state) {
		return Shapes.empty();
	}
}
