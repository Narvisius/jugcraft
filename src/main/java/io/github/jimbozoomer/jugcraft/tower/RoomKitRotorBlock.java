package io.github.jimbozoomer.jugcraft.tower;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

/**
 * A room kit piece with rotors that turn (the roof plant's dry cooler, tools/roof_plant.py): its anchor holds a block
 * entity the client draws them from (client/RoomKitRotorRenderer). Look-only, as every cosmetic piece of the tower:
 * nothing ticks on the server, the client turns the fans by the game time.
 */
public class RoomKitRotorBlock extends RoomKitBlock implements EntityBlock {
	static RoomKitRotorBlock create(RoomKit.Spec spec, Properties properties) {
		return (RoomKitRotorBlock) RoomKitBlock.create(spec, properties, RoomKitRotorBlock::new);
	}

	protected RoomKitRotorBlock(RoomKit.Spec spec, Properties properties) {
		super(spec, properties);
	}

	@Override
	public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return isAnchor(state) ? new RoomKitRotorBlockEntity(RoomKit.ROTOR_ENTITY, pos, state) : null;
	}

	/** Which way the piece faces (its models and rotors turned from north). */
	public net.minecraft.core.Direction facingOf(BlockState state) {
		return state.hasProperty(FACING_PROP) ? state.getValue(FACING_PROP) : net.minecraft.core.Direction.NORTH;
	}
}
