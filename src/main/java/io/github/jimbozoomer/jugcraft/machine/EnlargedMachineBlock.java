package io.github.jimbozoomer.jugcraft.machine;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

/**
 * A machine that was one block before batch 44 and is a multi-block now ({@link MachineKind#enlarged()}).
 * <p>
 * Copies built before then keep working where they stand: a block saved without {@link #COMPACT} loads with the
 * default, {@code compact=true}, which is one block with the old model and behaves exactly as before. Placing the
 * item always builds the full machine ({@code compact=false}). Commands and structures that place the default state
 * get the compact block too, as they always did.
 */
public class EnlargedMachineBlock extends LargeMachineBlock {
	/** A one-block copy from before the machine was enlarged. */
	public static final BooleanProperty COMPACT = BooleanProperty.create("compact");

	public EnlargedMachineBlock(Properties properties, MachineKind kind) {
		super(properties, kind);
		this.registerDefaultState(this.defaultBlockState().setValue(COMPACT, true));
	}

	@Override
	public Footprint footprint(BlockState state) {
		return state.getValue(COMPACT) ? Footprint.SINGLE : super.footprint(state);
	}

	@Override
	public BlockState formed(BlockState state) {
		return state.setValue(COMPACT, false);
	}

	/** A compact copy is one block, so it turns like any one-block machine; the full machine never does. */
	@Override
	protected BlockState rotate(BlockState state, Rotation rotation) {
		return state.getValue(COMPACT) ? state.setValue(FACING, rotation.rotate(state.getValue(FACING))) : state;
	}

	@Override
	protected BlockState mirror(BlockState state, Mirror mirror) {
		return state.getValue(COMPACT) ? state.rotate(mirror.getRotation(state.getValue(FACING))) : state;
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		super.createBlockStateDefinition(builder);
		builder.add(COMPACT);
	}
}
