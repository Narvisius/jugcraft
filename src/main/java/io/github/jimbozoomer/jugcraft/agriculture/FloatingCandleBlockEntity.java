package io.github.jimbozoomer.jugcraft.agriculture;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** Holds nothing: it lets the client draw Floating Candles bobbing in the air (client/FloatingCandleRenderer.java). */
public class FloatingCandleBlockEntity extends BlockEntity {
	public FloatingCandleBlockEntity(BlockPos pos, BlockState state) {
		super(JugcraftAgriculture.FLOATING_CANDLE_ENTITY, pos, state);
	}
}
