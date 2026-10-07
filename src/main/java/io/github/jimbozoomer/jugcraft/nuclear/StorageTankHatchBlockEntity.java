package io.github.jimbozoomer.jugcraft.nuclear;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** A tank roof hatch: holds nothing; it is there so client/StorageTankHatchRenderer can swing the lid. */
public class StorageTankHatchBlockEntity extends BlockEntity {
	/** Client: the lid's swing (renderer bookkeeping). */
	public boolean animOpen;
	public long animStart = Long.MIN_VALUE;

	public StorageTankHatchBlockEntity(BlockPos pos, BlockState state) {
		super(StorageTanks.HATCH_ENTITY, pos, state);
		animOpen = state.getValue(StorageTankHatchBlock.OPEN);
	}
}
