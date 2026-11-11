package io.github.jimbozoomer.jugcraft.warehouse;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

/**
 * An aisle sign hung over an aisle's end (part W3): its two faces show the aisle (its floor and letter), its bays and
 * levels, given by the terminal when it finds its warehouse (AisleSignBlockEntity; drawn by
 * client/warehouse/AisleSignRenderer.java).
 */
public class AisleSignBlock extends DecorBlock implements EntityBlock {
	public AisleSignBlock(Properties properties) {
		super("aisle_sign", properties);
	}

	@Override
	public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new AisleSignBlockEntity(pos, state);
	}
}
