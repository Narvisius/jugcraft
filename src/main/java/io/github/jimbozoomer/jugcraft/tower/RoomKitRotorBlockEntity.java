package io.github.jimbozoomer.jugcraft.tower;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/** The anchor of a room kit piece with turning rotors (tower/RoomKitRotorBlock): no data, no ticking. */
public class RoomKitRotorBlockEntity extends BlockEntity {
	public RoomKitRotorBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
		super(type, pos, state);
	}
}
